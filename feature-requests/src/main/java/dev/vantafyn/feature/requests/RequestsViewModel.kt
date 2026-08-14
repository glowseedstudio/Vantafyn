package dev.vantafyn.feature.requests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vantafyn.core.integrations.IntegrationResult
import dev.vantafyn.core.integrations.MediaRequestItem
import dev.vantafyn.core.integrations.MediaRequestSearchResult
import dev.vantafyn.core.integrations.MediaRequestType
import dev.vantafyn.core.jellyfin.JellyfinAvailabilityIndex
import dev.vantafyn.core.jellyfin.JellyfinAvailabilityMatch
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ombi.OmbiAccessMode
import dev.vantafyn.core.ombi.OmbiAccessRequest
import dev.vantafyn.core.ombi.OmbiAccessRequestStatus
import dev.vantafyn.core.ombi.OmbiCapabilities
import dev.vantafyn.core.ombi.OmbiConfig
import dev.vantafyn.core.ombi.OmbiConnectionReport
import dev.vantafyn.core.ombi.OmbiDiscoverRail
import dev.vantafyn.core.ombi.OmbiIdentityMode
import dev.vantafyn.core.ombi.OmbiLinkedAccountState
import dev.vantafyn.core.ombi.OmbiRepository
import dev.vantafyn.core.ombi.OmbiTvRequestSelection
import dev.vantafyn.core.ombi.OmbiUserCapabilities
import dev.vantafyn.core.ombi.OmbiUserMatch
import dev.vantafyn.core.ombi.OmbiUserMatchState
import dev.vantafyn.core.ombi.OmbiUserSession
import dev.vantafyn.core.ombi.OmbiUserMapping
import dev.vantafyn.core.ombi.RequestMediaDetail
import dev.vantafyn.core.ombi.RequestMediaSummary
import dev.vantafyn.core.ombi.RequestSearchFilterValue
import dev.vantafyn.core.ombi.VantafynCompanionCapabilities
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RequestsViewModel(application: Application) : AndroidViewModel(application) {
    private val ombiRepository = OmbiRepository(application)
    private val jellyfinProvider = JellyfinRepositoryProvider(application)
    private var searchJob: Job? = null
    private var activeSession: JellyfinSession? = null

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<RequestsUiState> = _state.asStateFlow()

    fun bindSession(session: JellyfinSession?) {
        activeSession = session
        ombiRepository.cleanupAccessRequests()
        val config = ombiRepository.config()
        val previousCompanion = _state.value.companionCapabilities?.takeIf { session != null }
        _state.update {
            it.copy(
                currentUserName = session?.user?.name,
                currentUserId = session?.user?.id?.toString(),
                currentServerName = session?.server?.name ?: session?.server?.url,
                isJellyfinAdmin = session?.user?.isAdministrator == true,
                config = config,
                hasApiKey = ombiRepository.hasApiKey(),
                baseUrl = config.baseUrl.ifBlank { it.baseUrl },
                accessMode = config.accessMode,
                identityMode = config.identityMode,
                capabilities = config.capabilities,
                connectionStatus = config.statusLabel(ombiRepository.hasApiKey()),
                accessRequests = ombiRepository.accessRequests(),
                userMappings = ombiRepository.userMappings(),
                currentUserMapping = session?.user?.id?.toString()?.let(ombiRepository::mappingFor),
                ombiUserSession = session?.user?.id?.toString()?.let(ombiRepository::userSession),
                userCapabilities = ombiRepository.cachedUserCapabilities(session?.user?.id?.toString()),
                companionCapabilities = previousCompanion,
            )
        }
        detectCompanion(session)
        val isAdmin = session?.user?.isAdministrator == true
        val userId = session?.user?.id?.toString()
        val canUse = canUseRequests(config, hasApiKey = ombiRepository.hasApiKey(), isAdmin = isAdmin)
        if (canUse && config.identityMode == OmbiIdentityMode.PerUserAccount && userId != null && ombiRepository.userSession(userId) != null) {
            validateLinkedAccount()
        }
        if (canUse && config.identityMode == OmbiIdentityMode.PerUserAccount && userId != null && ombiRepository.userSession(userId) == null) {
            checkOmbiUserMatch()
        }
        if (canUse && (config.identityMode == OmbiIdentityMode.SharedApiKey || isAdmin)) {
            loadRequests()
            loadDiscovery()
        }
        if (canUse && session != null && _state.value.availabilityIndex == null) {
            refreshAvailabilityIndex(silent = true)
        }
    }

    private fun detectCompanion(session: JellyfinSession?) {
        if (session == null) {
            _state.update { it.copy(companionCapabilities = null) }
            return
        }
        viewModelScope.launch {
            when (val result = ombiRepository.companionCapabilities(session)) {
                is IntegrationResult.Success -> {
                    _state.update {
                        it.copy(
                            companionCapabilities = result.value,
                            connectionStatus = if (result.value.requestsReady) "Companion connected" else it.connectionStatus,
                        )
                    }
                    if (result.value.requestsReady) {
                        if (result.value.requiresUserLogin) {
                            validateLinkedAccount()
                        } else {
                            loadDiscovery()
                        }
                    }
                }
                is IntegrationResult.Failure -> {
                    _state.update { it.copy(companionCapabilities = null) }
                }
            }
        }
    }

    fun startSetup() {
        if (!requireAdmin()) return
        _state.update {
            it.copy(
                mode = RequestsScreenMode.Setup,
                setupStep = OmbiSetupStep.Connect,
                message = null,
            )
        }
    }

    fun manageOmbi() {
        if (!requireAdmin()) return
        ombiRepository.cleanupAccessRequests()
        refreshConfig()
        _state.update { it.copy(mode = RequestsScreenMode.Manage, message = null) }
    }

    fun showSetupHealth() {
        if (!requireAdmin()) return
        refreshConfig()
        _state.update { it.copy(mode = RequestsScreenMode.Health, message = null) }
    }

    fun showRequests() {
        _state.update { it.copy(mode = RequestsScreenMode.Requests, message = null) }
    }

    fun checkOmbiUserMatch() {
        val snapshot = _state.value
        if (snapshot.usesCompanionRequests) return
        if (snapshot.identityMode != OmbiIdentityMode.PerUserAccount || snapshot.currentUserName.isNullOrBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(ombiUserMatch = OmbiUserMatch(OmbiUserMatchState.NotChecked)) }
            when (val result = ombiRepository.findOmbiUserMatch(snapshot.currentUserName)) {
                is IntegrationResult.Success -> {
                    val match = result.value
                    _state.update {
                        it.copy(
                            ombiUserMatch = match,
                            ombiUsername = if (match.state == OmbiUserMatchState.MatchFound) {
                                match.user?.userName.orEmpty()
                            } else {
                                it.ombiUsername
                            },
                        )
                    }
                }
                is IntegrationResult.Failure -> _state.update {
                    it.copy(ombiUserMatch = OmbiUserMatch(OmbiUserMatchState.UnknownUnavailable))
                }
            }
        }
    }

    fun onBaseUrlChanged(value: String) {
        if (!_state.value.isJellyfinAdmin) return
        _state.update { it.copy(baseUrl = value, message = null) }
    }

    fun onApiKeyChanged(value: String) {
        if (!_state.value.isJellyfinAdmin) return
        _state.update { it.copy(apiKey = value, message = null) }
    }

    fun onOmbiUsernameChanged(value: String) {
        _state.update { it.copy(ombiUsername = value, message = null) }
    }

    fun onOmbiPasswordChanged(value: String) {
        _state.update { it.copy(ombiPassword = value, message = null) }
    }

    fun onQueryChanged(value: String) {
        _state.update { it.copy(query = value, message = null) }
        searchJob?.cancel()
        if (value.trim().length >= 2 && _state.value.canSearchAndRequest) {
            searchJob = viewModelScope.launch {
                delay(350L)
                search()
            }
        } else {
            _state.update { it.copy(results = emptyList(), searchResults = emptyList()) }
        }
    }

    fun onSearchFilterChanged(value: RequestSearchFilter) {
        _state.update { it.copy(searchFilter = value, message = null) }
        if (_state.value.query.trim().length >= 2 && _state.value.canSearchAndRequest) {
            search()
        }
    }

    fun onAccessModeChanged(value: OmbiAccessMode) {
        if (!_state.value.isJellyfinAdmin) return
        _state.update { it.copy(accessMode = value, message = null) }
    }

    fun onIdentityModeChanged(value: OmbiIdentityMode) {
        if (!requireAdmin()) return
        ombiRepository.setIdentityMode(value)
        ombiRepository.cleanupAccessRequests()
        refreshConfig()
        _state.update {
            it.copy(
                identityMode = value,
                message = null,
            )
        }
    }

    fun nextSetupStep() {
        if (!_state.value.isJellyfinAdmin) return
        val snapshot = _state.value
        _state.update {
            it.copy(
                setupStep = when (snapshot.setupStep) {
                    OmbiSetupStep.Connect -> OmbiSetupStep.Authentication
                    OmbiSetupStep.Authentication -> OmbiSetupStep.Test
                    OmbiSetupStep.Test -> OmbiSetupStep.Finish
                    OmbiSetupStep.Access -> OmbiSetupStep.Finish
                    OmbiSetupStep.Finish -> OmbiSetupStep.Finish
                },
                message = null,
            )
        }
    }

    fun previousSetupStep() {
        if (!_state.value.isJellyfinAdmin) return
        val snapshot = _state.value
        _state.update {
            if (snapshot.setupStep == OmbiSetupStep.Connect) {
                it.copy(mode = RequestsScreenMode.Requests, message = null)
            } else {
                it.copy(
                    setupStep = when (snapshot.setupStep) {
                        OmbiSetupStep.Connect -> OmbiSetupStep.Connect
                        OmbiSetupStep.Authentication -> OmbiSetupStep.Connect
                        OmbiSetupStep.Test -> OmbiSetupStep.Authentication
                        OmbiSetupStep.Access -> OmbiSetupStep.Test
                        OmbiSetupStep.Finish -> OmbiSetupStep.Test
                    },
                    message = null,
                )
            }
        }
    }

    fun saveDraftAndTest() {
        if (!requireAdmin()) return
        val snapshot = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, message = null) }
            runCatching {
                ombiRepository.saveConfig(
                    accessMode = OmbiAccessMode.Disabled,
                    baseUrl = snapshot.baseUrl,
                    apiKey = snapshot.apiKey,
                    apiAlias = snapshot.currentUserName,
                )
            }.onFailure { throwable ->
                _state.update { it.copy(isTesting = false, message = throwable.message ?: "Check the Ombi server address.") }
                return@launch
            }
            when (val result = ombiRepository.testConnectionReport()) {
                is IntegrationResult.Success -> {
                    _state.update {
                        it.copy(
                            isTesting = false,
                            config = ombiRepository.config(),
                            hasApiKey = true,
                            apiKey = "",
                            report = result.value,
                            capabilities = result.value.capabilities,
                            connectionStatus = "Connected",
                            setupStep = OmbiSetupStep.Authentication,
                            accessMode = OmbiAccessMode.AllUsers,
                            message = null,
                        )
                    }
                }
                is IntegrationResult.Failure -> _state.update {
                    it.copy(
                        isTesting = false,
                        connectionStatus = "Connection failed",
                        message = result.message,
                    )
                }
            }
        }
    }

    fun finishSetup() {
        if (!requireAdmin()) return
        val snapshot = _state.value
        viewModelScope.launch {
            runCatching {
                ombiRepository.saveConfig(
                    accessMode = snapshot.accessMode,
                    baseUrl = snapshot.baseUrl,
                    apiKey = snapshot.apiKey,
                    apiAlias = snapshot.currentUserName,
                )
            }.onFailure { throwable ->
                _state.update { it.copy(message = throwable.message ?: "Could not save Ombi settings.") }
                return@launch
            }
            refreshConfig()
            ombiRepository.cleanupAccessRequests()
            _state.update { it.copy(mode = RequestsScreenMode.Requests, setupStep = OmbiSetupStep.Finish, message = "Requests are ready") }
            loadRequests()
        }
    }

    fun testConnection() {
        if (!requireAdmin()) return
        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, message = null) }
            when (val result = ombiRepository.testConnectionReport()) {
                is IntegrationResult.Success -> _state.update {
                    it.copy(
                        isTesting = false,
                        config = ombiRepository.config(),
                        report = result.value,
                        capabilities = result.value.capabilities,
                        connectionStatus = "Connected",
                        message = "Ombi connected",
                    )
                }
                is IntegrationResult.Failure -> _state.update {
                    it.copy(isTesting = false, config = ombiRepository.config(), connectionStatus = "Connection failed", message = result.message)
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (!requireAdmin()) return
        ombiRepository.setAccessMode(if (enabled) OmbiAccessMode.AllUsers else OmbiAccessMode.Disabled)
        refreshConfig()
    }

    fun requestOmbiAccess() {
        val snapshot = _state.value
        val userId = snapshot.currentUserId ?: return
        val userName = snapshot.currentUserName ?: "Vantafyn User"
        val request = ombiRepository.requestAccess(
            jellyfinUserId = userId,
            jellyfinUserName = userName,
            serverName = snapshot.currentServerName,
            note = null,
        )
        refreshConfig()
        _state.update {
            it.copy(
                currentUserMapping = ombiRepository.mappingFor(userId),
                message = if (request.status == OmbiAccessRequestStatus.Pending) {
                    "Your server admin has been notified."
                } else {
                    "Access has already been requested."
                },
            )
        }
    }

    fun markAccessRequestAccountCreated(userId: String) {
        if (!requireAdmin()) return
        ombiRepository.updateAccessRequest(userId, OmbiAccessRequestStatus.AccountCreated)
        refreshConfig()
    }

    fun markAccessRequestLinked(userId: String) {
        if (!requireAdmin()) return
        ombiRepository.updateAccessRequest(userId, OmbiAccessRequestStatus.Linked)
        refreshConfig()
    }

    fun dismissAccessRequest(userId: String) {
        if (!requireAdmin()) return
        ombiRepository.updateAccessRequest(userId, OmbiAccessRequestStatus.Dismissed)
        refreshConfig()
    }

    fun clearMapping(userId: String) {
        if (!requireAdmin()) return
        ombiRepository.clearMapping(userId)
        refreshConfig()
    }

    fun loginOmbiAccount() {
        val snapshot = _state.value
        val userId = snapshot.currentUserId ?: return
        val userName = snapshot.currentUserName ?: "Vantafyn User"
        viewModelScope.launch {
            _state.update { it.copy(isLinkingOmbi = true, message = null) }
            val companionSession = activeSession?.takeIf { snapshot.companionRequiresUserLogin }
            val result = if (companionSession != null) {
                ombiRepository.companionLoginUser(
                    session = companionSession,
                    username = snapshot.ombiUsername,
                    password = snapshot.ombiPassword,
                )
            } else {
                ombiRepository.loginUser(
                    jellyfinUserId = userId,
                    jellyfinUserName = userName,
                    username = snapshot.ombiUsername.ifBlank { snapshot.currentUserMapping?.ombiUserName.orEmpty() },
                    password = snapshot.ombiPassword,
                )
            }
            when (result) {
                is IntegrationResult.Success -> {
                    refreshConfig()
                    _state.update {
                        it.copy(
                            isLinkingOmbi = false,
                            ombiPassword = "",
                            ombiUserSession = result.value,
                            companionCapabilities = it.companionCapabilities?.copy(userLinked = true),
                            message = "Ombi linked as ${result.value.bestName}",
                        )
                    }
                    search()
                    loadRequests()
                    loadDiscovery()
                }
                is IntegrationResult.Failure -> _state.update {
                    it.copy(
                        isLinkingOmbi = false,
                        ombiPassword = "",
                        ombiUserSession = null,
                        message = "Ombi couldn't sign you in. Check your username and password.",
                    )
                }
            }
        }
    }

    fun unlinkOmbiAccount() {
        val userId = _state.value.currentUserId ?: return
        val companionSession = activeSession?.takeIf { _state.value.companionRequiresUserLogin }
        viewModelScope.launch {
            if (companionSession != null) {
                ombiRepository.companionLogoutUser(companionSession)
            } else {
                ombiRepository.unlinkUserSession(userId)
            }
            refreshConfig()
            _state.update {
                it.copy(
                    ombiUserSession = null,
                    ombiPassword = "",
                    companionCapabilities = it.companionCapabilities?.copy(userLinked = false),
                    discoveryRails = emptyList(),
                    searchResults = emptyList(),
                    message = "Ombi account unlinked from this device.",
                )
            }
        }
    }

    fun resetIntegration() {
        if (!requireAdmin()) return
        ombiRepository.clearConfig()
        _state.value = initialState().copy(
            currentUserId = _state.value.currentUserId,
            currentUserName = _state.value.currentUserName,
            currentServerName = _state.value.currentServerName,
            isJellyfinAdmin = _state.value.isJellyfinAdmin,
            message = "Ombi removed from this device",
        )
    }

    fun search() {
        if (!_state.value.canSearchAndRequest) {
            _state.update { it.copy(message = "Link your Ombi account before searching Requests.") }
            return
        }
        val query = _state.value.query.trim()
        if (query.length < 2) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val filter = _state.value.searchFilter
            _state.update { it.copy(isSearching = true, message = null, results = emptyList(), searchResults = emptyList(), activeRequestsView = RequestsView.Search) }
            val results = mutableListOf<MediaRequestSearchResult>()
            val failures = mutableListOf<String>()
            val userId = _state.value.currentUserId
            val perUser = _state.value.usesPerUserSession && userId != null
            val companionSession = activeSession?.takeIf { _state.value.companionCapabilities?.requestsReady == true }
            val discovery = if (companionSession != null) {
                ombiRepository.companionSearch(companionSession, query, filter.toCompanionFilter())
            } else {
                ombiRepository.searchDiscovery(query, if (perUser) userId else null)
            }
            when (discovery) {
                is IntegrationResult.Success -> {
                    val filtered = discovery.value.filter {
                        filter == RequestSearchFilter.All ||
                            (filter == RequestSearchFilter.Movies && it.mediaType == dev.vantafyn.core.ombi.RequestMediaType.Movie) ||
                            (filter == RequestSearchFilter.TvShows && it.mediaType == dev.vantafyn.core.ombi.RequestMediaType.Series)
                    }
                    _state.update {
                        it.copy(
                            isSearching = false,
                    searchResults = filtered,
                            availabilityMatches = availabilityMatchesFor(filtered),
                            message = null,
                        )
                    }
                    return@launch
                }
                is IntegrationResult.Failure -> failures += discovery.message
            }
            _state.update {
                it.copy(
                    isSearching = false,
                    results = results,
                    searchResults = results.map { result -> result.toRequestSummary() },
                    availabilityMatches = availabilityMatchesFor(results.map { result -> result.toRequestSummary() }),
                    message = if (results.isEmpty()) failures.firstOrNull() else null,
                )
            }
        }
    }

    fun selectRequestsView(value: RequestsView) {
        _state.update { it.copy(activeRequestsView = value, message = null) }
        if (value != RequestsView.Search && _state.value.discoveryRails.isEmpty() && _state.value.canSearchAndRequest) {
            loadDiscovery()
        }
        if (value == RequestsView.MyRequests && _state.value.requests.isEmpty() && _state.value.canSearchAndRequest) {
            loadRequests()
        }
    }

    fun openRequestItem(item: RequestMediaSummary) {
        if (!_state.value.canSearchAndRequest) return
        _state.update { it.copy(selectedRequestItem = item, selectedRequestDetail = null, isLoadingRequestDetail = true, requestDetailError = null, message = null) }
        viewModelScope.launch {
            val userId = _state.value.currentUserId?.takeIf { _state.value.usesPerUserSession }
            when (val result = ombiRepository.getRequestMediaDetail(item, userId)) {
                is IntegrationResult.Success -> _state.update {
                    it.copy(
                        selectedRequestItem = result.value.summary,
                        selectedRequestDetail = result.value,
                        isLoadingRequestDetail = false,
                        availabilityMatches = it.availabilityMatches + availabilityMatchesFor(listOf(result.value.summary)),
                    )
                }
                is IntegrationResult.Failure -> _state.update {
                    it.copy(isLoadingRequestDetail = false, requestDetailError = result.message)
                }
            }
        }
    }

    fun closeRequestItem() {
        _state.update { it.copy(selectedRequestItem = null, selectedRequestDetail = null, isLoadingRequestDetail = false, requestDetailError = null) }
    }

    fun request(item: RequestMediaSummary) {
        if (item.state != dev.vantafyn.core.ombi.RequestState.NotRequested) return
        if (!_state.value.canSearchAndRequest) {
            _state.update { it.copy(message = "This Ombi account is not linked for requests yet.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(activeRequestId = item.externalId, message = null) }
            val userId = _state.value.currentUserId
            val perUser = _state.value.usesPerUserSession && userId != null
            val result = when (item.mediaType) {
                dev.vantafyn.core.ombi.RequestMediaType.Movie -> {
                    val movieDbId = item.movieDbId?.takeIf { it.isNotBlank() }
                    if (movieDbId == null) {
                        _state.update {
                            it.copy(
                                activeRequestId = null,
                                message = "Ombi did not return a usable TMDb movie ID for this title.",
                            )
                        }
                        return@launch
                    }
                    val companionSession = activeSession?.takeIf { _state.value.companionCapabilities?.requestsReady == true }
                    when {
                        companionSession != null -> ombiRepository.companionRequestMovie(companionSession, movieDbId)
                        perUser -> ombiRepository.requestMovieForUser(userId, movieDbId)
                        else -> ombiRepository.requestMovie(movieDbId, _state.value.currentUserName)
                    }
                }
                dev.vantafyn.core.ombi.RequestMediaType.Series -> {
                    if (item.movieDbId.isNullOrBlank() && item.tvDbId.isNullOrBlank()) {
                        _state.update {
                            it.copy(
                                activeRequestId = null,
                                message = "Ombi did not return a usable TV identifier for this series.",
                            )
                        }
                        return@launch
                    }
                    val companionSession = activeSession?.takeIf { _state.value.companionCapabilities?.requestsReady == true }
                    if (companionSession != null) {
                        val providerId = item.tvDbId?.takeIf { it.isNotBlank() } ?: item.movieDbId.orEmpty()
                        ombiRepository.companionRequestSeries(companionSession, providerId)
                    } else if (perUser) {
                        ombiRepository.requestTvForUserByIds(userId, item.movieDbId, item.tvDbId, _state.value.tvRequestSelection)
                    } else {
                        ombiRepository.requestTvByIds(item.movieDbId, item.tvDbId, _state.value.currentUserName, _state.value.tvRequestSelection)
                    }
                }
            }
            when (result) {
                is IntegrationResult.Success -> {
                    _state.update { it.copy(activeRequestId = null, message = "Request sent") }
                    search()
                    loadRequests()
                    loadDiscovery()
                }
                is IntegrationResult.Failure -> _state.update { it.copy(activeRequestId = null, message = result.message) }
            }
        }
    }

    fun onTvRequestSelectionChanged(value: OmbiTvRequestSelection) {
        _state.update { it.copy(tvRequestSelection = value, message = null) }
    }

    fun request(item: MediaRequestSearchResult) {
        if (!_state.value.canSearchAndRequest) {
            _state.update { it.copy(message = "This Ombi account is not linked for requests yet.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(activeRequestId = item.providerId, message = null) }
            val userId = _state.value.currentUserId
            val perUser = _state.value.usesPerUserSession && userId != null
            val companionSession = activeSession?.takeIf { _state.value.companionCapabilities?.requestsReady == true }
            val result = when (item.type) {
                MediaRequestType.Movie -> when {
                    companionSession != null -> ombiRepository.companionRequestMovie(companionSession, item.providerId)
                    perUser -> ombiRepository.requestMovieForUser(userId, item.providerId)
                    else -> ombiRepository.requestMovie(item.providerId, _state.value.currentUserName)
                }
                MediaRequestType.Tv -> if (companionSession != null) {
                    ombiRepository.companionRequestSeries(companionSession, item.providerId)
                } else if (perUser) {
                    ombiRepository.requestTvForUser(userId, item.providerId, _state.value.tvRequestSelection)
                } else {
                    ombiRepository.requestTv(item.providerId, _state.value.currentUserName, _state.value.tvRequestSelection)
                }
            }
            when (result) {
                is IntegrationResult.Success -> {
                    _state.update { it.copy(activeRequestId = null, message = "Request sent") }
                    search()
                    loadRequests()
                    loadDiscovery()
                }
                is IntegrationResult.Failure -> _state.update { it.copy(activeRequestId = null, message = result.message) }
            }
        }
    }

    fun loadRequests() {
        if (!_state.value.canSearchAndRequest && !_state.value.isJellyfinAdmin) return
        if (_state.value.companionCapabilities?.requestsReady == true && !ombiRepository.hasApiKey()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRequests = true) }
            val userId = _state.value.currentUserId
            val result = if (_state.value.usesPerUserSession && userId != null) {
                ombiRepository.getLinkedUserRequests(userId)
            } else if (_state.value.isJellyfinAdmin) {
                ombiRepository.getAllRequests()
            } else {
                ombiRepository.getUserRequests(_state.value.currentUserName)
            }
            when (result) {
                is IntegrationResult.Success -> _state.update { it.copy(isLoadingRequests = false, requests = result.value) }
                is IntegrationResult.Failure -> _state.update { it.copy(isLoadingRequests = false, connectionStatus = "Connection failed", message = result.message) }
            }
        }
    }

    fun loadDiscovery() {
        if (!_state.value.canSearchAndRequest) return
        if (_state.value.companionCapabilities?.requestsReady == true && !ombiRepository.hasApiKey()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingDiscovery = true, discoveryError = null) }
            val snapshot = _state.value
            val userId = snapshot.currentUserId?.takeIf { snapshot.usesPerUserSession }
            when (val result = ombiRepository.getDiscoveryRails(userId, snapshot.currentUserName, snapshot.isJellyfinAdmin)) {
                is IntegrationResult.Success -> _state.update {
                    it.copy(
                        isLoadingDiscovery = false,
                        discoveryRails = result.value,
                        availabilityMatches = availabilityMatchesFor(result.value.flatMap { rail -> rail.items }),
                        userCapabilities = ombiRepository.cachedUserCapabilities(snapshot.currentUserId),
                        discoveryError = null,
                    )
                }
                is IntegrationResult.Failure -> _state.update {
                    it.copy(isLoadingDiscovery = false, discoveryError = result.message)
                }
            }
        }
    }

    fun refreshAvailabilityIndex(silent: Boolean = false) {
        val session = activeSession ?: return
        viewModelScope.launch {
            if (!silent) _state.update { it.copy(isRefreshingAvailability = true, availabilityMessage = null) }
            when (val result = jellyfinProvider.libraryRepository.buildAvailabilityIndex(session)) {
                is JellyfinResult.Success -> {
                    val allItems = _state.value.discoveryRails.flatMap { it.items } + _state.value.searchResults + listOfNotNull(_state.value.selectedRequestDetail?.summary)
                    _state.update {
                        it.copy(
                            availabilityIndex = result.value,
                            availabilityMatches = availabilityMatchesFor(allItems, result.value),
                            isRefreshingAvailability = false,
                            availabilityMessage = "Availability index refreshed",
                        )
                    }
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isRefreshingAvailability = false,
                        availabilityMessage = if (silent) it.availabilityMessage else "Jellyfin availability could not be verified.",
                    )
                }
            }
        }
    }

    fun openAvailableItem(item: RequestMediaSummary, onOpenMedia: (java.util.UUID) -> Unit) {
        val match = _state.value.availabilityMatches[item.availabilityKey()] ?: return
        onOpenMedia(match.itemId)
    }

    private fun refreshConfig() {
        val config = ombiRepository.config()
        val hasKey = ombiRepository.hasApiKey()
        _state.update {
            it.copy(
                config = config,
                hasApiKey = hasKey,
                baseUrl = config.baseUrl,
                accessMode = config.accessMode,
                identityMode = config.identityMode,
                capabilities = config.capabilities,
                connectionStatus = config.statusLabel(hasKey),
                accessRequests = ombiRepository.accessRequests(),
                userMappings = ombiRepository.userMappings(),
                currentUserMapping = it.currentUserId?.let(ombiRepository::mappingFor),
                ombiUserSession = it.currentUserId?.let(ombiRepository::userSession),
                userCapabilities = ombiRepository.cachedUserCapabilities(it.currentUserId),
            )
        }
    }

    private fun validateLinkedAccount() {
        val userId = _state.value.currentUserId ?: return
        if (!_state.value.canUseRequests) return
        viewModelScope.launch {
            _state.update { it.copy(isValidatingOmbiSession = true) }
            val companionSession = activeSession?.takeIf { _state.value.companionRequiresUserLogin }
            val result = if (companionSession != null) {
                ombiRepository.companionUserSession(companionSession)
            } else {
                ombiRepository.validateUserSession(userId)
            }
            when (result) {
                is IntegrationResult.Success -> {
                    refreshConfig()
                    _state.update {
                        it.copy(
                            isValidatingOmbiSession = false,
                            ombiUserSession = result.value,
                            companionCapabilities = it.companionCapabilities?.copy(userLinked = true),
                            connectionStatus = "Connected",
                        )
                    }
                    loadRequests()
                    loadDiscovery()
                }
                is IntegrationResult.Failure -> {
                    refreshConfig()
                    _state.update {
                        it.copy(
                            isValidatingOmbiSession = false,
                            ombiUserSession = null,
                            companionCapabilities = it.companionCapabilities?.copy(userLinked = false),
                            message = if (result.reason == dev.vantafyn.core.integrations.IntegrationFailureReason.Unauthorized) {
                                "Sign in to Ombi again to continue requesting."
                            } else {
                                result.message
                            },
                        )
                    }
                }
            }
        }
    }

    private fun initialState(): RequestsUiState {
        val config = ombiRepository.config()
        val hasKey = ombiRepository.hasApiKey()
        return RequestsUiState(
            config = config,
            hasApiKey = hasKey,
            baseUrl = config.baseUrl,
            accessMode = config.accessMode,
            identityMode = config.identityMode,
            capabilities = config.capabilities,
            connectionStatus = config.statusLabel(hasKey),
            accessRequests = ombiRepository.accessRequests(),
            userMappings = ombiRepository.userMappings(),
        )
    }

    private fun requireAdmin(): Boolean {
        if (_state.value.isJellyfinAdmin) return true
        _state.update {
            it.copy(
                mode = RequestsScreenMode.Requests,
                message = "Requests settings are managed by your server admin.",
            )
        }
        return false
    }
}

data class RequestsUiState(
    val config: OmbiConfig = OmbiConfig(),
    val hasApiKey: Boolean = false,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val currentServerName: String? = null,
    val isJellyfinAdmin: Boolean = false,
    val mode: RequestsScreenMode = RequestsScreenMode.Requests,
    val setupStep: OmbiSetupStep = OmbiSetupStep.Connect,
    val baseUrl: String = config.baseUrl,
    val apiKey: String = "",
    val accessMode: OmbiAccessMode = config.accessMode,
    val identityMode: OmbiIdentityMode = config.identityMode,
    val connectionStatus: String = "Not configured",
    val report: OmbiConnectionReport? = null,
    val capabilities: OmbiCapabilities = config.capabilities,
    val accessRequests: List<OmbiAccessRequest> = emptyList(),
    val userMappings: List<OmbiUserMapping> = emptyList(),
    val currentUserMapping: OmbiUserMapping? = null,
    val ombiUserSession: OmbiUserSession? = null,
    val ombiUserMatch: OmbiUserMatch = OmbiUserMatch(OmbiUserMatchState.NotChecked),
    val ombiUsername: String = "",
    val ombiPassword: String = "",
    val isLinkingOmbi: Boolean = false,
    val isValidatingOmbiSession: Boolean = false,
    val query: String = "",
    val searchFilter: RequestSearchFilter = RequestSearchFilter.All,
    val isTesting: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingRequests: Boolean = false,
    val results: List<MediaRequestSearchResult> = emptyList(),
    val requests: List<MediaRequestItem> = emptyList(),
    val activeRequestId: String? = null,
    val message: String? = null,
    val activeRequestsView: RequestsView = RequestsView.Discover,
    val discoveryRails: List<OmbiDiscoverRail> = emptyList(),
    val isLoadingDiscovery: Boolean = false,
    val discoveryError: String? = null,
    val searchResults: List<RequestMediaSummary> = emptyList(),
    val selectedRequestItem: RequestMediaSummary? = null,
    val selectedRequestDetail: RequestMediaDetail? = null,
    val isLoadingRequestDetail: Boolean = false,
    val requestDetailError: String? = null,
    val userCapabilities: OmbiUserCapabilities = OmbiUserCapabilities(
        canRequestMovies = true,
        canRequestSeries = true,
        canRequestSeasons = true,
        canRequestEpisodes = false,
        canManageOwnRequests = true,
        canViewOtherRequests = false,
        canManageRequests = false,
        moviesAutoApproved = false,
        seriesAutoApproved = false,
        remainingMovieRequests = null,
        remainingSeriesRequests = null,
    ),
    val availabilityIndex: JellyfinAvailabilityIndex? = null,
    val availabilityMatches: Map<String, JellyfinAvailabilityMatch> = emptyMap(),
    val isRefreshingAvailability: Boolean = false,
    val availabilityMessage: String? = null,
    val tvRequestSelection: OmbiTvRequestSelection = OmbiTvRequestSelection.AllSeasons,
    val companionCapabilities: VantafynCompanionCapabilities? = null,
) {
    val usesCompanionRequests: Boolean get() = companionCapabilities?.requestsReady == true
    val companionRequiresUserLogin: Boolean get() = usesCompanionRequests && companionCapabilities?.requiresUserLogin == true
    val isConfigured: Boolean get() = usesCompanionRequests || config.isConfigured && hasApiKey
    val canUseRequests: Boolean get() = usesCompanionRequests || canUseRequests(config, hasApiKey, isJellyfinAdmin)
    val pendingAccessRequestCount: Int get() = accessRequests.count { it.status == OmbiAccessRequestStatus.Pending }
    val hasRequestedAccess: Boolean
        get() = currentUserMapping?.state == OmbiLinkedAccountState.AccessRequested ||
            accessRequests.any {
                it.jellyfinUserId == currentUserId &&
                    it.status in setOf(OmbiAccessRequestStatus.Pending, OmbiAccessRequestStatus.Seen)
            }
    val canSearchAndRequest: Boolean
        get() = canUseRequests && (
            (usesCompanionRequests && (!companionRequiresUserLogin || ombiUserSession != null)) ||
                identityMode == OmbiIdentityMode.SharedApiKey ||
                (currentUserMapping?.state == OmbiLinkedAccountState.Linked && ombiUserSession != null)
            )
    val canShowOmbiLogin: Boolean
        get() = (companionRequiresUserLogin || identityMode == OmbiIdentityMode.PerUserAccount) &&
            !hasRequestedAccess &&
            ombiUserSession == null &&
            currentUserMapping?.state != OmbiLinkedAccountState.Disabled
    val usesPerUserSession: Boolean
        get() = (companionRequiresUserLogin || identityMode == OmbiIdentityMode.PerUserAccount) && ombiUserSession != null
}

private fun canUseRequests(config: OmbiConfig, hasApiKey: Boolean, isAdmin: Boolean): Boolean =
    hasApiKey && config.isAvailableFor(isAdmin)

private fun RequestSearchFilter.toCompanionFilter(): RequestSearchFilterValue =
    when (this) {
        RequestSearchFilter.All -> RequestSearchFilterValue.All
        RequestSearchFilter.Movies -> RequestSearchFilterValue.Movies
        RequestSearchFilter.TvShows -> RequestSearchFilterValue.Series
    }

private fun RequestsViewModel.availabilityMatchesFor(
    items: List<RequestMediaSummary>,
    index: JellyfinAvailabilityIndex? = state.value.availabilityIndex,
): Map<String, JellyfinAvailabilityMatch> =
    index?.let {
        items.mapNotNull { item -> it.find(item.providerIds(), item.jellyfinItemTypes())?.let { match -> item.availabilityKey() to match } }.toMap()
    }.orEmpty()

private fun RequestMediaSummary.providerIds(): Map<String, String> =
    buildMap {
        movieDbId?.takeIf { it.isNotBlank() }?.let {
            put("Tmdb", it)
            put("TheMovieDb", it)
        }
        tvDbId?.takeIf { it.isNotBlank() }?.let { put("Tvdb", it) }
        imdbId?.takeIf { it.isNotBlank() }?.let { put("Imdb", it) }
    }

internal fun RequestMediaSummary.availabilityKey(): String =
    when {
        !movieDbId.isNullOrBlank() -> "${mediaType.name}:tmdb:$movieDbId"
        !tvDbId.isNullOrBlank() -> "${mediaType.name}:tvdb:$tvDbId"
        !imdbId.isNullOrBlank() -> "${mediaType.name}:imdb:$imdbId"
        else -> "${mediaType.name}:external:$externalId"
    }

private fun RequestMediaSummary.jellyfinItemTypes(): Set<String> =
    when (mediaType) {
        dev.vantafyn.core.ombi.RequestMediaType.Movie -> setOf("Movie")
        dev.vantafyn.core.ombi.RequestMediaType.Series -> setOf("Series")
    }

enum class RequestsScreenMode {
    Requests,
    Setup,
    Manage,
    Health,
}

enum class RequestsView {
    Discover,
    Search,
    MyRequests,
}

enum class OmbiSetupStep {
    Connect,
    Authentication,
    Test,
    Access,
    Finish,
}

enum class RequestSearchFilter {
    All,
    Movies,
    TvShows,
}

private fun OmbiConfig.statusLabel(hasApiKey: Boolean): String =
    when {
        !isConfigured || !hasApiKey -> "Not configured"
        accessMode == OmbiAccessMode.Disabled -> "Disabled"
        lastFailureMessage != null -> "Connection failed"
        lastSuccessfulConnectionAt != null -> "Connected"
        else -> "Configured"
    }

private fun MediaRequestSearchResult.toRequestSummary(): RequestMediaSummary =
    RequestMediaSummary(
        externalId = providerId,
        movieDbId = if (type == MediaRequestType.Movie) providerId else null,
        tvDbId = if (type == MediaRequestType.Tv) providerId else null,
        imdbId = null,
        mediaType = if (type == MediaRequestType.Movie) dev.vantafyn.core.ombi.RequestMediaType.Movie else dev.vantafyn.core.ombi.RequestMediaType.Series,
        title = title,
        originalTitle = null,
        year = year,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        overview = overview,
        rating = null,
        state = when (status) {
            dev.vantafyn.core.integrations.MediaRequestStatus.Available -> dev.vantafyn.core.ombi.RequestState.Available
            dev.vantafyn.core.integrations.MediaRequestStatus.Pending -> dev.vantafyn.core.ombi.RequestState.PendingApproval
            dev.vantafyn.core.integrations.MediaRequestStatus.Approved -> dev.vantafyn.core.ombi.RequestState.Approved
            dev.vantafyn.core.integrations.MediaRequestStatus.Processing -> dev.vantafyn.core.ombi.RequestState.Processing
            dev.vantafyn.core.integrations.MediaRequestStatus.Denied -> dev.vantafyn.core.ombi.RequestState.Declined
            dev.vantafyn.core.integrations.MediaRequestStatus.NotRequested -> dev.vantafyn.core.ombi.RequestState.NotRequested
            dev.vantafyn.core.integrations.MediaRequestStatus.Unknown -> dev.vantafyn.core.ombi.RequestState.Unknown
        },
        requestId = null,
        isAvailableInJellyfin = status == dev.vantafyn.core.integrations.MediaRequestStatus.Available,
        availableSeasonCount = null,
        totalSeasonCount = null,
    )

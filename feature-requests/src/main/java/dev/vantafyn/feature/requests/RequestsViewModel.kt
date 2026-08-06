package dev.vantafyn.feature.requests

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vantafyn.core.integrations.IntegrationResult
import dev.vantafyn.core.integrations.MediaRequestItem
import dev.vantafyn.core.integrations.MediaRequestSearchResult
import dev.vantafyn.core.integrations.MediaRequestType
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ombi.OmbiAccessMode
import dev.vantafyn.core.ombi.OmbiAccessRequest
import dev.vantafyn.core.ombi.OmbiAccessRequestStatus
import dev.vantafyn.core.ombi.OmbiCapabilities
import dev.vantafyn.core.ombi.OmbiConfig
import dev.vantafyn.core.ombi.OmbiConnectionReport
import dev.vantafyn.core.ombi.OmbiIdentityMode
import dev.vantafyn.core.ombi.OmbiLinkedAccountState
import dev.vantafyn.core.ombi.OmbiRepository
import dev.vantafyn.core.ombi.OmbiUserMapping
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RequestsViewModel(application: Application) : AndroidViewModel(application) {
    private val ombiRepository = OmbiRepository(application)
    private var searchJob: Job? = null

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<RequestsUiState> = _state.asStateFlow()

    fun bindSession(session: JellyfinSession?) {
        val config = ombiRepository.config()
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
            )
        }
        if (config.isEnabledForAdmins && ombiRepository.hasApiKey()) {
            loadRequests()
        }
    }

    fun startSetup() {
        _state.update {
            it.copy(
                mode = RequestsScreenMode.Setup,
                setupStep = OmbiSetupStep.Connect,
                message = null,
            )
        }
    }

    fun manageOmbi() {
        refreshConfig()
        _state.update { it.copy(mode = RequestsScreenMode.Manage, message = null) }
    }

    fun showRequests() {
        _state.update { it.copy(mode = RequestsScreenMode.Requests, message = null) }
    }

    fun onBaseUrlChanged(value: String) {
        _state.update { it.copy(baseUrl = value, message = null) }
    }

    fun onApiKeyChanged(value: String) {
        _state.update { it.copy(apiKey = value, message = null) }
    }

    fun onQueryChanged(value: String) {
        _state.update { it.copy(query = value, message = null) }
    }

    fun onSearchFilterChanged(value: RequestSearchFilter) {
        _state.update { it.copy(searchFilter = value, message = null) }
    }

    fun onAccessModeChanged(value: OmbiAccessMode) {
        _state.update { it.copy(accessMode = value, message = null) }
    }

    fun onIdentityModeChanged(value: OmbiIdentityMode) {
        ombiRepository.setIdentityMode(value)
        refreshConfig()
        _state.update {
            it.copy(
                identityMode = value,
                message = if (value == OmbiIdentityMode.PerUserAccount) {
                    "Per-user account mode is enabled for access tracking. Ombi credential linking is not exposed until token support is confirmed."
                } else {
                    "Shared request account mode enabled."
                },
            )
        }
    }

    fun nextSetupStep() {
        val snapshot = _state.value
        _state.update {
            it.copy(
                setupStep = when (snapshot.setupStep) {
                    OmbiSetupStep.Connect -> OmbiSetupStep.Authentication
                    OmbiSetupStep.Authentication -> OmbiSetupStep.Test
                    OmbiSetupStep.Test -> OmbiSetupStep.Access
                    OmbiSetupStep.Access -> OmbiSetupStep.Finish
                    OmbiSetupStep.Finish -> OmbiSetupStep.Finish
                },
                message = null,
            )
        }
    }

    fun previousSetupStep() {
        val snapshot = _state.value
        _state.update {
            it.copy(
                setupStep = when (snapshot.setupStep) {
                    OmbiSetupStep.Connect -> OmbiSetupStep.Connect
                    OmbiSetupStep.Authentication -> OmbiSetupStep.Connect
                    OmbiSetupStep.Test -> OmbiSetupStep.Authentication
                    OmbiSetupStep.Access -> OmbiSetupStep.Test
                    OmbiSetupStep.Finish -> OmbiSetupStep.Access
                },
                message = null,
            )
        }
    }

    fun saveDraftAndTest() {
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
                            setupStep = OmbiSetupStep.Access,
                            accessMode = OmbiAccessMode.AllUsers,
                            message = "Ombi connected",
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
            _state.update { it.copy(mode = RequestsScreenMode.Requests, setupStep = OmbiSetupStep.Finish, message = "Requests enabled") }
            loadRequests()
        }
    }

    fun testConnection() {
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
                    "Your request will appear for admins on this device/server setup."
                } else {
                    "Access has already been requested."
                },
            )
        }
    }

    fun markAccessRequestAccountCreated(userId: String) {
        ombiRepository.updateAccessRequest(userId, OmbiAccessRequestStatus.AccountCreated)
        refreshConfig()
    }

    fun markAccessRequestLinked(userId: String) {
        ombiRepository.updateAccessRequest(userId, OmbiAccessRequestStatus.Linked)
        refreshConfig()
    }

    fun dismissAccessRequest(userId: String) {
        ombiRepository.updateAccessRequest(userId, OmbiAccessRequestStatus.Dismissed)
        refreshConfig()
    }

    fun clearMapping(userId: String) {
        ombiRepository.clearMapping(userId)
        refreshConfig()
    }

    fun resetIntegration() {
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
            _state.update { it.copy(message = "Linking Ombi accounts is not available in this build yet.") }
            return
        }
        val query = _state.value.query.trim()
        if (query.length < 2) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val filter = _state.value.searchFilter
            _state.update { it.copy(isSearching = true, message = null, results = emptyList()) }
            val results = mutableListOf<MediaRequestSearchResult>()
            val failures = mutableListOf<String>()
            if (filter == RequestSearchFilter.All || filter == RequestSearchFilter.Movies) {
                when (val result = ombiRepository.searchMovie(query)) {
                    is IntegrationResult.Success -> results += result.value
                    is IntegrationResult.Failure -> failures += result.message
                }
            }
            if (filter == RequestSearchFilter.All || filter == RequestSearchFilter.TvShows) {
                when (val result = ombiRepository.searchTv(query)) {
                    is IntegrationResult.Success -> results += result.value
                    is IntegrationResult.Failure -> failures += result.message
                }
            }
            _state.update {
                it.copy(
                    isSearching = false,
                    results = results,
                    message = if (results.isEmpty()) failures.firstOrNull() else null,
                )
            }
        }
    }

    fun request(item: MediaRequestSearchResult) {
        if (!_state.value.canSearchAndRequest) {
            _state.update { it.copy(message = "This Ombi account is not linked for requests yet.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(activeRequestId = item.providerId, message = null) }
            val result = when (item.type) {
                MediaRequestType.Movie -> ombiRepository.requestMovie(item.providerId, _state.value.currentUserName)
                MediaRequestType.Tv -> ombiRepository.requestTv(item.providerId, _state.value.currentUserName)
            }
            when (result) {
                is IntegrationResult.Success -> {
                    _state.update { it.copy(activeRequestId = null, message = "Request sent") }
                    search()
                    loadRequests()
                }
                is IntegrationResult.Failure -> _state.update { it.copy(activeRequestId = null, message = result.message) }
            }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRequests = true) }
            val result = if (_state.value.isJellyfinAdmin) {
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
            )
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
    val query: String = "",
    val searchFilter: RequestSearchFilter = RequestSearchFilter.All,
    val isTesting: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingRequests: Boolean = false,
    val results: List<MediaRequestSearchResult> = emptyList(),
    val requests: List<MediaRequestItem> = emptyList(),
    val activeRequestId: String? = null,
    val message: String? = null,
) {
    val isConfigured: Boolean get() = config.isConfigured && hasApiKey
    val canUseRequests: Boolean get() = isConfigured && config.isEnabledForAdmins
    val pendingAccessRequestCount: Int get() = accessRequests.count { it.status == OmbiAccessRequestStatus.Pending }
    val hasRequestedAccess: Boolean
        get() = currentUserMapping?.state == OmbiLinkedAccountState.AccessRequested ||
            accessRequests.any {
                it.jellyfinUserId == currentUserId &&
                    it.status != OmbiAccessRequestStatus.Dismissed &&
                    it.status != OmbiAccessRequestStatus.Linked
            }
    val canSearchAndRequest: Boolean
        get() = canUseRequests && (
            identityMode == OmbiIdentityMode.SharedApiKey ||
                currentUserMapping?.state == OmbiLinkedAccountState.Linked
            )
}

enum class RequestsScreenMode {
    Requests,
    Setup,
    Manage,
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

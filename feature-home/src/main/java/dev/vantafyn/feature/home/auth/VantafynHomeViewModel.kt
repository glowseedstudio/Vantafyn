package dev.vantafyn.feature.home.auth

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vantafyn.core.jellyfin.JellyfinAuthRepository
import dev.vantafyn.core.jellyfin.JellyfinAdminOverview
import dev.vantafyn.core.jellyfin.JellyfinAdminRepository
import dev.vantafyn.core.jellyfin.JellyfinFavoritesRepository
import dev.vantafyn.core.jellyfin.JellyfinHome
import dev.vantafyn.core.jellyfin.JellyfinHomeRepository
import dev.vantafyn.core.jellyfin.JellyfinEpisode
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.jellyfin.JellyfinLibraryRepository
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinMediaItem
import dev.vantafyn.core.jellyfin.JellyfinMediaRepository
import dev.vantafyn.core.jellyfin.JellyfinPlaybackInfo
import dev.vantafyn.core.jellyfin.JellyfinPlaybackRepository
import dev.vantafyn.core.jellyfin.JellyfinPublicUser
import dev.vantafyn.core.jellyfin.JellyfinQuickConnectRepository
import dev.vantafyn.core.jellyfin.JellyfinQuickConnectSession
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinRestoreFailureReason
import dev.vantafyn.core.jellyfin.JellyfinSearchRepository
import dev.vantafyn.core.jellyfin.JellyfinSearchResult
import dev.vantafyn.core.jellyfin.JellyfinServerConfig
import dev.vantafyn.core.jellyfin.JellyfinSessionRestoreFailure
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences
import dev.vantafyn.core.jellyfin.JellyfinAdminUserDetail
import dev.vantafyn.core.jellyfin.JellyfinUserPreferencesRepository
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.media.VantafynAudioTrack
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.VantafynPlaybackItem
import dev.vantafyn.core.media.VantafynMusicStopReason
import dev.vantafyn.core.media.VantafynSubtitleTrack
import dev.vantafyn.core.ombi.OmbiRepository
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VantafynHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repositories = JellyfinRepositoryProvider(application)
    private val authRepository: JellyfinAuthRepository = repositories.authRepository
    private val libraryRepository: JellyfinLibraryRepository = repositories.libraryRepository
    private val homeRepository: JellyfinHomeRepository = repositories.homeRepository
    private val quickConnectRepository: JellyfinQuickConnectRepository = repositories.quickConnectRepository
    private val mediaRepository: JellyfinMediaRepository = repositories.mediaRepository
    private val searchRepository: JellyfinSearchRepository = repositories.searchRepository
    private val favoritesRepository: JellyfinFavoritesRepository = repositories.favoritesRepository
    private val adminRepository: JellyfinAdminRepository = repositories.adminRepository
    private val userPreferencesRepository: JellyfinUserPreferencesRepository = repositories.userPreferencesRepository
    private val playbackRepository: JellyfinPlaybackRepository = repositories.playbackRepository
    private val ombiRepository = OmbiRepository(application)
    private val homeLayoutStorage = application.getSharedPreferences("vantafyn_home_layout", Context.MODE_PRIVATE)
    private val appPreferences = application.getSharedPreferences("vantafyn_app_preferences", Context.MODE_PRIVATE)
    private var searchJob: Job? = null
    private var quickConnectJob: Job? = null

    private val _state = MutableStateFlow(VantafynHomeUiState())
    val state: StateFlow<VantafynHomeUiState> = _state.asStateFlow()

    init {
        loadSavedProfiles()
        refreshOmbiRequestsAvailability()
    }

    fun onServerUrlChanged(value: String) {
        _state.update { it.copy(serverUrl = value, errorMessage = null) }
    }

    fun onUsernameChanged(value: String) {
        _state.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun continueFromWelcome() {
        _state.update { it.copy(step = VantafynSetupStep.ConnectServer, errorMessage = null) }
    }

    fun addProfile() {
        val snapshot = _state.value
        val existingServer = snapshot.server ?: snapshot.savedProfiles.maxByOrNull { it.lastUsedAt }?.let {
            JellyfinServerConfig(url = it.serverUrl, name = it.serverName, localId = it.serverRef)
        }
        if (existingServer != null) {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        step = VantafynSetupStep.ProfilePicker,
                        isLoading = true,
                        server = existingServer,
                        serverUrl = existingServer.url,
                        username = "",
                        password = "",
                        publicUsers = emptyList(),
                        manageProfiles = false,
                        pendingRemoval = null,
                        errorMessage = null,
                    )
                }
                val publicUsers = when (val result = authRepository.publicUsers(existingServer)) {
                    is JellyfinResult.Success -> result.value
                    is JellyfinResult.Failure -> emptyList()
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        publicUsers = publicUsers,
                        step = if (publicUsers.isNotEmpty()) VantafynSetupStep.ProfilePicker else VantafynSetupStep.Login,
                    )
                }
            }
            return
        }
        _state.update {
            it.copy(
                step = VantafynSetupStep.Welcome,
                serverUrl = "",
                username = "",
                password = "",
                server = null,
                publicUsers = emptyList(),
                manageProfiles = false,
                pendingRemoval = null,
                errorMessage = null,
            )
        }
    }

    fun showProfilePicker() {
        MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.ProfileSwitch)
        viewModelScope.launch {
            _state.update {
                it.copy(
                    step = VantafynSetupStep.ProfilePicker,
                    savedProfiles = authRepository.savedProfiles(),
                    session = null,
                    libraries = emptyList(),
                    home = null,
                    publicUsers = emptyList(),
                    manageProfiles = false,
                    pendingRemoval = null,
                    restoreFailureProfile = null,
                    restoreFailureReason = null,
                    restoreFailureMessage = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun continueToLogin() {
        _state.update {
            it.copy(
                step = if (it.publicUsers.isNotEmpty()) VantafynSetupStep.ProfilePicker else VantafynSetupStep.Login,
                errorMessage = null,
                manageProfiles = false,
                pendingRemoval = null,
            )
        }
    }

    fun connectToServer() {
        val url = _state.value.serverUrl
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.testServer(url)) {
                is JellyfinResult.Success -> {
                    val publicUsers = when (val usersResult = authRepository.publicUsers(result.value)) {
                        is JellyfinResult.Success -> usersResult.value
                        is JellyfinResult.Failure -> emptyList()
                    }
                    _state.update {
                        it.copy(
                            step = VantafynSetupStep.ServerConfirm,
                            isLoading = false,
                            server = result.value,
                            serverUrl = result.value.url,
                            publicUsers = publicUsers,
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun login() {
        val snapshot = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = authRepository.login(
                    serverUrl = snapshot.serverUrl,
                    username = snapshot.username,
                    password = snapshot.password,
                )
            ) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            password = "",
                            session = result.value,
                            server = result.value.server,
                            step = VantafynSetupStep.Home,
                            homeLayout = readHomeLayout(result.value.profileId),
                            themeMusicEnabled = readThemeMusicEnabled(result.value.profileId),
                            themeMusicVolume = readThemeMusicVolume(result.value.profileId),
                            selectedBackground = readSelectedBackground(result.value.profileId),
                            configuredSmartRows = readSmartRows(result.value.profileId),
                        )
                    }
                    refreshSavedProfiles()
                    loadLibraries(result.value)
                    loadFavorites(result.value)
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun startQuickConnect() {
        val server = _state.value.server ?: return
        quickConnectJob?.cancel()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    step = VantafynSetupStep.QuickConnect,
                    isLoading = true,
                    quickConnectSession = null,
                    errorMessage = null,
                )
            }
            when (val result = quickConnectRepository.initiate(server)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            quickConnectSession = result.value,
                            quickConnectMessage = "Enter this code in Jellyfin to approve Vantafyn.",
                        )
                    }
                    pollQuickConnect(result.value)
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun retryQuickConnect() {
        startQuickConnect()
    }

    fun cancelQuickConnect() {
        quickConnectJob?.cancel()
        quickConnectJob = null
        _state.update {
            it.copy(
                step = VantafynSetupStep.Login,
                isLoading = false,
                quickConnectSession = null,
                quickConnectMessage = null,
                errorMessage = null,
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.Logout)
            authRepository.logout()
            _state.value = VantafynHomeUiState(step = VantafynSetupStep.Welcome)
        }
    }

    fun confirmCurrentProfileLogout() {
        _state.update { it.copy(confirmLogout = true) }
    }

    fun cancelCurrentProfileLogout() {
        _state.update { it.copy(confirmLogout = false) }
    }

    fun logoutCurrentProfile() {
        val profileId = _state.value.session?.profileId ?: return
        viewModelScope.launch {
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.ProfileSwitch)
            authRepository.removeProfile(profileId)
            val profiles = authRepository.savedProfiles()
            _state.value = VantafynHomeUiState(
                step = if (profiles.isEmpty()) VantafynSetupStep.Welcome else VantafynSetupStep.ProfilePicker,
                savedProfiles = profiles,
            )
        }
    }

    fun selectProfile(profile: SavedProfile) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    step = VantafynSetupStep.ProfilePicker,
                    selectedProfileId = profile.id,
                    isLoading = true,
                    errorMessage = null,
                )
            }
            when (val result = authRepository.restoreSession(profile.id)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            step = VantafynSetupStep.Home,
                            selectedProfileId = profile.id,
                            isLoading = false,
                            session = result.value,
                            server = result.value.server,
                            serverUrl = result.value.server.url,
                            username = result.value.user.name,
                            password = "",
                            homeLayout = readHomeLayout(result.value.profileId),
                            themeMusicEnabled = readThemeMusicEnabled(result.value.profileId),
                            themeMusicVolume = readThemeMusicVolume(result.value.profileId),
                            selectedBackground = readSelectedBackground(result.value.profileId),
                            configuredSmartRows = readSmartRows(result.value.profileId),
                        )
                    }
                    refreshSavedProfiles()
                    loadLibraries(result.value)
                    loadFavorites(result.value)
                }
                is JellyfinResult.Failure -> {
                    handleRestoreFailure(profile, result)
                }
            }
        }
    }

    fun retryFailedRestore() {
        val profile = _state.value.restoreFailureProfile ?: return
        selectProfile(profile)
    }

    fun saveRecoveryServerAddress() {
        val snapshot = _state.value
        val profile = snapshot.restoreFailureProfile ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.updateSavedServerUrl(profile.id, snapshot.serverUrl)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            step = VantafynSetupStep.Home,
                            selectedProfileId = profile.id,
                            isLoading = false,
                            session = result.value,
                            server = result.value.server,
                            serverUrl = result.value.server.url,
                            username = result.value.user.name,
                            password = "",
                            restoreFailureProfile = null,
                            restoreFailureReason = null,
                            restoreFailureMessage = null,
                            failedProfileIds = it.failedProfileIds - profile.id,
                            homeLayout = readHomeLayout(result.value.profileId),
                            themeMusicEnabled = readThemeMusicEnabled(result.value.profileId),
                            themeMusicVolume = readThemeMusicVolume(result.value.profileId),
                            selectedBackground = readSelectedBackground(result.value.profileId),
                            configuredSmartRows = readSmartRows(result.value.profileId),
                        )
                    }
                    refreshSavedProfiles()
                    loadLibraries(result.value)
                    loadFavorites(result.value)
                }
                is JellyfinResult.Failure -> {
                    val reason = (result.cause as? JellyfinSessionRestoreFailure)?.reason
                    if (reason == JellyfinRestoreFailureReason.AuthExpired || reason == JellyfinRestoreFailureReason.Unauthorized) {
                        routeProfileToLogin(profile, snapshot.serverUrl, "Sign in again to use this server address.")
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                restoreFailureReason = reason ?: JellyfinRestoreFailureReason.UnknownError,
                                restoreFailureMessage = result.message,
                                errorMessage = result.message,
                                failedProfileIds = it.failedProfileIds + profile.id,
                            )
                        }
                    }
                }
            }
        }
    }

    fun useAnotherServerFromRecovery() {
        _state.update {
            it.copy(
                step = VantafynSetupStep.ConnectServer,
                isLoading = false,
                serverUrl = "",
                username = "",
                password = "",
                server = null,
                publicUsers = emptyList(),
                restoreFailureProfile = null,
                restoreFailureReason = null,
                restoreFailureMessage = null,
                errorMessage = null,
            )
        }
    }

    fun signInAgainFromRecovery() {
        val profile = _state.value.restoreFailureProfile ?: return
        routeProfileToLogin(profile, _state.value.serverUrl.ifBlank { profile.serverUrl }, "Sign in again to reconnect this profile.")
    }

    private fun routeProfileToLogin(profile: SavedProfile, serverUrl: String, message: String) {
        _state.update {
            it.copy(
                step = VantafynSetupStep.Login,
                selectedProfileId = profile.id,
                isLoading = false,
                server = JellyfinServerConfig(
                    url = serverUrl,
                    name = profile.serverName,
                    localId = profile.serverRef,
                ),
                serverUrl = serverUrl,
                username = profile.displayName,
                password = "",
                restoreFailureProfile = null,
                restoreFailureReason = null,
                restoreFailureMessage = null,
                errorMessage = message,
            )
        }
    }

    private fun handleRestoreFailure(profile: SavedProfile, result: JellyfinResult.Failure) {
        val failure = result.cause as? JellyfinSessionRestoreFailure
        val reason = failure?.reason ?: JellyfinRestoreFailureReason.UnknownError
        if (reason == JellyfinRestoreFailureReason.AuthExpired || reason == JellyfinRestoreFailureReason.Unauthorized) {
            routeProfileToLogin(profile, profile.serverUrl, result.message)
            return
        }
        _state.update {
            it.copy(
                step = VantafynSetupStep.ConnectionRecovery,
                selectedProfileId = profile.id,
                isLoading = false,
                server = JellyfinServerConfig(url = profile.serverUrl, name = profile.serverName, localId = profile.serverRef),
                serverUrl = profile.serverUrl,
                username = profile.displayName,
                password = "",
                restoreFailureProfile = profile,
                restoreFailureReason = reason,
                restoreFailureMessage = result.message,
                failedProfileIds = it.failedProfileIds + profile.id,
                errorMessage = null,
            )
        }
    }

    fun selectPublicUser(user: JellyfinPublicUser) {
        _state.update {
            it.copy(
                step = VantafynSetupStep.Login,
                server = user.server,
                serverUrl = user.server.url,
                username = user.displayName,
                password = "",
                errorMessage = null,
                manageProfiles = false,
                pendingRemoval = null,
            )
        }
    }

    fun toggleManageProfiles() {
        _state.update { it.copy(manageProfiles = !it.manageProfiles, pendingRemoval = null, errorMessage = null) }
    }

    fun requestRemoveProfile(profile: SavedProfile) {
        _state.update { it.copy(pendingRemoval = profile, errorMessage = null) }
    }

    fun cancelRemoveProfile() {
        _state.update { it.copy(pendingRemoval = null) }
    }

    fun confirmRemoveProfile() {
        val profile = _state.value.pendingRemoval ?: return
        removeProfile(profile)
    }

    fun removeProfile(profile: SavedProfile) {
        viewModelScope.launch {
            authRepository.removeProfile(profile.id)
            val profiles = authRepository.savedProfiles()
            _state.update {
                it.copy(
                    savedProfiles = profiles,
                    step = if (profiles.isEmpty()) VantafynSetupStep.Welcome else VantafynSetupStep.ProfilePicker,
                    manageProfiles = false,
                    pendingRemoval = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun retryLibraries() {
        _state.value.session?.let(::loadLibraries)
    }

    fun navigateMobile(destination: MobileDestination) {
        refreshOmbiRequestsAvailability()
        _state.update {
            val previous = if (destination.isRootDestination()) {
                it.previousMobileDestination
            } else {
                it.mobileDestination.rootDestination()
            }
            it.copy(
                mobileDestination = destination,
                previousMobileDestination = previous,
                selectedLibrary = if (destination == MobileDestination.LibraryDetail) it.selectedLibrary else null,
                selectedMediaId = if (destination == MobileDestination.MediaDetail) it.selectedMediaId else null,
                mediaDetail = if (destination == MobileDestination.MediaDetail) it.mediaDetail else null,
                selectedAdminUserId = if (destination == MobileDestination.AdminUserSettings) it.selectedAdminUserId else null,
                adminUserDetail = if (destination == MobileDestination.AdminUserSettings) it.adminUserDetail else null,
                adminUserError = if (destination == MobileDestination.AdminUserSettings) it.adminUserError else null,
                mobileMessage = null,
            )
        }
        if (destination == MobileDestination.Favorites) loadFavorites()
        if (destination == MobileDestination.Admin) loadAdminOverview()
        if (destination == MobileDestination.PlaybackPreferences) loadPlaybackPreferences()
    }

    fun refreshOmbiRequestsAvailability() {
        val config = ombiRepository.config()
        val hasApiKey = ombiRepository.hasApiKey()
        _state.update {
            it.copy(
                ombiConfigured = config.isConfigured && hasApiKey,
                ombiRequestsEnabledForUsers = config.isEnabledForUsers && hasApiKey,
                ombiRequestsEnabledForAdmins = config.isEnabledForAdmins && hasApiKey,
                pendingOmbiAccessRequestCount = ombiRepository.pendingAccessRequestCount(),
            )
        }
    }

    fun openLibrary(library: JellyfinLibrary) {
        val session = _state.value.session ?: return
        _state.update {
            it.copy(
                mobileDestination = MobileDestination.LibraryDetail,
                previousMobileDestination = MobileDestination.Libraries,
                selectedLibrary = library,
                libraryItems = emptyList(),
                isLibraryItemsLoading = true,
                libraryItemsError = null,
                mobileMessage = null,
            )
        }
        viewModelScope.launch {
            when (val result = libraryRepository.getLibraryItems(session, library)) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isLibraryItemsLoading = false, libraryItems = result.value) }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isLibraryItemsLoading = false, libraryItemsError = result.message) }
                }
            }
        }
    }

    fun retryLibraryItems() {
        _state.value.selectedLibrary?.let(::openLibrary)
    }

    fun openMedia(itemId: UUID) {
        val session = _state.value.session ?: return
        _state.update {
            it.copy(
                mobileDestination = MobileDestination.MediaDetail,
                previousMobileDestination = it.mobileDestination.rootDestination(),
                selectedMediaId = itemId,
                mediaDetail = null,
                selectedSeasonId = null,
                selectedSeasonEpisodes = emptyList(),
                isSeasonEpisodesLoading = false,
                seasonEpisodesError = null,
                isMediaDetailLoading = true,
                mediaDetailError = null,
                mobileMessage = null,
            )
        }
        viewModelScope.launch {
            when (val result = mediaRepository.getMediaDetail(session, itemId)) {
                is JellyfinResult.Success -> {
                    val selectedSeasonId = result.value.defaultSeasonId()
                    _state.update {
                        it.copy(
                            isMediaDetailLoading = false,
                            mediaDetail = result.value,
                            selectedSeasonId = selectedSeasonId,
                            selectedSeasonEpisodes = result.value.episodes,
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isMediaDetailLoading = false, mediaDetailError = result.message) }
                }
            }
        }
    }

    fun retryMediaDetail() {
        _state.value.selectedMediaId?.let(::openMedia)
    }

    fun selectSeason(seasonId: UUID?) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val detail = snapshot.mediaDetail ?: return
        if (!detail.itemType.equals("Series", ignoreCase = true)) return
        _state.update {
            it.copy(
                selectedSeasonId = seasonId,
                selectedSeasonEpisodes = if (seasonId == detail.seasons.firstOrNull()?.id) detail.episodes else emptyList(),
                isSeasonEpisodesLoading = true,
                seasonEpisodesError = null,
            )
        }
        viewModelScope.launch {
            when (val result = mediaRepository.getSeasonEpisodes(session, detail.id, seasonId)) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(
                        isSeasonEpisodesLoading = false,
                        selectedSeasonEpisodes = result.value,
                        seasonEpisodesError = null,
                    )
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isSeasonEpisodesLoading = false, seasonEpisodesError = result.message)
                }
            }
        }
    }

    fun navigateMobileBack() {
        val snapshot = _state.value
        when (snapshot.mobileDestination) {
            MobileDestination.Player -> exitPlayback(0L)
            MobileDestination.MediaDetail -> navigateMobile(snapshot.previousMobileDestination.rootDestination())
            MobileDestination.LibraryDetail -> navigateMobile(MobileDestination.Libraries)
            MobileDestination.HomeLayout,
            MobileDestination.PlaybackPreferences -> navigateMobile(MobileDestination.Profile)
            MobileDestination.AdminUserSettings -> closeAdminUser()
            else -> Unit
        }
    }

    fun toggleMediaFavorite() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val detail = snapshot.mediaDetail ?: return
        val target = !detail.isFavorite
        val optimisticItem = detail.toFavoriteMediaItem(target)
        logFavoriteAction("detail_toggle", detail.id, detail.itemType, session, "start target=$target")
        _state.update { it.withFavoriteState(detail.id, target, optimisticItem).copy(mobileMessage = null) }
        viewModelScope.launch {
            when (val result = mediaRepository.setFavorite(session, detail.id, target)) {
                is JellyfinResult.Success -> {
                    logFavoriteAction("detail_toggle", detail.id, detail.itemType, session, "success isFavorite=${result.value}")
                    _state.update { state ->
                        state.withFavoriteState(detail.id, result.value, detail.toFavoriteMediaItem(result.value))
                    }
                    loadFavorites(session)
                }
                is JellyfinResult.Failure -> {
                    logFavoriteAction("detail_toggle", detail.id, detail.itemType, session, "failure")
                    _state.update {
                        it.withFavoriteState(detail.id, detail.isFavorite, detail.toFavoriteMediaItem(detail.isFavorite))
                            .copy(mobileMessage = result.message)
                    }
                }
            }
        }
    }

    fun setMediaFavorite(itemId: UUID, isFavorite: Boolean) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val previousFavorite = snapshot.isItemFavorite(itemId)
        val target = snapshot.favoriteMediaItem(itemId, isFavorite)
        logFavoriteAction("set_media_favorite", itemId, target?.itemType, session, "start target=$isFavorite previous=$previousFavorite")
        _state.update { it.withFavoriteState(itemId, isFavorite, target).copy(mobileMessage = null) }
        viewModelScope.launch {
            when (val result = mediaRepository.setFavorite(session, itemId, isFavorite)) {
                is JellyfinResult.Success -> {
                    logFavoriteAction("set_media_favorite", itemId, target?.itemType, session, "success isFavorite=${result.value}")
                    _state.update {
                        it.withFavoriteState(itemId, result.value, target?.copy(isFavorite = result.value)).copy(mobileMessage = null)
                    }
                    loadFavorites(session)
                    loadLibraries(session)
                }
                is JellyfinResult.Failure -> {
                    logFavoriteAction("set_media_favorite", itemId, target?.itemType, session, "failure")
                    _state.update {
                        it.withFavoriteState(itemId, previousFavorite, target?.copy(isFavorite = previousFavorite))
                            .copy(mobileMessage = result.message)
                    }
                }
            }
        }
    }

    fun toggleMediaPlayed() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val detail = snapshot.mediaDetail ?: return
        val target = !detail.isPlayed
        _state.update { it.copy(mediaDetail = detail.copy(isPlayed = target), mobileMessage = null) }
        viewModelScope.launch {
            when (val result = mediaRepository.setPlayed(session, detail.id, target)) {
                is JellyfinResult.Success -> {
                    _state.update { state ->
                        state.copy(mediaDetail = state.mediaDetail?.copy(isPlayed = result.value))
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update {
                        it.copy(
                            mediaDetail = it.mediaDetail?.copy(isPlayed = detail.isPlayed),
                            mobileMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun setMediaPlayed(itemId: UUID, isPlayed: Boolean) {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            when (val result = mediaRepository.setPlayed(session, itemId, isPlayed)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            mediaDetail = it.mediaDetail?.takeIf { detail -> detail.id == itemId }?.copy(isPlayed = result.value) ?: it.mediaDetail,
                            mobileMessage = if (result.value) "Marked watched" else "Marked unwatched",
                        )
                    }
                    loadLibraries(session)
                }
                is JellyfinResult.Failure -> _state.update { it.copy(mobileMessage = result.message) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query, searchError = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update { it.copy(isSearchLoading = false, searchResults = emptyList()) }
            return
        }
        val session = _state.value.session ?: return
        searchJob = viewModelScope.launch {
            delay(400)
            _state.update { it.copy(isSearchLoading = true, searchError = null) }
            when (val result = searchRepository.search(session, query)) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isSearchLoading = false, searchResults = result.value) }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isSearchLoading = false, searchError = result.message) }
                }
            }
        }
    }

    fun loadFavorites() {
        val session = _state.value.session ?: return
        loadFavorites(session)
    }

    private fun loadFavorites(session: JellyfinSession) {
        viewModelScope.launch {
            _state.update { it.copy(isFavoritesLoading = true, favoritesError = null) }
            when (val result = favoritesRepository.getFavorites(session)) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isFavoritesLoading = false, favorites = result.value) }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isFavoritesLoading = false, favoritesError = result.message) }
                }
            }
        }
    }

    fun loadAdminOverview() {
        val session = _state.value.session ?: return
        if (!session.user.isAdministrator) return
        viewModelScope.launch {
            _state.update { it.copy(isAdminLoading = true, adminError = null) }
            when (val result = adminRepository.getOverview(session, _state.value.libraries)) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isAdminLoading = false, adminOverview = result.value) }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isAdminLoading = false, adminError = result.message) }
                }
            }
        }
    }

    fun loadPlaybackPreferences() {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPlaybackPreferencesLoading = true, playbackPreferencesError = null) }
            when (val result = userPreferencesRepository.getPlaybackPreferences(session)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            isPlaybackPreferencesLoading = false,
                            playbackPreferences = result.value,
                            editablePlaybackPreferences = result.value,
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isPlaybackPreferencesLoading = false, playbackPreferencesError = result.message) }
                }
            }
        }
    }

    fun editPlaybackPreferences(transform: (JellyfinUserPlaybackPreferences) -> JellyfinUserPlaybackPreferences) {
        _state.update { state ->
            val current = state.editablePlaybackPreferences ?: state.playbackPreferences ?: return@update state
            state.copy(editablePlaybackPreferences = transform(current), playbackPreferencesError = null)
        }
    }

    fun savePlaybackPreferences() {
        val session = _state.value.session ?: return
        val preferences = _state.value.editablePlaybackPreferences ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPlaybackPreferencesSaving = true, playbackPreferencesError = null) }
            when (val result = userPreferencesRepository.updatePlaybackPreferences(session, preferences)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            isPlaybackPreferencesSaving = false,
                            playbackPreferences = result.value,
                            editablePlaybackPreferences = result.value,
                            mobileMessage = "Playback preferences saved",
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isPlaybackPreferencesSaving = false, playbackPreferencesError = result.message) }
                }
            }
        }
    }

    fun changeCurrentUserPassword(currentPassword: String, newPassword: String) {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            when (val result = userPreferencesRepository.changePassword(session, currentPassword, newPassword)) {
                is JellyfinResult.Success -> _state.update { it.copy(mobileMessage = "Password updated") }
                is JellyfinResult.Failure -> _state.update { it.copy(mobileMessage = result.message) }
            }
        }
    }

    fun openAdminUser(userId: UUID) {
        val session = _state.value.session ?: return
        if (!session.user.isAdministrator) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    mobileDestination = MobileDestination.AdminUserSettings,
                    previousMobileDestination = MobileDestination.Admin,
                    selectedAdminUserId = userId,
                    adminUserDetail = null,
                    isAdminUserLoading = true,
                    adminUserError = null,
                    mobileMessage = null,
                )
            }
            when (val result = adminRepository.getUserDetail(session, userId)) {
                is JellyfinResult.Success -> _state.update { it.copy(isAdminUserLoading = false, adminUserDetail = result.value) }
                is JellyfinResult.Failure -> _state.update { it.copy(isAdminUserLoading = false, adminUserError = result.message) }
            }
        }
    }

    fun closeAdminUser() {
        _state.update {
            it.copy(
                mobileDestination = MobileDestination.Admin,
                previousMobileDestination = MobileDestination.Home,
                selectedAdminUserId = null,
                adminUserDetail = null,
                isAdminUserLoading = false,
                isAdminUserSaving = false,
                adminUserError = null,
            )
        }
    }

    fun createAdminUser(username: String, password: String) {
        val session = _state.value.session ?: return
        if (!session.user.isAdministrator) return
        viewModelScope.launch {
            _state.update { it.copy(isAdminUserSaving = true, adminUserError = null, mobileMessage = null) }
            when (val result = adminRepository.createUser(session, username, password)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            mobileDestination = MobileDestination.AdminUserSettings,
                            previousMobileDestination = MobileDestination.Admin,
                            selectedAdminUserId = result.value.user.id,
                            adminUserDetail = result.value,
                            isAdminUserSaving = false,
                            isAdminUserLoading = false,
                            mobileMessage = "User created",
                        )
                    }
                    loadAdminOverview()
                }
                is JellyfinResult.Failure -> _state.update { it.copy(isAdminUserSaving = false, adminUserError = result.message) }
            }
        }
    }

    fun updateSelectedAdminUser(
        isHidden: Boolean? = null,
        isDisabled: Boolean? = null,
        isAdministrator: Boolean? = null,
        enableAllFolders: Boolean? = null,
        enabledFolderIds: List<UUID>? = null,
    ) {
        val session = _state.value.session ?: return
        val detail = _state.value.adminUserDetail ?: return
        viewModelScope.launch {
            _state.update { it.copy(isAdminUserSaving = true, adminUserError = null) }
            when (
                val result = adminRepository.updateUserPolicy(
                    session = session,
                    userId = detail.user.id,
                    isHidden = isHidden,
                    isDisabled = isDisabled,
                    isAdministrator = isAdministrator,
                    enableAllFolders = enableAllFolders,
                    enabledFolderIds = enabledFolderIds,
                )
            ) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isAdminUserSaving = false, adminUserDetail = result.value, mobileMessage = "User updated") }
                    loadAdminOverview()
                }
                is JellyfinResult.Failure -> _state.update { it.copy(isAdminUserSaving = false, adminUserError = result.message) }
            }
        }
    }

    fun resetSelectedAdminPassword(newPassword: String) {
        val session = _state.value.session ?: return
        val detail = _state.value.adminUserDetail ?: return
        viewModelScope.launch {
            _state.update { it.copy(isAdminUserSaving = true, adminUserError = null) }
            when (val result = adminRepository.resetUserPassword(session, detail.user.id, newPassword)) {
                is JellyfinResult.Success -> _state.update { it.copy(isAdminUserSaving = false, mobileMessage = "Password reset") }
                is JellyfinResult.Failure -> _state.update { it.copy(isAdminUserSaving = false, adminUserError = result.message) }
            }
        }
    }

    fun showPlaybackComingSoon() {
        _state.update { it.copy(mobileMessage = "Playback coming next") }
    }

    fun startPlayback(
        forceTranscode: Boolean = false,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        positionMs: Long? = null,
    ) {
        val snapshot = _state.value
        if (snapshot.isPlaybackLoading) return
        val session = snapshot.session ?: return
        val detail = snapshot.mediaDetail ?: return
        val target = detail.playbackTarget(positionMs) ?: run {
            _state.update { it.copy(mobileMessage = "This item cannot be played yet") }
            return
        }
        startPlaybackTarget(session, target, forceTranscode, audioStreamIndex, subtitleStreamIndex)
    }

    fun startPlaybackFromBeginning() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val detail = snapshot.mediaDetail ?: return
        val target = detail.beginningPlaybackTarget(snapshot.selectedSeasonEpisodes) ?: run {
            _state.update { it.copy(mobileMessage = "This item cannot be played yet") }
            return
        }
        startPlaybackTarget(session, target, forceTranscode = false, audioStreamIndex = null, subtitleStreamIndex = null)
    }

    fun startEpisodePlayback(episode: JellyfinEpisode, fromBeginning: Boolean = false) {
        val session = _state.value.session ?: return
        val detail = _state.value.mediaDetail
        startPlaybackTarget(
            session = session,
            target = PlaybackTarget(
                id = episode.id,
                title = episode.title,
                subtitle = listOfNotNull(detail?.title, episode.subtitle).joinToString(" · ").ifBlank { null },
                startTicks = if (fromBeginning) 0L else episode.playbackPositionTicks.takeIf { (episode.progress ?: 0f) > 0.05f && !episode.isPlayed } ?: 0L,
            ),
            forceTranscode = false,
            audioStreamIndex = null,
            subtitleStreamIndex = null,
        )
    }

    private fun startPlaybackTarget(
        session: JellyfinSession,
        target: PlaybackTarget,
        forceTranscode: Boolean,
        audioStreamIndex: Int?,
        subtitleStreamIndex: Int?,
    ) {
        viewModelScope.launch {
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.VideoPlayback)
            val previousInfo = _state.value.playbackInfo
            if (previousInfo != null) {
                playbackRepository.reportStopped(session, previousInfo, target.startTicks)
            }
            _state.update {
                it.copy(
                    previousMobileDestination = if (it.mobileDestination == MobileDestination.Player) {
                        it.previousMobileDestination
                    } else {
                        it.mobileDestination
                    },
                    mobileDestination = MobileDestination.Player,
                    playbackInfo = null,
                    playbackItem = null,
                    activePlaybackTarget = target,
                    isPlaybackLoading = true,
                    playbackError = null,
                    canTryPlaybackTranscode = false,
                    hasPlaybackRetriedTranscode = forceTranscode,
                    hasReportedPlaybackStart = false,
                )
            }
            when (
                val result = playbackRepository.getPlaybackInfo(
                    session = session,
                    itemId = target.id,
                    title = target.title,
                    subtitle = target.subtitle,
                    startPositionTicks = target.startTicks,
                    forceTranscode = forceTranscode,
                    audioStreamIndex = audioStreamIndex,
                    subtitleStreamIndex = subtitleStreamIndex,
                    isLiveTv = target.isLiveTv,
                )
            ) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            isPlaybackLoading = false,
                            playbackInfo = result.value,
                            playbackItem = result.value.toPlaybackItem(),
                            playbackError = null,
                            canTryPlaybackTranscode = result.value.fallbackStreamUrl != null,
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update {
                        it.copy(
                            isPlaybackLoading = false,
                            playbackError = result.message,
                            canTryPlaybackTranscode = !forceTranscode,
                        )
                    }
                }
            }
        }
    }

    fun retryPlayback() {
        restartActivePlayback(forceTranscode = false)
    }

    fun tryTranscodedPlayback() {
        restartActivePlayback(forceTranscode = true)
    }

    fun startLiveTvPlayback(channelId: UUID, title: String, subtitle: String?) {
        val session = _state.value.session ?: return
        startPlaybackTarget(
            session = session,
            target = PlaybackTarget(channelId, title, subtitle, startTicks = 0L, isLiveTv = true),
            forceTranscode = false,
            audioStreamIndex = null,
            subtitleStreamIndex = null,
        )
    }

    fun handlePlayerError() {
        val snapshot = _state.value
        if (!snapshot.hasPlaybackRetriedTranscode && snapshot.playbackInfo?.fallbackStreamUrl != null) {
            restartActivePlayback(forceTranscode = true)
            return
        }
        val session = snapshot.session
        val info = snapshot.playbackInfo
        _state.update {
            it.copy(
                playbackItem = null,
                playbackInfo = null,
                activePlaybackTarget = null,
                isPlaybackLoading = false,
                playbackError = "This video could not be played on this device.",
                canTryPlaybackTranscode = snapshot.playbackInfo?.fallbackStreamUrl != null,
            )
        }
        if (session != null && info != null) {
            viewModelScope.launch {
                playbackRepository.reportStopped(session, info, info.startPositionTicks)
            }
        }
    }

    fun reportPlaybackStarted(positionMs: Long) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val info = snapshot.playbackInfo ?: return
        if (snapshot.hasReportedPlaybackStart) return
        _state.update { it.copy(hasReportedPlaybackStart = true) }
        viewModelScope.launch {
            playbackRepository.reportStarted(session, info, positionMs.toTicks())
        }
    }

    fun reportPlaybackProgress(positionMs: Long, isPaused: Boolean) {
        val session = _state.value.session ?: return
        val info = _state.value.playbackInfo ?: return
        viewModelScope.launch {
            playbackRepository.reportProgress(session, info, positionMs.toTicks(), isPaused)
        }
    }

    fun exitPlayback(positionMs: Long) {
        val snapshot = _state.value
        val session = snapshot.session
        val info = snapshot.playbackInfo
        _state.update {
            it.copy(
                mobileDestination = it.previousMobileDestination,
                playbackInfo = null,
                playbackItem = null,
                activePlaybackTarget = null,
                playbackError = null,
                isPlaybackLoading = false,
                hasReportedPlaybackStart = false,
            )
        }
        if (session != null && info != null) {
            viewModelScope.launch {
                playbackRepository.reportStopped(session, info, positionMs.toTicks())
                snapshot.selectedMediaId?.let { openMedia(it) }
                loadLibraries(session)
            }
        }
    }

    fun selectPlaybackAudioTrack(index: Int) {
        restartActivePlayback(forceTranscode = false, audioStreamIndex = index, subtitleStreamIndex = _state.value.playbackInfo?.subtitleStreamIndex)
    }

    fun selectPlaybackSubtitleTrack(index: Int?) {
        restartActivePlayback(forceTranscode = false, audioStreamIndex = _state.value.playbackInfo?.audioStreamIndex, subtitleStreamIndex = index ?: -1)
    }

    fun selectPlaybackAudioTrack(index: Int, positionMs: Long) {
        restartActivePlayback(
            forceTranscode = false,
            audioStreamIndex = index,
            subtitleStreamIndex = _state.value.playbackInfo?.subtitleStreamIndex,
            positionMs = positionMs,
        )
    }

    fun selectPlaybackSubtitleTrack(index: Int?, positionMs: Long) {
        restartActivePlayback(
            forceTranscode = false,
            audioStreamIndex = _state.value.playbackInfo?.audioStreamIndex,
            subtitleStreamIndex = index ?: -1,
            positionMs = positionMs,
        )
    }

    private fun restartActivePlayback(
        forceTranscode: Boolean,
        audioStreamIndex: Int? = _state.value.playbackInfo?.audioStreamIndex,
        subtitleStreamIndex: Int? = _state.value.playbackInfo?.subtitleStreamIndex,
        positionMs: Long? = null,
    ) {
        val session = _state.value.session ?: return
        val target = _state.value.activePlaybackTarget ?: return startPlayback(forceTranscode, audioStreamIndex, subtitleStreamIndex, positionMs)
        startPlaybackTarget(
            session = session,
            target = positionMs?.let { target.copy(startTicks = it.toTicks()) } ?: target,
            forceTranscode = forceTranscode,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
        )
    }

    fun clearMobileMessage() {
        _state.update { it.copy(mobileMessage = null) }
    }

    fun toggleHomeSection(type: HomeSectionType) {
        if (type == HomeSectionType.MediaBar) return
        _state.update { state ->
            val updated = state.homeLayout.map {
                if (it.type == type) it.copy(visible = !it.visible) else it
            }
            persistHomeLayout(state.session?.profileId, updated)
            state.copy(homeLayout = updated)
        }
    }

    fun moveHomeSection(type: HomeSectionType, direction: Int) {
        if (type == HomeSectionType.MediaBar) return
        _state.update { state ->
            val fixed = state.homeLayout.firstOrNull { it.type == HomeSectionType.MediaBar }
                ?: defaultHomeLayout().first { it.type == HomeSectionType.MediaBar }
            val current = state.homeLayout
                .filter { it.type != HomeSectionType.MediaBar }
                .sortedBy { it.order }
            val index = current.indexOfFirst { it.type == type }
            val target = (index + direction).coerceIn(0, current.lastIndex)
            if (index < 0 || index == target) return@update state
            val mutable = current.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(target, item)
            val updated = listOf(fixed.copy(visible = true, order = 0)) +
                mutable.mapIndexed { index, preference -> preference.copy(order = index + 1) }
            persistHomeLayout(state.session?.profileId, updated)
            state.copy(homeLayout = updated)
        }
    }

    fun resetHomeLayout() {
        _state.update { state ->
            val defaults = defaultHomeLayout()
            persistHomeLayout(state.session?.profileId, defaults)
            state.copy(homeLayout = defaults)
        }
    }

    fun addSmartRow(title: String) {
        _state.update { state ->
            if (title !in supportedSmartRows || title in state.configuredSmartRows) return@update state
            val updated = state.configuredSmartRows + title
            persistSmartRows(state.session?.profileId, updated)
            state.copy(configuredSmartRows = updated)
        }
    }

    fun removeSmartRow(title: String) {
        _state.update { state ->
            val updated = state.configuredSmartRows.filterNot { it == title }
            persistSmartRows(state.session?.profileId, updated)
            state.copy(configuredSmartRows = updated)
        }
    }

    fun cycleSectionArtwork(type: HomeSectionType) {
        updateHomeSection(type) { it.copy(artworkType = it.artworkType.next()) }
    }

    fun cycleSectionShape(type: HomeSectionType) {
        updateHomeSection(type) { it.copy(cardShape = it.cardShape.next()) }
    }

    fun cycleSectionSize(type: HomeSectionType) {
        updateHomeSection(type) { it.copy(cardSize = it.cardSize.next()) }
    }

    fun cycleSectionSpacing(type: HomeSectionType) {
        updateHomeSection(type) { it.copy(spacing = it.spacing.next()) }
    }

    private fun updateHomeSection(type: HomeSectionType, transform: (HomeSectionPreference) -> HomeSectionPreference) {
        _state.update { state ->
            val updated = state.homeLayout.map { if (it.type == type) transform(it) else it }
            persistHomeLayout(state.session?.profileId, updated)
            state.copy(homeLayout = updated)
        }
    }

    fun toggleThemeMusic() {
        _state.update { state ->
            val enabled = !state.themeMusicEnabled
            state.session?.profileId?.let { profileId ->
                homeLayoutStorage.edit().putBoolean("theme_music_$profileId", enabled).apply()
            }
            state.copy(themeMusicEnabled = enabled)
        }
    }

    fun selectThemeMusicVolume(volume: ThemeMusicVolume) {
        _state.update { state ->
            state.session?.profileId?.let { profileId ->
                homeLayoutStorage.edit().putString("theme_music_volume_$profileId", volume.name).apply()
            }
            state.copy(themeMusicVolume = volume)
        }
    }

    fun selectBackground(background: VantafynAppBackground) {
        _state.update { state ->
            val key = background.name
            val editor = homeLayoutStorage.edit().putString("background_app", key)
            state.session?.profileId?.let { profileId ->
                editor.putString("background_$profileId", key)
            }
            editor.apply()
            state.copy(selectedBackground = background)
        }
    }

    private fun readSelectedBackground(profileId: String?): VantafynAppBackground {
        val key = profileId?.let { homeLayoutStorage.getString("background_$it", null) }
            ?: homeLayoutStorage.getString("background_app", null)
        return key?.let { runCatching { VantafynAppBackground.valueOf(it) }.getOrNull() }
            ?: VantafynAppBackground.Nebula
    }

    private fun readSmartRows(profileId: String?): List<String> {
        val key = profileId?.let { "smart_rows_$it" } ?: return emptyList()
        return homeLayoutStorage.getString(key, null)
            ?.split('|')
            ?.filter { it in supportedSmartRows }
            .orEmpty()
    }

    private fun persistSmartRows(profileId: String?, rows: List<String>) {
        val key = profileId?.let { "smart_rows_$it" } ?: return
        homeLayoutStorage.edit().putString(key, rows.joinToString("|")).apply()
    }

    private fun readHomeLayout(profileId: String?): List<HomeSectionPreference> {
        val key = profileId?.let { "layout_$it" } ?: return defaultHomeLayout()
        val encoded = homeLayoutStorage.getString(key, null) ?: return defaultHomeLayout()
        val decoded = encoded
            .split(',')
            .mapNotNull { token ->
                val parts = token.split(':')
                val type = parts.getOrNull(0)?.let { runCatching { HomeSectionType.valueOf(it) }.getOrNull() }
                val visible = parts.getOrNull(1)?.toBooleanStrictOrNull()
                if (type != null && visible != null) {
                    HomeSectionPreference(
                        type = type,
                        visible = visible,
                        order = 0,
                        artworkType = parts.getOrNull(2)?.let { runCatching { VantafynArtworkType.valueOf(it) }.getOrNull() } ?: type.defaultArtworkType(),
                        cardShape = parts.getOrNull(3)?.let { runCatching { VantafynCardShape.valueOf(it) }.getOrNull() } ?: VantafynCardShape.Rounded,
                        cardSize = parts.getOrNull(4)?.let { runCatching { VantafynCardSize.valueOf(it) }.getOrNull() } ?: VantafynCardSize.Medium,
                        spacing = parts.getOrNull(5)?.let { runCatching { VantafynCardSpacing.valueOf(it) }.getOrNull() } ?: VantafynCardSpacing.Comfortable,
                    )
                } else {
                    null
                }
            }
            .mapIndexed { order, preference -> preference.copy(order = order) }
        val missing = defaultHomeLayout().filter { default -> decoded.none { it.type == default.type } }
        return (decoded + missing).mapIndexed { order, preference -> preference.copy(order = order) }
    }

    private fun persistHomeLayout(profileId: String?, layout: List<HomeSectionPreference>) {
        val key = profileId?.let { "layout_$it" } ?: return
        val encoded = layout.sortedBy { it.order }.joinToString(",") {
            "${it.type.name}:${it.visible}:${it.artworkType.name}:${it.cardShape.name}:${it.cardSize.name}:${it.spacing.name}"
        }
        homeLayoutStorage.edit().putString(key, encoded).apply()
    }

    private fun readThemeMusicEnabled(profileId: String?): Boolean =
        profileId?.let { homeLayoutStorage.getBoolean("theme_music_$it", true) } ?: true

    private fun readThemeMusicVolume(profileId: String?): ThemeMusicVolume {
        val key = profileId?.let { homeLayoutStorage.getString("theme_music_volume_$it", null) }
        return key?.let { runCatching { ThemeMusicVolume.valueOf(it) }.getOrNull() }
            ?: ThemeMusicVolume.Soft
    }

    private fun loadSavedProfiles() {
        viewModelScope.launch {
            _state.update { it.copy(step = VantafynSetupStep.Splash, isLoading = true) }
            val profiles = authRepository.savedProfiles()
            val autoLogin = readAutoLoginLastProfile()
            if (autoLogin) {
                val lastProfile = profiles.maxByOrNull { it.lastUsedAt }
                if (lastProfile != null) {
                    _state.update {
                        it.copy(
                            savedProfiles = profiles,
                            autoLoginLastProfile = true,
                            selectedBackground = readSelectedBackground(null),
                        )
                    }
                    selectProfile(lastProfile)
                    return@launch
                }
            }
            _state.update {
                it.copy(
                    step = if (profiles.isEmpty()) VantafynSetupStep.Welcome else VantafynSetupStep.ProfilePicker,
                    isLoading = false,
                    savedProfiles = profiles,
                    autoLoginLastProfile = autoLogin,
                    selectedBackground = readSelectedBackground(null),
                )
            }
        }
    }

    fun toggleAutoLoginLastProfile() {
        val enabled = !_state.value.autoLoginLastProfile
        appPreferences.edit().putBoolean(KEY_AUTO_LOGIN_LAST_PROFILE, enabled).apply()
        _state.update { it.copy(autoLoginLastProfile = enabled) }
    }

    private fun readAutoLoginLastProfile(): Boolean =
        appPreferences.getBoolean(KEY_AUTO_LOGIN_LAST_PROFILE, false)

    private suspend fun refreshSavedProfiles() {
        _state.update { it.copy(savedProfiles = authRepository.savedProfiles()) }
    }

    private fun loadLibraries(session: JellyfinSession) {
        viewModelScope.launch {
            _state.update { it.copy(isLibrariesLoading = true, errorMessage = null) }
            when (val result = libraryRepository.getLibraries(session)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            isLibrariesLoading = false,
                            libraries = result.value,
                        )
                    }
                    loadHome(session, result.value)
                }
                is JellyfinResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLibrariesLoading = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun loadHome(session: JellyfinSession, libraries: List<JellyfinLibrary>) {
        viewModelScope.launch {
            _state.update { it.copy(isHomeLoading = true, homeErrorMessage = null) }
            when (val result = homeRepository.getHome(session, libraries)) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isHomeLoading = false, home = result.value) }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isHomeLoading = false, homeErrorMessage = result.message) }
                }
            }
        }
    }

    private fun pollQuickConnect(session: JellyfinQuickConnectSession) {
        quickConnectJob?.cancel()
        quickConnectJob = viewModelScope.launch {
            repeat(60) {
                delay(2_000)
                when (val result = quickConnectRepository.poll(session)) {
                    is JellyfinResult.Success -> {
                        val jellyfinSession = result.value
                        if (jellyfinSession != null) {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    quickConnectMessage = "Authorized",
                                    quickConnectSession = null,
                                    session = jellyfinSession,
                                    server = jellyfinSession.server,
                                    serverUrl = jellyfinSession.server.url,
                                    username = jellyfinSession.user.name,
                                    password = "",
                                    step = VantafynSetupStep.Home,
                                    homeLayout = readHomeLayout(jellyfinSession.profileId),
                                    themeMusicEnabled = readThemeMusicEnabled(jellyfinSession.profileId),
                                    themeMusicVolume = readThemeMusicVolume(jellyfinSession.profileId),
                                    selectedBackground = readSelectedBackground(jellyfinSession.profileId),
                                    configuredSmartRows = readSmartRows(jellyfinSession.profileId),
                                )
                            }
                            refreshSavedProfiles()
                            loadLibraries(jellyfinSession)
                            loadFavorites(jellyfinSession)
                            return@launch
                        }
                        _state.update { it.copy(quickConnectMessage = "Waiting for approval...") }
                    }
                    is JellyfinResult.Failure -> {
                        _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                        return@launch
                    }
                }
            }
            _state.update { it.copy(isLoading = false, errorMessage = "Quick Connect expired. Try again.") }
        }
    }
}

data class VantafynHomeUiState(
    val step: VantafynSetupStep = VantafynSetupStep.Splash,
    val isLoading: Boolean = false,
    val isLibrariesLoading: Boolean = false,
    val isHomeLoading: Boolean = false,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val server: JellyfinServerConfig? = null,
    val session: JellyfinSession? = null,
    val savedProfiles: List<SavedProfile> = emptyList(),
    val autoLoginLastProfile: Boolean = false,
    val publicUsers: List<JellyfinPublicUser> = emptyList(),
    val manageProfiles: Boolean = false,
    val pendingRemoval: SavedProfile? = null,
    val selectedProfileId: String? = null,
    val restoreFailureProfile: SavedProfile? = null,
    val restoreFailureReason: JellyfinRestoreFailureReason? = null,
    val restoreFailureMessage: String? = null,
    val failedProfileIds: Set<String> = emptySet(),
    val libraries: List<JellyfinLibrary> = emptyList(),
    val home: JellyfinHome? = null,
    val homeErrorMessage: String? = null,
    val mobileDestination: MobileDestination = MobileDestination.Home,
    val selectedLibrary: JellyfinLibrary? = null,
    val libraryItems: List<JellyfinMediaItem> = emptyList(),
    val isLibraryItemsLoading: Boolean = false,
    val libraryItemsError: String? = null,
    val selectedMediaId: UUID? = null,
    val mediaDetail: JellyfinMediaDetail? = null,
    val selectedSeasonId: UUID? = null,
    val selectedSeasonEpisodes: List<JellyfinEpisode> = emptyList(),
    val isSeasonEpisodesLoading: Boolean = false,
    val seasonEpisodesError: String? = null,
    val isMediaDetailLoading: Boolean = false,
    val mediaDetailError: String? = null,
    val searchQuery: String = "",
    val searchResults: List<JellyfinSearchResult> = emptyList(),
    val isSearchLoading: Boolean = false,
    val searchError: String? = null,
    val favorites: List<JellyfinMediaItem> = emptyList(),
    val isFavoritesLoading: Boolean = false,
    val favoritesError: String? = null,
    val adminOverview: JellyfinAdminOverview? = null,
    val isAdminLoading: Boolean = false,
    val adminError: String? = null,
    val playbackPreferences: JellyfinUserPlaybackPreferences? = null,
    val editablePlaybackPreferences: JellyfinUserPlaybackPreferences? = null,
    val isPlaybackPreferencesLoading: Boolean = false,
    val isPlaybackPreferencesSaving: Boolean = false,
    val playbackPreferencesError: String? = null,
    val selectedAdminUserId: UUID? = null,
    val adminUserDetail: JellyfinAdminUserDetail? = null,
    val isAdminUserLoading: Boolean = false,
    val isAdminUserSaving: Boolean = false,
    val adminUserError: String? = null,
    val mobileMessage: String? = null,
    val confirmLogout: Boolean = false,
    val homeLayout: List<HomeSectionPreference> = defaultHomeLayout(),
    val themeMusicEnabled: Boolean = true,
    val themeMusicVolume: ThemeMusicVolume = ThemeMusicVolume.Soft,
    val selectedBackground: VantafynAppBackground = VantafynAppBackground.Nebula,
    val configuredSmartRows: List<String> = emptyList(),
    val previousMobileDestination: MobileDestination = MobileDestination.Home,
    val activePlaybackTarget: PlaybackTarget? = null,
    val playbackInfo: JellyfinPlaybackInfo? = null,
    val playbackItem: VantafynPlaybackItem? = null,
    val isPlaybackLoading: Boolean = false,
    val playbackError: String? = null,
    val canTryPlaybackTranscode: Boolean = false,
    val hasPlaybackRetriedTranscode: Boolean = false,
    val hasReportedPlaybackStart: Boolean = false,
    val quickConnectSession: JellyfinQuickConnectSession? = null,
    val quickConnectMessage: String? = null,
    val ombiConfigured: Boolean = false,
    val ombiRequestsEnabledForUsers: Boolean = false,
    val ombiRequestsEnabledForAdmins: Boolean = false,
    val pendingOmbiAccessRequestCount: Int = 0,
    val errorMessage: String? = null,
)

private fun VantafynHomeUiState.withFavoriteState(
    itemId: UUID,
    isFavorite: Boolean,
    favoriteItem: JellyfinMediaItem? = null,
): VantafynHomeUiState =
    copy(
        home = home?.copy(
            sections = home.sections.map { section ->
                section.copy(
                    items = section.items.map { item ->
                        if (item.id == itemId) item.copy(isFavorite = isFavorite) else item
                    },
                )
            },
        ),
        libraryItems = libraryItems.map { item ->
            if (item.id == itemId) item.copy(isFavorite = isFavorite) else item
        },
        searchResults = searchResults.map { item ->
            if (item.id == itemId) item.copy(isFavorite = isFavorite) else item
        },
        favorites = if (isFavorite) {
            val updated = favorites.map { item -> if (item.id == itemId) item.copy(isFavorite = true) else item }
            if (updated.any { it.id == itemId } || favoriteItem == null) {
                updated
            } else {
                listOf(favoriteItem.copy(isFavorite = true)) + updated
            }
        } else {
            favorites.filterNot { it.id == itemId }
        },
        mediaDetail = mediaDetail?.let { detail ->
            if (detail.id == itemId) detail.copy(isFavorite = isFavorite) else detail
        },
    )

private fun VantafynHomeUiState.isItemFavorite(itemId: UUID): Boolean {
    mediaDetail?.takeIf { it.id == itemId }?.let { return it.isFavorite }
    return favorites.any { it.id == itemId } ||
        libraryItems.any { it.id == itemId && it.isFavorite } ||
        searchResults.any { it.id == itemId && it.isFavorite } ||
        home?.sections.orEmpty().flatMap { it.items }.any { it.id == itemId && it.isFavorite }
}

private fun VantafynHomeUiState.favoriteMediaItem(itemId: UUID, isFavorite: Boolean): JellyfinMediaItem? =
    mediaDetail?.takeIf { it.id == itemId }?.toFavoriteMediaItem(isFavorite)
        ?: favorites.firstOrNull { it.id == itemId }?.copy(isFavorite = isFavorite)
        ?: libraryItems.firstOrNull { it.id == itemId }?.copy(isFavorite = isFavorite)
        ?: searchResults.firstOrNull { it.id == itemId }?.toMediaItem(isFavorite)
        ?: home?.sections.orEmpty()
            .flatMap { it.items }
            .firstOrNull { it.id == itemId }
            ?.toMediaItem(isFavorite)

private fun JellyfinMediaDetail.toFavoriteMediaItem(isFavorite: Boolean): JellyfinMediaItem =
    JellyfinMediaItem(
        id = id,
        title = title,
        subtitle = subtitle,
        year = year,
        itemType = itemType,
        imageUrl = imageUrl,
        backdropUrl = backdropUrl,
        thumbUrl = null,
        logoUrl = logoUrl,
        progress = progress,
        shape = dev.vantafyn.core.jellyfin.JellyfinMediaCardShape.Poster,
        isFavorite = isFavorite,
    )

private fun JellyfinMediaCard.toMediaItem(isFavorite: Boolean): JellyfinMediaItem =
    JellyfinMediaItem(
        id = id,
        title = title,
        subtitle = subtitle,
        year = year,
        itemType = itemType,
        imageUrl = imageUrl,
        backdropUrl = backdropUrl,
        thumbUrl = thumbUrl,
        logoUrl = logoUrl,
        progress = progress,
        shape = shape,
        isFavorite = isFavorite,
    )

private fun JellyfinSearchResult.toMediaItem(isFavorite: Boolean): JellyfinMediaItem =
    JellyfinMediaItem(
        id = id,
        title = title,
        subtitle = subtitle,
        year = year,
        itemType = itemType,
        imageUrl = imageUrl,
        backdropUrl = backdropUrl,
        thumbUrl = null,
        logoUrl = null,
        progress = null,
        shape = shape,
        isFavorite = isFavorite,
    )

private fun logFavoriteAction(
    action: String,
    itemId: UUID,
    itemType: String?,
    session: JellyfinSession,
    result: String,
) {
    Log.d(
        "VantafynFavorites",
        "action=$action itemId=$itemId itemType=${itemType ?: "unknown"} server=${session.server.url.safeHostForLog()} repository=JellyfinMediaRepository result=$result",
    )
}

private fun String.safeHostForLog(): String =
    runCatching {
        URI(this).let { uri ->
            listOfNotNull(uri.scheme, uri.host ?: uri.authority)
                .joinToString("://")
                .ifBlank { "unknown" }
        }
    }.getOrDefault("unknown")

enum class ThemeMusicVolume(val label: String, val level: Float) {
    Soft("Soft", 0.12f),
    Medium("Medium", 0.20f),
    High("High", 0.32f),
    Full("Full", 0.48f),
}

enum class MobileDestination {
    Home,
    Libraries,
    Search,
    Music,
    Favorites,
    Requests,
    Admin,
    AdminUserSettings,
    Profile,
    HomeLayout,
    PlaybackPreferences,
    LibraryDetail,
    MediaDetail,
    Player,
}

private fun MobileDestination.isRootDestination(): Boolean =
    when (this) {
        MobileDestination.Home,
        MobileDestination.Libraries,
        MobileDestination.Search,
        MobileDestination.Music,
        MobileDestination.Favorites,
        MobileDestination.Requests,
        MobileDestination.Admin,
        MobileDestination.Profile -> true
        MobileDestination.AdminUserSettings,
        MobileDestination.HomeLayout,
        MobileDestination.PlaybackPreferences,
        MobileDestination.LibraryDetail,
        MobileDestination.MediaDetail,
        MobileDestination.Player -> false
    }

private fun MobileDestination.rootDestination(): MobileDestination =
    if (isRootDestination()) this else MobileDestination.Home

data class PlaybackTarget(
    val id: UUID,
    val title: String,
    val subtitle: String?,
    val startTicks: Long,
    val isLiveTv: Boolean = false,
)

private fun JellyfinMediaDetail.playbackTarget(positionMs: Long? = null): PlaybackTarget? =
    if (itemType.equals("Series", ignoreCase = true)) {
        episodes.firstOrNull { (it.progress ?: 0f) < 0.95f }?.let {
            PlaybackTarget(
                id = it.id,
                title = it.title,
                subtitle = listOfNotNull(title, it.subtitle).joinToString(" · ").ifBlank { null },
                startTicks = 0L,
            )
        }
    } else {
        PlaybackTarget(
            id = id,
            title = title,
            subtitle = subtitle,
            startTicks = positionMs?.toTicks()
                ?: playbackPositionTicks.takeIf { (progress ?: 0f) > 0.05f && !isPlayed }
                ?: 0L,
        )
    }

private fun JellyfinMediaDetail.beginningPlaybackTarget(selectedEpisodes: List<JellyfinEpisode>): PlaybackTarget? =
    if (itemType.equals("Series", ignoreCase = true)) {
        val episode = (selectedEpisodes.ifEmpty { episodes })
            .sortedWith(compareBy<JellyfinEpisode> { it.seasonIndexNumber ?: Int.MAX_VALUE }.thenBy { it.indexNumber ?: Int.MAX_VALUE })
            .firstOrNull()
            ?: return null
        PlaybackTarget(
            id = episode.id,
            title = episode.title,
            subtitle = listOfNotNull(title, episode.subtitle).joinToString(" · ").ifBlank { null },
            startTicks = 0L,
        )
    } else {
        PlaybackTarget(
            id = id,
            title = title,
            subtitle = subtitle,
            startTicks = 0L,
        )
    }

private fun JellyfinMediaDetail.defaultSeasonId(): UUID? {
    if (!itemType.equals("Series", ignoreCase = true)) return null
    val nextEpisode = episodes.firstOrNull { (it.progress ?: 0f) in 0.01f..0.94f }
        ?: episodes.firstOrNull { !it.isPlayed }
        ?: episodes.firstOrNull()
    return seasons.firstOrNull { it.indexNumber == nextEpisode?.seasonIndexNumber }?.id ?: seasons.firstOrNull()?.id
}

private fun JellyfinPlaybackInfo.toPlaybackItem(): VantafynPlaybackItem =
    VantafynPlaybackItem(
        itemId = itemId.toString(),
        title = title,
        subtitle = subtitle,
        streamUrl = streamUrl,
        fallbackStreamUrl = fallbackStreamUrl,
        startPositionMs = startPositionTicks / 10_000L,
        durationMs = runtimeTicks?.let { it / 10_000L },
        sourceLabel = sourceLabel,
        selectedAudioStreamIndex = audioStreamIndex,
        selectedSubtitleStreamIndex = subtitleStreamIndex?.takeIf { it >= 0 },
        audioTracks = audioTracks.map {
            VantafynAudioTrack(
                index = it.index,
                label = it.label,
                language = it.language,
                codec = it.codec,
                channels = it.channels,
                isDefault = it.isDefault,
            )
        },
        subtitleTracks = subtitleTracks.map {
            VantafynSubtitleTrack(
                index = it.index,
                label = it.label,
                language = it.language,
                codec = it.codec,
                isExternal = it.isExternal,
                isDefault = it.isDefault,
            )
        },
    )

private fun Long.toTicks(): Long =
    coerceAtLeast(0L) * 10_000L

enum class VantafynAppBackground(val label: String) {
    Nebula("Nebula"),
    Background1("Glass blue"),
    Background2("Twilight"),
    Background3("Aurora"),
    Background4("Deep space"),
}

val supportedSmartRows = listOf(
    "New in Crime",
    "New in Thrillers",
    "New in Comedy",
    "New in Action",
    "New in Horror",
    "New in Drama",
    "Highly Rated",
    "Family Friendly",
    "Unwatched Movies",
    "Unwatched TV",
    "Recently Released Movies",
    "Recently Released TV",
)

data class HomeSectionPreference(
    val type: HomeSectionType,
    val visible: Boolean,
    val order: Int,
    val displayMode: VantafynCardDisplayMode = VantafynCardDisplayMode.Auto,
    val artworkType: VantafynArtworkType = VantafynArtworkType.Auto,
    val cardShape: VantafynCardShape = VantafynCardShape.Rounded,
    val cardSize: VantafynCardSize = VantafynCardSize.Medium,
    val spacing: VantafynCardSpacing = VantafynCardSpacing.Comfortable,
)

enum class HomeSectionType(val label: String) {
    MediaBar("Media bar"),
    MyMedia("My Media"),
    ContinueWatching("Continue Watching / Up Next"),
    RecentlyAddedMovies("Recently Added Movies"),
    RecentlyAddedTv("Recently Added TV"),
    LiveTvChannels("Live TV Channels"),
    SmartRows("Smart Rows"),
    OtherLibraries("Other libraries"),
}

enum class VantafynCardDisplayMode { Auto, Poster, Wide, Square, Compact }
enum class VantafynArtworkType { Auto, PrimaryPoster, Backdrop, Thumb, Logo }
enum class VantafynCardShape { Rounded, Squircle, Soft, Sharpish }
enum class VantafynCardSize { Small, Medium, Large }
enum class VantafynCardSpacing { Compact, Comfortable, Spacious }

fun defaultHomeLayout(): List<HomeSectionPreference> =
    listOf(
        HomeSectionType.MediaBar,
        HomeSectionType.MyMedia,
        HomeSectionType.ContinueWatching,
        HomeSectionType.RecentlyAddedMovies,
        HomeSectionType.RecentlyAddedTv,
        HomeSectionType.LiveTvChannels,
        HomeSectionType.SmartRows,
        HomeSectionType.OtherLibraries,
    ).mapIndexed { index, type ->
        HomeSectionPreference(
            type = type,
            visible = type != HomeSectionType.SmartRows,
            order = index,
            artworkType = type.defaultArtworkType(),
        )
    }

private fun HomeSectionType.defaultArtworkType(): VantafynArtworkType =
    when (this) {
        HomeSectionType.RecentlyAddedMovies -> VantafynArtworkType.PrimaryPoster
        HomeSectionType.ContinueWatching,
        HomeSectionType.RecentlyAddedTv,
        HomeSectionType.LiveTvChannels,
        HomeSectionType.MyMedia,
        HomeSectionType.OtherLibraries -> VantafynArtworkType.Backdrop
        else -> VantafynArtworkType.Auto
    }

private fun VantafynArtworkType.next(): VantafynArtworkType =
    enumValues<VantafynArtworkType>().let { it[(ordinal + 1) % it.size] }

private fun VantafynCardShape.next(): VantafynCardShape =
    enumValues<VantafynCardShape>().let { it[(ordinal + 1) % it.size] }

private fun VantafynCardSize.next(): VantafynCardSize =
    enumValues<VantafynCardSize>().let { it[(ordinal + 1) % it.size] }

private fun VantafynCardSpacing.next(): VantafynCardSpacing =
    enumValues<VantafynCardSpacing>().let { it[(ordinal + 1) % it.size] }

private const val KEY_AUTO_LOGIN_LAST_PROFILE = "auto_login_last_profile"

enum class VantafynSetupStep {
    Splash,
    Welcome,
    ConnectServer,
    ServerConfirm,
    Login,
    QuickConnect,
    ProfilePicker,
    ConnectionRecovery,
    Home,
}

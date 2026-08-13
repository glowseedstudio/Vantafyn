package dev.vantafyn.feature.home.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vantafyn.core.cast.PlaybackOutputCoordinator
import dev.vantafyn.core.jellyfin.JellyfinAuthRepository
import dev.vantafyn.core.jellyfin.JellyfinAdminOverview
import dev.vantafyn.core.jellyfin.JellyfinAdminRepository
import dev.vantafyn.core.jellyfin.JellyfinAdminTask
import dev.vantafyn.core.jellyfin.JellyfinFavoritesRepository
import dev.vantafyn.core.jellyfin.JellyfinHome
import dev.vantafyn.core.jellyfin.JellyfinHomeRepository
import dev.vantafyn.core.jellyfin.JellyfinEpisode
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.jellyfin.JellyfinLibraryItemFilter
import dev.vantafyn.core.jellyfin.JellyfinLibraryPage
import dev.vantafyn.core.jellyfin.JellyfinLibraryRepository
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinMediaItem
import dev.vantafyn.core.jellyfin.JellyfinMediaRepository
import dev.vantafyn.core.jellyfin.JellyfinPlaybackInfo
import dev.vantafyn.core.jellyfin.JellyfinPlaybackRepository
import dev.vantafyn.core.jellyfin.JellyfinProfileImageUpload
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
import dev.vantafyn.core.jellyfin.JellyfinUpNextCandidate
import dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences
import dev.vantafyn.core.jellyfin.JellyfinWebSocketEvent
import dev.vantafyn.core.jellyfin.JellyfinAdminUserDetail
import dev.vantafyn.core.jellyfin.JellyfinUserPreferencesRepository
import dev.vantafyn.core.jellyfin.JellyfinWatchPartyRepository
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.jellyfin.SyncPlayConnectionState
import dev.vantafyn.core.jellyfin.SyncPlayCommand
import dev.vantafyn.core.jellyfin.WatchPartyCandidate
import dev.vantafyn.core.jellyfin.WatchPartyInvite
import dev.vantafyn.core.jellyfin.WatchPartyInviteEventMapper
import dev.vantafyn.core.jellyfin.WatchPartyInviteRecipient
import dev.vantafyn.core.jellyfin.WatchPartyInviteStatus
import dev.vantafyn.core.jellyfin.WatchPartyMatch
import dev.vantafyn.core.jellyfin.WatchPartyMatchRule
import dev.vantafyn.core.jellyfin.WatchPartyMediaScope
import dev.vantafyn.core.jellyfin.WatchPartyMode
import dev.vantafyn.core.jellyfin.WatchPartyPlaybackState
import dev.vantafyn.core.jellyfin.WatchPartyRules
import dev.vantafyn.core.jellyfin.WatchPartySelectedMedia
import dev.vantafyn.core.jellyfin.WatchPartySession
import dev.vantafyn.core.jellyfin.WatchPartyMemberRealtimeState
import dev.vantafyn.core.jellyfin.WatchPartyMemberReadyStatus
import dev.vantafyn.core.jellyfin.WatchPartyVote
import dev.vantafyn.core.jellyfin.WatchPartyVoteValue
import dev.vantafyn.core.media.VantafynAudioTrack
import dev.vantafyn.core.media.AutoplaySettings
import dev.vantafyn.core.media.LongRunningTaskRegistry
import dev.vantafyn.core.media.LongRunningTaskType
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.UpNextCandidate
import dev.vantafyn.core.media.VantafynPlaybackItem
import dev.vantafyn.core.media.VantafynMusicStopReason
import dev.vantafyn.core.media.VantafynSubtitleTrack
import dev.vantafyn.core.ombi.OmbiRepository
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    private val watchPartyRepository: JellyfinWatchPartyRepository = repositories.watchPartyRepository
    private val realtimeClient = repositories.realtimeClient
    private val ombiRepository = OmbiRepository(application)
    private val homeLayoutStorage = application.getSharedPreferences("vantafyn_home_layout", Context.MODE_PRIVATE)
    private val appPreferences = application.getSharedPreferences("vantafyn_app_preferences", Context.MODE_PRIVATE)
    private val hasCompletedSetup: Boolean
        get() = appPreferences.getBoolean(KEY_SETUP_COMPLETED, false)
    private var searchJob: Job? = null
    private var quickConnectJob: Job? = null
    private var watchPartyRealtimeJob: Job? = null
    private var watchPartyInviteExpiryJob: Job? = null
    private var isAppForeground = false

    private val _state = MutableStateFlow(
        VantafynHomeUiState(
            autoplayCountdownSeconds = appPreferences.getInt(KEY_AUTOPLAY_COUNTDOWN_SECONDS, 10)
                .takeIf { value -> value in AUTOPLAY_COUNTDOWN_OPTIONS }
                ?: 10,
            passoutProtectionLimitMinutes = appPreferences.getInt(KEY_PASSOUT_PROTECTION_LIMIT_MINUTES, 180)
                .takeIf { value -> value in PASSOUT_PROTECTION_LIMIT_OPTIONS }
                ?: 180,
            watchPartyEnabled = appPreferences.getBoolean(KEY_WATCH_PARTY_ENABLED, true),
            watchPartyInvitesEnabled = appPreferences.getBoolean(KEY_WATCH_PARTY_INVITES_ENABLED, true),
            watchPartyInviteAnimationEnabled = appPreferences.getBoolean(KEY_WATCH_PARTY_INVITE_ANIMATION_ENABLED, true),
            watchPartyInviteExpirySeconds = appPreferences.getInt(KEY_WATCH_PARTY_INVITE_EXPIRY_SECONDS, 60)
                .takeIf { value -> value in WATCH_PARTY_INVITE_EXPIRY_OPTIONS }
                ?: 60,
        ),
    )
    val state: StateFlow<VantafynHomeUiState> = _state.asStateFlow()

    init {
        loadSavedProfiles()
        refreshOmbiRequestsAvailability()
    }

    override fun onCleared() {
        stopWatchPartyRealtime()
        watchPartyInviteExpiryJob?.cancel()
        super.onCleared()
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

    fun navigateSetupBack() {
        quickConnectJob?.cancel()
        quickConnectJob = null
        _state.update { state ->
            when (state.step) {
                VantafynSetupStep.Splash,
                VantafynSetupStep.Home -> state
                VantafynSetupStep.Welcome -> {
                    if (state.savedProfiles.isEmpty()) {
                        state
                    } else {
                        state.copy(
                            step = VantafynSetupStep.ProfilePicker,
                            errorMessage = null,
                            isLoading = false,
                            manageProfiles = false,
                            pendingRemoval = null,
                        )
                    }
                }
                VantafynSetupStep.ConnectServer -> {
                    if (state.savedProfiles.isEmpty()) {
                        state.copy(step = VantafynSetupStep.Welcome, errorMessage = null, isLoading = false)
                    } else {
                        state.copy(
                            step = VantafynSetupStep.ProfilePicker,
                            errorMessage = null,
                            isLoading = false,
                            server = null,
                            publicUsers = emptyList(),
                            manageProfiles = false,
                            pendingRemoval = null,
                        )
                    }
                }
                VantafynSetupStep.ServerConfirm -> state.copy(
                    step = VantafynSetupStep.ConnectServer,
                    errorMessage = null,
                    isLoading = false,
                )
                VantafynSetupStep.Login -> {
                    if (state.publicUsers.isNotEmpty()) {
                        state.copy(
                            step = VantafynSetupStep.ProfilePicker,
                            username = "",
                            password = "",
                            errorMessage = null,
                            isLoading = false,
                        )
                    } else {
                        state.copy(
                            step = if (state.server != null) VantafynSetupStep.ServerConfirm else VantafynSetupStep.ConnectServer,
                            username = "",
                            password = "",
                            errorMessage = null,
                            isLoading = false,
                        )
                    }
                }
                VantafynSetupStep.QuickConnect -> state.copy(
                    step = VantafynSetupStep.Login,
                    isLoading = false,
                    quickConnectSession = null,
                    quickConnectMessage = null,
                    errorMessage = null,
                )
                VantafynSetupStep.ProfilePicker -> {
                    if (state.server != null) {
                        state.copy(step = VantafynSetupStep.ServerConfirm, errorMessage = null, isLoading = false)
                    } else if (state.savedProfiles.isEmpty()) {
                        state.copy(step = VantafynSetupStep.Welcome, errorMessage = null, isLoading = false)
                    } else {
                        state
                    }
                }
                VantafynSetupStep.ConnectionRecovery -> state.copy(
                    step = if (state.savedProfiles.isEmpty()) VantafynSetupStep.Welcome else VantafynSetupStep.ProfilePicker,
                    restoreFailureProfile = null,
                    restoreFailureReason = null,
                    restoreFailureMessage = null,
                    errorMessage = null,
                    isLoading = false,
                )
            }
        }
    }

    fun addProfile() {
        val snapshot = _state.value
        if (snapshot.step == VantafynSetupStep.Home) {
            viewModelScope.launch {
                _state.update { it.copy(isLogoutTransitioning = true) }
                delay(LOGOUT_TRANSITION_DELAY_MS)
                applyAddProfileTarget(snapshot)
            }
            return
        }
        applyAddProfileTarget(snapshot)
    }

    private fun applyAddProfileTarget(snapshot: VantafynHomeUiState) {
        val existingServer = snapshot.server ?: snapshot.savedProfiles.maxByOrNull { it.lastUsedAt }?.let {
            JellyfinServerConfig(url = it.serverUrl, name = it.serverName, localId = it.serverRef)
        }
        if (existingServer != null) {
            _state.update {
                it.copy(
                    step = VantafynSetupStep.Login,
                    isLoading = false,
                    server = existingServer,
                    serverUrl = existingServer.url,
                    username = "",
                    password = "",
                    publicUsers = emptyList(),
                    manageProfiles = false,
                    pendingRemoval = null,
                    selectedProfileId = null,
                    restoreFailureProfile = null,
                    restoreFailureReason = null,
                    restoreFailureMessage = null,
                    errorMessage = null,
                    isLogoutTransitioning = false,
                )
            }
            return
        }
        _state.update {
            it.copy(
                step = VantafynSetupStep.ConnectServer,
                serverUrl = "",
                username = "",
                password = "",
                server = null,
                publicUsers = emptyList(),
                manageProfiles = false,
                pendingRemoval = null,
                errorMessage = null,
                isLogoutTransitioning = false,
            )
        }
    }

    fun showProfilePicker() {
        MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.ProfileSwitch)
        stopWatchPartyRealtime(clearInvites = true)
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
                    markSetupCompleted()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isStartupResolved = true,
                            password = "",
                            session = result.value,
                            server = result.value.server,
                            step = VantafynSetupStep.Home,
                            homeLayout = readHomeLayout(result.value.profileId),
                            themeMusicEnabled = readThemeMusicEnabled(result.value.profileId),
                            themeMusicVolume = readThemeMusicVolume(result.value.profileId),
                            selectedBackground = readSelectedBackground(result.value.profileId),
                            videoPlayerPreference = readVideoPlayerPreference(result.value.profileId),
                            configuredSmartRows = readSmartRows(result.value.profileId),
                            autoplayCountdownSeconds = readAutoplayCountdownSeconds(result.value.profileId),
                            passoutProtectionEnabled = readPassoutProtectionEnabled(result.value.profileId),
                            passoutProtectionLimitMinutes = readPassoutProtectionLimitMinutes(result.value.profileId),
                        )
                    }
                    refreshSavedProfiles()
                    startWatchPartyRealtime(result.value)
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
        stopWatchPartyRealtime(clearInvites = true)
        viewModelScope.launch {
            _state.update { it.copy(confirmLogout = false, isLogoutTransitioning = true) }
            PlaybackOutputCoordinator.get(getApplication()).clearForLogoutOrServerSwitch()
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.Logout)
            authRepository.logout()
            appPreferences.edit().putBoolean(KEY_SETUP_COMPLETED, false).apply()
            delay(LOGOUT_TRANSITION_DELAY_MS)
            _state.value = VantafynHomeUiState(step = VantafynSetupStep.Welcome, isStartupResolved = true)
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
        stopWatchPartyRealtime(clearInvites = true)
        viewModelScope.launch {
            _state.update { it.copy(confirmLogout = false, isLogoutTransitioning = true) }
            PlaybackOutputCoordinator.get(getApplication()).clearForLogoutOrServerSwitch()
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.ProfileSwitch)
            authRepository.removeProfile(profileId)
            val profiles = authRepository.savedProfiles()
            if (profiles.isEmpty()) {
                appPreferences.edit().putBoolean(KEY_SETUP_COMPLETED, false).apply()
            }
            delay(LOGOUT_TRANSITION_DELAY_MS)
            _state.value = VantafynHomeUiState(
                step = if (profiles.isEmpty()) VantafynSetupStep.Welcome else VantafynSetupStep.ProfilePicker,
                isStartupResolved = true,
                savedProfiles = profiles,
            )
        }
    }

    fun selectProfile(profile: SavedProfile, showPickerWhileRestoring: Boolean = true) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    step = if (showPickerWhileRestoring) VantafynSetupStep.ProfilePicker else VantafynSetupStep.Splash,
                    selectedProfileId = profile.id,
                    isLoading = true,
                    errorMessage = null,
                )
            }
            when (val result = authRepository.restoreSession(profile.id)) {
                is JellyfinResult.Success -> {
                    markSetupCompleted()
                    _state.update {
                        it.copy(
                            step = VantafynSetupStep.Home,
                            selectedProfileId = profile.id,
                            isLoading = false,
                            isStartupResolved = true,
                            session = result.value,
                            server = result.value.server,
                            serverUrl = result.value.server.url,
                            username = result.value.user.name,
                            password = "",
                            homeLayout = readHomeLayout(result.value.profileId),
                            themeMusicEnabled = readThemeMusicEnabled(result.value.profileId),
                            themeMusicVolume = readThemeMusicVolume(result.value.profileId),
                            selectedBackground = readSelectedBackground(result.value.profileId),
                            videoPlayerPreference = readVideoPlayerPreference(result.value.profileId),
                            configuredSmartRows = readSmartRows(result.value.profileId),
                            autoplayCountdownSeconds = readAutoplayCountdownSeconds(result.value.profileId),
                            passoutProtectionEnabled = readPassoutProtectionEnabled(result.value.profileId),
                            passoutProtectionLimitMinutes = readPassoutProtectionLimitMinutes(result.value.profileId),
                        )
                    }
                    refreshSavedProfiles()
                    startWatchPartyRealtime(result.value)
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
        selectProfile(profile, showPickerWhileRestoring = false)
    }

    fun saveRecoveryServerAddress() {
        val snapshot = _state.value
        val profile = snapshot.restoreFailureProfile ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.updateSavedServerUrl(profile.id, snapshot.serverUrl)) {
                is JellyfinResult.Success -> {
                    markSetupCompleted()
                    _state.update {
                        it.copy(
                            step = VantafynSetupStep.Home,
                            selectedProfileId = profile.id,
                            isLoading = false,
                            isStartupResolved = true,
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
                            videoPlayerPreference = readVideoPlayerPreference(result.value.profileId),
                            configuredSmartRows = readSmartRows(result.value.profileId),
                            autoplayCountdownSeconds = readAutoplayCountdownSeconds(result.value.profileId),
                            passoutProtectionEnabled = readPassoutProtectionEnabled(result.value.profileId),
                            passoutProtectionLimitMinutes = readPassoutProtectionLimitMinutes(result.value.profileId),
                        )
                    }
                    refreshSavedProfiles()
                    startWatchPartyRealtime(result.value)
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
                isStartupResolved = true,
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
                isStartupResolved = true,
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
            if (profiles.isEmpty()) {
                appPreferences.edit().putBoolean(KEY_SETUP_COMPLETED, false).apply()
            }
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
        if (destination == MobileDestination.WatchParty) {
            loadWatchPartyRecipients()
            if (_state.value.watchPartyCandidates.isEmpty()) loadWatchParty()
        } else if (_state.value.activeWatchParty == null) {
            stopWatchPartyRealtime(clearInvites = false)
        }
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
        openLibraryPage(library, startIndex = 0, filter = JellyfinLibraryItemFilter.All)
    }

    fun openLibraryPage(
        library: JellyfinLibrary,
        startIndex: Int,
        filter: JellyfinLibraryItemFilter = _state.value.libraryItemsFilter,
    ) {
        val session = _state.value.session ?: return
        _state.update {
            it.copy(
                mobileDestination = MobileDestination.LibraryDetail,
                previousMobileDestination = MobileDestination.Libraries,
                selectedLibrary = library,
                libraryItemsFilter = filter,
                libraryItems = emptyList(),
                libraryItemsPage = null,
                isLibraryItemsLoading = true,
                libraryItemsError = null,
                mobileMessage = null,
            )
        }
        viewModelScope.launch {
            when (val result = libraryRepository.getLibraryItemsPage(session, library, startIndex, LibraryItemsPageSize, filter)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            isLibraryItemsLoading = false,
                            libraryItems = result.value.items,
                            libraryItemsPage = result.value,
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isLibraryItemsLoading = false, libraryItemsError = result.message) }
                }
            }
        }
    }

    fun retryLibraryItems() {
        val state = _state.value
        val library = state.selectedLibrary ?: return
        openLibraryPage(library, state.libraryItemsPage?.startIndex ?: 0)
    }

    fun nextLibraryItemsPage() {
        val state = _state.value
        val library = state.selectedLibrary ?: return
        val page = state.libraryItemsPage ?: return
        if (page.hasNext) openLibraryPage(library, page.startIndex + page.pageSize, state.libraryItemsFilter)
    }

    fun previousLibraryItemsPage() {
        val state = _state.value
        val library = state.selectedLibrary ?: return
        val page = state.libraryItemsPage ?: return
        if (page.hasPrevious) openLibraryPage(library, (page.startIndex - page.pageSize).coerceAtLeast(0), state.libraryItemsFilter)
    }

    fun setLibraryItemsFilter(filter: JellyfinLibraryItemFilter) {
        val library = _state.value.selectedLibrary ?: return
        openLibraryPage(library, startIndex = 0, filter = filter)
    }

    fun reorderLibraries(orderedIds: List<UUID>) {
        _state.update { state ->
            val rank = orderedIds.mapIndexed { index, id -> id to index }.toMap()
            val currentIndex = state.libraries.mapIndexed { index, library -> library.id to index }.toMap()
            val updated = state.libraries.sortedWith(
                compareBy<JellyfinLibrary>(
                    { rank[it.id] ?: Int.MAX_VALUE },
                    { currentIndex[it.id] ?: Int.MAX_VALUE },
                ),
            )
            if (updated.map { it.id } == state.libraries.map { it.id }) return@update state
            persistLibraryOrder(state.session?.profileId, updated.map { it.id })
            state.copy(libraries = updated)
        }
    }

    fun openMedia(itemId: UUID) {
        val session = _state.value.session ?: return
        _state.update {
            it.copy(
                mobileDestination = MobileDestination.MediaDetail,
                previousMobileDestination = when (it.mobileDestination) {
                    MobileDestination.MediaDetail,
                    MobileDestination.Player -> it.previousMobileDestination
                    else -> it.mobileDestination
                },
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
            MobileDestination.MediaDetail -> navigateMobile(snapshot.previousMobileDestination)
            MobileDestination.LibraryDetail -> navigateMobile(MobileDestination.Libraries)
            MobileDestination.WatchParty -> navigateMobile(MobileDestination.Profile)
            MobileDestination.HomeLayout,
            MobileDestination.PlaybackPreferences -> navigateMobile(MobileDestination.Profile)
            MobileDestination.AdminUserSettings -> closeAdminUser()
            MobileDestination.Libraries,
            MobileDestination.Search,
            MobileDestination.Music,
            MobileDestination.Favorites,
            MobileDestination.Requests,
            MobileDestination.Admin,
            MobileDestination.Profile -> navigateMobile(MobileDestination.Home)
            else -> Unit
        }
    }

    fun toggleMediaFavorite() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val detail = snapshot.mediaDetail ?: return
        if (!detail.itemType.supportsMyListAction()) return
        val target = !detail.isFavorite
        val optimisticItem = detail.toFavoriteMediaItem(target)
        _state.update { it.withFavoriteState(detail.id, target, optimisticItem).copy(mobileMessage = null) }
        viewModelScope.launch {
            when (val result = mediaRepository.setFavorite(session, detail.id, target)) {
                is JellyfinResult.Success -> {
                    _state.update { state ->
                        state.withFavoriteState(detail.id, result.value, detail.toFavoriteMediaItem(result.value))
                    }
                    loadFavorites(session)
                }
                is JellyfinResult.Failure -> {
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
        if (!target?.itemType.supportsMyListAction()) return
        _state.update { it.withFavoriteState(itemId, isFavorite, target).copy(mobileMessage = null) }
        viewModelScope.launch {
            when (val result = mediaRepository.setFavorite(session, itemId, isFavorite)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.withFavoriteState(itemId, result.value, target?.copy(isFavorite = result.value)).copy(mobileMessage = null)
                    }
                    loadFavorites(session)
                    loadLibraries(session)
                }
                is JellyfinResult.Failure -> {
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
        val trimmed = query.trim()
        _state.update {
            it.copy(
                searchQuery = query,
                searchError = null,
                isSearchLoading = trimmed.length >= 2,
            )
        }
        searchJob?.cancel()
        if (trimmed.length < 2) {
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
        refreshAdminOverview(showLoading = true)
    }

    fun pollAdminOverview() {
        refreshAdminOverview(showLoading = false)
    }

    private fun refreshAdminOverview(showLoading: Boolean) {
        val session = _state.value.session ?: return
        if (!session.user.isAdministrator) return
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isAdminLoading = true, adminError = null) }
            }
            when (val result = adminRepository.getOverview(session, _state.value.libraries)) {
                is JellyfinResult.Success -> {
                    _state.update { state ->
                        val tracking = state.libraryScanTrackingAfter(result.value)
                        state.copy(
                            isAdminLoading = false,
                            adminOverview = result.value,
                            isLibraryScanTracking = tracking.isTracking,
                            libraryScanTrackingStartedAt = if (tracking.isTracking) {
                                state.libraryScanTrackingStartedAt
                            } else {
                                0L
                            },
                            hasObservedLibraryScanRunning = tracking.hasObservedRunning,
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    if (showLoading) {
                        _state.update { it.copy(isAdminLoading = false, adminError = result.message) }
                    }
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

    fun uploadCurrentUserProfileImage(bytes: ByteArray, mimeType: String) {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            _state.update { it.copy(isProfileImageSaving = true, profileImageError = null, mobileMessage = null) }
            val upload = runCatching { JellyfinProfileImageUpload(bytes, mimeType) }.getOrElse { throwable ->
                _state.update {
                    it.copy(
                        isProfileImageSaving = false,
                        profileImageError = throwable.message ?: "Couldn't read that image.",
                    )
                }
                return@launch
            }
            when (val result = userPreferencesRepository.uploadCurrentUserProfileImage(session, upload)) {
                is JellyfinResult.Success -> {
                    val profiles = authRepository.savedProfiles()
                    val publicUsers = refreshPublicUsers(result.value.server)
                    _state.update {
                        it.copy(
                            session = result.value,
                            server = result.value.server,
                            savedProfiles = profiles,
                            publicUsers = publicUsers,
                            isProfileImageSaving = false,
                            profileImageError = null,
                            mobileMessage = "Profile picture updated",
                        )
                    }
                    if (result.value.user.isAdministrator) refreshAdminOverview(showLoading = false)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isProfileImageSaving = false,
                        profileImageError = result.message,
                        mobileMessage = result.message,
                    )
                }
            }
        }
    }

    fun deleteCurrentUserProfileImage() {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            _state.update { it.copy(isProfileImageSaving = true, profileImageError = null, mobileMessage = null) }
            when (val result = userPreferencesRepository.deleteCurrentUserProfileImage(session)) {
                is JellyfinResult.Success -> {
                    val profiles = authRepository.savedProfiles()
                    val publicUsers = refreshPublicUsers(result.value.server)
                    _state.update {
                        it.copy(
                            session = result.value,
                            server = result.value.server,
                            savedProfiles = profiles,
                            publicUsers = publicUsers,
                            isProfileImageSaving = false,
                            profileImageError = null,
                            mobileMessage = "Profile picture removed",
                        )
                    }
                    if (result.value.user.isAdministrator) refreshAdminOverview(showLoading = false)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isProfileImageSaving = false,
                        profileImageError = result.message,
                        mobileMessage = result.message,
                    )
                }
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

    fun uploadSelectedAdminUserProfileImage(bytes: ByteArray, mimeType: String) {
        val session = _state.value.session ?: return
        val detail = _state.value.adminUserDetail ?: return
        if (!session.user.isAdministrator) return
        viewModelScope.launch {
            _state.update { it.copy(isProfileImageSaving = true, adminUserError = null, mobileMessage = null) }
            val upload = runCatching { JellyfinProfileImageUpload(bytes, mimeType) }.getOrElse { throwable ->
                _state.update {
                    it.copy(
                        isProfileImageSaving = false,
                        adminUserError = throwable.message ?: "Couldn't read that image.",
                    )
                }
                return@launch
            }
            when (val result = adminRepository.uploadUserProfileImage(session, detail.user.id, upload)) {
                is JellyfinResult.Success -> {
                    val updatedSession = if (detail.user.id == session.user.id) {
                        authRepository.restoreSession(session.profileId)
                    } else {
                        JellyfinResult.Success(session)
                    }
                    val sessionToStore = (updatedSession as? JellyfinResult.Success)?.value ?: _state.value.session
                    _state.update {
                        it.copy(
                            session = sessionToStore,
                            adminUserDetail = result.value,
                            isProfileImageSaving = false,
                            adminUserError = null,
                            mobileMessage = "Profile picture updated",
                        )
                    }
                    loadAdminOverview()
                    refreshSavedProfileImages()
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isProfileImageSaving = false,
                        adminUserError = result.message,
                        mobileMessage = result.message,
                    )
                }
            }
        }
    }

    fun deleteSelectedAdminUserProfileImage() {
        val session = _state.value.session ?: return
        val detail = _state.value.adminUserDetail ?: return
        if (!session.user.isAdministrator) return
        viewModelScope.launch {
            _state.update { it.copy(isProfileImageSaving = true, adminUserError = null, mobileMessage = null) }
            when (val result = adminRepository.deleteUserProfileImage(session, detail.user.id)) {
                is JellyfinResult.Success -> {
                    val updatedCurrentSession = if (detail.user.id == session.user.id) {
                        authRepository.restoreSession(session.profileId)
                    } else {
                        JellyfinResult.Success(session)
                    }
                    val sessionToStore = (updatedCurrentSession as? JellyfinResult.Success)?.value ?: _state.value.session
                    _state.update {
                        it.copy(
                            session = sessionToStore,
                            adminUserDetail = result.value,
                            isProfileImageSaving = false,
                            adminUserError = null,
                            mobileMessage = "Profile picture removed",
                        )
                    }
                    loadAdminOverview()
                    refreshSavedProfileImages()
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isProfileImageSaving = false,
                        adminUserError = result.message,
                        mobileMessage = result.message,
                    )
                }
            }
        }
    }

    fun setAdminPluginEnabled(pluginId: UUID, version: String?, enabled: Boolean) {
        val session = _state.value.session ?: return
        if (!session.user.isAdministrator) return
        val safeVersion = version?.takeIf { it.isNotBlank() } ?: run {
            _state.update { it.copy(mobileMessage = "Plugin version is unavailable") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isAdminActionRunning = true, adminError = null, mobileMessage = null) }
            when (val result = adminRepository.setPluginEnabled(session, pluginId, safeVersion, enabled)) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isAdminActionRunning = false, mobileMessage = if (enabled) "Plugin enabled" else "Plugin disabled") }
                    refreshAdminOverview(showLoading = false)
                }
                is JellyfinResult.Failure -> _state.update { it.copy(isAdminActionRunning = false, mobileMessage = result.message) }
            }
        }
    }

    fun scanAdminLibrary() {
        val session = _state.value.session ?: return
        if (!session.user.isAdministrator) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isAdminActionRunning = true,
                    isLibraryScanTracking = true,
                    libraryScanTrackingStartedAt = System.currentTimeMillis(),
                    hasObservedLibraryScanRunning = false,
                    adminError = null,
                    mobileMessage = null,
                )
            }
            when (val result = adminRepository.scanLibrary(session)) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isAdminActionRunning = false, mobileMessage = null) }
                    refreshAdminOverview(showLoading = false)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isAdminActionRunning = false,
                        isLibraryScanTracking = false,
                        libraryScanTrackingStartedAt = 0L,
                        hasObservedLibraryScanRunning = false,
                        mobileMessage = result.message,
                    )
                }
            }
        }
    }

    fun runAdminTask(taskId: String) {
        setAdminTaskRunning(taskId, running = true)
    }

    fun stopAdminTask(taskId: String) {
        setAdminTaskRunning(taskId, running = false)
    }

    private fun setAdminTaskRunning(taskId: String, running: Boolean) {
        val session = _state.value.session ?: return
        if (!session.user.isAdministrator) return
        viewModelScope.launch {
            _state.update { it.copy(isAdminActionRunning = true, adminError = null, mobileMessage = null) }
            when (val result = adminRepository.setScheduledTaskRunning(session, taskId, running)) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(isAdminActionRunning = false, mobileMessage = if (running) "Task started" else "Task stopped") }
                    refreshAdminOverview(showLoading = false)
                }
                is JellyfinResult.Failure -> _state.update { it.copy(isAdminActionRunning = false, mobileMessage = result.message) }
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
                itemType = "Episode",
                seriesId = episode.seriesId ?: detail?.id,
                seasonId = episode.seasonId,
                seriesName = episode.seriesName ?: detail?.title,
                seasonNumber = episode.seasonIndexNumber,
                episodeNumber = episode.indexNumber,
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
        previousStopPositionTicks: Long? = null,
        continuousPlaybackStartedAtMs: Long? = null,
    ) {
        viewModelScope.launch {
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.VideoPlayback)
            val previousInfo = _state.value.playbackInfo
            val autoplayWindowStartedAtMs = continuousPlaybackStartedAtMs ?: System.currentTimeMillis()
            if (previousInfo != null) {
                playbackRepository.reportStopped(session, previousInfo, previousStopPositionTicks ?: previousInfo.startPositionTicks)
            }
            val upNextCandidate = loadUpNextCandidate(session, target)
            val previousCandidate = loadPreviousEpisodeCandidate(target)
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
                            playbackItem = result.value.toPlaybackItem(
                                target = target,
                                previousCandidate = previousCandidate,
                                upNextCandidate = upNextCandidate,
                                autoplaySettings = it.autoplaySettings(),
                                continuousPlaybackStartedAtMs = autoplayWindowStartedAtMs,
                            ),
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

    fun loadWatchParty() {
        val session = _state.value.session ?: return
        startWatchPartyRealtime(session)
        viewModelScope.launch {
            _state.update { it.copy(isWatchPartyLoading = true, watchPartyError = null) }
            val rules = _state.value.watchPartyRules
            when (val result = watchPartyRepository.getCandidates(session, rules, limit = 48)) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(
                        isWatchPartyLoading = false,
                        watchPartyCandidates = result.value,
                        watchPartyCurrentIndex = 0,
                        watchPartyVotes = emptyList(),
                        watchPartyMatch = null,
                        watchPartyError = null,
                    )
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isWatchPartyLoading = false, watchPartyError = result.message)
                }
            }
        }
    }

    fun createWatchParty() {
        val session = _state.value.session ?: return
        startWatchPartyRealtime(session)
        viewModelScope.launch {
            _state.update { it.copy(isWatchPartyLoading = true, watchPartyError = null) }
            val rules = _state.value.watchPartyRules
            when (
                val result = watchPartyRepository.createSyncPlayGroup(
                    session = session,
                    name = _state.value.watchPartyName,
                    rules = rules,
                    mode = _state.value.watchPartyMode,
                    selectedMedia = _state.value.watchPartySelectedMedia,
                )
            ) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            isWatchPartyLoading = false,
                            activeWatchParty = result.value,
                            watchPartyError = null,
                        )
                    }
                    loadWatchParty()
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isWatchPartyLoading = false,
                        activeWatchParty = null,
                        watchPartyError = "SyncPlay is not available from this Jellyfin session: ${result.message}",
                    )
                }
            }
        }
    }

    fun leaveWatchParty() {
        val session = _state.value.session
        stopWatchPartyRealtime()
        _state.update {
            it.copy(
                activeWatchParty = null,
                watchPartyVotes = emptyList(),
                watchPartyMatch = null,
                watchPartyRealtimeMembers = emptyList(),
                watchPartyRealtimeConnectionState = SyncPlayConnectionState.Disconnected,
                watchPartySyncStateLabel = "Solo fallback",
                watchPartyError = null,
            )
        }
        if (session != null) {
            viewModelScope.launch { watchPartyRepository.leaveSyncPlayGroup(session) }
        }
    }

    fun updateWatchPartyName(value: String) {
        _state.update { it.copy(watchPartyName = value, watchPartyError = null) }
    }

    fun startWatchPartyFromDetail(mode: WatchPartyMode) {
        val detail = _state.value.mediaDetail ?: return
        val selectedMedia = if (mode == WatchPartyMode.FixedTitle) detail.toWatchPartySelectedMedia() else null
        _state.update {
            it.copy(
                watchPartyMode = mode,
                watchPartySelectedMedia = selectedMedia,
                watchPartyName = it.watchPartyName.ifBlank { "${it.session?.user?.name ?: "Vantafyn"} Watch Party" },
                mobileDestination = MobileDestination.WatchParty,
                previousMobileDestination = MobileDestination.MediaDetail,
                watchPartyError = null,
            )
        }
        loadWatchPartyRecipients()
        if (mode == WatchPartyMode.SwipeToMatch && _state.value.watchPartyCandidates.isEmpty()) {
            loadWatchParty()
        }
    }

    fun updateWatchPartyMode(mode: WatchPartyMode) {
        _state.update {
            it.copy(
                watchPartyMode = mode,
                watchPartySelectedMedia = if (mode == WatchPartyMode.FixedTitle) it.watchPartySelectedMedia else null,
                watchPartyError = null,
            )
        }
    }

    fun updateWatchPartyRules(rules: WatchPartyRules) {
        _state.update {
            it.copy(
                watchPartyRules = rules,
                watchPartyCandidates = emptyList(),
                watchPartyCurrentIndex = 0,
                watchPartyVotes = emptyList(),
                watchPartyMatch = null,
                watchPartyError = null,
            )
        }
    }

    fun loadWatchPartyRecipients() {
        val session = _state.value.session ?: return
        startWatchPartyRealtime(session)
        viewModelScope.launch {
            _state.update { it.copy(isWatchPartyRecipientsLoading = true, watchPartyError = null) }
            when (val result = watchPartyRepository.getInviteRecipients(session)) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(
                        isWatchPartyRecipientsLoading = false,
                        watchPartyInviteRecipients = result.value,
                        watchPartyError = null,
                    )
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isWatchPartyRecipientsLoading = false,
                        watchPartyInviteRecipients = emptyList(),
                        watchPartyError = "Invite recipients are available only when Jellyfin exposes active sessions: ${result.message}",
                    )
                }
            }
        }
    }

    fun toggleWatchPartyRecipient(sessionId: String) {
        _state.update {
            val selected = if (sessionId in it.selectedWatchPartyRecipientSessionIds) {
                it.selectedWatchPartyRecipientSessionIds - sessionId
            } else {
                it.selectedWatchPartyRecipientSessionIds + sessionId
            }
            it.copy(selectedWatchPartyRecipientSessionIds = selected, watchPartyError = null)
        }
    }

    fun sendWatchPartyInvites() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        if (!snapshot.watchPartyEnabled) {
            _state.update { it.copy(watchPartyError = "Watch Party is disabled in settings.") }
            return
        }
        val party = snapshot.activeWatchParty
        val selectedSessions = snapshot.selectedWatchPartyRecipientSessionIds
        if (selectedSessions.isEmpty()) {
            _state.update { it.copy(watchPartyError = "Choose at least one active recipient first.") }
            return
        }
        viewModelScope.launch {
            val ensuredParty = if (party == null) {
                _state.update { it.copy(isWatchPartyLoading = true, watchPartyError = null) }
                when (
                    val created = watchPartyRepository.createSyncPlayGroup(
                        session = session,
                        name = snapshot.watchPartyName,
                        rules = snapshot.watchPartyRules,
                        mode = snapshot.watchPartyMode,
                        selectedMedia = snapshot.watchPartySelectedMedia,
                    )
                ) {
                    is JellyfinResult.Success -> created.value
                    is JellyfinResult.Failure -> {
                        _state.update {
                            it.copy(
                                isWatchPartyLoading = false,
                                watchPartyError = "Could not create SyncPlay group before inviting: ${created.message}",
                            )
                        }
                        return@launch
                    }
                }
            } else {
                party
            }
            val recipients = snapshot.watchPartyInviteRecipients.filter { it.sessionId in selectedSessions }
            val now = System.currentTimeMillis()
            val invite = WatchPartyInvite(
                inviteId = UUID.randomUUID(),
                partyId = ensuredParty.id,
                serverAccountId = session.server.serverId ?: session.server.localId,
                mode = snapshot.watchPartyMode,
                mediaItemId = snapshot.watchPartySelectedMedia?.id,
                mediaType = snapshot.watchPartySelectedMedia?.itemType,
                mediaTitle = snapshot.watchPartySelectedMedia?.title,
                mediaArtworkUrl = snapshot.watchPartySelectedMedia?.artworkUrl,
                hostUserId = session.user.id,
                hostDisplayName = session.user.name,
                recipientUserId = recipients.firstOrNull()?.userId,
                recipientDisplayName = recipients.joinToString(", ") { it.displayName },
                createdAt = now,
                expiresAt = now + snapshot.watchPartyInviteExpirySeconds * 1_000L,
                status = WatchPartyInviteStatus.Pending,
            )
            when (val result = watchPartyRepository.sendInvite(session, invite, selectedSessions.toList())) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(
                        isWatchPartyLoading = false,
                        activeWatchParty = ensuredParty,
                        sentWatchPartyInvites = it.sentWatchPartyInvites + invite,
                        selectedWatchPartyRecipientSessionIds = emptySet(),
                        showWatchPartyInviteSentAnimation = it.watchPartyInviteAnimationEnabled,
                        watchPartyError = null,
                    )
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isWatchPartyLoading = false,
                        activeWatchParty = ensuredParty,
                        watchPartyError = "Invite could not be delivered through Jellyfin active sessions: ${result.message}",
                    )
                }
            }
        }
    }

    fun clearWatchPartyInviteAnimation() {
        _state.update { it.copy(showWatchPartyInviteSentAnimation = false) }
    }

    fun toggleWatchPartyEnabled() {
        val enabled = !_state.value.watchPartyEnabled
        appPreferences.edit().putBoolean(KEY_WATCH_PARTY_ENABLED, enabled).apply()
        _state.update { it.copy(watchPartyEnabled = enabled) }
        if (!enabled) leaveWatchParty()
    }

    fun toggleWatchPartyInvitesEnabled() {
        val enabled = !_state.value.watchPartyInvitesEnabled
        appPreferences.edit().putBoolean(KEY_WATCH_PARTY_INVITES_ENABLED, enabled).apply()
        _state.update {
            it.copy(
                watchPartyInvitesEnabled = enabled,
                incomingWatchPartyInvites = if (enabled) it.incomingWatchPartyInvites else emptyList(),
            )
        }
    }

    fun toggleWatchPartyInviteAnimationEnabled() {
        val enabled = !_state.value.watchPartyInviteAnimationEnabled
        appPreferences.edit().putBoolean(KEY_WATCH_PARTY_INVITE_ANIMATION_ENABLED, enabled).apply()
        _state.update { it.copy(watchPartyInviteAnimationEnabled = enabled) }
    }

    fun setWatchPartyInviteExpirySeconds(seconds: Int) {
        val value = seconds.takeIf { it in WATCH_PARTY_INVITE_EXPIRY_OPTIONS } ?: return
        appPreferences.edit().putInt(KEY_WATCH_PARTY_INVITE_EXPIRY_SECONDS, value).apply()
        _state.update { it.copy(watchPartyInviteExpirySeconds = value) }
    }

    fun onAppForegrounded() {
        isAppForeground = true
        _state.value.session
            ?.takeIf { shouldUseWatchPartyRealtime(_state.value) }
            ?.let { startWatchPartyRealtime(it) }
    }

    fun onAppBackgrounded() {
        isAppForeground = false
        if (_state.value.activeWatchParty == null) {
            stopWatchPartyRealtime(clearInvites = false)
        }
    }

    fun acceptIncomingWatchPartyInvite() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val invite = snapshot.activeIncomingWatchPartyInvite ?: return
        if (invite.expiresAt <= System.currentTimeMillis()) {
            expireIncomingWatchPartyInvite(invite.inviteId)
            return
        }
        viewModelScope.launch {
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.VideoPlayback)
            _state.update {
                it.copy(
                    incomingWatchPartyInvites = it.incomingWatchPartyInvites.map { queued ->
                        if (queued.inviteId == invite.inviteId) queued.copy(status = WatchPartyInviteStatus.Accepted) else queued
                    },
                    incomingWatchPartyMessage = "Joining ${invite.hostDisplayName}'s Watch Party",
                    watchPartyError = null,
                )
            }
            val rules = snapshot.watchPartyRules
            when (val result = watchPartyRepository.joinSyncPlayGroup(session, invite.partyId, rules)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            activeWatchParty = WatchPartySession(
                                id = invite.partyId,
                                name = "${invite.hostDisplayName}'s Watch Party",
                                serverId = session.server.serverId ?: session.server.localId,
                                serverName = session.server.name,
                                role = dev.vantafyn.core.jellyfin.WatchPartyRole.Participant,
                                rules = rules,
                                mode = invite.mode,
                                selectedMedia = invite.toSelectedMedia(),
                                members = listOf(
                                    dev.vantafyn.core.jellyfin.WatchPartyMember(
                                        id = session.user.id.toString(),
                                        displayName = session.user.name,
                                        role = dev.vantafyn.core.jellyfin.WatchPartyRole.Participant,
                                    ),
                                ),
                            ),
                            watchPartyMode = invite.mode,
                            watchPartySelectedMedia = invite.toSelectedMedia(),
                            mobileDestination = MobileDestination.WatchParty,
                            incomingWatchPartyInvites = it.incomingWatchPartyInvites.filterNot { queued -> queued.inviteId == invite.inviteId },
                            incomingWatchPartyMessage = "${invite.hostDisplayName} joined you to the lobby",
                            watchPartyError = null,
                        )
                    }
                    startWatchPartyRealtime(session)
                    if (invite.mode == WatchPartyMode.SwipeToMatch) loadWatchParty()
                }
                is JellyfinResult.Failure -> {
                    _state.update {
                        it.copy(
                            incomingWatchPartyInvites = it.incomingWatchPartyInvites.filterNot { queued -> queued.inviteId == invite.inviteId },
                            watchPartyError = "Could not join Watch Party: ${result.message}",
                        )
                    }
                }
            }
        }
    }

    fun declineIncomingWatchPartyInvite() {
        val invite = _state.value.activeIncomingWatchPartyInvite ?: return
        _state.update {
            it.copy(
                incomingWatchPartyInvites = it.incomingWatchPartyInvites.filterNot { queued -> queued.inviteId == invite.inviteId },
                incomingWatchPartyMessage = "Invite declined",
            )
        }
        _state.value.activeIncomingWatchPartyInvite?.let { scheduleInviteExpiry(it) }
    }

    fun clearIncomingWatchPartyMessage() {
        _state.update { it.copy(incomingWatchPartyMessage = null) }
    }

    fun toggleWatchPartyReady() {
        val userId = _state.value.session?.user?.id ?: return
        _state.update { state ->
            val current = state.localWatchPartyReadyStates[userId] == WatchPartyMemberReadyStatus.Ready
            state.copy(
                localWatchPartyReadyStates = state.localWatchPartyReadyStates + (
                    userId to if (current) WatchPartyMemberReadyStatus.NotReady else WatchPartyMemberReadyStatus.Ready
                    ),
            )
        }
    }

    private fun startWatchPartyRealtime(session: JellyfinSession) {
        val snapshot = _state.value
        if (!isAppForeground && snapshot.activeWatchParty == null) return
        if (!shouldUseWatchPartyRealtime(snapshot)) {
            _state.update {
                it.copy(
                    watchPartyRealtimeConnectionState = SyncPlayConnectionState.Disconnected,
                    watchPartySyncStateLabel = "Sync idle",
                    watchPartyRealtimeError = null,
                )
            }
            return
        }
        if (watchPartyRealtimeJob?.isActive == true) return
        LongRunningTaskRegistry.start(
            id = WATCH_PARTY_REALTIME_TASK_ID,
            type = LongRunningTaskType.WebSocket,
            owner = "WatchPartyRealtime",
            state = "connecting",
        )
        watchPartyRealtimeJob = viewModelScope.launch {
            var reconnectDelayMs = 1_000L
            while (isActive && shouldUseWatchPartyRealtime(_state.value)) {
                realtimeClient.events(session)
                    .catch { throwable ->
                        _state.update {
                            it.copy(
                                watchPartyRealtimeConnectionState = SyncPlayConnectionState.Failed,
                                watchPartySyncStateLabel = "Reconnect",
                                watchPartyRealtimeError = throwable.message ?: "Realtime connection failed",
                            )
                        }
                    }
                    .collect { event ->
                        if (event is JellyfinWebSocketEvent.ConnectionChanged && event.state == SyncPlayConnectionState.Connected) {
                            reconnectDelayMs = 1_000L
                        }
                        LongRunningTaskRegistry.tick(WATCH_PARTY_REALTIME_TASK_ID, event::class.simpleName ?: "event")
                        reduceWatchPartyRealtimeEvent(event)
                    }
                if (!shouldUseWatchPartyRealtime(_state.value)) break
                _state.update {
                    it.copy(
                        watchPartyRealtimeConnectionState = SyncPlayConnectionState.Reconnecting,
                        watchPartySyncStateLabel = "Reconnecting",
                    )
                }
                delay(reconnectDelayMs)
                reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)
            }
            LongRunningTaskRegistry.stop(WATCH_PARTY_REALTIME_TASK_ID, "not needed")
        }
    }

    private fun stopWatchPartyRealtime(clearInvites: Boolean = false) {
        watchPartyRealtimeJob?.cancel()
        watchPartyRealtimeJob = null
        LongRunningTaskRegistry.stop(WATCH_PARTY_REALTIME_TASK_ID, "stopped")
        if (clearInvites) {
            watchPartyInviteExpiryJob?.cancel()
            watchPartyInviteExpiryJob = null
            _state.update { it.copy(incomingWatchPartyInvites = emptyList(), incomingWatchPartyMessage = null) }
        }
    }

    private fun shouldUseWatchPartyRealtime(state: VantafynHomeUiState): Boolean =
        state.activeWatchParty != null ||
            state.mobileDestination == MobileDestination.WatchParty ||
            state.isWatchPartyRecipientsLoading ||
            state.isWatchPartyLoading

    private fun reduceWatchPartyRealtimeEvent(event: JellyfinWebSocketEvent) {
        when (event) {
            is JellyfinWebSocketEvent.ConnectionChanged -> _state.update {
                it.copy(
                    watchPartyRealtimeConnectionState = event.state,
                    watchPartySyncStateLabel = when (event.state) {
                        SyncPlayConnectionState.Connected -> if (it.activeWatchParty != null) "Watch Party active" else "Connected"
                        SyncPlayConnectionState.Connecting,
                        SyncPlayConnectionState.Reconnecting -> "Reconnecting"
                        SyncPlayConnectionState.Disconnected -> "Sync unknown"
                        SyncPlayConnectionState.Unsupported -> "Sync unavailable"
                        SyncPlayConnectionState.Failed -> "Reconnect"
                    },
                    watchPartyRealtimeError = event.message,
                )
            }
            is JellyfinWebSocketEvent.SessionsUpdated -> _state.update {
                it.copy(
                    watchPartyRealtimeMembers = event.members,
                    watchPartySyncStateLabel = if (it.activeWatchParty != null) "Watch Party active" else it.watchPartySyncStateLabel,
                )
            }
            is JellyfinWebSocketEvent.SyncPlayGroupUpdated -> _state.update {
                val currentGroup = it.activeWatchParty?.id
                if (event.groupId != null && currentGroup != null && event.groupId != currentGroup) {
                    it
                } else {
                    it.copy(
                        watchPartyLastGroupUpdate = event.updateType,
                        watchPartySyncStateLabel = when (event.updateType.lowercase()) {
                            "stateupdate", "state_update", "playqueue", "play_queue" -> "Sync state unavailable"
                            "groupjoined", "group_joined", "userjoined", "user_joined" -> "Watch Party active"
                            "groupleft", "group_left", "notingroup", "not_in_group" -> "Solo fallback"
                            else -> "Watch Party active"
                        },
                    )
                }
            }
            is JellyfinWebSocketEvent.SyncPlayCommandReceived -> _state.update {
                it.copy(
                    watchPartyLastSyncCommand = event.command,
                    watchPartyPlaybackState = it.watchPartyPlaybackState.copy(
                        itemId = event.playlistItemId,
                        positionTicks = event.positionTicks ?: it.watchPartyPlaybackState.positionTicks,
                        isPlaying = !event.command.equals("Pause", ignoreCase = true),
                    ),
                    watchPartySyncStateLabel = "Watch Party active",
                )
            }
            is JellyfinWebSocketEvent.PlaystateCommandReceived -> _state.update {
                it.copy(
                    watchPartyLastSyncCommand = event.command,
                    watchPartySyncStateLabel = "Watch Party active",
                )
            }
            is JellyfinWebSocketEvent.GeneralCommandReceived -> _state.update {
                if (!it.watchPartyInvitesEnabled) return@update it.copy(watchPartyLastSyncCommand = event.command)
                val invite = WatchPartyInviteEventMapper.fromGeneralCommand(
                    event = event,
                    fallbackServerAccountId = it.session?.server?.serverId ?: it.session?.server?.localId,
                    recipientUserId = it.session?.user?.id,
                    recipientDisplayName = it.session?.user?.name ?: "You",
                )
                if (invite == null) {
                    it.copy(watchPartyLastSyncCommand = event.command)
                } else {
                    enqueueIncomingWatchPartyInvite(it, invite)
                }
            }
            is JellyfinWebSocketEvent.UnknownMessage -> Unit
            is JellyfinWebSocketEvent.Error -> _state.update {
                it.copy(
                    watchPartyRealtimeConnectionState = SyncPlayConnectionState.Failed,
                    watchPartySyncStateLabel = if (event.recoverable) "Reconnect" else "Sync unavailable",
                    watchPartyRealtimeError = event.message,
                )
            }
        }
    }

    private fun enqueueIncomingWatchPartyInvite(state: VantafynHomeUiState, invite: WatchPartyInvite): VantafynHomeUiState {
        val accountId = state.session?.server?.serverId ?: state.session?.server?.localId
        if (invite.serverAccountId != null && accountId != null && invite.serverAccountId != accountId) return state
        if (invite.hostUserId == state.session?.user?.id) return state
        if (invite.expiresAt <= System.currentTimeMillis()) return state.copy(incomingWatchPartyMessage = "Watch Party invite expired")
        val queue = (state.incomingWatchPartyInvites.filterNot { it.inviteId == invite.inviteId } + invite)
            .sortedBy { it.createdAt }
        val activeInvite = state.activeIncomingWatchPartyInvite
        if (activeInvite == null || activeInvite.inviteId == invite.inviteId) {
            scheduleInviteExpiry(queue.first())
        }
        return state.copy(
            incomingWatchPartyInvites = queue,
            incomingWatchPartyMessage = null,
            watchPartyLastSyncCommand = "Watch Party invite",
        )
    }

    private fun scheduleInviteExpiry(invite: WatchPartyInvite) {
        watchPartyInviteExpiryJob?.cancel()
        watchPartyInviteExpiryJob = viewModelScope.launch {
            val delayMs = (invite.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(delayMs)
            expireIncomingWatchPartyInvite(invite.inviteId)
        }
    }

    private fun expireIncomingWatchPartyInvite(inviteId: UUID) {
        _state.update {
            it.copy(
                incomingWatchPartyInvites = it.incomingWatchPartyInvites.filterNot { invite -> invite.inviteId == inviteId },
                incomingWatchPartyMessage = "Watch Party invite expired",
            )
        }
        _state.value.activeIncomingWatchPartyInvite?.let { scheduleInviteExpiry(it) }
    }

    fun voteWatchPartyCandidate(value: WatchPartyVoteValue) {
        val snapshot = _state.value
        val candidate = snapshot.currentWatchPartyCandidate ?: return
        val memberId = snapshot.session?.user?.id?.toString() ?: return
        val vote = WatchPartyVote(candidate.id, memberId, value)
        val votes = snapshot.watchPartyVotes.filterNot { it.candidateId == candidate.id && it.memberId == memberId } + vote
        val match = if (value == WatchPartyVoteValue.Yes && snapshot.watchPartyRules.isMatched(votes, candidate.id, memberCount = 1)) {
            WatchPartyMatch(candidate, votes.filter { it.candidateId == candidate.id })
        } else {
            null
        }
        _state.update {
            it.copy(
                watchPartyVotes = votes,
                watchPartyCurrentIndex = (it.watchPartyCurrentIndex + 1).coerceAtMost(it.watchPartyCandidates.size),
                watchPartyMatch = match ?: it.watchPartyMatch,
            )
        }
    }

    fun startMatchedWatchPartyPlayback() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val candidate = snapshot.watchPartyMatch?.candidate ?: return
        viewModelScope.launch {
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.VideoPlayback)
            watchPartyRepository.sendSyncPlayCommand(session, SyncPlayCommand.StartItem(candidate.id, 0L))
            startPlaybackTarget(
                session = session,
                target = PlaybackTarget(
                    id = candidate.id,
                    title = candidate.title,
                    subtitle = candidate.subtitle,
                    startTicks = 0L,
                    itemType = candidate.itemType,
                ),
                forceTranscode = false,
                audioStreamIndex = null,
                subtitleStreamIndex = null,
            )
        }
    }

    fun startFixedWatchPartyPlayback() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val media = snapshot.watchPartySelectedMedia ?: snapshot.activeWatchParty?.selectedMedia ?: return
        viewModelScope.launch {
            MusicPlaybackController.get(getApplication()).stop(clearQueue = true, reason = VantafynMusicStopReason.VideoPlayback)
            watchPartyRepository.sendSyncPlayCommand(session, SyncPlayCommand.StartItem(media.id, 0L))
            startPlaybackTarget(
                session = session,
                target = PlaybackTarget(
                    id = media.id,
                    title = media.title,
                    subtitle = media.subtitle,
                    startTicks = 0L,
                    itemType = media.itemType,
                ),
                forceTranscode = false,
                audioStreamIndex = null,
                subtitleStreamIndex = null,
            )
        }
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

    fun playNextEpisode(candidate: UpNextCandidate, currentPositionMs: Long) {
        playAdjacentEpisode(candidate, currentPositionMs)
    }

    fun playPreviousEpisode(candidate: UpNextCandidate, currentPositionMs: Long) {
        playAdjacentEpisode(candidate, currentPositionMs)
    }

    private fun playAdjacentEpisode(candidate: UpNextCandidate, currentPositionMs: Long) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val id = runCatching { UUID.fromString(candidate.itemId) }.getOrNull() ?: return
        _state.update { it.copy(mobileMessage = null) }
        startPlaybackTarget(
            session = session,
            target = PlaybackTarget(
                id = id,
                title = candidate.title,
                subtitle = listOfNotNull(candidate.seriesName, candidate.episodeLabel).joinToString(" · ").ifBlank { null },
                startTicks = candidate.playbackPositionMs.toTicks(),
                itemType = "Episode",
                seriesId = candidate.seriesId?.let { runCatching { UUID.fromString(it) }.getOrNull() },
                seasonId = candidate.seasonId?.let { runCatching { UUID.fromString(it) }.getOrNull() },
                seriesName = candidate.seriesName,
                seasonNumber = candidate.seasonNumber,
                episodeNumber = candidate.episodeNumber,
            ),
            forceTranscode = false,
            audioStreamIndex = null,
            subtitleStreamIndex = null,
            previousStopPositionTicks = currentPositionMs.toTicks(),
            continuousPlaybackStartedAtMs = snapshot.playbackItem?.continuousPlaybackStartedAtMs,
        )
    }

    fun setAutoplayCountdownSeconds(seconds: Int) {
        if (seconds !in AUTOPLAY_COUNTDOWN_OPTIONS) return
        _state.update { state ->
            val editor = appPreferences.edit().putInt(KEY_AUTOPLAY_COUNTDOWN_SECONDS, seconds)
            state.session?.profileId?.let { profileId ->
                editor.putInt("${KEY_AUTOPLAY_COUNTDOWN_SECONDS}_$profileId", seconds)
            }
            editor.apply()
            state.copy(autoplayCountdownSeconds = seconds)
        }
    }

    fun togglePassoutProtection() {
        _state.update { state ->
            val enabled = !state.passoutProtectionEnabled
            val editor = appPreferences.edit().putBoolean(KEY_PASSOUT_PROTECTION_ENABLED, enabled)
            state.session?.profileId?.let { profileId ->
                editor.putBoolean("${KEY_PASSOUT_PROTECTION_ENABLED}_$profileId", enabled)
            }
            editor.apply()
            state.copy(passoutProtectionEnabled = enabled)
        }
    }

    fun setPassoutProtectionLimitMinutes(minutes: Int) {
        if (minutes !in PASSOUT_PROTECTION_LIMIT_OPTIONS) return
        _state.update { state ->
            val editor = appPreferences.edit().putInt(KEY_PASSOUT_PROTECTION_LIMIT_MINUTES, minutes)
            state.session?.profileId?.let { profileId ->
                editor.putInt("${KEY_PASSOUT_PROTECTION_LIMIT_MINUTES}_$profileId", minutes)
            }
            editor.apply()
            state.copy(passoutProtectionLimitMinutes = minutes)
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
        val positionTicks = positionMs.toTicks()
        _state.update { state ->
            state.copy(
                playbackItem = state.playbackItem?.copy(startPositionMs = positionMs.coerceAtLeast(0L)),
                activePlaybackTarget = state.activePlaybackTarget?.copy(startTicks = positionTicks),
                playbackInfo = state.playbackInfo?.copy(startPositionTicks = positionTicks),
            )
        }
        val session = _state.value.session ?: return
        val info = _state.value.playbackInfo ?: return
        viewModelScope.launch {
            playbackRepository.reportProgress(session, info, positionTicks, isPaused)
        }
    }

    fun prepareCastPlayback(positionMs: Long) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val target = snapshot.activePlaybackTarget ?: return
        if (snapshot.isPlaybackLoading || snapshot.playbackItem?.isCastResolved == true) return
        viewModelScope.launch {
            _state.update { it.copy(isPlaybackLoading = true, playbackError = null) }
            val positionTicks = positionMs.toTicks()
            snapshot.playbackInfo?.let { localInfo ->
                playbackRepository.reportStopped(session, localInfo, positionTicks)
            }
            when (
                val result = playbackRepository.getCastPlaybackInfo(
                    session = session,
                    itemId = target.id,
                    title = target.title,
                    subtitle = target.subtitle,
                    startPositionTicks = positionTicks,
                    forceTranscode = false,
                    audioStreamIndex = snapshot.playbackInfo?.audioStreamIndex,
                    subtitleStreamIndex = snapshot.playbackInfo?.subtitleStreamIndex,
                    isLiveTv = target.isLiveTv,
                )
            ) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(
                        isPlaybackLoading = false,
                        playbackInfo = result.value,
                        playbackItem = result.value.toPlaybackItem(
                            target = target.copy(startTicks = positionTicks),
                            previousCandidate = snapshot.playbackItem?.previousCandidate,
                            upNextCandidate = snapshot.playbackItem?.upNextCandidate,
                            autoplaySettings = snapshot.playbackItem?.autoplaySettings ?: it.autoplaySettings(),
                            continuousPlaybackStartedAtMs = snapshot.playbackItem?.continuousPlaybackStartedAtMs ?: System.currentTimeMillis(),
                            isCastResolved = true,
                        ),
                        hasReportedPlaybackStart = false,
                        canTryPlaybackTranscode = result.value.fallbackStreamUrl != null,
                    )
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isPlaybackLoading = false,
                        playbackError = result.message,
                        mobileMessage = "Couldn't start casting.",
                    )
                }
            }
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
        updateActivePlaybackTrackSelection(audioStreamIndex = index, subtitleStreamIndex = _state.value.playbackInfo?.subtitleStreamIndex)
    }

    fun selectPlaybackSubtitleTrack(index: Int?) {
        updateActivePlaybackTrackSelection(audioStreamIndex = _state.value.playbackInfo?.audioStreamIndex, subtitleStreamIndex = index)
    }

    fun selectPlaybackAudioTrack(index: Int, positionMs: Long) {
        updateActivePlaybackTrackSelection(audioStreamIndex = index, subtitleStreamIndex = _state.value.playbackInfo?.subtitleStreamIndex)
        reportPlaybackProgress(positionMs, false)
    }

    fun selectPlaybackSubtitleTrack(index: Int?, positionMs: Long) {
        updateActivePlaybackTrackSelection(audioStreamIndex = _state.value.playbackInfo?.audioStreamIndex, subtitleStreamIndex = index)
        reportPlaybackProgress(positionMs, false)
    }

    private fun updateActivePlaybackTrackSelection(audioStreamIndex: Int?, subtitleStreamIndex: Int?) {
        _state.update { state ->
            val playbackInfo = state.playbackInfo
            val playbackItem = state.playbackItem
            state.copy(
                playbackInfo = playbackInfo?.copy(
                    audioStreamIndex = audioStreamIndex,
                    subtitleStreamIndex = subtitleStreamIndex,
                ),
                playbackItem = playbackItem?.copy(
                    selectedAudioStreamIndex = audioStreamIndex,
                    selectedSubtitleStreamIndex = subtitleStreamIndex,
                ),
            )
        }
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
            previousStopPositionTicks = positionMs?.toTicks(),
            continuousPlaybackStartedAtMs = _state.value.playbackItem?.continuousPlaybackStartedAtMs,
        )
    }

    private suspend fun loadUpNextCandidate(session: JellyfinSession, target: PlaybackTarget): UpNextCandidate? {
        if (target.isLiveTv || !target.itemType.equals("Episode", ignoreCase = true)) return null
        return when (val result = mediaRepository.getNextEpisode(session, target.id)) {
            is JellyfinResult.Success -> result.value?.toUpNextCandidate()
            is JellyfinResult.Failure -> null
        }
    }

    private fun loadPreviousEpisodeCandidate(target: PlaybackTarget): UpNextCandidate? {
        if (target.isLiveTv || !target.itemType.equals("Episode", ignoreCase = true)) return null
        val state = _state.value
        val episodes = (state.selectedSeasonEpisodes + (state.mediaDetail?.episodes ?: emptyList()))
            .distinctBy { it.id }
            .sortedWith(compareBy<JellyfinEpisode> { it.seasonIndexNumber ?: Int.MAX_VALUE }.thenBy { it.indexNumber ?: Int.MAX_VALUE })
        val currentIndex = episodes.indexOfFirst { it.id == target.id }
        return episodes.getOrNull(currentIndex - 1)?.toUpNextCandidate()
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

    private suspend fun refreshPublicUsers(server: JellyfinServerConfig?): List<JellyfinPublicUser> {
        if (server == null) return _state.value.publicUsers
        return when (val result = authRepository.publicUsers(server)) {
            is JellyfinResult.Success -> result.value
            is JellyfinResult.Failure -> _state.value.publicUsers
        }
    }

    private fun refreshSavedProfileImages() {
        viewModelScope.launch {
            val session = _state.value.session
            _state.update {
                it.copy(
                    savedProfiles = authRepository.savedProfiles(),
                    publicUsers = refreshPublicUsers(session?.server),
                )
            }
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

    fun selectVideoPlayerPreference(preference: VantafynVideoPlayerPreference) {
        _state.update { state ->
            val editor = appPreferences.edit().putString(KEY_VIDEO_PLAYER_PREFERENCE, preference.name)
            state.session?.profileId?.let { profileId ->
                editor.putString("${KEY_VIDEO_PLAYER_PREFERENCE}_$profileId", preference.name)
            }
            editor.apply()
            state.copy(videoPlayerPreference = preference, mobileMessage = null)
        }
    }

    fun externalVideoPlayerLaunchFailed() {
        _state.update {
            it.copy(
                mobileDestination = it.previousMobileDestination,
                playbackItem = null,
                playbackInfo = null,
                playbackError = null,
                isPlaybackLoading = false,
                mobileMessage = "No external video player is available on this device.",
            )
        }
    }

    fun externalVideoPlayerLaunched() {
        _state.update {
            it.copy(
                mobileDestination = it.previousMobileDestination,
                playbackItem = null,
                playbackInfo = null,
                playbackError = null,
                isPlaybackLoading = false,
                mobileMessage = "Opened in external player",
            )
        }
    }

    private fun readSelectedBackground(profileId: String?): VantafynAppBackground {
        val key = profileId?.let { homeLayoutStorage.getString("background_$it", null) }
            ?: homeLayoutStorage.getString("background_app", null)
        return key?.let { runCatching { VantafynAppBackground.valueOf(it) }.getOrNull() }
            ?: VantafynAppBackground.Nebula
    }

    private fun readVideoPlayerPreference(profileId: String?): VantafynVideoPlayerPreference {
        val key = profileId?.let { appPreferences.getString("${KEY_VIDEO_PLAYER_PREFERENCE}_$it", null) }
            ?: appPreferences.getString(KEY_VIDEO_PLAYER_PREFERENCE, null)
        return key?.let { runCatching { VantafynVideoPlayerPreference.valueOf(it) }.getOrNull() }
            ?: VantafynVideoPlayerPreference.Vantafyn
    }

    private fun readAutoplayCountdownSeconds(profileId: String?): Int {
        val profileKey = profileId?.let { "${KEY_AUTOPLAY_COUNTDOWN_SECONDS}_$it" }
        return profileKey?.let { appPreferences.getInt(it, -1) }
            ?.takeIf { it in AUTOPLAY_COUNTDOWN_OPTIONS }
            ?: appPreferences.getInt(KEY_AUTOPLAY_COUNTDOWN_SECONDS, 10)
                .takeIf { it in AUTOPLAY_COUNTDOWN_OPTIONS }
            ?: 10
    }

    private fun readPassoutProtectionEnabled(profileId: String?): Boolean {
        val profileKey = profileId?.let { "${KEY_PASSOUT_PROTECTION_ENABLED}_$it" }
        return profileKey?.takeIf { appPreferences.contains(it) }?.let { appPreferences.getBoolean(it, false) }
            ?: appPreferences.getBoolean(KEY_PASSOUT_PROTECTION_ENABLED, false)
    }

    private fun readPassoutProtectionLimitMinutes(profileId: String?): Int {
        val profileKey = profileId?.let { "${KEY_PASSOUT_PROTECTION_LIMIT_MINUTES}_$it" }
        return profileKey?.let { appPreferences.getInt(it, -1) }
            ?.takeIf { it in PASSOUT_PROTECTION_LIMIT_OPTIONS }
            ?: appPreferences.getInt(KEY_PASSOUT_PROTECTION_LIMIT_MINUTES, 180)
                .takeIf { it in PASSOUT_PROTECTION_LIMIT_OPTIONS }
            ?: 180
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

    private fun applyLibraryOrder(profileId: String?, libraries: List<JellyfinLibrary>): List<JellyfinLibrary> {
        val savedOrder = readLibraryOrder(profileId)
        if (savedOrder.isEmpty()) return libraries
        val originalIndex = libraries.mapIndexed { index, library -> library.id to index }.toMap()
        val rank = savedOrder.mapIndexed { index, id -> id to index }.toMap()
        return libraries.sortedWith(
            compareBy<JellyfinLibrary>(
                { rank[it.id] ?: Int.MAX_VALUE },
                { originalIndex[it.id] ?: Int.MAX_VALUE },
            ),
        )
    }

    private fun readLibraryOrder(profileId: String?): List<UUID> {
        val key = profileId?.let { "library_order_$it" } ?: return emptyList()
        return homeLayoutStorage.getString(key, null)
            ?.split('|')
            ?.mapNotNull { token -> runCatching { UUID.fromString(token) }.getOrNull() }
            .orEmpty()
    }

    private fun persistLibraryOrder(profileId: String?, ids: List<UUID>) {
        val key = profileId?.let { "library_order_$it" } ?: return
        homeLayoutStorage.edit().putString(key, ids.joinToString("|")).apply()
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
            _state.update {
                it.copy(
                    step = if (hasCompletedSetup) VantafynSetupStep.Splash else it.step,
                    isStartupResolved = false,
                    isLoading = false,
                )
            }
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
                            videoPlayerPreference = readVideoPlayerPreference(null),
                        )
                    }
                    selectProfile(lastProfile, showPickerWhileRestoring = false)
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
                    videoPlayerPreference = readVideoPlayerPreference(null),
                    isStartupResolved = true,
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

    private fun markSetupCompleted() {
        appPreferences.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
    }

    private suspend fun refreshSavedProfiles() {
        _state.update { it.copy(savedProfiles = authRepository.savedProfiles()) }
    }

    private fun loadLibraries(session: JellyfinSession) {
        viewModelScope.launch {
            _state.update { it.copy(isLibrariesLoading = true, errorMessage = null) }
            when (val result = libraryRepository.getLibraries(session)) {
                is JellyfinResult.Success -> {
                    val libraries = applyLibraryOrder(session.profileId, result.value)
                    _state.update {
                        it.copy(
                            isLibrariesLoading = false,
                            libraries = libraries,
                        )
                    }
                    loadHome(session, libraries)
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
                            markSetupCompleted()
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
                                    videoPlayerPreference = readVideoPlayerPreference(jellyfinSession.profileId),
                                    configuredSmartRows = readSmartRows(jellyfinSession.profileId),
                                    autoplayCountdownSeconds = readAutoplayCountdownSeconds(jellyfinSession.profileId),
                                    passoutProtectionEnabled = readPassoutProtectionEnabled(jellyfinSession.profileId),
                                    passoutProtectionLimitMinutes = readPassoutProtectionLimitMinutes(jellyfinSession.profileId),
                                )
                            }
                            refreshSavedProfiles()
                            startWatchPartyRealtime(jellyfinSession)
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
    val isStartupResolved: Boolean = false,
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
    val libraryItemsFilter: JellyfinLibraryItemFilter = JellyfinLibraryItemFilter.All,
    val libraryItemsPage: JellyfinLibraryPage? = null,
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
    val isAdminActionRunning: Boolean = false,
    val isLibraryScanTracking: Boolean = false,
    val libraryScanTrackingStartedAt: Long = 0L,
    val hasObservedLibraryScanRunning: Boolean = false,
    val adminError: String? = null,
    val playbackPreferences: JellyfinUserPlaybackPreferences? = null,
    val editablePlaybackPreferences: JellyfinUserPlaybackPreferences? = null,
    val autoplayCountdownSeconds: Int = 10,
    val passoutProtectionEnabled: Boolean = false,
    val passoutProtectionLimitMinutes: Int = 180,
    val isPlaybackPreferencesLoading: Boolean = false,
    val isPlaybackPreferencesSaving: Boolean = false,
    val playbackPreferencesError: String? = null,
    val selectedAdminUserId: UUID? = null,
    val adminUserDetail: JellyfinAdminUserDetail? = null,
    val isAdminUserLoading: Boolean = false,
    val isAdminUserSaving: Boolean = false,
    val adminUserError: String? = null,
    val isProfileImageSaving: Boolean = false,
    val profileImageError: String? = null,
    val mobileMessage: String? = null,
    val confirmLogout: Boolean = false,
    val isLogoutTransitioning: Boolean = false,
    val homeLayout: List<HomeSectionPreference> = defaultHomeLayout(),
    val themeMusicEnabled: Boolean = true,
    val themeMusicVolume: ThemeMusicVolume = ThemeMusicVolume.Soft,
    val selectedBackground: VantafynAppBackground = VantafynAppBackground.Nebula,
    val videoPlayerPreference: VantafynVideoPlayerPreference = VantafynVideoPlayerPreference.Vantafyn,
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
    val activeWatchParty: WatchPartySession? = null,
    val watchPartyName: String = "",
    val watchPartyMode: WatchPartyMode = WatchPartyMode.SwipeToMatch,
    val watchPartySelectedMedia: WatchPartySelectedMedia? = null,
    val watchPartyRules: WatchPartyRules = WatchPartyRules(),
    val watchPartyInviteRecipients: List<WatchPartyInviteRecipient> = emptyList(),
    val selectedWatchPartyRecipientSessionIds: Set<String> = emptySet(),
    val sentWatchPartyInvites: List<WatchPartyInvite> = emptyList(),
    val showWatchPartyInviteSentAnimation: Boolean = false,
    val isWatchPartyRecipientsLoading: Boolean = false,
    val watchPartyRealtimeConnectionState: SyncPlayConnectionState = SyncPlayConnectionState.Disconnected,
    val watchPartyRealtimeMembers: List<WatchPartyMemberRealtimeState> = emptyList(),
    val localWatchPartyReadyStates: Map<UUID, WatchPartyMemberReadyStatus> = emptyMap(),
    val watchPartyPlaybackState: WatchPartyPlaybackState = WatchPartyPlaybackState(null, 0L, false),
    val watchPartySyncStateLabel: String = "Sync unknown",
    val watchPartyLastGroupUpdate: String? = null,
    val watchPartyLastSyncCommand: String? = null,
    val watchPartyRealtimeError: String? = null,
    val watchPartyCandidates: List<WatchPartyCandidate> = emptyList(),
    val watchPartyCurrentIndex: Int = 0,
    val watchPartyVotes: List<WatchPartyVote> = emptyList(),
    val watchPartyMatch: WatchPartyMatch? = null,
    val isWatchPartyLoading: Boolean = false,
    val watchPartyError: String? = null,
    val watchPartyEnabled: Boolean = true,
    val watchPartyInvitesEnabled: Boolean = true,
    val watchPartyInviteAnimationEnabled: Boolean = true,
    val watchPartyInviteExpirySeconds: Int = 60,
    val incomingWatchPartyInvites: List<WatchPartyInvite> = emptyList(),
    val incomingWatchPartyMessage: String? = null,
    val errorMessage: String? = null,
) {
    val currentWatchPartyCandidate: WatchPartyCandidate?
        get() = watchPartyCandidates.getOrNull(watchPartyCurrentIndex)

    val activeIncomingWatchPartyInvite: WatchPartyInvite?
        get() = incomingWatchPartyInvites.firstOrNull { it.status == WatchPartyInviteStatus.Pending }
}

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

private fun String?.supportsMyListAction(): Boolean =
    equals("Movie", ignoreCase = true) ||
        equals("Series", ignoreCase = true) ||
        equals("Episode", ignoreCase = true) ||
        equals("BoxSet", ignoreCase = true) ||
        equals("Audio", ignoreCase = true) ||
        equals("MusicAlbum", ignoreCase = true) ||
        equals("Book", ignoreCase = true) ||
        equals("LiveTvChannel", ignoreCase = true) ||
        equals("LiveTvProgram", ignoreCase = true)

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
    WatchParty,
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
        MobileDestination.WatchParty,
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
    val itemType: String? = null,
    val seriesId: UUID? = null,
    val seasonId: UUID? = null,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)

private fun JellyfinMediaDetail.playbackTarget(positionMs: Long? = null): PlaybackTarget? =
    if (itemType.equals("Series", ignoreCase = true)) {
        episodes.firstOrNull { (it.progress ?: 0f) < 0.95f }?.let {
            PlaybackTarget(
                id = it.id,
                title = it.title,
                subtitle = listOfNotNull(title, it.subtitle).joinToString(" · ").ifBlank { null },
                startTicks = 0L,
                itemType = "Episode",
                seriesId = id,
                seasonId = it.seasonId,
                seriesName = title,
                seasonNumber = it.seasonIndexNumber,
                episodeNumber = it.indexNumber,
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
            itemType = itemType,
            seriesId = seriesId,
            seasonId = seasonId,
            seriesName = seriesName,
            seasonNumber = seasonIndexNumber,
            episodeNumber = episodeIndexNumber,
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
            itemType = "Episode",
            seriesId = episode.seriesId ?: id,
            seasonId = episode.seasonId,
            seriesName = episode.seriesName ?: title,
            seasonNumber = episode.seasonIndexNumber,
            episodeNumber = episode.indexNumber,
        )
    } else {
        PlaybackTarget(
            id = id,
            title = title,
            subtitle = subtitle,
            startTicks = 0L,
            itemType = itemType,
            seriesId = seriesId,
            seasonId = seasonId,
            seriesName = seriesName,
            seasonNumber = seasonIndexNumber,
            episodeNumber = episodeIndexNumber,
        )
    }

private fun JellyfinMediaDetail.defaultSeasonId(): UUID? {
    if (!itemType.equals("Series", ignoreCase = true)) return null
    val nextEpisode = episodes.firstOrNull { (it.progress ?: 0f) in 0.01f..0.94f }
        ?: episodes.firstOrNull { !it.isPlayed }
        ?: episodes.firstOrNull()
    return seasons.firstOrNull { it.indexNumber == nextEpisode?.seasonIndexNumber }?.id ?: seasons.firstOrNull()?.id
}

private fun JellyfinPlaybackInfo.toPlaybackItem(
    target: PlaybackTarget,
    previousCandidate: UpNextCandidate? = null,
    upNextCandidate: UpNextCandidate?,
    autoplaySettings: AutoplaySettings,
    continuousPlaybackStartedAtMs: Long,
    isCastResolved: Boolean = false,
): VantafynPlaybackItem =
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
                deliveryUrl = it.deliveryUrl,
            )
        },
        itemType = target.itemType,
        isLiveStream = isLiveStream || target.isLiveTv,
        isCastResolved = isCastResolved,
        previousCandidate = previousCandidate,
        upNextCandidate = upNextCandidate,
        autoplaySettings = autoplaySettings,
        continuousPlaybackStartedAtMs = continuousPlaybackStartedAtMs,
    )

private fun JellyfinUpNextCandidate.toUpNextCandidate(): UpNextCandidate =
    UpNextCandidate(
        itemId = itemId.toString(),
        seriesId = seriesId?.toString(),
        seasonId = seasonId?.toString(),
        title = title,
        seriesName = seriesName,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        runtimeMs = runtimeTicks?.let { it / 10_000L },
        imageUrl = imageUrl,
        backdropUrl = backdropUrl,
        overview = overview,
        progress = progress,
        playbackPositionMs = playbackPositionTicks / 10_000L,
    )

private fun JellyfinEpisode.toUpNextCandidate(): UpNextCandidate =
    UpNextCandidate(
        itemId = id.toString(),
        seriesId = seriesId?.toString(),
        seasonId = seasonId?.toString(),
        title = title,
        seriesName = seriesName,
        seasonNumber = seasonIndexNumber,
        episodeNumber = indexNumber,
        runtimeMs = runtimeMinutes?.let { it * 60_000L },
        imageUrl = imageUrl,
        backdropUrl = null,
        overview = overview,
        progress = progress,
        playbackPositionMs = playbackPositionTicks / 10_000L,
    )

private fun VantafynHomeUiState.autoplaySettings(): AutoplaySettings =
    AutoplaySettings(
        enabled = playbackPreferences?.enableNextEpisodeAutoPlay ?: true,
        countdownSeconds = autoplayCountdownSeconds,
        passoutProtectionEnabled = passoutProtectionEnabled,
        passoutProtectionLimitMinutes = passoutProtectionLimitMinutes,
    )

private val AUTOPLAY_COUNTDOWN_OPTIONS = setOf(5, 10, 15, 30)
private val PASSOUT_PROTECTION_LIMIT_OPTIONS = setOf(60, 120, 180, 240, 300)
private const val KEY_AUTOPLAY_COUNTDOWN_SECONDS = "autoplay_countdown_seconds"
private const val LOGOUT_TRANSITION_DELAY_MS = 380L

private fun WatchPartyRules.isMatched(votes: List<WatchPartyVote>, candidateId: UUID, memberCount: Int): Boolean {
    val yesVotes = votes.count { it.candidateId == candidateId && it.vote == WatchPartyVoteValue.Yes }
    return when (matchRule) {
        WatchPartyMatchRule.Everyone -> yesVotes >= memberCount.coerceAtLeast(1)
        WatchPartyMatchRule.Majority -> yesVotes > memberCount.coerceAtLeast(1) / 2
    }
}

private fun JellyfinMediaDetail.toWatchPartySelectedMedia(): WatchPartySelectedMedia =
    WatchPartySelectedMedia(
        id = id,
        title = title,
        subtitle = subtitle,
        itemType = itemType,
        artworkUrl = imageUrl,
        backdropUrl = backdropUrl,
    )

private fun WatchPartyInvite.toSelectedMedia(): WatchPartySelectedMedia? =
    mediaItemId?.let { id ->
        WatchPartySelectedMedia(
            id = id,
            title = mediaTitle ?: "Selected title",
            subtitle = mediaType,
            itemType = mediaType,
            artworkUrl = mediaArtworkUrl,
            backdropUrl = null,
        )
    }

private const val KEY_PASSOUT_PROTECTION_ENABLED = "passout_protection_enabled"
private const val KEY_PASSOUT_PROTECTION_LIMIT_MINUTES = "passout_protection_limit_minutes"
private const val KEY_VIDEO_PLAYER_PREFERENCE = "video_player_preference"

private fun Long.toTicks(): Long =
    coerceAtLeast(0L) * 10_000L

enum class VantafynAppBackground(val label: String) {
    Nebula("Nebula"),
    Background1("Glass blue"),
    Background2("Twilight"),
    Background3("Aurora"),
    Background4("Deep space"),
}

enum class VantafynVideoPlayerPreference(
    val label: String,
    val summary: String,
) {
    Vantafyn(
        label = "Vantafyn player",
        summary = "Recommended. Uses the built-in Media3 player with Cast, track selection, progress, and Up Next.",
    ),
    External(
        label = "External app",
        summary = "Hands the Jellyfin stream to another video app. Progress reporting and track controls depend on that app.",
    ),
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

private data class LibraryScanTrackingResult(
    val isTracking: Boolean,
    val hasObservedRunning: Boolean,
)

private fun VantafynHomeUiState.libraryScanTrackingAfter(overview: JellyfinAdminOverview): LibraryScanTrackingResult {
    if (!isLibraryScanTracking) return LibraryScanTrackingResult(isTracking = false, hasObservedRunning = false)
    val scanTask = overview.tasks.firstOrNull { it.looksLikeLibraryScanTask() }
    val isActive = scanTask?.isActiveTask() == true
    val observedRunning = hasObservedLibraryScanRunning || isActive
    val inStartGrace = libraryScanTrackingStartedAt > 0L &&
        System.currentTimeMillis() - libraryScanTrackingStartedAt < LibraryScanStartGraceMs
    return LibraryScanTrackingResult(
        isTracking = scanTask == null || isActive || (!observedRunning && inStartGrace),
        hasObservedRunning = observedRunning,
    )
}

private fun JellyfinAdminTask.isActiveTask(): Boolean =
    progress?.let { it in 0.0..99.99 } == true ||
        state.equals("Running", ignoreCase = true) ||
        state.equals("Queued", ignoreCase = true) ||
        state.equals("Cancelling", ignoreCase = true)

private fun JellyfinAdminTask.looksLikeLibraryScanTask(): Boolean {
    val haystack = listOf(id, name, category).joinToString(" ").lowercase()
    return "library" in haystack && ("scan" in haystack || "refresh" in haystack)
}

private const val KEY_AUTO_LOGIN_LAST_PROFILE = "auto_login_last_profile"
private const val KEY_SETUP_COMPLETED = "setup_completed"
private const val KEY_WATCH_PARTY_ENABLED = "watch_party_enabled"
private const val KEY_WATCH_PARTY_INVITES_ENABLED = "watch_party_invites_enabled"
private const val KEY_WATCH_PARTY_INVITE_ANIMATION_ENABLED = "watch_party_invite_animation_enabled"
private const val KEY_WATCH_PARTY_INVITE_EXPIRY_SECONDS = "watch_party_invite_expiry_seconds"
private const val WATCH_PARTY_REALTIME_TASK_ID = "watchParty.realtime"
private const val LibraryScanStartGraceMs = 20_000L
private val WATCH_PARTY_INVITE_EXPIRY_OPTIONS = setOf(30, 60, 300)
private const val LibraryItemsPageSize = 60

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

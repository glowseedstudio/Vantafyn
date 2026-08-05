package dev.vantafyn.feature.home.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vantafyn.core.jellyfin.JellyfinAuthRepository
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.jellyfin.JellyfinLibraryRepository
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinServerConfig
import dev.vantafyn.core.jellyfin.JellyfinSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VantafynHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repositories = JellyfinRepositoryProvider(application)
    private val authRepository: JellyfinAuthRepository = repositories.authRepository
    private val libraryRepository: JellyfinLibraryRepository = repositories.libraryRepository

    private val _state = MutableStateFlow(VantafynHomeUiState())
    val state: StateFlow<VantafynHomeUiState> = _state.asStateFlow()

    init {
        restoreSession()
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

    fun connectToServer() {
        val url = _state.value.serverUrl
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.testServer(url)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            step = VantafynSetupStep.Login,
                            isLoading = false,
                            server = result.value,
                            serverUrl = result.value.url,
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
                        )
                    }
                    loadLibraries(result.value)
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = VantafynHomeUiState(step = VantafynSetupStep.Server)
        }
    }

    fun retryLibraries() {
        _state.value.session?.let(::loadLibraries)
    }

    private fun restoreSession() {
        viewModelScope.launch {
            _state.update { it.copy(step = VantafynSetupStep.Splash, isLoading = true) }
            when (val result = authRepository.restoreSession()) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            step = VantafynSetupStep.Home,
                            isLoading = false,
                            session = result.value,
                            server = result.value.server,
                            serverUrl = result.value.server.url,
                            username = result.value.user.name,
                        )
                    }
                    loadLibraries(result.value)
                }
                is JellyfinResult.Failure -> {
                    val savedServer = authRepository.savedServerHint()
                    _state.update {
                        it.copy(
                            step = if (savedServer == null) VantafynSetupStep.Server else VantafynSetupStep.Login,
                            isLoading = false,
                            server = savedServer,
                            serverUrl = savedServer?.url.orEmpty(),
                            errorMessage = null,
                        )
                    }
                }
            }
        }
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
}

data class VantafynHomeUiState(
    val step: VantafynSetupStep = VantafynSetupStep.Splash,
    val isLoading: Boolean = false,
    val isLibrariesLoading: Boolean = false,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val server: JellyfinServerConfig? = null,
    val session: JellyfinSession? = null,
    val libraries: List<JellyfinLibrary> = emptyList(),
    val errorMessage: String? = null,
)

enum class VantafynSetupStep {
    Splash,
    Server,
    Login,
    Home,
}

package dev.vantafyn.tv.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.vantafyn.core.ui.VantafynOnboardingBackground
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynHomeViewModel
import dev.vantafyn.feature.home.auth.VantafynSetupStep
import kotlinx.coroutines.delay

private val VantafynSetupCinematicEasing = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

enum class TvInternalSetupPhase {
    Welcome,
    ChooseMethod,
    Pairing,
    QuickConnect,
    ConnectServer,
    ServerConfirm,
    Login,
    ProfilePicker,
}

@Composable
fun TvSetupScreen(
    state: VantafynHomeUiState,
    viewModel: VantafynHomeViewModel,
    modifier: Modifier = Modifier,
) {
    var isChoosingMethod by remember { mutableStateOf(false) }
    var isPairing by remember { mutableStateOf(false) }
    var isQuickConnectFlow by rememberSaveable { mutableStateOf(false) }

    // Initial 2-second background-only reveal on first launch of setup
    var isInitialBackgroundPaused by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (isInitialBackgroundPaused) {
            delay(2_000L)
            isInitialBackgroundPaused = false
        }
    }

    // Map ViewModel VantafynSetupStep + local method choice into TV phase
    val currentPhase = when (state.step) {
        VantafynSetupStep.Splash,
        VantafynSetupStep.Welcome -> {
            when {
                isPairing -> TvInternalSetupPhase.Pairing
                isChoosingMethod -> TvInternalSetupPhase.ChooseMethod
                else -> TvInternalSetupPhase.Welcome
            }
        }
        VantafynSetupStep.QuickConnect -> TvInternalSetupPhase.QuickConnect
        VantafynSetupStep.ConnectServer -> TvInternalSetupPhase.ConnectServer
        VantafynSetupStep.ServerConfirm -> TvInternalSetupPhase.ServerConfirm
        VantafynSetupStep.Login -> TvInternalSetupPhase.Login
        VantafynSetupStep.ProfilePicker -> TvInternalSetupPhase.ProfilePicker
        else -> TvInternalSetupPhase.Welcome
    }

    // Hardware / D-pad Back Button handling
    BackHandler(enabled = true) {
        when (currentPhase) {
            TvInternalSetupPhase.Pairing -> {
                isPairing = false
                isChoosingMethod = true
            }
            TvInternalSetupPhase.QuickConnect -> {
                viewModel.cancelQuickConnect()
                if (state.server != null && !isChoosingMethod) {
                    viewModel.navigateSetupBack()
                } else {
                    isChoosingMethod = true
                }
            }
            TvInternalSetupPhase.ChooseMethod -> {
                isChoosingMethod = false
            }
            TvInternalSetupPhase.ConnectServer -> {
                viewModel.navigateSetupBack()
            }
            TvInternalSetupPhase.ServerConfirm,
            TvInternalSetupPhase.Login,
            TvInternalSetupPhase.ProfilePicker -> {
                viewModel.navigateSetupBack()
            }
            TvInternalSetupPhase.Welcome -> {
                // Exit or stay at Welcome
            }
        }
    }

    VantafynOnboardingBackground(
        tv = true,
        modifier = modifier.fillMaxSize(),
    ) {
        if (!isInitialBackgroundPaused) {
            AnimatedContent(
                targetState = currentPhase,
                transitionSpec = {
                    val isForward = targetState.ordinal >= initialState.ordinal
                    val enterDuration = 900
                    val exitDuration = 420
                    (
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = enterDuration,
                                delayMillis = 90,
                                easing = VantafynSetupCinematicEasing,
                            ),
                        ) + slideInVertically(
                            animationSpec = tween(
                                durationMillis = enterDuration,
                                delayMillis = 90,
                                easing = VantafynSetupCinematicEasing,
                            ),
                            initialOffsetY = { if (isForward) 22 else -22 },
                        )
                    ).togetherWith(
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = exitDuration,
                                easing = VantafynSetupCinematicEasing,
                            ),
                        ) + slideOutVertically(
                            animationSpec = tween(
                                durationMillis = exitDuration,
                                easing = VantafynSetupCinematicEasing,
                            ),
                            targetOffsetY = { if (isForward) -16 else 16 },
                        )
                    ).using(SizeTransform(clip = false))
                },
                label = "setupStepTransition",
            ) { phase ->
                when (phase) {
                    TvInternalSetupPhase.Welcome -> {
                        TvWelcomeScreen(
                            onGetStarted = {
                                isChoosingMethod = true
                            },
                        )
                    }

                    TvInternalSetupPhase.ChooseMethod -> {
                        TvSetupMethodScreen(
                            onPairWithMobile = {
                                isPairing = true
                            },
                            onQuickConnect = {
                                isPairing = false
                                isChoosingMethod = false
                                isQuickConnectFlow = true
                                if (state.server != null) {
                                    viewModel.startQuickConnect()
                                } else {
                                    viewModel.continueFromWelcome()
                                }
                            },
                            onManualSetup = {
                                isPairing = false
                                isChoosingMethod = false
                                isQuickConnectFlow = false
                                viewModel.continueFromWelcome()
                            },
                            onBack = {
                                isChoosingMethod = false
                            },
                        )
                    }

                    TvInternalSetupPhase.Pairing -> {
                        TvPairingScreen(
                            onPairingSuccess = { payload ->
                                isPairing = false
                                isChoosingMethod = false
                                viewModel.pairWithMobilePayload(payload)
                            },
                            onManualSetup = {
                                isPairing = false
                                isChoosingMethod = false
                                isQuickConnectFlow = false
                                viewModel.continueFromWelcome()
                            },
                            onBack = {
                                isPairing = false
                                isChoosingMethod = true
                            },
                        )
                    }

                    TvInternalSetupPhase.QuickConnect -> {
                        TvQuickConnectScreen(
                            session = state.quickConnectSession,
                            statusMessage = state.quickConnectMessage,
                            isLoading = state.isLoading,
                            errorMessage = state.errorMessage,
                            onRefresh = { viewModel.retryQuickConnect() },
                            onManualSetup = {
                                isQuickConnectFlow = false
                                viewModel.cancelQuickConnect()
                                viewModel.continueToLogin()
                            },
                            onBack = {
                                viewModel.cancelQuickConnect()
                                if (state.server != null && !isChoosingMethod) {
                                    viewModel.navigateSetupBack()
                                } else {
                                    isChoosingMethod = true
                                }
                            },
                        )
                    }

                    TvInternalSetupPhase.ConnectServer -> {
                        TvConnectServerScreen(
                            serverUrl = state.serverUrl,
                            isLoading = state.isLoading,
                            errorMessage = state.errorMessage,
                            onServerUrlChange = { viewModel.onServerUrlChanged(it) },
                            onConnect = { viewModel.connectToServer() },
                            onBack = { viewModel.navigateSetupBack() },
                        )
                    }

                    TvInternalSetupPhase.ServerConfirm -> {
                        TvServerConfirmScreen(
                            server = state.server,
                            hasPublicUsers = state.publicUsers.isNotEmpty(),
                            publicUsers = state.publicUsers,
                            savedProfiles = state.savedProfiles,
                            currentSessionUser = state.session?.user?.name,
                            isQuickConnectMode = isQuickConnectFlow,
                            onContinue = {
                                if (isQuickConnectFlow) {
                                    viewModel.startQuickConnect()
                                } else {
                                    viewModel.continueToLogin()
                                }
                            },
                            onQuickConnect = {
                                viewModel.startQuickConnect()
                            },
                            onUseDifferentServer = {
                                viewModel.onServerUrlChanged("")
                                viewModel.continueFromWelcome()
                            },
                        )
                    }

                    TvInternalSetupPhase.Login -> {
                        TvLoginScreen(
                            username = state.username,
                            password = state.password,
                            serverName = state.server?.name,
                            isLoading = state.isLoading,
                            errorMessage = state.errorMessage,
                            onUsernameChange = { viewModel.onUsernameChanged(it) },
                            onPasswordChange = { viewModel.onPasswordChanged(it) },
                            onLogin = { viewModel.login() },
                            onBack = { viewModel.navigateSetupBack() },
                            onQuickConnect = if (state.server != null) {
                                { viewModel.startQuickConnect() }
                            } else null,
                        )
                    }

                    TvInternalSetupPhase.ProfilePicker -> {
                        TvProfilePickerScreen(
                            savedProfiles = state.savedProfiles,
                            publicUsers = state.publicUsers,
                            server = state.server,
                            onSelectSavedProfile = { profile ->
                                viewModel.selectProfile(profile)
                            },
                            onSelectPublicUser = { user ->
                                viewModel.selectPublicUser(user)
                            },
                            onAddProfile = {
                                viewModel.addProfile()
                            },
                            onChangeServer = {
                                viewModel.onServerUrlChanged("")
                                viewModel.continueFromWelcome()
                            },
                            onBack = if (state.savedProfiles.isNotEmpty()) null else {
                                { viewModel.navigateSetupBack() }
                            },
                        )
                    }
                }
            }
        }
    }
}

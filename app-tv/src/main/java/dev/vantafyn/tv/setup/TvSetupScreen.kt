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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.vantafyn.core.ui.VantafynOnboardingBackground
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynHomeViewModel
import dev.vantafyn.feature.home.auth.VantafynSetupStep

private val VantafynSetupCinematicEasing = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

private enum class TvInternalSetupPhase {
    Welcome,
    ChooseMethod,
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

    // Map ViewModel VantafynSetupStep + local method choice into TV phase
    val currentPhase = when (state.step) {
        VantafynSetupStep.Splash,
        VantafynSetupStep.Welcome -> {
            if (isChoosingMethod) TvInternalSetupPhase.ChooseMethod else TvInternalSetupPhase.Welcome
        }
        VantafynSetupStep.ConnectServer -> TvInternalSetupPhase.ConnectServer
        VantafynSetupStep.ServerConfirm -> TvInternalSetupPhase.ServerConfirm
        VantafynSetupStep.Login -> TvInternalSetupPhase.Login
        VantafynSetupStep.ProfilePicker -> TvInternalSetupPhase.ProfilePicker
        else -> TvInternalSetupPhase.Welcome
    }

    // Hardware / D-pad Back Button handling
    BackHandler(enabled = true) {
        when (currentPhase) {
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
        AnimatedContent(
            targetState = currentPhase,
            transitionSpec = {
                val isForward = targetState.ordinal >= initialState.ordinal
                val enterDuration = 980
                val exitDuration = 520
                (
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = enterDuration,
                            delayMillis = 140,
                            easing = VantafynSetupCinematicEasing,
                        ),
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = enterDuration,
                            delayMillis = 140,
                            easing = VantafynSetupCinematicEasing,
                        ),
                        initialOffsetY = { if (isForward) 32 else -32 },
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
                        targetOffsetY = { if (isForward) -24 else 24 },
                    )
                ).using(SizeTransform(clip = false))
            },
            label = "setupStepTransition",
        ) { phase ->
            when (phase) {
                TvInternalSetupPhase.Welcome -> {
                    TvWelcomeScreen(
                        onGetStarted = {
                            viewModel.continueFromWelcome()
                        },
                    )
                }

                TvInternalSetupPhase.ChooseMethod -> {
                    TvSetupMethodScreen(
                        onManualSetup = {
                            isChoosingMethod = false
                            viewModel.continueFromWelcome()
                        },
                        onBack = {
                            isChoosingMethod = false
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
                        onContinue = { viewModel.continueToLogin() },
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

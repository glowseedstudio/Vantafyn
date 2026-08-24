package dev.vantafyn.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import dev.vantafyn.core.media.AppForegroundStateRepository
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.feature.home.auth.VantafynHomeViewModel
import dev.vantafyn.feature.home.auth.VantafynSetupStep
import dev.vantafyn.tv.setup.TvSetupScreen
import dev.vantafyn.tv.shell.TvShellScreen

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

private val TvCinematicEasing = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            VantafynTheme {
                TvAppRoot()
            }
        }
    }
}

@Composable
private fun TvAppRoot(
    viewModel: VantafynHomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    AppForegroundStateRepository.setForeground(true)
                    viewModel.onAppForegrounded()
                    dev.vantafyn.tv.remoteinput.TvRemoteInputManager.start()
                }
                Lifecycle.Event.ON_STOP -> {
                    AppForegroundStateRepository.setForeground(false)
                    viewModel.onAppBackgrounded()
                    dev.vantafyn.tv.remoteinput.TvRemoteInputManager.stop()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AnimatedContent(
        targetState = state.step == VantafynSetupStep.Home && state.isStartupResolved,
        transitionSpec = {
            if (targetState) {
                (
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 1_400,
                            delayMillis = 180,
                            easing = TvCinematicEasing,
                        ),
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 1_400,
                            delayMillis = 180,
                            easing = TvCinematicEasing,
                        ),
                        initialOffsetY = { 36 },
                    )
                ).togetherWith(
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = TvCinematicEasing,
                        ),
                    ) + slideOutVertically(
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = TvCinematicEasing,
                        ),
                        targetOffsetY = { -28 },
                    )
                ).using(SizeTransform(clip = false))
            } else {
                (
                    fadeIn(
                        animationSpec = tween(durationMillis = 800, easing = TvCinematicEasing),
                    ) + slideInVertically(
                        animationSpec = tween(durationMillis = 800, easing = TvCinematicEasing),
                        initialOffsetY = { -28 },
                    )
                ).togetherWith(
                    fadeOut(
                        animationSpec = tween(durationMillis = 500, easing = TvCinematicEasing),
                    ) + slideOutVertically(
                        animationSpec = tween(durationMillis = 500, easing = TvCinematicEasing),
                        targetOffsetY = { 28 },
                    )
                ).using(SizeTransform(clip = false))
            }
        },
        label = "TvRootTransition",
    ) { isHomeReady ->
        if (isHomeReady) {
            TvShellScreen(
                state = state,
                session = state.session,
                onOpenMediaDetails = { id ->
                    viewModel.openMedia(id)
                },
                onPlayMediaId = { id ->
                    viewModel.openMedia(id)
                    viewModel.startPlayback()
                },
                onStartPlayback = {
                    viewModel.startPlayback()
                },
                onStartPlaybackFromBeginning = {
                    viewModel.startPlaybackFromBeginning()
                },
                onSearchQueryChange = { query ->
                    viewModel.onSearchQueryChanged(query)
                },
                onSelectProfile = { profile ->
                    viewModel.selectProfile(profile)
                },
                onRefreshHome = {
                    viewModel.onAppForegrounded()
                },
                onLogout = {
                    viewModel.logoutCurrentProfile()
                },
            )
        } else {
            TvSetupScreen(
                state = state,
                viewModel = viewModel,
            )
        }
    }
}

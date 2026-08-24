package dev.vantafyn.tv.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

val VantafynTvSetupCinematicEasing = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

/**
 * Mobile-parity one-shot top-to-bottom content reveal container for TV setup screens.
 * Uses the same 0.19/1.0/0.22/1.0 cubic bezier cinematic easing and staggered entrance timing
 * as the Vantafyn mobile setup flow.
 */
@Composable
fun VantafynTvSetupReveal(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 1_000,
                delayMillis = delayMillis,
                easing = VantafynTvSetupCinematicEasing,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 1_000,
                delayMillis = delayMillis,
                easing = VantafynTvSetupCinematicEasing,
            ),
            initialOffsetY = { 16 },
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 360,
                easing = VantafynTvSetupCinematicEasing,
            ),
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 360,
                easing = VantafynTvSetupCinematicEasing,
            ),
            targetOffsetY = { -12 },
        ),
    ) {
        content()
    }
}

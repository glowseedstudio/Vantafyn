package dev.vantafyn.feature.home.games

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vantafyn.feature.home.rememberReducedMotionPreference

/**
 * Standard Vantafyn one-shot entrance reveal for Games and Pokémon Vault screens.
 * Exactly matches the media sections (Achievements, Social, Libraries, Discover).
 * Executes once on entry (Animatable 0f -> 1f over 440ms), with no ongoing loops.
 */
@Composable
fun GameScreenReveal(
    key: Any? = Unit,
    offsetY: Dp = 24.dp,
    durationMillis: Int = 440,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotionPreference()
    var revealProgress by remember(key) { mutableFloatStateOf(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(key) {
        if (!reducedMotion) {
            revealProgress = 0f
            val anim = Animatable(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            ) {
                revealProgress = value
            }
        } else {
            revealProgress = 1f
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = revealProgress
            translationY = (1f - revealProgress) * offsetY.toPx()
        }
    ) {
        content()
    }
}

/**
 * Standard Vantafyn transition spec for tab / screen transitions in AnimatedContent.
 * Gives tabs and sub-screens the same smooth slide-up + fade entrance as the media sections.
 */
fun gamesTabTransitionSpec(reducedMotion: Boolean): ContentTransform =
    if (reducedMotion) {
        fadeIn(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        ) togetherWith fadeOut(
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
        )
    } else {
        (fadeIn(
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            initialOffsetY = { (it * 0.05f).toInt().coerceAtLeast(36) }
        )) togetherWith fadeOut(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )
    }

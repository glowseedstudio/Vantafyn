package dev.vantafyn.tv.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val PulseGradientColors = listOf(
    Color(0xFF21D8FF), // Cyan
    Color(0xFF3E63FF), // Royal Blue
    Color(0xFF8B35FF), // Violet
)

/**
 * Adds a soft, luminous breathing cyan/blue/violet pulse glow behind the primary Welcome button.
 * The pulse is restrained, decorative, does not alter layout bounds, and permanently
 * stops when [enabled] is set to false.
 */
@Composable
fun VantafynTvPulsingButtonGlow(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "TvWelcomeButtonPulse")

    // Contracts completely behind the button (0.92x) -> Expands gently out (1.10x)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    // Fades to completely transparent when contracted (0.0f) -> softly luminous when expanded (0.60f)
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Softly feathered, luminous breathing aura that hides behind the button and peeks out
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = pulseScale * 1.08f
                    scaleY = pulseScale * 1.30f
                    alpha = pulseAlpha
                },
        ) {
            val cornerRadiusPx = 28.dp.toPx()
            drawRoundRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to PulseGradientColors[0].copy(alpha = 0.75f),
                        0.40f to PulseGradientColors[1].copy(alpha = 0.50f),
                        0.70f to PulseGradientColors[2].copy(alpha = 0.20f),
                        0.90f to PulseGradientColors[2].copy(alpha = 0.04f),
                        1.0f to Color.Transparent,
                    ),
                    radius = (size.width * 0.58f).coerceAtLeast(1f),
                ),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            )
        }

        content()
    }
}

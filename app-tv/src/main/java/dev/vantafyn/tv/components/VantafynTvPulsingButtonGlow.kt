package dev.vantafyn.tv.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PulseGradientColors = listOf(
    Color(0xFF21D8FF), // Cyan
    Color(0xFF3E63FF), // Royal Blue
    Color(0xFF8B35FF), // Violet
    Color(0xFFFF36C7), // Magenta
)

/**
 * Adds a soft, luminous breathing gradient pulse glow behind the primary Welcome button.
 * Starts fully collapsed behind the button (0dp expansion) and gently breathes out to 12dp,
 * with matching rounded pill curvature and no layer clipping.
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

    // Starts collapsed at 0dp behind the button, then gently breathes out to 12dp
    val pulseExpansion by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseExpansion",
    )

    // Fades from 0.0f (when collapsed) up to 0.55f (at peak breath)
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier
            .drawBehind {
                if (pulseAlpha <= 0.01f || pulseExpansion <= 0.1f) return@drawBehind

                val expansionPx = pulseExpansion.dp.toPx()
                val glowWidth = size.width + (expansionPx * 2f)
                val glowHeight = size.height + (expansionPx * 2f)
                val pillCornerRadius = (18.dp.toPx() + expansionPx)

                drawRoundRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to PulseGradientColors[0].copy(alpha = 0.65f * pulseAlpha),
                            0.35f to PulseGradientColors[1].copy(alpha = 0.45f * pulseAlpha),
                            0.65f to PulseGradientColors[2].copy(alpha = 0.25f * pulseAlpha),
                            0.85f to PulseGradientColors[3].copy(alpha = 0.08f * pulseAlpha),
                            1.0f to Color.Transparent,
                        ),
                        center = center,
                        radius = (glowWidth * 0.55f).coerceAtLeast(1f),
                    ),
                    topLeft = Offset(-expansionPx, -expansionPx),
                    size = Size(glowWidth, glowHeight),
                    cornerRadius = CornerRadius(pillCornerRadius, pillCornerRadius),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

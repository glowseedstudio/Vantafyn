package dev.vantafyn.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun VantafynWavyProgressBar(
    progress: Float,
    isPlaying: Boolean,
    isScrubbing: Boolean = false,
    modifier: Modifier = Modifier,
    waveHeight: Dp = 8.dp,
    wavelength: Dp = 28.dp,
    strokeWidth: Dp = 4.5.dp,
    brush: Brush = VantafynGradients.accentHorizontal(),
    inactiveColor: Color = Color.White.copy(alpha = 0.14f),
    showThumb: Boolean = false,
    thumbColor: Color = Color(0xFF31D7FF),
    thumbRadius: Dp = 6.dp,
) {
    val density = LocalDensity.current
    val waveHeightPx = with(density) { waveHeight.toPx() }
    val wavelengthPx = with(density) { wavelength.toPx() }.coerceAtLeast(10f)
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }

    val targetAmplitude = if (isPlaying && !isScrubbing) waveHeightPx / 2f else 0f
    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "wavyAmplitude",
    )

    val shouldAnimateWave = isPlaying && !isScrubbing
    val phase = if (shouldAnimateWave) {
        val infiniteTransition = rememberInfiniteTransition(label = "wavyProgressMotion")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wavyPhase",
        ).value
    } else {
        0f
    }

    val safeProgress = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val progressX = (safeProgress * width).coerceIn(0f, width)

        // 1. Inactive track (from progressX to width)
        if (progressX < width) {
            drawLine(
                color = inactiveColor,
                start = Offset(progressX, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }

        // 2. Active wavy track (from 0 to progressX)
        if (progressX > 0f) {
            if (animatedAmplitude <= 0.05f) {
                drawLine(
                    brush = brush,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )
            } else {
                val wavePath = Path()
                val stepPx = 2.5f
                val taperDistance = (wavelengthPx * 0.5f).coerceAtLeast(12f)
                val phaseOffset = phase * 2f * Math.PI.toFloat()

                var currentX = 0f
                wavePath.moveTo(0f, centerY)

                while (currentX < progressX) {
                    currentX = (currentX + stepPx).coerceAtMost(progressX)
                    val taperStart = (currentX / taperDistance).coerceIn(0f, 1f)
                    val taperEnd = ((progressX - currentX) / taperDistance).coerceIn(0f, 1f)
                    val taper = minOf(taperStart, taperEnd)

                    val angle = (currentX / wavelengthPx) * 2f * Math.PI.toFloat() - phaseOffset
                    val y = centerY + animatedAmplitude * taper * sin(angle)
                    wavePath.lineTo(currentX, y)
                }

                drawPath(
                    path = wavePath,
                    brush = brush,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        // 3. Optional thumb dot
        if (showThumb && progressX in 0f..width) {
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(progressX, centerY),
            )
        }
    }
}

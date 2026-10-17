package dev.vantafyn.feature.home.games.grid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Premium 3D tactile elevation and tilt modifier inspired by Wii Channel physics and iiSU.
 *
 * Implements:
 * 1. Perspective depth via [Modifier.graphicsLayer] with camera distance.
 * 2. Dynamic rotational tilt ([rotationX], [rotationY]) tracking touch/joystick offset.
 * 3. Spring elevation scaling on focus/hover.
 * 4. Specular acrylic glass sheen across the surface that reacts dynamically to tilt.
 */
@Composable
fun Modifier.tactile3DTilt(
    isFocused: Boolean = false,
    maxTiltDegrees: Float = 7f,
    focusScale: Float = 1.04f,
    showSheen: Boolean = true,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this

    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    var touchOffsetX by remember { mutableFloatStateOf(0f) }
    var touchOffsetY by remember { mutableFloatStateOf(0f) }

    // Spring animated tilt values
    val targetRotX = if (isFocused) {
        (-touchOffsetY * maxTiltDegrees).coerceIn(-maxTiltDegrees, maxTiltDegrees)
    } else {
        0f
    }
    val targetRotY = if (isFocused) {
        (touchOffsetX * maxTiltDegrees).coerceIn(-maxTiltDegrees, maxTiltDegrees)
    } else {
        0f
    }

    val rotX by animateFloatAsState(
        targetValue = targetRotX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tactileRotX",
    )
    val rotY by animateFloatAsState(
        targetValue = targetRotY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tactileRotY",
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tactileScale",
    )

    return this
        .graphicsLayer {
            this.cameraDistance = 16f * density
            this.rotationX = rotX
            this.rotationY = rotY
            this.scaleX = scale
            this.scaleY = scale
            this.shadowElevation = if (isFocused) 18.dp.toPx() else 4.dp.toPx()
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val normX = ((offset.x / size.width) - 0.5f) * 2f
                    val normY = ((offset.y / size.height) - 0.5f) * 2f
                    touchOffsetX = normX
                    touchOffsetY = normY
                },
                onDrag = { change, _ ->
                    val offset = change.position
                    val normX = ((offset.x / size.width) - 0.5f) * 2f
                    val normY = ((offset.y / size.height) - 0.5f) * 2f
                    touchOffsetX = normX.coerceIn(-1f, 1f)
                    touchOffsetY = normY.coerceIn(-1f, 1f)
                },
                onDragEnd = {
                    touchOffsetX = 0f
                    touchOffsetY = 0f
                },
                onDragCancel = {
                    touchOffsetX = 0f
                    touchOffsetY = 0f
                },
            )
        }
        .then(
            if (showSheen && isFocused) {
                Modifier.drawWithContent {
                    drawContent()
                    // Specular glass sheen highlight shifted by current tilt
                    val sheenShiftX = (rotY / maxTiltDegrees) * size.width * 0.4f
                    val sheenShiftY = (-rotX / maxTiltDegrees) * size.height * 0.4f

                    val sheenBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.02f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.02f),
                            Color.White.copy(alpha = 0.0f),
                        ),
                        start = Offset(sheenShiftX, sheenShiftY),
                        end = Offset(size.width + sheenShiftX, size.height + sheenShiftY),
                    )
                    drawRect(brush = sheenBrush)
                }
            } else {
                Modifier
            }
        )
}

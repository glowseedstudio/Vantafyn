package dev.vantafyn.feature.player.games

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors

@Composable
fun RetroTouchOverlay(
    visible: Boolean,
    onButtonPress: (RetroButton, Boolean) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        val view = LocalView.current
        val performHaptic = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Top Shoulder Bumpers & Menu Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // L1 Button
                TouchBumperButton(
                    label = "L",
                    onPress = { isDown ->
                        if (isDown) performHaptic()
                        onButtonPress(RetroButton.L1, isDown)
                    },
                )

                // Menu / Pause button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .border(1.dp, VantafynColors.Primary.copy(alpha = 0.6f), CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    performHaptic()
                                    onMenuClick()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = "Menu",
                        tint = VantafynColors.Primary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // R1 Button
                TouchBumperButton(
                    label = "R",
                    onPress = { isDown ->
                        if (isDown) performHaptic()
                        onButtonPress(RetroButton.R1, isDown)
                    },
                )
            }

            // Bottom Left: D-Pad
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp, start = 8.dp),
            ) {
                TouchDpad(
                    onDirectionChange = { dir, isDown ->
                        if (isDown) performHaptic()
                        onButtonPress(dir, isDown)
                    },
                )
            }

            // Bottom Center: Select & Start
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TouchPillButton(
                    label = "SELECT",
                    onPress = { isDown ->
                        if (isDown) performHaptic()
                        onButtonPress(RetroButton.Select, isDown)
                    },
                )
                TouchPillButton(
                    label = "START",
                    onPress = { isDown ->
                        if (isDown) performHaptic()
                        onButtonPress(RetroButton.Start, isDown)
                    },
                )
            }

            // Bottom Right: Action Diamond (A, B, X, Y)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 8.dp),
            ) {
                TouchActionDiamond(
                    onPress = { btn, isDown ->
                        if (isDown) performHaptic()
                        onButtonPress(btn, isDown)
                    },
                )
            }
        }
    }
}

@Composable
private fun TouchBumperButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(76.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) Color(0x9921D8FF) else Color(0x44000000))
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPress(true)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            isPressed = false
                            onPress(false)
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun TouchPillButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(68.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPressed) Color(0x88E026FF) else Color(0x44000000))
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPress(true)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            isPressed = false
                            onPress(false)
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else Color(0xCCFFFFFF),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun TouchDpad(
    onDirectionChange: (RetroButton, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dpadSize = 140.dp
    var activeDirection by remember { mutableStateOf<RetroButton?>(null) }

    Box(
        modifier = modifier
            .size(dpadSize)
            .clip(CircleShape)
            .background(Color(0x33000000))
            .border(1.dp, Color(0x44FFFFFF), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val dir = resolveDirection(down.position.x, down.position.y, size.width.toFloat(), size.height.toFloat())
                    if (dir != null) {
                        activeDirection = dir
                        onDirectionChange(dir, true)
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            activeDirection?.let { onDirectionChange(it, false) }
                            activeDirection = null
                            break
                        }
                        val currentDir = resolveDirection(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat())
                        if (currentDir != activeDirection) {
                            activeDirection?.let { onDirectionChange(it, false) }
                            activeDirection = currentDir
                            if (currentDir != null) onDirectionChange(currentDir, true)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Cross overlay
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x33FFFFFF)),
        )
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x33FFFFFF)),
        )

        // Center hub
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x55000000)),
        )
    }
}

private fun resolveDirection(x: Float, y: Float, width: Float, height: Float): RetroButton? {
    val centerX = width / 2f
    val centerY = height / 2f
    val dx = x - centerX
    val dy = y - centerY
    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
    if (dist < 15f) return null // Deadzone

    val angle = (kotlin.math.atan2(dy, dx) * 180 / Math.PI + 360) % 360
    return when {
        angle in 45.0..135.0 -> RetroButton.Down
        angle in 135.0..225.0 -> RetroButton.Left
        angle in 225.0..315.0 -> RetroButton.Up
        else -> RetroButton.Right
    }
}

@Composable
private fun TouchActionDiamond(
    onPress: (RetroButton, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = 150.dp
    val buttonSize = 48.dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // X Button (Top)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 0.dp),
        ) {
            RetroActionButton(
                label = "X",
                color = Color(0xFF21D8FF),
                button = RetroButton.X,
                onPress = onPress,
            )
        }

        // Y Button (Left)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 0.dp),
        ) {
            RetroActionButton(
                label = "Y",
                color = Color(0xFF00FFB2),
                button = RetroButton.Y,
                onPress = onPress,
            )
        }

        // B Button (Bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 0.dp),
        ) {
            RetroActionButton(
                label = "B",
                color = Color(0xFFFFB800),
                button = RetroButton.B,
                onPress = onPress,
            )
        }

        // A Button (Right)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 0.dp),
        ) {
            RetroActionButton(
                label = "A",
                color = Color(0xFFE026FF),
                button = RetroButton.A,
                onPress = onPress,
            )
        }
    }
}

@Composable
private fun RetroActionButton(
    label: String,
    color: Color,
    button: RetroButton,
    onPress: (RetroButton, Boolean) -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isPressed) color.copy(alpha = 0.85f) else Color(0x55000000))
            .border(2.dp, color.copy(alpha = 0.7f), CircleShape)
            .pointerInput(button) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPress(button, true)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            isPressed = false
                            onPress(button, false)
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.Black else color,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
        )
    }
}

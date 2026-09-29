package dev.vantafyn.feature.player.games

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.rounded.ChangeCircle
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import kotlin.math.roundToInt

enum class RetroControllerLayout {
    Standard,
    TwoButton,
    GBA,
    PlayStation,
    Nintendo64;

    companion object {
        fun from(systemId: String, core: String = ""): RetroControllerLayout {
            val key = (core.ifBlank { systemId }).lowercase().trim()
            return when {
                key == "n64" || key.contains("nintendo 64") || key.contains("mupen") || key.contains("parallel") -> Nintendo64
                key == "psx" || key == "ps1" || key.contains("playstation") || key.contains("pcsx") || key.contains("beetle") -> PlayStation
                key == "gba" || key.contains("advance") -> GBA
                key == "gb" || key == "gbc" || key == "nes" || key.contains("gameboy") || key.contains("famicom") ||
                    key.contains("atari") || key.contains("mastersystem") || key == "segams" || key == "segagg" -> TwoButton
                else -> Standard
            }
        }
    }
}

@Composable
fun RetroTouchOverlay(
    visible: Boolean,
    systemId: String = "",
    core: String = "",
    onButtonPress: (RetroButton, Boolean) -> Unit,
    onAxisChange: (String, Float) -> Unit = { _, _ -> },
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

        val layout = remember(systemId, core) { RetroControllerLayout.from(systemId, core) }
        var n64UseDpadInsteadOfStick by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // ── TOP SHOULDER BUMPERS & MENU ──────────────────────────────
            when (layout) {
                RetroControllerLayout.Nintendo64 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left: L and Z Trigger
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TouchBumperButton(
                                label = "L",
                                width = 64.dp,
                                onPress = { isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(RetroButton.L1, isDown)
                                },
                            )
                            TouchBumperButton(
                                label = "Z",
                                width = 64.dp,
                                color = Color(0xFFB388FF),
                                onPress = { isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(RetroButton.Z, isDown)
                                },
                            )
                        }

                        // Right: Menu & R Bumper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MenuPauseButton(onMenuClick = {
                                performHaptic()
                                onMenuClick()
                            })
                            TouchBumperButton(
                                label = "R",
                                width = 68.dp,
                                onPress = { isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(RetroButton.R1, isDown)
                                },
                            )
                        }
                    }
                }

                RetroControllerLayout.PlayStation -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left: L1 & L2
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TouchBumperButton(
                                label = "L1",
                                width = 58.dp,
                                onPress = { isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(RetroButton.L1, isDown)
                                },
                            )
                            TouchBumperButton(
                                label = "L2",
                                width = 58.dp,
                                color = Color(0xFFB388FF),
                                onPress = { isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(RetroButton.L2, isDown)
                                },
                            )
                        }

                        // Right: Menu, R2 & R1
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            MenuPauseButton(onMenuClick = {
                                performHaptic()
                                onMenuClick()
                            })
                            TouchBumperButton(
                                label = "R2",
                                width = 58.dp,
                                color = Color(0xFFB388FF),
                                onPress = { isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(RetroButton.R2, isDown)
                                },
                            )
                            TouchBumperButton(
                                label = "R1",
                                width = 58.dp,
                                onPress = { isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(RetroButton.R1, isDown)
                                },
                            )
                        }
                    }
                }

                RetroControllerLayout.TwoButton -> {
                    // Two-button systems (NES, GB) have no shoulder buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        MenuPauseButton(onMenuClick = {
                            performHaptic()
                            onMenuClick()
                        })
                    }
                }

                else -> {
                    // Standard (SNES, Genesis) & GBA
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TouchBumperButton(
                            label = "L",
                            onPress = { isDown ->
                                if (isDown) performHaptic()
                                onButtonPress(RetroButton.L1, isDown)
                            },
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MenuPauseButton(onMenuClick = {
                                performHaptic()
                                onMenuClick()
                            })
                            TouchBumperButton(
                                label = "R",
                                onPress = { isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(RetroButton.R1, isDown)
                                },
                            )
                        }
                    }
                }
            }

            // ── BOTTOM LEFT: D-PAD OR ANALOG STICK ───────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp, start = 8.dp),
            ) {
                if (layout == RetroControllerLayout.Nintendo64) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Toggle between Stick & D-pad
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x55000000))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable {
                                    performHaptic()
                                    n64UseDpadInsteadOfStick = !n64UseDpadInsteadOfStick
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChangeCircle,
                                contentDescription = null,
                                tint = VantafynColors.Primary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = if (n64UseDpadInsteadOfStick) "D-PAD ACTIVE" else "STICK ACTIVE",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }

                        if (!n64UseDpadInsteadOfStick) {
                            TouchAnalogStick(
                                onAxisChange = { x, y ->
                                    onAxisChange("left_x", x)
                                    onAxisChange("left_y", y)
                                },
                            )
                        } else {
                            TouchDpad(
                                onDirectionChange = { dir, isDown ->
                                    if (isDown) performHaptic()
                                    onButtonPress(dir, isDown)
                                },
                            )
                        }
                    }
                } else {
                    TouchDpad(
                        onDirectionChange = { dir, isDown ->
                            if (isDown) performHaptic()
                            onButtonPress(dir, isDown)
                        },
                    )
                }
            }

            // ── BOTTOM CENTER: SELECT & START ──────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (layout != RetroControllerLayout.Nintendo64) {
                    TouchPillButton(
                        label = "SELECT",
                        onPress = { isDown ->
                            if (isDown) performHaptic()
                            onButtonPress(RetroButton.Select, isDown)
                        },
                    )
                }
                TouchPillButton(
                    label = "START",
                    color = if (layout == RetroControllerLayout.Nintendo64) Color(0xFFFF4365) else Color.White.copy(alpha = 0.85f),
                    onPress = { isDown ->
                        if (isDown) performHaptic()
                        onButtonPress(RetroButton.Start, isDown)
                    },
                )
            }

            // ── BOTTOM RIGHT: ACTION BUTTONS ────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 8.dp),
            ) {
                when (layout) {
                    RetroControllerLayout.Nintendo64 -> {
                        TouchN64ActionCluster(
                            onPress = { btn, isDown ->
                                if (isDown) performHaptic()
                                onButtonPress(btn, isDown)
                            },
                        )
                    }

                    RetroControllerLayout.PlayStation -> {
                        TouchPlayStationDiamond(
                            onPress = { btn, isDown ->
                                if (isDown) performHaptic()
                                onButtonPress(btn, isDown)
                            },
                        )
                    }

                    RetroControllerLayout.TwoButton,
                    RetroControllerLayout.GBA -> {
                        TouchTwoButtonRow(
                            onPress = { btn, isDown ->
                                if (isDown) performHaptic()
                                onButtonPress(btn, isDown)
                            },
                        )
                    }

                    else -> {
                        // Standard 4-button diamond (SNES, Genesis)
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
    }
}

@Composable
private fun MenuPauseButton(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0x66000000))
            .border(1.dp, VantafynColors.Primary.copy(alpha = 0.6f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
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
}

// ────────────────────────────────────────────────────────────────────────
// ANALOG THUMBSTICK (N64 & 3D GAMES)
// ────────────────────────────────────────────────────────────────────────

@Composable
fun TouchAnalogStick(
    onAxisChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 140.dp,
    knobSizeDp: Dp = 54.dp,
) {
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { ((sizeDp - knobSizeDp) / 2).toPx() }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0x33000000))
            .border(1.5.dp, Color(0x5521D8FF), CircleShape)
            .pointerInput(maxRadiusPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    var currPos = down.position - center
                    val dist = currPos.getDistance()
                    if (dist > maxRadiusPx) {
                        currPos = currPos * (maxRadiusPx / dist)
                    }
                    knobOffset = currPos
                    val normX = (currPos.x / maxRadiusPx).coerceIn(-1f, 1f)
                    val normY = (currPos.y / maxRadiusPx).coerceIn(-1f, 1f)
                    onAxisChange(normX, normY)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            knobOffset = Offset.Zero
                            onAxisChange(0f, 0f)
                            break
                        }
                        var p = change.position - center
                        val d = p.getDistance()
                        if (d > maxRadiusPx) {
                            p = p * (maxRadiusPx / d)
                        }
                        knobOffset = p
                        val nx = (p.x / maxRadiusPx).coerceIn(-1f, 1f)
                        val ny = (p.y / maxRadiusPx).coerceIn(-1f, 1f)
                        onAxisChange(nx, ny)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Aesthetic concentric guide rings
        Box(
            modifier = Modifier
                .size(sizeDp * 0.65f)
                .border(1.dp, Color(0x22FFFFFF), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(sizeDp * 0.35f)
                .border(1.dp, Color(0x18FFFFFF), CircleShape)
        )

        // Draggable Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(knobSizeDp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xEE21D8FF),
                            Color(0x990099FF),
                            Color(0x77000000),
                        )
                    )
                )
                .border(2.dp, Color(0xCC21D8FF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xAAFFFFFF))
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// N64 ACTION CLUSTER: A/B BUTTONS + C-BUTTONS DIAMOND
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun TouchN64ActionCluster(
    onPress: (RetroButton, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Primary A & B buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RetroActionButton(
                label = "B",
                color = Color(0xFF00E676), // Classic N64 Green
                button = RetroButton.B,
                size = 46.dp,
                onPress = onPress,
            )
            RetroActionButton(
                label = "A",
                color = Color(0xFF21D8FF), // Classic N64 Cyan/Blue
                button = RetroButton.A,
                size = 50.dp,
                onPress = onPress,
            )
        }

        // Yellow C-Buttons Diamond
        TouchCButtonsDiamond(onPress = onPress)
    }
}

@Composable
private fun TouchCButtonsDiamond(
    onPress: (RetroButton, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = 114.dp
    val yellowC = Color(0xFFFFD700)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // C-Up
        Box(
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            CButton(
                label = "▲",
                color = yellowC,
                button = RetroButton.CUp,
                onPress = onPress,
            )
        }

        // C-Left
        Box(
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            CButton(
                label = "◀",
                color = yellowC,
                button = RetroButton.CLeft,
                onPress = onPress,
            )
        }

        // C-Right
        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            CButton(
                label = "▶",
                color = yellowC,
                button = RetroButton.CRight,
                onPress = onPress,
            )
        }

        // C-Down
        Box(
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CButton(
                label = "▼",
                color = yellowC,
                button = RetroButton.CDown,
                onPress = onPress,
            )
        }

        // Subtle Center "C" badge
        Text(
            text = "C",
            color = yellowC.copy(alpha = 0.5f),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun CButton(
    label: String,
    color: Color,
    button: RetroButton,
    onPress: (RetroButton, Boolean) -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isPressed) color.copy(alpha = 0.9f) else Color(0x55000000))
            .border(1.5.dp, color.copy(alpha = 0.8f), CircleShape)
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
            fontSize = 13.sp,
        )
    }
}

// ────────────────────────────────────────────────────────────────────────
// PLAYSTATION ACTION DIAMOND (△, ○, ✕, □)
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun TouchPlayStationDiamond(
    onPress: (RetroButton, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = 150.dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Triangle (Top) -> maps to RetroButton.X
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            RetroActionButton(
                label = "△",
                color = Color(0xFF00FFB2), // PS Green
                button = RetroButton.X,
                onPress = onPress,
            )
        }

        // Square (Left) -> maps to RetroButton.Y
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            RetroActionButton(
                label = "□",
                color = Color(0xFFE026FF), // PS Pink/Magenta
                button = RetroButton.Y,
                onPress = onPress,
            )
        }

        // Cross (Bottom) -> maps to RetroButton.B
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            RetroActionButton(
                label = "✕",
                color = Color(0xFF21D8FF), // PS Electric Blue
                button = RetroButton.B,
                onPress = onPress,
            )
        }

        // Circle (Right) -> maps to RetroButton.A
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            RetroActionButton(
                label = "○",
                color = Color(0xFFFF4365), // PS Coral Red
                button = RetroButton.A,
                onPress = onPress,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// TWO-BUTTON ROW (NES, GAME BOY, GBA)
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun TouchTwoButtonRow(
    onPress: (RetroButton, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // B Button (Angled slightly lower)
        Box(modifier = Modifier.offset(y = 10.dp)) {
            RetroActionButton(
                label = "B",
                color = Color(0xFFFFB800),
                button = RetroButton.B,
                size = 52.dp,
                onPress = onPress,
            )
        }

        // A Button (Angled slightly higher)
        Box(modifier = Modifier.offset(y = (-10).dp)) {
            RetroActionButton(
                label = "A",
                color = Color(0xFFE026FF),
                button = RetroButton.A,
                size = 52.dp,
                onPress = onPress,
            )
        }
    }
}

@Composable
private fun TouchBumperButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 76.dp,
    height: Dp = 38.dp,
    color: Color = Color(0xFF21D8FF),
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) color.copy(alpha = 0.6f) else Color(0x44000000))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
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
    color: Color = Color(0xFFE026FF),
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(68.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPressed) color.copy(alpha = 0.6f) else Color(0x44000000))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
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
    size: Dp = 48.dp,
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
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
            fontSize = (size.value * 0.38f).sp,
        )
    }
}

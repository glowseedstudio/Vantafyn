package dev.vantafyn.feature.player.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.ui.VantafynColors

enum class GameAspectRatio(val label: String, val cssValue: String) {
    Standard("4:3 Original", "4 / 3"),
    Widescreen("16:9 Wide", "16 / 9"),
    Square("1:1 Pixel", "1 / 1"),
}

enum class GameVideoFilter(val label: String, val id: String) {
    Crisp("Crisp Pixels", "crisp"),
    Crt("CRT Scanlines", "crt"),
    Smooth("Smooth Filter", "smooth"),
}

@Composable
fun GamePauseHud(
    visible: Boolean,
    game: GameDetail,
    aspectRatio: GameAspectRatio,
    fastForwardSpeed: Float,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    videoFilter: GameVideoFilter,
    onCycleVideoFilter: () -> Unit,
    showTouchControls: Boolean,
    onToggleTouchControls: () -> Unit,
    isTv: Boolean,
    onResume: () -> Unit,
    onToggleSpeed: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD0A0A0C))
                .clickable(onClick = onResume),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(380.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF131317))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(VantafynColors.Primary, VantafynColors.Secondary)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(enabled = false) {}
                    .padding(24.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    // Header: Title & System
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = game.cleanTitle.ifEmpty { game.title },
                            color = VantafynColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = game.systemId.uppercase(),
                                color = VantafynColors.Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (game.region != null) {
                                Text(
                                    text = "• ${game.region}",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                            if (game.core.isNotEmpty()) {
                                Text(
                                    text = "• ${game.core}",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Main Action Buttons Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Resume Button
                        HudMenuButton(
                            label = "Resume Game",
                            icon = Icons.Rounded.PlayArrow,
                            primary = true,
                            onClick = onResume,
                        )

                        // Speed & Aspect Ratio Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            HudMenuButton(
                                label = "Speed: ${fastForwardSpeed.toInt()}x",
                                icon = Icons.Rounded.FastForward,
                                onClick = onToggleSpeed,
                                modifier = Modifier.weight(1f),
                            )
                            HudMenuButton(
                                label = aspectRatio.label,
                                icon = Icons.Rounded.AspectRatio,
                                onClick = onCycleAspectRatio,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Sound & Video Filter Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            HudMenuButton(
                                label = if (isMuted) "Audio: Muted" else "Audio: On",
                                icon = if (isMuted) Icons.AutoMirrored.Rounded.VolumeMute else Icons.AutoMirrored.Rounded.VolumeUp,
                                onClick = onToggleMute,
                                modifier = Modifier.weight(1f),
                            )
                            HudMenuButton(
                                label = videoFilter.label,
                                icon = Icons.Rounded.Tv,
                                onClick = onCycleVideoFilter,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Touch Controls Toggle (mobile/tablet only)
                        if (!isTv) {
                            HudMenuButton(
                                label = if (showTouchControls) "Touch Controls: Visible" else "Touch Controls: Hidden",
                                icon = Icons.Rounded.TouchApp,
                                onClick = onToggleTouchControls,
                            )
                        }

                        // Reset Game
                        HudMenuButton(
                            label = "Reset Game",
                            icon = Icons.Rounded.Refresh,
                            onClick = onReset,
                        )

                        // Exit Game
                        HudMenuButton(
                            label = "Exit to Vantafyn",
                            icon = Icons.AutoMirrored.Rounded.ExitToApp,
                            danger = true,
                            onClick = onExit,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMenuButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    isLoading: Boolean = false,
) {
    val bgModifier = when {
        primary -> Modifier.background(
            Brush.horizontalGradient(listOf(VantafynColors.Primary, VantafynColors.Secondary)),
            RoundedCornerShape(14.dp)
        )
        danger -> Modifier.background(Color(0x33FF4444), RoundedCornerShape(14.dp))
        else -> Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(14.dp))
    }

    val contentColor = when {
        primary -> Color.Black
        danger -> Color(0xFFFF5555)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(bgModifier)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

package dev.vantafyn.feature.music.audiostream

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.media.music.MusicQualityPreferences
import dev.vantafyn.core.media.music.MusicStreamingQuality
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassModalPanel
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder

private val SheetRailClearance = 112.dp

/**
 * Layered icon featuring a settings gear/cog behind a foreground music note.
 */
@Composable
fun MusicBitrateSettingsIcon(
    modifier: Modifier = Modifier,
    tint: Color = VantafynColors.Ink,
    selected: Boolean = false,
) {
    val accentTint = if (selected) Color(0xFF21D8FF) else tint
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Settings cog in the background (slightly shifted top-left)
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint = accentTint.copy(alpha = 0.42f),
            modifier = Modifier
                .size(23.dp)
                .offset(x = (-2.5).dp, y = (-2.5).dp),
        )
        // Music note in the foreground (slightly shifted bottom-right)
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = accentTint,
            modifier = Modifier
                .size(17.dp)
                .offset(x = 3.5.dp, y = 3.5.dp),
        )
    }
}

/**
 * Dedicated top-left Now Playing icon button for streaming bitrate settings.
 */
@Composable
fun MusicBitrateSettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val haptic = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.size(size.dp),
    ) {
        MusicBitrateSettingsIcon(
            modifier = Modifier.size(24.dp),
            tint = VantafynColors.Ink.copy(alpha = 0.92f),
        )
    }
}

/**
 * Premium bottom sheet to dynamically change the music streaming bitrate on the fly.
 */
@Composable
fun MusicStreamingBitrateSheet(
    visible: Boolean,
    currentQuality: MusicStreamingQuality,
    onSelectQuality: (MusicStreamingQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val extraOffsetPx = remember(density) {
        with(density) { (SheetRailClearance + 56.dp).roundToPx() }
    }

    val isCellular = remember(visible) { MusicQualityPreferences.isMeteredOrCellular(context) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it + extraOffsetPx },
                    animationSpec = spring(
                        dampingRatio = 0.84f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(
                    targetOffsetY = { it + extraOffsetPx },
                    animationSpec = tween(
                        durationMillis = 260,
                        easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f),
                    ),
                ) + fadeOut(animationSpec = tween(220)),
            ) {
                VantafynGlassModalPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .padding(bottom = SheetRailClearance)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.5.dp),
                    cornerRadius = 30.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 38.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f)),
                        )

                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF21D8FF).copy(alpha = 0.16f))
                                        .border(1.dp, Color(0xFF21D8FF).copy(alpha = 0.32f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    MusicBitrateSettingsIcon(
                                        modifier = Modifier.size(24.dp),
                                        tint = Color(0xFF21D8FF),
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Streaming Audio Quality",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = VantafynColors.Ink,
                                        ),
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (isCellular) Icons.Rounded.NetworkCell else Icons.Rounded.NetworkWifi,
                                            contentDescription = null,
                                            tint = if (isCellular) Color(0xFFFFD166) else Color(0xFF21D8FF),
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = if (isCellular) "Cellular network" else "Wi-Fi network",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = VantafynColors.Muted,
                                                fontSize = 12.sp,
                                            ),
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = VantafynColors.Muted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        // Quality Options List
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MusicStreamingQuality.entries.forEach { quality ->
                                val isSelected = quality == currentQuality
                                val accentColor = when (quality) {
                                    MusicStreamingQuality.Lossless -> Color(0xFF21D8FF)
                                    MusicStreamingQuality.High -> Color(0xFFFFD166)
                                    MusicStreamingQuality.Medium -> Color(0xFF6EE7FF)
                                    MusicStreamingQuality.DataSaver -> Color(0xFF4ADE80)
                                }

                                VantafynGlassSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSelectQuality(quality)
                                        }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) accentColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(18.dp),
                                        ),
                                    variant = if (isSelected) VantafynGlassVariant.Panel else VantafynGlassVariant.Card,
                                    cornerRadius = 18.dp,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    text = quality.label,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) VantafynColors.Ink else VantafynColors.Ink.copy(alpha = 0.88f),
                                                    ),
                                                )
                                                if (quality == MusicStreamingQuality.Lossless) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF21D8FF).copy(alpha = 0.20f))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp),
                                                    ) {
                                                        Text(
                                                            text = "MASTER",
                                                            color = Color(0xFF21D8FF),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                letterSpacing = 0.5.sp,
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = quality.description,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = VantafynColors.Muted,
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                ),
                                            )
                                        }

                                        Icon(
                                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = if (isSelected) "Selected" else "Not selected",
                                            tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.30f),
                                            modifier = Modifier
                                                .padding(start = 12.dp)
                                                .size(22.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Footer hint
                        Text(
                            text = "Bitrate switches apply immediately to the current track and upcoming queue without resetting playback position.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = VantafynColors.Muted.copy(alpha = 0.68f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

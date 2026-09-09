package dev.vantafyn.feature.music.replaygain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.VantafynMusicTrack
import dev.vantafyn.core.media.replaygain.ReplayGainPreferences
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassModalPanel
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReplayGainDialog(
    playbackController: MusicPlaybackController,
    currentTrack: VantafynMusicTrack?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isEnabled by ReplayGainPreferences.isEnabledFlow.collectAsState()
    val preAmpWithGain by ReplayGainPreferences.preAmpWithReplayGainFlow.collectAsState()
    val gainWithoutGain by ReplayGainPreferences.gainWithoutReplayGainFlow.collectAsState()
    val preventClipping by ReplayGainPreferences.preventClippingFlow.collectAsState()

    val processor = playbackController.replayGainAudioProcessor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        VantafynGlassModalPanel(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .vantafynAnimatedModalBorder(cornerRadius = 28.dp, strokeWidth = 1.5.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {},
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF21D8FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF21D8FF),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Loudness Leveling",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VantafynColors.Ink,
                            )
                            Text(
                                text = "ReplayGain DSP Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = VantafynColors.Muted,
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = VantafynColors.Muted,
                        )
                    }
                }

                // Active Track Diagnostics Card
                VantafynGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    variant = VantafynGlassVariant.Card,
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "ACTIVE TRACK STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF21D8FF),
                                letterSpacing = 0.8.sp,
                            )
                            if (processor.isPeakLimitingActive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD166),
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        "Anti-Clip Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFD166),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        if (currentTrack != null) {
                            Text(
                                text = currentTrack.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = VantafynColors.Ink,
                                maxLines = 1,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                val trackGain = currentTrack.replayGainTrackGainDb
                                val trackPeak = currentTrack.replayGainTrackPeak
                                val effectiveGain = processor.currentEffectiveGainDb

                                Column {
                                    Text(
                                        text = "Embedded Gain",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VantafynColors.Muted,
                                    )
                                    Text(
                                        text = if (trackGain != null) String.format(Locale.US, "%+.2f dB", trackGain) else "None (Fallback)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (trackGain != null) VantafynColors.Ink else Color(0xFFFFD166),
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Peak Level",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VantafynColors.Muted,
                                    )
                                    Text(
                                        text = if (trackPeak != null) String.format(Locale.US, "%.3f", trackPeak) else "Unknown",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VantafynColors.Ink,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Applied Gain",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VantafynColors.Muted,
                                    )
                                    Text(
                                        text = if (isEnabled) String.format(Locale.US, "%+.2f dB", effectiveGain) else "0.00 dB (Off)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isEnabled) Color(0xFF21D8FF) else VantafynColors.Muted,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No track currently playing",
                                style = MaterialTheme.typography.bodySmall,
                                color = VantafynColors.Muted,
                            )
                        }
                    }
                }

                // Master Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { ReplayGainPreferences.setEnabled(context, !isEnabled) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable ReplayGain",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = VantafynColors.Ink,
                        )
                        Text(
                            text = "Equalizes perceived loudness across different tracks automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = VantafynColors.Muted,
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { ReplayGainPreferences.setEnabled(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF21D8FF),
                            uncheckedThumbColor = VantafynColors.Muted,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        ),
                    )
                }

                AnimatedVisibility(
                    visible = isEnabled,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Pre-amp With ReplayGain Slider
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Pre-amp with ReplayGain",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VantafynColors.Ink,
                                )
                                Text(
                                    text = String.format(Locale.US, "%+.1f dB", preAmpWithGain),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21D8FF),
                                )
                            }
                            Text(
                                text = "Baseline boost or cut applied on top of embedded track tags",
                                style = MaterialTheme.typography.bodySmall,
                                color = VantafynColors.Muted,
                            )
                            Slider(
                                value = preAmpWithGain,
                                onValueChange = { ReplayGainPreferences.setPreAmpWithReplayGain(context, (it * 2).roundToInt() / 2f) },
                                valueRange = -12f..12f,
                                steps = 47, // 0.5 dB increments
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF21D8FF),
                                    activeTrackColor = Color(0xFF21D8FF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                                ),
                            )
                        }

                        // Gain Without ReplayGain Slider (Fallback)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Fallback without ReplayGain",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VantafynColors.Ink,
                                )
                                Text(
                                    text = String.format(Locale.US, "%+.1f dB", gainWithoutGain),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD166),
                                )
                            }
                            Text(
                                text = "Default volume adjustment applied to tracks lacking ReplayGain tags",
                                style = MaterialTheme.typography.bodySmall,
                                color = VantafynColors.Muted,
                            )
                            Slider(
                                value = gainWithoutGain,
                                onValueChange = { ReplayGainPreferences.setGainWithoutReplayGain(context, (it * 2).roundToInt() / 2f) },
                                valueRange = -12f..12f,
                                steps = 47, // 0.5 dB increments
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD166),
                                    activeTrackColor = Color(0xFFFFD166),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                                ),
                            )
                        }

                        // Prevent Clipping Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable { ReplayGainPreferences.setPreventClipping(context, !preventClipping) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Prevent Clipping (Peak Limiter)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VantafynColors.Ink,
                                )
                                Text(
                                    text = "Automatically scales down gain if peak amplitude exceeds 0 dBFS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VantafynColors.Muted,
                                )
                            }
                            Switch(
                                checked = preventClipping,
                                onCheckedChange = { ReplayGainPreferences.setPreventClipping(context, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF21D8FF),
                                    uncheckedThumbColor = VantafynColors.Muted,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

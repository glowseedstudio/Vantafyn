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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import dev.vantafyn.core.media.VantafynAudioStreamInfo
import dev.vantafyn.core.media.VantafynMusicTrack
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassModalPanel
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import java.util.Locale

private val SheetRailClearance = 112.dp

/**
 * Premium frosted-glass quality pill displayed directly above the play/pause button
 * on the Now Playing screen. Displays active codec, bitrate, sample rate, and
 * Hi-Res / Lossless indicators.
 */
@Composable
fun AudioQualityBadgePill(
    audioStreamInfo: VantafynAudioStreamInfo?,
    track: VantafynMusicTrack?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val info = audioStreamInfo ?: VantafynAudioStreamInfo.fromTrackAndFormat(track, null)
    val displaySummary = info?.displaySummary?.takeIf { it.isNotBlank() }
        ?: listOfNotNull(track?.codec?.uppercase(Locale.ROOT) ?: track?.container?.uppercase(Locale.ROOT), "AUDIO").first()

    val isHiRes = info?.isHiRes == true
    val isLossless = info?.isLossless == true

    val borderColor = when {
        isHiRes -> Color(0xFFFFD166).copy(alpha = 0.38f)
        isLossless -> Color(0xFF21D8FF).copy(alpha = 0.35f)
        else -> Color.White.copy(alpha = 0.14f)
    }

    VantafynGlassSurface(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(999.dp)),
        variant = VantafynGlassVariant.Chip,
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isHiRes) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFD166).copy(alpha = 0.22f))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "HI-RES",
                        color = Color(0xFFFFD166),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                }
            } else if (isLossless) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF21D8FF).copy(alpha = 0.20f))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "LOSSLESS",
                        color = Color(0xFF21D8FF),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = VantafynColors.Muted,
                    modifier = Modifier.size(12.dp),
                )
            }

            Text(
                text = displaySummary,
                color = VantafynColors.Ink.copy(alpha = 0.90f),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = "View audio stream details",
                tint = VantafynColors.Muted.copy(alpha = 0.70f),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/**
 * Premium bottom sheet revealing full audio stream technical specifications:
 * Codec, Container, Bit Depth, Sample Rate, Bitrate, Channels, and ReplayGain Leveling.
 * Uses exact spring slide-up and cubic-bezier exit animations matching Vantafyn sheets.
 */
@Composable
fun AudioStreamDetailsSheet(
    visible: Boolean,
    track: VantafynMusicTrack?,
    audioStreamInfo: VantafynAudioStreamInfo?,
    onDismiss: () -> Unit,
    onOpenReplayGainSettings: (() -> Unit)? = null,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    val density = LocalDensity.current
    val extraOffsetPx = remember(density) {
        with(density) { (SheetRailClearance + 56.dp).roundToPx() }
    }
    val info = audioStreamInfo ?: VantafynAudioStreamInfo.fromTrackAndFormat(track, null)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            VantafynGlassModalPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = SheetRailClearance)
                    .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.5.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight + extraOffsetPx },
                            animationSpec = spring(
                                dampingRatio = 0.84f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight + extraOffsetPx },
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f),
                            ),
                        ),
                    ),
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(20.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = Color(0xFF21D8FF),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Audio Quality & Stream",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = VantafynColors.Ink,
                                ),
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = VantafynColors.Muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Quality Tier Hero Card
                    TierHeroCard(info = info)

                    // Audio Stream Specifications Grid
                    StreamSpecsGrid(info = info, track = track)

                    // ReplayGain & EQ Shortcut Button
                    if (onOpenReplayGainSettings != null) {
                        VantafynGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = {
                                    onDismiss()
                                    onOpenReplayGainSettings()
                                }),
                            variant = VantafynGlassVariant.Card,
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFF21D8FF),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Column {
                                        Text(
                                            text = "ReplayGain & Audio Processing",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = VantafynColors.Ink,
                                            ),
                                        )
                                        Text(
                                            text = "Loudness Leveling, Pre-amp & Clipping Prevention",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = VantafynColors.Muted,
                                            ),
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                                    contentDescription = null,
                                    tint = VantafynColors.Muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TierHeroCard(info: VantafynAudioStreamInfo?) {
    val isHiRes = info?.isHiRes == true
    val isLossless = info?.isLossless == true

    val gradientBrush = when {
        isHiRes -> Brush.horizontalGradient(
            listOf(
                Color(0xFFFFD166).copy(alpha = 0.22f),
                Color(0xFF21D8FF).copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.04f),
            )
        )
        isLossless -> Brush.horizontalGradient(
            listOf(
                Color(0xFF21D8FF).copy(alpha = 0.20f),
                Color(0xFF00A3FF).copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.04f),
            )
        )
        else -> Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f),
            )
        )
    }

    val cardBorder = when {
        isHiRes -> Color(0xFFFFD166).copy(alpha = 0.35f)
        isLossless -> Color(0xFF21D8FF).copy(alpha = 0.30f)
        else -> Color.White.copy(alpha = 0.10f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradientBrush)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            isHiRes -> Color(0xFFFFD166).copy(alpha = 0.25f)
                            isLossless -> Color(0xFF21D8FF).copy(alpha = 0.22f)
                            else -> Color.White.copy(alpha = 0.10f)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        isHiRes -> Icons.Rounded.AutoAwesome
                        else -> Icons.Rounded.GraphicEq
                    },
                    contentDescription = null,
                    tint = when {
                        isHiRes -> Color(0xFFFFD166)
                        isLossless -> Color(0xFF21D8FF)
                        else -> VantafynColors.Ink
                    },
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = when {
                        isHiRes -> "Hi-Res Lossless Audio"
                        isLossless -> "Lossless Studio Audio"
                        else -> "High Quality Audio"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isHiRes -> Color(0xFFFFD166)
                            isLossless -> Color(0xFF21D8FF)
                            else -> VantafynColors.Ink
                        },
                    ),
                )
                Text(
                    text = when {
                        isHiRes -> "Bit-perfect master stream delivered up to 24-bit / 192 kHz with zero compression loss."
                        isLossless -> "Delivered in full CD-quality bit-depth and sample frequency without lossy compression."
                        else -> "High-bitrate stream optimized for acoustic fidelity and network efficiency."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = VantafynColors.Muted,
                        lineHeight = 16.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StreamSpecsGrid(
    info: VantafynAudioStreamInfo?,
    track: VantafynMusicTrack?,
) {
    val rate = info?.sampleRateHz
    val sampleRateStr = if (rate != null && rate > 0) {
        val khz = rate / 1000.0
        if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format(Locale.ROOT, "%.1f kHz", khz)
    } else "44.1 kHz"

    val depth = info?.bitDepth
    val bitDepthStr = when {
        depth != null && depth > 0 -> "${depth}-bit"
        info?.isLossless == true -> "16-bit (CD Quality)"
        else -> "Lossy Encoded"
    }

    val br = info?.bitrateKbps
    val bitrateStr = when {
        br != null && br > 0 -> String.format(Locale.ROOT, "%,d kbps", br)
        else -> "Variable"
    }

    val channelsStr = when (info?.channelCount) {
        1 -> "Mono (1.0)"
        2 -> "Stereo (2.0)"
        6 -> "5.1 Surround"
        8 -> "7.1 Surround"
        null -> "Stereo (2.0)"
        else -> "${info.channelCount} Channels"
    }

    val appliedGain = info?.replayGainAppliedDb
    val replayGainStr = if (appliedGain != null) {
        String.format(Locale.ROOT, "%+.1f dB (Leveling Active)", appliedGain)
    } else if (track?.replayGainTrackGainDb != null) {
        String.format(Locale.ROOT, "%+.1f dB (Disabled)", track.replayGainTrackGainDb)
    } else {
        "Off (0.0 dB)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SpecRow(label = "Codec / Format", value = info?.codec ?: track?.codec?.uppercase(Locale.ROOT) ?: "FLAC / Native")
        SpecRow(label = "Container", value = info?.container ?: track?.container?.uppercase(Locale.ROOT) ?: "Direct Stream")
        SpecRow(label = "Sample Rate", value = sampleRateStr)
        SpecRow(label = "Bit Depth", value = bitDepthStr)
        SpecRow(label = "Bitrate", value = bitrateStr)
        SpecRow(label = "Channels", value = channelsStr)
        SpecRow(label = "ReplayGain Loudness", value = replayGainStr, isHighlight = info?.replayGainAppliedDb != null)
        SpecRow(label = "Audio Engine", value = "AndroidX Media3 ExoPlayer")
    }
}

@Composable
private fun SpecRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = VantafynColors.Muted,
            ),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isHighlight) Color(0xFF21D8FF) else VantafynColors.Ink,
            ),
        )
    }
}

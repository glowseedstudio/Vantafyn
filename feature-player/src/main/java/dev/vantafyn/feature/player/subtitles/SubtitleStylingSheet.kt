package dev.vantafyn.feature.player.subtitles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.Cue
import androidx.media3.ui.SubtitleView
import dev.vantafyn.core.media.subtitles.SubtitleBackgroundStyle
import dev.vantafyn.core.media.subtitles.SubtitleEdgeStyle
import dev.vantafyn.core.media.subtitles.SubtitleStyleConfig
import dev.vantafyn.core.media.subtitles.SubtitleStylePreferences
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant

private data class ColorOption(val label: String, val colorLong: Long, val displayColor: Color)

private val SubtitleColorPalette = listOf(
    ColorOption("White", 0xFFFFFFFF, Color.White),
    ColorOption("Cinema Gold", 0xFFFFD700, Color(0xFFFFD700)),
    ColorOption("Cyan Neon", 0xFF31D7FF, Color(0xFF31D7FF)),
    ColorOption("Soft Mint", 0xFF81F499, Color(0xFF81F499)),
    ColorOption("Amber", 0xFFFFB347, Color(0xFFFFB347)),
    ColorOption("Pastel Pink", 0xFFFF8DA1, Color(0xFFFF8DA1)),
)

@Composable
fun SubtitleStylingContent(
    currentConfig: SubtitleStyleConfig,
    onConfigChange: (SubtitleStyleConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. CINEMATIC REAL-TIME PREVIEW STAGE
        VantafynGlassCard(
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF192238),
                                Color(0xFF0A0F1D),
                                Color(0xFF03050A),
                            ),
                        ),
                    ),
            ) {
                // Subtle cinema background stars / bokeh
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color(0xFF31D7FF).copy(alpha = 0.08f), radius = 180f, center = Offset(size.width * 0.2f, size.height * 0.3f))
                    drawCircle(Color(0xFFFF5277).copy(alpha = 0.07f), radius = 140f, center = Offset(size.width * 0.85f, size.height * 0.7f))
                }

                // Live Android SubtitleView
                AndroidView(
                    factory = { ctx ->
                        SubtitleView(ctx).apply {
                            currentConfig.applyTo(this)
                            setCues(
                                listOf(
                                    Cue.Builder()
                                        .setText("The journey ahead will demand everything from us.\nAre you ready to discover what lies beyond?")
                                        .build(),
                                ),
                            )
                        }
                    },
                    update = { view ->
                        currentConfig.applyTo(view)
                        view.setCues(
                            listOf(
                                Cue.Builder()
                                    .setText("The journey ahead will demand everything from us.\nAre you ready to discover what lies beyond?")
                                    .build(),
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )

                // Top Badge indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "LIVE CINEMA PREVIEW",
                        color = Color(0xFF31D7FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        }

        // 2. QUICK STYLE PRESETS
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CURATED PRESETS",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetChip("Classic", isSelected = currentConfig == SubtitleStyleConfig.ClassicCinema) {
                    val updated = SubtitleStyleConfig.ClassicCinema
                    onConfigChange(updated)
                    SubtitleStylePreferences.save(context, updated)
                }
                PresetChip("Golden Hour", isSelected = currentConfig == SubtitleStyleConfig.GoldenHour) {
                    val updated = SubtitleStyleConfig.GoldenHour
                    onConfigChange(updated)
                    SubtitleStylePreferences.save(context, updated)
                }
                PresetChip("High Contrast", isSelected = currentConfig == SubtitleStyleConfig.HighContrast) {
                    val updated = SubtitleStyleConfig.HighContrast
                    onConfigChange(updated)
                    SubtitleStylePreferences.save(context, updated)
                }
                PresetChip("Cyber Neon", isSelected = currentConfig == SubtitleStyleConfig.CyberNeon) {
                    val updated = SubtitleStyleConfig.CyberNeon
                    onConfigChange(updated)
                    SubtitleStylePreferences.save(context, updated)
                }
            }
        }

        // 3. FONT SIZE SLIDER & FORMAT
        VantafynGlassSurface(
            variant = VantafynGlassVariant.Card,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Font Size",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${currentConfig.fontSizeSp.toInt()} sp",
                        color = Color(0xFF31D7FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Slider(
                    value = currentConfig.fontSizeSp,
                    onValueChange = { size ->
                        val updated = currentConfig.copy(fontSizeSp = size)
                        onConfigChange(updated)
                        SubtitleStylePreferences.save(context, updated)
                    },
                    valueRange = 12f..36f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF31D7FF),
                        activeTrackColor = Color(0xFF31D7FF),
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )

                // Bold and Italic row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VantafynGlassSurface(
                        variant = if (currentConfig.isBold) VantafynGlassVariant.Button else VantafynGlassVariant.Chip,
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val updated = currentConfig.copy(isBold = !currentConfig.isBold)
                                onConfigChange(updated)
                                SubtitleStylePreferences.save(context, updated)
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.FormatBold,
                                contentDescription = null,
                                tint = if (currentConfig.isBold) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Bold",
                                color = if (currentConfig.isBold) Color(0xFF31D7FF) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    VantafynGlassSurface(
                        variant = if (currentConfig.isItalic) VantafynGlassVariant.Button else VantafynGlassVariant.Chip,
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val updated = currentConfig.copy(isItalic = !currentConfig.isItalic)
                                onConfigChange(updated)
                                SubtitleStylePreferences.save(context, updated)
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.FormatItalic,
                                contentDescription = null,
                                tint = if (currentConfig.isItalic) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Italic",
                                color = if (currentConfig.isItalic) Color(0xFF31D7FF) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        // 4. TEXT COLOR PALETTE
        VantafynGlassSurface(
            variant = VantafynGlassVariant.Card,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Text Color",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SubtitleColorPalette.forEach { opt ->
                        val isSelected = (currentConfig.textColor and 0x00FFFFFF) == (opt.colorLong and 0x00FFFFFF)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(opt.displayColor)
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.20f),
                                    CircleShape,
                                )
                                .clickable {
                                    val updated = currentConfig.copy(textColor = opt.colorLong)
                                    onConfigChange(updated)
                                    SubtitleStylePreferences.save(context, updated)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (opt.colorLong == 0xFFFFFFFF || opt.colorLong == 0xFFFFD700) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. EDGE & OUTLINE STYLE
        VantafynGlassSurface(
            variant = VantafynGlassVariant.Card,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Edge & Shadows",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SubtitleEdgeStyle.entries.filter { it != SubtitleEdgeStyle.Raised && it != SubtitleEdgeStyle.Depressed }.forEach { edge ->
                        val isSelected = currentConfig.edgeStyle == edge
                        VantafynGlassSurface(
                            variant = if (isSelected) VantafynGlassVariant.Button else VantafynGlassVariant.Chip,
                            cornerRadius = 10.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val updated = currentConfig.copy(edgeStyle = edge)
                                    onConfigChange(updated)
                                    SubtitleStylePreferences.save(context, updated)
                                },
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = edge.label,
                                    color = if (isSelected) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. BACKGROUND BOX STYLE
        VantafynGlassSurface(
            variant = VantafynGlassVariant.Card,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Background Box",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SubtitleBackgroundStyle.entries.forEach { bg ->
                        val isSelected = currentConfig.backgroundStyle == bg
                        VantafynGlassSurface(
                            variant = if (isSelected) VantafynGlassVariant.Button else VantafynGlassVariant.Chip,
                            cornerRadius = 10.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val updated = currentConfig.copy(backgroundStyle = bg)
                                    onConfigChange(updated)
                                    SubtitleStylePreferences.save(context, updated)
                                },
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = bg.label,
                                    color = if (isSelected) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. RESET ACTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            VantafynGlassSurface(
                variant = VantafynGlassVariant.Chip,
                cornerRadius = 999.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable {
                        SubtitleStylePreferences.reset(context)
                        onConfigChange(SubtitleStyleConfig.Default)
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Reset to default styling",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PresetChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    VantafynGlassSurface(
        variant = if (isSelected) VantafynGlassVariant.Button else VantafynGlassVariant.Chip,
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = title,
            color = if (isSelected) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.85f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
        )
    }
}

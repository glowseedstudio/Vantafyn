package dev.vantafyn.feature.music.autoeq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.autoeq.AutoEqHardwareStatus
import dev.vantafyn.core.media.autoeq.AutoEqPreset
import dev.vantafyn.core.media.autoeq.AutoEqRepository
import dev.vantafyn.core.media.autoeq.EqualizerBandInfo
import dev.vantafyn.core.ui.VantafynColors

@Composable
fun AutoEqSearchScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = remember { MusicPlaybackController.get(context) }
    val effectsManager = controller.audioEffectsManager
    val repository = remember { AutoEqRepository.get(context) }

    val eqState by effectsManager.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedBrandFilter by remember { mutableStateOf<String?>(null) }
    var presets by remember { mutableStateOf<List<AutoEqPreset>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(searchQuery, selectedBrandFilter) {
        isLoading = true
        val all = repository.search(searchQuery)
        presets = if (selectedBrandFilter != null) {
            all.filter { it.brand.equals(selectedBrandFilter, ignoreCase = true) }
        } else {
            all
        }
        isLoading = false
    }

    val brands = remember {
        listOf("All", "Sony", "Apple", "Sennheiser", "Bose", "Audio-Technica", "Beyerdynamic", "Moondrop", "Samsung")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VantafynColors.Graphite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            // Header Row
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(VantafynColors.Primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = VantafynColors.Primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "AutoEQ Calibration",
                            color = VantafynColors.Ink,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Hardware headphone compensation curves",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Hardware Warning if unsupported
            if (eqState.hardwareStatus is AutoEqHardwareStatus.Unsupported) {
                val reason = (eqState.hardwareStatus as AutoEqHardwareStatus.Unsupported).reason
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF5A2020).copy(alpha = 0.45f))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF5252))
                    Text(
                        text = reason,
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Master Control & Active Preset Card
            ActivePresetCard(
                isEnabled = eqState.isEnabled,
                selectedPreset = eqState.selectedPreset,
                onToggleEnabled = { effectsManager.setEnabled(it) },
                onReset = { effectsManager.selectPreset(null) },
            )

            // Equalizer Hardware Bands Visualizer
            if (eqState.hardwareBands.isNotEmpty() && eqState.isEnabled) {
                Spacer(Modifier.height(12.dp))
                HardwareBandsVisualizer(bands = eqState.hardwareBands)
            }

            Spacer(Modifier.height(14.dp))

            // Search Box
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = {
                    Text("Search model (e.g. WH-1000XM5, AirPods, HD 600)...", color = VantafynColors.Muted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = VantafynColors.Muted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = VantafynColors.Muted)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = VantafynColors.Surface.copy(alpha = 0.85f),
                    unfocusedContainerColor = VantafynColors.Surface.copy(alpha = 0.65f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = VantafynColors.Ink,
                    unfocusedTextColor = VantafynColors.Ink,
                ),
            )

            Spacer(Modifier.height(10.dp))

            // Brand Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                brands.forEach { brand ->
                    val isSelected = if (brand == "All") selectedBrandFilter == null else selectedBrandFilter == brand
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedBrandFilter = if (brand == "All") null else brand
                        },
                        label = { Text(brand, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VantafynColors.Primary.copy(alpha = 0.22f),
                            selectedLabelColor = VantafynColors.Primary,
                            containerColor = VantafynColors.Surface.copy(alpha = 0.5f),
                            labelColor = VantafynColors.Muted,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) VantafynColors.Primary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Preset List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (presets.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No headphones found matching \"$searchQuery\"",
                                color = VantafynColors.Muted,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                items(presets, key = { it.id }) { preset ->
                    val isCurrent = eqState.selectedPreset?.id == preset.id
                    HeadphonePresetItem(
                        preset = preset,
                        isSelected = isCurrent,
                        onClick = {
                            effectsManager.selectPreset(preset)
                            if (!eqState.isEnabled) {
                                effectsManager.setEnabled(true)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePresetCard(
    isEnabled: Boolean,
    selectedPreset: AutoEqPreset?,
    onToggleEnabled: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isEnabled && selectedPreset != null) VantafynColors.Primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(300),
        label = "activeBorder",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VantafynColors.Surface)
            .border(1.2.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = if (selectedPreset != null) selectedPreset.name else "No Profile Selected",
                        color = VantafynColors.Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (selectedPreset != null) {
                            "${selectedPreset.brand} · ${selectedPreset.type} · ${selectedPreset.source}"
                        } else {
                            "Select your headphone model below to calibrate"
                        },
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                    )
                }

                Switch(
                    checked = isEnabled && selectedPreset != null,
                    onCheckedChange = { onToggleEnabled(it) },
                    enabled = selectedPreset != null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VantafynColors.Ink,
                        checkedTrackColor = VantafynColors.Primary,
                        uncheckedThumbColor = VantafynColors.Muted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                    ),
                )
            }

            if (selectedPreset != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Anti-clipping Headroom: ${selectedPreset.preamp} dB",
                        color = VantafynColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Text(
                        text = "Reset to Flat",
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(onClick = onReset)
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HardwareBandsVisualizer(
    bands: List<EqualizerBandInfo>,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VantafynColors.Surface.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Applied Hardware Bands (${bands.size} Bands)",
                color = VantafynColors.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bands.forEach { band ->
                    val gainDb = band.currentGainDb
                    val freqLabel = formatFreq(band.centerFreqHz)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp),
                    ) {
                        // Gain dB label
                        Text(
                            text = String.format(java.util.Locale.US, "%+.1f", gainDb),
                            color = if (gainDb != 0f) VantafynColors.Primary else VantafynColors.Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(4.dp))

                        // Mini Equalizer Bar
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            val normalizedHeight = ((gainDb + 15f) / 30f).coerceIn(0.1f, 1.0f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(normalizedHeight)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(VantafynColors.Primary, Color(0xFF00796B)),
                                        ),
                                    ),
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Freq label
                        Text(
                            text = freqLabel,
                            color = VantafynColors.Muted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadphonePresetItem(
    preset: AutoEqPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) VantafynColors.Primary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(250),
        label = "itemBorder",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) VantafynColors.Primary.copy(alpha = 0.10f) else VantafynColors.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) VantafynColors.Primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = if (isSelected) VantafynColors.Primary else VantafynColors.Muted,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column {
                    Text(
                        text = preset.name,
                        color = if (isSelected) VantafynColors.Primary else VantafynColors.Ink,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${preset.brand} · ${preset.type} (${preset.source})",
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(VantafynColors.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun formatFreq(freqHz: Int): String {
    return if (freqHz >= 1000) {
        val kHz = freqHz / 1000.0
        if (kHz >= 10.0 || kHz % 1.0 == 0.0) "${kHz.toInt()}k" else String.format(java.util.Locale.US, "%.1fk", kHz)
    } else {
        "${freqHz}"
    }
}

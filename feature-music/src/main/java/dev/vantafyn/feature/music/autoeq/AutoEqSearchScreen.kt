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
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.runtime.produceState
import dev.vantafyn.core.media.autoeq.ConnectedAudioDeviceDetector
import dev.vantafyn.core.media.autoeq.ConnectedBluetoothAudioDevice
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.autoeq.AutoEqHardwareStatus
import dev.vantafyn.core.media.autoeq.AutoEqPreset
import dev.vantafyn.core.media.autoeq.AutoEqRepository
import dev.vantafyn.core.media.autoeq.EqualizerBandInfo
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassChip
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder

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
    var showCreditsDialog by remember { mutableStateOf(false) }

    val connectedDevice by produceState<ConnectedBluetoothAudioDevice?>(
        initialValue = remember { ConnectedAudioDeviceDetector.getConnectedBluetoothDevice(context) },
    ) {
        ConnectedAudioDeviceDetector.observeConnectedBluetoothDevice(context).collect {
            value = it
        }
    }
    var suggestedPresets by remember { mutableStateOf<List<AutoEqPreset>>(emptyList()) }

    LaunchedEffect(connectedDevice?.name) {
        val devName = connectedDevice?.name
        suggestedPresets = if (!devName.isNullOrBlank()) {
            repository.findMatchesForDevice(devName)
        } else {
            emptyList()
        }
    }

    LaunchedEffect(searchQuery, selectedBrandFilter) {
        isLoading = true
        val all = repository.search(searchQuery)
        presets = if (selectedBrandFilter != null) {
            all.filter { it.brand.equals(selectedBrandFilter, ignoreCase = true) }
        } else {
            all
        }.distinctBy { it.id }
        isLoading = false
    }

    val brands = remember {
        listOf("All", "Sony", "Apple", "Sennheiser", "Bose", "Beyerdynamic", "Moondrop", "AKG", "Hifiman", "Audio-Technica", "Audeze")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VantafynColors.Graphite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF21D8FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF21D8FF).copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF21D8FF),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "AutoEQ Calibration",
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Hardware headphone compensation curves",
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = { showCreditsDialog = true },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "About AutoEQ & Licenses",
                            tint = VantafynColors.Ink,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = VantafynColors.Ink,
                            modifier = Modifier.size(22.dp),
                        )
                    }
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

            val currentConnected = connectedDevice
            if (currentConnected != null && suggestedPresets.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                ConnectedDeviceSuggestionSection(
                    deviceName = currentConnected.name,
                    presets = suggestedPresets,
                    selectedPreset = eqState.selectedPreset,
                    isEnabled = eqState.isEnabled,
                    onSelectPreset = { preset ->
                        effectsManager.selectPreset(preset)
                        if (!eqState.isEnabled) {
                            effectsManager.setEnabled(true)
                        }
                    },
                )
            }

            Spacer(Modifier.height(14.dp))

            // Search Box with VantafynTextField (compact single-line + focus glow highlight)
            VantafynTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search model",
                placeholder = "Search model (e.g. WH-1000XM5)...",
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = VantafynColors.Muted,
                            )
                        }
                    }
                } else {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = VantafynColors.Muted,
                        )
                    }
                },
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
                    VantafynGlassChip(
                        selected = isSelected,
                        onClick = {
                            selectedBrandFilter = if (brand == "All") null else brand
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = brand,
                            color = if (isSelected) Color(0xFF21D8FF) else VantafynColors.Muted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
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

        if (showCreditsDialog) {
            AutoEqCreditsDialog(onDismiss = { showCreditsDialog = false })
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
    val isActivated = isEnabled && selectedPreset != null

    VantafynGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = VantafynGlassVariant.Card,
        selected = isActivated,
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header / Status Eyebrow Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActivated) Color(0xFF21D8FF) else VantafynColors.Muted.copy(alpha = 0.5f),
                            ),
                    )
                    Text(
                        text = if (isActivated) "CALIBRATION ACTIVE" else if (selectedPreset != null) "CALIBRATION PAUSED" else "HEADPHONE PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (isActivated) Color(0xFF21D8FF) else VantafynColors.Muted,
                    )
                }

                if (selectedPreset != null) {
                    VantafynGlassSurface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onReset),
                        variant = VantafynGlassVariant.Chip,
                        cornerRadius = 999.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Reset to Flat",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Main Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActivated) {
                                Color(0xFF21D8FF).copy(alpha = 0.16f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActivated) Color(0xFF21D8FF).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (selectedPreset != null) Icons.Rounded.Headphones else Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (isActivated) Color(0xFF21D8FF) else VantafynColors.Muted,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedPreset?.name ?: "No Profile Selected",
                        color = VantafynColors.Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (selectedPreset != null) {
                            "${selectedPreset.brand} · ${selectedPreset.type} (${selectedPreset.source})"
                        } else {
                            "Select your headphone model below to calibrate"
                        },
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Switch(
                    checked = isActivated,
                    onCheckedChange = { onToggleEnabled(it) },
                    enabled = selectedPreset != null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF21D8FF),
                        uncheckedThumbColor = VantafynColors.Muted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }

            // Headroom Details if selected
            if (selectedPreset != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Anti-clipping Headroom",
                            style = MaterialTheme.typography.labelSmall,
                            color = VantafynColors.Muted,
                        )
                    }

                    Text(
                        text = "${selectedPreset.preamp} dB",
                        color = Color(0xFFFFD166),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
    VantafynGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = VantafynGlassVariant.Card,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "APPLIED HARDWARE BANDS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color(0xFF21D8FF),
                )
                Text(
                    text = "${bands.size} Bands",
                    style = MaterialTheme.typography.labelSmall,
                    color = VantafynColors.Muted,
                )
            }

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
                            color = if (gainDb != 0f) Color(0xFF21D8FF) else VantafynColors.Muted,
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
                                            listOf(Color(0xFF21D8FF), Color(0xFF00796B)),
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
    VantafynGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        variant = VantafynGlassVariant.Card,
        selected = isSelected,
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFF21D8FF).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF21D8FF).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF21D8FF) else VantafynColors.Muted,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column {
                    Text(
                        text = preset.name,
                        color = if (isSelected) Color(0xFF21D8FF) else VantafynColors.Ink,
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
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF21D8FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF0C101B),
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

@Composable
private fun AutoEqCreditsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        modifier = Modifier
            .imePadding()
            .vantafynAnimatedModalBorder(cornerRadius = 24.dp),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Got it",
                    color = Color(0xFF21D8FF),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://autoeq.app")))
                    }
                },
            ) {
                Text(
                    text = "autoeq.app",
                    color = VantafynColors.Muted,
                )
            }
        },
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF21D8FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF21D8FF).copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF21D8FF),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        text = "AutoEQ & Dataset Credits",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Open Source License & Attribution",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Headphone equalization compensation curves and target profiles are powered by Jaakko Pasanen's open-source AutoEq project, released under the permissive MIT License.",
                    color = VantafynColors.Ink.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )

                VantafynGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    variant = VantafynGlassVariant.Panel,
                    cornerRadius = 14.dp,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MEASUREMENT SOURCES & CONTRIBUTORS",
                            color = Color(0xFF21D8FF),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                        )
                        Text(
                            text = "• oratory1990 (Harman target ear simulator measurements)\n" +
                                "• crinacle (IEM & headphone frequency response database)\n" +
                                "• Rtings (Standardized acoustic laboratory tests)\n" +
                                "• Innerfidelity (Legacy database by Tyll Hertsens)\n" +
                                "• Super Review & Headphone.com Legacy",
                            color = VantafynColors.Ink.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Color(0xFF21D8FF),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "MIT License · Copyright © Jaakko Pasanen",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
    )
}

@Composable
private fun ConnectedDeviceSuggestionSection(
    deviceName: String,
    presets: List<AutoEqPreset>,
    selectedPreset: AutoEqPreset?,
    isEnabled: Boolean,
    onSelectPreset: (AutoEqPreset) -> Unit,
) {
    val topPreset = presets.first()
    val isTopActive = selectedPreset?.id == topPreset.id && isEnabled

    VantafynGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = VantafynGlassVariant.Card,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Bluetooth Icon + Connected Headphones Badge + Device Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF21D8FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = Color(0xFF21D8FF),
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "CONNECTED HEADPHONES",
                            color = Color(0xFF21D8FF),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                        )
                        Text(
                            text = deviceName,
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isTopActive) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF21D8FF).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF21D8FF).copy(alpha = 0.40f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF21D8FF),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "CALIBRATED",
                            color = Color(0xFF21D8FF),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            ),
                        )
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
            )

            // Primary Suggestion Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isTopActive) Color(0xFF21D8FF).copy(alpha = 0.12f)
                        else Color.White.copy(alpha = 0.05f),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isTopActive) Color(0xFF21D8FF).copy(alpha = 0.35f)
                        else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelectPreset(topPreset) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTopActive) Color(0xFF21D8FF).copy(alpha = 0.20f)
                                else Color.White.copy(alpha = 0.08f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = if (isTopActive) Color(0xFF21D8FF) else VantafynColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Column {
                        Text(
                            text = topPreset.name,
                            color = if (isTopActive) Color(0xFF21D8FF) else VantafynColors.Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${topPreset.brand} · ${topPreset.source} · Preamp ${topPreset.preamp} dB",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // One-tap action button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isTopActive) Color(0xFF21D8FF)
                            else Color(0xFF21D8FF).copy(alpha = 0.18f),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (isTopActive) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF0C101B),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Applied",
                            color = Color(0xFF0C101B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(
                            text = "Apply EQ",
                            color = Color(0xFF21D8FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Secondary variations (if multiple presets matched: ANC On/Off, alternate measurements, etc.)
            if (presets.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.drop(1).take(5).forEach { preset ->
                        val isPresetActive = selectedPreset?.id == preset.id && isEnabled
                        val label = buildString {
                            append(preset.source)
                            val cleanPresetName = preset.name.replace(preset.brand, "").trim()
                            if (cleanPresetName.isNotEmpty() && cleanPresetName != topPreset.name.replace(preset.brand, "").trim()) {
                                append(" · ")
                                append(cleanPresetName)
                            }
                        }

                        VantafynGlassChip(
                            selected = isPresetActive,
                            onClick = { onSelectPreset(preset) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = label,
                                color = if (isPresetActive) Color(0xFF21D8FF) else VantafynColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}



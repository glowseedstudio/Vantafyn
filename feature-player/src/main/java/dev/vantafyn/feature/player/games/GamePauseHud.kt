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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import dev.vantafyn.core.emulator.net.LinkSessionManager
import dev.vantafyn.core.emulator.net.LinkTransportMode
import dev.vantafyn.core.emulator.net.LinkSessionState
import dev.vantafyn.core.ui.VantafynGradients
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.emulator.NdsScreenLayout
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.ui.VantafynColors

enum class GameAspectRatio(val label: String, val cssValue: String) {
    Standard("4:3 Original", "4 / 3"),
    Widescreen("16:9 Wide", "16 / 9"),
    Square("1:1 Pixel", "1 / 1"),
}

enum class GameVideoFilter(val label: String, val id: String) {
    Crisp("Crisp Pixels", "crisp"),
    LcdGrid("LCD Grid", "lcd"),
    Crt("CRT Scanlines", "crt"),
    Smooth("Smooth Filter", "smooth");

    fun nextForSystem(isHandheld: Boolean): GameVideoFilter {
        return if (isHandheld) {
            when (this) {
                Crisp -> LcdGrid
                LcdGrid -> Smooth
                Smooth, Crt -> Crisp
            }
        } else {
            when (this) {
                Crisp -> Crt
                Crt -> Smooth
                Smooth, LcdGrid -> Crisp
            }
        }
    }
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
    hasPhysicalGamepad: Boolean = false,
    isTv: Boolean,
    isNativeMode: Boolean = false,
    isHandheld: Boolean = false,
    ndsLayout: NdsScreenLayout = NdsScreenLayout.TopBottom,
    onCycleNdsLayout: () -> Unit = {},
    hasSecondaryDisplay: Boolean = false,
    swapDualScreens: Boolean = false,
    onToggleSwapDualScreens: () -> Unit = {},
    onSyncCloudSave: () -> Unit = {},
    isSyncingSave: Boolean = false,
    syncSaveSuccess: Boolean = false,
    gbaColorCorrection: Boolean = true,
    onToggleGbaColorCorrection: () -> Unit = {},
    gbaAudioFiltering: Boolean = true,
    onToggleGbaAudioFiltering: () -> Unit = {},
    gbcColorCorrection: Boolean = true,
    onToggleGbcColorCorrection: () -> Unit = {},
    gbPalette: String = "colorized",
    onCycleGbPalette: () -> Unit = {},
    lcdGhosting: Boolean = false,
    onToggleLcdGhosting: () -> Unit = {},
    citraResolution: String = "1x (400x240)",
    onCycleCitraResolution: () -> Unit = {},
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
                    // Header: Title, System & Mode Badge
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
                            if (hasSecondaryDisplay) {
                                Text(
                                    text = "• Dual Physical Displays",
                                    color = Color(0xFF30D158),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else if (isNativeMode) {
                                Text(
                                    text = "• 64-Bit Native Core",
                                    color = Color(0xFF64D2FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            } else if (game.core.isNotEmpty()) {
                                Text(
                                    text = "• ${game.core}",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                            if (game.region != null) {
                                Text(
                                    text = "• ${game.region}",
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

                        // Speed & Screen Layout / Aspect Ratio Row
                        val is3dsSystem = game.systemId.lowercase() in listOf("3ds", "n3ds", "nintendo3ds") || game.core.contains("azahar", ignoreCase = true) || game.core.contains("citra", ignoreCase = true)
                        val isNdsSystem = game.systemId.lowercase() in listOf("nds", "ds") || game.core.contains("melonds", ignoreCase = true)
                        val isDualScreenSystem = isNdsSystem || is3dsSystem
                        val isGbaSystem = game.systemId.lowercase() in listOf("gba", "gameboy advance", "game boy advance") || game.core.contains("gpsp", ignoreCase = true) || game.core.contains("mgba", ignoreCase = true)
                        val isGbSystem = game.systemId.lowercase() in listOf("gb", "gbc", "gameboy", "game boy", "gameboy color", "game boy color") || game.core.contains("gambatte", ignoreCase = true) || game.core.contains("tgbdual", ignoreCase = true) || game.core.contains("sameboy", ignoreCase = true)
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
                            if (hasSecondaryDisplay) {
                                HudMenuButton(
                                    label = if (swapDualScreens) "Attached: Top" else "Attached: Touch",
                                    icon = Icons.Rounded.SwapVert,
                                    onClick = onToggleSwapDualScreens,
                                    modifier = Modifier.weight(1.3f),
                                )
                            } else if (isDualScreenSystem) {
                                HudMenuButton(
                                    label = ndsLayout.label,
                                    icon = Icons.Rounded.Dashboard,
                                    onClick = onCycleNdsLayout,
                                    modifier = Modifier.weight(1.3f),
                                )
                            } else {
                                val aspectLabel = when {
                                    isGbaSystem && aspectRatio == GameAspectRatio.Standard -> "3:2 Original"
                                    isGbaSystem && aspectRatio == GameAspectRatio.Widescreen -> "16:9 Wide"
                                    isGbaSystem && aspectRatio == GameAspectRatio.Square -> "1:1 Pixel"
                                    isGbSystem && aspectRatio == GameAspectRatio.Standard -> "10:9 Original"
                                    isGbSystem && aspectRatio == GameAspectRatio.Widescreen -> "4:3 Full"
                                    isGbSystem && aspectRatio == GameAspectRatio.Square -> "1:1 Pixel"
                                    else -> aspectRatio.label
                                }
                                HudMenuButton(
                                    label = aspectLabel,
                                    icon = Icons.Rounded.AspectRatio,
                                    onClick = onCycleAspectRatio,
                                    modifier = Modifier.weight(1f),
                                )
                            }
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

                        // 3DS Internal Resolution Scale & Screen Prominence Row
                        if (is3dsSystem) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                HudMenuButton(
                                    label = "Scale: ${citraResolution.substringBefore(" ")}",
                                    icon = Icons.Rounded.Tune,
                                    onClick = onCycleCitraResolution,
                                    modifier = Modifier.weight(1f),
                                )
                                HudMenuButton(
                                    label = if (swapDualScreens) "Prominent: Touch" else "Prominent: Top",
                                    icon = Icons.Rounded.SwapVert,
                                    onClick = onToggleSwapDualScreens,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // GBA Color Profile & Audio Anti-Aliasing Profile
                        if (isGbaSystem) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                HudMenuButton(
                                    label = if (gbaColorCorrection) "Color: LCD Balanced" else "Color: Vivid Raw",
                                    icon = Icons.Rounded.Palette,
                                    onClick = onToggleGbaColorCorrection,
                                    modifier = Modifier.weight(1f),
                                )
                                HudMenuButton(
                                    label = if (gbaAudioFiltering) "Audio: Anti-Aliased" else "Audio: Raw Hardware",
                                    icon = Icons.Rounded.GraphicEq,
                                    onClick = onToggleGbaAudioFiltering,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // Game Boy DMG Monochrome Palette & LCD Ghosting
                        val isGbPure = isGbSystem && (game.systemId.lowercase() in listOf("gb", "gameboy", "game boy") || (!game.systemId.lowercase().contains("gbc") && !game.title.contains("color", ignoreCase = true)))
                        val isGbcSystem = isGbSystem && (game.systemId.lowercase() in listOf("gbc", "gameboy color", "game boy color") || game.title.contains("color", ignoreCase = true))

                        if (isGbPure) {
                            val palLabel = when (gbPalette) {
                                "dmg" -> "Palette: DMG Green"
                                "pocket" -> "Palette: Pocket B&W"
                                else -> "Palette: Colorized"
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                HudMenuButton(
                                    label = palLabel,
                                    icon = Icons.Rounded.Palette,
                                    onClick = onCycleGbPalette,
                                    modifier = Modifier.weight(1f),
                                )
                                HudMenuButton(
                                    label = if (lcdGhosting) "Ghosting: LCD Authentic" else "Ghosting: Crisp Off",
                                    icon = Icons.Rounded.Tv,
                                    onClick = onToggleLcdGhosting,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else if (isGbcSystem) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                HudMenuButton(
                                    label = if (gbcColorCorrection) "Color: LCD Balanced" else "Color: Vivid Raw",
                                    icon = Icons.Rounded.Palette,
                                    onClick = onToggleGbcColorCorrection,
                                    modifier = Modifier.weight(1f),
                                )
                                HudMenuButton(
                                    label = if (lcdGhosting) "Ghosting: LCD Authentic" else "Ghosting: Crisp Off",
                                    icon = Icons.Rounded.Tv,
                                    onClick = onToggleLcdGhosting,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // In-game Battery Save (SRAM) Cloud Sync
                        if (isNativeMode) {
                            HudMenuButton(
                                label = when {
                                    syncSaveSuccess -> "Battery Save Synced to Cloud!"
                                    isSyncingSave -> "Syncing Battery Save to Cloud..."
                                    else -> "Sync Battery Save to Cloud"
                                    },
                                icon = if (syncSaveSuccess) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload,
                                isLoading = isSyncingSave,
                                onClick = onSyncCloudSave,
                            )
                        }

                        // Touch Controls Toggle (mobile/tablet only)
                        if (!isTv) {
                            val padLabel = when {
                                showTouchControls -> "Virtual Pad: Visible"
                                hasPhysicalGamepad -> "Virtual Pad: Off (Controller Active)"
                                else -> "Virtual Pad: Hidden"
                            }
                            HudMenuButton(
                                label = padLabel,
                                icon = Icons.Rounded.TouchApp,
                                onClick = onToggleTouchControls,
                            )
                            if (isDualScreenSystem) {
                                Text(
                                    text = if (is3dsSystem) "Stylus touch is active directly on the lower 3DS screen" else "Stylus touch is active directly on the lower DS screen",
                                    color = VantafynColors.Muted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }

                        // Wireless Link Cable & Multiplayer Session
                        if (isNativeMode && (isNdsSystem || isGbaSystem || isGbSystem)) {
                            val context = LocalContext.current
                            val linkManager = remember { LinkSessionManager.getInstance(context) }
                            val linkSessionState by linkManager.sessionState.collectAsState()
                            val discoveredPeers by linkManager.discoveredPeers.collectAsState()
                            val activeSession by linkManager.activeSession.collectAsState()
                            val transportMode = linkManager.transportMode

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Cable,
                                            contentDescription = null,
                                            tint = when (linkSessionState) {
                                                LinkSessionState.CONNECTED -> Color(0xFF00E676)
                                                LinkSessionState.ADVERTISING -> Color(0xFF00E5FF)
                                                LinkSessionState.DISCOVERING -> Color(0xFFA855F7)
                                                else -> VantafynColors.Muted
                                            },
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = "Wireless Link Cable",
                                            color = VantafynColors.Ink,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = when {
                                                transportMode == LinkTransportMode.OFFLINE -> "Offline"
                                                linkSessionState == LinkSessionState.CONNECTED -> "Connected"
                                                linkSessionState == LinkSessionState.ADVERTISING -> "Hosting Room"
                                                linkSessionState == LinkSessionState.DISCOVERING -> "Scanning"
                                                else -> "Ready"
                                            },
                                            color = when {
                                                linkSessionState == LinkSessionState.CONNECTED -> Color(0xFF00E676)
                                                linkSessionState == LinkSessionState.ADVERTISING -> Color(0xFF00E5FF)
                                                else -> VantafynColors.Muted
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    if (transportMode != LinkTransportMode.OFFLINE) {
                                        // Actions when not connected
                                        if (linkSessionState != LinkSessionState.CONNECTED && linkSessionState != LinkSessionState.ADVERTISING) {
                                            if (transportMode == LinkTransportMode.SERVER_RELAY) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(VantafynGradients.accentHorizontal())
                                                            .clickable {
                                                                linkManager.startServerRelaySession(
                                                                    gameId = game.id,
                                                                    gameTitle = game.title,
                                                                    core = game.core,
                                                                    roomCode = linkManager.serverRoomCode,
                                                                    isHost = true,
                                                                )
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = "Host Room #${linkManager.serverRoomCode}",
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.08f))
                                                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                linkManager.startServerRelaySession(
                                                                    gameId = game.id,
                                                                    gameTitle = game.title,
                                                                    core = game.core,
                                                                    roomCode = linkManager.serverRoomCode,
                                                                    isHost = false,
                                                                )
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = "Join Room #${linkManager.serverRoomCode}",
                                                            color = Color(0xFF00E5FF),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(VantafynGradients.accentHorizontal())
                                                            .clickable {
                                                                linkManager.startHosting(
                                                                    gameId = game.id,
                                                                    gameTitle = game.title,
                                                                    core = game.core,
                                                                )
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = "Host Room",
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.08f))
                                                            .clickable {
                                                                linkManager.startDiscovery(filterGameId = game.id)
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = "Scan Peers",
                                                            color = VantafynColors.Ink,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                    }
                                                }
                                            }

                                            // If nearby peers found
                                            if (discoveredPeers.isNotEmpty()) {
                                                Text(
                                                    text = "Nearby Handhelds Found:",
                                                    color = VantafynColors.Muted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                discoveredPeers.forEach { peer ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.05f))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = peer.hostName,
                                                                color = VantafynColors.Ink,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                            )
                                                            Text(
                                                                text = "${peer.ip}:${peer.port}",
                                                                color = VantafynColors.Muted,
                                                                fontSize = 10.sp,
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0xFF00E5FF))
                                                                .clickable {
                                                                    linkManager.connectToPeer(peer)
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                                        ) {
                                                            Text(
                                                                text = "Connect",
                                                                color = Color.Black,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Connected or Hosting: Show active session info & disconnect button
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = if (linkSessionState == LinkSessionState.ADVERTISING) {
                                                        if (activeSession?.transportMode == LinkTransportMode.SERVER_RELAY)
                                                            "Online Room #${activeSession?.peerName?.substringAfter("#") ?: "1234"} active"
                                                        else
                                                            "Room open on port ${activeSession?.port} (${activeSession?.remoteIp})"
                                                    } else {
                                                        "Connected to ${activeSession?.peerName}"
                                                    },
                                                    color = Color(0xFF00E5FF),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFFF3366).copy(alpha = 0.2f))
                                                        .border(1.dp, Color(0xFFFF3366).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                        .clickable { linkManager.disconnect() }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                ) {
                                                    Text(
                                                        text = "Disconnect",
                                                        color = Color(0xFFFF5277),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Wireless multiplayer is turned off in Retro Settings",
                                            color = VantafynColors.Muted,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
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
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val bgModifier = when {
        !enabled -> Modifier.background(Color(0x11FFFFFF), RoundedCornerShape(14.dp))
        primary -> Modifier.background(
            Brush.horizontalGradient(listOf(VantafynColors.Primary, VantafynColors.Secondary)),
            RoundedCornerShape(14.dp)
        )
        danger -> Modifier.background(Color(0x33FF4444), RoundedCornerShape(14.dp))
        else -> Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(14.dp))
    }

    val contentColor = when {
        !enabled -> Color(0x55FFFFFF)
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
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
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
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
}

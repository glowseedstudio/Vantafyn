package dev.vantafyn.feature.home.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import dev.vantafyn.core.emulator.net.LinkSessionManager
import dev.vantafyn.core.emulator.net.LinkTransportMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TextButton
import dev.vantafyn.core.ui.VantafynGradientProgressBar
import dev.vantafyn.core.ui.VantafynSoundEffects
import dev.vantafyn.feature.home.games.grid.rememberIsGamepadConnected
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradientIcon
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.player.games.GameStorageManager
import dev.vantafyn.feature.player.games.BatterySaveImportNormalizer
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PendingBatterySaveImport(val fileName: String, val bytes: ByteArray)

private enum class GameModalFocus {
    CORE,
    CLOSE,
    PLAY,
    DOWNLOAD,
    IMPORT,
    LINK,
}

data class SystemCoreOption(
    val id: String,
    val name: String,
    val description: String,
    val isRecommended: Boolean = false,
)

fun getAvailableCoresForSystem(systemId: String): List<SystemCoreOption> {
    val s = systemId.lowercase().trim()
    return when (s) {
        "gb", "gbc" -> listOf(
            SystemCoreOption(
                id = "gambatte",
                name = "Gambatte",
                description = "Single-player, high battery efficiency, audio/video accuracy.",
                isRecommended = true,
            ),
            SystemCoreOption(
                id = "tgbdual",
                name = "TGB Dual",
                description = "Built-in Link Cable emulation for trading between saves.",
                isRecommended = false,
            ),
            SystemCoreOption(
                id = "sameboy",
                name = "SameBoy",
                description = "Cycle-accurate Game Boy emulation with high audio fidelity.",
                isRecommended = false,
            ),
        )
        "gba" -> listOf(
            SystemCoreOption(
                id = "gpsp",
                name = "gpSP",
                description = "Optimized dynamic recompiler, highest battery life & smooth 60fps.",
                isRecommended = true,
            ),
            SystemCoreOption(
                id = "mgba",
                name = "mGBA",
                description = "High accuracy modern Game Boy Advance emulation.",
                isRecommended = false,
            ),
        )
        "nds", "ds" -> listOf(
            SystemCoreOption(
                id = "melonds",
                name = "melonDS",
                description = "Full dual-screen support, FreeBIOS, fast 64-bit performance.",
                isRecommended = true,
            ),
            SystemCoreOption(
                id = "desmume",
                name = "DeSmuME",
                description = "Classic Nintendo DS emulation.",
                isRecommended = false,
            ),
        )
        "3ds", "n3ds", "nintendo3ds" -> listOf(
            SystemCoreOption(
                id = "azahar",
                name = "Azahar",
                description = "Modern Nintendo 3DS Libretro core with dual-screen support.",
                isRecommended = true,
            ),
            SystemCoreOption(
                id = "citra",
                name = "Citra",
                description = "Classic Nintendo 3DS Libretro core.",
                isRecommended = false,
            ),
        )
        else -> emptyList()
    }
}

@Composable
fun GameDetailModal(
    game: GameDetail?,
    onDismiss: () -> Unit,
    onPlay: (GameDetail) -> Unit,
    onDeleteSave: ((GameDetail) -> Unit)? = null,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    onDownloadOffline: ((GameDetail) -> Unit)? = null,
    onDeleteOffline: ((GameDetail) -> Unit)? = null,
    session: JellyfinSession? = null,
    gamesRepository: JellyfinGamesRepository? = null,
    modifier: Modifier = Modifier,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val storageManager = remember(gamesRepository) { gamesRepository?.let { GameStorageManager(context, it) } }

    val availableCores = remember(game?.systemId) {
        game?.systemId?.let { getAvailableCoresForSystem(it) } ?: emptyList()
    }
    val defaultCore = remember(availableCores, game?.core) {
        availableCores.firstOrNull { it.isRecommended }?.id ?: game?.core?.ifEmpty { "auto" } ?: "auto"
    }
    var currentCoreId by remember(game?.id) {
        val saved = game?.id?.let { prefs.getString("selected_core_$it", null) }
        mutableStateOf(saved ?: defaultCore)
    }
    var showCorePickerModal by remember { mutableStateOf(false) }

    val linkManager = remember { LinkSessionManager.getInstance(context) }
    var showLinkChoiceModal by remember { mutableStateOf(false) }

    LaunchedEffect(session?.server?.url, session?.accessToken) {
        session?.server?.url?.let {
            if (it.isNotBlank()) linkManager.serverBaseUrl = it
        }
        session?.accessToken?.let {
            if (it.isNotBlank()) linkManager.serverAuthToken = it
        }
    }

    val isLinkCapable = remember(game?.systemId, currentCoreId) {
        val s = game?.systemId?.lowercase().orEmpty()
        val c = currentCoreId.lowercase()
        s in setOf("nds", "ds", "gba", "gb", "gbc") || c in setOf("melonds", "mgba", "gpsp", "tgbdual", "gambatte", "sameboy")
    }

    var pendingImport by remember { mutableStateOf<PendingBatterySaveImport?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val detail = game
        if (uri == null || detail == null || storageManager == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val fileName = displayNameForUri(context, uri) ?: "selected save"
            val readResult = runCatching { readBatterySaveUri(context, uri) }
            val prepared = readResult.getOrNull()?.let {
                BatterySaveImportNormalizer.normalize(fileName, it, detail.systemId, detail.core)
            }
            withContext(Dispatchers.Main) {
                when {
                    prepared == null -> importMessage = readResult.exceptionOrNull()?.message ?: "Could not read that save file."
                    prepared.isFailure -> importMessage = prepared.exceptionOrNull()?.message ?: "That battery save is not compatible with this game."
                    else -> pendingImport = PendingBatterySaveImport(fileName, prepared.getOrThrow().bytes)
                }
            }
        }
    }

    val isGamepadConnected = rememberIsGamepadConnected()
    val modalFocusRequester = remember { FocusRequester() }

    var focusedItem by remember(game?.id) { mutableStateOf(GameModalFocus.PLAY) }

    LaunchedEffect(game) {
        if (game != null) {
            focusedItem = GameModalFocus.PLAY
            try {
                modalFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    AnimatedVisibility(
        visible = game != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        if (game == null) return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE608080C))
                .clickable(onClick = onDismiss)
                .focusRequester(modalFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_B || event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                            onDismiss()
                            return@onPreviewKeyEvent true
                        }
                        return@onPreviewKeyEvent false
                    }
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            focusedItem = when (focusedItem) {
                                GameModalFocus.CLOSE -> if (availableCores.isNotEmpty()) GameModalFocus.CORE else GameModalFocus.CLOSE
                                GameModalFocus.CORE -> GameModalFocus.CORE
                                GameModalFocus.LINK -> if (storageManager != null) GameModalFocus.IMPORT else if (onDownloadOffline != null || isDownloaded || isDownloading) GameModalFocus.DOWNLOAD else GameModalFocus.PLAY
                                GameModalFocus.IMPORT -> if (onDownloadOffline != null || isDownloaded || isDownloading) GameModalFocus.DOWNLOAD else GameModalFocus.PLAY
                                GameModalFocus.DOWNLOAD -> GameModalFocus.PLAY
                                GameModalFocus.PLAY -> GameModalFocus.PLAY
                            }
                            VantafynSoundEffects.playChannelClick(context)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            focusedItem = when (focusedItem) {
                                GameModalFocus.CORE -> GameModalFocus.CLOSE
                                GameModalFocus.CLOSE -> GameModalFocus.CLOSE
                                GameModalFocus.PLAY -> {
                                    if (onDownloadOffline != null || isDownloaded || isDownloading) GameModalFocus.DOWNLOAD
                                    else if (storageManager != null) GameModalFocus.IMPORT
                                    else if (isLinkCapable) GameModalFocus.LINK
                                    else GameModalFocus.PLAY
                                }
                                GameModalFocus.DOWNLOAD -> {
                                    if (storageManager != null) GameModalFocus.IMPORT
                                    else if (isLinkCapable) GameModalFocus.LINK
                                    else GameModalFocus.DOWNLOAD
                                }
                                GameModalFocus.IMPORT -> {
                                    if (isLinkCapable) GameModalFocus.LINK
                                    else GameModalFocus.IMPORT
                                }
                                GameModalFocus.LINK -> GameModalFocus.LINK
                            }
                            VantafynSoundEffects.playChannelClick(context)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            focusedItem = if (availableCores.isNotEmpty()) GameModalFocus.CORE else GameModalFocus.CLOSE
                            VantafynSoundEffects.playChannelClick(context)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            focusedItem = GameModalFocus.PLAY
                            VantafynSoundEffects.playChannelClick(context)
                            true
                        }
                        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                            when (focusedItem) {
                                GameModalFocus.PLAY -> onPlay(game.copy(core = currentCoreId))
                                GameModalFocus.DOWNLOAD -> {
                                    if (isDownloaded && onDeleteOffline != null) onDeleteOffline(game)
                                    else if (!isDownloaded && !isDownloading && onDownloadOffline != null) onDownloadOffline(game)
                                }
                                GameModalFocus.IMPORT -> {
                                    if (storageManager != null) importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                                }
                                GameModalFocus.LINK -> {
                                    if (isLinkCapable) showLinkChoiceModal = true
                                }
                                GameModalFocus.CORE -> {
                                    if (availableCores.isNotEmpty()) showCorePickerModal = true
                                }
                                GameModalFocus.CLOSE -> onDismiss()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> {
                            onDismiss()
                            true
                        }
                        else -> true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = if (isLandscape) 520.dp else 400.dp)
                    .heightIn(max = if (isLandscape) 300.dp else 460.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.2.dp,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF9D00FF), Color(0xFFFF2A85))
                        ),
                        shape = RoundedCornerShape(22.dp),
                    )
                    .background(Color(0xFF101016))
                    .clickable(enabled = false) {},
            ) {
                // Background Box Art with Dark Scrim
                val effectiveBoxart = remember(game.id, game.boxartUrl) {
                    val p = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                    val local = p.getString("boxart_${game.id}", null)
                    GameBoxartScraper.convertToCdnUrl(local ?: game.boxartUrl)
                }
                if (!effectiveBoxart.isNullOrBlank()) {
                    AsyncImage(
                        model = effectiveBoxart,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.30f },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F0F16).copy(alpha = 0.88f),
                                    Color(0xFF0A0A10).copy(alpha = 0.95f),
                                    Color(0xFF07070C).copy(alpha = 0.98f),
                                )
                            )
                        )
                )

                // Single-window modal content - NO SCROLL
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isLandscape) 14.dp else 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Top Bar: System Pill + Region (Left) & Core Selector + Close (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.18f))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SportsEsports,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = game.systemId.uppercase(),
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp,
                                    )
                                }
                            }
                            if (!game.region.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFF2A85).copy(alpha = 0.16f))
                                        .border(1.dp, Color(0xFFFF2A85).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = game.region.orEmpty().uppercase(),
                                        color = Color(0xFFFF80AB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (availableCores.isNotEmpty()) {
                                val currentCoreOption = availableCores.firstOrNull { it.id.equals(currentCoreId, ignoreCase = true) }
                                val coreDisplayName = currentCoreOption?.name ?: currentCoreId
                                val isCoreFocused = isGamepadConnected && focusedItem == GameModalFocus.CORE
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            if (isCoreFocused) {
                                                scaleX = 1.05f
                                                scaleY = 1.05f
                                            }
                                        }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF9D00FF).copy(alpha = 0.22f))
                                        .border(
                                            width = if (isCoreFocused) 2.dp else 1.dp,
                                            brush = if (isCoreFocused) Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF2A85)))
                                            else Brush.linearGradient(listOf(Color(0xFFB794F4).copy(alpha = 0.6f), Color(0xFF9D00FF).copy(alpha = 0.4f))),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable { showCorePickerModal = true }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tune,
                                            contentDescription = null,
                                            tint = Color(0xFFD6BCFA),
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Text(
                                            text = "Core: $coreDisplayName",
                                            color = Color(0xFFEDE9FE),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDropDown,
                                            contentDescription = "Select Core",
                                            tint = Color(0xFFD6BCFA),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }

                            // Close Button
                            val isCloseFocused = isGamepadConnected && focusedItem == GameModalFocus.CLOSE
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .graphicsLayer {
                                        if (isCloseFocused) {
                                            scaleX = 1.1f
                                            scaleY = 1.1f
                                        }
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (isCloseFocused) 0.25f else 0.10f))
                                    .border(
                                        width = if (isCloseFocused) 2.dp else 1.dp,
                                        color = if (isCloseFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                        shape = CircleShape,
                                    )
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = if (isCloseFocused) Color.White else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Middle Section: Box Art Cartridge Preview & Title/Metadata
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (!effectiveBoxart.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(width = if (isLandscape) 56.dp else 68.dp, height = if (isLandscape) 72.dp else 88.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .background(Color(0xFF08080C)),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model = effectiveBoxart,
                                    contentDescription = game.cleanTitle,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = game.cleanTitle.ifEmpty { game.title },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isLandscape) 16.sp else 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val sizeFormatted = formatFileSize(game.sizeBytes)
                                if (sizeFormatted.isNotEmpty()) {
                                    Text(
                                        text = sizeFormatted,
                                        color = VantafynColors.Muted,
                                        fontSize = 11.5.sp,
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "Cloud Sync",
                                        color = Color(0xFF81C784),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                if (isDownloaded) {
                                    Text(
                                        text = "• Downloaded",
                                        color = Color(0xFF00E676),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action Buttons Row
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Primary Play Button
                        val isPlayFocused = isGamepadConnected && focusedItem == GameModalFocus.PLAY
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 44.dp else 48.dp)
                                .graphicsLayer {
                                    if (isPlayFocused) {
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                    }
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF9D00FF))
                                    )
                                )
                                .border(
                                    width = if (isPlayFocused) 2.5.dp else 0.dp,
                                    color = if (isPlayFocused) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable { onPlay(game.copy(core = currentCoreId)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = if (isPlayFocused) "PLAY GAME  [A]" else "PLAY GAME",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.5.sp,
                                    letterSpacing = 1.sp,
                                )
                            }
                        }

                        // Secondary Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Download Offline / Downloading / Delete Button
                            if (isDownloading) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        VantafynGradientProgressBar(
                                            progress = downloadProgress,
                                            height = 5.dp,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = "${(downloadProgress * 100).toInt()}%",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            } else if (isDownloaded) {
                                val isDownloadFocused = isGamepadConnected && focusedItem == GameModalFocus.DOWNLOAD
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .graphicsLayer { if (isDownloadFocused) { scaleX = 1.04f; scaleY = 1.04f } }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00E676).copy(alpha = 0.12f))
                                        .border(
                                            width = if (isDownloadFocused) 2.dp else 1.dp,
                                            color = if (isDownloadFocused) Color.White else Color(0xFF00E676).copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable { if (onDeleteOffline != null) onDeleteOffline(game) }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DownloadDone,
                                            contentDescription = null,
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            text = "Offline Ready",
                                            color = Color(0xFF00E676),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (onDeleteOffline != null) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFFF5277).copy(alpha = 0.8f),
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }
                            } else if (onDownloadOffline != null) {
                                val isDownloadFocused = isGamepadConnected && focusedItem == GameModalFocus.DOWNLOAD
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .graphicsLayer { if (isDownloadFocused) { scaleX = 1.04f; scaleY = 1.04f } }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(
                                            width = if (isDownloadFocused) 2.dp else 1.dp,
                                            color = if (isDownloadFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable { onDownloadOffline(game) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "Download Offline",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }

                            // Import Battery Save Button
                            if (storageManager != null) {
                                val isImportFocused = isGamepadConnected && focusedItem == GameModalFocus.IMPORT
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .graphicsLayer { if (isImportFocused) { scaleX = 1.04f; scaleY = 1.04f } }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(
                                            width = if (isImportFocused) 2.dp else 1.dp,
                                            color = if (isImportFocused) Color(0xFFB8A4FF) else Color(0xFFB8A4FF).copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.UploadFile,
                                            contentDescription = null,
                                            tint = Color(0xFFB8A4FF),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "Import Save",
                                            color = Color(0xFFDED3FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }

                            // Wireless Link Button
                            if (isLinkCapable) {
                                val isLinkFocused = isGamepadConnected && focusedItem == GameModalFocus.LINK
                                Box(
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(36.dp)
                                        .graphicsLayer { if (isLinkFocused) { scaleX = 1.04f; scaleY = 1.04f } }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                                        .border(
                                            width = if (isLinkFocused) 2.dp else 1.dp,
                                            color = if (isLinkFocused) Color(0xFF00E5FF) else Color(0xFF00E5FF).copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable { showLinkChoiceModal = true },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Cable,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "Link Play",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = { if (!isImporting) pendingImport = null },
            title = { Text("Import battery save") },
            text = {
                Text(
                    "Import ${pending.fileName} for ${game?.cleanTitle?.ifEmpty { game.title }}? " +
                        "Your current local save and any cloud save will be backed up first. " +
                        "Only continue if this is for the exact same game and region.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isImporting,
                    onClick = {
                        val detail = game
                        val manager = storageManager
                        if (detail == null || manager == null) return@TextButton
                        isImporting = true
                        scope.launch(Dispatchers.IO) {
                            val result = manager.importBatterySave(session, detail.id, pending.bytes)
                            withContext(Dispatchers.Main) {
                                isImporting = false
                                pendingImport = null
                                importMessage = result.fold(
                                    onSuccess = { imported ->
                                        if (session == null) "Save imported on this device. It will sync when you next play while connected."
                                        else if (imported.cloudSynced) "Save imported, backed up, and synced. You can now launch the game."
                                        else "Save imported and backed up locally. Cloud sync will retry automatically when available."
                                    },
                                    onFailure = { it.message ?: "Could not import that battery save." },
                                )
                            }
                        }
                    },
                ) { Text(if (isImporting) "Importing…" else "Import") }
            },
            dismissButton = {
                TextButton(enabled = !isImporting, onClick = { pendingImport = null }) { Text("Cancel") }
            },
        )
    }

    importMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { importMessage = null },
            title = { Text("Battery save") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { importMessage = null }) { Text("OK") } },
        )
    }

    if (showCorePickerModal && game != null && availableCores.isNotEmpty()) {
        CoreChoiceModal(
            systemId = game.systemId,
            currentCoreId = currentCoreId,
            availableCores = availableCores,
            onSelectCore = { chosenId ->
                currentCoreId = chosenId
                prefs.edit().putString("selected_core_${game.id}", chosenId).apply()
                showCorePickerModal = false
            },
            onDismiss = { showCorePickerModal = false },
        )
    }

    if (showLinkChoiceModal && game != null) {
        LinkChoiceModal(
            game = game,
            currentCoreId = currentCoreId,
            onDismiss = { showLinkChoiceModal = false },
            onLaunchWithLink = {
                showLinkChoiceModal = false
                onPlay(game.copy(core = currentCoreId))
            },
        )
    }
}

@Composable
private fun CoreChoiceModal(
    systemId: String,
    currentCoreId: String,
    availableCores: List<SystemCoreOption>,
    onSelectCore: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF24243A),
                            Color(0xFF171B2B),
                            Color(0xFF0D101B),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(VantafynColors.Primary, VantafynColors.Secondary),
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2200E5FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = systemId.uppercase(),
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "Emulation Core",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose core engine for this game",
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                availableCores.forEach { core ->
                    val isSelected = core.id.equals(currentCoreId, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) Color(0x2800FFB2) else Color(0x0EFFFFFF),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF00FFB2) else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelectCore(core.id) }
                            .padding(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = core.name,
                                        color = if (isSelected) Color(0xFF00FFB2) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    )
                                    if (core.isRecommended) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x3300FFB2))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = "RECOMMENDED",
                                                color = Color(0xFF00FFB2),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = core.description,
                                    color = VantafynColors.Muted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF00FFB2),
                                    modifier = Modifier.size(20.dp),
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
private fun LinkChoiceModal(
    game: GameDetail,
    currentCoreId: String,
    onDismiss: () -> Unit,
    onLaunchWithLink: () -> Unit,
) {
    val context = LocalContext.current
    val linkManager = remember { LinkSessionManager.getInstance(context) }
    var selectedMode by remember { mutableStateOf(linkManager.transportMode) }
    var roomCode by remember { mutableStateOf(linkManager.serverRoomCode) }
    val discoveredPeers by linkManager.discoveredPeers.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(game.id) {
        if (linkManager.autoDiscoveryEnabled) {
            linkManager.startDiscovery(filterGameId = game.id)
        }
        onDispose {
            linkManager.stopDiscovery()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(max = if (isLandscape) 460.dp else 400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF131317))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cable,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Multiplayer Link Session",
                                color = VantafynColors.Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = game.title,
                                color = VantafynColors.Muted,
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Mode Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        LinkTransportMode.LOCAL_WIFI to "Local Wi-Fi",
                        LinkTransportMode.SERVER_RELAY to "Server Relay",
                    ).forEach { (mode, label) ->
                        val selected = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) VantafynGradients.accentHorizontal()
                                    else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                                )
                                .clickable {
                                    selectedMode = mode
                                    linkManager.transportMode = mode
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else VantafynColors.Muted,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                if (selectedMode == LinkTransportMode.LOCAL_WIFI) {
                    Text(
                        text = "Connect with another device in the same room over Wi-Fi or Wi-Fi Direct (<2ms latency).",
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )

                    // Host Room button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VantafynGradients.accentHorizontal())
                            .clickable {
                                linkManager.startHosting(
                                    gameId = game.id,
                                    gameTitle = game.title,
                                    core = currentCoreId,
                                )
                                onLaunchWithLink()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Host Local Room & Play",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Discovered peers
                    if (discoveredPeers.isNotEmpty()) {
                        Text(
                            text = "Nearby Handhelds Found (${discoveredPeers.size}):",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        discoveredPeers.forEach { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00E5FF))
                                        .clickable {
                                            linkManager.connectToPeer(peer)
                                            onLaunchWithLink()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "Join & Play",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Server Relay mode
                    Text(
                        text = "Play over the internet with friends on your Vantafyn server. Both players enter the same 4-digit Room PIN.",
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )

                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { if (it.length <= 6) roomCode = it },
                        label = { Text("Room PIN (e.g. 1234)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = VantafynColors.Ink,
                            unfocusedTextColor = VantafynColors.Ink,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VantafynGradients.accentHorizontal())
                                .clickable {
                                    linkManager.startServerRelaySession(
                                        gameId = game.id,
                                        gameTitle = game.title,
                                        core = currentCoreId,
                                        roomCode = roomCode.ifBlank { "1234" },
                                        isHost = true,
                                    )
                                    onLaunchWithLink()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Host Online Room",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable {
                                    linkManager.startServerRelaySession(
                                        gameId = game.id,
                                        gameTitle = game.title,
                                        core = currentCoreId,
                                        roomCode = roomCode.ifBlank { "1234" },
                                        isHost = false,
                                    )
                                    onLaunchWithLink()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Join Online Room",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun displayNameForUri(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
    }
}.getOrNull()

private fun readBatterySaveUri(context: Context, uri: Uri): ByteArray {
    val maximumBytes = 32 * 1024 * 1024
    return context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count <= 0) break
            if (output.size() + count > maximumBytes) {
                throw IllegalArgumentException("That battery save exceeds the 32 MB import limit.")
            }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: throw IllegalArgumentException("Android could not open that file.")
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / (1024f * 1024f)
    return if (mb < 1f) {
        "${(bytes / 1024f).toInt()} KB"
    } else {
        "%.1f MB".format(mb)
    }
}

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.UploadFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import dev.vantafyn.core.ui.VantafynGradientProgressBar
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
    val scope = rememberCoroutineScope()
    val storageManager = remember(gamesRepository) { gamesRepository?.let { GameStorageManager(context, it) } }
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
                .background(Color(0xDD0A0A0C))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = if (isLandscape) 480.dp else 420.dp)
                    .heightIn(max = if (isLandscape) 330.dp else 700.dp)
                    .clip(RoundedCornerShape(if (isLandscape) 20.dp else 28.dp))
                    .background(Color(0xFF131317))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(VantafynColors.Primary, VantafynColors.Secondary)
                        ),
                        shape = RoundedCornerShape(if (isLandscape) 20.dp else 28.dp),
                    )
                    .clickable(enabled = false) {}
                    .padding(if (isLandscape) 14.dp else 24.dp),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 18.dp),
                ) {
                    // Top Bar with Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2221D8FF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                VantafynGradientIcon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                text = game.systemId.uppercase(),
                                style = TextStyle(
                                    brush = VantafynGradients.accentHorizontal(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp,
                                ),
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = VantafynColors.Muted,
                            )
                        }
                    }

                    val context = LocalContext.current
                    val effectiveBoxart = remember(game.id, game.boxartUrl) {
                        val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                        val local = prefs.getString("boxart_${game.id}", null)
                        GameBoxartScraper.convertToCdnUrl(local ?: game.boxartUrl)
                    }

                    if (!effectiveBoxart.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 80.dp else 160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0D0F18)),
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

                    // Game Title & Tags
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = game.cleanTitle.ifEmpty { game.title },
                            color = VantafynColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isLandscape) 17.sp else 22.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (game.region != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x22E026FF))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = game.region.orEmpty(),
                                        color = VantafynColors.Secondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                    )
                                }
                            }

                            val sizeFormatted = formatFileSize(game.sizeBytes)
                            if (sizeFormatted.isNotEmpty()) {
                                Text(
                                    text = sizeFormatted,
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }

                            if (game.core.isNotEmpty()) {
                                Text(
                                    text = "• Core: ${game.core}",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    // Cloud Save State Sync Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF00FFB2),
                                modifier = Modifier.size(20.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cross-Device Cloud Saves",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = "Saves automatically sync with your Jellyfin server",
                                    color = VantafynColors.Muted,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }

                    // Offline Download Action
                    if (isDownloading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Downloading ROM for Offline...",
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                    )
                                }
                                VantafynGradientProgressBar(
                                    progress = downloadProgress,
                                    height = 7.dp,
                                )
                            }
                        }
                    } else if (isDownloaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF00E676).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadDone,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Downloaded for Offline Play",
                                        color = Color(0xFF00E676),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                    )
                                    Text(
                                        text = "Playable anywhere without network",
                                        color = VantafynColors.Muted,
                                        fontSize = 11.sp,
                                    )
                                }
                                if (onDeleteOffline != null) {
                                    IconButton(
                                        onClick = { onDeleteOffline(game) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete downloaded ROM",
                                            tint = Color(0xFFFF5277).copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    } else if (onDownloadOffline != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                .clickable { onDownloadOffline(game) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Download for Offline Play",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Import happens before launch so EmulatorJS never runs while its battery save is replaced.
                    if (storageManager != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 40.dp else 46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color(0xFFB8A4FF).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .clickable { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.UploadFile,
                                    contentDescription = null,
                                    tint = Color(0xFFB8A4FF),
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "IMPORT BATTERY SAVE",
                                    color = Color(0xFFDED3FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.6.sp,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Play Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) 44.dp else 52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(VantafynGradients.accentHorizontal())
                            .clickable { onPlay(game) },
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
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = "PLAY GAME",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp,
                            )
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

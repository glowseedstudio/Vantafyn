package dev.vantafyn.feature.home.games

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GameSaveKind
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.player.games.GameStorageManager
import dev.vantafyn.feature.player.games.SaveConflictDialog
import dev.vantafyn.feature.player.games.SaveSyncInfo
import dev.vantafyn.feature.player.games.SaveSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ManagedSaveItem(
    val file: File,
    val gameId: String,
    val kind: GameSaveKind,
    val gameTitle: String,
    val systemDisplayName: String,
    val boxartUrl: String?,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val syncStatus: SaveSyncStatus = SaveSyncStatus.IN_SYNC,
    val syncInfo: SaveSyncInfo? = null,
)

@Composable
fun GamesSavesScreen(
    serverName: String,
    onBack: () -> Unit,
    games: List<GameSummary> = emptyList(),
    systems: List<GameSystem> = emptyList(),
    session: JellyfinSession? = null,
    gamesRepository: JellyfinGamesRepository? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageManager = remember(gamesRepository) {
        gamesRepository?.let { GameStorageManager(context, it) }
    }

    var savesList by remember { mutableStateOf<List<ManagedSaveItem>>(emptyList()) }
    var isLoadingSaves by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSyncingAll by remember { mutableStateOf(false) }
    var syncingSaveFile by remember { mutableStateOf<String?>(null) }
    var savePendingDelete by remember { mutableStateOf<ManagedSaveItem?>(null) }
    var activeConflictItem by remember { mutableStateOf<ManagedSaveItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedKindFilter by remember { mutableStateOf<GameSaveKind?>(null) }

    fun loadSaves() {
        isLoadingSaves = true
        coroutineScope.launch(Dispatchers.IO) {
            val savesDir = File(context.filesDir, "games/saves")
            val items = mutableListOf<ManagedSaveItem>()
            if (savesDir.exists() && savesDir.isDirectory) {
                val files = savesDir.listFiles().orEmpty().filter {
                    it.isFile && (it.extension.equals("sram", ignoreCase = true) ||
                            it.extension.equals("srm", ignoreCase = true) ||
                            it.extension.equals("sav", ignoreCase = true) ||
                            it.extension.equals("main", ignoreCase = true) ||
                            it.name.equals("main", ignoreCase = true) ||
                            it.extension.equals("bin", ignoreCase = true))
                }
                for (file in files) {
                    val rawName = file.nameWithoutExtension
                    val kind = GameSaveKind.Sram

                    val matched = games.firstOrNull { g ->
                        val safeId = g.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        val safeToken = g.token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        val cleanGameTitle = g.cleanTitle.ifEmpty { g.title }.replace(Regex("[^a-zA-Z0-9._ -]"), "_").trim()
                        val filenameStem = g.filename.substringBeforeLast('.')
                        safeId.equals(rawName, ignoreCase = true) ||
                                g.id.equals(rawName, ignoreCase = true) ||
                                (g.token.isNotBlank() && g.token.equals(rawName, ignoreCase = true)) ||
                                (safeToken.isNotBlank() && safeToken.equals(rawName, ignoreCase = true)) ||
                                filenameStem.equals(rawName, ignoreCase = true) ||
                                cleanGameTitle.equals(rawName, ignoreCase = true) ||
                                g.title.equals(rawName, ignoreCase = true) ||
                                g.cleanTitle.equals(rawName, ignoreCase = true)
                    }

                    val title = matched?.cleanTitle?.ifEmpty { matched.title }
                        ?: rawName.replace('_', ' ').trim()
                    val sysName = if (matched != null) {
                        systems.firstOrNull { it.id == matched.systemId }?.displayName ?: matched.systemId
                    } else {
                        "Retro Game"
                    }

                    val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                    val localScrapedBoxart = matched?.let { prefs.getString("boxart_${it.id}", null) }
                        ?: prefs.getString("boxart_$rawName", null)
                    val resolvedBoxart = GameBoxartScraper.convertToCdnUrl(localScrapedBoxart ?: matched?.boxartUrl)

                    items.add(
                        ManagedSaveItem(
                            file = file,
                            gameId = matched?.id ?: rawName,
                            kind = kind,
                            gameTitle = title,
                            systemDisplayName = sysName,
                            boxartUrl = resolvedBoxart,
                            sizeBytes = file.length(),
                            lastModifiedMs = file.lastModified(),
                        )
                    )
                }
            }
            items.sortByDescending { it.lastModifiedMs }
            withContext(Dispatchers.Main) {
                savesList = items
                isLoadingSaves = false
            }

            // In background, query cloud sync status for each save
            if (session != null && storageManager != null && items.isNotEmpty()) {
                val updated = items.map { item ->
                    val info = storageManager.checkSaveSync(session, item.gameId, item.kind)
                    item.copy(syncStatus = info.status, syncInfo = info)
                }
                withContext(Dispatchers.Main) {
                    savesList = updated
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSaves()
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(4000)
            statusMessage = null
        }
    }

    val totalBattery = remember(savesList) { savesList.count { it.kind == GameSaveKind.Sram } }
    val totalStorageBytes = remember(savesList) { savesList.sumOf { it.sizeBytes } }

    val filteredSaves = remember(savesList, searchQuery, selectedKindFilter) {
        savesList.filter { save ->
            val matchesKind = selectedKindFilter == null || save.kind == selectedKindFilter
            val matchesQuery = searchQuery.isBlank() ||
                    save.gameTitle.contains(searchQuery, ignoreCase = true) ||
                    save.systemDisplayName.contains(searchQuery, ignoreCase = true) ||
                    save.file.name.contains(searchQuery, ignoreCase = true)
            matchesKind && matchesQuery
        }
    }

    GameScreenReveal(
        key = "games_saves_screen",
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(top = 12.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompactBackButton(onClick = onBack)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cloud Saves",
                        color = VantafynColors.Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Manage battery saves & snapshot states",
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                    )
                }

                // Refresh button
                IconButton(
                    onClick = { loadSaves() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh saves",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Sync All button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSyncingAll) Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.3f), Color(0xFF9D00FF).copy(alpha = 0.3f)))
                            else VantafynGradients.accentHorizontal()
                        )
                        .clickable(enabled = !isSyncingAll && savesList.isNotEmpty()) {
                            if (session == null || gamesRepository == null) {
                                statusMessage = "Not connected to Jellyfin server"
                                return@clickable
                            }
                            isSyncingAll = true
                            coroutineScope.launch(Dispatchers.IO) {
                                var synced = 0
                                var failed = 0
                                for (save in savesList) {
                                    try {
                                        val res = if (storageManager != null) {
                                            storageManager.replaceCloudWithLocalSave(session, save.gameId, save.kind)
                                        } else {
                                            val data = save.file.readBytes()
                                            gamesRepository.uploadCloudSave(session, save.gameId, save.kind, data)
                                        }
                                        if (res.isSuccess) synced++ else failed++
                                    } catch (_: Exception) {
                                        failed++
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    isSyncingAll = false
                                    statusMessage = if (failed == 0) {
                                        "All $synced saves synced to $serverName!"
                                    } else {
                                        "Synced $synced saves ($failed failed)"
                                    }
                                    loadSaves()
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isSyncingAll) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = "Syncing...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Sync All",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // 2. Status message banner
        if (statusMessage != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = statusMessage.orEmpty(),
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF00E5FF).copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { statusMessage = null },
                    )
                }
            }
        }

        // 3. Overview Cloud & Storage Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0F2B26).copy(alpha = 0.85f),
                                Color(0xFF101935).copy(alpha = 0.85f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF00E676).copy(alpha = 0.6f),
                                Color(0xFF00E5FF).copy(alpha = 0.6f),
                            ),
                        ),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Connection Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud Sync Active",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Connected to $serverName • Instant multi-device sync",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                        )
                    }
                }

                // Storage and Counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SaveStatBox(
                        title = "BATTERY SAVES",
                        value = "$totalBattery",
                        accentColor = Color(0xFF00E676),
                        modifier = Modifier.weight(1f),
                    )
                    SaveStatBox(
                        title = "STORAGE USED",
                        value = formatSaveSize(totalStorageBytes),
                        accentColor = Color(0xFFFF2A85),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 4. Search and Filter Chips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VantafynTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search save files...",
                    placeholder = "Filter by game title or system...",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = VantafynColors.Muted,
                                )
                            }
                        }
                    } else null,
                )

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        SaveFilterPill(
                            label = "All Saves",
                            count = savesList.size,
                            isSelected = selectedKindFilter == null,
                            onClick = { selectedKindFilter = null },
                        )
                    }
                    item {
                        SaveFilterPill(
                            label = "Battery SRAM",
                            count = totalBattery,
                            isSelected = selectedKindFilter == GameSaveKind.Sram,
                            onClick = { selectedKindFilter = GameSaveKind.Sram },
                        )
                    }
                }
            }
        }

        // 5. Saves List or Empty State
        if (isLoadingSaves) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        } else if (filteredSaves.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null,
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(52.dp),
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No saves found matching \"$searchQuery\"" else "No Game Saves Found",
                            color = VantafynColors.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try searching for a different game or clearing filters."
                            else "When you play games in the Retro Vault, battery saves will automatically appear here and sync to $serverName.",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            items(filteredSaves, key = { it.file.absolutePath }) { save ->
                SaveCardItem(
                    save = save,
                    isSyncing = syncingSaveFile == save.file.name,
                    onSync = {
                        if (session == null || gamesRepository == null) {
                            statusMessage = "Not connected to Jellyfin server"
                            return@SaveCardItem
                        }
                        syncingSaveFile = save.file.name
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val res = if (storageManager != null) {
                                    storageManager.replaceCloudWithLocalSave(session, save.gameId, save.kind)
                                } else {
                                    val bytes = save.file.readBytes()
                                    gamesRepository.uploadCloudSave(session, save.gameId, save.kind, bytes)
                                }
                                withContext(Dispatchers.Main) {
                                    if (res.isSuccess) {
                                        statusMessage = "Replaced cloud save for \"${save.gameTitle}\" with local version!"
                                        loadSaves()
                                    } else {
                                        statusMessage = "Sync error: ${res.exceptionOrNull()?.message ?: "Failed"}"
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    statusMessage = "Sync failed: ${e.message}"
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    syncingSaveFile = null
                                }
                            }
                        }
                    },
                    onDownloadCloud = {
                        if (session == null || storageManager == null) {
                            statusMessage = "Not connected to Jellyfin server"
                            return@SaveCardItem
                        }
                        syncingSaveFile = save.file.name
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val res = storageManager.replaceLocalWithCloudSave(session, save.gameId, save.kind, save.syncInfo?.cachedCloudData)
                                withContext(Dispatchers.Main) {
                                    if (res.isSuccess) {
                                        statusMessage = "Downloaded cloud save for \"${save.gameTitle}\"!"
                                        loadSaves()
                                    } else {
                                        statusMessage = "Download error: ${res.exceptionOrNull()?.message ?: "Failed"}"
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    statusMessage = "Download failed: ${e.message}"
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    syncingSaveFile = null
                                }
                            }
                        }
                    },
                    onResolveConflict = {
                        activeConflictItem = save
                    },
                    onExport = {
                        exportSave(context, save.file)
                    },
                    onDelete = {
                        savePendingDelete = save
                    },
                )
            }
        }
    }
    }

    // Delete Confirmation Dialog
    if (savePendingDelete != null) {
        val target = savePendingDelete!!
        AlertDialog(
            onDismissRequest = { savePendingDelete = null },
            title = {
                Text(
                    text = "Delete Save?",
                    color = VantafynColors.Ink,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this battery save for \"${target.gameTitle}\"?\n\nThis will permanently delete the local file and remove it from your Jellyfin cloud storage.",
                    color = VantafynColors.Muted,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (target.file.exists()) {
                                target.file.delete()
                            }
                            if (session != null && gamesRepository != null) {
                                gamesRepository.deleteCloudSave(session, target.gameId, target.kind)
                            }
                            withContext(Dispatchers.Main) {
                                savePendingDelete = null
                                loadSaves()
                                statusMessage = "Deleted save for \"${target.gameTitle}\""
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5277), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { savePendingDelete = null }) {
                    Text("Cancel", color = VantafynColors.Muted)
                }
            },
            containerColor = Color(0xFF1E1E28),
        )
    }

    if (activeConflictItem != null && activeConflictItem!!.syncInfo != null) {
        val conflictItem = activeConflictItem!!
        SaveConflictDialog(
            gameTitle = conflictItem.gameTitle,
            conflict = conflictItem.syncInfo!!,
            onUseCloud = {
                if (session != null && storageManager != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        storageManager.replaceLocalWithCloudSave(session, conflictItem.gameId, conflictItem.kind, conflictItem.syncInfo?.cachedCloudData)
                        withContext(Dispatchers.Main) {
                            activeConflictItem = null
                            statusMessage = "Updated \"${conflictItem.gameTitle}\" with cloud save."
                            loadSaves()
                        }
                    }
                }
            },
            onUseLocal = {
                if (session != null && storageManager != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        storageManager.replaceCloudWithLocalSave(session, conflictItem.gameId, conflictItem.kind)
                        withContext(Dispatchers.Main) {
                            activeConflictItem = null
                            statusMessage = "Replaced cloud save with local save for \"${conflictItem.gameTitle}\"."
                            loadSaves()
                        }
                    }
                }
            },
            onDismiss = {
                activeConflictItem = null
            },
        )
    }
}

@Composable
private fun SaveStatBox(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SaveFilterPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = "$label ($count)",
            color = if (isSelected) Color(0xFF00E5FF) else VantafynColors.Muted,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SaveCardItem(
    save: ManagedSaveItem,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onDownloadCloud: () -> Unit,
    onResolveConflict: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val badgeColor = Color(0xFF00E676)
    val badgeText = "Battery SRAM"
    val badgeIcon = Icons.Rounded.Save

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                when (save.syncStatus) {
                    SaveSyncStatus.LOCAL_NEWER -> Color(0xFF00E676).copy(alpha = 0.35f)
                    SaveSyncStatus.CLOUD_NEWER -> Color(0xFF00E5FF).copy(alpha = 0.35f)
                    SaveSyncStatus.CONFLICT -> Color(0xFFFFB300).copy(alpha = 0.4f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
                RoundedCornerShape(16.dp),
            )
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top Row: Boxart + Game Title & System + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Boxart or Fallback Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E28))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!save.boxartUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = save.boxartUrl,
                            contentDescription = save.gameTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Title and System
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = save.gameTitle,
                        color = VantafynColors.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = save.systemDisplayName,
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Compact Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Export
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Export save",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete save",
                            tint = Color(0xFFFF5277).copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            // Middle Row: Metadata Tags (Badge, Status Badge, Size, Date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Type Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(0.8.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }

                // Status Badge
                when (save.syncStatus) {
                    SaveSyncStatus.LOCAL_NEWER -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E676).copy(alpha = 0.18f))
                                .border(0.8.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Offline Progress",
                                color = Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    SaveSyncStatus.CLOUD_NEWER -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.18f))
                                .border(0.8.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Cloud is Newer",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    SaveSyncStatus.CONFLICT -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFB300).copy(alpha = 0.18f))
                                .border(0.8.dp, Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Conflict",
                                color = Color(0xFFFFB300),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    SaveSyncStatus.IN_SYNC -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF00E676).copy(alpha = 0.8f),
                                modifier = Modifier.size(10.dp),
                            )
                            Text(
                                text = "Synced",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                            )
                        }
                    }
                    SaveSyncStatus.LOCAL_ONLY -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Local Only",
                                color = VantafynColors.Muted,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.weight(1f))

                // Size
                Text(
                    text = formatSaveSize(save.sizeBytes),
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = "•",
                    color = VantafynColors.Muted.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                )

                // Date
                Text(
                    text = formatSaveDate(save.lastModifiedMs),
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                )
            }

            // Bottom Action Button: Replace Cloud / Download Cloud / Resolve Conflict
            if (save.syncStatus == SaveSyncStatus.LOCAL_NEWER || save.syncStatus == SaveSyncStatus.LOCAL_ONLY) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00E676).copy(alpha = 0.22f), Color(0xFF00E5FF).copy(alpha = 0.22f))
                            )
                        )
                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable(enabled = !isSyncing, onClick = onSync)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = if (save.syncStatus == SaveSyncStatus.LOCAL_NEWER) "Replace Cloud Save (Keep Offline Progress)" else "Upload Save to Cloud",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else if (save.syncStatus == SaveSyncStatus.CLOUD_NEWER) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable(enabled = !isSyncing, onClick = onDownloadCloud)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Download Newer Cloud Save",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else if (save.syncStatus == SaveSyncStatus.CONFLICT) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable(enabled = !isSyncing, onClick = onResolveConflict)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Resolve Save Conflict",
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun exportSave(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Game Save: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Export Game Save (${file.name})").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.util.Log.e("GamesSavesScreen", "Failed to export save", e)
    }
}

private fun formatSaveSize(bytes: Long): String = when {
    bytes <= 0L -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
    else -> String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f))
}

private fun formatSaveDate(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Unknown"
    val sdf = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}

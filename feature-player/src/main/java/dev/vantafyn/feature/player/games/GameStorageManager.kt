package dev.vantafyn.feature.player.games

import android.content.Context
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GameSaveKind
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.CloudSaveEntry
import dev.vantafyn.core.jellyfin.mediaBrowserAuthHeader
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

class GameStorageManager(
    private val context: Context,
    private val gamesRepository: JellyfinGamesRepository,
) {
    companion object {
        val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private const val MAX_BATTERY_SAVE_BYTES = 32 * 1024 * 1024
    }

    val persistentRomsDir: File
        get() = File(context.filesDir, "games/roms").apply { mkdirs() }

    val cacheRomsDir: File
        get() = File(context.cacheDir, "games/roms").apply { mkdirs() }

    val emulatorCacheDir: File
        get() = File(context.filesDir, "games/emulator_cache").apply { mkdirs() }

    val savesDir: File
        get() = File(context.filesDir, "games/saves").apply { mkdirs() }

    val savesBackupDir: File
        get() = File(context.filesDir, "games/saves/backups").apply { mkdirs() }

    private val syncPrefs by lazy {
        context.getSharedPreferences("vantafyn_save_sync", Context.MODE_PRIVATE)
    }

    fun getCachedRomFile(token: String, extension: String): File {
        val safeToken = token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val ext = if (extension.startsWith(".")) extension else ".$extension"
        val persistent = File(persistentRomsDir, "$safeToken$ext")
        if (persistent.exists() && persistent.length() > 0) return persistent
        return File(cacheRomsDir, "$safeToken$ext")
    }

    fun isGameDownloaded(gameId: String, token: String = "", extension: String = ""): Boolean {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeToken = token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val ext = if (extension.isNotBlank()) (if (extension.startsWith(".")) extension else ".$extension") else ""

        if (ext.isNotBlank()) {
            if (File(persistentRomsDir, "$safeId$ext").let { it.exists() && it.length() > 0 }) return true
            if (safeToken.isNotBlank() && File(persistentRomsDir, "$safeToken$ext").let { it.exists() && it.length() > 0 }) return true
        }

        val files = persistentRomsDir.listFiles().orEmpty()
        return files.any { f ->
            f.length() > 0 && (
                f.nameWithoutExtension.equals(safeId, ignoreCase = true) ||
                (safeToken.isNotBlank() && f.nameWithoutExtension.equals(safeToken, ignoreCase = true))
            )
        }
    }

    fun deleteOfflineGame(gameId: String, token: String = "", extension: String = ""): Boolean {
        var deleted = false
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeToken = token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        for (dir in listOf(persistentRomsDir, cacheRomsDir)) {
            dir.listFiles().orEmpty().forEach { f ->
                if (f.nameWithoutExtension.equals(safeId, ignoreCase = true) ||
                    (safeToken.isNotBlank() && f.nameWithoutExtension.equals(safeToken, ignoreCase = true))) {
                    if (f.delete()) deleted = true
                }
            }
        }
        return deleted
    }

    fun listDownloadedGameKeys(): Set<String> {
        return persistentRomsDir.listFiles().orEmpty()
            .filter { it.isFile && it.length() > 0 }
            .map { it.nameWithoutExtension }
            .toSet()
    }

    fun resolveCoreDataFileName(systemId: String, core: String): String {
        val key = core.ifBlank { systemId }.lowercase().trim()
        return when {
            key == "gba" || key.contains("advance") -> "mgba-wasm.data"
            key == "gb" || key == "gbc" || key.contains("color") -> "gambatte-wasm.data"
            key == "snes" || key.contains("super nintendo") -> "snes9x-wasm.data"
            key == "nes" || key.contains("famicom") -> "nestopia-wasm.data"
            key == "segamd" || key.contains("genesis") || key.contains("sega") || key.contains("megadrive") || key.contains("mastersystem") || key.contains("gamegear") -> "genesis_plus_gx-wasm.data"
            key == "n64" || key.contains("nintendo 64") -> "mupen64plus_next-wasm.data"
            key.contains("desmume") -> "desmume-wasm.data"
            key == "nds" || key == "ds" || key.contains("nintendo ds") || key.contains("melonds") -> "melonds-wasm.data"
            key == "psx" || key == "ps1" || key.contains("playstation") -> "pcsx_rearmed-wasm.data"
            key == "arcade" || key == "mame" || key == "fbneo" -> "fbneo-wasm.data"
            else -> "${key}-wasm.data"
        }
    }

    suspend fun preCacheEmulatorCore(systemId: String, core: String) = withContext(Dispatchers.IO) {
        val baseCdn = "https://cdn.emulatorjs.org/stable/data/"
        val coreFile = resolveCoreDataFileName(systemId, core)
        val filesToCache = listOf(
            "loader.js" to File(emulatorCacheDir, "loader.js"),
            "emulator.min.js" to File(emulatorCacheDir, "emulator.min.js"),
            "emulator.min.css" to File(emulatorCacheDir, "emulator.min.css"),
            "cores/$coreFile" to File(emulatorCacheDir, "cores/$coreFile"),
            "compression/extract7z.js" to File(emulatorCacheDir, "compression/extract7z.js"),
            "compression/extract7z-wasm.data" to File(emulatorCacheDir, "compression/extract7z-wasm.data"),
            "compression/extract7z-wasm.wasm" to File(emulatorCacheDir, "compression/extract7z-wasm.wasm"),
        )
        for ((relPath, target) in filesToCache) {
            if (target.exists() && target.length() > 0) continue
            try {
                val url = "$baseCdn$relPath"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                }
                if (conn.responseCode in 200..299) {
                    target.parentFile?.mkdirs()
                    val temp = File(target.parentFile, "${target.name}.tmp")
                    conn.inputStream.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    temp.renameTo(target)
                }
            } catch (e: Exception) {
                android.util.Log.w("GameStorageManager", "Could not pre-cache emulator asset $relPath: ${e.message}")
            }
        }
    }

    fun getLocalSaveFile(gameId: String, kind: GameSaveKind): File {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(savesDir, "$safeId.${kind.value}")
    }

    suspend fun downloadRomIfNeeded(
        session: JellyfinSession?,
        libraryId: String,
        game: GameDetail,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val token = game.token.ifEmpty { game.id }
        val ext = game.extension.ifEmpty { game.filename.substringAfterLast('.', "") }
        val safeToken = token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileExt = if (ext.startsWith(".")) ext else ".$ext"

        // Check persistent first
        val persistentTarget = File(persistentRomsDir, "$safeToken$fileExt")
        if (persistentTarget.exists() && persistentTarget.length() > 0) {
            onProgress(1f)
            return@withContext persistentTarget
        }

        // Check cache dir
        val cacheTarget = File(cacheRomsDir, "$safeToken$fileExt")
        if (cacheTarget.exists() && cacheTarget.length() > 0) {
            onProgress(1f)
            return@withContext cacheTarget
        }

        downloadRomInternal(session, libraryId, game, cacheTarget, onProgress)
    }

    suspend fun downloadRomForOffline(
        session: JellyfinSession?,
        libraryId: String,
        game: GameDetail,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val token = game.token.ifEmpty { game.id }
        val ext = game.extension.ifEmpty { game.filename.substringAfterLast('.', "") }
        val safeToken = token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileExt = if (ext.startsWith(".")) ext else ".$ext"
        val persistentTarget = File(persistentRomsDir, "$safeToken$fileExt")

        if (persistentTarget.exists() && persistentTarget.length() > 0) {
            preCacheEmulatorCore(game.systemId, game.core)
            onProgress(1f)
            return@withContext persistentTarget
        }

        // If it's already in cacheDir, promote/copy it to persistent!
        val cacheTarget = File(cacheRomsDir, "$safeToken$fileExt")
        if (cacheTarget.exists() && cacheTarget.length() > 0) {
            cacheTarget.copyTo(persistentTarget, overwrite = true)
            preCacheEmulatorCore(game.systemId, game.core)
            onProgress(1f)
            return@withContext persistentTarget
        }

        val result = downloadRomInternal(session, libraryId, game, persistentTarget, onProgress)
        preCacheEmulatorCore(game.systemId, game.core)
        result
    }

    private suspend fun downloadRomInternal(
        session: JellyfinSession?,
        libraryId: String,
        game: GameDetail,
        target: File,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        if (session == null) {
            throw IllegalStateException("Cannot download game without an active session.")
        }
        val token = game.token.ifEmpty { game.id }
        val tempFile = File(target.parentFile, "${target.name}.tmp")
        if (tempFile.exists()) tempFile.delete()

        val downloadUrl = if (game.downloadUrl.isNotBlank() && !game.downloadUrl.contains("/ROM/?")) {
            game.downloadUrl
        } else {
            gamesRepository.getRomDownloadUrl(session, libraryId, token)
        }

        var currentUrl = downloadUrl
        var conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 90_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "*/*")
            val authHeader = session.mediaBrowserAuthHeader()
            setRequestProperty("Authorization", authHeader)
            setRequestProperty("X-Emby-Token", session.accessToken)
            setRequestProperty("X-MediaBrowser-Token", session.accessToken)
            setRequestProperty("X-Emby-Authorization", authHeader)
        }

        var responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
            responseCode == 307 || responseCode == 308) {
            val redirectUrl = conn.getHeaderField("Location")
            if (!redirectUrl.isNullOrBlank()) {
                conn.disconnect()
                currentUrl = redirectUrl
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 90_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "*/*")
                    val authHeader = session.mediaBrowserAuthHeader()
                    setRequestProperty("Authorization", authHeader)
                    setRequestProperty("X-Emby-Token", session.accessToken)
                    setRequestProperty("X-MediaBrowser-Token", session.accessToken)
                    setRequestProperty("X-Emby-Authorization", authHeader)
                }
                responseCode = conn.responseCode
            }
        }

        if (responseCode !in 200..299) {
            val safeUrl = currentUrl.substringBefore('?')
            val rawError = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            val sanitizedError = if (session.accessToken.isNotBlank()) {
                rawError.replace(session.accessToken, "[REDACTED]")
            } else rawError
            throw IllegalStateException("Server returned HTTP $responseCode downloading ROM ($safeUrl). $sanitizedError".trim())
        }

        val totalLength = conn.contentLengthLong.coerceAtLeast(1L)
        var downloaded = 0L

        conn.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalLength > 0) {
                        onProgress((downloaded.toFloat() / totalLength).coerceIn(0f, 1f))
                    }
                }
                output.flush()
            }
        }

        if (tempFile.renameTo(target)) {
            onProgress(1f)
            target
        } else {
            throw IllegalStateException("Failed to finalize downloaded ROM file.")
        }
    }

    fun backupSave(gameId: String, kind: GameSaveKind): File? {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val localFile = getLocalSaveFile(gameId, kind)
        if (!localFile.exists() || localFile.length() <= 0) return null

        val backupDir = savesBackupDir
        val backupFile = File(backupDir, "${safeId}_${System.currentTimeMillis()}.${kind.value}.bak")
        return try {
            localFile.copyTo(backupFile, overwrite = true)
            // Keep newest 10 backups per gameId+kind
            val prefix = "${safeId}_"
            val suffix = ".${kind.value}.bak"
            val existing = backupDir.listFiles().orEmpty()
                .filter { it.name.startsWith(prefix) && it.name.endsWith(suffix) }
                .sortedByDescending { it.lastModified() }
            if (existing.size > 10) {
                existing.drop(10).forEach { it.delete() }
            }
            backupFile
        } catch (e: Exception) {
            android.util.Log.e("GameStorageManager", "Failed to backup save for $safeId", e)
            null
        }
    }

    /**
     * Stores an emulator battery save using the same local/cloud pathway as normal gameplay.
     * The existing local save is backed up first; if a cloud save exists it is backed up too
     * before it can be replaced. A cloud failure deliberately leaves the imported local save
     * intact so the normal sync workflow can retry later.
     */
    suspend fun importBatterySave(
        session: JellyfinSession?,
        gameId: String,
        data: ByteArray,
    ): Result<BatterySaveImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(data.size in 512..MAX_BATTERY_SAVE_BYTES) {
                "That file is not a supported battery save (must be between 512 bytes and 32 MB)."
            }

            val localFile = getLocalSaveFile(gameId, GameSaveKind.Sram)
            backupSave(gameId, GameSaveKind.Sram)

            // Preserve a cloud-only save as well, so importing from a new device cannot erase
            // the only recoverable copy of existing progress.
            val cloudData = session?.let {
                gamesRepository.getCloudSave(it, gameId, GameSaveKind.Sram).getOrNull()
            }
            if (cloudData != null && cloudData.isNotEmpty()) {
                backupSaveBytes(gameId, GameSaveKind.Sram, cloudData, "cloud")
            }

            localFile.parentFile?.mkdirs()
            val tempFile = File(localFile.parentFile, "${localFile.name}.importing")
            tempFile.writeBytes(data)
            if (localFile.exists() && !localFile.delete()) {
                throw IllegalStateException("Could not replace the existing local battery save.")
            }
            if (!tempFile.renameTo(localFile)) {
                throw IllegalStateException("Could not finalize the imported battery save.")
            }

            val cloudSynced = session?.let {
                gamesRepository.uploadCloudSave(it, gameId, GameSaveKind.Sram, data).isSuccess
            } ?: false
            if (cloudSynced) {
                markSynced(gameId, GameSaveKind.Sram, computeHash(data), localFile.lastModified())
            }
            BatterySaveImportResult(cloudSynced = cloudSynced)
        }
    }

    private fun backupSaveBytes(gameId: String, kind: GameSaveKind, data: ByteArray, source: String): File? {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return runCatching {
            val backupFile = File(savesBackupDir, "${safeId}_${System.currentTimeMillis()}_${source}.${kind.value}.bak")
            backupFile.writeBytes(data)
            backupFile
        }.onFailure {
            android.util.Log.w("GameStorageManager", "Could not create $source backup for $safeId", it)
        }.getOrNull()
    }

    data class BatterySaveImportResult(val cloudSynced: Boolean)


    fun computeHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun markSynced(gameId: String, kind: GameSaveKind, hash: String, timestamp: Long) {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        syncPrefs.edit()
            .putString("hash_${safeId}_${kind.value}", hash)
            .putLong("time_${safeId}_${kind.value}", timestamp)
            .apply()
    }

    fun getSyncedHash(gameId: String, kind: GameSaveKind): String? {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return syncPrefs.getString("hash_${safeId}_${kind.value}", null)
    }

    fun getSyncedTime(gameId: String, kind: GameSaveKind): Long {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return syncPrefs.getLong("time_${safeId}_${kind.value}", 0L)
    }

    suspend fun checkSaveSync(
        session: JellyfinSession?,
        gameId: String,
        kind: GameSaveKind,
    ): SaveSyncInfo = withContext(Dispatchers.IO) {
        val localFile = getLocalSaveFile(gameId, kind)
        val localExists = localFile.exists() && localFile.length() > 0
        val localBytes = if (localExists) runCatching { localFile.readBytes() }.getOrNull() else null
        val localModified = if (localExists) localFile.lastModified() else 0L
        val localSize = if (localExists) localFile.length() else 0L

        if (session == null) {
            val status = if (localExists) SaveSyncStatus.LOCAL_ONLY else SaveSyncStatus.IN_SYNC
            return@withContext SaveSyncInfo(
                status = status,
                localLastModified = localModified,
                localSizeBytes = localSize,
            )
        }

        val cloudEntry = gamesRepository.getCloudSaveWithMetadata(session, gameId, kind).getOrNull()

        if (!localExists && cloudEntry == null) {
            return@withContext SaveSyncInfo(
                status = SaveSyncStatus.IN_SYNC,
            )
        }

        if (localExists && cloudEntry == null) {
            return@withContext SaveSyncInfo(
                status = SaveSyncStatus.LOCAL_ONLY,
                localLastModified = localModified,
                localSizeBytes = localSize,
            )
        }

        if (!localExists && cloudEntry != null) {
            return@withContext SaveSyncInfo(
                status = SaveSyncStatus.CLOUD_ONLY,
                cloudLastModified = cloudEntry.lastModifiedMs,
                cloudSizeBytes = cloudEntry.sizeBytes,
                cachedCloudData = cloudEntry.data,
            )
        }

        val localHash = computeHash(localBytes ?: ByteArray(0))
        val cloudHash = computeHash(cloudEntry!!.data)

        if (localHash == cloudHash) {
            markSynced(gameId, kind, localHash, localModified)
            return@withContext SaveSyncInfo(
                status = SaveSyncStatus.IN_SYNC,
                localLastModified = localModified,
                localSizeBytes = localSize,
                cloudLastModified = cloudEntry.lastModifiedMs,
                cloudSizeBytes = cloudEntry.sizeBytes,
                cachedCloudData = cloudEntry.data,
            )
        }

        val syncedHash = getSyncedHash(gameId, kind)
        val status = when {
            syncedHash != null && cloudHash == syncedHash && localHash != syncedHash -> {
                // Cloud matches last sync, local was modified offline!
                SaveSyncStatus.LOCAL_NEWER
            }
            syncedHash != null && localHash == syncedHash && cloudHash != syncedHash -> {
                // Local matches last sync, cloud has new updates from another device
                SaveSyncStatus.CLOUD_NEWER
            }
            else -> {
                if (localModified > cloudEntry.lastModifiedMs + 5000L) {
                    SaveSyncStatus.LOCAL_NEWER
                } else if (cloudEntry.lastModifiedMs > localModified + 5000L) {
                    SaveSyncStatus.CLOUD_NEWER
                } else {
                    SaveSyncStatus.CONFLICT
                }
            }
        }

        SaveSyncInfo(
            status = status,
            localLastModified = localModified,
            localSizeBytes = localSize,
            cloudLastModified = cloudEntry.lastModifiedMs,
            cloudSizeBytes = cloudEntry.sizeBytes,
            cachedCloudData = cloudEntry.data,
        )
    }

    suspend fun replaceCloudWithLocalSave(
        session: JellyfinSession,
        gameId: String,
        kind: GameSaveKind,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = getLocalSaveFile(gameId, kind)
            if (!localFile.exists() || localFile.length() <= 0) {
                throw IllegalStateException("Local save file does not exist.")
            }
            val data = localFile.readBytes()
            gamesRepository.uploadCloudSave(session, gameId, kind, data).getOrThrow()
            val hash = computeHash(data)
            markSynced(gameId, kind, hash, localFile.lastModified())
            Unit
        }
    }

    suspend fun replaceLocalWithCloudSave(
        session: JellyfinSession,
        gameId: String,
        kind: GameSaveKind,
        cloudData: ByteArray? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            backupSave(gameId, kind)
            val data = cloudData ?: gamesRepository.getCloudSave(session, gameId, kind).getOrThrow()
                ?: throw IllegalStateException("Cloud save not found on server.")
            val localFile = getLocalSaveFile(gameId, kind)
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(data)
            val hash = computeHash(data)
            markSynced(gameId, kind, hash, localFile.lastModified())
            Unit
        }
    }

    suspend fun loadSaveState(
        session: JellyfinSession?,
        gameId: String,
        kind: GameSaveKind = GameSaveKind.State,
        forceCloud: Boolean = false,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val localFile = getLocalSaveFile(gameId, kind)

        if (forceCloud && session != null) {
            val cloudEntry = gamesRepository.getCloudSaveWithMetadata(session, gameId, kind).getOrNull()
            if (cloudEntry != null && cloudEntry.data.isNotEmpty()) {
                backupSave(gameId, kind)
                localFile.parentFile?.mkdirs()
                localFile.writeBytes(cloudEntry.data)
                markSynced(gameId, kind, computeHash(cloudEntry.data), localFile.lastModified())
                return@withContext cloudEntry.data
            }
        }

        if (localFile.exists() && localFile.length() > 0) {
            return@withContext localFile.readBytes()
        }

        if (kind == GameSaveKind.Sram) {
            val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val altSrm = File(savesDir, "$safeId.srm")
            if (altSrm.exists() && altSrm.length() > 0) {
                return@withContext altSrm.readBytes()
            }
            val altSav = File(savesDir, "$safeId.sav")
            if (altSav.exists() && altSav.length() > 0) {
                return@withContext altSav.readBytes()
            }
        }

        if (session != null) {
            val cloudEntry = gamesRepository.getCloudSaveWithMetadata(session, gameId, kind).getOrNull()
            if (cloudEntry != null && cloudEntry.data.isNotEmpty()) {
                localFile.parentFile?.mkdirs()
                localFile.writeBytes(cloudEntry.data)
                markSynced(gameId, kind, computeHash(cloudEntry.data), localFile.lastModified())
                return@withContext cloudEntry.data
            }
        }

        null
    }

    suspend fun saveState(
        session: JellyfinSession?,
        gameId: String,
        data: ByteArray,
        kind: GameSaveKind = GameSaveKind.State,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = getLocalSaveFile(gameId, kind)
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(data)
            val hash = computeHash(data)
            if (session != null) {
                val cloudRes = gamesRepository.uploadCloudSave(session, gameId, kind, data)
                if (cloudRes.isSuccess) {
                    markSynced(gameId, kind, hash, localFile.lastModified())
                } else {
                    android.util.Log.w("GameStorageManager", "Cloud save sync error: ${cloudRes.exceptionOrNull()?.message}")
                }
            }
            Unit
        }
    }

    suspend fun deleteSave(
        session: JellyfinSession?,
        gameId: String,
        kind: GameSaveKind = GameSaveKind.State,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = getLocalSaveFile(gameId, kind)
            if (localFile.exists()) {
                backupSave(gameId, kind)
                localFile.delete()
            }
            val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            syncPrefs.edit()
                .remove("hash_${safeId}_${kind.value}")
                .remove("time_${safeId}_${kind.value}")
                .apply()
            if (session != null) {
                gamesRepository.deleteCloudSave(session, gameId, kind).getOrThrow()
            }
            Unit
        }
    }
}

enum class SaveSyncStatus {
    IN_SYNC,
    LOCAL_NEWER,
    CLOUD_NEWER,
    CONFLICT,
    LOCAL_ONLY,
    CLOUD_ONLY,
}

data class SaveSyncInfo(
    val status: SaveSyncStatus,
    val localLastModified: Long = 0L,
    val localSizeBytes: Long = 0L,
    val cloudLastModified: Long = 0L,
    val cloudSizeBytes: Long = 0L,
    val cachedCloudData: ByteArray? = null,
)

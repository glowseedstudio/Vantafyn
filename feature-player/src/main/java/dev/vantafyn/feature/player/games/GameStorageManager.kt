package dev.vantafyn.feature.player.games

import android.content.Context
import android.os.Build
import android.os.Environment
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameBoxartScraper
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
import dev.vantafyn.core.emulator.NativeCoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

class GameStorageManager(
    private val context: Context,
    private val gamesRepository: JellyfinGamesRepository,
) {
    val nativeCoreManager by lazy { NativeCoreManager(context) }
    companion object {
        val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private const val MAX_BATTERY_SAVE_BYTES = 32 * 1024 * 1024

        // Battery: in-game saves write locally immediately, but the network radio is only woken
        // once per this interval unless the save is forced (pause/exit flush).
        private const val CLOUD_UPLOAD_MIN_INTERVAL_MS = 30_000L
    }

    private val lastCloudUploadAtMs = mutableMapOf<String, Long>()

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
        val keys = persistentRomsDir.listFiles().orEmpty()
            .filter { it.isFile && it.length() > 0 }
            .map { it.nameWithoutExtension }
            .toMutableSet()
        val dir = getLocal3dsDirectory()
        if (dir.exists() && dir.isDirectory) {
            val validExts = setOf("3ds", "cci", "cxi", "3dsx", "cia")
            dir.listFiles().orEmpty()
                .filter { it.isFile && it.extension.lowercase() in validExts }
                .forEach { file ->
                    keys.add(file.name)
                    keys.add(file.nameWithoutExtension)
                    keys.add("local_3ds_${file.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}")
                }
        }
        return keys
    }

    fun scanLocal3dsGameSummaries(): List<GameSummary> {
        val dir = getLocal3dsDirectory()
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val files = dir.listFiles() ?: return emptyList()
        val validExts = setOf("3ds", "cci", "cxi", "3dsx", "cia")
        return files
            .filter { it.isFile && it.extension.lowercase() in validExts }
            .sortedBy { it.name.lowercase() }
            .map { file ->
                val baseName = file.nameWithoutExtension
                val cleanTitle = baseName
                    .replace(Regex("\\[.*?\\]"), "")
                    .replace(Regex("\\(.*?\\)"), "")
                    .replace(Regex("^[0-9]{3,5}\\s*[-_]\\s*"), "")
                    .trim()
                    .ifEmpty { baseName }
                val safeId = "local_3ds_${file.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}"
                val pokemonMeta = dev.vantafyn.core.jellyfin.PokemonGameDetector.detect(cleanTitle, file.name, "3ds")
                GameSummary(
                    id = safeId,
                    title = cleanTitle,
                    systemId = "3ds",
                    filename = file.name,
                    sizeBytes = file.length(),
                    token = file.name,
                    extension = file.extension,
                    boxartUrl = GameBoxartScraper.resolve3dsBoxartUrl(file.name),
                    pokemon = pokemonMeta,
                )
            }
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
        val isNative = systemId.lowercase() in listOf("nds", "ds", "gba", "gb", "gbc", "3ds", "n3ds", "nintendo3ds") ||
            core.contains("melonds", ignoreCase = true) || core.contains("gpsp", ignoreCase = true) ||
            core.contains("mgba", ignoreCase = true) || core.contains("gambatte", ignoreCase = true) ||
            core.contains("tgbdual", ignoreCase = true) || core.contains("sameboy", ignoreCase = true) ||
            core.contains("azahar", ignoreCase = true) || core.contains("citra", ignoreCase = true)

        if (!isNative) {
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
        // Pre-cache native core if this is a Nintendo DS, 3DS, Game Boy Advance, or supported native system
        val nativeCoreId = nativeCoreManager.getCoreIdForSystem(systemId)
        if (isNative) {
            nativeCoreManager.ensureCoreInstalled(nativeCoreId)
        }
    }

    fun getLocalSaveFile(gameId: String, kind: GameSaveKind): File {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(savesDir, "$safeId.${kind.value}")
    }

    fun findExistingLocalSaveFile(gameId: String, kind: GameSaveKind): File {
        val standard = getLocalSaveFile(gameId, kind)
        if (standard.exists() && standard.length() > 0) return standard

        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val altId = if (safeId.startsWith("local_3ds_")) safeId.removePrefix("local_3ds_") else safeId
        val nativeSaveDir = nativeCoreManager.getSaveDirectory()

        val altCandidates = mutableListOf(
            File(savesDir, "$safeId.sav"),
            File(savesDir, "$safeId.srm"),
            File(savesDir, "$safeId.main"),
            File(savesDir, "$safeId.bin"),
            File(savesDir, "$altId.sav"),
            File(savesDir, "$altId.srm"),
            File(savesDir, "$altId.main"),
            File(savesDir, "main"),
            File(nativeSaveDir, "$safeId.sav"),
            File(nativeSaveDir, "$safeId.srm"),
            File(nativeSaveDir, "$safeId.main"),
            File(nativeSaveDir, "$altId.sav"),
            File(nativeSaveDir, "$altId.srm"),
            File(nativeSaveDir, "$altId.main"),
            File(nativeSaveDir, "main"),
        )

        // Check if save file is stored alongside ROM in local 3DS directory
        val local3dsDir = getLocal3dsDirectory()
        if (local3dsDir.exists() && local3dsDir.isDirectory) {
            altCandidates.add(File(local3dsDir, "$altId.sav"))
            altCandidates.add(File(local3dsDir, "$altId.srm"))
            altCandidates.add(File(local3dsDir, "$altId.main"))
        }

        for (alt in altCandidates) {
            if (alt.exists() && alt.length() > 0) return alt
        }

        // Citra sdmc title save search fallback (00000001/main)
        val citraSearchRoots = listOf(
            File(nativeSaveDir, "Citra/sdmc/Nintendo 3DS"),
            File(nativeSaveDir, "citra/sdmc/Nintendo 3DS"),
            File(savesDir, "Citra/sdmc/Nintendo 3DS"),
            File(savesDir, "citra/sdmc/Nintendo 3DS"),
        )
        for (root in citraSearchRoots) {
            if (root.exists() && root.isDirectory) {
                val found = root.walkTopDown()
                    .filter { it.isFile && (it.name.equals("main", ignoreCase = true) || it.name.endsWith(".sav") || it.name.endsWith(".srm")) }
                    .maxByOrNull { it.lastModified() }
                if (found != null && found.length() > 0) return found
            }
        }

        return standard
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

    fun normalizeDirectoryPath(raw: String): File {
        var trimmed = raw.trim().trimEnd('/')
        // Fix common mobile keyboard typo: letter 'O' instead of digit '0' in /storage/emulated/0
        trimmed = trimmed
            .replace("/emulated/O/", "/emulated/0/")
            .replace("/emulated/o/", "/emulated/0/")
            .replace("/emulated/O", "/emulated/0")
            .replace("/emulated/o", "/emulated/0")

        val candidate = when {
            trimmed.startsWith("/storage/") || trimmed.startsWith("/sdcard/") -> File(trimmed)
            trimmed.startsWith("/sdcard") -> File(trimmed)
            trimmed.startsWith("/") -> {
                val direct = File(trimmed)
                if (direct.exists()) direct else File("/sdcard$trimmed")
            }
            else -> File("/sdcard/$trimmed")
        }
        if (candidate.exists()) return candidate

        if (candidate.absolutePath.startsWith("/storage/emulated/0")) {
            val alt = File(candidate.absolutePath.replaceFirst("/storage/emulated/0", "/sdcard"))
            if (alt.exists()) return alt
        } else if (candidate.absolutePath.startsWith("/sdcard")) {
            val alt = File(candidate.absolutePath.replaceFirst("/sdcard", "/storage/emulated/0"))
            if (alt.exists()) return alt
        }

        val parent = candidate.parentFile
        if (parent != null && parent.exists()) {
            val match = parent.listFiles()?.firstOrNull { it.name.equals(candidate.name, ignoreCase = true) }
            if (match != null) return match
        }
        return candidate
    }

    fun getLocal3dsDirectory(): File {
        val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
        val customPath = prefs.getString("local_3ds_rom_directory", null)?.takeIf { it.isNotBlank() }
        if (customPath != null) {
            val dir = normalizeDirectoryPath(customPath)
            if (dir.exists() && dir.isDirectory) return dir
        }
        val commonCandidates = listOf(
            File("/sdcard/Roms/3DS"),
            File("/sdcard/ROMs/3ds"),
            File("/sdcard/ROMs/3DS"),
            File("/sdcard/Roms/3ds"),
            File("/sdcard/Download/3ds"),
            File("/sdcard/Download"),
            File(context.getExternalFilesDir(null), "3ds"),
        )
        for (cand in commonCandidates) {
            if (cand.exists() && cand.isDirectory) return cand
        }
        val appExtDir = File(context.getExternalFilesDir(null), "3ds")
        if (!appExtDir.exists()) appExtDir.mkdirs()
        return appExtDir
    }

    private fun normalizeTitle(raw: String): String {
        return raw.lowercase()
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("^[0-9]{3,5}\\s*[-_]\\s*"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    fun findLocal3dsRom(game: GameDetail): File? {
        val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
        val customPath = prefs.getString("local_3ds_rom_directory", null)?.takeIf { it.isNotBlank() }

        val candidateDirs = mutableListOf<File>()
        if (customPath != null) {
            candidateDirs.add(normalizeDirectoryPath(customPath))
        }

        candidateDirs.add(File("/sdcard/Roms/3DS"))
        candidateDirs.add(File("/sdcard/ROMs/3ds"))
        candidateDirs.add(File("/sdcard/ROMs/3DS"))
        candidateDirs.add(File("/sdcard/Roms/3ds"))
        candidateDirs.add(File("/sdcard/Download/3ds"))
        candidateDirs.add(File("/sdcard/Download/3DS"))
        candidateDirs.add(File("/sdcard/Download"))
        candidateDirs.add(File("/sdcard/3ds"))
        candidateDirs.add(File("/sdcard/3DS"))
        candidateDirs.add(File(context.getExternalFilesDir(null), "3ds"))
        candidateDirs.add(File(context.getExternalFilesDir(null), "roms/3ds"))
        candidateDirs.add(File(context.filesDir, "3ds"))

        val extStorage = Environment.getExternalStorageDirectory()
        if (extStorage != null && extStorage.exists()) {
            candidateDirs.add(File(extStorage, "Roms/3DS"))
            candidateDirs.add(File(extStorage, "ROMs/3ds"))
        }

        val hasAllFiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
        android.util.Log.i(
            "Vantafyn3DS",
            "findLocal3dsRom searching for '${game.title}' (id=${game.id}, filename='${game.filename}', cleanTitle='${game.cleanTitle}'). AllFilesAccess=$hasAllFiles"
        )

        val targetFilename = game.filename.lowercase().trim()
        val targetWithoutExt = targetFilename.substringBeforeLast('.')
        val cleanGameTitle = game.cleanTitle.ifEmpty { game.title }.lowercase().trim()
        val normTargetTitle = normalizeTitle(cleanGameTitle)
        val validExts = setOf("cci", "3ds", "cxi", "3dsx", "cia")

        val distinctDirs = candidateDirs.distinctBy { it.absolutePath }
        for (dir in distinctDirs) {
            if (!dir.exists() || !dir.isDirectory) {
                continue
            }
            val files = try {
                dir.walkTopDown().maxDepth(3).filter { it.isFile && it.length() > 0L }.toList()
            } catch (e: Exception) {
                android.util.Log.w("Vantafyn3DS", "Error walking ${dir.absolutePath}: ${e.message}")
                dir.listFiles()?.filter { it.isFile && it.length() > 0L } ?: emptyList()
            }
            android.util.Log.i("Vantafyn3DS", "Scanning ${dir.absolutePath} (${files.size} total files found)")
            for (f in files) {
                val fExt = f.extension.lowercase()
                if (fExt !in validExts) continue
                val fName = f.name.lowercase()
                val fNameWithoutExt = f.nameWithoutExtension.lowercase()

                // 1. Exact filename match
                if (targetFilename.isNotBlank() && fName == targetFilename) {
                    android.util.Log.i("Vantafyn3DS", "MATCH SUCCESS (exact filename): ${f.absolutePath}")
                    return f
                }
                // 2. Exact name without extension match
                if (targetWithoutExt.isNotBlank() && fNameWithoutExt == targetWithoutExt) {
                    android.util.Log.i("Vantafyn3DS", "MATCH SUCCESS (name without ext): ${f.absolutePath}")
                    return f
                }
                // 3. Clean title match
                if (cleanGameTitle.isNotBlank() && (fNameWithoutExt == cleanGameTitle ||
                        fNameWithoutExt.startsWith(cleanGameTitle) ||
                        cleanGameTitle.startsWith(fNameWithoutExt))) {
                    android.util.Log.i("Vantafyn3DS", "MATCH SUCCESS (clean title): ${f.absolutePath}")
                    return f
                }
                // 4. Normalized title match (ignores brackets, regions, scene numbers, hyphens)
                val normFile = normalizeTitle(fNameWithoutExt)
                if (normTargetTitle.isNotBlank() && normFile.isNotBlank()) {
                    if (normFile == normTargetTitle || normFile.contains(normTargetTitle) || normTargetTitle.contains(normFile)) {
                        android.util.Log.i("Vantafyn3DS", "MATCH SUCCESS (normalized title '$normTargetTitle' == '$normFile'): ${f.absolutePath}")
                        return f
                    }
                }
            }
        }
        android.util.Log.w("Vantafyn3DS", "NO MATCH found for '${game.title}' across ${distinctDirs.size} folders.")
        return null
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

        val downloadUrl = if (game.downloadUrl.isNotBlank() && !game.downloadUrl.contains("/ROM/?") && !game.downloadUrl.contains("/ROM//")) {
            game.downloadUrl
        } else {
            val effLibId = libraryId.ifBlank { "default" }
            gamesRepository.getRomDownloadUrl(session, effLibId, token)
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
                val originHost = runCatching { URL(session.server.url).host.lowercase() }.getOrNull()
                val targetHost = runCatching { URL(currentUrl).host.lowercase() }.getOrNull()
                val isSameHost = originHost != null && targetHost != null && originHost == targetHost

                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 90_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "*/*")
                    if (isSameHost) {
                        val authHeader = session.mediaBrowserAuthHeader()
                        setRequestProperty("Authorization", authHeader)
                        setRequestProperty("X-Emby-Token", session.accessToken)
                        setRequestProperty("X-MediaBrowser-Token", session.accessToken)
                        setRequestProperty("X-Emby-Authorization", authHeader)
                    }
                }
                responseCode = conn.responseCode
            }
        }

        if (responseCode !in 200..299) {
            val safeUrl = currentUrl.substringBefore('?')
            val rawError = conn.errorStream?.bufferedReader()?.use { reader ->
                val charBuf = CharArray(4096)
                val read = reader.read(charBuf)
                if (read > 0) String(charBuf, 0, read) else ""
            } ?: ""
            val sanitizedError = if (session.accessToken.isNotBlank()) {
                rawError.replace(session.accessToken, "[REDACTED]")
            } else rawError
            throw IllegalStateException("Server returned HTTP $responseCode downloading ROM ($safeUrl). $sanitizedError".trim())
        }

        val serverLength = conn.contentLengthLong
        val totalLength = if (serverLength > 0L) serverLength else if (game.sizeBytes > 0L) game.sizeBytes else 1L
        var downloaded = 0L

        conn.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalLength > 0L) {
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
        val localFile = findExistingLocalSaveFile(gameId, kind)
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
        val localFile = findExistingLocalSaveFile(gameId, kind)
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
            val localFile = findExistingLocalSaveFile(gameId, kind)
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
            val localFile = findExistingLocalSaveFile(gameId, kind)
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
        kind: GameSaveKind = GameSaveKind.Sram,
        forceCloud: Boolean = false,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val localFile = findExistingLocalSaveFile(gameId, kind)

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

        if (session != null) {
            val cloudEntry = gamesRepository.getCloudSaveWithMetadata(session, gameId, kind).getOrNull()
            if (cloudEntry != null && cloudEntry.data.isNotEmpty()) {
                val targetFile = getLocalSaveFile(gameId, kind)
                targetFile.parentFile?.mkdirs()
                targetFile.writeBytes(cloudEntry.data)
                markSynced(gameId, kind, computeHash(cloudEntry.data), targetFile.lastModified())
                return@withContext cloudEntry.data
            }
        }

        null
    }

    suspend fun saveState(
        session: JellyfinSession?,
        gameId: String,
        data: ByteArray,
        kind: GameSaveKind = GameSaveKind.Sram,
        forceUpload: Boolean = false,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = getLocalSaveFile(gameId, kind)
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(data)
            val hash = computeHash(data)
            if (session != null) {
                val key = "${gameId}_${kind.value}"
                val now = System.currentTimeMillis()
                val lastUpload = lastCloudUploadAtMs[key] ?: 0L
                if (forceUpload || now - lastUpload >= CLOUD_UPLOAD_MIN_INTERVAL_MS) {
                    lastCloudUploadAtMs[key] = now
                    val cloudRes = gamesRepository.uploadCloudSave(session, gameId, kind, data)
                    if (cloudRes.isSuccess) {
                        markSynced(gameId, kind, hash, localFile.lastModified())
                    } else {
                        // Allow an immediate retry on the next save instead of waiting out the window.
                        lastCloudUploadAtMs.remove(key)
                        android.util.Log.w("GameStorageManager", "Cloud save sync error: ${cloudRes.exceptionOrNull()?.message}")
                    }
                } else {
                    // Local write landed; cloud upload deferred to the next window or forced flush.
                    // checkSaveSync still sees this as LOCAL_NEWER, so nothing is lost.
                    android.util.Log.d("GameStorageManager", "Cloud upload debounced for ${gameId}/${kind.value}")
                }
            }
            Unit
        }
    }

    suspend fun deleteSave(
        session: JellyfinSession?,
        gameId: String,
        kind: GameSaveKind = GameSaveKind.Sram,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = findExistingLocalSaveFile(gameId, kind)
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

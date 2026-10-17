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
        val dirs = getLocal3dsDirectories()
        val validExts = setOf("3ds", "cci", "cxi", "3dsx", "cia")
        val seenTokens = mutableSetOf<String>()
        val summaries = mutableListOf<GameSummary>()

        for (dir in dirs) {
            val files = dir.listFiles()?.filter { it.isFile && it.extension.lowercase() in validExts } ?: continue
            for (file in files) {
                val token = file.name
                if (seenTokens.add(token.lowercase())) {
                    val baseName = file.nameWithoutExtension
                    val cleanTitle = baseName
                        .replace(Regex("\\[.*?\\]"), "")
                        .replace(Regex("\\(.*?\\)"), "")
                        .replace(Regex("^[0-9]{3,5}\\s*[-_]\\s*"), "")
                        .trim()
                        .ifEmpty { baseName }
                    val safeId = "local_3ds_${file.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}"
                    val pokemonMeta = dev.vantafyn.core.jellyfin.PokemonGameDetector.detect(cleanTitle, file.name, "3ds")
                    summaries.add(
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
                    )
                }
            }
        }
        return summaries.sortedBy { it.title.lowercase() }
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

    fun getCitraRoots(): List<File> {
        val nativeSaveDir = nativeCoreManager.getSaveDirectory()
        return listOf(
            File(nativeSaveDir, "Azahar/sdmc/Nintendo 3DS"),
            File(nativeSaveDir, "azahar/sdmc/Nintendo 3DS"),
            File(savesDir, "Azahar/sdmc/Nintendo 3DS"),
            File(savesDir, "azahar/sdmc/Nintendo 3DS"),
            File(nativeSaveDir, "Citra/sdmc/Nintendo 3DS"),
            File(nativeSaveDir, "citra/sdmc/Nintendo 3DS"),
            File(savesDir, "Citra/sdmc/Nintendo 3DS"),
            File(savesDir, "citra/sdmc/Nintendo 3DS"),
        )
    }

    fun read3dsTitleIdFromRom(romFile: File?): String? {
        if (romFile == null || !romFile.exists() || !romFile.canRead()) return null
        return try {
            java.io.RandomAccessFile(romFile, "r").use { raf ->
                if (raf.length() >= 0x110) {
                    raf.seek(0x100)
                    val magic = ByteArray(4)
                    raf.readFully(magic)
                    if (String(magic, Charsets.US_ASCII) == "NCSD") {
                        raf.seek(0x108)
                        val idBytes = ByteArray(8)
                        raf.readFully(idBytes)
                        val sb = StringBuilder()
                        for (i in 7 downTo 0) {
                            sb.append(String.format("%02x", idBytes[i].toInt() and 0xFF))
                        }
                        val tid = sb.toString()
                        if (tid.startsWith("0004")) tid else null
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getStorageRoots(): List<File> {
        val roots = linkedSetOf<File>()
        val ext = Environment.getExternalStorageDirectory()
        if (ext != null && ext.exists()) roots.add(ext)
        val sdcard = File("/sdcard")
        if (sdcard.exists()) roots.add(sdcard)

        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles()?.forEach { c ->
                    if (c.isDirectory && c.name != "self" && c.canRead()) {
                        if (c.name == "emulated") {
                            c.listFiles()?.filter { it.isDirectory }?.forEach { roots.add(it) }
                        } else {
                            roots.add(c)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val extDirs = context.getExternalFilesDirs(null)
            for (f in extDirs) {
                if (f != null) {
                    val path = f.absolutePath
                    val marker = "/Android/"
                    if (path.contains(marker)) {
                        val rootPath = path.substringBefore(marker)
                        val root = File(rootPath)
                        if (root.exists() && root.isDirectory) {
                            roots.add(root)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return roots.toList()
    }

    private fun matchesKeywords(name: String, keywords: Collection<String>): Boolean {
        val lower = name.lowercase().trim()
        val tokens = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        for (rawKw in keywords) {
            val kw = rawKw.lowercase().trim()
            if (kw.isEmpty()) continue
            if (lower == kw) return true
            if (tokens.contains(kw)) return true
            if (kw.length >= 3 && (lower.contains(kw) || kw.contains(lower))) return true
        }
        return false
    }

    fun get3dsTitleIds(identifier: String): Set<String> {
        val lower = identifier.lowercase()
        val clean = lower.replace(Regex("[^a-z0-9]"), " ")
        return when {
            lower.contains("ultra") && (lower.contains("sun") || lower.contains("us")) -> setOf("001b5000")
            lower.contains("ultra") && (lower.contains("moon") || lower.contains("um")) -> setOf("001b5100")
            lower.contains("sun") && !lower.contains("ultra") -> setOf("00164800")
            lower.contains("moon") && !lower.contains("ultra") -> setOf("00175e00")
            lower.contains("omega") || lower.contains("ruby") -> setOf("0011c400")
            lower.contains("alpha") || lower.contains("sapphire") -> setOf("0011c500")
            lower.contains("00055d00") || clean.contains("pokemon x") || lower.contains("pokemon_x") || lower.endsWith("_x") || lower == "x" -> setOf("00055d00")
            lower.contains("00055e00") || clean.contains("pokemon y") || lower.contains("pokemon_y") || lower.endsWith("_y") || lower == "y" -> setOf("00055e00")
            lower.contains("ocarina") || lower.contains("oot") -> setOf("00033600", "00033500", "00033400")
            lower.contains("majora") || lower.contains("mm3d") -> setOf("00125600", "00125500", "000d6e00")
            lower.contains("between worlds") || lower.contains("albw") -> setOf("000ec400", "000ec300")
            else -> emptySet()
        }
    }

    fun get3dsFullTitleIds(identifier: String, romFile: File? = null): Set<String> {
        val results = linkedSetOf<String>()
        val romId = read3dsTitleIdFromRom(romFile)
        if (romId != null) {
            results.add(romId.uppercase())
            results.add(romId.lowercase())
        }
        val trimmed = identifier.trim()
        if (trimmed.length == 16 && trimmed.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            results.add(trimmed.uppercase())
            results.add(trimmed.lowercase())
        }
        val shortIds = get3dsTitleIds(identifier)
        for (sid in shortIds) {
            results.add("00040000" + sid.uppercase())
            results.add("00040000" + sid.lowercase())
        }
        return results
    }

    fun get3dsTextureKeywords(identifier: String): List<String> {
        val lower = identifier.lowercase()
        val clean = lower.replace(Regex("[^a-z0-9]"), " ")
        val list = mutableListOf<String>()
        if (lower.contains("001b5000") || (lower.contains("ultra") && (lower.contains("sun") || lower.contains("us")))) {
            list.addAll(listOf("usum", "ultra sun", "ultrasun", "ultra_sun", "001b5000", "00040000001b5000"))
        }
        if (lower.contains("001b5100") || (lower.contains("ultra") && (lower.contains("moon") || lower.contains("um")))) {
            list.addAll(listOf("usum", "ultra moon", "ultramoon", "ultra_moon", "001b5100", "00040000001b5100"))
        }
        if (lower.contains("00164800") || (lower.contains("sun") && !lower.contains("ultra"))) {
            list.addAll(listOf("pokemon sun", "sun", "00164800", "0004000000164800"))
        }
        if (lower.contains("00175e00") || (lower.contains("moon") && !lower.contains("ultra"))) {
            list.addAll(listOf("pokemon moon", "moon", "00175e00", "0004000000175e00"))
        }
        if (lower.contains("0011c400") || lower.contains("omega") || lower.contains("ruby")) {
            list.addAll(listOf("oras", "omega ruby", "omegaruby", "0011c400", "000400000011c400"))
        }
        if (lower.contains("0011c500") || lower.contains("alpha") || lower.contains("sapphire")) {
            list.addAll(listOf("oras", "alpha sapphire", "alphasapphire", "0011c500", "000400000011c500"))
        }
        if (lower.contains("00055d00") || clean.contains("pokemon x") || lower.contains("pokemon_x") || lower.endsWith("_x") || lower == "x") {
            list.addAll(listOf("xy", "pokemon x", "pokemonx", "pokemon_x", "00055d00", "0004000000055d00"))
        }
        if (lower.contains("00055e00") || clean.contains("pokemon y") || lower.contains("pokemon_y") || lower.endsWith("_y") || lower == "y") {
            list.addAll(listOf("xy", "pokemon y", "pokemony", "pokemon_y", "00055e00", "0004000000055e00"))
        }
        if (lower.contains("00033600") || lower.contains("00033500") || lower.contains("00033400") ||
            lower.contains("ocarina") || lower.contains("oot") || lower.contains("zelda")) {
            list.addAll(listOf("oot", "ocarina", "oot3d", "oot 3ds", "oot_3ds", "oot 3ds ss", "singlescreen", "00033600", "0004000000033600", "00033500", "0004000000033500", "00033400", "0004000000033400"))
        }
        if (lower.contains("00125600") || lower.contains("00125500") || lower.contains("000d6e00") ||
            lower.contains("majora") || lower.contains("mm3d")) {
            list.addAll(listOf("mm", "mm3d", "majora", "00125600", "0004000000125600", "00125500", "0004000000125500"))
        }
        val cleanSimple = lower.replace(Regex("[^a-z0-9]"), "")
        if (cleanSimple.length >= 3) list.add(cleanSimple)
        return list.distinct()
    }

    private fun isReShadeDir(dir: File): Boolean {
        val path = dir.absolutePath.lowercase()
        return path.contains("reshade") || path.contains("shaders")
    }

    private fun hasTextureFiles(dir: File): Boolean {
        if (isReShadeDir(dir)) return false
        return dir.listFiles()?.any {
            it.isFile && (it.name.startsWith("tex1_", ignoreCase = true) ||
                         it.name.endsWith(".png", ignoreCase = true) ||
                         it.name.endsWith(".dds", ignoreCase = true) ||
                         it.name.endsWith(".ktx", ignoreCase = true))
        } == true
    }

    private fun resolveTextureFolder(dir: File, targetTitleIds: Collection<String> = emptyList()): File? {
        if (isReShadeDir(dir)) return null

        var bestCandidate: File? = null
        fun searchTree(current: File, depth: Int) {
            if (depth > 6 || isReShadeDir(current)) return
            val name = current.name.lowercase()
            val matchesTid = targetTitleIds.any { tid ->
                val fullTid = tid.lowercase()
                val shortTid = if (tid.length == 16) tid.substring(8).lowercase() else fullTid
                name == fullTid || name == shortTid
            }
            val hasPackJson = File(current, "pack.json").exists()
            val hasDirectTextures = hasTextureFiles(current)
            val hasSubdirTextures = current.listFiles()?.any { it.isDirectory && !isReShadeDir(it) && hasTextureFiles(it) } == true
            if ((matchesTid || hasPackJson) && (hasDirectTextures || hasSubdirTextures)) {
                if (bestCandidate == null || current.absolutePath.contains("load/textures", ignoreCase = true)) {
                    bestCandidate = current
                }
            }
            val subdirs = current.listFiles()?.filter { it.isDirectory && !isReShadeDir(it) } ?: emptyList()
            for (sub in subdirs) {
                searchTree(sub, depth + 1)
            }
        }

        searchTree(dir, 0)
        if (bestCandidate != null) return bestCandidate

        if (hasTextureFiles(dir)) return dir
        var current = dir
        while (true) {
            val subdirs = current.listFiles()?.filter { it.isDirectory && !isReShadeDir(it) } ?: emptyList()
            if (hasTextureFiles(current)) return current
            if (subdirs.size == 1) {
                current = subdirs.first()
            } else if (subdirs.size > 1) {
                if (subdirs.any { hasTextureFiles(it) }) return current
                for (sub in subdirs) {
                    val found = resolveTextureFolder(sub, targetTitleIds)
                    if (found != null) return found
                }
                return null
            } else {
                return null
            }
        }
    }

    fun find3dsCustomTexturesDirectory(gameId: String, gameTitle: String? = null, romFile: File? = null): File? {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val altId = if (safeId.startsWith("local_3ds_")) safeId.removePrefix("local_3ds_") else safeId
        val decodedTitle = runCatching {
            String(android.util.Base64.decode(gameId, android.util.Base64.DEFAULT))
                .substringAfterLast('/')
                .substringBeforeLast('.')
        }.getOrNull()?.trim()

        val identifiers = listOfNotNull(gameTitle, decodedTitle, safeId, altId, gameId).distinct()
        val fullTitleIds = (identifiers.flatMap { get3dsFullTitleIds(it, romFile) } + listOfNotNull(read3dsTitleIdFromRom(romFile))).distinct()
        val keywords = identifiers.flatMap { get3dsTextureKeywords(it) }.map { it.lowercase() }.distinct()

        val searchRoots = linkedSetOf<File>()
        for (root in getStorageRoots()) {
            listOf(
                "Download/texture packs", "Download/texture_packs", "Download/Texture Packs",
                "Download/textures", "Download/Textures", "Download/mods", "Download/Mods",
                "Download", "texture packs", "texture_packs", "textures", "mods",
                "Vantafyn/textures", "RetroArch/saves/Citra/load/textures", "citra-emu/load/textures"
            ).forEach { rel ->
                val f = File(root, rel)
                if (f.exists() && f.isDirectory) searchRoots.add(f)
            }
        }
        val appTexDir = File(context.filesDir, "textures")
        if (appTexDir.exists() && appTexDir.isDirectory) searchRoots.add(appTexDir)

        for (root in searchRoots) {
            val children = root.listFiles()?.filter { it.isDirectory } ?: continue
            for (child in children) {
                val childName = child.name.lowercase()
                val isMatch = fullTitleIds.any { childName.contains(it.lowercase()) } ||
                        matchesKeywords(childName, keywords)
                if (isMatch) {
                    val resolved = resolveTextureFolder(child, fullTitleIds)
                    if (resolved != null) {
                        android.util.Log.i("GameStorageManager", "Found custom textures for $gameTitle in: ${resolved.absolutePath}")
                        return resolved
                    }
                }
            }
        }
        return null
    }

    fun setup3dsCustomTextures(gameId: String, gameTitle: String? = null, romFile: File? = null): Boolean {
        return update3dsAssets(gameId, gameTitle, romFile, customTexturesEnabled = true, mods3dsEnabled = true).first
    }

    fun remove3dsCustomTextures(gameId: String, gameTitle: String? = null, romFile: File? = null): Boolean {
        return update3dsAssets(gameId, gameTitle, romFile, customTexturesEnabled = false, mods3dsEnabled = true).first
    }

    data class Resolved3dsMod(
        val titleId: String,
        val modDir: File,
        val textureDir: File? = null,
    )

    private fun is3dsModDirectory(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val names = dir.list() ?: return false
        return names.any { name ->
            name.equals("romfs", ignoreCase = true) ||
            name.equals("exheader.bin", ignoreCase = true) ||
            name.equals("code.ips", ignoreCase = true) ||
            name.equals("exefs", ignoreCase = true) ||
            name.equals("cheats", ignoreCase = true)
        }
    }

    private fun score3dsModCandidate(dir: File, romTid: String?, chosenTid: String): Int {
        var score = 0
        val path = dir.absolutePath.lowercase()

        // 1. Exact match for ROM Title ID
        if (romTid != null && chosenTid.equals(romTid, ignoreCase = true)) {
            score += 1000
        }

        // 2. Strongly prefer directory tree targeted for Emulators (Citra/Azahar) over 3DS console hardware CFW
        if (path.contains("/emulator/") || path.contains("citra") || path.contains("azahar") || path.endsWith("/emulator")) {
            score += 400
        }
        if (path.contains("3ds hardware") || path.contains("/luma/") || path.contains("luma3ds") || path.contains("/hardware/")) {
            score -= 400
        }

        // 3. Strongly prefer dedicated mods directory over subfolders inside texture packs
        if (path.contains("/texture packs/") || path.contains("/texture_packs/") || path.contains("/textures/")) {
            score -= 200
        }
        if (path.contains("/download/mods/") || path.contains("/mods/")) {
            score += 100
        }

        // 4. ExeFS / Citra structure preferences
        if (File(dir, "exefs/code.ips").exists() || File(dir, "exefs/code.bps").exists() || File(dir, "exefs").isDirectory) {
            score += 100
        }
        if (File(dir, "exheader.bin").exists()) {
            score += 50
        }
        if (File(dir, "romfs").isDirectory) {
            score += 25
        }

        return score
    }

    fun find3dsMod(gameId: String, gameTitle: String? = null, romFile: File? = null): Resolved3dsMod? {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val altId = if (safeId.startsWith("local_3ds_")) safeId.removePrefix("local_3ds_") else safeId
        val decodedTitle = runCatching {
            String(android.util.Base64.decode(gameId, android.util.Base64.DEFAULT))
                .substringAfterLast('/')
                .substringBeforeLast('.')
        }.getOrNull()?.trim()

        val identifiers = listOfNotNull(gameTitle, decodedTitle, safeId, altId, gameId).distinct()
        val candidateTids = linkedSetOf<String>()
        val romTid = read3dsTitleIdFromRom(romFile)
        if (romTid != null) candidateTids.add(romTid)
        candidateTids.addAll(identifiers.flatMap { get3dsFullTitleIds(it, romFile) })

        val keywords = identifiers.flatMap { get3dsTextureKeywords(it) }.map { it.lowercase() }.distinct()

        val searchRoots = linkedSetOf<File>()
        for (root in getStorageRoots()) {
            listOf(
                "Download/mods", "Download/Mods", "Download/3ds/mods", "Download/3DS/mods",
                "Download", "mods", "Mods", "3ds/mods", "3DS/mods", "Vantafyn/mods",
                "RetroArch/saves/Citra/load/mods", "citra-emu/load/mods"
            ).forEach { rel ->
                val f = File(root, rel)
                if (f.exists() && f.isDirectory) searchRoots.add(f)
            }
        }
        val appModDir = File(context.filesDir, "mods")
        if (appModDir.exists() && appModDir.isDirectory) searchRoots.add(appModDir)

        val foundCandidates = mutableListOf<Pair<String, File>>()

        fun scanDir(dir: File, depth: Int) {
            if (depth > 6) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (!child.isDirectory) continue
                val name = child.name.lowercase()
                for (tid in candidateTids) {
                    val fullTid = tid.lowercase()
                    val shortTid = if (tid.length == 16) tid.substring(8).lowercase() else fullTid
                    if (name == fullTid || name == shortTid) {
                        if (is3dsModDirectory(child)) {
                            foundCandidates.add(tid to child)
                        }
                    }
                }
                scanDir(child, depth + 1)
            }
        }

        for (root in searchRoots) {
            scanDir(root, 0)
        }

        // If no direct TitleID directory matched, check directories matching keywords
        if (foundCandidates.isEmpty()) {
            for (root in searchRoots) {
                val children = root.listFiles()?.filter { it.isDirectory } ?: continue
                for (child in children) {
                    val childName = child.name.lowercase()
                    if (matchesKeywords(childName, keywords)) {
                        if (is3dsModDirectory(child)) {
                            val tid = candidateTids.firstOrNull() ?: "0004000000000000"
                            foundCandidates.add(tid to child)
                        } else {
                            val subMods = child.walkTopDown().maxDepth(4).filter { it.isDirectory && is3dsModDirectory(it) }.toList()
                            for (sub in subMods) {
                                val tid = candidateTids.firstOrNull() ?: "0004000000000000"
                                foundCandidates.add(tid to sub)
                            }
                        }
                    }
                }
            }
        }

        if (foundCandidates.isEmpty()) return null

        val best = foundCandidates.maxByOrNull { (tid, dir) ->
            score3dsModCandidate(dir, romTid, tid)
        } ?: return null

        val chosenTid = best.first
        val modDir = best.second

        // Find associated texture folder if one exists (e.g. load/textures/<TitleID>)
        var textureDir: File? = null
        val tidFolder = chosenTid.lowercase()
        val loadParent = modDir.parentFile?.parentFile // e.g. from .../load/mods/0004... -> .../load
        if (loadParent != null) {
            val siblingTexture1 = File(loadParent, "textures/$chosenTid")
            val siblingTexture2 = File(loadParent, "textures/${modDir.name}")
            val siblingTexture3 = File(loadParent, "textures")
            textureDir = when {
                siblingTexture1.exists() && siblingTexture1.isDirectory && hasTextureFiles(siblingTexture1) -> siblingTexture1
                siblingTexture2.exists() && siblingTexture2.isDirectory && hasTextureFiles(siblingTexture2) -> siblingTexture2
                siblingTexture3.exists() && siblingTexture3.isDirectory && hasTextureFiles(siblingTexture3) -> siblingTexture3
                else -> null
            }
        }
        if (textureDir == null) {
            // Check within the mod directory's ancestors for a textures directory
            var ancestor: File? = modDir.parentFile
            var hops = 0
            while (ancestor != null && hops < 5) {
                val texturesChild = File(ancestor, "textures")
                if (texturesChild.exists() && texturesChild.isDirectory) {
                    val resolved = resolveTextureFolder(texturesChild, listOf(chosenTid)) ?: texturesChild
                    if (hasTextureFiles(resolved) || resolved.listFiles()?.any { hasTextureFiles(it) } == true) {
                        textureDir = resolved
                        break
                    }
                }
                ancestor = ancestor.parentFile
                hops++
            }
        }

        android.util.Log.i("GameStorageManager", "Resolved 3DS mod for $gameTitle ($chosenTid): ${modDir.absolutePath}, textures: ${textureDir?.absolutePath}")
        return Resolved3dsMod(titleId = chosenTid, modDir = modDir, textureDir = textureDir)
    }

    fun find3dsModTextureDirectories(gameId: String, gameTitle: String? = null, romFile: File? = null): List<File> {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val altId = if (safeId.startsWith("local_3ds_")) safeId.removePrefix("local_3ds_") else safeId
        val decodedTitle = runCatching {
            String(android.util.Base64.decode(gameId, android.util.Base64.DEFAULT))
                .substringAfterLast('/')
                .substringBeforeLast('.')
        }.getOrNull()?.trim()

        val identifiers = listOfNotNull(gameTitle, decodedTitle, safeId, altId, gameId).distinct()
        val candidateTids = linkedSetOf<String>()
        val romTid = read3dsTitleIdFromRom(romFile)
        if (romTid != null) candidateTids.add(romTid)
        candidateTids.addAll(identifiers.flatMap { get3dsFullTitleIds(it, romFile) })

        val keywords = identifiers.flatMap { get3dsTextureKeywords(it) }.map { it.lowercase() }.distinct()

        val searchRoots = linkedSetOf<File>()
        for (root in getStorageRoots()) {
            listOf(
                "Download/mods", "Download/Mods", "Download/3ds/mods", "Download/3DS/mods",
                "Download", "mods", "Mods", "3ds/mods", "3DS/mods", "Vantafyn/mods",
                "RetroArch/saves/Citra/load/mods", "citra-emu/load/mods"
            ).forEach { rel ->
                val f = File(root, rel)
                if (f.exists() && f.isDirectory) searchRoots.add(f)
            }
        }
        val appModDir = File(context.filesDir, "mods")
        if (appModDir.exists() && appModDir.isDirectory) searchRoots.add(appModDir)

        val foundFolders = mutableListOf<File>()

        fun isTextureFile(f: File): Boolean {
            if (!f.isFile) return false
            val n = f.name.lowercase()
            return n.startsWith("tex1_") || n.endsWith(".png") || n.endsWith(".dds") || n.endsWith(".ktx")
        }

        fun findTextureContainers(dir: File, depth: Int = 0) {
            if (depth > 6 || isReShadeDir(dir)) return

            // If this directory is a RomFS/ExeFS code mod, do not treat it directly as a texture mod,
            // but inspect if it has a textures subdirectory inside it
            if (is3dsModDirectory(dir)) {
                val texSub = File(dir, "textures")
                if (texSub.exists() && texSub.isDirectory) {
                    findTextureContainers(texSub, depth + 1)
                }
                return
            }

            // If this directory directly contains texture files, it is a texture source
            val directTextures = dir.listFiles()?.any { isTextureFile(it) } == true
            if (directTextures) {
                foundFolders.add(dir)
                return
            }

            // Check if this directory is a top-level mod folder with categorical subfolders (e.g. Gen 1, Gen 2, etc.)
            val subdirs = dir.listFiles()?.filter { it.isDirectory && !isReShadeDir(it) } ?: emptyList()
            if (subdirs.isNotEmpty()) {
                val allSubdirsAreCategories = subdirs.all { sub ->
                    val sName = sub.name.lowercase()
                    sName.startsWith("gen") || sName.startsWith("form") || sName.startsWith("mega") ||
                    sName.startsWith("ride") || sName.startsWith("ui") || sName.startsWith("icon") ||
                    sName.startsWith("item") || sName.startsWith("texture") || sName.startsWith("char") ||
                    sName.startsWith("map") || sName.startsWith("env") || sName.startsWith("0004") ||
                    sName.startsWith("sprite") || sName.startsWith("poke") || sName.startsWith("pkmn") ||
                    sName.startsWith("hud") || sName.startsWith("battle") || sName.startsWith("menu") ||
                    sName.startsWith("bag") || sName.startsWith("dialogue") || sName.startsWith("pc")
                }
                val hasTexturesInSubtree = subdirs.any { sub ->
                    sub.walkTopDown().maxDepth(3).any { isTextureFile(it) }
                }
                if (allSubdirsAreCategories && hasTexturesInSubtree) {
                    foundFolders.add(dir)
                    return
                }

                for (sub in subdirs) {
                    findTextureContainers(sub, depth + 1)
                }
            }
        }

        for (root in searchRoots) {
            val children = root.listFiles()?.filter { it.isDirectory } ?: continue
            for (child in children) {
                val childName = child.name.lowercase()
                val isMatch = candidateTids.any { tid ->
                    val fullTid = tid.lowercase()
                    val shortTid = if (tid.length == 16) tid.substring(8).lowercase() else fullTid
                    childName == fullTid || childName == shortTid || childName.contains(fullTid)
                } || matchesKeywords(childName, keywords)

                if (isMatch) {
                    findTextureContainers(child, 0)
                }
            }
        }

        return foundFolders.distinctBy { it.canonicalPath }
    }

    fun update3dsAssets(
        gameId: String,
        gameTitle: String? = null,
        romFile: File? = null,
        customTexturesEnabled: Boolean = true,
        mods3dsEnabled: Boolean = true,
    ): Pair<Boolean, Boolean> {
        val identifiers = listOfNotNull(gameTitle, gameId).distinct()
        val allTids = (listOfNotNull(read3dsTitleIdFromRom(romFile)) +
                identifiers.flatMap { get3dsFullTitleIds(it, romFile) }).distinct()

        val textureBases = listOf(
            File(nativeCoreManager.getSaveDirectory(), "Citra/load/textures"),
            File(nativeCoreManager.getSaveDirectory(), "citra/load/textures"),
            File(nativeCoreManager.getSaveDirectory(), "Azahar/load/textures"),
            File(nativeCoreManager.getSaveDirectory(), "azahar/load/textures"),
            File(nativeCoreManager.getSystemDirectory(), "citra/load/textures"),
            File(nativeCoreManager.getSystemDirectory(), "Citra/load/textures"),
            File(nativeCoreManager.getSystemDirectory(), "azahar/load/textures"),
            File(nativeCoreManager.getSystemDirectory(), "Azahar/load/textures"),
            File(Environment.getExternalStorageDirectory(), "citra-emu/load/textures"),
            File(Environment.getExternalStorageDirectory(), "RetroArch/saves/Citra/load/textures"),
            File("/sdcard/citra-emu/load/textures"),
            File("/sdcard/RetroArch/saves/Citra/load/textures"),
        )
        val modBases = listOf(
            File(nativeCoreManager.getSaveDirectory(), "Citra/load/mods"),
            File(nativeCoreManager.getSaveDirectory(), "citra/load/mods"),
            File(nativeCoreManager.getSaveDirectory(), "Azahar/load/mods"),
            File(nativeCoreManager.getSaveDirectory(), "azahar/load/mods"),
            File(nativeCoreManager.getSystemDirectory(), "citra/load/mods"),
            File(nativeCoreManager.getSystemDirectory(), "Citra/load/mods"),
            File(nativeCoreManager.getSystemDirectory(), "azahar/load/mods"),
            File(nativeCoreManager.getSystemDirectory(), "Azahar/load/mods"),
            File(Environment.getExternalStorageDirectory(), "citra-emu/load/mods"),
            File(Environment.getExternalStorageDirectory(), "RetroArch/saves/Citra/load/mods"),
            File("/sdcard/citra-emu/load/mods"),
            File("/sdcard/RetroArch/saves/Citra/load/mods"),
        )

        // 1. Resolve Code / RomFS Mod
        val codeMod = if (mods3dsEnabled) find3dsMod(gameId, gameTitle, romFile) else null
        var hasCodeMod = false
        if (codeMod != null) {
            val modTids = (allTids + listOf(codeMod.titleId)).distinct()
            for (base in modBases) {
                base.mkdirs()
                for (tid in modTids) {
                    val link = File(base, tid)
                    try {
                        if (java.nio.file.Files.isSymbolicLink(link.toPath())) {
                            java.nio.file.Files.delete(link.toPath())
                        } else if (link.exists()) {
                            link.deleteRecursively()
                        }
                        android.system.Os.symlink(codeMod.modDir.absolutePath, link.absolutePath)
                        hasCodeMod = true
                        android.util.Log.i("GameStorageManager", "Linked 3DS code mod ${link.absolutePath} -> ${codeMod.modDir.absolutePath}")
                    } catch (e: Exception) {
                        android.util.Log.w("GameStorageManager", "Could not symlink code mod ${link.absolutePath}: ${e.message}")
                    }
                }
            }
        } else {
            // Remove code mods
            for (base in modBases) {
                for (tid in allTids) {
                    val link = File(base, tid)
                    try {
                        if (java.nio.file.Files.isSymbolicLink(link.toPath())) {
                            java.nio.file.Files.delete(link.toPath())
                        } else if (link.exists()) {
                            link.deleteRecursively()
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 2. Resolve Texture Sources
        val packDir = if (customTexturesEnabled) find3dsCustomTexturesDirectory(gameId, gameTitle, romFile) else null
        val modTextureDirs = if (mods3dsEnabled) {
            val dirs = find3dsModTextureDirectories(gameId, gameTitle, romFile).toMutableList()
            if (codeMod?.textureDir != null && codeMod.textureDir.exists() && dirs.none { it.canonicalPath == codeMod.textureDir.canonicalPath }) {
                dirs.add(codeMod.textureDir)
            }
            dirs
        } else emptyList()

        val textureSources = mutableMapOf<String, File>()
        if (packDir != null && packDir.exists()) {
            textureSources["pack_main"] = packDir
        }
        for ((index, modDir) in modTextureDirs.withIndex()) {
            val safeName = "mod_" + modDir.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val key = if (textureSources.containsKey(safeName)) "${safeName}_$index" else safeName
            textureSources[key] = modDir
        }

        val hasTextures = textureSources.containsKey("pack_main")
        val hasModTextures = modTextureDirs.isNotEmpty()
        val hasAnyTextures = textureSources.isNotEmpty()

        for (base in textureBases) {
            for (tid in allTids) {
                val tidDir = File(base, tid)
                if (!hasAnyTextures) {
                    try {
                        if (java.nio.file.Files.isSymbolicLink(tidDir.toPath())) {
                            java.nio.file.Files.delete(tidDir.toPath())
                        } else if (tidDir.exists()) {
                            tidDir.deleteRecursively()
                        }
                    } catch (_: Exception) {}
                    continue
                }

                // If tidDir was previously a single symlink, remove it so we can create a directory
                if (java.nio.file.Files.isSymbolicLink(tidDir.toPath())) {
                    try {
                        java.nio.file.Files.delete(tidDir.toPath())
                    } catch (e: Exception) {
                        android.util.Log.w("GameStorageManager", "Could not remove old symlink $tidDir: ${e.message}")
                    }
                }
                tidDir.mkdirs()

                // Clean up any old symlinks inside tidDir that are no longer active
                val children = tidDir.listFiles() ?: emptyArray()
                for (child in children) {
                    if (java.nio.file.Files.isSymbolicLink(child.toPath())) {
                        if (!textureSources.containsKey(child.name)) {
                            try {
                                java.nio.file.Files.delete(child.toPath())
                                android.util.Log.i("GameStorageManager", "Removed inactive texture link: ${child.absolutePath}")
                            } catch (_: Exception) {}
                        }
                    }
                }

                // Create symlinks for active texture sources
                for ((linkName, srcFile) in textureSources) {
                    val subLink = File(tidDir, linkName)
                    try {
                        if (java.nio.file.Files.isSymbolicLink(subLink.toPath())) {
                            val currentTarget = java.nio.file.Files.readSymbolicLink(subLink.toPath()).toString()
                            if (currentTarget == srcFile.absolutePath) {
                                continue // Already correctly linked
                            }
                            java.nio.file.Files.delete(subLink.toPath())
                        } else if (subLink.exists()) {
                            subLink.deleteRecursively()
                        }
                        android.system.Os.symlink(srcFile.absolutePath, subLink.absolutePath)
                        android.util.Log.i("GameStorageManager", "Linked 3DS texture source: ${subLink.absolutePath} -> ${srcFile.absolutePath}")
                    } catch (e: Exception) {
                        android.util.Log.w("GameStorageManager", "Could not link texture source ${subLink.absolutePath} to ${srcFile.absolutePath}: ${e.message}")
                    }
                }
            }
        }

        android.util.Log.i("GameStorageManager", "3DS Assets updated for '$gameTitle': texturesEnabled=$hasTextures (${packDir?.name}), modsEnabled=${hasCodeMod || hasModTextures} (code=$hasCodeMod, textures=${modTextureDirs.size})")
        return Pair(hasTextures, hasCodeMod || hasModTextures)
    }

    fun setup3dsMods(gameId: String, gameTitle: String? = null, romFile: File? = null): Boolean {
        return update3dsAssets(gameId, gameTitle, romFile, customTexturesEnabled = true, mods3dsEnabled = true).second
    }

    fun remove3dsMods(gameId: String, gameTitle: String? = null, romFile: File? = null): Boolean {
        return update3dsAssets(gameId, gameTitle, romFile, customTexturesEnabled = true, mods3dsEnabled = false).second
    }

    fun find3dsSdmcSaveFile(identifiers: List<String>): File? {
        val titleIds = identifiers.flatMap { get3dsTitleIds(it) }.toSet()
        if (titleIds.isEmpty()) return null
        for (root in getCitraRoots()) {
            if (root.exists() && root.isDirectory) {
                for (tid in titleIds) {
                    val candidate = root.walkTopDown()
                        .maxDepth(6)
                        .filter { it.isDirectory && it.name.equals(tid, ignoreCase = true) }
                        .flatMap { it.walkTopDown().maxDepth(4).filter { f -> f.isFile && f.name.equals("main", ignoreCase = true) } }
                        .firstOrNull { it.length() > 0 }
                    if (candidate != null) return candidate
                }
            }
        }
        return null
    }

    fun get3dsSdmcSaveDestination(titleId: String): File {
        val nativeSaveDir = nativeCoreManager.getSaveDirectory()
        for (root in getCitraRoots()) {
            if (root.exists() && root.isDirectory) {
                val tidDir = root.walkTopDown()
                    .maxDepth(6)
                    .firstOrNull { it.isDirectory && it.name.equals(titleId, ignoreCase = true) }
                if (tidDir != null) {
                    val dataDir = File(tidDir, "data/00000001")
                    dataDir.mkdirs()
                    return File(dataDir, "main")
                }
            }
        }
        val defaultDir = File(
            nativeSaveDir,
            "Azahar/sdmc/Nintendo 3DS/00000000000000000000000000000000/00000000000000000000000000000000/title/00040000/$titleId/data/00000001"
        )
        defaultDir.mkdirs()
        return File(defaultDir, "main")
    }

    fun sync3dsSaveFromSdmc(gameId: String, gameTitle: String? = null): File? {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val altId = if (safeId.startsWith("local_3ds_")) safeId.removePrefix("local_3ds_") else safeId
        val decodedTitle = runCatching {
            String(android.util.Base64.decode(gameId, android.util.Base64.DEFAULT))
                .substringAfterLast('/')
                .substringBeforeLast('.')
        }.getOrNull()?.trim()
        val idsToTry = listOfNotNull(gameTitle, decodedTitle, safeId, altId, gameId).distinct()
        val sdmcFile = find3dsSdmcSaveFile(idsToTry) ?: return null
        if (!sdmcFile.exists() || sdmcFile.length() <= 0) return null
        val standard = getLocalSaveFile(gameId, GameSaveKind.Sram)
        runCatching {
            standard.parentFile?.mkdirs()
            sdmcFile.copyTo(standard, overwrite = true)
        }
        return standard
    }

    fun sync3dsSaveToSdmc(gameId: String, data: ByteArray, gameTitle: String? = null) {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val altId = if (safeId.startsWith("local_3ds_")) safeId.removePrefix("local_3ds_") else safeId
        val decodedTitle = runCatching {
            String(android.util.Base64.decode(gameId, android.util.Base64.DEFAULT))
                .substringAfterLast('/')
                .substringBeforeLast('.')
        }.getOrNull()?.trim()
        val idsToTry = listOfNotNull(gameTitle, decodedTitle, safeId, altId, gameId).distinct()
        val titleIds = idsToTry.flatMap { get3dsTitleIds(it) }.toSet()
        if (titleIds.isEmpty()) return
        for (tid in titleIds) {
            runCatching {
                val dest = get3dsSdmcSaveDestination(tid)
                dest.parentFile?.mkdirs()
                dest.writeBytes(data)
                android.util.Log.i("GameStorageManager", "Synced 3DS save (${data.size} bytes) to SDMC destination: ${dest.absolutePath}")
            }
        }
    }

    fun findExistingLocalSaveFile(gameId: String, kind: GameSaveKind, gameTitle: String? = null): File {
        val standard = getLocalSaveFile(gameId, kind)
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val altId = if (safeId.startsWith("local_3ds_")) safeId.removePrefix("local_3ds_") else safeId
        val decodedTitle = runCatching {
            String(android.util.Base64.decode(gameId, android.util.Base64.DEFAULT))
                .substringAfterLast('/')
                .substringBeforeLast('.')
        }.getOrNull()?.trim()

        val idsToTry = listOfNotNull(gameTitle, decodedTitle, safeId, altId, gameId, gameId.lowercase()).distinct()

        // 1. For battery SRAM, check if an emulated 3DS SDMC save file exists
        if (kind == GameSaveKind.Sram) {
            val sdmcFile = find3dsSdmcSaveFile(idsToTry)
            if (sdmcFile != null && sdmcFile.exists() && sdmcFile.length() > 0) {
                // If SDMC main is newer than standard SRAM or standard does not exist, sync SDMC -> standard
                if (!standard.exists() || sdmcFile.lastModified() > standard.lastModified()) {
                    runCatching {
                        standard.parentFile?.mkdirs()
                        sdmcFile.copyTo(standard, overwrite = true)
                    }
                }
                return standard.takeIf { it.exists() && it.length() > 0 } ?: sdmcFile
            }
        }

        if (standard.exists() && standard.length() > 0) return standard

        val nativeSaveDir = nativeCoreManager.getSaveDirectory()

        val altCandidates = mutableListOf(
            File(savesDir, "$safeId.sav"),
            File(savesDir, "$safeId.srm"),
            File(savesDir, "$safeId.dsv"),
            File(savesDir, "$safeId.bin"),
            File(savesDir, "$altId.sav"),
            File(savesDir, "$altId.srm"),
            File(savesDir, "$altId.dsv"),
            File(nativeSaveDir, "$safeId.sav"),
            File(nativeSaveDir, "$safeId.srm"),
            File(nativeSaveDir, "$safeId.dsv"),
            File(nativeSaveDir, "$altId.sav"),
            File(nativeSaveDir, "$altId.srm"),
            File(nativeSaveDir, "$altId.dsv"),
        )

        val searchDirs = mutableListOf(
            savesDir,
            nativeSaveDir,
            File("/sdcard/Download/Quick Share"),
            File("/sdcard/Download"),
            File("/storage/emulated/0/Download/Quick Share"),
            File("/storage/emulated/0/Download"),
            File("/sdcard/RetroArch/saves"),
            File("/storage/emulated/0/RetroArch/saves"),
        )
        try {
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)?.let { dl ->
                searchDirs.add(File(dl, "Quick Share"))
                searchDirs.add(dl)
            }
        } catch (_: Throwable) {}

        val extensions = listOf(kind.value, "sav", "srm", "dsv", "bin", "main")

        for (dir in searchDirs.distinct()) {
            if (!dir.exists() || !dir.isDirectory) continue
            for (id in idsToTry) {
                for (ext in extensions) {
                    val candidate = File(dir, "$id.$ext")
                    if (candidate.exists() && candidate.length() > 0L) {
                        if (!standard.exists() || standard.length() <= 0L) {
                            runCatching {
                                standard.parentFile?.mkdirs()
                                candidate.copyTo(standard, overwrite = true)
                            }
                        }
                        return standard.takeIf { it.exists() && it.length() > 0L } ?: candidate
                    }
                }
            }
        }

        if (!decodedTitle.isNullOrBlank()) {
            for (dir in searchDirs.distinct()) {
                if (!dir.exists() || !dir.isDirectory) continue
                for (ext in extensions) {
                    val candidate = File(dir, "$decodedTitle.$ext")
                    if (candidate.exists() && candidate.length() > 0L) {
                        if (!standard.exists() || standard.length() <= 0L) {
                            runCatching {
                                standard.parentFile?.mkdirs()
                                candidate.copyTo(standard, overwrite = true)
                            }
                        }
                        return standard.takeIf { it.exists() && it.length() > 0L } ?: candidate
                    }
                }
            }
        }

        // Check alongside ROM in local 3DS directory using the game's actual ROM base name
        val local3dsDir = getLocal3dsDirectory()
        if (local3dsDir.exists() && local3dsDir.isDirectory) {
            altCandidates.add(File(local3dsDir, "$altId.sav"))
            altCandidates.add(File(local3dsDir, "$altId.srm"))
            altCandidates.add(File(local3dsDir, "$altId.main"))
        }

        for (alt in altCandidates) {
            if (alt.exists() && alt.length() > 0) return alt
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

    fun getLocal3dsDirectories(): List<File> {
        val dirs = linkedSetOf<File>()
        val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
        val customPath = prefs.getString("local_3ds_rom_directory", null)?.takeIf { it.isNotBlank() }
        if (customPath != null) {
            val customDir = normalizeDirectoryPath(customPath)
            if (customDir.exists() && customDir.isDirectory) {
                dirs.add(customDir)
            }
        }

        val relPaths = listOf(
            "Roms/3DS", "Roms/3ds", "ROMs/3DS", "ROMs/3ds", "roms/3ds", "roms/3DS",
            "3ds", "3DS", "Download/3ds", "Download/3DS", "Download"
        )

        for (root in getStorageRoots()) {
            for (rel in relPaths) {
                val candidate = File(root, rel)
                if (candidate.exists() && candidate.isDirectory) {
                    dirs.add(candidate)
                }
            }
        }

        val appExtDir = File(context.getExternalFilesDir(null), "3ds")
        if (appExtDir.exists() && appExtDir.isDirectory) {
            dirs.add(appExtDir)
        }
        val appExtRomsDir = File(context.getExternalFilesDir(null), "roms/3ds")
        if (appExtRomsDir.exists() && appExtRomsDir.isDirectory) {
            dirs.add(appExtRomsDir)
        }
        val appFilesDir = File(context.filesDir, "3ds")
        if (appFilesDir.exists() && appFilesDir.isDirectory) {
            dirs.add(appFilesDir)
        }

        return dirs.toList()
    }

    fun getLocal3dsDirectory(): File {
        return getLocal3dsDirectories().firstOrNull { it.exists() && it.isDirectory }
            ?: File(context.getExternalFilesDir(null), "3ds").apply { if (!exists()) mkdirs() }
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
        val candidateDirs = linkedSetOf<File>()
        candidateDirs.addAll(getLocal3dsDirectories())

        val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
        val customPath = prefs.getString("local_3ds_rom_directory", null)?.takeIf { it.isNotBlank() }
        if (customPath != null) {
            candidateDirs.add(normalizeDirectoryPath(customPath))
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

            sync3dsSaveToSdmc(gameId, data)

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
        gameTitle: String? = null,
    ): SaveSyncInfo = withContext(Dispatchers.IO) {
        val localFile = findExistingLocalSaveFile(gameId, kind, gameTitle)
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
        gameTitle: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = findExistingLocalSaveFile(gameId, kind, gameTitle)
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
        gameTitle: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            backupSave(gameId, kind)
            val data = cloudData ?: gamesRepository.getCloudSave(session, gameId, kind).getOrThrow()
                ?: throw IllegalStateException("Cloud save not found on server.")
            val localFile = findExistingLocalSaveFile(gameId, kind, gameTitle)
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(data)
            val standard = getLocalSaveFile(gameId, kind)
            if (standard.absolutePath != localFile.absolutePath) {
                runCatching {
                    standard.parentFile?.mkdirs()
                    standard.writeBytes(data)
                }
            }
            if (kind == GameSaveKind.Sram) {
                sync3dsSaveToSdmc(gameId, data, gameTitle)
            }
            val externalCandidates = listOf(
                File("/sdcard/Download/Quick Share", "$gameId.${kind.value}"),
                File("/sdcard/Download/Quick Share", "${gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.${kind.value}"),
                File("/sdcard/Download", "$gameId.${kind.value}"),
                File("/sdcard/Download", "${gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.${kind.value}"),
            )
            for (extFile in externalCandidates) {
                if (extFile.exists()) {
                    runCatching { extFile.writeBytes(data) }
                }
            }
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
        gameTitle: String? = null,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val localFile = findExistingLocalSaveFile(gameId, kind, gameTitle)

        if (forceCloud && session != null) {
            val cloudEntry = gamesRepository.getCloudSaveWithMetadata(session, gameId, kind).getOrNull()
            if (cloudEntry != null && cloudEntry.data.isNotEmpty()) {
                backupSave(gameId, kind)
                localFile.parentFile?.mkdirs()
                localFile.writeBytes(cloudEntry.data)
                if (kind == GameSaveKind.Sram) {
                    sync3dsSaveToSdmc(gameId, cloudEntry.data, gameTitle)
                }
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
                if (kind == GameSaveKind.Sram) {
                    sync3dsSaveToSdmc(gameId, cloudEntry.data, gameTitle)
                }
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
        gameTitle: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = getLocalSaveFile(gameId, kind)
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(data)
            if (kind == GameSaveKind.Sram) {
                sync3dsSaveToSdmc(gameId, data, gameTitle)
            }
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

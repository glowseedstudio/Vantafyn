package dev.vantafyn.core.jellyfin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * High-speed ROM box art scraper and metadata resolver.
 *
 * Emulators and frontends index the Libretro Thumbnails database once per console platform,
 * allowing instant local matching of hundreds of ROMs in milliseconds without hammering remote
 * servers with thousands of sequential HTTP roundtrips.
 */
object GameBoxartScraper {

    data class PlatformInfo(
        val libretroName: String,
        val githubRepo: String,
    )

    data class IndexedEntry(
        val rawFilename: String,
        val tokens: Set<String>,
        val joinedTokens: String,
        val tokenCount: Int,
        val scoreBias: Int,
    )

    private val ALL_PLATFORMS = listOf(
        PlatformInfo("Nintendo - Game Boy Advance", "Nintendo_-_Game_Boy_Advance"),
        PlatformInfo("Nintendo - Super Nintendo Entertainment System", "Nintendo_-_Super_Nintendo_Entertainment_System"),
        PlatformInfo("Nintendo - Nintendo Entertainment System", "Nintendo_-_Nintendo_Entertainment_System"),
        PlatformInfo("Nintendo - Nintendo 64", "Nintendo_-_Nintendo_64"),
        PlatformInfo("Nintendo - Game Boy", "Nintendo_-_Game_Boy"),
        PlatformInfo("Nintendo - Game Boy Color", "Nintendo_-_Game_Boy_Color"),
        PlatformInfo("Nintendo - Nintendo DS", "Nintendo_-_Nintendo_DS"),
        PlatformInfo("Nintendo - Nintendo 3DS", "Nintendo_-_Nintendo_3DS"),
        PlatformInfo("Nintendo - Virtual Boy", "Nintendo_-_Virtual_Boy"),
        PlatformInfo("Sony - PlayStation", "Sony_-_PlayStation"),
        PlatformInfo("Sony - PlayStation Portable", "Sony_-_PlayStation_Portable"),
        PlatformInfo("Sega - Mega Drive - Genesis", "Sega_-_Mega_Drive_-_Genesis"),
        PlatformInfo("Sega - Master System - Mark III", "Sega_-_Master_System_-_Mark_III"),
        PlatformInfo("Sega - Game Gear", "Sega_-_Game_Gear"),
        PlatformInfo("Atari - 2600", "Atari_-_2600"),
        PlatformInfo("Atari - 7800", "Atari_-_7800"),
        PlatformInfo("Atari - Lynx", "Atari_-_Lynx"),
        PlatformInfo("Bandai - WonderSwan", "Bandai_-_WonderSwan"),
        PlatformInfo("SNK - Neo Geo Pocket Color", "SNK_-_Neo_Geo_Pocket_Color"),
        PlatformInfo("NEC - PC Engine - TurboGrafx 16", "NEC_-_PC_Engine_-_TurboGrafx_16"),
        PlatformInfo("FBNeo - Arcade Games", "FBNeo_-_Arcade_Games"),
    )

    private val systemIndexCache = ConcurrentHashMap<String, List<IndexedEntry>>()

    /**
     * Maps standard emulator core identifiers and console abbreviations to canonical
     * Libretro directory names and GitHub repositories.
     */
    fun resolvePlatform(systemId: String): PlatformInfo? {
        val s = systemId.lowercase().trim().replace(" ", "").replace("-", "").replace("_", "")
        return when (s) {
            "gba", "gameboyadvance", "nintendogameboyadvance" -> PlatformInfo("Nintendo - Game Boy Advance", "Nintendo_-_Game_Boy_Advance")
            "snes", "sfc", "supernintendo", "superfamicom", "nintendosupernintendoentertainmentsystem" -> PlatformInfo("Nintendo - Super Nintendo Entertainment System", "Nintendo_-_Super_Nintendo_Entertainment_System")
            "nes", "famicom", "nintendo", "nintendonintendoentertainmentsystem" -> PlatformInfo("Nintendo - Nintendo Entertainment System", "Nintendo_-_Nintendo_Entertainment_System")
            "n64", "nintendo64", "nintendonintendo64" -> PlatformInfo("Nintendo - Nintendo 64", "Nintendo_-_Nintendo_64")
            "gb", "gameboy", "nintendogameboy" -> PlatformInfo("Nintendo - Game Boy", "Nintendo_-_Game_Boy")
            "gbc", "gameboycolor", "nintendogameboycolor" -> PlatformInfo("Nintendo - Game Boy Color", "Nintendo_-_Game_Boy_Color")
            "nds", "ds", "nintendods", "nintendonintendods" -> PlatformInfo("Nintendo - Nintendo DS", "Nintendo_-_Nintendo_DS")
            "3ds", "n3ds", "nintendo3ds" -> PlatformInfo("Nintendo - Nintendo 3DS", "Nintendo_-_Nintendo_3DS")
            "vb", "virtualboy", "nintendovirtualboy" -> PlatformInfo("Nintendo - Virtual Boy", "Nintendo_-_Virtual_Boy")
            "psx", "ps1", "psone", "playstation", "sonyplaystation" -> PlatformInfo("Sony - PlayStation", "Sony_-_PlayStation")
            "psp", "playstationportable", "sonyplaystationportable" -> PlatformInfo("Sony - PlayStation Portable", "Sony_-_PlayStation_Portable")
            "segamd", "genesis", "megadrive", "segagenesis", "segamegadrivegenesis" -> PlatformInfo("Sega - Mega Drive - Genesis", "Sega_-_Mega_Drive_-_Genesis")
            "segams", "mastersystem", "sms", "segamastersystemmarkiii" -> PlatformInfo("Sega - Master System - Mark III", "Sega_-_Master_System_-_Mark_III")
            "segagg", "gamegear", "gg", "segagamegear" -> PlatformInfo("Sega - Game Gear", "Sega_-_Game_Gear")
            "atari2600" -> PlatformInfo("Atari - 2600", "Atari_-_2600")
            "atari7800" -> PlatformInfo("Atari - 7800", "Atari_-_7800")
            "lynx", "atarilynx" -> PlatformInfo("Atari - Lynx", "Atari_-_Lynx")
            "ws", "wonderswan", "bandaiwonderswan" -> PlatformInfo("Bandai - WonderSwan", "Bandai_-_WonderSwan")
            "ngp", "neogeopocket", "snkneogeopocketcolor" -> PlatformInfo("SNK - Neo Geo Pocket Color", "SNK_-_Neo_Geo_Pocket_Color")
            "pce", "pcengine", "turbografx16", "turbografx", "necpceangineturbografx16", "necpceineturbografx16" -> PlatformInfo("NEC - PC Engine - TurboGrafx 16", "NEC_-_PC_Engine_-_TurboGrafx_16")
            "arcade", "mame", "fbneo", "fbneoarcadegames" -> PlatformInfo("FBNeo - Arcade Games", "FBNeo_-_Arcade_Games")
            else -> {
                ALL_PLATFORMS.firstOrNull {
                    it.libretroName.equals(systemId, ignoreCase = true) ||
                        it.githubRepo.equals(systemId, ignoreCase = true) ||
                        it.libretroName.replace(" ", "").replace("-", "").equals(s, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Cleans an input ROM title or filename into normalized search tokens.
     */
    fun cleanTokens(input: String): List<String> {
        var s = input
        val dot = s.lastIndexOf('.')
        if (dot > 0) s = s.substring(0, dot)

        // Strip scene release prefixes (e.g. "0001 - ")
        s = s.replace(Regex("^\\d{3,5}\\s*-\\s*"), "")

        // Strip GoodTools / dump tags like [!], [b1], (USA), (Rev 1)
        s = s.replace(Regex("\\[[^\\]]*\\]"), " ")
        s = s.replace(Regex("\\([^)]*\\)"), " ")

        // Handle inverted articles: "Legend of Zelda, The" -> "Legend of Zelda"
        s = s.replace(Regex(",\\s*(the|a|an)\\b", RegexOption.IGNORE_CASE), "")

        // Normalize separators
        s = s.replace(Regex("[_\\-&~:,'/\\\\]+"), " ")

        val words = Regex("[a-zA-Z0-9]+").findAll(s.lowercase()).map { it.value }.toList()
        val stopWords = setOf("the", "a", "an", "version", "edition", "game")
        val filtered = words.filter { it !in stopWords }
        return if (filtered.isNotEmpty()) filtered else words
    }

    /**
     * Pre-computes preference score for a canonical thumbnail filename.
     * Higher score indicates preferred standard release over demos/betas.
     */
    fun scoreFilename(filename: String): Int {
        var score = 100
        val lower = filename.lowercase()

        // Strongly deprioritize demos, betas, kiosks, and hacks
        if (lower.contains("(demo)") || lower.contains("(kiosk)") || lower.contains("(sample)") ||
            lower.contains("(beta)") || lower.contains("(proto)") || lower.contains("(hack)") ||
            lower.contains("(alternate)") || lower.contains("(alt)") || lower.contains("(bonus)")
        ) {
            score -= 60
        }

        // Deprioritize virtual console / ports if original exists
        if (lower.contains("(virtual console)") || lower.contains("(collection)") ||
            lower.contains("(switch)") || lower.contains("(aftermarket)") || lower.contains("(wii)")
        ) {
            score -= 20
        }

        // Region scoring preferences: USA/World > Europe > Japan
        when {
            lower.contains("(usa, europe)") || lower.contains("(world)") -> score += 30
            lower.contains("(usa)") -> score += 25
            lower.contains("(europe)") -> score += 20
            lower.contains("(japan)") -> score += 5
        }

        // Fewer extra parenthetical tags is cleaner
        val tagCount = Regex("\\([^)]*\\)").findAll(filename).count()
        score -= tagCount * 2

        return score
    }

    /**
     * Fetches and caches the system box art catalog from Libretro in a single network request.
     */
    suspend fun getSystemIndex(platform: PlatformInfo): List<IndexedEntry> = withContext(Dispatchers.IO) {
        systemIndexCache[platform.githubRepo]?.let { return@withContext it }

        val entries = fetchSystemDirectoryListing(platform)
        if (entries.isNotEmpty()) {
            systemIndexCache[platform.githubRepo] = entries
        }
        entries
    }

    private fun fetchSystemDirectoryListing(platform: PlatformInfo): List<IndexedEntry> {
        val encodedPlatform = URLEncoder.encode(platform.libretroName, "UTF-8").replace("+", "%20")
        val urlStr = "https://thumbnails.libretro.com/$encodedPlatform/Named_Boxarts/"

        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Vantafyn Emulator Scraper)")
            }

            if (conn.responseCode != 200) {
                conn.disconnect()
                return emptyList()
            }

            val html = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val filenames = mutableListOf<String>()
            val matcher = Regex("href=\"([^\"]+\\.png)\"", RegexOption.IGNORE_CASE)
            for (match in matcher.findAll(html)) {
                val rawHref = match.groupValues[1]
                val decoded = runCatching { URLDecoder.decode(rawHref, "UTF-8") }.getOrDefault(rawHref)
                if (decoded.endsWith(".png", ignoreCase = true)) {
                    filenames.add(decoded)
                }
            }

            filenames.map { fn ->
                val tokens = cleanTokens(fn).toSet()
                IndexedEntry(
                    rawFilename = fn,
                    tokens = tokens,
                    joinedTokens = tokens.joinToString(""),
                    tokenCount = tokens.size,
                    scoreBias = scoreFilename(fn),
                )
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Matches a ROM title or filename against the system's indexed box art catalog.
     * Returns the best matching canonical filename or null if unmatchable.
     */
    fun matchGame(gameTitle: String, filename: String, entries: List<IndexedEntry>): String? {
        if (entries.isEmpty()) return null

        val titleTokens = cleanTokens(gameTitle)
        val fileTokens = cleanTokens(filename)

        val titleJoined = titleTokens.joinToString("")
        val fileJoined = fileTokens.joinToString("")

        // 1. Exact joined token match from title or filename
        val exactCandidates = entries.filter {
            it.joinedTokens == titleJoined || (fileJoined.isNotEmpty() && it.joinedTokens == fileJoined)
        }
        if (exactCandidates.isNotEmpty()) {
            return exactCandidates.maxByOrNull { it.scoreBias }?.rawFilename
        }

        // 2. Token subset match: title tokens are entirely contained in the entry
        val titleSet = titleTokens.toSet()
        if (titleSet.isNotEmpty()) {
            val subsetCandidates = entries.filter { titleSet.all { t -> it.tokens.contains(t) } }
            if (subsetCandidates.isNotEmpty()) {
                return subsetCandidates.maxWithOrNull(
                    compareBy<IndexedEntry> { it.scoreBias }
                        .thenByDescending { -Math.abs(it.tokenCount - titleTokens.size) }
                )?.rawFilename
            }
        }

        // 3. Fallback: file tokens subset match
        val fileSet = fileTokens.toSet()
        if (fileSet.isNotEmpty()) {
            val subsetFileCandidates = entries.filter { fileSet.all { t -> it.tokens.contains(t) } }
            if (subsetFileCandidates.isNotEmpty()) {
                return subsetFileCandidates.maxWithOrNull(
                    compareBy<IndexedEntry> { it.scoreBias }
                        .thenByDescending { -Math.abs(it.tokenCount - fileTokens.size) }
                )?.rawFilename
            }
        }

        // 4. Overlap match: at least 75% of title tokens match
        if (titleTokens.size >= 2) {
            val overlapCandidates = entries.mapNotNull { entry ->
                val matchingCount = titleTokens.count { entry.tokens.contains(it) }
                val ratio = matchingCount.toFloat() / titleTokens.size
                if (ratio >= 0.75f) Pair(entry, ratio) else null
            }
            if (overlapCandidates.isNotEmpty()) {
                return overlapCandidates.maxWithOrNull(
                    compareBy<Pair<IndexedEntry, Float>> { it.second }
                        .thenBy { it.first.scoreBias }
                )?.first?.rawFilename
            }
        }

        return null
    }

    fun searchCandidates(query: String, entries: List<IndexedEntry>, limit: Int = 24): List<IndexedEntry> {
        if (entries.isEmpty()) return emptyList()
        val queryTokens = cleanTokens(query)
        if (queryTokens.isEmpty()) return entries.sortedByDescending { it.scoreBias }.take(limit)
        val querySet = queryTokens.toSet()
        val queryJoined = queryTokens.joinToString("")

        return entries.mapNotNull { entry ->
            val overlap = querySet.count { entry.tokens.contains(it) }
            val ratio = overlap.toFloat() / querySet.size.coerceAtLeast(1)
            val joinedBonus = when {
                entry.joinedTokens == queryJoined -> 120f
                entry.joinedTokens.contains(queryJoined) || queryJoined.contains(entry.joinedTokens) -> 45f
                else -> 0f
            }
            val score = (ratio * 100f) + joinedBonus + entry.scoreBias
            if (overlap > 0 || joinedBonus > 0f) entry to score else null
        }
            .sortedWith(
                compareByDescending<Pair<IndexedEntry, Float>> { it.second }
                    .thenByDescending { it.first.scoreBias }
                    .thenBy { it.first.rawFilename.length }
            )
            .map { it.first }
            .take(limit)
    }

    /**
     * Builds a fast global CDN URL for the matched box art filename.
     */
    fun buildCdnUrl(platform: PlatformInfo, filename: String): String {
        val encodedFilename = URLEncoder.encode(filename, "UTF-8")
            .replace("+", "%20")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%2C", ",")
        if (platform.githubRepo.contains("3DS", ignoreCase = true)) {
            return "https://raw.githubusercontent.com/libretro-thumbnails/${platform.githubRepo}/master/Named_Boxarts/$encodedFilename"
        }
        return "https://cdn.jsdelivr.net/gh/libretro-thumbnails/${platform.githubRepo}@master/Named_Boxarts/$encodedFilename"
    }

    fun resolve3dsBoxartUrl(fileName: String): String {
        val nameWithoutExt = fileName.substringBeforeLast('.')
        val encodedFilename = URLEncoder.encode("$nameWithoutExt.png", "UTF-8")
            .replace("+", "%20")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%2C", ",")
        return "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Nintendo_3DS/master/Named_Boxarts/$encodedFilename"
    }

    /**
     * Converts slow Libretro Apache URLs to ultra-fast CDN URLs.
     */
    fun convertToCdnUrl(originalUrl: String?): String? {
        if (originalUrl.isNullOrBlank()) return null
        if (!originalUrl.startsWith("https://thumbnails.libretro.com/")) return originalUrl

        val path = originalUrl.removePrefix("https://thumbnails.libretro.com/")
        val parts = path.split("/Named_Boxarts/")
        if (parts.size != 2) return originalUrl

        val platformPart = URLDecoder.decode(parts[0], "UTF-8")
        val filenamePart = parts[1] // Keep encoded

        val platform = resolvePlatform(platformPart)
        return if (platform != null) {
            buildCdnUrl(platform, URLDecoder.decode(filenamePart, "UTF-8"))
        } else {
            originalUrl
        }
    }
}

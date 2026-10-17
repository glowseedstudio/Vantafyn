package dev.vantafyn.feature.home.games.screensaver

import android.content.Context
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GamePlayTracker
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.resolveSystemDisplayName

data class ScreensaverSlide(
    val game: GameSummary,
    val systemDisplayName: String,
    val pictureUrls: List<String>,
    val logoUrl: String? = null,
) {
    val landscapeArtUrl: String
        get() = pictureUrls.firstOrNull().orEmpty()
}

object GameScreensaverResolver {

    /**
     * Resolves the best available landscape in-game art URL (snaps) for a given game.
     */
    fun resolveLandscapeArt(context: Context, game: GameSummary): String? {
        val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)

        // 1. Explicit local snap or landscape art override
        val explicitLandscape = prefs.getString("snap_${game.id}", null)
            ?: prefs.getString("landscape_art_${game.id}", null)
        if (!explicitLandscape.isNullOrBlank()) {
            return GameBoxartScraper.convertToCdnUrl(explicitLandscape)
        }

        // 2. 3DS platform check
        val is3ds = game.systemId.contains("3ds", ignoreCase = true) ||
            game.filename.endsWith(".3ds", ignoreCase = true) ||
            game.filename.endsWith(".cia", ignoreCase = true) ||
            game.filename.endsWith(".cci", ignoreCase = true)
        if (is3ds) {
            return GameBoxartScraper.resolve3dsSnapUrl(game.filename)
        }

        // 3. Convert scraped boxart URL (/Named_Boxarts/ -> /Named_Snaps/)
        val boxart = prefs.getString("boxart_${game.id}", null) ?: game.boxartUrl
        val snapUrl = GameBoxartScraper.convertBoxartUrlToSnapUrl(boxart)
        if (!snapUrl.isNullOrBlank()) {
            return GameBoxartScraper.convertToCdnUrl(snapUrl)
        }

        // 4. Platform candidate heuristic via standard Libretro naming
        val platform = GameBoxartScraper.resolvePlatform(game.systemId)
        if (platform != null) {
            val cleanBase = java.io.File(game.filename.ifBlank { game.cleanTitle.ifBlank { game.title } }).nameWithoutExtension
            if (cleanBase.isNotBlank()) {
                return GameBoxartScraper.buildSnapUrl(platform, "$cleanBase.png")
            }
        }

        return null
    }

    /**
     * Resolves the best available title screen art URL (titles) for a given game.
     */
    fun resolveTitleArt(context: Context, game: GameSummary): String? {
        val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)

        // 1. Explicit local title art override
        val explicitTitle = prefs.getString("title_art_${game.id}", null)
        if (!explicitTitle.isNullOrBlank()) {
            return GameBoxartScraper.convertToCdnUrl(explicitTitle)
        }

        // 2. 3DS platform check
        val is3ds = game.systemId.contains("3ds", ignoreCase = true) ||
            game.filename.endsWith(".3ds", ignoreCase = true) ||
            game.filename.endsWith(".cia", ignoreCase = true) ||
            game.filename.endsWith(".cci", ignoreCase = true)
        if (is3ds) {
            return GameBoxartScraper.resolve3dsTitleUrl(game.filename)
        }

        // 3. Convert scraped boxart URL (/Named_Boxarts/ -> /Named_Titles/)
        val boxart = prefs.getString("boxart_${game.id}", null) ?: game.boxartUrl
        val titleUrl = GameBoxartScraper.convertBoxartUrlToTitleUrl(boxart)
        if (!titleUrl.isNullOrBlank()) {
            return GameBoxartScraper.convertToCdnUrl(titleUrl)
        }

        // 4. Platform candidate heuristic via standard Libretro naming
        val platform = GameBoxartScraper.resolvePlatform(game.systemId)
        if (platform != null) {
            val cleanBase = java.io.File(game.filename.ifBlank { game.cleanTitle.ifBlank { game.title } }).nameWithoutExtension
            if (cleanBase.isNotBlank()) {
                return GameBoxartScraper.buildTitleUrl(platform, "$cleanBase.png")
            }
        }

        return null
    }

    /**
     * Resolves up to 2 distinct landscape pictures for a given game (in-game gameplay snap & title screen).
     */
    fun resolveGamePictures(context: Context, game: GameSummary): List<String> {
        val snap = resolveLandscapeArt(context, game)
        val title = resolveTitleArt(context, game)

        val pictures = mutableListOf<String>()
        if (!snap.isNullOrBlank()) {
            pictures.add(snap)
        }
        if (!title.isNullOrBlank() && title != snap) {
            pictures.add(title)
        }
        val boxart = game.boxartUrl
        if (pictures.isEmpty() && !boxart.isNullOrBlank()) {
            pictures.add(boxart)
        }
        return pictures
    }

    /**
     * Resolves the best available 3D/clear logo URL for a given game.
     */
    fun resolveGameLogo(context: Context, game: GameSummary): String? {
        val prefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)

        // 1. Explicit local logo override
        val explicitLogo = prefs.getString("logo_${game.id}", null)
        if (!explicitLogo.isNullOrBlank()) {
            return GameBoxartScraper.convertToCdnUrl(explicitLogo)
        }

        // 2. 3DS platform check
        val is3ds = game.systemId.contains("3ds", ignoreCase = true) ||
            game.filename.endsWith(".3ds", ignoreCase = true) ||
            game.filename.endsWith(".cia", ignoreCase = true) ||
            game.filename.endsWith(".cci", ignoreCase = true)
        if (is3ds) {
            return GameBoxartScraper.resolve3dsLogoUrl(game.filename)
        }

        // 3. Convert scraped boxart URL (/Named_Boxarts/ -> /Named_Logos/)
        val boxart = prefs.getString("boxart_${game.id}", null) ?: game.boxartUrl
        val logoUrl = GameBoxartScraper.convertBoxartUrlToLogoUrl(boxart)
        if (!logoUrl.isNullOrBlank()) {
            return GameBoxartScraper.convertToCdnUrl(logoUrl)
        }

        // 4. Platform candidate heuristic via standard Libretro naming
        val platform = GameBoxartScraper.resolvePlatform(game.systemId)
        if (platform != null) {
            val cleanBase = java.io.File(game.filename.ifBlank { game.cleanTitle.ifBlank { game.title } }).nameWithoutExtension
            if (cleanBase.isNotBlank()) {
                return GameBoxartScraper.buildLogoUrl(platform, "$cleanBase.png")
            }
        }

        return null
    }

    /**
     * Builds a list of slides for available games, favoring games and series the player is actively playing.
     */
    fun buildSlides(
        context: Context,
        games: List<GameSummary>,
        systems: List<GameSystem>,
    ): List<ScreensaverSlide> {
        val systemMap = systems.associateBy { it.id.lowercase().trim() }

        // Track recently played games and active franchises
        val tracker = GamePlayTracker(context)
        val recentGames = tracker.getRecentGames(40)
        val playedGameIds = recentGames.map { it.id }.toSet()
        val playedTitles = recentGames.map { it.title.lowercase() }

        val franchiseKeywords = mutableSetOf<String>()
        val genericWords = setOf(
            "the", "a", "an", "version", "edition", "game", "super", "advance",
            "3d", "hd", "pocket", "deluxe", "remastered", "collection", "world",
            "land", "island", "party", "kart",
        )
        for (title in playedTitles) {
            val tokens = GameBoxartScraper.cleanTokens(title).filter { it.length > 2 && it !in genericWords }
            franchiseKeywords.addAll(tokens)
        }

        val allSlides = mutableListOf<ScreensaverSlide>()
        for (game in games) {
            val pictures = resolveGamePictures(context, game)
            if (pictures.isEmpty()) continue

            val logoUrl = resolveGameLogo(context, game)
            val system = systemMap[game.systemId.lowercase().trim()]
            val displayName = system?.displayName ?: resolveSystemDisplayName(game.systemId, "")

            allSlides.add(
                ScreensaverSlide(
                    game = game,
                    systemDisplayName = displayName,
                    pictureUrls = pictures,
                    logoUrl = logoUrl,
                )
            )
        }

        if (allSlides.isEmpty()) return emptyList()

        // Calculate preference scores to favor played titles and series
        fun calculateScore(slide: ScreensaverSlide): Int {
            var score = 0
            if (playedGameIds.contains(slide.game.id)) {
                val index = recentGames.indexOfFirst { it.id == slide.game.id }
                score += 2000 - (index * 25).coerceAtLeast(0)
            }
            val titleTokens = GameBoxartScraper.cleanTokens(slide.game.title).toSet()
            val franchiseMatches = titleTokens.count { franchiseKeywords.contains(it) }
            if (franchiseMatches > 0) {
                score += 500 * franchiseMatches
            }
            return score
        }

        val scored = allSlides.map { slide -> slide to calculateScore(slide) }
        val favored = scored.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
        val unplayed = scored.filter { it.second == 0 }.shuffled().map { it.first }

        return if (favored.isNotEmpty()) {
            // Favored games and series come first, followed by a subset of other titles
            favored + unplayed.take(15)
        } else {
            allSlides.shuffled()
        }
    }
}

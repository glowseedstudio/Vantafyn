package dev.vantafyn.feature.home.games.pokemon

import android.content.Context
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.JellyfinSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Scrapes and persistently caches the official Pokémon logo to local disk so that
 * the Pokémon Vault hero card always displays a crisp, authentic logo when no saves are active.
 */
object PokemonLogoScraper {

    private const val LOGO_FILE_NAME = "pokemon_vault_scraped_logo.png"

    private val SCRAPE_TARGET_URLS = listOf(
        // High-resolution official International Pokémon logo (500px PNG)
        "https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/International_Pok%C3%A9mon_logo.svg/500px-International_Pok%C3%A9mon_logo.svg.png",
        // Fallback 800px official logo
        "https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/International_Pok%C3%A9mon_logo.svg/800px-International_Pok%C3%A9mon_logo.svg.png",
    )

    fun getCachedLogoFile(context: Context): File? {
        val file = File(context.filesDir, LOGO_FILE_NAME)
        return if (file.exists() && file.length() > 500) file else null
    }

    suspend fun getOrScrapeLogo(
        context: Context,
        availableGames: List<GameSummary> = emptyList(),
        session: JellyfinSession? = null,
    ): Any? = withContext(Dispatchers.IO) {
        val existing = getCachedLogoFile(context)
        if (existing != null) return@withContext existing

        val targetFile = File(context.filesDir, LOGO_FILE_NAME)

        // 1. Try scraping from user's linked Jellyfin Pokémon games if available
        if (session != null && availableGames.isNotEmpty()) {
            val pkmGame = availableGames.firstOrNull {
                it.pokemon?.isPokemonGame == true || it.title.contains("pokemon", ignoreCase = true)
            }
            if (pkmGame != null) {
                val gameLogoUrl = "${session.server.url.trimEnd('/')}/Items/${pkmGame.id}/Images/Logo"
                if (downloadToFile(gameLogoUrl, targetFile, session.accessToken)) {
                    return@withContext targetFile
                }
            }
        }

        // 2. Scrape canonical Pokémon franchise logo with standard browser User-Agent
        for (url in SCRAPE_TARGET_URLS) {
            if (downloadToFile(url, targetFile, null)) {
                return@withContext targetFile
            }
        }

        // If file exists even partially, return it; otherwise return primary URL for Coil with custom headers
        if (targetFile.exists() && targetFile.length() > 500) {
            targetFile
        } else {
            SCRAPE_TARGET_URLS.first()
        }
    }

    private fun downloadToFile(urlStr: String, destination: File, authToken: String? = null): Boolean {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                if (!authToken.isNullOrBlank()) {
                    setRequestProperty("X-Emby-Token", authToken)
                    setRequestProperty("Authorization", "MediaBrowser Client=\"Vantafyn\", Device=\"Android\", DeviceId=\"device\", Version=\"1.0.0\", Token=\"$authToken\"")
                }
            }

            if (conn.responseCode in 200..299) {
                val tempFile = File(destination.parentFile, "${destination.name}.tmp")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                conn.disconnect()

                if (tempFile.exists() && tempFile.length() > 500) {
                    if (destination.exists()) destination.delete()
                    tempFile.renameTo(destination)
                    return true
                }
            } else {
                conn.disconnect()
            }
            false
        }.getOrDefault(false)
    }
}

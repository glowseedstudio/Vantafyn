package dev.vantafyn.core.jellyfin

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RecentGameRecord(
    val id: String,
    val title: String,
    val systemId: String,
    val filename: String,
    val sizeBytes: Long,
    val token: String,
    val extension: String,
    val boxartUrl: String?,
    val lastPlayedMs: Long,
    val playTimeMs: Long,
) {
    fun toGameSummary(): GameSummary = GameSummary(
        id = id,
        title = title,
        systemId = systemId,
        filename = filename,
        sizeBytes = sizeBytes,
        token = token,
        extension = extension,
        boxartUrl = boxartUrl,
    )
}

class GamePlayTracker(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getTotalPlayTimeMs(): Long = prefs.getLong(KEY_TOTAL_PLAYTIME_MS, 0L)

    fun getPlayTimeForGame(gameId: String): Long = prefs.getLong(KEY_GAME_PLAYTIME_PREFIX + gameId, 0L)

    fun getRecentGames(limit: Int = 10): List<RecentGameRecord> {
        val jsonString = prefs.getString(KEY_RECENT_GAMES_JSON, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<RecentGameRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentGameRecord(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        systemId = obj.optString("systemId", ""),
                        filename = obj.optString("filename", ""),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        token = obj.optString("token", ""),
                        extension = obj.optString("extension", ""),
                        boxartUrl = if (obj.has("boxartUrl") && !obj.isNull("boxartUrl")) obj.getString("boxartUrl") else null,
                        lastPlayedMs = obj.optLong("lastPlayedMs", 0L),
                        playTimeMs = obj.optLong("playTimeMs", 0L),
                    ),
                )
            }
            list.take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordGameLaunched(game: GameDetail) {
        val now = System.currentTimeMillis()
        val currentRecent = getRecentGames(20).toMutableList()
        val existingIndex = currentRecent.indexOfFirst { it.id == game.id }
        val currentPlaytime = getPlayTimeForGame(game.id)

        val updatedRecord = RecentGameRecord(
            id = game.id,
            title = game.cleanTitle.ifEmpty { game.title },
            systemId = game.systemId,
            filename = game.filename,
            sizeBytes = game.sizeBytes,
            token = game.token,
            extension = game.extension,
            boxartUrl = game.boxartUrl,
            lastPlayedMs = now,
            playTimeMs = currentPlaytime,
        )

        if (existingIndex >= 0) {
            currentRecent.removeAt(existingIndex)
        }
        currentRecent.add(0, updatedRecord)
        saveRecentGames(currentRecent.take(15))
    }

    fun recordPlaySession(game: GameDetail, sessionDurationMs: Long) {
        if (sessionDurationMs <= 0L) return
        val currentTotal = getTotalPlayTimeMs()
        val currentForGame = getPlayTimeForGame(game.id)
        val newTotal = currentTotal + sessionDurationMs
        val newForGame = currentForGame + sessionDurationMs

        prefs.edit()
            .putLong(KEY_TOTAL_PLAYTIME_MS, newTotal)
            .putLong(KEY_GAME_PLAYTIME_PREFIX + game.id, newForGame)
            .apply()

        // Update playtime in recent records as well
        val currentRecent = getRecentGames(20).toMutableList()
        val existingIndex = currentRecent.indexOfFirst { it.id == game.id }
        if (existingIndex >= 0) {
            val old = currentRecent[existingIndex]
            currentRecent[existingIndex] = old.copy(playTimeMs = newForGame, lastPlayedMs = System.currentTimeMillis())
            saveRecentGames(currentRecent)
        }
    }

    private fun stripSensitiveQueryParams(url: String?): String? {
        if (url.isNullOrBlank()) return url
        return url.replace(Regex("([?&])(api_key|ApiKey|X-Emby-Token|token)=[^&]+(&)?", RegexOption.IGNORE_CASE)) { matchResult ->
            if (matchResult.groups[1]?.value == "?" && matchResult.groups[3]?.value == "&") "?" else ""
        }.trimEnd('?', '&')
    }

    private fun saveRecentGames(records: List<RecentGameRecord>) {
        try {
            val jsonArray = JSONArray()
            for (item in records) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("systemId", item.systemId)
                    put("filename", item.filename)
                    put("sizeBytes", item.sizeBytes)
                    put("token", item.token)
                    put("extension", item.extension)
                    put("boxartUrl", stripSensitiveQueryParams(item.boxartUrl))
                    put("lastPlayedMs", item.lastPlayedMs)
                    put("playTimeMs", item.playTimeMs)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_RECENT_GAMES_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Ignore error writing recent games
        }
    }

    companion object {
        private const val PREFS_NAME = "vantafyn_game_playtime"
        private const val KEY_TOTAL_PLAYTIME_MS = "total_playtime_ms"
        private const val KEY_GAME_PLAYTIME_PREFIX = "game_playtime_"
        private const val KEY_RECENT_GAMES_JSON = "recent_games_json"

        fun formatPlayTime(playTimeMs: Long): String {
            val totalSeconds = playTimeMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                totalSeconds > 10 -> "${totalSeconds}s"
                else -> "< 1m"
            }
        }
    }
}

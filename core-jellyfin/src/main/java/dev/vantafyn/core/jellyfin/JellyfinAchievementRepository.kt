package dev.vantafyn.core.jellyfin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID

class SdkJellyfinAchievementRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinAchievementRepository {

    override suspend fun checkAvailability(session: JellyfinSession): Boolean =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/summary")
                try {
                    conn.connectTimeout = 4_000
                    conn.readTimeout = 4_000
                    conn.responseCode in 200..299
                } finally {
                    conn.disconnect()
                }
            }.getOrDefault(false)
        }

    override suspend fun getSummary(session: JellyfinSession): JellyfinResult<JellyfinAchievementSummary> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/summary")
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Achievement summary unavailable (HTTP $code)")
                }

                val json = JSONObject(body)
                val summary = parseSummary(session.user.id, json)
                JellyfinResult.Success(summary)
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to load achievement summary", error)
            }
        }

    override suspend fun getAchievements(session: JellyfinSession): JellyfinResult<List<JellyfinAchievement>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}")
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Achievements unavailable (HTTP $code)")
                }

                android.util.Log.d("VANTAFYN_ACHIEVEMENTS", "raw JSON: $body")

                val list = parseAchievements(session, body)
                JellyfinResult.Success(list)
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to load achievements", error)
            }
        }

    override suspend fun getUnlocksSince(
        session: JellyfinSession,
        sinceIso: String?,
        deviceId: String,
    ): JellyfinResult<List<JellyfinAchievementUnlock>> =
        withContext(ioDispatcher) {
            runCatching {
                val queryParams = buildString {
                    append("deviceId=").append(URLEncoder.encode(deviceId, Charsets.UTF_8.name()))
                    if (!sinceIso.isNullOrBlank()) {
                        append("&since=").append(URLEncoder.encode(sinceIso, Charsets.UTF_8.name()))
                    }
                }
                val path = "Plugins/AchievementBadges/users/${session.user.id}/unlocks-since?$queryParams"
                val conn = session.openAuthenticatedConnection(path)
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Achievement unlocks unavailable (HTTP $code)")
                }

                val unlocks = parseUnlocks(session, body)
                JellyfinResult.Success(unlocks)
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to poll achievement unlocks", error)
            }
        }

    private fun parseSummary(userId: UUID, json: JSONObject): JellyfinAchievementSummary {
        val rankName = json.optStringOrNull("RankName", "rankName", "Rank", "rank", "TierName", "tierName")
            ?: "Rookie"
        val rankTier = json.optIntOrNull("RankTier", "rankTier", "Tier", "tier", "Level", "level") ?: 1
        val currentScore = json.optIntOrNull("CurrentScore", "currentScore", "Score", "score", "Points", "points") ?: 0
        val nextRankScore = json.optIntOrNull("NextRankScore", "nextRankScore", "NextTierScore", "nextTierScore")
        val unlockedCount = json.optIntOrNull("UnlockedCount", "unlockedCount", "Unlocked", "unlocked", "EarnedCount", "earnedCount") ?: 0
        val totalCount = json.optIntOrNull("TotalCount", "totalCount", "Total", "total", "BadgeCount", "badgeCount") ?: 0
        val progressPercentage = json.optIntOrNull("ProgressPercentage", "progressPercentage", "Progress", "progress", "CompletionPercentage")
            ?: if (totalCount > 0) ((unlockedCount.toFloat() / totalCount.toFloat()) * 100f).toInt().coerceIn(0, 100) else 0

        return JellyfinAchievementSummary(
            userId = userId,
            rankName = rankName,
            rankTier = rankTier,
            currentScore = currentScore,
            nextRankScore = nextRankScore,
            unlockedCount = unlockedCount,
            totalCount = totalCount,
            progressPercentage = progressPercentage,
        )
    }

    private fun parseAchievements(session: JellyfinSession, raw: String): List<JellyfinAchievement> {
        val trimmed = raw.trim()
        val array: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("badges")
                    ?: obj.optJSONArray("Badges")
                    ?: obj.optJSONArray("achievements")
                    ?: obj.optJSONArray("Achievements")
                    ?: obj.optJSONArray("items")
                    ?: obj.optJSONArray("Items")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val baseUrl = session.server.url.trimEnd('/')
        return (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            val id = obj.optStringOrNull("Id", "id", "BadgeId", "badgeId", "Key", "key") ?: return@mapNotNull null
            val name = obj.optStringOrNull("Name", "name", "Title", "title", "DisplayName", "displayName") ?: "Achievement"
            val description = obj.optStringOrNull("Description", "description", "Summary", "summary", "Criteria", "criteria") ?: ""
            val category = obj.optStringOrNull("Category", "category", "Type", "type", "Group", "group") ?: "General"
            val rarityStr = obj.optStringOrNull("Rarity", "rarity", "Tier", "tier", "Level", "level")
            val rarity = JellyfinAchievementRarity.fromString(rarityStr)

            val rawIcon = obj.optStringOrNull(
                "IconUrl", "iconUrl", "Icon", "icon", "BadgeUrl", "badgeUrl",
                "ImageUrl", "imageUrl", "ImagePath", "imagePath", "Glyph", "glyph", "IconName", "iconName"
            )
            val isWebUrl = rawIcon != null && (
                rawIcon.startsWith("http://") || rawIcon.startsWith("https://") ||
                rawIcon.startsWith("/") || rawIcon.endsWith(".png") ||
                rawIcon.endsWith(".jpg") || rawIcon.endsWith(".webp") || rawIcon.endsWith(".svg")
            )
            val iconUrl = if (isWebUrl) toAuthenticatedUrl(session, rawIcon) else null
            val iconName = if (!isWebUrl) rawIcon else null

            val isUnlocked = obj.optBooleanOrNull("IsUnlocked", "isUnlocked", "Unlocked", "unlocked", "Earned", "earned") ?: false
            val unlockedAt = obj.optStringOrNull("UnlockedAt", "unlockedAt", "DateUnlocked", "dateUnlocked", "EarnedAt", "earnedAt")
            val isHidden = obj.optBooleanOrNull("IsHidden", "isHidden", "Hidden", "hidden", "Secret", "secret") ?: false
            val score = obj.optIntOrNull("Score", "score", "Points", "points", "Value", "value") ?: 0
            val isEquipped = obj.optBooleanOrNull("IsEquipped", "isEquipped", "Equipped", "equipped") ?: false

            val rawCurrent = obj.optDoubleOrNull("CurrentValue", "currentValue", "CurrentProgress", "currentProgress", "ProgressValue", "progressValue", "Current", "current", "Progress", "progress", "Value", "value")
            val rawTarget = obj.optDoubleOrNull("TargetValue", "targetValue", "TargetProgress", "targetProgress", "Target", "target", "MaxProgress", "maxProgress", "Max", "max", "Required", "required", "Goal", "goal")
            val rawRatio = obj.optDoubleOrNull("ProgressRatio", "progressRatio", "ProgressPercentage", "progressPercentage", "CompletionRatio", "completionRatio", "Percentage", "percentage")?.toFloat()
            var rawUnit = obj.optStringOrNull("Unit", "unit", "ProgressUnit", "progressUnit", "Metric", "metric")

            if (rawUnit.isNullOrBlank()) {
                val descLower = description.lowercase()
                rawUnit = when {
                    descLower.contains("movie") -> if (rawTarget == 1.0) "movie" else "movies"
                    descLower.contains("episode") -> if (rawTarget == 1.0) "episode" else "episodes"
                    descLower.contains("item") -> if (rawTarget == 1.0) "item" else "items"
                    descLower.contains("hour") -> if (rawTarget == 1.0) "hour" else "hours"
                    descLower.contains("minute") -> if (rawTarget == 1.0) "minute" else "minutes"
                    descLower.contains("day") -> if (rawTarget == 1.0) "day" else "days"
                    descLower.contains("song") || descLower.contains("track") -> if (rawTarget == 1.0) "track" else "tracks"
                    category.equals("Music", ignoreCase = true) -> "tracks"
                    else -> null
                }
            }

            val normalizedRatio: Float? = when {
                isUnlocked -> 1.0f
                rawCurrent != null && rawTarget != null && rawTarget > 0.0 -> {
                    (rawCurrent / rawTarget).toFloat().coerceIn(0f, 1f)
                }
                rawRatio != null -> {
                    if (rawRatio > 1.0f) (rawRatio / 100f).coerceIn(0f, 1f) else rawRatio.coerceIn(0f, 1f)
                }
                rawCurrent != null && rawCurrent > 0.0 && rawCurrent <= 1.0 -> rawCurrent.toFloat()
                else -> null
            }

            val progressText: String? = when {
                isUnlocked -> "Completed"
                rawCurrent != null && rawTarget != null && rawTarget > 0.0 -> {
                    val curStr = if (rawCurrent % 1.0 == 0.0) rawCurrent.toLong().toString() else "%.1f".format(rawCurrent)
                    val tarStr = if (rawTarget % 1.0 == 0.0) rawTarget.toLong().toString() else "%.1f".format(rawTarget)
                    if (!rawUnit.isNullOrBlank()) "$curStr / $tarStr $rawUnit" else "$curStr / $tarStr"
                }
                normalizedRatio != null && normalizedRatio > 0f -> {
                    "${(normalizedRatio * 100).toInt()}%"
                }
                else -> null
            }

            if (!isUnlocked) {
                android.util.Log.d("VANTAFYN_ACHIEVEMENTS", "Locked badge: $name (id=$id), isUnlocked=$isUnlocked, rawCurrent=$rawCurrent, rawTarget=$rawTarget, ratio=$normalizedRatio, text=$progressText")
            }

            JellyfinAchievement(
                id = id,
                name = name,
                description = description,
                category = category,
                rarity = rarity,
                iconUrl = iconUrl,
                iconName = iconName,
                isUnlocked = isUnlocked,
                unlockedAt = unlockedAt,
                isHidden = isHidden,
                score = score,
                isEquipped = isEquipped,
                progressRatio = normalizedRatio,
                currentProgress = rawCurrent,
                maxProgress = rawTarget,
                progressUnit = rawUnit,
                progressText = progressText,
            )
        }
    }

    private fun parseUnlocks(session: JellyfinSession, raw: String): List<JellyfinAchievementUnlock> {
        val trimmed = raw.trim()
        val array: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("unlocks")
                    ?: obj.optJSONArray("Unlocks")
                    ?: obj.optJSONArray("items")
                    ?: obj.optJSONArray("Items")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        return (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            val id = obj.optStringOrNull("Id", "id", "UnlockId", "unlockId") ?: UUID.randomUUID().toString()
            val achievementId = obj.optStringOrNull("AchievementId", "achievementId", "BadgeId", "badgeId", "Id", "id") ?: return@mapNotNull null
            val name = obj.optStringOrNull("Name", "name", "Title", "title") ?: "Achievement Unlocked"
            val description = obj.optStringOrNull("Description", "description") ?: ""
            val score = obj.optIntOrNull("Score", "score", "Points", "points") ?: 0
            val rarityStr = obj.optStringOrNull("Rarity", "rarity")
            val rarity = JellyfinAchievementRarity.fromString(rarityStr)
            val rawIcon = obj.optStringOrNull(
                "IconUrl", "iconUrl", "Icon", "icon", "BadgeUrl", "badgeUrl",
                "ImageUrl", "imageUrl", "ImagePath", "imagePath", "Glyph", "glyph", "IconName", "iconName"
            )
            val isWebUrl = rawIcon != null && (
                rawIcon.startsWith("http://") || rawIcon.startsWith("https://") ||
                rawIcon.startsWith("/") || rawIcon.endsWith(".png") ||
                rawIcon.endsWith(".jpg") || rawIcon.endsWith(".webp") || rawIcon.endsWith(".svg")
            )
            val iconUrl = if (isWebUrl) toAuthenticatedUrl(session, rawIcon) else null
            val iconName = if (!isWebUrl) rawIcon else null
            val unlockedAt = obj.optStringOrNull("UnlockedAt", "unlockedAt", "Timestamp", "timestamp")

            JellyfinAchievementUnlock(
                id = id,
                achievementId = achievementId,
                name = name,
                description = description,
                iconUrl = iconUrl,
                iconName = iconName,
                score = score,
                rarity = rarity,
                unlockedAt = unlockedAt,
            )
        }
    }

    private fun toAuthenticatedUrl(session: JellyfinSession, rawIcon: String?, fallbackPath: String? = null): String? {
        val baseUrl = session.server.url.trimEnd('/')
        val path = when {
            !rawIcon.isNullOrBlank() -> {
                if (rawIcon.startsWith("http://") || rawIcon.startsWith("https://")) {
                    rawIcon
                } else if (rawIcon.startsWith("/")) {
                    "$baseUrl$rawIcon"
                } else {
                    "$baseUrl/Plugins/AchievementBadges/icons/$rawIcon"
                }
            }
            !fallbackPath.isNullOrBlank() -> "$baseUrl$fallbackPath"
            else -> return null
        }
        return if (path.contains("api_key=") || session.accessToken.isBlank()) {
            path
        } else {
            path + if (path.contains("?")) "&api_key=${session.accessToken}" else "?api_key=${session.accessToken}"
        }
    }

    private fun JSONObject.optStringOrNull(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
        }

    private fun JSONObject.optIntOrNull(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key ->
            if (has(key) && !isNull(key)) optInt(key) else null
        }

    private fun JSONObject.optBooleanOrNull(vararg keys: String): Boolean? =
        keys.firstNotNullOfOrNull { key ->
            if (has(key) && !isNull(key)) optBoolean(key) else null
        }

    private fun JSONObject.optDoubleOrNull(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { key ->
            if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null
        }
}

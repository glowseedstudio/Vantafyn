package dev.vantafyn.core.jellyfin

import java.util.UUID

enum class JellyfinAchievementRarity(val label: String) {
    Common("Common"),
    Uncommon("Uncommon"),
    Rare("Rare"),
    Epic("Epic"),
    Legendary("Legendary"),
    Mythic("Mythic"),
    Unknown("Standard");

    companion object {
        fun fromString(value: String?): JellyfinAchievementRarity =
            when (value?.trim()?.lowercase()) {
                "common", "0" -> Common
                "uncommon", "1" -> Uncommon
                "rare", "2" -> Rare
                "epic", "3" -> Epic
                "legendary", "4" -> Legendary
                "mythic", "5" -> Mythic
                else -> Unknown
            }
    }
}

data class JellyfinAchievement(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val rarity: JellyfinAchievementRarity,
    val iconUrl: String? = null,
    val iconName: String? = null,
    val isUnlocked: Boolean = false,
    val unlockedAt: String? = null,
    val isHidden: Boolean = false,
    val score: Int = 0,
    val isEquipped: Boolean = false,
    val progressRatio: Float? = null,
    val currentProgress: Double? = null,
    val maxProgress: Double? = null,
    val progressUnit: String? = null,
    val progressText: String? = null,
)

data class AchievementRankTier(
    val name: String,
    val minScore: Int,
    val colorHex: String,
    val iconName: String,
    val tierNumber: Int,
)

object AchievementRankHelper {
    val Tiers: List<AchievementRankTier> = listOf(
        AchievementRankTier("Rookie",       0,     "#9aa5b1", "sprout", 1),
        AchievementRankTier("Novice",       100,   "#4caf50", "eco", 2),
        AchievementRankTier("Viewer",       300,   "#2196f3", "visibility", 3),
        AchievementRankTier("Regular",      700,   "#03a9f4", "person", 4),
        AchievementRankTier("Enthusiast",   1500,  "#00bcd4", "star", 5),
        AchievementRankTier("Binger",       3000,  "#9c27b0", "bolt", 6),
        AchievementRankTier("Connoisseur",  5000,  "#e91e63", "workspace_premium", 7),
        AchievementRankTier("Maestro",      8000,  "#ff9800", "military_tech", 8),
        AchievementRankTier("Legend",       12000, "#f44336", "local_fire_department", 9),
        AchievementRankTier("Immortal",     20000, "#ffd700", "auto_awesome", 10),
    )

    fun getTier(score: Int): AchievementRankTier {
        var current = Tiers[0]
        for (tier in Tiers) {
            if (score >= tier.minScore) {
                current = tier
            } else {
                break
            }
        }
        return current
    }

    fun getNextTier(score: Int): AchievementRankTier? {
        for (tier in Tiers) {
            if (score < tier.minScore) {
                return tier
            }
        }
        return null
    }

    fun getProgressInTier(score: Int): Float {
        val currentTier = getTier(score)
        val nextTier = getNextTier(score) ?: return 1.0f
        val range = nextTier.minScore - currentTier.minScore
        if (range <= 0) return 1.0f
        val earnedInTier = (score - currentTier.minScore).coerceAtLeast(0)
        return (earnedInTier.toFloat() / range.toFloat()).coerceIn(0f, 1f)
    }
}

data class JellyfinAchievementSummary(
    val userId: UUID,
    val rankName: String,
    val rankTier: Int = 1,
    val currentScore: Int = 0,
    val nextRankScore: Int? = null,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val progressPercentage: Int = 0,
    val rankProgressRatio: Float = 0f,
)

data class JellyfinAchievementUnlock(
    val id: String,
    val achievementId: String,
    val name: String,
    val description: String,
    val iconUrl: String? = null,
    val iconName: String? = null,
    val score: Int = 0,
    val rarity: JellyfinAchievementRarity = JellyfinAchievementRarity.Unknown,
    val unlockedAt: String? = null,
)

interface JellyfinAchievementRepository {
    suspend fun checkAvailability(session: JellyfinSession): Boolean
    suspend fun getSummary(session: JellyfinSession): JellyfinResult<JellyfinAchievementSummary>
    suspend fun getAchievements(session: JellyfinSession): JellyfinResult<List<JellyfinAchievement>>
    suspend fun getUnlocksSince(
        session: JellyfinSession,
        sinceIso: String?,
        deviceId: String,
    ): JellyfinResult<List<JellyfinAchievementUnlock>>
    suspend fun getRecentUnlocks(
        session: JellyfinSession,
        limit: Int = 10,
    ): JellyfinResult<List<JellyfinAchievementUnlock>>
}

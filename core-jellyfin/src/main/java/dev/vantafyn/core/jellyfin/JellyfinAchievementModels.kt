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
)

data class JellyfinAchievementSummary(
    val userId: UUID,
    val rankName: String,
    val rankTier: Int = 1,
    val currentScore: Int = 0,
    val nextRankScore: Int? = null,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val progressPercentage: Int = 0,
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
}

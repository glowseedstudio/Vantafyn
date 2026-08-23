package dev.vantafyn.core.jellyfin

import java.util.UUID

data class JellyfinFriend(
    val userId: UUID,
    val username: String,
    val displayName: String,
    val avatarTag: String? = null,
    val avatarUrl: String? = null,
    val rankName: String? = null,
    val rankTier: Int = 1,
    val currentScore: Int = 0,
    val isOnline: Boolean = false,
    val lastSeen: String? = null,
    val currentlyWatching: String? = null,
    val equippedBadgeName: String? = null,
    val equippedBadgeIcon: String? = null,
)

data class JellyfinFriendRequest(
    val id: String,
    val senderId: UUID,
    val senderName: String,
    val senderAvatarTag: String? = null,
    val senderAvatarUrl: String? = null,
    val senderRankTier: Int = 1,
    val receiverId: UUID,
    val receiverName: String,
    val createdAt: String? = null,
    val isIncoming: Boolean = true,
)

data class JellyfinSocialConversation(
    val conversationId: String,
    val peerUserId: UUID,
    val peerName: String,
    val peerAvatarTag: String? = null,
    val peerAvatarUrl: String? = null,
    val peerRankTier: Int = 1,
    val peerIsOnline: Boolean = false,
    val lastMessageText: String? = null,
    val lastMessageTimestamp: String? = null,
    val unreadCount: Int = 0,
    val lastSenderId: UUID? = null,
)

data class JellyfinSocialMessage(
    val messageId: String,
    val conversationId: String,
    val senderId: UUID,
    val senderName: String,
    val senderAvatarTag: String? = null,
    val senderAvatarUrl: String? = null,
    val recipientId: UUID,
    val content: String,
    val timestamp: String? = null,
    val isRead: Boolean = false,
    val isFromSelf: Boolean = false,
)

data class JellyfinSocialSummary(
    val friendCount: Int = 0,
    val incomingRequestCount: Int = 0,
    val outgoingRequestCount: Int = 0,
    val unreadMessageCount: Int = 0,
)

data class JellyfinBlockedUser(
    val userId: UUID,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val blockedAtTimestamp: Long = System.currentTimeMillis(),
)

fun formatSocialSnippet(raw: String?): String {
    if (raw.isNullOrBlank()) return "Tap to start chatting"
    val trimmed = raw.trim()
    if (trimmed.startsWith("[media_rec|") && trimmed.endsWith("]")) {
        val content = trimmed.removePrefix("[media_rec|").removeSuffix("]")
        val parts = content.split("|")
        val title = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Media"
        val itemType = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
        val prefix = when {
            itemType.equals("Movie", ignoreCase = true) -> "🎬 "
            itemType.equals("Series", ignoreCase = true) || itemType.equals("Episode", ignoreCase = true) -> "📺 "
            itemType.equals("Audio", ignoreCase = true) || itemType.equals("Music", ignoreCase = true) -> "🎵 "
            else -> "🍿 "
        }
        return "$prefix$title"
    }
    if (trimmed.startsWith("[reaction|") && trimmed.endsWith("]")) {
        val content = trimmed.removePrefix("[reaction|").removeSuffix("]")
        val parts = content.split("|")
        val emoji = parts.getOrNull(1)?.trim() ?: "❤️"
        return "Reacted $emoji to a message"
    }
    return raw
}

fun parseSocialTimestampToMillis(timestampStr: String?): Long {
    if (timestampStr.isNullOrBlank()) return 0L
    val clean = timestampStr.trim()
    return try {
        java.time.OffsetDateTime.parse(clean).toInstant().toEpochMilli()
    } catch (e1: Exception) {
        try {
            java.time.Instant.parse(clean).toEpochMilli()
        } catch (e2: Exception) {
            try {
                java.time.LocalDateTime.parse(clean).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e3: Exception) {
                try {
                    clean.toLong()
                } catch (e4: Exception) {
                    0L
                }
            }
        }
    }
}

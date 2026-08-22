package dev.vantafyn.core.jellyfin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.UUID

interface JellyfinSocialRepository {
    suspend fun checkSocialAvailability(session: JellyfinSession): Boolean
    suspend fun getFriends(session: JellyfinSession): JellyfinResult<List<JellyfinFriend>>
    suspend fun getFriendRequests(session: JellyfinSession): JellyfinResult<List<JellyfinFriendRequest>>
    suspend fun sendFriendRequest(session: JellyfinSession, targetUsernameOrId: String): JellyfinResult<Unit>
    suspend fun acceptFriendRequest(session: JellyfinSession, requestId: String): JellyfinResult<Unit>
    suspend fun declineOrRemoveFriend(session: JellyfinSession, targetId: String): JellyfinResult<Unit>
    suspend fun getConversations(session: JellyfinSession): JellyfinResult<List<JellyfinSocialConversation>>
    suspend fun getMessages(session: JellyfinSession, conversationId: String, peerUserId: UUID? = null): JellyfinResult<List<JellyfinSocialMessage>>
    suspend fun sendMessage(session: JellyfinSession, recipientId: UUID, conversationId: String?, text: String): JellyfinResult<JellyfinSocialMessage>
    suspend fun markConversationRead(session: JellyfinSession, conversationId: String): JellyfinResult<Unit>
    suspend fun getUnreadSummary(session: JellyfinSession): JellyfinResult<JellyfinSocialSummary>
    suspend fun getDiscoverableUsers(session: JellyfinSession): JellyfinResult<List<JellyfinFriend>>
}

class SdkJellyfinSocialRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinSocialRepository {

    override suspend fun checkSocialAvailability(session: JellyfinSession): Boolean =
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

    override suspend fun getFriends(session: JellyfinSession): JellyfinResult<List<JellyfinFriend>> =
        withContext(ioDispatcher) {
            runCatching {
                var conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/friends")
                var code = conn.responseCode
                var body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code !in 200..299) {
                    // Fallback to /Plugins/AchievementBadges/friends
                    conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/friends")
                    code = conn.responseCode
                    body = if (code in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    }
                    conn.disconnect()
                }

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Friends unavailable (HTTP $code)")
                }

                val friends = parseFriends(session, body)
                JellyfinResult.Success(friends)
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to load friends", error)
            }
        }

    override suspend fun getFriendRequests(session: JellyfinSession): JellyfinResult<List<JellyfinFriendRequest>> =
        withContext(ioDispatcher) {
            runCatching {
                var conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/friends/requests")
                var code = conn.responseCode
                var body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code !in 200..299) {
                    // Fallback to user-scoped path
                    conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/friends/requests")
                    code = conn.responseCode
                    body = if (code in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    }
                    conn.disconnect()
                }

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Friend requests unavailable (HTTP $code)")
                }

                val requests = parseFriendRequests(session, body)
                JellyfinResult.Success(requests)
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to load friend requests", error)
            }
        }

    override suspend fun sendFriendRequest(session: JellyfinSession, targetUsernameOrId: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/friends/requests")
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val payload = JSONObject().apply {
                    put("target", targetUsernameOrId)
                    put("username", targetUsernameOrId)
                    put("userId", targetUsernameOrId)
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                conn.disconnect()

                if (code in 200..299) {
                    JellyfinResult.Success(Unit)
                } else {
                    JellyfinResult.Failure("Failed to send friend request (HTTP $code)")
                }
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to send friend request", error)
            }
        }

    override suspend fun acceptFriendRequest(session: JellyfinSession, requestId: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                var conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/friends/requests/$requestId/accept")
                conn.requestMethod = "POST"
                var code = conn.responseCode
                conn.disconnect()

                if (code !in 200..299) {
                    // Fallback to body-based accept
                    conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/friends/accept")
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    val payload = JSONObject().apply { put("requestId", requestId) }
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(payload.toString())
                        writer.flush()
                    }
                    code = conn.responseCode
                    conn.disconnect()
                }

                if (code in 200..299) {
                    JellyfinResult.Success(Unit)
                } else {
                    JellyfinResult.Failure("Failed to accept friend request (HTTP $code)")
                }
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to accept friend request", error)
            }
        }

    override suspend fun declineOrRemoveFriend(session: JellyfinSession, targetId: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                var conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/friends/requests/$targetId/decline")
                conn.requestMethod = "POST"
                var code = conn.responseCode
                conn.disconnect()

                if (code !in 200..299) {
                    // Try DELETE /Plugins/AchievementBadges/friends/$targetId
                    conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/friends/$targetId")
                    conn.requestMethod = "DELETE"
                    code = conn.responseCode
                    conn.disconnect()
                }

                if (code in 200..299) {
                    JellyfinResult.Success(Unit)
                } else {
                    JellyfinResult.Failure("Failed to decline/remove friend (HTTP $code)")
                }
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to remove friend", error)
            }
        }

    override suspend fun getConversations(session: JellyfinSession): JellyfinResult<List<JellyfinSocialConversation>> =
        withContext(ioDispatcher) {
            runCatching {
                var conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/conversations")
                var code = conn.responseCode
                var body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code !in 200..299) {
                    // Fallback to /Plugins/AchievementBadges/messages/conversations
                    conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/messages/conversations")
                    code = conn.responseCode
                    body = if (code in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    }
                    conn.disconnect()
                }

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Conversations unavailable (HTTP $code)")
                }

                val conversations = parseConversations(session, body)
                JellyfinResult.Success(conversations)
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to load conversations", error)
            }
        }

    override suspend fun getMessages(
        session: JellyfinSession,
        conversationId: String,
        peerUserId: UUID?,
    ): JellyfinResult<List<JellyfinSocialMessage>> =
        withContext(ioDispatcher) {
            runCatching {
                val encodedConvId = URLEncoder.encode(conversationId, Charsets.UTF_8.name())
                var conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/messages?conversationId=$encodedConvId")
                var code = conn.responseCode
                var body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code !in 200..299 && peerUserId != null) {
                    // Fallback to /Plugins/AchievementBadges/users/{userId}/messages/{peerId}
                    conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/messages/$peerUserId")
                    code = conn.responseCode
                    body = if (code in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    }
                    conn.disconnect()
                }

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Messages unavailable (HTTP $code)")
                }

                val messages = parseMessages(session, body, conversationId)
                JellyfinResult.Success(messages)
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to load messages", error)
            }
        }

    override suspend fun sendMessage(
        session: JellyfinSession,
        recipientId: UUID,
        conversationId: String?,
        text: String,
    ): JellyfinResult<JellyfinSocialMessage> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/messages")
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val payload = JSONObject().apply {
                    put("recipientId", recipientId.toString())
                    if (!conversationId.isNullOrBlank()) {
                        put("conversationId", conversationId)
                    }
                    put("content", text)
                    put("text", text)
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Failed to send message (HTTP $code)")
                }

                val parsed = if (body.isNotBlank() && body.trim().startsWith("{")) {
                    parseSingleMessage(session, JSONObject(body), conversationId ?: recipientId.toString())
                } else {
                    JellyfinSocialMessage(
                        messageId = UUID.randomUUID().toString(),
                        conversationId = conversationId ?: recipientId.toString(),
                        senderId = session.user.id,
                        senderName = session.user.name,
                        senderAvatarTag = null,
                        senderAvatarUrl = null,
                        recipientId = recipientId,
                        content = text,
                        timestamp = null,
                        isRead = true,
                        isFromSelf = true,
                    )
                }
                JellyfinResult.Success(parsed)
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to send message", error)
            }
        }

    override suspend fun markConversationRead(session: JellyfinSession, conversationId: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/messages/read")
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val payload = JSONObject().apply {
                    put("conversationId", conversationId)
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                conn.disconnect()

                if (code in 200..299) {
                    JellyfinResult.Success(Unit)
                } else {
                    JellyfinResult.Failure("Failed to mark read (HTTP $code)")
                }
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to mark read", error)
            }
        }

    override suspend fun getUnreadSummary(session: JellyfinSession): JellyfinResult<JellyfinSocialSummary> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/messages/unread-count")
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                if (code in 200..299 && body.isNotBlank()) {
                    val json = if (body.trim().startsWith("{")) JSONObject(body) else JSONObject().put("unreadCount", body.trim().toIntOrNull() ?: 0)
                    val unread = json.optIntOrNull("UnreadCount", "unreadCount", "Unread", "unread", "count") ?: 0
                    val requests = json.optIntOrNull("PendingRequests", "pendingRequests", "Requests", "requests") ?: 0
                    val friends = json.optIntOrNull("FriendCount", "friendCount", "Friends", "friends") ?: 0
                    JellyfinResult.Success(
                        JellyfinSocialSummary(
                            friendCount = friends,
                            incomingRequestCount = requests,
                            outgoingRequestCount = 0,
                            unreadMessageCount = unread,
                        ),
                    )
                } else {
                    // Fallback to computing from conversations
                    val convos = (getConversations(session) as? JellyfinResult.Success)?.value.orEmpty()
                    val totalUnread = convos.sumOf { it.unreadCount }
                    val requests = (getFriendRequests(session) as? JellyfinResult.Success)?.value.orEmpty().count { it.isIncoming }
                    val friends = (getFriends(session) as? JellyfinResult.Success)?.value.orEmpty().size
                    JellyfinResult.Success(
                        JellyfinSocialSummary(
                            friendCount = friends,
                            incomingRequestCount = requests,
                            outgoingRequestCount = 0,
                            unreadMessageCount = totalUnread,
                        ),
                    )
                }
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to fetch unread count", error)
            }
        }

    override suspend fun getDiscoverableUsers(session: JellyfinSession): JellyfinResult<List<JellyfinFriend>> =
        withContext(ioDispatcher) {
            runCatching {
                // Try plugin suggestions endpoint first
                var conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/friends/suggestions")
                var code = conn.responseCode
                var body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    ""
                }
                conn.disconnect()

                if (code !in 200..299 || body.isBlank() || body.trim() == "[]") {
                    // Fallback to Jellyfin public users list
                    conn = session.openAuthenticatedConnection("Users/Public")
                    code = conn.responseCode
                    body = if (code in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        ""
                    }
                    conn.disconnect()
                }

                if (code in 200..299 && body.isNotBlank()) {
                    val users = parseFriends(session, body)
                    JellyfinResult.Success(users.filter { it.userId != session.user.id })
                } else {
                    JellyfinResult.Success(emptyList())
                }
            }.getOrElse { error ->
                JellyfinResult.Failure(error.message ?: "Failed to load discoverable users", error)
            }
        }

    // --- JSON Parsing Helpers ---

    private fun parseFriends(session: JellyfinSession, body: String): List<JellyfinFriend> {
        val trimmed = body.trim()
        val array = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val json = JSONObject(trimmed)
                json.optJSONArray("Items")
                    ?: json.optJSONArray("items")
                    ?: json.optJSONArray("Friends")
                    ?: json.optJSONArray("friends")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val result = mutableListOf<JellyfinFriend>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val idStr = item.optStringOrNull("UserId", "userId", "Id", "id") ?: continue
            val userId = runCatching { UUID.fromString(idStr) }.getOrNull() ?: continue
            val username = item.optStringOrNull("Username", "username", "Name", "name") ?: "User"
            val displayName = item.optStringOrNull("DisplayName", "displayName", "Name", "name") ?: username
            val avatarTag = item.optStringOrNull("PrimaryImageTag", "primaryImageTag", "AvatarTag", "avatarTag", "ImageTag", "imageTag")
            val avatarUrl = toUserAvatarUrl(session, userId, avatarTag)
            val rankName = item.optStringOrNull("RankName", "rankName", "Rank", "rank", "TierName") ?: "Rookie"
            val rankTier = item.optIntOrNull("RankTier", "rankTier", "Tier", "tier", "Level", "level") ?: 1
            val currentScore = item.optIntOrNull("CurrentScore", "currentScore", "Score", "score", "Points", "points") ?: 0
            val isOnline = item.optBooleanOrNull("IsOnline", "isOnline", "Online", "online") ?: false
            val lastSeen = item.optStringOrNull("LastSeen", "lastSeen", "LastActivity", "lastActivity")
            val currentlyWatching = item.optStringOrNull("CurrentlyWatching", "currentlyWatching", "NowPlaying", "nowPlaying", "Activity", "activity")
            val equippedBadgeName = item.optStringOrNull("EquippedBadgeName", "equippedBadgeName", "ShowcaseBadge", "showcaseBadge")
            val equippedBadgeIcon = item.optStringOrNull("EquippedBadgeIcon", "equippedBadgeIcon", "BadgeIcon", "badgeIcon")

            result += JellyfinFriend(
                userId = userId,
                username = username,
                displayName = displayName,
                avatarTag = avatarTag,
                avatarUrl = avatarUrl,
                rankName = rankName,
                rankTier = rankTier,
                currentScore = currentScore,
                isOnline = isOnline,
                lastSeen = lastSeen,
                currentlyWatching = currentlyWatching,
                equippedBadgeName = equippedBadgeName,
                equippedBadgeIcon = equippedBadgeIcon,
            )
        }
        return result
    }

    private fun parseFriendRequests(session: JellyfinSession, body: String): List<JellyfinFriendRequest> {
        val trimmed = body.trim()
        val array = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val json = JSONObject(trimmed)
                json.optJSONArray("Items")
                    ?: json.optJSONArray("items")
                    ?: json.optJSONArray("Requests")
                    ?: json.optJSONArray("requests")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val result = mutableListOf<JellyfinFriendRequest>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optStringOrNull("Id", "id", "RequestId", "requestId") ?: UUID.randomUUID().toString()
            val senderIdStr = item.optStringOrNull("SenderId", "senderId", "FromUserId", "fromUserId", "UserId", "userId") ?: continue
            val senderId = runCatching { UUID.fromString(senderIdStr) }.getOrNull() ?: continue
            val senderName = item.optStringOrNull("SenderName", "senderName", "FromName", "fromName", "Username", "username") ?: "User"
            val senderAvatarTag = item.optStringOrNull("SenderAvatarTag", "senderAvatarTag", "PrimaryImageTag", "primaryImageTag", "ImageTag", "imageTag")
            val senderAvatarUrl = toUserAvatarUrl(session, senderId, senderAvatarTag)
            val senderRankTier = item.optIntOrNull("SenderRankTier", "senderRankTier", "RankTier", "rankTier", "Tier", "tier") ?: 1
            val receiverIdStr = item.optStringOrNull("ReceiverId", "receiverId", "ToUserId", "toUserId") ?: session.user.id.toString()
            val receiverId = runCatching { UUID.fromString(receiverIdStr) }.getOrNull() ?: session.user.id
            val receiverName = item.optStringOrNull("ReceiverName", "receiverName", "ToName", "toName") ?: session.user.name
            val createdAt = item.optStringOrNull("CreatedAt", "createdAt", "Date", "date", "Timestamp", "timestamp")
            val isIncoming = receiverId == session.user.id || senderId != session.user.id

            result += JellyfinFriendRequest(
                id = id,
                senderId = senderId,
                senderName = senderName,
                senderAvatarTag = senderAvatarTag,
                senderAvatarUrl = senderAvatarUrl,
                senderRankTier = senderRankTier,
                receiverId = receiverId,
                receiverName = receiverName,
                createdAt = createdAt,
                isIncoming = isIncoming,
            )
        }
        return result
    }

    private fun parseConversations(session: JellyfinSession, body: String): List<JellyfinSocialConversation> {
        val trimmed = body.trim()
        val array = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val json = JSONObject(trimmed)
                json.optJSONArray("Items")
                    ?: json.optJSONArray("items")
                    ?: json.optJSONArray("Conversations")
                    ?: json.optJSONArray("conversations")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val result = mutableListOf<JellyfinSocialConversation>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val peerIdStr = item.optStringOrNull("PeerUserId", "peerUserId", "UserId", "userId", "PeerId", "peerId", "RecipientId", "recipientId") ?: continue
            val peerUserId = runCatching { UUID.fromString(peerIdStr) }.getOrNull() ?: continue
            val convId = item.optStringOrNull("ConversationId", "conversationId", "Id", "id") ?: peerUserId.toString()
            val peerName = item.optStringOrNull("PeerName", "peerName", "Username", "username", "DisplayName", "displayName", "Name", "name") ?: "Friend"
            val peerAvatarTag = item.optStringOrNull("PeerAvatarTag", "peerAvatarTag", "PrimaryImageTag", "primaryImageTag", "ImageTag", "imageTag")
            val peerAvatarUrl = toUserAvatarUrl(session, peerUserId, peerAvatarTag)
            val peerRankTier = item.optIntOrNull("PeerRankTier", "peerRankTier", "RankTier", "rankTier", "Tier", "tier") ?: 1
            val peerIsOnline = item.optBooleanOrNull("PeerIsOnline", "peerIsOnline", "IsOnline", "isOnline") ?: false
            val lastMessageText = item.optStringOrNull("LastMessageText", "lastMessageText", "LastMessage", "lastMessage", "Content", "content", "Text", "text")
            val lastMessageTimestamp = item.optStringOrNull("LastMessageTimestamp", "lastMessageTimestamp", "LastTimestamp", "lastTimestamp", "Timestamp", "timestamp", "Date", "date")
            val unreadCount = item.optIntOrNull("UnreadCount", "unreadCount", "Unread", "unread") ?: 0
            val lastSenderIdStr = item.optStringOrNull("LastSenderId", "lastSenderId", "SenderId", "senderId")
            val lastSenderId = lastSenderIdStr?.let { runCatching { UUID.fromString(it) }.getOrNull() }

            result += JellyfinSocialConversation(
                conversationId = convId,
                peerUserId = peerUserId,
                peerName = peerName,
                peerAvatarTag = peerAvatarTag,
                peerAvatarUrl = peerAvatarUrl,
                peerRankTier = peerRankTier,
                peerIsOnline = peerIsOnline,
                lastMessageText = lastMessageText,
                lastMessageTimestamp = lastMessageTimestamp,
                unreadCount = unreadCount,
                lastSenderId = lastSenderId,
            )
        }
        return result
    }

    private fun parseMessages(session: JellyfinSession, body: String, defaultConvId: String): List<JellyfinSocialMessage> {
        val trimmed = body.trim()
        val array = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val json = JSONObject(trimmed)
                json.optJSONArray("Items")
                    ?: json.optJSONArray("items")
                    ?: json.optJSONArray("Messages")
                    ?: json.optJSONArray("messages")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val result = mutableListOf<JellyfinSocialMessage>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            result += parseSingleMessage(session, item, defaultConvId)
        }
        return result
    }

    private fun parseSingleMessage(session: JellyfinSession, item: JSONObject, defaultConvId: String): JellyfinSocialMessage {
        val id = item.optStringOrNull("Id", "id", "MessageId", "messageId") ?: UUID.randomUUID().toString()
        val convId = item.optStringOrNull("ConversationId", "conversationId") ?: defaultConvId
        val senderIdStr = item.optStringOrNull("SenderId", "senderId", "FromUserId", "fromUserId", "UserId", "userId") ?: session.user.id.toString()
        val senderId = runCatching { UUID.fromString(senderIdStr) }.getOrNull() ?: session.user.id
        val senderName = item.optStringOrNull("SenderName", "senderName", "FromName", "fromName", "Username", "username") ?: if (senderId == session.user.id) session.user.name else "Friend"
        val senderAvatarTag = item.optStringOrNull("SenderAvatarTag", "senderAvatarTag", "PrimaryImageTag", "primaryImageTag", "ImageTag", "imageTag")
        val senderAvatarUrl = toUserAvatarUrl(session, senderId, senderAvatarTag)
        val recipientIdStr = item.optStringOrNull("RecipientId", "recipientId", "ToUserId", "toUserId") ?: session.user.id.toString()
        val recipientId = runCatching { UUID.fromString(recipientIdStr) }.getOrNull() ?: session.user.id
        val content = item.optStringOrNull("Content", "content", "Text", "text", "Message", "message").orEmpty()
        val timestamp = item.optStringOrNull("Timestamp", "timestamp", "CreatedAt", "createdAt", "Date", "date")
        val isRead = item.optBooleanOrNull("IsRead", "isRead", "Read", "read") ?: false
        val isFromSelf = senderId == session.user.id

        return JellyfinSocialMessage(
            messageId = id,
            conversationId = convId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarTag = senderAvatarTag,
            senderAvatarUrl = senderAvatarUrl,
            recipientId = recipientId,
            content = content,
            timestamp = timestamp,
            isRead = isRead,
            isFromSelf = isFromSelf,
        )
    }

    private fun toUserAvatarUrl(session: JellyfinSession, userId: UUID, tag: String?): String {
        val baseUrl = session.server.url.trimEnd('/')
        return if (!tag.isNullOrBlank()) {
            "$baseUrl/Users/$userId/Images/Primary?tag=$tag&api_key=${session.accessToken}"
        } else {
            "$baseUrl/Users/$userId/Images/Primary?api_key=${session.accessToken}"
        }
    }

    private fun JSONObject.optStringOrNull(vararg keys: String): String? {
        for (key in keys) {
            if (!isNull(key)) {
                val value = optString(key, "")
                if (value.isNotBlank() && value != "null") return value
            }
        }
        return null
    }

    private fun JSONObject.optIntOrNull(vararg keys: String): Int? {
        for (key in keys) {
            if (!isNull(key)) {
                val value = optInt(key, Int.MIN_VALUE)
                if (value != Int.MIN_VALUE) return value
            }
        }
        return null
    }

    private fun JSONObject.optBooleanOrNull(vararg keys: String): Boolean? {
        for (key in keys) {
            if (!isNull(key)) {
                return optBoolean(key)
            }
        }
        return null
    }
}

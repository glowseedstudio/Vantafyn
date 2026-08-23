package dev.vantafyn.core.jellyfin

import android.util.Log
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
    suspend fun deleteOrClearConversation(session: JellyfinSession, conversationId: String, peerUserId: UUID? = null): JellyfinResult<Unit>
    suspend fun getUnreadSummary(session: JellyfinSession): JellyfinResult<JellyfinSocialSummary>
    suspend fun getDiscoverableUsers(session: JellyfinSession): JellyfinResult<List<JellyfinFriend>>
    suspend fun reportPresence(session: JellyfinSession)
}

class SdkJellyfinSocialRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinSocialRepository {

    override suspend fun reportPresence(session: JellyfinSession): Unit =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Sessions/Capabilities/Full", "POST")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val payload = JSONObject().apply {
                    put("PlayableMediaTypes", JSONArray(listOf("Video", "Audio")))
                    put("SupportedCommands", JSONArray(listOf("Play", "Pause", "Stop", "Seek", "DisplayMessage")))
                    put("SupportsMediaControl", true)
                    put("SupportsPersistentIdentifier", true)
                }.toString()
                conn.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                conn.disconnect()
                Log.d("VantafynSocial", "reportPresence [Sessions/Capabilities/Full] -> HTTP $code")
            }.onFailure {
                Log.w("VantafynSocial", "reportPresence error: ${it.message}")
            }
        }

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
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/friends")
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()
                Log.d("VantafynSocial", "getFriends [users/{id}/friends] -> HTTP $code: $body")

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Friends unavailable (HTTP $code)")
                }

                val friends = parseFriends(session, body)
                Log.d("VantafynSocial", "getFriends parsed: ${friends.size} friends")
                JellyfinResult.Success(friends)
            }.getOrElse { error ->
                Log.e("VantafynSocial", "getFriends error", error)
                JellyfinResult.Failure(error.message ?: "Failed to load friends", error)
            }
        }

    override suspend fun getFriendRequests(session: JellyfinSession): JellyfinResult<List<JellyfinFriendRequest>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/friends")
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()
                Log.d("VantafynSocial", "getFriendRequests [users/{id}/friends] -> HTTP $code (len=${body.length})")

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Friend requests unavailable (HTTP $code)")
                }

                val requests = parseFriendRequests(session, body)
                Log.d("VantafynSocial", "getFriendRequests total found: ${requests.size}")
                JellyfinResult.Success(requests)
            }.getOrElse { error ->
                Log.e("VantafynSocial", "getFriendRequests error", error)
                JellyfinResult.Failure(error.message ?: "Failed to load friend requests", error)
            }
        }

    override suspend fun sendFriendRequest(session: JellyfinSession, targetUsernameOrId: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                var targetGuidStr = targetUsernameOrId.trim()
                val parsedGuid = targetGuidStr.parseUuidOrNull()
                if (parsedGuid != null) {
                    targetGuidStr = parsedGuid.toString()
                } else {
                    val usersRes = getDiscoverableUsers(session)
                    val matched = (usersRes as? JellyfinResult.Success)?.value?.firstOrNull {
                        it.username.equals(targetUsernameOrId, ignoreCase = true) ||
                        it.displayName.equals(targetUsernameOrId, ignoreCase = true)
                    }
                    if (matched != null) {
                        targetGuidStr = matched.userId.toString()
                    }
                }

                Log.d("VantafynSocial", "sendFriendRequest sender=${session.user.id} target=$targetGuidStr")

                // C# AchievementBadges controller:
                //   POST /Plugins/AchievementBadges/users/{userId}/friends/{friendUserId}
                val endpoint = "Plugins/AchievementBadges/users/${session.user.id}/friends/$targetGuidStr"
                var success = false
                var lastCode = 0
                var lastErrorMessage = ""

                try {
                    val conn = session.openAuthenticatedConnection(endpoint)
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("Content-Length", "0")
                    conn.doOutput = false

                    lastCode = conn.responseCode
                    val respBody = if (lastCode in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    }
                    conn.disconnect()
                    Log.d("VantafynSocial", "POST [$endpoint] -> HTTP $lastCode, resp: $respBody")

                    val isExplicitFailure = respBody.contains("\"Success\":false", ignoreCase = true) ||
                        respBody.contains("\"success\":false", ignoreCase = true)

                    if (isExplicitFailure) {
                        val msg = runCatching { JSONObject(respBody).optString("Message") }.getOrNull()
                        if (!msg.isNullOrBlank()) lastErrorMessage = msg
                    }

                    success = lastCode in 200..299 && !isExplicitFailure
                } catch (e: Exception) {
                    Log.w("VantafynSocial", "POST [$endpoint] exception: ${e.message}")
                }

                if (success) {
                    Log.d("VantafynSocial", "sendFriendRequest SUCCESS for $targetGuidStr")
                    JellyfinResult.Success(Unit)
                } else {
                    val errMsg = lastErrorMessage.ifBlank { "Failed to send friend request (HTTP $lastCode)" }
                    Log.w("VantafynSocial", "sendFriendRequest FAILED: $errMsg")
                    JellyfinResult.Failure(errMsg)
                }
            }.getOrElse { error ->
                Log.e("VantafynSocial", "sendFriendRequest error", error)
                JellyfinResult.Failure(error.message ?: "Failed to send friend request", error)
            }
        }

    override suspend fun acceptFriendRequest(session: JellyfinSession, requestId: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                // C# AchievementBadges controller:
                //   POST /Plugins/AchievementBadges/users/{userId}/friends/{friendUserId}/accept
                val cleanId = requestId.parseUuidOrNull()?.toString() ?: requestId.trim()
                val endpoint = "Plugins/AchievementBadges/users/${session.user.id}/friends/$cleanId/accept"

                var success = false
                var lastCode = 0
                var lastErrorMessage = ""
                try {
                    val conn = session.openAuthenticatedConnection(endpoint)
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("Content-Length", "0")
                    conn.doOutput = false
                    lastCode = conn.responseCode
                    val respBody = if (lastCode in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    }
                    conn.disconnect()
                    Log.d("VantafynSocial", "acceptFriendRequest POST [$endpoint] -> HTTP $lastCode, resp: $respBody")
                    val isExplicitFailure = respBody.contains("\"Success\":false", ignoreCase = true)
                    if (isExplicitFailure) {
                        val msg = runCatching { JSONObject(respBody).optString("Message") }.getOrNull()
                        if (!msg.isNullOrBlank()) lastErrorMessage = msg
                    }
                    if (lastCode in 200..299 && !isExplicitFailure) {
                        success = true
                    }
                } catch (e: Exception) {
                    Log.w("VantafynSocial", "acceptFriendRequest exception: ${e.message}")
                }

                if (success) {
                    JellyfinResult.Success(Unit)
                } else {
                    JellyfinResult.Failure(lastErrorMessage.ifBlank { "Failed to accept friend request (HTTP $lastCode)" })
                }
            }.getOrElse { error ->
                Log.e("VantafynSocial", "acceptFriendRequest error", error)
                JellyfinResult.Failure(error.message ?: "Failed to accept friend request", error)
            }
        }

    override suspend fun declineOrRemoveFriend(session: JellyfinSession, targetId: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                // C# AchievementBadges controller:
                //   DELETE /Plugins/AchievementBadges/users/{userId}/friends/{friendUserId}
                val cleanId = targetId.parseUuidOrNull()?.toString() ?: targetId.trim()
                val endpoint = "Plugins/AchievementBadges/users/${session.user.id}/friends/$cleanId"

                var success = false
                var lastCode = 0
                try {
                    val conn = session.openAuthenticatedConnection(endpoint)
                    conn.requestMethod = "DELETE"
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    lastCode = conn.responseCode
                    val respBody = if (lastCode in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    }
                    conn.disconnect()
                    Log.d("VantafynSocial", "declineOrRemoveFriend DELETE [$endpoint] -> HTTP $lastCode, resp: $respBody")
                    val isExplicitFailure = respBody.contains("\"Success\":false", ignoreCase = true)
                    if (lastCode in 200..299 && !isExplicitFailure) {
                        success = true
                    }
                } catch (e: Exception) {
                    Log.w("VantafynSocial", "declineOrRemoveFriend exception: ${e.message}")
                }

                if (success) {
                    JellyfinResult.Success(Unit)
                } else {
                    JellyfinResult.Failure("Failed to remove friend or decline request (HTTP $lastCode)")
                }
            }.getOrElse { error ->
                Log.e("VantafynSocial", "declineOrRemoveFriend error", error)
                JellyfinResult.Failure(error.message ?: "Failed to remove friend or decline request", error)
            }
        }

    override suspend fun getConversations(session: JellyfinSession): JellyfinResult<List<JellyfinSocialConversation>> =
        withContext(ioDispatcher) {
            runCatching {
                // C# AchievementBadges controller:
                //   GET /Plugins/AchievementBadges/users/{userId}/messages/threads
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/messages/threads")
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()
                Log.d("VantafynSocial", "getConversations [messages/threads] -> HTTP $code (len=${body.length})")

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Conversations unavailable (HTTP $code)")
                }

                val conversations = parseConversations(session, body)
                JellyfinResult.Success(conversations)
            }.getOrElse { error ->
                Log.e("VantafynSocial", "getConversations error", error)
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
                // C# AchievementBadges controller:
                //   GET /Plugins/AchievementBadges/users/{userId}/conversations/{convId}/messages
                //   GET /Plugins/AchievementBadges/users/{userId}/messages/{otherUserId}
                val endpoints = mutableListOf<String>()
                if (conversationId.isNotBlank() && conversationId != peerUserId?.toString()) {
                    endpoints.add("Plugins/AchievementBadges/users/${session.user.id}/conversations/$conversationId/messages")
                }
                if (peerUserId != null) {
                    endpoints.add("Plugins/AchievementBadges/users/${session.user.id}/messages/$peerUserId")
                }
                if (endpoints.isEmpty() && conversationId.isNotBlank()) {
                    endpoints.add("Plugins/AchievementBadges/users/${session.user.id}/conversations/$conversationId/messages")
                }

                var body = ""
                var code = 0
                for (endpoint in endpoints) {
                    val conn = session.openAuthenticatedConnection(endpoint)
                    code = conn.responseCode
                    body = if (code in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else {
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    }
                    conn.disconnect()
                    if (code in 200..299) break
                }

                if (code !in 200..299) {
                    return@withContext JellyfinResult.Failure("Messages unavailable (HTTP $code)")
                }

                val messages = parseMessages(session, body, conversationId)
                JellyfinResult.Success(messages)
            }.getOrElse { error ->
                Log.e("VantafynSocial", "getMessages error", error)
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
                // C# AchievementBadges controller:
                //   POST /Plugins/AchievementBadges/users/{userId}/conversations/{convId}/messages (for existing convo)
                //   POST /Plugins/AchievementBadges/users/{userId}/messages/{otherUserId} (for 1:1 DM)
                val endpoint = if (!conversationId.isNullOrBlank() && conversationId != recipientId.toString()) {
                    "Plugins/AchievementBadges/users/${session.user.id}/conversations/$conversationId/messages"
                } else {
                    "Plugins/AchievementBadges/users/${session.user.id}/messages/$recipientId"
                }

                val conn = session.openAuthenticatedConnection(endpoint)
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val payload = JSONObject().apply {
                    put("text", text)
                    put("attachmentId", JSONObject.NULL)
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
                    val root = JSONObject(body.trim())
                    val sentObj = root.optJSONObject("Sent") ?: root.optJSONObject("sent") ?: root
                    val returnedConvId = root.optStringOrNull("ConversationId", "conversationId") ?: conversationId ?: recipientId.toString()
                    parseSingleMessage(session, sentObj, returnedConvId)
                } else {
                    JellyfinSocialMessage(
                        messageId = UUID.randomUUID().toString(),
                        conversationId = conversationId ?: recipientId.toString(),
                        senderId = session.user.id,
                        senderName = session.user.name,
                        senderAvatarTag = null,
                        senderAvatarUrl = toUserAvatarUrl(session, session.user.id, null),
                        recipientId = recipientId,
                        content = text,
                        timestamp = null,
                        isRead = true,
                        isFromSelf = true,
                    )
                }
                JellyfinResult.Success(parsed)
            }.getOrElse { error ->
                Log.e("VantafynSocial", "sendMessage error", error)
                JellyfinResult.Failure(error.message ?: "Failed to send message", error)
            }
        }

    override suspend fun markConversationRead(session: JellyfinSession, conversationId: String): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            // Getting messages automatically marks them read on the AchievementBadges plugin server.
            JellyfinResult.Success(Unit)
        }

    override suspend fun deleteOrClearConversation(
        session: JellyfinSession,
        conversationId: String,
        peerUserId: UUID?,
    ): JellyfinResult<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val endpoints = mutableListOf<String>()
                if (conversationId.isNotBlank() && conversationId != peerUserId?.toString()) {
                    endpoints.add("Plugins/AchievementBadges/users/${session.user.id}/conversations/$conversationId")
                    endpoints.add("Plugins/AchievementBadges/users/${session.user.id}/conversations/$conversationId/messages")
                }
                if (peerUserId != null) {
                    endpoints.add("Plugins/AchievementBadges/users/${session.user.id}/messages/$peerUserId")
                }

                for (endpoint in endpoints) {
                    try {
                        val conn = session.openAuthenticatedConnection(endpoint)
                        conn.requestMethod = "DELETE"
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        val code = conn.responseCode
                        conn.disconnect()
                        Log.d("VantafynSocial", "deleteConversation DELETE [$endpoint] -> HTTP $code")
                    } catch (e: Exception) {
                        Log.w("VantafynSocial", "deleteConversation DELETE [$endpoint] exception: ${e.message}")
                    }
                }
                JellyfinResult.Success(Unit)
            }.getOrElse { error ->
                Log.e("VantafynSocial", "deleteConversation error", error)
                JellyfinResult.Failure(error.message ?: "Failed to clear conversation", error)
            }
        }

    override suspend fun getUnreadSummary(session: JellyfinSession): JellyfinResult<JellyfinSocialSummary> =
        withContext(ioDispatcher) {
            runCatching {
                // C# AchievementBadges controller:
                //   GET /Plugins/AchievementBadges/users/{userId}/messages/unread-count
                val conn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/messages/unread-count")
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                }
                conn.disconnect()

                val unreadCount = if (code in 200..299 && body.isNotBlank() && body.trim().startsWith("{")) {
                    JSONObject(body.trim()).optIntOrNull("Count", "count") ?: 0
                } else {
                    0
                }

                // Query friends count & pending request count from friends endpoint
                val friendsRes = getFriends(session)
                val friendCount = (friendsRes as? JellyfinResult.Success)?.value?.size ?: 0
                val reqsRes = getFriendRequests(session)
                val incCount = (reqsRes as? JellyfinResult.Success)?.value?.count { it.isIncoming } ?: 0
                val outCount = (reqsRes as? JellyfinResult.Success)?.value?.count { !it.isIncoming } ?: 0

                JellyfinResult.Success(
                    JellyfinSocialSummary(
                        friendCount = friendCount,
                        incomingRequestCount = incCount,
                        outgoingRequestCount = outCount,
                        unreadMessageCount = unreadCount,
                    ),
                )
            }.getOrElse { error ->
                Log.e("VantafynSocial", "getUnreadSummary error", error)
                JellyfinResult.Failure(error.message ?: "Failed to fetch unread count", error)
            }
        }

    override suspend fun getDiscoverableUsers(session: JellyfinSession): JellyfinResult<List<JellyfinFriend>> =
        withContext(ioDispatcher) {
            runCatching {
                val userMap = mutableMapOf<UUID, JellyfinFriend>()

                // 1. Fetch server-wide members from Jellyfin standard Users / Users/Public endpoints
                for (uEndpoint in listOf("Users", "Users/Public")) {
                    runCatching {
                        val conn = session.openAuthenticatedConnection(uEndpoint)
                        val code = conn.responseCode
                        val body = if (code in 200..299) {
                            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        } else ""
                        conn.disconnect()
                        Log.d("VantafynSocial", "discoverable [$uEndpoint] -> HTTP $code (len=${body.length})")

                        if (body.isNotBlank() && body.trim() != "[]" && body.trim() != "{}") {
                            parseFriends(session, body).forEach {
                                if (it.userId != session.user.id && !userMap.containsKey(it.userId)) {
                                    userMap[it.userId] = it
                                }
                            }
                        }
                    }
                }

                // 2. Fetch AchievementBadges leaderboard & users for ranks, scores, and real-time online status
                for (pluginEndpoint in listOf("Plugins/AchievementBadges/leaderboard", "Plugins/AchievementBadges/users")) {
                    runCatching {
                        val lbConn = session.openAuthenticatedConnection(pluginEndpoint)
                        val lbCode = lbConn.responseCode
                        val lbBody = if (lbCode in 200..299) {
                            lbConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        } else ""
                        lbConn.disconnect()
                        Log.d("VantafynSocial", "discoverable [$pluginEndpoint] -> HTTP $lbCode (len=${lbBody.length})")

                        if (lbBody.isNotBlank() && lbBody.trim() != "[]" && lbBody.trim() != "{}") {
                            parseFriends(session, lbBody).forEach { lbUser ->
                                if (lbUser.userId != session.user.id) {
                                    val existing = userMap[lbUser.userId]
                                    if (existing != null) {
                                        userMap[lbUser.userId] = existing.copy(
                                            rankName = if (lbUser.rankName != "Rookie") lbUser.rankName else existing.rankName,
                                            rankTier = if (lbUser.rankTier > 1) lbUser.rankTier else existing.rankTier,
                                            currentScore = if (lbUser.currentScore > 0) lbUser.currentScore else existing.currentScore,
                                            isOnline = lbUser.isOnline || existing.isOnline,
                                            currentlyWatching = lbUser.currentlyWatching ?: existing.currentlyWatching,
                                            lastSeen = lbUser.lastSeen ?: existing.lastSeen,
                                            avatarUrl = lbUser.avatarUrl.takeIf { !it.isNullOrBlank() } ?: existing.avatarUrl,
                                        )
                                    } else {
                                        userMap[lbUser.userId] = lbUser
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Query active Sessions (if admin or self)
                val activeSessions = mutableListOf<JSONObject>()
                runCatching {
                    val sConn = session.openAuthenticatedConnection("Sessions")
                    val sCode = sConn.responseCode
                    val sBody = if (sCode in 200..299) {
                        sConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else ""
                    sConn.disconnect()
                    Log.d("VantafynSocial", "discoverable [Sessions] -> HTTP $sCode (len=${sBody.length})")
                    if (sBody.isNotBlank() && sBody.trim().startsWith("[")) {
                        val sArray = JSONArray(sBody.trim())
                        for (i in 0 until sArray.length()) {
                            sArray.optJSONObject(i)?.let { activeSessions.add(it) }
                        }
                    }
                }

                for (sItem in activeSessions) {
                    val uIdStr = sItem.optStringOrNull("UserId", "userId") ?: continue
                    val uId = uIdStr.parseUuidOrNull() ?: continue
                    if (uId == session.user.id) continue
                    val uName = sItem.optStringOrNull("UserName", "userName", "Username", "username") ?: "User"
                    val devName = sItem.optStringOrNull("DeviceName", "deviceName", "Client", "client")
                    val nowPlaying = sItem.optJSONObject("NowPlayingItem")
                    val lastActivityStr = sItem.optStringOrNull("LastActivityDate", "lastActivityDate")
                    val lastActivityTime = parseIsoOrEpochMillis(lastActivityStr)
                    val isActive = sItem.optBooleanOrNull("IsActive", "isActive") ?: true
                    val isTrulyActive = nowPlaying != null || (isActive && (lastActivityTime == 0L || (System.currentTimeMillis() - lastActivityTime) < 5 * 60 * 1000L))
                    if (!isTrulyActive) continue

                    val mediaTitle = formatNowPlayingDescription(nowPlaying, nowPlaying?.optStringOrNull("Name", "name"))
                    val watchingDesc = when {
                        !mediaTitle.isNullOrBlank() -> mediaTitle
                        !devName.isNullOrBlank() -> "Online on $devName"
                        else -> "Active now"
                    }

                    val existing = userMap[uId]
                    if (existing != null) {
                        userMap[uId] = existing.copy(
                            isOnline = true,
                            currentlyWatching = mediaTitle ?: existing.currentlyWatching ?: "Active now",
                        )
                    } else {
                        userMap[uId] = JellyfinFriend(
                            userId = uId,
                            username = uName,
                            displayName = uName,
                            avatarTag = null,
                            avatarUrl = toUserAvatarUrl(session, uId, null),
                            rankName = "Server Member",
                            rankTier = 1,
                            currentScore = 0,
                            isOnline = true,
                            lastSeen = "Now",
                            currentlyWatching = watchingDesc,
                            equippedBadgeName = null,
                            equippedBadgeIcon = null,
                        )
                    }
                }

                // 4. Apply IsOnline & NowPlaying from the AchievementBadges friends endpoint
                //    The plugin calculates Online via ISessionManager internally, ensuring accurate
                //    online status for non-admin accounts.
                runCatching {
                    val frConn = session.openAuthenticatedConnection("Plugins/AchievementBadges/users/${session.user.id}/friends")
                    val frCode = frConn.responseCode
                    val frBody = if (frCode in 200..299) {
                        frConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else ""
                    frConn.disconnect()
                    Log.d("VantafynSocial", "discoverable [friends isOnline pass] -> HTTP $frCode (len=${frBody.length})")

                    if (frBody.isNotBlank() && frBody.trim().startsWith("{")) {
                        val frJson = JSONObject(frBody.trim())
                        for (key in listOf("Friends", "friends", "Incoming", "incoming", "Outgoing", "outgoing")) {
                            val arr = frJson.optJSONArray(key) ?: continue
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                val userObj = item.optJSONObject("User") ?: item.optJSONObject("user") ?: item
                                val idStr = userObj.optStringOrNull("Id", "id", "UserId", "userId") ?: continue
                                val uid = idStr.parseUuidOrNull() ?: continue
                                if (uid == session.user.id) continue
                                val isOnline = item.optBooleanOrNull("Online", "online", "IsOnline", "isOnline")
                                     ?: userObj.optBooleanOrNull("Online", "online", "IsOnline", "isOnline")
                                     ?: false
                                val watchingObj = item.optJSONObject("NowPlaying") ?: item.optJSONObject("nowPlaying")
                                val watching = formatNowPlayingDescription(
                                    watchingObj,
                                    watchingObj?.optStringOrNull("Name", "name")
                                        ?: item.optStringOrNull("CurrentlyWatching", "currentlyWatching")
                                )
                                val existing = userMap[uid]
                                if (existing != null) {
                                    if (isOnline || existing.isOnline) {
                                        userMap[uid] = existing.copy(
                                            isOnline = true,
                                            currentlyWatching = watching ?: existing.currentlyWatching ?: "Active now",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter out current user and sort online users first
                val resultList = userMap.values
                    .filter { it.userId != session.user.id }
                    .sortedWith(compareByDescending<JellyfinFriend> { it.isOnline }.thenBy { it.displayName })

                Log.d("VantafynSocial", "getDiscoverableUsers result total: ${resultList.size}")
                JellyfinResult.Success(resultList)
            }.getOrElse { error ->
                Log.e("VantafynSocial", "getDiscoverableUsers error", error)
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
                json.optJSONArray("Friends")
                    ?: json.optJSONArray("friends")
                    ?: json.optJSONArray("Items")
                    ?: json.optJSONArray("items")
                    ?: json.optJSONArray("Leaderboard")
                    ?: json.optJSONArray("leaderboard")
                    ?: json.optJSONArray("Users")
                    ?: json.optJSONArray("users")
                    ?: json.optJSONArray("Entries")
                    ?: json.optJSONArray("entries")
                    ?: json.optJSONArray("Members")
                    ?: json.optJSONArray("members")
                    ?: json.optJSONArray("Results")
                    ?: json.optJSONArray("results")
                    ?: json.optJSONArray("Data")
                    ?: json.optJSONArray("data")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val result = mutableListOf<JellyfinFriend>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val nestedUser = item.optJSONObject("User")
                ?: item.optJSONObject("user")
                ?: item.optJSONObject("Friend")
                ?: item.optJSONObject("friend")
                ?: item.optJSONObject("Member")
                ?: item.optJSONObject("member")

            val idStr = nestedUser?.optStringOrNull("Id", "id", "UserId", "userId")
                ?: item.optStringOrNull(
                    "UserId", "userId",
                    "Id", "id",
                    "User_Id", "user_id",
                    "TargetUserId", "targetUserId",
                    "PeerId", "peerId",
                ) ?: continue

            val userId = idStr.parseUuidOrNull() ?: continue

            val username = nestedUser?.optStringOrNull("Username", "username", "Name", "name", "UserName", "userName")
                ?: item.optStringOrNull(
                    "UserName", "userName",
                    "Username", "username",
                    "Name", "name",
                    "DisplayName", "displayName",
                ) ?: "User"

            val displayName = nestedUser?.optStringOrNull("DisplayName", "displayName", "Name", "name")
                ?: item.optStringOrNull(
                    "DisplayName", "displayName",
                    "UserName", "userName",
                    "Name", "name",
                    "Username", "username",
                ) ?: username

            val avatarTag = nestedUser?.optStringOrNull("PrimaryImageTag", "primaryImageTag", "AvatarTag", "avatarTag", "ImageTag", "imageTag")
                ?: item.optStringOrNull("PrimaryImageTag", "primaryImageTag", "AvatarTag", "avatarTag", "ImageTag", "imageTag", "UserAvatarTag")

            val avatarUrl = toUserAvatarUrl(session, userId, avatarTag)
            val rankName = item.optStringOrNull("RankName", "rankName", "Rank", "rank", "TierName", "tierName") ?: "Rookie"
            val rankTier = item.optIntOrNull("RankTier", "rankTier", "Tier", "tier", "Level", "level") ?: 1
            val currentScore = item.optIntOrNull("CurrentScore", "currentScore", "Score", "score", "Points", "points", "TotalScore") ?: 0
            val rawOnline = item.optBooleanOrNull("Online", "online", "IsOnline", "isOnline", "IsActive", "isActive", "Active", "active")
                ?: nestedUser?.optBooleanOrNull("Online", "online", "IsOnline", "isOnline", "IsActive", "isActive", "Active", "active")
                ?: false
            val lastSeen = item.optStringOrNull("LastActivityDate", "lastActivityDate", "LastSeenDate", "lastSeenDate", "LastSeen", "lastSeen", "LastActivity", "lastActivity")
                ?: nestedUser?.optStringOrNull("LastActivityDate", "lastActivityDate", "LastSeenDate", "lastSeenDate", "LastSeen", "lastSeen", "LastActivity", "lastActivity")
            val lastActivityTime = parseSocialTimestampToMillis(lastSeen).takeIf { it > 0L }
                ?: parseIsoOrEpochMillis(lastSeen)
            val isRecentActivity = lastActivityTime > 0L && (System.currentTimeMillis() - lastActivityTime) < 5 * 60 * 1000L
            val nowPlayingString = item.optStringOrNull(
                "NowPlaying", "nowPlaying",
                "CurrentlyWatching", "currentlyWatching",
                "CurrentlyPlaying", "currentlyPlaying",
                "Watching", "watching",
                "MediaTitle", "mediaTitle",
                "Activity", "activity",
                "Playing", "playing",
                "Title", "title",
                "NowPlayingName", "nowPlayingName",
                "ItemName", "itemName",
            ) ?: nestedUser?.optStringOrNull(
                "NowPlaying", "nowPlaying",
                "CurrentlyWatching", "currentlyWatching",
                "CurrentlyPlaying", "currentlyPlaying",
                "Watching", "watching",
                "MediaTitle", "mediaTitle",
                "Activity", "activity",
                "Playing", "playing",
                "Title", "title",
                "NowPlayingName", "nowPlayingName",
                "ItemName", "itemName",
            )

            val nowPlayingObj = item.optJSONObject("NowPlayingItem")
                ?: item.optJSONObject("nowPlayingItem")
                ?: item.optJSONObject("NowPlaying")
                ?: item.optJSONObject("nowPlaying")
                ?: nestedUser?.optJSONObject("NowPlayingItem")
                ?: nestedUser?.optJSONObject("nowPlayingItem")
                ?: nestedUser?.optJSONObject("NowPlaying")
                ?: nestedUser?.optJSONObject("nowPlaying")

            val devName = item.optStringOrNull("DeviceName", "deviceName", "Client", "client")
                ?: nestedUser?.optStringOrNull("DeviceName", "deviceName", "Client", "client")
            val lastWatchedObj = item.optJSONObject("LastWatched") ?: item.optJSONObject("lastWatched")
            val mediaDescription = formatNowPlayingDescription(nowPlayingObj, nowPlayingString)
            val isOnline = rawOnline || nowPlayingObj != null || isRecentActivity || !nowPlayingString.isNullOrBlank()
            val currentlyWatching = when {
                !mediaDescription.isNullOrBlank() -> mediaDescription
                isOnline && !devName.isNullOrBlank() -> "Online on $devName"
                isOnline -> "Active now"
                else -> formatNowPlayingDescription(lastWatchedObj, null)?.let { "Last watched $it" }
            }
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
                lastSeen = if (isOnline) "Now" else lastSeen,
                currentlyWatching = currentlyWatching,
                equippedBadgeName = equippedBadgeName,
                equippedBadgeIcon = equippedBadgeIcon,
            )
        }
        return result
    }

    private fun parseFriendRequests(session: JellyfinSession, body: String): List<JellyfinFriendRequest> {
        val trimmed = body.trim()
        val arrayEntries = mutableListOf<Pair<JSONArray, Boolean?>>()
        if (trimmed.startsWith("[")) {
            runCatching { arrayEntries.add(JSONArray(trimmed) to null) }
        } else if (trimmed.startsWith("{")) {
            runCatching {
                val json = JSONObject(trimmed)
                json.optJSONArray("Incoming")?.let { arrayEntries.add(it to true) }
                json.optJSONArray("incoming")?.let { arrayEntries.add(it to true) }
                json.optJSONArray("IncomingRequests")?.let { arrayEntries.add(it to true) }
                json.optJSONArray("incomingRequests")?.let { arrayEntries.add(it to true) }
                json.optJSONArray("Outgoing")?.let { arrayEntries.add(it to false) }
                json.optJSONArray("outgoing")?.let { arrayEntries.add(it to false) }
                json.optJSONArray("OutgoingRequests")?.let { arrayEntries.add(it to false) }
                json.optJSONArray("outgoingRequests")?.let { arrayEntries.add(it to false) }
                json.optJSONArray("pending")?.let { arrayEntries.add(it to false) }
                json.optJSONArray("Pending")?.let { arrayEntries.add(it to false) }
                json.optJSONArray("requests")?.let { arrayEntries.add(it to null) }
                json.optJSONArray("Requests")?.let { arrayEntries.add(it to null) }
                json.optJSONArray("items")?.let { arrayEntries.add(it to null) }
                json.optJSONArray("Items")?.let { arrayEntries.add(it to null) }
            }
        }

        val result = mutableListOf<JellyfinFriendRequest>()
        for ((array, forcedDirection) in arrayEntries) {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue

                val explicitDirection = item.optStringOrNull("Direction", "direction", "Type", "type")
                val explicitIsIncoming = item.optBooleanOrNull("IsIncoming", "isIncoming", "Incoming", "incoming")

                val senderObj = item.optJSONObject("Sender") ?: item.optJSONObject("sender") ?: item.optJSONObject("FromUser")
                val receiverObj = item.optJSONObject("Receiver") ?: item.optJSONObject("receiver") ?: item.optJSONObject("ToUser")

                val rawUserIdStr = item.optStringOrNull(
                    "UserId", "userId",
                    "Id", "id",
                    "SenderId", "senderId",
                    "FromUserId", "fromUserId",
                    "RequesterId", "requesterId",
                )

                val senderIdStr = senderObj?.optStringOrNull("Id", "id", "UserId", "userId")
                    ?: item.optStringOrNull("SenderId", "senderId", "FromUserId", "fromUserId", "RequesterId", "requesterId")
                val receiverIdStr = receiverObj?.optStringOrNull("Id", "id", "UserId", "userId")
                    ?: item.optStringOrNull("ReceiverId", "receiverId", "ToUserId", "toUserId", "TargetUserId", "targetUserId")

                val parsedSenderId = senderIdStr?.parseUuidOrNull()
                val parsedReceiverId = receiverIdStr?.parseUuidOrNull()
                val parsedRawUserId = rawUserIdStr?.parseUuidOrNull()

                val isIncoming = when {
                    forcedDirection != null -> forcedDirection
                    explicitIsIncoming != null -> explicitIsIncoming
                    explicitDirection?.contains("In", ignoreCase = true) == true -> true
                    explicitDirection?.contains("Out", ignoreCase = true) == true -> false
                    parsedReceiverId == session.user.id -> true
                    parsedSenderId == session.user.id -> false
                    parsedSenderId != null && parsedSenderId != session.user.id -> true
                    else -> true
                }

                val finalSenderId = when {
                    isIncoming -> parsedSenderId ?: parsedRawUserId ?: session.user.id
                    else -> session.user.id
                }

                val finalReceiverId = when {
                    isIncoming -> session.user.id
                    else -> parsedReceiverId ?: parsedRawUserId ?: session.user.id
                }

                val targetOtherId = if (isIncoming) finalSenderId else finalReceiverId

                val otherName = item.optStringOrNull(
                    "UserName", "userName",
                    "Username", "username",
                    "DisplayName", "displayName",
                    "Name", "name",
                ) ?: "User"

                val senderName = if (isIncoming) otherName else session.user.name
                val receiverName = if (isIncoming) session.user.name else otherName

                val avatarTag = item.optStringOrNull("PrimaryImageTag", "primaryImageTag", "AvatarTag", "avatarTag")
                val avatarUrl = toUserAvatarUrl(session, targetOtherId, avatarTag)

                result += JellyfinFriendRequest(
                    id = targetOtherId.toString(),
                    senderId = finalSenderId,
                    senderName = senderName,
                    senderAvatarTag = avatarTag,
                    senderAvatarUrl = avatarUrl,
                    senderRankTier = item.optIntOrNull("SenderRankTier", "RankTier", "tier", "Level") ?: 1,
                    receiverId = finalReceiverId,
                    receiverName = receiverName,
                    createdAt = item.optStringOrNull("CreatedAt", "createdAt", "Date", "date"),
                    isIncoming = isIncoming,
                )
            }
        }
        return result
    }

    private fun parseConversations(session: JellyfinSession, body: String): List<JellyfinSocialConversation> {
        val trimmed = body.trim()
        val array = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val json = JSONObject(trimmed)
                json.optJSONArray("Threads")
                    ?: json.optJSONArray("threads")
                    ?: json.optJSONArray("Conversations")
                    ?: json.optJSONArray("conversations")
                    ?: json.optJSONArray("Items")
                    ?: json.optJSONArray("items")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val result = mutableListOf<JellyfinSocialConversation>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val otherUserIdStr = item.optStringOrNull("otherUserId", "OtherUserId", "peerUserId", "userId", "peerId")
            val participantsArr = item.optJSONArray("participants")
            val participantIdStr = if (participantsArr != null && participantsArr.length() > 0) {
                participantsArr.optJSONObject(0)?.optStringOrNull("userId", "UserId")
            } else null

            val peerUserId = otherUserIdStr?.parseUuidOrNull()
                ?: participantIdStr?.parseUuidOrNull()
                ?: continue

            val convId = item.optStringOrNull("conversationId", "ConversationId", "id", "Id") ?: peerUserId.toString()
            val peerName = item.optStringOrNull("otherUserName", "OtherUserName", "peerName", "title", "username", "displayName", "name") ?: "Friend"
            val peerAvatarUrl = toUserAvatarUrl(session, peerUserId, null)
            val lastMessageText = item.optStringOrNull("lastMessage", "LastMessage", "content", "text")
            val lastMessageTimestamp = item.optStringOrNull("lastAt", "LastAt", "sentAt", "timestamp", "date")
            val unreadCount = item.optIntOrNull("unreadCount", "UnreadCount", "unread") ?: 0

            result += JellyfinSocialConversation(
                conversationId = convId,
                peerUserId = peerUserId,
                peerName = peerName,
                peerAvatarTag = null,
                peerAvatarUrl = peerAvatarUrl,
                peerRankTier = 1,
                peerIsOnline = false,
                lastMessageText = lastMessageText,
                lastMessageTimestamp = lastMessageTimestamp,
                unreadCount = unreadCount,
                lastSenderId = null,
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
                json.optJSONArray("Messages")
                    ?: json.optJSONArray("messages")
                    ?: json.optJSONArray("Items")
                    ?: json.optJSONArray("items")
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
        val id = item.optStringOrNull("id", "Id", "messageId", "MessageId") ?: UUID.randomUUID().toString()
        val convId = item.optStringOrNull("conversationId", "ConversationId") ?: defaultConvId
        val senderIdStr = item.optStringOrNull("fromUserId", "FromUserId", "senderId", "SenderId", "userId") ?: session.user.id.toString()
        val senderId = senderIdStr.parseUuidOrNull() ?: session.user.id
        val senderName = item.optStringOrNull("fromUserName", "FromUserName", "senderName", "username") ?: if (senderId == session.user.id) session.user.name else "Friend"
        val senderAvatarUrl = toUserAvatarUrl(session, senderId, null)
        val recipientIdStr = item.optStringOrNull("toUserId", "ToUserId", "recipientId", "RecipientId") ?: session.user.id.toString()
        val recipientId = recipientIdStr.parseUuidOrNull() ?: session.user.id
        val content = item.optStringOrNull("text", "Text", "content", "message").orEmpty()
        val timestamp = item.optStringOrNull("sentAt", "SentAt", "timestamp", "createdAt", "date")
        val isRead = item.optStringOrNull("readAt", "ReadAt") != null || item.optBooleanOrNull("isRead", "read") == true
        val isFromSelf = senderId == session.user.id

        return JellyfinSocialMessage(
            messageId = id,
            conversationId = convId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarTag = null,
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

    private fun String?.parseUuidOrNull(): UUID? {
        if (this.isNullOrBlank()) return null
        val trimmed = this.trim()
        val clean = trimmed.replace("-", "")
        if (clean.length == 32) {
            return runCatching {
                val formatted = "${clean.substring(0, 8)}-${clean.substring(8, 12)}-${clean.substring(12, 16)}-${clean.substring(16, 20)}-${clean.substring(20, 32)}"
                UUID.fromString(formatted)
            }.getOrNull()
        }
        return runCatching { UUID.fromString(trimmed) }.getOrNull()
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
                val raw = opt(key)
                if (raw is Boolean) return raw
                if (raw is Number) return raw.toInt() == 1
                if (raw is String) return raw.equals("true", ignoreCase = true)
                return optBoolean(key, false)
            }
        }
        return null
    }

    private fun formatNowPlayingDescription(nowPlayingObj: JSONObject?, fallback: String? = null): String? {
        if (nowPlayingObj != null) {
            val seriesName = nowPlayingObj.optStringOrNull("SeriesName", "seriesName")
            val episodeName = nowPlayingObj.optStringOrNull("Name", "name", "EpisodeName", "episodeName", "Title", "title")

            if (!seriesName.isNullOrBlank() && !episodeName.isNullOrBlank() && !seriesName.equals(episodeName, ignoreCase = true)) {
                return "$seriesName • $episodeName"
            }
            if (!seriesName.isNullOrBlank()) return seriesName
            if (!episodeName.isNullOrBlank()) return episodeName
        }

        if (!fallback.isNullOrBlank()) {
            val trimmed = fallback.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                runCatching {
                    val obj = JSONObject(trimmed)
                    val sName = obj.optStringOrNull("SeriesName", "seriesName")
                    val eName = obj.optStringOrNull("Name", "name", "EpisodeName", "episodeName", "Title", "title")
                    if (!sName.isNullOrBlank() && !eName.isNullOrBlank() && !sName.equals(eName, ignoreCase = true)) {
                        return "$sName • $eName"
                    }
                    if (!sName.isNullOrBlank()) return sName
                    if (!eName.isNullOrBlank()) return eName
                }
            }
            return trimmed
        }

        return null
    }

    private fun parseIsoOrEpochMillis(timestampStr: String?): Long {
        if (timestampStr.isNullOrBlank()) return 0L
        return try {
            java.time.Instant.parse(timestampStr).toEpochMilli()
        } catch (e: Exception) {
            try {
                timestampStr.toLong()
            } catch (ex: Exception) {
                0L
            }
        }
    }
}

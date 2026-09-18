package dev.vantafyn.core.integrations.push

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

sealed interface VantafynPushEvent {
    data class DebugTest(
        val title: String,
        val message: String,
        val timestamp: Long,
    ) : VantafynPushEvent

    data class ChatMessage(
        val senderId: String,
        val senderName: String,
        val conversationId: String,
        val messageText: String,
        val timestamp: Long,
    ) : VantafynPushEvent

    data class AchievementUnlock(
        val userId: String,
        val achievementId: String,
        val title: String,
        val description: String,
        val timestamp: Long,
    ) : VantafynPushEvent

    data class Unknown(val rawJson: String) : VantafynPushEvent
}

sealed interface PushNavigationTarget {
    data class Chat(val conversationId: String, val senderId: String, val senderName: String?) : PushNavigationTarget
    data class Achievement(val badgeId: String) : PushNavigationTarget
}

/**
 * Internal message router for incoming UnifiedPush payloads.
 * Exposes shared flows for raw payloads and typed [VantafynPushEvent] instances
 * that Vantafyn handlers (chat, achievements, sync) can observe reactively without polling.
 */
object UnifiedPushPayloadDispatcher {
    private const val TAG = "UnifiedPushDispatcher"

    @Volatile
    var isAppInForeground: Boolean = false

    private val _incomingPayloads = MutableSharedFlow<UnifiedPushPayload>(extraBufferCapacity = 64)
    val incomingPayloads: SharedFlow<UnifiedPushPayload> = _incomingPayloads.asSharedFlow()

    private val _events = MutableSharedFlow<VantafynPushEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<VantafynPushEvent> = _events.asSharedFlow()

    private val _pendingNavigation = MutableSharedFlow<PushNavigationTarget>(replay = 1, extraBufferCapacity = 8)
    val pendingNavigation: SharedFlow<PushNavigationTarget> = _pendingNavigation.asSharedFlow()

    fun navigateTo(target: PushNavigationTarget) {
        Log.i(TAG, "Queued push notification navigation target: $target")
        _pendingNavigation.tryEmit(target)
    }

    fun clearPendingNavigation() {
        _pendingNavigation.resetReplayCache()
    }


    fun dispatch(payload: UnifiedPushPayload) {
        Log.i(TAG, "Received push payload: size=${payload.sizeBytes} bytes, isDecrypted=${payload.isDecrypted}, timestamp=${payload.receivedAtMillis}")
        _incomingPayloads.tryEmit(payload)

        val jsonString = runCatching { String(payload.content, Charsets.UTF_8) }.getOrNull() ?: return
        val event = parseEvent(jsonString)
        if (event != null) {
            _events.tryEmit(event)
        }
    }

    private fun parseEvent(jsonStr: String): VantafynPushEvent? {
        return runCatching {
            val json = JSONObject(jsonStr)
            val type = json.optString("type")
            when (type) {
                "debug_test" -> VantafynPushEvent.DebugTest(
                    title = json.optString("title", "Vantafyn Push Test"),
                    message = json.optString("message", "UnifiedPush test notification received"),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                )
                "chat_message" -> VantafynPushEvent.ChatMessage(
                    senderId = json.optString("senderId"),
                    senderName = json.optString("senderName", "Friend"),
                    conversationId = json.optString("conversationId"),
                    messageText = json.optString("messageText"),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                )
                "achievement_unlock" -> VantafynPushEvent.AchievementUnlock(
                    userId = json.optString("userId"),
                    achievementId = json.optString("achievementId"),
                    title = json.optString("title"),
                    description = json.optString("description"),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                )
                else -> VantafynPushEvent.Unknown(jsonStr)
            }
        }.getOrElse { e ->
            Log.w(TAG, "Unable to parse push payload as JSON event", e)
            null
        }
    }
}

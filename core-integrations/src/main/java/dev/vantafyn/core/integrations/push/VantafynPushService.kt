package dev.vantafyn.core.integrations.push

import android.util.Log
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/**
 * Android Service implementation for UnifiedPush connector callbacks.
 * Routes incoming events directly to [UnifiedPushManager] and [UnifiedPushPayloadDispatcher]
 * without leaking sensitive endpoints or payload contents in system logs.
 */
class VantafynPushService : PushService() {

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        // Log generic event without exposing the actual endpoint URL
        Log.i(TAG, "New UnifiedPush endpoint received for instance '$instance'")
        val manager = UnifiedPushManager.getInstance(applicationContext)
        manager.onEndpointReceived(endpoint.url)
    }

    override fun onMessage(message: PushMessage, instance: String) {
        val payload = UnifiedPushPayload(
            content = message.content,
            isDecrypted = message.decrypted,
            receivedAtMillis = System.currentTimeMillis(),
        )

        val contentString = runCatching { String(message.content, Charsets.UTF_8) }.getOrNull()
        Log.i(TAG, "UnifiedPush onMessage received: instance='$instance', bytes=${message.content.size}, decrypted=${message.decrypted}, content=$contentString")

        // Route to the internal dispatcher
        UnifiedPushPayloadDispatcher.dispatch(payload)

        // Record message reception in manager
        val manager = UnifiedPushManager.getInstance(applicationContext)
        manager.onPushReceived(payload.sizeBytes)

        // Display notifications based on parsed event
        if (contentString != null) {
            val json = runCatching { org.json.JSONObject(contentString) }.getOrNull()
            when (json?.optString("type")) {
                "chat_message" -> {
                    val senderName = json.optString("senderName", "Friend")
                    val messageText = json.optString("messageText", "New message received")
                    val conversationId = json.optString("conversationId", "")
                    val senderId = json.optString("senderId", "")
                    VantafynPushNotifier.showChatMessage(applicationContext, senderName, messageText, conversationId, senderId)
                }
                "achievement_unlock" -> {
                    val title = json.optString("title", "Achievement Unlocked")
                    val desc = json.optString("description", "")
                    val badgeId = json.optString("achievementId", "")
                    VantafynPushNotifier.showAchievementUnlock(applicationContext, title, desc, badgeId)
                }
                "debug_test" -> {
                    val title = json.optString("title", "Vantafyn Push Test")
                    val body = json.optString("message", "UnifiedPush test notification received")
                    VantafynPushNotifier.showNotification(applicationContext, title, body)
                }
                else -> {
                    if (contentString.contains("Vantafyn Push Test")) {
                        VantafynPushNotifier.showNotification(applicationContext, "Vantafyn Push Test", "UnifiedPush test notification received")
                    }
                }
            }
        }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log.w(TAG, "UnifiedPush registration failed for instance '$instance', reason: ${reason.name}")
        val manager = UnifiedPushManager.getInstance(applicationContext)
        manager.onRegistrationFailed(reason)
    }

    override fun onUnregistered(instance: String) {
        Log.i(TAG, "UnifiedPush unregistered for instance '$instance'")
        val manager = UnifiedPushManager.getInstance(applicationContext)
        manager.onUnregistered()
    }

    override fun onTempUnavailable(instance: String) {
        Log.w(TAG, "UnifiedPush distributor temporarily unavailable for instance '$instance'")
    }

    companion object {
        private const val TAG = "VantafynPushService"
    }
}

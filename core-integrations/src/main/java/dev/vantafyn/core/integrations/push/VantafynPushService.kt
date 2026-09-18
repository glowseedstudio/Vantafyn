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

        // Route to the internal dispatcher
        UnifiedPushPayloadDispatcher.dispatch(payload)

        // Record message reception in manager
        val manager = UnifiedPushManager.getInstance(applicationContext)
        manager.onPushReceived(payload.sizeBytes)

        // If test/debug notification, display a system notification
        val contentString = runCatching { String(message.content, Charsets.UTF_8) }.getOrNull()
        if (contentString != null && (contentString.contains("debug_test") || contentString.contains("Vantafyn Push Test"))) {
            val json = runCatching { org.json.JSONObject(contentString) }.getOrNull()
            val title = json?.optString("title") ?: "Vantafyn Push Test"
            val body = json?.optString("message") ?: json?.optString("body") ?: "UnifiedPush test notification received"
            VantafynPushNotifier.showNotification(applicationContext, title, body)
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

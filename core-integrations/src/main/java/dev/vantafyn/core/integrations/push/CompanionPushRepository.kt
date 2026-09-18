package dev.vantafyn.core.integrations.push

import android.util.Log
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.openAuthenticatedConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter

class CompanionPushRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun checkCapabilities(session: JellyfinSession): Boolean = withContext(ioDispatcher) {
        try {
            val conn = session.openAuthenticatedConnection("Vantafyn/Capabilities", "GET")
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(text)
                val notifications = json.optJSONObject("notifications")
                return@withContext notifications?.optBoolean("backgroundPush", false) ?: false
            }
            conn.disconnect()
            false
        } catch (e: Exception) {
            Log.d(TAG, "checkCapabilities failed: ${e.message}")
            false
        }
    }

    suspend fun registerDevice(
        session: JellyfinSession,
        deviceId: String,
        endpoint: String,
        distributor: String?,
        clientName: String = "Vantafyn Mobile",
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("endpoint", endpoint)
                if (!distributor.isNullOrBlank()) put("distributor", distributor)
                put("clientName", clientName)
            }
            val conn = session.openAuthenticatedConnection("Vantafyn/Notifications/Push/Register", "POST")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            val responseText = runCatching {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            conn.disconnect()

            if (code in 200..299) {
                Log.i(TAG, "Successfully registered push endpoint with Companion server (deviceId=$deviceId)")
                Result.success(Unit)
            } else {
                Log.w(TAG, "Failed to register push endpoint with Companion server: HTTP $code")
                Result.failure(Exception("HTTP $code: $responseText"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering push endpoint with Companion server", e)
            Result.failure(e)
        }
    }

    suspend fun unregisterDevice(
        session: JellyfinSession,
        deviceId: String,
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val conn = session.openAuthenticatedConnection("Vantafyn/Notifications/Push/Devices/$deviceId", "DELETE")
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299 || code == 404) {
                Log.i(TAG, "Unregistered push endpoint from Companion server (deviceId=$deviceId)")
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP $code"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering push endpoint from Companion server", e)
            Result.failure(e)
        }
    }

    suspend fun sendTestPush(
        session: JellyfinSession,
        deviceId: String,
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val body = JSONObject().apply {
                put("deviceId", deviceId)
            }
            val conn = session.openAuthenticatedConnection("Vantafyn/Notifications/Push/Test", "POST")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            val responseText = runCatching {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            conn.disconnect()

            if (code in 200..299) {
                val json = runCatching { JSONObject(responseText) }.getOrNull()
                val msg = json?.optString("message") ?: "Test push sent from server"
                Result.success(msg)
            } else {
                Result.failure(Exception("HTTP $code: $responseText"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test push from Companion server", e)
            Result.failure(e)
        }
    }

    suspend fun notifyChat(
        session: JellyfinSession,
        recipientUserId: String,
        senderName: String?,
        conversationId: String?,
        messageText: String,
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val body = JSONObject().apply {
                put("recipientUserId", recipientUserId)
                if (!conversationId.isNullOrBlank()) put("conversationId", conversationId)
                if (!senderName.isNullOrBlank()) put("senderName", senderName)
                put("messageText", messageText)
            }
            val conn = session.openAuthenticatedConnection("Vantafyn/Notifications/Push/NotifyChat", "POST")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP $code"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error dispatching chat push notification", e)
            Result.failure(e)
        }
    }

    suspend fun notifyAchievement(
        session: JellyfinSession,
        achievementId: String,
        title: String,
        description: String? = null,
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val body = JSONObject().apply {
                put("achievementId", achievementId)
                put("title", title)
                if (!description.isNullOrBlank()) put("description", description)
            }
            val conn = session.openAuthenticatedConnection("Vantafyn/Notifications/Push/NotifyAchievement", "POST")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP $code"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error dispatching achievement push notification", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "CompanionPushRepo"
    }
}

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
                put("DeviceId", deviceId)
                put("deviceId", deviceId)
                put("Endpoint", endpoint)
                put("endpoint", endpoint)
                if (!distributor.isNullOrBlank()) {
                    put("Distributor", distributor)
                    put("distributor", distributor)
                }
                put("ClientName", clientName)
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
                Log.w(TAG, "Failed to register push endpoint with Companion server: HTTP $code ($responseText)")
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

    suspend fun getRegisteredDevices(
        session: JellyfinSession,
    ): Result<List<String>> = withContext(ioDispatcher) {
        try {
            val conn = session.openAuthenticatedConnection("Vantafyn/Notifications/Push/Devices", "GET")
            val code = conn.responseCode
            val responseText = runCatching {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            conn.disconnect()

            if (code in 200..299) {
                val json = JSONObject(responseText)
                val arr = json.optJSONArray("devices")
                val deviceIds = mutableListOf<String>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        val id = obj?.optString("deviceId") ?: obj?.optString("DeviceId")
                        if (!id.isNullOrBlank()) {
                            deviceIds.add(id)
                        }
                    }
                }
                Result.success(deviceIds)
            } else {
                Result.failure(Exception("HTTP $code: $responseText"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting registered devices from Companion server", e)
            Result.failure(e)
        }
    }

    suspend fun unregisterOtherDevices(
        session: JellyfinSession,
        currentDeviceId: String,
    ): Result<Int> = withContext(ioDispatcher) {
        try {
            val devicesRes = getRegisteredDevices(session)
            if (devicesRes.isFailure) {
                return@withContext Result.failure(devicesRes.exceptionOrNull() ?: Exception("Unknown error"))
            }
            val devices = devicesRes.getOrNull().orEmpty()
            var removedCount = 0
            for (devId in devices) {
                if (!devId.equals(currentDeviceId, ignoreCase = true)) {
                    val unregRes = unregisterDevice(session, devId)
                    if (unregRes.isSuccess) {
                        removedCount++
                    }
                }
            }
            Log.i(TAG, "Cleaned up $removedCount other registered device(s) for user ${session.user.id}")
            Result.success(removedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up other registered devices", e)
            Result.failure(e)
        }
    }


    suspend fun sendTestPush(
        session: JellyfinSession,
        deviceId: String,
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val body = JSONObject().apply {
                put("DeviceId", deviceId)
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
                Log.i(TAG, "Test push successfully sent: $msg")
                Result.success(msg)
            } else {
                Log.w(TAG, "Failed to send test push from server: HTTP $code ($responseText)")
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
            val cleanUuid = runCatching {
                val raw = recipientUserId.replace("-", "").trim()
                if (raw.length == 32) {
                    "${raw.substring(0, 8)}-${raw.substring(8, 12)}-${raw.substring(12, 16)}-${raw.substring(16, 20)}-${raw.substring(20, 32)}"
                } else {
                    java.util.UUID.fromString(recipientUserId.trim()).toString()
                }
            }.getOrDefault(recipientUserId.trim())

            val body = JSONObject().apply {
                put("RecipientUserId", cleanUuid)
                put("recipientUserId", cleanUuid)
                if (!conversationId.isNullOrBlank()) {
                    put("ConversationId", conversationId)
                    put("conversationId", conversationId)
                }
                if (!senderName.isNullOrBlank()) {
                    put("SenderName", senderName)
                    put("senderName", senderName)
                }
                put("MessageText", messageText)
                put("messageText", messageText)
            }
            Log.d(TAG, "notifyChat request body: $body")
            val conn = session.openAuthenticatedConnection("Vantafyn/Notifications/Push/NotifyChat", "POST")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            val responseText = runCatching {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            conn.disconnect()

            if (code in 200..299) {
                Log.i(TAG, "Successfully dispatched chat push notification to recipient $cleanUuid: HTTP $code ($responseText)")
                Result.success(Unit)
            } else {
                Log.w(TAG, "Failed to dispatch chat push notification: HTTP $code ($responseText)")
                Result.failure(Exception("HTTP $code: $responseText"))
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
                put("AchievementId", achievementId)
                put("achievementId", achievementId)
                put("Title", title)
                put("title", title)
                if (!description.isNullOrBlank()) {
                    put("Description", description)
                    put("description", description)
                }
            }
            val conn = session.openAuthenticatedConnection("Vantafyn/Notifications/Push/NotifyAchievement", "POST")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            val responseText = runCatching {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            conn.disconnect()

            if (code in 200..299) {
                Log.i(TAG, "Successfully dispatched achievement push notification: HTTP $code ($responseText)")
                Result.success(Unit)
            } else {
                Log.w(TAG, "Failed to dispatch achievement push notification: HTTP $code ($responseText)")
                Result.failure(Exception("HTTP $code: $responseText"))
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

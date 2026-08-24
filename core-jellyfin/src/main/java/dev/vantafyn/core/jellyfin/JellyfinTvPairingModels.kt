package dev.vantafyn.core.jellyfin

import org.json.JSONObject
import java.util.UUID

/**
 * Payload sent from Vantafyn Mobile to Vantafyn Android TV during pairing.
 *
 * Contains only the necessary connection endpoints and session credentials.
 * NEVER contains passwords or integration API keys.
 */
data class TvPairingPayload(
    val code: String,
    val serverUrl: String,
    val localServerUrl: String?,
    val remoteServerUrl: String?,
    val serverName: String?,
    val serverVersion: String?,
    val serverId: String?,
    val userId: UUID,
    val userName: String,
    val userImageTag: String?,
    val accessToken: String,
    val profileId: String,
    val profileDisplayName: String,
    val profileImageUrl: String?,
    val hasPassword: Boolean = false,
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("code", code)
        json.put("serverUrl", serverUrl)
        json.put("localServerUrl", localServerUrl ?: JSONObject.NULL)
        json.put("remoteServerUrl", remoteServerUrl ?: JSONObject.NULL)
        json.put("serverName", serverName ?: JSONObject.NULL)
        json.put("serverVersion", serverVersion ?: JSONObject.NULL)
        json.put("serverId", serverId ?: JSONObject.NULL)
        json.put("userId", userId.toString())
        json.put("userName", userName)
        json.put("userImageTag", userImageTag ?: JSONObject.NULL)
        json.put("accessToken", accessToken)
        json.put("profileId", profileId)
        json.put("profileDisplayName", profileDisplayName)
        json.put("profileImageUrl", profileImageUrl ?: JSONObject.NULL)
        json.put("hasPassword", hasPassword)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): TvPairingPayload? {
            return try {
                val json = JSONObject(jsonString)
                val code = json.getString("code")
                val serverUrl = json.getString("serverUrl")
                val localServerUrl = if (json.isNull("localServerUrl")) null else json.optString("localServerUrl")
                val remoteServerUrl = if (json.isNull("remoteServerUrl")) null else json.optString("remoteServerUrl")
                val serverName = if (json.isNull("serverName")) null else json.optString("serverName")
                val serverVersion = if (json.isNull("serverVersion")) null else json.optString("serverVersion")
                val serverId = if (json.isNull("serverId")) null else json.optString("serverId")
                val userId = UUID.fromString(json.getString("userId"))
                val userName = json.getString("userName")
                val userImageTag = if (json.isNull("userImageTag")) null else json.optString("userImageTag")
                val accessToken = json.getString("accessToken")
                val profileId = json.optString("profileId", userId.toString())
                val profileDisplayName = json.optString("profileDisplayName", userName)
                val profileImageUrl = if (json.isNull("profileImageUrl")) null else json.optString("profileImageUrl")
                val hasPassword = json.optBoolean("hasPassword", false)

                TvPairingPayload(
                    code = code,
                    serverUrl = serverUrl,
                    localServerUrl = localServerUrl,
                    remoteServerUrl = remoteServerUrl,
                    serverName = serverName,
                    serverVersion = serverVersion,
                    serverId = serverId,
                    userId = userId,
                    userName = userName,
                    userImageTag = userImageTag,
                    accessToken = accessToken,
                    profileId = profileId,
                    profileDisplayName = profileDisplayName,
                    profileImageUrl = profileImageUrl,
                    hasPassword = hasPassword,
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Response returned from Vantafyn Android TV to Vantafyn Mobile.
 */
data class TvPairingResponse(
    val status: String, // "ok" or "error"
    val message: String? = null,
    val errorCode: String? = null,
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("status", status)
        if (message != null) json.put("message", message)
        if (errorCode != null) json.put("errorCode", errorCode)
        return json.toString()
    }

    companion object {
        fun success(): TvPairingResponse = TvPairingResponse(status = "ok")
        fun error(code: String, message: String): TvPairingResponse =
            TvPairingResponse(status = "error", errorCode = code, message = message)

        fun fromJson(jsonString: String): TvPairingResponse? {
            return try {
                val json = JSONObject(jsonString)
                TvPairingResponse(
                    status = json.optString("status", "error"),
                    message = if (json.isNull("message")) null else json.optString("message"),
                    errorCode = if (json.isNull("errorCode")) null else json.optString("errorCode"),
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * UDP beacon used by Android TV to advertise presence during the pairing session.
 */
data class TvDiscoveryBeacon(
    val tvName: String,
    val httpPort: Int,
    val protocolVersion: Int = 1,
) {
    fun toPacketString(): String = "VANTAFYN_TV_BEACON:$protocolVersion:$httpPort:$tvName"

    companion object {
        fun fromPacketString(packet: String): TvDiscoveryBeacon? {
            val parts = packet.split(":", limit = 4)
            if (parts.size >= 4 && parts[0] == "VANTAFYN_TV_BEACON") {
                val version = parts[1].toIntOrNull() ?: 1
                val port = parts[2].toIntOrNull() ?: return null
                val name = parts[3]
                return TvDiscoveryBeacon(tvName = name, httpPort = port, protocolVersion = version)
            }
            return null
        }
    }
}

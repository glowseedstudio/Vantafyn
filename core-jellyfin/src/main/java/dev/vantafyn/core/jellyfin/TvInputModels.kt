package dev.vantafyn.core.jellyfin

import org.json.JSONObject

data class TvRemoteInputPayload(
    val text: String = "",
    val fieldType: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val encrypted: Boolean = false,
    val clientPublicKey: String? = null,
    val iv: String? = null,
    val ciphertext: String? = null,
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("text", text)
        if (fieldType != null) json.put("fieldType", fieldType)
        json.put("timestampEpochMs", timestampEpochMs)
        json.put("encrypted", encrypted)
        if (clientPublicKey != null) json.put("clientPublicKey", clientPublicKey)
        if (iv != null) json.put("iv", iv)
        if (ciphertext != null) json.put("ciphertext", ciphertext)
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): TvRemoteInputPayload? {
            return try {
                val json = JSONObject(jsonStr)
                TvRemoteInputPayload(
                    text = json.optString("text", ""),
                    fieldType = if (json.has("fieldType")) json.optString("fieldType") else null,
                    timestampEpochMs = json.optLong("timestampEpochMs", System.currentTimeMillis()),
                    encrypted = json.optBoolean("encrypted", false),
                    clientPublicKey = if (json.has("clientPublicKey")) json.optString("clientPublicKey") else null,
                    iv = if (json.has("iv")) json.optString("iv") else null,
                    ciphertext = if (json.has("ciphertext")) json.optString("ciphertext") else null,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class TvRemoteInputResponse(
    val success: Boolean,
    val message: String? = null,
    val fieldName: String? = null,
    val isSensitive: Boolean = false,
    val serverPublicKey: String? = null,
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("success", success)
        if (message != null) json.put("message", message)
        if (fieldName != null) json.put("fieldName", fieldName)
        json.put("isSensitive", isSensitive)
        if (serverPublicKey != null) json.put("serverPublicKey", serverPublicKey)
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): TvRemoteInputResponse? {
            return try {
                val json = JSONObject(jsonStr)
                TvRemoteInputResponse(
                    success = json.optBoolean("success", false),
                    message = if (json.has("message")) json.optString("message") else null,
                    fieldName = if (json.has("fieldName")) json.optString("fieldName") else null,
                    isSensitive = json.optBoolean("isSensitive", false),
                    serverPublicKey = if (json.has("serverPublicKey")) json.optString("serverPublicKey") else null,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class TvRemoteInputTarget(
    val fieldId: String,
    val fieldName: String,
    val isSensitive: Boolean = false,
    val onTextReceived: (String) -> Unit,
)

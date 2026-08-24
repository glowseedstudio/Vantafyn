package dev.vantafyn.core.jellyfin

import org.json.JSONObject

data class TvRemoteInputPayload(
    val text: String,
    val fieldType: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis(),
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("text", text)
        if (fieldType != null) json.put("fieldType", fieldType)
        json.put("timestampEpochMs", timestampEpochMs)
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
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("success", success)
        if (message != null) json.put("message", message)
        if (fieldName != null) json.put("fieldName", fieldName)
        json.put("isSensitive", isSensitive)
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

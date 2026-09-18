package dev.vantafyn.core.integrations.push

import android.content.Context
import android.provider.Settings
import java.util.UUID

object VantafynDeviceIdProvider {
    private const val PREFS_NAME = "vantafyn_device_identity"
    private const val KEY_FALLBACK_DEVICE_ID = "fallback_device_id"
    private const val BUGGY_ANDROID_ID = "9774d56d682e549c"

    fun getDeviceId(context: Context): String {
        val appContext = context.applicationContext
        val androidId = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()

        if (!androidId.isNullOrBlank() && androidId != BUGGY_ANDROID_ID) {
            return androidId
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_FALLBACK_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val generated = "vnt-" + UUID.randomUUID().toString().replace("-", "").take(16)
        prefs.edit().putString(KEY_FALLBACK_DEVICE_ID, generated).apply()
        return generated
    }
}

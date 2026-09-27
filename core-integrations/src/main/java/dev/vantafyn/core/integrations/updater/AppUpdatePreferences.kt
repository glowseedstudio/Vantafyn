package dev.vantafyn.core.integrations.updater

import android.content.Context
import android.content.SharedPreferences

class AppUpdatePreferences(context: Context) {
    companion object {
        private const val PREFS_NAME = "vantafyn_app_update_prefs"
        private const val KEY_AUTO_CHECK = "auto_check_updates"
        private const val KEY_LAST_CHECK_TIME = "last_check_timestamp"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        const val DEFAULT_AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isAutoCheckEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CHECK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CHECK, value).apply()

    var lastCheckTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK_TIME, value).apply()

    var dismissedVersion: String?
        get() = prefs.getString(KEY_DISMISSED_VERSION, null)
        set(value) = prefs.edit().putString(KEY_DISMISSED_VERSION, value).apply()

    fun shouldAutoCheck(intervalMs: Long = DEFAULT_AUTO_CHECK_INTERVAL_MS): Boolean {
        if (!isAutoCheckEnabled) return false
        val now = System.currentTimeMillis()
        return (now - lastCheckTimestamp) >= intervalMs
    }

    fun recordCheckPerformed() {
        lastCheckTimestamp = System.currentTimeMillis()
    }

    fun dismissVersion(version: String) {
        dismissedVersion = version
    }

    fun isVersionDismissed(version: String): Boolean {
        return dismissedVersion == version
    }
}

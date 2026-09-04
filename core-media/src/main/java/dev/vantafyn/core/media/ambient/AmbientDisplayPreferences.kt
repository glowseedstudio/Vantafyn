package dev.vantafyn.core.media.ambient

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AmbientAutoLaunchMode(val label: String, val description: String) {
    Off(
        label = "Off",
        description = "Manual trigger only via the Ambient button in Now Playing",
    ),
    Always(
        label = "Always when playing",
        description = "Automatically opens synced lyrics over lock screen whenever you lock the device during playback",
    ),
    ChargingOnly(
        label = "Only when charging / docked",
        description = "Opens synced lyrics over lock screen when locked on a charger or wireless dock",
    ),
}

object AmbientDisplayPreferences {
    private const val PREFS_NAME = "vantafyn_ambient_prefs"
    private const val KEY_AUTO_LAUNCH_MODE = "ambient_auto_launch_mode"

    private val _modeFlow = MutableStateFlow(AmbientAutoLaunchMode.Always)
    val modeFlow: StateFlow<AmbientAutoLaunchMode> = _modeFlow.asStateFlow()

    fun getMode(context: Context): AmbientAutoLaunchMode {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_AUTO_LAUNCH_MODE, AmbientAutoLaunchMode.Always.name)
        val mode = AmbientAutoLaunchMode.entries.firstOrNull { it.name == name } ?: AmbientAutoLaunchMode.Always
        _modeFlow.value = mode
        return mode
    }

    fun setMode(context: Context, mode: AmbientAutoLaunchMode) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTO_LAUNCH_MODE, mode.name).apply()
        _modeFlow.value = mode
    }
}

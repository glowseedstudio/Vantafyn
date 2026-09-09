package dev.vantafyn.core.media.music

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SquigglyProgressPreferences {
    private const val PREFS_NAME = "vantafyn_music_prefs"
    private const val KEY_SQUIGGLY_ENABLED = "music_squiggly_progress_enabled"

    private val _enabledFlow = MutableStateFlow(true)
    val enabledFlow: StateFlow<Boolean> = _enabledFlow.asStateFlow()

    fun isEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_SQUIGGLY_ENABLED, true)
        _enabledFlow.value = enabled
        return enabled
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SQUIGGLY_ENABLED, enabled).apply()
        _enabledFlow.value = enabled
    }
}

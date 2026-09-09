package dev.vantafyn.core.media.autoeq

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AutoEqPreferences {
    private const val PREFS_NAME = "vantafyn_autoeq_prefs"
    private const val KEY_ENABLED = "autoeq_enabled"
    private const val KEY_SELECTED_PRESET_ID = "autoeq_selected_preset_id"

    private val _isEnabledFlow = MutableStateFlow(false)
    val isEnabledFlow: StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

    private val _selectedPresetIdFlow = MutableStateFlow<String?>(null)
    val selectedPresetIdFlow: StateFlow<String?> = _selectedPresetIdFlow.asStateFlow()

    fun isEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        _isEnabledFlow.value = enabled
        return enabled
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _isEnabledFlow.value = enabled
    }

    fun getSelectedPresetId(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_SELECTED_PRESET_ID, null)
        _selectedPresetIdFlow.value = id
        return id
    }

    fun setSelectedPresetId(context: Context, presetId: String?) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_PRESET_ID, presetId).apply()
        _selectedPresetIdFlow.value = presetId
    }
}

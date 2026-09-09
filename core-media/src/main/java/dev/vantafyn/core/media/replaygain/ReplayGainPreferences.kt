package dev.vantafyn.core.media.replaygain

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user preferences and reactive StateFlows for ReplayGain (Loudness Leveling).
 */
object ReplayGainPreferences {
    private const val PREFS_NAME = "vantafyn_replaygain_prefs"
    private const val KEY_ENABLED = "replaygain_enabled"
    private const val KEY_PRE_AMP_WITH_REPLAY_GAIN = "replaygain_pre_amp_with_gain"
    private const val KEY_GAIN_WITHOUT_REPLAY_GAIN = "replaygain_gain_without_gain"
    private const val KEY_PREVENT_CLIPPING = "replaygain_prevent_clipping"

    const val DEFAULT_PRE_AMP_WITH_REPLAY_GAIN_DB = 0.0f
    const val DEFAULT_GAIN_WITHOUT_REPLAY_GAIN_DB = -3.0f
    const val DEFAULT_PREVENT_CLIPPING = true
    const val DEFAULT_ENABLED = true

    const val MIN_PRE_AMP_DB = -12.0f
    const val MAX_PRE_AMP_DB = 12.0f

    private val _isEnabledFlow = MutableStateFlow(DEFAULT_ENABLED)
    val isEnabledFlow: StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

    private val _preAmpWithReplayGainFlow = MutableStateFlow(DEFAULT_PRE_AMP_WITH_REPLAY_GAIN_DB)
    val preAmpWithReplayGainFlow: StateFlow<Float> = _preAmpWithReplayGainFlow.asStateFlow()

    private val _gainWithoutReplayGainFlow = MutableStateFlow(DEFAULT_GAIN_WITHOUT_REPLAY_GAIN_DB)
    val gainWithoutReplayGainFlow: StateFlow<Float> = _gainWithoutReplayGainFlow.asStateFlow()

    private val _preventClippingFlow = MutableStateFlow(DEFAULT_PREVENT_CLIPPING)
    val preventClippingFlow: StateFlow<Boolean> = _preventClippingFlow.asStateFlow()

    fun isEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
        _isEnabledFlow.value = enabled
        return enabled
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _isEnabledFlow.value = enabled
    }

    fun getPreAmpWithReplayGain(context: Context): Float {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getFloat(KEY_PRE_AMP_WITH_REPLAY_GAIN, DEFAULT_PRE_AMP_WITH_REPLAY_GAIN_DB)
        _preAmpWithReplayGainFlow.value = value
        return value
    }

    fun setPreAmpWithReplayGain(context: Context, gainDb: Float) {
        val clamped = gainDb.coerceIn(MIN_PRE_AMP_DB, MAX_PRE_AMP_DB)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_PRE_AMP_WITH_REPLAY_GAIN, clamped).apply()
        _preAmpWithReplayGainFlow.value = clamped
    }

    fun getGainWithoutReplayGain(context: Context): Float {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getFloat(KEY_GAIN_WITHOUT_REPLAY_GAIN, DEFAULT_GAIN_WITHOUT_REPLAY_GAIN_DB)
        _gainWithoutReplayGainFlow.value = value
        return value
    }

    fun setGainWithoutReplayGain(context: Context, gainDb: Float) {
        val clamped = gainDb.coerceIn(MIN_PRE_AMP_DB, MAX_PRE_AMP_DB)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_GAIN_WITHOUT_REPLAY_GAIN, clamped).apply()
        _gainWithoutReplayGainFlow.value = clamped
    }

    fun isPreventClipping(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_PREVENT_CLIPPING, DEFAULT_PREVENT_CLIPPING)
        _preventClippingFlow.value = enabled
        return enabled
    }

    fun setPreventClipping(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PREVENT_CLIPPING, enabled).apply()
        _preventClippingFlow.value = enabled
    }

    /**
     * Initializes cached state from SharedPreferences.
     */
    fun init(context: Context) {
        isEnabled(context)
        getPreAmpWithReplayGain(context)
        getGainWithoutReplayGain(context)
        isPreventClipping(context)
    }
}

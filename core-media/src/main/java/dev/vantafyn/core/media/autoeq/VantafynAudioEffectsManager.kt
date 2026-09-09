package dev.vantafyn.core.media.autoeq

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * DSP Audio Effects Manager that bridges Media3 ExoPlayer audio sessions
 * with the hardware [android.media.audiofx.Equalizer].
 *
 * Implements log-linear acoustic frequency interpolation and anti-clipping headroom
 * normalization across diverse Android hardware band configurations (5, 8, 9, 10 bands).
 */
class VantafynAudioEffectsManager(
    private val context: Context,
    private val repository: AutoEqRepository = AutoEqRepository.get(context),
    parentScope: CoroutineScope? = null,
) {
    private val appContext = context.applicationContext
    private val scope = parentScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(AutoEqState())
    val state: StateFlow<AutoEqState> = _state.asStateFlow()

    private var currentAudioSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var hardwareMinLevelMb: Short = -1500
    private var hardwareMaxLevelMb: Short = 1500

    init {
        // Load initial preferences and restore saved preset
        scope.launch {
            val isEnabled = AutoEqPreferences.isEnabled(appContext)
            val savedPresetId = AutoEqPreferences.getSelectedPresetId(appContext)
            val preset = savedPresetId?.let { repository.getPresetById(it) }

            _state.update { it.copy(isEnabled = isEnabled, selectedPreset = preset) }
            applyCurrentConfigToHardware()
        }
    }

    /**
     * Called when Media3's ExoPlayer audio session ID changes.
     */
    fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (audioSessionId == currentAudioSessionId && equalizer != null) return

        Log.d(TAG, "Audio session ID changed: $currentAudioSessionId -> $audioSessionId")
        currentAudioSessionId = audioSessionId

        releaseEqualizer()

        if (audioSessionId <= 0) {
            Log.d(TAG, "Audio session ID is non-positive ($audioSessionId), skipping Equalizer init")
            return
        }

        initEqualizer(audioSessionId)
    }

    /**
     * Enables or disables AutoEQ compensation curve.
     */
    fun setEnabled(enabled: Boolean) {
        AutoEqPreferences.setEnabled(appContext, enabled)
        _state.update { it.copy(isEnabled = enabled) }
        applyCurrentConfigToHardware()
    }

    /**
     * Selects and applies a new AutoEQ preset.
     */
    fun selectPreset(preset: AutoEqPreset?) {
        AutoEqPreferences.setSelectedPresetId(appContext, preset?.id)
        _state.update { it.copy(selectedPreset = preset) }
        applyCurrentConfigToHardware()
    }

    /**
     * Safely initializes the native [Equalizer] on the specified audio session.
     */
    private fun initEqualizer(audioSessionId: Int) {
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            val numBands = eq.numberOfBands
            if (numBands <= 0) {
                eq.release()
                _state.update {
                    it.copy(hardwareStatus = AutoEqHardwareStatus.Unsupported("Hardware reports 0 equalizer bands"))
                }
                return
            }

            val range = eq.bandLevelRange
            hardwareMinLevelMb = range.getOrNull(0) ?: -1500
            hardwareMaxLevelMb = range.getOrNull(1) ?: 1500

            equalizer = eq
            _state.update { it.copy(hardwareStatus = AutoEqHardwareStatus.Supported) }
            applyCurrentConfigToHardware()
            Log.d(TAG, "Initialized Equalizer with $numBands bands (range: $hardwareMinLevelMb to $hardwareMaxLevelMb mB)")
        }.onFailure { e ->
            Log.w(TAG, "Device does not support hardware Equalizer on audio session $audioSessionId: ${e.message}")
            equalizer = null
            _state.update {
                it.copy(hardwareStatus = AutoEqHardwareStatus.Unsupported("Hardware equalizer unavailable: ${e.message}"))
            }
        }
    }

    /**
     * Applies the current enabled/preset state to the native [Equalizer].
     */
    @Synchronized
    private fun applyCurrentConfigToHardware() {
        val eq = equalizer ?: return
        val currentState = _state.value

        runCatching {
            if (!currentState.isEnabled || currentState.selectedPreset == null) {
                // Bypass/Reset: set all bands to flat 0 dB and disable effect
                val numBands = eq.numberOfBands
                val flatBands = ArrayList<EqualizerBandInfo>(numBands.toInt())
                for (b in 0 until numBands) {
                    val bandIndex = b.toShort()
                    eq.setBandLevel(bandIndex, 0.toShort())
                    flatBands.add(
                        EqualizerBandInfo(
                            bandIndex = bandIndex,
                            centerFreqHz = eq.getCenterFreq(bandIndex) / 1000,
                            minLevelMb = hardwareMinLevelMb,
                            maxLevelMb = hardwareMaxLevelMb,
                            currentLevelMb = 0,
                        ),
                    )
                }
                eq.enabled = false
                _state.update { it.copy(hardwareBands = flatBands) }
                Log.d(TAG, "AutoEQ bypassed / disabled")
                return
            }

            val preset = currentState.selectedPreset
            val numBands = eq.numberOfBands
            val bandInfos = ArrayList<EqualizerBandInfo>(numBands.toInt())

            // 1. Compute raw interpolated gains for all hardware bands
            val centerFrequenciesHz = (0 until numBands).map { b ->
                eq.getCenterFreq(b.toShort()) / 1000
            }
            val rawGainsDb = centerFrequenciesHz.map { freq ->
                interpolateGain(freq, preset.frequencies, preset.gains)
            }

            // 2. Anti-clipping headroom normalization:
            // Find peak gain; if peak > 0, apply attenuation to prevent digital clipping
            val peakGainDb = rawGainsDb.maxOrNull() ?: 0f
            val headroomOffsetDb = if (peakGainDb > 0f) -peakGainDb else 0f
            val effectiveOffsetDb = minOf(headroomOffsetDb, preset.preamp.coerceAtMost(0f))

            // 3. Apply normalized gains clamped to hardware range
            for (b in 0 until numBands) {
                val bandIndex = b.toShort()
                val normalizedGainDb = rawGainsDb[b] + effectiveOffsetDb
                val targetMb = (normalizedGainDb * 100f).roundToInt().toShort()
                val clampedMb = targetMb.coerceIn(hardwareMinLevelMb, hardwareMaxLevelMb)

                eq.setBandLevel(bandIndex, clampedMb)
                bandInfos.add(
                    EqualizerBandInfo(
                        bandIndex = bandIndex,
                        centerFreqHz = centerFrequenciesHz[b],
                        minLevelMb = hardwareMinLevelMb,
                        maxLevelMb = hardwareMaxLevelMb,
                        currentLevelMb = clampedMb,
                    ),
                )
            }

            eq.enabled = true
            _state.update { it.copy(hardwareBands = bandInfos) }
            Log.d(TAG, "Applied AutoEQ preset '${preset.name}' with ${bandInfos.size} bands (offset=${effectiveOffsetDb}dB)")
        }.onFailure { e ->
            Log.w(TAG, "Failed to apply EQ settings: ${e.message}", e)
        }
    }

    /**
     * Releases hardware Equalizer resources.
     */
    @Synchronized
    fun release() {
        releaseEqualizer()
        currentAudioSessionId = 0
    }

    private fun releaseEqualizer() {
        runCatching {
            equalizer?.enabled = false
            equalizer?.release()
        }
        equalizer = null
    }

    companion object {
        private const val TAG = "AutoEqEffectsManager"

        /**
         * Piecewise log-linear interpolation in acoustic frequency space (log10(f)).
         */
        fun interpolateGain(
            targetFreqHz: Int,
            frequencies: List<Int>,
            gains: List<Float>,
        ): Float {
            if (frequencies.isEmpty() || gains.isEmpty()) return 0f
            if (frequencies.size == 1 || targetFreqHz <= frequencies.first()) return gains.first()
            if (targetFreqHz >= frequencies.last()) return gains.last()

            // Find bracket [i, i+1]
            for (i in 0 until frequencies.size - 1) {
                val f1 = frequencies[i]
                val f2 = frequencies[i + 1]
                if (targetFreqHz in f1..f2) {
                    val g1 = gains[i]
                    val g2 = gains[i + 1]

                    val logTarget = log10(targetFreqHz.toDouble())
                    val logF1 = log10(f1.toDouble())
                    val logF2 = log10(f2.toDouble())

                    val t = (logTarget - logF1) / (logF2 - logF1)
                    return (g1 + t * (g2 - g1)).toFloat()
                }
            }
            return gains.last()
        }
    }
}

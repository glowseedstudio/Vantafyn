package dev.vantafyn.core.media.replaygain

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Custom Media3 AudioProcessor that implements ReplayGain (Loudness Leveling).
 *
 * Intercepts raw PCM audio buffers (16-bit signed integer and 32-bit float) and
 * dynamically scales the audio samples mathematically based on embedded ReplayGain
 * track gain (in dB), user-defined pre-amp offsets, and track peak values.
 *
 * Edge cases handled:
 * 1. Clipping Prevention: Automatically caps linear gain to (1.0 / Peak) if (Peak * Gain) > 1.0.
 * 2. Missing Metadata: Smoothly falls back to user-configured gainWithoutReplayGainDb.
 * 3. Pass-through: When disabled, returns isActive() = false to eliminate CPU overhead.
 * 4. Click-free transitions: Smoothly ramps gain across audio samples when track gain changes.
 */
@OptIn(UnstableApi::class)
class ReplayGainAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = ReplayGainPreferences.DEFAULT_ENABLED
        private set

    @Volatile
    var preAmpWithReplayGainDb: Float = ReplayGainPreferences.DEFAULT_PRE_AMP_WITH_REPLAY_GAIN_DB
        private set

    @Volatile
    var gainWithoutReplayGainDb: Float = ReplayGainPreferences.DEFAULT_GAIN_WITHOUT_REPLAY_GAIN_DB
        private set

    @Volatile
    var preventClipping: Boolean = ReplayGainPreferences.DEFAULT_PREVENT_CLIPPING
        private set

    @Volatile
    var currentTrackGainDb: Float? = null
        private set

    @Volatile
    var currentTrackPeak: Float? = null
        private set

    @Volatile
    var currentEffectiveGainDb: Float = 0.0f
        private set

    @Volatile
    var isPeakLimitingActive: Boolean = false
        private set

    private var targetLinearGain: Float = 1.0f
    private var currentLinearGain: Float = 1.0f

    // Ramp step per sample to eliminate click/pop artifacts on live UI slider changes (ramps over ~1024 samples)
    private val rampSamples = 1024
    private var rampStep: Float = 0.0f

    // Queue of pending track (gainDb, peak) tuples for gapless stream discontinuity sync
    private var trackGainQueue: List<Pair<Float?, Float?>> = emptyList()
    private var decodingQueueIndex: Int = -1

    init {
        recalculateGain(immediate = true)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> inputAudioFormat
            else -> throw UnhandledAudioFormatException(inputAudioFormat)
        }
    }

    override fun isActive(): Boolean {
        return inputAudioFormat.encoding != C.ENCODING_INVALID
    }

    /**
     * Updates the current track's embedded ReplayGain metadata.
     * When [immediate] is true, target gain is applied immediately with zero ramping,
     * ensuring the beginning of a track never starts louder and ramps down.
     */
    @Synchronized
    fun updateTrackGain(trackGainDb: Float?, trackPeak: Float?, immediate: Boolean = false) {
        currentTrackGainDb = trackGainDb
        currentTrackPeak = trackPeak
        recalculateGain(immediate = immediate)
    }

    /**
     * Synchronizes track gain from the main looper during onMediaItemTransition.
     * Only applies changes if current effective track metadata does not match.
     */
    @Synchronized
    fun syncTrackGain(trackGainDb: Float?, trackPeak: Float?) {
        if (currentTrackGainDb == trackGainDb && currentTrackPeak == trackPeak) return
        updateTrackGain(trackGainDb, trackPeak, immediate = true)
    }

    /**
     * Sets the upcoming queue of track gains and primes the starting track's gain.
     */
    @Synchronized
    fun setTrackQueue(queue: List<Pair<Float?, Float?>>, startIndex: Int) {
        trackGainQueue = queue
        decodingQueueIndex = startIndex
        val entry = queue.getOrNull(startIndex)
        updateTrackGain(entry?.first, entry?.second, immediate = true)
    }

    /**
     * Pre-selects a specific queue index (e.g. on manual skip/seek), applying its gain immediately.
     */
    @Synchronized
    fun setTrackQueueIndex(index: Int) {
        decodingQueueIndex = index
        val entry = trackGainQueue.getOrNull(index)
        if (entry != null) {
            updateTrackGain(entry.first, entry.second, immediate = true)
        }
    }

    /**
     * Updates the playlist track queue without changing current playing state.
     */
    @Synchronized
    fun updateTrackList(queue: List<Pair<Float?, Float?>>, currentIndex: Int) {
        trackGainQueue = queue
        decodingQueueIndex = currentIndex
    }

    /**
     * Called on the audio playback thread by [ForwardingAudioSink.handleDiscontinuity]
     * when the decoder switches to output buffers for the next track in a gapless sequence.
     */
    @Synchronized
    fun onStreamDiscontinuity() {
        if (trackGainQueue.isEmpty()) return
        val nextIndex = decodingQueueIndex + 1
        if (nextIndex in trackGainQueue.indices) {
            decodingQueueIndex = nextIndex
            val entry = trackGainQueue[nextIndex]
            updateTrackGain(entry.first, entry.second, immediate = true)
        }
    }

    /**
     * Updates configuration settings (pre-amp, fallback gain, clipping prevention).
     */
    @Synchronized
    fun setConfiguration(
        enabled: Boolean,
        preAmpWithGainDb: Float,
        gainWithoutGainDb: Float,
        preventClipping: Boolean,
        immediate: Boolean = false,
    ) {
        this.isEnabled = enabled
        this.preAmpWithReplayGainDb = preAmpWithGainDb
        this.gainWithoutReplayGainDb = gainWithoutGainDb
        this.preventClipping = preventClipping
        recalculateGain(immediate = immediate)
    }

    @Synchronized
    private fun recalculateGain(immediate: Boolean = false) {
        if (!isEnabled) {
            targetLinearGain = 1.0f
            currentLinearGain = 1.0f
            currentEffectiveGainDb = 0.0f
            isPeakLimitingActive = false
            rampStep = 0.0f
            return
        }

        val trackGain = currentTrackGainDb
        val rawGainDb = if (trackGain != null) {
            trackGain + preAmpWithReplayGainDb
        } else {
            gainWithoutReplayGainDb
        }

        var linear = dbToLinear(rawGainDb)
        var peakLimiting = false

        // Peak Limiting / Anti-Clipping: if (Peak * linearGain) > 1.0 (0 dBFS), cap gain
        val peak = currentTrackPeak
        if (preventClipping && peak != null && peak > 0.0f) {
            val maxAllowedGain = 1.0f / peak
            if (linear > maxAllowedGain) {
                linear = maxAllowedGain
                peakLimiting = true
            }
        }

        targetLinearGain = linear
        currentEffectiveGainDb = linearToDb(linear)
        isPeakLimitingActive = peakLimiting

        if (immediate) {
            currentLinearGain = targetLinearGain
            rampStep = 0.0f
        } else {
            updateRampStep()
        }
    }

    private fun updateRampStep() {
        rampStep = (targetLinearGain - currentLinearGain) / rampSamples
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val target = targetLinearGain
        val current = currentLinearGain

        // Fast path: pass-through if disabled or gain is exactly unity and no ramping in progress
        if (!isEnabled || (abs(current - 1.0f) < 0.0001f && abs(target - 1.0f) < 0.0001f)) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> processPcm16(inputBuffer, outputBuffer)
            C.ENCODING_PCM_FLOAT -> processPcmFloat(inputBuffer, outputBuffer)
            else -> outputBuffer.put(inputBuffer)
        }

        outputBuffer.flip()
    }

    private fun processPcm16(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer) {
        var gain = currentLinearGain
        val target = targetLinearGain
        val step = rampStep

        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short
            val scaled = (sample * gain).roundToInt().coerceIn(-32768, 32767)
            outputBuffer.putShort(scaled.toShort())

            if (gain != target) {
                gain += step
                if ((step > 0f && gain >= target) || (step < 0f && gain <= target)) {
                    gain = target
                }
            }
        }

        currentLinearGain = gain
    }

    private fun processPcmFloat(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer) {
        var gain = currentLinearGain
        val target = targetLinearGain
        val step = rampStep

        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.float
            val scaled = (sample * gain).coerceIn(-1.0f, 1.0f)
            outputBuffer.putFloat(scaled)

            if (gain != target) {
                gain += step
                if ((step > 0f && gain >= target) || (step < 0f && gain <= target)) {
                    gain = target
                }
            }
        }

        currentLinearGain = gain
    }

    override fun onReset() {
        currentLinearGain = targetLinearGain
        rampStep = 0.0f
    }

    @Suppress("DEPRECATION")
    override fun onFlush() {
        // Maintain target gain across flushes
        currentLinearGain = targetLinearGain
        rampStep = 0.0f
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        currentLinearGain = targetLinearGain
        rampStep = 0.0f
    }

    companion object {
        /**
         * Converts a decibel value to a linear amplitude multiplier:
         * multiplier = 10^(dB / 20)
         */
        fun dbToLinear(db: Float): Float =
            10.0f.pow(db / 20.0f)

        /**
         * Converts a linear amplitude multiplier to decibels:
         * dB = 20 * log10(multiplier)
         */
        fun linearToDb(linear: Float): Float =
            if (linear > 0.0f) 20.0f * kotlin.math.log10(linear) else -100.0f
    }
}

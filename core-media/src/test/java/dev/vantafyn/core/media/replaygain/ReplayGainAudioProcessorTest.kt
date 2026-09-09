package dev.vantafyn.core.media.replaygain

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class ReplayGainAudioProcessorTest {

    @Test
    fun dbToLinearConversionMath() {
        // 0 dB -> 1.0
        assertEquals(1.0f, ReplayGainAudioProcessor.dbToLinear(0.0f), 0.001f)

        // +6.02 dB -> ~2.0
        assertEquals(2.0f, ReplayGainAudioProcessor.dbToLinear(6.0206f), 0.01f)

        // -6.02 dB -> ~0.5
        assertEquals(0.5f, ReplayGainAudioProcessor.dbToLinear(-6.0206f), 0.01f)

        // -20 dB -> 0.1
        assertEquals(0.1f, ReplayGainAudioProcessor.dbToLinear(-20.0f), 0.001f)
    }

    @Test
    fun linearToDbConversionMath() {
        assertEquals(0.0f, ReplayGainAudioProcessor.linearToDb(1.0f), 0.001f)
        assertEquals(6.02f, ReplayGainAudioProcessor.linearToDb(2.0f), 0.01f)
        assertEquals(-6.02f, ReplayGainAudioProcessor.linearToDb(0.5f), 0.01f)
    }

    @Test
    fun peakLimitingPreventsClipping() {
        val processor = ReplayGainAudioProcessor()
        processor.setConfiguration(
            enabled = true,
            preAmpWithGainDb = 6.0f, // ~2.0x boost
            gainWithoutGainDb = 0.0f,
            preventClipping = true,
        )

        // Track with peak = 0.8. Gain = 2.0x -> Peak * Gain = 1.6 > 1.0!
        // Clipping limiter should clamp linear gain to 1.0 / 0.8 = 1.25x (~1.94 dB)
        processor.updateTrackGain(trackGainDb = 0.0f, trackPeak = 0.8f)

        assertTrue(processor.isPeakLimitingActive)
        assertEquals(1.25f, ReplayGainAudioProcessor.dbToLinear(processor.currentEffectiveGainDb), 0.01f)
    }

    @Test
    fun missingMetadataFallsBackToConfiguredGain() {
        val processor = ReplayGainAudioProcessor()
        processor.setConfiguration(
            enabled = true,
            preAmpWithGainDb = 0.0f,
            gainWithoutGainDb = -4.5f,
            preventClipping = true,
        )

        // No metadata (null gain & null peak)
        processor.updateTrackGain(trackGainDb = null, trackPeak = null)

        assertFalse(processor.isPeakLimitingActive)
        assertEquals(-4.5f, processor.currentEffectiveGainDb, 0.01f)
    }

    @Test
    fun disabledProcessorEntersPassThrough() {
        val processor = ReplayGainAudioProcessor()
        processor.setConfiguration(
            enabled = false,
            preAmpWithGainDb = 5.0f,
            gainWithoutGainDb = 5.0f,
            preventClipping = true,
        )

        assertFalse(processor.isEnabled)
        assertEquals(0.0f, processor.currentEffectiveGainDb, 0.001f)

        // Verify pass-through when audio format is configured
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        input.putShort(1000)
        input.putShort(-2000)
        input.flip()

        processor.queueInput(input)
        val output = processor.output
        output.order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(1000, output.short.toInt())
        assertEquals(-2000, output.short.toInt())
    }

    @Test
    fun pcm16BufferScalingAppliesGainCorrectly() {
        val processor = ReplayGainAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        // Set configuration to +6.02 dB (~2.0x gain) without clipping limit
        processor.setConfiguration(
            enabled = true,
            preAmpWithGainDb = 0.0f,
            gainWithoutGainDb = 0.0f,
            preventClipping = false,
        )
        processor.updateTrackGain(trackGainDb = 6.0206f, trackPeak = null)
        processor.flush() // Resets currentLinearGain immediately to targetLinearGain (no ramp)

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        input.putShort(1000)
        input.putShort(-2000)
        input.flip()

        processor.queueInput(input)
        val output = processor.output
        output.order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(2000, output.short.toInt())
        assertEquals(-4000, output.short.toInt())
    }

    @Test
    fun pcm16BufferClampingPreventsIntegerOverflow() {
        val processor = ReplayGainAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        // High gain (+12 dB -> ~4x gain)
        processor.setConfiguration(
            enabled = true,
            preAmpWithGainDb = 12.0f,
            gainWithoutGainDb = 0.0f,
            preventClipping = false,
        )
        processor.updateTrackGain(trackGainDb = 0.0f, trackPeak = null)
        processor.flush()

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        input.putShort(20000) // 20000 * 4 = 80000 -> clamped to 32767
        input.putShort(-20000) // -20000 * 4 = -80000 -> clamped to -32768
        input.flip()

        processor.queueInput(input)
        val output = processor.output
        output.order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(32767, output.short.toInt())
        assertEquals(-32768, output.short.toInt())
    }

    @Test
    fun immediateGainAppliesToFirstSampleWithoutRampDown() {
        val processor = ReplayGainAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        // Set configuration with -6.02 dB fallback (~0.5x gain)
        processor.setConfiguration(
            enabled = true,
            preAmpWithGainDb = 0.0f,
            gainWithoutGainDb = -6.0206f,
            preventClipping = false,
            immediate = true,
        )

        // With immediate = true, target and current are immediately 0.5x, no ramping from 1.0
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        input.putShort(2000)
        input.putShort(-4000)
        input.flip()

        processor.queueInput(input)
        val output = processor.output
        output.order(ByteOrder.LITTLE_ENDIAN)

        // Very first samples must be exactly scaled by 0.5x (1000 and -2000), not full volume!
        assertEquals(1000, output.short.toInt())
        assertEquals(-2000, output.short.toInt())
    }

    @Test
    fun trackQueueAdvanceOnDiscontinuityAppliesNextGainSeamlessly() {
        val processor = ReplayGainAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        processor.setConfiguration(
            enabled = true,
            preAmpWithGainDb = 0.0f,
            gainWithoutGainDb = 0.0f,
            preventClipping = false,
            immediate = true,
        )

        // Setup queue: Track 0 has +6.02 dB (2.0x), Track 1 has -6.02 dB (0.5x)
        val queue = listOf(
            6.0206f to null,
            -6.0206f to null,
        )
        processor.setTrackQueue(queue, startIndex = 0)

        // Track 0 samples
        val inputTrack0 = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        inputTrack0.putShort(1000)
        inputTrack0.putShort(-1000)
        inputTrack0.flip()
        processor.queueInput(inputTrack0)
        val outputTrack0 = processor.output
        outputTrack0.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(2000, outputTrack0.short.toInt())
        assertEquals(-2000, outputTrack0.short.toInt())

        // Simulate gapless track transition (decoder switches stream and triggers discontinuity)
        processor.onStreamDiscontinuity()

        // Track 1 samples should immediately be scaled by 0.5x from sample 0
        val inputTrack1 = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        inputTrack1.putShort(1000)
        inputTrack1.putShort(-1000)
        inputTrack1.flip()
        processor.queueInput(inputTrack1)
        val outputTrack1 = processor.output
        outputTrack1.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(500, outputTrack1.short.toInt())
        assertEquals(-500, outputTrack1.short.toInt())
    }
}

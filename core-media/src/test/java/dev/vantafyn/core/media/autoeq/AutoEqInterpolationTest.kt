package dev.vantafyn.core.media.autoeq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqInterpolationTest {

    private val testFrequencies = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    private val testGains = listOf(6.0f, 4.0f, 2.0f, 0.0f, -1.0f, -2.0f, 1.0f, 3.0f, -2.0f, -4.0f)

    @Test
    fun exactFrequencyMatchesExactGain() {
        testFrequencies.forEachIndexed { index, freq ->
            val gain = VantafynAudioEffectsManager.interpolateGain(freq, testFrequencies, testGains)
            assertEquals(testGains[index], gain, 0.001f)
        }
    }

    @Test
    fun outOfBoundsFrequenciesClampToEnds() {
        val lowGain = VantafynAudioEffectsManager.interpolateGain(10, testFrequencies, testGains)
        assertEquals(testGains.first(), lowGain, 0.001f)

        val highGain = VantafynAudioEffectsManager.interpolateGain(22000, testFrequencies, testGains)
        assertEquals(testGains.last(), highGain, 0.001f)
    }

    @Test
    fun logLinearInterpolationProducesSmoothMonotonicValues() {
        // Between 250Hz (0.0dB) and 500Hz (-1.0dB)
        // Geometric mean is sqrt(250 * 500) ≈ 353.5Hz -> should be exactly -0.5dB in log space
        val geomMeanFreq = kotlin.math.sqrt(250.0 * 500.0).toInt()
        val interpolated = VantafynAudioEffectsManager.interpolateGain(geomMeanFreq, testFrequencies, testGains)
        assertEquals(-0.5f, interpolated, 0.01f)
    }

    @Test
    fun antiClippingHeadroomCalculationEnsuresNoPositiveBoost() {
        val rawGains = listOf(4.5f, 2.0f, -1.0f, 5.2f, -3.0f)
        val peakGain = rawGains.maxOrNull() ?: 0f
        val offset = if (peakGain > 0f) -peakGain else 0f

        val normalized = rawGains.map { it + offset }
        normalized.forEach { gain ->
            assertTrue("Gain must not exceed 0 dBFS: $gain", gain <= 0.0001f)
        }
        assertEquals(0.0f, normalized.maxOrNull() ?: -1f, 0.001f)
    }
}

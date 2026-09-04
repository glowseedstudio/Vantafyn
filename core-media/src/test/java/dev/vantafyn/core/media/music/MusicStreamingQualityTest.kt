package dev.vantafyn.core.media.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicStreamingQualityTest {

    @Test
    fun testLosslessQualityHasNoBitrateCap() {
        val quality = MusicStreamingQuality.Lossless
        assertNull(quality.maxBitrateBps)
        assertNull(quality.maxBitrateKbps)
        assertTrue(quality.container.contains("flac"))
        assertTrue(quality.container.contains("alac"))
        assertTrue(quality.container.contains("wav"))
    }

    @Test
    fun testHighQualityHas320kbpsCap() {
        val quality = MusicStreamingQuality.High
        assertEquals(320_000, quality.maxBitrateBps)
        assertEquals(320, quality.maxBitrateKbps)
    }

    @Test
    fun testMediumQualityHas192kbpsCap() {
        val quality = MusicStreamingQuality.Medium
        assertEquals(192_000, quality.maxBitrateBps)
        assertEquals(192, quality.maxBitrateKbps)
    }

    @Test
    fun testDataSaverQualityHas128kbpsCap() {
        val quality = MusicStreamingQuality.DataSaver
        assertEquals(128_000, quality.maxBitrateBps)
        assertEquals(128, quality.maxBitrateKbps)
    }

    @Test
    fun testDefaults() {
        assertEquals(MusicStreamingQuality.Lossless, MusicStreamingQuality.DefaultWifi)
        assertEquals(MusicStreamingQuality.High, MusicStreamingQuality.DefaultCellular)
    }
}

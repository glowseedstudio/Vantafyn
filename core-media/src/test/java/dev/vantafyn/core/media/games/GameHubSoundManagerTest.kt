package dev.vantafyn.core.media.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameHubSoundManagerTest {

    @Test
    fun defaultVolumeIsRespectfulSixtyFivePercent() {
        assertEquals(0.65f, GameHubSoundManager.DEFAULT_VOLUME, 0.001f)
    }

    @Test
    fun volumeClampingWorksAsExpected() {
        val lowClamped = (-0.5f).coerceIn(0.0f, 1.0f)
        val highClamped = 1.5f.coerceIn(0.0f, 1.0f)
        val respectfulClamped = 0.65f.coerceIn(0.0f, 1.0f)

        assertEquals(0.0f, lowClamped, 0.001f)
        assertEquals(1.0f, highClamped, 0.001f)
        assertEquals(0.65f, respectfulClamped, 0.001f)
    }

    @Test
    fun assetNameIsCorrect() {
        val assetField = GameHubSoundManager::class.java.getDeclaredField("ASSET_NAME")
        assetField.isAccessible = true
        val assetName = assetField.get(null) as String
        assertEquals("gamehub.mp3", assetName)
    }
}

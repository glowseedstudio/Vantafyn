package dev.vantafyn.core.media.subtitles

import androidx.media3.ui.CaptionStyleCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SubtitleStylePreferencesTest {

    @Test
    fun defaultStyle_producesValidCaptionStyleCompat() {
        val config = SubtitleStyleConfig.Default
        val captionStyle = config.toCaptionStyleCompat()
        assertNotNull(captionStyle)
        assertEquals(CaptionStyleCompat.EDGE_TYPE_OUTLINE, captionStyle.edgeType)
    }

    @Test
    fun highContrastStyle_enablesBoxColor() {
        val config = SubtitleStyleConfig.HighContrast
        val captionStyle = config.toCaptionStyleCompat()
        assertNotNull(captionStyle)
        assertEquals(CaptionStyleCompat.EDGE_TYPE_NONE, captionStyle.edgeType)
        assertNotEquals(0, captionStyle.backgroundColor)
    }

    @Test
    fun goldenHourStyle_setsYellowTextColor() {
        val config = SubtitleStyleConfig.GoldenHour
        assertEquals(0xFFFFD700, config.textColor)
        assertEquals(SubtitleEdgeStyle.DropShadow, config.edgeStyle)
    }
}

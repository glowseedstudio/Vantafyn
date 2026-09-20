package dev.vantafyn.core.media

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VantafynExoPlayerFactoryTest {

    @Test
    fun createExtractorsFactory_returnsValidInstance() {
        val factory = VantafynExoPlayerFactory.createExtractorsFactory()
        assertNotNull(factory)
    }

    @Test
    fun media3ExtensionSupport_detectsLibassSubtitles() {
        val libassDecoder = VantafynMedia3ExtensionSupport.decoders.firstOrNull {
            it.label.contains("libass", ignoreCase = true)
        }
        assertNotNull("libass decoder should be present in extension decoders list", libassDecoder)
        assertTrue(
            "libass decoder should be available when ass-media library is bundled",
            libassDecoder?.isAvailable == true,
        )
    }

    @Test
    fun loadControls_haveValidConfigurations() {
        val videoControl = VantafynExoPlayerFactory.videoLoadControl()
        val musicControl = VantafynExoPlayerFactory.musicLoadControl()
        assertNotNull(videoControl)
        assertNotNull(musicControl)
    }
}

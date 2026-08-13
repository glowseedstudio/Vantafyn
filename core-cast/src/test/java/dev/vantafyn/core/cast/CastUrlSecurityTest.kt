package dev.vantafyn.core.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastUrlSecurityTest {
    @Test
    fun redactsSensitiveTokenQueryParameters() {
        val redacted = CastUrlSecurity.redact(
            "https://jellyfin.example.com/Audio/1/stream.mp3?Static=true&api_key=secret&X-Emby-Token=also-secret",
        )

        assertEquals(
            "https://jellyfin.example.com/Audio/1/stream.mp3?Static=true&api_key=<redacted>&X-Emby-Token=<redacted>",
            redacted,
        )
    }

    @Test
    fun keepsNonSensitiveQueryParametersAndFragments() {
        val redacted = CastUrlSecurity.redact(
            "https://jellyfin.example.com/Items/1/Images/Primary?quality=90&tag=abc#poster",
        )

        assertEquals(
            "https://jellyfin.example.com/Items/1/Images/Primary?quality=90&tag=abc#poster",
            redacted,
        )
    }

    @Test
    fun allowsNetworkReachableHttpAndHttpsUrls() {
        assertTrue(CastUrlSecurity.isCastReachableServerAddress("https://jellyfin.example.com/Audio/1/stream.mp3"))
        assertTrue(CastUrlSecurity.isCastReachableServerAddress("http://192.168.1.29:8096/Audio/1/stream.mp3"))
    }

    @Test
    fun rejectsLoopbackAndEmulatorOnlyUrls() {
        assertFalse(CastUrlSecurity.isCastReachableServerAddress("http://localhost:8096/Audio/1/stream.mp3"))
        assertFalse(CastUrlSecurity.isCastReachableServerAddress("http://127.0.0.1:8096/Audio/1/stream.mp3"))
        assertFalse(CastUrlSecurity.isCastReachableServerAddress("http://10.0.2.2:8096/Audio/1/stream.mp3"))
    }

    @Test
    fun rejectsNonHttpSchemes() {
        assertFalse(CastUrlSecurity.isCastReachableServerAddress("file:///sdcard/song.mp3"))
    }
}

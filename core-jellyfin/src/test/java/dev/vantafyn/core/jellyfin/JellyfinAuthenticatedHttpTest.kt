package dev.vantafyn.core.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class JellyfinAuthenticatedHttpTest {

    private val testSession = JellyfinSession(
        server = JellyfinServerConfig(url = "https://jellyfin.example.com"),
        user = JellyfinUser(id = UUID.randomUUID(), name = "TestUser"),
        profileId = "profile-uuid-1234",
        accessToken = "secret-token-xyz",
    )

    @Test
    fun mediaBrowserAuthHeader_formatsCorrectlyForJellyfin12() {
        val header = testSession.mediaBrowserAuthHeader()

        assertEquals(
            "MediaBrowser Client=\"Vantafyn\", Device=\"Android\", DeviceId=\"profile-uuid-1234\", Version=\"0.9.4\", Token=\"secret-token-xyz\"",
            header,
        )
    }

    @Test
    fun openAuthenticatedConnection_setsStandardAndLegacyHeadersWithoutUrlApiKey() {
        val conn = testSession.openAuthenticatedConnection("System/Info", "GET")

        // URL must not leak access token as api_key query parameter
        val url = conn.url.toString()
        assertEquals("https://jellyfin.example.com/System/Info", url)
        assertFalse(url.contains("api_key"))

        val expectedAuthHeader = testSession.mediaBrowserAuthHeader()

        // Legacy Jellyfin 10.x compatibility headers (and mirror of Authorization header)
        assertEquals("secret-token-xyz", conn.getRequestProperty("X-Emby-Token"))
        assertEquals("secret-token-xyz", conn.getRequestProperty("X-MediaBrowser-Token"))
        assertEquals(expectedAuthHeader, conn.getRequestProperty("X-Emby-Authorization"))
    }
}

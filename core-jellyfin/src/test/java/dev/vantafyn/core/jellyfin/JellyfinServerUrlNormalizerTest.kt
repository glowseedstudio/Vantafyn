package dev.vantafyn.core.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Test

class JellyfinServerUrlNormalizerTest {
    @Test
    fun normalizesLocalAndRemoteServerInputs() {
        val cases = mapOf(
            "192.168.1.29" to "http://192.168.1.29:8096",
            "192.168.1.29:8096" to "http://192.168.1.29:8096",
            "http://192.168.1.29:8096/" to "http://192.168.1.29:8096",
            "10.0.0.5" to "http://10.0.0.5:8096",
            "172.16.0.10" to "http://172.16.0.10:8096",
            "jellyfin.local" to "http://jellyfin.local:8096",
            "jelly-watch.org" to "https://jelly-watch.org",
            "https://jelly-watch.org/" to "https://jelly-watch.org",
        )

        cases.forEach { (input, expected) ->
            assertEquals(expected, JellyfinServerUrlNormalizer.normalize(input))
        }
    }
}

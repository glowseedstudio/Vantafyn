package dev.vantafyn.core.jellyfin

import java.net.HttpURLConnection
import java.net.URL

fun JellyfinSession.mediaBrowserAuthHeader(
    client: String = "Vantafyn",
    device: String = "Android",
    version: String = "0.9.7",
): String =
    "MediaBrowser Client=\"$client\", Device=\"$device\", DeviceId=\"$profileId\", Version=\"$version\", Token=\"$accessToken\""

fun JellyfinSession.openAuthenticatedConnection(
    pathAndQuery: String,
    method: String = "GET",
    connectTimeoutMs: Int = 8_000,
    readTimeoutMs: Int = 12_000,
): HttpURLConnection {
    val path = pathAndQuery.trimStart('/')
    val fullUrl = "${server.url.trimEnd('/')}/$path"
    return (URL(fullUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        val authHeader = mediaBrowserAuthHeader()
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json")
        // Standard Jellyfin 12.0 HTTP Header
        setRequestProperty("Authorization", authHeader)
        // Backwards-compatibility fallback headers for Jellyfin 10.8-10.11
        setRequestProperty("X-Emby-Token", accessToken)
        setRequestProperty("X-MediaBrowser-Token", accessToken)
        setRequestProperty("X-Emby-Authorization", authHeader)
    }
}

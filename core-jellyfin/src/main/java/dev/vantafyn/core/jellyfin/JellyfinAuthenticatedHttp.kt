package dev.vantafyn.core.jellyfin

import java.net.HttpURLConnection
import java.net.URL

fun JellyfinSession.openAuthenticatedConnection(pathAndQuery: String, method: String = "GET"): HttpURLConnection {
    val path = pathAndQuery.trimStart('/')
    val separator = if (path.contains("?")) "&" else "?"
    val fullUrl = "${server.url.trimEnd('/')}/$path${separator}api_key=$accessToken"
    return (URL(fullUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 8_000
        readTimeout = 12_000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("X-Emby-Token", accessToken)
        setRequestProperty("X-MediaBrowser-Token", accessToken)
        setRequestProperty("Authorization", "MediaBrowser Client=\"Vantafyn\", Device=\"Vantafyn\", DeviceId=\"$profileId\", Version=\"0.9.2\", Token=\"$accessToken\"")
    }
}

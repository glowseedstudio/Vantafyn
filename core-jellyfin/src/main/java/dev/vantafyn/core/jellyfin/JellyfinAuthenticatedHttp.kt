package dev.vantafyn.core.jellyfin

import java.net.HttpURLConnection
import java.net.URL

fun JellyfinSession.openAuthenticatedConnection(pathAndQuery: String, method: String = "GET"): HttpURLConnection {
    val path = pathAndQuery.trimStart('/')
    return (URL("${server.url.trimEnd('/')}/$path").openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 8_000
        readTimeout = 12_000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("X-Emby-Token", accessToken)
    }
}

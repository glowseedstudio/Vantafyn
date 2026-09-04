package dev.vantafyn.core.cast

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object CastUrlSecurity {
    private val sensitiveKeys = setOf(
        "api_key",
        "token",
        "access_token",
        "x-emby-token",
        "x-mediabrowser-token",
    )

    fun redact(url: String): String {
        val queryStart = url.indexOf('?')
        if (queryStart == -1) return url
        val fragmentStart = url.indexOf('#', startIndex = queryStart)
        val queryEnd = if (fragmentStart == -1) url.length else fragmentStart
        val query = url.substring(queryStart + 1, queryEnd)
        val redactedQuery = query
            .split("&")
            .joinToString("&") { part ->
                val key = part.substringBefore("=")
                val decodedKey = runCatching {
                    URLDecoder.decode(key, StandardCharsets.UTF_8.name())
                }.getOrDefault(key)
                if (decodedKey.lowercase() in sensitiveKeys) {
                    "$key=<redacted>"
                } else {
                    part
                }
            }
        return buildString {
            append(url.substring(0, queryStart))
            append('?')
            append(redactedQuery)
            if (fragmentStart != -1) append(url.substring(fragmentStart))
        }
    }

    fun isCastReachableServerAddress(url: String): Boolean {
        if (url.isBlank()) return false
        val scheme = url.substringBefore("://", "").lowercase()
        if (scheme !in setOf("http", "https")) return false
        val afterScheme = url.substringAfter("://", "")
        val hostAndPort = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = hostAndPort.substringBefore(':').lowercase().trim()
        if (host.isBlank()) return false
        return host !in loopbackHosts &&
            !host.endsWith(".localhost") &&
            !host.contains("10.0.2.2")
    }

    fun userMessageForUnreachableAddress(url: String): String =
        if (isCastReachableServerAddress(url)) {
            "Chromecast could not reach your Jellyfin server. Check Wi-Fi, VPN, reverse proxy, and certificate settings."
        } else {
            "Chromecast cannot use phone-only server addresses like localhost. Set a Cast server address reachable from your TV or speaker."
        }

    private val loopbackHosts = setOf(
        "localhost",
        "127.0.0.1",
        "::1",
        "0.0.0.0",
        "10.0.2.2",
    )
}

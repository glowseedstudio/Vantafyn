package dev.vantafyn.core.jellyfin

import java.net.URI

internal object JellyfinServerUrlNormalizer {
    fun normalize(input: String): String = candidates(input).first()

    fun candidates(input: String): List<String> {
        val trimmed = input.trim().trimEnd('/')
        require(trimmed.isNotBlank()) { "Enter a Jellyfin server URL" }

        val primary = normalizeSingle(trimmed)
        val parsed = URI(primary)
        val alternatives = buildList {
            add(primary)
            if (!hasScheme(trimmed)) {
                val host = parsed.host.orEmpty()
                when {
                    isLocalHost(host) -> {
                        add(url("http", host, parsed.port.takeIf { it != -1 } ?: JellyfinDefaultPort))
                        add(url("http", host, parsed.port.takeIf { it != -1 }))
                    }
                    else -> {
                        add(url("https", host, parsed.port.takeIf { it != -1 }))
                        add(url("http", host, parsed.port.takeIf { it != -1 }))
                    }
                }
            }
        }
        return alternatives.distinct()
    }

    private fun normalizeSingle(input: String): String {
        val withScheme = if (hasScheme(input)) {
            input
        } else {
            val host = input.substringBefore('/').substringBeforeLast(':')
            val scheme = if (isLocalHost(host)) "http" else "https"
            "$scheme://$input"
        }
        val uri = URI(withScheme)
        val host = uri.host?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Enter a valid server URL")
        val port = when {
            uri.port != -1 -> uri.port
            isLocalHost(host) -> JellyfinDefaultPort
            else -> null
        }
        return url(uri.scheme, host, port)
    }

    private fun hasScheme(input: String): Boolean =
        input.contains("://")

    private fun url(scheme: String, host: String, port: Int?): String =
        buildString {
            append(scheme.lowercase())
            append("://")
            append(host.lowercase())
            if (port != null) {
                append(':')
                append(port)
            }
        }.trimEnd('/')

    private fun isLocalHost(host: String): Boolean {
        val lower = host.lowercase()
        if (lower == "localhost" || lower.endsWith(".local")) return true
        val parts = lower.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return parts[0] == 10 ||
            parts[0] == 127 ||
            parts[0] == 192 && parts[1] == 168 ||
            parts[0] == 172 && parts[1] in 16..31
    }

    private const val JellyfinDefaultPort = 8096
}

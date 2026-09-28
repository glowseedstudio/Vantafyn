package dev.vantafyn.core.jellyfin

import java.io.Serializable

enum class GameSaveKind(val value: String) {
    State("state"),
    Sram("sram"),
    Settings("settings");

    companion object {
        fun fromValue(value: String): GameSaveKind =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: State
    }
}

data class GameLibrary(
    val id: String,
    val name: String,
    val path: String = "",
) : Serializable

data class GameSystem(
    val id: String,
    val name: String,
    val core: String,
    val extensions: List<String> = emptyList(),
    val gameCount: Int = 0,
    val icon: String = "",
) : Serializable

data class GameSummary(
    val id: String,
    val title: String,
    val systemId: String,
    val filename: String,
    val sizeBytes: Long = 0L,
    val token: String = "",
    val extension: String = "",
    val boxartUrl: String? = null,
) : Serializable {
    val cleanTitle: String
        get() = cleanGameTitle(title)

    val region: String?
        get() = extractGameRegion(title)
}

data class GameDetail(
    val id: String,
    val title: String,
    val systemId: String,
    val filename: String,
    val sizeBytes: Long = 0L,
    val token: String = "",
    val extension: String = "",
    val core: String = "",
    val cleanTitle: String = "",
    val region: String? = null,
    val downloadUrl: String = "",
    val boxartUrl: String? = null,
) : Serializable

data class GameSaveMetadata(
    val gameId: String,
    val kind: GameSaveKind,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
) : Serializable

/**
 * Removes standard dump/scene tags like (USA), (Europe), [!], (Rev 1), etc.
 */
fun cleanGameTitle(rawTitle: String): String {
    var title = rawTitle
    val dotIndex = title.lastIndexOf('.')
    if (dotIndex > 0) {
        title = title.substring(0, dotIndex)
    }
    // Remove (USA), (Rev 1), [!], etc.
    title = title.replace(Regex("\\s*\\([^)]*\\)"), "")
    title = title.replace(Regex("\\s*\\[[^]]*\\]"), "")
    return title.trim()
}

/**
 * Extracts region tag if present (e.g. USA, Europe, Japan, World).
 */
fun extractGameRegion(rawTitle: String): String? {
    val match = Regex("\\((USA|Europe|Japan|World|En|Fr|De|Es|It|Australia|Beta|Rev\\s*\\d*)[^)]*\\)", RegexOption.IGNORE_CASE)
        .find(rawTitle)
    return match?.groupValues?.getOrNull(1)?.uppercase()
}

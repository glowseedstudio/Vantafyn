package dev.vantafyn.core.downloads

import org.json.JSONArray
import org.json.JSONObject

fun DownloadOfflineManifest.toJsonString(): String =
    JSONObject()
        .put("itemId", itemId)
        .put("title", title)
        .put("generatedAtMillis", generatedAtMillis)
        .put("chaptersAvailable", chaptersAvailable)
        .put("trickplayAvailable", trickplayAvailable)
        .put("subtitles", JSONArray().also { array ->
            subtitles.forEach { subtitle ->
                array.put(
                    JSONObject()
                        .put("index", subtitle.index)
                        .put("label", subtitle.label)
                        .putNullable("language", subtitle.language)
                        .putNullable("codec", subtitle.codec)
                        .putNullable("localPath", subtitle.localPath)
                        .put("isDefault", subtitle.isDefault),
                )
            }
        })
        .put("segments", JSONArray().also { array ->
            segments.forEach { segment ->
                array.put(
                    JSONObject()
                        .put("id", segment.id)
                        .put("type", segment.type)
                        .put("startMs", segment.startMs)
                        .put("endMs", segment.endMs),
                )
            }
        })
        .putNullable(
            "lyrics",
            lyrics?.let { lyrics ->
                JSONObject()
                    .put("plainText", lyrics.plainText)
                    .put("syncedLines", JSONArray().also { array ->
                        lyrics.syncedLines.forEach { line ->
                            array.put(
                                JSONObject()
                                    .putNullable("startMs", line.startMs)
                                    .put("text", line.text),
                            )
                        }
                    })
            },
        )
        .toString()

fun parseDownloadOfflineManifest(json: String): DownloadOfflineManifest? =
    runCatching {
        val root = JSONObject(json)
        val itemId = root.optString("itemId").takeIf { it.isNotBlank() } ?: return@runCatching null
        DownloadOfflineManifest(
            itemId = itemId,
            title = root.optString("title"),
            generatedAtMillis = root.optLong("generatedAtMillis"),
            subtitles = root.optJSONArray("subtitles").toList { subtitle ->
                DownloadOfflineSubtitle(
                    index = subtitle.optInt("index"),
                    label = subtitle.optString("label"),
                    language = subtitle.optNullableString("language"),
                    codec = subtitle.optNullableString("codec"),
                    localPath = subtitle.optNullableString("localPath"),
                    isDefault = subtitle.optBoolean("isDefault"),
                )
            },
            segments = root.optJSONArray("segments").toList { segment ->
                DownloadOfflineSegment(
                    id = segment.optString("id"),
                    type = segment.optString("type"),
                    startMs = segment.optLong("startMs"),
                    endMs = segment.optLong("endMs"),
                )
            },
            lyrics = root.optJSONObject("lyrics")?.let { lyrics ->
                DownloadOfflineLyrics(
                    plainText = lyrics.optString("plainText"),
                    syncedLines = lyrics.optJSONArray("syncedLines").toList { line ->
                        DownloadOfflineLyricLine(
                            startMs = line.optLyricStartMs("startMs"),
                            text = line.optString("text"),
                        )
                    },
                )
            },
            chaptersAvailable = root.optBoolean("chaptersAvailable"),
            trickplayAvailable = root.optBoolean("trickplayAvailable"),
        )
    }.getOrNull()

fun Long.sanitizeLyricMillis(): Long =
    if (this > 1_000_000L) this / 10_000L else this

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject {
    if (value == null) {
        put(name, JSONObject.NULL)
    } else {
        put(name, value)
    }
    return this
}

private fun JSONObject.optNullableString(name: String): String? =
    optString(name).takeIf { has(name) && !isNull(name) && it.isNotBlank() }

private fun JSONObject.optNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private fun JSONObject.optLyricStartMs(name: String): Long? =
    optNullableLong(name)?.sanitizeLyricMillis()

private inline fun <T> JSONArray?.toList(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(transform(it)) }
        }
    }
}

fun parseOfflineLyrics(rawText: String): DownloadOfflineLyrics? {
    val trimmed = rawText.trim()
    if (trimmed.isBlank()) return null

    // 1. Try parsing as manifest JSON
    val manifest = parseDownloadOfflineManifest(trimmed)
    if (manifest != null) {
        return manifest.lyrics
    }

    // 2. Try parsing as standalone / direct lyrics JSON
    if (trimmed.startsWith("{")) {
        val direct = runCatching {
            val root = JSONObject(trimmed)
            val lyricsObj = root.optJSONObject("lyrics") ?: root
            val plain = lyricsObj.optString("plainText")
            val array = lyricsObj.optJSONArray("syncedLines")
            val lines = array?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val item = arr.optJSONObject(i) ?: return@mapNotNull null
                    DownloadOfflineLyricLine(
                        startMs = item.optLyricStartMs("startMs"),
                        text = item.optString("text"),
                    )
                }
            } ?: emptyList()
            if (plain.isNotBlank() || lines.isNotEmpty()) {
                DownloadOfflineLyrics(
                    plainText = plain.ifBlank { lines.joinToString("\n") { it.text } },
                    syncedLines = lines,
                )
            } else null
        }.getOrNull()
        if (direct != null) return direct

        // Do not fall back to plain text for JSON objects
        return null
    }

    // 3. Try parsing as LRC format
    val lrcLyrics = parseLrcLyrics(trimmed)
    if (lrcLyrics != null) return lrcLyrics

    // 4. Do not treat JSON arrays or invalid bracket syntax as plain text
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
        return null
    }

    // 5. Plain text fallback for actual plain text files
    return DownloadOfflineLyrics(
        plainText = trimmed,
        syncedLines = emptyList(),
    )
}

fun parseLrcLyrics(lrcText: String): DownloadOfflineLyrics? {
    val lines = lrcText.lines()
    val synced = mutableListOf<DownloadOfflineLyricLine>()
    val plain = mutableListOf<String>()
    val timestampRegex = Regex("""^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)$""")

    for (rawLine in lines) {
        val line = rawLine.trim()
        val match = timestampRegex.find(line)
        if (match != null) {
            val (minStr, secStr, fracStr, lyricText) = match.destructured
            val min = minStr.toLongOrNull() ?: 0L
            val sec = secStr.toLongOrNull() ?: 0L
            val frac = when (fracStr.length) {
                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                3 -> fracStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            val startMs = min * 60_000L + sec * 1_000L + frac
            val text = lyricText.trim()
            synced.add(DownloadOfflineLyricLine(startMs = startMs, text = text))
            if (text.isNotBlank()) plain.add(text)
        } else if (!line.startsWith("[") && line.isNotBlank()) {
            plain.add(line)
        }
    }
    if (synced.isEmpty() && plain.isEmpty()) return null
    return DownloadOfflineLyrics(
        plainText = plain.joinToString("\n"),
        syncedLines = synced,
    )
}


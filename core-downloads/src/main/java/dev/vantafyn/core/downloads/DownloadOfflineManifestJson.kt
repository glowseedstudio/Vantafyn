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
        DownloadOfflineManifest(
            itemId = root.optString("itemId"),
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
                            startMs = line.optNullableLong("startMs"),
                            text = line.optString("text"),
                        )
                    },
                )
            },
            chaptersAvailable = root.optBoolean("chaptersAvailable"),
            trickplayAvailable = root.optBoolean("trickplayAvailable"),
        )
    }.getOrNull()

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

private inline fun <T> JSONArray?.toList(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(transform(it)) }
        }
    }
}

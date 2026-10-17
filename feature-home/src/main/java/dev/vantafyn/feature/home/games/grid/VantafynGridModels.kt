package dev.vantafyn.feature.home.games.grid

import org.json.JSONObject

/**
 * Span dimension for a tactile channel tile in the Vantafyn 3D Grid.
 *
 * A base cell is 1x1. Larger tiles span multiple columns and rows
 * (e.g., 2x2 for large boxart, 4x2 for hero banners, 4x4 for full widgets).
 */
data class GridTileSpan(
    val columns: Int,
    val rows: Int,
) {
    val key: String get() = "${columns}x${rows}"

    companion object {
        val SPAN_1X1 = GridTileSpan(1, 1)
        val SPAN_2X1 = GridTileSpan(2, 1)
        val SPAN_1X2 = GridTileSpan(1, 2)
        val SPAN_2X2 = GridTileSpan(2, 2)
        val SPAN_4X2 = GridTileSpan(4, 2)
        val SPAN_2X4 = GridTileSpan(2, 4)
        val SPAN_4X4 = GridTileSpan(4, 4)

        fun fromString(str: String): GridTileSpan {
            return when (str.lowercase().trim()) {
                "1x1" -> SPAN_1X1
                "2x1" -> SPAN_2X1
                "1x2" -> SPAN_1X2
                "2x2" -> SPAN_2X2
                "4x2" -> SPAN_4X2
                "2x4" -> SPAN_2X4
                "4x4" -> SPAN_4X4
                else -> {
                    val parts = str.split("x")
                    if (parts.size == 2) {
                        val c = parts[0].toIntOrNull()?.coerceIn(1, 4) ?: 1
                        val r = parts[1].toIntOrNull()?.coerceIn(1, 4) ?: 1
                        GridTileSpan(c, r)
                    } else {
                        SPAN_1X1
                    }
                }
            }
        }

        val AVAILABLE_SPANS = listOf(SPAN_1X1, SPAN_2X1, SPAN_2X2, SPAN_4X2, SPAN_2X4, SPAN_4X4)
    }
}

/**
 * Type of content rendered inside a channel slot.
 */
enum class GridTileType {
    GAME,
    SYSTEM,
    CONTINUE_PLAYING,
    POKEMON_VAULT,
    PROFILE_STATS,
    CLOUD_SAVES,
    SETTINGS,
    LIBRARY,
    MAIN_HOME,
    SEARCH,
    EMPTY_CHANNEL;

    companion object {
        fun fromString(name: String): GridTileType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: GAME
        }
    }
}

/**
 * An item placed on a specific page, column, and row of the tactile channel grid.
 */
data class ChannelTileItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val type: GridTileType,
    val span: GridTileSpan = GridTileSpan.SPAN_1X1,
    val page: Int = 0,
    val col: Int = 0,
    val row: Int = 0,
    val imageUrl: String? = null,
    val targetKey: String? = null,
    val accentColorHex: String? = null,
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            if (subtitle != null) put("subtitle", subtitle)
            put("type", type.name)
            put("span", span.key)
            put("page", page)
            put("col", col)
            put("row", row)
            if (imageUrl != null) put("imageUrl", imageUrl)
            if (targetKey != null) put("targetKey", targetKey)
            if (accentColorHex != null) put("accentColorHex", accentColorHex)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ChannelTileItem {
            return ChannelTileItem(
                id = json.optString("id", ""),
                title = json.optString("title", ""),
                subtitle = if (json.has("subtitle")) json.optString("subtitle") else null,
                type = GridTileType.fromString(json.optString("type", "GAME")),
                span = GridTileSpan.fromString(json.optString("span", "1x1")),
                page = json.optInt("page", 0),
                col = json.optInt("col", 0),
                row = json.optInt("row", 0),
                imageUrl = if (json.has("imageUrl")) json.optString("imageUrl") else null,
                targetKey = if (json.has("targetKey")) json.optString("targetKey") else null,
                accentColorHex = if (json.has("accentColorHex")) json.optString("accentColorHex") else null,
            )
        }
    }
}

package dev.vantafyn.feature.home.games.grid

import android.content.Context
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.RecentGameRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistence and layout manager for the Vantafyn 3D tactile channel grid.
 */
object VantafynGridPersistence {
    private const val PREFS_NAME = "vantafyn_channel_grid_settings"
    private const val KEY_GRID_ITEMS = "channel_grid_items_json"
    private const val KEY_CUSTOMIZED = "channel_grid_user_customized"

    const val DEFAULT_COLUMNS = 6
    const val DEFAULT_ROWS = 4
    const val DEFAULT_PAGES = 5

    /**
     * Loads the persistent grid layout, generating a rich default layout if not previously customized.
     */
    fun loadGrid(
        context: Context,
        games: List<GameSummary>,
        systems: List<GameSystem>,
        recentGames: List<RecentGameRecord>,
    ): List<ChannelTileItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_GRID_ITEMS, null)

        if (!rawJson.isNullOrBlank()) {
            try {
                val array = JSONArray(rawJson)
                val items = mutableListOf<ChannelTileItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    items.add(ChannelTileItem.fromJson(obj))
                }
                if (items.isNotEmpty()) {
                    val retroPrefs = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                    val refreshed = items.map { item ->
                        when (item.type) {
                            GridTileType.GAME -> {
                                val targetId = item.targetKey ?: item.id.removePrefix("game_")
                                val game = games.firstOrNull { it.id == targetId }
                                val localArt = retroPrefs.getString("boxart_$targetId", null)
                                val effectiveArt = localArt ?: game?.boxartUrl ?: item.imageUrl
                                item.copy(
                                    title = game?.title ?: item.title,
                                    subtitle = game?.systemId ?: item.subtitle,
                                    imageUrl = effectiveArt,
                                    targetKey = targetId,
                                )
                            }
                            GridTileType.CONTINUE_PLAYING -> {
                                val rec = recentGames.firstOrNull()?.toGameSummary() ?: games.firstOrNull()
                                val recId = rec?.id ?: item.targetKey
                                val localArt = if (recId != null) retroPrefs.getString("boxart_$recId", null) else null
                                item.copy(
                                    subtitle = rec?.title ?: item.subtitle,
                                    imageUrl = localArt ?: rec?.boxartUrl ?: item.imageUrl,
                                    targetKey = recId,
                                )
                            }
                            GridTileType.SYSTEM -> {
                                val sysId = item.targetKey ?: item.id.removePrefix("system_")
                                val sys = systems.firstOrNull { it.id == sysId }
                                if (sys != null) {
                                    item.copy(
                                        title = sys.name,
                                        subtitle = "${sys.gameCount} Games",
                                        imageUrl = sys.logoUrl ?: item.imageUrl,
                                        targetKey = sysId,
                                    )
                                } else item
                            }
                            else -> item
                        }
                    }
                    return fillMissingSlotsWithPlaceholders(refreshed, DEFAULT_PAGES, DEFAULT_COLUMNS, DEFAULT_ROWS)
                }
            } catch (_: Exception) {}
        }

        // Generate intelligent default layout
        return generateDefaultLayout(games, systems, recentGames)
    }

    /**
     * Persists the current channel layout to SharedPreferences.
     */
    fun saveGrid(context: Context, items: List<ChannelTileItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        // Save only non-empty channel items to keep persistence compact
        for (item in items) {
            if (item.type != GridTileType.EMPTY_CHANNEL) {
                array.put(item.toJson())
            }
        }
        prefs.edit()
            .putString(KEY_GRID_ITEMS, array.toString())
            .putBoolean(KEY_CUSTOMIZED, true)
            .apply()
    }

    /**
     * Fills any unoccupied grid cells with EMPTY_CHANNEL slots.
     */
    fun fillMissingSlotsWithPlaceholders(
        items: List<ChannelTileItem>,
        pagesCount: Int = DEFAULT_PAGES,
        totalCols: Int = DEFAULT_COLUMNS,
        totalRows: Int = DEFAULT_ROWS,
    ): List<ChannelTileItem> {
        val result = mutableListOf<ChannelTileItem>()
        val occupied = Array(pagesCount) { Array(totalRows) { BooleanArray(totalCols) { false } } }

        // Mark cells occupied by actual items
        for (item in items) {
            if (item.type == GridTileType.EMPTY_CHANNEL) continue
            val p = item.page.coerceIn(0, pagesCount - 1)
            for (r in 0 until item.span.rows) {
                for (c in 0 until item.span.columns) {
                    val tr = item.row + r
                    val tc = item.col + c
                    if (tr in 0 until totalRows && tc in 0 until totalCols) {
                        occupied[p][tr][tc] = true
                    }
                }
            }
            result.add(item)
        }

        // Fill non-occupied 1x1 cells with EMPTY_CHANNEL placeholders
        for (p in 0 until pagesCount) {
            for (r in 0 until totalRows) {
                for (c in 0 until totalCols) {
                    if (!occupied[p][r][c]) {
                        result.add(
                            ChannelTileItem(
                                id = "empty_p${p}_r${r}_c${c}",
                                title = "Add Channel",
                                type = GridTileType.EMPTY_CHANNEL,
                                span = GridTileSpan.SPAN_1X1,
                                page = p,
                                col = c,
                                row = r,
                            )
                        )
                    }
                }
            }
        }

        return result
    }

    /**
     * Generates a premium initial home layout showcasing different tile spans:
     * - Top left: Hero "Continue Playing" Banner (4x2)
     * - Top right: Pokémon Vault Channel (2x2)
     * - Bottom left: Recent/Featured Game 1 (2x2)
     * - Bottom mid: Recent/Featured Game 2 (2x2)
     * - Bottom right: System Hub Channels (1x1 or 2x1)
     */
    fun generateDefaultLayout(
        games: List<GameSummary>,
        systems: List<GameSystem>,
        recentGames: List<RecentGameRecord>,
    ): List<ChannelTileItem> {
        val items = mutableListOf<ChannelTileItem>()

        // 1. Hero Continue Playing Banner (4x2) at (col=0, row=0)
        items.add(
            ChannelTileItem(
                id = "widget_continue_playing",
                title = "Continue Playing",
                subtitle = recentGames.firstOrNull()?.title ?: "Jump Back In",
                type = GridTileType.CONTINUE_PLAYING,
                span = GridTileSpan.SPAN_4X2,
                page = 0,
                col = 0,
                row = 0,
                imageUrl = recentGames.firstOrNull()?.boxartUrl,
            )
        )

        // 2. Pokémon Vault Channel (2x2) at (col=4, row=0)
        items.add(
            ChannelTileItem(
                id = "widget_pokemon_vault",
                title = "Pokémon Vault",
                subtitle = "Cloud Storage & Saves",
                type = GridTileType.POKEMON_VAULT,
                span = GridTileSpan.SPAN_2X2,
                page = 0,
                col = 4,
                row = 0,
            )
        )

        // 3. Search Channel (1x1) at (col=0, row=2)
        items.add(
            ChannelTileItem(
                id = "channel_search",
                title = "Search",
                subtitle = "Games & Systems",
                type = GridTileType.SEARCH,
                span = GridTileSpan.SPAN_1X1,
                page = 0,
                col = 0,
                row = 2,
            )
        )

        // 4. Cloud Saves Status Widget (2x1) at (col=1, row=2)
        items.add(
            ChannelTileItem(
                id = "widget_cloud_saves",
                title = "Cloud Saves",
                subtitle = "Active & Synced",
                type = GridTileType.CLOUD_SAVES,
                span = GridTileSpan.SPAN_2X1,
                page = 0,
                col = 1,
                row = 2,
            )
        )

        // 5. System Channel 1 (1x1) at (col=3, row=2)
        val firstSystem = systems.firstOrNull()
        if (firstSystem != null) {
            items.add(
                ChannelTileItem(
                    id = "system_${firstSystem.id}",
                    title = firstSystem.name,
                    subtitle = "${firstSystem.gameCount} Games",
                    type = GridTileType.SYSTEM,
                    span = GridTileSpan.SPAN_1X1,
                    page = 0,
                    col = 3,
                    row = 2,
                    imageUrl = firstSystem.logoUrl,
                    targetKey = firstSystem.id,
                )
            )
        }

        // 6. Settings Channel (1x1) at (col=4, row=2)
        items.add(
            ChannelTileItem(
                id = "channel_settings",
                title = "Settings",
                subtitle = "Preferences",
                type = GridTileType.SETTINGS,
                span = GridTileSpan.SPAN_1X1,
                page = 0,
                col = 4,
                row = 2,
            )
        )

        // 7. Games Library Channel (1x1) at (col=5, row=2)
        items.add(
            ChannelTileItem(
                id = "channel_library",
                title = "Games Library",
                subtitle = "All Consoles",
                type = GridTileType.LIBRARY,
                span = GridTileSpan.SPAN_1X1,
                page = 0,
                col = 5,
                row = 2,
            )
        )

        // 8. Featured Game 1 (2x1) at (col=0, row=3)
        val firstGame = recentGames.firstOrNull()?.toGameSummary() ?: games.firstOrNull()
        if (firstGame != null) {
            items.add(
                ChannelTileItem(
                    id = "game_${firstGame.id}",
                    title = firstGame.title,
                    subtitle = firstGame.systemId,
                    type = GridTileType.GAME,
                    span = GridTileSpan.SPAN_2X1,
                    page = 0,
                    col = 0,
                    row = 3,
                    imageUrl = firstGame.boxartUrl,
                    targetKey = firstGame.id,
                )
            )
        }

        // 9. Featured Game 2 (2x1) at (col=2, row=3)
        val secondGame = if (recentGames.size > 1) recentGames[1].toGameSummary() else games.getOrNull(1)
        if (secondGame != null) {
            items.add(
                ChannelTileItem(
                    id = "game_${secondGame.id}",
                    title = secondGame.title,
                    subtitle = secondGame.systemId,
                    type = GridTileType.GAME,
                    span = GridTileSpan.SPAN_2X1,
                    page = 0,
                    col = 2,
                    row = 3,
                    imageUrl = secondGame.boxartUrl,
                    targetKey = secondGame.id,
                )
            )
        }

        // 10. System Channel 2 (1x1) at (col=4, row=3)
        val secondSystem = systems.getOrNull(1)
        if (secondSystem != null) {
            items.add(
                ChannelTileItem(
                    id = "system_${secondSystem.id}",
                    title = secondSystem.name,
                    subtitle = "${secondSystem.gameCount} Games",
                    type = GridTileType.SYSTEM,
                    span = GridTileSpan.SPAN_1X1,
                    page = 0,
                    col = 4,
                    row = 3,
                    imageUrl = secondSystem.logoUrl,
                    targetKey = secondSystem.id,
                )
            )
        }

        // 11. Main Home (Media) Channel (1x1) at (col=5, row=3)
        items.add(
            ChannelTileItem(
                id = "channel_main_home",
                title = "Vantafyn Home",
                subtitle = "Media",
                type = GridTileType.MAIN_HOME,
                span = GridTileSpan.SPAN_1X1,
                page = 0,
                col = 5,
                row = 3,
            )
        )

        // Page 1: Cloud Saves Status Widget (2x1) at (col=2, row=0)
        items.add(
            ChannelTileItem(
                id = "widget_cloud_saves",
                title = "Cloud Saves",
                subtitle = "Active & Synced",
                type = GridTileType.CLOUD_SAVES,
                span = GridTileSpan.SPAN_2X1,
                page = 1,
                col = 2,
                row = 0,
            )
        )

        // Seed page 1 with additional games if available
        val remainingGames = games.drop(2).take(16)
        var p1Col = 4
        var p1Row = 0
        for (game in remainingGames) {
            if (p1Row >= DEFAULT_ROWS) break
            items.add(
                ChannelTileItem(
                    id = "game_${game.id}",
                    title = game.title,
                    subtitle = game.systemId,
                    type = GridTileType.GAME,
                    span = GridTileSpan.SPAN_1X1,
                    page = 1,
                    col = p1Col,
                    row = p1Row,
                    imageUrl = game.boxartUrl,
                    targetKey = game.id,
                )
            )
            p1Col++
            if (p1Col >= DEFAULT_COLUMNS) {
                p1Col = 0
                p1Row++
            }
        }

        return fillMissingSlotsWithPlaceholders(items, DEFAULT_PAGES, DEFAULT_COLUMNS, DEFAULT_ROWS)
    }

    /**
     * Resizes a channel tile to a new span. If the resized tile collides with any other tiles,
     * those tiles are automatically moved to the nearest open space on the current page,
     * or spilled over to the next page if the current page is full.
     * Guarantees that no two cards can ever occupy the same space!
     */
    fun resizeTile(
        items: List<ChannelTileItem>,
        targetId: String,
        newSpan: GridTileSpan,
        pagesCount: Int = DEFAULT_PAGES,
        totalCols: Int = DEFAULT_COLUMNS,
        totalRows: Int = DEFAULT_ROWS,
    ): List<ChannelTileItem> {
        val target = items.firstOrNull { it.id == targetId } ?: return items

        // 1. Separate real tiles from placeholders
        val realTiles = items.filter { it.type != GridTileType.EMPTY_CHANNEL }.toMutableList()
        val targetIndex = realTiles.indexOfFirst { it.id == targetId }
        if (targetIndex == -1) return items

        // 2. Compute safe bounds for target
        val targetPage = target.page.coerceIn(0, pagesCount - 1)
        val safeCol = target.col.coerceIn(0, totalCols - newSpan.columns)
        val safeRow = target.row.coerceIn(0, totalRows - newSpan.rows)
        val resizedTarget = target.copy(page = targetPage, span = newSpan, col = safeCol, row = safeRow)

        // 3. Grid occupancy tracker across all pages: [page][row][col]
        val occupied = Array(pagesCount) { Array(totalRows) { BooleanArray(totalCols) { false } } }

        fun canPlace(p: Int, c: Int, r: Int, span: GridTileSpan): Boolean {
            if (c + span.columns > totalCols || r + span.rows > totalRows) return false
            for (dr in 0 until span.rows) {
                for (dc in 0 until span.columns) {
                    if (occupied[p][r + dr][c + dc]) return false
                }
            }
            return true
        }

        fun markOccupied(p: Int, c: Int, r: Int, span: GridTileSpan) {
            for (dr in 0 until span.rows) {
                for (dc in 0 until span.columns) {
                    occupied[p][r + dr][c + dc] = true
                }
            }
        }

        // Place resized target first!
        markOccupied(targetPage, safeCol, safeRow, newSpan)
        val placedTiles = mutableListOf<ChannelTileItem>()
        placedTiles.add(resizedTarget)

        // 4. Place all other tiles. If they don't collide, keep their position.
        // If they collide, mark them for relocation.
        val unplaced = mutableListOf<ChannelTileItem>()
        for (tile in realTiles) {
            if (tile.id == targetId) continue
            val p = tile.page.coerceIn(0, pagesCount - 1)
            val c = tile.col.coerceIn(0, totalCols - tile.span.columns)
            val r = tile.row.coerceIn(0, totalRows - tile.span.rows)

            if (canPlace(p, c, r, tile.span)) {
                markOccupied(p, c, r, tile.span)
                placedTiles.add(tile.copy(page = p, col = c, row = r))
            } else {
                unplaced.add(tile)
            }
        }

        // 5. Relocate colliding tiles: find first available fitting slot starting from their preferred page
        for (tile in unplaced) {
            var placed = false
            for (p in tile.page until pagesCount) {
                for (r in 0 until totalRows) {
                    for (c in 0 until totalCols) {
                        if (canPlace(p, c, r, tile.span)) {
                            markOccupied(p, c, r, tile.span)
                            placedTiles.add(tile.copy(page = p, col = c, row = r))
                            placed = true
                            break
                        }
                    }
                    if (placed) break
                }
                if (placed) break
            }

            // If not placed on or after preferred page, search earlier pages 0 until tile.page
            if (!placed) {
                for (p in 0 until tile.page) {
                    for (r in 0 until totalRows) {
                        for (c in 0 until totalCols) {
                            if (canPlace(p, c, r, tile.span)) {
                                markOccupied(p, c, r, tile.span)
                                placedTiles.add(tile.copy(page = p, col = c, row = r))
                                placed = true
                                break
                            }
                        }
                        if (placed) break
                    }
                    if (placed) break
                }
            }
        }

        return fillMissingSlotsWithPlaceholders(placedTiles, pagesCount, totalCols, totalRows)
    }

    /**
     * Moves a tile to target (page, col, row), relocating any colliding tiles so no two tiles ever overlap.
     */
    fun moveTile(
        items: List<ChannelTileItem>,
        targetId: String,
        targetPage: Int,
        targetCol: Int,
        targetRow: Int,
        pagesCount: Int = DEFAULT_PAGES,
        totalCols: Int = DEFAULT_COLUMNS,
        totalRows: Int = DEFAULT_ROWS,
    ): List<ChannelTileItem> {
        val targetTile = items.firstOrNull { it.id == targetId } ?: return items
        val nonPlaceholders = items.filter { it.type != GridTileType.EMPTY_CHANNEL }

        val safePage = targetPage.coerceIn(0, pagesCount - 1)
        val safeCol = targetCol.coerceIn(0, totalCols - targetTile.span.columns)
        val safeRow = targetRow.coerceIn(0, totalRows - targetTile.span.rows)

        val movedTarget = targetTile.copy(page = safePage, col = safeCol, row = safeRow)

        val occupied = Array(pagesCount) { Array(totalRows) { BooleanArray(totalCols) { false } } }

        fun canPlace(p: Int, c: Int, r: Int, span: GridTileSpan): Boolean {
            if (c + span.columns > totalCols || r + span.rows > totalRows) return false
            for (dr in 0 until span.rows) {
                for (dc in 0 until span.columns) {
                    if (occupied[p][r + dr][c + dc]) return false
                }
            }
            return true
        }

        fun markOccupied(p: Int, c: Int, r: Int, span: GridTileSpan) {
            for (dr in 0 until span.rows) {
                for (dc in 0 until span.columns) {
                    occupied[p][r + dr][c + dc] = true
                }
            }
        }

        // 1. Mark target moved tile
        markOccupied(safePage, safeCol, safeRow, movedTarget.span)
        val placedTiles = mutableListOf<ChannelTileItem>()
        placedTiles.add(movedTarget)

        // 2. Separate remaining tiles into non-colliding and colliding
        val otherTiles = nonPlaceholders.filter { it.id != targetId }
        val remainingToPlace = mutableListOf<ChannelTileItem>()

        for (tile in otherTiles) {
            val p = tile.page.coerceIn(0, pagesCount - 1)
            if (canPlace(p, tile.col, tile.row, tile.span)) {
                markOccupied(p, tile.col, tile.row, tile.span)
                placedTiles.add(tile.copy(page = p))
            } else {
                remainingToPlace.add(tile)
            }
        }

        // 3. Relocate colliding tiles to nearest available slot on current page, or spill over to next pages
        for (tile in remainingToPlace) {
            var placed = false
            for (p in tile.page until pagesCount) {
                for (r in 0 until totalRows) {
                    for (c in 0 until totalCols) {
                        if (canPlace(p, c, r, tile.span)) {
                            markOccupied(p, c, r, tile.span)
                            placedTiles.add(tile.copy(page = p, col = c, row = r))
                            placed = true
                            break
                        }
                    }
                    if (placed) break
                }
                if (placed) break
            }

            if (!placed) {
                for (p in 0 until tile.page) {
                    for (r in 0 until totalRows) {
                        for (c in 0 until totalCols) {
                            if (canPlace(p, c, r, tile.span)) {
                                markOccupied(p, c, r, tile.span)
                                placedTiles.add(tile.copy(page = p, col = c, row = r))
                                placed = true
                                break
                            }
                        }
                        if (placed) break
                    }
                    if (placed) break
                }
            }
        }

        return fillMissingSlotsWithPlaceholders(placedTiles, pagesCount, totalCols, totalRows)
    }

    /**
     * Removes an item from the grid (replacing it with an empty placeholder).
     */
    fun removeTile(items: List<ChannelTileItem>, targetId: String): List<ChannelTileItem> {
        val filtered = items.filterNot { it.id == targetId }
        return fillMissingSlotsWithPlaceholders(filtered)
    }

    /**
     * Resets the channel grid back to default.
     */
    fun resetToDefault(
        context: Context,
        games: List<GameSummary>,
        systems: List<GameSystem>,
        recentGames: List<RecentGameRecord>,
    ): List<ChannelTileItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_GRID_ITEMS).putBoolean(KEY_CUSTOMIZED, false).apply()
        return generateDefaultLayout(games, systems, recentGames)
    }
}

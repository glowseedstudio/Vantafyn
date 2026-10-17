package dev.vantafyn.feature.home.games.dualscreen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.resolveSystemDisplayName
import dev.vantafyn.feature.home.games.grid.ChannelTileItem
import dev.vantafyn.feature.home.games.grid.GridTileType
import dev.vantafyn.feature.home.games.screensaver.GameScreensaverResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages secondary presentation displays (e.g. external USB-C, HDMI monitors or dual-screen hardware)
 * to project the Nintendo 3DS-style Top Screen in sync with bottom channel grid navigation.
 */
class GameDualScreenManager(
    private val context: Context,
) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _contentState = MutableStateFlow(DualScreenTopContent())
    val contentState: StateFlow<DualScreenTopContent> = _contentState.asStateFlow()

    private val _isExternalDisplayConnected = MutableStateFlow(false)
    val isExternalDisplayConnected: StateFlow<Boolean> = _isExternalDisplayConnected.asStateFlow()

    private val _connectedDisplayName = MutableStateFlow<String?>(null)
    val connectedDisplayName: StateFlow<String?> = _connectedDisplayName.asStateFlow()

    private var presentation: GameDualScreenPresentation? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            runOnMain { checkDisplays() }
        }

        override fun onDisplayRemoved(displayId: Int) {
            runOnMain { checkDisplays() }
        }

        override fun onDisplayChanged(displayId: Int) {
            runOnMain { checkDisplays() }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun findActivity(ctx: Context): Activity? {
        var current: Context? = ctx
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun start() {
        displayManager.registerDisplayListener(displayListener, mainHandler)
        runOnMain { checkDisplays() }
    }

    fun stop() {
        displayManager.unregisterDisplayListener(displayListener)
        runOnMain { dismissPresentation() }
    }

    fun isEnabled(): Boolean {
        return prefs.getBoolean("games_dual_screen_enabled", true)
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("games_dual_screen_enabled", enabled).apply()
        runOnMain { checkDisplays() }
    }

    fun checkDisplays() {
        runOnMain {
            val activity = findActivity(context)
            if (activity != null && (activity.isFinishing || activity.isDestroyed)) {
                dismissPresentation()
                return@runOnMain
            }

            val presentationList: List<Display> = displayManager
                .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
                .filter { it.isValid && it.displayId != Display.DEFAULT_DISPLAY }

            val presentationDisplays: List<Display> = if (presentationList.isNotEmpty()) {
                presentationList
            } else {
                displayManager.displays.filter { it.isValid && it.displayId != Display.DEFAULT_DISPLAY }
            }

            val targetDisplay = presentationDisplays.firstOrNull()
            _isExternalDisplayConnected.value = targetDisplay != null
            _connectedDisplayName.value = targetDisplay?.name

            if (targetDisplay != null && isEnabled()) {
                if (presentation == null || presentation?.display?.displayId != targetDisplay.displayId) {
                    dismissPresentation()
                    try {
                        presentation = GameDualScreenPresentation(context, targetDisplay, _contentState).apply {
                            show()
                        }
                    } catch (e: Exception) {
                        Log.e("GameDualScreenManager", "Failed to show dual screen presentation", e)
                        presentation = null
                    }
                }
            } else {
                dismissPresentation()
            }
        }
    }

    private fun dismissPresentation() {
        try {
            presentation?.dismiss()
        } catch (_: Exception) {}
        presentation = null
    }

    /**
     * Updates top screen content whenever the user moves focus across channels on the bottom grid.
     */
    fun updateSelection(
        item: ChannelTileItem?,
        games: List<GameSummary>,
        systems: List<GameSystem>,
    ) {
        if (item == null) {
            _contentState.update {
                DualScreenTopContent(
                    title = "Vantafyn Games Hub",
                    subtitle = "Navigate to explore your collection",
                    isGame = false,
                )
            }
            return
        }

        val systemMap = systems.associateBy { it.id.lowercase().trim() }

        when (item.type) {
            GridTileType.GAME, GridTileType.CONTINUE_PLAYING -> {
                val targetId = item.targetKey ?: item.id.removePrefix("game_")
                val game = games.firstOrNull { it.id == targetId || "game_${it.id}" == item.id }
                    ?: games.firstOrNull { it.title.equals(item.title, ignoreCase = true) }
                    ?: games.firstOrNull { it.cleanTitle.equals(item.title, ignoreCase = true) }
                    ?: games.firstOrNull { item.title.contains(it.cleanTitle, ignoreCase = true) }

                val isResume = item.type == GridTileType.CONTINUE_PLAYING
                if (game != null) {
                    val snap = GameScreensaverResolver.resolveLandscapeArt(context, game)
                    val titleArt = GameScreensaverResolver.resolveTitleArt(context, game)
                    val logo = GameScreensaverResolver.resolveGameLogo(context, game)
                        ?: GameBoxartScraper.convertBoxartUrlToLogoUrl(game.boxartUrl ?: item.imageUrl)
                    val system = systemMap[game.systemId.lowercase().trim()]
                    val sysName = system?.displayName ?: resolveSystemDisplayName(game.systemId, "")
                    val effectiveLandscape = snap ?: titleArt
                        ?: GameBoxartScraper.convertBoxartUrlToSnapUrl(game.boxartUrl ?: item.imageUrl)
                        ?: game.boxartUrl
                        ?: item.imageUrl

                    _contentState.value = DualScreenTopContent(
                        title = game.cleanTitle.ifBlank { game.title },
                        subtitle = if (isResume) "Resume: $sysName" else sysName,
                        landscapeArtUrl = effectiveLandscape,
                        logoUrl = logo,
                        boxartUrl = game.boxartUrl ?: item.imageUrl,
                        isGame = true,
                        systemDisplayName = sysName,
                        tileType = item.type,
                        game = game,
                    )
                } else {
                    val fallbackSnap = GameBoxartScraper.convertBoxartUrlToSnapUrl(item.imageUrl) ?: item.imageUrl
                    val fallbackLogo = GameBoxartScraper.convertBoxartUrlToLogoUrl(item.imageUrl)
                    _contentState.value = DualScreenTopContent(
                        title = item.title,
                        subtitle = if (isResume) "Continue Playing" else (item.subtitle ?: "Game Title"),
                        landscapeArtUrl = fallbackSnap,
                        logoUrl = fallbackLogo,
                        boxartUrl = item.imageUrl,
                        isGame = true,
                        systemDisplayName = item.subtitle,
                        tileType = item.type,
                    )
                }
            }
            GridTileType.SYSTEM -> {
                val sysId = item.targetKey ?: item.id.removePrefix("system_")
                val system = systemMap[sysId.lowercase().trim()]
                val sysName = system?.displayName ?: item.title
                _contentState.value = DualScreenTopContent(
                    title = sysName,
                    subtitle = item.subtitle ?: "${system?.gameCount ?: 0} Games Available",
                    landscapeArtUrl = item.imageUrl,
                    isGame = false,
                    systemDisplayName = sysName,
                    tileType = GridTileType.SYSTEM,
                )
            }
            GridTileType.PROFILE_STATS -> {
                _contentState.value = DualScreenTopContent(
                    title = "Player Profile & Activity",
                    subtitle = "Gamer Stats, Playtime & Badges",
                    isGame = false,
                    tileType = GridTileType.PROFILE_STATS,
                )
            }
            GridTileType.SETTINGS -> {
                _contentState.value = DualScreenTopContent(
                    title = "Games Hub Settings",
                    subtitle = "Configuration, Screensaver & Controls",
                    isGame = false,
                    tileType = GridTileType.SETTINGS,
                )
            }
            GridTileType.POKEMON_VAULT -> {
                _contentState.value = DualScreenTopContent(
                    title = "Pokémon Vault",
                    subtitle = "Cross-Generation Storage, Saves & Boxes",
                    isGame = false,
                    tileType = GridTileType.POKEMON_VAULT,
                )
            }
            GridTileType.CLOUD_SAVES -> {
                _contentState.value = DualScreenTopContent(
                    title = "Cloud Save Sync",
                    subtitle = "Instant Multi-Device Backup & Restore",
                    isGame = false,
                    tileType = GridTileType.CLOUD_SAVES,
                )
            }
            GridTileType.LIBRARY -> {
                _contentState.value = DualScreenTopContent(
                    title = "Game Collection",
                    subtitle = "Explore All Available Platforms & Titles",
                    isGame = false,
                    tileType = GridTileType.LIBRARY,
                )
            }
            GridTileType.SEARCH -> {
                _contentState.value = DualScreenTopContent(
                    title = "Search Games",
                    subtitle = "Find Titles Across All Platforms",
                    isGame = false,
                    tileType = GridTileType.SEARCH,
                )
            }
            GridTileType.MAIN_HOME -> {
                _contentState.value = DualScreenTopContent(
                    title = "Main Vantafyn Home",
                    subtitle = "Movies, TV Shows & Music",
                    isGame = false,
                    tileType = GridTileType.MAIN_HOME,
                )
            }
            GridTileType.EMPTY_CHANNEL -> {
                _contentState.value = DualScreenTopContent(
                    title = "Empty Channel Slot",
                    subtitle = "Add a Game, Console or Widget",
                    isGame = false,
                    tileType = GridTileType.EMPTY_CHANNEL,
                )
            }
            else -> {
                _contentState.value = DualScreenTopContent(
                    title = item.title,
                    subtitle = item.subtitle ?: "",
                    isGame = false,
                    tileType = item.type,
                )
            }
        }
    }
}

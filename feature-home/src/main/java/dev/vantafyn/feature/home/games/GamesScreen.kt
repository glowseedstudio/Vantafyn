package dev.vantafyn.feature.home.games

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.RecentGameRecord
import dev.vantafyn.core.media.games.GameHubSoundManager

@Composable
fun GamesScreen(
    activeTab: GamesTab,
    userName: String,
    userImageUrl: String?,
    serverName: String,
    systems: List<GameSystem>,
    games: List<GameSummary>,
    recentGames: List<RecentGameRecord>,
    totalPlayTimeMs: Long,
    selectedSystem: GameSystem?,
    isLoadingGames: Boolean,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSelectTab: (GamesTab) -> Unit,
    onSelectSystem: (GameSystem?) -> Unit,
    onOpenGame: (GameSummary) -> Unit,
    onBackToMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> GameHubSoundManager.resume(context)
                Lifecycle.Event.ON_PAUSE -> GameHubSoundManager.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        GameHubSoundManager.fadeIn(context)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            GameHubSoundManager.fadeOut()
        }
    }

    AnimatedContent(
        targetState = activeTab,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "GamesTabContent",
        modifier = modifier.fillMaxSize(),
    ) { tab ->
        when (tab) {
            GamesTab.Home -> {
                GamesHomeScreen(
                    userName = userName,
                    userImageUrl = userImageUrl,
                    systems = systems,
                    games = games,
                    recentGames = recentGames,
                    totalPlayTimeMs = totalPlayTimeMs,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onOpenGame = onOpenGame,
                    onSelectSystem = { sys ->
                        onSelectSystem(sys)
                        onSelectTab(GamesTab.Library)
                    },
                    onNavigateBack = onBackToMain,
                )
            }
            GamesTab.Library -> {
                GamesHubScreen(
                    systems = systems,
                    games = games,
                    selectedSystem = selectedSystem,
                    isLoading = isLoadingGames,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onSelectSystem = onSelectSystem,
                    onOpenGame = onOpenGame,
                    onBack = {
                        if (selectedSystem != null) {
                            onSelectSystem(null)
                        } else {
                            onSelectTab(GamesTab.Home)
                        }
                    },
                )
            }
            GamesTab.Saves -> {
                GamesSavesScreen(
                    serverName = serverName,
                    onBack = { onSelectTab(GamesTab.Home) },
                )
            }
            GamesTab.Settings -> {
                GamesSettingsScreen(
                    games = games,
                    systems = systems,
                    onBack = { onSelectTab(GamesTab.Home) },
                )
            }
        }
    }
}

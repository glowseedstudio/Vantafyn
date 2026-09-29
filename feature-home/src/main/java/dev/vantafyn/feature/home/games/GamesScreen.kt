package dev.vantafyn.feature.home.games

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.RecentGameRecord

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
    onSelectTab: (GamesTab) -> Unit,
    onSelectSystem: (GameSystem?) -> Unit,
    onOpenGame: (GameSummary) -> Unit,
    onBackToMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

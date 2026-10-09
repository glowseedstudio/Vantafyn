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
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.RecentGameRecord
import dev.vantafyn.core.media.games.GameHubSoundManager
import dev.vantafyn.core.media.games.GameHubTrack

import dev.vantafyn.core.jellyfin.DefaultJellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.feature.home.games.pokemon.PokemonVaultScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun GamesScreen(
    activeTab: GamesTab,
    userName: String,
    userImageUrl: String?,
    serverName: String,
    systems: List<GameSystem>,
    games: List<GameSummary>,
    allGames: List<GameSummary> = games,
    recentGames: List<RecentGameRecord>,
    totalPlayTimeMs: Long,
    selectedSystem: GameSystem?,
    isLoadingGames: Boolean,
    isRefreshing: Boolean = false,
    downloadedGameKeys: Set<String> = emptySet(),
    session: JellyfinSession? = null,
    gamesRepository: JellyfinGamesRepository? = null,
    pokemonRepository: JellyfinPokemonRepository? = null,
    isPokemonVaultAvailable: Boolean = false,
    vaultHomeTrigger: Long = 0L,
    onRefresh: () -> Unit = {},
    onSelectTab: (GamesTab) -> Unit,
    onSelectSystem: (GameSystem?) -> Unit,
    onOpenGame: (GameSummary) -> Unit,
    onRemoveRecentGame: (String) -> Unit = {},
    onBackToMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reducedMotion = dev.vantafyn.feature.home.rememberReducedMotionPreference()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> GameHubSoundManager.resume(context)
                Lifecycle.Event.ON_PAUSE -> GameHubSoundManager.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val initialTrack = if (activeTab == GamesTab.Vault) GameHubTrack.POKEMON_HOME else GameHubTrack.GAME_HUB
        GameHubSoundManager.fadeIn(context, track = initialTrack)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            GameHubSoundManager.fadeOut()
        }
    }

    LaunchedEffect(activeTab) {
        val targetTrack = if (activeTab == GamesTab.Vault) GameHubTrack.POKEMON_HOME else GameHubTrack.GAME_HUB
        GameHubSoundManager.crossfadeTo(context, targetTrack, durationMs = 900L)
    }

    GameScreenReveal(
        key = "games_screen_root",
        modifier = modifier.fillMaxSize(),
    ) {
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                gamesTabTransitionSpec(reducedMotion)
            },
            label = "GamesTabContent",
            modifier = Modifier.fillMaxSize(),
        ) { tab ->
            when (tab) {
                GamesTab.Home -> {
                    GamesHomeScreen(
                        userName = userName,
                        userImageUrl = userImageUrl,
                        systems = systems,
                        games = games,
                        allGames = allGames,
                        recentGames = recentGames,
                        totalPlayTimeMs = totalPlayTimeMs,
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        onOpenGame = onOpenGame,
                        onRemoveRecentGame = onRemoveRecentGame,
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
                        allGames = allGames,
                        selectedSystem = selectedSystem,
                        isLoading = isLoadingGames,
                        isRefreshing = isRefreshing,
                        downloadedGameKeys = downloadedGameKeys,
                        isPokemonVaultAvailable = isPokemonVaultAvailable,
                        onOpenPokemonVault = { onSelectTab(GamesTab.Vault) },
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
                GamesTab.Vault -> {
                    PokemonVaultScreen(
                        session = session,
                        pokemonRepository = pokemonRepository ?: remember { DefaultJellyfinPokemonRepository() },
                        onBack = { onSelectTab(GamesTab.Home) },
                        vaultHomeTrigger = vaultHomeTrigger,
                    )
                }
                GamesTab.Saves -> {
                    GamesSavesScreen(
                        serverName = serverName,
                        games = allGames,
                        systems = systems,
                        session = session,
                        gamesRepository = gamesRepository,
                        onBack = { onSelectTab(GamesTab.Home) },
                    )
                }
                GamesTab.Settings -> {
                    GamesSettingsScreen(
                        games = games,
                        allGames = allGames,
                        systems = systems,
                        onBack = {
                            onRefresh()
                            onSelectTab(GamesTab.Home)
                        },
                    )
                }
            }
        }
    }
}

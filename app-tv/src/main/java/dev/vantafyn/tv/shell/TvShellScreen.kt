package dev.vantafyn.tv.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvBackground
import dev.vantafyn.tv.nav.TvNavigationItem
import dev.vantafyn.tv.nav.TvNavigationState
import dev.vantafyn.tv.nav.TvRoute
import dev.vantafyn.tv.nav.rememberTvNavigationState
import dev.vantafyn.tv.screens.TvDetailScreen
import dev.vantafyn.tv.screens.TvHomeScreen
import dev.vantafyn.tv.screens.TvLibraryScreen
import dev.vantafyn.tv.screens.TvPlaceholderScreen
import dev.vantafyn.tv.screens.TvSearchScreen
import dev.vantafyn.tv.screens.TvSettingsScreen
import dev.vantafyn.tv.sidebar.VantafynTvSidebar
import java.util.UUID

@Composable
fun TvShellScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    navState: TvNavigationState = rememberTvNavigationState(),
    onOpenMediaDetails: (UUID) -> Unit = {},
    onPlayMediaId: (UUID) -> Unit = {},
    onStartPlayback: () -> Unit = {},
    onStartPlaybackFromBeginning: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSelectProfile: (SavedProfile) -> Unit = {},
    onRefreshHome: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    // Back navigation handler
    BackHandler(enabled = true) {
        if (!navState.navigateBack()) {
            // Already at Home with collapsed sidebar
        }
    }

    val isAdmin = session?.user?.isAdministrator == true
    val hasRequests = state.ombiConfigured

    val navItems = remember(isAdmin, hasRequests) {
        TvNavigationItem.defaultItems(isAdmin = isAdmin, hasRequests = hasRequests)
    }

    VantafynTvBackground(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            // --- EXPANDING SIDEBAR ---
            VantafynTvSidebar(
                items = navItems,
                currentRoute = navState.currentRoute,
                isExpanded = navState.isSidebarExpanded,
                session = session,
                onExpand = { navState.expandSidebar() },
                onCollapse = { navState.collapseSidebar() },
                onRouteSelected = { route ->
                    navState.navigateTo(route)
                },
                onProfileClicked = {
                    navState.navigateTo(TvRoute.Settings)
                },
            )

            // --- MAIN CONTENT VIEWPORT ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = navState.currentRoute,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "TvContentRoute",
                ) { targetRoute ->
                    when (targetRoute) {
                        TvRoute.Home -> {
                            TvHomeScreen(
                                state = state,
                                session = session,
                                onOpenMedia = { id ->
                                    onOpenMediaDetails(id)
                                    navState.navigateTo(TvRoute.Details(id))
                                },
                                onPlayMediaId = { id ->
                                    onPlayMediaId(id)
                                },
                                onExploreLibraries = {
                                    navState.navigateTo(TvRoute.Library())
                                },
                                onRefresh = onRefreshHome,
                            )
                        }

                        is TvRoute.Library -> {
                            TvLibraryScreen(
                                state = state,
                                session = session,
                                onOpenMedia = { id ->
                                    onOpenMediaDetails(id)
                                    navState.navigateTo(TvRoute.Details(id))
                                },
                            )
                        }

                        TvRoute.Search -> {
                            TvSearchScreen(
                                state = state,
                                session = session,
                                onQueryChange = onSearchQueryChange,
                                onOpenMedia = { id ->
                                    onOpenMediaDetails(id)
                                    navState.navigateTo(TvRoute.Details(id))
                                },
                            )
                        }

                        TvRoute.Music -> {
                            TvPlaceholderScreen(
                                title = "Music for Android TV",
                                description = "Full background music player, playlists, and lossless streaming are currently being optimized for TV.",
                                icon = Icons.Rounded.MusicNote,
                            )
                        }

                        TvRoute.Requests -> {
                            TvPlaceholderScreen(
                                title = "Ombi & Media Requests",
                                description = if (state.ombiConfigured) {
                                    "Browse, discover, and request new media directly from your Jellyfin server integrations."
                                } else {
                                    "Requests need setup. Use the Vantafyn mobile app to connect or manage Ombi."
                                },
                                icon = Icons.AutoMirrored.Rounded.Send,
                                hintText = if (state.ombiConfigured) "10-foot request browsing interface coming soon." else "Use the Vantafyn mobile app to configure Ombi.",
                            )
                        }

                        TvRoute.Admin -> {
                            TvPlaceholderScreen(
                                title = "Admin Dashboard",
                                description = "Server performance stats, active transcoding streams, and activity logs.",
                                icon = Icons.Rounded.AdminPanelSettings,
                            )
                        }

                        TvRoute.Settings -> {
                            TvSettingsScreen(
                                state = state,
                                session = session,
                                onSelectProfile = onSelectProfile,
                                onLogout = onLogout,
                            )
                        }

                        is TvRoute.Details -> {
                            TvDetailScreen(
                                detail = state.mediaDetail,
                                session = session,
                                onPlay = onStartPlayback,
                                onPlayFromStart = onStartPlaybackFromBeginning,
                                onToggleFavorite = {
                                    // Handled via ViewModel
                                },
                            )
                        }

                        is TvRoute.Player -> {
                            // Player route scaffold
                        }
                    }
                }
            }
        }
    }
}

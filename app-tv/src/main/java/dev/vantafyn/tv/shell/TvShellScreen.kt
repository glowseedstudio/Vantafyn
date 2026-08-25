package dev.vantafyn.tv.shell

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvBackground
import dev.vantafyn.tv.components.VantafynTvGlassButton
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
    val hasRequests = state.ombiConfigured || state.ombiRequestsEnabledForUsers
    var showExitDialog by remember { mutableStateOf(false) }

    val mainNavItems = remember(state.libraries, state.favorites, isAdmin, hasRequests) {
        TvNavigationItem.buildMainItems(
            libraries = state.libraries,
            hasFavorites = state.favorites.isNotEmpty(),
            isAdmin = isAdmin,
            hasRequests = hasRequests,
        )
    }

    val bottomNavItems = remember(state.hasUnseenWhatsNew) {
        TvNavigationItem.buildBottomItems(
            hasUnseenNotifications = state.hasUnseenWhatsNew,
            onExitApp = { showExitDialog = true },
        )
    }

    VantafynTvBackground(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            // --- EXPANDING SIDEBAR ---
            VantafynTvSidebar(
                mainItems = mainNavItems,
                bottomItems = bottomNavItems,
                currentRoute = navState.currentRoute,
                isExpanded = navState.isSidebarExpanded,
                session = session,
                onExpand = { navState.expandSidebar() },
                onCollapse = { navState.collapseSidebar() },
                onRouteSelected = { route ->
                    navState.navigateToFromDrawer(route)
                },
                onProfileClicked = {
                    navState.navigateToFromDrawer(TvRoute.Settings)
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

                        TvRoute.Favorites -> {
                            TvPlaceholderScreen(
                                title = "Favorites & My List",
                                description = "Your favorite movies, series, and music on this Jellyfin server.",
                                icon = Icons.Rounded.Favorite,
                            )
                        }

                        is TvRoute.Library -> {
                            TvLibraryScreen(
                                state = state,
                                session = session,
                                initialLibraryId = targetRoute.parentId,
                                title = targetRoute.title,
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

                        TvRoute.Notifications -> {
                            TvPlaceholderScreen(
                                title = "What's New & Updates",
                                description = "Explore the newest features, enhancements, and releases across Vantafyn.",
                                icon = Icons.Rounded.Notifications,
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

    // --- EXIT APP CONFIRMATION DIALOG ---
    if (showExitDialog) {
        val context = LocalContext.current
        Dialog(
            onDismissRequest = { showExitDialog = false },
        ) {
            Box(
                modifier = Modifier
                    .width(440.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xF00D1527))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(22.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color(0xFFFF4B6E),
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Exit Vantafyn?",
                        color = VantafynColors.Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to close the application?",
                        color = VantafynColors.Muted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VantafynTvGlassButton(
                            text = "Cancel",
                            icon = Icons.Rounded.Close,
                            isPrimary = false,
                            onClick = { showExitDialog = false },
                        )
                        VantafynTvGlassButton(
                            text = "Exit App",
                            icon = Icons.Rounded.PowerSettingsNew,
                            isPrimary = true,
                            onClick = {
                                showExitDialog = false
                                (context as? Activity)?.finish()
                            },
                        )
                    }
                }
            }
        }
    }
}


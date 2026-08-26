package dev.vantafyn.tv.shell

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import dev.vantafyn.core.jellyfin.JellyfinMediaSegmentBehavior
import dev.vantafyn.core.jellyfin.JellyfinMediaSegmentType
import dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.media.UpNextDisplayMode
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynThemePreset
import dev.vantafyn.feature.home.auth.BottomRailAccent
import dev.vantafyn.feature.home.auth.ThemeMusicVolume
import dev.vantafyn.feature.home.auth.VantafynAppBackground
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynVideoPlayerPreference
import dev.vantafyn.tv.components.VantafynTvBackground
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.nav.TvNavigationItem
import dev.vantafyn.tv.nav.TvNavigationState
import dev.vantafyn.tv.nav.TvRoute
import dev.vantafyn.tv.nav.TvSettingsCategory
import dev.vantafyn.tv.nav.rememberTvNavigationState
import dev.vantafyn.tv.nav.toNavigationItem
import dev.vantafyn.tv.screens.TvDetailScreen
import dev.vantafyn.tv.screens.TvHomeScreen
import dev.vantafyn.tv.screens.TvLibraryScreen
import dev.vantafyn.tv.screens.TvPlaceholderScreen
import dev.vantafyn.tv.screens.TvSearchScreen
import dev.vantafyn.tv.screens.TvSettingsScreen
import dev.vantafyn.tv.sidebar.VantafynTvSidebar
import java.util.UUID
import dev.vantafyn.core.ui.R as CoreUiR

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
    onSelectTheme: (VantafynThemePreset) -> Unit = {},
    onSelectBackground: (VantafynAppBackground) -> Unit = {},
    onToggleThemeMusic: () -> Unit = {},
    onSelectThemeMusicVolume: (ThemeMusicVolume) -> Unit = {},
    onSetBottomRailAccent: (BottomRailAccent) -> Unit = {},
    onToggleSoundEffects: () -> Unit = {},
    onToggleAutoLoginLastProfile: () -> Unit = {},
    onToggleWhatsNew: () -> Unit = {},
    onToggleAchievementsEnabled: () -> Unit = {},
    onToggleSocialEnabled: () -> Unit = {},
    onToggleSocialDockEnabled: () -> Unit = {},
    onSetDownloadWifiOnlyDefault: (Boolean) -> Unit = {},
    onToggleWatchPartyEnabled: () -> Unit = {},
    onToggleWatchPartyInvitesEnabled: () -> Unit = {},
    onToggleWatchPartyInviteAnimationEnabled: () -> Unit = {},
    onSetWatchPartyInviteExpirySeconds: (Int) -> Unit = {},
    onEditPlaybackPreferences: ((JellyfinUserPlaybackPreferences) -> JellyfinUserPlaybackPreferences) -> Unit = {},
    onSavePlaybackPreferences: () -> Unit = {},
    onSetAutoplayCountdownSeconds: (Int) -> Unit = {},
    onSetUpNextDisplayMode: (UpNextDisplayMode) -> Unit = {},
    onTogglePassoutProtection: () -> Unit = {},
    onSetPassoutProtectionLimitMinutes: (Int) -> Unit = {},
    onSelectVideoPlayerPreference: (VantafynVideoPlayerPreference) -> Unit = {},
    onSetMaxStreamingBitrateMbps: (Int?) -> Unit = {},
    onSetMediaSegmentBehavior: (JellyfinMediaSegmentType, JellyfinMediaSegmentBehavior) -> Unit = { _, _ -> },
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
    var settingsCategory by rememberSaveable { mutableStateOf(TvSettingsCategory.Appearance) }

    LaunchedEffect(session?.user?.id) {
        if (session != null) {
            navState.collapseSidebar()
        }
    }

    val sidebarExpandedContentOffset by animateDpAsState(
        targetValue = if (navState.isSidebarExpanded) 144.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 300f),
        label = "TvSidebarContentOffset",
    )
    val scaffoldSidebarInset = 80.dp + sidebarExpandedContentOffset
    val fullBleedContentStartPadding = 108.dp + sidebarExpandedContentOffset

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
    val settingsNavItems = remember { TvSettingsCategory.entries.map { it.toNavigationItem() } }

    VantafynTvBackground(
        modifier = modifier,
        backgroundResId = state.selectedBackground.tvDrawableResId(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- MAIN CONTENT CANVAS ---
            // The active screen draws full-bleed behind the translucent sidebar. Individual screens
            // keep their own safe content inset so artwork can pass underneath the rail without
            // putting focusable controls beneath it.
            Box(modifier = Modifier.fillMaxSize()) {
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
                                sidebarContentOffset = sidebarExpandedContentOffset,
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
                                title = "My List",
                                description = "Movies, series, and music you've added from this Jellyfin server.",
                                icon = Icons.Rounded.Favorite,
                                modifier = Modifier.padding(start = scaffoldSidebarInset),
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
                                modifier = Modifier.padding(start = scaffoldSidebarInset),
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
                                modifier = Modifier.padding(start = scaffoldSidebarInset),
                            )
                        }

                        TvRoute.Music -> {
                            TvPlaceholderScreen(
                                title = "Music for Android TV",
                                description = "Full background music player, playlists, and lossless streaming are currently being optimized for TV.",
                                icon = Icons.Rounded.MusicNote,
                                modifier = Modifier.padding(start = scaffoldSidebarInset),
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
                                modifier = Modifier.padding(start = scaffoldSidebarInset),
                            )
                        }

                        TvRoute.Admin -> {
                            TvPlaceholderScreen(
                                title = "Admin Dashboard",
                                description = "Server performance stats, active transcoding streams, and activity logs.",
                                icon = Icons.Rounded.AdminPanelSettings,
                                modifier = Modifier.padding(start = scaffoldSidebarInset),
                            )
                        }

                        TvRoute.Notifications -> {
                            TvPlaceholderScreen(
                                title = "What's New & Updates",
                                description = "Explore the newest features, enhancements, and releases across Vantafyn.",
                                icon = Icons.Rounded.Notifications,
                                modifier = Modifier.padding(start = scaffoldSidebarInset),
                            )
                        }

                        TvRoute.Settings -> {
                            TvSettingsScreen(
                                state = state,
                                session = session,
                                category = settingsCategory,
                                onSelectProfile = onSelectProfile,
                                onLogout = onLogout,
                                onSelectTheme = onSelectTheme,
                                onSelectBackground = onSelectBackground,
                                onToggleThemeMusic = onToggleThemeMusic,
                                onSelectThemeMusicVolume = onSelectThemeMusicVolume,
                                onSetBottomRailAccent = onSetBottomRailAccent,
                                onToggleSoundEffects = onToggleSoundEffects,
                                onToggleAutoLoginLastProfile = onToggleAutoLoginLastProfile,
                                onToggleWhatsNew = onToggleWhatsNew,
                                onToggleAchievementsEnabled = onToggleAchievementsEnabled,
                                onToggleSocialEnabled = onToggleSocialEnabled,
                                onToggleSocialDockEnabled = onToggleSocialDockEnabled,
                                onSetDownloadWifiOnlyDefault = onSetDownloadWifiOnlyDefault,
                                onToggleWatchPartyEnabled = onToggleWatchPartyEnabled,
                                onToggleWatchPartyInvitesEnabled = onToggleWatchPartyInvitesEnabled,
                                onToggleWatchPartyInviteAnimationEnabled = onToggleWatchPartyInviteAnimationEnabled,
                                onSetWatchPartyInviteExpirySeconds = onSetWatchPartyInviteExpirySeconds,
                                onEditPlaybackPreferences = onEditPlaybackPreferences,
                                onSavePlaybackPreferences = onSavePlaybackPreferences,
                                onSetAutoplayCountdownSeconds = onSetAutoplayCountdownSeconds,
                                onSetUpNextDisplayMode = onSetUpNextDisplayMode,
                                onTogglePassoutProtection = onTogglePassoutProtection,
                                onSetPassoutProtectionLimitMinutes = onSetPassoutProtectionLimitMinutes,
                                onSelectVideoPlayerPreference = onSelectVideoPlayerPreference,
                                onSetMaxStreamingBitrateMbps = onSetMaxStreamingBitrateMbps,
                                onSetMediaSegmentBehavior = onSetMediaSegmentBehavior,
                                modifier = Modifier.padding(start = scaffoldSidebarInset),
                            )
                        }

                        is TvRoute.Details -> {
                            TvDetailScreen(
                                detail = state.mediaDetail,
                                session = session,
                                contentStartPadding = fullBleedContentStartPadding,
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

            // --- EXPANDING SIDEBAR OVERLAY ---
            VantafynTvSidebar(
                mainItems = mainNavItems,
                bottomItems = bottomNavItems,
                currentRoute = navState.currentRoute,
                isExpanded = navState.isSidebarExpanded,
                accentMode = state.bottomRailAccent,
                session = session,
                modifier = Modifier.align(Alignment.CenterStart),
                settingsMode = navState.currentRoute == TvRoute.Settings,
                settingsItems = settingsNavItems,
                selectedSettingsCategory = settingsCategory,
                onSettingsCategorySelected = { settingsCategory = it },
                onExpand = { navState.expandSidebar() },
                onCollapse = { navState.collapseSidebar() },
                onRouteSelected = { route ->
                    navState.navigateToFromDrawer(route)
                },
                onProfileClicked = {
                    navState.navigateToFromDrawer(TvRoute.Settings)
                },
            )
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

private fun VantafynAppBackground.tvDrawableResId(): Int =
    when (this) {
        VantafynAppBackground.Nebula -> CoreUiR.drawable.vantafyn_onboarding_background
        VantafynAppBackground.Background1 -> CoreUiR.drawable.vantafyn_background_1
        VantafynAppBackground.Background2 -> CoreUiR.drawable.vantafyn_background_2
        VantafynAppBackground.Background3 -> CoreUiR.drawable.vantafyn_background_3
        VantafynAppBackground.Background4 -> CoreUiR.drawable.vantafyn_background_4
    }

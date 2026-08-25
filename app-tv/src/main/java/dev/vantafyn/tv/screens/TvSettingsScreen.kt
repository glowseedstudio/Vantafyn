package dev.vantafyn.tv.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinMediaSegmentBehavior
import dev.vantafyn.core.jellyfin.JellyfinMediaSegmentType
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.media.UpNextDisplayMode
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynThemePreset
import dev.vantafyn.core.ui.tokensFor
import dev.vantafyn.feature.home.auth.BottomRailAccent
import dev.vantafyn.feature.home.auth.MAX_STREAMING_BITRATE_MBPS_OPTIONS
import dev.vantafyn.feature.home.auth.ThemeMusicVolume
import dev.vantafyn.feature.home.auth.VantafynAppBackground
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynVideoPlayerPreference
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvScreenScaffold
import dev.vantafyn.tv.components.vantafynTvFocusable
import dev.vantafyn.tv.nav.TvSettingsCategory
import dev.vantafyn.core.ui.R as CoreUiR

@Composable
fun TvSettingsScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    category: TvSettingsCategory,
    modifier: Modifier = Modifier,
    onSelectProfile: (SavedProfile) -> Unit = {},
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
    VantafynTvScreenScaffold(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TvSettingsPageHeader(category)

            AnimatedContent(
                targetState = category,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
                },
                label = "TvSettingsCategoryContent",
                modifier = Modifier.weight(1f),
            ) { target ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    when (target) {
                        TvSettingsCategory.Appearance -> appearanceItems(
                            state = state,
                            onSelectTheme = onSelectTheme,
                            onSelectBackground = onSelectBackground,
                            onToggleThemeMusic = onToggleThemeMusic,
                            onSelectThemeMusicVolume = onSelectThemeMusicVolume,
                            onSetBottomRailAccent = onSetBottomRailAccent,
                            onToggleSoundEffects = onToggleSoundEffects,
                        )
                        TvSettingsCategory.Profile -> profileItems(
                            state = state,
                            session = session,
                            onSelectProfile = onSelectProfile,
                            onLogout = onLogout,
                        )
                        TvSettingsCategory.Playback -> playbackItems(
                            state = state,
                            onEditPlaybackPreferences = onEditPlaybackPreferences,
                            onSavePlaybackPreferences = onSavePlaybackPreferences,
                            onSetAutoplayCountdownSeconds = onSetAutoplayCountdownSeconds,
                            onSetUpNextDisplayMode = onSetUpNextDisplayMode,
                            onTogglePassoutProtection = onTogglePassoutProtection,
                            onSetPassoutProtectionLimitMinutes = onSetPassoutProtectionLimitMinutes,
                            onSelectVideoPlayerPreference = onSelectVideoPlayerPreference,
                            onSetMaxStreamingBitrateMbps = onSetMaxStreamingBitrateMbps,
                            onSetMediaSegmentBehavior = onSetMediaSegmentBehavior,
                        )
                        TvSettingsCategory.Permissions -> permissionsItems()
                        TvSettingsCategory.Vantafyn -> vantafynItems(
                            state = state,
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
                        )
                        TvSettingsCategory.About -> aboutItems(session)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appearanceItems(
    state: VantafynHomeUiState,
    onSelectTheme: (VantafynThemePreset) -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectThemeMusicVolume: (ThemeMusicVolume) -> Unit,
    onSetBottomRailAccent: (BottomRailAccent) -> Unit,
    onToggleSoundEffects: () -> Unit,
) {
    item {
        TvSettingsPanel(
            title = "Theme",
            icon = Icons.Rounded.AutoAwesome,
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(VantafynThemePreset.entries, key = { it.id }) { preset ->
                    TvThemeCard(
                        preset = preset,
                        selected = state.selectedTheme == preset,
                        onClick = { onSelectTheme(preset) },
                    )
                }
            }
        }
    }
    item {
        TvSettingsPanel(title = "Background", icon = Icons.Rounded.Language) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(VantafynAppBackground.entries, key = { it.name }) { background ->
                    TvBackgroundTile(
                        background = background,
                        selected = state.selectedBackground == background,
                        onClick = { onSelectBackground(background) },
                    )
                }
            }
        }
    }
    item {
        TvSettingsPanel(title = "Sound & side rail", icon = Icons.AutoMirrored.Rounded.VolumeUp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TvToggleRow("Theme music", state.themeMusicEnabled, onToggleThemeMusic)
                    TvToggleRow("Interface sounds", state.soundEffectsEnabled, onToggleSoundEffects)
                }
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    TvSegmentedSelector(
                        title = "Volume",
                        entries = ThemeMusicVolume.entries,
                        label = { it.label },
                        selected = state.themeMusicVolume,
                        enabled = state.themeMusicEnabled,
                        onSelect = onSelectThemeMusicVolume,
                    )
                    TvSegmentedSelector(
                        title = "Side rail accent",
                        entries = BottomRailAccent.entries,
                        label = { it.label },
                        selected = state.bottomRailAccent,
                        onSelect = onSetBottomRailAccent,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.profileItems(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    onSelectProfile: (SavedProfile) -> Unit,
    onLogout: () -> Unit,
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TvSettingsPanel(
                title = "Active profile",
                icon = Icons.Rounded.Person,
                modifier = Modifier.weight(0.85f),
            ) {
                val avatarUrl = session?.server?.url?.trimEnd('/')?.let { "${it}/Users/${session.user.id}/Images/Primary" }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.32f))
                            .border(BorderStroke(1.5.dp, VantafynGradients.accentHorizontal()), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = session?.user?.name ?: "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(session?.user?.name ?: "Vantafyn", color = VantafynColors.Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text(session?.server?.name ?: session?.server?.url.orEmpty(), color = VantafynColors.Muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                VantafynTvGlassButton("Logout", onClick = onLogout, icon = Icons.AutoMirrored.Rounded.ExitToApp, compact = true)
            }
            TvSettingsPanel(
                title = "Saved profiles",
                icon = Icons.Rounded.AccountCircle,
                modifier = Modifier.weight(1.15f),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(state.savedProfiles, key = { it.id }) { profile ->
                        TvSavedProfileCard(
                            profile = profile,
                            session = session,
                            selected = profile.jellyfinUserId == session?.user?.id,
                            onClick = { onSelectProfile(profile) },
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.playbackItems(
    state: VantafynHomeUiState,
    onEditPlaybackPreferences: ((JellyfinUserPlaybackPreferences) -> JellyfinUserPlaybackPreferences) -> Unit,
    onSavePlaybackPreferences: () -> Unit,
    onSetAutoplayCountdownSeconds: (Int) -> Unit,
    onSetUpNextDisplayMode: (UpNextDisplayMode) -> Unit,
    onTogglePassoutProtection: () -> Unit,
    onSetPassoutProtectionLimitMinutes: (Int) -> Unit,
    onSelectVideoPlayerPreference: (VantafynVideoPlayerPreference) -> Unit,
    onSetMaxStreamingBitrateMbps: (Int?) -> Unit,
    onSetMediaSegmentBehavior: (JellyfinMediaSegmentType, JellyfinMediaSegmentBehavior) -> Unit,
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TvSettingsPanel(
                title = "Video player",
                icon = Icons.Rounded.PlayCircle,
                modifier = Modifier.weight(0.92f),
            ) {
                TvSegmentedSelector(
                    title = "Player",
                    entries = VantafynVideoPlayerPreference.entries,
                    label = { it.label },
                    selected = state.videoPlayerPreference,
                    onSelect = onSelectVideoPlayerPreference,
                )
            }
            TvSettingsPanel(
                title = "Streaming quality",
                icon = Icons.Rounded.Speed,
                modifier = Modifier.weight(1.08f),
            ) {
                TvSegmentedSelector(
                    title = "Maximum streaming bitrate",
                    entries = listOf<Int?>(null) + MAX_STREAMING_BITRATE_MBPS_OPTIONS,
                    label = { it?.let { value -> "${value} Mbps" } ?: "Auto" },
                    selected = state.maxStreamingBitrateMbps,
                    onSelect = onSetMaxStreamingBitrateMbps,
                )
            }
        }
    }
    state.editablePlaybackPreferences?.let { preferences ->
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                TvSettingsPanel(
                    title = "Playback",
                    icon = Icons.Rounded.Speed,
                    modifier = Modifier.weight(1f),
                ) {
                    TvToggleRow("Autoplay next episode", preferences.enableNextEpisodeAutoPlay) {
                        onEditPlaybackPreferences { it.copy(enableNextEpisodeAutoPlay = !it.enableNextEpisodeAutoPlay) }
                    }
                    TvSegmentedSelector(
                        title = "Show Up Next",
                        entries = UpNextDisplayMode.entries,
                        label = { if (it == UpNextDisplayMode.BeforeEnd) "Before end" else "After finish" },
                        selected = state.upNextDisplayMode,
                        onSelect = onSetUpNextDisplayMode,
                    )
                    TvSegmentedSelector(
                        title = "Countdown",
                        entries = listOf(5, 10, 15, 30),
                        label = { "${it}s" },
                        selected = state.autoplayCountdownSeconds,
                        onSelect = onSetAutoplayCountdownSeconds,
                    )
                }
                TvSettingsPanel(
                    title = "Session safety",
                    icon = Icons.Rounded.Bedtime,
                    modifier = Modifier.weight(1f),
                ) {
                    TvToggleRow("Passout protection", state.passoutProtectionEnabled, onTogglePassoutProtection)
                    TvSegmentedSelector(
                        title = "Continue playing limit",
                        entries = listOf(60, 120, 180, 240, 300),
                        label = { "${it / 60}h" },
                        selected = state.passoutProtectionLimitMinutes,
                        enabled = state.passoutProtectionEnabled,
                        onSelect = onSetPassoutProtectionLimitMinutes,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                TvSettingsPanel(
                    title = "Audio",
                    icon = Icons.Rounded.GraphicEq,
                    modifier = Modifier.weight(1f),
                ) {
                    TvToggleRow("Play default audio track", preferences.playDefaultAudioTrack) {
                        onEditPlaybackPreferences { it.copy(playDefaultAudioTrack = !it.playDefaultAudioTrack) }
                    }
                    TvToggleRow("Remember audio selections", preferences.rememberAudioSelections) {
                        onEditPlaybackPreferences { it.copy(rememberAudioSelections = !it.rememberAudioSelections) }
                    }
                    TvInfoRow(Icons.Rounded.Language, "Audio language", preferences.audioLanguagePreference.orEmpty().ifBlank { "Jellyfin default" })
                }
                TvSettingsPanel(
                    title = "Subtitles",
                    icon = Icons.Rounded.Subtitles,
                    modifier = Modifier.weight(1f),
                ) {
                    TvToggleRow("Remember subtitle selections", preferences.rememberSubtitleSelections) {
                        onEditPlaybackPreferences { it.copy(rememberSubtitleSelections = !it.rememberSubtitleSelections) }
                    }
                    TvInfoRow(Icons.Rounded.Subtitles, "Subtitle mode", preferences.subtitleMode.orEmpty().subtitleModeDisplayLabel().ifBlank { "Jellyfin default" })
                    TvInfoRow(Icons.Rounded.Language, "Subtitle language", preferences.subtitleLanguagePreference.orEmpty().ifBlank { "Jellyfin default" })
                }
            }
        }
    }
    item {
        TvSettingsPanel(title = "Skip Segments", icon = Icons.Rounded.SkipNext) {
            listOf(
                JellyfinMediaSegmentType.Intro,
                JellyfinMediaSegmentType.Recap,
                JellyfinMediaSegmentType.Outro,
                JellyfinMediaSegmentType.Commercial,
                JellyfinMediaSegmentType.Preview,
            ).forEach { type ->
                TvSegmentedSelector(
                    title = type.segmentPreferenceLabel(),
                    entries = JellyfinMediaSegmentBehavior.entries,
                    label = { it.segmentBehaviorLabel() },
                    selected = state.mediaSegmentBehaviors[type] ?: JellyfinMediaSegmentBehavior.DoNothing,
                    onSelect = { onSetMediaSegmentBehavior(type, it) },
                )
            }
            VantafynTvGlassButton(
                text = if (state.isPlaybackPreferencesSaving) "Saving" else "Save Preferences",
                onClick = onSavePlaybackPreferences,
                isPrimary = true,
                enabled = !state.isPlaybackPreferencesSaving,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.permissionsItems() {
    item {
        TvSettingsPanel(title = "Permissions", icon = Icons.Rounded.Security) {
            TvInfoRow(Icons.Rounded.Dns, "Network", "Required for Jellyfin streaming")
            TvInfoRow(Icons.Rounded.CloudDownload, "Downloads", "Foreground service for offline saves")
            TvInfoRow(Icons.AutoMirrored.Rounded.VolumeUp, "Music playback", "Foreground service and media controls")
            TvInfoRow(Icons.Rounded.Notifications, "Notifications", "Used for playback and service status where Android requires it")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.vantafynItems(
    state: VantafynHomeUiState,
    onToggleAutoLoginLastProfile: () -> Unit,
    onToggleWhatsNew: () -> Unit,
    onToggleAchievementsEnabled: () -> Unit,
    onToggleSocialEnabled: () -> Unit,
    onToggleSocialDockEnabled: () -> Unit,
    onSetDownloadWifiOnlyDefault: (Boolean) -> Unit,
    onToggleWatchPartyEnabled: () -> Unit,
    onToggleWatchPartyInvitesEnabled: () -> Unit,
    onToggleWatchPartyInviteAnimationEnabled: () -> Unit,
    onSetWatchPartyInviteExpirySeconds: (Int) -> Unit,
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TvSettingsPanel(
                title = "App experience",
                icon = Icons.Rounded.NewReleases,
                modifier = Modifier.weight(1f),
            ) {
                TvToggleRow("What's New notifications", state.whatsNewEnabled, onToggleWhatsNew)
                TvInfoRow(
                    Icons.Rounded.Notifications,
                    "Unseen updates",
                    if (state.hasUnseenWhatsNew) "${state.whatsNewItems.size} new items" else "All caught up",
                )
                TvToggleRow("Use last profile on launch", state.autoLoginLastProfile, onToggleAutoLoginLastProfile)
            }
            TvSettingsPanel(
                title = "Social",
                icon = Icons.Rounded.Forum,
                modifier = Modifier.weight(1f),
            ) {
                TvToggleRow("Achievement Badges", state.achievementsEnabled, onToggleAchievementsEnabled)
                if (state.achievementsEnabled) {
                    TvInfoRow(
                        Icons.Rounded.EmojiEvents,
                        "Achievements service",
                        if (state.isAchievementsAvailable) "Available" else "Waiting for companion plugin",
                    )
                    TvToggleRow("Friends & Messaging", state.socialEnabled, onToggleSocialEnabled)
                    if (state.socialEnabled) {
                        TvToggleRow("Floating Chat Bubble", state.socialDockEnabled, onToggleSocialDockEnabled)
                    }
                }
            }
        }
    }
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TvSettingsPanel(
                title = "Downloads",
                icon = Icons.Rounded.Download,
                modifier = Modifier.weight(0.86f),
            ) {
                TvToggleRow("Wi-Fi only by default", state.downloadWifiOnlyDefault) {
                    onSetDownloadWifiOnlyDefault(!state.downloadWifiOnlyDefault)
                }
                TvInfoRow(
                    Icons.Rounded.CloudDownload,
                    "Offline library",
                    "Managed by Vantafyn's shared downloads engine",
                )
            }
            TvSettingsPanel(
                title = "Watch Party",
                icon = Icons.Rounded.Groups,
                modifier = Modifier.weight(1.14f),
            ) {
                TvToggleRow("Watch Party", state.watchPartyEnabled, onToggleWatchPartyEnabled)
                TvToggleRow("Invites", state.watchPartyInvitesEnabled, onToggleWatchPartyInvitesEnabled)
                TvToggleRow("Invite animation", state.watchPartyInviteAnimationEnabled, onToggleWatchPartyInviteAnimationEnabled)
                TvSegmentedSelector(
                    title = "Invite stays open",
                    entries = listOf(30, 60, 300),
                    label = {
                        when (it) {
                            30 -> "30 sec"
                            60 -> "1 min"
                            else -> "5 min"
                        }
                    },
                    selected = state.watchPartyInviteExpirySeconds,
                    enabled = state.watchPartyInvitesEnabled,
                    onSelect = onSetWatchPartyInviteExpirySeconds,
                )
            }
        }
    }
    item {
        TvSettingsPanel(title = "Vantafyn on TV", icon = Icons.Rounded.Tv) {
            TvInfoRow(Icons.Rounded.Tv, "Discover Vantafyn", "Feature guide is available from the mobile app")
            TvInfoRow(Icons.Rounded.Info, "App version", VANTAFYN_TV_APP_VERSION)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.aboutItems(session: JellyfinSession?) {
    item {
        TvSettingsPanel(title = "About Vantafyn TV", icon = Icons.Rounded.Info) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                VantafynLogoBadge(size = 72.dp, shape = RoundedCornerShape(20.dp))
                Column {
                    Text("Vantafyn TV", color = VantafynColors.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Private Jellyfin streaming for the living room.", color = VantafynColors.Muted, fontSize = 15.sp)
                }
            }
            TvInfoRow(Icons.Rounded.Dns, "Server", session?.server?.url.orEmpty().ifBlank { "Not connected" })
            TvInfoRow(Icons.Rounded.CheckCircle, "Privacy", "No analytics, trackers, or advertising SDKs")
        }
    }
}

@Composable
private fun TvSettingsPageHeader(category: TvSettingsCategory) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Settings",
            color = VantafynColors.Ink,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${category.label} · ${category.settingsSubtitle()}",
            color = VantafynColors.Muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun TvSettingsCategory.settingsSubtitle(): String =
    when (this) {
        TvSettingsCategory.Appearance -> "Theme, background and interface styling"
        TvSettingsCategory.Profile -> "Active account, saved users and server session"
        TvSettingsCategory.Playback -> "Player, bitrate, Up Next, audio, subtitles and skip behavior"
        TvSettingsCategory.Permissions -> "Android TV access used by Vantafyn"
        TvSettingsCategory.Vantafyn -> "App features, social, downloads and Watch Party"
        TvSettingsCategory.About -> "Version, privacy and server details"
    }

@Composable
private fun TvSettingsPanel(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(if (compact) 20.dp else 24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xD9232B40),
                        Color(0xF00A0E19),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), shape)
            .padding(if (compact) 18.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(if (compact) 38.dp else 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(if (compact) 20.dp else 24.dp))
            }
            Text(title, color = VantafynColors.Ink, fontSize = if (compact) 18.sp else 22.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun TvThemeCard(
    preset: VantafynThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = remember(preset) { tokensFor(preset) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(22.dp)
    val scale by animateFloatAsState(
        targetValue = if (isFocused || selected) 1.03f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tvThemeCardScale",
    )
    Column(
        modifier = Modifier
            .width(210.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .vantafynTvFocusable(interactionSource, shape = shape, scaleFocused = 1f)
            .clip(shape)
            .background(Color(0xDD111827))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(tokens.backgroundGradient))
                .border(BorderStroke(1.dp, Brush.horizontalGradient(tokens.accentColors)), RoundedCornerShape(18.dp)),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(preset.label, color = VantafynColors.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = VantafynColors.Secondary, modifier = Modifier.size(22.dp))
        }
        Text(if (selected) "Active now" else preset.description, color = if (selected) VantafynColors.Secondary else VantafynColors.Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TvBackgroundTile(
    background: VantafynAppBackground,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(238.dp)
            .vantafynTvFocusable(interactionSource, shape = shape, scaleFocused = 1.03f)
            .clip(shape)
            .background(Color(0xD90B0F1C))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(background.drawableResId()),
            contentDescription = background.label,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(background.label, color = VantafynColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            if (selected || isFocused) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VantafynColors.Secondary, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun TvSavedProfileCard(
    profile: SavedProfile,
    session: JellyfinSession?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    val avatarUrl = profile.imageUrl ?: session?.server?.url?.trimEnd('/')?.let { "${it}/Users/${profile.jellyfinUserId}/Images/Primary" }
    Column(
        modifier = Modifier
            .width(170.dp)
            .vantafynTvFocusable(interactionSource, shape = shape, scaleFocused = 1.03f)
            .clip(shape)
            .background(Color(0xD90B0F1C))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(BorderStroke(1.5.dp, if (selected) VantafynGradients.accentHorizontal() else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.18f)))), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = profile.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(profile.displayName, color = VantafynColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (selected) "Active" else "Switch", color = if (selected) VantafynColors.Secondary else VantafynColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TvToggleRow(
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .vantafynTvFocusable(interactionSource, shape = shape, scaleFocused = 1.015f)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.055f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, color = VantafynColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        TvSwitchVisual(checked)
    }
}

@Composable
private fun TvSwitchVisual(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) VantafynGradients.accentHorizontal() else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.10f))))
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f)),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> TvSegmentedSelector(
    title: String,
    entries: List<T>,
    label: (T) -> String,
    selected: T,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, color = VantafynColors.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry ->
                TvChoicePill(
                    text = label(entry),
                    selected = entry == selected,
                    enabled = enabled,
                    onClick = { onSelect(entry) },
                )
            }
        }
    }
}

@Composable
private fun TvChoicePill(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .widthIn(min = 96.dp)
            .vantafynTvFocusable(interactionSource, shape = shape, scaleFocused = 1.04f, borderWidth = if (selected) 2.dp else 1.5.dp)
            .clip(shape)
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.055f))
            .then(
                if (selected) {
                    Modifier.border(BorderStroke(1.4.dp, VantafynGradients.accentHorizontal()), shape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = when {
                !enabled -> VantafynColors.Muted.copy(alpha = 0.45f)
                selected -> VantafynColors.Ink
                else -> VantafynColors.Muted
            },
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun TvInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(22.dp))
        Text(title, color = VantafynColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f))
        Text(value, color = VantafynColors.Muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.2f))
    }
}

private fun VantafynAppBackground.drawableResId(): Int =
    when (this) {
        VantafynAppBackground.Nebula -> CoreUiR.drawable.vantafyn_onboarding_background
        VantafynAppBackground.Background1 -> CoreUiR.drawable.vantafyn_background_1
        VantafynAppBackground.Background2 -> CoreUiR.drawable.vantafyn_background_2
        VantafynAppBackground.Background3 -> CoreUiR.drawable.vantafyn_background_3
        VantafynAppBackground.Background4 -> CoreUiR.drawable.vantafyn_background_4
    }

private fun JellyfinMediaSegmentType.segmentPreferenceLabel(): String =
    when (this) {
        JellyfinMediaSegmentType.Intro -> "Intro"
        JellyfinMediaSegmentType.Recap -> "Recap"
        JellyfinMediaSegmentType.Outro -> "Credits"
        JellyfinMediaSegmentType.Commercial -> "Commercials"
        JellyfinMediaSegmentType.Preview -> "Previews"
        JellyfinMediaSegmentType.Unknown -> "Unknown"
    }

private fun JellyfinMediaSegmentBehavior.segmentBehaviorLabel(): String =
    when (this) {
        JellyfinMediaSegmentBehavior.Prompt -> "Prompt"
        JellyfinMediaSegmentBehavior.AutoSkip -> "Auto Skip"
        JellyfinMediaSegmentBehavior.DoNothing -> "Do Nothing"
    }

private fun String.subtitleModeDisplayLabel(): String =
    when (this) {
        "OnlyForced" -> "Forced only"
        "None" -> "Off"
        else -> this
    }

private const val VANTAFYN_TV_APP_VERSION = "0.9.2"

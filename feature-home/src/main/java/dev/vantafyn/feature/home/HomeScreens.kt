package dev.vantafyn.feature.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dev.vantafyn.core.cast.GoogleCastRouteButton
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.jellyfin.JellyfinLibraryItemFilter
import dev.vantafyn.core.jellyfin.JellyfinLibraryPage
import dev.vantafyn.core.jellyfin.JellyfinHomeSection
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinEpisode
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import dev.vantafyn.core.jellyfin.JellyfinMediaCardShape
import dev.vantafyn.core.jellyfin.JellyfinMediaItem
import dev.vantafyn.core.jellyfin.JellyfinPublicUser
import dev.vantafyn.core.jellyfin.JellyfinSearchResult
import dev.vantafyn.core.jellyfin.JellyfinHeroMediaItem
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.jellyfin.SyncPlayConnectionState
import dev.vantafyn.core.jellyfin.WatchPartyCandidate
import dev.vantafyn.core.jellyfin.WatchPartyInvite
import dev.vantafyn.core.jellyfin.WatchPartyMatchRule
import dev.vantafyn.core.jellyfin.WatchPartyMediaScope
import dev.vantafyn.core.jellyfin.WatchPartyMode
import dev.vantafyn.core.jellyfin.WatchPartyMemberPlaybackStatus
import dev.vantafyn.core.jellyfin.WatchPartyMemberPresence
import dev.vantafyn.core.jellyfin.WatchPartyMemberReadyStatus
import dev.vantafyn.core.jellyfin.WatchPartyRules
import dev.vantafyn.core.jellyfin.WatchPartySelectedMedia
import dev.vantafyn.core.jellyfin.WatchPartyVoteValue
import dev.vantafyn.core.media.AppForegroundStateRepository
import dev.vantafyn.core.media.LongRunningTaskRegistry
import dev.vantafyn.core.media.LongRunningTaskType
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.UpNextCandidate
import dev.vantafyn.core.media.VantafynMusicPlaybackState
import dev.vantafyn.core.media.VantafynPlaybackItem
import dev.vantafyn.core.ui.MobilePosterSpec
import dev.vantafyn.core.ui.PosterCard
import dev.vantafyn.core.ui.TvPosterSpec
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynBottomScrim
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynErrorCard
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassChip
import dev.vantafyn.core.ui.VantafynGlassDock
import dev.vantafyn.core.ui.VantafynGlassPanel
import dev.vantafyn.core.ui.VantafynGlassPill
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassTile
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynLogoHeader
import dev.vantafyn.core.ui.VantafynNavSelectedBrush
import dev.vantafyn.core.ui.VantafynOnboardingBackground
import dev.vantafyn.core.ui.VantafynPermissionUiState
import dev.vantafyn.core.ui.VantafynProfileCard
import dev.vantafyn.core.ui.VantafynScreenScaffold
import dev.vantafyn.core.ui.VantafynServerCard
import dev.vantafyn.core.ui.VantafynSetupHeader
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynHomeViewModel
import dev.vantafyn.feature.home.auth.HomeSectionType
import dev.vantafyn.feature.home.auth.MobileDestination
import dev.vantafyn.feature.home.auth.ThemeMusicVolume
import dev.vantafyn.feature.home.auth.HomeSectionPreference
import dev.vantafyn.feature.home.auth.VantafynAppBackground
import dev.vantafyn.feature.home.auth.VantafynArtworkType
import dev.vantafyn.feature.home.auth.VantafynCardShape
import dev.vantafyn.feature.home.auth.VantafynCardSize
import dev.vantafyn.feature.home.auth.VantafynCardSpacing
import dev.vantafyn.feature.home.auth.VantafynVideoPlayerPreference
import dev.vantafyn.feature.requests.RequestsScreen
import dev.vantafyn.feature.home.auth.VantafynSetupStep
import dev.vantafyn.feature.player.MobilePlayerScreen
import dev.vantafyn.feature.home.auth.supportedSmartRows
import dev.vantafyn.feature.music.MusicScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.io.ByteArrayOutputStream
import java.util.Date

@Composable
fun VantafynAppContent(
    tv: Boolean,
    modifier: Modifier = Modifier,
    notificationPermissionState: VantafynPermissionUiState = VantafynPermissionUiState(),
    onRequestMusicControlsPermission: ((() -> Unit) -> Unit) = { action -> action() },
    onNotificationPermissionSettingsAction: () -> Unit = {},
    viewModel: VantafynHomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backgroundResId = state.selectedBackground.drawableResId()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    AppForegroundStateRepository.setForeground(true)
                    viewModel.onAppForegrounded()
                }
                Lifecycle.Event.ON_STOP -> {
                    AppForegroundStateRepository.setForeground(false)
                    viewModel.onAppBackgrounded()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val duration = 620
                (
                    fadeIn(animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing))
                    ).togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)),
                    ).using(SizeTransform(clip = false))
            },
            label = "setupStepTransition",
        ) { step ->
            when (step) {
                VantafynSetupStep.Splash -> SplashScreen(tv = tv, backgroundResId = backgroundResId)
                VantafynSetupStep.Welcome -> WelcomeScreen(
                    state = state,
                    tv = tv,
                    onContinue = viewModel::continueFromWelcome,
                    onBack = viewModel::navigateSetupBack,
                    showBack = state.savedProfiles.isNotEmpty(),
                    backgroundResId = backgroundResId,
                )
                VantafynSetupStep.ConnectServer -> ConnectServerScreen(
                state = state,
                tv = tv,
                onServerUrlChanged = viewModel::onServerUrlChanged,
                onConnect = viewModel::connectToServer,
                onBack = viewModel::navigateSetupBack,
                backgroundResId = backgroundResId,
            )
                VantafynSetupStep.ServerConfirm -> ServerConfirmScreen(
                state = state,
                tv = tv,
                onContinue = viewModel::continueToLogin,
                onBack = viewModel::navigateSetupBack,
                backgroundResId = backgroundResId,
            )
                VantafynSetupStep.Login -> LoginScreen(
                state = state,
                tv = tv,
                onUsernameChanged = viewModel::onUsernameChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onLogin = viewModel::login,
                onQuickConnect = viewModel::startQuickConnect,
                onBack = viewModel::navigateSetupBack,
                backgroundResId = backgroundResId,
            )
                VantafynSetupStep.QuickConnect -> QuickConnectScreen(
                state = state,
                tv = tv,
                onBack = viewModel::navigateSetupBack,
                onRetry = viewModel::retryQuickConnect,
                backgroundResId = backgroundResId,
            )
                VantafynSetupStep.ProfilePicker -> ProfilePickerScreen(
                state = state,
                tv = tv,
                onSelect = viewModel::selectProfile,
                onSelectPublicUser = viewModel::selectPublicUser,
                onAddProfile = viewModel::addProfile,
                onToggleManage = viewModel::toggleManageProfiles,
                onRequestRemove = viewModel::requestRemoveProfile,
                onCancelRemove = viewModel::cancelRemoveProfile,
                onConfirmRemove = viewModel::confirmRemoveProfile,
                onBack = viewModel::navigateSetupBack,
                showBack = state.server != null || state.savedProfiles.isEmpty(),
                backgroundResId = backgroundResId,
            )
                VantafynSetupStep.ConnectionRecovery -> ConnectionRecoveryScreen(
                state = state,
                tv = tv,
                onServerUrlChanged = viewModel::onServerUrlChanged,
                onRetry = viewModel::retryFailedRestore,
                onSaveServerAddress = viewModel::saveRecoveryServerAddress,
                onUseAnotherServer = viewModel::useAnotherServerFromRecovery,
                onSignInAgain = viewModel::signInAgainFromRecovery,
                onChooseProfile = viewModel::showProfilePicker,
                onBack = viewModel::navigateSetupBack,
                backgroundResId = backgroundResId,
            )
                VantafynSetupStep.Home -> HomeScreen(
                state = state,
                tv = tv,
                onRetry = viewModel::retryLibraries,
                onSwitchUser = viewModel::showProfilePicker,
                onAddProfile = viewModel::addProfile,
                onQuickConnect = viewModel::startQuickConnect,
                onConfirmLogout = viewModel::confirmCurrentProfileLogout,
                onCancelLogout = viewModel::cancelCurrentProfileLogout,
                onLogoutCurrentProfile = viewModel::logoutCurrentProfile,
                onNavigateMobile = viewModel::navigateMobile,
                onOpenLibrary = viewModel::openLibrary,
                onRetryLibrary = viewModel::retryLibraryItems,
                onSetLibraryFilter = viewModel::setLibraryItemsFilter,
                onPreviousLibraryPage = viewModel::previousLibraryItemsPage,
                onNextLibraryPage = viewModel::nextLibraryItemsPage,
                onRefreshAdmin = viewModel::pollAdminOverview,
                onOpenMedia = viewModel::openMedia,
                onRetryMedia = viewModel::retryMediaDetail,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onLoadFavorites = viewModel::loadFavorites,
                onPlaybackComingSoon = viewModel::showPlaybackComingSoon,
                onClearMessage = viewModel::clearMobileMessage,
                onToggleHomeSection = viewModel::toggleHomeSection,
                onMoveHomeSection = viewModel::moveHomeSection,
                onResetHomeLayout = viewModel::resetHomeLayout,
                onAddSmartRow = viewModel::addSmartRow,
                onRemoveSmartRow = viewModel::removeSmartRow,
                onCycleArtwork = viewModel::cycleSectionArtwork,
                onCycleShape = viewModel::cycleSectionShape,
                onCycleSize = viewModel::cycleSectionSize,
                onCycleSpacing = viewModel::cycleSectionSpacing,
                onToggleThemeMusic = viewModel::toggleThemeMusic,
                onSelectThemeMusicVolume = viewModel::selectThemeMusicVolume,
                onToggleAutoLoginLastProfile = viewModel::toggleAutoLoginLastProfile,
                onSelectBackground = viewModel::selectBackground,
            onToggleMediaFavorite = viewModel::toggleMediaFavorite,
            onToggleMediaPlayed = viewModel::toggleMediaPlayed,
            onSetMediaFavorite = viewModel::setMediaFavorite,
            onSetMediaPlayed = viewModel::setMediaPlayed,
            onStartPlayback = { viewModel.startPlayback() },
            onStartPlaybackFromBeginning = viewModel::startPlaybackFromBeginning,
            onStartEpisodePlayback = viewModel::startEpisodePlayback,
            onSelectSeason = viewModel::selectSeason,
                onRetryPlayback = viewModel::retryPlayback,
                onTryTranscodedPlayback = viewModel::tryTranscodedPlayback,
                onExitPlayback = viewModel::exitPlayback,
                onPlaybackStarted = viewModel::reportPlaybackStarted,
                onPlaybackProgress = viewModel::reportPlaybackProgress,
                onPlaybackEnded = viewModel::exitPlayback,
                onPlayNextEpisode = viewModel::playNextEpisode,
            onPlayerError = viewModel::handlePlayerError,
            onPrepareCastPlayback = viewModel::prepareCastPlayback,
            onSelectPlaybackAudioTrack = viewModel::selectPlaybackAudioTrack,
                onSelectPlaybackSubtitleTrack = viewModel::selectPlaybackSubtitleTrack,
                onStartLiveTvPlayback = viewModel::startLiveTvPlayback,
                onEditPlaybackPreferences = viewModel::editPlaybackPreferences,
                onSavePlaybackPreferences = viewModel::savePlaybackPreferences,
                onSetAutoplayCountdownSeconds = viewModel::setAutoplayCountdownSeconds,
                onTogglePassoutProtection = viewModel::togglePassoutProtection,
                onSetPassoutProtectionLimitMinutes = viewModel::setPassoutProtectionLimitMinutes,
                onSelectVideoPlayerPreference = viewModel::selectVideoPlayerPreference,
                onExternalVideoPlayerLaunched = viewModel::externalVideoPlayerLaunched,
                onExternalVideoPlayerLaunchFailed = viewModel::externalVideoPlayerLaunchFailed,
                onChangePassword = viewModel::changeCurrentUserPassword,
                onOpenAdminUser = viewModel::openAdminUser,
                onCloseAdminUser = viewModel::closeAdminUser,
                onCreateAdminUser = viewModel::createAdminUser,
                onUpdateAdminUser = viewModel::updateSelectedAdminUser,
                onResetAdminPassword = viewModel::resetSelectedAdminPassword,
                onScanAdminLibrary = viewModel::scanAdminLibrary,
                onSetAdminPluginEnabled = viewModel::setAdminPluginEnabled,
                onRunAdminTask = viewModel::runAdminTask,
                onStopAdminTask = viewModel::stopAdminTask,
                onUploadCurrentProfileImage = viewModel::uploadCurrentUserProfileImage,
                onDeleteCurrentProfileImage = viewModel::deleteCurrentUserProfileImage,
                onUploadAdminProfileImage = viewModel::uploadSelectedAdminUserProfileImage,
                onDeleteAdminProfileImage = viewModel::deleteSelectedAdminUserProfileImage,
                onCreateWatchParty = viewModel::createWatchParty,
                onLoadWatchParty = viewModel::loadWatchParty,
                onLeaveWatchParty = viewModel::leaveWatchParty,
                onUpdateWatchPartyName = viewModel::updateWatchPartyName,
                onUpdateWatchPartyMode = viewModel::updateWatchPartyMode,
                onUpdateWatchPartyRules = viewModel::updateWatchPartyRules,
                onStartWatchPartyFromDetail = viewModel::startWatchPartyFromDetail,
                onLoadWatchPartyRecipients = viewModel::loadWatchPartyRecipients,
                onToggleWatchPartyRecipient = viewModel::toggleWatchPartyRecipient,
                onSendWatchPartyInvites = viewModel::sendWatchPartyInvites,
                onClearWatchPartyInviteAnimation = viewModel::clearWatchPartyInviteAnimation,
                onToggleWatchPartyReady = viewModel::toggleWatchPartyReady,
                onVoteWatchPartyCandidate = viewModel::voteWatchPartyCandidate,
                onStartMatchedWatchPartyPlayback = viewModel::startMatchedWatchPartyPlayback,
                onStartFixedWatchPartyPlayback = viewModel::startFixedWatchPartyPlayback,
                onToggleWatchPartyEnabled = viewModel::toggleWatchPartyEnabled,
                onToggleWatchPartyInvitesEnabled = viewModel::toggleWatchPartyInvitesEnabled,
                onToggleWatchPartyInviteAnimationEnabled = viewModel::toggleWatchPartyInviteAnimationEnabled,
                onSetWatchPartyInviteExpirySeconds = viewModel::setWatchPartyInviteExpirySeconds,
                onNavigateBack = viewModel::navigateMobileBack,
                notificationPermissionState = notificationPermissionState,
                onRequestMusicControlsPermission = onRequestMusicControlsPermission,
                onNotificationPermissionSettingsAction = onNotificationPermissionSettingsAction,
            )
            }
        }
        if (!tv) {
            WatchPartyInviteOverlay(
                invite = state.activeIncomingWatchPartyInvite,
                message = state.incomingWatchPartyMessage,
                onAccept = viewModel::acceptIncomingWatchPartyInvite,
                onDecline = viewModel::declineIncomingWatchPartyInvite,
                onClearMessage = viewModel::clearIncomingWatchPartyMessage,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun WatchPartyInviteOverlay(
    invite: WatchPartyInvite?,
    message: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember(invite?.inviteId) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(invite?.inviteId) {
        while (invite != null) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
    }
    AnimatedVisibility(
        visible = invite != null,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        enter = slideInVertically(animationSpec = tween(520, easing = FastOutSlowInEasing), initialOffsetY = { -it }) + fadeIn(tween(260)),
        exit = slideOutVertically(animationSpec = tween(420, easing = FastOutSlowInEasing), targetOffsetY = { -it }) + fadeOut(tween(260)),
    ) {
        val activeInvite = invite ?: return@AnimatedVisibility
        val remainingSeconds = ((activeInvite.expiresAt - now).coerceAtLeast(0L) / 1_000L).toInt()
        VantafynGlassPanel(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .vantafynAnimatedModalBorder(cornerRadius = 28.dp, strokeWidth = 1.5.dp),
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = activeInvite.mediaArtworkUrl,
                    contentDescription = activeInvite.mediaTitle,
                    modifier = Modifier
                        .size(width = 74.dp, height = 96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Watch Party Invite", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(activeInvite.inviteBody(), color = VantafynColors.Ink.copy(alpha = 0.84f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (activeInvite.mode == WatchPartyMode.SwipeToMatch) {
                        Text("Join the room, swipe through movies and shows, and Vantafyn will find a match.", color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(listOfNotNull(activeInvite.mediaType, remainingSeconds.takeIf { it > 0 }?.let { "${it}s left" }).joinToString(" · "), color = VantafynColors.Muted, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(activeInvite.hostDisplayName, imageUrl = null, modifier = Modifier.size(28.dp))
                        Text(activeInvite.hostDisplayName, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        VantafynButton("Accept", onClick = onAccept, enabled = remainingSeconds > 0, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = onDecline, modifier = Modifier.height(52.dp).weight(1f)) {
                            Text("Decline", color = VantafynColors.Ink)
                        }
                    }
                }
            }
        }
    }
    AnimatedVisibility(
        visible = invite == null && message != null,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(180)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(240)),
    ) {
        LaunchedEffect(message) {
            if (message != null) {
                kotlinx.coroutines.delay(1_600L)
                onClearMessage()
            }
        }
        VantafynGlassPill(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text(message.orEmpty(), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun WatchPartyInvite.inviteBody(): String =
    when (mode) {
        WatchPartyMode.FixedTitle -> "${hostDisplayName} invited you to watch ${mediaTitle ?: "a title"}."
        WatchPartyMode.SwipeToMatch -> "${hostDisplayName} invited you to pick something together."
    }

@Composable
fun SplashScreen(tv: Boolean, modifier: Modifier = Modifier, backgroundResId: Int = CoreUiR.drawable.vantafyn_onboarding_background, message: String = "Preparing Vantafyn") {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Vantafyn", color = VantafynColors.Ink, style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(VantafynSpacing.sm))
            Text(message, color = VantafynColors.Muted, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(VantafynSpacing.lg))
            VantafynLoadingIndicator("Signing in")
        }
    }
}

@Composable
private fun WelcomeScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        SetupBackScaffold(onBack = onBack, showBack = showBack) {
            CenterPane {
                VantafynLogoHeader(
                    title = "Vantafyn",
                    tagline = "Your media, beautifully streamed.",
                    tv = tv,
                )
                Spacer(Modifier.height(if (tv) 44.dp else 36.dp))
                VantafynButton("Continue", onClick = onContinue, modifier = Modifier.fillMaxWidth(if (tv) 0.42f else 0.68f))
                if (state.savedProfiles.isNotEmpty()) {
                    Spacer(Modifier.height(VantafynSpacing.lg))
                    Text("Add another Jellyfin profile", color = VantafynColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun ConnectServerScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onServerUrlChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onBack: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        SetupBackScaffold(onBack = onBack) {
            CenterPane {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                ) {
                    VantafynSetupHeader(
                        title = "Connect your server",
                        subtitle = "Use a local address or domain for your Jellyfin server.",
                        tv = tv,
                    )
                    Spacer(Modifier.height(if (tv) VantafynSpacing.md else VantafynSpacing.xs))
                    VantafynTextField(
                        value = state.serverUrl,
                        onValueChange = onServerUrlChanged,
                        label = "Server address",
                        placeholder = "http://192.168.1.29:8096",
                        enabled = !state.isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        tvKeyboardRequiresClick = tv,
                    )
                    Text("Example: https://media.example.com", color = VantafynColors.Muted.copy(alpha = 0.82f), style = MaterialTheme.typography.bodyLarge)
                    StatusBlock(state)
                    Spacer(Modifier.height(VantafynSpacing.sm))
                    VantafynButton(
                        "Connect",
                        onClick = onConnect,
                        enabled = !state.isLoading && state.serverUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(if (tv) 0.42f else 0.68f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerConfirmScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        SetupBackScaffold(onBack = onBack) {
            CenterPane {
                VantafynSetupHeader(
                    title = "Server found",
                    subtitle = "Ready to connect to your media library.",
                    tv = tv,
                )
                Spacer(Modifier.height(if (tv) VantafynSpacing.xxl else VantafynSpacing.xl))
                val server = state.server
                VantafynServerCard(
                    name = server?.name ?: "Jellyfin Server",
                    url = server?.url ?: state.serverUrl,
                    version = server?.version,
                ) {
                    val avatar = state.publicUsers.firstOrNull { it.isAdministrator && it.imageUrl != null }
                        ?: state.publicUsers.firstOrNull { it.displayName == state.session?.user?.name && it.imageUrl != null }
                        ?: state.publicUsers.firstOrNull { user -> state.savedProfiles.any { it.displayName == user.displayName } && user.imageUrl != null }
                    ProfileAvatar(
                        name = avatar?.displayName ?: state.session?.user?.name ?: server?.name ?: "Jellyfin Server",
                        imageUrl = avatar?.imageUrl,
                    )
                }
                StatusBlock(state)
                Spacer(Modifier.height(if (tv) VantafynSpacing.xxl else VantafynSpacing.xl))
                VantafynButton(
                    "Continue to Sign In",
                    onClick = onContinue,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(if (tv) 0.48f else 0.78f),
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onQuickConnect: () -> Unit,
    onBack: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        SetupBackScaffold(onBack = onBack) {
            CenterPane {
                VantafynSetupHeader(
                    title = state.server?.name ?: "Sign in",
                    subtitle = state.server?.url ?: state.serverUrl,
                    tv = tv,
                )
                Spacer(Modifier.height(VantafynSpacing.lg))
                VantafynTextField(
                    value = state.username,
                    onValueChange = onUsernameChanged,
                    label = "Username",
                    enabled = !state.isLoading,
                    tvKeyboardRequiresClick = tv,
                )
                Spacer(Modifier.height(VantafynSpacing.sm))
                VantafynTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    label = "Password",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    tvKeyboardRequiresClick = tv,
                )
                StatusBlock(state)
                Spacer(Modifier.height(VantafynSpacing.lg))
                VantafynButton(
                    "Sign In",
                    onClick = onLogin,
                    enabled = !state.isLoading && state.username.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(if (tv) 0.42f else 0.68f),
                )
                Spacer(Modifier.height(VantafynSpacing.sm))
                TextButton(onClick = onQuickConnect, enabled = state.server != null && !state.isLoading) {
                    Text("Use Quick Connect", color = VantafynColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun QuickConnectScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(VantafynSpacing.lg),
        ) {
            CompactBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart),
            )
            CenterPane {
                VantafynSetupHeader(
                    title = "Quick Connect",
                    subtitle = "Approve Vantafyn from your Jellyfin dashboard.",
                    tv = tv,
                )
                Spacer(Modifier.height(VantafynSpacing.xl))
                Box(
                    modifier = Modifier
                        .width(if (tv) 420.dp else 312.dp)
                        .height(if (tv) 238.dp else 210.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(VantafynColors.Surface.copy(alpha = 0.68f))
                        .padding(VantafynSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                        Text("Code", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            state.quickConnectSession?.code ?: "----",
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (tv) 76.dp else 68.dp),
                        )
                        Text(
                            state.quickConnectMessage ?: if (state.isLoading) "Preparing code..." else "Waiting for approval...",
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                StatusBlock(state)
                Spacer(Modifier.height(VantafynSpacing.lg))
                VantafynButton("Refresh Code", onClick = onRetry, enabled = !state.isLoading)
            }
        }
    }
}

@Composable
private fun SetupBackScaffold(
    onBack: () -> Unit,
    showBack: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (showBack) {
            CompactBackButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 16.dp, top = 14.dp),
            )
        }
    }
}

@Composable
private fun ConnectionRecoveryScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onServerUrlChanged: (String) -> Unit,
    onRetry: () -> Unit,
    onSaveServerAddress: () -> Unit,
    onUseAnotherServer: () -> Unit,
    onSignInAgain: () -> Unit,
    onChooseProfile: () -> Unit,
    onBack: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    val profile = state.restoreFailureProfile
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        SetupBackScaffold(onBack = onBack) {
            CenterPane {
            VantafynSetupHeader(
                title = "Can’t reach your server",
                subtitle = "Vantafyn couldn’t connect to the saved Jellyfin server for this profile.",
                tv = tv,
            )
            Spacer(Modifier.height(if (tv) VantafynSpacing.xl else VantafynSpacing.lg))
            VantafynGlassPanel(
                modifier = Modifier
                    .fillMaxWidth(if (tv) 0.62f else 1f)
                    .vantafynAnimatedModalBorder(),
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(profile?.displayName ?: "Saved profile", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(profile?.serverName ?: "Jellyfin Server", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(profile?.serverUrl ?: state.serverUrl, color = VantafynColors.Muted.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "You can retry, switch to a remote address, or sign in again without deleting this profile.",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if ((profile?.serverUrl ?: state.serverUrl).looksLocalServerAddress()) {
                        VantafynGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            variant = VantafynGlassVariant.Card,
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            Text(
                                "This address looks local. It usually only works on your home network. Use your remote Jellyfin address if you want access away from home.",
                                color = VantafynColors.Ink.copy(alpha = 0.86f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    VantafynTextField(
                        value = state.serverUrl,
                        onValueChange = onServerUrlChanged,
                        label = "Server address",
                        placeholder = "https://media.example.com",
                        enabled = !state.isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        tvKeyboardRequiresClick = tv,
                    )
                    state.restoreFailureMessage?.let { Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge) }
                }
            }
            StatusBlock(state)
            Spacer(Modifier.height(if (tv) VantafynSpacing.xl else VantafynSpacing.lg))
            Column(
                modifier = Modifier.fillMaxWidth(if (tv) 0.48f else 0.86f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VantafynButton("Retry", onClick = onRetry, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth())
                VantafynButton(
                    if ((profile?.serverUrl ?: state.serverUrl).looksLocalServerAddress()) "Enter remote address" else "Edit server address",
                    onClick = onSaveServerAddress,
                    enabled = !state.isLoading && state.serverUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = onUseAnotherServer, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                    Text("Use another server")
                }
                OutlinedButton(onClick = onSignInAgain, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in again")
                }
                if (state.savedProfiles.size > 1) {
                    TextButton(onClick = onChooseProfile, enabled = !state.isLoading) {
                        Text("Choose another profile")
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ProfilePickerScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onSelect: (SavedProfile) -> Unit,
    onSelectPublicUser: (JellyfinPublicUser) -> Unit,
    onAddProfile: () -> Unit,
    onToggleManage: () -> Unit,
    onRequestRemove: (SavedProfile) -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    if (tv) {
        TvProfilePickerScreen(
            state = state,
            onSelect = onSelect,
            onSelectPublicUser = onSelectPublicUser,
            onAddProfile = onAddProfile,
            onToggleManage = onToggleManage,
            onRequestRemove = onRequestRemove,
            onCancelRemove = onCancelRemove,
            onConfirmRemove = onConfirmRemove,
            onBack = onBack,
            showBack = showBack,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
    } else {
        MobileProfilePickerScreen(
            state = state,
            onSelect = onSelect,
            onSelectPublicUser = onSelectPublicUser,
            onAddProfile = onAddProfile,
            onToggleManage = onToggleManage,
            onRequestRemove = onRequestRemove,
            onCancelRemove = onCancelRemove,
            onConfirmRemove = onConfirmRemove,
            onBack = onBack,
            showBack = showBack,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
    }
}

@Composable
private fun MobileProfilePickerScreen(
    state: VantafynHomeUiState,
    onSelect: (SavedProfile) -> Unit,
    onSelectPublicUser: (JellyfinPublicUser) -> Unit,
    onAddProfile: () -> Unit,
    onToggleManage: () -> Unit,
    onRequestRemove: (SavedProfile) -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = false, modifier = modifier, backgroundResId = backgroundResId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            ManageProfilesButton(
                state = state,
                onToggleManage = onToggleManage,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            if (showBack) {
                CompactBackButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(52.dp))
                Text(
                    "Who's watching?",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(VantafynSpacing.xl))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    contentPadding = PaddingValues(bottom = VantafynSpacing.xl),
                ) {
                    gridItems(state.savedProfiles) { profile ->
                        SavedProfileTile(
                            profile = profile,
                            showServer = state.savedProfiles.map { it.serverRef }.distinct().size > 1,
                            warning = if (profile.id in state.failedProfileIds) "Server unreachable" else null,
                            manageMode = state.manageProfiles,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(198.dp),
                            onSelect = { onSelect(profile) },
                            onRemove = { onRequestRemove(profile) },
                        )
                    }
                    gridItems(state.publicUsers, key = { it.id }) { user ->
                        PublicUserTile(
                            user = user,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(198.dp),
                            onSelect = { onSelectPublicUser(user) },
                        )
                    }
                    item {
                        AddProfileTile(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(198.dp),
                            onClick = onAddProfile,
                        )
                    }
                }
                ProfilePickerFeedback(
                    state = state,
                    onCancelRemove = onCancelRemove,
                    onConfirmRemove = onConfirmRemove,
                )
            }
        }
    }
}

@Composable
private fun TvProfilePickerScreen(
    state: VantafynHomeUiState,
    onSelect: (SavedProfile) -> Unit,
    onSelectPublicUser: (JellyfinPublicUser) -> Unit,
    onAddProfile: () -> Unit,
    onToggleManage: () -> Unit,
    onRequestRemove: (SavedProfile) -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = true, modifier = modifier, backgroundResId = backgroundResId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = VantafynSpacing.tvGutter, vertical = 44.dp),
        ) {
            ManageProfilesButton(
                state = state,
                onToggleManage = onToggleManage,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            if (showBack) {
                CompactBackButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Who's watching?",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(46.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
                    contentPadding = PaddingValues(horizontal = VantafynSpacing.xl),
                ) {
                    items(state.savedProfiles) { profile ->
                        SavedProfileTile(
                            profile = profile,
                            showServer = state.savedProfiles.map { it.serverRef }.distinct().size > 1,
                            warning = if (profile.id in state.failedProfileIds) "Server unreachable" else null,
                            manageMode = state.manageProfiles,
                            modifier = Modifier
                                .width(150.dp)
                                .height(202.dp),
                            onSelect = { onSelect(profile) },
                            onRemove = { onRequestRemove(profile) },
                        )
                    }
                    items(state.publicUsers, key = { it.id }) { user ->
                        PublicUserTile(
                            user = user,
                            modifier = Modifier
                                .width(150.dp)
                                .height(202.dp),
                            onSelect = { onSelectPublicUser(user) },
                        )
                    }
                    item {
                        AddProfileTile(
                            modifier = Modifier
                                .width(150.dp)
                                .height(202.dp),
                            onClick = onAddProfile,
                        )
                    }
                }
                ProfilePickerFeedback(
                    state = state,
                    onCancelRemove = onCancelRemove,
                    onConfirmRemove = onConfirmRemove,
                )
            }
        }
    }
}

@Composable
private fun ManageProfilesButton(
    state: VantafynHomeUiState,
    onToggleManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.savedProfiles.isNotEmpty()) {
        TextButton(
            onClick = onToggleManage,
            modifier = modifier.widthIn(min = 96.dp),
        ) {
            Text(
                if (state.manageProfiles) "Done" else "Manage",
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProfilePickerFeedback(
    state: VantafynHomeUiState,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
) {
    if (state.isLoading) {
        Spacer(Modifier.height(VantafynSpacing.xl))
        VantafynLoadingIndicator("Restoring profile")
    }
    state.pendingRemoval?.let { profile ->
        Spacer(Modifier.height(VantafynSpacing.lg))
        VantafynErrorCard("Remove ${profile.displayName} from this device?") {
            VantafynButton("Remove", onClick = onConfirmRemove)
            Spacer(Modifier.width(VantafynSpacing.sm))
            OutlinedButton(onClick = onCancelRemove) {
                Text("Cancel")
            }
        }
    }
    state.errorMessage?.let {
        Spacer(Modifier.height(VantafynSpacing.lg))
        VantafynErrorCard(it)
    }
}

@Composable
private fun SavedProfileTile(
    profile: SavedProfile,
    showServer: Boolean,
    warning: String?,
    manageMode: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    VantafynProfileCard(
        label = profile.displayName,
        subtitle = warning ?: if (showServer) profile.serverName ?: profile.serverUrl else null,
        modifier = modifier,
        manageMode = manageMode,
        onClick = onSelect,
        onRemove = onRemove,
    ) {
        ProfileAvatar(
            name = profile.displayName,
            imageUrl = profile.imageUrl,
        )
    }
}

@Composable
private fun PublicUserTile(
    user: JellyfinPublicUser,
    modifier: Modifier,
    onSelect: () -> Unit,
) {
    VantafynProfileCard(
        label = user.displayName,
        modifier = modifier,
        onClick = onSelect,
    ) {
        ProfileAvatar(
            name = user.displayName,
            imageUrl = user.imageUrl,
        )
    }
}

@Composable
private fun AddProfileTile(modifier: Modifier, onClick: () -> Unit) {
    VantafynProfileCard(
        label = "Add Profile",
        modifier = modifier,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.56f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = VantafynColors.Ink, style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, imageUrl: String?, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF3D5266), Color(0xFF756A8A), Color(0xFF20252D)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(initials(name), color = VantafynColors.Ink, style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
private fun EditableProfileAvatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ProfileAvatar(name = name, imageUrl = imageUrl, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(27.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.62f))
                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class PreparedProfileImage(
    val bytes: ByteArray,
    val mimeType: String,
) {
    override fun equals(other: Any?): Boolean =
        other is PreparedProfileImage && bytes.contentEquals(other.bytes) && mimeType == other.mimeType

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + mimeType.hashCode()
}

private class ProfileImagePickerState(
    val open: () -> Unit,
    val Content: @Composable () -> Unit,
)

@Composable
private fun rememberProfileImagePicker(onUpload: (ByteArray, String) -> Unit): ProfileImagePickerState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prepared by remember { mutableStateOf<PreparedProfileImage?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isPreparing = true
            localError = null
            prepared = prepareProfileImage(context, uri)
                .onFailure { localError = "Couldn't read that image." }
                .getOrNull()
            isPreparing = false
        }
    }
    return ProfileImagePickerState(
        open = {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        Content = {
            when {
                isPreparing -> ProfileImageStatusCard(isSaving = true, error = null)
                localError != null -> ProfileImageStatusCard(isSaving = false, error = localError)
            }
            prepared?.let { image ->
                ProfileImagePreviewDialog(
                    image = image,
                    onDismiss = { prepared = null },
                    onUse = {
                        prepared = null
                        onUpload(image.bytes, image.mimeType)
                    },
                )
            }
        },
    )
}

@Composable
private fun ProfileImagePreviewDialog(
    image: PreparedProfileImage,
    onDismiss: () -> Unit,
    onUse: () -> Unit,
) {
    val bitmap = remember(image) { BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size) }
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        containerColor = VantafynColors.SurfaceHigh.copy(alpha = 0.96f),
        titleContentColor = VantafynColors.Ink,
        textContentColor = VantafynColors.Muted,
        title = { Text("Profile picture") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Profile picture preview",
                        modifier = Modifier
                            .size(164.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(999.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text("Vantafyn will upload a square image to Jellyfin.", textAlign = TextAlign.Center)
            }
        },
        confirmButton = { VantafynButton("Use Photo", onClick = onUse) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProfileImageStatusCard(isSaving: Boolean, error: String?) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(VantafynSpacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            if (isSaving) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = VantafynColors.Primary,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(
                text = if (isSaving) "Preparing profile picture..." else error.orEmpty(),
                color = if (error == null) VantafynColors.Muted else Color(0xFFFFC2C2),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private suspend fun prepareProfileImage(context: Context, uri: Uri): Result<PreparedProfileImage> =
    withContext(Dispatchers.IO) {
        runCatching {
            val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                context.contentResolver.openInputStream(uri).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } ?: throw IllegalArgumentException("Couldn't read that image.")
            val side = minOf(decoded.width, decoded.height)
            val cropped = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
            val scaled = if (cropped.width == 768 && cropped.height == 768) {
                cropped
            } else {
                Bitmap.createScaledBitmap(cropped, 768, 768, true)
            }
            val output = ByteArrayOutputStream()
            check(scaled.compress(Bitmap.CompressFormat.JPEG, 90, output)) { "Couldn't read that image." }
            PreparedProfileImage(bytes = output.toByteArray(), mimeType = "image/jpeg")
        }
    }

@Composable
private fun HomeScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onRetry: () -> Unit,
    onSwitchUser: () -> Unit,
    onAddProfile: () -> Unit,
    onQuickConnect: () -> Unit,
    onConfirmLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    onLogoutCurrentProfile: () -> Unit,
    onNavigateMobile: (MobileDestination) -> Unit,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onRetryLibrary: () -> Unit,
    onSetLibraryFilter: (JellyfinLibraryItemFilter) -> Unit,
    onPreviousLibraryPage: () -> Unit,
    onNextLibraryPage: () -> Unit,
    onRefreshAdmin: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onRetryMedia: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLoadFavorites: () -> Unit,
    onPlaybackComingSoon: () -> Unit,
    onClearMessage: () -> Unit,
    onToggleHomeSection: (HomeSectionType) -> Unit,
    onMoveHomeSection: (HomeSectionType, Int) -> Unit,
    onResetHomeLayout: () -> Unit,
    onAddSmartRow: (String) -> Unit,
    onRemoveSmartRow: (String) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectThemeMusicVolume: (ThemeMusicVolume) -> Unit,
    onToggleAutoLoginLastProfile: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onToggleMediaFavorite: () -> Unit,
    onToggleMediaPlayed: () -> Unit,
    onSetMediaFavorite: (java.util.UUID, Boolean) -> Unit,
    onSetMediaPlayed: (java.util.UUID, Boolean) -> Unit,
    onStartPlayback: () -> Unit,
    onStartPlaybackFromBeginning: () -> Unit,
    onStartEpisodePlayback: (JellyfinEpisode, Boolean) -> Unit,
    onSelectSeason: (java.util.UUID?) -> Unit,
    onRetryPlayback: () -> Unit,
    onTryTranscodedPlayback: () -> Unit,
    onExitPlayback: (Long) -> Unit,
    onPlaybackStarted: (Long) -> Unit,
    onPlaybackProgress: (Long, Boolean) -> Unit,
    onPlaybackEnded: (Long) -> Unit,
    onPlayNextEpisode: (UpNextCandidate, Long) -> Unit,
    onPlayerError: () -> Unit,
    onPrepareCastPlayback: (Long) -> Unit,
    onSelectPlaybackAudioTrack: (Int, Long) -> Unit,
    onSelectPlaybackSubtitleTrack: (Int?, Long) -> Unit,
    onStartLiveTvPlayback: (java.util.UUID, String, String?) -> Unit,
    onEditPlaybackPreferences: ((dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> Unit,
    onSavePlaybackPreferences: () -> Unit,
    onSetAutoplayCountdownSeconds: (Int) -> Unit,
    onTogglePassoutProtection: () -> Unit,
    onSetPassoutProtectionLimitMinutes: (Int) -> Unit,
    onSelectVideoPlayerPreference: (VantafynVideoPlayerPreference) -> Unit,
    onExternalVideoPlayerLaunched: () -> Unit,
    onExternalVideoPlayerLaunchFailed: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onOpenAdminUser: (java.util.UUID) -> Unit,
    onCloseAdminUser: () -> Unit,
    onCreateAdminUser: (String, String) -> Unit,
    onUpdateAdminUser: (Boolean?, Boolean?, Boolean?, Boolean?, List<java.util.UUID>?) -> Unit,
    onResetAdminPassword: (String) -> Unit,
    onScanAdminLibrary: () -> Unit,
    onSetAdminPluginEnabled: (java.util.UUID, String?, Boolean) -> Unit,
    onRunAdminTask: (String) -> Unit,
    onStopAdminTask: (String) -> Unit,
    onUploadCurrentProfileImage: (ByteArray, String) -> Unit,
    onDeleteCurrentProfileImage: () -> Unit,
    onUploadAdminProfileImage: (ByteArray, String) -> Unit,
    onDeleteAdminProfileImage: () -> Unit,
    onCreateWatchParty: () -> Unit,
    onLoadWatchParty: () -> Unit,
    onLeaveWatchParty: () -> Unit,
    onUpdateWatchPartyName: (String) -> Unit,
    onUpdateWatchPartyMode: (WatchPartyMode) -> Unit,
    onUpdateWatchPartyRules: (dev.vantafyn.core.jellyfin.WatchPartyRules) -> Unit,
    onStartWatchPartyFromDetail: (WatchPartyMode) -> Unit,
    onLoadWatchPartyRecipients: () -> Unit,
    onToggleWatchPartyRecipient: (String) -> Unit,
    onSendWatchPartyInvites: () -> Unit,
    onClearWatchPartyInviteAnimation: () -> Unit,
    onToggleWatchPartyReady: () -> Unit,
    onVoteWatchPartyCandidate: (dev.vantafyn.core.jellyfin.WatchPartyVoteValue) -> Unit,
    onStartMatchedWatchPartyPlayback: () -> Unit,
    onStartFixedWatchPartyPlayback: () -> Unit,
    onToggleWatchPartyEnabled: () -> Unit,
    onToggleWatchPartyInvitesEnabled: () -> Unit,
    onToggleWatchPartyInviteAnimationEnabled: () -> Unit,
    onSetWatchPartyInviteExpirySeconds: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    notificationPermissionState: VantafynPermissionUiState = VantafynPermissionUiState(),
    onRequestMusicControlsPermission: ((() -> Unit) -> Unit) = { action -> action() },
    onNotificationPermissionSettingsAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!tv) {
        MobileShellScreen(
            state = state,
            onRetryHome = onRetry,
            onSwitchUser = onSwitchUser,
            onAddProfile = onAddProfile,
            onQuickConnect = onQuickConnect,
            onConfirmLogout = onConfirmLogout,
            onCancelLogout = onCancelLogout,
            onLogoutCurrentProfile = onLogoutCurrentProfile,
            onNavigate = onNavigateMobile,
            onOpenLibrary = onOpenLibrary,
            onRetryLibrary = onRetryLibrary,
            onSetLibraryFilter = onSetLibraryFilter,
            onPreviousLibraryPage = onPreviousLibraryPage,
            onNextLibraryPage = onNextLibraryPage,
            onRefreshAdmin = onRefreshAdmin,
            onOpenMedia = onOpenMedia,
            onRetryMedia = onRetryMedia,
            onSearchQueryChanged = onSearchQueryChanged,
            onLoadFavorites = onLoadFavorites,
            onPlaybackComingSoon = onPlaybackComingSoon,
            onClearMessage = onClearMessage,
            onToggleHomeSection = onToggleHomeSection,
            onMoveHomeSection = onMoveHomeSection,
            onResetHomeLayout = onResetHomeLayout,
            onAddSmartRow = onAddSmartRow,
            onRemoveSmartRow = onRemoveSmartRow,
            onCycleArtwork = onCycleArtwork,
            onCycleShape = onCycleShape,
            onCycleSize = onCycleSize,
            onCycleSpacing = onCycleSpacing,
            onToggleThemeMusic = onToggleThemeMusic,
            onSelectThemeMusicVolume = onSelectThemeMusicVolume,
            onToggleAutoLoginLastProfile = onToggleAutoLoginLastProfile,
            onSelectBackground = onSelectBackground,
            onToggleMediaFavorite = onToggleMediaFavorite,
            onToggleMediaPlayed = onToggleMediaPlayed,
            onSetMediaFavorite = onSetMediaFavorite,
            onSetMediaPlayed = onSetMediaPlayed,
            onStartPlayback = onStartPlayback,
            onStartPlaybackFromBeginning = onStartPlaybackFromBeginning,
            onStartEpisodePlayback = onStartEpisodePlayback,
            onSelectSeason = onSelectSeason,
            onRetryPlayback = onRetryPlayback,
            onTryTranscodedPlayback = onTryTranscodedPlayback,
            onExitPlayback = onExitPlayback,
            onPlaybackStarted = onPlaybackStarted,
            onPlaybackProgress = onPlaybackProgress,
            onPlaybackEnded = onPlaybackEnded,
            onPlayNextEpisode = onPlayNextEpisode,
            onPlayerError = onPlayerError,
            onPrepareCastPlayback = onPrepareCastPlayback,
            onSelectPlaybackAudioTrack = onSelectPlaybackAudioTrack,
            onSelectPlaybackSubtitleTrack = onSelectPlaybackSubtitleTrack,
            onStartLiveTvPlayback = onStartLiveTvPlayback,
            onEditPlaybackPreferences = onEditPlaybackPreferences,
            onSavePlaybackPreferences = onSavePlaybackPreferences,
            onSetAutoplayCountdownSeconds = onSetAutoplayCountdownSeconds,
            onTogglePassoutProtection = onTogglePassoutProtection,
            onSetPassoutProtectionLimitMinutes = onSetPassoutProtectionLimitMinutes,
            onSelectVideoPlayerPreference = onSelectVideoPlayerPreference,
            onExternalVideoPlayerLaunched = onExternalVideoPlayerLaunched,
            onExternalVideoPlayerLaunchFailed = onExternalVideoPlayerLaunchFailed,
            onChangePassword = onChangePassword,
            onOpenAdminUser = onOpenAdminUser,
            onCloseAdminUser = onCloseAdminUser,
            onCreateAdminUser = onCreateAdminUser,
            onUpdateAdminUser = onUpdateAdminUser,
            onResetAdminPassword = onResetAdminPassword,
            onScanAdminLibrary = onScanAdminLibrary,
            onSetAdminPluginEnabled = onSetAdminPluginEnabled,
            onRunAdminTask = onRunAdminTask,
            onStopAdminTask = onStopAdminTask,
            onUploadCurrentProfileImage = onUploadCurrentProfileImage,
            onDeleteCurrentProfileImage = onDeleteCurrentProfileImage,
            onUploadAdminProfileImage = onUploadAdminProfileImage,
            onDeleteAdminProfileImage = onDeleteAdminProfileImage,
            onCreateWatchParty = onCreateWatchParty,
            onLoadWatchParty = onLoadWatchParty,
            onLeaveWatchParty = onLeaveWatchParty,
            onUpdateWatchPartyName = onUpdateWatchPartyName,
            onUpdateWatchPartyMode = onUpdateWatchPartyMode,
            onUpdateWatchPartyRules = onUpdateWatchPartyRules,
            onStartWatchPartyFromDetail = onStartWatchPartyFromDetail,
            onLoadWatchPartyRecipients = onLoadWatchPartyRecipients,
            onToggleWatchPartyRecipient = onToggleWatchPartyRecipient,
            onSendWatchPartyInvites = onSendWatchPartyInvites,
            onClearWatchPartyInviteAnimation = onClearWatchPartyInviteAnimation,
            onToggleWatchPartyReady = onToggleWatchPartyReady,
            onVoteWatchPartyCandidate = onVoteWatchPartyCandidate,
            onStartMatchedWatchPartyPlayback = onStartMatchedWatchPartyPlayback,
            onStartFixedWatchPartyPlayback = onStartFixedWatchPartyPlayback,
            onToggleWatchPartyEnabled = onToggleWatchPartyEnabled,
            onToggleWatchPartyInvitesEnabled = onToggleWatchPartyInvitesEnabled,
            onToggleWatchPartyInviteAnimationEnabled = onToggleWatchPartyInviteAnimationEnabled,
            onSetWatchPartyInviteExpirySeconds = onSetWatchPartyInviteExpirySeconds,
            onNavigateBack = onNavigateBack,
            notificationPermissionState = notificationPermissionState,
            onRequestMusicControlsPermission = onRequestMusicControlsPermission,
            onNotificationPermissionSettingsAction = onNotificationPermissionSettingsAction,
            modifier = modifier,
        )
        return
    }
    VantafynScreenScaffold(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = VantafynSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(if (tv) VantafynSpacing.xl else VantafynSpacing.lg),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                    Text(state.server?.name ?: "Vantafyn", color = VantafynColors.Ink, style = if (tv) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium)
                    Text("Signed in as ${state.session?.user?.name.orEmpty()}", color = VantafynColors.Muted, style = MaterialTheme.typography.titleLarge)
                }
            }
            if (state.isLibrariesLoading) {
                item { VantafynLoadingIndicator("Loading libraries") }
            }
            state.errorMessage?.let { message ->
                item {
                    VantafynErrorCard(message) {
                        VantafynButton("Retry", onClick = onRetry)
                    }
                }
            }
            if (!state.isLibrariesLoading && state.libraries.isEmpty() && state.errorMessage == null) {
                item { VantafynErrorCard("No libraries were returned for this user.") }
            }
            if (state.libraries.isNotEmpty()) {
                item { LibraryRow("Libraries", state.libraries, tv) }
            }
            items(state.libraries) { library ->
                LibraryRow(library.name, listOf(library), tv)
            }
            item {
                OutlinedButton(onClick = onSwitchUser) {
                    Text("Add or switch profile")
                }
            }
        }
    }
}

@Composable
private fun MobileShellScreen(
    state: VantafynHomeUiState,
    onRetryHome: () -> Unit,
    onSwitchUser: () -> Unit,
    onAddProfile: () -> Unit,
    onQuickConnect: () -> Unit,
    onConfirmLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    onLogoutCurrentProfile: () -> Unit,
    onNavigate: (MobileDestination) -> Unit,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onRetryLibrary: () -> Unit,
    onSetLibraryFilter: (JellyfinLibraryItemFilter) -> Unit,
    onPreviousLibraryPage: () -> Unit,
    onNextLibraryPage: () -> Unit,
    onRefreshAdmin: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onRetryMedia: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLoadFavorites: () -> Unit,
    onPlaybackComingSoon: () -> Unit,
    onClearMessage: () -> Unit,
    onToggleHomeSection: (HomeSectionType) -> Unit,
    onMoveHomeSection: (HomeSectionType, Int) -> Unit,
    onResetHomeLayout: () -> Unit,
    onAddSmartRow: (String) -> Unit,
    onRemoveSmartRow: (String) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectThemeMusicVolume: (ThemeMusicVolume) -> Unit,
    onToggleAutoLoginLastProfile: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onToggleMediaFavorite: () -> Unit,
    onToggleMediaPlayed: () -> Unit,
    onSetMediaFavorite: (java.util.UUID, Boolean) -> Unit,
    onSetMediaPlayed: (java.util.UUID, Boolean) -> Unit,
    onStartPlayback: () -> Unit,
    onStartPlaybackFromBeginning: () -> Unit,
    onStartEpisodePlayback: (JellyfinEpisode, Boolean) -> Unit,
    onSelectSeason: (java.util.UUID?) -> Unit,
    onRetryPlayback: () -> Unit,
    onTryTranscodedPlayback: () -> Unit,
    onExitPlayback: (Long) -> Unit,
    onPlaybackStarted: (Long) -> Unit,
    onPlaybackProgress: (Long, Boolean) -> Unit,
    onPlaybackEnded: (Long) -> Unit,
    onPlayNextEpisode: (UpNextCandidate, Long) -> Unit,
    onPlayerError: () -> Unit,
    onPrepareCastPlayback: (Long) -> Unit,
    onSelectPlaybackAudioTrack: (Int, Long) -> Unit,
    onSelectPlaybackSubtitleTrack: (Int?, Long) -> Unit,
    onStartLiveTvPlayback: (java.util.UUID, String, String?) -> Unit,
    onEditPlaybackPreferences: ((dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> Unit,
    onSavePlaybackPreferences: () -> Unit,
    onSetAutoplayCountdownSeconds: (Int) -> Unit,
    onTogglePassoutProtection: () -> Unit,
    onSetPassoutProtectionLimitMinutes: (Int) -> Unit,
    onSelectVideoPlayerPreference: (VantafynVideoPlayerPreference) -> Unit,
    onExternalVideoPlayerLaunched: () -> Unit,
    onExternalVideoPlayerLaunchFailed: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onOpenAdminUser: (java.util.UUID) -> Unit,
    onCloseAdminUser: () -> Unit,
    onCreateAdminUser: (String, String) -> Unit,
    onUpdateAdminUser: (Boolean?, Boolean?, Boolean?, Boolean?, List<java.util.UUID>?) -> Unit,
    onResetAdminPassword: (String) -> Unit,
    onScanAdminLibrary: () -> Unit,
    onSetAdminPluginEnabled: (java.util.UUID, String?, Boolean) -> Unit,
    onRunAdminTask: (String) -> Unit,
    onStopAdminTask: (String) -> Unit,
    onUploadCurrentProfileImage: (ByteArray, String) -> Unit,
    onDeleteCurrentProfileImage: () -> Unit,
    onUploadAdminProfileImage: (ByteArray, String) -> Unit,
    onDeleteAdminProfileImage: () -> Unit,
    onCreateWatchParty: () -> Unit,
    onLoadWatchParty: () -> Unit,
    onLeaveWatchParty: () -> Unit,
    onUpdateWatchPartyName: (String) -> Unit,
    onUpdateWatchPartyMode: (WatchPartyMode) -> Unit,
    onUpdateWatchPartyRules: (dev.vantafyn.core.jellyfin.WatchPartyRules) -> Unit,
    onStartWatchPartyFromDetail: (WatchPartyMode) -> Unit,
    onLoadWatchPartyRecipients: () -> Unit,
    onToggleWatchPartyRecipient: (String) -> Unit,
    onSendWatchPartyInvites: () -> Unit,
    onClearWatchPartyInviteAnimation: () -> Unit,
    onToggleWatchPartyReady: () -> Unit,
    onVoteWatchPartyCandidate: (dev.vantafyn.core.jellyfin.WatchPartyVoteValue) -> Unit,
    onStartMatchedWatchPartyPlayback: () -> Unit,
    onStartFixedWatchPartyPlayback: () -> Unit,
    onToggleWatchPartyEnabled: () -> Unit,
    onToggleWatchPartyInvitesEnabled: () -> Unit,
    onToggleWatchPartyInviteAnimationEnabled: () -> Unit,
    onSetWatchPartyInviteExpirySeconds: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    notificationPermissionState: VantafynPermissionUiState,
    onRequestMusicControlsPermission: ((() -> Unit) -> Unit),
    onNotificationPermissionSettingsAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mediaActionTarget by remember { mutableStateOf<MediaActionTarget?>(null) }
    var showMusicQuickPlayer by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val musicController = remember(context) { MusicPlaybackController.get(context) }
    val musicPlayback by musicController.state.collectAsStateWithLifecycle()
    val handlesSystemBack = state.mobileDestination != MobileDestination.Home ||
        state.confirmLogout ||
        state.mobileMessage != null ||
        showMusicQuickPlayer
    BackHandler(enabled = handlesSystemBack) {
        when {
            showMusicQuickPlayer -> showMusicQuickPlayer = false
            state.confirmLogout -> onCancelLogout()
            state.mobileMessage != null -> onClearMessage()
            state.mobileDestination == MobileDestination.AdminUserSettings -> onCloseAdminUser()
            else -> onNavigateBack()
        }
    }
    VantafynOnboardingBackground(tv = false, modifier = modifier, backgroundResId = state.selectedBackground.drawableResId()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            when (state.mobileDestination) {
                MobileDestination.Home -> MobileHomeContent(
                    state = state,
                    onRetry = onRetryHome,
                    onSearch = { onNavigate(MobileDestination.Search) },
                    onProfile = { onNavigate(MobileDestination.Profile) },
                    onOpenLibrary = onOpenLibrary,
                    onOpenMedia = onOpenMedia,
                    onMediaLongPress = { mediaActionTarget = it },
                    onStartLiveTvPlayback = onStartLiveTvPlayback,
                    onPlaybackComingSoon = onPlaybackComingSoon,
                )
                MobileDestination.Requests -> RequestsScreen(session = state.session, onOpenMedia = onOpenMedia)
                MobileDestination.MediaDetail -> MediaDetailScreen(
                    state = state,
                    onBack = onNavigateBack,
                    onRetry = onRetryMedia,
                    onOpenMedia = onOpenMedia,
                    onPlaybackComingSoon = onPlaybackComingSoon,
                    onStartPlayback = onStartPlayback,
                    onStartPlaybackFromBeginning = onStartPlaybackFromBeginning,
                    onStartEpisodePlayback = onStartEpisodePlayback,
                    onSelectSeason = onSelectSeason,
                    themeMusicEnabled = state.themeMusicEnabled,
                    themeMusicVolume = state.themeMusicVolume,
                    onToggleFavorite = onToggleMediaFavorite,
                    onTogglePlayed = onToggleMediaPlayed,
                    onStartWatchParty = onStartWatchPartyFromDetail,
                )
                MobileDestination.Player -> Box(Modifier.fillMaxSize()) {
                    if (state.videoPlayerPreference == VantafynVideoPlayerPreference.External) {
                        ExternalVideoPlayerLauncher(
                            item = state.playbackItem,
                            isLoading = state.isPlaybackLoading,
                            errorMessage = state.playbackError,
                            onRetry = onRetryPlayback,
                            onLaunched = onExternalVideoPlayerLaunched,
                            onLaunchFailed = onExternalVideoPlayerLaunchFailed,
                        )
                    } else {
                        MobilePlayerScreen(
                            item = state.playbackItem,
                            isLoading = state.isPlaybackLoading,
                            errorMessage = state.playbackError,
                            canTryTranscode = state.canTryPlaybackTranscode,
                            onBack = onExitPlayback,
                            onRetry = onRetryPlayback,
                            onTryTranscode = onTryTranscodedPlayback,
                            onStarted = onPlaybackStarted,
                            onProgress = onPlaybackProgress,
                            onEnded = onPlaybackEnded,
                            onPlayNext = onPlayNextEpisode,
                            onPlayerError = onPlayerError,
                            onPrepareCastPlayback = onPrepareCastPlayback,
                            onSelectAudioTrack = onSelectPlaybackAudioTrack,
                            onSelectSubtitleTrack = onSelectPlaybackSubtitleTrack,
                            suppressUpNext = state.activeWatchParty != null,
                        )
                    }
                    if (state.activeWatchParty != null) {
                        WatchPartyPlayerPill(
                            label = state.watchPartySyncStateLabel,
                            memberCount = state.watchPartyRealtimeMembers.size.coerceAtLeast(state.activeWatchParty.members.size),
                            isHost = state.activeWatchParty.role.name == "Host",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(top = 12.dp, end = 12.dp),
                        )
                    }
                }
                else -> Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (state.mobileDestination == MobileDestination.Music) {
                                Modifier
                            } else {
                                Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                            },
                        ),
                ) {
                    when (state.mobileDestination) {
                        MobileDestination.Libraries -> LibrariesScreen(state, onOpenLibrary)
                        MobileDestination.Search -> SearchScreen(state, onSearchQueryChanged, onOpenMedia, onMediaLongPress = { mediaActionTarget = it })
                        MobileDestination.Music -> MusicScreen(
                            session = state.session,
                            onRequestMusicControlsPermission = onRequestMusicControlsPermission,
                        )
                        MobileDestination.Favorites -> FavoritesScreen(state, onLoadFavorites, onOpenMedia, onRemoveFromMyList = { onSetMediaFavorite(it, false) }, onMediaLongPress = { mediaActionTarget = it })
                        MobileDestination.WatchParty -> WatchPartyScreen(
                            state = state,
                            onCreate = onCreateWatchParty,
                            onRefresh = onLoadWatchParty,
                            onLeave = onLeaveWatchParty,
                            onName = onUpdateWatchPartyName,
                            onMode = onUpdateWatchPartyMode,
                            onRules = onUpdateWatchPartyRules,
                            onRefreshRecipients = onLoadWatchPartyRecipients,
                            onToggleRecipient = onToggleWatchPartyRecipient,
                            onSendInvites = onSendWatchPartyInvites,
                            onClearInviteAnimation = onClearWatchPartyInviteAnimation,
                            onToggleReady = onToggleWatchPartyReady,
                            onVote = onVoteWatchPartyCandidate,
                            onStartMatched = onStartMatchedWatchPartyPlayback,
                            onStartFixed = onStartFixedWatchPartyPlayback,
                            onToggleWatchPartyEnabled = onToggleWatchPartyEnabled,
                            onToggleWatchPartyInvitesEnabled = onToggleWatchPartyInvitesEnabled,
                            onToggleWatchPartyInviteAnimationEnabled = onToggleWatchPartyInviteAnimationEnabled,
                            onSetWatchPartyInviteExpirySeconds = onSetWatchPartyInviteExpirySeconds,
                        )
                        MobileDestination.Admin -> AdminScreen(
                            state = state,
                            onOpenUser = onOpenAdminUser,
                            onCreateUser = onCreateAdminUser,
                            onRefresh = onRefreshAdmin,
                            onScanLibrary = onScanAdminLibrary,
                            onRunTask = onRunAdminTask,
                            onStopTask = onStopAdminTask,
                        )
                        MobileDestination.AdminUserSettings -> AdminUserSettingsScreen(
                            state = state,
                            onBack = onCloseAdminUser,
                            onUpdate = onUpdateAdminUser,
                            onResetPassword = onResetAdminPassword,
                            onUploadProfileImage = onUploadAdminProfileImage,
                            onDeleteProfileImage = onDeleteAdminProfileImage,
                        )
                        MobileDestination.Profile -> ProfileSettingsScreen(
                            state = state,
                            onAdmin = { onNavigate(MobileDestination.Admin) },
                            onRequests = { onNavigate(MobileDestination.Requests) },
                            onWatchParty = { onNavigate(MobileDestination.WatchParty) },
                            onHomeLayout = { onNavigate(MobileDestination.HomeLayout) },
                            onPlaybackPreferences = { onNavigate(MobileDestination.PlaybackPreferences) },
                            onToggleThemeMusic = onToggleThemeMusic,
                            onSelectThemeMusicVolume = onSelectThemeMusicVolume,
                            onToggleAutoLoginLastProfile = onToggleAutoLoginLastProfile,
                            onSwitchUser = onSwitchUser,
                            onAddProfile = onAddProfile,
                            onQuickConnect = onQuickConnect,
                            onLogout = onConfirmLogout,
                            onSelectBackground = onSelectBackground,
                            onChangePassword = onChangePassword,
                            onUploadProfileImage = onUploadCurrentProfileImage,
                            onDeleteProfileImage = onDeleteCurrentProfileImage,
                            notificationPermissionState = notificationPermissionState,
                            onNotificationPermissionAction = onNotificationPermissionSettingsAction,
                        )
                        MobileDestination.PlaybackPreferences -> PlaybackPreferencesScreen(
                            state = state,
                            onBack = onNavigateBack,
                            onEdit = onEditPlaybackPreferences,
                            onSave = onSavePlaybackPreferences,
                            onSetAutoplayCountdownSeconds = onSetAutoplayCountdownSeconds,
                            onTogglePassoutProtection = onTogglePassoutProtection,
                            onSetPassoutProtectionLimitMinutes = onSetPassoutProtectionLimitMinutes,
                            onSelectVideoPlayerPreference = onSelectVideoPlayerPreference,
                        )
                        MobileDestination.HomeLayout -> HomeLayoutScreen(
                            state = state,
                            onBack = onNavigateBack,
                            onToggle = onToggleHomeSection,
                            onMove = onMoveHomeSection,
                            onReset = onResetHomeLayout,
                            onAddSmartRow = onAddSmartRow,
                            onRemoveSmartRow = onRemoveSmartRow,
                            onCycleArtwork = onCycleArtwork,
                            onCycleShape = onCycleShape,
                            onCycleSize = onCycleSize,
                            onCycleSpacing = onCycleSpacing,
                        )
                        MobileDestination.LibraryDetail -> LibraryDetailScreen(
                            state = state,
                            onBack = onNavigateBack,
                            onRetry = onRetryLibrary,
                            onSetFilter = onSetLibraryFilter,
                            onPreviousPage = onPreviousLibraryPage,
                            onNextPage = onNextLibraryPage,
                            onOpenMedia = onOpenMedia,
                            onMediaLongPress = { mediaActionTarget = it },
                            onStartLiveTvPlayback = onStartLiveTvPlayback,
                            onPlaybackComingSoon = onPlaybackComingSoon,
                        )
                        else -> Unit
                    }
                }
            }
            if (state.mobileDestination != MobileDestination.Player) {
                MobileBottomNav(
                    selected = state.mobileDestination.bottomNavRoot(state.previousMobileDestination),
                    onSelected = onNavigate,
                    onMusicLongPress = if (state.mobileDestination != MobileDestination.Music) {
                        { showMusicQuickPlayer = true }
                    } else {
                        null
                    },
                    isAdmin = state.session?.user?.isAdministrator == true,
                    isMusicPlaying = musicPlayback.isPlaying,
                    pendingOmbiAccessRequestCount = state.pendingOmbiAccessRequestCount,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            if (showMusicQuickPlayer) {
                MusicQuickPlayerSheet(
                    playback = musicPlayback,
                    controller = musicController,
                    onDismiss = { showMusicQuickPlayer = false },
                    onOpenMusic = {
                        showMusicQuickPlayer = false
                        onNavigate(MobileDestination.Music)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
    state.mobileMessage?.let { message ->
        val isMyListError = (
            message.contains("My List", ignoreCase = true) ||
                message.contains("Couldn't reach Jellyfin", ignoreCase = true) ||
                message.contains("Session expired", ignoreCase = true)
            ) && (
            message.contains("couldn't", ignoreCase = true) ||
                message.contains("session expired", ignoreCase = true) ||
                message.contains("not allowed", ignoreCase = true)
            )
        if (isMyListError) {
            VantafynToast(
                message = message,
                onDismiss = onClearMessage,
            )
        } else {
            AlertDialog(
                modifier = Modifier.vantafynAnimatedModalBorder(),
                onDismissRequest = onClearMessage,
                confirmButton = {
                    TextButton(onClick = onClearMessage) { Text("OK") }
                },
                title = { Text(message) },
                text = {
                    Text(
                        when {
                            message.contains("My List", ignoreCase = true) -> "Your Jellyfin favorite status has been updated for this profile."
                            else -> "The action has been completed."
                        },
                    )
                },
            )
        }
    }
    if (state.confirmLogout) {
        AlertDialog(
            modifier = Modifier.vantafynAnimatedModalBorder(),
            onDismissRequest = onCancelLogout,
            confirmButton = {
                TextButton(onClick = onLogoutCurrentProfile) { Text("Log Out") }
            },
            dismissButton = {
                TextButton(onClick = onCancelLogout) { Text("Cancel") }
            },
            title = { Text("Log out of this profile?") },
            text = { Text("This removes the saved session for ${state.session?.user?.name.orEmpty()} from this device.") },
        )
    }
    mediaActionTarget?.let { target ->
        MediaContextMenu(
            target = target,
            onDismiss = { mediaActionTarget = null },
            onViewDetails = {
                mediaActionTarget = null
                onOpenMedia(target.id)
            },
            onAddToMyList = {
                mediaActionTarget = null
                onSetMediaFavorite(target.id, true)
            },
            onRemoveFromMyList = {
                mediaActionTarget = null
                onSetMediaFavorite(target.id, false)
            },
            onMarkWatched = {
                mediaActionTarget = null
                onSetMediaPlayed(target.id, true)
            },
            onMarkUnwatched = {
                mediaActionTarget = null
                onSetMediaPlayed(target.id, false)
            },
        )
    }
}

@Composable
private fun ExternalVideoPlayerLauncher(
    item: VantafynPlaybackItem?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onLaunched: () -> Unit,
    onLaunchFailed: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(item?.streamUrl) {
        val playbackItem = item ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(playbackItem.streamUrl), "video/*")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            onLaunched()
        } catch (_: ActivityNotFoundException) {
            onLaunchFailed()
        } catch (_: SecurityException) {
            onLaunchFailed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            errorMessage != null -> VantafynErrorCard(errorMessage) {
                VantafynButton("Retry", onClick = onRetry)
            }
            isLoading || item == null -> VantafynLoadingIndicator("Preparing external player")
            else -> VantafynLoadingIndicator("Opening external player")
        }
    }
}

@Composable
private fun MobileHomeContent(
    state: VantafynHomeUiState,
    onRetry: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
    onStartLiveTvPlayback: (java.util.UUID, String, String?) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    val hero = state.home?.heroItems.orEmpty()
    val hasHomeSections = state.home?.sections.orEmpty().any { it.items.isNotEmpty() }
    val showEmptyHome = !state.isHomeLoading &&
        hero.isEmpty() &&
        state.libraries.isEmpty() &&
        state.favorites.isEmpty() &&
        !hasHomeSections &&
        state.homeErrorMessage == null
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 2.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
            var revealIndex = 0
            when {
                hero.isNotEmpty() -> {
                    val index = revealIndex++
                    item(key = "home-hero") {
                        HomeContentReveal(index = index) {
                            HeroCarousel(items = hero, onOpen = { onOpenMedia(it.id) })
                        }
                    }
                }
                state.isHomeLoading -> {
                    val index = revealIndex++
                    item(key = "home-hero-skeleton") {
                        HomeContentReveal(index = index) {
                            HomeHeroSkeleton()
                        }
                    }
                }
                showEmptyHome -> {
                    val index = revealIndex++
                    item(key = "home-empty") {
                        HomeContentReveal(index = index) {
                            HomeFallbackHero(state)
                        }
                    }
                }
            }
            state.homeLayout
                .sortedBy { it.order }
                .filter { it.visible && it.type != HomeSectionType.MediaBar }
                .forEach { preference ->
                when (preference.type) {
                    HomeSectionType.MediaBar -> Unit
                    HomeSectionType.MyMedia -> {
                        val libraries = mainLibraries(state.libraries)
                        if (libraries.isNotEmpty()) {
                            val index = revealIndex++
                            item(key = "home-my-media") {
                                HomeContentReveal(index = index) {
                                    HomeRowInset {
                                        LibraryShowcaseRow(
                                            title = "My Media",
                                            libraries = libraries,
                                            onOpenLibrary = onOpenLibrary,
                                            showTileSubtext = false,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HomeSectionType.ContinueWatching -> homeSection(state, "Continue")?.let { section ->
                        if (section.items.isNotEmpty()) {
                            val index = revealIndex++
                            item(key = "home-section-${section.title}") {
                            HomeContentReveal(index = index) {
                                HomeRowInset {
                                    HomeMediaSection(
                                        section = section,
                                        preference = preference,
                                        onOpenMedia = onOpenMedia,
                                        onMediaLongPress = onMediaLongPress,
                                        onOpenLibrary = { libraryId -> state.libraries.firstOrNull { it.id == libraryId }?.let(onOpenLibrary) },
                                        onPlaybackComingSoon = onPlaybackComingSoon,
                                    )
                                }
                            }
                        }
                        }
                    }
                    HomeSectionType.RecentlyAddedMovies -> homeSection(state, "Movies")?.let { section ->
                        if (section.items.isNotEmpty()) {
                            val index = revealIndex++
                            item(key = "home-section-${section.title}") {
                            HomeContentReveal(index = index) {
                                HomeRowInset {
                                    HomeMediaSection(
                                        section = section,
                                        preference = preference,
                                        onOpenMedia = onOpenMedia,
                                        onMediaLongPress = onMediaLongPress,
                                        onOpenLibrary = { libraryId -> state.libraries.firstOrNull { it.id == libraryId }?.let(onOpenLibrary) },
                                        onPlaybackComingSoon = onPlaybackComingSoon,
                                    )
                                }
                            }
                        }
                        }
                    }
                    HomeSectionType.RecentlyAddedTv -> homeSection(state, "TV")?.let { section ->
                        if (section.items.isNotEmpty()) {
                            val index = revealIndex++
                            item(key = "home-section-${section.title}") {
                            HomeContentReveal(index = index) {
                                HomeRowInset {
                                    HomeMediaSection(
                                        section = section,
                                        preference = preference,
                                        onOpenMedia = onOpenMedia,
                                        onMediaLongPress = onMediaLongPress,
                                        onOpenLibrary = { libraryId -> state.libraries.firstOrNull { it.id == libraryId }?.let(onOpenLibrary) },
                                        onPlaybackComingSoon = onPlaybackComingSoon,
                                    )
                                }
                            }
                        }
                        }
                    }
                    HomeSectionType.LiveTvChannels -> homeSection(state, "Live TV")?.let { section ->
                        if (section.items.isNotEmpty()) {
                            val index = revealIndex++
                            item(key = "home-section-${section.title}") {
                            HomeContentReveal(index = index) {
                                HomeRowInset {
                                    HomeMediaSection(
                                        section = section,
                                        preference = preference,
                                        onOpenMedia = { id ->
                                            val channel = state.home?.liveTvChannels.orEmpty().firstOrNull { it.id == id }
                                            val program = state.home?.liveTvPrograms.orEmpty().firstOrNull { it.id == id }
                                            when {
                                                channel != null -> onStartLiveTvPlayback(channel.id, channel.name, channel.currentProgramName)
                                                program?.channelId != null -> program.channelId?.let { channelId ->
                                                    onStartLiveTvPlayback(channelId, program.title, program.subtitle)
                                                } ?: onPlaybackComingSoon()
                                                else -> onPlaybackComingSoon()
                                            }
                                        },
                                        onMediaLongPress = onMediaLongPress,
                                        onOpenLibrary = { libraryId -> state.libraries.firstOrNull { it.id == libraryId }?.let(onOpenLibrary) },
                                        onPlaybackComingSoon = onPlaybackComingSoon,
                                    )
                                }
                            }
                        }
                        }
                    }
                    HomeSectionType.SmartRows -> {
                        val smartSections = smartRowsFor(state)
                        if (smartSections.isNotEmpty()) {
                            val index = revealIndex++
                            item(key = "home-smart-rows") {
                                HomeContentReveal(index = index) {
                                    HomeRowInset {
                                        SmartRowsSection(
                                            sections = smartSections,
                                            preference = preference,
                                            onOpenMedia = onOpenMedia,
                                            onMediaLongPress = onMediaLongPress,
                                            onPlaybackComingSoon = onPlaybackComingSoon,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HomeSectionType.OtherLibraries -> {
                        val other = otherLibraries(state.libraries)
                        if (other.isNotEmpty()) {
                            val index = revealIndex++
                            item(key = "home-more-libraries") {
                                HomeContentReveal(index = index) {
                                    HomeRowInset { LibraryShowcaseRow("More Libraries", other, onOpenLibrary) }
                                }
                            }
                        }
                    }
                }
            }
            if (state.favorites.isNotEmpty()) {
                val index = revealIndex++
                item(key = "home-my-list") {
                    HomeContentReveal(index = index) {
                        HomeRowInset {
                            MyListHomeRow(items = state.favorites.take(16), onOpenMedia = onOpenMedia, onMediaLongPress = onMediaLongPress)
                        }
                    }
                }
            }
            if (state.isHomeLoading && !hasHomeSections && state.libraries.isEmpty() && state.favorites.isEmpty()) {
                item(key = "home-skeleton-rows") { HomeRowInset { HomeLoadingShelf() } }
            }
            state.homeErrorMessage?.let { message ->
                item {
                    HomeRowInset {
                        VantafynErrorCard(message) {
                            VantafynButton("Retry", onClick = onRetry)
                        }
                    }
                }
            }
        }
        MobileHomeProfileAvatar(
            state = state,
            onProfile = onProfile,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 2.dp, end = VantafynSpacing.xl),
        )
    }
}

@Composable
private fun MyListHomeRow(
    items: List<JellyfinMediaItem>,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("My List", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(items, key = { it.id }) { item ->
                MediaItemCard(item = item, onClick = { onOpenMedia(item.id) }, onLongPress = { onMediaLongPress(item.toMediaActionTarget(inMyList = true)) })
            }
        }
    }
}

@Composable
private fun HomeRowInset(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 8.dp)) {
        content()
    }
}

@Composable
private fun MobileHomeProfileAvatar(state: VantafynHomeUiState, onProfile: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .vantafynAnimatedModalBorder(cornerRadius = 15.dp, strokeWidth = 1.5.dp)
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.76f))
            .clickable(onClick = onProfile),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = state.savedProfiles.firstOrNull { it.jellyfinUserId == state.session?.user?.id }?.imageUrl
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = state.session?.user?.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(initials(state.session?.user?.name.orEmpty()), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GlassAction(text: String, onClick: () -> Unit = {}) {
    VantafynGlassChip(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = VantafynSpacing.md, vertical = VantafynSpacing.sm),
    ) {
        Text(text, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun VantafynToast(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        delay(3_800)
        onDismiss()
    }
    Popup(alignment = Alignment.BottomCenter) {
        VantafynGlassSurface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.lg, vertical = 118.dp),
            variant = VantafynGlassVariant.Card,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(horizontal = VantafynSpacing.md, vertical = VantafynSpacing.sm),
        ) {
            Text(
                message,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HeroCarousel(
    items: List<JellyfinHeroMediaItem>,
    onOpen: (JellyfinHeroMediaItem) -> Unit,
) {
    val carouselItems = remember(items) {
        items.distinctBy { it.heroCarouselKey() }
    }
    if (carouselItems.isEmpty()) return
    val listState = rememberLazyListState()
    val carouselKeys = remember(carouselItems) { carouselItems.map { it.id } }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(carouselKeys, lifecycleOwner) {
        if (carouselItems.size > 1) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(5_500)
                    val next = (listState.firstVisibleItemIndex + 1) % carouselItems.size
                    listState.animateScrollToItem(next)
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            items(carouselItems, key = { it.id }) { item ->
                CinematicHero(
                    item = item,
                    onOpen = { onOpen(item) },
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = VantafynSpacing.xl, bottom = VantafynSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            carouselItems.take(6).forEachIndexed { index, _ ->
                val selected = index == listState.firstVisibleItemIndex.coerceAtMost(5)
                Box(
                    modifier = Modifier
                        .width(if (selected) 18.dp else 7.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = if (selected) 0.78f else 0.34f)),
                )
            }
        }
    }
}

private fun JellyfinHeroMediaItem.heroCarouselKey(): String =
    title
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()

@Composable
private fun CinematicHero(
    item: JellyfinHeroMediaItem,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(292.dp)
            .background(VantafynColors.Graphite)
            .clickable(onClick = onOpen),
    ) {
        AsyncImage(
            model = item.backdropUrl ?: item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xC0070A12),
                            0.18f to Color(0x33070A12),
                            0.56f to Color.Transparent,
                            0.84f to Color(0xA8070A12),
                            1.00f to Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xD8070A12), Color(0x66070A12), Color.Transparent),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = VantafynSpacing.xl, end = 96.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (item.logoUrl != null) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .height(76.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    item.title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                listOfNotNull(
                    item.year?.toString(),
                    item.runtimeMinutes?.let { "${it}m" },
                    item.officialRating,
                    item.communityRating?.let { "★ ${"%.1f".format(it)}" },
                ).joinToString(" · "),
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.genres.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item.genres.take(2).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(genre, color = VantafynColors.Ink.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.42f to VantafynColors.Graphite.copy(alpha = 0.28f),
                            1.00f to VantafynColors.Graphite,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun HomeFallbackHero(state: VantafynHomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF172037), Color(0xFF302A52), VantafynColors.Graphite)))
            .padding(VantafynSpacing.lg),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text("Your library is ready", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Add media to Jellyfin and Vantafyn will bring it to life here.", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun HomeLoadingShelf() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        repeat(3) { row ->
            HomeSkeletonRow(row)
        }
    }
}

@Composable
private fun HomeHeroSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp)
            .background(VantafynColors.Graphite),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF101624),
                            Color(0xFF202844),
                            Color(0xFF0B0F19),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = VantafynSpacing.xl, bottom = 42.dp)
                .width(210.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(homeSkeletonBrush()),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = VantafynSpacing.xl, bottom = 78.dp)
                .width(156.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(homeSkeletonBrush()),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(70.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, VantafynColors.Graphite),
                    ),
                ),
        )
    }
}

@Composable
private fun HomeSkeletonRow(index: Int) {
    StaggeredSearchReveal(index = index) {
        Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            Box(
                modifier = Modifier
                    .width(if (index == 0) 96.dp else 142.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(homeSkeletonBrush()),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                items(4) {
                    Box(
                        modifier = Modifier
                            .width(if (index == 0) 210.dp else 142.dp)
                            .height(if (index == 0) 118.dp else 214.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(homeSkeletonBrush()),
                    )
                }
            }
        }
    }
}

@Composable
private fun homeSkeletonBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "homeSkeleton")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "homeSkeletonShift",
    )
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.050f),
            Color.White.copy(alpha = 0.105f),
            Color.White.copy(alpha = 0.045f),
        ),
        start = Offset(-260f + shift * 520f, 0f),
        end = Offset(shift * 520f, 220f),
    )
}

@Composable
private fun HomeContentReveal(
    index: Int,
    content: @Composable () -> Unit,
) {
    content()
}

@Composable
private fun LibraryShowcaseRow(
    title: String,
    libraries: List<JellyfinLibrary>,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    showTileSubtext: Boolean = true,
) {
    if (libraries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(libraries, key = { it.id }) { library ->
                Column(
                    modifier = Modifier.width(210.dp),
                    verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
                ) {
                    ArtworkBox(
                        imageUrl = library.imageUrl,
                        title = library.name,
                        wide = true,
                        progress = null,
                        onClick = { onOpenLibrary(library) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp),
                    )
                    Text(library.name, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    if (showTileSubtext) {
                        Text(library.collectionType?.replaceFirstChar(Char::titlecase) ?: "Library", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMediaSection(
    section: JellyfinHomeSection,
    preference: HomeSectionPreference,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
    onOpenLibrary: (java.util.UUID) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    if (section.items.isEmpty()) return
    val spacing = preference.spacing.toDp()
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(section.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            items(section.items, key = { it.id }) { item ->
                MediaArtworkCard(
                    item = item,
                    preference = preference,
                    onClick = {
                        when {
                            item.shape == JellyfinMediaCardShape.Library -> onOpenLibrary(item.id)
                            else -> onOpenMedia(item.id)
                        }
                    },
                    onLongPress = {
                        if (item.shape != JellyfinMediaCardShape.Library) {
                            onMediaLongPress(item.toMediaActionTarget())
                        }
                    },
                )
            }
        }
    }
}

private fun homeSection(state: VantafynHomeUiState, token: String): JellyfinHomeSection? =
    state.home?.sections.orEmpty().firstOrNull { it.title.contains(token, ignoreCase = true) }

private fun mainLibraries(libraries: List<JellyfinLibrary>): List<JellyfinLibrary> = libraries

private fun otherLibraries(libraries: List<JellyfinLibrary>): List<JellyfinLibrary> {
    val mainIds = mainLibraries(libraries).map { it.id }.toSet()
    return libraries.filterNot { it.id in mainIds }
}

private fun previewImagesFor(state: VantafynHomeUiState, type: HomeSectionType): List<String> =
    when (type) {
        HomeSectionType.MediaBar -> state.home?.heroItems.orEmpty().mapNotNull { it.backdropUrl ?: it.posterUrl }
        HomeSectionType.MyMedia -> mainLibraries(state.libraries).mapNotNull { it.imageUrl }
        HomeSectionType.ContinueWatching -> homeSection(state, "Continue")?.items.orEmpty().mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.RecentlyAddedMovies -> homeSection(state, "Movies")?.items.orEmpty().mapNotNull { it.imageUrl ?: it.backdropUrl }
        HomeSectionType.RecentlyAddedTv -> homeSection(state, "TV")?.items.orEmpty().mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.LiveTvChannels -> homeSection(state, "Live TV")?.items.orEmpty().mapNotNull { it.imageUrl ?: it.backdropUrl }
        HomeSectionType.SmartRows -> state.home?.sections.orEmpty()
            .filter { it.title in state.configuredSmartRows }
            .flatMap { it.items }
            .mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.OtherLibraries -> otherLibraries(state.libraries).mapNotNull { it.imageUrl }
    }

private fun smartRowsFor(state: VantafynHomeUiState): List<JellyfinHomeSection> =
    state.home?.sections.orEmpty().filter {
        it.title in state.configuredSmartRows && it.items.isNotEmpty()
    }

@Composable
private fun SmartRowsSection(
    sections: List<JellyfinHomeSection>,
    preference: HomeSectionPreference,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        sections.forEachIndexed { index, section ->
            HomeContentReveal(index = index) {
                HomeMediaSection(
                    section = section,
                    preference = preference,
                    onOpenMedia = onOpenMedia,
                    onMediaLongPress = onMediaLongPress,
                    onOpenLibrary = {},
                    onPlaybackComingSoon = onPlaybackComingSoon,
                )
            }
        }
    }
}

private fun HomeSectionPreference.cardWidth(wide: Boolean) =
    when (cardSize) {
        VantafynCardSize.Small -> if (wide) 194.dp else 124.dp
        VantafynCardSize.Medium -> if (wide) 226.dp else 142.dp
        VantafynCardSize.Large -> if (wide) 268.dp else 162.dp
    }

private fun HomeSectionPreference.cardHeight(wide: Boolean) =
    when (cardSize) {
        VantafynCardSize.Small -> if (wide) 110.dp else 186.dp
        VantafynCardSize.Medium -> if (wide) 128.dp else 214.dp
        VantafynCardSize.Large -> if (wide) 150.dp else 244.dp
    }

private fun HomeSectionPreference.cardCorner() =
    when (cardShape) {
        VantafynCardShape.Rounded -> 16.dp
        VantafynCardShape.Squircle -> 22.dp
        VantafynCardShape.Soft -> 12.dp
        VantafynCardShape.Sharpish -> 6.dp
    }

private fun VantafynCardSpacing.toDp() =
    when (this) {
        VantafynCardSpacing.Compact -> 8.dp
        VantafynCardSpacing.Comfortable -> VantafynSpacing.md
        VantafynCardSpacing.Spacious -> 20.dp
    }

private fun JellyfinMediaCard.resolveArtwork(preference: HomeSectionPreference?, wide: Boolean): String? =
    when (preference?.artworkType ?: VantafynArtworkType.Auto) {
        VantafynArtworkType.PrimaryPoster -> imageUrl ?: backdropUrl ?: thumbUrl
        VantafynArtworkType.Backdrop -> backdropUrl ?: thumbUrl ?: imageUrl
        VantafynArtworkType.Thumb -> thumbUrl ?: backdropUrl ?: imageUrl
        VantafynArtworkType.Logo -> logoUrl ?: backdropUrl ?: thumbUrl ?: imageUrl
        VantafynArtworkType.Auto -> if (wide) backdropUrl ?: thumbUrl ?: imageUrl else imageUrl ?: backdropUrl ?: thumbUrl
    }

private fun JellyfinMediaItem.resolveArtwork(wide: Boolean): String? =
    if (wide) backdropUrl ?: thumbUrl ?: imageUrl else imageUrl ?: backdropUrl ?: thumbUrl

@Composable
private fun MediaArtworkCard(item: JellyfinMediaCard, preference: HomeSectionPreference? = null, onClick: () -> Unit = {}, onLongPress: () -> Unit = {}) {
    val wide = preference?.type != HomeSectionType.RecentlyAddedMovies &&
        (item.shape == JellyfinMediaCardShape.Wide || item.shape == JellyfinMediaCardShape.Library || preference?.artworkType != VantafynArtworkType.PrimaryPoster)
    val progress = item.progress
    val width = preference?.cardWidth(wide) ?: if (wide) 226.dp else 142.dp
    val height = preference?.cardHeight(wide) ?: if (wide) 128.dp else 214.dp
    val corner = preference?.cardCorner() ?: 16.dp
    val artworkUrl = item.resolveArtwork(preference, wide)
    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(corner))
                .background(Brush.linearGradient(listOf(Color(0xFF24304D), Color(0xFF393456), VantafynColors.SurfaceHigh)))
                .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        ) {
            MissingArtworkFallback(title = item.title, wide = wide)
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(VantafynColors.Primary),
                )
            }
        }
        Text(
            item.title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (wide) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(iterations = 1),
        )
        item.subtitle?.let {
            Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MediaItemCard(item: JellyfinMediaItem, onClick: () -> Unit, onLongPress: () -> Unit = {}) {
    val wide = item.shape == JellyfinMediaCardShape.Wide || item.shape == JellyfinMediaCardShape.Library
    Column(
        modifier = Modifier.width(if (wide) 226.dp else 142.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        ArtworkBox(
            imageUrl = item.resolveArtwork(wide),
            title = item.title,
            wide = wide,
            progress = item.progress,
            onClick = onClick,
            onLongPress = onLongPress,
        )
        Text(
            item.title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (wide) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        item.subtitle?.let { Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun SearchResultRow(item: JellyfinSearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.58f))
            .clickable(onClick = onClick)
            .padding(VantafynSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBox(
            imageUrl = item.imageUrl ?: item.backdropUrl,
            title = item.title,
            wide = false,
            progress = null,
            onClick = onClick,
            modifier = Modifier.width(72.dp).height(104.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, maxLines = 2)
            Text(listOfNotNull(item.year?.toString(), item.itemType).joinToString("  "), color = VantafynColors.Muted)
            item.subtitle?.let { Text(it, color = VantafynColors.Muted.copy(alpha = 0.82f), maxLines = 1) }
        }
    }
}

@Composable
private fun ArtworkBox(
    imageUrl: String?,
    title: String,
    wide: Boolean,
    progress: Float?,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(if (wide) 128.dp else 214.dp),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF24304D), Color(0xFF393456), VantafynColors.SurfaceHigh)))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        MissingArtworkFallback(title = title, wide = wide)
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (progress != null && progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(VantafynColors.Primary),
            )
        }
    }
}

@Composable
private fun MissingArtworkFallback(title: String, wide: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1C253A),
                        Color(0xFF2D3150),
                        Color(0xFF151A26),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF62D6FF).copy(alpha = 0.08f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.28f, size.height * 0.18f),
            )
            drawCircle(
                color = Color(0xFF8EA2FF).copy(alpha = 0.10f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.76f, size.height * 0.72f),
            )
        }
        Text(
            initials(title),
            color = VantafynColors.Ink.copy(alpha = 0.78f),
            style = if (wide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.065f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                .padding(horizontal = if (wide) 18.dp else 14.dp, vertical = if (wide) 12.dp else 10.dp),
        )
    }
}

@Composable
private fun LibrariesScreen(state: VantafynHomeUiState, onOpenLibrary: (JellyfinLibrary) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Text(
                "Libraries",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (state.isLibrariesLoading) item { HomeLoadingShelf() }
        state.errorMessage?.let { item { VantafynErrorCard(it) } }
        if (!state.isLibrariesLoading && state.libraries.isEmpty()) {
            item { EmptyState("No libraries returned", "Check this user's Jellyfin permissions.") }
        }
        items(state.libraries, key = { it.id }) { library ->
            LibraryListCard(library = library, onClick = { onOpenLibrary(library) })
        }
    }
}

@Composable
private fun LibraryDetailScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSetFilter: (JellyfinLibraryItemFilter) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
    onStartLiveTvPlayback: (java.util.UUID, String, String?) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    val library = state.selectedLibrary
    val liveTv = library?.collectionType.isLiveTvCollection()
    val visibleItems = state.libraryItems
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                CompactBackButton(onClick = onBack)
                ScreenTitle(library?.name ?: "Library", null)
            }
        }
        if (!liveTv) {
            item {
                LibraryFilterChips(
                    selected = state.libraryItemsFilter,
                    onSelected = onSetFilter,
                )
            }
        }
        state.libraryItemsPage?.let { page ->
            item {
                LibraryPageControls(
                    page = page,
                    loading = state.isLibraryItemsLoading,
                    onPrevious = onPreviousPage,
                    onNext = onNextPage,
                )
            }
        }
        if (state.isLibraryItemsLoading) item { HomeLoadingShelf() }
        state.libraryItemsError?.let { message ->
            item { VantafynErrorCard(message) { VantafynButton("Retry", onClick = onRetry) } }
        }
        if (liveTv) {
            item {
                LiveTvGuideSection(
                    channels = state.home?.liveTvChannels.orEmpty(),
                    programs = state.home?.liveTvPrograms.orEmpty(),
                    onChannel = { channel -> onStartLiveTvPlayback(channel.id, channel.name, channel.currentProgramName) },
                    onProgram = { program ->
                        program.channelId?.let { onStartLiveTvPlayback(it, program.title, program.subtitle) }
                            ?: onPlaybackComingSoon()
                    },
                )
            }
        }
        if (!state.isLibraryItemsLoading && state.libraryItems.isEmpty() && state.libraryItemsError == null) {
            item {
                if (liveTv) {
                    EmptyState("No Live TV channels found", "Jellyfin did not return channels for this profile/server.")
                } else {
                    EmptyState("Nothing here yet", "This library returned no browsable items.")
                }
            }
        }
        if (!state.isLibraryItemsLoading && state.libraryItems.isNotEmpty() && visibleItems.isEmpty()) {
            item { EmptyState("No matching items", "Try All or a different filter.") }
        }
        if (visibleItems.isNotEmpty()) {
            val rows = visibleItems.chunked(2)
            itemsIndexed(rows, key = { index, row -> "${row.firstOrNull()?.id}-$index" }) { _, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                ) {
                    row.forEach { item ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            MediaItemCard(
                                item = item,
                                onClick = {
                                    if (liveTv || item.itemType?.startsWith("LiveTv") == true) {
                                        onStartLiveTvPlayback(item.id, item.title, item.subtitle)
                                    } else {
                                        onOpenMedia(item.id)
                                    }
                                },
                                onLongPress = {
                                    if (!liveTv && item.itemType?.startsWith("LiveTv") != true) {
                                        onMediaLongPress(item.toMediaActionTarget())
                                    }
                                },
                            )
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        state.libraryItemsPage?.let { page ->
            if (page.totalPages > 1) {
                item {
                    LibraryPageControls(
                        page = page,
                        loading = state.isLibraryItemsLoading,
                        onPrevious = onPreviousPage,
                        onNext = onNextPage,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryFilterChips(selected: JellyfinLibraryItemFilter, onSelected: (JellyfinLibraryItemFilter) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(JellyfinLibraryItemFilter.entries, key = { it.name }) { mode ->
            LibraryFilterChip(mode.label, selected == mode) { onSelected(mode) }
        }
    }
}

@Composable
private fun LibraryPageControls(
    page: JellyfinLibraryPage,
    loading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val firstItem = if (page.totalItems == 0) 0 else page.startIndex + 1
    val lastItem = (page.startIndex + page.items.size).coerceAtMost(page.totalItems)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryPageAction("Prev", enabled = page.hasPrevious && !loading, onClick = onPrevious)
        Text(
            "Page ${page.currentPage} of ${page.totalPages}  -  $firstItem-$lastItem of ${page.totalItems.groupedCountLabel()}",
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        LibraryPageAction("Next", enabled = page.hasNext && !loading, onClick = onNext)
    }
}

@Composable
private fun LibraryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) {
                    Brush.linearGradient(listOf(Color(0xFF2C3764), Color(0xFF171E32)))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.045f)))
                },
            )
            .border(
                width = 1.dp,
                brush = if (selected) {
                    VantafynGradients.accentHorizontal()
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.07f)))
                },
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) VantafynColors.Ink else VantafynColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun LibraryPageAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = if (enabled) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.42f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private val JellyfinLibraryItemFilter.label: String
    get() = when (this) {
        JellyfinLibraryItemFilter.All -> "All"
        JellyfinLibraryItemFilter.RecentlyAdded -> "Recently Added"
        JellyfinLibraryItemFilter.AZ -> "A-Z"
        JellyfinLibraryItemFilter.Favorites -> "Favorites"
        JellyfinLibraryItemFilter.Unwatched -> "Unwatched"
    }

private fun String?.isLiveTvCollection(): Boolean =
    this?.lowercase()?.replace(" ", "") in setOf("livetv", "livetvchannels")

@Composable
private fun LiveTvGuideSection(
    channels: List<dev.vantafyn.core.jellyfin.JellyfinLiveTvChannel>,
    programs: List<dev.vantafyn.core.jellyfin.JellyfinLiveTvProgram>,
    onChannel: (dev.vantafyn.core.jellyfin.JellyfinLiveTvChannel) -> Unit,
    onProgram: (dev.vantafyn.core.jellyfin.JellyfinLiveTvProgram) -> Unit,
) {
    var showGuide by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Program Guide", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            GlassAction("Guide", onClick = { showGuide = true })
        }
        if (channels.isEmpty() && programs.isEmpty()) {
            EmptyState("Guide data unavailable", "Jellyfin did not return channel or program listings for this profile.")
        } else {
            val programsByChannel = programs.groupBy { it.channelId }
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                channels.take(12).forEach { channel ->
                    val rowPrograms = programsByChannel[channel.id].orEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.055f))
                            .clickable { onChannel(channel) }
                            .padding(VantafynSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkBox(
                            imageUrl = channel.imageUrl,
                            title = channel.name,
                            wide = false,
                            progress = null,
                            onClick = { onChannel(channel) },
                            modifier = Modifier.size(58.dp),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(listOfNotNull(channel.number, channel.name).joinToString(" · "), color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(channel.currentProgramName ?: rowPrograms.firstOrNull()?.title ?: "No current program returned", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            rowPrograms.firstOrNull()?.let {
                                Text(listOfNotNull(it.startDate?.take(16), it.endDate?.take(16)).joinToString(" - "), color = VantafynColors.Muted.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showGuide) {
        AlertDialog(
            modifier = Modifier.vantafynAnimatedModalBorder(),
            onDismissRequest = { showGuide = false },
            confirmButton = { TextButton(onClick = { showGuide = false }) { Text("Close") } },
            containerColor = VantafynColors.SurfaceHigh.copy(alpha = 0.96f),
            shape = RoundedCornerShape(28.dp),
            title = { Text("Program Guide", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (channels.isEmpty() && programs.isEmpty()) {
                        Text("Guide data unavailable", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        Text("Jellyfin did not return channel or program listings for this profile.", color = VantafynColors.Muted)
                    } else {
                        Text("${channels.size} channels · ${programs.size} guide items", color = VantafynColors.Muted)
                        val programsByChannel = programs.groupBy { it.channelId }
                        channels.take(8).forEach { channel ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.055f))
                                    .clickable {
                                        showGuide = false
                                        onChannel(channel)
                                    }
                                    .padding(12.dp),
                            ) {
                                Text(listOfNotNull(channel.number, channel.name).joinToString(" · "), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(channel.currentProgramName ?: programsByChannel[channel.id].orEmpty().firstOrNull()?.title ?: "No current program returned", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SearchScreen(
    state: VantafynHomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    val trimmedQuery = state.searchQuery.trim()
    val groupedResults = state.searchResults.groupBy { it.itemType?.ifBlank { "Other" } ?: "Other" }
    val typeFilters = groupedResults.keys.sorted()
    val visibleGroups = if (selectedType == null) groupedResults else groupedResults.filterKeys { it.matchesSearchType(selectedType) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Text("Search", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(VantafynSpacing.md))
            VantafynTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                label = "Search Jellyfin",
                placeholder = "Movie, show, episode...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
        }
        if (typeFilters.isNotEmpty()) {
            item(key = "search-filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                    item { SelectableChip("All", selectedType == null) { selectedType = null } }
                    items(typeFilters, key = { it }) { type ->
                        SelectableChip(type.searchGroupLabel(), selectedType == type) { selectedType = type }
                    }
                }
            }
        }
        if (trimmedQuery.length < 2) {
            item(key = "search-idle") {
                EmptySearchState(
                    selectedType = selectedType,
                    onSelectType = { selectedType = it },
                )
            }
        }
        if (state.isSearchLoading) {
            item(key = "search-loading") { SearchLoadingState() }
        }
        state.searchError?.let { item(key = "search-error") { VantafynErrorCard(it) } }
        if (!state.isSearchLoading && trimmedQuery.length >= 2 && state.searchResults.isEmpty() && state.searchError == null) {
            item(key = "search-no-results-$trimmedQuery") {
                NoSearchResultsState(
                    selectedType = selectedType,
                    onClearSearch = { onSearchQueryChanged("") },
                )
            }
        }
        visibleGroups.toSortedMap().entries.forEachIndexed { sectionIndex, (type, results) ->
            item(key = "search-section-$type-$trimmedQuery") {
                AnimatedSearchSection(index = sectionIndex) {
                    Text(type.searchGroupLabel(), color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                        itemsIndexed(results, key = { _, item -> item.id }) { cardIndex, result ->
                            AnimatedSearchResultCard(index = cardIndex) {
                                SearchResultCard(
                                    item = result,
                                    onClick = { onOpenMedia(result.id) },
                                    onLongPress = { onMediaLongPress(result.toMediaActionTarget()) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SearchQuickFilter(
    val label: String,
    val type: String,
    val icon: ImageVector,
)

private val searchQuickFilters = listOf(
    SearchQuickFilter("Movies", "Movie", Icons.Rounded.Movie),
    SearchQuickFilter("TV Shows", "Series", Icons.Rounded.Tv),
    SearchQuickFilter("Episodes", "Episode", Icons.Rounded.PlayArrow),
    SearchQuickFilter("People", "Person", Icons.Rounded.Person),
    SearchQuickFilter("Music", "Music", Icons.Rounded.MusicNote),
    SearchQuickFilter("Collections", "BoxSet", Icons.Rounded.CollectionsBookmark),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptySearchState(
    selectedType: String?,
    onSelectType: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = VantafynSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Search your universe",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Find movies, shows, music, people, and more.",
                    color = VantafynColors.Muted.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            SubtleSearchSparkle()
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            searchQuickFilters.forEachIndexed { index, chip ->
                StaggeredSearchReveal(index = index) {
                    SearchQuickChip(
                        label = chip.label,
                        icon = chip.icon,
                        selected = selectedType == chip.type,
                        onClick = { onSelectType(if (selectedType == chip.type) null else chip.type) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchQuickChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "searchChipGlow",
    )
    VantafynGlassChip(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF31D7FF).copy(alpha = 0.05f + glow * 0.10f),
                        Color(0xFF8B5CFF).copy(alpha = 0.04f + glow * 0.12f),
                    ),
                ),
            ),
        selected = selected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.82f),
                modifier = Modifier.size(17.dp),
            )
            Text(
                label,
                color = if (selected) VantafynColors.Ink else VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun SearchLoadingState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VantafynSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubtleSearchSparkle()
        Text("Searching", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NoSearchResultsState(
    selectedType: String?,
    onClearSearch: () -> Unit,
) {
    StaggeredSearchReveal(index = 0) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = VantafynSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SubtleSearchSparkle()
            Text("No results", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Try another title, actor, artist, or keyword.",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (selectedType != null) {
                Text("Filtered by ${selectedType.searchGroupLabel()}", color = VantafynColors.Muted.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                "Clear search",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClearSearch)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AnimatedSearchSection(
    index: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    StaggeredSearchReveal(index = index) {
        Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md), content = content)
    }
}

@Composable
private fun AnimatedSearchResultCard(
    index: Int,
    content: @Composable () -> Unit,
) {
    StaggeredSearchReveal(index = index.coerceAtMost(8), content = content)
}

@Composable
private fun StaggeredSearchReveal(
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(index) {
        if (!visible) {
            delay((index * 34L).coerceAtMost(220L))
            visible = true
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 5 },
            ),
    ) {
        content()
    }
}

@Composable
private fun SubtleSearchSparkle() {
    val transition = rememberInfiniteTransition(label = "searchSparkle")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "searchSparklePulse",
    )
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF62D6FF).copy(alpha = 0.14f * pulse),
                        Color(0xFF8EA2FF).copy(alpha = 0.08f * pulse),
                        Color.Transparent,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = VantafynColors.Secondary.copy(alpha = 0.48f + 0.24f * pulse),
            modifier = Modifier.size(18.dp),
        )
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = VantafynColors.Ink.copy(alpha = 0.70f),
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    VantafynGlassChip(
        selected = selected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = if (selected) VantafynColors.Ink else VantafynColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SearchResultCard(item: JellyfinSearchResult, onClick: () -> Unit, onLongPress: () -> Unit = {}) {
    Column(
        modifier = Modifier.width(if (item.shape == JellyfinMediaCardShape.Wide) 210.dp else 132.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        ArtworkBox(
            imageUrl = item.imageUrl ?: item.backdropUrl,
            title = item.title,
            wide = item.shape == JellyfinMediaCardShape.Wide,
            progress = null,
            onClick = onClick,
            onLongPress = onLongPress,
        )
        Text(item.title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(item.year?.toString(), item.subtitle).joinToString(" · "), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FavoritesScreen(
    state: VantafynHomeUiState,
    onLoadFavorites: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onRemoveFromMyList: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
) {
    val grouped = state.favorites.groupBy { it.itemType?.ifBlank { "Other" } ?: "Other" }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("My List", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh My List",
                    tint = VantafynColors.Ink.copy(alpha = 0.86f),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onLoadFavorites)
                        .padding(8.dp),
                )
            }
        }
        if (state.isFavoritesLoading) item { MyListLoadingSkeleton() }
        state.favoritesError?.let { item { VantafynErrorCard(it) } }
        if (!state.isFavoritesLoading && state.favorites.isEmpty() && state.favoritesError == null) {
            item { EmptyState("Your list is empty", "Add movies and shows from their detail pages.") }
        }
        grouped.toSortedMap().forEach { (type, itemsForType) ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                    Text(type.searchGroupLabel(), color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                        items(itemsForType, key = { it.id }) { item ->
                            MediaItemCard(
                                item = item,
                                onClick = { onOpenMedia(item.id) },
                                onLongPress = { onMediaLongPress(item.toMediaActionTarget(inMyList = true)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xl)) {
        MyListSkeletonRow(labelWidth = 128.dp, wide = false)
        MyListSkeletonRow(labelWidth = 92.dp, wide = true)
        MyListSkeletonRow(labelWidth = 112.dp, wide = false)
    }
}

@Composable
private fun MyListSkeletonRow(labelWidth: androidx.compose.ui.unit.Dp, wide: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Box(
            modifier = Modifier
                .width(labelWidth)
                .height(18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(homeSkeletonBrush()),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(4) { index ->
                Column(
                    modifier = Modifier.width(if (wide) 210.dp else 142.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VantafynGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (wide) 118.dp else 214.dp),
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(homeSkeletonBrush()),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.86f else 0.66f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(homeSkeletonBrush()),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.52f else 0.44f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(homeSkeletonBrush()),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(
    state: VantafynHomeUiState,
    onOpenUser: (java.util.UUID) -> Unit,
    onCreateUser: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onScanLibrary: () -> Unit,
    onRunTask: (String) -> Unit,
    onStopTask: (String) -> Unit,
) {
    val overview = state.adminOverview
    val adminUser = overview?.users?.firstOrNull { it.id == state.session?.user?.id }
    var addUserExpanded by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        onDispose { LongRunningTaskRegistry.stop("admin.refresh", "admin screen disposed") }
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            LongRunningTaskRegistry.start(
                id = "admin.refresh",
                type = LongRunningTaskType.StatisticsRefresh,
                owner = "AdminScreen",
                state = "visible",
            )
            while (true) {
                delay(5_000L)
                LongRunningTaskRegistry.tick("admin.refresh", "refreshing")
                onRefresh()
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item { ScreenTitle("Admin", overview?.serverName ?: state.server?.name ?: "Jellyfin Server") }
        if (state.isAdminLoading) item { VantafynLoadingIndicator("Loading admin dashboard") }
        state.adminError?.let { item { VantafynErrorCard(it) } }
        if (overview != null) {
            item { AdminHeroCard(state = state, overview = overview, adminImageUrl = adminUser?.imageUrl) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), modifier = Modifier.fillMaxWidth()) {
                    AdminStatCard("Playing", overview.activeSessions.size.groupedCountLabel(), Modifier.weight(1f))
                    AdminStatCard("Users", overview.users.size.groupedCountLabel(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), modifier = Modifier.fillMaxWidth()) {
                    AdminStatCard("Libraries", overview.libraryCount.groupedCountLabel(), Modifier.weight(1f))
                    AdminStatCard("Sessions", overview.connectedSessionCount.groupedCountLabel(), Modifier.weight(1f))
                }
            }
            item {
                AdminLibraryScanCard(
                    isActionRunning = state.isAdminActionRunning,
                    onScanLibrary = onScanLibrary,
                )
            }
            item {
                AdminUserManagementCard(
                    expanded = addUserExpanded,
                    isSaving = state.isAdminUserSaving,
                    errorMessage = state.adminUserError,
                    username = newUsername,
                    password = newPassword,
                    onToggle = {
                        if (!state.isAdminUserSaving) {
                            addUserExpanded = !addUserExpanded
                            newUsername = ""
                            newPassword = ""
                        }
                    },
                    onUsername = { newUsername = it },
                    onPassword = { newPassword = it },
                    onCreate = {
                        onCreateUser(newUsername, newPassword)
                    },
                )
            }
            item { AdminUsersSection(overview.users, onOpenUser) }
            item { AdminMediaStatsPanel(overview) }
            item { AdminStatisticsPanel(overview) }
            item { AdminSessionsSection(overview.activeSessions) }
            item { AdminPluginsPanel(overview.plugins) }
            item {
                AdminTasksPanel(
                    tasks = overview.tasks,
                    isActionRunning = state.isAdminActionRunning,
                    onRunTask = onRunTask,
                    onStopTask = onStopTask,
                )
            }
        }
    }
}

@Composable
private fun AdminHeroCard(
    state: VantafynHomeUiState,
    overview: dev.vantafyn.core.jellyfin.JellyfinAdminOverview,
    adminImageUrl: String?,
) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF6B7DFF).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.12f),
                        radius = size.width * 0.9f,
                    ),
                    cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
                )
            },
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                name = state.session?.user?.name ?: "Admin",
                imageUrl = adminImageUrl,
                modifier = Modifier
                    .size(58.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    overview.serverName ?: state.server?.name ?: "Jellyfin Server",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(overview.serverVersion?.let { "Jellyfin $it" }, overview.operatingSystem).joinToString(" - "),
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SoftBadge("Admin")
        }
    }
}

@Composable
private fun AdminUserManagementCard(
    expanded: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    username: String,
    password: String,
    onToggle: () -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onCreate: () -> Unit,
) {
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(22.dp),
            )
            Text(
                "User Management",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            GlassAction(text = if (expanded) "Close" else "Add User", onClick = { if (!isSaving) onToggle() })
        }
        if (expanded) {
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(VantafynSpacing.md),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                    errorMessage?.let {
                        Text(it, color = Color(0xFFFFC2C2), style = MaterialTheme.typography.bodyMedium)
                    }
                    VantafynTextField(
                        value = username,
                        onValueChange = onUsername,
                        label = "User name",
                    )
                    VantafynTextField(
                        value = password,
                        onValueChange = onPassword,
                        label = "Temporary password",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    VantafynButton(
                        if (isSaving) "Creating" else "Create User",
                        onClick = onCreate,
                        enabled = username.isNotBlank() && password.length >= 6 && !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    VantafynGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(label, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AdminMediaStatsPanel(overview: dev.vantafyn.core.jellyfin.JellyfinAdminOverview) {
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Media Stats", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        VantafynGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF35C7F6).copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.08f, size.height * 0.12f),
                            radius = size.width * 0.78f,
                        ),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    )
                },
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminMediaStatTile("Total media", overview.totalItems.countOrUnavailable(), Icons.Rounded.CollectionsBookmark, Modifier.weight(1f))
                    AdminMediaStatTile("Movies", overview.moviesCount.countOrUnavailable(), Icons.Rounded.Movie, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminMediaStatTile("Series", overview.seriesCount.countOrUnavailable(), Icons.Rounded.Tv, Modifier.weight(1f))
                    AdminMediaStatTile("Episodes", overview.episodesCount.countOrUnavailable(), Icons.Rounded.PlayArrow, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminMediaStatTile("Music", overview.musicCount.countOrUnavailable(), Icons.Rounded.MusicNote, Modifier.weight(1f))
                    AdminMediaStatTile("Libraries", overview.libraryCount.groupedCountLabel(), Icons.Rounded.Apps, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AdminMediaStatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    VantafynGlassSurface(
        modifier = modifier,
        variant = VantafynGlassVariant.Card,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFDCE4FF),
                modifier = Modifier.size(22.dp),
            )
            Text(
                value,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AdminStatisticsPanel(overview: dev.vantafyn.core.jellyfin.JellyfinAdminOverview) {
    val statistics = overview.statistics
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Statistics", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Last ${statistics?.rangeLabel ?: "30 days"}", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            }
            SoftBadge(
                text = when (statistics?.capability) {
                    dev.vantafyn.core.jellyfin.JellyfinStatisticsCapability.PlaybackReporting -> "Playback Reporting"
                    dev.vantafyn.core.jellyfin.JellyfinStatisticsCapability.CoreActivityOnly -> "Limited"
                    else -> "Unavailable"
                },
                color = if (statistics?.capability == dev.vantafyn.core.jellyfin.JellyfinStatisticsCapability.PlaybackReporting) {
                    Color(0xFFDCE4FF)
                } else {
                    VantafynColors.Muted
                },
                background = if (statistics?.capability == dev.vantafyn.core.jellyfin.JellyfinStatisticsCapability.PlaybackReporting) {
                    Color(0xFF6578FF).copy(alpha = 0.18f)
                } else {
                    Color.White.copy(alpha = 0.08f)
                },
            )
        }
        val loadedStatistics = statistics
        if (
            loadedStatistics != null &&
            loadedStatistics.capability == dev.vantafyn.core.jellyfin.JellyfinStatisticsCapability.PlaybackReporting &&
            (loadedStatistics.totalWatchTimeSeconds > 0L || loadedStatistics.totalPlayCount > 0 || loadedStatistics.users.isNotEmpty() || loadedStatistics.trend.isNotEmpty())
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                AdminStatCard("Watch time", loadedStatistics.totalWatchTimeSeconds.watchTimeLabel(), Modifier.weight(1f))
                AdminStatCard("Plays", loadedStatistics.totalPlayCount.compactCountLabel(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                AdminStatCard("Top user", loadedStatistics.mostActiveUser?.displayName ?: "None", Modifier.weight(1f))
                AdminStatCard("Top title", loadedStatistics.mostWatchedTitle?.title ?: "Collecting", Modifier.weight(1f))
            }
            if (loadedStatistics.trend.isNotEmpty()) {
                WatchTimeTrendCard(loadedStatistics.trend)
            }
            if (loadedStatistics.users.isNotEmpty()) {
                AdminUserWatchLeaderboard(loadedStatistics.users.take(6))
            }
            if (loadedStatistics.media.isNotEmpty()) {
                AdminMostWatchedMedia(loadedStatistics.media.take(5))
            }
            AdminLatestWatchActivity(loadedStatistics.users.filter { !it.lastWatchedTitle.isNullOrBlank() }.take(4))
        } else {
            AdminStatisticsUnavailable(
                message = statistics?.message
                    ?: if (statistics?.capability == dev.vantafyn.core.jellyfin.JellyfinStatisticsCapability.PlaybackReporting) {
                        "Playback Reporting is connected. Statistics will appear here after playback has been recorded."
                    } else {
                        "Install and enable the Jellyfin Playback Reporting plugin to unlock watch-time statistics."
                    },
                pluginConnected = statistics?.capability == dev.vantafyn.core.jellyfin.JellyfinStatisticsCapability.PlaybackReporting,
            )
        }
    }
}

@Composable
private fun WatchTimeTrendCard(buckets: List<dev.vantafyn.core.jellyfin.JellyfinWatchTimeBucket>) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Watch time trend", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                Text(buckets.sumOf { it.watchTimeSeconds }.watchTimeLabel(), color = VantafynColors.Muted)
            }
            val maxValue = buckets.maxOfOrNull { it.watchTimeSeconds }?.coerceAtLeast(1L) ?: 1L
            Canvas(modifier = Modifier.fillMaxWidth().height(92.dp)) {
                val spacing = 5.dp.toPx()
                val barWidth = ((size.width - spacing * (buckets.size - 1).coerceAtLeast(0)) / buckets.size.coerceAtLeast(1)).coerceAtLeast(3.dp.toPx())
                buckets.forEachIndexed { index, bucket ->
                    val fraction = (bucket.watchTimeSeconds.toFloat() / maxValue.toFloat()).coerceIn(0.04f, 1f)
                    val barHeight = size.height * fraction
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFB252FF), Color(0xFF46CFFF)),
                        ),
                        topLeft = Offset(index * (barWidth + spacing), size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminUserWatchLeaderboard(users: List<dev.vantafyn.core.jellyfin.JellyfinUserWatchStats>) {
    val maxSeconds = users.maxOfOrNull { it.totalWatchTimeSeconds }?.coerceAtLeast(1L) ?: 1L
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Top viewers", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            users.forEach { user ->
                val fraction = (user.totalWatchTimeSeconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.05f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileAvatar(user.displayName, user.avatarUrl, Modifier.size(42.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(user.displayName, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(user.totalWatchTimeSeconds.watchTimeLabel(), color = VantafynColors.Muted, maxLines = 1)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(5.dp)
                                    .background(VantafynGradients.accentHorizontal()),
                            )
                        }
                        user.lastWatchedTitle?.let {
                            Text("Last watched $it", color = VantafynColors.Muted.copy(alpha = 0.76f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMostWatchedMedia(media: List<dev.vantafyn.core.jellyfin.JellyfinMediaWatchStats>) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Most watched", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            media.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(item.type, "${item.playCount} plays").joinToString(" • "), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(item.totalWatchTimeSeconds.watchTimeLabel(), color = Color(0xFFB9C6FF), fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AdminLatestWatchActivity(users: List<dev.vantafyn.core.jellyfin.JellyfinUserWatchStats>) {
    if (users.isEmpty()) return
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recently active", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            users.forEach { user ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(user.displayName, color = VantafynColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(user.lastClient ?: user.lastWatchedAt ?: "", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(user.lastWatchedTitle.orEmpty(), color = VantafynColors.Muted.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AdminStatisticsUnavailable(
    message: String,
    pluginConnected: Boolean,
) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF46CFFF).copy(alpha = 0.28f),
                                Color(0xFF9F55FF).copy(alpha = 0.24f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (pluginConnected) "No statistics yet" else "Statistics unavailable",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    message,
                    color = VantafynColors.Muted.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                )
            }
        }
    }
}

@Composable
private fun AdminMetricBar(label: String, value: Int?, maxValue: Int) {
    val fraction = ((value ?: 0).toFloat() / maxValue.toFloat()).coerceIn(0.06f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            Text(value?.toString() ?: "Requires plugin", color = VantafynColors.Muted)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.10f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(7.dp)
                    .background(VantafynGradients.accentHorizontal()),
            )
        }
    }
}

private fun Long.watchTimeLabel(): String {
    if (this <= 0L) return "0m"
    val totalMinutes = this / 60L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val minutes = totalMinutes % 60L
    return when {
        days > 0L -> "${days}d ${hours}h"
        hours > 0L -> "${hours}h ${minutes}m"
        else -> "${minutes.coerceAtLeast(1L)}m"
    }
}

private fun Int.compactCountLabel(): String =
    when {
        this >= 1_000_000 -> "${this / 1_000_000}M"
        this >= 10_000 -> "${this / 1_000}K"
        else -> toString()
    }

private fun Int.groupedCountLabel(): String = "%,d".format(this)

private fun Int?.countOrUnavailable(): String = this?.groupedCountLabel() ?: "Not available"

@Composable
private fun AdminSessionsSection(sessions: List<dev.vantafyn.core.jellyfin.JellyfinAdminSession>) {
    GlassPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Active Sessions", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SoftBadge("${sessions.size} live")
        }
        if (sessions.isEmpty()) {
            AdminEmptySessionsCard()
        }
        sessions.take(12).forEach { session ->
            AdminSessionCard(session)
        }
    }
}

@Composable
private fun AdminLibraryScanCard(isActionRunning: Boolean, onScanLibrary: () -> Unit) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VantafynGradients.accentHorizontal()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Library Scan", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Refresh Jellyfin media folders and metadata.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AdminToolAction(
                label = if (isActionRunning) "Starting" else "Scan",
                enabled = !isActionRunning,
                onClick = onScanLibrary,
            )
        }
    }
}

@Composable
private fun AdminPluginsPanel(
    plugins: List<dev.vantafyn.core.jellyfin.JellyfinAdminPlugin>,
) {
    AdminToolPanel(title = "Plugins", empty = plugins.isEmpty(), emptyText = "No plugins returned") {
        plugins.forEach { plugin ->
            AdminToolRow(
                title = plugin.name,
                meta = listOfNotNull(plugin.version?.let { "v$it" }, plugin.description).joinToString(" - "),
                trailing = plugin.status.orEmpty().ifBlank { if (plugin.canUninstall) "Installed" else "Core" },
            )
        }
    }
}

@Composable
private fun AdminTasksPanel(
    tasks: List<dev.vantafyn.core.jellyfin.JellyfinAdminTask>,
    isActionRunning: Boolean,
    onRunTask: (String) -> Unit,
    onStopTask: (String) -> Unit,
) {
    AdminToolPanel(title = "Scheduled Tasks", empty = tasks.isEmpty(), emptyText = "No scheduled tasks returned") {
        tasks.forEach { task ->
            val isRunning = task.state.equals("Running", ignoreCase = true) || task.state.equals("RUNNING", ignoreCase = true)
            AdminScheduledTaskCard(
                task = task,
                isRunning = isRunning,
                actionsEnabled = !isActionRunning && task.id.isNotBlank(),
                onRun = { onRunTask(task.id) },
                onStop = { onStopTask(task.id) },
            )
        }
    }
}

@Composable
private fun AdminScheduledTaskCard(
    task: dev.vantafyn.core.jellyfin.JellyfinAdminTask,
    isRunning: Boolean,
    actionsEnabled: Boolean,
    onRun: () -> Unit,
    onStop: () -> Unit,
) {
    VantafynGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = VantafynGlassVariant.Card,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(task.name, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(task.category, task.lastStatus).joinToString(" - ").ifBlank { task.lastEnded.orEmpty() },
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SoftBadge(task.state.orEmpty().ifBlank { "Ready" })
            }
            if (isRunning || task.progress != null) {
                AdminTaskProgressBar(progress = task.progress, isRunning = isRunning)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdminToolAction(label = "Run", enabled = actionsEnabled && !isRunning, onClick = onRun)
                AdminToolAction(label = "Stop", enabled = actionsEnabled && isRunning, onClick = onStop)
            }
        }
    }
}

@Composable
private fun AdminTaskProgressBar(progress: Double?, isRunning: Boolean) {
    val safeProgress = progress?.toFloat()?.coerceIn(0f, 100f)
    val transition = rememberInfiniteTransition(label = "adminTaskProgress")
    val activeAlpha by transition.animateFloat(
        initialValue = 0.46f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "adminTaskProgressAlpha",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (isRunning) "Running" else "Last progress",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                safeProgress?.let { "${it.toInt()}%" } ?: "In progress",
                color = VantafynColors.Ink.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.10f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (safeProgress != null) (safeProgress / 100f).coerceIn(0.03f, 1f) else 1f)
                    .height(7.dp)
                    .graphicsLayer(alpha = if (safeProgress == null && isRunning) activeAlpha else 1f)
                    .background(VantafynGradients.accentHorizontal()),
            )
        }
    }
}

@Composable
private fun AdminToolPanel(title: String, empty: Boolean, emptyText: String, content: @Composable ColumnScope.() -> Unit) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (empty) {
                Text(emptyText, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            } else {
                content()
            }
        }
    }
}

@Composable
private fun AdminToolRow(
    title: String,
    meta: String,
    trailing: String,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta.isNotBlank()) {
                Text(meta, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing.isNotBlank()) {
            SoftBadge(trailing)
        }
        if (actionLabel != null && onAction != null) {
            AdminToolAction(label = actionLabel, enabled = actionEnabled, onClick = onAction)
        }
    }
}

@Composable
private fun AdminToolAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) VantafynColors.Ink.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AdminEmptySessionsCard() {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(VantafynGradients.accentHorizontal())
                    .padding(1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(999.dp))
                        .background(VantafynColors.Graphite.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("0", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("No active playback", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Streams will appear here automatically.", color = VantafynColors.Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AdminSessionCard(session: dev.vantafyn.core.jellyfin.JellyfinAdminSession) {
    val playMethodLabel = session.playMethod ?: if (session.isTranscoding) "Transcoding" else "Direct Play"
    val playMethodForeground = if (session.isTranscoding) Color(0xFFFFC077) else Color(0xFF91F0B6)
    val playMethodBackground = if (session.isTranscoding) Color(0xFFFF8A3D).copy(alpha = 0.18f) else Color(0xFF2FD46D).copy(alpha = 0.16f)
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            ) {
                AsyncImage(
                    model = session.nowPlayingBackdropUrl ?: session.nowPlayingImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.76f),
                                ),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(VantafynSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AsyncImage(
                        model = session.nowPlayingImageUrl ?: session.nowPlayingBackdropUrl,
                        contentDescription = session.nowPlayingTitle,
                        modifier = Modifier
                            .width(58.dp)
                            .height(82.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentScale = ContentScale.Crop,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                            SoftBadge(if (session.isPaused) "Paused" else "Playing")
                            SoftBadge(
                                text = playMethodLabel,
                                color = playMethodForeground,
                                background = playMethodBackground,
                            )
                        }
                        Text(session.nowPlayingTitle ?: "Unknown title", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(session.nowPlayingSubtitle, session.nowPlayingType).joinToString(" · "), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Column(
                modifier = Modifier.padding(VantafynSpacing.md),
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
            ) {
                AdminSessionProgress(session)
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(name = session.userName ?: "User", imageUrl = session.userImageUrl, modifier = Modifier.size(38.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(session.userName ?: "Unknown user", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(session.client, session.deviceName).joinToString(" · "), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        session.remoteEndPoint?.takeIf { it.isNotBlank() }?.let { endpoint ->
                            Text(
                                "Streaming from $endpoint",
                                color = VantafynColors.Muted.copy(alpha = 0.76f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSessionProgress(session: dev.vantafyn.core.jellyfin.JellyfinAdminSession) {
    val progress = if ((session.runtimeTicks ?: 0L) > 0L) {
        ((session.positionTicks ?: 0L).toFloat() / (session.runtimeTicks ?: 1L).toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxSize()
                        .background(VantafynGradients.accentHorizontal()),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(session.positionTicks.toTimeLabel(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            Text(session.runtimeTicks.toTimeLabel(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AdminSessionTechnicalLine(session: dev.vantafyn.core.jellyfin.JellyfinAdminSession) {
    val bitrate = session.bitrate?.takeIf { it > 0 }?.let { "${it / 1_000_000.0}".take(3).trimEnd('.') + " Mbps" }
    val details = listOfNotNull(
        session.container?.uppercase(),
        session.videoCodec?.uppercase(),
        session.audioCodec?.uppercase(),
        bitrate,
    )
    if (details.isNotEmpty()) {
        Text(details.joinToString(" · "), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdminUsersSection(users: List<dev.vantafyn.core.jellyfin.JellyfinAdminUser>, onOpenUser: (java.util.UUID) -> Unit) {
    GlassPanel {
        Text("Users", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        users.forEach { user ->
            VantafynGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUser(user.id) },
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileAvatar(name = user.name, imageUrl = user.imageUrl, modifier = Modifier.size(48.dp))
                    Text(user.name, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WatchPartyScreen(
    state: VantafynHomeUiState,
    onCreate: () -> Unit,
    onRefresh: () -> Unit,
    onLeave: () -> Unit,
    onName: (String) -> Unit,
    onMode: (WatchPartyMode) -> Unit,
    onRules: (WatchPartyRules) -> Unit,
    onRefreshRecipients: () -> Unit,
    onToggleRecipient: (String) -> Unit,
    onSendInvites: () -> Unit,
    onClearInviteAnimation: () -> Unit,
    onToggleReady: () -> Unit,
    onVote: (WatchPartyVoteValue) -> Unit,
    onStartMatched: () -> Unit,
    onStartFixed: () -> Unit,
    onToggleWatchPartyEnabled: () -> Unit,
    onToggleWatchPartyInvitesEnabled: () -> Unit,
    onToggleWatchPartyInviteAnimationEnabled: () -> Unit,
    onSetWatchPartyInviteExpirySeconds: (Int) -> Unit,
) {
    LaunchedEffect(state.showWatchPartyInviteSentAnimation) {
        if (state.showWatchPartyInviteSentAnimation) {
            kotlinx.coroutines.delay(850)
            onClearInviteAnimation()
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 138.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text("Watch Party", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("SyncPlay rooms and swipe matching.", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SoftBadge(state.watchPartyRealtimeConnectionState.name.lowercase().replaceFirstChar(Char::titlecase))
            }
        }
        item {
            VantafynGlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(VantafynGradients.accentHorizontal()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("WP", color = VantafynColors.Ink, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(state.activeWatchParty?.name ?: "Start a room", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(state.activeWatchParty?.serverName ?: state.server?.name ?: "Current Jellyfin server", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SoftBadge(state.activeWatchParty?.role?.name ?: "Solo")
                }
                VantafynTextField(
                    value = state.watchPartyName,
                    onValueChange = onName,
                    label = "Party name",
                    placeholder = "${state.session?.user?.name ?: "My"} Watch Party",
                )
                WatchPartyModeSelector(selected = state.watchPartyMode, onSelected = onMode)
                state.watchPartySelectedMedia?.let { media ->
                    WatchPartySelectedMediaCard(media)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    VantafynButton(
                        text = if (state.activeWatchParty == null) "Create Party" else "Refresh",
                        onClick = if (state.activeWatchParty == null) onCreate else onRefresh,
                        enabled = state.watchPartyEnabled && !state.isWatchPartyLoading,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.activeWatchParty != null) {
                        OutlinedButton(onClick = onLeave, modifier = Modifier.height(58.dp)) {
                            Text("Leave", color = VantafynColors.Ink)
                        }
                    }
                }
            }
        }
        item {
            WatchPartySettingsPanel(
                state = state,
                onToggleWatchPartyEnabled = onToggleWatchPartyEnabled,
                onToggleWatchPartyInvitesEnabled = onToggleWatchPartyInvitesEnabled,
                onToggleWatchPartyInviteAnimationEnabled = onToggleWatchPartyInviteAnimationEnabled,
                onSetWatchPartyInviteExpirySeconds = onSetWatchPartyInviteExpirySeconds,
                onRetryRealtime = onRefreshRecipients,
            )
        }
        item {
            WatchPartyInvitePanel(
                state = state,
                onRefreshRecipients = onRefreshRecipients,
                onToggleRecipient = onToggleRecipient,
                onSendInvites = onSendInvites,
            )
        }
        item {
            WatchPartyRealtimePanel(state = state, onToggleReady = onToggleReady)
        }
        if (state.watchPartyMode == WatchPartyMode.FixedTitle && state.watchPartySelectedMedia != null) {
            item {
                VantafynGlassPanel(cornerRadius = 28.dp) {
                    Text("Lobby", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Invite everyone, wait until they are ready, then start the selected title through Jellyfin SyncPlay.", color = VantafynColors.Muted)
                    VantafynButton(
                        text = "Start Watching",
                        onClick = onStartFixed,
                        enabled = state.activeWatchParty != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            VantafynGlassPanel(cornerRadius = 28.dp) {
                Text("Match Rules", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                WatchPartyRuleSection(
                    title = "What to match",
                    options = WatchPartyMediaScope.entries,
                    selected = state.watchPartyRules.mediaScope,
                    label = { it.watchPartyLabel() },
                    onSelected = { onRules(state.watchPartyRules.copy(mediaScope = it)) },
                )
                WatchPartyRuleSection(
                    title = "Match when",
                    options = WatchPartyMatchRule.entries,
                    selected = state.watchPartyRules.matchRule,
                    label = { it.watchPartyLabel() },
                    onSelected = { onRules(state.watchPartyRules.copy(matchRule = it)) },
                )
                WatchPartyRuleSection(
                    title = "Runtime",
                    options = dev.vantafyn.core.jellyfin.WatchPartyRuntimeLimit.entries,
                    selected = state.watchPartyRules.runtimeLimit,
                    label = { it.watchPartyLabel() },
                    onSelected = { onRules(state.watchPartyRules.copy(runtimeLimit = it)) },
                )
                PremiumToggleRow("Unwatched only", "Hide titles this profile has already watched.", state.watchPartyRules.unwatchedOnly) {
                    onRules(state.watchPartyRules.copy(unwatchedOnly = !state.watchPartyRules.unwatchedOnly))
                }
                PremiumToggleRow("Family friendly", "Limit results to titles Jellyfin rates PG or below.", state.watchPartyRules.kidFriendlyOnly) {
                    onRules(state.watchPartyRules.copy(kidFriendlyOnly = !state.watchPartyRules.kidFriendlyOnly))
                }
            }
        }
        item {
            if (state.isWatchPartyLoading) {
                VantafynLoadingIndicator("Preparing Watch Party")
            }
            state.watchPartyError?.let { VantafynErrorCard(it) }
        }
        if (state.watchPartyMode == WatchPartyMode.SwipeToMatch) {
            item {
                state.watchPartyMatch?.let { match ->
                    WatchPartyMatchCard(match.candidate, onStartMatched)
                } ?: WatchPartyDeck(
                    candidate = state.currentWatchPartyCandidate,
                    remaining = (state.watchPartyCandidates.size - state.watchPartyCurrentIndex).coerceAtLeast(0),
                    onVote = onVote,
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun WatchPartySettingsPanel(
    state: VantafynHomeUiState,
    onToggleWatchPartyEnabled: () -> Unit,
    onToggleWatchPartyInvitesEnabled: () -> Unit,
    onToggleWatchPartyInviteAnimationEnabled: () -> Unit,
    onSetWatchPartyInviteExpirySeconds: (Int) -> Unit,
    onRetryRealtime: () -> Unit,
) {
    VantafynGlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(16.dp)) {
        Text("Watch Party Settings", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Invites appear while Vantafyn is open and connected.", color = VantafynColors.Muted)
        PremiumToggleRow("Enable Watch Party", "Create and join Jellyfin SyncPlay rooms.", state.watchPartyEnabled, onClick = onToggleWatchPartyEnabled)
        PremiumToggleRow("Allow invites", "Show incoming in-app invite cards on this device.", state.watchPartyInvitesEnabled, onClick = onToggleWatchPartyInvitesEnabled)
        PremiumToggleRow("Invite animation", "Use the Vantafyn slide-in invite card.", state.watchPartyInviteAnimationEnabled, onClick = onToggleWatchPartyInviteAnimationEnabled)
        Text("Invite expiry", color = VantafynColors.Muted, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(30 to "30 sec", 60 to "1 min", 300 to "5 min").forEach { (seconds, label) ->
                VantafynGlassChip(selected = state.watchPartyInviteExpirySeconds == seconds, onClick = { onSetWatchPartyInviteExpirySeconds(seconds) }) {
                    Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
        if (state.watchPartyRealtimeConnectionState == SyncPlayConnectionState.Failed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Realtime connection unavailable",
                    color = VantafynColors.Muted,
                    modifier = Modifier.weight(1f),
                )
                VantafynButton(
                    text = "Retry",
                    onClick = onRetryRealtime,
                    modifier = Modifier.width(112.dp),
                )
            }
        }
    }
}

@Composable
private fun WatchPartyModeSelector(selected: WatchPartyMode, onSelected: (WatchPartyMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WatchPartyMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) VantafynGradients.accentHorizontal() else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelected(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(mode.watchPartyLabel(), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun WatchPartySelectedMediaCard(media: WatchPartySelectedMedia) {
    VantafynGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = media.artworkUrl ?: media.backdropUrl,
                contentDescription = media.title,
                modifier = Modifier
                    .size(width = 72.dp, height = 96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SoftBadge("Watch this")
                Text(media.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(media.subtitle ?: media.itemType ?: "Selected title", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun WatchPartyInvitePanel(
    state: VantafynHomeUiState,
    onRefreshRecipients: () -> Unit,
    onToggleRecipient: (String) -> Unit,
    onSendInvites: () -> Unit,
) {
    VantafynGlassPanel(cornerRadius = 28.dp) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Invites", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Available now", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onRefreshRecipients),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh recipients", tint = VantafynColors.Ink)
            }
        }
        Text(
            "Invites are delivered through Jellyfin active sessions. They appear while a recipient has an active connected client; push notifications need a future backend.",
            color = VantafynColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.isWatchPartyRecipientsLoading) {
            VantafynLoadingIndicator("Finding active users")
        } else if (state.watchPartyInviteRecipients.isEmpty()) {
            VantafynGlassCard(cornerRadius = 20.dp, contentPadding = PaddingValues(16.dp)) {
                Text("No active recipients", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                Text("Open Vantafyn on another device or ask someone to connect to this Jellyfin server.", color = VantafynColors.Muted)
            }
        } else {
            state.watchPartyInviteRecipients.forEach { recipient ->
                val selected = recipient.sessionId in state.selectedWatchPartyRecipientSessionIds
                VantafynGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleRecipient(recipient.sessionId) },
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(recipient.displayName, recipient.imageUrl, Modifier.size(46.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(recipient.displayName, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(recipient.client, recipient.deviceName).joinToString(" · ").ifBlank { "Active Jellyfin session" }, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (selected) SoftBadge("Selected") else SoftBadge("Online")
                    }
                }
            }
        }
        VantafynButton(
            text = when (state.watchPartyMode) {
                WatchPartyMode.FixedTitle -> "Send Watch This Invite"
                WatchPartyMode.SwipeToMatch -> "Send Swipe Match Invite"
            },
            onClick = onSendInvites,
            enabled = state.selectedWatchPartyRecipientSessionIds.isNotEmpty() && !state.isWatchPartyLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(
            visible = state.showWatchPartyInviteSentAnimation,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(220)),
            exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut(tween(650)),
        ) {
            VantafynGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .vantafynAnimatedModalBorder(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(16.dp),
            ) {
                Text("Invite sent", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("They'll see it while Vantafyn is open and connected.", color = VantafynColors.Muted)
            }
        }
    }
}

@Composable
private fun WatchPartyRealtimePanel(state: VantafynHomeUiState, onToggleReady: () -> Unit) {
    val currentUserId = state.session?.user?.id
    val currentReady = currentUserId?.let { state.localWatchPartyReadyStates[it] } ?: WatchPartyMemberReadyStatus.NotReady
    VantafynGlassPanel(cornerRadius = 28.dp) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Room Status", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(state.watchPartySyncStateLabel, color = VantafynColors.Muted)
            }
            SoftBadge(state.watchPartyRealtimeConnectionState.name.lowercase().replaceFirstChar(Char::titlecase))
        }
        if (state.watchPartyRealtimeError != null) {
            Text(state.watchPartyRealtimeError, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
        }
        VantafynGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleReady),
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Your lobby readiness", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                    Text("This is Vantafyn lobby readiness, not a Jellyfin synced-playback guarantee.", color = VantafynColors.Muted)
                }
                SoftBadge(if (currentReady == WatchPartyMemberReadyStatus.Ready) "Ready" else "Not ready")
            }
        }
        if (state.watchPartyRealtimeMembers.isEmpty()) {
            VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                Text("Waiting for status", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                Text("Jellyfin has not sent active session status yet.", color = VantafynColors.Muted)
            }
        } else {
            state.watchPartyRealtimeMembers.take(8).forEach { member ->
                VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(member.displayName, imageUrl = null, modifier = Modifier.size(42.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(member.displayName, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOf(member.deviceName, member.playback.watchPartyLabel()).filterNotNull().joinToString(" · ").ifBlank { "Status unknown" }, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SoftBadge(member.presence.watchPartyLabel())
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchPartyPlayerPill(label: String, memberCount: Int, isHost: Boolean, modifier: Modifier = Modifier) {
    VantafynGlassCard(
        modifier = modifier,
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (label.contains("active", ignoreCase = true)) Color(0xFF68E6FF) else Color.White.copy(alpha = 0.42f)),
            )
            Text("Watch Party", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            Text("·", color = VantafynColors.Muted)
            Text(memberCount.coerceAtLeast(1).toString(), color = VantafynColors.Muted, fontWeight = FontWeight.SemiBold)
            Text(label, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isHost) SoftBadge("Host")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> WatchPartyRuleSection(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                VantafynGlassChip(selected = option == selected, onClick = { onSelected(option) }) {
                    Text(label(option), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun WatchPartyDeck(
    candidate: WatchPartyCandidate?,
    remaining: Int,
    onVote: (WatchPartyVoteValue) -> Unit,
    onRefresh: () -> Unit,
) {
    VantafynGlassPanel(cornerRadius = 30.dp, contentPadding = PaddingValues(0.dp)) {
        if (candidate == null) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No more matches", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text("Try widening your filters or refresh the deck.", color = VantafynColors.Muted, textAlign = TextAlign.Center)
                VantafynButton("Refresh Deck", onRefresh)
            }
            return@VantafynGlassPanel
        }
        var dragX by remember(candidate.id) { mutableStateOf(0f) }
        var dragY by remember(candidate.id) { mutableStateOf(0f) }
        val actionAlpha = (kotlin.math.abs(dragX) / 220f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(VantafynColors.Graphite),
        ) {
            AsyncImage(
                model = candidate.backdropUrl ?: candidate.imageUrl,
                contentDescription = candidate.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.82f)),
                        ),
                    ),
            )
            Text(
                text = if (dragX >= 0f) "YES" else "NOPE",
                color = if (dragX >= 0f) Color(0xFF68E6FF) else Color(0xFFFF7EA8),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(if (dragX >= 0f) Alignment.TopStart else Alignment.TopEnd)
                    .padding(24.dp)
                    .graphicsLayer(alpha = actionAlpha),
            )
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = dragX
                        translationY = dragY
                        rotationZ = (dragX / 36f).coerceIn(-7f, 7f)
                    }
                    .pointerInput(candidate.id) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    dragX > 170f -> onVote(WatchPartyVoteValue.Yes)
                                    dragX < -170f -> onVote(WatchPartyVoteValue.No)
                                }
                                dragX = 0f
                                dragY = 0f
                            },
                            onDragCancel = {
                                dragX = 0f
                                dragY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragX += dragAmount.x
                                dragY += dragAmount.y
                            },
                        )
                    }
                    .padding(18.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    AsyncImage(
                        model = candidate.imageUrl,
                        contentDescription = candidate.title,
                        modifier = Modifier
                            .width(126.dp)
                            .height(188.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentScale = ContentScale.Crop,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(candidate.title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(candidate.watchPartyMetaLine(), color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOfNotNull(candidate.itemType?.searchGroupLabel(), candidate.officialRating, candidate.runtimeMinutes?.let { "$it min" })
                                .take(4)
                                .forEach { SoftBadge(it) }
                        }
                    }
                }
                Text(candidate.overview ?: "No overview from Jellyfin.", color = VantafynColors.Ink.copy(alpha = 0.82f), maxLines = 4, overflow = TextOverflow.Ellipsis)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("$remaining in deck", color = VantafynColors.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WatchPartyVoteButton("No", Color(0xFFFF7EA8)) { onVote(WatchPartyVoteValue.No) }
                        WatchPartyVoteButton("Yes", Color(0xFF68E6FF)) { onVote(WatchPartyVoteValue.Yes) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchPartyVoteButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WatchPartyMatchCard(candidate: WatchPartyCandidate, onStartMatched: () -> Unit) {
    VantafynGlassPanel(modifier = Modifier.vantafynAnimatedModalBorder(), cornerRadius = 30.dp, contentPadding = PaddingValues(0.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(VantafynColors.Graphite),
        ) {
            AsyncImage(
                model = candidate.backdropUrl ?: candidate.imageUrl,
                contentDescription = candidate.title,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.84f)))))
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SoftBadge("It's a match")
                Text(candidate.title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(candidate.watchPartyMetaLine(), color = VantafynColors.Muted)
                VantafynButton("Start Watch Party", onStartMatched, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ProfileSettingsScreen(
    state: VantafynHomeUiState,
    onAdmin: () -> Unit,
    onRequests: () -> Unit,
    onWatchParty: () -> Unit,
    onHomeLayout: () -> Unit,
    onPlaybackPreferences: () -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectThemeMusicVolume: (ThemeMusicVolume) -> Unit,
    onToggleAutoLoginLastProfile: () -> Unit,
    onSwitchUser: () -> Unit,
    onAddProfile: () -> Unit,
    onQuickConnect: () -> Unit,
    onLogout: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onUploadProfileImage: (ByteArray, String) -> Unit,
    onDeleteProfileImage: () -> Unit,
    notificationPermissionState: VantafynPermissionUiState,
    onNotificationPermissionAction: () -> Unit,
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    val avatarPicker = rememberProfileImagePicker(onUploadProfileImage)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Text("Settings", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            ProfileDashboardCard(
                state = state,
                onChangePhoto = { avatarPicker.open() },
                onRemovePhoto = onDeleteProfileImage,
            )
        }
        if (state.isProfileImageSaving || state.profileImageError != null) {
            item {
                ProfileImageStatusCard(
                    isSaving = state.isProfileImageSaving,
                    error = state.profileImageError,
                )
            }
        }
        item {
            GlassPanel {
                Text("Appearance", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SettingsRowIcon(Icons.Rounded.Wallpaper)
                    Text("Background", color = VantafynColors.Muted, fontWeight = FontWeight.SemiBold)
                }
                BackgroundSelector(selected = state.selectedBackground, onSelect = onSelectBackground)
                ThemeMusicSettings(
                    checked = state.themeMusicEnabled,
                    selected = state.themeMusicVolume,
                    onToggle = onToggleThemeMusic,
                    onSelect = onSelectThemeMusicVolume,
                )
            }
        }
        item {
            GlassPanel {
                Text("Profile", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                SettingsRow("Switch User", "Choose another saved Jellyfin profile.", onSwitchUser, compact = true, icon = Icons.Rounded.SwitchAccount)
                PremiumToggleRow(
                    title = "Use last profile on launch",
                    subtitle = "Skip Who's watching on future opens and restore the most recently used account.",
                    checked = state.autoLoginLastProfile,
                    onClick = onToggleAutoLoginLastProfile,
                    icon = Icons.Rounded.Person,
                )
                SettingsRow("Add Profile", "Use ${state.server?.name ?: "this server"} with another user.", onAddProfile, compact = true, icon = Icons.Rounded.PersonAdd)
                SettingsRow("Quick Connect", "Authorize Vantafyn from Jellyfin.", onQuickConnect, compact = true, icon = Icons.Rounded.Link)
                SettingsRow("Log Out", "Remove this profile from this device.", onLogout, compact = true, destructive = true, icon = Icons.Rounded.Logout)
            }
        }
        item {
            GlassPanel {
                Text("Permissions", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                SettingsRow(
                    title = "Music controls / Notifications",
                    subtitle = "${notificationPermissionState.statusLabel}. Used for lock-screen and notification playback controls.",
                    onClick = onNotificationPermissionAction,
                    compact = true,
                    icon = Icons.Rounded.Notifications,
                )
                Text(
                    "Vantafyn only uses this notification permission for media playback controls. It does not use notifications for ads or tracking.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            GlassPanel {
                Text("Vantafyn", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (state.session?.user?.isAdministrator == true) {
                    SettingsRow("Admin", "Server overview, users, active sessions, and management tools.", onAdmin, compact = true, icon = Icons.Rounded.AdminPanelSettings)
                }
                if (state.session?.user?.isAdministrator == true || state.ombiRequestsEnabledForUsers) {
                    val ombiStatus = when {
                        !state.ombiConfigured -> "Ombi not configured"
                        state.ombiRequestsEnabledForUsers -> "Ombi connected and enabled"
                        state.ombiRequestsEnabledForAdmins -> "Ombi enabled for admins only"
                        else -> "Ombi disabled"
                    }
                    SettingsRow("Integrations & Requests", "$ombiStatus. Let users request movies and TV shows through Ombi.", onRequests, compact = true, icon = Icons.Rounded.Apps)
                }
                SettingsRow("Home Sections", "Preview and tune your home rows.", onHomeLayout, compact = true, icon = Icons.Rounded.ViewAgenda)
                SettingsRow("Playback Preferences", "Audio language, subtitle mode, and track memory.", onPlaybackPreferences, compact = true, icon = Icons.Rounded.Tune)
                SettingsRow("Watch Party", "Create a SyncPlay group and match something to watch together.", onWatchParty, compact = true, icon = Icons.Rounded.Groups)
                SettingsRow("Change Password", "Update your current Jellyfin password.", { showPasswordDialog = true }, compact = true, icon = Icons.Rounded.Lock)
                SettingsRow("App version", "Vantafyn 0.1.0", {}, compact = true, icon = Icons.Rounded.Info)
            }
        }
    }
    if (showPasswordDialog) {
        PasswordChangeDialog(
            title = "Change Password",
            requiresCurrent = true,
            onDismiss = { showPasswordDialog = false },
            onSubmit = { current, new ->
                showPasswordDialog = false
                onChangePassword(current, new)
            },
        )
    }
    avatarPicker.Content()
}

@Composable
private fun ProfileDashboardCard(
    state: VantafynHomeUiState,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    GlassPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            val imageUrl = state.savedProfiles.firstOrNull { it.jellyfinUserId == state.session?.user?.id }?.imageUrl
            EditableProfileAvatar(
                name = state.session?.user?.name ?: "Vantafyn User",
                imageUrl = imageUrl,
                modifier = Modifier.size(78.dp),
                onClick = onChangePhoto,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.session?.user?.name ?: "Vantafyn User", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (state.session?.user?.isAdministrator == true) {
                        SoftBadge("Admin")
                    }
                }
                Text(state.server?.name ?: "Jellyfin Server", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.server?.url ?: "", color = VantafynColors.Muted.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            VantafynGlassChip(onClick = onChangePhoto) {
                Text("Change photo", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            }
            if (state.session?.user?.primaryImageTag?.isNotBlank() == true) {
                VantafynGlassChip(onClick = onRemovePhoto) {
                    Text("Remove photo", color = Color(0xFFFFC2C2), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            MiniStat("Libraries", state.libraries.size.groupedCountLabel(), Modifier.weight(1f), compact = true)
            MiniStat("My List", state.favorites.size.takeIf { it > 0 }?.groupedCountLabel() ?: "Load", Modifier.weight(1f), compact = true)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    VantafynGlassCard(
        modifier = modifier
            .fillMaxWidth(),
        cornerRadius = if (compact) 14.dp else 16.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = if (compact) 9.dp else 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 3.dp)) {
            Text(value, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BackgroundSelector(selected: VantafynAppBackground, onSelect: (VantafynAppBackground) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(VantafynAppBackground.entries.toList(), key = { it.name }) { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) Color(0xFF7B8DFF).copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFF93A7FF) else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(4.dp)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(option.drawableResId()),
                    contentDescription = option.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(999.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun PremiumToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(VantafynSpacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { SettingsRowIcon(it) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (checked) Color(0xFF7B8DFF).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.12f))
                    .padding(4.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.94f)),
                )
            }
        }
    }
}

@Composable
private fun ThemeMusicSettings(
    checked: Boolean,
    selected: ThemeMusicVolume,
    onToggle: () -> Unit,
    onSelect: (ThemeMusicVolume) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowIcon(Icons.Rounded.VolumeUp)
            Text(
                "Theme music",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .then(
                        if (checked) {
                            Modifier.background(VantafynGradients.accentHorizontal())
                        } else {
                            Modifier.background(Color.White.copy(alpha = 0.12f))
                        },
                    )
                    .padding(4.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.94f)),
                )
            }
        }
        ThemeMusicVolumeSelector(
            selected = selected,
            enabled = checked,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun ThemeMusicVolumeSelector(
    selected: ThemeMusicVolume,
    enabled: Boolean,
    onSelect: (ThemeMusicVolume) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Volume", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        VantafynGlassSurface(
            modifier = Modifier
                .fillMaxWidth(),
            variant = VantafynGlassVariant.Chip,
            enabled = enabled,
            cornerRadius = 999.dp,
            contentPadding = PaddingValues(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeMusicVolume.entries.forEach { option ->
                    val isSelected = option == selected
                    val shape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isSelected && enabled) {
                                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                                } else {
                                    Modifier.clip(shape)
                                },
                            )
                            .background(if (isSelected && enabled) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable(enabled = enabled) { onSelect(option) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option.label,
                            color = when {
                                !enabled -> VantafynColors.Muted.copy(alpha = 0.48f)
                                isSelected -> VantafynColors.Ink
                                else -> VantafynColors.Muted
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftBadge(
    text: String,
    color: Color = Color(0xFFC8D2FF),
    background: Color = Color(0xFF7B8DFF).copy(alpha = 0.16f),
) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun PlaybackPreferencesScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onEdit: ((dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> Unit,
    onSave: () -> Unit,
    onSetAutoplayCountdownSeconds: (Int) -> Unit,
    onTogglePassoutProtection: () -> Unit,
    onSetPassoutProtectionLimitMinutes: (Int) -> Unit,
    onSelectVideoPlayerPreference: (VantafynVideoPlayerPreference) -> Unit,
) {
    val preferences = state.editablePlaybackPreferences
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                CompactBackButton(onClick = onBack)
                Text("Playback Preferences", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (state.isPlaybackPreferencesLoading) item { VantafynLoadingIndicator("Loading preferences") }
        state.playbackPreferencesError?.let { item { VantafynErrorCard(it) } }
        if (preferences != null) {
            item {
                GlassPanel {
                    Text("Video Player", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    VantafynVideoPlayerPreference.entries.forEach { option ->
                        VideoPlayerPreferenceRow(
                            option = option,
                            selected = state.videoPlayerPreference == option,
                            onSelect = { onSelectVideoPlayerPreference(option) },
                        )
                    }
                }
            }
            item {
                GlassPanel {
                    Text("Languages", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    VantafynTextField(
                        value = preferences.audioLanguagePreference.orEmpty(),
                        onValueChange = { value -> onEdit { it.copy(audioLanguagePreference = value) } },
                        label = "Preferred audio language",
                        placeholder = "eng",
                    )
                    VantafynTextField(
                        value = preferences.subtitleLanguagePreference.orEmpty(),
                        onValueChange = { value -> onEdit { it.copy(subtitleLanguagePreference = value) } },
                        label = "Preferred subtitle language",
                        placeholder = "eng",
                    )
                }
            }
            item {
                GlassPanel {
                    Text("Subtitles", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                        items(listOf("Default", "Always", "OnlyForced", "None", "Smart")) { mode ->
                            SelectableChip(mode, preferences.subtitleMode.equals(mode, ignoreCase = true)) {
                                onEdit { it.copy(subtitleMode = mode) }
                            }
                        }
                    }
                    PremiumToggleRow("Remember subtitle selections", "Saved through Jellyfin user configuration.", preferences.rememberSubtitleSelections) {
                        onEdit { it.copy(rememberSubtitleSelections = !it.rememberSubtitleSelections) }
                    }
                }
            }
            item {
                GlassPanel {
                    Text("Audio", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    PremiumToggleRow("Play default audio track", "Let Jellyfin choose the default track first.", preferences.playDefaultAudioTrack) {
                        onEdit { it.copy(playDefaultAudioTrack = !it.playDefaultAudioTrack) }
                    }
                    PremiumToggleRow("Remember audio selections", "Saved through Jellyfin user configuration.", preferences.rememberAudioSelections) {
                        onEdit { it.copy(rememberAudioSelections = !it.rememberAudioSelections) }
                    }
                    PremiumToggleRow("Autoplay next episode", "Show Up Next near the end of an episode and continue automatically.", preferences.enableNextEpisodeAutoPlay) {
                        onEdit { it.copy(enableNextEpisodeAutoPlay = !it.enableNextEpisodeAutoPlay) }
                    }
                    Text("Up Next countdown", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                        items(listOf(5, 10, 15, 30)) { seconds ->
                            SelectableChip("${seconds}s", state.autoplayCountdownSeconds == seconds) {
                                onSetAutoplayCountdownSeconds(seconds)
                            }
                        }
                    }
                    PremiumToggleRow(
                        "Passout protection",
                        "Stop autoplay after your selected continuous watching limit.",
                        state.passoutProtectionEnabled,
                        onClick = onTogglePassoutProtection,
                    )
                    Text("Continue playing limit", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                        items(listOf(1, 2, 3, 4, 5)) { hours ->
                            val minutes = hours * 60
                            SelectableChip(hours.toHourLimitLabel(), state.passoutProtectionLimitMinutes == minutes) {
                                onSetPassoutProtectionLimitMinutes(minutes)
                            }
                        }
                    }
                }
            }
            item {
                VantafynButton(
                    if (state.isPlaybackPreferencesSaving) "Saving" else "Save Preferences",
                    onClick = onSave,
                    enabled = !state.isPlaybackPreferencesSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerPreferenceRow(
    option: VantafynVideoPlayerPreference,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(VantafynSpacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowIcon(Icons.Rounded.PlayArrow)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(option.label, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(option.summary, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(
                        width = 1.dp,
                        color = if (selected) Color(0xFF7B8DFF) else Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(999.dp))
                            .background(VantafynGradients.accentHorizontal()),
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordChangeDialog(
    title: String,
    requiresCurrent: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                if (requiresCurrent) {
                    VantafynTextField(
                        value = current,
                        onValueChange = { current = it },
                        label = "Current password",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
                VantafynTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New password",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(current, newPassword) },
                enabled = newPassword.length >= 6 && (!requiresCurrent || current.isNotBlank()),
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AdminUserSettingsScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onUpdate: (Boolean?, Boolean?, Boolean?, Boolean?, List<java.util.UUID>?) -> Unit,
    onResetPassword: (String) -> Unit,
    onUploadProfileImage: (ByteArray, String) -> Unit,
    onDeleteProfileImage: () -> Unit,
) {
    val detail = state.adminUserDetail
    var resetPasswordExpanded by remember(detail?.user?.id) { mutableStateOf(false) }
    var newPassword by remember(detail?.user?.id) { mutableStateOf("") }
    val avatarPicker = rememberProfileImagePicker(onUploadProfileImage)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                CompactBackButton(onClick = onBack)
                Text("User Settings", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        if (state.isAdminUserLoading) {
            item { VantafynLoadingIndicator("Loading user settings") }
        }
        state.adminUserError?.let { message ->
            item { VantafynErrorCard(message) }
        }

        if (detail != null) {
            item {
                AdminUserProfileCard(
                    detail = detail,
                    isSaving = state.isProfileImageSaving,
                    onChangePhoto = { avatarPicker.open() },
                    onRemovePhoto = onDeleteProfileImage,
                )
            }
            if (state.isAdminUserSaving) {
                item {
                    VantafynGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(VantafynSpacing.md),
                    ) {
                        Text("Saving changes...", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                GlassPanel {
                    Text("Access", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    AdminToggleRow(
                        title = "Account enabled",
                        checked = !detail.user.isDisabled,
                    ) {
                        onUpdate(null, !detail.user.isDisabled, null, null, null)
                    }
                    AdminToggleRow(
                        title = "Administrator",
                        checked = detail.user.isAdministrator,
                    ) {
                        onUpdate(null, null, !detail.user.isAdministrator, null, null)
                    }
                    AdminToggleRow(
                        title = "Hidden from login",
                        checked = detail.user.isHidden,
                    ) {
                        onUpdate(!detail.user.isHidden, null, null, null, null)
                    }
                }
            }
            item {
                GlassPanel {
                    Text("Library Access", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    AdminToggleRow(
                        title = "All libraries",
                        checked = detail.enableAllFolders,
                    ) {
                        onUpdate(null, null, null, !detail.enableAllFolders, null)
                    }
                    if (state.libraries.isEmpty()) {
                        Text("No libraries are available from the current session.", color = VantafynColors.Muted)
                    } else {
                        state.libraries.forEach { library ->
                            val libraryEnabled = detail.enableAllFolders || library.id in detail.enabledFolderIds
                            AdminToggleRow(
                                title = library.name,
                                checked = libraryEnabled,
                            ) {
                                if (!detail.enableAllFolders) {
                                    val updated = if (library.id in detail.enabledFolderIds) {
                                        detail.enabledFolderIds - library.id
                                    } else {
                                        detail.enabledFolderIds + library.id
                                    }
                                    onUpdate(null, null, null, false, updated)
                                }
                            }
                        }
                    }
                }
            }
            item {
                GlassPanel {
                    Text("Security", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    SettingsRow(
                        title = if (resetPasswordExpanded) "Cancel Password Reset" else "Reset Password",
                        subtitle = "Set a new Jellyfin password for ${detail.user.name}.",
                        onClick = {
                            resetPasswordExpanded = !resetPasswordExpanded
                            newPassword = ""
                        },
                        compact = true,
                        destructive = resetPasswordExpanded,
                    )
                    if (resetPasswordExpanded) {
                        VantafynTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "New password",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        VantafynButton(
                            if (state.isAdminUserSaving) "Saving" else "Save New Password",
                            onClick = {
                                onResetPassword(newPassword)
                                resetPasswordExpanded = false
                                newPassword = ""
                            },
                            enabled = newPassword.length >= 6 && !state.isAdminUserSaving,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        } else if (!state.isAdminUserLoading) {
            item {
                EmptyState(
                    title = "User unavailable",
                    subtitle = "Open a user from the admin page to manage settings.",
                )
            }
        }
    }
    avatarPicker.Content()
}

@Composable
private fun AdminUserProfileCard(
    detail: dev.vantafyn.core.jellyfin.JellyfinAdminUserDetail,
    isSaving: Boolean,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    GlassPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            EditableProfileAvatar(name = detail.user.name, imageUrl = detail.user.imageUrl, modifier = Modifier.size(78.dp), onClick = onChangePhoto)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        detail.user.name,
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (detail.user.isAdministrator) SoftBadge("Admin")
                }
            }
        }
        VantafynGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Profile picture", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                VantafynGlassChip(onClick = onChangePhoto) {
                    Text(if (isSaving) "Uploading" else "Change", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                }
                if (detail.user.imageUrl != null) {
                    VantafynGlassChip(onClick = onRemovePhoto) {
                        Text("Remove", color = Color(0xFFFFC2C2), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminToggleRow(title: String, checked: Boolean, onClick: () -> Unit) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(VantafynSpacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (checked) Color(0xFF7B8DFF).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.12f))
                    .padding(4.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.94f)),
                )
            }
        }
    }
}

private fun Long?.toTimeLabel(): String {
    val totalSeconds = (this ?: return "--:--") / 10_000_000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun HomeLayoutScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onToggle: (HomeSectionType) -> Unit,
    onMove: (HomeSectionType, Int) -> Unit,
    onReset: () -> Unit,
    onAddSmartRow: (String) -> Unit,
    onRemoveSmartRow: (String) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                CompactBackButton(onClick = onBack)
                Text("Home Sections", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        items(
            state.homeLayout.sortedBy { it.order }.filter { it.type != HomeSectionType.MediaBar },
            key = { it.type.name },
        ) { preference ->
            HomeSectionEditorCard(
                state = state,
                preference = preference,
                onToggle = onToggle,
                onMove = onMove,
                onCycleArtwork = onCycleArtwork,
                onCycleShape = onCycleShape,
                onCycleSize = onCycleSize,
                onCycleSpacing = onCycleSpacing,
            )
        }
        item {
            GlassPanel {
                Text("Add Smart Row", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Only rows backed by real Jellyfin metadata queries are available.", color = VantafynColors.Muted)
                supportedSmartRows.forEach { row ->
                    val selected = row in state.configuredSmartRows
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(row, color = VantafynColors.Ink, modifier = Modifier.weight(1f))
                        TinyAction(if (selected) "Remove" else "Add") {
                            if (selected) onRemoveSmartRow(row) else onAddSmartRow(row)
                        }
                    }
                }
            }
        }
        item {
            VantafynButton("Reset to default", onClick = onReset, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HomeSectionEditorCard(
    state: VantafynHomeUiState,
    preference: HomeSectionPreference,
    onToggle: (HomeSectionType) -> Unit,
    onMove: (HomeSectionType, Int) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
) {
    GlassPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(preference.type.label, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(if (preference.visible) "Visible" else "Hidden", color = VantafynColors.Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                TinyAction(if (preference.visible) "Hide" else "Show") { onToggle(preference.type) }
                TinyAction("↑") { onMove(preference.type, -1) }
                TinyAction("↓") { onMove(preference.type, 1) }
            }
        }
        HomeSectionPreview(state, preference.type)
        if (preference.type != HomeSectionType.MediaBar) {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs), modifier = Modifier.fillMaxWidth()) {
                TinyAction("Art ${preference.artworkType.shortLabel()}", Modifier.weight(1f)) { onCycleArtwork(preference.type) }
                TinyAction("Shape ${preference.cardShape.shortLabel()}", Modifier.weight(1f)) { onCycleShape(preference.type) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs), modifier = Modifier.fillMaxWidth()) {
                TinyAction("Size ${preference.cardSize.shortLabel()}", Modifier.weight(1f)) { onCycleSize(preference.type) }
                TinyAction("Space ${preference.spacing.shortLabel()}", Modifier.weight(1f)) { onCycleSpacing(preference.type) }
            }
        }
    }
}

@Composable
private fun HomeSectionPreview(state: VantafynHomeUiState, type: HomeSectionType) {
    val images = previewImagesFor(state, type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (images.isEmpty()) {
            Text("No preview data yet", color = VantafynColors.Muted, modifier = Modifier.padding(horizontal = 6.dp))
        } else {
            images.take(4).forEach { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VantafynColors.SurfaceHigh.copy(alpha = 0.46f)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun TinyAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        color = VantafynColors.Ink,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.075f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

private fun VantafynArtworkType.shortLabel(): String =
    when (this) {
        VantafynArtworkType.Auto -> "Auto"
        VantafynArtworkType.PrimaryPoster -> "Poster"
        VantafynArtworkType.Backdrop -> "Backdrop"
        VantafynArtworkType.Thumb -> "Thumb"
        VantafynArtworkType.Logo -> "Logo"
    }

private fun VantafynCardShape.shortLabel(): String =
    when (this) {
        VantafynCardShape.Rounded -> "Rounded"
        VantafynCardShape.Squircle -> "Squircle"
        VantafynCardShape.Soft -> "Soft"
        VantafynCardShape.Sharpish -> "Sharp"
    }

private fun VantafynCardSize.shortLabel(): String =
    when (this) {
        VantafynCardSize.Small -> "Small"
        VantafynCardSize.Medium -> "Medium"
        VantafynCardSize.Large -> "Large"
    }

private fun VantafynCardSpacing.shortLabel(): String =
    when (this) {
        VantafynCardSpacing.Compact -> "Compact"
        VantafynCardSpacing.Comfortable -> "Comfort"
        VantafynCardSpacing.Spacious -> "Spacious"
    }

@Composable
private fun MediaDetailScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onPlaybackComingSoon: () -> Unit,
    onStartPlayback: () -> Unit,
    onStartPlaybackFromBeginning: () -> Unit,
    onStartEpisodePlayback: (JellyfinEpisode, Boolean) -> Unit,
    onSelectSeason: (java.util.UUID?) -> Unit,
    themeMusicEnabled: Boolean,
    themeMusicVolume: ThemeMusicVolume,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
    onStartWatchParty: (WatchPartyMode) -> Unit,
) {
    val detail = state.mediaDetail
    var showActions by remember { mutableStateOf(false) }
    var showMediaInfo by remember { mutableStateOf(false) }
    var showWatchPartyStart by remember { mutableStateOf(false) }
    DetailThemeAudio(
        url = detail?.themeSongUrl,
        enabled = themeMusicEnabled,
        volume = themeMusicVolume,
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        if (state.isMediaDetailLoading) item {
            HomeRowInset {
                VantafynLoadingIndicator("Loading details")
            }
        }
        state.mediaDetailError?.let { item {
            HomeRowInset {
                VantafynErrorCard(it) { VantafynButton("Retry", onClick = onRetry) }
            }
        } }
        if (detail != null) {
            item {
                MediaDetailHero(
                    detail = detail,
                    onBack = onBack,
                    onMore = { showActions = true },
                    onFavorite = onToggleFavorite,
                )
            }
            item {
                HomeRowInset {
                    DetailActionPanel(
                        detail = detail,
                        onStartPlayback = onStartPlayback,
                        onStartPlaybackFromBeginning = onStartPlaybackFromBeginning,
                        onMore = { showActions = true },
                        onToggleFavorite = onToggleFavorite,
                        onTogglePlayed = onTogglePlayed,
                    )
                }
            }
            item {
                HomeRowInset {
                    DetailOverview(detail)
                }
            }
            if (detail.itemType.equals("Series", ignoreCase = true)) {
                item {
                    HomeRowInset {
                        EpisodeSection(
                            detail = detail,
                            selectedSeasonId = state.selectedSeasonId,
                            episodes = state.selectedSeasonEpisodes,
                            isLoading = state.isSeasonEpisodesLoading,
                            errorMessage = state.seasonEpisodesError,
                            onSelectSeason = onSelectSeason,
                            onEpisode = { onStartEpisodePlayback(it, false) },
                            onEpisodeFromBeginning = { onStartEpisodePlayback(it, true) },
                        )
                    }
                }
            }
            if (detail.people.isNotEmpty()) {
                item {
                    HomeRowInset {
                        PeopleSection(detail, onPerson = onPlaybackComingSoon)
                    }
                }
            }
            if (detail.related.isNotEmpty()) {
                item {
                    HomeRowInset {
                        RelatedSection(detail, onOpenMedia)
                    }
                }
            }
            if (detail.externalLinks.isNotEmpty()) {
                item {
                    HomeRowInset {
                        ExternalLinksSection(detail, onOpen = onPlaybackComingSoon)
                    }
                }
            }
        }
    }
    if (detail != null && showActions) {
        DetailActionSheet(
            detail = detail,
            isAdmin = state.session?.user?.isAdministrator == true,
            onDismiss = { showActions = false },
            onPlay = {
                showActions = false
                onStartPlayback()
            },
            onWatchFromBeginning = {
                showActions = false
                onStartPlaybackFromBeginning()
            },
            onToggleFavorite = {
                showActions = false
                onToggleFavorite()
            },
            onTogglePlayed = {
                showActions = false
                onTogglePlayed()
            },
            onMediaInfo = {
                showActions = false
                showMediaInfo = true
            },
            onStartWatchParty = {
                showActions = false
                showWatchPartyStart = true
            },
        )
    }
    if (detail != null && showWatchPartyStart) {
        StartWatchPartySheet(
            detail = detail,
            onDismiss = { showWatchPartyStart = false },
            onMode = { mode ->
                showWatchPartyStart = false
                onStartWatchParty(mode)
            },
        )
    }
    if (detail != null && showMediaInfo) {
        MediaInfoSheet(
            detail = detail,
            isAdmin = state.session?.user?.isAdministrator == true,
            onDismiss = { showMediaInfo = false },
        )
    }
}

@Composable
private fun DetailThemeAudio(url: String?, enabled: Boolean, volume: ThemeMusicVolume) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val musicState by remember(context) { MusicPlaybackController.get(context).state }.collectAsStateWithLifecycle()
    val canPlayTheme = musicState.currentTrack == null
    DisposableEffect(url, enabled, volume, lifecycleOwner, canPlayTheme) {
        if (!enabled || !canPlayTheme || url.isNullOrBlank() || volume.level <= 0f) {
            onDispose { }
        } else {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            var fadeJob: Job? = null
            var released = false
            val player = ExoPlayer.Builder(context).build().apply {
                this.volume = 0f
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(url))
                prepare()
            }

            fun fadeTo(targetVolume: Float, durationMs: Long, after: () -> Unit = {}) {
                if (released) return
                fadeJob?.cancel()
                fadeJob = scope.launch {
                    val startVolume = player.volume
                    val steps = 18
                    repeat(steps) { index ->
                        if (!isActive || released) return@launch
                        val progress = (index + 1).toFloat() / steps.toFloat()
                        player.volume = startVolume + ((targetVolume - startVolume) * progress)
                        delay(durationMs / steps)
                    }
                    if (!released) {
                        player.volume = targetVolume
                        after()
                    }
                }
            }

            fun startSoftly() {
                if (released) return
                player.playWhenReady = true
                fadeTo(volume.level, durationMs = 900)
            }

            fun stopSoftly(releaseAfterFade: Boolean) {
                if (released) return
                fadeTo(0f, durationMs = 650) {
                    if (released) return@fadeTo
                    player.playWhenReady = false
                    player.pause()
                    if (releaseAfterFade) {
                        released = true
                        player.stop()
                        player.release()
                        scope.cancel()
                    }
                }
            }

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> startSoftly()
                    Lifecycle.Event.ON_STOP -> stopSoftly(releaseAfterFade = false)
                    Lifecycle.Event.ON_DESTROY -> stopSoftly(releaseAfterFade = true)
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                startSoftly()
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                stopSoftly(releaseAfterFade = true)
            }
        }
    }
}

@Composable
private fun DetailActionPanel(
    detail: JellyfinMediaDetail,
    onStartPlayback: () -> Unit,
    onStartPlaybackFromBeginning: () -> Unit,
    onMore: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        VantafynButton(detail.primaryActionLabel(), onClick = onStartPlayback, modifier = Modifier.fillMaxWidth())
        if (detail.streamInfo.isNotEmpty()) DetailChipRow(detail.streamInfo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            DetailAction(if (detail.isFavorite) "♥" else "♡", if (detail.isFavorite) "In My List" else "Add to My List", onToggleFavorite, Modifier.weight(1f))
            DetailAction("↺", "From Start", onStartPlaybackFromBeginning, Modifier.weight(1f))
            DetailAction("⋯", "More", onMore, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailAction(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    VantafynGlassTile(
        modifier = modifier.height(72.dp),
        onClick = onClick,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(vertical = VantafynSpacing.sm),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(icon, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(label, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DetailOverview(detail: JellyfinMediaDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Overview", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            detail.overview?.takeIf { it.isNotBlank() } ?: "No overview provided by Jellyfin.",
            color = VantafynColors.Ink.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailChipRow(values: List<String>) {
    if (values.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        values.forEach { value ->
            val tone = detailChipTone(value)
            VantafynGlassPill(
                selected = tone != null,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tone?.let {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(it.copy(alpha = 0.90f), RoundedCornerShape(999.dp)),
                        )
                    }
                    Text(
                        value,
                        color = tone ?: VantafynColors.Ink.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (tone != null) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun detailChipTone(value: String): Color? {
    val normalized = value.lowercase()
    return when {
        "hdr" in normalized || "dolby vision" in normalized || "dv" == normalized -> Color(0xFFFFD36A)
        "4k" in normalized || "2160" in normalized || "uhd" in normalized -> Color(0xFF8FE7FF)
        "1080" in normalized || "720" in normalized -> Color(0xFF6FA8FF)
        "sub" in normalized || "cc" == normalized || "caption" in normalized -> Color(0xFFC892FF)
        "eng" == normalized || "english" in normalized || "audio" in normalized -> Color(0xFF7CE7C8)
        "atmos" in normalized || "dts" in normalized || "aac" in normalized || "flac" in normalized || "truehd" in normalized -> Color(0xFFFF8AD8)
        else -> null
    }
}

@Composable
private fun EpisodeSection(
    detail: JellyfinMediaDetail,
    selectedSeasonId: java.util.UUID?,
    episodes: List<JellyfinEpisode>,
    isLoading: Boolean,
    errorMessage: String?,
    onSelectSeason: (java.util.UUID?) -> Unit,
    onEpisode: (JellyfinEpisode) -> Unit,
    onEpisodeFromBeginning: (JellyfinEpisode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Episodes", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(detail.seasons.firstOrNull { it.id == selectedSeasonId }?.title ?: "Seasons", color = VantafynColors.Muted)
        }
        if (detail.seasons.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                items(detail.seasons, key = { it.id }) { season ->
                    SelectableChip(season.title, selected = season.id == selectedSeasonId) { onSelectSeason(season.id) }
                }
            }
        }
        if (isLoading) {
            VantafynLoadingIndicator("Loading episodes")
        }
        errorMessage?.let {
            VantafynErrorCard(it)
        }
        if (!isLoading && episodes.isEmpty() && errorMessage == null) {
            EmptyState("No episodes", "Jellyfin did not return episodes for this season.")
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(episodes, key = { it.id }) { episode ->
                Column(modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                    ArtworkBox(
                        imageUrl = episode.imageUrl,
                        title = episode.title,
                        wide = true,
                        progress = episode.progress,
                        onClick = { onEpisode(episode) },
                        onLongPress = { onEpisodeFromBeginning(episode) },
                        modifier = Modifier.fillMaxWidth().height(146.dp),
                    )
                    Text(episode.subtitle ?: "Episode", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(episode.title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                    episode.overview?.let { Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 3) }
                }
            }
        }
    }
}

@Composable
private fun DetailActionSheet(
    detail: JellyfinMediaDetail,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onWatchFromBeginning: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
    onMediaInfo: () -> Unit,
    onStartWatchParty: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(detail.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(detail.subtitle ?: detail.itemType ?: "Media", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailSheetAction("▶", detail.primaryActionLabel(), onPlay)
                DetailSheetAction("↺", "Watch from beginning", onWatchFromBeginning)
                DetailSheetAction(if (detail.isFavorite) "♡" else "＋", if (detail.isFavorite) "Remove from My List" else "Add to My List", onToggleFavorite)
                DetailSheetAction(if (detail.isPlayed) "↺" else "✓", if (detail.isPlayed) "Mark unwatched" else "Mark watched", onTogglePlayed)
                if (!detail.itemType.equals("Audio", ignoreCase = true) && !detail.itemType.equals("LiveTvChannel", ignoreCase = true)) {
                    DetailSheetAction("◌", "Start Watch Party", onStartWatchParty)
                }
                DetailSheetAction("ⓘ", "Media info", onMediaInfo)
                if (detail.mediaSources.size > 1) {
                    DetailSheetAction("▤", "Versions available in Media info", onMediaInfo)
                }
                if (isAdmin) {
                    DetailSheetAction("↻", "Refresh metadata requires admin tools later", onDismiss, enabled = false)
                }
            }
        },
    )
}

@Composable
private fun StartWatchPartySheet(
    detail: JellyfinMediaDetail,
    onDismiss: () -> Unit,
    onMode: (WatchPartyMode) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(30.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Start Watch Party", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(detail.title, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WatchPartyModeChoice(
                    title = "Watch this title",
                    subtitle = if (detail.itemType.equals("Series", ignoreCase = true)) {
                        "Choose an episode before playback starts. Whole-series playback is not started directly."
                    } else {
                        "Invite people to watch this together."
                    },
                    onClick = { onMode(WatchPartyMode.FixedTitle) },
                )
                WatchPartyModeChoice(
                    title = "Let the group choose",
                    subtitle = "Invite people first, then everyone swipes to pick something together.",
                    onClick = { onMode(WatchPartyMode.SwipeToMatch) },
                )
            }
        },
    )
}

@Composable
private fun WatchPartyModeChoice(title: String, subtitle: String, onClick: () -> Unit) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = VantafynColors.Muted)
    }
}

@Composable
private fun DetailSheetAction(icon: String, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    VantafynGlassTile(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onClick,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = if (enabled) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.5f))
            Text(label, color = if (enabled) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MediaInfoSheet(detail: JellyfinMediaDetail, isAdmin: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(28.dp),
        title = { Text("Media info", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val lines = detail.mediaInfo.filter { isAdmin || !it.adminOnly }
                if (lines.isEmpty()) {
                    item { Text("Jellyfin did not expose technical media info for this item.", color = VantafynColors.Muted) }
                } else {
                    items(lines, key = { "${it.label}-${it.value}" }) { line ->
                        MediaInfoLine(line.label, line.value)
                    }
                }
                detail.mediaSources.forEachIndexed { index, source ->
                    item {
                        VantafynGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(source.name ?: "Version ${index + 1}", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                                listOfNotNull(source.container, source.resolution, source.dynamicRange, source.videoCodec, source.audioCodec, source.bitrateLabel, source.sizeLabel)
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { Text(it.joinToString(" · "), color = VantafynColors.Muted) }
                                if (source.audioTracks.isNotEmpty()) Text("Audio: ${source.audioTracks.take(3).joinToString(", ")}", color = VantafynColors.Muted)
                                if (source.subtitleTracks.isNotEmpty()) Text("Subtitles: ${source.subtitleTracks.take(3).joinToString(", ")}", color = VantafynColors.Muted)
                                val sourcePath = source.path
                                if (isAdmin && !sourcePath.isNullOrBlank()) Text(sourcePath, color = VantafynColors.Muted.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun MediaInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = VantafynColors.Muted)
        Text(value, color = VantafynColors.Ink, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PeopleSection(detail: JellyfinMediaDetail, onPerson: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Cast & Crew", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(detail.people, key = { it.id }) { person ->
                Column(modifier = Modifier.width(104.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF4F657D), Color(0xFF756A8A), Color(0xFF20252D))))
                            .clickable(onClick = onPerson),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (person.imageUrl != null) {
                            AsyncImage(model = person.imageUrl, contentDescription = person.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(initials(person.name), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(person.name, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 1, textAlign = TextAlign.Center)
                    Text(person.role ?: person.type.orEmpty(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun RelatedSection(detail: JellyfinMediaDetail, onOpenMedia: (java.util.UUID) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Related", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(detail.related, key = { it.id }) { item ->
                MediaItemCard(item = item, onClick = { onOpenMedia(item.id) })
            }
        }
    }
}

@Composable
private fun ExternalLinksSection(detail: JellyfinMediaDetail, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("External Links", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
            itemsIndexed(detail.externalLinks, key = { index, link -> "${link.url}-$index" }) { _, link ->
                GlassAction(link.name, onClick = onOpen)
            }
        }
    }
}

@Composable
private fun MediaDetailHero(detail: JellyfinMediaDetail, onBack: () -> Unit, onMore: () -> Unit, onFavorite: () -> Unit) {
    val finishAtLabel = rememberFinishAtLabel(detail)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(472.dp)
            .background(VantafynColors.SurfaceHigh),
    ) {
        AsyncImage(
            model = detail.backdropUrl ?: detail.imageUrl,
            contentDescription = detail.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xD8070A12),
                            0.22f to Color(0x44070A12),
                            0.58f to Color.Transparent,
                            0.82f to VantafynColors.Graphite.copy(alpha = 0.84f),
                            1.00f to VantafynColors.Graphite.copy(alpha = 0.98f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xB8070A12), Color(0x44070A12), Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(118.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.38f to VantafynColors.Graphite.copy(alpha = 0.30f),
                            0.76f to VantafynColors.Graphite.copy(alpha = 0.82f),
                            1.00f to VantafynColors.Graphite,
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.xl, vertical = VantafynSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailFlatBackButton(onClick = onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                DetailFlatIconButton(if (detail.isFavorite) "♥" else "♡", onFavorite)
                DetailFlatIconButton("⋯", onMore)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.xl, vertical = 34.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            detail.logoUrl?.let {
                AsyncImage(model = it, contentDescription = detail.title, modifier = Modifier.fillMaxWidth(0.68f).height(92.dp), contentScale = ContentScale.Fit)
            } ?: Text(
                detail.title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DetailChipRow(
                listOfNotNull(
                    detail.year?.toString(),
                    detail.runtimeMinutes?.let { "${it}m" },
                    finishAtLabel,
                    detail.officialRating,
                    detail.communityRating?.let { "★ ${"%.1f".format(it)}" },
                    detail.subtitle,
                ),
            )
            if (detail.genres.isNotEmpty()) {
                DetailChipRow(detail.genres.take(4))
            }
        }
    }
}

@Composable
private fun rememberFinishAtLabel(detail: JellyfinMediaDetail): String? {
    if ((detail.runtimeMinutes ?: 0) <= 0) return null
    var nowMs by remember(detail.id) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(detail.id, detail.runtimeMinutes, detail.playbackPositionTicks, detail.isPlayed) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(15_000L)
        }
    }
    return remember(detail.id, detail.runtimeMinutes, detail.playbackPositionTicks, detail.isPlayed, nowMs) {
        detail.finishAtLabel(nowMs)
    }
}

@Composable
private fun DetailFlatBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = 2.55.dp.toPx()
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(12.5.dp.toPx(), 3.5.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 10.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 10.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(12.5.dp.toPx(), 16.5.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(5.8.dp.toPx(), 10.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(18.dp.toPx(), 10.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DetailFlatIconButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(19.dp)) {
            val stroke = 2.35.dp.toPx()
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 3.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 9.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 9.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 15.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(5.5.dp.toPx(), 9.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 9.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun FloatingCircleButton(label: String, onClick: () -> Unit) {
    VantafynGlassTile(
        modifier = Modifier.size(42.dp),
        onClick = onClick,
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LibraryListCard(library: JellyfinLibrary, onClick: () -> Unit) {
    VantafynGlassTile(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.035f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                        ),
                    ),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                )
            },
        onClick = onClick,
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .height(66.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.Black.copy(alpha = 0.24f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(17.dp)),
            ) {
                ArtworkBox(
                    imageUrl = library.imageUrl,
                    title = library.name,
                    wide = true,
                    progress = null,
                    onClick = onClick,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.045f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.18f),
                                ),
                            ),
                        ),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(library.name, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FilterChips(labels: List<String>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
        items(labels) { label -> GlassAction(label) }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String? = null) {
    Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    GlassPanel {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = VantafynColors.Muted)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    compact: Boolean = false,
    destructive: Boolean = false,
    icon: ImageVector? = null,
) {
    val textColor = if (destructive) Color(0xFFFFB5BE) else VantafynColors.Ink
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = if (compact) 16.dp else 22.dp,
        contentPadding = PaddingValues(if (compact) VantafynSpacing.md else VantafynSpacing.lg),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { SettingsRowIcon(it, destructive = destructive) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = textColor, style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SettingsRowIcon(icon: ImageVector, destructive: Boolean = false) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.065f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) Color(0xFFFFB5BE) else Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    VantafynGlassPanel(modifier = modifier.fillMaxWidth(), cornerRadius = 22.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
            content = content,
        )
    }
}

@Composable
private fun MobileBottomNav(
    selected: MobileDestination,
    onSelected: (MobileDestination) -> Unit,
    onMusicLongPress: (() -> Unit)?,
    isAdmin: Boolean,
    isMusicPlaying: Boolean,
    pendingOmbiAccessRequestCount: Int,
    modifier: Modifier = Modifier,
) {
    val tabs = buildList {
        add(MobileDestination.Home)
        add(MobileDestination.Libraries)
        add(MobileDestination.Search)
        add(MobileDestination.Music)
        add(MobileDestination.Favorites)
        add(MobileDestination.Requests)
        if (isAdmin) add(MobileDestination.Admin)
        if (!isAdmin) add(MobileDestination.Profile)
    }
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxWidth()
            .background(VantafynBottomScrim())
            .padding(horizontal = VantafynSpacing.md, vertical = VantafynSpacing.sm),
    ) {
        VantafynGlassDock(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { destination ->
                    val tabSelected = selected == destination || (selected == MobileDestination.HomeLayout && destination == MobileDestination.Profile)
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelected(destination) },
                                onLongClick = if (destination == MobileDestination.Music) onMusicLongPress else null,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (tabSelected) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF5B8CFF).copy(alpha = 0.10f), RoundedCornerShape(999.dp)),
                            )
                        }
                        MiniNavIcon(
                            destination = destination,
                            selected = tabSelected,
                            activePulse = destination == MobileDestination.Music && isMusicPlaying && !tabSelected,
                        )
                        if (
                            pendingOmbiAccessRequestCount > 0 &&
                            (destination == MobileDestination.Requests || destination == MobileDestination.Admin)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 10.dp)
                                    .size(7.dp)
                                    .background(Color(0xFF7DDCFF), RoundedCornerShape(999.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicQuickPlayerSheet(
    playback: VantafynMusicPlaybackState,
    controller: MusicPlaybackController,
    onDismiss: () -> Unit,
    onOpenMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playback.currentTrack
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 10.dp, vertical = 82.dp),
    ) {
        VantafynGlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .vantafynAnimatedModalBorder(),
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)),
            ) {
                track?.artworkUrl?.let { artwork ->
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .background(VantafynColors.Graphite),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        VantafynColors.Graphite.copy(alpha = 0.94f),
                                        VantafynColors.Graphite.copy(alpha = 0.78f),
                                        VantafynColors.Graphite.copy(alpha = 0.92f),
                                    ),
                                ),
                            ),
                    )
                }
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Now Playing", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniPlayerTextButton("Open", onOpenMusic)
                            GoogleCastRouteButton(modifier = Modifier.size(38.dp))
                            MiniPlayerTextButton("Close", onDismiss)
                        }
                    }
                    if (track == null) {
                        Text("Nothing is playing", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Start music from the Music tab and it will appear here from anywhere in Vantafyn.", color = VantafynColors.Muted)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentScale = ContentScale.Crop,
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(track.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(listOfNotNull(track.artist, track.album).joinToString(" - "), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        MiniMusicProgress(playback)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiniPlayerIconControl(Icons.Rounded.SkipPrevious, "Previous", onClick = controller::previous)
                            MiniPlayerIconControl(
                                if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                if (playback.isPlaying) "Pause" else "Play",
                                emphasized = true,
                                onClick = controller::togglePlayPause,
                            )
                            MiniPlayerIconControl(Icons.Rounded.SkipNext, "Next", onClick = controller::next)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMusicProgress(playback: VantafynMusicPlaybackState) {
    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxSize()
                    .background(VantafynGradients.accentHorizontal()),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(playback.positionMs.toMusicTimeLabel(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            Text(playback.durationMs.toMusicTimeLabel(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MiniPlayerIconControl(icon: ImageVector, contentDescription: String, emphasized: Boolean = false, onClick: () -> Unit) {
    val size = if (emphasized) 62.dp else 50.dp
    Box(
        modifier = Modifier
            .padding(horizontal = if (emphasized) 18.dp else 4.dp)
            .size(size)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (emphasized) {
                    VantafynGradients.accentHorizontal()
                } else {
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.06f)))
                },
            )
            .border(1.dp, Color.White.copy(alpha = if (emphasized) 0.28f else 0.12f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = VantafynColors.Ink,
            modifier = Modifier.size(if (emphasized) 34.dp else 27.dp),
        )
    }
}

@Composable
private fun MiniPlayerTextButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = VantafynColors.Ink,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

private fun Long.toMusicTimeLabel(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun String.looksLocalServerAddress(): Boolean {
    val host = runCatching { java.net.URI(if (contains("://")) this else "http://$this").host.orEmpty() }
        .getOrDefault("")
        .lowercase()
    if (host.endsWith(".local")) return true
    val parts = host.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.size != 4) return false
    return parts[0] == 10 ||
        (parts[0] == 192 && parts[1] == 168) ||
        (parts[0] == 172 && parts[1] in 16..31)
}

@Composable
private fun SelectedNavWaterFill() {
    val transition = rememberInfiniteTransition(label = "navWater")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "navWaterOffset",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.height / 2f
        val travel = size.width * offset
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1018FF).copy(alpha = 0.96f),
                    Color(0xFF6D00FF).copy(alpha = 0.94f),
                    Color(0xFFFF007A).copy(alpha = 0.92f),
                    Color(0xFF00E7FF).copy(alpha = 0.96f),
                    Color(0xFF00FF9C).copy(alpha = 0.90f),
                    Color(0xFF1018FF).copy(alpha = 0.96f),
                ),
                start = androidx.compose.ui.geometry.Offset(-travel, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width - travel, size.height),
                tileMode = TileMode.Repeated,
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.22f),
                ),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = Color(0xFF00E7FF).copy(alpha = 0.36f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun MiniNavIcon(destination: MobileDestination, selected: Boolean, activePulse: Boolean = false) {
    val color = if (selected) Color(0xFF8FE7FF) else Color.White.copy(alpha = 0.78f)
    val pulseAlpha = if (activePulse) {
        val transition = rememberInfiniteTransition(label = "musicNavPulse")
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "musicNavPulseAlpha",
        ).value
    } else {
        0f
    }
    Canvas(modifier = Modifier.size(23.dp)) {
        val stroke = 2.35.dp.toPx()
        val outline = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val selectedBrush = VantafynNavSelectedBrush()
        val pulsingBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF34D7FF).copy(alpha = pulseAlpha),
                Color(0xFF7C5CFF).copy(alpha = pulseAlpha),
                Color(0xFFC44DFF).copy(alpha = pulseAlpha),
            ),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
        )
        fun drawIconPath(path: Path) {
            when {
                selected -> drawPath(path = path, brush = selectedBrush, style = outline)
                activePulse -> {
                    drawPath(path, color.copy(alpha = 0.34f), style = outline)
                    drawPath(path = path, brush = pulsingBrush, style = outline)
                }
                else -> drawPath(path, color, style = outline)
            }
        }
        fun drawIconLine(start: androidx.compose.ui.geometry.Offset, end: androidx.compose.ui.geometry.Offset) {
            when {
                selected -> drawLine(brush = selectedBrush, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
                activePulse -> {
                    drawLine(color.copy(alpha = 0.34f), start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(brush = pulsingBrush, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
                }
                else -> drawLine(color, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
        fun drawIconCircle(radius: Float, center: androidx.compose.ui.geometry.Offset) {
            when {
                selected -> drawCircle(brush = selectedBrush, radius = radius, center = center, style = outline)
                activePulse -> {
                    drawCircle(color.copy(alpha = 0.34f), radius = radius, center = center, style = outline)
                    drawCircle(brush = pulsingBrush, radius = radius, center = center, style = outline)
                }
                else -> drawCircle(color, radius = radius, center = center, style = outline)
            }
        }
        fun drawIconRoundRect(
            topLeft: androidx.compose.ui.geometry.Offset,
            size: androidx.compose.ui.geometry.Size,
            cornerRadius: androidx.compose.ui.geometry.CornerRadius,
        ) {
            when {
                selected -> drawRoundRect(brush = selectedBrush, topLeft = topLeft, size = size, cornerRadius = cornerRadius, style = outline)
                activePulse -> {
                    drawRoundRect(color.copy(alpha = 0.34f), topLeft = topLeft, size = size, cornerRadius = cornerRadius, style = outline)
                    drawRoundRect(brush = pulsingBrush, topLeft = topLeft, size = size, cornerRadius = cornerRadius, style = outline)
                }
                else -> drawRoundRect(color, topLeft = topLeft, size = size, cornerRadius = cornerRadius, style = outline)
            }
        }
        when (destination) {
            MobileDestination.Home -> {
                val path = Path().apply {
                    moveTo(3.5.dp.toPx(), 11.dp.toPx())
                    lineTo(11.5.dp.toPx(), 4.dp.toPx())
                    lineTo(19.5.dp.toPx(), 11.dp.toPx())
                    lineTo(19.5.dp.toPx(), 20.dp.toPx())
                    lineTo(14.3.dp.toPx(), 20.dp.toPx())
                    lineTo(14.3.dp.toPx(), 14.3.dp.toPx())
                    lineTo(8.7.dp.toPx(), 14.3.dp.toPx())
                    lineTo(8.7.dp.toPx(), 20.dp.toPx())
                    lineTo(3.5.dp.toPx(), 20.dp.toPx())
                    close()
                }
                drawIconPath(path)
            }
            MobileDestination.Libraries -> {
                listOf(
                    androidx.compose.ui.geometry.Offset(3.dp.toPx(), 3.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(13.dp.toPx(), 3.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(3.dp.toPx(), 13.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(13.dp.toPx(), 13.dp.toPx()),
                ).forEach { offset ->
                    drawIconRoundRect(
                        topLeft = offset,
                        size = androidx.compose.ui.geometry.Size(7.dp.toPx(), 7.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    )
                }
            }
            MobileDestination.Search -> {
                drawIconCircle(radius = 6.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(9.dp.toPx(), 9.dp.toPx()))
                drawIconLine(start = androidx.compose.ui.geometry.Offset(14.dp.toPx(), 14.dp.toPx()), end = androidx.compose.ui.geometry.Offset(20.dp.toPx(), 20.dp.toPx()))
            }
            MobileDestination.Music -> {
                drawIconLine(start = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 5.dp.toPx()), end = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 16.dp.toPx()))
                drawIconLine(start = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 5.dp.toPx()), end = androidx.compose.ui.geometry.Offset(17.dp.toPx(), 4.dp.toPx()))
                drawIconLine(start = androidx.compose.ui.geometry.Offset(17.dp.toPx(), 4.dp.toPx()), end = androidx.compose.ui.geometry.Offset(17.dp.toPx(), 14.dp.toPx()))
                drawIconCircle(radius = 3.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(6.5.dp.toPx(), 17.dp.toPx()))
                drawIconCircle(radius = 3.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(15.5.dp.toPx(), 15.dp.toPx()))
            }
            MobileDestination.Favorites -> {
                val path = Path().apply {
                    moveTo(11.5.dp.toPx(), 20.dp.toPx())
                    cubicTo(4.dp.toPx(), 15.dp.toPx(), 3.dp.toPx(), 10.dp.toPx(), 5.7.dp.toPx(), 7.dp.toPx())
                    cubicTo(8.dp.toPx(), 4.5.dp.toPx(), 10.5.dp.toPx(), 6.dp.toPx(), 11.5.dp.toPx(), 8.dp.toPx())
                    cubicTo(12.5.dp.toPx(), 6.dp.toPx(), 15.dp.toPx(), 4.5.dp.toPx(), 17.3.dp.toPx(), 7.dp.toPx())
                    cubicTo(20.dp.toPx(), 10.dp.toPx(), 19.dp.toPx(), 15.dp.toPx(), 11.5.dp.toPx(), 20.dp.toPx())
                }
                drawIconPath(path)
            }
            MobileDestination.Requests -> {
                drawIconRoundRect(
                    topLeft = androidx.compose.ui.geometry.Offset(3.5.dp.toPx(), 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 12.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5.dp.toPx()),
                )
                drawIconLine(
                    start = androidx.compose.ui.geometry.Offset(7.dp.toPx(), 12.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 12.dp.toPx()),
                )
                drawIconLine(
                    start = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 8.5.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 15.5.dp.toPx()),
                )
            }
            MobileDestination.Admin -> {
                val path = Path().apply {
                    moveTo(11.5.dp.toPx(), 3.dp.toPx())
                    lineTo(18.5.dp.toPx(), 6.dp.toPx())
                    lineTo(18.5.dp.toPx(), 11.4.dp.toPx())
                    cubicTo(18.5.dp.toPx(), 16.dp.toPx(), 15.4.dp.toPx(), 19.dp.toPx(), 11.5.dp.toPx(), 21.dp.toPx())
                    cubicTo(7.6.dp.toPx(), 19.dp.toPx(), 4.5.dp.toPx(), 16.dp.toPx(), 4.5.dp.toPx(), 11.4.dp.toPx())
                    lineTo(4.5.dp.toPx(), 6.dp.toPx())
                    close()
                }
                drawIconPath(path)
                drawIconLine(start = androidx.compose.ui.geometry.Offset(8.3.dp.toPx(), 11.5.dp.toPx()), end = androidx.compose.ui.geometry.Offset(10.6.dp.toPx(), 14.dp.toPx()))
                drawIconLine(start = androidx.compose.ui.geometry.Offset(10.6.dp.toPx(), 14.dp.toPx()), end = androidx.compose.ui.geometry.Offset(15.dp.toPx(), 9.dp.toPx()))
            }
            MobileDestination.Profile -> {
                drawIconCircle(radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f))
                repeat(8) { index ->
                    val angle = (index * 45.0) * kotlin.math.PI / 180.0
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val start = androidx.compose.ui.geometry.Offset(
                        x = cx + kotlin.math.cos(angle).toFloat() * 7.dp.toPx(),
                        y = cy + kotlin.math.sin(angle).toFloat() * 7.dp.toPx(),
                    )
                    val end = androidx.compose.ui.geometry.Offset(
                        x = cx + kotlin.math.cos(angle).toFloat() * 9.5.dp.toPx(),
                        y = cy + kotlin.math.sin(angle).toFloat() * 9.5.dp.toPx(),
                    )
                    drawIconLine(start = start, end = end)
                }
                drawIconCircle(radius = 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f))
            }
            else -> {
                drawIconCircle(radius = 4.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width / 2f, 7.5.dp.toPx()))
                drawIconRoundRect(
                    topLeft = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 14.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(13.dp.toPx(), 6.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun ProfileSettingsDialog(
    state: VantafynHomeUiState,
    onDismiss: () -> Unit,
    onSwitchUser: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onSwitchUser()
            }) {
                Text("Switch User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(state.session?.user?.name ?: "Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                Text(state.server?.name ?: "Jellyfin Server")
                Text(state.server?.url ?: "", color = VantafynColors.Muted)
                Text("Add Profile from the profile picker.")
                Text("App version 0.1.0")
            }
        },
    )
}

@Composable
private fun LibraryRow(
    title: String,
    libraries: List<JellyfinLibrary>,
    tv: Boolean,
    onOpenLibrary: (JellyfinLibrary) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp)) {
            items(libraries) { library ->
                Box(modifier = Modifier.clickable { onOpenLibrary(library) }) {
                    PosterCard(title = library.name, spec = if (tv) TvPosterSpec else MobilePosterSpec)
                }
            }
        }
    }
}

@Composable
private fun CenterPane(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val widthFraction = if (maxWidth > 700.dp) 0.56f else 1f
        Column(
            modifier = Modifier.fillMaxWidth(widthFraction),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Composable
private fun StatusBlock(state: VantafynHomeUiState) {
    if (state.isLoading) {
        Spacer(Modifier.height(VantafynSpacing.md))
        VantafynLoadingIndicator("Please wait")
    }
    state.errorMessage?.let {
        Spacer(Modifier.height(VantafynSpacing.md))
        VantafynErrorCard(it)
    }
}

private fun initials(name: String): String =
    name
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "V" }

private fun VantafynAppBackground.drawableResId(): Int =
    when (this) {
        VantafynAppBackground.Nebula -> CoreUiR.drawable.vantafyn_onboarding_background
        VantafynAppBackground.Background1 -> CoreUiR.drawable.vantafyn_background_1
        VantafynAppBackground.Background2 -> CoreUiR.drawable.vantafyn_background_2
        VantafynAppBackground.Background3 -> CoreUiR.drawable.vantafyn_background_3
        VantafynAppBackground.Background4 -> CoreUiR.drawable.vantafyn_background_4
    }

private data class MediaActionTarget(
    val id: java.util.UUID,
    val title: String,
    val subtitle: String?,
    val itemType: String?,
    val inMyList: Boolean = false,
)

@Composable
private fun MediaContextMenu(
    target: MediaActionTarget,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onAddToMyList: () -> Unit,
    onRemoveFromMyList: () -> Unit,
    onMarkWatched: () -> Unit,
    onMarkUnwatched: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(target.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(target.subtitle ?: target.itemType?.searchGroupLabel() ?: "Media", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ContextAction("ⓘ", "View details", onViewDetails)
                if (target.inMyList) {
                    ContextAction("♡", "Remove from My List", onRemoveFromMyList)
                } else {
                    ContextAction("＋", "Add to My List", onAddToMyList)
                }
                if (target.itemType?.contains("Episode", ignoreCase = true) == true || target.itemType?.contains("Movie", ignoreCase = true) == true) {
                    ContextAction("✓", "Mark watched", onMarkWatched)
                    ContextAction("↺", "Mark unwatched", onMarkUnwatched)
                }
                ContextAction("↓", "Downloads coming later", onDismiss, enabled = false)
            }
        },
    )
}

@Composable
private fun ContextAction(icon: String, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    VantafynGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        variant = VantafynGlassVariant.Card,
        enabled = enabled,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, color = if (enabled) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.5f))
            Text(label, color = if (enabled) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun JellyfinMediaCard.toMediaActionTarget(): MediaActionTarget =
    MediaActionTarget(id = id, title = title, subtitle = subtitle, itemType = itemType, inMyList = isFavorite)

private fun JellyfinMediaItem.toMediaActionTarget(inMyList: Boolean = false): MediaActionTarget =
    MediaActionTarget(id = id, title = title, subtitle = subtitle, itemType = itemType, inMyList = inMyList || isFavorite)

private fun JellyfinSearchResult.toMediaActionTarget(): MediaActionTarget =
    MediaActionTarget(id = id, title = title, subtitle = subtitle, itemType = itemType, inMyList = isFavorite)

private fun MobileDestination.bottomNavRoot(previous: MobileDestination): MobileDestination =
    when (this) {
        MobileDestination.MediaDetail,
        MobileDestination.Player -> previous.bottomNavRoot(MobileDestination.Home)
        MobileDestination.LibraryDetail -> MobileDestination.Libraries
        MobileDestination.AdminUserSettings -> MobileDestination.Admin
        MobileDestination.HomeLayout,
        MobileDestination.PlaybackPreferences -> MobileDestination.Profile
        else -> this
    }

private fun String.searchGroupLabel(): String =
    when (lowercase()) {
        "movie" -> "Movies"
        "series" -> "TV Shows"
        "episode" -> "Episodes"
        "boxset" -> "Collections"
        "audio", "musicalbum", "musicartist" -> "Music"
        "book" -> "Books"
        "livetvchannel", "livetvprogram" -> "Live TV"
        else -> replaceFirstChar(Char::titlecase)
    }

private fun WatchPartyMediaScope.watchPartyLabel(): String =
    when (this) {
        WatchPartyMediaScope.MoviesOnly -> "Movies"
        WatchPartyMediaScope.TvShowsOnly -> "TV Shows"
        WatchPartyMediaScope.MoviesAndTv -> "Movies + TV"
        WatchPartyMediaScope.ContinueWatchingOnly -> "Continue Watching"
        WatchPartyMediaScope.MyListOnly -> "My List"
        WatchPartyMediaScope.RecentlyAdded -> "Recently Added"
    }

private fun WatchPartyMode.watchPartyLabel(): String =
    when (this) {
        WatchPartyMode.FixedTitle -> "Watch this"
        WatchPartyMode.SwipeToMatch -> "Swipe to match"
    }

private fun WatchPartyMemberPresence.watchPartyLabel(): String =
    when (this) {
        WatchPartyMemberPresence.Online -> "Online"
        WatchPartyMemberPresence.Offline -> "Offline"
        WatchPartyMemberPresence.Unknown -> "Unknown"
    }

private fun WatchPartyMemberPlaybackStatus.watchPartyLabel(): String =
    when (this) {
        WatchPartyMemberPlaybackStatus.Playing -> "Playing"
        WatchPartyMemberPlaybackStatus.Paused -> "Paused"
        WatchPartyMemberPlaybackStatus.Buffering -> "Buffering"
        WatchPartyMemberPlaybackStatus.Unknown -> "Playback unknown"
    }

private fun WatchPartyMatchRule.watchPartyLabel(): String =
    when (this) {
        WatchPartyMatchRule.Everyone -> "Everyone"
        WatchPartyMatchRule.Majority -> "Majority"
    }

private fun dev.vantafyn.core.jellyfin.WatchPartyRuntimeLimit.watchPartyLabel(): String =
    when (this) {
        dev.vantafyn.core.jellyfin.WatchPartyRuntimeLimit.Under90Minutes -> "Under 90 min"
        dev.vantafyn.core.jellyfin.WatchPartyRuntimeLimit.Under2Hours -> "Under 2 hours"
        dev.vantafyn.core.jellyfin.WatchPartyRuntimeLimit.AnyLength -> "Any length"
    }

private fun WatchPartyCandidate.watchPartyMetaLine(): String =
    listOfNotNull(
        year?.toString(),
        runtimeMinutes?.let { "$it min" },
        officialRating,
        genres.take(2).joinToString(", ").takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { itemType?.searchGroupLabel() ?: "Jellyfin title" }

private fun String.matchesSearchType(selectedType: String?): Boolean {
    if (selectedType == null) return true
    val type = lowercase()
    return when (selectedType.lowercase()) {
        "music" -> type in setOf("audio", "musicalbum", "musicartist")
        "boxset" -> type == "boxset"
        else -> type == selectedType.lowercase()
    }
}

private fun Int.toHourLimitLabel(): String =
    if (this == 1) "1 hour" else "$this hours"

private fun JellyfinMediaDetail.primaryActionLabel(): String {
    val watchedProgress = progress
    return when {
        watchedProgress != null && watchedProgress > 0.05f -> "Resume"
        itemType.equals("Book", ignoreCase = true) -> "Open"
        itemType.equals("Episode", ignoreCase = true) && subtitle != null -> "Play $subtitle"
        else -> "Play"
    }
}

private fun JellyfinMediaDetail.finishAtLabel(nowMs: Long): String? {
    val runtimeMs = runtimeMinutes
        ?.takeIf { it > 0 }
        ?.toLong()
        ?.times(60_000L)
        ?: return null
    val resumeMs = if (!isPlayed && (progress ?: 0f) > 0.05f) {
        (playbackPositionTicks / 10_000L).coerceIn(0L, runtimeMs)
    } else {
        0L
    }
    val remainingMs = (runtimeMs - resumeMs).coerceAtLeast(60_000L)
    val finishTime = Date(nowMs + remainingMs)
    return "Finishes at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(finishTime)}"
}

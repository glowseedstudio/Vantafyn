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
import android.provider.Settings
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.EmojiEvents
import dev.vantafyn.core.jellyfin.JellyfinAchievementUnlock
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material.icons.rounded.CropLandscape
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import dev.vantafyn.core.cast.GoogleCastRouteButton
import dev.vantafyn.core.downloads.DownloadRecord
import dev.vantafyn.core.downloads.DownloadMediaType
import dev.vantafyn.core.downloads.DownloadStorageSummary
import dev.vantafyn.core.downloads.DownloadState
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.jellyfin.JellyfinLibraryItemFilter
import dev.vantafyn.core.jellyfin.JellyfinLibraryPage
import dev.vantafyn.core.jellyfin.JellyfinHomeSection
import dev.vantafyn.core.jellyfin.LibraryViewMode
import dev.vantafyn.core.jellyfin.LibrariesViewMode
import dev.vantafyn.core.jellyfin.JellyfinLyrics
import dev.vantafyn.core.jellyfin.JellyfinLyricLine
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinEpisode
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import dev.vantafyn.core.jellyfin.JellyfinMediaCardShape
import dev.vantafyn.core.jellyfin.JellyfinMediaItem
import dev.vantafyn.core.jellyfin.JellyfinMediaSegmentBehavior
import dev.vantafyn.core.jellyfin.JellyfinMediaSegmentType
import dev.vantafyn.core.jellyfin.JellyfinPublicUser
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSearchResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.JellyfinHeroMediaItem
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.jellyfin.SyncPlayConnectionState
import dev.vantafyn.core.jellyfin.JellyfinDisplayMessage
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
import dev.vantafyn.core.media.UpNextDisplayMode
import dev.vantafyn.core.media.VantafynExoPlayerFactory
import dev.vantafyn.core.media.VantafynMedia3ExtensionSupport
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
import dev.vantafyn.core.ui.VantafynGlassModalPanel
import dev.vantafyn.core.ui.VantafynGlassPanel
import dev.vantafyn.core.ui.VantafynGlassPill
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassTile
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.VantafynGradientSpinner
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynLogoHeader
import dev.vantafyn.core.ui.VantafynNavSelectedBrush
import dev.vantafyn.core.ui.VantafynOnboardingBackground
import dev.vantafyn.core.ui.VantafynPermissionStatus
import dev.vantafyn.core.ui.VantafynPermissionUiState
import dev.vantafyn.core.ui.VantafynProfileCard
import dev.vantafyn.core.ui.VantafynScreenScaffold
import dev.vantafyn.core.ui.VantafynServerCard
import dev.vantafyn.core.ui.VantafynSetupHeader
import dev.vantafyn.core.ui.VantafynSkeletonBlock
import dev.vantafyn.core.ui.VantafynSkeletonBrush
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.VantafynThemePreset
import dev.vantafyn.core.ui.tokensFor
import dev.vantafyn.core.ui.rememberLifecycleAwareMarquee
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynHomeViewModel
import dev.vantafyn.feature.home.auth.HomeSectionType
import dev.vantafyn.feature.home.auth.MobileDestination
import dev.vantafyn.feature.home.auth.ThemeMusicVolume
import dev.vantafyn.feature.home.auth.BottomRailAccent
import dev.vantafyn.feature.home.auth.HomeSectionPreference
import dev.vantafyn.feature.home.auth.MAX_STREAMING_BITRATE_MBPS_OPTIONS
import dev.vantafyn.feature.home.auth.VantafynAppBackground
import dev.vantafyn.feature.home.auth.VantafynArtworkType
import dev.vantafyn.feature.home.auth.VantafynCardShape
import dev.vantafyn.feature.home.auth.VantafynCardSize
import dev.vantafyn.feature.home.auth.VantafynCardSpacing
import dev.vantafyn.feature.home.auth.VantafynVideoPlayerPreference
import dev.vantafyn.feature.requests.RequestsScreen
import dev.vantafyn.feature.home.auth.VantafynSetupStep
import dev.vantafyn.feature.player.MobilePlayerScreen
import dev.vantafyn.feature.home.auth.defaultHomeLayout
import dev.vantafyn.feature.home.auth.supportedSmartRows
import dev.vantafyn.feature.music.MusicScreen
import dev.vantafyn.feature.music.MusicViewModel
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
import java.io.File
import java.util.Date
import java.util.UUID
import kotlin.math.abs

private val VantafynModalContainerColor: Color
    get() = VantafynColors.Graphite.copy(alpha = 0.96f)
private val VantafynSetupCinematicEasing = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

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
    val reducedMotion = rememberReducedMotionPreference()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.isStartupResolved && state.step != VantafynSetupStep.Home,
            enter = if (reducedMotion) {
                fadeIn(animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
            } else {
                fadeIn(animationSpec = tween(durationMillis = 360, easing = VantafynSetupCinematicEasing))
            },
            exit = if (reducedMotion) {
                fadeOut(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
            } else {
                fadeOut(animationSpec = tween(durationMillis = 620, easing = VantafynSetupCinematicEasing))
            },
        ) {
            VantafynOnboardingBackground(
                tv = tv,
                modifier = Modifier.matchParentSize(),
                backgroundResId = backgroundResId,
            ) {}
        }
        AnimatedContent(
            targetState = if (state.isStartupResolved || state.step == VantafynSetupStep.Home) state.step else null,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)))
                        .using(SizeTransform(clip = false))
                } else {
                    val enteringHome = targetState == VantafynSetupStep.Home
                    val enterDuration = if (enteringHome) 1_180 else 980
                    val exitDuration = if (enteringHome) 620 else 520
                    (
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = enterDuration,
                                delayMillis = if (enteringHome) 160 else 150,
                                easing = VantafynSetupCinematicEasing,
                            ),
                        ) + slideInVertically(
                            animationSpec = tween(
                                durationMillis = enterDuration,
                                delayMillis = if (enteringHome) 160 else 150,
                                easing = VantafynSetupCinematicEasing,
                            ),
                            initialOffsetY = { if (enteringHome) 12 else 8 },
                        )
                    ).togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = exitDuration, easing = VantafynSetupCinematicEasing)) +
                            slideOutVertically(
                                animationSpec = tween(durationMillis = exitDuration, easing = VantafynSetupCinematicEasing),
                                targetOffsetY = { if (enteringHome) -6 else -5 },
                            ),
                    ).using(SizeTransform(clip = false))
                }
            },
            label = "setupStepTransition",
        ) { step ->
            when (step) {
                null -> Box(Modifier.fillMaxSize())
                VantafynSetupStep.Splash -> SplashScreen(tv = tv)
                VantafynSetupStep.Welcome -> WelcomeScreen(
                    state = state,
                    tv = tv,
                    onContinue = viewModel::continueFromWelcome,
                    onBack = viewModel::navigateSetupBack,
                    showBack = state.savedProfiles.isNotEmpty(),
                )
                VantafynSetupStep.ConnectServer -> ConnectServerScreen(
                state = state,
                tv = tv,
                onLocalServerUrlChanged = viewModel::onLocalServerUrlChanged,
                onRemoteServerUrlChanged = viewModel::onRemoteServerUrlChanged,
                onConnect = viewModel::connectToServer,
                onBack = viewModel::navigateSetupBack,
            )
                VantafynSetupStep.ServerConfirm -> ServerConfirmScreen(
                state = state,
                tv = tv,
                onContinue = viewModel::continueToLogin,
                onBack = viewModel::navigateSetupBack,
            )
                VantafynSetupStep.Login -> LoginScreen(
                state = state,
                tv = tv,
                onUsernameChanged = viewModel::onUsernameChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onLogin = viewModel::login,
                onQuickConnect = viewModel::startQuickConnect,
                onBack = viewModel::navigateSetupBack,
            )
                VantafynSetupStep.QuickConnect -> QuickConnectScreen(
                state = state,
                tv = tv,
                onBack = viewModel::navigateSetupBack,
                onRetry = viewModel::retryQuickConnect,
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
                onWorkOffline = viewModel::workOfflineFromRecovery,
                onBack = viewModel::navigateSetupBack,
            )
                VantafynSetupStep.Home -> AnimatedVisibility(
                    visible = !state.isLogoutTransitioning,
                    enter = EnterTransition.None,
                    exit = if (reducedMotion) {
                        fadeOut(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
                    } else {
                        fadeOut(animationSpec = tween(durationMillis = 560, easing = VantafynSetupCinematicEasing)) +
                            slideOutVertically(
                                animationSpec = tween(durationMillis = 560, easing = VantafynSetupCinematicEasing),
                                targetOffsetY = { -5 },
                            )
                    },
                ) {
                    HomeScreen(
                        state = state,
                        tv = tv,
                        onRetry = viewModel::retryLibraries,
                        onSwitchUser = viewModel::showProfilePicker,
                        onAddProfile = viewModel::addProfile,
                        onQuickConnect = viewModel::openDeviceQuickConnect,
                        onDeviceQuickConnectCodeChanged = viewModel::onDeviceQuickConnectCodeChanged,
                        onAuthorizeDeviceQuickConnect = viewModel::authorizeDeviceQuickConnect,
                        onConfirmLogout = viewModel::confirmCurrentProfileLogout,
                        onCancelLogout = viewModel::cancelCurrentProfileLogout,
                        onLogoutCurrentProfile = viewModel::logoutCurrentProfile,
                        onNavigateMobile = viewModel::navigateMobile,
                        onOpenLibrary = viewModel::openLibrary,
                        onReorderLibraries = viewModel::reorderLibraries,
                        onSetLibrariesViewMode = viewModel::setLibrariesViewMode,
                        onRetryLibrary = viewModel::retryLibraryItems,
                        onSetLibraryFilter = viewModel::setLibraryItemsFilter,
                        onSetLibraryAlphabet = viewModel::setLibraryAlphabetKey,
                        onSetViewMode = viewModel::setLibraryViewMode,
                        onPreviousLibraryPage = viewModel::previousLibraryItemsPage,
                        onNextLibraryPage = viewModel::nextLibraryItemsPage,
                        onRefreshAdmin = viewModel::pollAdminOverview,
                        onOpenMedia = viewModel::openMedia,
                        onMarkWhatsNewSeen = viewModel::markWhatsNewSeen,
                        onToggleWhatsNew = viewModel::toggleWhatsNew,
                onRetryMedia = viewModel::retryMediaDetail,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onLoadFavorites = viewModel::loadFavorites,
                onPlaybackComingSoon = viewModel::showPlaybackComingSoon,
                onClearMessage = viewModel::clearMobileMessage,
                onToggleHomeSection = viewModel::toggleHomeSection,
                onMoveHomeSection = viewModel::moveHomeSection,
                onResetHomeLayout = viewModel::resetHomeLayout,
                onSaveHomeLayoutDraft = viewModel::saveHomeLayoutDraft,
                onAddSmartRow = viewModel::addSmartRow,
                onRemoveSmartRow = viewModel::removeSmartRow,
                onCycleArtwork = viewModel::cycleSectionArtwork,
                onCycleShape = viewModel::cycleSectionShape,
                onCycleSize = viewModel::cycleSectionSize,
                onCycleSpacing = viewModel::cycleSectionSpacing,
                onToggleThemeMusic = viewModel::toggleThemeMusic,
                onSelectThemeMusicVolume = viewModel::selectThemeMusicVolume,
                onSetBottomRailAccent = viewModel::setBottomRailAccent,
                onToggleAutoLoginLastProfile = viewModel::toggleAutoLoginLastProfile,
                onSelectBackground = viewModel::selectBackground,
                onSelectTheme = viewModel::selectTheme,
            onToggleMediaFavorite = viewModel::toggleMediaFavorite,
            onToggleMediaPlayed = viewModel::toggleMediaPlayed,
            onSetMediaFavorite = viewModel::setMediaFavorite,
            onSetMediaPlayed = viewModel::setMediaPlayed,
            onQueueMediaDownload = viewModel::queueMediaDownloadById,
            onOpenDownloads = viewModel::openDownloads,
            onRefreshDownloads = viewModel::loadDownloads,
            onPlayOfflineDownload = viewModel::playOfflineDownload,
            onCancelDownload = viewModel::cancelDownload,
            onRetryDownload = viewModel::retryDownload,
            onRemoveDownload = viewModel::removeDownload,
            onRemoveAllDownloads = viewModel::removeAllDownloads,
            onSetDownloadWifiOnlyDefault = viewModel::setDownloadWifiOnlyDefault,
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
                onPlayPreviousEpisode = viewModel::playPreviousEpisode,
            onPlayerError = viewModel::handlePlayerError,
                onPrepareCastPlayback = viewModel::prepareCastPlayback,
                onSelectPlaybackAudioTrack = viewModel::selectPlaybackAudioTrack,
                onSelectPlaybackSubtitleTrack = viewModel::selectPlaybackSubtitleTrack,
                onSyncPlayPause = viewModel::sendWatchPartyPause,
                onSyncPlayResume = viewModel::sendWatchPartyResume,
                onSyncPlaySeek = viewModel::sendWatchPartySeek,
                onStartLiveTvPlayback = viewModel::startLiveTvPlayback,
                onEditPlaybackPreferences = viewModel::editPlaybackPreferences,
                onSavePlaybackPreferences = viewModel::savePlaybackPreferences,
                onSetAutoplayCountdownSeconds = viewModel::setAutoplayCountdownSeconds,
                onSetUpNextDisplayMode = viewModel::setUpNextDisplayMode,
                onTogglePassoutProtection = viewModel::togglePassoutProtection,
                onSetPassoutProtectionLimitMinutes = viewModel::setPassoutProtectionLimitMinutes,
                onSelectVideoPlayerPreference = viewModel::selectVideoPlayerPreference,
                onSetMaxStreamingBitrateMbps = viewModel::setMaxStreamingBitrateMbps,
                onSetMediaSegmentBehavior = viewModel::setMediaSegmentBehavior,
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
                onLoadWatchParty = viewModel::shuffleWatchPartyDeck,
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
                onSetAdminSpeedLimit = viewModel::setAdminSpeedLimitMbps,
                onSendAdminSessionMessage = viewModel::sendAdminSessionMessage,
                onSendAdminBroadcastMessage = viewModel::sendAdminBroadcastMessage,
                onClearAdminSessionMessageError = viewModel::clearAdminSessionMessageError,
                onNavigateBack = viewModel::navigateMobileBack,
                onOpenAchievements = viewModel::openAchievements,
                onRetryAchievements = { viewModel.loadAchievements(force = true) },
                onDismissAchievementUnlock = viewModel::dismissAchievementUnlock,
                onToggleAchievementsEnabled = viewModel::toggleAchievementsEnabled,
                onToggleSocialEnabled = viewModel::toggleSocialEnabled,
                onToggleSocialDockEnabled = viewModel::toggleSocialDockEnabled,
                onDismissSocialDock = viewModel::dismissSocialDock,
                onOpenSocial = viewModel::openSocialScreen,
                onOpenSocialPanel = viewModel::openSocialPanel,
                onCloseSocialPanel = viewModel::closeSocialPanel,
                onOpenChatWithFriend = viewModel::openChatWithFriend,
                onOpenChatFromConversation = viewModel::openChatFromConversation,
                onSendChatMessage = viewModel::sendChatMessage,
                onAcceptFriendRequest = viewModel::acceptFriendRequest,
                onDeclineFriendRequest = viewModel::declineOrRemoveFriend,
                onSendFriendRequest = viewModel::sendFriendRequest,
                onRemoveFriend = viewModel::removeFriend,
                onBlockUser = viewModel::blockUser,
                onUnblockUser = viewModel::unblockUser,
                onDeleteConversation = viewModel::deleteConversation,
                onClearChatWithActivePeer = viewModel::clearChatWithActivePeer,
                onShareMediaToFriend = viewModel::shareMediaRecommendationToFriend,
                onDismissSocialIslandPreview = viewModel::dismissSocialIslandPreview,
                onSetActiveSocialTab = viewModel::setActiveSocialTab,
                onRefreshSocial = { viewModel.loadSocialData(force = true) },
                onSearchChatMedia = viewModel::searchChatMedia,
                onRefreshChatMessages = {
                    val activePeer = viewModel.state.value.activeChatPeer
                    if (activePeer != null) {
                        viewModel.openChatWithFriend(activePeer)
                    }
                },
                notificationPermissionState = notificationPermissionState,
                onRequestMusicControlsPermission = onRequestMusicControlsPermission,
                onNotificationPermissionSettingsAction = onNotificationPermissionSettingsAction,
            )
            }
        }
        }
        if (!tv) {
            DisplayMessageOverlay(
                message = state.displayMessage,
                compact = state.mobileDestination == MobileDestination.Player,
                onDismiss = viewModel::dismissDisplayMessage,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            WatchPartyInviteOverlay(
                invite = state.activeIncomingWatchPartyInvite,
                message = state.incomingWatchPartyMessage,
                onAccept = viewModel::acceptIncomingWatchPartyInvite,
                onDecline = viewModel::declineIncomingWatchPartyInvite,
                onClearMessage = viewModel::clearIncomingWatchPartyMessage,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            FriendRequestOverlay(
                request = state.activeIncomingFriendRequest,
                onAccept = {
                    state.activeIncomingFriendRequest?.let { req ->
                        viewModel.acceptFriendRequest(req.id)
                    }
                },
                onDecline = {
                    state.activeIncomingFriendRequest?.let { req ->
                        viewModel.declineOrRemoveFriend(req.id)
                    }
                },
                onDismiss = viewModel::dismissIncomingFriendRequest,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun Modifier.swipeToDismissTopNotification(
    onDismiss: () -> Unit,
): Modifier {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val dismissThresholdPx = remember(density) { with(density) { 20.dp.toPx() } }

    return this
        .offset { IntOffset(0, offsetY.roundToInt().coerceAtMost(0)) }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    if (dragAmount < 0f || offsetY < 0f) {
                        change.consume()
                        offsetY = (offsetY + dragAmount).coerceAtMost(0f)
                        if (offsetY < -dismissThresholdPx && !isDismissed) {
                            isDismissed = true
                            onDismiss()
                        }
                    }
                },
                onDragEnd = {
                    if (offsetY < -dismissThresholdPx && !isDismissed) {
                        isDismissed = true
                        onDismiss()
                    } else {
                        offsetY = 0f
                    }
                },
                onDragCancel = {
                    offsetY = 0f
                }
            )
        }
}

@Composable
private fun DisplayMessageOverlay(
    message: JellyfinDisplayMessage?,
    compact: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 12.dp),
        enter = slideInVertically(
            initialOffsetY = { -it - 24 },
            animationSpec = tween(420, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 24 },
            animationSpec = tween(280, easing = FastOutSlowInEasing),
        ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)),
    ) {
        message?.let { current ->
            VantafynGlassSurface(
                modifier = Modifier
                    .swipeToDismissTopNotification(onDismiss)
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.2.dp, durationMillis = 5200),
                variant = VantafynGlassVariant.Modal,
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = if (compact) 12.dp else 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 36.dp else 42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(12.dp))
                            .padding(if (compact) 6.dp else 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = CoreUiR.drawable.vantafyn_logo),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            current.header ?: "Jellyfin message",
                            color = VantafynColors.Ink,
                            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            current.text,
                            color = VantafynColors.Muted.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (compact) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss message",
                            tint = VantafynColors.Ink.copy(alpha = 0.86f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
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
                .swipeToDismissTopNotification(onClearMessage)
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
private fun FriendRequestOverlay(
    request: dev.vantafyn.core.jellyfin.JellyfinFriendRequest?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(request?.id) {
        if (request != null) {
            dev.vantafyn.core.ui.VantafynSoundEffects.playFriendRequestAlert(context)
            delay(5_000L)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = request != null,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        enter = slideInVertically(
            initialOffsetY = { -it - 24 },
            animationSpec = tween(420, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 24 },
            animationSpec = tween(280, easing = FastOutSlowInEasing),
        ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)),
    ) {
        request?.let { req ->
            Box(
                modifier = Modifier
                    .swipeToDismissTopNotification(onDismiss)
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF141828).copy(alpha = 0.96f),
                                Color(0xFF0C101C).copy(alpha = 0.96f),
                            ),
                        ),
                    )
                    .vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.3.dp, durationMillis = 4000)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Profile Avatar with presence-style accent ring & fallback monogram
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF201A38))
                            .border(1.2.dp, Color(0xFFA78BFA).copy(alpha = 0.60f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!req.senderAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = req.senderAvatarUrl,
                                contentDescription = req.senderName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = req.senderName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA),
                            )
                        }
                    }

                    // Text Details with generous width for long usernames
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = "FRIEND REQUEST",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA),
                            letterSpacing = 0.8.sp,
                        )
                        Text(
                            text = req.senderName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Sent you a request",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = VantafynColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Compact, elegant circular actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Decline
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                .clickable(onClick = onDecline),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Decline",
                                tint = Color(0xFFFF6688),
                                modifier = Modifier.size(17.dp),
                            )
                        }

                        // Accept
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VantafynGradients.accentHorizontal())
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                .clickable(onClick = onAccept),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Accept",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberReducedMotionPreference(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val resolver = context.contentResolver
        val animatorScale = runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val transitionScale = runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        }.getOrDefault(1f)
        animatorScale == 0f || transitionScale == 0f
    }
}

@Composable
private fun SetupMaterialize(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotionPreference()
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = if (reducedMotion) {
            fadeIn(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
        } else {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 1_500,
                    delayMillis = delayMillis,
                    easing = VantafynSetupCinematicEasing,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 1_500,
                    delayMillis = delayMillis,
                    easing = VantafynSetupCinematicEasing,
                ),
                initialOffsetY = { 7 },
            )
        },
        exit = fadeOut(animationSpec = tween(durationMillis = 380, easing = VantafynSetupCinematicEasing)),
    ) {
        content()
    }
}

@Composable
fun SplashScreen(tv: Boolean, modifier: Modifier = Modifier, message: String = "Preparing Vantafyn") {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SetupMaterialize(delayMillis = 0) {
                Text("Vantafyn", color = VantafynColors.Ink, style = MaterialTheme.typography.displayLarge)
            }
            Spacer(modifier = Modifier.height(VantafynSpacing.sm))
            SetupMaterialize(delayMillis = 280) {
                Text(message, color = VantafynColors.Muted, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(VantafynSpacing.lg))
            SetupMaterialize(delayMillis = 560) {
                VantafynLoadingIndicator("Signing in")
            }
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SetupBackScaffold(onBack = onBack, showBack = showBack) {
            CenterPane {
                SetupMaterialize(delayMillis = 0) {
                    VantafynLogoHeader(
                        title = "Vantafyn",
                        tagline = "Your media, beautifully streamed.",
                        tv = tv,
                    )
                }
                Spacer(Modifier.height(if (tv) 44.dp else 36.dp))
                SetupMaterialize(delayMillis = 260, modifier = Modifier.fillMaxWidth(),) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        VantafynButton("Continue", onClick = onContinue, modifier = Modifier.fillMaxWidth(if (tv) 0.48f else 0.78f))
                        if (state.savedProfiles.isNotEmpty()) {
                            Spacer(Modifier.height(VantafynSpacing.lg))
                            Text("Add another Jellyfin profile", color = VantafynColors.Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectServerScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onLocalServerUrlChanged: (String) -> Unit,
    onRemoteServerUrlChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SetupBackScaffold(onBack = onBack) {
            CenterPane {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                ) {
                    SetupMaterialize(delayMillis = 0) {
                        VantafynSetupHeader(
                            title = "Connect your server",
                            subtitle = "Use a local address or domain for your Jellyfin server.",
                            tv = tv,
                        )
                    }
                    Spacer(Modifier.height(if (tv) VantafynSpacing.md else VantafynSpacing.xs))
                    SetupMaterialize(delayMillis = 170, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                            VantafynTextField(
                                value = state.localServerUrl,
                                onValueChange = onLocalServerUrlChanged,
                                label = "Local address (optional)",
                                placeholder = "http://192.168.1.20:8096",
                                enabled = !state.isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                tvKeyboardRequiresClick = tv,
                            )
                            VantafynTextField(
                                value = state.remoteServerUrl,
                                onValueChange = onRemoteServerUrlChanged,
                                label = "Remote address (optional)",
                                placeholder = "https://jellyfin.example.com",
                                enabled = !state.isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                tvKeyboardRequiresClick = tv,
                            )
                            Text(
                                "Use one address or add both. Vantafyn tries local first, then remote.",
                                color = VantafynColors.Muted.copy(alpha = 0.82f),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            StatusBlock(state)
                        }
                    }
                    Spacer(Modifier.height(VantafynSpacing.sm))
                    SetupMaterialize(delayMillis = 360, modifier = Modifier.fillMaxWidth()) {
                        VantafynButton(
                            "Connect",
                            onClick = onConnect,
                            enabled = !state.isLoading && (state.localServerUrl.isNotBlank() || state.remoteServerUrl.isNotBlank()),
                            modifier = Modifier.fillMaxWidth(if (tv) 0.48f else 0.78f),
                        )
                    }
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SetupBackScaffold(onBack = onBack) {
            CenterPane {
                SetupMaterialize(delayMillis = 0) {
                    VantafynSetupHeader(
                        title = "Server found",
                        subtitle = "Ready to connect to your media library.",
                        tv = tv,
                    )
                }
                Spacer(Modifier.height(if (tv) VantafynSpacing.xxl else VantafynSpacing.xl))
                val server = state.server
                SetupMaterialize(delayMillis = 180, modifier = Modifier.fillMaxWidth()) {
                    val borderAlpha = remember { Animatable(0f) }
                    val infiniteTransition = rememberInfiniteTransition(label = "serverBorderSweep")
                    val borderShift by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 5_200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                        label = "serverBorderShift",
                    )

                    LaunchedEffect(Unit) {
                        borderAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 800, delayMillis = 400, easing = FastOutSlowInEasing),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                drawContent()
                                val alpha = borderAlpha.value
                                if (alpha > 0f) {
                                    val r = 22.dp.toPx()
                                    val s = Offset(-size.width * borderShift, -size.height * borderShift)
                                    val e = Offset(size.width * (1f - borderShift), size.height * (1f - borderShift))
                                    drawRoundRect(
                                        brush = Brush.linearGradient(
                                            colors = (VantafynGradients.AccentColors + VantafynGradients.AccentColors.first())
                                                .map { it.copy(alpha = it.alpha * alpha) },
                                            start = s,
                                            end = e,
                                            tileMode = TileMode.Repeated,
                                        ),
                                        cornerRadius = CornerRadius(r, r),
                                        style = Stroke(width = 1.5.dp.toPx()),
                                    )
                                }
                            },
                    ) {
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
                    }
                }
                SetupMaterialize(delayMillis = 320, modifier = Modifier.fillMaxWidth()) {
                    StatusBlock(state)
                }
                Spacer(Modifier.height(if (tv) VantafynSpacing.xxl else VantafynSpacing.xl))
                SetupMaterialize(delayMillis = 430, modifier = Modifier.fillMaxWidth()) {
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SetupBackScaffold(onBack = onBack) {
            CenterPane {
                SetupMaterialize(delayMillis = 0) {
                    VantafynSetupHeader(
                        title = state.server?.name ?: "Sign in",
                        subtitle = state.server?.url ?: state.serverUrl,
                        tv = tv,
                    )
                }
                Spacer(Modifier.height(VantafynSpacing.lg))
                SetupMaterialize(delayMillis = 170, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
                        VantafynTextField(
                            value = state.username,
                            onValueChange = onUsernameChanged,
                            label = "Username",
                            enabled = !state.isLoading,
                            tvKeyboardRequiresClick = tv,
                        )
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
                    }
                }
                Spacer(Modifier.height(VantafynSpacing.lg))
                SetupMaterialize(delayMillis = 380, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        VantafynButton(
                            "Sign In",
                            onClick = onLogin,
                            enabled = !state.isLoading && state.username.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(if (tv) 0.48f else 0.78f),
                        )
                        Spacer(Modifier.height(VantafynSpacing.sm))
                        TextButton(onClick = onQuickConnect, enabled = state.server != null && !state.isLoading) {
                            Text("Use Quick Connect", color = VantafynColors.Muted)
                        }
                    }
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
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
                SetupMaterialize(delayMillis = 0) {
                    VantafynSetupHeader(
                        title = "Quick Connect",
                        subtitle = "Approve Vantafyn from your Jellyfin dashboard.",
                        tv = tv,
                    )
                }
                Spacer(Modifier.height(VantafynSpacing.xl))
                SetupMaterialize(delayMillis = 170) {
	                    Box(
	                        modifier = Modifier
	                            .width(if (tv) 420.dp else 312.dp)
	                            .heightIn(min = if (tv) 238.dp else 210.dp)
	                            .vantafynAnimatedModalBorder(cornerRadius = 22.dp, strokeWidth = 1.4.dp, durationMillis = 4200)
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
	                                maxLines = 4,
	                                overflow = TextOverflow.Visible,
	                                modifier = Modifier.fillMaxWidth(),
	                            )
                        }
                    }
                }
                SetupMaterialize(delayMillis = 320, modifier = Modifier.fillMaxWidth()) {
                    StatusBlock(state)
                }
                Spacer(Modifier.height(VantafynSpacing.lg))
                SetupMaterialize(delayMillis = 430) {
                    VantafynButton("Refresh Code", onClick = onRetry, enabled = !state.isLoading)
                }
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
    onWorkOffline: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.restoreFailureProfile
    val currentAddress = profile?.serverUrl ?: state.serverUrl
    val looksLocal = currentAddress.looksLocalServerAddress()
    Box(modifier = modifier.fillMaxSize()) {
        SetupBackScaffold(onBack = onBack) {
            CenterPane {
                SetupMaterialize(delayMillis = 0) {
                    ConnectionRecoveryHeader(tv = tv)
                }
                Spacer(Modifier.height(if (tv) VantafynSpacing.lg else VantafynSpacing.md))
                SetupMaterialize(delayMillis = 180, modifier = Modifier.fillMaxWidth()) {
                    VantafynGlassPanel(
                        modifier = Modifier
                            .fillMaxWidth(if (tv) 0.58f else 1f)
                            .vantafynAnimatedModalBorder(),
                        cornerRadius = 30.dp,
                        contentPadding = PaddingValues(if (tv) 22.dp else 15.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProfileAvatar(
                                    name = profile?.displayName ?: "Saved profile",
                                    imageUrl = profile?.imageUrl,
                                    modifier = Modifier.size(if (tv) 60.dp else 48.dp),
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        profile?.displayName ?: "Saved profile",
                                        color = VantafynColors.Ink,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        profile?.serverName ?: "Jellyfin Server",
                                        color = VantafynColors.Muted,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (looksLocal) {
                                    SoftBadge("Local")
                                }
                            }

                            VantafynGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                variant = VantafynGlassVariant.Card,
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    currentAddress.ifBlank { "No saved server address" },
                                    color = VantafynColors.Ink.copy(alpha = 0.88f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (looksLocal) {
                                Text(
                                    "Local addresses usually only work at home. Use your remote address when you’re away.",
                                    color = VantafynColors.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            VantafynTextField(
                                value = state.serverUrl,
                                onValueChange = onServerUrlChanged,
                                label = "Update address",
                                placeholder = "https://media.example.com",
                                enabled = !state.isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                tvKeyboardRequiresClick = tv,
                            )

                            state.restoreFailureMessage?.let {
                                Text(
                                    it,
                                    color = VantafynColors.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            StatusBlock(state)

                            if (state.offlineDownloads.any { it.state == DownloadState.Completed }) {
                                VantafynGlassSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = VantafynGlassVariant.Card,
                                    cornerRadius = 20.dp,
                                    contentPadding = PaddingValues(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        SettingsRowIcon(Icons.Rounded.DownloadDone)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Text(
                                                "Offline library ready",
                                                color = VantafynColors.Ink,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                "Play saved titles while this server reconnects.",
                                                color = VantafynColors.Muted,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                }
                                VantafynButton(
                                    "Work offline",
                                    onClick = onWorkOffline,
                                    enabled = !state.isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                VantafynButton("Retry", onClick = onRetry, enabled = !state.isLoading, modifier = Modifier.height(52.dp).weight(1f))
                                VantafynButton(
                                    "Save",
                                    onClick = onSaveServerAddress,
                                    enabled = !state.isLoading && state.serverUrl.isNotBlank(),
                                    modifier = Modifier.height(52.dp).weight(1f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onSignInAgain,
                                    enabled = !state.isLoading,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .weight(1f),
                                ) {
                                    Text("Sign in again", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = onUseAnotherServer,
                                    enabled = !state.isLoading,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .weight(1f),
                                ) {
                                    Text("New server", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (state.savedProfiles.size > 1) {
                                TextButton(onClick = onChooseProfile, enabled = !state.isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                    Text("Choose another profile")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionRecoveryHeader(tv: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tv) 10.dp else 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (tv) 68.dp else 56.dp)
                .clip(RoundedCornerShape(if (tv) 18.dp else 16.dp))
                .background(Color.Black)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(if (tv) 18.dp else 16.dp))
                .padding(if (tv) 10.dp else 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = CoreUiR.drawable.vantafyn_logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            "Server unavailable",
            color = VantafynColors.Ink,
            style = if (tv) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            "We couldn't reach this saved Jellyfin server.",
            color = VantafynColors.Muted,
            style = if (tv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
    val revealKey = "mobile-profile-picker-${state.savedProfiles.size}-${state.publicUsers.size}-${showBack}"
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
                HomeContentReveal(index = 0, animate = true, revealKey = revealKey) {
                    Text(
                        "Who's watching?",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(VantafynSpacing.xl))
                HomeContentReveal(
                    index = 1,
                    animate = true,
                    revealKey = revealKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                        contentPadding = PaddingValues(bottom = VantafynSpacing.xl),
                    ) {
                        gridItems(state.savedProfiles, key = { it.id }) { profile ->
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
                }
            }
            HomeContentReveal(
                index = 2,
                animate = true,
                revealKey = revealKey,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            ) {
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
    val revealKey = "tv-profile-picker-${state.savedProfiles.size}-${state.publicUsers.size}-${showBack}"
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
                HomeContentReveal(index = 0, animate = true, revealKey = revealKey) {
                    Text(
                        "Who's watching?",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(46.dp))
                HomeContentReveal(index = 1, animate = true, revealKey = revealKey, modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
                        contentPadding = PaddingValues(horizontal = VantafynSpacing.xl),
                    ) {
                        items(state.savedProfiles, key = { it.id }) { profile ->
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
                }
            }
            HomeContentReveal(
                index = 2,
                animate = true,
                revealKey = revealKey,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
            ) {
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
            val label = if (state.manageProfiles) "Done" else "Manage"
            Text(
                label,
                style = MaterialTheme.typography.titleMedium.copy(
                    brush = VantafynGradients.accentHorizontal(),
                    fontWeight = FontWeight.SemiBold,
                ),
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> VantafynLoadingIndicator("Restoring profile")
            state.pendingRemoval != null -> {
                val profile = state.pendingRemoval
                VantafynAttentionPanel(
                    title = "Remove saved profile?",
                    message = "${profile.displayName} will be removed from this device. Jellyfin stays unchanged.",
                    icon = Icons.Rounded.Delete,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProfileRemovalActionButton(
                            text = "Cancel",
                            onClick = onCancelRemove,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            destructive = false,
                        )
                        ProfileRemovalActionButton(
                            text = "Remove",
                            onClick = onConfirmRemove,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            destructive = true,
                        )
                    }
                }
            }
            state.errorMessage != null -> VantafynAttentionPanel(
                title = "Something needs attention",
                message = state.errorMessage,
                icon = Icons.Rounded.Info,
            )
        }
    }
}

@Composable
private fun ProfileRemovalActionButton(
    text: String,
    onClick: () -> Unit,
    destructive: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    val background = if (destructive) {
        VantafynGradients.accentHorizontal()
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                VantafynColors.SurfaceHigh.copy(alpha = 0.74f),
                Color.White.copy(alpha = 0.05f),
            ),
        )
    }
    val border = if (destructive) {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.34f),
                Color.White.copy(alpha = 0.12f),
            ),
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                VantafynColors.Muted.copy(alpha = 0.14f),
            ),
        )
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color(0xFFF8FAFF),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun VantafynAttentionPanel(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    VantafynGlassModalPanel(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 520.dp)
            .vantafynAnimatedModalBorder(cornerRadius = 26.dp, strokeWidth = 1.25.dp),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(VantafynGradients.accentHorizontal()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        title,
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        message,
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                    )
                }
            }
            actions()
        }
    }
}

@Composable
private fun CompactSectionTitle(
    text: String,
    icon: ImageVector? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(22.dp))
        }
        Text(
            text,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> PreferenceChipGroup(
    items: List<T>,
    label: (T) -> String,
    selected: (T) -> Boolean,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
    ) {
        items.forEach { item ->
            SelectableChip(label(item), selected(item)) { onSelect(item) }
        }
    }
}

@Composable
private fun PlaybackPreferenceToggle(
    title: String,
    checked: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowIcon(icon)
            Text(
                title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            VantafynPremiumSwitchVisual(checked = checked)
        }
    }
}

private val LanguageOptions = listOf(
    "" to "No preference",
    "eng" to "English",
    "spa" to "Spanish",
    "fre" to "French",
    "ger" to "German",
    "ita" to "Italian",
    "por" to "Portuguese",
    "jpn" to "Japanese",
    "chi" to "Chinese",
    "kor" to "Korean",
    "hin" to "Hindi",
    "ara" to "Arabic",
    "rus" to "Russian",
    "dut" to "Dutch",
    "swe" to "Swedish",
    "nor" to "Norwegian",
    "dan" to "Danish",
    "fin" to "Finnish",
    "pol" to "Polish",
    "tur" to "Turkish",
    "tha" to "Thai",
    "vie" to "Vietnamese",
    "ind" to "Indonesian",
    "may" to "Malay",
    "cze" to "Czech",
    "gre" to "Greek",
    "heb" to "Hebrew",
    "hun" to "Hungarian",
    "ron" to "Romanian",
    "ukr" to "Ukrainian",
    "bul" to "Bulgarian",
    "hrv" to "Croatian",
    "slv" to "Slovenian",
    "lit" to "Lithuanian",
    "lav" to "Latvian",
    "est" to "Estonian",
    "cat" to "Catalan",
    "tam" to "Tamil",
    "tel" to "Telugu",
    "urd" to "Urdu",
    "ben" to "Bengali",
    "pan" to "Punjabi",
    "swa" to "Swahili",
    "fil" to "Filipino",
    "nob" to "Norwegian Bokmål",
    "srp" to "Serbian",
    "slk" to "Slovak",
    "glg" to "Galician",
    "baq" to "Basque",
    "bre" to "Breton",
    "gle" to "Irish",
    "gla" to "Scottish Gaelic",
    "wel" to "Welsh",
    "bos" to "Bosnian",
    "alb" to "Albanian",
    "mkd" to "Macedonian",
    "arm" to "Armenian",
    "geo" to "Georgian",
    "kaz" to "Kazakh",
    "uzb" to "Uzbek",
    "aze" to "Azerbaijani",
    "nep" to "Nepali",
    "sin" to "Sinhala",
    "mya" to "Burmese",
    "khm" to "Khmer",
    "lao" to "Lao",
    "tgl" to "Tagalog",
    "hau" to "Hausa",
    "ibo" to "Igbo",
    "yor" to "Yoruba",
    "orm" to "Oromo",
    "som" to "Somali",
    "amh" to "Amharic",
    "tir" to "Tigrinya",
    "xho" to "Xhosa",
    "zul" to "Zulu",
    "afr" to "Afrikaans",
    "mlt" to "Maltese",
    "fao" to "Faroese",
    "isl" to "Icelandic",
    "lat" to "Latin",
    "ltz" to "Luxembourgish",
    "roh" to "Romansh",
    "cos" to "Corsican",
    "fry" to "Western Frisian",
    "ast" to "Asturian",
    "arg" to "Aragonese",
    "mar" to "Marathi",
    "guj" to "Gujarati",
    "kan" to "Kannada",
    "mal" to "Malayalam",
    "ori" to "Odia",
    "asm" to "Assamese",
    "yue" to "Cantonese",
)

@Composable
private fun PlaybackLanguageField(
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val normalizedValue = remember(value) { value.trim().lowercase(Locale.US) }
    val displayLabel = LanguageOptions.firstOrNull { it.first == normalizedValue }?.second
        ?: normalizedValue.ifBlank { placeholder.ifBlank { "No preference" } }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Box {
            VantafynGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                cornerRadius = 16.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        displayLabel,
                        color = if (normalizedValue.isBlank()) VantafynColors.Muted else VantafynColors.Ink,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(0.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 220.dp)
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(VantafynColors.Graphite.copy(alpha = 0.96f))
                                .vantafynAnimatedModalBorder(cornerRadius = 16.dp, strokeWidth = 1.5.dp)
                                .padding(vertical = 4.dp),
                        ) {
                            Column {
                                LanguageOptions.forEach { (code, name) ->
                                    val isSelected = code == normalizedValue
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                name,
                                                color = if (isSelected) VantafynColors.Primary else VantafynColors.Ink,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            )
                                        },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = VantafynColors.Primary, modifier = Modifier.size(18.dp)) }
                                        } else null,
                                        onClick = {
                                            onValueChange(code.trim().lowercase(Locale.US))
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
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
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowIcon(Icons.Rounded.PlayArrow)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(option.label, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(option.shortPlaybackLabel(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

private fun VantafynVideoPlayerPreference.shortPlaybackLabel(): String =
    when (this) {
        VantafynVideoPlayerPreference.Vantafyn -> "Built-in player"
        VantafynVideoPlayerPreference.External -> "Open with another app"
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

private data class BundledProfileAvatar(
    val index: Int,
    val resId: Int,
    val thumbResId: Int,
)

private class ProfileImagePickerState(
    val open: () -> Unit,
    val Content: @Composable () -> Unit,
)

@Composable
private fun rememberProfileImagePicker(onUpload: (ByteArray, String) -> Unit): ProfileImagePickerState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bundledAvatars = remember { bundledProfileAvatars() }
    var isOpen by remember { mutableStateOf(false) }
    var selectedAvatar by remember(bundledAvatars) { mutableStateOf(bundledAvatars.firstOrNull()) }
    var prepared by remember { mutableStateOf<PreparedProfileImage?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isPreparing = true
            localError = null
            prepared = prepareProfileImage(context, uri)
                .onFailure { localError = "Couldn't read that photo." }
                .getOrNull()
            if (prepared != null) selectedAvatar = null
            isPreparing = false
        }
    }
    return ProfileImagePickerState(
        open = {
            localError = null
            prepared = null
            selectedAvatar = bundledAvatars.firstOrNull()
            isOpen = true
        },
        Content = {
            when {
                isPreparing -> ProfileImageStatusCard(isSaving = true, error = null)
                localError != null -> ProfileImageStatusCard(isSaving = false, error = localError)
            }
            if (isOpen) {
                ProfileImagePickerDialog(
                    avatars = bundledAvatars,
                    selectedAvatar = selectedAvatar,
                    customImage = prepared,
                    isPreparing = isPreparing,
                    error = localError,
                    onSelectAvatar = {
                        selectedAvatar = it
                        prepared = null
                        localError = null
                    },
                    onPickCustom = {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onDismiss = { isOpen = false },
                    onConfirm = {
                        val upload = prepared ?: selectedAvatar?.let { avatar ->
                            readBundledProfileAvatar(context, avatar.resId)
                                .onFailure { localError = "Couldn't prepare that avatar." }
                                .getOrNull()
                        }
                        if (upload != null) {
                            isOpen = false
                            prepared = null
                            onUpload(upload.bytes, upload.mimeType)
                        }
                    },
                )
            }
        },
    )
}

@Composable
private fun ProfileImagePickerDialog(
    avatars: List<BundledProfileAvatar>,
    selectedAvatar: BundledProfileAvatar?,
    customImage: PreparedProfileImage?,
    isPreparing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSelectAvatar: (BundledProfileAvatar) -> Unit,
    onPickCustom: () -> Unit,
    onConfirm: () -> Unit,
) {
    val customBitmap = remember(customImage) {
        customImage?.let { BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size) }
    }
    val canConfirm = selectedAvatar != null || customImage != null
    AlertDialog(
        modifier = Modifier
            .imePadding()
            .vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        containerColor = VantafynModalContainerColor,
        titleContentColor = VantafynColors.Ink,
        textContentColor = VantafynColors.Muted,
        title = { Text("Choose Profile Picture") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
            ) {
                Text(
                    "Choose a Vantafyn avatar or use your own.",
                    color = VantafynColors.Muted,
                    textAlign = TextAlign.Center,
                )
                ProfileImageLivePreview(
                    avatar = selectedAvatar,
                    customBitmap = customBitmap,
                )
                if (error != null) {
                    Text(error, color = Color(0xFFFFC2C2), textAlign = TextAlign.Center)
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 420.dp) 4 else 3
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        gridItems(avatars, key = { it.index }) { avatar ->
                            BundledProfileAvatarTile(
                                avatar = avatar,
                                selected = selectedAvatar?.resId == avatar.resId && customImage == null,
                                onClick = { onSelectAvatar(avatar) },
                            )
                        }
                        item(key = "custom-photo") {
                            CustomProfilePhotoTile(
                                selected = customImage != null,
                                isPreparing = isPreparing,
                                onClick = onPickCustom,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdminMessageSecondaryButton(
                    text = "Cancel",
                    enabled = !isPreparing,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                VantafynButton(
                    text = "Use Picture",
                    onClick = onConfirm,
                    enabled = canConfirm && !isPreparing,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun ProfileImageLivePreview(
    avatar: BundledProfileAvatar?,
    customBitmap: Bitmap?,
) {
    Box(
        modifier = Modifier
            .size(136.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .border(
                border = BorderStroke(
                    width = 2.dp,
                    brush = VantafynGradients.accentHorizontal(),
                ),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            customBitmap != null -> Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = "Selected custom profile picture",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp)),
                contentScale = ContentScale.Crop,
            )
            avatar != null -> Image(
                painter = painterResource(avatar.thumbResId),
                contentDescription = "Selected profile picture ${avatar.index}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp)),
                contentScale = ContentScale.Crop,
            )
            else -> Icon(Icons.Rounded.Person, contentDescription = null, tint = VantafynColors.Muted, modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun BundledProfileAvatarTile(
    avatar: BundledProfileAvatar,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.8f),
        label = "profileAvatarScale",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.08f))
            .border(
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    brush = if (selected) {
                        VantafynGradients.accentHorizontal()
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.14f),
                            ),
                        )
                    },
                ),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(avatar.thumbResId),
            contentDescription = if (selected) "Profile picture ${avatar.index}, selected" else "Profile picture ${avatar.index}",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(999.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun CustomProfilePhotoTile(
    selected: Boolean,
    isPreparing: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.08f))
            .border(
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    brush = if (selected) {
                        VantafynGradients.accentHorizontal()
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.14f),
                            ),
                        )
                    },
                ),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(4.dp)
            .clickable(enabled = !isPreparing, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isPreparing) {
            VantafynGradientSpinner(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("+", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("Your photo", color = VantafynColors.Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
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
                VantafynGradientSpinner(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
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

private fun bundledProfileAvatars(): List<BundledProfileAvatar> =
    R.drawable::class.java.fields
        .mapNotNull { field ->
            val name = field.name
            val index = name.removePrefix("pp").toIntOrNull()
            if (index != null && name == "pp$index") {
                val thumbResId = runCatching {
                    R.drawable::class.java.getField("pp${index}_thumb").getInt(null)
                }.getOrDefault(field.getInt(null))
                BundledProfileAvatar(index = index, resId = field.getInt(null), thumbResId = thumbResId)
            } else {
                null
            }
        }
        .sortedBy { it.index }

private fun readBundledProfileAvatar(context: Context, resId: Int): Result<PreparedProfileImage> =
    runCatching {
        context.resources.openRawResource(resId).use { input ->
            PreparedProfileImage(bytes = input.readBytes(), mimeType = "image/png")
        }
    }

@Composable
private fun BottomRailAccentSettings(
    selected: BottomRailAccent,
    onSelect: (BottomRailAccent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowIcon(Icons.Rounded.AutoAwesome)
            Text(
                "Bottom rail accent",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        VantafynGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            variant = VantafynGlassVariant.Chip,
            cornerRadius = 999.dp,
            contentPadding = PaddingValues(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BottomRailAccent.entries.forEach { option ->
                    val isSelected = option == selected
                    val shape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isSelected) {
                                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                                } else {
                                    Modifier.clip(shape)
                                },
                            )
                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { onSelect(option) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option.label,
                            color = if (isSelected) VantafynColors.Ink else VantafynColors.Muted,
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
private fun HomeScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onRetry: () -> Unit,
    onSwitchUser: () -> Unit,
    onAddProfile: () -> Unit,
    onQuickConnect: () -> Unit,
    onDeviceQuickConnectCodeChanged: (String) -> Unit,
    onAuthorizeDeviceQuickConnect: () -> Unit,
    onConfirmLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    onLogoutCurrentProfile: () -> Unit,
    onNavigateMobile: (MobileDestination) -> Unit,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onReorderLibraries: (List<UUID>) -> Unit,
    onSetLibrariesViewMode: (LibrariesViewMode) -> Unit,
    onRetryLibrary: () -> Unit,
    onSetLibraryFilter: (JellyfinLibraryItemFilter) -> Unit,
    onSetLibraryAlphabet: (String?) -> Unit,
    onSetViewMode: (LibraryViewMode) -> Unit,
    onPreviousLibraryPage: () -> Unit,
    onNextLibraryPage: () -> Unit,
    onRefreshAdmin: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMarkWhatsNewSeen: () -> Unit,
    onToggleWhatsNew: () -> Unit,
    onRetryMedia: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLoadFavorites: () -> Unit,
    onPlaybackComingSoon: () -> Unit,
    onClearMessage: () -> Unit,
    onToggleHomeSection: (HomeSectionType) -> Unit,
    onMoveHomeSection: (HomeSectionType, Int) -> Unit,
    onResetHomeLayout: () -> Unit,
    onSaveHomeLayoutDraft: (List<HomeSectionPreference>, List<String>) -> Unit,
    onAddSmartRow: (String) -> Unit,
    onRemoveSmartRow: (String) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectThemeMusicVolume: (ThemeMusicVolume) -> Unit,
    onSetBottomRailAccent: (BottomRailAccent) -> Unit,
    onToggleAutoLoginLastProfile: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onSelectTheme: (VantafynThemePreset) -> Unit,
    onToggleMediaFavorite: () -> Unit,
    onToggleMediaPlayed: () -> Unit,
    onSetMediaFavorite: (java.util.UUID, Boolean) -> Unit,
    onSetMediaPlayed: (java.util.UUID, Boolean) -> Unit,
    onQueueMediaDownload: (java.util.UUID) -> Unit,
    onOpenDownloads: () -> Unit,
    onRefreshDownloads: () -> Unit,
    onPlayOfflineDownload: (DownloadRecord) -> Unit,
    onCancelDownload: (DownloadRecord) -> Unit,
    onRetryDownload: (DownloadRecord) -> Unit,
    onRemoveDownload: (DownloadRecord) -> Unit,
    onRemoveAllDownloads: () -> Unit,
    onSetDownloadWifiOnlyDefault: (Boolean) -> Unit,
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
    onPlayPreviousEpisode: (UpNextCandidate, Long) -> Unit,
    onPlayerError: () -> Unit,
    onPrepareCastPlayback: (Long) -> Unit,
    onSelectPlaybackAudioTrack: (Int, Long) -> Unit,
    onSelectPlaybackSubtitleTrack: (Int?, Long) -> Unit,
    onSyncPlayPause: (Long) -> Unit,
    onSyncPlayResume: (Long) -> Unit,
    onSyncPlaySeek: (Long) -> Unit,
    onStartLiveTvPlayback: (java.util.UUID, String, String?) -> Unit,
    onEditPlaybackPreferences: ((dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> Unit,
    onSavePlaybackPreferences: () -> Unit,
    onSetAutoplayCountdownSeconds: (Int) -> Unit,
    onSetUpNextDisplayMode: (UpNextDisplayMode) -> Unit,
    onTogglePassoutProtection: () -> Unit,
    onSetPassoutProtectionLimitMinutes: (Int) -> Unit,
    onSelectVideoPlayerPreference: (VantafynVideoPlayerPreference) -> Unit,
    onSetMaxStreamingBitrateMbps: (Int?) -> Unit,
    onSetMediaSegmentBehavior: (JellyfinMediaSegmentType, JellyfinMediaSegmentBehavior) -> Unit,
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
    onSetAdminSpeedLimit: (Int?) -> Unit,
    onSendAdminSessionMessage: (String, String?, String, Long) -> Unit,
    onSendAdminBroadcastMessage: (String?, String, Long) -> Unit,
    onClearAdminSessionMessageError: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAchievements: () -> Unit = {},
    onRetryAchievements: () -> Unit = {},
    onDismissAchievementUnlock: () -> Unit = {},
    onToggleAchievementsEnabled: () -> Unit = {},
    onToggleSocialEnabled: () -> Unit = {},
    onToggleSocialDockEnabled: () -> Unit = {},
    onDismissSocialDock: () -> Unit = {},
    onOpenSocial: () -> Unit = {},
    onOpenSocialPanel: () -> Unit = {},
    onCloseSocialPanel: () -> Unit = {},
    onOpenChatWithFriend: (dev.vantafyn.core.jellyfin.JellyfinFriend) -> Unit = {},
    onOpenChatFromConversation: (dev.vantafyn.core.jellyfin.JellyfinSocialConversation) -> Unit = {},
    onSendChatMessage: (String) -> Unit = {},
    onAcceptFriendRequest: (String) -> Unit = {},
    onDeclineFriendRequest: (String) -> Unit = {},
    onSendFriendRequest: (String) -> Unit = {},
    onRemoveFriend: (dev.vantafyn.core.jellyfin.JellyfinFriend) -> Unit = {},
    onBlockUser: (java.util.UUID, String, String, String?) -> Unit = { _, _, _, _ -> },
    onUnblockUser: (java.util.UUID) -> Unit = {},
    onDeleteConversation: (dev.vantafyn.core.jellyfin.JellyfinSocialConversation) -> Unit = {},
    onClearChatWithActivePeer: () -> Unit = {},
    onShareMediaToFriend: (dev.vantafyn.core.jellyfin.JellyfinFriend, dev.vantafyn.core.jellyfin.JellyfinMediaDetail) -> Unit = { _, _ -> },
    onDismissSocialIslandPreview: () -> Unit = {},
    onSetActiveSocialTab: (dev.vantafyn.feature.home.SocialTab) -> Unit = {},
    onRefreshSocial: () -> Unit = {},
    onSearchChatMedia: (String) -> Unit = {},
    onRefreshChatMessages: () -> Unit = {},
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
            onDeviceQuickConnectCodeChanged = onDeviceQuickConnectCodeChanged,
            onAuthorizeDeviceQuickConnect = onAuthorizeDeviceQuickConnect,
            onConfirmLogout = onConfirmLogout,
            onCancelLogout = onCancelLogout,
            onLogoutCurrentProfile = onLogoutCurrentProfile,
            onNavigate = onNavigateMobile,
            onOpenAchievements = onOpenAchievements,
            onRetryAchievements = onRetryAchievements,
            onDismissAchievementUnlock = onDismissAchievementUnlock,
            onToggleAchievementsEnabled = onToggleAchievementsEnabled,
            onToggleSocialEnabled = onToggleSocialEnabled,
            onToggleSocialDockEnabled = onToggleSocialDockEnabled,
            onDismissSocialDock = onDismissSocialDock,
            onOpenSocial = onOpenSocial,
            onOpenSocialPanel = onOpenSocialPanel,
            onCloseSocialPanel = onCloseSocialPanel,
            onOpenChatWithFriend = onOpenChatWithFriend,
            onOpenChatFromConversation = onOpenChatFromConversation,
            onSendChatMessage = onSendChatMessage,
            onAcceptFriendRequest = onAcceptFriendRequest,
            onDeclineFriendRequest = onDeclineFriendRequest,
            onSendFriendRequest = onSendFriendRequest,
            onRemoveFriend = onRemoveFriend,
            onBlockUser = onBlockUser,
            onUnblockUser = onUnblockUser,
            onDeleteConversation = onDeleteConversation,
            onClearChatWithActivePeer = onClearChatWithActivePeer,
            onShareMediaToFriend = onShareMediaToFriend,
            onDismissSocialIslandPreview = onDismissSocialIslandPreview,
            onSetActiveSocialTab = onSetActiveSocialTab,
            onRefreshSocial = onRefreshSocial,
            onRefreshChatMessages = onRefreshChatMessages,
            onOpenLibrary = onOpenLibrary,
            onReorderLibraries = onReorderLibraries,
            onSetLibrariesViewMode = onSetLibrariesViewMode,
            onRetryLibrary = onRetryLibrary,
            onSetLibraryFilter = onSetLibraryFilter,
            onSetLibraryAlphabet = onSetLibraryAlphabet,
            onSetViewMode = onSetViewMode,
            onPreviousLibraryPage = onPreviousLibraryPage,
            onNextLibraryPage = onNextLibraryPage,
            onRefreshAdmin = onRefreshAdmin,
            onOpenMedia = onOpenMedia,
            onMarkWhatsNewSeen = onMarkWhatsNewSeen,
            onToggleWhatsNew = onToggleWhatsNew,
            onRetryMedia = onRetryMedia,
            onSearchQueryChanged = onSearchQueryChanged,
            onLoadFavorites = onLoadFavorites,
            onPlaybackComingSoon = onPlaybackComingSoon,
            onClearMessage = onClearMessage,
            onToggleHomeSection = onToggleHomeSection,
            onMoveHomeSection = onMoveHomeSection,
            onResetHomeLayout = onResetHomeLayout,
            onSaveHomeLayoutDraft = onSaveHomeLayoutDraft,
            onAddSmartRow = onAddSmartRow,
            onRemoveSmartRow = onRemoveSmartRow,
            onCycleArtwork = onCycleArtwork,
            onCycleShape = onCycleShape,
            onCycleSize = onCycleSize,
            onCycleSpacing = onCycleSpacing,
            onToggleThemeMusic = onToggleThemeMusic,
            onSelectThemeMusicVolume = onSelectThemeMusicVolume,
            onSetBottomRailAccent = onSetBottomRailAccent,
            onToggleAutoLoginLastProfile = onToggleAutoLoginLastProfile,
            onSelectBackground = onSelectBackground,
            onSelectTheme = onSelectTheme,
            onToggleMediaFavorite = onToggleMediaFavorite,
            onToggleMediaPlayed = onToggleMediaPlayed,
            onSetMediaFavorite = onSetMediaFavorite,
            onSetMediaPlayed = onSetMediaPlayed,
            onQueueMediaDownload = onQueueMediaDownload,
            onOpenDownloads = onOpenDownloads,
            onRefreshDownloads = onRefreshDownloads,
            onPlayOfflineDownload = onPlayOfflineDownload,
            onCancelDownload = onCancelDownload,
            onRetryDownload = onRetryDownload,
            onRemoveDownload = onRemoveDownload,
            onRemoveAllDownloads = onRemoveAllDownloads,
            onSetDownloadWifiOnlyDefault = onSetDownloadWifiOnlyDefault,
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
            onPlayPreviousEpisode = onPlayPreviousEpisode,
            onPlayerError = onPlayerError,
            onPrepareCastPlayback = onPrepareCastPlayback,
            onSelectPlaybackAudioTrack = onSelectPlaybackAudioTrack,
            onSelectPlaybackSubtitleTrack = onSelectPlaybackSubtitleTrack,
            onSyncPlayPause = onSyncPlayPause,
            onSyncPlayResume = onSyncPlayResume,
            onSyncPlaySeek = onSyncPlaySeek,
            onStartLiveTvPlayback = onStartLiveTvPlayback,
            onEditPlaybackPreferences = onEditPlaybackPreferences,
            onSavePlaybackPreferences = onSavePlaybackPreferences,
            onSetAutoplayCountdownSeconds = onSetAutoplayCountdownSeconds,
            onSetUpNextDisplayMode = onSetUpNextDisplayMode,
            onTogglePassoutProtection = onTogglePassoutProtection,
            onSetPassoutProtectionLimitMinutes = onSetPassoutProtectionLimitMinutes,
            onSelectVideoPlayerPreference = onSelectVideoPlayerPreference,
            onSetMaxStreamingBitrateMbps = onSetMaxStreamingBitrateMbps,
            onSetMediaSegmentBehavior = onSetMediaSegmentBehavior,
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
            onSetAdminSpeedLimit = onSetAdminSpeedLimit,
            onSendAdminSessionMessage = onSendAdminSessionMessage,
            onSendAdminBroadcastMessage = onSendAdminBroadcastMessage,
            onClearAdminSessionMessageError = onClearAdminSessionMessageError,
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
                item { LibraryGridSkeleton() }
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
            items(state.libraries, key = { it.id }) { library ->
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
    onDeviceQuickConnectCodeChanged: (String) -> Unit,
    onAuthorizeDeviceQuickConnect: () -> Unit,
    onConfirmLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    onLogoutCurrentProfile: () -> Unit,
    onNavigate: (MobileDestination) -> Unit,
    onOpenAchievements: () -> Unit = {},
    onRetryAchievements: () -> Unit = {},
    onDismissAchievementUnlock: () -> Unit = {},
    onToggleAchievementsEnabled: () -> Unit = {},
    onToggleSocialEnabled: () -> Unit = {},
    onToggleSocialDockEnabled: () -> Unit = {},
    onDismissSocialDock: () -> Unit = {},
    onOpenSocial: () -> Unit = {},
    onOpenSocialPanel: () -> Unit = {},
    onCloseSocialPanel: () -> Unit = {},
    onOpenChatWithFriend: (dev.vantafyn.core.jellyfin.JellyfinFriend) -> Unit = {},
    onOpenChatFromConversation: (dev.vantafyn.core.jellyfin.JellyfinSocialConversation) -> Unit = {},
    onSendChatMessage: (String) -> Unit = {},
    onAcceptFriendRequest: (String) -> Unit = {},
    onDeclineFriendRequest: (String) -> Unit = {},
    onSendFriendRequest: (String) -> Unit = {},
    onRemoveFriend: (dev.vantafyn.core.jellyfin.JellyfinFriend) -> Unit = {},
    onBlockUser: (java.util.UUID, String, String, String?) -> Unit = { _, _, _, _ -> },
    onUnblockUser: (java.util.UUID) -> Unit = {},
    onDeleteConversation: (dev.vantafyn.core.jellyfin.JellyfinSocialConversation) -> Unit = {},
    onClearChatWithActivePeer: () -> Unit = {},
    onShareMediaToFriend: (dev.vantafyn.core.jellyfin.JellyfinFriend, dev.vantafyn.core.jellyfin.JellyfinMediaDetail) -> Unit = { _, _ -> },
    onDismissSocialIslandPreview: () -> Unit = {},
    onSetActiveSocialTab: (dev.vantafyn.feature.home.SocialTab) -> Unit = {},
    onRefreshSocial: () -> Unit = {},
    onSearchChatMedia: (String) -> Unit = {},
    onRefreshChatMessages: () -> Unit = {},
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onReorderLibraries: (List<UUID>) -> Unit,
    onSetLibrariesViewMode: (LibrariesViewMode) -> Unit,
    onRetryLibrary: () -> Unit,
    onSetLibraryFilter: (JellyfinLibraryItemFilter) -> Unit,
    onSetLibraryAlphabet: (String?) -> Unit,
    onSetViewMode: (LibraryViewMode) -> Unit,
    onPreviousLibraryPage: () -> Unit,
    onNextLibraryPage: () -> Unit,
    onRefreshAdmin: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMarkWhatsNewSeen: () -> Unit,
    onToggleWhatsNew: () -> Unit,
    onRetryMedia: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLoadFavorites: () -> Unit,
    onPlaybackComingSoon: () -> Unit,
    onClearMessage: () -> Unit,
    onToggleHomeSection: (HomeSectionType) -> Unit,
    onMoveHomeSection: (HomeSectionType, Int) -> Unit,
    onResetHomeLayout: () -> Unit,
    onSaveHomeLayoutDraft: (List<HomeSectionPreference>, List<String>) -> Unit,
    onAddSmartRow: (String) -> Unit,
    onRemoveSmartRow: (String) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectThemeMusicVolume: (ThemeMusicVolume) -> Unit,
    onSetBottomRailAccent: (BottomRailAccent) -> Unit,
    onToggleAutoLoginLastProfile: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onSelectTheme: (VantafynThemePreset) -> Unit,
    onToggleMediaFavorite: () -> Unit,
    onToggleMediaPlayed: () -> Unit,
    onSetMediaFavorite: (java.util.UUID, Boolean) -> Unit,
    onSetMediaPlayed: (java.util.UUID, Boolean) -> Unit,
    onQueueMediaDownload: (java.util.UUID) -> Unit,
    onOpenDownloads: () -> Unit,
    onRefreshDownloads: () -> Unit,
    onPlayOfflineDownload: (DownloadRecord) -> Unit,
    onCancelDownload: (DownloadRecord) -> Unit,
    onRetryDownload: (DownloadRecord) -> Unit,
    onRemoveDownload: (DownloadRecord) -> Unit,
    onRemoveAllDownloads: () -> Unit,
    onSetDownloadWifiOnlyDefault: (Boolean) -> Unit,
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
    onPlayPreviousEpisode: (UpNextCandidate, Long) -> Unit,
    onPlayerError: () -> Unit,
    onPrepareCastPlayback: (Long) -> Unit,
    onSelectPlaybackAudioTrack: (Int, Long) -> Unit,
    onSelectPlaybackSubtitleTrack: (Int?, Long) -> Unit,
    onSyncPlayPause: (Long) -> Unit,
    onSyncPlayResume: (Long) -> Unit,
    onSyncPlaySeek: (Long) -> Unit,
    onStartLiveTvPlayback: (java.util.UUID, String, String?) -> Unit,
    onEditPlaybackPreferences: ((dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> Unit,
    onSavePlaybackPreferences: () -> Unit,
    onSetAutoplayCountdownSeconds: (Int) -> Unit,
    onSetUpNextDisplayMode: (UpNextDisplayMode) -> Unit,
    onTogglePassoutProtection: () -> Unit,
    onSetPassoutProtectionLimitMinutes: (Int) -> Unit,
    onSelectVideoPlayerPreference: (VantafynVideoPlayerPreference) -> Unit,
    onSetMaxStreamingBitrateMbps: (Int?) -> Unit,
    onSetMediaSegmentBehavior: (JellyfinMediaSegmentType, JellyfinMediaSegmentBehavior) -> Unit,
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
    onSetAdminSpeedLimit: (Int?) -> Unit,
    onSendAdminSessionMessage: (String, String?, String, Long) -> Unit,
    onSendAdminBroadcastMessage: (String?, String, Long) -> Unit,
    onClearAdminSessionMessageError: () -> Unit,
    onNavigateBack: () -> Unit,
    notificationPermissionState: VantafynPermissionUiState,
    onRequestMusicControlsPermission: ((() -> Unit) -> Unit),
    onNotificationPermissionSettingsAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mediaActionTarget by remember { mutableStateOf<MediaActionTarget?>(null) }
    var showMusicQuickPlayer by remember { mutableStateOf(false) }
    var homeEditorOpen by remember { mutableStateOf(false) }
    var homeEditorTarget by remember { mutableStateOf<HomeLayoutEditorTarget?>(null) }
    var homeEditorAddRowsOpen by remember { mutableStateOf(false) }
    var homeEditorPendingCloseDiscard by remember { mutableStateOf<Boolean?>(null) }
    var selectedHomeSectionName by rememberSaveable { mutableStateOf<String?>(null) }
    var draftHomeLayout by remember { mutableStateOf<List<HomeSectionPreference>?>(null) }
    var draftSmartRows by remember { mutableStateOf<List<String>?>(null) }
    var lastPlayerExitTime by remember { mutableLongStateOf(0L) }
    var wasInPlayer by remember { mutableStateOf(false) }
    var isIslandBannerVisible by remember { mutableStateOf(false) }
    var currentDisplayPreview by remember { mutableStateOf<dev.vantafyn.core.jellyfin.JellyfinSocialMessage?>(null) }

    LaunchedEffect(state.mobileDestination) {
        if (state.mobileDestination == MobileDestination.Player) {
            wasInPlayer = true
        } else if (wasInPlayer) {
            wasInPlayer = false
            lastPlayerExitTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(state.activeSocialIslandPreview, state.mobileDestination) {
        val preview = state.activeSocialIslandPreview
        if (preview != null && state.mobileDestination != MobileDestination.Player && state.mobileDestination != MobileDestination.Chat) {
            val timeSincePlayerExit = System.currentTimeMillis() - lastPlayerExitTime
            if (lastPlayerExitTime > 0L && timeSincePlayerExit < 2000L) {
                // Graceful 2 second cooldown after exiting watching content
                delay(2000L - timeSincePlayerExit)
            }
            if (state.mobileDestination != MobileDestination.Player && state.mobileDestination != MobileDestination.Chat) {
                currentDisplayPreview = preview
                isIslandBannerVisible = true
                delay(5000L) // Show notification for 5 seconds
                isIslandBannerVisible = false
                delay(400L) // Allow exit animation to finish
                currentDisplayPreview = null
                onDismissSocialIslandPreview()
            }
        } else {
            isIslandBannerVisible = false
            currentDisplayPreview = null
        }
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val musicController = remember(context) { MusicPlaybackController.get(context) }
    val musicPlayback by musicController.state.collectAsStateWithLifecycle()
    fun closeHomeEditor(discard: Boolean) {
        homeEditorTarget = null
        homeEditorAddRowsOpen = false
        selectedHomeSectionName = null
        homeEditorOpen = false
        homeEditorPendingCloseDiscard = discard
    }
    fun openHomeEditor() {
        homeEditorPendingCloseDiscard = null
        draftHomeLayout = state.homeLayout
        draftSmartRows = state.configuredSmartRows
        selectedHomeSectionName = state.homeLayout
            .sortedBy { it.order }
            .firstOrNull { it.type != HomeSectionType.MediaBar }
            ?.type
            ?.name
        homeEditorTarget = null
        homeEditorAddRowsOpen = false
        homeEditorOpen = true
    }
    LaunchedEffect(homeEditorOpen, homeEditorPendingCloseDiscard) {
        val pendingDiscard = homeEditorPendingCloseDiscard ?: return@LaunchedEffect
        if (!homeEditorOpen) {
            delay(580L)
            if (!homeEditorOpen && homeEditorPendingCloseDiscard == pendingDiscard) {
                draftHomeLayout = null
                draftSmartRows = null
                homeEditorPendingCloseDiscard = null
            }
        }
    }
    val handlesSystemBack = state.mobileDestination != MobileDestination.Home ||
        state.confirmLogout ||
        state.mobileMessage != null ||
        showMusicQuickPlayer ||
        homeEditorOpen
    BackHandler(enabled = handlesSystemBack) {
        when {
            homeEditorTarget != null -> homeEditorTarget = null
            homeEditorAddRowsOpen -> homeEditorAddRowsOpen = false
            homeEditorOpen -> closeHomeEditor(discard = true)
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
                    homeLayoutOverride = draftHomeLayout,
                    configuredSmartRowsOverride = draftSmartRows,
                    highlightedHomeSection = selectedHomeSectionName?.let { name ->
                        runCatching { HomeSectionType.valueOf(name) }.getOrNull()
                    },
                    onRetry = onRetryHome,
                    onSearch = { onNavigate(MobileDestination.Search) },
                    onProfile = { onNavigate(MobileDestination.Profile) },
                    onOpenAchievements = onOpenAchievements,
                    onOpenLibrary = { library ->
                        if (!homeEditorOpen) onOpenLibrary(library)
                    },
                    onOpenMedia = { mediaId ->
                        if (!homeEditorOpen) onOpenMedia(mediaId)
                    },
                    onMediaLongPress = { target ->
                        if (!homeEditorOpen) mediaActionTarget = target
                    },
                    onHeroLongPress = { item ->
                        if (!homeEditorOpen) {
                            mediaActionTarget = item.toMediaActionTarget(allowHomeCustomize = true)
                        }
                    },
                    onStartLiveTvPlayback = { channelId, channelName, programName ->
                        if (!homeEditorOpen) onStartLiveTvPlayback(channelId, channelName, programName)
                    },
                    onPlaybackComingSoon = {
                        if (!homeEditorOpen) onPlaybackComingSoon()
                    },
                )
                MobileDestination.Requests -> RequestsScreen(session = state.session, onOpenMedia = onOpenMedia)
                MobileDestination.Downloads -> DownloadsScreen(
                    state = state,
                    onBack = onNavigateBack,
                    onRefresh = onRefreshDownloads,
                    onPlay = onPlayOfflineDownload,
                    onCancel = onCancelDownload,
                    onRetry = onRetryDownload,
                    onRemove = onRemoveDownload,
                    onRemoveAll = onRemoveAllDownloads,
                    onSetWifiOnlyDefault = onSetDownloadWifiOnlyDefault,
                )
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
                    onQueueDownload = onQueueMediaDownload,
                    onStartWatchParty = onStartWatchPartyFromDetail,
                    onShareMediaToFriend = onShareMediaToFriend,
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
                            onPlayPrevious = onPlayPreviousEpisode,
                            onPlayerError = onPlayerError,
                            onPrepareCastPlayback = onPrepareCastPlayback,
                            onSelectAudioTrack = onSelectPlaybackAudioTrack,
                            onSelectSubtitleTrack = onSelectPlaybackSubtitleTrack,
                            onSyncPlayPause = onSyncPlayPause,
                            onSyncPlayResume = onSyncPlayResume,
                            onSyncPlaySeek = onSyncPlaySeek,
                            syncPlaybackCommand = state.watchPartyPlaybackCommand,
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
                        MobileDestination.Libraries -> LibrariesScreen(state, onOpenLibrary, onReorderLibraries, onSetLibrariesViewMode)
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
                            onOpenSettings = { onNavigate(MobileDestination.Profile) },
                            onCreateUser = onCreateAdminUser,
                            onRefresh = onRefreshAdmin,
                            onScanLibrary = onScanAdminLibrary,
                            onRunTask = onRunAdminTask,
                            onStopTask = onStopAdminTask,
                            onSetSpeedLimit = onSetAdminSpeedLimit,
                            onSendSessionMessage = onSendAdminSessionMessage,
                            onSendBroadcastMessage = onSendAdminBroadcastMessage,
                            onClearSessionMessageError = onClearAdminSessionMessageError,
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
                            onDownloads = onOpenDownloads,
                            onWatchParty = { onNavigate(MobileDestination.WatchParty) },
                            onHomeLayout = { onNavigate(MobileDestination.HomeLayout) },
                            onPlaybackPreferences = { onNavigate(MobileDestination.PlaybackPreferences) },
                            onToggleThemeMusic = onToggleThemeMusic,
                            onSelectThemeMusicVolume = onSelectThemeMusicVolume,
                            onSetBottomRailAccent = onSetBottomRailAccent,
                            onToggleAutoLoginLastProfile = onToggleAutoLoginLastProfile,
                            onSwitchUser = onSwitchUser,
                            onAddProfile = onAddProfile,
                            onSendTextToTv = { onNavigate(MobileDestination.TvInput) },
                            onQuickConnect = onQuickConnect,
                            onLogout = onConfirmLogout,
                            onSelectBackground = onSelectBackground,
                            onSelectTheme = onSelectTheme,
                            onChangePassword = onChangePassword,
                            onUploadProfileImage = onUploadCurrentProfileImage,
                            onDeleteProfileImage = onDeleteCurrentProfileImage,
                            notificationPermissionState = notificationPermissionState,
                            onNotificationPermissionAction = onNotificationPermissionSettingsAction,
                            onOpenMedia = onOpenMedia,
                            onMarkWhatsNewSeen = onMarkWhatsNewSeen,
                            onToggleWhatsNew = onToggleWhatsNew,
                            onToggleAchievementsEnabled = onToggleAchievementsEnabled,
                            onToggleSocialEnabled = onToggleSocialEnabled,
                            onToggleSocialDockEnabled = onToggleSocialDockEnabled,
                            onDiscoverVantafyn = { onNavigate(MobileDestination.DiscoverVantafyn) },
                        )
                        MobileDestination.DeviceQuickConnect -> DeviceQuickConnectScreen(
                            state = state,
                            onBack = onNavigateBack,
                            onCodeChanged = onDeviceQuickConnectCodeChanged,
                            onAuthorize = onAuthorizeDeviceQuickConnect,
                        )
                        MobileDestination.TvInput -> dev.vantafyn.feature.home.remoteinput.MobileSendTextToTvScreen(
                            state = state,
                            onBack = onNavigateBack,
                        )
                        MobileDestination.PlaybackPreferences -> PlaybackPreferencesScreen(
                            state = state,
                            onBack = onNavigateBack,
                            onEdit = onEditPlaybackPreferences,
                            onSave = onSavePlaybackPreferences,
                            onSetAutoplayCountdownSeconds = onSetAutoplayCountdownSeconds,
                            onSetUpNextDisplayMode = onSetUpNextDisplayMode,
                            onTogglePassoutProtection = onTogglePassoutProtection,
                            onSetPassoutProtectionLimitMinutes = onSetPassoutProtectionLimitMinutes,
                            onSelectVideoPlayerPreference = onSelectVideoPlayerPreference,
                            onSetMaxStreamingBitrateMbps = onSetMaxStreamingBitrateMbps,
                            onSetMediaSegmentBehavior = onSetMediaSegmentBehavior,
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
                            onSetAlphabet = onSetLibraryAlphabet,
                            onSetViewMode = onSetViewMode,
                            onPreviousPage = onPreviousLibraryPage,
                            onNextPage = onNextLibraryPage,
                            onOpenMedia = onOpenMedia,
                            onMediaLongPress = { mediaActionTarget = it },
                            onStartLiveTvPlayback = onStartLiveTvPlayback,
                            onPlaybackComingSoon = onPlaybackComingSoon,
                        )
                        MobileDestination.DiscoverVantafyn -> DiscoverVantafynScreen(
                            isAdmin = state.session?.user?.isAdministrator == true,
                            onBack = onNavigateBack,
                            onDeepLink = { action ->
                                when (action) {
                                    "open_settings" -> onNavigate(MobileDestination.Profile)
                                    "open_playback_preferences" -> onNavigate(MobileDestination.PlaybackPreferences)
                                    "open_home_layout" -> onNavigate(MobileDestination.HomeLayout)
                                    "open_watch_party" -> onNavigate(MobileDestination.WatchParty)
                                    "open_social" -> onNavigate(MobileDestination.Social)
                                    "open_achievements" -> onNavigate(MobileDestination.Achievements)
                                    "open_admin" -> onNavigate(MobileDestination.Admin)
                                    "open_send_text_to_tv" -> onNavigate(MobileDestination.TvInput)
                                    "open_pair_tv" -> onNavigate(MobileDestination.Profile)
                                    else -> Unit
                                }
                            },
                        )
                        MobileDestination.Achievements -> AchievementsScreen(
                            userName = state.session?.user?.name.orEmpty(),
                            userImageUrl = state.savedProfiles.firstOrNull { it.jellyfinUserId == state.session?.user?.id }?.imageUrl,
                            summary = state.achievementSummary,
                            achievements = state.achievements,
                            isLoading = state.isAchievementsLoading,
                            error = state.achievementError,
                            onBack = onNavigateBack,
                            onRetry = onRetryAchievements,
                        )
                        MobileDestination.Social -> SocialScreen(
                            friends = state.socialFriends,
                            requests = state.socialRequests,
                            conversations = state.socialConversations,
                            discoverableUsers = state.socialDiscoverableUsers,
                            isLoading = state.isSocialLoading,
                            onBack = onNavigateBack,
                            onRefresh = onRefreshSocial,
                            onOpenChat = onOpenChatWithFriend,
                            onOpenConversation = onOpenChatFromConversation,
                            onAcceptRequest = onAcceptFriendRequest,
                            onDeclineRequest = onDeclineFriendRequest,
                            onSendRequest = onSendFriendRequest,
                            onRemoveFriend = onRemoveFriend,
                            blockedUsers = state.socialBlockedUsers,
                            onBlockUser = onBlockUser,
                            onUnblockUser = onUnblockUser,
                            onDeleteConversation = onDeleteConversation,
                            selectedTab = state.activeSocialTab,
                            onSelectTab = onSetActiveSocialTab,
                        )
                        MobileDestination.Chat -> {
                            state.activeChatPeer?.let { peer ->
                                val chatMediaItems = remember(state.home, state.libraryItems, state.favorites) {
                                    val fromHome = state.home?.sections?.flatMap { it.items }.orEmpty()
                                    val fromLibrary = state.libraryItems.map { item ->
                                        dev.vantafyn.core.jellyfin.JellyfinMediaCard(
                                            id = item.id,
                                            title = item.title,
                                            subtitle = item.subtitle,
                                            year = item.year,
                                            itemType = item.itemType,
                                            imageUrl = item.imageUrl,
                                            backdropUrl = item.backdropUrl,
                                            thumbUrl = item.thumbUrl,
                                            logoUrl = item.logoUrl,
                                            progress = item.progress,
                                            shape = item.shape,
                                            isFavorite = item.isFavorite,
                                            isPlayed = item.isPlayed,
                                            unplayedItemCount = item.unplayedItemCount,
                                        )
                                    }
                                    val fromFavorites = state.favorites.map { item ->
                                        dev.vantafyn.core.jellyfin.JellyfinMediaCard(
                                            id = item.id,
                                            title = item.title,
                                            subtitle = item.subtitle,
                                            year = item.year,
                                            itemType = item.itemType,
                                            imageUrl = item.imageUrl,
                                            backdropUrl = item.backdropUrl,
                                            thumbUrl = item.thumbUrl,
                                            logoUrl = item.logoUrl,
                                            progress = item.progress,
                                            shape = item.shape,
                                            isFavorite = item.isFavorite,
                                            isPlayed = item.isPlayed,
                                            unplayedItemCount = item.unplayedItemCount,
                                        )
                                    }
                                    (fromHome + fromLibrary + fromFavorites).distinctBy { it.id }
                                }

                                ChatScreen(
                                    peer = peer,
                                    messages = state.activeChatMessages,
                                    isSending = state.isSendingChatMessage,
                                    errorMessage = state.chatErrorMessage,
                                    onBack = onNavigateBack,
                                    onSendMessage = onSendChatMessage,
                                    onRefresh = onRefreshChatMessages,
                                    onClearChat = onClearChatWithActivePeer,
                                    availableMedia = chatMediaItems,
                                    serverSearchResults = state.chatSearchResults,
                                    isServerSearching = state.isChatSearching,
                                    onSearchServerMedia = onSearchChatMedia,
                                    onOpenMedia = onOpenMedia,
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }
            val currentDraftLayout = draftHomeLayout
            val currentDraftSmartRows = draftSmartRows
            val reducedHomeEditorMotion = rememberReducedMotionPreference()
            val homeEditorEnterEasing = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }
            val homeEditorExitEasing = remember { CubicBezierEasing(0.32f, 0f, 0.2f, 1f) }
            AnimatedVisibility(
                visible = homeEditorOpen && state.mobileDestination == MobileDestination.Home && currentDraftLayout != null && currentDraftSmartRows != null,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = if (reducedHomeEditorMotion) {
                    fadeIn(animationSpec = tween(durationMillis = 220, easing = homeEditorEnterEasing))
                } else {
                    slideInVertically(
                        initialOffsetY = { -it / 3 },
                        animationSpec = tween(durationMillis = 760, easing = homeEditorEnterEasing),
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 420, delayMillis = 70, easing = homeEditorEnterEasing),
                    ) + scaleIn(
                        initialScale = 0.985f,
                        animationSpec = tween(durationMillis = 760, easing = homeEditorEnterEasing),
                    )
                },
                exit = if (reducedHomeEditorMotion) {
                    fadeOut(animationSpec = tween(durationMillis = 180, easing = homeEditorExitEasing))
                } else {
                    slideOutVertically(
                        targetOffsetY = { -it / 4 },
                        animationSpec = tween(durationMillis = 520, easing = homeEditorExitEasing),
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 260, easing = homeEditorExitEasing),
                    ) + scaleOut(
                        targetScale = 0.99f,
                        animationSpec = tween(durationMillis = 520, easing = homeEditorExitEasing),
                    )
                },
            ) {
                if (currentDraftLayout != null && currentDraftSmartRows != null) {
                    HomeCustomizeEditorPanel(
                        state = state,
                        draftLayout = currentDraftLayout,
                        draftSmartRows = currentDraftSmartRows,
                        selectedType = selectedHomeSectionName?.let { name -> runCatching { HomeSectionType.valueOf(name) }.getOrNull() },
                        onSelectType = { selectedHomeSectionName = it.name },
                        onLayoutChanged = { draftHomeLayout = normalizeHomeLayoutDraft(it) },
                        onEditOption = { homeEditorTarget = it },
                        onAddRow = { homeEditorAddRowsOpen = true },
                        onCancel = { closeHomeEditor(discard = true) },
                        onDone = {
                            val layout = draftHomeLayout ?: state.homeLayout
                            val smartRows = draftSmartRows ?: state.configuredSmartRows
                            onSaveHomeLayoutDraft(normalizeHomeLayoutDraft(layout), smartRows)
                            closeHomeEditor(discard = false)
                        },
                        onResetDraft = {
                            draftHomeLayout = defaultHomeLayout()
                            draftSmartRows = emptyList()
                            selectedHomeSectionName = defaultHomeLayout()
                                .firstOrNull { it.type != HomeSectionType.MediaBar }
                                ?.type
                                ?.name
                        },
                    )
                }
            }
            val currentEditorTarget = homeEditorTarget
            val currentEditorLayout = draftHomeLayout
            val currentEditorPreference = if (currentEditorTarget != null && currentEditorLayout != null) {
                currentEditorLayout.firstOrNull { it.type == currentEditorTarget.type }
            } else {
                null
            }
            if (homeEditorOpen && currentEditorTarget != null && currentEditorPreference != null) {
                HomeLayoutOptionDialog(
                    preference = currentEditorPreference,
                    target = currentEditorTarget,
                    onDismiss = { homeEditorTarget = null },
                    onSelectArtwork = { option ->
                        draftHomeLayout = updateHomeDraftSection(currentEditorLayout.orEmpty(), currentEditorPreference.type) {
                            it.copy(artworkType = option)
                        }
                    },
                    onSelectShape = { option ->
                        draftHomeLayout = updateHomeDraftSection(currentEditorLayout.orEmpty(), currentEditorPreference.type) {
                            it.copy(cardShape = option)
                        }
                    },
                    onSelectSize = { option ->
                        draftHomeLayout = updateHomeDraftSection(currentEditorLayout.orEmpty(), currentEditorPreference.type) {
                            it.copy(cardSize = option)
                        }
                    },
                    onSelectSpacing = { option ->
                        draftHomeLayout = updateHomeDraftSection(currentEditorLayout.orEmpty(), currentEditorPreference.type) {
                            it.copy(spacing = option)
                        }
                    },
                )
            }
            if (homeEditorOpen && homeEditorAddRowsOpen && currentDraftSmartRows != null) {
                HomeAddRowsDialog(
                    state = state,
                    selectedRows = currentDraftSmartRows,
                    onDismiss = { homeEditorAddRowsOpen = false },
                    onApply = { rows ->
                        val normalizedRows = rows.filter { row -> row in supportedSmartRows }.distinct()
                        draftSmartRows = normalizedRows
                        draftHomeLayout = updateHomeDraftSection(draftHomeLayout ?: state.homeLayout, HomeSectionType.SmartRows) {
                            it.copy(visible = normalizedRows.isNotEmpty())
                        }
                        selectedHomeSectionName = HomeSectionType.SmartRows.name
                        homeEditorAddRowsOpen = false
                    },
                )
            }
            if (state.mobileDestination != MobileDestination.Player && state.mobileDestination != MobileDestination.Chat) {
                AnimatedVisibility(
                    visible = homeEditorOpen && state.mobileDestination == MobileDestination.Home,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(178.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.42f),
                                    ),
                                ),
                            ),
                    )
                }
                val railMode = when (state.mobileDestination) {
                    MobileDestination.Social -> NavigationRailMode.Social(state.activeSocialTab)
                    else -> NavigationRailMode.Main(state.mobileDestination.bottomNavRoot(state.previousMobileDestination))
                }

                MobileBottomNav(
                    mode = railMode,
                    onSelected = { destination ->
                        if (destination == MobileDestination.Music && showMusicQuickPlayer) {
                            showMusicQuickPlayer = false
                        } else {
                            if (homeEditorOpen) {
                                closeHomeEditor(discard = true)
                            }
                            showMusicQuickPlayer = false
                            onNavigate(destination)
                        }
                    },
                    onSocialTabSelected = onSetActiveSocialTab,
                    onMusicLongPress = if (state.mobileDestination != MobileDestination.Music) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMusicQuickPlayer = !showMusicQuickPlayer
                        }
                    } else {
                        null
                    },
                    isAdmin = state.session?.user?.isAdministrator == true,
                    isMusicPlaying = musicPlayback.isPlaying,
                    pendingOmbiAccessRequestCount = state.pendingOmbiAccessRequestCount,
                    unreadMessagesCount = state.socialConversations.sumOf { it.unreadCount },
                    incomingFriendRequestsCount = state.socialRequests.count { it.isIncoming },
                    accentMode = state.bottomRailAccent,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            val isMusicMiniPlayerVisible = state.mobileDestination == MobileDestination.Music && musicPlayback.currentTrack != null
            val socialDockBottomPadding by animateDpAsState(
                targetValue = if (isMusicMiniPlayerVisible) 200.dp else 86.dp,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "socialDockBottomPadding",
            )
            val socialIslandBottomPadding by animateDpAsState(
                targetValue = when {
                    showMusicQuickPlayer && state.mobileDestination != MobileDestination.Music -> 340.dp
                    isMusicMiniPlayerVisible -> 200.dp
                    else -> 86.dp
                },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "socialIslandBottomPadding",
            )

            AnimatedVisibility(
                visible = showMusicQuickPlayer && state.mobileDestination != MobileDestination.Music,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight + 120 },
                    animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight + 120 },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)),
            ) {
                MusicQuickPlayerSheet(
                    playback = musicPlayback,
                    controller = musicController,
                    session = state.session,
                    onDismiss = { showMusicQuickPlayer = false },
                    onOpenMusic = {
                        showMusicQuickPlayer = false
                        onNavigate(MobileDestination.Music)
                    },
                    modifier = Modifier,
                )
            }

            var isDockDragging by remember { mutableStateOf(false) }
            var isDockDismissHovered by remember { mutableStateOf(false) }

            AnimatedVisibility(
                visible = isDockDragging,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(bottom = 30.dp),
                enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.6f),
                exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.6f),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isDockDismissHovered) 58.dp else 48.dp)
                        .clip(CircleShape)
                        .background(if (isDockDismissHovered) Color(0xFFFF3366).copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.68f))
                        .border(
                            width = if (isDockDismissHovered) 2.dp else 1.2.dp,
                            color = if (isDockDismissHovered) Color(0xFFFF6688) else Color.White.copy(alpha = 0.25f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove chat bubble",
                        tint = Color.White,
                        modifier = Modifier.size(if (isDockDismissHovered) 24.dp else 18.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.socialEnabled &&
                    state.socialDockEnabled &&
                    state.isAchievementsAvailable &&
                    state.mobileDestination != MobileDestination.Player &&
                    state.mobileDestination != MobileDestination.Social &&
                    state.mobileDestination != MobileDestination.Chat &&
                    !homeEditorOpen &&
                    !showMusicQuickPlayer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(bottom = socialDockBottomPadding, end = 18.dp),
                enter = scaleIn(initialScale = 0.8f, animationSpec = tween(260, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(220)),
                exit = scaleOut(targetScale = 0.8f, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180)),
            ) {
                FloatingSocialDock(
                    unreadCount = state.socialUnreadCount,
                    incomingRequestCount = state.socialRequests.count { it.isIncoming },
                    onClick = onOpenSocialPanel,
                    onDismiss = onDismissSocialDock,
                    onDragStateChanged = { dragging, hovering ->
                        isDockDragging = dragging
                        isDockDismissHovered = hovering
                    },
                )
            }
        }
    }
}
    state.mobileMessage?.let { message ->
        VantafynToast(
            message = message,
            onDismiss = onClearMessage,
        )
    }
    state.activeAchievementUnlock?.let { unlock ->
        AchievementUnlockToast(
            unlock = unlock,
            onDismiss = onDismissAchievementUnlock,
            onClick = onOpenAchievements,
        )
    }
    AnimatedVisibility(
        visible = isIslandBannerVisible && currentDisplayPreview != null && state.mobileDestination != MobileDestination.Player && state.mobileDestination != MobileDestination.Chat,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ) + fadeIn(animationSpec = tween(350)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(350, easing = FastOutSlowInEasing),
        ) + fadeOut(animationSpec = tween(250)),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        currentDisplayPreview?.let { preview ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                SocialIslandBanner(
                    message = preview,
                    onDismiss = {
                        isIslandBannerVisible = false
                        onDismissSocialIslandPreview()
                    },
                    onClick = {
                        isIslandBannerVisible = false
                        onDismissSocialIslandPreview()
                        val friend = state.socialFriends.firstOrNull { it.userId == preview.senderId }
                            ?: dev.vantafyn.core.jellyfin.JellyfinFriend(
                                userId = preview.senderId,
                                username = preview.senderName,
                                displayName = preview.senderName,
                                avatarTag = preview.senderAvatarTag,
                                avatarUrl = preview.senderAvatarUrl,
                            )
                        onOpenChatWithFriend(friend)
                    },
                )
            }
        }
    }
    if (state.isSocialPanelOpen) {
        FloatingSocialPanel(
            friends = state.socialFriends,
            requests = state.socialRequests,
            conversations = state.socialConversations,
            onDismiss = onCloseSocialPanel,
            onOpenFullSocial = onOpenSocial,
            onOpenChatWithFriend = onOpenChatWithFriend,
            onOpenChatFromConversation = onOpenChatFromConversation,
            onAcceptRequest = onAcceptFriendRequest,
        )
    }
    if (state.confirmLogout) {
        AlertDialog(
            modifier = Modifier.vantafynAnimatedModalBorder(),
            onDismissRequest = onCancelLogout,
            containerColor = VantafynModalContainerColor,
            shape = RoundedCornerShape(28.dp),
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
            onDownload = {
                mediaActionTarget = null
                onQueueMediaDownload(target.id)
            },
            onCustomizeHome = if (target.allowHomeCustomize) {
                {
                    mediaActionTarget = null
                    openHomeEditor()
                }
            } else {
                null
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
    homeLayoutOverride: List<HomeSectionPreference>? = null,
    configuredSmartRowsOverride: List<String>? = null,
    highlightedHomeSection: HomeSectionType? = null,
    onRetry: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onOpenAchievements: () -> Unit = {},
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
    onHeroLongPress: (JellyfinHeroMediaItem) -> Unit = {},
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
    val homeRevealKey = state.session?.profileId.orEmpty()
    var hasPlayedHomeReveal by rememberSaveable(homeRevealKey) { mutableStateOf(false) }
    var homeInitialRevealActive by remember(homeRevealKey, state.isHomeLoading) {
        mutableStateOf(!state.isHomeLoading && !hasPlayedHomeReveal)
    }
    LaunchedEffect(homeRevealKey, state.isHomeLoading) {
        if (!state.isHomeLoading && !hasPlayedHomeReveal) {
            homeInitialRevealActive = true
            delay(1_450L)
            homeInitialRevealActive = false
            hasPlayedHomeReveal = true
        } else {
            homeInitialRevealActive = false
        }
    }
    val homeListState = rememberLazyListState()
    val effectiveHomeLayout = homeLayoutOverride ?: state.homeLayout
    val effectiveSmartRows = configuredSmartRowsOverride ?: state.configuredSmartRows
    val homeEditorPreviewActive = highlightedHomeSection != null
    val density = LocalDensity.current
    LaunchedEffect(state.isHomeLoading, homeRevealKey) {
        if (!state.isHomeLoading) {
            homeListState.scrollToItem(0)
        }
    }
    LaunchedEffect(highlightedHomeSection, effectiveHomeLayout, effectiveSmartRows, state.home, state.libraries, state.favorites) {
        val selected = highlightedHomeSection ?: return@LaunchedEffect
        val targetIndex = homePreviewListIndexFor(
            selectedType = selected,
            state = state,
            layout = effectiveHomeLayout,
            configuredSmartRows = effectiveSmartRows,
            hasHeader = hero.isNotEmpty() || state.isHomeLoading || showEmptyHome,
        ) ?: return@LaunchedEffect
        val viewportHeight = homeListState.layoutInfo.viewportSize.height
        val editorRevealOffset = if (viewportHeight > 0) {
            -(viewportHeight * 0.40f).toInt()
        } else {
            -with(density) { 300.dp.toPx().toInt() }
        }
        homeListState.animateScrollToItem(targetIndex.coerceAtLeast(0), scrollOffset = editorRevealOffset)
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (homeEditorPreviewActive) 180.dp else 2.dp,
                bottom = 118.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
            var revealIndex = 0
            when {
                hero.isNotEmpty() -> {
                    val index = revealIndex++
                    item(key = "home-hero") {
                        HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                            HeroCarousel(
                                items = hero,
                                onOpen = { onOpenMedia(it.id) },
                                onLongPress = onHeroLongPress,
                            )
                        }
                    }
                }
                state.isHomeLoading -> {
                    val index = revealIndex++
                    item(key = "home-hero-skeleton") {
                        HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                            HomeHeroSkeleton()
                        }
                    }
                }
                showEmptyHome -> {
                    val index = revealIndex++
                    item(key = "home-empty") {
                        HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                            HomeFallbackHero(state)
                        }
                    }
                }
            }
            effectiveHomeLayout
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
                                HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                                    HomeRowInset(highlighted = highlightedHomeSection == preference.type) {
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
                            HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                                HomeRowInset(highlighted = highlightedHomeSection == preference.type) {
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
                            HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                                HomeRowInset(highlighted = highlightedHomeSection == preference.type) {
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
                            HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                                HomeRowInset(highlighted = highlightedHomeSection == preference.type) {
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
                            HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                                HomeRowInset(highlighted = highlightedHomeSection == preference.type) {
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
                        val smartSections = smartRowsFor(state, effectiveSmartRows)
                        if (smartSections.isNotEmpty()) {
                            val index = revealIndex++
                            item(key = "home-smart-rows") {
                                HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                                    HomeRowInset(highlighted = highlightedHomeSection == preference.type) {
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
                                HomeContentReveal(index = index, animate = homeInitialRevealActive) {
                                HomeRowInset(highlighted = highlightedHomeSection == preference.type) { LibraryShowcaseRow("More Libraries", other, onOpenLibrary) }
                                }
                            }
                        }
                    }
                }
            }
            if (state.favorites.isNotEmpty()) {
                val index = revealIndex++
                item(key = "home-my-list") {
                    HomeContentReveal(index = index, animate = homeInitialRevealActive) {
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
        HomeScrollStatusBarScrim(
            listState = homeListState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 2.dp, end = VantafynSpacing.xl),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isAchievementsAvailable) {
                MobileHomeTrophyButton(
                    hasUnseenUnlocks = state.hasUnseenAchievements,
                    onClick = onOpenAchievements,
                )
            }
            MobileHomeProfileAvatar(
                state = state,
                onProfile = onProfile,
                hasUnseenWhatsNew = state.hasUnseenWhatsNew,
            )
        }
    }
}

@Composable
private fun HomeScrollStatusBarScrim(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val statusBarPx = WindowInsets.statusBars.getTop(density).toFloat()
    val fadeStartPx = statusBarPx + with(density) { 132.dp.toPx() }
    val fadeEndPx = statusBarPx + with(density) { 28.dp.toPx() }
    val targetAlpha by remember(listState, density) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val headerInfo = layoutInfo.visibleItemsInfo.firstOrNull {
                it.key == "home-hero" || it.key == "home-hero-skeleton" || it.key == "home-empty"
            }
            when {
                headerInfo != null -> {
                    val headerBottom = headerInfo.offset + headerInfo.size
                    ((fadeStartPx - headerBottom) / (fadeStartPx - fadeEndPx)).coerceIn(0f, 1f)
                }
                layoutInfo.visibleItemsInfo.any {
                    val key = it.key.toString()
                    key.startsWith("home-section-") ||
                        key == "home-smart-rows" ||
                        key == "home-more-libraries" ||
                        key == "home-my-list"
                } || listState.firstVisibleItemIndex > 0 -> 1f
                else -> 0f
            }
        }
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "homeStatusScrimAlpha",
    )
    HomeStatusBarScrim(alpha = alpha, statusBarHeight = statusBarHeight, modifier = modifier)
}

@Composable
private fun HomeStatusBarScrim(alpha: Float, statusBarHeight: Dp, modifier: Modifier = Modifier) {
    if (alpha <= 0.001f) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + 72.dp)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Black.copy(alpha = 0.74f * alpha),
                        0.42f to VantafynColors.Graphite.copy(alpha = 0.58f * alpha),
                        0.72f to VantafynColors.Graphite.copy(alpha = 0.20f * alpha),
                        1.00f to Color.Transparent,
                    ),
                ),
            ),
    )
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
private fun HomeRowInset(
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "homeRowHighlight",
    )
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .padding(horizontal = if (highlightAlpha > 0.001f) 18.dp else 8.dp)
            .then(
                if (highlightAlpha > 0.001f) {
                    Modifier
                        .clip(shape)
                        .background(VantafynColors.SurfaceHigh.copy(alpha = 0.045f * highlightAlpha))
                        .border(1.dp, VantafynColors.Secondary.copy(alpha = 0.28f * highlightAlpha), shape)
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

@Composable
private fun MobileHomeProfileAvatar(state: VantafynHomeUiState, onProfile: () -> Unit, hasUnseenWhatsNew: Boolean = false) {
    Box {
        Box(
            modifier = Modifier
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
        if (hasUnseenWhatsNew) {
            WhatsNewGradientDot(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp),
            )
        }
    }
}

@Composable
private fun MobileHomeTrophyButton(
    hasUnseenUnlocks: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .vantafynAnimatedModalBorder(cornerRadius = 15.dp, strokeWidth = 1.5.dp)
                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.76f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = "Achievements",
                tint = if (hasUnseenUnlocks) Color(0xFFFFD700) else VantafynColors.Ink,
                modifier = Modifier.size(22.dp),
            )
        }
        if (hasUnseenUnlocks) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD700)),
            )
        }
    }
}

@Composable
private fun GlassAction(
    text: String,
    onClick: () -> Unit = {},
    gradientBorder: Boolean = false,
) {
    VantafynGlassChip(
        modifier = if (gradientBorder) {
            Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.2.dp, durationMillis = 4200)
        } else {
            Modifier
        },
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
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 110.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.96f))
                .vantafynAnimatedModalBorder(cornerRadius = 22.dp, strokeWidth = 1.3.dp, durationMillis = 4000)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AchievementUnlockToast(
    unlock: JellyfinAchievementUnlock,
    onDismiss: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(unlock.id) {
        dev.vantafyn.core.ui.VantafynSoundEffects.playAchievementUnlocked(context)
        delay(5_000)
        onDismiss()
    }
    Popup(alignment = Alignment.TopCenter) {
        Box(
            modifier = modifier
                .swipeToDismissTopNotification(onDismiss)
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1F1A12).copy(alpha = 0.96f),
                            Color(0xFF141008).copy(alpha = 0.96f),
                        ),
                    ),
                )
                .vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.3.dp, durationMillis = 4000)
                .clickable {
                    onClick()
                    onDismiss()
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.50f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (unlock.iconUrl != null) {
                        AsyncImage(
                            model = unlock.iconUrl,
                            contentDescription = unlock.name,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "ACHIEVEMENT UNLOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFFFD700),
                        )
                        if (unlock.score > 0) {
                            Text(
                                text = "+${unlock.score} PTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.70f),
                            )
                        }
                    }
                    Text(
                        text = unlock.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (unlock.description.isNotBlank()) {
                        Text(
                            text = unlock.description,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = VantafynColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HeroCarousel(
    items: List<JellyfinHeroMediaItem>,
    onOpen: (JellyfinHeroMediaItem) -> Unit,
    onLongPress: (JellyfinHeroMediaItem) -> Unit = {},
) {
    val carouselItems = remember(items) {
        items.distinctBy { it.heroCarouselKey() }
    }
    if (carouselItems.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { carouselItems.size })
    val carouselKeys = remember(carouselItems) { carouselItems.map { it.id } }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(carouselKeys, lifecycleOwner) {
        if (carouselItems.size > 1) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(5_500)
                    val next = (pagerState.currentPage + 1) % carouselItems.size
                    pagerState.animateScrollToPage(next)
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            key = { page -> carouselItems[page].id },
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 0.dp,
        ) { page ->
            carouselItems.getOrNull(page)?.let { item ->
                CinematicHero(
                    item = item,
                    onOpen = { onOpen(item) },
                    onLongPress = { onLongPress(item) },
                    modifier = Modifier.fillMaxWidth(),
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
                val selected = index == pagerState.currentPage.coerceAtMost(5)
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
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(292.dp)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black,
                                0.58f to Color.Black,
                                0.78f to Color.Black.copy(alpha = 0.76f),
                                0.91f to Color.Black.copy(alpha = 0.24f),
                                1.00f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            AsyncImage(
                model = item.backdropUrl ?: item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(VantafynColors.Graphite),
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
                                0.52f to Color.Transparent,
                                0.80f to Color(0x48070A12),
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
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.92f)
                .padding(start = VantafynSpacing.xl, end = 54.dp, bottom = 34.dp),
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
                val titleLength = item.title.length
                Text(
                    item.title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = when {
                            titleLength > 54 -> 27.sp
                            titleLength > 36 -> 30.sp
                            else -> MaterialTheme.typography.headlineMedium.fontSize
                        },
                        lineHeight = when {
                            titleLength > 54 -> 31.sp
                            titleLength > 36 -> 34.sp
                            else -> MaterialTheme.typography.headlineMedium.lineHeight
                        },
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HeroRatingMetadata(
                values = listOfNotNull(
                    item.year?.toString(),
                    item.runtimeMinutes?.let { "${it}m" },
                    item.officialRating,
                ),
                rating = item.communityRating?.let { "★ ${"%.1f".format(it)}" },
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
    }
}

@Composable
private fun HomeFallbackHero(state: VantafynHomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF172037), Color(0xFF302A52), VantafynColors.Graphite)))
            .drawWithContent {
                drawContent()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF31D7FF).copy(alpha = 0.12f), Color(0xFF8B5CFF).copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.75f, size.height * 0.3f),
                        radius = size.width * 0.45f,
                    ),
                )
            }
            .padding(VantafynSpacing.lg),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))))
                    .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color(0xFF31D7FF).copy(alpha = 0.18f))), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Your library is ready", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("Add media to Jellyfin and Vantafyn will bring it to life here.", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            }
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
private fun LibraryGridSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        repeat(3) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
            ) {
                repeat(2) { index ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        VantafynSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if ((row + index) % 2 == 0) 214.dp else 150.dp),
                            cornerRadius = 18.dp,
                        )
                        VantafynSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .height(15.dp),
                        )
                        VantafynSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactListSkeleton(rows: Int = 4, leadingSize: Dp = 52.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rows) { index ->
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VantafynSkeletonBlock(
                        modifier = Modifier.size(leadingSize),
                        cornerRadius = 14.dp,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VantafynSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(if (index % 2 == 0) 0.82f else 0.64f)
                                .height(14.dp),
                        )
                        VantafynSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(if (index % 2 == 0) 0.48f else 0.56f)
                                .height(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        VantafynGlassCard(cornerRadius = 24.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                VantafynSkeletonBlock(Modifier.size(58.dp), cornerRadius = 18.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VantafynSkeletonBlock(Modifier.fillMaxWidth(0.62f).height(18.dp))
                    VantafynSkeletonBlock(Modifier.fillMaxWidth(0.42f).height(12.dp))
                }
            }
        }
        CompactListSkeleton(rows = 3, leadingSize = 46.dp)
    }
}

@Composable
private fun PlaybackPreferencesSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        repeat(3) {
            VantafynGlassCard(cornerRadius = 24.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    VantafynSkeletonBlock(Modifier.fillMaxWidth(0.46f).height(20.dp))
                    repeat(2) {
                        VantafynSkeletonBlock(Modifier.fillMaxWidth().height(66.dp), cornerRadius = 18.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) {
                            VantafynSkeletonBlock(Modifier.weight(1f).height(38.dp), cornerRadius = 999.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        VantafynSkeletonBlock(Modifier.fillMaxWidth().height(92.dp), cornerRadius = 24.dp)
        HomeSkeletonRow(0)
        HomeSkeletonRow(1)
    }
}

@Composable
private fun DetailLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        VantafynSkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            cornerRadius = 0.dp,
        )
        Column(
            modifier = Modifier.padding(horizontal = VantafynSpacing.md),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VantafynSkeletonBlock(Modifier.fillMaxWidth(0.78f).height(26.dp))
            VantafynSkeletonBlock(Modifier.fillMaxWidth(0.56f).height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { VantafynSkeletonBlock(Modifier.width(86.dp).height(34.dp), cornerRadius = 999.dp) }
            }
            VantafynSkeletonBlock(Modifier.fillMaxWidth().height(92.dp), cornerRadius = 18.dp)
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
                .background(VantafynSkeletonBrush()),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = VantafynSpacing.xl, bottom = 78.dp)
                .width(156.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(VantafynSkeletonBrush()),
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
private fun HeroRatingMetadata(values: List<String>, rating: String?) {
    if (values.isEmpty() && rating == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (values.isNotEmpty()) {
            Text(
                text = values.joinToString(" · "),
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        rating?.let {
            Text(
                text = it,
                color = VantafynColors.Gold,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
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
                    .background(VantafynSkeletonBrush()),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                items(4) {
                    Box(
                        modifier = Modifier
                            .width(if (index == 0) 210.dp else 142.dp)
                            .height(if (index == 0) 118.dp else 214.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(VantafynSkeletonBrush()),
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeContentReveal(
    index: Int,
    animate: Boolean,
    revealKey: Any? = index,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var progress by remember(revealKey, index, animate) { mutableFloatStateOf(if (animate) 0f else 1f) }
    LaunchedEffect(revealKey, index, animate) {
        if (animate) {
            progress = 0f
            delay((index.coerceAtMost(8) * 112L).coerceAtMost(780L))
            Animatable(0f).animateTo(1f, animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)) {
                progress = value
            }
        } else {
            progress = 1f
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * size.height / 8f
            },
    ) {
        content()
    }
}

@Composable
private fun ScreenRevealWrapper(
    key: Any?,
    content: @Composable () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(key) {
        progress = 0f
        Animatable(0f).animateTo(1f, animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)) {
            progress = value
        }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * size.height / 6f
        },
    ) {
        content()
    }
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
                    Text(library.name, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = rememberLifecycleAwareMarquee())
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

private fun previewImagesFor(
    state: VantafynHomeUiState,
    type: HomeSectionType,
    configuredSmartRows: List<String> = state.configuredSmartRows,
): List<String> =
    when (type) {
        HomeSectionType.MediaBar -> state.home?.heroItems.orEmpty().mapNotNull { it.backdropUrl ?: it.posterUrl }
        HomeSectionType.MyMedia -> mainLibraries(state.libraries).mapNotNull { it.imageUrl }
        HomeSectionType.ContinueWatching -> homeSection(state, "Continue")?.items.orEmpty().mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.RecentlyAddedMovies -> homeSection(state, "Movies")?.items.orEmpty().mapNotNull { it.imageUrl ?: it.backdropUrl }
        HomeSectionType.RecentlyAddedTv -> homeSection(state, "TV")?.items.orEmpty().mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.LiveTvChannels -> homeSection(state, "Live TV")?.items.orEmpty().mapNotNull { it.imageUrl ?: it.backdropUrl }
        HomeSectionType.SmartRows -> state.home?.sections.orEmpty()
            .filter { it.title in configuredSmartRows }
            .flatMap { it.items }
            .mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.OtherLibraries -> otherLibraries(state.libraries).mapNotNull { it.imageUrl }
    }

private fun smartRowsFor(
    state: VantafynHomeUiState,
    configuredSmartRows: List<String> = state.configuredSmartRows,
): List<JellyfinHomeSection> =
    state.home?.sections.orEmpty().filter {
        it.title in configuredSmartRows && it.items.isNotEmpty()
    }

private fun homePreviewListIndexFor(
    selectedType: HomeSectionType,
    state: VantafynHomeUiState,
    layout: List<HomeSectionPreference>,
    configuredSmartRows: List<String>,
    hasHeader: Boolean,
): Int? {
    if (selectedType == HomeSectionType.MediaBar) return 0.takeIf { hasHeader }
    var index = if (hasHeader) 1 else 0
    normalizeHomeLayoutDraft(layout)
        .filter { it.visible && it.type != HomeSectionType.MediaBar }
        .forEach { preference ->
            val isRendered = when (preference.type) {
                HomeSectionType.MediaBar -> false
                HomeSectionType.MyMedia -> mainLibraries(state.libraries).isNotEmpty()
                HomeSectionType.ContinueWatching -> homeSection(state, "Continue")?.items?.isNotEmpty() == true
                HomeSectionType.RecentlyAddedMovies -> homeSection(state, "Movies")?.items?.isNotEmpty() == true
                HomeSectionType.RecentlyAddedTv -> homeSection(state, "TV")?.items?.isNotEmpty() == true
                HomeSectionType.LiveTvChannels -> homeSection(state, "Live TV")?.items?.isNotEmpty() == true
                HomeSectionType.SmartRows -> smartRowsFor(state, configuredSmartRows).isNotEmpty()
                HomeSectionType.OtherLibraries -> otherLibraries(state.libraries).isNotEmpty()
            }
            if (preference.type == selectedType) return if (isRendered) index else null
            if (isRendered) index += 1
        }
    return null
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
            HomeContentReveal(index = index, animate = false) {
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
private fun homeResumeProgressBrush(): Brush = remember {
    Brush.horizontalGradient(
        VantafynGradients.AccentColors.map { it.copy(alpha = 0.78f) },
    )
}

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
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                MissingArtworkFallback(title = item.title, wide = wide)
            }
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(homeResumeProgressBrush()),
                )
            }
            if (item.isPlayed) {
                WatchedCheckBadge(modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 6.dp))
            } else if (item.unplayedItemCount > 0) {
                UnwatchedCountBadge(
                    count = item.unplayedItemCount,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 6.dp),
                )
            }
        }
        Text(
            item.title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (wide) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = rememberLifecycleAwareMarquee(),
        )
        if (item.itemType != "LiveTvChannel" && item.itemType != "LiveTvProgram") {
            item.subtitle?.let {
                Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
            isPlayed = item.isPlayed,
            unplayedItemCount = item.unplayedItemCount,
            onClick = onClick,
            onLongPress = onLongPress,
        )
        Text(
            item.title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (wide) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = rememberLifecycleAwareMarquee(),
        )
        if (item.itemType != "LiveTvChannel" && item.itemType != "LiveTvProgram") {
            item.subtitle?.let {
                Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LibraryViewToggle(selected: LibraryViewMode, onSelect: (LibraryViewMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val icon = when (selected) {
        LibraryViewMode.Poster -> Icons.Rounded.ViewAgenda
        LibraryViewMode.Landscape -> Icons.Rounded.CropLandscape
        LibraryViewMode.Thumbnail -> Icons.Rounded.GridView
    }
    Box {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = "View mode", tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.Transparent)
                .padding(0.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VantafynColors.Graphite.copy(alpha = 0.96f))
                    .vantafynAnimatedModalBorder(cornerRadius = 16.dp, strokeWidth = 1.5.dp)
                    .padding(vertical = 4.dp),
            ) {
                Column {
                    LibraryViewMenuItem(
                        icon = Icons.Rounded.CropPortrait,
                        label = "Poster",
                        selected = selected == LibraryViewMode.Poster,
                        onClick = { onSelect(LibraryViewMode.Poster); expanded = false },
                    )
                    LibraryViewMenuItem(
                        icon = Icons.Rounded.CropLandscape,
                        label = "Landscape",
                        selected = selected == LibraryViewMode.Landscape,
                        onClick = { onSelect(LibraryViewMode.Landscape); expanded = false },
                    )
                    LibraryViewMenuItem(
                        icon = Icons.Rounded.GridView,
                        label = "Compact",
                        selected = selected == LibraryViewMode.Thumbnail,
                        onClick = { onSelect(LibraryViewMode.Thumbnail); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryViewMenuItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun LibraryLandscapeCard(item: JellyfinMediaItem, onClick: () -> Unit, onLongPress: () -> Unit = {}) {
    val progress = item.progress
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF24304D), Color(0xFF393456), VantafynColors.SurfaceHigh)))
                .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        ) {
            val artwork = item.backdropUrl ?: item.thumbUrl ?: item.imageUrl
            if (artwork != null) {
                AsyncImage(
                    model = artwork,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                MissingArtworkFallback(title = item.title, wide = true)
            }
            if (item.logoUrl != null) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .heightIn(max = 36.dp)
                        .widthIn(max = 120.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(homeResumeProgressBrush()),
                )
            }
            if (item.isPlayed) {
                WatchedCheckBadge(modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 6.dp))
            } else if (item.unplayedItemCount > 0) {
                UnwatchedCountBadge(
                    count = item.unplayedItemCount,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 6.dp),
                )
            }
        }
        Text(
            item.title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = rememberLifecycleAwareMarquee(),
        )
        item.subtitle?.let {
            Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LibraryThumbnailCard(item: JellyfinMediaItem, onClick: () -> Unit, onLongPress: () -> Unit = {}) {
    val progress = item.progress
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF24304D), Color(0xFF393456), VantafynColors.SurfaceHigh)))
                .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        ) {
            val artwork = item.imageUrl ?: item.backdropUrl ?: item.thumbUrl
            if (artwork != null) {
                AsyncImage(
                    model = artwork,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                MissingArtworkFallback(title = item.title, wide = false)
            }
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(homeResumeProgressBrush()),
                )
            }
            if (item.isPlayed) {
                WatchedCheckBadge(modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 6.dp))
            } else if (item.unplayedItemCount > 0) {
                UnwatchedCountBadge(
                    count = item.unplayedItemCount,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 6.dp),
                )
            }
        }
        Text(
            item.title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = rememberLifecycleAwareMarquee(),
        )
    }
}

@Composable
private fun LibraryItemRow(
    row: List<JellyfinMediaItem>,
    viewMode: LibraryViewMode,
    liveTv: Boolean,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
    onStartLiveTvPlayback: (java.util.UUID, String, String?) -> Unit,
) {
    val onClick: (JellyfinMediaItem) -> Unit = { item ->
        if (liveTv || item.itemType?.startsWith("LiveTv") == true) {
            onStartLiveTvPlayback(item.id, item.title, item.subtitle)
        } else {
            onOpenMedia(item.id)
        }
    }
    val onLongPress: (JellyfinMediaItem) -> Unit = { item ->
        if (!liveTv && item.itemType?.startsWith("LiveTv") != true) {
            onMediaLongPress(item.toMediaActionTarget())
        }
    }
    if (viewMode == LibraryViewMode.Landscape) {
        val item = row.firstOrNull() ?: return
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
            LibraryLandscapeCard(item = item, onClick = { onClick(item) }, onLongPress = { onLongPress(item) })
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            row.forEach { item ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when (viewMode) {
                        LibraryViewMode.Thumbnail -> LibraryThumbnailCard(item = item, onClick = { onClick(item) }, onLongPress = { onLongPress(item) })
                        else -> MediaItemCard(item = item, onClick = { onClick(item) }, onLongPress = { onLongPress(item) })
                    }
                }
            }
            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
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
    isPlayed: Boolean = false,
    unplayedItemCount: Int = 0,
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
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            MissingArtworkFallback(title = title, wide = wide)
        }
        if (progress != null && progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(homeResumeProgressBrush()),
            )
        }
        if (isPlayed) {
            WatchedCheckBadge(modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 6.dp))
        } else if (unplayedItemCount > 0) {
            UnwatchedCountBadge(
                count = unplayedItemCount,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 6.dp),
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
private fun LibrariesScreen(
    state: VantafynHomeUiState,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onReorderLibraries: (List<UUID>) -> Unit,
    onSetViewMode: (LibrariesViewMode) -> Unit,
) {
    var arranging by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val visualLibraries = remember { mutableStateListOf<JellyfinLibrary>() }
    val librariesRevealKey = remember(state.session?.profileId, state.isLibrariesLoading, state.libraries.size) {
        "${state.session?.profileId.orEmpty()}-${state.isLibrariesLoading}-${state.libraries.size}"
    }
    var librariesRevealActive by remember(librariesRevealKey) { mutableStateOf(!state.isLibrariesLoading) }
    LaunchedEffect(state.libraries.map { it.id }) {
        visualLibraries.clear()
        visualLibraries.addAll(state.libraries)
    }
    LaunchedEffect(librariesRevealKey) {
        if (!state.isLibrariesLoading) {
            librariesRevealActive = true
            delay(900L)
            librariesRevealActive = false
        } else {
            librariesRevealActive = false
        }
    }
    LaunchedEffect(state.mobileDestination, state.libraries.map { it.id }) {
        if (state.mobileDestination != MobileDestination.Libraries || state.libraries.size < 2) {
            arranging = false
        }
    }
    fun moveVisualLibrary(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in visualLibraries.indices || toIndex !in visualLibraries.indices) return
        val item = visualLibraries.removeAt(fromIndex)
        visualLibraries.add(toIndex, item)
    }
    fun finishArrange(commit: Boolean) {
        if (commit) {
            onReorderLibraries(visualLibraries.map { it.id })
        } else {
            visualLibraries.clear()
            visualLibraries.addAll(state.libraries)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            HomeContentReveal(index = 0, animate = librariesRevealActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Libraries",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    LibrariesViewToggle(selected = state.librariesViewMode, onSelect = onSetViewMode)
                }
            }
        }
        if (arranging) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Arrange libraries", color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    GlassAction("Done", onClick = {
                        arranging = false
                        finishArrange(commit = true)
                    })
                }
            }
        }
        if (state.isLibrariesLoading) item {
            HomeContentReveal(index = 1, animate = librariesRevealActive) { HomeLoadingShelf() }
        }
        state.errorMessage?.let { message ->
            item {
                HomeContentReveal(index = 1, animate = librariesRevealActive) { VantafynErrorCard(message) }
            }
        }
        if (!state.isLibrariesLoading && state.libraries.isEmpty()) {
            item {
                HomeContentReveal(index = 1, animate = librariesRevealActive) {
                    LibrariesEmptyState()
                }
            }
        }
        if (!state.isLibrariesLoading && visualLibraries.isNotEmpty() && !arranging && state.librariesViewMode == LibrariesViewMode.Grid) {
            val rows = visualLibraries.chunked(2)
            itemsIndexed(rows, key = { index, row -> "lib-grid-${row.firstOrNull()?.id}-$index" }) { index, row ->
                HomeContentReveal(index = index + 1, animate = librariesRevealActive) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                        row.forEach { library ->
                            Box(modifier = Modifier.weight(1f)) {
                                LibraryGridCard(library = library) { onOpenLibrary(library) }
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            itemsIndexed(visualLibraries, key = { _, library -> library.id }) { index, library ->
                HomeContentReveal(index = index + 1, animate = librariesRevealActive && !arranging) {
                    LibraryListCard(
                        library = library,
                        arranging = arranging,
                        onStartArrange = {
                            arranging = true
                        },
                        onMoveUp = { moveVisualLibrary(index, index - 1) },
                        onMoveDown = { moveVisualLibrary(index, index + 1) },
                        canMoveUp = index > 0,
                        canMoveDown = index < visualLibraries.lastIndex,
                        onClick = { onOpenLibrary(library) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryDetailScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSetFilter: (JellyfinLibraryItemFilter) -> Unit,
    onSetAlphabet: (String?) -> Unit,
    onSetViewMode: (LibraryViewMode) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMediaLongPress: (MediaActionTarget) -> Unit,
    onStartLiveTvPlayback: (java.util.UUID, String, String?) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    val library = state.selectedLibrary
    val liveTv = library?.collectionType.isLiveTvCollection()
    val showAlphabetRail = !liveTv && state.libraryItemsFilter.supportsLibraryAlphabetRail()
    val visibleItems = if (state.isLibraryItemsLoading) emptyList() else state.libraryItems
    val screenRevealKey = library?.id?.toString().orEmpty()
    var screenRevealActive by remember(screenRevealKey) { mutableStateOf(true) }
    LaunchedEffect(screenRevealKey) {
        screenRevealActive = true
        delay(1_100L)
        screenRevealActive = false
    }
    val contentRevealKey = "${library?.id}-${state.libraryItemsPage?.startIndex ?: -1}-${state.libraryItemsFilter.name}-${state.libraryItemsAlphabetKey.orEmpty()}-${state.libraryItemsPage?.totalItems ?: -1}"
    var contentRevealActive by remember(contentRevealKey, state.isLibraryItemsLoading) { mutableStateOf(!state.isLibraryItemsLoading) }
    LaunchedEffect(contentRevealKey, state.isLibraryItemsLoading) {
        if (!state.isLibraryItemsLoading) {
            contentRevealActive = true
            delay(1_100L)
            contentRevealActive = false
        } else {
            contentRevealActive = false
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HomeContentReveal(index = 0, animate = screenRevealActive, revealKey = screenRevealKey) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    CompactBackButton(onClick = onBack)
                    ScreenTitle(library?.name ?: "Library", null)
                    Spacer(modifier = Modifier.weight(1f))
                    if (!liveTv) LibraryViewToggle(selected = state.libraryViewMode, onSelect = onSetViewMode)
                }
            }
        }
        if (!liveTv) {
            item {
                HomeContentReveal(index = 1, animate = screenRevealActive, revealKey = screenRevealKey) {
                    LibraryFilterChips(
                        selected = state.libraryItemsFilter,
                        isMusic = library?.collectionType?.lowercase() == "music",
                        onSelected = onSetFilter,
                    )
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = showAlphabetRail,
                enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    expandVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                    shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)),
            ) {
                HomeContentReveal(index = 2, animate = screenRevealActive, revealKey = screenRevealKey) {
                    LibraryAlphabetRail(
                        selected = state.libraryItemsAlphabetKey,
                        enabled = !state.isLibraryItemsLoading,
                        onSelected = onSetAlphabet,
                    )
                }
            }
        }
        state.libraryItemsPage?.let { page ->
            item {
                HomeContentReveal(index = 3, animate = screenRevealActive, revealKey = screenRevealKey) {
                    LibraryPageControls(
                        page = page,
                        loading = state.isLibraryItemsLoading,
                        onPrevious = onPreviousPage,
                        onNext = onNextPage,
                    )
                }
            }
        }
        if (state.isLibraryItemsLoading) item { HomeContentReveal(index = 4, animate = contentRevealActive, revealKey = contentRevealKey) { HomeLoadingShelf() } }
        state.libraryItemsError?.let { message ->
            item { HomeContentReveal(index = 4, animate = contentRevealActive, revealKey = contentRevealKey) { VantafynErrorCard(message) { VantafynButton("Retry", onClick = onRetry) } } }
        }
        if (liveTv) {
            item {
                HomeContentReveal(index = 4, animate = contentRevealActive, revealKey = contentRevealKey) {
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
        }
        if (!state.isLibraryItemsLoading && state.libraryItems.isEmpty() && state.libraryItemsError == null) {
            item {
                HomeContentReveal(index = 4, animate = contentRevealActive, revealKey = contentRevealKey) {
                    if (liveTv) {
                        LibraryItemsEmptyState(title = "No Live TV channels", subtitle = "Jellyfin did not return channels for this profile/server.", icon = Icons.Rounded.Tv)
                    } else {
                        LibraryItemsEmptyState(title = "Nothing here yet", subtitle = "This library returned no browsable items.", icon = Icons.Rounded.CollectionsBookmark)
                    }
                }
            }
        }
        if (!state.isLibraryItemsLoading && state.libraryItems.isNotEmpty() && visibleItems.isEmpty()) {
            item { HomeContentReveal(index = 4, animate = contentRevealActive, revealKey = contentRevealKey) { LibraryItemsEmptyState(title = "No matching items", subtitle = "Try All or a different filter.", icon = Icons.Rounded.Search) } }
        }
        if (visibleItems.isNotEmpty()) {
            val isMusicLibrary = library?.collectionType?.lowercase() == "music"
            val isPlaylists = library?.collectionType?.lowercase() == "playlists" && state.libraryItemsFilter == JellyfinLibraryItemFilter.All
            val viewMode = state.libraryViewMode
            val chunkSize = if (viewMode == LibraryViewMode.Landscape) 1 else 2
            if (isMusicLibrary) {
                val albums = visibleItems.filter { it.itemType == "MusicAlbum" }
                val songs = visibleItems.filter { it.itemType == "Audio" }
                var revealIndex = 5
                if (albums.isNotEmpty()) {
                    item {
                        HomeContentReveal(index = revealIndex, animate = contentRevealActive, revealKey = contentRevealKey) {
                            Text("Albums", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    revealIndex++
                    val albumRows = albums.chunked(chunkSize)
                    itemsIndexed(albumRows, key = { index, row -> "album-${row.firstOrNull()?.id}-$index" }) { index, row ->
                        HomeContentReveal(index = revealIndex + index, animate = contentRevealActive, revealKey = contentRevealKey) {
                            LibraryItemRow(row, viewMode, liveTv, onOpenMedia, onMediaLongPress, onStartLiveTvPlayback)
                        }
                    }
                    revealIndex += albumRows.size
                }
                if (songs.isNotEmpty()) {
                    item {
                        HomeContentReveal(index = revealIndex, animate = contentRevealActive, revealKey = contentRevealKey) {
                            Text("Songs", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    revealIndex++
                    val songRows = songs.chunked(chunkSize)
                    itemsIndexed(songRows, key = { index, row -> "song-${row.firstOrNull()?.id}-$index" }) { index, row ->
                        HomeContentReveal(index = revealIndex + index, animate = contentRevealActive, revealKey = contentRevealKey) {
                            LibraryItemRow(row, viewMode, liveTv, onOpenMedia, onMediaLongPress, onStartLiveTvPlayback)
                        }
                    }
                }
            } else if (isPlaylists) {
                val musicPlaylists = visibleItems.filter { it.mediaType?.lowercase() == "audio" }
                val otherPlaylists = visibleItems.filter { it.mediaType?.lowercase() != "audio" }
                var revealIndex = 5
                if (otherPlaylists.isNotEmpty()) {
                    val rows = otherPlaylists.chunked(chunkSize)
                    itemsIndexed(rows, key = { index, row -> "playlist-${row.firstOrNull()?.id}-$index" }) { index, row ->
                        HomeContentReveal(index = revealIndex + index, animate = contentRevealActive, revealKey = contentRevealKey) {
                            LibraryItemRow(row, viewMode, liveTv, onOpenMedia, onMediaLongPress, onStartLiveTvPlayback)
                        }
                    }
                    revealIndex += rows.size
                }
                if (musicPlaylists.isNotEmpty()) {
                    item {
                        HomeContentReveal(index = revealIndex, animate = contentRevealActive, revealKey = contentRevealKey) {
                            Text("Music Playlists", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    revealIndex++
                    val rows = musicPlaylists.chunked(chunkSize)
                    itemsIndexed(rows, key = { index, row -> "music-playlist-${row.firstOrNull()?.id}-$index" }) { index, row ->
                        HomeContentReveal(index = revealIndex + index, animate = contentRevealActive, revealKey = contentRevealKey) {
                            LibraryItemRow(row, viewMode, liveTv, onOpenMedia, onMediaLongPress, onStartLiveTvPlayback)
                        }
                    }
                }
            } else {
                val rows = visibleItems.chunked(chunkSize)
                itemsIndexed(rows, key = { index, row -> "${row.firstOrNull()?.id}-$index" }) { index, row ->
                    HomeContentReveal(index = index + 5, animate = contentRevealActive, revealKey = contentRevealKey) {
                        LibraryItemRow(row, viewMode, liveTv, onOpenMedia, onMediaLongPress, onStartLiveTvPlayback)
                    }
                }
            }
        }
        state.libraryItemsPage?.let { page ->
            if (page.totalPages > 1) {
                item {
                    HomeContentReveal(index = 8, animate = screenRevealActive, revealKey = screenRevealKey) {
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
}

@Composable
private fun LibraryFilterChips(selected: JellyfinLibraryItemFilter, isMusic: Boolean, onSelected: (JellyfinLibraryItemFilter) -> Unit) {
    val entries = if (isMusic) {
        JellyfinLibraryItemFilter.entries.filter { it != JellyfinLibraryItemFilter.Unwatched }
    } else {
        JellyfinLibraryItemFilter.entries
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(entries, key = { it.name }) { mode ->
            LibraryFilterChip(mode.label, selected == mode) { onSelected(mode) }
        }
    }
}

@Composable
private fun LibraryAlphabetRail(
    selected: String?,
    enabled: Boolean,
    onSelected: (String?) -> Unit,
) {
    val letters = remember { listOf("#") + ('A'..'Z').map { it.toString() } }
    val selectedIndex = selected?.let { letters.indexOf(it).takeIf { index -> index >= 0 } } ?: -1
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem((selectedIndex - 3).coerceAtLeast(0))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .drawWithContent {
                drawContent()
                val edge = 28.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to VantafynColors.Graphite,
                        1f to Color.Transparent,
                        startX = 0f,
                        endX = edge,
                    ),
                    size = Size(edge, size.height),
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        1f to VantafynColors.Graphite,
                        startX = size.width - edge,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - edge, 0f),
                    size = Size(edge, size.height),
                )
            },
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(letters, key = { it }) { letter ->
                LibraryAlphabetLetter(
                    label = letter,
                    selected = selected == letter,
                    enabled = enabled,
                    onClick = {
                        scope.launch {
                            val index = letters.indexOf(letter).takeIf { it >= 0 } ?: 0
                            listState.animateScrollToItem((index - 3).coerceAtLeast(0))
                        }
                        onSelected(if (selected == letter) null else letter)
                    },
                )
            }
        }
    }
}

@Composable
private fun LibraryAlphabetLetter(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.82f),
        label = "libraryAlphabetPress",
    )
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 32.dp)
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.17f),
                                    VantafynColors.Surface.copy(alpha = 0.72f),
                                    VantafynColors.Graphite.copy(alpha = 0.58f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = VantafynGradients.accentHorizontal(),
                            shape = RoundedCornerShape(999.dp),
                        )
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = when {
                !enabled -> VantafynColors.Muted.copy(alpha = 0.38f)
                selected -> VantafynColors.Ink
                else -> VantafynColors.Muted.copy(alpha = 0.86f)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
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
    val display = LibraryPageControlDisplay(
        firstItem = firstItem,
        lastItem = lastItem,
        totalItems = page.totalItems,
        hasPrevious = page.hasPrevious,
        hasNext = page.hasNext,
        loading = loading,
    )
    AnimatedContent(
        targetState = display,
        transitionSpec = {
            fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(animationSpec = tween(170, easing = FastOutSlowInEasing))
        },
        label = "libraryPageControlsFade",
    ) { target ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryPageAction("Prev", enabled = target.hasPrevious && !target.loading, onClick = onPrevious)
            Text(
                "${target.firstItem}-${target.lastItem} of ${target.totalItems.groupedCountLabel()}",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            LibraryPageAction("Next", enabled = target.hasNext && !target.loading, onClick = onNext)
        }
    }
}

private data class LibraryPageControlDisplay(
    val firstItem: Int,
    val lastItem: Int,
    val totalItems: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val loading: Boolean,
)

@Composable
private fun LibraryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    VantafynGlassChip(
        selected = selected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
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

private fun JellyfinLibraryItemFilter.supportsLibraryAlphabetRail(): Boolean =
    this != JellyfinLibraryItemFilter.All && this != JellyfinLibraryItemFilter.RecentlyAdded

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
            containerColor = VantafynModalContainerColor,
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
            .imePadding()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
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
        if (trimmedQuery.length >= 2) {
            item(key = "search-status") {
                AnimatedVisibility(
                    visible = state.isSearchLoading,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
                ) {
                    SearchLoadingState()
                }
            }
        }
        state.searchError?.let { item(key = "search-error") { VantafynErrorCard(it) } }
        if (!state.isSearchLoading && trimmedQuery.length >= 2 && state.searchResults.isEmpty() && state.searchError == null) {
            item(key = "search-no-results") {
                NoSearchResultsState(
                    selectedType = selectedType,
                    onClearSearch = { onSearchQueryChanged("") },
                )
            }
        }
        visibleGroups.toSortedMap().entries.forEachIndexed { sectionIndex, (type, results) ->
            item(key = "search-section-$type") {
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
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f + glow * 0.05f),
                        Color(0xFF31D7FF).copy(alpha = 0.05f + glow * 0.08f),
                        Color(0xFF8B5CFF).copy(alpha = 0.04f + glow * 0.10f),
                    ),
                ),
            )
            .then(
                if (selected) {
                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.15.dp, durationMillis = 4200)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.82f),
                modifier = Modifier.size(15.dp),
            )
            Text(
                label,
                color = if (selected) VantafynColors.Ink else VantafynColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun SearchLoadingState() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        CompactListSkeleton(rows = 3, leadingSize = 58.dp)
        HomeSkeletonRow(1)
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val pulse = if (isResumed) {
        val transition = rememberInfiniteTransition(label = "searchSparkle")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "searchSparklePulse",
        ).value
    } else {
        0.35f
    }
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
        Text(item.title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = rememberLifecycleAwareMarquee())
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
    val groupedEntries = grouped.toSortedMap().entries.toList()
    var revealActive by remember(state.session?.profileId) { mutableStateOf(true) }
    LaunchedEffect(state.session?.profileId) {
        revealActive = true
        delay(1_100L)
        revealActive = false
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            HomeContentReveal(index = 0, animate = revealActive) {
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
        }
        if (state.isFavoritesLoading) item { HomeContentReveal(index = 1, animate = revealActive) { MyListLoadingSkeleton() } }
        state.favoritesError?.let { item { HomeContentReveal(index = 1, animate = revealActive) { VantafynErrorCard(it) } } }
        if (!state.isFavoritesLoading && state.favorites.isEmpty() && state.favoritesError == null) {
            item { HomeContentReveal(index = 1, animate = revealActive) { MyListEmptyState() } }
        }
        groupedEntries.forEachIndexed { groupIndex, (type, itemsForType) ->
            item {
                HomeContentReveal(index = groupIndex + 2, animate = revealActive) {
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
                .background(VantafynSkeletonBrush()),
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
                                .background(VantafynSkeletonBrush()),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.86f else 0.66f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(VantafynSkeletonBrush()),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.52f else 0.44f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(VantafynSkeletonBrush()),
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListEmptyState() {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp)
            .drawWithContent {
                drawContent()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF42D7FF).copy(alpha = 0.18f),
                            Color(0xFF9B5CFF).copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.52f, size.height * 0.18f),
                        radius = size.width * 0.62f,
                    ),
                )
            },
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.04f),
                                Color(0xFF0B1020).copy(alpha = 0.34f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color(0xFF42D7FF).copy(alpha = 0.32f),
                                Color(0xFFA65CFF).copy(alpha = 0.26f),
                            ),
                        ),
                        shape = RoundedCornerShape(36.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.26f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Start building your list",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Save films and shows you want close by. They will appear here as a private collection for this profile.",
                    color = VantafynColors.Muted.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MyListEmptyHint(
                    label = "Browse",
                    value = "Open a library",
                    modifier = Modifier.weight(1f),
                )
                MyListEmptyHint(
                    label = "Save",
                    value = "Tap the heart",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LibrariesEmptyState() {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp)
            .drawWithContent {
                drawContent()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF31D7FF).copy(alpha = 0.16f),
                            Color(0xFF8B5CFF).copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.48f, size.height * 0.18f),
                        radius = size.width * 0.62f,
                    ),
                )
            },
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.04f),
                                Color(0xFF0B1020).copy(alpha = 0.34f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color(0xFF31D7FF).copy(alpha = 0.32f),
                                Color(0xFF8B5CFF).copy(alpha = 0.26f),
                            ),
                        ),
                        shape = RoundedCornerShape(36.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.26f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CollectionsBookmark,
                        contentDescription = null,
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "No libraries found",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "This profile may not have access to any libraries. Check the Jellyfin server permissions for this user.",
                    color = VantafynColors.Muted.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MyListEmptyHint(
                    label = "Check",
                    value = "User permissions",
                    modifier = Modifier.weight(1f),
                )
                MyListEmptyHint(
                    label = "Verify",
                    value = "Server is online",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LibraryItemsEmptyState(title: String, subtitle: String, icon: ImageVector) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .drawWithContent {
                drawContent()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF31D7FF).copy(alpha = 0.14f),
                            Color(0xFF8B5CFF).copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.2f),
                        radius = size.width * 0.55f,
                    ),
                )
            },
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.03f),
                                Color(0xFF0B1020).copy(alpha = 0.30f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color(0xFF31D7FF).copy(alpha = 0.28f),
                                Color(0xFF8B5CFF).copy(alpha = 0.22f),
                            ),
                        ),
                        shape = RoundedCornerShape(32.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    subtitle,
                    color = VantafynColors.Muted.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    modifier = Modifier.widthIn(max = 280.dp),
                )
            }
        }
    }
}

@Composable
private fun MyListEmptyHint(label: String, value: String, modifier: Modifier = Modifier) {
    VantafynGlassCard(
        modifier = modifier.heightIn(min = 82.dp),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                label,
                color = VantafynColors.Muted.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                value,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AdminScreen(
    state: VantafynHomeUiState,
    onOpenUser: (java.util.UUID) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateUser: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onScanLibrary: () -> Unit,
    onRunTask: (String) -> Unit,
    onStopTask: (String) -> Unit,
    onSetSpeedLimit: (Int?) -> Unit,
    onSendSessionMessage: (String, String?, String, Long) -> Unit,
    onSendBroadcastMessage: (String?, String, Long) -> Unit,
    onClearSessionMessageError: () -> Unit,
) {
    val overview = state.adminOverview
    val adminUser = overview?.users?.firstOrNull { it.id == state.session?.user?.id }
    var addUserExpanded by remember { mutableStateOf(false) }
    var usersExpanded by rememberSaveable { mutableStateOf(false) }
    var mediaStatsExpanded by rememberSaveable { mutableStateOf(false) }
    var statisticsExpanded by rememberSaveable { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val adminRevealKey = "${state.session?.profileId}-${overview != null}"
    var revealActive by remember(adminRevealKey) { mutableStateOf(true) }
    var showSpeedLimitDialog by remember { mutableStateOf(false) }
    var messageTarget by remember { mutableStateOf<AdminMessageTarget?>(null) }
    LaunchedEffect(adminRevealKey) {
        revealActive = true
        delay(1_250L)
        revealActive = false
    }
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
            try {
                onRefresh()
                while (isActive) {
                    delay(8_000L)
                    LongRunningTaskRegistry.tick("admin.refresh", "refreshing")
                    onRefresh()
                }
            } finally {
                LongRunningTaskRegistry.stop("admin.refresh", "admin screen stopped")
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
            item { HomeContentReveal(index = 0, animate = revealActive, revealKey = adminRevealKey) { ScreenTitle("Admin", overview?.serverName ?: state.server?.name ?: "Jellyfin Server") } }
            if (state.isAdminLoading && overview == null) item { HomeContentReveal(index = 1, animate = revealActive, revealKey = adminRevealKey) { AdminDashboardSkeleton() } }
            state.adminError?.let { item { HomeContentReveal(index = 1, animate = revealActive, revealKey = adminRevealKey) { VantafynErrorCard(it) } } }
            if (overview != null) {
                item { HomeContentReveal(index = 1, animate = revealActive, revealKey = adminRevealKey) { AdminHeroCard(state = state, overview = overview, adminImageUrl = adminUser?.imageUrl, onAvatarClick = onOpenSettings) } }
                item {
                    HomeContentReveal(index = 2, animate = revealActive, revealKey = adminRevealKey) {
                        AdminLibraryScanCard(
                            scanTask = overview.tasks.firstOrNull { it.isLibraryScanTask() },
                            isActionRunning = state.isAdminActionRunning,
                            isScanTracking = state.isLibraryScanTracking,
                            onScanLibrary = onScanLibrary,
                        )
                    }
                }
                item {
                    HomeContentReveal(index = 3, animate = revealActive, revealKey = adminRevealKey) {
                        AdminMediaStatsPanel(
                            overview = overview,
                            expanded = mediaStatsExpanded,
                            onToggle = { mediaStatsExpanded = !mediaStatsExpanded },
                        )
                    }
                }
                item {
                    HomeContentReveal(index = 4, animate = revealActive, revealKey = adminRevealKey) {
                        AdminUsersManagementPanel(
                            users = overview.users,
                            usersExpanded = usersExpanded,
                            addUserExpanded = addUserExpanded,
                            isSaving = state.isAdminUserSaving,
                            errorMessage = state.adminUserError,
                            username = newUsername,
                            password = newPassword,
                            onToggleUsers = { usersExpanded = !usersExpanded },
                            onToggleAddUser = {
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
                            onOpenUser = onOpenUser,
                        )
                    }
                }
                item { HomeContentReveal(index = 5, animate = revealActive, revealKey = adminRevealKey) {
                    AdminSessionsSection(
                        sessions = overview.activeSessions,
                        speedLimitMbps = state.adminSpeedLimitMbps,
                        onTapSpeedLimit = { showSpeedLimitDialog = true },
                        onBroadcast = { messageTarget = AdminMessageTarget.Broadcast(overview.activeSessions) },
                        onMessage = { messageTarget = AdminMessageTarget.Session(it) },
                    )
                } }
                item { HomeContentReveal(index = 6, animate = revealActive, revealKey = adminRevealKey) { AdminStatisticsPanel(overview, expanded = statisticsExpanded, onToggle = { statisticsExpanded = !statisticsExpanded }) } }
                item { HomeContentReveal(index = 7, animate = revealActive, revealKey = adminRevealKey) { AdminPluginsPanel(overview.plugins) } }
                item {
                    HomeContentReveal(index = 8, animate = revealActive, revealKey = adminRevealKey) {
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
    }
    if (showSpeedLimitDialog) {
        SpeedLimitDialog(
            currentLimitMbps = state.adminSpeedLimitMbps,
            onDismiss = { showSpeedLimitDialog = false },
            onSave = { limit ->
                onSetSpeedLimit(limit)
                showSpeedLimitDialog = false
            },
        )
    }
    messageTarget?.let { target ->
        AdminSessionMessageComposer(
            target = target,
            isSending = state.isAdminSessionMessageSending,
            sentKey = state.adminSessionMessageSentKey,
            sentSummary = state.adminSessionMessageSentSummary,
            errorMessage = state.adminSessionMessageError,
            onDismiss = {
                onClearSessionMessageError()
                messageTarget = null
            },
            onClearError = onClearSessionMessageError,
            onSend = { header, text, timeoutMs ->
                when (target) {
                    is AdminMessageTarget.Session -> onSendSessionMessage(target.session.id, header, text, timeoutMs)
                    is AdminMessageTarget.Broadcast -> onSendBroadcastMessage(header, text, timeoutMs)
                }
            },
        )
    }
}

private sealed interface AdminMessageTarget {
    val id: String
    val title: String
    val recipientSummary: String

    data class Session(
        val session: dev.vantafyn.core.jellyfin.JellyfinAdminSession,
    ) : AdminMessageTarget {
        override val id: String = "session-${session.id}"
        override val title: String = "Send message"
        override val recipientSummary: String =
            listOfNotNull(session.userName, session.client, session.deviceName)
                .joinToString(" · ")
                .ifBlank { "Active Jellyfin session" }
    }

    data class Broadcast(
        val sessions: List<dev.vantafyn.core.jellyfin.JellyfinAdminSession>,
    ) : AdminMessageTarget {
        private val compatibleCount = sessions.count { it.supportsDisplayMessage }
        override val id: String = "broadcast-${sessions.map { it.id }.sorted().joinToString("-")}"
        override val title: String = "Broadcast message"
        override val recipientSummary: String =
            "$compatibleCount active ${if (compatibleCount == 1) "device" else "devices"}"
    }
}

@Composable
private fun AdminHeroCard(
    state: VantafynHomeUiState,
    overview: dev.vantafyn.core.jellyfin.JellyfinAdminOverview,
    adminImageUrl: String?,
    onAvatarClick: () -> Unit,
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
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onAvatarClick)
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
private fun AdminCollapsibleBody(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) +
            expandVertically(
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top,
            ),
        exit = fadeOut(tween(220, easing = FastOutSlowInEasing)) +
            shrinkVertically(
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top,
            ),
    ) {
        content()
    }
}

@Composable
private fun AdminUsersManagementPanel(
    users: List<dev.vantafyn.core.jellyfin.JellyfinAdminUser>,
    usersExpanded: Boolean,
    addUserExpanded: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    username: String,
    password: String,
    onToggleUsers: () -> Unit,
    onToggleAddUser: () -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onCreate: () -> Unit,
    onOpenUser: (java.util.UUID) -> Unit,
) {
    GlassPanel(modifier = Modifier.animateContentSize(animationSpec = tween(420, easing = FastOutSlowInEasing))) {
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
                "Users",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (usersExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (usersExpanded) "Collapse users" else "Expand users",
                tint = VantafynColors.Ink,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onToggleUsers)
                    .padding(3.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassAction(
                text = if (addUserExpanded) "Close" else "Add User",
                gradientBorder = true,
                onClick = { if (!isSaving) onToggleAddUser() },
            )
            SoftBadge("${users.size.groupedCountLabel()} profiles")
            SoftBadge("${users.count { it.isAdministrator }.groupedCountLabel()} admins")
        }
        AdminCollapsibleBody(visible = addUserExpanded) {
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
        AdminCollapsibleBody(visible = usersExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
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
                            if (user.isAdministrator) {
                                SoftBadge("Admin")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMediaStatsPanel(
    overview: dev.vantafyn.core.jellyfin.JellyfinAdminOverview,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    GlassPanel(modifier = Modifier.animateContentSize(animationSpec = tween(420, easing = FastOutSlowInEasing))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdminSectionHeader(
                title = "Media Stats",
                icon = Icons.Rounded.CollectionsBookmark,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse media stats" else "Expand media stats",
                tint = VantafynColors.Ink,
                modifier = Modifier.size(28.dp),
            )
        }
        AdminCollapsibleBody(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                AdminMediaStatRow("Total media", overview.totalItems.countOrUnavailable(), Icons.Rounded.CollectionsBookmark)
                AdminMediaStatRow("Movies", overview.moviesCount.countOrUnavailable(), Icons.Rounded.Movie)
                AdminMediaStatRow("Series", overview.seriesCount.countOrUnavailable(), Icons.Rounded.Tv)
                AdminMediaStatRow("Episodes", overview.episodesCount.countOrUnavailable(), Icons.Rounded.PlayArrow)
                AdminMediaStatRow("Music", overview.musicCount.countOrUnavailable(), Icons.Rounded.MusicNote)
            }
        }
    }
}

@Composable
private fun AdminMediaStatRow(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    VantafynGlassSurface(
        modifier = modifier.fillMaxWidth(),
        variant = VantafynGlassVariant.Card,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFDCE4FF),
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(7.dp),
            )
            Text(
                label,
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AdminStatisticsPanel(
    overview: dev.vantafyn.core.jellyfin.JellyfinAdminOverview,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val statistics = overview.statistics
    GlassPanel(modifier = Modifier.animateContentSize(animationSpec = tween(420, easing = FastOutSlowInEasing))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdminSectionHeader(
                title = "Statistics",
                icon = Icons.Rounded.AutoAwesome,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse statistics" else "Expand statistics",
                tint = VantafynColors.Ink,
                modifier = Modifier.size(28.dp),
            )
        }
        val loadedStatistics = statistics
        AdminCollapsibleBody(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                if (
                    loadedStatistics != null &&
                    loadedStatistics.capability == dev.vantafyn.core.jellyfin.JellyfinStatisticsCapability.PlaybackReporting &&
                    (loadedStatistics.totalWatchTimeSeconds > 0L || loadedStatistics.totalPlayCount > 0 || loadedStatistics.users.isNotEmpty() || loadedStatistics.trend.isNotEmpty())
                ) {
                    AdminStatisticsMetricGrid(overview, loadedStatistics)
                    AdminStatisticsContentMixCard(overview, loadedStatistics)
                    if (loadedStatistics.trend.isNotEmpty()) {
                        WatchTimeTrendCard(loadedStatistics.trend)
                    }
                    if (loadedStatistics.users.isNotEmpty()) {
                        AdminUserWatchLeaderboard(loadedStatistics.users.take(8))
                    }
                    if (loadedStatistics.media.isNotEmpty()) {
                        AdminMostWatchedMedia(loadedStatistics.media.take(5))
                    }
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
    }
}

@Composable
private fun AdminStatisticsMetricGrid(
    overview: dev.vantafyn.core.jellyfin.JellyfinAdminOverview,
    statistics: dev.vantafyn.core.jellyfin.JellyfinStatisticsOverview,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AdminStatisticsHeroMetric("Watch time", statistics.totalWatchTimeSeconds.watchTimeLabel(), Icons.Rounded.PlayArrow, Modifier.weight(1f))
            AdminStatisticsHeroMetric("Plays", statistics.totalPlayCount.groupedCountLabel(), Icons.Rounded.AutoAwesome, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AdminStatisticsHeroMetric("Users", overview.users.size.groupedCountLabel(), Icons.Rounded.Groups, Modifier.weight(1f))
            AdminStatisticsHeroMetric("Items", overview.totalItems.countOrUnavailable(), Icons.Rounded.Article, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdminStatisticsHeroMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    VantafynGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF42D7FF).copy(alpha = 0.30f),
                                Color(0xFF9B5CFF).copy(alpha = 0.24f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.94f), modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(label, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AdminStatisticsContentMixCard(
    overview: dev.vantafyn.core.jellyfin.JellyfinAdminOverview,
    statistics: dev.vantafyn.core.jellyfin.JellyfinStatisticsOverview,
) {
    val movies = overview.moviesCount?.coerceAtLeast(0) ?: 0
    val series = overview.seriesCount?.coerceAtLeast(0) ?: 0
    val music = overview.musicCount?.coerceAtLeast(0) ?: 0
    val total = (movies + series + music).coerceAtLeast(1)
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF58D7FF), modifier = Modifier.size(24.dp))
                Text("Content mix", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(statistics.rangeLabel, color = VantafynColors.Muted, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                AdminStatisticsDonut(
                    values = listOf(movies.toFloat(), series.toFloat(), music.toFloat()),
                    colors = listOf(Color(0xFF45D8FF), Color(0xFF9D66FF), Color(0xFFFF7EB8)),
                    modifier = Modifier.size(118.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminContentMixRow("Movies", movies, total, Color(0xFF45D8FF), Icons.Rounded.Movie)
                    AdminContentMixRow("Series", series, total, Color(0xFF9D66FF), Icons.Rounded.Tv)
                    AdminContentMixRow("Music", music, total, Color(0xFFFF7EB8), Icons.Rounded.MusicNote)
                }
            }
        }
    }
}

@Composable
private fun AdminContentMixRow(label: String, value: Int, total: Int, color: Color, icon: ImageVector) {
    val percent = ((value.toFloat() / total.toFloat()) * 100f).toInt()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$percent%", color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun AdminStatisticsDonut(values: List<Float>, colors: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val total = values.sum().coerceAtLeast(1f)
        var start = -90f
        values.forEachIndexed { index, raw ->
            val sweep = raw / total * 360f
            drawArc(
                color = colors[index],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
            )
            start += sweep
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = size.minDimension / 2.7f,
            style = Fill,
        )
    }
}

@Composable
private fun WatchTimeTrendCard(buckets: List<dev.vantafyn.core.jellyfin.JellyfinWatchTimeBucket>) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF58D7FF), modifier = Modifier.size(24.dp))
                Text("Viewing trend", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(buckets.sumOf { it.watchTimeSeconds }.watchTimeLabel(), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            }
            val maxValue = buckets.maxOfOrNull { it.watchTimeSeconds }?.coerceAtLeast(1L) ?: 1L
            Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                val leftInset = 18.dp.toPx()
                val rightInset = 10.dp.toPx()
                val topInset = 18.dp.toPx()
                val bottomInset = 24.dp.toPx()
                val chartWidth = (size.width - leftInset - rightInset).coerceAtLeast(1f)
                val chartHeight = (size.height - topInset - bottomInset).coerceAtLeast(1f)
                repeat(4) { index ->
                    val y = topInset + chartHeight * index / 3f
                    drawLine(
                        color = Color.White.copy(alpha = 0.065f),
                        start = Offset(leftInset, y),
                        end = Offset(size.width - rightInset, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                if (buckets.size == 1) {
                    val y = topInset + chartHeight * (1f - buckets.first().watchTimeSeconds.toFloat() / maxValue.toFloat())
                    drawCircle(Color(0xFF58D7FF), radius = 4.dp.toPx(), center = Offset(leftInset + chartWidth / 2f, y))
                    return@Canvas
                }
                val points = buckets.mapIndexed { index, bucket ->
                    val x = leftInset + chartWidth * (index.toFloat() / (buckets.lastIndex).coerceAtLeast(1).toFloat())
                    val y = topInset + chartHeight * (1f - (bucket.watchTimeSeconds.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f))
                    Offset(x, y)
                }
                val areaPath = Path().apply {
                    moveTo(points.first().x, size.height - bottomInset)
                    points.forEachIndexed { index, point ->
                        if (index == 0) lineTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    lineTo(points.last().x, size.height - bottomInset)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF45D8FF).copy(alpha = 0.34f), Color(0xFF9D66FF).copy(alpha = 0.04f)),
                    ),
                )
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { point -> lineTo(point.x, point.y) }
                }
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(listOf(Color(0xFF45D8FF), Color(0xFF9D66FF))),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                points.forEach { point ->
                    drawCircle(Color(0xFF45D8FF).copy(alpha = 0.26f), radius = 6.dp.toPx(), center = point)
                    drawCircle(Color.White.copy(alpha = 0.94f), radius = 2.4.dp.toPx(), center = point)
                }
                listOf(0, buckets.lastIndex / 2, buckets.lastIndex).distinct().forEach { index ->
                    drawContext.canvas.nativeCanvas.drawText(
                        buckets[index].label,
                        points[index].x - 10.dp.toPx(),
                        size.height - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(160, 220, 228, 255)
                            textSize = 11.sp.toPx()
                            isAntiAlias = true
                        },
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
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = Color(0xFF58D7FF), modifier = Modifier.size(24.dp))
                Text("Top viewers", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            users.forEach { user ->
                val fraction = (user.totalWatchTimeSeconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.05f, 1f)
                VantafynGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    variant = VantafynGlassVariant.Card,
                    cornerRadius = 20.dp,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                user.rank.toString(),
                                color = if (user.rank == 1) Color(0xFFFFD85D) else VantafynColors.Muted,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(28.dp),
                            )
                            ProfileAvatar(user.displayName, user.avatarUrl, Modifier.size(48.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(user.displayName, color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(user.totalWatchTimeSeconds.watchTimeLabel(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            SoftBadge(user.playCount.groupedCountLabel())
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(6.dp)
                                    .background(VantafynGradients.accentHorizontal()),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            AdminTinyUserMetric("Watch time", user.totalWatchTimeSeconds.watchTimeLabel(), Modifier.weight(1f))
                            AdminTinyUserMetric("Plays", user.playCount.groupedCountLabel(), Modifier.weight(1f))
                        }
                        if (user.hasContentBreakdown) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f)),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (user.moviesCount > 0) AdminTinyUserMetric("Movies", user.moviesCount.toString(), Modifier.weight(1f))
                                if (user.episodesCount > 0) AdminTinyUserMetric("Episodes", user.episodesCount.toString(), Modifier.weight(1f))
                                if (user.audioCount > 0) AdminTinyUserMetric("Songs", user.audioCount.toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTinyUserMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(label, color = VantafynColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AdminMostWatchedMedia(media: List<dev.vantafyn.core.jellyfin.JellyfinMediaWatchStats>) {
    val maxWatchTime = media.maxOfOrNull { it.totalWatchTimeSeconds }?.coerceAtLeast(1L) ?: 1L
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Movie, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(23.dp))
                Text(
                    "Most watched",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${media.size} titles",
                    color = VantafynColors.Muted.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            media.forEachIndexed { index, item ->
                AdminMostWatchedMediaCard(
                    rank = index + 1,
                    item = item,
                    progress = (item.totalWatchTimeSeconds.toFloat() / maxWatchTime.toFloat()).coerceIn(0.05f, 1f),
                )
            }
        }
    }
}

@Composable
private fun AdminMostWatchedMediaCard(
    rank: Int,
    item: dev.vantafyn.core.jellyfin.JellyfinMediaWatchStats,
    progress: Float,
) {
    VantafynGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = VantafynGlassVariant.Card,
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
            ) {
                if (item.posterUrl != null) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    AdminMostWatchedFallbackArt(rank = rank, title = item.title)
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Black.copy(alpha = 0.28f),
                                    VantafynColors.Graphite.copy(alpha = 0.88f),
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.48f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("#$rank", color = VantafynColors.Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            item.title,
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = rememberLifecycleAwareMarquee(),
                        )
                        Text(
                            listOfNotNull(item.type?.prettyMediaType(), item.uniqueUsers?.let { "$it viewers" }).joinToString(" · "),
                            color = VantafynColors.Muted.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.totalWatchTimeSeconds.watchTimeLabel(), color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${item.playCount.groupedCountLabel()} plays", color = VantafynColors.Muted, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
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
                            .fillMaxWidth(progress)
                            .height(5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF37D8FF).copy(alpha = 0.86f),
                                        Color(0xFF786AFF).copy(alpha = 0.82f),
                                        Color(0xFFB15CFF).copy(alpha = 0.78f),
                                    ),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMostWatchedFallbackArt(rank: Int, title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF182438),
                        Color(0xFF17152D),
                        Color(0xFF2C183F),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF5FD8FF).copy(alpha = 0.13f)),
        )
        Text(
            title.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
            color = VantafynColors.Ink.copy(alpha = 0.46f),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            "#$rank",
            color = Color.White.copy(alpha = 0.18f),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 6.dp),
        )
    }
}

private fun String.prettyMediaType(): String =
    when (lowercase(Locale.US)) {
        "movie" -> "Movie"
        "series" -> "Series"
        "episode" -> "Episode"
        "audio" -> "Song"
        "musicalbum", "music album" -> "Album"
        else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
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
            Text("Recently active", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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

private fun dev.vantafyn.core.jellyfin.JellyfinAdminTask.isRunning(): Boolean =
    state.equals("Running", ignoreCase = true) || state.equals("RUNNING", ignoreCase = true)

private fun dev.vantafyn.core.jellyfin.JellyfinAdminTask.isLibraryScanTask(): Boolean {
    val haystack = listOf(id, name, category).joinToString(" ").lowercase()
    return "library" in haystack &&
        ("scan" in haystack || "refresh" in haystack)
}

@Composable
private fun AdminSectionHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Text(
            title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AdminSessionsSection(
    sessions: List<dev.vantafyn.core.jellyfin.JellyfinAdminSession>,
    speedLimitMbps: Int?,
    onTapSpeedLimit: () -> Unit,
    onBroadcast: () -> Unit,
    onMessage: (dev.vantafyn.core.jellyfin.JellyfinAdminSession) -> Unit,
) {
    val measuredBitrates = sessions.mapNotNull { it.bitrate?.takeIf { bitrate -> bitrate > 0 } }
    val totalBitrate = measuredBitrates.sum().takeIf { it > 0 }
    val compatibleMessageTargets = sessions.count { it.supportsDisplayMessage }
    GlassPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AdminSectionHeader(title = "Active Sessions", icon = Icons.Rounded.Tv, modifier = Modifier.weight(1f))
            SoftBadge("${sessions.size} live")
        }
        AdminActiveBitrateSummary(
            totalBitrate = totalBitrate,
            speedLimitMbps = speedLimitMbps,
            onTap = onTapSpeedLimit,
        )
        if (compatibleMessageTargets > 0) {
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBroadcast() },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Broadcast Message",
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "$compatibleMessageTargets active ${if (compatibleMessageTargets == 1) "device" else "devices"}",
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
        }
        if (sessions.isEmpty()) {
            AdminEmptySessionsCard()
        }
        sessions.take(12).forEach { session ->
            AdminSessionCard(session = session, onMessage = onMessage)
        }
    }
}

@Composable
private fun AdminActiveBitrateSummary(
    totalBitrate: Int?,
    speedLimitMbps: Int?,
    onTap: () -> Unit,
) {
    val bitrateColor = when {
        totalBitrate == null || speedLimitMbps == null || speedLimitMbps <= 0 -> VantafynColors.Ink
        else -> {
            val limitBps = speedLimitMbps.toLong() * 1_000_000L
            val ratio = totalBitrate.toFloat() / limitBps.toFloat()
            when {
                ratio < 0.5f -> Color(0xFF4ADE80)
                ratio < 0.8f -> Color(0xFFFBBF24)
                else -> Color(0xFFF87171)
            }
        }
    }
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap() },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Total active bitrate",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (speedLimitMbps == null || speedLimitMbps <= 0) {
                    Text(
                        "Tap to set limit",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            val bitrateText = totalBitrate?.streamBitrateLabel() ?: "—"
            Text(
                bitrateText,
                color = bitrateColor,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = when {
                        bitrateText.length > 10 -> 14.sp
                        bitrateText.length > 8 -> 16.sp
                        else -> MaterialTheme.typography.titleLarge.fontSize
                    },
                ),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SpeedLimitDialog(
    currentLimitMbps: Int?,
    onDismiss: () -> Unit,
    onSave: (Int?) -> Unit,
) {
    var text by remember { mutableStateOf(currentLimitMbps?.toString() ?: "") }
    val parsed = text.toIntOrNull()
    val isValid = parsed == null || parsed > 0
    AlertDialog(
        modifier = Modifier
            .imePadding()
            .vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Upload speed limit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                VantafynTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    label = "Mbps (0 or blank to clear)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    "Used for display only — colours the active bitrate based on usage percentage.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsed?.takeIf { it > 0 }) },
                enabled = isValid,
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AdminSessionMessageComposer(
    target: AdminMessageTarget,
    isSending: Boolean,
    sentKey: Long,
    sentSummary: String?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onSend: (String?, String, Long) -> Unit,
) {
    var header by remember(target.id) { mutableStateOf("") }
    var message by remember(target.id) { mutableStateOf("") }
    var durationMs by remember(target.id) { mutableStateOf(8_000L) }
    var dispatched by remember(target.id) { mutableStateOf(false) }
    val initialSentKey = remember(target.id) { sentKey }
    LaunchedEffect(sentKey, isSending) {
        if (sentKey != initialSentKey && sentKey > 0L && !isSending) {
            dispatched = true
            delay(980L)
            onDismiss()
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (dispatched) 0.92f else 1f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.78f),
        label = "adminMessageScale",
    )
    val lift by animateFloatAsState(
        targetValue = if (dispatched) -92f else 0f,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
        label = "adminMessageLift",
    )
    val alpha by animateFloatAsState(
        targetValue = if (dispatched) 0f else 1f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "adminMessageAlpha",
    )
    Dialog(
        onDismissRequest = {
            if (!isSending && !dispatched) {
                onClearError()
                onDismiss()
            }
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val composerMaxHeight = maxHeight * 0.92f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .heightIn(max = composerMaxHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = lift
                        this.alpha = alpha
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(VantafynModalContainerColor)
                    .vantafynAnimatedModalBorder(cornerRadius = 28.dp)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = CoreUiR.drawable.vantafyn_logo),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    if (dispatched) sentSummary ?: "Sent" else target.title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        target.recipientSummary,
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    VantafynTextField(
                        value = header,
                        onValueChange = { header = it.take(48) },
                        label = "Title (optional)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                    VantafynTextField(
                        value = message,
                        onValueChange = { message = it.take(240) },
                        label = "Message",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5_000L to "5s", 8_000L to "8s", 15_000L to "15s").forEach { (value, label) ->
                            VantafynGlassChip(
                                selected = durationMs == value,
                                onClick = { durationMs = value },
                            ) {
                                Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    errorMessage?.let {
                        Text(
                            it,
                            color = Color(0xFFFFB4B4),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AdminMessageSecondaryButton(
                        text = "Cancel",
                        enabled = !isSending && !dispatched,
                        onClick = {
                            onClearError()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    VantafynButton(
                        text = if (isSending) "Sending" else "Send",
                        onClick = { onSend(header, message, durationMs) },
                        enabled = message.isNotBlank() && !isSending && !dispatched,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMessageSecondaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .widthIn(min = 132.dp)
            .height(58.dp)
            .padding(2.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) 0.10f else 0.04f),
                        VantafynColors.SurfaceHigh.copy(alpha = if (enabled) 0.76f else 0.38f),
                        Color.White.copy(alpha = if (enabled) 0.06f else 0.03f),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = if (enabled) 0.24f else 0.08f),
                            VantafynColors.Muted.copy(alpha = if (enabled) 0.16f else 0.06f),
                        ),
                    ),
                ),
                shape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = VantafynSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color(0xFFF8FAFF) else VantafynColors.Muted.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun AdminLibraryScanCard(
    scanTask: dev.vantafyn.core.jellyfin.JellyfinAdminTask?,
    isActionRunning: Boolean,
    isScanTracking: Boolean,
    onScanLibrary: () -> Unit,
) {
    val scanRunning = scanTask?.isRunning() == true
    val showProgress = isActionRunning || isScanTracking || scanRunning || scanTask?.progress != null
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Library Scan", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                AdminToolAction(
                    label = if (isActionRunning || (isScanTracking && !scanRunning)) "Starting" else if (scanRunning) "Running" else "Scan",
                    enabled = !isActionRunning && !isScanTracking && !scanRunning,
                    gradientBorder = true,
                    onClick = onScanLibrary,
                )
            }
            if (showProgress) {
                AdminTaskProgressBar(progress = scanTask?.progress, isRunning = isActionRunning || isScanTracking || scanRunning)
            }
        }
    }
}

@Composable
private fun AdminPluginsPanel(
    plugins: List<dev.vantafyn.core.jellyfin.JellyfinAdminPlugin>,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val enabledCount = plugins.count { it.isEnabled }
    val disabledCount = plugins.size - enabledCount
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(420, easing = FastOutSlowInEasing)),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.92f), modifier = Modifier.size(21.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Plugins", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            plugins.isEmpty() -> "No plugins returned"
                            disabledCount > 0 -> "$enabledCount active · $disabledCount disabled"
                            else -> "$enabledCount active"
                        },
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse plugins" else "Expand plugins",
                    tint = VantafynColors.Ink,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .padding(3.dp),
                )
            }
            AdminCollapsibleBody(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                    if (plugins.isEmpty()) {
                        EmptyState("No plugins found", "Jellyfin did not return installed plugins for this admin session.")
                    } else {
                        plugins
                            .sortedWith(compareByDescending<dev.vantafyn.core.jellyfin.JellyfinAdminPlugin> { it.isEnabled }.thenBy { it.name.lowercase() })
                            .forEach { plugin ->
                                AdminPluginCard(plugin)
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPluginCard(plugin: dev.vantafyn.core.jellyfin.JellyfinAdminPlugin) {
    VantafynGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = VantafynGlassVariant.Card,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (plugin.isEnabled) Color(0xFF30DCA5).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (plugin.isEnabled) Color(0xFF30DCA5) else VantafynColors.Muted.copy(alpha = 0.58f)),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(plugin.name, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    plugin.version?.let { "v$it" } ?: plugin.description.orEmpty().ifBlank { "Installed plugin" },
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SoftBadge(plugin.status.orEmpty().pluginStatusLabel())
        }
    }
}

private fun String.pluginStatusLabel(): String =
    when {
        equals("Active", ignoreCase = true) || equals("ACTIVE", ignoreCase = true) -> "Active"
        equals("Disabled", ignoreCase = true) || equals("DISABLED", ignoreCase = true) -> "Disabled"
        equals("Restart", ignoreCase = true) || equals("RESTART", ignoreCase = true) -> "Restart"
        isBlank() -> "Installed"
        else -> replace('_', ' ').lowercase().replaceFirstChar(Char::titlecase)
    }

@Composable
private fun AdminTasksPanel(
    tasks: List<dev.vantafyn.core.jellyfin.JellyfinAdminTask>,
    isActionRunning: Boolean,
    onRunTask: (String) -> Unit,
    onStopTask: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val runningCount = tasks.count { it.state.equals("Running", ignoreCase = true) || it.state.equals("RUNNING", ignoreCase = true) }
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(420, easing = FastOutSlowInEasing)),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.9f), modifier = Modifier.size(21.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Scheduled Tasks", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            tasks.isEmpty() -> "No tasks returned"
                            runningCount > 0 -> "$runningCount running"
                            else -> "${tasks.size} tasks"
                        },
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse tasks" else "Expand tasks",
                    tint = VantafynColors.Ink,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .padding(3.dp),
                )
            }
            AdminCollapsibleBody(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                    if (tasks.isEmpty()) {
                        Text("No scheduled tasks returned", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                    } else {
                        val groupedTasks = remember(tasks) {
                            tasks.groupBy { task ->
                                task.category?.takeIf { it.isNotBlank() } ?: "General"
                            }
                        }
                        groupedTasks.forEach { (categoryName, categoryTasks) ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = categoryName.uppercase(),
                                        color = VantafynColors.Ink.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                    )
                                    val runningInCategory = categoryTasks.count {
                                        it.state.equals("Running", ignoreCase = true) || it.state.equals("RUNNING", ignoreCase = true)
                                    }
                                    if (runningInCategory > 0) {
                                        Text(
                                            text = "$runningInCategory running",
                                            color = VantafynColors.Primary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                categoryTasks.forEach { task ->
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
                    }
                }
            }
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val activeAlpha = if (isResumed && safeProgress == null && isRunning) {
        val transition = rememberInfiniteTransition(label = "adminTaskProgress")
        transition.animateFloat(
            initialValue = 0.46f,
            targetValue = 0.92f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "adminTaskProgressAlpha",
        ).value
    } else {
        1f
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (isRunning) "Running" else "Last progress",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                safeProgress?.let { "${it.toInt()}%" } ?: if (isRunning) "Working" else "Queued",
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
	                    .fillMaxWidth(if (safeProgress != null) (safeProgress / 100f).coerceIn(0.03f, 1f) else 0.42f)
	                    .height(7.dp)
	                    .graphicsLayer(alpha = if (safeProgress == null && isRunning) activeAlpha else 1f)
	                    .background(VantafynGradients.accentHorizontal()),
            )
        }
    }
}

@Composable
private fun AdminToolPanel(title: String, icon: ImageVector? = null, empty: Boolean, emptyText: String, content: @Composable ColumnScope.() -> Unit) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) {
                AdminSectionHeader(title = title, icon = icon)
            } else {
                Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
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
private fun AdminToolAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    gradientBorder: Boolean = false,
) {
    Box(
        modifier = Modifier
            .then(
                if (gradientBorder && enabled) {
                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.2.dp, durationMillis = 4200)
                } else {
                    Modifier
                },
            )
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
private fun AdminSessionCard(
    session: dev.vantafyn.core.jellyfin.JellyfinAdminSession,
    onMessage: (dev.vantafyn.core.jellyfin.JellyfinAdminSession) -> Unit,
) {
    val playMethodLabel = session.playMethod ?: if (session.isTranscoding) "Transcoding" else "Direct Play"
    val playMethodForeground = if (session.isTranscoding) Color(0xFFFFC077) else Color(0xFF91F0B6)
    val playMethodBackground = if (session.isTranscoding) Color(0xFF3A2113).copy(alpha = 0.92f) else Color(0xFF123421).copy(alpha = 0.92f)
    val playbackStateBackground = if (session.isPaused) {
        Color(0xFF252B3A).copy(alpha = 0.94f)
    } else {
        Color(0xFF182B4B).copy(alpha = 0.94f)
    }
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
                            SoftBadge(
                                text = if (session.isPaused) "Paused" else "Playing",
                                color = VantafynColors.Ink,
                                background = playbackStateBackground,
                            )
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
                if (session.supportsDisplayMessage) {
                    IconButton(
                        onClick = { onMessage(session) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(42.dp),
                    ) {
                        GradientIcon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = "Send message",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(VantafynSpacing.md),
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
            ) {
                AdminSessionProgress(session)
                AdminSessionTechnicalLine(session)
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
private fun GradientIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = Color.White,
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = VantafynGradients.accentHorizontal(),
                    blendMode = BlendMode.SrcIn,
                )
            },
    )
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
    val details = buildList {
        session.streamQuality?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        session.container?.uppercase()?.let { container ->
            val audioCodec = session.audioCodec?.uppercase()
            if (audioCodec == null || container != audioCodec) add(container)
        }
        session.videoCodec?.uppercase()?.let { add(it) }
        session.audioCodec?.uppercase()?.let { add(it) }
        session.bitrate?.takeIf { it > 0 }?.let { add(it.streamBitrateLabel()) }
    }
    if (details.isNotEmpty()) {
        Text(details.joinToString(" · "), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    } else {
        Text("Bitrate unavailable", color = VantafynColors.Muted.copy(alpha = 0.74f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Int.streamBitrateLabel(): String {
    if (this <= 0) return "0 Mbps"
    return if (this >= 1_000_000) {
        val mbps = this / 1_000_000.0
        val text = if (mbps >= 10) "%.0f".format(Locale.US, mbps) else "%.1f".format(Locale.US, mbps)
        "${text.trimEnd('0').trimEnd('.')} Mbps"
    } else {
        "${this / 1_000} Kbps"
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
    var showMatchDeck by rememberSaveable { mutableStateOf(false) }
    val revealKey = "watch-party-${state.session?.profileId}"
    var revealActive by remember(revealKey) { mutableStateOf(true) }
    LaunchedEffect(revealKey) {
        revealActive = true
        delay(1_100L)
        revealActive = false
    }
    LaunchedEffect(state.watchPartyMode) {
        if (state.watchPartyMode != WatchPartyMode.SwipeToMatch) showMatchDeck = false
    }
    LaunchedEffect(state.showWatchPartyInviteSentAnimation) {
        if (state.showWatchPartyInviteSentAnimation) {
            kotlinx.coroutines.delay(850)
            onClearInviteAnimation()
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 138.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            HomeContentReveal(index = 0, animate = revealActive, revealKey = revealKey) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text("Watch Party", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    }
                    SoftBadge(state.watchPartyRealtimeConnectionState.watchPartyLabel())
                }
            }
        }
        item {
            HomeContentReveal(index = 1, animate = revealActive, revealKey = revealKey) {
                VantafynGlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(VantafynGradients.accentHorizontal()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Groups, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(26.dp))
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(state.activeWatchParty?.name ?: "Start a room", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(state.activeWatchParty?.serverName ?: state.server?.name ?: "Current Jellyfin server", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            SoftBadge(if (state.activeWatchParty == null) "Solo" else "Host")
                        }
                        VantafynTextField(
                            value = state.watchPartyName,
                            onValueChange = onName,
                            label = "Room name",
                            placeholder = "${state.session?.user?.name ?: "My"} Watch Party",
                        )
                        WatchPartyModeSelector(selected = state.watchPartyMode, onSelected = onMode)
                        state.watchPartySelectedMedia?.let { media ->
                            WatchPartySelectedMediaCard(media)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            VantafynButton(
                                text = if (state.activeWatchParty == null) "Create room" else "Refresh room",
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
            }
        }
        if (state.watchPartyMode == WatchPartyMode.SwipeToMatch) {
            item {
                HomeContentReveal(index = 2, animate = revealActive, revealKey = revealKey) {
                    WatchPartyMatchLaunchCard(
                        candidate = state.currentWatchPartyCandidate,
                        remaining = (state.watchPartyCandidates.size - state.watchPartyCurrentIndex).coerceAtLeast(0),
                        hasMatch = state.watchPartyMatch != null,
                        onOpen = { showMatchDeck = true },
                    )
                }
            }
        }
        item {
            HomeContentReveal(index = 3, animate = revealActive, revealKey = revealKey) {
                WatchPartySettingsPanel(
                    state = state,
                    onToggleWatchPartyEnabled = onToggleWatchPartyEnabled,
                    onToggleWatchPartyInvitesEnabled = onToggleWatchPartyInvitesEnabled,
                    onToggleWatchPartyInviteAnimationEnabled = onToggleWatchPartyInviteAnimationEnabled,
                    onSetWatchPartyInviteExpirySeconds = onSetWatchPartyInviteExpirySeconds,
                    onRetryRealtime = onRefreshRecipients,
                )
            }
        }
        item {
            HomeContentReveal(index = 4, animate = revealActive, revealKey = revealKey) {
                WatchPartyInvitePanel(
                    state = state,
                    onRefreshRecipients = onRefreshRecipients,
                    onToggleRecipient = onToggleRecipient,
                    onSendInvites = onSendInvites,
                )
            }
        }
        item {
            HomeContentReveal(index = 5, animate = revealActive, revealKey = revealKey) {
                WatchPartyRealtimePanel(state = state, onToggleReady = onToggleReady)
            }
        }
        if (state.watchPartyMode == WatchPartyMode.FixedTitle && state.watchPartySelectedMedia != null) {
            item {
                HomeContentReveal(index = 6, animate = revealActive, revealKey = revealKey) {
                    VantafynGlassPanel(cornerRadius = 28.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Lobby", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            VantafynButton(
                                text = "Start Watching",
                                onClick = onStartFixed,
                                enabled = state.activeWatchParty != null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        item {
            HomeContentReveal(index = 7, animate = revealActive, revealKey = revealKey) {
                VantafynGlassPanel(cornerRadius = 28.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Match style", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        WatchPartyRuleSection(
                            title = "What to match",
                            options = WatchPartyMediaScope.entries,
                            selected = state.watchPartyRules.mediaScope,
                            label = { it.watchPartyLabel() },
                            onSelected = { onRules(state.watchPartyRules.copy(mediaScope = it)) },
                        )
                        WatchPartyRuleSection(
                            title = "Choose when",
                            options = WatchPartyMatchRule.entries,
                            selected = state.watchPartyRules.matchRule,
                            label = { it.watchPartyLabel() },
                            onSelected = { onRules(state.watchPartyRules.copy(matchRule = it)) },
                        )
                        WatchPartyRuleSection(
                            title = "Length",
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
            }
        }
        item {
            HomeContentReveal(index = 8, animate = revealActive, revealKey = revealKey) {
                if (state.isWatchPartyLoading) {
                    VantafynLoadingIndicator("Preparing Watch Party")
                }
                state.watchPartyError?.let { VantafynErrorCard(it) }
            }
        }
    }
    if (showMatchDeck && state.watchPartyMode == WatchPartyMode.SwipeToMatch) {
        WatchPartyMatchDeckModal(
            candidate = state.currentWatchPartyCandidate,
            nextCandidate = state.watchPartyCandidates.getOrNull(state.watchPartyCurrentIndex + 1),
            match = state.watchPartyMatch?.candidate,
            remaining = (state.watchPartyCandidates.size - state.watchPartyCurrentIndex).coerceAtLeast(0),
            deckGeneration = state.watchPartyDeckGeneration,
            onDismiss = { showMatchDeck = false },
            onVote = onVote,
            onRefresh = onRefresh,
            onStartMatched = {
                showMatchDeck = false
                onStartMatched()
            },
        )
    }
}

@Composable
private fun WatchPartyMatchLaunchCard(
    candidate: WatchPartyCandidate?,
    remaining: Int,
    hasMatch: Boolean,
    onOpen: () -> Unit,
) {
    VantafynGlassPanel(cornerRadius = 28.dp, contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(VantafynGradients.accentHorizontal()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(30.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (hasMatch) "Match found" else "Swipe to match", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (hasMatch) "Open the deck to start watching." else candidate?.title ?: "Find something everyone wants to watch.",
                        color = VantafynColors.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SoftBadge(if (hasMatch) "Ready" else "$remaining")
            }
            VantafynButton(
                text = if (hasMatch) "View match" else "Open deck",
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WatchPartyMatchDeckModal(
    candidate: WatchPartyCandidate?,
    nextCandidate: WatchPartyCandidate?,
    match: WatchPartyCandidate?,
    remaining: Int,
    deckGeneration: Int,
    onDismiss: () -> Unit,
    onVote: (WatchPartyVoteValue) -> Unit,
    onRefresh: () -> Unit,
    onStartMatched: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 106.dp),
        contentAlignment = Alignment.Center,
    ) {
        VantafynGlassModalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 560.dp, max = 680.dp)
                .vantafynAnimatedModalBorder(cornerRadius = 34.dp, strokeWidth = 1.4.dp),
            cornerRadius = 34.dp,
            contentPadding = PaddingValues(14.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Pick something together", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (match == null) "$remaining titles waiting" else "It's a match", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("X", color = VantafynColors.Ink, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (match != null) {
                        WatchPartyMatchCelebration(candidate = match, onStartMatched = onStartMatched)
                    } else {
                        WatchPartyDeck(
                            candidate = candidate,
                            nextCandidate = nextCandidate,
                            remaining = remaining,
                            deckGeneration = deckGeneration,
                            onVote = onVote,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Party options", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            PremiumToggleRow("Watch Party", "", state.watchPartyEnabled, onClick = onToggleWatchPartyEnabled)
            PremiumToggleRow("Invites", "", state.watchPartyInvitesEnabled, onClick = onToggleWatchPartyInvitesEnabled)
            PremiumToggleRow("Invite animation", "", state.watchPartyInviteAnimationEnabled, onClick = onToggleWatchPartyInviteAnimationEnabled)
            Text("Invite stays open", color = VantafynColors.Muted, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(30 to "30 sec", 60 to "1 min", 300 to "5 min").forEach { (seconds, label) ->
                    VantafynGlassChip(
                        selected = state.watchPartyInviteExpirySeconds == seconds,
                        onClick = { onSetWatchPartyInviteExpirySeconds(seconds) },
                        modifier = Modifier.weight(1f),
                    ) {
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
                Text(media.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = rememberLifecycleAwareMarquee())
                Text(media.subtitle ?: media.itemType ?: "Selected title", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WatchPartyInvitePanel(
    state: VantafynHomeUiState,
    onRefreshRecipients: () -> Unit,
    onToggleRecipient: (String) -> Unit,
    onSendInvites: () -> Unit,
) {
    VantafynGlassPanel(cornerRadius = 28.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Invite viewers", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
            if (state.isWatchPartyRecipientsLoading) {
                VantafynLoadingIndicator("Finding active users")
            } else if (state.watchPartyInviteRecipients.isEmpty()) {
                VantafynGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nobody is available right now", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        Text("Open Vantafyn on another device, then refresh.", color = VantafynColors.Muted)
                    }
                }
            } else {
                VantafynGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.watchPartyInviteRecipients.forEach { recipient ->
                            val selected = recipient.sessionId in state.selectedWatchPartyRecipientSessionIds
                            WatchPartyRecipientTile(
                                name = recipient.displayName,
                                imageUrl = recipient.imageUrl,
                                online = recipient.active,
                                selected = selected,
                                onClick = { onToggleRecipient(recipient.sessionId) },
                            )
                        }
                    }
                }
            }
            VantafynButton(
                text = when (state.watchPartyMode) {
                    WatchPartyMode.FixedTitle -> "Send invite"
                    WatchPartyMode.SwipeToMatch -> "Invite to match"
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Invite sent", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("They'll see it while Vantafyn is open and connected.", color = VantafynColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchPartyRecipientTile(
    name: String,
    imageUrl: String?,
    online: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) Color.White.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.045f))
            .border(
                width = 1.dp,
                brush = if (selected) VantafynGradients.accentHorizontal() else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.06f))),
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            ProfileAvatar(name, imageUrl, Modifier.size(54.dp))
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (online) Color(0xFF66E6A8) else Color.White.copy(alpha = 0.35f))
                    .border(2.dp, VantafynColors.Graphite.copy(alpha = 0.95f), RoundedCornerShape(999.dp)),
            )
        }
        Text(
            name,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WatchPartyRealtimePanel(state: VantafynHomeUiState, onToggleReady: () -> Unit) {
    val currentUserId = state.session?.user?.id
    val currentReady = currentUserId?.let { state.localWatchPartyReadyStates[it] } ?: WatchPartyMemberReadyStatus.NotReady
    VantafynGlassPanel(cornerRadius = 28.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    Text("Room status", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(state.watchPartySyncStateLabel, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                SoftBadge(state.watchPartyRealtimeConnectionState.watchPartyLabel())
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
                    Text("Ready to watch", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                    SoftBadge(if (currentReady == WatchPartyMemberReadyStatus.Ready) "Ready" else "Not ready")
                }
            }
            if (state.watchPartyRealtimeMembers.isEmpty()) {
                VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Waiting for everyone", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        Text("People will appear here when they join.", color = VantafynColors.Muted)
                    }
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
    nextCandidate: WatchPartyCandidate?,
    remaining: Int,
    deckGeneration: Int,
    onVote: (WatchPartyVoteValue) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotionPreference()
    val cardKey = "${deckGeneration}-${candidate?.id}"
    val dragX = remember(cardKey) { Animatable(0f) }
    val dragY = remember(cardKey) { Animatable(0f) }
    val fade = remember(cardKey) { Animatable(0f) }
    var isCommitting by remember(cardKey) { mutableStateOf(false) }
    LaunchedEffect(cardKey) {
        isCommitting = false
        dragX.snapTo(0f)
        dragY.snapTo(if (reducedMotion) 0f else 18f)
        fade.snapTo(0f)
        if (reducedMotion) {
            fade.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
        } else {
            dragY.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
            fade.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 500.dp, max = 570.dp)
            .padding(top = 2.dp),
    ) {
        if (candidate == null) {
            VantafynGlassPanel(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(24.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(VantafynGradients.accentHorizontal()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(32.dp))
                    }
                    Text("Nothing left to match", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text("Shuffle the deck or loosen the match style.", color = VantafynColors.Muted, textAlign = TextAlign.Center)
                    VantafynButton("Shuffle deck", onRefresh, modifier = Modifier.fillMaxWidth())
                }
            }
            return@Box
        }

        val dragProgress = (abs(dragX.value) / 230f).coerceIn(0f, 1f)
        val actionAlpha = (dragProgress * 1.25f).coerceIn(0f, 1f)
        val actionScale = 0.88f + dragProgress * 0.18f
        val nextCardScale by animateFloatAsState(
            targetValue = 0.94f + dragProgress * 0.045f,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            label = "watchPartyNextScale",
        )
        val nextCardAlpha by animateFloatAsState(
            targetValue = if (nextCandidate == null) 0f else 0.46f + dragProgress * 0.20f,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            label = "watchPartyNextAlpha",
        )
        val voteAndThrow: (WatchPartyVoteValue) -> Unit = { value ->
            if (!isCommitting) {
                isCommitting = true
                scope.launch {
                    val direction = if (value == WatchPartyVoteValue.Yes) 1f else -1f
                    if (reducedMotion) {
                        fade.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                    } else {
                        dragX.animateTo(direction * 980f, tween(320, easing = FastOutSlowInEasing))
                        fade.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
                    }
                    onVote(value)
                }
            }
        }

        if (nextCandidate != null) {
            WatchPartySwipeCard(
                candidate = nextCandidate,
                remaining = (remaining - 1).coerceAtLeast(0),
                dragX = 0f,
                dragY = 22f - dragProgress * 18f,
                rotation = 0f,
                alpha = nextCardAlpha,
                scale = nextCardScale,
                actionAlpha = 0f,
                actionScale = 1f,
                onVote = null,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .graphicsLayer {
                    translationX = dragX.value
                    translationY = dragY.value
                    rotationZ = if (reducedMotion) 0f else (dragX.value / 34f).coerceIn(-9f, 9f)
                    scaleX = 1f - dragProgress * 0.018f
                    scaleY = 1f - dragProgress * 0.018f
                    alpha = fade.value
                }
                .clip(RoundedCornerShape(34.dp))
                .background(VantafynColors.Graphite)
                .pointerInput(cardKey) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    dragX.value > 170f -> voteAndThrow(WatchPartyVoteValue.Yes)
                                    dragX.value < -170f -> voteAndThrow(WatchPartyVoteValue.No)
                                    else -> {
                                        dragX.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                                        dragY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 460f))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                dragX.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 430f))
                                dragY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 430f))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (!isCommitting) {
                                scope.launch {
                                    dragX.snapTo(dragX.value + dragAmount.x)
                                    dragY.snapTo(dragY.value + dragAmount.y * 0.72f)
                                }
                            }
                        },
                    )
                },
        ) {
            WatchPartySwipeCardContent(
                candidate = candidate,
                remaining = remaining,
                dragX = dragX.value,
                actionAlpha = actionAlpha,
                actionScale = actionScale,
                onVote = voteAndThrow,
            )
        }
    }
}

@Composable
private fun WatchPartySwipeCard(
    candidate: WatchPartyCandidate,
    remaining: Int,
    dragX: Float,
    dragY: Float,
    rotation: Float,
    alpha: Float,
    scale: Float,
    actionAlpha: Float,
    actionScale: Float,
    onVote: ((WatchPartyVoteValue) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = dragX
                translationY = dragY
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(34.dp))
            .background(VantafynColors.Graphite),
    ) {
        WatchPartySwipeCardContent(
            candidate = candidate,
            remaining = remaining,
            dragX = dragX,
            actionAlpha = actionAlpha,
            actionScale = actionScale,
            onVote = onVote,
        )
    }
}

@Composable
private fun WatchPartySwipeCardContent(
    candidate: WatchPartyCandidate,
    remaining: Int,
    dragX: Float,
    actionAlpha: Float,
    actionScale: Float,
    onVote: ((WatchPartyVoteValue) -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        val picking = dragX >= 0f
        val actionColor = if (picking) Color(0xFF68E6FF) else Color(0xFFFF7EA8)
        Text(
            text = if (picking) "YES" else "NO",
            color = actionColor,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(if (picking) Alignment.TopStart else Alignment.TopEnd)
                .padding(24.dp)
                .graphicsLayer {
                    alpha = actionAlpha
                    scaleX = actionScale
                    scaleY = actionScale
                    shadowElevation = 12f * actionAlpha
                },
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = candidate.imageUrl,
                    contentDescription = candidate.title,
                    modifier = Modifier
                        .width(88.dp)
                        .height(132.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(candidate.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(candidate.watchPartyMetaLine(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOfNotNull(candidate.itemType?.searchGroupLabel(), candidate.officialRating, candidate.runtimeMinutes?.let { "$it min" })
                            .take(4)
                            .forEach { SoftBadge(it) }
                    }
                }
            }
            Text(candidate.overview ?: "No overview from Jellyfin.", color = VantafynColors.Ink.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
            Text("$remaining waiting", color = VantafynColors.Muted)
            if (onVote != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WatchPartyVoteButton("Pass", Color(0xFFFF7EA8), modifier = Modifier.weight(1f)) { onVote(WatchPartyVoteValue.No) }
                    WatchPartyVoteButton("Pick", Color(0xFF68E6FF), modifier = Modifier.weight(1f)) { onVote(WatchPartyVoteValue.Yes) }
                }
            }
        }
    }
}

@Composable
private fun WatchPartyMatchCelebration(
    candidate: WatchPartyCandidate,
    onStartMatched: () -> Unit,
) {
    val burst = remember(candidate.id) { Animatable(0f) }
    LaunchedEffect(candidate.id) {
        burst.snapTo(0f)
        burst.animateTo(1f, tween(1450, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                        listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.56f),
                            Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        MatchConfetti(progress = burst.value, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .graphicsLayer {
                    alpha = burst.value.coerceIn(0f, 1f)
                    scaleX = 0.92f + burst.value * 0.08f
                    scaleY = 0.92f + burst.value * 0.08f
                }
                .clip(RoundedCornerShape(999.dp))
                .background(VantafynGradients.accentHorizontal())
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text("It's a match", color = VantafynColors.Ink, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = candidate.imageUrl,
                contentDescription = candidate.title,
                modifier = Modifier
                    .width(132.dp)
                    .height(198.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.2.dp),
                contentScale = ContentScale.Crop,
            )
            Text(
                "It's a match",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = burst.value.coerceIn(0f, 1f)
                    translationY = (1f - burst.value) * 20f
                },
            )
            Text(
                candidate.title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                candidate.watchPartyMetaLine(),
                color = VantafynColors.Muted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            VantafynButton("Start watching", onStartMatched, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MatchConfetti(progress: Float, modifier: Modifier = Modifier) {
    val colors = listOf(Color(0xFF58D7FF), Color(0xFF9B5CFF), Color(0xFFFF7EA8), Color.White)
    Canvas(modifier = modifier) {
        val clamped = progress.coerceIn(0f, 1f)
        repeat(22) { index ->
            val direction = if (index % 2 == 0) 1f else -1f
            val x = size.width * ((index * 37 % 100) / 100f)
            val startY = size.height * 0.12f
            val y = startY + size.height * 0.44f * clamped + (index % 5) * 9f
            val drift = direction * (18f + (index % 7) * 8f) * clamped
            val alpha = (1f - clamped * 0.72f).coerceIn(0f, 0.82f)
            drawCircle(
                color = colors[index % colors.size].copy(alpha = alpha),
                radius = (3.5f + (index % 4)).dp.toPx(),
                center = Offset(x + drift, y),
            )
        }
    }
}

@Composable
private fun WatchPartyVoteButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
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
    onDownloads: () -> Unit,
    onWatchParty: () -> Unit,
    onHomeLayout: () -> Unit,
    onPlaybackPreferences: () -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectThemeMusicVolume: (ThemeMusicVolume) -> Unit,
    onSetBottomRailAccent: (BottomRailAccent) -> Unit,
    onToggleAutoLoginLastProfile: () -> Unit,
    onSwitchUser: () -> Unit,
    onAddProfile: () -> Unit,
    onPairTv: () -> Unit = {},
    onSendTextToTv: () -> Unit = {},
    onQuickConnect: () -> Unit,
    onLogout: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onSelectTheme: (VantafynThemePreset) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onUploadProfileImage: (ByteArray, String) -> Unit,
    onDeleteProfileImage: () -> Unit,
    notificationPermissionState: VantafynPermissionUiState,
    onNotificationPermissionAction: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onMarkWhatsNewSeen: () -> Unit,
    onToggleWhatsNew: () -> Unit,
    onToggleAchievementsEnabled: () -> Unit = {},
    onToggleSocialEnabled: () -> Unit = {},
    onToggleSocialDockEnabled: () -> Unit = {},
    onDiscoverVantafyn: () -> Unit,
    viewModel: VantafynHomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var soundEffectsOn by remember {
        mutableStateOf(dev.vantafyn.core.ui.VantafynSoundEffects.isSoundEffectsEnabled(context))
    }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showPairTvSheet by remember { mutableStateOf(false) }
    var permissionDetail by remember { mutableStateOf<PermissionDetail?>(null) }
    var revealActive by remember(state.session?.profileId) { mutableStateOf(true) }
    LaunchedEffect(state.session?.profileId) {
        revealActive = true
        delay(1_100L)
        revealActive = false
    }
    val avatarPicker = rememberProfileImagePicker(onUploadProfileImage)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
            item {
                HomeContentReveal(index = 0, animate = revealActive) {
                    Text("Settings", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                HomeContentReveal(index = 1, animate = revealActive) {
                    ProfileDashboardCard(
                        state = state,
                        onChangePhoto = { avatarPicker.open() },
                        onRemovePhoto = onDeleteProfileImage,
                    )
                }
            }
            if (state.isProfileImageSaving || state.profileImageError != null) {
                item {
                    HomeContentReveal(index = 2, animate = revealActive) {
                        ProfileImageStatusCard(
                            isSaving = state.isProfileImageSaving,
                            error = state.profileImageError,
                        )
                    }
                }
            }
            item {
                HomeContentReveal(index = 3, animate = revealActive) {
                    GlassPanel {
                        Text("Appearance", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        ThemeSelector(selected = state.selectedTheme, onSelect = onSelectTheme)
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
                        BottomRailAccentSettings(
                            selected = state.bottomRailAccent,
                            onSelect = onSetBottomRailAccent,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = {
                                    val next = !soundEffectsOn
                                    soundEffectsOn = next
                                    dev.vantafyn.core.ui.VantafynSoundEffects.setSoundEffectsEnabled(context, next)
                                })
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsRowIcon(Icons.Rounded.VolumeUp)
                            Text(
                                "Soundscapes",
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            VantafynPremiumSwitchVisual(checked = soundEffectsOn)
                        }
                    }
                }
            }
            item {
                HomeContentReveal(index = 4, animate = revealActive) {
                    GlassPanel {
                        Text("Profile", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        SettingsRow("Switch User", "", onSwitchUser, compact = true, icon = Icons.Rounded.SwitchAccount)
                        PremiumToggleRow(
                            title = "Use last profile on launch",
                            subtitle = "",
                            checked = state.autoLoginLastProfile,
                            onClick = onToggleAutoLoginLastProfile,
                            icon = Icons.Rounded.Person,
                        )
                        SettingsRow("Add Profile", "", onAddProfile, compact = true, icon = Icons.Rounded.PersonAdd)
                        SettingsRow("Pair a TV", "", { showPairTvSheet = true }, compact = true, icon = Icons.Rounded.Tv)
                        SettingsRow("Send text to TV", "", onSendTextToTv, compact = true, icon = Icons.Rounded.Send)
                        SettingsRow("Quick Connect", "", onQuickConnect, compact = true, icon = Icons.Rounded.Link)
                        SettingsRow("Change Password", "", { showPasswordDialog = true }, compact = true, icon = Icons.Rounded.Lock)
                        SettingsRow("Log Out", "", onLogout, compact = true, destructive = true, icon = Icons.Rounded.Logout)
                    }
                }
            }
            item {
                HomeContentReveal(index = 5, animate = revealActive) {
                    GlassPanel {
                        Text("Permissions", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        PermissionStatusGrid(
                            notificationPermissionState = notificationPermissionState,
                            onShowPermission = { permissionDetail = it },
                        )
                    }
                }
            }
            item {
                HomeContentReveal(index = 6, animate = revealActive) {
                    GlassPanel {
                        Text("Vantafyn", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        WhatsNewSettingsRow(
                            enabled = state.whatsNewEnabled,
                            hasUnseen = state.hasUnseenWhatsNew,
                            itemCount = remember(state.whatsNewItems) { groupWhatsNewItems(state.whatsNewItems).size },
                            onToggle = onToggleWhatsNew,
                            onClick = { showWhatsNew = true },
                        )
                        PremiumToggleRow(
                            title = "Achievement Badges",
                            subtitle = "",
                            checked = state.achievementsEnabled,
                            onClick = onToggleAchievementsEnabled,
                            icon = Icons.Rounded.EmojiEvents,
                        )
                        if (state.achievementsEnabled && state.isAchievementsAvailable) {
                            PremiumToggleRow(
                                title = "Friends & Messaging",
                                subtitle = "",
                                checked = state.socialEnabled,
                                onClick = onToggleSocialEnabled,
                                icon = Icons.Rounded.PeopleOutline,
                            )
                            if (state.socialEnabled) {
                                PremiumToggleRow(
                                    title = "Floating Chat Bubble",
                                    subtitle = "",
                                    checked = state.socialDockEnabled,
                                    onClick = onToggleSocialDockEnabled,
                                    icon = Icons.Rounded.Forum,
                                )
                            }
                        }
                        if (state.session?.user?.isAdministrator == true) {
                            SettingsRow("Admin", "", onAdmin, compact = true, icon = Icons.Rounded.AdminPanelSettings)
                        }
                        if (state.session?.user?.isAdministrator == true || state.ombiRequestsEnabledForUsers) {
                            SettingsRow("Integrations & Requests", "", onRequests, compact = true, icon = Icons.Rounded.Apps)
                        }
                        SettingsRow("Downloads", "", onDownloads, compact = true, icon = Icons.Rounded.Download)
                        SettingsRow("Playback Preferences", "", onPlaybackPreferences, compact = true, icon = Icons.Rounded.Tune)
                        SettingsRow("Watch Party", "", onWatchParty, compact = true, icon = Icons.Rounded.Groups)
                        SettingsRow("Discover Vantafyn", "", onDiscoverVantafyn, compact = true, icon = Icons.Rounded.AutoAwesome)
                        SettingsRow("App version $VANTAFYN_APP_VERSION", "", { showVersionDialog = true }, compact = true, icon = Icons.Rounded.Info)
                    }
                }
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
    if (showVersionDialog) {
        AppVersionDialog(onDismiss = { showVersionDialog = false })
    }
    permissionDetail?.let { detail ->
        PermissionDetailDialog(
            detail = detail,
            notificationPermissionState = notificationPermissionState,
            onDismiss = { permissionDetail = null },
            onNotificationAction = {
                permissionDetail = null
                onNotificationPermissionAction()
            },
        )
    }
    if (showWhatsNew) {
        WhatsNewModal(
            items = state.whatsNewItems,
            onDismiss = {
                showWhatsNew = false
                onMarkWhatsNewSeen()
            },
            onOpenItem = { id ->
                showWhatsNew = false
                onMarkWhatsNewSeen()
                onOpenMedia(id)
            },
        )
    }
    if (showPairTvSheet) {
        dev.vantafyn.feature.home.pairing.MobilePairTvSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = { showPairTvSheet = false },
        )
    }
    avatarPicker.Content()
}

@Composable
private fun DeviceQuickConnectScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onAuthorize: () -> Unit,
) {
    var revealActive by remember(state.session?.profileId) { mutableStateOf(true) }
    LaunchedEffect(state.session?.profileId) {
        revealActive = true
        delay(1_100L)
        revealActive = false
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            HomeContentReveal(index = 0, animate = revealActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactBackButton(onClick = onBack)
                    Text(
                        "Quick Connect",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        item {
            HomeContentReveal(index = 1, animate = revealActive) {
                GlassPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(VantafynColors.Surface.copy(alpha = 0.78f))
                                .vantafynAnimatedModalBorder(cornerRadius = 18.dp, strokeWidth = 1.1.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(25.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                "Authorize another device",
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Enter the code shown on your other Jellyfin device. This will not change the profile signed into Vantafyn.",
                                color = VantafynColors.Muted,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    VantafynTextField(
                        value = state.deviceQuickConnectCode,
                        onValueChange = onCodeChanged,
                        label = "Device code",
                        placeholder = "ABC123",
                        enabled = !state.isDeviceQuickConnectAuthorizing,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                    state.deviceQuickConnectMessage?.let { message ->
                        DeviceQuickConnectStatusCard(
                            title = "Device authorized",
                            message = message,
                            icon = Icons.Rounded.CheckCircle,
                            tint = Color(0xFF55F0C0),
                        )
                    }
                    state.deviceQuickConnectError?.let { message ->
                        DeviceQuickConnectStatusCard(
                            title = "Could not authorize",
                            message = message,
                            icon = Icons.Rounded.ErrorOutline,
                            tint = Color(0xFFFFB5BE),
                        )
                    }
                    VantafynButton(
                        text = if (state.isDeviceQuickConnectAuthorizing) "Authorizing..." else "Authorize Device",
                        onClick = onAuthorize,
                        enabled = !state.isDeviceQuickConnectAuthorizing && state.deviceQuickConnectCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceQuickConnectStatusCard(
    title: String,
    message: String,
    icon: ImageVector,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(VantafynColors.Surface.copy(alpha = 0.68f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    message,
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private sealed class WhatsNewItem {
    abstract val id: java.util.UUID
    abstract val title: String
    abstract val imageUrl: String?
    abstract val subtitle: String

    data class Single(
        override val id: java.util.UUID,
        override val title: String,
        override val imageUrl: String?,
        override val subtitle: String,
        val item: JellyfinMediaItem,
    ) : WhatsNewItem()

    data class SeriesGroup(
        override val id: java.util.UUID,
        override val title: String,
        override val imageUrl: String?,
        override val subtitle: String,
        val seriesItem: JellyfinMediaItem?,
        val episodes: List<JellyfinMediaItem>,
    ) : WhatsNewItem()
}

private fun groupWhatsNewItems(items: List<JellyfinMediaItem>): List<WhatsNewItem> {
    val result = mutableListOf<WhatsNewItem>()
    val episodes = items.filter { it.itemType == "Episode" }
    val nonEpisodes = items.filter { it.itemType != "Episode" }

    val episodesBySeries = episodes.filter { it.seriesId != null }.groupBy { it.seriesId!! }

    for ((seriesId, eps) in episodesBySeries) {
        val sortedEps = eps.sortedWith(
            compareByDescending<JellyfinMediaItem> { it.seasonNumber ?: 0 }
                .thenByDescending { it.episodeNumber ?: 0 },
        )
        val seriesEntry = nonEpisodes.find { it.id == seriesId && it.itemType == "Series" }

        if (sortedEps.size > 3) {
            val subtitle = "${sortedEps.size} new episode${if (sortedEps.size != 1) "s" else ""}"
            result.add(
                WhatsNewItem.SeriesGroup(
                    id = seriesId,
                    title = seriesEntry?.title ?: sortedEps.first().title.substringBefore(" - ").substringBefore(":"),
                    imageUrl = seriesEntry?.imageUrl ?: sortedEps.first().imageUrl,
                    subtitle = subtitle,
                    seriesItem = seriesEntry,
                    episodes = sortedEps,
                ),
            )
        } else {
            for (ep in sortedEps) {
                val s = ep.seasonNumber
                val e = ep.episodeNumber
                val epLabel = if (s != null && e != null) "S%02dE%02d".format(s, e) else ep.itemType ?: "Episode"
                val epName = ep.title.substringAfter(" - ").substringAfter(": ").trim()
                val displaySubtitle = if (epName.isNotBlank() && epName != ep.title) "$epLabel - $epName" else epLabel
                result.add(
                    WhatsNewItem.Single(
                        id = ep.id,
                        title = seriesEntry?.title ?: ep.title.substringBefore(" - ").substringBefore(":"),
                        imageUrl = ep.imageUrl,
                        subtitle = displaySubtitle,
                        item = ep,
                    ),
                )
            }
        }
    }

    for (item in nonEpisodes) {
        if (item.itemType == "Series" && episodesBySeries.containsKey(item.id)) continue
        val subtitle = when (item.itemType) {
            "Movie" -> item.year?.toString() ?: "Movie"
            "MusicAlbum" -> "Album"
            "Series" -> "Series"
            else -> item.itemType ?: ""
        }
        result.add(
            WhatsNewItem.Single(
                id = item.id,
                title = item.title,
                imageUrl = item.imageUrl,
                subtitle = subtitle,
                item = item,
            ),
        )
    }

    return result
}

@Composable
private fun WatchedCheckBadge(
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val infiniteTransition = rememberInfiniteTransition(label = "watchedBadge")
    val shift by if (isResumed) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "watchedBadgeShift",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val glassShape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier.size(width = 24.dp, height = 20.dp),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = VantafynGradients.AccentColors + VantafynGradients.AccentColors.first(),
                    start = Offset(-size.width * shift, -size.height * shift),
                    end = Offset(size.width * (1f - shift), size.height * (1f - shift)),
                    tileMode = TileMode.Repeated,
                ),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(1.dp)
                .clip(glassShape)
                .background(VantafynColors.Graphite.copy(alpha = 0.90f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(10.dp)) {
                val w = size.width
                val h = size.height
                drawPath(
                    path = Path().apply {
                        moveTo(w * 0.15f, h * 0.50f)
                        lineTo(w * 0.40f, h * 0.80f)
                        lineTo(w * 0.85f, h * 0.20f)
                    },
                    color = Color.White,
                    style = Stroke(width = w * 0.25f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Composable
private fun UnwatchedCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val displayText = if (count > 999) "999+" else count.toString()
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val infiniteTransition = rememberInfiniteTransition(label = "unwatchedBadge")
    val shift by if (isResumed) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "unwatchedBadgeShift",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val glassShape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier.size(width = 30.dp, height = 22.dp),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = VantafynGradients.AccentColors + VantafynGradients.AccentColors.first(),
                    start = Offset(-size.width * shift, -size.height * shift),
                    end = Offset(size.width * (1f - shift), size.height * (1f - shift)),
                    tileMode = TileMode.Repeated,
                ),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(1.dp)
                .clip(glassShape)
                .background(VantafynColors.Graphite.copy(alpha = 0.90f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayText,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.offset(y = (-1.5).dp),
            )
        }
    }
}

@Composable
private fun WhatsNewGradientDot(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val shift by if (isResumed) {
        val infiniteTransition = rememberInfiniteTransition(label = "whatsNewDot")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "whatsNewDotShift",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .shadow(2.dp, RoundedCornerShape(999.dp))
            .drawWithContent {
                if (isResumed) {
                    val start = Offset(-size.width * shift, -size.height * shift)
                    val end = Offset(size.width * (1f - shift), size.height * (1f - shift))
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = VantafynGradients.AccentColors + VantafynGradients.AccentColors.first(),
                            start = start,
                            end = end,
                            tileMode = TileMode.Repeated,
                        ),
                        radius = size.minDimension / 2f,
                    )
                } else {
                    drawCircle(
                        color = Color(0xFF8EA2FF),
                        radius = size.minDimension / 2f,
                    )
                }
            },
    )
}

@Composable
private fun WhatsNewSettingsRow(enabled: Boolean, hasUnseen: Boolean, itemCount: Int, onToggle: () -> Unit, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VantafynGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(VantafynColors.Graphite.copy(alpha = 0.78f))
                        .drawWithContent {
                            drawContent()
                            drawRoundRect(
                                brush = VantafynGradients.accentHorizontal(),
                                cornerRadius = CornerRadius(13.dp.toPx()),
                                style = Stroke(width = 1.05.dp.toPx()),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NewReleases,
                        contentDescription = null,
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("What's New", color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (enabled && hasUnseen) WhatsNewGradientDot()
                    }
                    Text(
                        if (enabled && itemCount > 0) "$itemCount update${if (itemCount != 1) "s" else ""} ready" else if (enabled) "No new updates" else "Hidden",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggle,
                        )
                        .padding(2.dp),
                ) {
                    VantafynPremiumSwitchVisual(checked = enabled)
                }
            }
        }
        if (enabled && itemCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(VantafynColors.Surface.copy(alpha = 0.42f))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 13.dp, vertical = 9.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "View latest changes",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "<",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.graphicsLayer { rotationZ = 180f },
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatsNewModal(
    items: List<JellyfinMediaItem>,
    onDismiss: () -> Unit,
    onOpenItem: (java.util.UUID) -> Unit,
) {
    val grouped = remember(items) { groupWhatsNewItems(items) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
        titleContentColor = VantafynColors.Ink,
        textContentColor = VantafynColors.Muted,
        modifier = Modifier.vantafynAnimatedModalBorder(),
        title = {
            Text("What's New", fontWeight = FontWeight.SemiBold)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
                contentPadding = PaddingValues(vertical = VantafynSpacing.xs),
            ) {
                items(grouped, key = { it.id }) { whatsNewItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                when (whatsNewItem) {
                                    is WhatsNewItem.Single -> onOpenItem(whatsNewItem.item.id)
                                    is WhatsNewItem.SeriesGroup -> {
                                        val targetId = whatsNewItem.seriesItem?.id ?: whatsNewItem.episodes.firstOrNull()?.seriesId
                                        if (targetId != null) onOpenItem(targetId)
                                    }
                                }
                            }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF24304D), Color(0xFF393456), VantafynColors.SurfaceHigh))),
                            contentAlignment = Alignment.Center,
                        ) {
                            val artwork = whatsNewItem.imageUrl
                            if (artwork != null) {
                                AsyncImage(
                                    model = artwork,
                                    contentDescription = whatsNewItem.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    initials(whatsNewItem.title),
                                    color = VantafynColors.Ink.copy(alpha = 0.78f),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                whatsNewItem.title,
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                whatsNewItem.subtitle,
                                color = VantafynColors.Muted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun ProfileDashboardCard(
    state: VantafynHomeUiState,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    GlassPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            val imageUrl = state.savedProfiles.firstOrNull { it.jellyfinUserId == state.session?.user?.id }?.imageUrl
            EditableProfileAvatar(
                name = state.session?.user?.name ?: "Vantafyn User",
                imageUrl = imageUrl,
                modifier = Modifier.size(92.dp),
                onClick = onChangePhoto,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), verticalAlignment = Alignment.Top) {
                    Text(
                        state.session?.user?.name ?: "Vantafyn User",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.session?.user?.isAdministrator == true) {
                        SoftBadge("Admin")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        state.server?.name ?: "Jellyfin Server",
                        color = VantafynColors.Ink.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.server?.url ?: "",
                        color = VantafynColors.Muted.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
private fun ThemeSelector(
    selected: VantafynThemePreset,
    onSelect: (VantafynThemePreset) -> Unit,
) {
    val reducedMotion = rememberReducedMotionPreference()
    val selectedIndex = VantafynThemePreset.entries.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val coroutineScope = rememberCoroutineScope()
    val cardSpacing = 10.dp

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingsRowIcon(Icons.Rounded.AutoAwesome)
            Column(Modifier.weight(1f)) {
                Text("Theme", color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Colour system", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(VantafynColors.Surface.copy(alpha = 0.58f))
                    .border(
                        BorderStroke(1.dp, Brush.horizontalGradient(VantafynGradients.AccentColors.map { it.copy(alpha = 0.46f) })),
                        RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    selected.label,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.08f to Color.Black,
                                0.92f to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                ) {
                    itemsIndexed(VantafynThemePreset.entries, key = { _, preset -> preset.id }) { index, preset ->
                        val emphasis = if (preset == selected) 1f else 0f
                        ThemePreviewCard(
                            preset = preset,
                            selected = preset == selected,
                            emphasis = emphasis,
                            onClick = {
                                onSelect(preset)
                                coroutineScope.launch {
                                    if (!reducedMotion) {
                                        listState.animateScrollToItem(index)
                                    } else {
                                        listState.scrollToItem(index)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    preset: VantafynThemePreset,
    selected: Boolean,
    emphasis: Float,
    onClick: () -> Unit,
) {
    val tokens = remember(preset) { tokensFor(preset) }
    val shape = RoundedCornerShape(18.dp)
    val targetScale = if (selected) 1.01f + (emphasis * 0.015f) else 0.982f + (emphasis * 0.018f)
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.86f),
        label = "themePreviewScale",
    )
    val depthAlpha by animateFloatAsState(
        targetValue = if (selected) 0.18f + (emphasis * 0.10f) else 0.06f + (emphasis * 0.06f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "themePreviewDepth",
    )
    Box(
        modifier = Modifier
            .width(150.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (selected) 8f + (emphasis * 4f) else emphasis * 3f
            },
    ) {
        VantafynGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (selected) {
                        Modifier.vantafynAnimatedModalBorder(cornerRadius = 18.dp, strokeWidth = 1.25.dp, durationMillis = 5400)
                    } else {
                        Modifier.clip(shape)
                    },
                )
                .clickable(onClick = onClick),
            variant = VantafynGlassVariant.Card,
            selected = selected,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(tokens.backgroundGradient))
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.linearGradient(tokens.accentColors.map { it.copy(alpha = depthAlpha) }),
                            ),
                            RoundedCornerShape(15.dp),
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        tokens.glassEdgeWhite.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        tokens.graphite.copy(alpha = 0.62f),
                                    ),
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(y = (-5).dp)
                            .padding(start = 12.dp)
                            .width(62.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        tokens.glassEdgeWhite.copy(alpha = 0.14f),
                                        tokens.glassNavyLift.copy(alpha = 0.80f),
                                        tokens.glassVioletLift.copy(alpha = 0.42f),
                                    ),
                                ),
                            )
                            .border(
                                BorderStroke(1.dp, Brush.linearGradient(tokens.accentColors.map { it.copy(alpha = 0.68f) })),
                                RoundedCornerShape(12.dp),
                            ),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(9.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tokens.accentColors.take(3).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(color.copy(alpha = 0.82f)),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.horizontalGradient(tokens.accentColors)),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        preset.label,
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = VantafynColors.Secondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    if (selected) "Active now" else preset.themeMoodLabel(),
                    color = if (selected) VantafynColors.Secondary else VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun VantafynThemePreset.themeMoodLabel(): String =
    when (this) {
        VantafynThemePreset.Nebula -> "Cinematic blue"
        VantafynThemePreset.Midnight -> "Cool graphite"
        VantafynThemePreset.Aurora -> "Teal aurora"
        VantafynThemePreset.Amethyst -> "Violet glass"
        VantafynThemePreset.Ember -> "Warm ember"
        VantafynThemePreset.Oled -> "True black"
    }

@Composable
private fun BackgroundSelector(selected: VantafynAppBackground, onSelect: (VantafynAppBackground) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VantafynAppBackground.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.08f))
                    .border(
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            brush = if (isSelected) {
                                VantafynGradients.accentHorizontal()
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.14f),
                                        Color.White.copy(alpha = 0.14f),
                                    ),
                                )
                            },
                        ),
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
            .heightIn(min = 64.dp)
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
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Box(
                modifier = Modifier,
            ) { VantafynPremiumSwitchVisual(checked = checked) }
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
                modifier = Modifier,
            ) { VantafynPremiumSwitchVisual(checked = checked) }
        }
        ThemeMusicVolumeSelector(
            selected = selected,
            enabled = checked,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun VantafynPremiumSwitchVisual(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
private fun PermissionStatusGrid(
    notificationPermissionState: VantafynPermissionUiState,
    onShowPermission: (PermissionDetail) -> Unit,
) {
    val notificationTone = when (notificationPermissionState.status) {
        VantafynPermissionStatus.Granted,
        VantafynPermissionStatus.Unsupported -> PermissionTone.Allowed
        VantafynPermissionStatus.NotRequested -> PermissionTone.Optional
        VantafynPermissionStatus.Denied,
        VantafynPermissionStatus.PermanentlyDenied -> PermissionTone.NeedsAttention
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermissionStatusRow(
            title = "Internet",
            state = "Allowed",
            icon = Icons.Rounded.Cloud,
            tone = PermissionTone.Allowed,
            onClick = { onShowPermission(PermissionDetail.Internet) },
        )
        PermissionStatusRow(
            title = "Downloads service",
            state = "Allowed",
            icon = Icons.Rounded.Download,
            tone = PermissionTone.Allowed,
            onClick = { onShowPermission(PermissionDetail.DownloadsService) },
        )
        PermissionStatusRow(
            title = "Music service",
            state = "Allowed",
            icon = Icons.Rounded.MusicNote,
            tone = PermissionTone.Allowed,
            onClick = { onShowPermission(PermissionDetail.MusicService) },
        )
        PermissionStatusRow(
            title = "Notifications",
            state = notificationPermissionState.statusLabel,
            icon = Icons.Rounded.Notifications,
            tone = notificationTone,
            onClick = { onShowPermission(PermissionDetail.Notifications) },
        )
    }
}

private enum class PermissionDetail(
    val title: String,
    val body: String,
    val icon: ImageVector,
    val stateLabel: String,
) {
    Internet(
        title = "Internet",
        body = "Connects to your Jellyfin server, Ombi, Cast devices, artwork, and streaming media.",
        icon = Icons.Rounded.Cloud,
        stateLabel = "Allowed",
    ),
    DownloadsService(
        title = "Downloads service",
        body = "Keeps active downloads reliable when Android moves Vantafyn to the background.",
        icon = Icons.Rounded.Download,
        stateLabel = "Allowed",
    ),
    MusicService(
        title = "Music service",
        body = "Keeps music playback alive for lock-screen controls, headphones, and Android Auto.",
        icon = Icons.Rounded.MusicNote,
        stateLabel = "Allowed",
    ),
    Notifications(
        title = "Notifications",
        body = "Shows Android music controls while audio is playing. Vantafyn does not use notifications for ads or tracking.",
        icon = Icons.Rounded.Notifications,
        stateLabel = "Media controls",
    ),
}

@Composable
private fun PermissionDetailDialog(
    detail: PermissionDetail,
    notificationPermissionState: VantafynPermissionUiState,
    onDismiss: () -> Unit,
    onNotificationAction: () -> Unit,
) {
    val isNotifications = detail == PermissionDetail.Notifications
    val status = if (isNotifications) notificationPermissionState.statusLabel else detail.stateLabel
    val primaryText = when {
        !isNotifications -> "Done"
        notificationPermissionState.status == VantafynPermissionStatus.Granted ||
            notificationPermissionState.status == VantafynPermissionStatus.Unsupported -> "Done"
        notificationPermissionState.status == VantafynPermissionStatus.PermanentlyDenied -> "Open Android Settings"
        else -> "Allow controls"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        VantafynGlassModalPanel(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.25.dp),
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(22.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(999.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = detail.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text(
                    detail.title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                SoftBadge(status)
                Text(
                    detail.body,
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                )
                VantafynButton(
                    text = primaryText,
                    onClick = if (isNotifications &&
                        notificationPermissionState.status != VantafynPermissionStatus.Granted &&
                        notificationPermissionState.status != VantafynPermissionStatus.Unsupported
                    ) {
                        onNotificationAction
                    } else {
                        onDismiss
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (primaryText != "Done") {
                    TextButton(onClick = onDismiss) {
                        Text("Not now", color = VantafynColors.Muted)
                    }
                }
            }
        }
    }
}

private enum class PermissionTone {
    Allowed,
    Optional,
    NeedsAttention,
}

@Composable
private fun PermissionStatusRow(
    title: String,
    state: String,
    icon: ImageVector,
    tone: PermissionTone,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = VantafynSpacing.md, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowIcon(icon, destructive = tone == PermissionTone.NeedsAttention)
            Text(
                title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(permissionToneBackground(tone))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state,
                    color = permissionToneText(tone),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun permissionToneBackground(tone: PermissionTone): Color = when (tone) {
    PermissionTone.Allowed -> Color(0xFF48D8A3).copy(alpha = 0.16f)
    PermissionTone.Optional -> Color(0xFF7B8DFF).copy(alpha = 0.16f)
    PermissionTone.NeedsAttention -> Color(0xFFFFA24D).copy(alpha = 0.18f)
}

private fun permissionToneText(tone: PermissionTone): Color = when (tone) {
    PermissionTone.Allowed -> Color(0xFF9EF4CE)
    PermissionTone.Optional -> Color(0xFFC8D2FF)
    PermissionTone.NeedsAttention -> Color(0xFFFFD0A3)
}

@Composable
private fun AppVersionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier
            .imePadding()
            .vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = CoreUiR.drawable.vantafyn_logo),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    "Vantafyn",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(VantafynGradients.accentHorizontal())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Version $VANTAFYN_APP_VERSION", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Built with love for people who want more from Jellyfin — one beautiful, unified experience for all their media.",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Thank you for using Vantafyn. Every detail is shaped with care by Glowseed Studio.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "♥",
                    color = Color(0xFFFF5D73),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
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
    onSetUpNextDisplayMode: (UpNextDisplayMode) -> Unit,
    onTogglePassoutProtection: () -> Unit,
    onSetPassoutProtectionLimitMinutes: (Int) -> Unit,
    onSelectVideoPlayerPreference: (VantafynVideoPlayerPreference) -> Unit,
    onSetMaxStreamingBitrateMbps: (Int?) -> Unit,
    onSetMediaSegmentBehavior: (JellyfinMediaSegmentType, JellyfinMediaSegmentBehavior) -> Unit,
) {
    val preferences = state.editablePlaybackPreferences
    var screenRevealActive by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        screenRevealActive = true
        delay(1_450L)
        screenRevealActive = false
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            HomeContentReveal(index = 0, animate = screenRevealActive) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    CompactBackButton(onClick = onBack)
                    Text("Playback Preferences", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (state.isPlaybackPreferencesLoading) item { PlaybackPreferencesSkeleton() }
        state.playbackPreferencesError?.let { item { VantafynErrorCard(it) } }
        if (preferences != null) {
            item {
                HomeContentReveal(index = 1, animate = screenRevealActive) {
                    GlassPanel {
                        CompactSectionTitle("Video Player", Icons.Rounded.PlayCircle)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            VantafynVideoPlayerPreference.entries.forEach { option ->
                                VideoPlayerPreferenceRow(
                                    option = option,
                                    selected = state.videoPlayerPreference == option,
                                    onSelect = { onSelectVideoPlayerPreference(option) },
                                )
                            }
                        }
                        CompactSectionTitle("Maximum streaming bitrate", Icons.Rounded.Speed)
                        PreferenceChipGroup(
                            items = listOf<Int?>(null) + MAX_STREAMING_BITRATE_MBPS_OPTIONS,
                            label = { it.maxStreamingBitrateLabel() },
                            selected = { option -> state.maxStreamingBitrateMbps == option },
                            onSelect = onSetMaxStreamingBitrateMbps,
                        )
                    }
                }
            }
            item {
                HomeContentReveal(index = 2, animate = screenRevealActive) {
                    GlassPanel(modifier = Modifier.animateContentSize(animationSpec = tween(450, easing = FastOutSlowInEasing))) {
                        CompactSectionTitle("Playback", Icons.Rounded.Speed)
                        PlaybackPreferenceToggle("Autoplay next episode", preferences.enableNextEpisodeAutoPlay, Icons.Rounded.QueuePlayNext) {
                            onEdit { it.copy(enableNextEpisodeAutoPlay = !it.enableNextEpisodeAutoPlay) }
                        }
                        CompactSectionTitle("Show Up Next")
                        VantafynGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            variant = VantafynGlassVariant.Chip,
                            cornerRadius = 999.dp,
                            contentPadding = PaddingValues(4.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                UpNextDisplayMode.entries.forEach { option ->
                                    val isSelected = option == state.upNextDisplayMode
                                    val shape = RoundedCornerShape(999.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                                                } else {
                                                    Modifier.clip(shape)
                                                },
                                            )
                                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                            .clickable { onSetUpNextDisplayMode(option) }
                                            .padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = option.upNextDisplayLabel(),
                                            color = if (isSelected) VantafynColors.Ink else VantafynColors.Muted,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                        CompactSectionTitle("Countdown")
                        VantafynGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            variant = VantafynGlassVariant.Chip,
                            cornerRadius = 999.dp,
                            contentPadding = PaddingValues(4.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5, 10, 15, 30).forEach { seconds ->
                                    val isSelected = seconds == state.autoplayCountdownSeconds
                                    val shape = RoundedCornerShape(999.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                                                } else {
                                                    Modifier.clip(shape)
                                                },
                                            )
                                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                            .clickable { onSetAutoplayCountdownSeconds(seconds) }
                                            .padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${seconds}s",
                                            color = if (isSelected) VantafynColors.Ink else VantafynColors.Muted,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                        PlaybackPreferenceToggle(
                            "Passout protection",
                            state.passoutProtectionEnabled,
                            Icons.Rounded.Bedtime,
                            onClick = onTogglePassoutProtection,
                        )
                        AnimatedVisibility(
                            visible = state.passoutProtectionEnabled,
                            enter = slideInVertically(initialOffsetY = { -it / 4 }, animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)),
                            exit = slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)),
                        ) {
                            Column {
                                CompactSectionTitle("Continue Playing (hours)")
                                VantafynGlassSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = VantafynGlassVariant.Chip,
                                    cornerRadius = 999.dp,
                                    contentPadding = PaddingValues(4.dp),
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(1, 2, 3, 4, 5).forEach { hours ->
                                            val isSelected = hours == state.passoutProtectionLimitMinutes / 60
                                            val shape = RoundedCornerShape(999.dp)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                                                        } else {
                                                            Modifier.clip(shape)
                                                        },
                                                    )
                                                    .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                                    .clickable { onSetPassoutProtectionLimitMinutes(hours * 60) }
                                                    .padding(vertical = 9.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "$hours",
                                                    color = if (isSelected) VantafynColors.Ink else VantafynColors.Muted,
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
                    }
                }
            }
            item {
                HomeContentReveal(index = 3, animate = screenRevealActive) {
                    GlassPanel {
                        CompactSectionTitle("Audio", Icons.Rounded.GraphicEq)
                        PlaybackPreferenceToggle("Play default audio track", preferences.playDefaultAudioTrack, Icons.Rounded.CheckCircle) {
                            onEdit { it.copy(playDefaultAudioTrack = !it.playDefaultAudioTrack) }
                        }
                        PlaybackPreferenceToggle("Remember audio selections", preferences.rememberAudioSelections, Icons.Rounded.Bookmark) {
                            onEdit { it.copy(rememberAudioSelections = !it.rememberAudioSelections) }
                        }
                    }
                }
            }
            item {
                HomeContentReveal(index = 4, animate = screenRevealActive) {
                    GlassPanel {
                        CompactSectionTitle("Subtitles", Icons.Rounded.Subtitles)
                        PreferenceChipGroup(
                            items = listOf("Default", "Always", "OnlyForced", "None", "Smart"),
                            label = { it.subtitleModeDisplayLabel() },
                            selected = { mode -> preferences.subtitleMode.equals(mode, ignoreCase = true) },
                            onSelect = { mode ->
                                onEdit { it.copy(subtitleMode = mode) }
                            },
                        )
                        PlaybackPreferenceToggle("Remember subtitle selections", preferences.rememberSubtitleSelections, Icons.Rounded.Bookmark) {
                            onEdit { it.copy(rememberSubtitleSelections = !it.rememberSubtitleSelections) }
                        }
                    }
                }
            }
            item {
                HomeContentReveal(index = 5, animate = screenRevealActive) {
                    GlassPanel {
                        CompactSectionTitle("Languages", Icons.Rounded.Translate)
                        PlaybackLanguageField(
                            title = "Audio",
                            value = preferences.audioLanguagePreference.orEmpty(),
                            onValueChange = { value -> onEdit { it.copy(audioLanguagePreference = value) } },
                            placeholder = "eng",
                        )
                        PlaybackLanguageField(
                            title = "Subtitles",
                            value = preferences.subtitleLanguagePreference.orEmpty(),
                            onValueChange = { value -> onEdit { it.copy(subtitleLanguagePreference = value) } },
                            placeholder = "eng",
                        )
                    }
                }
            }
            item {
                HomeContentReveal(index = 6, animate = screenRevealActive) {
                    GlassPanel {
                        CompactSectionTitle("Skip Segments", Icons.Rounded.SkipNext)
                        listOf(
                            JellyfinMediaSegmentType.Intro,
                            JellyfinMediaSegmentType.Recap,
                            JellyfinMediaSegmentType.Outro,
                            JellyfinMediaSegmentType.Commercial,
                            JellyfinMediaSegmentType.Preview,
                        ).forEach { type ->
                            MediaSegmentPreferenceRow(
                                type = type,
                                selected = state.mediaSegmentBehaviors[type] ?: JellyfinMediaSegmentBehavior.DoNothing,
                                onSelect = { behavior -> onSetMediaSegmentBehavior(type, behavior) },
                            )
                        }
                    }
                }
            }
            item {
                HomeContentReveal(index = 7, animate = screenRevealActive) {
                    Media3ExtensionDecoderPanel()
                }
            }
            item {
                HomeContentReveal(index = 8, animate = screenRevealActive) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Media3ExtensionDecoderPanel() {
    val decoders = remember { VantafynMedia3ExtensionSupport.decoders }
    val availableCount = decoders.count { it.isAvailable }
    GlassPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowIcon(Icons.Rounded.Tune)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Enhanced playback",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            SoftBadge(if (availableCount > 0) "$availableCount active" else "Ready")
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            decoders.forEach { decoder ->
                SoftBadge(
                    text = decoder.label,
                    color = if (decoder.isAvailable) VantafynColors.Ink else VantafynColors.Muted,
                    background = Color.White.copy(alpha = if (decoder.isAvailable) 0.12f else 0.08f),
                )
            }
        }
    }
}

@Composable
private fun MediaSegmentPreferenceRow(
    type: JellyfinMediaSegmentType,
    selected: JellyfinMediaSegmentBehavior,
    onSelect: (JellyfinMediaSegmentBehavior) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            type.segmentPreferenceLabel(),
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        VantafynGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            variant = VantafynGlassVariant.Chip,
            cornerRadius = 999.dp,
            contentPadding = PaddingValues(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                JellyfinMediaSegmentBehavior.entries.forEach { behavior ->
                    val isSelected = behavior == selected
                    val shape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isSelected) {
                                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                                } else {
                                    Modifier.clip(shape)
                                },
                            )
                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { onSelect(behavior) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = behavior.segmentBehaviorLabel(),
                            color = if (isSelected) VantafynColors.Ink else VantafynColors.Muted,
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

private fun UpNextDisplayMode.upNextDisplayLabel(): String =
    when (this) {
        UpNextDisplayMode.BeforeEnd -> "Before the end"
        UpNextDisplayMode.AfterCompletion -> "After episode ends"
    }

private fun Int?.maxStreamingBitrateLabel(): String =
    this?.let { "${it} Mbps" } ?: "Auto / Unlimited"

private fun String.subtitleModeDisplayLabel(): String =
    when (this) {
        "OnlyForced" -> "Forced only"
        "None" -> "Off"
        else -> this
    }

@Composable
private fun DownloadsScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit,
    onRetry: (DownloadRecord) -> Unit,
    onRemove: (DownloadRecord) -> Unit,
    onRemoveAll: () -> Unit,
    onSetWifiOnlyDefault: (Boolean) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(DownloadFilter.All) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var selectedDownload by remember { mutableStateOf<DownloadRecord?>(null) }
    val visibleDownloads = remember(state.offlineDownloads, query, selectedType) {
        state.offlineDownloads
            .asSequence()
            .filter { selectedType.matches(it) }
            .filter { it.matchesOfflineDownloadQuery(query) }
            .toList()
    }
    var screenRevealActive by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        screenRevealActive = true
        delay(1_450L)
        screenRevealActive = false
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .imePadding()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
            item {
                HomeContentReveal(index = 0, animate = screenRevealActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    ) {
                        CompactBackButton(onClick = onBack)
                        Text(
                            "Downloads",
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh downloads",
                            tint = VantafynColors.Ink.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onRefresh)
                                .padding(4.dp),
                        )
                    }
                }
            }
            item {
                HomeContentReveal(index = 1, animate = screenRevealActive) {
                    DownloadStorageCard(
                        summary = state.offlineDownloadStorageSummary,
                        records = state.offlineDownloads,
                        wifiOnly = state.downloadWifiOnlyDefault,
                        onToggleWifiOnly = { onSetWifiOnlyDefault(!state.downloadWifiOnlyDefault) },
                        query = query,
                        onQueryChange = { query = it },
                        selectedFilter = selectedType,
                        onSelectFilter = { selectedType = it },
                        showRemoveAll = state.offlineDownloads.isNotEmpty(),
                        onRemoveAll = { confirmClear = true },
                    )
                }
            }
            if (state.isDownloadsLoading) {
                item {
                    HomeContentReveal(index = 2, animate = screenRevealActive) {
                        DownloadsLoadingSkeleton()
                    }
                }
            }
            state.downloadsError?.let { error ->
                item {
                    HomeContentReveal(index = 2, animate = screenRevealActive) {
                        VantafynErrorCard(error) { VantafynButton("Retry", onClick = onRefresh) }
                    }
                }
            }
            if (!state.isDownloadsLoading && state.offlineDownloads.isEmpty() && state.downloadsError == null) {
                item {
                    HomeContentReveal(index = 2, animate = screenRevealActive) {
                        LibraryItemsEmptyState(
                            title = "Nothing saved yet",
                            subtitle = "Save movies, episodes, seasons, music, and audiobooks. Completed downloads will play without a server connection.",
                            icon = Icons.Rounded.Download,
                        )
                    }
                }
            }
            if (!state.isDownloadsLoading && state.offlineDownloads.isNotEmpty() && visibleDownloads.isEmpty()) {
                item {
                    HomeContentReveal(index = 2, animate = screenRevealActive) {
                        LibraryItemsEmptyState(
                            title = "Nothing matched",
                            subtitle = "Try a different title, artist, series, or media type.",
                            icon = Icons.Rounded.Search,
                        )
                    }
                }
            }
            if (!state.isDownloadsLoading && visibleDownloads.isNotEmpty()) {
                item {
                    HomeContentReveal(index = 3, animate = screenRevealActive, revealKey = visibleDownloads.map { it.id to it.state }) {
                        OfflineDownloadsRails(
                            records = visibleDownloads,
                            onPlay = onPlay,
                            onLongPress = { selectedDownload = it },
                            onCancel = onCancel,
                            onRetry = onRetry,
                            onRemove = onRemove,
                        )
                    }
                }
            }
        }
        selectedDownload?.let { record ->
            DownloadActionsModal(
                record = record,
                onDismiss = { selectedDownload = null },
                onPlay = {
                    selectedDownload = null
                    onPlay(record)
                },
                onCancel = {
                    selectedDownload = null
                    onCancel(record)
                },
                onRetry = {
                    selectedDownload = null
                    onRetry(record)
                },
                onRemove = {
                    selectedDownload = null
                    onRemove(record)
                },
            )
        }
        if (confirmClear) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable { confirmClear = false }
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                VantafynGlassModalPanel(
                    selected = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {},
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                            SettingsRowIcon(Icons.Rounded.Delete)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Clear offline library?", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text("Downloaded files for this profile will be removed from this device.", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                            OutlinedButton(
                                onClick = { confirmClear = false },
                                modifier = Modifier.weight(1f),
                            ) { Text("Cancel") }
                            VantafynButton(
                                text = "Remove",
                                onClick = {
                                    confirmClear = false
                                    onRemoveAll()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadStorageCard(
    summary: DownloadStorageSummary?,
    records: List<DownloadRecord>,
    wifiOnly: Boolean,
    onToggleWifiOnly: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: DownloadFilter,
    onSelectFilter: (DownloadFilter) -> Unit,
    showRemoveAll: Boolean,
    onRemoveAll: () -> Unit,
) {
    GlassPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            SettingsRowIcon(Icons.Rounded.Storage)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Offline Library", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${summary?.completedCount ?: records.count { it.state == DownloadState.Completed }} ready · ${summary?.activeCount ?: 0} active",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                (summary?.totalBytes ?: records.sumOf { it.totalBytes ?: it.bytesDownloaded }).downloadSizeLabel(),
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
            DownloadStatPill("Video", records.count { it.mediaType == DownloadMediaType.Movie || it.mediaType == DownloadMediaType.Episode || it.mediaType == DownloadMediaType.Season })
            DownloadStatPill("Music", records.count { it.mediaType == DownloadMediaType.MusicTrack || it.mediaType == DownloadMediaType.MusicAlbum })
            DownloadStatPill("Books", records.count { it.mediaType == DownloadMediaType.Audiobook })
        }
        PremiumToggleRow(
            title = "Wi-Fi only downloads",
            subtitle = "",
            checked = wifiOnly,
            icon = Icons.Rounded.Cloud,
            onClick = onToggleWifiOnly,
        )
        if (records.isNotEmpty()) {
            VantafynTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "Search downloads",
                placeholder = "Movies, episodes, songs...",
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                items(DownloadFilter.entries.toList(), key = { it.name }) { filter ->
                    SelectableChip(filter.label, selectedFilter == filter) {
                        onSelectFilter(filter)
                    }
                }
            }
        }
        if (showRemoveAll) {
            VantafynGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRemoveAll),
                cornerRadius = 18.dp,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    SettingsRowIcon(Icons.Rounded.Delete)
                    Text("Remove all downloads", color = Color(0xFFFFC2C2), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DownloadStatPill(label: String, count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Text("$count $label", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OfflineDownloadsRails(
    records: List<DownloadRecord>,
    onPlay: (DownloadRecord) -> Unit,
    onLongPress: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit,
    onRetry: (DownloadRecord) -> Unit,
    onRemove: (DownloadRecord) -> Unit,
) {
    val active = remember(records) { records.filter { it.isActiveDownload() } }
    val failed = remember(records) { records.filter { it.state == DownloadState.Failed || it.state == DownloadState.Cancelled } }
    val ready = remember(records) { records.filter { it.state == DownloadState.Completed } }
    val movies = remember(ready) { ready.filter { it.mediaType == DownloadMediaType.Movie }.sortedByOfflineTitle() }
    val tv = remember(ready) {
        ready.filter { it.mediaType == DownloadMediaType.Episode || it.mediaType == DownloadMediaType.Season }
            .sortedWith(compareBy<DownloadRecord> { it.seriesName ?: it.title }
                .thenBy { it.seasonNumber ?: 0 }
                .thenBy { it.episodeNumber ?: 0 }
                .thenBy { it.title })
    }
    val playlists = remember(ready) {
        ready.filter { it.mediaType == DownloadMediaType.MusicTrack && !it.parentId.isNullOrBlank() && !it.albumName.isNullOrBlank() }
            .groupBy { it.parentId.orEmpty() }
            .values
            .map { it.sortedByOfflineTitle() }
            .sortedBy { it.firstOrNull()?.albumName ?: it.firstOrNull()?.title.orEmpty() }
    }
    val albums = remember(ready) {
        ready.filter { it.mediaType == DownloadMediaType.MusicTrack && it.parentId.isNullOrBlank() && !it.albumId.isNullOrBlank() }
            .groupBy { it.albumId.orEmpty() }
            .values
            .map { it.sortedByOfflineTitle() }
            .sortedBy { it.firstOrNull()?.albumName ?: it.firstOrNull()?.title.orEmpty() }
    }
    val looseSongs = remember(ready) {
        ready.filter {
            it.mediaType == DownloadMediaType.MusicTrack &&
                it.parentId.isNullOrBlank() &&
                it.albumId.isNullOrBlank()
        }.sortedByOfflineTitle()
    }
    val musicAlbums = remember(ready) { ready.filter { it.mediaType == DownloadMediaType.MusicAlbum }.sortedByOfflineTitle() }
    val audiobooks = remember(ready) { ready.filter { it.mediaType == DownloadMediaType.Audiobook }.sortedByOfflineTitle() }

    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xl)) {
        if (active.isNotEmpty() || failed.isNotEmpty()) {
            OfflineDownloadRecordRail(
                title = "In progress",
                records = active + failed,
                cardWidth = 230.dp,
                wide = true,
                onPlay = onPlay,
                onLongPress = onLongPress,
            )
        }
        OfflineDownloadRecordRail(
            title = "Movies",
            records = movies,
            cardWidth = 142.dp,
            wide = false,
            onPlay = onPlay,
            onLongPress = onLongPress,
        )
        OfflineDownloadRecordRail(
            title = "TV",
            records = tv,
            cardWidth = 142.dp,
            wide = false,
            onPlay = onPlay,
            onLongPress = onLongPress,
        )
        OfflineDownloadCollectionRail(
            title = "Playlists",
            groups = playlists,
            onPlay = onPlay,
            onLongPress = onLongPress,
        )
        OfflineDownloadCollectionRail(
            title = "Albums",
            groups = albums,
            onPlay = onPlay,
            onLongPress = onLongPress,
        )
        OfflineDownloadRecordRail(
            title = "Saved albums",
            records = musicAlbums,
            cardWidth = 142.dp,
            wide = false,
            onPlay = onPlay,
            onLongPress = onLongPress,
        )
        OfflineDownloadRecordRail(
            title = "Songs",
            records = looseSongs,
            cardWidth = 220.dp,
            wide = true,
            onPlay = onPlay,
            onLongPress = onLongPress,
        )
        OfflineDownloadRecordRail(
            title = "Audiobooks",
            records = audiobooks,
            cardWidth = 220.dp,
            wide = true,
            onPlay = onPlay,
            onLongPress = onLongPress,
        )
    }
}

@Composable
private fun OfflineDownloadRecordRail(
    title: String,
    records: List<DownloadRecord>,
    cardWidth: Dp,
    wide: Boolean,
    onPlay: (DownloadRecord) -> Unit,
    onLongPress: (DownloadRecord) -> Unit,
) {
    if (records.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(
            title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(records, key = { it.id }) { record ->
                OfflineDownloadArtworkCard(
                    record = record,
                    title = record.title,
                    subtitle = record.offlineDisplaySubtitle(),
                    countLabel = null,
                    cardWidth = cardWidth,
                    wide = wide,
                    onPlay = { onPlay(record) },
                    onLongPress = { onLongPress(record) },
                )
            }
        }
    }
}

@Composable
private fun OfflineDownloadCollectionRail(
    title: String,
    groups: List<List<DownloadRecord>>,
    onPlay: (DownloadRecord) -> Unit,
    onLongPress: (DownloadRecord) -> Unit,
) {
    if (groups.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(
            title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(groups, key = { group -> group.firstOrNull()?.parentId ?: group.firstOrNull()?.albumId ?: group.firstOrNull()?.id.orEmpty() }) { group ->
                val representative = group.first()
                OfflineDownloadArtworkCard(
                    record = representative,
                    title = representative.albumName ?: representative.title,
                    subtitle = listOfNotNull(
                        representative.artistName,
                        "${group.size} ${if (group.size == 1) "track" else "tracks"}",
                    ).joinToString(" · "),
                    countLabel = group.size.toString(),
                    cardWidth = 142.dp,
                    wide = false,
                    onPlay = { onPlay(representative) },
                    onLongPress = { onLongPress(representative) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OfflineDownloadArtworkCard(
    record: DownloadRecord,
    title: String,
    subtitle: String?,
    countLabel: String?,
    cardWidth: Dp,
    wide: Boolean,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
) {
    val isReady = record.state == DownloadState.Completed
    Column(
        modifier = Modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (wide) Modifier.height(126.dp) else Modifier.aspectRatio(0.68f))
                .clip(RoundedCornerShape(18.dp))
                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.56f))
                .combinedClickable(
                    onClick = {
                        if (isReady) onPlay() else onLongPress()
                    },
                    onLongClick = onLongPress,
                ),
        ) {
            val imageModel = record.offlineArtworkModel()
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                OfflineDownloadArtworkFallback(
                    title = title,
                    icon = record.downloadIcon(isReady),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.58f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.62f),
                        ),
                    ),
            )
            record.progressFraction()?.takeIf { !isReady }?.let { progress ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(VantafynGradients.accentHorizontal()),
                    )
                }
            }
            val badge = countLabel ?: if (!isReady) record.downloadStatusLabel() else null
            if (!badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.54f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        badge,
                        color = if (record.state == DownloadState.Failed || record.state == DownloadState.Cancelled) Color(0xFFFFC2C2) else VantafynColors.Ink,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isReady) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = VantafynColors.Ink.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.32f))
                        .padding(5.dp),
                )
            }
        }
        Text(
            title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = rememberLifecycleAwareMarquee(),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OfflineDownloadArtworkFallback(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF182235),
                        Color(0xFF111827),
                        Color(0xFF251936),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VantafynColors.Ink.copy(alpha = 0.62f),
            modifier = Modifier.size(34.dp),
        )
        Text(
            title.take(1).uppercase(Locale.US),
            color = VantafynColors.Ink.copy(alpha = 0.22f),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
        )
    }
}

@Composable
private fun DownloadActionsModal(
    record: DownloadRecord,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val isReady = record.state == DownloadState.Completed
    val isActive = record.isActiveDownload()
    val canRetry = record.state == DownloadState.Failed || record.state == DownloadState.Cancelled
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 22.dp, vertical = 28.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        VantafynGlassModalPanel(
            selected = true,
            modifier = Modifier
                .fillMaxWidth()
                .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.25.dp)
                .clickable(enabled = false) {},
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.66f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val imageModel = record.offlineArtworkModel()
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = record.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(record.downloadIcon(isReady), contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(34.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(record.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        record.offlineDisplaySubtitle()?.let {
                            Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(record.downloadStatusLabel(), color = record.downloadStatusColor(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                record.progressFraction()?.takeIf { !isReady }?.let { progress ->
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
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(VantafynGradients.accentHorizontal()),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    if (isReady) {
                        VantafynButton("Play", onClick = onPlay, modifier = Modifier.weight(1f))
                    }
                    if (isActive) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                    if (canRetry) {
                        VantafynButton("Retry", onClick = onRetry, modifier = Modifier.weight(1f))
                    }
                    if (!isActive) {
                        OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) { Text("Remove", color = Color(0xFFFFC2C2)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRecordCard(
    record: DownloadRecord,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val isReady = record.state == DownloadState.Completed
    val isActive = record.state in setOf(
        DownloadState.Queued,
        DownloadState.Preparing,
        DownloadState.WaitingForNetwork,
        DownloadState.WaitingForWifi,
        DownloadState.Downloading,
        DownloadState.Finalizing,
    )
    val canRetry = record.state == DownloadState.Failed || record.state == DownloadState.Cancelled
    GlassPanel(
        modifier = Modifier.clickable(enabled = isReady, onClick = onPlay),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VantafynColors.SurfaceHigh.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                val poster = record.localPosterPath?.let(::File)?.takeIf { it.exists() }
                if (poster != null) {
                    AsyncImage(
                        model = poster,
                        contentDescription = record.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = record.downloadIcon(isReady),
                        contentDescription = null,
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(record.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                record.downloadSubtitle()?.let { subtitle ->
                    Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(record.downloadStatusLabel(), color = record.downloadStatusColor(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                record.progressFraction()?.let { progress ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.10f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(VantafynGradients.accentHorizontal()),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isReady) {
                    DownloadActionIcon(Icons.Rounded.PlayArrow, "Play", onPlay)
                }
                if (isActive) {
                    DownloadActionIcon(Icons.Rounded.Close, "Cancel", onCancel)
                }
                if (canRetry) {
                    DownloadActionIcon(Icons.Rounded.Replay, "Retry", onRetry)
                }
                if (!isActive) {
                    DownloadActionIcon(Icons.Rounded.Delete, "Remove", onRemove, tint = Color(0xFFFFC2C2))
                }
            }
        }
    }
}

@Composable
private fun DownloadActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = VantafynColors.Ink,
) {
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint.copy(alpha = 0.92f),
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
    )
}

private fun DownloadRecord.progressFraction(): Float? {
    val total = totalBytes ?: return null
    if (total <= 0L) return null
    return (bytesDownloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

private fun DownloadRecord.isActiveDownload(): Boolean =
    state in setOf(
        DownloadState.Queued,
        DownloadState.Preparing,
        DownloadState.WaitingForNetwork,
        DownloadState.WaitingForWifi,
        DownloadState.Downloading,
        DownloadState.Finalizing,
    )

private fun DownloadRecord.offlineArtworkModel(): Any? =
    localPosterPath
        ?.let(::File)
        ?.takeIf { it.exists() && it.length() > 0L }
        ?: remotePosterUrl?.takeIf { it.isNotBlank() }

private fun List<DownloadRecord>.sortedByOfflineTitle(): List<DownloadRecord> =
    sortedWith(compareBy<DownloadRecord> { it.sortTitle ?: it.title }.thenBy { it.title })

private fun DownloadRecord.offlineDisplaySubtitle(): String? =
    when (mediaType) {
        DownloadMediaType.Movie -> year?.toString()
        DownloadMediaType.Episode -> buildList {
            seriesName?.takeIf { it.isNotBlank() }?.let(::add)
            val episode = when {
                seasonNumber != null && episodeNumber != null -> "S${seasonNumber.toString().padStart(2, '0')} E${episodeNumber.toString().padStart(2, '0')}"
                seasonNumber != null -> "Season $seasonNumber"
                episodeNumber != null -> "Episode $episodeNumber"
                else -> null
            }
            episode?.let(::add)
        }.joinToString(" · ").takeIf { it.isNotBlank() }
        DownloadMediaType.Season -> listOfNotNull(seriesName, seasonName).joinToString(" · ").takeIf { it.isNotBlank() }
        DownloadMediaType.MusicTrack -> listOfNotNull(artistName, albumName).joinToString(" · ").takeIf { it.isNotBlank() }
        DownloadMediaType.MusicAlbum -> listOfNotNull(albumName, artistName).joinToString(" · ").takeIf { it.isNotBlank() }
        DownloadMediaType.Audiobook -> listOfNotNull(artistName, albumName).joinToString(" · ").takeIf { it.isNotBlank() } ?: "Audiobook"
    }

private fun DownloadRecord.downloadStatusLabel(): String =
    when (state) {
        DownloadState.Queued -> "Queued"
        DownloadState.Preparing -> "Preparing"
        DownloadState.WaitingForNetwork -> "Waiting for network"
        DownloadState.WaitingForWifi -> "Waiting for Wi-Fi"
        DownloadState.Downloading -> progressFraction()?.let { "${(it * 100).toInt()}%" } ?: "Downloading"
        DownloadState.Finalizing -> "Finishing"
        DownloadState.Completed -> "Ready offline"
        DownloadState.Failed -> failureReason ?: "Could not download"
        DownloadState.Cancelled -> "Cancelled"
    }

private fun DownloadRecord.downloadStatusColor(): Color =
    when (state) {
        DownloadState.Completed -> Color(0xFF78E6B2)
        DownloadState.Failed,
        DownloadState.Cancelled -> Color(0xFFFFA9A9)
        else -> VantafynColors.Ink.copy(alpha = 0.78f)
    }

private fun DownloadRecord.downloadSubtitle(): String? =
    when (mediaType) {
        DownloadMediaType.Movie -> year?.toString()
        DownloadMediaType.Episode -> listOfNotNull(seriesName, seasonNumber?.let { "Season $it" }, episodeNumber?.let { "Episode $it" })
            .joinToString(" · ")
            .takeIf { it.isNotBlank() }
        DownloadMediaType.Season -> listOfNotNull(seriesName, seasonName).joinToString(" · ").takeIf { it.isNotBlank() }
        DownloadMediaType.MusicTrack -> listOfNotNull(artistName, albumName).joinToString(" · ").takeIf { it.isNotBlank() }
        DownloadMediaType.MusicAlbum -> listOfNotNull(albumName, artistName).joinToString(" · ").takeIf { it.isNotBlank() }
        DownloadMediaType.Audiobook -> listOfNotNull(artistName, albumName).joinToString(" · ").takeIf { it.isNotBlank() } ?: "Audiobook"
    }

private fun DownloadRecord.downloadIcon(isReady: Boolean): ImageVector =
    if (!isReady) {
        Icons.Rounded.Download
    } else {
        when (mediaType) {
            DownloadMediaType.MusicTrack,
            DownloadMediaType.MusicAlbum,
            DownloadMediaType.Audiobook -> Icons.Rounded.MusicNote
            DownloadMediaType.Season,
            DownloadMediaType.Episode -> Icons.Rounded.Tv
            DownloadMediaType.Movie -> Icons.Rounded.Movie
        }
    }

private enum class DownloadFilter(val label: String) {
    All("All"),
    Video("Video"),
    Music("Music"),
    Books("Books"),
    Active("Active"),
    Ready("Ready");

    fun matches(record: DownloadRecord): Boolean =
        when (this) {
            All -> true
            Video -> record.mediaType == DownloadMediaType.Movie ||
                record.mediaType == DownloadMediaType.Episode ||
                record.mediaType == DownloadMediaType.Season
            Music -> record.mediaType == DownloadMediaType.MusicTrack ||
                record.mediaType == DownloadMediaType.MusicAlbum
            Books -> record.mediaType == DownloadMediaType.Audiobook
            Active -> record.state in setOf(
                DownloadState.Queued,
                DownloadState.Preparing,
                DownloadState.WaitingForNetwork,
                DownloadState.WaitingForWifi,
                DownloadState.Downloading,
                DownloadState.Finalizing,
            )
            Ready -> record.state == DownloadState.Completed
        }
}

private fun DownloadRecord.matchesOfflineDownloadQuery(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return true
    val haystack = listOfNotNull(
        title,
        sortTitle,
        seriesName,
        seasonName,
        albumName,
        artistName,
        year?.toString(),
        overview,
    ).joinToString(" ").lowercase()
    return trimmed.lowercase().split(Regex("\\s+")).all { it in haystack }
}

private fun Long.downloadSizeLabel(): String =
    when {
        this <= 0L -> "0 MB"
        this >= 1024L * 1024L * 1024L -> "%.1f GB".format(Locale.US, this / (1024.0 * 1024.0 * 1024.0))
        else -> "${this / (1024L * 1024L)} MB"
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
    var confirmPassword by remember { mutableStateOf("") }
    val passwordsMatch = newPassword == confirmPassword
    val showMismatch = confirmPassword.isNotEmpty() && !passwordsMatch
    val canSubmit = newPassword.length >= 6 &&
        passwordsMatch &&
        (!requiresCurrent || current.isNotBlank())
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
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
                VantafynTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm new password",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (showMismatch) {
                    Text(
                        "New passwords do not match.",
                        color = Color(0xFFFFB4C2),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        "Use at least 6 characters. You will need this password next time you sign in.",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(current, newPassword) },
                enabled = canSubmit,
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
    val revealKey = detail?.user?.id ?: "admin-user-settings"
    var resetPasswordExpanded by remember(detail?.user?.id) { mutableStateOf(false) }
    var newPassword by remember(detail?.user?.id) { mutableStateOf("") }
    val avatarPicker = rememberProfileImagePicker(onUploadProfileImage)
    var revealActive by remember(detail?.user?.id) { mutableStateOf(true) }
    LaunchedEffect(detail?.user?.id) {
        revealActive = true
        delay(1_250L)
        revealActive = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            HomeContentReveal(index = 0, animate = revealActive, revealKey = revealKey) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    CompactBackButton(onClick = onBack)
                    Text("User Settings", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (state.isAdminUserLoading) {
            item {
                HomeContentReveal(index = 1, animate = revealActive, revealKey = revealKey) {
                    CompactListSkeleton(rows = 4, leadingSize = 56.dp)
                }
            }
        }
        state.adminUserError?.let { message ->
            item { HomeContentReveal(index = 1, animate = revealActive, revealKey = revealKey) { VantafynErrorCard(message) } }
        }

        if (detail != null) {
            item {
                HomeContentReveal(index = 1, animate = revealActive, revealKey = revealKey) {
                    AdminUserProfileCard(
                        detail = detail,
                        isSaving = state.isProfileImageSaving,
                        onChangePhoto = { avatarPicker.open() },
                        onRemovePhoto = onDeleteProfileImage,
                    )
                }
            }
            if (state.isAdminUserSaving) {
                item {
                    HomeContentReveal(index = 2, animate = revealActive, revealKey = revealKey) {
                        VantafynGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(VantafynSpacing.md),
                        ) {
                            Text("Saving changes...", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            item {
                HomeContentReveal(index = 3, animate = revealActive, revealKey = revealKey) {
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
            }
            item {
                HomeContentReveal(index = 4, animate = revealActive, revealKey = revealKey) {
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
            }
            item {
                HomeContentReveal(index = 5, animate = revealActive, revealKey = revealKey) {
                    GlassPanel(modifier = Modifier.animateContentSize(animationSpec = tween(420, easing = FastOutSlowInEasing))) {
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
                        AnimatedVisibility(
                            visible = resetPasswordExpanded,
                            enter = fadeIn(tween(240, easing = FastOutSlowInEasing)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { -it / 8 },
                            exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) + slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { -it / 10 },
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
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
                }
            }
        } else if (!state.isAdminUserLoading) {
            item {
                HomeContentReveal(index = 1, animate = revealActive, revealKey = revealKey) {
                    EmptyState(
                        title = "User unavailable",
                        subtitle = "Open a user from the admin page to manage settings.",
                    )
                }
            }
        }
    }
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
            VantafynPremiumSwitchVisual(checked = checked)
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
    val editableSections = state.homeLayout.sortedBy { it.order }.filter { it.type != HomeSectionType.MediaBar }
    var editingTarget by remember { mutableStateOf<HomeLayoutEditorTarget?>(null) }
    val editingPreference = editingTarget?.let { target ->
        state.homeLayout.firstOrNull { it.type == target.type }
    }
    var screenRevealActive by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        screenRevealActive = true
        delay(1_450L)
        screenRevealActive = false
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            HomeContentReveal(index = 0, animate = screenRevealActive) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    CompactBackButton(onClick = onBack)
                    ScreenTitle("Home Sections", null)
                }
            }
        }
        item {
            HomeContentReveal(index = 1, animate = screenRevealActive) {
                VantafynGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ViewAgenda, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.88f), modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Hero row stays locked", color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Move the rows underneath it into the order you want.", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        itemsIndexed(
            editableSections,
            key = { _, item -> item.type.name },
        ) { index, preference ->
            HomeContentReveal(index = index + 2, animate = screenRevealActive) {
                HomeSectionEditorCard(
                    state = state,
                    preference = preference,
                    orderLabel = "${index + 1}".padStart(2, '0'),
                    canMoveUp = index > 0,
                    canMoveDown = index < editableSections.lastIndex,
                    onToggle = onToggle,
                    onMove = onMove,
                    onEditOption = { kind -> editingTarget = HomeLayoutEditorTarget(preference.type, kind) },
                )
            }
        }
        item {
            HomeContentReveal(index = editableSections.size + 2, animate = screenRevealActive) {
                GlassPanel {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.88f), modifier = Modifier.size(22.dp))
                        Text("Smart Rows", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    supportedSmartRows.forEach { row ->
                        val selected = row in state.configuredSmartRows
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(row, color = VantafynColors.Ink, modifier = Modifier.weight(1f))
                            TinyAction(if (selected) "Remove" else "Add", gradientBorder = !selected) {
                                if (selected) onRemoveSmartRow(row) else onAddSmartRow(row)
                            }
                        }
                    }
                }
            }
        }
        item {
            HomeContentReveal(index = editableSections.size + 3, animate = screenRevealActive) {
                VantafynButton("Reset to default", onClick = onReset, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (editingTarget != null && editingPreference != null) {
        HomeLayoutOptionDialog(
            preference = editingPreference,
            target = editingTarget!!,
            onDismiss = { editingTarget = null },
            onSelectArtwork = { target ->
                val steps = cycleDistance(editingPreference.artworkType.ordinal, target.ordinal, enumValues<VantafynArtworkType>().size)
                repeat(steps) { onCycleArtwork(editingPreference.type) }
            },
            onSelectShape = { target ->
                val steps = cycleDistance(editingPreference.cardShape.ordinal, target.ordinal, enumValues<VantafynCardShape>().size)
                repeat(steps) { onCycleShape(editingPreference.type) }
            },
            onSelectSize = { target ->
                val steps = cycleDistance(editingPreference.cardSize.ordinal, target.ordinal, enumValues<VantafynCardSize>().size)
                repeat(steps) { onCycleSize(editingPreference.type) }
            },
            onSelectSpacing = { target ->
                val steps = cycleDistance(editingPreference.spacing.ordinal, target.ordinal, enumValues<VantafynCardSpacing>().size)
                repeat(steps) { onCycleSpacing(editingPreference.type) }
            },
        )
    }
}

@Composable
private fun HomeSectionEditorCard(
    state: VantafynHomeUiState,
    preference: HomeSectionPreference,
    orderLabel: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (HomeSectionType) -> Unit,
    onMove: (HomeSectionType, Int) -> Unit,
    onEditOption: (HomeLayoutOptionKind) -> Unit,
) {
    VantafynGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    orderLabel,
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.Center,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(preference.type.label, color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (preference.visible) "Visible" else "Hidden", color = if (preference.visible) VantafynColors.Ink.copy(alpha = 0.72f) else VantafynColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                HomeSectionMoveButton(
                    icon = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Move ${preference.type.label} up",
                    enabled = canMoveUp,
                    onClick = { onMove(preference.type, -1) },
                )
                HomeSectionMoveButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Move ${preference.type.label} down",
                    enabled = canMoveDown,
                    onClick = { onMove(preference.type, 1) },
                )
                TinyAction(if (preference.visible) "Hide" else "Show", gradientBorder = !preference.visible) {
                    onToggle(preference.type)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                HomeSectionPreview(
                    state = state,
                    preference = preference,
                    modifier = Modifier.weight(1f),
                )
                Column(
                    modifier = Modifier.width(132.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    CompactHomeLayoutSelector("Art", preference.artworkType.shortLabel()) { onEditOption(HomeLayoutOptionKind.Artwork) }
                    CompactHomeLayoutSelector("Shape", preference.cardShape.shortLabel()) { onEditOption(HomeLayoutOptionKind.Shape) }
                    CompactHomeLayoutSelector("Size", preference.cardSize.shortLabel()) { onEditOption(HomeLayoutOptionKind.Size) }
                    CompactHomeLayoutSelector("Space", preference.spacing.shortLabel()) { onEditOption(HomeLayoutOptionKind.Spacing) }
                }
            }
        }
    }
}

@Composable
private fun HomeCustomizeEditorPanel(
    state: VantafynHomeUiState,
    draftLayout: List<HomeSectionPreference>,
    draftSmartRows: List<String>,
    selectedType: HomeSectionType?,
    onSelectType: (HomeSectionType) -> Unit,
    onLayoutChanged: (List<HomeSectionPreference>) -> Unit,
    onEditOption: (HomeLayoutEditorTarget) -> Unit,
    onAddRow: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onResetDraft: () -> Unit,
) {
    val reducedMotion = rememberReducedMotionPreference()
    val haptics = LocalHapticFeedback.current
    val editableSections = remember(draftLayout) {
        draftLayout.sortedBy { it.order }.filter { it.type != HomeSectionType.MediaBar }
    }
    val selectedPreference = editableSections.firstOrNull { it.type == selectedType } ?: editableSections.firstOrNull()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Black.copy(alpha = 0.50f),
                        0.68f to Color.Black.copy(alpha = 0.34f),
                        1.00f to Color.Transparent,
                    ),
                ),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        val panelMaxHeight = maxHeight * 0.60f
        val listMaxHeight = maxHeight * 0.22f
        VantafynGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = panelMaxHeight)
                .shadow(28.dp, RoundedCornerShape(30.dp), clip = false)
                .then(
                    if (reducedMotion) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(30.dp))
                    } else {
                        Modifier.vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.25.dp, durationMillis = 5600)
                    },
                ),
            variant = VantafynGlassVariant.Modal,
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xFF172136).copy(alpha = 0.97f),
                                0.58f to Color(0xFF090D18).copy(alpha = 0.95f),
                                1.00f to Color(0xFF04060E).copy(alpha = 0.98f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeDesignerHeader(
                    onCancel = onCancel,
                    onDone = onDone,
                    onReset = onResetDraft,
                    onAddRow = onAddRow,
                )
                CompactLockedHeroRow()
                selectedPreference?.let { preference ->
                    SelectedHomeRowInspector(
                        preference = preference,
                        draftSmartRows = draftSmartRows,
                        onEditOption = { kind -> onEditOption(HomeLayoutEditorTarget(preference.type, kind)) },
                    )
                } ?: AddRowCompactAction(onClick = onAddRow)
                LazyColumn(
                    modifier = Modifier.heightIn(max = listMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                ) {
                    itemsIndexed(editableSections, key = { _, item -> item.type.name }) { index, preference ->
                        CompactHomeEditorRow(
                            state = state,
                            preference = preference,
                            orderLabel = "${index + 1}".padStart(2, '0'),
                            configuredSmartRows = draftSmartRows,
                            selected = selectedType == preference.type,
                            onToggle = { type ->
                                onSelectType(type)
                                onLayoutChanged(updateHomeDraftSection(draftLayout, type) { it.copy(visible = !it.visible) })
                            },
                            onSelect = onSelectType,
                            onDragMove = { direction ->
                                onSelectType(preference.type)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLayoutChanged(moveHomeDraftSection(draftLayout, preference.type, direction))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDesignerHeader(
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onReset: () -> Unit,
    onAddRow: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "Customize Home",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Changes preview instantly",
                    color = VantafynColors.Muted.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TinyAction("Done", gradientBorder = true, compact = false) { onDone() }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddRowCompactAction(onClick = onAddRow)
            Spacer(Modifier.weight(1f))
            TinyAction("Reset") { onReset() }
            TinyAction("Cancel") { onCancel() }
        }
    }
}

@Composable
private fun CompactLockedHeroRow() {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("00", color = VantafynColors.Muted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.62f), modifier = Modifier.size(18.dp))
        Text("Hero", color = VantafynColors.Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("Locked", color = VantafynColors.Muted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompactHomeEditorRow(
    state: VantafynHomeUiState,
    preference: HomeSectionPreference,
    orderLabel: String,
    configuredSmartRows: List<String> = state.configuredSmartRows,
    selected: Boolean = false,
    onToggle: (HomeSectionType) -> Unit,
    onSelect: (HomeSectionType) -> Unit = {},
    onDragMove: ((Int) -> Unit)? = null,
) {
    val reducedMotion = rememberReducedMotionPreference()
    val density = LocalDensity.current
    val slotHeightPx = with(density) { 58.dp.toPx() }
    var dragY by remember(preference.type) { mutableFloatStateOf(0f) }
    val lift by animateFloatAsState(
        targetValue = if (selected && !reducedMotion) 1.008f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(stiffness = 520f, dampingRatio = 0.82f),
        label = "homeEditorLift",
    )
    val shape = RoundedCornerShape(18.dp)
    val cardModifier = Modifier
        .fillMaxWidth()
        .animateContentSize(animationSpec = if (reducedMotion) snap() else tween(260, easing = FastOutSlowInEasing))
        .graphicsLayer {
            scaleX = 1f
            scaleY = lift
            translationY = if (reducedMotion) 0f else dragY.coerceIn(-12f, 12f)
            shadowElevation = if (selected) 8f else 0f
        }
        .then(
            if (selected) {
                if (reducedMotion) {
                    Modifier.border(1.dp, VantafynColors.Secondary.copy(alpha = 0.40f), shape)
                } else {
                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 18.dp, strokeWidth = 1.05.dp, durationMillis = 5200)
                }
            } else {
                Modifier.border(1.dp, Color.White.copy(alpha = 0.09f), shape)
            },
        )
    Row(
        modifier = cardModifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (selected) 0.105f else 0.055f),
                        Color(0xFF0B1020).copy(alpha = if (selected) 0.74f else 0.62f),
                    ),
                ),
            )
            .clickable { onSelect(preference.type) }
            .padding(horizontal = 9.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            orderLabel,
            color = VantafynColors.Muted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(preference.type, onDragMove) {
                    if (onDragMove == null) return@pointerInput
                    detectDragGestures(
                        onDragEnd = { dragY = 0f },
                        onDragCancel = { dragY = 0f },
                    ) { change, dragAmount ->
                        change.consume()
                        dragY += dragAmount.y
                        val direction = when {
                            dragY > slotHeightPx * 0.58f -> 1
                            dragY < -slotHeightPx * 0.58f -> -1
                            else -> 0
                        }
                        if (direction != 0) {
                            onDragMove(direction)
                            dragY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.DragIndicator, contentDescription = "Drag ${preference.type.label}", tint = VantafynColors.Ink.copy(alpha = 0.70f), modifier = Modifier.size(20.dp))
        }
        HomeSectionTinyPreview(
            state = state,
            preference = preference,
            configuredSmartRows = configuredSmartRows,
        )
        Text(
            preference.type.label,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (preference.visible) "Visible" else "Hidden",
            color = if (preference.visible) VantafynColors.Secondary.copy(alpha = 0.95f) else VantafynColors.Muted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = if (preference.visible) 0.080f else 0.045f))
                .clickable { onToggle(preference.type) }
                .padding(horizontal = 9.dp, vertical = 5.dp)
                .widthIn(min = 42.dp),
            textAlign = TextAlign.Center,
        )
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.Tune,
            contentDescription = "Customize ${preference.type.label}",
            tint = if (selected) VantafynColors.Secondary.copy(alpha = 0.95f) else VantafynColors.Ink.copy(alpha = 0.66f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun HomeSectionTinyPreview(
    state: VantafynHomeUiState,
    preference: HomeSectionPreference,
    configuredSmartRows: List<String>,
) {
    val images = previewImagesFor(state, preference.type, configuredSmartRows)
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
    ) {
        images.take(2).forEachIndexed { index, imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = index * 14f
                        alpha = 1f - index * 0.34f
                        scaleX = 1f - index * 0.08f
                        scaleY = 1f - index * 0.08f
                    },
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun SelectedHomeRowInspector(
    preference: HomeSectionPreference,
    draftSmartRows: List<String>,
    onEditOption: (HomeLayoutOptionKind) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.24f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Card layout", color = VantafynColors.Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(preference.type.label, color = VantafynColors.Muted.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (preference.type == HomeSectionType.SmartRows) {
                Text("${draftSmartRows.size} rows", color = VantafynColors.Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FocusedHomeStyleButton("Art", preference.artworkType.shortLabel(), Modifier.weight(1f)) { onEditOption(HomeLayoutOptionKind.Artwork) }
            FocusedHomeStyleButton("Shape", preference.cardShape.shortLabel(), Modifier.weight(1f)) { onEditOption(HomeLayoutOptionKind.Shape) }
            FocusedHomeStyleButton("Size", preference.cardSize.shortLabel(), Modifier.weight(1f)) { onEditOption(HomeLayoutOptionKind.Size) }
            FocusedHomeStyleButton("Spacing", preference.spacing.shortLabel(), Modifier.weight(1f)) { onEditOption(HomeLayoutOptionKind.Spacing) }
        }
    }
}

@Composable
private fun AddRowCompactAction(onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        "+ Add Row",
        color = VantafynColors.Ink,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.05.dp, durationMillis = 5200)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.075f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun FocusedHomeStyleButton(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.060f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(label, color = VantafynColors.Muted.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = VantafynColors.Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeAddRowsDialog(
    state: VantafynHomeUiState,
    selectedRows: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit,
) {
    var draftSelection by remember(selectedRows) { mutableStateOf(selectedRows) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.2.dp, durationMillis = 5600),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Add Rows", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Choose smart shelves for Home.", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                supportedSmartRows.forEach { row ->
                    val selected = row in draftSelection
                    HomeSmartRowChoiceTile(
                        title = row,
                        description = smartRowDescription(row),
                        previewUrls = state.home?.sections.orEmpty()
                            .firstOrNull { it.title == row }
                            ?.items
                            .orEmpty()
                            .mapNotNull { it.backdropUrl ?: it.imageUrl }
                            .take(2),
                        selected = selected,
                        onClick = {
                            draftSelection = if (selected) {
                                draftSelection - row
                            } else {
                                (draftSelection + row).filter { it in supportedSmartRows }.distinct()
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TinyAction("Add ${draftSelection.size} Rows", gradientBorder = true, compact = false) {
                onApply(draftSelection)
            }
        },
        dismissButton = {
            TinyAction("Cancel", compact = false) { onDismiss() }
        },
    )
}

@Composable
private fun HomeSmartRowChoiceTile(
    title: String,
    description: String,
    previewUrls: List<String>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 18.dp, strokeWidth = 1.05.dp, durationMillis = 5200)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                }
            )
            .clip(shape)
            .background(Color.White.copy(alpha = if (selected) 0.09f else 0.045f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.055f)),
        ) {
            if (previewUrls.isEmpty()) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.62f), modifier = Modifier.align(Alignment.Center).size(18.dp))
            } else {
                previewUrls.forEachIndexed { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = index * 14f
                                alpha = 1f - index * 0.28f
                            },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(description, color = VantafynColors.Muted.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HomeSectionPreview(
    state: VantafynHomeUiState,
    preference: HomeSectionPreference,
    configuredSmartRows: List<String> = state.configuredSmartRows,
    modifier: Modifier = Modifier,
) {
    val images = previewImagesFor(state, preference.type, configuredSmartRows)
    val isPoster = preference.artworkType == VantafynArtworkType.PrimaryPoster ||
        preference.type == HomeSectionType.RecentlyAddedMovies
    val corner = preference.cardCorner()
    val spacing = preference.spacing.toDp().coerceAtMost(14.dp)
    Row(
        modifier = Modifier
            .then(modifier)
            .height(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(7.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (images.isEmpty()) {
            Text("No preview data yet", color = VantafynColors.Muted, modifier = Modifier.padding(horizontal = 6.dp))
        } else {
            images.take(if (isPoster) 3 else 2).forEach { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .then(if (isPoster) Modifier.width(previewPosterWidth(preference.cardSize)) else Modifier.weight(1f))
                        .fillMaxSize()
                        .clip(RoundedCornerShape(corner))
                        .background(VantafynColors.SurfaceHigh.copy(alpha = 0.46f)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun CompactHomeLayoutSelector(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.075f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(label, color = VantafynColors.Muted.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(value, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeLayoutOptionDialog(
    preference: HomeSectionPreference,
    target: HomeLayoutEditorTarget,
    onDismiss: () -> Unit,
    onSelectArtwork: (VantafynArtworkType) -> Unit,
    onSelectShape: (VantafynCardShape) -> Unit,
    onSelectSize: (VantafynCardSize) -> Unit,
    onSelectSpacing: (VantafynCardSpacing) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 28.dp, strokeWidth = 1.2.dp, durationMillis = 5200),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(target.kind.title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(preference.type.label, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (target.kind) {
                    HomeLayoutOptionKind.Artwork -> enumValues<VantafynArtworkType>().forEach { option ->
                        HomeLayoutChoiceRow(
                            title = option.shortLabel(),
                            selected = preference.artworkType == option,
                            onClick = { onSelectArtwork(option) },
                        )
                    }
                    HomeLayoutOptionKind.Shape -> enumValues<VantafynCardShape>().forEach { option ->
                        HomeLayoutChoiceRow(
                            title = option.shortLabel(),
                            selected = preference.cardShape == option,
                            onClick = { onSelectShape(option) },
                        )
                    }
                    HomeLayoutOptionKind.Size -> enumValues<VantafynCardSize>().forEach { option ->
                        HomeLayoutChoiceRow(
                            title = option.shortLabel(),
                            selected = preference.cardSize == option,
                            onClick = { onSelectSize(option) },
                        )
                    }
                    HomeLayoutOptionKind.Spacing -> enumValues<VantafynCardSpacing>().forEach { option ->
                        HomeLayoutChoiceRow(
                            title = option.shortLabel(),
                            selected = preference.spacing == option,
                            onClick = { onSelectSpacing(option) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun HomeLayoutChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 18.dp, strokeWidth = 1.1.dp, durationMillis = 5200)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                },
            )
            .clip(shape)
            .background(Color.White.copy(alpha = if (selected) 0.09f else 0.045f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(20.dp))
        }
    }
}

private data class HomeLayoutEditorTarget(
    val type: HomeSectionType,
    val kind: HomeLayoutOptionKind,
)

private enum class HomeLayoutOptionKind(val title: String) {
    Artwork("Artwork"),
    Shape("Card shape"),
    Size("Card size"),
    Spacing("Row spacing"),
}

private fun cycleDistance(currentOrdinal: Int, targetOrdinal: Int, count: Int): Int =
    (targetOrdinal - currentOrdinal + count) % count

private fun normalizeHomeLayoutDraft(layout: List<HomeSectionPreference>): List<HomeSectionPreference> {
    val fixed = layout.firstOrNull { it.type == HomeSectionType.MediaBar }
        ?: defaultHomeLayout().first { it.type == HomeSectionType.MediaBar }
    val editable = layout
        .filter { it.type != HomeSectionType.MediaBar }
        .distinctBy { it.type }
        .sortedBy { it.order }
    val missing = defaultHomeLayout()
        .filter { it.type != HomeSectionType.MediaBar }
        .filter { default -> editable.none { it.type == default.type } }
    return listOf(fixed.copy(visible = true, order = 0)) +
        (editable + missing).mapIndexed { index, preference -> preference.copy(order = index + 1) }
}

private fun updateHomeDraftSection(
    layout: List<HomeSectionPreference>,
    type: HomeSectionType,
    transform: (HomeSectionPreference) -> HomeSectionPreference,
): List<HomeSectionPreference> =
    normalizeHomeLayoutDraft(layout.map { preference ->
        if (preference.type == type && type != HomeSectionType.MediaBar) transform(preference) else preference
    })

private fun moveHomeDraftSection(
    layout: List<HomeSectionPreference>,
    type: HomeSectionType,
    direction: Int,
): List<HomeSectionPreference> {
    if (type == HomeSectionType.MediaBar || direction == 0) return normalizeHomeLayoutDraft(layout)
    val normalized = normalizeHomeLayoutDraft(layout)
    val fixed = normalized.first { it.type == HomeSectionType.MediaBar }
    val editable = normalized.filter { it.type != HomeSectionType.MediaBar }.sortedBy { it.order }.toMutableList()
    val index = editable.indexOfFirst { it.type == type }
    val target = (index + direction).coerceIn(0, editable.lastIndex)
    if (index < 0 || index == target) return normalized
    val item = editable.removeAt(index)
    editable.add(target, item)
    return listOf(fixed.copy(visible = true, order = 0)) +
        editable.mapIndexed { order, preference -> preference.copy(order = order + 1) }
}

private fun smartRowDescription(row: String): String =
    when {
        row.contains("Continue", ignoreCase = true) -> "Continue unfinished movies and episodes."
        row.contains("Next", ignoreCase = true) -> "Episodes ready for the next watch."
        row.contains("Recently", ignoreCase = true) -> "Fresh titles from your Jellyfin server."
        row.contains("Movie", ignoreCase = true) -> "A movie-focused Home shelf."
        row.contains("TV", ignoreCase = true) || row.contains("Series", ignoreCase = true) -> "A series-focused Home shelf."
        else -> "A smart shelf powered by Jellyfin."
    }

private fun previewPosterWidth(size: VantafynCardSize) =
    when (size) {
        VantafynCardSize.Small -> 44.dp
        VantafynCardSize.Medium -> 52.dp
        VantafynCardSize.Large -> 60.dp
    }

@Composable
private fun TinyAction(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    gradientBorder: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    val decoratedModifier = if (gradientBorder) {
        modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.1.dp, durationMillis = 4200)
    } else {
        modifier.border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    }
    Text(
        text,
        color = VantafynColors.Ink,
        style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = decoratedModifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.065f))
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 6.dp else 8.dp),
    )
}

@Composable
private fun HomeSectionMoveButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = VantafynColors.Ink.copy(alpha = if (enabled) 0.92f else 0.24f),
            modifier = Modifier.size(28.dp),
        )
    }
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
    onQueueDownload: (java.util.UUID) -> Unit,
    onStartWatchParty: (WatchPartyMode) -> Unit,
    onShareMediaToFriend: (dev.vantafyn.core.jellyfin.JellyfinFriend, JellyfinMediaDetail) -> Unit = { _, _ -> },
) {
    val detail = state.mediaDetail
    val detailRevealKey = state.selectedMediaId ?: detail?.id ?: "media-detail"
    val detailHeroKey = "detail-hero-$detailRevealKey"
    val detailListState = rememberLazyListState()
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val statusBarPx = WindowInsets.statusBars.getTop(density).toFloat()
    val detailScrimFadeStartPx = statusBarPx + with(density) { 132.dp.toPx() }
    val detailScrimFadeEndPx = statusBarPx + with(density) { 28.dp.toPx() }
    val detailStatusScrimTargetAlpha by remember(detailListState, detailHeroKey, density) {
        derivedStateOf {
            val layoutInfo = detailListState.layoutInfo
            val headerInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == detailHeroKey }
            when {
                headerInfo != null -> {
                    val headerBottom = headerInfo.offset + headerInfo.size
                    ((detailScrimFadeStartPx - headerBottom) / (detailScrimFadeStartPx - detailScrimFadeEndPx)).coerceIn(0f, 1f)
                }
                detailListState.firstVisibleItemIndex > 0 -> 1f
                else -> 0f
            }
        }
    }
    val detailStatusScrimAlpha by animateFloatAsState(
        targetValue = detailStatusScrimTargetAlpha,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "detailStatusScrimAlpha",
    )
    var showActions by remember { mutableStateOf(false) }
    var showMediaInfo by remember { mutableStateOf(false) }
    var showWatchPartyStart by remember { mutableStateOf(false) }
    var showShareToFriend by remember { mutableStateOf(false) }
    var detailRevealActive by remember(state.selectedMediaId) { mutableStateOf(true) }
    LaunchedEffect(state.selectedMediaId) {
        detailRevealActive = true
        delay(1_450L)
        detailRevealActive = false
    }
    DetailThemeAudio(
        url = detail?.themeSongUrl,
        enabled = themeMusicEnabled,
        volume = themeMusicVolume,
    )
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = detailListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
        if (state.isMediaDetailLoading) item {
            DetailLoadingSkeleton()
        }
        state.mediaDetailError?.let { item {
            HomeRowInset {
                VantafynErrorCard(it) { VantafynButton("Retry", onClick = onRetry) }
            }
        } }
        if (detail != null) {
            val isPerson = detail.itemType.equals("Person", ignoreCase = true)
            item(key = detailHeroKey) {
                HomeContentReveal(index = 0, animate = detailRevealActive, revealKey = detailRevealKey) {
                    if (isPerson) {
                        PersonDetailHero(
                            detail = detail,
                            onBack = onBack,
                        )
                    } else {
                        MediaDetailHero(
                            detail = detail,
                            onBack = onBack,
                            onMore = { showActions = true },
                            onFavorite = onToggleFavorite,
                        )
                    }
                }
            }
            if (!isPerson) {
                item {
                    HomeContentReveal(index = 1, animate = detailRevealActive, revealKey = detailRevealKey) {
                        HomeRowInset {
                            DetailActionPanel(
                                detail = detail,
                                onStartPlayback = onStartPlayback,
                                onStartPlaybackFromBeginning = onStartPlaybackFromBeginning,
                                onMore = { showActions = true },
                                onToggleFavorite = onToggleFavorite,
                                onTogglePlayed = onTogglePlayed,
                                onQueueDownload = onQueueDownload,
                            )
                        }
                    }
                }
            }
            item {
                HomeContentReveal(index = 2, animate = detailRevealActive, revealKey = detailRevealKey) {
                    HomeRowInset {
                        DetailOverview(detail)
                    }
                }
            }
            if (isPerson && state.personFilmography.isNotEmpty()) {
                item {
                    HomeContentReveal(index = 3, animate = detailRevealActive, revealKey = detailRevealKey) {
                        HomeRowInset {
                            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                                Text("Filmography", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                                    items(state.personFilmography, key = { it.id }) { item ->
                                        MediaItemCard(item = item, onClick = { onOpenMedia(item.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (detail.collectionItems.isNotEmpty()) {
                item {
                    HomeContentReveal(index = 3, animate = detailRevealActive, revealKey = detailRevealKey) {
                        HomeRowInset {
                            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                                Text("Movies", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                                    items(detail.collectionItems, key = { it.id }) { item ->
                                        MediaItemCard(item = item, onClick = { onOpenMedia(item.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (detail.itemType.equals("Series", ignoreCase = true)) {
                item {
                    HomeContentReveal(index = 3, animate = detailRevealActive, revealKey = detailRevealKey) {
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
                                onOpenEpisodeDetail = onOpenMedia,
                            )
                        }
                    }
                }
            }
            if (!isPerson && detail.people.isNotEmpty()) {
                item {
                    HomeContentReveal(index = 4, animate = detailRevealActive, revealKey = detailRevealKey) {
                        HomeRowInset {
                            PeopleSection(detail, onPerson = { onOpenMedia(it) })
                        }
                    }
                }
            }
            if (!isPerson && detail.related.isNotEmpty()) {
                item {
                    HomeContentReveal(index = 5, animate = detailRevealActive, revealKey = detailRevealKey) {
                        HomeRowInset {
                            RelatedSection(detail, onOpenMedia)
                        }
                    }
                }
            }
            if (detail.externalLinks.isNotEmpty()) {
                item {
                    HomeContentReveal(index = 6, animate = detailRevealActive, revealKey = detailRevealKey) {
                        HomeRowInset {
                            ExternalLinksSection(detail, onOpen = onPlaybackComingSoon)
                        }
                    }
                }
            }
        }
        }
        HomeStatusBarScrim(
            alpha = detailStatusScrimAlpha,
            statusBarHeight = statusBarHeight,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
    if (detail != null && showActions) {
        DetailActionSheet(
            detail = detail,
            isAdmin = state.session?.user?.isAdministrator == true,
            socialEnabled = state.socialEnabled && state.session != null,
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
            onQueueDownload = {
                showActions = false
                detail?.id?.let { onQueueDownload(it) }
            },
            onShareToFriend = {
                showActions = false
                showShareToFriend = true
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
    if (detail != null && showShareToFriend) {
        ShareMediaToFriendSheet(
            detail = detail,
            friends = state.socialFriends,
            onDismiss = { showShareToFriend = false },
            onShare = { friend ->
                showShareToFriend = false
                onShareMediaToFriend(friend, detail)
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
            val player = VantafynExoPlayerFactory.builder(context).build().apply {
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
    onQueueDownload: (java.util.UUID) -> Unit,
) {
    val supportsMyList = detail.itemType.supportsMyListAction()
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        VantafynButton(detail.primaryActionLabel(), onClick = onStartPlayback, modifier = Modifier.fillMaxWidth())
        if (detail.streamInfo.isNotEmpty()) DetailChipRow(detail.streamInfo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            if (supportsMyList) {
                DetailAction(if (detail.isFavorite) "♥" else "♡", if (detail.isFavorite) "In My List" else "Add to My List", onToggleFavorite, Modifier.weight(1f))
            }
            DetailAction("↺", "From Start", onStartPlaybackFromBeginning, Modifier.weight(1f))
            DetailAction("⋯", "More", onMore, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailAction(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isFavoriteAction = icon == "♥"
    val reducedMotion = rememberReducedMotionPreference()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.976f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = 0.78f,
                stiffness = 620f,
            )
        },
        label = "detailActionPressedScale",
    )
    val pressDepth by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "detailActionPressDepth",
    )
    val activeGlow by animateFloatAsState(
        targetValue = if (isFavoriteAction) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "detailActionActiveGlow",
    )
    val cornerRadius = 18.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .height(72.dp)
            .scale(scale)
            .clip(shape)
            .drawWithContent {
                val radius = cornerRadius.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.17f + 0.03f * pressDepth),
                            VantafynColors.SurfaceHigh.copy(alpha = 0.56f + 0.05f * pressDepth),
                            VantafynColors.Graphite.copy(alpha = 0.74f + 0.04f * pressDepth),
                        ),
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                )
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VantafynColors.Primary.copy(alpha = 0.16f + 0.06f * activeGlow + 0.04f * pressDepth),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, -size.height * 0.1f),
                        radius = size.width * 0.8f,
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                )
                if (activeGlow > 0f) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF2F64).copy(alpha = 0.16f * activeGlow),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.34f),
                            radius = size.width * 0.46f,
                        ),
                        cornerRadius = CornerRadius(radius, radius),
                    )
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f + 0.05f * pressDepth),
                            Color.Transparent,
                        ),
                        endY = size.height * 0.34f,
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f + 0.07f * pressDepth),
                            VantafynColors.Secondary.copy(alpha = 0.11f + 0.04f * pressDepth),
                            VantafynColors.Primary.copy(alpha = 0.13f + 0.06f * activeGlow + 0.04f * pressDepth),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 1.15.dp.toPx()),
                )
                drawContent()
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = VantafynSpacing.xs, horizontal = VantafynSpacing.xs),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Text(
                icon,
                color = if (isFavoriteAction) Color(0xFFFF3D67) else VantafynColors.Ink.copy(alpha = 0.96f),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = if (isFavoriteAction) 21.sp else 23.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    shadowElevation = if (isFavoriteAction) 7f else 0f
                    ambientShadowColor = Color(0xFFFF2F64)
                    spotShadowColor = Color(0xFFFF2F64)
                },
            )
            Text(
                label,
                color = if (isFavoriteAction) VantafynColors.Ink.copy(alpha = 0.88f) else VantafynColors.Muted.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            val showToneDot = tone != null && !value.isStarRating()
            VantafynGlassPill(
                selected = tone != null,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tone?.takeIf { showToneDot }?.let { dotTone ->
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(dotTone.copy(alpha = 0.90f), RoundedCornerShape(999.dp)),
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
        value.isStarRating() -> VantafynColors.Gold
        "hdr" in normalized || "dolby vision" in normalized || "dv" == normalized -> Color(0xFFFFD36A)
        "4k" in normalized || "2160" in normalized || "uhd" in normalized -> Color(0xFF8FE7FF)
        "1080" in normalized || "720" in normalized -> Color(0xFF6FA8FF)
        "sub" in normalized || "cc" == normalized || "caption" in normalized -> Color(0xFFC892FF)
        "eng" == normalized || "english" in normalized || "audio" in normalized -> Color(0xFF7CE7C8)
        "atmos" in normalized || "dts" in normalized || "aac" in normalized || "flac" in normalized || "truehd" in normalized -> Color(0xFFFF8AD8)
        else -> null
    }
}

private fun String.isStarRating(): Boolean = trimStart().startsWith("★")

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
    onOpenEpisodeDetail: (java.util.UUID) -> Unit,
) {
    val sortedSeasons = remember(detail.seasons) {
        detail.seasons.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
    }
    val selectedSeasonTitle = sortedSeasons.firstOrNull { it.id == selectedSeasonId }?.title
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        if (sortedSeasons.isNotEmpty()) {
            Text("Seasons", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                items(sortedSeasons, key = { it.id }) { season ->
                    val selected = season.id == selectedSeasonId
                    Box(
                        modifier = Modifier
                            .width(95.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) {
                                    Brush.linearGradient(listOf(VantafynColors.Primary.copy(alpha = 0.25f), VantafynColors.Secondary.copy(alpha = 0.15f)))
                                } else {
                                    Brush.linearGradient(listOf(Color(0xFF24304D), Color(0xFF393456), VantafynColors.SurfaceHigh))
                                },
                            )
                            .then(
                                if (selected) Modifier.vantafynAnimatedModalBorder(cornerRadius = 10.dp, strokeWidth = 2.5.dp)
                                else Modifier
                            )
                            .clickable { onSelectSeason(season.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (season.imageUrl != null) {
                            AsyncImage(
                                model = season.imageUrl,
                                contentDescription = season.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                season.title,
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                        if (season.isPlayed) {
                            WatchedCheckBadge(
                                modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 4.dp),
                            )
                        } else if (season.unplayedItemCount > 0) {
                            UnwatchedCountBadge(
                                count = season.unplayedItemCount,
                                modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Episodes", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (selectedSeasonTitle != null) {
                Text(selectedSeasonTitle, color = VantafynColors.Muted)
            }
        }
        if (isLoading) {
            CompactListSkeleton(rows = 4, leadingSize = 92.dp)
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
                        isPlayed = episode.isPlayed,
                        onClick = { onEpisode(episode) },
                        onLongPress = { onEpisodeFromBeginning(episode) },
                        modifier = Modifier.fillMaxWidth().height(146.dp),
                    )
                    Text(episode.subtitle ?: "Episode", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(episode.title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                    episode.overview?.let { Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onOpenEpisodeDetail(episode.id) }) }
                }
            }
        }
    }
}

@Composable
private fun DetailActionSheet(
    detail: JellyfinMediaDetail,
    isAdmin: Boolean,
    socialEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onWatchFromBeginning: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
    onQueueDownload: (java.util.UUID) -> Unit,
    onShareToFriend: () -> Unit = {},
    onMediaInfo: () -> Unit,
    onStartWatchParty: () -> Unit,
) {
    val supportsMyList = detail.itemType.supportsMyListAction()
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(detail.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = when { detail.title.length > 55 -> 17.sp; detail.title.length > 35 -> 19.sp; else -> 22.sp })
                Text(detail.subtitle ?: detail.itemType ?: "Media", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailSheetAction("▶", detail.primaryActionLabel(), onPlay)
                DetailSheetAction("↺", "Watch from beginning", onWatchFromBeginning)
                if (supportsMyList) {
                    DetailSheetAction(if (detail.isFavorite) "♡" else "＋", if (detail.isFavorite) "Remove from My List" else "Add to My List", onToggleFavorite)
                }
                DetailSheetAction("↓", "Save offline", { onQueueDownload(detail.id) })
                DetailSheetAction(if (detail.isPlayed) "↺" else "✓", if (detail.isPlayed) "Mark unwatched" else "Mark watched", onTogglePlayed)
                if (!detail.itemType.equals("Audio", ignoreCase = true) && !detail.itemType.equals("LiveTvChannel", ignoreCase = true)) {
                    DetailSheetAction("◌", "Start Watch Party", onStartWatchParty)
                }
                if (socialEnabled) {
                    DetailSheetAction("↗", "Share to a Friend", onShareToFriend)
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
private fun ShareMediaToFriendSheet(
    detail: JellyfinMediaDetail,
    friends: List<dev.vantafyn.core.jellyfin.JellyfinFriend>,
    onDismiss: () -> Unit,
    onShare: (dev.vantafyn.core.jellyfin.JellyfinFriend) -> Unit,
) {
    val context = LocalContext.current
    val sortedFriends = remember(friends) {
        friends.sortedWith(
            compareByDescending<dev.vantafyn.core.jellyfin.JellyfinFriend> { it.isOnline }
                .thenBy { it.displayName.lowercase() }
        )
    }

    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 28.dp, strokeWidth = 1.3.dp),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Share to a Friend",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Recommend \"${detail.title}\" directly into your chat",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            if (sortedFriends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No friends found yet.\nAdd friends from Friends & Messages to recommend movies and shows!",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sortedFriends, key = { it.userId }) { friend ->
                        VantafynGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dev.vantafyn.core.ui.VantafynSoundEffects.playMessageSent(context)
                                    onShare(friend)
                                },
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier.size(38.dp),
                                        contentAlignment = Alignment.BottomEnd,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E2638))
                                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!friend.avatarUrl.isNullOrBlank()) {
                                                coil3.compose.AsyncImage(
                                                    model = friend.avatarUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Text(
                                                    text = friend.displayName.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                        if (friend.isOnline) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E676))
                                                    .border(1.5.dp, Color(0xFF0D1019), CircleShape),
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = friend.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VantafynColors.Ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = if (friend.isOnline) "Active now" else "Offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (friend.isOnline) Color(0xFF55F0C0) else VantafynColors.Muted,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF00E5FF).copy(alpha = 0.20f), Color(0xFF7C4DFF).copy(alpha = 0.20f))
                                            )
                                        )
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.50f), CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Send,
                                            contentDescription = "Send",
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Text(
                                            text = "Send",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel",
                    tint = VantafynColors.Muted,
                )
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
        containerColor = VantafynModalContainerColor,
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = VantafynColors.Muted, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
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
        containerColor = VantafynModalContainerColor,
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
private fun PeopleSection(detail: JellyfinMediaDetail, onPerson: (java.util.UUID) -> Unit) {
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
                            .clickable(onClick = { onPerson(person.id) }),
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
    val supportsMyList = detail.itemType.supportsMyListAction()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(472.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black,
                                0.66f to Color.Black,
                                0.83f to Color.Black.copy(alpha = 0.78f),
                                0.94f to Color.Black.copy(alpha = 0.24f),
                                1.00f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            AsyncImage(
                model = detail.backdropUrl ?: detail.imageUrl,
                contentDescription = detail.title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(VantafynColors.SurfaceHigh),
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
                                0.86f to Color(0x64070A12),
                                1.00f to Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color(0xB8070A12), Color(0x44070A12), Color.Transparent))),
            )
        }
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
                if (supportsMyList) {
                    DetailFlatIconButton(if (detail.isFavorite) "♥" else "♡", onFavorite)
                }
                DetailFlatIconButton("⋯", onMore)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .padding(horizontal = VantafynSpacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            detail.logoUrl?.let {
                AsyncImage(model = it, contentDescription = detail.title, modifier = Modifier.fillMaxWidth(0.88f).height(160.dp), contentScale = ContentScale.Fit)
            } ?: DetailAutoTitle(detail.title)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.xl, vertical = 46.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            DetailChipRow(detailHeroChips(detail, finishAtLabel))
            DetailChipRow(detailHeroMetaChips(detail) + detail.genres.take(2))
        }
    }
}

@Composable
private fun PersonDetailHero(detail: JellyfinMediaDetail, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(472.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black,
                                0.66f to Color.Black,
                                0.83f to Color.Black.copy(alpha = 0.78f),
                                0.94f to Color.Black.copy(alpha = 0.24f),
                                1.00f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            AsyncImage(
                model = detail.backdropUrl ?: detail.imageUrl,
                contentDescription = detail.title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(VantafynColors.SurfaceHigh),
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
                                0.86f to Color(0x64070A12),
                                1.00f to Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color(0xB8070A12), Color(0x44070A12), Color.Transparent))),
            )
        }
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
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.xl, vertical = 46.dp),
            contentAlignment = Alignment.Center,
        ) {
            DetailAutoTitle(detail.title)
        }
    }
}

@Composable
private fun DetailAutoTitle(title: String) {
    val textMeasurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.headlineLarge
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val fontSize: TextUnit = with(LocalDensity.current) {
            val baseSize = style.fontSize
            val measured = textMeasurer.measure(
                text = AnnotatedString(title),
                style = style.copy(fontSize = baseSize, fontWeight = FontWeight.Bold),
                maxLines = 2,
            )
            val availablePx = availableWidth.roundToPx()
            if (measured.size.width <= availablePx) {
                baseSize
            } else {
                val ratio = availablePx.toFloat() / measured.size.width
                val scaled = baseSize * ratio
                if (scaled.value < 16f) 16.sp else scaled
            }
        }
        Text(
            title,
            color = VantafynColors.Ink,
            style = style.copy(fontSize = fontSize),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun detailHeroChips(detail: JellyfinMediaDetail, finishAtLabel: String?): List<String> =
    listOfNotNull(
        detail.year?.toString(),
        detail.runtimeMinutes?.let { "${it}m" },
        finishAtLabel,
    )

private fun detailHeroMetaChips(detail: JellyfinMediaDetail): List<String> =
    listOfNotNull(
        detail.officialRating,
        detail.communityRating?.let { "★ ${"%.1f".format(it)}" },
    )

private fun List<String>.withSingleDateChip(): List<String> {
    var hasDateChip = false
    return filter { value ->
        if (!value.isDateChip()) return@filter true
        if (hasDateChip) {
            false
        } else {
            hasDateChip = true
            true
        }
    }
}

private fun String.isDateChip(): Boolean {
    val value = trim()
    if (value.matches(Regex("""\d{4}"""))) return true
    if (value.matches(Regex("""\d{4}[-/]\d{1,2}([-/]\d{1,2})?"""))) return true
    if (value.matches(Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""))) return true
    return false
}

@Composable
private fun rememberFinishAtLabel(detail: JellyfinMediaDetail): String? {
    if ((detail.runtimeMinutes ?: 0) <= 0) return null
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowMs by remember(detail.id) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(detail.id, detail.runtimeMinutes, detail.playbackPositionTicks, detail.isPlayed, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                nowMs = System.currentTimeMillis()
                delay(15_000L)
            }
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
internal fun CompactBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
private fun LibrariesViewToggle(selected: LibrariesViewMode, onSelect: (LibrariesViewMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val icon = when (selected) {
        LibrariesViewMode.List -> Icons.Rounded.ViewAgenda
        LibrariesViewMode.Grid -> Icons.Rounded.GridView
    }
    Box {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = "View mode", tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.Transparent)
                .padding(0.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VantafynColors.Graphite.copy(alpha = 0.96f))
                    .vantafynAnimatedModalBorder(cornerRadius = 16.dp, strokeWidth = 1.5.dp)
                    .padding(vertical = 4.dp),
            ) {
                Column {
                    LibrariesViewMenuItem(
                        icon = Icons.Rounded.ViewAgenda,
                        label = "List",
                        selected = selected == LibrariesViewMode.List,
                        onClick = { onSelect(LibrariesViewMode.List); expanded = false },
                    )
                    LibrariesViewMenuItem(
                        icon = Icons.Rounded.GridView,
                        label = "Grid",
                        selected = selected == LibrariesViewMode.Grid,
                        onClick = { onSelect(LibrariesViewMode.Grid); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrariesViewMenuItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun LibraryGridCard(library: JellyfinLibrary, onClick: () -> Unit) {
    VantafynGlassTile(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color.Black.copy(alpha = 0.24f)),
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
            Text(
                library.name,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun LibraryListCard(
    library: JellyfinLibrary,
    arranging: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onStartArrange: () -> Unit = onLongPress,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
) {
    VantafynGlassTile(
        modifier = modifier
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
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!arranging) onClick() },
                    onLongClick = onStartArrange,
                ),
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(library.name, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            if (arranging) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    LibraryMoveButton(
                        icon = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Move ${library.name} up",
                        enabled = canMoveUp,
                        onClick = onMoveUp,
                    )
                    LibraryMoveButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Move ${library.name} down",
                        enabled = canMoveDown,
                        onClick = onMoveDown,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryMoveButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = VantafynColors.Ink.copy(alpha = if (enabled) 0.92f else 0.24f),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun FilterChips(labels: List<String>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
        items(labels, key = { it }) { label -> GlassAction(label) }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String, icon: ImageVector? = null) {
    VantafynGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))))
                        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color(0xFF31D7FF).copy(alpha = 0.18f))), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                }
            }
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 260.dp))
        }
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
            .heightIn(min = if (compact) 58.dp else 76.dp)
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
                Text(
                    title,
                    color = textColor,
                    style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                }
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
private fun BottomRailAccentBorder(
    mode: BottomRailAccent,
    tapTrigger: Int = 0,
    modifier: Modifier = Modifier,
) {
    if (mode == BottomRailAccent.Off) return
    val reducedMotion = rememberReducedMotionPreference()
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val infiniteTransition = rememberInfiniteTransition(label = "railAccent")
    val rippleAnim = remember { Animatable(0f) }
    LaunchedEffect(mode) {
        rippleAnim.snapTo(0f)
    }
    LaunchedEffect(tapTrigger) {
        if (mode == BottomRailAccent.TouchRipple && tapTrigger > 0 && isResumed) {
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
            rippleAnim.animateTo(0f, animationSpec = tween(700, easing = LinearEasing))
        }
    }
    val baseAlpha = when (mode) {
        BottomRailAccent.StillGlow -> 0.38f
        BottomRailAccent.Breathing -> {
            if (reducedMotion || !isResumed) 0.38f
            else infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.55f,
                animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Reverse),
                label = "breathe",
            ).value
        }
        BottomRailAccent.TouchRipple -> {
            if (reducedMotion) 0.38f
            else lerp(0.30f, 0.65f, rippleAnim.value).coerceIn(0f, 0.7f)
        }
        else -> 0f
    }
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val cornerRadius = 30.dp.toPx()
        val halfStroke = strokeWidth / 2f
        drawRoundRect(
            brush = VantafynGradients.accentHorizontal(),
            alpha = baseAlpha,
            topLeft = Offset(halfStroke, halfStroke),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth),
        )
    }
}

sealed interface NavigationRailMode {
    data class Main(val selected: MobileDestination) : NavigationRailMode
    data class Social(val selectedTab: dev.vantafyn.feature.home.SocialTab) : NavigationRailMode
}

@Composable
private fun SocialNavIcon(tab: dev.vantafyn.feature.home.SocialTab, selected: Boolean) {
    val color = if (selected) Color(0xFF8FE7FF) else Color.White.copy(alpha = 0.78f)
    Canvas(modifier = Modifier.size(23.dp)) {
        val stroke = 2.35.dp.toPx()
        val outline = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val selectedBrush = VantafynNavSelectedBrush()

        fun drawIconPath(path: Path) {
            if (selected) {
                drawPath(path = path, brush = selectedBrush, style = outline)
            } else {
                drawPath(path = path, color = color, style = outline)
            }
        }

        fun drawIconLine(start: androidx.compose.ui.geometry.Offset, end: androidx.compose.ui.geometry.Offset) {
            if (selected) {
                drawLine(brush = selectedBrush, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
            } else {
                drawLine(color = color, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }

        fun drawIconCircle(radius: Float, center: androidx.compose.ui.geometry.Offset) {
            if (selected) {
                drawCircle(brush = selectedBrush, radius = radius, center = center, style = outline)
            } else {
                drawCircle(color = color, radius = radius, center = center, style = outline)
            }
        }

        fun drawIconRoundRect(
            topLeft: androidx.compose.ui.geometry.Offset,
            size: androidx.compose.ui.geometry.Size,
            cornerRadius: androidx.compose.ui.geometry.CornerRadius,
        ) {
            if (selected) {
                drawRoundRect(brush = selectedBrush, topLeft = topLeft, size = size, cornerRadius = cornerRadius, style = outline)
            } else {
                drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = cornerRadius, style = outline)
            }
        }

        when (tab) {
            dev.vantafyn.feature.home.SocialTab.Messages -> {
                val path = Path().apply {
                    moveTo(4.5.dp.toPx(), 5.dp.toPx())
                    lineTo(18.5.dp.toPx(), 5.dp.toPx())
                    cubicTo(19.9.dp.toPx(), 5.dp.toPx(), 20.5.dp.toPx(), 5.6.dp.toPx(), 20.5.dp.toPx(), 7.dp.toPx())
                    lineTo(20.5.dp.toPx(), 14.dp.toPx())
                    cubicTo(20.5.dp.toPx(), 15.4.dp.toPx(), 19.9.dp.toPx(), 16.dp.toPx(), 18.5.dp.toPx(), 16.dp.toPx())
                    lineTo(10.5.dp.toPx(), 16.dp.toPx())
                    lineTo(6.dp.toPx(), 19.5.dp.toPx())
                    lineTo(6.dp.toPx(), 16.dp.toPx())
                    lineTo(4.5.dp.toPx(), 16.dp.toPx())
                    cubicTo(3.1.dp.toPx(), 16.dp.toPx(), 2.5.dp.toPx(), 15.4.dp.toPx(), 2.5.dp.toPx(), 14.dp.toPx())
                    lineTo(2.5.dp.toPx(), 7.dp.toPx())
                    cubicTo(2.5.dp.toPx(), 5.6.dp.toPx(), 3.1.dp.toPx(), 5.dp.toPx(), 4.5.dp.toPx(), 5.dp.toPx())
                    close()
                }
                drawIconPath(path)
            }
            dev.vantafyn.feature.home.SocialTab.Friends -> {
                // Two overlapping profile silhouettes
                // Main user
                drawIconCircle(radius = 3.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(8.5.dp.toPx(), 7.dp.toPx()))
                drawIconRoundRect(
                    topLeft = androidx.compose.ui.geometry.Offset(3.5.dp.toPx(), 13.5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 6.5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                )
                // Secondary user
                drawIconCircle(radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(16.5.dp.toPx(), 8.dp.toPx()))
                val secondPath = Path().apply {
                    moveTo(14.5.dp.toPx(), 14.dp.toPx())
                    cubicTo(16.5.dp.toPx(), 13.5.dp.toPx(), 19.5.dp.toPx(), 14.5.dp.toPx(), 20.dp.toPx(), 19.5.dp.toPx())
                }
                drawIconPath(secondPath)
            }
            dev.vantafyn.feature.home.SocialTab.Requests -> {
                drawIconRoundRect(
                    topLeft = androidx.compose.ui.geometry.Offset(3.dp.toPx(), 5.5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(17.dp.toPx(), 12.5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx()),
                )
                drawIconLine(
                    start = androidx.compose.ui.geometry.Offset(3.5.dp.toPx(), 6.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 12.dp.toPx()),
                )
                drawIconLine(
                    start = androidx.compose.ui.geometry.Offset(19.5.dp.toPx(), 6.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 12.dp.toPx()),
                )
            }
            dev.vantafyn.feature.home.SocialTab.Find -> {
                drawIconCircle(radius = 3.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(8.5.dp.toPx(), 7.dp.toPx()))
                drawIconRoundRect(
                    topLeft = androidx.compose.ui.geometry.Offset(3.5.dp.toPx(), 13.5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 6.5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                )
                drawIconLine(
                    start = androidx.compose.ui.geometry.Offset(17.5.dp.toPx(), 8.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(17.5.dp.toPx(), 16.dp.toPx()),
                )
                drawIconLine(
                    start = androidx.compose.ui.geometry.Offset(13.5.dp.toPx(), 12.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(21.5.dp.toPx(), 12.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun MobileBottomNav(
    mode: NavigationRailMode,
    onSelected: (MobileDestination) -> Unit,
    onSocialTabSelected: (dev.vantafyn.feature.home.SocialTab) -> Unit,
    onMusicLongPress: (() -> Unit)?,
    isAdmin: Boolean,
    isMusicPlaying: Boolean,
    pendingOmbiAccessRequestCount: Int,
    unreadMessagesCount: Int = 0,
    incomingFriendRequestsCount: Int = 0,
    accentMode: BottomRailAccent = BottomRailAccent.Off,
    modifier: Modifier = Modifier,
) {
    var tapTrigger by remember { mutableIntStateOf(0) }
    val mainTabs = remember(isAdmin) {
        buildList {
            add(MobileDestination.Home)
            add(MobileDestination.Libraries)
            add(MobileDestination.Search)
            add(MobileDestination.Music)
            add(MobileDestination.Favorites)
            add(MobileDestination.Requests)
            if (isAdmin) add(MobileDestination.Admin)
            if (!isAdmin) add(MobileDestination.Profile)
        }
    }
    val socialTabs = remember { dev.vantafyn.feature.home.SocialTab.entries }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(VantafynBottomScrim()),
    ) {
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.md, vertical = VantafynSpacing.sm),
        ) {
            VantafynGlassDock(modifier = Modifier.fillMaxWidth()) {
                AnimatedContent(
                    targetState = mode is NavigationRailMode.Social,
                    transitionSpec = {
                        val enterTransition = fadeIn(animationSpec = tween(200, delayMillis = 30, easing = FastOutSlowInEasing)) +
                            slideInVertically(animationSpec = tween(200, delayMillis = 30, easing = FastOutSlowInEasing)) { it / 5 }
                        val exitTransition = fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                            slideOutVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) { it / 5 }
                        (enterTransition togetherWith exitTransition).using(SizeTransform(clip = false))
                    },
                    label = "navigationRailContentMode",
                    modifier = Modifier.fillMaxWidth(),
                ) { isSocialMode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!isSocialMode) {
                            val selected = (mode as? NavigationRailMode.Main)?.selected
                            mainTabs.forEach { destination ->
                                val tabSelected = selected == destination || (selected == MobileDestination.HomeLayout && destination == MobileDestination.Profile)
                                val interactionSource = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .combinedClickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { tapTrigger++; onSelected(destination) },
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
                        } else {
                            val selectedSocialTab = (mode as? NavigationRailMode.Social)?.selectedTab
                            socialTabs.forEach { tab ->
                                val tabSelected = selectedSocialTab == tab
                                val interactionSource = remember { MutableInteractionSource() }
                                val badgeCount = when (tab) {
                                    dev.vantafyn.feature.home.SocialTab.Messages -> unreadMessagesCount
                                    dev.vantafyn.feature.home.SocialTab.Requests -> incomingFriendRequestsCount
                                    else -> 0
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .combinedClickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { tapTrigger++; onSocialTabSelected(tab) },
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
                                    SocialNavIcon(
                                        tab = tab,
                                        selected = tabSelected,
                                    )
                                    if (badgeCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 7.dp, end = 12.dp)
                                                .size(7.5.dp)
                                                .background(Color(0xFFFF3366), RoundedCornerShape(999.dp)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (accentMode != BottomRailAccent.Off) {
                BottomRailAccentBorder(
                    mode = accentMode,
                    tapTrigger = tapTrigger,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@Composable
private fun MusicQuickPlayerSheet(
    playback: VantafynMusicPlaybackState,
    controller: MusicPlaybackController,
    session: JellyfinSession?,
    onDismiss: () -> Unit,
    onOpenMusic: () -> Unit,
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel = viewModel(),
) {
    val musicState by musicViewModel.state.collectAsStateWithLifecycle()
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var popupLyricsPlayback by remember { mutableStateOf(playback) }
    val currentPlayback = if (showLyrics) popupLyricsPlayback else playback
    val track = currentPlayback.currentTrack
    val activeTrackId = track?.id
    val lifecycleOwner = LocalLifecycleOwner.current
    val lyricsRenderState = remember(activeTrackId, musicState.lyricsTrackId, musicState.isLyricsLoading, musicState.lyrics) {
        PopupLyricsRenderState(
            trackId = activeTrackId,
            lyricsTrackId = musicState.lyricsTrackId,
            lyrics = musicState.lyrics,
            isLoading = musicState.isLyricsLoading,
        )
    }

    LaunchedEffect(session) {
        musicViewModel.bindSession(session)
    }
    LaunchedEffect(showLyrics) {
        musicViewModel.setPopupLyricsActive(showLyrics)
        if (showLyrics) popupLyricsPlayback = controller.forcePlaybackSnapshot()
    }
    DisposableEffect(Unit) {
        onDispose {
            musicViewModel.setPopupLyricsActive(false)
        }
    }
    LaunchedEffect(showLyrics, lifecycleOwner) {
        if (!showLyrics) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                popupLyricsPlayback = controller.forcePlaybackSnapshot()
                delay(500L)
            }
        }
    }
    LaunchedEffect(track?.id) {
        if (track == null) showLyrics = false
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 10.dp, vertical = 82.dp),
    ) {
        VantafynGlassModalPanel(
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showLyrics) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { showLyrics = false },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.size(19.dp)) {
                                    val stroke = 2.35.dp.toPx()
                                    drawLine(
                                        color = VantafynColors.Ink,
                                        start = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 3.dp.toPx()),
                                        end = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 9.dp.toPx()),
                                        strokeWidth = stroke,
                                        cap = StrokeCap.Round,
                                    )
                                    drawLine(
                                        color = VantafynColors.Ink,
                                        start = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 9.dp.toPx()),
                                        end = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 15.dp.toPx()),
                                        strokeWidth = stroke,
                                        cap = StrokeCap.Round,
                                    )
                                }
                            }
                        } else {
                            Text("Now Playing", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoogleCastRouteButton(modifier = Modifier.size(38.dp))
                            if (!showLyrics) MiniPlayerTextButton("Open", onOpenMusic)
                            MiniPlayerTextButton("Close", onDismiss)
                        }
                    }
                    AnimatedContent(
                        targetState = showLyrics,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = 70, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
                        },
                        label = "quickPlayerLyricsMode",
                    ) { lyricsMode ->
                        if (track == null) {
                            MusicQuickPlayerEmptyState(onOpenMusic)
                        } else if (lyricsMode) {
                            PopupLyricsBody(
                                renderState = lyricsRenderState,
                                playbackMs = currentPlayback.positionMs,
                                isPlaying = currentPlayback.isPlaying,
                                currentPositionMs = controller::currentPositionMs,
                                onSeek = controller::seekTo,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(top = 10.dp),
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                MiniMusicProgress(currentPlayback)
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        MiniPlayerIconControl(Icons.Rounded.SkipPrevious, "Previous", onClick = controller::previous)
                                        MiniPlayerIconControl(
                                            if (currentPlayback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            if (currentPlayback.isPlaying) "Pause" else "Play",
                                            emphasized = true,
                                            onClick = controller::togglePlayPause,
                                        )
                                        MiniPlayerIconControl(Icons.Rounded.SkipNext, "Next", onClick = controller::next)
                                    }
                                    MiniPlayerSmallIconButton(
                                        icon = Icons.Rounded.Article,
                                        contentDescription = "Show lyrics",
                                        size = 32.dp,
                                        onClick = { showLyrics = true },
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicQuickPlayerEmptyState(onOpenMusic: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.13f),
                            Color(0xFF4AD7FF).copy(alpha = 0.10f),
                            Color(0xFF9B5CFF).copy(alpha = 0.14f),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = VantafynColors.Ink.copy(alpha = 0.92f),
                modifier = Modifier.size(42.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "Nothing playing",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Start a song and this panel becomes your quick control surface.",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        MiniPlayerTextButton("Browse music", onOpenMusic)
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

@Composable
private fun MiniPlayerSmallIconButton(
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = VantafynColors.Ink, modifier = Modifier.size(size * 0.55f))
    }
}

private data class PopupLyricsRenderState(
    val trackId: UUID?,
    val lyricsTrackId: UUID?,
    val lyrics: JellyfinLyrics?,
    val isLoading: Boolean,
)

@Composable
private fun PopupLyricsBody(
    renderState: PopupLyricsRenderState,
    playbackMs: Long,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        renderState.isLoading || renderState.trackId != renderState.lyricsTrackId -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                VantafynLoadingIndicator("Loading lyrics from Jellyfin")
            }
        }
        renderState.lyrics == null -> {
            PopupLyricsEmptyState(
                title = "No lyrics available",
                message = "Jellyfin did not expose lyrics for this track.",
                modifier = modifier,
            )
        }
        renderState.lyrics.isSynced -> {
            key(renderState.trackId, renderState.lyrics.syncedLines) {
                PopupSyncedLyricsView(
                    trackId = renderState.trackId,
                    lines = renderState.lyrics.syncedLines,
                    playbackMs = playbackMs,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    onSeek = onSeek,
                    modifier = modifier,
                )
            }
        }
        else -> {
            PopupPlainLyricsView(
                text = renderState.lyrics.plainText,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PopupSyncedLyricsView(
    trackId: UUID?,
    lines: List<JellyfinLyricLine>,
    playbackMs: Long,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeLineLeadMs = 120L
    var livePlaybackMs by remember(trackId, lines) { mutableLongStateOf(playbackMs) }
    val activeIndex = remember(lines, livePlaybackMs) { lines.activeIndex(livePlaybackMs + activeLineLeadMs).coerceAtLeast(0) }
    var suppressAutoFollowUntil by remember(trackId, lines) { mutableLongStateOf(0L) }
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            suppressAutoFollowUntil = System.currentTimeMillis() + 5_000L
        }
    }

    LaunchedEffect(playbackMs, isPlaying, trackId, lines) {
        if (!isPlaying || abs(playbackMs - livePlaybackMs) > 1_200L) {
            livePlaybackMs = playbackMs
        }
    }

    LaunchedEffect(trackId, lines, isPlaying, lifecycleOwner) {
        if (lines.isEmpty() || !isPlaying) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                livePlaybackMs = currentPositionMs()
                delay(PopupSyncedLyricsTickerIntervalMs)
            }
        }
    }

    LaunchedEffect(trackId, lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        suppressAutoFollowUntil = 0L
        val initialPos = currentPositionMs()
        livePlaybackMs = initialPos
        val targetIndex = lines.activeIndex(initialPos + activeLineLeadMs).coerceAtLeast(0)
        listState.scrollToItem(targetIndex.coerceAtMost(lines.lastIndex))
    }

    LaunchedEffect(activeIndex, lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        val isSuppressed = System.currentTimeMillis() < suppressAutoFollowUntil
        if (!isSuppressed) {
            val target = activeIndex.coerceIn(0, lines.lastIndex)
            if (target == 0) {
                listState.scrollToItem(0)
            } else {
                listState.animateScrollToItem(target)
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        if (lines.isEmpty()) {
            PopupLyricsEmptyState("No lyrics text", "Jellyfin returned an empty lyrics file.", Modifier.fillMaxSize())
            return@BoxWithConstraints
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = maxHeight * 0.42f, bottom = maxHeight * 0.48f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.startMs}-${line.text}-$index" }) { index, line ->
                PopupSyncedLyricLine(
                    line = line,
                    active = index == activeIndex,
                    onClick = {
                        line.startMs?.let {
                            suppressAutoFollowUntil = 0L
                            onSeek(it)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PopupSyncedLyricLine(line: JellyfinLyricLine, active: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.92f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "popupLyricLineScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.42f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "popupLyricLineAlpha",
    )
    Text(
        text = line.text.trim().ifBlank { "♪" },
        textAlign = TextAlign.Start,
        color = Color.White.copy(alpha = alpha),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            }
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = line.startMs != null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 2.dp),
    )
}

@Composable
private fun PopupPlainLyricsView(text: String, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val lines = remember(text) {
        text.trim()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    var suppressAutoScrollUntil by remember(text) { mutableStateOf(0L) }
    var autoScrolling by remember(text) { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(listState, text) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling && !autoScrolling) {
                suppressAutoScrollUntil = System.currentTimeMillis() + 2_000L
            }
        }
    }
    LaunchedEffect(lines, lifecycleOwner) {
        if (lines.isEmpty()) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                delay(1_400L)
                if (System.currentTimeMillis() < suppressAutoScrollUntil) continue
                val next = (listState.firstVisibleItemIndex + 1).coerceAtMost(lines.lastIndex)
                if (next == listState.firstVisibleItemIndex) {
                    delay(4_200L)
                    continue
                }
                autoScrolling = true
                listState.animateScrollToItem(next)
                autoScrolling = false
            }
        }
    }

    if (lines.isEmpty()) {
        PopupLyricsEmptyState("No lyrics text", "Jellyfin returned an empty lyrics file.", modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(top = 42.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(lines, key = { it }) { line ->
            Text(
                text = line,
                color = VantafynColors.Ink.copy(alpha = 0.86f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PopupLyricsEmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Article, contentDescription = null, tint = VantafynColors.Muted, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(message, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

private fun List<JellyfinLyricLine>.activeIndex(positionMs: Long): Int {
    if (isEmpty()) return -1
    var active = 0
    forEachIndexed { index, line ->
        val start = line.startMs ?: return@forEachIndexed
        if (start <= positionMs) active = index else return active
    }
    return active
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val transition = rememberInfiniteTransition(label = "navWater")
    val offset by if (isResumed) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "navWaterOffset",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val pulseAlpha = if (activePulse && isResumed) {
        val transition = rememberInfiniteTransition(label = "musicNavPulse")
        transition.animateFloat(
            initialValue = 0.08f,
            targetValue = 0.86f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
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
                Color(0xFF19D8FF).copy(alpha = pulseAlpha),
                Color(0xFF4E68FF).copy(alpha = pulseAlpha),
                Color(0xFF8F32FF).copy(alpha = pulseAlpha),
                Color(0xFFFF2EA6).copy(alpha = pulseAlpha),
            ),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
        )
        fun drawIconPath(path: Path) {
            when {
                selected -> drawPath(path = path, brush = selectedBrush, style = outline)
                activePulse -> {
                    drawPath(path, Color.White.copy(alpha = 0.88f - pulseAlpha * 0.28f), style = outline)
                    drawPath(path = path, brush = pulsingBrush, style = outline)
                }
                else -> drawPath(path, color, style = outline)
            }
        }
        fun drawIconLine(start: androidx.compose.ui.geometry.Offset, end: androidx.compose.ui.geometry.Offset) {
            when {
                selected -> drawLine(brush = selectedBrush, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
                activePulse -> {
                    drawLine(Color.White.copy(alpha = 0.88f - pulseAlpha * 0.28f), start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(brush = pulsingBrush, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
                }
                else -> drawLine(color, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
        fun drawIconCircle(radius: Float, center: androidx.compose.ui.geometry.Offset) {
            when {
                selected -> drawCircle(brush = selectedBrush, radius = radius, center = center, style = outline)
                activePulse -> {
                    drawCircle(Color.White.copy(alpha = 0.88f - pulseAlpha * 0.28f), radius = radius, center = center, style = outline)
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
                    drawRoundRect(Color.White.copy(alpha = 0.88f - pulseAlpha * 0.28f), topLeft = topLeft, size = size, cornerRadius = cornerRadius, style = outline)
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
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
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
                Text("App version $VANTAFYN_APP_VERSION")
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
            items(libraries, key = { it.id }) { library ->
                Box(modifier = Modifier.clickable { onOpenLibrary(library) }) {
                    PosterCard(title = library.name, spec = if (tv) TvPosterSpec else MobilePosterSpec)
                }
            }
        }
    }
}

@Composable
private fun CenterPane(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        val widthFraction = if (maxWidth > 700.dp) 0.56f else 0.88f
        Column(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .verticalScroll(rememberScrollState()),
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
    state.errorMessage?.let { message ->
        Spacer(Modifier.height(VantafynSpacing.md))
        VantafynGlassModalPanel(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.25.dp),
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(VantafynGradients.accentHorizontal()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        "Check this address",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        message.setupFriendlyError(),
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun String.setupFriendlyError(): String =
    when {
        contains("timed out", ignoreCase = true) || contains("timeout", ignoreCase = true) ->
            "The server did not answer in time. Check the address and try again."
        contains("resolve", ignoreCase = true) ->
            "That address could not be resolved. For local servers, enter the IP address directly."
        contains("reach", ignoreCase = true) ->
            "This device cannot reach that server on your network. Check Wi-Fi, VPN, or the server's local IP."
        contains("cleartext", ignoreCase = true) ->
            "This server is using HTTP. Enter the full local address and try again."
        else -> this
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
    val allowHomeCustomize: Boolean = false,
    val allowDownload: Boolean = true,
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
    onDownload: () -> Unit,
    onCustomizeHome: (() -> Unit)? = null,
) {
    val supportsMyList = target.itemType.supportsMyListAction()
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(target.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = when { target.title.length > 55 -> 17.sp; target.title.length > 35 -> 19.sp; else -> 22.sp })
                Text(target.subtitle ?: target.itemType?.searchGroupLabel() ?: "Media", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                onCustomizeHome?.let { action ->
                    ContextAction("✦", "Customize Home", action)
                }
                ContextAction("ⓘ", "View details", onViewDetails)
                if (supportsMyList) {
                    if (target.inMyList) {
                        ContextAction("♡", "Remove from My List", onRemoveFromMyList)
                    } else {
                        ContextAction("＋", "Add to My List", onAddToMyList)
                    }
                }
                if (target.itemType?.contains("Episode", ignoreCase = true) == true || target.itemType?.contains("Movie", ignoreCase = true) == true) {
                    ContextAction("✓", "Mark watched", onMarkWatched)
                    ContextAction("↺", "Mark unwatched", onMarkUnwatched)
                }
                if (target.allowDownload) {
                    ContextAction("↓", "Save offline", onDownload)
                }
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

private fun JellyfinHeroMediaItem.toMediaActionTarget(allowHomeCustomize: Boolean = false): MediaActionTarget =
    MediaActionTarget(
        id = id,
        title = title,
        subtitle = subtitle ?: year?.toString(),
        itemType = "Media",
        allowHomeCustomize = allowHomeCustomize,
        allowDownload = false,
    )

private fun MobileDestination.bottomNavRoot(previous: MobileDestination): MobileDestination =
    when (this) {
        MobileDestination.MediaDetail,
        MobileDestination.Player -> previous.bottomNavRoot(MobileDestination.Home)
        MobileDestination.LibraryDetail -> MobileDestination.Libraries
        MobileDestination.AdminUserSettings -> MobileDestination.Admin
        MobileDestination.HomeLayout,
        MobileDestination.PlaybackPreferences,
        MobileDestination.DiscoverVantafyn,
        MobileDestination.DeviceQuickConnect -> MobileDestination.Profile
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

private fun String?.supportsMyListAction(): Boolean =
    equals("Movie", ignoreCase = true) ||
        equals("Series", ignoreCase = true) ||
        equals("Episode", ignoreCase = true) ||
        equals("BoxSet", ignoreCase = true) ||
        equals("Audio", ignoreCase = true) ||
        equals("MusicAlbum", ignoreCase = true) ||
        equals("Book", ignoreCase = true) ||
        equals("LiveTvChannel", ignoreCase = true) ||
        equals("LiveTvProgram", ignoreCase = true)

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

private fun SyncPlayConnectionState.watchPartyLabel(): String =
    when (this) {
        SyncPlayConnectionState.Unsupported -> "Local"
        SyncPlayConnectionState.Disconnected -> "Offline"
        SyncPlayConnectionState.Connecting -> "Connecting"
        SyncPlayConnectionState.Connected -> "Connected"
        SyncPlayConnectionState.Reconnecting -> "Reconnecting"
        SyncPlayConnectionState.Failed -> "Needs retry"
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

private const val VANTAFYN_APP_VERSION = "0.9.3"
private const val PopupSyncedLyricsTickerIntervalMs = 250L

@Composable
private fun FloatingSocialDock(
    unreadCount: Int,
    incomingRequestCount: Int,
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    onDragStateChanged: (isDragging: Boolean, isHoveringDismiss: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val totalBadge = unreadCount + incomingRequestCount
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val haptic = LocalHapticFeedback.current

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val leftSnapOffsetPx = with(density) { -(screenWidthDp - 44.dp - 36.dp).toPx() }
    val centerXOffsetPx = leftSnapOffsetPx / 2f
    val maxUpOffsetPx = with(density) { -(screenHeightDp - 220.dp).toPx() }
    val maxDownOffsetPx = with(density) { 30.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isHoveringDismiss by remember { mutableStateOf(false) }

    val badgeScale = remember { Animatable(1f) }
    var prevBadgeCount by remember { mutableIntStateOf(totalBadge) }

    LaunchedEffect(totalBadge) {
        if (totalBadge > prevBadgeCount && totalBadge > 0) {
            badgeScale.snapTo(1.35f)
            badgeScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
        }
        prevBadgeCount = totalBadge
    }

    val pulseScaleAnim = remember { Animatable(1f) }
    val pulseAlphaAnim = remember { Animatable(0f) }

    LaunchedEffect(totalBadge) {
        if (totalBadge > 0) {
            repeat(3) {
                launch {
                    pulseScaleAnim.snapTo(1f)
                    pulseScaleAnim.animateTo(1.22f, tween(700, easing = FastOutSlowInEasing))
                    pulseScaleAnim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                }
                launch {
                    pulseAlphaAnim.snapTo(0.45f)
                    pulseAlphaAnim.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
                }
                delay(1200L)
            }
        }
    }

    val pulseScale = pulseScaleAnim.value
    val pulseAlpha = pulseAlphaAnim.value

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .scale(if (isHoveringDismiss) 0.85f else 1f)
            .size(48.dp)
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocityTracker.resetTracking()
                    velocityTracker.addPointerInputChange(down)
                    var totalDragDistance = 0f
                    val pointerId = down.id
                    isDragging = true
                    onDragStateChanged(true, false)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (change.changedToUp()) {
                            change.consume()
                            break
                        }
                        val dragAmount = change.positionChange()
                        if (dragAmount != Offset.Zero) {
                            change.consume()
                            totalDragDistance += dragAmount.getDistance()
                            velocityTracker.addPointerInputChange(change)

                            val newX = (offsetX.value + dragAmount.x).coerceIn(leftSnapOffsetPx - 30f, 30f)
                            val newY = (offsetY.value + dragAmount.y).coerceIn(maxUpOffsetPx - 30f, maxDownOffsetPx + 30f)

                            val hovering = kotlin.math.abs(newX - centerXOffsetPx) < 140f && newY > -80f
                            if (hovering != isHoveringDismiss) {
                                isHoveringDismiss = hovering
                                if (hovering) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onDragStateChanged(true, hovering)
                            }

                            coroutineScope.launch {
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                            }
                        }
                    }
                    val wasHovering = isHoveringDismiss
                    isDragging = false
                    isHoveringDismiss = false
                    onDragStateChanged(false, false)

                    if (totalDragDistance < viewConfiguration.touchSlop) {
                        onClick()
                    } else if (wasHovering) {
                        coroutineScope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            launch {
                                offsetX.animateTo(centerXOffsetPx, tween(180, easing = FastOutSlowInEasing))
                            }
                            launch {
                                offsetY.animateTo(maxDownOffsetPx, tween(180, easing = FastOutSlowInEasing))
                            }
                            delay(180L)
                            onDismiss()
                        }
                    } else {
                        val velocity = velocityTracker.calculateVelocity()
                        coroutineScope.launch {
                            var currentVx = velocity.x
                            var currentVy = velocity.y
                            var currX = offsetX.value
                            var currY = offsetY.value
                            val friction = 0.94f
                            val bounceRestitution = 0.65f
                            var lastFrameTimeNanos = 0L

                            if (kotlin.math.hypot(currentVx.toDouble(), currentVy.toDouble()) > 400.0) {
                                var bounces = 0
                                while (bounces < 5 && kotlin.math.hypot(currentVx.toDouble(), currentVy.toDouble()) > 250.0) {
                                    withFrameNanos { timeNanos ->
                                        if (lastFrameTimeNanos > 0L) {
                                            val dt = ((timeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                                            currX += currentVx * dt
                                            currY += currentVy * dt
                                            currentVx *= Math.pow(friction.toDouble(), (dt * 60f).toDouble()).toFloat()
                                            currentVy *= Math.pow(friction.toDouble(), (dt * 60f).toDouble()).toFloat()

                                            if (currX < leftSnapOffsetPx) {
                                                currX = leftSnapOffsetPx
                                                currentVx = -currentVx * bounceRestitution
                                                bounces++
                                            }
                                            if (currX > 0f) {
                                                currX = 0f
                                                currentVx = -currentVx * bounceRestitution
                                                bounces++
                                            }
                                            if (currY < maxUpOffsetPx) {
                                                currY = maxUpOffsetPx
                                                currentVy = -currentVy * bounceRestitution
                                                bounces++
                                            }
                                            if (currY > maxDownOffsetPx) {
                                                currY = maxDownOffsetPx
                                                currentVy = -currentVy * bounceRestitution
                                                bounces++
                                            }
                                        }
                                        lastFrameTimeNanos = timeNanos
                                    }
                                    offsetX.snapTo(currX)
                                    offsetY.snapTo(currY)
                                }
                            }

                            val targetX = if (currX < leftSnapOffsetPx / 2f) leftSnapOffsetPx else 0f
                            val targetY = currY.coerceIn(maxUpOffsetPx, maxDownOffsetPx)
                            launch {
                                offsetX.animateTo(
                                    targetValue = targetX,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow,
                                    ),
                                )
                            }
                            launch {
                                offsetY.animateTo(
                                    targetValue = targetY,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Main circular bubble body
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(VantafynColors.SurfaceHigh.copy(alpha = if (isDragging) 0.98f else 0.90f))
                .vantafynAnimatedModalBorder(cornerRadius = 22.dp, strokeWidth = 1.3.dp, durationMillis = 3800),
            contentAlignment = Alignment.Center,
        ) {
            if (totalBadge > 0 && pulseAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00E5FF).copy(alpha = pulseAlpha),
                                    Color(0xFFFF3366).copy(alpha = pulseAlpha),
                                ),
                            ),
                            CircleShape,
                        ),
                )
            }
            Icon(
                imageVector = Icons.Rounded.Forum,
                contentDescription = "Friends & Messages",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        // Notification count badge (Placed cleanly above bubble, not clipped)
        if (totalBadge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .scale(badgeScale.value)
                    .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF3366),
                                Color(0xFFFF5E8A),
                            ),
                        ),
                    )
                    .border(1.5.dp, Color(0xFF07090F), CircleShape)
                    .padding(horizontal = 4.5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (totalBadge > 99) "99+" else totalBadge.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SocialIslandBanner(
    message: dev.vantafyn.core.jellyfin.JellyfinSocialMessage,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(message.messageId) {
        dev.vantafyn.core.ui.VantafynSoundEffects.playNewMessageAlert(context)
    }
    Box(
        modifier = modifier
            .swipeToDismissTopNotification(onDismiss)
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF141828).copy(alpha = 0.96f),
                        Color(0xFF0C101C).copy(alpha = 0.96f),
                    ),
                ),
            )
            .vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.3.dp, durationMillis = 3500)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2638))
                    .border(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!message.senderAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = message.senderAvatarUrl,
                        contentDescription = message.senderName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = message.senderName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "NEW MESSAGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 0.8.sp,
                    )
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = VantafynColors.Muted,
                    )
                }
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dev.vantafyn.core.jellyfin.formatSocialSnippet(message.content),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss",
                    tint = VantafynColors.Muted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FloatingSocialPanel(
    friends: List<dev.vantafyn.core.jellyfin.JellyfinFriend>,
    requests: List<dev.vantafyn.core.jellyfin.JellyfinFriendRequest>,
    conversations: List<dev.vantafyn.core.jellyfin.JellyfinSocialConversation>,
    onDismiss: () -> Unit,
    onOpenFullSocial: () -> Unit,
    onOpenChatWithFriend: (dev.vantafyn.core.jellyfin.JellyfinFriend) -> Unit,
    onOpenChatFromConversation: (dev.vantafyn.core.jellyfin.JellyfinSocialConversation) -> Unit,
    onAcceptRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val incomingRequests = remember(requests) { requests.filter { it.isIncoming } }

    AlertDialog(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .vantafynAnimatedModalBorder(cornerRadius = 28.dp, strokeWidth = 1.3.dp),
        onDismissRequest = onDismiss,
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forum,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "Social Hub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VantafynColors.Ink,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (incomingRequests.isNotEmpty()) {
                    val firstReq = incomingRequests.first()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF55F0C0).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF55F0C0).copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Friend Request: ${firstReq.senderName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF55F0C0),
                            )
                            if (incomingRequests.size > 1) {
                                Text(
                                    text = "+${incomingRequests.size - 1} more pending",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VantafynColors.Muted,
                                )
                            }
                        }
                        VantafynButton(
                            text = "Accept",
                            onClick = { onAcceptRequest(firstReq.id) },
                        )
                    }
                }

                if (incomingRequests.isEmpty() && conversations.isEmpty() && friends.isEmpty()) {
                    VantafynGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        contentPadding = PaddingValues(20.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .vantafynAnimatedModalBorder(cornerRadius = 52.dp, strokeWidth = 1.2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PersonAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Text(
                                text = "Start Connecting",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VantafynColors.Ink,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Search for friends and start chatting on this server.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VantafynColors.Muted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else if (conversations.isNotEmpty()) {
                    Text(
                        text = "RECENT CHATS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = VantafynColors.Muted,
                    )
                }

                if (conversations.isEmpty() && friends.isNotEmpty()) {
                    Text(
                        text = "No active chats yet. Tap any friend below to start messaging!",
                        style = MaterialTheme.typography.bodySmall,
                        color = VantafynColors.Muted,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                } else {
                    conversations.take(3).forEach { conv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    onDismiss()
                                    onOpenChatFromConversation(conv)
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!conv.peerAvatarUrl.isNullOrBlank()) {
                                    coil3.compose.AsyncImage(
                                        model = conv.peerAvatarUrl,
                                        contentDescription = conv.peerName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(
                                        text = conv.peerName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = VantafynColors.Ink,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.peerName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = VantafynColors.Ink,
                                )
                                Text(
                                    text = dev.vantafyn.core.jellyfin.formatSocialSnippet(conv.lastMessageText),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VantafynColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (conv.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .sizeIn(minWidth = 19.dp, minHeight = 19.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3366))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = conv.unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                val onlineFriends = remember(friends) { friends.filter { it.isOnline } }
                val displayFriends = remember(friends, onlineFriends) {
                    if (onlineFriends.isNotEmpty()) onlineFriends else friends
                }

                if (friends.isNotEmpty()) {
                    Text(
                        text = if (onlineFriends.isNotEmpty()) "ONLINE FRIENDS (${onlineFriends.size})" else "FRIENDS (${friends.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = VantafynColors.Muted,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        displayFriends.take(6).forEach { friend ->
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onDismiss()
                                        onOpenChatWithFriend(friend)
                                    }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .border(
                                            1.dp,
                                            if (friend.isOnline) Color(0xFF55F0C0).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.18f),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!friend.avatarUrl.isNullOrBlank()) {
                                        coil3.compose.AsyncImage(
                                            model = friend.avatarUrl,
                                            contentDescription = friend.displayName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Text(
                                            text = friend.displayName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = VantafynColors.Ink,
                                        )
                                    }
                                    if (friend.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF55F0C0))
                                                .border(1.5.dp, Color(0xFF090B12), CircleShape)
                                                .align(Alignment.BottomEnd),
                                        )
                                    }
                                }
                                Text(
                                    text = friend.displayName.take(8),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = if (friend.isOnline) Color(0xFF55F0C0) else VantafynColors.Ink,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            VantafynButton(
                text = "Open Full Social Screen",
                onClick = onOpenFullSocial,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = {},
    )
}

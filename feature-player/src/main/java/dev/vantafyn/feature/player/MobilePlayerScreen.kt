package dev.vantafyn.feature.player

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import dev.vantafyn.core.cast.GoogleCastRouteButton
import dev.vantafyn.core.cast.PlaybackOutputCoordinator
import dev.vantafyn.core.cast.PlaybackOutputType
import dev.vantafyn.core.cast.CastSubtitleTrack
import dev.vantafyn.core.jellyfin.JellyfinMediaSegment
import dev.vantafyn.core.jellyfin.JellyfinMediaSegmentBehavior
import dev.vantafyn.core.jellyfin.JellyfinMediaSegmentType
import dev.vantafyn.core.media.UpNextCandidate
import dev.vantafyn.core.media.UpNextDisplayMode
import dev.vantafyn.core.media.UpNextState
import dev.vantafyn.core.media.VantafynExoPlayerFactory
import dev.vantafyn.core.media.VantafynAudioTrack
import dev.vantafyn.core.media.VantafynPlaybackItem
import dev.vantafyn.core.media.VantafynSubtitleTrack
import dev.vantafyn.core.media.VantafynTrackInfo
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassModalPanel
import dev.vantafyn.core.ui.VantafynGlassPanel
import dev.vantafyn.core.ui.VantafynGradientSpinner
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

@Composable
fun MobilePlayerScreen(
    item: VantafynPlaybackItem?,
    isLoading: Boolean,
    errorMessage: String?,
    canTryTranscode: Boolean,
    onBack: (Long) -> Unit,
    onRetry: () -> Unit,
    onTryTranscode: () -> Unit,
    onStarted: (Long) -> Unit,
    onProgress: (Long, Boolean) -> Unit,
    onEnded: (Long) -> Unit,
    onPlayNext: (UpNextCandidate, Long) -> Unit,
    onPlayPrevious: (UpNextCandidate, Long) -> Unit,
    onPlayerError: () -> Unit,
    onPrepareCastPlayback: (Long) -> Unit,
    onSelectAudioTrack: (Int, Long) -> Unit,
    onSelectSubtitleTrack: (Int?, Long) -> Unit,
    suppressUpNext: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var lastPositionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(item?.streamUrl) {
        lastPositionMs = item?.startPositionMs ?: 0L
    }
    BackHandler { onBack(lastPositionMs) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            item != null -> PlayerSurface(
                item = item,
                canTryTranscode = canTryTranscode,
                onPosition = { lastPositionMs = it },
                onBack = { onBack(lastPositionMs) },
                onRetry = onRetry,
                onTryTranscode = onTryTranscode,
                onStarted = onStarted,
                onProgress = onProgress,
                onEnded = onEnded,
                onPlayNext = onPlayNext,
                onPlayPrevious = onPlayPrevious,
                onPlayerError = onPlayerError,
                onPrepareCastPlayback = onPrepareCastPlayback,
                onSelectAudioTrack = onSelectAudioTrack,
                onSelectSubtitleTrack = onSelectSubtitleTrack,
                suppressUpNext = suppressUpNext,
            )
            isLoading -> PlayerLoadingIndicator(
                text = "Preparing playback",
                modifier = Modifier.align(Alignment.Center),
            )
            else -> PlaybackErrorOverlay(
                message = errorMessage ?: "Playback could not start.",
                canTryTranscode = canTryTranscode,
                onRetry = onRetry,
                onTryTranscode = onTryTranscode,
                onClose = { onBack(lastPositionMs) },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun PlayerSurface(
    item: VantafynPlaybackItem,
    canTryTranscode: Boolean,
    onPosition: (Long) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onTryTranscode: () -> Unit,
    onStarted: (Long) -> Unit,
    onProgress: (Long, Boolean) -> Unit,
    onEnded: (Long) -> Unit,
    onPlayNext: (UpNextCandidate, Long) -> Unit,
    onPlayPrevious: (UpNextCandidate, Long) -> Unit,
    onPlayerError: () -> Unit,
    onPrepareCastPlayback: (Long) -> Unit,
    onSelectAudioTrack: (Int, Long) -> Unit,
    onSelectSubtitleTrack: (Int?, Long) -> Unit,
    suppressUpNext: Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val outputCoordinator = remember(context) { PlaybackOutputCoordinator.get(context) }
    val outputState by outputCoordinator.state.collectAsStateWithLifecycle()
    var controlsVisible by remember(item.streamUrl) { mutableStateOf(true) }
    var isPlaying by remember(item.streamUrl) { mutableStateOf(false) }
    var isBuffering by remember(item.streamUrl) { mutableStateOf(true) }
    var durationMs by remember(item.streamUrl) { mutableLongStateOf(item.durationMs ?: 0L) }
    var positionMs by remember(item.streamUrl) { mutableLongStateOf(item.startPositionMs) }
    var started by remember(item.streamUrl) { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
    var castSubtitleSheetVisible by remember { mutableStateOf(false) }
    var playbackSpeed by remember(item.streamUrl) { mutableFloatStateOf(1f) }
    var resizeMode by remember(item.streamUrl) { mutableStateOf(PlayerResizeMode.Fit) }
    var selectedAudioIndex by remember(item.streamUrl) { mutableStateOf(item.selectedAudioStreamIndex) }
    var selectedSubtitleIndex by remember(item.streamUrl) { mutableStateOf(item.selectedSubtitleStreamIndex) }
    var upNextState by remember(item.itemId) { mutableStateOf<UpNextState>(UpNextState.Hidden) }
    var upNextCancelled by remember(item.itemId) { mutableStateOf(false) }
    var nextStarted by remember(item.itemId) { mutableStateOf(false) }
    var previousStarted by remember(item.itemId) { mutableStateOf(false) }
    var lastCastProgressReportMs by remember(item.itemId) { mutableLongStateOf(-1L) }
    var autoSkippedSegmentIds by remember(item.itemId) { mutableStateOf(emptySet<String>()) }
    val previousCandidate = item.previousCandidate
    val upNextCandidate = item.upNextCandidate
    val autoplaySettings = item.autoplaySettings
    val passoutProtectionLimitReached = autoplaySettings.passoutProtectionEnabled &&
        (System.currentTimeMillis() - item.continuousPlaybackStartedAtMs).coerceAtLeast(0L) >=
        autoplaySettings.passoutProtectionLimitMinutes * 60_000L
    val canUseUpNext = upNextCandidate != null &&
        !suppressUpNext &&
        autoplaySettings.enabled &&
        !passoutProtectionLimitReached &&
        autoplaySettings.onlyForEpisodes &&
        item.itemType.equals("Episode", ignoreCase = true) &&
        !item.isLiveStream
    val isCastingThisItem = outputState.activeOutput == PlaybackOutputType.GoogleCast &&
        outputState.castState.currentItemId == item.itemId
    val castState = outputState.castState
    val activeSegment = remember(positionMs, item.mediaSegments, durationMs) {
        item.mediaSegments.activeAt(positionMs, durationMs)
    }
    val activeSegmentBehavior = activeSegment?.let { segment ->
        item.mediaSegmentBehaviors[segment.type] ?: JellyfinMediaSegmentBehavior.DoNothing
    } ?: JellyfinMediaSegmentBehavior.DoNothing
    val trackSelector = remember(item.streamUrl) { DefaultTrackSelector(context) }
    val player = remember(item.streamUrl) {
        VantafynExoPlayerFactory.builder(context, trackSelector)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )
                setMediaItem(item.toMediaItem())
                prepare()
                if (item.startPositionMs > 0L) seekTo(item.startPositionMs)
                playWhenReady = !item.isCastResolved
            }
    }
    val skipSegment: (JellyfinMediaSegment) -> Unit = { segment ->
        val targetMs = (segment.endMs + 1L).coerceAtLeast(0L)
        autoSkippedSegmentIds = autoSkippedSegmentIds + segment.id.toString()
        if (isCastingThisItem) {
            outputCoordinator.seekTo(targetMs)
            positionMs = targetMs
            onPosition(targetMs)
            onProgress(targetMs, !castState.isPlaying)
        } else {
            val shouldContinuePlaying = player.playWhenReady || player.isPlaying
            player.seekTo(targetMs)
            if (shouldContinuePlaying) {
                player.play()
            } else {
                player.pause()
            }
            positionMs = targetMs
            onPosition(targetMs)
            onProgress(targetMs, !shouldContinuePlaying)
        }
    }
    KeepScreenAwake(enabled = !isCastingThisItem && (isPlaying || (isBuffering && player.playWhenReady)))
    PlayerImmersiveMode()

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                when (playbackState) {
                    Player.STATE_READY -> {
                        durationMs = player.duration.takeIf { it > 0 } ?: item.durationMs ?: durationMs
                        if (!started) {
                            started = true
                            onStarted(player.currentPosition)
                            selectedAudioIndex?.let { player.applyTrackSelection(C.TRACK_TYPE_AUDIO, it, item.audioTracks) }
                            player.applyTrackSelection(C.TRACK_TYPE_TEXT, selectedSubtitleIndex, item.subtitleTracks)
                        }
                    }
                    Player.STATE_ENDED -> {
                        val candidate = upNextCandidate
                        if (
                            candidate != null &&
                            canUseUpNext &&
                            autoplaySettings.playNextOnCompletion &&
                            !upNextCancelled &&
                            !nextStarted
                        ) {
                            if (autoplaySettings.displayMode == UpNextDisplayMode.AfterCompletion) {
                                controlsVisible = false
                                upNextState = UpNextState.Available(
                                    candidate = candidate,
                                    countdownSeconds = autoplaySettings.countdownSeconds,
                                    autoplayEnabled = true,
                                    shownAfterCompletion = true,
                                )
                            } else {
                                nextStarted = true
                                upNextState = UpNextState.PlayingNext
                                onPlayNext(candidate, player.currentPosition)
                            }
                        } else {
                            onEnded(player.currentPosition)
                        }
                    }
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
                if (value) controlsVisible = false
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlayerError()
            }
        }
        player.addListener(listener)
        onDispose {
            onProgress(player.currentPosition, true)
            player.removeListener(listener)
            player.stop()
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    player.pause()
                    onProgress(player.currentPosition, true)
                }
                Lifecycle.Event.ON_START -> Unit
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(outputState.castState.connectionState, outputState.activeOutput, item.streamUrl) {
        if (
            outputState.castState.connectionState == dev.vantafyn.core.cast.RemoteConnectionState.Connected &&
            outputState.activeOutput != PlaybackOutputType.GoogleCast &&
            item.itemType?.lowercase() in setOf("movie", "episode") &&
            !item.isLiveStream
        ) {
            val handoffPosition = player.currentPosition.coerceAtLeast(positionMs)
            player.pause()
            onProgress(handoffPosition, true)
            if (item.isCastResolved) {
                outputCoordinator.loadVideo(item, handoffPosition)
            } else {
                onPrepareCastPlayback(handoffPosition)
            }
        }
    }

    LaunchedEffect(isCastingThisItem) {
        if (isCastingThisItem) {
            player.pause()
            started = true
            onStarted(castState.positionMs)
        }
    }

    LaunchedEffect(player, isCastingThisItem, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                if (!isCastingThisItem) {
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                    durationMs = player.duration.takeIf { it > 0 } ?: durationMs
                    onPosition(positionMs)
                }
                delay(500L)
            }
        }
    }

    LaunchedEffect(isCastingThisItem, castState.positionMs, castState.durationMs) {
        if (isCastingThisItem) {
            positionMs = castState.positionMs
            durationMs = castState.durationMs.takeIf { it > 0L } ?: durationMs
            onPosition(positionMs)
        }
    }

    LaunchedEffect(player, started, isCastingThisItem, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                delay(7_000L)
                if (started && !isCastingThisItem) onProgress(player.currentPosition, !player.isPlaying)
            }
        }
    }

    LaunchedEffect(isCastingThisItem, castState.positionMs, castState.isPlaying) {
        if (!isCastingThisItem) return@LaunchedEffect
        if (lastCastProgressReportMs < 0L || abs(castState.positionMs - lastCastProgressReportMs) >= 10_000L) {
            lastCastProgressReportMs = castState.positionMs
            onProgress(castState.positionMs, !castState.isPlaying)
        }
    }

    LaunchedEffect(activeSegment?.id) {
        if (activeSegment == null && autoSkippedSegmentIds.isNotEmpty()) {
            autoSkippedSegmentIds = emptySet()
        }
    }

    LaunchedEffect(activeSegment?.id, activeSegmentBehavior, isCastingThisItem) {
        val segment = activeSegment ?: return@LaunchedEffect
        if (activeSegmentBehavior != JellyfinMediaSegmentBehavior.AutoSkip) return@LaunchedEffect
        if (segment.id.toString() in autoSkippedSegmentIds) return@LaunchedEffect
        skipSegment(segment)
    }

    LaunchedEffect(controlsVisible, isPlaying, sheet) {
        if (controlsVisible && isPlaying && sheet == null) {
            delay(4_200L)
            if (isPlaying && sheet == null) controlsVisible = false
        }
    }

    LaunchedEffect(positionMs, durationMs, canUseUpNext, upNextCancelled, nextStarted) {
        val candidate = upNextCandidate ?: return@LaunchedEffect
        if (autoplaySettings.displayMode == UpNextDisplayMode.AfterCompletion) {
            if ((upNextState as? UpNextState.Available)?.shownAfterCompletion != true) {
                upNextState = UpNextState.Hidden
            }
            return@LaunchedEffect
        }
        if (!canUseUpNext || durationMs <= 0L || nextStarted) {
            if (upNextState is UpNextState.Available) upNextState = UpNextState.Hidden
            return@LaunchedEffect
        }
        val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
        val watchedPercent = positionMs.toFloat() / durationMs.toFloat()
        val shouldShow = !upNextCancelled &&
            (remainingMs <= autoplaySettings.showBeforeEndSeconds * 1_000L || watchedPercent >= autoplaySettings.showBeforeEndPercent)
        val seekedAway = remainingMs > (autoplaySettings.showBeforeEndSeconds + 20) * 1_000L && watchedPercent < autoplaySettings.showBeforeEndPercent
        when {
            shouldShow && upNextState is UpNextState.Hidden -> {
                upNextState = UpNextState.Available(candidate, autoplaySettings.countdownSeconds, autoplayEnabled = true)
            }
            seekedAway && upNextState is UpNextState.Available -> {
                upNextState = UpNextState.Hidden
            }
        }
    }

    LaunchedEffect(upNextState, isPlaying, item.itemId) {
        val available = upNextState as? UpNextState.Available ?: return@LaunchedEffect
        if (!available.autoplayEnabled || (!isPlaying && !available.shownAfterCompletion) || nextStarted) return@LaunchedEffect
        delay(1_000L)
        val nextSeconds = available.countdownSeconds - 1
        if (nextSeconds <= 0) {
            nextStarted = true
            upNextState = UpNextState.PlayingNext
            onPlayNext(available.candidate, player.currentPosition)
        } else {
            upNextState = available.copy(countdownSeconds = nextSeconds)
        }
    }

    BackHandler(enabled = upNextState is UpNextState.Available) {
        val candidate = (upNextState as? UpNextState.Available)?.candidate
        upNextCancelled = true
        upNextState = candidate?.let { UpNextState.Cancelled(it) } ?: UpNextState.Hidden
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsVisible = !controlsVisible },
    ) {
        if (isCastingThisItem) {
            CastControllerSurface(
                item = item,
                receiverName = castState.receiverName,
                positionMs = castState.positionMs,
                durationMs = castState.durationMs.takeIf { it > 0L } ?: durationMs,
                isPlaying = castState.isPlaying,
                errorMessage = outputState.lastErrorMessage,
                subtitleTracks = castState.subtitleTracks,
                activeSubtitleTrackId = castState.activeSubtitleTrackId,
                onPlayPause = outputCoordinator::playPause,
                onSeekTo = outputCoordinator::seekTo,
                onSeekBy = { delta ->
                    val target = (castState.positionMs + delta).coerceIn(0L, (castState.durationMs.takeIf { it > 0L } ?: durationMs).coerceAtLeast(0L))
                    outputCoordinator.seekTo(target)
                    onProgress(target, !castState.isPlaying)
                },
                onPlayPrevious = {
                    val candidate = previousCandidate ?: return@CastControllerSurface
                    if (!previousStarted) {
                        previousStarted = true
                        onPlayPrevious(candidate, castState.positionMs)
                    }
                },
                onPlayNext = {
                    val candidate = upNextCandidate ?: return@CastControllerSurface
                    if (!nextStarted) {
                        nextStarted = true
                        upNextState = UpNextState.PlayingNext
                        onPlayNext(candidate, castState.positionMs)
                    }
                },
                onStopCasting = {
                    onProgress(castState.positionMs, true)
                    outputCoordinator.disconnect(stopPlayback = true)
                    onBack()
                },
                onSubtitles = { castSubtitleSheetVisible = true },
                onPlayHere = {
                    val resumePosition = castState.positionMs
                    outputCoordinator.playVideoOnThisDevice(stopCastPlayback = true)
                    player.seekTo(resumePosition)
                    player.play()
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AndroidView(
                factory = {
                    val initialResizeMode = resizeMode.media3Mode
                    PlayerView(it).apply {
                        useController = false
                        this.resizeMode = initialResizeMode
                        this.player = player
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = {
                    it.player = player
                    it.resizeMode = resizeMode.media3Mode
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isBuffering && !isCastingThisItem) {
            PlayerLoadingIndicator(
                text = "Buffering",
                modifier = Modifier.align(Alignment.Center),
            )
        }
        AnimatedVisibility(
            visible = controlsVisible && !isCastingThisItem,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(320)),
        ) {
            PlayerControls(
                item = item,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                positionMs = positionMs,
                durationMs = durationMs,
                selectedAudioIndex = selectedAudioIndex,
                selectedSubtitleIndex = selectedSubtitleIndex,
                onBack = onBack,
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                    onProgress(player.currentPosition, !player.isPlaying)
                },
                onSeekBy = {
                    player.seekTo((player.currentPosition + it).coerceIn(0L, durationMs.coerceAtLeast(player.currentPosition + it)))
                    onProgress(player.currentPosition, !player.isPlaying)
                },
                onSeekTo = {
                    player.seekTo(it)
                    onProgress(player.currentPosition, !player.isPlaying)
                },
                onAudio = { sheet = PlayerSheet.Audio },
                onSubtitles = { sheet = PlayerSheet.Subtitles },
                onMore = { sheet = PlayerSheet.More },
                onPlayPrevious = {
                    val candidate = previousCandidate ?: return@PlayerControls
                    if (!previousStarted) {
                        previousStarted = true
                        onPlayPrevious(candidate, player.currentPosition)
                    }
                },
                onPlayNext = {
                    val candidate = upNextCandidate ?: return@PlayerControls
                    if (!nextStarted) {
                        nextStarted = true
                        upNextState = UpNextState.PlayingNext
                        onPlayNext(candidate, player.currentPosition)
                    }
                },
            )
        }
        AnimatedVisibility(
            visible = activeSegment != null &&
                activeSegmentBehavior == JellyfinMediaSegmentBehavior.Prompt &&
                activeSegment.id.toString() !in autoSkippedSegmentIds,
            enter = fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing), initialOffsetY = { it / 5 }),
            exit = fadeOut(tween(260, easing = FastOutSlowInEasing)) +
                slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing), targetOffsetY = { it / 5 }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(end = 54.dp, start = 18.dp, bottom = if (controlsVisible) 166.dp else 54.dp),
        ) {
            val segment = activeSegment
            if (segment != null) {
                SkipSegmentOverlay(
                    label = segment.skipLabel(),
                    onSkip = { skipSegment(segment) },
                )
            }
        }
        AnimatedVisibility(
            visible = upNextState is UpNextState.Available,
            enter = fadeIn(tween(260)) + slideInVertically(animationSpec = tween(360, easing = FastOutSlowInEasing), initialOffsetY = { it / 3 }),
            exit = fadeOut(tween(260)) + slideOutVertically(animationSpec = tween(360, easing = FastOutSlowInEasing), targetOffsetY = { it / 3 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 560.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = if (controlsVisible) 162.dp else 24.dp),
        ) {
            val available = upNextState as? UpNextState.Available
            if (available != null) {
                UpNextOverlay(
                    state = available,
                    totalCountdownSeconds = autoplaySettings.countdownSeconds,
                    onPlayNow = {
                        if (!nextStarted) {
                            nextStarted = true
                            upNextState = UpNextState.PlayingNext
                            onPlayNext(available.candidate, player.currentPosition)
                        }
                    },
                    onCancel = {
                        upNextCancelled = true
                        upNextState = UpNextState.Cancelled(available.candidate)
                    },
                )
            }
        }
    }

    if (castSubtitleSheetVisible && isCastingThisItem) {
        CastSubtitleOptionsSheet(
            subtitleTracks = castState.subtitleTracks,
            activeSubtitleTrackId = castState.activeSubtitleTrackId,
            onDismiss = { castSubtitleSheetVisible = false },
            onSubtitle = {
                outputCoordinator.selectCastSubtitle(it?.castTrackId)
                selectedSubtitleIndex = it?.streamIndex
                onSelectSubtitleTrack(it?.streamIndex, castState.positionMs)
                castSubtitleSheetVisible = false
            },
        )
    }

    sheet?.let { current ->
        if (isCastingThisItem) return@let
        PlayerOptionsSheet(
            sheet = current,
            item = item,
            selectedAudioIndex = selectedAudioIndex,
            selectedSubtitleIndex = selectedSubtitleIndex,
            playbackSpeed = playbackSpeed,
            resizeMode = resizeMode,
            canTryTranscode = canTryTranscode,
            onDismiss = { sheet = null },
            onAudio = { track ->
                if (player.applyTrackSelection(C.TRACK_TYPE_AUDIO, track.index, item.audioTracks)) {
                    selectedAudioIndex = track.index
                    onSelectAudioTrack(track.index, player.currentPosition)
                }
                sheet = null
            },
            onSubtitle = { track ->
                val index = track?.index
                if (player.applyTrackSelection(C.TRACK_TYPE_TEXT, index, item.subtitleTracks)) {
                    selectedSubtitleIndex = index
                    onSelectSubtitleTrack(index, player.currentPosition)
                }
                sheet = null
            },
            onSpeed = { speed ->
                playbackSpeed = speed
                player.setPlaybackSpeed(speed)
                sheet = null
            },
            onResize = { mode ->
                resizeMode = mode
                sheet = null
            },
            onRetry = {
                sheet = null
                onRetry()
            },
            onTryTranscode = {
                sheet = null
                onTryTranscode()
            },
            onWatchFromBeginning = {
                player.seekTo(0L)
                onProgress(0L, !player.isPlaying)
                sheet = null
            },
            onStop = {
                sheet = null
                onBack()
            },
            onOpen = { sheet = it },
        )
    }
}

@Composable
private fun KeepScreenAwake(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val previous = view.keepScreenOn
        view.keepScreenOn = enabled
        onDispose {
            view.keepScreenOn = previous
        }
    }
}

@Composable
private fun PlayerImmersiveMode() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val window = activity?.window
        if (window == null) {
            onDispose {}
        } else {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val previousBehavior = controller.systemBarsBehavior
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = previousBehavior
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }
}

@Composable
private fun PlayerControls(
    item: VantafynPlaybackItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    selectedAudioIndex: Int?,
    selectedSubtitleIndex: Int?,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
    onMore: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.72f),
                        Color.Black.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.86f),
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerChevronBackButton(onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GoogleCastRouteButton(modifier = Modifier.size(44.dp))
                if (item.subtitleTracks.isNotEmpty()) {
                    PlayerIconButton(
                        icon = Icons.Rounded.ClosedCaption,
                        contentDescription = "Subtitles",
                        onClick = onSubtitles,
                        active = selectedSubtitleIndex != null,
                    )
                }
                if (item.audioTracks.size > 1) {
                    PlayerIconButton(
                        icon = Icons.Rounded.Audiotrack,
                        contentDescription = "Audio",
                        onClick = onAudio,
                        active = selectedAudioIndex != null,
                    )
                }
                PlayerIconButton(Icons.Rounded.MoreHoriz, "More", onMore)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    item.title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.subtitle?.let {
                    Text(it, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            VantafynPlayerProgressSlider(
                positionMs = positionMs,
                durationMs = durationMs,
                onSeekTo = onSeekTo,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(positionMs.formatMs(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(item.sourceLabel, if (isBuffering) "Buffering" else null).joinToString(" · "),
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("-${(durationMs - positionMs).coerceAtLeast(0L).formatMs()}", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.previousCandidate != null && !item.isLiveStream) {
                    PlayerIconButton(Icons.Rounded.SkipPrevious, "Previous episode", onPlayPrevious, size = 50.dp)
                }
                PlayerIconButton(Icons.Rounded.Replay10, "Back 10 seconds", { onSeekBy(-10_000L) }, size = 50.dp)
                PlayerPrimaryButton(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause" else "Play", onPlayPause)
                PlayerIconButton(Icons.Rounded.Forward10, "Forward 10 seconds", { onSeekBy(10_000L) }, size = 50.dp)
                if (item.upNextCandidate != null && !item.isLiveStream) {
                    PlayerIconButton(Icons.Rounded.SkipNext, "Next episode", onPlayNext, size = 50.dp)
                }
            }
        }
    }
}

@Composable
private fun PlayerChevronBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = 2.55.dp.toPx()
            drawLine(
                color = Color.White,
                start = Offset(12.5.dp.toPx(), 3.5.dp.toPx()),
                end = Offset(5.dp.toPx(), 10.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(5.dp.toPx(), 10.dp.toPx()),
                end = Offset(12.5.dp.toPx(), 16.5.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun VantafynPlayerProgressSlider(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val safePosition = positionMs.coerceIn(0L, safeDuration)
    val progress = (safePosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.17f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(VantafynGradients.accentHorizontal()),
            )
        }
        Slider(
            value = safePosition.toFloat(),
            onValueChange = { onSeekTo(it.toLong()) },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun CastControllerSurface(
    item: VantafynPlaybackItem,
    receiverName: String?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    errorMessage: String?,
    subtitleTracks: List<CastSubtitleTrack>,
    activeSubtitleTrackId: Long?,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onStopCasting: () -> Unit,
    onSubtitles: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayHere: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05070D),
                        Color(0xFF101421),
                        Color.Black,
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(22.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(VantafynGradients.accentHorizontal()),
                        contentAlignment = Alignment.Center,
                    ) {
                        GoogleCastRouteButton(modifier = Modifier.size(58.dp))
                    }
                    Text(
                        receiverName?.let { "Playing on $it" } ?: "Playing on Cast",
                        color = Color(0xFF6FE7FF),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            item.title,
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        item.subtitle?.let {
                            Text(it, color = VantafynColors.Muted, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    VantafynPlayerProgressSlider(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeekTo = onSeekTo,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(positionMs.formatMs(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
                        Text("-${(durationMs - positionMs).coerceAtLeast(0L).formatMs()}", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (subtitleTracks.isNotEmpty()) {
                            PlayerIconButton(
                                icon = Icons.Rounded.ClosedCaption,
                                contentDescription = "Cast subtitles",
                                onClick = onSubtitles,
                                active = activeSubtitleTrackId != null,
                                size = 48.dp,
                            )
                        }
                        if (item.previousCandidate != null && !item.isLiveStream) {
                            PlayerIconButton(Icons.Rounded.SkipPrevious, "Previous episode", onPlayPrevious, size = 48.dp)
                        }
                        PlayerIconButton(Icons.Rounded.Replay10, "Back 10 seconds", { onSeekBy(-10_000L) }, size = 48.dp)
                        PlayerPrimaryButton(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause Cast" else "Play Cast", onPlayPause)
                        PlayerIconButton(Icons.Rounded.Forward10, "Forward 10 seconds", { onSeekBy(10_000L) }, size = 48.dp)
                        if (item.upNextCandidate != null && !item.isLiveStream) {
                            PlayerIconButton(Icons.Rounded.SkipNext, "Next episode", onPlayNext, size = 48.dp)
                        }
                    }
                    errorMessage?.let {
                        Text(it, color = Color(0xFFFFC2C2), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(
                onClick = onStopCasting,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Text("Stop casting", color = VantafynColors.Ink)
            }
            VantafynButton("Play on this device", onClick = onPlayHere, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val background = if (active) {
        Brush.horizontalGradient(listOf(Color(0xFF37D7FF).copy(alpha = 0.28f), Color(0xFF8D5BFF).copy(alpha = 0.24f)))
    } else {
        Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.34f), Color.Black.copy(alpha = 0.20f)))
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
private fun PlayerPrimaryButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(VantafynGradients.accentHorizontal())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun SkipSegmentOverlay(
    label: String,
    onSkip: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF070A12).copy(alpha = 0.88f),
                        Color(0xFF111728).copy(alpha = 0.86f),
                        Color(0xFF090812).copy(alpha = 0.90f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.15.dp, durationMillis = 4200)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSkip,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
}

@Composable
private fun UpNextOverlay(
    state: UpNextState.Available,
    totalCountdownSeconds: Int,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
) {
    val candidate = state.candidate
    val progress = 1f - (state.countdownSeconds.toFloat() / totalCountdownSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val finishAtLabel = remember(candidate.itemId, candidate.runtimeMs, state.countdownSeconds) {
        candidate.runtimeMs
            ?.takeIf { it > 0L }
            ?.let { runtimeMs ->
                val finishMs = System.currentTimeMillis() + runtimeMs + state.countdownSeconds.coerceAtLeast(0) * 1_000L
                "Finishes at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(finishMs))}"
            }
    }
    VantafynGlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp)),
        ) {
            candidate.backdropUrl?.let { backdrop ->
                AsyncImage(
                    model = backdrop,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.92f),
                                    Color.Black.copy(alpha = 0.76f),
                                    Color.Black.copy(alpha = 0.90f),
                                ),
                            ),
                        ),
                )
            }
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 88.dp, height = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = candidate.imageUrl ?: candidate.backdropUrl,
                            contentDescription = candidate.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Up Next", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(candidate.seriesName ?: "Next episode", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${candidate.episodeLabel} · ${candidate.title}",
                            color = VantafynColors.Ink.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        finishAtLabel?.let { label ->
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.Black.copy(alpha = 0.36f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    label,
                                    color = VantafynColors.Ink.copy(alpha = 0.92f),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Text(
                        "Playing in ${state.countdownSeconds}",
                        color = VantafynColors.Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(VantafynGradients.accentHorizontal()),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VantafynButton("Play Now", onClick = onPlayNow, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text("Keep Watching", color = VantafynColors.Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerLoadingIndicator(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VantafynGradientSpinner(modifier = Modifier.size(30.dp), strokeWidth = 3.5.dp)
        Text(
            text = text,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PlaybackErrorOverlay(
    message: String,
    canTryTranscode: Boolean,
    onRetry: () -> Unit,
    onTryTranscode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VantafynGlassPanel(
        modifier = modifier.padding(24.dp),
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(22.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
        ) {
            Text("Playback problem", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(message, color = VantafynColors.Muted, textAlign = TextAlign.Center)
            VantafynButton("Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            if (canTryTranscode) {
                OutlinedButton(
                    onClick = onTryTranscode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VantafynColors.Ink),
                ) {
                    Text("Try Transcoding")
                }
            }
            TextButton(onClick = onClose) { Text("Close", color = VantafynColors.Ink) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerOptionsSheet(
    sheet: PlayerSheet,
    item: VantafynPlaybackItem,
    selectedAudioIndex: Int?,
    selectedSubtitleIndex: Int?,
    playbackSpeed: Float,
    resizeMode: PlayerResizeMode,
    canTryTranscode: Boolean,
    onDismiss: () -> Unit,
    onAudio: (VantafynAudioTrack) -> Unit,
    onSubtitle: (VantafynSubtitleTrack?) -> Unit,
    onSpeed: (Float) -> Unit,
    onResize: (PlayerResizeMode) -> Unit,
    onRetry: () -> Unit,
    onTryTranscode: () -> Unit,
    onWatchFromBeginning: () -> Unit,
    onStop: () -> Unit,
    onOpen: (PlayerSheet) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null,
        sheetGesturesEnabled = false,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            VantafynGlassModalPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.86f)
                    .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.5.dp),
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(sheet.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(sheet.subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
                        }
                        PlayerIconButton(Icons.Rounded.Close, "Close", onDismiss, size = 38.dp)
                    }
                    when (sheet) {
                        PlayerSheet.Audio -> {
                            if (item.audioTracks.isEmpty()) {
                                EmptyOption("No alternate audio tracks")
                            } else {
                                item.audioTracks.forEach { track ->
                                    TrackRow(
                                        title = track.label,
                                        detail = track.audioDetail(),
                                        selected = track.index == selectedAudioIndex,
                                        badges = buildList {
                                            if (track.isDefault) add("Default")
                                            track.channels?.channelLabel()?.let { add(it) }
                                        },
                                    ) { onAudio(track) }
                                }
                            }
                        }
                        PlayerSheet.Subtitles -> {
                            TrackRow("Off", "Disable subtitles", selected = selectedSubtitleIndex == null) { onSubtitle(null) }
                            if (item.subtitleTracks.isEmpty()) {
                                EmptyOption("No subtitle tracks available")
                            } else {
                                item.subtitleTracks.forEach { track ->
                                    TrackRow(
                                        title = track.label,
                                        detail = track.subtitleDetail(),
                                        selected = track.index == selectedSubtitleIndex,
                                        badges = buildList {
                                            if (track.isDefault) add("Default")
                                            if (track.isExternal) add("External")
                                            if (track.deliveryUrl == null && track.isExternal) add("Unavailable")
                                        },
                                        enabled = !track.isExternal || track.deliveryUrl != null,
                                    ) { onSubtitle(track) }
                                }
                            }
                        }
                        PlayerSheet.More -> {
                            OptionRow(Icons.Rounded.ClosedCaption, "Subtitles", item.selectedSubtitleLabel(selectedSubtitleIndex)) { onOpen(PlayerSheet.Subtitles) }
                            if (item.audioTracks.size > 1) {
                                OptionRow(Icons.Rounded.Audiotrack, "Audio", item.selectedAudioLabel(selectedAudioIndex)) { onOpen(PlayerSheet.Audio) }
                            }
                            OptionRow(Icons.Rounded.Speed, "Playback speed", "${playbackSpeed.cleanSpeed()}x") { onOpen(PlayerSheet.Speed) }
                            OptionRow(Icons.Rounded.Settings, "Screen fit", resizeMode.label) { onOpen(PlayerSheet.Resize) }
                            if (item.fallbackStreamUrl != null || canTryTranscode) {
                                OptionRow(Icons.Rounded.RestartAlt, "Try transcoding", "Preserves your current position where possible", onTryTranscode)
                            }
                            OptionRow(Icons.Rounded.Replay10, "Watch from beginning", "Start this title over", onWatchFromBeginning)
                            OptionRow(Icons.Rounded.Stop, "Stop playback", "Return to Vantafyn", onStop)
                            OptionRow(Icons.Rounded.Settings, "Retry playback", "Reload this playback source", onRetry)
                        }
                        PlayerSheet.Speed -> {
                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                TrackRow(
                                    title = "${speed.cleanSpeed()}x",
                                    detail = if (speed == 1f) "Normal speed" else "Playback speed",
                                    selected = speed == playbackSpeed,
                                ) { onSpeed(speed) }
                            }
                        }
                        PlayerSheet.Resize -> {
                            PlayerResizeMode.entries.forEach { mode ->
                                TrackRow(
                                    title = mode.label,
                                    detail = mode.description,
                                    selected = mode == resizeMode,
                                ) { onResize(mode) }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastSubtitleOptionsSheet(
    subtitleTracks: List<CastSubtitleTrack>,
    activeSubtitleTrackId: Long?,
    onDismiss: () -> Unit,
    onSubtitle: (CastSubtitleTrack?) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            VantafynGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.86f),
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Cast subtitles", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("Subtitles available on your Cast device.", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
                        }
                        PlayerIconButton(Icons.Rounded.Close, "Close", onDismiss, size = 38.dp)
                    }
                    TrackRow("Off", "Disable subtitles on Cast", selected = activeSubtitleTrackId == null) { onSubtitle(null) }
                    if (subtitleTracks.isEmpty()) {
                        EmptyOption("No Cast subtitles available")
                    } else {
                        subtitleTracks.forEach { track ->
                            TrackRow(
                                title = track.label,
                                detail = track.castSubtitleDetail(),
                                selected = track.castTrackId == activeSubtitleTrackId,
                                badges = buildList {
                                    if (track.isDefault) add("Default")
                                    if (track.isExternal) add("External")
                                },
                            ) { onSubtitle(track) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    title: String,
    detail: String,
    selected: Boolean = false,
    badges: List<String> = emptyList(),
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                color = if (enabled) VantafynColors.Ink else VantafynColors.Muted,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotBlank()) {
                Text(detail, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (badges.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    badges.forEach { BadgeText(it) }
                }
            }
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF6FE7FF), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun OptionRow(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = VantafynColors.Ink.copy(alpha = 0.92f), modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(detail, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BadgeText(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(text, color = VantafynColors.Ink.copy(alpha = 0.80f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyOption(text: String) {
    VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
        Text(text, color = VantafynColors.Muted)
    }
}

private enum class PlayerSheet(val title: String, val subtitle: String) {
    Audio("Audio", "Choose the stream for this playback."),
    Subtitles("Subtitles", "Embedded and external Jellyfin subtitle tracks."),
    More("Player Options", "Source, speed, retry, and stop actions."),
    Speed("Playback Speed", "Adjust video speed for this session."),
    Resize("Screen Fit", "Choose how video fills the display."),
}

private enum class PlayerResizeMode(
    val label: String,
    val description: String,
    val media3Mode: Int,
) {
    Fit("Fit", "Show the whole image without cropping.", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    Fill("Fill", "Fill the screen while preserving shape.", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    Zoom("Zoom", "Crop edges for a cinematic full-screen view.", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FixedWidth("Fixed Width", "Fit width and allow height to scale.", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FixedHeight("Fixed Height", "Fit height and allow width to scale.", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT),
}

private fun VantafynPlaybackItem.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(itemId)
        .setUri(streamUrl)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .setSubtitleConfigurations(
            subtitleTracks
                .filter { it.isExternal && !it.deliveryUrl.isNullOrBlank() }
                .map { track ->
                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(track.deliveryUrl))
                        .setLabel(track.label)
                        .setLanguage(track.language)
                        .setMimeType(track.subtitleMimeType())
                        .build()
                },
        )
        .build()

private fun Player.applyTrackSelection(trackType: @C.TrackType Int, streamIndex: Int?, knownTracks: List<VantafynTrackInfo>): Boolean {
    if (trackType == C.TRACK_TYPE_TEXT && streamIndex == null) {
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(trackType)
            .setTrackTypeDisabled(trackType, true)
            .build()
        return true
    }
    val selectedTrack = knownTracks.firstOrNull { it.index == streamIndex } ?: return false
    val supportedGroups = currentTracks.groups.filter { it.type == trackType && it.isSupported }
    val group = supportedGroups.bestMatchFor(selectedTrack, knownTracks.indexOf(selectedTrack)) ?: return false
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
        .setTrackTypeDisabled(trackType, false)
        .build()
    return true
}

private fun List<Tracks.Group>.bestMatchFor(track: VantafynTrackInfo, fallbackIndex: Int): Tracks.Group? =
    firstOrNull { group ->
        val format = group.mediaTrackGroup.getFormat(0)
        val codec = track.codec
        format.label.equals(track.label, ignoreCase = true) ||
            (track.language != null && format.language.equals(track.language, ignoreCase = true) && (codec == null || format.codecs.orEmpty().contains(codec, ignoreCase = true)))
    } ?: getOrNull(fallbackIndex.coerceAtLeast(0))

private fun VantafynSubtitleTrack.subtitleMimeType(): String =
    when (codec?.lowercase()) {
        "subrip", "srt" -> MimeTypes.APPLICATION_SUBRIP
        "webvtt", "vtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> MimeTypes.TEXT_UNKNOWN
    }

private fun List<JellyfinMediaSegment>.activeAt(positionMs: Long, durationMs: Long): JellyfinMediaSegment? =
    firstOrNull { segment ->
        segment.isUsableFor(durationMs) && positionMs >= segment.startMs && positionMs < segment.endMs
    }

private fun JellyfinMediaSegment.isUsableFor(durationMs: Long): Boolean {
    val segmentDurationMs = endMs - startMs
    if (segmentDurationMs <= 0L) return false
    if (durationMs > 0L && startMs >= durationMs) return false
    if (durationMs > 0L && type != JellyfinMediaSegmentType.Outro && endMs >= durationMs - 10_000L) return false
    if (durationMs > 0L && type != JellyfinMediaSegmentType.Outro && segmentDurationMs > durationMs * 0.35f) return false
    return when (type) {
        JellyfinMediaSegmentType.Intro,
        JellyfinMediaSegmentType.Recap,
        JellyfinMediaSegmentType.Preview,
        JellyfinMediaSegmentType.Commercial -> segmentDurationMs <= 15 * 60_000L
        JellyfinMediaSegmentType.Outro -> true
        JellyfinMediaSegmentType.Unknown -> false
    }
}

private fun JellyfinMediaSegment.skipLabel(): String =
    when (type) {
        JellyfinMediaSegmentType.Intro -> "Skip Intro"
        JellyfinMediaSegmentType.Recap -> "Skip Recap"
        JellyfinMediaSegmentType.Outro -> "Skip Credits"
        JellyfinMediaSegmentType.Commercial -> "Skip Commercial"
        JellyfinMediaSegmentType.Preview -> "Skip Preview"
        JellyfinMediaSegmentType.Unknown -> "Skip Segment"
    }

private fun Long.formatMs(): String {
    val totalSeconds = coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun VantafynPlaybackItem.selectedAudioLabel(index: Int?): String =
    audioTracks.firstOrNull { it.index == index }?.label ?: "Server default"

private fun VantafynPlaybackItem.selectedSubtitleLabel(index: Int?): String =
    subtitleTracks.firstOrNull { it.index == index }?.label ?: "Off"

private fun VantafynAudioTrack.audioDetail(): String =
    listOfNotNull(language?.uppercase(), codec?.uppercase(), channels?.channelLabel()).joinToString(" · ")

private fun VantafynSubtitleTrack.subtitleDetail(): String =
    listOfNotNull(language?.uppercase(), codec?.uppercase(), if (isExternal) "External" else "Embedded").joinToString(" · ")

private fun CastSubtitleTrack.castSubtitleDetail(): String =
    listOfNotNull(language?.uppercase(), codec?.uppercase(), contentType).joinToString(" · ")

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Int.channelLabel(): String? =
    when (this) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "$this ch"
    }

private fun Float.cleanSpeed(): String =
    if (this % 1f == 0f) toInt().toString() else toString().trimEnd('0').trimEnd('.')

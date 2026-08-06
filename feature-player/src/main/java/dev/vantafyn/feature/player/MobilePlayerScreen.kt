package dev.vantafyn.feature.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import dev.vantafyn.core.media.UpNextCandidate
import dev.vantafyn.core.media.UpNextState
import dev.vantafyn.core.media.VantafynAudioTrack
import dev.vantafyn.core.media.VantafynPlaybackItem
import dev.vantafyn.core.media.VantafynSubtitleTrack
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassPanel
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynSpacing
import kotlinx.coroutines.delay

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
    onPlayerError: () -> Unit,
    onSelectAudioTrack: (Int, Long) -> Unit,
    onSelectSubtitleTrack: (Int?, Long) -> Unit,
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
                onPosition = { lastPositionMs = it },
                onBack = { onBack(lastPositionMs) },
                onStarted = onStarted,
                onProgress = onProgress,
                onEnded = onEnded,
                onPlayNext = onPlayNext,
                onPlayerError = onPlayerError,
                onSelectAudioTrack = onSelectAudioTrack,
                onSelectSubtitleTrack = onSelectSubtitleTrack,
            )
            isLoading -> VantafynLoadingIndicator(
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
    onPosition: (Long) -> Unit,
    onBack: () -> Unit,
    onStarted: (Long) -> Unit,
    onProgress: (Long, Boolean) -> Unit,
    onEnded: (Long) -> Unit,
    onPlayNext: (UpNextCandidate, Long) -> Unit,
    onPlayerError: () -> Unit,
    onSelectAudioTrack: (Int, Long) -> Unit,
    onSelectSubtitleTrack: (Int?, Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var controlsVisible by remember(item.streamUrl) { mutableStateOf(true) }
    var isPlaying by remember(item.streamUrl) { mutableStateOf(false) }
    var durationMs by remember(item.streamUrl) { mutableLongStateOf(item.durationMs ?: 0L) }
    var positionMs by remember(item.streamUrl) { mutableLongStateOf(item.startPositionMs) }
    var started by remember(item.streamUrl) { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<TrackSheet?>(null) }
    var upNextState by remember(item.itemId) { mutableStateOf<UpNextState>(UpNextState.Hidden) }
    var upNextCancelled by remember(item.itemId) { mutableStateOf(false) }
    var nextStarted by remember(item.itemId) { mutableStateOf(false) }
    val upNextCandidate = item.upNextCandidate
    val autoplaySettings = item.autoplaySettings
    val passoutProtectionLimitReached = autoplaySettings.passoutProtectionEnabled &&
        (System.currentTimeMillis() - item.continuousPlaybackStartedAtMs).coerceAtLeast(0L) >=
        autoplaySettings.passoutProtectionLimitMinutes * 60_000L
    val canUseUpNext = upNextCandidate != null &&
        autoplaySettings.enabled &&
        !passoutProtectionLimitReached &&
        autoplaySettings.onlyForEpisodes &&
        item.itemType.equals("Episode", ignoreCase = true) &&
        !item.isLiveStream
    val player = remember(item.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            setMediaItem(MediaItem.fromUri(item.streamUrl))
            prepare()
            if (item.startPositionMs > 0L) seekTo(item.startPositionMs)
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        durationMs = player.duration.takeIf { it > 0 } ?: item.durationMs ?: durationMs
                        if (!started) {
                            started = true
                            onStarted(player.currentPosition)
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
                            nextStarted = true
                            upNextState = UpNextState.PlayingNext
                            onPlayNext(candidate, player.currentPosition)
                        } else {
                            onEnded(player.currentPosition)
                        }
                    }
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
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

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0 } ?: durationMs
            onPosition(positionMs)
            delay(500L)
        }
    }

    LaunchedEffect(player, started) {
        while (true) {
            delay(7_000L)
            if (started) onProgress(player.currentPosition, !player.isPlaying)
        }
    }

    LaunchedEffect(positionMs, durationMs, canUseUpNext, upNextCancelled, nextStarted) {
        val candidate = upNextCandidate ?: return@LaunchedEffect
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
        if (!available.autoplayEnabled || !isPlaying || nextStarted) return@LaunchedEffect
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
            .clickable { controlsVisible = !controlsVisible },
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    this.player = player
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(controlsVisible, modifier = Modifier.fillMaxSize()) {
            PlayerControls(
                item = item,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                onBack = onBack,
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                    onProgress(player.currentPosition, !player.isPlaying)
                },
                onSeekBy = {
                    player.seekTo((player.currentPosition + it).coerceAtLeast(0L))
                    onProgress(player.currentPosition, !player.isPlaying)
                },
                onSeekTo = {
                    player.seekTo(it)
                    onProgress(player.currentPosition, !player.isPlaying)
                },
                onAudio = { sheet = TrackSheet.Audio },
                onSubtitles = { sheet = TrackSheet.Subtitles },
            )
        }
        AnimatedVisibility(
            visible = upNextState is UpNextState.Available,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = if (controlsVisible) 154.dp else 24.dp),
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

    sheet?.let { current ->
        TrackSelectionSheet(
            sheet = current,
            audioTracks = item.audioTracks,
            subtitleTracks = item.subtitleTracks,
            selectedAudioIndex = item.selectedAudioStreamIndex,
            selectedSubtitleIndex = item.selectedSubtitleStreamIndex,
            onDismiss = { sheet = null },
            onAudio = {
                sheet = null
                onSelectAudioTrack(it, player.currentPosition)
            },
            onSubtitle = {
                sheet = null
                onSelectSubtitleTrack(it, player.currentPosition)
            },
        )
    }
}

@Composable
private fun PlayerControls(
    item: VantafynPlaybackItem,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.68f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.82f),
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(12.dp)) {
            Text("‹", color = VantafynColors.Ink)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            Text(item.title, color = VantafynColors.Ink)
            item.subtitle?.let { Text(it, color = VantafynColors.Muted) }
            Slider(
                value = positionMs.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${positionMs.formatMs()} / ${durationMs.formatMs()}", color = VantafynColors.Muted)
                item.sourceLabel?.let { Text(it, color = VantafynColors.Muted) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                Button(onClick = { onSeekBy(-10_000L) }) { Text("-10") }
                VantafynButton(if (isPlaying) "Pause" else "Play", onClick = onPlayPause)
                Button(onClick = { onSeekBy(30_000L) }) { Text("+30") }
                if (item.audioTracks.size > 1) Button(onClick = onAudio) { Text("Audio") }
                if (item.subtitleTracks.isNotEmpty()) Button(onClick = onSubtitles) { Text("Subs") }
            }
        }
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
private fun PlaybackErrorOverlay(
    message: String,
    canTryTranscode: Boolean,
    onRetry: () -> Unit,
    onTryTranscode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.86f))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
    ) {
        Text("Playback problem", color = VantafynColors.Ink)
        Text(message, color = VantafynColors.Muted)
        VantafynButton("Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
        if (canTryTranscode) {
            Button(onClick = onTryTranscode, modifier = Modifier.fillMaxWidth()) { Text("Try Transcoding") }
        }
        TextButton(onClick = onClose) { Text("Close") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackSelectionSheet(
    sheet: TrackSheet,
    audioTracks: List<VantafynAudioTrack>,
    subtitleTracks: List<VantafynSubtitleTrack>,
    selectedAudioIndex: Int?,
    selectedSubtitleIndex: Int?,
    onDismiss: () -> Unit,
    onAudio: (Int) -> Unit,
    onSubtitle: (Int?) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = 18.dp, vertical = 12.dp)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (sheet == TrackSheet.Audio) "Audio" else "Subtitles")
            if (sheet == TrackSheet.Subtitles) {
                TrackRow("Off", "Disable subtitles", selected = selectedSubtitleIndex == null) { onSubtitle(null) }
            }
            when (sheet) {
                TrackSheet.Audio -> audioTracks.forEach { track ->
                    TrackRow(track.label, track.detail(), selected = track.index == selectedAudioIndex) { onAudio(track.index) }
                }
                TrackSheet.Subtitles -> subtitleTracks.forEach { track ->
                    TrackRow(track.label, track.detail(), selected = track.index == selectedSubtitleIndex) { onSubtitle(track.index) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TrackRow(title: String, detail: String, selected: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(if (selected) "$title  ✓" else title)
        if (detail.isNotBlank()) Text(detail)
    }
}

private enum class TrackSheet {
    Audio,
    Subtitles,
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

private fun VantafynAudioTrack.detail(): String =
    listOfNotNull(language?.uppercase(), codec?.uppercase(), channels?.let { "$it ch" }).joinToString(" · ")

private fun VantafynSubtitleTrack.detail(): String =
    listOfNotNull(language?.uppercase(), codec?.uppercase(), if (isExternal) "External" else null).joinToString(" · ")

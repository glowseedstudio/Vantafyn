package dev.vantafyn.core.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class VantafynMusicTrack(
    val id: UUID,
    val title: String,
    val artist: String,
    val album: String?,
    val albumId: UUID?,
    val durationMs: Long?,
    val genres: List<String> = emptyList(),
    val streamUrl: String,
    val artworkUrl: String?,
    val isFavorite: Boolean = false,
)

enum class VantafynMusicStopReason {
    User,
    QueueChange,
    Skip,
    Ended,
    Logout,
    ProfileSwitch,
    VideoPlayback,
    Background,
    Error,
}

enum class VantafynMusicRepeatMode {
    Off,
    One,
    All,
}

enum class SleepTimerMode {
    Duration,
    EndOfTrack,
    EndOfQueue,
}

data class VantafynMusicPlaybackState(
    val queue: List<VantafynMusicTrack> = emptyList(),
    val queueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: VantafynMusicRepeatMode = VantafynMusicRepeatMode.Off,
    val errorMessage: String? = null,
    val sleepTimerRemainingSeconds: Long? = null,
    val sleepTimerMode: SleepTimerMode? = null,
) {
    val currentTrack: VantafynMusicTrack?
        get() = queue.getOrNull(queueIndex)
}

sealed interface VantafynMusicPlaybackEvent {
    data class TrackStarted(val track: VantafynMusicTrack, val positionMs: Long) : VantafynMusicPlaybackEvent
    data class TrackChanged(
        val previousTrack: VantafynMusicTrack?,
        val previousPositionMs: Long,
        val currentTrack: VantafynMusicTrack?,
        val reason: VantafynMusicStopReason,
    ) : VantafynMusicPlaybackEvent
    data class PauseChanged(val track: VantafynMusicTrack, val positionMs: Long, val isPaused: Boolean) : VantafynMusicPlaybackEvent
    data class Seeked(val track: VantafynMusicTrack, val positionMs: Long) : VantafynMusicPlaybackEvent
    data class Stopped(val track: VantafynMusicTrack?, val positionMs: Long, val reason: VantafynMusicStopReason) : VantafynMusicPlaybackEvent
}

class MusicPlaybackController private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastTransitionReason: VantafynMusicStopReason = VantafynMusicStopReason.QueueChange
    private var playbackServiceStarted = false
    private var lastRegistryTickMs: Long = 0L
    private val tracksByMediaId = mutableMapOf<String, VantafynMusicTrack>()

    internal val sessionPlayer: ExoPlayer = VantafynExoPlayerFactory.musicBuilder(context.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        setHandleAudioBecomingNoisy(true)
        setWakeMode(C.WAKE_MODE_NETWORK)
        enableCompatibleAudioOffload()
        addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d(TAG, "isPlaying changed: $isPlaying (track=${_state.value.currentTrack?.title?.take(20)})")
                    _state.update { it.copy(isPlaying = isPlaying, errorMessage = null) }
                    _state.value.currentTrack?.let { track ->
                        emitEvent(VantafynMusicPlaybackEvent.PauseChanged(track, currentPosition.coerceAtLeast(0L), !isPlaying))
                        if (isPlaying) emitEvent(VantafynMusicPlaybackEvent.TrackStarted(track, currentPosition.coerceAtLeast(0L)))
                    }
                    syncTicker()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val previous = _state.value.currentTrack
                    val previousPosition = _state.value.positionMs
                    val currentIndex = currentMediaItemIndex.takeIf { it >= 0 } ?: _state.value.queueIndex
                    val transitionStopReason = when (reason) {
                        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> VantafynMusicStopReason.Ended
                        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> lastTransitionReason
                        else -> lastTransitionReason
                    }
                    _state.update { state ->
                        state.copy(
                            queueIndex = currentIndex,
                            durationMs = duration.takeIf { it > 0 } ?: state.durationMs,
                            positionMs = currentPosition.coerceAtLeast(0L),
                        )
                    }
                    val current = _state.value.currentTrack
                    if (previous?.id != current?.id) {
                        emitEvent(
                            VantafynMusicPlaybackEvent.TrackChanged(
                                previousTrack = previous,
                                previousPositionMs = previousPosition,
                                currentTrack = current,
                                reason = transitionStopReason,
                            ),
                        )
                        current?.let { emitEvent(VantafynMusicPlaybackEvent.TrackStarted(it, currentPosition.coerceAtLeast(0L))) }
                    }
                    lastTransitionReason = VantafynMusicStopReason.QueueChange
                    if (_state.value.sleepTimerMode == SleepTimerMode.EndOfTrack) {
                        pause()
                        cancelSleepTimer()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _state.update { state ->
                        state.copy(
                            durationMs = duration.takeIf { it > 0 } ?: state.durationMs,
                            positionMs = currentPosition.coerceAtLeast(0L),
                        )
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        emitEvent(
                            VantafynMusicPlaybackEvent.Stopped(
                                track = _state.value.currentTrack,
                                positionMs = currentPosition.coerceAtLeast(0L),
                                reason = VantafynMusicStopReason.Ended,
                            ),
                        )
                        _state.update { it.copy(isPlaying = false) }
                        if (_state.value.sleepTimerMode == SleepTimerMode.EndOfQueue) {
                            cancelSleepTimer()
                        }
                    }
                    if (playbackState == Player.STATE_READY) {
                        forcePlaybackSnapshot()
                        syncTicker()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val failedTrack = _state.value.currentTrack
                    val failedPosition = currentPosition.coerceAtLeast(0L)
                    if (sessionPlayer.hasNextMediaItem()) {
                        emitEvent(VantafynMusicPlaybackEvent.Stopped(failedTrack, failedPosition, VantafynMusicStopReason.Error))
                        lastTransitionReason = VantafynMusicStopReason.Error
                        sessionPlayer.seekToNextMediaItem()
                        sessionPlayer.prepare()
                        sessionPlayer.playWhenReady = true
                        return
                    }
                    emitEvent(VantafynMusicPlaybackEvent.Stopped(failedTrack, failedPosition, VantafynMusicStopReason.Error))
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            errorMessage = "Music playback failed on this device.",
                        )
                    }
                }
            },
        )
    }

    private val _state = MutableStateFlow(VantafynMusicPlaybackState())
    val state: StateFlow<VantafynMusicPlaybackState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<VantafynMusicPlaybackEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<VantafynMusicPlaybackEvent> = _events.asSharedFlow()

    init {
        scope.launch {
            AppForegroundStateRepository.isForeground.collect { isForeground ->
                if (isForeground) {
                    forcePlaybackSnapshot()
                    if (sessionPlayer.isPlaying) {
                        tickerJob?.cancel()
                        tickerJob = null
                        syncTicker()
                    }
                } else {
                    tickerJob?.cancel()
                    tickerJob = null
                    LongRunningTaskRegistry.stop(MUSIC_TICKER_TASK_ID, "background idle")
                }
            }
        }
    }

    fun playQueue(queue: List<VantafynMusicTrack>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
        val previous = _state.value.currentTrack
        val previousPosition = sessionPlayer.currentPosition.coerceAtLeast(0L)
        if (previous != null && previous.id != queue[safeIndex].id) {
            emitEvent(VantafynMusicPlaybackEvent.Stopped(previous, previousPosition, VantafynMusicStopReason.QueueChange))
        }
        lastTransitionReason = VantafynMusicStopReason.QueueChange
        _state.update {
            it.copy(
                queue = queue,
                queueIndex = safeIndex,
                positionMs = 0L,
                durationMs = queue[safeIndex].durationMs ?: 0L,
                errorMessage = null,
            )
        }
        val mediaItems = queue.map { it.toMediaItem() }
        tracksByMediaId.clear()
        queue.forEach { track -> tracksByMediaId[track.id.toString()] = track }
        sessionPlayer.setMediaItems(mediaItems, safeIndex, 0L)
        sessionPlayer.prepare()
        ensurePlaybackService()
        sessionPlayer.playWhenReady = true
        emitEvent(VantafynMusicPlaybackEvent.TrackStarted(queue[safeIndex], 0L))
    }

    fun togglePlayPause() {
        if (sessionPlayer.isPlaying) {
            sessionPlayer.pause()
        } else {
            ensurePlaybackService()
            if (sessionPlayer.mediaItemCount > 0 &&
                (sessionPlayer.playbackState == Player.STATE_ENDED || sessionPlayer.playbackState == Player.STATE_IDLE)
            ) {
                val index = sessionPlayer.currentMediaItemIndex.takeIf { it >= 0 } ?: _state.value.queueIndex
                sessionPlayer.seekTo(index.coerceAtLeast(0), sessionPlayer.currentPosition.coerceAtLeast(0L))
                sessionPlayer.prepare()
            }
            sessionPlayer.play()
        }
    }

    fun pause() {
        if (sessionPlayer.isPlaying) sessionPlayer.pause()
    }

    fun stop(clearQueue: Boolean = false, reason: VantafynMusicStopReason = VantafynMusicStopReason.User) {
        val track = _state.value.currentTrack
        val position = sessionPlayer.currentPosition.coerceAtLeast(0L)
        emitEvent(VantafynMusicPlaybackEvent.Stopped(track, position, reason))
        sessionPlayer.playWhenReady = false
        sessionPlayer.stop()
        if (clearQueue) {
            sessionPlayer.clearMediaItems()
        }
        stopPlaybackService()
        _state.update {
            it.copy(
                queue = if (clearQueue) emptyList() else it.queue,
                queueIndex = if (clearQueue) 0 else it.queueIndex,
                isPlaying = false,
                positionMs = 0L,
                durationMs = if (clearQueue) 0L else it.durationMs,
            )
        }
    }

    fun suspendLocalPlaybackForCast(positionMs: Long) {
        val track = _state.value.currentTrack
        val safePosition = positionMs.coerceAtLeast(0L)
        emitEvent(VantafynMusicPlaybackEvent.Stopped(track, safePosition, VantafynMusicStopReason.Background))
        sessionPlayer.playWhenReady = false
        sessionPlayer.pause()
        sessionPlayer.stop()
        stopPlaybackService()
        _state.update {
            it.copy(
                isPlaying = false,
                positionMs = safePosition,
                durationMs = track?.durationMs ?: it.durationMs,
                errorMessage = null,
            )
        }
        syncTicker()
    }

    fun next() {
        ensurePlaybackService()
        if (sessionPlayer.hasNextMediaItem()) {
            lastTransitionReason = VantafynMusicStopReason.Skip
            sessionPlayer.seekToNextMediaItem()
            sessionPlayer.play()
            forcePlaybackSnapshot()
            syncTicker()
        } else if (_state.value.repeatMode == VantafynMusicRepeatMode.All && _state.value.queue.isNotEmpty()) {
            lastTransitionReason = VantafynMusicStopReason.Skip
            sessionPlayer.seekTo(0, 0L)
            sessionPlayer.play()
            forcePlaybackSnapshot()
            syncTicker()
        }
    }

    fun playQueueIndex(index: Int) {
        val safeIndex = index.takeIf { it in _state.value.queue.indices } ?: return
        ensurePlaybackService()
        lastTransitionReason = VantafynMusicStopReason.Skip
        sessionPlayer.seekTo(safeIndex, 0L)
        if (sessionPlayer.playbackState == Player.STATE_IDLE) sessionPlayer.prepare()
        sessionPlayer.play()
    }

    fun updateFavorite(trackId: UUID, isFavorite: Boolean) {
        _state.update { state ->
            state.copy(queue = state.queue.map { if (it.id == trackId) it.copy(isFavorite = isFavorite) else it })
        }
    }

    fun playNext(track: VantafynMusicTrack) {
        val currentQueue = _state.value.queue
        val existingIndex = currentQueue.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            sessionPlayer.removeMediaItem(existingIndex)
        }
        val currentIdx = sessionPlayer.currentMediaItemIndex.coerceAtLeast(0)
        val insertIdx = (currentIdx + 1).coerceAtMost(sessionPlayer.mediaItemCount)
        sessionPlayer.addMediaItem(insertIdx, track.toMediaItem())
        tracksByMediaId[track.id.toString()] = track
        val newQueue = currentQueue.toMutableList()
        if (existingIndex >= 0) newQueue.removeAt(existingIndex)
        newQueue.add(insertIdx, track)
        _state.update { it.copy(queue = newQueue, queueIndex = sessionPlayer.currentMediaItemIndex) }
    }

    fun addToQueue(track: VantafynMusicTrack) {
        sessionPlayer.addMediaItem(track.toMediaItem())
        tracksByMediaId[track.id.toString()] = track
        _state.update { it.copy(queue = it.queue + track) }
    }

    fun removeFromQueue(index: Int) {
        if (index !in 0 until sessionPlayer.mediaItemCount) return
        sessionPlayer.removeMediaItem(index)
        _state.update { state ->
            val newQueue = state.queue.toMutableList().also { if (index in it.indices) it.removeAt(index) }
            val newIndex = sessionPlayer.currentMediaItemIndex.coerceIn(0, newQueue.size.coerceAtLeast(1) - 1)
            state.copy(queue = newQueue, queueIndex = newIndex)
        }
    }

    fun clearUpcomingQueue() {
        val currentIdx = sessionPlayer.currentMediaItemIndex
        val queue = _state.value.queue
        if (currentIdx < 0 || currentIdx >= queue.size) return
        val totalCount = sessionPlayer.mediaItemCount
        if (totalCount > currentIdx + 1) {
            sessionPlayer.removeMediaItems(currentIdx + 1, totalCount)
        }
        _state.update { it.copy(queue = queue.take(currentIdx + 1)) }
    }

    fun clearAllQueue() {
        stop(clearQueue = true)
    }

    fun addMultipleToQueue(tracks: List<VantafynMusicTrack>) {
        if (tracks.isEmpty()) return
        val mediaItems = tracks.map { track ->
            tracksByMediaId[track.id.toString()] = track
            track.toMediaItem()
        }
        sessionPlayer.addMediaItems(mediaItems)
        _state.update { it.copy(queue = it.queue + tracks) }
    }

    fun playNextMultiple(tracks: List<VantafynMusicTrack>) {
        if (tracks.isEmpty()) return
        val currentQueue = _state.value.queue.toMutableList()
        val currentIdx = sessionPlayer.currentMediaItemIndex.coerceAtLeast(0)
        val insertIdx = (currentIdx + 1).coerceAtMost(sessionPlayer.mediaItemCount)
        val mediaItems = tracks.map { track ->
            tracksByMediaId[track.id.toString()] = track
            track.toMediaItem()
        }
        sessionPlayer.addMediaItems(insertIdx, mediaItems)
        currentQueue.addAll(insertIdx, tracks)
        _state.update { it.copy(queue = currentQueue, queueIndex = sessionPlayer.currentMediaItemIndex) }
    }

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        val totalSeconds = minutes * 60L
        _state.update {
            it.copy(
                sleepTimerRemainingSeconds = totalSeconds,
                sleepTimerMode = SleepTimerMode.Duration,
            )
        }
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _state.update { it.copy(sleepTimerRemainingSeconds = remaining) }
                if (remaining == 3L) {
                    fadeVolume(from = 1.0f, to = 0.0f, durationMs = 3000)
                }
            }
            pause()
            sessionPlayer.volume = 1.0f
            _state.update {
                it.copy(
                    sleepTimerRemainingSeconds = null,
                    sleepTimerMode = null,
                )
            }
        }
    }

    fun setSleepTimerEndOfTrack() {
        cancelSleepTimer()
        _state.update {
            it.copy(
                sleepTimerRemainingSeconds = null,
                sleepTimerMode = SleepTimerMode.EndOfTrack,
            )
        }
    }

    fun setSleepTimerEndOfQueue() {
        cancelSleepTimer()
        _state.update {
            it.copy(
                sleepTimerRemainingSeconds = null,
                sleepTimerMode = SleepTimerMode.EndOfQueue,
            )
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sessionPlayer.volume = 1.0f
        _state.update {
            it.copy(
                sleepTimerRemainingSeconds = null,
                sleepTimerMode = null,
            )
        }
    }

    private suspend fun fadeVolume(from: Float, to: Float, durationMs: Long) {
        val steps = 30
        val stepInterval = (durationMs / steps).coerceAtLeast(10L)
        val delta = (to - from) / steps
        var currentVol = from
        for (i in 0 until steps) {
            currentVol += delta
            sessionPlayer.volume = currentVol.coerceIn(0.0f, 1.0f)
            delay(stepInterval)
        }
        sessionPlayer.volume = to.coerceIn(0.0f, 1.0f)
    }

    fun previous() {
        ensurePlaybackService()
        if (sessionPlayer.currentPosition > 3_000L || !sessionPlayer.hasPreviousMediaItem()) {
            sessionPlayer.seekTo(0L)
            _state.value.currentTrack?.let { emitEvent(VantafynMusicPlaybackEvent.Seeked(it, 0L)) }
        } else {
            lastTransitionReason = VantafynMusicStopReason.Skip
            sessionPlayer.seekToPreviousMediaItem()
        }
        sessionPlayer.play()
        forcePlaybackSnapshot()
        syncTicker()
    }

    fun seekTo(positionMs: Long) {
        sessionPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = sessionPlayer.currentPosition.coerceAtLeast(0L)) }
        _state.value.currentTrack?.let { emitEvent(VantafynMusicPlaybackEvent.Seeked(it, sessionPlayer.currentPosition.coerceAtLeast(0L))) }
    }

    fun currentPositionMs(): Long = sessionPlayer.currentPosition.coerceAtLeast(0L)

    fun refreshPositionFromPlayer() {
        forcePlaybackSnapshot()
    }

    fun forcePlaybackSnapshot(): VantafynMusicPlaybackState {
        var snapshot = _state.value
        _state.update { state ->
            val currentIndex = sessionPlayer.currentMediaItemIndex.takeIf { it >= 0 } ?: state.queueIndex
            val currentTrack = state.queue.getOrNull(currentIndex) ?: state.currentTrack
            state.copy(
                queueIndex = currentIndex,
                positionMs = sessionPlayer.currentPosition.coerceAtLeast(0L),
                durationMs = sessionPlayer.duration.takeIf { it > 0 } ?: currentTrack?.durationMs ?: state.durationMs,
                isPlaying = sessionPlayer.isPlaying,
            ).also { snapshot = it }
        }
        return snapshot
    }

    fun toggleShuffle() {
        val enabled = !_state.value.shuffleEnabled
        sessionPlayer.shuffleModeEnabled = enabled
        _state.update { it.copy(shuffleEnabled = enabled) }
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            VantafynMusicRepeatMode.Off -> VantafynMusicRepeatMode.All
            VantafynMusicRepeatMode.All -> VantafynMusicRepeatMode.One
            VantafynMusicRepeatMode.One -> VantafynMusicRepeatMode.Off
        }
        sessionPlayer.repeatMode = when (next) {
            VantafynMusicRepeatMode.Off -> Player.REPEAT_MODE_OFF
            VantafynMusicRepeatMode.One -> Player.REPEAT_MODE_ONE
            VantafynMusicRepeatMode.All -> Player.REPEAT_MODE_ALL
        }
        _state.update { it.copy(repeatMode = next) }
    }

    internal fun adoptSystemQueue(queue: List<VantafynMusicTrack>, startIndex: Int = 0, startPositionMs: Long = 0L): List<MediaItem> {
        if (queue.isEmpty()) return emptyList()
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
        tracksByMediaId.clear()
        queue.forEach { track -> tracksByMediaId[track.id.toString()] = track }
        _state.update {
            it.copy(
                queue = queue,
                queueIndex = safeIndex,
                positionMs = startPositionMs.coerceAtLeast(0L),
                durationMs = queue[safeIndex].durationMs ?: 0L,
                errorMessage = null,
            )
        }
        ensurePlaybackService()
        return queue.map { it.toMediaItem() }
    }

    fun release() {
        tickerJob?.cancel()
        LongRunningTaskRegistry.stop(MUSIC_TICKER_TASK_ID, "controller released")
        sessionPlayer.release()
        scope.cancel()
    }

    private fun syncTicker() {
        if (!sessionPlayer.isPlaying || !AppForegroundStateRepository.isForeground.value) {
            tickerJob?.cancel()
            tickerJob = null
            LongRunningTaskRegistry.stop(MUSIC_TICKER_TASK_ID, if (!sessionPlayer.isPlaying) "music paused" else "background idle")
            Log.d(TAG, "Ticker stopped (playing=${sessionPlayer.isPlaying}, foreground=${AppForegroundStateRepository.isForeground.value})")
            return
        }
        if (tickerJob != null) return
        LongRunningTaskRegistry.start(
            id = MUSIC_TICKER_TASK_ID,
            type = LongRunningTaskType.MusicService,
            owner = "MusicPlaybackController",
            state = "playing",
        )
        Log.d(TAG, "Ticker started (foreground=true)")
        tickerJob = scope.launch {
            while (isActive) {
                _state.update { state ->
                    val currentIndex = sessionPlayer.currentMediaItemIndex.takeIf { it >= 0 } ?: state.queueIndex
                    val currentTrack = state.queue.getOrNull(currentIndex) ?: state.currentTrack
                    state.copy(
                        queueIndex = currentIndex,
                        positionMs = sessionPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = sessionPlayer.duration.takeIf { it > 0 } ?: currentTrack?.durationMs ?: state.durationMs,
                        isPlaying = sessionPlayer.isPlaying,
                    )
                }
                val now = System.currentTimeMillis()
                if (now - lastRegistryTickMs >= ForegroundTickerIntervalMs) {
                    lastRegistryTickMs = now
                    LongRunningTaskRegistry.tick(MUSIC_TICKER_TASK_ID, if (sessionPlayer.isPlaying) "playing" else "paused")
                }
                delay(ForegroundTickerIntervalMs)
            }
        }
    }

    private fun emitEvent(event: VantafynMusicPlaybackEvent) {
        _events.tryEmit(event)
    }

    @OptIn(UnstableApi::class)
    private fun ExoPlayer.enableCompatibleAudioOffload() {
        val audioOffloadPreferences = AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
            .setIsGaplessSupportRequired(false)
            .build()
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setAudioOffloadPreferences(audioOffloadPreferences)
            .build()
    }

    private fun ensurePlaybackService() {
        if (playbackServiceStarted) return
        val intent = Intent(appContext, VantafynMusicPlaybackService::class.java)
        runCatching {
            androidx.core.content.ContextCompat.startForegroundService(appContext, intent)
            playbackServiceStarted = true
        }.onFailure { e ->
            Log.w(TAG, "Failed to start playback service: ${e.message}")
        }
    }

    private fun stopPlaybackService() {
        val intent = Intent(appContext, VantafynMusicPlaybackService::class.java)
        runCatching { appContext.stopService(intent) }
        LongRunningTaskRegistry.stop("music.playbackService", "controller stopped service")
        playbackServiceStarted = false
    }

    private fun VantafynMusicTrack.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artworkUrl?.let(Uri::parse))
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

    companion object {
        private const val TAG = "MusicPlaybackController"
        private const val MUSIC_TICKER_TASK_ID = "music.positionTicker"
        private const val ForegroundTickerIntervalMs = 1_000L
        private const val BackgroundTickerIntervalMs = 10_000L
        private const val BackgroundRegistryTickIntervalMs = 60_000L

        @Volatile
        private var instance: MusicPlaybackController? = null

        fun get(context: Context): MusicPlaybackController =
            instance ?: synchronized(this) {
                instance ?: MusicPlaybackController(context).also { instance = it }
            }
    }
}

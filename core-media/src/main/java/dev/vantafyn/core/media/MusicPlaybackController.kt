package dev.vantafyn.core.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
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

data class VantafynMusicPlaybackState(
    val queue: List<VantafynMusicTrack> = emptyList(),
    val queueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: VantafynMusicRepeatMode = VantafynMusicRepeatMode.Off,
    val errorMessage: String? = null,
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
    private var lastTransitionReason: VantafynMusicStopReason = VantafynMusicStopReason.QueueChange
    private var playbackServiceStarted = false

    internal val sessionPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        setHandleAudioBecomingNoisy(true)
        setWakeMode(C.WAKE_MODE_NETWORK)
        addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
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
                    Log.d(
                        MusicLogTag,
                        "Media item transition reason=$reason queueIndex=$currentIndex mediaId=${mediaItem?.mediaId.orEmpty().take(36)}",
                    )
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
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d(
                        MusicLogTag,
                        "Player state=$playbackState queueIndex=${sessionPlayer.currentMediaItemIndex} count=${sessionPlayer.mediaItemCount} playing=${sessionPlayer.isPlaying}",
                    )
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
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.d(
                        MusicLogTag,
                        "Player error=${error.errorCodeName} queueIndex=${sessionPlayer.currentMediaItemIndex} hasNext=${sessionPlayer.hasNextMediaItem()}",
                    )
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

    fun playQueue(queue: List<VantafynMusicTrack>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
        val previous = _state.value.currentTrack
        val previousPosition = sessionPlayer.currentPosition.coerceAtLeast(0L)
        Log.d(MusicLogTag, "playQueue queueSize=${queue.size} startIndex=$safeIndex")
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
        sessionPlayer.setMediaItems(queue.map { it.toMediaItem() }, safeIndex, 0L)
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
                Log.d(MusicLogTag, "Recovering play command state=${sessionPlayer.playbackState} queueIndex=$index")
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

    fun next() {
        ensurePlaybackService()
        if (sessionPlayer.hasNextMediaItem()) {
            lastTransitionReason = VantafynMusicStopReason.Skip
            sessionPlayer.seekToNextMediaItem()
            sessionPlayer.play()
        } else if (_state.value.repeatMode == VantafynMusicRepeatMode.All && _state.value.queue.isNotEmpty()) {
            lastTransitionReason = VantafynMusicStopReason.Skip
            sessionPlayer.seekTo(0, 0L)
            sessionPlayer.play()
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
        val insertIndex = (_state.value.queueIndex + 1).coerceAtMost(_state.value.queue.size)
        sessionPlayer.addMediaItem(insertIndex, track.toMediaItem())
        _state.update { it.copy(queue = it.queue.toMutableList().apply { add(insertIndex, track) }) }
    }

    fun addToQueue(track: VantafynMusicTrack) {
        sessionPlayer.addMediaItem(track.toMediaItem())
        _state.update { it.copy(queue = it.queue + track) }
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
    }

    fun seekTo(positionMs: Long) {
        sessionPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = sessionPlayer.currentPosition.coerceAtLeast(0L)) }
        _state.value.currentTrack?.let { emitEvent(VantafynMusicPlaybackEvent.Seeked(it, sessionPlayer.currentPosition.coerceAtLeast(0L))) }
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

    fun release() {
        tickerJob?.cancel()
        sessionPlayer.release()
        scope.cancel()
    }

    private fun syncTicker() {
        if (!sessionPlayer.isPlaying) {
            tickerJob?.cancel()
            tickerJob = null
            return
        }
        if (tickerJob != null) return
        tickerJob = scope.launch {
            while (isActive) {
                _state.update { state ->
                    state.copy(
                        queueIndex = sessionPlayer.currentMediaItemIndex.takeIf { it >= 0 } ?: state.queueIndex,
                        positionMs = sessionPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = sessionPlayer.duration.takeIf { it > 0 } ?: state.currentTrack?.durationMs ?: state.durationMs,
                        isPlaying = sessionPlayer.isPlaying,
                    )
                }
                delay(500L)
            }
        }
    }

    private fun emitEvent(event: VantafynMusicPlaybackEvent) {
        _events.tryEmit(event)
    }

    private fun ensurePlaybackService() {
        if (playbackServiceStarted) return
        val intent = Intent(appContext, VantafynMusicPlaybackService::class.java)
        runCatching {
            androidx.core.content.ContextCompat.startForegroundService(appContext, intent)
        }.recoverCatching {
            appContext.startService(intent)
        }.onSuccess {
            playbackServiceStarted = true
        }.onFailure {
            Log.d(MusicLogTag, "Unable to start music playback service: ${it.message.orEmpty().take(120)}")
        }
    }

    private fun stopPlaybackService() {
        val intent = Intent(appContext, VantafynMusicPlaybackService::class.java)
        runCatching { appContext.stopService(intent) }
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
        private const val MusicLogTag = "VantafynMusic"

        @Volatile
        private var instance: MusicPlaybackController? = null

        fun get(context: Context): MusicPlaybackController =
            instance ?: synchronized(this) {
                instance ?: MusicPlaybackController(context).also { instance = it }
            }
    }
}

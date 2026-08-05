package dev.vantafyn.core.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
    val durationMs: Long?,
    val streamUrl: String,
    val artworkUrl: String?,
)

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

class MusicPlaybackController private constructor(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update { it.copy(isPlaying = isPlaying, errorMessage = null) }
                    syncTicker()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _state.update { state ->
                        state.copy(
                            queueIndex = currentMediaItemIndex.takeIf { it >= 0 } ?: state.queueIndex,
                            durationMs = duration.takeIf { it > 0 } ?: state.durationMs,
                            positionMs = currentPosition.coerceAtLeast(0L),
                        )
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
                        _state.update { it.copy(isPlaying = false) }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
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

    fun playQueue(queue: List<VantafynMusicTrack>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
        player.setMediaItems(queue.map { it.toMediaItem() }, safeIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        _state.update {
            it.copy(
                queue = queue,
                queueIndex = safeIndex,
                positionMs = 0L,
                durationMs = queue[safeIndex].durationMs ?: 0L,
                errorMessage = null,
            )
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun pause() {
        if (player.isPlaying) player.pause()
    }

    fun stop(clearQueue: Boolean = false) {
        player.stop()
        if (clearQueue) {
            player.clearMediaItems()
        }
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
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        } else if (_state.value.repeatMode == VantafynMusicRepeatMode.All && _state.value.queue.isNotEmpty()) {
            player.seekTo(0, 0L)
            player.play()
        }
    }

    fun previous() {
        if (player.currentPosition > 3_000L || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
        player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0L)) }
    }

    fun toggleShuffle() {
        val enabled = !_state.value.shuffleEnabled
        player.shuffleModeEnabled = enabled
        _state.update { it.copy(shuffleEnabled = enabled) }
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            VantafynMusicRepeatMode.Off -> VantafynMusicRepeatMode.All
            VantafynMusicRepeatMode.All -> VantafynMusicRepeatMode.One
            VantafynMusicRepeatMode.One -> VantafynMusicRepeatMode.Off
        }
        player.repeatMode = when (next) {
            VantafynMusicRepeatMode.Off -> Player.REPEAT_MODE_OFF
            VantafynMusicRepeatMode.One -> Player.REPEAT_MODE_ONE
            VantafynMusicRepeatMode.All -> Player.REPEAT_MODE_ALL
        }
        _state.update { it.copy(repeatMode = next) }
    }

    fun release() {
        tickerJob?.cancel()
        player.release()
        scope.cancel()
    }

    private fun syncTicker() {
        if (!player.isPlaying) {
            tickerJob?.cancel()
            tickerJob = null
            return
        }
        if (tickerJob != null) return
        tickerJob = scope.launch {
            while (isActive) {
                _state.update { state ->
                    state.copy(
                        queueIndex = player.currentMediaItemIndex.takeIf { it >= 0 } ?: state.queueIndex,
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.takeIf { it > 0 } ?: state.currentTrack?.durationMs ?: state.durationMs,
                        isPlaying = player.isPlaying,
                    )
                }
                delay(500L)
            }
        }
    }

    private fun VantafynMusicTrack.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(id.toString())
            .build()

    companion object {
        @Volatile
        private var instance: MusicPlaybackController? = null

        fun get(context: Context): MusicPlaybackController =
            instance ?: synchronized(this) {
                instance ?: MusicPlaybackController(context).also { instance = it }
            }
    }
}

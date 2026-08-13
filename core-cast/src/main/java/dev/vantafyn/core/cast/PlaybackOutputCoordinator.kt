package dev.vantafyn.core.cast

import android.content.Context
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.VantafynPlaybackItem
import dev.vantafyn.core.media.VantafynMusicPlaybackState
import dev.vantafyn.core.media.VantafynMusicRepeatMode
import dev.vantafyn.core.media.VantafynMusicStopReason
import dev.vantafyn.core.media.VantafynMusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class PlaybackOutputType {
    Local,
    GoogleCast,
    FutureSyncPlay,
    FutureVantafynTV,
}

data class PlaybackOutputState(
    val activeOutput: PlaybackOutputType = PlaybackOutputType.Local,
    val castState: RemotePlaybackState = RemotePlaybackState(),
    val lastErrorMessage: String? = null,
) {
    val isCasting: Boolean
        get() = activeOutput == PlaybackOutputType.GoogleCast &&
            castState.connectionState == RemoteConnectionState.Connected
}

class PlaybackOutputCoordinator private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val musicController = MusicPlaybackController.get(appContext)
    private val castTarget = GoogleCastPlaybackTarget.get(appContext)
    private var bridgeJob: Job? = null
    private var musicJob: Job? = null
    private var loadedSessionId: String? = null

    private val _state = MutableStateFlow(PlaybackOutputState())
    val state: StateFlow<PlaybackOutputState> = _state.asStateFlow()

    fun start() {
        castTarget.start()
        if (bridgeJob != null) return
        bridgeJob = scope.launch {
            castTarget.state.collectLatest { castState ->
                _state.update { it.copy(castState = castState) }
                if (castState.connectionState == RemoteConnectionState.Connected) {
                    transferCurrentMusicIfNeeded(musicController.state.value, castState)
                } else if (castState.connectionState == RemoteConnectionState.Disconnected) {
                    loadedSessionId = null
                    _state.update { it.copy(activeOutput = PlaybackOutputType.Local) }
                }
            }
        }
        musicJob = scope.launch {
            musicController.state.collectLatest { localState ->
                val outputState = _state.value
                if (
                    localState.isPlaying &&
                    outputState.castState.connectionState == RemoteConnectionState.Connected &&
                    outputState.activeOutput != PlaybackOutputType.GoogleCast
                ) {
                    transferCurrentMusicIfNeeded(localState, outputState.castState)
                }
            }
        }
    }

    fun stop() {
        bridgeJob?.cancel()
        bridgeJob = null
        musicJob?.cancel()
        musicJob = null
    }

    fun clearForLogoutOrServerSwitch() {
        scope.launch {
            runCatching { castTarget.disconnect(stopPlayback = true) }
            loadedSessionId = null
            _state.update { PlaybackOutputState() }
        }
    }

    fun playPause() {
        scope.launch {
            runCatching {
                if (castTarget.state.value.isPlaying) castTarget.pause() else castTarget.play()
            }.onFailure { setError(it) }
        }
    }

    fun seekTo(positionMs: Long) {
        scope.launch {
            runCatching { castTarget.seek(positionMs) }.onFailure { setError(it) }
        }
    }

    fun selectCastSubtitle(trackId: Long?) {
        scope.launch {
            runCatching {
                castTarget.selectSubtitleTrack(trackId)
                _state.update { it.copy(lastErrorMessage = null) }
            }.onFailure {
                _state.update { state -> state.copy(lastErrorMessage = "Couldn't switch subtitles while casting.") }
            }
        }
    }

    fun next() {
        scope.launch {
            runCatching { castTarget.skipNext() }.onFailure { setError(it) }
        }
    }

    fun previous() {
        scope.launch {
            runCatching { castTarget.skipPrevious() }.onFailure { setError(it) }
        }
    }

    fun disconnect(stopPlayback: Boolean) {
        scope.launch {
            runCatching { castTarget.disconnect(stopPlayback) }.onFailure { setError(it) }
        }
    }

    fun loadVideo(item: VantafynPlaybackItem, startPositionMs: Long, artworkUrl: String? = null, backdropUrl: String? = null) {
        scope.launch {
            runCatching {
                val remoteItem = item.toVideoRemoteQueueItem(artworkUrl, backdropUrl)
                castTarget.load(RemotePlaybackRequest(remoteItem, startPositionMs, autoplay = true))
                loadedSessionId = "video:${item.itemId}"
                _state.update { it.copy(activeOutput = PlaybackOutputType.GoogleCast, lastErrorMessage = null) }
            }.onFailure { error ->
                _state.update { it.copy(activeOutput = PlaybackOutputType.Local) }
                setError(error)
            }
        }
    }

    fun playVideoOnThisDevice(stopCastPlayback: Boolean = true) {
        scope.launch {
            runCatching { castTarget.disconnect(stopCastPlayback) }.onFailure { setError(it) }
            loadedSessionId = null
            _state.update { it.copy(activeOutput = PlaybackOutputType.Local) }
        }
    }

    private suspend fun transferCurrentMusicIfNeeded(localState: VantafynMusicPlaybackState, castState: RemotePlaybackState) {
        val track = localState.currentTrack ?: return
        val sessionId = "${track.id}:${localState.queueIndex}:${localState.queue.size}"
        if (loadedSessionId == sessionId || castState.currentItemId == track.id.toString()) return
        val queue = localState.queue.map { it.toRemoteQueueItem() }
        val position = localState.positionMs
        runCatching {
            castTarget.replaceQueue(queue, localState.queueIndex, position)
            if (localState.repeatMode != VantafynMusicRepeatMode.Off || localState.shuffleEnabled) {
                // Default receiver support varies; local flags are represented in state but queue-load is conservative.
            }
            loadedSessionId = sessionId
            musicController.stop(clearQueue = false, reason = VantafynMusicStopReason.Background)
            _state.update { it.copy(activeOutput = PlaybackOutputType.GoogleCast, lastErrorMessage = null) }
        }.onFailure { error ->
            loadedSessionId = null
            _state.update { it.copy(activeOutput = PlaybackOutputType.Local) }
            setError(error)
        }
    }

    private fun VantafynMusicTrack.toRemoteQueueItem(): RemoteQueueItem =
        RemoteQueueItem(
            itemId = id,
            title = title,
            artist = artist,
            albumTitle = album,
            streamUrl = streamUrl,
            artworkUrl = artworkUrl,
            durationMs = durationMs,
            contentType = contentTypeFor(streamUrl),
        )

    private fun VantafynPlaybackItem.toVideoRemoteQueueItem(artworkUrl: String?, backdropUrl: String?): RemoteQueueItem =
        CastTrackMapper.map(subtitleTracks = subtitleTracks, audioTracks = audioTracks).let { trackSupport ->
            RemoteQueueItem(
            itemId = UUID.fromString(itemId),
            title = if (itemType.equals("Episode", ignoreCase = true)) subtitle ?: title else title,
            artist = null,
            albumTitle = null,
            subtitle = if (itemType.equals("Episode", ignoreCase = true)) title else subtitle,
            seriesTitle = if (itemType.equals("Episode", ignoreCase = true)) title else null,
            streamUrl = streamUrl,
            artworkUrl = artworkUrl,
            backdropUrl = backdropUrl,
            durationMs = durationMs,
            contentType = videoContentTypeFor(streamUrl, isLiveStream),
            mediaKind = when {
                isLiveStream -> RemoteMediaKind.LiveTv
                itemType.equals("Episode", ignoreCase = true) -> RemoteMediaKind.Episode
                itemType.equals("Movie", ignoreCase = true) -> RemoteMediaKind.Movie
                else -> RemoteMediaKind.Unknown
            },
            isLive = isLiveStream,
            castSubtitleTracks = trackSupport.subtitles,
            castAudioTracks = trackSupport.audioTracks,
            activeSubtitleTrackId = selectedSubtitleStreamIndex?.let { selected ->
                trackSupport.subtitles.firstOrNull { it.streamIndex == selected }?.castTrackId
            } ?: trackSupport.subtitles.firstOrNull { it.isDefault }?.castTrackId,
        )
    }

    private fun contentTypeFor(url: String): String =
        when (url.substringBefore('?').substringAfterLast('.', "").lowercase()) {
            "m4a", "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "opus" -> "audio/ogg"
            "ogg" -> "audio/ogg"
            "mp4", "m4b" -> "audio/mp4"
            else -> "audio/mpeg"
        }

    private fun videoContentTypeFor(url: String, isLive: Boolean): String =
        when {
            isLive -> "application/x-mpegURL"
            url.substringBefore('?').endsWith(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
            url.substringBefore('?').substringAfterLast('.', "").lowercase() in setOf("mp4", "m4v", "mov") -> "video/mp4"
            url.substringBefore('?').substringAfterLast('.', "").lowercase() == "webm" -> "video/webm"
            else -> "video/mp4"
        }

    private fun setError(error: Throwable) {
        val message = when ((error as? CastCommandException)?.error) {
            CastError.ServerAddressUnreachable -> CastUrlSecurity.userMessageForUnreachableAddress(musicController.state.value.currentTrack?.streamUrl.orEmpty())
            CastError.ReceiverLoadFailed -> "Chromecast could not start this item. Vantafyn kept playback on this phone."
            CastError.SessionLost -> "Cast session was lost."
            CastError.RemoteCommandFailed -> "Chromecast did not accept that command."
            CastError.NoCompatibleMediaSource -> "No cast-compatible source is available."
            else -> "Cast is unavailable right now."
        }
        _state.update { it.copy(lastErrorMessage = message) }
    }

    companion object {
        @Volatile
        private var instance: PlaybackOutputCoordinator? = null

        fun get(context: Context): PlaybackOutputCoordinator =
            instance ?: synchronized(this) {
                instance ?: PlaybackOutputCoordinator(context).also { instance = it }
            }
    }
}

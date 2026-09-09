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
        get() = (activeOutput == PlaybackOutputType.GoogleCast || castState.connectionState == RemoteConnectionState.Connected) &&
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
        var lastConnectionState = castTarget.state.value.connectionState
        bridgeJob = scope.launch {
            castTarget.state.collectLatest { castState ->
                val prevConnectionState = lastConnectionState
                lastConnectionState = castState.connectionState
                _state.update { current -> current.copy(castState = castState) }
                if (castState.connectionState == RemoteConnectionState.Connected) {
                    if (prevConnectionState != RemoteConnectionState.Connected) {
                        val local = musicController.state.value
                        if (local.isPlaying && local.currentTrack != null) {
                            transferCurrentMusicIfNeeded(local, castState)
                        } else if (castState.hasActiveMedia) {
                            _state.update { it.copy(activeOutput = PlaybackOutputType.GoogleCast) }
                        }
                    }
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
                    outputState.activeOutput != PlaybackOutputType.GoogleCast &&
                    outputState.castState.connectionState == RemoteConnectionState.Connected
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
                requireActiveCastMedia()
                if (castTarget.state.value.isPlaying) castTarget.pause() else castTarget.play()
            }.onFailure { setError(it) }
        }
    }

    fun seekTo(positionMs: Long) {
        scope.launch {
            runCatching {
                requireActiveCastMedia()
                castTarget.seek(positionMs)
            }.onFailure { setError(it) }
        }
    }

    fun selectCastSubtitle(trackId: Long?) {
        scope.launch {
            runCatching {
                requireActiveCastMedia()
                castTarget.selectSubtitleTrack(trackId)
                _state.update { it.copy(lastErrorMessage = null) }
            }.onFailure {
                _state.update { state -> state.copy(lastErrorMessage = "Couldn't switch subtitles while casting.") }
            }
        }
    }

    fun next() {
        scope.launch {
            runCatching {
                requireActiveCastMedia()
                castTarget.skipNext()
            }.onFailure { setError(it) }
        }
    }

    fun previous() {
        scope.launch {
            runCatching {
                requireActiveCastMedia()
                castTarget.skipPrevious()
            }.onFailure { setError(it) }
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
                _state.update {
                    it.copy(
                        activeOutput = PlaybackOutputType.GoogleCast,
                        castState = it.castState.copy(
                            currentItemId = item.itemId,
                            positionMs = startPositionMs.coerceAtLeast(0L),
                            durationMs = item.durationMs ?: it.castState.durationMs,
                            isPlaying = true,
                            subtitleTracks = remoteItem.castSubtitleTracks,
                            activeSubtitleTrackId = remoteItem.activeSubtitleTrackId,
                            audioTracks = remoteItem.castAudioTracks,
                        ),
                        lastErrorMessage = null,
                    )
                }
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

    fun playQueueIndex(index: Int) {
        scope.launch {
            runCatching {
                requireActiveCastMedia()
                castTarget.playQueueIndex(index)
            }.onFailure { setError(it) }
        }
    }

    fun loadMusicQueue(tracks: List<VantafynMusicTrack>, startIndex: Int, startPositionMs: Long) {
        scope.launch {
            if (tracks.isEmpty()) return@launch
            val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
            val track = tracks[safeIndex]
            val sessionId = "${track.id}:$safeIndex:${tracks.size}"
            val queue = tracks.map { it.toRemoteQueueItem() }
            val position = startPositionMs.coerceAtLeast(0L)
            // Immediately suspend local phone audio hardware to avoid speaker bleed
            musicController.suspendLocalPlaybackForCast(position)
            runCatching {
                castTarget.replaceQueue(queue, safeIndex, position)
                loadedSessionId = sessionId
                _state.update {
                    it.copy(
                        activeOutput = PlaybackOutputType.GoogleCast,
                        castState = it.castState.copy(
                            currentItemId = track.id.toString(),
                            currentQueueIndex = safeIndex,
                            positionMs = position,
                            durationMs = track.durationMs ?: it.castState.durationMs,
                            isPlaying = true,
                        ),
                        lastErrorMessage = null,
                    )
                }
            }.onFailure { error ->
                loadedSessionId = null
                _state.update { it.copy(activeOutput = PlaybackOutputType.Local) }
                setError(error)
            }
        }
    }

    private suspend fun transferCurrentMusicIfNeeded(localState: VantafynMusicPlaybackState, castState: RemotePlaybackState) {
        val track = localState.currentTrack ?: return
        val sessionId = "${track.id}:${localState.queueIndex}:${localState.queue.size}"
        if (loadedSessionId == sessionId) return
        val queue = localState.queue.map { it.toRemoteQueueItem() }
        val position = localState.positionMs
        // Suspend local phone audio before loading remote queue to avoid dual playback
        musicController.suspendLocalPlaybackForCast(position)
        runCatching {
            castTarget.replaceQueue(queue, localState.queueIndex, position)
            loadedSessionId = sessionId
            _state.update {
                it.copy(
                    activeOutput = PlaybackOutputType.GoogleCast,
                    castState = it.castState.copy(
                        currentItemId = track.id.toString(),
                        currentQueueIndex = localState.queueIndex,
                        positionMs = position.coerceAtLeast(0L),
                        durationMs = track.durationMs ?: it.castState.durationMs,
                        isPlaying = true,
                    ),
                    lastErrorMessage = null,
                )
            }
        }.onFailure { error ->
            loadedSessionId = null
            _state.update { it.copy(activeOutput = PlaybackOutputType.Local) }
            setError(error)
        }
    }

    private fun requireActiveCastMedia() {
        val output = _state.value
        if (output.activeOutput != PlaybackOutputType.GoogleCast || output.castState.currentItemId.isNullOrBlank()) {
            throw CastCommandException(CastError.NoCompatibleMediaSource)
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

    private fun contentTypeFor(url: String): String {
        val lower = url.lowercase()
        val path = url.substringBefore('?').lowercase()
        val formatParam = url.substringAfter('?', "").split("&")
            .firstOrNull { it.startsWith("format=", ignoreCase = true) }
            ?.substringAfter("=")?.lowercase()
        return when {
            formatParam in setOf("flac") || path.endsWith(".flac") || lower.contains(".flac") -> "audio/flac"
            formatParam in setOf("aac", "m4a") || path.endsWith(".m4a") || path.endsWith(".aac") -> "audio/aac"
            formatParam in setOf("opus") || path.endsWith(".opus") || lower.contains(".opus") -> "audio/ogg"
            formatParam in setOf("ogg", "vorbis") || path.endsWith(".ogg") || lower.contains(".ogg") -> "audio/ogg"
            formatParam in setOf("wav") || path.endsWith(".wav") || lower.contains(".wav") -> "audio/wav"
            else -> "audio/mpeg"
        }
    }

    private fun videoContentTypeFor(url: String, isLive: Boolean): String {
        val lower = url.lowercase()
        val path = url.substringBefore('?').lowercase()
        return when {
            isLive -> "application/x-mpegURL"
            path.endsWith(".m3u8") || lower.contains(".m3u8") || lower.contains("/hls/") || lower.contains("protocol=hls") -> "application/x-mpegURL"
            path.endsWith(".mpd") || lower.contains(".mpd") || lower.contains("/dash/") -> "application/dash+xml"
            path.endsWith(".webm") || lower.contains(".webm") -> "video/webm"
            path.endsWith(".mkv") || lower.contains(".mkv") -> "video/x-matroska"
            path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".mov") -> "video/mp4"
            else -> "video/mp4"
        }
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

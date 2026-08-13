package dev.vantafyn.core.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Result
import dev.vantafyn.core.media.LongRunningTaskRegistry
import dev.vantafyn.core.media.LongRunningTaskType
import dev.vantafyn.core.media.VantafynMusicRepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

class GoogleCastPlaybackTarget private constructor(context: Context) : RemotePlaybackTarget {
    private val appContext = context.applicationContext
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val castContext: CastContext? by lazy {
        runCatching { CastContext.getSharedInstance(appContext) }.getOrNull()
    }
    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            updateSession(session, RemoteConnectionState.Connecting)
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            updateSession(session, RemoteConnectionState.Connected)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _state.update { it.copy(connectionState = RemoteConnectionState.Failed, lastError = CastError.ReceiverLaunchFailed) }
        }

        override fun onSessionEnding(session: CastSession) {
            updateSession(session, RemoteConnectionState.Disconnecting)
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            clearRemoteClient()
            _state.update { RemotePlaybackState(connectionState = RemoteConnectionState.Disconnected) }
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            updateSession(session, RemoteConnectionState.Reconnecting)
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            updateSession(session, RemoteConnectionState.Connected)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _state.update { it.copy(connectionState = RemoteConnectionState.Failed, lastError = CastError.SessionLost) }
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            updateSession(session, RemoteConnectionState.Reconnecting)
        }
    }
    private val remoteListener = object : RemoteMediaClient.Listener {
        override fun onStatusUpdated() {
            syncRemoteState()
        }

        override fun onMetadataUpdated() {
            syncRemoteState()
        }

        override fun onQueueStatusUpdated() {
            syncRemoteState()
        }

        override fun onPreloadStatusUpdated() = Unit
        override fun onSendingRemoteMediaRequest() = Unit
        override fun onAdBreakStatusUpdated() = Unit
    }
    private var remoteClient: RemoteMediaClient? = null
    private var registered = false
    private var positionTickerJob: Job? = null
    private var pendingQueue: List<RemoteQueueItem> = emptyList()

    private val _state = MutableStateFlow(RemotePlaybackState())
    val state: StateFlow<RemotePlaybackState> = _state.asStateFlow()

    override val id: String = "google-cast"
    override val name: String = "Google Cast"
    override val type: RemotePlaybackTargetType = RemotePlaybackTargetType.GoogleCast
    override val connectionState: RemoteConnectionState
        get() = _state.value.connectionState
    override val capabilities: RemotePlaybackCapabilities = RemotePlaybackCapabilities()

    fun start() {
        if (!VantafynCastFeatureFlags.googleCastEnabled || registered) return
        if (!scope.isActive) scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val sessionManager = castContext?.sessionManager ?: run {
            _state.update { it.copy(connectionState = RemoteConnectionState.Unavailable, lastError = CastError.CastUnavailable) }
            return
        }
        sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        registered = true
        sessionManager.currentCastSession?.let { updateSession(it, RemoteConnectionState.Connected) }
    }

    fun stop() {
        castContext?.sessionManager?.removeSessionManagerListener(listener, CastSession::class.java)
        registered = false
        stopPositionTicker("cast target stopped")
        clearRemoteClient()
        scope.cancel()
    }

    override suspend fun connect() {
        start()
    }

    override suspend fun disconnect(stopPlayback: Boolean) {
        val stoppedItem = _state.value.currentItemId
        val stoppedPosition = remoteClient?.approximateStreamPosition ?: _state.value.positionMs
        if (stopPlayback) {
            runCatching { remoteClient?.stop()?.awaitResult() }
        }
        castContext?.sessionManager?.endCurrentSession(stopPlayback)
        clearRemoteClient()
        _state.update {
            RemotePlaybackState(
                connectionState = RemoteConnectionState.Disconnected,
                lastStoppedItemId = stoppedItem,
                lastStoppedPositionMs = stoppedPosition.coerceAtLeast(0L),
            )
        }
    }

    override suspend fun load(request: RemotePlaybackRequest) {
        ensureReachable(request.item)
        val mediaInfo = request.item.toMediaInfo()
        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(request.autoplay)
            .setCurrentTime(request.startPositionMs.coerceAtLeast(0L))
            .apply {
                request.item.activeSubtitleTrackId?.let { setActiveTrackIds(longArrayOf(it)) }
            }
            .build()
        val client = remoteClient ?: throw CastCommandException(CastError.SessionLost)
        val result = client.load(loadRequest).awaitResult()
        if (!result.status.isSuccess) throw CastCommandException(CastError.ReceiverLoadFailed)
        pendingQueue = listOf(request.item)
        syncRemoteState()
    }

    override suspend fun play() {
        val result = (remoteClient ?: throw CastCommandException(CastError.SessionLost)).play().awaitResult()
        if (!result.status.isSuccess) throw CastCommandException(CastError.RemoteCommandFailed)
    }

    override suspend fun pause() {
        val result = (remoteClient ?: throw CastCommandException(CastError.SessionLost)).pause().awaitResult()
        if (!result.status.isSuccess) throw CastCommandException(CastError.RemoteCommandFailed)
    }

    override suspend fun seek(positionMs: Long) {
        val result = (remoteClient ?: throw CastCommandException(CastError.SessionLost))
            .seek(MediaSeekOptions.Builder().setPosition(positionMs.coerceAtLeast(0L)).build())
            .awaitResult()
        if (!result.status.isSuccess) throw CastCommandException(CastError.RemoteCommandFailed)
    }

    override suspend fun skipNext() {
        val result = (remoteClient ?: throw CastCommandException(CastError.SessionLost)).queueNext(null).awaitResult()
        if (!result.status.isSuccess) throw CastCommandException(CastError.RemoteCommandFailed)
    }

    override suspend fun skipPrevious() {
        val result = (remoteClient ?: throw CastCommandException(CastError.SessionLost)).queuePrev(null).awaitResult()
        if (!result.status.isSuccess) throw CastCommandException(CastError.RemoteCommandFailed)
    }

    override suspend fun setVolume(volume: Float) {
        val session = castContext?.sessionManager?.currentCastSession ?: throw CastCommandException(CastError.SessionLost)
        session.setVolume(volume.coerceIn(0f, 1f).toDouble())
        syncRemoteState()
    }

    override suspend fun setMuted(muted: Boolean) {
        val session = castContext?.sessionManager?.currentCastSession ?: throw CastCommandException(CastError.SessionLost)
        session.setMute(muted)
        syncRemoteState()
    }

    override suspend fun selectSubtitleTrack(trackId: Long?) {
        val client = remoteClient ?: throw CastCommandException(CastError.SessionLost)
        val activeIds = trackId?.let { longArrayOf(it) } ?: longArrayOf()
        val result = client.setActiveMediaTracks(activeIds).awaitResult()
        if (!result.status.isSuccess) throw CastCommandException(CastError.RemoteCommandFailed)
        syncRemoteState()
    }

    override suspend fun replaceQueue(queue: List<RemoteQueueItem>, startIndex: Int, startPositionMs: Long) {
        if (queue.isEmpty()) throw CastCommandException(CastError.NoCompatibleMediaSource)
        queue.forEach(::ensureReachable)
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
        val items = queue.map { MediaQueueItem.Builder(it.toMediaInfo()).setAutoplay(true).build() }.toTypedArray()
        val result = (remoteClient ?: throw CastCommandException(CastError.SessionLost))
            .queueLoad(items, safeIndex, MediaStatus.REPEAT_MODE_REPEAT_OFF, startPositionMs.coerceAtLeast(0L), null)
            .awaitResult()
        if (!result.status.isSuccess) throw CastCommandException(CastError.ReceiverLoadFailed)
        pendingQueue = queue
        syncRemoteState()
    }

    private fun updateSession(session: CastSession, state: RemoteConnectionState) {
        clearRemoteClient()
        remoteClient = session.remoteMediaClient?.also { it.addListener(remoteListener) }
        _state.update {
            it.copy(
                availability = true,
                connectionState = state,
                receiverName = session.castDevice?.friendlyName,
                receiverId = session.castDevice?.deviceId,
                volume = session.volume.toFloat(),
                isMuted = session.isMute,
                lastError = null,
            )
        }
        syncRemoteState()
        startPositionTicker()
    }

    private fun clearRemoteClient() {
        remoteClient?.removeListener(remoteListener)
        remoteClient = null
        pendingQueue = emptyList()
        stopPositionTicker("no remote client")
    }

    private fun syncRemoteState() {
        val client = remoteClient ?: return
        val status = client.mediaStatus
        val queueItem = status?.getQueueItemById(status.currentItemId)
        val mediaInfo = queueItem?.media ?: status?.mediaInfo
        val customData = mediaInfo?.customData
        val queueItems = status?.queueItems.orEmpty()
        val activeTrackIds = status?.activeTrackIds?.toSet().orEmpty()
        val currentJellyfinItemId = customData?.optString("jellyfinItemId")?.takeIf(String::isNotBlank)
        val pendingItem = pendingQueue.firstOrNull { it.itemId.toString() == currentJellyfinItemId }
        val textTracks = pendingItem?.castSubtitleTracks ?: mediaInfo?.mediaTracks.orEmpty()
            .filter { it.type == com.google.android.gms.cast.MediaTrack.TYPE_TEXT }
            .map {
                CastSubtitleTrack(
                    castTrackId = it.id,
                    streamIndex = it.id.toInt(),
                    label = it.name ?: it.language ?: "Subtitle ${it.id}",
                    language = it.language,
                    codec = null,
                    isExternal = true,
                    isDefault = false,
                    contentUrl = it.contentId.orEmpty(),
                    contentType = it.contentType.orEmpty(),
                )
            }
        _state.update {
            it.copy(
                currentItemId = currentJellyfinItemId ?: it.currentItemId,
                currentQueueIndex = queueItems.indexOfFirst { item -> item.itemId == status?.currentItemId }.coerceAtLeast(0),
                positionMs = client.approximateStreamPosition.coerceAtLeast(0L),
                durationMs = client.streamDuration.coerceAtLeast(0L),
                isPlaying = client.isPlaying,
                canSkipNext = queueItems.size > 1,
                canSkipPrevious = queueItems.size > 1,
                repeatMode = status?.queueRepeatMode.toVantafynRepeatMode(),
                subtitleTracks = textTracks,
                activeSubtitleTrackId = activeTrackIds.firstOrNull { id -> textTracks.any { it.castTrackId == id } },
                audioTracks = pendingItem?.castAudioTracks ?: it.audioTracks,
                audioSwitchingSupported = false,
            )
        }
        if (client.mediaStatus?.mediaInfo != null) {
            startPositionTicker()
        } else {
            stopPositionTicker("cast idle")
        }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        LongRunningTaskRegistry.start(
            id = CAST_TICKER_TASK_ID,
            type = LongRunningTaskType.CastReporter,
            owner = "GoogleCastPlaybackTarget",
            state = "active media",
        )
        positionTickerJob = scope.launch {
            while (isActive) {
                val client = remoteClient
                val hasActiveMedia = client?.mediaStatus?.mediaInfo != null
                if (!hasActiveMedia) {
                    stopPositionTicker("cast idle")
                    break
                }
                syncRemoteState()
                LongRunningTaskRegistry.tick(CAST_TICKER_TASK_ID, if (client?.isPlaying == true) "playing" else "paused")
                delay(5_000L)
            }
        }
    }

    private fun stopPositionTicker(reason: String) {
        positionTickerJob?.cancel()
        positionTickerJob = null
        LongRunningTaskRegistry.stop(CAST_TICKER_TASK_ID, reason)
    }

    private fun ensureReachable(item: RemoteQueueItem) {
        if (!CastUrlSecurity.isCastReachableServerAddress(item.streamUrl)) {
            throw CastCommandException(CastError.ServerAddressUnreachable)
        }
        item.artworkUrl?.let {
            if (!CastUrlSecurity.isCastReachableServerAddress(it)) {
                throw CastCommandException(CastError.ServerAddressUnreachable)
            }
        }
        item.castSubtitleTracks.forEach {
            if (!CastUrlSecurity.isCastReachableServerAddress(it.contentUrl)) {
                throw CastCommandException(CastError.ServerAddressUnreachable)
            }
        }
    }

    private fun RemoteQueueItem.toMediaInfo(): MediaInfo {
        val metadata = MediaMetadata(castMetadataType()).apply {
            putString(MediaMetadata.KEY_TITLE, title)
            when (mediaKind) {
                RemoteMediaKind.Music -> {
                    artist?.let { putString(MediaMetadata.KEY_ARTIST, it) }
                    albumTitle?.let { putString(MediaMetadata.KEY_ALBUM_TITLE, it) }
                }
                RemoteMediaKind.Episode -> {
                    seriesTitle?.let { putString(MediaMetadata.KEY_SERIES_TITLE, it) }
                    subtitle?.let { putString(MediaMetadata.KEY_SUBTITLE, it) }
                }
                RemoteMediaKind.Movie,
                RemoteMediaKind.LiveTv,
                RemoteMediaKind.Unknown -> {
                    subtitle?.let { putString(MediaMetadata.KEY_SUBTITLE, it) }
                }
            }
            overview?.let { putString(MediaMetadata.KEY_STUDIO, it.take(120)) }
            listOfNotNull(artworkUrl, backdropUrl)
                .distinct()
                .forEach { addImage(com.google.android.gms.common.images.WebImage(android.net.Uri.parse(it))) }
        }
        val customData = JSONObject()
            .put("jellyfinItemId", itemId.toString())
            .put("queueId", queueId)
            .put("mediaKind", mediaKind.name)
        return MediaInfo.Builder(streamUrl)
            .setStreamType(if (isLive) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setMetadata(metadata)
            .setStreamDuration(if (isLive) 0L else durationMs ?: 0L)
            .setCustomData(customData)
            .setMediaTracks(castSubtitleTracks.map(CastTrackMapper::toMediaTrack))
            .build()
    }

    private fun RemoteQueueItem.castMetadataType(): Int =
        when (mediaKind) {
            RemoteMediaKind.Music -> MediaMetadata.MEDIA_TYPE_MUSIC_TRACK
            RemoteMediaKind.Movie -> MediaMetadata.MEDIA_TYPE_MOVIE
            RemoteMediaKind.Episode -> MediaMetadata.MEDIA_TYPE_TV_SHOW
            RemoteMediaKind.LiveTv -> MediaMetadata.MEDIA_TYPE_GENERIC
            RemoteMediaKind.Unknown -> MediaMetadata.MEDIA_TYPE_GENERIC
        }

    private fun Int?.toVantafynRepeatMode(): VantafynMusicRepeatMode =
        when (this) {
            MediaStatus.REPEAT_MODE_REPEAT_SINGLE -> VantafynMusicRepeatMode.One
            MediaStatus.REPEAT_MODE_REPEAT_ALL,
            MediaStatus.REPEAT_MODE_REPEAT_ALL_AND_SHUFFLE -> VantafynMusicRepeatMode.All
            else -> VantafynMusicRepeatMode.Off
        }

    companion object {
        private const val CAST_TICKER_TASK_ID = "cast.positionTicker"

        @Volatile
        private var instance: GoogleCastPlaybackTarget? = null

        fun get(context: Context): GoogleCastPlaybackTarget =
            instance ?: synchronized(this) {
                instance ?: GoogleCastPlaybackTarget(context).also { instance = it }
            }
    }
}

class CastCommandException(val error: CastError) : RuntimeException(error.name)

private suspend fun <T : Result> PendingResult<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        setResultCallback { result -> continuation.resume(result) }
    }

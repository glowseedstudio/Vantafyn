package dev.vantafyn.core.cast

import dev.vantafyn.core.media.VantafynMusicRepeatMode
import java.util.UUID

object VantafynCastFeatureFlags {
    const val googleCastEnabled: Boolean = true
    const val customCastReceiverEnabled: Boolean = false
}

enum class RemotePlaybackTargetType {
    Local,
    GoogleCast,
    FutureSyncPlay,
    FutureVantafynTV,
}

enum class RemoteConnectionState {
    Unavailable,
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
    Disconnecting,
    Failed,
}

data class RemotePlaybackCapabilities(
    val canPlayPause: Boolean = true,
    val canSeek: Boolean = true,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSetVolume: Boolean = true,
    val canMute: Boolean = true,
    val canQueue: Boolean = true,
    val canShuffle: Boolean = true,
    val canRepeat: Boolean = true,
)

enum class CastPlaybackQuality(val bitrate: Int?) {
    Auto(null),
    Original(null),
    High(12_000_000),
    Medium(5_000_000),
    DataSaver(1_500_000),
}

enum class RemoteMediaKind {
    Music,
    Movie,
    Episode,
    LiveTv,
    Unknown,
}

enum class ReceiverMode {
    Default,
    Custom,
}

data class CastReceiverConfiguration(
    val mode: ReceiverMode,
    val applicationId: String?,
    val supportedNamespaces: Set<String>,
)

enum class CastError {
    CastUnavailable,
    ReceiverDiscoveryUnavailable,
    ReceiverConnectionFailed,
    ReceiverLaunchFailed,
    ServerAddressUnreachable,
    PlaybackInfoFailed,
    NoCompatibleMediaSource,
    AuthenticatedUrlCreationFailed,
    ReceiverLoadFailed,
    RemoteCommandFailed,
    SessionLost,
    UnsupportedOperation,
}

data class RemotePlaybackState(
    val availability: Boolean = false,
    val connectionState: RemoteConnectionState = RemoteConnectionState.Disconnected,
    val receiverName: String? = null,
    val receiverId: String? = null,
    val currentItemId: String? = null,
    val currentQueueIndex: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val volume: Float = 1f,
    val isMuted: Boolean = false,
    val canSeek: Boolean = true,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val repeatMode: VantafynMusicRepeatMode = VantafynMusicRepeatMode.Off,
    val shuffleEnabled: Boolean = false,
    val subtitleTracks: List<CastSubtitleTrack> = emptyList(),
    val activeSubtitleTrackId: Long? = null,
    val audioTracks: List<CastAudioTrack> = emptyList(),
    val audioSwitchingSupported: Boolean = false,
    val lastError: CastError? = null,
    val lastStoppedItemId: String? = null,
    val lastStoppedPositionMs: Long = 0L,
) {
    val connectedLabel: String?
        get() = receiverName?.takeIf { connectionState == RemoteConnectionState.Connected }?.let { "Playing on $it" }
}

data class RemoteQueueItem(
    val itemId: UUID,
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val subtitle: String? = null,
    val seriesTitle: String? = null,
    val overview: String? = null,
    val streamUrl: String,
    val artworkUrl: String?,
    val backdropUrl: String? = null,
    val durationMs: Long?,
    val contentType: String = "audio/mpeg",
    val mediaKind: RemoteMediaKind = RemoteMediaKind.Music,
    val isLive: Boolean = false,
    val queueId: Long = itemId.mostSignificantBits xor itemId.leastSignificantBits,
    val castSubtitleTracks: List<CastSubtitleTrack> = emptyList(),
    val castAudioTracks: List<CastAudioTrack> = emptyList(),
    val activeSubtitleTrackId: Long? = null,
)

data class RemotePlaybackRequest(
    val item: RemoteQueueItem,
    val startPositionMs: Long,
    val autoplay: Boolean = true,
)

interface RemotePlaybackTarget {
    val id: String
    val name: String
    val type: RemotePlaybackTargetType
    val connectionState: RemoteConnectionState
    val capabilities: RemotePlaybackCapabilities

    suspend fun connect()
    suspend fun disconnect(stopPlayback: Boolean)
    suspend fun load(request: RemotePlaybackRequest)
    suspend fun play()
    suspend fun pause()
    suspend fun seek(positionMs: Long)
    suspend fun skipNext()
    suspend fun skipPrevious()
    suspend fun setVolume(volume: Float)
    suspend fun setMuted(muted: Boolean)
    suspend fun selectSubtitleTrack(trackId: Long?)
    suspend fun replaceQueue(
        queue: List<RemoteQueueItem>,
        startIndex: Int,
        startPositionMs: Long,
    )
}

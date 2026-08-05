package dev.vantafyn.core.media

data class VantafynPlaybackItem(
    val itemId: String,
    val title: String,
    val subtitle: String? = null,
    val streamUrl: String,
    val fallbackStreamUrl: String? = null,
    val startPositionMs: Long = 0L,
    val durationMs: Long? = null,
    val sourceLabel: String? = null,
    val audioTracks: List<VantafynAudioTrack> = emptyList(),
    val subtitleTracks: List<VantafynSubtitleTrack> = emptyList(),
)

data class PlaybackRequest(
    val itemId: String,
    val title: String,
    val startPositionMs: Long = 0L,
)

enum class PlaybackEngine {
    Media3ExoPlayer,
}

sealed interface VantafynPlaybackState {
    data object Loading : VantafynPlaybackState
    data object Playing : VantafynPlaybackState
    data object Paused : VantafynPlaybackState
    data class Error(val message: String, val canTryTranscode: Boolean) : VantafynPlaybackState
}

data class VantafynPlaybackError(
    val message: String,
    val canTryTranscode: Boolean = false,
)

interface VantafynTrackInfo {
    val index: Int
    val label: String
    val language: String?
    val codec: String?
}

data class VantafynAudioTrack(
    override val index: Int,
    override val label: String,
    override val language: String?,
    override val codec: String?,
    val channels: Int?,
    val isDefault: Boolean,
) : VantafynTrackInfo

data class VantafynSubtitleTrack(
    override val index: Int,
    override val label: String,
    override val language: String?,
    override val codec: String?,
    val isExternal: Boolean,
    val isDefault: Boolean,
) : VantafynTrackInfo

interface VantafynPlaybackController {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
}

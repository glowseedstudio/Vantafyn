package dev.vantafyn.core.media

data class UpNextCandidate(
    val itemId: String,
    val seriesId: String?,
    val seasonId: String?,
    val title: String,
    val seriesName: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val runtimeMs: Long?,
    val imageUrl: String?,
    val backdropUrl: String?,
    val overview: String?,
    val progress: Float? = null,
    val playbackPositionMs: Long = 0L,
) {
    val episodeLabel: String
        get() = listOfNotNull(
            seasonNumber?.let { "S$it" },
            episodeNumber?.let { "E$it" },
        ).joinToString(" ").ifBlank { "Next episode" }
}

sealed interface UpNextState {
    data object Hidden : UpNextState
    data object Loading : UpNextState
    data class Available(
        val candidate: UpNextCandidate,
        val countdownSeconds: Int,
        val autoplayEnabled: Boolean,
    ) : UpNextState
    data class Cancelled(val candidate: UpNextCandidate) : UpNextState
    data object PlayingNext : UpNextState
    data class Unavailable(val reason: String) : UpNextState
}

data class AutoplaySettings(
    val enabled: Boolean = true,
    val countdownSeconds: Int = 10,
    val passoutProtectionEnabled: Boolean = false,
    val passoutProtectionLimitMinutes: Int = 180,
    val showBeforeEndSeconds: Int = 45,
    val showBeforeEndPercent: Float = 0.94f,
    val onlyForEpisodes: Boolean = true,
    val skipIfFinalEpisode: Boolean = true,
    val playNextOnCompletion: Boolean = true,
)

data class VantafynPlaybackItem(
    val itemId: String,
    val title: String,
    val subtitle: String? = null,
    val streamUrl: String,
    val fallbackStreamUrl: String? = null,
    val startPositionMs: Long = 0L,
    val durationMs: Long? = null,
    val sourceLabel: String? = null,
    val selectedAudioStreamIndex: Int? = null,
    val selectedSubtitleStreamIndex: Int? = null,
    val audioTracks: List<VantafynAudioTrack> = emptyList(),
    val subtitleTracks: List<VantafynSubtitleTrack> = emptyList(),
    val itemType: String? = null,
    val isLiveStream: Boolean = false,
    val isCastResolved: Boolean = false,
    val upNextCandidate: UpNextCandidate? = null,
    val autoplaySettings: AutoplaySettings = AutoplaySettings(),
    val continuousPlaybackStartedAtMs: Long = System.currentTimeMillis(),
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
    val deliveryUrl: String? = null,
) : VantafynTrackInfo

interface VantafynPlaybackController {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
}

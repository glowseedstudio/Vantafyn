package dev.vantafyn.core.media

data class PlaybackRequest(
    val itemId: String,
    val title: String,
    val startPositionMs: Long = 0L,
)

enum class PlaybackEngine {
    Media3ExoPlayer,
}

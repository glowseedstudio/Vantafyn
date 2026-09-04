package dev.vantafyn.feature.music.harmonia

import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.media.VantafynMusicStopReason
import dev.vantafyn.core.media.VantafynMusicTrack
import java.time.Clock
import java.time.Instant

class HarmoniaPlaybackTracker(
    private val historyRepository: HarmoniaHistoryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private var active: ActivePlayback? = null

    suspend fun onTrackStarted(session: JellyfinSession, track: VantafynMusicTrack, positionMs: Long) {
        val current = active
        if (current?.track?.id == track.id && current.session.profileId == session.profileId) return
        active = ActivePlayback(session, track, clock.instant(), positionMs.coerceAtLeast(0L))
    }

    suspend fun onTrackStopped(session: JellyfinSession, track: VantafynMusicTrack, positionMs: Long, reason: VantafynMusicStopReason) {
        val current = active?.takeIf { it.track.id == track.id && it.session.profileId == session.profileId }
        val endedAt = clock.instant()
        val start = current?.startedAt ?: endedAt
        val startPosition = current?.startPositionMs ?: 0L
        val rawListened = positionMs.coerceAtLeast(0L) - startPosition
        val listenedMs = when {
            rawListened > 0L -> rawListened
            reason == VantafynMusicStopReason.Ended && track.durationMs != null -> track.durationMs
            else -> 0L
        }?.coerceAtLeast(0L) ?: 0L
        if (listenedMs >= MIN_TRACK_LISTEN_MS) {
            historyRepository.add(
                HarmoniaPlaybackRecord(
                    id = "${session.server.localId}:${session.profileId}:${track.id}:${start.toEpochMilli()}",
                    userId = session.user.id,
                    serverId = session.server.localId,
                    profileId = session.profileId,
                    trackId = track.id,
                    trackTitle = track.title,
                    artist = track.artist,
                    album = track.album,
                    albumId = track.albumId,
                    genres = track.genres,
                    startedAt = start,
                    endedAt = endedAt,
                    listenedMs = listenedMs,
                    durationMs = track.durationMs,
                ),
            )
        }
        if (current != null) active = null
    }

    private data class ActivePlayback(
        val session: JellyfinSession,
        val track: VantafynMusicTrack,
        val startedAt: Instant,
        val startPositionMs: Long,
    )
}

private const val MIN_TRACK_LISTEN_MS = 15_000L


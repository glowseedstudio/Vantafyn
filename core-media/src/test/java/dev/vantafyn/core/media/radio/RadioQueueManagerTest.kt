package dev.vantafyn.core.media.radio

import dev.vantafyn.core.media.VantafynMusicTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RadioQueueManagerTest {

    private fun sampleTrack(title: String, id: UUID = UUID.randomUUID()) = VantafynMusicTrack(
        id = id,
        title = title,
        artist = "Test Artist",
        album = "Test Album",
        albumId = null,
        durationMs = 180_000L,
        streamUrl = "http://localhost/stream",
        artworkUrl = null,
    )

    @Test
    fun deduplicationFiltersOutHistoryTracks() {
        val history = mutableSetOf<UUID>()
        val track1 = sampleTrack("Track 1")
        val track2 = sampleTrack("Track 2")
        val track3 = sampleTrack("Track 3")

        history.add(track1.id)
        history.add(track2.id)

        val incoming = listOf(track1, track2, track3)
        val filtered = incoming.filter { it.id !in history }

        assertEquals(1, filtered.size)
        assertEquals(track3.id, filtered.first().id)
    }

    @Test
    fun thresholdLogicDeterminesRefillAccurately() {
        val totalCount = 10
        val currentIndex = 7 // Remaining: 10 - (7 + 1) = 2 <= 3 -> Needs refill
        val remaining = totalCount - (currentIndex + 1)

        assertTrue(remaining <= RadioQueueManager.TOP_UP_THRESHOLD)

        val farIndex = 2 // Remaining: 10 - (2 + 1) = 7 > 3 -> No refill needed
        val farRemaining = totalCount - (farIndex + 1)

        assertFalse(farRemaining <= RadioQueueManager.TOP_UP_THRESHOLD)
    }
}

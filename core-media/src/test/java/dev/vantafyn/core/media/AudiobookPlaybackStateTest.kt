package dev.vantafyn.core.media

import dev.vantafyn.core.jellyfin.JellyfinChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobookPlaybackStateTest {

    @Test
    fun resolvesCurrentChapterBasedOnPlaybackPosition() {
        val chapters = listOf(
            JellyfinChapter("ch-1", "Chapter 1: Prologue", startPositionMs = 0L, durationMs = 300_000L),
            JellyfinChapter("ch-2", "Chapter 2: The Departure", startPositionMs = 300_000L, durationMs = 500_000L),
            JellyfinChapter("ch-3", "Chapter 3: The Arrival", startPositionMs = 800_000L, durationMs = 400_000L),
        )

        val stateAtPrologue = VantafynMusicPlaybackState(
            isAudiobookMode = true,
            activeChapters = chapters,
            positionMs = 150_000L,
            playbackSpeed = 1.25f,
        )

        assertTrue(stateAtPrologue.isAudiobookMode)
        assertEquals(1.25f, stateAtPrologue.playbackSpeed)
        assertNotNull(stateAtPrologue.currentChapter)
        assertEquals("Chapter 1: Prologue", stateAtPrologue.currentChapter?.name)

        val stateAtDeparture = stateAtPrologue.copy(positionMs = 450_000L)
        assertEquals("Chapter 2: The Departure", stateAtDeparture.currentChapter?.name)

        val stateAtArrival = stateAtPrologue.copy(positionMs = 900_000L)
        assertEquals("Chapter 3: The Arrival", stateAtArrival.currentChapter?.name)
    }

    @Test
    fun returnsNullWhenNoChaptersOrEmpty() {
        val state = VantafynMusicPlaybackState(
            isAudiobookMode = true,
            activeChapters = emptyList(),
            positionMs = 100_000L,
        )
        assertNull(state.currentChapter)
    }
}

package dev.vantafyn.core.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AudiobookModelTest {

    private fun createMediaItem(
        title: String,
        itemType: String?,
        isAudiobook: Boolean = false,
    ) = JellyfinMediaItem(
        id = UUID.randomUUID(),
        title = title,
        subtitle = null,
        year = 2024,
        itemType = itemType,
        imageUrl = null,
        backdropUrl = null,
        logoUrl = null,
        progress = null,
        shape = JellyfinMediaCardShape.Poster,
        isAudiobook = isAudiobook,
    )

    private fun createMediaDetail(
        title: String,
        itemType: String?,
        author: String? = null,
        narrator: String? = null,
        chapters: List<JellyfinChapter> = emptyList(),
        isAudiobook: Boolean = false,
    ) = JellyfinMediaDetail(
        id = UUID.randomUUID(),
        title = title,
        subtitle = null,
        year = 2024,
        runtimeMinutes = 420,
        officialRating = null,
        communityRating = null,
        overview = "Audiobook test",
        genres = listOf("Audiobook", "Sci-Fi"),
        itemType = itemType,
        imageUrl = null,
        backdropUrl = null,
        logoUrl = null,
        isFavorite = false,
        isPlayed = false,
        progress = null,
        author = author,
        narrator = narrator,
        chapters = chapters,
        isAudiobook = isAudiobook,
    )

    @Test
    fun identifiesAudiobookMediaItemCorrectly() {
        val regularTrack = createMediaItem(title = "Bohemian Rhapsody", itemType = "Audio", isAudiobook = false)
        assertFalse(regularTrack.isAudiobookMedia)

        val audiobookByType = createMediaItem(title = "Dune", itemType = "AudioBook", isAudiobook = false)
        assertTrue(audiobookByType.isAudiobookMedia)

        val bookByType = createMediaItem(title = "Foundation", itemType = "Book", isAudiobook = false)
        assertTrue(bookByType.isAudiobookMedia)

        val flaggedAudiobook = createMediaItem(title = "Project Hail Mary", itemType = "Audio", isAudiobook = true)
        assertTrue(flaggedAudiobook.isAudiobookMedia)
    }

    @Test
    fun verifiesDetailWithChaptersAndMetadata() {
        val chapters = listOf(
            JellyfinChapter("ch-1", "Chapter 1: The Beginning", startPositionMs = 0L, durationMs = 600_000L),
            JellyfinChapter("ch-2", "Chapter 2: The Journey", startPositionMs = 600_000L, durationMs = 900_000L),
            JellyfinChapter("ch-3", "Chapter 3: The Climax", startPositionMs = 1_500_000L, durationMs = 1_200_000L),
        )

        val detail = createMediaDetail(
            title = "The Way of Kings",
            itemType = "AudioBook",
            author = "Brandon Sanderson",
            narrator = "Michael Kramer, Kate Reading",
            chapters = chapters,
            isAudiobook = true,
        )

        assertTrue(detail.isAudiobookMedia)
        assertEquals("Brandon Sanderson", detail.author)
        assertEquals("Michael Kramer, Kate Reading", detail.narrator)
        assertEquals(3, detail.chapters.size)
        assertEquals(0L, detail.chapters[0].startPositionMs)
        assertEquals(600_000L, detail.chapters[0].durationMs)
        assertEquals(600_000L, detail.chapters[1].startPositionMs)
        assertEquals("Chapter 3: The Climax", detail.chapters[2].name)
    }

    @Test
    fun identifiesBookFolderCorrectly() {
        val bookFolderItem = createMediaItem(
            title = "Book 01 - Harry Potter and the Philosopher's Stone",
            itemType = "AudioBook",
            isAudiobook = true,
        )
        assertTrue(bookFolderItem.isAudiobookMedia)

        val bookFolderDetail = createMediaDetail(
            title = "Book 01 - Harry Potter and the Philosopher's Stone",
            itemType = "Folder",
            chapters = listOf(
                JellyfinChapter("ch-1", "Chapter 01 - The Boy Who Lived", startPositionMs = 0L, durationMs = 1_800_000L),
            ),
            isAudiobook = true,
        )
        assertTrue(bookFolderDetail.isAudiobookMedia)
    }
}

package dev.vantafyn.feature.home.saw

import dev.vantafyn.feature.home.saw.SawMoviesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SawWatchOrderTest {

    @Test
    fun sawMoviesCatalog_hasValidSequence() {
        val movies = SawMoviesCatalog.movies
        assertTrue(movies.isNotEmpty())
        assertEquals(10, movies.size)

        // Unique IDs
        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        // Ensure release orders are sequential from 1 to 10
        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..10).toList(), releaseOrders)

        // Ensure chronological timeline orders are sequential from 1 to 10
        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..10).toList(), timelineOrders)

        // First movie in timeline is Saw (2004)
        val firstMovie = movies.first { it.timelineOrder == 1 }
        assertEquals("Saw", firstMovie.title)
        assertEquals(2004, firstMovie.releaseYear)

        // Saw X (2023) is releaseOrder 10 but timelineOrder 2
        val sawX = movies.first { it.title == "Saw X" }
        assertEquals(10, sawX.releaseOrder)
        assertEquals(2, sawX.timelineOrder)
        assertEquals(2023, sawX.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("saw", SawMoviesCatalog.normalizeTitle("Saw"))
        assertEquals("sawii", SawMoviesCatalog.normalizeTitle("Saw II"))
        assertEquals("saw3d", SawMoviesCatalog.normalizeTitle("Saw 3D"))
        assertEquals("jigsaw", SawMoviesCatalog.normalizeTitle("Jigsaw"))
        assertEquals("spiralfromthebookofsaw", SawMoviesCatalog.normalizeTitle("Spiral: From the Book of Saw"))
        assertEquals("sawx", SawMoviesCatalog.normalizeTitle("Saw X"))
    }

    @Test
    fun sawMatching_byTmdbOrImdbOrNormalizedTitle() {
        val saw = SawMoviesCatalog.movies.first { it.title == "Saw" }
        assertEquals(176, saw.tmdbId)
        assertEquals("tt0387564", saw.imdbId)
        assertTrue(saw.fallbackPosterUrl.contains("image.tmdb.org"))

        val jigsaw = SawMoviesCatalog.movies.first { it.title == "Jigsaw" }
        assertEquals(298250, jigsaw.tmdbId)
        assertEquals("tt4983590", jigsaw.imdbId)
        assertTrue(jigsaw.fallbackPosterUrl.contains("image.tmdb.org"))

        val sawX = SawMoviesCatalog.movies.first { it.title == "Saw X" }
        assertEquals(951491, sawX.tmdbId)
        assertEquals("tt17009710", sawX.imdbId)
        assertTrue(sawX.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

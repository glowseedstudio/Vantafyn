package dev.vantafyn.feature.home.mcu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McuWatchOrderTest {

    @Test
    fun mcuMoviesCatalog_hasValidSequence() {
        val movies = McuMoviesCatalog.movies
        assertTrue(movies.isNotEmpty())
        assertEquals(62, movies.size)

        // Unique IDs
        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        // Ensure chronological timeline orders are sequential from 1 to 62
        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..62).toList(), timelineOrders)

        // Ensure theatrical release orders are sequential from 1 to 62
        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..62).toList(), releaseOrders)

        // First movie in timeline is Captain America: The First Avenger (1942)
        assertEquals("Captain America: The First Avenger", movies.first { it.timelineOrder == 1 }.title)

        // First movie in release order is X-Men (2000)
        assertEquals("X-Men", movies.first { it.releaseOrder == 1 }.title)

        // Last movie in timeline and release order is Avengers: Doomsday (2026)
        assertEquals("Avengers: Doomsday", movies.first { it.timelineOrder == 62 }.title)
        assertEquals("Avengers: Doomsday", movies.first { it.releaseOrder == 62 }.title)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("captainamericathefirstavenger", McuMoviesCatalog.normalizeTitle("Captain America: The First Avenger"))
        assertEquals("theavengers", McuMoviesCatalog.normalizeTitle("Marvel's The Avengers"))
        assertEquals("theavengers", McuMoviesCatalog.normalizeTitle("The Avengers"))
        assertEquals("ironman2", McuMoviesCatalog.normalizeTitle("Iron Man 2"))
        assertEquals("spidermannowayhome", McuMoviesCatalog.normalizeTitle("Spider-Man: No Way Home"))
        assertEquals("deadpoolwolverine", McuMoviesCatalog.normalizeTitle("Deadpool & Wolverine"))
        assertEquals("thunderbolts", McuMoviesCatalog.normalizeTitle("Thunderbolts*"))
        // Multiverse additions
        assertEquals("x2", McuMoviesCatalog.normalizeTitle("X2"))
        assertEquals("fantasticfour", McuMoviesCatalog.normalizeTitle("Fantastic Four"))
        assertEquals("theamazingspiderman", McuMoviesCatalog.normalizeTitle("The Amazing Spider-Man"))
        assertEquals("darkphoenix", McuMoviesCatalog.normalizeTitle("Dark Phoenix"))
    }

    @Test
    fun mcuMatching_byTmdbOrImdbOrNormalizedTitle() {
        val doomsday = McuMoviesCatalog.movies.first { it.title == "Avengers: Doomsday" }
        assertEquals(1003596, doomsday.tmdbId)
        assertEquals("tt21361450", doomsday.imdbId)

        val cap1 = McuMoviesCatalog.movies.first { it.title == "Captain America: The First Avenger" }
        assertEquals(1771, cap1.tmdbId)
        assertTrue(cap1.fallbackPosterUrl.contains("image.tmdb.org"))

        val f4 = McuMoviesCatalog.movies.first { it.id == "fantastic_four_2005" }
        assertEquals(9738, f4.tmdbId)
        assertEquals("tt0120667", f4.imdbId)
        assertEquals("Fantastic Four", f4.phase)
        assertTrue(f4.fallbackPosterUrl.contains("image.tmdb.org"))

        val xmen = McuMoviesCatalog.movies.first { it.id == "x_men_2000" }
        assertEquals(36657, xmen.tmdbId)
        assertEquals("tt0120903", xmen.imdbId)
        assertEquals("X-Men Universe", xmen.phase)

        val firstSteps = McuMoviesCatalog.movies.first { it.id == "fantastic_four_first_steps" }
        assertEquals(617126, firstSteps.tmdbId)
        assertEquals("tt10676052", firstSteps.imdbId)
        assertEquals(2025, firstSteps.releaseYear)
        assertEquals("The Fantastic Four: First Steps", firstSteps.title)
        assertTrue(firstSteps.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

package dev.vantafyn.feature.home.residentevil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResidentEvilWatchOrderTest {

    @Test
    fun residentEvilMoviesCatalog_hasValidSequence() {
        val movies = ResidentEvilMoviesCatalog.movies
        assertTrue(movies.isNotEmpty())
        assertEquals(6, movies.size)

        // Unique IDs
        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        // Ensure release orders are sequential from 1 to 6
        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..6).toList(), releaseOrders)

        // Ensure chronological timeline orders are sequential from 1 to 6
        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..6).toList(), timelineOrders)

        // First movie is Resident Evil (2002)
        val firstMovie = movies.first { it.releaseOrder == 1 }
        assertEquals("Resident Evil", firstMovie.title)
        assertEquals(2002, firstMovie.releaseYear)

        // Last movie is Resident Evil: The Final Chapter (2016)
        val lastMovie = movies.first { it.releaseOrder == 6 }
        assertEquals("Resident Evil: The Final Chapter", lastMovie.title)
        assertEquals(2016, lastMovie.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("residentevil", ResidentEvilMoviesCatalog.normalizeTitle("Resident Evil"))
        assertEquals("residentevilapocalypse", ResidentEvilMoviesCatalog.normalizeTitle("Resident Evil: Apocalypse"))
        assertEquals("residentevilextinction", ResidentEvilMoviesCatalog.normalizeTitle("Resident Evil: Extinction"))
        assertEquals("residentevilafterlife", ResidentEvilMoviesCatalog.normalizeTitle("Resident Evil: Afterlife"))
        assertEquals("residentevilretribution", ResidentEvilMoviesCatalog.normalizeTitle("Resident Evil: Retribution"))
        assertEquals("residentevilthefinalchapter", ResidentEvilMoviesCatalog.normalizeTitle("Resident Evil: The Final Chapter"))
    }

    @Test
    fun residentEvilMatching_byTmdbOrImdbOrNormalizedTitle() {
        val re1 = ResidentEvilMoviesCatalog.movies.first { it.title == "Resident Evil" }
        assertEquals(1576, re1.tmdbId)
        assertEquals("tt0120804", re1.imdbId)
        assertTrue(re1.fallbackPosterUrl.contains("image.tmdb.org"))

        val reFinal = ResidentEvilMoviesCatalog.movies.first { it.title == "Resident Evil: The Final Chapter" }
        assertEquals(173897, reFinal.tmdbId)
        assertEquals("tt2592614", reFinal.imdbId)
        assertTrue(reFinal.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

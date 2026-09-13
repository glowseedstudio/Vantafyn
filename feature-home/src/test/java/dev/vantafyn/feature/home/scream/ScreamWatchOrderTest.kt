package dev.vantafyn.feature.home.scream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreamWatchOrderTest {

    @Test
    fun screamMoviesCatalog_hasValidSequence() {
        val movies = ScreamMoviesCatalog.movies
        assertEquals(7, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..7).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..7).toList(), timelineOrders)

        val firstMovie = movies.first { it.releaseOrder == 1 }
        assertEquals("Scream", firstMovie.title)
        assertEquals(1996, firstMovie.releaseYear)

        val seventhMovie = movies.first { it.releaseOrder == 7 }
        assertEquals("Scream 7", seventhMovie.title)
        assertEquals(2026, seventhMovie.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("scream", ScreamMoviesCatalog.normalizeTitle("Scream"))
        assertEquals("scream2", ScreamMoviesCatalog.normalizeTitle("Scream 2"))
        assertEquals("scream3", ScreamMoviesCatalog.normalizeTitle("Scream 3"))
        assertEquals("scream4", ScreamMoviesCatalog.normalizeTitle("Scream 4"))
        assertEquals("screamvi", ScreamMoviesCatalog.normalizeTitle("Scream VI"))
        assertEquals("scream7", ScreamMoviesCatalog.normalizeTitle("Scream 7"))
    }

    @Test
    fun screamMatching_byTmdbOrImdb() {
        val s1 = ScreamMoviesCatalog.movies.first { it.id == "scream_1996" }
        assertEquals(4232, s1.tmdbId)
        assertEquals("tt0120082", s1.imdbId)
        assertTrue(s1.fallbackPosterUrl.contains("image.tmdb.org"))

        val s6 = ScreamMoviesCatalog.movies.first { it.id == "scream_vi_2023" }
        assertEquals(934433, s6.tmdbId)
        assertEquals("tt17663992", s6.imdbId)
        assertTrue(s6.fallbackPosterUrl.contains("image.tmdb.org"))

        val s7 = ScreamMoviesCatalog.movies.first { it.id == "scream_7_2026" }
        assertEquals(1159559, s7.tmdbId)
        assertEquals("tt27047903", s7.imdbId)
        assertTrue(s7.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

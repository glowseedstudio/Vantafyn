package dev.vantafyn.feature.home.scary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaryMovieWatchOrderTest {

    @Test
    fun scaryMovieCatalog_hasValidSequence() {
        val movies = ScaryMovieMoviesCatalog.movies
        assertEquals(6, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..6).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..6).toList(), timelineOrders)

        val first = movies.first { it.releaseOrder == 1 }
        assertEquals("Scary Movie", first.title)
        assertEquals(2000, first.releaseYear)

        val fifth = movies.first { it.releaseOrder == 5 }
        assertEquals("Scary Movie 5", fifth.title)
        assertEquals(2013, fifth.releaseYear)

        val sixth = movies.first { it.releaseOrder == 6 }
        assertEquals("Scary Movie 6", sixth.title)
        assertEquals(2026, sixth.releaseYear)
        assertEquals(1273221, sixth.tmdbId)
        assertEquals("tt32093575", sixth.imdbId)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("scarymovie", ScaryMovieMoviesCatalog.normalizeTitle("Scary Movie"))
        assertEquals("scarymovie2", ScaryMovieMoviesCatalog.normalizeTitle("Scary Movie 2"))
        assertEquals("scarymovie3", ScaryMovieMoviesCatalog.normalizeTitle("Scary Movie 3"))
        assertEquals("scarymovie4", ScaryMovieMoviesCatalog.normalizeTitle("Scary Movie 4"))
        assertEquals("scarymovie5", ScaryMovieMoviesCatalog.normalizeTitle("Scary Movie 5"))
        assertEquals("scarymovie6", ScaryMovieMoviesCatalog.normalizeTitle("Scary Movie 6"))
    }

    @Test
    fun scaryMovieMatching_byTmdbOrImdb() {
        val first = ScaryMovieMoviesCatalog.movies.first { it.releaseOrder == 1 }
        assertEquals(4247, first.tmdbId)
        assertEquals("tt0175142", first.imdbId)
        assertTrue(first.fallbackPosterUrl.contains("image.tmdb.org"))

        val sixth = ScaryMovieMoviesCatalog.movies.first { it.releaseOrder == 6 }
        assertEquals(1273221, sixth.tmdbId)
        assertEquals("tt32093575", sixth.imdbId)
        assertTrue(sixth.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

package dev.vantafyn.feature.home.underworld

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnderworldWatchOrderTest {

    @Test
    fun underworldCatalog_hasValidSequence() {
        val movies = UnderworldMoviesCatalog.movies
        assertEquals(5, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..5).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..5).toList(), timelineOrders)

        // Prequel is timeline #1, release #3
        val prequel = movies.first { it.timelineOrder == 1 }
        assertEquals("Underworld: Rise of the Lycans", prequel.title)
        assertEquals(3, prequel.releaseOrder)
        assertEquals(2009, prequel.releaseYear)

        // Original film is release #1, timeline #2
        val firstRelease = movies.first { it.releaseOrder == 1 }
        assertEquals("Underworld", firstRelease.title)
        assertEquals(2, firstRelease.timelineOrder)
        assertEquals(2003, firstRelease.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("underworld", UnderworldMoviesCatalog.normalizeTitle("Underworld"))
        assertEquals("underworldevolution", UnderworldMoviesCatalog.normalizeTitle("Underworld: Evolution"))
        assertEquals("underworldriseofthelycans", UnderworldMoviesCatalog.normalizeTitle("Underworld: Rise of the Lycans"))
        assertEquals("underworldawakening", UnderworldMoviesCatalog.normalizeTitle("Underworld: Awakening"))
        assertEquals("underworldbloodwars", UnderworldMoviesCatalog.normalizeTitle("Underworld: Blood Wars"))
    }

    @Test
    fun underworldMatching_byTmdbOrImdb() {
        val first = UnderworldMoviesCatalog.movies.first { it.releaseOrder == 1 }
        assertEquals(277, first.tmdbId)
        assertEquals("tt0320691", first.imdbId)
        assertTrue(first.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

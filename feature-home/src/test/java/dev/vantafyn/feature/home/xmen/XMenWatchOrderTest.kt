package dev.vantafyn.feature.home.xmen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XMenWatchOrderTest {

    @Test
    fun xmenCatalog_hasValidSequence() {
        val movies = XMenMoviesCatalog.movies
        assertEquals(13, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..13).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..13).toList(), timelineOrders)

        // First Class is timeline #1, release #5
        val firstClass = movies.first { it.timelineOrder == 1 }
        assertEquals("X-Men: First Class", firstClass.title)
        assertEquals(5, firstClass.releaseOrder)
        assertEquals(2011, firstClass.releaseYear)

        // Original film is release #1, timeline #6
        val firstRelease = movies.first { it.releaseOrder == 1 }
        assertEquals("X-Men", firstRelease.title)
        assertEquals(6, firstRelease.timelineOrder)
        assertEquals(2000, firstRelease.releaseYear)

        // Logan is timeline #13
        val logan = movies.first { it.timelineOrder == 13 }
        assertEquals("Logan", logan.title)
        assertEquals(2017, logan.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("xmen", XMenMoviesCatalog.normalizeTitle("X-Men"))
        assertEquals("x2xmenunited", XMenMoviesCatalog.normalizeTitle("X2: X-Men United"))
        assertEquals("xmenfirstclass", XMenMoviesCatalog.normalizeTitle("X-Men: First Class"))
        assertEquals("deadpool", XMenMoviesCatalog.normalizeTitle("Deadpool"))
        assertEquals("logan", XMenMoviesCatalog.normalizeTitle("Logan"))
    }

    @Test
    fun xmenMatching_byTmdbOrImdb() {
        val first = XMenMoviesCatalog.movies.first { it.releaseOrder == 1 }
        assertEquals(36657, first.tmdbId)
        assertEquals("tt0120903", first.imdbId)
        assertTrue(first.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

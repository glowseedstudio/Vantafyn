package dev.vantafyn.feature.home.mordor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiddleEarthWatchOrderTest {

    @Test
    fun middleEarthCatalog_hasValidSequence() {
        val movies = MiddleEarthMoviesCatalog.movies
        assertEquals(7, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..7).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..7).toList(), timelineOrders)

        // War of the Rohirrim is timeline #1, release #7
        val rohirrim = movies.first { it.timelineOrder == 1 }
        assertEquals("The Lord of the Rings: The War of the Rohirrim", rohirrim.title)
        assertEquals(7, rohirrim.releaseOrder)
        assertEquals(2024, rohirrim.releaseYear)

        // The Hobbit 1 is timeline #2, release #4
        val hobbit1 = movies.first { it.timelineOrder == 2 }
        assertEquals("The Hobbit: An Unexpected Journey", hobbit1.title)
        assertEquals(4, hobbit1.releaseOrder)
        assertEquals(2012, hobbit1.releaseYear)

        // LOTR 1 is release #1, timeline #5
        val lotr1 = movies.first { it.releaseOrder == 1 }
        assertEquals("The Lord of the Rings: The Fellowship of the Ring", lotr1.title)
        assertEquals(5, lotr1.timelineOrder)
        assertEquals(2001, lotr1.releaseYear)

        // LOTR 3 is timeline #7, release #3
        val lotr3 = movies.first { it.timelineOrder == 7 }
        assertEquals("The Lord of the Rings: The Return of the King", lotr3.title)
        assertEquals(3, lotr3.releaseOrder)
        assertEquals(2003, lotr3.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("thelordoftheringsthefellowshipofthering", MiddleEarthMoviesCatalog.normalizeTitle("The Lord of the Rings: The Fellowship of the Ring"))
        assertEquals("thehobbitanunexpectedjourney", MiddleEarthMoviesCatalog.normalizeTitle("The Hobbit: An Unexpected Journey"))
        assertEquals("thelordoftheringsthewaroftherohirrim", MiddleEarthMoviesCatalog.normalizeTitle("The Lord of the Rings: The War of the Rohirrim"))
    }

    @Test
    fun middleEarthMatching_byTmdbOrImdb() {
        val firstRelease = MiddleEarthMoviesCatalog.movies.first { it.releaseOrder == 1 }
        assertEquals(120, firstRelease.tmdbId)
        assertEquals("tt0120737", firstRelease.imdbId)
        assertTrue(firstRelease.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

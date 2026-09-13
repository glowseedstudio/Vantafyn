package dev.vantafyn.feature.home.twilight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TwilightWatchOrderTest {

    @Test
    fun twilightCatalog_hasValidSequence() {
        val movies = TwilightMoviesCatalog.movies
        assertEquals(5, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..5).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..5).toList(), timelineOrders)

        val first = movies.first { it.releaseOrder == 1 }
        assertEquals("Twilight", first.title)
        assertEquals(2008, first.releaseYear)

        val last = movies.first { it.releaseOrder == 5 }
        assertEquals("The Twilight Saga: Breaking Dawn – Part 2", last.title)
        assertEquals(2012, last.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("twilight", TwilightMoviesCatalog.normalizeTitle("Twilight"))
        assertEquals("thetwilightsaganewmoon", TwilightMoviesCatalog.normalizeTitle("The Twilight Saga: New Moon"))
        assertEquals("thetwilightsagabreakingdawnpart1", TwilightMoviesCatalog.normalizeTitle("The Twilight Saga: Breaking Dawn – Part 1"))
        assertEquals("thetwilightsagabreakingdawnpart2", TwilightMoviesCatalog.normalizeTitle("The Twilight Saga: Breaking Dawn – Part 2"))
    }

    @Test
    fun twilightMatching_byTmdbOrImdb() {
        val first = TwilightMoviesCatalog.movies.first { it.releaseOrder == 1 }
        assertEquals(8966, first.tmdbId)
        assertEquals("tt1099212", first.imdbId)
        assertTrue(first.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

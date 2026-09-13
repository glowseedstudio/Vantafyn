package dev.vantafyn.feature.home.jurassic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JurassicWatchOrderTest {

    @Test
    fun jurassicMoviesCatalog_hasValidSequence() {
        val movies = JurassicMoviesCatalog.movies
        assertEquals(7, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..7).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..7).toList(), timelineOrders)

        assertEquals("Jurassic Park", movies.first { it.releaseOrder == 1 }.title)
        assertEquals("Jurassic World Rebirth", movies.first { it.releaseOrder == 7 }.title)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("jurassicpark", JurassicMoviesCatalog.normalizeTitle("Jurassic Park"))
        assertEquals("thelostworldjurassicpark", JurassicMoviesCatalog.normalizeTitle("The Lost World: Jurassic Park"))
        assertEquals("jurassicparkiii", JurassicMoviesCatalog.normalizeTitle("Jurassic Park III"))
        assertEquals("jurassicworld", JurassicMoviesCatalog.normalizeTitle("Jurassic World"))
        assertEquals("jurassicworldfallenkingdom", JurassicMoviesCatalog.normalizeTitle("Jurassic World: Fallen Kingdom"))
        assertEquals("jurassicworlddominion", JurassicMoviesCatalog.normalizeTitle("Jurassic World Dominion"))
        assertEquals("jurassicworldrebirth", JurassicMoviesCatalog.normalizeTitle("Jurassic World Rebirth"))
    }

    @Test
    fun jurassicMatching_byTmdbOrImdb() {
        val jp1 = JurassicMoviesCatalog.movies.first { it.id == "jurassic_park_1993" }
        assertEquals(329, jp1.tmdbId)
        assertEquals("tt0107290", jp1.imdbId)
        assertTrue(jp1.fallbackPosterUrl.contains("image.tmdb.org"))

        val jwRebirth = JurassicMoviesCatalog.movies.first { it.id == "jurassic_world_rebirth_2025" }
        assertEquals(1234821, jwRebirth.tmdbId)
        assertEquals("tt31036941", jwRebirth.imdbId)
        assertTrue(jwRebirth.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

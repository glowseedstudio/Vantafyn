package dev.vantafyn.feature.home.jumanji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JumanjiWatchOrderTest {

    @Test
    fun jumanjiMoviesCatalog_hasValidSequence() {
        val movies = JumanjiMoviesCatalog.movies
        assertEquals(3, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..3).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..3).toList(), timelineOrders)

        assertEquals("Jumanji", movies.first { it.releaseOrder == 1 }.title)
        assertEquals("Jumanji: The Next Level", movies.first { it.releaseOrder == 3 }.title)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("jumanji", JumanjiMoviesCatalog.normalizeTitle("Jumanji"))
        assertEquals("jumanjiwelcometothejungle", JumanjiMoviesCatalog.normalizeTitle("Jumanji: Welcome to the Jungle"))
        assertEquals("jumanjithenextlevel", JumanjiMoviesCatalog.normalizeTitle("Jumanji: The Next Level"))
    }

    @Test
    fun jumanjiMatching_byTmdbOrImdb() {
        val j1 = JumanjiMoviesCatalog.movies.first { it.id == "jumanji_1995" }
        assertEquals(8844, j1.tmdbId)
        assertEquals("tt0113497", j1.imdbId)
        assertTrue(j1.fallbackPosterUrl.contains("image.tmdb.org"))

        val j2 = JumanjiMoviesCatalog.movies.first { it.id == "jumanji_welcome_to_the_jungle_2017" }
        assertEquals(353486, j2.tmdbId)
        assertEquals("tt2283362", j2.imdbId)
        assertTrue(j2.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

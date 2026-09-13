package dev.vantafyn.feature.home.hunger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HungerGamesWatchOrderTest {

    @Test
    fun hungerGamesMoviesCatalog_hasValidSequence() {
        val movies = HungerGamesMoviesCatalog.movies
        assertEquals(5, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..5).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..5).toList(), timelineOrders)

        // Prequel is timeline #1, release #5
        val prequel = movies.first { it.timelineOrder == 1 }
        assertEquals("The Hunger Games: The Ballad of Songbirds & Snakes", prequel.title)
        assertEquals(5, prequel.releaseOrder)
        assertEquals(2023, prequel.releaseYear)

        // Original film is release #1, timeline #2
        val firstRelease = movies.first { it.releaseOrder == 1 }
        assertEquals("The Hunger Games", firstRelease.title)
        assertEquals(2, firstRelease.timelineOrder)
        assertEquals(2012, firstRelease.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("thehungergames", HungerGamesMoviesCatalog.normalizeTitle("The Hunger Games"))
        assertEquals("thehungergamescatchingfire", HungerGamesMoviesCatalog.normalizeTitle("The Hunger Games: Catching Fire"))
        assertEquals("thehungergamesmockingjaypart1", HungerGamesMoviesCatalog.normalizeTitle("The Hunger Games: Mockingjay – Part 1"))
        assertEquals("thehungergamesmockingjaypart2", HungerGamesMoviesCatalog.normalizeTitle("The Hunger Games: Mockingjay – Part 2"))
        assertEquals("thehungergamestheballadofsongbirdssnakes", HungerGamesMoviesCatalog.normalizeTitle("The Hunger Games: The Ballad of Songbirds & Snakes"))
    }

    @Test
    fun hungerGamesMatching_byTmdbOrImdb() {
        val hg1 = HungerGamesMoviesCatalog.movies.first { it.title == "The Hunger Games" }
        assertEquals(70160, hg1.tmdbId)
        assertEquals("tt1392170", hg1.imdbId)
        assertTrue(hg1.fallbackPosterUrl.contains("image.tmdb.org"))

        val prequel = HungerGamesMoviesCatalog.movies.first { it.releaseOrder == 5 }
        assertEquals(695721, prequel.tmdbId)
        assertEquals("tt10545296", prequel.imdbId)
        assertTrue(prequel.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

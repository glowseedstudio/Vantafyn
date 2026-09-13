package dev.vantafyn.feature.home.potter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HarryPotterWatchOrderTest {

    @Test
    fun harryPotterMoviesCatalog_hasValidSequence() {
        val movies = HarryPotterMoviesCatalog.movies
        assertTrue(movies.isNotEmpty())
        assertEquals(11, movies.size)

        // Unique IDs
        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        // Ensure release orders are sequential from 1 to 11
        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..11).toList(), releaseOrders)

        // Ensure chronological timeline orders are sequential from 1 to 11
        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..11).toList(), timelineOrders)

        // First movie in timeline order is Fantastic Beasts 1 (1926)
        val firstTimeline = movies.first { it.timelineOrder == 1 }
        assertEquals("Fantastic Beasts and Where to Find Them", firstTimeline.title)
        assertEquals(2016, firstTimeline.releaseYear)

        // First movie in release order is Philosopher's Stone (2001)
        val firstRelease = movies.first { it.releaseOrder == 1 }
        assertEquals("Harry Potter and the Philosopher's Stone", firstRelease.title)
        assertEquals(2001, firstRelease.releaseYear)

        // Last movie in timeline order is Deathly Hallows Part 2 (1998 Battle of Hogwarts)
        val lastTimeline = movies.first { it.timelineOrder == 11 }
        assertEquals("Harry Potter and the Deathly Hallows – Part 2", lastTimeline.title)
        assertEquals(2011, lastTimeline.releaseYear)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals(
            "harrypotterandthephilosophersstone",
            HarryPotterMoviesCatalog.normalizeTitle("Harry Potter and the Philosopher's Stone"),
        )
        // Sorcerer's Stone normalizes to match Philosopher's Stone
        assertEquals(
            "harrypotterandthephilosophersstone",
            HarryPotterMoviesCatalog.normalizeTitle("Harry Potter and the Sorcerer's Stone"),
        )
        assertEquals(
            "fantasticbeastsandwheretofindthem",
            HarryPotterMoviesCatalog.normalizeTitle("Fantastic Beasts and Where to Find Them"),
        )
        assertEquals(
            "harrypotterandthedeathlyhallowspart2",
            HarryPotterMoviesCatalog.normalizeTitle("Harry Potter and the Deathly Hallows – Part 2"),
        )
    }

    @Test
    fun harryPotterMatching_byTmdbOrImdbOrNormalizedTitle() {
        val hp1 = HarryPotterMoviesCatalog.movies.first { it.releaseOrder == 1 }
        assertEquals(671, hp1.tmdbId)
        assertEquals("tt0241527", hp1.imdbId)
        assertTrue(hp1.fallbackPosterUrl.contains("image.tmdb.org"))

        val fb1 = HarryPotterMoviesCatalog.movies.first { it.timelineOrder == 1 }
        assertEquals(259316, fb1.tmdbId)
        assertEquals("tt3183660", fb1.imdbId)
        assertTrue(fb1.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

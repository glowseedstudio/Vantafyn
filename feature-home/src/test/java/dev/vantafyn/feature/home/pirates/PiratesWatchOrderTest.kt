package dev.vantafyn.feature.home.pirates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PiratesWatchOrderTest {

    @Test
    fun piratesMoviesCatalog_hasValidSequence() {
        val movies = PiratesMoviesCatalog.movies
        assertEquals(5, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..5).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..5).toList(), timelineOrders)

        assertEquals("Pirates of the Caribbean: The Curse of the Black Pearl", movies.first { it.releaseOrder == 1 }.title)
        assertEquals("Pirates of the Caribbean: Dead Men Tell No Tales", movies.first { it.releaseOrder == 5 }.title)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("piratesofthecaribbeanthecurseoftheblackpearl", PiratesMoviesCatalog.normalizeTitle("Pirates of the Caribbean: The Curse of the Black Pearl"))
        assertEquals("piratesofthecaribbeandeadmanschest", PiratesMoviesCatalog.normalizeTitle("Pirates of the Caribbean: Dead Man's Chest"))
        assertEquals("piratesofthecaribbeanatworldsend", PiratesMoviesCatalog.normalizeTitle("Pirates of the Caribbean: At World's End"))
        assertEquals("piratesofthecaribbeanonstrangertides", PiratesMoviesCatalog.normalizeTitle("Pirates of the Caribbean: On Stranger Tides"))
        assertEquals("piratesofthecaribbeandeadmentellnotales", PiratesMoviesCatalog.normalizeTitle("Pirates of the Caribbean: Dead Men Tell No Tales"))
    }

    @Test
    fun piratesMatching_byTmdbOrImdb() {
        val p1 = PiratesMoviesCatalog.movies.first { it.id == "pirates_of_the_caribbean_the_curse_of_the_black_pearl_2003" }
        assertEquals(22, p1.tmdbId)
        assertEquals("tt0325980", p1.imdbId)
        assertTrue(p1.fallbackPosterUrl.contains("image.tmdb.org"))

        val p5 = PiratesMoviesCatalog.movies.first { it.id == "pirates_of_the_caribbean_dead_men_tell_no_tales_2017" }
        assertEquals(166426, p5.tmdbId)
        assertEquals("tt1790809", p5.imdbId)
        assertTrue(p5.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

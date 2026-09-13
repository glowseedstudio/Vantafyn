package dev.vantafyn.feature.home.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixWatchOrderTest {

    @Test
    fun matrixMoviesCatalog_hasValidSequence() {
        val movies = MatrixMoviesCatalog.movies
        assertEquals(4, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..4).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..4).toList(), timelineOrders)

        assertEquals("The Matrix", movies.first { it.releaseOrder == 1 }.title)
        assertEquals("The Matrix Resurrections", movies.first { it.releaseOrder == 4 }.title)
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("thematrix", MatrixMoviesCatalog.normalizeTitle("The Matrix"))
        assertEquals("thematrixreloaded", MatrixMoviesCatalog.normalizeTitle("The Matrix Reloaded"))
        assertEquals("thematrixrevolutions", MatrixMoviesCatalog.normalizeTitle("The Matrix Revolutions"))
        assertEquals("thematrixresurrections", MatrixMoviesCatalog.normalizeTitle("The Matrix Resurrections"))
    }

    @Test
    fun matrixMatching_byTmdbOrImdb() {
        val m1 = MatrixMoviesCatalog.movies.first { it.id == "the_matrix_1999" }
        assertEquals(603, m1.tmdbId)
        assertEquals("tt0133093", m1.imdbId)
        assertTrue(m1.fallbackPosterUrl.contains("image.tmdb.org"))

        val m4 = MatrixMoviesCatalog.movies.first { it.id == "the_matrix_resurrections_2021" }
        assertEquals(624860, m4.tmdbId)
        assertEquals("tt10838180", m4.imdbId)
        assertTrue(m4.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

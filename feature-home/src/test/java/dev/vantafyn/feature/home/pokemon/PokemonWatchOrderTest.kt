package dev.vantafyn.feature.home.pokemon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonWatchOrderTest {

    @Test
    fun pokemonMoviesCatalog_hasValidSequence() {
        val movies = PokemonMoviesCatalog.movies
        assertEquals(24, movies.size)

        val ids = movies.map { it.id }.toSet()
        assertEquals(movies.size, ids.size)

        val releaseOrders = movies.map { it.releaseOrder }.sorted()
        assertEquals((1..24).toList(), releaseOrders)

        val timelineOrders = movies.map { it.timelineOrder }.sorted()
        assertEquals((1..24).toList(), timelineOrders)

        val first = movies.first { it.releaseOrder == 1 }
        assertEquals("Pokémon: The First Movie - Mewtwo Strikes Back", first.title)
        assertEquals(1998, first.releaseYear)

        val pikachu = movies.first { it.title == "Pokémon Detective Pikachu" }
        assertEquals(2019, pikachu.releaseYear)
        assertTrue(pikachu.fallbackPosterUrl.contains("image.tmdb.org"))
    }

    @Test
    fun normalizeTitle_handlesVariations() {
        assertEquals("pokemonthefirstmoviemewtwostrikesback", PokemonMoviesCatalog.normalizeTitle("Pokémon: The First Movie - Mewtwo Strikes Back"))
        assertEquals("pokemondetectivepikachu", PokemonMoviesCatalog.normalizeTitle("Pokémon Detective Pikachu"))
    }

    @Test
    fun pokemonMatching_byTmdbOrImdb() {
        val first = PokemonMoviesCatalog.movies.first { it.releaseOrder == 1 }
        assertEquals(10228, first.tmdbId)
        assertEquals("tt0190641", first.imdbId)
        assertTrue(first.fallbackPosterUrl.contains("image.tmdb.org"))
    }
}

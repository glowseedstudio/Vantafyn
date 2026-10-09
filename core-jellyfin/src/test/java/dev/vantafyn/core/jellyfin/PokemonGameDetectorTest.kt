package dev.vantafyn.core.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonGameDetectorTest {

    private data class ClassicCase(
        val filename: String,
        val sys: String,
        val gen: Int,
        val expectedId: String,
    )

    @Test
    fun detect_detectsAll3dsMainlineGames() {
        val testCases = listOf(
            Triple("Pokemon Moon.cci", "3ds", "moon"),
            Triple("Pokemon Alpha Sapphire.cci", "3ds", "alphasapphire"),
            Triple("Pokemon Omega Ruby.cci", "3ds", "omegaruby"),
            Triple("Pokemon Sun.cci", "3ds", "sun"),
            Triple("Pokemon X.cci", "3ds", "x"),
            Triple("Pokemon Ultra Moon.cci", "3ds", "ultramoon"),
            Triple("Pokemon Ultra Sun.cci", "3ds", "ultrasun"),
            Triple("Pokemon Y.cci", "3ds", "y"),
            Triple("Pokémon Ultra Sun (USA) (En,Ja,Fr,De,Es,It,Zh-Hans,Zh-Hant).3ds", "3ds", "ultrasun"),
            Triple("Pokemon X", "3ds", "x"),
            Triple("Pokemon - Omega Ruby", "3ds", "omegaruby"),
        )

        for ((filename, sys, expectedId) in testCases) {
            val detected = PokemonGameDetector.detect(filename, filename, sys)
            assertNotNull("Expected detection for $filename", detected)
            assertEquals(expectedId, detected!!.pokemonGameId)
            assertTrue(detected.isPokemonGame)
            assertTrue(detected.vaultSupported)
            assertTrue(detected.generation in 6..7)
            assertEquals("3ds", detected.platform)
        }
    }

    @Test
    fun detect_ignoresNonPokemonAndSpinoffTitles() {
        val ignores = listOf(
            "Fantasy Life (Europe) (En,Fr,De,Es,It).3ds" to "3ds",
            "Mario Kart 7.3ds" to "3ds",
            "0668 Pokemon Mystery Dungeon - Blue Rescue Team (EU)(M5).nds" to "nds",
            "Pokemon Conquest (Europe) (NDSi Enhanced) [b]" to "nds",
            "Pokemon Pinball.gbc" to "gbc",
            "Pokemon Stadium.n64" to "n64",
        )

        for ((filename, sys) in ignores) {
            val detected = PokemonGameDetector.detect(filename, filename, sys)
            assertNull("Expected no detection for spinoff/non-pokemon $filename", detected)
        }
    }

    @Test
    fun detect_detectsClassicGenerations() {
        val classics = listOf(
            ClassicCase("Pokemon - Leaf Green Version (U) (V1.1).gba", "gba", 3, "leafgreen"),
            ClassicCase("Pokemon - Fire Red Version (U) (V1.1).gba", "gba", 3, "firered"),
            ClassicCase("Pokemon - HeartGold Version (v10) (EU).nds", "nds", 4, "heartgold"),
            ClassicCase("Pokemon - White Version 2 (USA, Europe) (NDSi Enhanced) [b].nds", "nds", 5, "white2"),
            ClassicCase("Pokemon Crystal.gbc", "gbc", 2, "crystal"),
            ClassicCase("Pokemon Yellow.gb", "gb", 1, "yellow"),
        )

        for (c in classics) {
            val detected = PokemonGameDetector.detect(c.filename, c.filename, c.sys)
            assertNotNull("Expected detection for ${c.filename}", detected)
            assertEquals(c.expectedId, detected!!.pokemonGameId)
            assertEquals(c.gen, detected.generation)
            assertTrue(detected.vaultSupported)
        }
    }
}

package dev.vantafyn.core.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Gen1NativeSaveParserTest {

    @Test
    fun parse_realPokemonRedSave_success() {
        val saveFile = File("/tmp/pokemon_red.sram")
        if (!saveFile.exists()) return

        val bytes = saveFile.readBytes()
        assertTrue("Must detect as Gen 1 save", Gen1NativeSaveParser.isGen1Save(bytes))

        val result = Gen1NativeSaveParser.parse(bytes, "Pokemon - Red Version", "game123")
        assertNotNull("Parse result must not be null", result)
        assertEquals("ASH", result!!.trainerName)
        assertEquals("477", result.trainerId)
        assertEquals(1, result.party.size)

        val charmander = result.party[0]
        assertEquals(4, charmander.speciesId)
        assertEquals("Charmander", charmander.species)
        assertEquals("CHARMANDER", charmander.nickname)
        assertEquals(6, charmander.level)
        assertTrue(charmander.isInParty)

        val details = result.pokemonDetails[charmander.id]
        assertNotNull("Details must exist for Charmander", details)
        assertEquals(15, details!!.currentHp)
        assertEquals(20, details.maxHp)
        assertTrue(details.moves.contains("Scratch"))
        assertTrue(details.moves.contains("Growl"))
    }
}

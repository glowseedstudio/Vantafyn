package dev.vantafyn.core.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PokemonNativeSaveParserTest {

    /** A 128 KB file that carries genuine Gen 3 section footers — the shape that leaked into HeartGold. */
    private fun syntheticGen3Save(): ByteArray {
        val save = ByteArray(128 * 1024)
        for (section in 0 until 14) {
            val offset = section * 4096
            save[offset + 4084] = (section and 0xFF).toByte()
            save[offset + 4085] = 0
            save[offset + 4088] = 0x25
            save[offset + 4089] = 0x20
            save[offset + 4090] = 0x01
            save[offset + 4091] = 0x08
        }
        return save
    }

    @Test
    fun gen3SaveIsRecognised() {
        val save = syntheticGen3Save()
        assertTrue(Gen3NativeSaveParser.isGen3Save(save))
        assertNotNull(Gen3NativeSaveParser.parse(save, "Pokemon - FireRed Version", "firered"))
    }

    @Test
    fun heartGoldMetadataRejectsGen3ShapedSave() {
        val save = syntheticGen3Save()

        assertNull(
            "A 128 KB Gen 3 file must never satisfy a Gen 4 game",
            PokemonNativeSaveParser.parse(
                saveBytes = save,
                gameTitle = "Pokemon - HeartGold Version",
                gameId = "heartgold",
                expectedGeneration = 4,
                expectedPlatform = "nds",
            ),
        )

        assertNull(
            "Title-only inference must reach the same conclusion",
            PokemonNativeSaveParser.parse(
                saveBytes = save,
                gameTitle = "Pokemon - HeartGold Version",
                gameId = "heartgold",
            ),
        )
    }

    @Test
    fun fireRedStillParsesAsGen3() {
        val parsed = PokemonNativeSaveParser.parse(
            saveBytes = syntheticGen3Save(),
            gameTitle = "Pokemon - FireRed Version",
            gameId = "firered",
            expectedGeneration = 3,
            expectedPlatform = "gba",
        )
        assertNotNull(parsed)
        assertEquals(3, parsed!!.generation)
        assertEquals("gba", parsed.platform)
    }

    @Test
    fun nonPokemonPlatformIsRejected() {
        assertNull(
            PokemonNativeSaveParser.parse(
                saveBytes = syntheticGen3Save(),
                gameTitle = "Pokemon Stadium",
                expectedPlatform = "n64",
            ),
        )
    }

    @Test
    fun unknownGen3TitleDoesNotInventARegion() {
        assertTrue(PokemonGymBadgeCatalog.forGen3("heartgold pokemon - heartgold version", 0xFF).isEmpty())
        assertTrue(PokemonGymBadgeCatalog.forGen3("", 0xFF).isEmpty())
    }

    @Test
    fun gen3RegionsAreOnlyReportedForRealGen3Titles() {
        assertEquals("kanto", PokemonGymBadgeCatalog.forGen3("firered pokemon - firered version", 0x01).single().region)
        assertEquals("hoenn", PokemonGymBadgeCatalog.forGen3("emerald pokemon - emerald version", 0x01).single().region)
    }

    @Test
    fun hgssBadgeRegionsAreJohtoAndKanto() {
        val regions = PokemonGymBadgeCatalog.forGen4("heartgold pokemon - heartgold version", 0b11, 0b101)
        assertEquals(listOf("johto", "kanto"), regions.map { it.region })

        val johto = regions[0].badges
        assertEquals(8, johto.size)
        assertTrue(johto[0].isEarned)
        assertTrue(johto[1].isEarned)
        assertFalse(johto[2].isEarned)

        val kanto = regions[1].badges
        assertEquals(8, kanto.size)
        assertTrue(kanto[0].isEarned)
        assertFalse(kanto[1].isEarned)
        assertTrue(kanto[2].isEarned)
        assertFalse(kanto[4].isEarned)
    }

    @Test
    fun sinnohBadgeRegionIsReportedForDiamondAndPlatinum() {
        assertEquals("sinnoh", PokemonGymBadgeCatalog.forGen4("diamond pokemon - diamond version", 0, 0).single().region)
        assertEquals("sinnoh", PokemonGymBadgeCatalog.forGen4("platinum pokemon - platinum version", 0, 0).single().region)
        assertTrue(PokemonGymBadgeCatalog.forGen4("mystery dungeon explorers of sky", 0, 0).isEmpty())
    }

    @Test
    fun base64GameIdsCannotSpoofARegion() {
        val base64LikeId = "L2hvbWUvamVsbHlmaW4vcm9tcy9ydWJ5"
        val key = pokemonGameKey(base64LikeId, "Pokemon Mystery Dungeon")
        assertFalse(key.contains(base64LikeId))
        assertTrue(PokemonGymBadgeCatalog.forGen3(key, 0xFF).isEmpty())

        assertEquals(
            "kanto",
            PokemonGymBadgeCatalog.forGen3(pokemonGameKey(base64LikeId, "Pokemon - FireRed Version"), 0x01).single().region,
        )
    }

    @Test
    fun uninitializedGen4SaveReturnsNull() {
        val dummy = ByteArray(512 * 1024)
        assertNull(Gen4NativeSaveParser.parse(dummy, "Pokemon - HeartGold Version", "heartgold"))
    }

    @Test
    fun uninitializedGen5SaveReturnsNull() {
        val dummy = ByteArray(512 * 1024)
        assertNull(Gen5NativeSaveParser.parse(dummy, "Pokemon - Black Version", "black"))
    }

    @Test
    fun unovaBadgeRegionIsReportedForGen5() {
        val regions = PokemonGymBadgeCatalog.forGen5("black pokemon - black version", 0b01)
        assertEquals(1, regions.size)
        assertEquals("unova", regions.single().region)
        assertEquals(8, regions.single().badges.size)
        assertTrue(regions.single().badges[0].isEarned)
        assertFalse(regions.single().badges[1].isEarned)
    }
}

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

    @Test
    fun uninitializedGen6SaveReturnsNull() {
        val dummy = ByteArray(1024 * 1024)
        assertNull(Gen6NativeSaveParser.parse(dummy, "Pokemon X", "pokemonx"))
    }

    @Test
    fun uninitializedGen7SaveReturnsNull() {
        val dummy = ByteArray(1024 * 1024)
        assertNull(Gen7NativeSaveParser.parse(dummy, "Pokemon Sun", "pokemonsun"))
    }

    @Test
    fun kalosBadgeRegionIsReportedForGen6XY() {
        val regions = PokemonGymBadgeCatalog.forGen6("pokemon x", 0b00000011)
        assertEquals(1, regions.size)
        assertEquals("kalos", regions.single().region)
        assertEquals(8, regions.single().badges.size)
        assertTrue(regions.single().badges[0].isEarned) // Bug
        assertTrue(regions.single().badges[1].isEarned) // Cliff
        assertFalse(regions.single().badges[2].isEarned) // Rumble
    }

    @Test
    fun hoennBadgeRegionIsReportedForGen6ORAS() {
        val regions = PokemonGymBadgeCatalog.forGen6("omega ruby pokemon - omega ruby", 0b00000001)
        assertEquals(1, regions.size)
        assertEquals("hoenn", regions.single().region)
        assertEquals(8, regions.single().badges.size)
        assertTrue(regions.single().badges[0].isEarned) // Stone
        assertFalse(regions.single().badges[1].isEarned)
    }

    @Test
    fun alolaBadgeRegionIsReportedForGen7() {
        val regions = PokemonGymBadgeCatalog.forGen7(
            gameKey = "pokemon ultra sun",
            stampFlags = 0b00011111,
            unlockedZCrystals = setOf("normalium-z", "firium-z"),
            isUsum = true,
        )
        assertEquals(2, regions.size)
        assertEquals("alola", regions[0].region)
        assertEquals(5, regions[0].badges.size)
        assertTrue(regions[0].badges.all { it.isEarned })
        assertEquals("zcrystals", regions[1].region)
        assertEquals(35, regions[1].badges.size)
        assertTrue(regions[1].badges.single { it.id == "normalium-z" }.isEarned)
        assertTrue(regions[1].badges.single { it.id == "firium-z" }.isEarned)
        assertFalse(regions[1].badges.single { it.id == "waterium-z" }.isEarned)
    }

    @Test
    fun gen6AndGen7SavesAreInferredAndParsed() {
        assertEquals(6, PokemonNativeSaveParser.inferGeneration("Pokemon X"))
        assertEquals(6, PokemonNativeSaveParser.inferGeneration("Pokemon Omega Ruby"))
        assertEquals(7, PokemonNativeSaveParser.inferGeneration("Pokemon Sun"))
        assertEquals(7, PokemonNativeSaveParser.inferGeneration("Pokemon Ultra Moon"))

        // Create a minimal synthetic Gen 6 save with Trainer ID and Kalos badges
        val gen6Save = ByteArray(1024 * 1024)
        // Status at 0x14000: TID at 0x14000 = 12345 (0x3039)
        gen6Save[0x14000] = 0x39.toByte()
        gen6Save[0x14001] = 0x30.toByte()
        // OT name at 0x14048: "Calem" in UTF-16LE
        val otName = "Calem".toByteArray(Charsets.UTF_16LE)
        System.arraycopy(otName, 0, gen6Save, 0x14048, otName.size)
        // Misc at 0x04200: Badges at 0x0420C = 0x05 (Badges 1 and 3)
        gen6Save[0x0420C] = 0x05.toByte()

        val parsed6 = PokemonNativeSaveParser.parse(
            saveBytes = gen6Save,
            gameTitle = "Pokemon X",
            gameId = "pokemonx",
            expectedGeneration = 6,
            expectedPlatform = "3ds",
        )
        assertNotNull(parsed6)
        assertEquals(6, parsed6!!.generation)
        assertEquals("3ds", parsed6.platform)
        assertEquals("12345", parsed6.trainerId)
        assertEquals("Calem", parsed6.trainerName)
        assertEquals(1, parsed6.gymBadges.size)
        assertEquals("kalos", parsed6.gymBadges[0].region)
        assertTrue(parsed6.gymBadges[0].badges[0].isEarned)
        assertFalse(parsed6.gymBadges[0].badges[1].isEarned)
        assertTrue(parsed6.gymBadges[0].badges[2].isEarned)

        // Create a minimal synthetic Gen 7 save
        val gen7Save = ByteArray(1024 * 1024)
        // Status at 0x01200: TID at 0x01200 = 54321 (0xD431)
        gen7Save[0x01200] = 0x31.toByte()
        gen7Save[0x01201] = 0xD4.toByte()
        val otSun = "Sun".toByteArray(Charsets.UTF_16LE)
        System.arraycopy(otSun, 0, gen7Save, 0x01238, otSun.size)
        // Misc at 0x04000: rawStamps at 0x04008 = (0x07 << 4) = 0x70
        gen7Save[0x04008] = 0x70.toByte()
        // Z-Crystals at 0x00D68: Slot 0 = 1831 (0x0727 -> Normalium Z)
        gen7Save[0x00D68] = 0x27.toByte()
        gen7Save[0x00D69] = 0x07.toByte()

        val parsed7 = PokemonNativeSaveParser.parse(
            saveBytes = gen7Save,
            gameTitle = "Pokemon Sun",
            gameId = "pokemonsun",
            expectedGeneration = 7,
            expectedPlatform = "3ds",
        )
        assertNotNull(parsed7)
        assertEquals(7, parsed7!!.generation)
        assertEquals("3ds", parsed7.platform)
        assertEquals("54321", parsed7.trainerId)
        assertEquals("Sun", parsed7.trainerName)
        assertEquals(2, parsed7.gymBadges.size)
        assertEquals("alola", parsed7.gymBadges[0].region)
        assertTrue(parsed7.gymBadges[0].badges[0].isEarned) // Melemele
        assertTrue(parsed7.gymBadges[0].badges[1].isEarned) // Akala
        assertTrue(parsed7.gymBadges[0].badges[2].isEarned) // Ula'ula
        assertFalse(parsed7.gymBadges[0].badges[3].isEarned) // Poni
        assertEquals("zcrystals", parsed7.gymBadges[1].region)
        assertEquals(29, parsed7.gymBadges[1].badges.size)
        assertTrue(parsed7.gymBadges[1].badges.single { it.id == "normalium-z" }.isEarned)
        assertFalse(parsed7.gymBadges[1].badges.single { it.id == "firium-z" }.isEarned)
    }
}

package dev.vantafyn.core.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameBoxartScraperTest {

    @Test
    fun resolvePlatform_mapsStandardCoresCorrectly() {
        val snes = GameBoxartScraper.resolvePlatform("snes")
        assertNotNull(snes)
        assertEquals("Nintendo - Super Nintendo Entertainment System", snes?.libretroName)
        assertEquals("Nintendo_-_Super_Nintendo_Entertainment_System", snes?.githubRepo)

        val gba = GameBoxartScraper.resolvePlatform("gba")
        assertEquals("Nintendo - Game Boy Advance", gba?.libretroName)
        assertEquals("Nintendo_-_Game_Boy_Advance", gba?.githubRepo)

        val genesis = GameBoxartScraper.resolvePlatform("segamd")
        assertEquals("Sega - Mega Drive - Genesis", genesis?.libretroName)
        assertEquals("Sega_-_Mega_Drive_-_Genesis", genesis?.githubRepo)

        val ps1 = GameBoxartScraper.resolvePlatform("psx")
        assertEquals("Sony - PlayStation", ps1?.libretroName)

        val n64 = GameBoxartScraper.resolvePlatform("n64")
        assertEquals("Nintendo - Nintendo 64", n64?.libretroName)
    }

    @Test
    fun cleanTokens_stripsScenePrefixesAndDumpTags() {
        val tokens1 = GameBoxartScraper.cleanTokens("0001 - Super Mario Advance (USA).gba")
        assertEquals(listOf("super", "mario", "advance"), tokens1)

        val tokens2 = GameBoxartScraper.cleanTokens("Pokemon - Emerald Version (USA, Europe) [!].gba")
        assertEquals(listOf("pokemon", "emerald"), tokens2)

        val tokens3 = GameBoxartScraper.cleanTokens("Legend of Zelda, The - The Minish Cap (USA).gba")
        assertEquals(listOf("legend", "of", "zelda", "minish", "cap"), tokens3)

        val tokens4 = GameBoxartScraper.cleanTokens("sonic_advance_2.zip")
        assertEquals(listOf("sonic", "advance", "2"), tokens4)
    }

    @Test
    fun matchGame_resolvesStandardAndFuzzyTitles() {
        val gba = GameBoxartScraper.resolvePlatform("gba")!!
        val sampleEntries = listOf(
            "Pokemon - Emerald Version (USA, Europe).png",
            "Super Mario Advance (USA, Europe).png",
            "Super Mario Advance (USA) (Demo) (Kiosk).png",
            "Legend of Zelda, The - The Minish Cap (USA).png",
            "Sonic Advance (USA) (En,Ja).png",
            "Metroid Fusion (USA).png",
            "Castlevania - Aria of Sorrow (USA).png",
        ).map { fn ->
            val tokens = GameBoxartScraper.cleanTokens(fn).toSet()
            GameBoxartScraper.IndexedEntry(
                rawFilename = fn,
                tokens = tokens,
                joinedTokens = tokens.joinToString(""),
                tokenCount = tokens.size,
                scoreBias = GameBoxartScraper.scoreFilename(fn),
            )
        }

        // Test 1: Pokemon Emerald without "Version"
        val match1 = GameBoxartScraper.matchGame("Pokemon Emerald", "Pokemon Emerald.gba", sampleEntries)
        assertEquals("Pokemon - Emerald Version (USA, Europe).png", match1)

        // Test 2: Numbered scene release
        val match2 = GameBoxartScraper.matchGame("0001 - Super Mario Advance", "0001 - Super Mario Advance (USA).gba", sampleEntries)
        assertEquals("Super Mario Advance (USA, Europe).png", match2)

        // Test 3: Zelda with inverted "The" vs non-inverted
        val match3 = GameBoxartScraper.matchGame("The Legend of Zelda: The Minish Cap", "The Legend of Zelda - The Minish Cap.gba", sampleEntries)
        assertEquals("Legend of Zelda, The - The Minish Cap (USA).png", match3)

        // Test 4: Underscore naming
        val match4 = GameBoxartScraper.matchGame("sonic_advance", "sonic_advance.gba", sampleEntries)
        assertEquals("Sonic Advance (USA) (En,Ja).png", match4)

        // Test 5: Standard clean title
        val match5 = GameBoxartScraper.matchGame("Metroid Fusion", "Metroid Fusion.gba", sampleEntries)
        assertEquals("Metroid Fusion (USA).png", match5)
    }

    @Test
    fun buildCdnUrl_formatsCorrectFastlyUrl() {
        val snes = GameBoxartScraper.resolvePlatform("snes")!!
        val url = GameBoxartScraper.buildCdnUrl(snes, "Super Mario World (USA).png")
        assertEquals(
            "https://cdn.jsdelivr.net/gh/libretro-thumbnails/Nintendo_-_Super_Nintendo_Entertainment_System@master/Named_Boxarts/Super%20Mario%20World%20(USA).png",
            url,
        )
    }

    @Test
    fun convertToCdnUrl_convertsSlowLibretroUrls() {
        val original = "https://thumbnails.libretro.com/Nintendo%20-%20Super%20Nintendo%20Entertainment%20System/Named_Boxarts/Super%20Mario%20World%20(USA).png"
        val converted = GameBoxartScraper.convertToCdnUrl(original)
        assertEquals(
            "https://cdn.jsdelivr.net/gh/libretro-thumbnails/Nintendo_-_Super_Nintendo_Entertainment_System@master/Named_Boxarts/Super%20Mario%20World%20(USA).png",
            converted,
        )
    }
}

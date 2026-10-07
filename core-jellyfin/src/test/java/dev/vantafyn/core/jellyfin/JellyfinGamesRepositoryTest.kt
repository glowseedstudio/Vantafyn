package dev.vantafyn.core.jellyfin

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinGamesRepositoryTest {

    private val testSession = JellyfinSession(
        server = JellyfinServerConfig(url = "https://jellyfin.example.com"),
        user = JellyfinUser(id = UUID.randomUUID(), name = "Player1"),
        profileId = "profile-uuid-retro",
        accessToken = "test-token-abc",
    )

    private val repo = DefaultJellyfinGamesRepository()

    @Test
    fun cleanGameTitle_stripsFileExtensionAndTags() {
        assertEquals("Super Mario World", cleanGameTitle("Super Mario World (USA).sfc"))
        assertEquals("Sonic The Hedgehog", cleanGameTitle("Sonic The Hedgehog (USA, Europe) [!].gen"))
        assertEquals("The Legend of Zelda", cleanGameTitle("The Legend of Zelda (Rev 1).nes"))
        assertEquals("Pokemon Emerald", cleanGameTitle("Pokemon Emerald (USA)"))
    }

    @Test
    fun extractGameRegion_identifiesStandardRegions() {
        assertEquals("USA", extractGameRegion("Super Mario World (USA).sfc"))
        assertEquals("EUROPE", extractGameRegion("Crash Bandicoot (Europe).chd"))
        assertEquals("JAPAN", extractGameRegion("Final Fantasy VII (Japan).iso"))
        assertNull(extractGameRegion("PlainGameName.nes"))
    }

    @Test
    fun gameSaveKind_resolvesCorrectly() {
        assertEquals(GameSaveKind.State, GameSaveKind.fromValue("state"))
        assertEquals(GameSaveKind.Sram, GameSaveKind.fromValue("sram"))
        assertEquals(GameSaveKind.Settings, GameSaveKind.fromValue("settings"))
        assertEquals(GameSaveKind.Sram, GameSaveKind.fromValue("unknown"))
    }

    @Test
    fun getRomDownloadUrl_buildsUrlWithApiKey() {
        val url = repo.getRomDownloadUrl(testSession, "lib-games-123", "token-xyz-abc")
        assertEquals(
            "https://jellyfin.example.com/Vantafyn/Games/lib-games-123/ROM/token-xyz-abc?api_key=test-token-abc",
            url,
        )
    }

    @Test
    fun gameSummary_computesCleanTitleAndRegion() {
        val summary = GameSummary(
            id = "game-1",
            title = "Super Metroid (USA) (Rev 1).sfc",
            systemId = "snes",
            filename = "Super Metroid (USA) (Rev 1).sfc",
            sizeBytes = 3145728L,
            token = "metroid-token",
            extension = ".sfc",
        )
        assertEquals("Super Metroid", summary.cleanTitle)
        assertEquals("USA", summary.region)
    }
}

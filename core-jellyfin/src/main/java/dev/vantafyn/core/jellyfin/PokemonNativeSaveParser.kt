package dev.vantafyn.core.jellyfin

/**
 * Single entry point for parsing native Pokémon save files (.sav / .sram / .dsv).
 *
 * Saves are dispatched from the *game's* known generation rather than by probing file
 * contents, because probe-only dispatch guesses wrong. HeartGold ships a 128 KB file that
 * happens to contain valid Gen 3 section footers, so it fell through to the Gen 3 parser and
 * rendered Hoenn badges plus a Gen 3 Pokédex under a Johto diploma.
 *
 * When a parse's generation disagrees with the game metadata the save is rejected outright,
 * so an unrecognised file yields "no save" instead of invented progress.
 */
object PokemonNativeSaveParser {

    private val NATIVE_POKEMON_PLATFORMS = setOf("gb", "gbc", "gba", "nds")

    private val GEN4_TITLE_TOKENS = listOf(
        "heartgold", "soulsilver", "heart gold", "soul silver", "hgss",
        "platinum", "diamond", "pearl",
    )
    private val GEN5_TITLE_TOKENS = listOf("black 2", "white 2", "b2w2", "black", "white")
    private val GEN3_TITLE_TOKENS = listOf(
        "firered", "fire red", "leafgreen", "leaf green", "emerald",
        "omega ruby", "alpha sapphire", "ruby", "sapphire",
    )
    private val GEN2_TITLE_TOKENS = listOf("crystal", "gold", "silver")
    private val GEN1_TITLE_TOKENS = listOf("yellow", "blue", "pikachu", "red")

    fun parse(
        saveBytes: ByteArray?,
        gameTitle: String,
        gameId: String = "",
        expectedGeneration: Int = 0,
        expectedPlatform: String = "",
    ): PokemonGameSaveDto? {
        if (saveBytes == null || saveBytes.isEmpty()) return null
        if (expectedPlatform.isNotBlank() && expectedPlatform !in NATIVE_POKEMON_PLATFORMS) return null

        val expected = expectedGeneration.takeIf { it in 1..5 } ?: inferGeneration(gameTitle)
        val parsed = when (expected) {
            1 -> Gen1NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            2 -> Gen2NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            3 -> Gen3NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            4 -> Gen4NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            5 -> Gen5NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            else -> probe(saveBytes, gameTitle, gameId)
        } ?: return null

        if (expected != null && parsed.generation != expected) return null
        if (expectedPlatform.isNotBlank() && parsed.platform != expectedPlatform) return null
        return parsed
    }

    private fun probe(saveBytes: ByteArray, gameTitle: String, gameId: String): PokemonGameSaveDto? = when {
        Gen1NativeSaveParser.isGen1Save(saveBytes) -> Gen1NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen2NativeSaveParser.isGen2Save(saveBytes) -> Gen2NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen4NativeSaveParser.isGen4Save(saveBytes, gameTitle) -> Gen4NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen5NativeSaveParser.isGen5Save(saveBytes, gameTitle) -> Gen5NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen3NativeSaveParser.isGen3Save(saveBytes) -> Gen3NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        else -> null
    }

    private fun inferGeneration(gameTitle: String): Int? {
        val title = gameTitle.lowercase()
        return when {
            GEN4_TITLE_TOKENS.any { title.contains(it) } -> 4
            GEN5_TITLE_TOKENS.any { title.contains(it) } -> 5
            GEN3_TITLE_TOKENS.any { title.contains(it) } -> 3
            GEN2_TITLE_TOKENS.any { title.contains(it) } -> 2
            GEN1_TITLE_TOKENS.any { title.contains(it) } -> 1
            else -> null
        }
    }
}

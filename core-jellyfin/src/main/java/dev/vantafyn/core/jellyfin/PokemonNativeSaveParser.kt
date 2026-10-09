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

    private val NATIVE_POKEMON_PLATFORMS = setOf("gb", "gbc", "gba", "nds", "3ds", "n3ds", "nintendo3ds")

    private val GEN7_TITLE_TOKENS = listOf("ultra sun", "ultra moon", "usum", "pokemon sun", "pokemon moon")
    private val GEN6_TITLE_TOKENS = listOf("omega ruby", "alpha sapphire", "oras", "pokemon x", "pokemon y")
    private val GEN4_TITLE_TOKENS = listOf(
        "heartgold", "soulsilver", "heart gold", "soul silver", "hgss",
        "platinum", "diamond", "pearl",
    )
    private val GEN5_TITLE_TOKENS = listOf("black 2", "white 2", "b2w2", "black", "white")
    private val GEN3_TITLE_TOKENS = listOf(
        "firered", "fire red", "leafgreen", "leaf green", "emerald",
        "ruby", "sapphire",
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

        val expected = expectedGeneration.takeIf { it in 1..7 } ?: inferGeneration(gameTitle)
        val parsed = when (expected) {
            1 -> Gen1NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            2 -> Gen2NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            3 -> Gen3NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            4 -> Gen4NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            5 -> Gen5NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            6 -> Gen6NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            7 -> Gen7NativeSaveParser.parse(saveBytes, gameTitle, gameId)
            else -> probe(saveBytes, gameTitle, gameId)
        } ?: return null

        if (expected != null && parsed.generation != expected) return null
        if (expectedPlatform.isNotBlank() && !parsed.platform.equals(expectedPlatform, ignoreCase = true)) return null
        return parsed
    }

    private fun probe(saveBytes: ByteArray, gameTitle: String, gameId: String): PokemonGameSaveDto? = when {
        Gen1NativeSaveParser.isGen1Save(saveBytes) -> Gen1NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen2NativeSaveParser.isGen2Save(saveBytes) -> Gen2NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen4NativeSaveParser.isGen4Save(saveBytes, gameTitle) -> Gen4NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen5NativeSaveParser.isGen5Save(saveBytes, gameTitle) -> Gen5NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen3NativeSaveParser.isGen3Save(saveBytes) -> Gen3NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen6NativeSaveParser.isGen6Save(saveBytes, gameTitle) -> Gen6NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        Gen7NativeSaveParser.isGen7Save(saveBytes, gameTitle) -> Gen7NativeSaveParser.parse(saveBytes, gameTitle, gameId)
        else -> null
    }

    internal fun inferGeneration(gameTitle: String): Int? {
        val title = gameTitle.lowercase()
        val words = title.split(Regex("[^a-z0-9]")).filter { it.isNotBlank() }

        if (GEN7_TITLE_TOKENS.any { title.contains(it) } || words.contains("sun") || words.contains("moon")) return 7
        if (GEN6_TITLE_TOKENS.any { title.contains(it) } || words.contains("x") || words.contains("y")) return 6
        if (GEN4_TITLE_TOKENS.any { title.contains(it) }) return 4
        if (GEN5_TITLE_TOKENS.any { title.contains(it) }) return 5
        if (GEN3_TITLE_TOKENS.any { title.contains(it) }) return 3
        if (GEN2_TITLE_TOKENS.any { title.contains(it) }) return 2
        if (GEN1_TITLE_TOKENS.any { title.contains(it) }) return 1
        return null
    }
}

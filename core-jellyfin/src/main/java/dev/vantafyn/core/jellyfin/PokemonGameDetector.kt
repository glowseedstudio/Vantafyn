package dev.vantafyn.core.jellyfin

/**
 * High-performance on-device Pokémon game detector for mainline titles (Gen 1 through 7).
 * Identifies cartridges from ROM file names, clean titles, and system identifiers,
 * populating [GamePokemonMetadata] for connected Vault rail and local save detection.
 */
object PokemonGameDetector {

    private sealed class Pattern(
        val pokemonGameId: String,
        val canonicalTitle: String,
        val generation: Int,
        val platform: String,
    )

    private val NON_VAULT_SPINOFF_PHRASES = setOf(
        "mystery dungeon", "dungeon", "conquest", "pinball", "stadium", "snap", "colosseum",
        "trading card", "battle revolution", "magikarp jump", "detective pikachu",
        "art academy", "puzzle league", "box ruby", "channel", "pokemon go", "pokemon sleep",
        "pokemon masters", "pokemon unite", "pokemon cafe"
    )

    private val NON_VAULT_SPINOFF_WORDS = setOf(
        "tcg", "xd", "trozei", "dash", "ranger", "rumble", "pokepark", "duel", "shuffle"
    )

    /**
     * Detects if a game title, filename, or system corresponds to a mainline Pokémon title.
     * Returns populated [GamePokemonMetadata] if matched, or null if not a mainline Pokémon title.
     */
    fun detect(
        title: String,
        fileName: String = "",
        systemId: String = "",
    ): GamePokemonMetadata? {
        val cleanT = cleanGameTitle(title).lowercase()
        val cleanF = cleanGameTitle(fileName).lowercase()
        val combined = "$cleanT $cleanF"
        val sys = systemId.lowercase().trim()

        // Verify Pokémon franchise indicator (or explicit 3ds cartridge names like "omega ruby", "ultra sun")
        val isPokemonFranchise = combined.contains("pokemon") ||
            combined.contains("pokémon") ||
            combined.contains("pocket monster") ||
            combined.contains("omega ruby") ||
            combined.contains("alpha sapphire") ||
            combined.contains("ultra sun") ||
            combined.contains("ultra moon") ||
            combined.contains("heartgold") ||
            combined.contains("soulsilver") ||
            combined.contains("firered") ||
            combined.contains("leafgreen")

        if (!isPokemonFranchise) return null

        val words = combined.split(Regex("[^a-z0-9]")).filter { it.isNotBlank() }.toSet()

        // Exclude spin-offs
        if (NON_VAULT_SPINOFF_PHRASES.any { phrase -> combined.contains(phrase) } ||
            NON_VAULT_SPINOFF_WORDS.any { word -> words.contains(word) }) {
            return null
        }

        // 1. Generation 7 (3DS)
        if (combined.contains("ultra sun") || combined.contains("ultrasun")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "ultrasun",
                canonicalTitle = "Pokémon Ultra Sun",
                generation = 7,
                platform = "3ds",
                vaultSupported = true,
            )
        }
        if (combined.contains("ultra moon") || combined.contains("ultramoon")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "ultramoon",
                canonicalTitle = "Pokémon Ultra Moon",
                generation = 7,
                platform = "3ds",
                vaultSupported = true,
            )
        }
        if ((combined.contains("pokemon sun") || combined.contains("pokémon sun") || (words.contains("sun") && (sys.contains("3ds") || sys.contains("citra")))) && !combined.contains("ultra")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "sun",
                canonicalTitle = "Pokémon Sun",
                generation = 7,
                platform = "3ds",
                vaultSupported = true,
            )
        }
        if ((combined.contains("pokemon moon") || combined.contains("pokémon moon") || (words.contains("moon") && (sys.contains("3ds") || sys.contains("citra")))) && !combined.contains("ultra")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "moon",
                canonicalTitle = "Pokémon Moon",
                generation = 7,
                platform = "3ds",
                vaultSupported = true,
            )
        }

        // 2. Generation 6 (3DS)
        if (combined.contains("omega ruby") || combined.contains("omegaruby") || combined.contains("oras")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "omegaruby",
                canonicalTitle = "Pokémon Omega Ruby",
                generation = 6,
                platform = "3ds",
                vaultSupported = true,
            )
        }
        if (combined.contains("alpha sapphire") || combined.contains("alphasapphire")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "alphasapphire",
                canonicalTitle = "Pokémon Alpha Sapphire",
                generation = 6,
                platform = "3ds",
                vaultSupported = true,
            )
        }
        if (words.contains("x") && (words.contains("pokemon") || words.contains("pokémon") || sys.contains("3ds") || sys.contains("citra"))) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "x",
                canonicalTitle = "Pokémon X",
                generation = 6,
                platform = "3ds",
                vaultSupported = true,
            )
        }
        if (words.contains("y") && !words.contains("yellow") && (words.contains("pokemon") || words.contains("pokémon") || sys.contains("3ds") || sys.contains("citra"))) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "y",
                canonicalTitle = "Pokémon Y",
                generation = 6,
                platform = "3ds",
                vaultSupported = true,
            )
        }

        // 3. Generation 5 (NDS)
        if (combined.contains("black 2") || combined.contains("black2") || combined.contains("black version 2")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "black2",
                canonicalTitle = "Pokémon Black 2",
                generation = 5,
                platform = "nds",
                vaultSupported = true,
            )
        }
        if (combined.contains("white 2") || combined.contains("white2") || combined.contains("white version 2")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "white2",
                canonicalTitle = "Pokémon White 2",
                generation = 5,
                platform = "nds",
                vaultSupported = true,
            )
        }
        if (combined.contains("black") && !combined.contains("black 2") && !combined.contains("black2")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "black",
                canonicalTitle = "Pokémon Black",
                generation = 5,
                platform = "nds",
                vaultSupported = true,
            )
        }
        if (combined.contains("white") && !combined.contains("white 2") && !combined.contains("white2")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "white",
                canonicalTitle = "Pokémon White",
                generation = 5,
                platform = "nds",
                vaultSupported = true,
            )
        }

        // 4. Generation 4 (NDS)
        if (combined.contains("heartgold") || combined.contains("heart gold") || combined.contains("hgss")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "heartgold",
                canonicalTitle = "Pokémon HeartGold",
                generation = 4,
                platform = "nds",
                vaultSupported = true,
            )
        }
        if (combined.contains("soulsilver") || combined.contains("soul silver")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "soulsilver",
                canonicalTitle = "Pokémon SoulSilver",
                generation = 4,
                platform = "nds",
                vaultSupported = true,
            )
        }
        if (combined.contains("platinum")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "platinum",
                canonicalTitle = "Pokémon Platinum",
                generation = 4,
                platform = "nds",
                vaultSupported = true,
            )
        }
        if (combined.contains("diamond")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "diamond",
                canonicalTitle = "Pokémon Diamond",
                generation = 4,
                platform = "nds",
                vaultSupported = true,
            )
        }
        if (combined.contains("pearl")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "pearl",
                canonicalTitle = "Pokémon Pearl",
                generation = 4,
                platform = "nds",
                vaultSupported = true,
            )
        }

        // 5. Generation 3 (GBA)
        if (combined.contains("firered") || combined.contains("fire red")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "firered",
                canonicalTitle = "Pokémon FireRed",
                generation = 3,
                platform = "gba",
                vaultSupported = true,
            )
        }
        if (combined.contains("leafgreen") || combined.contains("leaf green")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "leafgreen",
                canonicalTitle = "Pokémon LeafGreen",
                generation = 3,
                platform = "gba",
                vaultSupported = true,
            )
        }
        if (combined.contains("emerald")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "emerald",
                canonicalTitle = "Pokémon Emerald",
                generation = 3,
                platform = "gba",
                vaultSupported = true,
            )
        }
        if (combined.contains("ruby") && !combined.contains("omega")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "ruby",
                canonicalTitle = "Pokémon Ruby",
                generation = 3,
                platform = "gba",
                vaultSupported = true,
            )
        }
        if (combined.contains("sapphire") && !combined.contains("alpha")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "sapphire",
                canonicalTitle = "Pokémon Sapphire",
                generation = 3,
                platform = "gba",
                vaultSupported = true,
            )
        }

        // 6. Generation 2 (GBC)
        if (combined.contains("crystal")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "crystal",
                canonicalTitle = "Pokémon Crystal",
                generation = 2,
                platform = "gbc",
                vaultSupported = true,
            )
        }
        if (combined.contains("gold") && !combined.contains("heart")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "gold",
                canonicalTitle = "Pokémon Gold",
                generation = 2,
                platform = "gbc",
                vaultSupported = true,
            )
        }
        if (combined.contains("silver") && !combined.contains("soul")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "silver",
                canonicalTitle = "Pokémon Silver",
                generation = 2,
                platform = "gbc",
                vaultSupported = true,
            )
        }

        // 7. Generation 1 (GB)
        if (combined.contains("yellow") || combined.contains("pikachu")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "yellow",
                canonicalTitle = "Pokémon Yellow",
                generation = 1,
                platform = "gb",
                vaultSupported = true,
            )
        }
        if (combined.contains("red") && !combined.contains("fire")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "red",
                canonicalTitle = "Pokémon Red",
                generation = 1,
                platform = "gb",
                vaultSupported = true,
            )
        }
        if (combined.contains("blue")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "blue",
                canonicalTitle = "Pokémon Blue",
                generation = 1,
                platform = "gb",
                vaultSupported = true,
            )
        }
        if (combined.contains("green") && !combined.contains("leaf")) {
            return GamePokemonMetadata(
                isPokemonGame = true,
                pokemonGameId = "green",
                canonicalTitle = "Pokémon Green",
                generation = 1,
                platform = "gb",
                vaultSupported = true,
            )
        }

        return null
    }
}

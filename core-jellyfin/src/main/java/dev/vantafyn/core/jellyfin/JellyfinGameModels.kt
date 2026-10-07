package dev.vantafyn.core.jellyfin

import java.io.Serializable

enum class GameSaveKind(val value: String) {
    State("state"),
    Sram("sram"),
    Settings("settings");

    companion object {
        fun fromValue(value: String): GameSaveKind =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Sram
    }
}

data class CloudSaveEntry(
    val data: ByteArray,
    val lastModifiedMs: Long = 0L,
    val sizeBytes: Long = 0L,
) : Serializable

data class GameLibrary(
    val id: String,
    val name: String,
    val path: String = "",
) : Serializable

data class GameSystem(
    val id: String,
    val name: String,
    val core: String,
    val extensions: List<String> = emptyList(),
    val gameCount: Int = 0,
    val icon: String = "",
    val logoUrl: String? = null,
) : Serializable {
    val displayName: String
        get() = resolveSystemDisplayName(id, name)

    val officialLogoUrl: String
        get() = logoUrl?.takeIf { it.isNotBlank() } ?: resolveSystemLogoUrl(id, core)
}

fun resolveSystemDisplayName(id: String, rawName: String): String {
    val key = id.lowercase().trim()
    return when {
        key == "gba" || key == "gameboyadvance" -> "Game Boy Advance"
        key == "snes" || key == "sfc" || key == "supernintendo" -> "Super Nintendo"
        key == "nes" || key == "famicom" -> "Nintendo Entertainment System"
        key == "n64" || key == "nintendo64" -> "Nintendo 64"
        key == "gb" || key == "gameboy" -> "Game Boy"
        key == "gbc" || key == "gameboycolor" -> "Game Boy Color"
        key == "nds" || key == "ds" || key == "nintendods" -> "Nintendo DS"
        key == "psx" || key == "ps1" || key == "playstation" -> "Sony PlayStation"
        key == "psp" || key == "playstationportable" -> "PlayStation Portable"
        key == "segamd" || key == "genesis" || key == "megadrive" -> "Sega Genesis"
        key == "segams" || key == "mastersystem" -> "Sega Master System"
        key == "segagg" || key == "gamegear" -> "Sega Game Gear"
        key == "dreamcast" || key == "dc" -> "Sega Dreamcast"
        key == "atari2600" || key == "a2600" -> "Atari 2600"
        key == "atari7800" || key == "a7800" -> "Atari 7800"
        key == "arcade" || key == "mame" || key == "fbneo" -> "Arcade Classics"
        key == "neogeo" || key == "ngp" || key == "ngpc" -> "SNK Neo Geo"
        key == "wonderswan" || key == "ws" || key == "wsc" -> "Bandai WonderSwan"
        key == "pcengine" || key == "pce" || key == "tg16" || key == "turbografx16" -> "PC Engine / TurboGrafx"
        rawName.isNotBlank() -> rawName
        else -> id.uppercase()
    }
}

fun resolveSystemLogoUrl(id: String, core: String): String {
    val key = (if (id.isNotBlank()) id else core).lowercase().trim()
    val themeFolder = when {
        key.contains("gba") || key.contains("advance") -> "gba"
        key.contains("snes") || key.contains("sfc") -> "snes"
        key.contains("nes") || key.contains("famicom") -> "nes"
        key.contains("n64") -> "n64"
        key.contains("gbc") -> "gbc"
        key == "gb" || key.contains("gameboy") -> "gb"
        key.contains("nds") || key == "ds" -> "nds"
        key.contains("psx") || key.contains("ps1") || key.contains("playstation") -> "psx"
        key.contains("psp") -> "psp"
        key.contains("genesis") || key.contains("megadrive") || key.contains("segamd") -> "genesis"
        key.contains("mastersystem") || key.contains("segams") -> "mastersystem"
        key.contains("gamegear") || key.contains("segagg") -> "gamegear"
        key.contains("dreamcast") -> "dreamcast"
        key.contains("atari2600") -> "atari2600"
        key.contains("atari7800") -> "atari7800"
        key.contains("arcade") || key.contains("mame") || key.contains("fbneo") -> "arcade"
        key.contains("neogeo") || key.contains("ngp") -> "neogeo"
        key.contains("wonderswan") || key.contains("ws") -> "wonderswan"
        key.contains("pcengine") || key.contains("pce") || key.contains("turbo") -> "pcengine"
        else -> "gba"
    }
    return "https://raw.githubusercontent.com/RetroPie/es-theme-carbon/master/$themeFolder/art/system.svg"
}

data class GameSummary(
    val id: String,
    val title: String,
    val systemId: String,
    val filename: String,
    val sizeBytes: Long = 0L,
    val token: String = "",
    val extension: String = "",
    val boxartUrl: String? = null,
    val pokemon: GamePokemonMetadata? = null,
) : Serializable {
    val cleanTitle: String
        get() = cleanGameTitle(title)

    val region: String?
        get() = extractGameRegion(title)
}

data class GameDetail(
    val id: String,
    val title: String,
    val systemId: String,
    val filename: String,
    val sizeBytes: Long = 0L,
    val token: String = "",
    val extension: String = "",
    val core: String = "",
    val cleanTitle: String = "",
    val region: String? = null,
    val downloadUrl: String = "",
    val boxartUrl: String? = null,
    val pokemon: GamePokemonMetadata? = null,
) : Serializable

data class GamePokemonMetadata(
    val isPokemonGame: Boolean = true,
    val pokemonGameId: String = "",
    val canonicalTitle: String = "",
    val generation: Int = 0,
    val platform: String = "",
    val saveType: String = "sram",
    val hasSave: Boolean = false,
    val vaultSupported: Boolean = true,
    val detectionConfidence: String = "high",
) : Serializable

data class GameSaveMetadata(
    val gameId: String,
    val kind: GameSaveKind,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
) : Serializable

/**
 * Removes standard dump/scene tags like (USA), (Europe), [!], (Rev 1), etc.
 */
fun cleanGameTitle(rawTitle: String): String {
    var title = rawTitle
    val dotIndex = title.lastIndexOf('.')
    if (dotIndex > 0) {
        title = title.substring(0, dotIndex)
    }
    // Remove (USA), (Rev 1), [!], etc.
    title = title.replace(Regex("\\s*\\([^)]*\\)"), "")
    title = title.replace(Regex("\\s*\\[[^]]*\\]"), "")
    return title.trim()
}

/**
 * Extracts region tag if present (e.g. USA, Europe, Japan, World).
 */
fun extractGameRegion(rawTitle: String): String? {
    val match = Regex("\\((USA|Europe|Japan|World|En|Fr|De|Es|It|Australia|Beta|Rev\\s*\\d*)[^)]*\\)", RegexOption.IGNORE_CASE)
        .find(rawTitle)
    return match?.groupValues?.getOrNull(1)?.uppercase()
}

data class PokemonSummaryDto(
    val id: String = "",
    val species: String = "",
    val speciesId: Int = 0,
    val form: String? = null,
    val nickname: String = "",
    val level: Int = 1,
    val gender: String? = null,
    val isShiny: Boolean = false,
    val originalTrainer: String? = null,
    val originalTrainerId: String? = null,
    val originGame: String? = null,
    val currentGame: String? = null,
    val currentLocation: String = "",
    val boxIndex: Int? = null,
    val slotIndex: Int = 1,
    val isInParty: Boolean = false,
    val legalityStatus: String = "valid",
    val isHallOfFameMember: Boolean = false,
) : Serializable

data class PokemonBoxDto(
    val boxIndex: Int = 1,
    val name: String = "",
    val capacity: Int = 30,
    val occupiedCount: Int = 0,
    val entries: List<PokemonSummaryDto> = emptyList(),
) : Serializable

data class PokemonGymBadgeRegionDto(
    val region: String = "",
    val displayName: String = "",
    val generation: Int = 0,
    val badges: List<PokemonGymBadgeDto> = emptyList(),
) : Serializable

data class PokemonGymBadgeDto(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    val generation: Int = 0,
    val order: Int = 0,
    val isEarned: Boolean = false,
) : Serializable

data class PokemonBadgeArtCatalogDto(
    val configured: Boolean = false,
    val availableCount: Int = 0,
    val totalCount: Int = 0,
    val regions: List<PokemonBadgeArtRegionDto> = emptyList(),
) : Serializable

data class PokemonBadgeArtRegionDto(
    val id: String = "",
    val name: String = "",
    val generation: Int = 0,
    val availableCount: Int = 0,
    val totalCount: Int = 0,
    val badges: List<PokemonBadgeArtDto> = emptyList(),
) : Serializable

data class PokemonBadgeArtDto(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    val generation: Int = 0,
    val order: Int = 0,
    val available: Boolean = false,
    val imageUrl: String? = null,
) : Serializable

data class PokemonDiplomaProofDto(
    val gameId: String = "",
    val certificateId: String = "",
    val title: String = "",
    val uploadedAtUtc: String = "",
    val contentType: String = "image/png",
    val sizeBytes: Long = 0L,
    val imageUrl: String = "",
) : Serializable

data class PokemonGameSaveDto(
    val gameId: String = "",
    val title: String = "",
    val platform: String = "",
    val generation: Int = 0,
    val trainerName: String? = null,
    val trainerId: String? = null,
    val money: Int? = null,
    val pokedexSeen: Int? = null,
    val pokedexCaught: Int? = null,
    val saveFound: Boolean = false,
    val providerAvailable: Boolean = false,
    val errorMessage: String? = null,
    val party: List<PokemonSummaryDto> = emptyList(),
    val boxes: List<PokemonBoxDto> = emptyList(),
    val totalPokemonCount: Int = 0,
    val shinyCount: Int = 0,
    val pokemonDetails: Map<String, PokemonDetailsDto> = emptyMap(),
    val caughtSpeciesIds: List<Int> = emptyList(),
    val seenSpeciesIds: List<Int> = emptyList(),
    val gymBadges: List<PokemonGymBadgeRegionDto> = emptyList(),
) : Serializable

/**
 * Builds a stable key used to match a save against its game's region/badge tables.
 *
 * The raw Jellyfin game id is a base64 blob, so it can accidentally contain lowercase
 * words such as "ruby" and produce false region matches. Only short, lowercase slugs
 * (the `pokemonGameId` values) are kept; anything else falls back to the title alone.
 */
fun pokemonGameKey(gameId: String, gameTitle: String): String {
    val rawId = gameId.trim()
    val usableId = if (
        rawId.isNotEmpty() &&
        rawId.length <= 32 &&
        rawId.all { it.isLowerCase() || it.isDigit() || it == '_' || it == '-' }
    ) {
        rawId
    } else {
        ""
    }
    return "$usableId ${gameTitle.lowercase()}".trim()
}

object PokemonGymBadgeCatalog {
    private data class BadgeDef(val id: String, val name: String)

    private val kanto = listOf(
        BadgeDef("boulder", "Boulder Badge"),
        BadgeDef("cascade", "Cascade Badge"),
        BadgeDef("thunder", "Thunder Badge"),
        BadgeDef("rainbow", "Rainbow Badge"),
        BadgeDef("soul", "Soul Badge"),
        BadgeDef("marsh", "Marsh Badge"),
        BadgeDef("volcano", "Volcano Badge"),
        BadgeDef("earth", "Earth Badge"),
    )

    private val johto = listOf(
        BadgeDef("zephyr", "Zephyr Badge"),
        BadgeDef("hive", "Hive Badge"),
        BadgeDef("plain", "Plain Badge"),
        BadgeDef("fog", "Fog Badge"),
        BadgeDef("storm", "Storm Badge"),
        BadgeDef("mineral", "Mineral Badge"),
        BadgeDef("glacier", "Glacier Badge"),
        BadgeDef("rising", "Rising Badge"),
    )

    private val hoenn = listOf(
        BadgeDef("stone", "Stone Badge"),
        BadgeDef("knuckle", "Knuckle Badge"),
        BadgeDef("dynamo", "Dynamo Badge"),
        BadgeDef("heat", "Heat Badge"),
        BadgeDef("balance", "Balance Badge"),
        BadgeDef("feather", "Feather Badge"),
        BadgeDef("mind", "Mind Badge"),
        BadgeDef("rain", "Rain Badge"),
    )

    private val sinnoh = listOf(
        BadgeDef("coal", "Coal Badge"),
        BadgeDef("forest", "Forest Badge"),
        BadgeDef("cobble", "Cobble Badge"),
        BadgeDef("fen", "Fen Badge"),
        BadgeDef("relic", "Relic Badge"),
        BadgeDef("mine", "Mine Badge"),
        BadgeDef("icicle", "Icicle Badge"),
        BadgeDef("beacon", "Beacon Badge"),
    )

    fun forGen1(kantoFlags: Int): List<PokemonGymBadgeRegionDto> =
        listOf(createRegion("kanto", "Kanto", 1, kanto, kantoFlags))

    fun forGen2(johtoFlags: Int, kantoFlags: Int): List<PokemonGymBadgeRegionDto> =
        listOf(
            createRegion("johto", "Johto", 2, johto, johtoFlags),
            createRegion("kanto", "Kanto", 1, kanto, kantoFlags),
        )

    fun forGen3(gameKey: String, badgeFlags: Int): List<PokemonGymBadgeRegionDto> {
        val key = gameKey.lowercase()
        val isFrLg = key.contains("firered") ||
            key.contains("fire_red") ||
            key.contains("fire red") ||
            key.contains("leafgreen") ||
            key.contains("leaf_green") ||
            key.contains("leaf green")
        val isHoenn = key.contains("emerald") ||
            key.contains("omega ruby") ||
            key.contains("alpha sapphire") ||
            key.contains("ruby") ||
            key.contains("sapphire")
        return when {
            isFrLg -> listOf(createRegion("kanto", "Kanto", 1, kanto, badgeFlags))
            isHoenn -> listOf(createRegion("hoenn", "Hoenn", 3, hoenn, badgeFlags))
            else -> emptyList()
        }
    }

    /**
     * @param johtoFlags Sinnoh badge bitfield for DP/Pt, Johto badge bitfield for HGSS.
     * @param kantoFlags Kanto badge bitfield for HGSS (bits 8..15 of the 16-badge field).
     */
    fun forGen4(gameKey: String, johtoFlags: Int, kantoFlags: Int): List<PokemonGymBadgeRegionDto> {
        val key = gameKey.lowercase()
        val isHgss = key.contains("heartgold") ||
            key.contains("soulsilver") ||
            key.contains("heart gold") ||
            key.contains("soul silver") ||
            key.contains("hgss")
        val isSinnoh = key.contains("diamond") || key.contains("pearl") || key.contains("platinum")
        return when {
            isHgss -> listOf(
                createRegion("johto", "Johto", 2, johto, johtoFlags),
                createRegion("kanto", "Kanto", 1, kanto, kantoFlags),
            )
            isSinnoh -> listOf(createRegion("sinnoh", "Sinnoh", 4, sinnoh, johtoFlags))
            else -> emptyList()
        }
    }

    private fun createRegion(
        region: String,
        displayName: String,
        generation: Int,
        badges: List<BadgeDef>,
        flags: Int,
    ): PokemonGymBadgeRegionDto =
        PokemonGymBadgeRegionDto(
            region = region,
            displayName = displayName,
            generation = generation,
            badges = badges.mapIndexed { index, badge ->
                PokemonGymBadgeDto(
                    id = badge.id,
                    name = badge.name,
                    region = region,
                    generation = generation,
                    order = index + 1,
                    isEarned = (flags and (1 shl index)) != 0,
                )
            },
        )
}

data class PokemonStatsDto(
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val specialAttack: Int = 0,
    val specialDefense: Int = 0,
    val speed: Int = 0,
) : Serializable

data class PokemonDetailsDto(
    val summary: PokemonSummaryDto = PokemonSummaryDto(),
    val nature: String? = null,
    val ability: String? = null,
    val heldItem: String? = null,
    val moves: List<String> = emptyList(),
    val iv: PokemonStatsDto? = null,
    val ev: PokemonStatsDto? = null,
    val currentHp: Int? = null,
    val maxHp: Int? = null,
    val friendship: Int? = null,
    val pokeball: String? = null,
    val rawData: String? = null,
    val legalityStatus: String = "valid",
    val legalityReport: String? = null,
    val illegalitiesCount: Int = 0,
    val movesLegality: List<Boolean> = emptyList(),
    val learnableMoves: List<PokemonLearnableMoveDto> = emptyList(),
    val ribbons: List<PokemonRibbonDto> = emptyList(),
    val isHallOfFameMember: Boolean = false,
    val availableEvolutions: List<PokemonEvolutionOptionDto> = emptyList(),
) : Serializable

data class PokemonEvolutionOptionDto(
    val targetSpeciesId: Int = 0,
    val targetSpecies: String = "",
    val triggerMethod: String = "Trade",
    val requiredItem: String? = null,
    val requiredLevel: Int? = null,
    val description: String = "",
    val canEvolveNow: Boolean = true,
) : Serializable

data class PokemonRibbonDto(
    val key: String = "",
    val name: String = "",
    val category: String = "Memorial",
    val description: String = "",
    val title: String? = null,
    val iconColorHex: String = "#3B82F6",
) : Serializable

data class PokemonLearnableMoveDto(
    val name: String = "",
    val type: String = "Normal",
    val category: String = "Physical",
    val power: Int? = null,
    val accuracy: Int? = null,
    val pp: Int = 20,
    val learnMethod: String = "Level Up",
    val levelLearned: Int? = null,
    val description: String? = null,
) : Serializable

package dev.vantafyn.core.jellyfin

import java.io.Serializable

/**
 * Catalog of authentic mainline Pokémon ribbons and resolution logic for Android / offline Native parsers.
 */
object PokemonRibbonCatalog {

    val KNOWN_RIBBONS: Map<String, PokemonRibbonDto> = mapOf(
        // Champion Ribbons
        "champion" to PokemonRibbonDto(
            key = "champion",
            name = "Champion Ribbon",
            category = "Champion",
            description = "A Ribbon awarded for clearing the Pokémon League and entering the Hall of Fame in the Hoenn or Kanto region.",
            title = "the Champion",
            iconColorHex = "#F59E0B",
        ),
        "champion_sinnoh" to PokemonRibbonDto(
            key = "champion_sinnoh",
            name = "Sinnoh Champion Ribbon",
            category = "Champion",
            description = "A Ribbon awarded for beating the Sinnoh Pokémon League Champion and entering the Hall of Fame.",
            title = "the Sinnoh Champion",
            iconColorHex = "#3B82F6",
        ),
        "champion_kalos" to PokemonRibbonDto(
            key = "champion_kalos",
            name = "Kalos Champion Ribbon",
            category = "Champion",
            description = "A Ribbon awarded for beating the Kalos Champion and entering the Hall of Fame.",
            title = "the Kalos Champion",
            iconColorHex = "#EC4899",
        ),
        "champion_alola" to PokemonRibbonDto(
            key = "champion_alola",
            name = "Alola Champion Ribbon",
            category = "Champion",
            description = "A Ribbon awarded for becoming the first Champion of the Alola region Pokémon League.",
            title = "the Alola Champion",
            iconColorHex = "#F97316",
        ),
        "champion_galar" to PokemonRibbonDto(
            key = "champion_galar",
            name = "Galar Champion Ribbon",
            category = "Champion",
            description = "A Ribbon awarded for defeating the Champion in the Champion Cup of the Galar region.",
            title = "the Galar Champion",
            iconColorHex = "#8B5CF6",
        ),
        "champion_paldea" to PokemonRibbonDto(
            key = "champion_paldea",
            name = "Paldea Champion Ribbon",
            category = "Champion",
            description = "A Ribbon awarded for completing the Champion Assessment in the Paldea region.",
            title = "the Paldea Champion",
            iconColorHex = "#10B981",
        ),

        // Achievement & Effort Ribbons
        "effort" to PokemonRibbonDto(
            key = "effort",
            name = "Effort Ribbon",
            category = "Effort",
            description = "A Ribbon awarded to a Pokémon that has put forth maximum effort in its EV training (510 total effort points).",
            title = "the Effort-Minded",
            iconColorHex = "#EF4444",
        ),
        "footprint" to PokemonRibbonDto(
            key = "footprint",
            name = "Footprint Ribbon",
            category = "Affection",
            description = "A Ribbon awarded to a Pokémon that reached the pinnacle of experience and level (Lv. 100 or max friendship).",
            title = "the Experienced",
            iconColorHex = "#10B981",
        ),
        "best_friends" to PokemonRibbonDto(
            key = "best_friends",
            name = "Best Friends Ribbon",
            category = "Affection",
            description = "A Ribbon awarded to a Pokémon with deep, unshakable bonds of affection with its Trainer.",
            title = "the Great Friend",
            iconColorHex = "#EC4899",
        ),

        // Battle Facility & Tower Ribbons
        "winning" to PokemonRibbonDto(
            key = "winning",
            name = "Winning Ribbon",
            category = "Tower",
            description = "A Ribbon awarded for clearing the Battle Tower at Level 50 in the Hoenn region.",
            title = "the Tower Master",
            iconColorHex = "#6366F1",
        ),
        "victory" to PokemonRibbonDto(
            key = "victory",
            name = "Victory Ribbon",
            category = "Tower",
            description = "A Ribbon awarded for clearing Level 100 in the Battle Tower.",
            title = "the Victorious",
            iconColorHex = "#F59E0B",
        ),
        "master_rank" to PokemonRibbonDto(
            key = "master_rank",
            name = "Master Rank Ribbon",
            category = "Battle",
            description = "A Ribbon awarded for winning a Master Rank Tier match in the Ranked Battles stadium.",
            title = "the Rank Master",
            iconColorHex = "#8B5CF6",
        ),
        "ability" to PokemonRibbonDto(
            key = "ability",
            name = "Ability Ribbon",
            category = "Tower",
            description = "A Ribbon awarded for defeating Tower Tycoon Palmer in the Sinnoh Battle Tower.",
            title = "the Tower Master",
            iconColorHex = "#14B8A6",
        ),

        // Contest Ribbons
        "contest_star" to PokemonRibbonDto(
            key = "contest_star",
            name = "Contest Star Ribbon",
            category = "Contest",
            description = "A Ribbon awarded to a superstar Pokémon that conquered all Master Rank Super Contests.",
            title = "the Shining Star",
            iconColorHex = "#F43F5E",
        ),
        "cool" to PokemonRibbonDto(
            key = "cool",
            name = "Cool Ribbon",
            category = "Contest",
            description = "A Ribbon awarded for triumphing in the Cool Contest Master Rank.",
            title = "the Cool",
            iconColorHex = "#EF4444",
        ),
        "beauty" to PokemonRibbonDto(
            key = "beauty",
            name = "Beauty Ribbon",
            category = "Contest",
            description = "A Ribbon awarded for triumphing in the Beauty Contest Master Rank.",
            title = "the Beautiful",
            iconColorHex = "#3B82F6",
        ),
        "cute" to PokemonRibbonDto(
            key = "cute",
            name = "Cute Ribbon",
            category = "Contest",
            description = "A Ribbon awarded for triumphing in the Cute Contest Master Rank.",
            title = "the Adorable",
            iconColorHex = "#EC4899",
        ),
        "smart" to PokemonRibbonDto(
            key = "smart",
            name = "Smart Ribbon",
            category = "Contest",
            description = "A Ribbon awarded for triumphing in the Smart Contest Master Rank.",
            title = "the Clever",
            iconColorHex = "#10B981",
        ),
        "tough" to PokemonRibbonDto(
            key = "tough",
            name = "Tough Ribbon",
            category = "Contest",
            description = "A Ribbon awarded for triumphing in the Tough Contest Master Rank.",
            title = "the Sturdy",
            iconColorHex = "#F59E0B",
        ),
    )

    fun evaluateRibbons(
        explicitRibbons: Map<String, Byte>? = null,
        evs: PokemonStatsDto? = null,
        level: Int = 5,
        friendship: Int = 0,
        originGame: String? = null,
        generation: Int = 3,
        isParty: Boolean = false,
        isChampionRibbonSet: Boolean = false,
    ): List<PokemonRibbonDto> {
        val result = mutableMapOf<String, PokemonRibbonDto>()

        // 1. Explicit ribbons (e.g. from Save file bitflags)
        if (isChampionRibbonSet) {
            KNOWN_RIBBONS["champion"]?.let { result["champion"] = it }
        }

        if (explicitRibbons != null) {
            for ((k, v) in explicitRibbons) {
                if (v > 0) {
                    val match = KNOWN_RIBBONS[k.lowercase()]
                    if (match != null) {
                        result[match.key] = match
                    } else {
                        val formattedName = k.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        val ribbonName = if (formattedName.endsWith("Ribbon", ignoreCase = true)) formattedName else "$formattedName Ribbon"
                        result[k.lowercase()] = PokemonRibbonDto(
                            key = k.lowercase(),
                            name = ribbonName,
                            category = "Memorial",
                            description = "A prestigious ribbon earned across adventures: $formattedName.",
                            title = "the $formattedName",
                            iconColorHex = "#3B82F6",
                        )
                    }
                }
            }
        }

        // 2. Deterministic: Effort Ribbon (510 total EVs)
        if (evs != null) {
            val totalEvs = evs.hp + evs.attack + evs.defense + evs.speed + evs.specialAttack + evs.specialDefense
            if (totalEvs >= 508 && !result.containsKey("effort")) {
                KNOWN_RIBBONS["effort"]?.let { result["effort"] = it }
            }
        }

        // 3. Deterministic: Footprint Ribbon (Lv. 100 or max friendship)
        if ((level >= 100 || friendship >= 250) && !result.containsKey("footprint")) {
            KNOWN_RIBBONS["footprint"]?.let { result["footprint"] = it }
        }

        return result.values.sortedWith(
            compareBy<PokemonRibbonDto> { getCategoryOrder(it.category) }.thenBy { it.name }
        )
    }

    private fun resolveChampionRibbonKey(originGame: String?, generation: Int): String {
        if (!originGame.isNullOrBlank()) {
            val g = originGame.lowercase()
            if (g.contains("ruby") || g.contains("sapphire") || g.contains("emerald") || g.contains("firered") || g.contains("leafgreen") || g.contains("omega") || g.contains("alpha"))
                return "champion"
            if (g.contains("diamond") || g.contains("pearl") || g.contains("platinum") || g.contains("brilliant") || g.contains("shining"))
                return "champion_sinnoh"
            if (g.contains("x") || g.contains("y"))
                return "champion_kalos"
            if (g.contains("sun") || g.contains("moon") || g.contains("ultra"))
                return "champion_alola"
            if (g.contains("sword") || g.contains("shield"))
                return "champion_galar"
            if (g.contains("scarlet") || g.contains("violet"))
                return "champion_paldea"
        }

        return when (generation) {
            3 -> "champion"
            4 -> "champion_sinnoh"
            6 -> "champion_kalos"
            7 -> "champion_alola"
            8 -> "champion_galar"
            9 -> "champion_paldea"
            else -> "champion"
        }
    }

    private fun getCategoryOrder(category: String): Int = when (category.lowercase()) {
        "champion" -> 1
        "effort" -> 2
        "affection" -> 3
        "battle" -> 4
        "tower" -> 5
        "contest" -> 6
        else -> 7
    }
}

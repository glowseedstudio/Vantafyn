package dev.vantafyn.core.jellyfin

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pure Kotlin native Gen 3 save parser (.sav / .sram) for Game Boy Advance Pokémon titles:
 * Pokémon FireRed, LeafGreen, Emerald, Ruby, and Sapphire.
 *
 * Fully autonomous: does not require external PKVault container services or HTTP servers.
 */
object Gen3NativeSaveParser {

    private const val SECTION_SIZE = 4096
    private const val SECTIONS_PER_SLOT = 14
    private const val SLOT_SIZE = SECTION_SIZE * SECTIONS_PER_SLOT // 57,344 bytes
    private const val GEN3_SIGNATURE = 0x08012025

    // Substructure block permutation table (PID % 24)
    // G = 0 (Growth), A = 1 (Attacks), E = 2 (EVs), M = 3 (Misc)
    private val BLOCK_ORDERS = arrayOf(
        intArrayOf(0, 1, 2, 3), // 00: GAEM
        intArrayOf(0, 1, 3, 2), // 01: GAME
        intArrayOf(0, 2, 1, 3), // 02: GEAM
        intArrayOf(0, 2, 3, 1), // 03: GEMA
        intArrayOf(0, 3, 1, 2), // 04: GMAE
        intArrayOf(0, 3, 2, 1), // 05: GMEA
        intArrayOf(1, 0, 2, 3), // 06: AGEM
        intArrayOf(1, 0, 3, 2), // 07: AGME
        intArrayOf(1, 2, 0, 3), // 08: AEGM
        intArrayOf(1, 2, 3, 0), // 09: AEMG
        intArrayOf(1, 3, 0, 2), // 10: AMGE
        intArrayOf(1, 3, 2, 0), // 11: AMEG
        intArrayOf(2, 0, 1, 3), // 12: EGAM
        intArrayOf(2, 0, 3, 1), // 13: EGMA
        intArrayOf(2, 1, 0, 3), // 14: EAGM
        intArrayOf(2, 1, 3, 0), // 15: EAMG
        intArrayOf(2, 3, 0, 1), // 16: EMGA
        intArrayOf(2, 3, 1, 0), // 17: EMAG
        intArrayOf(3, 0, 1, 2), // 18: MGAE
        intArrayOf(3, 0, 2, 1), // 19: MGEA
        intArrayOf(3, 1, 0, 2), // 20: MAGE
        intArrayOf(3, 1, 2, 0), // 21: MAEG
        intArrayOf(3, 2, 0, 1), // 22: MEGA
        intArrayOf(3, 2, 1, 0), // 23: MEAG
    )

    private val NATURE_NAMES = arrayOf(
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky",
    )

    fun resolveSpeciesName(id: Int): String {
        return PokemonSpeciesCatalog.resolveSpeciesName(id)
    }

    private val GEN3_MOVE_NAMES = arrayOf(
        "Pound", "Karate Chop", "Double Slap", "Comet Punch", "Mega Punch",
        "Pay Day", "Fire Punch", "Ice Punch", "Thunder Punch", "Scratch",
        "Vise Grip", "Guillotine", "Razor Wind", "Swords Dance", "Cut",
        "Gust", "Wing Attack", "Whirlwind", "Fly", "Bind",
        "Slam", "Vine Whip", "Stomp", "Double Kick", "Mega Kick",
        "Jump Kick", "Rolling Kick", "Sand Attack", "Headbutt", "Horn Attack",
        "Fury Attack", "Horn Drill", "Tackle", "Body Slam", "Wrap",
        "Take Down", "Thrash", "Double-Edge", "Tail Whip", "Poison Sting",
        "Twineedle", "Pin Missile", "Leer", "Bite", "Growl",
        "Roar", "Sing", "Supersonic", "Sonic Boom", "Disable",
        "Acid", "Ember", "Flamethrower", "Mist", "Water Gun",
        "Hydro Pump", "Surf", "Ice Beam", "Blizzard", "Psybeam",
        "Bubble Beam", "Aurora Beam", "Hyper Beam", "Peck", "Drill Peck",
        "Submission", "Low Kick", "Counter", "Seismic Toss", "Strength",
        "Absorb", "Mega Drain", "Leech Seed", "Growth", "Razor Leaf",
        "Solar Beam", "Poison Powder", "Stun Spore", "Sleep Powder", "Petal Dance",
        "String Shot", "Dragon Rage", "Fire Spin", "Thunder Shock", "Thunderbolt",
        "Thunder Wave", "Thunder", "Rock Throw", "Earthquake", "Fissure",
        "Dig", "Toxic", "Confusion", "Psychic", "Hypnosis",
        "Meditate", "Agility", "Quick Attack", "Rage", "Teleport",
        "Night Shade", "Mimic", "Screech", "Double Team", "Recover",
        "Harden", "Minimize", "Smokescreen", "Confuse Ray", "Withdraw",
        "Defense Curl", "Barrier", "Light Screen", "Haze", "Reflect",
        "Focus Energy", "Bide", "Metronome", "Mirror Move", "Self-Destruct",
        "Egg Bomb", "Lick", "Smog", "Sludge", "Bone Club",
        "Fire Blast", "Waterfall", "Clamp", "Swift", "Skull Bash",
        "Spike Cannon", "Constrict", "Amnesia", "Kinesis", "Soft-Boiled",
        "High Jump Kick", "Glare", "Dream Eater", "Poison Gas", "Barrage",
        "Leech Life", "Lovely Kiss", "Sky Attack", "Transform", "Bubble",
        "Dizzy Punch", "Spore", "Flash", "Psywave", "Splash",
        "Acid Armor", "Crabhammer", "Explosion", "Fury Swipes", "Bonemerang",
        "Rest", "Rock Slide", "Hyper Fang", "Sharpen", "Conversion",
        "Tri Attack", "Super Fang", "Slash", "Substitute", "Struggle",
        "Sketch", "Triple Kick", "Thief", "Spider Web", "Mind Reader",
        "Nightmare", "Flame Wheel", "Snore", "Curse", "Flail",
        "Conversion 2", "Aeroblast", "Cotton Spore", "Reversal", "Spite",
        "Powder Snow", "Protect", "Mach Punch", "Scary Face", "Feint Attack",
        "Sweet Kiss", "Belly Drum", "Sludge Bomb", "Mud-Slap", "Octazooka",
        "Spikes", "Zap Cannon", "Foresight", "Destiny Bond", "Perish Song",
        "Icy Wind", "Detect", "Bone Rush", "Lock-On", "Outrage",
        "Sandstorm", "Giga Drain", "Endure", "Charm", "Rollout",
        "False Swipe", "Swagger", "Milk Drink", "Spark", "Fury Cutter",
        "Steel Wing", "Mean Look", "Attract", "Sleep Talk", "Heal Bell",
        "Return", "Present", "Frustration", "Safeguard", "Pain Split",
        "Sacred Fire", "Magnitude", "Dynamic Punch", "Megahorn", "Dragon Breath",
        "Baton Pass", "Encore", "Pursuit", "Rapid Spin", "Sweet Scent",
        "Iron Tail", "Metal Claw", "Vital Throw", "Morning Sun", "Synthesis",
        "Moonlight", "Hidden Power", "Cross Chop", "Twister", "Rain Dance",
        "Sunny Day", "Crunch", "Mirror Coat", "Psych Up", "Extreme Speed",
        "Ancient Power", "Shadow Ball", "Future Sight", "Rock Smash", "Whirlpool",
        "Beat Up", "Fake Out", "Uproar", "Stockpile", "Spit Up",
        "Swallow", "Heat Wave", "Hail", "Torment", "Flatter",
        "Will-O-Wisp", "Memento", "Facade", "Focus Punch", "Smelling Salts",
        "Follow Me", "Nature Power", "Charge", "Taunt", "Helping Hand",
        "Trick", "Role Play", "Wish", "Assist", "Ingrain",
        "Superpower", "Magic Coat", "Recycle", "Revenge", "Brick Break",
        "Yawn", "Knock Off", "Endeavor", "Eruption", "Skill Swap",
        "Imprison", "Refresh", "Grudge", "Snatch", "Secret Power",
        "Dive", "Arm Thrust", "Camouflage", "Tail Glow", "Luster Purge",
        "Mist Ball", "Feather Dance", "Teeter Dance", "Blaze Kick", "Mud Sport",
        "Ice Ball", "Needle Arm", "Slack Off", "Hyper Voice", "Poison Fang",
        "Crush Claw", "Blast Burn", "Hydro Cannon", "Meteor Mash", "Astonish",
        "Weather Ball", "Aromatherapy", "Fake Tears", "Air Cutter", "Overheat",
        "Odor Sleuth", "Rock Tomb", "Silver Wind", "Metal Sound", "Grass Whistle",
        "Tickle", "Cosmic Power", "Water Spout", "Signal Beam", "Shadow Punch",
        "Extrasensory", "Sky Uppercut", "Sand Tomb", "Sheer Cold", "Muddy Water",
        "Bullet Seed", "Aerial Ace", "Icicle Spear", "Iron Defense", "Block",
        "Howl", "Dragon Claw", "Frenzy Plant", "Bulk Up", "Bounce",
        "Mud Shot", "Poison Tail", "Covet", "Volt Tackle", "Magical Leaf",
        "Water Sport", "Calm Mind", "Leaf Blade", "Dragon Dance", "Rock Blast",
        "Shock Wave", "Water Pulse", "Doom Desire", "Psycho Boost"
    )

    fun resolveMoveName(moveId: Int): String {
        return if (moveId in 1..GEN3_MOVE_NAMES.size) {
            GEN3_MOVE_NAMES[moveId - 1]
        } else {
            "Move #$moveId"
        }
    }

    fun resolveGen3Ability(speciesId: Int, abilityBit: Int): String {
        return when (speciesId) {
            1, 2, 3 -> "Overgrow"
            4, 5, 6 -> "Blaze"
            7, 8, 9 -> "Torrent"
            10, 11, 13, 14 -> "Shield Dust"
            12, 48, 49 -> "Compound Eyes"
            15 -> "Swarm"
            16, 17, 18, 21, 22 -> "Keen Eye"
            19, 20 -> if (abilityBit == 0) "Run Away" else "Guts"
            23, 24 -> if (abilityBit == 0) "Intimidate" else "Shed Skin"
            25, 26 -> "Static"
            27, 28 -> "Sand Veil"
            29, 30, 31, 32, 33, 34 -> "Poison Point"
            35, 36, 39, 40 -> "Cute Charm"
            37, 38 -> "Flash Fire"
            41, 42 -> "Inner Focus"
            43, 44, 45, 69, 70, 71 -> "Chlorophyll"
            46, 47 -> "Effect Spore"
            50, 51 -> if (abilityBit == 0) "Sand Veil" else "Arena Trap"
            52, 53 -> "Pickup"
            54, 55 -> if (abilityBit == 0) "Damp" else "Cloud Nine"
            56, 57 -> "Vital Spirit"
            58, 59 -> if (abilityBit == 0) "Intimidate" else "Flash Fire"
            60, 61, 62 -> if (abilityBit == 0) "Water Absorb" else "Damp"
            63, 64, 65 -> if (abilityBit == 0) "Synchronize" else "Inner Focus"
            66, 67, 68 -> "Guts"
            72, 73 -> if (abilityBit == 0) "Clear Body" else "Liquid Ooze"
            74, 75, 76 -> if (abilityBit == 0) "Rock Head" else "Sturdy"
            77, 78 -> if (abilityBit == 0) "Run Away" else "Flash Fire"
            79, 80 -> if (abilityBit == 0) "Oblivious" else "Own Tempo"
            81, 82 -> if (abilityBit == 0) "Magnet Pull" else "Sturdy"
            83 -> "Keen Eye"
            84, 85 -> if (abilityBit == 0) "Run Away" else "Early Bird"
            86, 87 -> "Thick Fat"
            88, 89 -> if (abilityBit == 0) "Stench" else "Sticky Hold"
            90, 91 -> "Shell Armor"
            92, 93, 94 -> "Levitate"
            95 -> if (abilityBit == 0) "Rock Head" else "Sturdy"
            96, 97 -> "Insomnia"
            98, 99 -> "Hyper Cutter"
            100, 101 -> if (abilityBit == 0) "Soundproof" else "Static"
            102, 103 -> "Chlorophyll"
            104, 105 -> if (abilityBit == 0) "Rock Head" else "Lightning Rod"
            106 -> "Limber"
            107 -> "Keen Eye"
            108 -> "Own Tempo"
            109, 110 -> "Levitate"
            111, 112 -> if (abilityBit == 0) "Lightning Rod" else "Rock Head"
            113 -> if (abilityBit == 0) "Natural Cure" else "Serene Grace"
            114 -> "Chlorophyll"
            115 -> "Early Bird"
            116, 117 -> "Swift Swim"
            118, 119 -> if (abilityBit == 0) "Swift Swim" else "Water Veil"
            120, 121 -> if (abilityBit == 0) "Illuminate" else "Natural Cure"
            122 -> "Soundproof"
            123 -> "Swarm"
            124 -> "Oblivious"
            125 -> "Static"
            126 -> "Flame Body"
            127 -> "Hyper Cutter"
            128 -> "Intimidate"
            129 -> "Swift Swim"
            130 -> "Intimidate"
            131 -> if (abilityBit == 0) "Water Absorb" else "Shell Armor"
            132 -> "Limber"
            133 -> "Run Away"
            134 -> "Water Absorb"
            135 -> "Volt Absorb"
            136 -> "Flash Fire"
            137 -> "Trace"
            138, 139, 140, 141 -> if (abilityBit == 0) "Swift Swim" else "Battle Armor"
            142 -> if (abilityBit == 0) "Rock Head" else "Pressure"
            143 -> if (abilityBit == 0) "Immunity" else "Thick Fat"
            144, 145, 146, 150 -> "Pressure"
            147, 148, 149 -> "Shed Skin"
            151 -> "Synchronize"
            252, 253, 254 -> "Overgrow"
            255, 256, 257 -> "Blaze"
            258, 259, 260 -> "Torrent"
            261, 262 -> if (abilityBit == 0) "Run Away" else "Intimidate"
            263, 264 -> "Pickup"
            276, 277 -> "Guts"
            278, 279 -> "Keen Eye"
            280, 281, 282 -> if (abilityBit == 0) "Synchronize" else "Trace"
            382 -> "Drizzle"
            383 -> "Drought"
            384 -> "Air Lock"
            else -> if (abilityBit == 0) "Standard Ability" else "Secondary Ability"
        }
    }

    fun resolveBallName(ballId: Int): String {
        return when (ballId) {
            1 -> "Master Ball"
            2 -> "Ultra Ball"
            3 -> "Great Ball"
            4 -> "Poke Ball"
            5 -> "Safari Ball"
            6 -> "Net Ball"
            7 -> "Dive Ball"
            8 -> "Nest Ball"
            9 -> "Repeat Ball"
            10 -> "Timer Ball"
            11 -> "Luxury Ball"
            12 -> "Premier Ball"
            else -> "Poke Ball"
        }
    }

    fun resolveItemName(itemId: Int): String {
        return when (itemId) {
            0 -> "None"
            13 -> "Potion"
            14 -> "Antidote"
            15 -> "Burn Heal"
            16 -> "Ice Heal"
            17 -> "Awakening"
            18 -> "Parlyz Heal"
            19 -> "Full Restore"
            20 -> "Max Potion"
            21 -> "Hyper Potion"
            22 -> "Super Potion"
            23 -> "Full Heal"
            24 -> "Revive"
            25 -> "Max Revive"
            133 -> "Cheri Berry"
            134 -> "Chesto Berry"
            135 -> "Pecha Berry"
            136 -> "Rawst Berry"
            137 -> "Aspear Berry"
            138 -> "Leppa Berry"
            139 -> "Oran Berry"
            140 -> "Sitrus Berry"
            141 -> "Lum Berry"
            175 -> "Macho Brace"
            179 -> "Leftovers"
            197 -> "Scope Lens"
            198 -> "Quick Claw"
            205 -> "Choice Band"
            214 -> "Focus Band"
            236 -> "Light Ball"
            else -> "Item #$itemId"
        }
    }

    data class ParsedGen3Pokemon(
        val summary: PokemonSummaryDto,
        val details: PokemonDetailsDto,
    )

    fun isGen3Save(saveBytes: ByteArray?): Boolean {
        if (saveBytes == null || saveBytes.size < SLOT_SIZE) return false
        if (Gen1NativeSaveParser.isGen1Save(saveBytes)) return false
        if (Gen2NativeSaveParser.isGen2Save(saveBytes)) return false
        if (parseSlotSections(saveBytes, 0)?.all { it != null } == true) return true
        if (saveBytes.size >= SLOT_SIZE * 2 &&
            parseSlotSections(saveBytes, SLOT_SIZE)?.all { it != null } == true
        ) {
            return true
        }
        return false
    }

    fun parse(
        saveBytes: ByteArray,
        gameTitle: String = "FireRed",
        gameId: String = "",
    ): PokemonGameSaveDto? {
        if (!isGen3Save(saveBytes)) return null

        val sections = extractActiveSections(saveBytes) ?: return null

        // Section 0: Trainer Info
        val s0 = sections[0] ?: return null
        val trainerName = decodeGen3String(s0, 0, 7)
        val gender = if (s0[8].toInt() == 1) "Girl" else "Boy"
        val tid = (s0[10].toInt() and 0xFF) or ((s0[11].toInt() and 0xFF) shl 8)
        val sid = (s0[12].toInt() and 0xFF) or ((s0[13].toInt() and 0xFF) shl 8)

        // Security key for money in Emerald / FRLG
        var secKey = 0
        if (s0.size >= 0x0AFC) {
            secKey = ByteBuffer.wrap(s0, 0x0AF8, 4).order(ByteOrder.LITTLE_ENDIAN).int
        }
        val rawMoney = ByteBuffer.wrap(s0, 0x0290, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val money = max(0, if (secKey != 0) rawMoney xor secKey else rawMoney)

        // Section 1: Team / Party
        val partyList = mutableListOf<PokemonSummaryDto>()
        val pokemonDetails = mutableMapOf<String, PokemonDetailsDto>()
        val s1 = sections[1]
        if (s1 != null && s1.size >= 0x0290) {
            val partyCount = min(6, max(0, s1[0x0034].toInt() and 0xFF))
            for (p in 0 until partyCount) {
                val offset = 0x0038 + (p * 100)
                if (offset + 100 <= s1.size) {
                    val pkm = parsePokemon(s1, offset, isParty = true, partySlot = p + 1, boxIndex = null, gameTitle = gameTitle)
                    if (pkm != null) {
                        partyList.add(pkm.summary)
                        pokemonDetails[pkm.summary.id] = pkm.details
                    }
                }
            }
        }

        // Sections 5..13: PC Storage Boxes (1..14)
        val pcStream = assemblePcStream(sections)
        val boxesList = mutableListOf<PokemonBoxDto>()

        for (b in 1..14) {
            val boxEntries = mutableListOf<PokemonSummaryDto>()
            val boxStart = (b - 1) * 30 * 80

            if (pcStream != null && boxStart + (30 * 80) <= pcStream.size) {
                for (s in 0 until 30) {
                    val offset = boxStart + (s * 80)
                    val pkm = parsePokemon(pcStream, offset, isParty = false, partySlot = s + 1, boxIndex = b, gameTitle = gameTitle)
                    if (pkm != null) {
                        boxEntries.add(pkm.summary)
                        pokemonDetails[pkm.summary.id] = pkm.details
                    }
                }
            }

            boxesList.add(
                PokemonBoxDto(
                    boxIndex = b,
                    name = "Box $b",
                    capacity = 30,
                    occupiedCount = boxEntries.size,
                    entries = boxEntries,
                )
            )
        }

        val totalCount = partyList.size + boxesList.sumOf { it.occupiedCount }
        val shinyCount = partyList.count { it.isShiny } + boxesList.sumOf { it.entries.count { p -> p.isShiny } }

        // Pokedex bitfields in Section 0 (0x0028 caught, 0x005C seen, 49 bytes each)
        val caughtIds = mutableSetOf<Int>()
        val seenIds = mutableSetOf<Int>()
        val sec0 = sections[0]
        if (sec0 != null && totalCount > 0) {
            val cBytes = (0 until 49).map { offset ->
                val cIdx = 0x0028 + offset
                if (cIdx < sec0.size) sec0[cIdx].toInt() and 0xFF else 0
            }
            // Sanity check: Erased flash / corrupt memory is filled with 0xFF bytes
            val isErasedOrCorrupt = cBytes.all { it == 0xFF } || (cBytes.count { it == 0xFF } >= 20)
            if (!isErasedOrCorrupt) {
                for (offset in 0 until 49) {
                    val cIdx = 0x0028 + offset
                    val sIdx = 0x005C + offset
                    val cByte = if (cIdx < sec0.size) sec0[cIdx].toInt() and 0xFF else 0
                    val sByte = if (sIdx < sec0.size) sec0[sIdx].toInt() and 0xFF else 0
                    for (bit in 0 until 8) {
                        val speciesNum = offset * 8 + bit + 1
                        if (speciesNum in 1..386) {
                            if ((cByte and (1 shl bit)) != 0) {
                                caughtIds.add(speciesNum)
                                seenIds.add(speciesNum)
                            }
                            if ((sByte and (1 shl bit)) != 0) {
                                seenIds.add(speciesNum)
                            }
                        }
                    }
                }
            }
        }

        for (p in partyList) {
            if (p.speciesId > 0) {
                caughtIds.add(p.speciesId)
                seenIds.add(p.speciesId)
            }
        }
        for (b in boxesList) {
            for (p in b.entries) {
                if (p.speciesId > 0) {
                    caughtIds.add(p.speciesId)
                    seenIds.add(p.speciesId)
                }
            }
        }

        return PokemonGameSaveDto(
            gameId = gameId,
            title = gameTitle,
            platform = "gba",
            generation = 3,
            trainerName = trainerName.ifBlank { "Trainer" },
            trainerId = tid.toString(),
            money = money,
            pokedexSeen = seenIds.size,
            pokedexCaught = caughtIds.size,
            saveFound = true,
            providerAvailable = true,
            party = partyList,
            boxes = boxesList,
            totalPokemonCount = totalCount,
            shinyCount = shinyCount,
            pokemonDetails = pokemonDetails,
            caughtSpeciesIds = caughtIds.sorted(),
            seenSpeciesIds = seenIds.sorted(),
            gymBadges = PokemonGymBadgeCatalog.forGen3(gameKey(gameId, gameTitle), readBadgeFlags(sections, gameId, gameTitle)),
        )
    }

    private fun readBadgeFlags(sections: Array<ByteArray?>, gameId: String, gameTitle: String): Int {
        val sec2 = sections.getOrNull(2) ?: return 0
        val key = gameKey(gameId, gameTitle)
        val isFrLg = key.contains("firered") ||
            key.contains("fire_red") ||
            key.contains("fire red") ||
            key.contains("leafgreen") ||
            key.contains("leaf_green") ||
            key.contains("leaf green")
        val isEmerald = key.contains("emerald")

        if (isFrLg) {
            return if (sec2.size > 0x64) sec2[0x64].toInt() and 0xFF else 0
        }

        val firstFlagOffset = if (isEmerald) 0x3FC else 0x3A0
        if (sec2.size <= firstFlagOffset + 1) return 0

        val first = sec2[firstFlagOffset].toInt() and 0xFF
        val second = sec2[firstFlagOffset + 1].toInt() and 0xFF
        return ((first ushr 7) and 0x01) or ((second and 0x7F) shl 1)
    }

    private fun gameKey(gameId: String, gameTitle: String): String =
        pokemonGameKey(gameId, gameTitle)

    private fun extractActiveSections(saveBytes: ByteArray): Array<ByteArray?>? {
        val slot0 = parseSlotSections(saveBytes, 0)
        val slot1 = if (saveBytes.size >= SLOT_SIZE * 2) parseSlotSections(saveBytes, SLOT_SIZE) else null

        val counter0 = getSlotMaxSaveIndex(slot0)
        val counter1 = if (slot1 != null) getSlotMaxSaveIndex(slot1) else -1L

        return if (counter1 > counter0 && slot1 != null) slot1 else slot0
    }

    private fun parseSlotSections(buffer: ByteArray, slotOffset: Int): Array<ByteArray?>? {
        if (slotOffset + SLOT_SIZE > buffer.size) return null

        val sections = arrayOfNulls<ByteArray>(SECTIONS_PER_SLOT)
        for (i in 0 until SECTIONS_PER_SLOT) {
            val secOffset = slotOffset + (i * SECTION_SIZE)
            val footerOffset = secOffset + 4084

            val sectionId = (buffer[footerOffset].toInt() and 0xFF) or ((buffer[footerOffset + 1].toInt() and 0xFF) shl 8)
            val signature = ByteBuffer.wrap(buffer, secOffset + 4088, 4).order(ByteOrder.LITTLE_ENDIAN).int

            if (sectionId in 0 until SECTIONS_PER_SLOT && signature == GEN3_SIGNATURE) {
                val secData = ByteArray(SECTION_SIZE)
                System.arraycopy(buffer, secOffset, secData, 0, SECTION_SIZE)
                sections[sectionId] = secData
            }
        }

        return sections
    }

    private fun getSlotMaxSaveIndex(sections: Array<ByteArray?>?): Long {
        if (sections == null) return -1L
        var maxIndex = -1L
        for (s in sections) {
            if (s != null && s.size >= SECTION_SIZE) {
                val idx = ByteBuffer.wrap(s, 4092, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                if (idx > maxIndex) maxIndex = idx
            }
        }
        return maxIndex
    }

    private fun assemblePcStream(sections: Array<ByteArray?>): ByteArray? {
        val s5 = sections[5] ?: return null
        val pcStream = ByteArray(33600)
        var written = 0

        // Section 5 starts at offset 4
        val s5Len = min(3968 - 4, 33600)
        System.arraycopy(s5, 4, pcStream, 0, s5Len)
        written += s5Len

        for (s in 6..13) {
            val sec = sections[s] ?: break
            val toCopy = min(3968, 33600 - written)
            if (toCopy <= 0) break
            System.arraycopy(sec, 0, pcStream, written, toCopy)
            written += toCopy
        }

        return pcStream
    }

    private fun parsePokemon(
        buffer: ByteArray,
        offset: Int,
        isParty: Boolean,
        partySlot: Int,
        boxIndex: Int?,
        gameTitle: String,
    ): ParsedGen3Pokemon? {
        if (offset + 80 > buffer.size) return null

        val bb = ByteBuffer.wrap(buffer, offset, 80).order(ByteOrder.LITTLE_ENDIAN)
        val pid = bb.int.toLong() and 0xFFFFFFFFL
        val otid = bb.int.toLong() and 0xFFFFFFFFL
        val tid = (otid and 0xFFFFL).toInt()
        val sid = ((otid ushr 16) and 0xFFFFL).toInt()

        if (pid == 0L && otid == 0L) return null

        val rawChecksum = (buffer[offset + 28].toInt() and 0xFF) or ((buffer[offset + 29].toInt() and 0xFF) shl 8)

        // Decrypt 48 bytes (offset 32..79)
        val key = pid xor otid
        val decrypted = ByteArray(48)
        val decBb = ByteBuffer.wrap(decrypted).order(ByteOrder.LITTLE_ENDIAN)

        var runningSum = 0
        for (i in 0 until 12) {
            val encWord = ByteBuffer.wrap(buffer, offset + 32 + (i * 4), 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
            val decWord = encWord xor key
            decBb.putInt(decWord.toInt())

            val w1 = (decWord and 0xFFFFL).toInt()
            val w2 = ((decWord ushr 16) and 0xFFFFL).toInt()
            runningSum = (runningSum + w1 + w2) and 0xFFFF
        }

        // Substructure order permutation
        val orderIdx = (pid % 24).toInt()
        val order = BLOCK_ORDERS[orderIdx]

        var gOffset = 0
        var aOffset = 0
        var eOffset = 0
        var mOffset = 0

        for (i in 0 until 4) {
            when (order[i]) {
                0 -> gOffset = i * 12
                1 -> aOffset = i * 12
                2 -> eOffset = i * 12
                3 -> mOffset = i * 12
            }
        }

        val speciesId = (decrypted[gOffset].toInt() and 0xFF) or ((decrypted[gOffset + 1].toInt() and 0xFF) shl 8)
        if (speciesId <= 0 || speciesId > 412) return null

        // Substructure Growth
        val heldItemId = (decrypted[gOffset + 2].toInt() and 0xFF) or ((decrypted[gOffset + 3].toInt() and 0xFF) shl 8)
        val experience = ByteBuffer.wrap(decrypted, gOffset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
        val friendship = decrypted[gOffset + 9].toInt() and 0xFF

        // Substructure Attacks
        val m1 = (decrypted[aOffset].toInt() and 0xFF) or ((decrypted[aOffset + 1].toInt() and 0xFF) shl 8)
        val m2 = (decrypted[aOffset + 2].toInt() and 0xFF) or ((decrypted[aOffset + 3].toInt() and 0xFF) shl 8)
        val m3 = (decrypted[aOffset + 4].toInt() and 0xFF) or ((decrypted[aOffset + 5].toInt() and 0xFF) shl 8)
        val m4 = (decrypted[aOffset + 6].toInt() and 0xFF) or ((decrypted[aOffset + 7].toInt() and 0xFF) shl 8)
        val movesList = listOf(m1, m2, m3, m4).filter { it > 0 }.map { resolveMoveName(it) }

        // Substructure EVs
        val hpEv = decrypted[eOffset].toInt() and 0xFF
        val atkEv = decrypted[eOffset + 1].toInt() and 0xFF
        val defEv = decrypted[eOffset + 2].toInt() and 0xFF
        val speEv = decrypted[eOffset + 3].toInt() and 0xFF
        val spaEv = decrypted[eOffset + 4].toInt() and 0xFF
        val spdEv = decrypted[eOffset + 5].toInt() and 0xFF
        val evStats = PokemonStatsDto(
            hp = hpEv,
            attack = atkEv,
            defense = defEv,
            speed = speEv,
            specialAttack = spaEv,
            specialDefense = spdEv,
        )

        // Substructure Misc (IVs & ability)
        val ivData = ByteBuffer.wrap(decrypted, mOffset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
        val isEgg = ((ivData ushr 30) and 1L) == 1L
        if (isEgg) return null

        val hpIv = (ivData and 0x1FL).toInt()
        val atkIv = ((ivData ushr 5) and 0x1FL).toInt()
        val defIv = ((ivData ushr 10) and 0x1FL).toInt()
        val speIv = ((ivData ushr 15) and 0x1FL).toInt()
        val spaIv = ((ivData ushr 20) and 0x1FL).toInt()
        val spdIv = ((ivData ushr 25) and 0x1FL).toInt()
        val ivStats = PokemonStatsDto(
            hp = hpIv,
            attack = atkIv,
            defense = defIv,
            speed = speIv,
            specialAttack = spaIv,
            specialDefense = spdIv,
        )

        val abilityBit = ((ivData ushr 31) and 1L).toInt()
        val abilityName = resolveGen3Ability(speciesId, abilityBit)

        val origins = (decrypted[mOffset + 2].toInt() and 0xFF) or ((decrypted[mOffset + 3].toInt() and 0xFF) shl 8)
        val ballId = (origins ushr 11) and 0x0F
        val pokeballName = resolveBallName(ballId)

        val heldItemName = if (heldItemId > 0) resolveItemName(heldItemId) else null

        val rawNickname = decodeGen3String(buffer, offset + 8, 10)
        val otName = decodeGen3String(buffer, offset + 20, 7)
        val speciesName = resolveSpeciesName(speciesId)
        val nickname = rawNickname.ifBlank { speciesName }

        var currentHp: Int? = null
        var maxHp: Int? = null
        val level = if (isParty && offset + 100 <= buffer.size) {
            val partyLvl = buffer[offset + 84].toInt() and 0xFF
            currentHp = (buffer[offset + 86].toInt() and 0xFF) or ((buffer[offset + 87].toInt() and 0xFF) shl 8)
            maxHp = (buffer[offset + 88].toInt() and 0xFF) or ((buffer[offset + 89].toInt() and 0xFF) shl 8)
            if (partyLvl in 1..100) partyLvl else calculateLevelFromExp(experience)
        } else {
            calculateLevelFromExp(experience)
        }

        val pidLow = (pid and 0xFFFFL).toInt()
        val pidHigh = ((pid ushr 16) and 0xFFFFL).toInt()
        val isShiny = ((tid xor sid) xor (pidLow xor pidHigh)) < 8

        val pidByte = (pid and 0xFFL).toInt()
        val gender = if (pidByte < 31) "Female" else "Male"

        val pkmId = "gen3_${speciesId}_%08x".format(pid)

        val unownLetter = if (speciesId == 201) {
            val unownIdx = (((pid and 3L) or ((pid ushr 8 and 3L) shl 2) or ((pid ushr 16 and 3L) shl 4) or ((pid ushr 24 and 3L) shl 6)) % 28L).toInt()
            when (unownIdx) {
                in 0..25 -> ('A'.code + unownIdx).toChar().toString()
                26 -> "!"
                27 -> "?"
                else -> "A"
            }
        } else null

        val summary = PokemonSummaryDto(
            id = pkmId,
            species = speciesName,
            speciesId = speciesId,
            form = unownLetter,
            nickname = nickname,
            level = level,
            gender = gender,
            isShiny = isShiny,
            originalTrainer = otName.ifBlank { "Trainer" },
            originalTrainerId = tid.toString(),
            originGame = gameTitle,
            currentLocation = if (isParty) "Party" else "Box $boxIndex",
            boxIndex = boxIndex,
            slotIndex = partySlot,
            isInParty = isParty,
            legalityStatus = "valid",
        )

        val details = PokemonDetailsDto(
            summary = summary,
            nature = NATURE_NAMES[(pid % 25).toInt()],
            ability = abilityName,
            heldItem = heldItemName,
            moves = movesList,
            iv = ivStats,
            ev = evStats,
            currentHp = currentHp,
            maxHp = maxHp,
            friendship = friendship,
            pokeball = pokeballName,
            legalityStatus = "valid",
        )

        return ParsedGen3Pokemon(summary = summary, details = details)
    }

    private fun calculateLevelFromExp(exp: Long): Int {
        if (exp <= 0L) return 5
        val approx = cbrt(exp.toDouble()).toInt()
        return max(1, min(100, approx))
    }

    private fun decodeGen3String(buffer: ByteArray, offset: Int, length: Int): String {
        val sb = StringBuilder()
        for (i in 0 until length) {
            if (offset + i >= buffer.size) break
            val b = buffer[offset + i].toInt() and 0xFF
            if (b == 0xFF) break
            when (b) {
                0x00 -> sb.append(' ')
                in 0xA1..0xAA -> sb.append(('0' + (b - 0xA1)))
                in 0xBB..0xD4 -> sb.append(('A' + (b - 0xBB)))
                in 0xD5..0xEE -> sb.append(('a' + (b - 0xD5)))
                0xAB -> sb.append('!')
                0xAC -> sb.append('?')
                0xAD -> sb.append('.')
                0xAE -> sb.append('-')
                0xB5 -> sb.append('♀')
                0xB6 -> sb.append('♂')
            }
        }
        return sb.toString().trim()
    }
}

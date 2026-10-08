package dev.vantafyn.core.jellyfin

import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin native Gen 7 save parser (.sav / .srm / main) for Nintendo 3DS Pokémon titles:
 * Pokémon Sun, Pokémon Moon, Pokémon Ultra Sun, and Pokémon Ultra Moon.
 *
 * Fully autonomous: does not require external PKVault container services or HTTP servers.
 */
object Gen7NativeSaveParser {

    private const val SAVE_MIN_SIZE = 0x6BE00 // 441,856 bytes (SM raw save size)
    private const val USUM_SAVE_MIN_SIZE = 0x6CC00 // 445,440 bytes (USUM raw save size)

    // Sun & Moon offsets
    private const val SM_ZCRYSTAL_OFFSET = 0x00D68
    private const val SM_STATUS_OFFSET = 0x01200
    private const val SM_PARTY_OFFSET = 0x01400
    private const val SM_PARTY_COUNT_OFFSET = 0x01A18
    private const val SM_ZUKAN_OFFSET = 0x02A00
    private const val SM_ZUKAN_CAUGHT_OFFSET = 0x02A88
    private const val SM_ZUKAN_SEEN_OFFSET = 0x02AF0
    private const val SM_MISC_OFFSET = 0x04000
    private const val SM_BOX_OFFSET = 0x04E00

    // Ultra Sun & Ultra Moon offsets
    private const val USUM_ZCRYSTAL_OFFSET = 0x00D70
    private const val USUM_STATUS_OFFSET = 0x01400
    private const val USUM_PARTY_OFFSET = 0x01600
    private const val USUM_PARTY_COUNT_OFFSET = 0x01C18
    private const val USUM_ZUKAN_OFFSET = 0x02C00
    private const val USUM_ZUKAN_CAUGHT_OFFSET = 0x02C88
    private const val USUM_ZUKAN_SEEN_OFFSET = 0x02CF0
    private const val USUM_MISC_OFFSET = 0x04400
    private const val USUM_BOX_OFFSET = 0x05200

    private const val BOX_COUNT = 32
    private const val SLOTS_PER_BOX = 30
    private const val BOX_SLOT_SIZE = 232
    private const val PARTY_SLOT_SIZE = 260
    private const val GEN7_SPECIES_COUNT = 807

    private const val BEEF_SIGNATURE = 0x42454546L // "BEEF" footer magic

    private val BLOCK_ORDERS = arrayOf(
        intArrayOf(0, 1, 2, 3), // 00: ABCD
        intArrayOf(0, 1, 3, 2), // 01: ABDC
        intArrayOf(0, 2, 1, 3), // 02: ACBD
        intArrayOf(0, 2, 3, 1), // 03: ACDB
        intArrayOf(0, 3, 1, 2), // 04: ADBC
        intArrayOf(0, 3, 2, 1), // 05: ADCB
        intArrayOf(1, 0, 2, 3), // 06: BACD
        intArrayOf(1, 0, 3, 2), // 07: BADC
        intArrayOf(1, 2, 0, 3), // 08: BCAD
        intArrayOf(1, 2, 3, 0), // 09: BCDA
        intArrayOf(1, 3, 0, 2), // 10: BDAC
        intArrayOf(1, 3, 2, 0), // 11: BDCA
        intArrayOf(2, 0, 1, 3), // 12: CABD
        intArrayOf(2, 0, 3, 1), // 13: CADB
        intArrayOf(2, 1, 0, 3), // 14: CBAD
        intArrayOf(2, 1, 3, 0), // 15: CBDA
        intArrayOf(2, 3, 0, 1), // 16: CDAB
        intArrayOf(2, 3, 1, 0), // 17: CDBA
        intArrayOf(3, 0, 1, 2), // 18: DABC
        intArrayOf(3, 0, 2, 1), // 19: DACB
        intArrayOf(3, 1, 0, 2), // 20: DBAC
        intArrayOf(3, 1, 2, 0), // 21: DBCA
        intArrayOf(3, 2, 0, 1), // 22: DCAB
        intArrayOf(3, 2, 1, 0), // 23: DCBA
    )

    private val NATURE_NAMES = arrayOf(
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky",
    )

    fun isGen7Save(saveBytes: ByteArray?, gameTitle: String = ""): Boolean {
        if (saveBytes == null || saveBytes.size < SAVE_MIN_SIZE) return false
        val lower = gameTitle.lowercase()
        if (isGen7Title(lower)) return true
        return hasBeefSignature(saveBytes)
    }

    private fun isGen7Title(title: String): Boolean {
        if (title.contains("ultra sun") || title.contains("ultra moon") || title.contains("usum")) return true
        if (title.contains("pokemon sun") || title.contains("pokémon sun") ||
            title.contains("pokemon moon") || title.contains("pokémon moon")) return true
        val words = title.split(Regex("[^a-z0-9]")).filter { it.isNotBlank() }
        return words.contains("sun") || words.contains("moon")
    }

    private fun isUsumTitle(title: String): Boolean =
        title.contains("ultra sun") ||
        title.contains("ultra moon") ||
        title.contains("usum")

    private fun isSmTitle(title: String): Boolean =
        (title.contains("sun") || title.contains("moon")) && !isUsumTitle(title)

    private fun hasBeefSignature(saveBytes: ByteArray): Boolean {
        if (saveBytes.size < 0x200) return false
        if (saveBytes.size >= 0x1F0 && readUInt32LE(saveBytes, saveBytes.size - 0x1F0) == BEEF_SIGNATURE) {
            return true
        }
        val start = (saveBytes.size - 0x200).coerceAtLeast(0)
        for (i in start until saveBytes.size - 4 step 4) {
            if (readUInt32LE(saveBytes, i) == BEEF_SIGNATURE) return true
        }
        return false
    }

    private fun readUInt32LE(data: ByteArray, offset: Int): Long {
        if (offset + 4 > data.size || offset < 0) return 0xFFFFFFFFL
        return ((data[offset].toLong() and 0xFF)) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    private fun readUInt16LE(data: ByteArray, offset: Int): Int {
        if (offset + 2 > data.size || offset < 0) return 0
        return ((data[offset].toInt() and 0xFF)) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun resolveZCrystalSlug(id: Int): String? = when (id) {
        1831, 776, 1 -> "normalium-z"
        1832, 777, 2 -> "firium-z"
        1833, 778, 3 -> "waterium-z"
        1834, 779, 4 -> "electrium-z"
        1835, 780, 5 -> "grassium-z"
        1836, 781, 6 -> "icium-z"
        1837, 782, 7 -> "fightinium-z"
        1838, 783, 8 -> "poisonium-z"
        1839, 784, 9 -> "groundium-z"
        1840, 785, 10 -> "flyinium-z"
        1841, 786, 11 -> "psychium-z"
        1842, 787, 12 -> "buginium-z"
        1843, 788, 13 -> "rockium-z"
        1844, 789, 14 -> "ghostium-z"
        1845, 790, 15 -> "dragonium-z"
        1846, 791, 16 -> "darkinium-z"
        1847, 792, 17 -> "steelium-z"
        1848, 793, 18 -> "fairium-z"
        1849, 794, 19 -> "pikanium-z"
        1850, 795, 20 -> "decidium-z"
        1851, 796, 21 -> "incinium-z"
        1852, 797, 22 -> "primarium-z"
        1853, 798, 23 -> "tapunium-z"
        1854, 799, 24 -> "marshadium-z"
        1855, 800, 25 -> "aloraichium-z"
        1856, 801, 26 -> "snorlium-z"
        1857, 802, 27 -> "eevium-z"
        1858, 803, 28 -> "mewnium-z"
        1859, 804, 29 -> "pikashunium-z"
        1951, 921, 30 -> "solganium-z"
        1952, 922, 31 -> "lunalium-z"
        1953, 923, 32 -> "ultranecrozium-z"
        1954, 924, 33 -> "mimikium-z"
        1955, 925, 34 -> "lycanium-z"
        1956, 926, 35 -> "kommonium-z"
        else -> null
    }

    data class ParsedGen7Pokemon(
        val summary: PokemonSummaryDto,
        val details: PokemonDetailsDto,
        val heldItemId: Int = 0,
    )

    fun parse(saveBytes: ByteArray?, gameTitle: String = "Pokemon Gen 7", gameId: String = ""): PokemonGameSaveDto? {
        if (saveBytes == null || saveBytes.size < SAVE_MIN_SIZE) return null

        val titleLower = gameTitle.lowercase()
        val isUsum = when {
            isUsumTitle(titleLower) -> true
            isSmTitle(titleLower) -> false
            else -> saveBytes.size == USUM_SAVE_MIN_SIZE
        }

        val statusOffset = if (isUsum) USUM_STATUS_OFFSET else SM_STATUS_OFFSET
        val partyOffset = if (isUsum) USUM_PARTY_OFFSET else SM_PARTY_OFFSET
        val partyCountOffset = if (isUsum) USUM_PARTY_COUNT_OFFSET else SM_PARTY_COUNT_OFFSET
        val zukanOffset = if (isUsum) USUM_ZUKAN_OFFSET else SM_ZUKAN_OFFSET
        val zukanCaughtOffset = if (isUsum) USUM_ZUKAN_CAUGHT_OFFSET else SM_ZUKAN_CAUGHT_OFFSET
        val zukanSeenOffset = if (isUsum) USUM_ZUKAN_SEEN_OFFSET else SM_ZUKAN_SEEN_OFFSET
        val miscOffset = if (isUsum) USUM_MISC_OFFSET else SM_MISC_OFFSET
        val boxOffset = if (isUsum) USUM_BOX_OFFSET else SM_BOX_OFFSET

        // Trainer status
        val tid = if (statusOffset + 2 <= saveBytes.size) readUInt16LE(saveBytes, statusOffset) else 0
        val sid = if (statusOffset + 4 <= saveBytes.size) readUInt16LE(saveBytes, statusOffset + 2) else 0
        val genderFlag = if (statusOffset + 6 <= saveBytes.size) saveBytes[statusOffset + 5].toInt() and 0xFF else 0
        val trainerGender = if (genderFlag == 1) "F" else "M"
        val otName = if (statusOffset + 0x38 + 24 <= saveBytes.size) {
            decodeUtf16String(saveBytes, statusOffset + 0x38, 12).ifBlank { "TRAINER" }
        } else "TRAINER"

        // Misc info (money & stamps)
        val money = if (miscOffset + 8 <= saveBytes.size) {
            readUInt32LE(saveBytes, miscOffset + 4).toInt().coerceIn(0, 9999999)
        } else 0
        val rawStamps = if (miscOffset + 10 <= saveBytes.size) {
            readUInt16LE(saveBytes, miscOffset + 8)
        } else 0
        val stampFlags = (rawStamps ushr 4) and 0x7FFF

        // Z-Crystals pocket
        val zCrystalOffset = if (isUsum) USUM_ZCRYSTAL_OFFSET else SM_ZCRYSTAL_OFFSET
        val zCrystalSlotCount = if (isUsum) 35 else 29
        val unlockedZCrystals = mutableSetOf<String>()

        if (zCrystalOffset + (zCrystalSlotCount * 4) <= saveBytes.size) {
            for (slot in 0 until zCrystalSlotCount) {
                val offset = zCrystalOffset + (slot * 4)
                val itemId = readUInt16LE(saveBytes, offset)
                if (itemId > 0) {
                    val slug = resolveZCrystalSlug(itemId) ?: resolveZCrystalSlug(slot + 1)
                    if (slug != null) {
                        unlockedZCrystals.add(slug)
                    }
                }
            }
        }

        // Pokédex flags
        val caughtSpeciesIds = mutableListOf<Int>()
        val seenSpeciesIds = sortedSetOf<Int>()

        if (zukanOffset + 0x300 <= saveBytes.size) {
            for (speciesId in 1..GEN7_SPECIES_COUNT) {
                val bitIndex = speciesId - 1
                val byteOffset = zukanCaughtOffset + (bitIndex / 8)
                if (byteOffset < saveBytes.size) {
                    val byteVal = saveBytes[byteOffset].toInt() and 0xFF
                    if ((byteVal and (1 shl (bitIndex % 8))) != 0) {
                        caughtSpeciesIds.add(speciesId)
                        seenSpeciesIds.add(speciesId)
                    }
                }

                // Check 4 seen regions (0x8C bytes each)
                for (r in 0 until 4) {
                    val seenOffset = zukanSeenOffset + (r * 0x8C) + (bitIndex / 8)
                    if (seenOffset < saveBytes.size) {
                        val b = saveBytes[seenOffset].toInt() and 0xFF
                        if ((b and (1 shl (bitIndex % 8))) != 0) {
                            seenSpeciesIds.add(speciesId)
                            break
                        }
                    }
                }
            }
        }

        // 1. Party
        val partyList = mutableListOf<PokemonSummaryDto>()
        val pokemonDetails = mutableMapOf<String, PokemonDetailsDto>()

        val rawPartyCount = if (partyCountOffset + 4 <= saveBytes.size) {
            saveBytes[partyCountOffset].toInt() and 0xFF
        } else 0
        val partyCount = min(6, max(0, rawPartyCount))

        for (slot in 0 until partyCount) {
            val pkOffset = partyOffset + (slot * PARTY_SLOT_SIZE)
            if (pkOffset + PARTY_SLOT_SIZE <= saveBytes.size) {
                val pkm = parsePokemon(
                    data = saveBytes.sliceArray(pkOffset until pkOffset + PARTY_SLOT_SIZE),
                    isParty = true,
                    slot = slot + 1,
                    boxIndex = null,
                    gameTitle = gameTitle,
                )
                if (pkm != null) {
                    partyList.add(pkm.summary)
                    pokemonDetails[pkm.summary.id] = pkm.details
                    if (pkm.heldItemId > 0) {
                        resolveZCrystalSlug(pkm.heldItemId)?.let { unlockedZCrystals.add(it) }
                    }
                }
            }
        }

        // 2. Boxes (32 boxes * 30 slots)
        val boxesList = mutableListOf<PokemonBoxDto>()
        var totalCount = partyList.size
        var shinyCount = partyList.count { it.isShiny }

        for (b in 0 until BOX_COUNT) {
            val boxEntries = mutableListOf<PokemonSummaryDto>()
            val boxBase = boxOffset + (b * SLOTS_PER_BOX * BOX_SLOT_SIZE)

            for (s in 0 until SLOTS_PER_BOX) {
                val pkOffset = boxBase + (s * BOX_SLOT_SIZE)
                if (pkOffset + BOX_SLOT_SIZE <= saveBytes.size) {
                    val pkm = parsePokemon(
                        data = saveBytes.sliceArray(pkOffset until pkOffset + BOX_SLOT_SIZE),
                        isParty = false,
                        slot = s + 1,
                        boxIndex = b + 1,
                        gameTitle = gameTitle,
                    )
                    if (pkm != null) {
                        boxEntries.add(pkm.summary)
                        pokemonDetails[pkm.summary.id] = pkm.details
                        totalCount++
                        if (pkm.summary.isShiny) shinyCount++
                        if (pkm.heldItemId > 0) {
                            resolveZCrystalSlug(pkm.heldItemId)?.let { unlockedZCrystals.add(it) }
                        }
                    }
                }
            }

            boxesList.add(
                PokemonBoxDto(
                    boxIndex = b + 1,
                    name = "Box ${b + 1}",
                    entries = boxEntries,
                    capacity = SLOTS_PER_BOX,
                    occupiedCount = boxEntries.size,
                )
            )
        }

        // Ensure party and box species are reflected in caught/seen
        partyList.forEach { p ->
            if (p.speciesId in 1..GEN7_SPECIES_COUNT) {
                if (!caughtSpeciesIds.contains(p.speciesId)) caughtSpeciesIds.add(p.speciesId)
                seenSpeciesIds.add(p.speciesId)
            }
        }
        boxesList.forEach { box ->
            box.entries.forEach { p ->
                if (p.speciesId in 1..GEN7_SPECIES_COUNT) {
                    if (!caughtSpeciesIds.contains(p.speciesId)) caughtSpeciesIds.add(p.speciesId)
                    seenSpeciesIds.add(p.speciesId)
                }
            }
        }

        val gymBadges = PokemonGymBadgeCatalog.forGen7(
            gameKey = pokemonGameKey(gameId, gameTitle),
            stampFlags = stampFlags,
            unlockedZCrystals = unlockedZCrystals,
            isUsum = isUsum,
        )

        val isUninitialized = partyCount == 0 &&
            boxesList.all { it.occupiedCount == 0 } &&
            caughtSpeciesIds.isEmpty() &&
            stampFlags == 0 &&
            unlockedZCrystals.isEmpty() &&
            money == 0 &&
            tid == 0

        if (isUninitialized) return null

        return PokemonGameSaveDto(
            gameId = gameId,
            title = gameTitle,
            platform = "3ds",
            generation = 7,
            saveFound = true,
            providerAvailable = true,
            party = partyList,
            boxes = boxesList,
            totalPokemonCount = totalCount,
            shinyCount = shinyCount,
            pokemonDetails = pokemonDetails,
            pokedexCaught = caughtSpeciesIds.size,
            pokedexSeen = seenSpeciesIds.size,
            caughtSpeciesIds = caughtSpeciesIds.distinct().sorted(),
            seenSpeciesIds = seenSpeciesIds.toList(),
            gymBadges = gymBadges,
            trainerName = otName,
            trainerId = tid.toString(),
            money = money,
        )
    }

    fun parsePokemon(
        data: ByteArray,
        isParty: Boolean,
        slot: Int,
        boxIndex: Int?,
        gameTitle: String,
    ): ParsedGen7Pokemon? {
        if (data.size < BOX_SLOT_SIZE) return null

        val pv = readUInt32LE(data, 0)
        val chk = readUInt16LE(data, 6)
        if (pv == 0L && chk == 0) return null

        val d = data.copyOf()

        // 1. Decrypt 224 bytes (offset 8..231) using LCRNG seeded with pv
        var seed = pv and 0xFFFFFFFFL
        for (i in 8 until BOX_SLOT_SIZE step 2) {
            seed = (seed * 0x41C64E6DL + 0x6073L) and 0xFFFFFFFFL
            val xor = ((seed ushr 16) and 0xFFFFL).toInt()
            val w = readUInt16LE(d, i) xor xor
            d[i] = (w and 0xFF).toByte()
            d[i + 1] = ((w ushr 8) and 0xFF).toByte()
        }

        // 2. Decrypt party battle stats (offset 232..259) if party data
        if (isParty && d.size >= PARTY_SLOT_SIZE) {
            var partySeed = pv and 0xFFFFFFFFL
            for (i in BOX_SLOT_SIZE until PARTY_SLOT_SIZE step 2) {
                partySeed = (partySeed * 0x41C64E6DL + 0x6073L) and 0xFFFFFFFFL
                val xor = ((partySeed ushr 16) and 0xFFFFL).toInt()
                val w = readUInt16LE(d, i) xor xor
                d[i] = (w and 0xFF).toByte()
                d[i + 1] = ((w ushr 8) and 0xFF).toByte()
            }
        }

        // 3. Unshuffle blocks A, B, C, D (56 bytes each)
        val shiftVal = (((pv ushr 13) and 31L) % 24).toInt()
        val order = BLOCK_ORDERS[shiftVal]

        val unshuffled = ByteArray(d.size)
        System.arraycopy(d, 0, unshuffled, 0, 8)
        for (pos in 0 until 4) {
            val blk = order[pos]
            System.arraycopy(d, 8 + pos * 56, unshuffled, 8 + blk * 56, 56)
        }
        if (d.size > BOX_SLOT_SIZE) {
            System.arraycopy(d, BOX_SLOT_SIZE, unshuffled, BOX_SLOT_SIZE, d.size - BOX_SLOT_SIZE)
        }

        // Block A (offset 8..63)
        val speciesId = readUInt16LE(unshuffled, 8)
        if (speciesId <= 0 || speciesId > GEN7_SPECIES_COUNT) return null

        val heldItem = readUInt16LE(unshuffled, 10)
        val otId = readUInt16LE(unshuffled, 12)
        val secretId = readUInt16LE(unshuffled, 14)
        val exp = readUInt32LE(unshuffled, 16)
        val abilityId = unshuffled[20].toInt() and 0xFF
        val pid = readUInt32LE(unshuffled, 24)
        val natureIndex = (unshuffled[28].toInt() and 0xFF).coerceIn(0, 24)
        val natureName = NATURE_NAMES[natureIndex]

        val encounterByte = unshuffled[29].toInt() and 0xFF
        val genderBits = (encounterByte ushr 1) and 0x03
        val genderStr = when (genderBits) {
            0 -> "M"
            1 -> "F"
            else -> "Genderless"
        }
        val formBits = (encounterByte ushr 3) and 0x1F

        // EVs in Block A (offsets 30..35)
        val hpEv = unshuffled[30].toInt() and 0xFF
        val atkEv = unshuffled[31].toInt() and 0xFF
        val defEv = unshuffled[32].toInt() and 0xFF
        val speEv = unshuffled[33].toInt() and 0xFF
        val spaEv = unshuffled[34].toInt() and 0xFF
        val spdEv = unshuffled[35].toInt() and 0xFF
        val evStats = PokemonStatsDto(
            hp = hpEv.coerceIn(0, 252),
            attack = atkEv.coerceIn(0, 252),
            defense = defEv.coerceIn(0, 252),
            speed = speEv.coerceIn(0, 252),
            specialAttack = spaEv.coerceIn(0, 252),
            specialDefense = spdEv.coerceIn(0, 252),
        )

        // Block B (offset 64..119)
        val rawNick = decodeUtf16String(unshuffled, 64, 12)
        val m1 = readUInt16LE(unshuffled, 90)
        val m2 = readUInt16LE(unshuffled, 92)
        val m3 = readUInt16LE(unshuffled, 94)
        val m4 = readUInt16LE(unshuffled, 96)
        val movesList = listOf(m1, m2, m3, m4)
            .filter { it > 0 }
            .map { PokemonMoveCatalog.resolveMoveName(it) }

        // IVs in Block B (offset 112)
        val iv32 = readUInt32LE(unshuffled, 112)
        val hpIv = (iv32 and 0x1FL).toInt()
        val atkIv = ((iv32 ushr 5) and 0x1FL).toInt()
        val defIv = ((iv32 ushr 10) and 0x1FL).toInt()
        val speIv = ((iv32 ushr 15) and 0x1FL).toInt()
        val spaIv = ((iv32 ushr 20) and 0x1FL).toInt()
        val spdIv = ((iv32 ushr 25) and 0x1FL).toInt()
        val isEgg = ((iv32 ushr 30) and 1L) == 1L

        val ivStats = PokemonStatsDto(
            hp = hpIv.coerceIn(0, 31),
            attack = atkIv.coerceIn(0, 31),
            defense = defIv.coerceIn(0, 31),
            speed = speIv.coerceIn(0, 31),
            specialAttack = spaIv.coerceIn(0, 31),
            specialDefense = spdIv.coerceIn(0, 31),
        )

        // Block D (offset 176..231)
        val otName = decodeUtf16String(unshuffled, 176, 12).ifBlank { "TRAINER" }
        val friendship = unshuffled[200].toInt() and 0xFF

        val speciesName = PokemonSpeciesCatalog.resolveSpeciesName(speciesId)
        val nickname = if (isEgg) "Egg" else rawNick.ifBlank { speciesName }

        // Level & HP from party stats or calculated
        val level: Int
        var currentHp = 0
        var maxHp = 0
        if (isParty && unshuffled.size >= PARTY_SLOT_SIZE) {
            level = (unshuffled[236].toInt() and 0xFF).coerceIn(1, 100)
            currentHp = readUInt16LE(unshuffled, 238)
            maxHp = readUInt16LE(unshuffled, 240)
        } else {
            level = calculateLevelFromExp(exp)
        }

        // Gen 7 shiny calculation: threshold is 16
        val shinyVal = (otId xor secretId) xor ((pid and 0xFFFFL) xor (pid ushr 16)).toInt()
        val isShiny = shinyVal < 16

        val formStr = if (speciesId == 201) {
            when (formBits) {
                in 0..25 -> ('A'.code + formBits).toChar().toString()
                26 -> "!"
                27 -> "?"
                else -> null
            }
        } else if (formBits > 0) {
            "Form $formBits"
        } else null

        val pkmId = "gen7_${speciesId}_${otId}_${slot}_${if (isParty) "p" else "b$boxIndex"}"

        val summary = PokemonSummaryDto(
            id = pkmId,
            species = speciesName,
            speciesId = speciesId,
            form = formStr,
            nickname = nickname,
            level = level,
            gender = genderStr,
            isShiny = isShiny,
            originalTrainer = otName,
            originalTrainerId = otId.toString(),
            originGame = gameTitle,
            currentLocation = if (isParty) "Party" else "Box $boxIndex",
            boxIndex = boxIndex,
            slotIndex = slot,
            isInParty = isParty,
            legalityStatus = "valid",
        )

        val details = PokemonDetailsDto(
            summary = summary,
            nature = natureName,
            ability = if (abilityId > 0) "Ability #$abilityId" else null,
            heldItem = if (heldItem > 0) "Item #$heldItem" else null,
            moves = movesList,
            iv = ivStats,
            ev = evStats,
            currentHp = if (currentHp > 0) currentHp else null,
            maxHp = if (maxHp > 0) maxHp else null,
            friendship = friendship,
            pokeball = "Poké Ball",
            legalityStatus = "valid",
        )

        return ParsedGen7Pokemon(summary, details, heldItem)
    }

    private fun decodeUtf16String(data: ByteArray, offset: Int, maxChars: Int): String {
        val sb = StringBuilder()
        for (i in 0 until maxChars) {
            val charOffset = offset + (i * 2)
            if (charOffset + 2 > data.size) break
            val code = (data[charOffset].toInt() and 0xFF) or ((data[charOffset + 1].toInt() and 0xFF) shl 8)
            if (code == 0 || code == 0xFFFF) break
            sb.append(code.toChar())
        }
        return sb.toString().trim()
    }

    private fun calculateLevelFromExp(exp: Long): Int {
        for (lvl in 100 downTo 1) {
            val required = (lvl * lvl * lvl).toLong()
            if (exp >= required) return lvl
        }
        return 1
    }
}

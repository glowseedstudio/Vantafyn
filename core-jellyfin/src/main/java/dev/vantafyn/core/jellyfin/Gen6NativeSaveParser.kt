package dev.vantafyn.core.jellyfin

import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin native Gen 6 save parser (.sav / .srm / main) for Nintendo 3DS Pokémon titles:
 * Pokémon X, Pokémon Y, Pokémon Omega Ruby, and Pokémon Alpha Sapphire.
 *
 * Fully autonomous: does not require external PKVault container services or HTTP servers.
 */
object Gen6NativeSaveParser {

    private const val SAVE_MIN_SIZE = 0x65600 // 415,232 bytes (XY raw save size)
    private const val ORAS_SAVE_MIN_SIZE = 0x76000 // 483,328 bytes (ORAS raw save size)

    // Save block offsets (relative to raw save partition / main file)
    private const val STATUS_OFFSET = 0x14000
    private const val MISC_OFFSET = 0x04200
    private const val PARTY_OFFSET = 0x14200
    private const val PARTY_COUNT_OFFSET = 0x14818
    private const val ZUKAN_OFFSET = 0x15000
    private const val ZUKAN_CAUGHT_OFFSET = 0x15008
    private const val ZUKAN_SEEN_OFFSET = 0x15068

    private const val XY_BOX_OFFSET = 0x22600
    private const val ORAS_BOX_OFFSET = 0x33000

    private const val BOX_COUNT = 31
    private const val SLOTS_PER_BOX = 30
    private const val BOX_SLOT_SIZE = 232
    private const val PARTY_SLOT_SIZE = 260
    private const val GEN6_SPECIES_COUNT = 721

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

    fun isGen6Save(saveBytes: ByteArray?, gameTitle: String = ""): Boolean {
        if (saveBytes == null || saveBytes.size < SAVE_MIN_SIZE || saveBytes.size > 0x180000) return false
        val lower = gameTitle.lowercase()
        if (isOrasTitle(lower) || isXyTitle(lower)) return true
        return hasBeefSignature(saveBytes)
    }

    private fun isOrasTitle(title: String): Boolean =
        title.contains("omega ruby") ||
        title.contains("alpha sapphire") ||
        title.contains("oras")

    private fun isXyTitle(title: String): Boolean {
        if (title.contains("pokemon x") || title.contains("pokémon x") ||
            title.contains("pokemon y") || title.contains("pokémon y")) return true
        val words = title.split(Regex("[^a-z0-9]")).filter { it.isNotBlank() }
        return words.contains("x") || words.contains("y")
    }

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

    data class ParsedGen6Pokemon(
        val summary: PokemonSummaryDto,
        val details: PokemonDetailsDto,
    )

    fun parse(saveBytes: ByteArray?, gameTitle: String = "Pokemon Gen 6", gameId: String = ""): PokemonGameSaveDto? {
        if (saveBytes == null || saveBytes.size < SAVE_MIN_SIZE || saveBytes.size > 0x180000) return null
        val headerPartyCount = if (PARTY_COUNT_OFFSET + 4 <= saveBytes.size) {
            readUInt32LE(saveBytes, PARTY_COUNT_OFFSET).toInt()
        } else 0
        if (headerPartyCount !in 0..6) return null

        val isOras = isOrasTitle(gameTitle.lowercase()) || saveBytes.size >= ORAS_SAVE_MIN_SIZE
        val boxOffset = if (isOras) ORAS_BOX_OFFSET else XY_BOX_OFFSET

        // Trainer status
        val tid = if (STATUS_OFFSET + 2 <= saveBytes.size) readUInt16LE(saveBytes, STATUS_OFFSET) else 0
        val sid = if (STATUS_OFFSET + 4 <= saveBytes.size) readUInt16LE(saveBytes, STATUS_OFFSET + 2) else 0
        val genderFlag = if (STATUS_OFFSET + 6 <= saveBytes.size) saveBytes[STATUS_OFFSET + 5].toInt() and 0xFF else 0
        val trainerGender = if (genderFlag == 1) "F" else "M"
        val otName = if (STATUS_OFFSET + 0x48 + 24 <= saveBytes.size) {
            decodeUtf16String(saveBytes, STATUS_OFFSET + 0x48, 12).ifBlank { "TRAINER" }
        } else "TRAINER"

        // Misc info (money & badges)
        val money = if (MISC_OFFSET + 12 <= saveBytes.size) {
            readUInt32LE(saveBytes, MISC_OFFSET + 8).toInt().coerceIn(0, 9999999)
        } else 0
        val badgeFlags = if (MISC_OFFSET + 13 <= saveBytes.size) {
            saveBytes[MISC_OFFSET + 0x0C].toInt() and 0xFF
        } else 0
        val gymBadges = PokemonGymBadgeCatalog.forGen6(pokemonGameKey(gameId, gameTitle), badgeFlags)

        // Pokédex flags
        val caughtSpeciesIds = mutableListOf<Int>()
        val seenSpeciesIds = sortedSetOf<Int>()

        if (ZUKAN_OFFSET + 0x200 <= saveBytes.size) {
            for (speciesId in 1..GEN6_SPECIES_COUNT) {
                val bitIndex = speciesId - 1
                val byteOffset = ZUKAN_CAUGHT_OFFSET + (bitIndex / 8)
                if (byteOffset < saveBytes.size) {
                    val byteVal = saveBytes[byteOffset].toInt() and 0xFF
                    if ((byteVal and (1 shl (bitIndex % 8))) != 0) {
                        caughtSpeciesIds.add(speciesId)
                        seenSpeciesIds.add(speciesId)
                    }
                }

                // Check 4 seen regions (0x60 bytes each)
                for (r in 0 until 4) {
                    val seenOffset = ZUKAN_SEEN_OFFSET + (r * 0x60) + (bitIndex / 8)
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

        val rawPartyCount = if (PARTY_COUNT_OFFSET + 4 <= saveBytes.size) {
            saveBytes[PARTY_COUNT_OFFSET].toInt() and 0xFF
        } else 0
        val partyCount = min(6, max(0, rawPartyCount))

        for (slot in 0 until partyCount) {
            val pkOffset = PARTY_OFFSET + (slot * PARTY_SLOT_SIZE)
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
                }
            }
        }

        // 2. Boxes (31 boxes * 30 slots)
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
            if (p.speciesId in 1..GEN6_SPECIES_COUNT) {
                if (!caughtSpeciesIds.contains(p.speciesId)) caughtSpeciesIds.add(p.speciesId)
                seenSpeciesIds.add(p.speciesId)
            }
        }
        boxesList.forEach { box ->
            box.entries.forEach { p ->
                if (p.speciesId in 1..GEN6_SPECIES_COUNT) {
                    if (!caughtSpeciesIds.contains(p.speciesId)) caughtSpeciesIds.add(p.speciesId)
                    seenSpeciesIds.add(p.speciesId)
                }
            }
        }

        val isUninitialized = partyCount == 0 &&
            boxesList.all { it.occupiedCount == 0 } &&
            caughtSpeciesIds.isEmpty() &&
            badgeFlags == 0 &&
            money == 0 &&
            tid == 0

        if (isUninitialized) return null

        return PokemonGameSaveDto(
            gameId = gameId,
            title = gameTitle,
            platform = "3ds",
            generation = 6,
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
    ): ParsedGen6Pokemon? {
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
        if (speciesId <= 0 || speciesId > GEN6_SPECIES_COUNT) return null

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

        // Gen 6 shiny calculation: threshold is 16
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

        val pkmId = "gen6_${speciesId}_${otId}_${slot}_${if (isParty) "p" else "b$boxIndex"}"

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

        return ParsedGen6Pokemon(summary, details)
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
        // Fallback approximation: cube root formula covers Medium Fast (most common)
        for (lvl in 100 downTo 1) {
            val required = (lvl * lvl * lvl).toLong()
            if (exp >= required) return lvl
        }
        return 1
    }
}

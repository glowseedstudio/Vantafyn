package dev.vantafyn.core.jellyfin

import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin native Gen 5 save parser (.sav / .sram / .dsv) for Nintendo DS Pokémon titles:
 * Pokémon Black, White, Black 2, and White 2.
 *
 * Fully autonomous: does not require external PKVault container services or HTTP servers.
 */
object Gen5NativeSaveParser {

    private const val SAVE_COPY_SIZE = 0x24000
    private const val PARTY_OFFSET = 0x18E00
    private const val BOX_OFFSET = 0x400
    private const val BOX_COUNT = 24
    private const val SLOTS_PER_BOX = 30
    private const val PARTY_SLOT_SIZE = 220
    private const val POKEDEX_FLAGS_OFFSET_IN_BLOCK = 0x08
    private const val POKEDEX_FLAG_REGION_SIZE = 0x54
    private const val GEN5_SPECIES_COUNT = 649

    private data class Gen5VariantOffsets(
        val pokedexOffset: Int,
        val pokedexSize: Int,
    )

    private data class ParsedPokedexFlags(
        val caughtSpeciesIds: List<Int>,
        val seenSpeciesIds: List<Int>,
    )

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

    fun isGen5Save(saveBytes: ByteArray, gameTitle: String = ""): Boolean {
        if (saveBytes.size < 0x80000) return false
        val lower = gameTitle.lowercase()
        if (lower.contains("black") || lower.contains("white") || lower.contains("b2w2") ||
            lower.contains("black 2") || lower.contains("white 2")) {
            return true
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

    fun parse(saveBytes: ByteArray, gameTitle: String = "Pokemon Gen 5", gameId: String = ""): PokemonGameSaveDto? {
        if (saveBytes.size < 0x80000) return null

        val pokedex = readPokedexFlags(saveBytes, variantOffsets(gameTitle))
        val partyList = mutableListOf<PokemonSummaryDto>()
        val pokemonDetails = mutableMapOf<String, PokemonDetailsDto>()

        // 1. Parse Party
        val rawPartyCount = if (PARTY_OFFSET + 4 < saveBytes.size) {
            saveBytes[PARTY_OFFSET + 4].toInt() and 0xFF
        } else 0
        val partyCount = min(6, max(0, rawPartyCount))

        for (slot in 0 until partyCount) {
            val pkOffset = PARTY_OFFSET + 8 + (slot * PARTY_SLOT_SIZE)
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

        // 2. Parse Boxes (24 boxes * 30 slots)
        val boxesList = mutableListOf<PokemonBoxDto>()
        var totalCount = partyList.size
        var shinyCount = partyList.count { it.isShiny }

        for (b in 0 until BOX_COUNT) {
            val boxEntries = mutableListOf<PokemonSummaryDto>()
            val boxBase = BOX_OFFSET + (b * SLOTS_PER_BOX * 136) + (b * 0x10)

            for (s in 0 until SLOTS_PER_BOX) {
                val pkOffset = boxBase + (s * 136)
                if (pkOffset + 136 <= saveBytes.size) {
                    val pkm = parsePokemon(
                        data = saveBytes.sliceArray(pkOffset until pkOffset + 136),
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
                    capacity = 30,
                    occupiedCount = boxEntries.size,
                )
            )
        }

        return PokemonGameSaveDto(
            gameId = gameId,
            title = gameTitle,
            platform = "nds",
            generation = 5,
            saveFound = true,
            providerAvailable = true,
            party = partyList,
            boxes = boxesList,
            totalPokemonCount = totalCount,
            shinyCount = shinyCount,
            pokemonDetails = pokemonDetails,
            pokedexCaught = pokedex?.caughtSpeciesIds?.size,
            pokedexSeen = pokedex?.seenSpeciesIds?.size,
            caughtSpeciesIds = pokedex?.caughtSpeciesIds.orEmpty(),
            seenSpeciesIds = pokedex?.seenSpeciesIds.orEmpty(),
        )
    }

    private fun variantOffsets(gameTitle: String): Gen5VariantOffsets {
        val lower = gameTitle.lowercase()
        val isBlack2White2 = lower.contains("black 2") ||
            lower.contains("white 2") ||
            lower.contains("black2") ||
            lower.contains("white2") ||
            lower.contains("b2w2")

        return if (isBlack2White2) {
            Gen5VariantOffsets(pokedexOffset = 0x21400, pokedexSize = 0x04DC)
        } else {
            Gen5VariantOffsets(pokedexOffset = 0x21600, pokedexSize = 0x04D4)
        }
    }

    private fun readPokedexFlags(saveBytes: ByteArray, offsets: Gen5VariantOffsets): ParsedPokedexFlags? {
        val primary = readPokedexFlagsAt(saveBytes, baseOffset = 0, offsets)
        if (primary != null) return primary

        return readPokedexFlagsAt(saveBytes, baseOffset = SAVE_COPY_SIZE, offsets)
    }

    private fun readPokedexFlagsAt(
        saveBytes: ByteArray,
        baseOffset: Int,
        offsets: Gen5VariantOffsets,
    ): ParsedPokedexFlags? {
        val blockStart = baseOffset + offsets.pokedexOffset
        val blockEnd = blockStart + offsets.pokedexSize
        if (blockStart < 0 || blockEnd > saveBytes.size) return null

        val flagsStart = blockStart + POKEDEX_FLAGS_OFFSET_IN_BLOCK
        val caughtRegion = flagsStart until (flagsStart + POKEDEX_FLAG_REGION_SIZE)
        val seenRegion = (flagsStart + POKEDEX_FLAG_REGION_SIZE) until (flagsStart + POKEDEX_FLAG_REGION_SIZE * 2)
        if (seenRegion.last >= saveBytes.size) return null
        if (isErased(saveBytes, caughtRegion) && isErased(saveBytes, seenRegion)) return null

        val caughtIds = mutableListOf<Int>()
        val seenIds = mutableSetOf<Int>()

        for (speciesId in 1..GEN5_SPECIES_COUNT) {
            val bitIndex = speciesId - 1
            if (readFlag(saveBytes, flagsStart, bitIndex)) {
                caughtIds.add(speciesId)
                seenIds.add(speciesId)
            }
            if (readFlag(saveBytes, flagsStart + POKEDEX_FLAG_REGION_SIZE, bitIndex)) {
                seenIds.add(speciesId)
            }
        }

        return ParsedPokedexFlags(
            caughtSpeciesIds = caughtIds.sorted(),
            seenSpeciesIds = seenIds.sorted(),
        )
    }

    private fun readFlag(data: ByteArray, offset: Int, bitIndex: Int): Boolean {
        val byteOffset = offset + (bitIndex / 8)
        if (byteOffset !in data.indices) return false
        val mask = 1 shl (bitIndex % 8)
        return (data[byteOffset].toInt() and mask) != 0
    }

    private fun isErased(data: ByteArray, range: IntRange): Boolean =
        range.all { index -> index in data.indices && (data[index].toInt() and 0xFF) == 0xFF }

    data class ParsedGen5Pokemon(
        val summary: PokemonSummaryDto,
        val details: PokemonDetailsDto,
    )

    fun parsePokemon(
        data: ByteArray,
        isParty: Boolean,
        slot: Int,
        boxIndex: Int?,
        gameTitle: String,
    ): ParsedGen5Pokemon? {
        if (data.size < 136) return null

        val pid = readUInt32LE(data, 0)
        val chk = readUInt16LE(data, 6)
        if (pid == 0L && chk == 0) return null

        // Decrypt 136 bytes + party bytes
        val d = data.clone()
        var seed = chk.toLong() and 0xFFFFL
        for (i in 8 until 136 step 2) {
            seed = (0x41C64E6DL * seed + 0x6073L) and 0xFFFFFFFFL
            val xor = ((seed ushr 16) and 0xFFFFL).toInt()
            val w = readUInt16LE(d, i) xor xor
            d[i] = (w and 0xFF).toByte()
            d[i + 1] = ((w ushr 8) and 0xFF).toByte()
        }

        if (d.size >= 220) {
            var partySeed = pid and 0xFFFFFFFFL
            for (i in 136 until 220 step 2) {
                partySeed = (0x41C64E6DL * partySeed + 0x6073L) and 0xFFFFFFFFL
                val xor = ((partySeed ushr 16) and 0xFFFFL).toInt()
                val w = readUInt16LE(d, i) xor xor
                d[i] = (w and 0xFF).toByte()
                d[i + 1] = ((w ushr 8) and 0xFF).toByte()
            }
        }

        // Unshuffle blocks A, B, C, D (32 bytes each)
        val shiftVal = (((pid ushr 13) and 31L) % 24).toInt()
        val order = BLOCK_ORDERS[shiftVal]

        val unshuffled = ByteArray(d.size)
        System.arraycopy(d, 0, unshuffled, 0, 8)
        for (pos in 0 until 4) {
            val blk = order[pos]
            System.arraycopy(d, 8 + pos * 32, unshuffled, 8 + blk * 32, 32)
        }
        if (d.size > 136) {
            System.arraycopy(d, 136, unshuffled, 136, d.size - 136)
        }

        // Block A: 8..39
        val speciesId = readUInt16LE(unshuffled, 8)
        if (speciesId <= 0 || speciesId > 649) return null

        val heldItem = readUInt16LE(unshuffled, 10)
        val otId = readUInt16LE(unshuffled, 12)
        val secretId = readUInt16LE(unshuffled, 14)
        val exp = readUInt32LE(unshuffled, 16)
        val friendship = unshuffled[20].toInt() and 0xFF
        val abilityId = unshuffled[21].toInt() and 0xFF

        val hpEv = unshuffled[24].toInt() and 0xFF
        val atkEv = unshuffled[25].toInt() and 0xFF
        val defEv = unshuffled[26].toInt() and 0xFF
        val speEv = unshuffled[27].toInt() and 0xFF
        val spaEv = unshuffled[28].toInt() and 0xFF
        val spdEv = unshuffled[29].toInt() and 0xFF

        val evStats = PokemonStatsDto(
            hp = hpEv.coerceIn(0, 252),
            attack = atkEv.coerceIn(0, 252),
            defense = defEv.coerceIn(0, 252),
            speed = speEv.coerceIn(0, 252),
            specialAttack = spaEv.coerceIn(0, 252),
            specialDefense = spdEv.coerceIn(0, 252),
        )

        // Block B: 40..71
        val m1 = readUInt16LE(unshuffled, 40)
        val m2 = readUInt16LE(unshuffled, 42)
        val m3 = readUInt16LE(unshuffled, 44)
        val m4 = readUInt16LE(unshuffled, 46)
        val movesList = listOf(m1, m2, m3, m4).filter { it > 0 }.map { PokemonMoveCatalog.resolveMoveName(it) }

        val iv32 = readUInt32LE(unshuffled, 56)
        val hpIv = (iv32 and 0x1FL).toInt()
        val atkIv = ((iv32 ushr 5) and 0x1FL).toInt()
        val defIv = ((iv32 ushr 10) and 0x1FL).toInt()
        val speIv = ((iv32 ushr 15) and 0x1FL).toInt()
        val spaIv = ((iv32 ushr 20) and 0x1FL).toInt()
        val spdIv = ((iv32 ushr 25) and 0x1FL).toInt()
        val isEgg = ((iv32 ushr 30) and 1L) == 1L

        val ivStats = PokemonStatsDto(
            hp = hpIv,
            attack = atkIv,
            defense = defIv,
            speed = speIv,
            specialAttack = spaIv,
            specialDefense = spdIv,
        )

        val genderFlag = (unshuffled[64].toInt() and 0x06) ushr 1
        val genderStr = when (genderFlag) {
            0 -> "M"
            1 -> "F"
            else -> "Genderless"
        }

        // Block C: 72..103 (Nickname, 11 UTF-16LE characters = 22 bytes)
        val rawNick = decodeUtf16String(unshuffled, 72, 11)
        val speciesName = PokemonSpeciesCatalog.resolveSpeciesName(speciesId)
        val nickname = if (isEgg) "Egg" else rawNick.ifBlank { speciesName }

        // Block D: 104..135 (OT Name, 8 UTF-16LE characters = 16 bytes)
        val otName = decodeUtf16String(unshuffled, 104, 8).ifBlank { "TRAINER" }

        // Level & Stats
        val level: Int
        var currentHp = 0
        var maxHp = 0
        if (isParty && unshuffled.size >= 220) {
            level = (unshuffled[140].toInt() and 0xFF).coerceIn(1, 100)
            currentHp = readUInt16LE(unshuffled, 142)
            maxHp = readUInt16LE(unshuffled, 144)
        } else {
            level = calculateLevelFromExp(exp)
        }

        // Shiny calculation
        val shinyVal = (otId xor secretId) xor ((pid and 0xFFFFL) xor (pid ushr 16)).toInt()
        val isShiny = shinyVal < 8

        val natureName = NATURE_NAMES[(pid % 25L).toInt()]
        val pkmId = "gen5_${speciesId}_${otId}_${slot}_${if (isParty) "p" else "b$boxIndex"}"

        val summary = PokemonSummaryDto(
            id = pkmId,
            species = speciesName,
            speciesId = speciesId,
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

        return ParsedGen5Pokemon(summary, details)
    }

    private fun decodeUtf16String(data: ByteArray, offset: Int, maxChars: Int): String {
        val sb = StringBuilder()
        for (i in 0 until maxChars) {
            val charOffset = offset + (i * 2)
            if (charOffset + 2 > data.size) break
            val code = readUInt16LE(data, charOffset)
            if (code == 0xFFFF || code == 0x0000) break
            sb.append(code.toChar())
        }
        return sb.toString().trim()
    }

    private fun calculateLevelFromExp(exp: Long): Int {
        if (exp <= 0L) return 5
        val approx = kotlin.math.cbrt(exp.toDouble()).toInt()
        return max(1, min(100, approx))
    }
}

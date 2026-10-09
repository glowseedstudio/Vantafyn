package dev.vantafyn.core.jellyfin

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pure Kotlin native Gen 4 save parser (.sav / .sram / .dsv) for Nintendo DS Pokémon titles:
 * Pokémon Diamond, Pearl, Platinum, HeartGold, and SoulSilver.
 *
 * Fully autonomous: does not require external PKVault container services or HTTP servers.
 */
object Gen4NativeSaveParser {

    private const val PARTITION_SIZE = 0x40000 // 256 KB per save slot
    private const val POKEDEX_REGION_SIZE = 0x40
    private const val POKEDEX_CAUGHT_REGION = 0
    private const val POKEDEX_SEEN_REGION = 1
    private const val GEN4_SPECIES_COUNT = 493

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

    enum class Gen4Variant {
        DP, Pt, HGSS
    }

    data class VariantOffsets(
        val variant: Gen4Variant,
        val generalSize: Int,
        val storageSize: Int,
        val storageStart: Int,
        val partyOffset: Int,
        val partyCountOffset: Int,
        val boxDataStart: Int,
        val pokedexOffset: Int,
        val trainerNameOffset: Int,
        val tidOffset: Int,
        val sidOffset: Int,
    )

    private data class ParsedPokedexFlags(
        val caughtSpeciesIds: List<Int>,
        val seenSpeciesIds: List<Int>,
    )

    private val DP_OFFSETS = VariantOffsets(
        variant = Gen4Variant.DP,
        generalSize = 0xC100,
        storageSize = 0x121E0,
        storageStart = 0xC100,
        partyOffset = 0x98,
        partyCountOffset = 0x94,
        boxDataStart = 4,
        pokedexOffset = 0x12DC,
        trainerNameOffset = 0x64,
        tidOffset = 0x74,
        sidOffset = 0x76,
    )

    private val PT_OFFSETS = VariantOffsets(
        variant = Gen4Variant.Pt,
        generalSize = 0xCF2C,
        storageSize = 0x121E4,
        storageStart = 0xCF2C,
        partyOffset = 0xA0,
        partyCountOffset = 0x9C,
        boxDataStart = 4,
        pokedexOffset = 0x1328,
        trainerNameOffset = 0x68,
        tidOffset = 0x78,
        sidOffset = 0x7A,
    )

    private val HGSS_OFFSETS = VariantOffsets(
        variant = Gen4Variant.HGSS,
        generalSize = 0xF628,
        storageSize = 0x12310,
        storageStart = 0xF700,
        partyOffset = 0x98,
        partyCountOffset = 0x94,
        boxDataStart = 0,
        pokedexOffset = 0x12B8,
        trainerNameOffset = 0x64,
        tidOffset = 0x74,
        sidOffset = 0x76,
    )

    fun isGen4Save(saveBytes: ByteArray, gameTitle: String = ""): Boolean {
        if (saveBytes.size < 0x40000) return false // At least 256 KB
        val lowerTitle = gameTitle.lowercase()
        if (lowerTitle.contains("diamond") || lowerTitle.contains("pearl") ||
            lowerTitle.contains("platinum") || lowerTitle.contains("heartgold") ||
            lowerTitle.contains("soulsilver") || lowerTitle.contains("heart gold") ||
            lowerTitle.contains("soul silver") || lowerTitle.contains("hgss")) {
            return true
        }
        return false
    }

    private fun detectVariant(saveBytes: ByteArray, gameTitle: String): VariantOffsets {
        val lower = gameTitle.lowercase()
        return when {
            lower.contains("platinum") -> PT_OFFSETS
            lower.contains("heartgold") || lower.contains("soulsilver") ||
                lower.contains("heart gold") || lower.contains("soul silver") || lower.contains("hgss") -> HGSS_OFFSETS
            else -> DP_OFFSETS
        }
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

    private fun getActiveSlot(saveBytes: ByteArray, blockStart: Int, blockSize: Int): Int {
        val footerOffset = blockStart + blockSize - 0x14
        if (footerOffset + 8 > saveBytes.size) return -1
        val c1 = readUInt32LE(saveBytes, footerOffset)
        val size1 = readUInt32LE(saveBytes, footerOffset + 4)
        val v1 = c1 != 0xFFFFFFFFL && size1 == blockSize.toLong()

        if (footerOffset + PARTITION_SIZE + 8 > saveBytes.size) {
            return if (v1) 0 else -1
        }
        val c2 = readUInt32LE(saveBytes, footerOffset + PARTITION_SIZE)
        val size2 = readUInt32LE(saveBytes, footerOffset + PARTITION_SIZE + 4)
        val v2 = c2 != 0xFFFFFFFFL && size2 == blockSize.toLong()

        return when {
            v1 && v2 -> if (c2 > c1) 1 else 0
            v2 -> 1
            v1 -> 0
            else -> -1
        }
    }

    fun parse(saveBytes: ByteArray, gameTitle: String = "Pokemon Gen 4", gameId: String = ""): PokemonGameSaveDto? {
        if (saveBytes.size !in 524288..524410) return null

        val offsets = detectVariant(saveBytes, gameTitle)

        val activeSlotGeneral = getActiveSlot(saveBytes, 0, offsets.generalSize)
        val activeSlotStorage = getActiveSlot(saveBytes, offsets.storageStart, offsets.storageSize)
        if (activeSlotGeneral < 0 || activeSlotStorage < 0) return null

        val generalBase = if (activeSlotGeneral == 1) PARTITION_SIZE else 0
        val storageBase = (if (activeSlotStorage == 1) PARTITION_SIZE else 0) + offsets.storageStart
        val pokedex = readPokedexFlags(saveBytes, generalBase, offsets)

        val otName = decodeUtf16String(saveBytes, generalBase + offsets.trainerNameOffset, 7).ifBlank { "TRAINER" }
        val tid = readUInt16LE(saveBytes, generalBase + offsets.tidOffset)
        val (johtoBadgeFlags, kantoBadgeFlags) = readBadgeFlags(saveBytes, generalBase, offsets)

        val partyList = mutableListOf<PokemonSummaryDto>()
        val pokemonDetails = mutableMapOf<String, PokemonDetailsDto>()

        // 1. Parse Party
        val rawPartyCount = saveBytes[generalBase + offsets.partyCountOffset].toInt() and 0xFF
        val partyCount = min(6, max(0, rawPartyCount))

        for (slot in 0 until partyCount) {
            val pkOffset = generalBase + offsets.partyOffset + (slot * 236)
            if (pkOffset + 236 <= saveBytes.size) {
                val pkm = parsePokemon(
                    data = saveBytes.sliceArray(pkOffset until pkOffset + 236),
                    isParty = true,
                    slot = slot + 1,
                    boxIndex = null,
                    defaultOt = otName,
                    defaultTid = tid,
                    gameTitle = gameTitle,
                )
                if (pkm != null) {
                    partyList.add(pkm.summary)
                    pokemonDetails[pkm.summary.id] = pkm.details
                }
            }
        }

        // 2. Parse Boxes (18 boxes * 30 slots)
        val boxesList = mutableListOf<PokemonBoxDto>()
        var totalCount = partyList.size
        var shinyCount = partyList.count { it.isShiny }

        for (b in 0 until 18) {
            val boxEntries = mutableListOf<PokemonSummaryDto>()
            val boxStart = storageBase + offsets.boxDataStart + (b * 30 * 136)

            for (s in 0 until 30) {
                val pkOffset = boxStart + (s * 136)
                if (pkOffset + 136 <= saveBytes.size) {
                    val pkm = parsePokemon(
                        data = saveBytes.sliceArray(pkOffset until pkOffset + 136),
                        isParty = false,
                        slot = s + 1,
                        boxIndex = b + 1,
                        defaultOt = otName,
                        defaultTid = tid,
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

        val isUninitialized = partyList.isEmpty() &&
            boxesList.all { it.occupiedCount == 0 } &&
            (pokedex?.caughtSpeciesIds?.isEmpty() ?: true) &&
            (johtoBadgeFlags == 0 && kantoBadgeFlags == 0) &&
            (otName == "TRAINER" || otName.isBlank()) &&
            tid == 0
        if (isUninitialized || activeSlotGeneral < 0) return null

        return PokemonGameSaveDto(
            gameId = gameId,
            title = gameTitle,
            platform = "nds",
            generation = 4,
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
            gymBadges = PokemonGymBadgeCatalog.forGen4(
                pokemonGameKey(gameId, gameTitle),
                johtoBadgeFlags,
                kantoBadgeFlags,
            ),
        )
    }

    /**
     * Badge bitfield offsets are relative to the trainer name block, matching PKHeX:
     * `Badges = General[Trainer1 + 0x1A]` and, for HGSS only, `Badges16 = General[Trainer1 + 0x1F]`.
     * Bits are ordered gym-by-gym, so bit N is the Nth badge of that region.
     */
    private fun readBadgeFlags(
        saveBytes: ByteArray,
        generalBase: Int,
        offsets: VariantOffsets,
    ): Pair<Int, Int> {
        val trainerStart = generalBase + offsets.trainerNameOffset
        val nameEnd = trainerStart + 16
        if (trainerStart < 0 || nameEnd > saveBytes.size) return 0 to 0
        val erased = (trainerStart until nameEnd).all { (saveBytes[it].toInt() and 0xFF) == 0xFF }
        if (erased) return 0 to 0

        val johto = readBadgeByte(saveBytes, trainerStart + 0x1A)
        val kanto = if (offsets.variant == Gen4Variant.HGSS) {
            readBadgeByte(saveBytes, trainerStart + 0x1F)
        } else {
            0
        }
        return johto to kanto
    }

    private fun readBadgeByte(data: ByteArray, offset: Int): Int =
        if (offset in data.indices) data[offset].toInt() and 0xFF else 0

    private fun readPokedexFlags(
        saveBytes: ByteArray,
        generalBase: Int,
        offsets: VariantOffsets,
    ): ParsedPokedexFlags? {
        val dexStart = generalBase + offsets.pokedexOffset
        val dexEnd = dexStart + 4 + (POKEDEX_REGION_SIZE * 4)
        if (dexStart < 0 || dexEnd > saveBytes.size) return null

        val caughtRegion = dexStart + 4 + (POKEDEX_CAUGHT_REGION * POKEDEX_REGION_SIZE)
        val seenRegion = dexStart + 4 + (POKEDEX_SEEN_REGION * POKEDEX_REGION_SIZE)
        if (isErased(saveBytes, caughtRegion until (caughtRegion + POKEDEX_REGION_SIZE)) &&
            isErased(saveBytes, seenRegion until (seenRegion + POKEDEX_REGION_SIZE))) {
            return null
        }

        val caughtIds = mutableListOf<Int>()
        val seenIds = mutableSetOf<Int>()

        for (speciesId in 1..GEN4_SPECIES_COUNT) {
            val bitIndex = speciesId - 1
            if (readFlag(saveBytes, caughtRegion, bitIndex)) {
                caughtIds.add(speciesId)
                seenIds.add(speciesId)
            }
            if (readFlag(saveBytes, seenRegion, bitIndex)) {
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

    data class ParsedGen4Pokemon(
        val summary: PokemonSummaryDto,
        val details: PokemonDetailsDto,
    )

    fun parsePokemon(
        data: ByteArray,
        isParty: Boolean,
        slot: Int,
        boxIndex: Int?,
        defaultOt: String,
        defaultTid: Int,
        gameTitle: String,
    ): ParsedGen4Pokemon? {
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

        if (d.size >= 236) {
            var partySeed = pid and 0xFFFFFFFFL
            for (i in 136 until 236 step 2) {
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
        if (speciesId <= 0 || speciesId > 493) return null

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
        val otName = decodeUtf16String(unshuffled, 104, 8).ifBlank { defaultOt }

        // Level & Stats
        val level: Int
        var currentHp = 0
        var maxHp = 0
        if (isParty && unshuffled.size >= 236) {
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
        val pkmId = "gen4_${speciesId}_${otId}_${slot}_${if (isParty) "p" else "b$boxIndex"}"

        val formBits = (unshuffled[64].toInt() and 0xF8) ushr 3
        val unownLetter = if (speciesId == 201) {
            when (formBits) {
                in 0..25 -> ('A'.code + formBits).toChar().toString()
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

        return ParsedGen4Pokemon(summary, details)
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
        val approx = cbrt(exp.toDouble()).toInt()
        return max(1, min(100, approx))
    }
}

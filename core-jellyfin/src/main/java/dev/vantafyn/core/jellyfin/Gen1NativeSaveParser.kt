package dev.vantafyn.core.jellyfin

/**
 * Native parser for Generation 1 Pokémon save files (Red, Blue, Yellow).
 * Decodes 32KB battery-backed SRAM directly on Android with zero external dependencies.
 */
object Gen1NativeSaveParser {

    // Gen 1 Internal Species Index (1..190) -> National Pokédex ID (1..151)
    private val GEN1_INTERNAL_TO_DEX = intArrayOf(
        0, 112, 115, 32, 35, 21, 100, 34, 80, 2,
        103, 108, 102, 88, 94, 29, 31, 104, 111, 131,
        59, 151, 130, 90, 72, 92, 123, 120, 9, 127,
        114, 0, 0, 58, 95, 22, 16, 79, 64, 75,
        113, 67, 122, 106, 107, 24, 47, 54, 96, 76,
        0, 126, 0, 125, 82, 109, 0, 56, 86, 50,
        128, 0, 0, 0, 83, 48, 149, 0, 0, 0,
        84, 60, 124, 146, 144, 145, 132, 52, 98, 0,
        0, 0, 37, 38, 25, 26, 0, 0, 147, 148,
        140, 141, 116, 117, 0, 0, 27, 28, 138, 139,
        39, 40, 133, 136, 135, 134, 66, 41, 23, 46,
        61, 62, 13, 14, 15, 0, 85, 57, 51, 49,
        87, 0, 0, 10, 11, 12, 68, 0, 55, 97,
        42, 150, 143, 129, 0, 0, 89, 0, 99, 91,
        0, 101, 36, 110, 53, 105, 0, 93, 63, 65,
        17, 18, 121, 1, 3, 73, 0, 118, 119, 0,
        0, 0, 0, 77, 78, 19, 20, 33, 30, 74,
        137, 142, 0, 81, 0, 0, 4, 7, 5, 8,
        6, 0, 0, 0, 0, 43, 44, 45, 69, 70,
        71
    )

    private val NATURE_NAMES = arrayOf(
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky",
    )

    data class ParsedGen1Pokemon(
        val summary: PokemonSummaryDto,
        val details: PokemonDetailsDto,
    )

    fun isGen1Save(saveBytes: ByteArray?): Boolean {
        if (saveBytes == null || saveBytes.size < 8192) return false
        if (0x3523 >= saveBytes.size) return false

        var sum = 0
        for (i in 0x2598 until 0x3523) {
            sum += saveBytes[i].toInt() and 0xFF
        }
        val storedCheck = saveBytes[0x3523].toInt() and 0xFF
        return ((sum + storedCheck) and 0xFF) == 0xFF
    }

    fun parse(saveBytes: ByteArray, gameTitle: String, gameId: String): PokemonGameSaveDto? {
        if (!isGen1Save(saveBytes)) return null

        val trainerName = decodeGen1String(saveBytes, 0x2598, 11).ifBlank { "RED" }
        val tid = ((saveBytes[0x2605].toInt() and 0xFF) shl 8) or (saveBytes[0x2606].toInt() and 0xFF)

        // Money is BCD at 0x2999..0x299B
        val m0 = saveBytes[0x2999].toInt() and 0xFF
        val m1 = saveBytes[0x299A].toInt() and 0xFF
        val m2 = saveBytes[0x299B].toInt() and 0xFF
        val money = ((m0 ushr 4) * 10 + (m0 and 0x0F)) * 10000 +
            ((m1 ushr 4) * 10 + (m1 and 0x0F)) * 100 +
            ((m2 ushr 4) * 10 + (m2 and 0x0F))

        // Pokedex bitfields (0x25A3..0x25B5 for caught, 0x25B6..0x25C8 for seen)
        val caughtIds = mutableSetOf<Int>()
        val seenIds = mutableSetOf<Int>()
        val cBytes = (0 until 19).map { offset ->
            val caughtByteIdx = 0x25A3 + offset
            if (caughtByteIdx < saveBytes.size) saveBytes[caughtByteIdx].toInt() and 0xFF else 0
        }
        val isErasedOrCorrupt = cBytes.all { it == 0xFF } || (cBytes.count { it == 0xFF } >= 12)
        if (!isErasedOrCorrupt) {
            for (offset in 0 until 19) {
                val caughtByteIdx = 0x25A3 + offset
                val seenByteIdx = 0x25B6 + offset
                val cByte = if (caughtByteIdx < saveBytes.size) saveBytes[caughtByteIdx].toInt() and 0xFF else 0
                val sByte = if (seenByteIdx < saveBytes.size) saveBytes[seenByteIdx].toInt() and 0xFF else 0
                for (bit in 0 until 8) {
                    val speciesNum = offset * 8 + bit + 1
                    if (speciesNum in 1..151) {
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

        // 1. Parse Party
        val pokemonDetails = mutableMapOf<String, PokemonDetailsDto>()
        val partyList = mutableListOf<PokemonSummaryDto>()
        val rawPartyCount = saveBytes[0x2F2C].toInt() and 0xFF
        val partyCount = rawPartyCount.coerceIn(0, 6)

        for (slot in 0 until partyCount) {
            val pOffset = 0x2F34 + slot * 44
            val otOffset = 0x303C + slot * 11
            val nickOffset = 0x307E + slot * 11

            val pkm = parsePokemon(
                buffer = saveBytes,
                offset = pOffset,
                otOffset = otOffset,
                nickOffset = nickOffset,
                isParty = true,
                partySlot = slot + 1,
                boxIndex = null,
                gameTitle = gameTitle,
                gameId = gameId,
            )
            if (pkm != null) {
                partyList.add(pkm.summary)
                pokemonDetails[pkm.summary.id] = pkm.details
            }
        }

        // 2. Parse Boxes (1..12)
        val currentBoxIdx = saveBytes[0x284C].toInt() and 0x7F // 0-indexed (0..11)
        val boxesList = mutableListOf<PokemonBoxDto>()

        for (b in 0 until 12) {
            val boxIndex = b + 1
            val entries = mutableListOf<PokemonSummaryDto>()

            if (b == currentBoxIdx) {
                // Active Box at 0x30C0
                val bCount = (saveBytes[0x30C0].toInt() and 0xFF).coerceIn(0, 20)
                for (slot in 0 until bCount) {
                    val pOffset = 0x30D6 + slot * 33
                    val otOffset = 0x336A + slot * 11
                    val nickOffset = 0x3446 + slot * 11

                    val pkm = parsePokemon(
                        buffer = saveBytes,
                        offset = pOffset,
                        otOffset = otOffset,
                        nickOffset = nickOffset,
                        isParty = false,
                        partySlot = slot + 1,
                        boxIndex = boxIndex,
                        gameTitle = gameTitle,
                        gameId = gameId,
                    )
                    if (pkm != null) {
                        entries.add(pkm.summary)
                        pokemonDetails[pkm.summary.id] = pkm.details
                    }
                }
            } else {
                // Bank 2 (Boxes 1..6) or Bank 3 (Boxes 7..12)
                val bankBase = if (b < 6) 0x4000 else 0x6000
                val boxOffset = bankBase + (b % 6) * 1122
                if (boxOffset + 1122 <= saveBytes.size) {
                    val rawCount = saveBytes[boxOffset].toInt() and 0xFF
                    if (rawCount in 0..20) {
                        for (slot in 0 until rawCount) {
                            val pOffset = boxOffset + 22 + slot * 33
                            val otOffset = boxOffset + 22 + (20 * 33) + slot * 11
                            val nickOffset = boxOffset + 22 + (20 * 33) + (20 * 11) + slot * 11

                            val pkm = parsePokemon(
                                buffer = saveBytes,
                                offset = pOffset,
                                otOffset = otOffset,
                                nickOffset = nickOffset,
                                isParty = false,
                                partySlot = slot + 1,
                                boxIndex = boxIndex,
                                gameTitle = gameTitle,
                                gameId = gameId,
                            )
                            if (pkm != null) {
                                entries.add(pkm.summary)
                                pokemonDetails[pkm.summary.id] = pkm.details
                            }
                        }
                    }
                }
            }

            boxesList.add(
                PokemonBoxDto(
                    boxIndex = boxIndex,
                    name = "Box $boxIndex",
                    capacity = 20,
                    occupiedCount = entries.size,
                    entries = entries,
                )
            )
        }

        for (p in partyList) {
            if (p.speciesId in 1..151) {
                caughtIds.add(p.speciesId)
                seenIds.add(p.speciesId)
            }
        }
        for (b in boxesList) {
            for (p in b.entries) {
                if (p.speciesId in 1..151) {
                    caughtIds.add(p.speciesId)
                    seenIds.add(p.speciesId)
                }
            }
        }

        val totalPkm = partyList.size + boxesList.sumOf { it.occupiedCount }
        val shinyPkm = partyList.count { it.isShiny } + boxesList.sumOf { b -> b.entries.count { it.isShiny } }

        return PokemonGameSaveDto(
            gameId = gameId,
            title = gameTitle,
            platform = "gb",
            generation = 1,
            trainerName = trainerName,
            trainerId = tid.toString(),
            money = money,
            pokedexSeen = seenIds.size,
            pokedexCaught = caughtIds.size,
            saveFound = true,
            providerAvailable = true,
            party = partyList,
            boxes = boxesList,
            totalPokemonCount = totalPkm,
            shinyCount = shinyPkm,
            pokemonDetails = pokemonDetails,
            caughtSpeciesIds = caughtIds.sorted(),
            seenSpeciesIds = seenIds.sorted(),
            gymBadges = PokemonGymBadgeCatalog.forGen1(saveBytes[0x2602].toInt() and 0xFF),
        )
    }

    fun parsePokemon(
        buffer: ByteArray,
        offset: Int,
        otOffset: Int,
        nickOffset: Int,
        isParty: Boolean,
        partySlot: Int,
        boxIndex: Int?,
        gameTitle: String,
        gameId: String,
    ): ParsedGen1Pokemon? {
        val requiredSize = if (isParty) 44 else 33
        if (offset + requiredSize > buffer.size) return null

        val internalId = buffer[offset].toInt() and 0xFF
        if (internalId <= 0 || internalId >= GEN1_INTERNAL_TO_DEX.size) return null

        val speciesId = GEN1_INTERNAL_TO_DEX[internalId]
        if (speciesId <= 0 || speciesId > 151) return null

        val currentHp = ((buffer[offset + 1].toInt() and 0xFF) shl 8) or (buffer[offset + 2].toInt() and 0xFF)
        val boxLevel = buffer[offset + 3].toInt() and 0xFF

        val m1 = buffer[offset + 8].toInt() and 0xFF
        val m2 = buffer[offset + 9].toInt() and 0xFF
        val m3 = buffer[offset + 10].toInt() and 0xFF
        val m4 = buffer[offset + 11].toInt() and 0xFF
        val movesList = listOf(m1, m2, m3, m4).filter { it > 0 }.map { Gen3NativeSaveParser.resolveMoveName(it) }

        val otid = ((buffer[offset + 0x0C].toInt() and 0xFF) shl 8) or (buffer[offset + 0x0D].toInt() and 0xFF)
        val exp = ((buffer[offset + 0x0E].toInt() and 0xFF) shl 16) or
            ((buffer[offset + 0x0F].toInt() and 0xFF) shl 8) or
            (buffer[offset + 0x10].toInt() and 0xFF)

        // Stat Experience / EVs (0..65535, scale to 0..252)
        val hpEv = (((buffer[offset + 0x11].toInt() and 0xFF) shl 8) or (buffer[offset + 0x12].toInt() and 0xFF)) / 256
        val atkEv = (((buffer[offset + 0x13].toInt() and 0xFF) shl 8) or (buffer[offset + 0x14].toInt() and 0xFF)) / 256
        val defEv = (((buffer[offset + 0x15].toInt() and 0xFF) shl 8) or (buffer[offset + 0x16].toInt() and 0xFF)) / 256
        val speEv = (((buffer[offset + 0x17].toInt() and 0xFF) shl 8) or (buffer[offset + 0x18].toInt() and 0xFF)) / 256
        val spcEv = (((buffer[offset + 0x19].toInt() and 0xFF) shl 8) or (buffer[offset + 0x1A].toInt() and 0xFF)) / 256

        val evStats = PokemonStatsDto(
            hp = hpEv.coerceIn(0, 252),
            attack = atkEv.coerceIn(0, 252),
            defense = defEv.coerceIn(0, 252),
            speed = speEv.coerceIn(0, 252),
            specialAttack = spcEv.coerceIn(0, 252),
            specialDefense = spcEv.coerceIn(0, 252),
        )

        // DVs / IVs (0..15 -> scale to 31)
        val ivRaw = ((buffer[offset + 0x1B].toInt() and 0xFF) shl 8) or (buffer[offset + 0x1C].toInt() and 0xFF)
        val atkDv = (ivRaw ushr 12) and 0x0F
        val defDv = (ivRaw ushr 8) and 0x0F
        val speDv = (ivRaw ushr 4) and 0x0F
        val spcDv = ivRaw and 0x0F
        val hpDv = ((atkDv and 1) shl 3) or ((defDv and 1) shl 2) or ((speDv and 1) shl 1) or (spcDv and 1)

        val ivStats = PokemonStatsDto(
            hp = (hpDv * 31) / 15,
            attack = (atkDv * 31) / 15,
            defense = (defDv * 31) / 15,
            speed = (speDv * 31) / 15,
            specialAttack = (spcDv * 31) / 15,
            specialDefense = (spcDv * 31) / 15,
        )

        // Time Capsule shiny rule
        val isShiny = speDv == 10 && defDv == 10 && spcDv == 10 && (atkDv in intArrayOf(2, 3, 6, 7, 10, 11, 14, 15))

        val level = if (isParty) {
            val partyLvl = buffer[offset + 0x21].toInt() and 0xFF
            if (partyLvl in 1..100) partyLvl else 5
        } else {
            if (boxLevel in 1..100) boxLevel else 5
        }

        val maxHp = if (isParty) {
            ((buffer[offset + 0x22].toInt() and 0xFF) shl 8) or (buffer[offset + 0x23].toInt() and 0xFF)
        } else null

        val otName = decodeGen1String(buffer, otOffset, 11).ifBlank { "RED" }
        val rawNick = decodeGen1String(buffer, nickOffset, 11)
        val speciesName = PokemonSpeciesCatalog.resolveSpeciesName(speciesId)
        val nickname = rawNick.ifBlank { speciesName }

        // VC transfer nature rule: exp % 25
        val natureName = NATURE_NAMES[(exp % 25).coerceAtLeast(0).toInt()]
        val pkmId = "gen1_${speciesId}_${otid}_${partySlot}_${if (isParty) "p" else "b$boxIndex"}"

        val summary = PokemonSummaryDto(
            id = pkmId,
            species = speciesName,
            speciesId = speciesId,
            nickname = nickname,
            level = level,
            gender = "Genderless",
            isShiny = isShiny,
            originalTrainer = otName,
            originalTrainerId = otid.toString(),
            originGame = gameTitle,
            currentLocation = if (isParty) "Party" else "Box $boxIndex",
            boxIndex = boxIndex,
            slotIndex = partySlot,
            isInParty = isParty,
            legalityStatus = "valid",
        )

        val details = PokemonDetailsDto(
            summary = summary,
            nature = natureName,
            ability = Gen3NativeSaveParser.resolveGen3Ability(speciesId, 0),
            heldItem = null,
            moves = movesList,
            iv = ivStats,
            ev = evStats,
            currentHp = currentHp,
            maxHp = maxHp,
            friendship = 70,
            pokeball = "Poké Ball",
            legalityStatus = "valid",
        )

        return ParsedGen1Pokemon(summary = summary, details = details)
    }

    private fun decodeGen1String(buffer: ByteArray, offset: Int, length: Int): String {
        val sb = StringBuilder()
        for (i in 0 until length) {
            val idx = offset + i
            if (idx >= buffer.size) break
            val b = buffer[idx].toInt() and 0xFF
            if (b == 0x50) break // 0x50 is string terminator in Gen 1
            when (b) {
                0x7F -> sb.append(' ')
                in 0x80..0x99 -> sb.append(('A' + (b - 0x80)))
                in 0xA0..0xB9 -> sb.append(('a' + (b - 0xA0)))
                in 0xF6..0xFF -> sb.append(('0' + (b - 0xF6)))
                0xE0 -> sb.append('\'')
                0xE1 -> sb.append("PK")
                0xE2 -> sb.append("MN")
                0xE3 -> sb.append('-')
                0xE8 -> sb.append('.')
                0xEF -> sb.append('♂')
                0xF5 -> sb.append('♀')
                0xE4 -> sb.append('?')
                0xE5 -> sb.append('!')
            }
        }
        return sb.toString().trim()
    }
}

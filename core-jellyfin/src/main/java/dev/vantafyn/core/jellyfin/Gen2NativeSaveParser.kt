package dev.vantafyn.core.jellyfin

/**
 * Native parser for Generation 2 Pokémon save files (Gold, Silver, Crystal).
 * Decodes 32KB battery-backed SRAM directly on Android with zero external dependencies.
 */
object Gen2NativeSaveParser {

    private val NATURE_NAMES = arrayOf(
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky",
    )

    data class ParsedGen2Pokemon(
        val summary: PokemonSummaryDto,
        val details: PokemonDetailsDto,
    )

    fun isGen2Save(saveBytes: ByteArray?): Boolean {
        if (saveBytes == null || saveBytes.size != 32768) return false
        // Make sure it is not a Gen 1 save
        if (Gen1NativeSaveParser.isGen1Save(saveBytes)) return false

        // Check Gold/Silver party offset (0x288A) or Crystal party offset (0x2865)
        val gsCount = saveBytes[0x288A].toInt() and 0xFF
        val cryCount = saveBytes[0x2865].toInt() and 0xFF

        val isGs = gsCount in 0..6 && (gsCount == 0 || (saveBytes[0x288B].toInt() and 0xFF in 1..251))
        val isCry = cryCount in 0..6 && (cryCount == 0 || (saveBytes[0x2866].toInt() and 0xFF in 1..251))

        if (!isGs && !isCry) return false

        // Check for valid trainer name at 0x2009
        val tName = decodeGen2String(saveBytes, 0x2009, 11)
        return tName.isNotBlank() && tName.all { it.isLetterOrDigit() || it == ' ' || it == '.' || it == '-' }
    }

    fun parse(saveBytes: ByteArray, gameTitle: String, gameId: String): PokemonGameSaveDto? {
        if (!isGen2Save(saveBytes)) return null

        val isCrystal = run {
            val cryCount = saveBytes[0x2865].toInt() and 0xFF
            val gsCount = saveBytes[0x288A].toInt() and 0xFF
            if (cryCount in 1..6 && saveBytes[0x2866].toInt() and 0xFF in 1..251) true
            else if (gsCount in 1..6 && saveBytes[0x288B].toInt() and 0xFF in 1..251) false
            else gameTitle.contains("crystal", ignoreCase = true)
        }

        val trainerName = decodeGen2String(saveBytes, 0x2009, 11).ifBlank { "GOLD" }
        val tid = ((saveBytes[0x2002].toInt() and 0xFF) shl 8) or (saveBytes[0x2003].toInt() and 0xFF)

        // Money BCD at 0x23DB..0x23DD
        val m0 = saveBytes[0x23DB].toInt() and 0xFF
        val m1 = saveBytes[0x23DC].toInt() and 0xFF
        val m2 = saveBytes[0x23DD].toInt() and 0xFF
        val money = ((m0 ushr 4) * 10 + (m0 and 0x0F)) * 10000 +
            ((m1 ushr 4) * 10 + (m1 and 0x0F)) * 100 +
            ((m2 ushr 4) * 10 + (m2 and 0x0F))

        val partyOffset = if (isCrystal) 0x2865 else 0x288A
        val partyList = mutableListOf<PokemonSummaryDto>()
        val pokemonDetails = mutableMapOf<String, PokemonDetailsDto>()

        val rawPartyCount = saveBytes[partyOffset].toInt() and 0xFF
        val partyCount = rawPartyCount.coerceIn(0, 6)

        val partyStructsOffset = partyOffset + 8 // 1 byte count + 7 bytes species array
        val partyOtOffset = partyStructsOffset + (6 * 48)
        val partyNickOffset = partyOtOffset + (6 * 11)

        for (slot in 0 until partyCount) {
            val pOffset = partyStructsOffset + slot * 48
            val otOffset = partyOtOffset + slot * 11
            val nickOffset = partyNickOffset + slot * 11

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

        // 2. Parse Boxes (1..14)
        val currentBoxIdx = saveBytes[0x2724].toInt() and 0x7F // 0-indexed (0..13)
        val boxesList = mutableListOf<PokemonBoxDto>()

        for (b in 0 until 14) {
            val boxIndex = b + 1
            val entries = mutableListOf<PokemonSummaryDto>()

            if (b == currentBoxIdx) {
                // Active Box at 0x2D10
                val bCount = (saveBytes[0x2D10].toInt() and 0xFF).coerceIn(0, 20)
                val boxStructs = 0x2D10 + 22 // 1 count + 21 species
                val boxOt = boxStructs + (20 * 32)
                val boxNick = boxOt + (20 * 11)

                for (slot in 0 until bCount) {
                    val pOffset = boxStructs + slot * 32
                    val otOffset = boxOt + slot * 11
                    val nickOffset = boxNick + slot * 11

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
                // Bank 2 (Boxes 1..7) or Bank 3 (Boxes 8..14)
                val bankBase = if (b < 7) 0x4000 else 0x6000
                val boxOffset = bankBase + (b % 7) * 1102

                if (boxOffset + 1102 <= saveBytes.size) {
                    val rawCount = saveBytes[boxOffset].toInt() and 0xFF
                    if (rawCount in 0..20) {
                        val boxStructs = boxOffset + 22
                        val boxOt = boxStructs + (20 * 32)
                        val boxNick = boxOt + (20 * 11)

                        for (slot in 0 until rawCount) {
                            val pOffset = boxStructs + slot * 32
                            val otOffset = boxOt + slot * 11
                            val nickOffset = boxNick + slot * 11

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

        // Pokedex bitfields (GS: 0x2A4C caught, 0x2A6C seen; Crystal: 0x2A6C caught, 0x2A8C seen)
        val caughtIds = mutableSetOf<Int>()
        val seenIds = mutableSetOf<Int>()
        val caughtStart = if (isCrystal) 0x2A6C else 0x2A4C
        val seenStart = if (isCrystal) 0x2A8C else 0x2A6C
        val cBytes = (0 until 32).map { offset ->
            val cIdx = caughtStart + offset
            if (cIdx < saveBytes.size) saveBytes[cIdx].toInt() and 0xFF else 0
        }
        val isErasedOrCorrupt = cBytes.all { it == 0xFF } || (cBytes.count { it == 0xFF } >= 16)
        if (!isErasedOrCorrupt) {
            for (offset in 0 until 32) {
                val cIdx = caughtStart + offset
                val sIdx = seenStart + offset
                val cByte = if (cIdx < saveBytes.size) saveBytes[cIdx].toInt() and 0xFF else 0
                val sByte = if (sIdx < saveBytes.size) saveBytes[sIdx].toInt() and 0xFF else 0
                for (bit in 0 until 8) {
                    val speciesNum = offset * 8 + bit + 1
                    if (speciesNum in 1..251) {
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

        for (p in partyList) {
            if (p.speciesId in 1..251) {
                caughtIds.add(p.speciesId)
                seenIds.add(p.speciesId)
            }
        }
        for (b in boxesList) {
            for (p in b.entries) {
                if (p.speciesId in 1..251) {
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
            platform = "gbc",
            generation = 2,
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
            gymBadges = PokemonGymBadgeCatalog.forGen2(
                saveBytes[0x23E5].toInt() and 0xFF,
                saveBytes[0x23E6].toInt() and 0xFF,
            ),
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
    ): ParsedGen2Pokemon? {
        val requiredSize = if (isParty) 48 else 32
        if (offset + requiredSize > buffer.size) return null

        val speciesId = buffer[offset].toInt() and 0xFF
        if (speciesId <= 0 || speciesId > 251) return null

        val heldItemId = buffer[offset + 1].toInt() and 0xFF
        val m1 = buffer[offset + 2].toInt() and 0xFF
        val m2 = buffer[offset + 3].toInt() and 0xFF
        val m3 = buffer[offset + 4].toInt() and 0xFF
        val m4 = buffer[offset + 5].toInt() and 0xFF
        val movesList = listOf(m1, m2, m3, m4).filter { it > 0 }.map { Gen3NativeSaveParser.resolveMoveName(it) }

        val otid = ((buffer[offset + 6].toInt() and 0xFF) shl 8) or (buffer[offset + 7].toInt() and 0xFF)
        val exp = ((buffer[offset + 8].toInt() and 0xFF) shl 16) or
            ((buffer[offset + 9].toInt() and 0xFF) shl 8) or
            (buffer[offset + 10].toInt() and 0xFF)

        // Stat Experience / EVs (0..65535, scale to 0..252)
        val hpEv = (((buffer[offset + 0x0B].toInt() and 0xFF) shl 8) or (buffer[offset + 0x0C].toInt() and 0xFF)) / 256
        val atkEv = (((buffer[offset + 0x0D].toInt() and 0xFF) shl 8) or (buffer[offset + 0x0E].toInt() and 0xFF)) / 256
        val defEv = (((buffer[offset + 0x0F].toInt() and 0xFF) shl 8) or (buffer[offset + 0x10].toInt() and 0xFF)) / 256
        val speEv = (((buffer[offset + 0x11].toInt() and 0xFF) shl 8) or (buffer[offset + 0x12].toInt() and 0xFF)) / 256
        val spcEv = (((buffer[offset + 0x13].toInt() and 0xFF) shl 8) or (buffer[offset + 0x14].toInt() and 0xFF)) / 256

        val evStats = PokemonStatsDto(
            hp = hpEv.coerceIn(0, 252),
            attack = atkEv.coerceIn(0, 252),
            defense = defEv.coerceIn(0, 252),
            speed = speEv.coerceIn(0, 252),
            specialAttack = spcEv.coerceIn(0, 252),
            specialDefense = spcEv.coerceIn(0, 252),
        )

        // DVs / IVs
        val ivRaw = ((buffer[offset + 0x15].toInt() and 0xFF) shl 8) or (buffer[offset + 0x16].toInt() and 0xFF)
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

        val isShiny = speDv == 10 && defDv == 10 && spcDv == 10 && (atkDv in intArrayOf(2, 3, 6, 7, 10, 11, 14, 15))
        val friendship = buffer[offset + 0x1B].toInt() and 0xFF
        val rawLevel = buffer[offset + 0x1F].toInt() and 0xFF
        val level = if (rawLevel in 1..100) rawLevel else 5

        var currentHp: Int? = null
        var maxHp: Int? = null
        if (isParty) {
            currentHp = ((buffer[offset + 0x22].toInt() and 0xFF) shl 8) or (buffer[offset + 0x23].toInt() and 0xFF)
            maxHp = ((buffer[offset + 0x24].toInt() and 0xFF) shl 8) or (buffer[offset + 0x25].toInt() and 0xFF)
        }

        val otName = decodeGen2String(buffer, otOffset, 11).ifBlank { "GOLD" }
        val rawNick = decodeGen2String(buffer, nickOffset, 11)
        val speciesName = PokemonSpeciesCatalog.resolveSpeciesName(speciesId)
        val nickname = rawNick.ifBlank { speciesName }

        val natureName = NATURE_NAMES[(exp % 25).coerceAtLeast(0).toInt()]
        val pkmId = "gen2_${speciesId}_${otid}_${partySlot}_${if (isParty) "p" else "b$boxIndex"}"

        val unownLetter = if (speciesId == 201) {
            val letterIdx = (((atkDv and 6) shl 5) or ((defDv and 6) shl 3) or ((speDv and 6) shl 1) or ((spcDv and 6) shr 1)) / 10
            ('A'.code + letterIdx.coerceIn(0, 25)).toChar().toString()
        } else null

        val summary = PokemonSummaryDto(
            id = pkmId,
            species = speciesName,
            speciesId = speciesId,
            form = unownLetter,
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
            heldItem = if (heldItemId > 0) "Item #$heldItemId" else null,
            moves = movesList,
            iv = ivStats,
            ev = evStats,
            currentHp = currentHp,
            maxHp = maxHp,
            friendship = friendship,
            pokeball = "Poké Ball",
            legalityStatus = "valid",
        )

        return ParsedGen2Pokemon(summary = summary, details = details)
    }

    private fun decodeGen2String(buffer: ByteArray, offset: Int, length: Int): String {
        val sb = StringBuilder()
        for (i in 0 until length) {
            val idx = offset + i
            if (idx >= buffer.size) break
            val b = buffer[idx].toInt() and 0xFF
            if (b == 0x50) break
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

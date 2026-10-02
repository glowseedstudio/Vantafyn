using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon.Native;

/// <summary>
/// Native high-performance parser for Generation 1 Pokémon save files (Red, Blue, Yellow).
/// Completely self-contained; requires zero external services or Docker containers.
/// </summary>
public static class Gen1SaveParser
{
    private static readonly int[] Gen1InternalToDex =
    [
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
    ];

    private static readonly string[] NatureNames =
    [
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky"
    ];

    public static bool IsGen1Save(byte[] saveBytes)
    {
        if (saveBytes == null || (saveBytes.Length != 32768 && saveBytes.Length != 8192))
        {
            return false;
        }

        if (0x3523 >= saveBytes.Length)
        {
            return false;
        }

        int sum = 0;
        for (int i = 0x2598; i < 0x3523; i++)
        {
            sum += saveBytes[i];
        }

        byte storedCheck = saveBytes[0x3523];
        return ((sum + storedCheck) & 0xFF) == 0xFF;
    }

    public static PokemonSaveParseResult Parse(byte[] saveBytes, string gameId, PkVaultStaticCatalog catalog)
    {
        var result = new PokemonSaveParseResult();

        if (saveBytes == null || saveBytes.Length < 8192)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save data is missing or smaller than minimum Gen 1 save size.";
            return result;
        }

        if (!IsGen1Save(saveBytes))
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Data does not match Gen 1 save structure or checksum.";
            return result;
        }

        result.TrainerName = DecodeGen1String(saveBytes, 0x2598, 11);
        if (string.IsNullOrWhiteSpace(result.TrainerName)) result.TrainerName = "RED";

        ushort tid = (ushort)((saveBytes[0x2605] << 8) | saveBytes[0x2606]);
        result.TrainerId = tid.ToString();

        // Money BCD at 0x2999..0x299B
        byte m0 = saveBytes[0x2999];
        byte m1 = saveBytes[0x299A];
        byte m2 = saveBytes[0x299B];
        result.Money = (((m0 >> 4) * 10 + (m0 & 0x0F)) * 10000) +
                       (((m1 >> 4) * 10 + (m1 & 0x0F)) * 100) +
                       ((m2 >> 4) * 10 + (m2 & 0x0F));

        // Pokedex
        var caughtIds = new HashSet<int>();
        var seenIds = new HashSet<int>();
        var cBytes = new byte[19];
        for (int i = 0; i < 19; i++)
        {
            int cIdx = 0x25A3 + i;
            cBytes[i] = cIdx < saveBytes.Length ? saveBytes[cIdx] : (byte)0;
        }
        bool isErasedOrCorrupt = cBytes.All(b => b == 0xFF) || cBytes.Count(b => b == 0xFF) >= 12;
        if (!isErasedOrCorrupt)
        {
            for (int offset = 0; offset < 19; offset++)
            {
                int cIdx = 0x25A3 + offset;
                int sIdx = 0x25B6 + offset;
                byte cByte = cIdx < saveBytes.Length ? saveBytes[cIdx] : (byte)0;
                byte sByte = sIdx < saveBytes.Length ? saveBytes[sIdx] : (byte)0;
                for (int bit = 0; bit < 8; bit++)
                {
                    int speciesNum = offset * 8 + bit + 1;
                    if (speciesNum <= 151)
                    {
                        if ((cByte & (1 << bit)) != 0)
                        {
                            caughtIds.Add(speciesNum);
                            seenIds.Add(speciesNum);
                        }
                        if ((sByte & (1 << bit)) != 0)
                        {
                            seenIds.Add(speciesNum);
                        }
                    }
                }
            }
        }

        // 1. Parse Party
        var partyList = new List<PokemonSummaryDto>();
        int rawPartyCount = saveBytes[0x2F2C];
        int partyCount = Math.Clamp(rawPartyCount, 0, 6);

        for (int slot = 0; slot < partyCount; slot++)
        {
            int pOffset = 0x2F34 + slot * 44;
            int otOffset = 0x303C + slot * 11;
            int nickOffset = 0x307E + slot * 11;

            var pkm = ParsePokemon(saveBytes, pOffset, otOffset, nickOffset, true, slot + 1, null, gameId, catalog);
            if (pkm != null)
            {
                partyList.Add(pkm.Summary);
                result.Details[pkm.Summary.Id] = pkm;
            }
        }
        result.Party = partyList;

        // 2. Parse Boxes (1..12)
        int currentBoxIdx = saveBytes[0x284C] & 0x7F; // 0..11
        var boxesList = new List<PokemonBoxDto>();

        for (int b = 0; b < 12; b++)
        {
            int boxIndex = b + 1;
            var boxDto = new PokemonBoxDto
            {
                BoxIndex = boxIndex,
                Name = $"Box {boxIndex}",
                Capacity = 20,
            };

            var entries = new List<PokemonSummaryDto>();

            if (b == currentBoxIdx)
            {
                // Active Box at 0x30C0
                int bCount = Math.Clamp((int)saveBytes[0x30C0], 0, 20);
                for (int slot = 0; slot < bCount; slot++)
                {
                    int pOffset = 0x30D6 + slot * 33;
                    int otOffset = 0x336A + slot * 11;
                    int nickOffset = 0x3446 + slot * 11;

                    var pkm = ParsePokemon(saveBytes, pOffset, otOffset, nickOffset, false, slot + 1, boxIndex, gameId, catalog);
                    if (pkm != null)
                    {
                        entries.Add(pkm.Summary);
                        result.Details[pkm.Summary.Id] = pkm;
                    }
                }
            }
            else
            {
                int bankBase = b < 6 ? 0x4000 : 0x6000;
                int boxOffset = bankBase + (b % 6) * 1122;

                if (boxOffset + 1122 <= saveBytes.Length)
                {
                    int rawCount = saveBytes[boxOffset];
                    if (rawCount >= 0 && rawCount <= 20)
                    {
                        for (int slot = 0; slot < rawCount; slot++)
                        {
                            int pOffset = boxOffset + 22 + slot * 33;
                            int otOffset = boxOffset + 22 + (20 * 33) + slot * 11;
                            int nickOffset = boxOffset + 22 + (20 * 33) + (20 * 11) + slot * 11;

                            var pkm = ParsePokemon(saveBytes, pOffset, otOffset, nickOffset, false, slot + 1, boxIndex, gameId, catalog);
                            if (pkm != null)
                            {
                                entries.Add(pkm.Summary);
                                result.Details[pkm.Summary.Id] = pkm;
                            }
                        }
                    }
                }
            }

            boxDto.OccupiedCount = entries.Count;
            boxDto.Entries = entries;
            boxesList.Add(boxDto);
        }

        foreach (var p in partyList)
        {
            if (p.SpeciesId is > 0 and <= 151)
            {
                caughtIds.Add(p.SpeciesId);
                seenIds.Add(p.SpeciesId);
            }
        }
        foreach (var b in boxesList)
        {
            foreach (var p in b.Entries)
            {
                if (p.SpeciesId is > 0 and <= 151)
                {
                    caughtIds.Add(p.SpeciesId);
                    seenIds.Add(p.SpeciesId);
                }
            }
        }

        int totalOwnedCount = partyList.Count + boxesList.Sum(b => b.OccupiedCount);
        if (totalOwnedCount == 0)
        {
            caughtIds.Clear();
            seenIds.Clear();
        }

        result.PokedexCaught = caughtIds.Count;
        result.PokedexSeen = seenIds.Count;
        result.CaughtSpeciesIds = caughtIds.OrderBy(id => id).ToList();
        result.SeenSpeciesIds = seenIds.OrderBy(id => id).ToList();

        result.Boxes = boxesList;
        result.IsSuccess = true;
        return result;
    }

    public static PokemonDetailsDto? ParsePokemon(
        byte[] buffer,
        int offset,
        int otOffset,
        int nickOffset,
        bool isParty,
        int partySlot,
        int? boxIndex,
        string gameTitle,
        PkVaultStaticCatalog catalog)
    {
        int requiredSize = isParty ? 44 : 33;
        if (offset + requiredSize > buffer.Length) return null;

        int internalId = buffer[offset];
        if (internalId <= 0 || internalId >= Gen1InternalToDex.Length) return null;

        int speciesId = Gen1InternalToDex[internalId];
        if (speciesId <= 0 || speciesId > 151) return null;

        int currentHp = (buffer[offset + 1] << 8) | buffer[offset + 2];
        int boxLevel = buffer[offset + 3];

        int m1 = buffer[offset + 8];
        int m2 = buffer[offset + 9];
        int m3 = buffer[offset + 10];
        int m4 = buffer[offset + 11];

        var movesList = new List<string>();
        foreach (var m in new[] { m1, m2, m3, m4 })
        {
            if (m > 0) movesList.Add(catalog.ResolveMoveName(m));
        }

        ushort otid = (ushort)((buffer[offset + 0x0C] << 8) | buffer[offset + 0x0D]);
        uint exp = (uint)((buffer[offset + 0x0E] << 16) | (buffer[offset + 0x0F] << 8) | buffer[offset + 0x10]);

        // Stat Experience / EVs (0..65535, scale to 0..252)
        int hpEv = ((buffer[offset + 0x11] << 8) | buffer[offset + 0x12]) / 256;
        int atkEv = ((buffer[offset + 0x13] << 8) | buffer[offset + 0x14]) / 256;
        int defEv = ((buffer[offset + 0x15] << 8) | buffer[offset + 0x16]) / 256;
        int speEv = ((buffer[offset + 0x17] << 8) | buffer[offset + 0x18]) / 256;
        int spcEv = ((buffer[offset + 0x19] << 8) | buffer[offset + 0x1A]) / 256;

        var evStats = new PokemonStatsDto
        {
            Hp = Math.Clamp(hpEv, 0, 252),
            Attack = Math.Clamp(atkEv, 0, 252),
            Defense = Math.Clamp(defEv, 0, 252),
            Speed = Math.Clamp(speEv, 0, 252),
            SpecialAttack = Math.Clamp(spcEv, 0, 252),
            SpecialDefense = Math.Clamp(spcEv, 0, 252)
        };

        // DVs / IVs
        ushort ivRaw = (ushort)((buffer[offset + 0x1B] << 8) | buffer[offset + 0x1C]);
        int atkDv = (ivRaw >> 12) & 0x0F;
        int defDv = (ivRaw >> 8) & 0x0F;
        int speDv = (ivRaw >> 4) & 0x0F;
        int spcDv = ivRaw & 0x0F;
        int hpDv = ((atkDv & 1) << 3) | ((defDv & 1) << 2) | ((speDv & 1) << 1) | (spcDv & 1);

        var ivStats = new PokemonStatsDto
        {
            Hp = (hpDv * 31) / 15,
            Attack = (atkDv * 31) / 15,
            Defense = (defDv * 31) / 15,
            Speed = (speDv * 31) / 15,
            SpecialAttack = (spcDv * 31) / 15,
            SpecialDefense = (spcDv * 31) / 15
        };

        // Time Capsule shiny rule
        bool isShiny = speDv == 10 && defDv == 10 && spcDv == 10 && (atkDv is 2 or 3 or 6 or 7 or 10 or 11 or 14 or 15);

        int level = isParty ? Math.Clamp((int)buffer[offset + 0x21], 1, 100) : Math.Clamp(boxLevel, 1, 100);
        int? maxHp = isParty ? (int)((buffer[offset + 0x22] << 8) | buffer[offset + 0x23]) : null;

        string otName = DecodeGen1String(buffer, otOffset, 11);
        if (string.IsNullOrWhiteSpace(otName)) otName = "RED";

        string rawNick = DecodeGen1String(buffer, nickOffset, 11);
        string speciesName = catalog.ResolveSpeciesName(speciesId, rawNick, !string.IsNullOrWhiteSpace(rawNick));
        string nickname = string.IsNullOrWhiteSpace(rawNick) ? speciesName : rawNick;

        string natureName = NatureNames[(exp % 25)];
        string pkmId = $"gen1_{speciesId}_{otid}_{partySlot}_{(isParty ? "p" : $"b{boxIndex}")}";

        var summary = new PokemonSummaryDto
        {
            Id = pkmId,
            Species = speciesName,
            SpeciesId = speciesId,
            Nickname = nickname,
            Level = level,
            Gender = "Genderless",
            IsShiny = isShiny,
            OriginalTrainer = otName,
            OriginalTrainerId = otid.ToString(),
            OriginGame = gameTitle,
            CurrentLocation = isParty ? "Party" : $"Box {boxIndex}",
            BoxIndex = boxIndex,
            SlotIndex = partySlot,
            IsInParty = isParty,
            LegalityStatus = "valid"
        };

        var details = new PokemonDetailsDto
        {
            Summary = summary,
            Nature = natureName,
            Ability = catalog.ResolveAbilityName(speciesId) ?? "Static",
            HeldItem = null,
            Moves = movesList,
            Iv = ivStats,
            Ev = evStats,
            CurrentHp = currentHp,
            MaxHp = maxHp,
            Friendship = 70,
            Pokeball = "Poké Ball",
            LegalityStatus = "valid"
        };

        return details;
    }

    private static string DecodeGen1String(byte[] buffer, int offset, int length)
    {
        var sb = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            int idx = offset + i;
            if (idx >= buffer.Length) break;
            byte b = buffer[idx];
            if (b == 0x50) break; // 0x50 is string terminator in Gen 1

            switch (b)
            {
                case 0x7F: sb.Append(' '); break;
                case >= 0x80 and <= 0x99: sb.Append((char)('A' + (b - 0x80))); break;
                case >= 0xA0 and <= 0xB9: sb.Append((char)('a' + (b - 0xA0))); break;
                case >= 0xF6 and <= 0xFF: sb.Append((char)('0' + (b - 0xF6))); break;
                case 0xE0: sb.Append('\''); break;
                case 0xE1: sb.Append("PK"); break;
                case 0xE2: sb.Append("MN"); break;
                case 0xE3: sb.Append('-'); break;
                case 0xE8: sb.Append('.'); break;
                case 0xEF: sb.Append('♂'); break;
                case 0xF5: sb.Append('♀'); break;
                case 0xE4: sb.Append('?'); break;
                case 0xE5: sb.Append('!'); break;
            }
        }
        return sb.ToString().Trim();
    }
}

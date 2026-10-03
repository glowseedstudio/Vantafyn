using System;
using System.Collections.Generic;
using System.Text;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon.Native;

/// <summary>
/// Native high-performance parser for Generation 2 Pokémon save files (Gold, Silver, Crystal).
/// Completely self-contained; requires zero external services or Docker containers.
/// </summary>
public static class Gen2SaveParser
{
    private static readonly string[] NatureNames =
    [
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky"
    ];

    public static bool IsGen2Save(byte[] saveBytes)
    {
        if (saveBytes == null || saveBytes.Length != 32768)
        {
            return false;
        }

        if (Gen1SaveParser.IsGen1Save(saveBytes))
        {
            return false;
        }

        int gsCount = saveBytes[0x288A];
        int cryCount = saveBytes[0x2865];

        bool isGs = gsCount is >= 0 and <= 6 && (gsCount == 0 || (saveBytes[0x288B] is >= 1 and <= 251));
        bool isCry = cryCount is >= 0 and <= 6 && (cryCount == 0 || (saveBytes[0x2866] is >= 1 and <= 251));

        if (!isGs && !isCry) return false;

        string tName = DecodeGen2String(saveBytes, 0x2009, 11);
        return !string.IsNullOrWhiteSpace(tName);
    }

    public static PokemonSaveParseResult Parse(byte[] saveBytes, string gameId, PkVaultStaticCatalog catalog)
    {
        var result = new PokemonSaveParseResult();

        if (saveBytes == null || saveBytes.Length != 32768)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save data is missing or invalid size for Gen 2 save.";
            return result;
        }

        int cryCount = saveBytes[0x2865];
        int gsCount = saveBytes[0x288A];
        bool isCrystal = cryCount is >= 1 and <= 6 && saveBytes[0x2866] is >= 1 and <= 251;

        result.TrainerName = DecodeGen2String(saveBytes, 0x2009, 11);
        if (string.IsNullOrWhiteSpace(result.TrainerName)) result.TrainerName = "GOLD";

        ushort tid = (ushort)((saveBytes[0x2002] << 8) | saveBytes[0x2003]);
        result.TrainerId = tid.ToString();

        result.GymBadges = PokemonGymBadgeCatalog.ForGen2(saveBytes[0x23E5], saveBytes[0x23E6]);

        // Money BCD at 0x23DB..0x23DD
        byte m0 = saveBytes[0x23DB];
        byte m1 = saveBytes[0x23DC];
        byte m2 = saveBytes[0x23DD];
        result.Money = (((m0 >> 4) * 10 + (m0 & 0x0F)) * 10000) +
                       (((m1 >> 4) * 10 + (m1 & 0x0F)) * 100) +
                       ((m2 >> 4) * 10 + (m2 & 0x0F));

        // Pokedex bitfields (GS: 0x2A4C caught, 0x2A6C seen; Crystal: 0x2A6C caught, 0x2A8C seen)
        var caughtIds = new HashSet<int>();
        var seenIds = new HashSet<int>();
        int caughtStart = isCrystal ? 0x2A6C : 0x2A4C;
        int seenStart = isCrystal ? 0x2A8C : 0x2A6C;
        var cBytes = new byte[32];
        for (int i = 0; i < 32; i++)
        {
            int cIdx = caughtStart + i;
            cBytes[i] = cIdx < saveBytes.Length ? saveBytes[cIdx] : (byte)0;
        }
        bool isErasedOrCorrupt = cBytes.All(b => b == 0xFF) || cBytes.Count(b => b == 0xFF) >= 16;
        if (!isErasedOrCorrupt)
        {
            for (int offset = 0; offset < 32; offset++)
            {
                int cIdx = caughtStart + offset;
                int sIdx = seenStart + offset;
                byte cByte = cIdx < saveBytes.Length ? saveBytes[cIdx] : (byte)0;
                byte sByte = sIdx < saveBytes.Length ? saveBytes[sIdx] : (byte)0;
                for (int bit = 0; bit < 8; bit++)
                {
                    int speciesNum = offset * 8 + bit + 1;
                    if (speciesNum <= 251)
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

        int partyOffset = isCrystal ? 0x2865 : 0x288A;
        var partyList = new List<PokemonSummaryDto>();

        int rawPartyCount = saveBytes[partyOffset];
        int partyCount = Math.Clamp(rawPartyCount, 0, 6);

        int partyStructsOffset = partyOffset + 8;
        int partyOtOffset = partyStructsOffset + (6 * 48);
        int partyNickOffset = partyOtOffset + (6 * 11);

        for (int slot = 0; slot < partyCount; slot++)
        {
            int pOffset = partyStructsOffset + slot * 48;
            int otOffset = partyOtOffset + slot * 11;
            int nickOffset = partyNickOffset + slot * 11;

            var pkm = ParsePokemon(saveBytes, pOffset, otOffset, nickOffset, true, slot + 1, null, gameId, catalog);
            if (pkm != null)
            {
                partyList.Add(pkm.Summary);
                result.Details[pkm.Summary.Id] = pkm;
            }
        }
        result.Party = partyList;

        // 2. Parse Boxes (1..14)
        int currentBoxIdx = saveBytes[0x2724] & 0x7F; // 0..13
        var boxesList = new List<PokemonBoxDto>();

        for (int b = 0; b < 14; b++)
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
                // Active Box at 0x2D10
                int bCount = Math.Clamp((int)saveBytes[0x2D10], 0, 20);
                int boxStructs = 0x2D10 + 22;
                int boxOt = boxStructs + (20 * 32);
                int boxNick = boxOt + (20 * 11);

                for (int slot = 0; slot < bCount; slot++)
                {
                    int pOffset = boxStructs + slot * 32;
                    int otOffset = boxOt + slot * 11;
                    int nickOffset = boxNick + slot * 11;

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
                int bankBase = b < 7 ? 0x4000 : 0x6000;
                int boxOffset = bankBase + (b % 7) * 1102;

                if (boxOffset + 1102 <= saveBytes.Length)
                {
                    int rawCount = saveBytes[boxOffset];
                    if (rawCount is >= 0 and <= 20)
                    {
                        int boxStructs = boxOffset + 22;
                        int boxOt = boxStructs + (20 * 32);
                        int boxNick = boxOt + (20 * 11);

                        for (int slot = 0; slot < rawCount; slot++)
                        {
                            int pOffset = boxStructs + slot * 32;
                            int otOffset = boxOt + slot * 11;
                            int nickOffset = boxNick + slot * 11;

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
            if (p.SpeciesId is > 0 and <= 251)
            {
                caughtIds.Add(p.SpeciesId);
                seenIds.Add(p.SpeciesId);
            }
        }
        foreach (var b in boxesList)
        {
            foreach (var p in b.Entries)
            {
                if (p.SpeciesId is > 0 and <= 251)
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
        int requiredSize = isParty ? 48 : 32;
        if (offset + requiredSize > buffer.Length) return null;

        int speciesId = buffer[offset];
        if (speciesId is <= 0 or > 251) return null;

        int heldItemId = buffer[offset + 1];
        int m1 = buffer[offset + 2];
        int m2 = buffer[offset + 3];
        int m3 = buffer[offset + 4];
        int m4 = buffer[offset + 5];

        var movesList = new List<string>();
        foreach (var m in new[] { m1, m2, m3, m4 })
        {
            if (m > 0) movesList.Add(catalog.ResolveMoveName(m));
        }

        ushort otid = (ushort)((buffer[offset + 6] << 8) | buffer[offset + 7]);
        uint exp = (uint)((buffer[offset + 8] << 16) | (buffer[offset + 9] << 8) | buffer[offset + 10]);

        // Stat Experience / EVs
        int hpEv = ((buffer[offset + 0x0B] << 8) | buffer[offset + 0x0C]) / 256;
        int atkEv = ((buffer[offset + 0x0D] << 8) | buffer[offset + 0x0E]) / 256;
        int defEv = ((buffer[offset + 0x0F] << 8) | buffer[offset + 0x10]) / 256;
        int speEv = ((buffer[offset + 0x11] << 8) | buffer[offset + 0x12]) / 256;
        int spcEv = ((buffer[offset + 0x13] << 8) | buffer[offset + 0x14]) / 256;

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
        ushort ivRaw = (ushort)((buffer[offset + 0x15] << 8) | buffer[offset + 0x16]);
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

        bool isShiny = speDv == 10 && defDv == 10 && spcDv == 10 && (atkDv is 2 or 3 or 6 or 7 or 10 or 11 or 14 or 15);
        int friendship = buffer[offset + 0x1B];
        int rawLevel = buffer[offset + 0x1F];
        int level = rawLevel is >= 1 and <= 100 ? rawLevel : 5;

        int? currentHp = isParty ? (int)((buffer[offset + 0x22] << 8) | buffer[offset + 0x23]) : null;
        int? maxHp = isParty ? (int)((buffer[offset + 0x24] << 8) | buffer[offset + 0x25]) : null;

        string otName = DecodeGen2String(buffer, otOffset, 11);
        if (string.IsNullOrWhiteSpace(otName)) otName = "GOLD";

        string rawNick = DecodeGen2String(buffer, nickOffset, 11);
        string speciesName = catalog.ResolveSpeciesName(speciesId, rawNick, !string.IsNullOrWhiteSpace(rawNick));
        string nickname = string.IsNullOrWhiteSpace(rawNick) ? speciesName : rawNick;

        string natureName = NatureNames[(exp % 25)];
        string pkmId = $"gen2_{speciesId}_{otid}_{partySlot}_{(isParty ? "p" : $"b{boxIndex}")}";

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
            HeldItem = heldItemId > 0 ? $"Item #{heldItemId}" : null,
            Moves = movesList,
            Iv = ivStats,
            Ev = evStats,
            CurrentHp = currentHp,
            MaxHp = maxHp,
            Friendship = friendship,
            Pokeball = "Poké Ball",
            LegalityStatus = "valid"
        };

        return details;
    }

    private static string DecodeGen2String(byte[] buffer, int offset, int length)
    {
        var sb = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            int idx = offset + i;
            if (idx >= buffer.Length) break;
            byte b = buffer[idx];
            if (b == 0x50) break;

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

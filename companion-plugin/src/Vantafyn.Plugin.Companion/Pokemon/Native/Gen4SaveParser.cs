using System;
using System.Collections.Generic;
using System.Text;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon.Native;

/// <summary>
/// Native high-performance parser for Generation 4 Pokémon save files (Diamond, Pearl, Platinum, HeartGold, SoulSilver).
/// Completely self-contained; requires zero external services or Docker containers.
/// </summary>
public static class Gen4SaveParser
{
    private const int PartitionSize = 0x40000; // 256 KB
    private const int PokedexRegionSize = 0x40;
    private const int PokedexCaughtRegion = 0;
    private const int PokedexSeenRegion = 1;
    private const int Gen4SpeciesCount = 493;

    private static readonly int[][] BlockOrders =
    [
        [0, 1, 2, 3], [0, 1, 3, 2], [0, 2, 1, 3], [0, 2, 3, 1], [0, 3, 1, 2], [0, 3, 2, 1],
        [1, 0, 2, 3], [1, 0, 3, 2], [1, 2, 0, 3], [1, 2, 3, 0], [1, 3, 0, 2], [1, 3, 2, 0],
        [2, 0, 1, 3], [2, 0, 3, 1], [2, 1, 0, 3], [2, 1, 3, 0], [2, 3, 0, 1], [2, 3, 1, 0],
        [3, 0, 1, 2], [3, 0, 2, 1], [3, 1, 0, 2], [3, 1, 2, 0], [3, 2, 0, 1], [3, 2, 1, 0],
    ];

    private static readonly string[] NatureNames =
    [
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky"
    ];

    public static bool IsGen4Save(byte[] saveBytes, string gameId = "")
    {
        if (saveBytes == null || saveBytes.Length < 0x40000) return false;
        var lower = (gameId ?? string.Empty).ToLowerInvariant();
        return lower.Contains("diamond") || lower.Contains("pearl") ||
               lower.Contains("platinum") || lower.Contains("heartgold") ||
               lower.Contains("soulsilver") || lower.Contains("heart gold") ||
               lower.Contains("soul silver") || lower.Contains("hgss");
    }

    private static uint ReadUInt32LE(byte[] data, int offset)
    {
        if (offset + 4 > data.Length || offset < 0) return uint.MaxValue;
        return (uint)(data[offset] | (data[offset + 1] << 8) | (data[offset + 2] << 16) | (data[offset + 3] << 24));
    }

    private static ushort ReadUInt16LE(byte[] data, int offset)
    {
        if (offset + 2 > data.Length || offset < 0) return 0;
        return (ushort)(data[offset] | (data[offset + 1] << 8));
    }

    private static int GetActiveSlot(byte[] saveBytes, int blockStart, int blockSize)
    {
        int footerOffset = blockStart + blockSize - 0x14;
        uint c1 = ReadUInt32LE(saveBytes, footerOffset);
        if (footerOffset + PartitionSize + 4 > saveBytes.Length)
        {
            return c1 != uint.MaxValue ? 0 : -1;
        }
        uint c2 = ReadUInt32LE(saveBytes, footerOffset + PartitionSize);
        return (c2 != uint.MaxValue && (c1 == uint.MaxValue || c2 > c1)) ? 1 : 0;
    }

    private static (byte johtoOrSinnoh, byte kanto) ReadBadgeFlags(byte[] saveBytes, int generalBase, int trainerNameOffset, bool isHgss)
    {
        int trainerStart = generalBase + trainerNameOffset;
        int nameEnd = trainerStart + 16;
        if (trainerStart < 0 || nameEnd > saveBytes.Length) return (0, 0);
        bool erased = true;
        for (int i = trainerStart; i < nameEnd; i++)
        {
            if (saveBytes[i] != 0xFF)
            {
                erased = false;
                break;
            }
        }
        if (erased) return (0, 0);

        byte johtoOrSinnoh = (trainerStart + 0x1A < saveBytes.Length) ? saveBytes[trainerStart + 0x1A] : (byte)0;
        byte kanto = (isHgss && trainerStart + 0x1F < saveBytes.Length) ? saveBytes[trainerStart + 0x1F] : (byte)0;
        return (johtoOrSinnoh, kanto);
    }

    public static PokemonSaveParseResult Parse(byte[] saveBytes, string gameId, PkVaultStaticCatalog catalog)
    {
        var result = new PokemonSaveParseResult();
        if (saveBytes == null || saveBytes.Length < 0x40000)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save file size is invalid for Gen 4 (must be >= 256KB).";
            return result;
        }

        var lower = (gameId ?? string.Empty).ToLowerInvariant();
        int generalSize = 0xC100;
        int storageSize = 0x121E0;
        int storageStart = 0xC100;
        int partyOffset = 0x98;
        int partyCountOffset = 0x94;
        int boxDataStart = 4;
        int pokedexOffset = 0x12DC;
        int trainerNameOffset = 0x64;
        int tidOffset = 0x74;

        if (lower.Contains("platinum"))
        {
            generalSize = 0xCF2C;
            storageSize = 0x121E4;
            storageStart = 0xCF2C;
            partyOffset = 0xA0;
            partyCountOffset = 0x9C;
            boxDataStart = 4;
            pokedexOffset = 0x1328;
            trainerNameOffset = 0x68;
            tidOffset = 0x78;
        }
        else if (lower.Contains("heartgold") || lower.Contains("soulsilver") || lower.Contains("heart gold") || lower.Contains("soul silver") || lower.Contains("hgss"))
        {
            generalSize = 0xF628;
            storageSize = 0x12310;
            storageStart = 0xF700;
            partyOffset = 0x98;
            partyCountOffset = 0x94;
            boxDataStart = 0;
            pokedexOffset = 0x12B8;
            trainerNameOffset = 0x64;
            tidOffset = 0x74;
        }

        int activeGeneral = GetActiveSlot(saveBytes, 0, generalSize);
        int activeStorage = GetActiveSlot(saveBytes, storageStart, storageSize);

        int generalBase = activeGeneral == 1 ? PartitionSize : 0;
        int storageBase = (activeStorage == 1 ? PartitionSize : 0) + storageStart;
        var pokedex = ReadPokedexFlags(saveBytes, generalBase, pokedexOffset);

        string otName = DecodeUtf16String(saveBytes, generalBase + trainerNameOffset, 7);
        if (string.IsNullOrWhiteSpace(otName)) otName = "TRAINER";
        int tid = ReadUInt16LE(saveBytes, generalBase + tidOffset);

        result.TrainerName = otName;
        result.TrainerId = tid.ToString();

        // 1. Party
        int partyCount = Math.Clamp((int)saveBytes[generalBase + partyCountOffset], 0, 6);
        for (int slot = 0; slot < partyCount; slot++)
        {
            int pkOffset = generalBase + partyOffset + (slot * 236);
            if (pkOffset + 236 <= saveBytes.Length)
            {
                var pkm = ParsePokemon(saveBytes, pkOffset, 236, isParty: true, slot + 1, boxIndex: null, otName, tid, gameId ?? string.Empty, catalog);
                if (pkm != null)
                {
                    result.Party.Add(pkm.Value.Summary);
                    result.Details[pkm.Value.Summary.Id] = pkm.Value.Details;
                }
            }
        }

        // 2. Boxes (18 boxes)
        for (int b = 0; b < 18; b++)
        {
            var entries = new List<PokemonSummaryDto>();
            var box = new PokemonBoxDto
            {
                BoxIndex = b + 1,
                Name = $"Box {b + 1}",
                Capacity = 30,
                OccupiedCount = 0,
                Entries = entries
            };

            int boxStart = storageBase + boxDataStart + (b * 30 * 136);
            for (int s = 0; s < 30; s++)
            {
                int pkOffset = boxStart + (s * 136);
                if (pkOffset + 136 <= saveBytes.Length)
                {
                    var pkm = ParsePokemon(saveBytes, pkOffset, 136, isParty: false, s + 1, boxIndex: b + 1, otName, tid, gameId ?? string.Empty, catalog);
                    if (pkm != null)
                    {
                        entries.Add(pkm.Value.Summary);
                        result.Details[pkm.Value.Summary.Id] = pkm.Value.Details;
                    }
                }
            }

            box.OccupiedCount = entries.Count;
            result.Boxes.Add(box);
        }

        result.DetectedGeneration = 4;
        result.PokedexCaught = pokedex?.CaughtSpeciesIds.Count;
        result.PokedexSeen = pokedex?.SeenSpeciesIds.Count;
        result.CaughtSpeciesIds = pokedex?.CaughtSpeciesIds ?? [];
        result.SeenSpeciesIds = pokedex?.SeenSpeciesIds ?? [];

        bool isHgss = lower.Contains("heartgold") || lower.Contains("soulsilver") ||
                      lower.Contains("heart gold") || lower.Contains("soul silver") ||
                      lower.Contains("hgss");
        var (johtoOrSinnoh, kanto) = ReadBadgeFlags(saveBytes, generalBase, trainerNameOffset, isHgss);
        result.GymBadges = PokemonGymBadgeCatalog.ForGen4(gameId ?? string.Empty, johtoOrSinnoh, kanto);

        bool isUninitialized = partyCount == 0 &&
                               result.Boxes.All(b => b.OccupiedCount == 0) &&
                               (pokedex == null || pokedex.Value.CaughtSpeciesIds.Count == 0) &&
                               result.GymBadges.All(r => r.Badges.All(b => !b.IsEarned)) &&
                               (string.IsNullOrWhiteSpace(otName) || otName == "TRAINER") &&
                               tid == 0;
        if (isUninitialized || activeGeneral < 0)
        {
            result.GymBadges = [];
            result.Party = [];
            result.Boxes = [];
            result.CaughtSpeciesIds = [];
            result.SeenSpeciesIds = [];
            result.PokedexCaught = null;
            result.PokedexSeen = null;
            result.IsSuccess = false;
            result.ErrorMessage = "Save file is uninitialized (no in-game save exists).";
            return result;
        }

        result.IsSuccess = true;
        return result;
    }

    private static (List<int> CaughtSpeciesIds, List<int> SeenSpeciesIds)? ReadPokedexFlags(byte[] saveBytes, int generalBase, int pokedexOffset)
    {
        int dexStart = generalBase + pokedexOffset;
        int dexEnd = dexStart + 4 + (PokedexRegionSize * 4);
        if (dexStart < 0 || dexEnd > saveBytes.Length) return null;

        int caughtRegion = dexStart + 4 + (PokedexCaughtRegion * PokedexRegionSize);
        int seenRegion = dexStart + 4 + (PokedexSeenRegion * PokedexRegionSize);
        if (IsErased(saveBytes, caughtRegion, PokedexRegionSize) && IsErased(saveBytes, seenRegion, PokedexRegionSize))
        {
            return null;
        }

        var caughtIds = new List<int>();
        var seenIds = new SortedSet<int>();
        for (int speciesId = 1; speciesId <= Gen4SpeciesCount; speciesId++)
        {
            int bitIndex = speciesId - 1;
            if (ReadFlag(saveBytes, caughtRegion, bitIndex))
            {
                caughtIds.Add(speciesId);
                seenIds.Add(speciesId);
            }
            if (ReadFlag(saveBytes, seenRegion, bitIndex))
            {
                seenIds.Add(speciesId);
            }
        }

        return (caughtIds.OrderBy(id => id).ToList(), seenIds.ToList());
    }

    private static bool ReadFlag(byte[] data, int offset, int bitIndex)
    {
        int byteOffset = offset + (bitIndex / 8);
        if (byteOffset < 0 || byteOffset >= data.Length) return false;
        int mask = 1 << (bitIndex % 8);
        return (data[byteOffset] & mask) != 0;
    }

    private static bool IsErased(byte[] data, int offset, int length)
    {
        if (offset < 0 || offset + length > data.Length) return false;
        for (int i = 0; i < length; i++)
        {
            if (data[offset + i] != 0xFF) return false;
        }
        return true;
    }

    private static (PokemonSummaryDto Summary, PokemonDetailsDto Details)? ParsePokemon(
        byte[] buffer,
        int offset,
        int length,
        bool isParty,
        int slot,
        int? boxIndex,
        string defaultOt,
        int defaultTid,
        string gameId,
        PkVaultStaticCatalog catalog)
    {
        if (offset + length > buffer.Length || length < 136) return null;

        uint pid = ReadUInt32LE(buffer, offset);
        ushort chk = ReadUInt16LE(buffer, offset + 6);
        if (pid == 0 && chk == 0) return null;

        byte[] d = new byte[length];
        Array.Copy(buffer, offset, d, 0, length);

        uint seed = chk;
        for (int i = 8; i < 136; i += 2)
        {
            seed = (0x41C64E6Du * seed + 0x6073u);
            ushort xor = (ushort)(seed >> 16);
            ushort w = (ushort)(d[i] | (d[i + 1] << 8));
            w ^= xor;
            d[i] = (byte)(w & 0xFF);
            d[i + 1] = (byte)(w >> 8);
        }

        if (length >= 236)
        {
            uint partySeed = pid;
            for (int i = 136; i < 236; i += 2)
            {
                partySeed = (0x41C64E6Du * partySeed + 0x6073u);
                ushort xor = (ushort)(partySeed >> 16);
                ushort w = (ushort)(d[i] | (d[i + 1] << 8));
                w ^= xor;
                d[i] = (byte)(w & 0xFF);
                d[i + 1] = (byte)(w >> 8);
            }
        }

        int shiftVal = (int)(((pid >> 13) & 31) % 24);
        var order = BlockOrders[shiftVal];

        byte[] unshuffled = new byte[length];
        Array.Copy(d, 0, unshuffled, 0, 8);
        for (int pos = 0; pos < 4; pos++)
        {
            int blk = order[pos];
            Array.Copy(d, 8 + pos * 32, unshuffled, 8 + blk * 32, 32);
        }
        if (length > 136)
        {
            Array.Copy(d, 136, unshuffled, 136, length - 136);
        }

        ushort speciesId = ReadUInt16LE(unshuffled, 8);
        if (speciesId is <= 0 or > 493) return null;

        ushort heldItem = ReadUInt16LE(unshuffled, 10);
        ushort otId = ReadUInt16LE(unshuffled, 12);
        ushort secretId = ReadUInt16LE(unshuffled, 14);
        uint exp = ReadUInt32LE(unshuffled, 16);
        int friendship = unshuffled[20];
        int abilityId = unshuffled[21];

        int hpEv = unshuffled[24];
        int atkEv = unshuffled[25];
        int defEv = unshuffled[26];
        int speEv = unshuffled[27];
        int spaEv = unshuffled[28];
        int spdEv = unshuffled[29];

        var evStats = new PokemonStatsDto
        {
            Hp = Math.Clamp(hpEv, 0, 252),
            Attack = Math.Clamp(atkEv, 0, 252),
            Defense = Math.Clamp(defEv, 0, 252),
            Speed = Math.Clamp(speEv, 0, 252),
            SpecialAttack = Math.Clamp(spaEv, 0, 252),
            SpecialDefense = Math.Clamp(spdEv, 0, 252)
        };

        ushort m1 = ReadUInt16LE(unshuffled, 40);
        ushort m2 = ReadUInt16LE(unshuffled, 42);
        ushort m3 = ReadUInt16LE(unshuffled, 44);
        ushort m4 = ReadUInt16LE(unshuffled, 46);
        var movesList = new List<string>();
        foreach (var m in new[] { m1, m2, m3, m4 })
        {
            if (m > 0) movesList.Add(PokemonMoveCatalog.ResolveMoveName(m));
        }

        uint iv32 = ReadUInt32LE(unshuffled, 56);
        int hpIv = (int)(iv32 & 0x1F);
        int atkIv = (int)((iv32 >> 5) & 0x1F);
        int defIv = (int)((iv32 >> 10) & 0x1F);
        int speIv = (int)((iv32 >> 15) & 0x1F);
        int spaIv = (int)((iv32 >> 20) & 0x1F);
        int spdIv = (int)((iv32 >> 25) & 0x1F);
        bool isEgg = ((iv32 >> 30) & 1) == 1;

        var ivStats = new PokemonStatsDto
        {
            Hp = hpIv,
            Attack = atkIv,
            Defense = defIv,
            Speed = speIv,
            SpecialAttack = spaIv,
            SpecialDefense = spdIv
        };

        int genderFlag = (unshuffled[64] & 0x06) >> 1;
        string genderStr = genderFlag switch
        {
            0 => "M",
            1 => "F",
            _ => "Genderless"
        };

        string rawNick = DecodeUtf16String(unshuffled, 72, 11);
        string speciesName = PokemonSpeciesCatalog.ResolveSpeciesName(speciesId);
        string nickname = isEgg ? "Egg" : (string.IsNullOrWhiteSpace(rawNick) ? speciesName : rawNick);

        string pkmOtName = DecodeUtf16String(unshuffled, 104, 8);
        if (string.IsNullOrWhiteSpace(pkmOtName)) pkmOtName = defaultOt;

        int level = isParty && length >= 236 ? Math.Clamp((int)unshuffled[140], 1, 100) : CalculateLevelFromExp(speciesId, exp);
        int currentHp = isParty && length >= 236 ? ReadUInt16LE(unshuffled, 142) : 0;
        int maxHp = isParty && length >= 236 ? ReadUInt16LE(unshuffled, 144) : 0;

        int shinyVal = (otId ^ secretId) ^ ((int)(pid & 0xFFFF) ^ (int)(pid >> 16));
        bool isShiny = shinyVal < 8;

        string nature = NatureNames[(int)(pid % 25)];
        string pkmId = $"gen4_{speciesId}_{otId}_{slot}_{(isParty ? "p" : $"b{boxIndex}")}";

        var summary = new PokemonSummaryDto
        {
            Id = pkmId,
            Species = speciesName,
            SpeciesId = speciesId,
            Nickname = nickname,
            Level = level,
            Gender = genderStr,
            IsShiny = isShiny,
            OriginalTrainer = pkmOtName,
            OriginalTrainerId = otId.ToString(),
            OriginGame = gameId ?? "Pokemon Gen 4",
            CurrentLocation = isParty ? "Party" : $"Box {boxIndex}",
            BoxIndex = boxIndex,
            SlotIndex = slot,
            IsInParty = isParty,
            LegalityStatus = "valid"
        };

        var details = new PokemonDetailsDto
        {
            Summary = summary,
            Nature = nature,
            Ability = abilityId > 0 ? $"Ability #{abilityId}" : null,
            HeldItem = heldItem > 0 ? $"Item #{heldItem}" : null,
            Moves = movesList,
            Iv = ivStats,
            Ev = evStats,
            CurrentHp = currentHp > 0 ? currentHp : null,
            MaxHp = maxHp > 0 ? maxHp : null,
            Friendship = friendship,
            Pokeball = "Poké Ball",
            LegalityStatus = "valid"
        };

        return (summary, details);
    }

    private static int CalculateLevelFromExp(int speciesId, uint exp)
    {
        if (exp == 0) return 5;
        int lvl = (int)Math.Pow(exp, 1.0 / 3.0);
        return Math.Clamp(lvl, 1, 100);
    }

    private static string DecodeUtf16String(byte[] data, int offset, int maxChars)
    {
        var sb = new StringBuilder();
        for (int i = 0; i < maxChars; i++)
        {
            int charOffset = offset + (i * 2);
            if (charOffset + 2 > data.Length) break;
            ushort code = ReadUInt16LE(data, charOffset);
            if (code is 0xFFFF or 0x0000) break;
            sb.Append((char)code);
        }
        return sb.ToString().Trim();
    }
}

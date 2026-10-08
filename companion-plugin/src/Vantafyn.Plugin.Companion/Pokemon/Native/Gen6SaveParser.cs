using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon.Native;

/// <summary>
/// Native high-performance parser for Generation 6 Pokémon save files (X, Y, Omega Ruby, Alpha Sapphire).
/// Completely self-contained; requires zero external services or Docker containers.
/// </summary>
public static class Gen6SaveParser
{
    private const int SaveMinSize = 0x65600; // 415,232 bytes (XY raw save size)
    private const int OrasSaveMinSize = 0x76000; // 483,328 bytes (ORAS raw save size)

    private const int StatusOffset = 0x14000;
    private const int MiscOffset = 0x04200;
    private const int PartyOffset = 0x14200;
    private const int PartyCountOffset = 0x14818;
    private const int ZukanOffset = 0x15000;
    private const int ZukanCaughtOffset = 0x15008;
    private const int ZukanSeenOffset = 0x15068;

    private const int XyBoxOffset = 0x22600;
    private const int OrasBoxOffset = 0x33000;

    private const int BoxCount = 31;
    private const int SlotsPerBox = 30;
    private const int BoxSlotSize = 232;
    private const int PartySlotSize = 260;
    private const int Gen6SpeciesCount = 721;
    private const uint BeefSignature = 0x42454546u;

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

    public static bool IsGen6Save(byte[] saveBytes, string gameId = "")
    {
        if (saveBytes == null || saveBytes.Length < SaveMinSize) return false;
        var lower = (gameId ?? string.Empty).ToLowerInvariant();
        if (IsOrasTitle(lower) || IsXyTitle(lower)) return true;
        return HasBeefSignature(saveBytes);
    }

    private static bool IsOrasTitle(string title) =>
        title.Contains("omega ruby", StringComparison.Ordinal) ||
        title.Contains("alpha sapphire", StringComparison.Ordinal) ||
        title.Contains("oras", StringComparison.Ordinal);

    private static bool IsXyTitle(string title)
    {
        if (title.Contains("pokemon x", StringComparison.Ordinal) ||
            title.Contains("pokémon x", StringComparison.Ordinal) ||
            title.Contains("pokemon y", StringComparison.Ordinal) ||
            title.Contains("pokémon y", StringComparison.Ordinal))
            return true;

        var tokens = title.Split([' ', '_', '-', ':'], StringSplitOptions.RemoveEmptyEntries);
        return Array.Exists(tokens, t => t.Equals("x", StringComparison.OrdinalIgnoreCase) || t.Equals("y", StringComparison.OrdinalIgnoreCase));
    }

    private static bool HasBeefSignature(byte[] saveBytes)
    {
        if (saveBytes.Length < 0x200) return false;
        if (saveBytes.Length >= 0x1F0 && ReadUInt32LE(saveBytes, saveBytes.Length - 0x1F0) == BeefSignature)
            return true;

        int start = Math.Max(0, saveBytes.Length - 0x200);
        for (int i = start; i <= saveBytes.Length - 4; i += 4)
        {
            if (ReadUInt32LE(saveBytes, i) == BeefSignature) return true;
        }
        return false;
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

    public static PokemonSaveParseResult Parse(byte[] saveBytes, string gameId, PkVaultStaticCatalog catalog)
    {
        var result = new PokemonSaveParseResult();
        if (saveBytes == null || saveBytes.Length < SaveMinSize)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save file size is invalid for Gen 6 (must be >= 415KB).";
            return result;
        }

        bool isOras = IsOrasTitle(gameId) || saveBytes.Length >= OrasSaveMinSize;
        int boxOffset = isOras ? OrasBoxOffset : XyBoxOffset;

        // Trainer Info
        ushort tid = StatusOffset + 2 <= saveBytes.Length ? ReadUInt16LE(saveBytes, StatusOffset) : (ushort)0;
        ushort sid = StatusOffset + 4 <= saveBytes.Length ? ReadUInt16LE(saveBytes, StatusOffset + 2) : (ushort)0;
        string otName = StatusOffset + 0x48 + 24 <= saveBytes.Length
            ? DecodeUtf16String(saveBytes, StatusOffset + 0x48, 12)
            : "TRAINER";
        if (string.IsNullOrWhiteSpace(otName)) otName = "TRAINER";

        // Misc: Money & Badges
        uint money = MiscOffset + 12 <= saveBytes.Length ? ReadUInt32LE(saveBytes, MiscOffset + 8) : 0;
        if (money > 9999999) money = 9999999;
        byte badgeFlags = MiscOffset + 13 <= saveBytes.Length ? saveBytes[MiscOffset + 0x0C] : (byte)0;

        result.TrainerName = otName;
        result.TrainerId = tid.ToString();
        result.Money = (int)money;
        result.DetectedGeneration = 6;
        result.GymBadges = PokemonGymBadgeCatalog.ForGen6(gameId, badgeFlags);

        // Pokédex Flags
        var caughtIds = new List<int>();
        var seenIds = new SortedSet<int>();

        if (ZukanOffset + 0x200 <= saveBytes.Length)
        {
            for (int speciesId = 1; speciesId <= Gen6SpeciesCount; speciesId++)
            {
                int bitIndex = speciesId - 1;
                int byteOffset = ZukanCaughtOffset + (bitIndex / 8);
                if (byteOffset < saveBytes.Length)
                {
                    byte b = saveBytes[byteOffset];
                    if ((b & (1 << (bitIndex % 8))) != 0)
                    {
                        caughtIds.Add(speciesId);
                        seenIds.Add(speciesId);
                    }
                }

                for (int r = 0; r < 4; r++)
                {
                    int seenOffset = ZukanSeenOffset + (r * 0x60) + (bitIndex / 8);
                    if (seenOffset < saveBytes.Length)
                    {
                        byte b = saveBytes[seenOffset];
                        if ((b & (1 << (bitIndex % 8))) != 0)
                        {
                            seenIds.Add(speciesId);
                            break;
                        }
                    }
                }
            }
        }

        // 1. Party
        int partyCount = 0;
        if (PartyCountOffset + 4 <= saveBytes.Length)
        {
            partyCount = Math.Clamp((int)saveBytes[PartyCountOffset], 0, 6);
        }

        for (int slot = 0; slot < partyCount; slot++)
        {
            int pkOffset = PartyOffset + (slot * PartySlotSize);
            if (pkOffset + PartySlotSize <= saveBytes.Length)
            {
                var pkm = ParsePokemon(saveBytes, pkOffset, PartySlotSize, isParty: true, slot + 1, null, gameId, catalog);
                if (pkm != null)
                {
                    result.Party.Add(pkm.Value.Summary);
                    result.Details[pkm.Value.Summary.Id] = pkm.Value.Details;
                }
            }
        }

        // 2. Boxes (31 boxes * 30 slots)
        for (int b = 0; b < BoxCount; b++)
        {
            var entries = new List<PokemonSummaryDto>();
            var box = new PokemonBoxDto
            {
                BoxIndex = b + 1,
                Name = $"Box {b + 1}",
                Capacity = SlotsPerBox,
                OccupiedCount = 0,
                Entries = entries
            };

            int boxBase = boxOffset + (b * SlotsPerBox * BoxSlotSize);

            for (int s = 0; s < SlotsPerBox; s++)
            {
                int pkOffset = boxBase + (s * BoxSlotSize);
                if (pkOffset + BoxSlotSize <= saveBytes.Length)
                {
                    var pkm = ParsePokemon(saveBytes, pkOffset, BoxSlotSize, isParty: false, s + 1, b + 1, gameId, catalog);
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

        // Populate caught and seen from party/boxes
        foreach (var p in result.Party)
        {
            if (p.SpeciesId is > 0 and <= Gen6SpeciesCount)
            {
                if (!caughtIds.Contains(p.SpeciesId)) caughtIds.Add(p.SpeciesId);
                seenIds.Add(p.SpeciesId);
            }
        }
        foreach (var box in result.Boxes)
        {
            foreach (var p in box.Entries)
            {
                if (p.SpeciesId is > 0 and <= Gen6SpeciesCount)
                {
                    if (!caughtIds.Contains(p.SpeciesId)) caughtIds.Add(p.SpeciesId);
                    seenIds.Add(p.SpeciesId);
                }
            }
        }

        result.CaughtSpeciesIds = caughtIds;
        result.SeenSpeciesIds = [.. seenIds];
        result.PokedexCaught = caughtIds.Count;
        result.PokedexSeen = seenIds.Count;

        bool isUninitialized = partyCount == 0 &&
                               result.Boxes.TrueForAll(b => b.OccupiedCount == 0) &&
                               caughtIds.Count == 0 &&
                               badgeFlags == 0 &&
                               money == 0 &&
                               tid == 0;

        if (isUninitialized)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save file is uninitialized (no in-game save exists).";
            return result;
        }

        result.IsSuccess = true;
        return result;
    }

    private static (PokemonSummaryDto Summary, PokemonDetailsDto Details)? ParsePokemon(
        byte[] buffer,
        int offset,
        int length,
        bool isParty,
        int slot,
        int? boxIndex,
        string originGame,
        PkVaultStaticCatalog catalog)
    {
        if (offset + length > buffer.Length || length < BoxSlotSize) return null;

        uint pv = ReadUInt32LE(buffer, offset);
        ushort chk = ReadUInt16LE(buffer, offset + 6);
        if (pv == 0 && chk == 0) return null;

        byte[] d = new byte[length];
        Array.Copy(buffer, offset, d, 0, length);

        // 1. Decrypt 224 bytes using LCRNG seeded with pv
        uint seed = pv;
        for (int i = 8; i < BoxSlotSize; i += 2)
        {
            seed = (0x41C64E6Du * seed + 0x6073u);
            ushort xor = (ushort)(seed >> 16);
            ushort w = (ushort)(d[i] | (d[i + 1] << 8));
            w ^= xor;
            d[i] = (byte)(w & 0xFF);
            d[i + 1] = (byte)(w >> 8);
        }

        // 2. Decrypt party battle stats if party
        if (isParty && length >= PartySlotSize)
        {
            uint partySeed = pv;
            for (int i = BoxSlotSize; i < PartySlotSize; i += 2)
            {
                partySeed = (0x41C64E6Du * partySeed + 0x6073u);
                ushort xor = (ushort)(partySeed >> 16);
                ushort w = (ushort)(d[i] | (d[i + 1] << 8));
                w ^= xor;
                d[i] = (byte)(w & 0xFF);
                d[i + 1] = (byte)(w >> 8);
            }
        }

        // 3. Unshuffle blocks A, B, C, D (56 bytes each)
        int shiftVal = (int)(((pv >> 13) & 31) % 24);
        var order = BlockOrders[shiftVal];

        byte[] unshuffled = new byte[length];
        Array.Copy(d, 0, unshuffled, 0, 8);
        for (int pos = 0; pos < 4; pos++)
        {
            int blk = order[pos];
            Array.Copy(d, 8 + pos * 56, unshuffled, 8 + blk * 56, 56);
        }
        if (length > BoxSlotSize)
        {
            Array.Copy(d, BoxSlotSize, unshuffled, BoxSlotSize, length - BoxSlotSize);
        }

        // Block A: Species ID at 8
        ushort speciesId = ReadUInt16LE(unshuffled, 8);
        if (speciesId is <= 0 or > Gen6SpeciesCount) return null;

        ushort heldItem = ReadUInt16LE(unshuffled, 10);
        ushort otId = ReadUInt16LE(unshuffled, 12);
        ushort secretId = ReadUInt16LE(unshuffled, 14);
        uint exp = ReadUInt32LE(unshuffled, 16);
        int abilityId = unshuffled[20];
        uint pid = ReadUInt32LE(unshuffled, 24);
        int natureIndex = Math.Clamp((int)unshuffled[28], 0, 24);
        string natureName = NatureNames[natureIndex];

        byte encounterByte = unshuffled[29];
        int genderBits = (encounterByte >> 1) & 0x03;
        string genderStr = genderBits switch
        {
            0 => "M",
            1 => "F",
            _ => "Genderless"
        };
        int formBits = (encounterByte >> 3) & 0x1F;

        // EVs
        int hpEv = unshuffled[30];
        int atkEv = unshuffled[31];
        int defEv = unshuffled[32];
        int speEv = unshuffled[33];
        int spaEv = unshuffled[34];
        int spdEv = unshuffled[35];

        var evStats = new PokemonStatsDto
        {
            Hp = Math.Clamp(hpEv, 0, 252),
            Attack = Math.Clamp(atkEv, 0, 252),
            Defense = Math.Clamp(defEv, 0, 252),
            Speed = Math.Clamp(speEv, 0, 252),
            SpecialAttack = Math.Clamp(spaEv, 0, 252),
            SpecialDefense = Math.Clamp(spdEv, 0, 252)
        };

        // Block B: Nickname at 64, moves at 90, IVs at 112
        string rawNick = DecodeUtf16String(unshuffled, 64, 12);
        ushort m1 = ReadUInt16LE(unshuffled, 90);
        ushort m2 = ReadUInt16LE(unshuffled, 92);
        ushort m3 = ReadUInt16LE(unshuffled, 94);
        ushort m4 = ReadUInt16LE(unshuffled, 96);
        var movesList = new List<string>();
        foreach (var mid in new[] { m1, m2, m3, m4 })
        {
            if (mid > 0)
            {
                movesList.Add(PokemonMoveCatalog.ResolveMoveName(mid));
            }
        }

        uint iv32 = ReadUInt32LE(unshuffled, 112);
        int hpIv = (int)(iv32 & 0x1F);
        int atkIv = (int)((iv32 >> 5) & 0x1F);
        int defIv = (int)((iv32 >> 10) & 0x1F);
        int speIv = (int)((iv32 >> 15) & 0x1F);
        int spaIv = (int)((iv32 >> 20) & 0x1F);
        int spdIv = (int)((iv32 >> 25) & 0x1F);
        bool isEgg = ((iv32 >> 30) & 1) == 1;

        var ivStats = new PokemonStatsDto
        {
            Hp = Math.Clamp(hpIv, 0, 31),
            Attack = Math.Clamp(atkIv, 0, 31),
            Defense = Math.Clamp(defIv, 0, 31),
            Speed = Math.Clamp(speIv, 0, 31),
            SpecialAttack = Math.Clamp(spaIv, 0, 31),
            SpecialDefense = Math.Clamp(spdIv, 0, 31)
        };

        // Block D: OT Name at 176
        string otName = DecodeUtf16String(unshuffled, 176, 12);
        if (string.IsNullOrWhiteSpace(otName)) otName = "TRAINER";
        int friendship = unshuffled[200];

        string speciesName = PokemonSpeciesCatalog.ResolveSpeciesName(speciesId);
        string nickname = isEgg ? "Egg" : (string.IsNullOrWhiteSpace(rawNick) ? speciesName : rawNick);

        int level;
        int currentHp = 0;
        int maxHp = 0;
        if (isParty && length >= PartySlotSize)
        {
            level = Math.Clamp((int)unshuffled[236], 1, 100);
            currentHp = ReadUInt16LE(unshuffled, 238);
            maxHp = ReadUInt16LE(unshuffled, 240);
        }
        else
        {
            level = CalculateLevelFromExp(exp);
        }

        uint shinyVal = (uint)((otId ^ secretId) ^ ((pid & 0xFFFF) ^ (pid >> 16)));
        bool isShiny = shinyVal < 16;

        string? formStr = speciesId == 201
            ? formBits switch
            {
                <= 25 => ((char)('A' + formBits)).ToString(),
                26 => "!",
                27 => "?",
                _ => null
            }
            : (formBits > 0 ? $"Form {formBits}" : null);

        string pkmId = $"gen6_{speciesId}_{otId}_{slot}_{(isParty ? "p" : $"b{boxIndex}")}";

        var summary = new PokemonSummaryDto
        {
            Id = pkmId,
            Species = speciesName,
            SpeciesId = speciesId,
            Form = formStr,
            Nickname = nickname,
            Level = level,
            Gender = genderStr,
            IsShiny = isShiny,
            OriginalTrainer = otName,
            OriginalTrainerId = otId.ToString(),
            OriginGame = originGame,
            CurrentLocation = isParty ? "Party" : $"Box {boxIndex}",
            BoxIndex = boxIndex,
            SlotIndex = slot,
            IsInParty = isParty,
            LegalityStatus = "valid"
        };

        var details = new PokemonDetailsDto
        {
            Summary = summary,
            Nature = natureName,
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

    private static string DecodeUtf16String(byte[] data, int offset, int maxChars)
    {
        var sb = new StringBuilder();
        for (int i = 0; i < maxChars; i++)
        {
            int charOffset = offset + (i * 2);
            if (charOffset + 2 > data.Length) break;
            ushort code = (ushort)(data[charOffset] | (data[charOffset + 1] << 8));
            if (code is 0 or 0xFFFF) break;
            sb.Append((char)code);
        }
        return sb.ToString().Trim();
    }

    private static int CalculateLevelFromExp(uint exp)
    {
        for (int lvl = 100; lvl >= 1; lvl--)
        {
            long req = (long)lvl * lvl * lvl;
            if (exp >= req) return lvl;
        }
        return 1;
    }
}

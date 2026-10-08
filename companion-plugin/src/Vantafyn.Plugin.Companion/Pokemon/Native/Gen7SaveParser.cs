using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon.Native;

/// <summary>
/// Native high-performance parser for Generation 7 Pokémon save files (Sun, Moon, Ultra Sun, Ultra Moon).
/// Completely self-contained; requires zero external services or Docker containers.
/// </summary>
public static class Gen7SaveParser
{
    private const int SaveMinSize = 0x6BE00; // 441,856 bytes (SM raw save size)
    private const int UsumSaveMinSize = 0x6CC00; // 445,440 bytes (USUM raw save size)

    // Sun & Moon offsets
    private const int SmZCrystalOffset = 0x00D68;
    private const int SmStatusOffset = 0x01200;
    private const int SmPartyOffset = 0x01400;
    private const int SmPartyCountOffset = 0x01A18;
    private const int SmZukanOffset = 0x02A00;
    private const int SmZukanCaughtOffset = 0x02A88;
    private const int SmZukanSeenOffset = 0x02AF0;
    private const int SmMiscOffset = 0x04000;
    private const int SmBoxOffset = 0x04E00;

    // Ultra Sun & Ultra Moon offsets
    private const int UsumZCrystalOffset = 0x00D70;
    private const int UsumStatusOffset = 0x01400;
    private const int UsumPartyOffset = 0x01600;
    private const int UsumPartyCountOffset = 0x01C18;
    private const int UsumZukanOffset = 0x02C00;
    private const int UsumZukanCaughtOffset = 0x02C88;
    private const int UsumZukanSeenOffset = 0x02CF0;
    private const int UsumMiscOffset = 0x04400;
    private const int UsumBoxOffset = 0x05200;

    private const int BoxCount = 32;
    private const int SlotsPerBox = 30;
    private const int BoxSlotSize = 232;
    private const int PartySlotSize = 260;
    private const int Gen7SpeciesCount = 807;
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

    public static bool IsGen7Save(byte[] saveBytes, string gameId = "")
    {
        if (saveBytes == null || saveBytes.Length < SaveMinSize) return false;
        var lower = (gameId ?? string.Empty).ToLowerInvariant();
        if (IsGen7Title(lower)) return true;
        return HasBeefSignature(saveBytes);
    }

    private static bool IsGen7Title(string title)
    {
        if (title.Contains("ultra sun", StringComparison.Ordinal) ||
            title.Contains("ultra moon", StringComparison.Ordinal) ||
            title.Contains("usum", StringComparison.Ordinal))
            return true;

        if (title.Contains("pokemon sun", StringComparison.Ordinal) ||
            title.Contains("pokémon sun", StringComparison.Ordinal) ||
            title.Contains("pokemon moon", StringComparison.Ordinal) ||
            title.Contains("pokémon moon", StringComparison.Ordinal))
            return true;

        var tokens = title.Split([' ', '_', '-', ':'], StringSplitOptions.RemoveEmptyEntries);
        return Array.Exists(tokens, t => t.Equals("sun", StringComparison.OrdinalIgnoreCase) || t.Equals("moon", StringComparison.OrdinalIgnoreCase));
    }

    private static bool IsUsumTitle(string title) =>
        title.Contains("ultra sun", StringComparison.OrdinalIgnoreCase) ||
        title.Contains("ultra moon", StringComparison.OrdinalIgnoreCase) ||
        title.Contains("usum", StringComparison.OrdinalIgnoreCase);

    private static bool IsSmTitle(string title) =>
        (title.Contains("sun", StringComparison.OrdinalIgnoreCase) ||
         title.Contains("moon", StringComparison.OrdinalIgnoreCase)) &&
        !IsUsumTitle(title);

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

    public static string? ResolveZCrystalSlug(int id) => id switch
    {
        1831 or 776 or 1 => "normalium-z",
        1832 or 777 or 2 => "firium-z",
        1833 or 778 or 3 => "waterium-z",
        1834 or 779 or 4 => "electrium-z",
        1835 or 780 or 5 => "grassium-z",
        1836 or 781 or 6 => "icium-z",
        1837 or 782 or 7 => "fightinium-z",
        1838 or 783 or 8 => "poisonium-z",
        1839 or 784 or 9 => "groundium-z",
        1840 or 785 or 10 => "flyinium-z",
        1841 or 786 or 11 => "psychium-z",
        1842 or 787 or 12 => "buginium-z",
        1843 or 788 or 13 => "rockium-z",
        1844 or 789 or 14 => "ghostium-z",
        1845 or 790 or 15 => "dragonium-z",
        1846 or 791 or 16 => "darkinium-z",
        1847 or 792 or 17 => "steelium-z",
        1848 or 793 or 18 => "fairium-z",
        1849 or 794 or 19 => "pikanium-z",
        1850 or 795 or 20 => "decidium-z",
        1851 or 796 or 21 => "incinium-z",
        1852 or 797 or 22 => "primarium-z",
        1853 or 798 or 23 => "tapunium-z",
        1854 or 799 or 24 => "marshadium-z",
        1855 or 800 or 25 => "aloraichium-z",
        1856 or 801 or 26 => "snorlium-z",
        1857 or 802 or 27 => "eevium-z",
        1858 or 803 or 28 => "mewnium-z",
        1859 or 804 or 29 => "pikashunium-z",
        1951 or 921 or 30 => "solganium-z",
        1952 or 922 or 31 => "lunalium-z",
        1953 or 923 or 32 => "ultranecrozium-z",
        1954 or 924 or 33 => "mimikium-z",
        1955 or 925 or 34 => "lycanium-z",
        1956 or 926 or 35 => "kommonium-z",
        _ => null
    };

    public static PokemonSaveParseResult Parse(byte[] saveBytes, string gameId, PkVaultStaticCatalog catalog)
    {
        var result = new PokemonSaveParseResult();
        if (saveBytes == null || saveBytes.Length < SaveMinSize)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save file size is invalid for Gen 7 (must be >= 441KB).";
            return result;
        }

        bool isUsum = IsUsumTitle(gameId) ? true : (IsSmTitle(gameId) ? false : saveBytes.Length == UsumSaveMinSize);

        int statusOffset = isUsum ? UsumStatusOffset : SmStatusOffset;
        int partyOffset = isUsum ? UsumPartyOffset : SmPartyOffset;
        int partyCountOffset = isUsum ? UsumPartyCountOffset : SmPartyCountOffset;
        int zukanOffset = isUsum ? UsumZukanOffset : SmZukanOffset;
        int zukanCaughtOffset = isUsum ? UsumZukanCaughtOffset : SmZukanCaughtOffset;
        int zukanSeenOffset = isUsum ? UsumZukanSeenOffset : SmZukanSeenOffset;
        int miscOffset = isUsum ? UsumMiscOffset : SmMiscOffset;
        int boxOffset = isUsum ? UsumBoxOffset : SmBoxOffset;

        // Trainer Info
        ushort tid = statusOffset + 2 <= saveBytes.Length ? ReadUInt16LE(saveBytes, statusOffset) : (ushort)0;
        ushort sid = statusOffset + 4 <= saveBytes.Length ? ReadUInt16LE(saveBytes, statusOffset + 2) : (ushort)0;
        string otName = statusOffset + 0x38 + 24 <= saveBytes.Length
            ? DecodeUtf16String(saveBytes, statusOffset + 0x38, 12)
            : "TRAINER";
        if (string.IsNullOrWhiteSpace(otName)) otName = "TRAINER";

        // Misc: Money & Stamps
        uint money = miscOffset + 8 <= saveBytes.Length ? ReadUInt32LE(saveBytes, miscOffset + 4) : 0;
        if (money > 9999999) money = 9999999;
        ushort rawStamps = miscOffset + 10 <= saveBytes.Length ? ReadUInt16LE(saveBytes, miscOffset + 8) : (ushort)0;
        ushort stampFlags = (ushort)((rawStamps >> 4) & 0x7FFF);

        // Z-Crystals Pocket
        int zCrystalOffset = isUsum ? UsumZCrystalOffset : SmZCrystalOffset;
        int zCrystalSlotCount = isUsum ? 35 : 29;
        var unlockedZCrystals = new HashSet<string>(StringComparer.Ordinal);

        if (zCrystalOffset + (zCrystalSlotCount * 4) <= saveBytes.Length)
        {
            for (int slot = 0; slot < zCrystalSlotCount; slot++)
            {
                int offset = zCrystalOffset + (slot * 4);
                ushort itemId = ReadUInt16LE(saveBytes, offset);
                if (itemId > 0)
                {
                    var slug = ResolveZCrystalSlug(itemId) ?? ResolveZCrystalSlug(slot + 1);
                    if (slug != null)
                    {
                        unlockedZCrystals.Add(slug);
                    }
                }
            }
        }

        result.TrainerName = otName;
        result.TrainerId = tid.ToString();
        result.Money = (int)money;
        result.DetectedGeneration = 7;

        // Pokédex Flags
        var caughtIds = new List<int>();
        var seenIds = new SortedSet<int>();

        if (zukanOffset + 0x300 <= saveBytes.Length)
        {
            for (int speciesId = 1; speciesId <= Gen7SpeciesCount; speciesId++)
            {
                int bitIndex = speciesId - 1;
                int byteOffset = zukanCaughtOffset + (bitIndex / 8);
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
                    int seenOffset = zukanSeenOffset + (r * 0x8C) + (bitIndex / 8);
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
        if (partyCountOffset + 4 <= saveBytes.Length)
        {
            partyCount = Math.Clamp((int)saveBytes[partyCountOffset], 0, 6);
        }

        for (int slot = 0; slot < partyCount; slot++)
        {
            int pkOffset = partyOffset + (slot * PartySlotSize);
            if (pkOffset + PartySlotSize <= saveBytes.Length)
            {
                var pkm = ParsePokemon(saveBytes, pkOffset, PartySlotSize, isParty: true, slot + 1, null, gameId, catalog);
                if (pkm != null)
                {
                    result.Party.Add(pkm.Value.Summary);
                    result.Details[pkm.Value.Summary.Id] = pkm.Value.Details;
                    if (pkm.Value.HeldItemId > 0)
                    {
                        var slug = ResolveZCrystalSlug(pkm.Value.HeldItemId);
                        if (slug != null) unlockedZCrystals.Add(slug);
                    }
                }
            }
        }

        // 2. Boxes (32 boxes * 30 slots)
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
                        if (pkm.Value.HeldItemId > 0)
                        {
                            var slug = ResolveZCrystalSlug(pkm.Value.HeldItemId);
                            if (slug != null) unlockedZCrystals.Add(slug);
                        }
                    }
                }
            }

            box.OccupiedCount = entries.Count;
            result.Boxes.Add(box);
        }

        // Populate caught and seen from party/boxes
        foreach (var p in result.Party)
        {
            if (p.SpeciesId is > 0 and <= Gen7SpeciesCount)
            {
                if (!caughtIds.Contains(p.SpeciesId)) caughtIds.Add(p.SpeciesId);
                seenIds.Add(p.SpeciesId);
            }
        }
        foreach (var box in result.Boxes)
        {
            foreach (var p in box.Entries)
            {
                if (p.SpeciesId is > 0 and <= Gen7SpeciesCount)
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
        result.GymBadges = PokemonGymBadgeCatalog.ForGen7(gameId, stampFlags, unlockedZCrystals, isUsum);

        bool isUninitialized = partyCount == 0 &&
                               result.Boxes.TrueForAll(b => b.OccupiedCount == 0) &&
                               caughtIds.Count == 0 &&
                               stampFlags == 0 &&
                               unlockedZCrystals.Count == 0 &&
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

    private static (PokemonSummaryDto Summary, PokemonDetailsDto Details, int HeldItemId)? ParsePokemon(
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
        if (speciesId is <= 0 or > Gen7SpeciesCount) return null;

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

        string pkmId = $"gen7_{speciesId}_{otId}_{slot}_{(isParty ? "p" : $"b{boxIndex}")}";

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

        return (summary, details, (int)heldItem);
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

    public static bool IsEventUnlocked(byte[] saveBytes, string gameId, string eventId)
    {
        if (saveBytes == null || saveBytes.Length < SaveMinSize) return false;

        if (eventId == PokemonEventCatalog.Gen7MagearnaDelivery)
        {
            return HasSpeciesInSave(saveBytes, gameId, 801);
        }

        if (eventId == PokemonEventCatalog.Gen7AshGreninjaDelivery)
        {
            return HasSpeciesInSave(saveBytes, gameId, 658);
        }

        return false;
    }

    private static bool HasSpeciesInSave(byte[] saveBytes, string gameId, int targetSpeciesId)
    {
        try
        {
            bool isUsum = IsUsumTitle(gameId) || saveBytes.Length >= UsumSaveMinSize;
            int partyOffset = isUsum ? UsumPartyOffset : SmPartyOffset;
            int partyCountOffset = isUsum ? UsumPartyCountOffset : SmPartyCountOffset;
            int boxOffset = isUsum ? UsumBoxOffset : SmBoxOffset;

            int partyCount = partyCountOffset < saveBytes.Length ? Math.Clamp((int)saveBytes[partyCountOffset], 0, 6) : 0;
            for (int i = 0; i < partyCount; i++)
            {
                int slotOffset = partyOffset + (i * PartySlotSize);
                if (slotOffset + PartySlotSize <= saveBytes.Length)
                {
                    var pkm = ParsePokemon(saveBytes, slotOffset, PartySlotSize, true, i + 1, null, gameId, null!);
                    if (pkm?.Summary?.SpeciesId == targetSpeciesId) return true;
                }
            }

            for (int box = 0; box < BoxCount; box++)
            {
                for (int slot = 0; slot < SlotsPerBox; slot++)
                {
                    int slotOffset = boxOffset + (box * SlotsPerBox * BoxSlotSize) + (slot * BoxSlotSize);
                    if (slotOffset + BoxSlotSize <= saveBytes.Length)
                    {
                        var pkm = ParsePokemon(saveBytes, slotOffset, BoxSlotSize, false, slot + 1, box + 1, gameId, null!);
                        if (pkm?.Summary?.SpeciesId == targetSpeciesId) return true;
                    }
                }
            }
        }
        catch
        {
            // Non-fatal parse check
        }

        return false;
    }

    public static byte[] UnlockEvent(byte[] saveBytes, string gameId, string eventId)
    {
        if (saveBytes == null || saveBytes.Length < SaveMinSize)
        {
            throw new System.IO.InvalidDataException("Save file size is invalid for Gen 7 (must be >= 441KB).");
        }

        // Gen 7 event delivery is fulfilled through the Personal Vault and tracked via redeemed codes
        return (byte[])saveBytes.Clone();
    }
}

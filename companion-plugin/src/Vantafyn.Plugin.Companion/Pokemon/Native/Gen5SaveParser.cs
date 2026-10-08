using System;
using System.Collections.Generic;
using System.Text;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon.Native;

/// <summary>
/// Native high-performance parser for Generation 5 Pokémon save files (Black, White, Black 2, White 2).
/// Completely self-contained; requires zero external services or Docker containers.
/// </summary>
public static class Gen5SaveParser
{
    private const int SaveCopySize = 0x24000;
    private const int PartyOffset = 0x18E00;
    private const int BoxOffset = 0x400;
    private const int BoxCount = 24;
    private const int SlotsPerBox = 30;
    private const int PartySlotSize = 220;
    private const int PokedexFlagsOffsetInBlock = 0x08;
    private const int PokedexFlagRegionSize = 0x54;
    private const int Gen5SpeciesCount = 649;

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

    public static bool IsGen5Save(byte[] saveBytes, string gameId = "")
    {
        if (saveBytes == null || saveBytes.Length < 0x80000) return false;
        var lower = (gameId ?? string.Empty).ToLowerInvariant();
        return lower.Contains("black") || lower.Contains("white") || lower.Contains("b2w2") ||
               lower.Contains("black 2") || lower.Contains("white 2");
    }

    public static ushort CalculateCrc16(byte[] data, int offset, int length)
    {
        ushort crc = 0xFFFF;
        for (int i = 0; i < length; i++)
        {
            crc = (ushort)(crc ^ (data[offset + i] << 8));
            for (int b = 0; b < 8; b++)
            {
                if ((crc & 0x8000) != 0)
                    crc = (ushort)((crc << 1) ^ 0x1021);
                else
                    crc = (ushort)(crc << 1);
            }
        }
        return crc;
    }

    private static (int blockOffset, int blockDataLength, int checksumOffset, int keyPocketOffset, int slotCount, int copySize) GetGen5BagInfo(string gameId)
    {
        var lower = (gameId ?? string.Empty).ToLowerInvariant();
        bool isB2W2 = lower.Contains("black 2") || lower.Contains("white 2") || lower.Contains("black2") || lower.Contains("white2") || lower.Contains("b2w2");

        if (isB2W2)
        {
            // B2W2: Block 25 offset 0x18400, data length 0x09EC, checksum at 0x18DEE, Key items at 0x18A40 (104 slots), copy size 0x26000
            return (0x18400, 0x09EC, 0x18DEE, 0x18A40, 104, 0x26000);
        }

        // BW1: Block 25 offset 0x18400, data length 0x09C0, checksum at 0x18DC2, Key items at 0x188D8 (83 slots), copy size 0x24000
        return (0x18400, 0x09C0, 0x18DC2, 0x188D8, 83, 0x24000);
    }

    private static ushort? ResolveGen5EventItemId(string eventId) => eventId switch
    {
        PokemonEventCatalog.BwLibertyPass => 587,
        _ => null
    };

    public static bool IsEventUnlocked(byte[] saveBytes, string gameId, string eventId)
    {
        if (saveBytes == null || saveBytes.Length < 0x24000) return false;
        var targetItem = ResolveGen5EventItemId(eventId);
        if (targetItem == null) return false;

        var (_, _, _, keyPocketOffset, slotCount, copySize) = GetGen5BagInfo(gameId);

        // Check primary copy
        if (HasKeyItem(saveBytes, keyPocketOffset, slotCount, targetItem.Value))
            return true;

        // Check secondary copy if present
        if (saveBytes.Length >= copySize * 2)
        {
            if (HasKeyItem(saveBytes, copySize + keyPocketOffset, slotCount, targetItem.Value))
                return true;
        }

        return false;
    }

    public static byte[] UnlockEvent(byte[] saveBytes, string gameId, string eventId)
    {
        if (saveBytes == null || saveBytes.Length < 0x24000)
        {
            throw new InvalidDataException("Save file size is invalid for Gen 5 (must be >= 512KB).");
        }

        var targetItem = ResolveGen5EventItemId(eventId);
        if (targetItem == null)
        {
            throw new InvalidDataException($"Unsupported Gen 5 event '{eventId}'.");
        }

        var (blockOffset, blockDataLength, checksumOffset, keyPocketOffset, slotCount, copySize) = GetGen5BagInfo(gameId);
        var copy = (byte[])saveBytes.Clone();

        // Patch primary copy
        PatchBagKeyItem(copy, 0, blockOffset, blockDataLength, checksumOffset, keyPocketOffset, slotCount, targetItem.Value);

        // Patch backup copy if available
        if (copy.Length >= copySize * 2)
        {
            if (!IsErased(copy, copySize + blockOffset, blockDataLength))
            {
                PatchBagKeyItem(copy, copySize, blockOffset, blockDataLength, checksumOffset, keyPocketOffset, slotCount, targetItem.Value);
            }
        }

        return copy;
    }

    private static void PatchBagKeyItem(
        byte[] data,
        int baseOffset,
        int blockOffset,
        int blockDataLength,
        int checksumOffset,
        int keyPocketOffset,
        int slotCount,
        ushort itemId)
    {
        int actualPocketOffset = baseOffset + keyPocketOffset;
        int emptySlot = -1;

        for (int i = 0; i < slotCount; i++)
        {
            int slotOffset = actualPocketOffset + (i * 4);
            if (slotOffset + 4 > data.Length) break;
            ushort existing = ReadUInt16LE(data, slotOffset);
            if (existing == itemId)
            {
                // Already present, ensure quantity is at least 1
                WriteUInt16LE(data, slotOffset + 2, 1);
                RecalculateBagBlockCrc(data, baseOffset, blockOffset, blockDataLength, checksumOffset);
                return;
            }
            if (emptySlot < 0 && existing == 0)
            {
                emptySlot = slotOffset;
            }
        }

        if (emptySlot < 0)
        {
            throw new InvalidDataException("The Key Items pocket is full. Free one slot before unlocking this event.");
        }

        WriteUInt16LE(data, emptySlot, itemId);
        WriteUInt16LE(data, emptySlot + 2, 1);
        RecalculateBagBlockCrc(data, baseOffset, blockOffset, blockDataLength, checksumOffset);
    }

    private static void RecalculateBagBlockCrc(
        byte[] data,
        int baseOffset,
        int blockOffset,
        int blockDataLength,
        int checksumOffset)
    {
        int actualBlockOffset = baseOffset + blockOffset;
        int actualChecksumOffset = baseOffset + checksumOffset;
        ushort crc = CalculateCrc16(data, actualBlockOffset, blockDataLength);
        WriteUInt16LE(data, actualChecksumOffset, crc);
    }

    private static bool HasKeyItem(byte[] data, int pocketOffset, int slotCount, ushort itemId)
    {
        for (int i = 0; i < slotCount; i++)
        {
            int slotOffset = pocketOffset + (i * 4);
            if (slotOffset + 2 > data.Length) break;
            ushort existingId = ReadUInt16LE(data, slotOffset);
            if (existingId == itemId)
            {
                ushort qty = ReadUInt16LE(data, slotOffset + 2);
                if (qty > 0) return true;
            }
        }
        return false;
    }

    private static void WriteUInt16LE(byte[] data, int offset, ushort value)
    {
        if (offset + 2 <= data.Length)
        {
            data[offset] = (byte)(value & 0xFF);
            data[offset + 1] = (byte)((value >> 8) & 0xFF);
        }
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
        if (saveBytes == null || saveBytes.Length < 0x80000)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save file size is invalid for Gen 5 (must be >= 512KB).";
            return result;
        }

        var pokedex = ReadPokedexFlags(saveBytes, gameId);

        // 1. Party
        int partyCount = 0;
        if (PartyOffset + 4 < saveBytes.Length)
        {
            partyCount = Math.Clamp((int)saveBytes[PartyOffset + 4], 0, 6);
        }

        for (int slot = 0; slot < partyCount; slot++)
        {
            int pkOffset = PartyOffset + 8 + (slot * PartySlotSize);
            if (pkOffset + PartySlotSize <= saveBytes.Length)
            {
                var pkm = ParsePokemon(saveBytes, pkOffset, PartySlotSize, isParty: true, slot + 1, boxIndex: null, gameId, catalog);
                if (pkm != null)
                {
                    result.Party.Add(pkm.Value.Summary);
                    result.Details[pkm.Value.Summary.Id] = pkm.Value.Details;
                }
            }
        }

        // 2. Boxes (24 boxes)
        for (int b = 0; b < BoxCount; b++)
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

            int boxBase = BoxOffset + (b * SlotsPerBox * 136) + (b * 0x10);
            for (int s = 0; s < SlotsPerBox; s++)
            {
                int pkOffset = boxBase + (s * 136);
                if (pkOffset + 136 <= saveBytes.Length)
                {
                    var pkm = ParsePokemon(saveBytes, pkOffset, 136, isParty: false, s + 1, boxIndex: b + 1, gameId, catalog);
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

        result.DetectedGeneration = 5;
        result.PokedexCaught = pokedex?.CaughtSpeciesIds.Count;
        result.PokedexSeen = pokedex?.SeenSpeciesIds.Count;
        result.CaughtSpeciesIds = pokedex?.CaughtSpeciesIds ?? [];
        result.SeenSpeciesIds = pokedex?.SeenSpeciesIds ?? [];

        bool isUninitialized = partyCount == 0 &&
                               result.Boxes.All(b => b.OccupiedCount == 0) &&
                               (pokedex == null || pokedex.Value.CaughtSpeciesIds.Count == 0);
        if (isUninitialized)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save file is uninitialized (no in-game save exists).";
            return result;
        }

        result.IsSuccess = true;
        return result;
    }

    private static (List<int> CaughtSpeciesIds, List<int> SeenSpeciesIds)? ReadPokedexFlags(byte[] saveBytes, string gameId)
    {
        var (pokedexOffset, pokedexSize) = GetPokedexBlock(gameId);
        var primary = ReadPokedexFlagsAt(saveBytes, 0, pokedexOffset, pokedexSize);
        return primary ?? ReadPokedexFlagsAt(saveBytes, SaveCopySize, pokedexOffset, pokedexSize);
    }

    private static (int PokedexOffset, int PokedexSize) GetPokedexBlock(string gameId)
    {
        var lower = (gameId ?? string.Empty).ToLowerInvariant();
        bool isBlack2White2 = lower.Contains("black 2") ||
                              lower.Contains("white 2") ||
                              lower.Contains("black2") ||
                              lower.Contains("white2") ||
                              lower.Contains("b2w2");
        return isBlack2White2 ? (0x21400, 0x04DC) : (0x21600, 0x04D4);
    }

    private static (List<int> CaughtSpeciesIds, List<int> SeenSpeciesIds)? ReadPokedexFlagsAt(
        byte[] saveBytes,
        int baseOffset,
        int pokedexOffset,
        int pokedexSize)
    {
        int blockStart = baseOffset + pokedexOffset;
        int blockEnd = blockStart + pokedexSize;
        if (blockStart < 0 || blockEnd > saveBytes.Length) return null;

        int flagsStart = blockStart + PokedexFlagsOffsetInBlock;
        int caughtRegion = flagsStart;
        int seenRegion = flagsStart + PokedexFlagRegionSize;
        if (seenRegion + PokedexFlagRegionSize > saveBytes.Length) return null;
        if (IsErased(saveBytes, caughtRegion, PokedexFlagRegionSize) && IsErased(saveBytes, seenRegion, PokedexFlagRegionSize))
        {
            return null;
        }

        var caughtIds = new List<int>();
        var seenIds = new SortedSet<int>();
        for (int speciesId = 1; speciesId <= Gen5SpeciesCount; speciesId++)
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

        if (length >= 220)
        {
            uint partySeed = pid;
            for (int i = 136; i < 220; i += 2)
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
        if (speciesId is <= 0 or > 649) return null;

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
        if (string.IsNullOrWhiteSpace(pkmOtName)) pkmOtName = "TRAINER";

        int level = isParty && length >= 220 ? Math.Clamp((int)unshuffled[140], 1, 100) : CalculateLevelFromExp(speciesId, exp);
        int currentHp = isParty && length >= 220 ? ReadUInt16LE(unshuffled, 142) : 0;
        int maxHp = isParty && length >= 220 ? ReadUInt16LE(unshuffled, 144) : 0;

        int shinyVal = (otId ^ secretId) ^ ((int)(pid & 0xFFFF) ^ (int)(pid >> 16));
        bool isShiny = shinyVal < 8;

        string nature = NatureNames[(int)(pid % 25)];
        string pkmId = $"gen5_{speciesId}_{otId}_{slot}_{(isParty ? "p" : $"b{boxIndex}")}";

        int formBits = (unshuffled[64] & 0xF8) >> 3;
        string? form = null;
        if (speciesId == 201)
        {
            form = formBits switch
            {
                >= 0 and <= 25 => ((char)('A' + formBits)).ToString(),
                26 => "!",
                27 => "?",
                _ => "A"
            };
        }

        var summary = new PokemonSummaryDto
        {
            Id = pkmId,
            Species = speciesName,
            SpeciesId = speciesId,
            Form = form,
            Nickname = nickname,
            Level = level,
            Gender = genderStr,
            IsShiny = isShiny,
            OriginalTrainer = pkmOtName,
            OriginalTrainerId = otId.ToString(),
            OriginGame = gameId ?? "Pokemon Gen 5",
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

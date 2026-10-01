using System.Text;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon.Native;

/// <summary>
/// Native high-performance parser and mutator for Generation 3 Pokémon save files
/// (FireRed, LeafGreen, Emerald, Ruby, Sapphire).
/// Completely self-contained; requires zero external services or Docker containers.
/// </summary>
public static class Gen3SaveParser
{
    private const int SectionSize = 4096;
    private const int SectionCount = 14;
    private const int SlotSize = SectionSize * SectionCount; // 57,344 bytes
    private const uint Signature = 0x08012025;
    private const int FooterOffset = 4084; // 0x0FF4

    // Section data lengths for checksum calculation
    private static readonly int[] SectionDataLengths =
    [
        3884, // 0: Trainer Info
        3968, // 1: Team & Items
        3968, // 2: Game State
        3968, // 3: Misc Data
        3848, // 4: Rival Info
        3968, // 5: PC Buffer 0
        3968, // 6: PC Buffer 1
        3968, // 7: PC Buffer 2
        3968, // 8: PC Buffer 3
        3968, // 9: PC Buffer 4
        3968, // 10: PC Buffer 5
        3968, // 11: PC Buffer 6
        3968, // 12: PC Buffer 7
        2000  // 13: PC Buffer 8
    ];

    private static readonly string[] NatureNames =
    [
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky"
    ];

    // Permutation of the 4 substructures (0=Growth, 1=Attacks, 2=EVs, 3=Misc)
    private static readonly int[][] BlockOrders =
    [
        [0, 1, 2, 3], // 0: GAEM
        [0, 1, 3, 2], // 1: GAME
        [0, 2, 1, 3], // 2: GEAM
        [0, 2, 3, 1], // 3: GEMA
        [0, 3, 1, 2], // 4: GMAE
        [0, 3, 2, 1], // 5: GMEA
        [1, 0, 2, 3], // 6: AGEM
        [1, 0, 3, 2], // 7: AGME
        [1, 2, 0, 3], // 8: AEGM
        [1, 2, 3, 0], // 9: AEMG
        [1, 3, 0, 2], // 10: AMGE
        [1, 3, 2, 0], // 11: AMEG
        [2, 0, 1, 3], // 12: EGAM
        [2, 0, 3, 1], // 13: EGMA
        [2, 1, 0, 3], // 14: EAGM
        [2, 1, 3, 0], // 15: EAMG
        [2, 3, 0, 1], // 16: EMGA
        [2, 3, 1, 0], // 17: EMAG
        [3, 0, 1, 2], // 18: MGAE
        [3, 0, 2, 1], // 19: MGEA
        [3, 1, 0, 2], // 20: MAGE
        [3, 1, 2, 0], // 21: MAEG
        [3, 2, 0, 1], // 22: MEGA
        [3, 2, 1, 0], // 23: MEAG
    ];

    public static bool IsGen3Save(byte[] saveBytes)
    {
        if (saveBytes == null || saveBytes.Length < SectionSize)
        {
            return false;
        }

        // Check for signature in section footers
        for (int i = 0; i + SectionSize <= saveBytes.Length; i += SectionSize)
        {
            var sig = BitConverter.ToUInt32(saveBytes, i + FooterOffset + 4);
            if (sig == Signature)
            {
                return true;
            }
        }

        return false;
    }

    public static PokemonSaveParseResult Parse(byte[] saveBytes, string gameId, PkVaultStaticCatalog catalog)
    {
        var result = new PokemonSaveParseResult();

        if (saveBytes == null || saveBytes.Length < SectionSize)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Save data is missing or smaller than minimum GBA section size.";
            return result;
        }

        var (sections, activeSlotOffset) = ExtractActiveSections(saveBytes);
        if (sections == null || sections[0] == null)
        {
            result.IsSuccess = false;
            result.ErrorMessage = "Could not locate valid Generation 3 save sections.";
            return result;
        }

        var sec0 = sections[0];
        result.TrainerName = DecodeGen3String(sec0, 0, 7);
        var tid = BitConverter.ToUInt16(sec0, 0x0A);
        var sid = BitConverter.ToUInt16(sec0, 0x0C);
        result.TrainerId = tid.ToString();

        // Pokedex bitfields (Section 0: 0x0028 caught, 0x005C seen)
        var caughtIds = new HashSet<int>();
        var seenIds = new HashSet<int>();
        for (int offset = 0; offset < 49; offset++)
        {
            int cIdx = 0x0028 + offset;
            int sIdx = 0x005C + offset;
            byte cByte = cIdx < sec0.Length ? sec0[cIdx] : (byte)0;
            byte sByte = sIdx < sec0.Length ? sec0[sIdx] : (byte)0;
            for (int bit = 0; bit < 8; bit++)
            {
                int speciesNum = offset * 8 + bit + 1;
                if (speciesNum <= 386)
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

        // Trainer money (Section 1 in FRLG or RSE)
        var sec1 = sections[1];
        if (sec1 != null)
        {
            var isFrLg = gameId.Contains("fire", StringComparison.OrdinalIgnoreCase) ||
                         gameId.Contains("leaf", StringComparison.OrdinalIgnoreCase);

            // In FRLG, money is at 0x0290, security key at Section 0 + 0x0AF8
            if (isFrLg && sec0.Length > 0x0AFC && sec1.Length > 0x0294)
            {
                var securityKey = BitConverter.ToUInt32(sec0, 0x0AF8);
                var rawMoney = BitConverter.ToUInt32(sec1, 0x0290);
                result.Money = (int)(rawMoney ^ securityKey);
            }
            else if (sec1.Length > 0x0494)
            {
                result.Money = (int)BitConverter.ToUInt32(sec1, 0x0490);
            }
        }

        // Parse Party Pokémon (Section 1)
        var partyList = new List<PokemonSummaryDto>();
        if (sec1 != null)
        {
            // Auto-detect party offset: FRLG uses 0x0034; RSE uses 0x0234
            int partyCountOffset = 0x0034;
            int partyDataOffset = 0x0038;

            var frlgCount = BitConverter.ToUInt32(sec1, 0x0034);
            if (frlgCount > 6 && sec1.Length > 0x0238)
            {
                var rseCount = BitConverter.ToUInt32(sec1, 0x0234);
                if (rseCount <= 6)
                {
                    partyCountOffset = 0x0234;
                    partyDataOffset = 0x0238;
                }
            }

            int count = Math.Clamp((int)BitConverter.ToUInt32(sec1, partyCountOffset), 0, 6);
            for (int i = 0; i < count; i++)
            {
                int pkmOffset = partyDataOffset + (i * 100);
                if (pkmOffset + 100 <= sec1.Length)
                {
                    var pkm = ParsePokemon(sec1, pkmOffset, isParty: true, partySlot: i + 1, boxIndex: null, gameTitle: gameId, catalog: catalog);
                    if (pkm != null)
                    {
                        partyList.Add(pkm.Summary);
                        result.Details[pkm.Summary.Id] = pkm;
                    }
                }
            }
        }
        result.Party = partyList;

        // Parse PC Boxes (Sections 5 to 13)
        var boxesList = new List<PokemonBoxDto>();
        var pcStream = AssemblePcStream(sections);
        if (pcStream != null && pcStream.Length >= 33600)
        {
            for (int b = 0; b < 14; b++)
            {
                var boxDto = new PokemonBoxDto
                {
                    BoxIndex = b + 1,
                    Name = $"Box {b + 1}",
                    Capacity = 30,
                };

                var entries = new List<PokemonSummaryDto>();
                int boxBase = b * (30 * 80);

                for (int s = 0; s < 30; s++)
                {
                    int pkmOffset = boxBase + (s * 80);
                    if (pkmOffset + 80 <= pcStream.Length)
                    {
                        var pkm = ParsePokemon(pcStream, pkmOffset, isParty: false, partySlot: s + 1, boxIndex: b + 1, gameTitle: gameId, catalog: catalog);
                        if (pkm != null)
                        {
                            entries.Add(pkm.Summary);
                            result.Details[pkm.Summary.Id] = pkm;
                        }
                    }
                }

                boxDto.OccupiedCount = entries.Count;
                boxDto.Entries = entries;
                boxesList.Add(boxDto);
            }
        }
        else
        {
            // Create 14 empty boxes if PC stream could not be assembled
            for (int b = 1; b <= 14; b++)
            {
                boxesList.Add(new PokemonBoxDto { BoxIndex = b, Name = $"Box {b}", Capacity = 30, OccupiedCount = 0, Entries = [] });
            }
        }
        foreach (var p in partyList)
        {
            if (p.SpeciesId > 0)
            {
                caughtIds.Add(p.SpeciesId);
                seenIds.Add(p.SpeciesId);
            }
        }
        foreach (var b in boxesList)
        {
            foreach (var p in b.Entries)
            {
                if (p.SpeciesId > 0)
                {
                    caughtIds.Add(p.SpeciesId);
                    seenIds.Add(p.SpeciesId);
                }
            }
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
        bool isParty,
        int partySlot,
        int? boxIndex,
        string gameTitle,
        PkVaultStaticCatalog catalog)
    {
        if (offset + 80 > buffer.Length) return null;

        var pid = BitConverter.ToUInt32(buffer, offset + 0);
        var otid = BitConverter.ToUInt32(buffer, offset + 4);
        var tid = (ushort)(otid & 0xFFFF);
        var sid = (ushort)(otid >> 16);

        // 48 bytes of substructures at offset + 32
        var decrypted = new byte[48];
        Array.Copy(buffer, offset + 32, decrypted, 0, 48);

        uint key = pid ^ otid;
        for (int i = 0; i < 48; i += 4)
        {
            uint chunk = BitConverter.ToUInt32(decrypted, i);
            chunk ^= key;
            var bytes = BitConverter.GetBytes(chunk);
            Array.Copy(bytes, 0, decrypted, i, 4);
        }

        int orderIdx = (int)(pid % 24);
        var order = BlockOrders[orderIdx];

        int gOffset = Array.IndexOf(order, 0) * 12;
        int aOffset = Array.IndexOf(order, 1) * 12;
        int eOffset = Array.IndexOf(order, 2) * 12;
        int mOffset = Array.IndexOf(order, 3) * 12;

        ushort speciesId = BitConverter.ToUInt16(decrypted, gOffset + 0);
        if (speciesId == 0 || speciesId > 412)
        {
            return null; // Empty slot
        }

        ushort heldItemId = BitConverter.ToUInt16(decrypted, gOffset + 2);
        uint experience = BitConverter.ToUInt32(decrypted, gOffset + 4);
        byte friendship = decrypted[gOffset + 9];

        // Attacks
        var moveIds = new ushort[]
        {
            BitConverter.ToUInt16(decrypted, aOffset + 0),
            BitConverter.ToUInt16(decrypted, aOffset + 2),
            BitConverter.ToUInt16(decrypted, aOffset + 4),
            BitConverter.ToUInt16(decrypted, aOffset + 6),
        };
        var moves = moveIds.Where(m => m > 0).Select(m => catalog.ResolveMoveName(m)).ToList();

        // EVs
        var ev = new PokemonStatsDto
        {
            Hp = decrypted[eOffset + 0],
            Attack = decrypted[eOffset + 1],
            Defense = decrypted[eOffset + 2],
            Speed = decrypted[eOffset + 3],
            SpecialAttack = decrypted[eOffset + 4],
            SpecialDefense = decrypted[eOffset + 5],
        };

        // Misc (IVs, ability, egg)
        uint ivData = BitConverter.ToUInt32(decrypted, mOffset + 4);
        var iv = new PokemonStatsDto
        {
            Hp = (int)(ivData & 0x1F),
            Attack = (int)((ivData >> 5) & 0x1F),
            Defense = (int)((ivData >> 10) & 0x1F),
            Speed = (int)((ivData >> 15) & 0x1F),
            SpecialAttack = (int)((ivData >> 20) & 0x1F),
            SpecialDefense = (int)((ivData >> 25) & 0x1F),
        };
        bool isEgg = ((ivData >> 30) & 1) == 1;
        int abilityBit = (int)((ivData >> 31) & 1);

        string rawNickname = DecodeGen3String(buffer, offset + 8, 10);
        string otName = DecodeGen3String(buffer, offset + 20, 7);

        string speciesName = catalog.ResolveSpeciesName(speciesId, rawNickname, !string.IsNullOrWhiteSpace(rawNickname));
        string nickname = string.IsNullOrWhiteSpace(rawNickname) ? speciesName : rawNickname;

        int level = 5;
        int? currentHp = null;
        int? maxHp = null;

        if (isParty && offset + 100 <= buffer.Length)
        {
            level = Math.Clamp((int)buffer[offset + 84], 1, 100);
            currentHp = BitConverter.ToUInt16(buffer, offset + 86);
            maxHp = BitConverter.ToUInt16(buffer, offset + 88);
        }
        else
        {
            level = CalculateLevelFromExp(speciesId, experience);
        }

        bool isShiny = ((tid ^ sid) ^ ((ushort)(pid & 0xFFFF) ^ (ushort)(pid >> 16))) < 8;
        string nature = NatureNames[pid % 25];

        string? gender = ResolveGender(speciesId, pid);

        // Raw 80 or 100 byte payload
        int payloadLen = isParty && offset + 100 <= buffer.Length ? 100 : 80;
        var rawBytes = new byte[payloadLen];
        Array.Copy(buffer, offset, rawBytes, 0, payloadLen);
        string rawBase64 = Convert.ToBase64String(rawBytes);

        var pkmId = $"gen3_{speciesId}_{pid:x8}";

        var summary = new PokemonSummaryDto
        {
            Id = pkmId,
            Species = speciesName,
            SpeciesId = speciesId,
            Nickname = nickname,
            Level = level,
            Gender = gender,
            IsShiny = isShiny,
            OriginalTrainer = otName,
            OriginalTrainerId = tid.ToString(),
            OriginGame = gameTitle,
            CurrentGame = gameTitle,
            CurrentLocation = isParty ? "Party" : $"Box {boxIndex}",
            BoxIndex = boxIndex,
            SlotIndex = partySlot,
            IsInParty = isParty,
            LegalityStatus = "valid",
        };

        return new PokemonDetailsDto
        {
            Summary = summary,
            Nature = nature,
            Ability = ResolveGen3Ability(speciesId, abilityBit, catalog),
            HeldItem = heldItemId > 0 ? catalog.ResolveItemName(heldItemId) : null,
            Moves = moves,
            Iv = iv,
            Ev = ev,
            CurrentHp = currentHp,
            MaxHp = maxHp,
            Friendship = friendship,
            RawData = rawBase64,
            LegalityStatus = "valid",
        };
    }

    public static byte[] ExtractPokemon(byte[] saveBytes, bool isInParty, int? boxIndex, int slotIndex)
    {
        var copy = (byte[])saveBytes.Clone();
        var (sections, activeSlotOffset) = ExtractActiveSections(copy);
        if (sections == null) return copy;

        if (isInParty && sections[1] != null)
        {
            var sec1 = sections[1];
            int countOffset = BitConverter.ToUInt32(sec1, 0x0034) <= 6 ? 0x0034 : 0x0234;
            int dataOffset = countOffset + 4;
            int count = (int)BitConverter.ToUInt32(sec1, countOffset);

            int targetIdx = slotIndex - 1;
            if (targetIdx >= 0 && targetIdx < count)
            {
                // Shift subsequent party members up
                for (int i = targetIdx; i < count - 1; i++)
                {
                    Array.Copy(sec1, dataOffset + (i + 1) * 100, sec1, dataOffset + i * 100, 100);
                }
                // Zero out the last slot
                Array.Clear(sec1, dataOffset + (count - 1) * 100, 100);
                // Decrement count
                var newCountBytes = BitConverter.GetBytes((uint)(count - 1));
                Array.Copy(newCountBytes, 0, sec1, countOffset, 4);

                RecalculateSectionChecksum(sec1, 1);
            }
        }
        else if (boxIndex.HasValue)
        {
            var pcStream = AssemblePcStream(sections);
            if (pcStream != null)
            {
                int targetOffset = (boxIndex.Value - 1) * (30 * 80) + (slotIndex - 1) * 80;
                if (targetOffset + 80 <= pcStream.Length)
                {
                    Array.Clear(pcStream, targetOffset, 80);
                    DistributePcStream(pcStream, sections);
                }
            }
        }

        // Commit active sections back to save copy
        CommitSections(copy, sections, activeSlotOffset);
        return copy;
    }

    public static byte[] InjectPokemon(byte[] saveBytes, PokemonVaultEntry entry, int? targetBoxIndex, int targetSlotIndex)
    {
        var copy = (byte[])saveBytes.Clone();
        var (sections, activeSlotOffset) = ExtractActiveSections(copy);
        if (sections == null) return copy;

        byte[] pkm80 = CreatePokemon80Bytes(entry);

        var pcStream = AssemblePcStream(sections);
        if (pcStream != null)
        {
            int box = Math.Clamp(targetBoxIndex ?? 1, 1, 14) - 1;
            int slot = Math.Clamp(targetSlotIndex, 1, 30) - 1;
            int targetOffset = box * (30 * 80) + slot * 80;

            if (targetOffset + 80 <= pcStream.Length)
            {
                Array.Copy(pkm80, 0, pcStream, targetOffset, 80);
                DistributePcStream(pcStream, sections);
            }
        }

        CommitSections(copy, sections, activeSlotOffset);
        return copy;
    }

    private static (byte[][]? sections, int slotOffset) ExtractActiveSections(byte[] saveBytes)
    {
        int slot0Offset = 0;
        int slot1Offset = SlotSize;

        uint counter0 = GetSlotSaveCounter(saveBytes, slot0Offset);
        uint counter1 = saveBytes.Length >= slot1Offset + SlotSize ? GetSlotSaveCounter(saveBytes, slot1Offset) : 0;

        int activeOffset = slot0Offset;
        if (counter1 > counter0 && counter1 != uint.MaxValue)
        {
            activeOffset = slot1Offset;
        }

        var sections = new byte[SectionCount][];
        for (int i = 0; i < SectionCount; i++)
        {
            int secOffset = activeOffset + (i * SectionSize);
            if (secOffset + SectionSize > saveBytes.Length) break;

            ushort secId = BitConverter.ToUInt16(saveBytes, secOffset + FooterOffset);
            if (secId < SectionCount)
            {
                sections[secId] = new byte[SectionSize];
                Array.Copy(saveBytes, secOffset, sections[secId], 0, SectionSize);
            }
        }

        return (sections, activeOffset);
    }

    private static uint GetSlotSaveCounter(byte[] bytes, int slotOffset)
    {
        for (int i = 0; i < SectionCount; i++)
        {
            int secOffset = slotOffset + (i * SectionSize);
            if (secOffset + SectionSize > bytes.Length) break;

            ushort secId = BitConverter.ToUInt16(bytes, secOffset + FooterOffset);
            uint sig = BitConverter.ToUInt32(bytes, secOffset + FooterOffset + 4);
            if (secId == 0 && sig == Signature)
            {
                return BitConverter.ToUInt32(bytes, secOffset + FooterOffset + 8);
            }
        }
        return 0;
    }

    private static byte[]? AssemblePcStream(byte[][] sections)
    {
        if (sections[5] == null) return null;

        var pcStream = new byte[33600];
        int written = 0;

        // Section 5 starts at offset 4
        int s5Len = Math.Min(3968 - 4, 33600);
        Array.Copy(sections[5], 4, pcStream, 0, s5Len);
        written += s5Len;

        for (int s = 6; s <= 13 && written < 33600; s++)
        {
            if (sections[s] == null) break;
            int toCopy = Math.Min(3968, 33600 - written);
            Array.Copy(sections[s], 0, pcStream, written, toCopy);
            written += toCopy;
        }

        return pcStream;
    }

    private static void DistributePcStream(byte[] pcStream, byte[][] sections)
    {
        int read = 0;
        int s5Len = Math.Min(3968 - 4, pcStream.Length);
        Array.Copy(pcStream, 0, sections[5], 4, s5Len);
        read += s5Len;
        RecalculateSectionChecksum(sections[5], 5);

        for (int s = 6; s <= 13 && read < pcStream.Length; s++)
        {
            if (sections[s] == null) break;
            int toCopy = Math.Min(3968, pcStream.Length - read);
            Array.Copy(pcStream, read, sections[s], 0, toCopy);
            read += toCopy;
            RecalculateSectionChecksum(sections[s], s);
        }
    }

    private static void RecalculateSectionChecksum(byte[] section, int sectionId)
    {
        int length = SectionDataLengths[sectionId];
        uint sum = 0;
        for (int i = 0; i < length; i += 4)
        {
            sum += BitConverter.ToUInt32(section, i);
        }

        ushort checksum = (ushort)((sum & 0xFFFF) + (sum >> 16));
        var chkBytes = BitConverter.GetBytes(checksum);
        section[FooterOffset + 2] = chkBytes[0];
        section[FooterOffset + 3] = chkBytes[1];

        // Increment save counter
        uint counter = BitConverter.ToUInt32(section, FooterOffset + 8);
        var cntBytes = BitConverter.GetBytes(counter + 1);
        Array.Copy(cntBytes, 0, section, FooterOffset + 8, 4);
    }

    private static void CommitSections(byte[] target, byte[][] sections, int slotOffset)
    {
        for (int i = 0; i < SectionCount; i++)
        {
            var sec = sections[i];
            if (sec != null)
            {
                // Find where this sectionId belongs or write sequentially
                int destOffset = slotOffset + (i * SectionSize);
                if (destOffset + SectionSize <= target.Length)
                {
                    Array.Copy(sec, 0, target, destOffset, SectionSize);
                }
            }
        }
    }

    private static byte[] CreatePokemon80Bytes(PokemonVaultEntry entry)
    {
        if (!string.IsNullOrWhiteSpace(entry.RawData))
        {
            try
            {
                var raw = Convert.FromBase64String(entry.RawData);
                if (raw.Length >= 80)
                {
                    var pkm = new byte[80];
                    Array.Copy(raw, 0, pkm, 0, 80);
                    return pkm;
                }
            }
            catch { }
        }

        // Generate clean Gen 3 80-byte structure
        var bytes = new byte[80];
        var rng = new Random();
        uint pid = (uint)rng.Next();
        uint otid = 1337;

        Array.Copy(BitConverter.GetBytes(pid), 0, bytes, 0, 4);
        Array.Copy(BitConverter.GetBytes(otid), 0, bytes, 4, 4);

        var nickBytes = EncodeGen3String(entry.Nickname.ifEmpty(entry.Species), 10);
        Array.Copy(nickBytes, 0, bytes, 8, 10);

        var otBytes = EncodeGen3String(entry.OriginalTrainer.ifEmpty("Player"), 7);
        Array.Copy(otBytes, 0, bytes, 20, 7);

        // Substructures
        var decrypted = new byte[48];
        ushort specId = (ushort)Math.Clamp(entry.SpeciesId, 1, 386);
        Array.Copy(BitConverter.GetBytes(specId), 0, decrypted, 0, 2); // Species in Growth block

        // Encrypt with PID ^ OTID
        uint key = pid ^ otid;
        for (int i = 0; i < 48; i += 4)
        {
            uint chunk = BitConverter.ToUInt32(decrypted, i);
            chunk ^= key;
            Array.Copy(BitConverter.GetBytes(chunk), 0, bytes, 32 + i, 4);
        }

        // Checksum
        ushort sum = 0;
        for (int i = 0; i < 48; i += 2) sum += BitConverter.ToUInt16(decrypted, i);
        Array.Copy(BitConverter.GetBytes(sum), 0, bytes, 28, 2);

        return bytes;
    }

    private static string ifEmpty(this string? s, string fallback) =>
        string.IsNullOrWhiteSpace(s) ? fallback : s;

    public static string DecodeGen3String(byte[] data, int offset, int length)
    {
        var sb = new StringBuilder();
        for (int i = 0; i < length && offset + i < data.Length; i++)
        {
            byte b = data[offset + i];
            if (b == 0xFF) break;
            if (b == 0x00) sb.Append(' ');
            else if (b >= 0xA1 && b <= 0xAA) sb.Append((char)('0' + (b - 0xA1)));
            else if (b >= 0xBB && b <= 0xD4) sb.Append((char)('A' + (b - 0xBB)));
            else if (b >= 0xD5 && b <= 0xEE) sb.Append((char)('a' + (b - 0xD5)));
            else if (b == 0xAB) sb.Append('!');
            else if (b == 0xAC) sb.Append('?');
            else if (b == 0xAD) sb.Append('.');
            else if (b == 0xAE) sb.Append('-');
            else if (b == 0xB5) sb.Append('♀');
            else if (b == 0xB6) sb.Append('♂');
        }
        return sb.ToString().Trim();
    }

    public static byte[] EncodeGen3String(string text, int length)
    {
        var result = new byte[length];
        Array.Fill(result, (byte)0xFF);
        int outIdx = 0;
        foreach (char c in text)
        {
            if (outIdx >= length) break;
            if (c == ' ') result[outIdx++] = 0x00;
            else if (c >= '0' && c <= '9') result[outIdx++] = (byte)(0xA1 + (c - '0'));
            else if (c >= 'A' && c <= 'Z') result[outIdx++] = (byte)(0xBB + (c - 'A'));
            else if (c >= 'a' && c <= 'z') result[outIdx++] = (byte)(0xD5 + (c - 'a'));
            else if (c == '!') result[outIdx++] = 0xAB;
            else if (c == '?') result[outIdx++] = 0xAC;
            else if (c == '.') result[outIdx++] = 0xAD;
            else if (c == '-') result[outIdx++] = 0xAE;
            else if (c == '♀') result[outIdx++] = 0xB5;
            else if (c == '♂') result[outIdx++] = 0xB6;
        }
        return result;
    }

    private static int CalculateLevelFromExp(int speciesId, uint exp)
    {
        if (exp == 0) return 5;
        // Cubic approximation: L = cbrt(exp)
        int lvl = (int)Math.Pow(exp, 1.0 / 3.0);
        return Math.Clamp(lvl, 1, 100);
    }

    private static string? ResolveGender(int speciesId, uint pid)
    {
        // 0=Genderless, 1=MaleOnly, 2=FemaleOnly, or ratio
        byte threshold = (byte)(pid & 0xFF);
        // Common starter/general ratio: 12.5% female (threshold 31)
        return threshold < 31 ? "Female" : "Male";
    }

    private static string? ResolveGen3Ability(int speciesId, int abilityBit, PkVaultStaticCatalog catalog)
    {
        int abilityId = GetGen3AbilityId(speciesId, abilityBit);
        if (abilityId > 0)
        {
            var resolved = catalog.ResolveAbilityName(abilityId);
            if (!string.IsNullOrEmpty(resolved)) return resolved;
        }
        return abilityBit == 0 ? "Ability 1" : "Ability 2";
    }

    private static int GetGen3AbilityId(int speciesId, int abilityBit)
    {
        return (speciesId, abilityBit) switch
        {
            (1 or 2 or 3, _) => 65, // Overgrow
            (4 or 5 or 6, _) => 66, // Blaze
            (7 or 8 or 9, _) => 67, // Torrent
            (10 or 11, _) => 19, // Shield Dust
            (12, _) => 14, // Compound Eyes
            (13 or 14, _) => 19, // Shield Dust
            (15, _) => 68, // Swarm
            (16 or 17 or 18, _) => 51, // Keen Eye
            (19 or 20, 0) => 50, // Run Away
            (19 or 20, 1) => 62, // Guts
            (21 or 22, _) => 51, // Keen Eye
            (23 or 24, 0) => 22, // Intimidate
            (23 or 24, 1) => 61, // Shed Skin
            (25 or 26, _) => 9, // Static
            (27 or 28, _) => 8, // Sand Veil
            (29 or 30 or 31 or 32 or 33 or 34, _) => 38, // Poison Point
            (35 or 36, _) => 56, // Cute Charm
            (37 or 38, _) => 18, // Flash Fire
            (39 or 40, _) => 56, // Cute Charm
            (41 or 42, _) => 39, // Inner Focus
            (43 or 44 or 45, _) => 34, // Chlorophyll
            (46 or 47, _) => 27, // Effect Spore
            (48 or 49, _) => 14, // Compound Eyes
            (50 or 51, 0) => 8, // Sand Veil
            (50 or 51, 1) => 71, // Arena Trap
            (52 or 53, _) => 53, // Pickup
            (54 or 55, 0) => 6, // Damp
            (54 or 55, 1) => 13, // Cloud Nine
            (56 or 57, _) => 72, // Vital Spirit
            (58 or 59, 0) => 22, // Intimidate
            (58 or 59, 1) => 18, // Flash Fire
            (60 or 61 or 62, 0) => 11, // Water Absorb
            (60 or 61 or 62, 1) => 6, // Damp
            (63 or 64 or 65, 0) => 28, // Synchronize
            (63 or 64 or 65, 1) => 39, // Inner Focus
            (66 or 67 or 68, _) => 62, // Guts
            (69 or 70 or 71, _) => 34, // Chlorophyll
            (72 or 73, 0) => 29, // Clear Body
            (72 or 73, 1) => 64, // Liquid Ooze
            (74 or 75 or 76, 0) => 69, // Rock Head
            (74 or 75 or 76, 1) => 5, // Sturdy
            (77 or 78, 0) => 50, // Run Away
            (77 or 78, 1) => 18, // Flash Fire
            (79 or 80, 0) => 12, // Oblivious
            (79 or 80, 1) => 20, // Own Tempo
            (81 or 82, 0) => 42, // Magnet Pull
            (81 or 82, 1) => 5, // Sturdy
            (92 or 93 or 94, _) => 26, // Levitate
            (95, 0) => 69, // Rock Head
            (95, 1) => 5, // Sturdy
            (96 or 97, _) => 15, // Insomnia
            (129, _) => 33, // Swift Swim
            (130, _) => 22, // Intimidate
            (131, 0) => 11, // Water Absorb
            (131, 1) => 75, // Shell Armor
            (132, _) => 7, // Limber
            (133, _) => 50, // Run Away
            (134, _) => 11, // Water Absorb
            (135, _) => 10, // Volt Absorb
            (136, _) => 18, // Flash Fire
            (143, 0) => 17, // Immunity
            (143, 1) => 47, // Thick Fat
            (144 or 145 or 146 or 150, _) => 46, // Pressure
            (151, _) => 28, // Synchronize
            (252 or 253 or 254, _) => 65, // Overgrow
            (255 or 256 or 257, _) => 66, // Blaze
            (258 or 259 or 260, _) => 67, // Torrent
            _ => 0
        };
    }
}


using System.Collections.Concurrent;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// File-based implementation of IPokemonVaultStore.
/// Stores per-user vaults at {PokemonRoot}/vaults/{userId:N}/vault.json with per-user concurrency locking.
/// </summary>
public sealed class FilePokemonVaultStore(ICompanionPaths paths, IClock clock) : IPokemonVaultStore
{
    private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> Locks = new();

    public async Task<PokemonVault> GetOrCreateVaultAsync(Guid userId, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = VaultFilePath(userId);
            if (File.Exists(path))
            {
                var existing = await JsonFile.ReadAsync<PokemonVault>(path, cancellationToken).ConfigureAwait(false);
                if (existing != null)
                {
                    var modified = EnsureBoxes(existing);
                    if (modified)
                    {
                        Directory.CreateDirectory(UserVaultDirectory(userId));
                        await JsonFile.WriteAtomicAsync(path, existing, cancellationToken).ConfigureAwait(false);
                    }
                    return existing;
                }
            }

            var defaultVault = PokemonVault.CreateDefault(userId, clock.UtcNow);
            Directory.CreateDirectory(UserVaultDirectory(userId));
            await JsonFile.WriteAtomicAsync(path, defaultVault, cancellationToken).ConfigureAwait(false);
            return defaultVault;
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<PokemonVaultSummaryDto> GetVaultSummaryAsync(Guid userId, CancellationToken cancellationToken)
    {
        var vault = await GetOrCreateVaultAsync(userId, cancellationToken).ConfigureAwait(false);
        var totalCount = vault.Boxes.Sum(b => b.Entries.Count);
        var shinyCount = vault.Boxes.Sum(b => b.Entries.Count(e => e.IsShiny));

        return new PokemonVaultSummaryDto
        {
            UserId = userId,
            TotalCount = totalCount,
            BoxCount = vault.Boxes.Count,
            BoxCapacity = PokemonVaultBox.DefaultCapacity,
            TotalCapacity = vault.Boxes.Count * PokemonVaultBox.DefaultCapacity,
            ShinyCount = shinyCount,
            CreatedAtUtc = vault.CreatedAtUtc,
            UpdatedAtUtc = vault.UpdatedAtUtc
        };
    }

    public async Task<IReadOnlyList<PokemonVaultBoxSummaryDto>> GetBoxesSummaryAsync(Guid userId, CancellationToken cancellationToken)
    {
        var vault = await GetOrCreateVaultAsync(userId, cancellationToken).ConfigureAwait(false);
        return vault.Boxes.Select(b => new PokemonVaultBoxSummaryDto
        {
            BoxIndex = b.BoxIndex,
            Name = b.Name,
            Count = b.Entries.Count,
            Capacity = b.Capacity
        }).ToList();
    }

    public async Task<PokemonVaultBox?> GetBoxAsync(Guid userId, int boxIndex, CancellationToken cancellationToken)
    {
        var vault = await GetOrCreateVaultAsync(userId, cancellationToken).ConfigureAwait(false);
        return vault.Boxes.FirstOrDefault(b => b.BoxIndex == boxIndex);
    }

    public async Task<PokemonVaultEntry?> GetEntryAsync(Guid userId, string entryId, CancellationToken cancellationToken)
    {
        var vault = await GetOrCreateVaultAsync(userId, cancellationToken).ConfigureAwait(false);
        return vault.Boxes.SelectMany(b => b.Entries)
            .FirstOrDefault(e => string.Equals(e.Id, entryId, StringComparison.OrdinalIgnoreCase));
    }

    public async Task<PokemonVault> SaveVaultAsync(Guid userId, PokemonVault vault, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            vault.UserId = userId;
            vault.UpdatedAtUtc = clock.UtcNow;
            EnsureBoxes(vault);
            Directory.CreateDirectory(UserVaultDirectory(userId));
            await JsonFile.WriteAtomicAsync(VaultFilePath(userId), vault, cancellationToken).ConfigureAwait(false);
            return vault;
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<PokemonVaultEntry> AddOrUpdateEntryAsync(Guid userId, PokemonVaultEntry entry, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = VaultFilePath(userId);
            PokemonVault vault;
            if (File.Exists(path))
            {
                vault = (await JsonFile.ReadAsync<PokemonVault>(path, cancellationToken).ConfigureAwait(false))
                        ?? PokemonVault.CreateDefault(userId, clock.UtcNow);
            }
            else
            {
                vault = PokemonVault.CreateDefault(userId, clock.UtcNow);
            }

            EnsureBoxes(vault);

            // Remove existing entry from any box if updating
            foreach (var b in vault.Boxes)
            {
                b.Entries.RemoveAll(e => string.Equals(e.Id, entry.Id, StringComparison.OrdinalIgnoreCase));
            }

            var targetBox = vault.Boxes.FirstOrDefault(b => b.BoxIndex == entry.BoxIndex) ?? vault.Boxes[0];

            // If the specified slot is invalid (<= 0 or > Capacity) or already occupied by another entry in the target box,
            // find the next available slot starting from the target box.
            if (entry.SlotIndex <= 0 ||
                entry.SlotIndex > targetBox.Capacity ||
                targetBox.Entries.Any(e => e.SlotIndex == entry.SlotIndex))
            {
                var (nextBoxIdx, nextSlotIdx) = FindNextSlotInternal(vault, targetBox.BoxIndex);
                if (nextBoxIdx > 0 && nextSlotIdx > 0)
                {
                    targetBox = vault.Boxes.FirstOrDefault(b => b.BoxIndex == nextBoxIdx) ?? targetBox;
                    entry.BoxIndex = targetBox.BoxIndex;
                    entry.SlotIndex = nextSlotIdx;
                }
                else
                {
                    entry.BoxIndex = targetBox.BoxIndex;
                }
            }
            else
            {
                entry.BoxIndex = targetBox.BoxIndex;
            }

            if (string.IsNullOrWhiteSpace(entry.CurrentLocation) || entry.CurrentLocation.StartsWith("Box ") || entry.CurrentLocation.StartsWith("Vault Box "))
            {
                entry.CurrentLocation = $"{targetBox.Name}, Slot {entry.SlotIndex}";
            }

            var now = clock.UtcNow;
            if (entry.CreatedAtUtc == default)
            {
                entry.CreatedAtUtc = now;
            }
            entry.UpdatedAtUtc = now;
            vault.UpdatedAtUtc = now;

            targetBox.Entries.Add(entry);
            Directory.CreateDirectory(UserVaultDirectory(userId));
            await JsonFile.WriteAtomicAsync(path, vault, cancellationToken).ConfigureAwait(false);
            return entry;
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<(int BoxIndex, int SlotIndex)> FindNextAvailableSlotAsync(Guid userId, int preferredBoxIndex = 1, CancellationToken cancellationToken = default)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = VaultFilePath(userId);
            PokemonVault vault;
            if (File.Exists(path))
            {
                vault = (await JsonFile.ReadAsync<PokemonVault>(path, cancellationToken).ConfigureAwait(false))
                        ?? PokemonVault.CreateDefault(userId, clock.UtcNow);
            }
            else
            {
                vault = PokemonVault.CreateDefault(userId, clock.UtcNow);
            }

            var modified = EnsureBoxes(vault);
            if (modified)
            {
                Directory.CreateDirectory(UserVaultDirectory(userId));
                await JsonFile.WriteAtomicAsync(path, vault, cancellationToken).ConfigureAwait(false);
            }

            return FindNextSlotInternal(vault, preferredBoxIndex);
        }
        finally
        {
            lockObj.Release();
        }
    }

    public static (int BoxIndex, int SlotIndex) FindNextSlotInternal(PokemonVault vault, int preferredBoxIndex = 1)
    {
        if (vault.Boxes == null || vault.Boxes.Count == 0)
        {
            return (0, 0);
        }

        var startBox = Math.Clamp(preferredBoxIndex, 1, vault.Boxes.Count);

        // Check from preferredBoxIndex to end of boxes
        for (var bIdx = startBox; bIdx <= vault.Boxes.Count; bIdx++)
        {
            var box = vault.Boxes.FirstOrDefault(b => b.BoxIndex == bIdx);
            if (box == null)
            {
                continue;
            }

            var occupied = new HashSet<int>(box.Entries.Select(e => e.SlotIndex));
            for (var slot = 1; slot <= box.Capacity; slot++)
            {
                if (!occupied.Contains(slot))
                {
                    return (box.BoxIndex, slot);
                }
            }
        }

        // Wrap around from Box 1 up to startBox - 1
        for (var bIdx = 1; bIdx < startBox; bIdx++)
        {
            var box = vault.Boxes.FirstOrDefault(b => b.BoxIndex == bIdx);
            if (box == null)
            {
                continue;
            }

            var occupied = new HashSet<int>(box.Entries.Select(e => e.SlotIndex));
            for (var slot = 1; slot <= box.Capacity; slot++)
            {
                if (!occupied.Contains(slot))
                {
                    return (box.BoxIndex, slot);
                }
            }
        }

        return (0, 0);
    }

    public async Task<bool> RemoveEntryAsync(Guid userId, string entryId, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = VaultFilePath(userId);
            if (!File.Exists(path))
            {
                return false;
            }

            var vault = await JsonFile.ReadAsync<PokemonVault>(path, cancellationToken).ConfigureAwait(false);
            if (vault == null)
            {
                return false;
            }

            var removed = false;
            foreach (var box in vault.Boxes)
            {
                if (box.Entries.RemoveAll(e => string.Equals(e.Id, entryId, StringComparison.OrdinalIgnoreCase)) > 0)
                {
                    removed = true;
                }
            }

            if (removed)
            {
                vault.UpdatedAtUtc = clock.UtcNow;
                await JsonFile.WriteAtomicAsync(path, vault, cancellationToken).ConfigureAwait(false);
            }

            return removed;
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<PokemonVaultMoveResult> MoveEntryAsync(Guid userId, string entryId, int targetBoxIndex, int? targetSlotIndex, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = VaultFilePath(userId);
            if (!File.Exists(path))
            {
                return new PokemonVaultMoveResult { Success = false, Message = "Personal Vault not found." };
            }

            var vault = await JsonFile.ReadAsync<PokemonVault>(path, cancellationToken).ConfigureAwait(false);
            if (vault == null)
            {
                return new PokemonVaultMoveResult { Success = false, Message = "Personal Vault could not be loaded." };
            }

            EnsureBoxes(vault);

            PokemonVaultBox? sourceBox = null;
            PokemonVaultEntry? sourceEntry = null;
            foreach (var b in vault.Boxes)
            {
                sourceEntry = b.Entries.FirstOrDefault(e => string.Equals(e.Id, entryId, StringComparison.OrdinalIgnoreCase));
                if (sourceEntry != null)
                {
                    sourceBox = b;
                    break;
                }
            }

            if (sourceEntry == null || sourceBox == null)
            {
                return new PokemonVaultMoveResult { Success = false, Message = $"Pokémon entry '{entryId}' not found in Personal Vault." };
            }

            var clampedBox = Math.Clamp(targetBoxIndex, 1, vault.Boxes.Count);
            var targetBox = vault.Boxes.FirstOrDefault(b => b.BoxIndex == clampedBox) ?? vault.Boxes[0];

            int resolvedSlot;
            if (!targetSlotIndex.HasValue || targetSlotIndex.Value < 1 || targetSlotIndex.Value > targetBox.Capacity)
            {
                var occupied = new HashSet<int>(targetBox.Entries.Where(e => !string.Equals(e.Id, sourceEntry.Id, StringComparison.OrdinalIgnoreCase)).Select(e => e.SlotIndex));
                int freeSlot = 0;
                for (int s = 1; s <= targetBox.Capacity; s++)
                {
                    if (!occupied.Contains(s))
                    {
                        freeSlot = s;
                        break;
                    }
                }

                if (freeSlot == 0)
                {
                    return new PokemonVaultMoveResult { Success = false, Message = $"{targetBox.Name} is completely full." };
                }
                resolvedSlot = freeSlot;
            }
            else
            {
                resolvedSlot = targetSlotIndex.Value;
            }

            // If same box and same slot, no-op
            if (sourceBox.BoxIndex == targetBox.BoxIndex && sourceEntry.SlotIndex == resolvedSlot)
            {
                return new PokemonVaultMoveResult
                {
                    Success = true,
                    Message = $"{sourceEntry.Species} is already in {targetBox.Name}, Slot {resolvedSlot}.",
                    MovedEntry = sourceEntry,
                    WasSwapped = false
                };
            }

            var now = clock.UtcNow;
            var occupant = targetBox.Entries.FirstOrDefault(e => e.SlotIndex == resolvedSlot && !string.Equals(e.Id, sourceEntry.Id, StringComparison.OrdinalIgnoreCase));

            if (occupant != null)
            {
                // SWAP
                occupant.BoxIndex = sourceBox.BoxIndex;
                occupant.SlotIndex = sourceEntry.SlotIndex;
                occupant.CurrentLocation = $"{sourceBox.Name}, Slot {occupant.SlotIndex}";
                occupant.UpdatedAtUtc = now;

                if (sourceBox.BoxIndex != targetBox.BoxIndex)
                {
                    targetBox.Entries.Remove(occupant);
                    sourceBox.Entries.Add(occupant);
                }

                sourceEntry.BoxIndex = targetBox.BoxIndex;
                sourceEntry.SlotIndex = resolvedSlot;
                sourceEntry.CurrentLocation = $"{targetBox.Name}, Slot {sourceEntry.SlotIndex}";
                sourceEntry.UpdatedAtUtc = now;

                if (sourceBox.BoxIndex != targetBox.BoxIndex)
                {
                    sourceBox.Entries.Remove(sourceEntry);
                    targetBox.Entries.Add(sourceEntry);
                }

                vault.UpdatedAtUtc = now;
                await JsonFile.WriteAtomicAsync(path, vault, cancellationToken).ConfigureAwait(false);

                return new PokemonVaultMoveResult
                {
                    Success = true,
                    Message = $"Swapped {sourceEntry.Species} with {occupant.Species}.",
                    MovedEntry = sourceEntry,
                    DisplacedEntry = occupant,
                    WasSwapped = true
                };
            }
            else
            {
                // MOVE to empty slot
                sourceEntry.BoxIndex = targetBox.BoxIndex;
                sourceEntry.SlotIndex = resolvedSlot;
                sourceEntry.CurrentLocation = $"{targetBox.Name}, Slot {sourceEntry.SlotIndex}";
                sourceEntry.UpdatedAtUtc = now;

                if (sourceBox.BoxIndex != targetBox.BoxIndex)
                {
                    sourceBox.Entries.Remove(sourceEntry);
                    targetBox.Entries.Add(sourceEntry);
                }

                vault.UpdatedAtUtc = now;
                await JsonFile.WriteAtomicAsync(path, vault, cancellationToken).ConfigureAwait(false);

                return new PokemonVaultMoveResult
                {
                    Success = true,
                    Message = $"Moved {sourceEntry.Species} to {targetBox.Name}, Slot {sourceEntry.SlotIndex}.",
                    MovedEntry = sourceEntry,
                    WasSwapped = false
                };
            }
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<PokemonVaultEntry?> UpdateEntryMovesAsync(Guid userId, string entryId, IReadOnlyList<string> moves, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = VaultFilePath(userId);
            if (!File.Exists(path))
            {
                return null;
            }

            var vault = await JsonFile.ReadAsync<PokemonVault>(path, cancellationToken).ConfigureAwait(false);
            if (vault == null)
            {
                return null;
            }

            var entry = vault.Boxes.SelectMany(b => b.Entries)
                .FirstOrDefault(e => string.Equals(e.Id, entryId, StringComparison.OrdinalIgnoreCase));
            if (entry == null)
            {
                return null;
            }

            entry.Details ??= new PokemonDetailsDto { Summary = entry.ToSummaryDto() };
            entry.Details.Moves = moves;
            entry.Details.MovesLegality = moves.Select(_ => true).ToList();
            entry.Details.LearnableMoves ??= PokemonMovepoolProvider.GetLearnableMoves(entry.SpeciesId, entry.Species, entry.Level);
            entry.UpdatedAtUtc = clock.UtcNow;
            vault.UpdatedAtUtc = clock.UtcNow;

            await JsonFile.WriteAtomicAsync(path, vault, cancellationToken).ConfigureAwait(false);
            return entry;
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<PokemonVaultEntry?> EvolveEntryAsync(Guid userId, string entryId, int targetSpeciesId, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = VaultFilePath(userId);
            if (!File.Exists(path))
            {
                return null;
            }

            var vault = await JsonFile.ReadAsync<PokemonVault>(path, cancellationToken).ConfigureAwait(false);
            if (vault == null)
            {
                return null;
            }

            var entry = vault.Boxes.SelectMany(b => b.Entries)
                .FirstOrDefault(e => string.Equals(e.Id, entryId, StringComparison.OrdinalIgnoreCase));
            if (entry == null)
            {
                return null;
            }

            var availableEvolutions = PokemonEvolutionCatalog.GetAvailableEvolutions(
                entry.SpeciesId,
                entry.Species,
                entry.Level,
                entry.Details?.HeldItem);

            var evolutionMatch = availableEvolutions.FirstOrDefault(e => e.TargetSpeciesId == targetSpeciesId);
            if (evolutionMatch == null)
            {
                return null;
            }

            // Update species identity & preserve nicknames if custom
            var wasNicknamed = !string.Equals(entry.Nickname, entry.Species, StringComparison.OrdinalIgnoreCase);
            entry.SpeciesId = evolutionMatch.TargetSpeciesId;
            entry.Species = evolutionMatch.TargetSpecies;
            if (!wasNicknamed)
            {
                entry.Nickname = evolutionMatch.TargetSpecies;
            }

            entry.Details ??= new PokemonDetailsDto();
            entry.Details.Summary = entry.ToSummaryDto();
            entry.Details.LearnableMoves = PokemonMovepoolProvider.GetLearnableMoves(entry.SpeciesId, entry.Species, entry.Level);
            entry.Details.AvailableEvolutions = PokemonEvolutionCatalog.GetAvailableEvolutions(entry.SpeciesId, entry.Species, entry.Level, entry.Details.HeldItem);
            entry.Details.Ribbons = PokemonRibbonCatalog.EvaluateRibbons(
                null,
                entry.Details.Ev,
                entry.Level,
                entry.Details.Friendship ?? 100,
                entry.OriginGame,
                entry.Generation,
                isInParty: false);
            entry.Details.IsHallOfFameMember = entry.Details.Ribbons.Any(r => r.Category.Equals("Champion", StringComparison.OrdinalIgnoreCase)) || entry.Level >= 55;

            // Consume held item if item was required
            if (!string.IsNullOrEmpty(evolutionMatch.RequiredItem) &&
                string.Equals(entry.Details.HeldItem, evolutionMatch.RequiredItem, StringComparison.OrdinalIgnoreCase))
            {
                entry.Details.HeldItem = null;
            }

            entry.UpdatedAtUtc = clock.UtcNow;
            vault.UpdatedAtUtc = clock.UtcNow;

            await JsonFile.WriteAtomicAsync(path, vault, cancellationToken).ConfigureAwait(false);
            return entry;
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<PokemonUserProfile> GetUserProfileAsync(Guid userId, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = ProfileFilePath(userId);
            if (File.Exists(path))
            {
                var profile = await JsonFile.ReadAsync<PokemonUserProfile>(path, cancellationToken).ConfigureAwait(false);
                if (profile != null)
                {
                    return profile;
                }
            }

            return new PokemonUserProfile
            {
                UserId = userId,
                UpdatedAtUtc = clock.UtcNow
            };
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<PokemonUserProfile> SaveUserProfileAsync(Guid userId, PokemonUserProfile profile, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            profile.UserId = userId;
            profile.UpdatedAtUtc = clock.UtcNow;
            Directory.CreateDirectory(UserVaultDirectory(userId));
            await JsonFile.WriteAtomicAsync(ProfileFilePath(userId), profile, cancellationToken).ConfigureAwait(false);
            return profile;
        }
        finally
        {
            lockObj.Release();
        }
    }

    public async Task<PokemonVaultBox?> SortBoxAsync(Guid userId, int boxIndex, string criterion, bool ascending, CancellationToken cancellationToken)
    {
        var lockObj = LockFor(userId);
        await lockObj.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = VaultFilePath(userId);
            if (!File.Exists(path))
            {
                return null;
            }

            var vault = await JsonFile.ReadAsync<PokemonVault>(path, cancellationToken).ConfigureAwait(false);
            if (vault == null)
            {
                return null;
            }

            EnsureBoxes(vault);
            var box = vault.Boxes.FirstOrDefault(b => b.BoxIndex == boxIndex);
            if (box == null || box.Entries.Count == 0)
            {
                return box;
            }

            IEnumerable<PokemonVaultEntry> query = criterion.Trim().ToLowerInvariant() switch
            {
                "dex" or "pokedex" or "national" => ascending
                    ? box.Entries.OrderBy(e => e.SpeciesId).ThenBy(e => e.Level).ThenBy(e => e.Species)
                    : box.Entries.OrderByDescending(e => e.SpeciesId).ThenBy(e => e.Level).ThenBy(e => e.Species),
                "level" or "lvl" => ascending
                    ? box.Entries.OrderBy(e => e.Level).ThenBy(e => e.SpeciesId)
                    : box.Entries.OrderByDescending(e => e.Level).ThenBy(e => e.SpeciesId),
                "name" or "species" or "alphabetical" => ascending
                    ? box.Entries.OrderBy(e => e.Nickname ?? e.Species).ThenBy(e => e.Level)
                    : box.Entries.OrderByDescending(e => e.Nickname ?? e.Species).ThenBy(e => e.Level),
                "shiny" => ascending
                    ? box.Entries.OrderBy(e => e.IsShiny ? 0 : 1).ThenBy(e => e.SpeciesId).ThenBy(e => e.Level)
                    : box.Entries.OrderByDescending(e => e.IsShiny ? 1 : 0).ThenBy(e => e.SpeciesId).ThenBy(e => e.Level),
                "iv" or "potential" => ascending
                    ? box.Entries.OrderBy(e => TotalIv(e)).ThenBy(e => e.Level)
                    : box.Entries.OrderByDescending(e => TotalIv(e)).ThenBy(e => e.Level),
                _ => ascending
                    ? box.Entries.OrderBy(e => e.SpeciesId).ThenBy(e => e.Level)
                    : box.Entries.OrderByDescending(e => e.SpeciesId).ThenBy(e => e.Level)
            };

            var sortedEntries = query.ToList();
            for (var i = 0; i < sortedEntries.Count; i++)
            {
                var entry = sortedEntries[i];
                entry.SlotIndex = i + 1;
                entry.CurrentLocation = $"Vault Box {boxIndex}, Slot {i + 1}";
                entry.UpdatedAtUtc = clock.UtcNow;
            }

            box.Entries = sortedEntries;
            vault.UpdatedAtUtc = clock.UtcNow;

            await JsonFile.WriteAtomicAsync(path, vault, cancellationToken).ConfigureAwait(false);
            return box;
        }
        finally
        {
            lockObj.Release();
        }
    }

    private static int TotalIv(PokemonVaultEntry entry)
    {
        var iv = entry.Details?.Iv;
        if (iv == null) return 0;
        return iv.Hp + iv.Attack + iv.Defense + iv.Speed + iv.SpecialAttack + iv.SpecialDefense;
    }

    private static bool EnsureBoxes(PokemonVault vault)
    {
        var modified = false;
        if (vault.Boxes == null)
        {
            vault.Boxes = [];
            modified = true;
        }

        for (var i = 1; i <= PokemonVault.DefaultBoxCount; i++)
        {
            var box = vault.Boxes.FirstOrDefault(b => b.BoxIndex == i);
            if (box == null)
            {
                box = new PokemonVaultBox
                {
                    BoxIndex = i,
                    Name = $"Box {i}",
                    Capacity = PokemonVaultBox.DefaultCapacity,
                    Entries = []
                };
                vault.Boxes.Add(box);
                modified = true;
            }
            else if (box.Entries == null)
            {
                box.Entries = [];
                modified = true;
            }
        }

        vault.Boxes.Sort((a, b) => a.BoxIndex.CompareTo(b.BoxIndex));

        foreach (var b in vault.Boxes)
        {
            foreach (var entry in b.Entries)
            {
                bool isGen3 = entry.Generation == 3 ||
                              entry.OriginGame.Contains("fire", StringComparison.OrdinalIgnoreCase) ||
                              entry.OriginGame.Contains("leaf", StringComparison.OrdinalIgnoreCase) ||
                              entry.OriginGame.Contains("ruby", StringComparison.OrdinalIgnoreCase) ||
                              entry.OriginGame.Contains("sapph", StringComparison.OrdinalIgnoreCase) ||
                              entry.OriginGame.Contains("emer", StringComparison.OrdinalIgnoreCase);

                if ((entry.SpeciesId == 410 || entry.SpeciesId == 362 || entry.SpeciesId == 386) &&
                    (string.Equals(entry.Nickname, "DEOXYS", StringComparison.OrdinalIgnoreCase) ||
                     string.Equals(entry.Species, "DEOXYS", StringComparison.OrdinalIgnoreCase)))
                {
                    if (entry.SpeciesId != 386 || entry.Species != "Deoxys")
                    {
                        entry.SpeciesId = 386;
                        entry.Species = "Deoxys";
                        modified = true;
                    }
                }
                else if (entry.SpeciesId == 410 && (isGen3 || string.Equals(entry.Species, "Shieldon", StringComparison.OrdinalIgnoreCase)))
                {
                    if (entry.SpeciesId != 386 || entry.Species != "Deoxys")
                    {
                        entry.SpeciesId = 386;
                        entry.Species = "Deoxys";
                        modified = true;
                    }
                }
                else if (entry.SpeciesId is >= 277 and <= 411 && isGen3)
                {
                    var currentName = PkVault.PokemonSpeciesCatalog.ResolveSpeciesName(entry.SpeciesId);
                    if (!string.Equals(entry.Species, currentName, StringComparison.OrdinalIgnoreCase))
                    {
                        var corrected = Native.Gen3SaveParser.Gen3InternalIdToNationalDex(entry.SpeciesId);
                        if (corrected > 0)
                        {
                            entry.SpeciesId = corrected;
                            entry.Species = PkVault.PokemonSpeciesCatalog.ResolveSpeciesName(corrected);
                            modified = true;
                        }
                    }
                }
            }
        }

        // Heal duplicate or invalid slot collisions across all boxes
        foreach (var box in vault.Boxes)
        {
            var occupiedSlots = new HashSet<int>();
            var duplicates = new List<PokemonVaultEntry>();

            foreach (var entry in box.Entries)
            {
                entry.BoxIndex = box.BoxIndex;
                if (entry.SlotIndex >= 1 && entry.SlotIndex <= box.Capacity && occupiedSlots.Add(entry.SlotIndex))
                {
                    continue;
                }

                duplicates.Add(entry);
            }

            foreach (var dup in duplicates)
            {
                var (nextBoxIdx, nextSlotIdx) = FindNextSlotInternal(vault, box.BoxIndex);
                if (nextBoxIdx > 0 && nextSlotIdx > 0)
                {
                    var destBox = vault.Boxes.FirstOrDefault(b => b.BoxIndex == nextBoxIdx);
                    if (destBox != null)
                    {
                        if (destBox != box)
                        {
                            box.Entries.Remove(dup);
                            destBox.Entries.Add(dup);
                        }
                        dup.BoxIndex = destBox.BoxIndex;
                        dup.SlotIndex = nextSlotIdx;
                        if (string.IsNullOrWhiteSpace(dup.CurrentLocation) || dup.CurrentLocation.StartsWith("Box ") || dup.CurrentLocation.StartsWith("Vault Box "))
                        {
                            dup.CurrentLocation = $"{destBox.Name}, Slot {dup.SlotIndex}";
                        }
                        modified = true;
                    }
                }
            }
        }

        return modified;
    }

    private string UserVaultDirectory(Guid userId) => Path.Combine(paths.PokemonRoot, "vaults", userId.ToString("N"));

    private string VaultFilePath(Guid userId) => Path.Combine(UserVaultDirectory(userId), "vault.json");

    private string ProfileFilePath(Guid userId) => Path.Combine(UserVaultDirectory(userId), "profile.json");

    private static SemaphoreSlim LockFor(Guid userId) => Locks.GetOrAdd(userId, _ => new SemaphoreSlim(1, 1));
}

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
                    EnsureBoxes(existing);
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
            entry.BoxIndex = targetBox.BoxIndex;
            if (string.IsNullOrWhiteSpace(entry.CurrentLocation))
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

    private static void EnsureBoxes(PokemonVault vault)
    {
        vault.Boxes ??= [];

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
            }
            else
            {
                box.Entries ??= [];
            }
        }

        vault.Boxes.Sort((a, b) => a.BoxIndex.CompareTo(b.BoxIndex));
    }

    private string UserVaultDirectory(Guid userId) => Path.Combine(paths.PokemonRoot, "vaults", userId.ToString("N"));

    private string VaultFilePath(Guid userId) => Path.Combine(UserVaultDirectory(userId), "vault.json");

    private string ProfileFilePath(Guid userId) => Path.Combine(UserVaultDirectory(userId), "profile.json");

    private static SemaphoreSlim LockFor(Guid userId) => Locks.GetOrAdd(userId, _ => new SemaphoreSlim(1, 1));
}

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

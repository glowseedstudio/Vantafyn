using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Pokemon;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonVaultStoreTests
{
    [Fact]
    public async Task GetOrCreateVaultAsync_InitializesDefault30BoxesAndPersists()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userId = Guid.NewGuid();

        var vault = await store.GetOrCreateVaultAsync(userId, CancellationToken.None);

        Assert.NotNull(vault);
        Assert.Equal(userId, vault.UserId);
        Assert.Equal(30, vault.Boxes.Count);
        Assert.Equal("Box 1", vault.Boxes[0].Name);
        Assert.Equal("Box 30", vault.Boxes[29].Name);
        Assert.All(vault.Boxes, b => Assert.Equal(30, b.Capacity));
        Assert.All(vault.Boxes, b => Assert.Empty(b.Entries));

        // Subsequent call returns the existing vault
        var reload = await store.GetOrCreateVaultAsync(userId, CancellationToken.None);
        Assert.NotNull(reload);
        Assert.Equal(vault.CreatedAtUtc, reload.CreatedAtUtc);
    }

    [Fact]
    public async Task VaultSummary_ReflectsAccurateCountsAndShiny()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userId = Guid.NewGuid();

        await store.AddOrUpdateEntryAsync(userId, new PokemonVaultEntry
        {
            Id = "charizard-1",
            BoxIndex = 1,
            SlotIndex = 1,
            Species = "Charizard",
            SpeciesId = 6,
            Nickname = "Flame",
            Level = 50,
            IsShiny = true,
            OriginalTrainer = "Red",
            OriginGame = "Pokémon FireRed"
        }, CancellationToken.None);

        await store.AddOrUpdateEntryAsync(userId, new PokemonVaultEntry
        {
            Id = "blastoise-1",
            BoxIndex = 2,
            SlotIndex = 1,
            Species = "Blastoise",
            SpeciesId = 9,
            Nickname = "Hydro",
            Level = 45,
            IsShiny = false,
            OriginalTrainer = "Blue",
            OriginGame = "Pokémon FireRed"
        }, CancellationToken.None);

        var summary = await store.GetVaultSummaryAsync(userId, CancellationToken.None);

        Assert.Equal(userId, summary.UserId);
        Assert.Equal(2, summary.TotalCount);
        Assert.Equal(1, summary.ShinyCount);
        Assert.Equal(30, summary.BoxCount);
        Assert.Equal(900, summary.TotalCapacity);
    }

    [Fact]
    public async Task AddOrUpdateEntry_UpdatesEntryInPlace()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userId = Guid.NewGuid();

        var entry = new PokemonVaultEntry
        {
            Id = "pikachu-test",
            BoxIndex = 1,
            SlotIndex = 5,
            Species = "Pikachu",
            SpeciesId = 25,
            Nickname = "Sparky",
            Level = 20,
            IsShiny = false,
            OriginalTrainer = "Ash",
            OriginGame = "Pokémon Emerald"
        };

        await store.AddOrUpdateEntryAsync(userId, entry, CancellationToken.None);

        var fetched = await store.GetEntryAsync(userId, "pikachu-test", CancellationToken.None);
        Assert.NotNull(fetched);
        Assert.Equal("Sparky", fetched.Nickname);
        Assert.Equal(20, fetched.Level);
        Assert.Equal("Box 1, Slot 5", fetched.CurrentLocation);

        // Update level and nickname
        entry.Level = 25;
        entry.Nickname = "Sparky II";
        await store.AddOrUpdateEntryAsync(userId, entry, CancellationToken.None);

        var updated = await store.GetEntryAsync(userId, "pikachu-test", CancellationToken.None);
        Assert.NotNull(updated);
        Assert.Equal("Sparky II", updated.Nickname);
        Assert.Equal(25, updated.Level);

        // Verify it didn't create a duplicate in the box
        var box = await store.GetBoxAsync(userId, 1, CancellationToken.None);
        Assert.NotNull(box);
        Assert.Single(box.Entries);
    }

    [Fact]
    public async Task RemoveEntry_RemovesFromVault()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userId = Guid.NewGuid();

        await store.AddOrUpdateEntryAsync(userId, new PokemonVaultEntry
        {
            Id = "mew-1",
            BoxIndex = 1,
            SlotIndex = 1,
            Species = "Mew",
            SpeciesId = 151,
            Nickname = "Mew",
            Level = 30,
            OriginGame = "Pokémon Emerald"
        }, CancellationToken.None);

        Assert.NotNull(await store.GetEntryAsync(userId, "mew-1", CancellationToken.None));

        var removed = await store.RemoveEntryAsync(userId, "mew-1", CancellationToken.None);
        Assert.True(removed);

        Assert.Null(await store.GetEntryAsync(userId, "mew-1", CancellationToken.None));

        var secondRemoval = await store.RemoveEntryAsync(userId, "mew-1", CancellationToken.None);
        Assert.False(secondRemoval);
    }

    [Fact]
    public async Task UsersAreStrictlyIsolated_UserACannotSeeUserBVaultOrEntries()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userA = Guid.NewGuid();
        var userB = Guid.NewGuid();

        // User A stores a shiny Charizard
        await store.AddOrUpdateEntryAsync(userA, new PokemonVaultEntry
        {
            Id = "secret-charizard",
            BoxIndex = 1,
            SlotIndex = 1,
            Species = "Charizard",
            SpeciesId = 6,
            Nickname = "Dragon",
            Level = 100,
            IsShiny = true,
            OriginalTrainer = "PlayerA",
            OriginGame = "Pokémon Emerald"
        }, CancellationToken.None);

        // Verify User A has 1 Pokemon
        var summaryA = await store.GetVaultSummaryAsync(userA, CancellationToken.None);
        Assert.Equal(1, summaryA.TotalCount);
        Assert.Equal(1, summaryA.ShinyCount);

        // Verify User B has an empty vault
        var summaryB = await store.GetVaultSummaryAsync(userB, CancellationToken.None);
        Assert.Equal(0, summaryB.TotalCount);
        Assert.Equal(0, summaryB.ShinyCount);

        // User B cannot find User A's entry
        var entryForB = await store.GetEntryAsync(userB, "secret-charizard", CancellationToken.None);
        Assert.Null(entryForB);

        // User B box 1 is empty
        var boxB = await store.GetBoxAsync(userB, 1, CancellationToken.None);
        Assert.NotNull(boxB);
        Assert.Empty(boxB.Entries);

        // User A box 1 still contains the Pokemon
        var boxA = await store.GetBoxAsync(userA, 1, CancellationToken.None);
        Assert.NotNull(boxA);
        Assert.Single(boxA.Entries);
        Assert.Equal("secret-charizard", boxA.Entries[0].Id);
    }

    [Fact]
    public async Task UserProfile_SavesAndRetrievesPerUser()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userA = Guid.NewGuid();
        var userB = Guid.NewGuid();

        var profileA = new PokemonUserProfile
        {
            UserId = userA,
            LastSelectedGameId = "emerald-retro",
            CustomBoxNames = new Dictionary<int, string>
            {
                [1] = "Shinies",
                [2] = "Legendaries"
            }
        };

        await store.SaveUserProfileAsync(userA, profileA, CancellationToken.None);

        var loadedA = await store.GetUserProfileAsync(userA, CancellationToken.None);
        Assert.Equal("emerald-retro", loadedA.LastSelectedGameId);
        Assert.Equal("Shinies", loadedA.CustomBoxNames[1]);
        Assert.Equal("Legendaries", loadedA.CustomBoxNames[2]);

        // User B has untouched profile
        var loadedB = await store.GetUserProfileAsync(userB, CancellationToken.None);
        Assert.Null(loadedB.LastSelectedGameId);
        Assert.Empty(loadedB.CustomBoxNames);
    }

    [Fact]
    public async Task GetBoxesSummary_ReturnsAll30BoxesWithCorrectCounts()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userId = Guid.NewGuid();

        await store.AddOrUpdateEntryAsync(userId, new PokemonVaultEntry
        {
            Id = "p1",
            BoxIndex = 1,
            SlotIndex = 1,
            Species = "Bulbasaur"
        }, CancellationToken.None);

        await store.AddOrUpdateEntryAsync(userId, new PokemonVaultEntry
        {
            Id = "p2",
            BoxIndex = 1,
            SlotIndex = 2,
            Species = "Ivysaur"
        }, CancellationToken.None);

        await store.AddOrUpdateEntryAsync(userId, new PokemonVaultEntry
        {
            Id = "p3",
            BoxIndex = 5,
            SlotIndex = 1,
            Species = "Venusaur"
        }, CancellationToken.None);

        var boxes = await store.GetBoxesSummaryAsync(userId, CancellationToken.None);

        Assert.Equal(30, boxes.Count);
        Assert.Equal(2, boxes[0].Count); // Box 1
        Assert.Equal(0, boxes[1].Count); // Box 2
        Assert.Equal(1, boxes[4].Count); // Box 5
        Assert.Equal("Box 5", boxes[4].Name);
    }

    [Fact]
    public async Task GetBox_InvalidIndices_ReturnsNull()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userId = Guid.NewGuid();

        Assert.Null(await store.GetBoxAsync(userId, 0, CancellationToken.None));
        Assert.Null(await store.GetBoxAsync(userId, -1, CancellationToken.None));
        Assert.Null(await store.GetBoxAsync(userId, 31, CancellationToken.None));
        Assert.NotNull(await store.GetBoxAsync(userId, 1, CancellationToken.None));
        Assert.NotNull(await store.GetBoxAsync(userId, 30, CancellationToken.None));
    }

    [Fact]
    public async Task ConcurrentOperations_AreSafeAndConsistent()
    {
        using var temp = new TempPaths();
        var clock = new FixedClock();
        var store = new FilePokemonVaultStore(temp, clock);
        var userId = Guid.NewGuid();

        // Concurrently insert 30 Pokemon into Box 1 across multiple threads
        var tasks = Enumerable.Range(1, 30).Select(i => store.AddOrUpdateEntryAsync(userId, new PokemonVaultEntry
        {
            Id = $"pokemon-{i}",
            BoxIndex = 1,
            SlotIndex = i,
            Species = $"Mon-{i}",
            Level = i
        }, CancellationToken.None));

        await Task.WhenAll(tasks);

        var box = await store.GetBoxAsync(userId, 1, CancellationToken.None);
        Assert.NotNull(box);
        Assert.Equal(30, box.Entries.Count);

        var summary = await store.GetVaultSummaryAsync(userId, CancellationToken.None);
        Assert.Equal(30, summary.TotalCount);
    }

    private sealed class FixedClock : IClock
    {
        public DateTimeOffset UtcNow => new(2026, 9, 30, 12, 0, 0, TimeSpan.Zero);
    }

    private sealed class TempPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-pokemon-vault-tests", Guid.NewGuid().ToString("N"));
        public string DataRoot => _root;
        public string UserSettingsRoot => Directory.CreateDirectory(Path.Combine(_root, "user-settings")).FullName;
        public string PersonalPlaylistsRoot => Directory.CreateDirectory(Path.Combine(_root, "personal-playlists")).FullName;
        public string OmbiSessionsRoot => Directory.CreateDirectory(Path.Combine(_root, "ombi-sessions")).FullName;
        public string SecretsRoot => Directory.CreateDirectory(Path.Combine(_root, "secrets")).FullName;
        public string PushRegistrationsRoot => Directory.CreateDirectory(Path.Combine(_root, "push-registrations")).FullName;
        public string GameSavesRoot => Directory.CreateDirectory(Path.Combine(_root, "game-saves")).FullName;
        public string PokemonRoot => Directory.CreateDirectory(Path.Combine(_root, "pokemon")).FullName;

        public void Dispose()
        {
            if (Directory.Exists(_root))
            {
                Directory.Delete(_root, recursive: true);
            }
        }
    }
}

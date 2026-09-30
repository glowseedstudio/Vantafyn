using System;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Pokemon;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonJourneyAndPokedexTests : IDisposable
{
    private readonly string _tempDir;
    private readonly TestCompanionPaths _paths;
    private readonly TestClock _clock;

    public PokemonJourneyAndPokedexTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "VantafynPokedexTests_" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(_tempDir);
        _paths = new TestCompanionPaths(_tempDir);
        _clock = new TestClock(DateTimeOffset.UtcNow);
    }

    public void Dispose()
    {
        if (Directory.Exists(_tempDir))
        {
            try { Directory.Delete(_tempDir, true); } catch { }
        }
    }

    [Fact]
    public async Task GetUserPokedex_EmptyVault_ReturnsDefaultGenerationsWithZeroCounts()
    {
        var service = new FilePokemonJourneyService(_paths, _clock);
        var userId = Guid.NewGuid();

        var pokedex = await service.GetUserPokedexAsync(userId);

        Assert.NotNull(pokedex);
        Assert.Equal(userId, pokedex.UserId);
        Assert.True(pokedex.IsEnabled);
        Assert.Equal(0, pokedex.TotalCaught);
        Assert.Equal(0, pokedex.TotalSeen);
        Assert.Equal(0, pokedex.TotalShinies);
        Assert.Equal(9, pokedex.GenerationProgress.Count);
        Assert.All(pokedex.GenerationProgress, g => Assert.Equal(0, g.CaughtCount));
    }

    [Fact]
    public async Task RecordEncounter_SinglePokemon_UpdatesPokedexAndGenerationStats()
    {
        var service = new FilePokemonJourneyService(_paths, _clock);
        var userId = Guid.NewGuid();

        var pikachu = new PokemonSummaryDto
        {
            Id = "pika-1",
            Species = "Pikachu",
            SpeciesId = 25,
            Level = 15,
            IsShiny = true,
            OriginGame = "Pokémon Yellow"
        };

        await service.RecordEncounterAsync(userId, pikachu, "Pokémon Yellow", isCaught: true);

        var pokedex = await service.GetUserPokedexAsync(userId);

        Assert.Equal(1, pokedex.TotalCaught);
        Assert.Equal(1, pokedex.TotalSeen);
        Assert.Equal(1, pokedex.TotalShinies);

        var gen1 = pokedex.GenerationProgress.First(g => g.Generation == 1);
        Assert.Equal(1, gen1.CaughtCount);
        Assert.Equal(1, gen1.SeenCount);
        Assert.Equal(1, gen1.ShinyCount);

        var entry = Assert.Single(pokedex.Entries);
        Assert.Equal(25, entry.SpeciesId);
        Assert.Equal("Pikachu", entry.SpeciesName);
        Assert.True(entry.IsCaught);
        Assert.True(entry.IsSeen);
        Assert.True(entry.HasShiny);
        Assert.Equal("Pokémon Yellow", entry.FirstEncounteredGame);
    }

    [Fact]
    public async Task RecordJourneyStep_AppendsTimelineInOrder()
    {
        var service = new FilePokemonJourneyService(_paths, _clock);
        var userId = Guid.NewGuid();
        var charizardId = "char-123";

        var charizard = new PokemonSummaryDto
        {
            Id = charizardId,
            Species = "Charizard",
            SpeciesId = 6,
            Level = 36,
            IsShiny = false,
            OriginGame = "Pokémon FireRed",
            OriginalTrainer = "Red",
            OriginalTrainerId = "00001"
        };

        // Step 1: Caught in FireRed
        await service.RecordJourneyStepAsync(userId, charizardId, new PokemonJourneyStepDto
        {
            Action = "Encounter",
            SourceLocation = "Pallet Town",
            DestinationLocation = "Party Slot 1",
            GameTitle = "Pokémon FireRed",
            Generation = 3,
            Details = "Chosen as starter Charmander in FireRed"
        }, charizard);

        // Step 2: Deposited to Vault
        _clock.UtcNow = _clock.UtcNow.AddHours(2);
        await service.RecordJourneyStepAsync(userId, charizardId, new PokemonJourneyStepDto
        {
            Action = "Deposit",
            SourceLocation = "Pokémon FireRed (Party Slot 1)",
            DestinationLocation = "Personal Vault (Box 1 Slot 1)",
            GameTitle = "Pokémon FireRed",
            Generation = 3,
            Details = "Deposited into Vault Box 1"
        }, charizard);

        // Step 3: Withdrawn to HeartGold
        _clock.UtcNow = _clock.UtcNow.AddHours(5);
        await service.RecordJourneyStepAsync(userId, charizardId, new PokemonJourneyStepDto
        {
            Action = "Withdraw",
            SourceLocation = "Personal Vault (Box 1 Slot 1)",
            DestinationLocation = "Pokémon HeartGold (Box 1 Slot 1)",
            GameTitle = "Pokémon HeartGold",
            Generation = 4,
            Details = "Pal Park transfer to Johto"
        }, charizard);

        var journey = await service.GetPokemonJourneyAsync(userId, charizardId);

        Assert.NotNull(journey);
        Assert.Equal(charizardId, journey.PokemonId);
        Assert.Equal("Charizard", journey.Species);
        Assert.Equal("Pokémon FireRed", journey.OriginGame);
        Assert.Equal("Red", journey.OriginalTrainer);
        Assert.Equal(3, journey.Steps.Count);
        Assert.Equal("Encounter", journey.Steps[0].Action);
        Assert.Equal("Deposit", journey.Steps[1].Action);
        Assert.Equal("Withdraw", journey.Steps[2].Action);
    }

    [Fact]
    public async Task SynchronizeWithVault_PopulatesEntriesFromExistingVault()
    {
        var service = new FilePokemonJourneyService(_paths, _clock);
        var userId = Guid.NewGuid();

        var vault = PokemonVault.CreateDefault(userId, _clock.UtcNow);
        vault.Boxes[0].Entries.Add(new PokemonVaultEntry
        {
            Id = "mewtwo-1",
            BoxIndex = 1,
            SlotIndex = 1,
            Species = "Mewtwo",
            SpeciesId = 150,
            Level = 70,
            IsShiny = false,
            OriginGame = "Pokémon Blue",
            OriginalTrainer = "Blue",
            CreatedAtUtc = _clock.UtcNow,
            UpdatedAtUtc = _clock.UtcNow
        });
        vault.Boxes[1].Entries.Add(new PokemonVaultEntry
        {
            Id = "lucario-1",
            BoxIndex = 2,
            SlotIndex = 5,
            Species = "Lucario",
            SpeciesId = 448,
            Level = 50,
            IsShiny = true,
            OriginGame = "Pokémon Diamond",
            OriginalTrainer = "Dawn",
            CreatedAtUtc = _clock.UtcNow,
            UpdatedAtUtc = _clock.UtcNow
        });

        await service.SynchronizeWithVaultAsync(userId, vault);

        var pokedex = await service.GetUserPokedexAsync(userId);

        Assert.Equal(2, pokedex.TotalCaught);
        Assert.Equal(2, pokedex.TotalSeen);
        Assert.Equal(1, pokedex.TotalShinies);

        var gen1 = pokedex.GenerationProgress.First(g => g.Generation == 1);
        Assert.Equal(1, gen1.CaughtCount);

        var gen4 = pokedex.GenerationProgress.First(g => g.Generation == 4);
        Assert.Equal(1, gen4.CaughtCount);
        Assert.Equal(1, gen4.ShinyCount);
    }

    [Fact]
    public async Task UserIsolation_DifferentUsersDoNotSharePokedexOrJourneys()
    {
        var service = new FilePokemonJourneyService(_paths, _clock);
        var userA = Guid.NewGuid();
        var userB = Guid.NewGuid();

        var gengar = new PokemonSummaryDto
        {
            Id = "gengar-1",
            Species = "Gengar",
            SpeciesId = 94,
            Level = 45,
            IsShiny = false
        };

        await service.RecordEncounterAsync(userA, gengar, "Pokémon FireRed", isCaught: true);
        await service.RecordJourneyStepAsync(userA, "gengar-1", new PokemonJourneyStepDto { Action = "Encounter" }, gengar);

        var dexA = await service.GetUserPokedexAsync(userA);
        var dexB = await service.GetUserPokedexAsync(userB);

        var journeyA = await service.GetPokemonJourneyAsync(userA, "gengar-1");
        var journeyB = await service.GetPokemonJourneyAsync(userB, "gengar-1");

        Assert.Equal(1, dexA.TotalCaught);
        Assert.Equal(0, dexB.TotalCaught);

        Assert.NotNull(journeyA);
        Assert.Null(journeyB);
    }

    private sealed class TestCompanionPaths : ICompanionPaths
    {
        private readonly string _root;
        public TestCompanionPaths(string root) => _root = root;
        public string DataRoot => _root;
        public string UserSettingsRoot => Path.Combine(_root, "user-settings");
        public string PersonalPlaylistsRoot => Path.Combine(_root, "personal-playlists");
        public string OmbiSessionsRoot => Path.Combine(_root, "ombi-sessions");
        public string SecretsRoot => Path.Combine(_root, "secrets");
        public string PushRegistrationsRoot => Path.Combine(_root, "push-registrations");
        public string GameSavesRoot => Path.Combine(_root, "game-saves");
        public string PokemonRoot => Path.Combine(_root, "pokemon");
    }

    private sealed class TestClock : IClock
    {
        public DateTimeOffset UtcNow { get; set; }
        public TestClock(DateTimeOffset now) => UtcNow = now;
    }
}

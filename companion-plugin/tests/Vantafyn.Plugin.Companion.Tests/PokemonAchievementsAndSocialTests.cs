using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Pokemon;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonAchievementsAndSocialTests : IDisposable
{
    private readonly string _tempDir;
    private readonly TestCompanionPaths _paths;
    private readonly TestClock _clock;

    public PokemonAchievementsAndSocialTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "VantafynSocialTests_" + Guid.NewGuid().ToString("N"));
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
    public async Task GetUserAchievements_InitialUser_ReturnsAll10BadgesLockedWithZeroScore()
    {
        var service = new FilePokemonSocialService(_paths, _clock);
        var userId = Guid.NewGuid();

        var summary = await service.GetUserAchievementsAsync(userId);

        Assert.NotNull(summary);
        Assert.Equal(userId, summary.UserId);
        Assert.Equal(10, summary.TotalCount);
        Assert.Equal(0, summary.UnlockedCount);
        Assert.Equal(0, summary.TotalScore);
        Assert.Equal(10, summary.Achievements.Count);
        Assert.All(summary.Achievements, a =>
        {
            Assert.False(a.IsUnlocked);
            Assert.Null(a.UnlockedAtUtc);
            Assert.Equal(0, a.CurrentProgress);
        });
    }

    [Fact]
    public async Task ProcessPokemonDeposited_FirstDeposit_UnlocksVaultInitiateAndLogsActivity()
    {
        var service = new FilePokemonSocialService(_paths, _clock);
        var userId = Guid.NewGuid();
        var pokemon = new PokemonSummaryDto
        {
            Id = "pkm-1",
            Species = "Pikachu",
            SpeciesId = 25,
            Nickname = "Sparky",
            Level = 20,
            IsShiny = false
        };

        await service.ProcessPokemonDepositedAsync(userId, "Ash", pokemon, totalVaultCount: 1);

        var summary = await service.GetUserAchievementsAsync(userId);
        var firstDep = summary.Achievements.FirstOrDefault(a => a.Id == "pk-vault-first-deposit");
        Assert.NotNull(firstDep);
        Assert.True(firstDep.IsUnlocked);
        Assert.NotNull(firstDep.UnlockedAtUtc);
        Assert.Equal(1, firstDep.CurrentProgress);
        Assert.Equal(1, summary.UnlockedCount);
        Assert.Equal(100, summary.TotalScore);

        var activity = await service.GetRecentActivityAsync(10);
        // Latest event is PokemonDeposited, earlier is AchievementUnlocked
        Assert.Equal(2, activity.Count);
        Assert.Equal("PokemonDeposited", activity[0].EventType);
        Assert.Equal("Ash", activity[0].UserName);
        Assert.Equal("Pikachu", activity[0].SpeciesName);
        Assert.Equal("AchievementUnlocked", activity[1].EventType);
    }

    [Fact]
    public async Task ProcessPokemonDeposited_FiftyCount_UnlocksCollectorBadge()
    {
        var service = new FilePokemonSocialService(_paths, _clock);
        var userId = Guid.NewGuid();
        var pokemon = new PokemonSummaryDto
        {
            Id = "pkm-50",
            Species = "Charizard",
            SpeciesId = 6,
            Nickname = "Blaze",
            Level = 50
        };

        await service.ProcessPokemonDepositedAsync(userId, "Red", pokemon, totalVaultCount: 50);

        var summary = await service.GetUserAchievementsAsync(userId);
        var collector = summary.Achievements.FirstOrDefault(a => a.Id == "pk-vault-50");
        Assert.NotNull(collector);
        Assert.True(collector.IsUnlocked);
        Assert.Equal(50, collector.CurrentProgress);
    }

    [Fact]
    public async Task ProcessPokemonWithdrawn_UnlocksReadyForBattleAndLogsActivity()
    {
        var service = new FilePokemonSocialService(_paths, _clock);
        var userId = Guid.NewGuid();
        var pokemon = new PokemonSummaryDto
        {
            Id = "pkm-2",
            Species = "Snorlax",
            SpeciesId = 143,
            Nickname = "BigGuy",
            Level = 30
        };

        await service.ProcessPokemonWithdrawnAsync(userId, "Ash", pokemon, "Pokemon Emerald");

        var summary = await service.GetUserAchievementsAsync(userId);
        var withdrawAch = summary.Achievements.FirstOrDefault(a => a.Id == "pk-vault-withdraw");
        Assert.NotNull(withdrawAch);
        Assert.True(withdrawAch.IsUnlocked);

        var activity = await service.GetRecentActivityAsync(10);
        Assert.Equal(2, activity.Count);
        Assert.Equal("PokemonWithdrawn", activity[0].EventType);
        Assert.Contains("Pokemon Emerald", activity[0].Description);
    }

    [Fact]
    public async Task ProcessPokemonTransferred_CrossGen_UnlocksBothTransferAndCrossGenBadges()
    {
        var service = new FilePokemonSocialService(_paths, _clock);
        var userId = Guid.NewGuid();
        var pokemon = new PokemonSummaryDto
        {
            Id = "pkm-3",
            Species = "Lucario",
            SpeciesId = 448,
            Nickname = "Aura",
            Level = 45
        };

        await service.ProcessPokemonTransferredAsync(userId, "Riley", pokemon, "Pokemon Diamond", "Pokemon Black", isCrossGen: true);

        var summary = await service.GetUserAchievementsAsync(userId);
        var transferAch = summary.Achievements.FirstOrDefault(a => a.Id == "pk-transfer-first");
        var crossGenAch = summary.Achievements.FirstOrDefault(a => a.Id == "pk-crossgen-transfer");

        Assert.NotNull(transferAch);
        Assert.True(transferAch.IsUnlocked);
        Assert.NotNull(crossGenAch);
        Assert.True(crossGenAch.IsUnlocked);

        var activity = await service.GetRecentActivityAsync(10);
        Assert.Equal("CrossGenerationTransferCompleted", activity[0].EventType);
        Assert.Contains("Pokemon Diamond", activity[0].Description);
        Assert.Contains("Pokemon Black", activity[0].Description);
    }

    [Fact]
    public async Task ProcessShinyAdded_UnlocksGottaGleamEmAll()
    {
        var service = new FilePokemonSocialService(_paths, _clock);
        var userId = Guid.NewGuid();
        var shiny = new PokemonSummaryDto
        {
            Id = "pkm-shiny",
            Species = "Gyarados",
            SpeciesId = 130,
            Nickname = "Red Dragon",
            Level = 30,
            IsShiny = true
        };

        await service.ProcessShinyAddedAsync(userId, "Lance", shiny);

        var summary = await service.GetUserAchievementsAsync(userId);
        var shinyAch = summary.Achievements.FirstOrDefault(a => a.Id == "pk-shiny-first");

        Assert.NotNull(shinyAch);
        Assert.True(shinyAch.IsUnlocked);
        Assert.Equal(500, shinyAch.Score);

        var activity = await service.GetRecentActivityAsync(10);
        Assert.Equal("ShinyAdded", activity[0].EventType);
        Assert.True(activity[0].IsShiny);
    }

    [Fact]
    public async Task ProcessPokedexMilestones_UnlocksResearchAndKantoMaster()
    {
        var service = new FilePokemonSocialService(_paths, _clock);
        var userId = Guid.NewGuid();

        await service.ProcessPokedexMilestoneAsync(userId, "Oak", totalCaught: 10, gen1Caught: 10);
        var s1 = await service.GetUserAchievementsAsync(userId);
        var a10 = s1.Achievements.First(a => a.Id == "pk-pokedex-10");
        Assert.True(a10.IsUnlocked);

        await service.ProcessPokedexMilestoneAsync(userId, "Oak", totalCaught: 50, gen1Caught: 50);
        var s2 = await service.GetUserAchievementsAsync(userId);
        var a50 = s2.Achievements.First(a => a.Id == "pk-pokedex-50");
        Assert.True(a50.IsUnlocked);

        await service.ProcessPokedexMilestoneAsync(userId, "Oak", totalCaught: 151, gen1Caught: 151);
        var s3 = await service.GetUserAchievementsAsync(userId);
        var aKanto = s3.Achievements.First(a => a.Id == "pk-pokedex-kanto-master");
        Assert.True(aKanto.IsUnlocked);
        Assert.Equal(1000, aKanto.Score);
    }

    [Fact]
    public async Task ProcessTradeCompleted_BothUsers_UnlocksTradeBadgeForBothAndLogsActivity()
    {
        var service = new FilePokemonSocialService(_paths, _clock);
        var user1 = Guid.NewGuid();
        var user2 = Guid.NewGuid();

        var p1 = new PokemonSummaryDto
        {
            Id = "p1",
            Species = "Haunter",
            SpeciesId = 93,
            Nickname = "Spooky",
            Level = 35
        };

        var p2 = new PokemonSummaryDto
        {
            Id = "p2",
            Species = "Machoke",
            SpeciesId = 67,
            Nickname = "Muscle",
            Level = 35
        };

        await service.ProcessTradeCompletedAsync(user1, "Ash", user2, "Gary", p1, p2);

        var s1 = await service.GetUserAchievementsAsync(user1);
        var s2 = await service.GetUserAchievementsAsync(user2);

        Assert.True(s1.Achievements.First(a => a.Id == "pk-trade-first").IsUnlocked);
        Assert.True(s2.Achievements.First(a => a.Id == "pk-trade-first").IsUnlocked);

        var activity = await service.GetRecentActivityAsync(10);
        Assert.Equal("TradeCompleted", activity[0].EventType);
        Assert.Contains("Ash traded Haunter with Gary for Machoke", activity[0].Description);
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

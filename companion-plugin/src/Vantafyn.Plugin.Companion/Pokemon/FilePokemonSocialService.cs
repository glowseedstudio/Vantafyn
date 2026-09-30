using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Notifications;

namespace Vantafyn.Plugin.Companion.Pokemon;

public sealed class FilePokemonSocialService : IPokemonSocialService
{
    private readonly ICompanionPaths _paths;
    private readonly IClock _clock;
    private readonly IPushNotificationService? _pushService;
    private readonly ILogger<FilePokemonSocialService> _logger;
    private static readonly SemaphoreSlim ActivityLock = new(1, 1);
    private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> UserLocks = new();

    private sealed record AchievementDefinition(
        string Id,
        string Title,
        string Description,
        string Rarity,
        int Score,
        string IconName,
        int MaxProgress);

    private static readonly AchievementDefinition[] Definitions =
    [
        new("pk-vault-first-deposit", "Vault Initiate", "Deposited your first Pokémon into the personal cloud vault.", "Common", 100, "cloud_done", 1),
        new("pk-vault-50", "Pokemon Collector", "Stored 50 or more Pokémon in your personal vault.", "Rare", 250, "inventory_2", 50),
        new("pk-vault-withdraw", "Ready for Battle", "Withdrew a Pokémon from the cloud vault into an active game save.", "Common", 100, "sports_esports", 1),
        new("pk-transfer-first", "Cross-Game Traveler", "Transferred a Pokémon directly from one game save to another.", "Uncommon", 150, "swap_horiz", 1),
        new("pk-crossgen-transfer", "Time Traveler", "Successfully performed a cross-generation migration across historical eras.", "Epic", 300, "history_toggle_off", 1),
        new("pk-shiny-first", "Gotta Gleam 'Em All", "Registered a Shiny Pokémon in your vault or Pokédex.", "Legendary", 500, "auto_awesome", 1),
        new("pk-pokedex-10", "Research Assistant", "Registered 10 unique species in the National Pokédex.", "Common", 100, "menu_book", 10),
        new("pk-pokedex-50", "Field Researcher", "Registered 50 unique species in the National Pokédex.", "Rare", 300, "science", 50),
        new("pk-pokedex-kanto-master", "Kanto Master", "Completed the Generation I Pokédex (all 151 species).", "Mythic", 1000, "military_tech", 151),
        new("pk-trade-first", "Link Cable Connection", "Completed a Pokémon trade with another trainer.", "Rare", 250, "people", 1)
    ];

    public FilePokemonSocialService(
        ICompanionPaths paths,
        IClock clock,
        IPushNotificationService? pushService = null,
        ILogger<FilePokemonSocialService>? logger = null)
    {
        _paths = paths;
        _clock = clock;
        _pushService = pushService;
        _logger = logger ?? NullLogger<FilePokemonSocialService>.Instance;
    }

    private SemaphoreSlim UserLockFor(Guid userId) =>
        UserLocks.GetOrAdd(userId, _ => new SemaphoreSlim(1, 1));

    private string ActivityFilePath =>
        Path.Combine(_paths.PokemonRoot, "social", "activity_feed.json");

    private string UserAchievementsFilePath(Guid userId) =>
        Path.Combine(_paths.PokemonRoot, "achievements", userId.ToString("N"), "achievements.json");

    public async Task RecordActivityAsync(PokemonSocialActivityEvent activityEvent, CancellationToken cancellationToken = default)
    {
        await ActivityLock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var record = await LoadActivityRecordAsync(cancellationToken).ConfigureAwait(false);
            activityEvent.TimestampUtc = _clock.UtcNow;
            record.Events.Insert(0, activityEvent);

            if (record.Events.Count > 200)
            {
                record.Events = record.Events.Take(200).ToList();
            }

            record.UpdatedAtUtc = _clock.UtcNow;
            var path = ActivityFilePath;
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            await JsonFile.WriteAtomicAsync(path, record, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to record social activity event {EventType}.", activityEvent.EventType);
        }
        finally
        {
            ActivityLock.Release();
        }
    }

    public async Task<IReadOnlyList<PokemonSocialActivityEvent>> GetRecentActivityAsync(int limit = 30, CancellationToken cancellationToken = default)
    {
        await ActivityLock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var record = await LoadActivityRecordAsync(cancellationToken).ConfigureAwait(false);
            return record.Events.Take(Math.Max(1, Math.Min(limit, 100))).ToList();
        }
        finally
        {
            ActivityLock.Release();
        }
    }

    public async Task<PokemonAchievementsSummaryDto> GetUserAchievementsAsync(Guid userId, CancellationToken cancellationToken = default)
    {
        var sem = UserLockFor(userId);
        await sem.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var record = await LoadUserAchievementsRecordAsync(userId, cancellationToken).ConfigureAwait(false);
            var list = new List<PokemonAchievementDto>();
            int totalScore = 0;
            int unlockedCount = 0;

            foreach (var def in Definitions)
            {
                var isUnlocked = record.UnlockedAchievements.TryGetValue(def.Id, out var unlockedAt);
                if (isUnlocked)
                {
                    unlockedCount++;
                    totalScore += def.Score;
                }

                list.Add(new PokemonAchievementDto
                {
                    Id = def.Id,
                    Title = def.Title,
                    Description = def.Description,
                    Category = "Pokemon",
                    Rarity = def.Rarity,
                    Score = def.Score,
                    IconName = def.IconName,
                    IsUnlocked = isUnlocked,
                    UnlockedAtUtc = isUnlocked ? unlockedAt : null,
                    CurrentProgress = isUnlocked ? def.MaxProgress : 0,
                    MaxProgress = def.MaxProgress
                });
            }

            return new PokemonAchievementsSummaryDto
            {
                UserId = userId,
                TotalScore = totalScore,
                UnlockedCount = unlockedCount,
                TotalCount = Definitions.Length,
                Achievements = list
            };
        }
        finally
        {
            sem.Release();
        }
    }

    private async Task TryUnlockAsync(Guid userId, string userName, string achievementId, CancellationToken cancellationToken)
    {
        var def = Definitions.FirstOrDefault(d => string.Equals(d.Id, achievementId, StringComparison.OrdinalIgnoreCase));
        if (def == null) return;

        var sem = UserLockFor(userId);
        await sem.WaitAsync(cancellationToken).ConfigureAwait(false);
        bool newlyUnlocked = false;
        try
        {
            var record = await LoadUserAchievementsRecordAsync(userId, cancellationToken).ConfigureAwait(false);
            if (!record.UnlockedAchievements.ContainsKey(achievementId))
            {
                record.UnlockedAchievements[achievementId] = _clock.UtcNow;
                record.UpdatedAtUtc = _clock.UtcNow;
                var path = UserAchievementsFilePath(userId);
                Directory.CreateDirectory(Path.GetDirectoryName(path)!);
                await JsonFile.WriteAtomicAsync(path, record, cancellationToken).ConfigureAwait(false);
                newlyUnlocked = true;
            }
        }
        finally
        {
            sem.Release();
        }

        if (newlyUnlocked)
        {
            _logger.LogInformation("User {UserId} unlocked Pokémon achievement: {Title}", userId, def.Title);

            // Record to social activity feed
            await RecordActivityAsync(new PokemonSocialActivityEvent
            {
                UserId = userId,
                UserName = userName,
                EventType = "AchievementUnlocked",
                Title = $"Badge Unlocked: {def.Title}!",
                Description = $"{userName} earned the \"{def.Title}\" achievement! ({def.Description})"
            }, cancellationToken).ConfigureAwait(false);

            // Dispatch push notification to user devices if available
            if (_pushService != null)
            {
                try
                {
                    var pushPayload = new
                    {
                        type = "achievement_unlock",
                        userId = userId.ToString(),
                        achievementId = def.Id,
                        title = def.Title,
                        description = def.Description,
                        category = "Pokemon",
                        iconName = def.IconName,
                        timestamp = _clock.UtcNow.ToUnixTimeMilliseconds()
                    };
                    await _pushService.SendToUserAsync(userId, pushPayload, cancellationToken).ConfigureAwait(false);
                }
                catch (Exception ex)
                {
                    _logger.LogDebug(ex, "Push notification dispatch skipped for achievement {Title}.", def.Title);
                }
            }
        }
    }

    public async Task ProcessPokemonDepositedAsync(Guid userId, string userName, PokemonSummaryDto pokemon, int totalVaultCount, CancellationToken cancellationToken = default)
    {
        await TryUnlockAsync(userId, userName, "pk-vault-first-deposit", cancellationToken).ConfigureAwait(false);
        if (totalVaultCount >= 50)
        {
            await TryUnlockAsync(userId, userName, "pk-vault-50", cancellationToken).ConfigureAwait(false);
        }

        if (pokemon.IsShiny)
        {
            await ProcessShinyAddedAsync(userId, userName, pokemon, cancellationToken).ConfigureAwait(false);
        }

        await RecordActivityAsync(new PokemonSocialActivityEvent
        {
            UserId = userId,
            UserName = userName,
            EventType = "PokemonDeposited",
            Title = pokemon.IsShiny ? $"✨ Shiny {pokemon.Species} Deposited!" : $"{pokemon.Species} Deposited to Vault",
            Description = $"{userName} safely stored a {(pokemon.IsShiny ? "Shiny " : "")}{pokemon.Species} (Lv. {pokemon.Level}) into their Personal Cloud Vault.",
            SpeciesId = pokemon.SpeciesId,
            SpeciesName = pokemon.Species,
            IsShiny = pokemon.IsShiny
        }, cancellationToken).ConfigureAwait(false);
    }

    public async Task ProcessPokemonWithdrawnAsync(Guid userId, string userName, PokemonSummaryDto pokemon, string destinationGame, CancellationToken cancellationToken = default)
    {
        await TryUnlockAsync(userId, userName, "pk-vault-withdraw", cancellationToken).ConfigureAwait(false);

        await RecordActivityAsync(new PokemonSocialActivityEvent
        {
            UserId = userId,
            UserName = userName,
            EventType = "PokemonWithdrawn",
            Title = $"{pokemon.Species} Deployed for Battle!",
            Description = $"{userName} withdrew {pokemon.Nickname ?? pokemon.Species} to {destinationGame}.",
            SpeciesId = pokemon.SpeciesId,
            SpeciesName = pokemon.Species,
            IsShiny = pokemon.IsShiny
        }, cancellationToken).ConfigureAwait(false);
    }

    public async Task ProcessPokemonTransferredAsync(Guid userId, string userName, PokemonSummaryDto pokemon, string sourceGame, string destinationGame, bool isCrossGen, CancellationToken cancellationToken = default)
    {
        await TryUnlockAsync(userId, userName, "pk-transfer-first", cancellationToken).ConfigureAwait(false);
        if (isCrossGen)
        {
            await TryUnlockAsync(userId, userName, "pk-crossgen-transfer", cancellationToken).ConfigureAwait(false);
        }

        await RecordActivityAsync(new PokemonSocialActivityEvent
        {
            UserId = userId,
            UserName = userName,
            EventType = isCrossGen ? "CrossGenerationTransferCompleted" : "PokemonTransferred",
            Title = isCrossGen ? $"⏳ Era Migration: {pokemon.Species}" : $"{pokemon.Species} Transferred",
            Description = $"{userName} transferred {pokemon.Species} from {sourceGame} to {destinationGame}{(isCrossGen ? " across generations!" : ".")}",
            SpeciesId = pokemon.SpeciesId,
            SpeciesName = pokemon.Species,
            IsShiny = pokemon.IsShiny
        }, cancellationToken).ConfigureAwait(false);
    }

    public async Task ProcessShinyAddedAsync(Guid userId, string userName, PokemonSummaryDto pokemon, CancellationToken cancellationToken = default)
    {
        await TryUnlockAsync(userId, userName, "pk-shiny-first", cancellationToken).ConfigureAwait(false);

        await RecordActivityAsync(new PokemonSocialActivityEvent
        {
            UserId = userId,
            UserName = userName,
            EventType = "ShinyAdded",
            Title = $"✨ Rare Shiny Discovered: {pokemon.Species}!",
            Description = $"{userName} registered a rare Shiny {pokemon.Species} into their collection!",
            SpeciesId = pokemon.SpeciesId,
            SpeciesName = pokemon.Species,
            IsShiny = true
        }, cancellationToken).ConfigureAwait(false);
    }

    public async Task ProcessPokedexMilestoneAsync(Guid userId, string userName, int totalCaught, int gen1Caught, CancellationToken cancellationToken = default)
    {
        if (totalCaught >= 10)
        {
            await TryUnlockAsync(userId, userName, "pk-pokedex-10", cancellationToken).ConfigureAwait(false);
        }
        if (totalCaught >= 50)
        {
            await TryUnlockAsync(userId, userName, "pk-pokedex-50", cancellationToken).ConfigureAwait(false);
        }
        if (gen1Caught >= 151)
        {
            await TryUnlockAsync(userId, userName, "pk-pokedex-kanto-master", cancellationToken).ConfigureAwait(false);
        }
    }

    public async Task ProcessTradeCompletedAsync(Guid userId1, string userName1, Guid userId2, string userName2, PokemonSummaryDto pokemon1, PokemonSummaryDto pokemon2, CancellationToken cancellationToken = default)
    {
        await TryUnlockAsync(userId1, userName1, "pk-trade-first", cancellationToken).ConfigureAwait(false);
        await TryUnlockAsync(userId2, userName2, "pk-trade-first", cancellationToken).ConfigureAwait(false);

        await RecordActivityAsync(new PokemonSocialActivityEvent
        {
            UserId = userId1,
            UserName = userName1,
            EventType = "TradeCompleted",
            Title = $"🤝 Community Trade: {pokemon1.Species} ⇄ {pokemon2.Species}",
            Description = $"{userName1} traded {pokemon1.Species} with {userName2} for {pokemon2.Species}!",
            SpeciesId = pokemon1.SpeciesId,
            SpeciesName = pokemon1.Species,
            IsShiny = pokemon1.IsShiny || pokemon2.IsShiny
        }, cancellationToken).ConfigureAwait(false);
    }

    private async Task<CommunityActivityRecord> LoadActivityRecordAsync(CancellationToken cancellationToken)
    {
        var path = ActivityFilePath;
        if (File.Exists(path))
        {
            var loaded = await JsonFile.ReadAsync<CommunityActivityRecord>(path, cancellationToken).ConfigureAwait(false);
            if (loaded != null) return loaded;
        }
        return new CommunityActivityRecord
        {
            UpdatedAtUtc = _clock.UtcNow,
            Events = new List<PokemonSocialActivityEvent>()
        };
    }

    private async Task<UserPokemonAchievementsRecord> LoadUserAchievementsRecordAsync(Guid userId, CancellationToken cancellationToken)
    {
        var path = UserAchievementsFilePath(userId);
        if (File.Exists(path))
        {
            var loaded = await JsonFile.ReadAsync<UserPokemonAchievementsRecord>(path, cancellationToken).ConfigureAwait(false);
            if (loaded != null) return loaded;
        }
        return new UserPokemonAchievementsRecord
        {
            UserId = userId,
            UpdatedAtUtc = _clock.UtcNow,
            UnlockedAchievements = new Dictionary<string, DateTimeOffset>(StringComparer.OrdinalIgnoreCase)
        };
    }
}

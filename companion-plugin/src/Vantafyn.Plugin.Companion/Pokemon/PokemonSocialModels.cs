using System;
using System.Collections.Generic;

namespace Vantafyn.Plugin.Companion.Pokemon;

public sealed class PokemonSocialActivityEvent
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public Guid UserId { get; set; }
    public string UserName { get; set; } = string.Empty;
    public string EventType { get; set; } = string.Empty; // "PokemonDeposited", "PokemonWithdrawn", "PokemonTransferred", "CrossGenerationTransferCompleted", "ShinyAdded", "PokedexMilestoneReached", "TradeCompleted"
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public int? SpeciesId { get; set; }
    public string? SpeciesName { get; set; }
    public bool IsShiny { get; set; }
    public DateTimeOffset TimestampUtc { get; set; } = DateTimeOffset.UtcNow;
}

public sealed class PokemonAchievementDto
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = "Pokemon";
    public string Rarity { get; set; } = "Common"; // Common, Rare, Epic, Legendary, Mythic
    public int Score { get; set; }
    public string IconName { get; set; } = "catching_pokemon";
    public bool IsUnlocked { get; set; }
    public DateTimeOffset? UnlockedAtUtc { get; set; }
    public int CurrentProgress { get; set; }
    public int MaxProgress { get; set; }
    public double ProgressPercentage => MaxProgress > 0 ? Math.Min(100.0, Math.Round((double)CurrentProgress / MaxProgress * 100, 1)) : (IsUnlocked ? 100.0 : 0.0);
}

public sealed class PokemonAchievementsSummaryDto
{
    public Guid UserId { get; set; }
    public int TotalScore { get; set; }
    public int UnlockedCount { get; set; }
    public int TotalCount { get; set; }
    public List<PokemonAchievementDto> Achievements { get; set; } = new();
}

public sealed class UserPokemonAchievementsRecord
{
    public Guid UserId { get; set; }
    public DateTimeOffset UpdatedAtUtc { get; set; } = DateTimeOffset.UtcNow;
    public Dictionary<string, DateTimeOffset> UnlockedAchievements { get; set; } = new(StringComparer.OrdinalIgnoreCase);
}

public sealed class CommunityActivityRecord
{
    public DateTimeOffset UpdatedAtUtc { get; set; } = DateTimeOffset.UtcNow;
    public List<PokemonSocialActivityEvent> Events { get; set; } = new();
}

public sealed class PokemonPokedexSyncRequest
{
    public List<int> CaughtSpeciesIds { get; set; } = new();
    public List<int> SeenSpeciesIds { get; set; } = new();
    public string? OriginGame { get; set; }
}


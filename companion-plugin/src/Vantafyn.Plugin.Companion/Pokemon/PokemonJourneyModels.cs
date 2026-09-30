using System;
using System.Collections.Generic;

namespace Vantafyn.Plugin.Companion.Pokemon;

public sealed class PokemonJourneyStepDto
{
    public DateTimeOffset Timestamp { get; set; } = DateTimeOffset.UtcNow;
    public string Action { get; set; } = string.Empty; // "Deposit", "Withdraw", "Transfer", "Trade", "Encounter"
    public string? SourceLocation { get; set; }
    public string? DestinationLocation { get; set; }
    public string? Details { get; set; }
    public string? GameTitle { get; set; }
    public int? Generation { get; set; }
}

public sealed class PokemonJourneyDto
{
    public string PokemonId { get; set; } = string.Empty;
    public string Species { get; set; } = string.Empty;
    public int SpeciesId { get; set; }
    public string? Nickname { get; set; }
    public int Level { get; set; }
    public bool IsShiny { get; set; }
    public string? OriginGame { get; set; }
    public string? OriginalTrainer { get; set; }
    public string? OriginalTrainerId { get; set; }
    public List<PokemonJourneyStepDto> Steps { get; set; } = new();
}

public sealed class PokemonPokedexEntryDto
{
    public int SpeciesId { get; set; }
    public string SpeciesName { get; set; } = string.Empty;
    public int Generation { get; set; }
    public bool IsCaught { get; set; }
    public bool IsSeen { get; set; }
    public bool HasShiny { get; set; }
    public string? FirstEncounteredGame { get; set; }
    public DateTimeOffset? FirstEncounteredTimestamp { get; set; }
    public int EncounterCount { get; set; }
}

public sealed class PokemonPokedexGenerationProgressDto
{
    public int Generation { get; set; }
    public string GenerationName { get; set; } = string.Empty; // e.g., "Gen I (Kanto)"
    public int MinDexNumber { get; set; }
    public int MaxDexNumber { get; set; }
    public int TotalSpecies { get; set; }
    public int CaughtCount { get; set; }
    public int SeenCount { get; set; }
    public int ShinyCount { get; set; }
    public double CaughtPercentage => TotalSpecies > 0 ? Math.Round((double)CaughtCount / TotalSpecies * 100, 1) : 0;
}

public sealed class PokemonPokedexDto
{
    public Guid UserId { get; set; }
    public bool IsEnabled { get; set; } = true;
    public int TotalCaught { get; set; }
    public int TotalSeen { get; set; }
    public int TotalShinies { get; set; }
    public DateTimeOffset? LastUpdatedUtc { get; set; }
    public List<PokemonPokedexGenerationProgressDto> GenerationProgress { get; set; } = new();
    public List<PokemonPokedexEntryDto> Entries { get; set; } = new();
}

public sealed class UserPokedexRecord
{
    public Guid UserId { get; set; }
    public DateTimeOffset CreatedAtUtc { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAtUtc { get; set; } = DateTimeOffset.UtcNow;
    public Dictionary<int, PokemonPokedexEntryDto> Entries { get; set; } = new();
}

public sealed class UserJourneysRecord
{
    public Guid UserId { get; set; }
    public DateTimeOffset UpdatedAtUtc { get; set; } = DateTimeOffset.UtcNow;
    public Dictionary<string, PokemonJourneyDto> Journeys { get; set; } = new(StringComparer.OrdinalIgnoreCase);
}

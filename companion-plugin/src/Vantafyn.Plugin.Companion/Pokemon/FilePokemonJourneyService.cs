using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Pokemon;

public sealed class FilePokemonJourneyService : IPokemonJourneyService
{
    private readonly ICompanionPaths _paths;
    private readonly IClock _clock;
    private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> Locks = new();

    private static readonly (int Gen, string Name, int Min, int Max, int Total)[] GenerationBounds =
    [
        (1, "Gen I (Kanto)", 1, 151, 151),
        (2, "Gen II (Johto)", 152, 251, 100),
        (3, "Gen III (Hoenn)", 252, 386, 135),
        (4, "Gen IV (Sinnoh)", 387, 493, 107),
        (5, "Gen V (Unova)", 494, 649, 156),
        (6, "Gen VI (Kalos)", 650, 721, 72),
        (7, "Gen VII (Alola)", 722, 809, 88),
        (8, "Gen VIII (Galar)", 810, 905, 96),
        (9, "Gen IX (Paldea)", 906, 1025, 120),
    ];

    public FilePokemonJourneyService(ICompanionPaths paths, IClock clock)
    {
        _paths = paths;
        _clock = clock;
    }

    private SemaphoreSlim LockFor(Guid userId) =>
        Locks.GetOrAdd(userId, _ => new SemaphoreSlim(1, 1));

    private string PokedexFilePath(Guid userId) =>
        Path.Combine(_paths.PokemonRoot, "pokedex", userId.ToString("N"), "pokedex.json");

    private string JourneysFilePath(Guid userId) =>
        Path.Combine(_paths.PokemonRoot, "journeys", userId.ToString("N"), "journeys.json");

    public static int GetGenerationForSpecies(int speciesId) => speciesId switch
    {
        >= 1 and <= 151 => 1,
        >= 152 and <= 251 => 2,
        >= 252 and <= 386 => 3,
        >= 387 and <= 493 => 4,
        >= 494 and <= 649 => 5,
        >= 650 and <= 721 => 6,
        >= 722 and <= 809 => 7,
        >= 810 and <= 905 => 8,
        >= 906 and <= 1025 => 9,
        _ => 1
    };

    public async Task<PokemonPokedexDto> GetUserPokedexAsync(Guid userId, CancellationToken cancellationToken = default)
    {
        var sem = LockFor(userId);
        await sem.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var record = await LoadPokedexRecordAsync(userId, cancellationToken).ConfigureAwait(false);
            var entriesList = record.Entries.Values.OrderBy(e => e.SpeciesId).ToList();

            var genProgress = GenerationBounds.Select(g =>
            {
                var genEntries = entriesList.Where(e => e.SpeciesId >= g.Min && e.SpeciesId <= g.Max).ToList();
                return new PokemonPokedexGenerationProgressDto
                {
                    Generation = g.Gen,
                    GenerationName = g.Name,
                    MinDexNumber = g.Min,
                    MaxDexNumber = g.Max,
                    TotalSpecies = g.Total,
                    CaughtCount = genEntries.Count(e => e.IsCaught),
                    SeenCount = genEntries.Count(e => e.IsSeen),
                    ShinyCount = genEntries.Count(e => e.HasShiny)
                };
            }).ToList();

            return new PokemonPokedexDto
            {
                UserId = userId,
                IsEnabled = true,
                TotalCaught = entriesList.Count(e => e.IsCaught),
                TotalSeen = entriesList.Count(e => e.IsSeen),
                TotalShinies = entriesList.Count(e => e.HasShiny),
                LastUpdatedUtc = record.UpdatedAtUtc,
                GenerationProgress = genProgress,
                Entries = entriesList
            };
        }
        finally
        {
            sem.Release();
        }
    }

    public async Task<PokemonJourneyDto?> GetPokemonJourneyAsync(Guid userId, string pokemonId, CancellationToken cancellationToken = default)
    {
        var sem = LockFor(userId);
        await sem.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var record = await LoadJourneysRecordAsync(userId, cancellationToken).ConfigureAwait(false);
            if (record.Journeys.TryGetValue(pokemonId, out var journey))
            {
                return journey;
            }
            return null;
        }
        finally
        {
            sem.Release();
        }
    }

    public async Task RecordJourneyStepAsync(Guid userId, string pokemonId, PokemonJourneyStepDto step, PokemonSummaryDto? summary = null, CancellationToken cancellationToken = default)
    {
        var sem = LockFor(userId);
        await sem.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var record = await LoadJourneysRecordAsync(userId, cancellationToken).ConfigureAwait(false);
            if (!record.Journeys.TryGetValue(pokemonId, out var journey))
            {
                journey = new PokemonJourneyDto
                {
                    PokemonId = pokemonId,
                    Species = summary?.Species ?? "Unknown",
                    SpeciesId = summary?.SpeciesId ?? 0,
                    Nickname = summary?.Nickname,
                    Level = summary?.Level ?? 1,
                    IsShiny = summary?.IsShiny ?? false,
                    OriginGame = summary?.OriginGame,
                    OriginalTrainer = summary?.OriginalTrainer,
                    OriginalTrainerId = summary?.OriginalTrainerId
                };
                record.Journeys[pokemonId] = journey;
            }
            else if (summary != null)
            {
                if (!string.IsNullOrEmpty(summary.Species)) journey.Species = summary.Species;
                if (summary.SpeciesId > 0) journey.SpeciesId = summary.SpeciesId;
                if (!string.IsNullOrEmpty(summary.Nickname)) journey.Nickname = summary.Nickname;
                if (summary.Level > 0) journey.Level = summary.Level;
                if (summary.IsShiny) journey.IsShiny = true;
                if (!string.IsNullOrEmpty(summary.OriginGame)) journey.OriginGame = summary.OriginGame;
                if (!string.IsNullOrEmpty(summary.OriginalTrainer)) journey.OriginalTrainer = summary.OriginalTrainer;
                if (!string.IsNullOrEmpty(summary.OriginalTrainerId)) journey.OriginalTrainerId = summary.OriginalTrainerId;
            }

            step.Timestamp = _clock.UtcNow;
            journey.Steps.Add(step);
            record.UpdatedAtUtc = _clock.UtcNow;

            var path = JourneysFilePath(userId);
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            await JsonFile.WriteAtomicAsync(path, record, cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            sem.Release();
        }
    }

    public async Task RecordEncounterAsync(Guid userId, PokemonSummaryDto pokemon, string? gameName, bool isCaught = true, CancellationToken cancellationToken = default)
    {
        await RecordBatchEncountersAsync(userId, new[] { pokemon }, gameName, isCaught, cancellationToken).ConfigureAwait(false);
    }

    public async Task RecordBatchEncountersAsync(Guid userId, IEnumerable<PokemonSummaryDto> pokemons, string? gameName, bool isCaught = true, CancellationToken cancellationToken = default)
    {
        var sem = LockFor(userId);
        await sem.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var record = await LoadPokedexRecordAsync(userId, cancellationToken).ConfigureAwait(false);
            var modified = false;

            foreach (var pokemon in pokemons)
            {
                if (pokemon.SpeciesId <= 0) continue;

                if (!record.Entries.TryGetValue(pokemon.SpeciesId, out var entry))
                {
                    entry = new PokemonPokedexEntryDto
                    {
                        SpeciesId = pokemon.SpeciesId,
                        SpeciesName = !string.IsNullOrWhiteSpace(pokemon.Species) ? pokemon.Species : $"Pokémon #{pokemon.SpeciesId}",
                        Generation = GetGenerationForSpecies(pokemon.SpeciesId),
                        IsSeen = true,
                        IsCaught = isCaught,
                        HasShiny = pokemon.IsShiny,
                        FirstEncounteredGame = gameName ?? pokemon.OriginGame,
                        FirstEncounteredTimestamp = _clock.UtcNow,
                        EncounterCount = 1
                    };
                    record.Entries[pokemon.SpeciesId] = entry;
                    modified = true;
                }
                else
                {
                    var updated = false;
                    if (!entry.IsSeen) { entry.IsSeen = true; updated = true; }
                    if (isCaught && !entry.IsCaught) { entry.IsCaught = true; updated = true; }
                    if (pokemon.IsShiny && !entry.HasShiny) { entry.HasShiny = true; updated = true; }
                    if (string.IsNullOrEmpty(entry.FirstEncounteredGame) && !string.IsNullOrEmpty(gameName ?? pokemon.OriginGame))
                    {
                        entry.FirstEncounteredGame = gameName ?? pokemon.OriginGame;
                        updated = true;
                    }
                    entry.EncounterCount++;
                    if (updated) modified = true;
                }
            }

            if (modified)
            {
                record.UpdatedAtUtc = _clock.UtcNow;
                var path = PokedexFilePath(userId);
                Directory.CreateDirectory(Path.GetDirectoryName(path)!);
                await JsonFile.WriteAtomicAsync(path, record, cancellationToken).ConfigureAwait(false);
            }
        }
        finally
        {
            sem.Release();
        }
    }

    public async Task SynchronizeWithVaultAsync(Guid userId, PokemonVault vault, CancellationToken cancellationToken = default)
    {
        var vaultSummaries = new List<PokemonSummaryDto>();
        foreach (var box in vault.Boxes)
        {
            foreach (var entry in box.Entries)
            {
                if (entry.SpeciesId > 0)
                {
                    vaultSummaries.Add(new PokemonSummaryDto
                    {
                        Id = entry.Id,
                        Species = entry.Species,
                        SpeciesId = entry.SpeciesId,
                        Nickname = entry.Nickname,
                        Level = entry.Level,
                        IsShiny = entry.IsShiny,
                        OriginGame = entry.OriginGame,
                        OriginalTrainer = entry.OriginalTrainer,
                        OriginalTrainerId = entry.OriginalTrainerId,
                        BoxIndex = entry.BoxIndex,
                        SlotIndex = entry.SlotIndex
                    });
                }
            }
        }

        if (vaultSummaries.Count > 0)
        {
            await RecordBatchEncountersAsync(userId, vaultSummaries, "Personal Vault", isCaught: true, cancellationToken).ConfigureAwait(false);
        }
    }

    private async Task<UserPokedexRecord> LoadPokedexRecordAsync(Guid userId, CancellationToken cancellationToken)
    {
        var path = PokedexFilePath(userId);
        if (File.Exists(path))
        {
            var loaded = await JsonFile.ReadAsync<UserPokedexRecord>(path, cancellationToken).ConfigureAwait(false);
            if (loaded != null)
            {
                return loaded;
            }
        }

        return new UserPokedexRecord
        {
            UserId = userId,
            CreatedAtUtc = _clock.UtcNow,
            UpdatedAtUtc = _clock.UtcNow,
            Entries = new Dictionary<int, PokemonPokedexEntryDto>()
        };
    }

    private async Task<UserJourneysRecord> LoadJourneysRecordAsync(Guid userId, CancellationToken cancellationToken)
    {
        var path = JourneysFilePath(userId);
        if (File.Exists(path))
        {
            var loaded = await JsonFile.ReadAsync<UserJourneysRecord>(path, cancellationToken).ConfigureAwait(false);
            if (loaded != null)
            {
                return loaded;
            }
        }

        return new UserJourneysRecord
        {
            UserId = userId,
            UpdatedAtUtc = _clock.UtcNow,
            Journeys = new Dictionary<string, PokemonJourneyDto>(StringComparer.OrdinalIgnoreCase)
        };
    }
}

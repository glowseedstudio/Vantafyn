using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Vantafyn.Plugin.Companion.Pokemon;

public interface IPokemonJourneyService
{
    Task<PokemonPokedexDto> GetUserPokedexAsync(Guid userId, CancellationToken cancellationToken = default);
    Task<PokemonJourneyDto?> GetPokemonJourneyAsync(Guid userId, string pokemonId, CancellationToken cancellationToken = default);
    Task RecordJourneyStepAsync(Guid userId, string pokemonId, PokemonJourneyStepDto step, PokemonSummaryDto? summary = null, CancellationToken cancellationToken = default);
    Task RecordEncounterAsync(Guid userId, PokemonSummaryDto pokemon, string? gameName, bool isCaught = true, CancellationToken cancellationToken = default);
    Task RecordBatchEncountersAsync(Guid userId, IEnumerable<PokemonSummaryDto> pokemons, string? gameName, bool isCaught = true, CancellationToken cancellationToken = default);
    Task RecordSpeciesIdsAsync(Guid userId, IEnumerable<int> caughtSpeciesIds, IEnumerable<int> seenSpeciesIds, string? gameName, CancellationToken cancellationToken = default);
    Task SynchronizeWithVaultAsync(Guid userId, PokemonVault vault, CancellationToken cancellationToken = default);
}

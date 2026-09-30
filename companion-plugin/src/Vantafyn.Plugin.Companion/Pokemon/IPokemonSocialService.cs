using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Vantafyn.Plugin.Companion.Pokemon;

public interface IPokemonSocialService
{
    Task RecordActivityAsync(PokemonSocialActivityEvent activityEvent, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<PokemonSocialActivityEvent>> GetRecentActivityAsync(int limit = 30, CancellationToken cancellationToken = default);
    Task<PokemonAchievementsSummaryDto> GetUserAchievementsAsync(Guid userId, CancellationToken cancellationToken = default);
    Task ProcessPokemonDepositedAsync(Guid userId, string userName, PokemonSummaryDto pokemon, int totalVaultCount, CancellationToken cancellationToken = default);
    Task ProcessPokemonWithdrawnAsync(Guid userId, string userName, PokemonSummaryDto pokemon, string destinationGame, CancellationToken cancellationToken = default);
    Task ProcessPokemonTransferredAsync(Guid userId, string userName, PokemonSummaryDto pokemon, string sourceGame, string destinationGame, bool isCrossGen, CancellationToken cancellationToken = default);
    Task ProcessShinyAddedAsync(Guid userId, string userName, PokemonSummaryDto pokemon, CancellationToken cancellationToken = default);
    Task ProcessPokedexMilestoneAsync(Guid userId, string userName, int totalCaught, int gen1Caught, CancellationToken cancellationToken = default);
    Task ProcessTradeCompletedAsync(Guid userId1, string userName1, Guid userId2, string userName2, PokemonSummaryDto pokemon1, PokemonSummaryDto pokemon2, CancellationToken cancellationToken = default);
}

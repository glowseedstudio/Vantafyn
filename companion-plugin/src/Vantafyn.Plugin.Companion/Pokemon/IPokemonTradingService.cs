namespace Vantafyn.Plugin.Companion.Pokemon;

public interface IPokemonTradingService
{
    Task<PokemonTradeOperationResponse> CreateTradeAsync(
        string userId,
        string userName,
        CreateTradeRequest request,
        CancellationToken cancellationToken = default);

    Task<PokemonTradeOperationResponse> JoinLinkTradeAsync(
        string userId,
        string userName,
        JoinLinkTradeRequest request,
        CancellationToken cancellationToken = default);

    Task<PokemonTradeOperationResponse> AcceptTradeAsync(
        string userId,
        string userName,
        AcceptTradeRequest request,
        CancellationToken cancellationToken = default);

    Task<PokemonTradeOperationResponse> CancelTradeAsync(
        string userId,
        CancelTradeRequest request,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<PokemonTradeSession>> GetPendingTradesAsync(
        string userId,
        CancellationToken cancellationToken = default);

    Task<PokemonTradeSession?> GetTradeAsync(
        string tradeId,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<PokemonTradeSession>> GetTradeHistoryAsync(
        string userId,
        CancellationToken cancellationToken = default);
}

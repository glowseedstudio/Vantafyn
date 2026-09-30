namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Operational context passed into transactional Pokémon operations containing working copies of save data.
/// </summary>
public sealed class PokemonTransactionContext
{
    public string TransactionId { get; set; } = string.Empty;
    public Guid UserId { get; set; }
    public string SourceGameId { get; set; } = string.Empty;
    public string? DestinationGameId { get; set; }
    public byte[] SourceSaveWorkingCopy { get; set; } = [];
    public byte[]? DestinationSaveWorkingCopy { get; set; }
    public List<string> BackupIds { get; } = [];
}

/// <summary>
/// Result envelope for an atomic save transaction.
/// </summary>
public sealed class PokemonTransactionResult<TResult>
{
    public bool Success { get; set; }
    public TResult? Value { get; set; }
    public string? ErrorMessage { get; set; }
    public PokemonTransactionRecord Record { get; set; } = new();
}

/// <summary>
/// Coordinates atomic Pokémon operations with pre-mutation backups and guaranteed rollback on failure.
/// </summary>
public interface IPokemonTransactionManager
{
    /// <summary>
    /// Executes an operation within a safe transaction: acquires exclusive locks, backs up saves,
    /// operates on memory working copies, commits on success, and automatically rolls back if any step fails.
    /// </summary>
    Task<PokemonTransactionResult<TResult>> ExecuteAsync<TResult>(
        Guid userId,
        string operationType,
        string sourceGameId,
        string? destinationGameId,
        string? pokemonId,
        string? species,
        string? nickname,
        Func<PokemonTransactionContext, Task<TResult>> operation,
        CancellationToken cancellationToken);

    /// <summary>
    /// Lists transaction audit history for a user.
    /// </summary>
    Task<IReadOnlyList<PokemonTransactionRecord>> ListHistoryAsync(Guid userId, CancellationToken cancellationToken);

    /// <summary>
    /// Retrieves a specific transaction record by ID.
    /// </summary>
    Task<PokemonTransactionRecord?> GetTransactionAsync(Guid userId, string transactionId, CancellationToken cancellationToken);
}

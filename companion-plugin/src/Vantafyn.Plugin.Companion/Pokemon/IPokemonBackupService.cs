namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Service managing pre-mutation save backups and restoration with SHA256 integrity verification.
/// </summary>
public interface IPokemonBackupService
{
    /// <summary>
    /// Creates a timestamped backup of the current save state prior to an operation.
    /// </summary>
    Task<PokemonBackupMetadata> CreateBackupAsync(
        Guid userId,
        string gameId,
        byte[] saveBytes,
        string reason,
        string? transactionId,
        CancellationToken cancellationToken);

    /// <summary>
    /// Retrieves raw backup binary bytes after verifying SHA256 checksum integrity.
    /// </summary>
    Task<byte[]?> GetBackupBytesAsync(Guid userId, string backupId, CancellationToken cancellationToken);

    /// <summary>
    /// Retrieves metadata for a specific backup belonging to the user.
    /// </summary>
    Task<PokemonBackupMetadata?> GetBackupMetadataAsync(Guid userId, string backupId, CancellationToken cancellationToken);

    /// <summary>
    /// Lists all backups for a user, optionally filtered by game ID.
    /// </summary>
    Task<IReadOnlyList<PokemonBackupMetadata>> ListBackupsAsync(Guid userId, string? gameId, CancellationToken cancellationToken);

    /// <summary>
    /// Restores a backup onto the user's active battery save after verifying integrity.
    /// </summary>
    Task<bool> RestoreBackupAsync(Guid userId, string backupId, CancellationToken cancellationToken);

    /// <summary>
    /// Prunes older backups beyond the retain count for a game, guaranteeing at least one recovery copy is always preserved.
    /// </summary>
    Task<int> PruneOldBackupsAsync(Guid userId, string gameId, int retainCount, CancellationToken cancellationToken);
}

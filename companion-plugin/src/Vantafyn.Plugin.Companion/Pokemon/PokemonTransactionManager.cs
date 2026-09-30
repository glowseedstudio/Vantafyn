using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Orchestrates safe transactional save mutations with exclusive locking, pre-mutation backups,
/// temporary working copies, guaranteed rollback, and audit history persistence.
/// </summary>
public sealed class PokemonTransactionManager : IPokemonTransactionManager
{
    private readonly ICompanionPaths _paths;
    private readonly IClock _clock;
    private readonly IGameSavesService _savesService;
    private readonly ISaveOperationCoordinator _saveCoordinator;
    private readonly IPokemonBackupService _backupService;
    private readonly ILogger<PokemonTransactionManager> _logger;

    public PokemonTransactionManager(
        ICompanionPaths paths,
        IClock clock,
        IGameSavesService savesService,
        ISaveOperationCoordinator saveCoordinator,
        IPokemonBackupService backupService,
        ILogger<PokemonTransactionManager>? logger = null)
    {
        _paths = paths;
        _clock = clock;
        _savesService = savesService;
        _saveCoordinator = saveCoordinator;
        _backupService = backupService;
        _logger = logger ?? NullLogger<PokemonTransactionManager>.Instance;
    }

    public async Task<PokemonTransactionResult<TResult>> ExecuteAsync<TResult>(
        Guid userId,
        string operationType,
        string sourceGameId,
        string? destinationGameId,
        string? pokemonId,
        string? species,
        string? nickname,
        Func<PokemonTransactionContext, Task<TResult>> operation,
        CancellationToken cancellationToken)
    {
        var transactionId = Guid.NewGuid().ToString("N");
        var now = _clock.UtcNow;

        var record = new PokemonTransactionRecord
        {
            TransactionId = transactionId,
            UserId = userId,
            OperationType = operationType,
            SourceGameId = sourceGameId,
            DestinationGameId = destinationGameId,
            PokemonId = pokemonId,
            Species = species,
            Nickname = nickname,
            StartedAtUtc = now,
            Status = PokemonTransactionStatus.Pending
        };

        // 1. Verify source write permission
        var sourceCheck = await _saveCoordinator.CanWriteAsync(userId, sourceGameId, cancellationToken).ConfigureAwait(false);
        if (!sourceCheck.IsAllowed)
        {
            record.Status = PokemonTransactionStatus.Failed;
            record.ErrorMessage = sourceCheck.Reason;
            record.CompletedAtUtc = _clock.UtcNow;
            await AppendHistoryAsync(userId, record, cancellationToken).ConfigureAwait(false);
            return new PokemonTransactionResult<TResult>
            {
                Success = false,
                ErrorMessage = sourceCheck.Reason,
                Record = record
            };
        }

        // 2. Verify destination write permission if multi-save transfer
        if (!string.IsNullOrWhiteSpace(destinationGameId))
        {
            var destCheck = await _saveCoordinator.CanWriteAsync(userId, destinationGameId, cancellationToken).ConfigureAwait(false);
            if (!destCheck.IsAllowed)
            {
                record.Status = PokemonTransactionStatus.Failed;
                record.ErrorMessage = destCheck.Reason;
                record.CompletedAtUtc = _clock.UtcNow;
                await AppendHistoryAsync(userId, record, cancellationToken).ConfigureAwait(false);
                return new PokemonTransactionResult<TResult>
                {
                    Success = false,
                    ErrorMessage = destCheck.Reason,
                    Record = record
                };
            }
        }

        // 3. Acquire exclusive lock on source save
        var sourceLock = await _saveCoordinator.AcquireLockAsync(userId, sourceGameId, operationType, null, cancellationToken).ConfigureAwait(false);
        if (sourceLock == null)
        {
            record.Status = PokemonTransactionStatus.Failed;
            record.ErrorMessage = $"Failed to acquire lock on {sourceGameId}.";
            record.CompletedAtUtc = _clock.UtcNow;
            await AppendHistoryAsync(userId, record, cancellationToken).ConfigureAwait(false);
            return new PokemonTransactionResult<TResult>
            {
                Success = false,
                ErrorMessage = record.ErrorMessage,
                Record = record
            };
        }

        SaveLockHandle? destLock = null;
        if (!string.IsNullOrWhiteSpace(destinationGameId))
        {
            destLock = await _saveCoordinator.AcquireLockAsync(userId, destinationGameId, operationType, null, cancellationToken).ConfigureAwait(false);
            if (destLock == null)
            {
                await sourceLock.DisposeAsync().ConfigureAwait(false);
                record.Status = PokemonTransactionStatus.Failed;
                record.ErrorMessage = $"Failed to acquire lock on destination {destinationGameId}.";
                record.CompletedAtUtc = _clock.UtcNow;
                await AppendHistoryAsync(userId, record, cancellationToken).ConfigureAwait(false);
                return new PokemonTransactionResult<TResult>
                {
                    Success = false,
                    ErrorMessage = record.ErrorMessage,
                    Record = record
                };
            }
        }

        PokemonBackupMetadata? sourceBackup = null;
        PokemonBackupMetadata? destBackup = null;

        try
        {
            // 4. Retrieve source save bytes
            var sourceOriginalBytes = await _savesService.GetAsync(userId, sourceGameId, "sram", cancellationToken).ConfigureAwait(false);
            if (sourceOriginalBytes == null || sourceOriginalBytes.Length == 0)
            {
                throw new InvalidOperationException($"Save bytes missing for source {sourceGameId}.");
            }

            // 5. Create pre-mutation backup of source
            sourceBackup = await _backupService.CreateBackupAsync(
                userId, sourceGameId, sourceOriginalBytes, $"Pre-{operationType} backup", transactionId, cancellationToken).ConfigureAwait(false);
            record.BackupIds.Add(sourceBackup.BackupId);

            byte[]? destOriginalBytes = null;
            if (!string.IsNullOrWhiteSpace(destinationGameId))
            {
                destOriginalBytes = await _savesService.GetAsync(userId, destinationGameId, "sram", cancellationToken).ConfigureAwait(false);
                if (destOriginalBytes == null || destOriginalBytes.Length == 0)
                {
                    throw new InvalidOperationException($"Save bytes missing for destination {destinationGameId}.");
                }

                destBackup = await _backupService.CreateBackupAsync(
                    userId, destinationGameId, destOriginalBytes, $"Pre-{operationType} backup", transactionId, cancellationToken).ConfigureAwait(false);
                record.BackupIds.Add(destBackup.BackupId);
            }

            // 6. Build transaction context with isolated memory working copies
            var context = new PokemonTransactionContext
            {
                TransactionId = transactionId,
                UserId = userId,
                SourceGameId = sourceGameId,
                DestinationGameId = destinationGameId,
                SourceSaveWorkingCopy = (byte[])sourceOriginalBytes.Clone(),
                DestinationSaveWorkingCopy = destOriginalBytes != null ? (byte[])destOriginalBytes.Clone() : null
            };
            context.BackupIds.AddRange(record.BackupIds);

            // 7. Execute operation
            var resultValue = await operation(context).ConfigureAwait(false);

            // 8. Commit changes atomically
            await _savesService.SaveAsync(userId, sourceGameId, "sram", context.SourceSaveWorkingCopy, cancellationToken).ConfigureAwait(false);

            if (!string.IsNullOrWhiteSpace(destinationGameId) && context.DestinationSaveWorkingCopy != null)
            {
                await _savesService.SaveAsync(userId, destinationGameId, "sram", context.DestinationSaveWorkingCopy, cancellationToken).ConfigureAwait(false);
            }

            record.Status = PokemonTransactionStatus.Committed;
            record.CompletedAtUtc = _clock.UtcNow;
            await AppendHistoryAsync(userId, record, cancellationToken).ConfigureAwait(false);

            // Safe pruning (keeps at least 1 backup)
            await _backupService.PruneOldBackupsAsync(userId, sourceGameId, 10, cancellationToken).ConfigureAwait(false);
            if (!string.IsNullOrWhiteSpace(destinationGameId))
            {
                await _backupService.PruneOldBackupsAsync(userId, destinationGameId, 10, cancellationToken).ConfigureAwait(false);
            }

            _logger.LogInformation("Transaction {TransactionId} ({Operation}) committed successfully for user {UserId}",
                transactionId, operationType, userId);

            return new PokemonTransactionResult<TResult>
            {
                Success = true,
                Value = resultValue,
                Record = record
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Transaction {TransactionId} failed during operation. Executing rollback for user {UserId}", transactionId, userId);

            // 9. AUTOMATIC ROLLBACK
            var rollbackSuccessful = true;
            if (sourceBackup != null)
            {
                var restored = await _backupService.RestoreBackupAsync(userId, sourceBackup.BackupId, cancellationToken).ConfigureAwait(false);
                if (!restored) rollbackSuccessful = false;
            }

            if (destBackup != null)
            {
                var restored = await _backupService.RestoreBackupAsync(userId, destBackup.BackupId, cancellationToken).ConfigureAwait(false);
                if (!restored) rollbackSuccessful = false;
            }

            record.Status = rollbackSuccessful ? PokemonTransactionStatus.RolledBack : PokemonTransactionStatus.Failed;
            record.ErrorMessage = ex.Message;
            record.CompletedAtUtc = _clock.UtcNow;
            await AppendHistoryAsync(userId, record, cancellationToken).ConfigureAwait(false);

            return new PokemonTransactionResult<TResult>
            {
                Success = false,
                ErrorMessage = ex.Message,
                Record = record
            };
        }
        finally
        {
            // 10. Always release exclusive locks
            await sourceLock.DisposeAsync().ConfigureAwait(false);
            if (destLock != null)
            {
                await destLock.DisposeAsync().ConfigureAwait(false);
            }
        }
    }

    public async Task<IReadOnlyList<PokemonTransactionRecord>> ListHistoryAsync(Guid userId, CancellationToken cancellationToken)
    {
        var historyPath = UserHistoryFilePath(userId);
        if (!File.Exists(historyPath))
        {
            return [];
        }

        var list = await JsonFile.ReadAsync<List<PokemonTransactionRecord>>(historyPath, cancellationToken).ConfigureAwait(false);
        return list != null ? list.OrderByDescending(t => t.StartedAtUtc).ToList() : [];
    }

    public async Task<PokemonTransactionRecord?> GetTransactionAsync(Guid userId, string transactionId, CancellationToken cancellationToken)
    {
        var list = await ListHistoryAsync(userId, cancellationToken).ConfigureAwait(false);
        return list.FirstOrDefault(t => string.Equals(t.TransactionId, transactionId, StringComparison.OrdinalIgnoreCase));
    }

    private async Task AppendHistoryAsync(Guid userId, PokemonTransactionRecord record, CancellationToken cancellationToken)
    {
        try
        {
            var historyPath = UserHistoryFilePath(userId);
            Directory.CreateDirectory(Path.GetDirectoryName(historyPath)!);

            var list = File.Exists(historyPath)
                ? (await JsonFile.ReadAsync<List<PokemonTransactionRecord>>(historyPath, cancellationToken).ConfigureAwait(false) ?? [])
                : [];

            list.Add(record);
            await JsonFile.WriteAtomicAsync(historyPath, list, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to append history record for transaction {TransactionId}", record.TransactionId);
        }
    }

    private string UserHistoryFilePath(Guid userId) =>
        Path.Combine(_paths.PokemonRoot, "history", userId.ToString("N"), "transactions.json");
}

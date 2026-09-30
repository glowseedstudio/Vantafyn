using System.Collections.Concurrent;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// File-backed save coordinator combining in-process synchronization with on-disk lock persistence.
/// Recovers cleanly from plugin restarts and abandoned lock leases.
/// </summary>
public sealed class FileSaveOperationCoordinator : ISaveOperationCoordinator
{
    private static readonly TimeSpan DefaultLeaseDuration = TimeSpan.FromSeconds(60);
    private readonly ICompanionPaths _paths;
    private readonly IClock _clock;
    private readonly IGameSavesService _savesService;
    private readonly IGameSessionTracker _sessionTracker;
    private readonly ILogger<FileSaveOperationCoordinator> _logger;
    private readonly ConcurrentDictionary<string, SemaphoreSlim> _semaphores = new();

    public FileSaveOperationCoordinator(
        ICompanionPaths paths,
        IClock clock,
        IGameSavesService savesService,
        IGameSessionTracker sessionTracker,
        ILogger<FileSaveOperationCoordinator>? logger = null)
    {
        _paths = paths;
        _clock = clock;
        _savesService = savesService;
        _sessionTracker = sessionTracker;
        _logger = logger ?? NullLogger<FileSaveOperationCoordinator>.Instance;
    }

    public async Task<SaveAccessResult> CanReadAsync(Guid userId, string gameId, CancellationToken cancellationToken)
    {
        var state = await GetSaveStateAsync(userId, gameId, cancellationToken).ConfigureAwait(false);
        return state.CanRead
            ? SaveAccessResult.Allowed(state.State, state.Reason)
            : SaveAccessResult.Denied(state.State, state.Reason);
    }

    public async Task<SaveAccessResult> CanWriteAsync(Guid userId, string gameId, CancellationToken cancellationToken)
    {
        var state = await GetSaveStateAsync(userId, gameId, cancellationToken).ConfigureAwait(false);
        return state.CanWrite
            ? SaveAccessResult.Allowed(state.State, state.Reason)
            : SaveAccessResult.Denied(state.State, state.Reason);
    }

    public async Task<SaveOperationStateInfo> GetSaveStateAsync(Guid userId, string gameId, CancellationToken cancellationToken)
    {
        var normalizedId = NormalizeGameId(gameId);

        // 1. Verify save exists on server
        var saveBytes = await _savesService.GetAsync(userId, normalizedId, "sram", cancellationToken).ConfigureAwait(false);
        if (saveBytes == null || saveBytes.Length == 0)
        {
            return new SaveOperationStateInfo
            {
                GameId = normalizedId,
                State = SaveOperationState.Missing,
                CanRead = false,
                CanWrite = false,
                Reason = "Save file not found."
            };
        }

        // 2. Verify active game session
        var activeSession = _sessionTracker.GetActiveSession(userId, normalizedId);
        if (activeSession != null)
        {
            return new SaveOperationStateInfo
            {
                GameId = normalizedId,
                State = SaveOperationState.GameActive,
                CanRead = true,
                CanWrite = false,
                Reason = $"Finish and exit {normalizedId} before modifying this save.",
                ActiveSession = activeSession
            };
        }

        // 3. Verify operation lock
        var activeLock = await GetActiveLockAsync(userId, normalizedId, cancellationToken).ConfigureAwait(false);
        if (activeLock != null)
        {
            return new SaveOperationStateInfo
            {
                GameId = normalizedId,
                State = SaveOperationState.OperationLocked,
                CanRead = false,
                CanWrite = false,
                Reason = $"Save is currently locked for operation: {activeLock.Operation}.",
                ActiveLock = activeLock
            };
        }

        return new SaveOperationStateInfo
        {
            GameId = normalizedId,
            State = SaveOperationState.Available,
            CanRead = true,
            CanWrite = true,
            Reason = "Save is available for operations."
        };
    }

    public async Task<SaveLockHandle?> AcquireLockAsync(
        Guid userId,
        string gameId,
        string operation,
        TimeSpan? leaseDuration,
        CancellationToken cancellationToken)
    {
        var normalizedId = NormalizeGameId(gameId);
        var key = LockKey(userId, normalizedId);
        var sem = _semaphores.GetOrAdd(key, _ => new SemaphoreSlim(1, 1));

        await sem.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            // Re-verify current state while holding the lock
            var state = await GetSaveStateAsync(userId, normalizedId, cancellationToken).ConfigureAwait(false);
            if (!state.CanWrite)
            {
                _logger.LogWarning("Failed to acquire lock for user {UserId} game {GameId}: {Reason}", userId, normalizedId, state.Reason);
                return null;
            }

            var now = _clock.UtcNow;
            var duration = leaseDuration ?? DefaultLeaseDuration;
            var record = new SaveLockRecord
            {
                LockId = Guid.NewGuid().ToString("N"),
                UserId = userId,
                GameId = normalizedId,
                Operation = operation,
                AcquiredAtUtc = now,
                ExpiresAtUtc = now.Add(duration)
            };

            var lockPath = LockFilePath(userId, normalizedId);
            Directory.CreateDirectory(LocksDirectory);
            await JsonFile.WriteAtomicAsync(lockPath, record, cancellationToken).ConfigureAwait(false);

            _logger.LogInformation("Acquired save lock {LockId} for user {UserId} game {GameId} ({Operation}) until {Expires}",
                record.LockId, userId, normalizedId, operation, record.ExpiresAtUtc);

            return new SaveLockHandle(
                record.LockId,
                userId,
                normalizedId,
                operation,
                record.AcquiredAtUtc,
                record.ExpiresAtUtc,
                async handle => await ReleaseLockInternalAsync(handle).ConfigureAwait(false));
        }
        finally
        {
            sem.Release();
        }
    }

    public async Task<bool> ReleaseLockAsync(SaveLockHandle handle, CancellationToken cancellationToken)
    {
        return await ReleaseLockInternalAsync(handle).ConfigureAwait(false);
    }

    private async Task<bool> ReleaseLockInternalAsync(SaveLockHandle handle)
    {
        var key = LockKey(handle.UserId, handle.GameId);
        var sem = _semaphores.GetOrAdd(key, _ => new SemaphoreSlim(1, 1));

        await sem.WaitAsync().ConfigureAwait(false);
        try
        {
            var lockPath = LockFilePath(handle.UserId, handle.GameId);
            if (!File.Exists(lockPath))
            {
                return true;
            }

            var record = await JsonFile.ReadAsync<SaveLockRecord>(lockPath, CancellationToken.None).ConfigureAwait(false);
            if (record != null && record.LockId != handle.LockId)
            {
                // Another lock was acquired after expiration
                _logger.LogWarning("Lock mismatch releasing {LockId}: found {CurrentLockId}", handle.LockId, record.LockId);
                return false;
            }

            File.Delete(lockPath);
            _logger.LogInformation("Released save lock {LockId} for user {UserId} game {GameId}", handle.LockId, handle.UserId, handle.GameId);
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error releasing save lock {LockId}", handle.LockId);
            return false;
        }
        finally
        {
            sem.Release();
        }
    }

    private async Task<SaveLockRecord?> GetActiveLockAsync(Guid userId, string gameId, CancellationToken cancellationToken)
    {
        var lockPath = LockFilePath(userId, gameId);
        if (!File.Exists(lockPath))
        {
            return null;
        }

        try
        {
            var record = await JsonFile.ReadAsync<SaveLockRecord>(lockPath, cancellationToken).ConfigureAwait(false);
            if (record == null)
            {
                return null;
            }

            if (_clock.UtcNow <= record.ExpiresAtUtc)
            {
                return record;
            }

            // Lock has expired: recover safely
            _logger.LogWarning("Abandoned save lock {LockId} for user {UserId} game {GameId} has expired at {ExpiresAt} and is being recovered.",
                record.LockId, userId, gameId, record.ExpiresAtUtc);

            File.Delete(lockPath);
            return null;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error reading lock record at {Path}", lockPath);
            return null;
        }
    }

    private string LocksDirectory => Path.Combine(_paths.PokemonRoot, "locks");

    private string LockFilePath(Guid userId, string gameId) =>
        Path.Combine(LocksDirectory, $"{userId:N}_{Sanitize(gameId)}.lock.json");

    private static string LockKey(Guid userId, string gameId) => $"{userId:N}:{NormalizeGameId(gameId)}";

    private static string NormalizeGameId(string gameId) => gameId.Trim().ToLowerInvariant();

    private static string Sanitize(string value)
    {
        var invalid = Path.GetInvalidFileNameChars();
        var chars = value.Where(c => Array.IndexOf(invalid, c) < 0 && c != '.' && c != '/' && c != '\\').ToArray();
        return new string(chars);
    }
}

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// RAII lock handle for exclusive save file operations.
/// Safely releases the lock on disposal.
/// </summary>
public sealed class SaveLockHandle : IAsyncDisposable, IDisposable
{
    private readonly Func<SaveLockHandle, Task> _releaseAction;
    private int _disposed;

    public string LockId { get; }
    public Guid UserId { get; }
    public string GameId { get; }
    public string Operation { get; }
    public DateTimeOffset AcquiredAtUtc { get; }
    public DateTimeOffset ExpiresAtUtc { get; }

    public SaveLockHandle(
        string lockId,
        Guid userId,
        string gameId,
        string operation,
        DateTimeOffset acquiredAtUtc,
        DateTimeOffset expiresAtUtc,
        Func<SaveLockHandle, Task> releaseAction)
    {
        LockId = lockId;
        UserId = userId;
        GameId = gameId;
        Operation = operation;
        AcquiredAtUtc = acquiredAtUtc;
        ExpiresAtUtc = expiresAtUtc;
        _releaseAction = releaseAction;
    }

    public async ValueTask DisposeAsync()
    {
        if (Interlocked.Exchange(ref _disposed, 1) == 0)
        {
            await _releaseAction(this).ConfigureAwait(false);
        }
    }

    public void Dispose()
    {
        DisposeAsync().AsTask().GetAwaiter().GetResult();
    }
}

/// <summary>
/// Central coordinator managing save file states, active game conflicts, and exclusive mutation locking.
/// </summary>
public interface ISaveOperationCoordinator
{
    /// <summary>
    /// Checks whether reading the save is permitted.
    /// </summary>
    Task<SaveAccessResult> CanReadAsync(Guid userId, string gameId, CancellationToken cancellationToken);

    /// <summary>
    /// Checks whether modifying/mutating the save is permitted.
    /// </summary>
    Task<SaveAccessResult> CanWriteAsync(Guid userId, string gameId, CancellationToken cancellationToken);

    /// <summary>
    /// Attempts to acquire an exclusive mutation lock on a save. Returns null if already locked or game is running.
    /// </summary>
    Task<SaveLockHandle?> AcquireLockAsync(Guid userId, string gameId, string operation, TimeSpan? leaseDuration, CancellationToken cancellationToken);

    /// <summary>
    /// Releases an existing lock handle.
    /// </summary>
    Task<bool> ReleaseLockAsync(SaveLockHandle handle, CancellationToken cancellationToken);

    /// <summary>
    /// Gets detailed operational state info for a save file.
    /// </summary>
    Task<SaveOperationStateInfo> GetSaveStateAsync(Guid userId, string gameId, CancellationToken cancellationToken);
}

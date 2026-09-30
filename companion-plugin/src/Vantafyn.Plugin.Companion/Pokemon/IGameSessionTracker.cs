namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Tracks active emulator play sessions to prevent save mutations while games are running.
/// </summary>
public interface IGameSessionTracker
{
    /// <summary>
    /// Checks whether an unexpired active play session exists for the given user and game.
    /// </summary>
    bool IsGameActive(Guid userId, string gameId);

    /// <summary>
    /// Retrieves active play session metadata if currently running.
    /// </summary>
    ActiveGameSession? GetActiveSession(Guid userId, string gameId);

    /// <summary>
    /// Registers the start of a play session.
    /// </summary>
    ActiveGameSession StartSession(Guid userId, string gameId, string? deviceId = null, TimeSpan? leaseDuration = null);

    /// <summary>
    /// Updates heartbeat for an ongoing session to extend its validity lease.
    /// </summary>
    bool HeartbeatSession(Guid userId, string gameId, TimeSpan? extendBy = null);

    /// <summary>
    /// Terminates an active play session upon game exit.
    /// </summary>
    bool EndSession(Guid userId, string gameId);

    /// <summary>
    /// Lists all active sessions for a user.
    /// </summary>
    IReadOnlyList<ActiveGameSession> GetActiveSessions(Guid userId);
}

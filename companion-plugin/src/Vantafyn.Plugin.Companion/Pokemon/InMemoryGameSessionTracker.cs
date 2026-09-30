using System.Collections.Concurrent;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// In-memory tracker for active emulator sessions with automatic lease expiration.
/// </summary>
public sealed class InMemoryGameSessionTracker(IClock clock) : IGameSessionTracker
{
    private static readonly TimeSpan DefaultLease = TimeSpan.FromMinutes(2);
    private readonly ConcurrentDictionary<(Guid UserId, string GameId), ActiveGameSession> _sessions = new();

    public bool IsGameActive(Guid userId, string gameId)
    {
        return GetActiveSession(userId, gameId) != null;
    }

    public ActiveGameSession? GetActiveSession(Guid userId, string gameId)
    {
        var key = (userId, NormalizeGameId(gameId));
        if (_sessions.TryGetValue(key, out var session))
        {
            if (clock.UtcNow <= session.ExpiresAtUtc)
            {
                return session;
            }

            // Prune expired session
            _sessions.TryRemove(key, out _);
        }

        return null;
    }

    public ActiveGameSession StartSession(Guid userId, string gameId, string? deviceId = null, TimeSpan? leaseDuration = null)
    {
        var now = clock.UtcNow;
        var duration = leaseDuration ?? DefaultLease;
        var key = (userId, NormalizeGameId(gameId));

        var session = new ActiveGameSession
        {
            GameId = NormalizeGameId(gameId),
            UserId = userId,
            DeviceId = deviceId,
            StartedAtUtc = now,
            LastHeartbeatUtc = now,
            ExpiresAtUtc = now.Add(duration)
        };

        _sessions[key] = session;
        return session;
    }

    public bool HeartbeatSession(Guid userId, string gameId, TimeSpan? extendBy = null)
    {
        var key = (userId, NormalizeGameId(gameId));
        if (_sessions.TryGetValue(key, out var session))
        {
            var now = clock.UtcNow;
            var duration = extendBy ?? DefaultLease;
            session.LastHeartbeatUtc = now;
            session.ExpiresAtUtc = now.Add(duration);
            return true;
        }

        return false;
    }

    public bool EndSession(Guid userId, string gameId)
    {
        var key = (userId, NormalizeGameId(gameId));
        return _sessions.TryRemove(key, out _);
    }

    public IReadOnlyList<ActiveGameSession> GetActiveSessions(Guid userId)
    {
        var now = clock.UtcNow;
        var active = new List<ActiveGameSession>();

        foreach (var kvp in _sessions)
        {
            if (kvp.Key.UserId == userId)
            {
                if (now <= kvp.Value.ExpiresAtUtc)
                {
                    active.Add(kvp.Value);
                }
                else
                {
                    _sessions.TryRemove(kvp.Key, out _);
                }
            }
        }

        return active;
    }

    private static string NormalizeGameId(string gameId) => gameId.Trim().ToLowerInvariant();
}

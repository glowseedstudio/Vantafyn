using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Operational states for a game save file.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter))]
public enum SaveOperationState
{
    Available,
    GameActive,
    OperationLocked,
    Missing,
    Unknown,
    Error
}

/// <summary>
/// Structured result determining whether an operation (read/write) is permitted.
/// </summary>
public sealed class SaveAccessResult
{
    [JsonPropertyName("isAllowed")]
    public bool IsAllowed { get; set; }

    [JsonPropertyName("state")]
    public SaveOperationState State { get; set; }

    [JsonPropertyName("reason")]
    public string Reason { get; set; } = string.Empty;

    public static SaveAccessResult Allowed(SaveOperationState state = SaveOperationState.Available, string reason = "Save is available for operation.") =>
        new() { IsAllowed = true, State = state, Reason = reason };

    public static SaveAccessResult Denied(SaveOperationState state, string reason) =>
        new() { IsAllowed = false, State = state, Reason = reason };
}

/// <summary>
/// Information about an active game play session.
/// </summary>
public sealed class ActiveGameSession
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("userId")]
    public Guid UserId { get; set; }

    [JsonPropertyName("deviceId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? DeviceId { get; set; }

    [JsonPropertyName("startedAtUtc")]
    public DateTimeOffset StartedAtUtc { get; set; }

    [JsonPropertyName("lastHeartbeatUtc")]
    public DateTimeOffset LastHeartbeatUtc { get; set; }

    [JsonPropertyName("expiresAtUtc")]
    public DateTimeOffset ExpiresAtUtc { get; set; }
}

/// <summary>
/// Metadata for an active exclusive operation lock on a save file.
/// Persisted to disk to survive plugin restarts and facilitate automatic expiration recovery.
/// </summary>
public sealed class SaveLockRecord
{
    [JsonPropertyName("lockId")]
    public string LockId { get; set; } = Guid.NewGuid().ToString("N");

    [JsonPropertyName("userId")]
    public Guid UserId { get; set; }

    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("operation")]
    public string Operation { get; set; } = string.Empty;

    [JsonPropertyName("acquiredAtUtc")]
    public DateTimeOffset AcquiredAtUtc { get; set; }

    [JsonPropertyName("expiresAtUtc")]
    public DateTimeOffset ExpiresAtUtc { get; set; }
}

/// <summary>
/// Comprehensive save state report exposed to clients.
/// </summary>
public sealed class SaveOperationStateInfo
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("state")]
    public SaveOperationState State { get; set; }

    [JsonPropertyName("canRead")]
    public bool CanRead { get; set; }

    [JsonPropertyName("canWrite")]
    public bool CanWrite { get; set; }

    [JsonPropertyName("reason")]
    public string Reason { get; set; } = string.Empty;

    [JsonPropertyName("activeSession")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public ActiveGameSession? ActiveSession { get; set; }

    [JsonPropertyName("activeLock")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public SaveLockRecord? ActiveLock { get; set; }
}

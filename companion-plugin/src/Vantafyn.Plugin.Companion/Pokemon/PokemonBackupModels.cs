using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Status of a transactional Pokémon save mutation.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter))]
public enum PokemonTransactionStatus
{
    Pending,
    Committed,
    RolledBack,
    Failed
}

/// <summary>
/// Metadata describing a pre-mutation battery save backup.
/// Persisted alongside the raw backup binary at {PokemonRoot}/backups/{userId:N}/{gameId}/{backupId}.meta.json.
/// </summary>
public sealed class PokemonBackupMetadata
{
    [JsonPropertyName("backupId")]
    public string BackupId { get; set; } = Guid.NewGuid().ToString("N");

    [JsonPropertyName("userId")]
    public Guid UserId { get; set; }

    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("createdAtUtc")]
    public DateTimeOffset CreatedAtUtc { get; set; }

    [JsonPropertyName("reason")]
    public string Reason { get; set; } = string.Empty;

    [JsonPropertyName("checksumSha256")]
    public string ChecksumSha256 { get; set; } = string.Empty;

    [JsonPropertyName("sizeBytes")]
    public long SizeBytes { get; set; }

    [JsonPropertyName("transactionId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? TransactionId { get; set; }

    [JsonPropertyName("isRestored")]
    public bool IsRestored { get; set; }

    [JsonPropertyName("restoredAtUtc")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public DateTimeOffset? RestoredAtUtc { get; set; }

    public PokemonBackupDto ToDto() => new()
    {
        BackupId = BackupId,
        GameId = GameId,
        CreatedAtUtc = CreatedAtUtc,
        Reason = Reason,
        SizeBytes = SizeBytes,
        IsRestored = IsRestored,
        RestoredAtUtc = RestoredAtUtc
    };
}

/// <summary>
/// Client-facing representation of a save backup (zero filesystem paths leaked).
/// </summary>
public sealed class PokemonBackupDto
{
    [JsonPropertyName("backupId")]
    public string BackupId { get; set; } = string.Empty;

    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("createdAtUtc")]
    public DateTimeOffset CreatedAtUtc { get; set; }

    [JsonPropertyName("reason")]
    public string Reason { get; set; } = string.Empty;

    [JsonPropertyName("sizeBytes")]
    public long SizeBytes { get; set; }

    [JsonPropertyName("isRestored")]
    public bool IsRestored { get; set; }

    [JsonPropertyName("restoredAtUtc")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public DateTimeOffset? RestoredAtUtc { get; set; }
}

/// <summary>
/// Audit log record for a Pokémon operation or transfer.
/// </summary>
public sealed class PokemonTransactionRecord
{
    [JsonPropertyName("transactionId")]
    public string TransactionId { get; set; } = Guid.NewGuid().ToString("N");

    [JsonPropertyName("userId")]
    public Guid UserId { get; set; }

    [JsonPropertyName("operationType")]
    public string OperationType { get; set; } = string.Empty;

    [JsonPropertyName("sourceGameId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? SourceGameId { get; set; }

    [JsonPropertyName("destinationGameId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? DestinationGameId { get; set; }

    [JsonPropertyName("pokemonId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? PokemonId { get; set; }

    [JsonPropertyName("species")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Species { get; set; }

    [JsonPropertyName("nickname")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Nickname { get; set; }

    [JsonPropertyName("startedAtUtc")]
    public DateTimeOffset StartedAtUtc { get; set; }

    [JsonPropertyName("completedAtUtc")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public DateTimeOffset? CompletedAtUtc { get; set; }

    [JsonPropertyName("status")]
    public PokemonTransactionStatus Status { get; set; } = PokemonTransactionStatus.Pending;

    [JsonPropertyName("backupIds")]
    public List<string> BackupIds { get; set; } = [];

    [JsonPropertyName("errorMessage")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? ErrorMessage { get; set; }

    public PokemonTransactionDto ToDto() => new()
    {
        TransactionId = TransactionId,
        OperationType = OperationType,
        SourceGameId = SourceGameId,
        DestinationGameId = DestinationGameId,
        PokemonId = PokemonId,
        Species = Species,
        Nickname = Nickname,
        StartedAtUtc = StartedAtUtc,
        CompletedAtUtc = CompletedAtUtc,
        Status = Status,
        ErrorMessage = ErrorMessage
    };
}

/// <summary>
/// Client-facing summary of a transaction.
/// </summary>
public sealed class PokemonTransactionDto
{
    [JsonPropertyName("transactionId")]
    public string TransactionId { get; set; } = string.Empty;

    [JsonPropertyName("operationType")]
    public string OperationType { get; set; } = string.Empty;

    [JsonPropertyName("sourceGameId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? SourceGameId { get; set; }

    [JsonPropertyName("destinationGameId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? DestinationGameId { get; set; }

    [JsonPropertyName("pokemonId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? PokemonId { get; set; }

    [JsonPropertyName("species")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Species { get; set; }

    [JsonPropertyName("nickname")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Nickname { get; set; }

    [JsonPropertyName("startedAtUtc")]
    public DateTimeOffset StartedAtUtc { get; set; }

    [JsonPropertyName("completedAtUtc")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public DateTimeOffset? CompletedAtUtc { get; set; }

    [JsonPropertyName("status")]
    public PokemonTransactionStatus Status { get; set; }

    [JsonPropertyName("errorMessage")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? ErrorMessage { get; set; }
}

/// <summary>
/// Request to simulate an operation and verify transactional rollback.
/// </summary>
public sealed class PokemonTransactionSimulationRequest
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("shouldFail")]
    public bool ShouldFail { get; set; } = true;

    [JsonPropertyName("simulatedOperation")]
    public string SimulatedOperation { get; set; } = "Simulated Mutation";
}

/// <summary>
/// Verification report from a simulated transaction test.
/// </summary>
public sealed class PokemonTransactionSimulationResult
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    [JsonPropertyName("transactionId")]
    public string TransactionId { get; set; } = string.Empty;

    [JsonPropertyName("status")]
    public PokemonTransactionStatus Status { get; set; }

    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    [JsonPropertyName("saveChecksumBefore")]
    public string SaveChecksumBefore { get; set; } = string.Empty;

    [JsonPropertyName("saveChecksumAfter")]
    public string SaveChecksumAfter { get; set; } = string.Empty;

    [JsonPropertyName("rolledBack")]
    public bool RolledBack { get; set; }
}

using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Request to deposit a Pokémon from a game save into the user's personal vault.
/// </summary>
public sealed class PokemonDepositRequest
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("pokemonId")]
    public string PokemonId { get; set; } = string.Empty;

    [JsonPropertyName("isInParty")]
    public bool IsInParty { get; set; }

    [JsonPropertyName("boxIndex")]
    public int? BoxIndex { get; set; }

    [JsonPropertyName("slotIndex")]
    public int SlotIndex { get; set; } = 1;

    [JsonPropertyName("targetVaultBoxIndex")]
    public int TargetVaultBoxIndex { get; set; } = 1;

    [JsonPropertyName("targetVaultSlotIndex")]
    public int? TargetVaultSlotIndex { get; set; }
}

/// <summary>Result of copying a user-selected external emulator save into the personal vault.</summary>
public sealed class PokemonExternalSaveImportResponse
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    [JsonPropertyName("importedCount")]
    public int ImportedCount { get; set; }

    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    [JsonPropertyName("trainerName")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? TrainerName { get; set; }
}

/// <summary>A short-lived, user-scoped preview of an external emulator save.</summary>
public sealed class PokemonExternalSavePreview
{
    [JsonPropertyName("previewId")]
    public string PreviewId { get; set; } = string.Empty;

    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    [JsonPropertyName("trainerName")]
    public string? TrainerName { get; set; }

    [JsonPropertyName("party")]
    public IReadOnlyList<PokemonSummaryDto> Party { get; set; } = Array.Empty<PokemonSummaryDto>();

    [JsonPropertyName("boxes")]
    public IReadOnlyList<PokemonBoxDto> Boxes { get; set; } = Array.Empty<PokemonBoxDto>();
}

public sealed class PokemonExternalSaveCommitRequest
{
    [JsonPropertyName("previewId")]
    public string PreviewId { get; set; } = string.Empty;

    [JsonPropertyName("pokemonIds")]
    public List<string> PokemonIds { get; set; } = [];
}

/// <summary>
/// Request to withdraw a Pokémon from the user's personal vault back into a compatible game save.
/// </summary>
public sealed class PokemonWithdrawRequest
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("vaultEntryId")]
    public string VaultEntryId { get; set; } = string.Empty;

    [JsonPropertyName("targetBoxIndex")]
    public int? TargetBoxIndex { get; set; }

    [JsonPropertyName("targetSlotIndex")]
    public int? TargetSlotIndex { get; set; }

    [JsonPropertyName("targetParty")]
    public bool TargetParty { get; set; }
}

/// <summary>
/// Response payload for a completed or rolled-back Pokémon deposit/withdraw operation.
/// </summary>
public sealed class PokemonOperationResponse
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    [JsonPropertyName("transactionId")]
    public string TransactionId { get; set; } = string.Empty;

    [JsonPropertyName("operation")]
    public string Operation { get; set; } = string.Empty;

    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    [JsonPropertyName("sourceLocation")]
    public string SourceLocation { get; set; } = string.Empty;

    [JsonPropertyName("destinationLocation")]
    public string DestinationLocation { get; set; } = string.Empty;

    [JsonPropertyName("backupId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? BackupId { get; set; }

    [JsonPropertyName("vaultEntry")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public PokemonVaultEntry? VaultEntry { get; set; }

    [JsonPropertyName("pokemon")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public PokemonSummaryDto? Pokemon { get; set; }

    [JsonPropertyName("sourceGameId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? SourceGameId { get; set; }

    [JsonPropertyName("destinationGameId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? DestinationGameId { get; set; }

    [JsonPropertyName("isCrossGeneration")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public bool? IsCrossGeneration { get; set; }

    [JsonPropertyName("warnings")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public List<string>? Warnings { get; set; }
}

/// <summary>
/// Request to directly transfer a Pokémon from one game save to another.
/// </summary>
public sealed class PokemonDirectTransferRequest
{
    [JsonPropertyName("sourceGameId")]
    public string SourceGameId { get; set; } = string.Empty;

    [JsonPropertyName("destinationGameId")]
    public string DestinationGameId { get; set; } = string.Empty;

    [JsonPropertyName("pokemonId")]
    public string PokemonId { get; set; } = string.Empty;

    [JsonPropertyName("sourceIsInParty")]
    public bool SourceIsInParty { get; set; }

    [JsonPropertyName("sourceBoxIndex")]
    public int? SourceBoxIndex { get; set; }

    [JsonPropertyName("sourceSlotIndex")]
    public int SourceSlotIndex { get; set; } = 1;

    [JsonPropertyName("targetBoxIndex")]
    public int? TargetBoxIndex { get; set; }

    [JsonPropertyName("targetSlotIndex")]
    public int? TargetSlotIndex { get; set; }

    [JsonPropertyName("targetParty")]
    public bool TargetParty { get; set; }
}

/// <summary>
/// Pre-flight transfer compatibility validation request.
/// </summary>
public sealed class PokemonTransferValidateRequest
{
    [JsonPropertyName("sourceGameId")]
    public string SourceGameId { get; set; } = string.Empty;

    [JsonPropertyName("destinationGameId")]
    public string DestinationGameId { get; set; } = string.Empty;

    [JsonPropertyName("pokemonId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? PokemonId { get; set; }
}

/// <summary>
/// Result of a transfer compatibility validation.
/// </summary>
public sealed class PokemonTransferCompatibilityResult
{
    [JsonPropertyName("isCompatible")]
    public bool IsCompatible { get; set; }

    [JsonPropertyName("isCrossGeneration")]
    public bool IsCrossGeneration { get; set; }

    [JsonPropertyName("sourceGeneration")]
    public int SourceGeneration { get; set; }

    [JsonPropertyName("destinationGeneration")]
    public int DestinationGeneration { get; set; }

    [JsonPropertyName("direction")]
    public string Direction { get; set; } = "same_generation";

    [JsonPropertyName("reason")]
    public string Reason { get; set; } = string.Empty;

    [JsonPropertyName("warnings")]
    public List<string> Warnings { get; set; } = [];
}

/// <summary>
/// Result returned by IPokemonProvider when extracting a Pokémon from a save.
/// </summary>
public sealed class PokemonExtractResult
{
    [JsonPropertyName("isSuccess")]
    public bool IsSuccess { get; set; }

    [JsonPropertyName("errorMessage")]
    public string? ErrorMessage { get; set; }

    [JsonPropertyName("updatedSaveBytes")]
    public byte[]? UpdatedSaveBytes { get; set; }

    [JsonPropertyName("extractedPokemon")]
    public PokemonDetailsDto? ExtractedPokemon { get; set; }
}

/// <summary>
/// Result returned by IPokemonProvider when injecting a Pokémon into a save.
/// </summary>
public sealed class PokemonInjectResult
{
    [JsonPropertyName("isSuccess")]
    public bool IsSuccess { get; set; }

    [JsonPropertyName("errorMessage")]
    public string? ErrorMessage { get; set; }

    [JsonPropertyName("updatedSaveBytes")]
    public byte[]? UpdatedSaveBytes { get; set; }

    [JsonPropertyName("assignedLocation")]
    public string? AssignedLocation { get; set; }
}

/// <summary>
/// Request to restore a save file from a previously captured backup.
/// </summary>
public sealed class RestoreBackupRequest
{
    [JsonPropertyName("backupId")]
    public string BackupId { get; set; } = string.Empty;

    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;
}

/// <summary>
/// Response payload for a backup restore operation.
/// </summary>
public sealed class RestoreBackupResponse
{
    [JsonPropertyName("isSuccess")]
    public bool IsSuccess { get; set; }

    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    [JsonPropertyName("backupId")]
    public string? BackupId { get; set; }

    [JsonPropertyName("gameId")]
    public string? GameId { get; set; }
}

/// <summary>
/// Operational diagnostics and health metrics for the Pokémon integration subsystem.
/// </summary>
public sealed class PokemonDiagnosticsDto
{
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    [JsonPropertyName("providerType")]
    public string ProviderType { get; set; } = string.Empty;

    [JsonPropertyName("providerHealthy")]
    public bool ProviderHealthy { get; set; }

    [JsonPropertyName("providerMessage")]
    public string? ProviderMessage { get; set; }

    [JsonPropertyName("supportedGenerations")]
    public IReadOnlyList<string> SupportedGenerations { get; set; } = [];

    [JsonPropertyName("totalStoredPokemon")]
    public int TotalStoredPokemon { get; set; }

    [JsonPropertyName("totalShinyPokemon")]
    public int TotalShinyPokemon { get; set; }

    [JsonPropertyName("totalBackups")]
    public int TotalBackups { get; set; }

    [JsonPropertyName("activeSessions")]
    public int ActiveSessions { get; set; }

    [JsonPropertyName("activeTrades")]
    public int ActiveTrades { get; set; }
}

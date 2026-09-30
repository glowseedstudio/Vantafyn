using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Abstraction for a Pokémon manipulation provider (e.g. PKVault).
/// Isolates Vantafyn Companion from provider-specific protocols, DTOs, and licensing.
/// </summary>
public interface IPokemonProvider
{
    /// <summary>
    /// Unique identifier for this provider implementation (e.g. "pkvault").
    /// </summary>
    string ProviderName { get; }

    /// <summary>
    /// Performs an active health and connectivity check against the configured provider service.
    /// </summary>
    Task<PokemonConnectionTestResult> TestConnectionAsync(CancellationToken cancellationToken);

    /// <summary>
    /// Retrieves the operational capabilities supported by the active provider.
    /// </summary>
    Task<PokemonProviderCapabilities> GetCapabilitiesAsync(CancellationToken cancellationToken);

    /// <summary>
    /// Reads and parses Pokémon data (trainer, party, and storage boxes) from a battery save without modifying it.
    /// </summary>
    Task<PokemonSaveParseResult> ParseSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        CancellationToken cancellationToken);

    /// <summary>
    /// Extracts a Pokémon from a save file, returning updated save bytes with that Pokémon removed,
    /// alongside its full details and binary payload.
    /// </summary>
    Task<PokemonExtractResult> ExtractPokemonFromSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        string pokemonId,
        bool isInParty,
        int? boxIndex,
        int slotIndex,
        CancellationToken cancellationToken);

    /// <summary>
    /// Injects a Pokémon into a save file, returning updated save bytes with that Pokémon added.
    /// </summary>
    Task<PokemonInjectResult> InjectPokemonIntoSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        PokemonVaultEntry entry,
        int? targetBoxIndex,
        int? targetSlotIndex,
        bool targetParty,
        CancellationToken cancellationToken);
}

/// <summary>
/// Result of a connection or diagnostic health check against a Pokémon provider.
/// </summary>
public sealed record PokemonConnectionTestResult
{
    [JsonPropertyName("isSuccess")]
    public bool IsSuccess { get; init; }

    [JsonPropertyName("message")]
    public string Message { get; init; } = string.Empty;

    [JsonPropertyName("latencyMs")]
    public long LatencyMs { get; init; }

    [JsonPropertyName("providerVersion")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? ProviderVersion { get; init; }
}

/// <summary>
/// Capabilities advertised by the Pokémon provider backend.
/// </summary>
public sealed record PokemonProviderCapabilities
{
    [JsonPropertyName("canReadSaves")]
    public bool CanReadSaves { get; init; }

    [JsonPropertyName("canWriteSaves")]
    public bool CanWriteSaves { get; init; }

    [JsonPropertyName("canTransferSameGeneration")]
    public bool CanTransferSameGeneration { get; init; }

    [JsonPropertyName("canTransferCrossGeneration")]
    public bool CanTransferCrossGeneration { get; init; }

    [JsonPropertyName("canValidateLegality")]
    public bool CanValidateLegality { get; init; }

    [JsonPropertyName("supportedGenerations")]
    public IReadOnlyList<string> SupportedGenerations { get; init; } = Array.Empty<string>();

    [JsonPropertyName("supportedPlatforms")]
    public IReadOnlyList<string> SupportedPlatforms { get; init; } = Array.Empty<string>();
}

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Configuration options for the optional Pokémon Vault integration.
/// </summary>
public sealed class PokemonConfiguration
{
    /// <summary>
    /// Master toggle for the Pokémon integration. Default is false (opt-in).
    /// </summary>
    public bool Enabled { get; set; } = false;

    /// <summary>
    /// Active provider identifier (e.g. "pkvault").
    /// </summary>
    public string ProviderType { get; set; } = "pkvault";

    /// <summary>
    /// Base URL of the private external PKVault service (e.g. "http://localhost:5000" or "http://pkvault:5000").
    /// Never exposed directly to mobile/TV clients.
    /// </summary>
    public string? PkVaultBaseUrl { get; set; }

    /// <summary>
    /// HTTP timeout in seconds for communication with the Pokémon provider (clamped between 2 and 30).
    /// </summary>
    public int TimeoutSeconds { get; set; } = 5;

    /// <summary>
    /// Whether users are allowed to transfer Pokémon between supported games and their personal Vault.
    /// </summary>
    public bool AllowTransfers { get; set; } = true;

    /// <summary>
    /// Whether cross-generation transfers (e.g., Gen 3 -> Gen 4) are permitted.
    /// </summary>
    public bool AllowCrossGenerationTransfers { get; set; } = false;

    /// <summary>
    /// Whether direct or server-mediated trading between Jellyfin users is allowed.
    /// </summary>
    public bool AllowTrading { get; set; } = false;

    /// <summary>
    /// Whether Pokémon editing or stat modifications are permitted. Default is false.
    /// </summary>
    public bool AllowEditing { get; set; } = false;

    /// <summary>
    /// Whether automatic pre-mutation save backups are mandatory before any write operations.
    /// </summary>
    public bool AutoBackups { get; set; } = true;
}

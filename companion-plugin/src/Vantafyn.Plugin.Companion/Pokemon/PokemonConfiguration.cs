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

    /// <summary>
    /// Optional custom background image path or URL for Pokémon modals and vault storage.
    /// Can be a local filesystem path on the Jellyfin server or an HTTP/HTTPS image URL.
    /// If null or empty, the client falls back to the default dark background.
    /// </summary>
    public string? ModalBackgroundPath { get; set; }

    /// <summary>
    /// URL template for resolving Pokémon cry audio (.ogg).
    /// Placeholders {style} (latest or legacy) and {speciesId} are supported.
    /// Defaults to the official PokeAPI cries repository on GitHub.
    /// </summary>
    public string CrySourceUrlTemplate { get; set; } = "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/{style}/{speciesId}.ogg";

    /// <summary>
    /// Enables optional Pokédex narration through a private, server-side TTS service.
    /// This is deliberately opt-in so clients continue to work normally when no TTS
    /// container has been installed.
    /// </summary>
    public bool NarrationEnabled { get; set; } = false;

    /// <summary>
    /// Base URL reachable by the Jellyfin/Companion process, usually a Docker service
    /// name such as http://kokoro-tts:8880. This URL is never returned to clients.
    /// </summary>
    public string? NarrationBaseUrl { get; set; }

    /// <summary>Voice identifier understood by the configured Kokoro-compatible service.</summary>
    public string NarrationVoice { get; set; } = "am_michael";

    /// <summary>Speech rate sent to the TTS service, clamped to a safe range.</summary>
    public decimal NarrationSpeed { get; set; } = 0.93m;

    /// <summary>Timeout for a narration generation request.</summary>
    public int NarrationTimeoutSeconds { get; set; } = 15;

    /// <summary>
    /// Upgrades only the exact previously shipped narrator defaults to the measured male baseline.
    /// A voice or speed chosen by an administrator remains untouched.
    /// </summary>
    public bool MigrateLegacyNarrationDefault()
    {
        var isFormerPuckDefault = string.Equals(NarrationVoice, "am_puck", StringComparison.OrdinalIgnoreCase) && NarrationSpeed == 0.98m;
        var isFormerMichaelDefault = string.Equals(NarrationVoice, "am_michael", StringComparison.OrdinalIgnoreCase) && NarrationSpeed == 1.08m;
        var isFormerGeorgeDefault = string.Equals(NarrationVoice, "bm_george", StringComparison.OrdinalIgnoreCase) && NarrationSpeed == 0.92m;
        if (!isFormerPuckDefault && !isFormerMichaelDefault && !isFormerGeorgeDefault)
        {
            return false;
        }

        NarrationVoice = "am_michael";
        NarrationSpeed = 0.93m;
        return true;
    }
}

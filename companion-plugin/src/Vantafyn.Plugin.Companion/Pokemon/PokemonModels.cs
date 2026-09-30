using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Metadata identifying a retro title as a Pokémon game, its generation, platform, and save status.
/// </summary>
public sealed class GamePokemonMetadata
{
    [JsonPropertyName("isPokemonGame")]
    public bool IsPokemonGame { get; set; } = true;

    /// <summary>
    /// Canonical internal identifier (e.g. "emerald", "fire_red", "crystal", "platinum").
    /// </summary>
    [JsonPropertyName("pokemonGameId")]
    public string PokemonGameId { get; set; } = string.Empty;

    /// <summary>
    /// Clean display title (e.g. "Pokémon Emerald").
    /// </summary>
    [JsonPropertyName("canonicalTitle")]
    public string CanonicalTitle { get; set; } = string.Empty;

    /// <summary>
    /// Pokémon generation (1 for Red/Blue/Yellow, 2 for Gold/Silver/Crystal, 3 for R/S/E/FR/LG, 4 for D/P/Pt/HG/SS, 5 for B/W/B2/W2, etc.).
    /// </summary>
    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    /// <summary>
    /// Hardware platform identifier (e.g. "gb", "gbc", "gba", "nds", "n64").
    /// </summary>
    [JsonPropertyName("platform")]
    public string Platform { get; set; } = string.Empty;

    /// <summary>
    /// Battery save kind ("sram").
    /// </summary>
    [JsonPropertyName("saveType")]
    public string SaveType { get; set; } = "sram";

    /// <summary>
    /// Whether a battery save (.sram) exists on the server for the current user.
    /// </summary>
    [JsonPropertyName("hasSave")]
    public bool HasSave { get; set; }

    /// <summary>
    /// Whether this specific title is supported by PKVault for save extraction and manipulation.
    /// </summary>
    [JsonPropertyName("vaultSupported")]
    public bool VaultSupported { get; set; } = true;

    /// <summary>
    /// Detection confidence: "override", "exact", "high", or "heuristic".
    /// </summary>
    [JsonPropertyName("detectionConfidence")]
    public string DetectionConfidence { get; set; } = "high";
}

/// <summary>
/// Manual admin override mapping for a specific game token.
/// </summary>
public sealed class PokemonGameOverride
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("isPokemonGame")]
    public bool IsPokemonGame { get; set; } = true;

    [JsonPropertyName("pokemonGameId")]
    public string PokemonGameId { get; set; } = string.Empty;

    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    [JsonPropertyName("platform")]
    public string Platform { get; set; } = string.Empty;

    [JsonPropertyName("canonicalTitle")]
    public string? CanonicalTitle { get; set; }
}

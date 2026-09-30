using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// A Pokémon stored in a user's personal vault.
/// </summary>
public sealed class PokemonVaultEntry
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString("N");

    [JsonPropertyName("boxIndex")]
    public int BoxIndex { get; set; } = 1;

    [JsonPropertyName("slotIndex")]
    public int SlotIndex { get; set; } = 1;

    [JsonPropertyName("species")]
    public string Species { get; set; } = string.Empty;

    [JsonPropertyName("speciesId")]
    public int SpeciesId { get; set; }

    [JsonPropertyName("form")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Form { get; set; }

    [JsonPropertyName("nickname")]
    public string Nickname { get; set; } = string.Empty;

    [JsonPropertyName("level")]
    public int Level { get; set; } = 1;

    [JsonPropertyName("gender")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Gender { get; set; }

    [JsonPropertyName("isShiny")]
    public bool IsShiny { get; set; }

    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    [JsonPropertyName("originalTrainer")]
    public string OriginalTrainer { get; set; } = string.Empty;

    [JsonPropertyName("originalTrainerId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? OriginalTrainerId { get; set; }

    [JsonPropertyName("originGame")]
    public string OriginGame { get; set; } = string.Empty;

    [JsonPropertyName("originGameId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? OriginGameId { get; set; }

    [JsonPropertyName("currentLocation")]
    public string CurrentLocation { get; set; } = string.Empty;

    [JsonPropertyName("createdAtUtc")]
    public DateTimeOffset CreatedAtUtc { get; set; }

    [JsonPropertyName("updatedAtUtc")]
    public DateTimeOffset UpdatedAtUtc { get; set; }

    /// <summary>
    /// Optional serialized binary payload (e.g. base64 representation of PK3/PK4/PK5 raw data).
    /// </summary>
    [JsonPropertyName("rawData")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? RawData { get; set; }

    public PokemonSummaryDto ToSummaryDto()
    {
        return new PokemonSummaryDto
        {
            Id = Id,
            Species = Species,
            SpeciesId = SpeciesId,
            Form = Form,
            Nickname = string.IsNullOrWhiteSpace(Nickname) ? Species : Nickname,
            Level = Level,
            Gender = Gender,
            IsShiny = IsShiny,
            OriginalTrainer = OriginalTrainer,
            OriginalTrainerId = OriginalTrainerId,
            OriginGame = OriginGame,
            CurrentGame = OriginGame,
            CurrentLocation = CurrentLocation,
            BoxIndex = BoxIndex,
            SlotIndex = SlotIndex,
            IsInParty = false,
            LegalityStatus = "valid"
        };
    }
}

/// <summary>
/// A box in a user's vault (standard 30 slots).
/// </summary>
public sealed class PokemonVaultBox
{
    public const int DefaultCapacity = 30;

    [JsonPropertyName("boxIndex")]
    public int BoxIndex { get; set; } = 1;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("capacity")]
    public int Capacity { get; set; } = DefaultCapacity;

    [JsonPropertyName("entries")]
    public List<PokemonVaultEntry> Entries { get; set; } = [];
}

/// <summary>
/// Top-level personal Pokémon Vault container for a single Jellyfin user.
/// Default capacity: 30 boxes x 30 slots = 900 Pokémon.
/// </summary>
public sealed class PokemonVault
{
    public const int DefaultBoxCount = 30;

    [JsonPropertyName("userId")]
    public Guid UserId { get; set; }

    [JsonPropertyName("schemaVersion")]
    public int SchemaVersion { get; set; } = 1;

    [JsonPropertyName("createdAtUtc")]
    public DateTimeOffset CreatedAtUtc { get; set; }

    [JsonPropertyName("updatedAtUtc")]
    public DateTimeOffset UpdatedAtUtc { get; set; }

    [JsonPropertyName("boxes")]
    public List<PokemonVaultBox> Boxes { get; set; } = [];

    public static PokemonVault CreateDefault(Guid userId, DateTimeOffset now)
    {
        var vault = new PokemonVault
        {
            UserId = userId,
            SchemaVersion = 1,
            CreatedAtUtc = now,
            UpdatedAtUtc = now,
            Boxes = new List<PokemonVaultBox>(DefaultBoxCount)
        };

        for (var i = 1; i <= DefaultBoxCount; i++)
        {
            vault.Boxes.Add(new PokemonVaultBox
            {
                BoxIndex = i,
                Name = $"Box {i}",
                Capacity = PokemonVaultBox.DefaultCapacity,
                Entries = []
            });
        }

        return vault;
    }
}

/// <summary>
/// Lightweight summary of a user's vault.
/// </summary>
public sealed class PokemonVaultSummaryDto
{
    [JsonPropertyName("userId")]
    public Guid UserId { get; set; }

    [JsonPropertyName("totalCount")]
    public int TotalCount { get; set; }

    [JsonPropertyName("boxCount")]
    public int BoxCount { get; set; }

    [JsonPropertyName("boxCapacity")]
    public int BoxCapacity { get; set; }

    [JsonPropertyName("totalCapacity")]
    public int TotalCapacity { get; set; }

    [JsonPropertyName("shinyCount")]
    public int ShinyCount { get; set; }

    [JsonPropertyName("createdAtUtc")]
    public DateTimeOffset CreatedAtUtc { get; set; }

    [JsonPropertyName("updatedAtUtc")]
    public DateTimeOffset UpdatedAtUtc { get; set; }
}

/// <summary>
/// Summary of an individual vault box.
/// </summary>
public sealed class PokemonVaultBoxSummaryDto
{
    [JsonPropertyName("boxIndex")]
    public int BoxIndex { get; set; }

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("count")]
    public int Count { get; set; }

    [JsonPropertyName("capacity")]
    public int Capacity { get; set; } = PokemonVaultBox.DefaultCapacity;
}

/// <summary>
/// Per-user preferences and settings for the Pokémon integration.
/// </summary>
public sealed class PokemonUserProfile
{
    [JsonPropertyName("userId")]
    public Guid UserId { get; set; }

    [JsonPropertyName("lastSelectedGameId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? LastSelectedGameId { get; set; }

    [JsonPropertyName("customBoxNames")]
    public Dictionary<int, string> CustomBoxNames { get; set; } = [];

    [JsonPropertyName("updatedAtUtc")]
    public DateTimeOffset UpdatedAtUtc { get; set; }
}

/// <summary>
/// Overall Pokémon integration status and capabilities exposed to client applications.
/// </summary>
public sealed class PokemonIntegrationStatusDto
{
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    [JsonPropertyName("provider")]
    public string Provider { get; set; } = "none";

    [JsonPropertyName("providerHealthy")]
    public bool ProviderHealthy { get; set; }

    [JsonPropertyName("providerMessage")]
    public string ProviderMessage { get; set; } = string.Empty;

    [JsonPropertyName("supportedGenerations")]
    public IReadOnlyList<string> SupportedGenerations { get; set; } = Array.Empty<string>();

    [JsonPropertyName("vaultAvailable")]
    public bool VaultAvailable { get; set; }

    [JsonPropertyName("transfersAvailable")]
    public bool TransfersAvailable { get; set; }

    [JsonPropertyName("crossGenerationAvailable")]
    public bool CrossGenerationAvailable { get; set; }

    [JsonPropertyName("tradingAvailable")]
    public bool TradingAvailable { get; set; }
}

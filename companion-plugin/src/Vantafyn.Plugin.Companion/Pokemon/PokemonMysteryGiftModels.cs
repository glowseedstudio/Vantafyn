using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

public sealed class PokemonMysteryGiftDto
{
    [JsonPropertyName("code")]
    public string Code { get; set; } = string.Empty;

    [JsonPropertyName("aliases")]
    public List<string> Aliases { get; set; } = [];

    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("title")]
    public string Title { get; set; } = string.Empty;

    [JsonPropertyName("subtitle")]
    public string Subtitle { get; set; } = string.Empty;

    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    [JsonPropertyName("region")]
    public string Region { get; set; } = string.Empty;

    /// <summary>
    /// Type of reward: "EventItem", "Pokemon", or "Egg".
    /// </summary>
    [JsonPropertyName("rewardType")]
    public string RewardType { get; set; } = "EventItem";

    [JsonPropertyName("targetSpeciesId")]
    public int TargetSpeciesId { get; set; }

    [JsonPropertyName("targetSpeciesName")]
    public string TargetSpeciesName { get; set; } = string.Empty;

    [JsonPropertyName("accent")]
    public string Accent { get; set; } = "#FBBF24";

    [JsonPropertyName("inGameInstructions")]
    public string InGameInstructions { get; set; } = string.Empty;

    [JsonPropertyName("supportedGameIds")]
    public List<string> SupportedGameIds { get; set; } = [];

    [JsonPropertyName("eventId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? EventId { get; set; }

    [JsonPropertyName("isShiny")]
    public bool IsShiny { get; set; }

    [JsonPropertyName("originalTrainer")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? OriginalTrainer { get; set; }

    [JsonPropertyName("ribbonName")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? RibbonName { get; set; }

    [JsonPropertyName("isRedeemed")]
    public bool IsRedeemed { get; set; }
}

public sealed class PokemonMysteryGiftRedeemRequest
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("code")]
    public string Code { get; set; } = string.Empty;
}

public sealed class PokemonMysteryGiftRedeemResponse
{
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    [JsonPropertyName("code")]
    public string Code { get; set; } = string.Empty;

    [JsonPropertyName("title")]
    public string Title { get; set; } = string.Empty;

    [JsonPropertyName("subtitle")]
    public string Subtitle { get; set; } = string.Empty;

    [JsonPropertyName("rewardType")]
    public string RewardType { get; set; } = "EventItem";

    [JsonPropertyName("targetSpeciesId")]
    public int TargetSpeciesId { get; set; }

    [JsonPropertyName("targetSpeciesName")]
    public string TargetSpeciesName { get; set; } = string.Empty;

    [JsonPropertyName("accent")]
    public string Accent { get; set; } = "#FBBF24";

    [JsonPropertyName("inGameInstructions")]
    public string InGameInstructions { get; set; } = string.Empty;

    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    [JsonPropertyName("backupIds")]
    public IReadOnlyList<string> BackupIds { get; set; } = Array.Empty<string>();
}

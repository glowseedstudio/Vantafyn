using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>Canonical Pokédex metadata cached by the Companion server from PokéAPI.</summary>
public sealed class PokemonDexMetadataDto
{
    [JsonPropertyName("speciesId")] public int SpeciesId { get; set; }
    [JsonPropertyName("name")] public string Name { get; set; } = string.Empty;
    [JsonPropertyName("category")] public string Category { get; set; } = string.Empty;
    [JsonPropertyName("flavorText")] public string FlavorText { get; set; } = string.Empty;
    [JsonPropertyName("heightMeters")] public decimal HeightMeters { get; set; }
    [JsonPropertyName("weightKg")] public decimal WeightKg { get; set; }
    [JsonPropertyName("hp")] public int Hp { get; set; }
    [JsonPropertyName("attack")] public int Attack { get; set; }
    [JsonPropertyName("defense")] public int Defense { get; set; }
    [JsonPropertyName("spAtk")] public int SpAtk { get; set; }
    [JsonPropertyName("spDef")] public int SpDef { get; set; }
    [JsonPropertyName("speed")] public int Speed { get; set; }
    [JsonPropertyName("primaryType")] public string PrimaryType { get; set; } = string.Empty;
    [JsonPropertyName("secondaryType")] public string? SecondaryType { get; set; }
    [JsonPropertyName("source")] public string Source { get; set; } = "pokeapi";
}

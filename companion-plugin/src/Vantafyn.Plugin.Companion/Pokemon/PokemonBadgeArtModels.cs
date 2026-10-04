using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

public sealed class PokemonBadgeArtCatalogDto
{
    [JsonPropertyName("configured")]
    public bool Configured { get; set; }

    [JsonPropertyName("rootPath")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? RootPath { get; set; }

    [JsonPropertyName("availableCount")]
    public int AvailableCount { get; set; }

    [JsonPropertyName("totalCount")]
    public int TotalCount { get; set; }

    [JsonPropertyName("regions")]
    public List<PokemonBadgeRegionDto> Regions { get; set; } = [];
}

public sealed class PokemonBadgeRegionDto
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    [JsonPropertyName("availableCount")]
    public int AvailableCount { get; set; }

    [JsonPropertyName("totalCount")]
    public int TotalCount { get; set; }

    [JsonPropertyName("badges")]
    public List<PokemonBadgeArtDto> Badges { get; set; } = [];
}

public sealed class PokemonBadgeArtDto
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("region")]
    public string Region { get; set; } = string.Empty;

    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    [JsonPropertyName("order")]
    public int Order { get; set; }

    [JsonPropertyName("available")]
    public bool Available { get; set; }

    [JsonPropertyName("imageUrl")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? ImageUrl { get; set; }
}

public sealed record PokemonBadgeArtFile(string Path, string ContentType);

public sealed class PokemonDiplomaProofDto
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("certificateId")]
    public string CertificateId { get; set; } = string.Empty;

    [JsonPropertyName("title")]
    public string Title { get; set; } = string.Empty;

    [JsonPropertyName("uploadedAtUtc")]
    public DateTimeOffset UploadedAtUtc { get; set; }

    [JsonPropertyName("contentType")]
    public string ContentType { get; set; } = "image/png";

    [JsonPropertyName("sizeBytes")]
    public long SizeBytes { get; set; }

    [JsonPropertyName("imageUrl")]
    public string ImageUrl { get; set; } = string.Empty;
}

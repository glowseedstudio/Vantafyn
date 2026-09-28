using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Games;

/// <summary>A Jellyfin library recognized as holding retro game ROMs.</summary>
public sealed class GameLibrary
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Physical folder roots on disk. Not serialized to clients.</summary>
    [JsonIgnore]
    public List<string> Locations { get; set; } = new();
}

/// <summary>A top-level console/system folder within a game library (e.g. SNES, GBA).</summary>
public sealed class GameSystem
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Canonical core / system identifier (e.g. "nes", "snes", "gba", "n64").</summary>
    [JsonPropertyName("core")]
    public string Core { get; set; } = string.Empty;

    [JsonPropertyName("gameCount")]
    public int GameCount { get; set; }
}

/// <summary>Summary representation of a single game in a system.</summary>
public class GameSummary
{
    /// <summary>Opaque URL-safe token resolving to the ROM file on disk.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("title")]
    public string Title { get; set; } = string.Empty;

    [JsonPropertyName("system")]
    public string System { get; set; } = string.Empty;

    [JsonPropertyName("core")]
    public string Core { get; set; } = string.Empty;

    [JsonPropertyName("fileName")]
    public string FileName { get; set; } = string.Empty;

    [JsonPropertyName("sizeBytes")]
    public long SizeBytes { get; set; }

    [JsonPropertyName("boxartUrl")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? BoxartUrl { get; set; }
}

/// <summary>Full detail for a game, including archive or BIOS details.</summary>
public sealed class GameDetail : GameSummary
{
    [JsonPropertyName("isArchive")]
    public bool IsArchive { get; set; }

    [JsonPropertyName("extractedRomName")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? ExtractedRomName { get; set; }

    [JsonPropertyName("biosFiles")]
    public List<GameBios> BiosFiles { get; set; } = new();
}

/// <summary>A BIOS file needed by a console system.</summary>
public sealed class GameBios
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("fileName")]
    public string FileName { get; set; } = string.Empty;

    [JsonPropertyName("sizeBytes")]
    public long SizeBytes { get; set; }
}

/// <summary>Extracted ROM info from an archive index.</summary>
public sealed record ExtractedRomInfo(string Name, long Length);

/// <summary>Exception thrown when an archive entry exceeds safe memory limits.</summary>
public sealed class RomTooLargeException(long actualBytes, long maxBytes)
    : Exception($"ROM exceeds the allowed extraction limit: {actualBytes} > {maxBytes} bytes.")
{
    public long ActualBytes { get; } = actualBytes;
    public long MaxBytes { get; } = maxBytes;
}

using System.Collections.Concurrent;
using System.Text.RegularExpressions;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Pokemon;

public interface IPokemonGameDetector
{
    GamePokemonMetadata? Detect(string title, string fileName, string system, string? core = null);
    Task<GamePokemonMetadata?> GetMetadataAsync(Guid? userId, string gameId, string title, string fileName, string system, string? core = null, CancellationToken cancellationToken = default);
    Task<IReadOnlyDictionary<string, PokemonGameOverride>> GetOverridesAsync(CancellationToken cancellationToken);
    Task SetOverrideAsync(string gameId, PokemonGameOverride overrideEntry, CancellationToken cancellationToken);
    Task RemoveOverrideAsync(string gameId, CancellationToken cancellationToken);
}

public sealed class PokemonGameDetector : IPokemonGameDetector
{
    private readonly ICompanionPaths _paths;
    private readonly ConcurrentDictionary<string, PokemonGameOverride> _overrides = new(StringComparer.OrdinalIgnoreCase);
    private bool _overridesLoaded;
    private readonly SemaphoreSlim _overrideLock = new(1, 1);

    private static readonly Regex CleanPattern = new(@"[^a-zA-Z0-9]+", RegexOptions.Compiled);

    private sealed record CatalogEntry(
        string PokemonGameId,
        string CanonicalTitle,
        int Generation,
        string Platform,
        string[] Keywords,
        string[] Systems);

    private static readonly CatalogEntry[] Catalog =
    [
        // Gen 3 (GBA) - Checked early to prevent FireRed matching Red
        new("firered", "Pokémon FireRed", 3, "gba", ["firered", "fireredversion"], ["gba", "gameboyadvance"]),
        new("leafgreen", "Pokémon LeafGreen", 3, "gba", ["leafgreen", "leafgreenversion"], ["gba", "gameboyadvance"]),
        new("ruby", "Pokémon Ruby", 3, "gba", ["ruby", "rubyversion"], ["gba", "gameboyadvance"]),
        new("sapphire", "Pokémon Sapphire", 3, "gba", ["sapphire", "sapphireversion"], ["gba", "gameboyadvance"]),
        new("emerald", "Pokémon Emerald", 3, "gba", ["emerald", "emeraldversion"], ["gba", "gameboyadvance"]),

        // Gen 5 (NDS) - Checked before Gen 4 Black/White
        new("black2", "Pokémon Black 2", 5, "nds", ["black2", "blackversion2"], ["nds", "ds", "nintendods"]),
        new("white2", "Pokémon White 2", 5, "nds", ["white2", "whiteversion2"], ["nds", "ds", "nintendods"]),
        new("black", "Pokémon Black", 5, "nds", ["black", "blackversion"], ["nds", "ds", "nintendods"]),
        new("white", "Pokémon White", 5, "nds", ["white", "whiteversion"], ["nds", "ds", "nintendods"]),

        // Gen 4 (NDS)
        new("diamond", "Pokémon Diamond", 4, "nds", ["diamond", "diamondversion"], ["nds", "ds", "nintendods"]),
        new("pearl", "Pokémon Pearl", 4, "nds", ["pearl", "pearlversion"], ["nds", "ds", "nintendods"]),
        new("platinum", "Pokémon Platinum", 4, "nds", ["platinum", "platinumversion"], ["nds", "ds", "nintendods"]),
        new("heartgold", "Pokémon HeartGold", 4, "nds", ["heartgold", "heartgoldversion"], ["nds", "ds", "nintendods"]),
        new("soulsilver", "Pokémon SoulSilver", 4, "nds", ["soulsilver", "soulsilverversion"], ["nds", "ds", "nintendods"]),

        // Gen 2 (GBC)
        new("crystal", "Pokémon Crystal", 2, "gbc", ["crystal", "crystalversion"], ["gbc", "gameboycolor"]),
        new("gold", "Pokémon Gold", 2, "gbc", ["gold", "goldversion"], ["gbc", "gameboycolor", "gb", "gameboy"]),
        new("silver", "Pokémon Silver", 2, "gbc", ["silver", "silverversion"], ["gbc", "gameboycolor", "gb", "gameboy"]),

        // Gen 1 (GB)
        new("red", "Pokémon Red", 1, "gb", ["red", "redversion"], ["gb", "gameboy"]),
        new("blue", "Pokémon Blue", 1, "gb", ["blue", "blueversion"], ["gb", "gameboy"]),
        new("yellow", "Pokémon Yellow", 1, "gb", ["yellow", "yellowversion", "specialpikachu"], ["gb", "gameboy"]),
        new("green", "Pokémon Green", 1, "gb", ["green", "greenversion"], ["gb", "gameboy"])
    ];

    public PokemonGameDetector(ICompanionPaths paths)
    {
        _paths = paths;
    }

    public GamePokemonMetadata? Detect(string title, string fileName, string system, string? core = null)
    {
        var normTitle = Normalize(title);
        var normFile = Normalize(Path.GetFileNameWithoutExtension(fileName));
        var normSystem = Normalize(system);
        var normCore = Normalize(core ?? string.Empty);

        // Check if string contains "pokemon" or "pocketmonster"
        var isPokemon = normTitle.Contains("pokemon") || normTitle.Contains("pocketmonster") ||
                        normFile.Contains("pokemon") || normFile.Contains("pocketmonster");

        if (!isPokemon)
        {
            return null;
        }

        // Match against catalog
        foreach (var entry in Catalog)
        {
            // Verify platform/system affinity
            var systemMatches = IsSystemCompatible(normSystem, normCore, entry.Systems);
            if (!systemMatches && !string.IsNullOrWhiteSpace(normSystem))
            {
                continue;
            }

            foreach (var kw in entry.Keywords)
            {
                if (MatchesKeyword(normTitle, kw) || MatchesKeyword(normFile, kw))
                {
                    return new GamePokemonMetadata
                    {
                        IsPokemonGame = true,
                        PokemonGameId = entry.PokemonGameId,
                        CanonicalTitle = entry.CanonicalTitle,
                        Generation = entry.Generation,
                        Platform = entry.Platform,
                        SaveType = "sram",
                        HasSave = false,
                        VaultSupported = true,
                        DetectionConfidence = "high"
                    };
                }
            }
        }

        // Generic Pokémon fallback
        var detectedGen = InferGeneration(normSystem, normCore);
        return new GamePokemonMetadata
        {
            IsPokemonGame = true,
            PokemonGameId = "pokemon_generic",
            CanonicalTitle = title,
            Generation = detectedGen,
            Platform = !string.IsNullOrWhiteSpace(normSystem) ? normSystem : "retro",
            SaveType = "sram",
            HasSave = false,
            VaultSupported = detectedGen is >= 1 and <= 5,
            DetectionConfidence = "heuristic"
        };
    }

    public async Task<GamePokemonMetadata?> GetMetadataAsync(
        Guid? userId,
        string gameId,
        string title,
        string fileName,
        string system,
        string? core = null,
        CancellationToken cancellationToken = default)
    {
        await EnsureOverridesLoadedAsync(cancellationToken).ConfigureAwait(false);

        // 1. Check admin override
        if (!string.IsNullOrWhiteSpace(gameId) && _overrides.TryGetValue(gameId, out var customOverride))
        {
            if (!customOverride.IsPokemonGame)
            {
                return null;
            }

            var hasSaveOverride = userId.HasValue && CheckHasSave(userId.Value, gameId);
            return new GamePokemonMetadata
            {
                IsPokemonGame = true,
                PokemonGameId = customOverride.PokemonGameId,
                CanonicalTitle = customOverride.CanonicalTitle ?? title,
                Generation = customOverride.Generation,
                Platform = customOverride.Platform,
                SaveType = "sram",
                HasSave = hasSaveOverride,
                VaultSupported = true,
                DetectionConfidence = "override"
            };
        }

        // 2. Automated detection
        var detected = Detect(title, fileName, system, core);
        if (detected == null)
        {
            return null;
        }

        // 3. Inspect user save existence
        if (userId.HasValue && !string.IsNullOrWhiteSpace(gameId))
        {
            detected.HasSave = CheckHasSave(userId.Value, gameId);
        }

        return detected;
    }

    public async Task<IReadOnlyDictionary<string, PokemonGameOverride>> GetOverridesAsync(CancellationToken cancellationToken)
    {
        await EnsureOverridesLoadedAsync(cancellationToken).ConfigureAwait(false);
        return _overrides;
    }

    public async Task SetOverrideAsync(string gameId, PokemonGameOverride overrideEntry, CancellationToken cancellationToken)
    {
        await EnsureOverridesLoadedAsync(cancellationToken).ConfigureAwait(false);
        _overrides[gameId] = overrideEntry;
        await SaveOverridesToFileAsync(cancellationToken).ConfigureAwait(false);
    }

    public async Task RemoveOverrideAsync(string gameId, CancellationToken cancellationToken)
    {
        await EnsureOverridesLoadedAsync(cancellationToken).ConfigureAwait(false);
        _overrides.TryRemove(gameId, out _);
        await SaveOverridesToFileAsync(cancellationToken).ConfigureAwait(false);
    }

    private bool CheckHasSave(Guid userId, string gameId)
    {
        try
        {
            var safeGame = SanitizeFileName(gameId);
            var savePath = Path.Combine(_paths.GameSavesRoot, userId.ToString("N"), $"{safeGame}.sram");
            return File.Exists(savePath);
        }
        catch
        {
            return false;
        }
    }

    private static string SanitizeFileName(string value)
    {
        var invalid = Path.GetInvalidFileNameChars();
        var chars = value.Where(c => Array.IndexOf(invalid, c) < 0 && c != '.' && c != '/' && c != '\\').ToArray();
        var sanitized = new string(chars);
        return sanitized.Length > 200 ? sanitized[..200] : sanitized;
    }

    private async Task EnsureOverridesLoadedAsync(CancellationToken cancellationToken)
    {
        if (_overridesLoaded) return;

        await _overrideLock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            if (_overridesLoaded) return;
            var path = Path.Combine(_paths.PokemonRoot, "game-overrides.json");
            var dict = await JsonFile.ReadAsync<Dictionary<string, PokemonGameOverride>>(path, cancellationToken).ConfigureAwait(false);
            if (dict != null)
            {
                foreach (var (k, v) in dict)
                {
                    _overrides[k] = v;
                }
            }
            _overridesLoaded = true;
        }
        finally
        {
            _overrideLock.Release();
        }
    }

    private async Task SaveOverridesToFileAsync(CancellationToken cancellationToken)
    {
        await _overrideLock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = Path.Combine(_paths.PokemonRoot, "game-overrides.json");
            var dict = _overrides.ToDictionary(k => k.Key, v => v.Value);
            await JsonFile.WriteAtomicAsync(path, dict, cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            _overrideLock.Release();
        }
    }

    private static string Normalize(string text)
    {
        if (string.IsNullOrWhiteSpace(text)) return string.Empty;
        var lowered = text.ToLowerInvariant().Replace("é", "e");
        return CleanPattern.Replace(lowered, string.Empty);
    }

    private static bool IsSystemCompatible(string normSystem, string normCore, string[] allowedSystems)
    {
        if (string.IsNullOrWhiteSpace(normSystem) && string.IsNullOrWhiteSpace(normCore))
        {
            return true;
        }

        return allowedSystems.Any(s =>
            string.Equals(normSystem, s, StringComparison.OrdinalIgnoreCase) ||
            string.Equals(normCore, s, StringComparison.OrdinalIgnoreCase) ||
            (s.Length > 2 && (normSystem.Contains(s) || normCore.Contains(s))));
    }

    private static bool MatchesKeyword(string normalizedText, string keyword)
    {
        // Special case for Black/White 2
        if (keyword is "black2" or "white2")
        {
            return normalizedText.Contains(keyword) || normalizedText.Contains(keyword.Replace("2", "version2"));
        }

        // Avoid matching "black" when string is "black2"
        if (keyword is "black" or "blackversion")
        {
            if (normalizedText.Contains("black2") || normalizedText.Contains("blackversion2")) return false;
        }
        if (keyword is "white" or "whiteversion")
        {
            if (normalizedText.Contains("white2") || normalizedText.Contains("whiteversion2")) return false;
        }
        if (keyword is "red" or "redversion")
        {
            if (normalizedText.Contains("firered")) return false;
        }
        if (keyword is "green" or "greenversion")
        {
            if (normalizedText.Contains("leafgreen")) return false;
        }

        return normalizedText.Contains(keyword);
    }

    private static int InferGeneration(string normSystem, string normCore)
    {
        if (normSystem.Contains("gba") || normCore.Contains("gba")) return 3;
        if (normSystem.Contains("nds") || normCore.Contains("nds")) return 4;
        if (normSystem.Contains("gbc") || normCore.Contains("gbc")) return 2;
        if (normSystem.Contains("gb") || normCore.Contains("gb")) return 1;
        return 0;
    }
}

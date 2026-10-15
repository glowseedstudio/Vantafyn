using System;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Games;

/// <summary>
/// Manages per-user cloud save-state, SRAM battery saves, and core options under plugin storage.
/// </summary>
public sealed class GameSavesService : IGameSavesService
{
    private readonly string _savesPath;
    private static readonly SemaphoreSlim _lock = new(1, 1);

    public GameSavesService(ICompanionPaths companionPaths)
    {
        _savesPath = companionPaths.GameSavesRoot;
    }

    public static readonly Dictionary<string, string> Known3dsTitleIds = new(StringComparer.OrdinalIgnoreCase)
    {
        ["001b5000"] = "Pokémon Ultra Sun",
        ["001b5100"] = "Pokémon Ultra Moon",
        ["00164800"] = "Pokémon Sun",
        ["00175e00"] = "Pokémon Moon",
        ["0011c400"] = "Pokémon Omega Ruby",
        ["0011c500"] = "Pokémon Alpha Sapphire",
        ["00055d00"] = "Pokémon X",
        ["00055e00"] = "Pokémon Y",
    };

    public static string? Deduce3dsTitleId(string identifier)
    {
        var lower = identifier.ToLowerInvariant();
        if (lower.Contains("001b5000") || (lower.Contains("ultra") && (lower.Contains("sun") || lower.Contains("us")))) return "001b5000";
        if (lower.Contains("001b5100") || (lower.Contains("ultra") && (lower.Contains("moon") || lower.Contains("um")))) return "001b5100";
        if (lower.Contains("00164800") || lower.Contains("sun")) return "00164800";
        if (lower.Contains("00175e00") || lower.Contains("moon")) return "00175e00";
        if (lower.Contains("0011c400") || lower.Contains("omega") || lower.Contains("ruby")) return "0011c400";
        if (lower.Contains("0011c500") || lower.Contains("alpha") || lower.Contains("sapphire")) return "0011c500";
        if (lower.Contains("00055d00") || lower.Contains("pokemon x") || lower.Contains("pokemon_x") || lower.EndsWith("_x") || lower == "x") return "00055d00";
        if (lower.Contains("00055e00") || lower.Contains("pokemon y") || lower.Contains("pokemon_y") || lower.EndsWith("_y") || lower == "y") return "00055e00";
        return null;
    }

    public async Task<byte[]?> GetAsync(Guid userId, string gameId, string kind, CancellationToken cancellationToken)
    {
        var path = ResolvePath(userId, gameId, kind);
        if (path == null)
        {
            return null;
        }

        if (!File.Exists(path) && string.Equals(kind, "sram", StringComparison.OrdinalIgnoreCase))
        {
            var userDir = Path.GetDirectoryName(path);
            var safeGame = SanitizeFileName(gameId);
            if (userDir != null && Directory.Exists(userDir))
            {
                var candidateSav = Path.Combine(userDir, $"{safeGame}.sav");
                var candidateSrm = Path.Combine(userDir, $"{safeGame}.srm");
                var candidateMainExt = Path.Combine(userDir, $"{safeGame}.main");
                var candidateBin = Path.Combine(userDir, $"{safeGame}.bin");
                var candidateMain = Path.Combine(userDir, "main");
                if (File.Exists(candidateSav)) path = candidateSav;
                else if (File.Exists(candidateSrm)) path = candidateSrm;
                else if (File.Exists(candidateMainExt)) path = candidateMainExt;
                else if (File.Exists(candidateBin)) path = candidateBin;
                else if (File.Exists(candidateMain)) path = candidateMain;
                else
                {
                    // Fallback to case-insensitive match in userDir for Linux filesystems where gameId casing may differ
                    var matched = Directory.EnumerateFiles(userDir)
                        .FirstOrDefault(f => Path.GetFileNameWithoutExtension(f).Equals(safeGame, StringComparison.OrdinalIgnoreCase)
                            && (f.EndsWith(".sram", StringComparison.OrdinalIgnoreCase)
                                || f.EndsWith(".sav", StringComparison.OrdinalIgnoreCase)
                                || f.EndsWith(".srm", StringComparison.OrdinalIgnoreCase)
                                || f.EndsWith(".bin", StringComparison.OrdinalIgnoreCase)
                                || f.EndsWith(".main", StringComparison.OrdinalIgnoreCase)));
                    if (matched != null)
                    {
                        path = matched;
                    }
                    else
                    {
                        // 3DS Title ID and alternate game title resolution
                        var titleId = Deduce3dsTitleId(gameId);
                        if (!string.IsNullOrEmpty(titleId))
                        {
                            var titleMatch = Directory.EnumerateFiles(userDir)
                                .FirstOrDefault(f => (Path.GetFileName(f).Contains(titleId, StringComparison.OrdinalIgnoreCase)
                                    || (titleId == "001b5000" && Path.GetFileName(f).Contains("ultrasun", StringComparison.OrdinalIgnoreCase)))
                                    && (f.EndsWith(".sram", StringComparison.OrdinalIgnoreCase)
                                        || f.EndsWith(".sav", StringComparison.OrdinalIgnoreCase)
                                        || f.EndsWith(".main", StringComparison.OrdinalIgnoreCase)
                                        || f.EndsWith(".bin", StringComparison.OrdinalIgnoreCase)));
                            if (titleMatch != null) path = titleMatch;
                        }

                        // Local 3DS game prefix fallback
                        if (!File.Exists(path) && safeGame.StartsWith("local_3ds_", StringComparison.OrdinalIgnoreCase))
                        {
                            var stripped = safeGame[10..];
                            var strippedMatch = Directory.EnumerateFiles(userDir)
                                .FirstOrDefault(f => Path.GetFileName(f).Contains(stripped, StringComparison.OrdinalIgnoreCase));
                            if (strippedMatch != null) path = strippedMatch;
                        }
                    }
                }
            }
        }

        if (!File.Exists(path))
        {
            return null;
        }

        await _lock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            return await File.ReadAllBytesAsync(path, cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            _lock.Release();
        }
    }

    public async Task SaveAsync(Guid userId, string gameId, string kind, byte[] data, CancellationToken cancellationToken)
    {
        var path = ResolvePath(userId, gameId, kind);
        if (path == null)
        {
            throw new ArgumentException("Invalid game id.", nameof(gameId));
        }

        var dir = Path.GetDirectoryName(path)!;
        Directory.CreateDirectory(dir);

        await _lock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            await File.WriteAllBytesAsync(path, data, cancellationToken).ConfigureAwait(false);

            // For 3DS SRAM saves (Gen 6/7), also mirror to canonical title ID and name aliases
            if (string.Equals(kind, "sram", StringComparison.OrdinalIgnoreCase) && data.Length > 0)
            {
                var titleId = Deduce3dsTitleId(gameId);
                if (titleId == null && data.Length == 445440)
                {
                    // 445,440 bytes is the authentic Gen 7 (Sun/Moon/US/UM) main save size
                    titleId = gameId.Contains("moon", StringComparison.OrdinalIgnoreCase) ? "001b5100" : "001b5000";
                }

                if (!string.IsNullOrEmpty(titleId))
                {
                    var aliasPath = Path.Combine(dir, $"3ds_{titleId}.sram");
                    if (!string.Equals(aliasPath, path, StringComparison.OrdinalIgnoreCase))
                    {
                        await File.WriteAllBytesAsync(aliasPath, data, cancellationToken).ConfigureAwait(false);
                    }
                    if (titleId == "001b5000")
                    {
                        var usPath = Path.Combine(dir, "pokemon_ultrasun.sram");
                        if (!string.Equals(usPath, path, StringComparison.OrdinalIgnoreCase))
                        {
                            await File.WriteAllBytesAsync(usPath, data, cancellationToken).ConfigureAwait(false);
                        }
                    }
                }
            }
        }
        finally
        {
            _lock.Release();
        }
    }

    public async Task<bool> DeleteAsync(Guid userId, string gameId, string kind, CancellationToken cancellationToken)
    {
        var path = ResolvePath(userId, gameId, kind);
        if (path == null)
        {
            return false;
        }

        if (!File.Exists(path) && string.Equals(kind, "sram", StringComparison.OrdinalIgnoreCase))
        {
            var userDir = Path.GetDirectoryName(path);
            var safeGame = SanitizeFileName(gameId);
            if (userDir != null && Directory.Exists(userDir))
            {
                var candidateSav = Path.Combine(userDir, $"{safeGame}.sav");
                var candidateSrm = Path.Combine(userDir, $"{safeGame}.srm");
                var candidateMainExt = Path.Combine(userDir, $"{safeGame}.main");
                var candidateBin = Path.Combine(userDir, $"{safeGame}.bin");
                var candidateMain = Path.Combine(userDir, "main");
                if (File.Exists(candidateSav)) path = candidateSav;
                else if (File.Exists(candidateSrm)) path = candidateSrm;
                else if (File.Exists(candidateMainExt)) path = candidateMainExt;
                else if (File.Exists(candidateBin)) path = candidateBin;
                else if (File.Exists(candidateMain)) path = candidateMain;
            }
        }

        if (!File.Exists(path))
        {
            return false;
        }

        await _lock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            File.Delete(path);
            return true;
        }
        finally
        {
            _lock.Release();
        }
    }

    public async Task<IReadOnlyList<CloudSaveSummaryDto>> ListAsync(Guid userId, CancellationToken cancellationToken)
    {
        var userDir = Path.Combine(_savesPath, userId.ToString("N"));
        if (!Directory.Exists(userDir))
        {
            return Array.Empty<CloudSaveSummaryDto>();
        }

        var results = new List<CloudSaveSummaryDto>();
        var seenGameKeys = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        var validExtensions = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
        {
            ".sram", ".sav", ".srm", ".state", ".main"
        };

        foreach (var file in Directory.EnumerateFiles(userDir))
        {
            var ext = Path.GetExtension(file);
            if (!validExtensions.Contains(ext)) continue;

            var rawName = Path.GetFileNameWithoutExtension(file);
            var kind = ext.TrimStart('.').ToLowerInvariant();
            if (kind is "sav" or "srm" or "main") kind = "sram";

            var titleId = Deduce3dsTitleId(rawName);
            var gameTitle = titleId != null && Known3dsTitleIds.TryGetValue(titleId, out var kt)
                ? kt
                : rawName.Replace('_', ' ').Trim();

            // De-duplicate aliases (e.g. 3ds_001b5000 vs local_3ds_... vs pokemon_ultrasun)
            var dedupeKey = titleId ?? rawName;
            if (!seenGameKeys.Add(dedupeKey)) continue;

            var fi = new FileInfo(file);
            results.Add(new CloudSaveSummaryDto
            {
                GameId = rawName,
                Kind = kind,
                LastModifiedMs = new DateTimeOffset(fi.LastWriteTimeUtc).ToUnixTimeMilliseconds(),
                SizeBytes = fi.Length,
                TitleId = titleId,
                GameTitle = gameTitle,
            });
        }

        return results;
    }

    private string? ResolvePath(Guid userId, string gameId, string kind)
    {
        if (string.IsNullOrWhiteSpace(gameId))
        {
            return null;
        }

        var normalizedKind = kind?.Trim().ToLowerInvariant();
        var safeKind = normalizedKind switch
        {
            "sram" => "sram",
            "settings" => "settings",
            _ => "state"
        };

        var safeGame = SanitizeFileName(gameId);
        if (safeGame.Length == 0)
        {
            return null;
        }

        var path = Path.GetFullPath(Path.Combine(_savesPath, userId.ToString("N"), $"{safeGame}.{safeKind}"));

        // Ensure path cannot escape saves root
        var rootFull = Path.GetFullPath(_savesPath);
        var rootWithSep = rootFull.EndsWith(Path.DirectorySeparatorChar)
            ? rootFull
            : rootFull + Path.DirectorySeparatorChar;

        return path.StartsWith(rootWithSep, StringComparison.Ordinal) ? path : null;
    }

    private static string SanitizeFileName(string value)
    {
        var invalid = Path.GetInvalidFileNameChars();
        var chars = value.Where(c => Array.IndexOf(invalid, c) < 0 && c != '.' && c != '/' && c != '\\').ToArray();
        var sanitized = new string(chars);
        return sanitized.Length > 200 ? sanitized[..200] : sanitized;
    }
}

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

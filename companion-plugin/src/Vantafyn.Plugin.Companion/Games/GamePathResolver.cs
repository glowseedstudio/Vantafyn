using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Vantafyn.Plugin.Companion.Games;

/// <summary>
/// Resolves opaque game tokens and verifies that paths remain strictly contained within library roots.
/// </summary>
public sealed class GamePathResolver
{
    private readonly Func<IReadOnlyList<GameLibrary>> _getLibraries;

    public GamePathResolver(Func<IReadOnlyList<GameLibrary>> getLibraries)
    {
        _getLibraries = getLibraries;
    }

    public ResolvedGameFile? ResolveGameFile(string libraryId, string token)
    {
        var path = DecodeToken(token);
        return path == null ? null : ResolveAbsoluteGameFile(libraryId, path, requireExisting: true);
    }

    public ResolvedGameFile? ResolveAbsoluteGameFile(string libraryId, string path, bool requireExisting)
    {
        var libraries = _getLibraries();
        var library = libraries.FirstOrDefault(l => string.Equals(l.Id, libraryId, StringComparison.OrdinalIgnoreCase));
        if (library != null && IsWithinLibrary(library, path) && (!requireExisting || File.Exists(path)))
        {
            var systemDir = FindSystemDir(library, path);
            return new ResolvedGameFile(library, path, systemDir, systemDir == null ? string.Empty : Path.GetFileName(systemDir));
        }

        // Fallback: If libraryId is "default" or mismatched, search across all configured libraries
        foreach (var lib in libraries)
        {
            if (IsWithinLibrary(lib, path) && (!requireExisting || File.Exists(path)))
            {
                var systemDir = FindSystemDir(lib, path);
                return new ResolvedGameFile(lib, path, systemDir, systemDir == null ? string.Empty : Path.GetFileName(systemDir));
            }
        }

        return null;
    }

    public string? ResolveFilePath(string libraryId, string token, bool allowBios)
    {
        var resolved = ResolveGameFile(libraryId, token);
        if (resolved == null)
        {
            return null;
        }

        var isKnownGameFile = GameSystemCoreResolver.IsRomFile(resolved.RomPath) ||
                              GameSystemCoreResolver.IsBiosFile(resolved.RomPath);
        return !isKnownGameFile && !allowBios ? null : resolved.RomPath;
    }

    public static string EncodeToken(string absolutePath)
    {
        var bytes = Encoding.UTF8.GetBytes(absolutePath);
        return Convert.ToBase64String(bytes).Replace('+', '-').Replace('/', '_').TrimEnd('=');
    }

    public static string? DecodeToken(string token)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            return null;
        }

        try
        {
            var padded = token.Replace('-', '+').Replace('_', '/');
            switch (padded.Length % 4)
            {
                case 2: padded += "=="; break;
                case 3: padded += "="; break;
            }

            return Encoding.UTF8.GetString(Convert.FromBase64String(padded));
        }
        catch (Exception)
        {
            return null;
        }
    }

    private static string? FindSystemDir(GameLibrary library, string romPath)
    {
        foreach (var root in library.Locations)
        {
            var rootFull = Path.GetFullPath(root);
            var current = Path.GetDirectoryName(Path.GetFullPath(romPath));
            while (!string.IsNullOrEmpty(current))
            {
                var parent = Path.GetDirectoryName(current);
                if (parent != null && PathsEqual(parent, rootFull))
                {
                    return current;
                }

                if (PathsEqual(current, rootFull))
                {
                    break;
                }

                current = parent;
            }
        }

        return null;
    }

    public static bool IsWithinLibrary(GameLibrary library, string path) =>
        library.Locations.Any(root => IsWithinDirectory(root, path));

    private static bool IsWithinDirectory(string rootDir, string targetPath)
    {
        try
        {
            var fullRoot = Path.GetFullPath(rootDir);
            var fullTarget = Path.GetFullPath(targetPath);

            var rootWithSep = fullRoot.EndsWith(Path.DirectorySeparatorChar)
                ? fullRoot
                : fullRoot + Path.DirectorySeparatorChar;

            return fullTarget.StartsWith(rootWithSep, StringComparison.OrdinalIgnoreCase);
        }
        catch
        {
            return false;
        }
    }

    private static bool PathsEqual(string a, string b) =>
        string.Equals(Path.GetFullPath(a), Path.GetFullPath(b), StringComparison.OrdinalIgnoreCase);
}

public sealed record ResolvedGameFile(
    GameLibrary Library,
    string RomPath,
    string? SystemDir,
    string SystemName);

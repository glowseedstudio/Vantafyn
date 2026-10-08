using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text.RegularExpressions;
using MediaBrowser.Controller.Library;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;

namespace Vantafyn.Plugin.Companion.Games;

public sealed class GamesService : IGamesService
{
    private readonly ILibraryManager _libraryManager;
    private readonly GamePathResolver _paths;
    private readonly ILogger<GamesService> _logger;

    private static readonly Regex AutoDetectLibraryName =
        new("game|rom|emulat", RegexOptions.IgnoreCase | RegexOptions.Compiled);

    public const long MaxExtractedRomBytes = 512L * 1024 * 1024; // 512 MB ceiling

    public GamesService(ILibraryManager libraryManager, ILogger<GamesService>? logger = null)
    {
        _libraryManager = libraryManager;
        _logger = logger ?? NullLogger<GamesService>.Instance;
        _paths = new GamePathResolver(GetGameLibraries);
    }

    public IReadOnlyList<GameLibrary> GetGameLibraries()
    {
        var config = Plugin.Instance?.Configuration;
        var customPath = config?.CustomGamesPath;
        var configuredIds = config?.GameLibraryIds ?? new List<string>();

        var result = new List<GameLibrary>();

        // 1. If explicit custom path is specified on the host server
        if (!string.IsNullOrWhiteSpace(customPath) && Directory.Exists(customPath))
        {
            result.Add(new GameLibrary
            {
                Id = "custom-games",
                Name = "Custom Games Directory",
                Locations = new List<string> { customPath }
            });
        }

        // 2. If explicit Jellyfin library IDs are configured
        if (configuredIds.Count > 0)
        {
            foreach (var cid in configuredIds)
            {
                var resolved = ResolveConfiguredLibrary(cid);
                if (resolved != null && !result.Any(r => string.Equals(r.Id, resolved.Id, StringComparison.OrdinalIgnoreCase)))
                {
                    result.Add(resolved);
                }
            }
        }

        // 3. Fall back to auto-detect by library name if nothing else was resolved
        if (result.Count == 0)
        {
            foreach (var folder in _libraryManager.GetVirtualFolders())
            {
                if (string.IsNullOrEmpty(folder.Name) || !AutoDetectLibraryName.IsMatch(folder.Name))
                {
                    continue;
                }

                var locations = (folder.Locations ?? Array.Empty<string>()).Where(Directory.Exists).ToList();
                if (locations.Count == 0)
                {
                    continue;
                }

                result.Add(new GameLibrary
                {
                    Id = folder.ItemId ?? string.Empty,
                    Name = folder.Name,
                    Locations = locations
                });
            }
        }

        return result;
    }

    private GameLibrary? ResolveConfiguredLibrary(string? cid)
    {
        if (string.IsNullOrWhiteSpace(cid))
        {
            return null;
        }

        if (Guid.TryParse(cid, out var guid))
        {
            var item = _libraryManager.GetItemById(guid);
            var locs = (item?.PhysicalLocations ?? Array.Empty<string>()).Where(Directory.Exists).ToList();
            if (locs.Count > 0)
            {
                return new GameLibrary { Id = cid, Name = item?.Name ?? "Games", Locations = locs };
            }
        }

        foreach (var folder in _libraryManager.GetVirtualFolders())
        {
            if (string.Equals(folder.ItemId, cid, StringComparison.OrdinalIgnoreCase))
            {
                var locs = (folder.Locations ?? Array.Empty<string>()).Where(Directory.Exists).ToList();
                if (locs.Count > 0)
                {
                    return new GameLibrary { Id = cid, Name = folder.Name ?? "Games", Locations = locs };
                }
            }
        }

        return null;
    }

    public IReadOnlyList<GameSystem> GetSystems(string libraryId)
    {
        var library = GetGameLibraries().FirstOrDefault(l =>
            string.Equals(l.Id, libraryId, StringComparison.OrdinalIgnoreCase));
        if (library == null)
        {
            return Array.Empty<GameSystem>();
        }

        var systems = new List<GameSystem>();
        var seenSystems = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var root in library.Locations)
        {
            foreach (var systemDir in SafeEnumerateDirectories(root))
            {
                var name = Path.GetFileName(systemDir);
                if (string.IsNullOrEmpty(name) || name.StartsWith('.') || !seenSystems.Add(name))
                {
                    continue;
                }

                var games = GetGamesInSystem(systemDir, library.Id);
                if (games.Count == 0)
                {
                    continue;
                }

                systems.Add(new GameSystem
                {
                    Id = name,
                    Name = name,
                    Core = GameSystemCoreResolver.ResolveCore(name),
                    GameCount = games.Count,
                    LogoUrl = GameSystemCoreResolver.ResolveSystemLogoUrl(name)
                });
            }
        }

        return systems;
    }

    public IReadOnlyList<GameSummary> GetGames(string libraryId, string? system = null)
    {
        var library = GetGameLibraries().FirstOrDefault(l =>
            string.Equals(l.Id, libraryId, StringComparison.OrdinalIgnoreCase));
        if (library == null)
        {
            return Array.Empty<GameSummary>();
        }

        var results = new List<GameSummary>();

        foreach (var root in library.Locations)
        {
            if (!string.IsNullOrWhiteSpace(system))
            {
                var systemDir = Path.Combine(root, system);
                if (Directory.Exists(systemDir))
                {
                    results.AddRange(GetGamesInSystem(systemDir, library.Id));
                }
            }
            else
            {
                foreach (var systemDir in SafeEnumerateDirectories(root))
                {
                    results.AddRange(GetGamesInSystem(systemDir, library.Id));
                }
            }
        }

        return results;
    }

    public GameDetail? GetGame(string libraryId, string gameId)
    {
        var resolved = _paths.ResolveGameFile(libraryId, gameId);
        if (resolved == null)
        {
            return null;
        }

        var fileInfo = new FileInfo(resolved.RomPath);
        var isArchive = GameSystemCoreResolver.IsArchive(resolved.RomPath);
        var systemName = resolved.SystemName;
        var core = GameSystemCoreResolver.ResolveCore(systemName, fileInfo.Name);

        var biosFiles = new List<GameBios>();
        if (!string.IsNullOrEmpty(resolved.SystemDir) && Directory.Exists(resolved.SystemDir))
        {
            foreach (var file in SafeEnumerateFiles(resolved.SystemDir))
            {
                if (GameSystemCoreResolver.IsBiosFile(file))
                {
                    var biosInfo = new FileInfo(file);
                    biosFiles.Add(new GameBios
                    {
                        Id = GamePathResolver.EncodeToken(file),
                        FileName = biosInfo.Name,
                        SizeBytes = biosInfo.Length
                    });
                }
            }
        }

        string? extractedName = null;
        if (isArchive)
        {
            var info = GetExtractedRomInfo(resolved.RomPath);
            extractedName = info?.Name;
        }

        string? boxartUrl = null;
        var localBoxart = FindLocalBoxart(resolved.RomPath);
        if (localBoxart != null)
        {
            boxartUrl = $"/Vantafyn/Games/{Uri.EscapeDataString(libraryId)}/Games/{Uri.EscapeDataString(gameId)}/Boxart";
        }
        else
        {
            boxartUrl = GameSystemCoreResolver.GetLibretroBoxartUrl(core, fileInfo.Name);
        }

        return new GameDetail
        {
            Id = gameId,
            Title = GameSystemCoreResolver.CleanGameTitle(fileInfo.Name),
            System = systemName,
            Core = core,
            FileName = fileInfo.Name,
            SizeBytes = fileInfo.Length,
            IsArchive = isArchive,
            ExtractedRomName = extractedName,
            BiosFiles = biosFiles,
            BoxartUrl = boxartUrl
        };
    }

    public string? GetBoxartPath(string libraryId, string gameId)
    {
        var resolved = _paths.ResolveGameFile(libraryId, gameId);
        if (resolved == null)
        {
            return null;
        }

        return FindLocalBoxart(resolved.RomPath);
    }

    public string? ResolveFilePath(string libraryId, string token, bool allowBios) =>
        _paths.ResolveFilePath(libraryId, token, allowBios);

    public static byte[]? ExtractRomFromArchive(string archivePath)
    {
        if (!File.Exists(archivePath))
        {
            return null;
        }

        using var zip = ZipFile.OpenRead(archivePath);
        var chosen = ChooseZipEntry(zip);
        if (chosen == null)
        {
            return null;
        }

        if (chosen.Length > MaxExtractedRomBytes)
        {
            throw new RomTooLargeException(chosen.Length, MaxExtractedRomBytes);
        }

        using var entryStream = chosen.Open();
        using var ms = new MemoryStream();
        var buffer = new byte[81920];
        long total = 0;
        int read;
        while ((read = entryStream.Read(buffer, 0, buffer.Length)) > 0)
        {
            total += read;
            if (total > MaxExtractedRomBytes)
            {
                throw new RomTooLargeException(total, MaxExtractedRomBytes);
            }
            ms.Write(buffer, 0, read);
        }

        return ms.ToArray();
    }

    public static ExtractedRomInfo? GetExtractedRomInfo(string archivePath)
    {
        try
        {
            using var zip = ZipFile.OpenRead(archivePath);
            var entry = ChooseZipEntry(zip);
            return entry == null ? null : new ExtractedRomInfo(entry.Name, entry.Length);
        }
        catch
        {
            return null;
        }
    }

    byte[]? IGamesService.ExtractRomFromArchive(string archivePath) => ExtractRomFromArchive(archivePath);
    ExtractedRomInfo? IGamesService.GetExtractedRomInfo(string archivePath) => GetExtractedRomInfo(archivePath);

    private static ZipArchiveEntry? ChooseZipEntry(ZipArchive zip)
    {
        ZipArchiveEntry? largest = null;
        foreach (var entry in zip.Entries)
        {
            if (string.IsNullOrEmpty(entry.Name)) continue;

            if (GameSystemCoreResolver.IsKnownRomExtension(entry.Name))
            {
                return entry;
            }

            if (largest == null || entry.Length > largest.Length)
            {
                largest = entry;
            }
        }

        return largest;
    }

    private static readonly string[] SidecarImageExtensions = { ".png", ".jpg", ".jpeg", ".webp" };
    private static readonly string[] SidecarFolderImageNames = { "cover", "boxart", "poster", "folder", "front" };

    public static string? FindLocalBoxart(string romPath)
    {
        if (string.IsNullOrEmpty(romPath) || !File.Exists(romPath))
        {
            return null;
        }

        var dir = Path.GetDirectoryName(romPath);
        if (string.IsNullOrEmpty(dir) || !Directory.Exists(dir))
        {
            return null;
        }

        var nameWithoutExt = Path.GetFileNameWithoutExtension(romPath);

        // 1. Exact match: gameName.png, gameName.jpg, etc.
        foreach (var ext in SidecarImageExtensions)
        {
            var candidate = Path.Combine(dir, nameWithoutExt + ext);
            if (File.Exists(candidate))
            {
                return candidate;
            }
        }

        // 2. Generic names in the same folder: cover.png, boxart.jpg, etc.
        foreach (var baseName in SidecarFolderImageNames)
        {
            foreach (var ext in SidecarImageExtensions)
            {
                var candidate = Path.Combine(dir, baseName + ext);
                if (File.Exists(candidate))
                {
                    return candidate;
                }
            }
        }

        return null;
    }

    private List<GameSummary> GetGamesInSystem(string systemDir, string libraryId)
    {
        var results = new List<GameSummary>();
        var systemName = Path.GetFileName(systemDir);

        void ProcessFile(string file)
        {
            if (GameSystemCoreResolver.IsRomFile(file))
            {
                var fileInfo = new FileInfo(file);
                var token = GamePathResolver.EncodeToken(file);
                var core = GameSystemCoreResolver.ResolveCore(systemName, fileInfo.Name);

                string? boxartUrl = null;
                var localBoxart = FindLocalBoxart(file);
                if (localBoxart != null)
                {
                    boxartUrl = $"/Vantafyn/Games/{Uri.EscapeDataString(libraryId)}/Games/{Uri.EscapeDataString(token)}/Boxart";
                }
                else
                {
                    boxartUrl = GameSystemCoreResolver.GetLibretroBoxartUrl(core, fileInfo.Name);
                }

                results.Add(new GameSummary
                {
                    Id = token,
                    Title = GameSystemCoreResolver.CleanGameTitle(fileInfo.Name),
                    System = systemName,
                    Core = core,
                    FileName = fileInfo.Name,
                    SizeBytes = fileInfo.Length,
                    BoxartUrl = boxartUrl
                });
            }
        }

        foreach (var file in SafeEnumerateFiles(systemDir))
        {
            ProcessFile(file);
        }

        // Also check nested 1-level subfolders (e.g. System/GameName/Game.rom)
        foreach (var subDir in SafeEnumerateDirectories(systemDir))
        {
            foreach (var file in SafeEnumerateFiles(subDir))
            {
                ProcessFile(file);
            }
        }

        return results;
    }

    public object GetDiagnostics()
    {
        var config = Plugin.Instance?.Configuration;
        return new
        {
            gamesEnabled = config?.GamesEnabled ?? false,
            customGamesPath = config?.CustomGamesPath,
            configuredIds = config?.GameLibraryIds ?? new List<string>(),
            virtualFolders = _libraryManager.GetVirtualFolders().Select(f => new
            {
                name = f.Name,
                itemId = f.ItemId,
                locations = f.Locations,
                locationsExist = (f.Locations ?? Array.Empty<string>()).Select(Directory.Exists).ToArray()
            }).ToList(),
            resolvedLibraries = GetGameLibraries().Select(l => new
            {
                l.Id,
                l.Name,
                l.Locations
            }).ToList()
        };
    }

    private static IEnumerable<string> SafeEnumerateDirectories(string path)
    {
        try
        {
            return Directory.EnumerateDirectories(path);
        }
        catch
        {
            return Enumerable.Empty<string>();
        }
    }

    private static IEnumerable<string> SafeEnumerateFiles(string path)
    {
        try
        {
            return Directory.EnumerateFiles(path);
        }
        catch
        {
            return Enumerable.Empty<string>();
        }
    }
}

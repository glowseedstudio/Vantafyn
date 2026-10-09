using System;
using System.Collections.Generic;
using System.IO;

namespace Vantafyn.Plugin.Companion.Games;

/// <summary>
/// Resolves playable game files and maps console systems and ROM extensions to canonical Libretro cores.
/// </summary>
public static class GameSystemCoreResolver
{
    public const string DefaultCore = "nes";

    private static readonly IReadOnlyDictionary<string, string> ExtensionToCore =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            [".nes"] = "nes",
            [".fds"] = "nes",
            [".unf"] = "nes",
            [".sfc"] = "snes",
            [".smc"] = "snes",
            [".fig"] = "snes",
            [".gb"] = "gb",
            [".gbc"] = "gb",
            [".gba"] = "gba",
            [".agb"] = "gba",
            [".md"] = "segaMD",
            [".gen"] = "segaMD",
            [".smd"] = "segaMD",
            [".bin"] = "segaMD",
            [".sms"] = "segaMS",
            [".gg"] = "segaGG",
            [".sg"] = "segaMS",
            [".n64"] = "n64",
            [".z64"] = "n64",
            [".v64"] = "n64",
            [".nds"] = "nds",
            [".dsi"] = "nds",
            [".3ds"] = "citra",
            [".3dsx"] = "citra",
            [".cci"] = "citra",
            [".cxi"] = "citra",
            [".cia"] = "citra",
            [".vb"] = "vb",
            [".a26"] = "atari2600",
            [".a78"] = "atari7800",
            [".lnx"] = "lynx",
            [".ws"] = "ws",
            [".wsc"] = "ws",
            [".ngp"] = "ngp",
            [".ngc"] = "ngp",
            [".pce"] = "pce",
            [".sgx"] = "pce",
            [".chd"] = "psx",
            [".pbp"] = "psx",
            [".cue"] = "psx",
            [".cso"] = "psp",
            [".iso"] = "psp",
        };

    private static readonly IReadOnlyDictionary<string, string> SystemNameToCore =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["nes"] = "nes",
            ["famicom"] = "nes",
            ["nintendo"] = "nes",
            ["snes"] = "snes",
            ["superfamicom"] = "snes",
            ["supernintendo"] = "snes",
            ["super nintendo"] = "snes",
            ["gb"] = "gb",
            ["gameboy"] = "gb",
            ["game boy"] = "gb",
            ["gbc"] = "gb",
            ["gameboycolor"] = "gb",
            ["game boy color"] = "gb",
            ["gba"] = "gba",
            ["gameboyadvance"] = "gba",
            ["game boy advance"] = "gba",
            ["genesis"] = "segaMD",
            ["megadrive"] = "segaMD",
            ["mega drive"] = "segaMD",
            ["segagenesis"] = "segaMD",
            ["sega genesis"] = "segaMD",
            ["mastersystem"] = "segaMS",
            ["master system"] = "segaMS",
            ["sms"] = "segaMS",
            ["gamegear"] = "segaGG",
            ["game gear"] = "segaGG",
            ["gg"] = "segaGG",
            ["n64"] = "n64",
            ["nintendo64"] = "n64",
            ["nintendo 64"] = "n64",
            ["nds"] = "nds",
            ["nintendods"] = "nds",
            ["nintendo ds"] = "nds",
            ["3ds"] = "citra",
            ["n3ds"] = "citra",
            ["nintendo3ds"] = "citra",
            ["nintendo 3ds"] = "citra",
            ["citra"] = "citra",
            ["azahar"] = "citra",
            ["virtualboy"] = "vb",
            ["virtual boy"] = "vb",
            ["atari2600"] = "atari2600",
            ["atari 2600"] = "atari2600",
            ["atari7800"] = "atari7800",
            ["atari 7800"] = "atari7800",
            ["lynx"] = "lynx",
            ["wonderswan"] = "ws",
            ["neogeopocket"] = "ngp",
            ["neo geo pocket"] = "ngp",
            ["pcengine"] = "pce",
            ["pc engine"] = "pce",
            ["turbografx16"] = "pce",
            ["turbografx-16"] = "pce",
            ["psx"] = "psx",
            ["ps1"] = "psx",
            ["psone"] = "psx",
            ["playstation"] = "psx",
            ["psp"] = "psp",
            ["playstationportable"] = "psp",
            ["playstation portable"] = "psp",
            ["mame"] = "arcade",
            ["arcade"] = "arcade",
            ["fbneo"] = "arcade",
        };

    private static readonly HashSet<string> BiosExtensions =
        new(StringComparer.OrdinalIgnoreCase)
        {
            ".bin",
            ".rom",
            ".bios",
            ".dat"
        };

    private static readonly HashSet<string> ArchiveExtensions =
        new(StringComparer.OrdinalIgnoreCase)
        {
            ".zip",
            ".7z"
        };

    public static bool IsArchive(string path)
    {
        var ext = Path.GetExtension(path);
        return !string.IsNullOrEmpty(ext) && ArchiveExtensions.Contains(ext);
    }

    public static bool IsRomFile(string path)
    {
        var ext = Path.GetExtension(path);
        return !string.IsNullOrEmpty(ext) && (ExtensionToCore.ContainsKey(ext) || ArchiveExtensions.Contains(ext));
    }

    public static bool IsKnownRomExtension(string path)
    {
        var ext = Path.GetExtension(path);
        return !string.IsNullOrEmpty(ext) && ExtensionToCore.ContainsKey(ext);
    }

    public static bool IsBiosFile(string path)
    {
        var ext = Path.GetExtension(path);
        return !string.IsNullOrEmpty(ext) && BiosExtensions.Contains(ext);
    }

    public static string ResolveCore(string systemName, string? fileName = null)
    {
        // 1. Try file extension if provided
        if (!string.IsNullOrEmpty(fileName))
        {
            var ext = Path.GetExtension(fileName);
            if (!string.IsNullOrEmpty(ext) && ExtensionToCore.TryGetValue(ext, out var coreFromExt))
            {
                return coreFromExt;
            }
        }

        // 2. Try system directory name
        var normalizedSystem = systemName.Trim();
        if (SystemNameToCore.TryGetValue(normalizedSystem, out var coreFromSystem))
        {
            return coreFromSystem;
        }

        // 3. Fallback: try stripping spaces and special chars
        var condensed = normalizedSystem.Replace(" ", "").Replace("-", "").Replace("_", "");
        if (SystemNameToCore.TryGetValue(condensed, out var coreFromCondensed))
        {
            return coreFromCondensed;
        }

        return DefaultCore;
    }

    public static string CleanGameTitle(string fileName)
    {
        var nameWithoutExt = Path.GetFileNameWithoutExtension(fileName);
        if (string.IsNullOrWhiteSpace(nameWithoutExt))
        {
            return fileName;
        }

        // Remove release tags like (USA), [!], (En,Fr,De), (v1.1), etc. for a clean presentation
        var cleaned = System.Text.RegularExpressions.Regex.Replace(
            nameWithoutExt,
            @"(\s*[\(\[][^\)\]]*[\)\]])",
            "").Trim();

        return string.IsNullOrWhiteSpace(cleaned) ? nameWithoutExt : cleaned;
    }

    private static readonly IReadOnlyDictionary<string, string> CoreToLibretroPlatform =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["nes"] = "Nintendo - Nintendo Entertainment System",
            ["snes"] = "Nintendo - Super Nintendo Entertainment System",
            ["gb"] = "Nintendo - Game Boy",
            ["gba"] = "Nintendo - Game Boy Advance",
            ["n64"] = "Nintendo - Nintendo 64",
            ["nds"] = "Nintendo - Nintendo DS",
            ["3ds"] = "Nintendo - Nintendo 3DS",
            ["citra"] = "Nintendo - Nintendo 3DS",
            ["azahar"] = "Nintendo - Nintendo 3DS",
            ["vb"] = "Nintendo - Virtual Boy",
            ["segaMD"] = "Sega - Mega Drive - Genesis",
            ["segaMS"] = "Sega - Master System - Mark III",
            ["segaGG"] = "Sega - Game Gear",
            ["atari2600"] = "Atari - 2600",
            ["atari7800"] = "Atari - 7800",
            ["lynx"] = "Atari - Lynx",
            ["ws"] = "Bandai - WonderSwan",
            ["ngp"] = "SNK - Neo Geo Pocket Color",
            ["pce"] = "NEC - PC Engine - TurboGrafx 16",
            ["psx"] = "Sony - PlayStation",
            ["psp"] = "Sony - PlayStation Portable",
            ["arcade"] = "FBNeo - Arcade Games",
        };

    public static string? GetLibretroPlatform(string core)
    {
        return CoreToLibretroPlatform.TryGetValue(core, out var platform) ? platform : null;
    }

    public static string? GetLibretroBoxartUrl(string core, string fileName)
    {
        var platform = GetLibretroPlatform(core);
        if (string.IsNullOrEmpty(platform) || string.IsNullOrEmpty(fileName))
        {
            return null;
        }

        var nameWithoutExt = Path.GetFileNameWithoutExtension(fileName);
        if (string.IsNullOrEmpty(nameWithoutExt))
        {
            return null;
        }

        // Libretro thumbnail repository matches by escaped name without extension via fast global CDN
        var repoName = platform.Replace(" ", "_");
        return $"https://cdn.jsdelivr.net/gh/libretro-thumbnails/{repoName}@master/Named_Boxarts/{Uri.EscapeDataString(nameWithoutExt)}.png";
    }

    public static string? ResolveSystemLogoUrl(string systemName)
    {
        var core = ResolveCore(systemName);
        if (string.Equals(core, "citra", StringComparison.OrdinalIgnoreCase))
        {
            return "https://raw.githubusercontent.com/batocera-linux/batocera-themes/master/themes/batocera/3ds/_data/svg/logo.svg";
        }

        var themeFolder = core switch
        {
            "gba" => "gba",
            "snes" => "snes",
            "nes" => "nes",
            "n64" => "n64",
            "gb" => "gb",
            "nds" => "nds",
            "psx" => "psx",
            "psp" => "psp",
            "segaMD" => "genesis",
            "segaMS" => "mastersystem",
            "segaGG" => "gamegear",
            "dreamcast" => "dreamcast",
            "atari2600" => "atari2600",
            "atari7800" => "atari7800",
            "arcade" => "arcade",
            "ngp" => "neogeo",
            "ws" => "wonderswan",
            "pce" => "pcengine",
            _ => null
        };

        return themeFolder != null
            ? $"https://raw.githubusercontent.com/RetroPie/es-theme-carbon/master/{themeFolder}/art/system.svg"
            : null;
    }
}

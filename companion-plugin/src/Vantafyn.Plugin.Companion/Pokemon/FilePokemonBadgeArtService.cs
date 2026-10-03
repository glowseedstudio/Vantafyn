using System.Text.RegularExpressions;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Pokemon;

public sealed partial class FilePokemonBadgeArtService(ICompanionPaths paths) : IPokemonBadgeArtService
{
    private static readonly string[] ImageExtensions = [".png", ".webp", ".jpg", ".jpeg"];

    private static readonly PokemonBadgeDefinition[] Definitions =
    [
        new("kanto", "Kanto", 1, "boulder", "Boulder Badge", 1),
        new("kanto", "Kanto", 1, "cascade", "Cascade Badge", 2),
        new("kanto", "Kanto", 1, "thunder", "Thunder Badge", 3),
        new("kanto", "Kanto", 1, "rainbow", "Rainbow Badge", 4),
        new("kanto", "Kanto", 1, "soul", "Soul Badge", 5),
        new("kanto", "Kanto", 1, "marsh", "Marsh Badge", 6),
        new("kanto", "Kanto", 1, "volcano", "Volcano Badge", 7),
        new("kanto", "Kanto", 1, "earth", "Earth Badge", 8),

        new("johto", "Johto", 2, "zephyr", "Zephyr Badge", 1),
        new("johto", "Johto", 2, "hive", "Hive Badge", 2),
        new("johto", "Johto", 2, "plain", "Plain Badge", 3),
        new("johto", "Johto", 2, "fog", "Fog Badge", 4),
        new("johto", "Johto", 2, "storm", "Storm Badge", 5),
        new("johto", "Johto", 2, "mineral", "Mineral Badge", 6),
        new("johto", "Johto", 2, "glacier", "Glacier Badge", 7),
        new("johto", "Johto", 2, "rising", "Rising Badge", 8),

        new("hoenn", "Hoenn", 3, "stone", "Stone Badge", 1),
        new("hoenn", "Hoenn", 3, "knuckle", "Knuckle Badge", 2),
        new("hoenn", "Hoenn", 3, "dynamo", "Dynamo Badge", 3),
        new("hoenn", "Hoenn", 3, "heat", "Heat Badge", 4),
        new("hoenn", "Hoenn", 3, "balance", "Balance Badge", 5),
        new("hoenn", "Hoenn", 3, "feather", "Feather Badge", 6),
        new("hoenn", "Hoenn", 3, "mind", "Mind Badge", 7),
        new("hoenn", "Hoenn", 3, "rain", "Rain Badge", 8),
    ];

    public PokemonBadgeArtCatalogDto GetCatalog(PokemonConfiguration config, string imageUrlBase)
    {
        var root = ResolveRoot(config);
        var configured = root != null && Directory.Exists(root);
        var safeBase = imageUrlBase.TrimEnd('/');

        var regions = Definitions
            .GroupBy(d => new { d.RegionId, d.RegionName, d.Generation })
            .Select(regionGroup =>
            {
                var badges = regionGroup
                    .OrderBy(d => d.Order)
                    .Select(d =>
                    {
                        var available = configured && ResolveImagePath(root!, d.RegionId, d.Id) != null;
                        return new PokemonBadgeArtDto
                        {
                            Id = d.Id,
                            Name = d.Name,
                            Region = d.RegionId,
                            Generation = d.Generation,
                            Order = d.Order,
                            Available = available,
                            ImageUrl = available ? $"{safeBase}/{d.RegionId}/{d.Id}/Image" : null
                        };
                    })
                    .ToList();

                return new PokemonBadgeRegionDto
                {
                    Id = regionGroup.Key.RegionId,
                    Name = regionGroup.Key.RegionName,
                    Generation = regionGroup.Key.Generation,
                    AvailableCount = badges.Count(b => b.Available),
                    TotalCount = badges.Count,
                    Badges = badges
                };
            })
            .ToList();

        return new PokemonBadgeArtCatalogDto
        {
            Configured = configured,
            AvailableCount = regions.Sum(r => r.AvailableCount),
            TotalCount = regions.Sum(r => r.TotalCount),
            Regions = regions
        };
    }

    public PokemonBadgeArtFile? ResolveImage(PokemonConfiguration config, string regionId, string badgeId)
    {
        if (!SlugRegex().IsMatch(regionId) || !SlugRegex().IsMatch(badgeId))
        {
            return null;
        }

        if (!Definitions.Any(d => d.RegionId == regionId && d.Id == badgeId))
        {
            return null;
        }

        var root = ResolveRoot(config);
        if (root == null || !Directory.Exists(root))
        {
            return null;
        }

        var path = ResolveImagePath(root, regionId, badgeId);
        if (path == null)
        {
            return null;
        }

        return new PokemonBadgeArtFile(path, ContentTypeFor(path));
    }

    private string? ResolveRoot(PokemonConfiguration config)
    {
        var raw = config.BadgeArtPath?.Trim().Trim('"', '\'');
        if (string.IsNullOrWhiteSpace(raw))
        {
            raw = Path.Combine(paths.PokemonRoot, "badge-art");
        }

        try
        {
            return Path.GetFullPath(raw);
        }
        catch
        {
            return null;
        }
    }

    private static string? ResolveImagePath(string root, string regionId, string badgeId)
    {
        var rootFull = Path.GetFullPath(root);
        foreach (var ext in ImageExtensions)
        {
            var candidate = Path.GetFullPath(Path.Combine(rootFull, regionId, badgeId + ext));
            if (!IsInside(candidate, rootFull) || !File.Exists(candidate))
            {
                continue;
            }

            return candidate;
        }

        return null;
    }

    private static bool IsInside(string candidate, string root)
    {
        var rootWithSeparator = root.EndsWith(Path.DirectorySeparatorChar)
            ? root
            : root + Path.DirectorySeparatorChar;
        return candidate.StartsWith(rootWithSeparator, StringComparison.Ordinal) ||
            string.Equals(candidate, root, StringComparison.Ordinal);
    }

    private static string ContentTypeFor(string path) =>
        Path.GetExtension(path).ToLowerInvariant() switch
        {
            ".jpg" or ".jpeg" => "image/jpeg",
            ".webp" => "image/webp",
            _ => "image/png"
        };

    [GeneratedRegex("^[a-z0-9-]{1,40}$", RegexOptions.CultureInvariant)]
    private static partial Regex SlugRegex();

    private sealed record PokemonBadgeDefinition(
        string RegionId,
        string RegionName,
        int Generation,
        string Id,
        string Name,
        int Order);
}

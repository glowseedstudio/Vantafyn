using Vantafyn.Plugin.Companion.Pokemon;
using Vantafyn.Plugin.Companion.Core;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonBadgeArtServiceTests : IDisposable
{
    private readonly string _tempDir;
    private readonly TestCompanionPaths _paths;

    public PokemonBadgeArtServiceTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "VantafynBadgeArtTests_" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(_tempDir);
        _paths = new TestCompanionPaths(_tempDir);
    }

    public void Dispose()
    {
        if (Directory.Exists(_tempDir))
        {
            try { Directory.Delete(_tempDir, recursive: true); } catch { }
        }
    }

    [Fact]
    public void Catalog_ReturnsAllGenOneToThreeBadgeDefinitions()
    {
        var service = new FilePokemonBadgeArtService(_paths);
        var catalog = service.GetCatalog(new PokemonConfiguration(), "/Vantafyn/Pokemon/Badges");

        Assert.Equal(24, catalog.TotalCount);
        Assert.Equal(3, catalog.Regions.Count);
        Assert.Contains(catalog.Regions, r => r.Id == "kanto" && r.TotalCount == 8);
        Assert.Contains(catalog.Regions, r => r.Id == "johto" && r.TotalCount == 8);
        Assert.Contains(catalog.Regions, r => r.Id == "hoenn" && r.TotalCount == 8);
        Assert.Equal(0, catalog.AvailableCount);
    }

    [Fact]
    public void Catalog_MarksConfiguredImagesAvailable()
    {
        var root = Path.Combine(_tempDir, "badges");
        Directory.CreateDirectory(Path.Combine(root, "kanto"));
        File.WriteAllBytes(Path.Combine(root, "kanto", "boulder.png"), [0x89, 0x50, 0x4E, 0x47]);

        var service = new FilePokemonBadgeArtService(_paths);
        var config = new PokemonConfiguration { BadgeArtPath = root };
        var catalog = service.GetCatalog(config, "/Vantafyn/Pokemon/Badges");

        var boulder = catalog.Regions.Single(r => r.Id == "kanto").Badges.Single(b => b.Id == "boulder");
        Assert.True(catalog.Configured);
        Assert.Equal(1, catalog.AvailableCount);
        Assert.True(boulder.Available);
        Assert.Equal("/Vantafyn/Pokemon/Badges/kanto/boulder/Image", boulder.ImageUrl);
    }

    [Fact]
    public void ResolveImage_ToleratesCaseAndPrettyBadgeFileNames()
    {
        var root = Path.Combine(_tempDir, "badges");
        Directory.CreateDirectory(Path.Combine(root, "Kanto"));
        var expected = Path.Combine(root, "Kanto", "Boulder Badge.png");
        File.WriteAllBytes(expected, [0x89, 0x50, 0x4E, 0x47]);

        var service = new FilePokemonBadgeArtService(_paths);
        var config = new PokemonConfiguration { BadgeArtPath = root };

        var catalog = service.GetCatalog(config, "/Vantafyn/Pokemon/Badges");
        var resolved = service.ResolveImage(config, "kanto", "boulder");

        Assert.Equal(1, catalog.AvailableCount);
        Assert.NotNull(resolved);
        Assert.Equal(expected, resolved.Path);
    }

    [Fact]
    public void ResolveImage_OnlyAllowsKnownBadgeSlugsInsideConfiguredRoot()
    {
        var root = Path.Combine(_tempDir, "badges");
        Directory.CreateDirectory(Path.Combine(root, "johto"));
        var expected = Path.Combine(root, "johto", "rising.png");
        File.WriteAllBytes(expected, [0x89, 0x50, 0x4E, 0x47]);

        var service = new FilePokemonBadgeArtService(_paths);
        var config = new PokemonConfiguration { BadgeArtPath = root };

        var resolved = service.ResolveImage(config, "johto", "rising");

        Assert.NotNull(resolved);
        Assert.Equal(expected, resolved.Path);
        Assert.Equal("image/png", resolved.ContentType);
        Assert.Null(service.ResolveImage(config, "johto", "../rising"));
        Assert.Null(service.ResolveImage(config, "johto", "not-a-real-badge"));
    }

    private sealed class TestCompanionPaths(string root) : ICompanionPaths
    {
        public string DataRoot => root;
        public string UserSettingsRoot => Path.Combine(root, "user-settings");
        public string PersonalPlaylistsRoot => Path.Combine(root, "personal-playlists");
        public string OmbiSessionsRoot => Path.Combine(root, "ombi-sessions");
        public string SecretsRoot => Path.Combine(root, "secrets");
        public string PushRegistrationsRoot => Path.Combine(root, "push-registrations");
        public string GameSavesRoot => Path.Combine(root, "game-saves");
        public string PokemonRoot => Path.Combine(root, "pokemon");
    }
}

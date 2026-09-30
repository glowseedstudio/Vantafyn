using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Pokemon;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonGameDetectorTests
{
    [Theory]
    [InlineData("Pokemon - Emerald Version (USA, Europe)", "Pokemon - Emerald Version.gba", "GBA", "emerald", 3, "gba")]
    [InlineData("Pokemon - FireRed Version (USA)", "Pokemon - FireRed.gba", "GBA", "firered", 3, "gba")]
    [InlineData("Pokemon - Crystal Version", "Pokemon Crystal.gbc", "GBC", "crystal", 2, "gbc")]
    [InlineData("Pokemon - Red Version", "Pokemon Red.gb", "GB", "red", 1, "gb")]
    [InlineData("Pokemon - Platinum Version (USA)", "Pokemon Platinum.nds", "NDS", "platinum", 4, "nds")]
    [InlineData("Pokemon - Black Version (USA)", "Pokemon Black.nds", "NDS", "black", 5, "nds")]
    [InlineData("Pokemon - Black Version 2 (USA)", "Pokemon Black 2.nds", "NDS", "black2", 5, "nds")]
    public void Detect_IdentifiesKnownPokemonGames(
        string title,
        string fileName,
        string system,
        string expectedId,
        int expectedGen,
        string expectedPlatform)
    {
        using var temp = new TempTestPaths();
        var detector = new PokemonGameDetector(temp);

        var meta = detector.Detect(title, fileName, system);

        Assert.NotNull(meta);
        Assert.True(meta.IsPokemonGame);
        Assert.Equal(expectedId, meta.PokemonGameId);
        Assert.Equal(expectedGen, meta.Generation);
        Assert.Equal(expectedPlatform, meta.Platform);
        Assert.True(meta.VaultSupported);
    }

    [Theory]
    [InlineData("Super Mario World", "Super Mario World.sfc", "SNES")]
    [InlineData("The Legend of Zelda - The Minish Cap", "Zelda Minish Cap.gba", "GBA")]
    [InlineData("Metroid Fusion", "Metroid Fusion.gba", "GBA")]
    [InlineData("Sonic The Hedgehog", "Sonic.bin", "Genesis")]
    [InlineData("Chrono Trigger", "Chrono Trigger.sfc", "SNES")]
    public void Detect_IgnoresNonPokemonGames(string title, string fileName, string system)
    {
        using var temp = new TempTestPaths();
        var detector = new PokemonGameDetector(temp);

        var meta = detector.Detect(title, fileName, system);

        Assert.Null(meta);
    }

    [Fact]
    public async Task GetMetadata_DetectsSavePresence()
    {
        using var temp = new TempTestPaths();
        var detector = new PokemonGameDetector(temp);
        var userId = Guid.NewGuid();
        var gameId = "emerald-token-123";

        // Without save file
        var meta1 = await detector.GetMetadataAsync(userId, gameId, "Pokemon Emerald", "Pokemon Emerald.gba", "GBA");
        Assert.NotNull(meta1);
        Assert.False(meta1.HasSave);

        // Create mock save file
        var userSaveDir = Path.Combine(temp.GameSavesRoot, userId.ToString("N"));
        Directory.CreateDirectory(userSaveDir);
        var savePath = Path.Combine(userSaveDir, $"{gameId}.sram");
        await File.WriteAllBytesAsync(savePath, new byte[1024]);

        // With save file
        var meta2 = await detector.GetMetadataAsync(userId, gameId, "Pokemon Emerald", "Pokemon Emerald.gba", "GBA");
        Assert.NotNull(meta2);
        Assert.True(meta2.HasSave);
    }

    [Fact]
    public async Task AdminOverride_TakesPrecedence()
    {
        using var temp = new TempTestPaths();
        var detector = new PokemonGameDetector(temp);
        var gameId = "custom-rom-hack-456";

        // Unrecognized ROM hack initially
        var initial = await detector.GetMetadataAsync(null, gameId, "Radical Red v4.0", "RadicalRed.gba", "GBA");
        Assert.Null(initial);

        // Add override mapping
        await detector.SetOverrideAsync(gameId, new PokemonGameOverride
        {
            GameId = gameId,
            IsPokemonGame = true,
            PokemonGameId = "firered",
            Generation = 3,
            Platform = "gba",
            CanonicalTitle = "Pokémon Radical Red (FR Hack)"
        }, CancellationToken.None);

        // Query with override
        var overridden = await detector.GetMetadataAsync(null, gameId, "Radical Red v4.0", "RadicalRed.gba", "GBA");
        Assert.NotNull(overridden);
        Assert.Equal("firered", overridden.PokemonGameId);
        Assert.Equal("override", overridden.DetectionConfidence);
        Assert.Equal("Pokémon Radical Red (FR Hack)", overridden.CanonicalTitle);

        // Can also remove override
        await detector.RemoveOverrideAsync(gameId, CancellationToken.None);
        var afterRemoval = await detector.GetMetadataAsync(null, gameId, "Radical Red v4.0", "RadicalRed.gba", "GBA");
        Assert.Null(afterRemoval);
    }

    private sealed class TempTestPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-pokemon-tests", Guid.NewGuid().ToString("N"));
        public string DataRoot => _root;
        public string UserSettingsRoot => Ensure("user-settings");
        public string PersonalPlaylistsRoot => Ensure("personal-playlists");
        public string OmbiSessionsRoot => Ensure("ombi-sessions");
        public string SecretsRoot => Ensure("secrets");
        public string PushRegistrationsRoot => Ensure("push-registrations");
        public string GameSavesRoot => Ensure("game-saves");
        public string PokemonRoot => Ensure("pokemon");

        private string Ensure(string name) => Directory.CreateDirectory(Path.Combine(_root, name)).FullName;

        public void Dispose()
        {
            if (Directory.Exists(_root)) Directory.Delete(_root, recursive: true);
        }
    }
}

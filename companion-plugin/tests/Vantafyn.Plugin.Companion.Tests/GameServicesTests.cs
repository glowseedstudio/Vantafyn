using System;
using System.IO;
using System.IO.Compression;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class GameServicesTests
{
    [Fact]
    public void SystemCoreResolver_MapsExtensionsCorrectly()
    {
        Assert.Equal("nes", GameSystemCoreResolver.ResolveCore("NES", "smb.nes"));
        Assert.Equal("snes", GameSystemCoreResolver.ResolveCore("SNES", "smw.sfc"));
        Assert.Equal("snes", GameSystemCoreResolver.ResolveCore("SNES", "smw.smc"));
        Assert.Equal("gba", GameSystemCoreResolver.ResolveCore("GBA", "pokemon.gba"));
        Assert.Equal("gb", GameSystemCoreResolver.ResolveCore("GB", "tetris.gb"));
        Assert.Equal("gb", GameSystemCoreResolver.ResolveCore("GBC", "zelda.gbc"));
        Assert.Equal("segaMD", GameSystemCoreResolver.ResolveCore("Genesis", "sonic.md"));
        Assert.Equal("segaMD", GameSystemCoreResolver.ResolveCore("MegaDrive", "sonic.gen"));
        Assert.Equal("n64", GameSystemCoreResolver.ResolveCore("N64", "mario64.z64"));
        Assert.Equal("nds", GameSystemCoreResolver.ResolveCore("NDS", "mario_kart.nds"));
        Assert.Equal("psx", GameSystemCoreResolver.ResolveCore("PS1", "crash.chd"));
        Assert.Equal("psp", GameSystemCoreResolver.ResolveCore("PSP", "god_of_war.cso"));
        Assert.Equal("arcade", GameSystemCoreResolver.ResolveCore("Arcade", "pacman.zip"));
    }

    [Fact]
    public void SystemCoreResolver_MapsSystemNamesWhenNoExtensionGiven()
    {
        Assert.Equal("snes", GameSystemCoreResolver.ResolveCore("Super Nintendo"));
        Assert.Equal("gba", GameSystemCoreResolver.ResolveCore("Game Boy Advance"));
        Assert.Equal("n64", GameSystemCoreResolver.ResolveCore("Nintendo 64"));
        Assert.Equal("segaMD", GameSystemCoreResolver.ResolveCore("Sega Genesis"));
        Assert.Equal("psx", GameSystemCoreResolver.ResolveCore("PlayStation"));
    }

    [Fact]
    public void SystemCoreResolver_CleansGameTitles()
    {
        Assert.Equal("Super Mario World", GameSystemCoreResolver.CleanGameTitle("Super Mario World (USA).sfc"));
        Assert.Equal("Pokemon - Emerald Version", GameSystemCoreResolver.CleanGameTitle("Pokemon - Emerald Version (USA, Europe) [!].gba"));
        Assert.Equal("The Legend of Zelda - Ocarina of Time", GameSystemCoreResolver.CleanGameTitle("The Legend of Zelda - Ocarina of Time (USA) (Rev 1).z64"));
    }

    [Fact]
    public void PathResolver_EncodesAndDecodesTokensSafely()
    {
        var samplePath = "/media/games/SNES/Super Mario World.sfc";
        var token = GamePathResolver.EncodeToken(samplePath);

        Assert.False(string.IsNullOrWhiteSpace(token));
        Assert.DoesNotContain("/", token);
        Assert.DoesNotContain("+", token);

        var decoded = GamePathResolver.DecodeToken(token);
        Assert.Equal(samplePath, decoded);
    }

    [Fact]
    public void PathResolver_EnforcesPathContainment()
    {
        var libRoot = Path.Combine(Path.GetTempPath(), "test-lib-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(libRoot);

        try
        {
            var library = new GameLibrary
            {
                Id = "lib-1",
                Name = "Games",
                Locations = new List<string> { libRoot }
            };

            var validPath = Path.Combine(libRoot, "SNES", "game.sfc");
            var traversalPath = Path.Combine(libRoot, "..", "secret.txt");

            Assert.True(GamePathResolver.IsWithinLibrary(library, validPath));
            Assert.False(GamePathResolver.IsWithinLibrary(library, traversalPath));
        }
        finally
        {
            if (Directory.Exists(libRoot)) Directory.Delete(libRoot, true);
        }
    }

    [Fact]
    public async Task GameSavesService_SavesAndLoadsUserIsolatedSaves()
    {
        using var temp = new TempGamePaths();
        var service = new GameSavesService(temp);

        var userA = Guid.NewGuid();
        var userB = Guid.NewGuid();
        var gameId = "game-token-12345";

        var stateDataA = Encoding.UTF8.GetBytes("SaveStateUserData_UserA_Level4");
        var sramDataA = Encoding.UTF8.GetBytes("BatteryRam_UserA_HighScores");

        // Save for User A
        await service.SaveAsync(userA, gameId, "state", stateDataA, CancellationToken.None);
        await service.SaveAsync(userA, gameId, "sram", sramDataA, CancellationToken.None);

        // Load for User A
        var loadedStateA = await service.GetAsync(userA, gameId, "state", CancellationToken.None);
        var loadedSramA = await service.GetAsync(userA, gameId, "sram", CancellationToken.None);

        Assert.NotNull(loadedStateA);
        Assert.Equal("SaveStateUserData_UserA_Level4", Encoding.UTF8.GetString(loadedStateA!));
        Assert.NotNull(loadedSramA);
        Assert.Equal("BatteryRam_UserA_HighScores", Encoding.UTF8.GetString(loadedSramA!));

        // User B should have no saves for this game
        var loadedStateB = await service.GetAsync(userB, gameId, "state", CancellationToken.None);
        Assert.Null(loadedStateB);

        // Delete User A save
        var deleted = await service.DeleteAsync(userA, gameId, "state", CancellationToken.None);
        Assert.True(deleted);
        Assert.Null(await service.GetAsync(userA, gameId, "state", CancellationToken.None));

        // User A's sram should still exist
        Assert.NotNull(await service.GetAsync(userA, gameId, "sram", CancellationToken.None));
    }

    [Fact]
    public void GamesService_ExtractsSingleRomFromZip()
    {
        var tempZip = Path.Combine(Path.GetTempPath(), $"test-rom-{Guid.NewGuid():N}.zip");
        try
        {
            var romContent = Encoding.UTF8.GetBytes("Simulated NES ROM byte header and PRG data");
            using (var zip = ZipFile.Open(tempZip, ZipArchiveMode.Create))
            {
                var entry = zip.CreateEntry("Super Mario Bros.nes");
                using var entryStream = entry.Open();
                entryStream.Write(romContent, 0, romContent.Length);
            }

            var info = GamesService.GetExtractedRomInfo(tempZip);
            Assert.NotNull(info);
            Assert.Equal("Super Mario Bros.nes", info!.Name);
            Assert.Equal(romContent.Length, info.Length);

            var extracted = GamesService.ExtractRomFromArchive(tempZip);
            Assert.NotNull(extracted);
            Assert.Equal(romContent, extracted);
        }
        finally
        {
            if (File.Exists(tempZip)) File.Delete(tempZip);
        }
    }

    [Fact]
    public void SystemCoreResolver_GeneratesLibretroBoxartUrl()
    {
        var url = GameSystemCoreResolver.GetLibretroBoxartUrl("snes", "Super Mario World (USA).sfc");
        Assert.NotNull(url);
        Assert.Equal(
            "https://thumbnails.libretro.com/Nintendo%20-%20Super%20Nintendo%20Entertainment%20System/Named_Boxarts/Super%20Mario%20World%20%28USA%29.png",
            url);
    }

    [Fact]
    public void GamesService_FindsLocalSidecarBoxart()
    {
        var tempDir = Path.Combine(Path.GetTempPath(), "vantafyn-boxart-test-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(tempDir);
        try
        {
            var romPath = Path.Combine(tempDir, "Chrono Trigger (USA).sfc");
            File.WriteAllText(romPath, "dummy rom");

            // Without sidecar image
            Assert.Null(GamesService.FindLocalBoxart(romPath));

            // With exact sidecar image
            var imagePath = Path.Combine(tempDir, "Chrono Trigger (USA).png");
            File.WriteAllBytes(imagePath, new byte[] { 1, 2, 3 });

            var found = GamesService.FindLocalBoxart(romPath);
            Assert.NotNull(found);
            Assert.Equal(imagePath, found);
        }
        finally
        {
            if (Directory.Exists(tempDir)) Directory.Delete(tempDir, recursive: true);
        }
    }

    private sealed class TempGamePaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-games-tests", Guid.NewGuid().ToString("N"));
        public string DataRoot => _root;
        public string UserSettingsRoot => Directory.CreateDirectory(Path.Combine(_root, "user-settings")).FullName;
        public string PersonalPlaylistsRoot => Directory.CreateDirectory(Path.Combine(_root, "personal-playlists")).FullName;
        public string OmbiSessionsRoot => Directory.CreateDirectory(Path.Combine(_root, "ombi-sessions")).FullName;
        public string SecretsRoot => Directory.CreateDirectory(Path.Combine(_root, "secrets")).FullName;
        public string PushRegistrationsRoot => Directory.CreateDirectory(Path.Combine(_root, "push-registrations")).FullName;
        public string GameSavesRoot => Directory.CreateDirectory(Path.Combine(_root, "game-saves")).FullName;

        public void Dispose()
        {
            if (Directory.Exists(_root)) Directory.Delete(_root, recursive: true);
        }
    }
}

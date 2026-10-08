using System.Net;
using System.Security.Cryptography;
using System.Text.Json;
using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;
using Vantafyn.Plugin.Companion.Pokemon;
using Vantafyn.Plugin.Companion.Pokemon.Native;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonMysteryGiftTests
{
    private readonly Guid _userId = Guid.NewGuid();

    [Fact]
    public void Catalog_Find_MatchesByCodeAliasesAndNormalizes()
    {
        var byCode = PokemonMysteryGiftCatalog.Find("MANAPHY-EGG");
        Assert.NotNull(byCode);
        Assert.Equal("gen4-manaphy-egg", byCode.Id);

        var byAlias = PokemonMysteryGiftCatalog.Find("manaphy");
        Assert.NotNull(byAlias);
        Assert.Equal("gen4-manaphy-egg", byAlias.Id);

        var noHyphens = PokemonMysteryGiftCatalog.Find("libertypass");
        Assert.NotNull(noHyphens);
        Assert.Equal("bw-liberty-pass", noHyphens.Id);

        var lowercaseVictini = PokemonMysteryGiftCatalog.Find("victini");
        Assert.NotNull(lowercaseVictini);
        Assert.Equal("bw-liberty-pass", lowercaseVictini.Id);

        var sumDialga = PokemonMysteryGiftCatalog.Find("sum2013 dialga");
        Assert.NotNull(sumDialga);
        Assert.True(sumDialga.IsShiny);

        var invalid = PokemonMysteryGiftCatalog.Find("NONEXISTENT-CODE-12345");
        Assert.Null(invalid);
    }

    [Fact]
    public void Catalog_SupportsGame_CorrectlyIdentifiesCompatibility()
    {
        var libertyPass = PokemonMysteryGiftCatalog.Find("LIBERTY-PASS")!;
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(libertyPass, "pokemon_black_version"));
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(libertyPass, "white2"));
        Assert.False(PokemonMysteryGiftCatalog.SupportsGame(libertyPass, "platinum"));

        var memberCard = PokemonMysteryGiftCatalog.Find("MEMBER-CARD")!;
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(memberCard, "pokemon_platinum"));
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(memberCard, "diamond"));
        Assert.False(PokemonMysteryGiftCatalog.SupportsGame(memberCard, "black"));

        var surfPika = PokemonMysteryGiftCatalog.Find("SURF-PIKA")!;
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(surfPika, "heartgold"));
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(surfPika, "platinum"));

        // Gen 6
        var torchic = PokemonMysteryGiftCatalog.Find("XY-TORCHIC")!;
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(torchic, "pokemon_x"));
        Assert.False(PokemonMysteryGiftCatalog.SupportsGame(torchic, "omegaruby"));

        var beldum = PokemonMysteryGiftCatalog.Find("ORAS-BELDUM")!;
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(beldum, "alphasapphire"));
        Assert.False(PokemonMysteryGiftCatalog.SupportsGame(beldum, "pokemon_y"));

        // Gen 7
        var magearna = PokemonMysteryGiftCatalog.Find("MAGEARNA-QR")!;
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(magearna, "pokemon_sun"));
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(magearna, "ultramoon"));
        Assert.False(PokemonMysteryGiftCatalog.SupportsGame(magearna, "omegaruby"));

        var rockruff = PokemonMysteryGiftCatalog.Find("DUSK-ROCKRUFF")!;
        Assert.True(PokemonMysteryGiftCatalog.SupportsGame(rockruff, "ultrasun"));
        Assert.False(PokemonMysteryGiftCatalog.SupportsGame(rockruff, "pokemon_sun"));
    }

    [Fact]
    public void Catalog_CreateVaultEntry_GeneratesAuthenticData()
    {
        var surfPika = PokemonMysteryGiftCatalog.Find("SURF-PIKA")!;
        var entry = PokemonMysteryGiftCatalog.CreateVaultEntry(surfPika, "Pokémon Platinum", 4);

        Assert.Equal("Pikachu", entry.Species);
        Assert.Equal(25, entry.SpeciesId);
        Assert.Equal(50, entry.Level);
        Assert.Equal("PKTOPIA", entry.OriginalTrainer);
        Assert.NotNull(entry.Details);
        Assert.Equal("Light Ball", entry.Details.HeldItem);
        Assert.Equal("Cherish Ball", entry.Details.Pokeball);
        Assert.Contains("Surf", entry.Details.Moves);
        Assert.Contains("Volt Tackle", entry.Details.Moves);

        var shinyDialga = PokemonMysteryGiftCatalog.Find("SUM2013-DIALGA")!;
        var dialgaEntry = PokemonMysteryGiftCatalog.CreateVaultEntry(shinyDialga, "Pokémon Black", 5);
        Assert.Equal("Dialga", dialgaEntry.Species);
        Assert.True(dialgaEntry.IsShiny);
        Assert.Equal(100, dialgaEntry.Level);
        Assert.Equal("SUM2013", dialgaEntry.OriginalTrainer);
        Assert.Contains("Roar of Time", dialgaEntry.Details!.Moves);

        // Gen 6 Vault Entry
        var torchicGift = PokemonMysteryGiftCatalog.Find("XY-TORCHIC")!;
        var torchicEntry = PokemonMysteryGiftCatalog.CreateVaultEntry(torchicGift, "Pokémon X", 6);
        Assert.Equal("Torchic", torchicEntry.Species);
        Assert.Equal(10, torchicEntry.Level);
        Assert.Equal("Blazikenite", torchicEntry.Details!.HeldItem);
        Assert.Equal("Speed Boost", torchicEntry.Details.Ability);
        Assert.Equal("XY", torchicEntry.OriginalTrainer);

        var beldumGift = PokemonMysteryGiftCatalog.Find("ORAS-BELDUM")!;
        var beldumEntry = PokemonMysteryGiftCatalog.CreateVaultEntry(beldumGift, "Pokémon Omega Ruby", 6);
        Assert.True(beldumEntry.IsShiny);
        Assert.Equal("Metagrossite", beldumEntry.Details!.HeldItem);

        // Gen 7 Vault Entry
        var magearnaGift = PokemonMysteryGiftCatalog.Find("MAGEARNA-QR")!;
        var magearnaEntry = PokemonMysteryGiftCatalog.CreateVaultEntry(magearnaGift, "Pokémon Sun", 7);
        Assert.Equal("Magearna", magearnaEntry.Species);
        Assert.Equal(801, magearnaEntry.SpeciesId);
        Assert.Equal("Silver Bottle Cap", magearnaEntry.Details!.HeldItem);
        Assert.Contains("Fleur Cannon", magearnaEntry.Details.Moves);

        var greninjaGift = PokemonMysteryGiftCatalog.Find("ASH-GRENINJA")!;
        var greninjaEntry = PokemonMysteryGiftCatalog.CreateVaultEntry(greninjaGift, "Pokémon Moon", 7);
        Assert.Equal("Battle Bond", greninjaEntry.Details!.Ability);
        Assert.Equal(36, greninjaEntry.Level);

        var marshadowGift = PokemonMysteryGiftCatalog.Find("MT-TENSEI-MARSHADOW")!;
        var marshadowEntry = PokemonMysteryGiftCatalog.CreateVaultEntry(marshadowGift, "Pokémon Ultra Sun", 7);
        Assert.Equal("Marshadium Z", marshadowEntry.Details!.HeldItem);
    }

    [Fact]
    public void Gen6SaveParser_OrasEonTicket_UnlocksProperly()
    {
        var save = new byte[0x76000]; // 483,328 bytes ORAS
        Assert.False(Gen6SaveParser.IsEventUnlocked(save, "oras", PokemonEventCatalog.OrasEonTicket));

        var updated = Gen6SaveParser.UnlockEvent(save, "oras", PokemonEventCatalog.OrasEonTicket);
        Assert.True(Gen6SaveParser.IsEventUnlocked(updated, "oras", PokemonEventCatalog.OrasEonTicket));

        // Check key items offset directly
        ushort itemId = (ushort)(updated[0x00A40] | (updated[0x00A41] << 8));
        ushort count = (ushort)(updated[0x00A42] | (updated[0x00A43] << 8));
        Assert.Equal(726, itemId);
        Assert.Equal(1, count);
    }

    [Fact]
    public void PokemonEventCatalog_Supports3dsEvents()
    {
        var orasEvent = PokemonEventCatalog.Find(PokemonEventCatalog.OrasEonTicket);
        Assert.NotNull(orasEvent);
        Assert.True(PokemonEventCatalog.SupportsGame(orasEvent, "pokemon_omega_ruby"));
        Assert.False(PokemonEventCatalog.SupportsGame(orasEvent, "pokemon_x"));

        var magearnaEvent = PokemonEventCatalog.Find(PokemonEventCatalog.Gen7MagearnaDelivery);
        Assert.NotNull(magearnaEvent);
        Assert.True(PokemonEventCatalog.SupportsGame(magearnaEvent, "pokemon_ultra_moon"));
        Assert.False(PokemonEventCatalog.SupportsGame(magearnaEvent, "pokemon_omega_ruby"));
    }

    [Fact]
    public void Gen4SaveParser_EnableMysteryGift_SetsFlagAndRecalculatesChecksum()
    {
        var save = new byte[0x80000];
        int generalSize = 0xCF2C; // Platinum general block size
        BitConverter.GetBytes(100u).CopyTo(save, generalSize - 0x14);

        Assert.Equal(0, save[0x48]);

        var updated = Gen4SaveParser.EnableMysteryGift(save, "platinum");
        Assert.Equal(1, updated[0x48]);

        // Verify CRC16 recalculated properly
        ushort expectedCrc = Gen4SaveParser.CalculateCrc16(updated, 0, generalSize - 0x14);
        ushort storedCrc = BitConverter.ToUInt16(updated, generalSize - 0x02);
        Assert.Equal(expectedCrc, storedCrc);
    }

    [Fact]
    public async Task Controller_RedeemMysteryGift_KeyItemEvent_PatchesSaveAndUnlocksEvent()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonMysteryGiftRedeemRequest
        {
            GameId = "platinum",
            Code = "MEMBER-CARD"
        };

        var actionResult = await harness.Controller.RedeemMysteryGift(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonMysteryGiftRedeemResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.Equal("MEMBER-CARD", response.Code);
        Assert.Equal("Member Card", response.Title);
        Assert.Equal("EventItem", response.RewardType);
        Assert.Contains("Key Items", response.InGameInstructions);

        // Check that the save was updated with the Member Card
        var currentSaveBytes = await harness.SavesService.GetAsync(_userId, "platinum", "sram", CancellationToken.None);
        Assert.NotNull(currentSaveBytes);
        Assert.True(Gen4SaveParser.IsEventUnlocked(currentSaveBytes, "platinum", PokemonEventCatalog.Gen4MemberCard));
        // And Mystery Gift flag at 0x48 was set
        Assert.Equal(1, currentSaveBytes[0x48]);
    }

    [Fact]
    public async Task Controller_RedeemMysteryGift_PokemonReward_AddsToPersonalVault()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonMysteryGiftRedeemRequest
        {
            GameId = "platinum",
            Code = "SURF-PIKA"
        };

        var actionResult = await harness.Controller.RedeemMysteryGift(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonMysteryGiftRedeemResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.Equal("SURF-PIKA", response.Code);
        Assert.Equal("Surfing Pikachu", response.Title);
        Assert.Equal("Pokemon", response.RewardType);

        // Verify entry added to user's personal vault
        var vault = await harness.VaultStore.GetOrCreateVaultAsync(_userId, CancellationToken.None);
        var pikaEntry = vault.Boxes.SelectMany(b => b.Entries).FirstOrDefault(e => e.Species == "Pikachu");
        Assert.NotNull(pikaEntry);
        Assert.Equal("PKTOPIA", pikaEntry.OriginalTrainer);
    }

    [Fact]
    public async Task Controller_RedeemMysteryGift_DuplicateRedeem_ReturnsAlreadyRedeemed()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonMysteryGiftRedeemRequest
        {
            GameId = "platinum",
            Code = "OAKS-LETTER"
        };

        // First redemption
        var firstResult = await harness.Controller.RedeemMysteryGift(request, CancellationToken.None);
        var firstOk = Assert.IsType<OkObjectResult>(firstResult.Result);
        var firstResp = Assert.IsType<PokemonMysteryGiftRedeemResponse>(firstOk.Value);
        Assert.True(firstResp.Success);

        // Second redemption (should recognize already redeemed)
        var secondResult = await harness.Controller.RedeemMysteryGift(request, CancellationToken.None);
        var secondOk = Assert.IsType<OkObjectResult>(secondResult.Result);
        var secondResp = Assert.IsType<PokemonMysteryGiftRedeemResponse>(secondOk.Value);
        Assert.True(secondResp.Success);
        Assert.Contains("already been redeemed", secondResp.Message);
    }

    [Fact]
    public async Task Controller_RedeemMysteryGift_InvalidCode_ReturnsBadRequest()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonMysteryGiftRedeemRequest
        {
            GameId = "platinum",
            Code = "TOTALLY-BOGUS-CODE"
        };

        var actionResult = await harness.Controller.RedeemMysteryGift(request, CancellationToken.None);
        Assert.IsType<BadRequestObjectResult>(actionResult.Result);
    }

    [Fact]
    public async Task Controller_RedeemMysteryGift_OrasEonTicket_PatchesSaveAndUnlocksEvent()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonMysteryGiftRedeemRequest
        {
            GameId = "oras",
            Code = "EON-TICKET"
        };

        var actionResult = await harness.Controller.RedeemMysteryGift(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonMysteryGiftRedeemResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.Equal("EventItem", response.RewardType);

        // Verify save was patched with Eon Ticket (726)
        var saveBytes = await harness.SavesService.GetAsync(_userId, "oras", "sram", CancellationToken.None);
        Assert.NotNull(saveBytes);
        Assert.True(Gen6SaveParser.IsEventUnlocked(saveBytes, "oras", PokemonEventCatalog.OrasEonTicket));
    }

    [Fact]
    public async Task Controller_RedeemMysteryGift_Gen7Magearna_AddsToPersonalVault()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonMysteryGiftRedeemRequest
        {
            GameId = "sun",
            Code = "MAGEARNA-QR"
        };

        var actionResult = await harness.Controller.RedeemMysteryGift(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonMysteryGiftRedeemResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.Equal("Pokemon", response.RewardType);
        Assert.Equal("Magearna", response.TargetSpeciesName);

        // Verify entry added to Personal Vault
        var vault = await harness.VaultStore.GetOrCreateVaultAsync(_userId, CancellationToken.None);
        var magearnaEntry = vault.Boxes.SelectMany(b => b.Entries).FirstOrDefault(e => e.Species == "Magearna");
        Assert.NotNull(magearnaEntry);
        Assert.Equal(801, magearnaEntry.SpeciesId);
        Assert.Equal("Silver Bottle Cap", magearnaEntry.Details?.HeldItem);
    }

    [Fact]
    public async Task Controller_UnlockEvent_Gen7MagearnaDelivery_AddsToVaultAndMarksRedeemed()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonEventUnlockRequest
        {
            GameId = "sun",
            EventId = PokemonEventCatalog.Gen7MagearnaDelivery
        };

        var actionResult = await harness.Controller.UnlockEvent(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonEventUnlockResponse>(okResult.Value);

        Assert.True(response.Success);

        // Check event status is unlocked
        var statusResult = await harness.Controller.GetEventUnlockStatus("sun", CancellationToken.None);
        var statusOk = Assert.IsType<OkObjectResult>(statusResult.Result);
        var statuses = Assert.IsAssignableFrom<IEnumerable<PokemonEventUnlockStatusDto>>(statusOk.Value);
        var magearnaStatus = statuses.FirstOrDefault(s => s.EventId == PokemonEventCatalog.Gen7MagearnaDelivery);
        Assert.NotNull(magearnaStatus);
        Assert.True(magearnaStatus.Unlocked);
    }

    private sealed class TestHarness : IDisposable
    {
        private readonly TempPaths _tempPaths = new();
        public GameSavesService SavesService { get; }
        public FilePokemonVaultStore VaultStore { get; }
        public PokemonController Controller { get; }

        public TestHarness(Guid userId)
        {
            var clock = new ManualClock();
            SavesService = new GameSavesService(_tempPaths);
            var sessionTracker = new InMemoryGameSessionTracker(clock);
            var coordinator = new FileSaveOperationCoordinator(_tempPaths, clock, SavesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
            var backupService = new FilePokemonBackupService(_tempPaths, clock, SavesService, NullLogger<FilePokemonBackupService>.Instance);
            var txManager = new PokemonTransactionManager(_tempPaths, clock, SavesService, coordinator, backupService, NullLogger<PokemonTransactionManager>.Instance);
            VaultStore = new FilePokemonVaultStore(_tempPaths, clock);

            var generalSize = 0xCF2C;
            var platinumSave = new byte[0x80000];
            BitConverter.GetBytes(100u).CopyTo(platinumSave, generalSize - 0x14);
            ushort crc = Gen4SaveParser.CalculateCrc16(platinumSave, 0, generalSize - 0x14);
            BitConverter.GetBytes(crc).CopyTo(platinumSave, generalSize - 0x02);

            SavesService.SaveAsync(userId, "platinum", "sram", platinumSave, CancellationToken.None).GetAwaiter().GetResult();

            var blackSave = new byte[0x80000];
            SavesService.SaveAsync(userId, "black", "sram", blackSave, CancellationToken.None).GetAwaiter().GetResult();

            var orasSave = new byte[0x76000];
            SavesService.SaveAsync(userId, "oras", "sram", orasSave, CancellationToken.None).GetAwaiter().GetResult();

            var sunSave = new byte[0x6BE00];
            SavesService.SaveAsync(userId, "sun", "sram", sunSave, CancellationToken.None).GetAwaiter().GetResult();

            var games = new List<GameDetail>
            {
                new()
                {
                    Id = "platinum",
                    Title = "Pokémon Platinum Version",
                    FileName = "platinum.nds",
                    System = "nds",
                    Core = "melonds"
                },
                new()
                {
                    Id = "oras",
                    Title = "Pokémon Omega Ruby",
                    FileName = "omegaruby.3ds",
                    System = "3ds",
                    Core = "citra"
                },
                new()
                {
                    Id = "sun",
                    Title = "Pokémon Sun",
                    FileName = "sun.3ds",
                    System = "3ds",
                    Core = "citra"
                }
            };

            var gamesService = new TestGamesService(games);
            var detector = new PokemonGameDetector(_tempPaths);
            var authContext = new TestAuthorizationContext(userId);

            var config = new PokemonConfiguration { Enabled = true };
            var provider = new TestPokemonProvider();
            var factory = new TestPokemonProviderFactory(provider);

            Controller = new PokemonController(
                gamesService,
                SavesService,
                detector,
                VaultStore,
                factory,
                coordinator,
                sessionTracker,
                backupService,
                txManager,
                authContext,
                configuration: config,
                paths: _tempPaths
            );

            Controller.ControllerContext = new ControllerContext
            {
                HttpContext = new DefaultHttpContext()
            };
        }

        public void Dispose() => _tempPaths.Dispose();
    }

    private sealed class TestGamesService(IReadOnlyList<GameDetail> games) : IGamesService
    {
        public TestGamesService(GameDetail single) : this([single]) { }

        public IReadOnlyList<GameLibrary> GetGameLibraries() =>
            [new GameLibrary { Id = "lib1", Name = "Games", Locations = ["/tmp"] }];

        public IReadOnlyList<GameSystem> GetSystems(string libraryId) => [];
        public IReadOnlyList<GameSummary> GetGames(string libraryId, string? system = null) => games.Cast<GameSummary>().ToList();
        public GameDetail? GetGame(string libraryId, string gameId) => games.FirstOrDefault(g => g.Id == gameId);
        public string? ResolveFilePath(string libraryId, string token, bool allowBios) => null;
        public string? GetBoxartPath(string libraryId, string gameId) => null;
        public byte[]? ExtractRomFromArchive(string archivePath) => null;
        public ExtractedRomInfo? GetExtractedRomInfo(string archivePath) => null;
        public object GetDiagnostics() => new();
    }

    private sealed class TestAuthorizationContext(Guid userId) : IAuthorizationContext
    {
        public Task<AuthorizationInfo> GetAuthorizationInfo(HttpRequest request) =>
            Task.FromResult(CreateAuth(userId));

        public Task<AuthorizationInfo> GetAuthorizationInfo(HttpContext httpContext) =>
            Task.FromResult(CreateAuth(userId));

        private static AuthorizationInfo CreateAuth(Guid userId)
        {
            var auth = (AuthorizationInfo)System.Runtime.CompilerServices.RuntimeHelpers.GetUninitializedObject(typeof(AuthorizationInfo));
            var userProp = typeof(AuthorizationInfo).GetProperty("User");
            if (userProp != null)
            {
                var userType = userProp.PropertyType;
                var user = System.Runtime.CompilerServices.RuntimeHelpers.GetUninitializedObject(userType);
                userType.GetProperty("Id")?.SetValue(user, userId);
                var idField = userType.GetField("<Id>k__BackingField", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic);
                idField?.SetValue(user, userId);

                foreach (var field in userType.GetFields(System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance))
                {
                    if (field.FieldType == typeof(Guid))
                    {
                        field.SetValue(user, userId);
                    }
                }

                if (userProp.CanWrite)
                {
                    userProp.SetValue(auth, user);
                }
                else
                {
                    var userField = typeof(AuthorizationInfo).GetField("<User>k__BackingField", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic);
                    userField?.SetValue(auth, user);
                }
            }
            return auth;
        }
    }

    private sealed class ManualClock : IClock
    {
        private DateTimeOffset _current = new(2026, 9, 30, 12, 0, 0, TimeSpan.Zero);
        public DateTimeOffset UtcNow => _current;
        public void Advance(TimeSpan duration) => _current = _current.Add(duration);
    }

    private sealed class TempPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-mystery-gift-tests", Guid.NewGuid().ToString("N"));
        public string DataRoot => _root;
        public string UserSettingsRoot => Directory.CreateDirectory(Path.Combine(_root, "user-settings")).FullName;
        public string PersonalPlaylistsRoot => Directory.CreateDirectory(Path.Combine(_root, "personal-playlists")).FullName;
        public string OmbiSessionsRoot => Directory.CreateDirectory(Path.Combine(_root, "ombi-sessions")).FullName;
        public string SecretsRoot => Directory.CreateDirectory(Path.Combine(_root, "secrets")).FullName;
        public string PushRegistrationsRoot => Directory.CreateDirectory(Path.Combine(_root, "push-registrations")).FullName;
        public string GameSavesRoot => Directory.CreateDirectory(Path.Combine(_root, "game-saves")).FullName;
        public string PokemonRoot => Directory.CreateDirectory(Path.Combine(_root, "pokemon")).FullName;

        public TempPaths()
        {
            Directory.CreateDirectory(_root);
        }

        public void Dispose()
        {
            try
            {
                if (Directory.Exists(_root))
                {
                    Directory.Delete(_root, recursive: true);
                }
            }
            catch
            {
                // Best-effort cleanup
            }
        }
    }

    private sealed class TestPokemonProviderFactory(IPokemonProvider provider) : IPokemonProviderFactory
    {
        public IPokemonProvider Create(PokemonConfiguration config) => provider;
    }

    private sealed class TestPokemonProvider : IPokemonProvider
    {
        public string ProviderName => "mock";

        public Task<PokemonConnectionTestResult> TestConnectionAsync(CancellationToken cancellationToken) =>
            Task.FromResult(new PokemonConnectionTestResult { IsSuccess = true });

        public Task<PokemonProviderCapabilities> GetCapabilitiesAsync(CancellationToken cancellationToken) =>
            Task.FromResult(new PokemonProviderCapabilities());

        public Task<PokemonSaveParseResult> ParseSaveAsync(byte[] saveBytes, string pokemonGameId, string platform, int generation, CancellationToken cancellationToken) =>
            Task.FromResult(new PokemonSaveParseResult { IsSuccess = true });

        public Task<PokemonExtractResult> ExtractPokemonFromSaveAsync(
            byte[] saveBytes, string pokemonGameId, string platform, int generation,
            string pokemonId, bool isInParty, int? boxIndex, int slotIndex, CancellationToken cancellationToken) =>
            Task.FromResult(new PokemonExtractResult { IsSuccess = true });

        public Task<PokemonInjectResult> InjectPokemonIntoSaveAsync(
            byte[] saveBytes, string pokemonGameId, string platform, int generation,
            PokemonVaultEntry entry, int? targetBoxIndex, int? targetSlotIndex, bool targetParty, CancellationToken cancellationToken) =>
            Task.FromResult(new PokemonInjectResult { IsSuccess = true, UpdatedSaveBytes = saveBytes });
    }
}

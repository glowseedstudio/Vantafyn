using System.Net;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;
using Vantafyn.Plugin.Companion.Pokemon;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonDirectTransferAndMigrationTests
{
    private readonly Guid _userId = Guid.NewGuid();

    [Fact]
    public void ValidateTransfer_SameGeneration_ReturnsCompatible()
    {
        var source = new GamePokemonMetadata { Generation = 3, CanonicalTitle = "Pokémon Emerald", VaultSupported = true };
        var dest = new GamePokemonMetadata { Generation = 3, CanonicalTitle = "Pokémon FireRed", VaultSupported = true };
        var config = new PokemonConfiguration { Enabled = true, AllowTransfers = true };

        var result = PokemonMigrationValidator.ValidateTransfer(source, dest, config);

        Assert.True(result.IsCompatible);
        Assert.False(result.IsCrossGeneration);
        Assert.Equal("same_generation", result.Direction);
        Assert.Contains("same-generation", result.Reason);
    }

    [Fact]
    public void ValidateTransfer_ForwardMigration_Allowed_WhenConfigEnabled()
    {
        var source = new GamePokemonMetadata { Generation = 3, CanonicalTitle = "Pokémon Emerald", VaultSupported = true };
        var dest = new GamePokemonMetadata { Generation = 4, CanonicalTitle = "Pokémon Platinum", VaultSupported = true };
        var config = new PokemonConfiguration { Enabled = true, AllowTransfers = true, AllowCrossGenerationTransfers = true };

        var result = PokemonMigrationValidator.ValidateTransfer(source, dest, config);

        Assert.True(result.IsCompatible);
        Assert.True(result.IsCrossGeneration);
        Assert.Equal("forward_migration", result.Direction);
        Assert.Contains("Pal Park", result.Reason);
        Assert.Contains(result.Warnings, w => w.Contains("Pal Park"));
    }

    [Fact]
    public void ValidateTransfer_ForwardMigration_Blocked_WhenConfigDisabled()
    {
        var source = new GamePokemonMetadata { Generation = 3, CanonicalTitle = "Pokémon Emerald", VaultSupported = true };
        var dest = new GamePokemonMetadata { Generation = 4, CanonicalTitle = "Pokémon Platinum", VaultSupported = true };
        var config = new PokemonConfiguration { Enabled = true, AllowTransfers = true, AllowCrossGenerationTransfers = false };

        var result = PokemonMigrationValidator.ValidateTransfer(source, dest, config);

        Assert.False(result.IsCompatible);
        Assert.True(result.IsCrossGeneration);
        Assert.Contains("disabled in server configuration", result.Reason);
    }

    [Fact]
    public void ValidateTransfer_BackwardTransfer_Blocked()
    {
        var source = new GamePokemonMetadata { Generation = 4, CanonicalTitle = "Pokémon Platinum", VaultSupported = true };
        var dest = new GamePokemonMetadata { Generation = 3, CanonicalTitle = "Pokémon Emerald", VaultSupported = true };
        var config = new PokemonConfiguration { Enabled = true, AllowTransfers = true, AllowCrossGenerationTransfers = true };

        var result = PokemonMigrationValidator.ValidateTransfer(source, dest, config);

        Assert.False(result.IsCompatible);
        Assert.Equal("unsupported_backward", result.Direction);
        Assert.Contains("not supported", result.Reason);
    }

    [Fact]
    public void ValidateTransfer_TimeCapsule_Gen2ToGen1_AllowedForGen1Species()
    {
        var source = new GamePokemonMetadata { Generation = 2, CanonicalTitle = "Pokémon Crystal", VaultSupported = true };
        var dest = new GamePokemonMetadata { Generation = 1, CanonicalTitle = "Pokémon Red", VaultSupported = true };
        var config = new PokemonConfiguration { Enabled = true, AllowTransfers = true, AllowCrossGenerationTransfers = true };

        var pikachu = new PokemonDetailsDto
        {
            Summary = new PokemonSummaryDto { SpeciesId = 25, Species = "Pikachu" }
        };

        var result = PokemonMigrationValidator.ValidateTransfer(source, dest, config, pikachu);

        Assert.True(result.IsCompatible);
        Assert.Equal("time_capsule_backward", result.Direction);
        Assert.Contains("Time Capsule", result.Reason);
    }

    [Fact]
    public void ValidateTransfer_TimeCapsule_Gen2ToGen1_BlockedForGen2Species()
    {
        var source = new GamePokemonMetadata { Generation = 2, CanonicalTitle = "Pokémon Crystal", VaultSupported = true };
        var dest = new GamePokemonMetadata { Generation = 1, CanonicalTitle = "Pokémon Red", VaultSupported = true };
        var config = new PokemonConfiguration { Enabled = true, AllowTransfers = true, AllowCrossGenerationTransfers = true };

        var togepi = new PokemonDetailsDto
        {
            Summary = new PokemonSummaryDto { SpeciesId = 175, Species = "Togepi" }
        };

        var result = PokemonMigrationValidator.ValidateTransfer(source, dest, config, togepi);

        Assert.False(result.IsCompatible);
        Assert.Contains("does not exist in Generation 1", result.Reason);
    }

    [Fact]
    public async Task DirectTransfer_SameGeneration_Success_MutatesBothSavesAndCreatesBackups()
    {
        using var harness = new TestTransferHarness(_userId);

        var request = new PokemonDirectTransferRequest
        {
            SourceGameId = "emerald",
            DestinationGameId = "firered",
            PokemonId = "p25_pikachu",
            SourceIsInParty = false,
            SourceBoxIndex = 1,
            SourceSlotIndex = 1,
            TargetBoxIndex = 1,
            TargetSlotIndex = 5,
            TargetParty = false
        };

        var actionResult = await harness.Controller.DirectTransfer(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.Equal("DirectTransfer", response.Operation);
        Assert.Equal("emerald", response.SourceGameId);
        Assert.Equal("firered", response.DestinationGameId);

        // Verify source save on disk was updated (Pokémon removed)
        var sourceBytesOnDisk = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(harness.MutatedSourceBytes, sourceBytesOnDisk);

        // Verify destination save on disk was updated (Pokémon injected)
        var destBytesOnDisk = await harness.SavesService.GetAsync(_userId, "firered", "sram", CancellationToken.None);
        Assert.Equal(harness.MutatedDestBytes, destBytesOnDisk);

        // Verify pre-mutation backups created for both saves
        var sourceBackups = await harness.BackupService.ListBackupsAsync(_userId, "emerald", CancellationToken.None);
        Assert.NotEmpty(sourceBackups);

        var destBackups = await harness.BackupService.ListBackupsAsync(_userId, "firered", CancellationToken.None);
        Assert.NotEmpty(destBackups);
    }

    [Fact]
    public async Task DirectTransfer_ForwardPalParkMigration_Success()
    {
        using var harness = new TestTransferHarness(_userId);

        var request = new PokemonDirectTransferRequest
        {
            SourceGameId = "emerald",
            DestinationGameId = "platinum",
            PokemonId = "p25_pikachu",
            SourceIsInParty = false,
            SourceBoxIndex = 1,
            SourceSlotIndex = 1,
            TargetBoxIndex = 1,
            TargetSlotIndex = 1
        };

        var actionResult = await harness.Controller.DirectTransfer(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.Equal("CrossGenTransfer", response.Operation);
        Assert.True(response.IsCrossGeneration);
        Assert.NotNull(response.Warnings);
        Assert.Contains(response.Warnings, w => w.Contains("Pal Park"));
    }

    [Fact]
    public async Task DirectTransfer_RollsBackBothSaves_WhenInjectionFails()
    {
        using var harness = new TestTransferHarness(_userId, simulateInjectionFailure: true);

        var request = new PokemonDirectTransferRequest
        {
            SourceGameId = "emerald",
            DestinationGameId = "firered",
            PokemonId = "p25_pikachu"
        };

        var actionResult = await harness.Controller.DirectTransfer(request, CancellationToken.None);
        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(badRequest.Value);

        Assert.False(response.Success);

        // DUAL ROLLBACK VERIFICATION: Both saves MUST match their pristine initial states
        var sourceBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalSourceBytes, sourceBytes);

        var destBytes = await harness.SavesService.GetAsync(_userId, "firered", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalDestBytes, destBytes);
    }

    [Fact]
    public async Task DirectTransfer_Blocked_WhenDestinationSessionIsActivelyRunning()
    {
        using var harness = new TestTransferHarness(_userId);

        // Active game session on destination
        harness.SessionTracker.StartSession(_userId, "firered", "switch-client");

        var request = new PokemonDirectTransferRequest
        {
            SourceGameId = "emerald",
            DestinationGameId = "firered",
            PokemonId = "p25_pikachu"
        };

        var actionResult = await harness.Controller.DirectTransfer(request, CancellationToken.None);
        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(badRequest.Value);

        Assert.False(response.Success);
        Assert.Contains("Finish and exit firered", response.Message);

        // Neither save was touched
        var sourceBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalSourceBytes, sourceBytes);

        var destBytes = await harness.SavesService.GetAsync(_userId, "firered", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalDestBytes, destBytes);
    }

    private sealed class TestTransferHarness : IDisposable
    {
        private readonly TempPaths _tempPaths = new();
        public readonly byte[] OriginalSourceBytes = [0x50, 0x4B, 0x11, 0x11, 0x11, 0x11];
        public readonly byte[] MutatedSourceBytes = [0x50, 0x4B, 0x00, 0x00, 0x11, 0x11];

        public readonly byte[] OriginalDestBytes = [0x50, 0x4B, 0x22, 0x22, 0x22, 0x22];
        public readonly byte[] MutatedDestBytes = [0x50, 0x4B, 0xFF, 0xFF, 0x22, 0x22];

        public GameSavesService SavesService { get; }
        public InMemoryGameSessionTracker SessionTracker { get; }
        public FileSaveOperationCoordinator Coordinator { get; }
        public FilePokemonBackupService BackupService { get; }
        public PokemonTransactionManager TransactionManager { get; }
        public FilePokemonVaultStore VaultStore { get; }
        public PokemonController Controller { get; }

        public TestTransferHarness(Guid userId, bool simulateInjectionFailure = false)
        {
            var clock = new ManualClock();
            SavesService = new GameSavesService(_tempPaths);
            SessionTracker = new InMemoryGameSessionTracker(clock);
            Coordinator = new FileSaveOperationCoordinator(_tempPaths, clock, SavesService, SessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
            BackupService = new FilePokemonBackupService(_tempPaths, clock, SavesService, NullLogger<FilePokemonBackupService>.Instance);
            TransactionManager = new PokemonTransactionManager(_tempPaths, clock, SavesService, Coordinator, BackupService, NullLogger<PokemonTransactionManager>.Instance);
            VaultStore = new FilePokemonVaultStore(_tempPaths, clock);

            // Populate initial saves for both emerald, firered, and platinum
            SavesService.SaveAsync(userId, "emerald", "sram", OriginalSourceBytes, CancellationToken.None).GetAwaiter().GetResult();
            SavesService.SaveAsync(userId, "firered", "sram", OriginalDestBytes, CancellationToken.None).GetAwaiter().GetResult();
            SavesService.SaveAsync(userId, "platinum", "sram", OriginalDestBytes, CancellationToken.None).GetAwaiter().GetResult();

            var emerald = new GameDetail { Id = "emerald", Title = "Pokemon - Emerald Version (USA, Europe)", FileName = "Pokemon - Emerald.gba", System = "gba", Core = "mgba" };
            var firered = new GameDetail { Id = "firered", Title = "Pokemon - FireRed Version (USA, Europe)", FileName = "Pokemon - FireRed.gba", System = "gba", Core = "mgba" };
            var platinum = new GameDetail { Id = "platinum", Title = "Pokemon - Platinum Version (USA)", FileName = "Pokemon - Platinum.nds", System = "nds", Core = "melonds" };

            var gamesService = new TestGamesService([emerald, firered, platinum]);
            var detector = new PokemonGameDetector(_tempPaths);
            var provider = new TestTransferProvider(MutatedSourceBytes, MutatedDestBytes, simulateInjectionFailure);
            var providerFactory = new TestTransferProviderFactory(provider);

            var authContext = new TestAuthorizationContext(userId);
            var config = new PokemonConfiguration
            {
                Enabled = true,
                ProviderType = "pkvault",
                PkVaultBaseUrl = "http://localhost:5000",
                AllowTransfers = true,
                AllowCrossGenerationTransfers = true
            };

            Controller = new PokemonController(
                gamesService,
                SavesService,
                detector,
                VaultStore,
                providerFactory,
                Coordinator,
                SessionTracker,
                BackupService,
                TransactionManager,
                authContext,
                config);

            Controller.ControllerContext = new ControllerContext
            {
                HttpContext = new DefaultHttpContext()
            };
        }

        public void Dispose() => _tempPaths.Dispose();
    }

    private sealed class TestTransferProvider(
        byte[] mutatedSourceBytes,
        byte[] mutatedDestBytes,
        bool failInject) : IPokemonProvider
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
            string pokemonId, bool isInParty, int? boxIndex, int slotIndex, CancellationToken cancellationToken)
        {
            return Task.FromResult(new PokemonExtractResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = mutatedSourceBytes,
                ExtractedPokemon = new PokemonDetailsDto
                {
                    Summary = new PokemonSummaryDto
                    {
                        Id = pokemonId,
                        Species = "Pikachu",
                        SpeciesId = 25,
                        Nickname = "Sparky",
                        Level = 50,
                        Gender = "M",
                        IsShiny = true,
                        OriginalTrainer = "Ash",
                        OriginalTrainerId = "00001",
                        OriginGame = "Pokémon Emerald"
                    },
                    RawData = "base64payload"
                }
            });
        }

        public Task<PokemonInjectResult> InjectPokemonIntoSaveAsync(
            byte[] saveBytes, string pokemonGameId, string platform, int generation,
            PokemonVaultEntry entry, int? targetBoxIndex, int? targetSlotIndex, bool targetParty, CancellationToken cancellationToken)
        {
            if (failInject)
            {
                return Task.FromResult(new PokemonInjectResult { IsSuccess = false, ErrorMessage = "Simulated injection failure." });
            }

            return Task.FromResult(new PokemonInjectResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = mutatedDestBytes,
                AssignedLocation = $"Box {targetBoxIndex ?? 1} Slot {targetSlotIndex ?? 1}"
            });
        }
    }

    private sealed class TestTransferProviderFactory(IPokemonProvider provider) : IPokemonProviderFactory
    {
        public IPokemonProvider Create(PokemonConfiguration config) => provider;
    }

    private sealed class TestGamesService(IReadOnlyList<GameDetail> games) : IGamesService
    {
        public IReadOnlyList<GameLibrary> GetGameLibraries() =>
            [new GameLibrary { Id = "lib1", Name = "Games", Locations = ["/tmp"] }];

        public IReadOnlyList<GameSystem> GetSystems(string libraryId) => [];
        public IReadOnlyList<GameSummary> GetGames(string libraryId, string? system = null) => games;
        public GameDetail? GetGame(string libraryId, string gameId) => games.FirstOrDefault(g => g.Id == gameId);
        public string? ResolveFilePath(string libraryId, string token, bool allowBios) => null;
        public string? GetBoxartPath(string libraryId, string gameId) => null;
        public byte[]? ExtractRomFromArchive(string archivePath) => null;
        public ExtractedRomInfo? GetExtractedRomInfo(string archivePath) => null;
        public object GetDiagnostics() => new();
    }

    private sealed class TestAuthorizationContext(Guid userId) : MediaBrowser.Controller.Net.IAuthorizationContext
    {
        public Task<MediaBrowser.Controller.Net.AuthorizationInfo> GetAuthorizationInfo(HttpRequest request) =>
            Task.FromResult(CreateAuth(userId));

        public Task<MediaBrowser.Controller.Net.AuthorizationInfo> GetAuthorizationInfo(HttpContext httpContext) =>
            Task.FromResult(CreateAuth(userId));

        private static MediaBrowser.Controller.Net.AuthorizationInfo CreateAuth(Guid userId)
        {
            var auth = (MediaBrowser.Controller.Net.AuthorizationInfo)System.Runtime.CompilerServices.RuntimeHelpers.GetUninitializedObject(typeof(MediaBrowser.Controller.Net.AuthorizationInfo));
            var userProp = typeof(MediaBrowser.Controller.Net.AuthorizationInfo).GetProperty("User");
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
                    var userField = typeof(MediaBrowser.Controller.Net.AuthorizationInfo).GetField("<User>k__BackingField", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic);
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
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-transfer-tests", Guid.NewGuid().ToString("N"));
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
}

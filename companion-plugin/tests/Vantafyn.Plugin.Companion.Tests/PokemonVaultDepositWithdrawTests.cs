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
using Vantafyn.Plugin.Companion.Pokemon.PkVault;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonVaultDepositWithdrawTests
{
    private readonly Guid _userId = Guid.NewGuid();

    [Fact]
    public async Task Deposit_Success_TransfersPokemonFromSaveToPersonalVault()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonDepositRequest
        {
            GameId = "emerald",
            PokemonId = "p25_pikachu",
            IsInParty = false,
            BoxIndex = 1,
            SlotIndex = 1,
            TargetVaultBoxIndex = 1,
            TargetVaultSlotIndex = 1
        };

        var actionResult = await harness.Controller.Deposit(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.Equal("Deposit", response.Operation);
        Assert.NotNull(response.VaultEntry);
        Assert.Equal("Sparky", response.VaultEntry.Nickname);
        Assert.Equal("Pikachu", response.VaultEntry.Species);
        Assert.Equal(1, response.VaultEntry.BoxIndex);
        Assert.Equal(1, response.VaultEntry.SlotIndex);

        // Verify save in GameSavesService was updated to the mutated bytes
        var currentSaveBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.NotNull(currentSaveBytes);
        Assert.Equal(harness.MutatedSaveBytes, currentSaveBytes);

        // Verify entry is persisted in user's personal vault
        var vaultEntry = await harness.VaultStore.GetEntryAsync(_userId, "p25_pikachu", CancellationToken.None);
        Assert.NotNull(vaultEntry);
        Assert.Equal("Sparky", vaultEntry.Nickname);

        // Verify backup was created during the transaction
        var backups = await harness.BackupService.ListBackupsAsync(_userId, "emerald", CancellationToken.None);
        Assert.NotEmpty(backups);
    }

    [Fact]
    public async Task Deposit_AutoAssignsNextFreeVaultSlot_WhenSlotNotSpecified()
    {
        using var harness = new TestHarness(_userId);

        // Pre-populate Vault Box 1 Slot 1
        await harness.VaultStore.AddOrUpdateEntryAsync(_userId, new PokemonVaultEntry
        {
            Id = "existing_mon",
            BoxIndex = 1,
            SlotIndex = 1,
            Species = "Bulbasaur",
            Nickname = "Bulby"
        }, CancellationToken.None);

        var request = new PokemonDepositRequest
        {
            GameId = "emerald",
            PokemonId = "p25_pikachu",
            IsInParty = false,
            BoxIndex = 1,
            SlotIndex = 1,
            TargetVaultBoxIndex = 1,
            TargetVaultSlotIndex = null // Auto-assign!
        };

        var actionResult = await harness.Controller.Deposit(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.NotNull(response.VaultEntry);
        Assert.Equal(1, response.VaultEntry.BoxIndex);
        Assert.Equal(2, response.VaultEntry.SlotIndex); // Auto-assigned to next available slot
    }

    [Fact]
    public async Task Deposit_Fails_WhenTargetVaultSlotIsAlreadyOccupied()
    {
        using var harness = new TestHarness(_userId);

        // Pre-occupy slot 1
        await harness.VaultStore.AddOrUpdateEntryAsync(_userId, new PokemonVaultEntry
        {
            Id = "existing_mon",
            BoxIndex = 1,
            SlotIndex = 1,
            Species = "Bulbasaur",
            Nickname = "Bulby"
        }, CancellationToken.None);

        var request = new PokemonDepositRequest
        {
            GameId = "emerald",
            PokemonId = "p25_pikachu",
            TargetVaultBoxIndex = 1,
            TargetVaultSlotIndex = 1 // Already occupied
        };

        var actionResult = await harness.Controller.Deposit(request, CancellationToken.None);
        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        Assert.Contains("occupied", badRequest.Value?.ToString() ?? string.Empty);

        // Verify save was never touched
        var saveBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalSaveBytes, saveBytes);
    }

    [Fact]
    public async Task Withdraw_Success_TransfersPokemonFromPersonalVaultToSave()
    {
        using var harness = new TestHarness(_userId);

        // Place a Pokémon in the vault first
        await harness.VaultStore.AddOrUpdateEntryAsync(_userId, new PokemonVaultEntry
        {
            Id = "vault_charizard",
            BoxIndex = 2,
            SlotIndex = 5,
            Species = "Charizard",
            Nickname = "Flame",
            Level = 80,
            Generation = 3,
            OriginalTrainer = "Red"
        }, CancellationToken.None);

        var request = new PokemonWithdrawRequest
        {
            GameId = "emerald",
            VaultEntryId = "vault_charizard",
            TargetBoxIndex = 1,
            TargetSlotIndex = 10,
            TargetParty = false
        };

        var actionResult = await harness.Controller.Withdraw(request, CancellationToken.None);
        var okResult = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(okResult.Value);

        Assert.True(response.Success);
        Assert.Equal("Withdraw", response.Operation);
        Assert.Equal("Flame", response.Pokemon?.Nickname);

        // Verify removed from personal vault
        var entryAfter = await harness.VaultStore.GetEntryAsync(_userId, "vault_charizard", CancellationToken.None);
        Assert.Null(entryAfter);

        // Verify save was updated
        var currentSaveBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.NotNull(currentSaveBytes);
        Assert.Equal(harness.MutatedSaveBytes, currentSaveBytes);
    }

    [Fact]
    public async Task Withdraw_ReturnsNotFound_WhenPokemonNotInPersonalVault()
    {
        using var harness = new TestHarness(_userId);

        var request = new PokemonWithdrawRequest
        {
            GameId = "emerald",
            VaultEntryId = "non_existent_mon",
            TargetBoxIndex = 1,
            TargetSlotIndex = 1
        };

        var actionResult = await harness.Controller.Withdraw(request, CancellationToken.None);
        Assert.IsType<NotFoundObjectResult>(actionResult.Result);

        // Verify save was not touched
        var currentSaveBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalSaveBytes, currentSaveBytes);
    }

    [Fact]
    public async Task Deposit_RollsBackSaveAndVault_WhenExtractionFails()
    {
        using var harness = new TestHarness(_userId, simulateExtractionFailure: true);

        var request = new PokemonDepositRequest
        {
            GameId = "emerald",
            PokemonId = "p25_pikachu",
            TargetVaultBoxIndex = 1,
            TargetVaultSlotIndex = 1
        };

        var actionResult = await harness.Controller.Deposit(request, CancellationToken.None);
        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(badRequest.Value);

        Assert.False(response.Success);

        // Save bytes MUST match pristine initial state
        var currentSaveBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalSaveBytes, currentSaveBytes);

        // Personal vault MUST NOT contain the entry
        var entryInVault = await harness.VaultStore.GetEntryAsync(_userId, "p25_pikachu", CancellationToken.None);
        Assert.Null(entryInVault);
    }

    [Fact]
    public async Task Withdraw_RollsBackSaveAndPreservesVaultEntry_WhenInjectionFails()
    {
        using var harness = new TestHarness(_userId, simulateInjectionFailure: true);

        // Place Pokémon in vault
        var originalEntry = new PokemonVaultEntry
        {
            Id = "vault_blastoise",
            BoxIndex = 1,
            SlotIndex = 3,
            Species = "Blastoise",
            Nickname = "Shellshock",
            Generation = 3
        };
        await harness.VaultStore.AddOrUpdateEntryAsync(_userId, originalEntry, CancellationToken.None);

        var request = new PokemonWithdrawRequest
        {
            GameId = "emerald",
            VaultEntryId = "vault_blastoise",
            TargetBoxIndex = 1,
            TargetSlotIndex = 1
        };

        var actionResult = await harness.Controller.Withdraw(request, CancellationToken.None);
        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(badRequest.Value);

        Assert.False(response.Success);

        // Save bytes MUST match pristine initial state
        var currentSaveBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalSaveBytes, currentSaveBytes);

        // Personal vault MUST STILL contain the entry so it is NOT lost
        var entryInVault = await harness.VaultStore.GetEntryAsync(_userId, "vault_blastoise", CancellationToken.None);
        Assert.NotNull(entryInVault);
        Assert.Equal("Shellshock", entryInVault.Nickname);
    }

    [Fact]
    public async Task Deposit_Blocked_WhenGameSessionIsActivelyRunning()
    {
        using var harness = new TestHarness(_userId);

        // Start active game session
        harness.SessionTracker.StartSession(_userId, "emerald", "steamdeck-client");

        var request = new PokemonDepositRequest
        {
            GameId = "emerald",
            PokemonId = "p25_pikachu",
            TargetVaultBoxIndex = 1,
            TargetVaultSlotIndex = 1
        };

        var actionResult = await harness.Controller.Deposit(request, CancellationToken.None);
        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        var response = Assert.IsType<PokemonOperationResponse>(badRequest.Value);

        Assert.False(response.Success);
        Assert.Contains("Finish and exit", response.Message);

        // Verify save untouched
        var currentSaveBytes = await harness.SavesService.GetAsync(_userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(harness.OriginalSaveBytes, currentSaveBytes);
    }

    private sealed class TestHarness : IDisposable
    {
        private readonly TempPaths _tempPaths = new();
        public readonly byte[] OriginalSaveBytes = [0x50, 0x4B, 0x01, 0x02, 0x03, 0x04];
        public readonly byte[] MutatedSaveBytes = [0x50, 0x4B, 0xFF, 0xFF, 0x03, 0x04];

        public GameSavesService SavesService { get; }
        public InMemoryGameSessionTracker SessionTracker { get; }
        public FileSaveOperationCoordinator Coordinator { get; }
        public FilePokemonBackupService BackupService { get; }
        public PokemonTransactionManager TransactionManager { get; }
        public FilePokemonVaultStore VaultStore { get; }
        public PokemonController Controller { get; }

        public TestHarness(Guid userId, bool simulateExtractionFailure = false, bool simulateInjectionFailure = false)
        {
            var clock = new ManualClock();
            SavesService = new GameSavesService(_tempPaths);
            SessionTracker = new InMemoryGameSessionTracker(clock);
            Coordinator = new FileSaveOperationCoordinator(_tempPaths, clock, SavesService, SessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
            BackupService = new FilePokemonBackupService(_tempPaths, clock, SavesService, NullLogger<FilePokemonBackupService>.Instance);
            TransactionManager = new PokemonTransactionManager(_tempPaths, clock, SavesService, Coordinator, BackupService, NullLogger<PokemonTransactionManager>.Instance);
            VaultStore = new FilePokemonVaultStore(_tempPaths, clock);

            // Populate initial save
            SavesService.SaveAsync(userId, "emerald", "sram", OriginalSaveBytes, CancellationToken.None).GetAwaiter().GetResult();

            var game = new GameDetail
            {
                Id = "emerald",
                Title = "Pokemon - Emerald Version (USA, Europe)",
                FileName = "Pokemon - Emerald Version (USA, Europe).gba",
                System = "gba",
                Core = "mgba"
            };

            var gamesService = new TestGamesService(game);
            var detector = new PokemonGameDetector(_tempPaths);
            var provider = new TestPokemonProvider(MutatedSaveBytes, simulateExtractionFailure, simulateInjectionFailure);
            var providerFactory = new TestPokemonProviderFactory(provider);

            var authContext = new TestAuthorizationContext(userId);
            var config = new PokemonConfiguration
            {
                Enabled = true,
                ProviderType = "pkvault",
                PkVaultBaseUrl = "http://localhost:5000"
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

    private sealed class TestPokemonProvider(
        byte[] mutatedBytes,
        bool failExtract,
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
            if (failExtract)
            {
                return Task.FromResult(new PokemonExtractResult { IsSuccess = false, ErrorMessage = "Simulated extract failure." });
            }

            return Task.FromResult(new PokemonExtractResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = mutatedBytes,
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
                return Task.FromResult(new PokemonInjectResult { IsSuccess = false, ErrorMessage = "Simulated inject failure." });
            }

            return Task.FromResult(new PokemonInjectResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = mutatedBytes,
                AssignedLocation = $"Box {targetBoxIndex ?? 1} Slot {targetSlotIndex ?? 1}"
            });
        }
    }

    private sealed class TestPokemonProviderFactory(IPokemonProvider provider) : IPokemonProviderFactory
    {
        public IPokemonProvider Create(PokemonConfiguration config) => provider;
    }

    private sealed class TestGamesService(GameDetail game) : IGamesService
    {
        public IReadOnlyList<GameLibrary> GetGameLibraries() =>
            [new GameLibrary { Id = "lib1", Name = "Games", Locations = ["/tmp"] }];

        public IReadOnlyList<GameSystem> GetSystems(string libraryId) => [];
        public IReadOnlyList<GameSummary> GetGames(string libraryId, string? system = null) => [game];
        public GameDetail? GetGame(string libraryId, string gameId) => game;
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
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-depwith-tests", Guid.NewGuid().ToString("N"));
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

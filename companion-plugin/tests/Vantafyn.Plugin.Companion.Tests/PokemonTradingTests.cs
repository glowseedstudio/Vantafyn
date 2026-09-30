using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;
using Vantafyn.Plugin.Companion.Pokemon;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonTradingTests : IDisposable
{
    private readonly TempPaths _paths;
    private readonly FilePokemonVaultStore _vaultStore;
    private readonly FilePokemonBackupService _backupService;
    private readonly PokemonTransactionManager _transactionManager;
    private readonly FilePokemonTradingService _tradingService;

    public PokemonTradingTests()
    {
        _paths = new TempPaths();
        var clock = new ManualClock();
        _vaultStore = new FilePokemonVaultStore(_paths, clock);
        var savesService = new GameSavesService(_paths);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(_paths, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        _backupService = new FilePokemonBackupService(_paths, clock, savesService, NullLogger<FilePokemonBackupService>.Instance);

        _transactionManager = new PokemonTransactionManager(
            _paths,
            clock,
            savesService,
            coordinator,
            _backupService,
            NullLogger<PokemonTransactionManager>.Instance);

        _tradingService = new FilePokemonTradingService(
            _paths,
            _vaultStore,
            _transactionManager,
            NullLogger<FilePokemonTradingService>.Instance);
    }

    public void Dispose()
    {
        _paths.Dispose();
    }

    [Fact]
    public async Task CreateTrade_AndGetPending_WorksCorrectly()
    {
        var userAGuid = Guid.NewGuid();
        var userBGuid = Guid.NewGuid();
        var userA = userAGuid.ToString("N");
        var userB = userBGuid.ToString("N");

        // Seed User A's vault with Pikachu
        await _vaultStore.AddOrUpdateEntryAsync(userAGuid, new PokemonVaultEntry
        {
            Id = "pika-1",
            Species = "Pikachu",
            SpeciesId = 25,
            Nickname = "Sparky",
            Level = 25,
            BoxIndex = 1,
            SlotIndex = 1
        }, CancellationToken.None);

        // Create trade offer from User A targeting User B
        var createResult = await _tradingService.CreateTradeAsync(userA, "Ash", new CreateTradeRequest
        {
            TargetUserId = userB,
            TargetUserName = "Gary",
            Offer = new PokemonTradeOffer
            {
                PokemonId = "pika-1",
                IsVault = true
            }
        });

        Assert.True(createResult.IsSuccess);
        Assert.NotNull(createResult.TradeSession);
        Assert.Equal(PokemonTradeStatus.Pending, createResult.TradeSession.Status);
        Assert.Equal("Pikachu", createResult.TradeSession.InitiatorOffer.Species);

        // User B checks pending incoming trades
        var pending = await _tradingService.GetPendingTradesAsync(userB);
        Assert.Single(pending);
        Assert.Equal("pika-1", pending[0].InitiatorOffer.PokemonId);
    }

    [Fact]
    public async Task JoinLinkTrade_PerformsAtomicExchange_BetweenTwoUserVaults()
    {
        var userAGuid = Guid.NewGuid();
        var userBGuid = Guid.NewGuid();
        var userA = userAGuid.ToString("N");
        var userB = userBGuid.ToString("N");
        var linkPin = "749210";

        // User A has Charizard in Vault Box 1, Slot 1
        await _vaultStore.AddOrUpdateEntryAsync(userAGuid, new PokemonVaultEntry
        {
            Id = "char-1",
            Species = "Charizard",
            SpeciesId = 6,
            Nickname = "Flame",
            Level = 50,
            BoxIndex = 1,
            SlotIndex = 1
        }, CancellationToken.None);

        // User B has Blastoise in Vault Box 1, Slot 1
        await _vaultStore.AddOrUpdateEntryAsync(userBGuid, new PokemonVaultEntry
        {
            Id = "blast-1",
            Species = "Blastoise",
            SpeciesId = 9,
            Nickname = "Hydro",
            Level = 50,
            BoxIndex = 1,
            SlotIndex = 1
        }, CancellationToken.None);

        // User A creates Link Code room
        var createResult = await _tradingService.CreateTradeAsync(userA, "Red", new CreateTradeRequest
        {
            LinkCode = linkPin,
            Offer = new PokemonTradeOffer
            {
                PokemonId = "char-1",
                IsVault = true,
                TargetVaultBoxIndex = 1,
                TargetVaultSlotIndex = 5
            }
        });

        Assert.True(createResult.IsSuccess);
        Assert.Equal(PokemonTradeType.LinkCode, createResult.TradeSession!.Type);

        // User B joins with Link Code and offers Blastoise
        var joinResult = await _tradingService.JoinLinkTradeAsync(userB, "Blue", new JoinLinkTradeRequest
        {
            LinkCode = linkPin,
            Offer = new PokemonTradeOffer
            {
                PokemonId = "blast-1",
                IsVault = true,
                TargetVaultBoxIndex = 1,
                TargetVaultSlotIndex = 8
            }
        });

        Assert.True(joinResult.IsSuccess);
        Assert.Equal(PokemonTradeStatus.Completed, joinResult.TradeSession!.Status);
        Assert.NotNull(joinResult.TransactionId);

        // Verify User A now has Blastoise and NO LONGER has Charizard
        var userABox = await _vaultStore.GetBoxAsync(userAGuid, 1, CancellationToken.None);
        Assert.NotNull(userABox);
        Assert.DoesNotContain(userABox.Entries, e => e.Species == "Charizard");
        var blastInA = Assert.Single(userABox.Entries, e => e.Species == "Blastoise");
        Assert.Equal("Hydro", blastInA.Nickname);

        // Verify User B now has Charizard and NO LONGER has Blastoise
        var userBBox = await _vaultStore.GetBoxAsync(userBGuid, 1, CancellationToken.None);
        Assert.NotNull(userBBox);
        Assert.DoesNotContain(userBBox.Entries, e => e.Species == "Blastoise");
        var charInB = Assert.Single(userBBox.Entries, e => e.Species == "Charizard");
        Assert.Equal("Flame", charInB.Nickname);

        // Trade history
        var historyA = await _tradingService.GetTradeHistoryAsync(userA);
        Assert.Single(historyA);
        Assert.Equal(PokemonTradeStatus.Completed, historyA[0].Status);
    }

    [Fact]
    public async Task CancelTrade_SetsStatusToCancelled()
    {
        var userAGuid = Guid.NewGuid();
        var userA = userAGuid.ToString("N");

        await _vaultStore.AddOrUpdateEntryAsync(userAGuid, new PokemonVaultEntry
        {
            Id = "drag-1",
            Species = "Dragonite",
            SpeciesId = 149,
            Level = 55
        }, CancellationToken.None);

        var createResult = await _tradingService.CreateTradeAsync(userA, "Lance", new CreateTradeRequest
        {
            Offer = new PokemonTradeOffer { PokemonId = "drag-1", IsVault = true }
        });

        Assert.True(createResult.IsSuccess);
        var tradeId = createResult.TradeSession!.Id;

        var cancelResult = await _tradingService.CancelTradeAsync(userA, new CancelTradeRequest
        {
            TradeId = tradeId,
            Reason = "Changed my mind"
        });

        Assert.True(cancelResult.IsSuccess);
        Assert.Equal(PokemonTradeStatus.Cancelled, cancelResult.TradeSession!.Status);

        var loaded = await _tradingService.GetTradeAsync(tradeId);
        Assert.NotNull(loaded);
        Assert.Equal(PokemonTradeStatus.Cancelled, loaded.Status);
    }

    private sealed class ManualClock : IClock
    {
        private DateTimeOffset _current = new(2026, 9, 30, 12, 0, 0, TimeSpan.Zero);
        public DateTimeOffset UtcNow => _current;
        public void Advance(TimeSpan duration) => _current = _current.Add(duration);
    }

    private sealed class TempPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-trading-tests-" + Guid.NewGuid().ToString("N"));
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


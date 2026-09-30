using System.Security.Cryptography;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;
using Vantafyn.Plugin.Companion.Pokemon;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonBackupAndTransactionTests
{
    [Fact]
    public async Task BackupService_CreatesBackup_ValidatesChecksum()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var backupService = new FilePokemonBackupService(temp, clock, savesService, NullLogger<FilePokemonBackupService>.Instance);
        var userId = Guid.NewGuid();

        var originalBytes = new byte[] { 0x50, 0x4F, 0x4B, 0x45, 0x01, 0x02, 0x03 };
        var expectedChecksum = Convert.ToHexString(SHA256.HashData(originalBytes)).ToLowerInvariant();

        var meta = await backupService.CreateBackupAsync(userId, "emerald", originalBytes, "Pre-test backup", null, CancellationToken.None);

        Assert.NotNull(meta);
        Assert.Equal("emerald", meta.GameId);
        Assert.Equal(expectedChecksum, meta.ChecksumSha256);
        Assert.Equal(originalBytes.Length, meta.SizeBytes);
        Assert.False(meta.IsRestored);

        var retrievedBytes = await backupService.GetBackupBytesAsync(userId, meta.BackupId, CancellationToken.None);
        Assert.NotNull(retrievedBytes);
        Assert.Equal(originalBytes, retrievedBytes);
    }

    [Fact]
    public async Task BackupService_RestoresCorruptedSaveToOriginalState()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var backupService = new FilePokemonBackupService(temp, clock, savesService, NullLogger<FilePokemonBackupService>.Instance);
        var userId = Guid.NewGuid();

        var pristineBytes = new byte[] { 0xAA, 0xBB, 0xCC, 0xDD, 0xEE };
        await savesService.SaveAsync(userId, "firered", "sram", pristineBytes, CancellationToken.None);

        // Create backup
        var backup = await backupService.CreateBackupAsync(userId, "firered", pristineBytes, "Pre-mutation", null, CancellationToken.None);

        // Corrupt active save
        var corruptedBytes = new byte[] { 0x00, 0x00, 0x00 };
        await savesService.SaveAsync(userId, "firered", "sram", corruptedBytes, CancellationToken.None);

        // Verify save is currently corrupted
        var currentBytes = await savesService.GetAsync(userId, "firered", "sram", CancellationToken.None);
        Assert.Equal(corruptedBytes, currentBytes);

        // Restore backup
        var restored = await backupService.RestoreBackupAsync(userId, backup.BackupId, CancellationToken.None);
        Assert.True(restored);

        // Verify save is restored to pristine bytes
        var restoredBytes = await savesService.GetAsync(userId, "firered", "sram", CancellationToken.None);
        Assert.Equal(pristineBytes, restoredBytes);
    }

    [Fact]
    public async Task BackupService_PruningPolicy_NeverDeletesOnlyBackup()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var backupService = new FilePokemonBackupService(temp, clock, savesService, NullLogger<FilePokemonBackupService>.Instance);
        var userId = Guid.NewGuid();

        // Single backup
        await backupService.CreateBackupAsync(userId, "crystal", new byte[] { 1, 2, 3 }, "First", null, CancellationToken.None);

        // Pruning with retainCount 0 still keeps the only backup
        var prunedCount = await backupService.PruneOldBackupsAsync(userId, "crystal", 0, CancellationToken.None);
        Assert.Equal(0, prunedCount);

        var list = await backupService.ListBackupsAsync(userId, "crystal", CancellationToken.None);
        Assert.Single(list);

        // Create 4 more backups
        for (var i = 2; i <= 5; i++)
        {
            clock.Advance(TimeSpan.FromMinutes(1));
            await backupService.CreateBackupAsync(userId, "crystal", new byte[] { (byte)i }, $"Backup {i}", null, CancellationToken.None);
        }

        var fullList = await backupService.ListBackupsAsync(userId, "crystal", CancellationToken.None);
        Assert.Equal(5, fullList.Count);

        // Prune keeping newest 2
        var pruned = await backupService.PruneOldBackupsAsync(userId, "crystal", 2, CancellationToken.None);
        Assert.Equal(3, pruned);

        var remaining = await backupService.ListBackupsAsync(userId, "crystal", CancellationToken.None);
        Assert.Equal(2, remaining.Count);
    }

    [Fact]
    public async Task TransactionManager_SuccessfulOperation_CommitsAndLogsHistory()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        var backupService = new FilePokemonBackupService(temp, clock, savesService, NullLogger<FilePokemonBackupService>.Instance);
        var txManager = new PokemonTransactionManager(temp, clock, savesService, coordinator, backupService, NullLogger<PokemonTransactionManager>.Instance);
        var userId = Guid.NewGuid();

        var initialBytes = new byte[] { 0x10, 0x20, 0x30, 0x40 };
        await savesService.SaveAsync(userId, "emerald", "sram", initialBytes, CancellationToken.None);

        var txResult = await txManager.ExecuteAsync(
            userId: userId,
            operationType: "Vault Deposit",
            sourceGameId: "emerald",
            destinationGameId: null,
            pokemonId: "p123",
            species: "Rayquaza",
            nickname: "SkyLord",
            operation: async context =>
            {
                // Mutate the working copy
                context.SourceSaveWorkingCopy[0] = 0xFF;
                return await Task.FromResult(true);
            },
            cancellationToken: CancellationToken.None);

        Assert.True(txResult.Success);
        Assert.Equal(PokemonTransactionStatus.Committed, txResult.Record.Status);

        // Save on disk has the committed mutation
        var onDiskBytes = await savesService.GetAsync(userId, "emerald", "sram", CancellationToken.None);
        Assert.NotNull(onDiskBytes);
        Assert.Equal(0xFF, onDiskBytes[0]);

        // History record was saved
        var history = await txManager.ListHistoryAsync(userId, CancellationToken.None);
        Assert.Single(history);
        Assert.Equal("Vault Deposit", history[0].OperationType);
        Assert.Equal(PokemonTransactionStatus.Committed, history[0].Status);
    }

    [Fact]
    public async Task TransactionManager_DeliberateFailure_RollsBackSaveAndLogsFailure()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        var backupService = new FilePokemonBackupService(temp, clock, savesService, NullLogger<FilePokemonBackupService>.Instance);
        var txManager = new PokemonTransactionManager(temp, clock, savesService, coordinator, backupService, NullLogger<PokemonTransactionManager>.Instance);
        var userId = Guid.NewGuid();

        var initialBytes = new byte[] { 0xDE, 0xAD, 0xBE, 0xEF };
        await savesService.SaveAsync(userId, "emerald", "sram", initialBytes, CancellationToken.None);

        var checksumBefore = Convert.ToHexString(SHA256.HashData(initialBytes)).ToLowerInvariant();

        // Execute transaction that deliberately throws during operation
        var txResult = await txManager.ExecuteAsync<string>(
            userId: userId,
            operationType: "Faulty Transfer",
            sourceGameId: "emerald",
            destinationGameId: null,
            pokemonId: "p999",
            species: "Mew",
            nickname: "Mew",
            operation: async context =>
            {
                // Mutate working copy
                context.SourceSaveWorkingCopy[0] = 0x00;

                // Deliberately trigger failure (e.g. simulated network crash or PKVault error)
                throw new InvalidOperationException("Simulated mid-operation PKVault network disruption!");
            },
            cancellationToken: CancellationToken.None);

        // Transaction failed
        Assert.False(txResult.Success);
        Assert.Equal(PokemonTransactionStatus.RolledBack, txResult.Record.Status);
        Assert.Contains("Simulated mid-operation", txResult.ErrorMessage);

        // VERIFY ROLLBACK: save bytes on disk are UNCHANGED and match the initial pristine state!
        var bytesOnDisk = await savesService.GetAsync(userId, "emerald", "sram", CancellationToken.None);
        Assert.NotNull(bytesOnDisk);
        Assert.Equal(initialBytes, bytesOnDisk);

        var checksumAfter = Convert.ToHexString(SHA256.HashData(bytesOnDisk)).ToLowerInvariant();
        Assert.Equal(checksumBefore, checksumAfter);

        // History logs the rolled back transaction
        var history = await txManager.ListHistoryAsync(userId, CancellationToken.None);
        Assert.Single(history);
        Assert.Equal(PokemonTransactionStatus.RolledBack, history[0].Status);
        Assert.Contains("Simulated mid-operation", history[0].ErrorMessage);
    }

    [Fact]
    public async Task MultiUserIsolation_UserABackupAndHistoryInaccessibleToUserB()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        var backupService = new FilePokemonBackupService(temp, clock, savesService, NullLogger<FilePokemonBackupService>.Instance);
        var txManager = new PokemonTransactionManager(temp, clock, savesService, coordinator, backupService, NullLogger<PokemonTransactionManager>.Instance);

        var userA = Guid.NewGuid();
        var userB = Guid.NewGuid();

        await savesService.SaveAsync(userA, "emerald", "sram", new byte[] { 1, 2, 3 }, CancellationToken.None);
        var backupA = await backupService.CreateBackupAsync(userA, "emerald", new byte[] { 1, 2, 3 }, "User A backup", null, CancellationToken.None);

        // User B cannot see User A's backup
        var backupsB = await backupService.ListBackupsAsync(userB, null, CancellationToken.None);
        Assert.Empty(backupsB);

        var backupBytesForB = await backupService.GetBackupBytesAsync(userB, backupA.BackupId, CancellationToken.None);
        Assert.Null(backupBytesForB);

        var restoreAttemptByB = await backupService.RestoreBackupAsync(userB, backupA.BackupId, CancellationToken.None);
        Assert.False(restoreAttemptByB);

        // User B history is empty
        var historyB = await txManager.ListHistoryAsync(userB, CancellationToken.None);
        Assert.Empty(historyB);
    }

    [Fact]
    public async Task RestoreBackup_ViaService_RestoresBytesAndSetsFlags()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var backupService = new FilePokemonBackupService(temp, clock, savesService, NullLogger<FilePokemonBackupService>.Instance);
        var userId = Guid.NewGuid();

        var originalBytes = new byte[] { 0x11, 0x22, 0x33, 0x44 };
        await savesService.SaveAsync(userId, "emerald", "sram", originalBytes, CancellationToken.None);
        var backup = await backupService.CreateBackupAsync(userId, "emerald", originalBytes, "Test backup", null, CancellationToken.None);

        // Mutate save
        await savesService.SaveAsync(userId, "emerald", "sram", new byte[] { 0x99, 0x99 }, CancellationToken.None);

        // Restore
        var restored = await backupService.RestoreBackupAsync(userId, backup.BackupId, CancellationToken.None);
        Assert.True(restored);

        var loadedBytes = await savesService.GetAsync(userId, "emerald", "sram", CancellationToken.None);
        Assert.Equal(originalBytes, loadedBytes);

        var meta = await backupService.GetBackupMetadataAsync(userId, backup.BackupId, CancellationToken.None);
        Assert.NotNull(meta);
        Assert.True(meta.IsRestored);
        Assert.NotNull(meta.RestoredAtUtc);
    }

    private sealed class ManualClock : IClock
    {
        private DateTimeOffset _current = new(2026, 9, 30, 12, 0, 0, TimeSpan.Zero);
        public DateTimeOffset UtcNow => _current;
        public void Advance(TimeSpan duration) => _current = _current.Add(duration);
    }

    private sealed class TempPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-tx-tests", Guid.NewGuid().ToString("N"));
        public string DataRoot => _root;
        public string UserSettingsRoot => Directory.CreateDirectory(Path.Combine(_root, "user-settings")).FullName;
        public string PersonalPlaylistsRoot => Directory.CreateDirectory(Path.Combine(_root, "personal-playlists")).FullName;
        public string OmbiSessionsRoot => Directory.CreateDirectory(Path.Combine(_root, "ombi-sessions")).FullName;
        public string SecretsRoot => Directory.CreateDirectory(Path.Combine(_root, "secrets")).FullName;
        public string PushRegistrationsRoot => Directory.CreateDirectory(Path.Combine(_root, "push-registrations")).FullName;
        public string GameSavesRoot => Directory.CreateDirectory(Path.Combine(_root, "game-saves")).FullName;
        public string PokemonRoot => Directory.CreateDirectory(Path.Combine(_root, "pokemon")).FullName;

        public void Dispose()
        {
            if (Directory.Exists(_root))
            {
                Directory.Delete(_root, recursive: true);
            }
        }
    }
}

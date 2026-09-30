using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;
using Vantafyn.Plugin.Companion.Pokemon;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class SaveOperationCoordinatorTests
{
    [Fact]
    public async Task GetSaveState_WhenSaveMissing_ReturnsMissingAndBlocksWrite()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        var userId = Guid.NewGuid();

        var state = await coordinator.GetSaveStateAsync(userId, "emerald", CancellationToken.None);

        Assert.Equal(SaveOperationState.Missing, state.State);
        Assert.False(state.CanRead);
        Assert.False(state.CanWrite);

        var canWrite = await coordinator.CanWriteAsync(userId, "emerald", CancellationToken.None);
        Assert.False(canWrite.IsAllowed);
    }

    [Fact]
    public async Task GetSaveState_WhenSavePresentAndIdle_ReturnsAvailable()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        var userId = Guid.NewGuid();

        // Create save
        await savesService.SaveAsync(userId, "emerald", "sram", new byte[1024], CancellationToken.None);

        var state = await coordinator.GetSaveStateAsync(userId, "emerald", CancellationToken.None);

        Assert.Equal(SaveOperationState.Available, state.State);
        Assert.True(state.CanRead);
        Assert.True(state.CanWrite);

        var canWrite = await coordinator.CanWriteAsync(userId, "emerald", CancellationToken.None);
        Assert.True(canWrite.IsAllowed);
    }

    [Fact]
    public async Task ActiveGameSession_BlocksMutationUntilSessionEnds()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        var userId = Guid.NewGuid();

        await savesService.SaveAsync(userId, "emerald", "sram", new byte[1024], CancellationToken.None);

        // Start active game session
        sessionTracker.StartSession(userId, "emerald", "device-android-1");

        var state = await coordinator.GetSaveStateAsync(userId, "emerald", CancellationToken.None);
        Assert.Equal(SaveOperationState.GameActive, state.State);
        Assert.True(state.CanRead); // Safe read inspection allowed
        Assert.False(state.CanWrite); // Write blocked!
        Assert.Contains("Finish and exit", state.Reason);

        // Attempting to acquire mutation lock while game is active fails
        var lockHandle = await coordinator.AcquireLockAsync(userId, "emerald", "Vault Deposit", null, CancellationToken.None);
        Assert.Null(lockHandle);

        // End session
        sessionTracker.EndSession(userId, "emerald");

        var stateAfterExit = await coordinator.GetSaveStateAsync(userId, "emerald", CancellationToken.None);
        Assert.Equal(SaveOperationState.Available, stateAfterExit.State);
        Assert.True(stateAfterExit.CanWrite);

        // Now lock acquisition succeeds
        var acquiredHandle = await coordinator.AcquireLockAsync(userId, "emerald", "Vault Deposit", null, CancellationToken.None);
        Assert.NotNull(acquiredHandle);
        await acquiredHandle.DisposeAsync();
    }

    [Fact]
    public async Task ConcurrentOperations_BlockSimultaneousExclusiveLocks()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        var userId = Guid.NewGuid();

        await savesService.SaveAsync(userId, "emerald", "sram", new byte[1024], CancellationToken.None);

        // First lock succeeds
        var handle1 = await coordinator.AcquireLockAsync(userId, "emerald", "Operation 1", null, CancellationToken.None);
        Assert.NotNull(handle1);

        // State is now OperationLocked
        var state = await coordinator.GetSaveStateAsync(userId, "emerald", CancellationToken.None);
        Assert.Equal(SaveOperationState.OperationLocked, state.State);
        Assert.False(state.CanWrite);
        Assert.Contains("Operation 1", state.Reason);

        // Second lock is denied
        var handle2 = await coordinator.AcquireLockAsync(userId, "emerald", "Operation 2", null, CancellationToken.None);
        Assert.Null(handle2);

        // Release first lock
        await handle1.DisposeAsync();

        // State returns to Available
        var stateAfterRelease = await coordinator.GetSaveStateAsync(userId, "emerald", CancellationToken.None);
        Assert.Equal(SaveOperationState.Available, stateAfterRelease.State);
        Assert.True(stateAfterRelease.CanWrite);

        // Now second lock can be acquired
        var handle3 = await coordinator.AcquireLockAsync(userId, "emerald", "Operation 2", null, CancellationToken.None);
        Assert.NotNull(handle3);
        await handle3.DisposeAsync();
    }

    [Fact]
    public async Task AbandonedLock_RecoversAutomaticallyAfterExpiration()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);
        var userId = Guid.NewGuid();

        await savesService.SaveAsync(userId, "emerald", "sram", new byte[1024], CancellationToken.None);

        // Acquire lock with short lease (10 seconds)
        var handle = await coordinator.AcquireLockAsync(userId, "emerald", "Interrupted Operation", TimeSpan.FromSeconds(10), CancellationToken.None);
        Assert.NotNull(handle);

        // Fast forward clock past expiration (15 seconds)
        clock.Advance(TimeSpan.FromSeconds(15));

        // State check discovers expired lock, recovers, and returns Available
        var state = await coordinator.GetSaveStateAsync(userId, "emerald", CancellationToken.None);
        Assert.Equal(SaveOperationState.Available, state.State);
        Assert.True(state.CanWrite);

        // New operation can acquire lock
        var newHandle = await coordinator.AcquireLockAsync(userId, "emerald", "New Operation", null, CancellationToken.None);
        Assert.NotNull(newHandle);
        await newHandle.DisposeAsync();
    }

    [Fact]
    public async Task MultiUserIsolation_UserAActiveGameOrLockDoesNotAffectUserB()
    {
        using var temp = new TempPaths();
        var clock = new ManualClock();
        var savesService = new GameSavesService(temp);
        var sessionTracker = new InMemoryGameSessionTracker(clock);
        var coordinator = new FileSaveOperationCoordinator(temp, clock, savesService, sessionTracker, NullLogger<FileSaveOperationCoordinator>.Instance);

        var userA = Guid.NewGuid();
        var userB = Guid.NewGuid();

        // Both users have emerald saves
        await savesService.SaveAsync(userA, "emerald", "sram", new byte[512], CancellationToken.None);
        await savesService.SaveAsync(userB, "emerald", "sram", new byte[512], CancellationToken.None);

        // User A starts playing emerald and acquires a lock
        sessionTracker.StartSession(userA, "emerald", "device-a");
        var lockA = await coordinator.AcquireLockAsync(userA, "emerald", "User A Operation", null, CancellationToken.None);

        // User A is locked/active
        var stateA = await coordinator.GetSaveStateAsync(userA, "emerald", CancellationToken.None);
        Assert.False(stateA.CanWrite);

        // User B is completely unaffected!
        var stateB = await coordinator.GetSaveStateAsync(userB, "emerald", CancellationToken.None);
        Assert.Equal(SaveOperationState.Available, stateB.State);
        Assert.True(stateB.CanWrite);

        var lockB = await coordinator.AcquireLockAsync(userB, "emerald", "User B Operation", null, CancellationToken.None);
        Assert.NotNull(lockB);

        await lockB.DisposeAsync();
        if (lockA != null) await lockA.DisposeAsync();
    }

    private sealed class ManualClock : IClock
    {
        private DateTimeOffset _current = new(2026, 9, 30, 12, 0, 0, TimeSpan.Zero);
        public DateTimeOffset UtcNow => _current;
        public void Advance(TimeSpan duration) => _current = _current.Add(duration);
    }

    private sealed class TempPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-save-coordinator-tests", Guid.NewGuid().ToString("N"));
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

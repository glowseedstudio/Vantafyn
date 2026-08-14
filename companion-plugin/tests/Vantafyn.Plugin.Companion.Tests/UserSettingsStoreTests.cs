using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.UserSettings;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class UserSettingsStoreTests
{
    [Fact]
    public async Task SavesAndDetectsRevisionConflict()
    {
        using var temp = new TempPaths();
        var store = new FileUserSettingsStore(temp, new FixedClock());
        var user = Guid.NewGuid();

        var saved = await store.SaveAsync(user, new UserSettingsPutRequest(1, 0, "test", "{}", "{}", "{}"), CancellationToken.None);

        Assert.Equal(1, saved.Revision);
        await Assert.ThrowsAsync<RevisionConflictException>(() =>
            store.SaveAsync(user, new UserSettingsPutRequest(1, 0, "test", "{}", "{}", "{}"), CancellationToken.None));
    }

    [Fact]
    public async Task UsersAreIsolated()
    {
        using var temp = new TempPaths();
        var store = new FileUserSettingsStore(temp, new FixedClock());
        var userA = Guid.NewGuid();
        var userB = Guid.NewGuid();

        await store.SaveAsync(userA, new UserSettingsPutRequest(1, 0, "test", "{\"theme\":\"a\"}", "{}", "{}"), CancellationToken.None);

        Assert.NotNull(await store.GetAsync(userA, CancellationToken.None));
        Assert.Null(await store.GetAsync(userB, CancellationToken.None));
    }

    private sealed class FixedClock : IClock
    {
        public DateTimeOffset UtcNow => new(2026, 8, 14, 0, 0, 0, TimeSpan.Zero);
    }

    private sealed class TempPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-companion-tests", Guid.NewGuid().ToString("N"));
        public string DataRoot => _root;
        public string UserSettingsRoot => Directory.CreateDirectory(Path.Combine(_root, "user-settings")).FullName;
        public string PersonalPlaylistsRoot => Directory.CreateDirectory(Path.Combine(_root, "personal-playlists")).FullName;
        public string OmbiSessionsRoot => Directory.CreateDirectory(Path.Combine(_root, "ombi-sessions")).FullName;
        public string SecretsRoot => Directory.CreateDirectory(Path.Combine(_root, "secrets")).FullName;
        public void Dispose()
        {
            if (Directory.Exists(_root)) Directory.Delete(_root, recursive: true);
        }
    }
}

using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Notifications;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PushRegistrationStoreTests
{
    [Fact]
    public async Task UpsertsAndRetrievesDevicesForUser()
    {
        using var temp = new TempPaths();
        var store = new FilePushRegistrationStore(temp, new FixedClock());
        var user = Guid.NewGuid();

        var reg1 = new DevicePushRegistration(
            DeviceId: "device-abc-123",
            Endpoint: "https://push.jelly-watch.org/up12345",
            Distributor: "org.unifiedpush.distributor.ntfy",
            ClientName: "Vantafyn Mobile",
            RegisteredAtUtc: DateTime.UtcNow,
            UpdatedAtUtc: DateTime.UtcNow
        );

        var saved = await store.UpsertDeviceAsync(user, reg1, CancellationToken.None);
        Assert.Equal("device-abc-123", saved.DeviceId);

        var devices = await store.GetDevicesForUserAsync(user, CancellationToken.None);
        Assert.Single(devices);
        Assert.Equal("device-abc-123", devices[0].DeviceId);
        Assert.Equal("https://push.jelly-watch.org/up12345", devices[0].Endpoint);

        // Update endpoint (rotation)
        var regUpdated = reg1 with { Endpoint = "https://push.jelly-watch.org/up99999" };
        await store.UpsertDeviceAsync(user, regUpdated, CancellationToken.None);

        var devicesAfterUpdate = await store.GetDevicesForUserAsync(user, CancellationToken.None);
        Assert.Single(devicesAfterUpdate);
        Assert.Equal("https://push.jelly-watch.org/up99999", devicesAfterUpdate[0].Endpoint);
    }

    [Fact]
    public async Task SupportsMultipleDevicesPerUser()
    {
        using var temp = new TempPaths();
        var store = new FilePushRegistrationStore(temp, new FixedClock());
        var user = Guid.NewGuid();

        var phone = new DevicePushRegistration(
            DeviceId: "phone-1",
            Endpoint: "https://push.jelly-watch.org/up-phone",
            Distributor: "org.unifiedpush.distributor.ntfy",
            ClientName: "Vantafyn Mobile",
            RegisteredAtUtc: DateTime.UtcNow,
            UpdatedAtUtc: DateTime.UtcNow
        );
        var tablet = new DevicePushRegistration(
            DeviceId: "tablet-2",
            Endpoint: "https://push.jelly-watch.org/up-tablet",
            Distributor: "org.unifiedpush.distributor.ntfy",
            ClientName: "Vantafyn Mobile",
            RegisteredAtUtc: DateTime.UtcNow,
            UpdatedAtUtc: DateTime.UtcNow
        );

        await store.UpsertDeviceAsync(user, phone, CancellationToken.None);
        await store.UpsertDeviceAsync(user, tablet, CancellationToken.None);

        var devices = await store.GetDevicesForUserAsync(user, CancellationToken.None);
        Assert.Equal(2, devices.Count);

        // Remove single device
        var removed = await store.RemoveDeviceAsync(user, "phone-1", CancellationToken.None);
        Assert.True(removed);

        var remaining = await store.GetDevicesForUserAsync(user, CancellationToken.None);
        Assert.Single(remaining);
        Assert.Equal("tablet-2", remaining[0].DeviceId);
    }

    [Fact]
    public async Task UsersAreIsolated()
    {
        using var temp = new TempPaths();
        var store = new FilePushRegistrationStore(temp, new FixedClock());
        var userA = Guid.NewGuid();
        var userB = Guid.NewGuid();

        var regA = new DevicePushRegistration(
            DeviceId: "device-a",
            Endpoint: "https://push.jelly-watch.org/up-a",
            Distributor: "org.unifiedpush.distributor.ntfy",
            ClientName: "Vantafyn Mobile",
            RegisteredAtUtc: DateTime.UtcNow,
            UpdatedAtUtc: DateTime.UtcNow
        );

        await store.UpsertDeviceAsync(userA, regA, CancellationToken.None);

        Assert.NotEmpty(await store.GetDevicesForUserAsync(userA, CancellationToken.None));
        Assert.Empty(await store.GetDevicesForUserAsync(userB, CancellationToken.None));
    }

    private sealed class FixedClock : IClock
    {
        public DateTimeOffset UtcNow => new(2026, 9, 18, 12, 0, 0, TimeSpan.Zero);
    }

    private sealed class TempPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-companion-push-tests", Guid.NewGuid().ToString("N"));
        public string DataRoot => _root;
        public string UserSettingsRoot => Directory.CreateDirectory(Path.Combine(_root, "user-settings")).FullName;
        public string PersonalPlaylistsRoot => Directory.CreateDirectory(Path.Combine(_root, "personal-playlists")).FullName;
        public string OmbiSessionsRoot => Directory.CreateDirectory(Path.Combine(_root, "ombi-sessions")).FullName;
        public string SecretsRoot => Directory.CreateDirectory(Path.Combine(_root, "secrets")).FullName;
        public string PushRegistrationsRoot => Directory.CreateDirectory(Path.Combine(_root, "push-registrations")).FullName;
        public void Dispose()
        {
            if (Directory.Exists(_root)) Directory.Delete(_root, recursive: true);
        }
    }
}

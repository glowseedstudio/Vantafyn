using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Notifications;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PushNotificationServiceTests
{
    private sealed class TestHttpMessageHandler(Func<HttpRequestMessage, HttpResponseMessage> handler) : HttpMessageHandler
    {
        public List<HttpRequestMessage> SentRequests { get; } = [];

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            SentRequests.Add(request);
            return Task.FromResult(handler(request));
        }
    }

    private sealed class TestHttpClientFactory(HttpMessageHandler handler) : IHttpClientFactory
    {
        public HttpClient CreateClient(string name) => new(handler);
    }

    private sealed class FixedClock : IClock
    {
        public DateTimeOffset UtcNow => new(2026, 9, 18, 12, 0, 0, TimeSpan.Zero);
    }

    private sealed class TempPaths : ICompanionPaths, IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-companion-push-svc-tests", Guid.NewGuid().ToString("N"));
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

    [Fact]
    public async Task SendToUser_DeliversPayloadWithCorrectHeaders()
    {
        using var temp = new TempPaths();
        var store = new FilePushRegistrationStore(temp, new FixedClock());
        var user = Guid.NewGuid();

        await store.UpsertDeviceAsync(user, new DevicePushRegistration(
            DeviceId: "phone-1",
            Endpoint: "https://push.example.com/up123",
            Distributor: "ntfy",
            ClientName: "TestPhone",
            RegisteredAtUtc: DateTimeOffset.UtcNow,
            UpdatedAtUtc: DateTimeOffset.UtcNow
        ), CancellationToken.None);

        HttpRequestMessage? capturedRequest = null;
        var handler = new TestHttpMessageHandler(req =>
        {
            capturedRequest = req;
            return new HttpResponseMessage(HttpStatusCode.OK);
        });

        var service = new PushNotificationService(
            store,
            new TestHttpClientFactory(handler),
            NullLogger<PushNotificationService>.Instance);

        var payload = new { type = "chat_message", text = "hello" };
        var result = await service.SendToUserAsync(user, payload, CancellationToken.None);

        Assert.Equal(1, result.DevicesContacted);
        Assert.Equal(1, result.DevicesSucceeded);
        Assert.Equal(0, result.StaleEndpointsRemoved);
        Assert.NotNull(capturedRequest);
        Assert.Equal("high", capturedRequest.Headers.GetValues("Urgency").First());
    }

    [Fact]
    public async Task SendToUser_RemovesStaleEndpointOn404Or410()
    {
        using var temp = new TempPaths();
        var store = new FilePushRegistrationStore(temp, new FixedClock());
        var user = Guid.NewGuid();

        await store.UpsertDeviceAsync(user, new DevicePushRegistration(
            DeviceId: "stale-phone",
            Endpoint: "https://push.example.com/stale-topic",
            Distributor: "ntfy",
            ClientName: "OldPhone",
            RegisteredAtUtc: DateTimeOffset.UtcNow,
            UpdatedAtUtc: DateTimeOffset.UtcNow
        ), CancellationToken.None);

        var handler = new TestHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.Gone));

        var service = new PushNotificationService(
            store,
            new TestHttpClientFactory(handler),
            NullLogger<PushNotificationService>.Instance);

        var result = await service.SendToUserAsync(user, new { type = "test" }, CancellationToken.None);

        Assert.Equal(1, result.DevicesContacted);
        Assert.Equal(0, result.DevicesSucceeded);
        Assert.Equal(1, result.StaleEndpointsRemoved);

        var remaining = await store.GetDevicesForUserAsync(user, CancellationToken.None);
        Assert.Empty(remaining);
    }
}

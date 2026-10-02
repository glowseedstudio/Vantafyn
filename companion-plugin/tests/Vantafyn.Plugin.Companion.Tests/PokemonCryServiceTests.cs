using System.IO;
using System.Net;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Pokemon;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonCryServiceTests
{
    private sealed class TestHttpClientFactory(HttpMessageHandler handler) : IHttpClientFactory
    {
        public HttpClient CreateClient(string name) => new(handler, disposeHandler: false);
    }

    private sealed class MockHttpMessageHandler(Func<HttpRequestMessage, HttpResponseMessage> responder) : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            return Task.FromResult(responder(request));
        }
    }

    [Fact]
    public async Task GetCryStreamAsync_ServesFromDiskCache_WhenFileExists()
    {
        using var temp = new TempPaths();
        var cacheDir = Path.Combine(temp.PokemonRoot, "cries", "latest");
        Directory.CreateDirectory(cacheDir);
        var testBytes = new byte[] { 0x4F, 0x67, 0x67, 0x53, 0x00, 0x02 }; // OggS magic header
        await File.WriteAllBytesAsync(Path.Combine(cacheDir, "25.ogg"), testBytes);

        var httpCalled = false;
        var handler = new MockHttpMessageHandler(_ =>
        {
            httpCalled = true;
            return new HttpResponseMessage(HttpStatusCode.NotFound);
        });
        var factory = new TestHttpClientFactory(handler);
        var service = new FilePokemonCryService(temp, factory, NullLogger<FilePokemonCryService>.Instance);

        var (stream, contentType, found) = await service.GetCryStreamAsync(25, "latest", CancellationToken.None);

        Assert.True(found);
        Assert.Equal("audio/ogg", contentType);
        Assert.NotNull(stream);
        Assert.False(httpCalled);

        using var ms = new MemoryStream();
        await stream!.CopyToAsync(ms);
        Assert.Equal(testBytes, ms.ToArray());
        await stream.DisposeAsync();
    }

    [Fact]
    public async Task GetCryStreamAsync_DownloadsAndCaches_WhenFileMissing()
    {
        using var temp = new TempPaths();
        var expectedBytes = new byte[] { 0x4F, 0x67, 0x67, 0x53, 0x01, 0x02, 0x03 };
        string? requestedUrl = null;

        var handler = new MockHttpMessageHandler(req =>
        {
            requestedUrl = req.RequestUri?.ToString();
            return new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new ByteArrayContent(expectedBytes)
            };
        });
        var factory = new TestHttpClientFactory(handler);
        var service = new FilePokemonCryService(temp, factory, NullLogger<FilePokemonCryService>.Instance);

        var (stream, contentType, found) = await service.GetCryStreamAsync(6, "latest", CancellationToken.None);

        Assert.True(found);
        Assert.Equal("audio/ogg", contentType);
        Assert.NotNull(stream);
        Assert.Contains("latest/6.ogg", requestedUrl);

        using var ms = new MemoryStream();
        await stream!.CopyToAsync(ms);
        Assert.Equal(expectedBytes, ms.ToArray());
        await stream.DisposeAsync();

        // Verify disk cache now contains the file
        var cachedFile = Path.Combine(temp.PokemonRoot, "cries", "latest", "6.ogg");
        Assert.True(File.Exists(cachedFile));
        Assert.Equal(expectedBytes, await File.ReadAllBytesAsync(cachedFile));
    }

    [Fact]
    public async Task GetCryStreamAsync_LegacyFallsBackToLatest_WhenLegacy404s()
    {
        using var temp = new TempPaths();
        var latestBytes = new byte[] { 0x4F, 0x67, 0x67, 0x53, 0x09, 0x09 };
        var urls = new List<string>();

        var handler = new MockHttpMessageHandler(req =>
        {
            var url = req.RequestUri?.ToString() ?? "";
            urls.Add(url);
            if (url.Contains("/legacy/"))
            {
                return new HttpResponseMessage(HttpStatusCode.NotFound);
            }
            return new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new ByteArrayContent(latestBytes)
            };
        });
        var factory = new TestHttpClientFactory(handler);
        var service = new FilePokemonCryService(temp, factory, NullLogger<FilePokemonCryService>.Instance);

        var (stream, contentType, found) = await service.GetCryStreamAsync(700, "legacy", CancellationToken.None);

        Assert.True(found);
        Assert.Equal("audio/ogg", contentType);
        Assert.NotNull(stream);
        Assert.Contains(urls, u => u.Contains("/legacy/700.ogg"));
        Assert.Contains(urls, u => u.Contains("/latest/700.ogg"));

        using var ms = new MemoryStream();
        await stream!.CopyToAsync(ms);
        Assert.Equal(latestBytes, ms.ToArray());
        await stream.DisposeAsync();
    }

    [Fact]
    public async Task GetCryStreamAsync_ReturnsNotFound_WhenUpstreamFails()
    {
        using var temp = new TempPaths();
        var handler = new MockHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.NotFound));
        var factory = new TestHttpClientFactory(handler);
        var service = new FilePokemonCryService(temp, factory, NullLogger<FilePokemonCryService>.Instance);

        var (stream, contentType, found) = await service.GetCryStreamAsync(9999, "latest", CancellationToken.None);

        Assert.False(found);
        Assert.Null(stream);
    }

    private sealed class TempPaths : Core.ICompanionPaths, System.IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), "vantafyn-cry-tests", System.Guid.NewGuid().ToString("N"));
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
                try { Directory.Delete(_root, recursive: true); } catch { }
            }
        }
    }
}

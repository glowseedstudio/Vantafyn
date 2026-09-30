using System.Net;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Pokemon;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonFoundationTests
{
    [Fact]
    public void Configuration_HasSafeDefaults()
    {
        var config = new PokemonConfiguration();

        Assert.False(config.Enabled);
        Assert.Equal("pkvault", config.ProviderType);
        Assert.Null(config.PkVaultBaseUrl);
        Assert.Equal(5, config.TimeoutSeconds);
        Assert.True(config.AllowTransfers);
        Assert.False(config.AllowCrossGenerationTransfers);
        Assert.False(config.AllowTrading);
        Assert.False(config.AllowEditing);
        Assert.True(config.AutoBackups);
    }

    [Theory]
    [InlineData("http://localhost:5000", true)]
    [InlineData("https://pkvault.lan:8443", true)]
    [InlineData("http://192.168.1.100:5000/subpath", true)]
    [InlineData("", false)]
    [InlineData("   ", false)]
    [InlineData(null, false)]
    [InlineData("ftp://pkvault:21", false)]
    [InlineData("not a url", false)]
    public void ValidateBaseUrl_ValidatesCorrectly(string? url, bool shouldBeValid)
    {
        var error = PkVaultPokemonProvider.ValidateBaseUrl(url);

        if (shouldBeValid)
        {
            Assert.Null(error);
        }
        else
        {
            Assert.NotNull(error);
        }
    }

    [Fact]
    public async Task TestConnection_ReturnsSuccess_WhenEndpointResponds200()
    {
        var handler = new MockHttpMessageHandler((req) =>
        {
            return new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent("{\"version\":\"1.0.4\",\"status\":\"healthy\"}")
            };
        });

        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var result = await provider.TestConnectionAsync(CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Contains("Successfully connected", result.Message);
        Assert.Equal("1.0.4", result.ProviderVersion);
    }

    [Fact]
    public async Task TestConnection_ReturnsFailure_WhenEndpointReturns500()
    {
        var handler = new MockHttpMessageHandler((req) =>
        {
            return new HttpResponseMessage(HttpStatusCode.InternalServerError);
        });

        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var result = await provider.TestConnectionAsync(CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Contains("HTTP 500", result.Message);
    }

    [Fact]
    public async Task TestConnection_HandlesUnreachableHostGracefully()
    {
        var handler = new MockHttpMessageHandler((req) =>
        {
            throw new HttpRequestException("Connection refused");
        });

        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var result = await provider.TestConnectionAsync(CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Contains("Connection failed", result.Message);
    }

    [Fact]
    public async Task Capabilities_ReflectsConfiguration()
    {
        var handler = new MockHttpMessageHandler((req) => new HttpResponseMessage(HttpStatusCode.OK));
        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000",
            AllowTransfers = true,
            AllowCrossGenerationTransfers = true
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var caps = await provider.GetCapabilitiesAsync(CancellationToken.None);

        Assert.True(caps.CanReadSaves);
        Assert.True(caps.CanWriteSaves);
        Assert.True(caps.CanTransferSameGeneration);
        Assert.True(caps.CanTransferCrossGeneration);
        Assert.True(caps.CanValidateLegality);
        Assert.Contains("gba", caps.SupportedPlatforms);
    }

    private sealed class TestHttpClientFactory(HttpMessageHandler handler) : IHttpClientFactory
    {
        public HttpClient CreateClient(string name)
        {
            return new HttpClient(handler, disposeHandler: false);
        }
    }

    private sealed class MockHttpMessageHandler(Func<HttpRequestMessage, HttpResponseMessage> responder) : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            return Task.FromResult(responder(request));
        }
    }
}

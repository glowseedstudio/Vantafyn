using System.Net;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Pokemon;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonSaveReaderTests
{
    [Fact]
    public async Task ParseSaveAsync_ValidPayload_ParsesTrainerPartyAndBoxes()
    {
        var jsonResponse = """
        {
            "trainerName": "Red",
            "trainerId": "01337",
            "money": 99999,
            "pokedexSeen": 150,
            "pokedexCaught": 149,
            "party": [
                {
                    "species": "Pikachu",
                    "speciesId": 25,
                    "nickname": "Sparky",
                    "level": 50,
                    "gender": "M",
                    "isShiny": true,
                    "nature": "Timid",
                    "ability": "Static",
                    "heldItem": "Light Ball",
                    "originalTrainer": "Red",
                    "originalTrainerId": "01337",
                    "originGame": "Pokémon FireRed",
                    "moves": ["Thunderbolt", "Quick Attack", "Iron Tail", "Volt Tackle"],
                    "legalityStatus": "valid"
                }
            ],
            "boxes": [
                {
                    "boxIndex": 1,
                    "name": "Box 1",
                    "capacity": 30,
                    "pokemon": [
                        {
                            "species": "Charizard",
                            "speciesId": 6,
                            "nickname": "Flame",
                            "level": 85,
                            "gender": "M",
                            "isShiny": false,
                            "nature": "Adamant",
                            "ability": "Blaze",
                            "slotIndex": 1,
                            "legalityStatus": "valid"
                        }
                    ]
                }
            ]
        }
        """;

        var handler = new MockHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(jsonResponse)
        });
        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var dummySaveBytes = new byte[1024];
        var result = await provider.ParseSaveAsync(dummySaveBytes, "firered", "gba", 3, CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Equal("Red", result.TrainerName);
        Assert.Equal("01337", result.TrainerId);
        Assert.Equal(99999, result.Money);
        Assert.Equal(150, result.PokedexSeen);
        Assert.Equal(149, result.PokedexCaught);

        // Party assertions
        Assert.Single(result.Party);
        var partyMon = result.Party[0];
        Assert.Equal("Sparky", partyMon.Nickname);
        Assert.Equal("Pikachu", partyMon.Species);
        Assert.Equal(50, partyMon.Level);
        Assert.True(partyMon.IsShiny);
        Assert.True(partyMon.IsInParty);
        Assert.Equal("Party (Slot 1)", partyMon.CurrentLocation);

        // Boxes assertions
        Assert.Single(result.Boxes);
        var box1 = result.Boxes[0];
        Assert.Equal(1, box1.BoxIndex);
        Assert.Equal(1, box1.OccupiedCount);
        Assert.Single(box1.Entries);
        var boxMon = box1.Entries[0];
        Assert.Equal("Charizard", boxMon.Species);
        Assert.Equal(85, boxMon.Level);
        Assert.False(boxMon.IsShiny);
        Assert.False(boxMon.IsInParty);
        Assert.Equal(1, boxMon.BoxIndex);

        // Details dictionary assertions
        Assert.True(result.Details.ContainsKey(partyMon.Id));
        var partyDetails = result.Details[partyMon.Id];
        Assert.Equal("Timid", partyDetails.Nature);
        Assert.Equal("Static", partyDetails.Ability);
        Assert.Equal("Light Ball", partyDetails.HeldItem);
        Assert.Equal(4, partyDetails.Moves.Count);
        Assert.Contains("Volt Tackle", partyDetails.Moves);
    }

    [Fact]
    public async Task ParseSaveAsync_EmptySaveBytes_FailsGracefully()
    {
        var handler = new MockHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.OK));
        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var resultEmpty = await provider.ParseSaveAsync(Array.Empty<byte>(), "emerald", "gba", 3, CancellationToken.None);
        Assert.False(resultEmpty.IsSuccess);
        Assert.Contains("empty", resultEmpty.ErrorMessage, StringComparison.OrdinalIgnoreCase);

        var resultNull = await provider.ParseSaveAsync(null!, "emerald", "gba", 3, CancellationToken.None);
        Assert.False(resultNull.IsSuccess);
        Assert.Contains("empty", resultNull.ErrorMessage, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task ParseSaveAsync_ProviderError500_FailsGracefullyWithoutThrowing()
    {
        var handler = new MockHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.InternalServerError)
        {
            Content = new StringContent("{\"error\":\"Corrupted save file header\"}")
        });
        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var result = await provider.ParseSaveAsync(new byte[512], "emerald", "gba", 3, CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Contains("HTTP 500", result.ErrorMessage);
    }

    [Fact]
    public async Task ParseSaveAsync_ProviderUnreachable_FailsGracefully()
    {
        var handler = new MockHttpMessageHandler(_ => throw new HttpRequestException("Connection refused"));
        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var result = await provider.ParseSaveAsync(new byte[512], "emerald", "gba", 3, CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Contains("unreachable", result.ErrorMessage, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task ParseSaveAsync_MalformedJson_FailsGracefully()
    {
        var handler = new MockHttpMessageHandler(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent("<html><body>502 Bad Gateway</body></html>")
        });
        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var result = await provider.ParseSaveAsync(new byte[512], "emerald", "gba", 3, CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.NotNull(result.ErrorMessage);
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

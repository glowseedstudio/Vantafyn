using System.Net;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Pokemon;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;
using Vantafyn.Plugin.Companion.Pokemon.Native;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class PokemonSaveReaderTests
{
    [Fact]
    public async Task ParseSaveAsync_ValidPayload_ParsesTrainerPartyAndBoxes()
    {
        var saveInfosResponse = """
        {
            "saveInfos": {
                "firered.sav": {
                    "id": 42,
                    "path": "/pkvault/saves/firered.sav",
                    "trainerName": "Red",
                    "tid": 1337,
                    "sid": 0,
                    "generation": 3,
                    "dexSeenCount": 150,
                    "dexCaughtCount": 149,
                    "partyCount": 1,
                    "boxCount": 1,
                    "boxSlotCount": 30
                }
            }
        }
        """;

        var boxesResponse = """
        [
            {
                "id": "box_0",
                "type": 0,
                "name": "Box 1",
                "slotCount": 30,
                "order": 0,
                "idInt": 0
            }
        ]
        """;

        var pkmResponse = """
        [
            {
                "id": "sparky_party_1",
                "saveId": 42,
                "party": 0,
                "boxId": 0,
                "boxSlot": 0,
                "species": 25,
                "nickname": "Sparky",
                "isNicknamed": true,
                "level": 50,
                "gender": 0,
                "isShiny": true,
                "nature": 10,
                "ability": 9,
                "heldItem": 236,
                "moves": [85, 98, 231, 344],
                "originTrainerName": "Red",
                "tid": 1337
            },
            {
                "id": "flame_box_1",
                "saveId": 42,
                "party": -1,
                "boxId": 0,
                "boxSlot": 0,
                "species": 6,
                "nickname": "Charizard",
                "isNicknamed": false,
                "level": 85,
                "gender": 0,
                "isShiny": false,
                "nature": 3,
                "ability": 65,
                "heldItem": 0,
                "moves": [],
                "originTrainerName": "Red",
                "tid": 1337
            }
        ]
        """;

        var cleanupCalled = false;
        var handler = new MockHttpMessageHandler(req =>
        {
            var path = req.RequestUri?.AbsolutePath ?? string.Empty;
            if (path == "/api/save-infos")
            {
                if (req.Method == HttpMethod.Post)
                {
                    return new HttpResponseMessage(HttpStatusCode.OK)
                    {
                        Content = new StringContent(saveInfosResponse)
                    };
                }
                if (req.Method == HttpMethod.Delete)
                {
                    cleanupCalled = true;
                    return new HttpResponseMessage(HttpStatusCode.OK)
                    {
                        Content = new StringContent("{}")
                    };
                }
            }

            if (path == "/api/storage/box")
            {
                return new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new StringContent(boxesResponse)
                };
            }

            if (path == "/api/storage/save/42/pkm")
            {
                return new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new StringContent(pkmResponse)
                };
            }

            return new HttpResponseMessage(HttpStatusCode.NotFound);
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
        Assert.True(cleanupCalled);
        Assert.Equal("Red", result.TrainerName);
        Assert.Equal("1337", result.TrainerId);
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

    [Fact]
    public async Task ParseSaveAsync_WhenPkmFetchFails_CleansUpStagedSaveFile()
    {
        var saveInfosResponse = """
        {
            "saveInfos": {
                "test.sav": {
                    "id": 99,
                    "path": "/pkvault/saves/test.sav",
                    "trainerName": "Blue",
                    "tid": 54321
                }
            }
        }
        """;

        var cleanupCalled = false;
        var handler = new MockHttpMessageHandler(req =>
        {
            var path = req.RequestUri?.AbsolutePath ?? string.Empty;
            if (path == "/api/save-infos")
            {
                if (req.Method == HttpMethod.Post)
                {
                    return new HttpResponseMessage(HttpStatusCode.OK)
                    {
                        Content = new StringContent(saveInfosResponse)
                    };
                }
                if (req.Method == HttpMethod.Delete)
                {
                    cleanupCalled = true;
                    return new HttpResponseMessage(HttpStatusCode.OK)
                    {
                        Content = new StringContent("{}")
                    };
                }
            }

            if (path == "/api/storage/box")
            {
                return new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new StringContent("[]")
                };
            }

            if (path == "/api/storage/save/99/pkm")
            {
                return new HttpResponseMessage(HttpStatusCode.InternalServerError)
                {
                    Content = new StringContent("Internal server error")
                };
            }

            return new HttpResponseMessage(HttpStatusCode.NotFound);
        });

        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var result = await provider.ParseSaveAsync(new byte[512], "firered", "gba", 3, CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Contains("failed fetching Pokémon", result.ErrorMessage);
        Assert.True(cleanupCalled, "Expected DELETE /api/save-infos cleanup to be executed even on partial failure");
    }

    [Fact]
    public async Task ExtractPokemonFromSaveAsync_ValidRequest_PerformsDeleteCommitDownloadAndCleanup()
    {
        var saveInfosResponse = """
        {
            "saveInfos": {
                "test.sav": {
                    "id": 101,
                    "path": "/pkvault/saves/extract_101.sav",
                    "trainerName": "Red",
                    "tid": 1337
                }
            }
        }
        """;

        var pkmResponse = """
        [
            {
                "id": "pkm_pikachu_42",
                "saveId": 101,
                "party": 0,
                "boxId": 0,
                "boxSlot": 0,
                "species": 25,
                "nickname": "Sparky",
                "isNicknamed": true,
                "level": 50,
                "gender": 0,
                "isShiny": true,
                "nature": 10,
                "ability": 9,
                "heldItem": 236,
                "moves": [85, 98],
                "originTrainerName": "Red",
                "tid": 1337
            }
        ]
        """;

        var deleteCalled = false;
        var commitCalled = false;
        var downloadCalled = false;
        var cleanupCalled = false;

        var handler = new MockHttpMessageHandler(req =>
        {
            var path = req.RequestUri?.AbsolutePath ?? string.Empty;
            if (path == "/api/save-infos")
            {
                if (req.Method == HttpMethod.Post)
                {
                    return new HttpResponseMessage(HttpStatusCode.OK)
                    {
                        Content = new StringContent(saveInfosResponse)
                    };
                }
                if (req.Method == HttpMethod.Delete)
                {
                    cleanupCalled = true;
                    return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") };
                }
            }

            if (path == "/api/storage/save/101/pkm")
            {
                if (req.Method == HttpMethod.Get)
                {
                    return new HttpResponseMessage(HttpStatusCode.OK)
                    {
                        Content = new StringContent(pkmResponse)
                    };
                }
                if (req.Method == HttpMethod.Delete)
                {
                    deleteCalled = true;
                    return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") };
                }
            }

            if (path == "/api/storage/action/save" && req.Method == HttpMethod.Post)
            {
                commitCalled = true;
                return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") };
            }

            if (path == "/api/save-infos/101/download" && req.Method == HttpMethod.Get)
            {
                downloadCalled = true;
                return new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new ByteArrayContent([0xDE, 0xAD, 0xBE, 0xEF])
                };
            }

            return new HttpResponseMessage(HttpStatusCode.NotFound);
        });

        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var result = await provider.ExtractPokemonFromSaveAsync(
            saveBytes: new byte[512],
            pokemonGameId: "firered",
            platform: "gba",
            generation: 3,
            pokemonId: "pkm_pikachu_42",
            isInParty: true,
            boxIndex: null,
            slotIndex: 1,
            cancellationToken: CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.True(deleteCalled, "Expected DELETE /api/storage/save/101/pkm");
        Assert.True(commitCalled, "Expected POST /api/storage/action/save");
        Assert.True(downloadCalled, "Expected GET /api/save-infos/101/download");
        Assert.True(cleanupCalled, "Expected DELETE /api/save-infos?path=...");
        Assert.NotNull(result.UpdatedSaveBytes);
        Assert.Equal(new byte[] { 0xDE, 0xAD, 0xBE, 0xEF }, result.UpdatedSaveBytes);
        Assert.NotNull(result.ExtractedPokemon);
        Assert.Equal("Pikachu", result.ExtractedPokemon.Summary.Species);
        Assert.Equal("Sparky", result.ExtractedPokemon.Summary.Nickname);
    }

    [Fact]
    public async Task InjectPokemonIntoSaveAsync_ValidRequest_PerformsMoveCommitDownloadAndCleanup()
    {
        var saveInfosResponse = """
        {
            "saveInfos": {
                "test.sav": {
                    "id": 202,
                    "path": "/pkvault/saves/inject_202.sav",
                    "trainerName": "Red",
                    "tid": 1337
                }
            }
        }
        """;

        var boxesResponse = """
        [
            {
                "id": "box_guid_1",
                "type": 0,
                "name": "Box 1",
                "slotCount": 30,
                "order": 0,
                "idInt": 0
            }
        ]
        """;

        var moveCalled = false;
        var commitCalled = false;
        var downloadCalled = false;
        var cleanupCalled = false;

        var handler = new MockHttpMessageHandler(req =>
        {
            var path = req.RequestUri?.AbsolutePath ?? string.Empty;
            if (path == "/api/save-infos")
            {
                if (req.Method == HttpMethod.Post)
                {
                    return new HttpResponseMessage(HttpStatusCode.OK)
                    {
                        Content = new StringContent(saveInfosResponse)
                    };
                }
                if (req.Method == HttpMethod.Delete)
                {
                    cleanupCalled = true;
                    return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") };
                }
            }

            if (path == "/api/storage/box" && req.Method == HttpMethod.Get)
            {
                return new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new StringContent(boxesResponse)
                };
            }

            if (path == "/api/storage/move/pkm" && req.Method == HttpMethod.Put)
            {
                moveCalled = true;
                return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") };
            }

            if (path == "/api/storage/action/save" && req.Method == HttpMethod.Post)
            {
                commitCalled = true;
                return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") };
            }

            if (path == "/api/save-infos/202/download" && req.Method == HttpMethod.Get)
            {
                downloadCalled = true;
                return new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new ByteArrayContent([0xCA, 0xFE, 0xBA, 0xBE])
                };
            }

            return new HttpResponseMessage(HttpStatusCode.NotFound);
        });

        var factory = new TestHttpClientFactory(handler);
        var config = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = "http://localhost:5000"
        };
        var provider = new PkVaultPokemonProvider(factory, config, NullLogger<PkVaultPokemonProvider>.Instance);

        var vaultEntry = new PokemonVaultEntry
        {
            Id = "vault_pikachu_1",
            Species = "Pikachu",
            SpeciesId = 25,
            Nickname = "Sparky",
            RawData = "pkm_raw_variant_id"
        };

        var result = await provider.InjectPokemonIntoSaveAsync(
            saveBytes: new byte[512],
            pokemonGameId: "firered",
            platform: "gba",
            generation: 3,
            entry: vaultEntry,
            targetBoxIndex: 1,
            targetSlotIndex: 1,
            targetParty: false,
            cancellationToken: CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.True(moveCalled, "Expected PUT /api/storage/move/pkm");
        Assert.True(commitCalled, "Expected POST /api/storage/action/save");
        Assert.True(downloadCalled, "Expected GET /api/save-infos/202/download");
        Assert.True(cleanupCalled, "Expected DELETE /api/save-infos?path=...");
        Assert.NotNull(result.UpdatedSaveBytes);
        Assert.Equal(new byte[] { 0xCA, 0xFE, 0xBA, 0xBE }, result.UpdatedSaveBytes);
        Assert.Contains("Box 1 Slot 1", result.AssignedLocation);
    }

    [Fact]
    public void ParseGen1Save_ValidSave_ParsesAshAndCharmander()
    {
        if (!File.Exists("/tmp/pokemon_red.sram")) return;

        var bytes = File.ReadAllBytes("/tmp/pokemon_red.sram");
        Assert.True(Gen1SaveParser.IsGen1Save(bytes));

        var catalog = new PkVaultStaticCatalog();
        var result = Gen1SaveParser.Parse(bytes, "pokemon_red", catalog);

        Assert.True(result.IsSuccess);
        Assert.Equal("ASH", result.TrainerName);
        Assert.Equal("477", result.TrainerId);
        Assert.Single(result.Party);

        var charmander = result.Party[0];
        Assert.Equal(4, charmander.SpeciesId);
        Assert.Equal("Charmander", charmander.Species);
        Assert.Equal("CHARMANDER", charmander.Nickname);
        Assert.Equal(6, charmander.Level);
        Assert.True(charmander.IsInParty);
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

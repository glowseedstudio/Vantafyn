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

    [Fact]
    public void ParseGen1Save_ReadsKantoGymBadges()
    {
        var bytes = CreateGen1Save();
        bytes[0x2602] = 0b1000_0101;
        WriteGen1Checksum(bytes);

        var result = Gen1SaveParser.Parse(bytes, "pokemon_red", new PkVaultStaticCatalog());

        Assert.True(result.IsSuccess);
        var kanto = Assert.Single(result.GymBadges);
        Assert.Equal("kanto", kanto.Region);
        Assert.True(kanto.Badges.Single(b => b.Id == "boulder").IsEarned);
        Assert.True(kanto.Badges.Single(b => b.Id == "thunder").IsEarned);
        Assert.True(kanto.Badges.Single(b => b.Id == "earth").IsEarned);
        Assert.False(kanto.Badges.Single(b => b.Id == "cascade").IsEarned);
    }

    [Fact]
    public void ParseGen2Save_ReadsJohtoAndKantoGymBadges()
    {
        var bytes = new byte[32768];
        bytes[0x23E5] = 0b0000_0011;
        bytes[0x23E6] = 0b1000_0000;

        var result = Gen2SaveParser.Parse(bytes, "pokemon_crystal", new PkVaultStaticCatalog());

        Assert.True(result.IsSuccess);
        Assert.Equal(["johto", "kanto"], result.GymBadges.Select(r => r.Region));
        var johto = result.GymBadges.Single(r => r.Region == "johto");
        var kanto = result.GymBadges.Single(r => r.Region == "kanto");
        Assert.True(johto.Badges.Single(b => b.Id == "zephyr").IsEarned);
        Assert.True(johto.Badges.Single(b => b.Id == "hive").IsEarned);
        Assert.True(kanto.Badges.Single(b => b.Id == "earth").IsEarned);
        Assert.False(kanto.Badges.Single(b => b.Id == "boulder").IsEarned);
    }

    [Fact]
    public void ParseGen3Save_ReadsGameSpecificBadgeRegion()
    {
        var emerald = CreateGen3Save();
        emerald[(2 * 4096) + 0x3FC] = 0x80;
        emerald[(2 * 4096) + 0x3FD] = 0x40;

        var emeraldResult = Gen3SaveParser.Parse(emerald, "pokemon_emerald", new PkVaultStaticCatalog());

        Assert.True(emeraldResult.IsSuccess);
        var hoenn = Assert.Single(emeraldResult.GymBadges);
        Assert.Equal("hoenn", hoenn.Region);
        Assert.True(hoenn.Badges.Single(b => b.Id == "stone").IsEarned);
        Assert.True(hoenn.Badges.Single(b => b.Id == "rain").IsEarned);
        Assert.False(hoenn.Badges.Single(b => b.Id == "knuckle").IsEarned);

        var fireRed = CreateGen3Save();
        fireRed[(2 * 4096) + 0x64] = 0b0010_0001;

        var fireRedResult = Gen3SaveParser.Parse(fireRed, "pokemon_firered", new PkVaultStaticCatalog());

        Assert.True(fireRedResult.IsSuccess);
        var kanto = Assert.Single(fireRedResult.GymBadges);
        Assert.Equal("kanto", kanto.Region);
        Assert.True(kanto.Badges.Single(b => b.Id == "boulder").IsEarned);
        Assert.True(kanto.Badges.Single(b => b.Id == "marsh").IsEarned);
        Assert.False(kanto.Badges.Single(b => b.Id == "cascade").IsEarned);
    }

    [Fact]
    public async Task NativePokemonProvider_Gen4HeartGold_DoesNotFallBackToGen3()
    {
        var provider = new NativePokemonProvider();
        var dummyGen3Save = CreateGen3Save();

        var result = await provider.ParseSaveAsync(dummyGen3Save, "heartgold", "nds", 4, CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.NotEqual(3, result.DetectedGeneration);
    }

    [Fact]
    public async Task NativePokemonProvider_Gen4HeartGold_UninitializedSave_ReturnsFailure()
    {
        var provider = new NativePokemonProvider();
        // 512 KB blank file like freshly flushed melonDS SRAM before in-game save
        var blankBytes = new byte[0x80000];

        var result = await provider.ParseSaveAsync(blankBytes, "heartgold", "nds", 4, CancellationToken.None);

        Assert.False(result.IsSuccess);
        Assert.Empty(result.GymBadges);
        Assert.Empty(result.Party);
    }

    [Fact]
    public void PokemonGymBadgeCatalog_Gen3DoesNotInventHoennForHeartGold()
    {
        var badges = PokemonGymBadgeCatalog.ForGen3("heartgold", 0xFF);
        Assert.Empty(badges);
    }

    [Fact]
    public void PokemonGymBadgeCatalog_Gen4HeartGoldReturnsJohtoAndKanto()
    {
        // johtoFlags = 0b00000011 (Zephyr + Hive earned), kantoFlags = 0b00000101 (Boulder + Thunder earned)
        var regions = PokemonGymBadgeCatalog.ForGen4("heartgold", 0b00000011, 0b00000101);

        Assert.Equal(2, regions.Count);
        Assert.Equal("johto", regions[0].Region);
        Assert.Equal("kanto", regions[1].Region);

        Assert.True(regions[0].Badges.Single(b => b.Id == "zephyr").IsEarned);
        Assert.True(regions[0].Badges.Single(b => b.Id == "hive").IsEarned);
        Assert.False(regions[0].Badges.Single(b => b.Id == "plain").IsEarned);

        Assert.True(regions[1].Badges.Single(b => b.Id == "boulder").IsEarned);
        Assert.False(regions[1].Badges.Single(b => b.Id == "cascade").IsEarned);
        Assert.True(regions[1].Badges.Single(b => b.Id == "thunder").IsEarned);
    }

    [Fact]
    public void PokemonGymBadgeCatalog_Gen4SinnohReturnsSinnohRegion()
    {
        var regions = PokemonGymBadgeCatalog.ForGen4("platinum", 0b00000001, 0);

        var sinnoh = Assert.Single(regions);
        Assert.Equal("sinnoh", sinnoh.Region);
        Assert.True(sinnoh.Badges.Single(b => b.Id == "coal").IsEarned);
        Assert.False(sinnoh.Badges.Single(b => b.Id == "forest").IsEarned);
    }

    [Theory]
    [InlineData("diamond", PokemonEventCatalog.Gen4MemberCard, 426, 0xC100)]
    [InlineData("pearl", PokemonEventCatalog.Gen4OaksLetter, 427, 0xC100)]
    [InlineData("platinum", PokemonEventCatalog.Gen4AzureFlute, 428, 0xCF2C)]
    [InlineData("platinum", PokemonEventCatalog.PlatinumSecretKey, 467, 0xCF2C)]
    [InlineData("heartgold", PokemonEventCatalog.HgssEnigmaStone, 534, 0xF628)]
    public void Gen4SaveParser_UnlockEvent_AddsKeyItemAndRecomputesCrc(string gameId, string eventId, ushort expectedItemId, int generalSize)
    {
        var save = new byte[0x80000];
        // Set save counter in partition 0
        BitConverter.GetBytes(42u).CopyTo(save, generalSize - 0x14);

        Assert.False(Gen4SaveParser.IsEventUnlocked(save, gameId, eventId));

        var unlocked = Gen4SaveParser.UnlockEvent(save, gameId, eventId);
        Assert.True(Gen4SaveParser.IsEventUnlocked(unlocked, gameId, eventId));

        int pocketOffset = (gameId.Contains("platinum") || gameId.Contains("heartgold")) ? 0x08D8 : 0x08BC;
        ushort writtenItemId = BitConverter.ToUInt16(unlocked, pocketOffset);
        Assert.Equal(expectedItemId, writtenItemId);

        // Verify CRC16 recalculation
        ushort expectedCrc = Gen4SaveParser.CalculateCrc16(unlocked, 0, generalSize - 0x14);
        ushort storedCrc = BitConverter.ToUInt16(unlocked, generalSize - 0x02);
        Assert.Equal(expectedCrc, storedCrc);
    }

    [Fact]
    public void Gen5SaveParser_UnlockEvent_AddsLibertyPassAndRecomputesCrc()
    {
        var save = new byte[0x80000];

        Assert.False(Gen5SaveParser.IsEventUnlocked(save, "black", PokemonEventCatalog.BwLibertyPass));

        var unlocked = Gen5SaveParser.UnlockEvent(save, "black", PokemonEventCatalog.BwLibertyPass);
        Assert.True(Gen5SaveParser.IsEventUnlocked(unlocked, "black", PokemonEventCatalog.BwLibertyPass));

        // Verify CRC16 recalculation for Bag block (0x18400, length 0x09C0, crc at 0x18DC2)
        ushort expectedCrc = Gen5SaveParser.CalculateCrc16(unlocked, 0x18400, 0x09C0);
        ushort storedCrc = BitConverter.ToUInt16(unlocked, 0x18DC2);
        Assert.Equal(expectedCrc, storedCrc);
    }

    [Fact]
    public void PokemonGymBadgeCatalog_Gen6ReturnsKalosOrHoenn()
    {
        var kalos = PokemonGymBadgeCatalog.ForGen6("pokemon_x", 0b00000011);
        Assert.Single(kalos);
        Assert.Equal("kalos", kalos[0].Region);
        Assert.Equal(8, kalos[0].Badges.Count);
        Assert.True(kalos[0].Badges.Single(b => b.Id == "bug").IsEarned);
        Assert.True(kalos[0].Badges.Single(b => b.Id == "cliff").IsEarned);
        Assert.False(kalos[0].Badges.Single(b => b.Id == "rumble").IsEarned);

        var hoenn = PokemonGymBadgeCatalog.ForGen6("omegaruby", 0b00000001);
        Assert.Single(hoenn);
        Assert.Equal("hoenn", hoenn[0].Region);
        Assert.Equal(8, hoenn[0].Badges.Count);
        Assert.True(hoenn[0].Badges.Single(b => b.Id == "stone").IsEarned);
        Assert.False(hoenn[0].Badges.Single(b => b.Id == "knuckle").IsEarned);
    }

    [Fact]
    public void PokemonGymBadgeCatalog_Gen7ReturnsAlolaStamps()
    {
        var regions = PokemonGymBadgeCatalog.ForGen7("sun", 0b00011111, new HashSet<string> { "normalium-z", "firium-z" });
        Assert.Equal(2, regions.Count);
        Assert.Equal("alola", regions[0].Region);
        Assert.Equal(5, regions[0].Badges.Count);
        Assert.True(regions[0].Badges.All(b => b.IsEarned));
        Assert.Equal("zcrystals", regions[1].Region);
        Assert.Equal(29, regions[1].Badges.Count);
        Assert.True(regions[1].Badges.Single(b => b.Id == "normalium-z").IsEarned);
        Assert.True(regions[1].Badges.Single(b => b.Id == "firium-z").IsEarned);
        Assert.False(regions[1].Badges.Single(b => b.Id == "waterium-z").IsEarned);
    }

    [Fact]
    public async Task NativePokemonProvider_Gen6AndGen7_UninitializedSave_ReturnsFailure()
    {
        var provider = new NativePokemonProvider();
        var blankBytes = new byte[0x100000]; // 1MB

        var res6 = await provider.ParseSaveAsync(blankBytes, "x", "3ds", 6, CancellationToken.None);
        Assert.False(res6.IsSuccess);
        Assert.Empty(res6.Party);

        var res7 = await provider.ParseSaveAsync(blankBytes, "sun", "3ds", 7, CancellationToken.None);
        Assert.False(res7.IsSuccess);
        Assert.Empty(res7.Party);
    }

    [Fact]
    public async Task NativePokemonProvider_Gen6XY_WithValidData_ParsesSuccessfully()
    {
        var provider = new NativePokemonProvider();
        var saveBytes = new byte[0x100000];

        // Status at 0x14000: TID = 12345 (0x3039)
        BitConverter.GetBytes((ushort)12345).CopyTo(saveBytes, 0x14000);
        // OT name at 0x14048: "Calem" in UTF-16LE
        System.Text.Encoding.Unicode.GetBytes("Calem").CopyTo(saveBytes, 0x14048);
        // Misc at 0x04200: badges at 0x0420C = 0x05
        saveBytes[0x0420C] = 0x05;

        var result = await provider.ParseSaveAsync(saveBytes, "x", "3ds", 6, CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Equal(6, result.DetectedGeneration);
        Assert.Equal("12345", result.TrainerId);
        Assert.Equal("Calem", result.TrainerName);
        Assert.Single(result.GymBadges);
        Assert.Equal("kalos", result.GymBadges[0].Region);
        Assert.True(result.GymBadges[0].Badges.Single(b => b.Id == "bug").IsEarned);
        Assert.False(result.GymBadges[0].Badges.Single(b => b.Id == "cliff").IsEarned);
        Assert.True(result.GymBadges[0].Badges.Single(b => b.Id == "rumble").IsEarned);
    }

    [Fact]
    public async Task NativePokemonProvider_Gen7SM_WithValidData_ParsesSuccessfully()
    {
        var provider = new NativePokemonProvider();
        var saveBytes = new byte[0x100000];

        // Status at 0x01200: TID = 54321
        BitConverter.GetBytes((ushort)54321).CopyTo(saveBytes, 0x01200);
        // OT name at 0x01238: "Sun" in UTF-16LE
        System.Text.Encoding.Unicode.GetBytes("Sun").CopyTo(saveBytes, 0x01238);
        // Misc at 0x04000: rawStamps at 0x04008 = (0x07 << 4) = 0x70
        BitConverter.GetBytes((ushort)0x70).CopyTo(saveBytes, 0x04008);
        // Z-Crystals at 0x00D68: Slot 0 = 1831 (Normalium Z)
        BitConverter.GetBytes((ushort)1831).CopyTo(saveBytes, 0x00D68);

        var result = await provider.ParseSaveAsync(saveBytes, "sun", "3ds", 7, CancellationToken.None);

        Assert.True(result.IsSuccess);
        Assert.Equal(7, result.DetectedGeneration);
        Assert.Equal("54321", result.TrainerId);
        Assert.Equal("Sun", result.TrainerName);
        Assert.Equal(2, result.GymBadges.Count);
        Assert.Equal("alola", result.GymBadges[0].Region);
        Assert.True(result.GymBadges[0].Badges.Single(b => b.Id == "melemele").IsEarned);
        Assert.True(result.GymBadges[0].Badges.Single(b => b.Id == "akala").IsEarned);
        Assert.True(result.GymBadges[0].Badges.Single(b => b.Id == "ulaula").IsEarned);
        Assert.False(result.GymBadges[0].Badges.Single(b => b.Id == "poni").IsEarned);
        Assert.Equal("zcrystals", result.GymBadges[1].Region);
        Assert.Equal(29, result.GymBadges[1].Badges.Count);
        Assert.True(result.GymBadges[1].Badges.Single(b => b.Id == "normalium-z").IsEarned);
        Assert.False(result.GymBadges[1].Badges.Single(b => b.Id == "firium-z").IsEarned);
    }

    private static byte[] CreateGen1Save()
    {
        var bytes = new byte[32768];
        bytes[0x2598] = 0x50;
        bytes[0x2F2C] = 0;
        return bytes;
    }

    private static void WriteGen1Checksum(byte[] bytes)
    {
        var sum = 0;
        for (var i = 0x2598; i <= 0x3522; i++)
        {
            sum = (sum + bytes[i]) & 0xFF;
        }
        bytes[0x3523] = (byte)((0xFF - sum) & 0xFF);
    }

    private static byte[] CreateGen3Save()
    {
        var bytes = new byte[4096 * 14];
        for (var sectionId = 0; sectionId < 14; sectionId++)
        {
            var offset = sectionId * 4096;
            BitConverter.GetBytes((ushort)sectionId).CopyTo(bytes, offset + 0x0FF4);
            BitConverter.GetBytes(0x08012025u).CopyTo(bytes, offset + 0x0FF8);
            BitConverter.GetBytes(1u).CopyTo(bytes, offset + 0x0FFC);
        }
        return bytes;
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

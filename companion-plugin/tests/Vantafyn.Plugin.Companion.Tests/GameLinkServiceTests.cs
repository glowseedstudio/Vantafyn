using System;
using System.Threading;
using System.Threading.Tasks;
using Vantafyn.Plugin.Companion.Games;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class GameLinkServiceTests
{
    [Fact]
    public async Task CreateRoom_CreatesWaitingRoomWithCode()
    {
        var service = new InMemoryGameLinkService();
        var hostId = Guid.NewGuid();

        var room = await service.CreateRoomAsync(
            hostId,
            "Red",
            new CreateLinkRoomRequest
            {
                GameId = "pkmn_emerald",
                GameTitle = "Pokemon Emerald",
                Core = "mgba",
                RoomCode = "7788",
                Port = 6400,
            },
            CancellationToken.None);

        Assert.Equal("7788", room.RoomCode);
        Assert.Equal("pkmn_emerald", room.GameId);
        Assert.Equal("Pokemon Emerald", room.GameTitle);
        Assert.Equal("mgba", room.Core);
        Assert.Equal("Red", room.HostPlayerName);
        Assert.Equal(GameLinkRoomStatus.Waiting, room.Status);
        Assert.Equal("/Vantafyn/Games/Link/Relay?roomCode=7788", room.RelayEndpoint);
    }

    [Fact]
    public async Task CreateRoom_GeneratesRandomCode_WhenNoneProvided()
    {
        var service = new InMemoryGameLinkService();
        var hostId = Guid.NewGuid();

        var room = await service.CreateRoomAsync(
            hostId,
            "Blue",
            new CreateLinkRoomRequest
            {
                GameId = "pkmn_firered",
                GameTitle = "Pokemon FireRed",
                Core = "mgba",
            },
            CancellationToken.None);

        Assert.False(string.IsNullOrWhiteSpace(room.RoomCode));
        Assert.Equal(4, room.RoomCode.Length);
        Assert.Equal(GameLinkRoomStatus.Waiting, room.Status);
    }

    [Fact]
    public async Task JoinRoom_ConnectsClientSuccessfully()
    {
        var service = new InMemoryGameLinkService();
        var hostId = Guid.NewGuid();
        var clientId = Guid.NewGuid();

        var created = await service.CreateRoomAsync(
            hostId,
            "Ash",
            new CreateLinkRoomRequest
            {
                GameId = "pkmn_crystal",
                GameTitle = "Pokemon Crystal",
                Core = "tgbdual",
                RoomCode = "1234",
            },
            CancellationToken.None);

        var joined = await service.JoinRoomAsync(
            clientId,
            "Gary",
            "1234",
            new JoinLinkRoomRequest { PlayerName = "Gary" },
            CancellationToken.None);

        Assert.Equal(GameLinkRoomStatus.Connected, joined.Status);
        Assert.Equal("Gary", joined.ClientPlayerName);
        Assert.Equal(clientId.ToString("N"), joined.ClientUserId);

        var fetched = await service.GetRoomAsync("1234", CancellationToken.None);
        Assert.NotNull(fetched);
        Assert.Equal(GameLinkRoomStatus.Connected, fetched.Status);
    }

    [Fact]
    public async Task GetActiveRooms_FiltersByGameAndCore()
    {
        var service = new InMemoryGameLinkService();

        await service.CreateRoomAsync(
            Guid.NewGuid(),
            "Player1",
            new CreateLinkRoomRequest
            {
                GameId = "pkmn_platinum",
                GameTitle = "Pokemon Platinum",
                Core = "melonds",
                RoomCode = "0001",
            },
            CancellationToken.None);

        await service.CreateRoomAsync(
            Guid.NewGuid(),
            "Player2",
            new CreateLinkRoomRequest
            {
                GameId = "pkmn_emerald",
                GameTitle = "Pokemon Emerald",
                Core = "mgba",
                RoomCode = "0002",
            },
            CancellationToken.None);

        var ndsRooms = await service.GetActiveRoomsAsync(null, "melonds", CancellationToken.None);
        Assert.Single(ndsRooms);
        Assert.Equal("0001", ndsRooms[0].RoomCode);

        var emeraldRooms = await service.GetActiveRoomsAsync("pkmn_emerald", null, CancellationToken.None);
        Assert.Single(emeraldRooms);
        Assert.Equal("0002", emeraldRooms[0].RoomCode);
    }

    [Fact]
    public async Task HeartbeatAndCloseRoom_WorkAsExpected()
    {
        var service = new InMemoryGameLinkService();
        var hostId = Guid.NewGuid();

        await service.CreateRoomAsync(
            hostId,
            "Trainer",
            new CreateLinkRoomRequest
            {
                GameId = "pkmn_yellow",
                GameTitle = "Pokemon Yellow",
                Core = "tgbdual",
                RoomCode = "9999",
            },
            CancellationToken.None);

        var hbOk = await service.HeartbeatRoomAsync("9999", hostId, CancellationToken.None);
        Assert.True(hbOk);

        var closeOk = await service.CloseRoomAsync("9999", hostId, CancellationToken.None);
        Assert.True(closeOk);

        var postClose = await service.GetRoomAsync("9999", CancellationToken.None);
        Assert.Null(postClose);
    }
}

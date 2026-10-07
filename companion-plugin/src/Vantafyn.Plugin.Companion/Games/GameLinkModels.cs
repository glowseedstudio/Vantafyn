using System;
using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Games;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum GameLinkRoomStatus
{
    Waiting,
    Connected,
    Closed
}

public sealed class CreateLinkRoomRequest
{
    public string GameId { get; set; } = string.Empty;
    public string GameTitle { get; set; } = string.Empty;
    public string Core { get; set; } = string.Empty;
    public string? RoomCode { get; set; }
    public string? PlayerName { get; set; }
    public int Port { get; set; } = 6400;
}

public sealed class JoinLinkRoomRequest
{
    public string? PlayerName { get; set; }
}

public sealed class GameLinkRoomDto
{
    public string RoomCode { get; set; } = string.Empty;
    public string GameId { get; set; } = string.Empty;
    public string GameTitle { get; set; } = string.Empty;
    public string Core { get; set; } = string.Empty;
    public string HostUserId { get; set; } = string.Empty;
    public string HostPlayerName { get; set; } = string.Empty;
    public int Port { get; set; }
    public GameLinkRoomStatus Status { get; set; } = GameLinkRoomStatus.Waiting;
    public string? ClientUserId { get; set; }
    public string? ClientPlayerName { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset LastHeartbeat { get; set; }
    public string RelayEndpoint { get; set; } = string.Empty;
}

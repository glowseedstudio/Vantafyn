using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;

namespace Vantafyn.Plugin.Companion.Games;

public sealed class InMemoryGameLinkService : IGameLinkService
{
    private sealed class RoomState
    {
        public required string RoomCode { get; init; }
        public required string GameId { get; init; }
        public required string GameTitle { get; init; }
        public required string Core { get; init; }
        public required Guid HostUserId { get; init; }
        public required string HostPlayerName { get; init; }
        public required int Port { get; init; }
        public GameLinkRoomStatus Status { get; set; } = GameLinkRoomStatus.Waiting;
        public Guid? ClientUserId { get; set; }
        public string? ClientPlayerName { get; set; }
        public DateTimeOffset CreatedAt { get; init; } = DateTimeOffset.UtcNow;
        public DateTimeOffset LastHeartbeat { get; set; } = DateTimeOffset.UtcNow;

        public WebSocket? HostSocket { get; set; }
        public WebSocket? ClientSocket { get; set; }
        public TaskCompletionSource<bool> ReadySignal { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);

        public GameLinkRoomDto ToDto() => new()
        {
            RoomCode = RoomCode,
            GameId = GameId,
            GameTitle = GameTitle,
            Core = Core,
            HostUserId = HostUserId.ToString("N"),
            HostPlayerName = HostPlayerName,
            Port = Port,
            Status = Status,
            ClientUserId = ClientUserId?.ToString("N"),
            ClientPlayerName = ClientPlayerName,
            CreatedAt = CreatedAt,
            LastHeartbeat = LastHeartbeat,
            RelayEndpoint = $"/Vantafyn/Games/Link/Relay?roomCode={RoomCode}"
        };
    }

    private readonly ConcurrentDictionary<string, RoomState> _rooms = new(StringComparer.OrdinalIgnoreCase);
    private readonly ILogger<InMemoryGameLinkService>? _logger;

    public InMemoryGameLinkService(ILogger<InMemoryGameLinkService>? logger = null)
    {
        _logger = logger;
    }

    public Task<GameLinkRoomDto> CreateRoomAsync(
        Guid userId,
        string userName,
        CreateLinkRoomRequest request,
        CancellationToken cancellationToken)
    {
        CleanupExpiredRooms();

        var roomCode = !string.IsNullOrWhiteSpace(request.RoomCode)
            ? request.RoomCode.Trim().ToUpperInvariant()
            : GenerateRoomCode();

        var state = new RoomState
        {
            RoomCode = roomCode,
            GameId = request.GameId,
            GameTitle = request.GameTitle,
            Core = request.Core,
            HostUserId = userId,
            HostPlayerName = string.IsNullOrWhiteSpace(request.PlayerName) ? userName : request.PlayerName.Trim(),
            Port = request.Port > 0 ? request.Port : 6400,
        };

        _rooms[roomCode] = state;
        _logger?.LogInformation("Created link room {RoomCode} for game {GameTitle} ({Core}) by host {Host}",
            roomCode, request.GameTitle, request.Core, state.HostPlayerName);

        return Task.FromResult(state.ToDto());
    }

    public Task<GameLinkRoomDto?> GetRoomAsync(string roomCode, CancellationToken cancellationToken)
    {
        CleanupExpiredRooms();
        if (_rooms.TryGetValue(roomCode.Trim(), out var state))
        {
            return Task.FromResult<GameLinkRoomDto?>(state.ToDto());
        }
        return Task.FromResult<GameLinkRoomDto?>(null);
    }

    public Task<IReadOnlyList<GameLinkRoomDto>> GetActiveRoomsAsync(
        string? gameId,
        string? core,
        CancellationToken cancellationToken)
    {
        CleanupExpiredRooms();

        var query = _rooms.Values.AsEnumerable();
        if (!string.IsNullOrWhiteSpace(gameId))
        {
            query = query.Where(r => string.Equals(r.GameId, gameId, StringComparison.OrdinalIgnoreCase));
        }
        if (!string.IsNullOrWhiteSpace(core))
        {
            query = query.Where(r => string.Equals(r.Core, core, StringComparison.OrdinalIgnoreCase));
        }

        var list = query
            .Where(r => r.Status != GameLinkRoomStatus.Closed)
            .OrderByDescending(r => r.CreatedAt)
            .Select(r => r.ToDto())
            .ToList();

        return Task.FromResult<IReadOnlyList<GameLinkRoomDto>>(list);
    }

    public Task<GameLinkRoomDto> JoinRoomAsync(
        Guid userId,
        string userName,
        string roomCode,
        JoinLinkRoomRequest request,
        CancellationToken cancellationToken)
    {
        CleanupExpiredRooms();

        if (!_rooms.TryGetValue(roomCode.Trim(), out var state) || state.Status == GameLinkRoomStatus.Closed)
        {
            throw new KeyNotFoundException($"Room {roomCode} was not found or has expired.");
        }

        if (state.Status == GameLinkRoomStatus.Connected && state.ClientUserId != userId)
        {
            throw new InvalidOperationException($"Room {roomCode} is already full.");
        }

        state.ClientUserId = userId;
        state.ClientPlayerName = string.IsNullOrWhiteSpace(request.PlayerName) ? userName : request.PlayerName.Trim();
        state.Status = GameLinkRoomStatus.Connected;
        state.LastHeartbeat = DateTimeOffset.UtcNow;

        _logger?.LogInformation("User {Client} joined link room {RoomCode}", state.ClientPlayerName, roomCode);

        return Task.FromResult(state.ToDto());
    }

    public Task<bool> HeartbeatRoomAsync(string roomCode, Guid userId, CancellationToken cancellationToken)
    {
        if (_rooms.TryGetValue(roomCode.Trim(), out var state) && state.Status != GameLinkRoomStatus.Closed)
        {
            if (state.HostUserId == userId || state.ClientUserId == userId)
            {
                state.LastHeartbeat = DateTimeOffset.UtcNow;
                return Task.FromResult(true);
            }
        }
        return Task.FromResult(false);
    }

    public Task<bool> CloseRoomAsync(string roomCode, Guid userId, CancellationToken cancellationToken)
    {
        if (_rooms.TryGetValue(roomCode.Trim(), out var state))
        {
            if (state.HostUserId == userId || state.ClientUserId == userId)
            {
                state.Status = GameLinkRoomStatus.Closed;
                _rooms.TryRemove(roomCode.Trim(), out _);
                try { state.HostSocket?.Abort(); } catch { }
                try { state.ClientSocket?.Abort(); } catch { }
                return Task.FromResult(true);
            }
        }
        return Task.FromResult(false);
    }

    public async Task HandleRelayWebSocketAsync(
        HttpContext httpContext,
        string roomCode,
        string role,
        CancellationToken cancellationToken)
    {
        if (!_rooms.TryGetValue(roomCode.Trim(), out var state) || state.Status == GameLinkRoomStatus.Closed)
        {
            httpContext.Response.StatusCode = StatusCodes.Status404NotFound;
            return;
        }

        using var webSocket = await httpContext.WebSockets.AcceptWebSocketAsync().ConfigureAwait(false);
        var isHost = string.Equals(role, "host", StringComparison.OrdinalIgnoreCase);

        if (isHost)
        {
            state.HostSocket = webSocket;
            state.LastHeartbeat = DateTimeOffset.UtcNow;
            _logger?.LogInformation("Host WebSocket attached to room {RoomCode}", roomCode);
        }
        else
        {
            state.ClientSocket = webSocket;
            state.Status = GameLinkRoomStatus.Connected;
            state.LastHeartbeat = DateTimeOffset.UtcNow;
            state.ReadySignal.TrySetResult(true);
            _logger?.LogInformation("Client WebSocket attached to room {RoomCode}", roomCode);
        }

        try
        {
            // If host, wait until client connects or cancel
            if (isHost)
            {
                using var linkedCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
                linkedCts.CancelAfter(TimeSpan.FromMinutes(10));
                await state.ReadySignal.Task.WaitAsync(linkedCts.Token).ConfigureAwait(false);
            }

            // Both peers are now attached - bridge traffic!
            var hostSocket = state.HostSocket;
            var clientSocket = state.ClientSocket;

            if (hostSocket != null && clientSocket != null)
            {
                if (isHost)
                {
                    await PipeTrafficAsync(hostSocket, clientSocket, roomCode, "Host->Client", cancellationToken).ConfigureAwait(false);
                }
                else
                {
                    await PipeTrafficAsync(clientSocket, hostSocket, roomCode, "Client->Host", cancellationToken).ConfigureAwait(false);
                }
            }
            else
            {
                // Wait for closure
                var buffer = new byte[1024];
                while (webSocket.State == WebSocketState.Open && !cancellationToken.IsCancellationRequested)
                {
                    var res = await webSocket.ReceiveAsync(buffer, cancellationToken).ConfigureAwait(false);
                    if (res.MessageType == WebSocketMessageType.Close) break;
                }
            }
        }
        catch (OperationCanceledException)
        {
            // Expected on timeout or disconnect
        }
        catch (Exception ex)
        {
            _logger?.LogWarning(ex, "Relay WebSocket error in room {RoomCode} ({Role})", roomCode, role);
        }
        finally
        {
            if (isHost) state.HostSocket = null;
            else state.ClientSocket = null;

            if (webSocket.State == WebSocketState.Open || webSocket.State == WebSocketState.CloseReceived)
            {
                try
                {
                    await webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Session ended", CancellationToken.None).ConfigureAwait(false);
                }
                catch { }
            }
        }
    }

    private static async Task PipeTrafficAsync(
        WebSocket source,
        WebSocket target,
        string roomCode,
        string label,
        CancellationToken cancellationToken)
    {
        var buffer = new byte[8192];
        while (source.State == WebSocketState.Open && target.State == WebSocketState.Open && !cancellationToken.IsCancellationRequested)
        {
            var result = await source.ReceiveAsync(new ArraySegment<byte>(buffer), cancellationToken).ConfigureAwait(false);
            if (result.MessageType == WebSocketMessageType.Close)
            {
                if (target.State == WebSocketState.Open)
                {
                    await target.CloseOutputAsync(WebSocketCloseStatus.NormalClosure, "Peer closed connection", cancellationToken).ConfigureAwait(false);
                }
                break;
            }

            if (result.Count > 0 && target.State == WebSocketState.Open)
            {
                await target.SendAsync(
                    new ArraySegment<byte>(buffer, 0, result.Count),
                    result.MessageType,
                    result.EndOfMessage,
                    cancellationToken).ConfigureAwait(false);
            }
        }
    }

    private void CleanupExpiredRooms()
    {
        var cutoff = DateTimeOffset.UtcNow.AddMinutes(-5);
        foreach (var (code, room) in _rooms)
        {
            if (room.LastHeartbeat < cutoff || room.Status == GameLinkRoomStatus.Closed)
            {
                if (_rooms.TryRemove(code, out var removed))
                {
                    try { removed.HostSocket?.Abort(); } catch { }
                    try { removed.ClientSocket?.Abort(); } catch { }
                }
            }
        }
    }

    private static string GenerateRoomCode()
    {
        var pin = RandomNumberGenerator.GetInt32(1000, 9999);
        return pin.ToString();
    }
}

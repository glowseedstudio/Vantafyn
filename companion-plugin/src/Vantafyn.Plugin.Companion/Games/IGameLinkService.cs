using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;

namespace Vantafyn.Plugin.Companion.Games;

public interface IGameLinkService
{
    Task<GameLinkRoomDto> CreateRoomAsync(Guid userId, string userName, CreateLinkRoomRequest request, CancellationToken cancellationToken);
    Task<GameLinkRoomDto?> GetRoomAsync(string roomCode, CancellationToken cancellationToken);
    Task<IReadOnlyList<GameLinkRoomDto>> GetActiveRoomsAsync(string? gameId, string? core, CancellationToken cancellationToken);
    Task<GameLinkRoomDto> JoinRoomAsync(Guid userId, string userName, string roomCode, JoinLinkRoomRequest request, CancellationToken cancellationToken);
    Task<bool> HeartbeatRoomAsync(string roomCode, Guid userId, CancellationToken cancellationToken);
    Task<bool> CloseRoomAsync(string roomCode, Guid userId, CancellationToken cancellationToken);
    Task HandleRelayWebSocketAsync(HttpContext httpContext, string roomCode, string role, CancellationToken cancellationToken);
}

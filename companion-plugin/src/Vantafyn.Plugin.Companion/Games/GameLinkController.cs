using System;
using System.Collections.Generic;
using System.Net.Mime;
using System.Threading;
using System.Threading.Tasks;
using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Games;

[ApiController]
[Authorize]
[Route("Vantafyn/Games/Link")]
public sealed class GameLinkController : ControllerBase
{
    private readonly IGameLinkService _linkService;
    private readonly IAuthorizationContext _authorizationContext;

    public GameLinkController(
        IGameLinkService linkService,
        IAuthorizationContext authorizationContext)
    {
        _linkService = linkService;
        _authorizationContext = authorizationContext;
    }

    private static bool GamesEnabled() =>
        Plugin.Instance?.Configuration?.GamesEnabled != false;

    [HttpPost("Rooms")]
    [Consumes(MediaTypeNames.Application.Json)]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<ActionResult<GameLinkRoomDto>> CreateRoom(
        [FromBody] CreateLinkRoomRequest request,
        CancellationToken cancellationToken)
    {
        if (!GamesEnabled()) return Forbid();
        if (string.IsNullOrWhiteSpace(request.GameId))
        {
            return BadRequest("GameId is required.");
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var userName = await this.CurrentUserNameAsync(_authorizationContext).ConfigureAwait(false);

        var room = await _linkService.CreateRoomAsync(userId, userName, request, cancellationToken).ConfigureAwait(false);
        return Ok(room);
    }

    [HttpGet("Rooms")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<GameLinkRoomDto>>> GetRooms(
        [FromQuery] string? gameId = null,
        [FromQuery] string? core = null,
        CancellationToken cancellationToken = default)
    {
        if (!GamesEnabled()) return Ok(Array.Empty<GameLinkRoomDto>());

        var rooms = await _linkService.GetActiveRoomsAsync(gameId, core, cancellationToken).ConfigureAwait(false);
        return Ok(rooms);
    }

    [HttpGet("Rooms/{roomCode}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<GameLinkRoomDto>> GetRoom(
        [FromRoute] string roomCode,
        CancellationToken cancellationToken)
    {
        if (!GamesEnabled()) return NotFound();

        var room = await _linkService.GetRoomAsync(roomCode, cancellationToken).ConfigureAwait(false);
        if (room == null) return NotFound($"Room '{roomCode}' not found.");

        return Ok(room);
    }

    [HttpPost("Rooms/{roomCode}/Join")]
    [Consumes(MediaTypeNames.Application.Json)]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    public async Task<ActionResult<GameLinkRoomDto>> JoinRoom(
        [FromRoute] string roomCode,
        [FromBody] JoinLinkRoomRequest request,
        CancellationToken cancellationToken)
    {
        if (!GamesEnabled()) return Forbid();

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var userName = await this.CurrentUserNameAsync(_authorizationContext).ConfigureAwait(false);

        try
        {
            var room = await _linkService.JoinRoomAsync(userId, userName, roomCode, request, cancellationToken).ConfigureAwait(false);
            return Ok(room);
        }
        catch (KeyNotFoundException)
        {
            return NotFound($"Room '{roomCode}' not found.");
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(ex.Message);
        }
    }

    [HttpPost("Rooms/{roomCode}/Heartbeat")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult> Heartbeat(
        [FromRoute] string roomCode,
        CancellationToken cancellationToken)
    {
        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var success = await _linkService.HeartbeatRoomAsync(roomCode, userId, cancellationToken).ConfigureAwait(false);
        if (!success) return NotFound();
        return Ok();
    }

    [HttpDelete("Rooms/{roomCode}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult> CloseRoom(
        [FromRoute] string roomCode,
        CancellationToken cancellationToken)
    {
        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var success = await _linkService.CloseRoomAsync(roomCode, userId, cancellationToken).ConfigureAwait(false);
        if (!success) return NotFound();
        return Ok();
    }

    [AllowAnonymous]
    [HttpGet("Relay")]
    public async Task Relay(
        [FromQuery] string roomCode,
        [FromQuery] string role,
        CancellationToken cancellationToken)
    {
        if (!HttpContext.WebSockets.IsWebSocketRequest)
        {
            HttpContext.Response.StatusCode = StatusCodes.Status400BadRequest;
            await HttpContext.Response.WriteAsync("WebSocket connection expected.", cancellationToken).ConfigureAwait(false);
            return;
        }

        await _linkService.HandleRelayWebSocketAsync(HttpContext, roomCode, role, cancellationToken).ConfigureAwait(false);
    }
}

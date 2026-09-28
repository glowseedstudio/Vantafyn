using System;
using System.IO;
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
[Route("Vantafyn/Games/Saves")]
public sealed class GameSavesController : ControllerBase
{
    private readonly IGameSavesService _savesService;
    private readonly IAuthorizationContext _authorizationContext;

    private const long MaxSaveBytes = 32 * 1024 * 1024; // 32 MB ceiling

    public GameSavesController(IGameSavesService savesService, IAuthorizationContext authorizationContext)
    {
        _savesService = savesService;
        _authorizationContext = authorizationContext;
    }

    private static bool GamesEnabled() =>
        Plugin.Instance?.Configuration?.GamesEnabled != false;

    [HttpGet("{gameId}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetSave(
        [FromRoute] string gameId,
        [FromQuery] string? kind,
        CancellationToken cancellationToken)
    {
        if (!GamesEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var data = await _savesService.GetAsync(userId, gameId, kind ?? "state", cancellationToken).ConfigureAwait(false);
        if (data == null)
        {
            return NotFound();
        }

        return File(data, "application/octet-stream");
    }

    [HttpPut("{gameId}")]
    [RequestSizeLimit(MaxSaveBytes)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> PutSave(
        [FromRoute] string gameId,
        [FromQuery] string? kind,
        CancellationToken cancellationToken)
    {
        if (!GamesEnabled())
        {
            return NotFound();
        }

        if (string.IsNullOrWhiteSpace(gameId))
        {
            return BadRequest(new { error = "Game id is required." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);

        using var ms = new MemoryStream();
        await Request.Body.CopyToAsync(ms, cancellationToken).ConfigureAwait(false);
        var bytes = ms.ToArray();

        if (bytes.Length > MaxSaveBytes)
        {
            return StatusCode(StatusCodes.Status413PayloadTooLarge, new { error = "Save blob exceeds 32 MB ceiling." });
        }

        await _savesService.SaveAsync(userId, gameId, kind ?? "state", bytes, cancellationToken).ConfigureAwait(false);
        return NoContent();
    }

    [HttpDelete("{gameId}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> DeleteSave(
        [FromRoute] string gameId,
        [FromQuery] string? kind,
        CancellationToken cancellationToken)
    {
        if (!GamesEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var deleted = await _savesService.DeleteAsync(userId, gameId, kind ?? "state", cancellationToken).ConfigureAwait(false);
        return deleted ? NoContent() : NotFound();
    }
}

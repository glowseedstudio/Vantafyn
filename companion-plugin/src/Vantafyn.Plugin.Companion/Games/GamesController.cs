using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Mime;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace Vantafyn.Plugin.Companion.Games;

[ApiController]
[Authorize]
[Route("Vantafyn/Games")]
public sealed class GamesController : ControllerBase
{
    private readonly IGamesService _gamesService;

    public GamesController(IGamesService gamesService)
    {
        _gamesService = gamesService;
    }

    private static bool GamesEnabled() =>
        Plugin.Instance?.Configuration?.GamesEnabled != false;

    [HttpGet("Libraries")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public ActionResult<IEnumerable<GameLibrary>> GetLibraries()
    {
        if (!GamesEnabled())
        {
            return Ok(Array.Empty<GameLibrary>());
        }

        return Ok(_gamesService.GetGameLibraries());
    }

    [HttpGet("{libraryId}/Systems")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public ActionResult<IEnumerable<GameSystem>> GetSystems([FromRoute] string libraryId)
    {
        if (!GamesEnabled())
        {
            return Ok(Array.Empty<GameSystem>());
        }

        return Ok(_gamesService.GetSystems(libraryId));
    }

    [HttpGet("{libraryId}/Systems/{systemId}/Games")]
    [HttpGet("{libraryId}/Games")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public ActionResult<IEnumerable<GameSummary>> GetGames(
        [FromRoute] string libraryId,
        [FromRoute] string? systemId = null,
        [FromQuery] string? system = null)
    {
        if (!GamesEnabled())
        {
            return Ok(Array.Empty<GameSummary>());
        }

        var effectiveSystem = !string.IsNullOrWhiteSpace(systemId) ? systemId : system;
        return Ok(_gamesService.GetGames(libraryId, effectiveSystem));
    }

    [HttpGet("{libraryId}/Games/{gameId}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public ActionResult<GameDetail> GetGame(
        [FromRoute] string libraryId,
        [FromRoute] string gameId)
    {
        if (!GamesEnabled())
        {
            return NotFound();
        }

        var game = _gamesService.GetGame(libraryId, gameId);
        return game == null ? NotFound() : Ok(game);
    }

    [HttpGet("{libraryId}/Games/{gameId}/Boxart")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status302Found)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public IActionResult GetBoxart([FromRoute] string libraryId, [FromRoute] string gameId)
    {
        if (!GamesEnabled())
        {
            return NotFound();
        }

        var localPath = _gamesService.GetBoxartPath(libraryId, gameId);
        if (!string.IsNullOrEmpty(localPath) && System.IO.File.Exists(localPath))
        {
            var ext = Path.GetExtension(localPath).ToLowerInvariant();
            var contentType = ext switch
            {
                ".jpg" or ".jpeg" => "image/jpeg",
                ".webp" => "image/webp",
                _ => "image/png"
            };
            return PhysicalFile(localPath, contentType);
        }

        var game = _gamesService.GetGame(libraryId, gameId);
        if (game != null && !string.IsNullOrEmpty(game.BoxartUrl) && game.BoxartUrl.StartsWith("http", StringComparison.OrdinalIgnoreCase))
        {
            return Redirect(game.BoxartUrl);
        }

        return NotFound();
    }

    [HttpGet("{libraryId}/ROM/{token}")]
    [HttpHead("{libraryId}/ROM/{token}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status206PartialContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    [ProducesResponseType(StatusCodes.Status413PayloadTooLarge)]
    public IActionResult GetRom([FromRoute] string libraryId, [FromRoute] string token)
    {
        if (!GamesEnabled())
        {
            return NotFound();
        }

        var path = _gamesService.ResolveFilePath(libraryId, token, allowBios: false);
        if (string.IsNullOrEmpty(path) || !System.IO.File.Exists(path))
        {
            return NotFound();
        }

        if (GameSystemCoreResolver.IsArchive(path))
        {
            if (HttpMethods.IsHead(Request.Method))
            {
                var info = _gamesService.GetExtractedRomInfo(path);
                if (info == null)
                {
                    return NotFound();
                }

                Response.ContentType = "application/octet-stream";
                Response.ContentLength = info.Length;
                Response.Headers.AcceptRanges = "bytes";
                return new EmptyResult();
            }

            try
            {
                var romBytes = _gamesService.ExtractRomFromArchive(path);
                if (romBytes == null || romBytes.Length == 0)
                {
                    return NotFound();
                }

                return File(romBytes, "application/octet-stream", enableRangeProcessing: true);
            }
            catch (RomTooLargeException ex)
            {
                return StatusCode(
                    StatusCodes.Status413PayloadTooLarge,
                    new { error = $"ROM exceeds the {ex.MaxBytes / (1024 * 1024)} MB limit for in-memory extraction." });
            }
        }

        return PhysicalFile(path, "application/octet-stream", enableRangeProcessing: true);
    }

    [HttpGet("{libraryId}/Bios/{token}")]
    [HttpHead("{libraryId}/Bios/{token}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public IActionResult GetBios([FromRoute] string libraryId, [FromRoute] string token)
    {
        if (!GamesEnabled())
        {
            return NotFound();
        }

        var path = _gamesService.ResolveFilePath(libraryId, token, allowBios: true);
        if (string.IsNullOrEmpty(path) || !System.IO.File.Exists(path))
        {
            return NotFound();
        }

        return PhysicalFile(path, "application/octet-stream", enableRangeProcessing: true);
    }

    [HttpGet("Debug")]
    [Authorize(Policy = "RequiresElevation")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public ActionResult<object> Debug() => Ok(_gamesService.GetDiagnostics());
}

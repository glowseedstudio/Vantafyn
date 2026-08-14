using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.PersonalPlaylists;

[ApiController]
[Authorize]
[Route("Vantafyn/PersonalPlaylists")]
public sealed class PersonalPlaylistsController(
    IAuthorizationContext authorizationContext,
    IPersonalPlaylistStore playlists) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> List(CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.PersonalPlaylistsEnabled != true) return StatusCode(503, new { error = "Personal playlists are disabled." });
        var userId = this.CurrentUserId(authorizationContext);
        return Ok(new { playlists = await playlists.ListAsync(userId, cancellationToken).ConfigureAwait(false) });
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreatePersonalPlaylistRequest request, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        return Ok(await playlists.CreateAsync(userId, request, cancellationToken).ConfigureAwait(false));
    }

    [HttpPost("{playlistId}/Items")]
    public async Task<IActionResult> AddItem(string playlistId, [FromBody] AddPersonalPlaylistItemRequest request, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var playlist = await playlists.AddItemAsync(userId, playlistId, request, cancellationToken).ConfigureAwait(false);
        return playlist == null ? NotFound(new { error = "Playlist was not found." }) : Ok(playlist);
    }
}

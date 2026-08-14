using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.WatchParties;

[ApiController]
[Authorize]
[Route("Vantafyn/WatchParties")]
public sealed class WatchPartiesController(
    IAuthorizationContext authorizationContext,
    IWatchPartyService watchParties) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateWatchPartyRequest request, CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.WatchPartiesEnabled != true) return StatusCode(503, new { error = "Watch Parties are disabled." });
        var userId = this.CurrentUserId(authorizationContext);
        return Ok(await watchParties.CreateAsync(userId, request, cancellationToken).ConfigureAwait(false));
    }

    [HttpGet("{partyId}")]
    public async Task<IActionResult> Get(string partyId, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var party = await watchParties.GetAsync(partyId, userId, cancellationToken).ConfigureAwait(false);
        return party == null ? NotFound(new { error = "Party was not found." }) : Ok(party);
    }

    [HttpGet("{partyId}/Snapshot")]
    public async Task<IActionResult> Snapshot(string partyId, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var party = await watchParties.GetAsync(partyId, userId, cancellationToken).ConfigureAwait(false);
        return party == null ? NotFound(new { error = "Party was not found." }) : Ok(party.Snapshot);
    }

    [HttpPost("{partyId}/Join")]
    public async Task<IActionResult> Join(string partyId, [FromBody] JoinRequest request, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        return Ok(await watchParties.JoinAsync(partyId, userId, request.InviteId, cancellationToken).ConfigureAwait(false));
    }

    [HttpPost("{partyId}/Leave")]
    public async Task<IActionResult> Leave(string partyId, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        await watchParties.LeaveAsync(partyId, userId, cancellationToken).ConfigureAwait(false);
        return NoContent();
    }

    [HttpPost("{partyId}/End")]
    public async Task<IActionResult> End(string partyId, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        await watchParties.EndAsync(partyId, userId, cancellationToken).ConfigureAwait(false);
        return NoContent();
    }

    [HttpPost("{partyId}/Commands")]
    public async Task<IActionResult> Command(string partyId, [FromBody] WatchPartyCommandRequest request, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        return Ok(await watchParties.CommandAsync(partyId, userId, request, cancellationToken).ConfigureAwait(false));
    }

    [HttpPost("{partyId}/Invites")]
    public async Task<IActionResult> Invite(string partyId, [FromBody] InviteRequest request, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        return Ok(await watchParties.InviteAsync(partyId, userId, request, cancellationToken).ConfigureAwait(false));
    }
}

public sealed record JoinRequest(string? InviteId);

using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.UserSettings;

[ApiController]
[Authorize]
[Route("Vantafyn/UserSettings")]
public sealed class UserSettingsController(
    IAuthorizationContext authorizationContext,
    IUserSettingsStore store) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Get(CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.UserSettingsEnabled != true) return StatusCode(503, new { error = "User settings sync is disabled." });
        var userId = this.CurrentUserId(authorizationContext);
        var envelope = await store.GetAsync(userId, cancellationToken).ConfigureAwait(false);
        if (envelope == null) return NotFound(new { error = "No Vantafyn settings have been stored for this user." });
        return Ok(ToResponse(envelope));
    }

    [HttpPut]
    public async Task<IActionResult> Put([FromBody] UserSettingsPutRequest request, CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.UserSettingsEnabled != true) return StatusCode(503, new { error = "User settings sync is disabled." });
        var userId = this.CurrentUserId(authorizationContext);
        try
        {
            var envelope = await store.SaveAsync(userId, request, cancellationToken).ConfigureAwait(false);
            return Ok(ToResponse(envelope));
        }
        catch (RevisionConflictException conflict)
        {
            var current = await store.GetAsync(userId, cancellationToken).ConfigureAwait(false);
            return Conflict(new { error = "Revision conflict.", currentRevision = conflict.CurrentRevision, current });
        }
        catch (PayloadTooLargeException ex)
        {
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpDelete]
    public async Task<IActionResult> Delete(CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.UserSettingsEnabled != true) return StatusCode(503, new { error = "User settings sync is disabled." });
        var userId = this.CurrentUserId(authorizationContext);
        await store.DeleteAsync(userId, cancellationToken).ConfigureAwait(false);
        return NoContent();
    }

    private static UserSettingsResponse ToResponse(UserSettingsEnvelope envelope) => new(
        envelope.SchemaVersion,
        envelope.Revision,
        envelope.UpdatedAtUtc,
        envelope.AppVersion,
        envelope.Shared,
        envelope.Mobile,
        envelope.Tv);
}

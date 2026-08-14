using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Requests;

namespace Vantafyn.Plugin.Companion.Api;

[ApiController]
[Authorize]
[Route("Vantafyn")]
public sealed class CapabilitiesController(
    IAuthorizationContext authorizationContext,
    IOmbiUserSessionStore ombiUserSessionStore) : ControllerBase
{
    [HttpGet("Capabilities")]
    public async Task<IActionResult> GetCapabilities(CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        var requestsConfigured = config.RequestsEnabled &&
            !string.IsNullOrWhiteSpace(config.Ombi.BaseUrl) &&
            !string.IsNullOrWhiteSpace(config.Ombi.ApiKey);
        var ombiSession = requestsConfigured && config.Ombi.RequireUserLogin
            ? await ombiUserSessionStore.ReadAsync(userId, cancellationToken).ConfigureAwait(false)
            : null;

        return Ok(new
        {
            pluginVersion = "0.1.0",
            apiVersion = 1,
            userSettings = new
            {
                state = config.UserSettingsEnabled ? "ready" : "disabled"
            },
            requests = new
            {
                state = !config.RequestsEnabled ? "disabled" : requestsConfigured ? "ready" : "unconfigured",
                provider = "ombi",
                serverConfigured = requestsConfigured,
                requiresUserLogin = requestsConfigured && config.Ombi.RequireUserLogin,
                userLinked = requestsConfigured && (!config.Ombi.RequireUserLogin || ombiSession != null),
            },
            watchParties = new
            {
                state = config.WatchPartiesEnabled ? "ready" : "disabled"
            },
            personalPlaylists = new
            {
                state = config.PersonalPlaylistsEnabled ? "ready" : "disabled"
            },
            notifications = new
            {
                liveSession = config.NotificationsEnabled,
                backgroundPush = false
            }
        });
    }
}

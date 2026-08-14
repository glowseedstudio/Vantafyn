using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Requests;

namespace Vantafyn.Plugin.Companion.Api;

[ApiController]
[Authorize(Policy = "RequiresElevation")]
[Route("Vantafyn/Admin")]
public sealed class AdminController(
    ICompanionDiagnostics diagnostics,
    IOmbiClientFactory ombiClientFactory) : ControllerBase
{
    [HttpGet("Configuration")]
    public IActionResult GetConfiguration()
    {
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        return Ok(new
        {
            config.UserSettingsEnabled,
            config.RequestsEnabled,
            config.WatchPartiesEnabled,
            config.PersonalPlaylistsEnabled,
            config.NotificationsEnabled,
            ombi = new
            {
                config.Ombi.BaseUrl,
                hasApiKey = !string.IsNullOrWhiteSpace(config.Ombi.ApiKey),
                config.Ombi.TimeoutSeconds,
                config.Ombi.RequireUserLogin
            }
        });
    }

    [HttpPost("Configuration")]
    public IActionResult SaveConfiguration([FromBody] AdminConfigurationRequest request)
    {
        var plugin = Plugin.Instance ?? throw new InvalidOperationException("Plugin instance is unavailable.");
        var config = plugin.Configuration;
        config.UserSettingsEnabled = request.UserSettingsEnabled;
        config.RequestsEnabled = request.RequestsEnabled;
        config.WatchPartiesEnabled = request.WatchPartiesEnabled;
        config.PersonalPlaylistsEnabled = request.PersonalPlaylistsEnabled;
        config.NotificationsEnabled = request.NotificationsEnabled;
        config.Ombi.BaseUrl = request.OmbiBaseUrl?.Trim();
        if (!string.IsNullOrWhiteSpace(request.OmbiApiKey))
        {
            config.Ombi.ApiKey = request.OmbiApiKey.Trim();
        }
        config.Ombi.TimeoutSeconds = Math.Clamp(request.OmbiTimeoutSeconds, 2, 30);
        config.Ombi.RequireUserLogin = request.OmbiRequireUserLogin;
        plugin.SaveConfiguration();
        return Ok(GetConfigurationPayload(config));
    }

    [HttpPost("Requests/TestConnection")]
    public async Task<IActionResult> TestOmbiConnection(CancellationToken cancellationToken)
    {
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        var client = ombiClientFactory.Create(config.Ombi);
        var result = await client.TestConnectionAsync(cancellationToken).ConfigureAwait(false);
        return Ok(result);
    }

    [HttpGet("Diagnostics")]
    public IActionResult GetDiagnostics()
    {
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        return Ok(diagnostics.Snapshot(config));
    }

    private static object GetConfigurationPayload(PluginConfiguration config) => new
    {
        config.UserSettingsEnabled,
        config.RequestsEnabled,
        config.WatchPartiesEnabled,
        config.PersonalPlaylistsEnabled,
        config.NotificationsEnabled,
        ombi = new
        {
            config.Ombi.BaseUrl,
            hasApiKey = !string.IsNullOrWhiteSpace(config.Ombi.ApiKey),
            config.Ombi.TimeoutSeconds,
            config.Ombi.RequireUserLogin
        }
    };
}

public sealed record AdminConfigurationRequest(
    bool UserSettingsEnabled,
    bool RequestsEnabled,
    bool WatchPartiesEnabled,
    bool PersonalPlaylistsEnabled,
    bool NotificationsEnabled,
    string? OmbiBaseUrl,
    string? OmbiApiKey,
    int OmbiTimeoutSeconds,
    bool OmbiRequireUserLogin);

using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Requests;

[ApiController]
[Authorize]
[Route("Vantafyn/Requests")]
public sealed class RequestsController(
    IAuthorizationContext authorizationContext,
    IOmbiClientFactory ombiClientFactory,
    IOmbiUserSessionStore userSessionStore) : ControllerBase
{
    [HttpGet("Capabilities")]
    public async Task<IActionResult> Capabilities(CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        var configured = config.RequestsEnabled && !string.IsNullOrWhiteSpace(config.Ombi.BaseUrl) && !string.IsNullOrWhiteSpace(config.Ombi.ApiKey);
        var userSession = configured && config.Ombi.RequireUserLogin
            ? await userSessionStore.ReadAsync(userId, cancellationToken).ConfigureAwait(false)
            : null;
        return Ok(new
        {
            state = !config.RequestsEnabled ? "disabled" : configured ? "ready" : "unconfigured",
            provider = "ombi",
            serverConfigured = configured,
            requiresUserLogin = configured && config.Ombi.RequireUserLogin,
            userLinked = configured && (!config.Ombi.RequireUserLogin || userSession != null)
        });
    }

    [HttpGet("Search")]
    public async Task<IActionResult> Search([FromQuery] string query, [FromQuery] string? type, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        if (!config.RequestsEnabled) return StatusCode(503, new { error = "Requests are disabled." });
        if (string.IsNullOrWhiteSpace(config.Ombi.BaseUrl) || string.IsNullOrWhiteSpace(config.Ombi.ApiKey)) return StatusCode(503, new { error = "Requests are not configured." });
        var token = await UserTokenIfRequiredAsync(userId, config, cancellationToken).ConfigureAwait(false);
        if (token == MissingUserToken) return Unauthorized(new { error = "Link your Ombi account before using Requests." });
        var results = await ombiClientFactory.Create(config.Ombi).SearchAsync(query, type, token, cancellationToken).ConfigureAwait(false);
        return Ok(new { results });
    }

    [HttpGet("UserSession")]
    public async Task<IActionResult> UserSession(CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var existing = await userSessionStore.ReadAsync(userId, cancellationToken).ConfigureAwait(false);
        if (existing == null) return NotFound(new { linked = false });
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        var validated = await ombiClientFactory.Create(config.Ombi).ValidateUserAsync(existing, cancellationToken).ConfigureAwait(false);
        await userSessionStore.SaveAsync(userId, validated, validated.AccessToken, cancellationToken).ConfigureAwait(false);
        return Ok(new { linked = true, session = PublicSession(validated) });
    }

    [HttpPost("UserSession/Login")]
    public async Task<IActionResult> LoginUserSession([FromBody] OmbiUserLoginBody body, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        if (!config.RequestsEnabled) return StatusCode(503, new { error = "Requests are disabled." });
        if (string.IsNullOrWhiteSpace(config.Ombi.BaseUrl) || string.IsNullOrWhiteSpace(config.Ombi.ApiKey)) return StatusCode(503, new { error = "Requests are not configured." });
        var result = await ombiClientFactory.Create(config.Ombi).LoginUserAsync(body.Username, body.Password, cancellationToken).ConfigureAwait(false);
        if (!result.Success || result.Session == null) return Unauthorized(result);
        await userSessionStore.SaveAsync(userId, result.Session, result.Session.AccessToken, cancellationToken).ConfigureAwait(false);
        return Ok(new
        {
            result.Success,
            result.State,
            result.Message,
            session = PublicSession(result.Session)
        });
    }

    [HttpDelete("UserSession")]
    public async Task<IActionResult> DeleteUserSession(CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        await userSessionStore.DeleteAsync(userId, cancellationToken).ConfigureAwait(false);
        return Ok(new { linked = false });
    }

    [HttpPost("Movies")]
    public async Task<IActionResult> RequestMovie([FromBody] RequestCreateBody body, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var config = ReadyConfiguration();
        var client = ombiClientFactory.Create(config.Ombi);
        var token = await UserTokenIfRequiredAsync(userId, config, cancellationToken).ConfigureAwait(false);
        if (token == MissingUserToken) return Unauthorized(new { error = "Link your Ombi account before requesting movies." });
        var result = await client.RequestMovieAsync(body.ProviderId, token, cancellationToken).ConfigureAwait(false);
        return result.Success ? Ok(result) : StatusCode(502, result);
    }

    [HttpPost("Series")]
    public async Task<IActionResult> RequestSeries([FromBody] RequestCreateBody body, CancellationToken cancellationToken)
    {
        var userId = this.CurrentUserId(authorizationContext);
        var config = ReadyConfiguration();
        var client = ombiClientFactory.Create(config.Ombi);
        var token = await UserTokenIfRequiredAsync(userId, config, cancellationToken).ConfigureAwait(false);
        if (token == MissingUserToken) return Unauthorized(new { error = "Link your Ombi account before requesting series." });
        var result = await client.RequestSeriesAsync(body.ProviderId, token, cancellationToken).ConfigureAwait(false);
        return result.Success ? Ok(result) : StatusCode(502, result);
    }

    private PluginConfiguration ReadyConfiguration()
    {
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        if (!config.RequestsEnabled) throw new InvalidOperationException("Requests are disabled.");
        if (string.IsNullOrWhiteSpace(config.Ombi.BaseUrl) || string.IsNullOrWhiteSpace(config.Ombi.ApiKey)) throw new InvalidOperationException("Requests are not configured.");
        return config;
    }

    private const string MissingUserToken = "__missing_ombi_user_session__";

    private async Task<string?> UserTokenIfRequiredAsync(Guid userId, PluginConfiguration config, CancellationToken cancellationToken)
    {
        if (!config.Ombi.RequireUserLogin) return null;
        var session = await userSessionStore.ReadAsync(userId, cancellationToken).ConfigureAwait(false);
        return session?.AccessToken ?? MissingUserToken;
    }

    private static object PublicSession(OmbiLinkedUserSession session) => new
    {
        session.OmbiUserName,
        session.DisplayName,
        session.OmbiUserId,
        session.ExpiresAt,
        session.Roles,
        session.LastLoginAt,
        session.LastValidatedAt
    };
}

public sealed record RequestCreateBody(string ProviderId);
public sealed record OmbiUserLoginBody(string Username, string Password);

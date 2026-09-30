using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Pokemon;
using Vantafyn.Plugin.Companion.Requests;

namespace Vantafyn.Plugin.Companion.Api;

[ApiController]
[Authorize(Policy = "RequiresElevation")]
[Route("Vantafyn/Admin")]
public sealed class AdminController(
    ICompanionDiagnostics diagnostics,
    IOmbiClientFactory ombiClientFactory,
    IPokemonProviderFactory pokemonProviderFactory,
    IPokemonGameDetector pokemonGameDetector) : ControllerBase
{
    [HttpGet("Configuration")]
    public IActionResult GetConfiguration()
    {
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        return Ok(GetConfigurationPayload(config));
    }

    [HttpPost("Configuration")]
    public IActionResult SaveConfiguration([FromBody] AdminConfigurationRequest request)
    {
        var plugin = Plugin.Instance ?? throw new InvalidOperationException("Plugin instance is unavailable.");
        var config = plugin.Configuration;
        config.UserSettingsEnabled = request.UserSettingsEnabled;
        config.GamesEnabled = request.GamesEnabled;
        config.CustomGamesPath = string.IsNullOrWhiteSpace(request.CustomGamesPath) ? null : request.CustomGamesPath.Trim();
        if (request.GameLibraryIds != null)
        {
            config.GameLibraryIds = request.GameLibraryIds;
        }
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

        if (request.PokemonEnabled.HasValue)
        {
            config.Pokemon.Enabled = request.PokemonEnabled.Value;
        }
        if (!string.IsNullOrWhiteSpace(request.PokemonProviderType))
        {
            config.Pokemon.ProviderType = request.PokemonProviderType.Trim();
        }
        if (request.PokemonPkVaultBaseUrl != null)
        {
            config.Pokemon.PkVaultBaseUrl = string.IsNullOrWhiteSpace(request.PokemonPkVaultBaseUrl) ? null : request.PokemonPkVaultBaseUrl.Trim();
        }
        if (request.PokemonTimeoutSeconds.HasValue)
        {
            config.Pokemon.TimeoutSeconds = Math.Clamp(request.PokemonTimeoutSeconds.Value, 2, 30);
        }
        if (request.PokemonAllowTransfers.HasValue)
        {
            config.Pokemon.AllowTransfers = request.PokemonAllowTransfers.Value;
        }
        if (request.PokemonAllowCrossGenerationTransfers.HasValue)
        {
            config.Pokemon.AllowCrossGenerationTransfers = request.PokemonAllowCrossGenerationTransfers.Value;
        }
        if (request.PokemonAllowTrading.HasValue)
        {
            config.Pokemon.AllowTrading = request.PokemonAllowTrading.Value;
        }
        if (request.PokemonAllowEditing.HasValue)
        {
            config.Pokemon.AllowEditing = request.PokemonAllowEditing.Value;
        }
        if (request.PokemonAutoBackups.HasValue)
        {
            config.Pokemon.AutoBackups = request.PokemonAutoBackups.Value;
        }

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

    [HttpPost("Pokemon/TestConnection")]
    public async Task<IActionResult> TestPokemonConnection(CancellationToken cancellationToken)
    {
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        var provider = pokemonProviderFactory.Create(config.Pokemon);
        var result = await provider.TestConnectionAsync(cancellationToken).ConfigureAwait(false);
        return Ok(result);
    }

    [HttpGet("Pokemon/Overrides")]
    public async Task<IActionResult> GetPokemonOverrides(CancellationToken cancellationToken)
    {
        var overrides = await pokemonGameDetector.GetOverridesAsync(cancellationToken).ConfigureAwait(false);
        return Ok(overrides);
    }

    [HttpPost("Pokemon/Overrides")]
    public async Task<IActionResult> SetPokemonOverride([FromBody] PokemonGameOverride overrideEntry, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(overrideEntry.GameId))
        {
            return BadRequest(new { error = "GameId is required." });
        }

        await pokemonGameDetector.SetOverrideAsync(overrideEntry.GameId, overrideEntry, cancellationToken).ConfigureAwait(false);
        return Ok(new { success = true });
    }

    [HttpDelete("Pokemon/Overrides/{gameId}")]
    public async Task<IActionResult> DeletePokemonOverride([FromRoute] string gameId, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(gameId))
        {
            return BadRequest(new { error = "GameId is required." });
        }

        await pokemonGameDetector.RemoveOverrideAsync(gameId, cancellationToken).ConfigureAwait(false);
        return NoContent();
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
        config.GamesEnabled,
        config.CustomGamesPath,
        config.GameLibraryIds,
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
        },
        pokemon = new
        {
            config.Pokemon.Enabled,
            config.Pokemon.ProviderType,
            config.Pokemon.PkVaultBaseUrl,
            config.Pokemon.TimeoutSeconds,
            config.Pokemon.AllowTransfers,
            config.Pokemon.AllowCrossGenerationTransfers,
            config.Pokemon.AllowTrading,
            config.Pokemon.AllowEditing,
            config.Pokemon.AutoBackups
        }
    };
}

public sealed record AdminConfigurationRequest(
    bool UserSettingsEnabled,
    bool GamesEnabled,
    string? CustomGamesPath,
    List<string>? GameLibraryIds,
    bool RequestsEnabled,
    bool WatchPartiesEnabled,
    bool PersonalPlaylistsEnabled,
    bool NotificationsEnabled,
    string? OmbiBaseUrl,
    string? OmbiApiKey,
    int OmbiTimeoutSeconds,
    bool OmbiRequireUserLogin,
    bool? PokemonEnabled = null,
    string? PokemonProviderType = null,
    string? PokemonPkVaultBaseUrl = null,
    int? PokemonTimeoutSeconds = null,
    bool? PokemonAllowTransfers = null,
    bool? PokemonAllowCrossGenerationTransfers = null,
    bool? PokemonAllowTrading = null,
    bool? PokemonAllowEditing = null,
    bool? PokemonAutoBackups = null);

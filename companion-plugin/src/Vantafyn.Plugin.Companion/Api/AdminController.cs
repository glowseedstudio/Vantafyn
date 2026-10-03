using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
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
    IPokemonGameDetector pokemonGameDetector,
    ICompanionPaths companionPaths) : ControllerBase
{
    private static readonly HttpClient NarrationTestClient = new();
    [HttpGet("Configuration")]
    public IActionResult GetConfiguration()
    {
        var plugin = Plugin.Instance;
        var config = plugin?.Configuration ?? new PluginConfiguration();
        if (plugin != null && config.Pokemon.MigrateLegacyNarrationDefault())
        {
            plugin.SaveConfiguration();
        }
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
        if (request.PokemonModalBackgroundPath != null)
        {
            config.Pokemon.ModalBackgroundPath = string.IsNullOrWhiteSpace(request.PokemonModalBackgroundPath) ? null : request.PokemonModalBackgroundPath.Trim();
        }
        if (request.PokemonNarrationEnabled.HasValue)
        {
            config.Pokemon.NarrationEnabled = request.PokemonNarrationEnabled.Value;
        }
        if (request.PokemonNarrationBaseUrl != null)
        {
            config.Pokemon.NarrationBaseUrl = string.IsNullOrWhiteSpace(request.PokemonNarrationBaseUrl) ? null : request.PokemonNarrationBaseUrl.Trim();
        }
        if (!string.IsNullOrWhiteSpace(request.PokemonNarrationVoice))
        {
            config.Pokemon.NarrationVoice = request.PokemonNarrationVoice.Trim();
        }
        if (request.PokemonNarrationSpeed.HasValue)
        {
            config.Pokemon.NarrationSpeed = Math.Clamp(request.PokemonNarrationSpeed.Value, 0.75m, 1.35m);
        }
        if (request.PokemonNarrationTimeoutSeconds.HasValue)
        {
            config.Pokemon.NarrationTimeoutSeconds = Math.Clamp(request.PokemonNarrationTimeoutSeconds.Value, 5, 60);
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
    public async Task<IActionResult> TestPokemonConnection(
        [FromBody] PokemonTestConnectionRequest? request = null,
        CancellationToken cancellationToken = default)
    {
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        var isEnabled = request?.Enabled ?? config.Pokemon.Enabled;
        if (!isEnabled)
        {
            return Ok(new PokemonConnectionTestResult
            {
                IsSuccess = false,
                Message = "Pokémon Vault integration is disabled. Please enable it before testing.",
                LatencyMs = 0
            });
        }

        var baseUrl = !string.IsNullOrWhiteSpace(request?.BaseUrl)
            ? request.BaseUrl
            : config.Pokemon.PkVaultBaseUrl;

        var timeout = request?.TimeoutSeconds ?? config.Pokemon.TimeoutSeconds;

        var testConfig = new PokemonConfiguration
        {
            Enabled = true,
            PkVaultBaseUrl = baseUrl,
            TimeoutSeconds = timeout,
            AllowTransfers = config.Pokemon.AllowTransfers,
            AllowCrossGenerationTransfers = config.Pokemon.AllowCrossGenerationTransfers,
            AutoBackups = config.Pokemon.AutoBackups
        };

        var provider = pokemonProviderFactory.Create(testConfig);
        var result = await provider.TestConnectionAsync(cancellationToken).ConfigureAwait(false);
        return Ok(result);
    }

    [HttpPost("Pokemon/Narration/TestConnection")]
    public async Task<IActionResult> TestPokemonNarrationConnection(
        [FromBody] PokemonNarrationTestConnectionRequest? request = null,
        CancellationToken cancellationToken = default)
    {
        var config = Plugin.Instance?.Configuration ?? new PluginConfiguration();
        var enabled = request?.Enabled ?? config.Pokemon.NarrationEnabled;
        var baseUrl = request?.BaseUrl ?? config.Pokemon.NarrationBaseUrl;
        var timeoutSeconds = Math.Clamp(request?.TimeoutSeconds ?? config.Pokemon.NarrationTimeoutSeconds, 5, 60);
        if (!enabled) return Ok(new { isSuccess = false, message = "Pokédex narration is disabled." });
        if (!Uri.TryCreate(baseUrl?.TrimEnd('/') + "/v1/audio/voices", UriKind.Absolute, out var endpoint) || endpoint.Scheme is not ("http" or "https"))
            return Ok(new { isSuccess = false, message = "Enter a valid local TTS service URL." });

        try
        {
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            timeout.CancelAfter(TimeSpan.FromSeconds(timeoutSeconds));
            using var response = await NarrationTestClient.GetAsync(endpoint, timeout.Token).ConfigureAwait(false);
            return Ok(new { isSuccess = response.IsSuccessStatusCode, message = response.IsSuccessStatusCode ? "Kokoro TTS service is reachable." : $"TTS service returned HTTP {(int)response.StatusCode}." });
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            return Ok(new { isSuccess = false, message = "TTS service timed out." });
        }
        catch
        {
            return Ok(new { isSuccess = false, message = "TTS service could not be reached from the Jellyfin server." });
        }
    }

    /// <summary>
    /// Removes only generated narration audio. Pokémon saves, vaults, metadata, cries and all
    /// other Companion data live in separate directories and are never included here.
    /// </summary>
    [HttpPost("Pokemon/Narration/Regenerate")]
    public IActionResult RegeneratePokemonNarrations()
    {
        var narrationDirectory = Path.Combine(companionPaths.PokemonRoot, "pokedex-narration");
        if (!Directory.Exists(narrationDirectory))
        {
            return Ok(new { success = true, deletedCount = 0, message = "There were no cached Pokédex narrations to remove." });
        }

        try
        {
            var deletedCount = Directory.EnumerateFiles(narrationDirectory, "*.mp3", SearchOption.AllDirectories).Count();
            Directory.Delete(narrationDirectory, recursive: true);
            return Ok(new { success = true, deletedCount, message = $"Removed {deletedCount} cached Pokédex narration file(s). They will regenerate using the current voice profile." });
        }
        catch (Exception ex)
        {
            return StatusCode(StatusCodes.Status500InternalServerError, new { success = false, message = $"Could not remove cached Pokédex narrations: {ex.Message}" });
        }
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
            config.Pokemon.AutoBackups,
            config.Pokemon.ModalBackgroundPath,
            config.Pokemon.NarrationEnabled,
            config.Pokemon.NarrationBaseUrl,
            config.Pokemon.NarrationVoice,
            config.Pokemon.NarrationSpeed,
            config.Pokemon.NarrationTimeoutSeconds
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
    bool? PokemonAutoBackups = null,
    string? PokemonModalBackgroundPath = null,
    bool? PokemonNarrationEnabled = null,
    string? PokemonNarrationBaseUrl = null,
    string? PokemonNarrationVoice = null,
    decimal? PokemonNarrationSpeed = null,
    int? PokemonNarrationTimeoutSeconds = null);

public sealed record PokemonTestConnectionRequest(
    string? BaseUrl = null,
    bool? Enabled = null,
    int? TimeoutSeconds = null);

public sealed record PokemonNarrationTestConnectionRequest(
    string? BaseUrl = null,
    bool? Enabled = null,
    int? TimeoutSeconds = null);

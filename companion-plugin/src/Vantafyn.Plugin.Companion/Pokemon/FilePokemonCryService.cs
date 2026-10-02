using System;
using System.Collections.Concurrent;
using System.Globalization;
using System.IO;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// File-cached implementation of <see cref="IPokemonCryService"/>.
/// Serves Pokémon cries from local disk, downloading on-demand from PokeAPI cries repository.
/// </summary>
public sealed class FilePokemonCryService : IPokemonCryService
{
    private const string DefaultUrlTemplate =
        "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/{style}/{speciesId}.ogg";

    private readonly ICompanionPaths _paths;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ILogger<FilePokemonCryService> _logger;
    private readonly ConcurrentDictionary<string, SemaphoreSlim> _downloadLocks = new(StringComparer.OrdinalIgnoreCase);

    public FilePokemonCryService(
        ICompanionPaths paths,
        IHttpClientFactory httpClientFactory,
        ILogger<FilePokemonCryService> logger)
    {
        _paths = paths;
        _httpClientFactory = httpClientFactory;
        _logger = logger;
    }

    public async Task<(Stream? Stream, string ContentType, bool Found)> GetCryStreamAsync(
        int speciesId,
        string? style = null,
        CancellationToken cancellationToken = default)
    {
        if (speciesId <= 0)
        {
            return (null, "audio/ogg", false);
        }

        var normalizedStyle = NormalizeStyle(style);
        var cryFilePath = GetCryFilePath(speciesId, normalizedStyle);

        // 1. Check existing disk cache
        if (File.Exists(cryFilePath))
        {
            try
            {
                var fileInfo = new FileInfo(cryFilePath);
                if (fileInfo.Length > 0)
                {
                    return (new FileStream(cryFilePath, FileMode.Open, FileAccess.Read, FileShare.Read), "audio/ogg", true);
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to read cached cry file {Path}. Retrying download.", cryFilePath);
            }
        }

        // 2. Synchronize download per species+style to prevent parallel duplicate fetches
        var lockKey = $"{normalizedStyle}:{speciesId}";
        var semaphore = _downloadLocks.GetOrAdd(lockKey, _ => new SemaphoreSlim(1, 1));

        await semaphore.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            // Re-check cache under lock
            if (File.Exists(cryFilePath))
            {
                var fileInfo = new FileInfo(cryFilePath);
                if (fileInfo.Length > 0)
                {
                    return (new FileStream(cryFilePath, FileMode.Open, FileAccess.Read, FileShare.Read), "audio/ogg", true);
                }
            }

            // Ensure cries directory exists
            var directory = Path.GetDirectoryName(cryFilePath);
            if (!string.IsNullOrEmpty(directory) && !Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }

            var template = Plugin.Instance?.Configuration?.Pokemon?.CrySourceUrlTemplate;
            if (string.IsNullOrWhiteSpace(template))
            {
                template = DefaultUrlTemplate;
            }

            var client = _httpClientFactory.CreateClient();
            var targetUrl = BuildUrl(template, normalizedStyle, speciesId);

            var downloaded = await TryDownloadAndCacheAsync(client, targetUrl, cryFilePath, cancellationToken).ConfigureAwait(false);

            // If legacy style was requested but not found (e.g. Gen 6+ Pokémon don't have legacy cries), fall back to latest
            if (!downloaded && string.Equals(normalizedStyle, "legacy", StringComparison.OrdinalIgnoreCase))
            {
                var latestFilePath = GetCryFilePath(speciesId, "latest");
                if (File.Exists(latestFilePath) && new FileInfo(latestFilePath).Length > 0)
                {
                    return (new FileStream(latestFilePath, FileMode.Open, FileAccess.Read, FileShare.Read), "audio/ogg", true);
                }

                var latestUrl = BuildUrl(template, "latest", speciesId);
                var latestDir = Path.GetDirectoryName(latestFilePath);
                if (!string.IsNullOrEmpty(latestDir) && !Directory.Exists(latestDir))
                {
                    Directory.CreateDirectory(latestDir);
                }

                downloaded = await TryDownloadAndCacheAsync(client, latestUrl, latestFilePath, cancellationToken).ConfigureAwait(false);
                if (downloaded)
                {
                    cryFilePath = latestFilePath;
                }
            }

            if (downloaded && File.Exists(cryFilePath))
            {
                return (new FileStream(cryFilePath, FileMode.Open, FileAccess.Read, FileShare.Read), "audio/ogg", true);
            }

            return (null, "audio/ogg", false);
        }
        finally
        {
            semaphore.Release();
        }
    }

    private string GetCryFilePath(int speciesId, string style)
    {
        return Path.Combine(_paths.PokemonRoot, "cries", style, $"{speciesId}.ogg");
    }

    private static string NormalizeStyle(string? style)
    {
        if (string.IsNullOrWhiteSpace(style))
        {
            return "latest";
        }

        var trimmed = style.Trim().ToLowerInvariant();
        return trimmed is "legacy" ? "legacy" : "latest";
    }

    private static string BuildUrl(string template, string style, int speciesId)
    {
        return template
            .Replace("{style}", style, StringComparison.OrdinalIgnoreCase)
            .Replace("{speciesId}", speciesId.ToString(CultureInfo.InvariantCulture), StringComparison.OrdinalIgnoreCase);
    }

    private async Task<bool> TryDownloadAndCacheAsync(
        HttpClient client,
        string url,
        string destinationPath,
        CancellationToken cancellationToken)
    {
        var tempFile = $"{destinationPath}.tmp.{Guid.NewGuid():N}";
        try
        {
            using var response = await client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, cancellationToken).ConfigureAwait(false);
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogDebug("Cry request to {Url} returned status {StatusCode}.", url, response.StatusCode);
                return false;
            }

            await using (var targetStream = File.Create(tempFile))
            {
                await response.Content.CopyToAsync(targetStream, cancellationToken).ConfigureAwait(false);
            }

            var tempInfo = new FileInfo(tempFile);
            if (tempInfo.Length == 0)
            {
                File.Delete(tempFile);
                return false;
            }

            File.Move(tempFile, destinationPath, overwrite: true);
            _logger.LogInformation("Successfully cached Pokémon cry from {Url} to {Path}", url, destinationPath);
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to download and cache Pokémon cry from {Url}", url);
            if (File.Exists(tempFile))
            {
                try { File.Delete(tempFile); } catch { /* ignore cleanup errors */ }
            }
            return false;
        }
    }
}

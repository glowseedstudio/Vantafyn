using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace Vantafyn.Plugin.Companion.Requests;

public sealed record OmbiConnectionResult(bool Success, string State, string Message, string? Version = null);
public sealed record OmbiSearchResult(string Provider, string Type, string Id, string Title, int? Year, string? PosterPath, bool Available, bool Requested);
public sealed record CreateRequestResult(bool Success, string State, string Message, string? RequestId = null);
public sealed record OmbiLinkedUserSession(
    string OmbiUserName,
    string? DisplayName,
    string? OmbiUserId,
    string? ExpiresAt,
    IReadOnlyList<string> Roles,
    DateTimeOffset LastLoginAt,
    DateTimeOffset LastValidatedAt,
    string AccessToken);
public sealed record OmbiUserLoginResult(bool Success, string State, string Message, OmbiLinkedUserSession? Session = null);

public interface IOmbiClient
{
    Task<OmbiConnectionResult> TestConnectionAsync(CancellationToken cancellationToken);
    Task<OmbiUserLoginResult> LoginUserAsync(string username, string password, CancellationToken cancellationToken);
    Task<OmbiLinkedUserSession> ValidateUserAsync(OmbiLinkedUserSession previous, CancellationToken cancellationToken);
    Task<IReadOnlyList<OmbiSearchResult>> SearchAsync(string query, string? type, string? bearerToken, CancellationToken cancellationToken);
    Task<CreateRequestResult> RequestMovieAsync(string providerId, string? bearerToken, CancellationToken cancellationToken);
    Task<CreateRequestResult> RequestSeriesAsync(string providerId, string? bearerToken, CancellationToken cancellationToken);
}

public interface IOmbiClientFactory
{
    IOmbiClient Create(OmbiConfiguration configuration);
}

public sealed class OmbiClientFactory(IHttpClientFactory httpClientFactory) : IOmbiClientFactory
{
    public IOmbiClient Create(OmbiConfiguration configuration) => new OmbiClient(httpClientFactory.CreateClient("Vantafyn.Companion.Ombi"), configuration);
}

internal sealed class OmbiClient(HttpClient httpClient, OmbiConfiguration configuration) : IOmbiClient
{
    private readonly Uri? _baseUri = TryBaseUri(configuration.BaseUrl);

    public async Task<OmbiConnectionResult> TestConnectionAsync(CancellationToken cancellationToken)
    {
        if (_baseUri == null || string.IsNullOrWhiteSpace(configuration.ApiKey))
        {
            return new OmbiConnectionResult(false, "unconfigured", "Ombi URL and API key are required.");
        }

        try
        {
            using var response = await SendAsync(HttpMethod.Get, "api/v1/Status", null, null, cancellationToken).ConfigureAwait(false);
            if ((int)response.StatusCode == 401 || (int)response.StatusCode == 403)
            {
                return new OmbiConnectionResult(false, "unauthorized", "Ombi responded, but the API key was rejected.");
            }

            if (!response.IsSuccessStatusCode)
            {
                return new OmbiConnectionResult(false, "degraded", $"Ombi returned HTTP {(int)response.StatusCode}.");
            }

            var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
            var version = TryReadString(body, "version") ?? TryReadString(body, "Version");
            return new OmbiConnectionResult(true, "ready", "Connected to Ombi.", version);
        }
        catch (OperationCanceledException)
        {
            throw;
        }
        catch
        {
            return new OmbiConnectionResult(false, "unavailable", "Ombi could not be reached.");
        }
    }

    public async Task<OmbiUserLoginResult> LoginUserAsync(string username, string password, CancellationToken cancellationToken)
    {
        if (_baseUri == null)
        {
            return new OmbiUserLoginResult(false, "unconfigured", "Ombi URL is required.");
        }
        if (string.IsNullOrWhiteSpace(username) || string.IsNullOrWhiteSpace(password))
        {
            return new OmbiUserLoginResult(false, "invalid", "Enter your Ombi username and password.");
        }

        try
        {
            var payload = JsonSerializer.Serialize(new
            {
                username = username.Trim(),
                password,
                rememberMe = true,
                usePlexAdminAccount = false,
                usePlexOAuth = false
            });
            using var response = await SendAsync(HttpMethod.Post, "api/v1/Token", payload, null, cancellationToken).ConfigureAwait(false);
            if ((int)response.StatusCode is 401 or 403)
            {
                return new OmbiUserLoginResult(false, "unauthorized", "Ombi rejected that username or password.");
            }
            if (!response.IsSuccessStatusCode)
            {
                return new OmbiUserLoginResult(false, "failed", $"Ombi returned HTTP {(int)response.StatusCode}.");
            }

            var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
            var token = TryReadString(body, "access_token") ?? TryReadString(body, "accessToken");
            if (string.IsNullOrWhiteSpace(token))
            {
                return new OmbiUserLoginResult(false, "failed", "Ombi did not return an access token.");
            }

            var expiresAt = TryReadString(body, "expiration") ?? TryReadString(body, "expires");
            var session = await ReadIdentityAsync(token, username.Trim(), expiresAt, DateTimeOffset.UtcNow, cancellationToken).ConfigureAwait(false);
            return new OmbiUserLoginResult(true, "linked", "Ombi account linked.", session);
        }
        catch (OperationCanceledException)
        {
            throw;
        }
        catch
        {
            return new OmbiUserLoginResult(false, "unavailable", "Ombi could not sign you in.");
        }
    }

    public async Task<OmbiLinkedUserSession> ValidateUserAsync(OmbiLinkedUserSession previous, CancellationToken cancellationToken) =>
        await ReadIdentityAsync(previous.AccessToken, previous.OmbiUserName, previous.ExpiresAt, previous.LastLoginAt, cancellationToken).ConfigureAwait(false);

    public async Task<IReadOnlyList<OmbiSearchResult>> SearchAsync(string query, string? type, string? bearerToken, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(query)) return Array.Empty<OmbiSearchResult>();
        var route = type?.Equals("series", StringComparison.OrdinalIgnoreCase) == true
            ? $"api/v1/Search/tv/{Uri.EscapeDataString(query)}"
            : $"api/v1/Search/movie/{Uri.EscapeDataString(query)}";
        using var response = await SendAsync(HttpMethod.Get, route, null, bearerToken, cancellationToken).ConfigureAwait(false);
        if (!response.IsSuccessStatusCode) return Array.Empty<OmbiSearchResult>();
        var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        return ParseSearch(body, type?.Equals("series", StringComparison.OrdinalIgnoreCase) == true ? "series" : "movie");
    }

    public async Task<CreateRequestResult> RequestMovieAsync(string providerId, string? bearerToken, CancellationToken cancellationToken)
    {
        var payload = JsonSerializer.Serialize(new { theMovieDbId = providerId });
        using var response = await SendAsync(HttpMethod.Post, "api/v1/Request/movie", payload, bearerToken, cancellationToken).ConfigureAwait(false);
        return response.IsSuccessStatusCode
            ? new CreateRequestResult(true, "created", "Movie request created.")
            : new CreateRequestResult(false, "failed", $"Ombi returned HTTP {(int)response.StatusCode}.");
    }

    public async Task<CreateRequestResult> RequestSeriesAsync(string providerId, string? bearerToken, CancellationToken cancellationToken)
    {
        var payload = JsonSerializer.Serialize(new { tvDbId = providerId });
        using var response = await SendAsync(HttpMethod.Post, "api/v1/Request/tv", payload, bearerToken, cancellationToken).ConfigureAwait(false);
        return response.IsSuccessStatusCode
            ? new CreateRequestResult(true, "created", "Series request created.")
            : new CreateRequestResult(false, "failed", $"Ombi returned HTTP {(int)response.StatusCode}.");
    }

    private async Task<OmbiLinkedUserSession> ReadIdentityAsync(
        string accessToken,
        string fallbackUserName,
        string? expiresAt,
        DateTimeOffset lastLoginAt,
        CancellationToken cancellationToken)
    {
        using var response = await SendAsync(HttpMethod.Get, "api/v1/Identity", null, accessToken, cancellationToken).ConfigureAwait(false);
        if (!response.IsSuccessStatusCode)
        {
            throw new UnauthorizedAccessException("Ombi user session is no longer valid.");
        }
        var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        return ParseUserSession(body, fallbackUserName, expiresAt, lastLoginAt, accessToken);
    }

    private async Task<HttpResponseMessage> SendAsync(HttpMethod method, string relativePath, string? json, string? bearerToken, CancellationToken cancellationToken)
    {
        if (_baseUri == null) throw new InvalidOperationException("Ombi is not configured.");
        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeout.CancelAfter(TimeSpan.FromSeconds(Math.Clamp(configuration.TimeoutSeconds, 2, 30)));
        var request = new HttpRequestMessage(method, new Uri(_baseUri, relativePath.TrimStart('/')));
        if (!string.IsNullOrWhiteSpace(bearerToken))
        {
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", bearerToken);
        }
        else if (!string.IsNullOrWhiteSpace(configuration.ApiKey))
        {
            request.Headers.Add("ApiKey", configuration.ApiKey);
        }
        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        if (json != null)
        {
            request.Content = new StringContent(json, Encoding.UTF8, "application/json");
        }
        return await httpClient.SendAsync(request, timeout.Token).ConfigureAwait(false);
    }

    private static Uri? TryBaseUri(string? value)
    {
        if (!Uri.TryCreate(value, UriKind.Absolute, out var uri)) return null;
        return uri.Scheme is "http" or "https" ? uri : null;
    }

    private static string? TryReadString(string json, string property)
    {
        try
        {
            using var document = JsonDocument.Parse(json);
            return document.RootElement.TryGetProperty(property, out var element) ? element.GetString() : null;
        }
        catch
        {
            return null;
        }
    }

    private static OmbiLinkedUserSession ParseUserSession(
        string json,
        string fallbackUserName,
        string? expiresAt,
        DateTimeOffset lastLoginAt,
        string accessToken)
    {
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        var username = ReadAnyString(root, "userName", "username", "emailAddress") ?? fallbackUserName;
        return new OmbiLinkedUserSession(
            username,
            ReadAnyString(root, "alias", "displayName") ?? username,
            ReadAnyString(root, "id", "userId"),
            expiresAt,
            ReadClaims(root),
            lastLoginAt,
            DateTimeOffset.UtcNow,
            accessToken);
    }

    private static IReadOnlyList<OmbiSearchResult> ParseSearch(string json, string type)
    {
        try
        {
            using var document = JsonDocument.Parse(json);
            var root = document.RootElement.ValueKind == JsonValueKind.Array
                ? document.RootElement
                : document.RootElement.TryGetProperty("results", out var results) ? results : default;
            if (root.ValueKind != JsonValueKind.Array) return Array.Empty<OmbiSearchResult>();
            return root.EnumerateArray().Select(item => new OmbiSearchResult(
                "ombi",
                type,
                ReadAnyString(item, "theMovieDbId", "tvDbId", "id", "Id") ?? string.Empty,
                ReadAnyString(item, "title", "name", "Title") ?? "Untitled",
                ReadAnyInt(item, "releaseDate", "firstAirDate", "year"),
                ReadAnyString(item, "posterPath", "poster", "PosterPath"),
                ReadAnyBool(item, "available", "isAvailable"),
                ReadAnyBool(item, "requested", "isRequested"))).Where(x => !string.IsNullOrWhiteSpace(x.Id)).ToList();
        }
        catch
        {
            return Array.Empty<OmbiSearchResult>();
        }
    }

    private static string? ReadAnyString(JsonElement item, params string[] names)
    {
        foreach (var name in names)
        {
            if (item.TryGetProperty(name, out var element))
            {
                if (element.ValueKind == JsonValueKind.String) return element.GetString();
                if (element.ValueKind == JsonValueKind.Number) return element.GetRawText();
            }
        }
        return null;
    }

    private static int? ReadAnyInt(JsonElement item, params string[] names)
    {
        var raw = ReadAnyString(item, names);
        if (raw?.Length >= 4 && int.TryParse(raw[..4], out var year)) return year;
        return int.TryParse(raw, out var value) ? value : null;
    }

    private static bool ReadAnyBool(JsonElement item, params string[] names)
    {
        foreach (var name in names)
        {
            if (item.TryGetProperty(name, out var element) && element.ValueKind is JsonValueKind.True or JsonValueKind.False)
            {
                return element.GetBoolean();
            }
        }
        return false;
    }

    private static IReadOnlyList<string> ReadClaims(JsonElement item)
    {
        if (!item.TryGetProperty("claims", out var claims) || claims.ValueKind != JsonValueKind.Array) return Array.Empty<string>();
        return claims.EnumerateArray()
            .Select(x => ReadAnyString(x, "value", "type", "claimValue", "claimType") ?? x.ToString())
            .Where(x => !string.IsNullOrWhiteSpace(x))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();
    }
}

using System.Diagnostics;
using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;

namespace Vantafyn.Plugin.Companion.Pokemon.PkVault;

/// <summary>
/// HTTP client adapter communicating with an external, independently deployed PKVault service.
/// </summary>
public sealed class PkVaultPokemonProvider : IPokemonProvider
{
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly PokemonConfiguration _config;
    private readonly ILogger<PkVaultPokemonProvider> _logger;

    public string ProviderName => "pkvault";

    public PkVaultPokemonProvider(
        IHttpClientFactory httpClientFactory,
        PokemonConfiguration config,
        ILogger<PkVaultPokemonProvider>? logger = null)
    {
        _httpClientFactory = httpClientFactory;
        _config = config;
        _logger = logger ?? NullLogger<PkVaultPokemonProvider>.Instance;
    }

    public async Task<PokemonConnectionTestResult> TestConnectionAsync(CancellationToken cancellationToken)
    {
        var validationError = ValidateBaseUrl(_config.PkVaultBaseUrl);
        if (validationError != null)
        {
            return new PokemonConnectionTestResult
            {
                IsSuccess = false,
                Message = validationError,
                LatencyMs = 0
            };
        }

        var uri = new Uri(_config.PkVaultBaseUrl!.Trim());
        var timeout = TimeSpan.FromSeconds(Math.Clamp(_config.TimeoutSeconds, 2, 30));
        using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        cts.CancelAfter(timeout);

        var stopwatch = Stopwatch.StartNew();
        try
        {
            var client = _httpClientFactory.CreateClient("Vantafyn.Pokemon.PkVault");
            client.BaseAddress = uri;
            client.Timeout = timeout;

            // Attempt probing /api/health or fallback to /health or root /
            HttpResponseMessage response;
            try
            {
                response = await client.GetAsync("/api/health", cts.Token).ConfigureAwait(false);
            }
            catch (HttpRequestException)
            {
                response = await client.GetAsync("/health", cts.Token).ConfigureAwait(false);
            }

            stopwatch.Stop();

            if (response.IsSuccessStatusCode)
            {
                string? version = null;
                try
                {
                    var healthJson = await response.Content.ReadFromJsonAsync<PkVaultHealthPayload>(cancellationToken: cts.Token).ConfigureAwait(false);
                    version = healthJson?.Version;
                }
                catch
                {
                    // Body might be plain text or custom JSON; success status code is sufficient
                }

                return new PokemonConnectionTestResult
                {
                    IsSuccess = true,
                    Message = $"Successfully connected to PKVault ({response.StatusCode}).",
                    LatencyMs = stopwatch.ElapsedMilliseconds,
                    ProviderVersion = version
                };
            }

            return new PokemonConnectionTestResult
            {
                IsSuccess = false,
                Message = $"PKVault responded with HTTP {(int)response.StatusCode} ({response.ReasonPhrase}).",
                LatencyMs = stopwatch.ElapsedMilliseconds
            };
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            stopwatch.Stop();
            _logger.LogWarning("Connection to PKVault at {BaseUrl} timed out after {TimeoutSeconds}s.", _config.PkVaultBaseUrl, _config.TimeoutSeconds);
            return new PokemonConnectionTestResult
            {
                IsSuccess = false,
                Message = $"Connection timed out after {_config.TimeoutSeconds}s.",
                LatencyMs = stopwatch.ElapsedMilliseconds
            };
        }
        catch (Exception ex)
        {
            stopwatch.Stop();
            _logger.LogError(ex, "Failed to connect to PKVault at {BaseUrl}.", _config.PkVaultBaseUrl);
            return new PokemonConnectionTestResult
            {
                IsSuccess = false,
                Message = $"Connection failed: {ex.Message}",
                LatencyMs = stopwatch.ElapsedMilliseconds
            };
        }
    }

    public async Task<PokemonProviderCapabilities> GetCapabilitiesAsync(CancellationToken cancellationToken)
    {
        var test = await TestConnectionAsync(cancellationToken).ConfigureAwait(false);
        if (!test.IsSuccess)
        {
            return new PokemonProviderCapabilities
            {
                CanReadSaves = false,
                CanWriteSaves = false,
                CanTransferSameGeneration = false,
                CanTransferCrossGeneration = false,
                CanValidateLegality = false
            };
        }

        // Return PKVault supported capabilities
        return new PokemonProviderCapabilities
        {
            CanReadSaves = true,
            CanWriteSaves = _config.AllowTransfers,
            CanTransferSameGeneration = _config.AllowTransfers,
            CanTransferCrossGeneration = _config.AllowTransfers && _config.AllowCrossGenerationTransfers,
            CanValidateLegality = true,
            SupportedGenerations = new[] { "1", "2", "3", "4", "5", "6", "7", "8", "9" },
            SupportedPlatforms = new[] { "gb", "gbc", "gba", "nds", "3ds", "switch" }
        };
    }

    public static string? ValidateBaseUrl(string? rawUrl)
    {
        if (string.IsNullOrWhiteSpace(rawUrl))
        {
            return "PKVault base URL is not configured.";
        }

        if (!Uri.TryCreate(rawUrl.Trim(), UriKind.Absolute, out var uri))
        {
            return "PKVault base URL is not a valid absolute URL.";
        }

        if (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps)
        {
            return "PKVault base URL must use http or https scheme.";
        }

        return null;
    }

    public async Task<PokemonSaveParseResult> ParseSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        CancellationToken cancellationToken)
    {
        var validationError = ValidateBaseUrl(_config.PkVaultBaseUrl);
        if (validationError != null)
        {
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = validationError
            };
        }

        if (saveBytes == null || saveBytes.Length == 0)
        {
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "Save data is empty or missing."
            };
        }

        var uri = new Uri(_config.PkVaultBaseUrl!.Trim());
        var timeout = TimeSpan.FromSeconds(Math.Clamp(_config.TimeoutSeconds, 2, 30));
        using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        cts.CancelAfter(timeout);

        try
        {
            var client = _httpClientFactory.CreateClient("Vantafyn.Pokemon.PkVault");
            client.BaseAddress = uri;
            client.Timeout = timeout;

            using var formData = new MultipartFormDataContent();
            var fileContent = new ByteArrayContent(saveBytes);
            formData.Add(fileContent, "file", $"{pokemonGameId}.sav");
            formData.Add(new StringContent(pokemonGameId), "gameId");
            formData.Add(new StringContent(platform), "platform");
            formData.Add(new StringContent(generation.ToString()), "generation");

            var response = await client.PostAsync("/api/save/parse", formData, cts.Token).ConfigureAwait(false);
            if (!response.IsSuccessStatusCode)
            {
                var errorBody = await response.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault returned {StatusCode} parsing save for {GameId}: {Error}", response.StatusCode, pokemonGameId, errorBody);
                return new PokemonSaveParseResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault rejected save data (HTTP {(int)response.StatusCode})."
                };
            }

            var parseResponse = await response.Content.ReadFromJsonAsync<PkVaultSaveParseResponse>(cancellationToken: cts.Token).ConfigureAwait(false);
            if (parseResponse == null)
            {
                return new PokemonSaveParseResult
                {
                    IsSuccess = false,
                    ErrorMessage = "PKVault returned an empty response."
                };
            }

            var result = new PokemonSaveParseResult
            {
                IsSuccess = true,
                TrainerName = parseResponse.TrainerName,
                TrainerId = parseResponse.TrainerId,
                Money = parseResponse.Money,
                PokedexSeen = parseResponse.PokedexSeen,
                PokedexCaught = parseResponse.PokedexCaught
            };

            // Map Party
            if (parseResponse.Party != null)
            {
                for (var i = 0; i < parseResponse.Party.Count; i++)
                {
                    var p = parseResponse.Party[i];
                    p.IsInParty = true;
                    p.SlotIndex = i + 1;
                    var summary = MapToSummary(p, pokemonGameId, $"party_{i + 1}");
                    result.Party.Add(summary);
                    result.Details[summary.Id] = MapToDetails(p, summary);
                }
            }

            // Map Boxes
            if (parseResponse.Boxes != null)
            {
                foreach (var b in parseResponse.Boxes)
                {
                    var boxEntries = new List<PokemonSummaryDto>();
                    if (b.Pokemon != null)
                    {
                        for (var i = 0; i < b.Pokemon.Count; i++)
                        {
                            var p = b.Pokemon[i];
                            p.IsInParty = false;
                            p.BoxIndex = b.BoxIndex;
                            if (p.SlotIndex <= 0) p.SlotIndex = i + 1;
                            var summary = MapToSummary(p, pokemonGameId, $"box_{b.BoxIndex}_{p.SlotIndex}");
                            boxEntries.Add(summary);
                            result.Details[summary.Id] = MapToDetails(p, summary);
                        }
                    }

                    result.Boxes.Add(new PokemonBoxDto
                    {
                        BoxIndex = b.BoxIndex,
                        Name = string.IsNullOrWhiteSpace(b.Name) ? $"Box {b.BoxIndex}" : b.Name,
                        Capacity = b.Capacity > 0 ? b.Capacity : 30,
                        OccupiedCount = boxEntries.Count,
                        Entries = boxEntries
                    });
                }
            }

            return result;
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            _logger.LogWarning("Save parsing request to PKVault timed out for {GameId}.", pokemonGameId);
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "PKVault parsing request timed out."
            };
        }
        catch (HttpRequestException ex)
        {
            _logger.LogWarning(ex, "Failed to connect to PKVault for save parsing of {GameId}.", pokemonGameId);
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "PKVault service is unreachable."
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unexpected error parsing save for {GameId}.", pokemonGameId);
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = $"Save parsing failed: {ex.Message}"
            };
        }
    }

    private static PokemonSummaryDto MapToSummary(PkVaultPokemonDto p, string gameId, string fallbackId)
    {
        return new PokemonSummaryDto
        {
            Id = string.IsNullOrWhiteSpace(p.Id) ? fallbackId : p.Id,
            Species = p.Species,
            SpeciesId = p.SpeciesId,
            Form = p.Form,
            Nickname = string.IsNullOrWhiteSpace(p.Nickname) ? p.Species : p.Nickname,
            Level = Math.Clamp(p.Level, 1, 100),
            Gender = p.Gender,
            IsShiny = p.IsShiny,
            OriginalTrainer = p.OriginalTrainer,
            OriginalTrainerId = p.OriginalTrainerId,
            OriginGame = p.OriginGame,
            CurrentGame = gameId,
            CurrentLocation = p.IsInParty
                ? $"Party (Slot {p.SlotIndex})"
                : $"Box {p.BoxIndex ?? 1} (Slot {p.SlotIndex})",
            BoxIndex = p.BoxIndex,
            SlotIndex = p.SlotIndex,
            IsInParty = p.IsInParty,
            LegalityStatus = string.IsNullOrWhiteSpace(p.LegalityStatus) ? "valid" : p.LegalityStatus
        };
    }

    private static PokemonDetailsDto MapToDetails(PkVaultPokemonDto p, PokemonSummaryDto summary)
    {
        return new PokemonDetailsDto
        {
            Summary = summary,
            Nature = p.Nature,
            Ability = p.Ability,
            HeldItem = p.HeldItem,
            Moves = p.Moves ?? [],
            Iv = p.Iv,
            Ev = p.Ev,
            CurrentHp = p.CurrentHp,
            MaxHp = p.MaxHp,
            Friendship = p.Friendship,
            Pokeball = p.Pokeball,
            RawData = p.RawData
        };
    }

    public async Task<PokemonExtractResult> ExtractPokemonFromSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        string pokemonId,
        bool isInParty,
        int? boxIndex,
        int slotIndex,
        CancellationToken cancellationToken)
    {
        var validationError = ValidateBaseUrl(_config.PkVaultBaseUrl);
        if (validationError != null)
        {
            return new PokemonExtractResult { IsSuccess = false, ErrorMessage = validationError };
        }

        if (saveBytes == null || saveBytes.Length == 0)
        {
            return new PokemonExtractResult { IsSuccess = false, ErrorMessage = "Save data is empty." };
        }

        var uri = new Uri(_config.PkVaultBaseUrl!.Trim());
        var timeout = TimeSpan.FromSeconds(Math.Clamp(_config.TimeoutSeconds, 2, 30));
        using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        cts.CancelAfter(timeout);

        try
        {
            var client = _httpClientFactory.CreateClient("Vantafyn.Pokemon.PkVault");
            client.BaseAddress = uri;
            client.Timeout = timeout;

            using var formData = new MultipartFormDataContent();
            formData.Add(new ByteArrayContent(saveBytes), "file", $"{pokemonGameId}.sav");
            formData.Add(new StringContent(pokemonGameId), "gameId");
            formData.Add(new StringContent(platform), "platform");
            formData.Add(new StringContent(generation.ToString()), "generation");
            formData.Add(new StringContent(pokemonId), "pokemonId");
            formData.Add(new StringContent(isInParty.ToString()), "isInParty");
            if (boxIndex.HasValue) formData.Add(new StringContent(boxIndex.Value.ToString()), "boxIndex");
            formData.Add(new StringContent(slotIndex.ToString()), "slotIndex");

            var response = await client.PostAsync("/api/save/extract", formData, cts.Token).ConfigureAwait(false);
            if (!response.IsSuccessStatusCode)
            {
                var errorBody = await response.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault returned {StatusCode} extracting Pokemon: {Error}", response.StatusCode, errorBody);
                return new PokemonExtractResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault could not extract Pokémon (HTTP {(int)response.StatusCode})."
                };
            }

            var extractResponse = await response.Content.ReadFromJsonAsync<PkVaultExtractResponse>(cancellationToken: cts.Token).ConfigureAwait(false);
            if (extractResponse == null || extractResponse.Pokemon == null)
            {
                return new PokemonExtractResult { IsSuccess = false, ErrorMessage = "PKVault returned an empty extraction response." };
            }

            byte[]? updatedBytes = null;
            if (!string.IsNullOrWhiteSpace(extractResponse.UpdatedSaveBase64))
            {
                updatedBytes = Convert.FromBase64String(extractResponse.UpdatedSaveBase64);
            }

            var summary = MapToSummary(extractResponse.Pokemon, pokemonGameId, pokemonId);
            var details = MapToDetails(extractResponse.Pokemon, summary);

            return new PokemonExtractResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = updatedBytes,
                ExtractedPokemon = details
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to extract Pokémon {PokemonId} from {GameId}", pokemonId, pokemonGameId);
            return new PokemonExtractResult { IsSuccess = false, ErrorMessage = $"Extraction failed: {ex.Message}" };
        }
    }

    public async Task<PokemonInjectResult> InjectPokemonIntoSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        PokemonVaultEntry entry,
        int? targetBoxIndex,
        int? targetSlotIndex,
        bool targetParty,
        CancellationToken cancellationToken)
    {
        var validationError = ValidateBaseUrl(_config.PkVaultBaseUrl);
        if (validationError != null)
        {
            return new PokemonInjectResult { IsSuccess = false, ErrorMessage = validationError };
        }

        if (saveBytes == null || saveBytes.Length == 0)
        {
            return new PokemonInjectResult { IsSuccess = false, ErrorMessage = "Save data is empty." };
        }

        var uri = new Uri(_config.PkVaultBaseUrl!.Trim());
        var timeout = TimeSpan.FromSeconds(Math.Clamp(_config.TimeoutSeconds, 2, 30));
        using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        cts.CancelAfter(timeout);

        try
        {
            var client = _httpClientFactory.CreateClient("Vantafyn.Pokemon.PkVault");
            client.BaseAddress = uri;
            client.Timeout = timeout;

            using var formData = new MultipartFormDataContent();
            formData.Add(new ByteArrayContent(saveBytes), "file", $"{pokemonGameId}.sav");
            formData.Add(new StringContent(pokemonGameId), "gameId");
            formData.Add(new StringContent(platform), "platform");
            formData.Add(new StringContent(generation.ToString()), "generation");
            if (targetBoxIndex.HasValue) formData.Add(new StringContent(targetBoxIndex.Value.ToString()), "targetBoxIndex");
            if (targetSlotIndex.HasValue) formData.Add(new StringContent(targetSlotIndex.Value.ToString()), "targetSlotIndex");
            formData.Add(new StringContent(targetParty.ToString()), "targetParty");
            formData.Add(new StringContent(System.Text.Json.JsonSerializer.Serialize(entry)), "pokemonJson");

            var response = await client.PostAsync("/api/save/inject", formData, cts.Token).ConfigureAwait(false);
            if (!response.IsSuccessStatusCode)
            {
                var errorBody = await response.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault returned {StatusCode} injecting Pokemon: {Error}", response.StatusCode, errorBody);
                return new PokemonInjectResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault could not inject Pokémon (HTTP {(int)response.StatusCode})."
                };
            }

            var injectResponse = await response.Content.ReadFromJsonAsync<PkVaultInjectResponse>(cancellationToken: cts.Token).ConfigureAwait(false);
            if (injectResponse == null)
            {
                return new PokemonInjectResult { IsSuccess = false, ErrorMessage = "PKVault returned an empty injection response." };
            }

            byte[]? updatedBytes = null;
            if (!string.IsNullOrWhiteSpace(injectResponse.UpdatedSaveBase64))
            {
                updatedBytes = Convert.FromBase64String(injectResponse.UpdatedSaveBase64);
            }

            return new PokemonInjectResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = updatedBytes,
                AssignedLocation = injectResponse.AssignedLocation
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to inject Pokémon {Species} into {GameId}", entry.Species, pokemonGameId);
            return new PokemonInjectResult { IsSuccess = false, ErrorMessage = $"Injection failed: {ex.Message}" };
        }
    }

    private sealed record PkVaultHealthPayload(
        [property: JsonPropertyName("version")] string? Version,
        [property: JsonPropertyName("status")] string? Status);

    private sealed class PkVaultSaveParseResponse
    {
        [JsonPropertyName("trainerName")]
        public string? TrainerName { get; set; }

        [JsonPropertyName("trainerId")]
        public string? TrainerId { get; set; }

        [JsonPropertyName("money")]
        public int? Money { get; set; }

        [JsonPropertyName("pokedexSeen")]
        public int? PokedexSeen { get; set; }

        [JsonPropertyName("pokedexCaught")]
        public int? PokedexCaught { get; set; }

        [JsonPropertyName("party")]
        public List<PkVaultPokemonDto>? Party { get; set; }

        [JsonPropertyName("boxes")]
        public List<PkVaultBoxDto>? Boxes { get; set; }
    }

    private sealed class PkVaultBoxDto
    {
        [JsonPropertyName("boxIndex")]
        public int BoxIndex { get; set; } = 1;

        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;

        [JsonPropertyName("capacity")]
        public int Capacity { get; set; } = 30;

        [JsonPropertyName("pokemon")]
        public List<PkVaultPokemonDto>? Pokemon { get; set; }
    }

    private sealed class PkVaultPokemonDto
    {
        [JsonPropertyName("id")]
        public string? Id { get; set; }

        [JsonPropertyName("species")]
        public string Species { get; set; } = string.Empty;

        [JsonPropertyName("speciesId")]
        public int SpeciesId { get; set; }

        [JsonPropertyName("form")]
        public string? Form { get; set; }

        [JsonPropertyName("nickname")]
        public string Nickname { get; set; } = string.Empty;

        [JsonPropertyName("level")]
        public int Level { get; set; } = 1;

        [JsonPropertyName("gender")]
        public string? Gender { get; set; }

        [JsonPropertyName("isShiny")]
        public bool IsShiny { get; set; }

        [JsonPropertyName("nature")]
        public string? Nature { get; set; }

        [JsonPropertyName("ability")]
        public string? Ability { get; set; }

        [JsonPropertyName("heldItem")]
        public string? HeldItem { get; set; }

        [JsonPropertyName("originalTrainer")]
        public string? OriginalTrainer { get; set; }

        [JsonPropertyName("originalTrainerId")]
        public string? OriginalTrainerId { get; set; }

        [JsonPropertyName("originGame")]
        public string? OriginGame { get; set; }

        [JsonPropertyName("slotIndex")]
        public int SlotIndex { get; set; } = 1;

        [JsonPropertyName("boxIndex")]
        public int? BoxIndex { get; set; }

        [JsonPropertyName("isInParty")]
        public bool IsInParty { get; set; }

        [JsonPropertyName("legalityStatus")]
        public string LegalityStatus { get; set; } = "valid";

        [JsonPropertyName("moves")]
        public List<string>? Moves { get; set; }

        [JsonPropertyName("iv")]
        public PokemonStatsDto? Iv { get; set; }

        [JsonPropertyName("ev")]
        public PokemonStatsDto? Ev { get; set; }

        [JsonPropertyName("currentHp")]
        public int? CurrentHp { get; set; }

        [JsonPropertyName("maxHp")]
        public int? MaxHp { get; set; }

        [JsonPropertyName("friendship")]
        public int? Friendship { get; set; }

        [JsonPropertyName("pokeball")]
        public string? Pokeball { get; set; }

        [JsonPropertyName("rawData")]
        public string? RawData { get; set; }
    }

    private sealed class PkVaultExtractResponse
    {
        [JsonPropertyName("pokemon")]
        public PkVaultPokemonDto? Pokemon { get; set; }

        [JsonPropertyName("updatedSaveBase64")]
        public string? UpdatedSaveBase64 { get; set; }
    }

    private sealed class PkVaultInjectResponse
    {
        [JsonPropertyName("updatedSaveBase64")]
        public string? UpdatedSaveBase64 { get; set; }

        [JsonPropertyName("assignedLocation")]
        public string? AssignedLocation { get; set; }
    }
}

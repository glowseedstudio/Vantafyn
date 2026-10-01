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
    private readonly PkVaultStaticCatalog _catalog = new();
    private volatile bool _staticDataLoaded;
    private readonly SemaphoreSlim _staticDataLock = new(1, 1);

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
        if (!_config.Enabled)
        {
            return new PokemonConnectionTestResult
            {
                IsSuccess = false,
                Message = "Pokémon Vault integration is disabled in configuration.",
                LatencyMs = 0
            };
        }

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

        var baseAddress = _config.PkVaultBaseUrl!.Trim().TrimEnd('/') + "/";
        var baseUri = new Uri(baseAddress);
        var requestUri = new Uri(baseUri, "api/settings");
        var timeoutSeconds = Math.Clamp(_config.TimeoutSeconds > 0 ? _config.TimeoutSeconds : 5, 2, 15);
        var timeout = TimeSpan.FromSeconds(timeoutSeconds);

        using var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        cts.CancelAfter(timeout);

        var stopwatch = Stopwatch.StartNew();
        try
        {
            var client = _httpClientFactory.CreateClient("Vantafyn.Pokemon.PkVault");
            client.BaseAddress = baseUri;
            client.Timeout = timeout;

            HttpResponseMessage response;
            try
            {
                response = await client.GetAsync(requestUri, cts.Token).ConfigureAwait(false);
            }
            catch (HttpRequestException ex)
            {
                stopwatch.Stop();
                _logger.LogWarning("Failed to connect to PKVault at {RequestUri}: {Message}", requestUri, ex.Message);
                return new PokemonConnectionTestResult
                {
                    IsSuccess = false,
                    Message = $"Could not connect to PKVault at {baseAddress.TrimEnd('/')}: {ex.Message}",
                    LatencyMs = stopwatch.ElapsedMilliseconds
                };
            }

            stopwatch.Stop();

            if (response.IsSuccessStatusCode)
            {
                PkVaultSettingsResponse? settings = null;
                try
                {
                    settings = await response.Content.ReadFromJsonAsync<PkVaultSettingsResponse>(cancellationToken: cts.Token).ConfigureAwait(false);
                }
                catch (System.Text.Json.JsonException jsonEx)
                {
                    _logger.LogWarning(
                        "PKVault at {RequestUri} returned HTTP {StatusCode}, but response body was not valid settings JSON: {Error}",
                        requestUri, (int)response.StatusCode, jsonEx.Message);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(
                        "Error reading settings response from PKVault at {RequestUri}: {Error}",
                        requestUri, ex.Message);
                }

                var version = settings?.Version;
                var pkhexVersion = settings?.PkhexVersion;
                var canUploadSaves = settings?.CanUploadSaves;
                var canCreateBackup = settings?.CanCreateBackup;

                _logger.LogInformation(
                    "Successfully connected to PKVault at {RequestUri} (HTTP {StatusCode}, version: {Version}, pkhexVersion: {PkhexVersion}) in {LatencyMs}ms.",
                    requestUri, (int)response.StatusCode, version ?? "unknown", pkhexVersion ?? "unknown", stopwatch.ElapsedMilliseconds);

                var msg = !string.IsNullOrWhiteSpace(version)
                    ? (!string.IsNullOrWhiteSpace(pkhexVersion)
                        ? $"Connected to PKVault v{version} (PKHeX {pkhexVersion})"
                        : $"Connected to PKVault v{version}")
                    : "Connected to PKVault";

                return new PokemonConnectionTestResult
                {
                    IsSuccess = true,
                    Message = msg,
                    LatencyMs = stopwatch.ElapsedMilliseconds,
                    ProviderVersion = version,
                    PkhexVersion = pkhexVersion,
                    CanUploadSaves = canUploadSaves,
                    CanCreateBackup = canCreateBackup
                };
            }

            _logger.LogWarning(
                "PKVault connection check to {RequestUri} failed with HTTP {StatusCode} ({ReasonPhrase}).",
                requestUri, (int)response.StatusCode, response.ReasonPhrase);

            return new PokemonConnectionTestResult
            {
                IsSuccess = false,
                Message = $"PKVault returned HTTP {(int)response.StatusCode} ({response.ReasonPhrase}) for {requestUri.AbsolutePath}.",
                LatencyMs = stopwatch.ElapsedMilliseconds
            };
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            stopwatch.Stop();
            _logger.LogWarning("Connection to PKVault at {RequestUri} timed out after {TimeoutSeconds}s.", requestUri, timeoutSeconds);
            return new PokemonConnectionTestResult
            {
                IsSuccess = false,
                Message = $"Connection to PKVault timed out after {timeoutSeconds}s.",
                LatencyMs = stopwatch.ElapsedMilliseconds
            };
        }
        catch (Exception ex)
        {
            stopwatch.Stop();
            _logger.LogWarning(ex, "Unexpected error connecting to PKVault at {RequestUri}.", requestUri);
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

        var client = _httpClientFactory.CreateClient("Vantafyn.Pokemon.PkVault");
        client.BaseAddress = uri;
        client.Timeout = timeout;

        // Best effort: warm up static data catalog if reachable
        _ = EnsureStaticDataLoadedAsync(client, cts.Token);

        string? stagedPath = null;
        try
        {
            // 1. Upload ephemeral save via POST /api/save-infos?saveFilesNames={name}&overwrite=true
            var ephemeralFileName = $"vantafyn_{Guid.NewGuid():N}_{pokemonGameId}.sav";
            using var formData = new MultipartFormDataContent();
            var fileContent = new ByteArrayContent(saveBytes);
            fileContent.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream");
            formData.Add(fileContent, "saveFiles", ephemeralFileName);

            var uploadUri = $"/api/save-infos?saveFilesNames={Uri.EscapeDataString(ephemeralFileName)}&overwrite=true";
            var uploadResponse = await client.PostAsync(uploadUri, formData, cts.Token).ConfigureAwait(false);
            if (!uploadResponse.IsSuccessStatusCode)
            {
                var errorBody = await uploadResponse.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault returned {StatusCode} uploading save for {GameId}: {Error}", uploadResponse.StatusCode, pokemonGameId, errorBody);
                return new PokemonSaveParseResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault rejected save data (HTTP {(int)uploadResponse.StatusCode})."
                };
            }

            var dataDto = await uploadResponse.Content.ReadFromJsonAsync<PkVaultDataDto>(cancellationToken: cts.Token).ConfigureAwait(false);
            var saveInfo = dataDto?.SaveInfos?.Values.FirstOrDefault();
            if (saveInfo == null)
            {
                return new PokemonSaveParseResult
                {
                    IsSuccess = false,
                    ErrorMessage = "PKVault could not identify save information."
                };
            }

            var saveId = saveInfo.Id;
            stagedPath = saveInfo.Path;

            // 2. Fetch PC box layouts via GET /api/storage/box?saveId={saveId}
            var boxResponse = await client.GetAsync($"/api/storage/box?saveId={saveId}", cts.Token).ConfigureAwait(false);
            List<PkVaultBoxItemDto>? boxesList = null;
            if (boxResponse.IsSuccessStatusCode)
            {
                boxesList = await boxResponse.Content.ReadFromJsonAsync<List<PkVaultBoxItemDto>>(cancellationToken: cts.Token).ConfigureAwait(false);
            }

            var pcBoxes = boxesList?.Where(b => b.Type == 0).OrderBy(b => b.Order).ToList();
            if (pcBoxes == null || pcBoxes.Count == 0)
            {
                pcBoxes = boxesList?.OrderBy(b => b.Order).ToList() ?? [];
            }

            // 3. Fetch all Pokémon in save via GET /api/storage/save/{saveId}/pkm
            var pkmResponse = await client.GetAsync($"/api/storage/save/{saveId}/pkm", cts.Token).ConfigureAwait(false);
            if (!pkmResponse.IsSuccessStatusCode)
            {
                var errorBody = await pkmResponse.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault returned {StatusCode} fetching Pokémon for save {SaveId}: {Error}", pkmResponse.StatusCode, saveId, errorBody);
                return new PokemonSaveParseResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault failed fetching Pokémon (HTTP {(int)pkmResponse.StatusCode})."
                };
            }

            var pkmList = await pkmResponse.Content.ReadFromJsonAsync<List<PkVaultPkmSaveItemDto>>(cancellationToken: cts.Token).ConfigureAwait(false) ?? [];

            // Extract PKVault PKHeX legality state if present
            Dictionary<string, PkVaultPkmLegalityDto>? legalityMap = null;
            var activeSaveData = dataDto?.Saves?.FirstOrDefault(s => s.SaveId == saveId) ?? dataDto?.Saves?.FirstOrDefault();
            if (activeSaveData?.SavePkmLegality?.Data != null && activeSaveData.SavePkmLegality.Data.Count > 0)
            {
                legalityMap = activeSaveData.SavePkmLegality.Data;
            }

            if ((legalityMap == null || legalityMap.Count == 0) && pkmList.Count > 0)
            {
                try
                {
                    var pkmQuery = string.Join("&", pkmList.Take(60).Select(p => $"pkmIds={Uri.EscapeDataString(p.Id)}"));
                    var legResp = await client.GetAsync($"/api/storage/pkm/legality?{pkmQuery}&saveId={saveId}", cts.Token).ConfigureAwait(false);
                    if (legResp.IsSuccessStatusCode)
                    {
                        legalityMap = await legResp.Content.ReadFromJsonAsync<Dictionary<string, PkVaultPkmLegalityDto>>(cancellationToken: cts.Token).ConfigureAwait(false);
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogDebug(ex, "Could not fetch on-demand legality from PKVault");
                }
            }

            PkVaultPkmLegalityDto? FindLegality(PkVaultPkmSaveItemDto p)
            {
                if (legalityMap == null) return null;
                if (legalityMap.TryGetValue(p.Id, out var l)) return l;
                if (!string.IsNullOrEmpty(p.IdBase) && legalityMap.TryGetValue(p.IdBase, out var lb)) return lb;
                return null;
            }

            var result = new PokemonSaveParseResult
            {
                IsSuccess = true,
                TrainerName = saveInfo.TrainerName,
                TrainerId = saveInfo.Tid > 0 ? saveInfo.Tid.ToString() : null,
                PokedexSeen = saveInfo.DexSeenCount,
                PokedexCaught = saveInfo.DexCaughtCount
            };

            // 4. Map Party Pokémon (Party >= 0)
            var partyPkms = pkmList.Where(p => p.Party >= 0).OrderBy(p => p.Party).ToList();
            for (var i = 0; i < partyPkms.Count; i++)
            {
                var p = partyPkms[i];
                var slotIndex = p.Party >= 0 ? p.Party + 1 : i + 1;
                var leg = FindLegality(p);
                var summary = MapToSummary(p, pokemonGameId, isInParty: true, boxIndex: null, slotIndex: slotIndex, legality: leg);
                result.Party.Add(summary);
                result.Details[summary.Id] = MapToDetails(p, summary, legality: leg);
            }

            // 5. Map Box Pokémon (Party < 0)
            var boxPkms = pkmList.Where(p => p.Party < 0).ToList();
            var boxGroups = boxPkms.GroupBy(p => p.BoxId).ToDictionary(g => g.Key, g => g.OrderBy(p => p.BoxSlot).ToList());

            if (pcBoxes.Count > 0)
            {
                for (var bIdx = 0; bIdx < pcBoxes.Count; bIdx++)
                {
                    var boxMeta = pcBoxes[bIdx];
                    var boxNumber = bIdx + 1;
                    boxGroups.TryGetValue(boxMeta.IdInt, out var monsInBox);
                    monsInBox ??= (boxGroups.TryGetValue(bIdx, out var m2) ? m2 : []);

                    var entries = new List<PokemonSummaryDto>();
                    if (monsInBox != null)
                    {
                        for (var i = 0; i < monsInBox.Count; i++)
                        {
                            var p = monsInBox[i];
                            var slotIndex = p.BoxSlot >= 0 ? p.BoxSlot + 1 : i + 1;
                            var leg = FindLegality(p);
                            var summary = MapToSummary(p, pokemonGameId, isInParty: false, boxIndex: boxNumber, slotIndex: slotIndex, legality: leg);
                            entries.Add(summary);
                            result.Details[summary.Id] = MapToDetails(p, summary, legality: leg);
                        }
                    }

                    result.Boxes.Add(new PokemonBoxDto
                    {
                        BoxIndex = boxNumber,
                        Name = string.IsNullOrWhiteSpace(boxMeta.Name) ? $"Box {boxNumber}" : boxMeta.Name,
                        Capacity = boxMeta.SlotCount > 0 ? boxMeta.SlotCount : 30,
                        OccupiedCount = entries.Count,
                        Entries = entries
                    });
                }
            }
            else
            {
                var boxKeys = boxGroups.Keys.OrderBy(k => k).ToList();
                if (boxKeys.Count == 0) boxKeys.Add(0);

                foreach (var bKey in boxKeys)
                {
                    var boxNumber = bKey + 1;
                    var monsInBox = boxGroups.TryGetValue(bKey, out var m) ? m : [];
                    var entries = new List<PokemonSummaryDto>();
                    for (var i = 0; i < monsInBox.Count; i++)
                    {
                        var p = monsInBox[i];
                        var slotIndex = p.BoxSlot >= 0 ? p.BoxSlot + 1 : i + 1;
                        var summary = MapToSummary(p, pokemonGameId, isInParty: false, boxIndex: boxNumber, slotIndex: slotIndex);
                        entries.Add(summary);
                        result.Details[summary.Id] = MapToDetails(p, summary);
                    }

                    result.Boxes.Add(new PokemonBoxDto
                    {
                        BoxIndex = boxNumber,
                        Name = $"Box {boxNumber}",
                        Capacity = 30,
                        OccupiedCount = entries.Count,
                        Entries = entries
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
        finally
        {
            if (!string.IsNullOrWhiteSpace(stagedPath))
            {
                try
                {
                    await client.DeleteAsync($"/api/save-infos?path={Uri.EscapeDataString(stagedPath)}", CancellationToken.None).ConfigureAwait(false);
                }
                catch (Exception cleanupEx)
                {
                    _logger.LogDebug(cleanupEx, "Failed to clean up staged save at {StagedPath} on PKVault", stagedPath);
                }
            }
        }
    }

    private async Task EnsureStaticDataLoadedAsync(HttpClient client, CancellationToken ct)
    {
        if (_staticDataLoaded) return;
        await _staticDataLock.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            if (_staticDataLoaded) return;
            var response = await client.GetAsync("/api/static-data", ct).ConfigureAwait(false);
            if (response.IsSuccessStatusCode)
            {
                var staticData = await response.Content.ReadFromJsonAsync<PkVaultStaticDataDto>(cancellationToken: ct).ConfigureAwait(false);
                if (staticData != null)
                {
                    _catalog.PopulateFromStaticData(staticData);
                }
            }
            _staticDataLoaded = true;
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Could not load PKVault static data catalog; using built-in fallbacks.");
        }
        finally
        {
            _staticDataLock.Release();
        }
    }

    private PokemonSummaryDto MapToSummary(
        PkVaultPkmSaveItemDto p,
        string gameId,
        bool isInParty,
        int? boxIndex,
        int slotIndex,
        PkVaultPkmLegalityDto? legality = null)
    {
        var id = !string.IsNullOrWhiteSpace(p.Id)
            ? p.Id
            : (isInParty ? $"party_{slotIndex}" : $"box_{boxIndex ?? 1}_{slotIndex}");

        var speciesName = _catalog.ResolveSpeciesName(p.Species, p.Nickname, p.IsNicknamed);
        var nickname = !string.IsNullOrWhiteSpace(p.Nickname) ? p.Nickname : speciesName;

        var legalityStatus = "valid";
        if (legality != null)
        {
            legalityStatus = (legality.IsValid && legality.IllegalitiesCount == 0) ? "valid" : "illegal";
        }
        else
        {
            var ivsValid = p.IVs == null || p.IVs.All(iv => iv is >= 0 and <= 31);
            var evsValid = p.EVs == null || (p.EVs.All(ev => ev is >= 0 and <= 252) && p.EVs.Sum() <= 510);
            var levelValid = p.Level is >= 1 and <= 100;
            var speciesValid = p.Species > 0;
            legalityStatus = (ivsValid && evsValid && levelValid && speciesValid) ? "valid" : "illegal";
        }

        return new PokemonSummaryDto
        {
            Id = id,
            SpeciesId = p.Species,
            Species = speciesName,
            Form = p.Form > 0 ? p.Form.ToString() : null,
            Nickname = nickname,
            Level = Math.Clamp(p.Level, 1, 100),
            Gender = FormatGender(p.Gender),
            IsShiny = p.IsShiny,
            OriginalTrainer = p.OriginTrainerName,
            OriginalTrainerId = p.Tid > 0 ? p.Tid.ToString() : null,
            CurrentGame = gameId,
            CurrentLocation = isInParty
                ? $"Party (Slot {slotIndex})"
                : $"Box {boxIndex ?? 1} (Slot {slotIndex})",
            BoxIndex = isInParty ? null : boxIndex,
            SlotIndex = slotIndex,
            IsInParty = isInParty,
            LegalityStatus = legalityStatus
        };
    }

    private PokemonDetailsDto MapToDetails(
        PkVaultPkmSaveItemDto p,
        PokemonSummaryDto summary,
        PkVaultPkmLegalityDto? legality = null)
    {
        var natureName = _catalog.ResolveNatureName(p.Nature);
        var abilityName = _catalog.ResolveAbilityName(p.Ability);
        var heldItemName = _catalog.ResolveItemName(p.HeldItem);
        var moveNames = (p.Moves ?? []).Select(m => _catalog.ResolveMoveName(m)).ToList();

        PokemonStatsDto? ivs = null;
        if (p.IVs != null && p.IVs.Length >= 6)
        {
            ivs = new PokemonStatsDto
            {
                Hp = p.IVs[0],
                Attack = p.IVs[1],
                Defense = p.IVs[2],
                Speed = p.IVs[3],
                SpecialAttack = p.IVs[4],
                SpecialDefense = p.IVs[5]
            };
        }

        PokemonStatsDto? evs = null;
        if (p.EVs != null && p.EVs.Length >= 6)
        {
            evs = new PokemonStatsDto
            {
                Hp = p.EVs[0],
                Attack = p.EVs[1],
                Defense = p.EVs[2],
                Speed = p.EVs[3],
                SpecialAttack = p.EVs[4],
                SpecialDefense = p.EVs[5]
            };
        }

        int? currentHp = null;
        int? maxHp = null;
        if (p.Stats != null && p.Stats.Length > 0)
        {
            currentHp = p.Stats[0];
            maxHp = p.Stats[0];
        }

        var legalityStatus = summary.LegalityStatus;
        string? legalityReport = null;
        var illegalitiesCount = 0;
        IReadOnlyList<bool>? movesLegality = null;

        if (legality != null)
        {
            legalityReport = legality.ValidityReport;
            illegalitiesCount = legality.IllegalitiesCount;
            movesLegality = legality.MovesLegality;
        }
        else
        {
            movesLegality = p.Moves?.Select(_ => true).ToList();
        }

        var ribbons = PokemonRibbonCatalog.EvaluateRibbons(
            p.Ribbons,
            evs,
            summary.Level,
            p.Friendship,
            summary.OriginGame,
            generation: 3,
            isInParty: summary.IsInParty);

        var isHallOfFame = ribbons.Any(r => r.Category.Equals("Champion", StringComparison.OrdinalIgnoreCase)) ||
            (summary.Level >= 55 && summary.IsInParty);

        summary.IsHallOfFameMember = isHallOfFame;

        return new PokemonDetailsDto
        {
            Summary = summary,
            Nature = natureName,
            Ability = abilityName,
            HeldItem = heldItemName,
            Moves = moveNames,
            Iv = ivs,
            Ev = evs,
            CurrentHp = currentHp,
            MaxHp = maxHp,
            Friendship = p.Friendship > 0 ? p.Friendship : null,
            Pokeball = p.Ball > 0 ? p.Ball.ToString() : null,
            RawData = p.DynamicChecksum,
            LegalityStatus = legalityStatus,
            LegalityReport = legalityReport,
            IllegalitiesCount = illegalitiesCount,
            MovesLegality = movesLegality,
            LearnableMoves = PokemonMovepoolProvider.GetLearnableMoves(summary.SpeciesId, summary.Species, summary.Level),
            Ribbons = ribbons,
            IsHallOfFameMember = isHallOfFame,
            AvailableEvolutions = PokemonEvolutionCatalog.GetAvailableEvolutions(summary.SpeciesId, summary.Species, summary.Level, heldItemName)
        };
    }

    private static string? FormatGender(int gender) => gender switch
    {
        0 => "M",
        1 => "F",
        _ => null
    };

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

        var client = _httpClientFactory.CreateClient("Vantafyn.Pokemon.PkVault");
        client.BaseAddress = uri;
        client.Timeout = timeout;

        string? stagedPath = null;
        try
        {
            // 1. Upload temporary save via POST /api/save-infos?saveFilesNames={name}&overwrite=true
            var ephemeralFileName = $"extract_{Guid.NewGuid():N}_{pokemonGameId}.sav";
            using var formData = new MultipartFormDataContent();
            var fileContent = new ByteArrayContent(saveBytes);
            fileContent.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream");
            formData.Add(fileContent, "saveFiles", ephemeralFileName);

            var uploadUri = $"/api/save-infos?saveFilesNames={Uri.EscapeDataString(ephemeralFileName)}&overwrite=true";
            var uploadResponse = await client.PostAsync(uploadUri, formData, cts.Token).ConfigureAwait(false);
            if (!uploadResponse.IsSuccessStatusCode)
            {
                var errorBody = await uploadResponse.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault upload failed during extraction for {GameId}: {Error}", pokemonGameId, errorBody);
                return new PokemonExtractResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault rejected save data (HTTP {(int)uploadResponse.StatusCode})."
                };
            }

            var dataDto = await uploadResponse.Content.ReadFromJsonAsync<PkVaultDataDto>(cancellationToken: cts.Token).ConfigureAwait(false);
            var saveInfo = dataDto?.SaveInfos?.Values.FirstOrDefault();
            if (saveInfo == null)
            {
                return new PokemonExtractResult
                {
                    IsSuccess = false,
                    ErrorMessage = "PKVault could not identify save information."
                };
            }

            var saveId = saveInfo.Id;
            stagedPath = saveInfo.Path;

            // 2. Locate the Pokémon to extract via GET /api/storage/save/{saveId}/pkm
            var pkmResponse = await client.GetAsync($"/api/storage/save/{saveId}/pkm", cts.Token).ConfigureAwait(false);
            if (!pkmResponse.IsSuccessStatusCode)
            {
                return new PokemonExtractResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault failed fetching Pokémon (HTTP {(int)pkmResponse.StatusCode})."
                };
            }

            var pkmList = await pkmResponse.Content.ReadFromJsonAsync<List<PkVaultPkmSaveItemDto>>(cancellationToken: cts.Token).ConfigureAwait(false) ?? [];

            // Match Pokémon by ID, party slot, or box slot
            PkVaultPkmSaveItemDto? targetPkm = null;
            if (!string.IsNullOrWhiteSpace(pokemonId))
            {
                targetPkm = pkmList.FirstOrDefault(p => string.Equals(p.Id, pokemonId, StringComparison.OrdinalIgnoreCase));
            }

            if (targetPkm == null)
            {
                if (isInParty)
                {
                    targetPkm = pkmList.FirstOrDefault(p => p.Party >= 0 && (p.Party == slotIndex - 1 || p.Party == slotIndex));
                }
                else
                {
                    targetPkm = pkmList.FirstOrDefault(p => p.Party < 0 &&
                        (boxIndex == null || p.BoxId == boxIndex.Value - 1 || p.BoxId == boxIndex.Value) &&
                        (p.BoxSlot == slotIndex - 1 || p.BoxSlot == slotIndex));
                }
            }

            if (targetPkm == null)
            {
                return new PokemonExtractResult
                {
                    IsSuccess = false,
                    ErrorMessage = "Target Pokémon was not found in save."
                };
            }

            var summary = MapToSummary(targetPkm, pokemonGameId, isInParty, boxIndex, slotIndex);
            var details = MapToDetails(targetPkm, summary);

            // 3. Delete Pokémon from save via DELETE /api/storage/save/{saveId}/pkm?pkmIds={pkmId}
            var deleteUri = $"/api/storage/save/{saveId}/pkm?pkmIds={Uri.EscapeDataString(targetPkm.Id)}";
            var deleteResponse = await client.DeleteAsync(deleteUri, cts.Token).ConfigureAwait(false);
            if (!deleteResponse.IsSuccessStatusCode)
            {
                var errorBody = await deleteResponse.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault failed deleting Pokémon {PkmId} from save {SaveId}: {Error}", targetPkm.Id, saveId, errorBody);
                return new PokemonExtractResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault could not remove Pokémon from save (HTTP {(int)deleteResponse.StatusCode})."
                };
            }

            // 4. Commit changes to save via POST /api/storage/action/save
            var commitResponse = await client.PostAsync("/api/storage/action/save", null, cts.Token).ConfigureAwait(false);
            if (!commitResponse.IsSuccessStatusCode)
            {
                var errorBody = await commitResponse.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault failed committing save {SaveId}: {Error}", saveId, errorBody);
                return new PokemonExtractResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault could not commit save changes (HTTP {(int)commitResponse.StatusCode})."
                };
            }

            // 5. Download updated save bytes via GET /api/save-infos/{saveId}/download
            var downloadResponse = await client.GetAsync($"/api/save-infos/{saveId}/download", cts.Token).ConfigureAwait(false);
            if (!downloadResponse.IsSuccessStatusCode)
            {
                return new PokemonExtractResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault could not download updated save (HTTP {(int)downloadResponse.StatusCode})."
                };
            }

            var updatedBytes = await downloadResponse.Content.ReadAsByteArrayAsync(cts.Token).ConfigureAwait(false);

            return new PokemonExtractResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = updatedBytes,
                ExtractedPokemon = details
            };
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            return new PokemonExtractResult { IsSuccess = false, ErrorMessage = "PKVault extraction timed out." };
        }
        catch (HttpRequestException ex)
        {
            _logger.LogWarning(ex, "Failed to connect to PKVault for save extraction of {GameId}.", pokemonGameId);
            return new PokemonExtractResult { IsSuccess = false, ErrorMessage = "PKVault service is unreachable." };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to extract Pokémon {PokemonId} from {GameId}", pokemonId, pokemonGameId);
            return new PokemonExtractResult { IsSuccess = false, ErrorMessage = $"Extraction failed: {ex.Message}" };
        }
        finally
        {
            if (!string.IsNullOrWhiteSpace(stagedPath))
            {
                try
                {
                    await client.DeleteAsync($"/api/save-infos?path={Uri.EscapeDataString(stagedPath)}", CancellationToken.None).ConfigureAwait(false);
                }
                catch (Exception cleanupEx)
                {
                    _logger.LogDebug(cleanupEx, "Failed to clean up staged save at {StagedPath} on PKVault", stagedPath);
                }
            }
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

        var client = _httpClientFactory.CreateClient("Vantafyn.Pokemon.PkVault");
        client.BaseAddress = uri;
        client.Timeout = timeout;

        string? stagedPath = null;
        try
        {
            // 1. Upload temporary save via POST /api/save-infos?saveFilesNames={name}&overwrite=true
            var ephemeralFileName = $"inject_{Guid.NewGuid():N}_{pokemonGameId}.sav";
            using var formData = new MultipartFormDataContent();
            var fileContent = new ByteArrayContent(saveBytes);
            fileContent.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream");
            formData.Add(fileContent, "saveFiles", ephemeralFileName);

            var uploadUri = $"/api/save-infos?saveFilesNames={Uri.EscapeDataString(ephemeralFileName)}&overwrite=true";
            var uploadResponse = await client.PostAsync(uploadUri, formData, cts.Token).ConfigureAwait(false);
            if (!uploadResponse.IsSuccessStatusCode)
            {
                var errorBody = await uploadResponse.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault upload failed during injection for {GameId}: {Error}", pokemonGameId, errorBody);
                return new PokemonInjectResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault rejected save data (HTTP {(int)uploadResponse.StatusCode})."
                };
            }

            var dataDto = await uploadResponse.Content.ReadFromJsonAsync<PkVaultDataDto>(cancellationToken: cts.Token).ConfigureAwait(false);
            var saveInfo = dataDto?.SaveInfos?.Values.FirstOrDefault();
            if (saveInfo == null)
            {
                return new PokemonInjectResult
                {
                    IsSuccess = false,
                    ErrorMessage = "PKVault could not identify save information."
                };
            }

            var saveId = saveInfo.Id;
            stagedPath = saveInfo.Path;

            // 2. Find target box via GET /api/storage/box?saveId={saveId}
            var boxResponse = await client.GetAsync($"/api/storage/box?saveId={saveId}", cts.Token).ConfigureAwait(false);
            List<PkVaultBoxItemDto>? boxesList = null;
            if (boxResponse.IsSuccessStatusCode)
            {
                boxesList = await boxResponse.Content.ReadFromJsonAsync<List<PkVaultBoxItemDto>>(cancellationToken: cts.Token).ConfigureAwait(false);
            }

            PkVaultBoxItemDto? targetBox = null;
            if (targetParty)
            {
                targetBox = boxesList?.FirstOrDefault(b => b.Type == 14);
            }
            else if (targetBoxIndex.HasValue)
            {
                targetBox = boxesList?.FirstOrDefault(b => b.Type == 0 && (b.Order == targetBoxIndex.Value - 1 || b.IdInt == targetBoxIndex.Value - 1));
            }

            targetBox ??= boxesList?.FirstOrDefault(b => b.Type == 0) ?? boxesList?.FirstOrDefault();
            var targetBoxId = targetBox?.Id ?? "0";
            var targetSlot = Math.Max(0, (targetSlotIndex ?? 1) - 1);
            var pkmId = !string.IsNullOrWhiteSpace(entry.RawData) ? entry.RawData : entry.Id;

            // 3. Move/Inject Pokémon into save via PUT /api/storage/move/pkm
            var moveUri = $"/api/storage/move/pkm?pkmIds={Uri.EscapeDataString(pkmId)}&sourceSaveId=&targetSaveId={saveId}&targetBoxId={Uri.EscapeDataString(targetBoxId)}&targetBoxSlots={targetSlot}&attached=false";
            var moveResponse = await client.PutAsync(moveUri, null, cts.Token).ConfigureAwait(false);
            if (!moveResponse.IsSuccessStatusCode)
            {
                var errorBody = await moveResponse.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault move failed for {PkmId} to save {SaveId}: {Error}", pkmId, saveId, errorBody);
                return new PokemonInjectResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault could not inject Pokémon into save (HTTP {(int)moveResponse.StatusCode})."
                };
            }

            // 4. Commit changes to save via POST /api/storage/action/save
            var commitResponse = await client.PostAsync("/api/storage/action/save", null, cts.Token).ConfigureAwait(false);
            if (!commitResponse.IsSuccessStatusCode)
            {
                var errorBody = await commitResponse.Content.ReadAsStringAsync(cts.Token).ConfigureAwait(false);
                _logger.LogWarning("PKVault failed committing injected save {SaveId}: {Error}", saveId, errorBody);
                return new PokemonInjectResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault could not commit save changes (HTTP {(int)commitResponse.StatusCode})."
                };
            }

            // 5. Download updated save bytes via GET /api/save-infos/{saveId}/download
            var downloadResponse = await client.GetAsync($"/api/save-infos/{saveId}/download", cts.Token).ConfigureAwait(false);
            if (!downloadResponse.IsSuccessStatusCode)
            {
                return new PokemonInjectResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"PKVault could not download updated save (HTTP {(int)downloadResponse.StatusCode})."
                };
            }

            var updatedBytes = await downloadResponse.Content.ReadAsByteArrayAsync(cts.Token).ConfigureAwait(false);
            var locationDescription = targetParty
                ? $"Party Slot {targetSlot + 1}"
                : $"Box {targetBoxIndex ?? 1} Slot {targetSlot + 1}";

            return new PokemonInjectResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = updatedBytes,
                AssignedLocation = locationDescription
            };
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            return new PokemonInjectResult { IsSuccess = false, ErrorMessage = "PKVault injection timed out." };
        }
        catch (HttpRequestException ex)
        {
            _logger.LogWarning(ex, "Failed to connect to PKVault for save injection of {GameId}.", pokemonGameId);
            return new PokemonInjectResult { IsSuccess = false, ErrorMessage = "PKVault service is unreachable." };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to inject Pokémon {Species} into {GameId}", entry.Species, pokemonGameId);
            return new PokemonInjectResult { IsSuccess = false, ErrorMessage = $"Injection failed: {ex.Message}" };
        }
        finally
        {
            if (!string.IsNullOrWhiteSpace(stagedPath))
            {
                try
                {
                    await client.DeleteAsync($"/api/save-infos?path={Uri.EscapeDataString(stagedPath)}", CancellationToken.None).ConfigureAwait(false);
                }
                catch (Exception cleanupEx)
                {
                    _logger.LogDebug(cleanupEx, "Failed to clean up staged save at {StagedPath} on PKVault", stagedPath);
                }
            }
        }
    }

    private sealed class PkVaultSettingsResponse
    {
        [JsonPropertyName("version")]
        public string? Version { get; set; }

        [JsonPropertyName("pkhexVersion")]
        public string? PkhexVersion { get; set; }

        [JsonPropertyName("canUploadSaves")]
        public bool? CanUploadSaves { get; set; }

        [JsonPropertyName("canCreateBackup")]
        public bool? CanCreateBackup { get; set; }
    }

    private sealed class PkVaultDataDto
    {
        [JsonPropertyName("saveInfos")]
        public Dictionary<string, PkVaultSaveInfoDto>? SaveInfos { get; set; }

        [JsonPropertyName("saves")]
        public List<PkVaultDataSaveDto>? Saves { get; set; }

        [JsonPropertyName("mainPkmLegalities")]
        public PkVaultDataStateLegalityDto? MainPkmLegalities { get; set; }
    }

    private sealed class PkVaultDataSaveDto
    {
        [JsonPropertyName("saveId")]
        public int SaveId { get; set; }

        [JsonPropertyName("savePkmLegality")]
        public PkVaultDataStateLegalityDto? SavePkmLegality { get; set; }
    }

    private sealed class PkVaultDataStateLegalityDto
    {
        [JsonPropertyName("all")]
        public bool All { get; set; }

        [JsonPropertyName("data")]
        public Dictionary<string, PkVaultPkmLegalityDto>? Data { get; set; }
    }

    private sealed class PkVaultPkmLegalityDto
    {
        [JsonPropertyName("id")]
        public string Id { get; set; } = string.Empty;

        [JsonPropertyName("saveId")]
        public int? SaveId { get; set; }

        [JsonPropertyName("movesLegality")]
        public List<bool>? MovesLegality { get; set; }

        [JsonPropertyName("relearnMovesLegality")]
        public List<bool>? RelearnMovesLegality { get; set; }

        [JsonPropertyName("isValid")]
        public bool IsValid { get; set; }

        [JsonPropertyName("validityReport")]
        public string? ValidityReport { get; set; }

        [JsonPropertyName("illegalitiesCount")]
        public int IllegalitiesCount { get; set; }
    }

    private sealed class PkVaultSaveInfoDto
    {
        [JsonPropertyName("id")]
        public int Id { get; set; }

        [JsonPropertyName("path")]
        public string Path { get; set; } = string.Empty;

        [JsonPropertyName("trainerName")]
        public string? TrainerName { get; set; }

        [JsonPropertyName("tid")]
        public uint Tid { get; set; }

        [JsonPropertyName("sid")]
        public uint Sid { get; set; }

        [JsonPropertyName("generation")]
        public int Generation { get; set; }

        [JsonPropertyName("dexSeenCount")]
        public int DexSeenCount { get; set; }

        [JsonPropertyName("dexCaughtCount")]
        public int DexCaughtCount { get; set; }

        [JsonPropertyName("partyCount")]
        public int PartyCount { get; set; }

        [JsonPropertyName("boxCount")]
        public int BoxCount { get; set; }

        [JsonPropertyName("boxSlotCount")]
        public int BoxSlotCount { get; set; }
    }

    private sealed class PkVaultBoxItemDto
    {
        [JsonPropertyName("id")]
        public string Id { get; set; } = string.Empty;

        [JsonPropertyName("type")]
        public int Type { get; set; }

        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;

        [JsonPropertyName("slotCount")]
        public int SlotCount { get; set; } = 30;

        [JsonPropertyName("order")]
        public int Order { get; set; }

        [JsonPropertyName("idInt")]
        public int IdInt { get; set; }
    }

    private sealed class PkVaultPkmSaveItemDto
    {
        [JsonPropertyName("id")]
        public string Id { get; set; } = string.Empty;

        [JsonPropertyName("idBase")]
        public string? IdBase { get; set; }

        [JsonPropertyName("saveId")]
        public int SaveId { get; set; }

        [JsonPropertyName("party")]
        public int Party { get; set; } = -1;

        [JsonPropertyName("boxId")]
        public int BoxId { get; set; }

        [JsonPropertyName("boxSlot")]
        public int BoxSlot { get; set; }

        [JsonPropertyName("species")]
        public int Species { get; set; }

        [JsonPropertyName("form")]
        public int Form { get; set; }

        [JsonPropertyName("nickname")]
        public string? Nickname { get; set; }

        [JsonPropertyName("isNicknamed")]
        public bool IsNicknamed { get; set; }

        [JsonPropertyName("level")]
        public int Level { get; set; } = 1;

        [JsonPropertyName("gender")]
        public int Gender { get; set; }

        [JsonPropertyName("isShiny")]
        public bool IsShiny { get; set; }

        [JsonPropertyName("nature")]
        public int Nature { get; set; }

        [JsonPropertyName("ability")]
        public int Ability { get; set; }

        [JsonPropertyName("heldItem")]
        public int HeldItem { get; set; }

        [JsonPropertyName("moves")]
        public List<int>? Moves { get; set; }

        [JsonPropertyName("iVs")]
        public int[]? IVs { get; set; }

        [JsonPropertyName("eVs")]
        public int[]? EVs { get; set; }

        [JsonPropertyName("stats")]
        public int[]? Stats { get; set; }

        [JsonPropertyName("friendship")]
        public int Friendship { get; set; }

        [JsonPropertyName("ball")]
        public int Ball { get; set; }

        [JsonPropertyName("originTrainerName")]
        public string? OriginTrainerName { get; set; }

        [JsonPropertyName("tid")]
        public uint Tid { get; set; }

        [JsonPropertyName("ribbons")]
        public Dictionary<string, byte>? Ribbons { get; set; }

        [JsonPropertyName("dynamicChecksum")]
        public string? DynamicChecksum { get; set; }
    }
}

using System.IO;
using System.Net.Mime;
using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;

namespace Vantafyn.Plugin.Companion.Pokemon;

[ApiController]
[Authorize]
[Route("Vantafyn/Pokemon")]
public sealed class PokemonController : ControllerBase
{
    // Bump this whenever the synthesis prompt or client-side narration profile changes in a
    // way that should not reuse previously rendered species audio.
    private const string NarrationRenderProfile = "pokedex-voice-v2";
    private static readonly ConcurrentDictionary<string, (Guid UserId, DateTimeOffset ExpiresAt, PokemonSaveParseResult Parsed, int Generation)> ExternalSavePreviews = new();
    private readonly IGamesService _gamesService;
    private readonly IGameSavesService _gameSavesService;
    private readonly IPokemonGameDetector _detector;
    private readonly IPokemonVaultStore _vaultStore;
    private readonly IPokemonProviderFactory _providerFactory;
    private readonly ISaveOperationCoordinator _saveCoordinator;
    private readonly IGameSessionTracker _sessionTracker;
    private readonly IPokemonBackupService _backupService;
    private readonly IPokemonTransactionManager _transactionManager;
    private readonly IPokemonTradingService? _tradingService;
    private readonly IPokemonJourneyService? _journeyService;
    private readonly IPokemonSocialService? _socialService;
    private readonly IPokemonCryService? _cryService;
    private readonly IAuthorizationContext _authorizationContext;
    private readonly PokemonConfiguration? _overrideConfig;
    private readonly ICompanionPaths? _paths;
    private static readonly HttpClient PokeApiClient = new() { Timeout = TimeSpan.FromSeconds(12) };
    private static readonly HttpClient NarrationClient = new();

    public PokemonController(
        IGamesService gamesService,
        IGameSavesService gameSavesService,
        IPokemonGameDetector detector,
        IPokemonVaultStore vaultStore,
        IPokemonProviderFactory providerFactory,
        ISaveOperationCoordinator saveCoordinator,
        IGameSessionTracker sessionTracker,
        IPokemonBackupService backupService,
        IPokemonTransactionManager transactionManager,
        IAuthorizationContext authorizationContext,
        PokemonConfiguration? configuration = null,
        IPokemonTradingService? tradingService = null,
        IPokemonJourneyService? journeyService = null,
        IPokemonSocialService? socialService = null,
        IPokemonCryService? cryService = null,
        ICompanionPaths? paths = null)
    {
        _gamesService = gamesService;
        _gameSavesService = gameSavesService;
        _detector = detector;
        _vaultStore = vaultStore;
        _providerFactory = providerFactory;
        _saveCoordinator = saveCoordinator;
        _sessionTracker = sessionTracker;
        _backupService = backupService;
        _transactionManager = transactionManager;
        _authorizationContext = authorizationContext;
        _overrideConfig = configuration;
        _tradingService = tradingService;
        _journeyService = journeyService;
        _socialService = socialService;
        _cryService = cryService;
        _paths = paths;
    }

    private PokemonConfiguration Configuration
    {
        get
        {
            if (_overrideConfig != null) return _overrideConfig;
            var plugin = Plugin.Instance;
            var config = plugin?.Configuration?.Pokemon ?? new PokemonConfiguration();
            if (plugin != null && config.MigrateLegacyNarrationDefault())
            {
                plugin.SaveConfiguration();
            }
            return config;
        }
    }

    private bool IsPokemonIntegrationEnabled() =>
        Configuration.Enabled;

    /// <summary>
    /// Returns canonical Pokédex metadata. Responses are shared, persisted server-side and
    /// populated from PokéAPI only when a species is first requested.
    /// </summary>
    [HttpGet("Dex/{speciesId:int}")]
    [Produces(MediaTypeNames.Application.Json)]
    public async Task<ActionResult<PokemonDexMetadataDto>> GetDexMetadata(int speciesId, CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled()) return NotFound(new { error = "Pokémon integration is disabled." });
        if (speciesId is < 1 or > 1025) return BadRequest(new { error = "SpeciesId must be between 1 and 1025." });
        try
        {
            return Ok(await GetDexMetadataAsync(speciesId, cancellationToken).ConfigureAwait(false));
        }
        catch (Exception ex)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = $"Pokédex metadata is unavailable: {ex.Message}" });
        }
    }

    /// <summary>
    /// Streams cached Pokédex narration. The companion is the only component which can
    /// reach the TTS service; the service URL and its Docker network stay private.
    /// </summary>
    [HttpGet("Narration/{speciesId:int}")]
    [Produces("audio/mpeg")]
    public async Task<IActionResult> GetNarration(int speciesId, CancellationToken cancellationToken)
    {
        var config = Configuration;
        if (!IsPokemonIntegrationEnabled() || !config.NarrationEnabled)
        {
            return NotFound(new { error = "Pokédex narration is disabled." });
        }
        if (speciesId is < 1 or > 1025) return BadRequest(new { error = "SpeciesId must be between 1 and 1025." });
        if (!TryGetNarrationEndpoint(config.NarrationBaseUrl, out var endpoint))
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Pokédex narration is not configured." });
        }

        try
        {
            var metadata = await GetDexMetadataAsync(speciesId, cancellationToken).ConfigureAwait(false);
            var script = BuildNarrationScript(metadata);
            var cachePath = NarrationCachePath(speciesId, script, config);
            if (System.IO.File.Exists(cachePath)) return PhysicalFile(cachePath, "audio/mpeg", enableRangeProcessing: true);

            using var request = new HttpRequestMessage(HttpMethod.Post, endpoint)
            {
                Content = new StringContent(JsonSerializer.Serialize(new
                {
                    model = "kokoro",
                    input = script,
                    voice = config.NarrationVoice,
                    speed = (double)Math.Clamp(config.NarrationSpeed, 0.75m, 1.35m),
                    response_format = "mp3"
                }), Encoding.UTF8, "application/json")
            };
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            timeout.CancelAfter(TimeSpan.FromSeconds(Math.Clamp(config.NarrationTimeoutSeconds, 5, 60)));
            using var response = await NarrationClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, timeout.Token).ConfigureAwait(false);
            if (!response.IsSuccessStatusCode)
            {
                return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Pokédex narration service did not generate audio." });
            }
            if (response.Content.Headers.ContentLength is > 25L * 1024L * 1024L)
            {
                return StatusCode(StatusCodes.Status502BadGateway, new { error = "Pokédex narration response was too large." });
            }

            Directory.CreateDirectory(Path.GetDirectoryName(cachePath)!);
            var tempPath = cachePath + ".tmp";
            await using (var source = await response.Content.ReadAsStreamAsync(timeout.Token).ConfigureAwait(false))
            await using (var target = new FileStream(tempPath, FileMode.Create, FileAccess.Write, FileShare.None))
            {
                var buffer = new byte[81920];
                long total = 0;
                int read;
                while ((read = await source.ReadAsync(buffer, timeout.Token).ConfigureAwait(false)) > 0)
                {
                    total += read;
                    if (total > 25L * 1024L * 1024L) throw new InvalidDataException("Narration response was too large.");
                    await target.WriteAsync(buffer.AsMemory(0, read), timeout.Token).ConfigureAwait(false);
                }
            }
            System.IO.File.Move(tempPath, cachePath, true);
            return PhysicalFile(cachePath, "audio/mpeg", enableRangeProcessing: true);
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Pokédex narration service timed out." });
        }
        catch
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Pokédex narration is temporarily unavailable." });
        }
    }

    private async Task<PokemonDexMetadataDto> GetDexMetadataAsync(int speciesId, CancellationToken cancellationToken)
    {
        var root = _paths?.PokemonRoot ?? Path.Combine(Plugin.Instance?.DataRootPath ?? Path.GetTempPath(), "pokemon");
        var path = Path.Combine(root, "pokedex-metadata", $"{speciesId}.json");
        var cached = await JsonFile.ReadAsync<PokemonDexMetadataDto>(path, cancellationToken).ConfigureAwait(false);
        if (cached != null && !string.IsNullOrWhiteSpace(cached.Name)) return cached;
        using var speciesDoc = JsonDocument.Parse(await PokeApiClient.GetStringAsync($"https://pokeapi.co/api/v2/pokemon-species/{speciesId}/", cancellationToken).ConfigureAwait(false));
        using var pokemonDoc = JsonDocument.Parse(await PokeApiClient.GetStringAsync($"https://pokeapi.co/api/v2/pokemon/{speciesId}/", cancellationToken).ConfigureAwait(false));
        var species = speciesDoc.RootElement; var pokemon = pokemonDoc.RootElement;
        static string Name(JsonElement item, string property) => item.GetProperty(property).GetProperty("name").GetString() ?? string.Empty;
        static string StringValue(JsonElement item, string property) => item.ValueKind != JsonValueKind.Undefined && item.TryGetProperty(property, out var value) ? value.GetString() ?? string.Empty : string.Empty;
        var genus = StringValue(species.GetProperty("genera").EnumerateArray().LastOrDefault(x => Name(x, "language") == "en"), "genus");
        var flavor = StringValue(species.GetProperty("flavor_text_entries").EnumerateArray().LastOrDefault(x => Name(x, "language") == "en"), "flavor_text");
        var stats = pokemon.GetProperty("stats").EnumerateArray().ToDictionary(x => Name(x, "stat"), x => x.GetProperty("base_stat").GetInt32());
        var types = pokemon.GetProperty("types").EnumerateArray().OrderBy(x => x.GetProperty("slot").GetInt32()).Select(x => Name(x, "type")).ToList();
        var dto = new PokemonDexMetadataDto { SpeciesId = speciesId, Name = HumanizeSpeciesName(species.GetProperty("name").GetString()), Category = genus, FlavorText = string.Join(" ", flavor.Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries)), HeightMeters = pokemon.GetProperty("height").GetDecimal() / 10m, WeightKg = pokemon.GetProperty("weight").GetDecimal() / 10m, Hp = stats.GetValueOrDefault("hp"), Attack = stats.GetValueOrDefault("attack"), Defense = stats.GetValueOrDefault("defense"), SpAtk = stats.GetValueOrDefault("special-attack"), SpDef = stats.GetValueOrDefault("special-defense"), Speed = stats.GetValueOrDefault("speed"), PrimaryType = types.ElementAtOrDefault(0) ?? string.Empty, SecondaryType = types.ElementAtOrDefault(1) };
        await JsonFile.WriteAtomicAsync(path, dto, cancellationToken).ConfigureAwait(false);
        return dto;
    }

    private string NarrationCachePath(int speciesId, string script, PokemonConfiguration config)
    {
        var root = _paths?.PokemonRoot ?? Path.Combine(Plugin.Instance?.DataRootPath ?? Path.GetTempPath(), "pokemon");
        var key = $"{NarrationRenderProfile}|{config.NarrationVoice}|{config.NarrationSpeed:0.00}|{script}";
        var hash = Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(key))).ToLowerInvariant()[..16];
        return Path.Combine(root, "pokedex-narration", speciesId.ToString(), $"{hash}.mp3");
    }

    private static bool TryGetNarrationEndpoint(string? baseUrl, out Uri endpoint)
    {
        endpoint = null!;
        if (!Uri.TryCreate(baseUrl?.TrimEnd('/') + "/v1/audio/speech", UriKind.Absolute, out var uri)) return false;
        if (uri.Scheme is not ("http" or "https") || string.IsNullOrWhiteSpace(uri.Host)) return false;
        endpoint = uri;
        return true;
    }

    private static string BuildNarrationScript(PokemonDexMetadataDto metadata)
    {
        var name = FormatNarrationName(metadata.Name);
        var category = string.IsNullOrWhiteSpace(metadata.Category) ? "Pokémon" : metadata.Category;
        var flavor = string.IsNullOrWhiteSpace(metadata.FlavorText) ? "No Pokédex entry is available." : metadata.FlavorText;
        return $"{name}. The {FormatNarrationText(category)}. {FormatNarrationText(flavor)}";
    }

    // Kokoro is a general English narrator, rather than a Pokémon-specific voice model.
    // Use plain phonetic spellings here instead of engine-specific markup: this works with
    // every OpenAI-compatible service supported by the Companion and keeps the spoken
    // wording independent of the on-screen canonical spelling.
    private static string FormatNarrationText(string text) => text
        .Replace("Pokémon", "Poh-kay-mon", StringComparison.OrdinalIgnoreCase)
        .Replace("Pokédex", "Poh-kay-dex", StringComparison.OrdinalIgnoreCase);

    private static string FormatNarrationName(string? name)
    {
        var displayName = string.IsNullOrWhiteSpace(name) ? "Unknown Pokémon" : name.Trim();
        var phoneticName = displayName switch
        {
            "Mr Mime" => "Mister Mime",
            "Mime Jr" => "Mime Junior",
            "Farfetch D" => "Far-fetched",
            "Sirfetch D" => "Sir-fetched",
            "Nidoran F" => "Nidoran female",
            "Nidoran M" => "Nidoran male",
            "Type Null" => "Type Null",
            "Jangmo O" => "Jang-mo-oh",
            "Hakamo O" => "Ha-ka-mo-oh",
            "Kommo O" => "Kom-mo-oh",
            "Ho Oh" => "Ho-oh",
            "Porygon Z" => "Porygon Zee",
            "Great Tusk" => "Great Tusk",
            _ => displayName
        };
        return FormatNarrationText(phoneticName);
    }

    private static string HumanizeSpeciesName(string? name) => string.Join(" ", (name ?? "Pokémon")
        .Split('-', StringSplitOptions.RemoveEmptyEntries)
        .Select(part => char.ToUpperInvariant(part[0]) + part[1..]));

    /// <summary>
    /// Reads an external Gen 6–9 save into a short-lived, user-scoped selection preview.
    /// </summary>
    [HttpPost("Vault/ImportSave/Preview")]
    [Consumes("multipart/form-data")]
    public async Task<ActionResult<PokemonExternalSavePreview>> PreviewExternalSave([FromForm] IFormFile? saveFile, CancellationToken cancellationToken)
    {
        const long maxSaveBytes = 64L * 1024L * 1024L;
        if (!IsPokemonIntegrationEnabled()) return NotFound(new { error = "Pokémon integration is disabled." });
        if (saveFile == null || saveFile.Length == 0 || saveFile.Length > maxSaveBytes) return BadRequest(new { error = "Choose a save file no larger than 64 MB." });
        await using var input = saveFile.OpenReadStream();
        using var bytes = new MemoryStream((int)saveFile.Length);
        await input.CopyToAsync(bytes, cancellationToken).ConfigureAwait(false);
        var provider = _providerFactory.Create(Configuration);
        var capabilities = await provider.GetCapabilitiesAsync(cancellationToken).ConfigureAwait(false);
        if (!capabilities.CanReadSaves || !capabilities.SupportedGenerations.Any(g => g is "6" or "7" or "8" or "9")) return BadRequest(new { error = "PKVault Gen 6–9 support is required." });
        var parsed = await provider.ParseSaveAsync(bytes.ToArray(), "external-emulator-save", "external", 0, cancellationToken).ConfigureAwait(false);
        var generation = parsed.DetectedGeneration.GetValueOrDefault();
        if (!parsed.IsSuccess) return BadRequest(new { error = parsed.ErrorMessage ?? "The selected file is not a readable Pokémon save." });
        if (generation is < 6 or > 9) return BadRequest(new { error = "This importer accepts Gen 6–9 emulator saves only." });
        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var previewId = Guid.NewGuid().ToString("N");
        ExternalSavePreviews.Where(p => p.Value.ExpiresAt <= DateTimeOffset.UtcNow).Select(p => p.Key).ToList().ForEach(k => ExternalSavePreviews.TryRemove(k, out _));
        ExternalSavePreviews[previewId] = (userId, DateTimeOffset.UtcNow.AddMinutes(10), parsed, generation);
        return Ok(new PokemonExternalSavePreview { PreviewId = previewId, Generation = generation, TrainerName = parsed.TrainerName, Party = parsed.Party, Boxes = parsed.Boxes });
    }

    [HttpPost("Vault/ImportSave/Commit")]
    public async Task<ActionResult<PokemonExternalSaveImportResponse>> CommitExternalSavePreview([FromBody] PokemonExternalSaveCommitRequest request, CancellationToken cancellationToken)
    {
        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        if (string.IsNullOrWhiteSpace(request.PreviewId) || !ExternalSavePreviews.TryRemove(request.PreviewId, out var preview) || preview.UserId != userId || preview.ExpiresAt <= DateTimeOffset.UtcNow)
            return BadRequest(new { error = "This save preview has expired. Choose the save again." });
        var selectedIds = request.PokemonIds.Where(id => !string.IsNullOrWhiteSpace(id)).ToHashSet(StringComparer.Ordinal);
        var all = preview.Parsed.Party.Concat(preview.Parsed.Boxes.SelectMany(b => b.Entries)).Where(p => p.SpeciesId > 0).ToList();
        var selected = all.Where(p => selectedIds.Contains(p.Id)).ToList();
        if (selected.Count == 0) return BadRequest(new { error = "Select at least one Pokémon to import." });
        if (selected.Count != selectedIds.Count) return BadRequest(new { error = "The selection does not match this save preview." });
        var vault = await _vaultStore.GetOrCreateVaultAsync(userId, cancellationToken).ConfigureAwait(false);
        var slots = vault.Boxes.OrderBy(b => b.BoxIndex).SelectMany(b => Enumerable.Range(1, b.Capacity).Where(s => b.Entries.All(e => e.SlotIndex != s)).Select(s => (Box: b, Slot: s))).Take(selected.Count).ToList();
        if (slots.Count != selected.Count) return BadRequest(new { error = "There are not enough free Vault slots for this selection." });
        var now = DateTimeOffset.UtcNow;
        for (var i = 0; i < selected.Count; i++)
        {
            var p = selected[i]; preview.Parsed.Details.TryGetValue(p.Id, out var details); var target = slots[i];
            target.Box.Entries.Add(new PokemonVaultEntry { Id = Guid.NewGuid().ToString("N"), BoxIndex = target.Box.BoxIndex, SlotIndex = target.Slot, Species = p.Species, SpeciesId = p.SpeciesId, Form = p.Form, Nickname = string.IsNullOrWhiteSpace(p.Nickname) ? p.Species : p.Nickname, Level = p.Level, Gender = p.Gender, IsShiny = p.IsShiny, Generation = preview.Generation, OriginalTrainer = p.OriginalTrainer ?? preview.Parsed.TrainerName ?? string.Empty, OriginalTrainerId = p.OriginalTrainerId ?? preview.Parsed.TrainerId, OriginGame = p.OriginGame ?? $"Imported Gen {preview.Generation} emulator save", OriginGameId = "external-import", CurrentLocation = $"Vault Box {target.Box.BoxIndex}, Slot {target.Slot}", CreatedAtUtc = now, UpdatedAtUtc = now, RawData = details?.RawData, Details = details, LegalityStatus = details?.LegalityStatus ?? p.LegalityStatus });
        }
        await _vaultStore.SaveVaultAsync(userId, vault, cancellationToken).ConfigureAwait(false);
        if (_journeyService != null) try { await _journeyService.RecordBatchEncountersAsync(userId, selected, $"Imported Gen {preview.Generation} emulator save", true, cancellationToken).ConfigureAwait(false); } catch { }
        return Ok(new PokemonExternalSaveImportResponse { Success = true, ImportedCount = selected.Count, Generation = preview.Generation, TrainerName = preview.Parsed.TrainerName, Message = $"Imported {selected.Count} Pokémon. Your original save was not changed." });
    }

    /// <summary>
    /// Lists all detected Pokémon games across all available libraries on this server for the current user,
    /// complete with save status (hasSave) and generation metadata.
    /// </summary>
    [HttpGet("Games")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<IEnumerable<GameSummary>>> GetPokemonGames(CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return Ok(Array.Empty<GameSummary>());
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var libraries = _gamesService.GetGameLibraries();
        var result = new List<GameSummary>();

        foreach (var library in libraries)
        {
            var games = _gamesService.GetGames(library.Id);
            foreach (var game in games)
            {
                var meta = await _detector.GetMetadataAsync(
                    userId: userId,
                    gameId: game.Id,
                    title: game.Title,
                    fileName: game.FileName,
                    system: game.System,
                    core: game.Core,
                    cancellationToken: cancellationToken).ConfigureAwait(false);

                if (meta != null && meta.IsPokemonGame)
                {
                    game.Pokemon = meta;
                    result.Add(game);
                }
            }
        }

        return Ok(result);
    }

    /// <summary>
    /// Retrieves game details and Pokémon metadata for a specific game ID.
    /// </summary>
    [HttpGet("Games/{libraryId}/{gameId}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<GameDetail>> GetPokemonGame(
        [FromRoute] string libraryId,
        [FromRoute] string gameId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = _gamesService.GetGame(libraryId, gameId) ?? FindGame(gameId);
        if (game == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var meta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: game.Id,
            title: game.Title,
            fileName: game.FileName,
            system: game.System,
            core: game.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        if (meta == null || !meta.IsPokemonGame)
        {
            return NotFound();
        }

        game.Pokemon = meta;
        return Ok(game);
    }

    /// <summary>
    /// Returns the current user's Pokémon integration status, provider health, and operational capabilities.
    /// </summary>
    [HttpGet("Status")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<PokemonIntegrationStatusDto>> GetStatus(CancellationToken cancellationToken)
    {
        var config = Configuration;
        var bgPath = config.ModalBackgroundPath?.Trim('\"', '\'').Trim();
        var hasBackground = !string.IsNullOrEmpty(bgPath);

        if (!config.Enabled)
        {
            return Ok(new PokemonIntegrationStatusDto
            {
                Enabled = false,
                Provider = "none",
                ProviderHealthy = false,
                ProviderMessage = "Pokémon integration is disabled.",
                SupportedGenerations = [],
                VaultAvailable = false,
                TransfersAvailable = false,
                CrossGenerationAvailable = false,
                TradingAvailable = false,
                HasCustomBackground = hasBackground
            });
        }

        try
        {
            var provider = _providerFactory.Create(config);
            var testResult = await provider.TestConnectionAsync(cancellationToken).ConfigureAwait(false);
            var capabilities = await provider.GetCapabilitiesAsync(cancellationToken).ConfigureAwait(false);

            return Ok(new PokemonIntegrationStatusDto
            {
                Enabled = true,
                Provider = provider.ProviderName,
                ProviderHealthy = testResult.IsSuccess,
                ProviderMessage = testResult.Message,
                SupportedGenerations = capabilities.SupportedGenerations,
                VaultAvailable = true,
                TransfersAvailable = config.AllowTransfers && capabilities.CanTransferSameGeneration,
                CrossGenerationAvailable = config.AllowCrossGenerationTransfers && capabilities.CanTransferCrossGeneration,
                TradingAvailable = config.AllowTrading,
                HasCustomBackground = hasBackground
            });
        }
        catch (Exception ex)
        {
            return Ok(new PokemonIntegrationStatusDto
            {
                Enabled = true,
                Provider = config.ProviderType,
                ProviderHealthy = true,
                ProviderMessage = $"Native Pokémon Engine (Fallback: {ex.Message})",
                SupportedGenerations = ["1", "2", "3"],
                VaultAvailable = true,
                TransfersAvailable = config.AllowTransfers,
                CrossGenerationAvailable = config.AllowCrossGenerationTransfers,
                TradingAvailable = config.AllowTrading,
                HasCustomBackground = hasBackground
            });
        }
    }

    /// <summary>
    /// Serves or redirects to the custom Pokémon modal/vault background image if configured.
    /// </summary>
    [HttpGet("Background")]
    [AllowAnonymous]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status302Found)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public IActionResult GetBackground()
    {
        var rawPath = Configuration.ModalBackgroundPath;
        if (string.IsNullOrWhiteSpace(rawPath))
        {
            return NotFound();
        }

        var path = rawPath.Trim('\"', '\'').Trim();
        if (string.IsNullOrEmpty(path))
        {
            return NotFound();
        }

        if (path.StartsWith("http://", StringComparison.OrdinalIgnoreCase) ||
            path.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
        {
            return Redirect(path);
        }

        string? resolvedFile = null;
        if (System.IO.File.Exists(path))
        {
            resolvedFile = path;
        }
        else
        {
            var candidates = new List<string>
            {
                Path.Combine(Plugin.Instance?.DataFolderPath ?? string.Empty, path),
                Path.Combine(Plugin.Instance?.DataFolderPath ?? string.Empty, Path.GetFileName(path)),
                Path.Combine(Directory.GetCurrentDirectory(), path),
                Path.Combine(Directory.GetCurrentDirectory(), Path.GetFileName(path)),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), path),
            };

            resolvedFile = candidates.FirstOrDefault(System.IO.File.Exists);
        }

        if (resolvedFile != null)
        {
            var ext = Path.GetExtension(resolvedFile).ToLowerInvariant();
            var contentType = ext switch
            {
                ".jpg" or ".jpeg" => "image/jpeg",
                ".webp" => "image/webp",
                ".gif" => "image/gif",
                _ => "image/png"
            };
            return PhysicalFile(resolvedFile, contentType);
        }

        return NotFound();
    }

    /// <summary>
    /// Serves Pokémon cry audio stream (.ogg) for the given species ID.
    /// Downloads and caches on-demand on the server if not already present.
    /// </summary>
    [HttpGet("Cries/{speciesId}")]
    [AllowAnonymous]
    [Produces("audio/ogg")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetCry(
        [FromRoute] int speciesId,
        [FromQuery] string? style,
        CancellationToken cancellationToken)
    {
        if (_cryService == null || speciesId <= 0)
        {
            return NotFound();
        }

        var (stream, contentType, found) = await _cryService.GetCryStreamAsync(speciesId, style, cancellationToken).ConfigureAwait(false);
        if (!found || stream == null)
        {
            return NotFound();
        }

        return File(stream, contentType, enableRangeProcessing: true);
    }

    /// <summary>
    /// Retrieves the current authenticated user's Pokémon Vault summary (box count, total Pokémon count, shiny count).
    /// </summary>
    [HttpGet("Vault")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonVaultSummaryDto>> GetVaultSummary(CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var summary = await _vaultStore.GetVaultSummaryAsync(userId, cancellationToken).ConfigureAwait(false);
        return Ok(summary);
    }

    /// <summary>
    /// Retrieves a list of all vault boxes with occupancy count for the current user.
    /// </summary>
    [HttpGet("Vault/Boxes")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<IEnumerable<PokemonVaultBoxSummaryDto>>> GetBoxes(CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var boxes = await _vaultStore.GetBoxesSummaryAsync(userId, cancellationToken).ConfigureAwait(false);
        return Ok(boxes);
    }

    /// <summary>
    /// Retrieves the Pokémon entries inside a specific vault box (1-indexed) for the current user.
    /// </summary>
    [HttpGet("Vault/Boxes/{boxIndex:int}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonVaultBox>> GetBox(
        [FromRoute] int boxIndex,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var box = await _vaultStore.GetBoxAsync(userId, boxIndex, cancellationToken).ConfigureAwait(false);
        if (box == null)
        {
            return NotFound();
        }

        return Ok(box);
    }

    /// <summary>
    /// Sorts and compacts the Pokémon within a specified Vault box by Pokédex #, Level, Shiny, Name, or IVs.
    /// </summary>
    [HttpPost("Vault/Boxes/{boxIndex:int}/Sort")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonVaultBox>> SortVaultBox(
        [FromRoute] int boxIndex,
        [FromQuery] string criterion = "dex",
        [FromQuery] bool ascending = true,
        CancellationToken cancellationToken = default)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var sortedBox = await _vaultStore.SortBoxAsync(userId, boxIndex, criterion, ascending, cancellationToken).ConfigureAwait(false);
        if (sortedBox == null)
        {
            return NotFound();
        }

        return Ok(sortedBox);
    }

    /// <summary>
    /// Retrieves a specific Pokémon entry by its ID from the current user's vault.
    /// </summary>
    [HttpGet("Vault/Entries/{entryId}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonVaultEntry>> GetEntry(
        [FromRoute] string entryId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var entry = await _vaultStore.GetEntryAsync(userId, entryId, cancellationToken).ConfigureAwait(false);
        if (entry == null)
        {
            return NotFound();
        }

        entry.Details ??= new PokemonDetailsDto { Summary = entry.ToSummaryDto() };
        entry.Details.LearnableMoves ??= PokemonMovepoolProvider.GetLearnableMoves(entry.SpeciesId, entry.Species, entry.Level);
        entry.Details.Ribbons ??= PokemonRibbonCatalog.EvaluateRibbons(
            null,
            entry.Details.Ev,
            entry.Level,
            entry.Details.Friendship ?? 100,
            entry.OriginGame,
            entry.Generation,
            isInParty: false);
        entry.Details.IsHallOfFameMember = entry.Details.Ribbons.Any(r => r.Category.Equals("Champion", StringComparison.OrdinalIgnoreCase)) || entry.Level >= 55;
        entry.Details.AvailableEvolutions ??= PokemonEvolutionCatalog.GetAvailableEvolutions(entry.SpeciesId, entry.Species, entry.Level, entry.Details.HeldItem);
        return Ok(entry);
    }

    /// <summary>
    /// Updates or relearns active moves for a Pokémon in the user's personal vault.
    /// </summary>
    [HttpPut("Vault/Entries/{id}/Moves")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonVaultEntry>> UpdateVaultPokemonMoves(
        [FromRoute] string id,
        [FromBody] UpdatePokemonMovesRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var updated = await _vaultStore.UpdateEntryMovesAsync(userId, id, request.Moves, cancellationToken).ConfigureAwait(false);
        if (updated == null)
        {
            return NotFound();
        }

        return Ok(updated);
    }

    /// <summary>
    /// Triggers an in-vault cloud evolution on a Pokémon in the user's personal vault.
    /// </summary>
    [HttpPost("Vault/Entries/{id}/Evolve")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonVaultEntry>> EvolveVaultPokemon(
        [FromRoute] string id,
        [FromBody] EvolvePokemonRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var updated = await _vaultStore.EvolveEntryAsync(userId, id, request.TargetSpeciesId, cancellationToken).ConfigureAwait(false);
        if (updated == null)
        {
            return BadRequest(new { message = "Evolution requirements not met or species mismatch." });
        }

        return Ok(updated);
    }

    private GameDetail? FindGame(string gameId)
    {
        var libraries = _gamesService.GetGameLibraries();
        if (libraries.Count == 0)
        {
            var direct = _gamesService.GetGame(string.Empty, gameId);
            if (direct != null) return direct;
        }

        foreach (var library in libraries)
        {
            var game = _gamesService.GetGame(library.Id, gameId);
            if (game != null)
            {
                return game;
            }

            var summary = _gamesService.GetGames(library.Id).FirstOrDefault(g => string.Equals(g.Id, gameId, StringComparison.OrdinalIgnoreCase));
            if (summary != null)
            {
                return new GameDetail
                {
                    Id = summary.Id,
                    Title = summary.Title,
                    FileName = summary.FileName,
                    System = summary.System,
                    Core = summary.Core
                };
            }
        }
        return null;
    }

    /// <summary>
    /// Deposits a Pokémon from a player's save file into their personal vault.
    /// Safely performs extraction, pre-mutation backup, isolated save mutation, and personal vault addition.
    /// Automatically rolls back on any error.
    /// </summary>
    [HttpPost("Vault/Deposit")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonOperationResponse>> Deposit(
        [FromBody] PokemonDepositRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (string.IsNullOrWhiteSpace(request.GameId) || string.IsNullOrWhiteSpace(request.PokemonId))
        {
            return BadRequest(new { error = "GameId and PokemonId are required." });
        }

        if (request.TargetVaultBoxIndex < 1 || request.TargetVaultBoxIndex > PokemonVault.DefaultBoxCount)
        {
            return BadRequest(new { error = $"TargetVaultBoxIndex must be between 1 and {PokemonVault.DefaultBoxCount}." });
        }

        if (request.TargetVaultSlotIndex is < 1 or > PokemonVaultBox.DefaultCapacity)
        {
            return BadRequest(new { error = $"TargetVaultSlotIndex must be between 1 and {PokemonVaultBox.DefaultCapacity}." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);

        var game = FindGame(request.GameId);
        if (game == null)
        {
            return NotFound(new { error = $"Game '{request.GameId}' not found." });
        }

        var meta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: game.Id,
            title: game.Title,
            fileName: game.FileName,
            system: game.System,
            core: game.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        if (meta == null || !meta.IsPokemonGame)
        {
            return BadRequest(new { error = $"Game '{request.GameId}' is not a detected Pokémon game." });
        }

        var vaultBox = await _vaultStore.GetBoxAsync(userId, request.TargetVaultBoxIndex, cancellationToken).ConfigureAwait(false);
        var occupiedSlots = vaultBox?.Entries.Select(e => e.SlotIndex).ToHashSet() ?? [];

        int targetSlot;
        if (request.TargetVaultSlotIndex.HasValue)
        {
            if (occupiedSlots.Contains(request.TargetVaultSlotIndex.Value))
            {
                return BadRequest(new { error = $"Vault Box {request.TargetVaultBoxIndex} Slot {request.TargetVaultSlotIndex.Value} is already occupied." });
            }
            targetSlot = request.TargetVaultSlotIndex.Value;
        }
        else
        {
            var freeSlot = Enumerable.Range(1, PokemonVaultBox.DefaultCapacity).FirstOrDefault(s => !occupiedSlots.Contains(s));
            if (freeSlot == 0)
            {
                return BadRequest(new { error = $"Vault Box {request.TargetVaultBoxIndex} is full." });
            }
            targetSlot = freeSlot;
        }

        var config = Configuration;
        var provider = _providerFactory.Create(config);

        PokemonVaultEntry? addedEntry = null;

        var txResult = await _transactionManager.ExecuteAsync(
            userId: userId,
            operationType: "Deposit",
            sourceGameId: request.GameId,
            destinationGameId: null,
            pokemonId: request.PokemonId,
            species: null,
            nickname: null,
            operation: async context =>
            {
                var extractResult = await provider.ExtractPokemonFromSaveAsync(
                    context.SourceSaveWorkingCopy,
                    meta.PokemonGameId,
                    meta.Platform,
                    meta.Generation,
                    request.PokemonId,
                    request.IsInParty,
                    request.BoxIndex,
                    request.SlotIndex,
                    cancellationToken).ConfigureAwait(false);

                if (!extractResult.IsSuccess || extractResult.UpdatedSaveBytes == null || extractResult.ExtractedPokemon == null)
                {
                    throw new InvalidOperationException(extractResult.ErrorMessage ?? "Extraction failed.");
                }

                // Update save working copy with mutated bytes
                context.SourceSaveWorkingCopy = extractResult.UpdatedSaveBytes;

                var details = extractResult.ExtractedPokemon;
                var summary = details.Summary ?? new PokemonSummaryDto
                {
                    Id = request.PokemonId,
                    Species = "Unknown",
                    Nickname = "Unknown",
                    Level = 1,
                    OriginGame = meta.CanonicalTitle
                };

                addedEntry = new PokemonVaultEntry
                {
                    Id = string.IsNullOrWhiteSpace(request.PokemonId) ? Guid.NewGuid().ToString("N") : request.PokemonId,
                    BoxIndex = request.TargetVaultBoxIndex,
                    SlotIndex = targetSlot,
                    Species = summary.Species,
                    SpeciesId = summary.SpeciesId,
                    Form = summary.Form,
                    Nickname = string.IsNullOrWhiteSpace(summary.Nickname) ? summary.Species : summary.Nickname,
                    Level = summary.Level,
                    Gender = summary.Gender,
                    IsShiny = summary.IsShiny,
                    Generation = meta.Generation,
                    OriginalTrainer = summary.OriginalTrainer ?? string.Empty,
                    OriginalTrainerId = summary.OriginalTrainerId,
                    OriginGame = summary.OriginGame ?? meta.CanonicalTitle,
                    OriginGameId = meta.PokemonGameId,
                    CurrentLocation = $"Vault Box {request.TargetVaultBoxIndex}, Slot {targetSlot}",
                    CreatedAtUtc = DateTimeOffset.UtcNow,
                    UpdatedAtUtc = DateTimeOffset.UtcNow,
                    RawData = details.RawData,
                    Details = details,
                    LegalityStatus = details.LegalityStatus ?? "valid"
                };

                try
                {
                    await _vaultStore.AddOrUpdateEntryAsync(userId, addedEntry, cancellationToken).ConfigureAwait(false);
                    if (_journeyService != null && addedEntry != null)
                    {
                        try
                        {
                            await _journeyService.RecordEncounterAsync(userId, summary, meta.CanonicalTitle, isCaught: true, cancellationToken).ConfigureAwait(false);
                            await _journeyService.RecordJourneyStepAsync(userId, addedEntry.Id, new PokemonJourneyStepDto
                            {
                                Action = "Deposit",
                                SourceLocation = $"{meta.CanonicalTitle} ({(request.IsInParty ? $"Party Slot {request.SlotIndex}" : $"Box {request.BoxIndex ?? 1} Slot {request.SlotIndex}")})",
                                DestinationLocation = $"Personal Vault (Box {addedEntry.BoxIndex} Slot {addedEntry.SlotIndex})",
                                GameTitle = meta.CanonicalTitle,
                                Generation = meta.Generation,
                                Details = $"Deposited into Personal Vault Box {addedEntry.BoxIndex} Slot {addedEntry.SlotIndex}"
                            }, summary, cancellationToken).ConfigureAwait(false);
                        }
                        catch
                        {
                            // Journey tracking failure is non-fatal to save transaction
                        }
                    }
                }
                catch
                {
                    addedEntry = null;
                    throw;
                }

                return new PokemonOperationResponse
                {
                    Success = true,
                    TransactionId = context.TransactionId,
                    Operation = "Deposit",
                    Message = $"Successfully deposited {addedEntry?.Nickname ?? "Pokémon"} into Vault Box {addedEntry?.BoxIndex ?? request.TargetVaultBoxIndex} Slot {addedEntry?.SlotIndex ?? targetSlot}.",
                    SourceLocation = request.IsInParty ? $"Party Slot {request.SlotIndex}" : $"Box {request.BoxIndex ?? 1} Slot {request.SlotIndex}",
                    DestinationLocation = $"Vault Box {addedEntry?.BoxIndex ?? request.TargetVaultBoxIndex} Slot {addedEntry?.SlotIndex ?? targetSlot}",
                    BackupId = context.BackupIds.FirstOrDefault(),
                    VaultEntry = addedEntry,
                    Pokemon = addedEntry?.ToSummaryDto()
                };
            },
            cancellationToken).ConfigureAwait(false);

        if (!txResult.Success)
        {
            if (addedEntry != null)
            {
                try
                {
                    await _vaultStore.RemoveEntryAsync(userId, addedEntry.Id, CancellationToken.None).ConfigureAwait(false);
                }
                catch
                {
                    // Ignore cleanup failure during error reporting
                }
            }

            return BadRequest(new PokemonOperationResponse
            {
                Success = false,
                TransactionId = txResult.Record.TransactionId,
                Operation = "Deposit",
                Message = txResult.ErrorMessage ?? "Deposit transaction failed.",
                BackupId = txResult.Record.BackupIds.FirstOrDefault()
            });
        }

        if (_socialService != null && addedEntry != null)
        {
            try
            {
                var userName = await this.CurrentUserNameAsync(_authorizationContext).ConfigureAwait(false);
                var vaultSummary = await _vaultStore.GetVaultSummaryAsync(userId, cancellationToken).ConfigureAwait(false);
                await _socialService.ProcessPokemonDepositedAsync(userId, userName, addedEntry.ToSummaryDto(), vaultSummary.TotalCount, cancellationToken).ConfigureAwait(false);
            }
            catch
            {
                // Social logging is non-fatal
            }
        }

        return Ok(txResult.Value);
    }

    /// <summary>
    /// Copies every Pokémon from a Gen 6–9 emulator save into the current user's vault.
    /// The uploaded file is never written back or otherwise changed; PKVault's temporary parse copy is cleaned up after reading.
    /// </summary>
    [HttpPost("Vault/ImportSave")]
    [Consumes("multipart/form-data")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<ActionResult<PokemonExternalSaveImportResponse>> ImportExternalSave(
        [FromForm] IFormFile? saveFile,
        CancellationToken cancellationToken)
    {
        const long maxSaveBytes = 64L * 1024L * 1024L;
        if (!IsPokemonIntegrationEnabled()) return NotFound(new { error = "Pokémon integration is disabled." });
        if (saveFile == null || saveFile.Length == 0) return BadRequest(new { error = "Choose a non-empty emulator save file." });
        if (saveFile.Length > maxSaveBytes) return BadRequest(new { error = "Save files larger than 64 MB are not supported." });

        await using var input = saveFile.OpenReadStream();
        using var buffer = new MemoryStream((int)Math.Min(saveFile.Length, maxSaveBytes));
        await input.CopyToAsync(buffer, cancellationToken).ConfigureAwait(false);

        var provider = _providerFactory.Create(Configuration);
        var capabilities = await provider.GetCapabilitiesAsync(cancellationToken).ConfigureAwait(false);
        if (!capabilities.CanReadSaves || !capabilities.SupportedGenerations.Any(g => g is "6" or "7" or "8" or "9"))
        {
            return BadRequest(new { error = "A PKVault provider with Gen 6–9 support is required for external save import." });
        }

        // PKVault identifies the save itself. These placeholders are only used as safe origin labels.
        var parsed = await provider.ParseSaveAsync(buffer.ToArray(), "external-emulator-save", "external", 0, cancellationToken).ConfigureAwait(false);
        var generation = parsed.DetectedGeneration.GetValueOrDefault();
        if (!parsed.IsSuccess) return BadRequest(new { error = parsed.ErrorMessage ?? "The selected file is not a readable Pokémon save." });
        if (generation is < 6 or > 9) return BadRequest(new { error = "This importer accepts Gen 6–9 emulator saves only." });

        var sourcePokemon = parsed.Party.Concat(parsed.Boxes.SelectMany(b => b.Entries))
            .Where(p => p.SpeciesId > 0).ToList();
        if (sourcePokemon.Count == 0) return BadRequest(new { error = "No Pokémon were found in this save." });

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var vault = await _vaultStore.GetOrCreateVaultAsync(userId, cancellationToken).ConfigureAwait(false);
        var freeSlots = vault.Boxes.Sum(b => Math.Max(0, b.Capacity - b.Entries.Count));
        if (freeSlots < sourcePokemon.Count)
        {
            return BadRequest(new { error = $"Vault needs {sourcePokemon.Count} free slots, but only {freeSlots} are available." });
        }

        var now = DateTimeOffset.UtcNow;
        var slots = vault.Boxes.OrderBy(b => b.BoxIndex)
            .SelectMany(b => Enumerable.Range(1, b.Capacity).Where(slot => b.Entries.All(e => e.SlotIndex != slot)).Select(slot => (Box: b, Slot: slot)))
            .Take(sourcePokemon.Count).ToList();
        for (var i = 0; i < sourcePokemon.Count; i++)
        {
            var summary = sourcePokemon[i];
            parsed.Details.TryGetValue(summary.Id, out var details);
            var target = slots[i];
            target.Box.Entries.Add(new PokemonVaultEntry
            {
                Id = Guid.NewGuid().ToString("N"), BoxIndex = target.Box.BoxIndex, SlotIndex = target.Slot,
                Species = summary.Species, SpeciesId = summary.SpeciesId, Form = summary.Form,
                Nickname = string.IsNullOrWhiteSpace(summary.Nickname) ? summary.Species : summary.Nickname,
                Level = summary.Level, Gender = summary.Gender, IsShiny = summary.IsShiny, Generation = generation,
                OriginalTrainer = summary.OriginalTrainer ?? parsed.TrainerName ?? string.Empty,
                OriginalTrainerId = summary.OriginalTrainerId ?? parsed.TrainerId,
                OriginGame = summary.OriginGame ?? $"Imported Gen {generation} emulator save",
                OriginGameId = "external-import", CurrentLocation = $"Vault Box {target.Box.BoxIndex}, Slot {target.Slot}",
                CreatedAtUtc = now, UpdatedAtUtc = now, RawData = details?.RawData, Details = details,
                LegalityStatus = details?.LegalityStatus ?? summary.LegalityStatus
            });
        }
        await _vaultStore.SaveVaultAsync(userId, vault, cancellationToken).ConfigureAwait(false);

        if (_journeyService != null)
        {
            try
            {
                await _journeyService.RecordBatchEncountersAsync(userId, sourcePokemon, $"Imported Gen {generation} emulator save", true, cancellationToken).ConfigureAwait(false);
                if (parsed.CaughtSpeciesIds.Count > 0 || parsed.SeenSpeciesIds.Count > 0)
                    await _journeyService.RecordSpeciesIdsAsync(userId, parsed.CaughtSpeciesIds, parsed.SeenSpeciesIds, $"Imported Gen {generation} emulator save", false, cancellationToken).ConfigureAwait(false);
            }
            catch { /* Vault import succeeded; Pokédex sync will retry on the next vault refresh. */ }
        }

        return Ok(new PokemonExternalSaveImportResponse { Success = true, ImportedCount = sourcePokemon.Count, Generation = generation, TrainerName = parsed.TrainerName, Message = $"Imported {sourcePokemon.Count} Pokémon. Your original save was not changed." });
    }

    /// <summary>
    /// Withdraws a Pokémon from the user's personal vault back into a compatible game save file.
    /// Safely performs injection, pre-mutation backup, isolated save mutation, and personal vault removal.
    /// Automatically rolls back on any error.
    /// </summary>
    [HttpPost("Vault/Withdraw")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonOperationResponse>> Withdraw(
        [FromBody] PokemonWithdrawRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (string.IsNullOrWhiteSpace(request.GameId) || string.IsNullOrWhiteSpace(request.VaultEntryId))
        {
            return BadRequest(new { error = "GameId and VaultEntryId are required." });
        }

        if (request.TargetParty)
        {
            if (request.TargetSlotIndex is < 1 or > 6)
            {
                return BadRequest(new { error = "TargetSlotIndex for party must be between 1 and 6." });
            }
        }
        else
        {
            if (request.TargetBoxIndex is < 1 or > PokemonVault.DefaultBoxCount)
            {
                return BadRequest(new { error = $"TargetBoxIndex must be between 1 and {PokemonVault.DefaultBoxCount}." });
            }
            if (request.TargetSlotIndex is < 1 or > PokemonVaultBox.DefaultCapacity)
            {
                return BadRequest(new { error = $"TargetSlotIndex must be between 1 and {PokemonVaultBox.DefaultCapacity}." });
            }
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);

        var vaultEntry = await _vaultStore.GetEntryAsync(userId, request.VaultEntryId, cancellationToken).ConfigureAwait(false);
        if (vaultEntry == null)
        {
            return NotFound(new { error = $"Pokémon entry '{request.VaultEntryId}' not found in personal vault." });
        }

        var game = FindGame(request.GameId);
        if (game == null)
        {
            return NotFound(new { error = $"Game '{request.GameId}' not found." });
        }

        var meta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: game.Id,
            title: game.Title,
            fileName: game.FileName,
            system: game.System,
            core: game.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        if (meta == null || !meta.IsPokemonGame)
        {
            return BadRequest(new { error = $"Game '{request.GameId}' is not a detected Pokémon game." });
        }

        var config = Configuration;
        var provider = _providerFactory.Create(config);

        bool entryRemoved = false;

        var txResult = await _transactionManager.ExecuteAsync(
            userId: userId,
            operationType: "Withdraw",
            sourceGameId: request.GameId,
            destinationGameId: null,
            pokemonId: vaultEntry.Id,
            species: vaultEntry.Species,
            nickname: vaultEntry.Nickname,
            operation: async context =>
            {
                var injectResult = await provider.InjectPokemonIntoSaveAsync(
                    context.SourceSaveWorkingCopy,
                    meta.PokemonGameId,
                    meta.Platform,
                    meta.Generation,
                    vaultEntry,
                    request.TargetBoxIndex,
                    request.TargetSlotIndex,
                    request.TargetParty,
                    cancellationToken).ConfigureAwait(false);

                if (!injectResult.IsSuccess || injectResult.UpdatedSaveBytes == null)
                {
                    throw new InvalidOperationException(injectResult.ErrorMessage ?? "Injection failed.");
                }

                // Update save working copy with mutated bytes
                context.SourceSaveWorkingCopy = injectResult.UpdatedSaveBytes;

                // Remove entry from vault
                var removed = await _vaultStore.RemoveEntryAsync(userId, vaultEntry.Id, cancellationToken).ConfigureAwait(false);
                if (!removed)
                {
                    throw new InvalidOperationException("Failed to remove Pokémon from personal vault.");
                }
                entryRemoved = true;

                var destinationLocation = injectResult.AssignedLocation
                    ?? (request.TargetParty
                        ? $"Party (Slot {request.TargetSlotIndex ?? 1})"
                        : $"Box {request.TargetBoxIndex ?? 1} (Slot {request.TargetSlotIndex ?? 1})");

                if (_journeyService != null)
                {
                    try
                    {
                        var summary = new PokemonSummaryDto
                        {
                            Species = vaultEntry.Species,
                            SpeciesId = vaultEntry.SpeciesId,
                            Nickname = vaultEntry.Nickname,
                            Level = vaultEntry.Level,
                            IsShiny = vaultEntry.IsShiny,
                            OriginGame = vaultEntry.OriginGame,
                            OriginalTrainer = vaultEntry.OriginalTrainer
                        };
                        await _journeyService.RecordEncounterAsync(userId, summary, meta.CanonicalTitle, isCaught: true, cancellationToken).ConfigureAwait(false);
                        await _journeyService.RecordJourneyStepAsync(userId, vaultEntry.Id, new PokemonJourneyStepDto
                        {
                            Action = "Withdraw",
                            SourceLocation = $"Personal Vault (Box {vaultEntry.BoxIndex} Slot {vaultEntry.SlotIndex})",
                            DestinationLocation = $"{meta.CanonicalTitle} ({destinationLocation})",
                            GameTitle = meta.CanonicalTitle,
                            Generation = meta.Generation,
                            Details = $"Withdrawn to {meta.CanonicalTitle}"
                        }, summary, cancellationToken).ConfigureAwait(false);
                    }
                    catch
                    {
                        // Non-fatal
                    }
                }

                return new PokemonOperationResponse
                {
                    Success = true,
                    TransactionId = context.TransactionId,
                    Operation = "Withdraw",
                    Message = $"Successfully withdrew {vaultEntry.Nickname} to {destinationLocation}.",
                    SourceLocation = $"Vault Box {vaultEntry.BoxIndex} Slot {vaultEntry.SlotIndex}",
                    DestinationLocation = destinationLocation,
                    BackupId = context.BackupIds.FirstOrDefault(),
                    VaultEntry = vaultEntry,
                    Pokemon = vaultEntry.ToSummaryDto()
                };
            },
            cancellationToken).ConfigureAwait(false);

        if (!txResult.Success)
        {
            if (entryRemoved)
            {
                try
                {
                    await _vaultStore.AddOrUpdateEntryAsync(userId, vaultEntry, CancellationToken.None).ConfigureAwait(false);
                }
                catch
                {
                    // Ignore restore failure during error reporting
                }
            }

            return BadRequest(new PokemonOperationResponse
            {
                Success = false,
                TransactionId = txResult.Record.TransactionId,
                Operation = "Withdraw",
                Message = txResult.ErrorMessage ?? "Withdraw transaction failed.",
                BackupId = txResult.Record.BackupIds.FirstOrDefault()
            });
        }

        if (_socialService != null && vaultEntry != null)
        {
            try
            {
                var userName = await this.CurrentUserNameAsync(_authorizationContext).ConfigureAwait(false);
                await _socialService.ProcessPokemonWithdrawnAsync(userId, userName, vaultEntry.ToSummaryDto(), meta.CanonicalTitle, cancellationToken).ConfigureAwait(false);
            }
            catch
            {
                // Social logging is non-fatal
            }
        }

        return Ok(txResult.Value);
    }

    /// <summary>
    /// Validates transfer compatibility and cross-generation migration rules between two Pokémon game saves.
    /// Provides pre-flight compatibility evaluation for client user interfaces.
    /// </summary>
    [HttpPost("Transfers/Validate")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonTransferCompatibilityResult>> ValidateTransfer(
        [FromBody] PokemonTransferValidateRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (string.IsNullOrWhiteSpace(request.SourceGameId) || string.IsNullOrWhiteSpace(request.DestinationGameId))
        {
            return BadRequest(new { error = "SourceGameId and DestinationGameId are required." });
        }

        if (string.Equals(request.SourceGameId, request.DestinationGameId, StringComparison.OrdinalIgnoreCase))
        {
            return BadRequest(new { error = "Source and destination games must be different." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);

        var sourceGame = FindGame(request.SourceGameId);
        if (sourceGame == null)
        {
            return NotFound(new { error = $"Source game '{request.SourceGameId}' not found." });
        }

        var destGame = FindGame(request.DestinationGameId);
        if (destGame == null)
        {
            return NotFound(new { error = $"Destination game '{request.DestinationGameId}' not found." });
        }

        var sourceMeta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: sourceGame.Id,
            title: sourceGame.Title,
            fileName: sourceGame.FileName,
            system: sourceGame.System,
            core: sourceGame.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        var destMeta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: destGame.Id,
            title: destGame.Title,
            fileName: destGame.FileName,
            system: destGame.System,
            core: destGame.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        if (sourceMeta == null || !sourceMeta.IsPokemonGame)
        {
            return BadRequest(new { error = $"Source game '{request.SourceGameId}' is not a detected Pokémon game." });
        }

        if (destMeta == null || !destMeta.IsPokemonGame)
        {
            return BadRequest(new { error = $"Destination game '{request.DestinationGameId}' is not a detected Pokémon game." });
        }

        var result = PokemonMigrationValidator.ValidateTransfer(sourceMeta, destMeta, Configuration);
        return Ok(result);
    }

    /// <summary>
    /// Executes a direct game-to-game Pokémon transfer across compatible games or generations.
    /// Locks both saves, generates pre-mutation backups for both saves, executes atomic extraction
    /// and injection across in-memory working copies, commits on success, and guarantees full dual rollback on failure.
    /// </summary>
    [HttpPost("Transfers/Direct")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonOperationResponse>> DirectTransfer(
        [FromBody] PokemonDirectTransferRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (string.IsNullOrWhiteSpace(request.SourceGameId) || string.IsNullOrWhiteSpace(request.DestinationGameId) || string.IsNullOrWhiteSpace(request.PokemonId))
        {
            return BadRequest(new { error = "SourceGameId, DestinationGameId, and PokemonId are required." });
        }

        if (string.Equals(request.SourceGameId, request.DestinationGameId, StringComparison.OrdinalIgnoreCase))
        {
            return BadRequest(new { error = "Source and destination games must be different." });
        }

        if (request.TargetParty)
        {
            if (request.TargetSlotIndex is < 1 or > 6)
            {
                return BadRequest(new { error = "TargetSlotIndex for party must be between 1 and 6." });
            }
        }
        else
        {
            if (request.TargetBoxIndex is < 1 or > PokemonVault.DefaultBoxCount)
            {
                return BadRequest(new { error = $"TargetBoxIndex must be between 1 and {PokemonVault.DefaultBoxCount}." });
            }
            if (request.TargetSlotIndex is < 1 or > PokemonVaultBox.DefaultCapacity)
            {
                return BadRequest(new { error = $"TargetSlotIndex must be between 1 and {PokemonVaultBox.DefaultCapacity}." });
            }
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);

        var sourceGame = FindGame(request.SourceGameId);
        if (sourceGame == null)
        {
            return NotFound(new { error = $"Source game '{request.SourceGameId}' not found." });
        }

        var destGame = FindGame(request.DestinationGameId);
        if (destGame == null)
        {
            return NotFound(new { error = $"Destination game '{request.DestinationGameId}' not found." });
        }

        var sourceMeta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: sourceGame.Id,
            title: sourceGame.Title,
            fileName: sourceGame.FileName,
            system: sourceGame.System,
            core: sourceGame.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        var destMeta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: destGame.Id,
            title: destGame.Title,
            fileName: destGame.FileName,
            system: destGame.System,
            core: destGame.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        if (sourceMeta == null || !sourceMeta.IsPokemonGame)
        {
            return BadRequest(new { error = $"Source game '{request.SourceGameId}' is not a detected Pokémon game." });
        }

        if (destMeta == null || !destMeta.IsPokemonGame)
        {
            return BadRequest(new { error = $"Destination game '{request.DestinationGameId}' is not a detected Pokémon game." });
        }

        var preCompat = PokemonMigrationValidator.ValidateTransfer(sourceMeta, destMeta, Configuration);
        if (!preCompat.IsCompatible)
        {
            return BadRequest(new PokemonOperationResponse
            {
                Success = false,
                Operation = "DirectTransfer",
                Message = preCompat.Reason,
                SourceGameId = request.SourceGameId,
                DestinationGameId = request.DestinationGameId,
                IsCrossGeneration = preCompat.IsCrossGeneration,
                Warnings = preCompat.Warnings.Count > 0 ? preCompat.Warnings : null
            });
        }

        var config = Configuration;
        var provider = _providerFactory.Create(config);

        var opType = preCompat.IsCrossGeneration ? "CrossGenTransfer" : "DirectTransfer";

        var txResult = await _transactionManager.ExecuteAsync(
            userId: userId,
            operationType: opType,
            sourceGameId: request.SourceGameId,
            destinationGameId: request.DestinationGameId,
            pokemonId: request.PokemonId,
            species: null,
            nickname: null,
            operation: async context =>
            {
                if (context.DestinationSaveWorkingCopy == null)
                {
                    throw new InvalidOperationException($"Destination save working copy was not loaded for {request.DestinationGameId}.");
                }

                // 1. Extract from source save working copy
                var extractResult = await provider.ExtractPokemonFromSaveAsync(
                    context.SourceSaveWorkingCopy,
                    sourceMeta.PokemonGameId,
                    sourceMeta.Platform,
                    sourceMeta.Generation,
                    request.PokemonId,
                    request.SourceIsInParty,
                    request.SourceBoxIndex,
                    request.SourceSlotIndex,
                    cancellationToken).ConfigureAwait(false);

                if (!extractResult.IsSuccess || extractResult.UpdatedSaveBytes == null || extractResult.ExtractedPokemon == null)
                {
                    throw new InvalidOperationException(extractResult.ErrorMessage ?? "Extraction from source save failed.");
                }

                // 2. Perform detailed validation with extracted Pokémon metadata
                var postCompat = PokemonMigrationValidator.ValidateTransfer(sourceMeta, destMeta, config, extractResult.ExtractedPokemon);
                if (!postCompat.IsCompatible)
                {
                    throw new InvalidOperationException(postCompat.Reason);
                }

                // Update source save working copy
                context.SourceSaveWorkingCopy = extractResult.UpdatedSaveBytes;

                // 3. Prepare entry for injection into destination save
                var details = extractResult.ExtractedPokemon;
                var summary = details.Summary ?? new PokemonSummaryDto
                {
                    Id = request.PokemonId,
                    Species = "Unknown",
                    Nickname = "Unknown",
                    Level = 1,
                    OriginGame = sourceMeta.CanonicalTitle
                };

                var intermediateEntry = new PokemonVaultEntry
                {
                    Id = request.PokemonId,
                    BoxIndex = request.TargetBoxIndex ?? 1,
                    SlotIndex = request.TargetSlotIndex ?? 1,
                    Species = summary.Species,
                    SpeciesId = summary.SpeciesId,
                    Form = summary.Form,
                    Nickname = string.IsNullOrWhiteSpace(summary.Nickname) ? summary.Species : summary.Nickname,
                    Level = summary.Level,
                    Gender = summary.Gender,
                    IsShiny = summary.IsShiny,
                    Generation = sourceMeta.Generation,
                    OriginalTrainer = summary.OriginalTrainer ?? string.Empty,
                    OriginalTrainerId = summary.OriginalTrainerId,
                    OriginGame = summary.OriginGame ?? sourceMeta.CanonicalTitle,
                    OriginGameId = sourceMeta.PokemonGameId,
                    CurrentLocation = request.TargetParty ? "Party" : $"Box {request.TargetBoxIndex ?? 1}",
                    CreatedAtUtc = DateTimeOffset.UtcNow,
                    UpdatedAtUtc = DateTimeOffset.UtcNow,
                    RawData = details.RawData,
                    Details = details,
                    LegalityStatus = details.LegalityStatus ?? "valid"
                };

                // 4. Inject into destination save working copy
                var injectResult = await provider.InjectPokemonIntoSaveAsync(
                    context.DestinationSaveWorkingCopy,
                    destMeta.PokemonGameId,
                    destMeta.Platform,
                    destMeta.Generation,
                    intermediateEntry,
                    request.TargetBoxIndex,
                    request.TargetSlotIndex,
                    request.TargetParty,
                    cancellationToken).ConfigureAwait(false);

                if (!injectResult.IsSuccess || injectResult.UpdatedSaveBytes == null)
                {
                    throw new InvalidOperationException(injectResult.ErrorMessage ?? "Injection into destination save failed.");
                }

                // Update destination save working copy
                context.DestinationSaveWorkingCopy = injectResult.UpdatedSaveBytes;

                var destLocation = injectResult.AssignedLocation
                    ?? (request.TargetParty
                        ? $"Party (Slot {request.TargetSlotIndex ?? 1})"
                        : $"Box {request.TargetBoxIndex ?? 1} (Slot {request.TargetSlotIndex ?? 1})");

                var sourceLocation = request.SourceIsInParty
                    ? $"Party Slot {request.SourceSlotIndex}"
                    : $"Box {request.SourceBoxIndex ?? 1} Slot {request.SourceSlotIndex}";

                if (_journeyService != null)
                {
                    try
                    {
                        await _journeyService.RecordEncounterAsync(userId, summary, destMeta.CanonicalTitle, isCaught: true, cancellationToken).ConfigureAwait(false);
                        await _journeyService.RecordJourneyStepAsync(userId, request.PokemonId, new PokemonJourneyStepDto
                        {
                            Action = preCompat.IsCrossGeneration ? "CrossGenTransfer" : "Transfer",
                            SourceLocation = $"{sourceMeta.CanonicalTitle} ({sourceLocation})",
                            DestinationLocation = $"{destMeta.CanonicalTitle} ({destLocation})",
                            GameTitle = destMeta.CanonicalTitle,
                            Generation = destMeta.Generation,
                            Details = preCompat.IsCrossGeneration
                                ? $"Cross-generation migration ({preCompat.Direction}) from Gen {sourceMeta.Generation} to Gen {destMeta.Generation}"
                                : $"Direct transfer from {sourceMeta.CanonicalTitle} to {destMeta.CanonicalTitle}"
                        }, summary, cancellationToken).ConfigureAwait(false);
                    }
                    catch
                    {
                        // Non-fatal
                    }
                }

                return new PokemonOperationResponse
                {
                    Success = true,
                    TransactionId = context.TransactionId,
                    Operation = opType,
                    Message = $"Successfully transferred {intermediateEntry.Nickname} from {sourceMeta.CanonicalTitle} to {destMeta.CanonicalTitle} ({destLocation}).",
                    SourceLocation = $"{sourceMeta.CanonicalTitle} - {sourceLocation}",
                    DestinationLocation = $"{destMeta.CanonicalTitle} - {destLocation}",
                    SourceGameId = request.SourceGameId,
                    DestinationGameId = request.DestinationGameId,
                    IsCrossGeneration = preCompat.IsCrossGeneration,
                    Warnings = preCompat.Warnings.Count > 0 ? preCompat.Warnings : null,
                    BackupId = context.BackupIds.FirstOrDefault(),
                    Pokemon = intermediateEntry.ToSummaryDto()
                };
            },
            cancellationToken).ConfigureAwait(false);

        if (!txResult.Success)
        {
            return BadRequest(new PokemonOperationResponse
            {
                Success = false,
                TransactionId = txResult.Record.TransactionId,
                Operation = opType,
                Message = txResult.ErrorMessage ?? "Direct transfer transaction failed.",
                SourceGameId = request.SourceGameId,
                DestinationGameId = request.DestinationGameId,
                IsCrossGeneration = preCompat.IsCrossGeneration,
                BackupId = txResult.Record.BackupIds.FirstOrDefault()
            });
        }

        if (_socialService != null && txResult.Value?.Pokemon != null)
        {
            try
            {
                var userName = await this.CurrentUserNameAsync(_authorizationContext).ConfigureAwait(false);
                await _socialService.ProcessPokemonTransferredAsync(userId, userName, txResult.Value.Pokemon, sourceMeta.CanonicalTitle, destMeta.CanonicalTitle, preCompat.IsCrossGeneration, cancellationToken).ConfigureAwait(false);
            }
            catch
            {
                // Social logging is non-fatal
            }
        }

        return Ok(txResult.Value);
    }

    /// <summary>
    /// Retrieves the current user's Pokémon profile and preferences.
    /// </summary>
    [HttpGet("Profile")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonUserProfile>> GetProfile(CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var profile = await _vaultStore.GetUserProfileAsync(userId, cancellationToken).ConfigureAwait(false);
        return Ok(profile);
    }

    /// <summary>
    /// Updates the current user's Pokémon profile and preferences.
    /// </summary>
    [HttpPut("Profile")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonUserProfile>> UpdateProfile(
        [FromBody] PokemonUserProfile request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var updated = await _vaultStore.SaveUserProfileAsync(userId, request, cancellationToken).ConfigureAwait(false);
        return Ok(updated);
    }

    /// <summary>
    /// Reads and returns Pokémon data (trainer info, party, and storage boxes) from the user's battery save (.sram)
    /// without modifying the save file.
    /// </summary>
    [HttpGet("Games/{libraryId}/{gameId}/Save")]
    [HttpGet("Games/{gameId}/Save")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonGameSaveDto>> GetGameSave(
        [FromRoute] string? libraryId,
        [FromRoute] string gameId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = (!string.IsNullOrWhiteSpace(libraryId) ? _gamesService.GetGame(libraryId, gameId) : null) ?? FindGame(gameId);
        if (game == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var meta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: game.Id,
            title: game.Title,
            fileName: game.FileName,
            system: game.System,
            core: game.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        if (meta == null || !meta.IsPokemonGame)
        {
            return NotFound();
        }

        var saveBytes = await _gameSavesService.GetAsync(userId, game.Id, "sram", cancellationToken).ConfigureAwait(false);
        if (saveBytes == null || saveBytes.Length == 0)
        {
            return Ok(new PokemonGameSaveDto
            {
                GameId = game.Id,
                Title = game.Title,
                Platform = meta.Platform,
                Generation = meta.Generation,
                SaveFound = false,
                ProviderAvailable = true,
                ErrorMessage = "No battery save (.sram) found for this game."
            });
        }

        var config = Configuration;
        var provider = _providerFactory.Create(config);
        var parseResult = await provider.ParseSaveAsync(
            saveBytes,
            meta.PokemonGameId,
            meta.Platform,
            meta.Generation,
            cancellationToken).ConfigureAwait(false);

        if (!parseResult.IsSuccess)
        {
            return Ok(new PokemonGameSaveDto
            {
                GameId = game.Id,
                Title = game.Title,
                Platform = meta.Platform,
                Generation = meta.Generation,
                SaveFound = true,
                ProviderAvailable = false,
                ErrorMessage = parseResult.ErrorMessage
            });
        }

        var totalCount = parseResult.Party.Count + parseResult.Boxes.Sum(b => b.OccupiedCount);
        var shinyCount = parseResult.Party.Count(p => p.IsShiny) + parseResult.Boxes.Sum(b => b.Entries.Count(p => p.IsShiny));

        if (_journeyService != null)
        {
            try
            {
                var saveSummaries = parseResult.Party
                    .Concat(parseResult.Boxes.SelectMany(b => b.Entries))
                    .Where(p => p.SpeciesId > 0)
                    .ToList();

                if (saveSummaries.Count > 0)
                {
                    await _journeyService.RecordBatchEncountersAsync(userId, saveSummaries, meta.CanonicalTitle, isCaught: true, cancellationToken).ConfigureAwait(false);
                }

                if (parseResult.CaughtSpeciesIds.Count > 0 || parseResult.SeenSpeciesIds.Count > 0)
                {
                    await _journeyService.RecordSpeciesIdsAsync(userId, parseResult.CaughtSpeciesIds, parseResult.SeenSpeciesIds, meta.CanonicalTitle, replaceExisting: false, cancellationToken).ConfigureAwait(false);
                }
            }
            catch
            {
                // Non-fatal if journey recording encounters transient error
            }
        }

        return Ok(new PokemonGameSaveDto
        {
            GameId = game.Id,
            Title = game.Title,
            Platform = meta.Platform,
            Generation = meta.Generation,
            TrainerName = parseResult.TrainerName,
            TrainerId = parseResult.TrainerId,
            Money = parseResult.Money,
            PokedexSeen = parseResult.PokedexSeen,
            PokedexCaught = parseResult.PokedexCaught,
            CaughtSpeciesIds = parseResult.CaughtSpeciesIds,
            SeenSpeciesIds = parseResult.SeenSpeciesIds,
            SaveFound = true,
            ProviderAvailable = true,
            Party = parseResult.Party,
            Boxes = parseResult.Boxes,
            TotalPokemonCount = totalCount,
            ShinyCount = shinyCount
        });
    }

    /// <summary>
    /// Retrieves a single box from the game save.
    /// </summary>
    [HttpGet("Games/{libraryId}/{gameId}/Save/Boxes/{boxIndex:int}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonBoxDto>> GetGameSaveBox(
        [FromRoute] string libraryId,
        [FromRoute] string gameId,
        [FromRoute] int boxIndex,
        CancellationToken cancellationToken)
    {
        var saveResult = await GetGameSave(libraryId, gameId, cancellationToken).ConfigureAwait(false);
        if (saveResult.Result is not OkObjectResult ok || ok.Value is not PokemonGameSaveDto saveDto || !saveDto.SaveFound || !saveDto.ProviderAvailable)
        {
            return NotFound();
        }

        var box = saveDto.Boxes.FirstOrDefault(b => b.BoxIndex == boxIndex);
        if (box == null)
        {
            return NotFound();
        }

        return Ok(box);
    }

    /// <summary>
    /// Retrieves details for a specific Pokémon in the game save (party or box).
    /// </summary>
    [HttpGet("Games/{libraryId}/{gameId}/Save/Pokemon/{pokemonId}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonDetailsDto>> GetGameSavePokemon(
        [FromRoute] string libraryId,
        [FromRoute] string gameId,
        [FromRoute] string pokemonId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = (!string.IsNullOrWhiteSpace(libraryId) ? _gamesService.GetGame(libraryId, gameId) : null) ?? FindGame(gameId);
        if (game == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var meta = await _detector.GetMetadataAsync(
            userId: userId,
            gameId: game.Id,
            title: game.Title,
            fileName: game.FileName,
            system: game.System,
            core: game.Core,
            cancellationToken: cancellationToken).ConfigureAwait(false);

        if (meta == null || !meta.IsPokemonGame)
        {
            return NotFound();
        }

        var saveBytes = await _gameSavesService.GetAsync(userId, game.Id, "sram", cancellationToken).ConfigureAwait(false);
        if (saveBytes == null || saveBytes.Length == 0)
        {
            return NotFound();
        }

        var config = Configuration;
        var provider = _providerFactory.Create(config);
        var parseResult = await provider.ParseSaveAsync(
            saveBytes,
            meta.PokemonGameId,
            meta.Platform,
            meta.Generation,
            cancellationToken).ConfigureAwait(false);

        if (!parseResult.IsSuccess)
        {
            return NotFound();
        }

        if (parseResult.Details.TryGetValue(pokemonId, out var details))
        {
            return Ok(details);
        }

        var foundSummary = parseResult.Party.FirstOrDefault(p => string.Equals(p.Id, pokemonId, StringComparison.OrdinalIgnoreCase))
            ?? parseResult.Boxes.SelectMany(b => b.Entries).FirstOrDefault(p => string.Equals(p.Id, pokemonId, StringComparison.OrdinalIgnoreCase));

        if (foundSummary != null)
        {
            return Ok(new PokemonDetailsDto { Summary = foundSummary });
        }

        return NotFound();
    }

    /// <summary>
    /// Checks the operational safety and lock state of a game save (e.g. whether it is active or locked).
    /// </summary>
    [HttpGet("Games/{libraryId}/{gameId}/LockState")]
    [HttpGet("Games/{gameId}/LockState")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<SaveOperationStateInfo>> GetSaveLockState(
        [FromRoute] string? libraryId,
        [FromRoute] string gameId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = (!string.IsNullOrWhiteSpace(libraryId) ? _gamesService.GetGame(libraryId, gameId) : null) ?? FindGame(gameId);
        if (game == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var state = await _saveCoordinator.GetSaveStateAsync(userId, game.Id, cancellationToken).ConfigureAwait(false);
        return Ok(state);
    }

    /// <summary>
    /// Registers that the user has launched and is actively playing a Pokémon game.
    /// </summary>
    [HttpPost("Games/{libraryId}/{gameId}/Session/Start")]
    [HttpPost("Games/{gameId}/Session/Start")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<ActiveGameSession>> StartGameSession(
        [FromRoute] string? libraryId,
        [FromRoute] string gameId,
        [FromQuery] string? deviceId)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = (!string.IsNullOrWhiteSpace(libraryId) ? _gamesService.GetGame(libraryId, gameId) : null) ?? FindGame(gameId);
        if (game == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var session = _sessionTracker.StartSession(userId, game.Id, deviceId);
        return Ok(session);
    }

    /// <summary>
    /// Extends the active lease of a running game session.
    /// </summary>
    [HttpPost("Games/{libraryId}/{gameId}/Session/Heartbeat")]
    [HttpPost("Games/{gameId}/Session/Heartbeat")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult> HeartbeatGameSession(
        [FromRoute] string? libraryId,
        [FromRoute] string gameId)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = (!string.IsNullOrWhiteSpace(libraryId) ? _gamesService.GetGame(libraryId, gameId) : null) ?? FindGame(gameId);
        if (game == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var refreshed = _sessionTracker.HeartbeatSession(userId, game.Id);
        return refreshed ? Ok(new { success = true }) : NotFound(new { error = "No active session found." });
    }

    /// <summary>
    /// Notifies the server that the user has stopped playing and exited the game.
    /// </summary>
    [HttpPost("Games/{libraryId}/{gameId}/Session/End")]
    [HttpPost("Games/{gameId}/Session/End")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult> EndGameSession(
        [FromRoute] string? libraryId,
        [FromRoute] string gameId)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = (!string.IsNullOrWhiteSpace(libraryId) ? _gamesService.GetGame(libraryId, gameId) : null) ?? FindGame(gameId);
        if (game == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        _sessionTracker.EndSession(userId, game.Id);
        return Ok(new { success = true });
    }

    /// <summary>
    /// Lists all save backups created for the authenticated user.
    /// </summary>
    [HttpGet("Backups")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<IEnumerable<PokemonBackupDto>>> GetBackups(
        [FromQuery] string? gameId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return Ok(Array.Empty<PokemonBackupDto>());
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var backups = await _backupService.ListBackupsAsync(userId, gameId, cancellationToken).ConfigureAwait(false);
        return Ok(backups.Select(b => b.ToDto()));
    }

    /// <summary>
    /// Lists save backups for a specific game for the current user.
    /// </summary>
    [HttpGet("Games/{libraryId}/{gameId}/Backups")]
    [HttpGet("Games/{gameId}/Backups")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<IEnumerable<PokemonBackupDto>>> GetGameBackups(
        [FromRoute] string? libraryId,
        [FromRoute] string gameId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = (!string.IsNullOrWhiteSpace(libraryId) ? _gamesService.GetGame(libraryId, gameId) : null) ?? FindGame(gameId);
        if (game == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var backups = await _backupService.ListBackupsAsync(userId, game.Id, cancellationToken).ConfigureAwait(false);
        return Ok(backups.Select(b => b.ToDto()));
    }

    /// <summary>
    /// Restores a pre-mutation backup to the user's active game save.
    /// Fails safely if the game is actively running in an emulator session.
    /// </summary>
    [HttpPost("Backups/Restore")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<RestoreBackupResponse>> RestoreBackup(
        [FromBody] RestoreBackupRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (string.IsNullOrWhiteSpace(request.BackupId) || string.IsNullOrWhiteSpace(request.GameId))
        {
            return BadRequest(new RestoreBackupResponse { IsSuccess = false, Message = "BackupId and GameId are required." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);

        // Verify game session is not active
        if (_sessionTracker.IsGameActive(userId, request.GameId))
        {
            return Conflict(new RestoreBackupResponse
            {
                IsSuccess = false,
                Message = $"Cannot restore backup while game {request.GameId} is currently being played. Exit the emulator first.",
                GameId = request.GameId,
                BackupId = request.BackupId
            });
        }

        // Acquire lock before restore
        await using var lockHandle = await _saveCoordinator.AcquireLockAsync(userId, request.GameId, "Restore Backup", TimeSpan.FromSeconds(30), cancellationToken).ConfigureAwait(false);
        if (lockHandle == null)
        {
            return Conflict(new RestoreBackupResponse
            {
                IsSuccess = false,
                Message = $"Cannot restore backup: save for game {request.GameId} is currently locked by another operation.",
                GameId = request.GameId,
                BackupId = request.BackupId
            });
        }

        var success = await _backupService.RestoreBackupAsync(userId, request.BackupId, cancellationToken).ConfigureAwait(false);
        if (!success)
        {
            return BadRequest(new RestoreBackupResponse
            {
                IsSuccess = false,
                Message = $"Failed to restore backup {request.BackupId}. The backup file may be corrupt or missing.",
                GameId = request.GameId,
                BackupId = request.BackupId
            });
        }

        return Ok(new RestoreBackupResponse
        {
            IsSuccess = true,
            Message = $"Successfully restored save for game {request.GameId} from backup {request.BackupId}.",
            GameId = request.GameId,
            BackupId = request.BackupId
        });
    }

    /// <summary>
    /// Lists Pokémon operation and transfer audit history for the authenticated user.
    /// </summary>
    [HttpGet("History")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<IEnumerable<PokemonTransactionDto>>> GetHistory(CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return Ok(Array.Empty<PokemonTransactionDto>());
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var history = await _transactionManager.ListHistoryAsync(userId, cancellationToken).ConfigureAwait(false);
        return Ok(history.Select(h => h.ToDto()));
    }

    /// <summary>
    /// Retrieves a specific transaction audit record by ID.
    /// </summary>
    [HttpGet("History/{transactionId}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonTransactionDto>> GetTransaction(
        [FromRoute] string transactionId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var record = await _transactionManager.GetTransactionAsync(userId, transactionId, cancellationToken).ConfigureAwait(false);
        if (record == null)
        {
            return NotFound();
        }

        return Ok(record.ToDto());
    }

    /// <summary>
    /// Executes a simulated Pokémon save transaction to demonstrate and verify the transactional
    /// locking, pre-mutation backup, and automatic rollback framework.
    /// </summary>
    [HttpPost("Transactions/Simulate")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonTransactionSimulationResult>> SimulateTransaction(
        [FromBody] PokemonTransactionSimulationRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        if (string.IsNullOrWhiteSpace(request.GameId))
        {
            return BadRequest(new { error = "GameId is required." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var originalBytes = await _gameSavesService.GetAsync(userId, request.GameId, "sram", cancellationToken).ConfigureAwait(false);
        if (originalBytes == null || originalBytes.Length == 0)
        {
            return BadRequest(new { error = $"No save file found for game {request.GameId} to simulate against." });
        }

        var checksumBefore = Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(originalBytes)).ToLowerInvariant();

        var result = await _transactionManager.ExecuteAsync<string>(
            userId: userId,
            operationType: request.SimulatedOperation,
            sourceGameId: request.GameId,
            destinationGameId: null,
            pokemonId: "simulated-test",
            species: "TestMon",
            nickname: "Tester",
            operation: async context =>
            {
                // Mutate the working copy
                if (context.SourceSaveWorkingCopy.Length > 0)
                {
                    context.SourceSaveWorkingCopy[0] = (byte)(context.SourceSaveWorkingCopy[0] ^ 0xFF);
                }

                if (request.ShouldFail)
                {
                    throw new InvalidOperationException("Deliberate failure triggered to test automatic transaction rollback.");
                }

                return await Task.FromResult("Operation completed successfully.").ConfigureAwait(false);
            },
            cancellationToken: cancellationToken).ConfigureAwait(false);

        // Verify save state on disk after operation
        var bytesAfter = await _gameSavesService.GetAsync(userId, request.GameId, "sram", cancellationToken).ConfigureAwait(false);
        var checksumAfter = bytesAfter != null
            ? Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(bytesAfter)).ToLowerInvariant()
            : string.Empty;

        return Ok(new PokemonTransactionSimulationResult
        {
            Success = result.Success,
            TransactionId = result.Record.TransactionId,
            Status = result.Record.Status,
            Message = result.Success ? "Transaction succeeded and committed." : $"Transaction failed: {result.ErrorMessage}",
            SaveChecksumBefore = checksumBefore,
            SaveChecksumAfter = checksumAfter,
            RolledBack = result.Record.Status == PokemonTransactionStatus.RolledBack && checksumBefore == checksumAfter
        });
    }

    /// <summary>
    /// Creates a direct or link code trade offer.
    /// </summary>
    [HttpPost("Trades/Create")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonTradeOperationResponse>> CreateTrade(
        [FromBody] CreateTradeRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (_tradingService == null)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new PokemonTradeOperationResponse { IsSuccess = false, Message = "Trading service is not configured." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var userName = User.Identity?.Name ?? "Player";

        var result = await _tradingService.CreateTradeAsync(userId.ToString("N"), userName, request, cancellationToken).ConfigureAwait(false);
        if (!result.IsSuccess)
        {
            return BadRequest(result);
        }

        return Ok(result);
    }

    /// <summary>
    /// Joins a link trade room by PIN / link code.
    /// </summary>
    [HttpPost("Trades/Join")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonTradeOperationResponse>> JoinLinkTrade(
        [FromBody] JoinLinkTradeRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (_tradingService == null)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new PokemonTradeOperationResponse { IsSuccess = false, Message = "Trading service is not configured." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var userName = User.Identity?.Name ?? "Player";

        var result = await _tradingService.JoinLinkTradeAsync(userId.ToString("N"), userName, request, cancellationToken).ConfigureAwait(false);
        if (!result.IsSuccess)
        {
            return BadRequest(result);
        }

        return Ok(result);
    }

    /// <summary>
    /// Accepts a direct trade offer with a counter-offer, performing an atomic two-sided exchange.
    /// </summary>
    [HttpPost("Trades/Accept")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonTradeOperationResponse>> AcceptTrade(
        [FromBody] AcceptTradeRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (_tradingService == null)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new PokemonTradeOperationResponse { IsSuccess = false, Message = "Trading service is not configured." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var userName = User.Identity?.Name ?? "Player";

        var result = await _tradingService.AcceptTradeAsync(userId.ToString("N"), userName, request, cancellationToken).ConfigureAwait(false);
        if (!result.IsSuccess)
        {
            return BadRequest(result);
        }

        if (_socialService != null && result.TradeSession != null)
        {
            try
            {
                var s = result.TradeSession;
                if (s.InitiatorOffer != null && s.TargetOffer != null)
                {
                    var u1 = Guid.TryParse(s.InitiatorUserId, out var g1) ? g1 : Guid.Empty;
                    var u2 = Guid.TryParse(s.TargetUserId, out var g2) ? g2 : Guid.Empty;
                    await _socialService.ProcessTradeCompletedAsync(
                        u1,
                        s.InitiatorUserName,
                        u2,
                        s.TargetUserName ?? "Trainer",
                        s.InitiatorOffer.ToSummaryDto(),
                        s.TargetOffer.ToSummaryDto(),
                        cancellationToken).ConfigureAwait(false);
                }
            }
            catch
            {
                // Social logging is non-fatal
            }
        }

        return Ok(result);
    }

    /// <summary>
    /// Cancels a pending trade.
    /// </summary>
    [HttpPost("Trades/Cancel")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonTradeOperationResponse>> CancelTrade(
        [FromBody] CancelTradeRequest request,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound(new { error = "Pokémon integration is disabled." });
        }

        if (_tradingService == null)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new PokemonTradeOperationResponse { IsSuccess = false, Message = "Trading service is not configured." });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var result = await _tradingService.CancelTradeAsync(userId.ToString("N"), request, cancellationToken).ConfigureAwait(false);
        if (!result.IsSuccess)
        {
            return BadRequest(result);
        }

        return Ok(result);
    }

    /// <summary>
    /// Gets pending trade invitations / rooms for the current user.
    /// </summary>
    [HttpGet("Trades/Pending")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<IEnumerable<PokemonTradeSession>>> GetPendingTrades(CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled() || _tradingService == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var trades = await _tradingService.GetPendingTradesAsync(userId.ToString("N"), cancellationToken).ConfigureAwait(false);
        return Ok(trades);
    }

    /// <summary>
    /// Gets details of a specific trade session by its ID.
    /// </summary>
    [HttpGet("Trades/{tradeId}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonTradeSession>> GetTrade(
        [FromRoute] string tradeId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled() || _tradingService == null)
        {
            return NotFound();
        }

        var trade = await _tradingService.GetTradeAsync(tradeId, cancellationToken).ConfigureAwait(false);
        if (trade == null)
        {
            return NotFound();
        }

        return Ok(trade);
    }

    /// <summary>
    /// Gets trade history for the current user.
    /// </summary>
    [HttpGet("Trades/History")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<IEnumerable<PokemonTradeSession>>> GetTradeHistory(CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled() || _tradingService == null)
        {
            return NotFound();
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var history = await _tradingService.GetTradeHistoryAsync(userId.ToString("N"), cancellationToken).ConfigureAwait(false);
        return Ok(history);
    }

    /// <summary>
    /// Retrieves comprehensive operational diagnostics and health statistics for the Pokémon subsystem.
    /// </summary>
    [HttpGet("Diagnostics")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<PokemonDiagnosticsDto>> GetDiagnostics(CancellationToken cancellationToken)
    {
        var config = Configuration;
        var provider = _providerFactory.Create(config);
        var testResult = await provider.TestConnectionAsync(cancellationToken).ConfigureAwait(false);
        var caps = await provider.GetCapabilitiesAsync(cancellationToken).ConfigureAwait(false);

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var vaultSummary = await _vaultStore.GetVaultSummaryAsync(userId, cancellationToken).ConfigureAwait(false);
        var backups = await _backupService.ListBackupsAsync(userId, null, cancellationToken).ConfigureAwait(false);
        var activeSessions = _sessionTracker.GetActiveSessions(userId);
        var pendingTrades = _tradingService != null ? await _tradingService.GetPendingTradesAsync(userId.ToString("N"), cancellationToken).ConfigureAwait(false) : [];

        var diag = new PokemonDiagnosticsDto
        {
            Enabled = config.Enabled,
            ProviderType = config.ProviderType,
            ProviderHealthy = testResult.IsSuccess,
            ProviderMessage = testResult.Message,
            SupportedGenerations = caps.SupportedGenerations,
            TotalStoredPokemon = vaultSummary.TotalCount,
            TotalShinyPokemon = vaultSummary.ShinyCount,
            TotalBackups = backups.Count,
            ActiveSessions = activeSessions.Count,
            ActiveTrades = pendingTrades.Count
        };

        return Ok(diag);
    }

    /// <summary>
    /// Retrieves the current user's aggregated Pokédex and per-generation completion statistics.
    /// Safe fallback when disabled.
    /// </summary>
    [HttpGet("Pokedex")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<PokemonPokedexDto>> GetPokedex(CancellationToken cancellationToken)
    {
        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        if (!IsPokemonIntegrationEnabled() || _journeyService == null)
        {
            return Ok(new PokemonPokedexDto
            {
                UserId = userId,
                IsEnabled = false,
                TotalCaught = 0,
                TotalSeen = 0,
                TotalShinies = 0,
                GenerationProgress = new(),
                Entries = new()
            });
        }

        // Synchronize vault Pokémon to ensure Pokédex is up to date
        try
        {
            var vault = await _vaultStore.GetOrCreateVaultAsync(userId, cancellationToken).ConfigureAwait(false);
            await _journeyService.SynchronizeWithVaultAsync(userId, vault, cancellationToken).ConfigureAwait(false);
        }
        catch
        {
            // Non-fatal if vault synchronization encounters empty store
        }

        var pokedex = await _journeyService.GetUserPokedexAsync(userId, cancellationToken).ConfigureAwait(false);

        if (_socialService != null)
        {
            try
            {
                var userName = await this.CurrentUserNameAsync(_authorizationContext).ConfigureAwait(false);
                var gen1 = pokedex.GenerationProgress.FirstOrDefault(g => g.Generation == 1);
                await _socialService.ProcessPokedexMilestoneAsync(userId, userName, pokedex.TotalCaught, gen1?.CaughtCount ?? 0, cancellationToken).ConfigureAwait(false);
            }
            catch
            {
                // Non-fatal
            }
        }

        return Ok(pokedex);
    }

    /// <summary>
    /// Synchronizes external caught and seen species IDs into the user's Pokédex.
    /// </summary>
    [HttpPost("Pokedex/Sync")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<PokemonPokedexDto>> SyncPokedex(
        [FromBody] PokemonPokedexSyncRequest request,
        CancellationToken cancellationToken)
    {
        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        if (!IsPokemonIntegrationEnabled() || _journeyService == null)
        {
            return Ok(new PokemonPokedexDto { UserId = userId, IsEnabled = false });
        }

        if (request.CaughtSpeciesIds.Count > 0 || request.SeenSpeciesIds.Count > 0 || request.ReplaceExisting)
        {
            await _journeyService.RecordSpeciesIdsAsync(
                userId,
                request.CaughtSpeciesIds,
                request.SeenSpeciesIds,
                request.OriginGame,
                request.ReplaceExisting,
                cancellationToken).ConfigureAwait(false);
        }

        var pokedex = await _journeyService.GetUserPokedexAsync(userId, cancellationToken).ConfigureAwait(false);

        if (_socialService != null)
        {
            try
            {
                var userName = await this.CurrentUserNameAsync(_authorizationContext).ConfigureAwait(false);
                var gen1 = pokedex.GenerationProgress.FirstOrDefault(g => g.Generation == 1);
                await _socialService.ProcessPokedexMilestoneAsync(userId, userName, pokedex.TotalCaught, gen1?.CaughtCount ?? 0, cancellationToken).ConfigureAwait(false);
            }
            catch
            {
                // Non-fatal
            }
        }

        return Ok(pokedex);
    }

    /// <summary>
    /// Retrieves the lineage / journey history for a specific Pokémon.
    /// Safe fallback when disabled.
    /// </summary>
    [HttpGet("Journey/{pokemonId}")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<PokemonJourneyDto>> GetPokemonJourney([FromRoute] string pokemonId, CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled() || _journeyService == null)
        {
            return Ok(new PokemonJourneyDto { PokemonId = pokemonId, Steps = new() });
        }

        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        var journey = await _journeyService.GetPokemonJourneyAsync(userId, pokemonId, cancellationToken).ConfigureAwait(false);

        if (journey == null)
        {
            // If not directly logged in journeys yet, synthesize from vault if present
            var vault = await _vaultStore.GetOrCreateVaultAsync(userId, cancellationToken).ConfigureAwait(false);
            foreach (var box in vault.Boxes)
            {
                var entry = box.Entries.FirstOrDefault(e => string.Equals(e.Id, pokemonId, StringComparison.OrdinalIgnoreCase));
                if (entry != null)
                {
                    journey = new PokemonJourneyDto
                    {
                        PokemonId = entry.Id,
                        Species = entry.Species,
                        SpeciesId = entry.SpeciesId,
                        Nickname = entry.Nickname,
                        Level = entry.Level,
                        IsShiny = entry.IsShiny,
                        OriginGame = entry.OriginGame,
                        OriginalTrainer = entry.OriginalTrainer,
                        OriginalTrainerId = entry.OriginalTrainerId,
                        Steps = new List<PokemonJourneyStepDto>
                        {
                            new()
                            {
                                Timestamp = entry.CreatedAtUtc,
                                Action = "Deposit",
                                SourceLocation = entry.OriginGame ?? "Original Game",
                                DestinationLocation = $"Personal Vault (Box {entry.BoxIndex} Slot {entry.SlotIndex})",
                                Details = $"Stored in Personal Vault Box {entry.BoxIndex}"
                            }
                        }
                    };
                    break;
                }
            }
        }

        return Ok(journey ?? new PokemonJourneyDto { PokemonId = pokemonId, Steps = new() });
    }

    /// <summary>
    /// Retrieves user's Pokémon achievements and milestone completion summary.
    /// Safe fallback when disabled.
    /// </summary>
    [HttpGet("Achievements")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<PokemonAchievementsSummaryDto>> GetAchievements(CancellationToken cancellationToken)
    {
        var userId = await this.CurrentUserIdAsync(_authorizationContext).ConfigureAwait(false);
        if (!IsPokemonIntegrationEnabled() || _socialService == null)
        {
            return Ok(new PokemonAchievementsSummaryDto
            {
                UserId = userId,
                TotalScore = 0,
                UnlockedCount = 0,
                TotalCount = 0,
                Achievements = new()
            });
        }

        var summary = await _socialService.GetUserAchievementsAsync(userId, cancellationToken).ConfigureAwait(false);
        return Ok(summary);
    }

    /// <summary>
    /// Retrieves recent Pokémon social activity feed across the server.
    /// Safe fallback when disabled.
    /// </summary>
    [HttpGet("Social/Activity")]
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<PokemonSocialActivityEvent>>> GetSocialActivity(
        [FromQuery] int limit = 30,
        CancellationToken cancellationToken = default)
    {
        if (!IsPokemonIntegrationEnabled() || _socialService == null)
        {
            return Ok(Array.Empty<PokemonSocialActivityEvent>());
        }

        var events = await _socialService.GetRecentActivityAsync(limit, cancellationToken).ConfigureAwait(false);
        return Ok(events);
    }
}

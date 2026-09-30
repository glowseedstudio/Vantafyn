using System.Net.Mime;
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
    private readonly IAuthorizationContext _authorizationContext;
    private readonly PokemonConfiguration? _overrideConfig;

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
        IPokemonSocialService? socialService = null)
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
    }

    private PokemonConfiguration Configuration =>
        _overrideConfig ?? Plugin.Instance?.Configuration?.Pokemon ?? new PokemonConfiguration();

    private bool IsPokemonIntegrationEnabled() =>
        Configuration.Enabled;

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

        var game = _gamesService.GetGame(libraryId, gameId);
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
                TradingAvailable = false
            });
        }

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
            TradingAvailable = config.AllowTrading
        });
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

        return Ok(entry);
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
                    RawData = details.RawData
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
                    Message = $"Successfully deposited {addedEntry.Nickname} into Vault Box {addedEntry.BoxIndex} Slot {addedEntry.SlotIndex}.",
                    SourceLocation = request.IsInParty ? $"Party Slot {request.SlotIndex}" : $"Box {request.BoxIndex ?? 1} Slot {request.SlotIndex}",
                    DestinationLocation = $"Vault Box {addedEntry.BoxIndex} Slot {addedEntry.SlotIndex}",
                    BackupId = context.BackupIds.FirstOrDefault(),
                    VaultEntry = addedEntry,
                    Pokemon = addedEntry.ToSummaryDto()
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
                    RawData = details.RawData
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
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<PokemonGameSaveDto>> GetGameSave(
        [FromRoute] string libraryId,
        [FromRoute] string gameId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = _gamesService.GetGame(libraryId, gameId);
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

        var game = _gamesService.GetGame(libraryId, gameId);
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
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<SaveOperationStateInfo>> GetSaveLockState(
        [FromRoute] string libraryId,
        [FromRoute] string gameId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = _gamesService.GetGame(libraryId, gameId);
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
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<ActiveGameSession>> StartGameSession(
        [FromRoute] string libraryId,
        [FromRoute] string gameId,
        [FromQuery] string? deviceId)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = _gamesService.GetGame(libraryId, gameId);
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
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult> HeartbeatGameSession(
        [FromRoute] string libraryId,
        [FromRoute] string gameId)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = _gamesService.GetGame(libraryId, gameId);
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
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult> EndGameSession(
        [FromRoute] string libraryId,
        [FromRoute] string gameId)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = _gamesService.GetGame(libraryId, gameId);
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
    [Produces(MediaTypeNames.Application.Json)]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<IEnumerable<PokemonBackupDto>>> GetGameBackups(
        [FromRoute] string libraryId,
        [FromRoute] string gameId,
        CancellationToken cancellationToken)
    {
        if (!IsPokemonIntegrationEnabled())
        {
            return NotFound();
        }

        var game = _gamesService.GetGame(libraryId, gameId);
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



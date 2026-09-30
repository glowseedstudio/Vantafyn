using System.Text.Json;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Pokemon;

public sealed class FilePokemonTradingService : IPokemonTradingService
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = true
    };

    private readonly ICompanionPaths _paths;
    private readonly IPokemonVaultStore _vaultStore;
    private readonly IPokemonTransactionManager _transactionManager;
    private readonly ILogger<FilePokemonTradingService> _logger;
    private readonly SemaphoreSlim _lock = new(1, 1);

    public FilePokemonTradingService(
        ICompanionPaths paths,
        IPokemonVaultStore vaultStore,
        IPokemonTransactionManager transactionManager,
        ILogger<FilePokemonTradingService>? logger = null)
    {
        _paths = paths;
        _vaultStore = vaultStore;
        _transactionManager = transactionManager;
        _logger = logger ?? NullLogger<FilePokemonTradingService>.Instance;
    }

    private static Guid ParseUserGuid(string userId)
    {
        if (Guid.TryParse(userId, out var guid))
        {
            return guid;
        }
        using var md5 = System.Security.Cryptography.MD5.Create();
        var hash = md5.ComputeHash(System.Text.Encoding.UTF8.GetBytes(userId));
        return new Guid(hash);
    }

    private string TradesDirectory
    {
        get
        {
            var dir = Path.Combine(_paths.PokemonRoot, "trades");
            Directory.CreateDirectory(dir);
            return dir;
        }
    }

    private string GetTradeFilePath(string tradeId) =>
        Path.Combine(TradesDirectory, $"{tradeId}.json");

    public async Task<PokemonTradeOperationResponse> CreateTradeAsync(
        string userId,
        string userName,
        CreateTradeRequest request,
        CancellationToken cancellationToken = default)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(userId);
        ArgumentNullException.ThrowIfNull(request);
        ArgumentNullException.ThrowIfNull(request.Offer);

        await _lock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var userGuid = ParseUserGuid(userId);

            // Verify initiator owns the offered Pokemon
            if (request.Offer.IsVault)
            {
                var entry = await _vaultStore.GetEntryAsync(userGuid, request.Offer.PokemonId, cancellationToken).ConfigureAwait(false);
                if (entry == null)
                {
                    return new PokemonTradeOperationResponse
                    {
                        IsSuccess = false,
                        Message = $"Offered Pokémon '{request.Offer.PokemonId}' was not found in your Personal Vault."
                    };
                }

                // Populate offer metadata from actual vault entry
                request.Offer.Species = entry.Species;
                request.Offer.SpeciesId = entry.SpeciesId;
                request.Offer.Nickname = entry.Nickname;
                request.Offer.Level = entry.Level;
                request.Offer.IsShiny = entry.IsShiny;
                request.Offer.Generation = entry.Generation;
                request.Offer.OriginalTrainer = entry.OriginalTrainer;
                request.Offer.BoxIndex = entry.BoxIndex;
                request.Offer.SlotIndex = entry.SlotIndex;
            }

            request.Offer.UserId = userId;
            request.Offer.UserName = userName;

            var tradeType = !string.IsNullOrWhiteSpace(request.LinkCode)
                ? PokemonTradeType.LinkCode
                : PokemonTradeType.Direct;

            var session = new PokemonTradeSession
            {
                Id = Guid.NewGuid().ToString("N"),
                Type = tradeType,
                LinkCode = request.LinkCode?.Trim(),
                Status = PokemonTradeStatus.Pending,
                InitiatorUserId = userId,
                InitiatorUserName = userName,
                TargetUserId = request.TargetUserId,
                TargetUserName = request.TargetUserName,
                InitiatorOffer = request.Offer,
                CreatedAtUtc = DateTime.UtcNow
            };

            await SaveTradeSessionAsync(session, cancellationToken).ConfigureAwait(false);

            return new PokemonTradeOperationResponse
            {
                IsSuccess = true,
                Message = tradeType == PokemonTradeType.LinkCode
                    ? $"Link Trade room created with code '{session.LinkCode}'. Waiting for a partner..."
                    : $"Direct trade request sent to {request.TargetUserName ?? "partner"}.",
                TradeSession = session
            };
        }
        finally
        {
            _lock.Release();
        }
    }

    public async Task<PokemonTradeOperationResponse> JoinLinkTradeAsync(
        string userId,
        string userName,
        JoinLinkTradeRequest request,
        CancellationToken cancellationToken = default)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(userId);
        ArgumentNullException.ThrowIfNull(request);
        ArgumentException.ThrowIfNullOrWhiteSpace(request.LinkCode);
        ArgumentNullException.ThrowIfNull(request.Offer);

        await _lock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var userGuid = ParseUserGuid(userId);

            // Find active link code session
            var session = (await LoadAllTradesAsync(cancellationToken).ConfigureAwait(false))
                .FirstOrDefault(s => s.Type == PokemonTradeType.LinkCode &&
                                     s.Status == PokemonTradeStatus.Pending &&
                                     string.Equals(s.LinkCode, request.LinkCode.Trim(), StringComparison.OrdinalIgnoreCase) &&
                                     s.InitiatorUserId != userId);

            if (session == null)
            {
                return new PokemonTradeOperationResponse
                {
                    IsSuccess = false,
                    Message = $"No pending link trade room found for code '{request.LinkCode}'."
                };
            }

            // Verify partner owns their offered Pokémon
            if (request.Offer.IsVault)
            {
                var entry = await _vaultStore.GetEntryAsync(userGuid, request.Offer.PokemonId, cancellationToken).ConfigureAwait(false);
                if (entry == null)
                {
                    return new PokemonTradeOperationResponse
                    {
                        IsSuccess = false,
                        Message = $"Offered Pokémon '{request.Offer.PokemonId}' was not found in your Personal Vault."
                    };
                }

                request.Offer.Species = entry.Species;
                request.Offer.SpeciesId = entry.SpeciesId;
                request.Offer.Nickname = entry.Nickname;
                request.Offer.Level = entry.Level;
                request.Offer.IsShiny = entry.IsShiny;
                request.Offer.Generation = entry.Generation;
                request.Offer.OriginalTrainer = entry.OriginalTrainer;
                request.Offer.BoxIndex = entry.BoxIndex;
                request.Offer.SlotIndex = entry.SlotIndex;
            }

            request.Offer.UserId = userId;
            request.Offer.UserName = userName;

            session.TargetUserId = userId;
            session.TargetUserName = userName;
            session.TargetOffer = request.Offer;

            // Execute the atomic trade exchange!
            return await ExecuteAtomicTradeInternalAsync(session, cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            _lock.Release();
        }
    }

    public async Task<PokemonTradeOperationResponse> AcceptTradeAsync(
        string userId,
        string userName,
        AcceptTradeRequest request,
        CancellationToken cancellationToken = default)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(userId);
        ArgumentNullException.ThrowIfNull(request);
        ArgumentException.ThrowIfNullOrWhiteSpace(request.TradeId);
        ArgumentNullException.ThrowIfNull(request.Offer);

        await _lock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var userGuid = ParseUserGuid(userId);

            var session = await GetTradeAsync(request.TradeId, cancellationToken).ConfigureAwait(false);
            if (session == null)
            {
                return new PokemonTradeOperationResponse
                {
                    IsSuccess = false,
                    Message = $"Trade session '{request.TradeId}' not found."
                };
            }

            if (session.Status != PokemonTradeStatus.Pending)
            {
                return new PokemonTradeOperationResponse
                {
                    IsSuccess = false,
                    Message = $"Trade session is no longer pending (current status: {session.Status})."
                };
            }

            if (!string.IsNullOrWhiteSpace(session.TargetUserId) && session.TargetUserId != userId)
            {
                return new PokemonTradeOperationResponse
                {
                    IsSuccess = false,
                    Message = "You are not the designated recipient of this trade request."
                };
            }

            if (request.Offer.IsVault)
            {
                var entry = await _vaultStore.GetEntryAsync(userGuid, request.Offer.PokemonId, cancellationToken).ConfigureAwait(false);
                if (entry == null)
                {
                    return new PokemonTradeOperationResponse
                    {
                        IsSuccess = false,
                        Message = $"Offered Pokémon '{request.Offer.PokemonId}' was not found in your Personal Vault."
                    };
                }

                request.Offer.Species = entry.Species;
                request.Offer.SpeciesId = entry.SpeciesId;
                request.Offer.Nickname = entry.Nickname;
                request.Offer.Level = entry.Level;
                request.Offer.IsShiny = entry.IsShiny;
                request.Offer.Generation = entry.Generation;
                request.Offer.OriginalTrainer = entry.OriginalTrainer;
                request.Offer.BoxIndex = entry.BoxIndex;
                request.Offer.SlotIndex = entry.SlotIndex;
            }

            request.Offer.UserId = userId;
            request.Offer.UserName = userName;

            session.TargetUserId = userId;
            session.TargetUserName = userName;
            session.TargetOffer = request.Offer;

            // Execute the atomic trade exchange!
            return await ExecuteAtomicTradeInternalAsync(session, cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            _lock.Release();
        }
    }

    private async Task<PokemonTradeOperationResponse> ExecuteAtomicTradeInternalAsync(
        PokemonTradeSession session,
        CancellationToken cancellationToken)
    {
        var initiator = session.InitiatorOffer;
        var target = session.TargetOffer;

        if (target == null)
        {
            session.Status = PokemonTradeStatus.Failed;
            session.FailureReason = "Missing target offer.";
            await SaveTradeSessionAsync(session, cancellationToken).ConfigureAwait(false);
            return new PokemonTradeOperationResponse { IsSuccess = false, Message = session.FailureReason };
        }

        var guidA = ParseUserGuid(session.InitiatorUserId);
        var guidB = ParseUserGuid(session.TargetUserId!);

        var txId = $"trade_{session.Id}_{DateTime.UtcNow:yyyyMMddHHmmss}";

        try
        {
            // Currently personal vault to personal vault trading
            if (initiator.IsVault && target.IsVault)
            {
                var entryA = await _vaultStore.GetEntryAsync(guidA, initiator.PokemonId, cancellationToken).ConfigureAwait(false);
                var entryB = await _vaultStore.GetEntryAsync(guidB, target.PokemonId, cancellationToken).ConfigureAwait(false);

                if (entryA == null || entryB == null)
                {
                    throw new InvalidOperationException("One of the offered Pokémon is no longer available in the vault.");
                }

                // Remove entry A from User A
                var remA = await _vaultStore.RemoveEntryAsync(guidA, entryA.Id, cancellationToken).ConfigureAwait(false);
                if (!remA) throw new InvalidOperationException($"Failed to remove '{entryA.Species}' from {session.InitiatorUserName}'s vault.");

                // Remove entry B from User B
                var remB = await _vaultStore.RemoveEntryAsync(guidB, entryB.Id, cancellationToken).ConfigureAwait(false);
                if (!remB)
                {
                    // Rollback A
                    await _vaultStore.AddOrUpdateEntryAsync(guidA, entryA, cancellationToken).ConfigureAwait(false);
                    throw new InvalidOperationException($"Failed to remove '{entryB.Species}' from {session.TargetUserName}'s vault.");
                }

                // Add entry A to User B's vault
                var newEntryForB = new PokemonVaultEntry
                {
                    Id = Guid.NewGuid().ToString("N"),
                    Species = entryA.Species,
                    SpeciesId = entryA.SpeciesId,
                    Form = entryA.Form,
                    Nickname = entryA.Nickname,
                    Level = entryA.Level,
                    Gender = entryA.Gender,
                    IsShiny = entryA.IsShiny,
                    Generation = entryA.Generation,
                    OriginalTrainer = entryA.OriginalTrainer,
                    OriginalTrainerId = entryA.OriginalTrainerId,
                    OriginGame = entryA.OriginGame,
                    OriginGameId = entryA.OriginGameId,
                    CurrentLocation = $"Vault Box {target.TargetVaultBoxIndex ?? 1}",
                    CreatedAtUtc = DateTimeOffset.UtcNow,
                    UpdatedAtUtc = DateTimeOffset.UtcNow,
                    RawData = entryA.RawData,
                    BoxIndex = target.TargetVaultBoxIndex ?? 1,
                    SlotIndex = target.TargetVaultSlotIndex ?? 1,
                };

                // Add entry B to User A's vault
                var newEntryForA = new PokemonVaultEntry
                {
                    Id = Guid.NewGuid().ToString("N"),
                    Species = entryB.Species,
                    SpeciesId = entryB.SpeciesId,
                    Form = entryB.Form,
                    Nickname = entryB.Nickname,
                    Level = entryB.Level,
                    Gender = entryB.Gender,
                    IsShiny = entryB.IsShiny,
                    Generation = entryB.Generation,
                    OriginalTrainer = entryB.OriginalTrainer,
                    OriginalTrainerId = entryB.OriginalTrainerId,
                    OriginGame = entryB.OriginGame,
                    OriginGameId = entryB.OriginGameId,
                    CurrentLocation = $"Vault Box {initiator.TargetVaultBoxIndex ?? 1}",
                    CreatedAtUtc = DateTimeOffset.UtcNow,
                    UpdatedAtUtc = DateTimeOffset.UtcNow,
                    RawData = entryB.RawData,
                    BoxIndex = initiator.TargetVaultBoxIndex ?? 1,
                    SlotIndex = initiator.TargetVaultSlotIndex ?? 1,
                };

                try
                {
                    await _vaultStore.AddOrUpdateEntryAsync(guidB, newEntryForB, cancellationToken).ConfigureAwait(false);
                    await _vaultStore.AddOrUpdateEntryAsync(guidA, newEntryForA, cancellationToken).ConfigureAwait(false);
                }
                catch (Exception addEx)
                {
                    // Rollback both sides!
                    await _vaultStore.RemoveEntryAsync(guidB, newEntryForB.Id, cancellationToken).ConfigureAwait(false);
                    await _vaultStore.RemoveEntryAsync(guidA, newEntryForA.Id, cancellationToken).ConfigureAwait(false);
                    await _vaultStore.AddOrUpdateEntryAsync(guidA, entryA, cancellationToken).ConfigureAwait(false);
                    await _vaultStore.AddOrUpdateEntryAsync(guidB, entryB, cancellationToken).ConfigureAwait(false);
                    throw new InvalidOperationException($"Failed to commit Pokémon into target vaults: {addEx.Message}. Rolled back both sides.");
                }
            }

            session.Status = PokemonTradeStatus.Completed;
            session.CompletedAtUtc = DateTime.UtcNow;
            session.TransactionId = txId;

            await SaveTradeSessionAsync(session, cancellationToken).ConfigureAwait(false);

            _logger.LogInformation(
                "Trade {TradeId} successfully completed between {UserA} ({PokemonA}) and {UserB} ({PokemonB})",
                session.Id, session.InitiatorUserName, initiator.Species, session.TargetUserName, target.Species);

            return new PokemonTradeOperationResponse
            {
                IsSuccess = true,
                Message = $"Trade completed successfully! Exchanged {initiator.Species} with {session.TargetUserName}'s {target.Species}.",
                TradeSession = session,
                TransactionId = txId
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to complete trade {TradeId}", session.Id);
            session.Status = PokemonTradeStatus.Failed;
            session.FailureReason = ex.Message;
            await SaveTradeSessionAsync(session, cancellationToken).ConfigureAwait(false);

            return new PokemonTradeOperationResponse
            {
                IsSuccess = false,
                Message = $"Trade exchange failed: {ex.Message}",
                TradeSession = session
            };
        }
    }

    public async Task<PokemonTradeOperationResponse> CancelTradeAsync(
        string userId,
        CancelTradeRequest request,
        CancellationToken cancellationToken = default)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(userId);
        ArgumentNullException.ThrowIfNull(request);

        await _lock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var session = await GetTradeAsync(request.TradeId, cancellationToken).ConfigureAwait(false);
            if (session == null)
            {
                return new PokemonTradeOperationResponse { IsSuccess = false, Message = "Trade session not found." };
            }

            if (session.Status != PokemonTradeStatus.Pending)
            {
                return new PokemonTradeOperationResponse { IsSuccess = false, Message = "Only pending trade requests can be cancelled." };
            }

            if (session.InitiatorUserId != userId && session.TargetUserId != userId)
            {
                return new PokemonTradeOperationResponse { IsSuccess = false, Message = "You are not a participant in this trade." };
            }

            session.Status = PokemonTradeStatus.Cancelled;
            session.FailureReason = request.Reason ?? "Cancelled by participant.";
            await SaveTradeSessionAsync(session, cancellationToken).ConfigureAwait(false);

            return new PokemonTradeOperationResponse
            {
                IsSuccess = true,
                Message = "Trade request cancelled.",
                TradeSession = session
            };
        }
        finally
        {
            _lock.Release();
        }
    }

    public async Task<IReadOnlyList<PokemonTradeSession>> GetPendingTradesAsync(
        string userId,
        CancellationToken cancellationToken = default)
    {
        var trades = await LoadAllTradesAsync(cancellationToken).ConfigureAwait(false);
        return trades
            .Where(t => t.Status == PokemonTradeStatus.Pending &&
                        (t.TargetUserId == userId || (t.Type == PokemonTradeType.LinkCode && t.InitiatorUserId != userId)))
            .OrderByDescending(t => t.CreatedAtUtc)
            .ToList();
    }

    public async Task<PokemonTradeSession?> GetTradeAsync(
        string tradeId,
        CancellationToken cancellationToken = default)
    {
        var filePath = GetTradeFilePath(tradeId);
        if (!File.Exists(filePath)) return null;

        var json = await File.ReadAllTextAsync(filePath, cancellationToken).ConfigureAwait(false);
        return JsonSerializer.Deserialize<PokemonTradeSession>(json, JsonOptions);
    }

    public async Task<IReadOnlyList<PokemonTradeSession>> GetTradeHistoryAsync(
        string userId,
        CancellationToken cancellationToken = default)
    {
        var trades = await LoadAllTradesAsync(cancellationToken).ConfigureAwait(false);
        return trades
            .Where(t => t.InitiatorUserId == userId || t.TargetUserId == userId)
            .OrderByDescending(t => t.CompletedAtUtc ?? t.CreatedAtUtc)
            .ToList();
    }

    private async Task<List<PokemonTradeSession>> LoadAllTradesAsync(CancellationToken cancellationToken)
    {
        var files = Directory.GetFiles(TradesDirectory, "*.json");
        var list = new List<PokemonTradeSession>();

        foreach (var file in files)
        {
            try
            {
                var json = await File.ReadAllTextAsync(file, cancellationToken).ConfigureAwait(false);
                var session = JsonSerializer.Deserialize<PokemonTradeSession>(json, JsonOptions);
                if (session != null)
                {
                    list.Add(session);
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to parse trade session file {File}", file);
            }
        }

        return list;
    }

    private async Task SaveTradeSessionAsync(PokemonTradeSession session, CancellationToken cancellationToken)
    {
        var filePath = GetTradeFilePath(session.Id);
        var json = JsonSerializer.Serialize(session, JsonOptions);
        await File.WriteAllTextAsync(filePath, json, cancellationToken).ConfigureAwait(false);
    }
}

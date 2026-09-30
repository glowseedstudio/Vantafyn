using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum PokemonTradeStatus
{
    Pending,
    Active,
    Completed,
    Cancelled,
    Rejected,
    Failed
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum PokemonTradeType
{
    Direct,
    LinkCode
}

public sealed class PokemonTradeOffer
{
    public string UserId { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public bool IsVault { get; set; } = true;
    public string? GameId { get; set; }
    public string PokemonId { get; set; } = string.Empty;
    public string Species { get; set; } = string.Empty;
    public int SpeciesId { get; set; }
    public string Nickname { get; set; } = string.Empty;
    public int Level { get; set; } = 1;
    public bool IsShiny { get; set; }
    public int Generation { get; set; } = 1;
    public string? OriginalTrainer { get; set; }
    public int? BoxIndex { get; set; }
    public int SlotIndex { get; set; } = 1;
    public bool IsInParty { get; set; }
    public int? TargetVaultBoxIndex { get; set; }
    public int? TargetVaultSlotIndex { get; set; }

    public PokemonSummaryDto ToSummaryDto() => new()
    {
        Id = PokemonId,
        Species = Species,
        SpeciesId = SpeciesId,
        Nickname = string.IsNullOrWhiteSpace(Nickname) ? Species : Nickname,
        Level = Level,
        IsShiny = IsShiny,
        OriginalTrainer = OriginalTrainer
    };
}

public sealed class PokemonTradeSession
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public PokemonTradeType Type { get; set; } = PokemonTradeType.Direct;
    public string? LinkCode { get; set; }
    public PokemonTradeStatus Status { get; set; } = PokemonTradeStatus.Pending;
    public string InitiatorUserId { get; set; } = string.Empty;
    public string InitiatorUserName { get; set; } = string.Empty;
    public string? TargetUserId { get; set; }
    public string? TargetUserName { get; set; }
    public PokemonTradeOffer InitiatorOffer { get; set; } = new();
    public PokemonTradeOffer? TargetOffer { get; set; }
    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
    public DateTime? CompletedAtUtc { get; set; }
    public string? TransactionId { get; set; }
    public string? FailureReason { get; set; }
}

public sealed class CreateTradeRequest
{
    public string? TargetUserId { get; set; }
    public string? TargetUserName { get; set; }
    public string? LinkCode { get; set; }
    public PokemonTradeOffer Offer { get; set; } = new();
}

public sealed class JoinLinkTradeRequest
{
    public string LinkCode { get; set; } = string.Empty;
    public PokemonTradeOffer Offer { get; set; } = new();
}

public sealed class AcceptTradeRequest
{
    public string TradeId { get; set; } = string.Empty;
    public PokemonTradeOffer Offer { get; set; } = new();
}

public sealed class CancelTradeRequest
{
    public string TradeId { get; set; } = string.Empty;
    public string? Reason { get; set; }
}

public sealed class PokemonTradeOperationResponse
{
    public bool IsSuccess { get; set; }
    public string Message { get; set; } = string.Empty;
    public PokemonTradeSession? TradeSession { get; set; }
    public string? TransactionId { get; set; }
}

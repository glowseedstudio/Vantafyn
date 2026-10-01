using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Stat values (IVs or EVs).
/// </summary>
public sealed class PokemonStatsDto
{
    [JsonPropertyName("hp")]
    public int Hp { get; set; }

    [JsonPropertyName("attack")]
    public int Attack { get; set; }

    [JsonPropertyName("defense")]
    public int Defense { get; set; }

    [JsonPropertyName("specialAttack")]
    public int SpecialAttack { get; set; }

    [JsonPropertyName("specialDefense")]
    public int SpecialDefense { get; set; }

    [JsonPropertyName("speed")]
    public int Speed { get; set; }
}

/// <summary>
/// High-level summary of a Pokémon in a save file or party.
/// </summary>
public sealed class PokemonSummaryDto
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("species")]
    public string Species { get; set; } = string.Empty;

    [JsonPropertyName("speciesId")]
    public int SpeciesId { get; set; }

    [JsonPropertyName("form")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Form { get; set; }

    [JsonPropertyName("nickname")]
    public string Nickname { get; set; } = string.Empty;

    [JsonPropertyName("level")]
    public int Level { get; set; } = 1;

    [JsonPropertyName("gender")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Gender { get; set; }

    [JsonPropertyName("isShiny")]
    public bool IsShiny { get; set; }

    [JsonPropertyName("originalTrainer")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? OriginalTrainer { get; set; }

    [JsonPropertyName("originalTrainerId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? OriginalTrainerId { get; set; }

    [JsonPropertyName("originGame")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? OriginGame { get; set; }

    [JsonPropertyName("currentGame")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? CurrentGame { get; set; }

    [JsonPropertyName("currentLocation")]
    public string CurrentLocation { get; set; } = string.Empty;

    [JsonPropertyName("boxIndex")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? BoxIndex { get; set; }

    [JsonPropertyName("slotIndex")]
    public int SlotIndex { get; set; } = 1;

    [JsonPropertyName("isInParty")]
    public bool IsInParty { get; set; }

    [JsonPropertyName("legalityStatus")]
    public string LegalityStatus { get; set; } = "valid";

    [JsonPropertyName("isHallOfFameMember")]
    public bool IsHallOfFameMember { get; set; }
}

/// <summary>
/// Full details for a single Pokémon in a save file or vault.
/// </summary>
public sealed class PokemonDetailsDto
{
    [JsonPropertyName("summary")]
    public PokemonSummaryDto Summary { get; set; } = new();

    [JsonPropertyName("nature")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Nature { get; set; }

    [JsonPropertyName("ability")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Ability { get; set; }

    [JsonPropertyName("heldItem")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? HeldItem { get; set; }

    [JsonPropertyName("moves")]
    public IReadOnlyList<string> Moves { get; set; } = Array.Empty<string>();

    [JsonPropertyName("iv")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public PokemonStatsDto? Iv { get; set; }

    [JsonPropertyName("ev")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public PokemonStatsDto? Ev { get; set; }

    [JsonPropertyName("currentHp")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? CurrentHp { get; set; }

    [JsonPropertyName("maxHp")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? MaxHp { get; set; }

    [JsonPropertyName("friendship")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? Friendship { get; set; }

    [JsonPropertyName("pokeball")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Pokeball { get; set; }

    [JsonPropertyName("rawData")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? RawData { get; set; }

    [JsonPropertyName("legalityStatus")]
    public string LegalityStatus { get; set; } = "valid";

    [JsonPropertyName("legalityReport")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? LegalityReport { get; set; }

    [JsonPropertyName("illegalitiesCount")]
    public int IllegalitiesCount { get; set; }

    [JsonPropertyName("movesLegality")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public IReadOnlyList<bool>? MovesLegality { get; set; }

    [JsonPropertyName("learnableMoves")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public IReadOnlyList<PokemonLearnableMoveDto>? LearnableMoves { get; set; }

    [JsonPropertyName("ribbons")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public IReadOnlyList<PokemonRibbonDto>? Ribbons { get; set; }

    [JsonPropertyName("isHallOfFameMember")]
    public bool IsHallOfFameMember { get; set; }

    [JsonPropertyName("availableEvolutions")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public IReadOnlyList<PokemonEvolutionOptionDto>? AvailableEvolutions { get; set; }
}

/// <summary>
/// A move that a Pokémon can learn or relearn via level-up, TM/HM, tutor, or egg moves.
/// </summary>
public sealed class PokemonLearnableMoveDto
{
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("type")]
    public string Type { get; set; } = "Normal";

    [JsonPropertyName("category")]
    public string Category { get; set; } = "Physical"; // Physical, Special, Status

    [JsonPropertyName("power")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? Power { get; set; }

    [JsonPropertyName("accuracy")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? Accuracy { get; set; }

    [JsonPropertyName("pp")]
    public int Pp { get; set; } = 20;

    [JsonPropertyName("learnMethod")]
    public string LearnMethod { get; set; } = "Level Up"; // Level Up, Machine, Tutor, Egg

    [JsonPropertyName("levelLearned")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? LevelLearned { get; set; }

    [JsonPropertyName("description")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Description { get; set; }
}

/// <summary>
/// Request payload to update or relearn active moves on a Pokémon in the Vault.
/// </summary>
public sealed class UpdatePokemonMovesRequest
{
    [JsonPropertyName("moves")]
    public List<string> Moves { get; set; } = [];
}

/// <summary>
/// Request payload to trigger in-vault evolution on a Pokémon.
/// </summary>
public sealed class EvolvePokemonRequest
{
    [JsonPropertyName("targetSpeciesId")]
    public int TargetSpeciesId { get; set; }
}

/// <summary>
/// A box within a game's PC storage system.
/// </summary>
public sealed class PokemonBoxDto
{
    [JsonPropertyName("boxIndex")]
    public int BoxIndex { get; set; } = 1;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("capacity")]
    public int Capacity { get; set; } = 30;

    [JsonPropertyName("occupiedCount")]
    public int OccupiedCount { get; set; }

    [JsonPropertyName("entries")]
    public IReadOnlyList<PokemonSummaryDto> Entries { get; set; } = Array.Empty<PokemonSummaryDto>();
}

/// <summary>
/// Representation of a parsed Pokémon battery save (.sram).
/// </summary>
public sealed class PokemonGameSaveDto
{
    [JsonPropertyName("gameId")]
    public string GameId { get; set; } = string.Empty;

    [JsonPropertyName("title")]
    public string Title { get; set; } = string.Empty;

    [JsonPropertyName("platform")]
    public string Platform { get; set; } = string.Empty;

    [JsonPropertyName("generation")]
    public int Generation { get; set; }

    [JsonPropertyName("trainerName")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? TrainerName { get; set; }

    [JsonPropertyName("trainerId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? TrainerId { get; set; }

    [JsonPropertyName("money")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? Money { get; set; }

    [JsonPropertyName("pokedexSeen")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? PokedexSeen { get; set; }

    [JsonPropertyName("pokedexCaught")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? PokedexCaught { get; set; }

    [JsonPropertyName("saveFound")]
    public bool SaveFound { get; set; }

    [JsonPropertyName("providerAvailable")]
    public bool ProviderAvailable { get; set; }

    [JsonPropertyName("errorMessage")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? ErrorMessage { get; set; }

    [JsonPropertyName("party")]
    public IReadOnlyList<PokemonSummaryDto> Party { get; set; } = Array.Empty<PokemonSummaryDto>();

    [JsonPropertyName("boxes")]
    public IReadOnlyList<PokemonBoxDto> Boxes { get; set; } = Array.Empty<PokemonBoxDto>();

    [JsonPropertyName("totalPokemonCount")]
    public int TotalPokemonCount { get; set; }

    [JsonPropertyName("shinyCount")]
    public int ShinyCount { get; set; }
}

/// <summary>
/// Intermediate result returned by IPokemonProvider after parsing save bytes.
/// </summary>
public sealed class PokemonSaveParseResult
{
    [JsonPropertyName("isSuccess")]
    public bool IsSuccess { get; set; }

    [JsonPropertyName("errorMessage")]
    public string? ErrorMessage { get; set; }

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
    public List<PokemonSummaryDto> Party { get; set; } = [];

    [JsonPropertyName("boxes")]
    public List<PokemonBoxDto> Boxes { get; set; } = [];

    [JsonPropertyName("details")]
    public Dictionary<string, PokemonDetailsDto> Details { get; set; } = [];
}

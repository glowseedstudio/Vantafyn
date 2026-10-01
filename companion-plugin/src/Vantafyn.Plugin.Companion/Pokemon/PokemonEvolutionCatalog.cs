namespace Vantafyn.Plugin.Companion.Pokemon;

using System.Text.Json.Serialization;

/// <summary>
/// Defines an evolution path for a Pokémon.
/// </summary>
public sealed class PokemonEvolutionOptionDto
{
    [JsonPropertyName("targetSpeciesId")]
    public int TargetSpeciesId { get; set; }

    [JsonPropertyName("targetSpecies")]
    public string TargetSpecies { get; set; } = string.Empty;

    [JsonPropertyName("triggerMethod")]
    public string TriggerMethod { get; set; } = "Trade"; // Trade, Item, Level Up, Friendship

    [JsonPropertyName("requiredItem")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? RequiredItem { get; set; }

    [JsonPropertyName("requiredLevel")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? RequiredLevel { get; set; }

    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    [JsonPropertyName("canEvolveNow")]
    public bool CanEvolveNow { get; set; } = true;
}

/// <summary>
/// Master repository of authentic mainline Pokémon evolutions, specifically
/// trade evolutions, item evolutions, and level evolutions executable in Cloud Storage.
/// </summary>
public static class PokemonEvolutionCatalog
{
    private static readonly Dictionary<int, List<EvolutionDefinition>> EvolutionsBySpecies = new()
    {
        // Generation 1 Trade Evolutions
        [64] = [new(65, "Alakazam", "Trade", null, null, "Evolves by trade into Alakazam.")], // Kadabra -> Alakazam
        [67] = [new(68, "Machamp", "Trade", null, null, "Evolves by trade into Machamp.")], // Machoke -> Machamp
        [75] = [new(76, "Golem", "Trade", null, null, "Evolves by trade into Golem.")], // Graveler -> Golem
        [93] = [new(94, "Gengar", "Trade", null, null, "Evolves by trade into Gengar.")], // Haunter -> Gengar

        // Generation 2 Trade + Item Evolutions
        [61] = [new(186, "Politoed", "Trade + Item", "King's Rock", null, "Evolves by trading while holding King's Rock.")], // Poliwhirl -> Politoed
        [79] = [new(199, "Slowking", "Trade + Item", "King's Rock", null, "Evolves by trading while holding King's Rock.")], // Slowpoke -> Slowking
        [95] = [new(208, "Steelix", "Trade + Item", "Metal Coat", null, "Evolves by trading while holding Metal Coat.")], // Onix -> Steelix
        [117] = [new(230, "Kingdra", "Trade + Item", "Dragon Scale", null, "Evolves by trading while holding Dragon Scale.")], // Seadra -> Kingdra
        [123] = [new(212, "Scizor", "Trade + Item", "Metal Coat", null, "Evolves by trading while holding Metal Coat.")], // Scyther -> Scizor
        [137] = [new(233, "Porygon2", "Trade + Item", "Up-Grade", null, "Evolves by trading while holding Up-Grade.")], // Porygon -> Porygon2

        // Generation 3 Trade + Item Evolutions
        [356] = [new(477, "Dusknoir", "Trade + Item", "Reaper Cloth", null, "Evolves by trading while holding Reaper Cloth.")], // Dusclops -> Dusknoir
        [366] = [
            new(367, "Huntail", "Trade + Item", "Deep Sea Tooth", null, "Evolves by trading while holding Deep Sea Tooth."),
            new(368, "Gorebyss", "Trade + Item", "Deep Sea Scale", null, "Evolves by trading while holding Deep Sea Scale.")
        ], // Clamperl -> Huntail / Gorebyss

        // Generation 4 Trade + Item Evolutions
        [112] = [new(464, "Rhyperior", "Trade + Item", "Protector", null, "Evolves by trading while holding Protector.")], // Rhydon -> Rhyperior
        [125] = [new(466, "Electivire", "Trade + Item", "Electirizer", null, "Evolves by trading while holding Electirizer.")], // Electabuzz -> Electivire
        [126] = [new(467, "Magmortar", "Trade + Item", "Magmarizer", null, "Evolves by trading while holding Magmarizer.")], // Magmar -> Magmortar
        [233] = [new(474, "Porygon-Z", "Trade + Item", "Dubious Disc", null, "Evolves by trading while holding Dubious Disc.")], // Porygon2 -> Porygon-Z

        // Generation 5 Trade Evolutions
        [525] = [new(526, "Gigalith", "Trade", null, null, "Evolves by trade into Gigalith.")], // Boldore -> Gigalith
        [533] = [new(534, "Conkeldurr", "Trade", null, null, "Evolves by trade into Conkeldurr.")], // Gurdurr -> Conkeldurr
        [588] = [new(589, "Escavalier", "Trade", "Shelmet", null, "Evolves by trading for Shelmet.")], // Karrablast -> Escavalier
        [616] = [new(617, "Accelgor", "Trade", "Karrablast", null, "Evolves by trading for Karrablast.")], // Shelmet -> Accelgor

        // Generation 6 Trade + Item Evolutions
        [682] = [new(683, "Aromatisse", "Trade + Item", "Sachet", null, "Evolves by trading while holding Sachet.")], // Spritzee -> Aromatisse
        [684] = [new(685, "Slurpuff", "Trade + Item", "Whipped Dream", null, "Evolves by trading while holding Whipped Dream.")], // Swirlix -> Slurpuff
        [708] = [new(709, "Trevenant", "Trade", null, null, "Evolves by trade into Trevenant.")], // Phantump -> Trevenant
        [710] = [new(711, "Gourgeist", "Trade", null, null, "Evolves by trade into Gourgeist.")], // Pumpkaboo -> Gourgeist

        // Iconic Evolutionary Stone & Special Evolutions
        [25] = [new(26, "Raichu", "Thunder Stone", "Thunder Stone", null, "Evolves using a Thunder Stone.")], // Pikachu -> Raichu
        [133] = [
            new(134, "Vaporeon", "Water Stone", "Water Stone", null, "Evolves using a Water Stone."),
            new(135, "Jolteon", "Thunder Stone", "Thunder Stone", null, "Evolves using a Thunder Stone."),
            new(136, "Flareon", "Fire Stone", "Fire Stone", null, "Evolves using a Fire Stone."),
            new(196, "Espeon", "Daylight Friendship", null, null, "Evolves through friendship during the day."),
            new(197, "Umbreon", "Nighttime Friendship", null, null, "Evolves through friendship at night."),
            new(470, "Leafeon", "Leaf Stone", "Leaf Stone", null, "Evolves using a Leaf Stone."),
            new(471, "Glaceon", "Ice Stone", "Ice Stone", null, "Evolves using an Ice Stone."),
            new(700, "Sylveon", "Fairy Bond", null, null, "Evolves with deep affection and a Fairy-type move.")
        ], // Eevee line
    };

    /// <summary>
    /// Gets available evolution paths for a Pokémon species and current state.
    /// </summary>
    public static List<PokemonEvolutionOptionDto> GetAvailableEvolutions(
        int speciesId,
        string? speciesName,
        int level,
        string? heldItem)
    {
        if (!EvolutionsBySpecies.TryGetValue(speciesId, out var defs))
        {
            return [];
        }

        return defs.Select(d =>
        {
            var canEvolve = true;
            if (d.RequiredLevel.HasValue && level < d.RequiredLevel.Value)
            {
                canEvolve = false;
            }

            return new PokemonEvolutionOptionDto
            {
                TargetSpeciesId = d.TargetSpeciesId,
                TargetSpecies = d.TargetSpecies,
                TriggerMethod = d.TriggerMethod,
                RequiredItem = d.RequiredItem,
                RequiredLevel = d.RequiredLevel,
                Description = d.Description,
                CanEvolveNow = canEvolve
            };
        }).ToList();
    }

    private sealed record EvolutionDefinition(
        int TargetSpeciesId,
        string TargetSpecies,
        string TriggerMethod,
        string? RequiredItem,
        int? RequiredLevel,
        string Description);
}

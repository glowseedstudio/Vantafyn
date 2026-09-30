using System.Collections.Concurrent;
using System.Text.Json.Serialization;

namespace Vantafyn.Plugin.Companion.Pokemon.PkVault;

/// <summary>
/// Thread-safe catalog of Pokémon static data (species, moves, abilities, items, natures).
/// Pre-seeded with retro defaults and enriched via PKVault's GET /api/static-data.
/// </summary>
public sealed class PkVaultStaticCatalog
{
    private static readonly string[] NatureNames =
    [
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty",
        "Bold", "Docile", "Relaxed", "Impish", "Lax",
        "Timid", "Hasty", "Serious", "Jolly", "Naive",
        "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky"
    ];

    private readonly ConcurrentDictionary<int, string> _speciesNames = new();
    private readonly ConcurrentDictionary<int, string> _moveNames = new();
    private readonly ConcurrentDictionary<int, string> _abilityNames = new();
    private readonly ConcurrentDictionary<int, string> _itemNames = new();
    private readonly ConcurrentDictionary<int, string> _natureNames = new();

    public PkVaultStaticCatalog()
    {
        SeedDefaults();
    }

    private void SeedDefaults()
    {
        // Seed Natures (0..24)
        for (var i = 0; i < NatureNames.Length; i++)
        {
            _natureNames[i] = NatureNames[i];
        }

        // Seed Gen 1 Pokémon (1..151)
        var gen1 = new[]
        {
            "Bulbasaur", "Ivysaur", "Venusaur", "Charmander", "Charmeleon", "Charizard",
            "Squirtle", "Wartortle", "Blastoise", "Caterpie", "Metapod", "Butterfree",
            "Weedle", "Kakuna", "Beedrill", "Pidgey", "Pidgeotto", "Pidgeot",
            "Rattata", "Raticate", "Spearow", "Fearow", "Ekans", "Arbok",
            "Pikachu", "Raichu", "Sandshrew", "Sandslash", "Nidoran♀", "Nidorina",
            "Nidoqueen", "Nidoran♂", "Nidorino", "Nidoking", "Clefairy", "Clefable",
            "Vulpix", "Ninetales", "Jigglypuff", "Wigglytuff", "Zubat", "Golbat",
            "Oddish", "Gloom", "Vileplume", "Paras", "Parasect", "Venonat",
            "Venomoth", "Diglett", "Dugtrio", "Meowth", "Persian", "Psyduck",
            "Golduck", "Mankey", "Primeape", "Growlithe", "Arcanine", "Poliwag",
            "Poliwhirl", "Poliwrath", "Abra", "Kadabra", "Alakazam", "Machop",
            "Machoke", "Machamp", "Bellsprout", "Weepinbell", "Victreebel", "Tentacool",
            "Tentacruel", "Geodude", "Graveler", "Golem", "Ponyta", "Rapidash",
            "Slowpoke", "Slowbro", "Magnemite", "Magneton", "Farfetch'd", "Doduo",
            "Dodrio", "Seel", "Dewgong", "Grimer", "Muk", "Shellder",
            "Cloyster", "Gastly", "Haunter", "Gengar", "Onix", "Drowzee",
            "Hypno", "Krabby", "Kingler", "Voltorb", "Electrode", "Exeggcute",
            "Exeggutor", "Cubone", "Marowak", "Hitmonlee", "Hitmonchan", "Lickitung",
            "Koffing", "Weezing", "Rhyhorn", "Rhydon", "Chansey", "Tangela",
            "Kangaskhan", "Horsea", "Seadra", "Goldeen", "Seaking", "Staryu",
            "Starmie", "Mr. Mime", "Scyther", "Jynx", "Electabuzz", "Magmar",
            "Pinsir", "Tauros", "Magikarp", "Gyarados", "Lapras", "Ditto",
            "Eevee", "Vaporeon", "Jolteon", "Flareon", "Porygon", "Omanyte",
            "Omastar", "Kabuto", "Kabutops", "Aerodactyl", "Snorlax", "Articuno",
            "Zapdos", "Moltres", "Dratini", "Dragonair", "Dragonite", "Mewtwo", "Mew"
        };

        for (var i = 0; i < gen1.Length; i++)
        {
            _speciesNames[i + 1] = gen1[i];
        }

        // Seed common test/starter abilities & items & moves
        _abilityNames[9] = "Static";
        _abilityNames[65] = "Blaze";
        _itemNames[236] = "Light Ball";
        _moveNames[85] = "Thunderbolt";
        _moveNames[98] = "Quick Attack";
        _moveNames[231] = "Iron Tail";
        _moveNames[344] = "Volt Tackle";
    }

    public string ResolveSpeciesName(int speciesId, string? nickname, bool isNicknamed)
    {
        if (_speciesNames.TryGetValue(speciesId, out var knownName))
        {
            return knownName;
        }

        if (!isNicknamed && !string.IsNullOrWhiteSpace(nickname))
        {
            return nickname;
        }

        if (!string.IsNullOrWhiteSpace(nickname))
        {
            return nickname;
        }

        return $"#{speciesId}";
    }

    public string ResolveNatureName(int natureId)
    {
        if (_natureNames.TryGetValue(natureId, out var name))
        {
            return name;
        }

        if (natureId >= 0 && natureId < NatureNames.Length)
        {
            return NatureNames[natureId];
        }

        return natureId.ToString();
    }

    public string? ResolveAbilityName(int abilityId)
    {
        if (abilityId <= 0) return null;
        if (_abilityNames.TryGetValue(abilityId, out var name))
        {
            return name;
        }
        return abilityId.ToString();
    }

    public string? ResolveItemName(int itemId)
    {
        if (itemId <= 0) return null;
        if (_itemNames.TryGetValue(itemId, out var name))
        {
            return name;
        }
        return itemId.ToString();
    }

    public string ResolveMoveName(int moveId)
    {
        if (_moveNames.TryGetValue(moveId, out var name))
        {
            return name;
        }
        return moveId.ToString();
    }

    public void PopulateFromStaticData(PkVaultStaticDataDto staticData)
    {
        if (staticData == null) return;

        // Moves
        if (staticData.Moves != null)
        {
            foreach (var kvp in staticData.Moves)
            {
                if (int.TryParse(kvp.Key, out var id) && !string.IsNullOrWhiteSpace(kvp.Value?.Name))
                {
                    _moveNames[id] = kvp.Value.Name;
                }
            }
        }

        // Abilities
        if (staticData.Abilities != null)
        {
            foreach (var kvp in staticData.Abilities)
            {
                if (int.TryParse(kvp.Key, out var id) && !string.IsNullOrWhiteSpace(kvp.Value?.Name))
                {
                    _abilityNames[id] = kvp.Value.Name;
                }
            }
        }

        // Items
        if (staticData.Items?.Items != null)
        {
            foreach (var kvp in staticData.Items.Items)
            {
                if (int.TryParse(kvp.Key, out var id) && !string.IsNullOrWhiteSpace(kvp.Value?.Name))
                {
                    _itemNames[id] = kvp.Value.Name;
                }
            }
        }

        // Natures
        if (staticData.Natures != null)
        {
            foreach (var kvp in staticData.Natures)
            {
                if (int.TryParse(kvp.Key, out var id) && !string.IsNullOrWhiteSpace(kvp.Value?.Name))
                {
                    _natureNames[id] = kvp.Value.Name;
                }
            }
        }

        // Species
        if (staticData.Species != null)
        {
            foreach (var kvp in staticData.Species)
            {
                if (!int.TryParse(kvp.Key, out var id) || kvp.Value?.Forms == null) continue;
                foreach (var formList in kvp.Value.Forms.Values)
                {
                    var formItem = formList.FirstOrDefault(f => !string.IsNullOrWhiteSpace(f?.Name));
                    if (formItem != null && !string.IsNullOrWhiteSpace(formItem.Name))
                    {
                        _speciesNames[id] = formItem.Name;
                        break;
                    }
                }
            }
        }
    }
}

public sealed class PkVaultStaticDataDto
{
    [JsonPropertyName("species")]
    public Dictionary<string, PkVaultStaticSpeciesDto>? Species { get; set; }

    [JsonPropertyName("moves")]
    public Dictionary<string, PkVaultStaticItemDto>? Moves { get; set; }

    [JsonPropertyName("abilities")]
    public Dictionary<string, PkVaultStaticItemDto>? Abilities { get; set; }

    [JsonPropertyName("items")]
    public PkVaultStaticItemsDataDto? Items { get; set; }

    [JsonPropertyName("natures")]
    public Dictionary<string, PkVaultStaticItemDto>? Natures { get; set; }
}

public sealed class PkVaultStaticSpeciesDto
{
    [JsonPropertyName("id")]
    public int Id { get; set; }

    [JsonPropertyName("forms")]
    public Dictionary<string, List<PkVaultStaticSpeciesFormDto>>? Forms { get; set; }
}

public sealed class PkVaultStaticSpeciesFormDto
{
    [JsonPropertyName("name")]
    public string? Name { get; set; }
}

public sealed class PkVaultStaticItemDto
{
    [JsonPropertyName("id")]
    public int Id { get; set; }

    [JsonPropertyName("name")]
    public string? Name { get; set; }
}

public sealed class PkVaultStaticItemsDataDto
{
    [JsonPropertyName("items")]
    public Dictionary<string, PkVaultStaticItemDto>? Items { get; set; }
}

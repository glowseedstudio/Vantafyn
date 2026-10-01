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

        // Seed All Pokémon species (1..1025)
        for (var i = 0; i < PokemonSpeciesCatalog.AllSpecies.Length; i++)
        {
            _speciesNames[i + 1] = PokemonSpeciesCatalog.AllSpecies[i];
        }

        // Seed common test/starter abilities & items & moves
        _abilityNames[4] = "Battle Armor";
        _abilityNames[5] = "Sturdy";
        _abilityNames[6] = "Damp";
        _abilityNames[7] = "Limber";
        _abilityNames[8] = "Sand Veil";
        _abilityNames[9] = "Static";
        _abilityNames[10] = "Volt Absorb";
        _abilityNames[11] = "Water Absorb";
        _abilityNames[12] = "Oblivious";
        _abilityNames[13] = "Cloud Nine";
        _abilityNames[14] = "Compound Eyes";
        _abilityNames[15] = "Insomnia";
        _abilityNames[17] = "Immunity";
        _abilityNames[18] = "Flash Fire";
        _abilityNames[19] = "Shield Dust";
        _abilityNames[20] = "Own Tempo";
        _abilityNames[22] = "Intimidate";
        _abilityNames[26] = "Levitate";
        _abilityNames[27] = "Effect Spore";
        _abilityNames[28] = "Synchronize";
        _abilityNames[29] = "Clear Body";
        _abilityNames[30] = "Natural Cure";
        _abilityNames[31] = "Lightning Rod";
        _abilityNames[32] = "Serene Grace";
        _abilityNames[33] = "Swift Swim";
        _abilityNames[34] = "Chlorophyll";
        _abilityNames[35] = "Illuminate";
        _abilityNames[36] = "Trace";
        _abilityNames[38] = "Poison Point";
        _abilityNames[39] = "Inner Focus";
        _abilityNames[41] = "Water Veil";
        _abilityNames[42] = "Magnet Pull";
        _abilityNames[43] = "Soundproof";
        _abilityNames[46] = "Pressure";
        _abilityNames[47] = "Thick Fat";
        _abilityNames[48] = "Early Bird";
        _abilityNames[49] = "Flame Body";
        _abilityNames[50] = "Run Away";
        _abilityNames[51] = "Keen Eye";
        _abilityNames[52] = "Hyper Cutter";
        _abilityNames[53] = "Pickup";
        _abilityNames[56] = "Cute Charm";
        _abilityNames[61] = "Shed Skin";
        _abilityNames[62] = "Guts";
        _abilityNames[64] = "Liquid Ooze";
        _abilityNames[65] = "Overgrow";
        _abilityNames[66] = "Blaze";
        _abilityNames[67] = "Torrent";
        _abilityNames[68] = "Swarm";
        _abilityNames[69] = "Rock Head";
        _abilityNames[71] = "Arena Trap";
        _abilityNames[72] = "Vital Spirit";
        _abilityNames[75] = "Shell Armor";
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

        var catalogName = PokemonSpeciesCatalog.ResolveSpeciesName(speciesId);
        if (!catalogName.StartsWith('#'))
        {
            return catalogName;
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

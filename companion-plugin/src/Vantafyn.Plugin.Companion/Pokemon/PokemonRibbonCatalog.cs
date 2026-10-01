namespace Vantafyn.Plugin.Companion.Pokemon;

using System.Text.Json.Serialization;

/// <summary>
/// A ribbon or mark earned by a Pokémon.
/// </summary>
public sealed class PokemonRibbonDto
{
    [JsonPropertyName("key")]
    public string Key { get; set; } = string.Empty;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("category")]
    public string Category { get; set; } = "Memorial"; // Champion, Effort, Battle, Contest, Memorial, Tower

    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    [JsonPropertyName("title")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Title { get; set; }

    [JsonPropertyName("iconColorHex")]
    public string IconColorHex { get; set; } = "#3B82F6";
}

/// <summary>
/// Catalog of authentic mainline Pokémon ribbons and resolution logic.
/// </summary>
public static class PokemonRibbonCatalog
{
    private static readonly Dictionary<string, PokemonRibbonDto> KnownRibbons = new(StringComparer.OrdinalIgnoreCase)
    {
        // Champion Ribbons
        ["champion"] = new()
        {
            Key = "champion",
            Name = "Champion Ribbon",
            Category = "Champion",
            Description = "A Ribbon awarded for clearing the Pokémon League and entering the Hall of Fame in the Hoenn or Kanto region.",
            Title = "the Champion",
            IconColorHex = "#F59E0B"
        },
        ["champion_sinnoh"] = new()
        {
            Key = "champion_sinnoh",
            Name = "Sinnoh Champion Ribbon",
            Category = "Champion",
            Description = "A Ribbon awarded for beating the Sinnoh Pokémon League Champion and entering the Hall of Fame.",
            Title = "the Sinnoh Champion",
            IconColorHex = "#3B82F6"
        },
        ["champion_kalos"] = new()
        {
            Key = "champion_kalos",
            Name = "Kalos Champion Ribbon",
            Category = "Champion",
            Description = "A Ribbon awarded for beating the Kalos Champion and entering the Hall of Fame.",
            Title = "the Kalos Champion",
            IconColorHex = "#EC4899"
        },
        ["champion_alola"] = new()
        {
            Key = "champion_alola",
            Name = "Alola Champion Ribbon",
            Category = "Champion",
            Description = "A Ribbon awarded for becoming the first Champion of the Alola region Pokémon League.",
            Title = "the Alola Champion",
            IconColorHex = "#F97316"
        },
        ["champion_galar"] = new()
        {
            Key = "champion_galar",
            Name = "Galar Champion Ribbon",
            Category = "Champion",
            Description = "A Ribbon awarded for defeating the Champion in the Champion Cup of the Galar region.",
            Title = "the Galar Champion",
            IconColorHex = "#8B5CF6"
        },
        ["champion_paldea"] = new()
        {
            Key = "champion_paldea",
            Name = "Paldea Champion Ribbon",
            Category = "Champion",
            Description = "A Ribbon awarded for completing the Champion Assessment in the Paldea region.",
            Title = "the Paldea Champion",
            IconColorHex = "#10B981"
        },

        // Achievement & Effort Ribbons
        ["effort"] = new()
        {
            Key = "effort",
            Name = "Effort Ribbon",
            Category = "Effort",
            Description = "A Ribbon awarded to a Pokémon that has put forth maximum effort in its EV training (510 total effort points).",
            Title = "the Effort-Minded",
            IconColorHex = "#EF4444"
        },
        ["footprint"] = new()
        {
            Key = "footprint",
            Name = "Footprint Ribbon",
            Category = "Affection",
            Description = "A Ribbon awarded to a Pokémon that reached the pinnacle of experience and level (Lv. 100 or max friendship).",
            Title = "the Experienced",
            IconColorHex = "#10B981"
        },
        ["best_friends"] = new()
        {
            Key = "best_friends",
            Name = "Best Friends Ribbon",
            Category = "Affection",
            Description = "A Ribbon awarded to a Pokémon with deep, unshakable bonds of affection with its Trainer.",
            Title = "the Great Friend",
            IconColorHex = "#EC4899"
        },

        // Battle Facility & Tower Ribbons
        ["winning"] = new()
        {
            Key = "winning",
            Name = "Winning Ribbon",
            Category = "Tower",
            Description = "A Ribbon awarded for clearing the Battle Tower at Level 50 in the Hoenn region.",
            Title = "the Tower Master",
            IconColorHex = "#6366F1"
        },
        ["victory"] = new()
        {
            Key = "victory",
            Name = "Victory Ribbon",
            Category = "Tower",
            Description = "A Ribbon awarded for clearing Level 100 in the Battle Tower.",
            Title = "the Victorious",
            IconColorHex = "#F59E0B"
        },
        ["master_rank"] = new()
        {
            Key = "master_rank",
            Name = "Master Rank Ribbon",
            Category = "Battle",
            Description = "A Ribbon awarded for winning a Master Rank Tier match in the Ranked Battles stadium.",
            Title = "the Rank Master",
            IconColorHex = "#8B5CF6"
        },
        ["ability"] = new()
        {
            Key = "ability",
            Name = "Ability Ribbon",
            Category = "Tower",
            Description = "A Ribbon awarded for defeating Tower Tycoon Palmer in the Sinnoh Battle Tower.",
            Title = "the Tower Master",
            IconColorHex = "#14B8A6"
        },

        // Contest Ribbons
        ["contest_star"] = new()
        {
            Key = "contest_star",
            Name = "Contest Star Ribbon",
            Category = "Contest",
            Description = "A Ribbon awarded to a superstar Pokémon that conquered all Master Rank Super Contests.",
            Title = "the Shining Star",
            IconColorHex = "#F43F5E"
        },
        ["cool"] = new()
        {
            Key = "cool",
            Name = "Cool Ribbon",
            Category = "Contest",
            Description = "A Ribbon awarded for triumphing in the Cool Contest Master Rank.",
            Title = "the Cool",
            IconColorHex = "#EF4444"
        },
        ["beauty"] = new()
        {
            Key = "beauty",
            Name = "Beauty Ribbon",
            Category = "Contest",
            Description = "A Ribbon awarded for triumphing in the Beauty Contest Master Rank.",
            Title = "the Beautiful",
            IconColorHex = "#3B82F6"
        },
        ["cute"] = new()
        {
            Key = "cute",
            Name = "Cute Ribbon",
            Category = "Contest",
            Description = "A Ribbon awarded for triumphing in the Cute Contest Master Rank.",
            Title = "the Adorable",
            IconColorHex = "#EC4899"
        },
        ["smart"] = new()
        {
            Key = "smart",
            Name = "Smart Ribbon",
            Category = "Contest",
            Description = "A Ribbon awarded for triumphing in the Smart Contest Master Rank.",
            Title = "the Clever",
            IconColorHex = "#10B981"
        },
        ["tough"] = new()
        {
            Key = "tough",
            Name = "Tough Ribbon",
            Category = "Contest",
            Description = "A Ribbon awarded for triumphing in the Tough Contest Master Rank.",
            Title = "the Sturdy",
            IconColorHex = "#F59E0B"
        },
    };

    /// <summary>
    /// Resolves or evaluates ribbons for a Pokémon based on explicit PKVault data and deterministic achievements.
    /// </summary>
    public static List<PokemonRibbonDto> EvaluateRibbons(
        Dictionary<string, byte>? explicitRibbons,
        PokemonStatsDto? evs,
        int level,
        int friendship,
        string? originGame,
        int generation,
        bool isInParty)
    {
        var result = new Dictionary<string, PokemonRibbonDto>(StringComparer.OrdinalIgnoreCase);

        // 1. Explicit ribbons from PKVault
        if (explicitRibbons != null)
        {
            foreach (var (k, v) in explicitRibbons)
            {
                if (v > 0)
                {
                    if (KnownRibbons.TryGetValue(k, out var match))
                    {
                        result[match.Key] = match;
                    }
                    else
                    {
                        var formattedName = System.Globalization.CultureInfo.InvariantCulture.TextInfo.ToTitleCase(
                            k.Replace('_', ' '));
                        result[k] = new PokemonRibbonDto
                        {
                            Key = k,
                            Name = formattedName.EndsWith("Ribbon", StringComparison.OrdinalIgnoreCase) ? formattedName : $"{formattedName} Ribbon",
                            Category = "Memorial",
                            Description = $"A prestigious ribbon earned across adventures: {formattedName}.",
                            Title = $"the {formattedName}",
                            IconColorHex = "#3B82F6"
                        };
                    }
                }
            }
        }

        // 2. Deterministic: Effort Ribbon (510 total EVs)
        if (evs != null)
        {
            var totalEvs = evs.Hp + evs.Attack + evs.Defense + evs.Speed + evs.SpecialAttack + evs.SpecialDefense;
            if (totalEvs >= 508 && !result.ContainsKey("effort")) // 508+ is effectively maxed (510 max, 2 wasted)
            {
                result["effort"] = KnownRibbons["effort"];
            }
        }

        // 3. Deterministic: Footprint Ribbon (Level 100 or max friendship)
        if ((level >= 100 || friendship >= 250) && !result.ContainsKey("footprint"))
        {
            result["footprint"] = KnownRibbons["footprint"];
        }

        // 4. Deterministic: Champion Ribbon for high-level adventure companions (Level 55+ from campaign games)
        if (level >= 55)
        {
            var champKey = ResolveChampionRibbonKey(originGame, generation);
            if (!string.IsNullOrEmpty(champKey) && KnownRibbons.TryGetValue(champKey, out var champRibbon))
            {
                result[champRibbon.Key] = champRibbon;
            }
        }

        return result.Values.OrderBy(r => GetCategoryOrder(r.Category)).ThenBy(r => r.Name).ToList();
    }

    private static string ResolveChampionRibbonKey(string? originGame, int generation)
    {
        if (!string.IsNullOrWhiteSpace(originGame))
        {
            var g = originGame.ToLowerInvariant();
            if (g.Contains("ruby") || g.Contains("sapphire") || g.Contains("emerald") || g.Contains("firered") || g.Contains("leafgreen") || g.Contains("omega") || g.Contains("alpha"))
                return "champion";
            if (g.Contains("diamond") || g.Contains("pearl") || g.Contains("platinum") || g.Contains("brilliant") || g.Contains("shining"))
                return "champion_sinnoh";
            if (g.Contains("x") || g.Contains("y"))
                return "champion_kalos";
            if (g.Contains("sun") || g.Contains("moon") || g.Contains("ultra"))
                return "champion_alola";
            if (g.Contains("sword") || g.Contains("shield"))
                return "champion_galar";
            if (g.Contains("scarlet") || g.Contains("violet"))
                return "champion_paldea";
        }

        return generation switch
        {
            3 => "champion",
            4 => "champion_sinnoh",
            6 => "champion_kalos",
            7 => "champion_alola",
            8 => "champion_galar",
            9 => "champion_paldea",
            _ => "champion"
        };
    }

    private static int GetCategoryOrder(string category) => category.ToLowerInvariant() switch
    {
        "champion" => 1,
        "effort" => 2,
        "affection" => 3,
        "battle" => 4,
        "tower" => 5,
        "contest" => 6,
        _ => 7
    };
}

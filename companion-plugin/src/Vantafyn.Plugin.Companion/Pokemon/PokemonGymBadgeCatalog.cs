namespace Vantafyn.Plugin.Companion.Pokemon;

public static class PokemonGymBadgeCatalog
{
    private static readonly BadgeDef[] Kanto =
    [
        new("boulder", "Boulder Badge"),
        new("cascade", "Cascade Badge"),
        new("thunder", "Thunder Badge"),
        new("rainbow", "Rainbow Badge"),
        new("soul", "Soul Badge"),
        new("marsh", "Marsh Badge"),
        new("volcano", "Volcano Badge"),
        new("earth", "Earth Badge")
    ];

    private static readonly BadgeDef[] Johto =
    [
        new("zephyr", "Zephyr Badge"),
        new("hive", "Hive Badge"),
        new("plain", "Plain Badge"),
        new("fog", "Fog Badge"),
        new("storm", "Storm Badge"),
        new("mineral", "Mineral Badge"),
        new("glacier", "Glacier Badge"),
        new("rising", "Rising Badge")
    ];

    private static readonly BadgeDef[] Hoenn =
    [
        new("stone", "Stone Badge"),
        new("knuckle", "Knuckle Badge"),
        new("dynamo", "Dynamo Badge"),
        new("heat", "Heat Badge"),
        new("balance", "Balance Badge"),
        new("feather", "Feather Badge"),
        new("mind", "Mind Badge"),
        new("rain", "Rain Badge")
    ];

    public static List<PokemonGymBadgeRegionDto> ForGen1(byte kantoFlags) =>
    [
        CreateRegion("kanto", "Kanto", 1, Kanto, kantoFlags)
    ];

    public static List<PokemonGymBadgeRegionDto> ForGen2(byte johtoFlags, byte kantoFlags) =>
    [
        CreateRegion("johto", "Johto", 2, Johto, johtoFlags),
        CreateRegion("kanto", "Kanto", 1, Kanto, kantoFlags)
    ];

    public static List<PokemonGymBadgeRegionDto> ForGen3(string gameId, byte badgeFlags)
    {
        var key = gameId.ToLowerInvariant();
        var isFrLg = key.Contains("firered", StringComparison.Ordinal) ||
                     key.Contains("fire_red", StringComparison.Ordinal) ||
                     key.Contains("fire red", StringComparison.Ordinal) ||
                     key.Contains("leafgreen", StringComparison.Ordinal) ||
                     key.Contains("leaf_green", StringComparison.Ordinal) ||
                     key.Contains("leaf green", StringComparison.Ordinal);

        return
        [
            isFrLg
                ? CreateRegion("kanto", "Kanto", 1, Kanto, badgeFlags)
                : CreateRegion("hoenn", "Hoenn", 3, Hoenn, badgeFlags)
        ];
    }

    private static PokemonGymBadgeRegionDto CreateRegion(string region, string displayName, int generation, IReadOnlyList<BadgeDef> badges, byte flags)
    {
        return new PokemonGymBadgeRegionDto
        {
            Region = region,
            DisplayName = displayName,
            Generation = generation,
            Badges = badges.Select((badge, index) => new PokemonGymBadgeDto
            {
                Id = badge.Id,
                Name = badge.Name,
                Region = region,
                Generation = generation,
                Order = index + 1,
                IsEarned = (flags & (1 << index)) != 0
            }).ToList()
        };
    }

    private sealed record BadgeDef(string Id, string Name);
}

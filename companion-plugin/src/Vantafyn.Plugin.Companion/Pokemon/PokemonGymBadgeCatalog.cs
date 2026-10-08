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

    private static readonly BadgeDef[] Sinnoh =
    [
        new("coal", "Coal Badge"),
        new("forest", "Forest Badge"),
        new("cobble", "Cobble Badge"),
        new("fen", "Fen Badge"),
        new("relic", "Relic Badge"),
        new("mine", "Mine Badge"),
        new("icicle", "Icicle Badge"),
        new("beacon", "Beacon Badge")
    ];

    private static readonly BadgeDef[] Unova =
    [
        new("trio", "Trio Badge"),
        new("basic", "Basic Badge"),
        new("insect", "Insect Badge"),
        new("bolt", "Bolt Badge"),
        new("quake", "Quake Badge"),
        new("jet", "Jet Badge"),
        new("freeze", "Freeze Badge"),
        new("legend", "Legend Badge")
    ];

    private static readonly BadgeDef[] Kalos =
    [
        new("bug", "Bug Badge"),
        new("cliff", "Cliff Badge"),
        new("rumble", "Rumble Badge"),
        new("plant", "Plant Badge"),
        new("voltage", "Voltage Badge"),
        new("fairy", "Fairy Badge"),
        new("psychic", "Psychic Badge"),
        new("iceberg", "Iceberg Badge")
    ];

    private static readonly BadgeDef[] Alola =
    [
        new("melemele", "Melemele Stamp"),
        new("akala", "Akala Stamp"),
        new("ulaula", "Ula'ula Stamp"),
        new("poni", "Poni Stamp"),
        new("champion", "Island Challenge Stamp")
    ];

    private static readonly BadgeDef[] ZCrystalsSm =
    [
        new("normalium-z", "Normalium Z"),
        new("firium-z", "Firium Z"),
        new("waterium-z", "Waterium Z"),
        new("electrium-z", "Electrium Z"),
        new("grassium-z", "Grassium Z"),
        new("icium-z", "Icium Z"),
        new("fightinium-z", "Fightinium Z"),
        new("poisonium-z", "Poisonium Z"),
        new("groundium-z", "Groundium Z"),
        new("flyinium-z", "Flyinium Z"),
        new("psychium-z", "Psychium Z"),
        new("buginium-z", "Buginium Z"),
        new("rockium-z", "Rockium Z"),
        new("ghostium-z", "Ghostium Z"),
        new("dragonium-z", "Dragonium Z"),
        new("darkinium-z", "Darkinium Z"),
        new("steelium-z", "Steelium Z"),
        new("fairium-z", "Fairium Z"),
        new("pikanium-z", "Pikanium Z"),
        new("decidium-z", "Decidium Z"),
        new("incinium-z", "Incinium Z"),
        new("primarium-z", "Primarium Z"),
        new("tapunium-z", "Tapunium Z"),
        new("marshadium-z", "Marshadium Z"),
        new("aloraichium-z", "Aloraichium Z"),
        new("snorlium-z", "Snorlium Z"),
        new("eevium-z", "Eevium Z"),
        new("mewnium-z", "Mewnium Z"),
        new("pikashunium-z", "Pikashunium Z")
    ];

    private static readonly BadgeDef[] ZCrystalsUsum =
    [
        ..ZCrystalsSm,
        new("solganium-z", "Solganium Z"),
        new("lunalium-z", "Lunalium Z"),
        new("ultranecrozium-z", "Ultranecrozium Z"),
        new("mimikium-z", "Mimikium Z"),
        new("lycanium-z", "Lycanium Z"),
        new("kommonium-z", "Kommonium Z")
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
        var key = (gameId ?? string.Empty).ToLowerInvariant();
        var isFrLg = key.Contains("firered", StringComparison.Ordinal) ||
                     key.Contains("fire_red", StringComparison.Ordinal) ||
                     key.Contains("fire red", StringComparison.Ordinal) ||
                     key.Contains("leafgreen", StringComparison.Ordinal) ||
                     key.Contains("leaf_green", StringComparison.Ordinal) ||
                     key.Contains("leaf green", StringComparison.Ordinal);

        var isHoenn = (key.Contains("emerald", StringComparison.Ordinal) ||
                      key.Contains("ruby", StringComparison.Ordinal) ||
                      key.Contains("sapphire", StringComparison.Ordinal) ||
                      key.Contains("rse", StringComparison.Ordinal)) &&
                      !key.Contains("omega", StringComparison.Ordinal) &&
                      !key.Contains("alpha", StringComparison.Ordinal);

        if (isFrLg)
        {
            return [ CreateRegion("kanto", "Kanto", 1, Kanto, badgeFlags) ];
        }
        if (isHoenn)
        {
            return [ CreateRegion("hoenn", "Hoenn", 3, Hoenn, badgeFlags) ];
        }
        return [];
    }

    public static List<PokemonGymBadgeRegionDto> ForGen4(string gameId, byte johtoOrSinnohFlags, byte kantoFlags)
    {
        var key = (gameId ?? string.Empty).ToLowerInvariant();
        var isHgss = key.Contains("heartgold", StringComparison.Ordinal) ||
                     key.Contains("soulsilver", StringComparison.Ordinal) ||
                     key.Contains("heart gold", StringComparison.Ordinal) ||
                     key.Contains("soul silver", StringComparison.Ordinal) ||
                     key.Contains("hgss", StringComparison.Ordinal);

        var isSinnoh = key.Contains("diamond", StringComparison.Ordinal) ||
                       key.Contains("pearl", StringComparison.Ordinal) ||
                       key.Contains("platinum", StringComparison.Ordinal);

        if (isHgss)
        {
            return
            [
                CreateRegion("johto", "Johto", 2, Johto, johtoOrSinnohFlags),
                CreateRegion("kanto", "Kanto", 1, Kanto, kantoFlags)
            ];
        }
        if (isSinnoh)
        {
            return [ CreateRegion("sinnoh", "Sinnoh", 4, Sinnoh, johtoOrSinnohFlags) ];
        }
        return [];
    }

    public static List<PokemonGymBadgeRegionDto> ForGen5(string gameId, byte unovaFlags)
    {
        var key = (gameId ?? string.Empty).ToLowerInvariant();
        var isUnova = key.Contains("black", StringComparison.Ordinal) ||
                      key.Contains("white", StringComparison.Ordinal) ||
                      key.Contains("b2w2", StringComparison.Ordinal);

        if (isUnova)
        {
            return [ CreateRegion("unova", "Unova", 5, Unova, unovaFlags) ];
        }
        return [];
    }

    public static List<PokemonGymBadgeRegionDto> ForGen6(string gameId, byte badgeFlags)
    {
        var key = (gameId ?? string.Empty).ToLowerInvariant();
        var isOras = key.Contains("omega", StringComparison.Ordinal) ||
                     key.Contains("alpha", StringComparison.Ordinal) ||
                     key.Contains("oras", StringComparison.Ordinal);

        return isOras
            ? [ CreateRegion("hoenn", "Hoenn", 3, Hoenn, badgeFlags) ]
            : [ CreateRegion("kalos", "Kalos", 6, Kalos, badgeFlags) ];
    }

    public static List<PokemonGymBadgeRegionDto> ForGen7(
        string gameId,
        ushort stampFlags,
        ISet<string>? unlockedZCrystals = null,
        bool isUsum = false)
    {
        var alola = CreateRegion("alola", "Alola", 7, Alola, (byte)(stampFlags & 0xFF));
        var crystals = isUsum ? ZCrystalsUsum : ZCrystalsSm;
        var zCrystalsRegion = new PokemonGymBadgeRegionDto
        {
            Region = "zcrystals",
            DisplayName = "Z-Crystals",
            Generation = 7,
            Badges = crystals.Select((c, idx) => new PokemonGymBadgeDto
            {
                Id = c.Id,
                Name = c.Name,
                Region = "zcrystals",
                Generation = 7,
                Order = idx + 1,
                IsEarned = unlockedZCrystals != null && unlockedZCrystals.Contains(c.Id)
            }).ToList()
        };
        return [ alola, zCrystalsRegion ];
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

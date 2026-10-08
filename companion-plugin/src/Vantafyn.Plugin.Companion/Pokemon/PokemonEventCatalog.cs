namespace Vantafyn.Plugin.Companion.Pokemon;

public static class PokemonEventCatalog
{
    public const string CrystalGsBall = "crystal-gs-ball";
    public const string EmeraldOldSeaMap = "emerald-old-sea-map";
    public const string EmeraldAuroraTicket = "emerald-aurora-ticket";
    public const string EmeraldMysticTicket = "emerald-mystic-ticket";
    public const string FrlgAuroraTicket = "frlg-aurora-ticket";
    public const string FrlgMysticTicket = "frlg-mystic-ticket";
    public const string RseEonTicket = "rse-eon-ticket";
    public const string Gen4MemberCard = "gen4-member-card";
    public const string Gen4OaksLetter = "gen4-oaks-letter";
    public const string Gen4AzureFlute = "gen4-azure-flute";
    public const string PlatinumSecretKey = "platinum-secret-key";
    public const string HgssEnigmaStone = "hgss-enigma-stone";
    public const string BwLibertyPass = "bw-liberty-pass";
    public const string OrasEonTicket = "oras-eon-ticket";
    public const string Gen7MagearnaDelivery = "gen7-magearna-delivery";
    public const string Gen7AshGreninjaDelivery = "gen7-ash-greninja-delivery";

    private static readonly IReadOnlyList<PokemonEventUnlockDto> Events =
    [
        new()
        {
            Id = CrystalGsBall,
            Title = "GS Ball",
            Subtitle = "Unlock Ilex Forest Celebi Event",
            Description = "Enables the Crystal GS Ball sequence so the player can receive the GS Ball and complete the shrine event in-game.",
            Generation = 2,
            Region = "Johto",
            Legendary = "Celebi",
            Accent = "#34D399",
            TargetSpeciesId = 251,
            InGameLocation = "Ilex Forest Shrine (Deliver GS Ball to Kurt)",
            SupportedGameIds = ["crystal"]
        },
        new()
        {
            Id = EmeraldOldSeaMap,
            Title = "Old Sea Map",
            Subtitle = "Unlock Faraway Island Mew Event",
            Description = "Adds the Old Sea Map and opens Faraway Island travel in Emerald so Mew can be hunted in-game.",
            Generation = 3,
            Region = "Hoenn",
            Legendary = "Mew",
            Accent = "#F472B6",
            TargetSpeciesId = 151,
            InGameLocation = "Faraway Island (S.S. Tidal from Lilycove Harbor)",
            SupportedGameIds = ["emerald"]
        },
        new()
        {
            Id = EmeraldAuroraTicket,
            Title = "AuroraTicket",
            Subtitle = "Unlock Birth Island Deoxys Event",
            Description = "Adds the AuroraTicket and enables Birth Island travel in Emerald so Deoxys can be encountered in-game.",
            Generation = 3,
            Region = "Hoenn",
            Legendary = "Deoxys",
            Accent = "#38BDF8",
            TargetSpeciesId = 386,
            InGameLocation = "Birth Island (Ferry from Lilycove Harbor)",
            SupportedGameIds = ["emerald"]
        },
        new()
        {
            Id = EmeraldMysticTicket,
            Title = "MysticTicket",
            Subtitle = "Unlock Navel Rock Lugia & Ho-Oh Event",
            Description = "Adds the MysticTicket and enables Navel Rock travel in Emerald for Lugia and Ho-Oh.",
            Generation = 3,
            Region = "Hoenn",
            Legendary = "Lugia & Ho-Oh",
            Accent = "#A78BFA",
            TargetSpeciesId = 249,
            InGameLocation = "Navel Rock (Ferry from Lilycove Harbor)",
            SupportedGameIds = ["emerald"]
        },
        new()
        {
            Id = FrlgAuroraTicket,
            Title = "AuroraTicket",
            Subtitle = "Unlock Birth Island Deoxys Event",
            Description = "Adds the AuroraTicket and enables Birth Island travel in FireRed and LeafGreen.",
            Generation = 3,
            Region = "Kanto",
            Legendary = "Deoxys",
            Accent = "#38BDF8",
            TargetSpeciesId = 386,
            InGameLocation = "Birth Island (Seagallop Ferry from Vermilion)",
            SupportedGameIds = ["firered", "leafgreen"]
        },
        new()
        {
            Id = FrlgMysticTicket,
            Title = "MysticTicket",
            Subtitle = "Unlock Navel Rock Lugia & Ho-Oh Event",
            Description = "Adds the MysticTicket and enables Navel Rock travel in FireRed and LeafGreen.",
            Generation = 3,
            Region = "Kanto",
            Legendary = "Lugia & Ho-Oh",
            Accent = "#A78BFA",
            TargetSpeciesId = 249,
            InGameLocation = "Navel Rock (Seagallop Ferry from Vermilion)",
            SupportedGameIds = ["firered", "leafgreen"]
        },
        new()
        {
            Id = RseEonTicket,
            Title = "Eon Ticket",
            Subtitle = "Unlock Southern Island Lati Event",
            Description = "Adds the Eon Ticket and enables Southern Island travel where supported by Ruby, Sapphire, and Emerald saves.",
            Generation = 3,
            Region = "Hoenn",
            Legendary = "Latios / Latias",
            Accent = "#FBBF24",
            TargetSpeciesId = 381,
            InGameLocation = "Southern Island (Ferry from Lilycove / Slateport)",
            SupportedGameIds = ["ruby", "sapphire", "emerald"]
        },
        new()
        {
            Id = Gen4MemberCard,
            Title = "Member Card",
            Subtitle = "Unlock Newmoon Island Darkrai Event",
            Description = "Adds the Member Card and unlocks the Canalave Harbor Inn so Darkrai can be confronted on Newmoon Island in-game.",
            Generation = 4,
            Region = "Sinnoh",
            Legendary = "Darkrai",
            Accent = "#A855F7",
            TargetSpeciesId = 491,
            InGameLocation = "Newmoon Island (Sleep at Canalave City Harbor Inn)",
            SupportedGameIds = ["diamond", "pearl", "platinum"]
        },
        new()
        {
            Id = Gen4OaksLetter,
            Title = "Oak's Letter",
            Subtitle = "Unlock Flower Paradise Shaymin Event",
            Description = "Adds Oak's Letter and blooms the Seabreak Path on Route 224 so Shaymin can be encountered at Flower Paradise in-game.",
            Generation = 4,
            Region = "Sinnoh",
            Legendary = "Shaymin",
            Accent = "#10B981",
            TargetSpeciesId = 492,
            InGameLocation = "Flower Paradise (Seabreak Path at Route 224)",
            SupportedGameIds = ["diamond", "pearl", "platinum"]
        },
        new()
        {
            Id = Gen4AzureFlute,
            Title = "Azure Flute",
            Subtitle = "Unlock Hall of Origin Arceus Event",
            Description = "Adds the ethereal Azure Flute to summon the staircase of light atop Spear Pillar and ascend to battle Arceus in-game.",
            Generation = 4,
            Region = "Sinnoh",
            Legendary = "Arceus",
            Accent = "#F59E0B",
            TargetSpeciesId = 493,
            InGameLocation = "Hall of Origin (Blow flute atop Spear Pillar)",
            SupportedGameIds = ["diamond", "pearl", "platinum"]
        },
        new()
        {
            Id = PlatinumSecretKey,
            Title = "Secret Key",
            Subtitle = "Unlock Rotom Appliance Room",
            Description = "Adds the Secret Key to open Charon's secret room in Team Galactic Eterna Building and unlock all 5 Rotom appliance forms.",
            Generation = 4,
            Region = "Sinnoh",
            Legendary = "Rotom (Appliances)",
            Accent = "#F97316",
            TargetSpeciesId = 479,
            InGameLocation = "Galactic Eterna Building (Ground floor hidden room)",
            SupportedGameIds = ["platinum"]
        },
        new()
        {
            Id = HgssEnigmaStone,
            Title = "Enigma Stone",
            Subtitle = "Unlock Pewter Museum Lati Twin Event",
            Description = "Adds the Enigma Stone to trigger Steven Stone's museum appraisal and summon Latios (HeartGold) or Latias (SoulSilver) in-game.",
            Generation = 4,
            Region = "Kanto / Johto",
            Legendary = "Latios / Latias",
            Accent = "#06B6D4",
            TargetSpeciesId = 381,
            InGameLocation = "Pewter City Museum (Steven Stone appraisal)",
            SupportedGameIds = ["heartgold", "soulsilver", "hgss"]
        },
        new()
        {
            Id = BwLibertyPass,
            Title = "Liberty Pass",
            Subtitle = "Unlock Liberty Garden Victini Event",
            Description = "Adds the Liberty Pass so you can board the Castelia City ferry to Liberty Garden and battle Victini in the lighthouse cellar in-game.",
            Generation = 5,
            Region = "Unova",
            Legendary = "Victini",
            Accent = "#EF4444",
            TargetSpeciesId = 494,
            InGameLocation = "Liberty Garden (Ferry from Castelia City pier)",
            SupportedGameIds = ["black", "white", "black2", "white2", "b2w2"]
        },
        new()
        {
            Id = OrasEonTicket,
            Title = "Eon Ticket",
            Subtitle = "Unlock Southern Island Lati Event",
            Description = "Adds the Eon Ticket to your Key Items in Omega Ruby and Alpha Sapphire to sail with Norman from Petalburg to Southern Island and confront the other Eon twin holding Soul Dew.",
            Generation = 6,
            Region = "Hoenn",
            Legendary = "Latios / Latias",
            Accent = "#F59E0B",
            TargetSpeciesId = 381,
            InGameLocation = "Southern Island (Ferry from Petalburg / Slateport with Norman)",
            SupportedGameIds = ["omegaruby", "alphasapphire", "oras"]
        },
        new()
        {
            Id = Gen7MagearnaDelivery,
            Title = "Magearna Delivery",
            Subtitle = "Hau'oli Antiquities Event",
            Description = "Delivers the Mythical Pokémon Magearna holding a Silver Bottle Cap to the deliveryman waiting at the Antiquities of the Ages store inside Hau'oli City Mall.",
            Generation = 7,
            Region = "Alola",
            Legendary = "Magearna",
            Accent = "#94A3B8",
            TargetSpeciesId = 801,
            InGameLocation = "Hau'oli City Mall (Antiquities of the Ages store deliveryman)",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"]
        },
        new()
        {
            Id = Gen7AshGreninjaDelivery,
            Title = "Ash-Greninja",
            Subtitle = "Battle Bond Demo Delivery",
            Description = "Delivers the Special Demo Battle Bond Greninja with Water Shuriken and Aerial Ace to the deliveryman waiting beside the counter in any Pokémon Center.",
            Generation = 7,
            Region = "Alola",
            Legendary = "Ash-Greninja",
            Accent = "#0284C7",
            TargetSpeciesId = 658,
            InGameLocation = "Any Pokémon Center (Deliveryman waiting by the counter)",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"]
        }
    ];

    public static IReadOnlyList<PokemonEventUnlockDto> All => Events;

    public static PokemonEventUnlockDto? Find(string? eventId) =>
        Events.FirstOrDefault(e => string.Equals(e.Id, eventId, StringComparison.OrdinalIgnoreCase));

    public static bool SupportsGame(PokemonEventUnlockDto definition, string gameId)
    {
        var key = NormalizeGameKey(gameId);
        return definition.SupportedGameIds.Any(s => IsGameKeyMatch(key, s));
    }

    public static bool IsGameKeyMatch(string key, string target)
    {
        if (target.Length <= 2)
        {
            return string.Equals(key, target, StringComparison.OrdinalIgnoreCase);
        }

        if (target is "sun" or "moon")
        {
            if (key.Contains("ultra", StringComparison.OrdinalIgnoreCase)) return false;
        }

        if (target is "black" or "white")
        {
            if (key.Contains("2", StringComparison.OrdinalIgnoreCase) || key.Contains("version2", StringComparison.OrdinalIgnoreCase))
            {
                return false;
            }
        }

        if (target is "red")
        {
            if (key.Contains("fire", StringComparison.OrdinalIgnoreCase)) return false;
        }

        if (target is "ruby")
        {
            if (key.Contains("omega", StringComparison.OrdinalIgnoreCase)) return false;
        }

        if (target is "sapphire")
        {
            if (key.Contains("alpha", StringComparison.OrdinalIgnoreCase)) return false;
        }

        if (target is "green")
        {
            if (key.Contains("leaf", StringComparison.OrdinalIgnoreCase)) return false;
        }

        return key.Contains(target, StringComparison.OrdinalIgnoreCase);
    }

    public static string NormalizeGameKey(string? gameId)
    {
        var key = gameId?.Trim().ToLowerInvariant() ?? string.Empty;
        return key
            .Replace("pokemon", "", StringComparison.Ordinal)
            .Replace("pokémon", "", StringComparison.Ordinal)
            .Replace("version", "", StringComparison.Ordinal)
            .Replace("-", "", StringComparison.Ordinal)
            .Replace("_", "", StringComparison.Ordinal)
            .Replace(" ", "", StringComparison.Ordinal);
    }
}

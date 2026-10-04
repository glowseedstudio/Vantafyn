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

    private static readonly IReadOnlyList<PokemonEventUnlockDto> Events =
    [
        new()
        {
            Id = CrystalGsBall,
            Title = "GS Ball",
            Subtitle = "Unlock the Ilex Forest Celebi event",
            Description = "Enables the Crystal GS Ball sequence so the player can receive the GS Ball and complete the shrine event in-game.",
            Generation = 2,
            Region = "Johto",
            Legendary = "Celebi",
            Accent = "#34D399",
            SupportedGameIds = ["crystal"]
        },
        new()
        {
            Id = EmeraldOldSeaMap,
            Title = "Old Sea Map",
            Subtitle = "Unlock Faraway Island",
            Description = "Adds the Old Sea Map and opens Faraway Island travel in Emerald so Mew can be hunted in-game.",
            Generation = 3,
            Region = "Hoenn",
            Legendary = "Mew",
            Accent = "#F472B6",
            SupportedGameIds = ["emerald"]
        },
        new()
        {
            Id = EmeraldAuroraTicket,
            Title = "AuroraTicket",
            Subtitle = "Unlock Birth Island",
            Description = "Adds the AuroraTicket and enables Birth Island travel in Emerald so Deoxys can be encountered in-game.",
            Generation = 3,
            Region = "Hoenn",
            Legendary = "Deoxys",
            Accent = "#38BDF8",
            SupportedGameIds = ["emerald"]
        },
        new()
        {
            Id = EmeraldMysticTicket,
            Title = "MysticTicket",
            Subtitle = "Unlock Navel Rock",
            Description = "Adds the MysticTicket and enables Navel Rock travel in Emerald for Lugia and Ho-Oh.",
            Generation = 3,
            Region = "Hoenn",
            Legendary = "Lugia & Ho-Oh",
            Accent = "#A78BFA",
            SupportedGameIds = ["emerald"]
        },
        new()
        {
            Id = FrlgAuroraTicket,
            Title = "AuroraTicket",
            Subtitle = "Unlock Birth Island",
            Description = "Adds the AuroraTicket and enables Birth Island travel in FireRed and LeafGreen.",
            Generation = 3,
            Region = "Kanto",
            Legendary = "Deoxys",
            Accent = "#38BDF8",
            SupportedGameIds = ["firered", "leafgreen"]
        },
        new()
        {
            Id = FrlgMysticTicket,
            Title = "MysticTicket",
            Subtitle = "Unlock Navel Rock",
            Description = "Adds the MysticTicket and enables Navel Rock travel in FireRed and LeafGreen.",
            Generation = 3,
            Region = "Kanto",
            Legendary = "Lugia & Ho-Oh",
            Accent = "#A78BFA",
            SupportedGameIds = ["firered", "leafgreen"]
        },
        new()
        {
            Id = RseEonTicket,
            Title = "Eon Ticket",
            Subtitle = "Unlock Southern Island",
            Description = "Adds the Eon Ticket and enables Southern Island travel where supported by Ruby, Sapphire, and Emerald saves.",
            Generation = 3,
            Region = "Hoenn",
            Legendary = "Latios / Latias",
            Accent = "#FBBF24",
            SupportedGameIds = ["ruby", "sapphire", "emerald"]
        }
    ];

    public static IReadOnlyList<PokemonEventUnlockDto> All => Events;

    public static PokemonEventUnlockDto? Find(string? eventId) =>
        Events.FirstOrDefault(e => string.Equals(e.Id, eventId, StringComparison.OrdinalIgnoreCase));

    public static bool SupportsGame(PokemonEventUnlockDto definition, string gameId)
    {
        var key = NormalizeGameKey(gameId);
        return definition.SupportedGameIds.Any(s => key.Contains(s, StringComparison.OrdinalIgnoreCase));
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

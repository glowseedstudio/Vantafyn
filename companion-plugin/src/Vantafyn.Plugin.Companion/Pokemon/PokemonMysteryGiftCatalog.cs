using System;
using System.Collections.Generic;
using System.Linq;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Curated historical archive of authentic promotional Pokémon Mystery Gift codes.
/// Supports both key item event distributions (Member Card, Oak's Letter, Azure Flute, Liberty Pass, etc.)
/// and authentic promotional Pokémon/Egg distributions (Manaphy Egg, Surfing Pikachu, Shiny Creation Trio, etc.).
/// </summary>
public static class PokemonMysteryGiftCatalog
{
    private static readonly IReadOnlyList<PokemonMysteryGiftDto> Gifts =
    [
        new()
        {
            Code = "LIBERTY-PASS",
            Aliases = ["VICTINI", "LIBERTYGARDEN", "LIBERTY"],
            Id = "bw-liberty-pass",
            Title = "Liberty Pass",
            Subtitle = "Liberty Garden Victini Event Pass",
            Description = "Authentic Nintendo Wi-Fi launch distribution for Pokémon Black and White. Grants the ferry pass from Castelia City pier to reach Liberty Garden.",
            Generation = 5,
            Region = "Unova",
            RewardType = "EventItem",
            TargetSpeciesId = 494,
            TargetSpeciesName = "Victini",
            Accent = "#EF4444",
            InGameInstructions = "The Liberty Pass is in your Key Items pocket! Head to the westernmost pier in Castelia City and board the ferry to Liberty Garden to confront Victini in the lighthouse basement.",
            SupportedGameIds = ["black", "white", "black2", "white2", "b2w2"],
            EventId = PokemonEventCatalog.BwLibertyPass
        },
        new()
        {
            Code = "MEMBER-CARD",
            Aliases = ["DARKRAI", "NEWMOON", "MEMBERCARD"],
            Id = "gen4-member-card",
            Title = "Member Card",
            Subtitle = "Newmoon Island Darkrai Pass",
            Description = "Authentic Wi-Fi distribution key card for Sinnoh. Opens the locked Harbor Inn in Canalave City to begin the nightmare voyage.",
            Generation = 4,
            Region = "Sinnoh",
            RewardType = "EventItem",
            TargetSpeciesId = 491,
            TargetSpeciesName = "Darkrai",
            Accent = "#A855F7",
            InGameInstructions = "The Member Card is delivered to your Key Items pocket and Mystery Gift is enabled! Visit the Harbor Inn in Canalave City, drift into sleep, and awaken on Newmoon Island.",
            SupportedGameIds = ["diamond", "pearl", "platinum"],
            EventId = PokemonEventCatalog.Gen4MemberCard
        },
        new()
        {
            Code = "OAKS-LETTER",
            Aliases = ["SHAYMIN", "SEABREAK", "OAKSLETTER"],
            Id = "gen4-oaks-letter",
            Title = "Oak's Letter",
            Subtitle = "Flower Paradise Shaymin Event",
            Description = "Authentic letter from Professor Oak inviting you to Route 224 to witness the white mirror stone and unlock Seabreak Path.",
            Generation = 4,
            Region = "Sinnoh",
            RewardType = "EventItem",
            TargetSpeciesId = 492,
            TargetSpeciesName = "Shaymin",
            Accent = "#10B981",
            InGameInstructions = "Oak's Letter is delivered to your Key Items! Head to the white stone at Route 224. Professor Oak will meet you there to open the blooming Seabreak Path to Flower Paradise.",
            SupportedGameIds = ["diamond", "pearl", "platinum"],
            EventId = PokemonEventCatalog.Gen4OaksLetter
        },
        new()
        {
            Code = "AZURE-FLUTE",
            Aliases = ["ARCEUS", "ORIGIN", "AZUREFLUTE"],
            Id = "gen4-azure-flute",
            Title = "Azure Flute",
            Subtitle = "Hall of Origin Arceus Event",
            Description = "The legendary, ethereal flute whose notes summon the glowing celestial staircase above Spear Pillar.",
            Generation = 4,
            Region = "Sinnoh",
            RewardType = "EventItem",
            TargetSpeciesId = 493,
            TargetSpeciesName = "Arceus",
            Accent = "#F59E0B",
            InGameInstructions = "The Azure Flute is in your Key Items pocket! Ascend Mount Coronet to Spear Pillar. Stand upon the fluted tile and play the flute to ascend the glowing staircase to the Hall of Origin.",
            SupportedGameIds = ["diamond", "pearl", "platinum"],
            EventId = PokemonEventCatalog.Gen4AzureFlute
        },
        new()
        {
            Code = "SECRET-KEY",
            Aliases = ["ROTOM", "APPLIANCE", "SECRETKEY"],
            Id = "platinum-secret-key",
            Title = "Secret Key",
            Subtitle = "Rotom Appliance Room Key",
            Description = "Authentic Wi-Fi Mystery Gift key to unlock Charon's secret room in Team Galactic Eterna Building.",
            Generation = 4,
            Region = "Sinnoh",
            RewardType = "EventItem",
            TargetSpeciesId = 479,
            TargetSpeciesName = "Rotom",
            Accent = "#F97316",
            InGameInstructions = "The Secret Key is in your Key Items! Enter the Galactic Eterna Building ground floor and interact with the wall next to the bookshelf to unlock Rotom's 5 appliance forms.",
            SupportedGameIds = ["platinum"],
            EventId = PokemonEventCatalog.PlatinumSecretKey
        },
        new()
        {
            Code = "ENIGMA-STONE",
            Aliases = ["SOUL-DEW", "SOULDEW", "ENIGMASTONE", "LATIOS", "LATIAS"],
            Id = "hgss-enigma-stone",
            Title = "Enigma Stone",
            Subtitle = "Pewter Museum Lati Twin Event",
            Description = "Authentic Wi-Fi distribution stone for HeartGold and SoulSilver containing the crystallised essence of Soul Dew.",
            Generation = 4,
            Region = "Kanto / Johto",
            RewardType = "EventItem",
            TargetSpeciesId = 381,
            TargetSpeciesName = "Latios / Latias",
            Accent = "#06B6D4",
            InGameInstructions = "The Enigma Stone is in your Key Items! Visit the Pewter City Museum of Science where Steven Stone will appraise it and summon Latios (HeartGold) or Latias (SoulSilver).",
            SupportedGameIds = ["heartgold", "soulsilver", "hgss"],
            EventId = PokemonEventCatalog.HgssEnigmaStone
        },
        new()
        {
            Code = "MANAPHY-EGG",
            Aliases = ["MANAPHY", "RANGER-EGG", "RANGEREGG"],
            Id = "gen4-manaphy-egg",
            Title = "Manaphy Egg",
            Subtitle = "Pokémon Ranger Special Mission Egg",
            Description = "The historic special mission distribution egg transferred from Fiore to Sinnoh and Johto. Hatches into the Prince of the Sea.",
            Generation = 4,
            Region = "Sinnoh / Johto",
            RewardType = "Egg",
            TargetSpeciesId = 490,
            TargetSpeciesName = "Manaphy",
            Accent = "#0284C7",
            InGameInstructions = "The Manaphy Egg has arrived in your collection! Visit any Poké Mart delivery man or withdraw it from your Personal Vault to your party. Travel across the region until it hatches!",
            SupportedGameIds = ["diamond", "pearl", "platinum", "heartgold", "soulsilver", "hgss"],
            OriginalTrainer = "RANGER",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "SURF-PIKA",
            Aliases = ["PIKACHU-SURF", "SURFINGPIKACHU", "BATTLE-REVOLUTION", "PKTOPIA"],
            Id = "gen4-surf-pikachu",
            Title = "Surfing Pikachu",
            Subtitle = "Pokémon Battle Revolution Special Gift",
            Description = "The iconic Surfing Pikachu reward from Pokémon Battle Revolution. Holds a Light Ball and knows Surf and Volt Tackle.",
            Generation = 4,
            Region = "Sinnoh / Johto",
            RewardType = "Pokemon",
            TargetSpeciesId = 25,
            TargetSpeciesName = "Pikachu",
            Accent = "#FBBF24",
            InGameInstructions = "Surfing Pikachu has arrived in your Personal Vault! Withdraw it to your party or box to command Surf and Volt Tackle with Light Ball boost.",
            SupportedGameIds = ["diamond", "pearl", "platinum", "heartgold", "soulsilver", "hgss"],
            OriginalTrainer = "PKTOPIA",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "SUM2013-DIALGA",
            Aliases = ["SHINY-DIALGA", "SUM2013DIALGA", "DIALGA"],
            Id = "gen5-sum2013-dialga",
            Title = "Shiny Dialga (SUM2013)",
            Subtitle = "Summer 2013 Shiny Creation Trio",
            Description = "The GameStop & GAME distribution Shiny Dialga celebrating the legendary rulers of time and space in Unova.",
            Generation = 5,
            Region = "Unova",
            RewardType = "Pokemon",
            TargetSpeciesId = 483,
            TargetSpeciesName = "Dialga",
            Accent = "#38BDF8",
            IsShiny = true,
            InGameInstructions = "Shiny Dialga (SUM2013) has arrived in your Personal Vault! Withdraw it to your party or PC box to command the flow of time with Roar of Time and Draco Meteor.",
            SupportedGameIds = ["black", "white", "black2", "white2", "b2w2"],
            OriginalTrainer = "SUM2013",
            RibbonName = "Souvenir Ribbon"
        },
        new()
        {
            Code = "SUM2013-PALKIA",
            Aliases = ["SHINY-PALKIA", "SUM2013PALKIA", "PALKIA"],
            Id = "gen5-sum2013-palkia",
            Title = "Shiny Palkia (SUM2013)",
            Subtitle = "Summer 2013 Shiny Creation Trio",
            Description = "The GameStop & GAME distribution Shiny Palkia commanding the fabric of spatial dimensions in Unova.",
            Generation = 5,
            Region = "Unova",
            RewardType = "Pokemon",
            TargetSpeciesId = 484,
            TargetSpeciesName = "Palkia",
            Accent = "#EC4899",
            IsShiny = true,
            InGameInstructions = "Shiny Palkia (SUM2013) has arrived in your Personal Vault! Withdraw it to your party or PC box to rend space with Spacial Rend and Hydro Pump.",
            SupportedGameIds = ["black", "white", "black2", "white2", "b2w2"],
            OriginalTrainer = "SUM2013",
            RibbonName = "Souvenir Ribbon"
        },
        new()
        {
            Code = "SUM2013-GIRATINA",
            Aliases = ["SHINY-GIRATINA", "SUM2013GIRATINA", "GIRATINA"],
            Id = "gen5-sum2013-giratina",
            Title = "Shiny Giratina (SUM2013)",
            Subtitle = "Summer 2013 Shiny Creation Trio",
            Description = "The GameStop & GAME distribution Shiny Giratina reigning over the Distortion World with Shadow Force.",
            Generation = 5,
            Region = "Unova",
            RewardType = "Pokemon",
            TargetSpeciesId = 487,
            TargetSpeciesName = "Giratina",
            Accent = "#E11D48",
            IsShiny = true,
            InGameInstructions = "Shiny Giratina (SUM2013) has arrived in your Personal Vault! Withdraw it to your party or PC box to unleash Shadow Force and Dragon Pulse.",
            SupportedGameIds = ["black", "white", "black2", "white2", "b2w2"],
            OriginalTrainer = "SUM2013",
            RibbonName = "Souvenir Ribbon"
        },
        new()
        {
            Code = "GAMESTP-PICHU",
            Aliases = ["SHINY-PICHU", "SPIKY-EAR", "GAMESTPPICHU"],
            Id = "gen4-gamestp-pichu",
            Title = "Pikachu-Colored Pichu",
            Subtitle = "Ilex Forest Spiky-Eared Pichu Trigger",
            Description = "The shiny Pichu distribution that unlocks the special Spiky-Eared Pichu encounter at the Ilex Forest Shrine in HeartGold & SoulSilver.",
            Generation = 4,
            Region = "Johto",
            RewardType = "Pokemon",
            TargetSpeciesId = 172,
            TargetSpeciesName = "Pichu",
            Accent = "#FACC15",
            IsShiny = true,
            InGameInstructions = "Pikachu-Colored Pichu is delivered! Place Pichu first in your party and interact with the Ilex Forest Shrine in HGSS. The Spiky-Eared Pichu will emerge and join your team!",
            SupportedGameIds = ["heartgold", "soulsilver", "hgss", "diamond", "pearl", "platinum"],
            OriginalTrainer = "GAMESTP",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "ASH-PIKACHU",
            Aliases = ["ASH-PIKA", "ASHPIKACHU", "SATOSHI-PIKACHU"],
            Id = "gen4-ash-pikachu",
            Title = "Ash's Pikachu",
            Subtitle = "Ash Ketchum Promotional Event",
            Description = "Ash's iconic partner holding a Light Ball, knowing Volt Tackle, Iron Tail, Quick Attack, and Thunderbolt.",
            Generation = 4,
            Region = "Sinnoh / Johto",
            RewardType = "Pokemon",
            TargetSpeciesId = 25,
            TargetSpeciesName = "Pikachu",
            Accent = "#EAB308",
            InGameInstructions = "Ash's Pikachu has arrived in your Personal Vault! Withdraw it to your game to battle with maximum friendship and Light Ball boost.",
            SupportedGameIds = ["diamond", "pearl", "platinum", "heartgold", "soulsilver", "hgss", "black", "white"],
            OriginalTrainer = "Ash",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "GS-BALL",
            Aliases = ["CELEBI", "GSBALL", "KURT-GSBALL"],
            Id = "crystal-gs-ball",
            Title = "GS Ball",
            Subtitle = "Ilex Forest Celebi Event",
            Description = "The mysterious golden and silver Poké Ball from the Pokémon Mobile System GB distribution.",
            Generation = 2,
            Region = "Johto",
            RewardType = "EventItem",
            TargetSpeciesId = 251,
            TargetSpeciesName = "Celebi",
            Accent = "#34D399",
            InGameInstructions = "The GS Ball sequence is activated in your Crystal save! Deliver the GS Ball to Kurt in Azalea Town, wait 24 hours, then bring it to the Ilex Forest Shrine.",
            SupportedGameIds = ["crystal"],
            EventId = PokemonEventCatalog.CrystalGsBall
        },
        new()
        {
            Code = "OLD-SEA-MAP",
            Aliases = ["FARAWAY-MEW", "OLDSEAMAP", "MEW", "FARAWAY"],
            Id = "emerald-old-sea-map",
            Title = "Old Sea Map",
            Subtitle = "Faraway Island Mew Event",
            Description = "The faded nautical chart leading to the uncharted jungles of Faraway Island where Mew plays hide-and-seek.",
            Generation = 3,
            Region = "Hoenn",
            RewardType = "EventItem",
            TargetSpeciesId = 151,
            TargetSpeciesName = "Mew",
            Accent = "#F472B6",
            InGameInstructions = "The Old Sea Map is in your Key Items! Board the S.S. Tidal at Lilycove City Harbor to set sail for Faraway Island and catch Mew.",
            SupportedGameIds = ["emerald"],
            EventId = PokemonEventCatalog.EmeraldOldSeaMap
        },
        new()
        {
            Code = "AURORA-TICKET",
            Aliases = ["BIRTH-ISLAND", "AURORATICKET", "DEOXYS"],
            Id = "gen3-aurora-ticket",
            Title = "AuroraTicket",
            Subtitle = "Birth Island Deoxys Event",
            Description = "The mystical Sevii Islands ferry pass to Birth Island to solve the triangular monolith puzzle.",
            Generation = 3,
            Region = "Kanto / Hoenn",
            RewardType = "EventItem",
            TargetSpeciesId = 386,
            TargetSpeciesName = "Deoxys",
            Accent = "#38BDF8",
            InGameInstructions = "The AuroraTicket is in your Key Items! Board the Seagallop Ferry from Vermilion City (FRLG) or S.S. Tidal from Lilycove (Emerald) to visit Birth Island.",
            SupportedGameIds = ["emerald", "firered", "leafgreen"],
            EventId = PokemonEventCatalog.EmeraldAuroraTicket
        },
        new()
        {
            Code = "MYSTIC-TICKET",
            Aliases = ["NAVEL-ROCK", "MYSTI法施行TICKET", "LUGIA", "HO-OH", "MYSTICTICKET"],
            Id = "gen3-mystic-ticket",
            Title = "MysticTicket",
            Subtitle = "Navel Rock Lugia & Ho-Oh Event",
            Description = "The ferry ticket to the solitary volcanic island of Navel Rock housing Lugia in the depths and Ho-Oh at the summit.",
            Generation = 3,
            Region = "Kanto / Hoenn",
            RewardType = "EventItem",
            TargetSpeciesId = 249,
            TargetSpeciesName = "Lugia & Ho-Oh",
            Accent = "#A78BFA",
            InGameInstructions = "The MysticTicket is in your Key Items! Board the ferry to Navel Rock to confront both legendary bird masters in-game.",
            SupportedGameIds = ["emerald", "firered", "leafgreen"],
            EventId = PokemonEventCatalog.EmeraldMysticTicket
        },
        new()
        {
            Code = "EON-TICKET",
            Aliases = ["SOUTHERN-ISLAND", "EONTICKET", "SOUTHERN"],
            Id = "rse-eon-ticket",
            Title = "Eon Ticket",
            Subtitle = "Southern Island Lati Event",
            Description = "The E-Reader ferry ticket granting passage to Southern Island to encounter Latios or Latias holding the Soul Dew.",
            Generation = 3,
            Region = "Hoenn",
            RewardType = "EventItem",
            TargetSpeciesId = 381,
            TargetSpeciesName = "Latios / Latias",
            Accent = "#FBBF24",
            InGameInstructions = "The Eon Ticket is in your Key Items! Board the ferry from Lilycove City or Slateport City to sail to Southern Island.",
            SupportedGameIds = ["ruby", "sapphire", "emerald"],
            EventId = PokemonEventCatalog.RseEonTicket
        }
    ];

    public static IReadOnlyList<PokemonMysteryGiftDto> All => Gifts;

    public static PokemonMysteryGiftDto? Find(string? code)
    {
        if (string.IsNullOrWhiteSpace(code)) return null;
        var normalized = NormalizeCode(code);
        return Gifts.FirstOrDefault(g =>
            NormalizeCode(g.Code) == normalized ||
            NormalizeCode(g.Id) == normalized ||
            g.Aliases.Any(a => NormalizeCode(a) == normalized));
    }

    public static bool SupportsGame(PokemonMysteryGiftDto gift, string gameId)
    {
        var key = PokemonEventCatalog.NormalizeGameKey(gameId);
        return gift.SupportedGameIds.Any(s => key.Contains(s, StringComparison.OrdinalIgnoreCase));
    }

    public static string NormalizeCode(string? code)
    {
        if (string.IsNullOrWhiteSpace(code)) return string.Empty;
        return code.Trim()
            .ToUpperInvariant()
            .Replace("-", "", StringComparison.Ordinal)
            .Replace("_", "", StringComparison.Ordinal)
            .Replace(" ", "", StringComparison.Ordinal);
    }

    /// <summary>
    /// Creates an authentic PokemonVaultEntry corresponding to this gift.
    /// </summary>
    public static PokemonVaultEntry CreateVaultEntry(PokemonMysteryGiftDto gift, string originGame, int generation)
    {
        var entry = new PokemonVaultEntry
        {
            Id = $"mg_{gift.Id}_{Guid.NewGuid():N}",
            Species = gift.TargetSpeciesName.Split(['/', '&']).First().Trim(),
            SpeciesId = gift.TargetSpeciesId,
            Nickname = gift.TargetSpeciesName.Split(['/', '&']).First().Trim(),
            IsShiny = gift.IsShiny,
            Generation = generation,
            OriginGame = originGame,
            OriginalTrainer = gift.OriginalTrainer ?? "Fateful Encounter",
            OriginalTrainerId = gift.Generation == 5 ? "08303" : "06257",
            CurrentLocation = gift.RewardType == "Egg" ? "Fateful Encounter Egg" : "Mystery Gift Delivery",
            CreatedAtUtc = DateTimeOffset.UtcNow,
            UpdatedAtUtc = DateTimeOffset.UtcNow,
            LegalityStatus = "valid"
        };

        var details = new PokemonDetailsDto
        {
            Nature = gift.IsShiny ? "Timid" : "Hardy",
            Ability = gift.TargetSpeciesId == 25 ? "Static" : (gift.TargetSpeciesId == 490 ? "Hydration" : "Pressure"),
            Pokeball = gift.RewardType == "Egg" ? "Poké Ball" : "Cherish Ball",
            Friendship = 255,
            LegalityStatus = "valid"
        };

        if (gift.Code.Contains("SURF-PIKA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 50;
            details.HeldItem = "Light Ball";
            details.Moves = ["Surf", "Volt Tackle", "Tail Whip", "Thunder Wave"];
        }
        else if (gift.Code.Contains("ASH-PIKACHU", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 50;
            details.HeldItem = "Light Ball";
            details.Moves = ["Volt Tackle", "Iron Tail", "Quick Attack", "Thunderbolt"];
        }
        else if (gift.Code.Contains("MANAPHY-EGG", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 1;
            entry.Nickname = "Manaphy Egg";
            details.Moves = ["Tail Glow", "Bubble", "Water Sport"];
        }
        else if (gift.Code.Contains("GAMESTP-PICHU", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 30;
            details.HeldItem = "Everstone";
            details.Moves = ["Charge", "Volt Tackle", "Endeavor", "Endure"];
        }
        else if (gift.Code.Contains("DIALGA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Moves = ["Dragon Pulse", "Draco Meteor", "Aura Sphere", "Roar of Time"];
        }
        else if (gift.Code.Contains("PALKIA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Moves = ["Hydro Pump", "Draco Meteor", "Aura Sphere", "Spacial Rend"];
        }
        else if (gift.Code.Contains("GIRATINA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Moves = ["Dragon Pulse", "Dragon Claw", "Aura Sphere", "Shadow Force"];
        }
        else
        {
            entry.Level = 50;
        }

        entry.Details = details;
        return entry;
    }
}

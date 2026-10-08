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
        },
        new()
        {
            Code = "ORAS-EON-TICKET",
            Aliases = ["EON-TICKET-ORAS", "SOUTHERN-ORAS", "EON-TICKET"],
            Id = "oras-eon-ticket",
            Title = "Eon Ticket (ORAS)",
            Subtitle = "Southern Island Lati Event",
            Description = "The serial code and StreetPass distribution ticket granting passage to Southern Island in Omega Ruby and Alpha Sapphire to catch the other Eon twin.",
            Generation = 6,
            Region = "Hoenn",
            RewardType = "EventItem",
            TargetSpeciesId = 381,
            TargetSpeciesName = "Latios / Latias",
            Accent = "#F59E0B",
            InGameInstructions = "The Eon Ticket is in your Key Items! Speak to Norman in Petalburg Gym, then take the ferry to Southern Island.",
            SupportedGameIds = ["omegaruby", "alphasapphire", "oras"],
            EventId = PokemonEventCatalog.OrasEonTicket
        },
        new()
        {
            Code = "XY-TORCHIC",
            Aliases = ["TORCHIC", "BLAZIKENITE", "XYTORCHIC"],
            Id = "gen6-xy-torchic",
            Title = "Speed Boost Torchic",
            Subtitle = "XY Worldwide Launch Gift",
            Description = "The worldwide launch event Torchic holding the Blazikenite Mega Stone, featuring the Hidden Ability Speed Boost.",
            Generation = 6,
            Region = "Kalos",
            RewardType = "Pokemon",
            TargetSpeciesId = 255,
            TargetSpeciesName = "Torchic",
            Accent = "#F97316",
            InGameInstructions = "Torchic (holding Blazikenite) has arrived in your Personal Vault! Withdraw it to your party or PC box to unleash Speed Boost and Mega Blaziken.",
            SupportedGameIds = ["x", "y", "xy"],
            OriginalTrainer = "XY",
            RibbonName = "Premier Ribbon"
        },
        new()
        {
            Code = "ORAS-BELDUM",
            Aliases = ["SHINY-BELDUM", "METAGROSSITE", "ORASBELDUM", "STEVEN-BELDUM"],
            Id = "gen6-oras-beldum",
            Title = "Shiny Beldum (Steven's)",
            Subtitle = "ORAS Launch Distribution",
            Description = "Steven Stone's commemorative Shiny Beldum holding the Metagrossite Mega Stone, distributed at the launch of Omega Ruby and Alpha Sapphire.",
            Generation = 6,
            Region = "Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 374,
            TargetSpeciesName = "Beldum",
            Accent = "#38BDF8",
            IsShiny = true,
            InGameInstructions = "Shiny Beldum (holding Metagrossite) has arrived in your Personal Vault! Withdraw it to your party to evolve into Shiny Mega Metagross.",
            SupportedGameIds = ["omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "Steven",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "HOPE-DIANCIE",
            Aliases = ["DIANCIE", "HOPEDIANCIE", "NOV2014-DIANCIE"],
            Id = "gen6-hope-diancie",
            Title = "Mythical Diancie (Hope)",
            Subtitle = "Jewel Pokémon Wi-Fi Distribution",
            Description = "The Mythical Jewel Pokémon Diancie, capable of Mega Evolving into Mega Diancie.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 719,
            TargetSpeciesName = "Diancie",
            Accent = "#F472B6",
            InGameInstructions = "Mythical Diancie has arrived in your Personal Vault! Withdraw it to command Diamond Storm and Moonblast.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "Hope",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "HOOPA-UNBOUND",
            Aliases = ["HOOPA", "MAC-HOOPA", "ALEXANDER-HOOPA", "PRISON-BOTTLE"],
            Id = "gen6-hoopa",
            Title = "Mythical Hoopa (Alexander)",
            Subtitle = "Mischief Pokémon Event",
            Description = "The Mythical Mischief Pokémon Hoopa, capable of using the Prison Bottle to transform into Hoopa Unbound.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 720,
            TargetSpeciesName = "Hoopa",
            Accent = "#8B5CF6",
            InGameInstructions = "Mythical Hoopa has arrived in your Personal Vault! Withdraw it to command Hyperspace Hole and Psychic.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "Alexander",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "HELEN-VOLCANION",
            Aliases = ["VOLCANION", "HELEN", "NEBULA"],
            Id = "gen6-helen-volcanion",
            Title = "Mythical Volcanion (Helen)",
            Subtitle = "Steam Pokémon Event",
            Description = "The dual Fire/Water Mythical Steam Pokémon Volcanion, wielding Steam Eruption.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 721,
            TargetSpeciesName = "Volcanion",
            Accent = "#EF4444",
            InGameInstructions = "Mythical Volcanion has arrived in your Personal Vault! Withdraw it to scorch and scald foes with Steam Eruption.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "Helen",
            RibbonName = "Wishing Ribbon"
        },
        new()
        {
            Code = "GALILEO-RAYQUAZA",
            Aliases = ["SHINY-RAYQUAZA", "GALILEO", "DRAGON-ASCENT"],
            Id = "gen6-galileo-rayquaza",
            Title = "Shiny Rayquaza (Galileo)",
            Subtitle = "Ancient Skies Shiny Event",
            Description = "The black Shiny Sky High Pokémon Rayquaza knowing its signature Mega Evolution move Dragon Ascent.",
            Generation = 6,
            Region = "Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 384,
            TargetSpeciesName = "Rayquaza",
            Accent = "#10B981",
            IsShiny = true,
            InGameInstructions = "Shiny Rayquaza has arrived in your Personal Vault! Withdraw it to unleash Dragon Ascent and Mega Evolve without a Mega Stone.",
            SupportedGameIds = ["omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "Galileo",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "GF-MEW",
            Aliases = ["GFMEW", "20TH-MEW"],
            Id = "gen6-gf-mew",
            Title = "Mythical Mew (GF 20th)",
            Subtitle = "20th Anniversary Mythical Distribution",
            Description = "The Mythical New Species Pokémon Mew distributed to celebrate 20 years of Pokémon.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 151,
            TargetSpeciesName = "Mew",
            Accent = "#F472B6",
            InGameInstructions = "Mythical Mew has arrived in your Personal Vault! Withdraw it to your party or PC box.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "GF",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "GF-CELEBI",
            Aliases = ["GFCELEBI", "20TH-CELEBI"],
            Id = "gen6-gf-celebi",
            Title = "Mythical Celebi (GF 20th)",
            Subtitle = "20th Anniversary Mythical Distribution",
            Description = "The Mythical Time Travel Pokémon Celebi distributed to celebrate 20 years of Pokémon.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 251,
            TargetSpeciesName = "Celebi",
            Accent = "#34D399",
            InGameInstructions = "Mythical Celebi has arrived in your Personal Vault! Withdraw it to your party or PC box.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "GF",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "GF-JIRACHI",
            Aliases = ["GFJIRACHI", "20TH-JIRACHI"],
            Id = "gen6-gf-jirachi",
            Title = "Mythical Jirachi (GF 20th)",
            Subtitle = "20th Anniversary Mythical Distribution",
            Description = "The Mythical Wish Pokémon Jirachi distributed to celebrate 20 years of Pokémon.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 385,
            TargetSpeciesName = "Jirachi",
            Accent = "#FACC15",
            InGameInstructions = "Mythical Jirachi has arrived in your Personal Vault! Withdraw it to grant wishes with Serene Grace.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "GF",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "GF-DARKRAI",
            Aliases = ["GFDARKRAI", "20TH-DARKRAI"],
            Id = "gen6-gf-darkrai",
            Title = "Mythical Darkrai (GF 20th)",
            Subtitle = "20th Anniversary Mythical Distribution",
            Description = "The Mythical Pitch-Black Pokémon Darkrai distributed to celebrate 20 years of Pokémon.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 491,
            TargetSpeciesName = "Darkrai",
            Accent = "#64748B",
            InGameInstructions = "Mythical Darkrai has arrived in your Personal Vault! Withdraw it to induce nightmares with Dark Void.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "GF",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "GF-ARCEUS",
            Aliases = ["GFARCEUS", "20TH-ARCEUS"],
            Id = "gen6-gf-arceus",
            Title = "Mythical Arceus (GF 20th)",
            Subtitle = "20th Anniversary Mythical Distribution",
            Description = "The Mythical Alpha Pokémon Arceus distributed to celebrate 20 years of Pokémon.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 493,
            TargetSpeciesName = "Arceus",
            Accent = "#F59E0B",
            InGameInstructions = "Mythical Arceus has arrived in your Personal Vault! Withdraw it to unleash Judgment across all 18 types.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "GF",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "GF-GENESECT",
            Aliases = ["GFGENESECT", "20TH-GENESECT"],
            Id = "gen6-gf-genesect",
            Title = "Mythical Genesect (GF 20th)",
            Subtitle = "20th Anniversary Mythical Distribution",
            Description = "The Mythical Paleozoic Pokémon Genesect distributed to celebrate 20 years of Pokémon.",
            Generation = 6,
            Region = "Kalos / Hoenn",
            RewardType = "Pokemon",
            TargetSpeciesId = 649,
            TargetSpeciesName = "Genesect",
            Accent = "#8B5CF6",
            InGameInstructions = "Mythical Genesect has arrived in your Personal Vault! Withdraw it to fire Techno Blast with drive enhancements.",
            SupportedGameIds = ["x", "y", "xy", "omegaruby", "alphasapphire", "oras"],
            OriginalTrainer = "GF",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "MAGEARNA-QR",
            Aliases = ["MAGEARNA", "MAGEARNAQR", "HAUOLI-MAGEARNA"],
            Id = "gen7-magearna-qr",
            Title = "Mythical Magearna",
            Subtitle = "Hau'oli Antiquities Delivery",
            Description = "The Artificial Pokémon Magearna created 500 years ago, holding a Silver Bottle Cap and knowing Fleur Cannon.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 801,
            TargetSpeciesName = "Magearna",
            Accent = "#94A3B8",
            InGameInstructions = "Mythical Magearna has arrived in your Personal Vault! Withdraw it to command Fleur Cannon with Soul-Heart.",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"],
            EventId = PokemonEventCatalog.Gen7MagearnaDelivery,
            OriginalTrainer = "QR Event",
            RibbonName = "Wishing Ribbon"
        },
        new()
        {
            Code = "ASH-GRENINJA",
            Aliases = ["BATTLE-BOND", "ASHGRENINJA", "DEMO-GRENINJA"],
            Id = "gen7-ash-greninja",
            Title = "Ash-Greninja",
            Subtitle = "Special Demo Version Gift",
            Description = "The bond-phenomenon Greninja transferred from the Special Demo Version featuring the exclusive Ability Battle Bond.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 658,
            TargetSpeciesName = "Greninja",
            Accent = "#0284C7",
            InGameInstructions = "Ash-Greninja has arrived in your Personal Vault! Withdraw it to unleash Battle Bond and powered-up Water Shurikens.",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"],
            EventId = PokemonEventCatalog.Gen7AshGreninjaDelivery,
            OriginalTrainer = "Ash",
            RibbonName = "Souvenir Ribbon"
        },
        new()
        {
            Code = "SUNMOON-MUNCHLAX",
            Aliases = ["MUNCHLAX", "SNORLIUM", "PULVERIZING-PANCAKE"],
            Id = "gen7-sunmoon-munchlax",
            Title = "Early Adopter Munchlax",
            Subtitle = "Sun & Moon Launch Special",
            Description = "The early purchase Munchlax holding the exclusive Snorlium Z crystal and knowing Happy Hour and Hold Back.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 446,
            TargetSpeciesName = "Munchlax",
            Accent = "#0D9488",
            InGameInstructions = "Munchlax (holding Snorlium Z) has arrived in your Personal Vault! Evolve into Snorlax to unleash Pulverizing Pancake.",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Early Adopter",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "DUSK-ROCKRUFF",
            Aliases = ["ROCKRUFF", "DUSKROCKRUFF", "OWN-TEMPO"],
            Id = "gen7-dusk-rockruff",
            Title = "Own Tempo Rockruff",
            Subtitle = "Ultra Sun & Ultra Moon Launch Gift",
            Description = "The special Rockruff possessing the Own Tempo ability, allowing it to evolve into Dusk Form Lycanroc at twilight.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 744,
            TargetSpeciesName = "Rockruff",
            Accent = "#EA580C",
            InGameInstructions = "Own Tempo Rockruff has arrived in your Personal Vault! Train between 5:00 PM and 5:59 PM to achieve Dusk Form Lycanroc.",
            SupportedGameIds = ["ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Ultra Launch",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "MT-TENSEI-MARSHADOW",
            Aliases = ["MARSHADOW", "MARSHADIUM", "MTTENSEI"],
            Id = "gen7-marshadow",
            Title = "Mythical Marshadow",
            Subtitle = "Gloomdweller Marshadium Z Event",
            Description = "The Mythical Gloomdweller Pokémon Marshadow holding its exclusive Marshadium Z crystal to perform Soul-Stealing 7-Star Strike.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 802,
            TargetSpeciesName = "Marshadow",
            Accent = "#475569",
            InGameInstructions = "Mythical Marshadow (holding Marshadium Z) has arrived in your Personal Vault! Unleash Soul-Stealing 7-Star Strike in battle.",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Mt. Tensei",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "FULA-CITY-ZERAORA",
            Aliases = ["ZERAORA", "FULACITY", "THUNDERCLAP"],
            Id = "gen7-zeraora",
            Title = "Mythical Zeraora",
            Subtitle = "Thunderclap Pokémon Event",
            Description = "The Mythical Thunderclap Pokémon Zeraora wielding Plasma Fists and holding an Air Balloon.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 807,
            TargetSpeciesName = "Zeraora",
            Accent = "#FACC15",
            InGameInstructions = "Mythical Zeraora has arrived in your Personal Vault! Withdraw it to unleash electrifying Plasma Fists.",
            SupportedGameIds = ["ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Fula City",
            RibbonName = "Wishing Ribbon"
        },
        new()
        {
            Code = "MELEMELE-TAPUKOKO",
            Aliases = ["SHINY-TAPUKOKO", "MELEMELE", "TAPU-KOKO"],
            Id = "gen7-shiny-tapukoko",
            Title = "Shiny Tapu Koko (Melemele)",
            Subtitle = "Island Guardian Shiny Distribution",
            Description = "The guardian deity of Melemele Island appearing in its dazzling black Shiny coloration holding an Electric Seed.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 785,
            TargetSpeciesName = "Tapu Koko",
            Accent = "#FBBF24",
            IsShiny = true,
            InGameInstructions = "Shiny Tapu Koko has arrived in your Personal Vault! Withdraw it to command Electric Surge and Nature's Madness.",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Melemele",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "ULTRA-POIPOLE",
            Aliases = ["SHINY-POIPOLE", "POIPOLE", "ULTRAPOIPOLE"],
            Id = "gen7-shiny-poipole",
            Title = "Shiny Poipole (Ultra)",
            Subtitle = "Ultra Space Poison Pin Event",
            Description = "The white and gold Shiny Ultra Beast Poipole (UB Adhesive) distributed to celebrate Ultra Sun and Ultra Moon.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 803,
            TargetSpeciesName = "Poipole",
            Accent = "#A855F7",
            IsShiny = true,
            InGameInstructions = "Shiny Poipole has arrived in your Personal Vault! Teach it Dragon Pulse to evolve into Shiny Naganadel.",
            SupportedGameIds = ["ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Ultra",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "PIKACHU20",
            Aliases = ["CAP-PIKACHU", "ASH-CAP", "PIKASHUNIUM"],
            Id = "gen7-cap-pikachu",
            Title = "Original Cap Pikachu",
            Subtitle = "I Choose You! 20th Anniversary Gift",
            Description = "Ash's partner wearing the original Indigo League hat holding the exclusive Pikashunium Z crystal to unleash 10,000,000 Volt Thunderbolt.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 25,
            TargetSpeciesName = "Pikachu",
            Accent = "#EAB308",
            InGameInstructions = "Original Cap Pikachu (holding Pikashunium Z) has arrived in your Personal Vault! Fire the 10,000,000 Volt Thunderbolt Z-Move.",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Ash",
            RibbonName = "Wishing Ribbon"
        },
        new()
        {
            Code = "ECLIPSE-SOLGALEO",
            Aliases = ["SHINY-SOLGALEO", "ECLIPSE-SUN", "SOLGALEO"],
            Id = "gen7-shiny-solgaleo",
            Title = "Shiny Solgaleo (Eclipse)",
            Subtitle = "Secret Shiny Legendary Distribution",
            Description = "The dazzling crimson Shiny Solgaleo holding a Master Ball distributed via the Pokémon Pass app.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 791,
            TargetSpeciesName = "Solgaleo",
            Accent = "#EF4444",
            IsShiny = true,
            InGameInstructions = "Shiny Solgaleo (holding Master Ball) has arrived in your Personal Vault! Command Sunsteel Strike in battle.",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Eclipse",
            RibbonName = "Classic Ribbon"
        },
        new()
        {
            Code = "ECLIPSE-LUNALA",
            Aliases = ["SHINY-LUNALA", "ECLIPSE-MOON", "LUNALA"],
            Id = "gen7-shiny-lunala",
            Title = "Shiny Lunala (Eclipse)",
            Subtitle = "Secret Shiny Legendary Distribution",
            Description = "The blood-moon crimson Shiny Lunala holding a Master Ball distributed via the Pokémon Pass app.",
            Generation = 7,
            Region = "Alola",
            RewardType = "Pokemon",
            TargetSpeciesId = 792,
            TargetSpeciesName = "Lunala",
            Accent = "#8B5CF6",
            IsShiny = true,
            InGameInstructions = "Shiny Lunala (holding Master Ball) has arrived in your Personal Vault! Command Moongeist Beam in battle.",
            SupportedGameIds = ["sun", "moon", "ultrasun", "ultramoon", "usum"],
            OriginalTrainer = "Eclipse",
            RibbonName = "Classic Ribbon"
        }
    ];

    public static IReadOnlyList<PokemonMysteryGiftDto> All => Gifts;

    public static PokemonMysteryGiftDto? Find(string? code, string? gameId = null)
    {
        if (string.IsNullOrWhiteSpace(code)) return null;
        var normalized = NormalizeCode(code);
        var matches = Gifts.Where(g =>
            NormalizeCode(g.Code) == normalized ||
            NormalizeCode(g.Id) == normalized ||
            g.Aliases.Any(a => NormalizeCode(a) == normalized)).ToList();

        if (matches.Count == 0) return null;
        if (matches.Count == 1 || string.IsNullOrWhiteSpace(gameId)) return matches[0];

        var compatible = matches.FirstOrDefault(g => SupportsGame(g, gameId));
        return compatible ?? matches[0];
    }

    public static bool SupportsGame(PokemonMysteryGiftDto gift, string gameId)
    {
        var key = PokemonEventCatalog.NormalizeGameKey(gameId);
        return gift.SupportedGameIds.Any(s => PokemonEventCatalog.IsGameKeyMatch(key, s));
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
            OriginalTrainerId = gift.Generation >= 6 ? "02276" : (gift.Generation == 5 ? "08303" : "06257"),
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
        else if (gift.Code.Contains("TORCHIC", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 10;
            details.HeldItem = "Blazikenite";
            details.Ability = "Speed Boost";
            details.Nature = "Adamant";
            details.Moves = ["Scratch", "Growl", "Focus Energy", "Ember"];
        }
        else if (gift.Code.Contains("BELDUM", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 5;
            details.HeldItem = "Metagrossite";
            details.Ability = "Clear Body";
            details.Nature = "Jolly";
            details.Moves = ["Hold Back", "Iron Head", "Zen Headbutt", "Iron Defense"];
        }
        else if (gift.Code.Contains("DIANCIE", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 50;
            details.HeldItem = "Normal Gem";
            details.Ability = "Clear Body";
            details.Nature = "Naive";
            details.Moves = ["Diamond Storm", "Moonblast", "Dazzling Gleam", "Protect"];
        }
        else if (gift.Code.Contains("HOOPA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 50;
            details.HeldItem = "Focus Sash";
            details.Ability = "Magician";
            details.Nature = "Modest";
            details.Moves = ["Hyperspace Hole", "Psychic", "Astonish", "Nasty Plot"];
        }
        else if (gift.Code.Contains("VOLCANION", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 70;
            details.HeldItem = "Assault Vest";
            details.Ability = "Water Absorb";
            details.Nature = "Modest";
            details.Moves = ["Steam Eruption", "Flamethrower", "Hydro Pump", "Explosion"];
        }
        else if (gift.Code.Contains("RAYQUAZA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 70;
            details.HeldItem = "Life Orb";
            details.Ability = "Air Lock";
            details.Nature = "Jolly";
            details.Moves = ["Dragon Ascent", "Dragon Claw", "Extreme Speed", "Dragon Dance"];
        }
        else if (gift.Code.Contains("GF-MEW", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Ability = "Synchronize";
            details.Moves = ["Pound"];
        }
        else if (gift.Code.Contains("GF-CELEBI", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Ability = "Natural Cure";
            details.Moves = ["Confusion", "Recover", "Heal Bell", "Safeguard"];
        }
        else if (gift.Code.Contains("GF-JIRACHI", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Ability = "Serene Grace";
            details.Moves = ["Wish", "Confusion", "Rest"];
        }
        else if (gift.Code.Contains("GF-DARKRAI", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Ability = "Bad Dreams";
            details.Moves = ["Dark Void", "Ominous Wind", "Nightmare", "Feint Attack"];
        }
        else if (gift.Code.Contains("GF-ARCEUS", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Ability = "Multitype";
            details.Moves = ["Judgment", "Recover", "Hyper Beam", "Perish Song"];
        }
        else if (gift.Code.Contains("GF-GENESECT", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 100;
            details.Ability = "Download";
            details.Moves = ["Techno Blast", "Magnet Bomb", "Solar Beam", "Signal Beam"];
        }
        else if (gift.Code.Contains("MAGEARNA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 50;
            details.HeldItem = "Silver Bottle Cap";
            details.Ability = "Soul-Heart";
            details.Nature = "Quiet";
            details.Moves = ["Fleur Cannon", "Flash Cannon", "Lucky Chant", "Helping Hand"];
        }
        else if (gift.Code.Contains("ASH-GRENINJA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 36;
            details.Ability = "Battle Bond";
            details.Nature = "Timid";
            details.Pokeball = "Poké Ball";
            details.Moves = ["Water Shuriken", "Aerial Ace", "Double Team", "Night Slash"];
        }
        else if (gift.Code.Contains("MUNCHLAX", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 5;
            details.HeldItem = "Snorlium Z";
            details.Ability = "Thick Fat";
            details.Nature = "Careful";
            details.Moves = ["Hold Back", "Happy Hour", "Tackle", "Metronome"];
        }
        else if (gift.Code.Contains("ROCKRUFF", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 10;
            details.HeldItem = "Focus Band";
            details.Ability = "Own Tempo";
            details.Nature = "Jolly";
            details.Moves = ["Tackle", "Bite", "Fire Fang", "Happy Hour"];
        }
        else if (gift.Code.Contains("MARSHADOW", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 50;
            details.HeldItem = "Marshadium Z";
            details.Ability = "Technician";
            details.Nature = "Jolly";
            details.Moves = ["Spectral Thief", "Close Combat", "Force Palm", "Shadow Sneak"];
        }
        else if (gift.Code.Contains("ZERAORA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 50;
            details.HeldItem = "Air Balloon";
            details.Ability = "Volt Absorb";
            details.Nature = "Hasty";
            details.Moves = ["Plasma Fists", "Thunder Punch", "Close Combat", "Thunder"];
        }
        else if (gift.Code.Contains("TAPUKOKO", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 60;
            details.HeldItem = "Electric Seed";
            details.Ability = "Electric Surge";
            details.Nature = "Timid";
            details.Moves = ["Nature's Madness", "Discharge", "Agility", "Electro Ball"];
        }
        else if (gift.Code.Contains("POIPOLE", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 40;
            details.Ability = "Beast Boost";
            details.Nature = "Modest";
            details.Moves = ["Venom Drench", "Nasty Plot", "Poison Jab", "Dragon Pulse"];
        }
        else if (gift.Code.Contains("PIKACHU20", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 1;
            details.HeldItem = "Pikashunium Z";
            details.Ability = "Static";
            details.Nature = "Hardy";
            details.Pokeball = "Poké Ball";
            details.Moves = ["Thunderbolt", "Quick Attack", "Thunder", "Agility"];
        }
        else if (gift.Code.Contains("SOLGALEO", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 60;
            details.HeldItem = "Master Ball";
            details.Ability = "Full Metal Body";
            details.Nature = "Adamant";
            details.Moves = ["Sunsteel Strike", "Zen Headbutt", "Noble Roar", "Morning Sun"];
        }
        else if (gift.Code.Contains("LUNALA", StringComparison.OrdinalIgnoreCase))
        {
            entry.Level = 60;
            details.HeldItem = "Master Ball";
            details.Ability = "Shadow Shield";
            details.Nature = "Modest";
            details.Moves = ["Moongeist Beam", "Psychic", "Moonlight", "Magic Coat"];
        }
        else
        {
            entry.Level = 50;
        }

        entry.Details = details;
        return entry;
    }
}

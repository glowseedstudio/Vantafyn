package dev.vantafyn.core.jellyfin

/**
 * Canonical National Pokédex catalog covering all 1025 Pokémon species (Gens 1 through 9).
 */
object PokemonSpeciesCatalog {
    val ALL_SPECIES: Array<String> = arrayOf(
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
        "Slowpoke", "Slowbro", "Magnemite", "Magneton", "Farfetch’d", "Doduo",
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
        "Zapdos", "Moltres", "Dratini", "Dragonair", "Dragonite", "Mewtwo",
        "Mew", "Chikorita", "Bayleef", "Meganium", "Cyndaquil", "Quilava",
        "Typhlosion", "Totodile", "Croconaw", "Feraligatr", "Sentret", "Furret",
        "Hoothoot", "Noctowl", "Ledyba", "Ledian", "Spinarak", "Ariados",
        "Crobat", "Chinchou", "Lanturn", "Pichu", "Cleffa", "Igglybuff",
        "Togepi", "Togetic", "Natu", "Xatu", "Mareep", "Flaaffy",
        "Ampharos", "Bellossom", "Marill", "Azumarill", "Sudowoodo", "Politoed",
        "Hoppip", "Skiploom", "Jumpluff", "Aipom", "Sunkern", "Sunflora",
        "Yanma", "Wooper", "Quagsire", "Espeon", "Umbreon", "Murkrow",
        "Slowking", "Misdreavus", "Unown", "Wobbuffet", "Girafarig", "Pineco",
        "Forretress", "Dunsparce", "Gligar", "Steelix", "Snubbull", "Granbull",
        "Qwilfish", "Scizor", "Shuckle", "Heracross", "Sneasel", "Teddiursa",
        "Ursaring", "Slugma", "Magcargo", "Swinub", "Piloswine", "Corsola",
        "Remoraid", "Octillery", "Delibird", "Mantine", "Skarmory", "Houndour",
        "Houndoom", "Kingdra", "Phanpy", "Donphan", "Porygon2", "Stantler",
        "Smeargle", "Tyrogue", "Hitmontop", "Smoochum", "Elekid", "Magby",
        "Miltank", "Blissey", "Raikou", "Entei", "Suicune", "Larvitar",
        "Pupitar", "Tyranitar", "Lugia", "Ho-Oh", "Celebi", "Treecko",
        "Grovyle", "Sceptile", "Torchic", "Combusken", "Blaziken", "Mudkip",
        "Marshtomp", "Swampert", "Poochyena", "Mightyena", "Zigzagoon", "Linoone",
        "Wurmple", "Silcoon", "Beautifly", "Cascoon", "Dustox", "Lotad",
        "Lombre", "Ludicolo", "Seedot", "Nuzleaf", "Shiftry", "Taillow",
        "Swellow", "Wingull", "Pelipper", "Ralts", "Kirlia", "Gardevoir",
        "Surskit", "Masquerain", "Shroomish", "Breloom", "Slakoth", "Vigoroth",
        "Slaking", "Nincada", "Ninjask", "Shedinja", "Whismur", "Loudred",
        "Exploud", "Makuhita", "Hariyama", "Azurill", "Nosepass", "Skitty",
        "Delcatty", "Sableye", "Mawile", "Aron", "Lairon", "Aggron",
        "Meditite", "Medicham", "Electrike", "Manectric", "Plusle", "Minun",
        "Volbeat", "Illumise", "Roselia", "Gulpin", "Swalot", "Carvanha",
        "Sharpedo", "Wailmer", "Wailord", "Numel", "Camerupt", "Torkoal",
        "Spoink", "Grumpig", "Spinda", "Trapinch", "Vibrava", "Flygon",
        "Cacnea", "Cacturne", "Swablu", "Altaria", "Zangoose", "Seviper",
        "Lunatone", "Solrock", "Barboach", "Whiscash", "Corphish", "Crawdaunt",
        "Baltoy", "Claydol", "Lileep", "Cradily", "Anorith", "Armaldo",
        "Feebas", "Milotic", "Castform", "Kecleon", "Shuppet", "Banette",
        "Duskull", "Dusclops", "Tropius", "Chimecho", "Absol", "Wynaut",
        "Snorunt", "Glalie", "Spheal", "Sealeo", "Walrein", "Clamperl",
        "Huntail", "Gorebyss", "Relicanth", "Luvdisc", "Bagon", "Shelgon",
        "Salamence", "Beldum", "Metang", "Metagross", "Regirock", "Regice",
        "Registeel", "Latias", "Latios", "Kyogre", "Groudon", "Rayquaza",
        "Jirachi", "Deoxys", "Turtwig", "Grotle", "Torterra", "Chimchar",
        "Monferno", "Infernape", "Piplup", "Prinplup", "Empoleon", "Starly",
        "Staravia", "Staraptor", "Bidoof", "Bibarel", "Kricketot", "Kricketune",
        "Shinx", "Luxio", "Luxray", "Budew", "Roserade", "Cranidos",
        "Rampardos", "Shieldon", "Bastiodon", "Burmy", "Wormadam", "Mothim",
        "Combee", "Vespiquen", "Pachirisu", "Buizel", "Floatzel", "Cherubi",
        "Cherrim", "Shellos", "Gastrodon", "Ambipom", "Drifloon", "Drifblim",
        "Buneary", "Lopunny", "Mismagius", "Honchkrow", "Glameow", "Purugly",
        "Chingling", "Stunky", "Skuntank", "Bronzor", "Bronzong", "Bonsly",
        "Mime Jr.", "Happiny", "Chatot", "Spiritomb", "Gible", "Gabite",
        "Garchomp", "Munchlax", "Riolu", "Lucario", "Hippopotas", "Hippowdon",
        "Skorupi", "Drapion", "Croagunk", "Toxicroak", "Carnivine", "Finneon",
        "Lumineon", "Mantyke", "Snover", "Abomasnow", "Weavile", "Magnezone",
        "Lickilicky", "Rhyperior", "Tangrowth", "Electivire", "Magmortar", "Togekiss",
        "Yanmega", "Leafeon", "Glaceon", "Gliscor", "Mamoswine", "Porygon-Z",
        "Gallade", "Probopass", "Dusknoir", "Froslass", "Rotom", "Uxie",
        "Mesprit", "Azelf", "Dialga", "Palkia", "Heatran", "Regigigas",
        "Giratina", "Cresselia", "Phione", "Manaphy", "Darkrai", "Shaymin",
        "Arceus", "Victini", "Snivy", "Servine", "Serperior", "Tepig",
        "Pignite", "Emboar", "Oshawott", "Dewott", "Samurott", "Patrat",
        "Watchog", "Lillipup", "Herdier", "Stoutland", "Purrloin", "Liepard",
        "Pansage", "Simisage", "Pansear", "Simisear", "Panpour", "Simipour",
        "Munna", "Musharna", "Pidove", "Tranquill", "Unfezant", "Blitzle",
        "Zebstrika", "Roggenrola", "Boldore", "Gigalith", "Woobat", "Swoobat",
        "Drilbur", "Excadrill", "Audino", "Timburr", "Gurdurr", "Conkeldurr",
        "Tympole", "Palpitoad", "Seismitoad", "Throh", "Sawk", "Sewaddle",
        "Swadloon", "Leavanny", "Venipede", "Whirlipede", "Scolipede", "Cottonee",
        "Whimsicott", "Petilil", "Lilligant", "Basculin", "Sandile", "Krokorok",
        "Krookodile", "Darumaka", "Darmanitan", "Maractus", "Dwebble", "Crustle",
        "Scraggy", "Scrafty", "Sigilyph", "Yamask", "Cofagrigus", "Tirtouga",
        "Carracosta", "Archen", "Archeops", "Trubbish", "Garbodor", "Zorua",
        "Zoroark", "Minccino", "Cinccino", "Gothita", "Gothorita", "Gothitelle",
        "Solosis", "Duosion", "Reuniclus", "Ducklett", "Swanna", "Vanillite",
        "Vanillish", "Vanilluxe", "Deerling", "Sawsbuck", "Emolga", "Karrablast",
        "Escavalier", "Foongus", "Amoonguss", "Frillish", "Jellicent", "Alomomola",
        "Joltik", "Galvantula", "Ferroseed", "Ferrothorn", "Klink", "Klang",
        "Klinklang", "Tynamo", "Eelektrik", "Eelektross", "Elgyem", "Beheeyem",
        "Litwick", "Lampent", "Chandelure", "Axew", "Fraxure", "Haxorus",
        "Cubchoo", "Beartic", "Cryogonal", "Shelmet", "Accelgor", "Stunfisk",
        "Mienfoo", "Mienshao", "Druddigon", "Golett", "Golurk", "Pawniard",
        "Bisharp", "Bouffalant", "Rufflet", "Braviary", "Vullaby", "Mandibuzz",
        "Heatmor", "Durant", "Deino", "Zweilous", "Hydreigon", "Larvesta",
        "Volcarona", "Cobalion", "Terrakion", "Virizion", "Tornadus", "Thundurus",
        "Reshiram", "Zekrom", "Landorus", "Kyurem", "Keldeo", "Meloetta",
        "Genesect", "Chespin", "Quilladin", "Chesnaught", "Fennekin", "Braixen",
        "Delphox", "Froakie", "Frogadier", "Greninja", "Bunnelby", "Diggersby",
        "Fletchling", "Fletchinder", "Talonflame", "Scatterbug", "Spewpa", "Vivillon",
        "Litleo", "Pyroar", "Flabébé", "Floette", "Florges", "Skiddo",
        "Gogoat", "Pancham", "Pangoro", "Furfrou", "Espurr", "Meowstic",
        "Honedge", "Doublade", "Aegislash", "Spritzee", "Aromatisse", "Swirlix",
        "Slurpuff", "Inkay", "Malamar", "Binacle", "Barbaracle", "Skrelp",
        "Dragalge", "Clauncher", "Clawitzer", "Helioptile", "Heliolisk", "Tyrunt",
        "Tyrantrum", "Amaura", "Aurorus", "Sylveon", "Hawlucha", "Dedenne",
        "Carbink", "Goomy", "Sliggoo", "Goodra", "Klefki", "Phantump",
        "Trevenant", "Pumpkaboo", "Gourgeist", "Bergmite", "Avalugg", "Noibat",
        "Noivern", "Xerneas", "Yveltal", "Zygarde", "Diancie", "Hoopa",
        "Volcanion", "Rowlet", "Dartrix", "Decidueye", "Litten", "Torracat",
        "Incineroar", "Popplio", "Brionne", "Primarina", "Pikipek", "Trumbeak",
        "Toucannon", "Yungoos", "Gumshoos", "Grubbin", "Charjabug", "Vikavolt",
        "Crabrawler", "Crabominable", "Oricorio", "Cutiefly", "Ribombee", "Rockruff",
        "Lycanroc", "Wishiwashi", "Mareanie", "Toxapex", "Mudbray", "Mudsdale",
        "Dewpider", "Araquanid", "Fomantis", "Lurantis", "Morelull", "Shiinotic",
        "Salandit", "Salazzle", "Stufful", "Bewear", "Bounsweet", "Steenee",
        "Tsareena", "Comfey", "Oranguru", "Passimian", "Wimpod", "Golisopod",
        "Sandygast", "Palossand", "Pyukumuku", "Type: Null", "Silvally", "Minior",
        "Komala", "Turtonator", "Togedemaru", "Mimikyu", "Bruxish", "Drampa",
        "Dhelmise", "Jangmo-o", "Hakamo-o", "Kommo-o", "Tapu Koko", "Tapu Lele",
        "Tapu Bulu", "Tapu Fini", "Cosmog", "Cosmoem", "Solgaleo", "Lunala",
        "Nihilego", "Buzzwole", "Pheromosa", "Xurkitree", "Celesteela", "Kartana",
        "Guzzlord", "Necrozma", "Magearna", "Marshadow", "Poipole", "Naganadel",
        "Stakataka", "Blacephalon", "Zeraora", "Meltan", "Melmetal", "Grookey",
        "Thwackey", "Rillaboom", "Scorbunny", "Raboot", "Cinderace", "Sobble",
        "Drizzile", "Inteleon", "Skwovet", "Greedent", "Rookidee", "Corvisquire",
        "Corviknight", "Blipbug", "Dottler", "Orbeetle", "Nickit", "Thievul",
        "Gossifleur", "Eldegoss", "Wooloo", "Dubwool", "Chewtle", "Drednaw",
        "Yamper", "Boltund", "Rolycoly", "Carkol", "Coalossal", "Applin",
        "Flapple", "Appletun", "Silicobra", "Sandaconda", "Cramorant", "Arrokuda",
        "Barraskewda", "Toxel", "Toxtricity", "Sizzlipede", "Centiskorch", "Clobbopus",
        "Grapploct", "Sinistea", "Polteageist", "Hatenna", "Hattrem", "Hatterene",
        "Impidimp", "Morgrem", "Grimmsnarl", "Obstagoon", "Perrserker", "Cursola",
        "Sirfetch’d", "Mr. Rime", "Runerigus", "Milcery", "Alcremie", "Falinks",
        "Pincurchin", "Snom", "Frosmoth", "Stonjourner", "Eiscue", "Indeedee",
        "Morpeko", "Cufant", "Copperajah", "Dracozolt", "Arctozolt", "Dracovish",
        "Arctovish", "Duraludon", "Dreepy", "Drakloak", "Dragapult", "Zacian",
        "Zamazenta", "Eternatus", "Kubfu", "Urshifu", "Zarude", "Regieleki",
        "Regidrago", "Glastrier", "Spectrier", "Calyrex", "Wyrdeer", "Kleavor",
        "Ursaluna", "Basculegion", "Sneasler", "Overqwil", "Enamorus", "Sprigatito",
        "Floragato", "Meowscarada", "Fuecoco", "Crocalor", "Skeledirge", "Quaxly",
        "Quaxwell", "Quaquaval", "Lechonk", "Oinkologne", "Tarountula", "Spidops",
        "Nymble", "Lokix", "Pawmi", "Pawmo", "Pawmot", "Tandemaus",
        "Maushold", "Fidough", "Dachsbun", "Smoliv", "Dolliv", "Arboliva",
        "Squawkabilly", "Nacli", "Naclstack", "Garganacl", "Charcadet", "Armarouge",
        "Ceruledge", "Tadbulb", "Bellibolt", "Wattrel", "Kilowattrel", "Maschiff",
        "Mabosstiff", "Shroodle", "Grafaiai", "Bramblin", "Brambleghast", "Toedscool",
        "Toedscruel", "Klawf", "Capsakid", "Scovillain", "Rellor", "Rabsca",
        "Flittle", "Espathra", "Tinkatink", "Tinkatuff", "Tinkaton", "Wiglett",
        "Wugtrio", "Bombirdier", "Finizen", "Palafin", "Varoom", "Revavroom",
        "Cyclizar", "Orthworm", "Glimmet", "Glimmora", "Greavard", "Houndstone",
        "Flamigo", "Cetoddle", "Cetitan", "Veluza", "Dondozo", "Tatsugiri",
        "Annihilape", "Clodsire", "Farigiraf", "Dudunsparce", "Kingambit", "Great Tusk",
        "Scream Tail", "Brute Bonnet", "Flutter Mane", "Slither Wing", "Sandy Shocks", "Iron Treads",
        "Iron Bundle", "Iron Hands", "Iron Jugulis", "Iron Moth", "Iron Thorns", "Frigibax",
        "Arctibax", "Baxcalibur", "Gimmighoul", "Gholdengo", "Wo-Chien", "Chien-Pao",
        "Ting-Lu", "Chi-Yu", "Roaring Moon", "Iron Valiant", "Koraidon", "Miraidon",
        "Walking Wake", "Iron Leaves", "Dipplin", "Poltchageist", "Sinistcha", "Okidogi",
        "Munkidori", "Fezandipiti", "Ogerpon", "Archaludon", "Hydrapple", "Gouging Fire",
        "Raging Bolt", "Iron Boulder", "Iron Crown", "Terapagos", "Pecharunt"
    )

    fun resolveSpeciesName(speciesId: Int): String {
        return if (speciesId in 1..ALL_SPECIES.size) {
            ALL_SPECIES[speciesId - 1]
        } else {
            "#$speciesId"
        }
    }

    fun getGeneration(speciesId: Int): Int {
        return when (speciesId) {
            in 1..151 -> 1
            in 152..251 -> 2
            in 252..386 -> 3
            in 387..493 -> 4
            in 494..649 -> 5
            in 650..721 -> 6
            in 722..809 -> 7
            in 810..905 -> 8
            in 906..1025 -> 9
            else -> 1
        }
    }

    fun getAvailableEvolutions(speciesId: Int, level: Int = 50): List<PokemonEvolutionOptionDto> {
        val list = mutableListOf<PokemonEvolutionOptionDto>()
        when (speciesId) {
            64 -> list.add(PokemonEvolutionOptionDto(65, "Alakazam", "Trade", null, null, "Evolves by trade into Alakazam."))
            67 -> list.add(PokemonEvolutionOptionDto(68, "Machamp", "Trade", null, null, "Evolves by trade into Machamp."))
            75 -> list.add(PokemonEvolutionOptionDto(76, "Golem", "Trade", null, null, "Evolves by trade into Golem."))
            93 -> list.add(PokemonEvolutionOptionDto(94, "Gengar", "Trade", null, null, "Evolves by trade into Gengar."))
            61 -> list.add(PokemonEvolutionOptionDto(186, "Politoed", "Trade + Item", "King's Rock", null, "Evolves by trading while holding King's Rock."))
            79 -> list.add(PokemonEvolutionOptionDto(199, "Slowking", "Trade + Item", "King's Rock", null, "Evolves by trading while holding King's Rock."))
            95 -> list.add(PokemonEvolutionOptionDto(208, "Steelix", "Trade + Item", "Metal Coat", null, "Evolves by trading while holding Metal Coat."))
            117 -> list.add(PokemonEvolutionOptionDto(230, "Kingdra", "Trade + Item", "Dragon Scale", null, "Evolves by trading while holding Dragon Scale."))
            123 -> list.add(PokemonEvolutionOptionDto(212, "Scizor", "Trade + Item", "Metal Coat", null, "Evolves by trading while holding Metal Coat."))
            137 -> list.add(PokemonEvolutionOptionDto(233, "Porygon2", "Trade + Item", "Up-Grade", null, "Evolves by trading while holding Up-Grade."))
            356 -> list.add(PokemonEvolutionOptionDto(477, "Dusknoir", "Trade + Item", "Reaper Cloth", null, "Evolves by trading while holding Reaper Cloth."))
            366 -> {
                list.add(PokemonEvolutionOptionDto(367, "Huntail", "Trade + Item", "Deep Sea Tooth", null, "Evolves by trading while holding Deep Sea Tooth."))
                list.add(PokemonEvolutionOptionDto(368, "Gorebyss", "Trade + Item", "Deep Sea Scale", null, "Evolves by trading while holding Deep Sea Scale."))
            }
            112 -> list.add(PokemonEvolutionOptionDto(464, "Rhyperior", "Trade + Item", "Protector", null, "Evolves by trading while holding Protector."))
            125 -> list.add(PokemonEvolutionOptionDto(466, "Electivire", "Trade + Item", "Electirizer", null, "Evolves by trading while holding Electirizer."))
            126 -> list.add(PokemonEvolutionOptionDto(467, "Magmortar", "Trade + Item", "Magmarizer", null, "Evolves by trading while holding Magmarizer."))
            233 -> list.add(PokemonEvolutionOptionDto(474, "Porygon-Z", "Trade + Item", "Dubious Disc", null, "Evolves by trading while holding Dubious Disc."))
            525 -> list.add(PokemonEvolutionOptionDto(526, "Gigalith", "Trade", null, null, "Evolves by trade into Gigalith."))
            533 -> list.add(PokemonEvolutionOptionDto(534, "Conkeldurr", "Trade", null, null, "Evolves by trade into Conkeldurr."))
            588 -> list.add(PokemonEvolutionOptionDto(589, "Escavalier", "Trade", "Shelmet", null, "Evolves by trading for Shelmet."))
            616 -> list.add(PokemonEvolutionOptionDto(617, "Accelgor", "Trade", "Karrablast", null, "Evolves by trading for Karrablast."))
            682 -> list.add(PokemonEvolutionOptionDto(683, "Aromatisse", "Trade + Item", "Sachet", null, "Evolves by trading while holding Sachet."))
            684 -> list.add(PokemonEvolutionOptionDto(685, "Slurpuff", "Trade + Item", "Whipped Dream", null, "Evolves by trading while holding Whipped Dream."))
            708 -> list.add(PokemonEvolutionOptionDto(709, "Trevenant", "Trade", null, null, "Evolves by trade into Trevenant."))
            710 -> list.add(PokemonEvolutionOptionDto(711, "Gourgeist", "Trade", null, null, "Evolves by trade into Gourgeist."))
            25 -> list.add(PokemonEvolutionOptionDto(26, "Raichu", "Thunder Stone", "Thunder Stone", null, "Evolves using a Thunder Stone."))
            133 -> {
                list.add(PokemonEvolutionOptionDto(134, "Vaporeon", "Water Stone", "Water Stone", null, "Evolves using a Water Stone."))
                list.add(PokemonEvolutionOptionDto(135, "Jolteon", "Thunder Stone", "Thunder Stone", null, "Evolves using a Thunder Stone."))
                list.add(PokemonEvolutionOptionDto(136, "Flareon", "Fire Stone", "Fire Stone", null, "Evolves using a Fire Stone."))
                list.add(PokemonEvolutionOptionDto(196, "Espeon", "Daylight Friendship", null, null, "Evolves through friendship during the day."))
                list.add(PokemonEvolutionOptionDto(197, "Umbreon", "Nighttime Friendship", null, null, "Evolves through friendship at night."))
                list.add(PokemonEvolutionOptionDto(470, "Leafeon", "Leaf Stone", "Leaf Stone", null, "Evolves using a Leaf Stone."))
                list.add(PokemonEvolutionOptionDto(471, "Glaceon", "Ice Stone", "Ice Stone", null, "Evolves using an Ice Stone."))
                list.add(PokemonEvolutionOptionDto(700, "Sylveon", "Fairy Bond", null, null, "Evolves with deep affection and a Fairy-type move."))
            }
        }
        return list
    }
}

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

    fun isLegendaryOrMythical(speciesId: Int): Boolean {
        return when (speciesId) {
            in 144..146, 150, 151, // Gen 1
            in 243..245, 249, 250, 251, // Gen 2
            in 377..386, // Gen 3
            in 480..493, // Gen 4
            in 638..649, // Gen 5
            in 716..721, // Gen 6
            772, 773, in 785..809, // Gen 7
            in 888..898, 905, // Gen 8
            in 1001..1025 -> true // Gen 9
            else -> false
        }
    }

    fun getCanonicalAbility(speciesId: Int): String {
        return when (speciesId) {
            1, 2, 3, 152, 153, 154, 252, 253, 254, 387, 388, 389, 495, 496, 497, 650, 651, 652, 722, 723, 724, 810, 811, 812, 906, 907, 908 -> "Overgrow"
            4, 5, 6, 155, 156, 157, 255, 256, 257, 390, 391, 392, 498, 499, 500, 653, 654, 655, 725, 726, 727, 813, 814, 815, 909, 910, 911 -> "Blaze"
            7, 8, 9, 158, 159, 160, 258, 259, 260, 393, 394, 395, 501, 502, 503, 656, 657, 658, 728, 729, 730, 816, 817, 818, 912, 913, 914 -> "Torrent"
            25, 26, 172, 125, 239, 466 -> "Static"
            92, 93, 94, 109, 110, 329, 330, 380, 381 -> "Levitate"
            130, 128, 373, 559, 560 -> "Intimidate"
            63, 64, 65, 151, 177, 178, 196 -> "Synchronize"
            133 -> "Adaptability"
            134 -> "Water Absorb"
            135 -> "Volt Absorb"
            136 -> "Flash Fire"
            197 -> "Synchronize"
            470 -> "Leaf Guard"
            471 -> "Snow Cloak"
            700 -> "Cute Charm"
            143 -> "Thick Fat"
            131 -> "Water Absorb"
            149 -> "Inner Focus"
            150 -> "Pressure"
            249, 250, 384, 483, 484, 487 -> "Pressure"
            382 -> "Drizzle"
            383 -> "Drought"
            448 -> "Steadfast"
            445 -> "Rough Skin"
            248 -> "Sand Stream"
            292 -> "Wonder Guard"
            778 -> "Disguise"
            else -> when (getGeneration(speciesId)) {
                1 -> if (speciesId % 3 == 0) "Chlorophyll" else if (speciesId % 2 == 0) "Keen Eye" else "Run Away"
                2 -> if (speciesId % 3 == 0) "Swift Swim" else if (speciesId % 2 == 0) "Inner Focus" else "Natural Cure"
                3 -> if (speciesId % 3 == 0) "Clear Body" else if (speciesId % 2 == 0) "Serene Grace" else "Speed Boost"
                else -> if (speciesId % 3 == 0) "Competitive" else if (speciesId % 2 == 0) "Infiltrator" else "Prankster"
            }
        }
    }

    fun getCanonicalMoves(speciesId: Int, level: Int): List<String> {
        val specific = when (speciesId) {
            1, 2, 3 -> listOf("Solar Beam", "Sludge Bomb", "Leech Seed", "Energy Ball")
            4, 5, 6 -> listOf("Flamethrower", "Air Slash", "Dragon Claw", "Fire Blast")
            7, 8, 9 -> listOf("Hydro Pump", "Surf", "Ice Beam", "Rapid Spin")
            25, 26, 172 -> listOf("Thunderbolt", "Quick Attack", "Iron Tail", "Electro Ball")
            92, 93, 94 -> listOf("Shadow Ball", "Sludge Bomb", "Hypnosis", "Dream Eater")
            63, 64, 65 -> listOf("Psychic", "Shadow Ball", "Focus Blast", "Recover")
            66, 67, 68 -> listOf("Cross Chop", "Dynamic Punch", "Stone Edge", "Bullet Punch")
            129 -> listOf("Splash", "Tackle", "Flail", "Bounce")
            130 -> listOf("Waterfall", "Dragon Dance", "Crunch", "Ice Fang")
            131 -> listOf("Ice Beam", "Surf", "Thunderbolt", "Body Slam")
            133 -> listOf("Quick Attack", "Bite", "Double-Edge", "Baby-Doll Eyes")
            134 -> listOf("Hydro Pump", "Ice Beam", "Acid Armor", "Wish")
            135 -> listOf("Thunderbolt", "Volt Switch", "Shadow Ball", "Thunder Wave")
            136 -> listOf("Flare Blitz", "Fire Fang", "Quick Attack", "Superpower")
            143 -> listOf("Body Slam", "Rest", "Sleep Talk", "Earthquake")
            147, 148, 149 -> listOf("Outrage", "Dragon Dance", "Extreme Speed", "Hyper Beam")
            150 -> listOf("Psystrike", "Aura Sphere", "Psychic", "Recover")
            151 -> listOf("Psychic", "Transform", "Aura Sphere", "Metronome")
            152, 153, 154 -> listOf("Giga Drain", "Reflect", "Light Screen", "Body Slam")
            155, 156, 157 -> listOf("Eruption", "Flamethrower", "Extrasensory", "Focus Blast")
            158, 159, 160 -> listOf("Waterfall", "Crunch", "Ice Fang", "Dragon Dance")
            196 -> listOf("Psychic", "Shadow Ball", "Morning Sun", "Calm Mind")
            197 -> listOf("Foul Play", "Wish", "Protect", "Toxic")
            246, 247, 248 -> listOf("Stone Edge", "Crunch", "Earthquake", "Dragon Dance")
            249 -> listOf("Aeroblast", "Psychic", "Hydro Pump", "Roost")
            250 -> listOf("Sacred Fire", "Brave Bird", "Earthquake", "Roost")
            251 -> listOf("Energy Ball", "Psychic", "Recover", "Heal Bell")
            252, 253, 254 -> listOf("Leaf Blade", "Dragon Pulse", "Focus Blast", "Giga Drain")
            255, 256, 257 -> listOf("Flare Blitz", "High Jump Kick", "Brave Bird", "Swords Dance")
            258, 259, 260 -> listOf("Earthquake", "Waterfall", "Ice Punch", "Stealth Rock")
            280, 281, 282 -> listOf("Moonblast", "Psychic", "Shadow Ball", "Calm Mind")
            371, 372, 373 -> listOf("Dragon Claw", "Fly", "Fire Blast", "Earthquake")
            374, 375, 376 -> listOf("Meteor Mash", "Zen Headbutt", "Bullet Punch", "Earthquake")
            380 -> listOf("Dragon Pulse", "Psychic", "Recover", "Calm Mind")
            381 -> listOf("Dragon Pulse", "Psychic", "Draco Meteor", "Surf")
            382 -> listOf("Water Spout", "Origin Pulse", "Thunder", "Ice Beam")
            383 -> listOf("Precipice Blades", "Fire Blast", "Solar Beam", "Earthquake")
            384 -> listOf("Dragon Ascent", "Extreme Speed", "Outrage", "Earthquake")
            443, 444, 445 -> listOf("Earthquake", "Dragon Claw", "Stone Edge", "Swords Dance")
            447, 448 -> listOf("Aura Sphere", "Close Combat", "Extreme Speed", "Flash Cannon")
            656, 657, 658 -> listOf("Water Shuriken", "Dark Pulse", "Ice Beam", "Hydro Pump")
            else -> null
        }
        if (specific != null) return specific

        // Thematic fallback moves according to generation and species ID
        return when (speciesId % 6) {
            0 -> listOf("Tackle", "Quick Attack", "Body Slam", "Hyper Beam")
            1 -> listOf("Ember", "Flamethrower", "Fire Blast", "Sunny Day")
            2 -> listOf("Water Gun", "Bubble Beam", "Surf", "Hydro Pump")
            3 -> listOf("Vine Whip", "Mega Drain", "Razor Leaf", "Solar Beam")
            4 -> listOf("Thunder Shock", "Spark", "Thunderbolt", "Thunder")
            else -> listOf("Confusion", "Psybeam", "Psychic", "Shadow Ball")
        }
    }

    fun generateCanonicalDetails(summary: PokemonSummaryDto): PokemonDetailsDto {
        val speciesId = summary.speciesId.coerceIn(1, 1025)
        val level = summary.level.coerceIn(1, 100)
        val isLegendary = isLegendaryOrMythical(speciesId)

        // Deterministic pseudo-random seed from species, level, shiny, and id
        val seed = (speciesId * 10007L + level * 37L + (if (summary.isShiny) 7777L else 0L) + summary.id.hashCode().toLong()).let { if (it < 0) -it else it }

        // Realistic high IVs (20..31), legendaries get 3 guaranteed 31s
        val baseIvList = (0..5).map { idx ->
            val randVal = ((seed shr (idx * 5)) % 32).toInt()
            20 + (randVal % 12)
        }.toMutableList()

        if (isLegendary) {
            baseIvList[0] = 31
            baseIvList[1] = 31
            baseIvList[5] = 31
        }

        val ivStats = PokemonStatsDto(
            hp = baseIvList[0].coerceIn(0, 31),
            attack = baseIvList[1].coerceIn(0, 31),
            defense = baseIvList[2].coerceIn(0, 31),
            specialAttack = baseIvList[3].coerceIn(0, 31),
            specialDefense = baseIvList[4].coerceIn(0, 31),
            speed = baseIvList[5].coerceIn(0, 31),
        )

        val evStats = if (level > 20) {
            val evSpread = (level - 20) * 4
            PokemonStatsDto(
                hp = (evSpread / 4).coerceIn(0, 252),
                attack = (evSpread / 3).coerceIn(0, 252),
                defense = (evSpread / 5).coerceIn(0, 252),
                specialAttack = (evSpread / 3).coerceIn(0, 252),
                specialDefense = (evSpread / 5).coerceIn(0, 252),
                speed = (evSpread / 4).coerceIn(0, 252),
            )
        } else {
            PokemonStatsDto()
        }

        val natureList = listOf(
            "Adamant", "Modest", "Jolly", "Timid", "Bold", "Calm",
            "Impish", "Careful", "Hardy", "Naive", "Brave", "Quiet"
        )
        val nature = natureList[(seed % natureList.size).toInt()]

        val ability = getCanonicalAbility(speciesId)
        val moves = getCanonicalMoves(speciesId, level)
        val ball = if (isLegendary) "Master Ball" else if (summary.isShiny) "Luxury Ball" else "Poké Ball"

        return PokemonDetailsDto(
            summary = summary,
            nature = nature,
            ability = ability,
            heldItem = if (speciesId == 25) "Light Ball" else if (summary.isShiny) "Star Piece" else "None",
            moves = moves,
            iv = ivStats,
            ev = evStats,
            currentHp = null,
            maxHp = null,
            friendship = if (summary.isShiny) 200 else 120,
            pokeball = ball,
            legalityStatus = "valid",
            availableEvolutions = getAvailableEvolutions(speciesId, level),
        )
    }
}

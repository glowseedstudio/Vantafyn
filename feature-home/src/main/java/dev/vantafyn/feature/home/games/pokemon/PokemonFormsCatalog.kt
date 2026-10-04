package dev.vantafyn.feature.home.games.pokemon

/**
 * Model representing an authentic form, trim, or regional variation of a Pokémon.
 */
data class PokemonFormModel(
    val name: String,
    val spriteKey: String, // e.g. "676-heart" or "10001"
    val description: String? = null,
    val isMega: Boolean = false,
)

/**
 * Catalog of canonical Pokémon alternate forms, styling trims, appliance forms, and regional variations.
 * Provides Pokémon HOME 3D asset resolution across all supported forms.
 */
object PokemonFormsCatalog {

    fun hasAlternateForms(speciesId: Int): Boolean {
        return FORMS_BY_SPECIES.containsKey(speciesId)
    }

    fun getForms(speciesId: Int): List<PokemonFormModel> {
        return FORMS_BY_SPECIES[speciesId] ?: emptyList()
    }

    fun getMegaForms(speciesId: Int): List<PokemonFormModel> {
        return getForms(speciesId).filter { it.isMega }
    }

    fun isSpinda(speciesId: Int): Boolean = speciesId == 327

    const val SPINDA_LORE: String =
        "4,294,967,296 Spot Patterns: Spinda's facial spots are procedurally placed by a 32-bit personality value, rendering virtually every specimen uniquely distinct."

    private fun base(speciesId: Int) = PokemonFormModel("Base", speciesId.toString(), "Standard Pokédex form")

    private fun mega(name: String, spriteKey: String, description: String) =
        PokemonFormModel(name, spriteKey, description, isMega = true)

    private fun singleMega(speciesId: Int, spriteKey: String, stone: String) = listOf(
        base(speciesId),
        mega("Mega", spriteKey, "Mega Evolution awakened by $stone"),
    )

    private val FORMS_BY_SPECIES: Map<Int, List<PokemonFormModel>> = mapOf(
        // Mega Evolution
        3 to singleMega(3, "10033", "Venusaurite"),
        6 to listOf(
            base(6),
            mega("Mega X", "10034", "Mega Evolution awakened by Charizardite X"),
            mega("Mega Y", "10035", "Mega Evolution awakened by Charizardite Y"),
        ),
        9 to singleMega(9, "10036", "Blastoisinite"),
        15 to singleMega(15, "10090", "Beedrillite"),
        18 to singleMega(18, "10073", "Pidgeotite"),
        65 to singleMega(65, "10037", "Alakazite"),
        80 to listOf(
            PokemonFormModel("Kanto", "80", "Shellder-triggered evolved form (Water/Psychic)"),
            PokemonFormModel("Galarian", "10165", "Shellder clamped on its arm, weaponizing psychic venom (Poison/Psychic)"),
            mega("Mega", "10071", "Mega Evolution awakened by Slowbronite"),
        ),
        94 to singleMega(94, "10038", "Gengarite"),
        115 to singleMega(115, "10039", "Kangaskhanite"),
        127 to singleMega(127, "10040", "Pinsirite"),
        130 to singleMega(130, "10041", "Gyaradosite"),
        142 to singleMega(142, "10042", "Aerodactylite"),
        150 to listOf(
            base(150),
            mega("Mega X", "10043", "Mega Evolution awakened by Mewtwonite X"),
            mega("Mega Y", "10044", "Mega Evolution awakened by Mewtwonite Y"),
        ),
        181 to singleMega(181, "10045", "Ampharosite"),
        208 to singleMega(208, "10072", "Steelixite"),
        212 to singleMega(212, "10046", "Scizorite"),
        214 to singleMega(214, "10047", "Heracronite"),
        229 to singleMega(229, "10048", "Houndoominite"),
        248 to singleMega(248, "10049", "Tyranitarite"),
        254 to singleMega(254, "10065", "Sceptilite"),
        257 to singleMega(257, "10050", "Blazikenite"),
        260 to singleMega(260, "10064", "Swampertite"),
        282 to singleMega(282, "10051", "Gardevoirite"),
        302 to singleMega(302, "10066", "Sablenite"),
        303 to singleMega(303, "10052", "Mawilite"),
        306 to singleMega(306, "10053", "Aggronite"),
        308 to singleMega(308, "10054", "Medichamite"),
        310 to singleMega(310, "10055", "Manectite"),
        319 to singleMega(319, "10070", "Sharpedonite"),
        323 to singleMega(323, "10087", "Cameruptite"),
        334 to singleMega(334, "10067", "Altarianite"),
        354 to singleMega(354, "10056", "Banettite"),
        359 to singleMega(359, "10057", "Absolite"),
        362 to singleMega(362, "10074", "Glalitite"),
        373 to singleMega(373, "10089", "Salamencite"),
        376 to singleMega(376, "10076", "Metagrossite"),
        380 to singleMega(380, "10062", "Latiasite"),
        381 to singleMega(381, "10063", "Latiosite"),
        382 to listOf(
            base(382),
            mega("Primal", "10077", "Primal Reversion awakened by the Blue Orb"),
        ),
        383 to listOf(
            base(383),
            mega("Primal", "10078", "Primal Reversion awakened by the Red Orb"),
        ),
        384 to listOf(
            base(384),
            mega("Mega", "10079", "Mega Evolution unleashed through Dragon Ascent"),
        ),
        428 to singleMega(428, "10088", "Lopunnite"),
        445 to singleMega(445, "10058", "Garchompite"),
        448 to singleMega(448, "10059", "Lucarionite"),
        460 to singleMega(460, "10060", "Abomasite"),
        475 to singleMega(475, "10068", "Galladite"),
        531 to singleMega(531, "10069", "Audinite"),
        719 to singleMega(719, "10075", "Diancite"),

        // Furfrou
        676 to listOf(
            PokemonFormModel("Natural", "676", "Untrimmed natural coat"),
            PokemonFormModel("Heart", "676-heart", "Trimmed with romantic pink hearts"),
            PokemonFormModel("Star", "676-star", "Trimmed with celestial blue stars"),
            PokemonFormModel("Diamond", "676-diamond", "Trimmed with amber diamond crests"),
            PokemonFormModel("Debutante", "676-debutante", "Trimmed with an elegant orange bonnet"),
            PokemonFormModel("Matron", "676-matron", "Trimmed with refined brown collar tufts"),
            PokemonFormModel("Dandy", "676-dandy", "Trimmed with stylish emerald bands"),
            PokemonFormModel("La Reine", "676-la-reine", "Trimmed with regal French aristocratic crests"),
            PokemonFormModel("Kabuki", "676-kabuki", "Trimmed in traditional Japanese theatrical style"),
            PokemonFormModel("Pharaoh", "676-pharaoh", "Trimmed inspired by ancient Egyptian pharaohs"),
        ),

        // Deoxys
        386 to listOf(
            PokemonFormModel("Normal", "386", "Balanced cosmic extraterrestrial form"),
            PokemonFormModel("Attack", "10001", "Sharpened appendages configured for devastating offensive power"),
            PokemonFormModel("Defense", "10002", "Hardened shell engineered for maximum protective resilience"),
            PokemonFormModel("Speed", "10003", "Streamlined aerodynamic body optimized for hyper-velocity travel"),
        ),

        // Rotom
        479 to listOf(
            PokemonFormModel("Normal", "479", "Original plasma electric ghost form"),
            PokemonFormModel("Heat", "10008", "Inhabits a microwave oven (Electric/Fire)"),
            PokemonFormModel("Wash", "10009", "Inhabits a washing machine (Electric/Water)"),
            PokemonFormModel("Frost", "10010", "Inhabits a refrigerator (Electric/Ice)"),
            PokemonFormModel("Fan", "10011", "Inhabits an electric fan (Electric/Flying)"),
            PokemonFormModel("Mow", "10012", "Inhabits a rotary lawn mower (Electric/Grass)"),
        ),

        // Castform
        351 to listOf(
            PokemonFormModel("Normal", "351", "Baseline atmospheric form (Normal)"),
            PokemonFormModel("Sunny", "10013", "Transformed by intense sunlight (Fire)"),
            PokemonFormModel("Rainy", "10014", "Transformed by heavy rain (Water)"),
            PokemonFormModel("Snowy", "10015", "Transformed by hailing snow (Ice)"),
        ),

        // Giratina
        487 to listOf(
            PokemonFormModel("Altered", "487", "Six-legged corporeal form in our reality"),
            PokemonFormModel("Origin", "10007", "Levitating serpentine form in the Distortion World"),
        ),

        // Shaymin
        492 to listOf(
            PokemonFormModel("Land", "492", "Gentle hedgehog form blooming with Gracidea"),
            PokemonFormModel("Sky", "10006", "Agile soaring canine form transformed by Gracidea flower"),
        ),

        // Basculin
        550 to listOf(
            PokemonFormModel("Red-Striped", "550", "Violent nature with red stripe markings"),
            PokemonFormModel("Blue-Striped", "10016", "Calm nature with blue stripe markings"),
            PokemonFormModel("White-Striped", "10247", "Ancient Hisuian lineage that evolves into Basculegion"),
        ),

        // Deerling & Sawsbuck
        585 to listOf(
            PokemonFormModel("Spring", "585", "Pink floral seasonal coat"),
            PokemonFormModel("Summer", "10060", "Lush green seasonal coat"),
            PokemonFormModel("Autumn", "10061", "Warm orange-brown seasonal coat"),
            PokemonFormModel("Winter", "10062", "Thick insulating brown-grey seasonal coat"),
        ),
        586 to listOf(
            PokemonFormModel("Spring", "586", "Spring blossoming flower antlers"),
            PokemonFormModel("Summer", "10064", "Full dense green leafy antlers"),
            PokemonFormModel("Autumn", "10065", "Vibrant autumn foliage antlers"),
            PokemonFormModel("Winter", "10066", "Frost-draped winter branch antlers"),
        ),

        // Forces of Nature (Therian / Incarnate)
        641 to listOf(
            PokemonFormModel("Incarnate", "641", "Cloud-riding djinn master of winds"),
            PokemonFormModel("Therian", "10019", "Avian beast soaring through skies"),
        ),
        642 to listOf(
            PokemonFormModel("Incarnate", "642", "Cloud-riding djinn hurling lightning bolts"),
            PokemonFormModel("Therian", "10020", "Draconic reptilian beast of thunder"),
        ),
        645 to listOf(
            PokemonFormModel("Incarnate", "645", "Bountiful djinn guardian of fertile earth"),
            PokemonFormModel("Therian", "10021", "Quadruped tiger beast commanding earth"),
        ),
        905 to listOf(
            PokemonFormModel("Incarnate", "905", "Bringer of spring and revitalizing warmth"),
            PokemonFormModel("Therian", "10249", "Ancient reptilian avian spirit"),
        ),

        // Kyurem
        646 to listOf(
            PokemonFormModel("Normal", "646", "Hollow icy dragon shell"),
            PokemonFormModel("Black Kyurem", "10022", "Fused with Zekrom using the DNA Splicers"),
            PokemonFormModel("White Kyurem", "10023", "Fused with Reshiram using the DNA Splicers"),
        ),

        // Keldeo
        647 to listOf(
            PokemonFormModel("Ordinary", "647", "Young colt aspiring to be a Sword of Justice"),
            PokemonFormModel("Resolute", "10024", "Courage awakened through Secret Sword"),
        ),

        // Meloetta
        648 to listOf(
            PokemonFormModel("Aria", "648", "Vocal melodic form (Normal/Psychic)"),
            PokemonFormModel("Pirouette", "10018", "Dynamic dance rhythm form (Normal/Fighting)"),
        ),

        // Greninja
        658 to listOf(
            PokemonFormModel("Normal", "658", "Master shinobi ninja frog"),
            PokemonFormModel("Ash-Greninja", "10117", "Bond phenomenon transformation with giant Water Shuriken"),
        ),

        // Aegislash
        681 to listOf(
            PokemonFormModel("Shield", "681", "Defensive stance behind ancestral shield"),
            PokemonFormModel("Blade", "10026", "Offensive stance brandishing mystical steel blade"),
        ),

        // Zygarde
        718 to listOf(
            PokemonFormModel("50%", "718", "Serpentine monitor of the ecological balance"),
            PokemonFormModel("10%", "10118", "Speedy canine hound formed from cell clusters"),
            PokemonFormModel("Complete", "10120", "Giant humanoid titan unleashing Core Enforcer"),
        ),

        // Oricorio
        741 to listOf(
            PokemonFormModel("Baile", "741", "Flamenco dancer fueled by passion (Fire/Flying)"),
            PokemonFormModel("Pom-Pom", "10123", "Cheerful pom-pom cheerleader (Electric/Flying)"),
            PokemonFormModel("Pa'u", "10124", "Graceful relaxed hula dancer (Psychic/Flying)"),
            PokemonFormModel("Sensu", "10125", "Traditional folding-fan dancer (Ghost/Flying)"),
        ),

        // Lycanroc
        745 to listOf(
            PokemonFormModel("Midday", "745", "Loyal quadruped wolf active under solar rays"),
            PokemonFormModel("Midnight", "10126", "Bipedal ferocious wolf thirsting for combat"),
            PokemonFormModel("Dusk", "10152", "Rare evening wolf bathing in the green flash of twilight"),
        ),

        // Wishiwashi
        746 to listOf(
            PokemonFormModel("Solo", "746", "Small solitary fry schooling fish"),
            PokemonFormModel("School", "10127", "Colossal sea titan coalesced from millions of Wishiwashi"),
        ),

        // Mimikyu
        778 to listOf(
            PokemonFormModel("Disguised", "778", "Sheltered beneath hand-drawn Pikachu cloth rag"),
            PokemonFormModel("Busted", "10143", "Disguise neck broken following impact"),
        ),

        // Necrozma
        800 to listOf(
            PokemonFormModel("Normal", "800", "Remnant crystalline prism deprived of light"),
            PokemonFormModel("Dusk Mane", "10155", "Absorbed Solgaleo's solar light energy"),
            PokemonFormModel("Dawn Wings", "10156", "Absorbed Lunala's lunar light energy"),
            PokemonFormModel("Ultra", "10157", "Original blinding radiant dragon of pure light"),
        ),

        // Toxtricity
        849 to listOf(
            PokemonFormModel("Amped", "849", "Aggressive lead guitarist with high energy"),
            PokemonFormModel("Low Key", "10160", "Rhythmic bassist with relaxed disposition"),
        ),

        // Urshifu
        892 to listOf(
            PokemonFormModel("Single Strike", "892", "Master of the Tower of Darkness (Fighting/Dark)"),
            PokemonFormModel("Rapid Strike", "10183", "Master of the Tower of Waters (Fighting/Water)"),
        ),

        // Calyrex
        898 to listOf(
            PokemonFormModel("Normal", "898", "Ancient benevolent sovereign of the Crown Tundra"),
            PokemonFormModel("Ice Rider", "10193", "Mounted upon Glastrier, the steed of blizzards"),
            PokemonFormModel("Shadow Rider", "10194", "Mounted upon Spectrier, the steed of darkness"),
        ),

        // Palafin
        964 to listOf(
            PokemonFormModel("Zero", "964", "Inconspicuous playful dolphin form"),
            PokemonFormModel("Hero", "10255", "Mighty superhero dolphin with overwhelming strength"),
        ),

        // Tatsugiri
        978 to listOf(
            PokemonFormModel("Curly", "978", "Orange sushi chef commander"),
            PokemonFormModel("Droopy", "10256", "Pink reclining commander"),
            PokemonFormModel("Stretchy", "10257", "Yellow elongated commander"),
        ),

        // Ogerpon
        1017 to listOf(
            PokemonFormModel("Teal Mask", "1017", "Traditional wooden leaf teal mask (Grass)"),
            PokemonFormModel("Wellspring", "10273", "Shimmering fountain mask (Grass/Water)"),
            PokemonFormModel("Hearthflame", "10274", "Blazing fiery festive mask (Grass/Fire)"),
            PokemonFormModel("Cornerstone", "10275", "Solid mineral bedrock mask (Grass/Rock)"),
        ),

        // Terapagos
        1024 to listOf(
            PokemonFormModel("Normal", "1024", "Dormant baby crystal turtle form"),
            PokemonFormModel("Terastal", "10276", "Active armored turtle shell carrying all elemental types"),
            PokemonFormModel("Stellar", "10277", "Cosmic planetarium form manifesting pure Stellar energy"),
        ),

        // Regional Variants
        19 to listOf(
            PokemonFormModel("Kanto", "19", "Small sharp-toothed city rodent (Normal)"),
            PokemonFormModel("Alolan", "10091", "Nocturnal island scavenger with dark fur (Dark/Normal)"),
        ),
        20 to listOf(
            PokemonFormModel("Kanto", "20", "Aggressive long-whiskered rodent (Normal)"),
            PokemonFormModel("Alolan", "10092", "Heavier island boss with dark fur (Dark/Normal)"),
        ),
        26 to listOf(
            PokemonFormModel("Kanto", "26", "Classic Electric rodent evolution"),
            PokemonFormModel("Alolan", "10100", "Surfs on its tail using psychokinesis (Electric/Psychic)"),
        ),
        27 to listOf(
            PokemonFormModel("Kanto", "27", "Burrowing desert mouse (Ground)"),
            PokemonFormModel("Alolan", "10101", "Snowfield armor plates adapted for icy peaks (Ice/Steel)"),
        ),
        28 to listOf(
            PokemonFormModel("Kanto", "28", "Fast-clawing desert tunneler (Ground)"),
            PokemonFormModel("Alolan", "10102", "Icy steel spines evolved for snowy mountains (Ice/Steel)"),
        ),
        37 to listOf(
            PokemonFormModel("Kanto", "37", "Traditional red fox kitsune (Fire)"),
            PokemonFormModel("Alolan", "10103", "Adapted to snow-capped mountain peaks (Ice)"),
        ),
        38 to listOf(
            PokemonFormModel("Kanto", "38", "Mystic nine-tailed fire spirit (Fire)"),
            PokemonFormModel("Alolan", "10104", "Sacred mountain guardian spirit (Ice/Fairy)"),
        ),
        50 to listOf(
            PokemonFormModel("Kanto", "50", "Tiny burrowing mole trio scout (Ground)"),
            PokemonFormModel("Alolan", "10105", "Island digger with metallic whiskers (Ground/Steel)"),
        ),
        51 to listOf(
            PokemonFormModel("Kanto", "51", "Three-headed tunneling earth mover (Ground)"),
            PokemonFormModel("Alolan", "10106", "Long-haired volcanic soil specialist (Ground/Steel)"),
        ),
        52 to listOf(
            PokemonFormModel("Kanto", "52", "Lucky coin cat (Normal)"),
            PokemonFormModel("Alolan", "10107", "Pampered aristocratic feline (Dark)"),
            PokemonFormModel("Galarian", "10161", "Hardened seafaring Viking cat (Steel)"),
        ),
        53 to listOf(
            PokemonFormModel("Kanto", "53", "Elegant jewel-headed feline (Normal)"),
            PokemonFormModel("Alolan", "10108", "Refined royal feline with a proud temperament (Dark)"),
        ),
        74 to listOf(
            PokemonFormModel("Kanto", "74", "Rock-bodied mountain Pokémon (Rock/Ground)"),
            PokemonFormModel("Alolan", "10109", "Magnetized volcanic rock body (Rock/Electric)"),
        ),
        75 to listOf(
            PokemonFormModel("Kanto", "75", "Boulder-armed mountain climber (Rock/Ground)"),
            PokemonFormModel("Alolan", "10110", "Magnetic mineral body with iron-rich growths (Rock/Electric)"),
        ),
        76 to listOf(
            PokemonFormModel("Kanto", "76", "Massive rolling boulder titan (Rock/Ground)"),
            PokemonFormModel("Alolan", "10111", "Magnetic cannon-bearing volcanic golem (Rock/Electric)"),
        ),
        77 to listOf(
            PokemonFormModel("Kanto", "77", "Fiery maned thoroughbred colt (Fire)"),
            PokemonFormModel("Galarian", "10162", "Fairy woodland pastel horned pony (Psychic)"),
        ),
        78 to listOf(
            PokemonFormModel("Kanto", "78", "Blazing horned steed (Fire)"),
            PokemonFormModel("Galarian", "10163", "Glimmering forest unicorn (Psychic/Fairy)"),
        ),
        79 to listOf(
            PokemonFormModel("Kanto", "79", "Dopey waterfront psychic amphibian (Water/Psychic)"),
            PokemonFormModel("Galarian", "10164", "Spicy-brained shoreline form with latent psychic power (Psychic)"),
        ),
        83 to listOf(
            PokemonFormModel("Kanto", "83", "Leek-wielding wild duck duelist (Normal/Flying)"),
            PokemonFormModel("Galarian", "10166", "Proud leek knight of Galar (Fighting)"),
        ),
        88 to listOf(
            PokemonFormModel("Kanto", "88", "Living sludge born from pollution (Poison)"),
            PokemonFormModel("Alolan", "10112", "Toxic rainbow sludge adapted to island waste (Poison/Dark)"),
        ),
        89 to listOf(
            PokemonFormModel("Kanto", "89", "Massive living toxic sludge (Poison)"),
            PokemonFormModel("Alolan", "10113", "Rainbow crystalline toxin hoarder (Poison/Dark)"),
        ),
        103 to listOf(
            PokemonFormModel("Kanto", "103", "Stout three-headed coconut palm (Grass/Psychic)"),
            PokemonFormModel("Alolan", "10114", "Towering tropical dragon neck tree (Grass/Dragon)"),
        ),
        105 to listOf(
            PokemonFormModel("Kanto", "105", "Bone keeper ground warrior (Ground)"),
            PokemonFormModel("Alolan", "10115", "Twirling spectral emerald fire bones (Fire/Ghost)"),
        ),
        110 to listOf(
            PokemonFormModel("Kanto", "110", "Toxic twin-head pollution smog (Poison)"),
            PokemonFormModel("Galarian", "10167", "Top-hat smokestack purifying chimney (Poison/Fairy)"),
        ),
        122 to listOf(
            PokemonFormModel("Kanto", "122", "Pantomime barrier artist (Psychic/Fairy)"),
            PokemonFormModel("Galarian", "10168", "Tap-dancing frozen performer (Ice/Psychic)"),
        ),
        128 to listOf(
            PokemonFormModel("Kanto", "128", "Single-breed wild charging bull (Normal)"),
            PokemonFormModel("Paldean Combat", "10250", "Black-maned Fighting breed of Paldea (Fighting)"),
            PokemonFormModel("Paldean Blaze", "10251", "Fire-charged Paldean breed with crimson horns (Fighting/Fire)"),
            PokemonFormModel("Paldean Aqua", "10252", "Water-charged Paldean breed with blue accents (Fighting/Water)"),
        ),
        144 to listOf(
            PokemonFormModel("Kanto", "144", "Legendary ice bird of frozen skies (Ice/Flying)"),
            PokemonFormModel("Galarian", "10169", "Psychic duelist with a chilling gaze (Psychic/Flying)"),
        ),
        145 to listOf(
            PokemonFormModel("Kanto", "145", "Legendary thunder bird of storm clouds (Electric/Flying)"),
            PokemonFormModel("Galarian", "10170", "Lightning-fast battle runner (Fighting/Flying)"),
        ),
        146 to listOf(
            PokemonFormModel("Kanto", "146", "Legendary fire bird wrapped in flame (Fire/Flying)"),
            PokemonFormModel("Galarian", "10171", "Dark flame phoenix with sinister aura (Dark/Flying)"),
        ),
        194 to listOf(
            PokemonFormModel("Johto", "194", "Water mud fish axolotl (Water/Ground)"),
            PokemonFormModel("Paldean", "10253", "Poison mud dweller (Poison/Ground)"),
        ),
        199 to listOf(
            PokemonFormModel("Johto", "199", "Royal Shellder-crowned sage (Water/Psychic)"),
            PokemonFormModel("Galarian", "10172", "Poisonous Shellder crown channels strange spells (Poison/Psychic)"),
        ),
        222 to listOf(
            PokemonFormModel("Johto", "222", "Bright coral branch Pokémon (Water/Rock)"),
            PokemonFormModel("Galarian", "10173", "Bleached ghost coral from ancient seas (Ghost)"),
        ),
        263 to listOf(
            PokemonFormModel("Hoenn", "263", "Zigzag-patterned restless raccoon Pokémon (Normal)"),
            PokemonFormModel("Galarian", "10174", "Black-and-white wild runner (Dark/Normal)"),
        ),
        264 to listOf(
            PokemonFormModel("Hoenn", "264", "Straight-line rushing evolution (Normal)"),
            PokemonFormModel("Galarian", "10175", "Defiant dark-striped sprinter (Dark/Normal)"),
        ),
        554 to listOf(
            PokemonFormModel("Unova", "554", "Fire-powered round desert spirit (Fire)"),
            PokemonFormModel("Galarian", "10176", "Snowball-shaped cold-climate form (Ice)"),
        ),
        555 to listOf(
            PokemonFormModel("Unova", "555", "Blazing ape-like powerhouse (Fire)"),
            PokemonFormModel("Galarian", "10177", "Ice-forged snowman powerhouse (Ice)"),
            PokemonFormModel("Galarian Zen", "10178", "Zen Mode awakened in an icy body (Ice/Fire)"),
        ),
        562 to listOf(
            PokemonFormModel("Unova", "562", "Ancient mask-bearing spirit (Ghost)"),
            PokemonFormModel("Galarian", "10179", "Cursed clay tablet spirit (Ground/Ghost)"),
        ),
        570 to listOf(
            PokemonFormModel("Unova", "570", "Tricky illusionist dark fox (Dark)"),
            PokemonFormModel("Hisuian", "10238", "Ghostly revenge spirit draped in spectral fleece (Normal/Ghost)"),
        ),
        571 to listOf(
            PokemonFormModel("Unova", "571", "Master of deceit and shadow (Dark)"),
            PokemonFormModel("Hisuian", "10239", "Vengeful spiteful spirit with billowing locks (Normal/Ghost)"),
        ),
        618 to listOf(
            PokemonFormModel("Unova", "618", "Flat mud-trap fish charged with electricity (Ground/Electric)"),
            PokemonFormModel("Galarian", "10180", "Bear-trap patterned metal mud fish (Ground/Steel)"),
        ),
    )
}

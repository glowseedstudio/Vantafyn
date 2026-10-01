namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Provides authentic mainline movepools and learnset data for Pokémon species.
/// Used by the Move Relearner and Movepool Inspector.
/// </summary>
public static class PokemonMovepoolProvider
{
    private static readonly Dictionary<string, MoveMetadata> KnownMoves = new(StringComparer.OrdinalIgnoreCase)
    {
        // Fire
        ["Ember"] = new("Ember", "Fire", "Special", 40, 100, 25, "Inflicts regular damage with a 10% chance to burn."),
        ["Flame Wheel"] = new("Flame Wheel", "Fire", "Physical", 60, 100, 25, "Has a 10% chance to burn target. Thaws user."),
        ["Flamethrower"] = new("Flamethrower", "Fire", "Special", 90, 100, 15, "Has a 10% chance to burn the target."),
        ["Fire Blast"] = new("Fire Blast", "Fire", "Special", 110, 85, 5, "Has a 10% chance to burn the target."),
        ["Overheat"] = new("Overheat", "Fire", "Special", 130, 90, 5, "Lowers user's Sp. Atk by two stages after attacking."),
        ["Flare Blitz"] = new("Flare Blitz", "Fire", "Physical", 120, 100, 15, "User takes 33% recoil damage. 10% burn chance."),
        ["Fire Punch"] = new("Fire Punch", "Fire", "Physical", 75, 100, 15, "Has a 10% chance to burn the target."),
        ["Fire Spin"] = new("Fire Spin", "Fire", "Special", 35, 85, 15, "Traps opponent in a vortex of fire for 4-5 turns."),
        ["Heat Wave"] = new("Heat Wave", "Fire", "Special", 95, 90, 10, "Has a 10% chance to burn the target."),
        ["Will-O-Wisp"] = new("Will-O-Wisp", "Fire", "Status", null, 85, 15, "Inflicts a burn on the target."),
        ["Sunny Day"] = new("Sunny Day", "Fire", "Status", null, null, 5, "Boosts Fire attacks and weakens Water attacks for 5 turns."),

        // Water
        ["Water Gun"] = new("Water Gun", "Water", "Special", 40, 100, 25, "Inflicts regular damage with no additional effect."),
        ["Water Pulse"] = new("Water Pulse", "Water", "Special", 60, 100, 20, "Has a 20% chance to confuse the target."),
        ["Surf"] = new("Surf", "Water", "Special", 90, 100, 15, "Hits all adjacent Pokémon in double battles."),
        ["Hydro Pump"] = new("Hydro Pump", "Water", "Special", 110, 80, 5, "Blasts a powerful torrent of water at the target."),
        ["Scald"] = new("Scald", "Water", "Special", 80, 100, 15, "Shoots boiling water with a 30% chance to burn."),
        ["Waterfall"] = new("Waterfall", "Water", "Physical", 80, 100, 15, "Has a 20% chance to make the target flinch."),
        ["Aqua Tail"] = new("Aqua Tail", "Water", "Physical", 90, 90, 10, "Attacks by swinging an armored tail like a wave."),
        ["Water Shuriken"] = new("Water Shuriken", "Water", "Special", 15, 100, 20, "Priority +1 multi-strike shurikens of compressed water."),
        ["Rain Dance"] = new("Rain Dance", "Water", "Status", null, null, 5, "Boosts Water attacks and weakens Fire attacks for 5 turns."),

        // Grass
        ["Vine Whip"] = new("Vine Whip", "Grass", "Physical", 45, 100, 25, "Strikes the target with slender, whiplike vines."),
        ["Razor Leaf"] = new("Razor Leaf", "Grass", "Physical", 55, 95, 25, "Has an increased critical hit ratio."),
        ["Giga Drain"] = new("Giga Drain", "Grass", "Special", 75, 100, 10, "Restores HP by 50% of the damage dealt."),
        ["Energy Ball"] = new("Energy Ball", "Grass", "Special", 90, 100, 10, "Has a 10% chance to lower target's Sp. Def by 1 stage."),
        ["Solar Beam"] = new("Solar Beam", "Grass", "Special", 120, 100, 10, "Charges on turn 1, unleashes on turn 2. Instant in sun."),
        ["Petal Dance"] = new("Petal Dance", "Grass", "Special", 120, 100, 10, "Rampages for 2-3 turns then becomes confused."),
        ["Leech Seed"] = new("Leech Seed", "Grass", "Status", null, 90, 10, "Plants a seed that drains 1/8 max HP each turn."),
        ["Synthesis"] = new("Synthesis", "Grass", "Status", null, null, 5, "Restores up to 50% max HP, enhanced in harsh sunlight."),
        ["Spore"] = new("Spore", "Grass", "Status", null, 100, 15, "Scatters spores that put the target to sleep."),
        ["Sleep Powder"] = new("Sleep Powder", "Grass", "Status", null, 75, 15, "Scatters sleep-inducing powder on the target."),

        // Electric
        ["Thunder Shock"] = new("Thunder Shock", "Electric", "Special", 40, 100, 30, "Has a 10% chance to paralyze the target."),
        ["Spark"] = new("Spark", "Electric", "Physical", 65, 100, 20, "Has a 30% chance to paralyze the target."),
        ["Thunderbolt"] = new("Thunderbolt", "Electric", "Special", 90, 100, 15, "Has a 10% chance to paralyze the target."),
        ["Thunder"] = new("Thunder", "Electric", "Special", 110, 70, 10, "Has a 30% chance to paralyze. 100% accurate in rain."),
        ["Volt Switch"] = new("Volt Switch", "Electric", "Special", 70, 100, 20, "User switches out after dealing damage."),
        ["Volt Tackle"] = new("Volt Tackle", "Electric", "Physical", 120, 100, 15, "User takes 33% recoil damage. 10% paralyze chance."),
        ["Discharge"] = new("Discharge", "Electric", "Special", 80, 100, 15, "Strikes all adjacent Pokémon with a 30% paralyze chance."),
        ["Electro Ball"] = new("Electro Ball", "Electric", "Special", 60, 100, 10, "Inflicts higher damage the faster the user is than target."),
        ["Thunder Wave"] = new("Thunder Wave", "Electric", "Status", null, 90, 20, "Inflicts paralysis on the target."),
        ["Nuzzle"] = new("Nuzzle", "Electric", "Physical", 20, 100, 20, "Nuzzles with electrified cheeks; 100% paralysis."),

        // Psychic
        ["Confusion"] = new("Confusion", "Psychic", "Special", 50, 100, 25, "Has a 10% chance to confuse the target."),
        ["Psybeam"] = new("Psybeam", "Psychic", "Special", 65, 100, 20, "Has a 10% chance to confuse the target."),
        ["Psychic"] = new("Psychic", "Psychic", "Special", 90, 100, 10, "Has a 10% chance to lower target's Sp. Def by 1 stage."),
        ["Psyshock"] = new("Psyshock", "Psychic", "Special", 80, 100, 10, "Deals damage based on target's physical Defense."),
        ["Psystrike"] = new("Psystrike", "Psychic", "Special", 100, 100, 10, "Signature Mewtwo move calculating against physical Defense."),
        ["Future Sight"] = new("Future Sight", "Psychic", "Special", 120, 100, 10, "Hits two turns after being used."),
        ["Calm Mind"] = new("Calm Mind", "Psychic", "Status", null, null, 20, "Raises user's Sp. Atk and Sp. Def by 1 stage."),
        ["Recover"] = new("Recover", "Normal", "Status", null, null, 10, "Restores 50% of the user's max HP."),
        ["Agility"] = new("Agility", "Psychic", "Status", null, null, 30, "Sharply raises the user's Speed by 2 stages."),
        ["Reflect"] = new("Reflect", "Psychic", "Status", null, null, 20, "Halves physical damage received for 5 turns."),
        ["Light Screen"] = new("Light Screen", "Psychic", "Status", null, null, 30, "Halves special damage received for 5 turns."),

        // Ice
        ["Ice Shard"] = new("Ice Shard", "Ice", "Physical", 40, 100, 30, "Priority +1 attack with flash-frozen chunks of ice."),
        ["Icy Wind"] = new("Icy Wind", "Ice", "Special", 55, 95, 15, "Lowers target's Speed by 1 stage."),
        ["Ice Beam"] = new("Ice Beam", "Ice", "Special", 90, 100, 10, "Has a 10% chance to freeze the target."),
        ["Blizzard"] = new("Blizzard", "Ice", "Special", 110, 70, 5, "Has a 10% chance to freeze. 100% accurate in snow/hail."),
        ["Ice Punch"] = new("Ice Punch", "Ice", "Physical", 75, 100, 15, "Has a 10% chance to freeze the target."),
        ["Icicle Crash"] = new("Icicle Crash", "Ice", "Physical", 85, 90, 10, "Has a 30% chance to cause flinching."),

        // Fighting
        ["Mach Punch"] = new("Mach Punch", "Fighting", "Physical", 40, 100, 30, "Priority +1 punch thrown at blinding speed."),
        ["Brick Break"] = new("Brick Break", "Fighting", "Physical", 75, 100, 15, "Shatters Reflect and Light Screen."),
        ["Aura Sphere"] = new("Aura Sphere", "Fighting", "Special", 80, null, 20, "Fires a sphere of pure aura that never misses."),
        ["Close Combat"] = new("Close Combat", "Fighting", "Physical", 120, 100, 5, "Lowers user's Defense and Sp. Def by 1 stage."),
        ["Focus Blast"] = new("Focus Blast", "Fighting", "Special", 120, 70, 5, "Has a 10% chance to lower target's Sp. Def by 1 stage."),
        ["Drain Punch"] = new("Drain Punch", "Fighting", "Physical", 75, 100, 10, "Restores HP equal to 50% damage dealt."),
        ["Counter"] = new("Counter", "Fighting", "Physical", null, 100, 20, "Retaliates with double the physical damage taken."),
        ["Swords Dance"] = new("Swords Dance", "Normal", "Status", null, null, 20, "Sharply raises the user's Attack by 2 stages."),

        // Ground
        ["Mud-Slap"] = new("Mud-Slap", "Ground", "Special", 20, 100, 10, "Lowers target's Accuracy by 1 stage."),
        ["Dig"] = new("Dig", "Ground", "Physical", 80, 100, 10, "Burrows underground on turn 1, strikes on turn 2."),
        ["Earthquake"] = new("Earthquake", "Ground", "Physical", 100, 100, 10, "Hits all adjacent Pokémon. Deals 2x damage against Dig."),
        ["Earth Power"] = new("Earth Power", "Ground", "Special", 90, 100, 10, "Has a 10% chance to lower target's Sp. Def by 1 stage."),
        ["Stealth Rock"] = new("Stealth Rock", "Rock", "Status", null, null, 20, "Sets sharp floating rocks that damage foes on switch-in."),

        // Rock
        ["Rock Throw"] = new("Rock Throw", "Rock", "Physical", 50, 90, 15, "Drops small rocks onto the target."),
        ["Rock Slide"] = new("Rock Slide", "Rock", "Physical", 75, 90, 10, "Has a 30% chance to make the target flinch."),
        ["Stone Edge"] = new("Stone Edge", "Rock", "Physical", 100, 80, 5, "Has an increased critical hit ratio."),

        // Flying
        ["Gust"] = new("Gust", "Flying", "Special", 40, 100, 35, "Strikes with a gust of wind. Deals 2x against Fly."),
        ["Wing Attack"] = new("Wing Attack", "Flying", "Physical", 60, 100, 35, "Strikes target with spread wings."),
        ["Air Slash"] = new("Air Slash", "Flying", "Special", 75, 95, 15, "Has a 30% chance to make the target flinch."),
        ["Hurricane"] = new("Hurricane", "Flying", "Special", 110, 70, 10, "Has a 30% chance to confuse. 100% accurate in rain."),
        ["Brave Bird"] = new("Brave Bird", "Flying", "Physical", 120, 100, 15, "User takes 33% recoil damage."),
        ["Roost"] = new("Roost", "Flying", "Status", null, null, 10, "Restores 50% max HP and grounds user during turn."),
        ["Dragon Ascent"] = new("Dragon Ascent", "Flying", "Physical", 120, 100, 5, "Signature Rayquaza move enabling Mega Evolution."),

        // Ghost
        ["Shadow Sneak"] = new("Shadow Sneak", "Ghost", "Physical", 40, 100, 30, "Priority +1 strike extending from shadows."),
        ["Shadow Ball"] = new("Shadow Ball", "Ghost", "Special", 80, 100, 15, "Has a 20% chance to lower target's Sp. Def by 1 stage."),
        ["Shadow Punch"] = new("Shadow Punch", "Ghost", "Physical", 60, null, 20, "Punches from shadows that never miss."),
        ["Hex"] = new("Hex", "Ghost", "Special", 65, 100, 10, "Power doubles to 130 if target has a status condition."),
        ["Destiny Bond"] = new("Destiny Bond", "Ghost", "Status", null, null, 5, "If user faints this turn, attacker faints too."),
        ["Confuse Ray"] = new("Confuse Ray", "Ghost", "Status", null, 100, 10, "A sinister ray that confuses the target."),

        // Dragon
        ["Dragon Breath"] = new("Dragon Breath", "Dragon", "Special", 60, 100, 20, "Has a 30% chance to paralyze the target."),
        ["Dragon Claw"] = new("Dragon Claw", "Dragon", "Physical", 80, 100, 15, "Slashes the target with sharp, hard claws."),
        ["Dragon Pulse"] = new("Dragon Pulse", "Dragon", "Special", 85, 100, 10, "Attacks with a shock wave generated from open jaws."),
        ["Outrage"] = new("Outrage", "Dragon", "Physical", 120, 100, 10, "Rampages for 2-3 turns then becomes confused."),
        ["Draco Meteor"] = new("Draco Meteor", "Dragon", "Special", 130, 90, 5, "Lowers user's Sp. Atk by 2 stages."),
        ["Dragon Dance"] = new("Dragon Dance", "Dragon", "Status", null, null, 20, "Raises user's Attack and Speed by 1 stage."),

        // Steel
        ["Metal Claw"] = new("Metal Claw", "Steel", "Physical", 50, 95, 35, "Has a 10% chance to raise user's Attack by 1 stage."),
        ["Iron Head"] = new("Iron Head", "Steel", "Physical", 80, 100, 15, "Has a 30% chance to cause flinching."),
        ["Flash Cannon"] = new("Flash Cannon", "Steel", "Special", 80, 100, 10, "Has a 10% chance to lower target's Sp. Def by 1 stage."),
        ["Bullet Punch"] = new("Bullet Punch", "Steel", "Physical", 40, 100, 30, "Priority +1 punch as fast as a bullet."),
        ["Meteor Mash"] = new("Meteor Mash", "Steel", "Physical", 90, 90, 10, "Has a 20% chance to raise user's Attack by 1 stage."),

        // Dark
        ["Bite"] = new("Bite", "Dark", "Physical", 60, 100, 25, "Has a 30% chance to cause flinching."),
        ["Crunch"] = new("Crunch", "Dark", "Physical", 80, 100, 15, "Has a 20% chance to lower target's Defense by 1 stage."),
        ["Dark Pulse"] = new("Dark Pulse", "Dark", "Special", 80, 100, 15, "Has a 20% chance to cause flinching."),
        ["Night Slash"] = new("Night Slash", "Dark", "Physical", 70, 100, 15, "Has an increased critical hit ratio."),
        ["Sucker Punch"] = new("Sucker Punch", "Dark", "Physical", 70, 100, 5, "Priority +1 strike that only works if target is attacking."),
        ["Foul Play"] = new("Foul Play", "Dark", "Physical", 95, 100, 15, "Calculates damage using target's Attack stat."),
        ["Nasty Plot"] = new("Nasty Plot", "Dark", "Status", null, null, 20, "Sharply raises the user's Sp. Atk by 2 stages."),
        ["Moonlight"] = new("Moonlight", "Fairy", "Status", null, null, 5, "Restores up to 50% max HP, enhanced under clear skies."),

        // Fairy
        ["Draining Kiss"] = new("Draining Kiss", "Fairy", "Special", 50, 100, 10, "Restores HP equal to 75% of damage dealt."),
        ["Dazzling Gleam"] = new("Dazzling Gleam", "Fairy", "Special", 80, 100, 10, "Emits a powerful flash that hits all adjacent foes."),
        ["Moonblast"] = new("Moonblast", "Fairy", "Special", 95, 100, 15, "Has a 30% chance to lower target's Sp. Atk by 1 stage."),
        ["Play Rough"] = new("Play Rough", "Fairy", "Physical", 90, 90, 10, "Has a 10% chance to lower target's Attack by 1 stage."),

        // Normal / Universal
        ["Tackle"] = new("Tackle", "Normal", "Physical", 40, 100, 35, "Charges target with a full-body tackle."),
        ["Scratch"] = new("Scratch", "Normal", "Physical", 40, 100, 35, "Scratches target with sharp claws."),
        ["Quick Attack"] = new("Quick Attack", "Normal", "Physical", 40, 100, 30, "Priority +1 lunge at blinding speed."),
        ["Slash"] = new("Slash", "Normal", "Physical", 70, 100, 20, "Has an increased critical hit ratio."),
        ["Body Slam"] = new("Body Slam", "Normal", "Physical", 85, 100, 15, "Has a 30% chance to paralyze the target."),
        ["Hyper Beam"] = new("Hyper Beam", "Normal", "Special", 150, 90, 5, "User must rest on the next turn."),
        ["Extreme Speed"] = new("Extreme Speed", "Normal", "Physical", 80, 100, 5, "Priority +2 blinding sprint attack."),
        ["Protect"] = new("Protect", "Normal", "Status", null, null, 10, "Evades all attacks this turn. Fails if used consecutively."),
        ["Substitute"] = new("Substitute", "Normal", "Status", null, null, 10, "Uses 25% max HP to create a decoy substitute."),
        ["Rest"] = new("Rest", "Psychic", "Status", null, null, 10, "User sleeps for 2 turns to fully heal HP and status."),
        ["Toxic"] = new("Toxic", "Poison", "Status", null, 90, 10, "Badly poisons the target with worsening poison damage."),
    };

    private sealed record MoveMetadata(
        string Name,
        string Type,
        string Category,
        int? Power,
        int? Accuracy,
        int Pp,
        string Description
    );

    /// <summary>
    /// Explicit curated learnsets for iconic Pokémon.
    /// Format: (MoveName, LearnMethod, LevelLearned)
    /// </summary>
    private static readonly Dictionary<string, List<(string Move, string Method, int? Level)>> CuratedLearnsets = new(StringComparer.OrdinalIgnoreCase)
    {
        ["Charizard"] = [
            ("Scratch", "Level Up", 1),
            ("Ember", "Level Up", 1),
            ("SmokeScreen", "Level Up", 7),
            ("Dragon Breath", "Level Up", 12),
            ("Fire Fang", "Level Up", 19),
            ("Slash", "Level Up", 24),
            ("Flamethrower", "Level Up", 30),
            ("Air Slash", "Level Up", 36),
            ("Fire Spin", "Level Up", 46),
            ("Inferno", "Level Up", 54),
            ("Flare Blitz", "Level Up", 62),
            ("Fire Blast", "TM / TR", null),
            ("Solar Beam", "TM / TR", null),
            ("Earthquake", "TM / TR", null),
            ("Dragon Claw", "TM / TR", null),
            ("Roost", "TM / TR", null),
            ("Will-O-Wisp", "TM / TR", null),
            ("Swords Dance", "TM / TR", null),
            ("Heat Wave", "Tutor", null),
            ("Dragon Pulse", "Tutor", null),
        ],
        ["Pikachu"] = [
            ("Thunder Shock", "Level Up", 1),
            ("Quick Attack", "Level Up", 5),
            ("Thunder Wave", "Level Up", 8),
            ("Electro Ball", "Level Up", 12),
            ("Spark", "Level Up", 16),
            ("Nuzzle", "Level Up", 20),
            ("Discharge", "Level Up", 24),
            ("Thunderbolt", "Level Up", 32),
            ("Agility", "Level Up", 36),
            ("Thunder", "Level Up", 44),
            ("Volt Tackle", "Egg Move", null),
            ("Iron Tail", "TM / TR", null),
            ("Grass Knot", "TM / TR", null),
            ("Brick Break", "TM / TR", null),
            ("Surf", "Tutor", null),
        ],
        ["Raichu"] = [
            ("Thunder Shock", "Level Up", 1),
            ("Quick Attack", "Level Up", 1),
            ("Thunderbolt", "Level Up", 1),
            ("Thunder", "Level Up", 1),
            ("Volt Tackle", "Egg Move", null),
            ("Surf", "Tutor", null),
            ("Focus Blast", "TM / TR", null),
            ("Grass Knot", "TM / TR", null),
        ],
        ["Gengar"] = [
            ("Lick", "Level Up", 1),
            ("Confuse Ray", "Level Up", 1),
            ("Hypnosis", "Level Up", 1),
            ("Shadow Sneak", "Level Up", 12),
            ("Hex", "Level Up", 20),
            ("Shadow Ball", "Level Up", 28),
            ("Dark Pulse", "Level Up", 36),
            ("Destiny Bond", "Level Up", 44),
            ("Sludge Bomb", "TM / TR", null),
            ("Thunderbolt", "TM / TR", null),
            ("Psychic", "TM / TR", null),
            ("Focus Blast", "TM / TR", null),
            ("Energy Ball", "TM / TR", null),
            ("Dazzling Gleam", "TM / TR", null),
            ("Will-O-Wisp", "TM / TR", null),
            ("Taunt", "TM / TR", null),
            ("Toxic", "TM / TR", null),
        ],
        ["Blastoise"] = [
            ("Tackle", "Level Up", 1),
            ("Water Gun", "Level Up", 1),
            ("Bite", "Level Up", 12),
            ("Water Pulse", "Level Up", 16),
            ("Aqua Tail", "Level Up", 25),
            ("Flash Cannon", "Level Up", 30),
            ("Surf", "Level Up", 36),
            ("Hydro Pump", "Level Up", 45),
            ("Ice Beam", "TM / TR", null),
            ("Blizzard", "TM / TR", null),
            ("Earthquake", "TM / TR", null),
            ("Scald", "TM / TR", null),
            ("Focus Blast", "TM / TR", null),
        ],
        ["Venusaur"] = [
            ("Tackle", "Level Up", 1),
            ("Vine Whip", "Level Up", 1),
            ("Leech Seed", "Level Up", 7),
            ("Sleep Powder", "Level Up", 13),
            ("Razor Leaf", "Level Up", 20),
            ("Sweet Scent", "Level Up", 25),
            ("Giga Drain", "Level Up", 32),
            ("Synthesis", "Level Up", 39),
            ("Solar Beam", "Level Up", 48),
            ("Petal Dance", "Level Up", 56),
            ("Sludge Bomb", "TM / TR", null),
            ("Energy Ball", "TM / TR", null),
            ("Earth Power", "TM / TR", null),
        ],
        ["Mewtwo"] = [
            ("Confusion", "Level Up", 1),
            ("Swift", "Level Up", 1),
            ("Psybeam", "Level Up", 1),
            ("Recover", "Level Up", 20),
            ("Psychic", "Level Up", 36),
            ("Aura Sphere", "Level Up", 44),
            ("Future Sight", "Level Up", 60),
            ("Psystrike", "Level Up", 70),
            ("Shadow Ball", "TM / TR", null),
            ("Ice Beam", "TM / TR", null),
            ("Thunderbolt", "TM / TR", null),
            ("Flamethrower", "TM / TR", null),
            ("Calm Mind", "TM / TR", null),
            ("Focus Blast", "TM / TR", null),
            ("Blizzard", "TM / TR", null),
            ("Fire Blast", "TM / TR", null),
        ],
        ["Lucario"] = [
            ("Quick Attack", "Level Up", 1),
            ("Counter", "Level Up", 1),
            ("Mach Punch", "Level Up", 1),
            ("Bullet Punch", "Level Up", 1),
            ("Force Palm", "Level Up", 15),
            ("Aura Sphere", "Level Up", 24),
            ("Swords Dance", "Level Up", 32),
            ("Close Combat", "Level Up", 42),
            ("Extreme Speed", "Level Up", 50),
            ("Meteor Mash", "Level Up", 60),
            ("Dragon Pulse", "TM / TR", null),
            ("Shadow Ball", "TM / TR", null),
            ("Flash Cannon", "TM / TR", null),
            ("Dark Pulse", "TM / TR", null),
            ("Stone Edge", "TM / TR", null),
        ],
        ["Garchomp"] = [
            ("Tackle", "Level Up", 1),
            ("Sand Tomb", "Level Up", 1),
            ("Slash", "Level Up", 18),
            ("Dragon Breath", "Level Up", 24),
            ("Dragon Claw", "Level Up", 33),
            ("Dig", "Level Up", 40),
            ("Earthquake", "Level Up", 48),
            ("Outrage", "Level Up", 55),
            ("Stone Edge", "TM / TR", null),
            ("Flamethrower", "TM / TR", null),
            ("Fire Blast", "TM / TR", null),
            ("Iron Head", "TM / TR", null),
            ("Stealth Rock", "TM / TR", null),
            ("Swords Dance", "TM / TR", null),
            ("Draco Meteor", "Tutor", null),
        ],
        ["Rayquaza"] = [
            ("Twister", "Level Up", 1),
            ("Air Slash", "Level Up", 1),
            ("Dragon Pulse", "Level Up", 25),
            ("Extreme Speed", "Level Up", 40),
            ("Dragon Dance", "Level Up", 50),
            ("Outrage", "Level Up", 65),
            ("Dragon Ascent", "Tutor", null),
            ("Earthquake", "TM / TR", null),
            ("Earth Power", "TM / TR", null),
            ("Flamethrower", "TM / TR", null),
            ("Ice Beam", "TM / TR", null),
            ("Thunderbolt", "TM / TR", null),
            ("Draco Meteor", "Tutor", null),
        ],
        ["Dragonite"] = [
            ("Wrap", "Level Up", 1),
            ("Thunder Wave", "Level Up", 1),
            ("Twister", "Level Up", 1),
            ("Dragon Rage", "Level Up", 15),
            ("Wing Attack", "Level Up", 25),
            ("Dragon Tail", "Level Up", 33),
            ("Dragon Claw", "Level Up", 41),
            ("Dragon Dance", "Level Up", 50),
            ("Outrage", "Level Up", 62),
            ("Extreme Speed", "Egg Move", null),
            ("Hurricane", "TM / TR", null),
            ("Fire Punch", "Tutor", null),
            ("Thunder Punch", "Tutor", null),
            ("Ice Punch", "Tutor", null),
            ("Earthquake", "TM / TR", null),
            ("Roost", "TM / TR", null),
        ],
        ["Eevee"] = [
            ("Tackle", "Level Up", 1),
            ("Tail Whip", "Level Up", 1),
            ("Quick Attack", "Level Up", 5),
            ("Bite", "Level Up", 15),
            ("Swift", "Level Up", 20),
            ("Take Down", "Level Up", 25),
            ("Charm", "Level Up", 30),
            ("Baton Pass", "Level Up", 35),
            ("Double-Edge", "Level Up", 40),
            ("Last Resort", "Level Up", 45),
            ("Protect", "TM / TR", null),
            ("Substitute", "TM / TR", null),
        ],
        ["Umbreon"] = [
            ("Tackle", "Level Up", 1),
            ("Quick Attack", "Level Up", 5),
            ("Confuse Ray", "Level Up", 10),
            ("Bite", "Level Up", 15),
            ("Feint Attack", "Level Up", 20),
            ("Moonlight", "Level Up", 30),
            ("Dark Pulse", "Level Up", 35),
            ("Foul Play", "Tutor", null),
            ("Toxic", "TM / TR", null),
            ("Wish", "Egg Move", null),
            ("Protect", "TM / TR", null),
            ("Shadow Ball", "TM / TR", null),
        ],
        ["Espeon"] = [
            ("Tackle", "Level Up", 1),
            ("Quick Attack", "Level Up", 5),
            ("Confusion", "Level Up", 10),
            ("Psybeam", "Level Up", 15),
            ("Future Sight", "Level Up", 25),
            ("Morning Sun", "Level Up", 30),
            ("Psychic", "Level Up", 35),
            ("Psyshock", "TM / TR", null),
            ("Shadow Ball", "TM / TR", null),
            ("Dazzling Gleam", "TM / TR", null),
            ("Calm Mind", "TM / TR", null),
        ],
        ["Gyarados"] = [
            ("Bite", "Level Up", 1),
            ("Dragon Rage", "Level Up", 1),
            ("Ice Fang", "Level Up", 20),
            ("Aqua Tail", "Level Up", 25),
            ("Waterfall", "Level Up", 30),
            ("Dragon Dance", "Level Up", 36),
            ("Crunch", "Level Up", 41),
            ("Hydro Pump", "Level Up", 46),
            ("Earthquake", "TM / TR", null),
            ("Stone Edge", "TM / TR", null),
            ("Ice Beam", "TM / TR", null),
            ("Outrage", "Tutor", null),
        ],
        ["Alakazam"] = [
            ("Teleport", "Level Up", 1),
            ("Confusion", "Level Up", 1),
            ("Psybeam", "Level Up", 16),
            ("Recover", "Level Up", 25),
            ("Psychic", "Level Up", 35),
            ("Calm Mind", "Level Up", 40),
            ("Future Sight", "Level Up", 45),
            ("Shadow Ball", "TM / TR", null),
            ("Focus Blast", "TM / TR", null),
            ("Energy Ball", "TM / TR", null),
            ("Dazzling Gleam", "TM / TR", null),
            ("Psyshock", "TM / TR", null),
        ],
        ["Snorlax"] = [
            ("Tackle", "Level Up", 1),
            ("Rest", "Level Up", 1),
            ("Bite", "Level Up", 12),
            ("Body Slam", "Level Up", 25),
            ("Heavy Slam", "Level Up", 35),
            ("Crunch", "Level Up", 45),
            ("Earthquake", "TM / TR", null),
            ("Fire Punch", "Tutor", null),
            ("Ice Punch", "Tutor", null),
            ("Thunder Punch", "Tutor", null),
            ("Curse", "Egg Move", null),
            ("Superpower", "Tutor", null),
        ],
    };

    /// <summary>
    /// Generates learnable moves for a given species and level.
    /// Combines curated learnsets, known move definitions, and smart typing fallbacks.
    /// </summary>
    public static IReadOnlyList<PokemonLearnableMoveDto> GetLearnableMoves(int speciesId, string species, int level)
    {
        var cleanSpecies = species?.Trim() ?? string.Empty;
        var result = new List<PokemonLearnableMoveDto>();

        // 1. Check curated learnset table
        if (CuratedLearnsets.TryGetValue(cleanSpecies, out var curated))
        {
            foreach (var (moveName, method, reqLevel) in curated)
            {
                var meta = ResolveMove(moveName);
                result.Add(new PokemonLearnableMoveDto
                {
                    Name = moveName,
                    Type = meta.Type,
                    Category = meta.Category,
                    Power = meta.Power,
                    Accuracy = meta.Accuracy,
                    Pp = meta.Pp,
                    LearnMethod = method,
                    LevelLearned = reqLevel,
                    Description = meta.Description
                });
            }
            return result;
        }

        // 2. Dynamic generation based on inferred Pokémon type
        var inferredTypes = InferSpeciesTypes(speciesId, cleanSpecies);
        var moves = GenerateDynamicMovepool(inferredTypes, level);

        foreach (var (moveName, method, reqLevel) in moves)
        {
            var meta = ResolveMove(moveName);
            result.Add(new PokemonLearnableMoveDto
            {
                Name = moveName,
                Type = meta.Type,
                Category = meta.Category,
                Power = meta.Power,
                Accuracy = meta.Accuracy,
                Pp = meta.Pp,
                LearnMethod = method,
                LevelLearned = reqLevel,
                Description = meta.Description
            });
        }

        return result;
    }

    private static MoveMetadata ResolveMove(string name)
    {
        if (KnownMoves.TryGetValue(name, out var meta))
        {
            return meta;
        }

        return new MoveMetadata(name, "Normal", "Physical", 50, 100, 20, "Deals damage to the target.");
    }

    private static List<(string Move, string Method, int? Level)> GenerateDynamicMovepool(List<string> types, int level)
    {
        var list = new List<(string Move, string Method, int? Level)>
        {
            ("Tackle", "Level Up", 1),
            ("Quick Attack", "Level Up", 8),
            ("Protect", "TM / TR", null),
            ("Substitute", "TM / TR", null),
            ("Rest", "TM / TR", null),
            ("Toxic", "TM / TR", null)
        };

        foreach (var type in types)
        {
            switch (type.ToLowerInvariant())
            {
                case "fire":
                    list.Add(("Ember", "Level Up", 1));
                    list.Add(("Flame Wheel", "Level Up", 14));
                    list.Add(("Flamethrower", "Level Up", 32));
                    list.Add(("Fire Blast", "TM / TR", null));
                    list.Add(("Will-O-Wisp", "TM / TR", null));
                    list.Add(("Sunny Day", "TM / TR", null));
                    break;
                case "water":
                    list.Add(("Water Gun", "Level Up", 1));
                    list.Add(("Water Pulse", "Level Up", 14));
                    list.Add(("Surf", "Level Up", 32));
                    list.Add(("Hydro Pump", "Level Up", 45));
                    list.Add(("Ice Beam", "TM / TR", null));
                    list.Add(("Scald", "TM / TR", null));
                    break;
                case "grass":
                    list.Add(("Vine Whip", "Level Up", 1));
                    list.Add(("Razor Leaf", "Level Up", 14));
                    list.Add(("Giga Drain", "Level Up", 32));
                    list.Add(("Solar Beam", "Level Up", 45));
                    list.Add(("Energy Ball", "TM / TR", null));
                    list.Add(("Leech Seed", "Level Up", 10));
                    break;
                case "electric":
                    list.Add(("Thunder Shock", "Level Up", 1));
                    list.Add(("Spark", "Level Up", 14));
                    list.Add(("Thunderbolt", "Level Up", 32));
                    list.Add(("Thunder", "Level Up", 45));
                    list.Add(("Volt Switch", "TM / TR", null));
                    list.Add(("Thunder Wave", "TM / TR", null));
                    break;
                case "psychic":
                    list.Add(("Confusion", "Level Up", 1));
                    list.Add(("Psybeam", "Level Up", 14));
                    list.Add(("Psychic", "Level Up", 32));
                    list.Add(("Psyshock", "TM / TR", null));
                    list.Add(("Calm Mind", "TM / TR", null));
                    list.Add(("Shadow Ball", "TM / TR", null));
                    break;
                case "ice":
                    list.Add(("Ice Shard", "Level Up", 1));
                    list.Add(("Icy Wind", "Level Up", 14));
                    list.Add(("Ice Beam", "Level Up", 32));
                    list.Add(("Blizzard", "Level Up", 45));
                    list.Add(("Ice Punch", "Tutor", null));
                    break;
                case "fighting":
                    list.Add(("Mach Punch", "Level Up", 1));
                    list.Add(("Brick Break", "Level Up", 20));
                    list.Add(("Close Combat", "Level Up", 45));
                    list.Add(("Drain Punch", "TM / TR", null));
                    list.Add(("Swords Dance", "TM / TR", null));
                    break;
                case "dragon":
                    list.Add(("Dragon Breath", "Level Up", 15));
                    list.Add(("Dragon Claw", "Level Up", 32));
                    list.Add(("Dragon Pulse", "Level Up", 40));
                    list.Add(("Outrage", "Level Up", 50));
                    list.Add(("Dragon Dance", "TM / TR", null));
                    list.Add(("Draco Meteor", "Tutor", null));
                    break;
                case "dark":
                    list.Add(("Bite", "Level Up", 1));
                    list.Add(("Crunch", "Level Up", 28));
                    list.Add(("Dark Pulse", "Level Up", 36));
                    list.Add(("Foul Play", "Tutor", null));
                    list.Add(("Nasty Plot", "TM / TR", null));
                    break;
                case "steel":
                    list.Add(("Metal Claw", "Level Up", 1));
                    list.Add(("Iron Head", "Level Up", 32));
                    list.Add(("Flash Cannon", "Level Up", 38));
                    list.Add(("Bullet Punch", "Egg Move", null));
                    break;
                case "ground":
                    list.Add(("Mud-Slap", "Level Up", 1));
                    list.Add(("Dig", "Level Up", 22));
                    list.Add(("Earthquake", "Level Up", 38));
                    list.Add(("Earth Power", "TM / TR", null));
                    list.Add(("Stealth Rock", "TM / TR", null));
                    break;
                case "rock":
                    list.Add(("Rock Throw", "Level Up", 1));
                    list.Add(("Rock Slide", "Level Up", 25));
                    list.Add(("Stone Edge", "Level Up", 40));
                    list.Add(("Stealth Rock", "TM / TR", null));
                    break;
                case "ghost":
                    list.Add(("Shadow Sneak", "Level Up", 1));
                    list.Add(("Shadow Ball", "Level Up", 30));
                    list.Add(("Hex", "Level Up", 20));
                    list.Add(("Destiny Bond", "Level Up", 45));
                    list.Add(("Confuse Ray", "Level Up", 12));
                    break;
                case "flying":
                    list.Add(("Gust", "Level Up", 1));
                    list.Add(("Wing Attack", "Level Up", 15));
                    list.Add(("Air Slash", "Level Up", 30));
                    list.Add(("Roost", "TM / TR", null));
                    list.Add(("Brave Bird", "Level Up", 48));
                    break;
                case "fairy":
                    list.Add(("Draining Kiss", "Level Up", 1));
                    list.Add(("Dazzling Gleam", "Level Up", 28));
                    list.Add(("Moonblast", "Level Up", 42));
                    list.Add(("Play Rough", "TM / TR", null));
                    break;
            }
        }

        return list;
    }

    private static List<string> InferSpeciesTypes(int speciesId, string name)
    {
        var lower = name.ToLowerInvariant();
        if (lower.Contains("fire") || lower.Contains("flame") || lower.Contains("char") || lower.Contains("vulpix") || lower.Contains("flare") || lower.Contains("cyndaquil") || lower.Contains("torchic") || lower.Contains("chimchar") || lower.Contains("tepig") || lower.Contains("fennekin") || lower.Contains("litten") || lower.Contains("scorbunny") || lower.Contains("fuecoco"))
            return ["Fire"];
        if (lower.Contains("water") || lower.Contains("aqua") || lower.Contains("squirtle") || lower.Contains("poliwag") || lower.Contains("totodile") || lower.Contains("mudkip") || lower.Contains("piplup") || lower.Contains("oshawott") || lower.Contains("froakie") || lower.Contains("popplio") || lower.Contains("sobble") || lower.Contains("quaxly") || lower.Contains("magikarp") || lower.Contains("vaporeon"))
            return ["Water"];
        if (lower.Contains("grass") || lower.Contains("leaf") || lower.Contains("bulbasaur") || lower.Contains("chikorita") || lower.Contains("treecko") || lower.Contains("turtwig") || lower.Contains("snivy") || lower.Contains("chespin") || lower.Contains("rowlet") || lower.Contains("grookey") || lower.Contains("sprigatito") || lower.Contains("leafeon"))
            return ["Grass"];
        if (lower.Contains("thunder") || lower.Contains("electr") || lower.Contains("pika") || lower.Contains("jolteon") || lower.Contains("zapdos") || lower.Contains("raikou") || lower.Contains("luxray") || lower.Contains("zekrom") || lower.Contains("miraidon"))
            return ["Electric"];
        if (lower.Contains("psych") || lower.Contains("mew") || lower.Contains("abra") || lower.Contains("espeon") || lower.Contains("celebi") || lower.Contains("deoxys") || lower.Contains("gardevoir") || lower.Contains("cresselia"))
            return ["Psychic"];
        if (lower.Contains("dragon") || lower.Contains("draco") || lower.Contains("dratini") || lower.Contains("bagon") || lower.Contains("gible") || lower.Contains("axew") || lower.Contains("deino") || lower.Contains("goomy") || lower.Contains("dreepy"))
            return ["Dragon"];
        if (lower.Contains("ghost") || lower.Contains("ghastly") || lower.Contains("haunter") || lower.Contains("misdreavus") || lower.Contains("duskull") || lower.Contains("litwick") || lower.Contains("mimikyu"))
            return ["Ghost"];

        return ["Normal"];
    }
}

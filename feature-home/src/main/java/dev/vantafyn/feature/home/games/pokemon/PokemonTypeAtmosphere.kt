package dev.vantafyn.feature.home.games.pokemon

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import kotlin.math.sin

/**
 * 18 Canonical Pokémon Types with distinct atmospheric palettes and aura glows.
 */
enum class PokemonType(
    val id: Int,
    val displayName: String,
    val accentColor: Color,
    val secondaryAccent: Color,
    val bgGradientStart: Color,
    val bgGradientMid: Color,
    val bgGradientEnd: Color,
    val glowColor: Color,
) {
    Normal(
        id = 1,
        displayName = "Normal",
        accentColor = Color(0xFFA8A878),
        secondaryAccent = Color(0xFFC6C6A7),
        bgGradientStart = Color(0xFF1E212D),
        bgGradientMid = Color(0xFF161822),
        bgGradientEnd = Color(0xFF10121A),
        glowColor = Color(0xFFD4D4B8),
    ),
    Fire(
        id = 2,
        displayName = "Fire",
        accentColor = Color(0xFFF97316),
        secondaryAccent = Color(0xFFEF4444),
        bgGradientStart = Color(0xFF2E1210),
        bgGradientMid = Color(0xFF1D0C0E),
        bgGradientEnd = Color(0xFF110809),
        glowColor = Color(0xFFFF5722),
    ),
    Water(
        id = 3,
        displayName = "Water",
        accentColor = Color(0xFF38BDF8),
        secondaryAccent = Color(0xFF2563EB),
        bgGradientStart = Color(0xFF0F2038),
        bgGradientMid = Color(0xFF0A1526),
        bgGradientEnd = Color(0xFF060D18),
        glowColor = Color(0xFF0284C7),
    ),
    Grass(
        id = 4,
        displayName = "Grass",
        accentColor = Color(0xFF22C55E),
        secondaryAccent = Color(0xFF15803D),
        bgGradientStart = Color(0xFF102818),
        bgGradientMid = Color(0xFF0B1B10),
        bgGradientEnd = Color(0xFF07110A),
        glowColor = Color(0xFF16A34A),
    ),
    Electric(
        id = 5,
        displayName = "Electric",
        accentColor = Color(0xFFFACC15),
        secondaryAccent = Color(0xFFEAB308),
        bgGradientStart = Color(0xFF2A230B),
        bgGradientMid = Color(0xFF1B1607),
        bgGradientEnd = Color(0xFF100E05),
        glowColor = Color(0xFFFDE047),
    ),
    Ice(
        id = 6,
        displayName = "Ice",
        accentColor = Color(0xFF67E8F9),
        secondaryAccent = Color(0xFF06B6D4),
        bgGradientStart = Color(0xFF102735),
        bgGradientMid = Color(0xFF0B1923),
        bgGradientEnd = Color(0xFF061016),
        glowColor = Color(0xFF22D3EE),
    ),
    Fighting(
        id = 7,
        displayName = "Fighting",
        accentColor = Color(0xFFF87171),
        secondaryAccent = Color(0xFFB91C1C),
        bgGradientStart = Color(0xFF2B1412),
        bgGradientMid = Color(0xFF1B0C0B),
        bgGradientEnd = Color(0xFF100706),
        glowColor = Color(0xFFDC2626),
    ),
    Poison(
        id = 8,
        displayName = "Poison",
        accentColor = Color(0xFFA855F7),
        secondaryAccent = Color(0xFF7E22CE),
        bgGradientStart = Color(0xFF23102E),
        bgGradientMid = Color(0xFF160A1D),
        bgGradientEnd = Color(0xFF0D0612),
        glowColor = Color(0xFF9333EA),
    ),
    Ground(
        id = 9,
        displayName = "Ground",
        accentColor = Color(0xFFF59E0B),
        secondaryAccent = Color(0xFFD97706),
        bgGradientStart = Color(0xFF281E10),
        bgGradientMid = Color(0xFF1A130A),
        bgGradientEnd = Color(0xFF100B06),
        glowColor = Color(0xFFB45309),
    ),
    Flying(
        id = 10,
        displayName = "Flying",
        accentColor = Color(0xFF818CF8),
        secondaryAccent = Color(0xFF6366F1),
        bgGradientStart = Color(0xFF171B38),
        bgGradientMid = Color(0xFF0F1226),
        bgGradientEnd = Color(0xFF090B18),
        glowColor = Color(0xFF4F46E5),
    ),
    Psychic(
        id = 11,
        displayName = "Psychic",
        accentColor = Color(0xFFF43F5E),
        secondaryAccent = Color(0xFFE11D48),
        bgGradientStart = Color(0xFF2C101F),
        bgGradientMid = Color(0xFF1C0A14),
        bgGradientEnd = Color(0xFF11060C),
        glowColor = Color(0xFFFB7185),
    ),
    Bug(
        id = 12,
        displayName = "Bug",
        accentColor = Color(0xFF84CC16),
        secondaryAccent = Color(0xFF65A30D),
        bgGradientStart = Color(0xFF17240E),
        bgGradientMid = Color(0xFF0E1709),
        bgGradientEnd = Color(0xFF080F05),
        glowColor = Color(0xFF4D7C0F),
    ),
    Rock(
        id = 13,
        displayName = "Rock",
        accentColor = Color(0xFFD97706),
        secondaryAccent = Color(0xFFB45309),
        bgGradientStart = Color(0xFF261D15),
        bgGradientMid = Color(0xFF18120D),
        bgGradientEnd = Color(0xFF0E0B08),
        glowColor = Color(0xFF92400E),
    ),
    Ghost(
        id = 14,
        displayName = "Ghost",
        accentColor = Color(0xFF8B5CF6),
        secondaryAccent = Color(0xFF6D28D9),
        bgGradientStart = Color(0xFF1D1432),
        bgGradientMid = Color(0xFF120C20),
        bgGradientEnd = Color(0xFF0B0714),
        glowColor = Color(0xFF7C3AED),
    ),
    Dragon(
        id = 15,
        displayName = "Dragon",
        accentColor = Color(0xFF6366F1),
        secondaryAccent = Color(0xFF4338CA),
        bgGradientStart = Color(0xFF1B163B),
        bgGradientMid = Color(0xFF110E26),
        bgGradientEnd = Color(0xFF0A0818),
        glowColor = Color(0xFF4F46E5),
    ),
    Dark(
        id = 16,
        displayName = "Dark",
        accentColor = Color(0xFF64748B),
        secondaryAccent = Color(0xFF475569),
        bgGradientStart = Color(0xFF161822),
        bgGradientMid = Color(0xFF0E1017),
        bgGradientEnd = Color(0xFF08090E),
        glowColor = Color(0xFF334155),
    ),
    Steel(
        id = 17,
        displayName = "Steel",
        accentColor = Color(0xFF94A3B8),
        secondaryAccent = Color(0xFF64748B),
        bgGradientStart = Color(0xFF1A1F29),
        bgGradientMid = Color(0xFF11141B),
        bgGradientEnd = Color(0xFF0A0C11),
        glowColor = Color(0xFFCBD5E1),
    ),
    Fairy(
        id = 18,
        displayName = "Fairy",
        accentColor = Color(0xFFF472B6),
        secondaryAccent = Color(0xFFEC4899),
        bgGradientStart = Color(0xFF2C1425),
        bgGradientMid = Color(0xFF1C0D18),
        bgGradientEnd = Color(0xFF10070E),
        glowColor = Color(0xFFF9A8D4),
    );

    companion object {
        fun fromId(id: Int): PokemonType = entries.firstOrNull { it.id == id } ?: Normal
        fun fromName(name: String): PokemonType = entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Normal
    }
}

/**
 * High-performance National Dex Types Registry mapping species IDs 1..1025.
 */
object PokemonTypeCatalog {
    // 2050 bytes encoded in Base64: 2 bytes per Pokémon (PrimaryType, SecondaryType)
    private const val TYPE_DATA_BASE64 =
        "BAgECAQIAgACAAIKAwADAAMADAAMAAwKDAgMCAwIAQoBCgEKAQABAAEKAQoIAAgABQAFAAkACQAIAAgACAkIAAgACAkSABIAAgACAAESARIICggKBAgECAQIDAQMBAwIDAgJAAkAAQABAAMAAwAHAAcAAgACAAMAAwADBwsACwALAAcABwAHAAQIBAgECAMIAwgNCQ0JDQkCAAIAAwsDCwURBREBCgEKAQoDAAMGCAAIAAMAAwYOCA4IDggNCQsACwADAAMABQAFAAQLBAsJAAkABwAHAAEACAAIAAkNCQ0BAAQAAQADAAMAAwADAAMAAwsLEgwKBgsFAAIADAABAAMAAwoDBgEAAQADAAUAAgABAA0DDQMNAw0DDQoBAAYKBQoCCg8ADwAPCgsACwAEAAQABAACAAIAAgADAAMAAwABAAEAAQoBCgwKDAoMCAwICAoDBQMFBQASAAESEgASCgsKCwoFAAUABQAEAAMSAxINAAMABAoECgQKAQAEAAQADAoDCQMJCwAQABAKAwsOAAsACwABCwwADBEBAAkKEQkSABIAAwgMEQwNDAcQBgEAAQACAAINBgkGCQMNAwADAAYKAwoRChACEAIDDwkACQABAAEAAQAHAAcABgsFAAIAAQABAAUAAgADAA0JDQkNEAsKAgoLBAQABAAEAAIAAgcCBwMAAwkDCRAAEAABAAEADAAMAAwKDAAMCAMEAwQDBAQABBAEEAEKAQoDCgMKCxILEgsSDAMMCgQABAcBAAEAAQAMCQwKDA4BAAEAAQAHAAcAARINAAEAAQAQDhESEQ0RDRENBwsHCwUABQAFAAUADAAMAAQICAAIAAMQAxADAAMAAgkCCQIACwALAAEACQAJDwkPBAAEEAEKDwoBAAgADQsNCwMJAwkDAAMQCQsJCw0EDQQNDA0MAwADAAEAAQAOAA4ADgAOAAQKCwAQAAsABgAGAAYDBgMGAwMAAwADAAMNAwAPAA8ADwoRCxELEQsNAAYAEQAPCw8LAwAJAA8KEQsLAAQABAAECQIAAgcCBwMAAwADEQEKAQoBCgEAAQMMAAwABQAFAAUABAgECA0ADQANEQ0RDAAMBAwKDAoMCgUAAwADAAQABAADAAMJAQAOCg4KAQABAA4AEAoBAAEACwAIEAgQEQsRCw0ACxIBAAEKDhAPCQ8JDwkBAAcABxEJAAkACAwIEAgHCAcEAAMAAwADCgQGBAYQBgURAQAJDQQABQACABIKDAoEAAYACQoGCQEACwcNEQ4ABg4FDgsACwALABEPAw8CEQEADg8LAAMAAwAQAAQAAQALAgQABAAEAAIAAgcCBwMAAwADAAEAAQABAAEAAQAQABAABAAEAAIAAgADAAMACwALAAEKAQoBCgUABQANAA0ADQALCgsKCQAJEQEABwAHAAcAAwADCQMJBwAHAAwEDAQMBAwIDAgMCAQSBBIEAAQAAwAJEAkQCRACAAIABAAMDQwNEAcQBwsKDgAOAAMNAw0NCg0KCAAIABAAEAABAAEACwALAAsACwALAAsAAwoDCgYABgAGAAEEAQQFCgwADBEECAQIAw4DDgMADAUMBQQRBBERABEAEQAFAAUABQALAAsADgIOAg4CDwAPAA8ABgAGAAYADAAMAAkFBwAHAA8ACQ4JDhAREBEBAAEKAQoQChAKAgAMERAPEA8QDwwCDAIRBw0HBAcKAAUKDwIPBQkKDwYDBwELDBEEAAQABAcCAAIAAgsDAAMAAxABAAEJAQoCCgIKDAAMAAwKAgECARIAEgASAAQABAAHAAcQAQALAAsAEQ4RDhEOEgASABIAEgAQCxALDQMNAwgDCA8DAAMABQEFAQ0PDQ8NBg0GEgAHCgUSDRIPAA8ADwAREg4EDgQOBA4EBgAGAAoPCg8SABAKDwkNEgsOAgMECgQKBA4CAAIAAhADAAMAAxIBCgEKAQoBAAEADAAMBQwFBwAHBgIKDBIMEg0ADQADAAgDCAMJAAkAAwwDDAQABAAEEgQSCAIIAgEHAQcEAAQABAASAAELBwAMAwwDDgkOCQMAAQABAA0KAQACDwURDhIDCwEPDgQPAA8HDwcFEgsSBBIDEgsACwALEQsODQgMBwwHBQARCgQREA8LABESBw4IAAgPDRECDgUAEQARAAQABAAEAAIAAgACAAMAAwADAAEAAQAKAAoAChEMAAwLDAsQABAABAAEAAEAAQADAAMNBQAFAA0ADQINAgQPBA8EDwkACQAKAwMAAwAFCAUIAgwCDAcABwAOAA4ACwALAAsSEBIQEhASEAERAA4ABwAGCwkOEgASAAcABQAGDAYMDQAGAAsBBRARABEABQ8FBgMPAwYRDw8ODw4PDhIABwAIDwcABxAQBAUADwAGAA4ACwQBCwwNCQEDDgcIEAgSCgQABAAEEAIAAgACDgMAAwADBwEAAQAMAAwADAAMEAUABQcFBwEAAQASABIABAEEAQQBAQoNAA0ADQACAAILAg4FAAUABQoFChAAEAAIAQgBBA4EDgkECQQNAAQABAIMAAwLCwALABIREhESEQMAAwAKEAMAAwARCBEIDwERAA0IDQgOAA4ACgcGAAYAAwsDAA8DBw4ICQELAQAQEQkHEgsEEA4SDAcFCQkRBgMHBRAKAggNBQ8GDwYPBg4AEQ4QBBAGEAkQAg8QEgcHDwUPAw8ECwQPBA4EDggHCAsIEgQAEQ8EDwIPBQ8NCxELAQAIDg=="

    private val TYPE_DATA: ByteArray by lazy {
        android.util.Base64.decode(TYPE_DATA_BASE64, android.util.Base64.DEFAULT)
    }

    fun getTypes(
        speciesId: Int,
        speciesName: String = "",
        formKey: String? = null,
        formName: String? = null,
    ): Pair<PokemonType, PokemonType?> {
        resolveFormOverride(speciesId, formKey, formName)?.let { return it }
        if (speciesId in 1..1025) {
            val idx = (speciesId - 1) * 2
            if (idx + 1 < TYPE_DATA.size) {
                val t1 = PokemonType.fromId(TYPE_DATA[idx].toInt())
                val t2Id = TYPE_DATA[idx + 1].toInt()
                val t2 = if (t2Id > 0 && t2Id != t1.id) PokemonType.fromId(t2Id) else null
                return t1 to t2
            }
        }
        return fallbackBySpeciesName(speciesName)
    }

    private fun resolveFormOverride(speciesId: Int, formKey: String?, formName: String?): Pair<PokemonType, PokemonType?>? {
        val key = formKey?.trim()?.lowercase()
        if (!key.isNullOrBlank()) {
            FORM_TYPE_OVERRIDES[key]?.let { return it }
        }
        val name = formName?.trim()?.lowercase().orEmpty()
        if (name.isBlank()) return null
        if ("alolan" in name || "alola" in name) {
            ALOLAN_TYPE_OVERRIDES[speciesId]?.let { return it }
        }
        if (speciesId == 555 && ("zen" in name) && ("galarian" in name || "galar" in name)) {
            return PokemonType.Ice to PokemonType.Fire
        }
        if ("galarian" in name || "galar" in name) {
            GALARIAN_TYPE_OVERRIDES[speciesId]?.let { return it }
        }
        if ("paldean" in name || "paldea" in name) {
            if ("blaze" in name) return PokemonType.Fighting to PokemonType.Fire
            if ("aqua" in name) return PokemonType.Fighting to PokemonType.Water
            PALDEAN_TYPE_OVERRIDES[speciesId]?.let { return it }
        }
        if ("hisuian" in name || "hisui" in name) {
            HISUIAN_TYPE_OVERRIDES[speciesId]?.let { return it }
        }
        return when {
            name.contains("alolan rattata") || name.contains("rattata-alola") -> PokemonType.Dark to PokemonType.Normal
            name.contains("alolan raticate") || name.contains("raticate-alola") -> PokemonType.Dark to PokemonType.Normal
            name.contains("alolan raichu") || name.contains("raichu-alola") -> PokemonType.Electric to PokemonType.Psychic
            name.contains("alolan sandshrew") || name.contains("sandshrew-alola") -> PokemonType.Ice to PokemonType.Steel
            name.contains("alolan sandslash") || name.contains("sandslash-alola") -> PokemonType.Ice to PokemonType.Steel
            name.contains("alolan vulpix") || name.contains("vulpix-alola") -> PokemonType.Ice to null
            name.contains("alolan ninetales") || name.contains("ninetales-alola") -> PokemonType.Ice to PokemonType.Fairy
            name.contains("alolan diglett") || name.contains("diglett-alola") -> PokemonType.Ground to PokemonType.Steel
            name.contains("alolan dugtrio") || name.contains("dugtrio-alola") -> PokemonType.Ground to PokemonType.Steel
            name.contains("alolan meowth") || name.contains("meowth-alola") -> PokemonType.Dark to null
            name.contains("alolan persian") || name.contains("persian-alola") -> PokemonType.Dark to null
            name.contains("alolan geodude") || name.contains("geodude-alola") -> PokemonType.Rock to PokemonType.Electric
            name.contains("alolan graveler") || name.contains("graveler-alola") -> PokemonType.Rock to PokemonType.Electric
            name.contains("alolan golem") || name.contains("golem-alola") -> PokemonType.Rock to PokemonType.Electric
            name.contains("alolan grimer") || name.contains("grimer-alola") -> PokemonType.Poison to PokemonType.Dark
            name.contains("alolan muk") || name.contains("muk-alola") -> PokemonType.Poison to PokemonType.Dark
            name.contains("alolan exeggutor") || name.contains("exeggutor-alola") -> PokemonType.Grass to PokemonType.Dragon
            name.contains("alolan marowak") || name.contains("marowak-alola") -> PokemonType.Fire to PokemonType.Ghost
            name.contains("galarian meowth") || name.contains("meowth-galar") -> PokemonType.Steel to null
            name.contains("galarian ponyta") || name.contains("ponyta-galar") -> PokemonType.Psychic to null
            name.contains("galarian rapidash") || name.contains("rapidash-galar") -> PokemonType.Psychic to PokemonType.Fairy
            name.contains("galarian slowpoke") || name.contains("slowpoke-galar") -> PokemonType.Psychic to null
            name.contains("galarian slowbro") || name.contains("slowbro-galar") -> PokemonType.Poison to PokemonType.Psychic
            name.contains("galarian farfetch") || name.contains("farfetchd-galar") -> PokemonType.Fighting to null
            name.contains("galarian weezing") || name.contains("weezing-galar") -> PokemonType.Poison to PokemonType.Fairy
            name.contains("galarian mr mime") || name.contains("mr-mime-galar") -> PokemonType.Ice to PokemonType.Psychic
            name.contains("galarian articuno") || name.contains("articuno-galar") -> PokemonType.Psychic to PokemonType.Flying
            name.contains("galarian zapdos") || name.contains("zapdos-galar") -> PokemonType.Fighting to PokemonType.Flying
            name.contains("galarian moltres") || name.contains("moltres-galar") -> PokemonType.Dark to PokemonType.Flying
            name.contains("galarian slowking") || name.contains("slowking-galar") -> PokemonType.Poison to PokemonType.Psychic
            name.contains("galarian corsola") || name.contains("corsola-galar") -> PokemonType.Ghost to null
            name.contains("galarian zigzagoon") || name.contains("zigzagoon-galar") -> PokemonType.Dark to PokemonType.Normal
            name.contains("galarian linoone") || name.contains("linoone-galar") -> PokemonType.Dark to PokemonType.Normal
            name.contains("galarian darumaka") || name.contains("darumaka-galar") -> PokemonType.Ice to null
            name.contains("galarian darmanitan zen") || name.contains("darmanitan-galar-zen") -> PokemonType.Ice to PokemonType.Fire
            name.contains("galarian darmanitan") || name.contains("darmanitan-galar") -> PokemonType.Ice to null
            name.contains("galarian yamask") || name.contains("yamask-galar") -> PokemonType.Ground to PokemonType.Ghost
            name.contains("galarian stunfisk") || name.contains("stunfisk-galar") -> PokemonType.Ground to PokemonType.Steel
            name.contains("paldean tauros blaze") || name.contains("tauros-paldea-blaze") -> PokemonType.Fighting to PokemonType.Fire
            name.contains("paldean tauros aqua") || name.contains("tauros-paldea-aqua") -> PokemonType.Fighting to PokemonType.Water
            name.contains("paldean tauros") || name.contains("tauros-paldea") -> PokemonType.Fighting to null
            name.contains("paldean wooper") || name.contains("wooper-paldea") -> PokemonType.Poison to PokemonType.Ground
            name.contains("hisuian zoroark") || name.contains("zoroark-hisui") -> PokemonType.Normal to PokemonType.Ghost
            name.contains("hisuian zorua") || name.contains("zorua-hisui") -> PokemonType.Normal to PokemonType.Ghost
            else -> null
        }
    }

    private val ALOLAN_TYPE_OVERRIDES: Map<Int, Pair<PokemonType, PokemonType?>> = mapOf(
        19 to (PokemonType.Dark to PokemonType.Normal),
        20 to (PokemonType.Dark to PokemonType.Normal),
        26 to (PokemonType.Electric to PokemonType.Psychic),
        27 to (PokemonType.Ice to PokemonType.Steel),
        28 to (PokemonType.Ice to PokemonType.Steel),
        37 to (PokemonType.Ice to null),
        38 to (PokemonType.Ice to PokemonType.Fairy),
        50 to (PokemonType.Ground to PokemonType.Steel),
        51 to (PokemonType.Ground to PokemonType.Steel),
        52 to (PokemonType.Dark to null),
        53 to (PokemonType.Dark to null),
        74 to (PokemonType.Rock to PokemonType.Electric),
        75 to (PokemonType.Rock to PokemonType.Electric),
        76 to (PokemonType.Rock to PokemonType.Electric),
        88 to (PokemonType.Poison to PokemonType.Dark),
        89 to (PokemonType.Poison to PokemonType.Dark),
        103 to (PokemonType.Grass to PokemonType.Dragon),
        105 to (PokemonType.Fire to PokemonType.Ghost),
    )

    private val GALARIAN_TYPE_OVERRIDES: Map<Int, Pair<PokemonType, PokemonType?>> = mapOf(
        52 to (PokemonType.Steel to null),
        77 to (PokemonType.Psychic to null),
        78 to (PokemonType.Psychic to PokemonType.Fairy),
        79 to (PokemonType.Psychic to null),
        80 to (PokemonType.Poison to PokemonType.Psychic),
        83 to (PokemonType.Fighting to null),
        110 to (PokemonType.Poison to PokemonType.Fairy),
        122 to (PokemonType.Ice to PokemonType.Psychic),
        144 to (PokemonType.Psychic to PokemonType.Flying),
        145 to (PokemonType.Fighting to PokemonType.Flying),
        146 to (PokemonType.Dark to PokemonType.Flying),
        199 to (PokemonType.Poison to PokemonType.Psychic),
        222 to (PokemonType.Ghost to null),
        263 to (PokemonType.Dark to PokemonType.Normal),
        264 to (PokemonType.Dark to PokemonType.Normal),
        554 to (PokemonType.Ice to null),
        555 to (PokemonType.Ice to null),
        562 to (PokemonType.Ground to PokemonType.Ghost),
        618 to (PokemonType.Ground to PokemonType.Steel),
    )

    private val PALDEAN_TYPE_OVERRIDES: Map<Int, Pair<PokemonType, PokemonType?>> = mapOf(
        128 to (PokemonType.Fighting to null),
        194 to (PokemonType.Poison to PokemonType.Ground),
    )

    private val HISUIAN_TYPE_OVERRIDES: Map<Int, Pair<PokemonType, PokemonType?>> = mapOf(
        58 to (PokemonType.Fire to PokemonType.Rock),
        59 to (PokemonType.Fire to PokemonType.Rock),
        100 to (PokemonType.Electric to PokemonType.Grass),
        101 to (PokemonType.Electric to PokemonType.Grass),
        157 to (PokemonType.Fire to PokemonType.Ghost),
        211 to (PokemonType.Dark to PokemonType.Poison),
        215 to (PokemonType.Poison to PokemonType.Fighting),
        503 to (PokemonType.Water to PokemonType.Dark),
        549 to (PokemonType.Grass to PokemonType.Fighting),
        570 to (PokemonType.Normal to PokemonType.Ghost),
        571 to (PokemonType.Normal to PokemonType.Ghost),
        628 to (PokemonType.Psychic to PokemonType.Flying),
        705 to (PokemonType.Steel to PokemonType.Dragon),
        706 to (PokemonType.Steel to PokemonType.Dragon),
        713 to (PokemonType.Ice to PokemonType.Rock),
        724 to (PokemonType.Grass to PokemonType.Fighting),
    )

    private val FORM_TYPE_OVERRIDES: Map<String, Pair<PokemonType, PokemonType?>> = mapOf(
        "10091" to (PokemonType.Dark to PokemonType.Normal),
        "10092" to (PokemonType.Dark to PokemonType.Normal),
        "10100" to (PokemonType.Electric to PokemonType.Psychic),
        "10101" to (PokemonType.Ice to PokemonType.Steel),
        "10102" to (PokemonType.Ice to PokemonType.Steel),
        "10103" to (PokemonType.Ice to null),
        "10104" to (PokemonType.Ice to PokemonType.Fairy),
        "10105" to (PokemonType.Ground to PokemonType.Steel),
        "10106" to (PokemonType.Ground to PokemonType.Steel),
        "10107" to (PokemonType.Dark to null),
        "10108" to (PokemonType.Dark to null),
        "10109" to (PokemonType.Rock to PokemonType.Electric),
        "10110" to (PokemonType.Rock to PokemonType.Electric),
        "10111" to (PokemonType.Rock to PokemonType.Electric),
        "10112" to (PokemonType.Poison to PokemonType.Dark),
        "10113" to (PokemonType.Poison to PokemonType.Dark),
        "10114" to (PokemonType.Grass to PokemonType.Dragon),
        "10115" to (PokemonType.Fire to PokemonType.Ghost),
        "10161" to (PokemonType.Steel to null),
        "10162" to (PokemonType.Psychic to null),
        "10163" to (PokemonType.Psychic to PokemonType.Fairy),
        "10164" to (PokemonType.Psychic to null),
        "10165" to (PokemonType.Poison to PokemonType.Psychic),
        "10166" to (PokemonType.Fighting to null),
        "10167" to (PokemonType.Poison to PokemonType.Fairy),
        "10168" to (PokemonType.Ice to PokemonType.Psychic),
        "10169" to (PokemonType.Psychic to PokemonType.Flying),
        "10170" to (PokemonType.Fighting to PokemonType.Flying),
        "10171" to (PokemonType.Dark to PokemonType.Flying),
        "10172" to (PokemonType.Poison to PokemonType.Psychic),
        "10173" to (PokemonType.Ghost to null),
        "10174" to (PokemonType.Dark to PokemonType.Normal),
        "10175" to (PokemonType.Dark to PokemonType.Normal),
        "10176" to (PokemonType.Ice to null),
        "10177" to (PokemonType.Ice to null),
        "10178" to (PokemonType.Ice to PokemonType.Fire),
        "10179" to (PokemonType.Ground to PokemonType.Ghost),
        "10180" to (PokemonType.Ground to PokemonType.Steel),
        "10250" to (PokemonType.Fighting to null),
        "10251" to (PokemonType.Fighting to PokemonType.Fire),
        "10252" to (PokemonType.Fighting to PokemonType.Water),
        "10253" to (PokemonType.Poison to PokemonType.Ground),
        "10229" to (PokemonType.Fire to PokemonType.Rock),
        "10230" to (PokemonType.Fire to PokemonType.Rock),
        "10231" to (PokemonType.Electric to PokemonType.Grass),
        "10232" to (PokemonType.Electric to PokemonType.Grass),
        "10233" to (PokemonType.Fire to PokemonType.Ghost),
        "10234" to (PokemonType.Dark to PokemonType.Poison),
        "10235" to (PokemonType.Poison to PokemonType.Fighting),
        "10236" to (PokemonType.Water to PokemonType.Dark),
        "10237" to (PokemonType.Grass to PokemonType.Fighting),
        "10238" to (PokemonType.Normal to PokemonType.Ghost),
        "10239" to (PokemonType.Normal to PokemonType.Ghost),
        "10240" to (PokemonType.Psychic to PokemonType.Flying),
        "10241" to (PokemonType.Steel to PokemonType.Dragon),
        "10242" to (PokemonType.Steel to PokemonType.Dragon),
        "10243" to (PokemonType.Ice to PokemonType.Rock),
        "10244" to (PokemonType.Grass to PokemonType.Fighting),
        "10004" to (PokemonType.Bug to PokemonType.Ground),
        "10005" to (PokemonType.Bug to PokemonType.Steel),
        "10017" to (PokemonType.Water to PokemonType.Ground),
        "10025" to (PokemonType.Water to PokemonType.Ground),
    )

    private fun fallbackBySpeciesName(name: String): Pair<PokemonType, PokemonType?> {
        val lower = name.lowercase()
        return when {
            lower.contains("fire") || lower.contains("flame") || lower.contains("char") || lower.contains("cinder") -> PokemonType.Fire to null
            lower.contains("water") || lower.contains("aqua") || lower.contains("hydro") || lower.contains("blast") -> PokemonType.Water to null
            lower.contains("grass") || lower.contains("leaf") || lower.contains("flora") || lower.contains("saur") -> PokemonType.Grass to null
            lower.contains("thunder") || lower.contains("volt") || lower.contains("spark") || lower.contains("pika") -> PokemonType.Electric to null
            lower.contains("ice") || lower.contains("frost") || lower.contains("glac") || lower.contains("snow") -> PokemonType.Ice to null
            lower.contains("ghost") || lower.contains("shadow") || lower.contains("spect") || lower.contains("gengar") -> PokemonType.Ghost to null
            lower.contains("psych") || lower.contains("tele") || lower.contains("mew") -> PokemonType.Psychic to null
            lower.contains("dragon") || lower.contains("drake") -> PokemonType.Dragon to null
            lower.contains("fairy") || lower.contains("pixie") -> PokemonType.Fairy to null
            lower.contains("dark") || lower.contains("night") -> PokemonType.Dark to null
            lower.contains("steel") || lower.contains("iron") || lower.contains("metal") -> PokemonType.Steel to null
            lower.contains("ground") || lower.contains("earth") || lower.contains("sand") -> PokemonType.Ground to null
            lower.contains("rock") || lower.contains("stone") -> PokemonType.Rock to null
            lower.contains("poison") || lower.contains("toxic") || lower.contains("venom") -> PokemonType.Poison to null
            lower.contains("fly") || lower.contains("bird") || lower.contains("wing") || lower.contains("air") -> PokemonType.Flying to null
            lower.contains("bug") || lower.contains("beetle") || lower.contains("cater") -> PokemonType.Bug to null
            lower.contains("fight") || lower.contains("champ") || lower.contains("karate") -> PokemonType.Fighting to null
            else -> PokemonType.Normal to null
        }
    }
}

/**
 * Elegant, cinematic type atmosphere canvas and scrim for the featured Pokémon Hero Card.
 */
@Composable
fun PokemonHeroTypeAtmosphere(
    primaryType: PokemonType,
    secondaryType: PokemonType?,
    verticalPresentation: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isReducedMotion = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0f
        }.getOrDefault(false)
    }

    val pulsePhase by if (isReducedMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "HeroAtmospherePulse")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28318f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "AtmospherePhase",
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Procedural Type Atmosphere & Ambient Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Base rich dark gradient for the primary type
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryType.bgGradientStart,
                        primaryType.bgGradientMid,
                        primaryType.bgGradientEnd,
                    ),
                    start = if (verticalPresentation) Offset(width / 2f, 0f) else Offset(0f, 0f),
                    end = if (verticalPresentation) Offset(width / 2f, height) else Offset(width, height),
                )
            )

            // Primary Type Atmospheric Signature
            drawTypeAtmosphere(
                primaryType = primaryType,
                secondaryType = secondaryType,
                width = width,
                height = height,
                pulsePhase = pulsePhase,
            )

            // Subtle Secondary Type Blend (top-right ambient mist)
            if (secondaryType != null && secondaryType != primaryType) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryType.accentColor.copy(alpha = 0.16f),
                            secondaryType.secondaryAccent.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        center = Offset(width * 0.88f, height * 0.20f),
                        radius = width * 0.48f,
                    ),
                    center = Offset(width * 0.88f, height * 0.20f),
                    radius = width * 0.48f,
                )
            }
        }

        // Layer 2: protect the artwork without flattening the full-card type atmosphere.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (verticalPresentation) {
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xFF06080E).copy(alpha = 0.10f),
                                0.38f to Color(0xFF06080E).copy(alpha = 0.18f),
                                0.70f to Color(0xFF06080E).copy(alpha = 0.50f),
                                1.00f to Color(0xFF06080E).copy(alpha = 0.78f),
                            ),
                        )
                    } else {
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xFF06080E).copy(alpha = 0.94f),
                                0.45f to Color(0xFF06080E).copy(alpha = 0.86f),
                                0.65f to Color(0xFF06080E).copy(alpha = 0.50f),
                                0.82f to Color(0xFF06080E).copy(alpha = 0.16f),
                                1.00f to Color(0xFF06080E).copy(alpha = 0.06f),
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY,
                        )
                    }
                )
        )

        // Layer 3: Subtle bottom grounding vignette for clean text anchoring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.60f to Color.Transparent,
                            1.00f to Color(0xFF04060A).copy(alpha = 0.50f),
                        )
                    )
                )
        )
    }
}

private fun DrawScope.drawTypeAtmosphere(
    primaryType: PokemonType,
    secondaryType: PokemonType?,
    width: Float,
    height: Float,
    pulsePhase: Float,
) {
    val artCenterX = width * 0.78f
    val artCenterY = height * 0.50f
    val pulseFactor = 1.0f + 0.05f * sin(pulsePhase)

    when (primaryType) {
        PokemonType.Fire -> {
            // Radiant ember heat haze
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF97316).copy(alpha = 0.28f * pulseFactor),
                        Color(0xFFDC2626).copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.54f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.54f,
            )
            // Floating subtle ember motes
            drawCircle(
                color = Color(0xFFFDE047).copy(alpha = 0.35f),
                radius = 2.5f,
                center = Offset(artCenterX - 45f + 10f * sin(pulsePhase), artCenterY - 35f),
            )
            drawCircle(
                color = Color(0xFFF97316).copy(alpha = 0.40f),
                radius = 3.2f,
                center = Offset(artCenterX + 40f, artCenterY - 45f - 8f * sin(pulsePhase + 1f)),
            )
            drawCircle(
                color = Color(0xFFEA580C).copy(alpha = 0.25f),
                radius = 2.0f,
                center = Offset(artCenterX - 20f, artCenterY + 40f),
            )
        }

        PokemonType.Water -> {
            // Flowing blue ripples and aquatic luminance
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.25f * pulseFactor),
                        Color(0xFF0284C7).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.56f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.56f,
            )
            // Soft ripple wave arcs
            val path = Path().apply {
                moveTo(artCenterX - 75f, artCenterY - 30f)
                cubicTo(
                    artCenterX - 20f, artCenterY - 50f + 6f * sin(pulsePhase),
                    artCenterX + 30f, artCenterY - 10f,
                    artCenterX + 85f, artCenterY - 25f,
                )
            }
            drawPath(
                path = path,
                color = Color(0xFF7DD3FC).copy(alpha = 0.18f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }

        PokemonType.Grass -> {
            // Leaf canopy & bioluminescent forest bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF22C55E).copy(alpha = 0.26f * pulseFactor),
                        Color(0xFF15803D).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.54f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.54f,
            )
            // Gentle bioluminescent motes
            drawCircle(
                color = Color(0xFF86EFAC).copy(alpha = 0.35f),
                radius = 3f,
                center = Offset(artCenterX - 35f, artCenterY - 40f + 6f * sin(pulsePhase)),
            )
            drawCircle(
                color = Color(0xFF4ADE80).copy(alpha = 0.30f),
                radius = 2.5f,
                center = Offset(artCenterX + 45f, artCenterY + 25f),
            )
        }

        PokemonType.Electric -> {
            // Atmospheric energy aura and ionized spark glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFACC15).copy(alpha = 0.26f * pulseFactor),
                        Color(0xFF854D0E).copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.52f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.52f,
            )
            // Faint lightning arc shimmer
            val path = Path().apply {
                moveTo(artCenterX + 20f, artCenterY - 60f)
                lineTo(artCenterX + 10f, artCenterY - 20f)
                lineTo(artCenterX + 35f, artCenterY - 15f)
                lineTo(artCenterX + 25f, artCenterY + 30f)
            }
            drawPath(
                path = path,
                color = Color(0xFFFEF08A).copy(alpha = 0.22f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
            )
        }

        PokemonType.Psychic -> {
            // Dreamy nebula mist & celestial aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF43F5E).copy(alpha = 0.26f * pulseFactor),
                        Color(0xFFBE185D).copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.55f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.55f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFEC4899).copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX - 40f, artCenterY + 20f),
                    radius = width * 0.36f,
                ),
                center = Offset(artCenterX - 40f, artCenterY + 20f),
                radius = width * 0.36f,
            )
        }

        PokemonType.Ghost -> {
            // Smoky purple spectral haze
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.28f * pulseFactor),
                        Color(0xFF5B21B6).copy(alpha = 0.15f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.56f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.56f,
            )
            // Ethereal ghostly vapor ribbon
            val path = Path().apply {
                moveTo(artCenterX - 65f, artCenterY + 40f)
                cubicTo(
                    artCenterX - 30f, artCenterY + 10f,
                    artCenterX - 10f, artCenterY - 30f,
                    artCenterX + 45f, artCenterY - 50f,
                )
            }
            drawPath(
                path = path,
                color = Color(0xFFA78BFA).copy(alpha = 0.18f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
            )
        }

        PokemonType.Ice -> {
            // Frosted shimmer & crystalline glacial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF67E8F9).copy(alpha = 0.25f * pulseFactor),
                        Color(0xFF0E7490).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.54f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.54f,
            )
            // Glacial diamond sparkles
            drawCircle(
                color = Color(0xFFE0F2FE).copy(alpha = 0.40f),
                radius = 2.5f,
                center = Offset(artCenterX - 40f, artCenterY - 25f),
            )
            drawCircle(
                color = Color(0xFFBAE6FD).copy(alpha = 0.35f),
                radius = 3.0f,
                center = Offset(artCenterX + 35f, artCenterY - 35f),
            )
        }

        PokemonType.Dragon -> {
            // Cosmic celestial storm energy
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = 0.28f * pulseFactor),
                        Color(0xFF4338CA).copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.58f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.58f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.15f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX + 30f, artCenterY - 20f),
                    radius = width * 0.34f,
                ),
                center = Offset(artCenterX + 30f, artCenterY - 20f),
                radius = width * 0.34f,
            )
        }

        PokemonType.Dark -> {
            // Shadowy low-contrast lunar noir
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF475569).copy(alpha = 0.24f * pulseFactor),
                        Color(0xFF1E293B).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.52f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.52f,
            )
        }

        PokemonType.Fairy -> {
            // Soft sparkle glow & enchanted twilight stardust
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF472B6).copy(alpha = 0.26f * pulseFactor),
                        Color(0xFFBE185D).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.54f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.54f,
            )
            // Fairy stardust sparkles
            drawCircle(
                color = Color(0xFFFDF2F8).copy(alpha = 0.45f),
                radius = 2.5f,
                center = Offset(artCenterX - 30f, artCenterY - 30f),
            )
            drawCircle(
                color = Color(0xFFFCE7F3).copy(alpha = 0.35f),
                radius = 2.0f,
                center = Offset(artCenterX + 35f, artCenterY + 20f),
            )
        }

        PokemonType.Normal -> {
            // Sophisticated brushed slate-taupe atmosphere with satin sweep
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA8A878).copy(alpha = 0.20f * pulseFactor),
                        Color(0xFF1E212D).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.52f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.52f,
            )
            // Subtle horizontal satin highlight bar
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE2E8F0).copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    startX = artCenterX - 60f,
                    endX = artCenterX + 60f,
                ),
                start = Offset(artCenterX - 60f, artCenterY + 15f),
                end = Offset(artCenterX + 60f, artCenterY + 15f),
                strokeWidth = 2.0f,
            )
        }

        PokemonType.Fighting -> {
            // Dynamic martial energy rings & crimson focus
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFEF4444).copy(alpha = 0.24f * pulseFactor),
                        Color(0xFF991B1B).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.54f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.54f,
            )
            // Faint diagonal dynamic strike arc
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFCA5A5).copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                ),
                start = Offset(artCenterX - 35f, artCenterY + 45f),
                end = Offset(artCenterX + 45f, artCenterY - 35f),
                strokeWidth = 2.2f,
            )
        }

        PokemonType.Poison -> {
            // Toxic miasma bioluminescence & subtle purple haze
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA855F7).copy(alpha = 0.25f * pulseFactor),
                        Color(0xFF6B21A8).copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.54f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.54f,
            )
            drawCircle(
                color = Color(0xFFC084FC).copy(alpha = 0.28f),
                radius = 3.0f,
                center = Offset(artCenterX - 30f, artCenterY + 25f + 4f * sin(pulsePhase)),
            )
        }

        PokemonType.Ground -> {
            // Warm desert earth glow & subtle strata
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.22f * pulseFactor),
                        Color(0xFFB45309).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.52f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.52f,
            )
            drawLine(
                color = Color(0xFFFDE68A).copy(alpha = 0.15f),
                start = Offset(artCenterX - 50f, artCenterY + 30f),
                end = Offset(artCenterX + 50f, artCenterY + 30f),
                strokeWidth = 1.5f,
            )
        }

        PokemonType.Rock -> {
            // Chiseled stone amber & mineral crystalline facets
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD97706).copy(alpha = 0.22f * pulseFactor),
                        Color(0xFF78350F).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.52f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.52f,
            )
            drawCircle(
                color = Color(0xFFFEF3C7).copy(alpha = 0.30f),
                radius = 2.0f,
                center = Offset(artCenterX + 30f, artCenterY - 20f),
            )
        }

        PokemonType.Bug -> {
            // Meadow vitality & emerald forest shimmer
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF84CC16).copy(alpha = 0.24f * pulseFactor),
                        Color(0xFF4D7C0F).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.52f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.52f,
            )
            drawCircle(
                color = Color(0xFFBEF264).copy(alpha = 0.32f),
                radius = 2.2f,
                center = Offset(artCenterX - 25f, artCenterY - 30f + 5f * sin(pulsePhase)),
            )
        }

        PokemonType.Steel -> {
            // Polished titanium sheen & metallic specular glint
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF94A3B8).copy(alpha = 0.22f * pulseFactor),
                        Color(0xFF334155).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.52f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.52f,
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFF8FAFC).copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                ),
                start = Offset(artCenterX - 40f, artCenterY + 20f),
                end = Offset(artCenterX + 40f, artCenterY - 20f),
                strokeWidth = 2.0f,
            )
        }

        PokemonType.Flying -> {
            // Aerodynamic wind currents & breezy sky sweep
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF818CF8).copy(alpha = 0.24f * pulseFactor),
                        Color(0xFF4338CA).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(artCenterX, artCenterY),
                    radius = width * 0.54f,
                ),
                center = Offset(artCenterX, artCenterY),
                radius = width * 0.54f,
            )
            val path = Path().apply {
                moveTo(artCenterX - 60f, artCenterY + 10f)
                cubicTo(
                    artCenterX - 20f, artCenterY - 20f,
                    artCenterX + 20f, artCenterY + 20f,
                    artCenterX + 60f, artCenterY - 10f,
                )
            }
            drawPath(
                path = path,
                color = Color(0xFFC7D2FE).copy(alpha = 0.20f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.0f)
            )
        }
    }
}

/**
 * Premium, polished type chip badge with subtle glass pill styling.
 */
@Composable
fun PokemonTypeBadge(
    type: PokemonType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(type.accentColor.copy(alpha = 0.16f))
            .border(
                width = 0.8.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        type.accentColor.copy(alpha = 0.55f),
                        type.secondaryAccent.copy(alpha = 0.30f),
                    )
                ),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(4.5.dp)
                .clip(CircleShape)
                .background(type.accentColor)
        )
        Text(
            text = type.displayName.uppercase(),
            color = Color(0xFFF1F5F9),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
    }
}

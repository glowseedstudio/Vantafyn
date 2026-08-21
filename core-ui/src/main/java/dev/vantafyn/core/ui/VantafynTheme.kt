package dev.vantafyn.core.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class VantafynThemePreset(
    val id: String,
    val label: String,
    val description: String,
) {
    Nebula("nebula", "Nebula", "The original Vantafyn look: cinematic blue, violet and glass."),
    Midnight("midnight", "Midnight", "Near-black graphite with restrained cool blue accents."),
    Aurora("aurora", "Aurora", "Deep teal and blue with soft cyan-green highlights."),
    Amethyst("amethyst", "Amethyst", "Indigo glass with refined violet illumination."),
    Ember("ember", "Ember", "Warm black surfaces with restrained amber and red accents."),
    Oled("oled", "OLED", "True-black surfaces with minimal premium glow."),
    ;

    companion object {
        val Default = Nebula

        fun fromId(id: String?): VantafynThemePreset =
            entries.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) } ?: Default
    }
}

@Immutable
data class VantafynThemeTokens(
    val preset: VantafynThemePreset,
    val graphite: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val ink: Color,
    val muted: Color,
    val primary: Color,
    val secondary: Color,
    val gold: Color,
    val destructive: Color,
    val border: Color,
    val accentColors: List<Color>,
    val backgroundGradient: List<Color>,
    val navDockColors: List<Color>,
    val navDockBorderColors: List<Color>,
    val glassNavyCore: Color,
    val glassNavyLift: Color,
    val glassIndigoLift: Color,
    val glassVioletLift: Color,
    val glassCyanSpecular: Color,
    val glassBlueSpecular: Color,
    val glassVioletSpecular: Color,
    val glassEdgeWhite: Color,
    val bottomScrimColors: List<Color>,
)

object VantafynThemeController {
    private const val PREFS_NAME = "vantafyn_app_preferences"
    const val KEY_THEME_ID = "app_theme_id"

    private var initialized = false
    private var renderedTokens by mutableStateOf<VantafynThemeTokens?>(null)
    var selectedPreset by mutableStateOf(VantafynThemePreset.Default)
        private set

    val tokens: VantafynThemeTokens
        get() = renderedTokens ?: tokensFor(selectedPreset)

    fun initialize(context: Context) {
        if (initialized) return
        selectedPreset = readTheme(context)
        initialized = true
    }

    fun selectTheme(context: Context, preset: VantafynThemePreset) {
        selectedPreset = preset
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_ID, preset.id)
            .apply()
    }

    fun readTheme(context: Context): VantafynThemePreset =
        VantafynThemePreset.fromId(
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_THEME_ID, null),
        )

    internal fun publishRenderedTokens(tokens: VantafynThemeTokens) {
        renderedTokens = tokens
    }
}

fun tokensFor(preset: VantafynThemePreset): VantafynThemeTokens =
    when (preset) {
        VantafynThemePreset.Nebula -> VantafynThemeTokens(
            preset = preset,
            graphite = Color(0xFF070A12),
            surface = Color(0xFF141822),
            surfaceHigh = Color(0xFF1B2030),
            ink = Color(0xFFF5F7FF),
            muted = Color(0xFFB8C0D8),
            primary = Color(0xFF8EA2FF),
            secondary = Color(0xFF62D6FF),
            gold = Color(0xFFFFC94A),
            destructive = Color(0xFFA96A72),
            border = Color.White.copy(alpha = 0.12f),
            accentColors = listOf(Color(0xFF31D7FF), Color(0xFF5B8CFF), Color(0xFF8B5CFF), Color(0xFFC05CFF)),
            backgroundGradient = listOf(Color(0xFF0E1320), Color(0xFF070A12), Color(0xFF060912)),
            navDockColors = listOf(Color(0xFF101525).copy(alpha = 0.91f), Color(0xFF141A2B).copy(alpha = 0.88f), Color(0xFF0B1020).copy(alpha = 0.93f)),
            navDockBorderColors = listOf(Color.White.copy(alpha = 0.16f), Color(0xFF5B8CFF).copy(alpha = 0.12f), Color(0xFF8B5CFF).copy(alpha = 0.10f), Color.White.copy(alpha = 0.08f)),
            glassNavyCore = Color(0xFF10182A),
            glassNavyLift = Color(0xFF17233B),
            glassIndigoLift = Color(0xFF252A4F),
            glassVioletLift = Color(0xFF332456),
            glassCyanSpecular = Color(0xFF67DCFF),
            glassBlueSpecular = Color(0xFF5B8CFF),
            glassVioletSpecular = Color(0xFF9B62FF),
            glassEdgeWhite = Color(0xFFF3F8FF),
            bottomScrimColors = listOf(Color.Transparent, Color(0xFF050812).copy(alpha = 0.28f), Color(0xFF050812).copy(alpha = 0.56f)),
        )
        VantafynThemePreset.Midnight -> tokensFor(VantafynThemePreset.Nebula).copy(
            preset = preset,
            graphite = Color(0xFF03050A),
            surface = Color(0xFF0D111A),
            surfaceHigh = Color(0xFF151B27),
            muted = Color(0xFFACB8D0),
            primary = Color(0xFF7CA7FF),
            secondary = Color(0xFF56C8F6),
            accentColors = listOf(Color(0xFF49C7FF), Color(0xFF587DFF), Color(0xFF6B5DFF), Color(0xFF9B67FF)),
            backgroundGradient = listOf(Color(0xFF080D16), Color(0xFF03050A), Color(0xFF020308)),
            navDockColors = listOf(Color(0xFF080D16).copy(alpha = 0.94f), Color(0xFF101827).copy(alpha = 0.90f), Color(0xFF050914).copy(alpha = 0.95f)),
            glassNavyCore = Color(0xFF0A1020),
            glassNavyLift = Color(0xFF111A2D),
            glassIndigoLift = Color(0xFF1B2540),
            glassVioletLift = Color(0xFF24274A),
        )
        VantafynThemePreset.Aurora -> tokensFor(VantafynThemePreset.Nebula).copy(
            preset = preset,
            graphite = Color(0xFF03100F),
            surface = Color(0xFF0B1B1D),
            surfaceHigh = Color(0xFF10292D),
            muted = Color(0xFFB2D0D2),
            primary = Color(0xFF77F0DC),
            secondary = Color(0xFF4BC8FF),
            accentColors = listOf(Color(0xFF35E7C7), Color(0xFF3EC9FF), Color(0xFF4A83FF), Color(0xFF7A69FF)),
            backgroundGradient = listOf(Color(0xFF071919), Color(0xFF03100F), Color(0xFF061120)),
            glassNavyCore = Color(0xFF0B2023),
            glassNavyLift = Color(0xFF123138),
            glassIndigoLift = Color(0xFF16414A),
            glassVioletLift = Color(0xFF1D3550),
            glassCyanSpecular = Color(0xFF71F3E7),
            glassBlueSpecular = Color(0xFF4BC8FF),
            glassVioletSpecular = Color(0xFF6E7CFF),
        )
        VantafynThemePreset.Amethyst -> tokensFor(VantafynThemePreset.Nebula).copy(
            preset = preset,
            graphite = Color(0xFF090713),
            surface = Color(0xFF151125),
            surfaceHigh = Color(0xFF211A38),
            primary = Color(0xFFA991FF),
            secondary = Color(0xFF70CAFF),
            accentColors = listOf(Color(0xFF6FD2FF), Color(0xFF817DFF), Color(0xFFA15CFF), Color(0xFFD16CFF)),
            backgroundGradient = listOf(Color(0xFF140E24), Color(0xFF090713), Color(0xFF05040C)),
            glassNavyCore = Color(0xFF141226),
            glassNavyLift = Color(0xFF201B38),
            glassIndigoLift = Color(0xFF312556),
            glassVioletLift = Color(0xFF43245E),
            glassBlueSpecular = Color(0xFF817DFF),
            glassVioletSpecular = Color(0xFFC06CFF),
        )
        VantafynThemePreset.Ember -> tokensFor(VantafynThemePreset.Nebula).copy(
            preset = preset,
            graphite = Color(0xFF0E0706),
            surface = Color(0xFF1B1110),
            surfaceHigh = Color(0xFF291918),
            muted = Color(0xFFD2B8AF),
            primary = Color(0xFFFFA35C),
            secondary = Color(0xFFFF6E8D),
            gold = Color(0xFFFFCA60),
            destructive = Color(0xFFFF7D7D),
            accentColors = listOf(Color(0xFFFFBA55), Color(0xFFFF7C61), Color(0xFFFF5E9C), Color(0xFFA56DFF)),
            backgroundGradient = listOf(Color(0xFF190D0B), Color(0xFF0E0706), Color(0xFF070403)),
            glassNavyCore = Color(0xFF201412),
            glassNavyLift = Color(0xFF321C18),
            glassIndigoLift = Color(0xFF422421),
            glassVioletLift = Color(0xFF4A2136),
            glassCyanSpecular = Color(0xFFFFB35F),
            glassBlueSpecular = Color(0xFFFF7C61),
            glassVioletSpecular = Color(0xFFFF5E9C),
        )
        VantafynThemePreset.Oled -> tokensFor(VantafynThemePreset.Nebula).copy(
            preset = preset,
            graphite = Color.Black,
            surface = Color(0xFF07090D),
            surfaceHigh = Color(0xFF10131A),
            muted = Color(0xFFAEB7CA),
            primary = Color(0xFF80A7FF),
            secondary = Color(0xFF55D8FF),
            accentColors = listOf(Color(0xFF31D7FF), Color(0xFF5B8CFF), Color(0xFF8062FF), Color(0xFFA65CFF)),
            backgroundGradient = listOf(Color.Black, Color.Black, Color(0xFF020307)),
            navDockColors = listOf(Color.Black.copy(alpha = 0.96f), Color(0xFF070B14).copy(alpha = 0.92f), Color.Black.copy(alpha = 0.97f)),
            glassNavyCore = Color(0xFF050912),
            glassNavyLift = Color(0xFF0A1020),
            glassIndigoLift = Color(0xFF131A30),
            glassVioletLift = Color(0xFF17142B),
        )
    }

object VantafynColors {
    val Graphite: Color get() = VantafynThemeController.tokens.graphite
    val Surface: Color get() = VantafynThemeController.tokens.surface
    val SurfaceHigh: Color get() = VantafynThemeController.tokens.surfaceHigh
    val Ink: Color get() = VantafynThemeController.tokens.ink
    val Muted: Color get() = VantafynThemeController.tokens.muted
    val Primary: Color get() = VantafynThemeController.tokens.primary
    val Secondary: Color get() = VantafynThemeController.tokens.secondary
    val Gold: Color get() = VantafynThemeController.tokens.gold
    val Destructive: Color get() = VantafynThemeController.tokens.destructive
    val Border: Color get() = VantafynThemeController.tokens.border
}

object VantafynSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val tvGutter = 64.dp
}

object VantafynRadii {
    val button = 12.dp
    val card = 12.dp
    val sheet = 16.dp
}

object VantafynMotion {
    const val Quick = 140
    const val Standard = 240
    const val Slow = 420
    val EaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

@Immutable
data class PosterSpec(
    val width: Dp,
    val height: Dp,
)

val TvPosterSpec = PosterSpec(width = 156.dp, height = 234.dp)
val MobilePosterSpec = PosterSpec(width = 124.dp, height = 186.dp)

@Composable
fun VantafynTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    VantafynThemeController.initialize(context)
    val targetPreset = VantafynThemeController.selectedPreset
    val targetTokens = remember(targetPreset) { tokensFor(targetPreset) }
    val tokens = rememberAnimatedThemeTokens(targetTokens)
    SideEffect {
        VantafynThemeController.publishRenderedTokens(tokens)
    }
    val colorScheme = darkColorScheme(
        background = tokens.graphite,
        surface = tokens.surface,
        surfaceVariant = tokens.surfaceHigh,
        primary = tokens.primary,
        secondary = tokens.secondary,
        tertiary = tokens.destructive,
        onBackground = tokens.ink,
        onSurface = tokens.ink,
        onPrimary = Color(0xFF071024),
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography.copy(
            displayLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 44.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            ),
        ),
        content = content,
    )
}

@Composable
private fun rememberAnimatedThemeTokens(target: VantafynThemeTokens): VantafynThemeTokens {
    val spec = remember {
        tween<Color>(
            durationMillis = 260,
            easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
        )
    }
    return target.copy(
        graphite = animateThemeColor(target.graphite, spec, "themeGraphite"),
        surface = animateThemeColor(target.surface, spec, "themeSurface"),
        surfaceHigh = animateThemeColor(target.surfaceHigh, spec, "themeSurfaceHigh"),
        ink = animateThemeColor(target.ink, spec, "themeInk"),
        muted = animateThemeColor(target.muted, spec, "themeMuted"),
        primary = animateThemeColor(target.primary, spec, "themePrimary"),
        secondary = animateThemeColor(target.secondary, spec, "themeSecondary"),
        gold = animateThemeColor(target.gold, spec, "themeGold"),
        destructive = animateThemeColor(target.destructive, spec, "themeDestructive"),
        border = animateThemeColor(target.border, spec, "themeBorder"),
        accentColors = animateThemeColorList(target.accentColors, spec, "themeAccent"),
        backgroundGradient = animateThemeColorList(target.backgroundGradient, spec, "themeBackground"),
        navDockColors = animateThemeColorList(target.navDockColors, spec, "themeNavDock"),
        navDockBorderColors = animateThemeColorList(target.navDockBorderColors, spec, "themeNavBorder"),
        glassNavyCore = animateThemeColor(target.glassNavyCore, spec, "themeGlassCore"),
        glassNavyLift = animateThemeColor(target.glassNavyLift, spec, "themeGlassLift"),
        glassIndigoLift = animateThemeColor(target.glassIndigoLift, spec, "themeGlassIndigo"),
        glassVioletLift = animateThemeColor(target.glassVioletLift, spec, "themeGlassViolet"),
        glassCyanSpecular = animateThemeColor(target.glassCyanSpecular, spec, "themeGlassCyan"),
        glassBlueSpecular = animateThemeColor(target.glassBlueSpecular, spec, "themeGlassBlue"),
        glassVioletSpecular = animateThemeColor(target.glassVioletSpecular, spec, "themeGlassVioletSpec"),
        glassEdgeWhite = animateThemeColor(target.glassEdgeWhite, spec, "themeGlassEdge"),
        bottomScrimColors = animateThemeColorList(target.bottomScrimColors, spec, "themeBottomScrim"),
    )
}

@Composable
private fun animateThemeColor(
    target: Color,
    animationSpec: androidx.compose.animation.core.AnimationSpec<Color>,
    label: String,
): Color {
    val color by animateColorAsState(targetValue = target, animationSpec = animationSpec, label = label)
    return color
}

@Composable
private fun animateThemeColorList(
    target: List<Color>,
    animationSpec: androidx.compose.animation.core.AnimationSpec<Color>,
    label: String,
): List<Color> {
    val animated = ArrayList<Color>(target.size)
    for (index in target.indices) {
        animated += animateThemeColor(target[index], animationSpec, "$label$index")
    }
    return animated
}

@Composable
fun VantafynSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = VantafynThemeController.tokens.backgroundGradient,
                ),
            ),
    ) {
        content()
    }
}

@Composable
fun PosterCard(
    title: String,
    modifier: Modifier = Modifier,
    spec: PosterSpec = TvPosterSpec,
) {
    var focused by remember { mutableStateOf(false) }
    val border = if (focused) {
        BorderStroke(2.dp, VantafynColors.Primary)
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    }

    Column(
        modifier = modifier.width(spec.width),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .width(spec.width)
                .height(spec.height)
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .clip(RoundedCornerShape(VantafynRadii.card))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VantafynColors.SurfaceHigh,
                            Color(0xFF30353C),
                            Color(0xFF15181D),
                        ),
                    ),
                )
                .border(border, RoundedCornerShape(VantafynRadii.card))
                .padding(VantafynSpacing.md),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = title.take(2).uppercase(),
                color = VantafynColors.Ink.copy(alpha = 0.64f),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Text(
            text = title,
            color = if (focused) VantafynColors.Ink else VantafynColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = rememberLifecycleAwareMarquee(),
        )
    }
}

fun tvScreenPadding(): PaddingValues = PaddingValues(
    start = VantafynSpacing.tvGutter,
    top = VantafynSpacing.xl,
    end = VantafynSpacing.tvGutter,
    bottom = VantafynSpacing.xl,
)

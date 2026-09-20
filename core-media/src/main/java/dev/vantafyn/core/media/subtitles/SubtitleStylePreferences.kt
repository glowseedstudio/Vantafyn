package dev.vantafyn.core.media.subtitles

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SubtitleEdgeStyle(val label: String, val compatEdgeType: Int) {
    None("None", CaptionStyleCompat.EDGE_TYPE_NONE),
    Outline("Outline", CaptionStyleCompat.EDGE_TYPE_OUTLINE),
    DropShadow("Drop Shadow", CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW),
    Raised("Raised", CaptionStyleCompat.EDGE_TYPE_RAISED),
    Depressed("Depressed", CaptionStyleCompat.EDGE_TYPE_DEPRESSED),
}

enum class SubtitleBackgroundStyle(val label: String) {
    None("None"),
    SurroundBox("Box"),
    FullWindow("Window"),
}

data class SubtitleStyleConfig(
    val fontSizeSp: Float = 20f,
    val textColor: Long = 0xFFFFFFFF,
    val textOpacity: Float = 1.0f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val edgeStyle: SubtitleEdgeStyle = SubtitleEdgeStyle.Outline,
    val edgeColor: Long = 0xFF000000,
    val backgroundStyle: SubtitleBackgroundStyle = SubtitleBackgroundStyle.None,
    val backgroundColor: Long = 0xFF000000,
    val backgroundOpacity: Float = 0.65f,
    val bottomPaddingFraction: Float = 0.08f,
) {
    @OptIn(UnstableApi::class)
    fun toCaptionStyleCompat(): CaptionStyleCompat {
        val textAlphaInt = (textOpacity.coerceIn(0.1f, 1.0f) * 255).toInt()
        val argbTextColor = (textColor.toInt() and 0x00FFFFFF) or (textAlphaInt shl 24)

        val bgAlphaInt = (backgroundOpacity.coerceIn(0.0f, 1.0f) * 255).toInt()
        val argbBgColor = (backgroundColor.toInt() and 0x00FFFFFF) or (bgAlphaInt shl 24)

        val argbEdgeColor = (edgeColor.toInt() and 0x00FFFFFF) or (textAlphaInt shl 24)

        val typeface = when {
            isBold && isItalic -> Typeface.defaultFromStyle(Typeface.BOLD_ITALIC)
            isBold -> Typeface.defaultFromStyle(Typeface.BOLD)
            isItalic -> Typeface.defaultFromStyle(Typeface.ITALIC)
            else -> Typeface.DEFAULT
        }

        val boxColor = if (backgroundStyle == SubtitleBackgroundStyle.SurroundBox) argbBgColor else 0
        val windowColor = if (backgroundStyle == SubtitleBackgroundStyle.FullWindow) argbBgColor else 0

        return CaptionStyleCompat(
            argbTextColor,
            boxColor,
            windowColor,
            edgeStyle.compatEdgeType,
            argbEdgeColor,
            typeface,
        )
    }

    @OptIn(UnstableApi::class)
    fun applyTo(view: SubtitleView) {
        view.setStyle(toCaptionStyleCompat())
        view.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
        view.setBottomPaddingFraction(bottomPaddingFraction)
    }

    companion object {
        val Default = SubtitleStyleConfig()

        val ClassicCinema = SubtitleStyleConfig(
            fontSizeSp = 20f,
            textColor = 0xFFFFFFFF,
            textOpacity = 1.0f,
            isBold = false,
            edgeStyle = SubtitleEdgeStyle.Outline,
            edgeColor = 0xFF000000,
            backgroundStyle = SubtitleBackgroundStyle.None,
        )

        val GoldenHour = SubtitleStyleConfig(
            fontSizeSp = 21f,
            textColor = 0xFFFFD700,
            textOpacity = 1.0f,
            isBold = true,
            edgeStyle = SubtitleEdgeStyle.DropShadow,
            edgeColor = 0xFF000000,
            backgroundStyle = SubtitleBackgroundStyle.None,
        )

        val HighContrast = SubtitleStyleConfig(
            fontSizeSp = 20f,
            textColor = 0xFFFFFFFF,
            textOpacity = 1.0f,
            isBold = true,
            edgeStyle = SubtitleEdgeStyle.None,
            backgroundStyle = SubtitleBackgroundStyle.SurroundBox,
            backgroundColor = 0xFF000000,
            backgroundOpacity = 0.80f,
        )

        val CyberNeon = SubtitleStyleConfig(
            fontSizeSp = 20f,
            textColor = 0xFF31D7FF,
            textOpacity = 1.0f,
            isBold = true,
            edgeStyle = SubtitleEdgeStyle.Outline,
            edgeColor = 0xFF140D2B,
            backgroundStyle = SubtitleBackgroundStyle.None,
        )
    }
}

object SubtitleStylePreferences {
    private const val PREFS_NAME = "vantafyn_subtitle_style_prefs"
    private const val KEY_FONT_SIZE = "sub_font_size"
    private const val KEY_TEXT_COLOR = "sub_text_color"
    private const val KEY_TEXT_OPACITY = "sub_text_opacity"
    private const val KEY_IS_BOLD = "sub_is_bold"
    private const val KEY_IS_ITALIC = "sub_is_italic"
    private const val KEY_EDGE_STYLE = "sub_edge_style"
    private const val KEY_EDGE_COLOR = "sub_edge_color"
    private const val KEY_BG_STYLE = "sub_bg_style"
    private const val KEY_BG_COLOR = "sub_bg_color"
    private const val KEY_BG_OPACITY = "sub_bg_opacity"
    private const val KEY_BOTTOM_PADDING = "sub_bottom_padding"

    private val _configFlow = MutableStateFlow(SubtitleStyleConfig.Default)
    val configFlow: StateFlow<SubtitleStyleConfig> = _configFlow.asStateFlow()

    fun get(context: Context): SubtitleStyleConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val edgeStyleName = prefs.getString(KEY_EDGE_STYLE, SubtitleEdgeStyle.Outline.name)
        val edgeStyle = SubtitleEdgeStyle.entries.firstOrNull { it.name == edgeStyleName } ?: SubtitleEdgeStyle.Outline

        val bgStyleName = prefs.getString(KEY_BG_STYLE, SubtitleBackgroundStyle.None.name)
        val bgStyle = SubtitleBackgroundStyle.entries.firstOrNull { it.name == bgStyleName } ?: SubtitleBackgroundStyle.None

        val config = SubtitleStyleConfig(
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE, 20f),
            textColor = prefs.getLong(KEY_TEXT_COLOR, 0xFFFFFFFF),
            textOpacity = prefs.getFloat(KEY_TEXT_OPACITY, 1.0f),
            isBold = prefs.getBoolean(KEY_IS_BOLD, false),
            isItalic = prefs.getBoolean(KEY_IS_ITALIC, false),
            edgeStyle = edgeStyle,
            edgeColor = prefs.getLong(KEY_EDGE_COLOR, 0xFF000000),
            backgroundStyle = bgStyle,
            backgroundColor = prefs.getLong(KEY_BG_COLOR, 0xFF000000),
            backgroundOpacity = prefs.getFloat(KEY_BG_OPACITY, 0.65f),
            bottomPaddingFraction = prefs.getFloat(KEY_BOTTOM_PADDING, 0.08f),
        )
        _configFlow.value = config
        return config
    }

    fun save(context: Context, config: SubtitleStyleConfig) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_FONT_SIZE, config.fontSizeSp)
            .putLong(KEY_TEXT_COLOR, config.textColor)
            .putFloat(KEY_TEXT_OPACITY, config.textOpacity)
            .putBoolean(KEY_IS_BOLD, config.isBold)
            .putBoolean(KEY_IS_ITALIC, config.isItalic)
            .putString(KEY_EDGE_STYLE, config.edgeStyle.name)
            .putLong(KEY_EDGE_COLOR, config.edgeColor)
            .putString(KEY_BG_STYLE, config.backgroundStyle.name)
            .putLong(KEY_BG_COLOR, config.backgroundColor)
            .putFloat(KEY_BG_OPACITY, config.backgroundOpacity)
            .putFloat(KEY_BOTTOM_PADDING, config.bottomPaddingFraction)
            .apply()

        _configFlow.value = config
    }

    fun reset(context: Context) {
        save(context, SubtitleStyleConfig.Default)
    }
}

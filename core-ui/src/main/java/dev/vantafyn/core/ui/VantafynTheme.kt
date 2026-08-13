package dev.vantafyn.core.ui

import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object VantafynColors {
    val Graphite = Color(0xFF070A12)
    val Surface = Color(0xFF141822)
    val SurfaceHigh = Color(0xFF1B2030)
    val Ink = Color(0xFFF5F7FF)
    val Muted = Color(0xFFB8C0D8)
    val Primary = Color(0xFF8EA2FF)
    val Secondary = Color(0xFF62D6FF)
    val Gold = Color(0xFFFFC94A)
    val Destructive = Color(0xFFA96A72)
    val Border = Color.White.copy(alpha = 0.12f)
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

private val colorScheme = darkColorScheme(
    background = VantafynColors.Graphite,
    surface = VantafynColors.Surface,
    surfaceVariant = VantafynColors.SurfaceHigh,
    primary = VantafynColors.Primary,
    secondary = VantafynColors.Secondary,
    tertiary = VantafynColors.Destructive,
    onBackground = VantafynColors.Ink,
    onSurface = VantafynColors.Ink,
    onPrimary = Color(0xFF071024),
)

@Composable
fun VantafynTheme(content: @Composable () -> Unit) {
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
fun VantafynSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E1320),
                        VantafynColors.Graphite,
                        Color(0xFF060912),
                    ),
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
        )
    }
}

fun tvScreenPadding(): PaddingValues = PaddingValues(
    start = VantafynSpacing.tvGutter,
    top = VantafynSpacing.xl,
    end = VantafynSpacing.tvGutter,
    bottom = VantafynSpacing.xl,
)

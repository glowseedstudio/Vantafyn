package dev.vantafyn.tv.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients

object TvFocusTokens {
    const val FocusScale = 1.05f
    const val ButtonFocusScale = 1.06f
    const val DefaultScale = 1.0f
    val FocusBorderWidth = 2.5.dp
    val DefaultBorderWidth = 1.dp
    val FocusCornerRadius = 14.dp

    val FocusBorderBrush: Brush
        @Composable get() = Brush.horizontalGradient(VantafynFocusGradientColors)

    val UnfocusedBorderColor = Color.White.copy(alpha = 0.12f)
    val CardSurface = VantafynColors.Surface
    val GlassCardSurface = Color(0xCC141822)
}

@Composable
fun Modifier.vantafynTvFocusable(
    interactionSource: MutableInteractionSource,
    shape: Shape = RoundedCornerShape(TvFocusTokens.FocusCornerRadius),
    scaleFocused: Float = TvFocusTokens.FocusScale,
    borderWidth: Dp = TvFocusTokens.FocusBorderWidth,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scaleFocused else TvFocusTokens.DefaultScale,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "TvFocusScale",
    )
    val borderBrush = TvFocusTokens.FocusBorderBrush

    return this
        .scale(animatedScale)
        .then(
            if (isFocused) {
                Modifier.border(
                    border = BorderStroke(borderWidth, borderBrush),
                    shape = shape,
                )
            } else {
                Modifier.border(
                    border = BorderStroke(TvFocusTokens.DefaultBorderWidth, TvFocusTokens.UnfocusedBorderColor),
                    shape = shape,
                )
            }
        )
}

@Composable
fun VantafynLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(14.dp),
) {
    Box(
        modifier = modifier
            .then(Modifier.size(size))
            .clip(shape)
            .background(Color.Black)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = CoreUiR.drawable.vantafyn_logo),
            contentDescription = "Vantafyn",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

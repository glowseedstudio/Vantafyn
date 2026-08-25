package dev.vantafyn.tv.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors

@Composable
fun VantafynTvGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "TvButtonScale",
    )

    val shape = RoundedCornerShape(if (compact) 15.dp else 18.dp)
    val accentGradient = Brush.horizontalGradient(VantafynFocusGradientColors)
    val horizontalPadding = if (compact) 15.dp else 24.dp
    val verticalPadding = if (compact) 6.dp else 13.dp
    val iconSize = if (compact) 14.dp else 20.dp
    val iconGap = if (compact) 6.dp else 10.dp
    val textSize = if (compact) 12.sp else 15.sp

    val backgroundBrush = if (isPrimary) {
        if (enabled) {
            accentGradient
        } else {
            Brush.horizontalGradient(
                listOf(
                    VantafynColors.SurfaceHigh.copy(alpha = 0.7f),
                    Color(0xFF252B3D).copy(alpha = 0.7f),
                ),
            )
        }
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xCC141A2E),
                Color(0xEE0B0F1C),
            ),
        )
    }

    val borderStroke = if (isFocused && enabled) {
        if (isPrimary) {
            BorderStroke(2.dp, Color.White.copy(alpha = 0.85f))
        } else {
            BorderStroke(2.dp, accentGradient)
        }
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = if (compact) 34.dp else 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundBrush, shape)
            .border(borderStroke, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource, enabled = enabled)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) VantafynColors.Ink else VantafynColors.Muted,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(modifier = Modifier.width(iconGap))
            }

            Text(
                text = text,
                color = if (enabled) VantafynColors.Ink else VantafynColors.Muted,
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

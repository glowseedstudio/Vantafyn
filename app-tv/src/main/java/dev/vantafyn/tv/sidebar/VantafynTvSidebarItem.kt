package dev.vantafyn.tv.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients

@Composable
fun VantafynTvSidebarItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    onFocused: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "SidebarItemScale",
    )

    val backgroundBrush = when {
        isFocused -> Brush.horizontalGradient(
            listOf(
                Color(0x358EA2FF),
                Color(0x20C05CFF),
            )
        )
        isSelected -> Brush.horizontalGradient(
            listOf(
                Color(0x248EA2FF),
                Color(0x0A8EA2FF),
            )
        )
        else -> Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.Transparent,
            )
        )
    }

    val contentColor = when {
        isFocused -> VantafynColors.Ink
        isSelected -> VantafynColors.Primary
        else -> VantafynColors.Muted
    }

    val accentGradient = VantafynGradients.accentHorizontal()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isFocused) {
                    Modifier.border(
                        border = BorderStroke(1.5.dp, accentGradient),
                        shape = shape,
                    )
                } else if (isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = Color(0x338EA2FF),
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(backgroundBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // Icon with active badge indicator
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )

            // Small badge dot on collapsed state if badgeCount > 0
            if (!isExpanded && badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(VantafynColors.Primary)
                )
            }
        }

        // Label (visible when expanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = spring(stiffness = 600f)),
            exit = fadeOut(animationSpec = spring(stiffness = 600f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x338EA2FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            color = VantafynColors.Primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

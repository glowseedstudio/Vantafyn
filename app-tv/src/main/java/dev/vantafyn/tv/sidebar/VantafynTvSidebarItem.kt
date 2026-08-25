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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "SidebarItemScale",
    )

    // Pure transparent dark glass - zero blue wash
    val backgroundBrush = when {
        isFocused -> Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.10f),
            )
        )
        isSelected -> Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.04f),
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
        isSelected -> VantafynColors.Ink
        else -> Color(0xFFCBD5E1)
    }

    val accentGradient = VantafynGradients.accentHorizontal()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isFocused) {
                    Modifier.border(
                        border = BorderStroke(1.5.dp, accentGradient),
                        shape = shape,
                    )
                } else if (isSelected) {
                    Modifier.border(
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
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
            .padding(horizontal = 14.dp),
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
            if (badgeCount > 0 && !isExpanded) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9500))
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = spring(stiffness = 600f)),
            exit = fadeOut(animationSpec = spring(stiffness = 600f)),
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF9500))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VantafynTvSidebarIconButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    onFocused: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) onFocused()
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "SidebarIconScale",
    )

    val shape = RoundedCornerShape(12.dp)
    val accentGradient = VantafynGradients.accentHorizontal()

    val backgroundBrush = when {
        isFocused -> Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.12f),
            )
        )
        isSelected -> Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.04f),
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color.Transparent,
            )
        )
    }

    val borderStroke = when {
        isFocused -> BorderStroke(1.5.dp, accentGradient)
        isSelected -> BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
        else -> BorderStroke(1.dp, Color.Transparent)
    }

    val contentColor = when {
        isFocused -> VantafynColors.Ink
        isSelected -> VantafynColors.Ink
        else -> VantafynColors.Muted
    }

    Box(
        modifier = modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundBrush, shape)
            .border(borderStroke, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9500))
            )
        }
    }
}

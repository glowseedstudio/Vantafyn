package dev.vantafyn.tv.setup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Smartphone
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassPalette
import dev.vantafyn.tv.components.VantafynFocusGradientColors
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton

@Composable
fun TvSetupMethodScreen(
    onPairWithMobile: () -> Unit,
    onQuickConnect: () -> Unit,
    onManualSetup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quickConnectFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            quickConnectFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VantafynLogoBadge(
                    size = 52.dp,
                    shape = RoundedCornerShape(14.dp),
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Set up Vantafyn",
                    color = VantafynColors.Ink,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Choose how you'd like to connect this TV to your Jellyfin server",
                    color = VantafynColors.Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Options Row (3 Centered Cards with Custom Gradient Accent Colors & Ambient Glow)
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Mobile Pairing (Cyan Theme)
                SetupOptionCard(
                    title = "Pair with mobile app",
                    subtitle = "Use your phone to connect this TV seamlessly.",
                    icon = Icons.Rounded.Smartphone,
                    accentColor = Color(0xFF21D8FF), // Electric Cyan
                    onClick = onPairWithMobile,
                )

                // 2. Quick Connect (Purple Theme - Initial Default Focus)
                SetupOptionCard(
                    title = "Quick Connect",
                    subtitle = "Approve this TV from another Jellyfin app.",
                    icon = Icons.Rounded.Bolt,
                    accentColor = Color(0xFFA855F7), // Vibrant Gradient Purple / Violet
                    focusRequester = quickConnectFocusRequester,
                    onClick = onQuickConnect,
                )

                // 3. Manual Setup (Pink Theme)
                SetupOptionCard(
                    title = "Sign in manually",
                    subtitle = "Enter your Jellyfin server address and credentials.",
                    icon = Icons.Rounded.Key,
                    accentColor = Color(0xFFFF36C7), // Hot Magenta / Pink
                    onClick = onManualSetup,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Back Button
            VantafynTvGlassButton(
                text = "Back",
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                isPrimary = false,
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun SetupOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "SetupOptionCardScale",
    )

    // See-through translucent navy glass matching VantafynTvTextField & Mobile Glass Cards
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) {
            VantafynGlassPalette.NavyLift.copy(alpha = 0.65f)
        } else {
            VantafynGlassPalette.NavyCore.copy(alpha = 0.42f)
        },
        label = "SetupOptionCardContainer",
    )

    val accentGradient = Brush.horizontalGradient(VantafynFocusGradientColors)

    val borderStroke = if (isFocused) {
        BorderStroke(2.dp, accentGradient)
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    }

    Box(
        modifier = modifier
            .width(250.dp)
            .height(195.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(containerColor, shape)
            .border(borderStroke, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Centered Premium Icon Badge with Soft Ambient Glow of its Color
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .drawBehind {
                        val glowRadius = 30.dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (isFocused) 0.55f else 0.35f),
                                    accentColor.copy(alpha = if (isFocused) 0.22f else 0.12f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = glowRadius,
                            ),
                            radius = glowRadius,
                        )
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = 0.22f),
                                accentColor.copy(alpha = 0.08f),
                            ),
                        ),
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    accentColor.copy(alpha = 0.50f),
                                ),
                            ),
                        ),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) Color.White else accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Centered Title
            Text(
                text = title,
                color = if (isFocused) VantafynColors.Ink else Color(0xFFE2E8F0),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Centered Subtitle
            Text(
                text = subtitle,
                color = VantafynColors.Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

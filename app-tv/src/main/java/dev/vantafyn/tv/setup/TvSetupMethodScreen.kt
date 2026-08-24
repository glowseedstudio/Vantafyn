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
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import dev.vantafyn.tv.components.VantafynTvSetupCinematicEasing

@Composable
fun TvSetupMethodScreen(
    onPairWithMobile: () -> Unit,
    onManualSetup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRevealed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isRevealed = true
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "methodHeaderAlpha",
    )
    val headerTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 28f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "methodHeaderTranslationY",
    )

    val cardsAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 160, easing = VantafynTvSetupCinematicEasing),
        label = "methodCardsAlpha",
    )
    val cardsTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 24f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 160, easing = VantafynTvSetupCinematicEasing),
        label = "methodCardsTranslationY",
    )

    val actionsAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 320, easing = VantafynTvSetupCinematicEasing),
        label = "methodActionsAlpha",
    )
    val actionsTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 18f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 320, easing = VantafynTvSetupCinematicEasing),
        label = "methodActionsTranslationY",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = headerAlpha
                    translationY = headerTranslationY
                },
            ) {
                VantafynLogoBadge(
                    size = 56.dp,
                    shape = RoundedCornerShape(16.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Set up Vantafyn",
                    color = VantafynColors.Ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose how you'd like to connect this TV to your Jellyfin server",
                    color = VantafynColors.Muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Options Row (Centered Cards with Translucent Glass)
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    alpha = cardsAlpha
                    translationY = cardsTranslationY
                },
            ) {
                SetupOptionCard(
                    title = "Pair with mobile app",
                    subtitle = "Use your phone to connect this TV seamlessly.",
                    icon = Icons.Rounded.Smartphone,
                    onClick = onPairWithMobile,
                )

                SetupOptionCard(
                    title = "Sign in manually",
                    subtitle = "Enter your Jellyfin server address and credentials.",
                    icon = Icons.Rounded.Key,
                    onClick = onManualSetup,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Back Button
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = actionsAlpha
                    translationY = actionsTranslationY
                },
            ) {
                VantafynTvGlassButton(
                    text = "Back",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    isPrimary = false,
                    onClick = onBack,
                )
            }

            // Bottom clearance for scaled button focus borders
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SetupOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
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

    Box(
        modifier = modifier
            .width(310.dp)
            .height(230.dp)
            .scale(scale)
            .clip(shape)
            .background(containerColor)
            .then(
                if (isFocused) {
                    Modifier.border(BorderStroke(2.dp, accentGradient), shape)
                } else {
                    Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Centered Premium Icon Badge with Gradient Glow
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0x338EA2FF),
                                Color(0x1821D8FF),
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color(0xFF21D8FF).copy(alpha = 0.35f),
                                )
                            ),
                        ),
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) Color.White else VantafynColors.Primary,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Centered Title
            Text(
                text = title,
                color = if (isFocused) VantafynColors.Ink else Color(0xFFE2E8F0),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Centered Subtitle
            Text(
                text = subtitle,
                color = VantafynColors.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

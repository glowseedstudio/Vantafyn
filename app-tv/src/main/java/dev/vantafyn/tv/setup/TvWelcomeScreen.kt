package dev.vantafyn.tv.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvPulsingButtonGlow
import dev.vantafyn.tv.components.VantafynTvSetupCinematicEasing

@Composable
fun TvWelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPulseActive by remember { mutableStateOf(true) }
    var isRevealed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isRevealed = true
        focusRequester.requestFocus()
    }

    // Top Header staggered reveal
    val headerAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "headerAlpha",
    )
    val headerTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 32f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "headerTranslationY",
    )

    // Bottom Action staggered reveal
    val actionAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 360, easing = VantafynTvSetupCinematicEasing),
        label = "actionAlpha",
    )
    val actionTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 28f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 360, easing = VantafynTvSetupCinematicEasing),
        label = "actionTranslationY",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Header Group (0ms delay)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = headerAlpha
                    translationY = headerTranslationY
                },
            ) {
                VantafynLogoBadge(
                    size = 96.dp,
                    shape = RoundedCornerShape(24.dp),
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Welcome to Vantafyn",
                    color = VantafynColors.Ink,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your Jellyfin library, built for the living room.",
                    color = VantafynColors.Muted,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Action Group (180ms delay) with soft cyan/blue/violet pulse
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = actionAlpha
                    translationY = actionTranslationY
                },
                contentAlignment = Alignment.Center,
            ) {
                VantafynTvPulsingButtonGlow(enabled = isPulseActive) {
                    VantafynTvGlassButton(
                        text = "Get Started",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        isPrimary = true,
                        modifier = Modifier.focusRequester(focusRequester),
                        onClick = {
                            isPulseActive = false
                            onGetStarted()
                        },
                    )
                }
            }

            // Bottom clearance for scaled button focus borders
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

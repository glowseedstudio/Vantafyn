package dev.vantafyn.tv.setup

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvPulsingButtonGlow

@Composable
fun TvWelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPulseActive by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

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
            // Header Section
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

            Spacer(modifier = Modifier.height(36.dp))

            // Action Group with soft breathing pulse glow
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

            // Bottom clearance for scaled button focus borders
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

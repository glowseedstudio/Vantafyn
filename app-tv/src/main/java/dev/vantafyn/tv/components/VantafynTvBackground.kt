package dev.vantafyn.tv.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.core.ui.VantafynColors

@Composable
fun VantafynTvBackground(
    modifier: Modifier = Modifier,
    @DrawableRes backgroundResId: Int = CoreUiR.drawable.vantafyn_onboarding_background,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VantafynColors.Graphite)
    ) {
        // --- 1. REAL CINEMATIC NEBULA BACKGROUND IMAGE ---
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.04f
                    scaleY = 1.04f
                },
        )

        // --- 2. TV BASE DARK SCRIM ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f)),
        )

        // --- 3. HORIZONTAL READABILITY SCRIM (START TO END) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            VantafynColors.Graphite.copy(alpha = 0.72f),
                            VantafynColors.Graphite.copy(alpha = 0.36f),
                            Color.Transparent,
                        )
                    )
                ),
        )

        // --- 4. VERTICAL TOP & BOTTOM VIGNETTE SCRIM ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VantafynColors.Graphite.copy(alpha = 0.35f),
                            Color.Transparent,
                            VantafynColors.Graphite.copy(alpha = 0.70f),
                        )
                    )
                ),
        )

        // --- 5. CONTENT LAYER ---
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

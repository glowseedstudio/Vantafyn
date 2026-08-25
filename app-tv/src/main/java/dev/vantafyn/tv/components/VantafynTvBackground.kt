package dev.vantafyn.tv.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.core.ui.VantafynColors

@Composable
fun VantafynTvBackground(
    modifier: Modifier = Modifier,
    @DrawableRes backgroundResId: Int = CoreUiR.drawable.vantafyn_onboarding_background,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        val am = context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
        am != null && am.isTouchExplorationEnabled
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val driftX = if (reduceMotion || !isResumed) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "tvBgDrift")
        val offset by infiniteTransition.animateFloat(
            initialValue = -0.018f,
            targetValue = 0.018f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "tvBgDriftX",
        )
        offset
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VantafynColors.Graphite)
    ) {
        // --- 1. REAL CINEMATIC NEBULA BACKGROUND IMAGE ---
        Crossfade(
            targetState = backgroundResId,
            animationSpec = tween(durationMillis = 420),
            label = "tvBackgroundCrossfade",
        ) { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.04f
                        scaleY = 1.04f
                        translationX = size.width * driftX
                    },
            )
        }

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

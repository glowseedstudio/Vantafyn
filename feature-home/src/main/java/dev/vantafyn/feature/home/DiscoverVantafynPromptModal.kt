package dev.vantafyn.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradientButton
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder

@Composable
fun DiscoverVantafynPromptModal(
    onExplore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionPreference()
    val scaleAnim = remember { Animatable(if (reducedMotion) 1f else 0.92f) }
    val alphaAnim = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            )
        }
    }

    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(VantafynColors.Graphite.copy(alpha = 0.96f))
                    .vantafynAnimatedModalBorder(cornerRadius = 28.dp)
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Header Logo Tile matching About Vantafyn dialog
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(22.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = CoreUiR.drawable.vantafyn_logo),
                        contentDescription = "Vantafyn Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title
                Text(
                    text = "There’s a lot to discover",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Body Paragraph 1
                Text(
                    text = "Vantafyn is packed with features, shortcuts and ways to make the experience your own — more than we could reasonably fit into an introduction.",
                    color = VantafynColors.Ink.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Body Paragraph 2
                Text(
                    text = "If you'd like to see everything Vantafyn can do, take a look through Discover Vantafyn. You can always come back to it later.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action: Explore Vantafyn
                VantafynGradientButton(
                    text = "Explore Vantafyn",
                    onClick = onExplore,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Secondary Action: Maybe Later
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Maybe Later",
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

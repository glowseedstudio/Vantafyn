package dev.vantafyn.tv.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.JellyfinQuickConnectSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassPalette
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvSetupCinematicEasing

@Composable
fun TvQuickConnectScreen(
    session: JellyfinQuickConnectSession?,
    statusMessage: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
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
        label = "qcHeaderAlpha",
    )
    val headerTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 28f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "qcHeaderTranslationY",
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 160, easing = VantafynTvSetupCinematicEasing),
        label = "qcContentAlpha",
    )
    val contentTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 24f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 160, easing = VantafynTvSetupCinematicEasing),
        label = "qcContentTranslationY",
    )

    val actionsAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 320, easing = VantafynTvSetupCinematicEasing),
        label = "qcActionsAlpha",
    )
    val actionsTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 18f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 320, easing = VantafynTvSetupCinematicEasing),
        label = "qcActionsTranslationY",
    )

    val code = session?.code
    val isExpired = errorMessage != null && (errorMessage.contains("expired", ignoreCase = true) || errorMessage.contains("timeout", ignoreCase = true))

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

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Quick Connect",
                    color = VantafynColors.Ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Approve this TV from another Jellyfin app or dashboard",
                    color = VantafynColors.Muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main PIN / Code Card with Animated Gradient Border
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentTranslationY
                },
                contentAlignment = Alignment.Center,
            ) {
                val cardShape = RoundedCornerShape(24.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(480.dp)
                        .vantafynAnimatedModalBorder(
                            cornerRadius = 24.dp,
                            strokeWidth = 2.dp,
                            durationMillis = 4200,
                        )
                        .clip(cardShape)
                        .background(VantafynGlassPalette.NavyCore.copy(alpha = 0.45f))
                        .padding(horizontal = 36.dp, vertical = 28.dp),
                ) {
                    if (errorMessage != null && !isExpired) {
                        // Error State
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFFD4D4),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    } else if (isExpired) {
                        // Expired State
                        Text(
                            text = "Code Expired",
                            color = Color(0xFFFF6B6B),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The Quick Connect session timed out. Please generate a new code.",
                            color = VantafynColors.Muted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        // Active PIN / Code Display
                        val displayCode = if (code.isNullOrBlank()) {
                            if (isLoading) "------" else "----"
                        } else {
                            if (code.length == 6) {
                                "${code.substring(0, 3)}  ${code.substring(3)}"
                            } else {
                                code
                            }
                        }

                        Text(
                            text = "QUICK CONNECT CODE",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x228EA2FF))
                                .border(1.dp, Color(0x448EA2FF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 28.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = displayCode,
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 6.sp,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Info Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = VantafynColors.Primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Text(
                                text = statusMessage ?: if (isLoading) "Preparing code..." else "Waiting for approval...",
                                color = VantafynColors.Muted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Actions Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    alpha = actionsAlpha
                    translationY = actionsTranslationY
                },
            ) {
                VantafynTvGlassButton(
                    text = "Refresh Code",
                    icon = Icons.Rounded.Refresh,
                    isPrimary = isExpired || errorMessage != null,
                    enabled = !isLoading,
                    onClick = onRefresh,
                )

                VantafynTvGlassButton(
                    text = "Sign In Manually",
                    icon = Icons.Rounded.Key,
                    isPrimary = false,
                    onClick = onManualSetup,
                )

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

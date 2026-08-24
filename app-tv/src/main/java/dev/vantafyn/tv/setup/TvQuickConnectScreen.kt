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
import androidx.compose.foundation.layout.fillMaxWidth
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
    val code = session?.code
    val isExpired = errorMessage != null && (errorMessage.contains("expired", ignoreCase = true) || errorMessage.contains("timeout", ignoreCase = true))

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 16.dp),
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
                    size = 48.dp,
                    shape = RoundedCornerShape(14.dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Quick Connect",
                    color = VantafynColors.Ink,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Approve this TV from another Jellyfin app or web dashboard",
                    color = VantafynColors.Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main PIN / Code Card with Animated Gradient Border
            val cardShape = RoundedCornerShape(22.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(480.dp)
                    .vantafynAnimatedModalBorder(
                        cornerRadius = 22.dp,
                        strokeWidth = 1.5.dp,
                        durationMillis = 4800,
                    )
                    .clip(cardShape)
                    .background(VantafynColors.Graphite.copy(alpha = 0.96f))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
            ) {
                if (errorMessage != null && !isExpired) {
                    // Error State
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFFD4D4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                } else if (isExpired) {
                    // Expired State
                    Text(
                        text = "Code Expired",
                        color = Color(0xFFFF6B6B),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The Quick Connect session timed out. Please generate a new code.",
                        color = VantafynColors.Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    // Active PIN / Code Display
                    val activeCode = code ?: (if (isLoading) "------" else "----")

                    LuxurySegmentedCodeDisplay(
                        code = activeCode,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                            text = statusMessage ?: if (isLoading) "Preparing code..." else "Waiting for approval from your other device...",
                            color = VantafynColors.Muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Actions Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
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
        }
    }
}

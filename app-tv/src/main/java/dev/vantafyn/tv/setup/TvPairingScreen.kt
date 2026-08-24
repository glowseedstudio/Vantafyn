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
import androidx.compose.material.icons.rounded.Phonelink
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.TvPairingPayload
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvSetupCinematicEasing
import dev.vantafyn.tv.pairing.TvPairingServer
import kotlinx.coroutines.delay

@Composable
fun TvPairingScreen(
    onPairingSuccess: (TvPairingPayload) -> Unit,
    onManualSetup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pairingCode by remember { mutableStateOf("") }
    var expiresAtMs by remember { mutableLongStateOf(0L) }
    var isExpired by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var successPayload by remember { mutableStateOf<TvPairingPayload?>(null) }
    var isRevealed by remember { mutableStateOf(false) }

    val refreshFocusRequester = remember { FocusRequester() }

    // Start ephemeral pairing server
    val pairingServer = remember {
        TvPairingServer(
            onPairedPayload = { payload ->
                successPayload = payload
                isSuccess = true
            },
            onExpiredOrLimitReached = {
                isExpired = true
            },
        )
    }

    DisposableEffect(Unit) {
        pairingServer.start()
        pairingCode = pairingServer.currentCode
        expiresAtMs = pairingServer.expiresAtEpochMs
        onDispose {
            pairingServer.stop()
        }
    }

    // Auto-advance after brief success animation
    LaunchedEffect(isSuccess, successPayload) {
        if (isSuccess && successPayload != null) {
            delay(1_200L)
            onPairingSuccess(successPayload!!)
        }
    }

    // Live Countdown Timer
    var remainingSeconds by remember { mutableLongStateOf(300L) }
    LaunchedEffect(expiresAtMs, isExpired) {
        while (!isExpired && expiresAtMs > System.currentTimeMillis()) {
            val diff = (expiresAtMs - System.currentTimeMillis()) / 1000
            remainingSeconds = diff.coerceAtLeast(0)
            if (remainingSeconds <= 0) {
                isExpired = true
                break
            }
            delay(1_000L)
        }
    }

    LaunchedEffect(Unit) {
        isRevealed = true
        delay(100L)
        try {
            refreshFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Cinematic Staggered Reveal
    val headerAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "pairingHeaderAlpha",
    )
    val headerTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 28f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "pairingHeaderTranslationY",
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 160, easing = VantafynTvSetupCinematicEasing),
        label = "pairingContentAlpha",
    )
    val contentTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 24f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 160, easing = VantafynTvSetupCinematicEasing),
        label = "pairingContentTranslationY",
    )

    val actionsAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 320, easing = VantafynTvSetupCinematicEasing),
        label = "pairingActionsAlpha",
    )
    val actionsTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 18f,
        animationSpec = tween(durationMillis = 1_200, delayMillis = 320, easing = VantafynTvSetupCinematicEasing),
        label = "pairingActionsTranslationY",
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
            modifier = Modifier.fillMaxWidth(),
        ) {
            // --- HEADER ---
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
                    text = if (isSuccess) "TV Paired Successfully!" else "Pair with your phone",
                    color = VantafynColors.Ink,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isSuccess) {
                        "Connecting as ${successPayload?.profileDisplayName}..."
                    } else {
                        "Open Vantafyn on your phone and enter this code"
                    },
                    color = VantafynColors.Muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- MAIN PAIRING CARD / SUCCESS CARD ---
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentTranslationY
                },
                contentAlignment = Alignment.Center,
            ) {
                val cardShape = RoundedCornerShape(22.dp)

                if (isSuccess) {
                    // Success View
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(460.dp)
                            .clip(cardShape)
                            .background(Color(0xD910182A))
                            .border(BorderStroke(1.dp, Color(0xFF21D8FF).copy(alpha = 0.50f)), cardShape)
                            .padding(horizontal = 36.dp, vertical = 32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF21D8FF),
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connected to ${successPayload?.serverName ?: "Jellyfin"}",
                            color = VantafynColors.Ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Signed in as ${successPayload?.userName}",
                            color = VantafynColors.Muted,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(
                            color = VantafynColors.Primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    // Active Pairing Card
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(520.dp)
                            .clip(cardShape)
                            .background(Color(0xD910182A))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), cardShape)
                            .padding(horizontal = 36.dp, vertical = 26.dp),
                    ) {
                        if (isExpired) {
                            Text(
                                text = "Code Expired",
                                color = Color(0xFFFF6B6B),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The pairing session timed out. Please generate a new code.",
                                color = VantafynColors.Muted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            // Formatted 6-digit Code (e.g. "VF7 890")
                            val formattedCode = remember(pairingCode) {
                                if (pairingCode.length == 6) {
                                    "${pairingCode.substring(0, 3)}  ${pairingCode.substring(3)}"
                                } else {
                                    pairingCode
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x228EA2FF))
                                    .border(1.dp, Color(0x448EA2FF), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 28.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = formattedCode,
                                    color = Color.White,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Expiry Countdown
                            val minutes = remainingSeconds / 60
                            val seconds = remainingSeconds % 60
                            val timeStr = String.format("%d:%02d", minutes, seconds)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Phonelink,
                                    contentDescription = null,
                                    tint = VantafynColors.Primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Code expires in $timeStr",
                                    color = if (remainingSeconds < 60) Color(0xFFFFB020) else VantafynColors.Muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Instructions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "1. Open Vantafyn Mobile > Profile & Settings",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "2. Tap \"Pair a TV\" and enter code above",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "3. Confirm server & profile to connect instantly",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- ACTIONS ---
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
                    isPrimary = isExpired,
                    modifier = Modifier.focusRequester(refreshFocusRequester),
                    onClick = {
                        pairingCode = pairingServer.refreshCode()
                        expiresAtMs = pairingServer.expiresAtEpochMs
                        isExpired = false
                    },
                )

                VantafynTvGlassButton(
                    text = "Sign In Manually",
                    icon = Icons.Rounded.Tune,
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

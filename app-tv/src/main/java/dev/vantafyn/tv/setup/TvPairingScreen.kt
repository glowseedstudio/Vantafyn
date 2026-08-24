package dev.vantafyn.tv.setup

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.TvPairingPayload
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassPalette
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.pairing.TvPairingServer
import kotlinx.coroutines.delay

private val CodeTileGradientColors = listOf(
    Color(0xFF21D8FF), // Tile 1: Electric Cyan
    Color(0xFF3284FF), // Tile 2: Vibrant Cyan-Blue
    Color(0xFF5850FF), // Tile 3: Royal Blue-Violet
    Color(0xFF8B35FF), // Tile 4: Deep Violet
    Color(0xFFC836E2), // Tile 5: Violet-Magenta
    Color(0xFFFF36C7), // Tile 6: Hot Magenta
)

@Composable
fun TvPairingScreen(
    onPairingSuccess: (TvPairingPayload) -> Unit,
    onManualSetup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pairingCode by remember { mutableStateOf("") }
    var expiresAtMs by remember { mutableLongStateOf(0L) }
    var isSuccess by remember { mutableStateOf(false) }
    var successPayload by remember { mutableStateOf<TvPairingPayload?>(null) }
    var isExpired by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(300) }

    val refreshFocusRequester = remember { FocusRequester() }

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

    // Start embedded local HTTP pairing server
    DisposableEffect(pairingServer) {
        val started = pairingServer.start()
        if (started) {
            pairingCode = pairingServer.currentCode
            expiresAtMs = pairingServer.expiresAtEpochMs
        } else {
            pairingCode = "ERROR"
        }

        onDispose {
            pairingServer.stop()
        }
    }

    // Expiry countdown timer (ticks every 1s)
    LaunchedEffect(expiresAtMs, isSuccess) {
        if (isSuccess || expiresAtMs <= 0L) return@LaunchedEffect
        while (!isSuccess) {
            val now = System.currentTimeMillis()
            val diffMs = expiresAtMs - now
            if (diffMs <= 0) {
                remainingSeconds = 0
                isExpired = true
                break
            } else {
                remainingSeconds = (diffMs / 1000).toInt()
                isExpired = false
            }
            delay(1000L)
        }
    }

    // Auto-advance after showing success message
    LaunchedEffect(isSuccess) {
        if (isSuccess && successPayload != null) {
            delay(1_800L)
            onPairingSuccess(successPayload!!)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 16.dp),
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
            ) {
                VantafynLogoBadge(
                    size = 48.dp,
                    shape = RoundedCornerShape(14.dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isSuccess) "TV Paired Successfully!" else "Pair with your phone",
                    color = VantafynColors.Ink,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isSuccess) {
                        "Connecting as ${successPayload?.profileDisplayName}..."
                    } else {
                        "Open Vantafyn on your phone and enter this code"
                    },
                    color = VantafynColors.Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- MAIN PAIRING CARD / SUCCESS CARD (Matching Phone Modal Style & Animated Border) ---
            val cardShape = RoundedCornerShape(22.dp)

            if (isSuccess) {
                // Success View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(440.dp)
                        .vantafynAnimatedModalBorder(
                            cornerRadius = 22.dp,
                            strokeWidth = 1.5.dp,
                            durationMillis = 4800,
                        )
                        .clip(cardShape)
                        .background(VantafynColors.Graphite.copy(alpha = 0.96f))
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF21D8FF),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Connected to ${successPayload?.serverName ?: "Jellyfin"}",
                        color = VantafynColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Signed in as ${successPayload?.userName}",
                        color = VantafynColors.Muted,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = VantafynColors.Primary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                // Active Pairing Card with Mobile-Style Rotating Gradient Border & Dark Surface
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
                    if (isExpired) {
                        Text(
                            text = "Code Expired",
                            color = Color(0xFFFF6B6B),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The pairing session timed out. Please generate a new code.",
                            color = VantafynColors.Muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        // Ultra-Premium Segmented Code Tile Display with Full Continuous Gradient Flow
                        LuxurySegmentedCodeDisplay(
                            code = pairingCode,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Expiry Countdown (Cleanly rendered directly on dark surface)
                        val minutes = remainingSeconds / 60
                        val seconds = remainingSeconds % 60
                        val timeStr = String.format("%d:%02d", minutes, seconds)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                tint = Color(0xFF21D8FF),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Code expires in $timeStr",
                                color = if (remainingSeconds < 60) Color(0xFFFFB020) else Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clear 3-step Instructions with perfectly centered gradient color badges (Cyan -> Purple -> Magenta)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            InstructionStep(
                                number = "1",
                                text = "Open Vantafyn Mobile → Profile & Settings",
                                accentColor = Color(0xFF21D8FF), // Cyan
                            )
                            InstructionStep(
                                number = "2",
                                text = "Tap \"Pair a TV\" and enter the 6-digit code above",
                                accentColor = Color(0xFFA855F7), // Purple / Violet
                            )
                            InstructionStep(
                                number = "3",
                                text = "Confirm your server & profile to connect instantly",
                                accentColor = Color(0xFFFF36C7), // Magenta / Pink
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- BOTTOM ACTIONS ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
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
        }
    }
}

/**
 * Renders a 6-character code as two balanced 3-character glass badge groups with
 * continuous cyan-to-magenta top border glow flowing seamlessly across all 6 tiles.
 */
@Composable
fun LuxurySegmentedCodeDisplay(
    code: String,
    modifier: Modifier = Modifier,
) {
    val cleanCode = code.replace(" ", "").trim()
    val paddedCode = cleanCode.padEnd(6, '-').take(6)
    val group1 = paddedCode.substring(0, 3)
    val group2 = paddedCode.substring(3, 6)

    Box(
        modifier = modifier
            .drawBehind {
                // Ambient continuous horizontal gradient glow spanning all 6 code tiles
                val glowWidth = size.width + 36.dp.toPx()
                val glowHeight = size.height + 24.dp.toPx()
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF21D8FF).copy(alpha = 0.24f),
                            Color(0xFF3E63FF).copy(alpha = 0.18f),
                            Color(0xFF8B35FF).copy(alpha = 0.18f),
                            Color(0xFFFF36C7).copy(alpha = 0.24f),
                        ),
                    ),
                    topLeft = Offset(-18.dp.toPx(), -12.dp.toPx()),
                    size = Size(glowWidth, glowHeight),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // First 3 characters (Indices 0, 1, 2: Cyan to Royal Blue to Violet)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                group1.forEachIndexed { idx, char ->
                    CodeTile(
                        char = char.toString(),
                        accentColor = CodeTileGradientColors[idx],
                    )
                }
            }

            // Luminous gradient divider linking the two 3-character segments
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CodeTileGradientColors[2],
                                CodeTileGradientColors[3],
                            ),
                        ),
                    ),
            )

            // Last 3 characters (Indices 3, 4, 5: Violet to Magenta)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                group2.forEachIndexed { idx, char ->
                    val globalIdx = idx + 3
                    CodeTile(
                        char = char.toString(),
                        accentColor = CodeTileGradientColors[globalIdx],
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeTile(
    char: String,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.28f),
                        Color(0x18101426),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.95f), // Top glow of its gradient color
                            accentColor.copy(alpha = 0.20f), // Fades softly towards the bottom
                        ),
                    ),
                ),
                RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InstructionStep(
    number: String,
    text: String,
    accentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.20f))
                .border(1.dp, accentColor.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false,
                    ),
                ),
            )
        }
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
        )
    }
}

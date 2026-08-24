package dev.vantafyn.tv.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Phonelink
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.vantafynTvFocusable

@Composable
fun TvSetupMethodScreen(
    onManualSetup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPairingInfo by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showPairingInfo) {
            val shape = RoundedCornerShape(22.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .clip(shape)
                    .background(Color(0xD910182A))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape)
                    .padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x228EA2FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = VantafynColors.Primary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Mobile Pairing",
                    color = VantafynColors.Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Seamless phone-to-TV QR pairing is coming in a future update.\n\nFor now, please sign in manually on this TV.",
                    color = VantafynColors.Muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    VantafynTvGlassButton(
                        text = "Sign In Manually",
                        isPrimary = true,
                        onClick = onManualSetup,
                    )
                    VantafynTvGlassButton(
                        text = "Back",
                        isPrimary = false,
                        onClick = { showPairingInfo = false },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
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
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose how you'd like to connect this TV to your Jellyfin server",
                    color = VantafynColors.Muted,
                    fontSize = 15.sp,
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Options Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SetupOptionCard(
                        title = "Pair with mobile app",
                        subtitle = "Use your phone to connect this TV.",
                        badge = "Future",
                        icon = Icons.Rounded.Phonelink,
                        onClick = { showPairingInfo = true },
                    )

                    SetupOptionCard(
                        title = "Sign in manually",
                        subtitle = "Enter your Jellyfin server address and credentials.",
                        icon = Icons.Rounded.Tune,
                        onClick = onManualSetup,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                VantafynTvGlassButton(
                    text = "Back",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    isPrimary = false,
                    onClick = onBack,
                )

                // Bottom clearance for scaled button focus borders
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun SetupOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    Box(
        modifier = modifier
            .width(280.dp)
            .height(180.dp)
            .vantafynTvFocusable(interactionSource = interactionSource, shape = shape)
            .clip(shape)
            .background(Color(0xD910182A))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x228EA2FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VantafynColors.Primary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                if (!badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x338EA2FF))
                            .border(1.dp, Color(0x668EA2FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badge,
                            color = VantafynColors.Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    color = if (isFocused) VantafynColors.Ink else Color(0xFFD4DBEE),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = VantafynColors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

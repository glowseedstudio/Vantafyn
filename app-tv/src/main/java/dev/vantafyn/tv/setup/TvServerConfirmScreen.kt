package dev.vantafyn.tv.setup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPublicUser
import dev.vantafyn.core.jellyfin.JellyfinServerConfig
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton

@Composable
fun TvServerConfirmScreen(
    server: JellyfinServerConfig?,
    hasPublicUsers: Boolean,
    publicUsers: List<JellyfinPublicUser> = emptyList(),
    savedProfiles: List<SavedProfile> = emptyList(),
    currentSessionUser: String? = null,
    isQuickConnectMode: Boolean = false,
    onContinue: () -> Unit,
    onQuickConnect: (() -> Unit)? = null,
    onUseDifferentServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            VantafynLogoBadge(
                size = 56.dp,
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Server Found",
                color = VantafynColors.Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Successfully connected to your Jellyfin instance",
                color = VantafynColors.Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glass Server Card with Animated Gradient Border Sweep
            val borderAlpha = remember { Animatable(0f) }
            val infiniteTransition = rememberInfiniteTransition(label = "serverBorderSweep")
            val borderShiftState = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 5_200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "serverBorderShift",
            )

            LaunchedEffect(Unit) {
                borderAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = FastOutSlowInEasing),
                )
            }

            // Server Admin Avatar Resolution (matching mobile app)
            val adminUser = publicUsers.firstOrNull { it.isAdministrator && it.imageUrl != null }
                ?: publicUsers.firstOrNull { it.isAdministrator }
                ?: publicUsers.firstOrNull { it.displayName == currentSessionUser && it.imageUrl != null }
                ?: publicUsers.firstOrNull { user -> savedProfiles.any { it.displayName == user.displayName } && user.imageUrl != null }
                ?: publicUsers.firstOrNull { it.imageUrl != null }
                ?: publicUsers.firstOrNull()

            val avatarUrl = adminUser?.imageUrl ?: adminUser?.id?.let { userId ->
                server?.url?.let { "${it.trimEnd('/')}/Users/$userId/Images/Primary" }
            }

            val shape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxWidth()
                    .drawBehind {
                        val stroke = 1.5.dp.toPx()
                        val shift = borderShiftState.value
                        val colors = listOf(
                            Color(0xFF21D8FF).copy(alpha = borderAlpha.value * 0.85f),
                            Color(0xFF388BFF).copy(alpha = borderAlpha.value * 0.70f),
                            Color(0xFF8B35FF).copy(alpha = borderAlpha.value * 0.75f),
                            Color(0xFFFF36C7).copy(alpha = borderAlpha.value * 0.85f),
                            Color(0xFF21D8FF).copy(alpha = borderAlpha.value * 0.85f),
                        )
                        val stops = floatArrayOf(
                            ((0f + shift) % 1f),
                            ((0.25f + shift) % 1f),
                            ((0.5f + shift) % 1f),
                            ((0.75f + shift) % 1f),
                            ((1f + shift) % 1f),
                        ).sortedArray()

                        val sortedColors = stops.mapIndexed { idx, _ ->
                            colors[idx % colors.size]
                        }

                        drawRoundRect(
                            brush = Brush.sweepGradient(sortedColors),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                        )
                    }
                    .clip(shape)
                    .background(Color(0x33121721))
                    .border(
                        BorderStroke(1.dp, Color(0x33FFFFFF)),
                        shape = shape,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3D5266), Color(0xFF756A8A), Color(0xFF20252D))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = adminUser?.displayName ?: server?.name ?: "Server Admin",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            val initial = (adminUser?.displayName?.take(1) ?: server?.name?.take(1) ?: "J").uppercase()
                            Text(
                                text = initial,
                                color = VantafynColors.Ink,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = server?.name?.ifBlank { "Jellyfin Server" } ?: "Jellyfin Server",
                            color = VantafynColors.Ink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = server?.url.orEmpty(),
                            color = VantafynColors.Muted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VantafynTvGlassButton(
                    text = if (isQuickConnectMode) "Quick Connect" else (if (hasPublicUsers) "Choose Profile" else "Sign In"),
                    icon = if (isQuickConnectMode) Icons.Rounded.Bolt else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    isPrimary = true,
                    illuminatedPrimary = true,
                    onClick = onContinue,
                )

                if (!isQuickConnectMode && onQuickConnect != null) {
                    VantafynTvGlassButton(
                        text = "Quick Connect",
                        icon = Icons.Rounded.Bolt,
                        isPrimary = true,
                        illuminatedPrimary = true,
                        onClick = onQuickConnect,
                    )
                }

                VantafynTvGlassButton(
                    text = "Different Server",
                    icon = Icons.Rounded.Refresh,
                    isPrimary = true,
                    illuminatedPrimary = true,
                    onClick = onUseDifferentServer,
                )
            }

            // Bottom clearance for scaled button focus borders
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

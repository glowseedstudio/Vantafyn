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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onContinue: () -> Unit,
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
            modifier = Modifier.fillMaxWidth(0.6f),
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
            val borderShift by infiniteTransition.animateFloat(
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
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        val alpha = borderAlpha.value
                        if (alpha > 0f) {
                            val r = 18.dp.toPx()
                            val s = Offset(-size.width * borderShift, -size.height * borderShift)
                            val e = Offset(size.width * (1f - borderShift), size.height * (1f - borderShift))
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = (VantafynGradients.AccentColors + VantafynGradients.AccentColors.first())
                                        .map { it.copy(alpha = it.alpha * alpha * 0.85f) },
                                    start = s,
                                    end = e,
                                    tileMode = TileMode.Repeated,
                                ),
                                cornerRadius = CornerRadius(r, r),
                                style = Stroke(width = 1.5.dp.toPx()),
                            )
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Color(0xD910182A))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape)
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Admin Avatar Picture / Initials Fallback
                    val avatarShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(avatarShape)
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
                    text = if (hasPublicUsers) "Choose Profile" else "Sign In",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    isPrimary = true,
                    onClick = onContinue,
                )

                VantafynTvGlassButton(
                    text = "Different Server",
                    icon = Icons.Rounded.Refresh,
                    isPrimary = false,
                    onClick = onUseDifferentServer,
                )
            }

            // Bottom clearance for scaled button focus borders
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

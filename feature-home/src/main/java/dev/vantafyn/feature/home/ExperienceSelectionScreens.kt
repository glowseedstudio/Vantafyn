package dev.vantafyn.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.experience.ExperienceMode
import dev.vantafyn.core.experience.MusicBackendType
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynSetupHeader
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.feature.home.auth.VantafynHomeUiState

@Composable
fun ExperienceSelectionScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onSelectExperience: (ExperienceMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SetupBackScaffold(onBack = onBack, showBack = true) {
            CenterPane {
                SetupMaterialize(delayMillis = 0) {
                    VantafynSetupHeader(
                        title = "Choose Your Experience",
                        subtitle = null,
                        tv = tv,
                    )
                }
                Spacer(Modifier.height(VantafynSpacing.xl))
                SetupMaterialize(delayMillis = 180, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ExperienceCard(
                            title = "Full Media Experience",
                            description = "Movies, TV Series, Music, Live TV, Collections, and WatchParty powered by your Jellyfin server.",
                            icon = Icons.Rounded.Movie,
                            accentGradient = listOf(Color(0xFF3880FF), Color(0xFF6B42FF)),
                            onClick = { onSelectExperience(ExperienceMode.FullMedia) },
                        )

                        ExperienceCard(
                            title = "Music Experience",
                            description = "A dedicated music player for streaming your library via OpenSubsonic, Navidrome, Gonic, LMS, or Jellyfin.",
                            icon = Icons.Rounded.GraphicEq,
                            accentGradient = listOf(Color(0xFF9D4EDD), Color(0xFFFF5470)),
                            onClick = { onSelectExperience(ExperienceMode.MusicOnly) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MusicBackendSelectionScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onSelectBackend: (MusicBackendType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SetupBackScaffold(onBack = onBack, showBack = true) {
            CenterPane {
                SetupMaterialize(delayMillis = 0) {
                    VantafynSetupHeader(
                        title = "Select Music Provider",
                        subtitle = null,
                        tv = tv,
                    )
                }
                Spacer(Modifier.height(VantafynSpacing.xl))
                SetupMaterialize(delayMillis = 180, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ExperienceCard(
                            title = "OpenSubsonic API",
                            description = "Connect directly to any server supporting OpenSubsonic or Subsonic REST (Navidrome, Gonic, LMS, etc.).",
                            icon = Icons.Rounded.LibraryMusic,
                            accentGradient = listOf(Color(0xFF00B4D8), Color(0xFF7209B7)),
                            onClick = { onSelectBackend(MusicBackendType.OpenSubsonic) },
                        )

                        ExperienceCard(
                            title = "Jellyfin Music",
                            description = "Connect to your Jellyfin server, streamlined exclusively for music browsing and playback.",
                            icon = Icons.Rounded.Cloud,
                            accentGradient = listOf(Color(0xFF4361EE), Color(0xFF3A0CA3)),
                            onClick = { onSelectBackend(MusicBackendType.Jellyfin) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectSubsonicScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onServerUrlChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SetupBackScaffold(onBack = onBack, showBack = true) {
            CenterPane {
                SetupMaterialize(delayMillis = 0) {
                    VantafynSetupHeader(
                        title = "Connect to Subsonic",
                        subtitle = null,
                        tv = tv,
                    )
                }
                Spacer(Modifier.height(VantafynSpacing.lg))
                SetupMaterialize(delayMillis = 180, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        VantafynTextField(
                            value = state.subsonicServerUrl,
                            onValueChange = onServerUrlChanged,
                            label = "Server URL",
                            placeholder = "https://music.example.com",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )

                        VantafynTextField(
                            value = state.subsonicUsername,
                            onValueChange = onUsernameChanged,
                            label = "Username",
                            placeholder = "Username",
                        )

                        VantafynTextField(
                            value = state.subsonicPassword,
                            onValueChange = onPasswordChanged,
                            label = "Password or App Token",
                            placeholder = "••••••••",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )

                        if (!state.subsonicErrorMessage.isNullOrBlank()) {
                            Text(
                                text = state.subsonicErrorMessage,
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        Spacer(Modifier.height(VantafynSpacing.sm))

                        if (state.isSubsonicConnecting) {
                            CircularProgressIndicator(
                                color = VantafynColors.Primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp),
                            )
                        } else {
                            VantafynButton(
                                text = "Connect & Stream",
                                onClick = onConnect,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperienceCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentGradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "experience_card_scale",
    )

    VantafynGlassCard(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(accentGradient.map { it.copy(alpha = 0.22f) }))
                    .border(
                        BorderStroke(1.dp, Brush.linearGradient(accentGradient.map { it.copy(alpha = 0.65f) })),
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )

                Text(
                    text = description,
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

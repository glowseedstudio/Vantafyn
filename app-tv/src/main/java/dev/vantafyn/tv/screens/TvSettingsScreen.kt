package dev.vantafyn.tv.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvScreenScaffold
import dev.vantafyn.tv.components.VantafynTvSectionHeader
import dev.vantafyn.tv.components.vantafynTvFocusable

@Composable
fun TvSettingsScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    onSelectProfile: (SavedProfile) -> Unit = {},
    onLogout: () -> Unit = {},
) {
    VantafynTvScreenScaffold(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Section 1: User Profiles
            item {
                Column {
                    VantafynTvSectionHeader(
                        title = "Switch Profile",
                        subtitle = "Select a saved user profile on this server",
                    )

                    if (state.savedProfiles.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(state.savedProfiles, key = { it.id }) { profile ->
                                val isCurrent = profile.jellyfinUserId == session?.user?.id
                                val avatarUrl = profile.imageUrl ?: session?.server?.url?.let {
                                    "${it.trimEnd('/')}/Users/${profile.jellyfinUserId}/Images/Primary"
                                }

                                val shape = RoundedCornerShape(16.dp)
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused = interactionSource.collectIsFocusedAsState().value

                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .vantafynTvFocusable(interactionSource = interactionSource, shape = shape)
                                        .clip(shape)
                                        .background(Color(0xD910182A))
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { onSelectProfile(profile) },
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF161C2E))
                                                .border(
                                                    1.5.dp,
                                                    if (isCurrent) VantafynColors.Primary else Color.White.copy(alpha = 0.20f),
                                                    CircleShape,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            AsyncImage(
                                                model = avatarUrl,
                                                contentDescription = profile.displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(56.dp),
                                            )
                                            if (avatarUrl.isNullOrBlank()) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AccountCircle,
                                                    contentDescription = null,
                                                    tint = VantafynColors.Ink,
                                                    modifier = Modifier.size(36.dp),
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = profile.displayName,
                                            color = if (isFocused) VantafynColors.Ink else Color(0xFFD4DBEE),
                                            fontSize = 14.sp,
                                            fontWeight = if (isFocused || isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        )

                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x338EA2FF))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    color = VantafynColors.Primary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Server Info Card
            item {
                Column {
                    VantafynTvSectionHeader(
                        title = "Server Information",
                        subtitle = "Current Jellyfin connection details",
                    )

                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .clip(shape)
                            .background(Color(0xD910182A))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape)
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x228EA2FF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Dns,
                                    contentDescription = null,
                                    tint = VantafynColors.Primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = session?.server?.name?.ifBlank { "Jellyfin Server" } ?: "Jellyfin Server",
                                    color = VantafynColors.Ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = session?.server?.url.orEmpty(),
                                    color = VantafynColors.Muted,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: App Information
            item {
                Column {
                    VantafynTvSectionHeader(
                        title = "About Vantafyn TV",
                        subtitle = "Client version and build information",
                    )

                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .clip(shape)
                            .background(Color(0xD910182A))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape)
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x228EA2FF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tv,
                                    contentDescription = null,
                                    tint = VantafynColors.Primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Vantafyn for Android TV",
                                    color = VantafynColors.Ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Native 10-foot media experience",
                                    color = VantafynColors.Muted,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Sign Out / Account Action
            item {
                Column {
                    VantafynTvSectionHeader(
                        title = "Session",
                    )

                    VantafynTvGlassButton(
                        text = "Sign Out",
                        icon = Icons.AutoMirrored.Rounded.ExitToApp,
                        isPrimary = false,
                        onClick = onLogout,
                    )
                }
            }
        }
    }
}

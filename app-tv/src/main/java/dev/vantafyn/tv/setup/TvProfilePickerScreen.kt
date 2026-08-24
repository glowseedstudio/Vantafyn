package dev.vantafyn.tv.setup

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import dev.vantafyn.tv.components.vantafynTvFocusable

@Composable
fun TvProfilePickerScreen(
    server: JellyfinServerConfig?,
    savedProfiles: List<SavedProfile>,
    publicUsers: List<JellyfinPublicUser>,
    onSelectSavedProfile: (SavedProfile) -> Unit,
    onSelectPublicUser: (JellyfinPublicUser) -> Unit,
    onAddProfile: () -> Unit,
    onChangeServer: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 48.dp),
            ) {
                VantafynLogoBadge(
                    size = 56.dp,
                    shape = RoundedCornerShape(16.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Who's Watching?",
                    color = VantafynColors.Ink,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select your profile on ${server?.name?.ifBlank { "Jellyfin" } ?: "Jellyfin"}",
                    color = VantafynColors.Muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Profile Cards Horizontal Scroller with Premium Edge Fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fadingHorizontalEdges(fadeWidth = 72.dp),
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(horizontal = 64.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 1. Saved Profiles
                    if (savedProfiles.isNotEmpty()) {
                        items(savedProfiles, key = { "saved_${it.id}" }) { profile ->
                            val avatarUrl = profile.imageUrl ?: server?.url?.let {
                                "${it.trimEnd('/')}/Users/${profile.jellyfinUserId}/Images/Primary"
                            }
                            ProfileCard(
                                displayName = profile.displayName,
                                avatarUrl = avatarUrl,
                                hasPassword = false,
                                onClick = { onSelectSavedProfile(profile) },
                            )
                        }
                    } else if (publicUsers.isNotEmpty()) {
                        // 2. Public Users on Server
                        items(publicUsers, key = { "public_${it.id}" }) { user ->
                            val avatarUrl = user.imageUrl ?: server?.url?.let {
                                "${it.trimEnd('/')}/Users/${user.id}/Images/Primary"
                            }
                            ProfileCard(
                                displayName = user.displayName,
                                avatarUrl = avatarUrl,
                                hasPassword = user.hasPassword,
                                onClick = { onSelectPublicUser(user) },
                            )
                        }
                    }

                    // 3. Add Profile Card
                    item(key = "add_profile") {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused = interactionSource.collectIsFocusedAsState().value
                        val shape = CircleShape

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(130.dp)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onAddProfile,
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(108.dp)
                                    .vantafynTvFocusable(
                                        interactionSource = interactionSource,
                                        shape = shape,
                                        scaleFocused = 1.08f,
                                    )
                                    .clip(shape)
                                    .background(Color(0xD910182A)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add Profile",
                                    tint = VantafynColors.Primary,
                                    modifier = Modifier.size(36.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Add Profile",
                                color = if (isFocused) VantafynColors.Ink else Color(0xFFD4DBEE),
                                fontSize = 14.sp,
                                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(42.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 48.dp),
            ) {
                VantafynTvGlassButton(
                    text = "Change Server",
                    icon = Icons.Rounded.Refresh,
                    isPrimary = false,
                    onClick = onChangeServer,
                )

                if (onBack != null) {
                    VantafynTvGlassButton(
                        text = "Back",
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        isPrimary = false,
                        onClick = onBack,
                    )
                }
            }

            // Bottom clearance for scaled button focus borders
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

private fun Modifier.fadingHorizontalEdges(
    fadeWidth: Dp = 72.dp,
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadeWidthPx = fadeWidth.toPx()
        // Left edge fade out to transparent
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = 0f,
                endX = fadeWidthPx,
            ),
            blendMode = BlendMode.DstIn,
        )
        // Right edge fade out to transparent
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = size.width - fadeWidthPx,
                endX = size.width,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
private fun ProfileCard(
    displayName: String,
    avatarUrl: String?,
    hasPassword: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val shape = CircleShape

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(130.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
    ) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .vantafynTvFocusable(
                    interactionSource = interactionSource,
                    shape = shape,
                    scaleFocused = 1.08f,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Circular Avatar Picture / Initials
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
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
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = displayName.take(2).uppercase(),
                        color = VantafynColors.Ink,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // Gradient Lock Badge (Unclipped, sitting cleanly on top)
            if (hasPassword) {
                val lockShape = CircleShape
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(26.dp)
                        .clip(lockShape)
                        .background(
                            Brush.linearGradient(
                                colors = VantafynGradients.AccentColors,
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.50f), lockShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Password Required",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = displayName,
            color = if (isFocused) VantafynColors.Ink else Color(0xFFD4DBEE),
            fontSize = 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

package dev.vantafyn.tv.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
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
import dev.vantafyn.tv.components.VantafynFocusGradientColors
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvSetupCinematicEasing
import kotlinx.coroutines.delay

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
    var isRevealed by remember { mutableStateOf(false) }
    val firstProfileRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isRevealed = true
        delay(120)
        try {
            firstProfileRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // 1. Top Header staggered reveal (0ms delay)
    val headerAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "profileHeaderAlpha",
    )
    val headerTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 32f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 0, easing = VantafynTvSetupCinematicEasing),
        label = "profileHeaderTranslationY",
    )

    // 2. Profile Row staggered reveal (180ms delay)
    val rowAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 180, easing = VantafynTvSetupCinematicEasing),
        label = "profileRowAlpha",
    )
    val rowTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 28f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 180, easing = VantafynTvSetupCinematicEasing),
        label = "profileRowTranslationY",
    )

    // 3. Bottom Actions staggered reveal (360ms delay)
    val actionsAlpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 360, easing = VantafynTvSetupCinematicEasing),
        label = "profileActionsAlpha",
    )
    val actionsTranslationY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 20f,
        animationSpec = tween(durationMillis = 1_500, delayMillis = 360, easing = VantafynTvSetupCinematicEasing),
        label = "profileActionsTranslationY",
    )

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
            // Header Group (0ms delay)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .graphicsLayer {
                        alpha = headerAlpha
                        translationY = headerTranslationY
                    },
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
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Profile cards stay centred for small households and scroll only when needed.
            val profileCount = (if (savedProfiles.isNotEmpty()) savedProfiles.size else publicUsers.size) + 1
            val useCenteredProfiles = profileCount <= 4
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (useCenteredProfiles) Modifier else Modifier.fadingHorizontalEdges(fadeWidth = 72.dp))
                    .graphicsLayer {
                        alpha = rowAlpha
                        translationY = rowTranslationY
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (useCenteredProfiles) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 64.dp, vertical = 6.dp),
                    ) {
                        if (savedProfiles.isNotEmpty()) {
                            savedProfiles.forEachIndexed { index, profile ->
                                val avatarUrl = profile.imageUrl ?: server?.url?.let {
                                    "${it.trimEnd('/')}/Users/${profile.jellyfinUserId}/Images/Primary"
                                }
                                ProfileCard(
                                    displayName = profile.displayName,
                                    avatarUrl = avatarUrl,
                                    hasPassword = false,
                                    onClick = { onSelectSavedProfile(profile) },
                                    modifier = if (index == 0) Modifier.focusRequester(firstProfileRequester) else Modifier,
                                )
                            }
                        } else if (publicUsers.isNotEmpty()) {
                            publicUsers.forEachIndexed { index, user ->
                                val avatarUrl = user.imageUrl ?: server?.url?.let {
                                    "${it.trimEnd('/')}/Users/${user.id}/Images/Primary"
                                }
                                ProfileCard(
                                    displayName = user.displayName,
                                    avatarUrl = avatarUrl,
                                    hasPassword = user.hasPassword,
                                    onClick = { onSelectPublicUser(user) },
                                    modifier = if (index == 0) Modifier.focusRequester(firstProfileRequester) else Modifier,
                                )
                            }
                        }
                        AddProfileCard(
                            onClick = onAddProfile,
                            modifier = if (savedProfiles.isEmpty() && publicUsers.isEmpty()) {
                                Modifier.focusRequester(firstProfileRequester)
                            } else {
                                Modifier
                            },
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(horizontal = 64.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (savedProfiles.isNotEmpty()) {
                            itemsIndexed(savedProfiles, key = { _, it -> "saved_${it.id}" }) { index, profile ->
                                val avatarUrl = profile.imageUrl ?: server?.url?.let {
                                    "${it.trimEnd('/')}/Users/${profile.jellyfinUserId}/Images/Primary"
                                }
                                ProfileCard(
                                    displayName = profile.displayName,
                                    avatarUrl = avatarUrl,
                                    hasPassword = false,
                                    onClick = { onSelectSavedProfile(profile) },
                                    modifier = if (index == 0) Modifier.focusRequester(firstProfileRequester) else Modifier,
                                )
                            }
                        } else if (publicUsers.isNotEmpty()) {
                            itemsIndexed(publicUsers, key = { _, it -> "public_${it.id}" }) { index, user ->
                                val avatarUrl = user.imageUrl ?: server?.url?.let {
                                    "${it.trimEnd('/')}/Users/${user.id}/Images/Primary"
                                }
                                ProfileCard(
                                    displayName = user.displayName,
                                    avatarUrl = avatarUrl,
                                    hasPassword = user.hasPassword,
                                    onClick = { onSelectPublicUser(user) },
                                    modifier = if (index == 0) Modifier.focusRequester(firstProfileRequester) else Modifier,
                                )
                            }
                        }

                        item(key = "add_profile") {
                            AddProfileCard(onClick = onAddProfile)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(42.dp))

            // Action Buttons (360ms delay)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .graphicsLayer {
                        alpha = actionsAlpha
                        translationY = actionsTranslationY
                    },
            ) {
                VantafynTvGlassButton(
                    text = "Change Server",
                    icon = Icons.Rounded.Refresh,
                    isPrimary = true,
                    illuminatedPrimary = true,
                    modifier = Modifier.focusProperties {
                        up = firstProfileRequester
                    },
                    onClick = onChangeServer,
                )

                if (onBack != null) {
                    VantafynTvGlassButton(
                        text = "Back",
                        icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        isPrimary = true,
                        illuminatedPrimary = true,
                        modifier = Modifier.focusProperties {
                            up = firstProfileRequester
                        },
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
private fun AddProfileCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val shape = CircleShape

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "AddProfileScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(130.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
    ) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .scale(scale)
                .clip(shape)
                .background(Color(0xD910182A))
                .then(
                    if (isFocused) {
                        Modifier.border(
                            BorderStroke(2.5.dp, Brush.horizontalGradient(VantafynFocusGradientColors)),
                            shape,
                        )
                    } else {
                        Modifier.border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            shape,
                        )
                    },
                ),
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

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "ProfileCardScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(130.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
    ) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .scale(scale),
            contentAlignment = Alignment.Center,
        ) {
            // Circular Avatar Picture / Initials with Focus Ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3D5266), Color(0xFF756A8A), Color(0xFF20252D))
                        )
                    )
                    .then(
                        if (isFocused) {
                            Modifier.border(
                                BorderStroke(2.5.dp, Brush.horizontalGradient(VantafynFocusGradientColors)),
                                shape,
                            )
                        } else {
                            Modifier.border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                shape,
                            )
                        }
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

            // Gradient Lock Badge (Rendered ON TOP of avatar and focus ring)
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

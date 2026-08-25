package dev.vantafyn.tv.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.auth.BottomRailAccent
import dev.vantafyn.tv.nav.TvNavigationItem
import dev.vantafyn.tv.nav.TvRoute
import dev.vantafyn.tv.nav.TvSettingsCategory
import kotlinx.coroutines.delay

@Composable
fun VantafynTvSidebar(
    mainItems: List<TvNavigationItem>,
    bottomItems: List<TvNavigationItem>,
    currentRoute: TvRoute,
    isExpanded: Boolean,
    accentMode: BottomRailAccent,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    settingsMode: Boolean = false,
    settingsItems: List<TvNavigationItem> = emptyList(),
    selectedSettingsCategory: TvSettingsCategory = TvSettingsCategory.Appearance,
    onSettingsCategorySelected: (TvSettingsCategory) -> Unit = {},
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {},
    onRouteSelected: (TvRoute) -> Unit = {},
    onProfileClicked: () -> Unit = {},
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 240.dp else 72.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "TvSidebarWidth",
    )
    val sidebarFrostAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 260f),
        label = "TvSidebarFrostAlpha",
    )

    // Full-bleed artwork remains visible, but expanded navigation gets enough frost for readable text.
    val backgroundBrush = Brush.horizontalGradient(
        listOf(
            Color(0x38000000).copy(alpha = 0.22f + (0.46f * sidebarFrostAlpha)),
            Color(0x18000000).copy(alpha = 0.10f + (0.30f * sidebarFrostAlpha)),
        )
    )

    val shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    val accentGradient = VantafynGradients.accentHorizontal()
    val ambientTransition = rememberInfiniteTransition(label = "tvSideRailAccent")
    val breathingAlpha by ambientTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(tween(5_800, easing = LinearEasing), RepeatMode.Reverse),
        label = "tvSideRailBreathingAlpha",
    )
    val sideRailAccentAlpha = when (accentMode) {
        BottomRailAccent.Off -> 0f
        BottomRailAccent.StillGlow -> 0.16f
        BottomRailAccent.Breathing -> breathingAlpha
        BottomRailAccent.TouchRipple -> if (isExpanded) 0.22f else 0.13f
    }
    val homeFocusRequester = remember { FocusRequester() }
    var initialHomeFocusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute, mainItems) {
        if (!initialHomeFocusRequested && currentRoute == TvRoute.Home && mainItems.any { it.route == TvRoute.Home }) {
            delay(120)
            homeFocusRequester.requestFocus()
            initialHomeFocusRequested = true
        }
    }

    Box(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .drawBehind {
                if (sideRailAccentAlpha > 0f) {
                    drawRoundRect(
                        brush = accentGradient,
                        alpha = sideRailAccentAlpha,
                        topLeft = Offset(-18.dp.toPx(), 10.dp.toPx()),
                        size = Size(size.width + 34.dp.toPx(), size.height - 20.dp.toPx()),
                        cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                    )
                    drawRoundRect(
                        brush = accentGradient,
                        alpha = sideRailAccentAlpha * 0.32f,
                        topLeft = Offset(size.width - 2.dp.toPx(), 24.dp.toPx()),
                        size = Size(6.dp.toPx(), size.height - 48.dp.toPx()),
                        cornerRadius = CornerRadius(999.dp.toPx(), 999.dp.toPx()),
                    )
                }
            }
            .clip(shape)
            .background(backgroundBrush)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.045f * sidebarFrostAlpha),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.18f * sidebarFrostAlpha),
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = shape,
            )
            .padding(vertical = 16.dp, horizontal = 10.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (!isExpanded) {
                                onExpand()
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionRight -> {
                            if (isExpanded) {
                                onCollapse()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }
            }
            .focusGroup()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // --- UPPER SECTION (Profile + 3 Action Buttons + Divider + Scrollable Main Navigation) ---
            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                // --- 1. TOP: USER PROFILE AVATAR AREA (FOCUSABLE) ---
                if (session != null) {
                    val profileInteractionSource = remember { MutableInteractionSource() }
                    val isProfileFocused by profileInteractionSource.collectIsFocusedAsState()
                    val avatarUrl = "${session.server.url.trimEnd('/')}/Users/${session.user.id}/Images/Primary"
                    val profileShape = RoundedCornerShape(14.dp)
                    val serverSubtitle = session.server.name
                        ?: session.user.serverName
                        ?: if (session.user.isAdministrator) "Administrator" else "Member"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(profileShape)
                            .background(if (isProfileFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                            .then(
                                if (isProfileFocused) {
                                    Modifier.border(BorderStroke(1.5.dp, accentGradient), profileShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = profileInteractionSource,
                                indication = null,
                                onClick = onProfileClicked,
                            )
                            .focusable(interactionSource = profileInteractionSource)
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (isProfileFocused) VantafynColors.Primary else Color.White.copy(alpha = 0.20f), CircleShape)
                                .background(Color.Black.copy(alpha = 0.40f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = session.user.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp),
                            )
                            if (avatarUrl.isBlank()) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = VantafynColors.Ink,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(animationSpec = spring(stiffness = 600f)),
                            exit = fadeOut(animationSpec = spring(stiffness = 600f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp),
                            ) {
                                Text(
                                    text = session.user.name,
                                    color = VantafynColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = serverSubtitle,
                                    color = VantafynColors.Muted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- 2. ACTION BUTTONS RAIL (SEARCH, NOTIFICATIONS, EXIT) ---
                if (isExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        bottomItems.forEach { item ->
                            val isSelected = when (item.route) {
                                TvRoute.Search -> currentRoute == TvRoute.Search
                                TvRoute.Notifications -> currentRoute == TvRoute.Notifications
                                TvRoute.Settings -> currentRoute == TvRoute.Settings
                                else -> false
                            }

                            VantafynTvSidebarIconButton(
                                icon = item.icon,
                                label = item.label,
                                isSelected = isSelected,
                                badgeCount = item.badgeCount,
                                onFocused = {},
                                onClick = {
                                    if (item.onClick != null) item.onClick.invoke()
                                    else onRouteSelected(item.route)
                                },
                            )
                        }
                    }
                } else {
                    // Collapsed state: Render single search icon (1 row)
                    val searchItem = bottomItems.firstOrNull { it.route == TvRoute.Search } ?: bottomItems.first()
                    VantafynTvSidebarItem(
                        label = searchItem.label,
                        icon = searchItem.icon,
                        isSelected = currentRoute == TvRoute.Search,
                        isExpanded = false,
                        badgeCount = searchItem.badgeCount,
                        onFocused = {},
                        onClick = {
                            if (searchItem.onClick != null) searchItem.onClick.invoke()
                            else onRouteSelected(searchItem.route)
                        },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtle glass divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 6.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )

                Spacer(modifier = Modifier.height(6.dp))

                // --- 3. MAIN NAVIGATION ---
                // Matches the mobile social rail mode swap: the whole icon set fades and slides as one.
                AnimatedContent(
                    targetState = settingsMode,
                    transitionSpec = {
                        val enterTransition = fadeIn(animationSpec = tween(200, delayMillis = 30, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                            slideInVertically(animationSpec = tween(200, delayMillis = 30, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 5 }
                        val exitTransition = fadeOut(animationSpec = tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                            slideOutVertically(animationSpec = tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 5 }
                        (enterTransition togetherWith exitTransition).using(SizeTransform(clip = false))
                    },
                    label = "tvSidebarContentMode",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { isSettingsMode ->
                    val visibleMainItems = if (isSettingsMode) settingsItems else mainItems
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(visibleMainItems, key = { item -> "${isSettingsMode}_${item.route}_${item.label}" }) { item ->
                            val category = TvSettingsCategory.entries.firstOrNull { it.label == item.label }
                            val isSelected = if (isSettingsMode) {
                                category == selectedSettingsCategory
                            } else {
                                when (item.route) {
                                    TvRoute.Home -> currentRoute == TvRoute.Home
                                    TvRoute.Favorites -> currentRoute == TvRoute.Favorites
                                    is TvRoute.Library -> (currentRoute is TvRoute.Library) && (currentRoute.parentId == item.route.parentId)
                                    TvRoute.Search -> currentRoute == TvRoute.Search
                                    TvRoute.Music -> currentRoute == TvRoute.Music
                                    TvRoute.Requests -> currentRoute == TvRoute.Requests
                                    TvRoute.Admin -> currentRoute == TvRoute.Admin
                                    TvRoute.Settings -> currentRoute == TvRoute.Settings
                                    TvRoute.Notifications -> currentRoute == TvRoute.Notifications
                                    else -> false
                                }
                            }

                            VantafynTvSidebarItem(
                                label = item.label,
                                icon = item.icon,
                                isSelected = isSelected,
                                isExpanded = isExpanded,
                                badgeCount = item.badgeCount,
                                modifier = if (item.route == TvRoute.Home) Modifier.focusRequester(homeFocusRequester) else Modifier,
                                onFocused = {},
                                onClick = {
                                    if (isSettingsMode && category != null) {
                                        onSettingsCategorySelected(category)
                                    } else if (item.onClick != null) {
                                        item.onClick.invoke()
                                    } else {
                                        onRouteSelected(item.route)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

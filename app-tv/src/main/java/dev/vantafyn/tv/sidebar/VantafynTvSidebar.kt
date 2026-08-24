package dev.vantafyn.tv.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.nav.TvNavigationItem
import dev.vantafyn.tv.nav.TvRoute

@Composable
fun VantafynTvSidebar(
    items: List<TvNavigationItem>,
    currentRoute: TvRoute,
    isExpanded: Boolean,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {},
    onRouteSelected: (TvRoute) -> Unit = {},
    onProfileClicked: () -> Unit = {},
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 240.dp else 72.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
        label = "TvSidebarWidth",
    )

    // Premium dark navy glass backdrop
    val backgroundBrush = Brush.horizontalGradient(
        listOf(
            Color(0xF0070A12),
            Color(0xD910182A),
        )
    )

    val shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    val accentGradient = VantafynGradients.accentHorizontal()

    Box(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = shape,
            )
            .padding(vertical = 20.dp, horizontal = 10.dp)
            .focusGroup()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
        ) {
            // --- 1. TOP: USER PROFILE AVATAR AREA (FOCUSABLE) ---
            if (session != null) {
                val profileInteractionSource = remember { MutableInteractionSource() }
                val isProfileFocused by profileInteractionSource.collectIsFocusedAsState()
                val avatarUrl = "${session.server.url.trimEnd('/')}/Users/${session.user.id}/Images/Primary"
                val profileShape = RoundedCornerShape(14.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(profileShape)
                        .background(if (isProfileFocused) Color(0x338EA2FF) else Color.Transparent)
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
                            .background(Color(0xFF161C2E)),
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
                                text = if (session.user.isAdministrator) "Administrator" else "Member",
                                color = VantafynColors.Primary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // --- 2. BRAND LOGO HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VantafynLogoBadge(
                    size = 36.dp,
                    shape = RoundedCornerShape(10.dp),
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = spring(stiffness = 600f)),
                    exit = fadeOut(animationSpec = spring(stiffness = 600f)),
                ) {
                    Text(
                        text = "VANTAFYN",
                        color = VantafynColors.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. NAVIGATION ITEMS ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items.forEach { item ->
                    val isSelected = when (item.route) {
                        TvRoute.Home -> currentRoute == TvRoute.Home
                        is TvRoute.Library -> currentRoute is TvRoute.Library
                        TvRoute.Search -> currentRoute == TvRoute.Search
                        TvRoute.Music -> currentRoute == TvRoute.Music
                        TvRoute.Requests -> currentRoute == TvRoute.Requests
                        TvRoute.Admin -> currentRoute == TvRoute.Admin
                        TvRoute.Settings -> currentRoute == TvRoute.Settings
                        else -> false
                    }

                    VantafynTvSidebarItem(
                        label = item.label,
                        icon = item.icon,
                        isSelected = isSelected,
                        isExpanded = isExpanded,
                        badgeCount = item.badgeCount,
                        onFocused = onExpand,
                        onClick = {
                            onRouteSelected(item.route)
                        },
                    )
                }
            }
        }
    }
}

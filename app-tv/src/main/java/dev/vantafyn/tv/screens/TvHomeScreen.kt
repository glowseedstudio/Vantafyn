package dev.vantafyn.tv.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.JellyfinMediaCardShape
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvFallbackHero
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvHero
import dev.vantafyn.tv.components.VantafynTvPosterCard
import dev.vantafyn.tv.components.VantafynTvSectionHeader
import dev.vantafyn.tv.components.VantafynTvWideCard
import java.util.UUID

@Composable
fun TvHomeScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    onOpenMedia: (UUID) -> Unit = {},
    onPlayMediaId: (UUID) -> Unit = {},
    onExploreLibraries: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    // --- 1. ERROR STATE (IF HOME FAILED TO LOAD AND IS EMPTY) ---
    if (state.homeErrorMessage != null && state.home == null && !state.isHomeLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val shape = RoundedCornerShape(24.dp)
            Column(
                modifier = Modifier
                    .clip(shape)
                    .background(Color(0xD910182A))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape)
                    .padding(horizontal = 48.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Couldn't load your home screen",
                    color = VantafynColors.Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please check your server connection and try again.",
                    color = VantafynColors.Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                VantafynTvGlassButton(
                    text = "Retry",
                    icon = Icons.Rounded.Refresh,
                    isPrimary = true,
                    onClick = onRefresh,
                )
            }
        }
        return
    }

    // --- 2. LOADING STATE (INITIAL LOAD WITH NO DATA) ---
    if (state.isHomeLoading && state.home == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    color = VantafynColors.Primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Loading your media library...",
                    color = VantafynColors.Muted,
                    fontSize = 15.sp,
                )
            }
        }
        return
    }

    val heroItem = state.home?.heroItems?.firstOrNull()
    val sections = state.home?.sections.orEmpty()
    val libraries = state.libraries

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // --- 3. CINEMATIC TV HERO (OR FALLBACK) ---
        item(key = "tv_hero_banner") {
            if (heroItem != null) {
                VantafynTvHero(
                    item = heroItem,
                    onPlay = { onPlayMediaId(it) },
                    onDetails = { onOpenMedia(it) },
                )
            } else {
                VantafynTvFallbackHero(
                    serverName = session?.server?.name,
                    onExplore = onExploreLibraries,
                )
            }
        }

        // --- 4. DYNAMIC HOME CAROUSELS ---
        sections.forEachIndexed { index, section ->
            if (section.items.isNotEmpty()) {
                item(key = "section_${section.title}_$index") {
                    Column {
                        VantafynTvSectionHeader(
                            title = section.title,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(end = 32.dp),
                        ) {
                            items(section.items, key = { it.id }) { item ->
                                val imgUrl = item.imageUrl ?: item.backdropUrl
                                if (item.shape == JellyfinMediaCardShape.Wide) {
                                    VantafynTvWideCard(
                                        title = item.title,
                                        imageUrl = imgUrl,
                                        subtitle = item.subtitle ?: item.year?.toString(),
                                        progressPercentage = item.progress?.times(100f),
                                        onClick = { onOpenMedia(item.id) },
                                    )
                                } else {
                                    VantafynTvPosterCard(
                                        title = item.title,
                                        imageUrl = imgUrl,
                                        subtitle = item.subtitle ?: item.year?.toString(),
                                        progressPercentage = item.progress?.times(100f),
                                        onClick = { onOpenMedia(item.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. MEDIA LIBRARIES CAROUSEL ---
        if (libraries.isNotEmpty()) {
            item(key = "row_libraries") {
                Column {
                    VantafynTvSectionHeader(
                        title = "Your Libraries",
                        subtitle = "Browse movies, TV shows, and media collections",
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 32.dp),
                    ) {
                        items(libraries, key = { it.id }) { lib ->
                            val imageUrl = lib.imageUrl ?: "${session?.server?.url?.trimEnd('/')}/Items/${lib.id}/Images/Primary"
                            VantafynTvWideCard(
                                title = lib.name,
                                imageUrl = imageUrl,
                                subtitle = lib.collectionType?.replaceFirstChar { it.uppercase() } ?: "Media",
                                onClick = { onOpenMedia(lib.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

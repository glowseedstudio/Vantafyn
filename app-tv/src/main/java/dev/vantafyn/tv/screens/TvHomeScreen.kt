package dev.vantafyn.tv.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinMediaCardShape
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvFallbackHero
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvPosterCard
import dev.vantafyn.tv.components.VantafynTvSectionHeader
import dev.vantafyn.tv.components.VantafynTvWideCard
import dev.vantafyn.tv.media.TvArtworkResolver
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

    val heroItems = state.home?.heroItems.orEmpty()
    val sections = state.home?.sections.orEmpty()
    val libraries = state.libraries
    val serverUrl = session?.server?.url
    val initialHero = heroItems.firstOrNull()?.toSpotlight(serverUrl)

    val spotlightState = rememberTvHomeSpotlightState(initialHero)

    LaunchedEffect(initialHero) {
        spotlightState.updateIfNull(initialHero)
    }

    val activeSpotlight = spotlightState.currentItem ?: initialHero
    val backdropUrl = activeSpotlight?.backdropUrl

    Box(modifier = modifier.fillMaxSize()) {
        // --- 1. PINNED TOP SPOTLIGHT BACKDROP LAYER (CLIPPED TO TOP REGION ONLY; NEVER SCROLLS DOWN) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.TopCenter)
                .clipToBounds()
        ) {
            Crossfade(
                targetState = backdropUrl,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "spotlight_backdrop_crossfade",
                modifier = Modifier.fillMaxSize(),
            ) { url ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.99f }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Black,
                                        0.48f to Color.Black,
                                        0.75f to Color.Black.copy(alpha = 0.40f),
                                        1.00f to Color.Transparent,
                                    ),
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                ) {
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = activeSpotlight?.title,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Ambient gradient scrim behind text for maximum legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color(0xBB080A10),
                                        0.42f to Color(0x66080A10),
                                        0.78f to Color.Transparent,
                                    )
                                )
                            )
                    )
                }
            }
        }

        // --- 2. FOREGROUND CONTENT SCROLLER (WHOLPHIN / NETFLIX DENSITY) ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- SPOTLIGHT TEXT OVERLAY & ACTION BUTTONS ---
            item(key = "spotlight_content_overlay") {
                if (activeSpotlight != null) {
                    TvHomeSpotlightOverlay(
                        item = activeSpotlight,
                        onPlay = { onPlayMediaId(activeSpotlight.id) },
                        onDetails = { onOpenMedia(activeSpotlight.id) },
                    )
                } else {
                    VantafynTvFallbackHero(
                        serverName = session?.server?.name,
                        onExplore = onExploreLibraries,
                    )
                }
            }

            // --- RAIL 1: MEDIA LIBRARIES / MY MEDIA (IMMEDIATELY BELOW SPOTLIGHT) ---
            if (libraries.isNotEmpty()) {
                item(key = "row_libraries") {
                    Column {
                        VantafynTvSectionHeader(
                            title = "Your Libraries",
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(start = 36.dp, end = 48.dp),
                        ) {
                            items(libraries, key = { it.id }) { lib ->
                                val imageUrl = lib.imageUrl ?: if (serverUrl != null) TvArtworkResolver.buildPrimaryUrl(serverUrl, lib.id, width = 400) else null
                                val subtitle = lib.collectionType?.replaceFirstChar { it.uppercase() } ?: "Media"
                                VantafynTvWideCard(
                                    title = lib.name,
                                    imageUrl = imageUrl,
                                    subtitle = subtitle,
                                    width = 170.dp,
                                    onClick = { onOpenMedia(lib.id) },
                                    onFocus = {
                                        spotlightState.update(lib.toSpotlight(serverUrl))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // --- RAILS 2+: DYNAMIC HOME CAROUSELS (CONTINUE WATCHING, RECENTLY ADDED, ETC.) ---
            sections.forEachIndexed { index, section ->
                if (section.items.isNotEmpty()) {
                    item(key = "section_${section.title}_$index") {
                        Column {
                            VantafynTvSectionHeader(
                                title = section.title,
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(start = 36.dp, end = 48.dp),
                            ) {
                                items(section.items, key = { it.id }) { item ->
                                    val onCardFocus = {
                                        val heroMatch = heroItems.firstOrNull { h -> h.id == item.id }
                                        spotlightState.update(item.toSpotlight(serverUrl, heroMatch))
                                    }

                                    if (item.shape == JellyfinMediaCardShape.Wide) {
                                        val wideImg = item.thumbUrl ?: item.backdropUrl ?: item.imageUrl ?: if (serverUrl != null) TvArtworkResolver.buildThumbUrl(serverUrl, item.id) else null
                                        VantafynTvWideCard(
                                            title = item.title,
                                            imageUrl = wideImg,
                                            subtitle = item.subtitle ?: item.year?.toString(),
                                            progressPercentage = item.progress?.times(100f),
                                            onClick = { onOpenMedia(item.id) },
                                            onFocus = onCardFocus,
                                        )
                                    } else {
                                        val posterImg = item.imageUrl ?: if (serverUrl != null) TvArtworkResolver.buildPrimaryUrl(serverUrl, item.id) else null
                                        VantafynTvPosterCard(
                                            title = item.title,
                                            imageUrl = posterImg,
                                            subtitle = item.subtitle ?: item.year?.toString(),
                                            progressPercentage = item.progress?.times(100f),
                                            onClick = { onOpenMedia(item.id) },
                                            onFocus = onCardFocus,
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
}

@Composable
private fun TvHomeSpotlightOverlay(
    item: TvHomeSpotlightItem,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 36.dp, end = 48.dp, top = 28.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        // Logo Artwork or Bold Title
        if (!item.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.logoUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = 56.dp)
                    .fillMaxWidth(0.42f),
                alignment = Alignment.CenterStart,
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Text(
                text = item.title,
                color = VantafynColors.Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Metadata Row with Golden Star Rating
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val rating = item.communityRating
            if (rating != null && rating > 0f) {
                Text(
                    text = "★ ${String.format("%.1f", rating)}",
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            val year = item.year
            if (year != null && year > 0) {
                Text(
                    text = year.toString(),
                    color = VantafynColors.Ink.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            val runtime = item.runtimeMinutes
            if (runtime != null && runtime > 0) {
                val hrs = runtime / 60
                val mins = runtime % 60
                val runtimeLabel = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                Text(
                    text = "•  $runtimeLabel",
                    color = VantafynColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (!item.officialRating.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.officialRating.orEmpty(),
                        color = VantafynColors.Ink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (item.genres.isNotEmpty()) {
                Text(
                    text = "•  " + item.genres.take(3).joinToString(", "),
                    color = VantafynColors.Muted.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Full Overview (up to 3 lines for 10-foot readability without taking excess vertical space)
        if (!item.overview.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.overview.orEmpty(),
                color = Color(0xFFD1D8E6),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.68f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stable Action Buttons (Directly triggers action on current spotlight item)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VantafynTvGlassButton(
                text = "Play",
                icon = Icons.Rounded.PlayArrow,
                isPrimary = true,
                onClick = onPlay,
            )

            VantafynTvGlassButton(
                text = "Details",
                icon = Icons.Rounded.Info,
                isPrimary = false,
                onClick = onDetails,
            )
        }
    }
}

package dev.vantafyn.tv.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinMediaCardShape
import dev.vantafyn.core.jellyfin.JellyfinMediaItem
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvPosterCard
import dev.vantafyn.tv.components.VantafynTvSectionHeader
import dev.vantafyn.tv.components.VantafynTvWideCard
import dev.vantafyn.tv.media.TvArtworkResolver
import java.util.UUID
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvMyListScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    sidebarContentOffset: Dp = 0.dp,
    onOpenMedia: (UUID) -> Unit = {},
    onPlayMediaId: (UUID) -> Unit = {},
    onPreviewMedia: (UUID) -> Unit = {},
) {
    val favorites = state.favorites
    val serverUrl = session?.server?.url
    val groupedSections = remember(favorites) { favorites.toTvMyListSections() }
    val initialHeroItem = remember(groupedSections) { groupedSections.firstOrNull()?.items?.firstOrNull() }
    val initialHero = remember(initialHeroItem, serverUrl) { initialHeroItem?.toMyListSpotlight(serverUrl) }
    val spotlightState = rememberTvHomeSpotlightState(initialHero)
    var activeSpotlightMediaId by remember { mutableStateOf<UUID?>(null) }
    var spotlightActionFocused by remember { mutableStateOf(false) }

    LaunchedEffect(initialHeroItem?.id, initialHero) {
        val initialId = initialHeroItem?.id ?: return@LaunchedEffect
        activeSpotlightMediaId = initialId
        spotlightState.replace(initialHero)
    }

    LaunchedEffect(state.mediaDetail, activeSpotlightMediaId, serverUrl) {
        val detail = state.mediaDetail
        if (detail != null && detail.id == activeSpotlightMediaId) {
            spotlightState.replace(detail.toMyListSpotlight(serverUrl))
        }
    }

    LaunchedEffect(activeSpotlightMediaId) {
        val focusedId = activeSpotlightMediaId ?: return@LaunchedEffect
        delay(60)
        onPreviewMedia(focusedId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        TvMyListBackdrop(
            item = spotlightState.currentItem ?: initialHero,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvMyListHeroContentHeight)
                .align(Alignment.TopStart),
        ) {
            Crossfade(
                targetState = spotlightState.currentItem ?: initialHero,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                label = "my_list_spotlight_content_crossfade",
                modifier = Modifier.fillMaxSize(),
            ) { spotlight ->
                if (spotlight != null) {
                    TvMyListSpotlightOverlay(
                        item = spotlight,
                        contentStartPadding = TvMyListContentStartPadding + sidebarContentOffset,
                        onPlay = { onPlayMediaId(spotlight.id) },
                        onDetails = { onOpenMedia(spotlight.id) },
                        onActionFocusChanged = { hasFocus -> spotlightActionFocused = hasFocus },
                    )
                } else {
                    TvMyListEmptyHero(
                        isLoading = state.isFavoritesLoading,
                        error = state.favoritesError,
                        contentStartPadding = TvMyListContentStartPadding + sidebarContentOffset,
                    )
                }
            }
        }

        val density = LocalDensity.current
        val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
        val railBringIntoViewSpec = remember(density) {
            TvMyListRailBringIntoViewSpec(
                with(density) { TvMyListRailFocusTopInset.toPx() },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = TvMyListRailsTopPadding)
                .clipToBounds()
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black,
                                0.86f to Color.Black,
                                0.98f to Color.Transparent,
                                1.00f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides railBringIntoViewSpec) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = TvMyListRailViewportTopPadding, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(TvMyListRailSpacing),
                ) {
                    if (state.isFavoritesLoading && favorites.isEmpty()) {
                        item(key = "my_list_loading") {
                            TvMyListStatusRow(
                                title = "Loading My List",
                                subtitle = "Collecting your saved movies, shows, and music.",
                                startPadding = TvMyListContentStartPadding + sidebarContentOffset,
                                loading = true,
                            )
                        }
                    }

                    state.favoritesError?.let { error ->
                        item(key = "my_list_error") {
                            TvMyListStatusRow(
                                title = "Couldn't Load My List",
                                subtitle = error,
                                startPadding = TvMyListContentStartPadding + sidebarContentOffset,
                                error = true,
                            )
                        }
                    }

                    if (!state.isFavoritesLoading && favorites.isEmpty() && state.favoritesError == null) {
                        item(key = "my_list_empty") {
                            TvMyListStatusRow(
                                title = "Your My List Is Empty",
                                subtitle = "Add movies, shows, episodes, and music from their detail pages.",
                                startPadding = TvMyListContentStartPadding + sidebarContentOffset,
                            )
                        }
                    }

                    groupedSections.forEach { section ->
                        item(key = "my_list_${section.title}") {
                            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                                TvMyListRail(
                                    title = section.title,
                                    items = section.items,
                                    serverUrl = serverUrl,
                                    startPadding = TvMyListContentStartPadding + sidebarContentOffset,
                                    onOpenMedia = onOpenMedia,
                                    onCardFocus = { item ->
                                        activeSpotlightMediaId = item.id
                                        spotlightState.update(item.toMyListSpotlight(serverUrl))
                                    },
                                    onCardFocusChanged = { _, _ -> },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvMyListBackdrop(
    item: TvHomeSpotlightItem?,
    modifier: Modifier = Modifier,
) {
    val backdropUrl = item?.backdropUrl ?: item?.posterUrl
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .clipToBounds(),
    ) {
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            label = "my_list_spotlight_backdrop_crossfade",
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
                    },
            ) {
                if (!url.isNullOrBlank()) {
                    AsyncImage(
                        model = url,
                        contentDescription = item?.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color(0xBB080A10),
                                    0.42f to Color(0x66080A10),
                                    0.78f to Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun TvMyListSpotlightOverlay(
    item: TvHomeSpotlightItem,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    contentStartPadding: Dp = TvMyListContentStartPadding,
    onActionFocusChanged: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = contentStartPadding, end = 48.dp, top = TvMyListHeroContentTopPadding, bottom = 8.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        var logoFailed by remember(item.id, item.logoUrl) { mutableStateOf(false) }
        val showLogoArtwork = !item.logoUrl.isNullOrBlank() && !logoFailed
        Box(
            modifier = Modifier
                .fillMaxWidth(0.50f)
                .height(TvMyListLogoSlotHeight),
            contentAlignment = Alignment.BottomStart,
        ) {
            if (showLogoArtwork) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    onError = { logoFailed = true },
                    modifier = Modifier
                        .widthIn(max = 310.dp)
                        .heightIn(max = TvMyListLogoSlotHeight)
                        .fillMaxHeight(),
                    alignment = Alignment.BottomStart,
                )
            } else {
                Text(
                    text = item.title,
                    color = VantafynColors.Ink,
                    fontSize = 27.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.92f),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(TvMyListMetadataSlotHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
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
                            .padding(horizontal = 5.dp, vertical = 1.dp),
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(TvMyListDescriptionSlotHeight),
            contentAlignment = Alignment.TopStart,
        ) {
            val description = item.overview?.takeIf { it.isNotBlank() }
                ?: item.subtitle?.takeIf { it.isNotBlank() }
                ?: "Saved to My List"
            Box(
                modifier = Modifier
                    .widthIn(max = TvMyListDescriptionMaxWidth)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xC0060A12),
                                0.68f to Color(0x96070D18),
                                1.00f to Color(0x24070D18),
                            ),
                        ),
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.035f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.08f),
                            ),
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                Text(
                    text = description,
                    color = Color(0xFFD1D8E6),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .height(TvMyListActionSlotHeight)
                .onFocusChanged { onActionFocusChanged(it.hasFocus) },
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VantafynTvGlassButton(
                    text = "Play",
                    icon = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    compact = true,
                    illuminatedPrimary = true,
                    modifier = Modifier.width(92.dp),
                    onClick = onPlay,
                )
                VantafynTvGlassButton(
                    text = "Details",
                    icon = Icons.Rounded.Info,
                    isPrimary = true,
                    compact = true,
                    illuminatedPrimary = true,
                    modifier = Modifier.width(104.dp),
                    onClick = onDetails,
                )
            }
        }
    }
}

@Composable
private fun TvMyListRail(
    title: String,
    items: List<JellyfinMediaItem>,
    serverUrl: String?,
    startPadding: Dp,
    onOpenMedia: (UUID) -> Unit,
    onCardFocus: (JellyfinMediaItem) -> Unit,
    onCardFocusChanged: (JellyfinMediaItem, Boolean) -> Unit,
) {
    val rowState = rememberLazyListState()
    val leftFadeAlpha by animateFloatAsState(
        targetValue = if (rowState.firstVisibleItemIndex > 0 || rowState.firstVisibleItemScrollOffset > 8) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tv_my_list_rail_left_fade",
    )

    Column {
        VantafynTvSectionHeader(
            title = title,
            startPadding = startPadding,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding)
                .clipToBounds()
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = 1f - leftFadeAlpha),
                                0.035f to Color.Black,
                                0.96f to Color.Black,
                                1.00f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            LazyRow(
                state = rowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 10.dp, top = 12.dp, end = 48.dp, bottom = 8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    val isWideCard = item.shape == JellyfinMediaCardShape.Wide ||
                        item.shape == JellyfinMediaCardShape.Library
                    if (isWideCard) {
                        val wideImg = item.thumbUrl ?: item.backdropUrl ?: item.imageUrl
                            ?: if (serverUrl != null) TvArtworkResolver.buildThumbUrl(serverUrl, item.id) else null
                        VantafynTvWideCard(
                            title = item.title,
                            imageUrl = wideImg,
                            subtitle = item.subtitle ?: item.year?.toString(),
                            width = TvMyListWideCardWidth,
                            progressPercentage = item.progress?.times(100f),
                            onClick = { onOpenMedia(item.id) },
                            onFocus = { onCardFocus(item) },
                            onFocusChanged = { hasFocus -> onCardFocusChanged(item, hasFocus) },
                        )
                    } else {
                        val posterImg = item.imageUrl
                            ?: if (serverUrl != null) TvArtworkResolver.buildPrimaryUrl(serverUrl, item.id) else null
                        VantafynTvPosterCard(
                            title = item.title,
                            imageUrl = posterImg,
                            subtitle = item.subtitle ?: item.year?.toString(),
                            width = TvMyListPosterCardWidth,
                            progressPercentage = item.progress?.times(100f),
                            onClick = { onOpenMedia(item.id) },
                            onFocus = { onCardFocus(item) },
                            onFocusChanged = { hasFocus -> onCardFocusChanged(item, hasFocus) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvMyListEmptyHero(
    isLoading: Boolean,
    error: String?,
    contentStartPadding: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = contentStartPadding, end = 48.dp, top = TvMyListHeroContentTopPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "My List",
            color = VantafynColors.Ink,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = when {
                isLoading -> "Loading your saved media."
                error != null -> "Your saved media could not be loaded."
                else -> "Movies, series, episodes, music, and more that you save will appear here."
            },
            color = VantafynColors.Muted.copy(alpha = 0.9f),
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.46f),
        )
    }
}

@Composable
private fun TvMyListStatusRow(
    title: String,
    subtitle: String,
    startPadding: Dp,
    loading: Boolean = false,
    error: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .padding(start = startPadding + 10.dp, end = 48.dp, top = 12.dp)
            .clip(shape)
            .background(Color(0x9910182A))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            loading -> CircularProgressIndicator(
                color = VantafynColors.Primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(26.dp),
            )
            error -> Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(28.dp),
            )
            else -> Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = VantafynColors.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Column {
            Text(
                text = title,
                color = VantafynColors.Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = VantafynColors.Muted,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private data class TvMyListSection(
    val title: String,
    val items: List<JellyfinMediaItem>,
)

private fun List<JellyfinMediaItem>.toTvMyListSections(): List<TvMyListSection> {
    val grouped = groupBy { item -> item.itemType?.ifBlank { "Other" } ?: "Other" }
    val preferredOrder = listOf(
        "Movie",
        "Series",
        "Episode",
        "BoxSet",
        "Audio",
        "MusicAlbum",
        "MusicArtist",
        "Book",
        "LiveTvChannel",
        "LiveTvProgram",
        "Other",
    )

    return grouped.entries
        .sortedWith(
            compareBy<Map.Entry<String, List<JellyfinMediaItem>>> { entry ->
                preferredOrder.indexOfFirst { it.equals(entry.key, ignoreCase = true) }
                    .takeIf { it >= 0 }
                    ?: Int.MAX_VALUE
            }.thenBy { it.key },
        )
        .map { (type, items) ->
            TvMyListSection(type.tvMyListGroupLabel(), items)
        }
}

private fun JellyfinMediaItem.toMyListSpotlight(serverUrl: String? = null): TvHomeSpotlightItem =
    run {
        val sanitizedSubtitle = subtitle?.takeUnless { year != null && it.trim() == year.toString() }
        TvHomeSpotlightItem(
            id = id,
            title = title,
            subtitle = sanitizedSubtitle ?: itemType?.tvMyListGroupLabel() ?: mediaType,
            overview = sanitizedSubtitle,
            year = year,
            backdropUrl = if (serverUrl != null) {
                TvArtworkResolver.buildBackdropUrl(serverUrl, id)
            } else {
                backdropUrl ?: thumbUrl
            },
            logoUrl = if (serverUrl != null) {
                TvArtworkResolver.buildLogoUrl(serverUrl, id)
            } else {
                logoUrl
            },
            posterUrl = imageUrl ?: if (serverUrl != null) TvArtworkResolver.buildPrimaryUrl(serverUrl, id) else null,
        )
    }

private fun JellyfinMediaDetail.toMyListSpotlight(serverUrl: String? = null): TvHomeSpotlightItem =
    run {
        val sanitizedSubtitle = subtitle?.takeUnless { year != null && it.trim() == year.toString() }
        TvHomeSpotlightItem(
            id = id,
            title = title,
            subtitle = sanitizedSubtitle ?: itemType?.tvMyListGroupLabel(),
            overview = overview,
            year = year,
            runtimeMinutes = runtimeMinutes,
            officialRating = officialRating,
            communityRating = communityRating,
            genres = genres,
            backdropUrl = if (serverUrl != null) {
                TvArtworkResolver.buildBackdropUrl(serverUrl, id)
            } else {
                backdropUrl
            },
            logoUrl = if (serverUrl != null) {
                TvArtworkResolver.buildLogoUrl(serverUrl, id)
            } else {
                logoUrl
            },
            posterUrl = imageUrl ?: if (serverUrl != null) TvArtworkResolver.buildPrimaryUrl(serverUrl, id) else null,
        )
    }

private fun String.tvMyListGroupLabel(): String =
    when (lowercase()) {
        "movie" -> "Movies"
        "series" -> "TV Shows"
        "episode" -> "Episodes"
        "boxset" -> "Collections"
        "audio", "musicalbum", "musicartist" -> "Music"
        "book" -> "Books"
        "livetvchannel", "livetvprogram" -> "Live TV"
        else -> replaceFirstChar(Char::titlecase)
    }

@OptIn(ExperimentalFoundationApi::class)
private class TvMyListRailBringIntoViewSpec(
    private val spaceAbovePx: Float,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float = offset - spaceAbovePx
}

private val TvMyListHeroContentHeight: Dp = 310.dp
private val TvMyListHeroContentTopPadding: Dp = 34.dp
private val TvMyListContentStartPadding: Dp = 108.dp
private val TvMyListRailsTopPadding: Dp = 276.dp
private val TvMyListRailViewportTopPadding: Dp = 18.dp
private val TvMyListRailFocusTopInset: Dp = 36.dp
private val TvMyListRailSpacing: Dp = 12.dp
private val TvMyListWideCardWidth: Dp = 144.dp
private val TvMyListPosterCardWidth: Dp = 88.dp
private val TvMyListLogoSlotHeight: Dp = 78.dp
private val TvMyListMetadataSlotHeight: Dp = 20.dp
private val TvMyListDescriptionSlotHeight: Dp = 58.dp
private val TvMyListDescriptionMaxWidth: Dp = 520.dp
private val TvMyListActionSlotHeight: Dp = 38.dp

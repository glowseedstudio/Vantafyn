package dev.vantafyn.tv.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinHomeSection
import dev.vantafyn.core.jellyfin.JellyfinMediaCardShape
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.auth.HomeSectionType
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvFallbackHero
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvPosterCard
import dev.vantafyn.tv.components.VantafynTvSectionHeader
import dev.vantafyn.tv.components.VantafynTvWideCard
import dev.vantafyn.tv.media.TvArtworkResolver
import java.util.UUID
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvHomeScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    sidebarContentOffset: Dp = 0.dp,
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
    val orderedSections = remember(state.home?.sections, state.homeLayout, state.configuredSmartRows) {
        tvHomeOrderedSections(state)
    }
    val serverUrl = session?.server?.url
    val initialHero = heroItems.firstOrNull()?.toSpotlight(serverUrl)

    val spotlightState = rememberTvHomeSpotlightState(initialHero)
    var focusedSpotlightMediaId by remember { mutableStateOf<UUID?>(null) }
    var spotlightActionFocused by remember { mutableStateOf(false) }
    val spotlightPinnedByFocus = focusedSpotlightMediaId != null || spotlightActionFocused

    LaunchedEffect(initialHero) {
        spotlightState.updateIfNull(initialHero)
    }

    LaunchedEffect(heroItems, serverUrl, spotlightPinnedByFocus) {
        if (spotlightPinnedByFocus || heroItems.size <= 1) return@LaunchedEffect

        var index = heroItems.indexOfFirst { it.id == spotlightState.currentItem?.id }
            .takeIf { it >= 0 }
            ?: 0

        while (true) {
            delay(9_000)
            if (heroItems.isEmpty()) continue
            index = (index + 1) % heroItems.size
            spotlightState.update(heroItems[index].toSpotlight(serverUrl))
        }
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

        // --- 2. FIXED SPOTLIGHT COPY / ACTION LAYER (NEVER PARTICIPATES IN ROW SCROLLING) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvHomeHeroContentHeight)
                .align(Alignment.TopStart),
        ) {
            Crossfade(
                targetState = activeSpotlight,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                label = "spotlight_content_crossfade",
                modifier = Modifier.fillMaxSize(),
            ) { spotlight ->
                if (spotlight != null) {
                    TvHomeSpotlightOverlay(
                        item = spotlight,
                        contentStartPadding = TvHomeContentStartPadding + sidebarContentOffset,
                        onPlay = { onPlayMediaId(spotlight.id) },
                        onDetails = { onOpenMedia(spotlight.id) },
                        onActionFocusChanged = { hasFocus -> spotlightActionFocused = hasFocus },
                    )
                } else {
                    VantafynTvFallbackHero(
                        serverName = session?.server?.name,
                        onExplore = onExploreLibraries,
                    )
                }
            }
        }

        // --- 3. FOREGROUND CONTENT SCROLLER (WHOLPHIN / NETFLIX DENSITY) ---
        val density = LocalDensity.current
        val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
        val railBringIntoViewSpec = remember(density) {
            TvHomeRailBringIntoViewSpec(
                with(density) { TvHomeRailFocusTopInset.toPx() },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = TvHomeRailsTopPadding)
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
                    contentPadding = PaddingValues(top = TvHomeRailViewportTopPadding, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(TvHomeRailSpacing),
                ) {
                    // --- DYNAMIC HOME CAROUSELS. My Media is provided by the shared home model and pinned first for TV. ---
                    orderedSections.forEachIndexed { index, section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "section_${section.title}_$index") {
                                CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                                    val railStartPadding = TvHomeContentStartPadding + sidebarContentOffset
                                    val rowState = rememberLazyListState()
                                    val leftFadeAlpha by animateFloatAsState(
                                        targetValue = if (
                                            rowState.firstVisibleItemIndex > 0 ||
                                            rowState.firstVisibleItemScrollOffset > 8
                                        ) {
                                            1f
                                        } else {
                                            0f
                                        },
                                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                        label = "tv_home_rail_left_fade",
                                    )
                                    Column {
                                        VantafynTvSectionHeader(
                                            title = section.title,
                                            startPadding = railStartPadding,
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = railStartPadding)
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
                                                items(section.items, key = { it.id }) { item ->
                                                    val onCardFocus = {
                                                        if (item.shape == JellyfinMediaCardShape.Library) {
                                                            focusedSpotlightMediaId = null
                                                        } else {
                                                            val heroMatch = heroItems.firstOrNull { h -> h.id == item.id }
                                                            focusedSpotlightMediaId = item.id
                                                            spotlightState.update(item.toSpotlight(serverUrl, heroMatch))
                                                        }
                                                    }
                                                    val onCardFocusChanged: (Boolean) -> Unit = { hasFocus ->
                                                        if (!hasFocus && focusedSpotlightMediaId == item.id) {
                                                            focusedSpotlightMediaId = null
                                                        }
                                                    }

                                                    val isWideCard = item.shape == JellyfinMediaCardShape.Wide ||
                                                        item.shape == JellyfinMediaCardShape.Library
                                                    if (isWideCard) {
                                                        val wideImg = item.thumbUrl ?: item.backdropUrl ?: item.imageUrl ?: if (serverUrl != null) TvArtworkResolver.buildThumbUrl(serverUrl, item.id) else null
                                                        VantafynTvWideCard(
                                                            title = item.title,
                                                            imageUrl = wideImg,
                                                            subtitle = if (item.shape == JellyfinMediaCardShape.Library) null else item.subtitle ?: item.year?.toString(),
                                                            width = TvHomeWideCardWidth,
                                                            progressPercentage = item.progress?.times(100f),
                                                            onClick = { onOpenMedia(item.id) },
                                                            onFocus = onCardFocus,
                                                            onFocusChanged = onCardFocusChanged,
                                                        )
                                                    } else {
                                                        val posterImg = item.imageUrl ?: if (serverUrl != null) TvArtworkResolver.buildPrimaryUrl(serverUrl, item.id) else null
                                                        VantafynTvPosterCard(
                                                            title = item.title,
                                                            imageUrl = posterImg,
                                                            subtitle = item.subtitle ?: item.year?.toString(),
                                                            width = TvHomePosterCardWidth,
                                                            progressPercentage = item.progress?.times(100f),
                                                            onClick = { onOpenMedia(item.id) },
                                                            onFocus = onCardFocus,
                                                            onFocusChanged = onCardFocusChanged,
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
            }
        }
    }
}

private val TvHomeHeroContentHeight: Dp = 310.dp
private val TvHomeHeroContentTopPadding: Dp = 34.dp
private val TvHomeContentStartPadding: Dp = 108.dp
private val TvHomeRailsTopPadding: Dp = 276.dp
private val TvHomeRailViewportTopPadding: Dp = 18.dp
private val TvHomeRailFocusTopInset: Dp = 36.dp
private val TvHomeRailSpacing: Dp = 12.dp
private val TvHomeWideCardWidth: Dp = 144.dp
private val TvHomePosterCardWidth: Dp = 88.dp
private val TvHomeLogoSlotHeight: Dp = 78.dp
private val TvHomeMetadataSlotHeight: Dp = 20.dp
private val TvHomeDescriptionSlotHeight: Dp = 58.dp
private val TvHomeActionSlotHeight: Dp = 38.dp

@OptIn(ExperimentalFoundationApi::class)
private class TvHomeRailBringIntoViewSpec(
    private val spaceAbovePx: Float,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float = offset - spaceAbovePx
}

private fun tvHomeOrderedSections(state: VantafynHomeUiState): List<JellyfinHomeSection> {
    val sections = state.home?.sections.orEmpty()
    if (sections.isEmpty()) return emptyList()

    val configuredSmartRows = state.configuredSmartRows.toSet()
    val visibleLayout = state.homeLayout
        .sortedBy { it.order }
        .filter { preference ->
            preference.type != HomeSectionType.MediaBar &&
                (preference.visible || (preference.type == HomeSectionType.SmartRows && configuredSmartRows.isNotEmpty()))
        }

    return buildList {
        visibleLayout.forEach { preference ->
            when (preference.type) {
                HomeSectionType.MediaBar -> Unit
                HomeSectionType.MyMedia -> {
                    sections.tvSectionExact("My Media")?.let(::add)
                }
                HomeSectionType.ContinueWatching -> {
                    sections.tvSectionContains("Continue")?.let(::add)
                }
                HomeSectionType.RecentlyAddedMovies -> {
                    sections.tvSectionExact("Recently Added Movies")?.let(::add)
                }
                HomeSectionType.RecentlyAddedTv -> {
                    sections.tvSectionExact("Recently Added TV")?.let(::add)
                }
                HomeSectionType.LiveTvChannels -> {
                    sections.tvSectionContains("Live TV")?.let(::add)
                }
                HomeSectionType.SmartRows -> {
                    sections
                        .filter { section -> section.title in configuredSmartRows && section.items.isNotEmpty() }
                        .forEach(::add)
                }
                HomeSectionType.OtherLibraries -> {
                    sections.tvSectionExact("Other Libraries")?.let(::add)
                        ?: sections.tvSectionExact("More Libraries")?.let(::add)
                }
            }
        }
    }
}

private fun List<JellyfinHomeSection>.tvSectionExact(title: String): JellyfinHomeSection? =
    firstOrNull { section -> section.title.equals(title, ignoreCase = true) && section.items.isNotEmpty() }

private fun List<JellyfinHomeSection>.tvSectionContains(token: String): JellyfinHomeSection? =
    firstOrNull { section -> section.title.contains(token, ignoreCase = true) && section.items.isNotEmpty() }

@Composable
private fun TvHomeSpotlightOverlay(
    item: TvHomeSpotlightItem,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    contentStartPadding: Dp = TvHomeContentStartPadding,
    onActionFocusChanged: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = contentStartPadding, end = 48.dp, top = TvHomeHeroContentTopPadding, bottom = 8.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        var logoFailed by remember(item.id, item.logoUrl) { mutableStateOf(false) }
        val showLogoArtwork = !item.logoUrl.isNullOrBlank() && !logoFailed

        // Reserve a stable logo/title slot so late-loaded logo artwork never pushes metadata/actions down.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.50f)
                .height(TvHomeLogoSlotHeight),
            contentAlignment = Alignment.BottomStart,
        ) {
            if (showLogoArtwork) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TvHomeLogoSlotHeight),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        onError = { logoFailed = true },
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .heightIn(max = TvHomeLogoSlotHeight)
                            .fillMaxHeight(),
                        alignment = Alignment.BottomStart,
                    )
                }
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

        // Metadata Row with Golden Star Rating
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(TvHomeMetadataSlotHeight),
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Full Overview (up to 3 lines for 10-foot readability without taking excess vertical space)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(TvHomeDescriptionSlotHeight),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                text = item.overview.orEmpty(),
                color = Color(0xFFD1D8E6),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stable Action Buttons (Directly triggers action on current spotlight item)
        Box(
            modifier = Modifier
                .height(TvHomeActionSlotHeight)
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

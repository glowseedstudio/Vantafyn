package dev.vantafyn.tv.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
import dev.vantafyn.core.jellyfin.JellyfinMediaCardShape
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinMediaItem
import dev.vantafyn.core.jellyfin.JellyfinPerson
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.vantafynTvFocusable
import dev.vantafyn.tv.components.VantafynTvPosterCard
import dev.vantafyn.tv.components.VantafynTvSectionHeader
import dev.vantafyn.tv.components.VantafynTvWideCard
import dev.vantafyn.tv.media.TvArtworkResolver
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvDetailScreen(
    detail: JellyfinMediaDetail?,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    contentStartPadding: Dp = 48.dp,
    onPlay: () -> Unit = {},
    onPlayFromStart: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onOpenMedia: (UUID) -> Unit = {},
) {
    if (detail == null || session == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Loading media details...", color = VantafynColors.Muted, fontSize = 16.sp)
        }
        return
    }

    val serverUrl = session.server.url
    val backdropUrl = TvArtworkResolver.buildBackdropUrl(serverUrl, detail.id)
    val logoUrl = detail.logoUrl ?: TvArtworkResolver.buildLogoUrl(serverUrl, detail.id)
    val primaryActionFocusRequester = remember { FocusRequester() }
    val synopsisFocusRequester = remember { FocusRequester() }
    val firstCastFocusRequester = remember { FocusRequester() }

    LaunchedEffect(detail.id) {
        primaryActionFocusRequester.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        TvDetailBackdrop(
            title = detail.title,
            backdropUrl = detail.backdropUrl ?: backdropUrl,
        )

        TvMovieDetailTopInfo(
            detail = detail,
            logoUrl = logoUrl,
            contentStartPadding = contentStartPadding,
            onPlay = onPlay,
            onPlayFromStart = onPlayFromStart,
            onToggleFavorite = onToggleFavorite,
            primaryActionFocusRequester = primaryActionFocusRequester,
        )

        val density = LocalDensity.current
        val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
        val lowerBringIntoViewSpec = remember(density) {
            TvDetailLowerBringIntoViewSpec(
                with(density) { TvDetailLowerFocusTopInset.toPx() },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = TvDetailLowerViewportTopPadding)
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
            CompositionLocalProvider(LocalBringIntoViewSpec provides lowerBringIntoViewSpec) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "detail_synopsis_${detail.id}") {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                            TvDetailSynopsisPocket(
                                detail = detail,
                                startPadding = contentStartPadding,
                                focusRequester = synopsisFocusRequester,
                                downFocusRequester = if (detail.people.isNotEmpty()) firstCastFocusRequester else null,
                            )
                        }
                    }

                    if (detail.people.isNotEmpty()) {
                        item(key = "detail_people_${detail.id}") {
                            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                                TvDetailPeopleRail(
                                    people = detail.people,
                                    startPadding = contentStartPadding,
                                    firstFocusRequester = firstCastFocusRequester,
                                    upFocusRequester = synopsisFocusRequester,
                                )
                            }
                        }
                    }

                    if (detail.collectionItems.isNotEmpty()) {
                        item(key = "detail_collection_${detail.id}") {
                            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                                TvDetailMediaRail(
                                    title = "Movies",
                                    items = detail.collectionItems,
                                    serverUrl = serverUrl,
                                    startPadding = contentStartPadding,
                                    onOpenMedia = onOpenMedia,
                                )
                            }
                        }
                    }

                    if (detail.collections.isNotEmpty()) {
                        item(key = "detail_included_collections_${detail.id}") {
                            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                                TvDetailMediaRail(
                                    title = "Part of Collection",
                                    items = detail.collections,
                                    serverUrl = serverUrl,
                                    startPadding = contentStartPadding,
                                    onOpenMedia = onOpenMedia,
                                )
                            }
                        }
                    }

                    if (detail.related.isNotEmpty()) {
                        item(key = "detail_related_${detail.id}") {
                            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                                TvDetailMediaRail(
                                    title = "Related",
                                    items = detail.related,
                                    serverUrl = serverUrl,
                                    startPadding = contentStartPadding,
                                    onOpenMedia = onOpenMedia,
                                )
                            }
                        }
                    }

                    if (detail.externalLinks.isNotEmpty()) {
                        item(key = "detail_links_${detail.id}") {
                            TvDetailExternalLinks(
                                detail = detail,
                                startPadding = contentStartPadding,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvDetailBackdrop(
    title: String,
    backdropUrl: String?,
) {
    Crossfade(
        targetState = backdropUrl,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "tv_detail_backdrop_crossfade",
        modifier = Modifier.fillMaxSize(),
    ) { url ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black,
                                0.54f to Color.Black,
                                0.76f to Color.Black.copy(alpha = 0.70f),
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
                    contentDescription = title,
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
                                0.00f to Color(0xE6080A10),
                                0.34f to Color(0xB0080A10),
                                0.68f to Color(0x30080A10),
                                1.00f to Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xB0060A12), Color.Transparent),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xF0060A12)),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun TvMovieDetailTopInfo(
    detail: JellyfinMediaDetail,
    logoUrl: String?,
    contentStartPadding: Dp,
    onPlay: () -> Unit,
    onPlayFromStart: () -> Unit,
    onToggleFavorite: () -> Unit,
    primaryActionFocusRequester: FocusRequester,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDetailTopInfoHeight)
            .padding(start = contentStartPadding, end = 54.dp, top = 48.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        TvDetailLogoOrTitle(
            title = detail.title,
            logoUrl = logoUrl,
        )
        Spacer(modifier = Modifier.height(12.dp))
        TvDetailMetadataRow(detail)
        if (detail.streamInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(9.dp))
            TvDetailCompactChipRow(detail.streamInfo)
        }
        Spacer(modifier = Modifier.height(14.dp))
        TvDetailActions(
            detail = detail,
            onPlay = onPlay,
            onPlayFromStart = onPlayFromStart,
            onToggleFavorite = onToggleFavorite,
            primaryActionFocusRequester = primaryActionFocusRequester,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TvDetailLogoOrTitle(
    title: String,
    logoUrl: String?,
) {
    var logoFailed by remember(title, logoUrl) { mutableStateOf(false) }
    val showLogo = !logoUrl.isNullOrBlank() && !logoFailed
    Box(
        modifier = Modifier
            .fillMaxWidth(0.50f)
            .height(TvDetailLogoSlotHeight),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (showLogo) {
            AsyncImage(
                model = logoUrl,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                onError = { logoFailed = true },
                modifier = Modifier
                    .widthIn(max = 390.dp)
                    .heightIn(max = TvDetailLogoSlotHeight)
                    .fillMaxHeight(),
            )
        } else {
            Text(
                text = title,
                color = VantafynColors.Ink,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.92f),
            )
        }
    }
}

@Composable
private fun TvDetailMetadataRow(detail: JellyfinMediaDetail) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.fillMaxWidth(0.72f),
    ) {
        detail.year?.takeIf { it > 0 }?.let { TvDetailMetaText(it.toString()) }
        detail.runtimeMinutes?.takeIf { it > 0 }?.let { TvDetailMetaText("•  ${it.runtimeLabel()}") }
        detail.officialRating?.takeIf { it.isNotBlank() }?.let { rating ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = rating,
                    color = VantafynColors.Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        detail.communityRating?.takeIf { it > 0f }?.let { rating ->
            Text(
                text = "★ ${String.format(Locale.US, "%.1f", rating)}",
                color = Color(0xFFFFD76A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (detail.genres.isNotEmpty()) {
            TvDetailMetaText(
                text = "•  " + detail.genres.take(3).joinToString(", "),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TvDetailMetaText(
    text: String,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        color = VantafynColors.Ink.copy(alpha = 0.86f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TvDetailActions(
    detail: JellyfinMediaDetail,
    onPlay: () -> Unit,
    onPlayFromStart: () -> Unit,
    onToggleFavorite: () -> Unit,
    primaryActionFocusRequester: FocusRequester,
) {
    val hasProgress = detail.progress != null && (detail.progress ?: 0f) > 0.05f
    val supportsMyList = detail.itemType.supportsMyListAction()
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VantafynTvGlassButton(
            text = if (hasProgress) "Resume" else "Play",
            icon = Icons.Rounded.PlayArrow,
            isPrimary = true,
            compact = true,
            illuminatedPrimary = true,
            modifier = Modifier
                .width(if (hasProgress) 126.dp else 92.dp)
                .focusRequester(primaryActionFocusRequester),
            onClick = onPlay,
        )
        if (hasProgress) {
            VantafynTvGlassButton(
                text = "From Start",
                icon = Icons.Rounded.Replay,
                isPrimary = false,
                compact = true,
                modifier = Modifier.width(142.dp),
                onClick = onPlayFromStart,
            )
        }
        if (supportsMyList) {
            VantafynTvGlassButton(
                text = if (detail.isFavorite) "In My List" else "+ My List",
                icon = if (detail.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.Add,
                isPrimary = false,
                compact = true,
                modifier = Modifier.width(126.dp),
                onClick = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun TvDetailSynopsisPocket(
    detail: JellyfinMediaDetail,
    startPadding: Dp,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .padding(start = startPadding, end = 54.dp)
            .fillMaxWidth(0.58f)
            .focusRequester(focusRequester)
            .then(
                if (downFocusRequester != null) {
                    Modifier.focusProperties { down = downFocusRequester }
                } else {
                    Modifier
                },
            )
            .vantafynTvFocusable(
                interactionSource = interactionSource,
                shape = shape,
                scaleFocused = 1.01f,
                borderWidth = 1.5.dp,
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(if (isFocused) 0xB2060A12 else 0x9A060A12),
                        0.70f to Color(if (isFocused) 0x78070D18 else 0x66070D18),
                        1.00f to Color(0x16070D18),
                    ),
                ),
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 15.dp, vertical = 10.dp),
    ) {
        Text(
            text = detail.overview?.takeIf { it.isNotBlank() } ?: "No overview provided by Jellyfin.",
            color = Color(0xDDE6ECF6),
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun TvDetailCompactChipRow(values: List<String>) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(0.54f)
            .clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        contentPadding = PaddingValues(end = 14.dp),
    ) {
        items(values.take(10), key = { it }) { value ->
            val tone = detailChipTone(value)
            val showToneDot = !value.isStarRating()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x7A10182A))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (showToneDot) {
                        tone?.let { dotTone ->
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(dotTone.copy(alpha = 0.90f)),
                            )
                        }
                    }
                    Text(
                        text = value,
                        color = tone ?: VantafynColors.Ink.copy(alpha = 0.88f),
                        fontSize = 10.sp,
                        fontWeight = if (tone != null) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvDetailPeopleRail(
    people: List<JellyfinPerson>,
    startPadding: Dp,
    firstFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
) {
    Column(
        modifier = Modifier.height(176.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        VantafynTvSectionHeader(
            title = "Cast & Crew",
            startPadding = startPadding,
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding)
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 10.dp, top = 7.dp, end = 58.dp, bottom = 16.dp),
        ) {
            items(people.take(24), key = { it.id }) { person ->
                val isFirstPerson = person.id == people.firstOrNull()?.id
                TvDetailPersonCard(
                    person = person,
                    focusRequester = if (isFirstPerson) firstFocusRequester else null,
                    upFocusRequester = if (isFirstPerson) upFocusRequester else null,
                )
            }
        }
    }
}

@Composable
private fun TvDetailPersonCard(
    person: JellyfinPerson,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = CircleShape
    Column(
        modifier = Modifier
            .width(104.dp)
            .height(142.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .then(
                    if (upFocusRequester != null) {
                        Modifier.focusProperties { up = upFocusRequester }
                    } else {
                        Modifier
                    },
                )
                .vantafynTvFocusable(
                    interactionSource = interactionSource,
                    shape = shape,
                    scaleFocused = 1.06f,
                    borderWidth = 2.dp,
                )
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF29364E), Color(0xFF1B2236), Color(0xFF101524)),
                    ),
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = if (isFocused) 0.24f else 0.14f)), shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!person.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = person.imageUrl,
                    contentDescription = person.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = person.name.initials(),
                    color = VantafynColors.Ink.copy(alpha = 0.86f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = person.name,
            color = VantafynColors.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = person.role ?: person.type.orEmpty(),
            color = VantafynColors.Muted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TvDetailMediaRail(
    title: String,
    items: List<JellyfinMediaItem>,
    serverUrl: String,
    startPadding: Dp,
    onOpenMedia: (UUID) -> Unit,
) {
    Column(
        modifier = Modifier.height(226.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        VantafynTvSectionHeader(
            title = title,
            startPadding = startPadding,
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding)
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 10.dp, top = 8.dp, end = 58.dp, bottom = 26.dp),
        ) {
            items(items, key = { it.id }) { item ->
                val isWideCard = item.shape == JellyfinMediaCardShape.Wide ||
                    item.shape == JellyfinMediaCardShape.Library
                if (isWideCard) {
                    VantafynTvWideCard(
                        title = item.title,
                        imageUrl = item.thumbUrl ?: item.backdropUrl ?: item.imageUrl ?: TvArtworkResolver.buildThumbUrl(serverUrl, item.id),
                        subtitle = item.subtitle ?: item.year?.toString(),
                        width = 150.dp,
                        progressPercentage = item.progress?.times(100f),
                        onClick = { onOpenMedia(item.id) },
                    )
                } else {
                    VantafynTvPosterCard(
                        title = item.title,
                        imageUrl = item.imageUrl ?: TvArtworkResolver.buildPrimaryUrl(serverUrl, item.id),
                        subtitle = item.subtitle ?: item.year?.toString(),
                        width = 92.dp,
                        progressPercentage = item.progress?.times(100f),
                        onClick = { onOpenMedia(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TvDetailExternalLinks(
    detail: JellyfinMediaDetail,
    startPadding: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 62.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Links",
            color = VantafynColors.Ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            detail.externalLinks.take(4).forEach { link ->
                VantafynTvGlassButton(
                    text = link.name,
                    icon = Icons.Rounded.Link,
                    isPrimary = false,
                    compact = true,
                    modifier = Modifier.widthIn(min = 112.dp, max = 180.dp),
                    onClick = {},
                )
            }
        }
    }
}

private fun Int.runtimeLabel(): String {
    val hours = this / 60
    val minutes = this % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun String.initials(): String =
    trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "?" }

private fun String?.supportsMyListAction(): Boolean =
    equals("Movie", ignoreCase = true) ||
        equals("Series", ignoreCase = true) ||
        equals("Episode", ignoreCase = true) ||
        equals("Season", ignoreCase = true) ||
        equals("BoxSet", ignoreCase = true) ||
        equals("Audio", ignoreCase = true) ||
        equals("MusicAlbum", ignoreCase = true) ||
        equals("Playlist", ignoreCase = true) ||
        equals("Book", ignoreCase = true)

private fun detailChipTone(value: String): Color? {
    val normalized = value.trim().lowercase(Locale.US)
    return when {
        value.isStarRating() -> Color(0xFFFFD76A)
        "hdr" in normalized || "dolby vision" in normalized || normalized == "dv" -> Color(0xFFFFD36A)
        "4k" in normalized || "2160" in normalized || "uhd" in normalized -> Color(0xFF8FE7FF)
        "1080" in normalized || "720" in normalized -> Color(0xFF6FA8FF)
        "sub" in normalized || normalized == "cc" || "caption" in normalized -> Color(0xFFC892FF)
        "eng" == normalized || "english" in normalized || "audio" in normalized -> Color(0xFF7CE7C8)
        "atmos" in normalized || "dts" in normalized || "aac" in normalized ||
            "flac" in normalized || "truehd" in normalized -> Color(0xFFFF8AD8)
        else -> null
    }
}

private fun String.isStarRating(): Boolean = trimStart().startsWith("★")

private class TvDetailLowerBringIntoViewSpec(
    private val spaceAbovePx: Float,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float = offset - spaceAbovePx
}

private val TvDetailTopInfoHeight: Dp = 388.dp
private val TvDetailLowerViewportTopPadding: Dp = 286.dp
private val TvDetailLowerFocusTopInset: Dp = 24.dp
private val TvDetailLogoSlotHeight: Dp = 104.dp

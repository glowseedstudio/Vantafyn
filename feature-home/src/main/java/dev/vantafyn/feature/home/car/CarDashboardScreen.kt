package dev.vantafyn.feature.home.car

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.vantafyn.core.downloads.DownloadRecord
import dev.vantafyn.core.jellyfin.JellyfinHeroMediaItem
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.auth.MobileDestination
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import java.util.UUID

/**
 * Top category tabs for the In-Car Widescreen Experience.
 */
enum class CarCategory(val title: String, val icon: ImageVector) {
    Movies("Movies", Icons.Rounded.Movie),
    TvShows("TV Shows", Icons.Rounded.Tv),
    Music("Music", Icons.Rounded.MusicNote),
    Downloads("Downloads", Icons.Rounded.FileDownload),
}

/**
 * Ultra-premium, cinematic In-Car Dashboard designed for widescreen car touchscreens (10"–15").
 *
 * Features:
 * 1. Top cinematic category switcher pills (Movies, TV Shows, Music, Offline Downloads) + Search.
 * 2. Oversized 16:9 Continue Watching / Next Up hero banner with 1-tap "Resume" button & progress bar.
 * 3. High-resolution poster / backdrop horizontal shelves with driver-friendly touch targets.
 * 4. 1-tap launch into full-screen parked playback.
 */
@Composable
fun CarDashboardScreen(
    state: VantafynHomeUiState,
    onOpenMedia: (UUID) -> Unit,
    onNavigate: (MobileDestination) -> Unit,
    onPlayOfflineDownload: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by rememberSaveable { mutableStateOf(CarCategory.Movies) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VantafynColors.Graphite,
                        VantafynColors.Surface,
                        Color(0xFF0D0F14),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
        ) {
            // 1. Top Automotive Navigation Bar
            CarTopNavigationBar(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                onSearchClick = { onNavigate(MobileDestination.Search) },
                serverName = state.server?.name ?: "Jellyfin",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )

            // 2. Animated Category Content Switcher
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = {
                    fadeIn(animationSpec = tween(240))
                        .togetherWith(fadeOut(animationSpec = tween(180)))
                },
                label = "CarCategoryTransition",
            ) { category ->
                when (category) {
                    CarCategory.Movies -> CarMoviesView(
                        state = state,
                        onOpenMedia = onOpenMedia,
                    )
                    CarCategory.TvShows -> CarTvShowsView(
                        state = state,
                        onOpenMedia = onOpenMedia,
                    )
                    CarCategory.Music -> CarMusicView(
                        state = state,
                        onNavigate = onNavigate,
                    )
                    CarCategory.Downloads -> CarDownloadsView(
                        state = state,
                        onPlayOfflineDownload = onPlayOfflineDownload,
                    )
                }
            }
        }
    }
}

/**
 * Top Automotive Navigation Header with Large Category Pills & Quick Search.
 */
@Composable
private fun CarTopNavigationBar(
    selectedCategory: CarCategory,
    onSelectCategory: (CarCategory) -> Unit,
    onSearchClick: () -> Unit,
    serverName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category Pills (52dp touch targets)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CarCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                val pillBackground by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF31D7FF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                    animationSpec = tween(200),
                    label = "pillBg",
                )
                val pillBorder by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF31D7FF).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f),
                    animationSpec = tween(200),
                    label = "pillBorder",
                )
                val contentTint by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.75f),
                    animationSpec = tween(200),
                    label = "pillTint",
                )

                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(pillBackground)
                        .border(1.5.dp, pillBorder, RoundedCornerShape(25.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectCategory(category) },
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.title,
                            tint = contentTint,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = category.title,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }

        // Right side: Quick Search Button & Connected Server
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Search Pill
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(25.dp))
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Search",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Server Indicator
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E222D))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676)),
                    )
                    Text(
                        text = serverName,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Movies Tab: Spotlight Hero + Continue Watching + Recently Added Movies.
 */
@Composable
private fun CarMoviesView(
    state: VantafynHomeUiState,
    onOpenMedia: (UUID) -> Unit,
) {
    val hero = state.home?.heroItems?.firstOrNull { it.backdropUrl != null || it.posterUrl != null }
    val continueWatching = state.home?.sections?.firstOrNull {
        it.title.contains("Continue", ignoreCase = true) || it.title.contains("Resume", ignoreCase = true)
    }?.items.orEmpty().filter { it.itemType != "Episode" }

    val recentMovies = state.home?.sections?.firstOrNull {
        it.title.contains("Movie", ignoreCase = true) || it.title.contains("Latest", ignoreCase = true)
    }?.items.orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // Spotlight Hero (16:9 Widescreen)
        if (hero != null) {
            CarSpotlightHero(
                hero = hero,
                onResumeClick = { onOpenMedia(hero.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }

        // Continue Watching Shelf
        if (continueWatching.isNotEmpty()) {
            CarMediaShelf(
                title = "Continue Watching",
                subtitle = "Pick up right where you left off",
                items = continueWatching,
                onItemClick = onOpenMedia,
                isLandscape = true,
            )
        }

        // Recent Movies Shelf
        if (recentMovies.isNotEmpty()) {
            CarMediaShelf(
                title = "Movies in 4K & HDR",
                subtitle = "Recently added to your library",
                items = recentMovies,
                onItemClick = onOpenMedia,
                isLandscape = false,
            )
        }

        // Extra sections from server
        state.home?.sections?.filter {
            !it.title.contains("Continue", ignoreCase = true) &&
                !it.title.contains("Next Up", ignoreCase = true) &&
                it.items.isNotEmpty()
        }?.take(3)?.forEach { section ->
            CarMediaShelf(
                title = section.title,
                subtitle = null,
                items = section.items,
                onItemClick = onOpenMedia,
                isLandscape = false,
            )
        }
    }
}

/**
 * TV Shows Tab: Next Up Spotlight + Next Up Shelf + Series Shelves.
 */
@Composable
private fun CarTvShowsView(
    state: VantafynHomeUiState,
    onOpenMedia: (UUID) -> Unit,
) {
    val nextUpSection = state.home?.sections?.firstOrNull {
        it.title.contains("Next Up", ignoreCase = true) || it.title.contains("Continue", ignoreCase = true)
    }
    val nextUpItems = nextUpSection?.items.orEmpty()
    val featuredNextUp = nextUpItems.firstOrNull()

    val showSections = state.home?.sections?.filter {
        it.title.contains("Show", ignoreCase = true) ||
            it.title.contains("Series", ignoreCase = true) ||
            it.title.contains("TV", ignoreCase = true)
    }.orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // Next Up Hero Banner
        if (featuredNextUp != null) {
            CarNextUpHero(
                item = featuredNextUp,
                onResumeClick = { onOpenMedia(featuredNextUp.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }

        // Next Up Shelf (Landscape Episode Cards)
        if (nextUpItems.isNotEmpty()) {
            CarMediaShelf(
                title = "Next Up Episodes",
                subtitle = "Ready for the next episode",
                items = nextUpItems,
                onItemClick = onOpenMedia,
                isLandscape = true,
            )
        }

        // TV Show Shelves
        showSections.forEach { section ->
            CarMediaShelf(
                title = section.title,
                subtitle = null,
                items = section.items,
                onItemClick = onOpenMedia,
                isLandscape = false,
            )
        }
    }
}

/**
 * Music Tab: In-Car Harmonia Integration with Large Album Art & Quick Access.
 */
@Composable
private fun CarMusicView(
    state: VantafynHomeUiState,
    onNavigate: (MobileDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Music Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF9D4EDD).copy(alpha = 0.35f),
                            Color(0xFF31D7FF).copy(alpha = 0.35f),
                            Color(0xFF161922),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(0.65f),
                ) {
                    Text(
                        text = "Harmonia In-Car Audio",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Lossless FLAC, Hi-Res audio, and full offline caching for smooth road trips without dropouts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF31D7FF), Color(0xFF9D4EDD)),
                            ),
                        )
                        .clickable { onNavigate(MobileDestination.Music) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Open Harmonia",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Downloads Tab: Offline Road Trip Media without cellular/Wi-Fi needed.
 */
@Composable
private fun CarDownloadsView(
    state: VantafynHomeUiState,
    onPlayOfflineDownload: (DownloadRecord) -> Unit,
) {
    val downloads = state.offlineDownloads

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Storage Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF181C26))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF31D7FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF31D7FF),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Road Trip Offline Downloads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "${downloads.size} titles ready for 100% offline playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No offline downloads saved yet.\nDownload movies & TV episodes from your library to watch on the go.",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            // Horizontal list of downloaded items
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(downloads, key = { it.id }) { record ->
                    CarDownloadCard(
                        record = record,
                        onClick = { onPlayOfflineDownload(record) },
                    )
                }
            }
        }
    }
}

/**
 * 16:9 Spotlight Hero Banner for Parked Car Experience.
 */
@Composable
private fun CarSpotlightHero(
    hero: JellyfinHeroMediaItem,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageUrl = hero.backdropUrl ?: hero.posterUrl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141720))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
    ) {
        // High-Resolution Backdrop Image
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = hero.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Gradient Protection Overlays (Left & Bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.95f),
                        0.55f to Color.Black.copy(alpha = 0.70f),
                        1.0f to Color.Transparent,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.70f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )

        // Hero Info & Instant Action
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Genre / Year Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                hero.year?.let { year ->
                    Text(
                        text = year.toString(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                hero.runtimeMinutes?.let { mins ->
                    Text(
                        text = "•  ${mins}m",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                    )
                }
                hero.officialRating?.let { rating ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = rating,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = hero.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            hero.overview?.let { overview ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.65f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Oversized 54dp "▶ Watch Now" Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(27.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF31D7FF), Color(0xFF9D4EDD)),
                        ),
                    )
                    .clickable(onClick = onResumeClick)
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Watch Now",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

/**
 * 16:9 Next Up Episode Hero Banner for TV Shows.
 */
@Composable
private fun CarNextUpHero(
    item: JellyfinMediaCard,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageUrl = item.backdropUrl ?: item.thumbUrl ?: item.imageUrl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141720))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.95f),
                        0.60f to Color.Black.copy(alpha = 0.70f),
                        1.0f to Color.Transparent,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF31D7FF).copy(alpha = 0.25f))
                    .border(1.dp, Color(0xFF31D7FF).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "NEXT UP",
                    color = Color(0xFF31D7FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large 50dp Resume Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF31D7FF), Color(0xFF9D4EDD)),
                        ),
                    )
                    .clickable(onClick = onResumeClick)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "Resume Episode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

/**
 * Horizontal Media Shelf with Header and Driver-Friendly Poster/Backdrop Cards.
 */
@Composable
private fun CarMediaShelf(
    title: String,
    subtitle: String?,
    items: List<JellyfinMediaCard>,
    onItemClick: (UUID) -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Shelf Header
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }

        // Horizontal Row of Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items, key = { it.id }) { card ->
                if (isLandscape) {
                    CarLandscapeCard(card = card, onClick = { onItemClick(card.id) })
                } else {
                    CarPortraitCard(card = card, onClick = { onItemClick(card.id) })
                }
            }
        }
    }
}

/**
 * Portrait Poster Card (2:3 aspect ratio) optimized for in-car touch targets.
 */
@Composable
private fun CarPortraitCard(
    card: JellyfinMediaCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageUrl = card.imageUrl ?: card.posterUrl()

    Column(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF181C26))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Progress Bar if in progress
            card.progress?.let { progress ->
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = Color(0xFF31D7FF),
                        trackColor = Color.Black.copy(alpha = 0.5f),
                    )
                }
            }

            // Unplayed Count Badge
            if (card.unplayedItemCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF31D7FF))
                        .size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = card.unplayedItemCount.toString(),
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = card.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        card.subtitle?.let { sub ->
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Landscape Backdrop Card (16:9 aspect ratio) for Episodes & Next Up.
 */
@Composable
private fun CarLandscapeCard(
    card: JellyfinMediaCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageUrl = card.backdropUrl ?: card.thumbUrl ?: card.imageUrl

    Column(
        modifier = modifier
            .width(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF181C26))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Subtle bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.8f),
                        ),
                    ),
            )

            // Play overlay icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Progress Bar
            card.progress?.let { progress ->
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = Color(0xFF31D7FF),
                        trackColor = Color.Black.copy(alpha = 0.5f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = card.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        card.subtitle?.let { sub ->
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Offline Download Card.
 */
@Composable
private fun CarDownloadCard(
    record: DownloadRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalMb = (record.totalBytes ?: record.bytesDownloaded) / 1_000_000L

    Box(
        modifier = modifier
            .width(220.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1B1F2B))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF31D7FF).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF31D7FF),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "READY",
                        color = Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${record.mediaType.name} • $totalMb MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}

private fun JellyfinMediaCard.posterUrl(): String? = imageUrl ?: backdropUrl ?: thumbUrl

package dev.vantafyn.feature.home.xmen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import java.util.UUID

@Composable
fun XMenWatchGuideDialog(
    movies: List<XMenWatchItemUi>,
    isLoading: Boolean,
    errorMessage: String?,
    sortMode: XMenSortMode,
    filterMode: XMenFilterMode,
    onToggleSort: (XMenSortMode) -> Unit,
    onSelectFilter: (XMenFilterMode) -> Unit,
    onOpenMovie: (UUID) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val vantafynModalContainerColor = VantafynColors.Graphite.copy(alpha = 0.96f)

    val totalMovies = movies.size
    val watchedCount = movies.count { it.isPlayed }
    val onServerCount = movies.count { it.isOnServer }
    val notOnServerCount = movies.count { !it.isOnServer }
    val progressFraction = if (totalMovies > 0) watchedCount.toFloat() / totalMovies.toFloat() else 0f
    val progressPercent = (progressFraction * 100).toInt()

    val sortedMovies = remember(movies, sortMode) {
        when (sortMode) {
            XMenSortMode.Timeline -> movies.sortedBy { it.movie.timelineOrder }
            XMenSortMode.Release -> movies.sortedBy { it.movie.releaseOrder }
        }
    }

    val displayMovies = remember(sortedMovies, filterMode) {
        when (filterMode) {
            XMenFilterMode.All -> sortedMovies
            XMenFilterMode.Watched -> sortedMovies.filter { it.isPlayed }
            XMenFilterMode.Unwatched -> sortedMovies.filter { !it.isPlayed }
            XMenFilterMode.Missing -> sortedMovies.filter { !it.isOnServer }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val maxHeightPx = maxHeight
            val headerArtworkUrl = remember(movies) {
                val serverItem = movies.firstOrNull {
                    it.isOnServer && !it.serverBackdropUrl.isNullOrBlank()
                }?.serverBackdropUrl
                serverItem ?: "https://image.tmdb.org/t/p/w780/3QUVzbcNyfGe3ocWkYAT8emK8Co.jpg"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .heightIn(max = maxHeightPx * 0.92f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(vantafynModalContainerColor)
                    .vantafynAnimatedModalBorder(cornerRadius = 32.dp),
            ) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                ) {
                    AsyncImage(
                        model = headerArtworkUrl,
                        contentDescription = "X-Men Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        vantafynModalContainerColor.copy(alpha = 0.5f),
                                        vantafynModalContainerColor,
                                    ),
                                    startY = 20f,
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        vantafynModalContainerColor.copy(alpha = 0.85f),
                                        Color.Transparent,
                                    ),
                                    endX = 500f,
                                ),
                            ),
                    )

                    // Top row: Brand pill + actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF59E0B))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "MUTANT",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                            )
                            Text(
                                "|",
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp,
                            )
                            Text(
                                "X-MEN",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable(onClick = onRefresh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "✕",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    // Title & Subtitle at bottom of banner
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(
                            "X-Men: The Complete Mutant Saga",
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                        )
                        Text(
                            "All 13 films — First Class to Logan",
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Progress card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                .padding(14.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Franchise Progress",
                                        color = VantafynColors.Ink,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "$watchedCount of $totalMovies watched ($progressPercent%)",
                                        color = Color(0xFFF59E0B),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(99.dp)),
                                    color = Color(0xFFF59E0B),
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    strokeCap = StrokeCap.Round,
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "✓  $watchedCount watched",
                                        color = VantafynColors.Muted,
                                        fontSize = 11.sp,
                                    )
                                    Text(
                                        "🖥  $onServerCount in library" + if (notOnServerCount > 0) " ($notOnServerCount missing)" else "",
                                        color = if (onServerCount == totalMovies) Color(0xFF4CAF50) else VantafynColors.Muted,
                                        fontSize = 11.sp,
                                        fontWeight = if (onServerCount == totalMovies) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }

                    // Sort & Filter row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Sort toggle
                            val nextSort = if (sortMode == XMenSortMode.Timeline) XMenSortMode.Release else XMenSortMode.Timeline
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(99.dp))
                                    .clickable { onToggleSort(nextSort) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    "⇄  ${sortMode.label}",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Filter chips
                            XMenFilterMode.entries.forEach { mode ->
                                val selected = filterMode == mode
                                val count = when (mode) {
                                    XMenFilterMode.All -> totalMovies
                                    XMenFilterMode.Watched -> watchedCount
                                    XMenFilterMode.Unwatched -> totalMovies - watchedCount
                                    XMenFilterMode.Missing -> notOnServerCount
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(
                                            if (selected) Color.White.copy(alpha = 0.18f)
                                            else VantafynColors.SurfaceHigh.copy(alpha = 0.5f),
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) Color.White.copy(alpha = 0.4f)
                                            else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(99.dp),
                                        )
                                        .clickable { onSelectFilter(mode) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        "${mode.label} ($count)",
                                        color = if (selected) VantafynColors.Ink else VantafynColors.Muted,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }

                    // Loading State
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                VantafynLoadingIndicator("Syncing with your Jellyfin library...")
                            }
                        }
                    }

                    // Error banner
                    errorMessage?.let { msg ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF8B0000).copy(alpha = 0.3f))
                                    .border(1.dp, Color(0xFF8B0000).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                            ) {
                                Text(msg, color = Color(0xFFFF9E9E), fontSize = 12.sp)
                            }
                        }
                    }

                    // Movie List
                    itemsIndexed(displayMovies, key = { _, it -> it.movie.id }) { index, item ->
                        XMenMovieCard(
                            item = item,
                            orderIndex = if (sortMode == XMenSortMode.Timeline) item.movie.timelineOrder else item.movie.releaseOrder,
                            sortMode = sortMode,
                            onOpen = { item.mediaItemId?.let(onOpenMovie) },
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun XMenMovieCard(
    item: XMenWatchItemUi,
    orderIndex: Int,
    sortMode: XMenSortMode,
    onOpen: () -> Unit,
) {
    val movie = item.movie
    val isAvailable = item.isOnServer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (item.isPlayed) VantafynColors.SurfaceHigh.copy(alpha = 0.3f)
                else VantafynColors.SurfaceHigh.copy(alpha = 0.55f),
            )
            .border(
                1.dp,
                when {
                    item.isPlayed -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                    isAvailable -> Color(0xFFF59E0B).copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.06f)
                },
                RoundedCornerShape(16.dp),
            )
            .clickable(enabled = isAvailable, onClick = onOpen)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Order index badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        item.isPlayed -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        isAvailable -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                        else -> Color.White.copy(alpha = 0.07f)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$orderIndex",
                color = when {
                    item.isPlayed -> Color(0xFF4CAF50)
                    isAvailable -> Color.White
                    else -> VantafynColors.Muted
                },
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }

        // Poster image
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            AsyncImage(
                model = item.displayPosterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            if (item.isPlayed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Watched",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Era pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        movie.era,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                    )
                }

                Text(
                    "${movie.releaseYear}",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                )
            }

            Text(
                text = movie.title,
                color = if (item.isPlayed) VantafynColors.Ink.copy(alpha = 0.7f) else VantafynColors.Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = movie.timelineSetting,
                color = VantafynColors.Muted.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Action / Status
        Box(contentAlignment = Alignment.Center) {
            when {
                item.isPlayed -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            "Watched",
                            color = Color(0xFF4CAF50),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                isAvailable -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "Play",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Missing",
                            color = VantafynColors.Muted.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

package dev.vantafyn.feature.home.saw

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
fun SawWatchGuideDialog(
    movies: List<SawWatchItemUi>,
    isLoading: Boolean,
    errorMessage: String?,
    sortMode: SawSortMode,
    filterMode: SawFilterMode,
    onToggleSort: (SawSortMode) -> Unit,
    onSelectFilter: (SawFilterMode) -> Unit,
    onOpenMovie: (UUID) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val vantafynModalContainerColor = VantafynColors.Graphite.copy(alpha = 0.96f)
    
    // Compute stats
    val totalMovies = movies.size
    val watchedCount = movies.count { it.isPlayed }
    val onServerCount = movies.count { it.isOnServer }
    val notOnServerCount = movies.count { !it.isOnServer }
    val progressFraction = if (totalMovies > 0) watchedCount.toFloat() / totalMovies.toFloat() else 0f
    val progressPercent = (progressFraction * 100).toInt()

    // Filter and sort items
    val sortedMovies = remember(movies, sortMode) {
        when (sortMode) {
            SawSortMode.Timeline -> movies.sortedBy { it.movie.timelineOrder }
            SawSortMode.Release -> movies.sortedBy { it.movie.releaseOrder }
        }
    }

    val displayMovies = remember(sortedMovies, filterMode) {
        when (filterMode) {
            SawFilterMode.All -> sortedMovies
            SawFilterMode.Watched -> sortedMovies.filter { it.isPlayed }
            SawFilterMode.Unwatched -> sortedMovies.filter { !it.isPlayed }
            SawFilterMode.Missing -> sortedMovies.filter { !it.isOnServer }
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
                val serverSaw1 = movies.firstOrNull {
                    it.isOnServer && it.movie.id == "saw_2004" && !it.serverBackdropUrl.isNullOrBlank()
                }?.serverBackdropUrl
                val anyServer = movies.firstOrNull {
                    it.isOnServer && !it.serverBackdropUrl.isNullOrBlank()
                }?.serverBackdropUrl
                serverSaw1 ?: anyServer ?: "https://image.tmdb.org/t/p/w780/ok4ot3YbfDYZcINXf91JUfq3maB.jpg"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .heightIn(max = maxHeightPx * 0.94f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(vantafynModalContainerColor)
                    .vantafynAnimatedModalBorder(cornerRadius = 28.dp),
            ) {
                // Header Artwork Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                ) {
                    AsyncImage(
                        model = headerArtworkUrl,
                        contentDescription = "Saw Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Dark Gradient Overlay for text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.50f),
                                        Color.Black.copy(alpha = 0.15f),
                                        vantafynModalContainerColor.copy(alpha = 0.85f),
                                        vantafynModalContainerColor,
                                    ),
                                ),
                            ),
                    )

                    // Top Action Bar: SAW Badge + Refresh & Close Icons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Horizontal SAW badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF8B0000))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "SAW",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                            )
                        }

                        // Refresh and Close circular buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable(onClick = onRefresh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Title & Subtitle at bottom of header banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "SAW: The Complete Guide",
                            color = VantafynColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.titleLarge.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.9f),
                                    blurRadius = 10f,
                                ),
                            ),
                        )
                        Text(
                            "All 10 films — release & timeline order",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Inner content Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Progress Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.75f))
                            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Text(
                                        "Watched $watchedCount of $totalMovies films",
                                        color = VantafynColors.Ink,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                    )
                                }
                                Text(
                                    "$progressPercent%",
                                    color = Color(0xFF4ADE80),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progressFraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF4ADE80),
                                trackColor = Color.White.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Available on server: $onServerCount",
                                    color = VantafynColors.Muted,
                                    fontSize = 11.sp,
                                )
                                if (notOnServerCount > 0) {
                                    Text(
                                        "Not on server: $notOnServerCount",
                                        color = Color(0xFFFFB86C),
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }

                    // Controls Row: Sort & Filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Sort toggle
                        val isTimeline = sortMode == SawSortMode.Timeline
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(VantafynColors.Primary.copy(alpha = 0.18f))
                                .border(1.dp, VantafynColors.Primary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .clickable {
                                    onToggleSort(if (isTimeline) SawSortMode.Release else SawSortMode.Timeline)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (isTimeline) "📅 Timeline Order" else "🎬 Release Order",
                                color = VantafynColors.Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }

                        // Filter chips
                        SawFilterMode.entries.forEach { mode ->
                            val isSelected = filterMode == mode
                            val label = when (mode) {
                                SawFilterMode.All -> "All ($totalMovies)"
                                SawFilterMode.Watched -> "Watched ($watchedCount)"
                                SawFilterMode.Unwatched -> "Unwatched (${totalMovies - watchedCount})"
                                SawFilterMode.Missing -> "Missing ($notOnServerCount)"
                            }
                            val bgColor = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
                            val borderColor = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
                            val textColor = if (isSelected) VantafynColors.Ink else VantafynColors.Muted

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                    .clickable { onSelectFilter(mode) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    // Error message banner
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4A1818))
                                .padding(10.dp),
                        ) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFFB4B4),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    // Content List or Loading
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth(),
                    ) {
                        when {
                            isLoading && movies.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    VantafynLoadingIndicator("Syncing with your Jellyfin library...")
                                }
                            }

                            displayMovies.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "No movies match this filter.",
                                        color = VantafynColors.Muted,
                                        fontSize = 14.sp,
                                    )
                                }
                            }

                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp),
                                ) {
                                    itemsIndexed(
                                        items = displayMovies,
                                        key = { _, item -> item.movie.id },
                                    ) { _, item ->
                                        SawMovieCard(
                                            index = if (sortMode == SawSortMode.Timeline) item.movie.timelineOrder else item.movie.releaseOrder,
                                            item = item,
                                            onOpen = {
                                                if (item.isOnServer && item.mediaItemId != null) {
                                                    onDismiss()
                                                    onOpenMovie(item.mediaItemId)
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
        }
    }
}

@Composable
private fun SawMovieCard(
    index: Int,
    item: SawWatchItemUi,
    onOpen: () -> Unit,
) {
    val isClickable = item.isOnServer && item.mediaItemId != null
    val borderColor = when {
        item.isPlayed -> Color(0xFF4ADE80).copy(alpha = 0.28f)
        item.isOnServer -> VantafynColors.Primary.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.06f)
    }
    val backgroundColor = when {
        item.isPlayed -> Color(0xFF4ADE80).copy(alpha = 0.04f)
        item.isOnServer -> Color.White.copy(alpha = 0.04f)
        else -> Color.Black.copy(alpha = 0.25f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = isClickable, onClick = onOpen)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Order badge
        Box(
            modifier = Modifier
                .width(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = String.format("%02d", index),
                color = VantafynColors.Muted.copy(alpha = 0.75f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }

        // Poster
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.displayPosterUrl.isNotBlank()) {
                AsyncImage(
                    model = item.displayPosterUrl,
                    contentDescription = item.movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = VantafynColors.Muted,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Movie info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // Franchise badge & timeline tag
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = item.movie.franchise,
                        color = VantafynColors.Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = item.movie.timelineSetting,
                    color = VantafynColors.Muted.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Title
            Text(
                text = item.movie.title,
                color = VantafynColors.Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Year
            Text(
                text = item.movie.releaseYear.toString(),
                color = VantafynColors.Muted,
                fontSize = 11.sp,
            )
        }

        // Status badge
        Box(
            modifier = Modifier.padding(start = 4.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            when {
                item.isPlayed -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF4ADE80).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Watched",
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }

                item.isOnServer -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(VantafynColors.Primary.copy(alpha = 0.15f))
                            .border(1.dp, VantafynColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = VantafynColors.Primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "On Server",
                            color = VantafynColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFE28743).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFE28743).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Not on server",
                            color = Color(0xFFE28743),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

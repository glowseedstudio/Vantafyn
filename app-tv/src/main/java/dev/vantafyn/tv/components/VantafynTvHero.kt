package dev.vantafyn.tv.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinHeroMediaItem
import dev.vantafyn.core.ui.VantafynColors
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun VantafynTvHeroCarousel(
    items: List<JellyfinHeroMediaItem>,
    modifier: Modifier = Modifier,
    onPlay: (UUID) -> Unit = {},
    onDetails: (UUID) -> Unit = {},
) {
    val carouselItems = remember(items) {
        items.distinctBy { it.title.lowercase().trim() }
    }
    if (carouselItems.isEmpty()) return

    var currentIndex by remember(carouselItems) { mutableStateOf(0) }

    LaunchedEffect(carouselItems) {
        if (carouselItems.size > 1) {
            while (true) {
                delay(10_000)
                currentIndex = (currentIndex + 1) % carouselItems.size
            }
        }
    }

    val currentItem = carouselItems.getOrNull(currentIndex) ?: carouselItems.first()
    val backdropUrl = currentItem.backdropUrl ?: currentItem.posterUrl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        // --- 1. FULL ARTWORK BACKDROP WITH SOLID TOP (BOTH SIDES VISIBLE) & BOTTOM FADE ---
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            label = "hero_backdrop_crossfade",
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
                                    0.60f to Color.Black,
                                    0.80f to Color.Black.copy(alpha = 0.55f),
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
                        contentDescription = currentItem.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Soft subtle ambient vignette over text area (doesn't wipe out left artwork)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color(0xAA0B0E14),
                                    0.45f to Color(0x660B0E14),
                                    0.80f to Color.Transparent,
                                )
                            )
                        )
                )
            }
        }

        // --- 2. HERO CONTENT LAYER WITH STABLE BUTTONS (FOCUS NEVER JUMPS) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, end = 48.dp, top = 80.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            // Logo Artwork or Bold Title
            if (!currentItem.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = currentItem.logoUrl,
                    contentDescription = currentItem.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .heightIn(max = 84.dp)
                        .fillMaxWidth(0.48f),
                    alignment = Alignment.CenterStart,
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Text(
                    text = currentItem.title,
                    color = VantafynColors.Ink,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Metadata Row with Golden Star Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Gold star rating
                val rating = currentItem.communityRating
                if (rating != null && rating > 0f) {
                    Text(
                        text = "★ ${String.format("%.1f", rating)}",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val year = currentItem.year
                if (year != null && year > 0) {
                    Text(
                        text = year.toString(),
                        color = VantafynColors.Ink.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                val runtime = currentItem.runtimeMinutes
                if (runtime != null && runtime > 0) {
                    val hrs = runtime / 60
                    val mins = runtime % 60
                    val runtimeLabel = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                    Text(
                        text = "•  $runtimeLabel",
                        color = VantafynColors.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (!currentItem.officialRating.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentItem.officialRating.orEmpty(),
                            color = VantafynColors.Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (currentItem.genres.isNotEmpty()) {
                    Text(
                        text = "•  " + currentItem.genres.take(3).joinToString(", "),
                        color = VantafynColors.Muted.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Full Overview (up to 4 lines for 10-foot readability without truncation)
            if (!currentItem.overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = currentItem.overview.orEmpty(),
                    color = Color(0xFFD1D8E6),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.72f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: STABLE single instance, dynamic onClick referencing currentItem.id!
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VantafynTvGlassButton(
                    text = "Play",
                    icon = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    onClick = { onPlay(currentItem.id) },
                )

                VantafynTvGlassButton(
                    text = "Details",
                    icon = Icons.Rounded.Info,
                    isPrimary = false,
                    onClick = { onDetails(currentItem.id) },
                )
            }
        }

        // Dot indicators at bottom-end
        if (carouselItems.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                carouselItems.take(6).forEachIndexed { index, _ ->
                    val selected = index == currentIndex.coerceAtMost(5)
                    Box(
                        modifier = Modifier
                            .width(if (selected) 20.dp else 7.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = if (selected) 0.85f else 0.30f)),
                    )
                }
            }
        }
    }
}

@Composable
fun VantafynTvFallbackHero(
    serverName: String?,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(start = 36.dp, end = 48.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Welcome to Vantafyn",
                color = VantafynColors.Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Text(
                text = "Connected to ${serverName?.ifBlank { "Jellyfin Server" } ?: "Jellyfin Server"}. Browse your libraries below.",
                color = VantafynColors.Muted,
                fontSize = 14.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            VantafynTvGlassButton(
                text = "Explore Libraries",
                icon = Icons.Rounded.PlayArrow,
                isPrimary = true,
                onClick = onExplore,
            )
        }
    }
}

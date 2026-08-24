package dev.vantafyn.tv.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.util.UUID

@Composable
fun VantafynTvHero(
    item: JellyfinHeroMediaItem,
    modifier: Modifier = Modifier,
    onPlay: (UUID) -> Unit = {},
    onDetails: (UUID) -> Unit = {},
) {
    val backdropUrl = item.backdropUrl ?: item.posterUrl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        // --- 1. HERO BACKDROP WITH TOP-ALIGNED ARTWORK & DstIn BOTTOM FADE ---
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
                                0.50f to Color.Black,
                                0.72f to Color.Black.copy(alpha = 0.65f),
                                0.88f to Color.Black.copy(alpha = 0.20f),
                                1.00f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
        ) {
            if (!backdropUrl.isNullOrBlank()) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Left horizontal gradient scrim for text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to VantafynColors.Graphite.copy(alpha = 0.95f),
                                0.35f to VantafynColors.Graphite.copy(alpha = 0.80f),
                                0.65f to VantafynColors.Graphite.copy(alpha = 0.40f),
                                1.00f to Color.Transparent,
                            )
                        )
                    )
            )
        }

        // --- 2. HERO CONTENT OVERLAY ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, end = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start,
        ) {
            // Logo Artwork or Bold Title Fallback
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .heightIn(max = 76.dp)
                        .fillMaxWidth(0.42f),
                    alignment = Alignment.CenterStart,
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Text(
                    text = item.title,
                    color = VantafynColors.Ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Metadata Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val year = item.year
                if (year != null && year > 0) {
                    Text(
                        text = year.toString(),
                        color = VantafynColors.Muted,
                        fontSize = 13.sp,
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (!item.officialRating.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.officialRating.orEmpty(),
                            color = VantafynColors.Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                val rating = item.communityRating
                if (rating != null && rating > 0f) {
                    Text(
                        text = "★ ${String.format("%.1f", rating)}",
                        color = Color(0xFFFFD700),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (item.genres.isNotEmpty()) {
                    Text(
                        text = "•  " + item.genres.take(2).joinToString(", "),
                        color = VantafynColors.Muted.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Short Overview (Max 2 lines for 10-foot readability)
            if (!item.overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.overview.orEmpty(),
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.55f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VantafynTvGlassButton(
                    text = "Play",
                    icon = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    onClick = { onPlay(item.id) },
                )

                VantafynTvGlassButton(
                    text = "Details",
                    icon = Icons.Rounded.Info,
                    isPrimary = false,
                    onClick = { onDetails(item.id) },
                )
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

package dev.vantafyn.tv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.tv.components.VantafynTvGlassButton

@Composable
fun TvDetailScreen(
    detail: JellyfinMediaDetail?,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit = {},
    onPlayFromStart: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
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

    val backdropUrl = detail.backdropUrl ?: "${session.server.url.trimEnd('/')}/Items/${detail.id}/Images/Backdrop/0"

    Box(modifier = modifier.fillMaxSize()) {
        // --- 1. FULLSCREEN BACKDROP WITH HARDWARE FADE ---
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
                                0.60f to Color.Black,
                                0.82f to Color.Black.copy(alpha = 0.60f),
                                1.00f to Color.Transparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
        ) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Horizontal scrim for text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to VantafynColors.Graphite.copy(alpha = 0.95f),
                                0.40f to VantafynColors.Graphite.copy(alpha = 0.80f),
                                0.70f to VantafynColors.Graphite.copy(alpha = 0.35f),
                                1.00f to Color.Transparent,
                            )
                        )
                    )
            )

            // Top vignette
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(VantafynColors.Graphite.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
            )
        }

        // --- 2. DETAIL CONTENT LAYER ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = detail.title,
                color = VantafynColors.Ink,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            if (!detail.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.subtitle.orEmpty(),
                    color = VantafynColors.Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Chips Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val year = detail.year
                if (year != null && year > 0) {
                    Text(
                        text = year.toString(),
                        color = VantafynColors.Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                val runtime = detail.runtimeMinutes
                if (runtime != null && runtime > 0) {
                    val hrs = runtime / 60
                    val mins = runtime % 60
                    val runtimeLabel = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                    Text(
                        text = "•  $runtimeLabel",
                        color = VantafynColors.Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (!detail.officialRating.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = detail.officialRating.orEmpty(),
                            color = VantafynColors.Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                val rating = detail.communityRating
                if (rating != null && rating > 0f) {
                    Text(
                        text = "★ ${String.format("%.1f", rating)}",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (detail.genres.isNotEmpty()) {
                    Text(
                        text = "•  " + detail.genres.take(3).joinToString(", "),
                        color = VantafynColors.Muted.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Overview Text
            if (!detail.overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = detail.overview.orEmpty(),
                    color = Color(0xFFCBD5E1),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth(0.60f),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hasProgress = detail.progress != null && (detail.progress ?: 0f) > 0f
                VantafynTvGlassButton(
                    text = if (hasProgress) "Resume" else "Play",
                    icon = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    onClick = onPlay,
                )

                if (hasProgress) {
                    VantafynTvGlassButton(
                        text = "Start from beginning",
                        icon = Icons.Rounded.Replay,
                        isPrimary = false,
                        onClick = onPlayFromStart,
                    )
                }

                VantafynTvGlassButton(
                    text = if (detail.isFavorite) "Favorited" else "Favorite",
                    icon = if (detail.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    isPrimary = false,
                    onClick = onToggleFavorite,
                )
            }
        }
    }
}

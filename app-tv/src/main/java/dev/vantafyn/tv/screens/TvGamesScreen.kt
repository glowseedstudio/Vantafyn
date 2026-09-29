package dev.vantafyn.tv.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import android.content.Context
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.vantafyn.core.media.games.GameHubSoundManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.games.GameDetailModal
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvScreenScaffold
import dev.vantafyn.tv.components.VantafynTvSectionHeader

@Composable
fun TvGamesScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    onSelectSystem: (GameSystem?) -> Unit,
    onOpenGame: (GameSummary) -> Unit,
    onPlayGame: (GameDetail) -> Unit,
    onDismissGameDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> GameHubSoundManager.resume(context)
                Lifecycle.Event.ON_PAUSE -> GameHubSoundManager.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        GameHubSoundManager.fadeIn(context)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            GameHubSoundManager.fadeOut()
        }
    }

    VantafynTvScreenScaffold(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            VantafynTvSectionHeader(
                title = "Retro Games",
                subtitle = "Play classic console titles directly on your TV",
            )

            // System Category Filter Tabs
            if (state.gameSystems.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                ) {
                    item {
                        VantafynTvGlassButton(
                            text = "All Systems (${state.gameSystems.sumOf { it.gameCount }})",
                            onClick = { onSelectSystem(null) },
                            isPrimary = state.selectedGameSystem == null,
                            compact = true,
                        )
                    }
                    items(state.gameSystems, key = { it.id }) { system ->
                        VantafynTvGlassButton(
                            text = "${system.name} (${system.gameCount})",
                            onClick = { onSelectSystem(system) },
                            isPrimary = state.selectedGameSystem?.id == system.id,
                            compact = true,
                        )
                    }
                }
            }

            // Games Grid or Empty/Loading State
            if (state.isLoadingGames) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(54.dp),
                        color = VantafynColors.Primary,
                        strokeWidth = 3.dp,
                    )
                }
            } else if (state.gamesList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(64.dp),
                        )
                        Text(
                            text = "No games found in this category",
                            color = VantafynColors.Muted,
                            fontSize = 16.sp,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 190.dp),
                    contentPadding = PaddingValues(bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.gamesList, key = { it.id }) { game ->
                        TvGameCard(
                            game = game,
                            onClick = { onOpenGame(game) },
                        )
                    }
                }
            }
        }

        // Game Detail Modal on TV
        GameDetailModal(
            game = state.activeGameDetail,
            onDismiss = onDismissGameDetail,
            onPlay = onPlayGame,
        )
    }
}

@Composable
private fun TvGameCard(
    game: GameSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "TvGameCardScale",
    )

    val borderModifier = if (isFocused) {
        Modifier.border(
            width = 2.dp,
            brush = Brush.horizontalGradient(
                listOf(VantafynColors.Primary, VantafynColors.Secondary)
            ),
            shape = RoundedCornerShape(18.dp),
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = Color(0x1EFFFFFF),
            shape = RoundedCornerShape(18.dp),
        )
    }

    val context = LocalContext.current
    val effectiveBoxart = remember(game.id, game.boxartUrl) {
        val local = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
            .getString("boxart_${game.id}", null)
        GameBoxartScraper.convertToCdnUrl(local ?: game.boxartUrl)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (isFocused) Color(0xFF1E1E28) else Color(0xFF13131A))
            .then(borderModifier)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(12.dp),
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Boxart Image container or Fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D0F18)),
                contentAlignment = Alignment.Center,
            ) {
                if (!effectiveBoxart.isNullOrBlank()) {
                    AsyncImage(
                        model = effectiveBoxart,
                        contentDescription = game.cleanTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        tint = VantafynColors.Muted.copy(alpha = 0.35f),
                        modifier = Modifier.size(52.dp),
                    )
                }

                // System & Region Tag Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xDD0A0E1A))
                        .border(0.5.dp, Color(0x4421D8FF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = game.systemId.uppercase(),
                        color = VantafynColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }

                if (game.region != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xDD0A0E1A))
                            .border(0.5.dp, Color(0x44E026FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = game.region.orEmpty(),
                            color = VantafynColors.Secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            // Title
            Text(
                text = game.cleanTitle,
                color = VantafynColors.Ink,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp),
            )

            // Bottom: Size & Play Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatGameFileSize(game.sizeBytes),
                    color = VantafynColors.Muted,
                    fontSize = 12.sp,
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFocused) {
                                Brush.horizontalGradient(
                                    listOf(VantafynColors.Primary, VantafynColors.Secondary)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0x3321D8FF), Color(0x33E026FF))
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isFocused) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private fun formatGameFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / (1024f * 1024f)
    return if (mb < 1f) {
        "${(bytes / 1024f).toInt()} KB"
    } else {
        "%.1f MB".format(mb)
    }
}

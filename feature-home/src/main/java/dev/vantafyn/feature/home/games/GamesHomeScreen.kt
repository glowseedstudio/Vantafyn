package dev.vantafyn.feature.home.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GamePlayTracker
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.RecentGameRecord
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@Composable
fun GamesHomeScreen(
    userName: String,
    userImageUrl: String?,
    systems: List<GameSystem>,
    games: List<GameSummary>,
    allGames: List<GameSummary> = games,
    recentGames: List<RecentGameRecord>,
    totalPlayTimeMs: Long,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onOpenGame: (GameSummary) -> Unit,
    onRemoveRecentGame: (String) -> Unit = {},
    onSelectSystem: (GameSystem) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalGamesCount = remember(allGames, games, systems) {
        if (allGames.isNotEmpty()) allGames.size
        else if (games.isNotEmpty()) games.size
        else systems.sumOf { it.gameCount }
    }

    val formattedPlaytime = remember(totalPlayTimeMs) {
        GamePlayTracker.formatPlayTime(totalPlayTimeMs)
    }

    val pullToRefreshState = rememberPullToRefreshState()

    GameScreenReveal(
        key = "games_home_screen",
        modifier = modifier.fillMaxSize(),
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = pullToRefreshState,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color(0xFF1E1E28),
                color = Color(0xFF00E5FF),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
        // 1. Top Bar: Back Button & Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CompactBackButton(onClick = onNavigateBack)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Games Hub",
                        color = VantafynColors.Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (systems.isNotEmpty()) "${systems.size} systems • $totalGamesCount games" else "Retro arcade & collection",
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // 2. Centered Big Gamer Profile Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.6f),
                                Color(0xFF9D00FF).copy(alpha = 0.45f),
                                Color(0xFFFF2A85).copy(alpha = 0.45f),
                            ),
                        ),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Large Avatar with glowing cyber ring
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF9D00FF), Color(0xFFFF2A85)),
                            ),
                            shape = CircleShape,
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E28)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!userImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userImageUrl,
                            contentDescription = userName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = userName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        )
                    }
                }

                // Centered User Name
                Text(
                    text = userName,
                    color = VantafynColors.Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Centered "RETRO VAULT PILOT" badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF00E5FF), CircleShape),
                    )
                    Text(
                        text = "RETRO VAULT PILOT",
                        color = Color(0xFF8FE7FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }

        // 2. Metrics Statistics Grid (4 Key Metrics)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GamerStatCard(
                        title = "TOTAL PLAYTIME",
                        value = formattedPlaytime,
                        icon = Icons.Rounded.Schedule,
                        accentColor = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f),
                    )
                    GamerStatCard(
                        title = "GAMES",
                        value = "$totalGamesCount",
                        icon = Icons.Rounded.SportsEsports,
                        accentColor = Color(0xFF9D00FF),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GamerStatCard(
                        title = "SYSTEMS",
                        value = "${systems.size}",
                        icon = Icons.Rounded.Dns,
                        accentColor = Color(0xFFFF2A85),
                        modifier = Modifier.weight(1f),
                    )
                    GamerStatCard(
                        title = "CLOUD SAVES",
                        value = "SYNCED",
                        icon = Icons.Rounded.CloudDone,
                        accentColor = Color(0xFF00E676),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 3. Continue Playing / Jump Back In Carousel
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF00E5FF), CircleShape),
                    )
                    Text(
                        text = "CONTINUE PLAYING",
                        color = VantafynColors.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }

                if (recentGames.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recentGames, key = { it.id }) { record ->
                            RecentGameCard(
                                record = record,
                                onClick = { onOpenGame(record.toGameSummary()) },
                                onLongPress = { onRemoveRecentGame(record.id) },
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = VantafynColors.Muted,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                text = "Your Retro Journey Begins Here",
                                color = VantafynColors.Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Pick any title from your library or roll the dice below to start playing!",
                                color = VantafynColors.Muted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // 4. Consoles & Systems Quick Jump Grid
        if (systems.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF9D00FF), CircleShape),
                        )
                        Text(
                            text = "CONSOLES & SYSTEMS",
                            color = VantafynColors.Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        systems.chunked(2).forEach { rowSystems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowSystems.forEach { sys ->
                                    SystemQuickCard(
                                        system = sys,
                                        onClick = { onSelectSystem(sys) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (rowSystems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. "Roll the Dice" / Surprise Me Random Game Picker
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E1435).copy(alpha = 0.85f),
                                Color(0xFF101935).copy(alpha = 0.85f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFF2A85).copy(alpha = 0.6f),
                                Color(0xFF00E5FF).copy(alpha = 0.6f),
                            ),
                        ),
                        RoundedCornerShape(18.dp),
                    )
                    .clickable {
                        if (games.isNotEmpty()) {
                            val randomGame = games.random()
                            onOpenGame(randomGame)
                        }
                    }
                    .padding(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF2A85), Color(0xFF9D00FF)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Casino,
                            contentDescription = "Random Pick",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Roll the Dice",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Can't decide? Let fate pick a classic game from your library.",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(VantafynGradients.accentHorizontal())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "ROLL",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
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
private fun GamerStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column {
                Text(
                    text = title,
                    color = VantafynColors.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                )
                Text(
                    text = value,
                    color = VantafynColors.Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RecentGameCard(
    record: RecentGameRecord,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val effectiveBoxart = remember(record.id, record.boxartUrl) {
            val local = context.getSharedPreferences("vantafyn_retro_settings", android.content.Context.MODE_PRIVATE)
                .getString("boxart_${record.id}", null)
            GameBoxartScraper.convertToCdnUrl(local ?: record.boxartUrl)
        }
        Box(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E1E28))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
        ) {
            if (!effectiveBoxart.isNullOrBlank()) {
                AsyncImage(
                    model = effectiveBoxart,
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF22283A), Color(0xFF111420)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            // System Badge in top right (matches media card watched/unwatched badge style)
            val glassShape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(glassShape)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(VantafynGradients.AccentColors),
                        shape = glassShape,
                    )
                    .background(VantafynColors.Graphite.copy(alpha = 0.92f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = record.systemId.uppercase(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // Quick play badge overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(VantafynGradients.accentHorizontal()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Resume",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = record.title,
            color = VantafynColors.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = if (record.playTimeMs > 0) "Played ${GamePlayTracker.formatPlayTime(record.playTimeMs)}" else "Recently added",
            color = VantafynColors.Muted,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SystemQuickCard(
    system: GameSystem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (system.officialLogoUrl.isNotBlank()) {
                AsyncImage(
                    model = system.officialLogoUrl,
                    contentDescription = system.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(),
                )
            } else {
                Text(
                    text = system.displayName,
                    color = VantafynColors.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = "${system.gameCount} ${if (system.gameCount == 1) "title" else "titles"}",
            color = VantafynColors.Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

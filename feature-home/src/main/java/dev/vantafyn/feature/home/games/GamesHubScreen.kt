package dev.vantafyn.feature.home.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradientIcon
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.feature.home.CompactBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.window.Dialog

@Composable
fun GamesHubScreen(
    systems: List<GameSystem>,
    games: List<GameSummary>,
    allGames: List<GameSummary> = games,
    selectedSystem: GameSystem?,
    downloadedGameKeys: Set<String> = emptySet(),
    isPokemonVaultAvailable: Boolean = false,
    onOpenPokemonVault: () -> Unit = {},
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSelectSystem: (GameSystem?) -> Unit,
    onOpenGame: (GameSummary) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var isOfflineFilterSelected by remember { mutableStateOf(false) }
    var gameActionTarget by remember { mutableStateOf<GameSummary?>(null) }
    var identifyGameTarget by remember { mutableStateOf<GameSummary?>(null) }
    var boxartRevision by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
    }

    val basePool = if (isOfflineFilterSelected) {
        allGames.filter { g ->
            val safeId = g.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val safeToken = g.token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            downloadedGameKeys.contains(safeId) || (safeToken.isNotBlank() && downloadedGameKeys.contains(safeToken))
        }
    } else {
        games
    }

    val totalLibraryGamesCount = remember(allGames, games, systems) {
        if (allGames.isNotEmpty()) allGames.size
        else if (games.isNotEmpty()) games.size
        else systems.sumOf { it.gameCount }
    }

    val downloadedCount = remember(allGames, downloadedGameKeys) {
        allGames.count { g ->
            val safeId = g.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val safeToken = g.token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            downloadedGameKeys.contains(safeId) || (safeToken.isNotBlank() && downloadedGameKeys.contains(safeToken))
        }
    }

    val filteredGames = remember(basePool, searchQuery) {
        if (searchQuery.isBlank()) basePool
        else basePool.filter { it.cleanTitle.contains(searchQuery, ignoreCase = true) || it.title.contains(searchQuery, ignoreCase = true) }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    GameScreenReveal(
        key = selectedSystem?.id ?: (if (isOfflineFilterSelected) "downloaded" else "all"),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(top = if (isLandscape) 4.dp else 12.dp),
        ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CompactBackButton(
                    onClick = {
                        if (isOfflineFilterSelected) {
                            isOfflineFilterSelected = false
                        } else if (selectedSystem != null) {
                            onSelectSystem(null)
                        } else {
                            onBack()
                        }
                    },
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOfflineFilterSelected) "Downloaded Games" else if (selectedSystem != null) selectedSystem.displayName else "All Consoles",
                        color = VantafynColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isOfflineFilterSelected) {
                            "$downloadedCount downloaded offline"
                        } else if (selectedSystem != null) {
                            "${filteredGames.size} of $totalLibraryGamesCount games"
                        } else if (searchQuery.isNotBlank()) {
                            "${filteredGames.size} matching \"$searchQuery\""
                        } else {
                            "$totalLibraryGamesCount games across ${systems.size} systems"
                        },
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                    )
                }

                Box(modifier = Modifier.width(260.dp)) {
                    VantafynTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Search games...",
                        placeholder = "Search games...",
                        leadingIcon = {
                            VantafynGradientIcon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = VantafynColors.Muted,
                                    )
                                }
                            }
                        } else null,
                    )
                }
            }
        } else {
            // Top Bar: Back Button, Title, and Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CompactBackButton(
                    onClick = {
                        if (isOfflineFilterSelected) {
                            isOfflineFilterSelected = false
                        } else if (selectedSystem != null) {
                            onSelectSystem(null)
                        } else {
                            onBack()
                        }
                    },
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOfflineFilterSelected) "Downloaded Games" else if (selectedSystem != null) selectedSystem.displayName else "All Consoles",
                        color = VantafynColors.Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isOfflineFilterSelected) {
                            "Showing $downloadedCount downloaded offline games"
                        } else if (selectedSystem != null) {
                            "Showing ${filteredGames.size} of $totalLibraryGamesCount games"
                        } else if (searchQuery.isNotBlank()) {
                            "Found ${filteredGames.size} games matching \"$searchQuery\""
                        } else {
                            "$totalLibraryGamesCount games available across ${systems.size} systems"
                        },
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                    )
                }
            }

            // Search Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                VantafynTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search games...",
                    placeholder = "Search games...",
                    leadingIcon = {
                        VantafynGradientIcon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = VantafynColors.Muted,
                                )
                            }
                        }
                    } else null,
                )
            }
        }

        // Systems Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SystemFilterPill(
                    label = "All Systems",
                    count = totalLibraryGamesCount,
                    isSelected = selectedSystem == null && !isOfflineFilterSelected,
                    onClick = {
                        isOfflineFilterSelected = false
                        onSelectSystem(null)
                    },
                )
            }
            if (isPokemonVaultAvailable) {
                item {
                    PokemonVaultFilterPill(
                        onClick = onOpenPokemonVault,
                    )
                }
            }
            if (downloadedCount > 0) {
                item {
                    SystemFilterPill(
                        label = "Downloaded",
                        count = downloadedCount,
                        isSelected = isOfflineFilterSelected,
                        onClick = {
                            isOfflineFilterSelected = true
                            onSelectSystem(null)
                        },
                    )
                }
            }
            items(systems) { system ->
                SystemFilterPill(
                    label = system.displayName,
                    count = system.gameCount,
                    isSelected = selectedSystem?.id == system.id && !isOfflineFilterSelected,
                    onClick = {
                        isOfflineFilterSelected = false
                        onSelectSystem(system)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val pullToRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = pullToRefreshState,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
            val isBrowsingConsoles = selectedSystem == null && !isOfflineFilterSelected && searchQuery.isBlank()

            if (isLoading && !isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = VantafynColors.Primary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            } else if (isBrowsingConsoles) {
                if (systems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = VantafynColors.Muted,
                                modifier = Modifier.size(56.dp),
                            )
                            Text(
                                text = "No consoles found",
                                color = VantafynColors.Muted,
                                fontSize = 15.sp,
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = if (isLandscape) 24.dp else 140.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(systems, key = { it.id }) { system ->
                            SystemQuickCard(
                                system = system,
                                onClick = { onSelectSystem(system) },
                            )
                        }
                    }
                }
            } else if (filteredGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(56.dp),
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No games matching \"$searchQuery\"" else "No games found in this system",
                            color = VantafynColors.Muted,
                            fontSize = 15.sp,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = if (isLandscape) 24.dp else 140.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredGames, key = { it.id }) { game ->
                        val safeId = remember(game.id) { game.id.replace(Regex("[^a-zA-Z0-9_-]"), "_") }
                        val safeToken = remember(game.token) { game.token.replace(Regex("[^a-zA-Z0-9_-]"), "_") }
                        val isDownloaded = downloadedGameKeys.contains(game.id) ||
                            downloadedGameKeys.contains(safeId) ||
                            (game.token.isNotBlank() && (downloadedGameKeys.contains(game.token) || downloadedGameKeys.contains(safeToken)))

                        GameCard(
                            game = game,
                            isDownloaded = isDownloaded,
                            boxartRevision = boxartRevision,
                            onClick = { onOpenGame(game) },
                            onLongPress = { gameActionTarget = game },
                        )
                    }
                }
            }
        }
    }

    gameActionTarget?.let { game ->
        GameLibraryActionSheet(
            game = game,
            hasCustomArtwork = prefs.contains("boxart_${game.id}"),
            onDismiss = { gameActionTarget = null },
            onIdentify = {
                gameActionTarget = null
                identifyGameTarget = game
            },
            onClearArtwork = {
                prefs.edit().remove("boxart_${game.id}").apply()
                boxartRevision++
                gameActionTarget = null
            },
        )
    }

    identifyGameTarget?.let { game ->
        IdentifyGameArtworkDialog(
            game = game,
            currentBoxartUrl = GameBoxartScraper.convertToCdnUrl(prefs.getString("boxart_${game.id}", null) ?: game.boxartUrl),
            onDismiss = { identifyGameTarget = null },
            onSelectArtwork = { url ->
                prefs.edit().putString("boxart_${game.id}", url).apply()
                boxartRevision++
                identifyGameTarget = null
            },
        )
    }
}
}

@Composable
private fun SystemFilterPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgModifier = if (isSelected) {
        Modifier.background(
            VantafynGradients.accentHorizontal(),
            RoundedCornerShape(20.dp),
        )
    } else {
        Modifier
            .background(Color(0xFF16161D).copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
    }

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else VantafynColors.Ink.copy(alpha = 0.85f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
            )
            if (count > 0) {
                Text(
                    text = "($count)",
                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else VantafynColors.Muted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun PokemonVaultFilterPill(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF16161D).copy(alpha = 0.85f), RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = VantafynGradients.accentHorizontal(),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CatchingPokemon,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Pokémon Vault",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun GameLibraryActionSheet(
    game: GameSummary,
    hasCustomArtwork: Boolean,
    onDismiss: () -> Unit,
    onIdentify: () -> Unit,
    onClearArtwork: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF24243A),
                            Color(0xFF171B2B),
                            Color(0xFF0D101B),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = game.cleanTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Game library actions",
                color = VantafynColors.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )

            GameActionRow(
                title = "Identify game artwork",
                subtitle = "Search Libretro covers and choose the right box art",
                onClick = onIdentify,
            )
            if (hasCustomArtwork) {
                GameActionRow(
                    title = "Clear custom artwork",
                    subtitle = "Return this title to the server or default cover",
                    onClick = onClearArtwork,
                    accent = Color(0xFFFFB020),
                )
            }
            GameActionRow(
                title = "Cancel",
                subtitle = "Keep everything as it is",
                onClick = onDismiss,
                accent = VantafynColors.Muted,
            )
        }
    }
}

@Composable
private fun GameActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Color = Color(0xFF00E5FF),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = VantafynColors.Muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun IdentifyGameArtworkDialog(
    game: GameSummary,
    currentBoxartUrl: String?,
    onDismiss: () -> Unit,
    onSelectArtwork: (String) -> Unit,
) {
    var query by remember(game.id) { mutableStateOf(game.cleanTitle.ifBlank { game.title }) }
    var catalog by remember(game.id) { mutableStateOf<List<GameBoxartScraper.IndexedEntry>>(emptyList()) }
    var isLoading by remember(game.id) { mutableStateOf(true) }
    var errorMessage by remember(game.id) { mutableStateOf<String?>(null) }
    val platform = remember(game.systemId) { GameBoxartScraper.resolvePlatform(game.systemId) }
    val candidates = remember(query, catalog) {
        GameBoxartScraper.searchCandidates(query, catalog, limit = 30)
    }

    LaunchedEffect(game.id, platform) {
        if (platform == null) {
            isLoading = false
            errorMessage = "Artwork matching is not available for ${game.systemId.uppercase()} yet."
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        catalog = withContext(Dispatchers.IO) {
            GameBoxartScraper.getSystemIndex(platform)
        }
        isLoading = false
        if (catalog.isEmpty()) errorMessage = "Could not load the ${platform.libretroName} artwork catalog."
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF20263D),
                            Color(0xFF151A2A),
                            Color(0xFF090C16),
                        ),
                    ),
                )
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D101B))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!currentBoxartUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentBoxartUrl,
                            contentDescription = game.cleanTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Identify Artwork",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = game.cleanTitle,
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Muted,
                    )
                }
            }

            VantafynTextField(
                value = query,
                onValueChange = { query = it },
                label = "Search title",
                placeholder = "Type a game title...",
                leadingIcon = {
                    VantafynGradientIcon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(30.dp))
                            Text(
                                text = "Loading ${platform?.libretroName ?: "artwork"} catalog...",
                                color = VantafynColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = Color(0xFFFFB020),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                candidates.isEmpty() -> {
                    Text(
                        text = "No artwork matches found. Try a shorter title or remove region/version words.",
                        color = VantafynColors.Muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                else -> {
                    Text(
                        text = "Choose a cover. This only changes Vantafyn artwork for this game.",
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    LazyColumn(
                        modifier = Modifier.height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(candidates, key = { it.rawFilename }) { candidate ->
                            val cdnUrl = platform?.let { GameBoxartScraper.buildCdnUrl(it, candidate.rawFilename) }.orEmpty()
                            GameArtworkCandidateRow(
                                title = candidate.rawFilename.removeSuffix(".png"),
                                imageUrl = cdnUrl,
                                onClick = { if (cdnUrl.isNotBlank()) onSelectArtwork(cdnUrl) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameArtworkCandidateRow(
    title: String,
    imageUrl: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.075f),
                        Color.White.copy(alpha = 0.035f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D101B)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Libretro cover art",
                color = Color(0xFF00E5FF),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun GameCard(
    game: GameSummary,
    isDownloaded: Boolean = false,
    boxartRevision: Int = 0,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14141B).copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(10.dp),
    ) {
        val context = LocalContext.current
        val effectiveBoxart = remember(game.id, game.boxartUrl, boxartRevision) {
            val local = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                .getString("boxart_${game.id}", null)
            GameBoxartScraper.convertToCdnUrl(local ?: game.boxartUrl)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Boxart Image container or Fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(12.dp))
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
                        modifier = Modifier.size(44.dp),
                    )
                }

                // System Tag in top right (matches media card watched/unwatched badge style)
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
                        text = game.systemId.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                    )
                }

                if (game.region != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(glassShape)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(Color(0xFF21D8FF).copy(alpha = 0.6f), Color(0xFFE026FF).copy(alpha = 0.6f))),
                                shape = glassShape,
                            )
                            .background(VantafynColors.Graphite.copy(alpha = 0.92f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = game.region.orEmpty(),
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                        )
                    }
                }

                if (isDownloaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xEE0A121A))
                            .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DownloadDone,
                                contentDescription = "Downloaded for Offline Play",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = "OFFLINE",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }

            // Game Clean Title
            Text(
                text = game.cleanTitle,
                color = VantafynColors.Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(36.dp),
            )

            // Play Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatGameFileSize(game.sizeBytes),
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(VantafynGradients.accentHorizontal()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
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

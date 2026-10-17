package dev.vantafyn.feature.home.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import dev.vantafyn.feature.home.games.screensaver.GameHubScreensaver
import dev.vantafyn.feature.home.games.dualscreen.GameDualScreenManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import dev.vantafyn.feature.home.games.grid.ConsoleStatusTray
import dev.vantafyn.feature.home.games.grid.ChannelTileItem
import dev.vantafyn.feature.home.games.grid.GridTileType
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
import androidx.compose.foundation.layout.offset
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
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import android.view.KeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GamePlayTracker
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.jellyfin.RecentGameRecord
import androidx.compose.runtime.setValue
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynSoundEffects
import dev.vantafyn.feature.home.CompactBackButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import dev.vantafyn.feature.home.games.grid.VantafynChannelGrid
import dev.vantafyn.feature.home.games.grid.VantafynGridPersistence

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
    onOpenPokemonVault: () -> Unit = {},
    onOpenSaves: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSelectLibrary: () -> Unit = {},
    onSelectSearch: () -> Unit = {},
    onNavigateBack: () -> Unit,
    isOffline: Boolean = false,
    isGamesOnlyMode: Boolean = false,
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
    var pendingRecentRemoval by remember { mutableStateOf<RecentGameRecord?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("vantafyn_retro_settings", android.content.Context.MODE_PRIVATE)
    }
    LaunchedEffect(userName, userImageUrl) {
        prefs.edit().apply {
            if (userName.isNotBlank()) putString("user_profile_name", userName)
            if (!userImageUrl.isNullOrBlank()) putString("user_profile_avatar", userImageUrl)
            apply()
        }
    }
    var isChannelView by remember {
        mutableStateOf(prefs.getBoolean("games_home_channel_view", true))
    }
    var channelItems by remember(games, systems, recentGames) {
        mutableStateOf(
            VantafynGridPersistence.loadGrid(context, allGames.ifEmpty { games }, systems, recentGames)
        )
    }

    val profileFocusRequester = remember { FocusRequester() }
    val channelsToggleFocusRequester = remember { FocusRequester() }
    val overviewToggleFocusRequester = remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }
    val isGamepadConnected = dev.vantafyn.feature.home.games.grid.rememberIsGamepadConnected()

    val activity = context as? android.app.Activity
    DisposableEffect(isGamesOnlyMode, isChannelView) {
        if (isGamesOnlyMode && isChannelView && activity != null) {
            val window = activity.window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            onDispose {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        } else {
            onDispose {}
        }
    }

    val dualScreenManager = remember(context) { GameDualScreenManager(context) }
    DisposableEffect(dualScreenManager, isGamesOnlyMode) {
        if (isGamesOnlyMode) {
            dualScreenManager.start()
        }
        onDispose {
            dualScreenManager.stop()
        }
    }

    var lastFocusedChannelItem by remember { mutableStateOf<ChannelTileItem?>(null) }
    LaunchedEffect(lastFocusedChannelItem, allGames, games, systems, channelItems, isGamesOnlyMode) {
        if (isGamesOnlyMode) {
            val targetItem = lastFocusedChannelItem
                ?: channelItems.firstOrNull { it != null && it.type != GridTileType.EMPTY_CHANNEL }
            if (targetItem != null) {
                dualScreenManager.updateSelection(targetItem, allGames.ifEmpty { games }, systems)
            }
        }
    }

    val screensaverEnabled = prefs.getBoolean("games_screensaver_enabled", true)
    val screensaverTimeoutMs = prefs.getLong("games_screensaver_timeout_ms", 300_000L)
    val screensaverSlideMs = prefs.getLong("games_screensaver_slide_ms", 8_000L)
    val screensaverShowClock = prefs.getBoolean("games_screensaver_show_clock", true)

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isScreensaverActive by remember { mutableStateOf(false) }

    fun recordActivity() {
        lastInteractionTime = System.currentTimeMillis()
        if (isScreensaverActive) {
            isScreensaverActive = false
        }
    }

    LaunchedEffect(lastInteractionTime, screensaverEnabled, isChannelView, isGamesOnlyMode, screensaverTimeoutMs, pendingRecentRemoval) {
        if (!screensaverEnabled || !isChannelView || !isGamesOnlyMode || pendingRecentRemoval != null) {
            isScreensaverActive = false
            return@LaunchedEffect
        }
        while (true) {
            val elapsed = System.currentTimeMillis() - lastInteractionTime
            val remaining = screensaverTimeoutMs - elapsed
            if (remaining <= 0L) {
                isScreensaverActive = true
                break
            }
            delay(remaining.coerceAtLeast(1000L))
        }
    }

    GameScreenReveal(
        key = "games_home_screen",
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                    recordActivity()
                    false
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        recordActivity()
                    }
                },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isGamesOnlyMode && isChannelView) {
                            Modifier.padding(top = 8.dp)
                        } else {
                            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        }
                    ),
            ) {
                // Console Top Bar: Player Avatar (Left) + View Switcher & Hardware Status Tray (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: Back Button (if not games-only mode) + User Profile Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!isGamesOnlyMode) {
                            CompactBackButton(onClick = onNavigateBack)
                        }

                        // User Profile Avatar with Controller Focus
                        var isProfileFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .focusRequester(profileFocusRequester)
                                .onFocusChanged { isProfileFocused = it.isFocused }
                                .focusable()
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E28))
                                .border(
                                    width = if (isGamepadConnected && isProfileFocused) 2.5.dp else 1.5.dp,
                                    brush = if (isGamepadConnected && isProfileFocused) {
                                        Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF2A85)))
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF9D00FF)))
                                    },
                                    shape = CircleShape,
                                )
                                .clickable {
                                    VantafynSoundEffects.playChannelClick(context)
                                    onNavigateToSettings()
                                }
                                .onPreviewKeyEvent {
                                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (it.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER -> {
                                            VantafynSoundEffects.playChannelClick(context)
                                            onNavigateToSettings()
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            channelsToggleFocusRequester.requestFocus()
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            gridFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                },
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
                                    color = Color(0xFF00E5FF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Right Cluster: Magnetic View Switcher Pill + Console Status Tray (Wifi/Cellular, Battery %, Clock)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Magnetic Animated View Switcher Pill [ Grid | List ]
                        val pillTargetX = if (isChannelView) 3.dp else 37.dp
                        val animatedPillX by animateDpAsState(
                            targetValue = pillTargetX,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                            label = "magneticPillX",
                        )

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(72.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF14141E).copy(alpha = 0.85f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            // Magnetic Sliding Indicator Pill
                            Box(
                                modifier = Modifier
                                    .offset(x = animatedPillX)
                                    .size(32.dp)
                                    .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = Color(0xFF00E5FF), spotColor = Color(0xFF00E5FF))
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF9D00FF))
                                        )
                                    )
                            )

                            // Two View Icons
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                // 3D Channels Grid Icon
                                var isChannelsFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .focusRequester(channelsToggleFocusRequester)
                                        .onFocusChanged { isChannelsFocused = it.isFocused }
                                        .focusable()
                                        .clip(CircleShape)
                                        .then(
                                            if (isGamepadConnected && isChannelsFocused) {
                                                Modifier.border(2.dp, Color(0xFF00E5FF), CircleShape)
                                            } else Modifier
                                        )
                                        .clickable {
                                            if (!isChannelView) {
                                                VantafynSoundEffects.playChannelClick(context)
                                                isChannelView = true
                                                prefs.edit().putBoolean("games_home_channel_view", true).apply()
                                            }
                                        }
                                        .onPreviewKeyEvent {
                                            if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                            when (it.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER -> {
                                                    if (!isChannelView) {
                                                        VantafynSoundEffects.playChannelClick(context)
                                                        isChannelView = true
                                                        prefs.edit().putBoolean("games_home_channel_view", true).apply()
                                                    }
                                                    true
                                                }
                                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                    profileFocusRequester.requestFocus()
                                                    true
                                                }
                                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    overviewToggleFocusRequester.requestFocus()
                                                    true
                                                }
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    gridFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ConsoleGridIcon(isSelected = isChannelView)
                                }

                                // Overview List Icon
                                var isOverviewFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .focusRequester(overviewToggleFocusRequester)
                                        .onFocusChanged { isOverviewFocused = it.isFocused }
                                        .focusable()
                                        .clip(CircleShape)
                                        .then(
                                            if (isGamepadConnected && isOverviewFocused) {
                                                Modifier.border(2.dp, Color(0xFFFF2A85), CircleShape)
                                            } else Modifier
                                        )
                                        .clickable {
                                            if (isChannelView) {
                                                VantafynSoundEffects.playChannelClick(context)
                                                isChannelView = false
                                                prefs.edit().putBoolean("games_home_channel_view", false).apply()
                                            }
                                        }
                                        .onPreviewKeyEvent {
                                            if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                            when (it.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER -> {
                                                    if (isChannelView) {
                                                        VantafynSoundEffects.playChannelClick(context)
                                                        isChannelView = false
                                                        prefs.edit().putBoolean("games_home_channel_view", false).apply()
                                                    }
                                                    true
                                                }
                                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                    channelsToggleFocusRequester.requestFocus()
                                                    true
                                                }
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    gridFocusRequester.requestFocus()
                                                    true
                                                }
                                                else -> false
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ConsoleOverviewIcon(isSelected = !isChannelView)
                                }
                            }
                        }

                        // Hardware Console Status Tray (Wifi/Cellular, Battery %, Clock)
                        ConsoleStatusTray()
                    }
                }

                if (isChannelView) {
                    VantafynChannelGrid(
                        items = channelItems,
                        games = allGames.ifEmpty { games },
                        systems = systems,
                        onOpenGame = onOpenGame,
                        onSelectSystem = onSelectSystem,
                        onOpenPokemonVault = onOpenPokemonVault,
                        onOpenSaves = onOpenSaves,
                        onNavigateToSettings = onNavigateToSettings,
                        onSelectLibrary = onSelectLibrary,
                        onSelectSearch = onSelectSearch,
                        onNavigateBack = onNavigateBack,
                        onGridUpdated = { updated ->
                            channelItems = updated
                        },
                        onRequestFocusTopBar = {
                            profileFocusRequester.requestFocus()
                        },
                        onFocusedTileChanged = { focusedItem ->
                            lastFocusedChannelItem = focusedItem
                            if (isGamesOnlyMode) {
                                dualScreenManager.updateSelection(focusedItem, allGames.ifEmpty { games }, systems)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusRequester(gridFocusRequester)
                            .padding(bottom = 0.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {

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

                // Centered "RETRO VAULT PILOT" / "OFFLINE VAULT PILOT" badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isOffline) Color(0xFFFFB300).copy(alpha = 0.14f)
                            else Color(0xFF00E5FF).copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isOffline) Color(0xFFFFB300).copy(alpha = 0.45f)
                            else Color(0xFF00E5FF).copy(alpha = 0.35f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isOffline) Color(0xFFFFB300) else Color(0xFF00E5FF), CircleShape),
                    )
                    Text(
                        text = if (isOffline) "OFFLINE VAULT PILOT" else "RETRO VAULT PILOT",
                        color = if (isOffline) Color(0xFFFFD54F) else Color(0xFF8FE7FF),
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
                        title = if (isOffline) "LOCAL SAVES" else "CLOUD SAVES",
                        value = if (isOffline) "STANDBY" else "SYNCED",
                        icon = if (isOffline) Icons.Rounded.Save else Icons.Rounded.CloudDone,
                        accentColor = if (isOffline) Color(0xFFFFB300) else Color(0xFF00E676),
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
                                onLongPress = { pendingRecentRemoval = record },
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

        // Ambient Console Screensaver Overlay
        AnimatedVisibility(
            visible = isScreensaverActive,
            enter = fadeIn(animationSpec = tween(800)),
            exit = fadeOut(animationSpec = tween(400)),
        ) {
            GameHubScreensaver(
                games = allGames.ifEmpty { games },
                systems = systems,
                slideDurationMs = screensaverSlideMs,
                showClock = screensaverShowClock,
                onWake = {
                    recordActivity()
                    isScreensaverActive = false
                },
            )
        }
    }
    }
}

    pendingRecentRemoval?.let { record ->
        ContinuePlayingGameModal(
            record = record,
            onDismiss = { pendingRecentRemoval = null },
            onResume = {
                onOpenGame(record.toGameSummary())
                pendingRecentRemoval = null
            },
            onRemove = {
                onRemoveRecentGame(record.id)
                pendingRecentRemoval = null
            },
        )
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
private fun ContinuePlayingGameModal(
    record: RecentGameRecord,
    onDismiss: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val effectiveBoxart = remember(record.id, record.boxartUrl) {
        val local = context.getSharedPreferences("vantafyn_retro_settings", android.content.Context.MODE_PRIVATE)
            .getString("boxart_${record.id}", null)
        GameBoxartScraper.convertToCdnUrl(local ?: record.boxartUrl)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isLandscape) 460.dp else 400.dp)
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
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(VantafynColors.Primary, VantafynColors.Secondary),
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(if (isLandscape) 16.dp else 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp),
        ) {
            // Header Row: Thumbnail + System Badge + Title + Playtime + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Game Cover Art / Icon
                Box(
                    modifier = Modifier
                        .width(if (isLandscape) 48.dp else 56.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E28))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!effectiveBoxart.isNullOrBlank()) {
                        AsyncImage(
                            model = effectiveBoxart,
                            contentDescription = record.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Title & Badges
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    1.dp,
                                    Brush.linearGradient(VantafynGradients.AccentColors),
                                    RoundedCornerShape(6.dp),
                                )
                                .background(VantafynColors.Graphite.copy(alpha = 0.95f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = record.systemId.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }

                        Text(
                            text = "CONTINUE PLAYING",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                    }

                    Text(
                        text = record.title,
                        color = Color.White,
                        fontSize = if (isLandscape) 15.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = if (record.playTimeMs > 0) "Played ${GamePlayTracker.formatPlayTime(record.playTimeMs)}" else "Recently played",
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                    )
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Primary Action: Resume Game
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 44.dp else 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(VantafynGradients.accentHorizontal())
                    .clickable(onClick = onResume),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "RESUME GAME",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.8.sp,
                    )
                }
            }

            // Destructive / Hide Action: Remove from Continue Playing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF28131E).copy(alpha = 0.6f))
                    .border(1.dp, Color(0xFFFF5277).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onRemove)
                    .padding(if (isLandscape) 10.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5277).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5277),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Remove from Continue Playing",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Hides from this rail. Your saves and play history stay safe.",
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    )
                }
            }

            // Cancel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancel",
                    color = VantafynColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun SystemQuickCard(
    system: GameSystem,
    onClick: () -> Unit,
    isFocused: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (isFocused) 1.05f else 1f
                scaleY = if (isFocused) 1.05f else 1f
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (isFocused) 0.10f else 0.04f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (isFocused) {
                    Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF2A85)))
                } else {
                    androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.08f))
                },
                shape = RoundedCornerShape(16.dp),
            )
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
            val is3ds = system.id.equals("3ds", ignoreCase = true) ||
                system.id.contains("3ds", ignoreCase = true) ||
                system.name.contains("3ds", ignoreCase = true)
            if (is3ds) {
                Image(
                    painter = painterResource(dev.vantafyn.feature.home.R.drawable.system_logo_3ds),
                    contentDescription = system.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .fillMaxWidth(0.9f),
                )
            } else if (system.officialLogoUrl.isNotBlank()) {
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

/**
 * Pixel-perfect console 2x2 grid icon for channel mode toggle with neon gradient styling.
 */
@Composable
private fun ConsoleGridIcon(isSelected: Boolean) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
        val dSize = size.width * 0.38f
        val gap = size.width - (dSize * 2f)
        val corner = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())

        val brush = if (isSelected) {
            Brush.linearGradient(listOf(Color(0xFF0A0E1A), Color(0xFF141422)))
        } else {
            Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.85f), Color(0xFF00B0FF).copy(alpha = 0.85f)))
        }

        // Top-left
        drawRoundRect(
            brush = brush,
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(dSize, dSize),
            cornerRadius = corner,
        )
        // Top-right
        drawRoundRect(
            brush = brush,
            topLeft = androidx.compose.ui.geometry.Offset(dSize + gap, 0f),
            size = androidx.compose.ui.geometry.Size(dSize, dSize),
            cornerRadius = corner,
        )
        // Bottom-left
        drawRoundRect(
            brush = brush,
            topLeft = androidx.compose.ui.geometry.Offset(0f, dSize + gap),
            size = androidx.compose.ui.geometry.Size(dSize, dSize),
            cornerRadius = corner,
        )
        // Bottom-right
        drawRoundRect(
            brush = brush,
            topLeft = androidx.compose.ui.geometry.Offset(dSize + gap, dSize + gap),
            size = androidx.compose.ui.geometry.Size(dSize, dSize),
            cornerRadius = corner,
        )
    }
}

/**
 * Pixel-perfect console 3-bar list/overview icon for view switcher with neon gradient styling.
 */
@Composable
private fun ConsoleOverviewIcon(isSelected: Boolean) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
        val brush = if (isSelected) {
            Brush.linearGradient(listOf(Color(0xFF0A0E1A), Color(0xFF141422)))
        } else {
            Brush.linearGradient(listOf(Color(0xFF9D00FF).copy(alpha = 0.85f), Color(0xFFFF2A85).copy(alpha = 0.85f)))
        }
        val barHeight = size.height * 0.18f
        val corner = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
        val spacing = (size.height - (barHeight * 3f)) / 2f

        for (i in 0..2) {
            val y = i * (barHeight + spacing)
            drawRoundRect(
                brush = brush,
                topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                cornerRadius = corner,
            )
        }
    }
}


package dev.vantafyn.feature.home.games.grid

import android.content.Context
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynSoundEffects
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.feature.home.games.IdentifyGameArtworkDialog
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Premium 3D Multi-Page Channel Grid inspired by Wii Channel UI and iiSU.
 *
 * Supports:
 * - Deterministic Spatial D-Pad & Controller Navigation for handhelds (Retroid Pocket).
 * - Multi-span tiles (1x1, 2x1, 2x2, 4x2, 2x4, 4x4).
 * - Full-screen layout with maximum usable screen area.
 * - Deep tactile empty socket framing.
 * - Scraped Libretro Box Art resolution with hold-to-search / change artwork.
 * - Channels for Games, Systems, Pokémon Vault, Settings, Library, and Main Home.
 */
private enum class ChevronDirection { LEFT, RIGHT }

/**
 * Console-styled thick vector chevron button with neon glow and no card backing.
 */
@Composable
private fun ConsoleChevronButton(
    direction: ChevronDirection,
    onClick: () -> Unit,
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetAlpha = if (isPressed) 1f else if (isActive) 0.85f else 0.12f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
        label = "chevronAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f,
        label = "chevronScale",
    )

    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(width = 36.dp, height = 72.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    VantafynSoundEffects.playChannelClick(context)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(width = 16.dp, height = 32.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            val strokeWidth = 5.5.dp.toPx()
            val path = Path().apply {
                if (direction == ChevronDirection.LEFT) {
                    moveTo(size.width * 0.85f, 2.dp.toPx())
                    lineTo(size.width * 0.15f, size.height / 2f)
                    lineTo(size.width * 0.85f, size.height - 2.dp.toPx())
                } else {
                    moveTo(size.width * 0.15f, 2.dp.toPx())
                    lineTo(size.width * 0.85f, size.height / 2f)
                    lineTo(size.width * 0.15f, size.height - 2.dp.toPx())
                }
            }

            // Outer cyan glowing halo
            drawPath(
                path = path,
                color = Color(0xFF00E5FF).copy(alpha = 0.38f * alpha),
                style = Stroke(
                    width = strokeWidth + 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // Crisp console white/cyan core stroke
            drawPath(
                path = path,
                color = if (isPressed) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.92f * alpha),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}


@Composable
fun VantafynChannelGrid(
    items: List<ChannelTileItem>,
    games: List<GameSummary>,
    systems: List<GameSystem>,
    onOpenGame: (GameSummary) -> Unit,
    onSelectSystem: (GameSystem) -> Unit,
    onOpenPokemonVault: () -> Unit,
    onOpenSaves: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSelectLibrary: () -> Unit = {},
    onSelectSearch: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onGridUpdated: (List<ChannelTileItem>) -> Unit,
    onRequestFocusTopBar: () -> Unit = {},
    onFocusedTileChanged: (ChannelTileItem?) -> Unit = {},
    modifier: Modifier = Modifier,
    pagesCount: Int = VantafynGridPersistence.DEFAULT_PAGES,
    columns: Int = VantafynGridPersistence.DEFAULT_COLUMNS,
    rows: Int = VantafynGridPersistence.DEFAULT_ROWS,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { pagesCount }
    val focusRequester = remember { FocusRequester() }

    var selectedTileForEdit by remember { mutableStateOf<ChannelTileItem?>(null) }
    var selectedEmptySlotForAdd by remember { mutableStateOf<ChannelTileItem?>(null) }
    var identifyTargetGame by remember { mutableStateOf<GameSummary?>(null) }
    var boxartRevision by remember { mutableIntStateOf(0) }

    var isEditMode by remember { mutableStateOf(false) }
    var draggingTileId by remember { mutableStateOf<String?>(null) }
    var dragOriginLeftPx by remember { mutableFloatStateOf(0f) }
    var dragOriginTopPx by remember { mutableFloatStateOf(0f) }
    var dragTotalOffset by remember { mutableStateOf(Offset.Zero) }
    var dragHoverCol by remember { mutableIntStateOf(0) }
    var dragHoverRow by remember { mutableIntStateOf(0) }
    var lastPageFlipTime by remember { mutableStateOf(0L) }
    var localItems by remember(items) { mutableStateOf(items) }

    // Synchronize localItems when external items change and not dragging
    LaunchedEffect(items) {
        if (draggingTileId == null) {
            localItems = items
        }
    }

    // Initialize focus to first non-empty item on current page
    var focusedTileId by remember {
        mutableStateOf<String?>(
            localItems.firstOrNull { it.page == 0 && it.type != GridTileType.EMPTY_CHANNEL }?.id
                ?: localItems.firstOrNull { it.page == 0 }?.id
        )
    }

    LaunchedEffect(focusedTileId, localItems) {
        val current = localItems.firstOrNull { it.id == focusedTileId }
        onFocusedTileChanged(current)
    }

    // Auto-focus when composable enters
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Play tactile click sound when D-Pad focus moves to another card (Wii style)
    var lastFocusedTileId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focusedTileId) {
        if (lastFocusedTileId != null && focusedTileId != null && focusedTileId != lastFocusedTileId) {
            VantafynSoundEffects.playChannelClick(context)
        }
        lastFocusedTileId = focusedTileId
    }

    // Auto-fade inactivity timer for side chevrons (fade down to 0.12f after 3 seconds of no paging or navigation)
    var chevronsActive by remember { mutableStateOf(true) }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, focusedTileId) {
        chevronsActive = true
        kotlinx.coroutines.delay(3000L)
        chevronsActive = false
    }

    // Ensure focus moves with page flips if current focused tile is on a different page and play click
    var lastPage by remember { mutableIntStateOf(pagerState.currentPage) }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != lastPage) {
            VantafynSoundEffects.playChannelClick(context)
            lastPage = pagerState.currentPage
        }
        val pageItems = localItems.filter { it.page == pagerState.currentPage }
        val currentInPage = pageItems.any { it.id == focusedTileId }
        if (!currentInPage) {
            val firstCandidate = pageItems.firstOrNull { it.type != GridTileType.EMPTY_CHANNEL }
                ?: pageItems.firstOrNull()
            if (firstCandidate != null) {
                focusedTileId = firstCandidate.id
            }
        }
    }

    fun activateItem(item: ChannelTileItem) {
        focusedTileId = item.id
        when (item.type) {
            GridTileType.EMPTY_CHANNEL -> {
                selectedEmptySlotForAdd = item
            }
            GridTileType.GAME -> {
                val targetId = item.targetKey ?: item.id.removePrefix("game_")
                val found = games.firstOrNull { it.id == targetId }
                if (found != null) {
                    onOpenGame(found)
                } else {
                    onOpenGame(
                        GameSummary(
                            id = targetId,
                            title = item.title,
                            systemId = item.subtitle ?: "GBA",
                            filename = "",
                            boxartUrl = item.imageUrl,
                        )
                    )
                }
            }
            GridTileType.CONTINUE_PLAYING -> {
                val targetId = item.targetKey ?: item.id.removePrefix("game_")
                val found = games.firstOrNull { it.id == targetId } ?: games.firstOrNull()
                if (found != null) onOpenGame(found)
            }
            GridTileType.POKEMON_VAULT -> {
                onOpenPokemonVault()
            }
            GridTileType.CLOUD_SAVES -> {
                onOpenSaves()
            }
            GridTileType.SEARCH -> {
                onSelectSearch()
            }
            GridTileType.SYSTEM -> {
                val targetId = item.targetKey ?: item.id.removePrefix("system_")
                val found = systems.firstOrNull { it.id == targetId }
                if (found != null) onSelectSystem(found)
            }
            GridTileType.SETTINGS -> {
                onNavigateToSettings()
            }
            GridTileType.LIBRARY -> {
                onSelectLibrary()
            }
            GridTileType.MAIN_HOME -> {
                onNavigateBack()
            }
            GridTileType.PROFILE_STATS -> {
                onNavigateToSettings()
            }
            else -> {}
        }
    }

    fun openOptionsForItem(item: ChannelTileItem) {
        if (item.type != GridTileType.EMPTY_CHANNEL) {
            selectedTileForEdit = item
        } else {
            selectedEmptySlotForAdd = item
        }
    }

    val isGamepadConnected = rememberIsGamepadConnected()

    // Helper functions for directional spatial navigation
    fun overlapsVertically(a: ChannelTileItem, b: ChannelTileItem): Boolean {
        val aTop = a.row
        val aBottom = a.row + a.span.rows
        val bTop = b.row
        val bBottom = b.row + b.span.rows
        return maxOf(aTop, bTop) < minOf(aBottom, bBottom)
    }

    fun overlapsHorizontally(a: ChannelTileItem, b: ChannelTileItem): Boolean {
        val aLeft = a.col
        val aRight = a.col + a.span.columns
        val bLeft = b.col
        val bRight = b.col + b.span.columns
        return maxOf(aLeft, bLeft) < minOf(aRight, bRight)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                val sortedItems = localItems.sortedWith(compareBy<ChannelTileItem> { it.row }.thenBy { it.col })
                val current = sortedItems.firstOrNull { it.id == focusedTileId }
                    ?: sortedItems.firstOrNull { it.page == pagerState.currentPage }
                    ?: sortedItems.firstOrNull()

                val curPage = current?.page ?: pagerState.currentPage
                val pageTiles = sortedItems.filter { it.page == curPage }

                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B -> {
                        if (isEditMode) {
                            isEditMode = false
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (current != null) {
                            val rightEdge = current.col + current.span.columns
                            val rightCandidates = pageTiles.filter { it.col >= rightEdge }
                            val rowMatched = rightCandidates.filter { overlapsVertically(current, it) }
                            val target = if (rowMatched.isNotEmpty()) {
                                rowMatched.minByOrNull { (it.col - rightEdge) * 100 + abs(it.row - current.row) }
                            } else {
                                rightCandidates.minByOrNull { (it.col - rightEdge) * 100 + abs(it.row - current.row) * 10 }
                            }

                            if (target != null) {
                                focusedTileId = target.id
                                true
                            } else if (curPage < pagesCount - 1) {
                                val nextPage = curPage + 1
                                val nextItems = sortedItems.filter { it.page == nextPage }
                                val nextRowMatched = nextItems.filter { overlapsVertically(current, it) }
                                val targetInNext = if (nextRowMatched.isNotEmpty()) {
                                    nextRowMatched.minByOrNull { it.col * 100 + abs(it.row - current.row) }
                                } else {
                                    nextItems.minByOrNull { it.col * 100 + abs(it.row - current.row) * 10 }
                                }
                                if (targetInNext != null) {
                                    focusedTileId = targetInNext.id
                                }
                                scope.launch {
                                    pagerState.animateScrollToPage(nextPage)
                                }
                                true
                            } else false
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (current != null) {
                            val leftCandidates = pageTiles.filter { it.col + it.span.columns <= current.col }
                            val rowMatched = leftCandidates.filter { overlapsVertically(current, it) }
                            val target = if (rowMatched.isNotEmpty()) {
                                rowMatched.minByOrNull { (current.col - (it.col + it.span.columns)) * 100 + abs(it.row - current.row) }
                            } else {
                                leftCandidates.minByOrNull { (current.col - (it.col + it.span.columns)) * 100 + abs(it.row - current.row) * 10 }
                            }

                            if (target != null) {
                                focusedTileId = target.id
                                true
                            } else if (curPage > 0) {
                                val prevPage = curPage - 1
                                val prevItems = sortedItems.filter { it.page == prevPage }
                                val prevRowMatched = prevItems.filter { overlapsVertically(current, it) }
                                val targetInPrev = if (prevRowMatched.isNotEmpty()) {
                                    prevRowMatched.minByOrNull { (columns - (it.col + it.span.columns)) * 100 + abs(it.row - current.row) }
                                } else {
                                    prevItems.minByOrNull { (columns - (it.col + it.span.columns)) * 100 + abs(it.row - current.row) * 10 }
                                }
                                if (targetInPrev != null) {
                                    focusedTileId = targetInPrev.id
                                }
                                scope.launch {
                                    pagerState.animateScrollToPage(prevPage)
                                }
                                true
                            } else false
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (current != null) {
                            val bottomEdge = current.row + current.span.rows
                            val downCandidates = pageTiles.filter { it.row >= bottomEdge }
                            val colMatched = downCandidates.filter { overlapsHorizontally(current, it) }
                            val target = if (colMatched.isNotEmpty()) {
                                colMatched.minByOrNull { (it.row - bottomEdge) * 100 + abs(it.col - current.col) }
                            } else {
                                downCandidates.minByOrNull { (it.row - bottomEdge) * 100 + abs(it.col - current.col) * 10 }
                            }

                            if (target != null) {
                                focusedTileId = target.id
                                true
                            } else false
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (current != null) {
                            val upCandidates = pageTiles.filter { it.row + it.span.rows <= current.row }
                            val colMatched = upCandidates.filter { overlapsHorizontally(current, it) }
                            val target = if (colMatched.isNotEmpty()) {
                                colMatched.minByOrNull { (current.row - (it.row + it.span.rows)) * 100 + abs(it.col - current.col) }
                            } else {
                                upCandidates.minByOrNull { (current.row - (it.row + it.span.rows)) * 100 + abs(it.col - current.col) * 10 }
                            }

                            if (target != null) {
                                focusedTileId = target.id
                                true
                            } else {
                                onRequestFocusTopBar()
                                true
                            }
                        } else false
                    }
                    KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_PAGE_UP -> {
                        if (pagerState.currentPage > 0) {
                            val prevPage = pagerState.currentPage - 1
                            val prevItems = sortedItems.filter { it.page == prevPage }
                            val targetInPrev = prevItems.firstOrNull { it.type != GridTileType.EMPTY_CHANNEL }
                                ?: prevItems.firstOrNull()
                            if (targetInPrev != null) focusedTileId = targetInPrev.id
                            scope.launch {
                                pagerState.animateScrollToPage(prevPage)
                            }
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_PAGE_DOWN -> {
                        if (pagerState.currentPage < pagesCount - 1) {
                            val nextPage = pagerState.currentPage + 1
                            val nextItems = sortedItems.filter { it.page == nextPage }
                            val targetInNext = nextItems.firstOrNull { it.type != GridTileType.EMPTY_CHANNEL }
                                ?: nextItems.firstOrNull()
                            if (targetInNext != null) focusedTileId = targetInNext.id
                            scope.launch {
                                pagerState.animateScrollToPage(nextPage)
                            }
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER -> {
                        if (current != null) {
                            activateItem(current)
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_MENU -> {
                        if (current != null) {
                            openOptionsForItem(current)
                            true
                        } else false
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Main Grid with Horizontal Pager & Thick Side Arrows
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = draggingTileId == null,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 2.dp),
                pageSpacing = 16.dp,
            ) { pageIndex ->
                val pageItems = localItems.filter { it.page == pageIndex }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val spacingPx = with(density) { 12.dp.toPx() }
                    val totalWidthPx = constraints.maxWidth.toFloat()
                    val totalHeightPx = constraints.maxHeight.toFloat()

                    val unitWidthPx = (totalWidthPx - (columns - 1) * spacingPx) / columns
                    val unitHeightPx = (totalHeightPx - (rows - 1) * spacingPx) / rows

                    for (item in pageItems) {
                        key(item.id) {
                            val currentItem by rememberUpdatedState(item)
                            val leftPx = item.col * (unitWidthPx + spacingPx)
                            val topPx = item.row * (unitHeightPx + spacingPx)
                            val widthPx = item.span.columns * unitWidthPx + (item.span.columns - 1) * spacingPx
                            val heightPx = item.span.rows * unitHeightPx + (item.span.rows - 1) * spacingPx

                            val widthDp = with(density) { widthPx.toDp() }
                            val heightDp = with(density) { heightPx.toDp() }

                            val isFocused = isGamepadConnected && focusedTileId == item.id
                            val isBeingDragged = item.id == draggingTileId
                            val isWiggling = isEditMode && !isBeingDragged

                            val animLeftDp by animateDpAsState(
                                targetValue = with(density) { leftPx.toDp() },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                label = "tileLeft_${item.id}",
                            )
                            val animTopDp by animateDpAsState(
                                targetValue = with(density) { topPx.toDp() },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                label = "tileTop_${item.id}",
                            )

                            val finalLeftDp = if (isBeingDragged) {
                                with(density) { (dragOriginLeftPx + dragTotalOffset.x).toDp() }
                            } else {
                                animLeftDp
                            }
                            val finalTopDp = if (isBeingDragged) {
                                with(density) { (dragOriginTopPx + dragTotalOffset.y).toDp() }
                            } else {
                                animTopDp
                            }
                            val zIndex = if (isBeingDragged) 100f else 1f

                            val gestureModifier = if (item.type != GridTileType.EMPTY_CHANNEL) {
                                Modifier.pointerInput(item.id) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                                        var isLongPress = false

                                        try {
                                            withTimeout(longPressTimeout) {
                                                val up = waitForUpOrCancellation()
                                                if (up != null) {
                                                    up.consume()
                                                    if (isEditMode) {
                                                        openOptionsForItem(currentItem)
                                                    } else {
                                                        activateItem(currentItem)
                                                    }
                                                }
                                            }
                                        } catch (_: PointerEventTimeoutCancellationException) {
                                            isLongPress = true
                                        }

                                        if (isLongPress) {
                                            down.consume()
                                            isEditMode = true
                                            draggingTileId = currentItem.id
                                            dragOriginLeftPx = currentItem.col * (unitWidthPx + spacingPx)
                                            dragOriginTopPx = currentItem.row * (unitHeightPx + spacingPx)
                                            dragTotalOffset = Offset.Zero
                                            dragHoverCol = currentItem.col
                                            dragHoverRow = currentItem.row
                                            VantafynSoundEffects.playChannelClick(context)

                                            try {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                    if (change.changedToUp()) {
                                                        change.consume()
                                                        val normalized = VantafynGridPersistence.fillMissingSlotsWithPlaceholders(
                                                            localItems,
                                                            pagesCount = pagesCount,
                                                            totalCols = columns,
                                                            totalRows = rows,
                                                        )
                                                        localItems = normalized
                                                        VantafynGridPersistence.saveGrid(context, normalized)
                                                        onGridUpdated(normalized)
                                                        draggingTileId = null
                                                        dragTotalOffset = Offset.Zero
                                                        VantafynSoundEffects.playChannelClick(context)
                                                        break
                                                    }
                                                    val dragAmount = change.positionChange()
                                                    if (dragAmount != Offset.Zero) {
                                                        change.consume()
                                                        dragTotalOffset += dragAmount

                                                        val curLeft = dragOriginLeftPx + dragTotalOffset.x
                                                        val curTop = dragOriginTopPx + dragTotalOffset.y
                                                        val curCenterX = curLeft + widthPx / 2f
                                                        val curCenterY = curTop + heightPx / 2f

                                                        val now = System.currentTimeMillis()
                                                        if (curLeft < -spacingPx * 1.5f && pageIndex > 0 && (now - lastPageFlipTime > 650L)) {
                                                            // Dragged to left chevron -> move to previous page
                                                            lastPageFlipTime = now
                                                            val prevPage = pageIndex - 1
                                                            scope.launch { pagerState.animateScrollToPage(prevPage) }
                                                            val targetCol = (columns - currentItem.span.columns).coerceAtLeast(0)
                                                            dragHoverCol = targetCol
                                                            localItems = VantafynGridPersistence.moveTile(
                                                                items = localItems,
                                                                targetId = currentItem.id,
                                                                targetPage = prevPage,
                                                                targetCol = targetCol,
                                                                targetRow = dragHoverRow,
                                                                pagesCount = pagesCount,
                                                                totalCols = columns,
                                                                totalRows = rows,
                                                            )
                                                            VantafynSoundEffects.playChannelClick(context)
                                                        } else if ((curLeft + widthPx) > (totalWidthPx + spacingPx * 1.5f) && pageIndex < pagesCount - 1 && (now - lastPageFlipTime > 650L)) {
                                                            // Dragged to right chevron -> move to next page
                                                            lastPageFlipTime = now
                                                            val nextPage = pageIndex + 1
                                                            scope.launch { pagerState.animateScrollToPage(nextPage) }
                                                            val targetCol = 0
                                                            dragHoverCol = targetCol
                                                            localItems = VantafynGridPersistence.moveTile(
                                                                items = localItems,
                                                                targetId = currentItem.id,
                                                                targetPage = nextPage,
                                                                targetCol = targetCol,
                                                                targetRow = dragHoverRow,
                                                                pagesCount = pagesCount,
                                                                totalCols = columns,
                                                                totalRows = rows,
                                                            )
                                                            VantafynSoundEffects.playChannelClick(context)
                                                        } else {
                                                            val targetCol = ((curCenterX - widthPx / 2f + unitWidthPx / 2f) / (unitWidthPx + spacingPx))
                                                                .toInt().coerceIn(0, columns - currentItem.span.columns)
                                                            val targetRow = ((curCenterY - heightPx / 2f + unitHeightPx / 2f) / (unitHeightPx + spacingPx))
                                                                .toInt().coerceIn(0, rows - currentItem.span.rows)

                                                            if (targetCol != dragHoverCol || targetRow != dragHoverRow) {
                                                                dragHoverCol = targetCol
                                                                dragHoverRow = targetRow
                                                                localItems = VantafynGridPersistence.moveTile(
                                                                    items = localItems,
                                                                    targetId = currentItem.id,
                                                                    targetPage = pageIndex,
                                                                    targetCol = targetCol,
                                                                    targetRow = targetRow,
                                                                    pagesCount = pagesCount,
                                                                    totalCols = columns,
                                                                    totalRows = rows,
                                                                )
                                                                VantafynSoundEffects.playChannelClick(context)
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                draggingTileId = null
                                                dragTotalOffset = Offset.Zero
                                            }
                                        }
                                    }
                                }
                            } else {
                                Modifier.pointerInput(item.id) {
                                    detectTapGestures(
                                        onTap = {
                                            selectedEmptySlotForAdd = currentItem
                                        }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .offset(x = finalLeftDp, y = finalTopDp)
                                    .size(width = widthDp, height = heightDp)
                                    .zIndex(zIndex)
                                    .graphicsLayer {
                                        scaleX = if (isBeingDragged) 1.08f else 1f
                                        scaleY = if (isBeingDragged) 1.08f else 1f
                                    }
                                    .shadow(
                                        elevation = if (isBeingDragged) 16.dp else 0.dp,
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                    .then(gestureModifier)
                            ) {
                                VantafynChannelTile(
                                    item = currentItem,
                                    isFocused = isFocused,
                                    isWiggling = isWiggling,
                                )
                            }
                        }
                    }
                }
            }

            // Left Navigation Flank Arrow (<) - Thick console vector stroke, no card backing
            if (pagerState.currentPage > 0) {
                ConsoleChevronButton(
                    direction = ChevronDirection.LEFT,
                    isActive = chevronsActive,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp),
                )
            }

            // Right Navigation Flank Arrow (>) - Thick console vector stroke, no card backing
            if (pagerState.currentPage < pagesCount - 1) {
                ConsoleChevronButton(
                    direction = ChevronDirection.RIGHT,
                    isActive = chevronsActive,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp),
                )
            }
        }

        // Bottom Bar: Edit Mode Finish Indicator OR Console Page Dots
        Row(
            modifier = Modifier
                .padding(bottom = 6.dp, top = 2.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                        .border(1.2.dp, Color(0xFF00E5FF), RoundedCornerShape(999.dp))
                        .clickable { isEditMode = false }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = "Done",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Editing Channels • Tap to Finish",
                        color = Color(0xFF8FE7FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                val dotColors = listOf(
                    Color(0xFF00E5FF), // Page 0: Electric Cyan
                    Color(0xFF00A3FF), // Page 1: Ocean Azure
                    Color(0xFF7000FF), // Page 2: Royal Purple
                    Color(0xFF9D00FF), // Page 3: Electric Violet
                    Color(0xFFFF2A85), // Page 4: Neon Magenta
                )
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until pagesCount) {
                        val isSelected = pagerState.currentPage == i
                        val baseColor = dotColors.getOrElse(i) { Color(0xFF00E5FF) }
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) baseColor
                                    else baseColor.copy(alpha = 0.35f)
                                )
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                    } else Modifier
                                )
                                .clickable {
                                    VantafynSoundEffects.playChannelClick(context)
                                    scope.launch {
                                        pagerState.animateScrollToPage(i)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    // Tile Edit / Resizing Dialog
    selectedTileForEdit?.let { tile ->
        ChannelTileOptionsDialog(
            item = tile,
            onDismiss = { selectedTileForEdit = null },
            onResize = { newSpan ->
                val updated = VantafynGridPersistence.resizeTile(items, tile.id, newSpan)
                VantafynGridPersistence.saveGrid(context, updated)
                onGridUpdated(updated)
                selectedTileForEdit = null
            },
            onSearchBoxArt = {
                val targetId = tile.targetKey ?: tile.id.removePrefix("game_")
                val foundGame = games.firstOrNull { it.id == targetId }
                    ?: GameSummary(
                        id = targetId,
                        title = tile.title,
                        systemId = tile.subtitle ?: "GBA",
                        filename = "",
                        boxartUrl = tile.imageUrl,
                    )
                identifyTargetGame = foundGame
                selectedTileForEdit = null
            },
            onRemove = {
                val updated = VantafynGridPersistence.removeTile(items, tile.id)
                VantafynGridPersistence.saveGrid(context, updated)
                onGridUpdated(updated)
                selectedTileForEdit = null
            },
            onResetToDefault = {
                val defaults = VantafynGridPersistence.resetToDefault(context, games, systems, emptyList())
                onGridUpdated(defaults)
                selectedTileForEdit = null
            },
        )
    }

    // Libretro Artwork Identification / Scraping Dialog
    identifyTargetGame?.let { targetGame ->
        val localArt = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
            .getString("boxart_${targetGame.id}", null)
        IdentifyGameArtworkDialog(
            game = targetGame,
            currentBoxartUrl = GameBoxartScraper.convertToCdnUrl(localArt ?: targetGame.boxartUrl),
            onDismiss = { identifyTargetGame = null },
            onSelectArtwork = { url ->
                context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                    .edit()
                    .putString("boxart_${targetGame.id}", url)
                    .apply()

                val updated = items.map { item ->
                    if (item.targetKey == targetGame.id || item.id == "game_${targetGame.id}") {
                        item.copy(imageUrl = url)
                    } else item
                }
                VantafynGridPersistence.saveGrid(context, updated)
                onGridUpdated(updated)
                boxartRevision++
                identifyTargetGame = null
            },
        )
    }

    // Slot Content Assignment Dialog (Add Channel)
    selectedEmptySlotForAdd?.let { emptySlot ->
        AddChannelContentDialog(
            emptySlot = emptySlot,
            games = games,
            systems = systems,
            onDismiss = { selectedEmptySlotForAdd = null },
            onSelectGame = { game ->
                val local = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                    .getString("boxart_${game.id}", null)
                val newItem = ChannelTileItem(
                    id = "game_${game.id}",
                    title = game.title,
                    subtitle = game.systemId,
                    type = GridTileType.GAME,
                    span = GridTileSpan.SPAN_1X1,
                    page = emptySlot.page,
                    col = emptySlot.col,
                    row = emptySlot.row,
                    imageUrl = local ?: game.boxartUrl,
                    targetKey = game.id,
                )
                val filtered = items.filterNot { it.id == emptySlot.id } + newItem
                val updated = VantafynGridPersistence.fillMissingSlotsWithPlaceholders(filtered)
                VantafynGridPersistence.saveGrid(context, updated)
                onGridUpdated(updated)
                selectedEmptySlotForAdd = null
            },
            onSelectSystem = { sys ->
                val newItem = ChannelTileItem(
                    id = "system_${sys.id}",
                    title = sys.name,
                    subtitle = "${sys.gameCount} Games",
                    type = GridTileType.SYSTEM,
                    span = GridTileSpan.SPAN_1X1,
                    page = emptySlot.page,
                    col = emptySlot.col,
                    row = emptySlot.row,
                    imageUrl = sys.logoUrl,
                    targetKey = sys.id,
                )
                val filtered = items.filterNot { it.id == emptySlot.id } + newItem
                val updated = VantafynGridPersistence.fillMissingSlotsWithPlaceholders(filtered)
                VantafynGridPersistence.saveGrid(context, updated)
                onGridUpdated(updated)
                selectedEmptySlotForAdd = null
            },
            onSelectSpecialChannel = { type, title, subtitle, span, iconVector ->
                val newItem = ChannelTileItem(
                    id = "channel_${type.name.lowercase()}_${System.currentTimeMillis()}",
                    title = title,
                    subtitle = subtitle,
                    type = type,
                    span = span,
                    page = emptySlot.page,
                    col = emptySlot.col,
                    row = emptySlot.row,
                )
                val filtered = items.filterNot { it.id == emptySlot.id } + newItem
                val updated = VantafynGridPersistence.fillMissingSlotsWithPlaceholders(filtered)
                VantafynGridPersistence.saveGrid(context, updated)
                onGridUpdated(updated)
                selectedEmptySlotForAdd = null
            },
        )
    }
}

/**
 * Dialog to adjust tile size, spans, search box art, or remove it from the home grid.
 */
@Composable
private fun ChannelTileOptionsDialog(
    item: ChannelTileItem,
    onDismiss: () -> Unit,
    onResize: (GridTileSpan) -> Unit,
    onSearchBoxArt: () -> Unit,
    onRemove: () -> Unit,
    onResetToDefault: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.55f),
                            Color(0xFFBD00FF).copy(alpha = 0.35f),
                        )
                    ),
                    RoundedCornerShape(26.dp),
                )
                .background(Color(0xFF100F1A)),
        ) {
            // Nebula Background
            Image(
                painter = painterResource(CoreUiR.drawable.vantafyn_onboarding_nebula),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.38f },
            )
            // Dark Scrim over nebula
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0B0A13).copy(alpha = 0.88f),
                                Color(0xFF131221).copy(alpha = 0.94f),
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = VantafynColors.Muted)
                    }
                }

                // Search / Change Box Art option for Game channels
                if (item.type == GridTileType.GAME || item.type == GridTileType.CONTINUE_PLAYING) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickable { onSearchBoxArt() }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search Artwork",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Search Box Art",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Scrape cover artwork from Libretro database",
                                color = Color(0xFF8FE7FF),
                                fontSize = 10.sp,
                            )
                        }
                    }
                }

                // Span Resizing Section
                Text(
                    text = "RESIZE CHANNEL",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (span in listOf(GridTileSpan.SPAN_1X1, GridTileSpan.SPAN_2X1, GridTileSpan.SPAN_2X2)) {
                        val isCurrent = item.span == span
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) Color(0xFF00E5FF).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onResize(span) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = span.key,
                                color = if (isCurrent) Color(0xFF8FE7FF) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (span in listOf(GridTileSpan.SPAN_4X2, GridTileSpan.SPAN_2X4, GridTileSpan.SPAN_4X4)) {
                        val isCurrent = item.span == span
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) Color(0xFF00E5FF).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onResize(span) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = span.key,
                                color = if (isCurrent) Color(0xFF8FE7FF) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Remove from Grid (all tiles removable) or Reset All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onRemove,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Remove Channel",
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    TextButton(
                        onClick = onResetToDefault,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset Grid",
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Reset All", color = VantafynColors.Muted)
                    }
                }
            }
        }
    }
}

/**
 * Enhanced dialog enabling players to select a game (with rich box art), console system,
 * or shortcut/widget channel to place into an empty socket.
 */
@Composable
private fun AddChannelContentDialog(
    emptySlot: ChannelTileItem,
    games: List<GameSummary>,
    systems: List<GameSystem>,
    onDismiss: () -> Unit,
    onSelectGame: (GameSummary) -> Unit,
    onSelectSystem: (GameSystem) -> Unit,
    onSelectSpecialChannel: (type: GridTileType, title: String, subtitle: String, span: GridTileSpan, icon: ImageVector) -> Unit,
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableIntStateOf(0) } // 0 = Games, 1 = Consoles, 2 = Shortcuts
    var searchQuery by remember { mutableStateOf("") }

    val filteredGames = remember(games, searchQuery) {
        if (searchQuery.isBlank()) games.take(30)
        else games.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.systemId.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(480.dp)
                .height(510.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.55f),
                            Color(0xFFBD00FF).copy(alpha = 0.35f),
                        )
                    ),
                    RoundedCornerShape(26.dp),
                )
                .background(Color(0xFF100F1A)),
        ) {
            // Nebula Background
            Image(
                painter = painterResource(CoreUiR.drawable.vantafyn_onboarding_nebula),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.38f },
            )
            // Dark Scrim over nebula
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0B0A13).copy(alpha = 0.88f),
                                Color(0xFF131221).copy(alpha = 0.94f),
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header with glowing icon and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Add Channel",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Slot (${emptySlot.col + 1}, ${emptySlot.row + 1}) • Page ${emptySlot.page + 1}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = VantafynColors.Muted)
                    }
                }

                // Category Tabs: [ Games | Consoles | Shortcuts ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("Games", "Consoles", "Shortcuts").forEachIndexed { index, label ->
                        val isSelected = selectedCategory == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .clickable { selectedCategory = index }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF8FE7FF) else VantafynColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }

                // Search Bar (if on Games tab)
                if (selectedCategory == 0) {
                    VantafynTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Search",
                        placeholder = "Search game title or console...",
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = VantafynColors.Muted)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Content Area based on Category
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedCategory) {
                        0 -> {
                            // Games List with Box Art Thumbnails
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(filteredGames, key = { it.id }) { game ->
                                    val localArt = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                                        .getString("boxart_${game.id}", null)
                                    val effectiveArt = GameBoxartScraper.convertToCdnUrl(localArt ?: game.boxartUrl)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .clickable { onSelectGame(game) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        // 3:4 Boxart Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .width(40.dp)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0E101A))
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!effectiveArt.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = effectiveArt,
                                                    contentDescription = game.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.SportsEsports,
                                                    contentDescription = null,
                                                    tint = VantafynColors.Muted.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = game.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = game.systemId.uppercase(),
                                                color = Color(0xFF00E5FF),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Consoles / Systems List
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(systems, key = { it.id }) { sys ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .clickable { onSelectSystem(sys) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1E162B)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!sys.logoUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = sys.logoUrl,
                                                    contentDescription = sys.name,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.size(30.dp),
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.Dns,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD180FF),
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sys.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "${sys.gameCount} Games Available",
                                                color = VantafynColors.Muted,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Special Channels / System Shortcuts List
                            val shortcuts = listOf(
                                Triple(GridTileType.SETTINGS, "System Settings", "Preferences, Video & Audio") to (GridTileSpan.SPAN_1X1 to Icons.Rounded.Settings),
                                Triple(GridTileType.POKEMON_VAULT, "Pokémon Vault", "Cloud Pokémon storage & boxes") to (GridTileSpan.SPAN_2X2 to Icons.Rounded.CatchingPokemon),
                                Triple(GridTileType.CLOUD_SAVES, "Cloud Saves", "Real-time sync status") to (GridTileSpan.SPAN_2X1 to Icons.Rounded.CloudDone),
                                Triple(GridTileType.SEARCH, "Search", "Universal games & consoles search") to (GridTileSpan.SPAN_1X1 to Icons.Rounded.Search),
                                Triple(GridTileType.LIBRARY, "Games Library", "Full list of consoles & games") to (GridTileSpan.SPAN_1X1 to Icons.Rounded.SportsEsports),
                                Triple(GridTileType.MAIN_HOME, "Vantafyn Home", "Return to Movies, TV & Media") to (GridTileSpan.SPAN_2X1 to Icons.Rounded.Home),
                                Triple(GridTileType.CONTINUE_PLAYING, "Continue Playing Banner", "Hero resume banner") to (GridTileSpan.SPAN_4X2 to Icons.Rounded.PlayArrow),
                                Triple(GridTileType.PROFILE_STATS, "Player Profile", "User stats & playtime") to (GridTileSpan.SPAN_2X1 to Icons.Rounded.Person),
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(shortcuts) { (info, meta) ->
                                    val (type, title, subtitle) = info
                                    val (span, icon) = meta

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                onSelectSpecialChannel(type, title, subtitle, span, icon)
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = title,
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "$subtitle • ${span.key}",
                                                color = VantafynColors.Muted,
                                                fontSize = 10.sp,
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
}

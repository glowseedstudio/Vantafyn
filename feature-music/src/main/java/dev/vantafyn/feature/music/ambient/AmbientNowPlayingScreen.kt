package dev.vantafyn.feature.music.ambient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.vantafyn.core.ui.rememberDevicePosture
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinLyricLine
import dev.vantafyn.core.jellyfin.JellyfinLyrics
import dev.vantafyn.core.media.VantafynMusicPlaybackState
import dev.vantafyn.core.media.VantafynMusicRepeatMode
import dev.vantafyn.core.media.VantafynMusicTrack
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class AmbientDisplayMode {
    LyricsFocus,
    ArtworkAndLyrics,
}

@Composable
fun AmbientNowPlayingScreen(
    playbackState: VantafynMusicPlaybackState,
    lyrics: JellyfinLyrics?,
    isLyricsLoading: Boolean,
    currentPositionMs: () -> Long = { playbackState.positionMs },
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var displayMode by remember { mutableStateOf(AmbientDisplayMode.LyricsFocus) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Live Clock
    var currentTimeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (isActive) {
            currentTimeText = timeFormat.format(Date())
            delay(1_000L)
        }
    }

    // Live Position Ticker for smooth seekbar and lyrics sync
    var livePositionMs by remember(playbackState.currentTrack?.id, playbackState.isPlaying) {
        mutableLongStateOf(playbackState.positionMs)
    }
    LaunchedEffect(playbackState.currentTrack?.id, playbackState.isPlaying) {
        if (!playbackState.isPlaying) {
            livePositionMs = playbackState.positionMs
            return@LaunchedEffect
        }
        while (isActive) {
            livePositionMs = currentPositionMs()
            delay(100L)
        }
    }

    // Live Battery Level
    var batteryPercentage by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    if (level >= 0 && scale > 0) {
                        batteryPercentage = ((level / scale.toFloat()) * 100).roundToInt()
                    }
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Auto-Dim Controls after 8 seconds of inactivity
    LaunchedEffect(lastInteractionMs, controlsVisible) {
        if (controlsVisible) {
            delay(8_000L)
            controlsVisible = false
        }
    }

    // OLED Burn-in pixel shifting (±4dp slow drift every 60s)
    var burnInShiftX by remember { mutableFloatStateOf(0f) }
    var burnInShiftY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val offsets = listOf(
            0f to 0f,
            -3f to 2f,
            3f to -2f,
            -2f to -3f,
            2f to 3f,
            -3f to 1f,
        )
        var offsetIndex = 0
        while (isActive) {
            delay(60_000L)
            offsetIndex = (offsetIndex + 1) % offsets.size
            burnInShiftX = offsets[offsetIndex].first
            burnInShiftY = offsets[offsetIndex].second
        }
    }

    val animatedShiftX by animateFloatAsState(targetValue = burnInShiftX, animationSpec = tween(3000), label = "burnInX")
    val animatedShiftY by animateFloatAsState(targetValue = burnInShiftY, animationSpec = tween(3000), label = "burnInY")

    val currentTrack = playbackState.currentTrack

    val posture = rememberDevicePosture()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onDoubleTap = {
                        displayMode = if (displayMode == AmbientDisplayMode.LyricsFocus) {
                            AmbientDisplayMode.ArtworkAndLyrics
                        } else {
                            AmbientDisplayMode.LyricsFocus
                        }
                        lastInteractionMs = System.currentTimeMillis()
                    },
                )
            },
    ) {
        val isCoverScreen = maxHeight < 580.dp || (maxHeight.value / maxWidth.value.coerceAtLeast(1f)) < 1.35f

        // Dynamic Ambient Background Glow
        AmbientArtworkBackdrop(artworkUrl = currentTrack?.artworkUrl)

        // Main Content Container with OLED Burn-in micro-shift
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedShiftX.dp.roundToPx(), animatedShiftY.dp.roundToPx()) },
        ) {
            if (isCoverScreen) {
                // Dedicated Compact Cover Screen Layout (Razr 50 Ultra / Galaxy Z Flip)
                CoverScreenAmbientLayout(
                    track = currentTrack,
                    playbackState = playbackState,
                    lyrics = lyrics,
                    isLyricsLoading = isLyricsLoading,
                    livePositionMs = livePositionMs,
                    currentTimeText = currentTimeText,
                    batteryPercentage = batteryPercentage,
                    isCharging = isCharging,
                    controlsVisible = controlsVisible,
                    onPlayPause = {
                        onPlayPause()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onNext = {
                        onNext()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onPrevious = {
                        onPrevious()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onSeek = { pos ->
                        onSeek(pos)
                        livePositionMs = pos
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onToggleShuffle = {
                        onToggleShuffle()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onCycleRepeat = {
                        onCycleRepeat()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onClose = onClose,
                )
            } else if (posture.isTabletop) {
                // Dedicated Flex / Tabletop Mode (Half-folded on desk)
                FlexJukeboxAmbientLayout(
                    track = currentTrack,
                    playbackState = playbackState,
                    lyrics = lyrics,
                    isLyricsLoading = isLyricsLoading,
                    livePositionMs = livePositionMs,
                    currentTimeText = currentTimeText,
                    batteryPercentage = batteryPercentage,
                    isCharging = isCharging,
                    controlsVisible = controlsVisible,
                    onPlayPause = {
                        onPlayPause()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onNext = {
                        onNext()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onPrevious = {
                        onPrevious()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onSeek = { pos ->
                        onSeek(pos)
                        livePositionMs = pos
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onToggleShuffle = {
                        onToggleShuffle()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onCycleRepeat = {
                        onCycleRepeat()
                        lastInteractionMs = System.currentTimeMillis()
                    },
                    onClose = onClose,
                )
            } else {
                // Standard Tall Inner Display Layout
                // Top Ambient Header (Safely positioned with status bar insets)
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(400)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    AmbientTopBar(
                        timeText = currentTimeText,
                        batteryPercent = batteryPercentage,
                        isCharging = isCharging,
                        displayMode = displayMode,
                        onToggleMode = {
                            displayMode = if (displayMode == AmbientDisplayMode.LyricsFocus) {
                                AmbientDisplayMode.ArtworkAndLyrics
                            } else {
                                AmbientDisplayMode.LyricsFocus
                            }
                            lastInteractionMs = System.currentTimeMillis()
                        },
                        onClose = onClose,
                    )
                }

                // Central Lyrics / Artwork Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(top = 96.dp, bottom = if (controlsVisible) 190.dp else 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (displayMode == AmbientDisplayMode.LyricsFocus) {
                        AmbientLyricsBody(
                            lyrics = lyrics,
                            isLoading = isLyricsLoading,
                            playbackMs = livePositionMs,
                            isPlaying = playbackState.isPlaying,
                            track = currentTrack,
                            onSeek = { pos ->
                                onSeek(pos)
                                livePositionMs = pos
                                lastInteractionMs = System.currentTimeMillis()
                            },
                        )
                    } else {
                        AmbientArtworkAndLyricsBody(
                            track = currentTrack,
                            lyrics = lyrics,
                            playbackMs = livePositionMs,
                            onSeek = { pos ->
                                onSeek(pos)
                                livePositionMs = pos
                                lastInteractionMs = System.currentTimeMillis()
                            },
                        )
                    }
                }

                // Bottom Glass Playback Control Deck
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(400)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    AmbientControlDeck(
                        track = currentTrack,
                        playbackState = playbackState,
                        livePositionMs = livePositionMs,
                        onPlayPause = {
                            onPlayPause()
                            lastInteractionMs = System.currentTimeMillis()
                        },
                        onNext = {
                            onNext()
                            lastInteractionMs = System.currentTimeMillis()
                        },
                        onPrevious = {
                            onPrevious()
                            lastInteractionMs = System.currentTimeMillis()
                        },
                        onSeek = { pos ->
                            onSeek(pos)
                            livePositionMs = pos
                            lastInteractionMs = System.currentTimeMillis()
                        },
                        onToggleShuffle = {
                            onToggleShuffle()
                            lastInteractionMs = System.currentTimeMillis()
                        },
                        onCycleRepeat = {
                            onCycleRepeat()
                            lastInteractionMs = System.currentTimeMillis()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientTopBar(
    timeText: String,
    batteryPercent: Int,
    isCharging: Boolean,
    displayMode: AmbientDisplayMode,
    onToggleMode: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Clock & Battery Chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = timeText,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(Color.White.copy(alpha = 0.25f)),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                    contentDescription = null,
                    tint = if (isCharging) Color(0xFF00FF9C) else Color.White.copy(alpha = 0.80f),
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "$batteryPercent%",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Actions: Mode Toggle & Close
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(
                onClick = onToggleMode,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)),
            ) {
                Icon(
                    imageVector = if (displayMode == AmbientDisplayMode.LyricsFocus) Icons.Rounded.MusicNote else Icons.Rounded.Lyrics,
                    contentDescription = "Toggle view",
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Exit ambient mode",
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AmbientArtworkBackdrop(artworkUrl: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(72.dp)
                    .graphicsLayer { alpha = 0.22f },
            )
        }
        // Signature Cyan -> Purple -> Magenta ambient gradient sweep from one side to the other
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF31D7FF).copy(alpha = 0.16f),
                            Color(0xFF9D4EDD).copy(alpha = 0.14f),
                            Color(0xFFFF5277).copy(alpha = 0.16f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f), Color.Black),
                    ),
                ),
        )
    }
}

@Composable
private fun AmbientLyricsBody(
    lyrics: JellyfinLyrics?,
    isLoading: Boolean,
    playbackMs: Long,
    isPlaying: Boolean,
    track: VantafynMusicTrack?,
    onSeek: (Long) -> Unit,
) {
    if (isLoading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Loading synced lyrics...",
                color = Color.White.copy(alpha = 0.60f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    if (lyrics == null || (lyrics.syncedLines.isEmpty() && lyrics.plainText.isBlank())) {
        AmbientTrackHeroFallback(track = track)
        return
    }

    if (lyrics.isSynced) {
        AmbientSyncedLyricsList(
            lines = lyrics.syncedLines,
            playbackMs = playbackMs,
            isPlaying = isPlaying,
            onSeek = onSeek,
        )
    } else {
        AmbientPlainLyricsList(text = lyrics.plainText)
    }
}

@Composable
private fun AmbientSyncedLyricsList(
    lines: List<JellyfinLyricLine>,
    playbackMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val activeLineLeadMs = 120L
    val activeIndex = remember(lines, playbackMs) {
        lines.indexOfLast { (it.startMs ?: 0L) <= (playbackMs + activeLineLeadMs) }.coerceAtLeast(0)
    }

    LaunchedEffect(activeIndex) {
        if (lines.isNotEmpty()) {
            val target = activeIndex.coerceIn(0, lines.lastIndex)
            listState.animateScrollToItem(target)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = maxHeight * 0.36f, bottom = maxHeight * 0.44f),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.startMs}-$index" }) { index, line ->
                val isActive = index == activeIndex
                AmbientSyncedLyricLine(
                    line = line,
                    active = isActive,
                    onClick = { line.startMs?.let(onSeek) },
                )
            }
        }
    }
}

@Composable
private fun AmbientSyncedLyricLine(
    line: JellyfinLyricLine,
    active: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.06f else 0.94f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "ambientLyricScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (active) 1.0f else 0.32f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "ambientLyricAlpha",
    )

    Text(
        text = line.text.trim().ifBlank { "♪" },
        textAlign = TextAlign.Center,
        color = if (active) Color(0xFF8FE7FF) else Color.White.copy(alpha = alpha),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = line.startMs != null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun AmbientPlainLyricsList(text: String) {
    val lines = remember(text) { text.trim().lines().filter { it.isNotBlank() } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(lines) { _, line ->
            Text(
                text = line,
                color = Color.White.copy(alpha = 0.70f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AmbientArtworkAndLyricsBody(
    track: VantafynMusicTrack?,
    lyrics: JellyfinLyrics?,
    playbackMs: Long,
    onSeek: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Floating Album Art with Dynamic Glowing Shadow
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(28.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF00E7FF), ambientColor = Color(0xFF6D00FF))
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF141418)),
            contentAlignment = Alignment.Center,
        ) {
            if (track?.artworkUrl != null) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // 3-Line Karaoke Preview
        if (lyrics?.isSynced == true && lyrics.syncedLines.isNotEmpty()) {
            val lines = lyrics.syncedLines
            val activeIndex = lines.indexOfLast { (it.startMs ?: 0L) <= (playbackMs + 120L) }.coerceAtLeast(0)
            val prevLine = lines.getOrNull(activeIndex - 1)
            val currentLine = lines.getOrNull(activeIndex)
            val nextLine = lines.getOrNull(activeIndex + 1)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (prevLine != null) {
                    Text(
                        text = prevLine.text,
                        color = Color.White.copy(alpha = 0.28f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (currentLine != null) {
                    Text(
                        text = currentLine.text,
                        color = Color(0xFF8FE7FF),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (nextLine != null) {
                    Text(
                        text = nextLine.text,
                        color = Color.White.copy(alpha = 0.38f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Text(
                text = track?.title ?: "No track playing",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = listOfNotNull(track?.artist, track?.album).joinToString(" • "),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AmbientTrackHeroFallback(track: VantafynMusicTrack?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF00E7FF))
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF141418)),
            contentAlignment = Alignment.Center,
        ) {
            if (track?.artworkUrl != null) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(60.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = track?.title ?: "No track playing",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = listOfNotNull(track?.artist, track?.album).joinToString(" • "),
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AmbientControlDeck(
    track: VantafynMusicTrack?,
    playbackState: VantafynMusicPlaybackState,
    livePositionMs: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    Surface(
        color = Color(0xFF0E0E13).copy(alpha = 0.88f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF31D7FF).copy(alpha = 0.45f),
                    Color(0xFFFF5277).copy(alpha = 0.28f),
                    Color(0xFF9D4EDD).copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.08f),
                ),
            ),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Track Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "Nothing playing",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(track?.artist, track?.album).joinToString(" — "),
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Gradient Scrubber (consistent with Now Playing)
            val duration = playbackState.durationMs.coerceAtLeast(1L).toFloat()
            val position = livePositionMs.coerceIn(0L, duration.toLong()).toFloat()
            var isDragging by remember { mutableStateOf(false) }
            var dragPosition by remember { mutableFloatStateOf(0f) }
            val displayValue = if (isDragging) dragPosition else position

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.14f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((displayValue / duration).coerceIn(0f, 1f))
                                .fillMaxSize()
                                .background(VantafynGradients.accentHorizontal()),
                        )
                    }
                    Slider(
                        value = displayValue,
                        onValueChange = {
                            isDragging = true
                            dragPosition = it
                        },
                        onValueChangeFinished = {
                            onSeek(dragPosition.toLong())
                            isDragging = false
                        },
                        valueRange = 0f..duration,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF31D7FF),
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                        ),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTime(displayValue.toLong()),
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 11.sp,
                    )
                    Text(
                        text = formatTime(duration.toLong()),
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 11.sp,
                    )
                }
            }

            // Playback Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(12.dp, CircleShape, spotColor = Color(0xFF00E7FF))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(VantafynGradients.AccentColors),
                        )
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == VantafynMusicRepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playbackState.repeatMode == VantafynMusicRepeatMode.One) {
                            Color(0xFFFF5277)
                        } else if (playbackState.repeatMode != VantafynMusicRepeatMode.Off) {
                            Color(0xFF31D7FF)
                        } else {
                            Color.White.copy(alpha = 0.60f)
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverScreenAmbientLayout(
    track: VantafynMusicTrack?,
    playbackState: VantafynMusicPlaybackState,
    lyrics: JellyfinLyrics?,
    isLyricsLoading: Boolean,
    livePositionMs: Long,
    currentTimeText: String,
    batteryPercentage: Int,
    isCharging: Boolean,
    controlsVisible: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // 1. Top Minimal Bar (Clock, Battery, Close) - Auto-fades to prevent burn-in
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(350)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VantafynGlassSurface(
                    variant = VantafynGlassVariant.Chip,
                    cornerRadius = 999.dp,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = currentTimeText,
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 10.sp,
                        )
                        Text(
                            text = "$batteryPercentage%",
                            color = if (isCharging) Color(0xFF00FF9C) else Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        )
                        if (isCharging) {
                            Icon(
                                imageVector = Icons.Rounded.BatteryChargingFull,
                                contentDescription = "Charging",
                                tint = Color(0xFF00FF9C),
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // 2. Track Header Row (Artwork + Marquee Title & Artist) - Auto-fades to prevent burn-in
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(350)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF31D7FF).copy(alpha = 0.40f),
                                    Color(0xFFFF5277).copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.10f),
                                ),
                            ),
                            RoundedCornerShape(12.dp),
                        )
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!track?.artworkUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = "Artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = track?.title.orEmpty().ifBlank { "No track playing" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                    Text(
                        text = listOfNotNull(track?.artist, track?.album).joinToString(" - ").ifBlank { "Vantafyn Ambient" },
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        val hasLyrics = (lyrics?.syncedLines?.isNotEmpty() == true && lyrics.isSynced) || lyrics?.plainText?.isNotBlank() == true

        if (hasLyrics) {
            // 3. Compact Real-time 2-3 Line Karaoke Synced Lyrics Ticker (Full screen when controls fade)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF31D7FF).copy(alpha = 0.25f),
                                Color(0xFFFF5277).copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.06f),
                            ),
                        ),
                        RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (lyrics?.syncedLines?.isNotEmpty() == true && lyrics.isSynced) {
                    val activeIndex = remember(lyrics.syncedLines, livePositionMs) {
                        lyrics.syncedLines.indexOfLast { (it.startMs ?: 0L) <= (livePositionMs + 120L) }.coerceAtLeast(0)
                    }
                    val listState = rememberLazyListState()

                    LaunchedEffect(activeIndex) {
                        if (lyrics.syncedLines.isNotEmpty()) {
                            val target = activeIndex.coerceIn(0, lyrics.syncedLines.lastIndex)
                            listState.animateScrollToItem(target)
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val verticalPadding = (maxHeight * 0.35f).coerceAtLeast(8.dp)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = verticalPadding, bottom = verticalPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            itemsIndexed(lyrics.syncedLines, key = { index, line -> "${line.startMs}-$index" }) { index, line ->
                                val isCurrent = index == activeIndex
                                val isNear = kotlin.math.abs(index - activeIndex) <= 1

                                val scale by animateFloatAsState(
                                    targetValue = if (isCurrent) 1.06f else 0.94f,
                                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                    label = "coverLyricScale",
                                )
                                val alpha by animateFloatAsState(
                                    targetValue = if (isCurrent) 1.0f else if (isNear) 0.45f else 0.18f,
                                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                    label = "coverLyricAlpha",
                                )

                                Text(
                                    text = line.text.trim().ifBlank { "♪" },
                                    color = if (isCurrent) Color(0xFF31D7FF) else Color.White.copy(alpha = alpha),
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (isCurrent) 16.sp else 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth(0.95f)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                                        }
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = line.startMs != null) { line.startMs?.let(onSeek) }
                                        .padding(vertical = 2.dp, horizontal = 4.dp),
                                    lineHeight = if (isCurrent) 20.sp else 16.sp,
                                )
                            }
                        }
                    }
                } else if (lyrics?.plainText?.isNotBlank() == true) {
                    Text(
                        text = lyrics.plainText,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            // No lyrics available: Display prominent Album Art Hero with glowing shadow and gradient border
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLyricsLoading) {
                    Text(
                        text = "Loading lyrics...",
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.92f)
                            .aspectRatio(1f)
                            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF31D7FF).copy(alpha = 0.55f))
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF31D7FF).copy(alpha = 0.65f),
                                        Color(0xFFFF5277).copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.15f),
                                    ),
                                ),
                                RoundedCornerShape(20.dp),
                            )
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!track?.artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = "Artwork",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.50f),
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }
                }
            }
        }

        // 4. Compact Control Deck with Subtle Gradient Border - Auto-fades to prevent burn-in
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(350)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                color = Color(0xFF0E0E13).copy(alpha = 0.88f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF31D7FF).copy(alpha = 0.40f),
                            Color(0xFFFF5277).copy(alpha = 0.25f),
                            Color(0xFF9D4EDD).copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.08f),
                        ),
                    ),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Scrubber
                    val duration = (track?.durationMs ?: playbackState.durationMs).coerceAtLeast(1L)
                    var draggingValue by remember { mutableFloatStateOf(-1f) }
                    val displayValue = if (draggingValue >= 0f) draggingValue else livePositionMs.toFloat()

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Slider(
                                value = displayValue.coerceIn(0f, duration.toFloat()),
                                onValueChange = { draggingValue = it },
                                onValueChangeFinished = {
                                    if (draggingValue >= 0f) {
                                        onSeek(draggingValue.toLong())
                                        draggingValue = -1f
                                    }
                                },
                                valueRange = 0f..duration.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                                thumb = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .shadow(4.dp, CircleShape, spotColor = Color(0xFF31D7FF))
                                            .clip(CircleShape)
                                            .background(Color(0xFF31D7FF)),
                                    )
                                },
                                track = { sliderState ->
                                    val rangeDiff = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                                    val fraction = if (rangeDiff > 0f) (sliderState.value - sliderState.valueRange.start) / rangeDiff else 0f

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color.White.copy(alpha = 0.15f)),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(VantafynGradients.accentHorizontal()),
                                        )
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent,
                                ),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatTime(displayValue.toLong()),
                                color = Color.White.copy(alpha = 0.60f),
                                fontSize = 10.sp,
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.White.copy(alpha = 0.60f),
                                fontSize = 10.sp,
                            )
                        }
                    }

                    // Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playbackState.shuffleEnabled) Color(0xFF31D7FF) else Color.White.copy(alpha = 0.60f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(10.dp, CircleShape, spotColor = Color(0xFF00E7FF))
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(VantafynGradients.AccentColors),
                                )
                                .clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        IconButton(onClick = onCycleRepeat, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (playbackState.repeatMode == VantafynMusicRepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                contentDescription = "Repeat",
                                tint = if (playbackState.repeatMode == VantafynMusicRepeatMode.One) {
                                    Color(0xFFFF5277)
                                } else if (playbackState.repeatMode != VantafynMusicRepeatMode.Off) {
                                    Color(0xFF31D7FF)
                                } else {
                                    Color.White.copy(alpha = 0.60f)
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
private fun FlexJukeboxAmbientLayout(
    track: VantafynMusicTrack?,
    playbackState: VantafynMusicPlaybackState,
    lyrics: JellyfinLyrics?,
    isLyricsLoading: Boolean,
    livePositionMs: Long,
    currentTimeText: String,
    batteryPercentage: Int,
    isCharging: Boolean,
    controlsVisible: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // TOP HALF: Upright Artwork & Track Info Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Centered Stage: Artwork + Title & Artist (Stationary, independent of header visibility)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Artwork Hero
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF31D7FF).copy(alpha = 0.5f))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF31D7FF).copy(alpha = 0.6f),
                                    Color(0xFFFF5277).copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.15f),
                                ),
                            ),
                            RoundedCornerShape(24.dp),
                        )
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!track?.artworkUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = "Artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Title & Artist
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth(0.9f),
                ) {
                    Text(
                        text = track?.title.orEmpty().ifBlank { "Nothing playing" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                    Text(
                        text = listOfNotNull(track?.artist, track?.album).joinToString(" - ").ifBlank { "Vantafyn Music" },
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Status Header (Time, Battery, Flex Jukebox Pill & Close) - Floats on top, Auto-fades after 8s
            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(350)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VantafynGlassSurface(
                        variant = VantafynGlassVariant.Chip,
                        cornerRadius = 999.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(currentTimeText, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("•", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            Text("$batteryPercentage%", color = if (isCharging) Color(0xFF00FF9C) else Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            if (isCharging) {
                                Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = Color(0xFF00FF9C), modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                    VantafynGlassSurface(
                        variant = VantafynGlassVariant.Chip,
                        cornerRadius = 999.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("Flex Jukebox", color = Color(0xFF31D7FF), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Hinge Spacer
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFF040508)),
        )

        // BOTTOM HALF: Flat Synced Lyrics & Control Console
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Synced Lyrics Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0E0F16).copy(alpha = 0.85f))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF31D7FF).copy(alpha = 0.30f),
                                    Color(0xFFFF5277).copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.06f),
                                ),
                            ),
                            RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLyricsLoading) {
                        Text(
                            text = "Loading synced lyrics...",
                            color = Color.White.copy(alpha = 0.50f),
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        )
                    } else if (lyrics?.syncedLines?.isNotEmpty() == true && lyrics.isSynced) {
                        AmbientSyncedLyricsList(
                            lines = lyrics.syncedLines,
                            playbackMs = livePositionMs,
                            isPlaying = playbackState.isPlaying,
                            onSeek = onSeek,
                        )
                    } else if (lyrics?.plainText?.isNotBlank() == true) {
                        AmbientPlainLyricsList(text = lyrics.plainText)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(28.dp))
                            Text("Synced lyrics will appear here", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Controls Card
                AmbientControlDeck(
                    track = track,
                    playbackState = playbackState,
                    livePositionMs = livePositionMs,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                )
            }
        }
    }
}

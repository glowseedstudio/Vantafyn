package dev.vantafyn.feature.music

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import dev.vantafyn.feature.music.audiostream.AudioQualityBadgePill
import dev.vantafyn.feature.music.audiostream.AudioStreamDetailsSheet
import dev.vantafyn.feature.music.audiostream.MusicBitrateSettingsIconButton
import dev.vantafyn.feature.music.audiostream.MusicStreamingBitrateSheet
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.isActive
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import dev.vantafyn.core.media.SleepTimerMode
import dev.vantafyn.core.media.VantafynMusicPlaybackState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.repeatOnLifecycle
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import dev.vantafyn.core.cast.GoogleCastRouteButton
import dev.vantafyn.core.jellyfin.JellyfinLyrics
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicArtist
import dev.vantafyn.core.jellyfin.JellyfinLyricLine
import dev.vantafyn.core.jellyfin.JellyfinMusicPlaylist
import dev.vantafyn.core.jellyfin.JellyfinMusicTrack
import dev.vantafyn.core.jellyfin.JellyfinMusicTrackPage
import dev.vantafyn.core.jellyfin.MusicSongsFilter
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.media.VantafynMusicRepeatMode
import dev.vantafyn.core.media.VantafynMusicTrack
import dev.vantafyn.feature.music.harmonia.HarmoniaPeriod
import dev.vantafyn.feature.music.harmonia.HarmoniaPersona
import dev.vantafyn.feature.music.harmonia.HarmoniaRankedItem
import dev.vantafyn.feature.music.harmonia.HarmoniaRecap
import dev.vantafyn.feature.music.harmonia.HarmoniaRecapPreview
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynCircularProgressIndicator
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynErrorCard
import dev.vantafyn.core.ui.VantafynGlassChip
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassDock
import dev.vantafyn.core.ui.VantafynGlassModalPanel
import dev.vantafyn.core.ui.VantafynGlassPanel
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynSkeletonBrush
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.rememberLifecycleAwareMarquee
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val VantafynModalContainerColor: Color
    get() = VantafynColors.Graphite.copy(alpha = 0.96f)

private val MusicBottomSheetRailClearance = 112.dp
private const val SyncedLyricsTickerIntervalMs = 250L

private fun MusicScreenState.scrollResetKey(): String =
    when (this) {
        MusicScreenState.Home -> "home"
        is MusicScreenState.Album -> "album:${album.id}"
        is MusicScreenState.Artist -> "artist:${artist.id}"
        is MusicScreenState.Playlist -> "playlist:${playlist.id}"
        is MusicScreenState.Songs -> "songs"
        is MusicScreenState.HarmoniaRecap -> "harmonia:${preview.id}"
    }

@Composable
fun MusicScreen(
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    onRequestMusicControlsPermission: ((() -> Unit) -> Unit) = { action -> action() },
    onNavigateToDownloads: (() -> Unit)? = null,
    onHarmoniaActiveChanged: ((Boolean) -> Unit)? = null,
    viewModel: MusicViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isHarmoniaActive = state.screen is MusicScreenState.HarmoniaRecap
    LaunchedEffect(isHarmoniaActive) {
        onHarmoniaActiveChanged?.invoke(isHarmoniaActive)
    }
    DisposableEffect(Unit) {
        onDispose {
            onHarmoniaActiveChanged?.invoke(false)
        }
    }
    var actionTrack by remember { mutableStateOf<JellyfinMusicTrack?>(null) }
    var musicContextItem by remember { mutableStateOf<MusicContextItem?>(null) }
    var playlistPickerTracks by remember { mutableStateOf<List<JellyfinMusicTrack>>(emptyList()) }
    val choosePlaylistForTrack: (JellyfinMusicTrack) -> Unit = { track ->
        playlistPickerTracks = listOf(track)
    }
    var showCurrentPlaylistPicker by remember { mutableStateOf(false) }
    var detailsTrack by remember { mutableStateOf<MusicTrackDetails?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var createPlaylistTracks by remember { mutableStateOf<List<JellyfinMusicTrack>>(emptyList()) }
    var selectedTrackIds by remember(state.screen) { mutableStateOf<Set<java.util.UUID>>(emptySet()) }
    var trackToRemoveFromPlaylist by remember { mutableStateOf<JellyfinMusicTrack?>(null) }
    var showBulkRemoveConfirmation by remember { mutableStateOf(false) }
    val toggleSelectTrack: (JellyfinMusicTrack) -> Unit = { track ->
        selectedTrackIds = if (selectedTrackIds.contains(track.id)) {
            selectedTrackIds - track.id
        } else {
            selectedTrackIds + track.id
        }
    }
    val currentScreenTracks = when (val s = state.screen) {
        is MusicScreenState.Album -> s.tracks
        is MusicScreenState.Playlist -> s.tracks
        is MusicScreenState.Songs -> {
            val query = state.songsSearchQuery.trim().lowercase()
            if (query.isEmpty()) s.tracks
            else s.tracks.filter { it.title.lowercase().contains(query) || it.artist.lowercase().contains(query) }
        }
        else -> state.searchResults.ifEmpty { state.home?.songs?.take(20).orEmpty() }
    }
    val unsavedHarmoniaRecaps = remember(state.harmoniaRecaps, state.savedHarmoniaRecaps) {
        val savedIds = state.savedHarmoniaRecaps.map { it.id }.toSet()
        state.harmoniaRecaps.filterNot { it.id in savedIds }
    }
    val startMusic: (() -> Unit) -> Unit = { action -> onRequestMusicControlsPermission(action) }
    val showInitialLoading = state.isLoading &&
        state.home == null &&
        state.searchResults.isEmpty() &&
        state.screen == MusicScreenState.Home
    val musicListState = rememberLazyListState()
    var musicListBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
    val screenScrollKey = state.screen.scrollResetKey()
    val contentRevealKey = when (val s = state.screen) {
        is MusicScreenState.Album -> "album:${s.album.id}"
        is MusicScreenState.Playlist -> "playlist:${s.playlist.id}"
        is MusicScreenState.Songs -> "songs"
        is MusicScreenState.Artist -> "artist:${s.artist.id}"
        else -> screenScrollKey
    }
    val homeRevealKey = "${session?.profileId}-${state.home != null}-${state.searchResults.isNotEmpty()}"
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    var homeRevealActive by remember(homeRevealKey) { mutableStateOf(true) }
    LaunchedEffect(homeRevealKey) {
        homeRevealActive = true
        delay(1_100L)
        homeRevealActive = false
    }
    var nestedRevealActive by remember(contentRevealKey) { mutableStateOf(state.screen != MusicScreenState.Home) }
    LaunchedEffect(contentRevealKey) {
        musicListState.scrollToItem(0)
        if (state.screen != MusicScreenState.Home) {
            nestedRevealActive = true
            delay(1_450L)
            nestedRevealActive = false
        }
    }
    LaunchedEffect(session?.profileId) {
        viewModel.bindSession(session)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        viewModel.setMusicScreenActive(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setMusicScreenActive(true)
                Lifecycle.Event.ON_STOP -> viewModel.setMusicScreenActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setMusicScreenActive(false)
        }
    }
    BackHandler(enabled = state.showNowPlaying || state.screen != MusicScreenState.Home || state.searchQuery.isNotBlank()) {
        if (state.showLyricsScreen) {
            viewModel.closeLyrics()
        } else if (state.showNowPlaying) {
            viewModel.closeNowPlaying()
        } else if (state.screen != MusicScreenState.Home) {
            viewModel.showHome()
        } else if (state.searchQuery.isNotBlank()) {
            viewModel.clearSearch()
        }
    }
    Box(modifier.fillMaxSize()) {
        if (state.screen is MusicScreenState.HarmoniaRecap) {
            val recapScreen = state.screen as MusicScreenState.HarmoniaRecap
            val isRecapSaved = state.selectedHarmoniaRecap?.isSaved
                ?: state.savedHarmoniaRecaps.any { it.id == recapScreen.preview.id }
                || recapScreen.preview.isSaved
            HarmoniaStoryExperience(
                preview = recapScreen.preview,
                recap = state.selectedHarmoniaRecap,
                isLoading = state.isHarmoniaRecapLoading,
                error = state.harmoniaRecapError,
                topTrack = state.harmoniaTopTrack,
                isMuted = state.isHarmoniaMuted,
                isSaved = isRecapSaved,
                onToggleMute = viewModel::toggleHarmoniaMute,
                onToggleSave = { shouldSave -> viewModel.saveHarmoniaRecap(recapScreen.preview.id, shouldSave) },
                onBack = viewModel::showHome,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val quickPicksTracks = remember(state.home, state.recentlyPlayed) {
                val h = state.home
                val candidates = mutableListOf<JellyfinMusicTrack>()
                if (state.recentlyPlayed.isNotEmpty()) {
                    candidates.addAll(state.recentlyPlayed.map { track ->
                        JellyfinMusicTrack(
                            id = track.id,
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            albumId = track.albumId,
                            durationMs = track.durationMs,
                            artworkUrl = track.artworkUrl,
                            hasLyrics = true,
                            streamUrl = track.streamUrl,
                            isFavorite = track.isFavorite,
                            genres = track.genres,
                        )
                    })
                }
                if (h != null && h.onRepeat.isNotEmpty()) {
                    candidates.addAll(h.onRepeat)
                }
                if (h != null && h.recentlyAdded.isNotEmpty()) {
                    candidates.addAll(h.recentlyAdded)
                }
                val seenArtists = mutableSetOf<String>()
                val result = mutableListOf<JellyfinMusicTrack>()
                for (t in candidates) {
                    val norm = t.artist.trim().lowercase()
                    if (norm.isNotBlank() && norm !in seenArtists) {
                        seenArtists.add(norm)
                        result.add(t)
                        if (result.size >= 8) break
                    }
                }
                if (result.size < 4) {
                    for (t in candidates) {
                        if (result.none { it.id == t.id }) {
                            result.add(t)
                            if (result.size >= 8) break
                        }
                    }
                }
                result
            }
            val allHomeTracks = remember(state.home, state.recentlyPlayed) {
                val list = mutableListOf<JellyfinMusicTrack>()
                state.home?.let { h ->
                    list.addAll(h.onRepeat)
                    list.addAll(h.recentlyAdded)
                    list.addAll(h.songs)
                }
                if (state.recentlyPlayed.isNotEmpty()) {
                    list.addAll(state.recentlyPlayed.map { track ->
                        JellyfinMusicTrack(
                            id = track.id,
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            albumId = track.albumId,
                            durationMs = track.durationMs,
                            artworkUrl = track.artworkUrl,
                            hasLyrics = true,
                            streamUrl = track.streamUrl,
                            isFavorite = track.isFavorite,
                            genres = track.genres,
                        )
                    })
                }
                val seen = mutableSetOf<java.util.UUID>()
                list.filter { seen.add(it.id) }
            }
            val energizeKeywords = remember {
                listOf("rock", "metal", "electronic", "dance", "techno", "house", "edm", "pop", "hip hop", "rap", "punk", "dnb", "drum and bass", "synth", "hardcore", "party", "club", "upbeat", "alternative")
            }
            val energizeTracks = remember(allHomeTracks) {
                val matched = allHomeTracks.filter { track ->
                    track.genres.any { g -> energizeKeywords.any { kw -> g.contains(kw, ignoreCase = true) } }
                }
                matched.ifEmpty { allHomeTracks.take(15) }
            }
            val energizeAlbums = remember(state.home?.albums) {
                state.home?.albums.orEmpty().filter { album ->
                    album.genres.any { g -> energizeKeywords.any { kw -> g.contains(kw, ignoreCase = true) } }
                }
            }
            val chillKeywords = remember {
                listOf("chill", "ambient", "lo-fi", "lofi", "jazz", "acoustic", "classical", "piano", "soul", "r&b", "rnb", "indie", "folk", "downtempo", "relax", "sleep", "mellow", "lounge")
            }
            val chillTracks = remember(allHomeTracks) {
                val matched = allHomeTracks.filter { track ->
                    track.genres.any { g -> chillKeywords.any { kw -> g.contains(kw, ignoreCase = true) } }
                }
                matched.ifEmpty { allHomeTracks.takeLast(15) }
            }
            val chillAlbums = remember(state.home?.albums) {
                state.home?.albums.orEmpty().filter { album ->
                    album.genres.any { g -> chillKeywords.any { kw -> g.contains(kw, ignoreCase = true) } }
                }
            }
            val favoriteTracks = remember(allHomeTracks) {
                val matched = allHomeTracks.filter { it.isFavorite }
                matched.ifEmpty { state.home?.onRepeat.orEmpty().take(10) }
            }
            val favoriteAlbums = remember(state.home?.albums) {
                state.home?.albums.orEmpty().filter { it.isFavorite }
            }
            val favoritePlaylists = remember(state.home?.playlists) {
                state.home?.playlists.orEmpty().filter { it.isFavorite }
            }
            LazyColumn(
                state = musicListState,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .imePadding()
                    .onGloballyPositioned { coords ->
                        musicListBoundsInWindow = runCatching { coords.boundsInWindow() }.getOrNull()
                    },
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 18.dp, bottom = if (state.playback.currentTrack != null) 224.dp else 118.dp),
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
            ) {
                val home = state.home
                val isHomeEmpty = home == null || (
                    home.albums.isEmpty() &&
                    home.artists.isEmpty() &&
                    home.playlists.isEmpty() &&
                    home.songs.isEmpty() &&
                    home.recentlyAdded.isEmpty() &&
                    home.onRepeat.isEmpty() &&
                    state.recentlyPlayed.isEmpty() &&
                    state.harmoniaRecaps.isEmpty() &&
                    state.savedHarmoniaRecaps.isEmpty()
                )
                item {
                    if (state.screen == MusicScreenState.Home) {
                        MusicContentReveal(index = 0, animate = homeRevealActive, revealKey = homeRevealKey) {
                            MusicHomeHeader()
                        }
                    } else {
                        val trailingAction: (@Composable () -> Unit)? = when (val s = state.screen) {
                            is MusicScreenState.Album -> {
                                {
                                    MusicTopFavoriteButton(
                                        isFavorite = s.album.isFavorite,
                                        onClick = { viewModel.toggleAlbumFavorite(s.album) },
                                    )
                                }
                            }
                            is MusicScreenState.Playlist -> {
                                {
                                    MusicTopFavoriteButton(
                                        isFavorite = s.playlist.isFavorite,
                                        onClick = { viewModel.togglePlaylistFavorite(s.playlist) },
                                    )
                                }
                            }
                            else -> null
                        }
                        MusicTopBackHeader(
                            title = "Music",
                            onBack = viewModel::showHome,
                            trailingAction = trailingAction,
                        )
                    }
                }
            val errorMsg = state.errorMessage
            if (errorMsg != null && !(isHomeEmpty && state.screen == MusicScreenState.Home)) {
                item {
                    MusicContentReveal(index = 1, animate = state.screen == MusicScreenState.Home && homeRevealActive, revealKey = homeRevealKey) {
                        VantafynErrorCard(errorMsg) { VantafynButton("Retry", onClick = viewModel::loadHome) }
                    }
                }
            }
            if (state.screen == MusicScreenState.Home) {
                item {
                    MusicContentReveal(index = 1, animate = homeRevealActive, revealKey = homeRevealKey) {
                        VantafynTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::search,
                            label = "Search music",
                            placeholder = "Songs, albums, artists",
                            trailingIcon = if (state.searchQuery.isNotBlank()) {
                                {
                                    IconButton(onClick = viewModel::clearSearch) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear search",
                                            tint = VantafynColors.Muted,
                                        )
                                    }
                                }
                            } else null,
                        )
                    }
                }
                if (state.searchQuery.isBlank()) {
                    item {
                        MusicContentReveal(index = 1, animate = homeRevealActive, revealKey = homeRevealKey) {
                            MusicHomeMoodChips(
                                selectedMood = state.selectedHomeMood,
                                onMoodSelected = viewModel::selectHomeMood,
                            )
                        }
                    }
                }
            }
            if (showInitialLoading) {
                item(key = "music-loading-skeleton") {
                    MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                        MusicLoadingSkeleton()
                    }
                }
            }
            when (val screen = state.screen) {
                MusicScreenState.Home -> {
                    if (state.searchResults.isNotEmpty()) {
                        item {
                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                MusicTrackList(
                                    title = "Search Results",
                                    tracks = state.searchResults,
                                    playlists = state.home?.playlists.orEmpty(),
                                    pendingTrackId = state.pendingPlayTrackId,
                                    currentTrackId = state.playback.currentTrack?.id,
                                    onTrack = { track -> startMusic { viewModel.playTrack(track, state.searchResults) } },
                                    onChoosePlaylist = choosePlaylistForTrack,
                                    onLongPress = { actionTrack = it },
                                    animateReveal = false,
                                )
                            }
                        }
                    } else if (state.isSearchLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = VantafynColors.Primary,
                                    strokeWidth = 2.5.dp,
                                )
                            }
                        }
                    } else if (state.searchQuery.isNotBlank() && !state.isLoading) {
                        item {
                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                VantafynGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    cornerRadius = 20.dp,
                                    contentPadding = PaddingValues(24.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = VantafynColors.Muted,
                                            modifier = Modifier.size(36.dp),
                                        )
                                        Text(
                                            text = "No songs found for \"${state.searchQuery.trim()}\"",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = VantafynColors.Ink,
                                            textAlign = TextAlign.Center,
                                        )
                                        Text(
                                            text = "Try searching by song title, artist, or album name",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VantafynColors.Muted,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (state.searchQuery.isBlank()) {
                        if (isHomeEmpty && !state.isLoading && !showInitialLoading) {
                            item(key = "music-offline-empty-state") {
                                MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                    MusicOfflineEmptyState(
                                        onNavigateToDownloads = onNavigateToDownloads,
                                        onRetry = viewModel::loadHome,
                                    )
                                }
                            }
                        } else {
                            state.home?.let { home ->
                                when (state.selectedHomeMood) {
                                    MusicHomeMood.All -> {
                                        if (unsavedHarmoniaRecaps.isNotEmpty()) item {
                                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                HarmoniaRail(
                                                    recaps = unsavedHarmoniaRecaps,
                                                    onRecap = viewModel::openHarmoniaRecap,
                                                )
                                            }
                                        }
                                        if (state.recentlyPlayed.isNotEmpty()) item {
                                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                val recentJellyfinTracks = remember(state.recentlyPlayed) {
                                                    state.recentlyPlayed.map { track ->
                                                        JellyfinMusicTrack(
                                                            id = track.id,
                                                            title = track.title,
                                                            artist = track.artist,
                                                            album = track.album,
                                                            albumId = track.albumId,
                                                            durationMs = track.durationMs,
                                                            artworkUrl = track.artworkUrl,
                                                            hasLyrics = true,
                                                            streamUrl = track.streamUrl,
                                                            isFavorite = track.isFavorite,
                                                            genres = track.genres,
                                                        )
                                                    }
                                                }
                                                MusicTrackRow(
                                                    title = "Recently Played",
                                                    tracks = recentJellyfinTracks,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, recentJellyfinTracks) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (quickPicksTracks.isNotEmpty()) item {
                                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicQuickPicksRow(
                                                    tracks = quickPicksTracks,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onStartRadio = { track -> startMusic { viewModel.startRadio(track) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (home.onRepeat.isNotEmpty()) item {
                                            MusicContentReveal(index = 3, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicOnRepeatRow(
                                                    tracks = home.onRepeat,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, home.onRepeat) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (home.rediscover.isNotEmpty()) item {
                                            MusicContentReveal(index = 3, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicRediscoverRow(
                                                    tracks = home.rediscover,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, home.rediscover) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (home.recentlyAdded.isNotEmpty()) item {
                                            MusicContentReveal(index = 3, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicTrackRow(
                                                    title = "Recently Added",
                                                    tracks = home.recentlyAdded,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, home.recentlyAdded) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (home.albums.isNotEmpty()) item {
                                            MusicContentReveal(index = 4, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicAlbumRow(
                                                    albums = home.albums,
                                                    onAlbum = viewModel::openAlbum,
                                                    onLongPress = { album -> musicContextItem = MusicContextItem.Album(album) },
                                                )
                                            }
                                        }
                                        if (home.similarArtists.isNotEmpty() && !home.similarSeedArtist.isNullOrBlank()) item {
                                            MusicContentReveal(index = 5, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicArtistRow(
                                                    artists = home.similarArtists,
                                                    onArtist = viewModel::openArtist,
                                                    onLongPress = { artist -> musicContextItem = MusicContextItem.Artist(artist) },
                                                    title = "Similar to ${home.similarSeedArtist}",
                                                    icon = Icons.Rounded.AutoAwesome,
                                                )
                                            }
                                        }
                                        if (home.playlists.isNotEmpty()) item {
                                            MusicContentReveal(index = 7, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicPlaylistRow(home.playlists, onPlaylist = viewModel::openPlaylist)
                                            }
                                        }
                                        if (home.songs.isNotEmpty()) item {
                                            MusicContentReveal(index = 8, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                    MusicSectionHeader("Songs", "View all", viewModel::showSongs)
                                                    MusicTrackList(
                                                        title = "",
                                                        tracks = home.songs.take(20),
                                                        playlists = home.playlists,
                                                        pendingTrackId = state.pendingPlayTrackId,
                                                        currentTrackId = state.playback.currentTrack?.id,
                                                        onTrack = { track -> startMusic { viewModel.playTrack(track, home.songs) } },
                                                        onChoosePlaylist = choosePlaylistForTrack,
                                                        onLongPress = { actionTrack = it },
                                                        animateReveal = false,
                                                    )
                                                }
                                            }
                                        }
                                        if (state.savedHarmoniaRecaps.isNotEmpty()) item {
                                            MusicContentReveal(index = 8, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                SavedHarmoniaRail(
                                                    recaps = state.savedHarmoniaRecaps,
                                                    onRecap = viewModel::openHarmoniaRecap,
                                                )
                                            }
                                        }
                                    }
                                    MusicHomeMood.Energize -> {
                                        item {
                                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicMoodHeroCard(
                                                    title = "Energize Mix",
                                                    subtitle = "High-tempo beats & powerful tracks to pump you up",
                                                    icon = Icons.Rounded.Bolt,
                                                    onPlay = {
                                                        if (energizeTracks.isNotEmpty()) {
                                                            startMusic { viewModel.playTrack(energizeTracks.first(), energizeTracks) }
                                                        }
                                                    },
                                                    onStartRadio = energizeTracks.firstOrNull()?.let { seed ->
                                                        { startMusic { viewModel.startRadio(seed) } }
                                                    },
                                                )
                                            }
                                        }
                                        if (energizeTracks.isNotEmpty()) item {
                                            MusicContentReveal(index = 3, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicTrackRow(
                                                    title = "Energize Songs",
                                                    tracks = energizeTracks,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, energizeTracks) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (energizeAlbums.isNotEmpty()) item {
                                            MusicContentReveal(index = 4, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicAlbumRow(
                                                    albums = energizeAlbums,
                                                    title = "Energize Albums",
                                                    onAlbum = viewModel::openAlbum,
                                                    onLongPress = { album -> musicContextItem = MusicContextItem.Album(album) },
                                                )
                                            }
                                        }
                                    }
                                    MusicHomeMood.Chill -> {
                                        item {
                                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicMoodHeroCard(
                                                    title = "Chill Vibe",
                                                    subtitle = "Smooth, mellow, and acoustic rhythms to unwind",
                                                    icon = Icons.Rounded.GraphicEq,
                                                    onPlay = {
                                                        if (chillTracks.isNotEmpty()) {
                                                            startMusic { viewModel.playTrack(chillTracks.first(), chillTracks) }
                                                        }
                                                    },
                                                    onStartRadio = chillTracks.firstOrNull()?.let { seed ->
                                                        { startMusic { viewModel.startRadio(seed) } }
                                                    },
                                                )
                                            }
                                        }
                                        if (chillTracks.isNotEmpty()) item {
                                            MusicContentReveal(index = 3, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicTrackRow(
                                                    title = "Chill Songs",
                                                    tracks = chillTracks,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, chillTracks) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (chillAlbums.isNotEmpty()) item {
                                            MusicContentReveal(index = 4, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicAlbumRow(
                                                    albums = chillAlbums,
                                                    title = "Chill Albums",
                                                    onAlbum = viewModel::openAlbum,
                                                    onLongPress = { album -> musicContextItem = MusicContextItem.Album(album) },
                                                )
                                            }
                                        }
                                    }
                                    MusicHomeMood.OnRepeat -> {
                                        item {
                                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicMoodHeroCard(
                                                    title = "Your Heavy Rotation",
                                                    subtitle = "Tracks you have been spinning on repeat recently",
                                                    icon = Icons.Rounded.Repeat,
                                                    onPlay = {
                                                        if (home.onRepeat.isNotEmpty()) {
                                                            startMusic { viewModel.playTrack(home.onRepeat.first(), home.onRepeat) }
                                                        }
                                                    },
                                                    onStartRadio = home.onRepeat.firstOrNull()?.let { seed ->
                                                        { startMusic { viewModel.startRadio(seed) } }
                                                    },
                                                )
                                            }
                                        }
                                        if (home.onRepeat.isNotEmpty()) item {
                                            MusicContentReveal(index = 3, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicOnRepeatRow(
                                                    tracks = home.onRepeat,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, home.onRepeat) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (home.rediscover.isNotEmpty()) item {
                                            MusicContentReveal(index = 4, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicRediscoverRow(
                                                    tracks = home.rediscover,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, home.rediscover) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (home.onRepeat.isNotEmpty()) item {
                                            MusicContentReveal(index = 5, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                    MusicSectionHeader("Most Played", "View all", viewModel::showSongs)
                                                    MusicTrackList(
                                                        title = "",
                                                        tracks = home.onRepeat,
                                                        playlists = home.playlists,
                                                        pendingTrackId = state.pendingPlayTrackId,
                                                        currentTrackId = state.playback.currentTrack?.id,
                                                        onTrack = { track -> startMusic { viewModel.playTrack(track, home.onRepeat) } },
                                                        onChoosePlaylist = choosePlaylistForTrack,
                                                        onLongPress = { actionTrack = it },
                                                        animateReveal = false,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    MusicHomeMood.Favorites -> {
                                        item {
                                            MusicContentReveal(index = 2, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicMoodHeroCard(
                                                    title = "Liked & Loved",
                                                    subtitle = "Your favorite tracks, albums, and curated playlists",
                                                    icon = Icons.Rounded.Favorite,
                                                    onPlay = {
                                                        if (favoriteTracks.isNotEmpty()) {
                                                            startMusic { viewModel.playTrack(favoriteTracks.first(), favoriteTracks) }
                                                        }
                                                    },
                                                    onStartRadio = favoriteTracks.firstOrNull()?.let { seed ->
                                                        { startMusic { viewModel.startRadio(seed) } }
                                                    },
                                                )
                                            }
                                        }
                                        if (favoriteTracks.isNotEmpty()) item {
                                            MusicContentReveal(index = 3, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicTrackRow(
                                                    title = "Favorite Songs",
                                                    tracks = favoriteTracks,
                                                    pendingTrackId = state.pendingPlayTrackId,
                                                    onTrack = { track -> startMusic { viewModel.playTrack(track, favoriteTracks) } },
                                                    onLongPress = { track -> musicContextItem = MusicContextItem.Track(track) },
                                                )
                                            }
                                        }
                                        if (favoriteAlbums.isNotEmpty()) item {
                                            MusicContentReveal(index = 4, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicAlbumRow(
                                                    albums = favoriteAlbums,
                                                    title = "Favorite Albums",
                                                    onAlbum = viewModel::openAlbum,
                                                    onLongPress = { album -> musicContextItem = MusicContextItem.Album(album) },
                                                )
                                            }
                                        }
                                        if (favoritePlaylists.isNotEmpty()) item {
                                            MusicContentReveal(index = 5, animate = homeRevealActive, revealKey = homeRevealKey) {
                                                MusicPlaylistRow(
                                                    playlists = favoritePlaylists,
                                                    title = "Favorite Playlists",
                                                    onPlaylist = viewModel::openPlaylist,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
            }
                is MusicScreenState.Album -> {
                    item(key = "album-header-${screen.album.id}") {
                        MusicContentReveal(index = 0, animate = nestedRevealActive, revealKey = contentRevealKey) {
                            MusicDetailHeader(
                                title = screen.album.title,
                                subtitle = screen.album.artist ?: "Album",
                                imageUrl = screen.album.artworkUrl,
                                onBack = viewModel::showHome,
                                onDownload = { viewModel.queueAlbumDownload(screen.album, screen.tracks) },
                                isDownloaded = state.isPlaylistDownloaded,
                                isDownloading = state.isPlaylistDownloading,
                                downloadProgress = state.playlistDownloadProgress,
                                isFavorite = screen.album.isFavorite,
                                onToggleFavorite = { viewModel.toggleAlbumFavorite(screen.album) },
                            ) {
                                screen.tracks.firstOrNull()?.let { track -> startMusic { viewModel.playTrack(track, screen.tracks) } }
                            }
                        }
                    }
                    item(key = "album-tracks-${screen.album.id}") {
                        MusicTrackList(
                            title = "Tracks",
                            tracks = screen.tracks,
                            page = screen.page,
                            isPageLoading = state.isMusicPageLoading,
                            onPreviousPage = viewModel::previousMusicPage,
                            onNextPage = viewModel::nextMusicPage,
                            playlists = state.home?.playlists.orEmpty(),
                            pendingTrackId = state.pendingPlayTrackId,
                            currentTrackId = state.playback.currentTrack?.id,
                            selectedTrackIds = selectedTrackIds,
                            onToggleSelectTrack = toggleSelectTrack,
                            onLongPressTrack = toggleSelectTrack,
                            onTrack = { track -> startMusic { viewModel.playTrack(track, screen.tracks) } },
                            onChoosePlaylist = choosePlaylistForTrack,
                            onLongPress = { actionTrack = it },
                            animateReveal = true,
                        )
                    }
                }
                is MusicScreenState.Artist -> {
                    item {
                        MusicContentReveal(index = 0, animate = nestedRevealActive, revealKey = contentRevealKey) {
                            MusicDetailHeader(
                                title = screen.artist.name,
                                subtitle = "Artist",
                                imageUrl = screen.artist.imageUrl,
                                onBack = viewModel::showHome,
                                onPlay = { startMusic { viewModel.startArtistRadio(screen.artist) } },
                            )
                        }
                    }
                    item {
                        MusicContentReveal(index = 1, animate = nestedRevealActive, revealKey = contentRevealKey) {
                            if (screen.albums.isEmpty()) {
                                VantafynGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 20.dp,
                                    contentPadding = PaddingValues(16.dp),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.06f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Rounded.Album, contentDescription = null, tint = VantafynColors.Muted, modifier = Modifier.size(20.dp))
                                        }
                                        Text("No albums found for this artist yet.", color = VantafynColors.Muted)
                                    }
                                }
                            } else {
                                MusicAlbumRow(
                                    albums = screen.albums,
                                    onAlbum = viewModel::openAlbum,
                                    onLongPress = { album -> musicContextItem = MusicContextItem.Album(album) },
                                )
                            }
                        }
                    }
                    if (screen.similarArtists.isNotEmpty()) {
                        item {
                            MusicContentReveal(index = 2, animate = nestedRevealActive, revealKey = contentRevealKey) {
                                MusicArtistRow(
                                    artists = screen.similarArtists,
                                    onArtist = viewModel::openArtist,
                                    onLongPress = { artist -> musicContextItem = MusicContextItem.Artist(artist) },
                                    title = "Fans Also Like",
                                    icon = Icons.Rounded.AutoAwesome,
                                )
                            }
                        }
                    }
                }
                is MusicScreenState.Playlist -> {
                    item(key = "playlist-header-${screen.playlist.id}") {
                        MusicContentReveal(index = 0, animate = nestedRevealActive, revealKey = contentRevealKey) {
                            MusicDetailHeader(
                                title = screen.playlist.name,
                                subtitle = "${screen.page.totalItems.coerceAtLeast(screen.tracks.size)} tracks",
                                imageUrl = screen.playlist.imageUrl,
                                onBack = viewModel::showHome,
                                onDownload = { viewModel.queuePlaylistDownload(screen.playlist, screen.tracks) },
                                isDownloaded = state.isPlaylistDownloaded,
                                isDownloading = state.isPlaylistDownloading,
                                downloadProgress = state.playlistDownloadProgress,
                                trackImageUrls = screen.playlist.trackImageUrls,
                                onToggleReorder = viewModel::toggleReorderMode,
                                isReordering = state.isReorderMode,
                                isFavorite = screen.playlist.isFavorite,
                                onToggleFavorite = { viewModel.togglePlaylistFavorite(screen.playlist) },
                            ) {
                                screen.tracks.firstOrNull()?.let { track -> startMusic { viewModel.playTrack(track, screen.tracks) } }
                            }
                        }
                    }
                    item(key = "playlist-tracks-${screen.playlist.id}") {
                        MusicTrackList(
                            title = "Playlist",
                            tracks = screen.tracks,
                            page = screen.page,
                            isPageLoading = state.isMusicPageLoading,
                            onPreviousPage = viewModel::previousMusicPage,
                            onNextPage = viewModel::nextMusicPage,
                            playlists = state.home?.playlists.orEmpty(),
                            pendingTrackId = state.pendingPlayTrackId,
                            currentTrackId = state.playback.currentTrack?.id,
                            selectedTrackIds = selectedTrackIds,
                            onToggleSelectTrack = toggleSelectTrack,
                            onLongPressTrack = toggleSelectTrack,
                            onTrack = { track -> startMusic { viewModel.playTrack(track, screen.tracks) } },
                            onChoosePlaylist = choosePlaylistForTrack,
                            onLongPress = { actionTrack = it },
                            animateReveal = true,
                            isReorderMode = state.isReorderMode,
                            onReorder = viewModel::movePlaylistTrack,
                            onToggleReorderMode = viewModel::toggleReorderMode,
                            lazyListState = musicListState,
                            viewportBoundsInWindow = musicListBoundsInWindow,
                        )
                    }
                }
                is MusicScreenState.Songs -> {
                    val songsQuery = state.songsSearchQuery.trim().lowercase()
                    val filteredTracks = if (songsQuery.isEmpty()) screen.tracks
                    else screen.tracks.filter {
                        it.title.lowercase().contains(songsQuery) || it.artist.lowercase().contains(songsQuery)
                    }
                    item {
                        MusicContentReveal(index = 0, animate = nestedRevealActive, revealKey = contentRevealKey) {
                            MusicSongsFilterChips(
                                selected = state.songsFilter,
                                onSelected = viewModel::setSongsFilter,
                            )
                        }
                    }
                    if (state.songsFilter.supportsMusicSongsAlphabetRail()) {
                        item {
                            MusicContentReveal(index = 1, animate = nestedRevealActive, revealKey = contentRevealKey) {
                                MusicSongsAlphabetRail(
                                    selected = state.songsAlphabetKey,
                                    enabled = !state.isMusicPageLoading,
                                    onSelected = viewModel::setSongsAlphabetKey,
                                )
                            }
                        }
                    }
                    item {
                        MusicContentReveal(index = 2, animate = nestedRevealActive, revealKey = contentRevealKey) {
                            VantafynTextField(
                                value = state.songsSearchQuery,
                                onValueChange = viewModel::filterSongsLocally,
                                label = "Search songs",
                                placeholder = "Filter by title or artist",
                            )
                        }
                    }
                    item {
                        MusicContentReveal(index = 3, animate = nestedRevealActive, revealKey = contentRevealKey) {
                            MusicTrackList(
                                title = "All Songs",
                                tracks = filteredTracks,
                                page = screen.page,
                                isPageLoading = state.isMusicPageLoading,
                                onPreviousPage = viewModel::previousMusicPage,
                                onNextPage = viewModel::nextMusicPage,
                                playlists = state.home?.playlists.orEmpty(),
                                pendingTrackId = state.pendingPlayTrackId,
                                currentTrackId = state.playback.currentTrack?.id,
                                selectedTrackIds = selectedTrackIds,
                                onToggleSelectTrack = toggleSelectTrack,
                                onLongPressTrack = toggleSelectTrack,
                                onTrack = { track -> startMusic { viewModel.playTrack(track, filteredTracks) } },
                                onChoosePlaylist = choosePlaylistForTrack,
                                onLongPress = { actionTrack = it },
                                animateReveal = false,
                            )
                        }
                    }
                    if (screen.page.hasNext && state.songsSearchQuery.isBlank()) {
                        item(key = "songs-load-more-trigger") {
                            LaunchedEffect(screen.page.tracks.size) {
                                viewModel.loadMoreSongs()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = VantafynColors.Primary,
                                    strokeWidth = 2.5.dp,
                                )
                            }
                        }
                    }
                }
                is MusicScreenState.HarmoniaRecap -> Unit
            }
        }
        }
        if (state.screen !is MusicScreenState.HarmoniaRecap) {
            MusicStatusBarScrim(
                alpha = 0.92f,
                statusBarHeight = statusBarHeight,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        AnimatedVisibility(
            visible = state.playback.currentTrack != null && state.screen !is MusicScreenState.HarmoniaRecap,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight + 96 },
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight + 96 },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
        ) {
            state.playback.currentTrack?.let {
            MusicMiniPlayer(
                track = it,
                isPlaying = state.playback.isPlaying,
                progress = progressFraction(state.playback.positionMs, state.playback.durationMs),
                isScrolling = musicListState.isScrollInProgress,
                onOpen = viewModel::openNowPlaying,
                onToggle = {
                    if (state.playback.isPlaying) {
                        viewModel.togglePlayPause()
                    } else {
                        startMusic { viewModel.togglePlayPause() }
                    }
                },
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onStop = viewModel::stopMusic,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(start = 8.dp, end = 8.dp, bottom = 96.dp),
            )
            }
        }
        if (state.showNowPlaying) {
            if (state.showLyricsScreen) {
                LyricsScreen(state = state, viewModel = viewModel)
            } else {
                NowPlayingDialog(
                    state = state,
                    viewModel = viewModel,
                    onRequestMusicControlsPermission = startMusic,
                    onChoosePlaylist = { showCurrentPlaylistPicker = true },
                    onTrackDetails = { detailsTrack = it },
                )
            }
        }
        if (state.isPlaylistSaving) {
            VantafynGlassModalPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = 150.dp)
                    .vantafynAnimatedModalBorder(cornerRadius = 22.dp, strokeWidth = 1.2.dp),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
                VantafynLoadingIndicator("Creating playlist...")
            }
        }
        state.message?.let { message ->
            LaunchedEffect(message) {
                delay(1_400L)
                viewModel.clearMessage()
            }
            MusicSuccessToast(
                message = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = if (state.playback.currentTrack != null) 166.dp else 110.dp),
            )
        }
        actionTrack?.let { track ->
            MusicTrackContextMenu(
                track = track,
                playlists = state.home?.playlists.orEmpty(),
                onDismiss = { actionTrack = null },
                onPlay = {
                    actionTrack = null
                    startMusic { viewModel.playTrack(track, listOf(track)) }
                },
                onPlayNext = {
                    actionTrack = null
                    viewModel.playNext(track)
                },
                onAddToQueue = {
                    actionTrack = null
                    viewModel.addToQueue(track)
                },
                onDownload = {
                    actionTrack = null
                    viewModel.queueTrackDownload(track)
                },
                onChoosePlaylist = {
                    actionTrack = null
                    choosePlaylistForTrack(track)
                },
                onGoToAlbum = {
                    actionTrack = null
                    viewModel.openTrackAlbum(track)
                },
                onTrackDetails = {
                    actionTrack = null
                    detailsTrack = track.toDetails()
                },
                onRemoveFromPlaylist = if (state.screen is MusicScreenState.Playlist) {
                    {
                        actionTrack = null
                        trackToRemoveFromPlaylist = track
                    }
                } else null,
                onSelectTracks = {
                    actionTrack = null
                    selectedTrackIds = setOf(track.id)
                },
            )
        }
        musicContextItem?.let { item ->
            when (item) {
                is MusicContextItem.Track -> {
                    val track = item.track
                    MusicContextActionSheet(
                        title = track.title,
                        subtitle = track.artist,
                        artworkUrl = track.artworkUrl,
                        isFavorite = track.isFavorite,
                        onDismiss = { musicContextItem = null },
                        onPlay = {
                            musicContextItem = null
                            startMusic { viewModel.playTrack(track, listOf(track)) }
                        },
                        onPlayNext = {
                            musicContextItem = null
                            viewModel.playNext(track)
                        },
                        onAddToQueue = {
                            musicContextItem = null
                            viewModel.addToQueue(track)
                        },
                        onStartRadio = {
                            musicContextItem = null
                            startMusic { viewModel.startRadio(track) }
                        },
                        onToggleFavorite = {
                            musicContextItem = null
                            viewModel.toggleFavorite(track)
                        },
                        onAddToPlaylist = {
                            musicContextItem = null
                            choosePlaylistForTrack(track)
                        },
                        onDownload = {
                            musicContextItem = null
                            viewModel.queueTrackDownload(track)
                        },
                    )
                }
                is MusicContextItem.Album -> {
                    val album = item.album
                    MusicContextActionSheet(
                        title = album.title,
                        subtitle = album.artist ?: "Album",
                        artworkUrl = album.artworkUrl,
                        onDismiss = { musicContextItem = null },
                        onPlay = {
                            musicContextItem = null
                            startMusic { viewModel.playAlbum(album) }
                        },
                        onPlayNext = {
                            musicContextItem = null
                            viewModel.playAlbumNext(album.id)
                        },
                        onAddToQueue = {
                            musicContextItem = null
                            viewModel.addAlbumToQueue(album.id)
                        },
                        onToggleFavorite = null,
                        onAddToPlaylist = null,
                        onDownload = {
                            musicContextItem = null
                            viewModel.downloadAlbum(album.id)
                        },
                        onStartRadio = {
                            musicContextItem = null
                            startMusic { viewModel.startAlbumRadio(album) }
                        },
                    )
                }
                is MusicContextItem.Artist -> {
                    val artist = item.artist
                    MusicContextActionSheet(
                        title = artist.name,
                        subtitle = "Artist",
                        artworkUrl = artist.imageUrl,
                        onDismiss = { musicContextItem = null },
                        onPlay = {
                            musicContextItem = null
                            viewModel.openArtist(artist)
                        },
                        onPlayNext = {
                            musicContextItem = null
                            viewModel.playArtistNext(artist.id)
                        },
                        onAddToQueue = {
                            musicContextItem = null
                            viewModel.addArtistToQueue(artist.id)
                        },
                        onToggleFavorite = null,
                        onAddToPlaylist = null,
                        onDownload = null,
                        onStartRadio = {
                            musicContextItem = null
                            startMusic { viewModel.startArtistRadio(artist) }
                        },
                    )
                }
            }
        }
        detailsTrack?.let { track ->
            MusicTrackDetailsSheet(
                track = track,
                onDismiss = { detailsTrack = null },
            )
        }
        if (playlistPickerTracks.isNotEmpty()) {
            MusicPlaylistPickerSheet(
                playlists = state.home?.playlists.orEmpty(),
                onDismiss = { playlistPickerTracks = emptyList() },
                onPlaylist = { playlist ->
                    val tracksToAdd = playlistPickerTracks
                    playlistPickerTracks = emptyList()
                    viewModel.addTracksToPlaylist(playlist, tracksToAdd)
                },
                onCreateNew = {
                    createPlaylistTracks = playlistPickerTracks
                    playlistPickerTracks = emptyList()
                    showCreatePlaylistDialog = true
                },
            )
        }
        if (showCurrentPlaylistPicker) {
            MusicPlaylistPickerSheet(
                playlists = state.home?.playlists.orEmpty(),
                onDismiss = { showCurrentPlaylistPicker = false },
                onPlaylist = { playlist ->
                    showCurrentPlaylistPicker = false
                    viewModel.addCurrentToPlaylist(playlist)
                },
                onCreateNew = {
                    showCurrentPlaylistPicker = false
                    createPlaylistTracks = emptyList()
                    showCreatePlaylistDialog = true
                },
            )
        }
        if (showCreatePlaylistDialog) {
            var newName by remember { mutableStateOf("") }
            AlertDialog(
                modifier = Modifier
                    .imePadding()
                    .vantafynAnimatedModalBorder(),
                onDismissRequest = { showCreatePlaylistDialog = false },
                containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
                shape = RoundedCornerShape(28.dp),
                title = { Text("New playlist", color = VantafynColors.Ink, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                        VantafynTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = "Playlist name",
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = newName.trim().ifBlank { "Vantafyn Playlist" }
                            val tracks = createPlaylistTracks
                            showCreatePlaylistDialog = false
                            createPlaylistTracks = emptyList()
                            viewModel.createPlaylistAndAddTracks(name, tracks)
                        },
                        enabled = newName.isNotBlank(),
                    ) {
                        Text("Create", color = VantafynColors.Primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = { TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel", color = VantafynColors.Muted) } },
            )
        }
        state.playlistDuplicatePrompt?.let { prompt ->
            MusicPlaylistDuplicateDialog(
                prompt = prompt,
                onConfirm = { tracksToAdd ->
                    viewModel.confirmDuplicateAddToPlaylist(prompt.playlist, tracksToAdd)
                },
                onDismiss = viewModel::dismissPlaylistDuplicatePrompt,
            )
        }
        trackToRemoveFromPlaylist?.let { track ->
            val playlist = (state.screen as? MusicScreenState.Playlist)?.playlist
            if (playlist != null) {
                AlertDialog(
                    modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 28.dp),
                    onDismissRequest = { trackToRemoveFromPlaylist = null },
                    containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(28.dp),
                    title = { Text("Remove from playlist", color = VantafynColors.Ink, fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to remove \"${track.title}\" from \"${playlist.name}\"?", color = VantafynColors.Ink.copy(alpha = 0.85f)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val toRemove = track
                                trackToRemoveFromPlaylist = null
                                viewModel.removeTracksFromPlaylist(playlist, listOf(toRemove))
                            }
                        ) {
                            Text("Remove", color = VantafynColors.Destructive, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { trackToRemoveFromPlaylist = null }) {
                            Text("Cancel", color = VantafynColors.Muted)
                        }
                    },
                )
            }
        }
        if (showBulkRemoveConfirmation) {
            val playlist = (state.screen as? MusicScreenState.Playlist)?.playlist
            if (playlist != null) {
                val tracksToRemove = currentScreenTracks.filter { selectedTrackIds.contains(it.id) }
                AlertDialog(
                    modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 28.dp),
                    onDismissRequest = { showBulkRemoveConfirmation = false },
                    containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(28.dp),
                    title = { Text("Remove from playlist", color = VantafynColors.Ink, fontWeight = FontWeight.Bold) },
                    text = { Text("Remove ${tracksToRemove.size} songs from \"${playlist.name}\"?", color = VantafynColors.Ink.copy(alpha = 0.85f)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showBulkRemoveConfirmation = false
                                selectedTrackIds = emptySet()
                                viewModel.removeTracksFromPlaylist(playlist, tracksToRemove)
                            }
                        ) {
                            Text("Remove", color = VantafynColors.Destructive, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBulkRemoveConfirmation = false }) {
                            Text("Cancel", color = VantafynColors.Muted)
                        }
                    },
                )
            }
        }
        if (selectedTrackIds.isNotEmpty()) {
            val isAllSelected = currentScreenTracks.isNotEmpty() && currentScreenTracks.all { selectedTrackIds.contains(it.id) }
            MusicMultiSelectActionBar(
                selectedCount = selectedTrackIds.size,
                isAllSelected = isAllSelected,
                onToggleSelectAll = {
                    selectedTrackIds = if (isAllSelected) {
                        emptySet()
                    } else {
                        currentScreenTracks.map { it.id }.toSet()
                    }
                },
                onAddToPlaylist = {
                    val selected = currentScreenTracks.filter { selectedTrackIds.contains(it.id) }
                    selectedTrackIds = emptySet()
                    playlistPickerTracks = selected
                },
                onQueue = {
                    val selected = currentScreenTracks.filter { selectedTrackIds.contains(it.id) }
                    selectedTrackIds = emptySet()
                    selected.forEach { viewModel.addToQueue(it) }
                    viewModel.showMessage("Added ${selected.size} tracks to queue")
                },
                onRemoveFromPlaylist = if (state.screen is MusicScreenState.Playlist) {
                    { showBulkRemoveConfirmation = true }
                } else null,
                onClose = { selectedTrackIds = emptySet() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = if (state.playback.currentTrack != null) 166.dp else 108.dp),
            )
        }
    }
}

@Composable
private fun MusicStatusBarScrim(alpha: Float, statusBarHeight: Dp, modifier: Modifier = Modifier) {
    if (alpha <= 0.001f) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + 72.dp)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Black.copy(alpha = 0.74f * alpha),
                        0.42f to VantafynColors.Graphite.copy(alpha = 0.58f * alpha),
                        0.72f to VantafynColors.Graphite.copy(alpha = 0.20f * alpha),
                        1.00f to Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
private fun MusicHomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            "Music",
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HarmoniaRail(
    recaps: List<HarmoniaRecapPreview>,
    onRecap: (HarmoniaRecapPreview) -> Unit,
) {
    val uniqueRecaps = remember(recaps) {
        recaps
            .sortedWith(compareByDescending<HarmoniaRecapPreview> { it.periodStart }.thenByDescending { it.generatedAt })
            .distinctBy { "${it.periodType}:${it.periodStart.toEpochMilli()}" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Harmonia",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Your music, remembered.",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        ) {
            items(uniqueRecaps, key = { it.id }) { recap ->
                HarmoniaRecapCard(
                    preview = recap,
                    yearly = recap.periodType == HarmoniaPeriod.YEARLY,
                    onClick = { onRecap(recap) },
                )
            }
        }
    }
}

@Composable
private fun HarmoniaRecapCard(
    preview: HarmoniaRecapPreview,
    yearly: Boolean,
    onClick: () -> Unit,
) {
    val ambient = if (yearly) 0.65f else 0.46f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.982f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.84f),
        label = "harmoniaCardPress",
    )
    val shape = RoundedCornerShape(if (yearly) 26.dp else 22.dp)
    Box(
        modifier = Modifier
            .width(if (yearly) 304.dp else 268.dp)
            .height(if (yearly) 176.dp else 156.dp)
            .scale(scale)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        VantafynColors.Graphite.copy(alpha = 0.92f),
                        VantafynColors.Surface.copy(alpha = 0.82f),
                        VantafynColors.Graphite.copy(alpha = 0.94f),
                    ),
                    start = Offset.Zero,
                    end = Offset(520f, 260f),
                ),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        if (!preview.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = preview.artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
                    .alpha(0.35f),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VantafynGradients.AccentColors.first().copy(alpha = 0.32f * ambient),
                            VantafynGradients.AccentColors.last().copy(alpha = 0.18f * ambient),
                            Color.Transparent,
                        ),
                        center = Offset(if (yearly) 64f else 46f, if (yearly) 42f else 34f),
                        radius = if (yearly) 360f else 260f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            VantafynGradients.AccentColors.last().copy(alpha = 0.12f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (yearly) 18.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
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
                            .size(if (yearly) 36.dp else 32.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(VantafynGradients.accentHorizontal()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (yearly) Icons.Rounded.AutoAwesome else Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (yearly) 20.dp else 17.dp),
                        )
                    }
                    Text(
                        preview.periodLabel(),
                        color = VantafynColors.Ink.copy(alpha = 0.95f),
                        style = if (yearly) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                if (yearly) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "YEARLY",
                            color = Color(0xFFFFD166),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        if (yearly) "Year in Review" else "Month in Review",
                        color = VantafynColors.Ink.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        preview.totalListeningTimeMs.formatListeningTime(),
                        color = VantafynColors.Ink,
                        style = if (yearly) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        preview.harmoniaTease(),
                        color = VantafynColors.Muted.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(width = if (yearly) 68.dp else 60.dp, height = if (yearly) 58.dp else 52.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = if (yearly) 16.dp else 14.dp)
                            .size(if (yearly) 52.dp else 46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10131A))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (yearly) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .border(0.75.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(if (yearly) 16.dp else 14.dp)
                                .clip(CircleShape)
                                .background(VantafynGradients.accentHorizontal()),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(if (yearly) 54.dp else 48.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(VantafynColors.Surface)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!preview.artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = preview.artworkUrl,
                                contentDescription = preview.topTrack ?: preview.topArtist,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(VantafynGradients.accentHorizontal()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
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
private fun SavedHarmoniaRail(
    recaps: List<HarmoniaRecapPreview>,
    onRecap: (HarmoniaRecapPreview) -> Unit,
) {
    val uniqueRecaps = remember(recaps) {
        recaps
            .sortedWith(compareByDescending<HarmoniaRecapPreview> { it.periodStart }.thenByDescending { it.generatedAt })
            .distinctBy { "${it.periodType}:${it.periodStart.toEpochMilli()}" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VantafynGradients.accentHorizontal()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    "Saved Recaps",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "Your Harmonia music memories, kept forever.",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        ) {
            items(uniqueRecaps, key = { "saved-${it.id}" }) { recap ->
                SavedHarmoniaCard(
                    preview = recap,
                    onClick = { onRecap(recap) },
                )
            }
        }
    }
}

@Composable
private fun SavedHarmoniaCard(
    preview: HarmoniaRecapPreview,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.84f),
        label = "savedHarmoniaPress",
    )
    val yearly = preview.periodType == HarmoniaPeriod.YEARLY
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .width(if (yearly) 296.dp else 260.dp)
            .height(if (yearly) 168.dp else 156.dp)
            .scale(scale)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        VantafynColors.Graphite.copy(alpha = 0.92f),
                        VantafynColors.Surface.copy(alpha = 0.82f),
                        VantafynColors.Graphite.copy(alpha = 0.94f),
                    ),
                ),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        if (!preview.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = preview.artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(26.dp)
                    .alpha(0.32f),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD166).copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                        center = Offset(40f, 30f),
                        radius = 260f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color(0xFFFFD166).copy(alpha = 0.08f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFD166).copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        preview.periodLabel(),
                        color = VantafynColors.Ink,
                        style = if (yearly) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                if (yearly) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFFFD166).copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "YEARLY",
                            color = Color(0xFFFFD166),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        if (yearly) "Year in Review" else "Month in Review",
                        color = VantafynColors.Ink.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        preview.totalListeningTimeMs.formatListeningTime(),
                        color = VantafynColors.Ink,
                        style = if (yearly) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        preview.harmoniaTease(),
                        color = VantafynColors.Muted.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(width = if (yearly) 68.dp else 60.dp, height = if (yearly) 58.dp else 52.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = if (yearly) 16.dp else 14.dp)
                            .size(if (yearly) 52.dp else 46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10131A))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (yearly) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .border(0.75.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(if (yearly) 16.dp else 14.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFFFD166),
                                            VantafynGradients.AccentColors.first(),
                                        ),
                                    ),
                                ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(if (yearly) 54.dp else 48.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(VantafynColors.Surface)
                            .border(1.dp, Color(0xFFFFD166).copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!preview.artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = preview.artworkUrl,
                                contentDescription = preview.topTrack ?: preview.topArtist,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFFD166).copy(alpha = 0.8f),
                                                VantafynGradients.AccentColors.first(),
                                            ),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmark,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
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
private fun HarmoniaStoryExperience(
    preview: HarmoniaRecapPreview,
    recap: HarmoniaRecap?,
    isLoading: Boolean,
    error: String?,
    topTrack: JellyfinMusicTrack?,
    isMuted: Boolean,
    isSaved: Boolean,
    onToggleMute: () -> Unit,
    onToggleSave: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val slides = remember(recap, isSaved, context) { recap?.harmoniaStorySlides(context, isSaved).orEmpty() }
    val slideCount = slides.size
    var slideIndex by rememberSaveable(preview.id, slideCount) { mutableIntStateOf(0) }
    LaunchedEffect(slideCount) {
        if (slideCount > 0) slideIndex = slideIndex.coerceIn(0, slideCount - 1)
    }

    var isHolding by remember { mutableStateOf(false) }
    val slideProgress = remember { Animatable(0f) }
    var lastSlideIndex by remember { mutableIntStateOf(-1) }

    // Auto-advance timer (6.5s per slide, pauses when holding)
    LaunchedEffect(slideIndex, isHolding, slideCount) {
        if (slideCount <= 0 || isHolding) return@LaunchedEffect
        if (lastSlideIndex != slideIndex) {
            slideProgress.snapTo(0f)
            lastSlideIndex = slideIndex
        }
        val current = slideProgress.value
        val durationMs = ((1f - current) * 6500f).toLong().toInt().coerceIn(100, 6500)
        val animation = slideProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = LinearEasing,
            ),
        )
        if (animation.endReason == AnimationEndReason.Finished && slideProgress.value >= 0.99f) {
            slideProgress.snapTo(0f)
            if (slideIndex < slideCount - 1) {
                slideIndex++
            }
        }
    }

    val currentSlide = slides.getOrNull(slideIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VantafynColors.Graphite),
    ) {
        HarmoniaNebulaBackdrop(slideIndex, currentSlide?.artworkUrl ?: topTrack?.artworkUrl ?: preview.artworkUrl)

        // Gesture handling layer for touch-to-hold, swipe, and tap
        if (slides.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slideCount, slideIndex) {
                        awaitEachGesture {
                            try {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                isHolding = true
                                var dragX = 0f
                                var hasDragged = false
                                val startTime = System.currentTimeMillis()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        val dx = change.position.x - change.previousPosition.x
                                        dragX += dx
                                        if (kotlin.math.abs(dragX) > 20f) {
                                            hasDragged = true
                                        }
                                    } else {
                                        isHolding = false
                                        val elapsed = System.currentTimeMillis() - startTime
                                        if (hasDragged) {
                                            if (dragX < -48f) {
                                                if (slideIndex < slideCount - 1) slideIndex++ else onBack()
                                            } else if (dragX > 48f) {
                                                slideIndex = (slideIndex - 1).coerceAtLeast(0)
                                            }
                                        } else if (elapsed < 350) {
                                            if (down.position.x < size.width * 0.35f) {
                                                slideIndex = (slideIndex - 1).coerceAtLeast(0)
                                            } else {
                                                if (slideIndex < slideCount - 1) slideIndex++ else onBack()
                                            }
                                        }
                                        break
                                    }
                                }
                            } finally {
                                isHolding = false
                            }
                        }
                    },
            )
        }

        when {
            isLoading -> VantafynLoadingIndicator(
                "Loading Harmonia...",
                modifier = Modifier
                    .align(Alignment.Center)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            )
            error != null -> VantafynGlassCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(22.dp),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Harmonia is not ready", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                    Text(error, color = VantafynColors.Muted, textAlign = TextAlign.Center)
                }
            }
            slides.isEmpty() -> VantafynGlassCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(22.dp),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(20.dp),
            ) {
                Text("No story slides are available for this recap yet.", color = VantafynColors.Muted, textAlign = TextAlign.Center)
            }
            else -> {
                val scaleCard by animateFloatAsState(
                    targetValue = if (isHolding) 0.97f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "harmoniaHoldScale",
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    // 1. Top Section: Progress indicators & Header controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HarmoniaStoryProgress(
                            slideIndex = slideIndex,
                            slideCount = slideCount,
                            currentProgress = slideProgress.value,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HarmoniaAudioPill(
                                track = topTrack,
                                isMuted = isMuted,
                                onToggleMute = onToggleMute,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = { onToggleSave(!isSaved) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = if (isSaved) "Saved" else "Save Recap",
                                        tint = if (isSaved) Color(0xFFFFD166) else Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 2. Middle Section: Centered Animated Slide Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .scale(scaleCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = slides[slideIndex],
                            transitionSpec = {
                                (fadeIn(tween(320, easing = FastOutSlowInEasing)) + scaleIn(tween(360, easing = FastOutSlowInEasing), initialScale = 0.93f)) togetherWith
                                    (fadeOut(tween(180, easing = FastOutSlowInEasing)) + scaleOut(tween(220, easing = FastOutSlowInEasing), targetScale = 1.05f))
                            },
                            label = "harmoniaStorySlide",
                            modifier = Modifier.fillMaxWidth(),
                        ) { slide ->
                            HarmoniaStorySlideContent(
                                slide = slide,
                                isHolding = isHolding,
                                onToggleSave = { onToggleSave(!isSaved) },
                                onReplay = { slideIndex = 0 },
                                onBack = onBack,
                            )
                        }
                    }

                    // 3. Bottom Section: Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VantafynGlassChip(
                            selected = false,
                            enabled = slideIndex > 0,
                            onClick = { slideIndex = (slideIndex - 1).coerceAtLeast(0) },
                        ) {
                            Text("Back", color = VantafynColors.Ink, maxLines = 1)
                        }
                        Text(
                            "${slideIndex + 1} / $slideCount",
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (slideIndex == slideCount - 1) {
                                VantafynGlassChip(selected = false, onClick = { slideIndex = 0 }) {
                                    Text("Replay", color = VantafynColors.Ink, maxLines = 1)
                                }
                            }
                            VantafynGlassChip(
                                selected = true,
                                onClick = {
                                    if (slideIndex == slideCount - 1) onBack() else slideIndex += 1
                                },
                            ) {
                                Text(if (slideIndex == slideCount - 1) "Close" else "Next", color = VantafynColors.Ink, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HarmoniaStoryProgress(
    slideIndex: Int,
    slideCount: Int,
    currentProgress: Float,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        repeat(slideCount) { index ->
            val progress = when {
                index < slideIndex -> 1f
                index == slideIndex -> currentProgress.coerceIn(0f, 1f)
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White,
                                        Color(0xFFE2E8F0),
                                    ),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HarmoniaAudioPill(
    track: JellyfinMusicTrack?,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
) {
    if (track != null) {
        val artist = track.artist
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnimatedEqualizerBars(isPlaying = !isMuted)
                Text(
                    text = if (artist.isNotBlank()) "${track.title} • $artist" else track.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp),
                )
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFD166),
                modifier = Modifier.size(16.dp),
            )
            Text(
                "Harmonia Recap",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AnimatedEqualizerBars(isPlaying: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val shouldAnimate = isPlaying && isResumed

    val (h1, h2, h3) = if (shouldAnimate) {
        val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
        val bar1 = infiniteTransition.animateFloat(
            initialValue = 4f,
            targetValue = 14f,
            animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "bar1",
        ).value
        val bar2 = infiniteTransition.animateFloat(
            initialValue = 12f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "bar2",
        ).value
        val bar3 = infiniteTransition.animateFloat(
            initialValue = 6f,
            targetValue = 16f,
            animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "bar3",
        ).value
        Triple(bar1, bar2, bar3)
    } else {
        Triple(4f, 8f, 5f)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.size(width = 13.dp, height = 16.dp),
    ) {
        Box(Modifier.width(2.5.dp).height(h1.dp).clip(RoundedCornerShape(99.dp)).background(Color.White))
        Box(Modifier.width(2.5.dp).height(h2.dp).clip(RoundedCornerShape(99.dp)).background(Color.White))
        Box(Modifier.width(2.5.dp).height(h3.dp).clip(RoundedCornerShape(99.dp)).background(Color.White))
    }
}

@Composable
private fun HarmoniaNebulaBackdrop(slideIndex: Int, artworkUrl: String?) {
    val drift by animateFloatAsState(
        targetValue = (slideIndex % 6) / 6f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "harmoniaBackdropDrift",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050812), VantafynColors.Graphite, Color(0xFF060A16)))),
    )
    if (!artworkUrl.isNullOrBlank()) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(45.dp)
                .alpha(0.25f),
            contentScale = ContentScale.Crop,
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(VantafynGradients.AccentColors.first().copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(220f + drift * 260f, 130f + drift * 120f),
                    radius = 520f,
                ),
            ),
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(VantafynGradients.AccentColors.last().copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(920f - drift * 280f, 620f),
                    radius = 620f,
                ),
            ),
    )
}

@Composable
private fun HarmoniaStorySlideContent(
    slide: HarmoniaStorySlide,
    isHolding: Boolean,
    onToggleSave: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffset = remember { Animatable(18f) }
    val visualScale = remember { Animatable(0.85f) }

    LaunchedEffect(slide) {
        entranceAlpha.snapTo(0f)
        entranceOffset.snapTo(18f)
        visualScale.snapTo(0.85f)

        launch {
            entranceAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            entranceOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
        launch {
            visualScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entranceAlpha.value
                translationY = entranceOffset.value
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val isFinal = slide.visual is HarmoniaStoryVisual.FinalSummary
        if (isFinal) {
            Text(
                text = slide.title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
        } else {
            // Typography header group - strictly centered
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = slide.kicker.uppercase(Locale.getDefault()),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                }
                Text(
                    text = slide.title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                slide.metric?.let {
                    Text(
                        text = it,
                        color = Color(0xFF00F2FE),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
                slide.body?.let {
                    Text(
                        text = it,
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Visual presentation with spring scale
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(visualScale.value),
            contentAlignment = Alignment.Center,
        ) {
            when (val visual = slide.visual) {
                is HarmoniaStoryVisual.Intro -> HarmoniaIntroVisual(visual.totalMs, visual.trackCount)
                is HarmoniaStoryVisual.Scale -> HarmoniaScaleVisual(visual.tracksPlayed, visual.uniqueArtists, visual.averageSessionMs)
                is HarmoniaStoryVisual.HeroTrack -> HarmoniaHeroTrackVisual(visual.item, isPlaying = !isHolding)
                is HarmoniaStoryVisual.HeroArtist -> HarmoniaHeroArtistVisual(visual.item, isBreathing = !isHolding)
                is HarmoniaStoryVisual.RankedTracks -> HarmoniaRankedTracksVisual(visual.items)
                is HarmoniaStoryVisual.RankedArtists -> HarmoniaRankedArtistsVisual(visual.items)
                is HarmoniaStoryVisual.Albums -> HarmoniaAlbumsVisual(visual.items)
                is HarmoniaStoryVisual.Genres -> HarmoniaGenreVisual(visual.items)
                is HarmoniaStoryVisual.Persona -> HarmoniaPersonaVisual(visual.persona)
                is HarmoniaStoryVisual.Heatmap -> HarmoniaHeatmapVisual(visual.values, visual.streakDays, visual.peakDate, visual.peakTimeMs)
                is HarmoniaStoryVisual.Hourly -> HarmoniaHourlyVisual(visual.values, visual.peakHour)
                is HarmoniaStoryVisual.Monthly -> HarmoniaMonthlyVisual(visual.values)
                is HarmoniaStoryVisual.FinalSummary -> HarmoniaFinalSummaryVisual(
                    summary = visual,
                    onToggleSave = onToggleSave,
                    onReplay = onReplay,
                    onBack = onBack,
                )
                HarmoniaStoryVisual.None -> Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun HarmoniaIntroVisual(totalMs: Long, trackCount: Int) {
    val totalMinutes = (totalMs / 60_000L).coerceAtLeast(0L)
    val animatedMinutes = remember { Animatable(0f) }

    LaunchedEffect(totalMinutes) {
        animatedMinutes.snapTo(0f)
        animatedMinutes.animateTo(
            targetValue = totalMinutes.toFloat(),
            animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "introPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "introPulseScale",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .scale(pulseScale),
            contentAlignment = Alignment.Center,
        ) {
            // Luminous backdrop aura rings
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF00F2FE).copy(alpha = 0.35f),
                                Color(0xFF4FACFE).copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            // Grooved sound capsule disc
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D111A))
                    .border(2.dp, VantafynGradients.accentHorizontal(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(VantafynGradients.accentHorizontal()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${animatedMinutes.value.toLong().coerceAtLeast(0L)} MINUTES",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
            Text(
                text = "of pure music discovery",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (trackCount > 0) {
            HarmoniaPill(
                icon = Icons.Rounded.MusicNote,
                text = "$trackCount tracks played",
                tint = Color(0xFF00F2FE),
            )
        }
    }
}

@Composable
private fun HarmoniaScaleVisual(
    tracksPlayed: Int,
    uniqueArtists: Int,
    averageSessionMs: Long,
) {
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alphaAnim.value),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VantafynGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VantafynGradients.accentHorizontal()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("$tracksPlayed", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Total tracks played", color = VantafynColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        VantafynGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF0844), Color(0xFFFFB199)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("$uniqueArtists", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Unique artists explored", color = VantafynColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (averageSessionMs > 0L) {
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFF77737), Color(0xFFFCCC63)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(averageSessionMs.formatListeningTime(), color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Average listening session", color = VantafynColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun HarmoniaHeroTrackVisual(item: HarmoniaRankedItem, isPlaying: Boolean) {
    val vinylSlide = remember { Animatable(0f) }
    LaunchedEffect(item.id) {
        vinylSlide.snapTo(0f)
        vinylSlide.animateTo(
            targetValue = 40f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinAngle",
    )
    val activeRotation = if (isPlaying) spinRotation else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Vinyl disc sliding out from behind sleeve and rotating
            Box(
                modifier = Modifier
                    .offset(x = vinylSlide.value.dp)
                    .size(175.dp)
                    .graphicsLayer { rotationZ = activeRotation }
                    .clip(CircleShape)
                    .background(Color(0xFF0A0D14))
                    .border(1.5.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Vinyl grooves
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                )
                // Center label
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(VantafynGradients.accentHorizontal()),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A0D14)),
                    )
                }
            }

            // Album jacket cover sleeve
            Box(
                modifier = Modifier
                    .offset(x = (-16).dp)
                    .size(175.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(VantafynColors.Surface)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp)),
            ) {
                if (!item.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.artworkUrl,
                        contentDescription = item.label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VantafynGradients.accentHorizontal()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.label,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle?.let {
                Text(
                    text = it,
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HarmoniaPill(
                icon = Icons.Rounded.PlayArrow,
                text = "${item.playCount} plays",
                tint = Color(0xFF00F2FE),
            )
            HarmoniaPill(
                icon = Icons.Rounded.Schedule,
                text = item.listeningTimeMs.formatListeningTime(),
            )
        }
    }
}

@Composable
private fun HarmoniaHeroArtistVisual(item: HarmoniaRankedItem, isBreathing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "artistBreathing")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "artistAuraScale",
    )
    val activeAuraScale = if (isBreathing) auraScale else 1.0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Outer breathing aura ring
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(activeAuraScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                VantafynGradients.AccentColors.first().copy(alpha = 0.45f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            // Artist circular portrait
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .border(3.dp, VantafynGradients.accentHorizontal(), CircleShape)
                    .background(VantafynColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.artworkUrl,
                        contentDescription = item.label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.label,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Your soundtrack wouldn't be the same without them",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HarmoniaPill(
                icon = Icons.Rounded.Star,
                text = "Top Artist",
                tint = Color(0xFFFFD166),
            )
            HarmoniaPill(
                icon = Icons.Rounded.PlayArrow,
                text = "${item.playCount} plays",
            )
            HarmoniaPill(
                icon = Icons.Rounded.Schedule,
                text = item.listeningTimeMs.formatListeningTime(),
            )
        }
    }
}

@Composable
private fun HarmoniaRankedTracksVisual(items: List<HarmoniaRankedItem>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.take(5).forEachIndexed { index, item ->
            val rowAlpha = remember { Animatable(0f) }
            val rowOffset = remember { Animatable(16f) }

            LaunchedEffect(index) {
                delay(index * 70L)
                launch { rowAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                launch { rowOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) }
            }

            VantafynGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = rowAlpha.value
                        translationY = rowOffset.value
                    },
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val badgeGradient = when (index) {
                        0 -> listOf(Color(0xFFFFD166), Color(0xFFFF9F1C))
                        1 -> listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                        2 -> listOf(Color(0xFFF77737), Color(0xFFC05621))
                        else -> listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f))
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(badgeGradient)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "#${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VantafynColors.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!item.artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.artworkUrl,
                                contentDescription = item.label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            color = VantafynColors.Ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = listOfNotNull(item.subtitle, "${item.playCount} plays", item.listeningTimeMs.formatListeningTime()).joinToString(" • "),
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HarmoniaRankedArtistsVisual(items: List<HarmoniaRankedItem>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.take(5).forEachIndexed { index, item ->
            val rowAlpha = remember { Animatable(0f) }
            val rowOffset = remember { Animatable(16f) }

            LaunchedEffect(index) {
                delay(index * 70L)
                launch { rowAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                launch { rowOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) }
            }

            VantafynGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = rowAlpha.value
                        translationY = rowOffset.value
                    },
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val badgeGradient = when (index) {
                        0 -> listOf(Color(0xFFFFD166), Color(0xFFFF9F1C))
                        1 -> listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                        2 -> listOf(Color(0xFFF77737), Color(0xFFC05621))
                        else -> listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f))
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(badgeGradient)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "#${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(VantafynColors.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!item.artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.artworkUrl,
                                contentDescription = item.label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            color = VantafynColors.Ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${item.playCount} plays • ${item.listeningTimeMs.formatListeningTime()}",
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HarmoniaAlbumsVisual(items: List<HarmoniaRankedItem>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.take(4).forEachIndexed { index, item ->
            val rowAlpha = remember { Animatable(0f) }
            val rowOffset = remember { Animatable(16f) }

            LaunchedEffect(index) {
                delay(index * 70L)
                launch { rowAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                launch { rowOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) }
            }

            VantafynGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = rowAlpha.value
                        translationY = rowOffset.value
                    },
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VantafynColors.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!item.artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.artworkUrl,
                                contentDescription = item.label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            color = VantafynColors.Ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(item.subtitle, "${item.playCount} plays", item.listeningTimeMs.formatListeningTime()).joinToString(" • "),
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HarmoniaGenreVisual(items: List<HarmoniaRankedItem>) {
    val total = items.sumOf { it.listeningTimeMs }.coerceAtLeast(1L)
    val paletteGradients = listOf(
        listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)),
        listOf(Color(0xFFFF0844), Color(0xFFFFB199)),
        listOf(Color(0xFFF77737), Color(0xFFFCCC63)),
        listOf(Color(0xFF9B51E0), Color(0xFFE056FD)),
        listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        items.take(5).forEachIndexed { index, item ->
            val percentage = ((item.listeningTimeMs * 100) / total).toInt()
            val gradient = paletteGradients[index % paletteGradients.size]
            val targetRatio = (item.listeningTimeMs.toFloat() / total).coerceIn(0.05f, 1f)
            val animatedWidth = remember { Animatable(0f) }

            LaunchedEffect(item.id) {
                delay(index * 70L)
                animatedWidth.animateTo(
                    targetValue = targetRatio,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(gradient)),
                        )
                        Text(
                            item.label,
                            color = VantafynColors.Ink,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "$percentage% • ${item.listeningTimeMs.formatListeningTime()}",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(animatedWidth.value)
                            .height(9.dp)
                            .background(Brush.horizontalGradient(gradient)),
                    )
                }
            }
        }
    }
}

@Composable
private fun HarmoniaPersonaVisual(persona: HarmoniaPersona) {
    val icon = when {
        persona.title.contains("Devotee", ignoreCase = true) -> Icons.Rounded.Favorite
        persona.title.contains("Night Owl", ignoreCase = true) -> Icons.Rounded.NightlightRound
        persona.title.contains("Morning", ignoreCase = true) -> Icons.Rounded.WbSunny
        persona.title.contains("Chameleon", ignoreCase = true) -> Icons.Rounded.Explore
        persona.title.contains("Adventurer", ignoreCase = true) || persona.title.contains("Explorer", ignoreCase = true) -> Icons.Rounded.AutoAwesome
        else -> Icons.Rounded.Headphones
    }

    val infiniteTransition = rememberInfiniteTransition(label = "personaGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "personaGlowScale",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(glowScale),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF00F2FE).copy(alpha = 0.35f),
                                Color(0xFFFF007A).copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0C101A))
                    .border(2.5.dp, VantafynGradients.accentHorizontal(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFFD166),
                    modifier = Modifier.size(54.dp),
                )
            }
        }

        HarmoniaPill(
            icon = Icons.Rounded.Star,
            text = "TRAIT • ${persona.dominantTrait.uppercase(Locale.getDefault())}",
            tint = Color(0xFFFFD166),
        )

        VantafynGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(16.dp),
        ) {
            Text(
                text = "\"${persona.description}\"",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HarmoniaHeatmapVisual(
    values: Map<LocalDate, Long>,
    streakDays: Int,
    peakDate: LocalDate?,
    peakTimeMs: Long,
) {
    val maxValue = values.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val days = values.keys.sorted()
    val columns = 16
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (streakDays > 1) {
                    HarmoniaPill(icon = Icons.Rounded.LocalFireDepartment, text = "$streakDays Day Streak", tint = Color(0xFFFF6B6B))
                }
                if (peakDate != null) {
                    val context = LocalContext.current
                    HarmoniaPill(icon = Icons.Rounded.AutoAwesome, text = "Peak: ${peakDate.formatLocalizedDate(context)}", tint = Color(0xFFFFD166))
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(145.dp)) {
            val gap = 4.dp.toPx()
            val cell = ((size.width - gap * (columns - 1)) / columns).coerceAtMost(14.dp.toPx())
            days.takeLast(columns * 7).forEachIndexed { index, day ->
                val x = (index / 7) * (cell + gap)
                val y = (index % 7) * (cell + gap)
                val ratio = (values[day].orZero().toFloat() / maxValue)
                val cellColor = if (ratio <= 0.001f) {
                    Color.White.copy(alpha = 0.06f)
                } else {
                    val color1 = Color(0xFF00F2FE)
                    val color2 = Color(0xFFFF007A)
                    androidx.compose.ui.graphics.lerp(color1, color2, ratio.coerceIn(0f, 1f)).copy(alpha = 0.35f + ratio * 0.65f)
                }
                drawRoundRect(
                    color = cellColor,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun HarmoniaHourlyVisual(
    values: List<Pair<Int, Long>>,
    peakHour: Int?,
) {
    val maxValue = values.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    val personality = when (peakHour) {
        in 22..23, in 0..4 -> "Night Owl" to "Most active late at night"
        in 5..10 -> "Early Bird" to "Mornings started with music"
        in 11..16 -> "Daylight Groove" to "Soundtrack to your day"
        else -> "Evening Unwind" to "Easing into the evening"
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            HarmoniaPill(
                icon = if (peakHour in 5..16) Icons.Rounded.WbSunny else Icons.Rounded.NightlightRound,
                text = "${personality.first} • ${personality.second}",
                tint = Color(0xFF00F2FE),
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(135.dp)) {
            val barWidth = size.width / 24f
            values.forEach { (hour, value) ->
                val height = (size.height * (value.toFloat() / maxValue)).coerceAtLeast(4.dp.toPx())
                val isPeak = hour == peakHour
                val color = if (isPeak) {
                    Color(0xFF00F2FE)
                } else {
                    Color.White.copy(alpha = 0.15f + 0.55f * (value.toFloat() / maxValue))
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(hour * barWidth + 2.dp.toPx(), size.height - height),
                    size = Size(barWidth - 4.dp.toPx(), height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(999f, 999f),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("12 AM", color = VantafynColors.Muted, style = MaterialTheme.typography.labelSmall)
            Text("6 AM", color = VantafynColors.Muted, style = MaterialTheme.typography.labelSmall)
            Text("12 PM", color = VantafynColors.Muted, style = MaterialTheme.typography.labelSmall)
            Text("6 PM", color = VantafynColors.Muted, style = MaterialTheme.typography.labelSmall)
            Text("11 PM", color = VantafynColors.Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun HarmoniaMonthlyVisual(values: List<Pair<String, Long>>) {
    val maxValue = values.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        values.forEach { (month, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(month, color = VantafynColors.Muted, modifier = Modifier.width(34.dp), style = MaterialTheme.typography.bodySmall)
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.09f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((value.toFloat() / maxValue).coerceIn(0.04f, 1f))
                            .height(8.dp)
                            .background(VantafynGradients.accentHorizontal()),
                    )
                }
            }
        }
    }
}

@Composable
private fun HarmoniaFinalSummaryVisual(
    summary: HarmoniaStoryVisual.FinalSummary,
    onToggleSave: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val totalMinutes = (summary.totalTimeMs / 60_000L).coerceAtLeast(0L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Flagship Holographic Recap Poster Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0F172A).copy(alpha = 0.94f),
                            Color(0xFF1E1B4B).copy(alpha = 0.90f),
                            Color(0xFF0B1120).copy(alpha = 0.96f),
                        ),
                        start = Offset.Zero,
                        end = Offset(600f, 900f),
                    ),
                )
                .border(
                    1.2.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF00F2FE).copy(alpha = 0.60f),
                            Color(0xFF9B5CFF).copy(alpha = 0.50f),
                            Color(0xFFFF2A85).copy(alpha = 0.55f),
                            Color(0xFFFFD166).copy(alpha = 0.50f),
                        ),
                    ),
                    RoundedCornerShape(26.dp),
                ),
        ) {
            // Ambient inner glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00F2FE).copy(alpha = 0.14f),
                                Color(0xFF7928CA).copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                            center = Offset(120f, 80f),
                            radius = 450f,
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Top Header Row: Branding + Total Time Glow Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFFD166), Color(0xFFFF6584)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            "VANTAFYN HARMONIA",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                        )
                    }

                    // Stat Badge: Total Time
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00F2FE).copy(alpha = 0.22f),
                                        Color(0xFF4FACFE).copy(alpha = 0.15f),
                                    ),
                                ),
                            )
                            .border(
                                0.8.dp,
                                Color(0xFF00F2FE).copy(alpha = 0.40f),
                                RoundedCornerShape(999.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = Color(0xFF00F2FE),
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                "$totalMinutes MINS",
                                color = Color(0xFF00F2FE),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }

                // Persona Banner Centerpiece
                summary.persona?.let { persona ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF6366F1).copy(alpha = 0.30f),
                                        Color(0xFFA855F7).copy(alpha = 0.26f),
                                        Color(0xFFEC4899).copy(alpha = 0.20f),
                                    ),
                                ),
                            )
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF818CF8).copy(alpha = 0.50f),
                                        Color(0xFFC084FC).copy(alpha = 0.40f),
                                    ),
                                ),
                                RoundedCornerShape(16.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFFFFD166).copy(alpha = 0.35f),
                                                Color(0xFFFF9F43).copy(alpha = 0.15f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    )
                                    .border(1.dp, Color(0xFFFFD166).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD166),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "MUSICAL PERSONA",
                                    color = Color(0xFFC084FC),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                )
                                Text(
                                    persona.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    persona.subtitle,
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // Top Media Spotlight Duo (Track & Artist)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Top Track Card
                    summary.topTrack?.let { track ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .padding(10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // Album Art Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!track.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = track.artworkUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Icon(
                                                Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = Color(0xFF00F2FE),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "TOP TRACK",
                                            color = Color(0xFFFFD166),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                        )
                                        Text(
                                            track.label,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        track.subtitle ?: "Top track",
                                        color = Color.White.copy(alpha = 0.65f),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Text(
                                        "${track.playCount} plays",
                                        color = Color(0xFF00F2FE),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    // Top Artist Card
                    summary.topArtist?.let { artist ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .padding(10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // Artist Portrait Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.45f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!artist.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = artist.artworkUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Icon(
                                                Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = Color(0xFFC084FC),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "TOP ARTIST",
                                            color = Color(0xFFC084FC),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                        )
                                        Text(
                                            artist.label,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Most played",
                                        color = Color.White.copy(alpha = 0.65f),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    Text(
                                        "${artist.playCount} plays",
                                        color = Color(0xFFFF6584),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Metadata Chips Row (Genre & Total Tracks)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    summary.topGenre?.let { genre ->
                        HarmoniaPill(
                            icon = Icons.Rounded.MusicNote,
                            text = genre.label,
                            tint = Color(0xFF00F2FE),
                        )
                    }
                    HarmoniaPill(
                        icon = Icons.Rounded.Headphones,
                        text = "${summary.totalTracks} tracks played",
                        tint = Color(0xFFFFD166),
                    )
                }
            }
        }

        // Action Buttons Row (Balanced, tactile and elevated)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Save Recap (Primary Action)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (summary.isSaved) {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFD166).copy(alpha = 0.25f),
                                    Color(0xFFFF9F43).copy(alpha = 0.20f),
                                ),
                            )
                        } else {
                            VantafynGradients.accentHorizontal()
                        },
                    )
                    .border(
                        1.dp,
                        if (summary.isSaved) Color(0xFFFFD166).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.20f),
                        RoundedCornerShape(16.dp),
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleSave()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (summary.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        tint = if (summary.isSaved) Color(0xFFFFD166) else Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (summary.isSaved) "Saved in Library" else "Save Recap",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Replay Button (Secondary Action)
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReplay()
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.Replay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Replay",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HarmoniaPill(
    icon: ImageVector,
    text: String,
    tint: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            Text(text, color = VantafynColors.Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class HarmoniaStorySlide(
    val kicker: String,
    val title: String,
    val metric: String? = null,
    val body: String? = null,
    val visual: HarmoniaStoryVisual = HarmoniaStoryVisual.None,
    val artworkUrl: String? = null,
)

private sealed interface HarmoniaStoryVisual {
    data object None : HarmoniaStoryVisual
    data class Intro(val totalMs: Long, val trackCount: Int) : HarmoniaStoryVisual
    data class Scale(val tracksPlayed: Int, val uniqueArtists: Int, val averageSessionMs: Long) : HarmoniaStoryVisual
    data class HeroTrack(val item: HarmoniaRankedItem) : HarmoniaStoryVisual
    data class HeroArtist(val item: HarmoniaRankedItem) : HarmoniaStoryVisual
    data class RankedTracks(val items: List<HarmoniaRankedItem>) : HarmoniaStoryVisual
    data class RankedArtists(val items: List<HarmoniaRankedItem>) : HarmoniaStoryVisual
    data class Albums(val items: List<HarmoniaRankedItem>) : HarmoniaStoryVisual
    data class Genres(val items: List<HarmoniaRankedItem>) : HarmoniaStoryVisual
    data class Persona(val persona: HarmoniaPersona) : HarmoniaStoryVisual
    data class Heatmap(
        val values: Map<LocalDate, Long>,
        val streakDays: Int = 0,
        val peakDate: LocalDate? = null,
        val peakTimeMs: Long = 0L,
    ) : HarmoniaStoryVisual
    data class Hourly(val values: List<Pair<Int, Long>>, val peakHour: Int? = null) : HarmoniaStoryVisual
    data class Monthly(val values: List<Pair<String, Long>>) : HarmoniaStoryVisual
    data class FinalSummary(
        val totalTimeMs: Long,
        val totalTracks: Int,
        val topTrack: HarmoniaRankedItem?,
        val topArtist: HarmoniaRankedItem?,
        val topAlbum: HarmoniaRankedItem?,
        val topGenre: HarmoniaRankedItem?,
        val persona: HarmoniaPersona? = null,
        val isSaved: Boolean,
    ) : HarmoniaStoryVisual
}

private fun HarmoniaRecap.harmoniaStorySlides(context: Context, isSaved: Boolean): List<HarmoniaStorySlide> {
    val yearly = periodType == HarmoniaPeriod.YEARLY
    val label = HarmoniaRecapPreview(
        id = id,
        userId = userId,
        periodType = periodType,
        periodStart = periodStart,
        periodEnd = periodEnd,
        generatedAt = generatedAt,
        dataVersion = dataVersion,
        totalListeningTimeMs = statistics.totalListeningTimeMs.value ?: 0L,
        totalTracksPlayed = statistics.totalTracksPlayed.value ?: 0,
        topArtist = statistics.topArtists.value?.firstOrNull()?.label,
        topTrack = statistics.topTracks.value?.firstOrNull()?.label,
        artworkUrl = statistics.topTracks.value?.firstOrNull()?.artworkUrl ?: statistics.topAlbums.value?.firstOrNull()?.artworkUrl,
    ).periodLabel()
    val stats = statistics
    val topTrackItem = stats.topTracks.value?.firstOrNull()
    val topArtistItem = stats.topArtists.value?.firstOrNull()
    val topAlbumItem = stats.topAlbums.value?.firstOrNull()
    val topGenreItem = stats.topGenres.value?.firstOrNull()

    return buildList {
        // 1. Intro - Sound Capsule with animated minutes count-up
        add(
            HarmoniaStorySlide(
                kicker = "Harmonia • $label",
                title = if (yearly) "Your Year in Music" else "Your Month in Music",
                metric = stats.totalListeningTimeMs.value?.formatListeningTime(),
                body = "Every late night, morning commute, and quiet afternoon — here is your sonic journey.",
                artworkUrl = topTrackItem?.artworkUrl ?: topAlbumItem?.artworkUrl,
                visual = HarmoniaStoryVisual.Intro(
                    totalMs = stats.totalListeningTimeMs.value ?: 0L,
                    trackCount = stats.totalTracksPlayed.value ?: 0,
                ),
            ),
        )

        // 2. Listening Horizon & Scale
        add(
            HarmoniaStorySlide(
                kicker = "Listening Scale",
                title = "You explored ${stats.totalTracksPlayed.value ?: 0} tracks",
                metric = stats.uniqueArtists.value?.let { "$it artists explored" },
                body = stats.averageListeningSessionMs.value?.let { "Average session: ${it.formatListeningTime()}" },
                artworkUrl = topAlbumItem?.artworkUrl,
                visual = HarmoniaStoryVisual.Scale(
                    tracksPlayed = stats.totalTracksPlayed.value ?: 0,
                    uniqueArtists = stats.uniqueArtists.value ?: 0,
                    averageSessionMs = stats.averageListeningSessionMs.value ?: 0L,
                ),
            ),
        )

        // 3. Top Artist Spotlight
        topArtistItem?.let { artist ->
            add(
                HarmoniaStorySlide(
                    kicker = "Top Artist",
                    title = artist.label,
                    metric = artist.listeningTimeMs.formatListeningTime(),
                    body = "${artist.playCount} plays • You spent the most time with them",
                    artworkUrl = artist.artworkUrl,
                    visual = HarmoniaStoryVisual.HeroArtist(artist),
                ),
            )
        }

        // 4. Top 5 Artists
        stats.topArtists.value?.takeIf { it.size > 1 }?.let { artists ->
            add(
                HarmoniaStorySlide(
                    kicker = "Top Artists",
                    title = "The voices that defined your sound",
                    artworkUrl = artists.firstOrNull()?.artworkUrl,
                    visual = HarmoniaStoryVisual.RankedArtists(artists),
                ),
            )
        }

        // 5. Top Track Spotlight
        topTrackItem?.let { track ->
            add(
                HarmoniaStorySlide(
                    kicker = "Top Track",
                    title = track.label,
                    metric = track.listeningTimeMs.formatListeningTime(),
                    body = listOfNotNull(track.subtitle, "${track.playCount} plays").joinToString(" • "),
                    artworkUrl = track.artworkUrl,
                    visual = HarmoniaStoryVisual.HeroTrack(track),
                ),
            )
        }

        // 6. Top 5 Tracks
        stats.topTracks.value?.takeIf { it.size > 1 }?.let { tracks ->
            add(
                HarmoniaStorySlide(
                    kicker = "Top Tracks",
                    title = "Heavy Rotation",
                    artworkUrl = tracks.firstOrNull()?.artworkUrl,
                    visual = HarmoniaStoryVisual.RankedTracks(tracks),
                ),
            )
        }

        // 7. Top Albums
        stats.topAlbums.value?.takeIf { it.isNotEmpty() }?.let { albums ->
            add(
                HarmoniaStorySlide(
                    kicker = "Top Albums",
                    title = "The records you returned to",
                    artworkUrl = albums.firstOrNull()?.artworkUrl,
                    visual = HarmoniaStoryVisual.Albums(albums),
                ),
            )
        }

        // 8. Sound Palette / Genres
        stats.topGenres.value?.takeIf { it.isNotEmpty() }?.let { genres ->
            add(
                HarmoniaStorySlide(
                    kicker = "Top Genres",
                    title = "Your Sound Palette",
                    body = genres.firstOrNull()?.let { "Your sound leaned towards ${it.label}." },
                    visual = HarmoniaStoryVisual.Genres(genres),
                ),
            )
        }

        // 9. Listening Persona / Archetype
        stats.persona.value?.let { persona ->
            add(
                HarmoniaStorySlide(
                    kicker = "Listening Persona",
                    title = persona.title,
                    metric = persona.subtitle,
                    body = persona.description,
                    visual = HarmoniaStoryVisual.Persona(persona),
                ),
            )
        }

        // 10. Activity & Heatmap
        stats.dailyHeatmap.value?.takeIf { it.isNotEmpty() }?.let { heatmap ->
            add(
                HarmoniaStorySlide(
                    kicker = "Activity",
                    title = if (yearly) "Your listening year, day by day" else "Your listening month, day by day",
                    metric = stats.listeningStreakDays.value?.takeIf { it > 1 }?.let { "🔥 $it Day Streak" },
                    body = stats.longestListeningDay.value?.let { "Peak: ${it.date.formatLocalizedDate(context)} (${it.listeningTimeMs.formatListeningTime()})" },
                    visual = HarmoniaStoryVisual.Heatmap(
                        values = heatmap,
                        streakDays = stats.listeningStreakDays.value ?: 0,
                        peakDate = stats.longestListeningDay.value?.date,
                        peakTimeMs = stats.longestListeningDay.value?.listeningTimeMs ?: 0L,
                    ),
                ),
            )
        }

        // 11. Daily Rhythm
        stats.listeningByHour.value?.takeIf { it.any { hour -> hour.listeningTimeMs > 0L } }?.let { hours ->
            val activeHour = stats.mostActiveHour.value?.hour?.formatHour()
            add(
                HarmoniaStorySlide(
                    kicker = "Time Patterns",
                    title = "Your listening rhythm",
                    metric = activeHour?.let { "Peak hour: $it" },
                    body = stats.mostActiveDay.value?.date?.let { "Most active day: ${it.formatLocalizedDate(context)}" },
                    visual = HarmoniaStoryVisual.Hourly(
                        values = hours.map { it.hour to it.listeningTimeMs },
                        peakHour = stats.mostActiveHour.value?.hour,
                    ),
                ),
            )
        }

        // 12. Monthly Journey (if yearly)
        stats.listeningByMonth.value?.takeIf { yearly && it.isNotEmpty() }?.let { months ->
            add(
                HarmoniaStorySlide(
                    kicker = "Monthly Journey",
                    title = "The shape of your year",
                    visual = HarmoniaStoryVisual.Monthly(
                        months.map {
                            it.month.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()) to it.listeningTimeMs
                        },
                    ),
                ),
            )
        }

        // 13. Month-over-month comparison
        stats.comparisonListeningDeltaMs.value?.let { delta ->
            if (!yearly) {
                add(
                    HarmoniaStorySlide(
                        kicker = "Compared with last month",
                        title = if (delta >= 0) "You listened more" else "A quieter month",
                        metric = kotlin.math.abs(delta).formatListeningTime(),
                    ),
                )
            }
        }

        // 14. Replay behavior (if distinct)
        stats.mostReplayedTrack.value?.let { replayed ->
            add(
                HarmoniaStorySlide(
                    kicker = "Replay Behaviour",
                    title = "You kept coming back to ${replayed.label}",
                    metric = "${replayed.playCount} plays",
                    body = replayed.listeningTimeMs.formatListeningTime(),
                    artworkUrl = replayed.artworkUrl,
                ),
            )
        }

        // 15. Final Summary Poster
        add(
            HarmoniaStorySlide(
                kicker = "Harmonia Recap",
                title = if (yearly) "This was your year in music." else "This was your month in music.",
                visual = HarmoniaStoryVisual.FinalSummary(
                    totalTimeMs = stats.totalListeningTimeMs.value ?: 0L,
                    totalTracks = stats.totalTracksPlayed.value ?: 0,
                    topTrack = topTrackItem,
                    topArtist = topArtistItem,
                    topAlbum = topAlbumItem,
                    topGenre = topGenreItem,
                    persona = stats.persona.value,
                    isSaved = isSaved,
                ),
            ),
        )
    }
}

private fun LocalDate.formatLocalizedDate(context: Context? = null): String {
    if (context != null) {
        try {
            val calendar = java.util.Calendar.getInstance().apply {
                set(year, monthValue - 1, dayOfMonth)
            }
            return android.text.format.DateFormat.getMediumDateFormat(context).format(calendar.time)
        } catch (_: Exception) {}
    }
    return try {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(this)
    } catch (_: Exception) {
        "${dayOfMonth} ${month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())} $year"
    }
}

private fun HarmoniaRecapPreview.periodLabel(): String {
    val date = periodStart.atZone(ZoneId.systemDefault()).toLocalDate()
    return when (periodType) {
        HarmoniaPeriod.MONTHLY -> "${date.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} ${date.year}"
        HarmoniaPeriod.YEARLY -> date.year.toString()
    }
}

private fun HarmoniaRecapPreview.harmoniaTease(): String =
    listOfNotNull(
        topArtist?.let { "Top artist: $it" },
        topTrack?.let { "Top track: $it" },
    ).ifEmpty {
        listOf("$totalTracksPlayed tracks remembered")
    }.joinToString("  •  ")

private fun Long.formatListeningTime(): String {
    val totalMinutes = (this / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m listened"
        hours > 0L -> "${hours}h listened"
        else -> "${minutes}m listened"
    }
}

private fun Long?.orZero(): Long = this ?: 0L

private fun Int.formatHour(): String {
    val hour = Math.floorMod(this, 24)
    val suffix = if (hour < 12) "AM" else "PM"
    val display = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    return "$display $suffix"
}

@Composable
private fun MusicLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
        MusicSkeletonTrackList()
        MusicSkeletonArtworkRow(labelWidth = 116.dp)
        MusicSkeletonArtworkRow(labelWidth = 82.dp)
        MusicSkeletonArtworkRow(labelWidth = 94.dp)
    }
}

@Composable
private fun MusicSkeletonTrackList() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(musicSkeletonBrush()),
        )
        repeat(4) { index ->
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(musicSkeletonBrush()),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (index % 2 == 0) 0.82f else 0.64f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(musicSkeletonBrush()),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (index % 2 == 0) 0.48f else 0.56f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(musicSkeletonBrush()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicSkeletonArtworkRow(labelWidth: androidx.compose.ui.unit.Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Box(
            modifier = Modifier
                .width(labelWidth)
                .height(18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(musicSkeletonBrush()),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(4) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(128.dp)) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(musicSkeletonBrush()),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(musicSkeletonBrush()),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.58f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(musicSkeletonBrush()),
                    )
                }
            }
        }
    }
}

@Composable
private fun musicSkeletonBrush(): Brush {
    return VantafynSkeletonBrush()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicQuickPicksRow(
    tracks: List<JellyfinMusicTrack>,
    pendingTrackId: java.util.UUID?,
    onStartRadio: (JellyfinMusicTrack) -> Unit,
    onLongPress: ((JellyfinMusicTrack) -> Unit)? = null,
) {
    if (tracks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = null,
                    tint = VantafynColors.Ink,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Quick Picks",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Infinite Station Radio",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(tracks, key = { index, track -> "qp-${track.id}-$index" }) { _, track ->
                MusicQuickPickCard(
                    track = track,
                    isLoading = pendingTrackId == track.id,
                    onClick = { onStartRadio(track) },
                    onLongClick = { onLongPress?.invoke(track) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicQuickPickCard(
    track: JellyfinMusicTrack,
    isLoading: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .width(144.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            MusicArt(track.artworkUrl, Modifier.size(144.dp), cornerRadius = 20, title = track.title, subtitle = track.artist)
            // Gradient vignette at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.75f),
                        ),
                    ),
            )
            // Floating radio station badge at top-end
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = null,
                    tint = VantafynColors.Ink,
                    modifier = Modifier.size(15.dp),
                )
            }
            // Station pill at bottom-start
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(VantafynColors.SurfaceHigh.copy(alpha = 0.85f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = VantafynColors.Ink,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "Radio",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = VantafynColors.Ink,
                    ),
                )
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.40f)),
                    contentAlignment = Alignment.Center,
                ) {
                    VantafynGradientLoadingRing(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = track.title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(rememberLifecycleAwareMarquee()),
            )
            Text(
                text = "${track.artist} Radio",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(rememberLifecycleAwareMarquee()),
            )
        }
    }
}

@Composable
private fun MusicOnRepeatRow(
    tracks: List<JellyfinMusicTrack>,
    pendingTrackId: java.util.UUID?,
    onTrack: (JellyfinMusicTrack) -> Unit,
    onLongPress: ((JellyfinMusicTrack) -> Unit)? = null,
) {
    if (tracks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = VantafynColors.Ink,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "On Repeat",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Most Played",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(tracks, key = { index, track -> "repeat-${track.id}-$index" }) { _, track ->
                MusicArtworkTile(
                    imageUrl = track.artworkUrl,
                    title = track.title,
                    subtitle = track.artist,
                    badgeText = track.playCount?.takeIf { it > 0 }?.let { if (it == 1) "1 play" else "$it plays" } ?: "On Repeat",
                    isLoading = pendingTrackId == track.id,
                    onClick = { onTrack(track) },
                    onLongClick = { onLongPress?.invoke(track) },
                )
            }
        }
    }
}

@Composable
private fun MusicRediscoverRow(
    tracks: List<JellyfinMusicTrack>,
    pendingTrackId: java.util.UUID?,
    onTrack: (JellyfinMusicTrack) -> Unit,
    onLongPress: ((JellyfinMusicTrack) -> Unit)? = null,
) {
    if (tracks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Replay,
                    contentDescription = null,
                    tint = VantafynColors.Ink,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Forgotten Favorites",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Rediscover",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(tracks, key = { index, track -> "rediscover-${track.id}-$index" }) { _, track ->
                MusicArtworkTile(
                    imageUrl = track.artworkUrl,
                    title = track.title,
                    subtitle = track.artist,
                    badgeText = "Rediscover",
                    isLoading = pendingTrackId == track.id,
                    onClick = { onTrack(track) },
                    onLongClick = { onLongPress?.invoke(track) },
                )
            }
        }
    }
}

private data class MusicMoodTheme(
    val primaryColor: Color,
    val secondaryColor: Color,
    val icon: ImageVector,
)

private fun MusicHomeMood.theme(): MusicMoodTheme = when (this) {
    MusicHomeMood.All -> MusicMoodTheme(
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF00B0FF),
        icon = Icons.Rounded.MusicNote,
    )
    MusicHomeMood.Energize -> MusicMoodTheme(
        primaryColor = Color(0xFFFF9100),
        secondaryColor = Color(0xFFFF3D00),
        icon = Icons.Rounded.Bolt,
    )
    MusicHomeMood.Chill -> MusicMoodTheme(
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFF00BFA5),
        icon = Icons.Rounded.GraphicEq,
    )
    MusicHomeMood.OnRepeat -> MusicMoodTheme(
        primaryColor = Color(0xFFD500F9),
        secondaryColor = Color(0xFF7C3AED),
        icon = Icons.Rounded.Repeat,
    )
    MusicHomeMood.Favorites -> MusicMoodTheme(
        primaryColor = Color(0xFFFF1744),
        secondaryColor = Color(0xFFE11D48),
        icon = Icons.Rounded.Favorite,
    )
}

@Composable
private fun MusicHomeMoodChips(
    selectedMood: MusicHomeMood,
    onMoodSelected: (MusicHomeMood) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(MusicHomeMood.entries, key = { it.name }) { mood ->
            val isSelected = selectedMood == mood
            val theme = mood.theme()

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF0F131D).copy(alpha = 0.72f))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(
                                    theme.primaryColor.copy(alpha = 0.32f),
                                    theme.secondaryColor.copy(alpha = 0.18f),
                                ),
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    theme.primaryColor.copy(alpha = 0.10f),
                                    theme.primaryColor.copy(alpha = 0.04f),
                                ),
                            )
                        },
                    )
                    .then(
                        if (isSelected) {
                            Modifier.vantafynAnimatedModalBorder(
                                cornerRadius = 999.dp,
                                strokeWidth = 1.4.dp,
                                durationMillis = 4200,
                            )
                        } else {
                            Modifier.border(
                                1.dp,
                                theme.primaryColor.copy(alpha = 0.30f),
                                RoundedCornerShape(999.dp),
                            )
                        },
                    )
                    .clickable { onMoodSelected(mood) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = theme.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else theme.primaryColor.copy(alpha = 0.90f),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = mood.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else theme.primaryColor.copy(alpha = 0.95f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicMoodHeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onPlay: () -> Unit,
    onStartRadio: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    VantafynGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VantafynColors.Ink,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = VantafynColors.Muted,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onStartRadio != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .clickable(onClick = onStartRadio),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Radio,
                            contentDescription = "Start Radio",
                            tint = VantafynColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(VantafynGradients.accentHorizontal())
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicTrackRow(
    title: String,
    tracks: List<JellyfinMusicTrack>,
    pendingTrackId: java.util.UUID?,
    onTrack: (JellyfinMusicTrack) -> Unit,
    onLongPress: ((JellyfinMusicTrack) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(tracks, key = { index, track -> "${track.id}-$index" }) { _, track ->
                MusicArtworkTile(
                    imageUrl = track.artworkUrl,
                    title = track.title,
                    subtitle = track.artist,
                    isLoading = pendingTrackId == track.id,
                    onClick = { onTrack(track) },
                    onLongClick = { onLongPress?.invoke(track) },
                )
            }
        }
    }
}

@Composable
private fun MusicAlbumRow(
    albums: List<JellyfinMusicAlbum>,
    onAlbum: (JellyfinMusicAlbum) -> Unit,
    onLongPress: ((JellyfinMusicAlbum) -> Unit)? = null,
    title: String = "Albums",
) {
    if (albums.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(albums, key = { index, album -> "${album.id}-$index" }) { _, album ->
                MusicArtworkTile(
                    imageUrl = album.artworkUrl,
                    title = album.title,
                    subtitle = album.artist ?: "Album",
                    onClick = { onAlbum(album) },
                    onLongClick = { onLongPress?.invoke(album) },
                )
            }
        }
    }
}

@Composable
private fun MusicArtistRow(
    artists: List<JellyfinMusicArtist>,
    onArtist: (JellyfinMusicArtist) -> Unit,
    onLongPress: ((JellyfinMusicArtist) -> Unit)? = null,
    title: String = "Artists",
    icon: ImageVector? = null,
) {
    if (artists.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VantafynColors.Ink,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(artists, key = { index, artist -> "${artist.id}-$index" }) { _, artist ->
                MusicArtworkTile(
                    imageUrl = artist.imageUrl,
                    title = artist.name,
                    subtitle = "Artist",
                    onClick = { onArtist(artist) },
                    onLongClick = { onLongPress?.invoke(artist) },
                )
            }
        }
    }
}

@Composable
private fun MusicPlaylistRow(
    playlists: List<JellyfinMusicPlaylist>,
    onPlaylist: (JellyfinMusicPlaylist) -> Unit,
    title: String = "Playlists",
) {
    if (playlists.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(playlists, key = { index, playlist -> "${playlist.id}-$index" }) { _, playlist ->
                MusicArtworkTile(playlist.imageUrl, playlist.name, "${playlist.trackCount ?: 0} tracks", trackImageUrls = playlist.trackImageUrls) { onPlaylist(playlist) }
            }
        }
    }
}

private fun MusicSongsFilter.supportsMusicSongsAlphabetRail(): Boolean =
    this == MusicSongsFilter.AZ || this == MusicSongsFilter.Favorites

@Composable
private fun MusicSongsFilterChips(selected: MusicSongsFilter, onSelected: (MusicSongsFilter) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(MusicSongsFilter.entries, key = { it.name }) { mode ->
            VantafynGlassChip(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    mode.label,
                    color = if (selected == mode) VantafynColors.Ink else VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MusicSongsAlphabetRail(
    selected: String?,
    enabled: Boolean,
    onSelected: (String?) -> Unit,
) {
    val letters = remember { listOf("#") + ('A'..'Z').map { it.toString() } }
    val selectedIndex = selected?.let { letters.indexOf(it).takeIf { index -> index >= 0 } } ?: -1
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem((selectedIndex - 3).coerceAtLeast(0))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .drawWithContent {
                drawContent()
                val edge = 28.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to VantafynColors.Graphite,
                        1f to Color.Transparent,
                        startX = 0f,
                        endX = edge,
                    ),
                    size = Size(edge, size.height),
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        1f to VantafynColors.Graphite,
                        startX = size.width - edge,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - edge, 0f),
                    size = Size(edge, size.height),
                )
            },
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(letters, key = { it }) { letter ->
                MusicSongsAlphabetLetter(
                    label = letter,
                    selected = selected == letter,
                    enabled = enabled,
                    onClick = {
                        scope.launch {
                            val index = letters.indexOf(letter).takeIf { it >= 0 } ?: 0
                            listState.animateScrollToItem((index - 3).coerceAtLeast(0))
                        }
                        onSelected(if (selected == letter) null else letter)
                    },
                )
            }
        }
    }
}

@Composable
private fun MusicSongsAlphabetLetter(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.82f),
        label = "musicSongsAlphabetPress",
    )
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 32.dp)
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.17f),
                                    VantafynColors.Surface.copy(alpha = 0.72f),
                                    VantafynColors.Graphite.copy(alpha = 0.58f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = VantafynGradients.accentHorizontal(),
                            shape = RoundedCornerShape(999.dp),
                        )
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = when {
                !enabled -> VantafynColors.Muted.copy(alpha = 0.38f)
                selected -> VantafynColors.Ink
                else -> VantafynColors.Muted.copy(alpha = 0.86f)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun MusicTrackList(
    title: String,
    tracks: List<JellyfinMusicTrack>,
    page: JellyfinMusicTrackPage? = null,
    isPageLoading: Boolean = false,
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    playlists: List<JellyfinMusicPlaylist> = emptyList(),
    pendingTrackId: java.util.UUID? = null,
    currentTrackId: java.util.UUID? = null,
    selectedTrackIds: Set<java.util.UUID> = emptySet(),
    onToggleSelectTrack: ((JellyfinMusicTrack) -> Unit)? = null,
    onLongPressTrack: ((JellyfinMusicTrack) -> Unit)? = null,
    onTrack: (JellyfinMusicTrack) -> Unit,
    onChoosePlaylist: (JellyfinMusicTrack) -> Unit = {},
    onLongPress: (JellyfinMusicTrack) -> Unit = {},
    animateReveal: Boolean = true,
    isReorderMode: Boolean = false,
    onReorder: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onToggleReorderMode: (() -> Unit)? = null,
    lazyListState: LazyListState? = null,
    viewportBoundsInWindow: Rect? = null,
) {
    val trackRevealKey = "${page?.startIndex ?: 0}:${page?.totalItems ?: tracks.size}:${tracks.size}:${tracks.firstOrNull()?.id}:${tracks.lastOrNull()?.id}"
    var hasPlayed by remember(trackRevealKey) { mutableStateOf(false) }
    var revealTrackRows by remember(trackRevealKey) { mutableStateOf(animateReveal && tracks.isNotEmpty() && !hasPlayed) }
    LaunchedEffect(trackRevealKey) {
        if (!animateReveal || tracks.isEmpty()) {
            revealTrackRows = false
            return@LaunchedEffect
        }
        if (hasPlayed) return@LaunchedEffect
        revealTrackRows = true
        delay(1_550L)
        revealTrackRows = false
        hasPlayed = true
    }

    var localTracks by remember(tracks) { mutableStateOf(tracks) }
    var draggedTrackId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var currentPointerWindowY by remember { mutableFloatStateOf(-1f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val spacingPx = with(density) { 10.dp.toPx() }
    val isSelectionActive = selectedTrackIds.isNotEmpty()

    LaunchedEffect(tracks) {
        if (draggedTrackId == null) {
            localTracks = tracks
        }
    }

    LaunchedEffect(draggedTrackId) {
        val activeTrackId = draggedTrackId ?: return@LaunchedEffect
        val listState = lazyListState ?: return@LaunchedEffect
        val step = if (itemHeightPx > 0f) itemHeightPx + spacingPx else with(density) { 76.dp.toPx() }
        val maxScrollSpeedPx = with(density) { 14.dp.toPx() }
        val topThresholdPx = with(density) { 110.dp.toPx() }
        val bottomThresholdPx = with(density) { 150.dp.toPx() }

        while (isActive && draggedTrackId == activeTrackId) {
            val pointerY = currentPointerWindowY
            val bounds = viewportBoundsInWindow
            val topLimit = bounds?.top ?: with(density) { 70.dp.toPx() }
            val bottomLimit = bounds?.bottom ?: with(density) { (config.screenHeightDp.dp - 100.dp).toPx() }

            val topZone = topLimit + topThresholdPx
            val bottomZone = bottomLimit - bottomThresholdPx

            val scrollDelta = if (pointerY > 0f && pointerY < topZone) {
                val factor = ((topZone - pointerY) / topThresholdPx).coerceIn(0.15f, 1f)
                -maxScrollSpeedPx * factor
            } else if (pointerY > bottomZone && pointerY <= bottomLimit + with(density) { 60.dp.toPx() }) {
                val factor = ((pointerY - bottomZone) / bottomThresholdPx).coerceIn(0.15f, 1f)
                maxScrollSpeedPx * factor
            } else {
                0f
            }

            if (scrollDelta != 0f) {
                val consumed = listState.scrollBy(scrollDelta)
                if (consumed != 0f) {
                    dragOffsetY += consumed
                    val curr = localTracks.indexOfFirst { (it.playlistItemId ?: it.id.toString()) == activeTrackId }
                    if (curr != -1) {
                        if (dragOffsetY > step * 0.5f && curr < localTracks.lastIndex) {
                            val next = curr + 1
                            val list = localTracks.toMutableList()
                            val item = list.removeAt(curr)
                            list.add(next, item)
                            localTracks = list
                            dragOffsetY -= step
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else if (dragOffsetY < -step * 0.5f && curr > 0) {
                            val prev = curr - 1
                            val list = localTracks.toMutableList()
                            val item = list.removeAt(curr)
                            list.add(prev, item)
                            localTracks = list
                            dragOffsetY += step
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else if (curr == 0 && dragOffsetY < -step * 0.5f) {
                            dragOffsetY = -step * 0.5f
                        } else if (curr == localTracks.lastIndex && dragOffsetY > step * 0.5f) {
                            dragOffsetY = step * 0.5f
                        }
                    }
                }
            }
            delay(16L)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title.isNotBlank()) Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        localTracks.forEachIndexed { index, track ->
            val trackKey = track.playlistItemId ?: track.id.toString()
            key(trackKey) {
                val isItemDragged = draggedTrackId == trackKey
                val isTrackSelected = selectedTrackIds.contains(track.id)
                var handleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                MusicContentReveal(index = index, animate = revealTrackRows, revealKey = trackRevealKey) {
                    VantafynGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                if (coords.size.height > 0) itemHeightPx = coords.size.height.toFloat()
                            }
                            .zIndex(if (isItemDragged) 10f else 1f)
                            .graphicsLayer {
                                if (isItemDragged) {
                                    translationY = dragOffsetY
                                    scaleX = 1.025f
                                    scaleY = 1.025f
                                    shadowElevation = 18f
                                } else {
                                    translationY = 0f
                                    scaleX = 1f
                                    scaleY = 1f
                                    shadowElevation = 0f
                                }
                            }
                            .then(
                                if (isTrackSelected) Modifier.vantafynAnimatedModalBorder(cornerRadius = 18.dp, strokeWidth = 1.6.dp)
                                else if (track.id == currentTrackId) Modifier.vantafynAnimatedModalBorder(cornerRadius = 18.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                                else Modifier
                            )
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionActive && onToggleSelectTrack != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleSelectTrack(track)
                                    } else if (isReorderMode && onToggleReorderMode != null) {
                                        onToggleReorderMode()
                                    } else {
                                        onTrack(track)
                                    }
                                },
                                onLongClick = {
                                    if (isReorderMode && onToggleReorderMode != null) {
                                        onToggleReorderMode()
                                    } else if (onLongPressTrack != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPressTrack(track)
                                    } else {
                                        onLongPress(track)
                                    }
                                },
                            ),
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (isSelectionActive) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isTrackSelected) Brush.horizontalGradient(listOf(VantafynColors.Primary, VantafynColors.Secondary))
                                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                        )
                                        .then(
                                            if (!isTrackSelected) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isTrackSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                            if (isReorderMode && onReorder != null) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .onGloballyPositioned { coords ->
                                            handleCoords = coords
                                        }
                                        .pointerInput(trackKey, isReorderMode) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    val initialPos = localTracks.indexOfFirst {
                                                        (it.playlistItemId ?: it.id.toString()) == trackKey
                                                    }
                                                    if (initialPos == -1) return@detectDragGestures
                                                    draggedTrackId = trackKey
                                                    dragStartIndex = initialPos
                                                    dragOffsetY = 0f
                                                    currentPointerWindowY = handleCoords?.let { coords ->
                                                        runCatching { coords.positionInWindow().y }.getOrNull()
                                                    } ?: -1f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (draggedTrackId != trackKey) return@detectDragGestures
                                                    val curr = localTracks.indexOfFirst {
                                                        (it.playlistItemId ?: it.id.toString()) == trackKey
                                                    }
                                                    if (curr == -1) return@detectDragGestures
                                                    handleCoords?.let { coords ->
                                                        val winY = runCatching { coords.positionInWindow().y }.getOrNull()
                                                        if (winY != null) {
                                                            currentPointerWindowY = winY + change.position.y
                                                        }
                                                    }
                                                    dragOffsetY += dragAmount.y
                                                    val step = if (itemHeightPx > 0f) itemHeightPx + spacingPx else with(density) { 76.dp.toPx() }

                                                    if (dragOffsetY > step * 0.5f && curr < localTracks.lastIndex) {
                                                        val next = curr + 1
                                                        val list = localTracks.toMutableList()
                                                        val item = list.removeAt(curr)
                                                        list.add(next, item)
                                                        localTracks = list
                                                        dragOffsetY -= step
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    } else if (dragOffsetY < -step * 0.5f && curr > 0) {
                                                        val prev = curr - 1
                                                        val list = localTracks.toMutableList()
                                                        val item = list.removeAt(curr)
                                                        list.add(prev, item)
                                                        localTracks = list
                                                        dragOffsetY += step
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    } else if (curr == 0 && dragOffsetY < -step * 0.5f) {
                                                        dragOffsetY = -step * 0.5f
                                                    } else if (curr == localTracks.lastIndex && dragOffsetY > step * 0.5f) {
                                                        dragOffsetY = step * 0.5f
                                                    }
                                                },
                                                onDragEnd = {
                                                    val start = dragStartIndex
                                                    val finish = localTracks.indexOfFirst {
                                                        (it.playlistItemId ?: it.id.toString()) == trackKey
                                                    }
                                                    draggedTrackId = null
                                                    dragStartIndex = null
                                                    dragOffsetY = 0f
                                                    currentPointerWindowY = -1f
                                                    if (start != null && finish != -1 && start != finish) {
                                                        onReorder(start, finish)
                                                    }
                                                },
                                                onDragCancel = {
                                                    draggedTrackId = null
                                                    dragStartIndex = null
                                                    dragOffsetY = 0f
                                                    currentPointerWindowY = -1f
                                                    localTracks = tracks
                                                },
                                            )
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        tint = VantafynColors.Ink,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            MusicArt(track.artworkUrl, Modifier.size(52.dp), title = track.title, subtitle = track.artist)
                            Column(Modifier.weight(1f)) {
                                Text(track.title, color = VantafynColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(track.durationMs?.formatTime().orEmpty(), color = VantafynColors.Muted)
                            if (pendingTrackId == track.id) {
                                VantafynGradientLoadingRing(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            }
                            if (playlists.isNotEmpty() && !isSelectionActive) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onChoosePlaylist(track) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "+",
                                        color = VantafynColors.Ink,
                                        fontWeight = FontWeight.Bold,
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

@Composable
private fun MusicSectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        VantafynGlassChip(selected = true, onClick = onAction) {
            Text(action, color = VantafynColors.Ink, maxLines = 1)
        }
    }
}

@Composable
private fun MusicChevronBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(19.dp)) {
            val stroke = 2.35.dp.toPx()
            drawLine(
                color = VantafynColors.Ink,
                start = Offset(11.5.dp.toPx(), 3.dp.toPx()),
                end = Offset(5.dp.toPx(), 9.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = VantafynColors.Ink,
                start = Offset(5.dp.toPx(), 9.dp.toPx()),
                end = Offset(11.5.dp.toPx(), 15.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MusicSimpleHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicChevronBackButton(onBack)
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MusicTopBackHeader(
    title: String,
    onBack: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusicChevronBackButton(onBack)
            Text(
                title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

private val MusicFavoritePink = Color(0xFFFF4B6E)

@Composable
private fun MusicTopFavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isFavorite) MusicFavoritePink.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) MusicFavoritePink else VantafynColors.Ink,
            modifier = Modifier.size(20.dp),
        )
    }
}

private enum class DetailDownloadState {
    READY,
    DOWNLOADING,
    DOWNLOADED,
}

@Composable
private fun MusicDetailHeader(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onBack: () -> Unit,
    onDownload: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    trackImageUrls: List<String> = emptyList(),
    onToggleReorder: (() -> Unit)? = null,
    isReordering: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onPlay: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (trackImageUrls.size >= 2) {
            PlaylistArtGrid(trackImageUrls, Modifier.size(188.dp))
        } else {
            MusicCollectionArtwork(imageUrl, Modifier.size(188.dp), title = title, subtitle = subtitle)
        }
        Text(
            title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                subtitle,
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onPlay != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VantafynButton(
                    "Play",
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                )
                if (onToggleFavorite != null) {
                    VantafynGlassSurface(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onToggleFavorite),
                        variant = if (isFavorite) VantafynGlassVariant.Panel else VantafynGlassVariant.Card,
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) MusicFavoritePink else VantafynColors.Ink,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                if (onToggleReorder != null) {
                    VantafynGlassSurface(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onToggleReorder),
                        variant = if (isReordering) VantafynGlassVariant.Panel else VantafynGlassVariant.Card,
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isReordering) Icons.Rounded.Check else Icons.Rounded.Reorder,
                                contentDescription = if (isReordering) "Done reordering" else "Reorder playlist",
                                tint = if (isReordering) VantafynColors.Primary else VantafynColors.Ink,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                if (onDownload != null) {
                    val downloadButtonState = when {
                        isDownloaded -> DetailDownloadState.DOWNLOADED
                        isDownloading -> DetailDownloadState.DOWNLOADING
                        else -> DetailDownloadState.READY
                    }
                    VantafynGlassSurface(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                enabled = downloadButtonState == DetailDownloadState.READY,
                                onClick = onDownload,
                            ),
                        variant = VantafynGlassVariant.Card,
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = downloadButtonState,
                                transitionSpec = {
                                    (fadeIn(tween(300)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                        .togetherWith(fadeOut(tween(180)) + scaleOut())
                                },
                                label = "detail_download_button_state",
                            ) { buttonState ->
                                when (buttonState) {
                                    DetailDownloadState.READY -> {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = "Save offline",
                                            tint = VantafynColors.Ink,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    DetailDownloadState.DOWNLOADING -> {
                                        Box(
                                            modifier = Modifier.size(38.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            VantafynCircularProgressIndicator(
                                                progress = if (downloadProgress > 0f) downloadProgress else null,
                                                modifier = Modifier.size(36.dp),
                                                strokeWidth = 3.dp,
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDownward,
                                                contentDescription = "Downloading",
                                                tint = VantafynColors.Ink,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                    }
                                    DetailDownloadState.DOWNLOADED -> {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(VantafynGradients.accentHorizontal()),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Downloaded",
                                                tint = VantafynColors.Ink,
                                                modifier = Modifier.size(19.dp),
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
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun MusicCollectionArtwork(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.035f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center,
    ) {
        MusicArt(imageUrl, Modifier.fillMaxSize(), cornerRadius = 30, title = title, subtitle = subtitle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                        ),
                    ),
                ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicArtworkTile(
    imageUrl: String?,
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    badgeText: String? = null,
    trackImageUrls: List<String> = emptyList(),
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (trackImageUrls.size >= 2) {
                PlaylistArtGrid(trackImageUrls, Modifier.size(128.dp))
            } else {
                MusicArt(imageUrl, Modifier.size(128.dp), title = title, subtitle = subtitle)
            }
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                            fontWeight = FontWeight.SemiBold,
                            color = VantafynColors.Ink,
                        ),
                        maxLines = 1,
                    )
                }
            }
            if (isLoading) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center,
                ) {
                    VantafynGradientLoadingRing(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(rememberLifecycleAwareMarquee()),
            )
            Text(
                text = subtitle,
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(rememberLifecycleAwareMarquee()),
            )
        }
    }
}

@Composable
private fun VantafynGradientLoadingRing(modifier: Modifier = Modifier, strokeWidth: androidx.compose.ui.unit.Dp = 2.dp) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val rotation by if (isResumed) {
        val transition = rememberInfiniteTransition(label = "musicGradientLoadingRing")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 960, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "musicGradientLoadingRotation",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val ringColors = remember {
        listOf(
            Color(0xFF31D7FF),
            Color(0xFF45C0FF),
            Color(0xFF5B8CFF),
            Color(0xFF8368FF),
            Color(0xFFC05CFF),
            Color(0xFF9A62FF),
            Color(0xFF5B8CFF),
            Color(0xFF45C0FF),
            Color(0xFF31D7FF),
        )
    }
    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        val strokePx = strokeWidth.toPx()
        drawCircle(
            color = Color.White.copy(alpha = 0.13f),
            radius = (size.minDimension - strokePx) / 2f,
            style = Stroke(width = strokePx),
        )
        drawArc(
            brush = Brush.sweepGradient(ringColors),
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun rememberReducedMotionPreference(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        val resolver = context.contentResolver
        val animatorScale = runCatching {
            android.provider.Settings.Global.getFloat(resolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val transitionScale = runCatching {
            android.provider.Settings.Global.getFloat(resolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        }.getOrDefault(1f)
        animatorScale == 0f || transitionScale == 0f
    }
}

@Composable
private fun MiniPlayerInteriorAtmosphere(
    fadeAlpha: Float,
    isPlaying: Boolean,
    isScrolling: Boolean,
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    if (fadeAlpha <= 0.005f) return

    val reducedMotion = rememberReducedMotionPreference()
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val shouldAnimate = isResumed && !reducedMotion && isPlaying && fadeAlpha > 0.01f

    val driftProgress by if (shouldAnimate) {
        val infiniteTransition = rememberInfiniteTransition(label = "miniPlayerInteriorAtmosphere")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(6500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "miniDrift",
        )
    } else {
        animateFloatAsState(
            targetValue = 0.5f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing),
            label = "miniDriftSettled",
        )
    }

    val pulseProgress by if (shouldAnimate) {
        val infiniteTransition = rememberInfiniteTransition(label = "miniPlayerPulse")
        infiniteTransition.animateFloat(
            initialValue = 0.70f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "miniPulse",
        )
    } else {
        animateFloatAsState(
            targetValue = 0.85f,
            animationSpec = tween(800, easing = FastOutSlowInEasing),
            label = "miniPulseSettled",
        )
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                val width = size.width
                val height = size.height
                val radius = width * 0.44f
                val bloomY = height * 0.5f

                // Pre-allocated static gradient brushes cached per size change (0 allocations during animation)
                val cyanBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 1.30f),
                        Color(0xFF00B0FF).copy(alpha = 0.65f),
                        Color.Transparent,
                    ),
                    center = Offset.Zero,
                    radius = radius,
                )
                val violetBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9B5CFF).copy(alpha = 1.30f),
                        Color(0xFF5B8CFF).copy(alpha = 0.65f),
                        Color.Transparent,
                    ),
                    center = Offset.Zero,
                    radius = radius,
                )
                val washBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.35f),
                        Color(0xFF5B8CFF).copy(alpha = 0.45f),
                        Color(0xFF9B5CFF).copy(alpha = 0.40f),
                        Color(0xFF00E5FF).copy(alpha = 0.35f),
                    ),
                    startX = 0f,
                    endX = width,
                )

                onDrawBehind {
                    if (width <= 0f || height <= 0f) return@onDrawBehind

                    val drift = if (isScrolling) 0.5f else driftProgress
                    val pulse = if (isScrolling) 0.85f else pulseProgress

                    val bloom1X = width * (0.08f + drift * 0.42f)
                    val bloom2X = width * (0.92f - drift * 0.42f)

                    val baseAlpha = (0.09f * pulse) * fadeAlpha

                    // 1. Electric Cyan Nebula Bloom (zero-alloc GPU matrix translation)
                    translate(left = bloom1X, top = bloomY) {
                        drawCircle(
                            brush = cyanBrush,
                            radius = radius,
                            center = Offset.Zero,
                            alpha = baseAlpha,
                        )
                    }

                    // 2. Violet / Indigo Light Bloom (zero-alloc GPU matrix translation)
                    translate(left = bloom2X, top = bloomY) {
                        drawCircle(
                            brush = violetBrush,
                            radius = radius,
                            center = Offset.Zero,
                            alpha = baseAlpha,
                        )
                    }

                    // 3. Continuous ambient horizontal gradient wash
                    drawRect(
                        brush = washBrush,
                        alpha = baseAlpha,
                        topLeft = Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(width, height),
                    )
                }
            },
    ) { }
}

@Composable
private fun MusicMiniPlayer(
    track: VantafynMusicTrack,
    isPlaying: Boolean,
    progress: Float,
    isScrolling: Boolean = false,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dismissThresholdPx = with(density) { 116.dp.toPx() }
    val maxDragPx = with(density) { 340.dp.toPx() }
    val offsetX = remember(track.id) { Animatable(0f) }
    var isDismissing by remember(track.id) { mutableStateOf(false) }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPlaying) 900 else 650, easing = FastOutSlowInEasing),
        label = "musicMiniBorderAlpha",
    )

    VantafynGlassDock(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX.value
                val dragRatio = (abs(offsetX.value) / dismissThresholdPx).coerceIn(0f, 1.5f)
                alpha = if (isDismissing) {
                    (1f - (abs(offsetX.value) / (dismissThresholdPx * 1.6f))).coerceIn(0f, 1f)
                } else {
                    1f - (dragRatio * 0.22f)
                }
            }
            .then(
                if (borderAlpha > 0.01f) {
                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 22.dp, strokeWidth = 1.5.dp, alpha = borderAlpha)
                } else {
                    Modifier
                },
            )
            .pointerInput(track.id) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (!isDismissing) {
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-maxDragPx, maxDragPx)
                            scope.launch { offsetX.snapTo(newOffset) }
                        }
                    },
                    onDragEnd = {
                        if (!isDismissing) {
                            val currentOffset = offsetX.value
                            val shouldDismiss = abs(currentOffset) >= dismissThresholdPx
                            if (shouldDismiss) {
                                isDismissing = true
                                scope.launch {
                                    val target = if (currentOffset > 0) maxDragPx * 1.5f else -maxDragPx * 1.5f
                                    offsetX.animateTo(
                                        targetValue = target,
                                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                                    )
                                    onStop()
                                }
                            } else {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        if (!isDismissing) {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
            .clickable(onClick = onOpen),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        MiniPlayerInteriorAtmosphere(
            fadeAlpha = borderAlpha,
            isPlaying = isPlaying,
            isScrolling = isScrolling,
            cornerRadius = 22.dp,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MusicArt(track.artworkUrl, Modifier.size(50.dp), cornerRadius = 14, title = track.title, subtitle = track.artist)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    VantafynMarqueeText(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge.copy(color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold),
                    )
                    VantafynMarqueeText(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(color = VantafynColors.Muted),
                    )
                }
                GoogleCastRouteButton(modifier = Modifier.size(38.dp))
                FlatMusicIconButton(Icons.Rounded.SkipPrevious, "Previous", onPrevious, size = 38)
                GradientPlayButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = onToggle,
                    size = 42,
                )
                FlatMusicIconButton(Icons.Rounded.SkipNext, "Next", onNext, size = 38)
            }
            MusicProgressStrip(progress = progress, height = 4)
        }
    }
}

@Composable
private fun NowPlayingDialog(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onRequestMusicControlsPermission: ((() -> Unit) -> Unit),
    onChoosePlaylist: () -> Unit,
    onTrackDetails: (MusicTrackDetails) -> Unit,
) {
    var showPlaylistName by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showAutoEqDialog by remember { mutableStateOf(false) }
    var showReplayGainDialog by remember { mutableStateOf(false) }
    var showAudioStreamDetailsSheet by remember { mutableStateOf(false) }
    var showStreamingBitrateSheet by remember { mutableStateOf(false) }
    var showSaveQueueDialog by remember { mutableStateOf(false) }
    var contextQueueIndex by remember { mutableIntStateOf(-1) }
    val track = state.playback.currentTrack ?: return
    val isDownloaded = track.streamUrl.startsWith("file:") || track.streamUrl.startsWith("content:")
    val context = androidx.compose.ui.platform.LocalContext.current
    var revealActive by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        revealActive = true
        kotlinx.coroutines.delay(1_450L)
        revealActive = false
    }
    Box(
        Modifier.fillMaxSize(),
    ) {
        MusicReactiveBackground(track = track)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            VantafynColors.Graphite.copy(alpha = 0.10f),
                            VantafynColors.Graphite.copy(alpha = 0.24f),
                        ),
                    ),
                ),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                MusicContentReveal(index = 0, animate = revealActive) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (!isDownloaded) {
                                MusicBitrateSettingsIconButton(
                                    onClick = { showStreamingBitrateSheet = true },
                                    size = 40,
                                )
                            }
                            FlatMusicIconButton(
                                icon = Icons.Rounded.Bedtime,
                                contentDescription = "Sleep Timer",
                                onClick = { showSleepTimerSheet = true },
                                selected = state.playback.sleepTimerMode != null,
                                size = 40,
                            )
                            if (state.playback.sleepTimerMode != null) {
                                val label = when (state.playback.sleepTimerMode) {
                                    SleepTimerMode.Duration -> {
                                        val secs = state.playback.sleepTimerRemainingSeconds ?: 0L
                                        val mins = (secs + 59) / 60
                                        "${mins}m"
                                    }
                                    SleepTimerMode.EndOfTrack -> "Track"
                                    SleepTimerMode.EndOfQueue -> "End"
                                    null -> ""
                                }
                                VantafynGlassSurface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { showSleepTimerSheet = true },
                                    variant = VantafynGlassVariant.Chip,
                                    cornerRadius = 999.dp,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(12.dp))
                                        Text(label, color = Color(0xFF21D8FF), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        Text(
                            "Now Playing",
                            color = VantafynColors.Ink.copy(alpha = 0.90f),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GoogleCastRouteButton(modifier = Modifier.size(44.dp))
                            FlatMusicIconButton(Icons.Rounded.Close, "Close", viewModel::closeNowPlaying, size = 44)
                        }

                        if (state.isRadioActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, 0) {
                                            placeable.placeRelative(0, 0)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                InfiniteRadioPill()
                            }
                        }
                    }
                }
            }
            item {
                MusicContentReveal(index = 1, animate = revealActive) {
                    AnimatedContent(
                        targetState = track,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing))
                        },
                        contentAlignment = Alignment.Center,
                        label = "NowPlayingAlbumArtCrossfade",
                    ) { currentTrack ->
                        MusicArt(
                            imageUrl = currentTrack.artworkUrl,
                            modifier = Modifier
                                .size(296.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(30.dp)),
                            cornerRadius = 30,
                            title = currentTrack.title,
                            subtitle = currentTrack.artist,
                        )
                    }
                }
            }
            item {
                MusicContentReveal(index = 2, animate = revealActive) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        VantafynMarqueeText(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                        )
                        VantafynMarqueeText(
                            text = listOfNotNull(track.artist, track.album).joinToString(" - "),
                            style = MaterialTheme.typography.bodyLarge.copy(color = VantafynColors.Muted),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            item {
                MusicContentReveal(index = 3, animate = revealActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MusicScrubber(
                            positionMs = state.playback.positionMs,
                            durationMs = state.playback.durationMs,
                            isPlaying = state.playback.isPlaying,
                            onSeek = viewModel::seekTo,
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = state.playback.positionMs.formatTime(),
                                color = VantafynColors.Muted,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFeatureSettings = "tnum",
                                ),
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                            AudioQualityBadgePill(
                                audioStreamInfo = state.playback.audioStreamInfo,
                                track = track,
                                onClick = { showAudioStreamDetailsSheet = true },
                                modifier = Modifier.align(Alignment.Center),
                            )
                            Text(
                                text = state.playback.durationMs.formatTime(),
                                color = VantafynColors.Muted,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFeatureSettings = "tnum",
                                ),
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )
                        }
                    }
                }
            }
            item {
                MusicContentReveal(index = 4, animate = revealActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FlatMusicIconButton(Icons.Rounded.Shuffle, "Shuffle", viewModel::toggleShuffle, selected = state.playback.shuffleEnabled, size = 46)
                        FlatMusicIconButton(Icons.Rounded.SkipPrevious, "Previous", viewModel::previous, size = 54)
                        GradientPlayButton(
                            if (state.playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (state.playback.isPlaying) "Pause" else "Play",
                            {
                                if (state.playback.isPlaying) {
                                    viewModel.togglePlayPause()
                                } else {
                                    onRequestMusicControlsPermission { viewModel.togglePlayPause() }
                                }
                            },
                            size = 72,
                        )
                        FlatMusicIconButton(Icons.Rounded.SkipNext, "Next", viewModel::next, size = 54)
                        FlatMusicIconButton(
                            if (state.playback.repeatMode == VantafynMusicRepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            "Repeat",
                            viewModel::cycleRepeat,
                            selected = state.playback.repeatMode != VantafynMusicRepeatMode.Off,
                        )
                    }
                }
            }
            if (state.isCasting) {
                item {
                    MusicContentReveal(index = 5, animate = revealActive) {
                        CastDeviceVolumeBar(
                            deviceName = state.castReceiverName ?: "Cast device",
                            volume = state.castVolume,
                            isMuted = state.isCastMuted,
                            onVolumeChange = viewModel::setCastVolume,
                            onToggleMute = viewModel::toggleCastMute,
                        )
                    }
                }
            }
            item {
                MusicContentReveal(index = 5, animate = revealActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconPill(Icons.Rounded.PlaylistAdd, "New Playlist") { showPlaylistName = true }
                        IconPill(Icons.Rounded.Subtitles, "Lyrics", viewModel::openLyrics)
                        IconPill(Icons.Rounded.MoreHoriz, "More") { showMoreSheet = true }
                    }
                }
            }
            item {
                MusicContentReveal(index = 6, animate = revealActive) {
                    QueuePanel(
                        queue = state.playback.queue,
                        index = state.playback.queueIndex,
                        onTrack = viewModel::playQueueIndex,
                        onRemove = viewModel::removeQueueItem,
                        onReorder = viewModel::moveQueueTrack,
                        onLongPress = { contextQueueIndex = it },
                        onSaveQueue = { showSaveQueueDialog = true },
                        onClearUpcoming = viewModel::clearUpcomingQueue,
                        onClearAll = viewModel::clearAllQueue,
                    )
                }
            }
            item {
                Spacer(Modifier.height(96.dp))
            }
        }
        CurrentTrackMoreSheet(
            visible = showMoreSheet,
            track = track,
            playlists = state.home?.playlists.orEmpty(),
            isRadioActive = state.isRadioActive,
            onDismiss = { showMoreSheet = false },
            onNewPlaylist = {
                showMoreSheet = false
                showPlaylistName = true
            },
            onPlayNext = {
                showMoreSheet = false
                viewModel.playCurrentNext()
            },
            onAddToQueue = {
                showMoreSheet = false
                viewModel.addCurrentToQueue()
            },
            onToggleRadio = {
                showMoreSheet = false
                if (state.isRadioActive) {
                    viewModel.stopRadio()
                } else {
                    viewModel.startRadio(track)
                }
            },
            onChoosePlaylist = {
                showMoreSheet = false
                onChoosePlaylist()
            },
            canGoToArtist = state.home?.artists?.any { it.name.equals(track.artist, ignoreCase = true) } == true,
            onGoToAlbum = {
                showMoreSheet = false
                viewModel.openCurrentAlbum()
            },
            onGoToArtist = {
                showMoreSheet = false
                viewModel.openCurrentArtist()
            },
            onFavorite = {
                showMoreSheet = false
                viewModel.toggleCurrentFavorite()
            },
            onOpenAutoEq = {
                showMoreSheet = false
                showAutoEqDialog = true
            },
            onOpenReplayGain = {
                showMoreSheet = false
                showReplayGainDialog = true
            },
            onTrackDetails = {
                showMoreSheet = false
                onTrackDetails(track.toDetails())
            },
        )
        SleepTimerSheet(
            visible = showSleepTimerSheet,
            playback = state.playback,
            onDismiss = { showSleepTimerSheet = false },
            onSetDuration = viewModel::setSleepTimer,
            onSetEndOfTrack = viewModel::setSleepTimerEndOfTrack,
            onSetEndOfQueue = viewModel::setSleepTimerEndOfQueue,
            onCancel = viewModel::cancelSleepTimer,
        )
        AudioStreamDetailsSheet(
            visible = showAudioStreamDetailsSheet,
            track = track,
            audioStreamInfo = state.playback.audioStreamInfo,
            onDismiss = { showAudioStreamDetailsSheet = false },
            onOpenReplayGainSettings = { showReplayGainDialog = true },
        )
        MusicStreamingBitrateSheet(
            visible = showStreamingBitrateSheet,
            currentQuality = dev.vantafyn.core.media.music.MusicQualityPreferences.resolveCurrentQuality(context),
            onSelectQuality = { quality ->
                viewModel.setStreamingQuality(quality)
                showStreamingBitrateSheet = false
            },
            onDismiss = { showStreamingBitrateSheet = false },
        )
        if (showAutoEqDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAutoEqDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            ) {
                dev.vantafyn.feature.music.autoeq.AutoEqSearchScreen(
                    onDismiss = { showAutoEqDialog = false },
                )
            }
        }
        if (showReplayGainDialog) {
            val playbackController = remember(context) { dev.vantafyn.core.media.MusicPlaybackController.get(context) }
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showReplayGainDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            ) {
                dev.vantafyn.feature.music.replaygain.ReplayGainDialog(
                    playbackController = playbackController,
                    currentTrack = track,
                    onDismiss = { showReplayGainDialog = false },
                )
            }
        }
    }
    if (showPlaylistName) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            modifier = Modifier
                .imePadding()
                .vantafynAnimatedModalBorder(),
            onDismissRequest = { showPlaylistName = false },
            containerColor = VantafynModalContainerColor,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Create playlist", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold) },
            text = { VantafynTextField(value = name, onValueChange = { name = it }, label = "Playlist name") },
            confirmButton = {
                TextButton(onClick = {
                    showPlaylistName = false
                    viewModel.createPlaylistWithCurrent(name)
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showPlaylistName = false }) { Text("Cancel") } },
        )
    }
    if (showSaveQueueDialog) {
        MusicSaveQueueDialog(
            onDismiss = { showSaveQueueDialog = false },
            onSave = { playlistTitle ->
                viewModel.saveQueueAsPlaylist(playlistTitle)
            },
        )
    }
    if (contextQueueIndex >= 0) {
        val queueTrack = state.playback.queue.getOrNull(contextQueueIndex)
        if (queueTrack != null) {
            QueueTrackContextMenu(
                track = queueTrack,
                onDismiss = { contextQueueIndex = -1 },
                onPlayNext = { viewModel.playQueueItemNext(contextQueueIndex) },
                onRemoveFromQueue = { viewModel.removeQueueItem(contextQueueIndex) },
                onGoToAlbum = { },
            )
        }
    }
}

@Composable
private fun InfiniteRadioPill(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val infiniteTransition = rememberInfiniteTransition(label = "radio_pulse")
    val radioAlphaState = if (isResumed) {
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "radio_alpha",
        )
    } else null

    val radioGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF21D8FF),
                Color(0xFFFF36C7),
            ),
        )
    }
    VantafynGlassSurface(
        modifier = modifier.clip(RoundedCornerShape(999.dp)),
        variant = VantafynGlassVariant.Chip,
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.graphicsLayer {
                alpha = radioAlphaState?.value ?: 0.95f
            },
        ) {
            Icon(
                Icons.Rounded.Radio,
                contentDescription = "Infinite Radio Active",
                tint = Color(0xFF21D8FF),
                modifier = Modifier.size(13.dp),
            )
            Text(
                "INFINITE RADIO",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall.copy(
                    brush = radioGradient,
                ),
                letterSpacing = 0.6.sp,
            )
        }
    }
}

@Composable
private fun MusicReactiveBackground(track: VantafynMusicTrack) {
    val context = LocalContext.current
    val fallbackPalette = remember(track.id, track.artworkUrl) { track.musicPalette() }
    var palette by remember(track.id, track.artworkUrl) { mutableStateOf(fallbackPalette) }
    LaunchedEffect(track.id, track.artworkUrl) {
        palette = fallbackPalette
        val artworkUrl = track.artworkUrl ?: return@LaunchedEffect
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .allowHardware(false)
                    .size(96, 96)
                    .build()
                val result = context.imageLoader.execute(request)
                (result as? SuccessResult)
                    ?.image
                    ?.let { it as? BitmapImage }
                    ?.bitmap
                    ?.let { bitmap -> bitmap.toMusicPalette(fallbackPalette) }
            }.getOrNull()
        }
        if (extracted != null) palette = extracted
    }
    val animatedBase by animateColorAsState(
        targetValue = palette.base,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "reactive_bg_base",
    )
    val animatedAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "reactive_bg_accent",
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "reactive_bg_secondary",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(animatedBase),
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(animatedAccent.copy(alpha = 0.70f), Color.Transparent),
                    center = Offset(160f, 220f),
                    radius = 720f,
                ),
            ),
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(animatedSecondary.copy(alpha = 0.62f), Color.Transparent),
                    center = Offset(860f, 860f),
                    radius = 840f,
                ),
            ),
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        animatedBase.copy(alpha = 0.34f),
                        VantafynColors.Graphite.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.30f),
                    ),
                ),
            ),
    )
}

private data class MusicPalette(val base: Color, val accent: Color, val secondary: Color)

private fun VantafynMusicTrack.musicPalette(): MusicPalette {
    val seed = (artworkUrl ?: album ?: artist ?: title).hashCode()
    val palettes = listOf(
        MusicPalette(Color(0xFF060B18), Color(0xFF00D4FF), Color(0xFF7B4DFF)),
        MusicPalette(Color(0xFF100817), Color(0xFFFF4FD8), Color(0xFF516BFF)),
        MusicPalette(Color(0xFF07130F), Color(0xFF31F3B0), Color(0xFF3478FF)),
        MusicPalette(Color(0xFF140D08), Color(0xFFFFB04D), Color(0xFF8E5CFF)),
        MusicPalette(Color(0xFF08101B), Color(0xFF46B7FF), Color(0xFFD64DFF)),
    )
    return palettes[(seed and Int.MAX_VALUE) % palettes.size]
}

private data class SampledColor(val red: Int, val green: Int, val blue: Int, val hue: Float, val saturation: Float, val luminance: Float)

private fun Bitmap.toMusicPalette(fallback: MusicPalette): MusicPalette {
    val samples = sampledArtworkColors()
    if (samples.isEmpty()) return fallback
    val accent = samples.maxByOrNull { it.paletteScore() } ?: return fallback
    val secondary = samples
        .filter { hueDistance(it.hue, accent.hue) > 32f }
        .maxByOrNull { it.paletteScore() }
        ?: samples.getOrNull(samples.size / 2)
        ?: accent
    return MusicPalette(
        base = averageColor(samples).darkened(0.20f),
        accent = accent.toComposeColor().lifted(),
        secondary = secondary.toComposeColor().lifted(),
    )
}

private fun Bitmap.sampledArtworkColors(): List<SampledColor> {
    val stepX = max(1, width / 18)
    val stepY = max(1, height / 18)
    val colors = mutableListOf<SampledColor>()
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val pixel = getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha > 180) {
                val red = android.graphics.Color.red(pixel)
                val green = android.graphics.Color.green(pixel)
                val blue = android.graphics.Color.blue(pixel)
                val hsl = FloatArray(3)
                android.graphics.Color.RGBToHSV(red, green, blue, hsl)
                val luminance = colorLuminance(red, green, blue)
                if (hsl[1] > 0.16f && luminance in 0.08f..0.88f) {
                    colors += SampledColor(red, green, blue, hsl[0], hsl[1], luminance)
                }
            }
            x += stepX
        }
        y += stepY
    }
    return colors
}

private fun SampledColor.paletteScore(): Float {
    val balancedLight = 1f - abs(luminance - 0.52f)
    return saturation * 0.72f + balancedLight * 0.28f
}

private fun averageColor(colors: List<SampledColor>): Color {
    val red = colors.sumOf { it.red }.toFloat() / colors.size
    val green = colors.sumOf { it.green }.toFloat() / colors.size
    val blue = colors.sumOf { it.blue }.toFloat() / colors.size
    return Color(red.toInt(), green.toInt(), blue.toInt())
}

private fun SampledColor.toComposeColor(): Color = Color(red, green, blue)

private fun Color.darkened(amount: Float): Color =
    Color(red = red * amount, green = green * amount, blue = blue * amount, alpha = alpha)

private fun Color.lifted(): Color =
    Color(
        red = min(1f, red * 1.22f + 0.035f),
        green = min(1f, green * 1.22f + 0.035f),
        blue = min(1f, blue * 1.22f + 0.035f),
        alpha = alpha,
    )

private fun colorLuminance(red: Int, green: Int, blue: Int): Float =
    (0.2126f * (red / 255f)) + (0.7152f * (green / 255f)) + (0.0722f * (blue / 255f))

private fun hueDistance(first: Float, second: Float): Float {
    val distance = abs(first - second)
    return min(distance, 360f - distance)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VantafynMarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Text(
        text = text,
        color = style.color,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        textAlign = textAlign,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                    Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 900,
                        repeatDelayMillis = 4200,
                        velocity = 28.dp,
                    )
                } else {
                    Modifier
                }
            ),
    )
}

@Composable
private fun MusicProgressStrip(progress: Float, modifier: Modifier = Modifier, height: Int = 5) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(VantafynGradients.accentHorizontal()),
        )
    }
}

@Composable
private fun CastDeviceVolumeBar(
    deviceName: String,
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.70f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cast,
                        contentDescription = "Casting",
                        tint = VantafynColors.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = VantafynColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${if (isMuted) 0 else (volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFeatureSettings = "tnum",
                        color = VantafynColors.Muted,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isMuted || volume == 0f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) VantafynColors.Muted else VantafynColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Slider(
                    value = if (isMuted) 0f else volume.coerceIn(0f, 1f),
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = VantafynColors.Primary,
                        activeTrackColor = VantafynColors.Primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun FlatMusicIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Int = 44,
    selected: Boolean = false,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) Color(0xFF6EE7FF) else VantafynColors.Ink.copy(alpha = 0.92f),
            modifier = Modifier.size((size * 0.52f).dp),
        )
    }
}

@Composable
private fun GradientPlayButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Int,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(VantafynGradients.accentHorizontal())
            .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size((size * 0.48f).dp))
    }
}

private data class MusicTrackDetails(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long?,
    val artworkUrl: String?,
    val hasLyrics: Boolean?,
    val isFavorite: Boolean,
)

@Composable
private fun MusicTrackDetailsSheet(
    track: MusicTrackDetails,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        VantafynGlassModalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = MusicBottomSheetRailClearance)
                .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.5.dp)
                .clickable(enabled = false) {},
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MusicArt(track.artworkUrl, Modifier.size(72.dp), cornerRadius = 18, title = track.title, subtitle = track.artist)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Track details", color = VantafynColors.Muted, style = MaterialTheme.typography.labelLarge)
                        Text(
                            track.title,
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FlatMusicIconButton(Icons.Rounded.Close, "Close details", onDismiss, size = 40)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MusicDetailLine("Album", track.album?.takeIf { it.isNotBlank() } ?: "Unknown album")
                    MusicDetailLine("Duration", track.durationMs?.formatTime()?.takeIf { it != "0:00" } ?: "Unknown")
                    MusicDetailLine("Lyrics", when (track.hasLyrics) {
                        true -> "Available"
                        false -> "Not available"
                        null -> "Checking from current playback"
                    })
                    MusicDetailLine("My List", if (track.isFavorite) "Added" else "Not added")
                }
                Text(
                    "Jellyfin item ${track.id}",
                    color = VantafynColors.Muted.copy(alpha = 0.70f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MusicDetailLine(label: String, value: String) {
    VantafynGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = VantafynGlassVariant.Card,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = VantafynColors.Muted, fontWeight = FontWeight.SemiBold)
            Text(
                value,
                color = VantafynColors.Ink,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun CurrentTrackMoreSheet(
    visible: Boolean,
    track: VantafynMusicTrack,
    playlists: List<JellyfinMusicPlaylist>,
    isRadioActive: Boolean = false,
    onDismiss: () -> Unit,
    onNewPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleRadio: () -> Unit = {},
    onChoosePlaylist: () -> Unit,
    canGoToArtist: Boolean,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onFavorite: () -> Unit,
    onOpenAutoEq: () -> Unit = {},
    onOpenReplayGain: () -> Unit = {},
    onTrackDetails: () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topClearance = maxOf(statusBarTop + 48.dp, 84.dp)
    val extraOffsetPx = remember(density) {
        with(density) { (MusicBottomSheetRailClearance + 56.dp).roundToPx() }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            VantafynGlassModalPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = topClearance, bottom = MusicBottomSheetRailClearance)
                    .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.5.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight + extraOffsetPx },
                            animationSpec = spring(
                                dampingRatio = 0.84f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight + extraOffsetPx },
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f),
                            ),
                        ),
                    ),
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Drag indicator handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 38.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                    )

                    // Track header row with artwork, title/artist, and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MusicArt(
                            imageUrl = track.artworkUrl,
                            modifier = Modifier.size(50.dp),
                            cornerRadius = 14,
                            title = track.title,
                            subtitle = track.artist,
                        )
                        Column(Modifier.weight(1f)) {
                            VantafynMarqueeText(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = VantafynColors.Ink,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            VantafynMarqueeText(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = VantafynColors.Muted,
                                ),
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = VantafynColors.Muted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )

                    // Scrollable list of actions
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MusicMenuAction(
                            if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            if (track.isFavorite) "Remove from My List" else "Add to My List",
                            onFavorite,
                        )
                        MusicMenuAction(
                            Icons.Rounded.Radio,
                            if (isRadioActive) "End Station Radio" else "Start Station Radio",
                            onToggleRadio,
                        )
                        MusicMenuAction(Icons.Rounded.NavigateNext, "Play next", onPlayNext)
                        MusicMenuAction(Icons.Rounded.QueueMusic, "Add to queue", onAddToQueue)
                        MusicMenuAction(Icons.Rounded.PlaylistAdd, "New playlist", onNewPlaylist)
                        if (playlists.isNotEmpty()) {
                            MusicMenuAction(Icons.Rounded.Add, "Add to playlist", onChoosePlaylist)
                        }
                        if (track.albumId != null) MusicMenuAction(Icons.Rounded.Album, "Go to album", onGoToAlbum)
                        if (canGoToArtist) MusicMenuAction(Icons.Rounded.LibraryMusic, "Go to artist", onGoToArtist)
                        MusicMenuAction(Icons.Rounded.GraphicEq, "Headphone EQ (AutoEQ)", onOpenAutoEq)
                        MusicMenuAction(Icons.Rounded.VolumeUp, "Loudness Leveling (ReplayGain)", onOpenReplayGain)
                        MusicMenuAction(Icons.Rounded.Info, "View track details", onTrackDetails)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

sealed interface MusicContextItem {
    data class Track(val track: JellyfinMusicTrack) : MusicContextItem
    data class Album(val album: JellyfinMusicAlbum) : MusicContextItem
    data class Artist(val artist: JellyfinMusicArtist) : MusicContextItem
}

@Composable
private fun MusicContextActionSheet(
    title: String,
    subtitle: String?,
    artworkUrl: String?,
    isFavorite: Boolean = false,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        VantafynGlassModalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = MusicBottomSheetRailClearance)
                .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.5.dp)
                .clickable(enabled = false) {},
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MusicArt(artworkUrl, Modifier.size(56.dp), cornerRadius = 16, title = title, subtitle = subtitle)
                    Column(Modifier.weight(1f)) {
                        VantafynMarqueeText(title, MaterialTheme.typography.titleMedium.copy(color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold))
                        subtitle?.let { VantafynMarqueeText(it, MaterialTheme.typography.bodyMedium.copy(color = VantafynColors.Muted)) }
                    }
                }
                MusicMenuAction(Icons.Rounded.PlayArrow, "Play Now") {
                    onPlay()
                    onDismiss()
                }
                onStartRadio?.let { action ->
                    MusicMenuAction(Icons.Rounded.Radio, "Start Station Radio") {
                        action()
                        onDismiss()
                    }
                }
                MusicMenuAction(Icons.Rounded.NavigateNext, "Play Next") {
                    onPlayNext()
                    onDismiss()
                }
                MusicMenuAction(Icons.Rounded.QueueMusic, "Add to Queue") {
                    onAddToQueue()
                    onDismiss()
                }
                onToggleFavorite?.let { action ->
                    MusicMenuAction(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isFavorite) "Remove Favorite" else "Add to Favorites") {
                        action()
                        onDismiss()
                    }
                }
                onAddToPlaylist?.let { action ->
                    MusicMenuAction(Icons.Rounded.PlaylistAdd, "Add to Playlist") {
                        action()
                        onDismiss()
                    }
                }
                onDownload?.let { action ->
                    MusicMenuAction(Icons.Rounded.Download, "Download Offline") {
                        action()
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerSheet(
    visible: Boolean,
    playback: VantafynMusicPlaybackState,
    onDismiss: () -> Unit,
    onSetDuration: (Int) -> Unit,
    onSetEndOfTrack: () -> Unit,
    onSetEndOfQueue: () -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    val density = LocalDensity.current
    val extraOffsetPx = remember(density) {
        with(density) { (MusicBottomSheetRailClearance + 56.dp).roundToPx() }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            VantafynGlassModalPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = MusicBottomSheetRailClearance)
                    .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.5.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight + extraOffsetPx },
                            animationSpec = spring(
                                dampingRatio = 0.84f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight + extraOffsetPx },
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f),
                            ),
                        ),
                    ),
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(24.dp))
                            Text("Sleep Timer", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                        FlatMusicIconButton(Icons.Rounded.Close, "Close timer", onDismiss, size = 38)
                    }

                    if (playback.sleepTimerMode != null) {
                        val statusText = when (playback.sleepTimerMode) {
                            SleepTimerMode.Duration -> {
                                val secs = playback.sleepTimerRemainingSeconds ?: 0L
                                val mins = secs / 60
                                val remSecs = secs % 60
                                "Active: ${mins}m ${remSecs}s remaining"
                            }
                            SleepTimerMode.EndOfTrack -> "Active: Stopping after current track"
                            SleepTimerMode.EndOfQueue -> "Active: Stopping at end of album/playlist"
                            null -> ""
                        }
                        VantafynGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(statusText, color = Color(0xFF21D8FF), fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = {
                                    onCancel()
                                    onDismiss()
                                }) {
                                    Text("Turn Off", color = Color(0xFFFF5252))
                                }
                            }
                        }
                    }

                    val durations = listOf(15, 30, 45, 60)
                    durations.forEach { mins ->
                        VantafynGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetDuration(mins)
                                    onDismiss()
                                },
                            variant = VantafynGlassVariant.Card,
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("$mins minutes", color = VantafynColors.Ink, fontWeight = FontWeight.Medium)
                                Icon(Icons.Rounded.Timer, contentDescription = null, tint = VantafynColors.Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    VantafynGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSetEndOfTrack()
                                onDismiss()
                            },
                        variant = VantafynGlassVariant.Card,
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("End of this track", color = VantafynColors.Ink, fontWeight = FontWeight.Medium)
                            Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = VantafynColors.Muted, modifier = Modifier.size(18.dp))
                        }
                    }

                    VantafynGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSetEndOfQueue()
                                onDismiss()
                            },
                        variant = VantafynGlassVariant.Card,
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("End of album / playlist", color = VantafynColors.Ink, fontWeight = FontWeight.Medium)
                            Icon(Icons.Rounded.Stop, contentDescription = null, tint = VantafynColors.Muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicSaveQueueDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        modifier = Modifier
            .imePadding()
            .vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onSave(title)
                    onDismiss()
                }
            }) {
                Text("Save", color = Color(0xFF21D8FF), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VantafynColors.Muted)
            }
        },
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Save Queue as Playlist", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter a name for the new playlist:", color = VantafynColors.Muted)
                VantafynTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Playlist Name",
                    placeholder = "e.g. Chill Session, Road Trip",
                )
            }
        },
    )
}

@Composable
private fun MusicQueueSheet(
    state: MusicUiState,
    onDismiss: () -> Unit,
    onTrack: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onSaveQueue: () -> Unit,
    onClearUpcoming: () -> Unit,
    onClearAll: () -> Unit,
    onReorder: ((Int, Int) -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        VantafynGlassModalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = MusicBottomSheetRailClearance)
                .clickable(enabled = false) {},
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Queue", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    FlatMusicIconButton(Icons.Rounded.Close, "Close queue", onDismiss, size = 40)
                }
                QueuePanel(
                    queue = state.playback.queue,
                    index = state.playback.queueIndex,
                    onTrack = onTrack,
                    onRemove = onRemove,
                    onReorder = onReorder,
                    onLongPress = onLongPress,
                    onSaveQueue = onSaveQueue,
                    onClearUpcoming = onClearUpcoming,
                    onClearAll = onClearAll,
                )
            }
        }
    }
}

@Composable
private fun LyricsPanel(state: MusicUiState) {
    val lyrics = state.lyrics
    VantafynGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Lyrics", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            when {
                state.isLyricsLoading -> Text("Loading lyrics", color = VantafynColors.Muted)
                lyrics == null -> Text("No lyrics available", color = VantafynColors.Muted)
                lyrics.isSynced -> {
                    val active = lyrics.syncedLines.activeIndex(state.playback.positionMs)
                    lyrics.syncedLines.drop((active - 3).coerceAtLeast(0)).take(8).forEachIndexed { index, line ->
                        val actualIndex = (active - 3).coerceAtLeast(0) + index
                        Text(
                            line.text,
                            color = if (actualIndex == active) VantafynColors.Ink else VantafynColors.Muted,
                            fontWeight = if (actualIndex == active) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
                else -> Text(lyrics.plainText, color = VantafynColors.Ink.copy(alpha = 0.86f))
            }
        }
    }
}

@Composable
private fun LyricsScreen(state: MusicUiState, viewModel: MusicViewModel) {
    val track = state.playback.currentTrack ?: return
    val context = androidx.compose.ui.platform.LocalContext.current
    val lyricsState = remember(track.id, state.lyricsTrackId, state.isLyricsLoading, state.lyrics) {
        LyricsRenderState(
            trackId = track.id,
            lyricsTrackId = state.lyricsTrackId,
            lyrics = if (state.lyricsTrackId == track.id) state.lyrics else null,
            isLoading = state.isLyricsLoading || (state.lyricsTrackId != track.id),
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(VantafynColors.Graphite),
    ) {
        MusicReactiveBackground(track)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.16f), VantafynColors.Graphite.copy(alpha = 0.92f)))),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Lyrics", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        if (lyricsState.lyrics?.source?.contains("Offline") == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VantafynColors.SurfaceHigh.copy(alpha = 0.8f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VantafynColors.Primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                    Text(track.title, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FlatMusicIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track",
                        onClick = viewModel::previous,
                        size = 36,
                    )
                    FlatMusicIconButton(
                        icon = if (state.playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.playback.isPlaying) "Pause" else "Play",
                        onClick = viewModel::togglePlayPause,
                        size = 38,
                    )
                    FlatMusicIconButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        onClick = viewModel::next,
                        size = 36,
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    FlatMusicIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close lyrics",
                        onClick = viewModel::closeLyrics,
                        size = 38,
                    )
                }
            }
            AnimatedContent(
                targetState = lyricsState,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 260, delayMillis = 70, easing = FastOutSlowInEasing),
                    ) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    )
                },
                modifier = Modifier.weight(1f),
                label = "lyricsTrackCrossfade",
            ) { renderState ->
                LyricsBody(
                    renderState = renderState,
                    playbackMs = state.playback.positionMs,
                    isPlaying = state.playback.isPlaying,
                    currentPositionMs = viewModel::currentPlaybackPositionMs,
                    onSeek = viewModel::seekTo,
                )
            }
        }
    }
}

private data class LyricsRenderState(
    val trackId: java.util.UUID,
    val lyricsTrackId: java.util.UUID?,
    val lyrics: JellyfinLyrics?,
    val isLoading: Boolean,
)

@Composable
private fun LyricsBody(
    renderState: LyricsRenderState,
    playbackMs: Long,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    onSeek: (Long) -> Unit,
) {
    when {
        renderState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VantafynLoadingIndicator("Loading lyrics")
            }
        }
        renderState.lyrics == null -> {
            LyricsEmptyState("No lyrics available", "No synchronized or plain lyrics were found for this track.")
        }
        renderState.lyrics.isSynced -> {
            SyncedLyricsView(
                trackId = renderState.trackId,
                lines = renderState.lyrics.syncedLines,
                playbackMs = playbackMs,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                onSeek = onSeek,
                modifier = Modifier.fillMaxSize(),
            )
        }
        else -> {
            PlainLyricsView(
                text = renderState.lyrics.plainText,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SyncedLyricsView(
    trackId: java.util.UUID,
    lines: List<JellyfinLyricLine>,
    playbackMs: Long,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    key(trackId, lines) {
        SyncedLyricsList(
            trackId = trackId,
            lines = lines,
            playbackMs = playbackMs,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            onSeek = onSeek,
            modifier = modifier,
        )
    }
}

@Composable
private fun SyncedLyricsList(
    trackId: java.util.UUID,
    lines: List<JellyfinLyricLine>,
    playbackMs: Long,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeLineLeadMs = 120L
    var livePlaybackMs by remember(trackId, lines) { mutableLongStateOf(playbackMs) }
    val activeIndex = remember(lines, livePlaybackMs) { lines.activeIndex(livePlaybackMs + activeLineLeadMs) }
    var suppressAutoFollowUntil by remember(trackId, lines) { mutableLongStateOf(0L) }
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            suppressAutoFollowUntil = System.currentTimeMillis() + 5_000L
        }
    }

    LaunchedEffect(playbackMs, isPlaying, trackId, lines) {
        if (!isPlaying || abs(playbackMs - livePlaybackMs) > 1_200L) {
            livePlaybackMs = playbackMs
        }
    }

    LaunchedEffect(trackId, lines, isPlaying, lifecycleOwner) {
        if (lines.isEmpty() || !isPlaying) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                livePlaybackMs = currentPositionMs()
                delay(SyncedLyricsTickerIntervalMs)
            }
        }
    }

    LaunchedEffect(trackId, lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        suppressAutoFollowUntil = 0L
        val initialPos = currentPositionMs()
        livePlaybackMs = initialPos
        val targetIndex = lines.activeIndex(initialPos + activeLineLeadMs).coerceAtLeast(0)
        listState.scrollToItem(targetIndex.coerceAtMost(lines.lastIndex))
    }

    LaunchedEffect(activeIndex, lines) {
        if (lines.isEmpty() || activeIndex < 0) return@LaunchedEffect
        val isSuppressed = System.currentTimeMillis() < suppressAutoFollowUntil
        if (!isSuppressed) {
            val target = activeIndex.coerceIn(0, lines.lastIndex)
            if (target == 0) {
                listState.scrollToItem(0)
            } else {
                listState.animateScrollToItem(target)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = maxHeight * 0.42f, bottom = maxHeight * 0.48f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.startMs}-${line.text}-$index" }) { index, line ->
                SyncedLyricLine(
                    line = line,
                    active = index == activeIndex,
                    onClick = {
                        line.startMs?.let {
                            suppressAutoFollowUntil = 0L
                            onSeek(it)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SyncedLyricLine(line: JellyfinLyricLine, active: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1f else 0.92f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "lyricLineScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.42f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "lyricLineAlpha",
    )
    Text(
        text = line.text.trim().ifBlank { "♪" },
        textAlign = TextAlign.Start,
        color = Color.White.copy(alpha = alpha),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            }
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = line.startMs != null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 3.dp),
    )
}

@Composable
private fun PlainLyricsView(text: String, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val lines = remember(text) {
        text.trim()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    if (lines.isEmpty()) {
        LyricsEmptyState("No lyrics text", "Jellyfin returned an empty lyrics file.")
        return
    }
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        state = listState,
        contentPadding = PaddingValues(top = 74.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(lines, key = { index, line -> "$index-$line" }) { _, line ->
            Text(
                line,
                color = Color.White.copy(alpha = 0.60f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun LyricsEmptyState(title: String, subtitle: String) {
    VantafynGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.Article, contentDescription = null, tint = VantafynColors.Muted, modifier = Modifier.size(30.dp))
            Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(subtitle, color = VantafynColors.Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MusicSuccessToast(message: String, modifier: Modifier = Modifier) {
    VantafynGlassModalPanel(
        modifier = modifier
            .vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.2.dp),
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF1EC878)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Text(
                text = message,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MusicPlaylistPickerSheet(
    playlists: List<JellyfinMusicPlaylist>,
    onDismiss: () -> Unit,
    onPlaylist: (JellyfinMusicPlaylist) -> Unit,
    onCreateNew: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        VantafynGlassModalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = MusicBottomSheetRailClearance)
                .vantafynAnimatedModalBorder(cornerRadius = 30.dp, strokeWidth = 1.5.dp)
                .clickable(enabled = false) {},
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Add to playlist", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = VantafynColors.Ink)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "__create_new__") {
                        MusicMenuAction(
                            icon = Icons.Rounded.Add,
                            label = "Create new playlist",
                            onClick = { onCreateNew() },
                        )
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        MusicMenuAction(
                            icon = Icons.Rounded.PlaylistAdd,
                            label = playlist.name,
                            onClick = { onPlaylist(playlist) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicTrackContextMenu(
    track: JellyfinMusicTrack,
    playlists: List<JellyfinMusicPlaylist>,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onChoosePlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onTrackDetails: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onSelectTracks: (() -> Unit)? = null,
) {
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MusicMenuAction(Icons.Rounded.PlayArrow, "Play", onPlay)
                MusicMenuAction(Icons.Rounded.NavigateNext, "Play next", onPlayNext)
                MusicMenuAction(Icons.Rounded.QueueMusic, "Add to queue", onAddToQueue)
                onSelectTracks?.let { action ->
                    MusicMenuAction(Icons.Rounded.Check, "Select tracks", action)
                }
                MusicMenuAction(Icons.Rounded.Download, "Save offline", onDownload)
                if (playlists.isNotEmpty()) {
                    MusicMenuAction(Icons.Rounded.PlaylistAdd, "Add to playlist", onChoosePlaylist)
                }
                if (track.albumId != null) MusicMenuAction(Icons.Rounded.Album, "Go to album", onGoToAlbum)
                MusicMenuAction(Icons.Rounded.Info, "View track details", onTrackDetails)
                onRemoveFromPlaylist?.let { action ->
                    VantafynGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = action),
                        variant = VantafynGlassVariant.Card,
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = VantafynColors.Destructive)
                            Text("Remove from playlist", color = VantafynColors.Destructive, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun MusicMenuAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    VantafynGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        variant = VantafynGlassVariant.Card,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = VantafynColors.Ink)
            Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueTrackRow(
    track: VantafynMusicTrack,
    current: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (current) Color.White.copy(alpha = 0.12f) else Color(0xFF16161C))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        MusicArt(track.artworkUrl, Modifier.size(46.dp), cornerRadius = 13, title = track.title, subtitle = track.artist)
        if (current) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(VantafynGradients.accentHorizontal()),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = VantafynColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal)
            Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(track.durationMs?.formatTime().orEmpty(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodySmall)
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleQueueTrackRow(
    track: VantafynMusicTrack,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    var isRemoved by remember(track.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var hasVibratedThreshold by remember(track.id) { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                isRemoved = true
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.38f },
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            if (!hasVibratedThreshold) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                hasVibratedThreshold = true
            }
        } else {
            hasVibratedThreshold = false
        }
    }

    LaunchedEffect(isRemoved) {
        if (isRemoved) {
            delay(280L)
            onRemove()
        }
    }

    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(animationSpec = tween(180)),
        modifier = modifier,
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                val offset = kotlin.math.abs(runCatching { dismissState.requireOffset() }.getOrDefault(0f))
                val progress = (offset / 180f).coerceIn(0f, 1f)
                val alpha = (progress * 0.95f).coerceIn(0f, 0.95f)
                val iconScale by animateFloatAsState(
                    targetValue = if (progress > 0.45f) 1.15f else (0.65f + 0.35f * progress),
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "trashIconScale",
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFFE53935).copy(alpha = alpha * 0.35f),
                                    Color(0xFFD32F2F).copy(alpha = alpha),
                                )
                            )
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remove",
                        tint = Color.White.copy(alpha = (progress * 1.4f).coerceIn(0f, 1f)),
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                    )
                }
            },
        ) {
            QueueTrackRow(
                track = track,
                current = false,
                onClick = onClick,
                onLongClick = onLongClick,
                trailingContent = trailingContent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueuePanel(
    queue: List<VantafynMusicTrack>,
    index: Int,
    onTrack: (Int) -> Unit,
    onRemove: ((Int) -> Unit)? = null,
    onReorder: ((Int, Int) -> Unit)? = null,
    onLongPress: ((Int) -> Unit)? = null,
    onSaveQueue: (() -> Unit)? = null,
    onClearUpcoming: (() -> Unit)? = null,
    onClearAll: (() -> Unit)? = null,
) {
    val currentTrack = queue.getOrNull(index)
    val upcomingInitial = remember(queue, index) {
        if (queue.size > index + 1) queue.drop(index + 1).take(15) else emptyList()
    }
    var localUpcoming by remember(upcomingInitial) { mutableStateOf(upcomingInitial) }
    var draggedOffset by remember { mutableStateOf<Int?>(null) }
    var dragStartOffset by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val spacingPx = with(density) { 10.dp.toPx() }

    LaunchedEffect(upcomingInitial) {
        if (draggedOffset == null) {
            localUpcoming = upcomingInitial
        }
    }

    VantafynGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Up next", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onSaveQueue != null && queue.isNotEmpty()) {
                        FlatMusicIconButton(Icons.Rounded.PlaylistAdd, "Save queue as playlist", onSaveQueue, size = 34)
                    }
                    if (onClearUpcoming != null && queue.size > index + 1) {
                        FlatMusicIconButton(Icons.Rounded.ClearAll, "Clear upcoming queue", onClearUpcoming, size = 34)
                    }
                    if (onClearAll != null && queue.isNotEmpty()) {
                        FlatMusicIconButton(Icons.Rounded.DeleteOutline, "Clear all queue", onClearAll, size = 34)
                    }
                }
            }

            if (currentTrack != null) {
                QueueTrackRow(
                    track = currentTrack,
                    current = true,
                    onClick = { onTrack(index) },
                    onLongClick = { onLongPress?.invoke(index) },
                )
            }

            localUpcoming.forEachIndexed { upcomingOffset, track ->
                key(track.id) {
                    val absoluteIndex = (index + 1) + upcomingOffset
                    val isItemDragged = draggedOffset == upcomingOffset

                    val dragHandle: (@Composable () -> Unit)? = if (onReorder != null && localUpcoming.size > 1) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .pointerInput(track.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedOffset = upcomingOffset
                                                dragStartOffset = upcomingOffset
                                                dragOffsetY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val curr = draggedOffset ?: return@detectDragGestures
                                                dragOffsetY += dragAmount.y
                                                val step = if (itemHeightPx > 0f) itemHeightPx + spacingPx else with(density) { 68.dp.toPx() }

                                                if (dragOffsetY > step * 0.5f && curr < localUpcoming.lastIndex) {
                                                    val next = curr + 1
                                                    val list = localUpcoming.toMutableList()
                                                    val item = list.removeAt(curr)
                                                    list.add(next, item)
                                                    localUpcoming = list
                                                    draggedOffset = next
                                                    dragOffsetY -= step
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                } else if (dragOffsetY < -step * 0.5f && curr > 0) {
                                                    val prev = curr - 1
                                                    val list = localUpcoming.toMutableList()
                                                    val item = list.removeAt(curr)
                                                    list.add(prev, item)
                                                    localUpcoming = list
                                                    draggedOffset = prev
                                                    dragOffsetY += step
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            },
                                            onDragEnd = {
                                                val start = dragStartOffset
                                                val finish = draggedOffset
                                                draggedOffset = null
                                                dragStartOffset = null
                                                dragOffsetY = 0f
                                                if (start != null && finish != null && start != finish) {
                                                    onReorder((index + 1) + start, (index + 1) + finish)
                                                }
                                            },
                                            onDragCancel = {
                                                draggedOffset = null
                                                dragStartOffset = null
                                                dragOffsetY = 0f
                                                localUpcoming = upcomingInitial
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = VantafynColors.Muted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    } else null

                    val rowModifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            if (coords.size.height > 0) itemHeightPx = coords.size.height.toFloat()
                        }
                        .zIndex(if (isItemDragged) 10f else 1f)
                        .graphicsLayer {
                            if (isItemDragged) {
                                translationY = dragOffsetY
                                scaleX = 1.025f
                                scaleY = 1.025f
                                shadowElevation = 16f
                            } else {
                                translationY = 0f
                                scaleX = 1f
                                scaleY = 1f
                                shadowElevation = 0f
                            }
                        }

                    if (onRemove != null) {
                        DismissibleQueueTrackRow(
                            track = track,
                            onRemove = { onRemove(absoluteIndex) },
                            onClick = { onTrack(absoluteIndex) },
                            onLongClick = { onLongPress?.invoke(absoluteIndex) },
                            modifier = rowModifier,
                            trailingContent = dragHandle,
                        )
                    } else {
                        QueueTrackRow(
                            track = track,
                            current = false,
                            onClick = { onTrack(absoluteIndex) },
                            onLongClick = { onLongPress?.invoke(absoluteIndex) },
                            trailingContent = dragHandle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackContextMenu(
    track: VantafynMusicTrack,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    onGoToAlbum: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynModalContainerColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MusicMenuAction(Icons.Rounded.NavigateNext, "Play next") {
                    onPlayNext()
                    onDismiss()
                }
                MusicMenuAction(Icons.Rounded.Close, "Remove from queue") {
                    onRemoveFromQueue()
                    onDismiss()
                }
                if (track.albumId != null) {
                    MusicMenuAction(Icons.Rounded.Album, "Go to album") {
                        onGoToAlbum()
                        onDismiss()
                    }
                }
            }
        },
    )
}

private data class CoverPreset(
    val bgStart: Color,
    val bgMid: Color,
    val bgEnd: Color,
    val accent: Color,
    val secondary: Color,
)

private val PREMIUM_COVER_PRESETS = listOf(
    // 0: Deep Celestial Cyan
    CoverPreset(Color(0xFF030A18), Color(0xFF07213D), Color(0xFF0C3860), Color(0xFF00E5FF), Color(0xFF38BDF8)),
    // 1: Neon Violet & Fuchsia
    CoverPreset(Color(0xFF140521), Color(0xFF2B0A4C), Color(0xFF4C1D95), Color(0xFFFF2E93), Color(0xFFA855F7)),
    // 2: Electric Sunset Amber
    CoverPreset(Color(0xFF1A0A02), Color(0xFF3B1506), Color(0xFF7C2D12), Color(0xFFFF7A00), Color(0xFFFBBF24)),
    // 3: Emerald Borealis Teal
    CoverPreset(Color(0xFF021612), Color(0xFF053127), Color(0xFF0F5142), Color(0xFF10B981), Color(0xFF34D399)),
    // 4: Royal Ultramarine
    CoverPreset(Color(0xFF080C26), Color(0xFF1B184E), Color(0xFF312E81), Color(0xFF6366F1), Color(0xFF818CF8)),
    // 5: Crimson Pulse
    CoverPreset(Color(0xFF1E040B), Color(0xFF3F0717), Color(0xFF881337), Color(0xFFFF3366), Color(0xFFFB7185)),
    // 6: Cyber Coral & Lavender
    CoverPreset(Color(0xFF16061E), Color(0xFF340C37), Color(0xFF581C87), Color(0xFFFF6492), Color(0xFFC084FC)),
    // 7: Deep Obsidian Gold
    CoverPreset(Color(0xFF120E03), Color(0xFF2C1C05), Color(0xFF452E07), Color(0xFFF59E0B), Color(0xFFFDE047)),
)

private fun resolveCoverPreset(seedText: String?): CoverPreset {
    val hash = (seedText?.trim()?.lowercase() ?: "vantafyn").hashCode()
    val index = (hash and Int.MAX_VALUE) % PREMIUM_COVER_PRESETS.size
    return PREMIUM_COVER_PRESETS[index]
}

private fun extractCoverMonogram(title: String?): String {
    if (title.isNullOrBlank()) return "♪"
    val cleaned = title.trim()
    val words = cleaned.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> {
            val first = words[0].firstOrNull()?.uppercase() ?: ""
            val second = words[1].firstOrNull()?.uppercase() ?: ""
            "$first$second"
        }
        cleaned.length >= 2 -> cleaned.take(2).uppercase()
        else -> cleaned.take(1).uppercase()
    }
}

@Composable
private fun PremiumCoverPlaceholder(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    cornerRadius: Int = 22,
    compact: Boolean = false,
) {
    val preset = remember(title, subtitle) {
        resolveCoverPreset(title ?: subtitle)
    }
    val isArtist = remember(subtitle) {
        subtitle?.equals("Artist", ignoreCase = true) == true
    }
    val monogram = remember(title) {
        extractCoverMonogram(title)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(preset.bgStart, preset.bgMid, preset.bgEnd),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = kotlin.math.min(size.width, size.height) * 0.48f

            // Concentric vinyl grooves
            val grooveFractions = if (compact) {
                floatArrayOf(0.88f, 0.65f, 0.45f)
            } else {
                floatArrayOf(0.92f, 0.83f, 0.74f, 0.65f, 0.56f, 0.47f, 0.38f)
            }
            for (f in grooveFractions) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.065f),
                    radius = maxRadius * f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            // Specular sheen light sweep
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
                radius = maxRadius * 0.96f,
                center = center,
            )

            // Outer vinyl groove edge accent
            drawCircle(
                color = preset.accent.copy(alpha = 0.24f),
                radius = maxRadius * 0.96f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )

            // Subtle outer border highlight
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.20f),
                        preset.accent.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    cornerRadius.dp.toPx(),
                    cornerRadius.dp.toPx(),
                ),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Center vinyl label / artist medallion
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val minDim = kotlin.math.min(maxWidth.value, maxHeight.value)
            val labelSize = (minDim * if (compact) 0.58f else 0.44f).dp

            Box(
                modifier = Modifier
                    .size(labelSize)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                preset.bgMid.copy(alpha = 0.95f),
                                preset.bgStart.copy(alpha = 0.98f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                preset.accent.copy(alpha = 0.90f),
                                preset.secondary.copy(alpha = 0.55f),
                            ),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Vinyl center spindle hole
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spindleRadius = size.minDimension * 0.12f
                    drawCircle(
                        color = preset.bgStart,
                        radius = spindleRadius,
                        center = center,
                    )
                    drawCircle(
                        color = preset.accent.copy(alpha = 0.75f),
                        radius = spindleRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }

                if (isArtist) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = preset.accent,
                        modifier = Modifier.size(labelSize * 0.48f),
                    )
                } else if (!compact && minDim >= 70) {
                    Text(
                        text = monogram,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontSize = (minDim * 0.14f).sp,
                        maxLines = 1,
                    )
                } else {
                    Text(
                        text = monogram,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = (minDim * 0.18f).coerceAtLeast(10f).sp,
                        maxLines = 1,
                    )
                }
            }

            // High-fidelity footer pill on larger cards (e.g. 128dp album cards)
            if (!compact && minDim >= 96) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isArtist) Icons.Rounded.Person else Icons.Rounded.Album,
                            contentDescription = null,
                            tint = preset.accent,
                            modifier = Modifier.size(9.dp),
                        )
                        Text(
                            text = if (isArtist) "ARTIST" else "VINYL",
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistArtGrid(trackImageUrls: List<String>, modifier: Modifier = Modifier) {
    val urls = trackImageUrls.take(4)
    val fallback = urls.firstOrNull()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.66f)),
    ) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 2) {
                Row(Modifier.weight(1f)) {
                    for (col in 0 until 2) {
                        val idx = row * 2 + col
                        val url = urls.getOrNull(idx) ?: fallback
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!url.isNullOrBlank()) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                PremiumCoverPlaceholder(
                                    title = "${idx + 1}",
                                    subtitle = "Track",
                                    cornerRadius = 6,
                                    compact = true,
                                    modifier = Modifier.fillMaxSize(),
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
private fun MusicArt(
    imageUrl: String?,
    modifier: Modifier,
    cornerRadius: Int = 22,
    title: String? = null,
    subtitle: String? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasError by remember(imageUrl) { mutableStateOf(false) }
    val showPlaceholder = imageUrl.isNullOrBlank() || hasError

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color(0xFF141416)),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            PremiumCoverPlaceholder(
                title = title,
                subtitle = subtitle,
                cornerRadius = cornerRadius,
                compact = cornerRadius <= 14,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = remember(imageUrl) {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(300)
                        .build()
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { hasError = true },
            )
        }
    }
}

@Composable
private fun MiniControl(label: String, onClick: () -> Unit) {
    VantafynGlassSurface(
        modifier = Modifier
            .clickable(onClick = onClick),
        variant = VantafynGlassVariant.Chip,
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = VantafynColors.Ink, maxLines = 1)
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, size: Int = 42, selected: Boolean = false) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size.dp)
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp))
                    .background(VantafynGradients.accentHorizontal())
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = contentDescription, tint = VantafynColors.Ink, modifier = Modifier.size((size * 0.48f).dp))
            }
        } else {
            VantafynGlassSurface(
                modifier = Modifier.fillMaxSize(),
                variant = VantafynGlassVariant.Chip,
                cornerRadius = 999.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = contentDescription, tint = VantafynColors.Ink)
                }
            }
        }
    }
}

@Composable
private fun IconPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    VantafynGlassSurface(
        modifier = Modifier
            .clickable(onClick = onClick),
        variant = VantafynGlassVariant.Chip,
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = VantafynColors.Ink, modifier = Modifier.size(18.dp))
            Text(label, color = VantafynColors.Ink, maxLines = 1)
        }
    }
}

@Composable
private fun MusicScrubber(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean = false,
    onSeek: (Long) -> Unit,
) {
    val duration = durationMs.toFloat().coerceAtLeast(1f)
    val value = positionMs.toFloat().coerceIn(0f, duration)
    val context = androidx.compose.ui.platform.LocalContext.current
    val squigglyEnabled by dev.vantafyn.core.media.music.SquigglyProgressPreferences.enabledFlow
        .collectAsStateWithLifecycle(initialValue = dev.vantafyn.core.media.music.SquigglyProgressPreferences.isEnabled(context))

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    val displayValue = if (isScrubbing) scrubValue else value
    val progress = (displayValue / duration).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxWidth()) {
        if (squigglyEnabled) {
            dev.vantafyn.core.ui.VantafynWavyProgressBar(
                progress = progress,
                isPlaying = isPlaying,
                isScrubbing = isScrubbing,
                waveHeight = 8.dp,
                wavelength = 28.dp,
                strokeWidth = 5.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxSize()
                        .background(VantafynGradients.accentHorizontal()),
                )
            }
        }
        Slider(
            value = displayValue,
            onValueChange = {
                isScrubbing = true
                scrubValue = it
            },
            onValueChangeFinished = {
                onSeek(scrubValue.toLong())
                isScrubbing = false
            },
            valueRange = 0f..duration,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .alpha(0.94f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF31D7FF),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

private fun progressFraction(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

@Composable
private fun MusicContentReveal(
    index: Int,
    animate: Boolean,
    revealKey: Any? = index,
    content: @Composable () -> Unit,
) {
    var progress by remember(revealKey, index, animate) { mutableFloatStateOf(if (animate) 0f else 1f) }
    LaunchedEffect(revealKey, index, animate) {
        if (animate) {
            progress = 0f
            delay((index.coerceAtMost(8) * 112L).coerceAtMost(780L))
            Animatable(0f).animateTo(1f, animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)) {
                progress = value
            }
        } else {
            progress = 1f
        }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * size.height / 8f
        },
    ) {
        content()
    }
}

private fun JellyfinMusicTrack.toDetails(): MusicTrackDetails =
    MusicTrackDetails(
        id = id.toString(),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        hasLyrics = hasLyrics,
        isFavorite = isFavorite,
    )

private fun VantafynMusicTrack.toDetails(): MusicTrackDetails =
    MusicTrackDetails(
        id = id.toString(),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        hasLyrics = null,
        isFavorite = isFavorite,
    )

private fun Long.formatTime(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun Int.groupedMusicCountLabel(): String = "%,d".format(this)

@Composable
private fun MusicOfflineEmptyState(
    onNavigateToDownloads: (() -> Unit)?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VantafynGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF42D7FF).copy(alpha = 0.16f),
                            Color(0xFF9B5CFF).copy(alpha = 0.07f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = size.width * 0.65f,
                    ),
                )
            },
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.04f),
                                Color(0xFF0B1020).copy(alpha = 0.38f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.26f),
                                Color(0xFF42D7FF).copy(alpha = 0.38f),
                                Color(0xFFA65CFF).copy(alpha = 0.24f),
                            ),
                        ),
                        shape = RoundedCornerShape(32.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFF42D7FF),
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(VantafynColors.Secondary.copy(alpha = 0.12f))
                    .border(1.dp, VantafynColors.Secondary.copy(alpha = 0.28f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(VantafynColors.Secondary),
                    )
                    Text(
                        text = "OFFLINE MODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = VantafynColors.Secondary,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Listening Offline",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Your music server is currently disconnected. All your downloaded tracks, albums, and playlists remain ready for instant playback in your downloads.",
                    color = VantafynColors.Muted.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.widthIn(max = 340.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                OfflineFeaturePill(
                    icon = Icons.Rounded.Bolt,
                    label = "Zero Buffering",
                    modifier = Modifier.weight(1f),
                )
                OfflineFeaturePill(
                    icon = Icons.Rounded.GraphicEq,
                    label = "Full Fidelity",
                    modifier = Modifier.weight(1f),
                )
                OfflineFeaturePill(
                    icon = Icons.Rounded.DownloadDone,
                    label = "Cached Library",
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onNavigateToDownloads != null) {
                    VantafynButton(
                        text = "Open Downloads Library",
                        onClick = onNavigateToDownloads,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                VantafynGlassChip(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = VantafynColors.Ink,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry Server Connection",
                            style = MaterialTheme.typography.labelLarge,
                            color = VantafynColors.Ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineFeaturePill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    VantafynGlassSurface(
        modifier = modifier,
        cornerRadius = 14.dp,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        variant = VantafynGlassVariant.Card,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VantafynColors.Secondary.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = VantafynColors.Ink.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MusicPlaylistDuplicateDialog(
    prompt: PlaylistDuplicatePrompt,
    onConfirm: (List<JellyfinMusicTrack>) -> Unit,
    onDismiss: () -> Unit,
) {
    val isSingle = prompt.candidateTracks.size == 1 && prompt.duplicateTracks.size == 1
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 28.dp),
        onDismissRequest = onDismiss,
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (isSingle) "Song already in playlist" else "Duplicate songs found",
                color = VantafynColors.Ink,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val message = if (isSingle) {
                    "\"${prompt.duplicateTracks.first().title}\" is already in \"${prompt.playlist.name}\". Would you like to add it again?"
                } else {
                    "${prompt.duplicateTracks.size} of ${prompt.candidateTracks.size} songs are already in \"${prompt.playlist.name}\". How would you like to proceed?"
                }
                Text(message, color = VantafynColors.Ink.copy(alpha = 0.85f))
            }
        },
        confirmButton = {
            if (isSingle) {
                TextButton(onClick = { onConfirm(prompt.candidateTracks) }) {
                    Text("Add Anyway", color = VantafynColors.Primary, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (prompt.newTracks.isNotEmpty()) {
                        TextButton(onClick = { onConfirm(prompt.newTracks) }) {
                            Text("Skip Duplicates (${prompt.newTracks.size})", color = VantafynColors.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { onConfirm(prompt.candidateTracks) }) {
                        Text("Add All (${prompt.candidateTracks.size})", color = VantafynColors.Ink)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VantafynColors.Muted)
            }
        },
    )
}

@Composable
private fun MusicMultiSelectActionBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onQueue: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VantafynGlassDock(
        modifier = modifier
            .fillMaxWidth()
            .vantafynAnimatedModalBorder(cornerRadius = 28.dp, strokeWidth = 1.3.dp, durationMillis = 4200),
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel selection",
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = "$selectedCount selected",
                    color = VantafynColors.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isAllSelected) VantafynColors.Primary.copy(alpha = 0.18f)
                            else Color.White.copy(alpha = 0.08f),
                        )
                        .border(
                            0.8.dp,
                            if (isAllSelected) VantafynColors.Primary.copy(alpha = 0.45f)
                            else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(999.dp),
                        )
                        .clickable(onClick = onToggleSelectAll)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isAllSelected) "Deselect" else "Select All",
                        color = if (isAllSelected) VantafynColors.Primary else VantafynColors.Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable(onClick = onAddToPlaylist),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = VantafynColors.Primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable(onClick = onQueue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = "Add to queue",
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (onRemoveFromPlaylist != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A1215).copy(alpha = 0.65f))
                            .border(0.8.dp, VantafynColors.Destructive.copy(alpha = 0.40f), CircleShape)
                            .clickable(onClick = onRemoveFromPlaylist),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Remove from playlist",
                            tint = VantafynColors.Destructive,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}



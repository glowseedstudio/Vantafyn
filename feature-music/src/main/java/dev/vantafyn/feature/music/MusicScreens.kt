package dev.vantafyn.feature.music

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicArtist
import dev.vantafyn.core.jellyfin.JellyfinLyricLine
import dev.vantafyn.core.jellyfin.JellyfinMusicPlaylist
import dev.vantafyn.core.jellyfin.JellyfinMusicTrack
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.media.VantafynMusicRepeatMode
import dev.vantafyn.core.media.VantafynMusicTrack
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynErrorCard
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassDock
import dev.vantafyn.core.ui.VantafynGlassPanel
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun MusicScreen(
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    onRequestMusicControlsPermission: ((() -> Unit) -> Unit) = { action -> action() },
    viewModel: MusicViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var actionTrack by remember { mutableStateOf<JellyfinMusicTrack?>(null) }
    val startMusic: (() -> Unit) -> Unit = { action -> onRequestMusicControlsPermission(action) }
    val showInitialLoading = state.isLoading &&
        state.home == null &&
        state.searchResults.isEmpty() &&
        state.screen == MusicScreenState.Home
    LaunchedEffect(session?.profileId) {
        viewModel.bindSession(session)
    }
    BackHandler(enabled = state.showNowPlaying || state.screen != MusicScreenState.Home) {
        if (state.showLyricsScreen) {
            viewModel.closeLyrics()
        } else if (state.showNowPlaying) {
            viewModel.closeNowPlaying()
        } else {
            viewModel.showHome()
        }
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (state.playback.currentTrack != null) 190.dp else 118.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
            item {
                MusicHomeHeader()
            }
            state.errorMessage?.let { message ->
                item { VantafynErrorCard(message) { VantafynButton("Retry", onClick = viewModel::loadHome) } }
            }
            item {
                VantafynTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::search,
                    label = "Search music",
                    placeholder = "Songs, albums, artists",
                )
            }
            if (showInitialLoading) {
                item(key = "music-loading-skeleton") { MusicLoadingSkeleton() }
            }
            when (val screen = state.screen) {
                MusicScreenState.Home -> {
                    if (state.searchResults.isNotEmpty()) {
                        item {
                            MusicTrackList(
                                title = "Search Results",
                                tracks = state.searchResults,
                                playlists = state.home?.playlists.orEmpty(),
                                onTrack = { track -> startMusic { viewModel.playTrack(track, state.searchResults) } },
                                onAddToPlaylist = viewModel::addTrackToPlaylist,
                                onLongPress = { actionTrack = it },
                            )
                        }
                    }
                    state.home?.let { home ->
                        if (home.recentlyAdded.isNotEmpty()) item {
                            MusicTrackRow("Recently Added", home.recentlyAdded) { track -> startMusic { viewModel.playTrack(track, home.recentlyAdded) } }
                        }
                        if (home.albums.isNotEmpty()) item {
                            MusicAlbumRow(home.albums, onAlbum = viewModel::openAlbum)
                        }
                        if (home.artists.isNotEmpty()) item {
                            MusicArtistRow(home.artists, onArtist = viewModel::openArtist)
                        }
                        if (home.playlists.isNotEmpty()) item {
                            MusicPlaylistRow(home.playlists, onPlaylist = viewModel::openPlaylist)
                        }
                        if (home.songs.isNotEmpty()) item {
                            MusicSectionHeader("Songs", "View all", viewModel::showSongs)
                            MusicTrackList(
                                title = "",
                                tracks = home.songs.take(20),
                                playlists = home.playlists,
                                onTrack = { track -> startMusic { viewModel.playTrack(track, home.songs) } },
                                onAddToPlaylist = viewModel::addTrackToPlaylist,
                                onLongPress = { actionTrack = it },
                            )
                        }
                    }
                }
                is MusicScreenState.Album -> {
                    item {
                        MusicDetailHeader(screen.album.title, screen.album.artist ?: "Album", screen.album.artworkUrl, onBack = viewModel::showHome) {
                            screen.tracks.firstOrNull()?.let { track -> startMusic { viewModel.playTrack(track, screen.tracks) } }
                        }
                    }
                    item {
                        MusicTrackList(
                            title = "Tracks",
                            tracks = screen.tracks,
                            playlists = state.home?.playlists.orEmpty(),
                            onTrack = { track -> startMusic { viewModel.playTrack(track, screen.tracks) } },
                            onAddToPlaylist = viewModel::addTrackToPlaylist,
                            onLongPress = { actionTrack = it },
                        )
                    }
                }
                is MusicScreenState.Artist -> {
                    item {
                        MusicDetailHeader(screen.artist.name, "Artist", screen.artist.imageUrl, onBack = viewModel::showHome, onPlay = null)
                    }
                    item {
                        if (screen.albums.isEmpty()) {
                            Text("No albums found for this artist yet.", color = VantafynColors.Muted)
                        } else {
                            MusicAlbumRow(screen.albums, onAlbum = viewModel::openAlbum)
                        }
                    }
                }
                is MusicScreenState.Playlist -> {
                    item {
                        MusicDetailHeader(screen.playlist.name, "${screen.tracks.size} tracks", screen.playlist.imageUrl, onBack = viewModel::showHome) {
                            screen.tracks.firstOrNull()?.let { track -> startMusic { viewModel.playTrack(track, screen.tracks) } }
                        }
                    }
                    item {
                        MusicTrackList(
                            title = "Playlist",
                            tracks = screen.tracks,
                            playlists = state.home?.playlists.orEmpty(),
                            onTrack = { track -> startMusic { viewModel.playTrack(track, screen.tracks) } },
                            onAddToPlaylist = viewModel::addTrackToPlaylist,
                            onLongPress = { actionTrack = it },
                        )
                    }
                }
                is MusicScreenState.Songs -> {
                    item {
                        MusicSimpleHeader("Songs", onBack = viewModel::showHome)
                    }
                    item {
                        MusicTrackList(
                            title = "All Songs",
                            tracks = screen.tracks,
                            playlists = state.home?.playlists.orEmpty(),
                            onTrack = { track -> startMusic { viewModel.playTrack(track, screen.tracks) } },
                            onAddToPlaylist = viewModel::addTrackToPlaylist,
                            onLongPress = { actionTrack = it },
                        )
                    }
                }
            }
        }
        state.playback.currentTrack?.let {
            MusicMiniPlayer(
                track = it,
                isPlaying = state.playback.isPlaying,
                progress = progressFraction(state.playback.positionMs, state.playback.durationMs),
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 8.dp, end = 8.dp, bottom = 86.dp),
            )
        }
        if (state.showNowPlaying) {
            if (state.showLyricsScreen) {
                LyricsScreen(state = state, viewModel = viewModel)
            } else {
                NowPlayingDialog(state = state, viewModel = viewModel, onRequestMusicControlsPermission = startMusic)
            }
        }
        state.message?.let { message ->
            AlertDialog(
                modifier = Modifier.vantafynAnimatedModalBorder(),
                onDismissRequest = viewModel::clearMessage,
                confirmButton = { TextButton(onClick = viewModel::clearMessage) { Text("OK") } },
                title = { Text(message) },
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
                onAddToPlaylist = { playlist ->
                    actionTrack = null
                    viewModel.addTrackToPlaylist(track, playlist)
                },
            )
        }
    }
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
    val transition = rememberInfiniteTransition(label = "musicSkeleton")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "musicSkeletonShift",
    )
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.050f),
            Color.White.copy(alpha = 0.105f),
            Color.White.copy(alpha = 0.045f),
        ),
        start = Offset(-260f + shift * 520f, 0f),
        end = Offset(shift * 520f, 220f),
    )
}

@Composable
private fun MusicTrackRow(title: String, tracks: List<JellyfinMusicTrack>, onTrack: (JellyfinMusicTrack) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(tracks, key = { index, track -> "${track.id}-$index" }) { _, track ->
                MusicArtworkTile(
                    imageUrl = track.artworkUrl,
                    title = track.title,
                    subtitle = track.artist,
                    onClick = { onTrack(track) },
                )
            }
        }
    }
}

@Composable
private fun MusicAlbumRow(albums: List<JellyfinMusicAlbum>, onAlbum: (JellyfinMusicAlbum) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Albums", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(albums, key = { index, album -> "${album.id}-$index" }) { _, album ->
                MusicArtworkTile(album.artworkUrl, album.title, album.artist ?: "Album") { onAlbum(album) }
            }
        }
    }
}

@Composable
private fun MusicArtistRow(artists: List<JellyfinMusicArtist>, onArtist: (JellyfinMusicArtist) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Artists", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(artists, key = { index, artist -> "${artist.id}-$index" }) { _, artist ->
                MusicArtworkTile(artist.imageUrl, artist.name, "Artist") { onArtist(artist) }
            }
        }
    }
}

@Composable
private fun MusicPlaylistRow(playlists: List<JellyfinMusicPlaylist>, onPlaylist: (JellyfinMusicPlaylist) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Playlists", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(playlists, key = { index, playlist -> "${playlist.id}-$index" }) { _, playlist ->
                MusicArtworkTile(playlist.imageUrl, playlist.name, "${playlist.trackCount ?: 0} tracks") { onPlaylist(playlist) }
            }
        }
    }
}

@Composable
private fun MusicTrackList(
    title: String,
    tracks: List<JellyfinMusicTrack>,
    playlists: List<JellyfinMusicPlaylist> = emptyList(),
    onTrack: (JellyfinMusicTrack) -> Unit,
    onAddToPlaylist: (JellyfinMusicTrack, JellyfinMusicPlaylist) -> Unit = { _, _ -> },
    onLongPress: (JellyfinMusicTrack) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title.isNotBlank()) Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        tracks.forEach { track ->
            VantafynGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { onTrack(track) }, onLongClick = { onLongPress(track) }),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MusicArt(track.artworkUrl, Modifier.size(52.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = VantafynColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(track.durationMs?.formatTime().orEmpty(), color = VantafynColors.Muted)
                    playlists.firstOrNull()?.let { playlist ->
                        MiniControl("+") { onAddToPlaylist(track, playlist) }
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
        MiniControl(action, onAction)
    }
}

@Composable
private fun MusicSimpleHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        MiniControl("Back", onBack)
    }
}

@Composable
private fun MusicDetailHeader(title: String, subtitle: String, imageUrl: String?, onBack: () -> Unit, onPlay: (() -> Unit)?) {
    VantafynGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MusicSimpleHeader(title, onBack)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MusicArt(imageUrl, Modifier.size(112.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(subtitle, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (onPlay != null) VantafynButton("Play", onClick = onPlay, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun MusicArtworkTile(imageUrl: String?, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MusicArt(imageUrl, Modifier.size(128.dp))
        Text(title, color = VantafynColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MusicMiniPlayer(
    track: VantafynMusicTrack,
    isPlaying: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VantafynGlassDock(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MusicArt(track.artworkUrl, Modifier.size(50.dp), cornerRadius = 14)
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
) {
    var showPlaylistName by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    val track = state.playback.currentTrack ?: return
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.size(44.dp))
                    Text("Now Playing", color = VantafynColors.Ink.copy(alpha = 0.90f), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        FlatMusicIconButton(Icons.Rounded.MoreHoriz, "More", { showMoreSheet = true }, size = 44)
                        FlatMusicIconButton(Icons.Rounded.Close, "Close", viewModel::closeNowPlaying, size = 44)
                    }
                }
            }
            item {
                MusicArt(
                    imageUrl = track.artworkUrl,
                    modifier = Modifier
                        .size(296.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(30.dp)),
                    cornerRadius = 30,
                )
            }
            item {
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
            item {
                MusicScrubber(
                    positionMs = state.playback.positionMs,
                    durationMs = state.playback.durationMs,
                    onSeek = viewModel::seekTo,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(state.playback.positionMs.formatTime(), color = VantafynColors.Muted)
                    Text(state.playback.durationMs.formatTime(), color = VantafynColors.Muted)
                }
            }
            item {
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
            item {
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
            item {
                QueuePanel(
                    queue = state.playback.queue,
                    index = state.playback.queueIndex,
                    onTrack = viewModel::playQueueIndex,
                )
            }
            item {
                Spacer(Modifier.height(96.dp))
            }
        }
        if (showMoreSheet) {
            CurrentTrackMoreSheet(
                track = track,
                playlists = state.home?.playlists.orEmpty(),
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
                onAddToPlaylist = { playlist ->
                    showMoreSheet = false
                    viewModel.addCurrentToPlaylist(playlist)
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
            )
        }
    }
    if (showPlaylistName) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            modifier = Modifier.vantafynAnimatedModalBorder(),
            onDismissRequest = { showPlaylistName = false },
            title = { Text("Create playlist") },
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
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.base),
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.70f), Color.Transparent),
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
                    colors = listOf(palette.secondary.copy(alpha = 0.62f), Color.Transparent),
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
                        palette.base.copy(alpha = 0.34f),
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
            .basicMarquee(
                iterations = Int.MAX_VALUE,
                initialDelayMillis = 900,
                repeatDelayMillis = 4200,
                velocity = 28.dp,
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

@Composable
private fun CurrentTrackMoreSheet(
    track: VantafynMusicTrack,
    playlists: List<JellyfinMusicPlaylist>,
    onDismiss: () -> Unit,
    onNewPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (JellyfinMusicPlaylist) -> Unit,
    canGoToArtist: Boolean,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onFavorite: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        VantafynGlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .clickable(enabled = false) {},
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MusicArt(track.artworkUrl, Modifier.size(58.dp), cornerRadius = 16)
                    Column(Modifier.weight(1f)) {
                        VantafynMarqueeText(track.title, MaterialTheme.typography.titleMedium.copy(color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold))
                        VantafynMarqueeText(track.artist, MaterialTheme.typography.bodyMedium.copy(color = VantafynColors.Muted))
                    }
                }
                MusicMenuAction(
                    if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (track.isFavorite) "Remove from My List" else "Add to My List",
                    onFavorite,
                )
                MusicMenuAction(Icons.Rounded.NavigateNext, "Play next", onPlayNext)
                MusicMenuAction(Icons.Rounded.QueueMusic, "Add to queue", onAddToQueue)
                MusicMenuAction(Icons.Rounded.PlaylistAdd, "New playlist", onNewPlaylist)
                playlists.firstOrNull()?.let { playlist ->
                    MusicMenuAction(Icons.Rounded.Add, "Add to ${playlist.name.take(24)}") { onAddToPlaylist(playlist) }
                }
                if (track.albumId != null) MusicMenuAction(Icons.Rounded.Album, "Go to album", onGoToAlbum)
                if (canGoToArtist) MusicMenuAction(Icons.Rounded.LibraryMusic, "Go to artist", onGoToArtist)
                MusicMenuAction(Icons.Rounded.Info, "View track details", onDismiss)
            }
        }
    }
}

@Composable
private fun MusicQueueSheet(
    state: MusicUiState,
    onDismiss: () -> Unit,
    onTrack: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        VantafynGlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .clickable(enabled = false) {},
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Queue", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    FlatMusicIconButton(Icons.Rounded.Close, "Close queue", onDismiss, size = 40)
                }
                QueuePanel(queue = state.playback.queue, index = state.playback.queueIndex, onTrack = onTrack)
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
                    Text("Lyrics", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                    Text(track.title, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FlatMusicIconButton(Icons.Rounded.Close, "Close lyrics", viewModel::closeLyrics)
            }
            when {
                state.isLyricsLoading -> VantafynLoadingIndicator("Loading lyrics")
                state.lyrics == null -> {
                    LyricsEmptyState("No lyrics available", "Jellyfin did not expose lyrics for this track.")
                }
                state.lyrics.isSynced -> {
                    SyncedLyricsView(
                        lines = state.lyrics.syncedLines,
                        playbackMs = state.playback.positionMs,
                        onSeek = viewModel::seekTo,
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    PlainLyricsView(
                        text = state.lyrics.plainText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lines: List<JellyfinLyricLine>,
    playbackMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val activeLineLeadMs = 120L
    val activeIndex = remember(lines, playbackMs) { lines.activeIndex(playbackMs + activeLineLeadMs).coerceAtLeast(0) }
    var suppressAutoFollowUntil by remember(lines) { mutableStateOf(0L) }
    var programmaticScroll by remember(lines) { mutableStateOf(false) }
    var lastPlaybackMs by remember(lines) { mutableStateOf(-1L) }
    var pendingRewindTicks by remember(lines) { mutableStateOf(0) }

    LaunchedEffect(listState, lines) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling && !programmaticScroll) {
                suppressAutoFollowUntil = System.currentTimeMillis() + 5_000L
            }
        }
    }

    LaunchedEffect(activeIndex, playbackMs, lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        val rewindDrop = if (lastPlaybackMs >= 0L) lastPlaybackMs - playbackMs else 0L
        val rewindCandidate = lastPlaybackMs >= 0L && playbackMs + 350L < lastPlaybackMs
        val rewound = when {
            !rewindCandidate -> {
                pendingRewindTicks = 0
                false
            }
            rewindDrop >= 1_400L -> {
                pendingRewindTicks = 0
                true
            }
            else -> {
                pendingRewindTicks += 1
                if (pendingRewindTicks >= 2) {
                    pendingRewindTicks = 0
                    true
                } else {
                    false
                }
            }
        }
        lastPlaybackMs = playbackMs
        val suppressAutoFollow = System.currentTimeMillis() < suppressAutoFollowUntil
        if (rewound && !suppressAutoFollow) suppressAutoFollowUntil = 0L
        if (!suppressAutoFollow) {
            programmaticScroll = true
            val target = if (activeIndex <= 0) 0 else (activeIndex - 4).coerceAtLeast(0)
            listState.animateScrollToItem(target)
            programmaticScroll = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        state = listState,
        contentPadding = PaddingValues(top = 72.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(lines, key = { index, line -> "${line.startMs}-${line.text}-$index" }) { index, line ->
            SyncedLyricLine(
                line = line,
                active = index == activeIndex,
                onClick = {
                    line.startMs?.let {
                        suppressAutoFollowUntil = System.currentTimeMillis() + 900L
                        onSeek(it)
                    }
                },
            )
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
    var suppressAutoScrollUntil by remember(text) { mutableStateOf(0L) }
    var autoScrolling by remember(text) { mutableStateOf(false) }

    LaunchedEffect(listState, text) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling && !autoScrolling) {
                suppressAutoScrollUntil = System.currentTimeMillis() + 2_000L
            }
        }
    }
    LaunchedEffect(lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(1_400L)
            if (System.currentTimeMillis() < suppressAutoScrollUntil) continue
            val next = (listState.firstVisibleItemIndex + 1).coerceAtMost(lines.lastIndex)
            if (next == listState.firstVisibleItemIndex) continue
            autoScrolling = true
            listState.animateScrollToItem(next)
            autoScrolling = false
        }
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = VantafynColors.Muted)
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
    onAddToPlaylist: (JellyfinMusicPlaylist) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.vantafynAnimatedModalBorder(),
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
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
                playlists.firstOrNull()?.let { playlist ->
                    MusicMenuAction(Icons.Rounded.PlaylistAdd, "Add to ${playlist.name.take(18)}") { onAddToPlaylist(playlist) }
                }
                MusicMenuAction(Icons.Rounded.LibraryMusic, "Go to album", onDismiss)
                MusicMenuAction(Icons.Rounded.MoreHoriz, "More info", onDismiss)
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

@Composable
private fun QueuePanel(queue: List<VantafynMusicTrack>, index: Int, onTrack: (Int) -> Unit) {
    VantafynGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Up next", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            queue.drop(index).take(10).forEachIndexed { offset, track ->
                val absoluteIndex = index + offset
                val current = absoluteIndex == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (current) Color.White.copy(alpha = 0.095f) else Color.White.copy(alpha = 0.035f))
                        .clickable { onTrack(absoluteIndex) }
                        .padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    MusicArt(track.artworkUrl, Modifier.size(46.dp), cornerRadius = 13)
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
                }
            }
        }
    }
}

@Composable
private fun MusicArt(imageUrl: String?, modifier: Modifier, cornerRadius: Int = 22) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.66f)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("♪", color = VantafynColors.Ink)
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
private fun MusicScrubber(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    val duration = durationMs.toFloat().coerceAtLeast(1f)
    val value = positionMs.toFloat().coerceIn(0f, duration)
    Box(modifier = Modifier.fillMaxWidth()) {
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
                    .fillMaxWidth((value / duration).coerceIn(0f, 1f))
                    .fillMaxSize()
                    .background(VantafynGradients.accentHorizontal()),
            )
        }
        Slider(
            value = value,
            onValueChange = { onSeek(it.toLong()) },
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

private fun Long.formatTime(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

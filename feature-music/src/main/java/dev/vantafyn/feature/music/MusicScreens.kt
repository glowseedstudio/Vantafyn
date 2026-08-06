package dev.vantafyn.feature.music

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicArtist
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
            if (state.isLoading) item { VantafynLoadingIndicator("Loading music") }
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
        Text("Music", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        Text("Albums, artists, songs, playlists, and lyrics from Jellyfin.", color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MusicArt(track.artworkUrl, Modifier.size(54.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = VantafynColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                RoundIconButton(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause" else "Play", onToggle)
                RoundIconButton(Icons.Rounded.SkipNext, "Next", onNext)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(VantafynColors.Ink.copy(alpha = 0.12f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(VantafynGradients.accentHorizontal()),
                )
            }
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
    val track = state.playback.currentTrack ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(VantafynColors.Graphite)
            .padding(18.dp),
    ) {
        AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.18f), contentScale = ContentScale.Crop)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VantafynColors.Graphite.copy(alpha = 0.72f),
                            VantafynColors.Graphite.copy(alpha = 0.96f),
                        ),
                    ),
                ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Now Playing", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoundIconButton(Icons.Rounded.QueueMusic, "Queue", {})
                        RoundIconButton(Icons.Rounded.Close, "Close", viewModel::closeNowPlaying)
                    }
                }
            }
            item { MusicArt(track.artworkUrl, Modifier.size(284.dp)) }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(track.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(track.artist, track.album).joinToString(" - "), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.Rounded.Shuffle, "Shuffle", viewModel::toggleShuffle, selected = state.playback.shuffleEnabled)
                    RoundIconButton(Icons.Rounded.SkipPrevious, "Previous", viewModel::previous, size = 48)
                    RoundIconButton(
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
                        selected = true,
                    )
                    RoundIconButton(Icons.Rounded.SkipNext, "Next", viewModel::next, size = 48)
                    RoundIconButton(
                        if (state.playback.repeatMode == VantafynMusicRepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat",
                        viewModel::cycleRepeat,
                        selected = state.playback.repeatMode != VantafynMusicRepeatMode.Off,
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    item { IconPill(Icons.Rounded.PlaylistAdd, "New Playlist") { showPlaylistName = true } }
                    state.home?.playlists?.firstOrNull()?.let { playlist ->
                        item { IconPill(Icons.Rounded.Add, "Add to ${playlist.name.take(10)}") { viewModel.addCurrentToPlaylist(playlist) } }
                    }
                    item { IconPill(Icons.Rounded.Subtitles, "Lyrics", viewModel::openLyrics) }
                    item { IconPill(Icons.Rounded.MoreHoriz, "More") {} }
                }
            }
            item { QueuePanel(state.playback.queue, state.playback.queueIndex) }
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
            .background(VantafynColors.Graphite)
            .padding(18.dp),
    ) {
        AsyncImage(model = track.artworkUrl, contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.14f), contentScale = ContentScale.Crop)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(VantafynColors.Graphite.copy(alpha = 0.76f), VantafynColors.Graphite.copy(alpha = 0.98f)))),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Lyrics", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        Text(track.title, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    RoundIconButton(Icons.Rounded.Close, "Close lyrics", viewModel::closeLyrics)
                }
            }
            when {
                state.isLyricsLoading -> item { VantafynLoadingIndicator("Loading lyrics") }
                state.lyrics == null -> item {
                    LyricsEmptyState("No lyrics available", "Jellyfin did not expose lyrics for this track.")
                }
                state.lyrics.isSynced -> {
                    val active = state.lyrics.syncedLines.activeIndex(state.playback.positionMs)
                    itemsIndexed(state.lyrics.syncedLines, key = { index, line -> "${line.startMs}-${line.text}-$index" }) { index, line ->
                        val isActive = index == active
                        Text(
                            line.text.ifBlank { "…" },
                            color = if (isActive) VantafynColors.Ink else VantafynColors.Muted.copy(alpha = 0.68f),
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isActive) VantafynColors.Primary.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable(enabled = line.startMs != null) { line.startMs?.let(viewModel::seekTo) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
                else -> item {
                    Text(
                        state.lyrics.plainText.ifBlank { "No lyrics available" },
                        color = VantafynColors.Ink.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), VantafynColors.SurfaceHigh.copy(alpha = 0.42f))))
                            .padding(18.dp),
                    )
                }
            }
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
private fun QueuePanel(queue: List<VantafynMusicTrack>, index: Int) {
    VantafynGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Queue", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            queue.drop(index).take(8).forEach { track ->
                Text(track.title, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MusicArt(imageUrl: String?, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
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

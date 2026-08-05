package dev.vantafyn.feature.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField

@Composable
fun MusicScreen(
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(session?.profileId) {
        viewModel.bindSession(session)
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (state.playback.currentTrack != null) 190.dp else 118.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
            item {
                MusicHero(
                    track = state.playback.currentTrack,
                    onNowPlaying = viewModel::openNowPlaying,
                    onToggle = viewModel::togglePlayPause,
                    isPlaying = state.playback.isPlaying,
                )
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
            if (state.searchResults.isNotEmpty()) {
                item {
                    MusicTrackList(
                        title = "Search Results",
                        tracks = state.searchResults,
                        onTrack = { viewModel.playTrack(it, state.searchResults) },
                    )
                }
            }
            state.home?.let { home ->
                if (home.recentlyAdded.isNotEmpty()) item {
                    MusicTrackRow("Recently Added", home.recentlyAdded) { viewModel.playTrack(it, home.recentlyAdded) }
                }
                if (home.albums.isNotEmpty()) item {
                    MusicAlbumRow(home.albums, onAlbum = viewModel::playAlbum)
                }
                if (home.artists.isNotEmpty()) item {
                    MusicArtistRow(home.artists)
                }
                if (home.playlists.isNotEmpty()) item {
                    MusicPlaylistRow(home.playlists, onPlaylist = viewModel::playPlaylist)
                }
                if (home.songs.isNotEmpty()) item {
                    MusicTrackList("Songs", home.songs.take(20)) { viewModel.playTrack(it, home.songs) }
                }
            }
        }
        state.playback.currentTrack?.let {
            MusicMiniPlayer(
                track = it,
                isPlaying = state.playback.isPlaying,
                progress = progressFraction(state.playback.positionMs, state.playback.durationMs),
                onOpen = viewModel::openNowPlaying,
                onToggle = viewModel::togglePlayPause,
                onNext = viewModel::next,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 8.dp, end = 8.dp, bottom = 86.dp),
            )
        }
        if (state.showNowPlaying) {
            NowPlayingDialog(state = state, viewModel = viewModel)
        }
        state.message?.let { message ->
            AlertDialog(
                onDismissRequest = viewModel::clearMessage,
                confirmButton = { TextButton(onClick = viewModel::clearMessage) { Text("OK") } },
                title = { Text(message) },
            )
        }
    }
}

@Composable
private fun MusicHero(track: VantafynMusicTrack?, isPlaying: Boolean, onNowPlaying: () -> Unit, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.52f))
            .clickable(enabled = track != null, onClick = onNowPlaying)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Music", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        Text(
            track?.title ?: "Your Jellyfin music, ready when you are.",
            color = VantafynColors.Ink,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(track?.artist ?: "Albums, artists, playlists, songs, and lyrics.", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (track != null) {
            VantafynButton(if (isPlaying) "Pause" else "Play", onClick = onToggle, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MusicTrackRow(title: String, tracks: List<JellyfinMusicTrack>, onTrack: (JellyfinMusicTrack) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tracks, key = { it.id }) { track ->
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
            items(albums, key = { it.id }) { album ->
                MusicArtworkTile(album.artworkUrl, album.title, album.artist ?: "Album") { onAlbum(album) }
            }
        }
    }
}

@Composable
private fun MusicArtistRow(artists: List<JellyfinMusicArtist>) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Artists", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(artists, key = { it.id }) { artist ->
                MusicArtworkTile(artist.imageUrl, artist.name, "Artist") {}
            }
        }
    }
}

@Composable
private fun MusicPlaylistRow(playlists: List<JellyfinMusicPlaylist>, onPlaylist: (JellyfinMusicPlaylist) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Playlists", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                MusicArtworkTile(playlist.imageUrl, playlist.name, "${playlist.trackCount ?: 0} tracks") { onPlaylist(playlist) }
            }
        }
    }
}

@Composable
private fun MusicTrackList(title: String, tracks: List<JellyfinMusicTrack>, onTrack: (JellyfinMusicTrack) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        tracks.forEach { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(VantafynColors.SurfaceHigh.copy(alpha = 0.42f))
                    .clickable { onTrack(track) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MusicArt(track.artworkUrl, Modifier.size(52.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = VantafynColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(track.durationMs?.formatTime().orEmpty(), color = VantafynColors.Muted)
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.92f))
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MusicArt(track.artworkUrl, Modifier.size(54.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = VantafynColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MiniControl(if (isPlaying) "Pause" else "Play", onToggle)
            MiniControl("Next", onNext)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(VantafynColors.Ink.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(VantafynColors.Ink.copy(alpha = 0.78f)),
            )
        }
    }
}

@Composable
private fun NowPlayingDialog(state: MusicUiState, viewModel: MusicViewModel) {
    var showPlaylistName by remember { mutableStateOf(false) }
    val track = state.playback.currentTrack ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(VantafynColors.Graphite.copy(alpha = 0.98f))
            .padding(18.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Now Playing", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                    MiniControl("Close", viewModel::closeNowPlaying)
                }
            }
            item { MusicArt(track.artworkUrl, Modifier.size(260.dp)) }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(track.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(track.artist, track.album).joinToString(" - "), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            item {
                Slider(
                    value = state.playback.positionMs.toFloat().coerceAtMost(state.playback.durationMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..state.playback.durationMs.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(state.playback.positionMs.formatTime(), color = VantafynColors.Muted)
                    Text(state.playback.durationMs.formatTime(), color = VantafynColors.Muted)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    MiniControl(if (state.playback.shuffleEnabled) "Shuffle On" else "Shuffle", viewModel::toggleShuffle)
                    MiniControl("Prev", viewModel::previous)
                    MiniControl(if (state.playback.isPlaying) "Pause" else "Play", viewModel::togglePlayPause)
                    MiniControl("Next", viewModel::next)
                    MiniControl(
                        when (state.playback.repeatMode) {
                            VantafynMusicRepeatMode.Off -> "Repeat"
                            VantafynMusicRepeatMode.All -> "Repeat All"
                            VantafynMusicRepeatMode.One -> "Repeat One"
                        },
                        viewModel::cycleRepeat,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniControl("New Playlist") { showPlaylistName = true }
                    state.home?.playlists?.firstOrNull()?.let { playlist ->
                        MiniControl("Add to ${playlist.name.take(10)}") { viewModel.addCurrentToPlaylist(playlist) }
                    }
                }
            }
            item { LyricsPanel(state) }
            item { QueuePanel(state.playback.queue, state.playback.queueIndex) }
        }
    }
    if (showPlaylistName) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.42f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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

@Composable
private fun QueuePanel(queue: List<VantafynMusicTrack>, index: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.36f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Queue", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        queue.drop(index).take(8).forEach { track ->
            Text(track.title, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    Text(
        label,
        color = VantafynColors.Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(VantafynColors.Ink.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        maxLines = 1,
    )
}

private fun progressFraction(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

private fun Long.formatTime(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

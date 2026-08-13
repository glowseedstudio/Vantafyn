package dev.vantafyn.feature.music

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vantafyn.core.jellyfin.JellyfinLyricLine
import dev.vantafyn.core.jellyfin.JellyfinLyrics
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicArtist
import dev.vantafyn.core.jellyfin.JellyfinMusicHome
import dev.vantafyn.core.jellyfin.JellyfinMusicPlaylist
import dev.vantafyn.core.jellyfin.JellyfinMusicRepository
import dev.vantafyn.core.jellyfin.JellyfinMusicTrack
import dev.vantafyn.core.jellyfin.JellyfinMediaRepository
import dev.vantafyn.core.jellyfin.JellyfinPlaybackInfo
import dev.vantafyn.core.jellyfin.JellyfinPlaybackMethod
import dev.vantafyn.core.jellyfin.JellyfinPlaybackRepository
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.VantafynMusicPlaybackEvent
import dev.vantafyn.core.media.VantafynMusicPlaybackState
import dev.vantafyn.core.media.VantafynMusicStopReason
import dev.vantafyn.core.media.VantafynMusicTrack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repositories = JellyfinRepositoryProvider(application)
    private val musicRepository: JellyfinMusicRepository = repositories.musicRepository
    private val mediaRepository: JellyfinMediaRepository = repositories.mediaRepository
    private val playbackRepository: JellyfinPlaybackRepository = repositories.playbackRepository
    private val playbackController = MusicPlaybackController.get(application)
    private var session: JellyfinSession? = null
    private var lyricsJob: Job? = null
    private val playbackInfoByTrack = mutableMapOf<UUID, JellyfinPlaybackInfo>()
    private var reportedTrackId: UUID? = null
    private var lastProgressReportMs: Long = 0L
    private var lastProgressTrackId: UUID? = null
    private var lastPausedState: Boolean? = null

    private val _state = MutableStateFlow(MusicUiState(playback = playbackController.state.value))
    val state: StateFlow<MusicUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            playbackController.state.collect { playback ->
                _state.update { it.copy(playback = playback) }
                val track = playback.currentTrack
                if (track != null && track.id != _state.value.lyricsTrackId) {
                    loadLyrics(track.id)
                }
                maybeReportTimedProgress(playback)
            }
        }
        viewModelScope.launch {
            playbackController.events.collect { event ->
                handlePlaybackEvent(event)
            }
        }
    }

    fun bindSession(session: JellyfinSession?) {
        if (session == null) return
        if (this.session?.profileId == session.profileId && _state.value.home != null) return
        this.session = session
        loadHome()
    }

    fun loadHome() {
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = musicRepository.getMusicHome(activeSession)) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(isLoading = false, home = result.value, errorMessage = null)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun playTrack(track: JellyfinMusicTrack, queue: List<JellyfinMusicTrack>) {
        val activeSession = session ?: return
        viewModelScope.launch {
            val safeQueue = queue.ifEmpty { listOf(track) }
            val queueIds = safeQueue.map { it.id }.toSet()
            playbackInfoByTrack.keys.removeAll { it !in queueIds }
            val preparedQueue = safeQueue.map { queuedTrack ->
                val playbackInfo = preparePlaybackInfo(activeSession, queuedTrack)
                if (playbackInfo != null) {
                    playbackInfoByTrack[queuedTrack.id] = playbackInfo
                    queuedTrack.toPlaybackTrack(streamUrl = playbackInfo.streamUrl)
                } else {
                    queuedTrack.toPlaybackTrack()
                }
            }
            Log.d(
                "VantafynMusic",
                "Starting track '${track.title.take(80)}' queueSize=${safeQueue.size} prepared=${preparedQueue.count { it.id in playbackInfoByTrack }}",
            )
            playbackController.playQueue(
                queue = preparedQueue,
                startIndex = safeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0),
            )
        }
    }

    fun playAlbum(album: JellyfinMusicAlbum) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getAlbumTracks(activeSession, album.id)) {
                is JellyfinResult.Success -> {
                    Log.d("VantafynMusic", "Loaded album '${album.title.take(80)}' tracks=${result.value.size}")
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Album(album, result.value),
                            errorMessage = if (result.value.isEmpty()) "No tracks found for this album." else null,
                        )
                    }
                    result.value.firstOrNull()?.let { playTrack(it, result.value) }
                }
                is JellyfinResult.Failure -> {
                    Log.d("VantafynMusic", "Album load failed: ${result.message.take(120)}")
                    _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun playPlaylist(playlist: JellyfinMusicPlaylist) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getPlaylistItems(activeSession, playlist.id)) {
                is JellyfinResult.Success -> {
                    Log.d("VantafynMusic", "Loaded playlist '${playlist.name.take(80)}' tracks=${result.value.size}")
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Playlist(playlist, result.value),
                            errorMessage = if (result.value.isEmpty()) "This playlist is empty." else null,
                        )
                    }
                    result.value.firstOrNull()?.let { playTrack(it, result.value) }
                }
                is JellyfinResult.Failure -> {
                    Log.d("VantafynMusic", "Playlist load failed: ${result.message.take(120)}")
                    _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun openAlbum(album: JellyfinMusicAlbum) {
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            when (val result = musicRepository.getAlbumTracks(activeSession, album.id)) {
                is JellyfinResult.Success -> _state.update { it.copy(screen = MusicScreenState.Album(album, result.value)) }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun openPlaylist(playlist: JellyfinMusicPlaylist) {
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            when (val result = musicRepository.getPlaylistItems(activeSession, playlist.id)) {
                is JellyfinResult.Success -> _state.update { it.copy(screen = MusicScreenState.Playlist(playlist, result.value)) }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun openArtist(artist: JellyfinMusicArtist) {
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            when (val result = musicRepository.getArtistAlbums(activeSession, artist.id)) {
                is JellyfinResult.Success -> _state.update { it.copy(screen = MusicScreenState.Artist(artist, result.value)) }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun showSongs() {
        _state.value.home?.songs?.let { songs ->
            _state.update { it.copy(screen = MusicScreenState.Songs(songs)) }
        }
    }

    fun showHome() {
        _state.update { it.copy(screen = MusicScreenState.Home) }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        val activeSession = session ?: return
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            when (val result = musicRepository.searchMusic(activeSession, query)) {
                is JellyfinResult.Success -> _state.update { it.copy(searchResults = result.value) }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun openNowPlaying() {
        _state.update { it.copy(showNowPlaying = true) }
    }

    fun closeNowPlaying() {
        _state.update { it.copy(showNowPlaying = false, showLyricsScreen = false) }
    }

    fun openLyrics() {
        _state.update { it.copy(showLyricsScreen = true, showNowPlaying = true) }
    }

    fun closeLyrics() {
        _state.update { it.copy(showLyricsScreen = false) }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()
    fun next() = playbackController.next()
    fun previous() = playbackController.previous()
    fun playQueueIndex(index: Int) = playbackController.playQueueIndex(index)
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun cycleRepeat() = playbackController.cycleRepeatMode()
    fun playNext(track: JellyfinMusicTrack) {
        playbackController.playNext(track.toPlaybackTrack())
        _state.update { it.copy(message = "Queued next") }
    }

    fun addToQueue(track: JellyfinMusicTrack) {
        playbackController.addToQueue(track.toPlaybackTrack())
        _state.update { it.copy(message = "Added to queue") }
    }

    fun playCurrentNext() {
        val current = playbackController.state.value.currentTrack ?: return
        playbackController.playNext(current)
        _state.update { it.copy(message = "Queued next") }
    }

    fun addCurrentToQueue() {
        val current = playbackController.state.value.currentTrack ?: return
        playbackController.addToQueue(current)
        _state.update { it.copy(message = "Added to queue") }
    }

    fun openCurrentAlbum() {
        val current = playbackController.state.value.currentTrack ?: return
        val albumId = current.albumId ?: return
        val activeSession = session ?: return
        val album = _state.value.home?.albums?.firstOrNull { it.id == albumId }
            ?: JellyfinMusicAlbum(
                id = albumId,
                title = current.album ?: "Album",
                artist = current.artist,
                year = null,
                artworkUrl = current.artworkUrl,
            )
        viewModelScope.launch {
            when (val result = musicRepository.getAlbumTracks(activeSession, albumId)) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(screen = MusicScreenState.Album(album, result.value), showNowPlaying = false)
                }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun openCurrentArtist() {
        val current = playbackController.state.value.currentTrack ?: return
        val artist = _state.value.home?.artists?.firstOrNull { it.name.equals(current.artist, ignoreCase = true) } ?: return
        openArtist(artist)
        closeNowPlaying()
    }

    fun openTrackAlbum(track: JellyfinMusicTrack) {
        val albumId = track.albumId ?: return
        val album = _state.value.home?.albums?.firstOrNull { it.id == albumId }
            ?: JellyfinMusicAlbum(
                id = albumId,
                title = track.album ?: "Album",
                artist = track.artist,
                year = null,
                artworkUrl = track.artworkUrl,
            )
        openAlbum(album)
    }

    fun toggleCurrentFavorite() {
        val activeSession = session ?: return
        val current = playbackController.state.value.currentTrack ?: return
        val targetFavorite = !current.isFavorite
        viewModelScope.launch {
            when (val result = mediaRepository.setFavorite(activeSession, current.id, targetFavorite)) {
                is JellyfinResult.Success -> {
                    playbackController.updateFavorite(current.id, result.value)
                    _state.update { state ->
                        state.copy(
                            home = state.home?.copyWithFavorite(current.id, result.value),
                            message = if (result.value) "Added to My List" else "Removed from My List",
                        )
                    }
                }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun createPlaylistWithCurrent(name: String) {
        val activeSession = session ?: return
        val current = playbackController.state.value.currentTrack ?: return
        val playlistName = name.trim().ifBlank { "Vantafyn Playlist" }
        val optimisticPlaylist = JellyfinMusicPlaylist(
            id = UUID.randomUUID(),
            name = playlistName,
            imageUrl = current.artworkUrl,
            trackCount = 1,
        )
        viewModelScope.launch {
            _state.update { it.copy(isPlaylistSaving = true, errorMessage = null, message = null) }
            when (val result = musicRepository.createPlaylist(activeSession, playlistName, listOf(current.id))) {
                is JellyfinResult.Success -> {
                    val loaded = refreshHomeForPlaylist(activeSession, playlistName, result.value)
                    if (!loaded) {
                        _state.update { state ->
                            state.copy(
                                isPlaylistSaving = false,
                                home = state.home?.let { home ->
                                    home.copy(playlists = (home.playlists + optimisticPlaylist).distinctBy { it.name.lowercase() })
                                },
                                message = "Playlist created",
                            )
                        }
                    }
                }
                is JellyfinResult.Failure -> {
                    val loaded = refreshHomeForPlaylist(activeSession, playlistName, null)
                    if (!loaded) {
                        _state.update { it.copy(isPlaylistSaving = false, errorMessage = result.message) }
                    }
                }
            }
        }
    }

    fun addCurrentToPlaylist(playlist: JellyfinMusicPlaylist) {
        val activeSession = session ?: return
        val current = playbackController.state.value.currentTrack ?: return
        viewModelScope.launch {
            when (val result = musicRepository.addToPlaylist(activeSession, playlist.id, listOf(current.id))) {
                is JellyfinResult.Success -> {
                    Log.d("VantafynMusic", "Added current track to playlist '${playlist.name.take(80)}'")
                    _state.update { it.copy(message = "Added to ${playlist.name}", home = it.home?.incrementPlaylistCount(playlist.id)) }
                }
                is JellyfinResult.Failure -> {
                    Log.d("VantafynMusic", "Add to playlist failed: ${result.message.take(120)}")
                    _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun addTrackToPlaylist(track: JellyfinMusicTrack, playlist: JellyfinMusicPlaylist) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.addToPlaylist(activeSession, playlist.id, listOf(track.id))) {
                is JellyfinResult.Success -> _state.update { it.copy(message = "Added to ${playlist.name}", home = it.home?.incrementPlaylistCount(playlist.id)) }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun stopForProfileChange() {
        playbackController.stop(clearQueue = true, reason = VantafynMusicStopReason.ProfileSwitch)
    }

    fun pauseForBackground() {
        if (playbackController.state.value.isPlaying) {
            Log.d("VantafynMusic", "Pausing music for app background")
            playbackController.pause()
        }
    }

    private fun loadLyrics(trackId: UUID) {
        val activeSession = session ?: return
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _state.update { it.copy(lyricsTrackId = trackId, lyrics = null, isLyricsLoading = true) }
            when (val result = musicRepository.getLyrics(activeSession, trackId)) {
                is JellyfinResult.Success -> _state.update {
                    Log.d("VantafynMusic", "Lyrics ${if (result.value == null) "missing" else "loaded"} for track=$trackId")
                    it.copy(isLyricsLoading = false, lyrics = result.value)
                }
                is JellyfinResult.Failure -> _state.update {
                    Log.d("VantafynMusic", "Lyrics load failed: ${result.message.take(120)}")
                    it.copy(isLyricsLoading = false, lyrics = null)
                }
            }
        }
    }

    private suspend fun preparePlaybackInfo(activeSession: JellyfinSession, track: JellyfinMusicTrack): JellyfinPlaybackInfo? =
        when (
            val result = playbackRepository.getPlaybackInfo(
                session = activeSession,
                itemId = track.id,
                title = track.title,
                subtitle = track.artist,
            )
        ) {
            is JellyfinResult.Success -> result.value.also {
                Log.d("VantafynMusic", "Prepared playback info mediaSource=${it.mediaSourceId != null} playSession=${it.playSessionId != null}")
            }
            is JellyfinResult.Failure -> {
                Log.d("VantafynMusic", "Music playback prepare failed, using universal audio URL: ${result.message.take(120)}")
                null
            }
        }

    private fun handlePlaybackEvent(event: VantafynMusicPlaybackEvent) {
        when (event) {
            is VantafynMusicPlaybackEvent.TrackStarted -> reportStarted(event.track, event.positionMs)
            is VantafynMusicPlaybackEvent.TrackChanged -> {
                event.previousTrack?.let { reportStopped(it, event.previousPositionMs, event.reason) }
                _state.update { it.copy(lyricsTrackId = null, lyrics = null, isLyricsLoading = false) }
                event.currentTrack?.let { reportStarted(it, 0L) }
            }
            is VantafynMusicPlaybackEvent.PauseChanged -> reportProgress(event.track, event.positionMs, event.isPaused, force = true)
            is VantafynMusicPlaybackEvent.Seeked -> reportProgress(event.track, event.positionMs, isPaused = !_state.value.playback.isPlaying, force = true)
            is VantafynMusicPlaybackEvent.Stopped -> event.track?.let { reportStopped(it, event.positionMs, event.reason) }
        }
    }

    private fun maybeReportTimedProgress(playback: VantafynMusicPlaybackState) {
        val track = playback.currentTrack ?: return
        if (!playback.isPlaying) return
        val now = System.currentTimeMillis()
        if (lastProgressTrackId != track.id || now - lastProgressReportMs >= MusicProgressReportIntervalMs) {
            reportProgress(track, playback.positionMs, isPaused = false, force = true)
            lastProgressTrackId = track.id
            lastProgressReportMs = now
        }
    }

    private fun reportStarted(track: VantafynMusicTrack, positionMs: Long) {
        if (reportedTrackId == track.id) return
        val activeSession = session ?: return
        val info = playbackInfoByTrack[track.id] ?: track.toFallbackPlaybackInfo()
        reportedTrackId = track.id
        lastPausedState = false
        Log.d("VantafynMusic", "Reporting start '${track.title.take(80)}' playSession=${info.playSessionId != null}")
        viewModelScope.launch {
            reportResult("start", playbackRepository.reportStarted(activeSession, info, positionMs.toTicks()))
        }
    }

    private fun reportProgress(track: VantafynMusicTrack, positionMs: Long, isPaused: Boolean, force: Boolean = false) {
        val activeSession = session ?: return
        val info = playbackInfoByTrack[track.id] ?: track.toFallbackPlaybackInfo()
        val now = System.currentTimeMillis()
        if (!force && lastProgressTrackId == track.id && now - lastProgressReportMs < MusicProgressReportIntervalMs) return
        if (force || lastPausedState != isPaused) {
            lastPausedState = isPaused
        }
        lastProgressTrackId = track.id
        lastProgressReportMs = now
        viewModelScope.launch {
            reportResult("progress", playbackRepository.reportProgress(activeSession, info, positionMs.toTicks(), isPaused))
        }
    }

    private fun reportStopped(track: VantafynMusicTrack, positionMs: Long, reason: VantafynMusicStopReason) {
        if (reportedTrackId != track.id && !playbackInfoByTrack.containsKey(track.id)) return
        val activeSession = session ?: return
        val info = playbackInfoByTrack[track.id] ?: track.toFallbackPlaybackInfo()
        reportedTrackId = null
        playbackInfoByTrack.remove(track.id)
        Log.d("VantafynMusic", "Reporting stop '${track.title.take(80)}' reason=$reason")
        viewModelScope.launch {
            reportResult("stop", playbackRepository.reportStopped(activeSession, info, positionMs.toTicks()))
        }
    }

    private fun reportResult(action: String, result: JellyfinResult<Unit>) {
        if (result is JellyfinResult.Failure) {
            Log.d("VantafynMusic", "Play-state $action failed: ${result.message.take(120)}")
        }
    }

    private suspend fun refreshHomeForPlaylist(activeSession: JellyfinSession, playlistName: String, playlistId: UUID?): Boolean {
        repeat(8) { attempt ->
            if (attempt > 0) delay(700L)
            when (val homeResult = musicRepository.getMusicHome(activeSession)) {
                is JellyfinResult.Success -> {
                    val found = homeResult.value.playlists.any { playlist ->
                        playlist.id == playlistId || playlist.name.equals(playlistName, ignoreCase = true)
                    }
                    _state.update {
                        it.copy(
                            isPlaylistSaving = !found,
                            home = homeResult.value,
                            errorMessage = null,
                            message = if (found) "Playlist created" else it.message,
                        )
                    }
                    if (found) return true
                }
                is JellyfinResult.Failure -> {
                    if (attempt == 7) {
                        _state.update { it.copy(isPlaylistSaving = false, errorMessage = homeResult.message) }
                    }
                }
            }
        }
        _state.update { it.copy(isPlaylistSaving = false) }
        return false
    }
}

data class MusicUiState(
    val isLoading: Boolean = false,
    val isPlaylistSaving: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val home: JellyfinMusicHome? = null,
    val playback: VantafynMusicPlaybackState,
    val showNowPlaying: Boolean = false,
    val showLyricsScreen: Boolean = false,
    val lyricsTrackId: UUID? = null,
    val lyrics: JellyfinLyrics? = null,
    val isLyricsLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<JellyfinMusicTrack> = emptyList(),
    val screen: MusicScreenState = MusicScreenState.Home,
)

sealed interface MusicScreenState {
    data object Home : MusicScreenState
    data class Album(val album: JellyfinMusicAlbum, val tracks: List<JellyfinMusicTrack>) : MusicScreenState
    data class Artist(val artist: JellyfinMusicArtist, val albums: List<JellyfinMusicAlbum>) : MusicScreenState
    data class Playlist(val playlist: JellyfinMusicPlaylist, val tracks: List<JellyfinMusicTrack>) : MusicScreenState
    data class Songs(val tracks: List<JellyfinMusicTrack>) : MusicScreenState
}

fun List<JellyfinLyricLine>.activeIndex(positionMs: Long): Int =
    indexOfLast { line -> line.startMs?.let { it <= positionMs } == true }.coerceAtLeast(0)

private fun JellyfinMusicTrack.toPlaybackTrack(streamUrl: String = this.streamUrl): VantafynMusicTrack =
    VantafynMusicTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        streamUrl = streamUrl,
        artworkUrl = artworkUrl,
        isFavorite = isFavorite,
    )

private fun VantafynMusicTrack.toFallbackPlaybackInfo(): JellyfinPlaybackInfo =
    JellyfinPlaybackInfo(
        itemId = id,
        title = title,
        subtitle = artist,
        streamUrl = streamUrl,
        fallbackStreamUrl = null,
        playSessionId = null,
        mediaSourceId = null,
        liveStreamId = null,
        method = JellyfinPlaybackMethod.DirectStream,
        runtimeTicks = durationMs?.toTicks(),
        startPositionTicks = 0L,
        audioStreamIndex = null,
        subtitleStreamIndex = null,
        audioTracks = emptyList(),
        subtitleTracks = emptyList(),
        sourceLabel = "Universal audio",
        isLiveStream = false,
    )

private fun JellyfinMusicHome.copyWithFavorite(trackId: UUID, isFavorite: Boolean): JellyfinMusicHome =
    copy(
        recentlyAdded = recentlyAdded.mapFavorite(trackId, isFavorite),
        songs = songs.mapFavorite(trackId, isFavorite),
    )

private fun JellyfinMusicHome.incrementPlaylistCount(playlistId: UUID): JellyfinMusicHome =
    copy(
        playlists = playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackCount = playlist.trackCount?.plus(1))
            } else {
                playlist
            }
        },
    )

private fun List<JellyfinMusicTrack>.mapFavorite(trackId: UUID, isFavorite: Boolean): List<JellyfinMusicTrack> =
    map { if (it.id == trackId) it.copy(isFavorite = isFavorite) else it }

private fun Long.toTicks(): Long =
    coerceAtLeast(0L) * 10_000L

private const val MusicProgressReportIntervalMs = 10_000L

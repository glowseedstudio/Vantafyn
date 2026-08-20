package dev.vantafyn.feature.music

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vantafyn.core.downloads.OfflineDownloadManager
import dev.vantafyn.core.jellyfin.JellyfinLyricLine
import dev.vantafyn.core.jellyfin.JellyfinLyrics
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicArtist
import dev.vantafyn.core.jellyfin.JellyfinMusicHome
import dev.vantafyn.core.jellyfin.JellyfinMusicPlaylist
import dev.vantafyn.core.jellyfin.JellyfinMusicTrackPage
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
import dev.vantafyn.core.media.AppForegroundStateRepository
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
    private val offlineDownloadManager = OfflineDownloadManager(application)
    private val appPreferences = application.getSharedPreferences("vantafyn_app_preferences", Context.MODE_PRIVATE)
    private val playbackController = MusicPlaybackController.get(application)
    private var session: JellyfinSession? = null
    private var lyricsJob: Job? = null
    private val playbackInfoByTrack = mutableMapOf<UUID, JellyfinPlaybackInfo>()
    private var reportedTrackId: UUID? = null
    private var lastProgressReportMs: Long = 0L
    private var lastProgressTrackId: UUID? = null
    private var lastPausedState: Boolean? = null
    private var playRequestJob: Job? = null
    private var musicPageJob: Job? = null
    private var pendingPlayTrackId: UUID? = null
    private var musicScreenActive = false
    private var popupLyricsActive = false
    private var lyricsPrefetchJob: Job? = null
    private val lyricsCache = LinkedHashMap<LyricsCacheKey, JellyfinLyrics?>()

    private val _state = MutableStateFlow(MusicUiState(playback = playbackController.state.value))
    val state: StateFlow<MusicUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            playbackController.state.collect { playback ->
                if (shouldPublishPlaybackToUi(playback)) {
                    _state.update { it.copy(playback = playback) }
                }
                val track = playback.currentTrack
                if (track != null && (_state.value.showLyricsScreen || popupLyricsActive) && track.id != _state.value.lyricsTrackId) {
                    loadLyrics(track.id)
                } else if (track != null && shouldPrefetchLyrics()) {
                    prefetchLyrics(track.id)
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
        if (this.session?.profileId == session.profileId && this.session?.server?.localId == session.server.localId && _state.value.home != null) return
        if (this.session?.profileId != session.profileId || this.session?.server?.localId != session.server.localId) {
            lyricsCache.clear()
            lyricsJob?.cancel()
            lyricsPrefetchJob?.cancel()
        }
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
        if (pendingPlayTrackId == track.id && playRequestJob?.isActive == true) return
        playRequestJob?.cancel()
        pendingPlayTrackId = track.id
        _state.update { it.copy(pendingPlayTrackId = track.id, errorMessage = null) }
        playRequestJob = viewModelScope.launch {
            val safeQueue = queue.ifEmpty { listOf(track) }
            val queueIds = safeQueue.map { it.id }.toSet()
            playbackInfoByTrack.keys.removeAll { it !in queueIds }
            try {
                val preparedQueue = safeQueue.map { queuedTrack ->
                    val playbackInfo = preparePlaybackInfo(activeSession, queuedTrack)
                    if (playbackInfo != null) {
                        playbackInfoByTrack[queuedTrack.id] = playbackInfo
                        queuedTrack.toPlaybackTrack(streamUrl = playbackInfo.streamUrl)
                    } else {
                        queuedTrack.toPlaybackTrack()
                    }
                }
                playbackController.playQueue(
                    queue = preparedQueue,
                    startIndex = safeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0),
                )
            } finally {
                if (pendingPlayTrackId == track.id) {
                    pendingPlayTrackId = null
                    _state.update { it.copy(pendingPlayTrackId = null) }
                }
            }
        }
    }

    fun playAlbum(album: JellyfinMusicAlbum) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getAlbumTracks(activeSession, album.id)) {
                is JellyfinResult.Success -> {
                    result.value.firstOrNull()?.let { playTrack(it, result.value) }
                    openAlbum(album)
                    if (result.value.isEmpty()) _state.update { it.copy(errorMessage = "No tracks found for this album.") }
                }
                is JellyfinResult.Failure -> {
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
                    result.value.firstOrNull()?.let { playTrack(it, result.value) }
                    openPlaylist(playlist)
                    if (result.value.isEmpty()) _state.update { it.copy(errorMessage = "This playlist is empty.") }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun openAlbum(album: JellyfinMusicAlbum) {
        loadAlbumPage(album, startIndex = 0)
    }

    fun openPlaylist(playlist: JellyfinMusicPlaylist) {
        loadPlaylistPage(playlist, startIndex = 0)
    }

    private fun loadAlbumPage(album: JellyfinMusicAlbum, startIndex: Int) {
        val activeSession = session ?: return
        musicPageJob?.cancel()
        musicPageJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    screen = MusicScreenState.Album(album, it.musicTrackPageFor(album.id, startIndex)),
                    isMusicPageLoading = true,
                    errorMessage = null,
                )
            }
            when (val result = musicRepository.getAlbumTracksPage(activeSession, album.id, startIndex)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Album(album, result.value),
                            isMusicPageLoading = false,
                        )
                    }
                    checkPlaylistDownloadStatus(activeSession, album.id.toString(), result.value.totalItems)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isMusicPageLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun loadPlaylistPage(playlist: JellyfinMusicPlaylist, startIndex: Int) {
        val activeSession = session ?: return
        musicPageJob?.cancel()
        musicPageJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    screen = MusicScreenState.Playlist(playlist, it.musicTrackPageFor(playlist.id, startIndex)),
                    isMusicPageLoading = true,
                    errorMessage = null,
                )
            }
            when (val result = musicRepository.getPlaylistItemsPage(activeSession, playlist.id, startIndex)) {
                is JellyfinResult.Success -> {
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Playlist(playlist, result.value),
                            isMusicPageLoading = false,
                        )
                    }
                    checkPlaylistDownloadStatus(activeSession, playlist.id.toString(), result.value.totalItems)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isMusicPageLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private suspend fun checkPlaylistDownloadStatus(
        session: dev.vantafyn.core.jellyfin.JellyfinSession,
        playlistId: String,
        trackCount: Int,
    ) {
        val uuid = runCatching { UUID.fromString(playlistId) }.getOrNull() ?: return
        val downloaded = offlineDownloadManager.isPlaylistFullyDownloaded(session, uuid, trackCount)
        _state.update { it.copy(isPlaylistDownloaded = downloaded) }
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
        loadSongsPage(startIndex = 0)
    }

    private fun loadSongsPage(startIndex: Int) {
        val activeSession = session ?: return
        musicPageJob?.cancel()
        musicPageJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    screen = MusicScreenState.Songs(it.musicTrackPageFor(null, startIndex)),
                    isMusicPageLoading = true,
                    errorMessage = null,
                )
            }
            when (val result = musicRepository.getSongsPage(activeSession, startIndex)) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(screen = MusicScreenState.Songs(result.value), isMusicPageLoading = false)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isMusicPageLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun previousMusicPage() {
        when (val screen = _state.value.screen) {
            is MusicScreenState.Album -> if (screen.page.hasPrevious) loadAlbumPage(screen.album, (screen.page.startIndex - screen.page.pageSize).coerceAtLeast(0))
            is MusicScreenState.Playlist -> if (screen.page.hasPrevious) loadPlaylistPage(screen.playlist, (screen.page.startIndex - screen.page.pageSize).coerceAtLeast(0))
            is MusicScreenState.Songs -> if (screen.page.hasPrevious) loadSongsPage((screen.page.startIndex - screen.page.pageSize).coerceAtLeast(0))
            else -> Unit
        }
    }

    fun nextMusicPage() {
        when (val screen = _state.value.screen) {
            is MusicScreenState.Album -> if (screen.page.hasNext) loadAlbumPage(screen.album, screen.page.startIndex + screen.page.pageSize)
            is MusicScreenState.Playlist -> if (screen.page.hasNext) loadPlaylistPage(screen.playlist, screen.page.startIndex + screen.page.pageSize)
            is MusicScreenState.Songs -> if (screen.page.hasNext) loadSongsPage(screen.page.startIndex + screen.page.pageSize)
            else -> Unit
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
        playbackController.state.value.currentTrack?.id?.let(::prefetchLyrics)
    }

    fun closeNowPlaying() {
        _state.update { it.copy(showNowPlaying = false, showLyricsScreen = false) }
    }

    fun setMusicScreenActive(active: Boolean) {
        musicScreenActive = active
        if (active) {
            _state.update { it.copy(playback = playbackController.state.value) }
            playbackController.state.value.currentTrack?.id?.takeIf { shouldPrefetchLyrics() }?.let(::prefetchLyrics)
        }
    }

    private fun shouldPublishPlaybackToUi(playback: VantafynMusicPlaybackState): Boolean {
        if (AppForegroundStateRepository.isForeground.value || musicScreenActive || popupLyricsActive) return true
        val current = _state.value.playback
        return current.currentTrack?.id != playback.currentTrack?.id ||
            current.queueIndex != playback.queueIndex ||
            current.queue.size != playback.queue.size ||
            current.isPlaying != playback.isPlaying ||
            current.durationMs != playback.durationMs ||
            playback.errorMessage != null
    }

    fun openLyrics() {
        val track = playbackController.state.value.currentTrack
        _state.update { it.copy(showLyricsScreen = true, showNowPlaying = true) }
        if (track != null && track.id != _state.value.lyricsTrackId) {
            loadLyrics(track.id)
        }
    }

    fun setPopupLyricsActive(active: Boolean) {
        popupLyricsActive = active
        if (active) {
            playbackController.state.value.currentTrack?.id?.let { trackId ->
                if (trackId != _state.value.lyricsTrackId) loadLyrics(trackId)
            }
        }
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

    fun queueTrackDownload(track: JellyfinMusicTrack) {
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null, message = null) }
            when (val result = offlineDownloadManager.queueMusicTrack(activeSession, track, requireWifi = readDownloadWifiOnlyDefault())) {
                is JellyfinResult.Success -> _state.update { it.copy(message = "Download queued") }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun queueAlbumDownload(album: JellyfinMusicAlbum, tracks: List<JellyfinMusicTrack>) {
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null, message = null) }
            when (val result = offlineDownloadManager.queueMusicAlbum(activeSession, album, tracks, requireWifi = readDownloadWifiOnlyDefault())) {
                is JellyfinResult.Success -> _state.update { it.copy(message = "${result.value} tracks queued") }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun queuePlaylistDownload(playlist: JellyfinMusicPlaylist, tracks: List<JellyfinMusicTrack>) {
        val activeSession = session ?: return
        if (tracks.isEmpty()) {
            _state.update { it.copy(errorMessage = "This playlist does not have any tracks to save.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null, message = null) }
            when (
                val result = offlineDownloadManager.queueMusicPlaylist(
                    session = activeSession,
                    playlist = playlist,
                    tracks = tracks,
                    requireWifi = readDownloadWifiOnlyDefault(),
                )
            ) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(message = "${result.value} tracks queued as ${playlist.name}", errorMessage = null)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(message = null, errorMessage = result.message)
                }
            }
        }
    }

    private fun readDownloadWifiOnlyDefault(): Boolean =
        appPreferences.getBoolean(KEY_DOWNLOAD_WIFI_ONLY_DEFAULT, true)

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
        val album = _state.value.home?.albums?.firstOrNull { it.id == albumId }
            ?: JellyfinMusicAlbum(
                id = albumId,
                title = current.album ?: "Album",
                artist = current.artist,
                year = null,
                artworkUrl = current.artworkUrl,
            )
        _state.update { it.copy(showNowPlaying = false) }
        openAlbum(album)
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
                    _state.update { it.copy(message = "Added to ${playlist.name}", home = it.home?.incrementPlaylistCount(playlist.id)) }
                }
                is JellyfinResult.Failure -> {
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

    fun createPlaylistAndAddTrack(name: String, track: JellyfinMusicTrack?) {
        val activeSession = session ?: return
        val trackIds = listOfNotNull(track?.id)
        viewModelScope.launch {
            _state.update { it.copy(isPlaylistSaving = true, errorMessage = null, message = null) }
            when (val result = musicRepository.createPlaylist(activeSession, name, trackIds)) {
                is JellyfinResult.Success -> {
                    val loaded = refreshHomeForPlaylist(activeSession, name, result.value)
                    if (!loaded) {
                        _state.update { state ->
                            state.copy(
                                isPlaylistSaving = false,
                                message = "Playlist created",
                            )
                        }
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isPlaylistSaving = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun stopForProfileChange() {
        playbackController.stop(clearQueue = true, reason = VantafynMusicStopReason.ProfileSwitch)
    }

    fun stopMusic() {
        playbackController.stop(clearQueue = true, reason = VantafynMusicStopReason.User)
    }

    fun pauseForBackground() {
        if (playbackController.state.value.isPlaying) {
            playbackController.pause()
        }
    }

    private fun loadLyrics(trackId: UUID) {
        val activeSession = session ?: return
        val cacheKey = activeSession.lyricsCacheKey(trackId)
        if (lyricsCache.containsKey(cacheKey)) {
            _state.update { it.copy(lyricsTrackId = trackId, lyrics = lyricsCache[cacheKey], isLyricsLoading = false) }
            return
        }
        lyricsJob?.cancel()
        lyricsPrefetchJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _state.update { it.copy(lyricsTrackId = trackId, lyrics = null, isLyricsLoading = true) }
            when (val result = musicRepository.getLyrics(activeSession, trackId)) {
                is JellyfinResult.Success -> _state.update {
                    cacheLyrics(cacheKey, result.value)
                    it.copy(isLyricsLoading = false, lyrics = result.value)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isLyricsLoading = false, lyrics = null)
                }
            }
        }
    }

    private fun prefetchLyrics(trackId: UUID) {
        val activeSession = session ?: return
        val cacheKey = activeSession.lyricsCacheKey(trackId)
        if (lyricsCache.containsKey(cacheKey)) return
        if (lyricsJob?.isActive == true && _state.value.lyricsTrackId == trackId) return
        if (lyricsPrefetchJob?.isActive == true) return
        lyricsPrefetchJob = viewModelScope.launch {
            when (val result = musicRepository.getLyrics(activeSession, trackId)) {
                is JellyfinResult.Success -> {
                    cacheLyrics(cacheKey, result.value)
                    if (_state.value.showLyricsScreen && playbackController.state.value.currentTrack?.id == trackId) {
                        _state.update { it.copy(lyricsTrackId = trackId, lyrics = result.value, isLyricsLoading = false) }
                    }
                }
                is JellyfinResult.Failure -> Unit
            }
        }
    }

    private fun cacheLyrics(key: LyricsCacheKey, lyrics: JellyfinLyrics?) {
        lyricsCache[key] = lyrics
        while (lyricsCache.size > 12) {
            lyricsCache.remove(lyricsCache.keys.first())
        }
    }

    private fun shouldPrefetchLyrics(): Boolean = musicScreenActive && AppForegroundStateRepository.isForeground.value && (_state.value.showNowPlaying || _state.value.showLyricsScreen)

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
            }
            is JellyfinResult.Failure -> {
                null
            }
        }

    private fun handlePlaybackEvent(event: VantafynMusicPlaybackEvent) {
        when (event) {
            is VantafynMusicPlaybackEvent.TrackStarted -> reportStarted(event.track, event.positionMs)
            is VantafynMusicPlaybackEvent.TrackChanged -> {
                event.previousTrack?.let { reportStopped(it, event.previousPositionMs, event.reason) }
                val currentTrack = event.currentTrack
                if (_state.value.showLyricsScreen && currentTrack != null) {
                    loadLyrics(currentTrack.id)
                } else {
                    _state.update { it.copy(lyricsTrackId = null, lyrics = null, isLyricsLoading = false) }
                }
                currentTrack?.let { reportStarted(it, 0L) }
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
        val interval = if (AppForegroundStateRepository.isForeground.value) MusicProgressReportIntervalMs else MusicBackgroundProgressReportIntervalMs
        if (lastProgressTrackId != track.id || now - lastProgressReportMs >= interval) {
            reportProgress(track, playback.positionMs, isPaused = false, force = true)
            lastProgressTrackId = track.id
            lastProgressReportMs = now
        }
    }

    private fun reportStarted(track: VantafynMusicTrack, positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L)
        if (reportedTrackId == track.id) return
        val activeSession = session ?: return
        val info = playbackInfoByTrack[track.id] ?: track.toFallbackPlaybackInfo()
        reportedTrackId = track.id
        lastPausedState = false
        viewModelScope.launch {
            reportResult("start", playbackRepository.reportStarted(activeSession, info, safePosition.toTicks()))
        }
    }

    private fun reportProgress(track: VantafynMusicTrack, positionMs: Long, isPaused: Boolean, force: Boolean = false) {
        val activeSession = session ?: return
        val info = playbackInfoByTrack[track.id] ?: track.toFallbackPlaybackInfo()
        val now = System.currentTimeMillis()
        val interval = if (AppForegroundStateRepository.isForeground.value) MusicProgressReportIntervalMs else MusicBackgroundProgressReportIntervalMs
        if (!force && lastProgressTrackId == track.id && now - lastProgressReportMs < interval) return
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
        viewModelScope.launch {
            reportResult("stop", playbackRepository.reportStopped(activeSession, info, positionMs.toTicks()))
        }
    }

    private fun reportResult(action: String, result: JellyfinResult<Unit>) {
        if (result is JellyfinResult.Failure) {
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
    val isMusicPageLoading: Boolean = false,
    val isPlaylistSaving: Boolean = false,
    val isPlaylistDownloaded: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val home: JellyfinMusicHome? = null,
    val playback: VantafynMusicPlaybackState,
    val pendingPlayTrackId: UUID? = null,
    val showNowPlaying: Boolean = false,
    val showLyricsScreen: Boolean = false,
    val lyricsTrackId: UUID? = null,
    val lyrics: JellyfinLyrics? = null,
    val isLyricsLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<JellyfinMusicTrack> = emptyList(),
    val screen: MusicScreenState = MusicScreenState.Home,
)

private data class LyricsCacheKey(
    val serverId: String,
    val profileId: String,
    val trackId: UUID,
)

private fun JellyfinSession.lyricsCacheKey(trackId: UUID): LyricsCacheKey =
    LyricsCacheKey(
        serverId = server.localId,
        profileId = profileId,
        trackId = trackId,
    )

sealed interface MusicScreenState {
    data object Home : MusicScreenState
    data class Album(val album: JellyfinMusicAlbum, val page: JellyfinMusicTrackPage) : MusicScreenState {
        val tracks: List<JellyfinMusicTrack>
            get() = page.tracks
    }
    data class Artist(val artist: JellyfinMusicArtist, val albums: List<JellyfinMusicAlbum>) : MusicScreenState
    data class Playlist(val playlist: JellyfinMusicPlaylist, val page: JellyfinMusicTrackPage) : MusicScreenState {
        val tracks: List<JellyfinMusicTrack>
            get() = page.tracks
    }
    data class Songs(val page: JellyfinMusicTrackPage) : MusicScreenState {
        val tracks: List<JellyfinMusicTrack>
            get() = page.tracks
    }
}

private fun MusicUiState.musicTrackPageFor(parentId: UUID?, startIndex: Int): JellyfinMusicTrackPage {
    val currentPage = when (val current = screen) {
        is MusicScreenState.Album -> current.page.takeIf { current.album.id == parentId }
        is MusicScreenState.Playlist -> current.page.takeIf { current.playlist.id == parentId }
        is MusicScreenState.Songs -> current.page.takeIf { parentId == null }
        else -> null
    }
    return JellyfinMusicTrackPage(
        tracks = emptyList(),
        startIndex = startIndex.coerceAtLeast(0),
        pageSize = currentPage?.pageSize ?: MusicTrackPageSize,
        totalItems = currentPage?.totalItems ?: 0,
    )
}

private const val MusicTrackPageSize = 60

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
private const val MusicBackgroundProgressReportIntervalMs = 60_000L
private const val KEY_DOWNLOAD_WIFI_ONLY_DEFAULT = "download_wifi_only_default"

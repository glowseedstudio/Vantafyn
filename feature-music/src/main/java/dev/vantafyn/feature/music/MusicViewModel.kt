package dev.vantafyn.feature.music

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vantafyn.core.cast.PlaybackOutputCoordinator
import dev.vantafyn.core.downloads.DownloadFileKind
import dev.vantafyn.core.downloads.DownloadFileStore
import dev.vantafyn.core.downloads.DownloadMediaType
import dev.vantafyn.core.downloads.DownloadOfflineLyricLine
import dev.vantafyn.core.downloads.DownloadOfflineLyrics
import dev.vantafyn.core.downloads.DownloadOfflineManifest
import dev.vantafyn.core.downloads.DownloadState
import dev.vantafyn.core.downloads.OfflineDownloadManager
import dev.vantafyn.core.downloads.SqliteDownloadRepository
import dev.vantafyn.core.downloads.parseOfflineLyrics
import dev.vantafyn.core.downloads.toJsonString
import kotlinx.coroutines.Dispatchers
import java.io.File
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
import dev.vantafyn.core.jellyfin.MusicSongsFilter
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
import dev.vantafyn.core.media.music.MusicStreamingQuality
import dev.vantafyn.core.experience.ExperienceMode
import dev.vantafyn.core.experience.ExperiencePreferences
import dev.vantafyn.core.experience.MusicBackendType
import dev.vantafyn.core.media.music.MusicQualityPreferences
import dev.vantafyn.core.media.music.MusicResult
import dev.vantafyn.core.subsonic.SubsonicClient
import dev.vantafyn.core.subsonic.SubsonicCredentials
import dev.vantafyn.core.subsonic.SubsonicMusicDataProvider
import dev.vantafyn.feature.music.harmonia.HarmoniaGenerationResult
import dev.vantafyn.feature.music.harmonia.HarmoniaGenerator
import dev.vantafyn.feature.music.harmonia.HarmoniaPlaybackTracker
import dev.vantafyn.feature.music.harmonia.HarmoniaRecap
import dev.vantafyn.feature.music.harmonia.HarmoniaRecapPreview
import dev.vantafyn.feature.music.harmonia.SqliteHarmoniaStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    private val downloadRepository = SqliteDownloadRepository(application)
    private val appPreferences = application.getSharedPreferences("vantafyn_app_preferences", Context.MODE_PRIVATE)
    private val playbackController = MusicPlaybackController.get(application)
    private val outputCoordinator = PlaybackOutputCoordinator.get(application)
    private val harmoniaStore = SqliteHarmoniaStore(application)
    private val harmoniaTracker = HarmoniaPlaybackTracker(harmoniaStore)
    private val harmoniaGenerator = HarmoniaGenerator(harmoniaStore, harmoniaStore)
    private var session: JellyfinSession? = null
    private var lyricsJob: Job? = null
    private var downloadStatusJob: Job? = null
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
    private var searchJob: Job? = null
    private val lyricsCache = LinkedHashMap<LyricsCacheKey, JellyfinLyrics?>()

    private val _state = MutableStateFlow(MusicUiState(playback = playbackController.state.value))
    val state: StateFlow<MusicUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            playbackController.state.collect { playback ->
                if (!outputCoordinator.state.value.isCasting && shouldPublishPlaybackToUi(playback)) {
                    _state.update { it.copy(playback = playback) }
                }
                val track = playback.currentTrack
                if (track != null && (_state.value.showLyricsScreen || popupLyricsActive) && track.id != _state.value.lyricsTrackId) {
                    loadLyrics(track.id)
                } else if (track != null && shouldPrefetchLyrics()) {
                    prefetchLyrics(track.id)
                }
                val nextTrack = playback.queue.getOrNull(playback.queueIndex + 1)
                if (nextTrack != null && (_state.value.showLyricsScreen || popupLyricsActive || shouldPrefetchLyrics())) {
                    prefetchLyrics(nextTrack.id)
                }
                maybeReportTimedProgress(playback)
            }
        }
        viewModelScope.launch {
            playbackController.events.collect { event ->
                handlePlaybackEvent(event)
            }
        }
        viewModelScope.launch {
            outputCoordinator.state.collect { output ->
                val cast = output.castState
                val playback = _state.value.playback
                val isCasting = output.isCasting
                if (isCasting && playback.queue.isNotEmpty()) {
                    val castQueueIndex = cast.currentQueueIndex.coerceIn(0, playback.queue.lastIndex.coerceAtLeast(0))
                    val resolvedIndex = if (!cast.currentItemId.isNullOrBlank()) {
                        playback.queue.indexOfFirst { it.id.toString().equals(cast.currentItemId, ignoreCase = true) }
                            .takeIf { it >= 0 } ?: castQueueIndex
                    } else {
                        castQueueIndex
                    }
                    _state.update {
                        it.copy(
                            isCasting = true,
                            castReceiverName = cast.receiverName,
                            castVolume = cast.volume,
                            isCastMuted = cast.isMuted,
                            playback = it.playback.copy(
                                queueIndex = resolvedIndex,
                                isPlaying = cast.isPlaying,
                                positionMs = cast.positionMs,
                                durationMs = cast.durationMs.takeIf { duration -> duration > 0L }
                                    ?: playback.queue.getOrNull(resolvedIndex)?.durationMs
                                    ?: it.playback.durationMs,
                                repeatMode = cast.repeatMode,
                            ),
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isCasting = isCasting,
                            castReceiverName = if (isCasting) cast.receiverName else null,
                            castVolume = cast.volume,
                            isCastMuted = cast.isMuted,
                        )
                    }
                }
            }
        }
        playbackController.radioQueueManager.fetchDelegate = { seedTrack, excludeIds, limit ->
            val activeSession = session
            if (activeSession == null) {
                emptyList()
            } else {
                val result = musicRepository.getSimilarTracks(
                    session = activeSession,
                    trackId = seedTrack.id,
                    limit = limit,
                    excludeTrackIds = excludeIds,
                )
                when (result) {
                    is JellyfinResult.Success -> result.value.map { it.toPlaybackTrack() }
                    is JellyfinResult.Failure -> emptyList()
                }
            }
        }
        viewModelScope.launch {
            playbackController.radioQueueManager.isRadioActive.collect { active ->
                _state.update { it.copy(isRadioActive = active) }
            }
        }
    }

    fun bindSession(session: JellyfinSession?) {
        if (session == null) return
        dev.vantafyn.core.media.VantafynMediaCache.updateJellyfinSession(session)
        if (this.session?.profileId == session.profileId && this.session?.server?.localId == session.server.localId && _state.value.home != null) return
        if (this.session?.profileId != session.profileId || this.session?.server?.localId != session.server.localId) {
            lyricsCache.clear()
            lyricsJob?.cancel()
            lyricsPrefetchJob?.cancel()
        }
        this.session = session
        loadHome()
    }

    private fun getSubsonicCredentials(): SubsonicCredentials? {
        val prefs = getApplication<Application>().getSharedPreferences("vantafyn_subsonic_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("subsonic_url", null) ?: return null
        val user = prefs.getString("subsonic_username", null) ?: return null
        val pass = prefs.getString("subsonic_password", null) ?: return null
        return SubsonicCredentials(url, user, pass)
    }

    fun isSubsonicActive(): Boolean {
        val context = getApplication<Application>()
        val mode = ExperiencePreferences.getExperienceMode(context)
        val backend = ExperiencePreferences.getMusicBackendType(context)
        return mode == ExperienceMode.MusicOnly && backend == MusicBackendType.OpenSubsonic
    }

    fun loadHome() {
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = true, errorMessage = null) }
                    when (val result = provider.getMusicHome()) {
                        is MusicResult.Success -> {
                            val home = result.value
                            val jHome = JellyfinMusicHome(
                                libraries = emptyList(),
                                recentlyAdded = emptyList(),
                                albums = home.recentAlbums.map {
                                    JellyfinMusicAlbum(
                                        id = it.id,
                                        title = it.title,
                                        artist = it.artist,
                                        year = it.year,
                                        artworkUrl = it.coverUrl,
                                    )
                                },
                                artists = home.topArtists.map {
                                    JellyfinMusicArtist(
                                        id = it.id,
                                        name = it.name,
                                        imageUrl = it.imageUrl,
                                    )
                                },
                                playlists = home.playlists.map {
                                    JellyfinMusicPlaylist(
                                        id = it.id,
                                        name = it.title,
                                        imageUrl = it.coverUrl,
                                        trackCount = it.trackCount,
                                    )
                                },
                                songs = home.spotlightTracks.map {
                                    JellyfinMusicTrack(
                                        id = it.id,
                                        title = it.title,
                                        artist = it.artist,
                                        album = it.album,
                                        albumId = it.albumId,
                                        durationMs = it.durationMs,
                                        artworkUrl = it.artworkUrl,
                                        hasLyrics = true,
                                        streamUrl = it.streamUrl,
                                        isFavorite = it.isFavorite,
                                        genres = it.genres,
                                    )
                                },
                            )
                            _state.update { it.copy(isLoading = false, home = jHome, errorMessage = null) }
                            loadRecentlyPlayed()
                        }
                        is MusicResult.Failure -> {
                            _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                        }
                    }
                }
                return
            }
        }

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
            loadHarmoniaPreviews(activeSession)
            loadRecentlyPlayed()
        }
    }

    fun generateTestMonthlyHarmonia(previousCompletedMonth: Boolean = true) {
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(isHarmoniaGenerating = true, harmoniaGenerationMessage = null) }
            val result = harmoniaGenerator.generateMonthly(
                userId = activeSession.user.id,
                serverId = activeSession.server.localId,
                profileId = activeSession.profileId,
                previousCompleted = previousCompletedMonth,
            )
            _state.update {
                it.copy(
                    isHarmoniaGenerating = false,
                    lastHarmoniaGenerationResult = result,
                    harmoniaGenerationMessage = result.message(),
                )
            }
            if (result is HarmoniaGenerationResult.Generated) loadHarmoniaPreviews(activeSession)
        }
    }

    fun generateTestYearlyHarmonia() {
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(isHarmoniaGenerating = true, harmoniaGenerationMessage = null) }
            val result = harmoniaGenerator.generateYearly(
                userId = activeSession.user.id,
                serverId = activeSession.server.localId,
                profileId = activeSession.profileId,
            )
            _state.update {
                it.copy(
                    isHarmoniaGenerating = false,
                    lastHarmoniaGenerationResult = result,
                    harmoniaGenerationMessage = result.message(),
                )
            }
            if (result is HarmoniaGenerationResult.Generated) loadHarmoniaPreviews(activeSession)
        }
    }

    fun openHarmoniaRecap(preview: HarmoniaRecapPreview) {
        val activeSession = session ?: return
        _state.update {
            it.copy(
                screen = MusicScreenState.HarmoniaRecap(preview),
                selectedHarmoniaRecap = null,
                isHarmoniaRecapLoading = true,
                harmoniaRecapError = null,
                harmoniaTopTrack = null,
                isHarmoniaMuted = false,
            )
        }
        viewModelScope.launch {
            when (val result = harmoniaGenerator.loadRecap(
                preview = preview,
                serverId = activeSession.server.localId,
                profileId = activeSession.profileId,
            )) {
                is HarmoniaGenerationResult.Generated -> {
                    _state.update {
                        it.copy(
                            selectedHarmoniaRecap = result.recap,
                            isHarmoniaRecapLoading = false,
                            harmoniaRecapError = null,
                        )
                    }
                    val topTrackRanked = result.recap.statistics.topTracks.value?.firstOrNull()
                    if (topTrackRanked != null) {
                        val trackUuid = runCatching { UUID.fromString(topTrackRanked.id) }.getOrNull()
                        if (trackUuid != null) {
                            val resolved = when (val res = musicRepository.getTrack(activeSession, trackUuid)) {
                                is JellyfinResult.Success -> res.value
                                is JellyfinResult.Failure -> null
                            }
                            if (resolved != null && _state.value.screen is MusicScreenState.HarmoniaRecap) {
                                _state.update { it.copy(harmoniaTopTrack = resolved) }
                                playTrack(resolved, listOf(resolved))
                            }
                        }
                    }
                }
                is HarmoniaGenerationResult.NotEnoughData -> _state.update {
                    it.copy(
                        selectedHarmoniaRecap = null,
                        isHarmoniaRecapLoading = false,
                        harmoniaRecapError = result.reason,
                    )
                }
            }
        }
    }

    fun toggleHarmoniaMute() {
        val newMuted = !_state.value.isHarmoniaMuted
        _state.update { it.copy(isHarmoniaMuted = newMuted) }
        if (newMuted) {
            playbackController.pause()
        } else {
            if (!playbackController.state.value.isPlaying) {
                playbackController.togglePlayPause()
            }
        }
    }

    fun saveHarmoniaRecap(recapId: String, isSaved: Boolean) {
        val activeSession = session ?: return
        viewModelScope.launch {
            harmoniaStore.setRecapSaved(recapId, isSaved)
            loadHarmoniaPreviews(activeSession)
            _state.update { state ->
                val updated = state.selectedHarmoniaRecap?.takeIf { it.id == recapId }?.copy(isSaved = isSaved)
                    ?: state.selectedHarmoniaRecap
                state.copy(selectedHarmoniaRecap = updated)
            }
        }
    }

    fun playTrack(track: JellyfinMusicTrack, queue: List<JellyfinMusicTrack>) {
        if (isSubsonicActive()) {
            val safeQueue = queue.ifEmpty { listOf(track) }
            val tracks = safeQueue.map { it.toPlaybackTrack() }
            val startIndex = safeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            if (outputCoordinator.state.value.isCasting) {
                _state.update {
                    it.copy(
                        playback = it.playback.copy(
                            queue = tracks,
                            queueIndex = startIndex,
                            isPlaying = true,
                            positionMs = 0L,
                            durationMs = tracks.getOrNull(startIndex)?.durationMs ?: 0L,
                        ),
                    )
                }
                outputCoordinator.loadMusicQueue(tracks, startIndex, 0L)
            } else {
                playbackController.playQueue(
                    queue = tracks,
                    startIndex = startIndex,
                )
            }
            return
        }
        val activeSession = session ?: return
        dev.vantafyn.core.media.VantafynMediaCache.updateJellyfinSession(activeSession)
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
                    }
                    val streamUrl = queuedTrack.streamUrl.ifBlank { playbackInfo?.streamUrl.orEmpty() }
                    queuedTrack.toPlaybackTrack(streamUrl = streamUrl)
                }
                val startIndex = safeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                if (outputCoordinator.state.value.isCasting) {
                    _state.update {
                        it.copy(
                            playback = it.playback.copy(
                                queue = preparedQueue,
                                queueIndex = startIndex,
                                isPlaying = true,
                                positionMs = 0L,
                                durationMs = preparedQueue.getOrNull(startIndex)?.durationMs ?: 0L,
                            ),
                        )
                    }
                    outputCoordinator.loadMusicQueue(preparedQueue, startIndex, 0L)
                } else {
                    playbackController.playQueue(
                        queue = preparedQueue,
                        startIndex = startIndex,
                    )
                }
            } finally {
                if (pendingPlayTrackId == track.id) {
                    pendingPlayTrackId = null
                    _state.update { it.copy(pendingPlayTrackId = null) }
                }
            }
        }
    }

    fun setStreamingQuality(quality: MusicStreamingQuality) {
        playbackController.setStreamingQuality(quality)
    }

    fun playAlbum(album: JellyfinMusicAlbum) {
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                viewModelScope.launch {
                    when (val result = provider.getAlbumDetail(album.id)) {
                        is MusicResult.Success -> {
                            val tracks = result.value.tracks.map {
                                JellyfinMusicTrack(
                                    id = it.id,
                                    title = it.title,
                                    artist = it.artist,
                                    album = it.album,
                                    albumId = it.albumId,
                                    durationMs = it.durationMs,
                                    artworkUrl = it.artworkUrl,
                                    hasLyrics = true,
                                    streamUrl = it.streamUrl,
                                    isFavorite = it.isFavorite,
                                    genres = it.genres,
                                )
                            }
                            tracks.firstOrNull()?.let { playTrack(it, tracks) }
                            val curScreen = _state.value.screen
                            if (curScreen !is MusicScreenState.Album || curScreen.album.id != album.id) {
                                openAlbum(album)
                            }
                        }
                        is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                    }
                }
                return
            }
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getAlbumTracks(activeSession, album.id)) {
                is JellyfinResult.Success -> {
                    result.value.firstOrNull()?.let { playTrack(it, result.value) }
                    val curScreen = _state.value.screen
                    if (curScreen !is MusicScreenState.Album || curScreen.album.id != album.id) {
                        openAlbum(album)
                    }
                    if (result.value.isEmpty()) _state.update { it.copy(errorMessage = "No tracks found for this album.") }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun playPlaylist(playlist: JellyfinMusicPlaylist) {
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                viewModelScope.launch {
                    when (val result = provider.getPlaylistDetail(playlist.id)) {
                        is MusicResult.Success -> {
                            val tracks = result.value.tracks.map {
                                JellyfinMusicTrack(
                                    id = it.id,
                                    title = it.title,
                                    artist = it.artist,
                                    album = it.album,
                                    albumId = it.albumId,
                                    durationMs = it.durationMs,
                                    artworkUrl = it.artworkUrl,
                                    hasLyrics = true,
                                    streamUrl = it.streamUrl,
                                    isFavorite = it.isFavorite,
                                    genres = it.genres,
                                )
                            }
                            tracks.firstOrNull()?.let { playTrack(it, tracks) }
                            val curScreen = _state.value.screen
                            if (curScreen !is MusicScreenState.Playlist || curScreen.playlist.id != playlist.id) {
                                openPlaylist(playlist)
                            }
                        }
                        is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                    }
                }
                return
            }
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getPlaylistItems(activeSession, playlist.id)) {
                is JellyfinResult.Success -> {
                    result.value.firstOrNull()?.let { playTrack(it, result.value) }
                    val curScreen = _state.value.screen
                    if (curScreen !is MusicScreenState.Playlist || curScreen.playlist.id != playlist.id) {
                        openPlaylist(playlist)
                    }
                    if (result.value.isEmpty()) _state.update { it.copy(errorMessage = "This playlist is empty.") }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun playPlaylistFromTrack(playlist: JellyfinMusicPlaylist, track: JellyfinMusicTrack) {
        val currentScreen = _state.value.screen
        if (currentScreen is MusicScreenState.Playlist && currentScreen.playlist.id == playlist.id && currentScreen.tracks.isNotEmpty()) {
            playTrack(track, currentScreen.tracks)
            return
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getPlaylistItems(activeSession, playlist.id)) {
                is JellyfinResult.Success -> {
                    playTrack(track, result.value)
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
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                musicPageJob?.cancel()
                musicPageJob = viewModelScope.launch {
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Album(album, it.musicTrackPageFor(album.id, startIndex)),
                            isMusicPageLoading = true,
                            errorMessage = null,
                        )
                    }
                    when (val result = provider.getAlbumDetail(album.id)) {
                        is MusicResult.Success -> {
                            val tracks = result.value.tracks.map {
                                JellyfinMusicTrack(
                                    id = it.id,
                                    title = it.title,
                                    artist = it.artist,
                                    album = it.album,
                                    albumId = it.albumId,
                                    durationMs = it.durationMs,
                                    artworkUrl = it.artworkUrl,
                                    hasLyrics = true,
                                    streamUrl = it.streamUrl,
                                    isFavorite = it.isFavorite,
                                    genres = it.genres,
                                )
                            }
                            val page = JellyfinMusicTrackPage(
                                tracks = tracks,
                                startIndex = 0,
                                pageSize = tracks.size,
                                totalItems = tracks.size,
                            )
                            val updatedAlbum = album.copy(isFavorite = result.value.isFavorite)
                            _state.update {
                                it.copy(
                                    screen = MusicScreenState.Album(updatedAlbum, page),
                                    isMusicPageLoading = false,
                                    home = it.home?.copyWithAlbumFavorite(album.id, result.value.isFavorite),
                                )
                            }
                        }
                        is MusicResult.Failure -> _state.update {
                            it.copy(isMusicPageLoading = false, errorMessage = result.message)
                        }
                    }
                }
                return
            }
        }
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
                    observeAlbumDownloadStatus(activeSession, album.id, result.value.totalItems)
                    val favResult = mediaRepository.refreshFavoriteState(activeSession, album.id)
                    if (favResult is JellyfinResult.Success) {
                        val isFav = favResult.value
                        _state.update { current ->
                            val curScreen = current.screen
                            if (curScreen is MusicScreenState.Album && curScreen.album.id == album.id) {
                                current.copy(
                                    screen = curScreen.copy(album = curScreen.album.copy(isFavorite = isFav)),
                                    home = current.home?.copyWithAlbumFavorite(album.id, isFav),
                                )
                            } else {
                                current
                            }
                        }
                    }
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isMusicPageLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun loadPlaylistPage(playlist: JellyfinMusicPlaylist, startIndex: Int) {
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                musicPageJob?.cancel()
                musicPageJob = viewModelScope.launch {
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Playlist(playlist, it.musicTrackPageFor(playlist.id, startIndex)),
                            isMusicPageLoading = true,
                            errorMessage = null,
                        )
                    }
                    when (val result = provider.getPlaylistDetail(playlist.id)) {
                        is MusicResult.Success -> {
                            val tracks = result.value.tracks.map {
                                JellyfinMusicTrack(
                                    id = it.id,
                                    title = it.title,
                                    artist = it.artist,
                                    album = it.album,
                                    albumId = it.albumId,
                                    durationMs = it.durationMs,
                                    artworkUrl = it.artworkUrl,
                                    hasLyrics = true,
                                    streamUrl = it.streamUrl,
                                    isFavorite = it.isFavorite,
                                    genres = it.genres,
                                )
                            }
                            val page = JellyfinMusicTrackPage(
                                tracks = tracks,
                                startIndex = 0,
                                pageSize = tracks.size,
                                totalItems = tracks.size,
                            )
                            val updatedPlaylist = playlist.copy(isFavorite = result.value.isFavorite)
                            _state.update {
                                it.copy(
                                    screen = MusicScreenState.Playlist(updatedPlaylist, page),
                                    isMusicPageLoading = false,
                                    home = it.home?.copyWithPlaylistFavorite(playlist.id, result.value.isFavorite),
                                )
                            }
                        }
                        is MusicResult.Failure -> _state.update {
                            it.copy(isMusicPageLoading = false, errorMessage = result.message)
                        }
                    }
                }
                return
            }
        }
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
                    observePlaylistDownloadStatus(activeSession, playlist.id, result.value.totalItems)
                    val favResult = mediaRepository.refreshFavoriteState(activeSession, playlist.id)
                    if (favResult is JellyfinResult.Success) {
                        val isFav = favResult.value
                        _state.update { current ->
                            val curScreen = current.screen
                            if (curScreen is MusicScreenState.Playlist && curScreen.playlist.id == playlist.id) {
                                current.copy(
                                    screen = curScreen.copy(playlist = curScreen.playlist.copy(isFavorite = isFav)),
                                    home = current.home?.copyWithPlaylistFavorite(playlist.id, isFav),
                                )
                            } else {
                                current
                            }
                        }
                    }
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isMusicPageLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun observePlaylistDownloadStatus(
        session: JellyfinSession,
        playlistId: UUID,
        trackCount: Int,
    ) {
        downloadStatusJob?.cancel()
        downloadStatusJob = viewModelScope.launch {
            val initial = offlineDownloadManager.getPlaylistDownloadProgress(session, playlistId, trackCount)
            _state.update {
                it.copy(
                    isPlaylistDownloaded = initial.isCompleted,
                    isPlaylistDownloading = initial.isDownloading,
                    playlistDownloadProgress = initial.progress,
                )
            }
            offlineDownloadManager.observePlaylistDownloadProgress(session, playlistId, trackCount)
                .collect { progress ->
                    _state.update {
                        it.copy(
                            isPlaylistDownloaded = progress.isCompleted,
                            isPlaylistDownloading = progress.isDownloading,
                            playlistDownloadProgress = progress.progress,
                        )
                    }
                }
        }
    }

    private fun observeAlbumDownloadStatus(
        session: JellyfinSession,
        albumId: UUID,
        trackCount: Int,
    ) {
        downloadStatusJob?.cancel()
        downloadStatusJob = viewModelScope.launch {
            val initial = offlineDownloadManager.getAlbumDownloadProgress(session, albumId, trackCount)
            _state.update {
                it.copy(
                    isPlaylistDownloaded = initial.isCompleted,
                    isPlaylistDownloading = initial.isDownloading,
                    playlistDownloadProgress = initial.progress,
                )
            }
            offlineDownloadManager.observeAlbumDownloadProgress(session, albumId, trackCount)
                .collect { progress ->
                    _state.update {
                        it.copy(
                            isPlaylistDownloaded = progress.isCompleted,
                            isPlaylistDownloading = progress.isDownloading,
                            playlistDownloadProgress = progress.progress,
                        )
                    }
                }
        }
    }

    fun openArtist(artist: JellyfinMusicArtist) {
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                viewModelScope.launch {
                    _state.update { it.copy(errorMessage = null) }
                    when (val result = provider.getArtistDetail(artist.id)) {
                        is MusicResult.Success -> {
                            val albums = result.value.albums.map {
                                JellyfinMusicAlbum(
                                    id = it.id,
                                    title = it.title,
                                    artist = it.artist,
                                    year = it.year,
                                    artworkUrl = it.coverUrl,
                                )
                            }
                            _state.update { it.copy(screen = MusicScreenState.Artist(artist, albums)) }
                        }
                        is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                    }
                }
                return
            }
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            val albumsDeferred = async { musicRepository.getArtistAlbums(activeSession, artist.id) }
            val similarDeferred = async { musicRepository.getSimilarArtists(activeSession, artist.id, limit = 12) }
            val albumsResult = albumsDeferred.await()
            val similarResult = similarDeferred.await()
            when (albumsResult) {
                is JellyfinResult.Success -> {
                    val similarArtists = if (similarResult is JellyfinResult.Success) similarResult.value else emptyList()
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Artist(
                                artist = artist,
                                albums = albumsResult.value,
                                similarArtists = similarArtists,
                            )
                        )
                    }
                }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = albumsResult.message) }
            }
        }
    }

    fun showSongs() {
        _state.update { it.copy(songsFilter = MusicSongsFilter.All, songsAlphabetKey = null, songsSearchQuery = "") }
        loadSongsPage(startIndex = 0)
    }

    private fun loadSongsPage(startIndex: Int) {
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                val current = _state.value
                val filter = current.songsFilter
                musicPageJob?.cancel()
                musicPageJob = viewModelScope.launch {
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Songs(it.musicTrackPageFor(null, startIndex)),
                            isMusicPageLoading = true,
                            errorMessage = null,
                        )
                    }
                    val result = if (filter == MusicSongsFilter.Favorites) {
                        provider.getStarredSongs()
                    } else {
                        provider.getRandomSongs(size = 50)
                    }
                    when (result) {
                        is MusicResult.Success -> {
                            val tracks = result.value.map {
                                JellyfinMusicTrack(
                                    id = it.id,
                                    title = it.title,
                                    artist = it.artist,
                                    album = it.album,
                                    albumId = it.albumId,
                                    durationMs = it.durationMs,
                                    artworkUrl = it.artworkUrl,
                                    hasLyrics = true,
                                    streamUrl = it.streamUrl,
                                    isFavorite = it.isFavorite,
                                    genres = it.genres,
                                )
                            }
                            val page = JellyfinMusicTrackPage(
                                tracks = tracks,
                                startIndex = 0,
                                pageSize = tracks.size,
                                totalItems = tracks.size,
                            )
                            _state.update {
                                it.copy(screen = MusicScreenState.Songs(page), isMusicPageLoading = false)
                            }
                        }
                        is MusicResult.Failure -> {
                            _state.update {
                                it.copy(isMusicPageLoading = false, errorMessage = result.message)
                            }
                        }
                    }
                }
                return
            }
        }
        val activeSession = session ?: return
        val current = _state.value
        val filter = current.songsFilter
        val alphabetKey = current.songsAlphabetKey.takeIf { filter.supportsAlphabetRail() }?.normalizedSongsAlphabetKey()
        musicPageJob?.cancel()
        musicPageJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    screen = MusicScreenState.Songs(it.musicTrackPageFor(null, startIndex)),
                    isMusicPageLoading = true,
                    errorMessage = null,
                )
            }
            when (val result = musicRepository.getSongsPage(activeSession, startIndex, filter = filter, alphabetKey = alphabetKey)) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(screen = MusicScreenState.Songs(result.value), isMusicPageLoading = false)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isMusicPageLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun loadMoreSongs() {
        val currentScreen = _state.value.screen as? MusicScreenState.Songs ?: return
        if (_state.value.isMusicPageLoading || _state.value.isMusicLoadingMore) return
        val page = currentScreen.page
        if (!page.hasNext) return

        val activeSession = session ?: return
        val current = _state.value
        val filter = current.songsFilter
        val alphabetKey = current.songsAlphabetKey.takeIf { filter.supportsAlphabetRail() }?.normalizedSongsAlphabetKey()
        val nextStartIndex = page.startIndex + page.tracks.size

        viewModelScope.launch {
            _state.update { it.copy(isMusicLoadingMore = true) }
            when (val result = musicRepository.getSongsPage(activeSession, startIndex = nextStartIndex, limit = 100, filter = filter, alphabetKey = alphabetKey)) {
                is JellyfinResult.Success -> {
                    val newTracks = (page.tracks + result.value.tracks).distinctBy { it.id }
                    val updatedPage = JellyfinMusicTrackPage(
                        tracks = newTracks,
                        startIndex = page.startIndex,
                        pageSize = 100,
                        totalItems = result.value.totalItems,
                    )
                    _state.update {
                        it.copy(
                            screen = MusicScreenState.Songs(updatedPage),
                            isMusicLoadingMore = false,
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isMusicLoadingMore = false) }
                }
            }
        }
    }

    fun setSongsFilter(filter: MusicSongsFilter) {
        _state.update { it.copy(songsFilter = filter, songsAlphabetKey = null) }
        loadSongsPage(startIndex = 0)
    }

    fun setSongsAlphabetKey(key: String?) {
        _state.update { it.copy(songsAlphabetKey = key) }
        loadSongsPage(startIndex = 0)
    }

    fun filterSongsLocally(query: String) {
        _state.update { it.copy(songsSearchQuery = query) }
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
        if (_state.value.isReorderMode) {
            _state.update { it.copy(isReorderMode = false) }
            persistPlaylistReorder()
        }
        if (_state.value.screen is MusicScreenState.HarmoniaRecap) {
            _state.update {
                it.copy(
                    screen = MusicScreenState.Home,
                    selectedHarmoniaRecap = null,
                    harmoniaTopTrack = null,
                    isHarmoniaMuted = false,
                )
            }
        } else {
            _state.update { it.copy(screen = MusicScreenState.Home) }
        }
    }

    private suspend fun loadHarmoniaPreviews(activeSession: JellyfinSession) {
        val latest = harmoniaStore.latestPreviews(
            userId = activeSession.user.id,
            serverId = activeSession.server.localId,
            profileId = activeSession.profileId,
        )
        val saved = harmoniaStore.savedPreviews(
            userId = activeSession.user.id,
            serverId = activeSession.server.localId,
            profileId = activeSession.profileId,
        )
        val home = _state.value.home
        val recentlyPlayed = _state.value.recentlyPlayed
        fun resolveArtwork(preview: HarmoniaRecapPreview): HarmoniaRecapPreview {
            if (!preview.artworkUrl.isNullOrBlank()) return preview
            val matchedArt = home?.songs?.firstOrNull { it.title.equals(preview.topTrack, ignoreCase = true) }?.artworkUrl
                ?: home?.recentlyAdded?.firstOrNull { it.title.equals(preview.topTrack, ignoreCase = true) }?.artworkUrl
                ?: home?.albums?.firstOrNull { it.artist.equals(preview.topArtist, ignoreCase = true) }?.artworkUrl
                ?: recentlyPlayed.firstOrNull { it.title.equals(preview.topTrack, ignoreCase = true) }?.artworkUrl
                ?: home?.artists?.firstOrNull { it.name.equals(preview.topArtist, ignoreCase = true) }?.imageUrl
            return if (matchedArt != null) preview.copy(artworkUrl = matchedArt) else preview
        }
        _state.update {
            it.copy(
                harmoniaRecaps = latest.map(::resolveArtwork),
                savedHarmoniaRecaps = saved.map(::resolveArtwork),
            )
        }
    }

    fun search(query: String) {
        val trimmed = query.trim()
        searchJob?.cancel()
        _state.update { it.copy(searchQuery = query, isSearchLoading = trimmed.isNotBlank()) }
        if (trimmed.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), isSearchLoading = false) }
            return
        }
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                searchJob = viewModelScope.launch {
                    delay(300)
                    val provider = createSubsonicProvider(creds)
                    when (val result = provider.searchMusic(trimmed)) {
                        is MusicResult.Success -> {
                            if (_state.value.searchQuery.trim().isNotBlank()) {
                                var tracks = result.value.tracks.map {
                                    JellyfinMusicTrack(
                                        id = it.id,
                                        title = it.title,
                                        artist = it.artist,
                                        album = it.album,
                                        albumId = it.albumId,
                                        durationMs = it.durationMs,
                                        artworkUrl = it.artworkUrl,
                                        hasLyrics = true,
                                        streamUrl = it.streamUrl,
                                        isFavorite = it.isFavorite,
                                        genres = it.genres,
                                    )
                                }
                                if (tracks.isEmpty() && result.value.artists.isNotEmpty()) {
                                    val artistTracks = mutableListOf<JellyfinMusicTrack>()
                                    for (artist in result.value.artists.take(3)) {
                                        when (val artistDetail = provider.getArtistDetail(artist.id)) {
                                            is MusicResult.Success -> {
                                                val candidateTracks = artistDetail.value.topTracks.ifEmpty {
                                                    val firstAlbum = artistDetail.value.albums.firstOrNull()
                                                    if (firstAlbum != null) {
                                                        when (val albumDetail = provider.getAlbumDetail(firstAlbum.id)) {
                                                            is MusicResult.Success -> albumDetail.value.tracks
                                                            is MusicResult.Failure -> emptyList()
                                                        }
                                                    } else emptyList()
                                                }
                                                candidateTracks.forEach { track ->
                                                    artistTracks.add(
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
                                                    )
                                                }
                                            }
                                            is MusicResult.Failure -> Unit
                                        }
                                    }
                                    tracks = artistTracks.distinctBy { it.id }
                                }
                                _state.update { it.copy(searchResults = tracks, isSearchLoading = false) }
                            }
                        }
                        is MusicResult.Failure -> {
                            if (_state.value.searchQuery.trim().isNotBlank()) {
                                _state.update { it.copy(errorMessage = result.message, isSearchLoading = false) }
                            }
                        }
                    }
                }
                return
            }
        }
        val activeSession = session ?: run {
            _state.update { it.copy(isSearchLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            when (val result = musicRepository.searchMusic(activeSession, trimmed)) {
                is JellyfinResult.Success -> {
                    if (_state.value.searchQuery.trim().isNotBlank()) {
                        _state.update { it.copy(searchResults = result.value, isSearchLoading = false) }
                    }
                }
                is JellyfinResult.Failure -> {
                    if (_state.value.searchQuery.trim().isNotBlank()) {
                        _state.update { it.copy(errorMessage = result.message, isSearchLoading = false) }
                    }
                }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearchLoading = false) }
    }

    fun openNowPlaying() {
        _state.update { it.copy(showNowPlaying = true) }
        playbackController.state.value.currentTrack?.id?.let(::prefetchLyrics)
    }

    fun closeNowPlaying() {
        _state.update { it.copy(showNowPlaying = false, showLyricsScreen = false) }
    }

    fun setSleepTimer(minutes: Int) {
        playbackController.setSleepTimer(minutes)
        _state.update { it.copy(message = "Sleep timer set for $minutes min") }
    }

    fun setSleepTimerEndOfTrack() {
        playbackController.setSleepTimerEndOfTrack()
        _state.update { it.copy(message = "Sleep timer: stopping after this track") }
    }

    fun setSleepTimerEndOfQueue() {
        playbackController.setSleepTimerEndOfQueue()
        _state.update { it.copy(message = "Sleep timer: stopping at end of playlist/album") }
    }

    fun cancelSleepTimer() {
        playbackController.cancelSleepTimer()
        _state.update { it.copy(message = "Sleep timer turned off") }
    }

    fun clearUpcomingQueue() {
        playbackController.clearUpcomingQueue()
        _state.update { it.copy(message = "Upcoming queue cleared") }
    }

    fun clearAllQueue() {
        playbackController.clearAllQueue()
        _state.update { it.copy(message = "Queue cleared") }
    }

    fun playTrackNext(track: VantafynMusicTrack) {
        playbackController.playNext(track)
        _state.update { it.copy(message = "Playing next: ${track.title}") }
    }

    fun addTrackToQueue(track: VantafynMusicTrack) {
        playbackController.addToQueue(track)
        _state.update { it.copy(message = "Added to queue: ${track.title}") }
    }

    fun playAlbumNext(albumId: UUID) {
        viewModelScope.launch {
            if (isSubsonicActive()) {
                val creds = getSubsonicCredentials() ?: return@launch
                val provider = createSubsonicProvider(creds)
                when (val result = provider.getAlbumDetail(albumId)) {
                    is MusicResult.Success -> {
                        playbackController.playNextMultiple(result.value.tracks)
                        _state.update { it.copy(message = "Playing next: ${result.value.title}") }
                    }
                    is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            } else {
                val activeSession = session ?: return@launch
                when (val result = musicRepository.getAlbumTracks(activeSession, albumId)) {
                    is JellyfinResult.Success -> {
                        val tracks = result.value.map { it.toPlaybackTrack() }
                        playbackController.playNextMultiple(tracks)
                        _state.update { it.copy(message = "Playing next: Album") }
                    }
                    is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun addAlbumToQueue(albumId: UUID) {
        viewModelScope.launch {
            if (isSubsonicActive()) {
                val creds = getSubsonicCredentials() ?: return@launch
                val provider = createSubsonicProvider(creds)
                when (val result = provider.getAlbumDetail(albumId)) {
                    is MusicResult.Success -> {
                        playbackController.addMultipleToQueue(result.value.tracks)
                        _state.update { it.copy(message = "Added to queue: ${result.value.title}") }
                    }
                    is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            } else {
                val activeSession = session ?: return@launch
                when (val result = musicRepository.getAlbumTracks(activeSession, albumId)) {
                    is JellyfinResult.Success -> {
                        val tracks = result.value.map { it.toPlaybackTrack() }
                        playbackController.addMultipleToQueue(tracks)
                        _state.update { it.copy(message = "Added to queue: Album") }
                    }
                    is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun playArtistNext(artistId: UUID) {
        viewModelScope.launch {
            if (isSubsonicActive()) {
                val creds = getSubsonicCredentials() ?: return@launch
                val provider = createSubsonicProvider(creds)
                when (val result = provider.getArtistDetail(artistId)) {
                    is MusicResult.Success -> {
                        playbackController.playNextMultiple(result.value.topTracks)
                        _state.update { it.copy(message = "Playing next: ${result.value.name}") }
                    }
                    is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            } else {
                val activeSession = session ?: return@launch
                when (val result = musicRepository.getArtistAlbums(activeSession, artistId)) {
                    is JellyfinResult.Success -> {
                        val firstAlbum = result.value.firstOrNull() ?: return@launch
                        when (val trackResult = musicRepository.getAlbumTracks(activeSession, firstAlbum.id)) {
                            is JellyfinResult.Success -> {
                                val tracks = trackResult.value.map { it.toPlaybackTrack() }
                                playbackController.playNextMultiple(tracks)
                                _state.update { it.copy(message = "Playing next: ${firstAlbum.artist ?: "Artist"}") }
                            }
                            is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = trackResult.message) }
                        }
                    }
                    is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun addArtistToQueue(artistId: UUID) {
        viewModelScope.launch {
            if (isSubsonicActive()) {
                val creds = getSubsonicCredentials() ?: return@launch
                val provider = createSubsonicProvider(creds)
                when (val result = provider.getArtistDetail(artistId)) {
                    is MusicResult.Success -> {
                        playbackController.addMultipleToQueue(result.value.topTracks)
                        _state.update { it.copy(message = "Added to queue: ${result.value.name}") }
                    }
                    is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            } else {
                val activeSession = session ?: return@launch
                when (val result = musicRepository.getArtistAlbums(activeSession, artistId)) {
                    is JellyfinResult.Success -> {
                        val firstAlbum = result.value.firstOrNull() ?: return@launch
                        when (val trackResult = musicRepository.getAlbumTracks(activeSession, firstAlbum.id)) {
                            is JellyfinResult.Success -> {
                                val tracks = trackResult.value.map { it.toPlaybackTrack() }
                                playbackController.addMultipleToQueue(tracks)
                                _state.update { it.copy(message = "Added to queue: ${firstAlbum.artist ?: "Artist"}") }
                            }
                            is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = trackResult.message) }
                        }
                    }
                    is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun saveQueueAsPlaylist(title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        val currentQueue = playbackController.state.value.queue
        if (currentQueue.isEmpty()) {
            _state.update { it.copy(errorMessage = "Queue is empty") }
            return
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPlaylistSaving = true) }
            val trackIds = currentQueue.map { it.id }
            when (val result = musicRepository.createPlaylist(activeSession, trimmed, trackIds)) {
                is JellyfinResult.Success -> {
                    refreshHomeForPlaylist(activeSession, trimmed, result.value)
                    _state.update { it.copy(isPlaylistSaving = false, message = "Saved queue as \"$trimmed\"") }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(isPlaylistSaving = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun loadRecentlyPlayed() {
        val activeSession = session ?: return
        viewModelScope.launch {
            val userUuid = activeSession.profileId.let { runCatching { UUID.fromString(it) }.getOrElse { UUID.randomUUID() } }
            val records = harmoniaStore.recentRecords(
                userId = userUuid,
                serverId = activeSession.server.localId,
                profileId = activeSession.profileId,
                limit = 40,
            )
            val tracks = records.distinctBy { it.trackId }.map { rec ->
                VantafynMusicTrack(
                    id = rec.trackId,
                    title = rec.trackTitle,
                    artist = rec.artist,
                    album = rec.album,
                    albumId = rec.albumId,
                    durationMs = rec.durationMs,
                    genres = rec.genres,
                    streamUrl = "",
                    artworkUrl = null,
                )
            }
            _state.update { it.copy(recentlyPlayed = tracks) }
        }
    }

    fun toggleFavorite(track: JellyfinMusicTrack) {
        val targetFavorite = !track.isFavorite
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials() ?: return
            val provider = createSubsonicProvider(creds)
            viewModelScope.launch {
                when (val result = provider.setFavorite(track.id, targetFavorite)) {
                    is MusicResult.Success -> {
                        playbackController.updateFavorite(track.id, targetFavorite)
                        _state.update {
                            it.copy(
                                home = it.home?.copyWithFavorite(track.id, targetFavorite),
                                message = if (targetFavorite) "Added to Favorites" else "Removed from Favorites",
                            )
                        }
                    }
                    is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                }
            }
            return
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = mediaRepository.setFavorite(activeSession, track.id, targetFavorite)) {
                is JellyfinResult.Success -> {
                    playbackController.updateFavorite(track.id, result.value)
                    _state.update {
                        it.copy(
                            home = it.home?.copyWithFavorite(track.id, result.value),
                            message = if (result.value) "Added to Favorites" else "Removed from Favorites",
                        )
                    }
                }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun toggleAlbumFavorite(album: JellyfinMusicAlbum) {
        val targetFavorite = !album.isFavorite
        _state.update { current ->
            val curScreen = current.screen
            val newScreen = if (curScreen is MusicScreenState.Album && curScreen.album.id == album.id) {
                curScreen.copy(album = curScreen.album.copy(isFavorite = targetFavorite))
            } else {
                curScreen
            }
            current.copy(
                screen = newScreen,
                home = current.home?.copyWithAlbumFavorite(album.id, targetFavorite),
            )
        }
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials() ?: return
            val provider = createSubsonicProvider(creds)
            viewModelScope.launch {
                when (val result = provider.setFavorite(album.id, targetFavorite)) {
                    is MusicResult.Success -> {
                        _state.update {
                            it.copy(
                                message = if (targetFavorite) "Added to Favorites" else "Removed from Favorites",
                            )
                        }
                    }
                    is MusicResult.Failure -> {
                        _state.update { current ->
                            val curScreen = current.screen
                            val newScreen = if (curScreen is MusicScreenState.Album && curScreen.album.id == album.id) {
                                curScreen.copy(album = curScreen.album.copy(isFavorite = !targetFavorite))
                            } else {
                                curScreen
                            }
                            current.copy(
                                screen = newScreen,
                                home = current.home?.copyWithAlbumFavorite(album.id, !targetFavorite),
                                errorMessage = result.message,
                            )
                        }
                    }
                }
            }
            return
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = mediaRepository.setFavorite(activeSession, album.id, targetFavorite)) {
                is JellyfinResult.Success -> {
                    _state.update { current ->
                        val curScreen = current.screen
                        val newScreen = if (curScreen is MusicScreenState.Album && curScreen.album.id == album.id) {
                            curScreen.copy(album = curScreen.album.copy(isFavorite = result.value))
                        } else {
                            curScreen
                        }
                        current.copy(
                            screen = newScreen,
                            home = current.home?.copyWithAlbumFavorite(album.id, result.value),
                            message = if (result.value) "Added to Favorites" else "Removed from Favorites",
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { current ->
                        val curScreen = current.screen
                        val newScreen = if (curScreen is MusicScreenState.Album && curScreen.album.id == album.id) {
                            curScreen.copy(album = curScreen.album.copy(isFavorite = !targetFavorite))
                        } else {
                            curScreen
                        }
                        current.copy(
                            screen = newScreen,
                            home = current.home?.copyWithAlbumFavorite(album.id, !targetFavorite),
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun togglePlaylistFavorite(playlist: JellyfinMusicPlaylist) {
        val targetFavorite = !playlist.isFavorite
        _state.update { current ->
            val curScreen = current.screen
            val newScreen = if (curScreen is MusicScreenState.Playlist && curScreen.playlist.id == playlist.id) {
                curScreen.copy(playlist = curScreen.playlist.copy(isFavorite = targetFavorite))
            } else {
                curScreen
            }
            current.copy(
                screen = newScreen,
                home = current.home?.copyWithPlaylistFavorite(playlist.id, targetFavorite),
            )
        }
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials() ?: return
            val provider = createSubsonicProvider(creds)
            viewModelScope.launch {
                when (val result = provider.setFavorite(playlist.id, targetFavorite)) {
                    is MusicResult.Success -> {
                        _state.update {
                            it.copy(
                                message = if (targetFavorite) "Added to Favorites" else "Removed from Favorites",
                            )
                        }
                    }
                    is MusicResult.Failure -> {
                        _state.update { current ->
                            val curScreen = current.screen
                            val newScreen = if (curScreen is MusicScreenState.Playlist && curScreen.playlist.id == playlist.id) {
                                curScreen.copy(playlist = curScreen.playlist.copy(isFavorite = !targetFavorite))
                            } else {
                                curScreen
                            }
                            current.copy(
                                screen = newScreen,
                                home = current.home?.copyWithPlaylistFavorite(playlist.id, !targetFavorite),
                                errorMessage = result.message,
                            )
                        }
                    }
                }
            }
            return
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = mediaRepository.setFavorite(activeSession, playlist.id, targetFavorite)) {
                is JellyfinResult.Success -> {
                    _state.update { current ->
                        val curScreen = current.screen
                        val newScreen = if (curScreen is MusicScreenState.Playlist && curScreen.playlist.id == playlist.id) {
                            curScreen.copy(playlist = curScreen.playlist.copy(isFavorite = result.value))
                        } else {
                            curScreen
                        }
                        current.copy(
                            screen = newScreen,
                            home = current.home?.copyWithPlaylistFavorite(playlist.id, result.value),
                            message = if (result.value) "Added to Favorites" else "Removed from Favorites",
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { current ->
                        val curScreen = current.screen
                        val newScreen = if (curScreen is MusicScreenState.Playlist && curScreen.playlist.id == playlist.id) {
                            curScreen.copy(playlist = curScreen.playlist.copy(isFavorite = !targetFavorite))
                        } else {
                            curScreen
                        }
                        current.copy(
                            screen = newScreen,
                            home = current.home?.copyWithPlaylistFavorite(playlist.id, !targetFavorite),
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun downloadAlbum(albumId: UUID) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getAlbumTracks(activeSession, albumId)) {
                is JellyfinResult.Success -> {
                    val album = state.value.home?.albums?.find { it.id == albumId } ?: JellyfinMusicAlbum(albumId, "Album", null, null, null)
                    queueAlbumDownload(album, result.value)
                }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
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
        if (track != null && (track.id != _state.value.lyricsTrackId || _state.value.lyrics == null)) {
            loadLyrics(track.id)
        }
    }

    fun setPopupLyricsActive(active: Boolean) {
        popupLyricsActive = active
        if (active) {
            playbackController.state.value.currentTrack?.id?.let { trackId ->
                if (trackId != _state.value.lyricsTrackId || _state.value.lyrics == null) loadLyrics(trackId)
            }
        }
    }

    fun closeLyrics() {
        _state.update { it.copy(showLyricsScreen = false) }
    }

    fun togglePlayPause() {
        if (outputCoordinator.state.value.isCasting) {
            outputCoordinator.playPause()
        } else {
            playbackController.togglePlayPause()
        }
    }

    fun next() {
        if (outputCoordinator.state.value.isCasting) {
            outputCoordinator.next()
        } else {
            playbackController.next()
        }
    }

    fun previous() {
        if (outputCoordinator.state.value.isCasting) {
            outputCoordinator.previous()
        } else {
            playbackController.previous()
        }
    }

    fun playQueueIndex(index: Int) {
        if (outputCoordinator.state.value.isCasting) {
            outputCoordinator.playQueueIndex(index)
        } else {
            playbackController.playQueueIndex(index)
        }
    }

    fun playQueueItemNext(index: Int) {
        val track = playbackController.state.value.queue.getOrNull(index) ?: return
        playbackController.removeFromQueue(index)
        playbackController.playNext(track)
        _state.update { it.copy(message = "Queued next") }
    }

    fun removeQueueItem(index: Int) {
        playbackController.removeFromQueue(index)
        _state.update { it.copy(message = "Removed from queue") }
    }

    fun moveQueueTrack(fromIndex: Int, toIndex: Int) {
        playbackController.moveQueueTrack(fromIndex, toIndex)
    }

    fun openAlbumById(albumId: UUID) {
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                viewModelScope.launch {
                    when (val result = provider.getAlbumDetail(albumId)) {
                        is MusicResult.Success -> {
                            val album = JellyfinMusicAlbum(
                                id = result.value.id,
                                title = result.value.title,
                                artist = result.value.artist,
                                year = result.value.year,
                                artworkUrl = result.value.coverUrl,
                            )
                            openAlbum(album)
                        }
                        else -> {}
                    }
                }
                return
            }
        }
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getArtistAlbums(activeSession, albumId)) {
                is JellyfinResult.Success -> {
                    val album = result.value.firstOrNull { it.id == albumId }
                    if (album != null) openAlbum(album)
                }
                else -> {}
            }
        }
    }

    fun seekTo(positionMs: Long) {
        if (outputCoordinator.state.value.isCasting) {
            outputCoordinator.seekTo(positionMs)
        } else {
            playbackController.seekTo(positionMs)
        }
    }
    fun currentPlaybackPositionMs(): Long =
        if (outputCoordinator.state.value.isCasting) {
            outputCoordinator.state.value.castState.positionMs
        } else {
            playbackController.currentPositionMs()
        }
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun cycleRepeat() = playbackController.cycleRepeatMode()
    fun setCastVolume(volume: Float) = outputCoordinator.setVolume(volume)
    fun toggleCastMute() = outputCoordinator.toggleMute()
    fun playNext(track: JellyfinMusicTrack) {
        playbackController.playNext(track.toPlaybackTrack())
        _state.update { it.copy(message = "Queued next") }
    }

    fun addToQueue(track: JellyfinMusicTrack) {
        playbackController.addToQueue(track.toPlaybackTrack())
        _state.update { it.copy(message = "Added to queue") }
    }

    val isRadioActive: StateFlow<Boolean> = playbackController.radioQueueManager.isRadioActive

    fun startRadio(track: VantafynMusicTrack) {
        playbackController.radioQueueManager.startRadio(track)
        _state.update { it.copy(message = "Starting Radio for ${track.title}") }
    }

    fun startRadio(track: JellyfinMusicTrack) {
        playbackController.radioQueueManager.startRadio(track.toPlaybackTrack())
        _state.update { it.copy(message = "Starting Radio for ${track.title}") }
    }

    fun startAlbumRadio(album: JellyfinMusicAlbum) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getAlbumTracks(activeSession, album.id)) {
                is JellyfinResult.Success -> {
                    val firstTrack = result.value.firstOrNull()
                    if (firstTrack != null) {
                        startRadio(firstTrack)
                    } else {
                        _state.update { it.copy(message = "No tracks found in album") }
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun startArtistRadio(artist: JellyfinMusicArtist) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getArtistAlbums(activeSession, artist.id)) {
                is JellyfinResult.Success -> {
                    val firstAlbum = result.value.firstOrNull()
                    if (firstAlbum != null) {
                        when (val trackResult = musicRepository.getAlbumTracks(activeSession, firstAlbum.id)) {
                            is JellyfinResult.Success -> {
                                val firstTrack = trackResult.value.firstOrNull()
                                if (firstTrack != null) {
                                    startRadio(firstTrack)
                                } else {
                                    _state.update { it.copy(message = "No tracks found for ${artist.name}") }
                                }
                            }
                            is JellyfinResult.Failure -> {
                                _state.update { it.copy(message = trackResult.message) }
                            }
                        }
                    } else {
                        _state.update { it.copy(message = "No albums found for ${artist.name}") }
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    fun stopRadio() {
        playbackController.radioQueueManager.stopRadio("user_requested")
        _state.update { it.copy(message = "Radio ended") }
    }

    fun selectHomeMood(mood: MusicHomeMood) {
        _state.update { current ->
            val nextMood = if (current.selectedHomeMood == mood) MusicHomeMood.All else mood
            current.copy(selectedHomeMood = nextMood)
        }
    }

    fun shuffleTracks(tracks: List<JellyfinMusicTrack>) {
        if (tracks.isEmpty()) return
        val shuffled = tracks.shuffled()
        playTrack(shuffled.first(), shuffled)
    }

    private var playlistInitialTracks: List<JellyfinMusicTrack>? = null

    fun toggleReorderMode() {
        val currentReorder = _state.value.isReorderMode
        if (!currentReorder) {
            val screen = _state.value.screen as? MusicScreenState.Playlist
            playlistInitialTracks = screen?.page?.tracks?.toList()
            _state.update { it.copy(isReorderMode = true) }
        } else {
            _state.update { it.copy(isReorderMode = false) }
            persistPlaylistReorder()
        }
    }

    fun movePlaylistTrack(fromIndex: Int, toIndex: Int) {
        val screen = _state.value.screen as? MusicScreenState.Playlist ?: return
        val tracks = screen.page.tracks.toMutableList()
        if (fromIndex !in tracks.indices || toIndex !in tracks.indices || fromIndex == toIndex) return
        val track = tracks.removeAt(fromIndex)
        tracks.add(toIndex, track)
        _state.update { it.copy(screen = screen.copy(page = screen.page.copy(tracks = tracks))) }
    }

    private fun persistPlaylistReorder() {
        val initial = playlistInitialTracks ?: return
        playlistInitialTracks = null
        val activeSession = session ?: return
        val screen = _state.value.screen as? MusicScreenState.Playlist ?: return
        val currentTracks = screen.page.tracks
        if (initial == currentTracks) return

        viewModelScope.launch {
            val workingList = initial.toMutableList()
            var anyFailed = false

            for (targetIndex in currentTracks.indices) {
                val targetTrack = currentTracks[targetIndex]
                val currentPos = workingList.indexOfFirst {
                    if (it.playlistItemId != null && targetTrack.playlistItemId != null) {
                        it.playlistItemId == targetTrack.playlistItemId
                    } else {
                        it.id == targetTrack.id
                    }
                }
                if (currentPos != -1 && currentPos != targetIndex) {
                    val playlistItemId = targetTrack.playlistItemId ?: targetTrack.id.toString()
                    val globalIndex = screen.page.startIndex + targetIndex
                    val result = musicRepository.movePlaylistItem(activeSession, screen.playlist.id, playlistItemId, globalIndex)
                    if (result is JellyfinResult.Success) {
                        val moved = workingList.removeAt(currentPos)
                        workingList.add(targetIndex, moved)
                    } else {
                        anyFailed = true
                        break
                    }
                }
            }

            if (anyFailed) {
                _state.update { it.copy(errorMessage = "Could not sync playlist order to server.") }
            }

            val refreshed = musicRepository.getPlaylistItemsPage(activeSession, screen.playlist.id, screen.page.startIndex)
            if (refreshed is JellyfinResult.Success) {
                val latestScreen = _state.value.screen as? MusicScreenState.Playlist
                if (latestScreen != null && latestScreen.playlist.id == screen.playlist.id) {
                    _state.update { it.copy(screen = latestScreen.copy(page = refreshed.value)) }
                }
            }
        }
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
            _state.update {
                it.copy(
                    isPlaylistDownloading = true,
                    playlistDownloadProgress = 0f,
                    errorMessage = null,
                    message = null,
                )
            }
            val downloadTracks = when (val result = musicRepository.getAlbumTracks(activeSession, album.id)) {
                is JellyfinResult.Success -> result.value.ifEmpty { tracks }
                is JellyfinResult.Failure -> tracks
            }
            when (val result = offlineDownloadManager.queueMusicAlbum(activeSession, album, downloadTracks, requireWifi = readDownloadWifiOnlyDefault())) {
                is JellyfinResult.Success -> {
                    _state.update { it.copy(message = "${result.value} tracks queued") }
                    observeAlbumDownloadStatus(activeSession, album.id, downloadTracks.size)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isPlaylistDownloading = false,
                        errorMessage = result.message,
                    )
                }
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
            _state.update {
                it.copy(
                    isPlaylistDownloading = true,
                    playlistDownloadProgress = 0f,
                    errorMessage = null,
                    message = null,
                )
            }
            val downloadTracks = when (val result = musicRepository.getPlaylistItems(activeSession, playlist.id)) {
                is JellyfinResult.Success -> result.value.ifEmpty { tracks }
                is JellyfinResult.Failure -> tracks
            }
            when (
                val result = offlineDownloadManager.queueMusicPlaylist(
                    session = activeSession,
                    playlist = playlist,
                    tracks = downloadTracks,
                    requireWifi = readDownloadWifiOnlyDefault(),
                )
            ) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(message = "${result.value} tracks queued as ${playlist.name}", errorMessage = null)
                }.also {
                    observePlaylistDownloadStatus(activeSession, playlist.id, downloadTracks.size)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(
                        isPlaylistDownloading = false,
                        message = null,
                        errorMessage = result.message,
                    )
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
        val current = playbackController.state.value.currentTrack ?: return
        val targetFavorite = !current.isFavorite
        if (isSubsonicActive()) {
            val creds = getSubsonicCredentials()
            if (creds != null) {
                val provider = createSubsonicProvider(creds)
                viewModelScope.launch {
                    when (val result = provider.setFavorite(current.id, targetFavorite)) {
                        is MusicResult.Success -> {
                            playbackController.updateFavorite(current.id, targetFavorite)
                            _state.update { state ->
                                state.copy(
                                    home = state.home?.copyWithFavorite(current.id, targetFavorite),
                                    message = if (targetFavorite) "Added to Favorites" else "Removed from Favorites",
                                )
                            }
                        }
                        is MusicResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
                    }
                }
                return
            }
        }
        val activeSession = session ?: return
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
        val current = playbackController.state.value.currentTrack ?: return
        addTracksToPlaylist(playlist, listOf(current.toJellyfinTrack()))
    }

    fun addTrackToPlaylist(track: JellyfinMusicTrack, playlist: JellyfinMusicPlaylist) {
        addTracksToPlaylist(playlist, listOf(track))
    }

    fun addTracksToPlaylist(
        playlist: JellyfinMusicPlaylist,
        tracks: List<JellyfinMusicTrack>,
        bypassDuplicateCheck: Boolean = false,
    ) {
        val activeSession = session ?: return
        if (tracks.isEmpty()) return

        if (!bypassDuplicateCheck) {
            viewModelScope.launch {
                _state.update { it.copy(isCheckingPlaylistDuplicates = true, errorMessage = null) }
                val existingTracks = when (val res = musicRepository.getPlaylistItems(activeSession, playlist.id)) {
                    is JellyfinResult.Success -> res.value
                    is JellyfinResult.Failure -> emptyList()
                }
                _state.update { it.copy(isCheckingPlaylistDuplicates = false) }

                if (existingTracks.isNotEmpty()) {
                    val existingIds = existingTracks.map { it.id }.toSet()
                    val duplicates = tracks.filter { existingIds.contains(it.id) }
                    val newOnes = tracks.filterNot { existingIds.contains(it.id) }
                    if (duplicates.isNotEmpty()) {
                        _state.update {
                            it.copy(
                                playlistDuplicatePrompt = PlaylistDuplicatePrompt(
                                    playlist = playlist,
                                    candidateTracks = tracks,
                                    duplicateTracks = duplicates,
                                    newTracks = newOnes,
                                )
                            )
                        }
                        return@launch
                    }
                }

                performAddToPlaylist(activeSession, playlist, tracks)
            }
        } else {
            viewModelScope.launch {
                performAddToPlaylist(activeSession, playlist, tracks)
            }
        }
    }

    private suspend fun performAddToPlaylist(
        activeSession: dev.vantafyn.core.jellyfin.JellyfinSession,
        playlist: JellyfinMusicPlaylist,
        tracks: List<JellyfinMusicTrack>,
    ) {
        when (val result = musicRepository.addToPlaylist(activeSession, playlist.id, tracks.map { it.id })) {
            is JellyfinResult.Success -> {
                val count = tracks.size
                val msg = if (count == 1) {
                    "Added \"${tracks.first().title}\" to ${playlist.name}"
                } else {
                    "Added $count tracks to ${playlist.name}"
                }
                _state.update {
                    it.copy(
                        message = msg,
                        playlistDuplicatePrompt = null,
                        home = it.home?.incrementPlaylistCount(playlist.id, delta = count),
                    )
                }
            }
            is JellyfinResult.Failure -> {
                _state.update { it.copy(errorMessage = result.message, playlistDuplicatePrompt = null) }
            }
        }
    }

    fun confirmDuplicateAddToPlaylist(playlist: JellyfinMusicPlaylist, tracks: List<JellyfinMusicTrack>) {
        addTracksToPlaylist(playlist, tracks, bypassDuplicateCheck = true)
    }

    fun dismissPlaylistDuplicatePrompt() {
        _state.update { it.copy(playlistDuplicatePrompt = null) }
    }

    fun removeTracksFromPlaylist(playlist: JellyfinMusicPlaylist, tracks: List<JellyfinMusicTrack>) {
        val activeSession = session ?: return
        if (tracks.isEmpty()) return

        viewModelScope.launch {
            val itemIds = tracks.mapNotNull { it.playlistItemId ?: it.id.toString() }
            when (val result = musicRepository.removeFromPlaylist(activeSession, playlist.id, itemIds)) {
                is JellyfinResult.Success -> {
                    val removedTrackIds = tracks.map { it.id }.toSet()
                    val removedCount = tracks.size
                    _state.update { state ->
                        val updatedScreen = if (state.screen is MusicScreenState.Playlist && state.screen.playlist.id == playlist.id) {
                            val currentTracks = state.screen.tracks
                            val remainingTracks = currentTracks.filterNot { removedTrackIds.contains(it.id) }
                            state.screen.copy(
                                page = state.screen.page.copy(
                                    tracks = remainingTracks,
                                    totalItems = (state.screen.page.totalItems - removedCount).coerceAtLeast(0)
                                )
                            )
                        } else {
                            state.screen
                        }
                        state.copy(
                            screen = updatedScreen,
                            message = if (removedCount == 1) "Removed from ${playlist.name}" else "Removed $removedCount tracks from ${playlist.name}",
                            home = state.home?.decrementPlaylistCount(playlist.id, count = removedCount),
                        )
                    }
                }
                is JellyfinResult.Failure -> {
                    _state.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun createPlaylistAndAddTracks(name: String, tracks: List<JellyfinMusicTrack>) {
        val activeSession = session ?: return
        val trackIds = tracks.map { it.id }
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

    fun createPlaylistAndAddTrack(name: String, track: JellyfinMusicTrack?) {
        createPlaylistAndAddTracks(name, listOfNotNull(track))
    }

    fun showMessage(message: String) {
        _state.update { it.copy(message = message) }
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

    private suspend fun loadOfflineLyrics(trackId: UUID): JellyfinLyrics? {
        val currentTrack = playbackController.state.value.currentTrack
        val trackIdStr = trackId.toString()

        var record = downloadRepository.getByItemId(trackIdStr)

        if (record == null) {
            val completed = runCatching { downloadRepository.listAllCompleted() }.getOrNull().orEmpty()
            record = completed.firstOrNull { r ->
                r.mediaType == DownloadMediaType.MusicTrack && (
                    r.identity.itemId == trackIdStr ||
                    r.id == trackIdStr ||
                    runCatching { UUID.fromString(r.identity.itemId) }.getOrNull() == trackId ||
                    UUID.nameUUIDFromBytes(r.identity.itemId.toByteArray()) == trackId ||
                    UUID.nameUUIDFromBytes(r.id.toByteArray()) == trackId ||
                    (currentTrack != null && (
                        (r.localMediaPath?.let { path -> currentTrack.streamUrl.contains(File(path).name) } == true) ||
                        (r.title.equals(currentTrack.title, ignoreCase = true) &&
                         (r.artistName.isNullOrBlank() || r.artistName.equals(currentTrack.artist, ignoreCase = true)))
                    ))
                )
            }
        }

        if (record == null) return null

        val lyricsFile = record.localLyricsPath?.let(::File)?.takeIf { it.exists() && it.length() > 0L }
        val metadataFile = record.localMetadataPath?.let(::File)?.takeIf { it.exists() && it.length() > 0L }

        for (file in listOfNotNull(lyricsFile, metadataFile)) {
            val text = runCatching { file.readText() }.getOrNull() ?: continue
            val offlineLyrics = parseOfflineLyrics(text)
            if (offlineLyrics != null) {
                val synced = offlineLyrics.syncedLines.map { line ->
                    JellyfinLyricLine(startMs = line.startMs?.sanitizeLyricMillis(), text = line.text)
                }
                val plain = offlineLyrics.plainText.ifBlank {
                    synced.joinToString("\n") { it.text }
                }
                if (plain.isNotBlank() || synced.isNotEmpty()) {
                    return JellyfinLyrics(
                        plainText = plain,
                        syncedLines = synced,
                        source = "Offline Download",
                    )
                }
            }
        }

        val audioFile = record.localMediaPath?.let(::File)?.takeIf { it.exists() }
        if (audioFile != null) {
            val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists() && lrcFile.length() > 0L) {
                val lrcText = runCatching { lrcFile.readText() }.getOrNull()
                if (!lrcText.isNullOrBlank()) {
                    val offlineLyrics = parseOfflineLyrics(lrcText)
                    if (offlineLyrics != null) {
                        val synced = offlineLyrics.syncedLines.map { line ->
                            JellyfinLyricLine(startMs = line.startMs?.sanitizeLyricMillis(), text = line.text)
                        }
                        val plain = offlineLyrics.plainText.ifBlank {
                            synced.joinToString("\n") { it.text }
                        }
                        if (plain.isNotBlank() || synced.isNotEmpty()) {
                            return JellyfinLyrics(
                                plainText = plain,
                                syncedLines = synced,
                                source = "Offline Download",
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    private fun maybePersistOfflineLyrics(trackId: UUID, lyrics: JellyfinLyrics) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = downloadRepository.getByItemId(trackId.toString()) ?: return@launch
            if (record.state != DownloadState.Completed) return@launch
            if (record.localLyricsPath?.let(::File)?.exists() == true) return@launch

            val offlineLyrics = DownloadOfflineLyrics(
                plainText = lyrics.plainText,
                syncedLines = lyrics.syncedLines.map { line ->
                    DownloadOfflineLyricLine(startMs = line.startMs, text = line.text)
                },
            )
            val fileStore = DownloadFileStore(getApplication())
            val target = fileStore.targetFor(record.identity, DownloadFileKind.Lyrics, "json")
            if (target.tempFile.parentFile?.let { it.exists() || it.mkdirs() } == true) {
                target.tempFile.writeText(
                    DownloadOfflineManifest(
                        itemId = record.identity.itemId,
                        title = record.title,
                        generatedAtMillis = System.currentTimeMillis(),
                        lyrics = offlineLyrics,
                    ).toJsonString(),
                )
                if (target.finalFile.exists()) target.finalFile.delete()
                if (target.tempFile.renameTo(target.finalFile) || runCatching {
                        target.tempFile.copyTo(target.finalFile, overwrite = true)
                        target.tempFile.delete()
                    }.isSuccess
                ) {
                    val updatedFlags = buildList {
                        record.offlineFeatureFlags?.split(",")?.filter { it.isNotBlank() }?.let { addAll(it) }
                        if (!contains("lyrics")) add("lyrics")
                    }.joinToString(",")
                    downloadRepository.updateLocalSidecarPaths(
                        id = record.id,
                        localSubtitlePath = record.localSubtitlePath,
                        localMetadataPath = record.localMetadataPath,
                        localLyricsPath = target.finalFile.absolutePath,
                        localChaptersPath = record.localChaptersPath,
                        localTrickplayPath = record.localTrickplayPath,
                        offlineFeatureFlags = updatedFlags,
                        updatedAtMillis = System.currentTimeMillis(),
                    )
                }
            }
        }
    }

    fun loadLyrics(trackId: UUID) {
        val cacheKey = session?.lyricsCacheKey(trackId) ?: LyricsCacheKey(trackId = trackId)
        val cached = lyricsCache[cacheKey] ?: lyricsCache[LyricsCacheKey(trackId = trackId)]
        if (cached != null) {
            _state.update { it.copy(lyricsTrackId = trackId, lyrics = cached, isLyricsLoading = false) }
            return
        }

        lyricsJob?.cancel()
        lyricsPrefetchJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _state.update { it.copy(lyricsTrackId = trackId, lyrics = null, isLyricsLoading = true) }

            val offlineLyrics = loadOfflineLyrics(trackId)
            if (offlineLyrics != null) {
                cacheLyrics(cacheKey, offlineLyrics)
                cacheLyrics(LyricsCacheKey(trackId = trackId), offlineLyrics)
                _state.update { it.copy(lyricsTrackId = trackId, lyrics = offlineLyrics, isLyricsLoading = false) }
                return@launch
            }

            if (isSubsonicActive()) {
                val creds = getSubsonicCredentials()
                if (creds != null) {
                    val provider = createSubsonicProvider(creds)
                    when (val result = provider.getLyrics(trackId)) {
                        is MusicResult.Success -> {
                            val l = result.value
                            val jLyrics = if (l != null) {
                                JellyfinLyrics(
                                    syncedLines = l.syncedLines.map {
                                        JellyfinLyricLine(startMs = it.startMs, text = it.text)
                                    },
                                    plainText = l.plainText.orEmpty(),
                                    source = "Subsonic",
                                )
                            } else null
                            cacheLyrics(cacheKey, jLyrics)
                            _state.update { it.copy(isLyricsLoading = false, lyrics = jLyrics) }
                            if (jLyrics != null) maybePersistOfflineLyrics(trackId, jLyrics)
                        }
                        is MusicResult.Failure -> {
                            _state.update { it.copy(isLyricsLoading = false, lyrics = null) }
                        }
                    }
                    return@launch
                }
            }

            var activeSession = session
            if (activeSession == null) {
                val authRepository = repositories.authRepository
                val profiles = authRepository.savedProfiles()
                val activeProfileId = appPreferences.getString("active_profile_id", null) ?: profiles.firstOrNull()?.id
                if (activeProfileId != null) {
                    when (val result = authRepository.restoreSession(activeProfileId)) {
                        is JellyfinResult.Success -> {
                            activeSession = result.value
                            this@MusicViewModel.session = result.value
                        }
                        else -> Unit
                    }
                }
            }

            if (activeSession == null) {
                _state.update { it.copy(isLyricsLoading = false, lyrics = null) }
                return@launch
            }

            when (val result = musicRepository.getLyrics(activeSession, trackId)) {
                is JellyfinResult.Success -> {
                    val lyricValue = result.value
                    cacheLyrics(cacheKey, lyricValue)
                    _state.update {
                        it.copy(isLyricsLoading = false, lyrics = lyricValue)
                    }
                    if (lyricValue != null) maybePersistOfflineLyrics(trackId, lyricValue)
                }
                is JellyfinResult.Failure -> {
                    _state.update {
                        it.copy(isLyricsLoading = false, lyrics = null)
                    }
                }
            }
        }
    }

    private fun prefetchLyrics(trackId: UUID) {
        val cacheKey = session?.lyricsCacheKey(trackId) ?: LyricsCacheKey(trackId = trackId)
        if (lyricsCache.containsKey(cacheKey) || lyricsCache.containsKey(LyricsCacheKey(trackId = trackId))) return
        if (lyricsJob?.isActive == true && _state.value.lyricsTrackId == trackId) return
        if (lyricsPrefetchJob?.isActive == true) return
        lyricsPrefetchJob = viewModelScope.launch {
            val offlineLyrics = loadOfflineLyrics(trackId)
            if (offlineLyrics != null) {
                cacheLyrics(cacheKey, offlineLyrics)
                cacheLyrics(LyricsCacheKey(trackId = trackId), offlineLyrics)
                if ((_state.value.showLyricsScreen || popupLyricsActive) && playbackController.state.value.currentTrack?.id == trackId) {
                    _state.update { it.copy(lyricsTrackId = trackId, lyrics = offlineLyrics, isLyricsLoading = false) }
                }
                return@launch
            }
            val activeSession = session ?: return@launch
            when (val result = musicRepository.getLyrics(activeSession, trackId)) {
                is JellyfinResult.Success -> {
                    val lyricValue = result.value
                    cacheLyrics(cacheKey, lyricValue)
                    if ((_state.value.showLyricsScreen || popupLyricsActive) && playbackController.state.value.currentTrack?.id == trackId) {
                        _state.update { it.copy(lyricsTrackId = trackId, lyrics = lyricValue, isLyricsLoading = false) }
                    }
                    if (lyricValue != null) maybePersistOfflineLyrics(trackId, lyricValue)
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

    private fun createSubsonicProvider(creds: SubsonicCredentials): SubsonicMusicDataProvider =
        SubsonicMusicDataProvider(SubsonicClient(creds), getApplication())

    private suspend fun preparePlaybackInfo(activeSession: JellyfinSession, track: JellyfinMusicTrack): JellyfinPlaybackInfo? {
        val currentQuality = MusicQualityPreferences.resolveCurrentQuality(getApplication())
        return when (
            val result = playbackRepository.getPlaybackInfo(
                session = activeSession,
                itemId = track.id,
                title = track.title,
                subtitle = track.artist,
                maxStreamingBitrate = currentQuality.maxBitrateBps,
            )
        ) {
            is JellyfinResult.Success -> result.value
            is JellyfinResult.Failure -> null
        }
    }

    private fun handlePlaybackEvent(event: VantafynMusicPlaybackEvent) {
        when (event) {
            is VantafynMusicPlaybackEvent.TrackStarted -> {
                trackHarmoniaStart(event.track, event.positionMs)
                reportStarted(event.track, event.positionMs)
            }
            is VantafynMusicPlaybackEvent.TrackChanged -> {
                event.previousTrack?.let {
                    trackHarmoniaStop(it, event.previousPositionMs, event.reason)
                    reportStopped(it, event.previousPositionMs, event.reason)
                }
                val currentTrack = event.currentTrack
                if ((_state.value.showLyricsScreen || popupLyricsActive) && currentTrack != null) {
                    loadLyrics(currentTrack.id)
                } else {
                    _state.update { it.copy(lyricsTrackId = null, lyrics = null, isLyricsLoading = false) }
                }
                currentTrack?.let {
                    trackHarmoniaStart(it, 0L)
                    reportStarted(it, 0L)
                }
            }
            is VantafynMusicPlaybackEvent.PauseChanged -> reportProgress(event.track, event.positionMs, event.isPaused, force = true)
            is VantafynMusicPlaybackEvent.Seeked -> reportProgress(event.track, event.positionMs, isPaused = !_state.value.playback.isPlaying, force = true)
            is VantafynMusicPlaybackEvent.Stopped -> event.track?.let {
                trackHarmoniaStop(it, event.positionMs, event.reason)
                reportStopped(it, event.positionMs, event.reason)
            }
        }
    }

    private fun trackHarmoniaStart(track: VantafynMusicTrack, positionMs: Long) {
        val activeSession = session ?: return
        viewModelScope.launch {
            harmoniaTracker.onTrackStarted(activeSession, track, positionMs)
        }
    }

    private fun trackHarmoniaStop(track: VantafynMusicTrack, positionMs: Long, reason: VantafynMusicStopReason) {
        val activeSession = session ?: return
        viewModelScope.launch {
            harmoniaTracker.onTrackStopped(activeSession, track, positionMs, reason)
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

data class PlaylistDuplicatePrompt(
    val playlist: JellyfinMusicPlaylist,
    val candidateTracks: List<JellyfinMusicTrack>,
    val duplicateTracks: List<JellyfinMusicTrack>,
    val newTracks: List<JellyfinMusicTrack>,
)

data class MusicUiState(
    val isLoading: Boolean = false,
    val isSearchLoading: Boolean = false,
    val isMusicPageLoading: Boolean = false,
    val isMusicLoadingMore: Boolean = false,
    val isPlaylistSaving: Boolean = false,
    val isPlaylistDownloaded: Boolean = false,
    val isPlaylistDownloading: Boolean = false,
    val playlistDownloadProgress: Float = 0f,
    val isReorderMode: Boolean = false,
    val isRadioActive: Boolean = false,
    val isCasting: Boolean = false,
    val castReceiverName: String? = null,
    val castVolume: Float = 1f,
    val isCastMuted: Boolean = false,
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
    val songsSearchQuery: String = "",
    val songsFilter: MusicSongsFilter = MusicSongsFilter.All,
    val songsAlphabetKey: String? = null,
    val isHarmoniaGenerating: Boolean = false,
    val lastHarmoniaGenerationResult: HarmoniaGenerationResult? = null,
    val harmoniaGenerationMessage: String? = null,
    val harmoniaRecaps: List<HarmoniaRecapPreview> = emptyList(),
    val savedHarmoniaRecaps: List<HarmoniaRecapPreview> = emptyList(),
    val selectedHarmoniaRecap: HarmoniaRecap? = null,
    val harmoniaTopTrack: JellyfinMusicTrack? = null,
    val isHarmoniaMuted: Boolean = false,
    val isHarmoniaRecapLoading: Boolean = false,
    val harmoniaRecapError: String? = null,
    val recentlyPlayed: List<VantafynMusicTrack> = emptyList(),
    val selectedHomeMood: MusicHomeMood = MusicHomeMood.All,
    val playlistDuplicatePrompt: PlaylistDuplicatePrompt? = null,
    val isCheckingPlaylistDuplicates: Boolean = false,
)

enum class MusicHomeMood(val label: String) {
    All("All"),
    Energize("Energize"),
    Chill("Chill"),
    OnRepeat("On Repeat"),
    Favorites("Favorites"),
}

private data class LyricsCacheKey(
    val serverId: String = "local",
    val profileId: String = "local",
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
    data class Artist(
        val artist: JellyfinMusicArtist,
        val albums: List<JellyfinMusicAlbum>,
        val similarArtists: List<JellyfinMusicArtist> = emptyList(),
    ) : MusicScreenState
    data class Playlist(val playlist: JellyfinMusicPlaylist, val page: JellyfinMusicTrackPage) : MusicScreenState {
        val tracks: List<JellyfinMusicTrack>
            get() = page.tracks
    }
    data class Songs(val page: JellyfinMusicTrackPage) : MusicScreenState {
        val tracks: List<JellyfinMusicTrack>
            get() = page.tracks
    }
    data class HarmoniaRecap(val preview: HarmoniaRecapPreview) : MusicScreenState
}

private fun MusicSongsFilter.supportsAlphabetRail(): Boolean =
    this == MusicSongsFilter.AZ || this == MusicSongsFilter.Favorites

private fun String?.normalizedSongsAlphabetKey(): String? {
    val value = this?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
    return when {
        value == "#" -> value
        value.length == 1 && value[0] in 'A'..'Z' -> value
        else -> null
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
        tracks = if (currentPage != null && startIndex == currentPage.startIndex) currentPage.tracks else emptyList(),
        startIndex = startIndex.coerceAtLeast(0),
        pageSize = currentPage?.pageSize ?: MusicTrackPageSize,
        totalItems = currentPage?.totalItems ?: 0,
    )
}

private const val MusicTrackPageSize = 60

private fun HarmoniaGenerationResult.message(): String =
    when (this) {
        is HarmoniaGenerationResult.Generated -> "Generated ${recap.periodType.name.lowercase()} Harmonia recap."
        is HarmoniaGenerationResult.NotEnoughData -> reason
    }

fun Long.sanitizeLyricMillis(): Long =
    if (this > 1_000_000L) this / 10_000L else this

fun List<JellyfinLyricLine>.activeIndex(positionMs: Long): Int =
    indexOfLast { line -> line.startMs?.let { it.sanitizeLyricMillis() <= positionMs } == true }

private fun JellyfinMusicTrack.toPlaybackTrack(streamUrl: String = this.streamUrl): VantafynMusicTrack =
    VantafynMusicTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        genres = genres,
        streamUrl = streamUrl,
        artworkUrl = artworkUrl,
        isFavorite = isFavorite,
        replayGainTrackGainDb = replayGainTrackGainDb,
        replayGainTrackPeak = replayGainTrackPeak,
        container = container,
        codec = codec,
        bitrate = bitrate,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        channels = channels,
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
        onRepeat = onRepeat.mapFavorite(trackId, isFavorite),
    )

private fun JellyfinMusicHome.copyWithAlbumFavorite(albumId: UUID, isFavorite: Boolean): JellyfinMusicHome =
    copy(
        albums = albums.map { if (it.id == albumId) it.copy(isFavorite = isFavorite) else it },
    )

private fun JellyfinMusicHome.copyWithPlaylistFavorite(playlistId: UUID, isFavorite: Boolean): JellyfinMusicHome =
    copy(
        playlists = playlists.map { if (it.id == playlistId) it.copy(isFavorite = isFavorite) else it },
    )

private fun JellyfinMusicHome.incrementPlaylistCount(playlistId: UUID, delta: Int = 1): JellyfinMusicHome =
    copy(
        playlists = playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackCount = playlist.trackCount?.plus(delta))
            } else {
                playlist
            }
        },
    )

private fun JellyfinMusicHome.decrementPlaylistCount(playlistId: UUID, count: Int = 1): JellyfinMusicHome =
    copy(
        playlists = playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackCount = playlist.trackCount?.minus(count)?.coerceAtLeast(0))
            } else {
                playlist
            }
        },
    )

private fun List<JellyfinMusicTrack>.mapFavorite(trackId: UUID, isFavorite: Boolean): List<JellyfinMusicTrack> =
    map { if (it.id == trackId) it.copy(isFavorite = isFavorite) else it }

private fun VantafynMusicTrack.toJellyfinTrack(): JellyfinMusicTrack =
    JellyfinMusicTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        hasLyrics = false,
        streamUrl = streamUrl,
        isFavorite = isFavorite,
        genres = genres,
        replayGainTrackGainDb = replayGainTrackGainDb,
        replayGainTrackPeak = replayGainTrackPeak,
        container = container,
        codec = codec,
        bitrate = bitrate,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        channels = channels,
    )

private fun Long.toTicks(): Long =
    coerceAtLeast(0L) * 10_000L

private const val MusicProgressReportIntervalMs = 10_000L
private const val MusicBackgroundProgressReportIntervalMs = 70_000L
private const val KEY_DOWNLOAD_WIFI_ONLY_DEFAULT = "download_wifi_only_default"

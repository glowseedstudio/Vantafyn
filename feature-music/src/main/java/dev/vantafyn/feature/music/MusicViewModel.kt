package dev.vantafyn.feature.music

import android.app.Application
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
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.VantafynMusicPlaybackState
import dev.vantafyn.core.media.VantafynMusicTrack
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repositories = JellyfinRepositoryProvider(application)
    private val musicRepository: JellyfinMusicRepository = repositories.musicRepository
    private val playbackController = MusicPlaybackController.get(application)
    private var session: JellyfinSession? = null
    private var lyricsJob: Job? = null

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
        playbackController.playQueue(
            queue = queue.map { it.toPlaybackTrack() },
            startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0),
        )
    }

    fun playAlbum(album: JellyfinMusicAlbum) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getAlbumTracks(activeSession, album.id)) {
                is JellyfinResult.Success -> result.value.firstOrNull()?.let { playTrack(it, result.value) }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun playPlaylist(playlist: JellyfinMusicPlaylist) {
        val activeSession = session ?: return
        viewModelScope.launch {
            when (val result = musicRepository.getPlaylistItems(activeSession, playlist.id)) {
                is JellyfinResult.Success -> result.value.firstOrNull()?.let { playTrack(it, result.value) }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
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
        _state.update { it.copy(showNowPlaying = false) }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()
    fun next() = playbackController.next()
    fun previous() = playbackController.previous()
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun cycleRepeat() = playbackController.cycleRepeatMode()

    fun createPlaylistWithCurrent(name: String) {
        val activeSession = session ?: return
        val current = playbackController.state.value.currentTrack ?: return
        viewModelScope.launch {
            when (val result = musicRepository.createPlaylist(activeSession, name, listOf(current.id))) {
                is JellyfinResult.Success -> loadHome()
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun addCurrentToPlaylist(playlist: JellyfinMusicPlaylist) {
        val activeSession = session ?: return
        val current = playbackController.state.value.currentTrack ?: return
        viewModelScope.launch {
            when (val result = musicRepository.addToPlaylist(activeSession, playlist.id, listOf(current.id))) {
                is JellyfinResult.Success -> _state.update { it.copy(message = "Added to ${playlist.name}") }
                is JellyfinResult.Failure -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun stopForProfileChange() {
        playbackController.stop(clearQueue = true)
    }

    private fun loadLyrics(trackId: UUID) {
        val activeSession = session ?: return
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _state.update { it.copy(lyricsTrackId = trackId, lyrics = null, isLyricsLoading = true) }
            when (val result = musicRepository.getLyrics(activeSession, trackId)) {
                is JellyfinResult.Success -> _state.update {
                    it.copy(isLyricsLoading = false, lyrics = result.value)
                }
                is JellyfinResult.Failure -> _state.update {
                    it.copy(isLyricsLoading = false, lyrics = null)
                }
            }
        }
    }
}

data class MusicUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val home: JellyfinMusicHome? = null,
    val playback: VantafynMusicPlaybackState,
    val showNowPlaying: Boolean = false,
    val lyricsTrackId: UUID? = null,
    val lyrics: JellyfinLyrics? = null,
    val isLyricsLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<JellyfinMusicTrack> = emptyList(),
)

fun List<JellyfinLyricLine>.activeIndex(positionMs: Long): Int =
    indexOfLast { line -> line.startMs?.let { it <= positionMs } == true }.coerceAtLeast(0)

private fun JellyfinMusicTrack.toPlaybackTrack(): VantafynMusicTrack =
    VantafynMusicTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = streamUrl,
        artworkUrl = artworkUrl,
    )

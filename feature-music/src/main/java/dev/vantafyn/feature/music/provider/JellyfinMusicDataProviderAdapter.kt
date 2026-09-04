package dev.vantafyn.feature.music.provider

import dev.vantafyn.core.jellyfin.JellyfinMusicRepository
import dev.vantafyn.core.jellyfin.JellyfinPlaybackRepository
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.media.VantafynMusicTrack
import dev.vantafyn.core.media.music.MusicAlbum
import dev.vantafyn.core.media.music.MusicAlbumDetail
import dev.vantafyn.core.media.music.MusicArtist
import dev.vantafyn.core.media.music.MusicArtistDetail
import dev.vantafyn.core.media.music.MusicDataProvider
import dev.vantafyn.core.media.music.MusicHomeData
import dev.vantafyn.core.media.music.MusicLyricLine
import dev.vantafyn.core.media.music.MusicLyrics
import dev.vantafyn.core.media.music.MusicPlaylist
import dev.vantafyn.core.media.music.MusicPlaylistDetail
import dev.vantafyn.core.media.music.MusicResult
import dev.vantafyn.core.media.music.MusicSearchResult
import java.util.UUID

class JellyfinMusicDataProviderAdapter(
    private val musicRepository: JellyfinMusicRepository,
    private val playbackRepository: JellyfinPlaybackRepository,
    private val sessionProvider: () -> JellyfinSession?,
) : MusicDataProvider {
    override val providerId: String = "jellyfin"
    override val providerName: String = "Jellyfin"

    private fun requireSession(): JellyfinSession =
        sessionProvider() ?: throw IllegalStateException("No active Jellyfin session")

    override suspend fun getMusicHome(): MusicResult<MusicHomeData> = runCatching {
        val session = requireSession()
        when (val result = musicRepository.getMusicHome(session)) {
            is JellyfinResult.Success -> {
                val home = result.value
                val spotlight = home.songs.take(10).map {
                    VantafynMusicTrack(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        albumId = it.albumId,
                        durationMs = it.durationMs,
                        genres = it.genres,
                        streamUrl = it.streamUrl,
                        artworkUrl = it.artworkUrl,
                        isFavorite = it.isFavorite,
                    )
                }
                val recentAlbums = home.albums.map {
                    MusicAlbum(
                        id = it.id,
                        title = it.title,
                        artist = it.artist ?: "Unknown Artist",
                        year = it.year,
                        coverUrl = it.artworkUrl,
                        trackCount = null,
                        genres = emptyList(),
                    )
                }
                val topArtists = home.artists.map {
                    MusicArtist(
                        id = it.id,
                        name = it.name,
                        imageUrl = it.imageUrl,
                        albumCount = null,
                    )
                }
                val playlists = home.playlists.map {
                    MusicPlaylist(
                        id = it.id,
                        title = it.name,
                        owner = null,
                        trackCount = it.trackCount,
                        coverUrl = it.imageUrl,
                        durationMs = null,
                    )
                }
                MusicHomeData(
                    spotlightTracks = spotlight,
                    recentAlbums = recentAlbums,
                    topArtists = topArtists,
                    playlists = playlists,
                )
            }
            is JellyfinResult.Failure -> throw IllegalStateException(result.message)
        }
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load Jellyfin music home", it) },
    )

    override suspend fun getArtists(page: Int, query: String?): MusicResult<List<MusicArtist>> = runCatching {
        val session = requireSession()
        when (val result = musicRepository.getMusicHome(session)) {
            is JellyfinResult.Success -> result.value.artists.map {
                MusicArtist(id = it.id, name = it.name, imageUrl = it.imageUrl, albumCount = null)
            }
            is JellyfinResult.Failure -> throw IllegalStateException(result.message)
        }
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load artists", it) },
    )

    override suspend fun getArtistDetail(artistId: UUID): MusicResult<MusicArtistDetail> = runCatching {
        val session = requireSession()
        when (val result = musicRepository.getArtistAlbums(session, artistId)) {
            is JellyfinResult.Success -> {
                val albums = result.value.map {
                    MusicAlbum(
                        id = it.id,
                        title = it.title,
                        artist = it.artist ?: "Unknown Artist",
                        year = it.year,
                        coverUrl = it.artworkUrl,
                        trackCount = null,
                        genres = emptyList(),
                    )
                }
                MusicArtistDetail(
                    id = artistId,
                    name = albums.firstOrNull()?.artist ?: "Artist",
                    overview = null,
                    imageUrl = albums.firstOrNull()?.coverUrl,
                    albums = albums,
                    topTracks = emptyList(),
                )
            }
            is JellyfinResult.Failure -> throw IllegalStateException(result.message)
        }
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load artist detail", it) },
    )

    override suspend fun getAlbumDetail(albumId: UUID): MusicResult<MusicAlbumDetail> = runCatching {
        val session = requireSession()
        when (val result = musicRepository.getAlbumTracks(session, albumId)) {
            is JellyfinResult.Success -> {
                val tracks = result.value.map {
                    VantafynMusicTrack(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        albumId = it.albumId,
                        durationMs = it.durationMs,
                        genres = it.genres,
                        streamUrl = it.streamUrl,
                        artworkUrl = it.artworkUrl,
                        isFavorite = it.isFavorite,
                    )
                }
                val first = tracks.firstOrNull()
                MusicAlbumDetail(
                    id = albumId,
                    title = first?.album ?: "Album",
                    artist = first?.artist ?: "Artist",
                    artistId = null,
                    year = null,
                    coverUrl = first?.artworkUrl,
                    genres = first?.genres.orEmpty(),
                    tracks = tracks,
                )
            }
            is JellyfinResult.Failure -> throw IllegalStateException(result.message)
        }
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load album detail", it) },
    )

    override suspend fun getPlaylists(): MusicResult<List<MusicPlaylist>> = runCatching {
        val session = requireSession()
        when (val result = musicRepository.getMusicHome(session)) {
            is JellyfinResult.Success -> result.value.playlists.map {
                MusicPlaylist(
                    id = it.id,
                    title = it.name,
                    owner = null,
                    trackCount = it.trackCount,
                    coverUrl = it.imageUrl,
                    durationMs = null,
                )
            }
            is JellyfinResult.Failure -> throw IllegalStateException(result.message)
        }
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load playlists", it) },
    )

    override suspend fun getPlaylistDetail(playlistId: UUID): MusicResult<MusicPlaylistDetail> = runCatching {
        val session = requireSession()
        when (val result = musicRepository.getPlaylistItems(session, playlistId)) {
            is JellyfinResult.Success -> {
                val tracks = result.value.map {
                    VantafynMusicTrack(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        albumId = it.albumId,
                        durationMs = it.durationMs,
                        genres = it.genres,
                        streamUrl = it.streamUrl,
                        artworkUrl = it.artworkUrl,
                        isFavorite = it.isFavorite,
                    )
                }
                MusicPlaylistDetail(
                    id = playlistId,
                    title = "Playlist",
                    owner = null,
                    tracks = tracks,
                )
            }
            is JellyfinResult.Failure -> throw IllegalStateException(result.message)
        }
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load playlist detail", it) },
    )

    override suspend fun searchMusic(query: String): MusicResult<MusicSearchResult> = runCatching {
        val session = requireSession()
        when (val result = musicRepository.searchMusic(session, query)) {
            is JellyfinResult.Success -> {
                val tracks = result.value.map {
                    VantafynMusicTrack(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        albumId = it.albumId,
                        durationMs = it.durationMs,
                        genres = it.genres,
                        streamUrl = it.streamUrl,
                        artworkUrl = it.artworkUrl,
                        isFavorite = it.isFavorite,
                    )
                }
                MusicSearchResult(
                    artists = emptyList(),
                    albums = emptyList(),
                    tracks = tracks,
                )
            }
            is JellyfinResult.Failure -> throw IllegalStateException(result.message)
        }
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to search music", it) },
    )

    override suspend fun resolveStreamUrl(trackId: UUID): String =
        "${requireSession().server.url.trimEnd('/')}/Audio/$trackId/stream"

    override suspend fun getLyrics(trackId: UUID): MusicResult<MusicLyrics?> = runCatching {
        val session = requireSession()
        when (val result = musicRepository.getLyrics(session, trackId)) {
            is JellyfinResult.Success -> {
                val l = result.value
                if (l != null) {
                    MusicLyrics(
                        syncedLines = l.syncedLines.map { MusicLyricLine(startMs = it.startMs ?: 0L, text = it.text) },
                        plainText = l.plainText,
                    )
                } else null
            }
            is JellyfinResult.Failure -> throw IllegalStateException(result.message)
        }
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to get lyrics", it) },
    )

    override suspend fun setFavorite(trackId: UUID, isFavorite: Boolean): MusicResult<Unit> =
        MusicResult.Success(Unit)

    override suspend fun scrobble(trackId: UUID, submissionTimeMs: Long): MusicResult<Unit> =
        MusicResult.Success(Unit)
}

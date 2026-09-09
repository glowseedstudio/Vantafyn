package dev.vantafyn.core.media.music

import dev.vantafyn.core.media.VantafynMusicTrack
import java.util.UUID

sealed interface MusicResult<out T> {
    data class Success<out T>(val value: T) : MusicResult<T>
    data class Failure(val message: String, val throwable: Throwable? = null) : MusicResult<Nothing>
}

data class MusicArtist(
    val id: UUID,
    val name: String,
    val imageUrl: String? = null,
    val albumCount: Int? = null,
)

data class MusicArtistDetail(
    val id: UUID,
    val name: String,
    val overview: String? = null,
    val imageUrl: String? = null,
    val albums: List<MusicAlbum> = emptyList(),
    val topTracks: List<VantafynMusicTrack> = emptyList(),
)

data class MusicAlbum(
    val id: UUID,
    val title: String,
    val artist: String,
    val artistId: UUID? = null,
    val year: Int? = null,
    val coverUrl: String? = null,
    val trackCount: Int? = null,
    val genres: List<String> = emptyList(),
    val isFavorite: Boolean = false,
)

data class MusicAlbumDetail(
    val id: UUID,
    val title: String,
    val artist: String,
    val artistId: UUID? = null,
    val year: Int? = null,
    val coverUrl: String? = null,
    val genres: List<String> = emptyList(),
    val tracks: List<VantafynMusicTrack> = emptyList(),
    val isFavorite: Boolean = false,
)

data class MusicPlaylist(
    val id: UUID,
    val title: String,
    val owner: String? = null,
    val trackCount: Int? = null,
    val coverUrl: String? = null,
    val durationMs: Long? = null,
    val isFavorite: Boolean = false,
)

data class MusicPlaylistDetail(
    val id: UUID,
    val title: String,
    val owner: String? = null,
    val tracks: List<VantafynMusicTrack> = emptyList(),
    val isFavorite: Boolean = false,
)

data class MusicLyricLine(
    val startMs: Long,
    val text: String,
)

data class MusicLyrics(
    val syncedLines: List<MusicLyricLine> = emptyList(),
    val plainText: String? = null,
)

data class MusicSearchResult(
    val artists: List<MusicArtist> = emptyList(),
    val albums: List<MusicAlbum> = emptyList(),
    val tracks: List<VantafynMusicTrack> = emptyList(),
)

data class MusicHomeData(
    val spotlightTracks: List<VantafynMusicTrack> = emptyList(),
    val recentlyPlayed: List<VantafynMusicTrack> = emptyList(),
    val mostPlayed: List<VantafynMusicTrack> = emptyList(),
    val recentAlbums: List<MusicAlbum> = emptyList(),
    val topArtists: List<MusicArtist> = emptyList(),
    val playlists: List<MusicPlaylist> = emptyList(),
)

interface MusicDataProvider {
    val providerId: String
    val providerName: String

    suspend fun getMusicHome(): MusicResult<MusicHomeData>
    suspend fun getArtists(page: Int = 0, query: String? = null): MusicResult<List<MusicArtist>>
    suspend fun getArtistDetail(artistId: UUID): MusicResult<MusicArtistDetail>
    suspend fun getAlbumDetail(albumId: UUID): MusicResult<MusicAlbumDetail>
    suspend fun getPlaylists(): MusicResult<List<MusicPlaylist>>
    suspend fun getPlaylistDetail(playlistId: UUID): MusicResult<MusicPlaylistDetail>
    suspend fun searchMusic(query: String): MusicResult<MusicSearchResult>
    suspend fun resolveStreamUrl(trackId: UUID): String
    suspend fun getLyrics(trackId: UUID): MusicResult<MusicLyrics?>
    suspend fun setFavorite(trackId: UUID, isFavorite: Boolean): MusicResult<Unit>
    suspend fun scrobble(trackId: UUID, submissionTimeMs: Long): MusicResult<Unit>
    suspend fun getRandomSongs(size: Int = 50): MusicResult<List<VantafynMusicTrack>> = MusicResult.Success(emptyList())
    suspend fun getStarredSongs(): MusicResult<List<VantafynMusicTrack>> = MusicResult.Success(emptyList())
}

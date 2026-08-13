package dev.vantafyn.core.media

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicArtist
import dev.vantafyn.core.jellyfin.JellyfinMusicHome
import dev.vantafyn.core.jellyfin.JellyfinMusicPlaylist
import dev.vantafyn.core.jellyfin.JellyfinMusicTrack
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

internal class VantafynMusicMediaLibraryProvider(context: Context) {
    private val appContext = context.applicationContext
    private val repositories = JellyfinRepositoryProvider(appContext)
    private var session: JellyfinSession? = null
    private var home: JellyfinMusicHome? = null
    private val albumTracks = mutableMapOf<UUID, List<JellyfinMusicTrack>>()
    private val artistAlbums = mutableMapOf<UUID, List<JellyfinMusicAlbum>>()
    private val playlistTracks = mutableMapOf<UUID, List<JellyfinMusicTrack>>()
    private val searchResults = mutableMapOf<String, List<JellyfinMusicTrack>>()

    fun rootItem(): MediaItem =
        browsableItem(
            mediaId = ROOT_ID,
            title = "Vantafyn",
            subtitle = "Music",
            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        )

    fun getChildren(parentId: String): List<MediaItem> =
        runBlocking(Dispatchers.IO) {
            when {
                !ensureReady() -> listOf(signInItem())
                parentId == ROOT_ID -> rootChildren()
                parentId == RECENT_ID -> home.orEmpty().recentlyAdded.map { it.toPlayableMediaItem(RECENT_ID) }
                parentId == SONGS_ID -> home.orEmpty().songs.map { it.toPlayableMediaItem(SONGS_ID) }
                parentId == ALBUMS_ID -> home.orEmpty().albums.map { it.toAlbumItem() }
                parentId == ARTISTS_ID -> home.orEmpty().artists.map { it.toArtistItem() }
                parentId == PLAYLISTS_ID -> home.orEmpty().playlists.map { it.toPlaylistItem() }
                parentId == QUEUE_ID -> MusicPlaybackController.get(appContext).state.value.queue.map { it.toMediaItemForBrowse(QUEUE_ID) }
                parentId.startsWith(ALBUM_PREFIX) -> tracksForAlbum(parentId.removePrefix(ALBUM_PREFIX)).map { it.toPlayableMediaItem(parentId) }
                parentId.startsWith(ARTIST_PREFIX) -> albumsForArtist(parentId.removePrefix(ARTIST_PREFIX)).map { it.toAlbumItem() }
                parentId.startsWith(PLAYLIST_PREFIX) -> tracksForPlaylist(parentId.removePrefix(PLAYLIST_PREFIX)).map { it.toPlayableMediaItem(parentId) }
                parentId.startsWith(SEARCH_PREFIX) -> searchResults[parentId.removePrefix(SEARCH_PREFIX).lowercase()].orEmpty().map { it.toPlayableMediaItem(parentId) }
                else -> emptyList()
            }
        }

    fun getItem(mediaId: String): MediaItem? =
        when {
            mediaId == ROOT_ID -> rootItem()
            mediaId == RECENT_ID -> browsableItem(RECENT_ID, "Recently added", null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            mediaId == ALBUMS_ID -> browsableItem(ALBUMS_ID, "Albums", null, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
            mediaId == ARTISTS_ID -> browsableItem(ARTISTS_ID, "Artists", null, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
            mediaId == PLAYLISTS_ID -> browsableItem(PLAYLISTS_ID, "Playlists", null, MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
            mediaId == SONGS_ID -> browsableItem(SONGS_ID, "Songs", null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            mediaId == QUEUE_ID -> browsableItem(QUEUE_ID, "Now playing queue", null, MediaMetadata.MEDIA_TYPE_PLAYLIST)
            mediaId.startsWith(TRACK_PREFIX) -> {
                val resolved = resolveQueue(mediaId)
                resolved.tracks.getOrNull(resolved.startIndex)?.toPlayableMediaItem(resolved.containerId)
            }
            else -> null
        }

    fun search(query: String): Int =
        runBlocking(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.length < 2 || !ensureReady()) return@runBlocking 0
            val activeSession = session ?: return@runBlocking 0
            val results = when (val result = repositories.musicRepository.searchMusic(activeSession, clean, 50)) {
                is JellyfinResult.Success -> result.value
                is JellyfinResult.Failure -> emptyList()
            }
            searchResults[clean.lowercase()] = results
            results.size
        }

    fun searchChildren(query: String): List<MediaItem> {
        val clean = query.trim().lowercase()
        return searchResults[clean].orEmpty().map { it.toPlayableMediaItem("$SEARCH_PREFIX$clean") }
    }

    fun resolveQueue(mediaId: String): ResolvedMusicQueue {
        val parts = mediaId.split("|", limit = 3)
        val containerId = parts.getOrNull(1).orEmpty().ifBlank { SONGS_ID }
        val trackId = parts.getOrNull(2).orEmpty()
        val tracks = runBlocking(Dispatchers.IO) {
            when {
                containerId == RECENT_ID -> home.orEmpty().recentlyAdded
                containerId == SONGS_ID -> home.orEmpty().songs
                containerId == QUEUE_ID -> MusicPlaybackController.get(appContext).state.value.queue.map { it.toJellyfinTrack() }
                containerId.startsWith(ALBUM_PREFIX) -> tracksForAlbum(containerId.removePrefix(ALBUM_PREFIX))
                containerId.startsWith(PLAYLIST_PREFIX) -> tracksForPlaylist(containerId.removePrefix(PLAYLIST_PREFIX))
                containerId.startsWith(SEARCH_PREFIX) -> searchResults[containerId.removePrefix(SEARCH_PREFIX).lowercase()].orEmpty()
                else -> home.orEmpty().songs
            }
        }
        val startIndex = tracks.indexOfFirst { it.id.toString() == trackId }.coerceAtLeast(0)
        return ResolvedMusicQueue(containerId = containerId, tracks = tracks, startIndex = startIndex)
    }

    private suspend fun ensureReady(): Boolean {
        if (session != null && home != null) return true
        return withTimeoutOrNull(12_000L) {
            val profiles = repositories.authRepository.savedProfiles()
            if (profiles.isEmpty()) return@withTimeoutOrNull false
            profiles
                .sortedByDescending { it.lastUsedAt }
                .firstNotNullOfOrNull { profile ->
                    val restored = when (val result = repositories.authRepository.restoreSession(profile.id)) {
                        is JellyfinResult.Success -> result.value
                        is JellyfinResult.Failure -> return@firstNotNullOfOrNull null
                    }
                    val loadedHome = when (val result = repositories.musicRepository.getMusicHome(restored)) {
                        is JellyfinResult.Success -> result.value
                        is JellyfinResult.Failure -> return@firstNotNullOfOrNull null
                    }
                    restored to loadedHome
                }
                ?.let { (restored, loadedHome) ->
                    session = restored
                    home = loadedHome
                    true
                } ?: false
        } == true
    }

    private fun rootChildren(): List<MediaItem> {
        val snapshot = home.orEmpty()
        return listOf(
            browsableItem(RECENT_ID, "Recently added", "${snapshot.recentlyAdded.size} tracks", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            browsableItem(ALBUMS_ID, "Albums", "${snapshot.albums.size} albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
            browsableItem(ARTISTS_ID, "Artists", "${snapshot.artists.size} artists", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
            browsableItem(PLAYLISTS_ID, "Playlists", "${snapshot.playlists.size} playlists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
            browsableItem(SONGS_ID, "Songs", "${snapshot.songs.size} tracks", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            browsableItem(QUEUE_ID, "Now playing queue", "${MusicPlaybackController.get(appContext).state.value.queue.size} tracks", MediaMetadata.MEDIA_TYPE_PLAYLIST),
        )
    }

    private suspend fun tracksForAlbum(rawId: String): List<JellyfinMusicTrack> {
        val activeSession = session ?: return emptyList()
        val albumId = rawId.toUuidOrNull() ?: return emptyList()
        return albumTracks.getOrPut(albumId) {
            when (val result = repositories.musicRepository.getAlbumTracks(activeSession, albumId)) {
                is JellyfinResult.Success -> result.value
                is JellyfinResult.Failure -> emptyList()
            }
        }
    }

    private suspend fun albumsForArtist(rawId: String): List<JellyfinMusicAlbum> {
        val activeSession = session ?: return emptyList()
        val artistId = rawId.toUuidOrNull() ?: return emptyList()
        return artistAlbums.getOrPut(artistId) {
            when (val result = repositories.musicRepository.getArtistAlbums(activeSession, artistId)) {
                is JellyfinResult.Success -> result.value
                is JellyfinResult.Failure -> emptyList()
            }
        }
    }

    private suspend fun tracksForPlaylist(rawId: String): List<JellyfinMusicTrack> {
        val activeSession = session ?: return emptyList()
        val playlistId = rawId.toUuidOrNull() ?: return emptyList()
        return playlistTracks.getOrPut(playlistId) {
            when (val result = repositories.musicRepository.getPlaylistItems(activeSession, playlistId)) {
                is JellyfinResult.Success -> result.value
                is JellyfinResult.Failure -> emptyList()
            }
        }
    }

    private fun JellyfinMusicHome?.orEmpty(): JellyfinMusicHome =
        this ?: JellyfinMusicHome(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    private fun JellyfinMusicTrack.toPlayableMediaItem(containerId: String): MediaItem =
        toPlaybackTrack().toMediaItemForBrowse(containerId)

    private fun JellyfinMusicTrack.toPlaybackTrack(): VantafynMusicTrack =
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
        )

    private fun VantafynMusicTrack.toMediaItemForBrowse(containerId: String): MediaItem =
        MediaItem.Builder()
            .setMediaId("$TRACK_PREFIX|$containerId|$id")
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setDurationMs(durationMs)
                    .setArtworkUri(artworkUrl?.let(Uri::parse))
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setExtras(Bundle().apply { putString(EXTRA_TRACK_ID, id.toString()) })
                    .build(),
            )
            .build()

    private fun JellyfinMusicAlbum.toAlbumItem(): MediaItem =
        browsableItem(
            mediaId = "$ALBUM_PREFIX$id",
            title = title,
            subtitle = listOfNotNull(artist, year?.toString()).joinToString(" - ").ifBlank { null },
            mediaType = MediaMetadata.MEDIA_TYPE_ALBUM,
            artworkUrl = artworkUrl,
        )

    private fun JellyfinMusicArtist.toArtistItem(): MediaItem =
        browsableItem(
            mediaId = "$ARTIST_PREFIX$id",
            title = name,
            subtitle = "Artist",
            mediaType = MediaMetadata.MEDIA_TYPE_ARTIST,
            artworkUrl = imageUrl,
        )

    private fun JellyfinMusicPlaylist.toPlaylistItem(): MediaItem =
        browsableItem(
            mediaId = "$PLAYLIST_PREFIX$id",
            title = name,
            subtitle = "Playlist",
            mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
            artworkUrl = imageUrl,
        )

    private fun browsableItem(
        mediaId: String,
        title: String,
        subtitle: String?,
        mediaType: Int,
        artworkUrl: String? = null,
    ): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription(subtitle)
                    .setArtworkUri(artworkUrl?.let(Uri::parse))
                    .setMediaType(mediaType)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun signInItem(): MediaItem =
        browsableItem(
            mediaId = SIGN_IN_ID,
            title = "Sign in to Vantafyn",
            subtitle = "Open Vantafyn on your phone to connect Jellyfin.",
            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        )

    private fun String.toUuidOrNull(): UUID? =
        runCatching { UUID.fromString(this) }.getOrNull()

    data class ResolvedMusicQueue(
        val containerId: String,
        val tracks: List<JellyfinMusicTrack>,
        val startIndex: Int,
    )

    companion object {
        const val ROOT_ID = "vf-root"
        const val RECENT_ID = "vf-recently-added"
        const val ALBUMS_ID = "vf-albums"
        const val ARTISTS_ID = "vf-artists"
        const val PLAYLISTS_ID = "vf-playlists"
        const val SONGS_ID = "vf-songs"
        const val QUEUE_ID = "vf-queue"
        const val SIGN_IN_ID = "vf-sign-in"
        const val ALBUM_PREFIX = "vf-album:"
        const val ARTIST_PREFIX = "vf-artist:"
        const val PLAYLIST_PREFIX = "vf-playlist:"
        const val SEARCH_PREFIX = "vf-search:"
        const val TRACK_PREFIX = "vf-track"
        const val EXTRA_TRACK_ID = "dev.vantafyn.media.TRACK_ID"
    }
}

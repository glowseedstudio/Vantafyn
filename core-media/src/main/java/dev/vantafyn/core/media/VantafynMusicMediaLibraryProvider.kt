package dev.vantafyn.core.media

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.vantafyn.core.downloads.DownloadMediaType
import dev.vantafyn.core.downloads.DownloadRecord
import dev.vantafyn.core.downloads.DownloadRepository
import dev.vantafyn.core.downloads.SqliteDownloadRepository
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicArtist
import dev.vantafyn.core.jellyfin.JellyfinMusicHome
import dev.vantafyn.core.jellyfin.JellyfinMusicPlaylist
import dev.vantafyn.core.jellyfin.JellyfinMusicTrack
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.media.music.MusicResult
import dev.vantafyn.core.subsonic.SubsonicClient
import dev.vantafyn.core.subsonic.SubsonicCredentials
import dev.vantafyn.core.subsonic.SubsonicMusicDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

internal class VantafynMusicMediaLibraryProvider(context: Context) {
    private val appContext = context.applicationContext
    private val repositories = JellyfinRepositoryProvider(appContext)
    private val downloadRepository: DownloadRepository = SqliteDownloadRepository(appContext)

    private var session: JellyfinSession? = null
    private var subsonicProvider: SubsonicMusicDataProvider? = null
    private var home: JellyfinMusicHome? = null
    private val sessionMutex = Mutex()

    private val albumTracks = mutableMapOf<UUID, List<JellyfinMusicTrack>>()
    private val artistAlbums = mutableMapOf<UUID, List<JellyfinMusicAlbum>>()
    private val playlistTracks = mutableMapOf<UUID, List<JellyfinMusicTrack>>()
    private val searchResults = mutableMapOf<String, List<JellyfinMusicTrack>>()

    fun rootItem(): MediaItem {
        val extras = Bundle().apply {
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
        }
        return MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Vantafyn")
                    .setSubtitle("Music")
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    fun rootChildren(): List<MediaItem> =
        listOf(
            browsableItem(RECENT_ID, "Recently added", null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = true),
            browsableItem(ALBUMS_ID, "Albums", null, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS, isGrid = true),
            browsableItem(ARTISTS_ID, "Artists", null, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS, isGrid = true),
            browsableItem(PLAYLISTS_ID, "Playlists", null, MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS, isGrid = true),
            browsableItem(SONGS_ID, "Songs", null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = false),
            browsableItem(DOWNLOADS_ID, "Downloads", "Offline music", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = true),
            browsableItem(QUEUE_ID, "Now playing queue", null, MediaMetadata.MEDIA_TYPE_PLAYLIST, isGrid = false),
        )

    suspend fun getChildrenAsync(parentId: String): List<MediaItem> =
        withContext(Dispatchers.IO) {
            when {
                parentId == ROOT_ID -> rootChildren()
                parentId == QUEUE_ID -> MusicPlaybackController.get(appContext).state.value.queue.map { it.toMediaItemForBrowse(QUEUE_ID) }
                parentId == DOWNLOADS_ID -> getDownloadedMusicItems()
                parentId == DOWNLOADS_SONGS_ID -> getDownloadedSongs()
                parentId.startsWith(DOWNLOAD_ALBUM_PREFIX) -> getDownloadedAlbumTracks(parentId.removePrefix(DOWNLOAD_ALBUM_PREFIX))
                !ensureReady() -> {
                    val offlineSongs = getDownloadedSongs()
                    if (offlineSongs.isNotEmpty()) {
                        offlineSongs
                    } else {
                        listOf(signInItem())
                    }
                }
                parentId == RECENT_ID -> home.orEmpty().recentlyAdded.map { it.toPlayableMediaItem(RECENT_ID) }
                parentId == SONGS_ID -> home.orEmpty().songs.map { it.toPlayableMediaItem(SONGS_ID) }
                parentId == ALBUMS_ID -> home.orEmpty().albums.map { it.toAlbumItem() }
                parentId == ARTISTS_ID -> home.orEmpty().artists.map { it.toArtistItem() }
                parentId == PLAYLISTS_ID -> home.orEmpty().playlists.map { it.toPlaylistItem() }
                parentId.startsWith(ALBUM_PREFIX) -> tracksForAlbum(parentId.removePrefix(ALBUM_PREFIX)).map { it.toPlayableMediaItem(parentId) }
                parentId.startsWith(ARTIST_PREFIX) -> albumsForArtist(parentId.removePrefix(ARTIST_PREFIX)).map { it.toAlbumItem() }
                parentId.startsWith(PLAYLIST_PREFIX) -> tracksForPlaylist(parentId.removePrefix(PLAYLIST_PREFIX)).map { it.toPlayableMediaItem(parentId) }
                parentId.startsWith(SEARCH_PREFIX) -> searchResults[parentId.removePrefix(SEARCH_PREFIX).lowercase()].orEmpty().map { it.toPlayableMediaItem(parentId) }
                else -> emptyList()
            }
        }

    fun getChildren(parentId: String): List<MediaItem> =
        if (parentId == ROOT_ID) {
            rootChildren()
        } else if (parentId == QUEUE_ID) {
            MusicPlaybackController.get(appContext).state.value.queue.map { it.toMediaItemForBrowse(QUEUE_ID) }
        } else {
            runBlocking(Dispatchers.IO) { getChildrenAsync(parentId) }
        }

    suspend fun getItemAsync(mediaId: String): MediaItem? =
        withContext(Dispatchers.IO) {
            when {
                mediaId == ROOT_ID -> rootItem()
                mediaId == RECENT_ID -> browsableItem(RECENT_ID, "Recently added", null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = true)
                mediaId == ALBUMS_ID -> browsableItem(ALBUMS_ID, "Albums", null, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS, isGrid = true)
                mediaId == ARTISTS_ID -> browsableItem(ARTISTS_ID, "Artists", null, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS, isGrid = true)
                mediaId == PLAYLISTS_ID -> browsableItem(PLAYLISTS_ID, "Playlists", null, MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS, isGrid = true)
                mediaId == SONGS_ID -> browsableItem(SONGS_ID, "Songs", null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = false)
                mediaId == DOWNLOADS_ID -> browsableItem(DOWNLOADS_ID, "Downloads", "Offline music", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = true)
                mediaId == DOWNLOADS_SONGS_ID -> browsableItem(DOWNLOADS_SONGS_ID, "All downloaded songs", null, MediaMetadata.MEDIA_TYPE_PLAYLIST, isGrid = false)
                mediaId == QUEUE_ID -> browsableItem(QUEUE_ID, "Now playing queue", null, MediaMetadata.MEDIA_TYPE_PLAYLIST, isGrid = false)
                mediaId.startsWith(TRACK_PREFIX) -> {
                    val resolved = resolveQueueAsync(mediaId)
                    resolved.tracks.getOrNull(resolved.startIndex)?.toPlayableMediaItem(resolved.containerId)
                }
                mediaId.startsWith(DOWNLOAD_ALBUM_PREFIX) -> {
                    val albumKey = mediaId.removePrefix(DOWNLOAD_ALBUM_PREFIX)
                    val records = downloadRepository.listAllCompleted()
                        .filter { (it.albumId == albumKey || it.albumName == albumKey) && isMusicRecord(it) }
                    val first = records.firstOrNull()
                    if (first != null) {
                        val poster = first.localPosterPath?.let { "file://$it" } ?: first.remotePosterUrl
                        browsableItem(
                            mediaId = mediaId,
                            title = first.albumName ?: first.title,
                            subtitle = first.artistName,
                            mediaType = MediaMetadata.MEDIA_TYPE_ALBUM,
                            artworkUrl = poster,
                            isGrid = true,
                        )
                    } else null
                }
                mediaId.startsWith(ALBUM_PREFIX) -> {
                    val albumId = mediaId.removePrefix(ALBUM_PREFIX).toUuidOrNull()
                    if (albumId != null) {
                        ensureReady()
                        home?.albums?.firstOrNull { it.id == albumId }?.toAlbumItem()
                    } else null
                }
                mediaId.startsWith(ARTIST_PREFIX) -> {
                    val artistId = mediaId.removePrefix(ARTIST_PREFIX).toUuidOrNull()
                    if (artistId != null) {
                        ensureReady()
                        home?.artists?.firstOrNull { it.id == artistId }?.toArtistItem()
                    } else null
                }
                mediaId.startsWith(PLAYLIST_PREFIX) -> {
                    val playlistId = mediaId.removePrefix(PLAYLIST_PREFIX).toUuidOrNull()
                    if (playlistId != null) {
                        ensureReady()
                        home?.playlists?.firstOrNull { it.id == playlistId }?.toPlaylistItem()
                    } else null
                }
                else -> null
            }
        }

    fun getItem(mediaId: String): MediaItem? =
        when {
            mediaId == ROOT_ID -> rootItem()
            mediaId == RECENT_ID -> browsableItem(RECENT_ID, "Recently added", null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = true)
            mediaId == ALBUMS_ID -> browsableItem(ALBUMS_ID, "Albums", null, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS, isGrid = true)
            mediaId == ARTISTS_ID -> browsableItem(ARTISTS_ID, "Artists", null, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS, isGrid = true)
            mediaId == PLAYLISTS_ID -> browsableItem(PLAYLISTS_ID, "Playlists", null, MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS, isGrid = true)
            mediaId == SONGS_ID -> browsableItem(SONGS_ID, "Songs", null, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = false)
            mediaId == DOWNLOADS_ID -> browsableItem(DOWNLOADS_ID, "Downloads", "Offline music", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, isGrid = true)
            mediaId == DOWNLOADS_SONGS_ID -> browsableItem(DOWNLOADS_SONGS_ID, "All downloaded songs", null, MediaMetadata.MEDIA_TYPE_PLAYLIST, isGrid = false)
            mediaId == QUEUE_ID -> browsableItem(QUEUE_ID, "Now playing queue", null, MediaMetadata.MEDIA_TYPE_PLAYLIST, isGrid = false)
            else -> runBlocking(Dispatchers.IO) { getItemAsync(mediaId) }
        }

    suspend fun searchAsync(query: String): Int =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.length < 2 || !ensureReady()) return@withContext 0

            val provider = subsonicProvider
            if (provider != null) {
                val results = when (val res = provider.searchMusic(clean)) {
                    is MusicResult.Success -> res.value.tracks.map { it.toJellyfinTrack() }
                    is MusicResult.Failure -> emptyList()
                }
                searchResults[clean.lowercase()] = results
                return@withContext results.size
            }

            val activeSession = session ?: return@withContext 0
            val results = when (val result = repositories.musicRepository.searchMusic(activeSession, clean, 50)) {
                is JellyfinResult.Success -> result.value
                is JellyfinResult.Failure -> emptyList()
            }
            searchResults[clean.lowercase()] = results
            results.size
        }

    fun search(query: String): Int =
        runBlocking(Dispatchers.IO) { searchAsync(query) }

    fun searchChildren(query: String): List<MediaItem> {
        val clean = query.trim().lowercase()
        return searchResults[clean].orEmpty().map { it.toPlayableMediaItem("$SEARCH_PREFIX$clean") }
    }

    suspend fun resolveQueueAsync(mediaId: String): ResolvedMusicQueue =
        withContext(Dispatchers.IO) {
            val parts = mediaId.split("|", limit = 3)
            val containerId = parts.getOrNull(1).orEmpty().ifBlank { SONGS_ID }
            val trackId = parts.getOrNull(2).orEmpty()

            val tracks = when {
                containerId == DOWNLOADS_ID || containerId == DOWNLOADS_SONGS_ID -> {
                    val records = downloadRepository.listAllCompleted().filter(::isMusicRecord)
                    records.map { it.toJellyfinTrack() }
                }
                containerId.startsWith(DOWNLOAD_ALBUM_PREFIX) -> {
                    val albumKey = containerId.removePrefix(DOWNLOAD_ALBUM_PREFIX)
                    val records = downloadRepository.listAllCompleted()
                        .filter { (it.albumId == albumKey || it.albumName == albumKey) && isMusicRecord(it) }
                    records.map { it.toJellyfinTrack() }
                }
                else -> {
                    ensureReady()
                    when {
                        containerId == RECENT_ID -> home.orEmpty().recentlyAdded
                        containerId == SONGS_ID -> home.orEmpty().songs
                        containerId == QUEUE_ID -> MusicPlaybackController.get(appContext).state.value.queue.map { it.toJellyfinTrack() }
                        containerId.startsWith(ALBUM_PREFIX) -> tracksForAlbum(containerId.removePrefix(ALBUM_PREFIX))
                        containerId.startsWith(ARTIST_PREFIX) -> albumsForArtist(containerId.removePrefix(ARTIST_PREFIX)).flatMap {
                            tracksForAlbum(it.id.toString())
                        }
                        containerId.startsWith(PLAYLIST_PREFIX) -> tracksForPlaylist(containerId.removePrefix(PLAYLIST_PREFIX))
                        containerId.startsWith(SEARCH_PREFIX) -> searchResults[containerId.removePrefix(SEARCH_PREFIX).lowercase()].orEmpty()
                        else -> home.orEmpty().songs
                    }
                }
            }
            val startIndex = tracks.indexOfFirst { it.id.toString() == trackId }.coerceAtLeast(0)
            ResolvedMusicQueue(containerId = containerId, tracks = tracks, startIndex = startIndex)
        }

    fun resolveQueue(mediaId: String): ResolvedMusicQueue =
        runBlocking(Dispatchers.IO) { resolveQueueAsync(mediaId) }

    private suspend fun getDownloadedMusicItems(): List<MediaItem> {
        val records = downloadRepository.listAllCompleted().filter(::isMusicRecord)
        if (records.isEmpty()) {
            return listOf(
                browsableItem(
                    mediaId = "vf-downloads-empty",
                    title = "No offline music",
                    subtitle = "Download tracks or albums on phone to play offline in car",
                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                    isGrid = false,
                ),
            )
        }

        val resultList = mutableListOf<MediaItem>()
        resultList.add(
            browsableItem(
                mediaId = DOWNLOADS_SONGS_ID,
                title = "All downloaded songs",
                subtitle = "${records.size} tracks available offline",
                mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                isGrid = false,
            ),
        )

        val albums = records.filter { !it.albumName.isNullOrBlank() }.groupBy { it.albumId ?: it.albumName.orEmpty() }
        albums.forEach { (albumKey, tracksInAlbum) ->
            val first = tracksInAlbum.first()
            val poster = first.localPosterPath?.let { "file://$it" } ?: first.remotePosterUrl
            resultList.add(
                browsableItem(
                    mediaId = "$DOWNLOAD_ALBUM_PREFIX$albumKey",
                    title = first.albumName ?: first.title,
                    subtitle = first.artistName ?: "${tracksInAlbum.size} tracks",
                    mediaType = MediaMetadata.MEDIA_TYPE_ALBUM,
                    artworkUrl = poster,
                    isGrid = true,
                ),
            )
        }

        val looseSongs = records.filter { it.albumName.isNullOrBlank() }
        looseSongs.forEach { rec ->
            resultList.add(rec.toPlayableDownloadedItem(DOWNLOADS_ID))
        }

        return resultList
    }

    private suspend fun getDownloadedSongs(): List<MediaItem> =
        downloadRepository.listAllCompleted()
            .filter(::isMusicRecord)
            .map { it.toPlayableDownloadedItem(DOWNLOADS_SONGS_ID) }

    private suspend fun getDownloadedAlbumTracks(albumKey: String): List<MediaItem> =
        downloadRepository.listAllCompleted()
            .filter { (it.albumId == albumKey || it.albumName == albumKey) && isMusicRecord(it) }
            .map { it.toPlayableDownloadedItem("$DOWNLOAD_ALBUM_PREFIX$albumKey") }

    private fun isMusicRecord(record: DownloadRecord): Boolean =
        record.mediaType == DownloadMediaType.MusicTrack || record.mediaType == DownloadMediaType.MusicAlbum

    private fun DownloadRecord.toPlayableDownloadedItem(containerId: String): MediaItem =
        toJellyfinTrack().toPlayableMediaItem(containerId)

    private fun DownloadRecord.toJellyfinTrack(): JellyfinMusicTrack {
        val parsedId = runCatching { UUID.fromString(identity.itemId) }.getOrNull()
            ?: UUID.nameUUIDFromBytes(id.toByteArray())
        val albumParsedId = albumId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val localUri = localMediaPath?.let { if (it.startsWith("file://")) it else "file://$it" }.orEmpty()
        val posterUri = localPosterPath?.let { if (it.startsWith("file://")) it else "file://$it" } ?: remotePosterUrl
        val duration = runtimeTicks?.let { it / 10_000L } ?: 0L
        return JellyfinMusicTrack(
            id = parsedId,
            title = title,
            artist = artistName.orEmpty(),
            album = albumName,
            albumId = albumParsedId,
            durationMs = duration,
            genres = emptyList(),
            artworkUrl = posterUri,
            hasLyrics = !localLyricsPath.isNullOrBlank() || offlineFeatureFlags?.contains("lyrics") == true,
            streamUrl = localUri,
            isFavorite = false,
        )
    }

    private fun getSubsonicCredentials(): SubsonicCredentials? {
        val prefs = appContext.getSharedPreferences("vantafyn_subsonic_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("subsonic_url", null) ?: return null
        val user = prefs.getString("subsonic_username", null) ?: return null
        val pass = prefs.getString("subsonic_password", null) ?: return null
        if (url.isBlank() || user.isBlank()) return null
        return SubsonicCredentials(url, user, pass)
    }

    private suspend fun ensureReady(): Boolean {
        if ((session != null || subsonicProvider != null) && home != null) return true
        return sessionMutex.withLock {
            if ((session != null || subsonicProvider != null) && home != null) return@withLock true

            withTimeoutOrNull(14_000L) {
                // 1. Try Subsonic first if credentials exist
                val subsonicCreds = getSubsonicCredentials()
                if (subsonicCreds != null) {
                    val client = SubsonicClient(subsonicCreds)
                    val provider = SubsonicMusicDataProvider(client, appContext)
                    val homeResult = provider.getMusicHome()
                    val artistsResult = provider.getArtists(0, null)
                    val randomSongsResult = provider.getRandomSongs(40)

                    if (homeResult is MusicResult.Success) {
                        val h = homeResult.value
                        val artistsList = if (artistsResult is MusicResult.Success) {
                            artistsResult.value.map {
                                JellyfinMusicArtist(
                                    id = it.id,
                                    name = it.name,
                                    imageUrl = it.imageUrl,
                                )
                            }
                        } else emptyList()

                        val songsList = if (randomSongsResult is MusicResult.Success) {
                            randomSongsResult.value.map { it.toJellyfinTrack() }
                        } else emptyList()

                        val jHome = JellyfinMusicHome(
                            libraries = emptyList(),
                            recentlyAdded = h.recentAlbums.map {
                                JellyfinMusicTrack(
                                    id = it.id,
                                    title = it.title,
                                    artist = it.artist,
                                    album = it.title,
                                    albumId = it.id,
                                    durationMs = 0L,
                                    genres = it.genres,
                                    artworkUrl = it.coverUrl,
                                    hasLyrics = false,
                                    streamUrl = "",
                                    isFavorite = false,
                                )
                            },
                            albums = h.recentAlbums.map {
                                JellyfinMusicAlbum(
                                    id = it.id,
                                    title = it.title,
                                    artist = it.artist,
                                    year = it.year,
                                    artworkUrl = it.coverUrl,
                                )
                            },
                            artists = artistsList,
                            playlists = h.playlists.map {
                                JellyfinMusicPlaylist(
                                    id = it.id,
                                    name = it.title,
                                    imageUrl = it.coverUrl,
                                    trackCount = it.trackCount,
                                )
                            },
                            songs = songsList,
                        )
                        subsonicProvider = provider
                        home = jHome
                        return@withTimeoutOrNull true
                    }
                }

                // 2. Try Jellyfin Saved Profiles
                val profiles = repositories.authRepository.savedProfiles()
                if (profiles.isNotEmpty()) {
                    val candidate = profiles
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
                    if (candidate != null) {
                        session = candidate.first
                        home = candidate.second
                        return@withTimeoutOrNull true
                    }
                }

                // 3. Fallback to offline completed downloads if any exist
                val downloadedSongs = downloadRepository.listAllCompleted().filter(::isMusicRecord)
                if (downloadedSongs.isNotEmpty()) {
                    home = JellyfinMusicHome(
                        libraries = emptyList(),
                        recentlyAdded = downloadedSongs.take(20).map { it.toJellyfinTrack() },
                        albums = downloadedSongs.filter { !it.albumName.isNullOrBlank() }.groupBy { it.albumId ?: it.albumName.orEmpty() }.map { (key, list) ->
                            val first = list.first()
                            JellyfinMusicAlbum(
                                id = UUID.nameUUIDFromBytes(key.toByteArray()),
                                title = first.albumName ?: first.title,
                                artist = first.artistName ?: "Various",
                                year = first.year,
                                artworkUrl = first.localPosterPath?.let { "file://$it" } ?: first.remotePosterUrl,
                            )
                        },
                        artists = emptyList(),
                        playlists = emptyList(),
                        songs = downloadedSongs.map { it.toJellyfinTrack() },
                    )
                    return@withTimeoutOrNull true
                }

                false
            } == true
        }
    }

    private suspend fun tracksForAlbum(rawId: String): List<JellyfinMusicTrack> {
        val albumId = rawId.toUuidOrNull() ?: return emptyList()

        val provider = subsonicProvider
        if (provider != null) {
            return albumTracks.getOrPut(albumId) {
                when (val result = provider.getAlbumDetail(albumId)) {
                    is MusicResult.Success -> result.value.tracks.map { it.toJellyfinTrack() }
                    is MusicResult.Failure -> emptyList()
                }
            }
        }

        val activeSession = session ?: return emptyList()
        return albumTracks.getOrPut(albumId) {
            when (val result = repositories.musicRepository.getAlbumTracks(activeSession, albumId)) {
                is JellyfinResult.Success -> result.value
                is JellyfinResult.Failure -> emptyList()
            }
        }
    }

    private suspend fun albumsForArtist(rawId: String): List<JellyfinMusicAlbum> {
        val artistId = rawId.toUuidOrNull() ?: return emptyList()

        val provider = subsonicProvider
        if (provider != null) {
            return artistAlbums.getOrPut(artistId) {
                when (val result = provider.getArtistDetail(artistId)) {
                    is MusicResult.Success -> result.value.albums.map {
                        JellyfinMusicAlbum(
                            id = it.id,
                            title = it.title,
                            artist = it.artist,
                            year = it.year,
                            artworkUrl = it.coverUrl,
                        )
                    }
                    is MusicResult.Failure -> emptyList()
                }
            }
        }

        val activeSession = session ?: return emptyList()
        return artistAlbums.getOrPut(artistId) {
            when (val result = repositories.musicRepository.getArtistAlbums(activeSession, artistId)) {
                is JellyfinResult.Success -> result.value
                is JellyfinResult.Failure -> emptyList()
            }
        }
    }

    private suspend fun tracksForPlaylist(rawId: String): List<JellyfinMusicTrack> {
        val playlistId = rawId.toUuidOrNull() ?: return emptyList()

        val provider = subsonicProvider
        if (provider != null) {
            return playlistTracks.getOrPut(playlistId) {
                when (val result = provider.getPlaylistDetail(playlistId)) {
                    is MusicResult.Success -> result.value.tracks.map { it.toJellyfinTrack() }
                    is MusicResult.Failure -> emptyList()
                }
            }
        }

        val activeSession = session ?: return emptyList()
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
            genres = genres,
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
            genres = genres,
            artworkUrl = artworkUrl,
            hasLyrics = false,
            streamUrl = streamUrl,
            isFavorite = isFavorite,
        )

    private fun VantafynMusicTrack.toMediaItemForBrowse(containerId: String): MediaItem {
        val extras = Bundle().apply {
            putString(EXTRA_TRACK_ID, id.toString())
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }
        return MediaItem.Builder()
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
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    private fun JellyfinMusicAlbum.toAlbumItem(): MediaItem =
        browsableItem(
            mediaId = "$ALBUM_PREFIX$id",
            title = title,
            subtitle = listOfNotNull(artist, year?.toString()).joinToString(" - ").ifBlank { null },
            mediaType = MediaMetadata.MEDIA_TYPE_ALBUM,
            artworkUrl = artworkUrl,
            isGrid = true,
        )

    private fun JellyfinMusicArtist.toArtistItem(): MediaItem =
        browsableItem(
            mediaId = "$ARTIST_PREFIX$id",
            title = name,
            subtitle = "Artist",
            mediaType = MediaMetadata.MEDIA_TYPE_ARTIST,
            artworkUrl = imageUrl,
            isGrid = true,
        )

    private fun JellyfinMusicPlaylist.toPlaylistItem(): MediaItem =
        browsableItem(
            mediaId = "$PLAYLIST_PREFIX$id",
            title = name,
            subtitle = "Playlist",
            mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
            artworkUrl = imageUrl,
            isGrid = true,
        )

    private fun browsableItem(
        mediaId: String,
        title: String,
        subtitle: String?,
        mediaType: Int,
        artworkUrl: String? = null,
        isGrid: Boolean = false,
    ): MediaItem {
        val extras = Bundle().apply {
            putInt(CONTENT_STYLE_BROWSABLE_HINT, if (isGrid) CONTENT_STYLE_GRID else CONTENT_STYLE_LIST)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }
        return MediaItem.Builder()
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
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    private fun signInItem(): MediaItem =
        browsableItem(
            mediaId = SIGN_IN_ID,
            title = "Sign in to Vantafyn",
            subtitle = "Open Vantafyn on your phone to connect Jellyfin or Subsonic.",
            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            isGrid = false,
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
        const val DOWNLOADS_ID = "vf-downloads"
        const val DOWNLOADS_SONGS_ID = "vf-downloads:songs"
        const val QUEUE_ID = "vf-queue"
        const val SIGN_IN_ID = "vf-sign-in"
        const val ALBUM_PREFIX = "vf-album:"
        const val ARTIST_PREFIX = "vf-artist:"
        const val PLAYLIST_PREFIX = "vf-playlist:"
        const val DOWNLOAD_ALBUM_PREFIX = "vf-download-album:"
        const val SEARCH_PREFIX = "vf-search:"
        const val TRACK_PREFIX = "vf-track"
        const val EXTRA_TRACK_ID = "dev.vantafyn.media.TRACK_ID"

        // Standard Android Auto Content Style Bundle Keys
        const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        const val CONTENT_STYLE_LIST = 1
        const val CONTENT_STYLE_GRID = 2
    }
}

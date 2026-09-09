package dev.vantafyn.core.subsonic

import android.content.Context
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
import dev.vantafyn.core.media.music.MusicQualityPreferences
import dev.vantafyn.core.media.music.MusicResult
import dev.vantafyn.core.media.music.MusicSearchResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SubsonicCredentials(
    val serverUrl: String,
    val username: String,
    val passwordOrToken: String,
    val isTokenAuth: Boolean = true,
)

class SubsonicClient(
    private val credentials: SubsonicCredentials,
) {
    private val clientName = "Vantafyn"
    private val apiVersion = "1.16.1"

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String =
        UUID.randomUUID().toString().replace("-", "").take(12)

    fun buildAuthQuery(): String {
        val salt = generateSalt()
        val token = md5(credentials.passwordOrToken + salt)
        return "u=${URLEncoder.encode(credentials.username, "UTF-8")}&t=$token&s=$salt&v=$apiVersion&c=$clientName&f=json"
    }

    fun buildStreamUrl(trackId: String, maxBitRateKbps: Int? = null): String {
        val base = credentials.serverUrl.trimEnd('/')
        val bitrateParam = if (maxBitRateKbps != null && maxBitRateKbps > 0) "&maxBitRate=$maxBitRateKbps" else ""
        return "$base/rest/stream.view?id=$trackId$bitrateParam&${buildAuthQuery()}"
    }

    fun buildCoverArtUrl(coverId: String?, size: Int = 500): String? {
        if (coverId.isNullOrBlank()) return null
        val base = credentials.serverUrl.trimEnd('/')
        return "$base/rest/getCoverArt.view?id=$coverId&size=$size&${buildAuthQuery()}"
    }

    suspend fun executeGet(endpoint: String, params: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            val base = credentials.serverUrl.trimEnd('/')
            val queryParams = params.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            val auth = buildAuthQuery()
            val urlString = if (queryParams.isBlank()) {
                "$base/rest/$endpoint.view?$auth"
            } else {
                "$base/rest/$endpoint.view?$queryParams&$auth"
            }

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 12_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(stream))
            val responseText = reader.readText()
            reader.close()
            conn.disconnect()

            val json = JSONObject(responseText)
            val subsonicResponse = json.optJSONObject("subsonic-response")
                ?: throw IllegalStateException("Invalid Subsonic API response format")

            if (subsonicResponse.optString("status") != "ok") {
                val error = subsonicResponse.optJSONObject("error")
                val message = error?.optString("message") ?: "Subsonic request failed (code: ${error?.optInt("code")})"
                throw IllegalStateException(message)
            }

            subsonicResponse
        }

    suspend fun ping(): Boolean = runCatching {
        val response = executeGet("ping")
        response.optString("status") == "ok"
    }.getOrDefault(false)
}

class SubsonicMusicDataProvider(
    private val client: SubsonicClient,
    private val context: Context? = null,
) : MusicDataProvider {
    override val providerId: String = "opensubsonic"
    override val providerName: String = "OpenSubsonic"

    private val uuidIdMap = mutableMapOf<UUID, String>()
    private val stringIdMap = mutableMapOf<String, UUID>()

    private fun resolveStreamUrl(rawTrackId: String): String {
        val maxBitrate = context?.let { MusicQualityPreferences.resolveCurrentQuality(it).maxBitrateKbps }
        return client.buildStreamUrl(rawTrackId, maxBitrate)
    }

    private fun getUuid(stringId: String): UUID {
        return stringIdMap.getOrPut(stringId) {
            val generated = runCatching { UUID.fromString(stringId) }.getOrElse {
                UUID.nameUUIDFromBytes("subsonic:$stringId".toByteArray(Charsets.UTF_8))
            }
            uuidIdMap[generated] = stringId
            generated
        }
    }

    private fun getStringId(uuid: UUID): String =
        uuidIdMap[uuid] ?: uuid.toString()

    override suspend fun getMusicHome(): MusicResult<MusicHomeData> = runCatching {
        val recentAlbumsJson = client.executeGet("getAlbumList2", mapOf("type" to "newest", "size" to "20"))
        val albumList = recentAlbumsJson.optJSONObject("albumList2")?.optJSONArray("album") ?: JSONArray()
        val recentAlbums = mutableListOf<MusicAlbum>()
        for (i in 0 until albumList.length()) {
            val obj = albumList.getJSONObject(i)
            val id = getUuid(obj.optString("id"))
            recentAlbums.add(
                MusicAlbum(
                    id = id,
                    title = obj.optString("title", obj.optString("name", "Untitled")),
                    artist = obj.optString("artist", "Unknown Artist"),
                    artistId = obj.optString("artistId").takeIf { it.isNotBlank() }?.let { getUuid(it) },
                    year = obj.optInt("year").takeIf { it > 0 },
                    coverUrl = client.buildCoverArtUrl(obj.optString("coverArt", obj.optString("id"))),
                    trackCount = obj.optInt("songCount").takeIf { it > 0 },
                    genres = listOfNotNull(obj.optString("genre").takeIf { it.isNotBlank() }),
                )
            )
        }

        val playlistsJson = client.executeGet("getPlaylists")
        val playlistArray = playlistsJson.optJSONObject("playlists")?.optJSONArray("playlist") ?: JSONArray()
        val playlists = mutableListOf<MusicPlaylist>()
        for (i in 0 until playlistArray.length()) {
            val obj = playlistArray.getJSONObject(i)
            val id = getUuid(obj.optString("id"))
            playlists.add(
                MusicPlaylist(
                    id = id,
                    title = obj.optString("name", "Untitled Playlist"),
                    owner = obj.optString("owner"),
                    trackCount = obj.optInt("songCount"),
                    coverUrl = client.buildCoverArtUrl(obj.optString("coverArt", obj.optString("id"))),
                    durationMs = obj.optLong("duration") * 1000L,
                )
            )
        }

        MusicHomeData(
            recentAlbums = recentAlbums,
            playlists = playlists,
        )
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load Subsonic music home", it) },
    )

    override suspend fun getArtists(page: Int, query: String?): MusicResult<List<MusicArtist>> = runCatching {
        val json = client.executeGet("getArtists")
        val indexArray = json.optJSONObject("artists")?.optJSONArray("index") ?: JSONArray()
        val artists = mutableListOf<MusicArtist>()
        for (i in 0 until indexArray.length()) {
            val indexObj = indexArray.getJSONObject(i)
            val artistArray = indexObj.optJSONArray("artist") ?: JSONArray()
            for (j in 0 until artistArray.length()) {
                val artistObj = artistArray.getJSONObject(j)
                val id = getUuid(artistObj.optString("id"))
                val name = artistObj.optString("name")
                if (query.isNullOrBlank() || name.contains(query, ignoreCase = true)) {
                    artists.add(
                        MusicArtist(
                            id = id,
                            name = name,
                            imageUrl = client.buildCoverArtUrl(artistObj.optString("artistImageUrl", artistObj.optString("coverArt"))),
                            albumCount = artistObj.optInt("albumCount"),
                        )
                    )
                }
            }
        }
        artists
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load Subsonic artists", it) },
    )

    override suspend fun getArtistDetail(artistId: UUID): MusicResult<MusicArtistDetail> = runCatching {
        val sid = getStringId(artistId)
        val json = client.executeGet("getArtist", mapOf("id" to sid))
        val artistObj = json.optJSONObject("artist") ?: throw IllegalStateException("Artist not found")
        val albumArray = artistObj.optJSONArray("album") ?: JSONArray()
        val albums = mutableListOf<MusicAlbum>()
        for (i in 0 until albumArray.length()) {
            val obj = albumArray.getJSONObject(i)
            albums.add(
                MusicAlbum(
                    id = getUuid(obj.optString("id")),
                    title = obj.optString("title", obj.optString("name")),
                    artist = artistObj.optString("name"),
                    artistId = artistId,
                    year = obj.optInt("year").takeIf { it > 0 },
                    coverUrl = client.buildCoverArtUrl(obj.optString("coverArt", obj.optString("id"))),
                    trackCount = obj.optInt("songCount"),
                )
            )
        }
        MusicArtistDetail(
            id = artistId,
            name = artistObj.optString("name"),
            imageUrl = client.buildCoverArtUrl(artistObj.optString("artistImageUrl", artistObj.optString("coverArt"))),
            albums = albums,
        )
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load Subsonic artist detail", it) },
    )

    override suspend fun getAlbumDetail(albumId: UUID): MusicResult<MusicAlbumDetail> = runCatching {
        val sid = getStringId(albumId)
        val json = client.executeGet("getAlbum", mapOf("id" to sid))
        val albumObj = json.optJSONObject("album") ?: throw IllegalStateException("Album not found")
        val songArray = albumObj.optJSONArray("song") ?: JSONArray()
        val tracks = mutableListOf<VantafynMusicTrack>()
        val artistName = albumObj.optString("artist", "Unknown Artist")
        val albumTitle = albumObj.optString("title", albumObj.optString("name", "Untitled Album"))
        val coverUrl = client.buildCoverArtUrl(albumObj.optString("coverArt", sid))

        for (i in 0 until songArray.length()) {
            val obj = songArray.getJSONObject(i)
            tracks.add(
                parseSubsonicTrack(
                    obj = obj,
                    defaultArtist = artistName,
                    defaultAlbum = albumTitle,
                    defaultAlbumId = albumId,
                    defaultCoverArt = albumObj.optString("coverArt", sid),
                )
            )
        }

        MusicAlbumDetail(
            id = albumId,
            title = albumTitle,
            artist = artistName,
            artistId = albumObj.optString("artistId").takeIf { it.isNotBlank() }?.let { getUuid(it) },
            year = albumObj.optInt("year").takeIf { it > 0 },
            coverUrl = coverUrl,
            genres = listOfNotNull(albumObj.optString("genre").takeIf { it.isNotBlank() }),
            tracks = tracks,
        )
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load Subsonic album detail", it) },
    )

    override suspend fun getPlaylists(): MusicResult<List<MusicPlaylist>> = runCatching {
        val json = client.executeGet("getPlaylists")
        val playlistArray = json.optJSONObject("playlists")?.optJSONArray("playlist") ?: JSONArray()
        val playlists = mutableListOf<MusicPlaylist>()
        for (i in 0 until playlistArray.length()) {
            val obj = playlistArray.getJSONObject(i)
            playlists.add(
                MusicPlaylist(
                    id = getUuid(obj.optString("id")),
                    title = obj.optString("name", "Untitled Playlist"),
                    owner = obj.optString("owner"),
                    trackCount = obj.optInt("songCount"),
                    coverUrl = client.buildCoverArtUrl(obj.optString("coverArt", obj.optString("id"))),
                    durationMs = obj.optLong("duration") * 1000L,
                )
            )
        }
        playlists
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load Subsonic playlists", it) },
    )

    override suspend fun getPlaylistDetail(playlistId: UUID): MusicResult<MusicPlaylistDetail> = runCatching {
        val sid = getStringId(playlistId)
        val json = client.executeGet("getPlaylist", mapOf("id" to sid))
        val playlistObj = json.optJSONObject("playlist") ?: throw IllegalStateException("Playlist not found")
        val entryArray = playlistObj.optJSONArray("entry") ?: JSONArray()
        val tracks = mutableListOf<VantafynMusicTrack>()
        for (i in 0 until entryArray.length()) {
            val obj = entryArray.getJSONObject(i)
            tracks.add(parseSubsonicTrack(obj))
        }
        MusicPlaylistDetail(
            id = playlistId,
            title = playlistObj.optString("name", "Untitled Playlist"),
            owner = playlistObj.optString("owner"),
            tracks = tracks,
        )
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to load Subsonic playlist detail", it) },
    )

    override suspend fun searchMusic(query: String): MusicResult<MusicSearchResult> = runCatching {
        val json = client.executeGet("search3", mapOf("query" to query))
        val searchResult3 = json.optJSONObject("searchResult3") ?: JSONObject()
        val artistArray = searchResult3.optJSONArray("artist") ?: JSONArray()
        val albumArray = searchResult3.optJSONArray("album") ?: JSONArray()
        val songArray = searchResult3.optJSONArray("song") ?: JSONArray()

        val artists = mutableListOf<MusicArtist>()
        for (i in 0 until artistArray.length()) {
            val obj = artistArray.getJSONObject(i)
            artists.add(
                MusicArtist(
                    id = getUuid(obj.optString("id")),
                    name = obj.optString("name"),
                    imageUrl = client.buildCoverArtUrl(obj.optString("artistImageUrl", obj.optString("coverArt"))),
                )
            )
        }

        val albums = mutableListOf<MusicAlbum>()
        for (i in 0 until albumArray.length()) {
            val obj = albumArray.getJSONObject(i)
            albums.add(
                MusicAlbum(
                    id = getUuid(obj.optString("id")),
                    title = obj.optString("title", obj.optString("name")),
                    artist = obj.optString("artist", "Unknown Artist"),
                    artistId = obj.optString("artistId").takeIf { it.isNotBlank() }?.let { getUuid(it) },
                    year = obj.optInt("year").takeIf { it > 0 },
                    coverUrl = client.buildCoverArtUrl(obj.optString("coverArt", obj.optString("id"))),
                )
            )
        }

        val tracks = mutableListOf<VantafynMusicTrack>()
        for (i in 0 until songArray.length()) {
            val obj = songArray.getJSONObject(i)
            tracks.add(parseSubsonicTrack(obj))
        }

        MusicSearchResult(artists = artists, albums = albums, tracks = tracks)
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to search Subsonic music", it) },
    )

    override suspend fun resolveStreamUrl(trackId: UUID): String =
        resolveStreamUrl(getStringId(trackId))

    override suspend fun getLyrics(trackId: UUID): MusicResult<MusicLyrics?> = runCatching {
        val sid = getStringId(trackId)
        val json = runCatching { client.executeGet("getLyricsBySongId", mapOf("id" to sid)) }
            .getOrElse { client.executeGet("getLyrics", mapOf("id" to sid)) }
        val lyricsObj = json.optJSONObject("lyricsList")?.optJSONArray("structuredLyrics")?.optJSONObject(0)
            ?: json.optJSONObject("lyrics")
        if (lyricsObj == null) {
            return@runCatching null
        }

        val lineArray = lyricsObj.optJSONArray("line")
        val lines = mutableListOf<MusicLyricLine>()
        if (lineArray != null) {
            for (i in 0 until lineArray.length()) {
                val l = lineArray.getJSONObject(i)
                lines.add(MusicLyricLine(startMs = l.optLong("start"), text = l.optString("value", l.optString("text"))))
            }
        }
        val plain = lyricsObj.optString("content", lyricsObj.optString("value")).takeIf { it.isNotBlank() }

        MusicLyrics(syncedLines = lines, plainText = plain)
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to get lyrics", it) },
    )

    override suspend fun setFavorite(trackId: UUID, isFavorite: Boolean): MusicResult<Unit> = runCatching {
        val sid = getStringId(trackId)
        val endpoint = if (isFavorite) "star" else "unstar"
        client.executeGet(endpoint, mapOf("id" to sid))
        Unit
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to update favorite status", it) },
    )

    override suspend fun scrobble(trackId: UUID, submissionTimeMs: Long): MusicResult<Unit> = runCatching {
        val sid = getStringId(trackId)
        client.executeGet("scrobble", mapOf("id" to sid, "time" to submissionTimeMs.toString(), "submission" to "true"))
        Unit
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to scrobble track", it) },
    )

    override suspend fun getRandomSongs(size: Int): MusicResult<List<VantafynMusicTrack>> = runCatching {
        val json = client.executeGet("getRandomSongs", mapOf("size" to size.toString()))
        val songArray = json.optJSONObject("randomSongs")?.optJSONArray("song") ?: JSONArray()
        val tracks = mutableListOf<VantafynMusicTrack>()
        for (i in 0 until songArray.length()) {
            val obj = songArray.getJSONObject(i)
            tracks.add(parseSubsonicTrack(obj))
        }
        tracks
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to get random songs", it) },
    )

    override suspend fun getStarredSongs(): MusicResult<List<VantafynMusicTrack>> = runCatching {
        val json = runCatching { client.executeGet("getStarred2") }
            .getOrElse { client.executeGet("getStarred") }
        val starredObj = json.optJSONObject("starred2") ?: json.optJSONObject("starred") ?: JSONObject()
        val songArray = starredObj.optJSONArray("song") ?: JSONArray()
        val tracks = mutableListOf<VantafynMusicTrack>()
        for (i in 0 until songArray.length()) {
            val obj = songArray.getJSONObject(i)
            tracks.add(parseSubsonicTrack(obj, forceFavorite = true))
        }
        tracks
    }.fold(
        onSuccess = { MusicResult.Success(it) },
        onFailure = { MusicResult.Failure(it.message ?: "Failed to get starred songs", it) },
    )

    private fun parseSubsonicTrack(
        obj: JSONObject,
        defaultArtist: String? = null,
        defaultAlbum: String? = null,
        defaultAlbumId: UUID? = null,
        defaultCoverArt: String? = null,
        forceFavorite: Boolean? = null,
    ): VantafynMusicTrack {
        val sid = obj.optString("id")
        val trackId = getUuid(sid)
        val rg = obj.optJSONObject("replayGain")
        val gain = rg?.optDouble("trackGain")?.takeIf { !it.isNaN() }?.toFloat()
        val peak = rg?.optDouble("trackPeak")?.takeIf { !it.isNaN() }?.toFloat()
        val rawBitrate = obj.optInt("bitRate").takeIf { it > 0 } ?: obj.optInt("bitrate").takeIf { it > 0 }
        val suffix = obj.optString("suffix").takeIf { it.isNotBlank() }
        val samplingRate = obj.optInt("samplingRate").takeIf { it > 0 }
        val bitDepth = obj.optInt("bitDepth").takeIf { it > 0 }
        val channelCount = obj.optInt("channelCount").takeIf { it > 0 }
        val coverArtId = obj.optString("coverArt").takeIf { it.isNotBlank() } ?: defaultCoverArt ?: sid

        return VantafynMusicTrack(
            id = trackId,
            title = obj.optString("title", "Untitled Track"),
            artist = obj.optString("artist", defaultArtist ?: "Unknown Artist"),
            album = obj.optString("album").takeIf { it.isNotBlank() } ?: defaultAlbum,
            albumId = obj.optString("albumId").takeIf { it.isNotBlank() }?.let { getUuid(it) } ?: defaultAlbumId,
            durationMs = obj.optLong("duration") * 1000L,
            genres = listOfNotNull(obj.optString("genre").takeIf { it.isNotBlank() }),
            streamUrl = resolveStreamUrl(sid),
            artworkUrl = client.buildCoverArtUrl(coverArtId),
            isFavorite = forceFavorite ?: obj.optBoolean("starred", false),
            replayGainTrackGainDb = gain,
            replayGainTrackPeak = peak,
            container = suffix,
            codec = suffix,
            bitrate = rawBitrate,
            sampleRate = samplingRate,
            bitDepth = bitDepth,
            channels = channelCount,
        )
    }
}

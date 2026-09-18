package dev.vantafyn.feature.music.cache

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicArtist
import dev.vantafyn.core.jellyfin.JellyfinMusicPlaylist
import dev.vantafyn.core.jellyfin.JellyfinMusicTrack
import dev.vantafyn.core.jellyfin.MusicSongsFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

data class MusicLibraryCacheScope(
    val providerId: String,
    val serverId: String,
    val profileId: String,
)

class MusicLibraryCacheStore(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val database = MusicLibraryCacheDatabase(context.applicationContext)

    suspend fun hasFreshSongs(scope: MusicLibraryCacheScope): Boolean = withContext(dispatcher) {
        lastSyncedAt(scope, CacheKindSongs)?.let { System.currentTimeMillis() - it < CacheFreshnessMillis } == true
    }

    suspend fun getSongs(
        scope: MusicLibraryCacheScope,
        filter: MusicSongsFilter,
        alphabetKey: String?,
    ): List<JellyfinMusicTrack> = withContext(dispatcher) {
        val clauses = mutableListOf("provider_id = ?", "server_id = ?", "profile_id = ?")
        val args = mutableListOf(scope.providerId, scope.serverId, scope.profileId)
        if (filter == MusicSongsFilter.Favorites) {
            clauses += "is_favorite = 1"
        }
        val key = alphabetKey.takeIf { filter == MusicSongsFilter.AZ || filter == MusicSongsFilter.Favorites }
        when (key) {
            "#" -> clauses += "(sort_letter IS NULL OR sort_letter < 'A' OR sort_letter > 'Z')"
            null -> Unit
            else -> {
                clauses += "sort_letter = ?"
                args += key
            }
        }
        queryTracks(
            selection = clauses.joinToString(" AND "),
            selectionArgs = args.toTypedArray(),
            orderBy = "sort_title COLLATE NOCASE ASC, sort_artist COLLATE NOCASE ASC, sort_album COLLATE NOCASE ASC, item_id ASC",
        )
    }

    suspend fun replaceSongs(scope: MusicLibraryCacheScope, tracks: List<JellyfinMusicTrack>) = withContext(dispatcher) {
        database.writableDatabase.transaction {
            deleteScoped("music_tracks", scope)
            tracks.forEach { track ->
                insertWithOnConflict("music_tracks", null, track.toValues(scope), SQLiteDatabase.CONFLICT_REPLACE)
            }
            markSynced(scope, CacheKindSongs)
        }
        Unit
    }

    suspend fun replaceAlbums(scope: MusicLibraryCacheScope, albums: List<JellyfinMusicAlbum>) = withContext(dispatcher) {
        database.writableDatabase.transaction {
            deleteScoped("music_albums", scope)
            albums.forEach { album ->
                insertWithOnConflict("music_albums", null, album.toValues(scope), SQLiteDatabase.CONFLICT_REPLACE)
            }
            markSynced(scope, CacheKindAlbums)
        }
        Unit
    }

    suspend fun replaceArtists(scope: MusicLibraryCacheScope, artists: List<JellyfinMusicArtist>) = withContext(dispatcher) {
        database.writableDatabase.transaction {
            deleteScoped("music_artists", scope)
            artists.forEach { artist ->
                insertWithOnConflict("music_artists", null, artist.toValues(scope), SQLiteDatabase.CONFLICT_REPLACE)
            }
            markSynced(scope, CacheKindArtists)
        }
        Unit
    }

    suspend fun replacePlaylists(scope: MusicLibraryCacheScope, playlists: List<JellyfinMusicPlaylist>) = withContext(dispatcher) {
        database.writableDatabase.transaction {
            deleteScoped("music_playlists", scope)
            playlists.forEach { playlist ->
                insertWithOnConflict("music_playlists", null, playlist.toValues(scope), SQLiteDatabase.CONFLICT_REPLACE)
            }
            markSynced(scope, CacheKindPlaylists)
        }
        Unit
    }

    suspend fun replaceHomeLibrary(scope: MusicLibraryCacheScope, albums: List<JellyfinMusicAlbum>, artists: List<JellyfinMusicArtist>, playlists: List<JellyfinMusicPlaylist>) {
        replaceAlbums(scope, albums)
        replaceArtists(scope, artists)
        replacePlaylists(scope, playlists)
    }

    private fun queryTracks(
        selection: String,
        selectionArgs: Array<String>,
        orderBy: String,
    ): List<JellyfinMusicTrack> {
        val cursor = database.readableDatabase.query(
            "music_tracks",
            null,
            selection,
            selectionArgs,
            null,
            null,
            orderBy,
        )
        return cursor.use {
            buildList {
                while (it.moveToNext()) add(it.toTrack())
            }
        }
    }

    private fun SQLiteDatabase.deleteScoped(table: String, scope: MusicLibraryCacheScope) {
        delete(
            table,
            "provider_id = ? AND server_id = ? AND profile_id = ?",
            arrayOf(scope.providerId, scope.serverId, scope.profileId),
        )
    }

    private fun SQLiteDatabase.markSynced(scope: MusicLibraryCacheScope, kind: String) {
        insertWithOnConflict(
            "music_cache_state",
            null,
            ContentValues().apply {
                put("provider_id", scope.providerId)
                put("server_id", scope.serverId)
                put("profile_id", scope.profileId)
                put("kind", kind)
                put("synced_at_millis", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun lastSyncedAt(scope: MusicLibraryCacheScope, kind: String): Long? {
        val cursor = database.readableDatabase.query(
            "music_cache_state",
            arrayOf("synced_at_millis"),
            "provider_id = ? AND server_id = ? AND profile_id = ? AND kind = ?",
            arrayOf(scope.providerId, scope.serverId, scope.profileId, kind),
            null,
            null,
            null,
            "1",
        )
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else null }
    }
}

private class MusicLibraryCacheDatabase(context: Context) : SQLiteOpenHelper(
    context,
    "vantafyn_music_library_cache.db",
    null,
    1,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE music_cache_state (
                provider_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                profile_id TEXT NOT NULL,
                kind TEXT NOT NULL,
                synced_at_millis INTEGER NOT NULL,
                PRIMARY KEY(provider_id, server_id, profile_id, kind)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE music_tracks (
                provider_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                profile_id TEXT NOT NULL,
                item_id TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                album_id TEXT,
                duration_ms INTEGER,
                artwork_url TEXT,
                stream_url TEXT NOT NULL,
                has_lyrics INTEGER NOT NULL,
                is_favorite INTEGER NOT NULL,
                genres_json TEXT NOT NULL,
                replay_gain_track_gain_db REAL,
                replay_gain_track_peak REAL,
                container TEXT,
                codec TEXT,
                bitrate INTEGER,
                sample_rate INTEGER,
                bit_depth INTEGER,
                channels INTEGER,
                play_count INTEGER,
                sort_title TEXT NOT NULL,
                sort_artist TEXT NOT NULL,
                sort_album TEXT NOT NULL,
                sort_letter TEXT,
                updated_at_millis INTEGER NOT NULL,
                PRIMARY KEY(provider_id, server_id, profile_id, item_id)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_music_tracks_scope_sort ON music_tracks(provider_id, server_id, profile_id, sort_title COLLATE NOCASE, sort_artist COLLATE NOCASE)")
        db.execSQL("CREATE INDEX idx_music_tracks_scope_letter ON music_tracks(provider_id, server_id, profile_id, sort_letter)")
        db.execSQL("CREATE INDEX idx_music_tracks_scope_favorite ON music_tracks(provider_id, server_id, profile_id, is_favorite)")
        db.execSQL(
            """
            CREATE TABLE music_albums (
                provider_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                profile_id TEXT NOT NULL,
                item_id TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT,
                year INTEGER,
                artwork_url TEXT,
                genres_json TEXT NOT NULL,
                is_favorite INTEGER NOT NULL,
                sort_title TEXT NOT NULL,
                updated_at_millis INTEGER NOT NULL,
                PRIMARY KEY(provider_id, server_id, profile_id, item_id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE music_artists (
                provider_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                profile_id TEXT NOT NULL,
                item_id TEXT NOT NULL,
                name TEXT NOT NULL,
                image_url TEXT,
                sort_name TEXT NOT NULL,
                updated_at_millis INTEGER NOT NULL,
                PRIMARY KEY(provider_id, server_id, profile_id, item_id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE music_playlists (
                provider_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                profile_id TEXT NOT NULL,
                item_id TEXT NOT NULL,
                name TEXT NOT NULL,
                image_url TEXT,
                track_count INTEGER,
                track_image_urls_json TEXT NOT NULL,
                is_favorite INTEGER NOT NULL,
                is_user_created INTEGER NOT NULL,
                sort_name TEXT NOT NULL,
                updated_at_millis INTEGER NOT NULL,
                PRIMARY KEY(provider_id, server_id, profile_id, item_id)
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS music_playlists")
        db.execSQL("DROP TABLE IF EXISTS music_artists")
        db.execSQL("DROP TABLE IF EXISTS music_albums")
        db.execSQL("DROP TABLE IF EXISTS music_tracks")
        db.execSQL("DROP TABLE IF EXISTS music_cache_state")
        onCreate(db)
    }
}

private fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

private fun JellyfinMusicTrack.toValues(scope: MusicLibraryCacheScope): ContentValues =
    ContentValues().apply {
        putScope(scope)
        put("item_id", id.toString())
        put("title", title)
        put("artist", artist)
        putNullable("album", album)
        putNullable("album_id", albumId?.toString())
        putNullable("duration_ms", durationMs)
        putNullable("artwork_url", artworkUrl)
        put("stream_url", streamUrl)
        put("has_lyrics", hasLyrics.asInt())
        put("is_favorite", isFavorite.asInt())
        put("genres_json", genres.toJsonArrayString())
        putNullable("replay_gain_track_gain_db", replayGainTrackGainDb)
        putNullable("replay_gain_track_peak", replayGainTrackPeak)
        putNullable("container", container)
        putNullable("codec", codec)
        putNullable("bitrate", bitrate)
        putNullable("sample_rate", sampleRate)
        putNullable("bit_depth", bitDepth)
        putNullable("channels", channels)
        putNullable("play_count", playCount)
        put("sort_title", title.toSortKey())
        put("sort_artist", artist.toSortKey())
        put("sort_album", album.orEmpty().toSortKey())
        putNullable("sort_letter", title.sortLetter())
        put("updated_at_millis", System.currentTimeMillis())
    }

private fun JellyfinMusicAlbum.toValues(scope: MusicLibraryCacheScope): ContentValues =
    ContentValues().apply {
        putScope(scope)
        put("item_id", id.toString())
        put("title", title)
        putNullable("artist", artist)
        putNullable("year", year)
        putNullable("artwork_url", artworkUrl)
        put("genres_json", genres.toJsonArrayString())
        put("is_favorite", isFavorite.asInt())
        put("sort_title", title.toSortKey())
        put("updated_at_millis", System.currentTimeMillis())
    }

private fun JellyfinMusicArtist.toValues(scope: MusicLibraryCacheScope): ContentValues =
    ContentValues().apply {
        putScope(scope)
        put("item_id", id.toString())
        put("name", name)
        putNullable("image_url", imageUrl)
        put("sort_name", name.toSortKey())
        put("updated_at_millis", System.currentTimeMillis())
    }

private fun JellyfinMusicPlaylist.toValues(scope: MusicLibraryCacheScope): ContentValues =
    ContentValues().apply {
        putScope(scope)
        put("item_id", id.toString())
        put("name", name)
        putNullable("image_url", imageUrl)
        putNullable("track_count", trackCount)
        put("track_image_urls_json", trackImageUrls.toJsonArrayString())
        put("is_favorite", isFavorite.asInt())
        put("is_user_created", isUserCreated.asInt())
        put("sort_name", name.toSortKey())
        put("updated_at_millis", System.currentTimeMillis())
    }

private fun Cursor.toTrack(): JellyfinMusicTrack =
    JellyfinMusicTrack(
        id = UUID.fromString(string("item_id")),
        title = string("title"),
        artist = string("artist"),
        album = nullableString("album"),
        albumId = nullableString("album_id")?.let(UUID::fromString),
        durationMs = nullableLong("duration_ms"),
        artworkUrl = nullableString("artwork_url"),
        hasLyrics = int("has_lyrics") == 1,
        streamUrl = string("stream_url"),
        isFavorite = int("is_favorite") == 1,
        genres = string("genres_json").jsonArrayToStrings(),
        replayGainTrackGainDb = nullableFloat("replay_gain_track_gain_db"),
        replayGainTrackPeak = nullableFloat("replay_gain_track_peak"),
        container = nullableString("container"),
        codec = nullableString("codec"),
        bitrate = nullableInt("bitrate"),
        sampleRate = nullableInt("sample_rate"),
        bitDepth = nullableInt("bit_depth"),
        channels = nullableInt("channels"),
        playCount = nullableInt("play_count"),
    )

private fun ContentValues.putScope(scope: MusicLibraryCacheScope) {
    put("provider_id", scope.providerId)
    put("server_id", scope.serverId)
    put("profile_id", scope.profileId)
}

private fun ContentValues.putNullable(key: String, value: String?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun ContentValues.putNullable(key: String, value: Int?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun ContentValues.putNullable(key: String, value: Long?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun ContentValues.putNullable(key: String, value: Float?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun Cursor.string(name: String): String = getString(getColumnIndexOrThrow(name))

private fun Cursor.nullableString(name: String): String? {
    val index = getColumnIndexOrThrow(name)
    return if (isNull(index)) null else getString(index)
}

private fun Cursor.int(name: String): Int = getInt(getColumnIndexOrThrow(name))

private fun Cursor.nullableInt(name: String): Int? {
    val index = getColumnIndexOrThrow(name)
    return if (isNull(index)) null else getInt(index)
}

private fun Cursor.nullableLong(name: String): Long? {
    val index = getColumnIndexOrThrow(name)
    return if (isNull(index)) null else getLong(index)
}

private fun Cursor.nullableFloat(name: String): Float? {
    val index = getColumnIndexOrThrow(name)
    return if (isNull(index)) null else getFloat(index)
}

private fun Boolean.asInt(): Int = if (this) 1 else 0

private fun String.toSortKey(): String = trim().lowercase()

private fun String.sortLetter(): String? {
    val first = trim().firstOrNull()?.uppercaseChar() ?: return null
    return first.takeIf { it in 'A'..'Z' }?.toString()
}

private fun List<String>.toJsonArrayString(): String =
    JSONArray().also { array -> forEach { array.put(it) } }.toString()

private fun String.jsonArrayToStrings(): List<String> {
    val array = runCatching { JSONArray(this) }.getOrNull() ?: return emptyList()
    return buildList {
        for (i in 0 until array.length()) {
            array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private const val CacheKindSongs = "songs"
private const val CacheKindAlbums = "albums"
private const val CacheKindArtists = "artists"
private const val CacheKindPlaylists = "playlists"
private const val CacheFreshnessMillis = 6 * 60 * 60 * 1000L

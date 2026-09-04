package dev.vantafyn.feature.music.harmonia

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class SqliteHarmoniaStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HarmoniaHistoryRepository, HarmoniaRecapRepository {
    private val database = HarmoniaDatabase(context.applicationContext)

    override suspend fun add(record: HarmoniaPlaybackRecord): Unit =
        withContext(ioDispatcher) {
            database.writableDatabase.insertWithOnConflict(
                "harmonia_playback_records",
                null,
                record.toValues(),
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }

    override suspend fun recordsFor(
        userId: UUID,
        serverId: String,
        profileId: String,
        period: HarmoniaPeriodRange,
    ): List<HarmoniaPlaybackRecord> =
        withContext(ioDispatcher) {
            database.readableDatabase.query(
                "harmonia_playback_records",
                null,
                "user_id = ? AND server_id = ? AND profile_id = ? AND started_at_ms >= ? AND started_at_ms < ?",
                arrayOf(userId.toString(), serverId, profileId, period.start.toEpochMilli().toString(), period.endExclusive.toEpochMilli().toString()),
                null,
                null,
                "started_at_ms ASC",
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.toPlaybackRecord())
                    }
                }
            }
        }

    override suspend fun recentRecords(
        userId: UUID,
        serverId: String,
        profileId: String,
        limit: Int,
    ): List<HarmoniaPlaybackRecord> =
        withContext(ioDispatcher) {
            database.readableDatabase.query(
                "harmonia_playback_records",
                null,
                "user_id = ? AND server_id = ? AND profile_id = ?",
                arrayOf(userId.toString(), serverId, profileId),
                null,
                null,
                "started_at_ms DESC",
                limit.toString(),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.toPlaybackRecord())
                    }
                }
            }
        }

    override suspend fun upsert(recap: HarmoniaRecap, serverId: String, profileId: String): Unit =
        withContext(ioDispatcher) {
            database.writableDatabase.insertWithOnConflict(
                "harmonia_recaps",
                null,
                recap.toValues(serverId, profileId),
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }

    override suspend fun latestPreviews(userId: UUID, serverId: String, profileId: String): List<HarmoniaRecapPreview> =
        withContext(ioDispatcher) {
            database.readableDatabase.query(
                "harmonia_recaps",
                null,
                "user_id = ? AND server_id = ? AND profile_id = ? AND data_version = ?",
                arrayOf(userId.toString(), serverId, profileId, HARMONIA_DATA_VERSION.toString()),
                null,
                null,
                "period_start_ms DESC, generated_at_ms DESC",
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.toRecapPreview())
                    }
                }.distinctBy { it.periodType }
            }
        }

    override suspend fun previewForPeriod(
        userId: UUID,
        serverId: String,
        profileId: String,
        period: HarmoniaPeriodRange,
    ): HarmoniaRecapPreview? =
        withContext(ioDispatcher) {
            database.readableDatabase.query(
                "harmonia_recaps",
                null,
                "user_id = ? AND server_id = ? AND profile_id = ? AND period_type = ? AND period_start_ms = ? AND data_version = ?",
                arrayOf(
                    userId.toString(),
                    serverId,
                    profileId,
                    period.type.name,
                    period.start.toEpochMilli().toString(),
                    HARMONIA_DATA_VERSION.toString(),
                ),
                null,
                null,
                "generated_at_ms DESC",
                "1",
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.toRecapPreview() else null
            }
        }
}

private class HarmoniaDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS harmonia_playback_records (
                id TEXT PRIMARY KEY NOT NULL,
                user_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                profile_id TEXT NOT NULL,
                track_id TEXT NOT NULL,
                track_title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                album_id TEXT,
                genres TEXT NOT NULL,
                started_at_ms INTEGER NOT NULL,
                ended_at_ms INTEGER NOT NULL,
                listened_ms INTEGER NOT NULL,
                duration_ms INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_harmonia_records_user_period
            ON harmonia_playback_records(user_id, server_id, profile_id, started_at_ms)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS harmonia_recaps (
                id TEXT PRIMARY KEY NOT NULL,
                user_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                profile_id TEXT NOT NULL,
                period_type TEXT NOT NULL,
                period_start_ms INTEGER NOT NULL,
                period_end_ms INTEGER NOT NULL,
                generated_at_ms INTEGER NOT NULL,
                data_version INTEGER NOT NULL,
                total_listening_time_ms INTEGER NOT NULL DEFAULT 0,
                total_tracks_played INTEGER NOT NULL DEFAULT 0,
                top_artist TEXT,
                top_track TEXT,
                record_count INTEGER NOT NULL DEFAULT 0,
                history_signature TEXT NOT NULL DEFAULT '',
                is_finalized INTEGER NOT NULL DEFAULT 0,
                summary TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_harmonia_recaps_period
            ON harmonia_recaps(user_id, server_id, profile_id, period_type, period_start_ms, data_version)
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE harmonia_recaps ADD COLUMN total_listening_time_ms INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE harmonia_recaps ADD COLUMN total_tracks_played INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE harmonia_recaps ADD COLUMN top_artist TEXT")
            db.execSQL("ALTER TABLE harmonia_recaps ADD COLUMN top_track TEXT")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE harmonia_recaps ADD COLUMN record_count INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE harmonia_recaps ADD COLUMN history_signature TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE harmonia_recaps ADD COLUMN is_finalized INTEGER NOT NULL DEFAULT 0")
        }
        onCreate(db)
    }

    private companion object {
        const val DATABASE_NAME = "vantafyn_harmonia.db"
        const val DATABASE_VERSION = 3
    }
}

private fun HarmoniaPlaybackRecord.toValues(): ContentValues =
    ContentValues().apply {
        put("id", id)
        put("user_id", userId.toString())
        put("server_id", serverId)
        put("profile_id", profileId)
        put("track_id", trackId.toString())
        put("track_title", trackTitle)
        put("artist", artist)
        put("album", album)
        put("album_id", albumId?.toString())
        put("genres", genres.joinToString(separator = GENRE_SEPARATOR))
        put("started_at_ms", startedAt.toEpochMilli())
        put("ended_at_ms", endedAt.toEpochMilli())
        put("listened_ms", listenedMs)
        put("duration_ms", durationMs)
    }

private fun HarmoniaRecap.toValues(serverId: String, profileId: String): ContentValues =
    ContentValues().apply {
        put("id", id)
        put("user_id", userId.toString())
        put("server_id", serverId)
        put("profile_id", profileId)
        put("period_type", periodType.name)
        put("period_start_ms", periodStart.toEpochMilli())
        put("period_end_ms", periodEnd.toEpochMilli())
        put("generated_at_ms", generatedAt.toEpochMilli())
        put("data_version", dataVersion)
        put("total_listening_time_ms", statistics.totalListeningTimeMs.value ?: 0L)
        put("total_tracks_played", statistics.totalTracksPlayed.value ?: 0)
        put("top_artist", statistics.topArtists.value?.firstOrNull()?.label)
        put("top_track", statistics.topTracks.value?.firstOrNull()?.label)
        put("record_count", recordCount)
        put("history_signature", historySignature)
        put("is_finalized", if (isFinalized) 1 else 0)
        put("summary", statistics.summaryText())
    }

private fun android.database.Cursor.toPlaybackRecord(): HarmoniaPlaybackRecord =
    HarmoniaPlaybackRecord(
        id = string("id"),
        userId = UUID.fromString(string("user_id")),
        serverId = string("server_id"),
        profileId = string("profile_id"),
        trackId = UUID.fromString(string("track_id")),
        trackTitle = string("track_title"),
        artist = string("artist"),
        album = nullableString("album"),
        albumId = nullableString("album_id")?.let(UUID::fromString),
        genres = string("genres").split(GENRE_SEPARATOR).filter { it.isNotBlank() },
        startedAt = Instant.ofEpochMilli(long("started_at_ms")),
        endedAt = Instant.ofEpochMilli(long("ended_at_ms")),
        listenedMs = long("listened_ms"),
        durationMs = nullableLong("duration_ms"),
    )

private fun android.database.Cursor.toRecapPreview(): HarmoniaRecapPreview =
    HarmoniaRecapPreview(
        id = string("id"),
        userId = UUID.fromString(string("user_id")),
        periodType = HarmoniaPeriod.valueOf(string("period_type")),
        periodStart = Instant.ofEpochMilli(long("period_start_ms")),
        periodEnd = Instant.ofEpochMilli(long("period_end_ms")),
        generatedAt = Instant.ofEpochMilli(long("generated_at_ms")),
        dataVersion = long("data_version").toInt(),
        totalListeningTimeMs = long("total_listening_time_ms"),
        totalTracksPlayed = long("total_tracks_played").toInt(),
        topArtist = nullableString("top_artist"),
        topTrack = nullableString("top_track"),
        recordCount = long("record_count").toInt(),
        historySignature = string("history_signature"),
        isFinalized = long("is_finalized") == 1L,
    )

private fun HarmoniaStatistics.summaryText(): String =
    listOf(
        "totalListeningTimeMs=${totalListeningTimeMs.value}",
        "totalTracksPlayed=${totalTracksPlayed.value}",
        "uniqueTracks=${uniqueTracks.value}",
        "uniqueArtists=${uniqueArtists.value}",
        "topArtist=${topArtists.value?.firstOrNull()?.label}",
        "topTrack=${topTracks.value?.firstOrNull()?.label}",
        "topAlbum=${topAlbums.value?.firstOrNull()?.label}",
        "topGenre=${topGenres.value?.firstOrNull()?.label}",
    ).joinToString(separator = "\n")

private fun android.database.Cursor.string(column: String): String =
    getString(getColumnIndexOrThrow(column))

private fun android.database.Cursor.nullableString(column: String): String? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getString(index)
}

private fun android.database.Cursor.long(column: String): Long =
    getLong(getColumnIndexOrThrow(column))

private fun android.database.Cursor.nullableLong(column: String): Long? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getLong(index)
}

private const val GENRE_SEPARATOR = "||"

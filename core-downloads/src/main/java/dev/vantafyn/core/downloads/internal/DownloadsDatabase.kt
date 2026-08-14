package dev.vantafyn.core.downloads.internal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class DownloadsDatabase(
    context: Context,
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_DOWNLOAD_RECORDS)
        db.execSQL(CREATE_DOWNLOAD_IDENTITY_INDEX)
        db.execSQL(CREATE_DOWNLOAD_USER_STATE_INDEX)
        db.execSQL(CREATE_PENDING_MUTATIONS)
        db.execSQL(CREATE_PENDING_MUTATION_STATE_INDEX)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        if (oldVersion < 2) {
            db.addColumnIfMissing("download_records", "remote_poster_url", "TEXT")
            db.addColumnIfMissing("download_records", "remote_backdrop_url", "TEXT")
            db.addColumnIfMissing("download_records", "remote_logo_url", "TEXT")
            db.addColumnIfMissing("pending_user_data_mutations", "profile_id", "TEXT NOT NULL DEFAULT ''")
        }
    }

    companion object {
        const val DATABASE_NAME = "vantafyn_downloads.db"
        const val DATABASE_VERSION = 2

        private const val CREATE_DOWNLOAD_RECORDS = """
            CREATE TABLE download_records (
                id TEXT NOT NULL PRIMARY KEY,
                profile_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                item_id TEXT NOT NULL,
                media_source_id TEXT NOT NULL,
                media_type TEXT NOT NULL,
                title TEXT NOT NULL,
                sort_title TEXT,
                overview TEXT,
                year INTEGER,
                runtime_ticks INTEGER,
                parent_id TEXT,
                series_id TEXT,
                series_name TEXT,
                season_id TEXT,
                season_name TEXT,
                season_number INTEGER,
                episode_number INTEGER,
                album_id TEXT,
                album_name TEXT,
                artist_name TEXT,
                local_media_path TEXT,
                temp_media_path TEXT,
                local_poster_path TEXT,
                local_backdrop_path TEXT,
                local_logo_path TEXT,
                local_subtitle_path TEXT,
                remote_poster_url TEXT,
                remote_backdrop_url TEXT,
                remote_logo_url TEXT,
                selected_audio_track_id TEXT,
                selected_subtitle_track_id TEXT,
                state TEXT NOT NULL,
                bytes_downloaded INTEGER NOT NULL DEFAULT 0,
                total_bytes INTEGER,
                created_at_millis INTEGER NOT NULL,
                updated_at_millis INTEGER NOT NULL,
                completed_at_millis INTEGER,
                failure_category TEXT,
                failure_reason TEXT,
                local_playback_position_ticks INTEGER NOT NULL DEFAULT 0,
                local_played INTEGER NOT NULL DEFAULT 0,
                sync_state TEXT NOT NULL,
                UNIQUE(server_id, user_id, item_id, media_source_id)
            )
        """

        private const val CREATE_DOWNLOAD_IDENTITY_INDEX = """
            CREATE INDEX idx_download_records_identity
            ON download_records(server_id, user_id, item_id, media_source_id)
        """

        private const val CREATE_DOWNLOAD_USER_STATE_INDEX = """
            CREATE INDEX idx_download_records_user_state
            ON download_records(server_id, user_id, state)
        """

        private const val CREATE_PENDING_MUTATIONS = """
            CREATE TABLE pending_user_data_mutations (
                profile_id TEXT NOT NULL,
                server_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                item_id TEXT NOT NULL,
                playback_position_ticks INTEGER NOT NULL,
                played INTEGER NOT NULL DEFAULT 0,
                updated_at_millis INTEGER NOT NULL,
                retry_count INTEGER NOT NULL DEFAULT 0,
                sync_state TEXT NOT NULL,
                failure_reason TEXT,
                PRIMARY KEY(server_id, user_id, item_id)
            )
        """

        private const val CREATE_PENDING_MUTATION_STATE_INDEX = """
            CREATE INDEX idx_pending_user_data_mutations_state
            ON pending_user_data_mutations(sync_state)
        """
    }
}

private fun SQLiteDatabase.addColumnIfMissing(
    table: String,
    column: String,
    definition: String,
) {
    val exists = rawQuery("PRAGMA table_info($table)", null).use { cursor ->
        var found = false
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == column) {
                found = true
                break
            }
        }
        found
    }
    if (!exists) {
        execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
    }
}

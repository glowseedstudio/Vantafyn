package dev.vantafyn.core.downloads

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import dev.vantafyn.core.downloads.internal.DownloadsDatabase
import dev.vantafyn.core.downloads.internal.booleanInt
import dev.vantafyn.core.downloads.internal.int
import dev.vantafyn.core.downloads.internal.long
import dev.vantafyn.core.downloads.internal.nullableInt
import dev.vantafyn.core.downloads.internal.nullableLong
import dev.vantafyn.core.downloads.internal.nullableString
import dev.vantafyn.core.downloads.internal.string
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteDownloadRepository(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DownloadRepository {
    private val database = DownloadsDatabase(context.applicationContext)

    override suspend fun upsert(record: DownloadRecord) = withContext(dispatcher) {
        database.writableDatabase.insertWithOnConflict(
            "download_records",
            null,
            record.toValues(),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        Unit
    }

    override suspend fun get(id: String): DownloadRecord? = withContext(dispatcher) {
        queryDownload(
            selection = "id = ?",
            selectionArgs = arrayOf(id),
        ).firstOrNull()
    }

    override suspend fun getByIdentity(identity: DownloadIdentity): DownloadRecord? = withContext(dispatcher) {
        queryDownload(
            selection = "server_id = ? AND user_id = ? AND item_id = ? AND media_source_id = ?",
            selectionArgs = arrayOf(identity.serverId, identity.userId, identity.itemId, identity.mediaSourceId),
        ).firstOrNull()
    }

    override suspend fun listForUser(
        serverId: String,
        userId: String,
    ): List<DownloadRecord> = withContext(dispatcher) {
        queryDownload(
            selection = "server_id = ? AND user_id = ?",
            selectionArgs = arrayOf(serverId, userId),
            orderBy = "sort_title COLLATE NOCASE ASC, title COLLATE NOCASE ASC",
        )
    }

    override suspend fun listByState(
        serverId: String,
        userId: String,
        states: Set<DownloadState>,
    ): List<DownloadRecord> = withContext(dispatcher) {
        if (states.isEmpty()) return@withContext emptyList()
        val placeholders = states.joinToString(",") { "?" }
        queryDownload(
            selection = "server_id = ? AND user_id = ? AND state IN ($placeholders)",
            selectionArgs = arrayOf(serverId, userId, *states.map { it.name }.toTypedArray()),
            orderBy = "updated_at_millis DESC",
        )
    }

    override suspend fun updateState(
        id: String,
        state: DownloadState,
        updatedAtMillis: Long,
        failureCategory: DownloadFailureCategory?,
        failureReason: String?,
    ) = withContext(dispatcher) {
        val values = ContentValues().apply {
            put("state", state.name)
            put("updated_at_millis", updatedAtMillis)
            if (state == DownloadState.Completed) {
                put("completed_at_millis", updatedAtMillis)
            }
            if (failureCategory != null) {
                put("failure_category", failureCategory.name)
            } else {
                putNull("failure_category")
            }
            if (failureReason != null) {
                put("failure_reason", failureReason)
            } else {
                putNull("failure_reason")
            }
        }
        database.writableDatabase.update("download_records", values, "id = ?", arrayOf(id))
        Unit
    }

    override suspend fun updateProgress(
        id: String,
        bytesDownloaded: Long,
        totalBytes: Long?,
        updatedAtMillis: Long,
    ) = withContext(dispatcher) {
        val values = ContentValues().apply {
            put("bytes_downloaded", bytesDownloaded.coerceAtLeast(0L))
            if (totalBytes != null) put("total_bytes", totalBytes.coerceAtLeast(0L)) else putNull("total_bytes")
            put("updated_at_millis", updatedAtMillis)
        }
        database.writableDatabase.update("download_records", values, "id = ?", arrayOf(id))
        Unit
    }

    override suspend fun updateLocalPlaybackState(
        id: String,
        playbackPositionTicks: Long,
        played: Boolean,
        syncState: DownloadSyncState,
        updatedAtMillis: Long,
    ) = withContext(dispatcher) {
        val values = ContentValues().apply {
            put("local_playback_position_ticks", playbackPositionTicks.coerceAtLeast(0L))
            put("local_played", if (played) 1 else 0)
            put("sync_state", syncState.name)
            put("updated_at_millis", updatedAtMillis)
        }
        database.writableDatabase.update("download_records", values, "id = ?", arrayOf(id))
        Unit
    }

    override suspend fun updateLocalArtworkPaths(
        id: String,
        localPosterPath: String?,
        localBackdropPath: String?,
        localLogoPath: String?,
        updatedAtMillis: Long,
    ) = withContext(dispatcher) {
        val values = ContentValues().apply {
            putNullable("local_poster_path", localPosterPath)
            putNullable("local_backdrop_path", localBackdropPath)
            putNullable("local_logo_path", localLogoPath)
            put("updated_at_millis", updatedAtMillis)
        }
        database.writableDatabase.update("download_records", values, "id = ?", arrayOf(id))
        Unit
    }

    override suspend fun delete(id: String) = withContext(dispatcher) {
        database.writableDatabase.delete("download_records", "id = ?", arrayOf(id))
        Unit
    }

    override suspend fun upsertPendingUserDataMutation(mutation: PendingUserDataMutation) = withContext(dispatcher) {
        database.writableDatabase.insertWithOnConflict(
            "pending_user_data_mutations",
            null,
            mutation.toValues(),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        Unit
    }

    override suspend fun listPendingUserDataMutations(
        serverId: String?,
        userId: String?,
    ): List<PendingUserDataMutation> = withContext(dispatcher) {
        val clauses = mutableListOf("sync_state != ?")
        val args = mutableListOf(DownloadSyncState.Synced.name)
        if (serverId != null) {
            clauses += "server_id = ?"
            args += serverId
        }
        if (userId != null) {
            clauses += "user_id = ?"
            args += userId
        }
        queryMutations(
            selection = clauses.joinToString(" AND "),
            selectionArgs = args.toTypedArray(),
            orderBy = "updated_at_millis ASC",
        )
    }

    override suspend fun markPendingUserDataMutationSynced(
        serverId: String,
        userId: String,
        itemId: String,
    ) = withContext(dispatcher) {
        val values = ContentValues().apply {
            put("sync_state", DownloadSyncState.Synced.name)
            putNull("failure_reason")
        }
        database.writableDatabase.update(
            "pending_user_data_mutations",
            values,
            "server_id = ? AND user_id = ? AND item_id = ?",
            arrayOf(serverId, userId, itemId),
        )
        Unit
    }

    override suspend fun markPendingUserDataMutationFailed(
        serverId: String,
        userId: String,
        itemId: String,
        retryCount: Int,
        failureReason: String?,
    ) = withContext(dispatcher) {
        val values = ContentValues().apply {
            put("sync_state", DownloadSyncState.Failed.name)
            put("retry_count", retryCount.coerceAtLeast(0))
            if (failureReason != null) put("failure_reason", failureReason) else putNull("failure_reason")
        }
        database.writableDatabase.update(
            "pending_user_data_mutations",
            values,
            "server_id = ? AND user_id = ? AND item_id = ?",
            arrayOf(serverId, userId, itemId),
        )
        Unit
    }

    override suspend fun storageSummary(
        serverId: String,
        userId: String,
    ): DownloadStorageSummary = withContext(dispatcher) {
        database.readableDatabase.rawQuery(
            """
                SELECT
                    COUNT(*) AS record_count,
                    SUM(CASE WHEN state = ? THEN 1 ELSE 0 END) AS completed_count,
                    SUM(CASE WHEN state IN (?, ?, ?, ?, ?) THEN 1 ELSE 0 END) AS active_count,
                    SUM(CASE WHEN state = ? THEN 1 ELSE 0 END) AS failed_count,
                    COALESCE(SUM(COALESCE(total_bytes, bytes_downloaded)), 0) AS total_bytes
                FROM download_records
                WHERE server_id = ? AND user_id = ?
            """.trimIndent(),
            arrayOf(
                DownloadState.Completed.name,
                DownloadState.Queued.name,
                DownloadState.Preparing.name,
                DownloadState.WaitingForNetwork.name,
                DownloadState.WaitingForWifi.name,
                DownloadState.Downloading.name,
                DownloadState.Failed.name,
                serverId,
                userId,
            ),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                DownloadStorageSummary(0, 0, 0, 0, 0L)
            } else {
                DownloadStorageSummary(
                    recordCount = cursor.int("record_count"),
                    completedCount = cursor.int("completed_count"),
                    activeCount = cursor.int("active_count"),
                    failedCount = cursor.int("failed_count"),
                    totalBytes = cursor.long("total_bytes"),
                )
            }
        }
    }

    private fun queryDownload(
        selection: String,
        selectionArgs: Array<String>,
        orderBy: String? = null,
    ): List<DownloadRecord> {
        return database.readableDatabase.query(
            "download_records",
            null,
            selection,
            selectionArgs,
            null,
            null,
            orderBy,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toDownloadRecord())
                }
            }
        }
    }

    private fun queryMutations(
        selection: String,
        selectionArgs: Array<String>,
        orderBy: String? = null,
    ): List<PendingUserDataMutation> {
        return database.readableDatabase.query(
            "pending_user_data_mutations",
            null,
            selection,
            selectionArgs,
            null,
            null,
            orderBy,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toPendingUserDataMutation())
                }
            }
        }
    }
}

private fun DownloadRecord.toValues(): ContentValues = ContentValues().apply {
    put("id", id)
    put("profile_id", profileId)
    put("server_id", identity.serverId)
    put("user_id", identity.userId)
    put("item_id", identity.itemId)
    put("media_source_id", identity.mediaSourceId)
    put("media_type", mediaType.name)
    put("title", title)
    putNullable("sort_title", sortTitle)
    putNullable("overview", overview)
    putNullable("year", year)
    putNullable("runtime_ticks", runtimeTicks)
    putNullable("parent_id", parentId)
    putNullable("series_id", seriesId)
    putNullable("series_name", seriesName)
    putNullable("season_id", seasonId)
    putNullable("season_name", seasonName)
    putNullable("season_number", seasonNumber)
    putNullable("episode_number", episodeNumber)
    putNullable("album_id", albumId)
    putNullable("album_name", albumName)
    putNullable("artist_name", artistName)
    putNullable("local_media_path", localMediaPath)
    putNullable("temp_media_path", tempMediaPath)
    putNullable("local_poster_path", localPosterPath)
    putNullable("local_backdrop_path", localBackdropPath)
    putNullable("local_logo_path", localLogoPath)
    putNullable("local_subtitle_path", localSubtitlePath)
    putNullable("remote_poster_url", remotePosterUrl)
    putNullable("remote_backdrop_url", remoteBackdropUrl)
    putNullable("remote_logo_url", remoteLogoUrl)
    putNullable("selected_audio_track_id", selectedAudioTrackId)
    putNullable("selected_subtitle_track_id", selectedSubtitleTrackId)
    put("state", state.name)
    put("bytes_downloaded", bytesDownloaded)
    putNullable("total_bytes", totalBytes)
    put("created_at_millis", createdAtMillis)
    put("updated_at_millis", updatedAtMillis)
    putNullable("completed_at_millis", completedAtMillis)
    putNullable("failure_category", failureCategory?.name)
    putNullable("failure_reason", failureReason)
    put("local_playback_position_ticks", localPlaybackPositionTicks)
    put("local_played", if (localPlayed) 1 else 0)
    put("sync_state", syncState.name)
}

private fun PendingUserDataMutation.toValues(): ContentValues = ContentValues().apply {
    put("profile_id", profileId)
    put("server_id", serverId)
    put("user_id", userId)
    put("item_id", itemId)
    put("playback_position_ticks", playbackPositionTicks)
    put("played", if (played) 1 else 0)
    put("updated_at_millis", updatedAtMillis)
    put("retry_count", retryCount)
    put("sync_state", syncState.name)
    putNullable("failure_reason", failureReason)
}

private fun Cursor.toDownloadRecord(): DownloadRecord {
    val identity = DownloadIdentity(
        serverId = string("server_id"),
        userId = string("user_id"),
        itemId = string("item_id"),
        mediaSourceId = string("media_source_id"),
    )
    return DownloadRecord(
        id = string("id"),
        profileId = string("profile_id"),
        identity = identity,
        mediaType = enumValueOf(string("media_type")),
        title = string("title"),
        sortTitle = nullableString("sort_title"),
        overview = nullableString("overview"),
        year = nullableInt("year"),
        runtimeTicks = nullableLong("runtime_ticks"),
        parentId = nullableString("parent_id"),
        seriesId = nullableString("series_id"),
        seriesName = nullableString("series_name"),
        seasonId = nullableString("season_id"),
        seasonName = nullableString("season_name"),
        seasonNumber = nullableInt("season_number"),
        episodeNumber = nullableInt("episode_number"),
        albumId = nullableString("album_id"),
        albumName = nullableString("album_name"),
        artistName = nullableString("artist_name"),
        localMediaPath = nullableString("local_media_path"),
        tempMediaPath = nullableString("temp_media_path"),
        localPosterPath = nullableString("local_poster_path"),
        localBackdropPath = nullableString("local_backdrop_path"),
        localLogoPath = nullableString("local_logo_path"),
        localSubtitlePath = nullableString("local_subtitle_path"),
        remotePosterUrl = nullableString("remote_poster_url"),
        remoteBackdropUrl = nullableString("remote_backdrop_url"),
        remoteLogoUrl = nullableString("remote_logo_url"),
        selectedAudioTrackId = nullableString("selected_audio_track_id"),
        selectedSubtitleTrackId = nullableString("selected_subtitle_track_id"),
        state = enumValueOf(string("state")),
        bytesDownloaded = long("bytes_downloaded"),
        totalBytes = nullableLong("total_bytes"),
        createdAtMillis = long("created_at_millis"),
        updatedAtMillis = long("updated_at_millis"),
        completedAtMillis = nullableLong("completed_at_millis"),
        failureCategory = nullableString("failure_category")?.let { enumValueOf<DownloadFailureCategory>(it) },
        failureReason = nullableString("failure_reason"),
        localPlaybackPositionTicks = long("local_playback_position_ticks"),
        localPlayed = booleanInt("local_played"),
        syncState = enumValueOf(string("sync_state")),
    )
}

private fun Cursor.toPendingUserDataMutation(): PendingUserDataMutation = PendingUserDataMutation(
    profileId = nullableString("profile_id").orEmpty(),
    serverId = string("server_id"),
    userId = string("user_id"),
    itemId = string("item_id"),
    playbackPositionTicks = long("playback_position_ticks"),
    played = booleanInt("played"),
    updatedAtMillis = long("updated_at_millis"),
    retryCount = int("retry_count"),
    syncState = enumValueOf(string("sync_state")),
    failureReason = nullableString("failure_reason"),
)

private fun ContentValues.putNullable(key: String, value: String?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun ContentValues.putNullable(key: String, value: Int?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun ContentValues.putNullable(key: String, value: Long?) {
    if (value == null) putNull(key) else put(key, value)
}

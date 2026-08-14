package dev.vantafyn.core.downloads

interface DownloadRepository {
    suspend fun upsert(record: DownloadRecord)

    suspend fun get(id: String): DownloadRecord?

    suspend fun getByIdentity(identity: DownloadIdentity): DownloadRecord?

    suspend fun listForUser(
        serverId: String,
        userId: String,
    ): List<DownloadRecord>

    suspend fun listByState(
        serverId: String,
        userId: String,
        states: Set<DownloadState>,
    ): List<DownloadRecord>

    suspend fun updateState(
        id: String,
        state: DownloadState,
        updatedAtMillis: Long,
        failureCategory: DownloadFailureCategory? = null,
        failureReason: String? = null,
    )

    suspend fun updateProgress(
        id: String,
        bytesDownloaded: Long,
        totalBytes: Long?,
        updatedAtMillis: Long,
    )

    suspend fun updateLocalPlaybackState(
        id: String,
        playbackPositionTicks: Long,
        played: Boolean,
        syncState: DownloadSyncState,
        updatedAtMillis: Long,
    )

    suspend fun updateLocalArtworkPaths(
        id: String,
        localPosterPath: String?,
        localBackdropPath: String?,
        localLogoPath: String?,
        updatedAtMillis: Long,
    )

    suspend fun delete(id: String)

    suspend fun upsertPendingUserDataMutation(mutation: PendingUserDataMutation)

    suspend fun listPendingUserDataMutations(
        serverId: String? = null,
        userId: String? = null,
    ): List<PendingUserDataMutation>

    suspend fun markPendingUserDataMutationSynced(
        serverId: String,
        userId: String,
        itemId: String,
    )

    suspend fun markPendingUserDataMutationFailed(
        serverId: String,
        userId: String,
        itemId: String,
        retryCount: Int,
        failureReason: String?,
    )

    suspend fun storageSummary(
        serverId: String,
        userId: String,
    ): DownloadStorageSummary
}

package dev.vantafyn.core.downloads

enum class DownloadMediaType {
    Movie,
    Episode,
    Season,
    MusicTrack,
    MusicAlbum,
    Audiobook,
}

enum class DownloadState {
    Queued,
    Preparing,
    WaitingForNetwork,
    WaitingForWifi,
    Downloading,
    Finalizing,
    Completed,
    Failed,
    Cancelled,
}

enum class DownloadSyncState {
    None,
    Pending,
    Syncing,
    Synced,
    Failed,
}

enum class DownloadFailureCategory {
    NetworkUnavailable,
    ServerUnavailable,
    AuthenticationRequired,
    SourceUnavailable,
    StorageFull,
    StorageUnavailable,
    DownloadInterrupted,
    FinalizationFailed,
    Unknown,
}

fun DownloadState.isTerminal(): Boolean = this == DownloadState.Completed ||
    this == DownloadState.Failed ||
    this == DownloadState.Cancelled

fun DownloadState.canTransitionTo(next: DownloadState): Boolean = when (this) {
    DownloadState.Queued -> next in setOf(
        DownloadState.Preparing,
        DownloadState.WaitingForNetwork,
        DownloadState.WaitingForWifi,
        DownloadState.Downloading,
        DownloadState.Cancelled,
        DownloadState.Failed,
    )
    DownloadState.Preparing -> next in setOf(
        DownloadState.WaitingForNetwork,
        DownloadState.WaitingForWifi,
        DownloadState.Downloading,
        DownloadState.Cancelled,
        DownloadState.Failed,
    )
    DownloadState.WaitingForNetwork,
    DownloadState.WaitingForWifi -> next in setOf(
        DownloadState.Downloading,
        DownloadState.Cancelled,
        DownloadState.Failed,
    )
    DownloadState.Downloading -> next in setOf(
        DownloadState.WaitingForNetwork,
        DownloadState.WaitingForWifi,
        DownloadState.Finalizing,
        DownloadState.Cancelled,
        DownloadState.Failed,
    )
    DownloadState.Finalizing -> next in setOf(
        DownloadState.Completed,
        DownloadState.Failed,
    )
    DownloadState.Completed,
    DownloadState.Failed,
    DownloadState.Cancelled -> false
}

data class DownloadRecord(
    val id: String,
    val profileId: String,
    val identity: DownloadIdentity,
    val mediaType: DownloadMediaType,
    val title: String,
    val sortTitle: String? = null,
    val overview: String? = null,
    val year: Int? = null,
    val runtimeTicks: Long? = null,
    val parentId: String? = null,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seasonId: String? = null,
    val seasonName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val artistName: String? = null,
    val localMediaPath: String? = null,
    val tempMediaPath: String? = null,
    val localPosterPath: String? = null,
    val localBackdropPath: String? = null,
    val localLogoPath: String? = null,
    val localSubtitlePath: String? = null,
    val remotePosterUrl: String? = null,
    val remoteBackdropUrl: String? = null,
    val remoteLogoUrl: String? = null,
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    val state: DownloadState = DownloadState.Queued,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis,
    val completedAtMillis: Long? = null,
    val failureCategory: DownloadFailureCategory? = null,
    val failureReason: String? = null,
    val localPlaybackPositionTicks: Long = 0L,
    val localPlayed: Boolean = false,
    val syncState: DownloadSyncState = DownloadSyncState.None,
)

data class PendingUserDataMutation(
    val profileId: String,
    val serverId: String,
    val userId: String,
    val itemId: String,
    val playbackPositionTicks: Long,
    val played: Boolean,
    val updatedAtMillis: Long,
    val retryCount: Int = 0,
    val syncState: DownloadSyncState = DownloadSyncState.Pending,
    val failureReason: String? = null,
)

data class DownloadStorageSummary(
    val recordCount: Int,
    val completedCount: Int,
    val activeCount: Int,
    val failedCount: Int,
    val totalBytes: Long,
)

package dev.vantafyn.core.downloads

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.vantafyn.core.jellyfin.JellyfinEpisode
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinPlaybackMethod
import dev.vantafyn.core.jellyfin.JellyfinPlaybackRepository
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import java.util.UUID

class OfflineDownloadManager(
    context: Context,
    private val repository: DownloadRepository = SqliteDownloadRepository(context),
    private val fileStore: DownloadFileStore = DownloadFileStore(context),
    private val playbackRepository: JellyfinPlaybackRepository =
        JellyfinRepositoryProvider(context.applicationContext).playbackRepository,
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    suspend fun queueMedia(
        session: JellyfinSession,
        detail: JellyfinMediaDetail,
        requireWifi: Boolean = false,
    ): JellyfinResult<DownloadRecord> {
        val mediaType = detail.downloadMediaType()
            ?: return JellyfinResult.Failure("Downloads are available for movies and episodes first.")
        val playback = when (
            val result = playbackRepository.getPlaybackInfo(
                session = session,
                itemId = detail.id,
                title = detail.title,
                subtitle = detail.subtitle,
                startPositionTicks = detail.playbackPositionTicks,
                forceTranscode = false,
            )
        ) {
            is JellyfinResult.Failure -> return JellyfinResult.Failure(result.message, result.cause)
            is JellyfinResult.Success -> result.value
        }
        if (playback.isLiveStream || playback.method == JellyfinPlaybackMethod.Transcode) {
            return JellyfinResult.Failure("This item needs a direct media source before it can be saved offline.")
        }
        val mediaSourceId = playback.mediaSourceId ?: detail.mediaSources.firstOrNull()?.id ?: "default"
        val identity = DownloadIdentity(
            serverId = session.server.localId,
            userId = session.user.id.toString(),
            itemId = detail.id.toString(),
            mediaSourceId = mediaSourceId,
        )
        val extension = detail.mediaSources.firstOrNull { it.id == mediaSourceId }?.container
            ?: playback.streamUrl.substringBefore('?').substringAfterLast('.', "bin")
        val target = fileStore.targetFor(identity, DownloadFileKind.Media, extension)
        val storage = fileStore.availabilityFor(requiredBytes = null)
        if (!storage.available) {
            return JellyfinResult.Failure("There isn't enough available storage for this download.")
        }
        if (!fileStore.ensureParentDirectory(target.tempFile)) {
            return JellyfinResult.Failure("Vantafyn couldn't prepare offline storage.")
        }

        val now = System.currentTimeMillis()
        val record = DownloadRecord(
            id = identity.stableKey,
            profileId = session.profileId,
            identity = identity,
            mediaType = mediaType,
            title = detail.title,
            sortTitle = detail.title,
            overview = detail.overview,
            year = detail.year,
            runtimeTicks = playback.runtimeTicks,
            parentId = detail.seriesId?.toString(),
            seriesId = detail.seriesId?.toString(),
            seriesName = detail.seriesName,
            seasonId = detail.seasonId?.toString(),
            seasonName = detail.seasonIndexNumber?.let { "Season $it" },
            seasonNumber = detail.seasonIndexNumber,
            episodeNumber = detail.episodeIndexNumber,
            localMediaPath = target.finalFile.absolutePath,
            tempMediaPath = target.tempFile.absolutePath,
            localPosterPath = null,
            localBackdropPath = null,
            localLogoPath = null,
            remotePosterUrl = detail.imageUrl,
            remoteBackdropUrl = detail.backdropUrl,
            remoteLogoUrl = detail.logoUrl,
            selectedAudioTrackId = playback.audioStreamIndex?.toString(),
            selectedSubtitleTrackId = playback.subtitleStreamIndex?.toString(),
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        repository.upsert(record)
        enqueue(record, requireWifi)
        return JellyfinResult.Success(record)
    }

    suspend fun queueEpisode(
        session: JellyfinSession,
        series: JellyfinMediaDetail,
        episode: JellyfinEpisode,
        requireWifi: Boolean = false,
    ): JellyfinResult<DownloadRecord> {
        val playback = when (
            val result = playbackRepository.getPlaybackInfo(
                session = session,
                itemId = episode.id,
                title = episode.title,
                subtitle = listOfNotNull(series.title, episode.subtitle).joinToString(" · ").ifBlank { null },
                startPositionTicks = episode.playbackPositionTicks,
                forceTranscode = false,
            )
        ) {
            is JellyfinResult.Failure -> return JellyfinResult.Failure(result.message, result.cause)
            is JellyfinResult.Success -> result.value
        }
        if (playback.isLiveStream || playback.method == JellyfinPlaybackMethod.Transcode) {
            return JellyfinResult.Failure("${episode.title} needs a direct media source before it can be saved offline.")
        }
        val mediaSourceId = playback.mediaSourceId ?: "default"
        val identity = DownloadIdentity(
            serverId = session.server.localId,
            userId = session.user.id.toString(),
            itemId = episode.id.toString(),
            mediaSourceId = mediaSourceId,
        )
        val extension = playback.streamUrl.substringBefore('?').substringAfterLast('.', "bin")
        val target = fileStore.targetFor(identity, DownloadFileKind.Media, extension)
        val storage = fileStore.availabilityFor(requiredBytes = null)
        if (!storage.available) {
            return JellyfinResult.Failure("There isn't enough available storage for this download.")
        }
        if (!fileStore.ensureParentDirectory(target.tempFile)) {
            return JellyfinResult.Failure("Vantafyn couldn't prepare offline storage.")
        }

        val now = System.currentTimeMillis()
        val record = DownloadRecord(
            id = identity.stableKey,
            profileId = session.profileId,
            identity = identity,
            mediaType = DownloadMediaType.Episode,
            title = episode.title,
            sortTitle = listOfNotNull(
                episode.seasonIndexNumber?.toString()?.padStart(4, '0'),
                episode.indexNumber?.toString()?.padStart(4, '0'),
                episode.title,
            ).joinToString(" "),
            overview = episode.overview,
            runtimeTicks = playback.runtimeTicks ?: episode.runtimeMinutes?.let { it * 60L * 10_000_000L },
            parentId = episode.seriesId?.toString() ?: series.id.toString(),
            seriesId = episode.seriesId?.toString() ?: series.id.toString(),
            seriesName = episode.seriesName ?: series.title,
            seasonId = episode.seasonId?.toString(),
            seasonName = episode.seasonIndexNumber?.let { "Season $it" },
            seasonNumber = episode.seasonIndexNumber,
            episodeNumber = episode.indexNumber,
            localMediaPath = target.finalFile.absolutePath,
            tempMediaPath = target.tempFile.absolutePath,
            remotePosterUrl = episode.imageUrl ?: series.imageUrl,
            remoteBackdropUrl = series.backdropUrl,
            remoteLogoUrl = series.logoUrl,
            selectedAudioTrackId = playback.audioStreamIndex?.toString(),
            selectedSubtitleTrackId = playback.subtitleStreamIndex?.toString(),
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        repository.upsert(record)
        enqueue(record, requireWifi)
        return JellyfinResult.Success(record)
    }

    suspend fun cancel(recordId: String) {
        workManager.cancelUniqueWork(workName(recordId))
        repository.updateState(recordId, DownloadState.Cancelled, System.currentTimeMillis())
    }

    suspend fun retry(record: DownloadRecord, requireWifi: Boolean = false) {
        repository.updateState(record.id, DownloadState.Queued, System.currentTimeMillis())
        enqueue(record, requireWifi)
    }

    suspend fun remove(record: DownloadRecord) {
        workManager.cancelUniqueWork(workName(record.id))
        fileStore.deleteFilesFor(record)
        repository.delete(record.id)
    }

    private fun enqueue(
        record: DownloadRecord,
        requireWifi: Boolean,
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
            .setInputData(Data.Builder().putString(OfflineDownloadWorker.KEY_RECORD_ID, record.id).build())
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(workName(record.id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun workName(recordId: String): String = "offline-download-$recordId"
}

private fun JellyfinMediaDetail.downloadMediaType(): DownloadMediaType? =
    when (itemType?.lowercase()) {
        "movie" -> DownloadMediaType.Movie
        "episode" -> DownloadMediaType.Episode
        else -> null
    }

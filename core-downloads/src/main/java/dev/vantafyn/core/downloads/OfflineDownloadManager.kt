package dev.vantafyn.core.downloads

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.vantafyn.core.jellyfin.JellyfinEpisode
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinMusicAlbum
import dev.vantafyn.core.jellyfin.JellyfinMusicPlaylist
import dev.vantafyn.core.jellyfin.JellyfinMusicTrack
import dev.vantafyn.core.jellyfin.JellyfinPlaybackMethod
import dev.vantafyn.core.jellyfin.JellyfinPlaybackRepository
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
            ?: return JellyfinResult.Failure("This item cannot be saved offline yet.")
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
        existingReusableRecord(identity)?.let { return JellyfinResult.Success(it) }
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

    suspend fun queueMusicTrack(
        session: JellyfinSession,
        track: JellyfinMusicTrack,
        requireWifi: Boolean = false,
        playlistId: UUID? = null,
        playlistName: String? = null,
    ): JellyfinResult<DownloadRecord> {
        val playback = when (
            val result = playbackRepository.getPlaybackInfo(
                session = session,
                itemId = track.id,
                title = track.title,
                subtitle = track.artist,
                startPositionTicks = 0L,
                forceTranscode = false,
            )
        ) {
            is JellyfinResult.Failure -> return JellyfinResult.Failure(result.message, result.cause)
            is JellyfinResult.Success -> result.value
        }
        if (playback.isLiveStream || playback.method == JellyfinPlaybackMethod.Transcode) {
            return JellyfinResult.Failure("${track.title} needs a direct audio source before it can be saved offline.")
        }
        return queueAudioRecord(
            session = session,
            itemId = track.id,
            mediaSourceId = playback.mediaSourceId ?: "default",
            mediaType = DownloadMediaType.MusicTrack,
            title = track.title,
            sortTitle = listOfNotNull(track.album, track.title).joinToString(" ").ifBlank { track.title },
            runtimeTicks = playback.runtimeTicks ?: track.durationMs?.let { it * 10_000L },
            parentId = playlistId?.toString(),
            albumId = track.albumId?.toString(),
            albumName = playlistName ?: track.album,
            artistName = track.artist,
            remotePosterUrl = track.artworkUrl,
            playback = playback,
            requireWifi = requireWifi,
        )
    }

    suspend fun queueMusicPlaylist(
        session: JellyfinSession,
        playlist: JellyfinMusicPlaylist,
        tracks: List<JellyfinMusicTrack>,
        requireWifi: Boolean = false,
    ): JellyfinResult<Int> {
        if (tracks.isEmpty()) return JellyfinResult.Failure("This playlist does not have any tracks to save.")
        var queued = 0
        var firstFailure: JellyfinResult.Failure? = null
        tracks.forEachIndexed { index, track ->
            val enriched = track.copy(
                album = playlist.name,
                artworkUrl = track.artworkUrl ?: playlist.imageUrl,
            )
            when (
                val result = queueMusicTrack(
                    session = session,
                    track = enriched,
                    requireWifi = requireWifi,
                    playlistId = playlist.id,
                    playlistName = playlist.name,
                )
            ) {
                is JellyfinResult.Success -> {
                    val ordered = result.value.copy(sortTitle = index.toString().padStart(5, '0') + " ${track.title}")
                    repository.upsert(ordered)
                    queued += 1
                }
                is JellyfinResult.Failure -> if (firstFailure == null) firstFailure = result
            }
        }
        return when {
            queued > 0 -> JellyfinResult.Success(queued)
            firstFailure != null -> firstFailure
            else -> JellyfinResult.Failure("This playlist could not be saved offline.")
        }
    }

    suspend fun queueMusicAlbum(
        session: JellyfinSession,
        album: JellyfinMusicAlbum,
        tracks: List<JellyfinMusicTrack>,
        requireWifi: Boolean = false,
    ): JellyfinResult<Int> {
        if (tracks.isEmpty()) return JellyfinResult.Failure("This album does not have any tracks to save.")
        var queued = 0
        var firstFailure: JellyfinResult.Failure? = null
        tracks.forEach { track ->
            val enriched = track.copy(
                album = track.album ?: album.title,
                albumId = track.albumId ?: album.id,
                artworkUrl = track.artworkUrl ?: album.artworkUrl,
            )
            when (val result = queueMusicTrack(session, enriched, requireWifi)) {
                is JellyfinResult.Success -> queued += 1
                is JellyfinResult.Failure -> if (firstFailure == null) firstFailure = result
            }
        }
        return when {
            queued > 0 -> JellyfinResult.Success(queued)
            firstFailure != null -> firstFailure
            else -> JellyfinResult.Failure("This album could not be saved offline.")
        }
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
        existingReusableRecord(identity)?.let { return JellyfinResult.Success(it) }
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
        fileStore.deleteTempFileFor(record)
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
            .addTag(recordDownloadTag(record.id))
            .apply {
                record.parentId?.takeIf { it.isNotBlank() }?.let { addTag(parentDownloadTag(it)) }
                record.albumId?.takeIf { it.isNotBlank() }?.let { addTag(albumDownloadTag(it)) }
            }
            .build()
        workManager.enqueueUniqueWork(workName(record.id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun workName(recordId: String): String = "offline-download-$recordId"

    private suspend fun queueAudioRecord(
        session: JellyfinSession,
        itemId: UUID,
        mediaSourceId: String,
        mediaType: DownloadMediaType,
        title: String,
        sortTitle: String?,
        runtimeTicks: Long?,
        parentId: String? = null,
        albumId: String?,
        albumName: String?,
        artistName: String?,
        remotePosterUrl: String?,
        playback: dev.vantafyn.core.jellyfin.JellyfinPlaybackInfo,
        requireWifi: Boolean,
    ): JellyfinResult<DownloadRecord> {
        val identity = DownloadIdentity(
            serverId = session.server.localId,
            userId = session.user.id.toString(),
            itemId = itemId.toString(),
            mediaSourceId = mediaSourceId,
        )
        existingReusableRecord(identity)?.let { existing ->
            val updated = existing.copy(
                parentId = parentId ?: existing.parentId,
                albumName = albumName ?: existing.albumName,
                sortTitle = sortTitle ?: existing.sortTitle,
                remotePosterUrl = remotePosterUrl ?: existing.remotePosterUrl,
                updatedAtMillis = System.currentTimeMillis(),
            )
            if (updated != existing) repository.upsert(updated)
            return JellyfinResult.Success(updated)
        }
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
            mediaType = mediaType,
            title = title,
            sortTitle = sortTitle ?: title,
            runtimeTicks = runtimeTicks,
            parentId = parentId,
            albumId = albumId,
            albumName = albumName,
            artistName = artistName,
            localMediaPath = target.finalFile.absolutePath,
            tempMediaPath = target.tempFile.absolutePath,
            remotePosterUrl = remotePosterUrl,
            selectedAudioTrackId = playback.audioStreamIndex?.toString(),
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        repository.upsert(record)
        enqueue(record, requireWifi)
        return JellyfinResult.Success(record)
    }

    private suspend fun existingReusableRecord(identity: DownloadIdentity): DownloadRecord? {
        val existing = repository.getByIdentity(identity) ?: return null
        if (existing.state == DownloadState.Completed && fileStore.hasCompletedMediaFile(existing)) {
            return existing
        }
        if (existing.state in setOf(
                DownloadState.Queued,
                DownloadState.Preparing,
                DownloadState.WaitingForNetwork,
                DownloadState.WaitingForWifi,
                DownloadState.Downloading,
                DownloadState.Finalizing,
            )
        ) {
            return existing
        }
        fileStore.deleteTempFileFor(existing)
        return null
    }

    suspend fun isPlaylistFullyDownloaded(
        session: JellyfinSession,
        playlistId: UUID,
        expectedTrackCount: Int,
    ): Boolean {
        if (expectedTrackCount <= 0) return false
        val all = repository.listForUser(
            serverId = session.server.localId,
            userId = session.user.id.toString(),
        )
        val completedCount = all.count { record ->
            record.parentId == playlistId.toString() && record.state == DownloadState.Completed
        }
        return completedCount >= expectedTrackCount
    }

    suspend fun isAlbumFullyDownloaded(
        session: JellyfinSession,
        albumId: UUID,
        expectedTrackCount: Int,
    ): Boolean {
        if (expectedTrackCount <= 0) return false
        val all = repository.listForUser(
            serverId = session.server.localId,
            userId = session.user.id.toString(),
        )
        val completedCount = all.count { record ->
            record.albumId == albumId.toString() && record.state == DownloadState.Completed
        }
        return completedCount >= expectedTrackCount
    }

    fun observePlaylistFullyDownloaded(
        session: JellyfinSession,
        playlistId: UUID,
        expectedTrackCount: Int,
    ): Flow<Boolean> =
        workManager.workInfosByTagFlow(parentDownloadTag(playlistId.toString()))
            .map { isPlaylistFullyDownloaded(session, playlistId, expectedTrackCount) }
            .distinctUntilChanged()

    fun observeAlbumFullyDownloaded(
        session: JellyfinSession,
        albumId: UUID,
        expectedTrackCount: Int,
    ): Flow<Boolean> =
        workManager.workInfosByTagFlow(albumDownloadTag(albumId.toString()))
            .map { isAlbumFullyDownloaded(session, albumId, expectedTrackCount) }
            .distinctUntilChanged()
}

private fun WorkManager.workInfosByTagFlow(tag: String): Flow<List<WorkInfo>> = callbackFlow {
    val liveData = getWorkInfosByTagLiveData(tag)
    val observer = Observer<List<WorkInfo>> { workInfos ->
        trySend(workInfos.orEmpty())
    }
    liveData.observeForever(observer)
    awaitClose { liveData.removeObserver(observer) }
}

private fun recordDownloadTag(recordId: String): String = "vantafyn-download-record-$recordId"

private fun parentDownloadTag(parentId: String): String = "vantafyn-download-parent-$parentId"

private fun albumDownloadTag(albumId: String): String = "vantafyn-download-album-$albumId"

private fun JellyfinMediaDetail.downloadMediaType(): DownloadMediaType? =
    when (itemType?.lowercase()) {
        "movie" -> DownloadMediaType.Movie
        "episode" -> DownloadMediaType.Episode
        "audio" -> DownloadMediaType.MusicTrack
        "book",
        "audiobook" -> DownloadMediaType.Audiobook
        else -> null
    }

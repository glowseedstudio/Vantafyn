package dev.vantafyn.core.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.JellyfinPlaybackInfo
import dev.vantafyn.core.jellyfin.JellyfinSubtitleTrack
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class OfflineDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val repository = SqliteDownloadRepository(appContext)
    private val fileStore = DownloadFileStore(appContext)
    private val jellyfin = JellyfinRepositoryProvider(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val recordId = inputData.getString(KEY_RECORD_ID) ?: return@withContext Result.failure()
        val record = repository.get(recordId) ?: return@withContext Result.failure()
        try {
            setForeground(createForegroundInfo(record, "Preparing"))
            repository.updateState(record.id, DownloadState.Preparing, System.currentTimeMillis())
            val session = when (val restored = jellyfin.authRepository.restoreSession(record.profileId)) {
                is JellyfinResult.Failure -> {
                    repository.updateState(
                        record.id,
                        DownloadState.Failed,
                        System.currentTimeMillis(),
                        DownloadFailureCategory.AuthenticationRequired,
                        "Saved Jellyfin profile could not be restored.",
                    )
                    return@withContext Result.failure()
                }
                is JellyfinResult.Success -> restored.value
            }
            val playback = when (
                val info = jellyfin.playbackRepository.getPlaybackInfo(
                    session = session,
                    itemId = UUID.fromString(record.identity.itemId),
                    title = record.title,
                    subtitle = record.seriesName,
                    startPositionTicks = 0L,
                    forceTranscode = false,
                )
            ) {
                is JellyfinResult.Failure -> {
                    repository.updateState(
                        record.id,
                        DownloadState.Failed,
                        System.currentTimeMillis(),
                        DownloadFailureCategory.SourceUnavailable,
                        info.message,
                    )
                    return@withContext Result.retry()
                }
                is JellyfinResult.Success -> info.value
            }
            val temp = record.tempMediaPath?.let(::File)
            val final = record.localMediaPath?.let(::File)
            if (temp == null || final == null || temp.parentFile?.let { it.exists() || it.mkdirs() } != true) {
                repository.updateState(
                    record.id,
                    DownloadState.Failed,
                    System.currentTimeMillis(),
                    DownloadFailureCategory.StorageUnavailable,
                    "Offline storage is unavailable.",
                )
                return@withContext Result.failure()
            }
            repository.updateState(record.id, DownloadState.Downloading, System.currentTimeMillis())
            downloadToTemp(record, playback.streamUrl, temp)
            repository.updateState(record.id, DownloadState.Finalizing, System.currentTimeMillis())
            if (final.exists() && !final.delete()) {
                throw IllegalStateException("Could not replace existing offline file.")
            }
            if (!temp.renameTo(final)) {
                throw IllegalStateException("Could not finish offline file.")
            }
            if (!final.exists() || final.length() <= 0L) {
                if (final.exists()) final.delete()
                throw IllegalStateException("Downloaded file was empty.")
            }
            repository.updateProgress(record.id, final.length(), final.length(), System.currentTimeMillis())
            downloadArtwork(record)
            writeOfflineManifest(record, session, playback)
            repository.updateState(record.id, DownloadState.Completed, System.currentTimeMillis())
            Result.success()
        } catch (cancelled: CancellationException) {
            fileStore.deleteTempFileFor(record)
            throw cancelled
        } catch (throwable: Throwable) {
            fileStore.deleteTempFileFor(record)
            val category = throwable.toFailureCategory()
            repository.updateState(
                record.id,
                DownloadState.Failed,
                System.currentTimeMillis(),
                category,
                throwable.userSafeMessage(),
            )
            category.toWorkResult()
        }
    }

    private suspend fun downloadArtwork(record: DownloadRecord) {
        val poster = downloadOptionalAsset(
            record = record,
            url = record.remotePosterUrl,
            kind = DownloadFileKind.Poster,
            extension = "webp",
        )
        val backdrop = downloadOptionalAsset(
            record = record,
            url = record.remoteBackdropUrl,
            kind = DownloadFileKind.Backdrop,
            extension = "webp",
        )
        val logo = downloadOptionalAsset(
            record = record,
            url = record.remoteLogoUrl,
            kind = DownloadFileKind.Logo,
            extension = "webp",
        )
        if (poster != null || backdrop != null || logo != null) {
            repository.updateLocalArtworkPaths(
                id = record.id,
                localPosterPath = poster ?: record.localPosterPath,
                localBackdropPath = backdrop ?: record.localBackdropPath,
                localLogoPath = logo ?: record.localLogoPath,
                updatedAtMillis = System.currentTimeMillis(),
            )
        }
    }

    private suspend fun writeOfflineManifest(
        record: DownloadRecord,
        session: JellyfinSession,
        playback: JellyfinPlaybackInfo,
    ) {
        val subtitles = playback.subtitleTracks.map { track ->
            val localPath = track.downloadExternalSubtitle(record)
            DownloadOfflineSubtitle(
                index = track.index,
                label = track.label,
                language = track.language,
                codec = track.codec,
                localPath = localPath,
                isDefault = track.isDefault,
            )
        }
        val segments = when (val result = jellyfin.mediaSegmentRepository.getItemSegments(session, UUID.fromString(record.identity.itemId))) {
            is JellyfinResult.Success -> result.value.map { segment ->
                DownloadOfflineSegment(
                    id = segment.id.toString(),
                    type = segment.type.name,
                    startMs = segment.startMs,
                    endMs = segment.endMs,
                )
            }
            is JellyfinResult.Failure -> emptyList()
        }
        val lyrics = if (record.mediaType == DownloadMediaType.MusicTrack || record.mediaType == DownloadMediaType.Audiobook) {
            when (val result = jellyfin.musicRepository.getLyrics(session, UUID.fromString(record.identity.itemId))) {
                is JellyfinResult.Success -> result.value?.let { lyric ->
                    DownloadOfflineLyrics(
                        plainText = lyric.plainText,
                        syncedLines = lyric.syncedLines.map { line ->
                            DownloadOfflineLyricLine(startMs = line.startMs, text = line.text)
                        },
                    )
                }
                is JellyfinResult.Failure -> null
            }
        } else {
            null
        }
        val manifest = DownloadOfflineManifest(
            itemId = record.identity.itemId,
            title = record.title,
            generatedAtMillis = System.currentTimeMillis(),
            subtitles = subtitles,
            segments = segments,
            lyrics = lyrics,
            chaptersAvailable = false,
            trickplayAvailable = false,
        )
        val metadataTarget = fileStore.targetFor(record.identity, DownloadFileKind.Metadata, "json")
        if (metadataTarget.tempFile.parentFile?.let { it.exists() || it.mkdirs() } != true) return
        metadataTarget.tempFile.writeText(manifest.toJsonString())
        if (metadataTarget.finalFile.exists() && !metadataTarget.finalFile.delete()) return
        if (!metadataTarget.tempFile.renameTo(metadataTarget.finalFile)) return

        val lyricsPath = lyrics?.let {
            val lyricsTarget = fileStore.targetFor(record.identity, DownloadFileKind.Lyrics, "json")
            if (lyricsTarget.tempFile.parentFile?.let { parent -> parent.exists() || parent.mkdirs() } == true) {
                lyricsTarget.tempFile.writeText(it.toStandaloneJson())
                if (lyricsTarget.finalFile.exists()) lyricsTarget.finalFile.delete()
                if (lyricsTarget.tempFile.renameTo(lyricsTarget.finalFile)) lyricsTarget.finalFile.absolutePath else null
            } else {
                null
            }
        }
        repository.updateLocalSidecarPaths(
            id = record.id,
            localSubtitlePath = subtitles.firstOrNull { !it.localPath.isNullOrBlank() }?.localPath ?: record.localSubtitlePath,
            localMetadataPath = metadataTarget.finalFile.absolutePath,
            localLyricsPath = lyricsPath ?: record.localLyricsPath,
            localChaptersPath = record.localChaptersPath,
            localTrickplayPath = record.localTrickplayPath,
            offlineFeatureFlags = buildList {
                if (subtitles.any { !it.localPath.isNullOrBlank() }) add("subtitles")
                if (segments.isNotEmpty()) add("segments")
                if (lyrics != null) add("lyrics")
            }.joinToString(",").ifBlank { null },
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    private suspend fun JellyfinSubtitleTrack.downloadExternalSubtitle(record: DownloadRecord): String? {
        if (!isExternal || deliveryUrl.isNullOrBlank()) return null
        val extension = when (codec?.lowercase()) {
            "subrip", "srt" -> "srt"
            "webvtt", "vtt" -> "vtt"
            "ass", "ssa" -> codec.orEmpty().lowercase()
            "ttml", "dfxp" -> "ttml"
            else -> "sub"
        }
        return downloadOptionalAsset(
            record = record,
            url = deliveryUrl,
            kind = DownloadFileKind.Subtitle,
            extension = extension,
            suffix = "subtitle_$index",
        )
    }

    private suspend fun downloadOptionalAsset(
        record: DownloadRecord,
        url: String?,
        kind: DownloadFileKind,
        extension: String,
        suffix: String? = null,
    ): String? {
        if (url.isNullOrBlank()) return null
        val target = DownloadFileStore(applicationContext).targetFor(record.identity, kind, extension, suffix)
        if (target.tempFile.parentFile?.let { it.exists() || it.mkdirs() } != true) return null
        return runCatching {
            downloadUrlToFile(url, target.tempFile)
            if (target.finalFile.exists() && !target.finalFile.delete()) return@runCatching null
            if (!target.tempFile.renameTo(target.finalFile)) return@runCatching null
            target.finalFile.absolutePath
        }.getOrNull()
    }

    private suspend fun downloadToTemp(
        record: DownloadRecord,
        streamUrl: String,
        tempFile: File,
    ) {
        downloadUrlToFile(streamUrl, tempFile) { downloaded, totalBytes ->
            repository.updateProgress(record.id, downloaded, totalBytes, System.currentTimeMillis())
            setForeground(createForegroundInfo(record, downloaded.percentOf(totalBytes)))
        }
    }

    private suspend fun downloadUrlToFile(
        streamUrl: String,
        tempFile: File,
        onProgress: (suspend (downloaded: Long, totalBytes: Long?) -> Unit)? = null,
    ) {
        val connection = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Server returned HTTP $code.")
            }
            val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
            ensureEnoughStorage(tempFile, totalBytes)
            var downloaded = 0L
            var lastProgressAt = 0L
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (onProgress != null && downloaded - lastProgressAt >= PROGRESS_GRANULARITY_BYTES) {
                            lastProgressAt = downloaded
                            onProgress(downloaded, totalBytes)
                        }
                    }
                }
            }
            onProgress?.invoke(downloaded, totalBytes)
        } finally {
            connection.disconnect()
        }
    }

    private fun ensureEnoughStorage(tempFile: File, totalBytes: Long?) {
        val required = totalBytes ?: return
        val parent = tempFile.parentFile ?: throw StorageUnavailableException("Offline storage is unavailable.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw StorageUnavailableException("Offline storage is unavailable.")
        }
        val freeBytes = StatFs(parent.absolutePath).availableBytes
        if (freeBytes <= required) {
            throw StorageFullException("There isn't enough available storage for this download.")
        }
    }

    private fun createForegroundInfo(
        record: DownloadRecord,
        progressLabel: String,
    ): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Saving for offline")
            .setContentText("${record.title} - $progressLabel")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                record.id.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(record.id.hashCode(), notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Offline downloads",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun Long.percentOf(total: Long?): String =
        total?.takeIf { it > 0L }
            ?.let { "${((toDouble() / it.toDouble()) * 100).roundToInt().coerceIn(0, 100)}%" }
            ?: "${this / (1024L * 1024L)} MB"

    private fun DownloadOfflineLyrics.toStandaloneJson(): String =
        DownloadOfflineManifest(
            itemId = "",
            title = "",
            generatedAtMillis = System.currentTimeMillis(),
            lyrics = this,
        ).toJsonString()

    private fun Throwable.toFailureCategory(): DownloadFailureCategory =
        when (this) {
            is java.net.UnknownHostException,
            is java.net.SocketTimeoutException -> DownloadFailureCategory.NetworkUnavailable
            is java.io.IOException -> DownloadFailureCategory.DownloadInterrupted
            is StorageFullException -> DownloadFailureCategory.StorageFull
            is StorageUnavailableException -> DownloadFailureCategory.StorageUnavailable
            is IllegalStateException -> DownloadFailureCategory.FinalizationFailed
            else -> DownloadFailureCategory.Unknown
        }

    private fun DownloadFailureCategory.toWorkResult(): Result =
        when (this) {
            DownloadFailureCategory.NetworkUnavailable,
            DownloadFailureCategory.ServerUnavailable,
            DownloadFailureCategory.DownloadInterrupted,
            DownloadFailureCategory.Unknown -> Result.retry()
            DownloadFailureCategory.AuthenticationRequired,
            DownloadFailureCategory.SourceUnavailable,
            DownloadFailureCategory.StorageFull,
            DownloadFailureCategory.StorageUnavailable,
            DownloadFailureCategory.FinalizationFailed -> Result.failure()
        }

    private fun Throwable.userSafeMessage(): String =
        message?.takeIf { it.isNotBlank() }?.take(160) ?: "The download could not finish."

    companion object {
        const val KEY_RECORD_ID = "record_id"
        private const val CHANNEL_ID = "vantafyn_offline_downloads"
        private const val PROGRESS_GRANULARITY_BYTES = 512L * 1024L
    }
}

private class StorageFullException(message: String) : IllegalStateException(message)

private class StorageUnavailableException(message: String) : IllegalStateException(message)

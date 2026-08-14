package dev.vantafyn.core.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class OfflineDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val repository = SqliteDownloadRepository(appContext)
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
            repository.updateProgress(record.id, final.length(), final.length(), System.currentTimeMillis())
            downloadArtwork(record)
            repository.updateState(record.id, DownloadState.Completed, System.currentTimeMillis())
            Result.success()
        } catch (throwable: Throwable) {
            repository.updateState(
                record.id,
                DownloadState.Failed,
                System.currentTimeMillis(),
                throwable.toFailureCategory(),
                throwable.userSafeMessage(),
            )
            Result.retry()
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

    private suspend fun downloadOptionalAsset(
        record: DownloadRecord,
        url: String?,
        kind: DownloadFileKind,
        extension: String,
    ): String? {
        if (url.isNullOrBlank()) return null
        val target = DownloadFileStore(applicationContext).targetFor(record.identity, kind, extension)
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

    private fun Throwable.toFailureCategory(): DownloadFailureCategory =
        when (this) {
            is java.net.UnknownHostException,
            is java.net.SocketTimeoutException -> DownloadFailureCategory.NetworkUnavailable
            is java.io.IOException -> DownloadFailureCategory.DownloadInterrupted
            is IllegalStateException -> DownloadFailureCategory.FinalizationFailed
            else -> DownloadFailureCategory.Unknown
        }

    private fun Throwable.userSafeMessage(): String =
        message?.takeIf { it.isNotBlank() }?.take(160) ?: "The download could not finish."

    companion object {
        const val KEY_RECORD_ID = "record_id"
        private const val CHANNEL_ID = "vantafyn_offline_downloads"
        private const val PROGRESS_GRANULARITY_BYTES = 512L * 1024L
    }
}

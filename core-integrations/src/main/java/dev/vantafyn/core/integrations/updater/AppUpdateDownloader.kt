package dev.vantafyn.core.integrations.updater

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

class AppUpdateDownloader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        private const val TAG = "AppUpdateDownloader"
        private const val BUFFER_SIZE = 16 * 1024
        private const val MAX_REDIRECTS = 5
    }

    suspend fun downloadApk(
        releaseInfo: AppReleaseInfo,
        target: AppTarget,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<File> = withContext(ioDispatcher) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val finalApk = File(updatesDir, target.assetPattern)
        val tempApk = File(updatesDir, "${target.assetPattern}.tmp")

        if (tempApk.exists()) tempApk.delete()
        if (finalApk.exists()) finalApk.delete()

        var connection: HttpURLConnection? = null
        try {
            var currentUrl = releaseInfo.apkDownloadUrl
            var redirectCount = 0

            while (redirectCount < MAX_REDIRECTS) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Vantafyn-Updater/${releaseInfo.versionName}")
                    setRequestProperty("Accept", "application/octet-stream")
                }

                val responseCode = connection.responseCode
                if (responseCode in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER, 307, 308)) {
                    val newLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = newLocation
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val responseCode = connection?.responseCode ?: -1
            if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("Failed to download update: HTTP $responseCode")
                )
            }

            val totalBytes = connection!!.contentLengthLong.let { len ->
                if (len > 0L) len else releaseInfo.apkSizeBytes
            }

            var bytesDownloaded = 0L
            var lastProgressEmitTime = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempApk).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        if (!coroutineContext.isActive) {
                            tempApk.delete()
                            throw CancellationException("Download cancelled by user")
                        }

                        output.write(buffer, 0, read)
                        bytesDownloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastProgressEmitTime >= 100 || (totalBytes > 0 && bytesDownloaded == totalBytes)) {
                            lastProgressEmitTime = now
                            val percent = if (totalBytes > 0) {
                                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            onProgress(
                                DownloadProgress(
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = totalBytes,
                                    percent = percent,
                                    isCompleted = false,
                                )
                            )
                        }
                    }
                    output.flush()
                }
            }

            if (!tempApk.exists() || tempApk.length() == 0L) {
                return@withContext Result.failure(IllegalStateException("Downloaded APK file is empty"))
            }

            if (!tempApk.renameTo(finalApk)) {
                tempApk.copyTo(finalApk, overwrite = true)
                tempApk.delete()
            }

            onProgress(
                DownloadProgress(
                    bytesDownloaded = finalApk.length(),
                    totalBytes = totalBytes,
                    percent = 1f,
                    isCompleted = true,
                )
            )

            Log.i(TAG, "Successfully downloaded update APK to ${finalApk.absolutePath} (${finalApk.length()} bytes)")
            Result.success(finalApk)
        } catch (e: CancellationException) {
            tempApk.delete()
            throw e
        } catch (e: Exception) {
            tempApk.delete()
            Log.e(TAG, "Download failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}

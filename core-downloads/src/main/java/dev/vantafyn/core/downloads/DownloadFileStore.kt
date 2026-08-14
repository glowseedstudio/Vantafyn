package dev.vantafyn.core.downloads

import android.content.Context
import android.os.StatFs
import java.io.File

enum class DownloadFileKind(
    val directoryName: String,
) {
    Media("media"),
    Poster("posters"),
    Backdrop("backdrops"),
    Logo("logos"),
    Subtitle("subtitles"),
}

data class DownloadFileTarget(
    val finalFile: File,
    val tempFile: File,
)

data class DownloadStorageAvailability(
    val available: Boolean,
    val freeBytes: Long,
    val reason: DownloadFailureCategory? = null,
)

class DownloadFileStore(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun rootFor(
        serverId: String,
        userId: String,
    ): File = File(appContext.filesDir, "offline/${serverId.safePathSegment()}/${userId.safePathSegment()}")

    fun targetFor(
        identity: DownloadIdentity,
        kind: DownloadFileKind,
        extension: String,
    ): DownloadFileTarget {
        val directory = File(rootFor(identity.serverId, identity.userId), kind.directoryName)
        val cleanExtension = extension.trim().trimStart('.').ifBlank { "bin" }.safePathSegment()
        val baseName = "${identity.itemId.safePathSegment()}_${identity.mediaSourceId.safePathSegment()}.$cleanExtension"
        return DownloadFileTarget(
            finalFile = File(directory, baseName),
            tempFile = File(directory, "$baseName.download"),
        )
    }

    fun ensureParentDirectory(file: File): Boolean = file.parentFile?.let { parent ->
        parent.exists() || parent.mkdirs()
    } == true

    fun deleteFilesFor(record: DownloadRecord) {
        listOfNotNull(
            record.localMediaPath,
            record.tempMediaPath,
            record.localPosterPath,
            record.localBackdropPath,
            record.localLogoPath,
            record.localSubtitlePath,
        ).forEach { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
    }

    fun availabilityFor(requiredBytes: Long? = null): DownloadStorageAvailability {
        val filesDir = appContext.filesDir ?: return DownloadStorageAvailability(
            available = false,
            freeBytes = 0L,
            reason = DownloadFailureCategory.StorageUnavailable,
        )
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            return DownloadStorageAvailability(
                available = false,
                freeBytes = 0L,
                reason = DownloadFailureCategory.StorageUnavailable,
            )
        }
        val freeBytes = StatFs(filesDir.absolutePath).availableBytes
        val required = requiredBytes ?: 0L
        return DownloadStorageAvailability(
            available = freeBytes > required,
            freeBytes = freeBytes,
            reason = if (freeBytes > required) null else DownloadFailureCategory.StorageFull,
        )
    }
}

private fun String.safePathSegment(): String = trim()
    .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
    .ifBlank { "unknown" }

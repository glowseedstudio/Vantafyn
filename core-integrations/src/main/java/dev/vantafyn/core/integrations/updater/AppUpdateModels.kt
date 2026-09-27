package dev.vantafyn.core.integrations.updater

enum class AppTarget(val assetPattern: String) {
    MOBILE("app-mobile-release.apk"),
    TV("app-tv-release.apk");

    fun matchesAsset(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return when (this) {
            MOBILE -> lower == "app-mobile-release.apk" || (lower.contains("mobile") && lower.endsWith(".apk"))
            TV -> lower == "app-tv-release.apk" || (lower.contains("tv") && lower.endsWith(".apk"))
        }
    }
}

data class AppReleaseInfo(
    val versionName: String,
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val publishedAt: String,
)

sealed interface UpdateCheckResult {
    data class Available(val releaseInfo: AppReleaseInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String, val throwable: Throwable? = null) : UpdateCheckResult
}

data class DownloadProgress(
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Float = 0f,
    val isCompleted: Boolean = false,
    val error: String? = null,
)

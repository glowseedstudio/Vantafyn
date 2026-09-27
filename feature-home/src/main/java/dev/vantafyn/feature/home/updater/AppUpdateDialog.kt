package dev.vantafyn.feature.home.updater

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.integrations.updater.AppReleaseInfo
import dev.vantafyn.core.integrations.updater.AppTarget
import dev.vantafyn.core.integrations.updater.AppUpdateChecker
import dev.vantafyn.core.integrations.updater.AppUpdateDownloader
import dev.vantafyn.core.integrations.updater.AppUpdateInstaller
import dev.vantafyn.core.integrations.updater.AppUpdatePreferences
import dev.vantafyn.core.integrations.updater.DownloadProgress
import dev.vantafyn.core.integrations.updater.UpdateCheckResult
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun AppUpdateDialog(
    currentVersion: String,
    target: AppTarget = AppTarget.MOBILE,
    initialCheckOnOpen: Boolean = true,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { AppUpdatePreferences(context) }
    val checker = remember { AppUpdateChecker() }
    val downloader = remember { AppUpdateDownloader(context) }

    var checkState by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var isChecking by remember { mutableStateOf(initialCheckOnOpen) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var hasInstallPermission by remember { mutableStateOf(AppUpdateInstaller.canInstallPackages(context)) }

    fun startCheck() {
        isChecking = true
        checkState = null
        downloadProgress = null
        downloadedApkFile = null
        coroutineScope.launch {
            val result = checker.checkForUpdate(currentVersion, target)
            preferences.recordCheckPerformed()
            checkState = result
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        if (initialCheckOnOpen) {
            startCheck()
        }
    }

    AlertDialog(
        modifier = Modifier
            .imePadding()
            .vantafynAnimatedModalBorder(),
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
        containerColor = Color(0xFF131317),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    tint = VantafynColors.Primary,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = when {
                        isChecking -> "Checking for Updates"
                        checkState is UpdateCheckResult.Available -> "Update Available"
                        checkState is UpdateCheckResult.UpToDate -> "You're Up to Date"
                        checkState is UpdateCheckResult.Error -> "Check Failed"
                        else -> "Software Update"
                    },
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when {
                    isChecking -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = VantafynColors.Primary,
                                strokeWidth = 3.dp,
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Checking GitHub for releases...",
                                color = VantafynColors.Muted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    checkState is UpdateCheckResult.UpToDate -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                "Vantafyn is up to date!",
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "You are currently running version $currentVersion. No newer release is available.",
                                color = VantafynColors.Muted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    checkState is UpdateCheckResult.Error -> {
                        val error = checkState as UpdateCheckResult.Error
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    "Could not complete check",
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                error.message,
                                color = VantafynColors.Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    checkState is UpdateCheckResult.Available -> {
                        val available = checkState as UpdateCheckResult.Available
                        val release = available.releaseInfo

                        // Version badge & size row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VantafynGradients.accentHorizontal())
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    "v${release.versionName}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                            if (release.apkSizeBytes > 0L) {
                                Text(
                                    formatFileSize(release.apkSizeBytes),
                                    color = VantafynColors.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        // Release title
                        if (release.releaseTitle.isNotBlank()) {
                            Text(
                                release.releaseTitle,
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Release Notes / Changelog
                        if (release.releaseNotes.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0C0C0F))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    text = release.releaseNotes.trim(),
                                    color = VantafynColors.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 18.sp,
                                )
                            }
                        }

                        // Downloading progress state
                        if (isDownloading) {
                            val progress = downloadProgress
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress?.percent ?: 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = VantafynColors.Primary,
                                    trackColor = Color.White.copy(alpha = 0.12f),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    val percentInt = ((progress?.percent ?: 0f) * 100).toInt()
                                    Text(
                                        "$percentInt%",
                                        color = VantafynColors.Ink,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (progress != null && progress.totalBytes > 0L) {
                                        Text(
                                            "${formatFileSize(progress.bytesDownloaded)} / ${formatFileSize(progress.totalBytes)}",
                                            color = VantafynColors.Muted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }

                        // Download Complete state
                        if (downloadedApkFile != null) {
                            hasInstallPermission = AppUpdateInstaller.canInstallPackages(context)
                            if (!hasInstallPermission) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF231B10))
                                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        "Android requires permission to install apps from Vantafyn.",
                                        color = Color(0xFFFFE082),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                checkState is UpdateCheckResult.UpToDate -> {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = VantafynColors.Primary, fontWeight = FontWeight.Bold)
                    }
                }

                checkState is UpdateCheckResult.Error -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { startCheck() }) {
                            Text("Retry", color = VantafynColors.Primary, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close", color = VantafynColors.Muted)
                        }
                    }
                }

                checkState is UpdateCheckResult.Available -> {
                    val available = checkState as UpdateCheckResult.Available
                    val release = available.releaseInfo

                    when {
                        downloadedApkFile != null -> {
                            if (!hasInstallPermission) {
                                Button(
                                    onClick = {
                                        AppUpdateInstaller.openInstallPermissionSettings(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                ) {
                                    Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        downloadedApkFile?.let { file ->
                                            AppUpdateInstaller.installApk(context, file)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VantafynColors.Primary),
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Install Now", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        isDownloading -> {
                            OutlinedButton(
                                onClick = {
                                    downloadJob?.cancel()
                                    isDownloading = false
                                    downloadProgress = null
                                },
                            ) {
                                Text("Cancel", color = Color(0xFFFF5252))
                            }
                        }

                        else -> {
                            Button(
                                onClick = {
                                    isDownloading = true
                                    downloadJob = coroutineScope.launch {
                                        val result = downloader.downloadApk(release, target) { progress ->
                                            downloadProgress = progress
                                        }
                                        isDownloading = false
                                        result.onSuccess { file ->
                                            downloadedApkFile = file
                                            // Automatically trigger installer if permission is granted
                                            if (AppUpdateInstaller.canInstallPackages(context)) {
                                                AppUpdateInstaller.installApk(context, file)
                                            }
                                        }.onFailure { err ->
                                            checkState = UpdateCheckResult.Error(
                                                err.message ?: "Failed to download update"
                                            )
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VantafynColors.Primary),
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download & Install", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                else -> {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = VantafynColors.Muted)
                    }
                }
            }
        },
        dismissButton = {
            if (checkState is UpdateCheckResult.Available && !isDownloading && downloadedApkFile == null) {
                TextButton(onClick = onDismiss) {
                    Text("Later", color = VantafynColors.Muted)
                }
            }
        },
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        val kb = bytes / 1024.0
        String.format(Locale.US, "%.0f KB", kb)
    }
}

package dev.vantafyn.mobile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.vantafyn.core.ui.VantafynPermissionStatus
import dev.vantafyn.core.ui.VantafynPermissionUiState

private const val PREFS_NAME = "vantafyn_permissions"
private const val KEY_NOTIFICATION_REQUESTED = "notification_requested"
private const val KEY_NOTIFICATION_DISMISSED = "notification_dismissed"
private const val KEY_CHAT_NOTIFICATION_DISMISSED = "chat_notification_dismissed"

private enum class PermissionRequestSource {
    Music,
    Chat,
}

class PermissionRequestCoordinator(
    private val activity: Activity,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
) {
    private val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var pendingAction: (() -> Unit)? = null
    private var pendingSource: PermissionRequestSource? = null

    var notificationState by mutableStateOf(readNotificationState())
        private set
    var showMusicNotificationExplainer by mutableStateOf(false)
        private set
    var showChatNotificationExplainer by mutableStateOf(false)
        private set
    var noticeTitle by mutableStateOf<String?>(null)
        private set
    var noticeMessage by mutableStateOf<String?>(null)
        private set

    fun refresh() {
        notificationState = readNotificationState()
    }

    fun requestForMusicControls(onContinue: () -> Unit) {
        refresh()
        when (notificationState.status) {
            VantafynPermissionStatus.Granted,
            VantafynPermissionStatus.Unsupported,
            -> onContinue()
            VantafynPermissionStatus.NotRequested -> {
                if (notificationState.dismissed) {
                    onContinue()
                } else {
                    pendingAction = onContinue
                    showMusicNotificationExplainer = true
                }
            }
            VantafynPermissionStatus.Denied -> {
                if (notificationState.dismissed) {
                    onContinue()
                } else {
                    pendingAction = onContinue
                    showMusicNotificationExplainer = true
                }
            }
            VantafynPermissionStatus.PermanentlyDenied -> {
                noticeTitle = "Music controls are limited"
                noticeMessage = "Music will still play in the app, but lock-screen controls may not appear unless notifications are allowed in Android Settings."
                onContinue()
            }
        }
    }

    fun requestForChatNotifications() {
        refresh()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) return

        val dismissed = prefs.getBoolean(KEY_CHAT_NOTIFICATION_DISMISSED, false)
        if (!dismissed) {
            showChatNotificationExplainer = true
        }
    }

    fun requestFromSettings() {
        refresh()
        when (notificationState.status) {
            VantafynPermissionStatus.PermanentlyDenied -> openAppNotificationSettings()
            VantafynPermissionStatus.Granted,
            VantafynPermissionStatus.Unsupported,
            -> {
                noticeTitle = "Notification permission"
                noticeMessage = "Music controls and notifications are already allowed on this device."
            }
            else -> {
                pendingAction = null
                showMusicNotificationExplainer = true
            }
        }
    }

    fun allowMusicControls() {
        showMusicNotificationExplainer = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationState = readNotificationState()
            pendingAction?.invoke()
            pendingAction = null
            return
        }
        pendingSource = PermissionRequestSource.Music
        prefs.edit()
            .putBoolean(KEY_NOTIFICATION_REQUESTED, true)
            .putBoolean(KEY_NOTIFICATION_DISMISSED, false)
            .apply()
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun notNow() {
        showMusicNotificationExplainer = false
        prefs.edit().putBoolean(KEY_NOTIFICATION_DISMISSED, true).apply()
        notificationState = readNotificationState()
        noticeTitle = "Music controls are limited"
        noticeMessage = "Music will still play in the app, but lock-screen controls may not appear unless notifications are allowed."
        pendingAction?.invoke()
        pendingAction = null
    }

    fun allowChatNotifications() {
        showChatNotificationExplainer = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationState = readNotificationState()
            return
        }
        if (notificationState.status == VantafynPermissionStatus.PermanentlyDenied) {
            openAppNotificationSettings()
            return
        }
        pendingSource = PermissionRequestSource.Chat
        prefs.edit()
            .putBoolean(KEY_NOTIFICATION_REQUESTED, true)
            .putBoolean(KEY_CHAT_NOTIFICATION_DISMISSED, false)
            .apply()
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun dismissChatExplainer() {
        showChatNotificationExplainer = false
        prefs.edit().putBoolean(KEY_CHAT_NOTIFICATION_DISMISSED, true).apply()
        notificationState = readNotificationState()
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        notificationState = readNotificationState()
        if (!granted) {
            if (pendingSource == PermissionRequestSource.Chat) {
                noticeTitle = "Chat notifications disabled"
                noticeMessage = "Chat notifications are disabled. You can enable them in Android Settings to receive message alerts."
            } else {
                noticeTitle = "Music controls are limited"
                noticeMessage = "Music will still play in the app, but lock-screen controls may not appear unless notifications are allowed."
            }
        }
        pendingSource = null
        pendingAction?.invoke()
        pendingAction = null
    }

    fun dismissNotice() {
        noticeTitle = null
        noticeMessage = null
    }

    fun openAppNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.packageName, null))
        }
        activity.startActivity(intent)
    }

    private fun readNotificationState(): VantafynPermissionUiState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return VantafynPermissionUiState(status = VantafynPermissionStatus.Unsupported)
        }
        val granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val requested = prefs.getBoolean(KEY_NOTIFICATION_REQUESTED, false)
        val dismissed = prefs.getBoolean(KEY_NOTIFICATION_DISMISSED, false)
        val status = when {
            granted -> VantafynPermissionStatus.Granted
            requested && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS) -> VantafynPermissionStatus.PermanentlyDenied
            requested -> VantafynPermissionStatus.Denied
            else -> VantafynPermissionStatus.NotRequested
        }
        return VantafynPermissionUiState(status = status, dismissed = dismissed)
    }
}

@Composable
fun rememberPermissionRequestCoordinator(): PermissionRequestCoordinator {
    val activity = checkNotNull(LocalActivity.current) { "PermissionRequestCoordinator requires an Activity context." }
    var coordinatorRef by remember { mutableStateOf<PermissionRequestCoordinator?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        coordinatorRef?.onNotificationPermissionResult(granted)
    }
    val coordinator = remember(activity, launcher) { PermissionRequestCoordinator(activity, launcher) }
    SideEffect {
        coordinatorRef = coordinator
    }
    LaunchedEffect(Unit) {
        coordinator.refresh()
    }
    return coordinator
}

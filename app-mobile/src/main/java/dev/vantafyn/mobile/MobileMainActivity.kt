package dev.vantafyn.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import dev.vantafyn.core.ui.VantafynSurface
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.core.ui.VantafynPermissionSheet
import dev.vantafyn.feature.home.VantafynAppContent

class MobileMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VantafynTheme {
                VantafynSurface {
                    val permissionCoordinator = rememberPermissionRequestCoordinator()
                    Box {
                        VantafynAppContent(
                            tv = false,
                            notificationPermissionState = permissionCoordinator.notificationState,
                            onRequestMusicControlsPermission = permissionCoordinator::requestForMusicControls,
                            onNotificationPermissionSettingsAction = permissionCoordinator::requestFromSettings,
                        )
                        if (permissionCoordinator.showMusicNotificationExplainer) {
                            VantafynPermissionSheet(
                                title = "Allow music controls?",
                                body = "Vantafyn uses notifications to keep music playing when your phone is locked and to show play, pause, next, and previous controls.",
                                trustNote = "Vantafyn only uses this notification permission for media playback controls. It does not use notifications for ads or tracking.",
                                primaryAction = "Allow controls",
                                secondaryAction = "Not now",
                                onPrimary = permissionCoordinator::allowMusicControls,
                                onSecondary = permissionCoordinator::notNow,
                            )
                        }
                        permissionCoordinator.noticeMessage?.let { message ->
                            VantafynPermissionSheet(
                                title = "Music controls are limited",
                                body = message,
                                primaryAction = "OK",
                                secondaryAction = "Open Android Settings",
                                onPrimary = permissionCoordinator::dismissNotice,
                                onSecondary = permissionCoordinator::openAppNotificationSettings,
                            )
                        }
                    }
                }
            }
        }
    }
}

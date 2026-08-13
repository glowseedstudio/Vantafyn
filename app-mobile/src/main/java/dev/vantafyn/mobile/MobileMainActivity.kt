package dev.vantafyn.mobile

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import dev.vantafyn.core.cast.PlaybackOutputCoordinator
import androidx.compose.foundation.layout.Box
import dev.vantafyn.core.ui.VantafynSurface
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.core.ui.VantafynPermissionSheet
import dev.vantafyn.feature.home.VantafynAppContent
import androidx.fragment.app.FragmentActivity

class MobileMainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            VantafynTheme {
                VantafynSurface {
                    val context = LocalContext.current
                    DisposableEffect(context) {
                        val coordinator = PlaybackOutputCoordinator.get(context)
                        coordinator.start()
                        onDispose { coordinator.stop() }
                    }
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

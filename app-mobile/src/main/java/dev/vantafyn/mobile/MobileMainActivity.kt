package dev.vantafyn.mobile

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
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
import dev.vantafyn.feature.player.VantafynPipState
import android.content.Intent
import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.fragment.app.FragmentActivity
import dev.vantafyn.core.integrations.push.VantafynPushNotifier

class MobileMainActivity : FragmentActivity() {

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val coordinator = PlaybackOutputCoordinator.get(this)
        if (coordinator.state.value.isCasting) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        coordinator.adjustCastVolume(0.05f)
                    }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        coordinator.adjustCastVolume(-0.05f)
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        VantafynPipState.update(isInPictureInPictureMode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        VantafynPushNotifier.routeIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        dev.vantafyn.core.integrations.push.UnifiedPushPayloadDispatcher.isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        dev.vantafyn.core.integrations.push.UnifiedPushPayloadDispatcher.isAppInForeground = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VantafynPushNotifier.routeIntent(intent)
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
                            onRequestChatNotificationsPermission = permissionCoordinator::requestForChatNotifications,
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
                        if (permissionCoordinator.showChatNotificationExplainer) {
                            VantafynPermissionSheet(
                                icon = Icons.Rounded.ChatBubbleOutline,
                                title = "Allow chat notifications?",
                                body = "Vantafyn uses notifications to alert you when friends send you messages, media recommendations, and watch party invites.",
                                trustNote = "Vantafyn only uses notifications for chat messages and unlocks. It does not use notifications for ads or tracking.",
                                primaryAction = "Allow notifications",
                                secondaryAction = "Not now",
                                onPrimary = permissionCoordinator::allowChatNotifications,
                                onSecondary = permissionCoordinator::dismissChatExplainer,
                            )
                        }
                        permissionCoordinator.noticeMessage?.let { message ->
                            VantafynPermissionSheet(
                                title = permissionCoordinator.noticeTitle ?: "Notification permission",
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

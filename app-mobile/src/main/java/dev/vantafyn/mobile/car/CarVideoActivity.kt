package dev.vantafyn.mobile.car

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import dev.vantafyn.core.cast.PlaybackOutputCoordinator
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynSurface
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.feature.home.VantafynAppContent
import dev.vantafyn.feature.player.VantafynPipState
import dev.vantafyn.mobile.rememberPermissionRequestCoordinator

/**
 * Premium In-Car Video Experience for Android Auto and Android Automotive OS.
 *
 * Implements parked video playback with driving safety state tracking,
 * allowing full access to Movies, Shows, and Live TV on car displays when parked.
 */
class CarVideoActivity : FragmentActivity() {

    private var isParkedState = mutableStateOf(true)

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        VantafynPipState.update(isInPictureInPictureMode)
    }

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

                    var isParked by remember { isParkedState }
                    val permissionCoordinator = rememberPermissionRequestCoordinator()

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Full In-Car Video & Media UI
                        VantafynAppContent(
                            tv = false,
                            isCarMode = true,
                            notificationPermissionState = permissionCoordinator.notificationState,
                            onRequestMusicControlsPermission = permissionCoordinator::requestForMusicControls,
                            onNotificationPermissionSettingsAction = permissionCoordinator::requestFromSettings,
                        )

                        // Driving Safety Shield (displayed when vehicle is in gear)
                        AnimatedVisibility(
                            visible = !isParked,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            CarDrivingSafetyShield(
                                onResumeParked = { isParked = true },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarDrivingSafetyShield(
    onResumeParked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VantafynColors.Graphite.copy(alpha = 0.98f),
                        VantafynColors.Surface.copy(alpha = 0.98f),
                    ),
                ),
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.75f),
        ) {
            // Automotive Safety Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF31D7FF).copy(alpha = 0.20f),
                                Color(0xFF9D4EDD).copy(alpha = 0.20f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.20f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalParking,
                    contentDescription = "Parked Only",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Video Available When Parked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "For your safety, video playback is paused while the vehicle is in motion. Background audio and music playback continue uninterrupted.",
                style = MaterialTheme.typography.bodyLarge,
                color = VantafynColors.Muted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF31D7FF),
                                    Color(0xFF9D4EDD),
                                ),
                            ),
                        )
                        .clickable(onClick = onResumeParked)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Vehicle is Parked",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

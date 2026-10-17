package dev.vantafyn.feature.home.games.grid

import android.content.Context
import android.hardware.input.InputManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.view.InputDevice
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Date

/**
 * Live detector for whether a physical game controller (inbuilt handheld controller,
 * attached USB gamepad, or Bluetooth controller) is connected.
 */
@Composable
fun rememberIsGamepadConnected(): Boolean {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(hasConnectedGamepad()) }

    DisposableEffect(context) {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                isConnected = hasConnectedGamepad()
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                isConnected = hasConnectedGamepad()
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                isConnected = hasConnectedGamepad()
            }
        }
        inputManager?.registerInputDeviceListener(listener, null)
        isConnected = hasConnectedGamepad()
        onDispose {
            inputManager?.unregisterInputDeviceListener(listener)
        }
    }

    return isConnected
}

fun hasConnectedGamepad(): Boolean {
    val deviceIds = InputDevice.getDeviceIds()
    for (id in deviceIds) {
        val dev = InputDevice.getDevice(id) ?: continue
        if (dev.isVirtual) continue
        val sources = dev.sources
        if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        ) {
            return true
        }
    }
    return false
}

/**
 * Compact, battery-friendly console status tray showing:
 * - Network status (Wi-Fi or Cellular with signal state)
 * - Battery percentage and charging indicator
 * - Local time formatted to the user's phone default settings
 */
@Composable
fun ConsoleStatusTray(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var timeText by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var isWifi by remember { mutableStateOf(false) }
    var isCellular by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(true) }

    // Update time and hardware status every 15 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val format = android.text.format.DateFormat.getTimeFormat(context)
            timeText = format.format(Date())

            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val rawLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            batteryLevel = rawLevel.coerceIn(0, 100)
            val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: 0
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null
            isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            delay(15000L)
        }
    }

    val pulse = rememberInfiniteTransition(label = "chargingPulse")
    val boltAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "boltAlpha",
    )

    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Network Indicator (Wi-Fi / Cellular)
        if (isWifi) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = "Wi-Fi Connected",
                tint = if (isConnected) Color.White.copy(alpha = 0.9f) else Color(0xFFFF5252),
                modifier = Modifier.size(16.dp),
            )
        } else if (isCellular) {
            Icon(
                imageVector = Icons.Rounded.SignalCellularAlt,
                contentDescription = "Cellular Connected",
                tint = if (isConnected) Color.White.copy(alpha = 0.9f) else Color(0xFFFF5252),
                modifier = Modifier.size(16.dp),
            )
        } else if (!isConnected) {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = "Offline",
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp),
            )
        }

        // Battery symbol with percentage
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$batteryLevel%",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Box(
                modifier = Modifier.size(width = 22.dp, height = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(width = 22.dp, height = 11.dp)) {
                    val strokeW = 1.2.dp.toPx()
                    val bodyWidth = size.width - 3.dp.toPx()
                    val bodyHeight = size.height

                    // Outer battery body outline
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(0f, 0f),
                        size = Size(bodyWidth, bodyHeight),
                        cornerRadius = CornerRadius(2.5.dp.toPx()),
                        style = Stroke(width = strokeW),
                    )

                    // Battery positive terminal notch
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(bodyWidth + 0.5.dp.toPx(), bodyHeight * 0.28f),
                        size = Size(1.8.dp.toPx(), bodyHeight * 0.44f),
                        cornerRadius = CornerRadius(1.dp.toPx()),
                    )

                    // Inner battery fill level
                    val fillPadding = 2.dp.toPx()
                    val maxFillWidth = bodyWidth - fillPadding * 2
                    val currentFillWidth = (maxFillWidth * (batteryLevel / 100f)).coerceAtLeast(0f)
                    val fillColor = if (batteryLevel <= 15) {
                        Color(0xFFFF5252)
                    } else if (isCharging) {
                        Color(0xFF00E5FF)
                    } else {
                        Color.White.copy(alpha = 0.9f)
                    }

                    if (currentFillWidth > 0f) {
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(fillPadding, fillPadding),
                            size = Size(currentFillWidth, bodyHeight - fillPadding * 2),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                        )
                    }
                }

                if (isCharging) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Charging",
                        tint = Color(0xFF00E5FF).copy(alpha = boltAlpha),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        // Time in User's selected phone default format
        if (timeText.isNotEmpty()) {
            Text(
                text = timeText,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

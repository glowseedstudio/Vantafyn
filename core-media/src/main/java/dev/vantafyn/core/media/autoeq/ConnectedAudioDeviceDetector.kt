package dev.vantafyn.core.media.autoeq

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import dev.vantafyn.core.media.VantafynMusicPlaybackService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ConnectedBluetoothAudioDevice(
    val name: String,
    val type: Int,
)

object ConnectedAudioDeviceDetector {

    val BLUETOOTH_OUTPUT_TYPES = setOfNotNull(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AudioDeviceInfo.TYPE_BLE_HEADSET else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AudioDeviceInfo.TYPE_BLE_SPEAKER else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AudioDeviceInfo.TYPE_BLE_BROADCAST else null,
    )

    private val CAR_NAME_REGEX = Regex(
        """\b(car|automotive|infotainment|headunit|head-unit|uconnect|carplay|android\s*auto|entune|idrive|mbux|starlink|mylink|intellilink|sensus|r-link|easylink|carmultimedia|car-audio|caraudio|bt_car|car_bt|carbt|my\s*car|vehicle|in-car|zlink|tlink|autokit|carlinkit|sync)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val CAR_BRANDS_REGEX = Regex(
        """\b(toyota|honda|mazda|ford|subaru|nissan|bmw|audi|mercedes|volkswagen|\bvw\b|volvo|hyundai|kia|lexus|porsche|tesla|chevrolet|chevy|chrysler|dodge|jeep|cadillac|buick|gmc|peugeot|renault|citroen|skoda|seat|mitsubishi|suzuki|genesis|jaguar|infiniti|acura|lincoln)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val SPEAKER_NAME_REGEX = Regex(
        """\b(speaker|soundbar|soundlink|soundbox|home\s*theater)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun isCarDevice(device: AudioDeviceInfo): Boolean {
        if (device.type == AudioDeviceInfo.TYPE_BUS) return true
        val name = device.productName?.toString() ?: return false
        return isCarDeviceName(name)
    }

    fun isCarDeviceName(name: String): Boolean {
        if (name.isBlank()) return false
        return CAR_NAME_REGEX.containsMatchIn(name) || CAR_BRANDS_REGEX.containsMatchIn(name)
    }

    fun isSpeakerDevice(device: AudioDeviceInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER) return true
        val name = device.productName?.toString() ?: return false
        return isSpeakerDeviceName(name)
    }

    fun isSpeakerDeviceName(name: String): Boolean {
        if (name.isBlank()) return false
        return SPEAKER_NAME_REGEX.containsMatchIn(name)
    }

    fun isCarModeActive(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_CAR) return true
        if (VantafynMusicPlaybackService.isAndroidAutoConnected) return true
        return false
    }

    fun isHeadphoneOutputActive(context: Context): Boolean {
        if (isCarModeActive(context)) return false

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val devices = runCatching { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull() ?: emptyArray()
        if (devices.isEmpty()) return false

        if (devices.any { it.type == AudioDeviceInfo.TYPE_BUS }) return false

        val btDevices = devices.filter { it.type in BLUETOOTH_OUTPUT_TYPES }
        if (btDevices.any { isCarDevice(it) }) {
            return false
        }

        val hasWiredOrUsbHeadphone = devices.any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
                device.type == AudioDeviceInfo.TYPE_HEARING_AID
        }

        if (hasWiredOrUsbHeadphone) {
            return true
        }

        val hasHeadphoneBluetooth = btDevices.any { bt ->
            !isCarDevice(bt) && !isSpeakerDevice(bt)
        }

        return hasHeadphoneBluetooth
    }

    fun getConnectedBluetoothDevice(context: Context): ConnectedBluetoothAudioDevice? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return null
        return runCatching {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val bt = devices.firstOrNull { it.type in BLUETOOTH_OUTPUT_TYPES } ?: return null
            val rawName = bt.productName?.toString()?.trim()
            if (rawName.isNullOrBlank()) return null
            ConnectedBluetoothAudioDevice(name = rawName, type = bt.type)
        }.getOrNull()
    }

    fun observeConnectedBluetoothDevice(context: Context): Flow<ConnectedBluetoothAudioDevice?> = callbackFlow {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val handler = Handler(Looper.getMainLooper())
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                trySend(getConnectedBluetoothDevice(context))
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                trySend(getConnectedBluetoothDevice(context))
            }
        }

        trySend(getConnectedBluetoothDevice(context))
        audioManager.registerAudioDeviceCallback(callback, handler)

        awaitClose {
            runCatching { audioManager.unregisterAudioDeviceCallback(callback) }
        }
    }

    fun observeAudioRoutingChanges(context: Context): Flow<Unit> = callbackFlow {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            trySend(Unit)
            close()
            return@callbackFlow
        }

        val handler = Handler(Looper.getMainLooper())
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                trySend(Unit)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                trySend(Unit)
            }
        }

        trySend(Unit)
        audioManager.registerAudioDeviceCallback(callback, handler)

        awaitClose {
            runCatching { audioManager.unregisterAudioDeviceCallback(callback) }
        }
    }
}

package dev.vantafyn.core.media.autoeq

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ConnectedBluetoothAudioDevice(
    val name: String,
    val type: Int,
)

object ConnectedAudioDeviceDetector {

    private val BLUETOOTH_OUTPUT_TYPES = setOfNotNull(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AudioDeviceInfo.TYPE_BLE_HEADSET else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AudioDeviceInfo.TYPE_BLE_SPEAKER else null,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AudioDeviceInfo.TYPE_BLE_BROADCAST else null,
    )

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
}

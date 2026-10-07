package dev.vantafyn.core.emulator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import android.view.Surface
import androidx.annotation.Keep
import java.io.File
import kotlin.concurrent.thread

/**
 * AV characteristics reported by the loaded Libretro core.
 */
data class CoreAvInfo(
    val width: Int,
    val height: Int,
    val aspect: Double,
    val fps: Double,
    val sampleRate: Double,
)

/**
 * Clean-room Kotlin engine that drives the native C11 Vantafyn Libretro Host.
 */
@Keep
class NativeEmulatorEngine(
    private val onFrameRenderedCallback: () -> Unit = {},
    private val onGeometryChangedCallback: (width: Int, height: Int, aspect: Double) -> Unit = { _, _, _ -> },
    private val onCoreMessageCallback: (String) -> Unit = {},
    private val onCoreShutdownCallback: () -> Unit = {},
    private val onFatalErrorCallback: (String) -> Unit = {},
) {
    companion object {
        private const val TAG = "NativeEmulatorEngine"

        init {
            System.loadLibrary("vantafyn_emulator")
        }

        // Standard Libretro joypad button masks
        const val RETRO_DEVICE_ID_JOYPAD_B = 0
        const val RETRO_DEVICE_ID_JOYPAD_Y = 1
        const val RETRO_DEVICE_ID_JOYPAD_SELECT = 2
        const val RETRO_DEVICE_ID_JOYPAD_START = 3
        const val RETRO_DEVICE_ID_JOYPAD_UP = 4
        const val RETRO_DEVICE_ID_JOYPAD_DOWN = 5
        const val RETRO_DEVICE_ID_JOYPAD_LEFT = 6
        const val RETRO_DEVICE_ID_JOYPAD_RIGHT = 7
        const val RETRO_DEVICE_ID_JOYPAD_A = 8
        const val RETRO_DEVICE_ID_JOYPAD_X = 9
        const val RETRO_DEVICE_ID_JOYPAD_L = 10
        const val RETRO_DEVICE_ID_JOYPAD_R = 11
        const val RETRO_DEVICE_ID_JOYPAD_L2 = 12
        const val RETRO_DEVICE_ID_JOYPAD_R2 = 13
        const val RETRO_DEVICE_ID_JOYPAD_L3 = 14
        const val RETRO_DEVICE_ID_JOYPAD_R3 = 15
    }

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    @Volatile private var isAudioRunning = false

    private var isEngineInitialized = false
    @Volatile var isRunning = false
        private set

    init {
        isEngineInitialized = nativeInit()
        if (!isEngineInitialized) {
            Log.e(TAG, "Failed to initialize native emulator session")
        }
    }

    /**
     * Loads a core and game ROM.
     */
    fun loadGame(
        corePath: File,
        romPath: File,
        systemDir: File,
        saveDir: File,
    ): CoreAvInfo? {
        if (!isEngineInitialized) return null

        val av = nativeLoad(
            corePath.absolutePath,
            romPath.absolutePath,
            systemDir.absolutePath,
            saveDir.absolutePath,
        ) ?: return null

        val avInfo = CoreAvInfo(
            width = av[0].toInt(),
            height = av[1].toInt(),
            aspect = av[2],
            fps = av[3],
            sampleRate = av[4],
        )

        initAudio(avInfo.sampleRate.toInt())
        return avInfo
    }

    fun setSurface(surface: Surface?) {
        nativeSetSurface(surface)
    }

    fun setSecondarySurface(surface: Surface?) {
        nativeSetSecondarySurface(surface)
    }

    fun setDualScreenSwap(swap: Boolean) {
        nativeSetDualScreenSwap(swap)
    }

    fun start() {
        nativeStart()
        isRunning = true
    }

    fun pause() {
        nativePause()
        isRunning = false
    }

    fun resume() {
        nativeResume()
        isRunning = true
    }

    fun reset() {
        nativeReset()
    }

    fun stop() {
        isRunning = false
        stopAudio()
        nativeStop()
    }

    fun destroy() {
        stop()
        nativeDestroy()
        isEngineInitialized = false
    }

    fun setInputMask(port: Int, mask: Int) {
        nativeSetInput(port, mask)
    }

    fun setTouch(x: Short, y: Short, pressed: Boolean) {
        nativeSetTouch(x, y, pressed)
    }

    fun saveSram(destFile: File): Boolean {
        destFile.parentFile?.mkdirs()
        return nativeSaveSram(destFile.absolutePath)
    }

    fun loadSram(srcFile: File): Boolean {
        if (!srcFile.exists()) return false
        return nativeLoadSram(srcFile.absolutePath)
    }

    fun saveState(): ByteArray? = nativeSaveState()

    fun loadState(stateData: ByteArray): Boolean = nativeLoadState(stateData)

    fun setFastForward(speedRatio: Int) {
        nativeSetFastForward(speedRatio)
    }

    fun setOption(key: String, value: String) {
        nativeSetOption(key, value)
    }

    @Volatile
    var isMuted: Boolean = false
        set(value) {
            field = value
            audioTrack?.setVolume(if (value) 0f else 1f)
        }

    // -----------------------------------------------------------------------
    // Audio Pipeline via Android AudioTrack
    // -----------------------------------------------------------------------

    private fun initAudio(sampleRate: Int) {
        stopAudio()
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (isMuted) {
                audioTrack?.setVolume(0f)
            }
            audioTrack?.play()
            isAudioRunning = true

            audioThread = thread(name = "VantafynNativeAudio", isDaemon = true) {
                val tempBuffer = ShortArray(2048)
                while (isAudioRunning) {
                    val readSamples = nativeReadAudio(tempBuffer, 0, tempBuffer.size)
                    if (readSamples > 0 && audioTrack != null) {
                        audioTrack?.write(tempBuffer, 0, readSamples)
                    } else {
                        try {
                            Thread.sleep(5)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack setup error", e)
        }
    }

    private fun stopAudio() {
        isAudioRunning = false
        audioThread?.interrupt()
        audioThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioTrack", e)
        }
        audioTrack = null
    }

    // -----------------------------------------------------------------------
    // Native Callbacks invoked from JNI
    // -----------------------------------------------------------------------

    @Keep
    private fun onFrameRendered() {
        onFrameRenderedCallback()
    }

    @Keep
    private fun onGeometryChanged(width: Int, height: Int, aspect: Double) {
        onGeometryChangedCallback(width, height, aspect)
    }

    @Keep
    private fun onCoreMessage(message: String) {
        onCoreMessageCallback(message)
    }

    @Keep
    private fun onCoreShutdown() {
        onCoreShutdownCallback()
    }

    @Keep
    private fun onFatalError(error: String) {
        onFatalErrorCallback(error)
    }

    // -----------------------------------------------------------------------
    // External Native Declarations
    // -----------------------------------------------------------------------

    private external fun nativeInit(): Boolean
    private external fun nativeLoad(corePath: String, romPath: String, systemDir: String, saveDir: String): DoubleArray?
    private external fun nativeStart()
    private external fun nativePause()
    private external fun nativeResume()
    private external fun nativeReset()
    private external fun nativeStop()
    private external fun nativeDestroy()
    private external fun nativeSetSurface(surface: Surface?)
    private external fun nativeSetSecondarySurface(surface: Surface?)
    private external fun nativeSetDualScreenSwap(swap: Boolean)
    private external fun nativeReadAudio(buffer: ShortArray, offset: Int, lengthSamples: Int): Int
    private external fun nativeSetInput(port: Int, mask: Int)
    private external fun nativeSetTouch(x: Short, y: Short, pressed: Boolean)
    private external fun nativeSaveSram(path: String): Boolean
    private external fun nativeLoadSram(path: String): Boolean
    private external fun nativeSaveState(): ByteArray?
    private external fun nativeLoadState(data: ByteArray): Boolean
    private external fun nativeSetFastForward(ratio: Int)
    private external fun nativeSetOption(key: String, value: String)
}

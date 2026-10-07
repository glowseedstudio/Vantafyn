package dev.vantafyn.core.emulator

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Manages downloading, updating, and locating native Libretro cores.
 * Architected to support melonDS (NDS), mGBA (GBA), Gambatte (GB/GBC), etc.
 */
class NativeCoreManager(private val context: Context) {
    companion object {
        private const val TAG = "NativeCoreManager"
        private const val BUILDBOT_BASE = "https://buildbot.libretro.com/nightly/android/latest"

        // Map system identifier to Libretro core name
        private val SYSTEM_TO_CORE = mapOf(
            "nds" to "melonds",
            "gba" to "gpsp",
            "gb" to "gambatte",
            "gbc" to "gambatte",
            "snes" to "snes9x",
            "nes" to "fceumm",
        )
    }

    private val coresDir: File by lazy {
        File(context.filesDir, "native_cores").apply { mkdirs() }
    }

    private val systemDir: File by lazy {
        File(context.filesDir, "system").apply { mkdirs() }
    }

    private val savesDir: File by lazy {
        File(context.filesDir, "saves").apply { mkdirs() }
    }

    fun getSystemDirectory(): File = systemDir
    fun getSaveDirectory(): File = savesDir

    fun getSupportedAbi(): String {
        val supported = Build.SUPPORTED_ABIS
        return when {
            supported.contains("arm64-v8a") -> "arm64-v8a"
            supported.contains("x86_64") -> "x86_64"
            supported.contains("armeabi-v7a") -> "armeabi-v7a"
            supported.contains("x86") -> "x86"
            else -> "arm64-v8a"
        }
    }

    fun getCoreIdForSystem(system: String): String {
        return SYSTEM_TO_CORE[system.lowercase()] ?: system.lowercase()
    }

    fun getCoreFile(coreId: String): File {
        val abi = getSupportedAbi()
        return File(coresDir, "${coreId}_libretro_android_${abi}.so")
    }

    fun isCoreInstalled(coreId: String): Boolean {
        val f = getCoreFile(coreId)
        return f.exists() && f.length() > 0
    }

    /**
     * Downloads and extracts the native core for the device's architecture.
     */
    suspend fun ensureCoreInstalled(coreId: String, onProgress: (Float) -> Unit = {}): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getCoreFile(coreId)
        if (targetFile.exists() && targetFile.length() > 0) {
            return@withContext Result.success(targetFile)
        }

        val abi = getSupportedAbi()
        val urlString = "$BUILDBOT_BASE/$abi/${coreId}_libretro_android.so.zip"
        Log.i(TAG, "Fetching native core from: $urlString")

        try {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; Vantafyn Native Core Downloader)")
                instanceFollowRedirects = true
            }

            if (conn.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP Error ${conn.responseCode} while downloading $coreId"))
            }

            val totalBytes = conn.contentLength.toLong()
            var downloadedBytes = 0L

            val tempZip = File(coresDir, "${coreId}_temp.zip")
            conn.inputStream.use { input ->
                FileOutputStream(tempZip).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            onProgress(downloadedBytes.toFloat() / totalBytes)
                        }
                    }
                }
            }

            // Extract .so from zip
            var extracted = false
            tempZip.inputStream().use { fileIn ->
                ZipInputStream(fileIn).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".so")) {
                            FileOutputStream(targetFile).use { outSo ->
                                zipIn.copyTo(outSo)
                            }
                            extracted = true
                            break
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            tempZip.delete()

            if (extracted && targetFile.exists() && targetFile.length() > 0) {
                targetFile.setExecutable(true, false)
                targetFile.setReadable(true, false)
                Log.i(TAG, "Core $coreId installed successfully (${targetFile.length()} bytes)")
                Result.success(targetFile)
            } else {
                targetFile.delete()
                Result.failure(Exception("Failed to extract valid .so from core archive"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error installing core $coreId", e)
            targetFile.delete()
            Result.failure(e)
        }
    }
}

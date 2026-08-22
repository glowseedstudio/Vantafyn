package dev.vantafyn.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.io.File
import java.net.URL
import java.util.concurrent.Executors

/**
 * Bitmap loader bridge for Media3 MediaSession.
 *
 * Automatically resolves and scales album artwork for external controllers
 * including Wear OS (e.g. Galaxy Watch Ultra, Pixel Watch), Android Auto,
 * and Bluetooth AVRCP receivers.
 */
class VantafynBitmapLoader(
    private val context: Context,
    private val cachedBitmapSupplier: (String) -> Bitmap?,
    private val fallbackBitmapSupplier: () -> Bitmap,
    private val scaleBitmapFn: (Bitmap) -> Bitmap,
    private val cachePutFn: (String, Bitmap) -> Unit,
) : BitmapLoader {

    private val executor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2))

    override fun supportsMimeType(mimeType: String): Boolean =
        mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return executor.submit<Bitmap> {
            val decoded = BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: fallbackBitmapSupplier()
            scaleBitmapFn(decoded)
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val uriString = uri.toString()
        cachedBitmapSupplier(uriString)?.let { cached ->
            return Futures.immediateFuture(cached)
        }

        return executor.submit<Bitmap> {
            try {
                val rawBitmap = when {
                    uriString.startsWith("http://", ignoreCase = true) ||
                    uriString.startsWith("https://", ignoreCase = true) -> {
                        URL(uriString).openStream().use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    uriString.startsWith("content://", ignoreCase = true) -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    uriString.startsWith("file://", ignoreCase = true) -> {
                        val path = uri.path
                        if (path != null && File(path).exists()) {
                            BitmapFactory.decodeFile(path)
                        } else null
                    }
                    File(uriString).exists() -> {
                        BitmapFactory.decodeFile(uriString)
                    }
                    else -> null
                }

                val finalBitmap = if (rawBitmap != null) {
                    val scaled = scaleBitmapFn(rawBitmap)
                    cachePutFn(uriString, scaled)
                    scaled
                } else {
                    fallbackBitmapSupplier()
                }

                finalBitmap
            } catch (_: Throwable) {
                fallbackBitmapSupplier()
            }
        }
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        if (metadata.artworkData != null) {
            return decodeBitmap(metadata.artworkData!!)
        }
        if (metadata.artworkUri != null) {
            return loadBitmap(metadata.artworkUri!!)
        }
        return Futures.immediateFuture(fallbackBitmapSupplier())
    }
}

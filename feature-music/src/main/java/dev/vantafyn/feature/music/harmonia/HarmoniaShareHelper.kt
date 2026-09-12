package dev.vantafyn.feature.music.harmonia

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object HarmoniaShareHelper {

    suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val shareDir = File(context.cacheDir, "harmonia_shares").apply {
            if (!exists()) mkdirs()
        }
        // Clean up older shared images so cache doesn't grow
        shareDir.listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000L) {
                file.delete()
            }
        }

        val file = File(shareDir, "harmonia_recap_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        file
    }

    suspend fun shareRecap(
        context: Context,
        bitmap: Bitmap,
        message: String = "Check out my Vantafyn listening recap!",
    ) {
        try {
            val file = saveBitmapToCache(context, bitmap)
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                clipData = ClipData.newRawUri("Harmonia Recap", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Harmonia Recap").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("HarmoniaShareHelper", "Failed to share recap", e)
        }
    }
}

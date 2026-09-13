package dev.vantafyn.core.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Serves generated album artwork and placeholders to Android Auto and other external
 * media controllers when an album, playlist, or track does not have remote artwork.
 */
class VantafynArtworkContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = "image/png"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: return null
        return try {
            val title = uri.getQueryParameter("title") ?: ""
            val subtitle = uri.getQueryParameter("sub") ?: ""
            val cacheDir = File(ctx.cacheDir, "vantafyn_artwork_placeholders").apply { mkdirs() }
            val key = "${title}_${subtitle}".hashCode().let { abs(it).toString(16) }
            val file = File(cacheDir, "placeholder_$key.png")

            if (!file.exists() || file.length() == 0L) {
                val bitmap = generatePlaceholderBitmap(ctx, title, subtitle)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                bitmap.recycle()
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open placeholder file for $uri: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "VantafynArtworkProvider"
        private const val SIZE = 512

        fun getPlaceholderUri(context: Context, title: String?, subtitle: String? = null): Uri {
            return Uri.Builder()
                .scheme("content")
                .authority("${context.packageName}.artwork")
                .path("placeholder")
                .appendQueryParameter("title", title.orEmpty())
                .appendQueryParameter("sub", subtitle.orEmpty())
                .build()
        }

        private data class Preset(
            val bgStart: Int,
            val bgMid: Int,
            val bgEnd: Int,
            val accent: Int,
            val secondary: Int,
        )

        private val PRESETS = listOf(
            Preset(0xFF030A18.toInt(), 0xFF07213D.toInt(), 0xFF0C3860.toInt(), 0xFF00E5FF.toInt(), 0xFF38BDF8.toInt()),
            Preset(0xFF140521.toInt(), 0xFF2B0A4C.toInt(), 0xFF4C1D95.toInt(), 0xFFFF2E93.toInt(), 0xFFA855F7.toInt()),
            Preset(0xFF1A0A02.toInt(), 0xFF3B1506.toInt(), 0xFF7C2D12.toInt(), 0xFFFF7A00.toInt(), 0xFFFBBF24.toInt()),
            Preset(0xFF021612.toInt(), 0xFF053127.toInt(), 0xFF0F5142.toInt(), 0xFF10B981.toInt(), 0xFF34D399.toInt()),
            Preset(0xFF080C26.toInt(), 0xFF1B184E.toInt(), 0xFF312E81.toInt(), 0xFF6366F1.toInt(), 0xFF818CF8.toInt()),
            Preset(0xFF1E040B.toInt(), 0xFF3F0717.toInt(), 0xFF881337.toInt(), 0xFFFF3366.toInt(), 0xFFFB7185.toInt()),
            Preset(0xFF16061E.toInt(), 0xFF340C37.toInt(), 0xFF581C87.toInt(), 0xFFFF6492.toInt(), 0xFFC084FC.toInt()),
            Preset(0xFF120E03.toInt(), 0xFF2C1C05.toInt(), 0xFF452E07.toInt(), 0xFFF59E0B.toInt(), 0xFFFDE047.toInt()),
        )

        fun generatePlaceholderBitmap(context: Context, title: String?, subtitle: String?): Bitmap {
            val seed = (title?.trim()?.lowercase() ?: subtitle?.trim()?.lowercase() ?: "vantafyn")
            val hash = seed.hashCode()
            val preset = PRESETS[(hash and Int.MAX_VALUE) % PRESETS.size]

            val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Background gradient
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, SIZE.toFloat(), SIZE.toFloat(),
                    intArrayOf(preset.bgStart, preset.bgMid, preset.bgEnd),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val cornerRadius = 48f
            canvas.drawRoundRect(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()), cornerRadius, cornerRadius, bgPaint)

            // 2. Vinyl grooves (concentric circles)
            val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = Color.argb(20, 255, 255, 255)
            }
            val center = SIZE / 2f
            val maxRadius = SIZE * 0.44f
            val grooveFractions = floatArrayOf(0.92f, 0.82f, 0.72f, 0.62f, 0.52f, 0.42f)
            for (fraction in grooveFractions) {
                canvas.drawCircle(center, center, maxRadius * fraction, groovePaint)
            }

            // 3. Center vinyl disc badge
            val badgeRadius = SIZE * 0.22f
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.argb(160, 10, 12, 18)
            }
            canvas.drawCircle(center, center, badgeRadius, badgePaint)

            val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = preset.accent
            }
            canvas.drawCircle(center, center, badgeRadius, badgeStrokePaint)

            // 4. Monogram or Music note
            val monogram = extractMonogram(title)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = if (monogram.length > 1) 68f else 84f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val textBounds = Rect()
            textPaint.getTextBounds(monogram, 0, monogram.length, textBounds)
            val textY = center + (textBounds.height() / 2f) - textBounds.bottom
            canvas.drawText(monogram, center, textY, textPaint)

            return bitmap
        }

        private fun extractMonogram(title: String?): String {
            if (title.isNullOrBlank()) return "♪"
            val cleaned = title.trim()
            val words = cleaned.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }
            return when {
                words.size >= 2 -> {
                    val first = words[0].firstOrNull()?.uppercase() ?: ""
                    val second = words[1].firstOrNull()?.uppercase() ?: ""
                    "$first$second"
                }
                cleaned.length >= 2 -> cleaned.take(2).uppercase()
                else -> cleaned.take(1).uppercase()
            }
        }
    }
}

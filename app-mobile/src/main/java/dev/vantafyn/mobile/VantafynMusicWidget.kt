package dev.vantafyn.mobile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.vantafyn.core.media.VantafynMusicPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private val Ink = Color(0xFFF5F7FF)
private val Muted = Color(0xFFB8C0D8)
private val TrackBg = Color(0x1AFFFFFF)
private val AccentCyan = 0xFF31D7FF.toInt()
private val AccentBlue = 0xFF5B8CFF.toInt()
private val AccentViolet = 0xFF8B5CFF.toInt()
private val AccentMagenta = 0xFFC05CFF.toInt()
private val DarkBg = Color(0xFF141822)

class VantafynMusicWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, null)
        val artist = prefs.getString(KEY_ARTIST, null)
        val artworkUrl = prefs.getString(KEY_ARTWORK_URL, null)
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
        val hasTrack = prefs.getBoolean(KEY_HAS_TRACK, false)
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val durationMs = prefs.getLong(KEY_DURATION_MS, 0L)
        val packageName = context.packageName

        val artBitmap = if (hasTrack && artworkUrl != null) loadWidgetArtwork(context, artworkUrl) else null
        val brandBitmap = makeWidgetBrandMark(context, 180)

        provideContent {
            GlanceTheme {
                if (hasTrack && title != null) {
                    NowPlayingWidget(
                        title = title,
                        artist = artist,
                        isPlaying = isPlaying,
                        packageName = packageName,
                        artBitmap = artBitmap,
                        brandBitmap = brandBitmap,
                        positionMs = positionMs,
                        durationMs = durationMs,
                    )
                } else {
                    EmptyWidget(brandBitmap)
                }
            }
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
        val side = maxOf(bitmap.width, bitmap.height)
        if (side <= targetSize) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val scale = targetSize.toFloat() / side.toFloat()
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
    }

    private fun roundCorners(bitmap: Bitmap, r: Int): Bitmap {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawRoundRect(RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()), r.toFloat(), r.toFloat(), p)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        c.drawBitmap(bitmap, 0f, 0f, p)
        return out
    }

    private suspend fun loadWidgetArtwork(context: Context, artworkUrl: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "$ART_CACHE_PREFIX${Integer.toHexString(artworkUrl.hashCode())}.png")
            runCatching {
                if (cacheFile.exists()) {
                    BitmapFactory.decodeFile(cacheFile.absolutePath)
                } else {
                    URL(artworkUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { raw ->
                            roundCorners(scaleBitmap(raw, ART_SIZE), ART_CORNER_RADIUS).also { rounded ->
                                cacheFile.parentFile?.mkdirs()
                                cacheFile.outputStream().use { out ->
                                    rounded.compress(Bitmap.CompressFormat.PNG, 90, out)
                                }
                            }
                        }
                    }
                }
            }.getOrNull()
        }

    private fun makeWidgetBrandMark(context: Context, size: Int): Bitmap {
        val brand = loadBestBrandDrawable(context)
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                size.toFloat(),
                size.toFloat(),
                intArrayOf(AccentCyan, AccentBlue, AccentViolet, AccentMagenta),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        c.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), size * 0.28f, size * 0.28f, paint)
        val inset = (size * 0.16f).toInt()
        brand.setBounds(inset, inset, size - inset, size - inset)
        brand.draw(c)
        return b
    }

    private fun loadBestBrandDrawable(context: Context): Drawable =
        listOf(
            "vantafyn_logo" to "drawable",
            "ic_launcher_foreground" to "drawable",
            "ic_launcher" to "mipmap",
        )
            .firstNotNullOfOrNull { (name, type) ->
                context.resources.getIdentifier(name, type, context.packageName)
                    .takeIf { it != 0 }
                    ?.let { ContextCompat.getDrawable(context, it) }
            }
            ?: context.packageManager.getApplicationIcon(context.packageName)

    companion object {
        private const val ART_SIZE = 160
        private const val ART_CORNER_RADIUS = 24
        private const val ART_CACHE_PREFIX = "vantafyn_widget_art_"
        const val WIDGET_PREFS = "vantafyn_widget_playback"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_ALBUM = "album"
        const val KEY_ARTWORK_URL = "artwork_url"
        const val KEY_IS_PLAYING = "is_playing"
        const val KEY_HAS_TRACK = "has_track"
        const val KEY_POSITION_MS = "position_ms"
        const val KEY_DURATION_MS = "duration_ms"
    }
}

@Composable
private fun NowPlayingWidget(
    title: String,
    artist: String?,
    isPlaying: Boolean,
    packageName: String,
    artBitmap: Bitmap?,
    brandBitmap: Bitmap,
    positionMs: Long,
    durationMs: Long,
) {
    val openApp = actionStartActivity(MobileMainActivity::class.java)
    val previous = widgetBroadcast(packageName, VantafynMusicPlaybackService.ACTION_PREVIOUS)
    val toggle = widgetBroadcast(packageName, VantafynMusicPlaybackService.ACTION_TOGGLE_PLAYBACK)
    val next = widgetBroadcast(packageName, VantafynMusicPlaybackService.ACTION_NEXT)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable(openApp),
    ) {
        Image(
            provider = ImageProvider(makeWidgetGlassBackground(900, 240)),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (artBitmap != null) {
                    Image(
                        provider = ImageProvider(artBitmap),
                        contentDescription = "Album art",
                        modifier = GlanceModifier.size(54.dp),
                    )
                } else {
                    Image(
                        provider = ImageProvider(brandBitmap),
                        contentDescription = "Vantafyn",
                        modifier = GlanceModifier.size(54.dp),
                    )
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                Column(
                    modifier = GlanceModifier.width(118.dp),
                ) {
                    Text(
                        text = title,
                        style = TextStyle(color = ColorProvider(Ink), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    if (!artist.isNullOrBlank()) {
                        Text(
                            text = artist,
                            style = TextStyle(color = ColorProvider(Muted), fontSize = 12.sp),
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Image(
                    provider = ImageProvider(makeTransportIcon(TransportIcon.Previous, 96, primary = false)),
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(34.dp)
                        .clickable(previous),
                )

                Spacer(modifier = GlanceModifier.width(4.dp))
                Image(
                    provider = ImageProvider(makeTransportIcon(if (isPlaying) TransportIcon.Pause else TransportIcon.Play, 112, primary = true)),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = GlanceModifier
                        .size(42.dp)
                        .clickable(toggle),
                )

                Spacer(modifier = GlanceModifier.width(4.dp))

                Image(
                    provider = ImageProvider(makeTransportIcon(TransportIcon.Next, 96, primary = false)),
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(34.dp)
                        .clickable(next),
                )
            }

            Spacer(modifier = GlanceModifier.height(7.dp))

            val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(TrackBg),
            ) {
                if (progress > 0f) {
                    Image(
                        provider = ImageProvider(makeAccentProgress(progress, 700, 20)),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWidget(brandBitmap: Bitmap) {
    val openApp = actionStartActivity(MobileMainActivity::class.java)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable(openApp),
    ) {
        Image(
            provider = ImageProvider(makeWidgetGlassBackground(900, 240)),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(brandBitmap),
                contentDescription = "Vantafyn",
                modifier = GlanceModifier.size(54.dp),
            )

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.width(180.dp)) {
                Text(
                    text = "Vantafyn Music",
                    style = TextStyle(color = ColorProvider(Ink), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    text = "Start music in Vantafyn",
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun widgetBroadcast(packageName: String, action: String) =
    actionSendBroadcast(
        Intent(action).setClassName(
            packageName,
            VantafynMusicWidgetReceiver::class.java.name,
        ),
    )

private enum class TransportIcon {
    Previous,
    Play,
    Pause,
    Next,
}

private fun makeWidgetGlassBackground(w: Int, h: Int): Bitmap {
    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val radius = h * 0.36f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            w.toFloat(),
            h.toFloat(),
            intArrayOf(
                0xF0182130.toInt(),
                0xEE151A2A.toInt(),
                0xF0181327.toInt(),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
    }
    val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    c.drawRoundRect(rect, radius, radius, paint)

    paint.shader = LinearGradient(
        0f,
        0f,
        0f,
        h.toFloat(),
        intArrayOf(0x55FFFFFF, 0x12FFFFFF, 0x00000000),
        null,
        Shader.TileMode.CLAMP,
    )
    c.drawRoundRect(RectF(1f, 1f, w - 1f, h * 0.58f), radius, radius, paint)

    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    paint.color = 0x3D9CB6FF
    c.drawRoundRect(RectF(1.5f, 1.5f, w - 1.5f, h - 1.5f), radius, radius, paint)
    return b
}

private fun makeGeneratedWidgetBrandMark(size: Int): Bitmap {
    val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
    val radius = size * 0.28f

    paint.shader = LinearGradient(
        0f,
        0f,
        size.toFloat(),
        size.toFloat(),
        intArrayOf(AccentCyan, AccentBlue, AccentViolet, AccentMagenta),
        null,
        Shader.TileMode.CLAMP,
    )
    c.drawRoundRect(rect, radius, radius, paint)

    paint.shader = null
    paint.color = 0xFFFFFFFF.toInt()
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeJoin = Paint.Join.ROUND
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = size * 0.082f

    val left = size * 0.30f
    val right = size * 0.70f
    val top = size * 0.30f
    val mid = size * 0.49f
    val bottom = size * 0.73f
    c.drawLine(left, top, left, bottom, paint)
    c.drawLine(right, top, right, bottom, paint)
    c.drawLine(left, top, right, top, paint)
    c.drawLine(left, mid, right, mid, paint)

    paint.style = Paint.Style.FILL
    c.drawCircle(left - size * 0.10f, bottom, size * 0.105f, paint)
    c.drawCircle(right - size * 0.10f, bottom, size * 0.105f, paint)
    return b
}

private fun makeTransportIcon(icon: TransportIcon, size: Int, primary: Boolean): Bitmap {
    val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val center = size / 2f

    if (primary) {
        paint.shader = LinearGradient(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            intArrayOf(AccentCyan, AccentBlue, AccentViolet, AccentMagenta),
            null,
            Shader.TileMode.CLAMP,
        )
        c.drawCircle(center, center, size * 0.48f, paint)
    }

    paint.shader = null
    paint.color = 0xFFFFFFFF.toInt()
    paint.style = Paint.Style.FILL
    when (icon) {
        TransportIcon.Play -> {
            val path = android.graphics.Path().apply {
                moveTo(size * 0.40f, size * 0.31f)
                lineTo(size * 0.40f, size * 0.69f)
                lineTo(size * 0.70f, size * 0.50f)
                close()
            }
            c.drawPath(path, paint)
        }
        TransportIcon.Pause -> {
            c.drawRoundRect(RectF(size * 0.35f, size * 0.30f, size * 0.45f, size * 0.70f), size * 0.04f, size * 0.04f, paint)
            c.drawRoundRect(RectF(size * 0.55f, size * 0.30f, size * 0.65f, size * 0.70f), size * 0.04f, size * 0.04f, paint)
        }
        TransportIcon.Previous -> {
            c.drawRoundRect(RectF(size * 0.27f, size * 0.31f, size * 0.34f, size * 0.69f), size * 0.025f, size * 0.025f, paint)
            drawTriangle(c, paint, size * 0.36f, size * 0.50f, size * 0.64f, size * 0.31f, size * 0.64f, size * 0.69f)
        }
        TransportIcon.Next -> {
            c.drawRoundRect(RectF(size * 0.66f, size * 0.31f, size * 0.73f, size * 0.69f), size * 0.025f, size * 0.025f, paint)
            drawTriangle(c, paint, size * 0.64f, size * 0.50f, size * 0.36f, size * 0.31f, size * 0.36f, size * 0.69f)
        }
    }
    return b
}

private fun drawTriangle(
    canvas: Canvas,
    paint: Paint,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    x3: Float,
    y3: Float,
) {
    val path = android.graphics.Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
        lineTo(x3, y3)
        close()
    }
    canvas.drawPath(path, paint)
}

private fun makeAccentPill(w: Int, h: Int): Bitmap {
    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(AccentCyan, AccentBlue, AccentViolet, AccentMagenta), null, Shader.TileMode.CLAMP)
    }
    c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), h / 2f, h / 2f, p)
    return b
}

private fun makeAccentProgress(progress: Float, maxW: Int, h: Int): Bitmap {
    val w = (maxW * progress).toInt().coerceAtLeast(1)
    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(AccentCyan, AccentBlue, AccentViolet, AccentMagenta), null, Shader.TileMode.CLAMP)
    }
    c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), h / 2f, h / 2f, p)
    return b
}

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        val artBitmap = if (hasTrack && artworkUrl != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(artworkUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { raw ->
                            roundCorners(scaleBitmap(raw, ART_SIZE), ART_CORNER_RADIUS)
                        }
                    }
                }.getOrNull()
            }
        } else null

        provideContent {
            GlanceTheme {
                if (hasTrack && title != null) {
                    NowPlayingWidget(
                        title = title,
                        artist = artist,
                        isPlaying = isPlaying,
                        packageName = packageName,
                        artBitmap = artBitmap,
                        positionMs = positionMs,
                        durationMs = durationMs,
                    )
                } else {
                    EmptyWidget(packageName)
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

    companion object {
        private const val ART_SIZE = 160
        private const val ART_CORNER_RADIUS = 24
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
    positionMs: Long,
    durationMs: Long,
) {
    val openApp = actionStartActivity(MobileMainActivity::class.java)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (artBitmap != null) {
                Image(
                    provider = ImageProvider(artBitmap),
                    contentDescription = "Album art",
                    modifier = GlanceModifier
                        .size(52.dp)
                        .clickable(openApp),
                )
            } else {
                Box(
                    modifier = GlanceModifier
                        .size(52.dp)
                        .background(TrackBg)
                        .clickable(openApp),
                ) {
                    Text(
                        "\u266B",
                        style = TextStyle(color = ColorProvider(Muted), fontSize = 20.sp),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            Column(
                modifier = GlanceModifier.clickable(openApp),
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

            Spacer(modifier = GlanceModifier.width(4.dp))

            Text(
                text = "|\u25C0",
                style = TextStyle(color = ColorProvider(Ink), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .size(36.dp)
                    .clickable(actionSendBroadcast(Intent(VantafynMusicPlaybackService.ACTION_PREVIOUS).setPackage(packageName))),
            )

            Spacer(modifier = GlanceModifier.width(2.dp))

            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .clickable(actionSendBroadcast(Intent(VantafynMusicPlaybackService.ACTION_TOGGLE_PLAYBACK).setPackage(packageName))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(makeAccentPill(176, 176)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(44.dp),
                )
                Text(
                    text = if (isPlaying) "||" else "\u25B6",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
            }

            Spacer(modifier = GlanceModifier.width(2.dp))

            Text(
                text = "\u25B6|",
                style = TextStyle(color = ColorProvider(Ink), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .size(36.dp)
                    .clickable(actionSendBroadcast(Intent(VantafynMusicPlaybackService.ACTION_NEXT).setPackage(packageName))),
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(4.dp)
                .background(TrackBg),
        ) {
            if (progress > 0f) {
                Image(
                    provider = ImageProvider(makeAccentProgress(progress, 600, 16)),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyWidget(packageName: String) {
    val openApp = actionStartActivity(MobileMainActivity::class.java)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(52.dp)
                    .background(TrackBg),
            ) {
                Text(
                    "\u266B",
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 20.sp),
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.clickable(openApp)) {
                Text(
                    text = "Vantafyn",
                    style = TextStyle(color = ColorProvider(Ink), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Tap to open",
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 13.sp),
                )
            }
        }
    }
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

package dev.vantafyn.mobile

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.vantafyn.core.media.VantafynMusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VantafynMusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VantafynMusicWidget()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            VantafynMusicPlaybackService.ACTION_PLAYBACK_STATE_CHANGED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Main.immediate).launch {
                    try {
                        glanceAppWidget.updateAll(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
                return
            }
            VantafynMusicPlaybackService.ACTION_TOGGLE_PLAYBACK,
            VantafynMusicPlaybackService.ACTION_PREVIOUS,
            VantafynMusicPlaybackService.ACTION_NEXT,
            VantafynMusicPlaybackService.ACTION_STOP -> {
                try {
                    context.startForegroundService(
                        Intent(context, VantafynMusicPlaybackService::class.java).setAction(intent.action)
                    )
                } catch (e: ForegroundServiceStartNotAllowedException) {
                    Log.w("WidgetReceiver", "Cannot start foreground service from background", e)
                }
                return
            }
        }
        super.onReceive(context, intent)
    }
}

package dev.vantafyn.core.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VantafynMusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createMusicPlaybackChannel()

        val controller = MusicPlaybackController.get(this)
        startForegroundImmediately(controller.state.value.currentTrack)
        mediaSession = MediaSession.Builder(this, controller.sessionPlayer)
            .setSessionActivity(createLaunchPendingIntent())
            .build()
            .also { session ->
                setMediaNotificationProvider(
                    DefaultMediaNotificationProvider.Builder(this)
                        .setNotificationId(NOTIFICATION_ID)
                        .setChannelId(CHANNEL_ID)
                        .setChannelName(R.string.vantafyn_music_playback_channel)
                        .build()
                        .apply {
                            setSmallIcon(android.R.drawable.ic_media_play)
                        },
                )
                startAsForegroundService(session)
            }

        serviceScope.launch {
            controller.state.collect {
                triggerNotificationUpdate()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createMusicPlaybackChannel()
        if (mediaSession == null) {
            startForegroundImmediately(MusicPlaybackController.get(this).state.value.currentTrack)
        } else {
            triggerNotificationUpdate()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(CHANNEL_ID)?.apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun startAsForegroundService(session: MediaSession) {
        onUpdateNotification(session, startInForegroundRequired = true)
    }

    private fun startForegroundImmediately(track: VantafynMusicTrack? = null) {
        val subtitle = listOfNotNull(track?.artist, track?.album)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { "Music controls" }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track?.title?.takeIf { it.isNotBlank() } ?: "Vantafyn Music")
            .setContentText(subtitle)
            .setContentIntent(createLaunchPendingIntent())
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createLaunchPendingIntent(): PendingIntent {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(packageName)
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createMusicPlaybackChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Playback controls for music playing in Vantafyn"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "vantafyn_music_controls_v2"
        private const val NOTIFICATION_ID = 4207
    }
}

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
import androidx.media3.common.MediaItem
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

class VantafynMusicPlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private var mediaLibraryProvider: VantafynMusicMediaLibraryProvider? = null
    private lateinit var playbackController: MusicPlaybackController
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastNotificationTrackId: UUID? = null
    private var lastNotificationPlaying: Boolean? = null

    override fun onCreate() {
        super.onCreate()
        createMusicPlaybackChannel()

        playbackController = MusicPlaybackController.get(this)
        mediaLibraryProvider = VantafynMusicMediaLibraryProvider(this)
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
        startForegroundImmediately(playbackController.state.value.currentTrack)
        mediaSession = MediaLibrarySession.Builder(
            this,
            playbackController.sessionPlayer,
            LibraryCallback(playbackController) { mediaLibraryProvider ?: VantafynMusicMediaLibraryProvider(this).also { mediaLibraryProvider = it } },
        )
            .setSessionActivity(createLaunchPendingIntent())
            .build()
            .also { session ->
                startAsForegroundService(session)
            }

        serviceScope.launch {
            playbackController.state.collect { state ->
                val trackId = state.currentTrack?.id
                if (trackId != lastNotificationTrackId || state.isPlaying != lastNotificationPlaying) {
                    lastNotificationTrackId = trackId
                    lastNotificationPlaying = state.isPlaying
                    mediaSession?.let { session -> startForegroundWithMediaNotification(session, state) }
                        ?: triggerNotificationUpdate()
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createMusicPlaybackChannel()
        when (intent?.action) {
            ACTION_TOGGLE_PLAYBACK -> playbackController.togglePlayPause()
            ACTION_PREVIOUS -> playbackController.previous()
            ACTION_NEXT -> playbackController.next()
            ACTION_STOP -> {
                playbackController.stop(reason = VantafynMusicStopReason.User)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (mediaSession == null) {
            startForegroundImmediately(MusicPlaybackController.get(this).state.value.currentTrack)
        } else {
            startForegroundWithMediaNotification(mediaSession ?: return START_STICKY, playbackController.state.value)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val state = if (::playbackController.isInitialized) playbackController.state.value else MusicPlaybackController.get(this).state.value
        startForegroundWithMediaNotification(session, state)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(CHANNEL_ID)?.apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
    }

    override fun onDestroy() {
        LongRunningTaskRegistry.stop(MUSIC_SERVICE_TASK_ID, "service destroyed")
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun startAsForegroundService(session: MediaSession) {
        val state = if (::playbackController.isInitialized) playbackController.state.value else MusicPlaybackController.get(this).state.value
        startForegroundWithMediaNotification(session, state)
    }

    private fun startForegroundImmediately(track: VantafynMusicTrack? = null) {
        LongRunningTaskRegistry.start(
            id = MUSIC_SERVICE_TASK_ID,
            type = LongRunningTaskType.MusicService,
            owner = "VantafynMusicPlaybackService",
            state = if (track == null) "starting" else "active session",
        )
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

    private fun startForegroundWithMediaNotification(session: MediaSession, state: VantafynMusicPlaybackState) {
        val track = state.currentTrack
        LongRunningTaskRegistry.start(
            id = MUSIC_SERVICE_TASK_ID,
            type = LongRunningTaskType.MusicService,
            owner = "VantafynMusicPlaybackService",
            state = if (state.isPlaying) "playing" else "paused",
        )
        val subtitle = listOfNotNull(track?.artist, track?.album)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { "Music controls" }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track?.title?.takeIf { it.isNotBlank() } ?: "Vantafyn Music")
            .setContentText(subtitle)
            .setSubText(track?.album?.takeIf { it.isNotBlank() })
            .setContentIntent(createLaunchPendingIntent())
            .setDeleteIntent(serviceIntent(ACTION_STOP, 4))
            .setOnlyAlertOnce(true)
            .setOngoing(state.isPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(
                state.durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                state.positionMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                state.durationMs <= 0L,
            )
            .addAction(android.R.drawable.ic_media_previous, "Previous", serviceIntent(ACTION_PREVIOUS, 1))
            .addAction(
                if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (state.isPlaying) "Pause" else "Play",
                serviceIntent(ACTION_TOGGLE_PLAYBACK, 2),
            )
            .addAction(android.R.drawable.ic_media_next, "Next", serviceIntent(ACTION_NEXT, 3))
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2),
            )
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

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, VantafynMusicPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

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
        private const val MUSIC_SERVICE_TASK_ID = "music.playbackService"
        private const val ACTION_TOGGLE_PLAYBACK = "dev.vantafyn.music.action.TOGGLE_PLAYBACK"
        private const val ACTION_PREVIOUS = "dev.vantafyn.music.action.PREVIOUS"
        private const val ACTION_NEXT = "dev.vantafyn.music.action.NEXT"
        private const val ACTION_STOP = "dev.vantafyn.music.action.STOP"
    }

    private class LibraryCallback(
        private val playbackController: MusicPlaybackController,
        private val provider: () -> VantafynMusicMediaLibraryProvider,
    ) : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(provider().rootItem(), params))

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            provider().getItem(mediaId)
                ?.let { Futures.immediateFuture(LibraryResult.ofItem(it, null)) }
                ?: Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, null))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
            val items = provider().getChildren(parentId).paged(page, pageSize)
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            val count = provider().search(query)
            session.notifySearchResultChanged(browser, query, count, params)
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
            val items = provider().searchChildren(query).paged(page, pageSize)
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val first = mediaItems.getOrNull(startIndex.coerceAtLeast(0)) ?: mediaItems.firstOrNull()
            val mediaId = first?.mediaId.orEmpty()
            val resolved = when {
                mediaId.startsWith(VantafynMusicMediaLibraryProvider.TRACK_PREFIX) -> provider().resolveQueue(mediaId)
                mediaId == VantafynMusicMediaLibraryProvider.RECENT_ID ||
                    mediaId == VantafynMusicMediaLibraryProvider.SONGS_ID ||
                    mediaId == VantafynMusicMediaLibraryProvider.QUEUE_ID ||
                    mediaId.startsWith(VantafynMusicMediaLibraryProvider.ALBUM_PREFIX) ||
                    mediaId.startsWith(VantafynMusicMediaLibraryProvider.PLAYLIST_PREFIX) ||
                    mediaId.startsWith(VantafynMusicMediaLibraryProvider.SEARCH_PREFIX) -> {
                    val children = provider().getChildren(mediaId)
                    val trackId = children.firstOrNull()?.mediaId.orEmpty()
                    provider().resolveQueue(trackId)
                }
                else -> null
            }
            val queue = resolved?.tracks.orEmpty().map {
                VantafynMusicTrack(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    albumId = it.albumId,
                    durationMs = it.durationMs,
                    streamUrl = it.streamUrl,
                    artworkUrl = it.artworkUrl,
                    isFavorite = it.isFavorite,
                )
            }
            if (queue.isEmpty()) {
                return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs))
            }
            val resolvedItems = playbackController.adoptSystemQueue(queue, resolved?.startIndex ?: startIndex, startPositionMs)
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    resolvedItems,
                    resolved?.startIndex ?: startIndex.coerceIn(0, resolvedItems.lastIndex),
                    startPositionMs.coerceAtLeast(0L),
                ),
            )
        }

        private fun List<MediaItem>.paged(page: Int, pageSize: Int): List<MediaItem> {
            if (page < 0 || pageSize <= 0) return this
            val from = (page * pageSize).coerceAtMost(size)
            val to = (from + pageSize).coerceAtMost(size)
            return subList(from, to)
        }
    }
}

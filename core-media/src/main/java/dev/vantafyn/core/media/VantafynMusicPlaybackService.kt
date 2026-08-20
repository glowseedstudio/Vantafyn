package dev.vantafyn.core.media

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID

class VantafynMusicPlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private var mediaLibraryProvider: VantafynMusicMediaLibraryProvider? = null
    private lateinit var playbackController: MusicPlaybackController
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastNotificationTrackId: UUID? = null
    private var lastNotificationPlaying: Boolean? = null
    private var lastWidgetTrackId: UUID? = null
    private var lastWidgetPlaying: Boolean? = null
    private var lastWidgetArtworkUrl: String? = null
    private var lastWidgetDurationMs: Long = Long.MIN_VALUE
    private var lastWidgetPositionBucket: Long = Long.MIN_VALUE
    private var lastWidgetUpdateAtMs: Long = 0L
    private var isForegroundService = false
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val artworkCache = object : LinkedHashMap<String, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > MAX_ARTWORK_CACHE_SIZE
    }
    private val appIconBitmap: Bitmap by lazy { createFallbackNotificationArtwork() }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Music service created")
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
        try {
            startForegroundImmediately(playbackController.state.value.currentTrack)
        } catch (_: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "Cannot start foreground from background — stopping")
            stopSelf()
            return
        }
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
                    if (!isForegroundService) {
                        Log.d(TAG, "State change → startForeground (track=${trackId?.toString()?.take(8)}, playing=${state.isPlaying})")
                        mediaSession?.let { session -> startForegroundWithMediaNotification(session, state, loadLargeIcon = true) }
                            ?: triggerNotificationUpdate()
                    } else {
                        Log.d(TAG, "State change → notify (track=${trackId?.toString()?.take(8)}, playing=${state.isPlaying})")
                        updateNotificationOnly(state, loadLargeIcon = true)
                    }
                }
                maybePersistWidgetState(state, force = trackId != lastWidgetTrackId || state.isPlaying != lastWidgetPlaying)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createMusicPlaybackChannel()
        if (intent == null || intent.action == null) {
            return super.onStartCommand(intent, flags, startId)
        }
        when (intent.action) {
            ACTION_TOGGLE_PLAYBACK -> playbackController.togglePlayPause()
            ACTION_PREVIOUS -> playbackController.previous()
            ACTION_NEXT -> playbackController.next()
            ACTION_STOP -> {
                playbackController.stop(reason = VantafynMusicStopReason.User)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        try {
            if (mediaSession == null) {
                startForegroundImmediately(MusicPlaybackController.get(this).state.value.currentTrack)
            } else if (!isForegroundService) {
                startForegroundWithMediaNotification(mediaSession ?: return START_STICKY, playbackController.state.value, loadLargeIcon = true)
            } else {
                updateNotificationOnly(playbackController.state.value, loadLargeIcon = true)
            }
        } catch (_: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "Cannot start foreground from background — stopping")
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        playbackController.stop(clearQueue = true, reason = VantafynMusicStopReason.User)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val state = if (::playbackController.isInitialized) playbackController.state.value else MusicPlaybackController.get(this).state.value
        try {
            if (!isForegroundService) {
                startForegroundWithMediaNotification(session, state, loadLargeIcon = true)
            } else {
                updateNotificationOnly(state, loadLargeIcon = true)
            }
        } catch (_: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "Cannot start foreground from background in onUpdateNotification")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(CHANNEL_ID)?.apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Music service destroyed")
        LongRunningTaskRegistry.stop(MUSIC_SERVICE_TASK_ID, "service destroyed")
        isForegroundService = false
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun maybePersistWidgetState(state: VantafynMusicPlaybackState, force: Boolean = false) {
        val track = state.currentTrack
        val now = System.currentTimeMillis()
        val intervalMs = if (AppForegroundStateRepository.isForeground.value) {
            WIDGET_FOREGROUND_REFRESH_MS
        } else {
            WIDGET_BACKGROUND_REFRESH_MS
        }
        val positionBucket = if (state.isPlaying) state.positionMs / intervalMs else state.positionMs / WIDGET_FOREGROUND_REFRESH_MS
        val contentChanged = track?.id != lastWidgetTrackId ||
            state.isPlaying != lastWidgetPlaying ||
            track?.artworkUrl != lastWidgetArtworkUrl ||
            state.durationMs != lastWidgetDurationMs
        val shouldUpdateProgress = AppForegroundStateRepository.isForeground.value &&
            state.isPlaying &&
            positionBucket != lastWidgetPositionBucket &&
            now - lastWidgetUpdateAtMs >= intervalMs
        if (!force && !contentChanged && !shouldUpdateProgress) return

        lastWidgetTrackId = track?.id
        lastWidgetPlaying = state.isPlaying
        lastWidgetArtworkUrl = track?.artworkUrl
        lastWidgetDurationMs = state.durationMs
        lastWidgetPositionBucket = positionBucket
        lastWidgetUpdateAtMs = now
        persistWidgetState(state)
        sendBroadcast(Intent(ACTION_PLAYBACK_STATE_CHANGED).setPackage(packageName))
    }

    private fun persistWidgetState(state: VantafynMusicPlaybackState) {
        val track = state.currentTrack
        getSharedPreferences(WIDGET_PREFS, MODE_PRIVATE).edit().apply {
            putString(KEY_TITLE, track?.title)
            putString(KEY_ARTIST, track?.artist)
            putString(KEY_ALBUM, track?.album)
            putString(KEY_ARTWORK_URL, track?.artworkUrl)
            putBoolean(KEY_IS_PLAYING, state.isPlaying)
            putBoolean(KEY_HAS_TRACK, track != null)
            putLong(KEY_POSITION_MS, state.positionMs)
            putLong(KEY_DURATION_MS, state.durationMs)
            apply()
        }
    }

    private fun startAsForegroundService(session: MediaSession) {
        val state = if (::playbackController.isInitialized) playbackController.state.value else MusicPlaybackController.get(this).state.value
        startForegroundWithMediaNotification(session, state, loadLargeIcon = true)
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
            .setLargeIcon(appIconBitmap)
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

    private fun startForegroundWithMediaNotification(
        session: MediaSession,
        state: VantafynMusicPlaybackState,
        largeIcon: Bitmap? = cachedLargeIcon(state.currentTrack?.artworkUrl),
        loadLargeIcon: Boolean = false,
    ) {
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
            .setLargeIcon(largeIcon ?: appIconBitmap)
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
        isForegroundService = true
        if (loadLargeIcon && track?.artworkUrl != null && largeIcon == null) {
            loadArtworkForNotification(session, state, track.id, track.artworkUrl)
        }
    }

    private fun updateNotificationOnly(state: VantafynMusicPlaybackState, loadLargeIcon: Boolean = false) {
        val session = mediaSession ?: return
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
            .setLargeIcon(cachedLargeIcon(track?.artworkUrl) ?: appIconBitmap)
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

        notificationManager.notify(NOTIFICATION_ID, notification)
        if (loadLargeIcon && track?.artworkUrl != null && cachedLargeIcon(track.artworkUrl) == null) {
            loadArtworkForNotification(session, state, track.id, track.artworkUrl)
        }
    }

    private fun loadArtworkForNotification(session: MediaSession, state: VantafynMusicPlaybackState, trackId: UUID, artworkUrl: String) {
        serviceScope.launch {
            Log.d(TAG, "Loading artwork for track ${trackId.toString().take(8)}")
            val bitmap = loadLargeIcon(artworkUrl) ?: return@launch
            val latestState = playbackController.state.value
            if (latestState.currentTrack?.id == trackId) {
                updateNotificationOnly(latestState, loadLargeIcon = false)
            }
        }
    }

    private fun cachedLargeIcon(artworkUrl: String?): Bitmap? =
        artworkUrl?.let { synchronized(artworkCache) { artworkCache[it] } }

    private suspend fun loadLargeIcon(artworkUrl: String): Bitmap? {
        cachedLargeIcon(artworkUrl)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                URL(artworkUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }?.let { bitmap ->
                    val scaled = scaleNotificationArtwork(bitmap)
                    synchronized(artworkCache) {
                        artworkCache[artworkUrl] = scaled
                    }
                    scaled
                }
            }.getOrNull()
        }
    }

    private fun scaleNotificationArtwork(bitmap: Bitmap): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= NOTIFICATION_ARTWORK_MAX_SIZE) return bitmap
        val scale = NOTIFICATION_ARTWORK_MAX_SIZE.toFloat() / largestSide.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun createFallbackNotificationArtwork(): Bitmap {
        val size = NOTIFICATION_ARTWORK_MAX_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val corner = size * 0.22f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            intArrayOf(
                0xFF101827.toInt(),
                0xFF17233A.toInt(),
                0xFF1A1732.toInt(),
                0xFF281544.toInt(),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(bounds, corner, corner, paint)

        paint.shader = LinearGradient(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            intArrayOf(0xFF36D8FF.toInt(), 0xFF6C75FF.toInt(), 0xFFB45CFF.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.018f
        val ringInset = paint.strokeWidth / 2f
        canvas.drawRoundRect(
            RectF(ringInset, ringInset, size - ringInset, size - ringInset),
            corner,
            corner,
            paint,
        )

        val brandDrawable = loadBestBrandDrawable()
        val iconInset = (size * 0.18f).toInt()
        brandDrawable.setBounds(iconInset, iconInset, size - iconInset, size - iconInset)
        brandDrawable.draw(canvas)
        return bitmap
    }

    private fun loadBestBrandDrawable() =
        listOf(
            "vantafyn_logo" to "drawable",
            "ic_launcher_foreground" to "drawable",
            "ic_launcher" to "mipmap",
        )
            .firstNotNullOfOrNull { (name, type) ->
                resources.getIdentifier(name, type, packageName)
                    .takeIf { it != 0 }
                    ?.let { ContextCompat.getDrawable(this, it) }
            }
            ?: packageManager.getApplicationIcon(packageName)

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getForegroundService(
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
        private const val TAG = "MusicPlaybackService"
        const val CHANNEL_ID = "vantafyn_music_controls_v2"
        private const val NOTIFICATION_ID = 4207
        private const val MUSIC_SERVICE_TASK_ID = "music.playbackService"
        private const val MAX_ARTWORK_CACHE_SIZE = 8
        private const val NOTIFICATION_ARTWORK_MAX_SIZE = 512
        private const val WIDGET_FOREGROUND_REFRESH_MS = 30_000L
        private const val WIDGET_BACKGROUND_REFRESH_MS = 5 * 60_000L
        const val ACTION_TOGGLE_PLAYBACK = "dev.vantafyn.music.action.TOGGLE_PLAYBACK"
        const val ACTION_PREVIOUS = "dev.vantafyn.music.action.PREVIOUS"
        const val ACTION_NEXT = "dev.vantafyn.music.action.NEXT"
        const val ACTION_STOP = "dev.vantafyn.music.action.STOP"
        const val ACTION_PLAYBACK_STATE_CHANGED = "dev.vantafyn.music.action.PLAYBACK_STATE_CHANGED"
        private const val WIDGET_PREFS = "vantafyn_widget_playback"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_ALBUM = "album"
        private const val KEY_ARTWORK_URL = "artwork_url"
        private const val KEY_IS_PLAYING = "is_playing"
        private const val KEY_HAS_TRACK = "has_track"
        private const val KEY_POSITION_MS = "position_ms"
        private const val KEY_DURATION_MS = "duration_ms"
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

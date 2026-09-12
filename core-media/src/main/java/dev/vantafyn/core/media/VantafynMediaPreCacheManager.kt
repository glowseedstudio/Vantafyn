package dev.vantafyn.core.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Smart Pre-Caching Manager for Media3 ExoPlayer.
 *
 * Automatically pre-caches the upcoming 3 to 5 tracks in the active queue into [SimpleCache]
 * using Media3's [CacheWriter]. Handles rapid-skipping debounce, network awareness,
 * and cooperative cancellation.
 */
@OptIn(UnstableApi::class)
class VantafynMediaPreCacheManager(
    private val context: Context,
    private val cache: SimpleCache,
    private val cacheDataSourceFactory: CacheDataSource.Factory,
    val prefetchAheadCount: Int = 3,
    parentScope: CoroutineScope? = null,
) {
    private val appContext = context.applicationContext
    private val scope = parentScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var currentPrefetchJob: Job? = null
    private val activeWriters = ConcurrentHashMap<String, CacheWriter>()
    private var attachedPlayer: Player? = null

    /**
     * Player.Listener that reacts to queue changes, track progression, and playback state.
     */
    val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            schedulePrefetch(reasonLabel = "onMediaItemTransition")
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            schedulePrefetch(reasonLabel = "onTimelineChanged")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE, Player.STATE_ENDED -> cancelAll("playback ended/idle")
                Player.STATE_READY -> schedulePrefetch(reasonLabel = "playback ready")
            }
        }
    }

    /**
     * Attaches this pre-fetcher to a live [Player].
     */
    fun attachPlayer(player: Player) {
        if (attachedPlayer == player) return
        attachedPlayer?.removeListener(playerListener)
        attachedPlayer = player
        player.addListener(playerListener)
        schedulePrefetch("initial attach")
    }

    /**
     * Detaches from the [Player].
     */
    fun detachPlayer(player: Player) {
        player.removeListener(playerListener)
        if (attachedPlayer == player) attachedPlayer = null
        cancelAll("detached player")
    }

    /**
     * Triggers a debounced pre-cache cycle for upcoming tracks.
     */
    fun schedulePrefetch(reasonLabel: String = "") {
        val player = attachedPlayer ?: return
        val currentIndex = player.currentMediaItemIndex
        val totalCount = player.mediaItemCount

        if (currentIndex < 0 || totalCount <= 0) {
            cancelAll("empty queue")
            return
        }

        // Collect upcoming MediaItems
        val upcomingItems = mutableListOf<MediaItem>()
        val maxIndex = minOf(currentIndex + prefetchAheadCount, totalCount - 1)
        for (i in (currentIndex + 1)..maxIndex) {
            upcomingItems.add(player.getMediaItemAt(i))
        }

        if (upcomingItems.isEmpty()) {
            Log.d(TAG, "No upcoming tracks to pre-cache ($reasonLabel)")
            return
        }

        // Cancel previous pre-cache job (e.g. if the user skipped tracks)
        currentPrefetchJob?.cancel()

        currentPrefetchJob = scope.launch(Dispatchers.IO) {
            // 400ms debounce: avoids firing wasteful downloads during rapid skipping
            delay(DEBOUNCE_MS)

            if (!isActive) return@launch

            // Network Awareness Check
            if (!isNetworkConnected()) {
                Log.d(TAG, "Skipping pre-cache: No active or validated network connection")
                return@launch
            }

            Log.d(TAG, "Starting pre-cache for ${upcomingItems.size} tracks (reason: $reasonLabel)")

            // Also pre-cache current track artwork to ensure it is immediately on disk
            if (currentIndex in 0 until totalCount) {
                runCatching {
                    player.getMediaItemAt(currentIndex).mediaMetadata.artworkUri?.let { artUri ->
                        launch(Dispatchers.IO) {
                            VantafynArtworkLoader.preCacheArtwork(appContext, artUri.toString())
                        }
                    }
                }
            }

            for ((offset, mediaItem) in upcomingItems.withIndex()) {
                if (!isActive) break

                // Pre-cache artwork alongside track audio stream
                mediaItem.mediaMetadata.artworkUri?.let { artUri ->
                    launch(Dispatchers.IO) {
                        VantafynArtworkLoader.preCacheArtwork(appContext, artUri.toString())
                    }
                }

                val uri = mediaItem.localConfiguration?.uri ?: continue
                val cacheKey = mediaItem.localConfiguration?.customCacheKey ?: uri.toString()

                // Skip local file downloads (file:// or content://)
                if (!isNetworkUri(uri)) {
                    Log.d(TAG, "Skipping pre-cache for local/offline track: $cacheKey")
                    continue
                }

                // If network dropped mid-queue, abort remaining items
                if (!isNetworkConnected()) {
                    Log.d(TAG, "Network lost during pre-cache loop. Aborting remaining items.")
                    break
                }

                preCacheTrack(uri, cacheKey, trackIndexOffset = offset + 1)
            }
        }
    }

    /**
     * Uses Media3's [CacheWriter] to buffer the track into [SimpleCache].
     */
    private suspend fun preCacheTrack(uri: Uri, cacheKey: String, trackIndexOffset: Int) {
        withContext(Dispatchers.IO) {
            val dataSpec = DataSpec.Builder()
                .setUri(uri)
                .setKey(cacheKey)
                .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                .build()

            // Use DataSource specifically configured for downloading
            val downloadingDataSource = cacheDataSourceFactory.createDataSourceForDownloading()

            val writer = CacheWriter(
                /* dataSource = */ downloadingDataSource,
                /* dataSpec = */ dataSpec,
                /* tempBuffer = */ ByteArray(BUFFER_SIZE_BYTES),
                /* progressListener = */ null,
            )

            activeWriters[cacheKey] = writer
            try {
                Log.d(TAG, "Buffering upcoming track +$trackIndexOffset: $cacheKey")
                writer.cache()
                Log.d(TAG, "Successfully pre-cached track +$trackIndexOffset: $cacheKey")
            } catch (e: CancellationException) {
                writer.cancel()
                throw e
            } catch (e: InterruptedIOException) {
                // Normal when cancelled by user skipping
                Log.d(TAG, "Pre-caching cancelled for track +$trackIndexOffset: $cacheKey")
            } catch (e: Exception) {
                Log.w(TAG, "Pre-cache failed for track +$trackIndexOffset ($cacheKey): ${e.message}")
            } finally {
                activeWriters.remove(cacheKey)
            }
        }
    }

    /**
     * Cancels any active pre-fetching jobs and running [CacheWriter]s immediately.
     */
    fun cancelAll(reason: String = "cancelled") {
        Log.d(TAG, "Cancelling all pre-caching tasks: $reason")
        currentPrefetchJob?.cancel()
        currentPrefetchJob = null

        activeWriters.values.forEach { writer ->
            runCatching { writer.cancel() }
        }
        activeWriters.clear()
    }

    /**
     * Checks if the device has an active, validated internet connection.
     */
    private fun isNetworkConnected(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun isNetworkUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        return scheme == "http" || scheme == "https"
    }

    companion object {
        private const val TAG = "VantafynPreCache"
        private const val DEBOUNCE_MS = 400L
        private const val BUFFER_SIZE_BYTES = 128 * 1024 // 128 KB chunk buffer
    }
}

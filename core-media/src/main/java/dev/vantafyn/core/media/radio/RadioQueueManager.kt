package dev.vantafyn.core.media.radio

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.media.VantafynMusicTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID

/**
 * Dynamic Queue Replenisher for Infinite Radio & Smart Mixes.
 *
 * Automatically monitors the remaining queue depth in Media3 ExoPlayer. When remaining tracks
 * fall to or below the threshold, fetches similar tracks from Jellyfin and seamlessly appends
 * them to the active player queue while tracking history for absolute deduplication.
 */
class RadioQueueManager(
    private val playbackController: MusicPlaybackController,
    parentScope: CoroutineScope? = null,
) {
    private val scope = parentScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isRadioActive = MutableStateFlow(false)
    val isRadioActive: StateFlow<Boolean> = _isRadioActive.asStateFlow()

    private val _isFetchingBatch = MutableStateFlow(false)
    val isFetchingBatch: StateFlow<Boolean> = _isFetchingBatch.asStateFlow()

    private val _currentSeedTrack = MutableStateFlow<VantafynMusicTrack?>(null)
    val currentSeedTrack: StateFlow<VantafynMusicTrack?> = _currentSeedTrack.asStateFlow()

    /**
     * Delegate supplied by the application/viewmodel to fetch similar tracks from Jellyfin.
     */
    @Volatile
    var fetchDelegate: (suspend (seedTrack: VantafynMusicTrack, excludeIds: Set<UUID>, limit: Int) -> List<VantafynMusicTrack>)? = null

    /**
     * Session history set preventing duplicate tracks during continuous radio playback.
     */
    val historyTrackIds: MutableSet<UUID> = Collections.synchronizedSet(mutableSetOf<UUID>())

    private var activeFetchJob: Job? = null
    private var attachedPlayer: Player? = null

    /**
     * Player listener checking queue depth on transitions.
     */
    val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            checkAndTopUpQueue("onMediaItemTransition")
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            checkAndTopUpQueue("onTimelineChanged")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && _isRadioActive.value) {
                checkAndTopUpQueue("playback ended")
            }
        }
    }

    fun attachPlayer(player: Player) {
        if (attachedPlayer == player) return
        attachedPlayer?.removeListener(playerListener)
        attachedPlayer = player
        player.addListener(playerListener)
    }

    fun detachPlayer(player: Player) {
        player.removeListener(playerListener)
        if (attachedPlayer == player) attachedPlayer = null
        stopRadio("detached player")
    }

    /**
     * Starts Infinite Radio anchored on the given seed track.
     */
    fun startRadio(seedTrack: VantafynMusicTrack) {
        Log.d(TAG, "Starting Radio anchored on: '${seedTrack.title}' (${seedTrack.id})")
        activeFetchJob?.cancel()
        historyTrackIds.clear()
        historyTrackIds.add(seedTrack.id)

        _currentSeedTrack.value = seedTrack
        _isRadioActive.value = true

        // Play seed track as index 0
        playbackController.playQueue(
            queue = listOf(seedTrack),
            startIndex = 0,
        )

        // Fetch initial batch of similar tracks
        fetchBatch(seedTrack, INITIAL_BATCH_SIZE)
    }

    /**
     * Disables Radio Mode and clears history.
     */
    fun stopRadio(reason: String = "user_stopped") {
        if (!_isRadioActive.value && _currentSeedTrack.value == null) return
        Log.d(TAG, "Stopping Radio Mode: $reason")
        activeFetchJob?.cancel()
        activeFetchJob = null
        _isRadioActive.value = false
        _isFetchingBatch.value = false
        _currentSeedTrack.value = null
        historyTrackIds.clear()
    }

    /**
     * Checks if the queue has fallen below the threshold and needs more tracks.
     */
    fun checkAndTopUpQueue(triggerReason: String = "") {
        if (!_isRadioActive.value || _isFetchingBatch.value) return

        val player = attachedPlayer ?: playbackController.sessionPlayer
        val currentIndex = player.currentMediaItemIndex
        val totalCount = player.mediaItemCount

        if (currentIndex < 0 || totalCount <= 0) return

        val remainingTracks = totalCount - (currentIndex + 1)
        Log.d(TAG, "Queue check ($triggerReason): $remainingTracks remaining (threshold: $TOP_UP_THRESHOLD)")

        if (remainingTracks <= TOP_UP_THRESHOLD) {
            val seed = _currentSeedTrack.value ?: return
            fetchBatch(seed, REFILL_BATCH_SIZE)
        }
    }

    private fun fetchBatch(seed: VantafynMusicTrack, batchSize: Int) {
        val delegate = fetchDelegate ?: run {
            Log.w(TAG, "Cannot fetch radio batch: fetchDelegate is not configured")
            return
        }

        if (_isFetchingBatch.value) return

        activeFetchJob?.cancel()
        activeFetchJob = scope.launch {
            _isFetchingBatch.value = true
            try {
                Log.d(TAG, "Fetching radio batch (history size: ${historyTrackIds.size})")
                val newTracks = withContext(Dispatchers.IO) {
                    delegate(seed, historyTrackIds.toSet(), batchSize)
                }

                if (!isActive) return@launch

                if (newTracks.isEmpty()) {
                    Log.w(TAG, "No new tracks returned from Jellyfin for Radio. Library may be exhausted.")
                    val player = attachedPlayer ?: playbackController.sessionPlayer
                    val remaining = player.mediaItemCount - (player.currentMediaItemIndex + 1)
                    if (remaining <= 0) {
                        stopRadio("library_exhausted")
                    }
                    return@launch
                }

                // Register all new IDs into history
                newTracks.forEach { historyTrackIds.add(it.id) }

                // Append new tracks to player queue
                withContext(Dispatchers.Main) {
                    playbackController.addMultipleToQueue(newTracks)
                    Log.d(TAG, "Successfully appended ${newTracks.size} radio tracks to queue")
                }
            } catch (e: CancellationException) {
                // Cancelled normally
            } catch (e: Exception) {
                Log.w(TAG, "Radio top-up failed (will retry on next track transition): ${e.message}")
            } finally {
                _isFetchingBatch.value = false
            }
        }
    }

    companion object {
        private const val TAG = "RadioQueueManager"
        const val TOP_UP_THRESHOLD = 3
        const val INITIAL_BATCH_SIZE = 15
        const val REFILL_BATCH_SIZE = 10
    }
}

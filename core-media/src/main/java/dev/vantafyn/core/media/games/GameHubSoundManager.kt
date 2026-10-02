package dev.vantafyn.core.media.games

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

import dev.vantafyn.core.media.MusicPlaybackController

/**
 * Ambient background audio manager for Vantafyn Game Hub.
 *
 * Plays looping menu audio (gamehub.mp3) with smooth volume fade-in when entering the
 * hub, smooth fade-out when switching away to the main home screen or other destinations,
 * and immediate stop when an emulator game session begins.
 */
enum class GameHubTrack(val assetName: String) {
    GAME_HUB("gamehub.mp3"),
    POKEMON_HOME("warm_home.mp3"),
}

object GameHubSoundManager {

    private const val TAG = "GameHubSoundManager"
    const val DEFAULT_VOLUME = 0.40f
    private const val ASSET_NAME = "gamehub.mp3"
    private const val PREFS_NAME = "vantafyn_retro_settings"
    private const val KEY_BGM_ENABLED = "gamehub_bgm_enabled"
    private const val KEY_BGM_VOLUME = "gamehub_bgm_volume"

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var fadeJob: Job? = null
    private var musicObserverJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    @Volatile
    var activeTrack: GameHubTrack = GameHubTrack.GAME_HUB
        private set

    @Volatile
    private var currentVolume = 0.0f
    @Volatile
    private var isPlayingOrFadingIn = false
    @Volatile
    private var duckCount = 0

    /**
     * Checks if standard music playback is currently active in Vantafyn.
     */
    fun isMusicPlaying(context: Context): Boolean {
        return runCatching {
            MusicPlaybackController.get(context).state.value.isPlaying
        }.getOrDefault(false)
    }

    fun isMusicEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BGM_ENABLED, true)
    }

    fun setMusicEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BGM_ENABLED, enabled).apply()
        if (!enabled) {
            fadeOut(durationMs = 400L)
        } else {
            fadeIn(context)
        }
    }

    fun getTargetVolume(context: Context): Float {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getFloat(KEY_BGM_VOLUME, DEFAULT_VOLUME)
        return if (saved >= 0.64f && saved <= 0.66f) DEFAULT_VOLUME else saved.coerceIn(0.0f, 1.0f)
    }

    fun setTargetVolume(context: Context, volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_BGM_VOLUME, clamped).apply()
        if (isPlayingOrFadingIn) {
            currentVolume = clamped
            try {
                mediaPlayer?.setVolume(clamped, clamped)
            } catch (e: Exception) {
                Log.w(TAG, "Error setting volume: ${e.message}")
            }
        }
    }

    /**
     * Seamlessly crossfades between the currently playing track and a new target track.
     * Smoothly lowers outgoing track volume while ramping up incoming track volume.
     */
    @Synchronized
    fun crossfadeTo(context: Context, targetTrack: GameHubTrack, durationMs: Long = 1000L) {
        val appContext = context.applicationContext
        if (activeTrack == targetTrack && (isPlayingOrFadingIn || mediaPlayer?.isPlaying == true)) {
            return
        }

        if (!isMusicEnabled(appContext)) {
            activeTrack = targetTrack
            return
        }

        if (isMusicPlaying(appContext)) {
            Log.d(TAG, "Music playback is active; suppressing ambient track.")
            activeTrack = targetTrack
            return
        }

        val targetVol = getTargetVolume(appContext)
        val outgoingPlayer = mediaPlayer
        val outgoingVol = currentVolume

        val incomingPlayer = try {
            createPlayer(appContext, targetTrack.assetName)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create incoming player for ${targetTrack.assetName}: ${e.message}")
            null
        }

        if (incomingPlayer == null) {
            activeTrack = targetTrack
            fadeOut(durationMs = 400L)
            return
        }

        mediaPlayer = incomingPlayer
        activeTrack = targetTrack
        isPlayingOrFadingIn = true

        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                incomingPlayer.setVolume(0f, 0f)
                incomingPlayer.start()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start incoming player: ${e.message}")
                resetPlayer()
                return@launch
            }

            val stepMs = 25L
            val stepCount = (durationMs / stepMs).coerceAtLeast(1)

            for (i in 1..stepCount) {
                delay(stepMs)
                val fraction = i.toFloat() / stepCount
                val outVol = (outgoingVol * (1f - fraction)).coerceAtLeast(0f)
                val inVol = (targetVol * fraction).coerceIn(0f, targetVol)
                currentVolume = inVol

                try {
                    outgoingPlayer?.setVolume(outVol, outVol)
                } catch (_: Exception) { }

                try {
                    incomingPlayer.setVolume(inVol, inVol)
                } catch (_: Exception) { }
            }

            currentVolume = targetVol
            try {
                incomingPlayer.setVolume(targetVol, targetVol)
            } catch (_: Exception) { }

            try {
                outgoingPlayer?.setVolume(0f, 0f)
                if (outgoingPlayer?.isPlaying == true) {
                    outgoingPlayer.pause()
                }
                outgoingPlayer?.release()
            } catch (_: Exception) { }
        }
    }

    /**
     * Smoothly fades in the Game Hub ambient music up to the target volume (default 40%).
     */
    @Synchronized
    fun fadeIn(context: Context, durationMs: Long = 800L, targetVolume: Float? = null, track: GameHubTrack = activeTrack) {
        val appContext = context.applicationContext
        if (track != activeTrack && isPlayingOrFadingIn) {
            crossfadeTo(appContext, track, durationMs)
            return
        }
        activeTrack = track

        if (!isMusicEnabled(appContext)) {
            return
        }

        if (isMusicPlaying(appContext)) {
            Log.d(TAG, "Music playback is active; suppressing Game Hub ambient music.")
            isPlayingOrFadingIn = false
            return
        }

        val target = (targetVolume ?: getTargetVolume(appContext)).coerceIn(0.0f, 1.0f)
        isPlayingOrFadingIn = true

        val player = try {
            ensurePlayer(appContext, track)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize MediaPlayer for Game Hub audio: ${e.message}")
            return
        } ?: return

        try {
            if (!player.isPlaying) {
                player.setVolume(currentVolume, currentVolume)
                player.start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error starting MediaPlayer: ${e.message}")
            resetPlayer()
            return
        }

        musicObserverJob?.cancel()
        musicObserverJob = scope.launch {
            try {
                MusicPlaybackController.get(appContext).state.collect { state ->
                    if (state.isPlaying && isPlayingOrFadingIn) {
                        Log.d(TAG, "Music playback started; fading out Game Hub audio.")
                        fadeOut(durationMs = 400L)
                    }
                }
            } catch (_: Exception) { }
        }

        fadeJob?.cancel()
        fadeJob = scope.launch {
            val stepMs = 25L
            val stepCount = (durationMs / stepMs).coerceAtLeast(1)
            val startVol = currentVolume
            val volumeDelta = (target - startVol) / stepCount

            for (i in 1..stepCount) {
                delay(stepMs)
                currentVolume = (startVol + volumeDelta * i).coerceIn(0.0f, target)
                try {
                    mediaPlayer?.setVolume(currentVolume, currentVolume)
                } catch (e: Exception) {
                    break
                }
            }
            currentVolume = target
            try {
                mediaPlayer?.setVolume(currentVolume, currentVolume)
            } catch (_: Exception) { }
        }
    }

    /**
     * Smoothly fades out the Game Hub music down to silence, then pauses.
     */
    @Synchronized
    fun fadeOut(durationMs: Long = 600L, onComplete: (() -> Unit)? = null) {
        isPlayingOrFadingIn = false
        duckCount = 0
        musicObserverJob?.cancel()
        musicObserverJob = null
        val player = mediaPlayer ?: return

        fadeJob?.cancel()
        fadeJob = scope.launch {
            val stepMs = 25L
            val stepCount = (durationMs / stepMs).coerceAtLeast(1)
            val startVol = currentVolume
            val volumeDelta = startVol / stepCount

            for (i in 1..stepCount) {
                delay(stepMs)
                currentVolume = (startVol - volumeDelta * i).coerceAtLeast(0.0f)
                try {
                    player.setVolume(currentVolume, currentVolume)
                } catch (e: Exception) {
                    break
                }
            }
            currentVolume = 0.0f
            try {
                player.setVolume(0f, 0f)
                if (player.isPlaying) {
                    player.pause()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error pausing MediaPlayer after fade-out: ${e.message}")
            }
            onComplete?.invoke()
        }
    }

    /**
     * Ducks (smoothly lowers) ambient background music down to a lower percentage (e.g. 20% of target volume)
     * so that foreground sound effects (like Pokémon cries) can be clearly heard.
     * Uses reference counting so nested screens don't unduck prematurely.
     */
    @Synchronized
    fun duck(context: Context, duckFactor: Float = 0.20f, durationMs: Long = 400L) {
        val appContext = context.applicationContext
        duckCount++
        if (duckCount > 1) return // Already ducked
        val player = mediaPlayer ?: return
        if (!player.isPlaying && !isPlayingOrFadingIn) return

        val baseTarget = getTargetVolume(appContext)
        val duckTarget = (baseTarget * duckFactor).coerceIn(0.01f, 1.0f)

        fadeJob?.cancel()
        fadeJob = scope.launch {
            val stepMs = 25L
            val stepCount = (durationMs / stepMs).coerceAtLeast(1)
            val startVol = currentVolume
            val volumeDelta = (duckTarget - startVol) / stepCount
            val minV = minOf(startVol, duckTarget)
            val maxV = maxOf(startVol, duckTarget)

            for (i in 1..stepCount) {
                delay(stepMs)
                currentVolume = (startVol + volumeDelta * i).coerceIn(minV, maxV)
                try {
                    player.setVolume(currentVolume, currentVolume)
                } catch (e: Exception) {
                    break
                }
            }
            currentVolume = duckTarget
            try {
                player.setVolume(currentVolume, currentVolume)
            } catch (_: Exception) { }
        }
    }

    /**
     * Restores ambient background music from ducked volume back to the target volume.
     */
    @Synchronized
    fun unduck(context: Context, durationMs: Long = 400L) {
        val appContext = context.applicationContext
        duckCount = (duckCount - 1).coerceAtLeast(0)
        if (duckCount > 0) return // Still nested
        val player = mediaPlayer ?: return
        if (!player.isPlaying && !isPlayingOrFadingIn) return

        val baseTarget = getTargetVolume(appContext)

        fadeJob?.cancel()
        fadeJob = scope.launch {
            val stepMs = 25L
            val stepCount = (durationMs / stepMs).coerceAtLeast(1)
            val startVol = currentVolume
            val volumeDelta = (baseTarget - startVol) / stepCount
            val minV = minOf(startVol, baseTarget)
            val maxV = maxOf(startVol, baseTarget)

            for (i in 1..stepCount) {
                delay(stepMs)
                currentVolume = (startVol + volumeDelta * i).coerceIn(minV, maxV)
                try {
                    player.setVolume(currentVolume, currentVolume)
                } catch (e: Exception) {
                    break
                }
            }
            currentVolume = baseTarget
            try {
                player.setVolume(currentVolume, currentVolume)
            } catch (_: Exception) { }
        }
    }

    /**
     * Stops playback immediately. Always call when an emulator game starts playing.
     */
    @Synchronized
    fun stop(instant: Boolean = true) {
        isPlayingOrFadingIn = false
        duckCount = 0
        musicObserverJob?.cancel()
        musicObserverJob = null
        fadeJob?.cancel()
        fadeJob = null

        val player = mediaPlayer ?: return
        try {
            if (instant) {
                currentVolume = 0.0f
                player.setVolume(0f, 0f)
                if (player.isPlaying) {
                    player.pause()
                }
                player.seekTo(0)
            } else {
                fadeOut(durationMs = 200L) {
                    try {
                        player.seekTo(0)
                    } catch (_: Exception) { }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Game Hub sound player: ${e.message}")
        }
    }

    /**
     * Pauses playback (e.g. when app moves to background).
     */
    @Synchronized
    fun pause() {
        duckCount = 0
        musicObserverJob?.cancel()
        musicObserverJob = null
        fadeJob?.cancel()
        fadeJob = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception)
        {
            Log.w(TAG, "Error pausing Game Hub audio: ${e.message}")
        }
    }

    /**
     * Resumes playback with a smooth fade in if it was active and music is not playing.
     */
    fun resume(context: Context) {
        if (isMusicPlaying(context)) {
            Log.d(TAG, "Music playback is active; suppressing Game Hub ambient resume.")
            return
        }
        if (isPlayingOrFadingIn) {
            fadeIn(context, durationMs = 500L)
        }
    }

    /**
     * Releases MediaPlayer resources completely.
     */
    @Synchronized
    fun release() {
        isPlayingOrFadingIn = false
        musicObserverJob?.cancel()
        musicObserverJob = null
        fadeJob?.cancel()
        fadeJob = null
        try {
            mediaPlayer?.release()
        } catch (_: Exception) { }
        mediaPlayer = null
        currentVolume = 0.0f
    }

    @Synchronized
    private fun ensurePlayer(context: Context, track: GameHubTrack = activeTrack): MediaPlayer? {
        mediaPlayer?.let { return it }

        val player = try {
            createPlayer(context, track.assetName)
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing player for ${track.assetName}: ${e.message}")
            return null
        }

        currentVolume = 0.0f
        mediaPlayer = player
        return player
    }

    private fun createPlayer(context: Context, assetName: String): MediaPlayer {
        val player = MediaPlayer()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes)
        player.isLooping = true

        var dataSourceSet = false
        try {
            val afd = context.assets.openFd(assetName)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            dataSourceSet = true
        } catch (e: Exception) {
            Log.d(TAG, "openFd not supported or asset compressed, falling back to cache file: ${e.message}")
        }

        if (!dataSourceSet) {
            val cacheFile = File(context.cacheDir, assetName)
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            player.setDataSource(cacheFile.absolutePath)
        }

        player.prepare()
        player.setVolume(0.0f, 0.0f)
        return player
    }

    @Synchronized
    private fun resetPlayer() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) { }
        mediaPlayer = null
        currentVolume = 0.0f
    }
}

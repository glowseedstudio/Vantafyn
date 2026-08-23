package dev.vantafyn.core.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log

object VantafynSoundEffects {
    private const val TAG = "VantafynAudio"
    private const val RESPECTFUL_VOLUME = 0.65f // Calm, premium, respectful volume level
    private const val PREFS_NAME = "vantafyn_sound_effects"
    private const val KEY_ENABLED = "sound_effects_enabled"

    fun isSoundEffectsEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun setSoundEffectsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private var soundPool: SoundPool? = null
    private val soundIdMap = mutableMapOf<Int, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()

    private val mediaAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    @Synchronized
    fun preload(context: Context) {
        ensureInitialized(context.applicationContext)
    }

    @Synchronized
    private fun ensureInitialized(context: Context): SoundPool {
        soundPool?.let { return it }

        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(mediaAttributes)
            .build()

        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }

        // Eagerly preload all raw sound assets
        val rawSounds = listOf(
            R.raw.new_message,
            R.raw.friend_request,
            R.raw.achievement_unlocked,
            R.raw.message_sent,
        )
        for (resId in rawSounds) {
            try {
                val sId = pool.load(context, resId, 1)
                soundIdMap[resId] = sId
            } catch (_: Exception) {}
        }

        soundPool = pool
        return pool
    }

    private fun playRawSound(context: Context, rawResId: Int, volume: Float = RESPECTFUL_VOLUME) {
        val appContext = context.applicationContext
        if (!isSoundEffectsEnabled(appContext)) return
        try {
            val pool = ensureInitialized(appContext)
            val soundId = soundIdMap.getOrPut(rawResId) {
                pool.load(appContext, rawResId, 1)
            }

            if (soundId in loadedSoundIds) {
                val streamId = pool.play(soundId, volume, volume, 1, 0, 1.0f)
                if (streamId == 0) {
                    // Fallback to MediaPlayer if SoundPool stream allocation fails (common on Samsung power-saving or mute policies)
                    playWithMediaPlayer(appContext, rawResId, volume)
                }
            } else {
                // Not yet loaded in SoundPool - play immediately via MediaPlayer so no audio is dropped
                playWithMediaPlayer(appContext, rawResId, volume)
            }
        } catch (e: Exception) {
            Log.w(TAG, "SoundPool play error, falling back to MediaPlayer: ${e.message}")
            playWithMediaPlayer(appContext, rawResId, volume)
        }
    }

    private fun playWithMediaPlayer(context: Context, rawResId: Int, volume: Float) {
        try {
            val mp = MediaPlayer.create(context, rawResId, mediaAttributes, AudioManager.AUDIOFOCUS_NONE) ?: return
            mp.setVolume(volume, volume)
            mp.setOnCompletionListener { player ->
                try {
                    player.stop()
                    player.release()
                } catch (_: Exception) {}
            }
            mp.start()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play sound with MediaPlayer: ${e.message}")
        }
    }

    fun playNewMessageAlert(context: Context) {
        playRawSound(context, R.raw.new_message, volume = 0.65f)
    }

    fun playFriendRequestAlert(context: Context) {
        playRawSound(context, R.raw.friend_request, volume = 0.70f)
    }

    fun playAchievementUnlocked(context: Context) {
        playRawSound(context, R.raw.achievement_unlocked, volume = 0.75f)
    }

    fun playMessageSent(context: Context) {
        playRawSound(context, R.raw.message_sent, volume = 0.60f)
    }
}

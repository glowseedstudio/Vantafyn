package dev.vantafyn.core.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

object VantafynSoundEffects {
    private const val TAG = "VantafynAudio"
    private const val RESPECTFUL_VOLUME = 0.65f // Calm, premium, respectful volume level

    private var soundPool: SoundPool? = null
    private val soundIdMap = mutableMapOf<Int, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()

    @Synchronized
    private fun ensureInitialized(context: Context): SoundPool {
        soundPool?.let { return it }

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }

        soundPool = pool
        return pool
    }

    private fun playRawSound(context: Context, rawResId: Int, volume: Float = RESPECTFUL_VOLUME) {
        runCatching {
            val pool = ensureInitialized(context.applicationContext)
            val soundId = soundIdMap.getOrPut(rawResId) {
                pool.load(context.applicationContext, rawResId, 1)
            }

            if (soundId in loadedSoundIds) {
                pool.play(soundId, volume, volume, 1, 0, 1.0f)
            } else {
                // In case it was just loaded, schedule or play with a short delay
                pool.postDelayedPlay(soundId, volume)
            }
        }.onFailure {
            Log.w(TAG, "Failed to play sound effect: ${it.message}")
        }
    }

    private fun SoundPool.postDelayedPlay(soundId: Int, volume: Float) {
        // Retry shortly once loaded
        val thread = Thread {
            for (i in 0 until 5) {
                Thread.sleep(80)
                if (soundId in loadedSoundIds) {
                    play(soundId, volume, volume, 1, 0, 1.0f)
                    break
                }
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    fun playNewMessageAlert(context: Context) {
        playRawSound(context, R.raw.new_message, volume = 0.60f)
    }

    fun playFriendRequestAlert(context: Context) {
        playRawSound(context, R.raw.friend_request, volume = 0.65f)
    }

    fun playAchievementUnlocked(context: Context) {
        playRawSound(context, R.raw.achievement_unlocked, volume = 0.70f)
    }

    fun playMessageSent(context: Context) {
        playRawSound(context, R.raw.message_sent, volume = 0.55f)
    }
}

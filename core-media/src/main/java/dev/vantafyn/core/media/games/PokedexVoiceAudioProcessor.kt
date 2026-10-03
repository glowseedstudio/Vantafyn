package dev.vantafyn.core.media.games

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A restrained synthetic device treatment for Pokédex narration.
 *
 * It mixes a low-frequency ring modulation with light 12-bit quantisation. This retains
 * intelligibility while removing the plain audiobook character of a raw TTS narrator.
 */
@OptIn(UnstableApi::class)
class PokedexVoiceAudioProcessor : BaseAudioProcessor() {
    private var phase = 0.0
    private var phaseStep = 0.0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding !in setOf(C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT)) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        phaseStep = (2.0 * PI * 74.0) / inputAudioFormat.sampleRate
        return inputAudioFormat
    }

    override fun isActive(): Boolean = inputAudioFormat.encoding != C.ENCODING_INVALID

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> while (inputBuffer.hasRemaining()) {
                output.putShort((treat(inputBuffer.short / 32768f) * 32767f).roundToInt().coerceIn(-32768, 32767).toShort())
            }
            C.ENCODING_PCM_FLOAT -> while (inputBuffer.hasRemaining()) output.putFloat(treat(inputBuffer.float))
        }
        output.flip()
    }

    private fun treat(sample: Float): Float {
        val carrier = sin(phase).toFloat()
        phase += phaseStep
        if (phase >= 2.0 * PI) phase -= 2.0 * PI
        // 62% clean speech preserves words; the remainder supplies the metallic voice.
        val modulated = sample * (0.62f + 0.38f * carrier)
        // 12-bit quantisation adds a subtle digital console texture without harsh clipping.
        return ((modulated * 2048f).roundToInt() / 2048f).coerceIn(-1f, 1f)
    }

    override fun onFlush() { phase = 0.0 }
    override fun onReset() { phase = 0.0 }
}

package dev.vantafyn.core.media.games

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * The single source of truth for the Pokédex voice treatment.
 *
 * The fundamental is deliberately untouched. The bright, narrow electronic character is made
 * with vocal-tract-style EQ and a quiet parallel digital bus, rather than a global pitch shift
 * or a ring modulator that would impose a fixed synthetic fundamental.
 */
object PokedexVoiceProcessingConfig {
    const val highPassHz = 120f
    const val lowMidCutHz = 285f
    const val lowMidCutDb = -2.5f
    const val presenceHz = 2_850f
    const val presenceDb = 5f
    const val articulationHz = 4_500f
    const val articulationDb = 2.5f
    const val highCutHz = 8_000f

    const val compressorThresholdDb = -18f
    const val compressorRatio = 3f
    const val compressorAttackMs = 6f
    const val compressorReleaseMs = 75f

    const val busHighPassHz = 300f
    const val busLowPassHz = 5_500f
    const val busWetMix = 0.12f
    const val busSaturationDrive = 1.45f
    const val busQuantisationSteps = 4_096f
    const val busDoubleDelayMs = 7f
}

/**
 * Restrained Pokédex narration treatment for decoded PCM.
 *
 * When [debugDirectory] is supplied, each run writes raw_kokoro.wav, eq_formant.wav,
 * electronic_bus.wav and final_pokedex.wav. It is optional so release builds do not retain
 * spoken lore in cache.
 */
@OptIn(UnstableApi::class)
class PokedexVoiceAudioProcessor(
    private val debugDirectory: File? = null,
) : BaseAudioProcessor() {
    private var channelCount = 0
    private var eqChains = emptyArray<EqChain>()
    private var busChains = emptyArray<BusChain>()
    private var delayLine = FloatArray(0)
    private var delayCursor = 0
    private var compressorEnvelope = 0f
    private var attackCoefficient = 0f
    private var releaseCoefficient = 0f
    private var debugWriters: DebugWriters? = null

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding !in setOf(C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT)) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        channelCount = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate.toFloat()
        eqChains = Array(channelCount) { EqChain(sampleRate) }
        busChains = Array(channelCount) { BusChain(sampleRate) }
        delayLine = FloatArray(max(1, (sampleRate * PokedexVoiceProcessingConfig.busDoubleDelayMs / 1_000f).roundToInt() * channelCount))
        attackCoefficient = coefficient(PokedexVoiceProcessingConfig.compressorAttackMs, sampleRate)
        releaseCoefficient = coefficient(PokedexVoiceProcessingConfig.compressorReleaseMs, sampleRate)
        resetState()
        closeDebugWriters()
        return inputAudioFormat
    }

    override fun isActive(): Boolean = inputAudioFormat.encoding != C.ENCODING_INVALID

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        ensureDebugWriters()
        val output = replaceOutputBuffer(inputBuffer.remaining())
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        val raw = FloatArray(channelCount)
        val eq = FloatArray(channelCount)

        while (inputBuffer.hasRemaining()) {
            for (channel in 0 until channelCount) {
                raw[channel] = when (inputAudioFormat.encoding) {
                    C.ENCODING_PCM_16BIT -> inputBuffer.short / 32768f
                    else -> inputBuffer.float.coerceIn(-1f, 1f)
                }
                eq[channel] = eqChains[channel].process(raw[channel])
            }
            applyCompression(eq)
            for (channel in 0 until channelCount) {
                val bus = busChains[channel].process(eq[channel])
                val delayedBus = delayLine[delayCursor]
                delayLine[delayCursor] = bus
                delayCursor = (delayCursor + 1) % delayLine.size
                val doubledBus = bus * 0.7f + delayedBus * 0.3f
                val finalSample = (eq[channel] * (1f - PokedexVoiceProcessingConfig.busWetMix) +
                    doubledBus * PokedexVoiceProcessingConfig.busWetMix).coerceIn(-1f, 1f)
                writeSample(output, finalSample)
                debugWriters?.write(raw[channel], eq[channel], bus, finalSample)
            }
        }
        output.flip()
    }

    override fun onQueueEndOfStream() {
        closeDebugWriters()
    }

    override fun onFlush() { closeDebugWriters(); resetState() }
    override fun onReset() { closeDebugWriters(); resetState() }

    private fun applyCompression(samples: FloatArray) {
        val peak = samples.maxOf(::abs)
        val coefficient = if (peak > compressorEnvelope) attackCoefficient else releaseCoefficient
        compressorEnvelope = coefficient * compressorEnvelope + (1f - coefficient) * peak
        val threshold = dbToAmplitude(PokedexVoiceProcessingConfig.compressorThresholdDb)
        val gain = if (compressorEnvelope <= threshold || compressorEnvelope <= 0f) 1f else {
            val compressed = threshold * (compressorEnvelope / threshold).pow(1f / PokedexVoiceProcessingConfig.compressorRatio)
            compressed / compressorEnvelope
        }
        for (index in samples.indices) samples[index] = (samples[index] * gain).coerceIn(-1f, 1f)
    }

    private fun ensureDebugWriters() {
        if (debugWriters != null || debugDirectory == null) return
        debugWriters = debugDirectory?.let { directory ->
            runCatching { DebugWriters(directory, inputAudioFormat.sampleRate, channelCount) }.getOrNull()
        }
    }

    private fun closeDebugWriters() { debugWriters?.close(); debugWriters = null }

    private fun resetState() {
        delayLine.fill(0f)
        delayCursor = 0
        compressorEnvelope = 0f
        eqChains.forEach(EqChain::reset)
        busChains.forEach(BusChain::reset)
    }

    private fun writeSample(output: ByteBuffer, sample: Float) {
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> output.putShort((sample * 32767f).roundToInt().coerceIn(-32768, 32767).toShort())
            else -> output.putFloat(sample)
        }
    }

    private class EqChain(sampleRate: Float) {
        private val filters = listOf(
            Biquad.highPass(sampleRate, PokedexVoiceProcessingConfig.highPassHz, 0.707f),
            Biquad.peaking(sampleRate, PokedexVoiceProcessingConfig.lowMidCutHz, 0.9f, PokedexVoiceProcessingConfig.lowMidCutDb),
            Biquad.peaking(sampleRate, PokedexVoiceProcessingConfig.presenceHz, 0.9f, PokedexVoiceProcessingConfig.presenceDb),
            Biquad.peaking(sampleRate, PokedexVoiceProcessingConfig.articulationHz, 0.9f, PokedexVoiceProcessingConfig.articulationDb),
            Biquad.lowPass(sampleRate, PokedexVoiceProcessingConfig.highCutHz, 0.707f),
        )
        fun process(sample: Float) = filters.fold(sample) { value, filter -> filter.process(value) }
        fun reset() = filters.forEach(Biquad::reset)
    }

    private class BusChain(sampleRate: Float) {
        private val highPass = Biquad.highPass(sampleRate, PokedexVoiceProcessingConfig.busHighPassHz, 0.707f)
        private val lowPass = Biquad.lowPass(sampleRate, PokedexVoiceProcessingConfig.busLowPassHz, 0.707f)
        private val saturationNormaliser = tanh(PokedexVoiceProcessingConfig.busSaturationDrive)

        fun process(sample: Float): Float {
            val bandLimited = lowPass.process(highPass.process(sample))
            val saturated = tanh(bandLimited * PokedexVoiceProcessingConfig.busSaturationDrive) / saturationNormaliser
            return (saturated * PokedexVoiceProcessingConfig.busQuantisationSteps).roundToInt() /
                PokedexVoiceProcessingConfig.busQuantisationSteps
        }

        fun reset() { highPass.reset(); lowPass.reset() }
    }

    private class Biquad private constructor(
        private val b0: Float, private val b1: Float, private val b2: Float, private val a1: Float, private val a2: Float,
    ) {
        private var x1 = 0f; private var x2 = 0f; private var y1 = 0f; private var y2 = 0f
        fun process(input: Float): Float {
            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = input; y2 = y1; y1 = output
            return output
        }
        fun reset() { x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f }

        companion object {
            fun highPass(sampleRate: Float, frequency: Float, q: Float): Biquad {
                val omega = 2.0 * PI * frequency / sampleRate; val alpha = sin(omega) / (2.0 * q); val cosine = cos(omega)
                return normalized((1.0 + cosine) / 2.0, -(1.0 + cosine), (1.0 + cosine) / 2.0, 1.0 + alpha, -2.0 * cosine, 1.0 - alpha)
            }
            fun lowPass(sampleRate: Float, frequency: Float, q: Float): Biquad {
                val omega = 2.0 * PI * frequency / sampleRate; val alpha = sin(omega) / (2.0 * q); val cosine = cos(omega)
                return normalized((1.0 - cosine) / 2.0, 1.0 - cosine, (1.0 - cosine) / 2.0, 1.0 + alpha, -2.0 * cosine, 1.0 - alpha)
            }
            fun peaking(sampleRate: Float, frequency: Float, q: Float, gainDb: Float): Biquad {
                val omega = 2.0 * PI * frequency / sampleRate; val alpha = sin(omega) / (2.0 * q)
                val amplitude = 10.0.pow(gainDb / 40.0); val cosine = cos(omega)
                return normalized(1.0 + alpha * amplitude, -2.0 * cosine, 1.0 - alpha * amplitude, 1.0 + alpha / amplitude, -2.0 * cosine, 1.0 - alpha / amplitude)
            }
            private fun normalized(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) = Biquad(
                (b0 / a0).toFloat(), (b1 / a0).toFloat(), (b2 / a0).toFloat(), (a1 / a0).toFloat(), (a2 / a0).toFloat(),
            )
        }
    }

    private class DebugWriters(directory: File, sampleRate: Int, channels: Int) : AutoCloseable {
        private val raw: WavWriter; private val eq: WavWriter; private val bus: WavWriter; private val final: WavWriter
        init {
            check(directory.exists() || directory.mkdirs()) { "Could not create Pokédex debug directory" }
            raw = WavWriter(File(directory, "raw_kokoro.wav"), sampleRate, channels)
            eq = WavWriter(File(directory, "eq_formant.wav"), sampleRate, channels)
            bus = WavWriter(File(directory, "electronic_bus.wav"), sampleRate, channels)
            final = WavWriter(File(directory, "final_pokedex.wav"), sampleRate, channels)
        }
        fun write(rawSample: Float, eqSample: Float, busSample: Float, finalSample: Float) {
            raw.write(rawSample); eq.write(eqSample); bus.write(busSample); final.write(finalSample)
        }
        override fun close() { raw.close(); eq.close(); bus.close(); final.close() }
    }

    private class WavWriter(path: File, private val sampleRate: Int, private val channels: Int) : AutoCloseable {
        private val file = RandomAccessFile(path, "rw")
        private var dataBytes = 0L
        init { file.setLength(0); file.write(ByteArray(44)) }
        fun write(sample: Float) {
            val value = (sample.coerceIn(-1f, 1f) * 32767f).roundToInt().coerceIn(-32768, 32767)
            file.write(value and 0xff); file.write((value ushr 8) and 0xff); dataBytes += 2
        }
        override fun close() {
            file.seek(0)
            ascii("RIFF"); intLE((36 + dataBytes).toInt()); ascii("WAVEfmt "); intLE(16); shortLE(1); shortLE(channels)
            intLE(sampleRate); intLE(sampleRate * channels * 2); shortLE(channels * 2); shortLE(16); ascii("data"); intLE(dataBytes.toInt())
            file.close()
        }
        private fun ascii(value: String) = file.write(value.toByteArray(Charsets.US_ASCII))
        private fun intLE(value: Int) { file.write(value and 0xff); file.write((value ushr 8) and 0xff); file.write((value ushr 16) and 0xff); file.write((value ushr 24) and 0xff) }
        private fun shortLE(value: Int) { file.write(value and 0xff); file.write((value ushr 8) and 0xff) }
    }

    private companion object {
        fun coefficient(milliseconds: Float, sampleRate: Float) = kotlin.math.exp(-1f / (milliseconds * sampleRate / 1_000f))
        fun dbToAmplitude(db: Float) = 10f.pow(db / 20f)
    }
}

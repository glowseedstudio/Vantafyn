package dev.vantafyn.core.media.games

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.math.roundToInt
import kotlin.math.sin

class PokedexVoiceAudioProcessorTest {

    private val format = AudioProcessor.AudioFormat(44_100, 1, C.ENCODING_PCM_16BIT)

    @Test
    fun silenceStaysSilentWithoutAnInjectedOscillator() {
        val processor = PokedexVoiceAudioProcessor()
        processor.configure(format)
        processor.flush()
        val input = ByteBuffer.allocateDirect(2_048).order(ByteOrder.LITTLE_ENDIAN)
        repeat(1_024) { input.putShort(0) }
        input.flip()

        processor.queueInput(input)
        val output = processor.output.order(ByteOrder.LITTLE_ENDIAN)

        while (output.hasRemaining()) assertEquals(0, output.short.toInt())
    }

    @Test
    fun debugCaptureWritesEveryPipelineStageAsWav() {
        val directory = Files.createTempDirectory("pokedex-voice-test").toFile()
        val processor = PokedexVoiceAudioProcessor(directory)
        processor.configure(format)
        processor.flush()
        val input = ByteBuffer.allocateDirect(4_410).order(ByteOrder.LITTLE_ENDIAN)
        repeat(2_205) { index ->
            input.putShort((sin(index * 0.08) * 8_000).roundToInt().toShort())
        }
        input.flip()

        processor.queueInput(input)
        processor.queueEndOfStream()

        listOf("raw_kokoro.wav", "eq_formant.wav", "electronic_bus.wav", "final_pokedex.wav").forEach { name ->
            val file = directory.resolve(name)
            assertTrue("$name was not written", file.isFile && file.length() > 44)
            assertEquals("RIFF", file.inputStream().use { inputStream ->
                ByteArray(4).also(inputStream::read).toString(Charsets.US_ASCII)
            })
        }
    }
}

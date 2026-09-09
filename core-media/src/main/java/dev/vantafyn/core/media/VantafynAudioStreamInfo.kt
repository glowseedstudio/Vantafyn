package dev.vantafyn.core.media

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import java.util.Locale

/**
 * Encapsulates the active audio stream playback metadata (codec, container, sample rate, bit depth,
 * bitrate, channels, and lossless/hi-res classification) for audiophile-grade display.
 */
data class VantafynAudioStreamInfo(
    val codec: String? = null,
    val container: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channelCount: Int? = null,
    val isLossless: Boolean = false,
    val isHiRes: Boolean = false,
    val replayGainAppliedDb: Float? = null,
    val isDirectPlay: Boolean = true,
) {
    /**
     * E.g. "HI-RES LOSSLESS", "LOSSLESS", "HQ" or null.
     */
    val displayQualityBadge: String?
        get() = when {
            isHiRes -> "HI-RES LOSSLESS"
            isLossless -> "LOSSLESS"
            bitrateKbps != null && bitrateKbps >= 256 -> "HQ"
            else -> null
        }

    /**
     * E.g. "FLAC · 24-bit / 96 kHz · 1411 kbps" or "AAC · 320 kbps".
     */
    val displaySummary: String
        get() {
            val parts = mutableListOf<String>()
            val c = (codec ?: container)?.uppercase(Locale.ROOT)
            if (!c.isNullOrBlank()) parts.add(c)
            if (bitDepth != null && bitDepth > 0 && isLossless) parts.add("${bitDepth}-bit")
            if (sampleRateHz != null && sampleRateHz > 0) {
                val khz = sampleRateHz / 1000.0
                val formattedKhz = if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format(Locale.ROOT, "%.1f kHz", khz)
                parts.add(formattedKhz)
            }
            if (bitrateKbps != null && bitrateKbps > 0) parts.add("$bitrateKbps kbps")
            return parts.joinToString(" · ")
        }

    /**
     * Compact label for the now-playing chip, e.g. "FLAC · 96 kHz" or "AAC · 320 kbps".
     */
    val displayCompact: String
        get() {
            val c = (codec ?: container)?.uppercase(Locale.ROOT) ?: "AUDIO"
            val rate = if (sampleRateHz != null && sampleRateHz > 0) {
                val khz = sampleRateHz / 1000.0
                if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format(Locale.ROOT, "%.1f kHz", khz)
            } else null
            val br = if (bitrateKbps != null && bitrateKbps > 0 && rate == null) "${bitrateKbps} kbps" else null
            return listOfNotNull(c, rate, br).joinToString(" · ")
        }

    companion object {
        fun fromTrackAndFormat(
            track: VantafynMusicTrack?,
            format: Format?,
            replayGainAppliedDb: Float? = null,
        ): VantafynAudioStreamInfo? {
            if (track == null && format == null) return null

            // 1. Codec extraction
            val formatMime = format?.sampleMimeType?.lowercase(Locale.ROOT)
            val detectedCodec = when {
                formatMime == null -> null
                formatMime == MimeTypes.AUDIO_FLAC || formatMime.contains("flac") -> "FLAC"
                formatMime == MimeTypes.AUDIO_ALAC || formatMime.contains("alac") -> "ALAC"
                formatMime == MimeTypes.AUDIO_AAC || formatMime.contains("mp4a") || formatMime.contains("aac") -> "AAC"
                formatMime == MimeTypes.AUDIO_MPEG || formatMime.contains("mpeg") || formatMime.contains("mp3") -> "MP3"
                formatMime == MimeTypes.AUDIO_OPUS || formatMime.contains("opus") -> "Opus"
                formatMime == MimeTypes.AUDIO_VORBIS || formatMime.contains("vorbis") -> "Vorbis"
                formatMime == MimeTypes.AUDIO_RAW -> "PCM"
                formatMime == MimeTypes.AUDIO_WAV || formatMime.contains("wav") -> "WAV"
                else -> formatMime.substringAfterLast("audio/").substringAfterLast('-').uppercase(Locale.ROOT)
            } ?: track?.codec?.uppercase(Locale.ROOT) ?: track?.container?.uppercase(Locale.ROOT)

            // 2. Container
            val detectedContainer = track?.container?.uppercase(Locale.ROOT)
                ?: format?.containerMimeType?.substringAfterLast('/')?.uppercase(Locale.ROOT)

            // 3. Sample Rate
            val sampleRateHz = format?.sampleRate?.takeIf { it > 0 }
                ?: track?.sampleRate?.takeIf { it > 0 }

            // 4. Bitrate
            val bitrateKbps = when {
                format?.bitrate != null && format.bitrate > 0 -> (format.bitrate / 1000)
                track?.bitrate != null && track.bitrate > 0 -> {
                    if (track.bitrate > 5000) track.bitrate / 1000 else track.bitrate
                }
                else -> null
            }

            // 5. Channels
            val channelCount = format?.channelCount?.takeIf { it > 0 }
                ?: track?.channels?.takeIf { it > 0 }
                ?: 2

            // 6. Lossless & Hi-Res classification
            val lowerCodec = (detectedCodec ?: "").lowercase(Locale.ROOT)
            val lowerContainer = (detectedContainer ?: "").lowercase(Locale.ROOT)
            val isLossless = lowerCodec in setOf("flac", "alac", "wav", "pcm", "dsd", "dsf", "dff", "ape", "wv") ||
                lowerContainer in setOf("flac", "alac", "wav", "aiff", "dsd") ||
                (formatMime != null && (formatMime.contains("flac") || formatMime.contains("alac") || formatMime.contains("wav") || formatMime.contains("raw")))

            // 7. Bit Depth
            val detectedBitDepth = track?.bitDepth?.takeIf { it > 0 }
                ?: format?.let { f ->
                    when (f.pcmEncoding) {
                        C.ENCODING_PCM_16BIT -> 16
                        C.ENCODING_PCM_24BIT -> 24
                        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
                        else -> if (isLossless && sampleRateHz != null && bitrateKbps != null && channelCount > 0) {
                            val bps = bitrateKbps * 1000L
                            val calculatedDepth = (bps / (sampleRateHz.toLong() * channelCount)).toInt()
                            if (calculatedDepth in 16..32) calculatedDepth else null
                        } else null
                    }
                }

            val isHiRes = isLossless && ((sampleRateHz ?: 0) > 48000 || (detectedBitDepth ?: 16) > 16)

            return VantafynAudioStreamInfo(
                codec = detectedCodec,
                container = detectedContainer,
                bitrateKbps = bitrateKbps,
                sampleRateHz = sampleRateHz,
                bitDepth = detectedBitDepth,
                channelCount = channelCount,
                isLossless = isLossless,
                isHiRes = isHiRes,
                replayGainAppliedDb = replayGainAppliedDb,
                isDirectPlay = true,
            )
        }
    }
}


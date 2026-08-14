package dev.vantafyn.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

object VantafynExoPlayerFactory {
    @OptIn(UnstableApi::class)
    fun renderersFactory(context: Context): DefaultRenderersFactory =
        DefaultRenderersFactory(context.applicationContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

    fun builder(context: Context): ExoPlayer.Builder =
        ExoPlayer.Builder(context.applicationContext, renderersFactory(context))

    fun builder(context: Context, trackSelector: DefaultTrackSelector): ExoPlayer.Builder =
        ExoPlayer.Builder(context.applicationContext, renderersFactory(context))
            .setTrackSelector(trackSelector)
}

object VantafynMedia3ExtensionSupport {
    val decoders: List<VantafynExtensionDecoder> = listOf(
        VantafynExtensionDecoder(
            label = "FFmpeg audio",
            summary = "Extra audio codecs when the Media3 FFmpeg extension is bundled.",
            classNames = listOf("androidx.media3.decoder.ffmpeg.FfmpegLibrary"),
        ),
        VantafynExtensionDecoder(
            label = "libass subtitles",
            summary = "Advanced ASS/SSA subtitle styling when a compatible Media3 libass extension is bundled.",
            classNames = listOf(
                "androidx.media3.decoder.libass.LibassLibrary",
                "androidx.media3.decoder.subtitle.libass.LibassLibrary",
            ),
        ),
        VantafynExtensionDecoder(
            label = "FLAC audio",
            summary = "Native FLAC extension support when bundled.",
            classNames = listOf("androidx.media3.decoder.flac.FlacLibrary"),
        ),
        VantafynExtensionDecoder(
            label = "Opus audio",
            summary = "Native Opus extension support when bundled.",
            classNames = listOf("androidx.media3.decoder.opus.OpusLibrary"),
        ),
        VantafynExtensionDecoder(
            label = "AV1 video",
            summary = "Software AV1 fallback when bundled.",
            classNames = listOf("androidx.media3.decoder.av1.Gav1Library"),
        ),
    )

    val availableDecoders: List<VantafynExtensionDecoder>
        get() = decoders.filter { it.isAvailable }
}

data class VantafynExtensionDecoder(
    val label: String,
    val summary: String,
    private val classNames: List<String>,
) {
    val isAvailable: Boolean
        get() = classNames.any(::classExists)
}

private fun classExists(name: String): Boolean =
    runCatching { Class.forName(name) }.isSuccess

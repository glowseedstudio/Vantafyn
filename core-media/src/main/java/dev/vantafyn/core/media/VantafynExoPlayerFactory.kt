package dev.vantafyn.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

object VantafynExoPlayerFactory {
    @OptIn(UnstableApi::class)
    fun renderersFactory(context: Context): DefaultRenderersFactory =
        DefaultRenderersFactory(context.applicationContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableAudioTrackPlaybackParams(true)

    @OptIn(UnstableApi::class)
    fun musicLoadControl(): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 120_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000,
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 30_000,
                /* retainBackBufferFromKeyframe = */ false,
            )
            .build()

    @OptIn(UnstableApi::class)
    fun videoLoadControl(): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000,
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 10_000,
                /* retainBackBufferFromKeyframe = */ false,
            )
            .build()

    @OptIn(UnstableApi::class)
    fun musicRenderersFactory(
        context: Context,
        audioProcessors: Array<androidx.media3.common.audio.AudioProcessor> = emptyArray(),
        onStreamDiscontinuity: (() -> Unit)? = null,
    ): DefaultRenderersFactory =
        object : DefaultRenderersFactory(context.applicationContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOffload: Boolean,
            ): androidx.media3.exoplayer.audio.AudioSink? {
                val defaultSink = androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(true)
                    .setAudioProcessors(audioProcessors)
                    .build()
                return if (onStreamDiscontinuity != null) {
                    object : androidx.media3.exoplayer.audio.ForwardingAudioSink(defaultSink) {
                        override fun handleDiscontinuity() {
                            super.handleDiscontinuity()
                            onStreamDiscontinuity.invoke()
                        }
                    }
                } else {
                    defaultSink
                }
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableAudioTrackPlaybackParams(true)
        }

    @OptIn(UnstableApi::class)
    fun musicBuilder(
        context: Context,
        audioProcessors: Array<androidx.media3.common.audio.AudioProcessor> = emptyArray(),
        onStreamDiscontinuity: (() -> Unit)? = null,
    ): ExoPlayer.Builder {
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
            VantafynMediaCache.getCacheDataSourceFactory(context),
        )
        return ExoPlayer.Builder(
            context.applicationContext,
            musicRenderersFactory(context, audioProcessors, onStreamDiscontinuity),
        )
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(musicLoadControl())
    }

    @OptIn(UnstableApi::class)
    fun builder(context: Context): ExoPlayer.Builder =
        ExoPlayer.Builder(context.applicationContext, renderersFactory(context))
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    androidx.media3.datasource.DefaultDataSource.Factory(
                        context.applicationContext,
                        VantafynMediaCache.getHttpDataSourceFactory(),
                    ),
                ),
            )
            .setLoadControl(videoLoadControl())

    @OptIn(UnstableApi::class)
    fun builder(context: Context, trackSelector: DefaultTrackSelector): ExoPlayer.Builder =
        ExoPlayer.Builder(context.applicationContext, renderersFactory(context))
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    androidx.media3.datasource.DefaultDataSource.Factory(
                        context.applicationContext,
                        VantafynMediaCache.getHttpDataSourceFactory(),
                    ),
                ),
            )
            .setTrackSelector(trackSelector)
            .setLoadControl(videoLoadControl())
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

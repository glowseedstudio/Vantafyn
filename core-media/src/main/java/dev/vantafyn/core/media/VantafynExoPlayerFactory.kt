package dev.vantafyn.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import android.os.Build
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.extractor.DefaultExtractorsFactory
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.factory.AssRenderersFactory
import io.github.peerless2012.ass.media.kt.withAssMkvSupport
import io.github.peerless2012.ass.media.parser.AssSubtitleParserFactory
import io.github.peerless2012.ass.media.type.AssRenderType

object VantafynExoPlayerFactory {
    @OptIn(UnstableApi::class)
    fun createExtractorsFactory(): DefaultExtractorsFactory =
        DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)

    @OptIn(UnstableApi::class)
    fun renderersFactory(context: Context): DefaultRenderersFactory =
        object : DefaultRenderersFactory(context.applicationContext) {
            @OptIn(ExperimentalApi::class)
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>,
            ) {
                var videoRendererBuilder =
                    MediaCodecVideoRenderer.Builder(context)
                        .setCodecAdapterFactory(codecAdapterFactory)
                        .setMediaCodecSelector(mediaCodecSelector)
                        .setAllowedJoiningTimeMs(allowedVideoJoiningTimeMs)
                        .setEnableDecoderFallback(enableDecoderFallback)
                        .setEventHandler(eventHandler)
                        .setEventListener(eventListener)
                        .setMaxDroppedFramesToNotify(MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY)
                        .experimentalSetParseAv1SampleDependencies(false)
                        .experimentalSetLateThresholdToDropDecoderInputUs(C.TIME_UNSET)

                if (Build.VERSION.SDK_INT >= 34) {
                    videoRendererBuilder =
                        videoRendererBuilder.experimentalSetEnableMediaCodecBufferDecodeOnlyFlag(false)
                }
                out.add(videoRendererBuilder.build())

                if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
                    return
                }
                var extensionRendererIndex = out.size
                if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
                    extensionRendererIndex--
                }

                try {
                    val clazz = Class.forName("androidx.media3.decoder.av1.Libdav1dVideoRenderer")
                    val constructor = clazz.getConstructor(
                        Long::class.javaPrimitiveType,
                        Handler::class.java,
                        VideoRendererEventListener::class.java,
                        Int::class.javaPrimitiveType,
                    )
                    val renderer = constructor.newInstance(
                        allowedVideoJoiningTimeMs,
                        eventHandler,
                        eventListener,
                        MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY,
                    ) as Renderer
                    out.add(extensionRendererIndex, renderer)
                } catch (_: Throwable) {
                    // AV1 optional extension not bundled
                }
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableAudioTrackPlaybackParams(true)
            setEnableDecoderFallback(true)
        }

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
            setEnableDecoderFallback(true)
        }

    @OptIn(UnstableApi::class)
    fun createTrackSelector(
        context: Context,
        disableAudioOffload: Boolean = false,
    ): DefaultTrackSelector =
        DefaultTrackSelector(context.applicationContext).apply {
            val offloadMode = if (disableAudioOffload) {
                AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
            } else {
                AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
            }
            setParameters(
                buildUponParameters()
                    .setAudioOffloadPreferences(
                        AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(offloadMode)
                            .build(),
                    ),
            )
        }

    @OptIn(UnstableApi::class)
    fun musicBuilder(
        context: Context,
        audioProcessors: Array<androidx.media3.common.audio.AudioProcessor> = emptyArray(),
        onStreamDiscontinuity: (() -> Unit)? = null,
    ): ExoPlayer.Builder {
        val mediaSourceFactory = DefaultMediaSourceFactory(
            VantafynMediaCache.getCacheDataSourceFactory(context),
            createExtractorsFactory(),
        )
        return ExoPlayer.Builder(
            context.applicationContext,
            musicRenderersFactory(context, audioProcessors, onStreamDiscontinuity),
        )
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(musicLoadControl())
    }

    @OptIn(UnstableApi::class)
    fun createVideoPlayer(
        context: Context,
        trackSelector: DefaultTrackSelector = createTrackSelector(context),
    ): VantafynVideoPlayerInstance {
        val assSetup = runCatching {
            val renderType = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                AssRenderType.OVERLAY_CANVAS
            } else {
                AssRenderType.OVERLAY_OPEN_GL
            }
            val handler = AssHandler(renderType)
            val parserFactory = AssSubtitleParserFactory(handler)
            val renderers = AssRenderersFactory(handler, renderersFactory(context))
            val extractorsFactory = createExtractorsFactory()
            val mkvExtractors = extractorsFactory.withAssMkvSupport(
                parserFactory,
                handler,
            )
            val mediaSourceFactory = DefaultMediaSourceFactory(
                DefaultDataSource.Factory(
                    context.applicationContext,
                    VantafynMediaCache.getHttpDataSourceFactory(),
                ),
                mkvExtractors,
            ).setSubtitleParserFactory(parserFactory)
            Triple(handler, renderers, mediaSourceFactory)
        }.getOrNull()

        return if (assSetup != null) {
            val (handler, renderers, sourceFactory) = assSetup
            val player = ExoPlayer.Builder(context.applicationContext, renderers)
                .setMediaSourceFactory(sourceFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(videoLoadControl())
                .build()
            handler.init(player)
            VantafynVideoPlayerInstance(player, handler)
        } else {
            val player = builder(context, trackSelector).build()
            VantafynVideoPlayerInstance(player, null)
        }
    }

    @OptIn(UnstableApi::class)
    fun builder(context: Context): ExoPlayer.Builder =
        ExoPlayer.Builder(context.applicationContext, renderersFactory(context))
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    DefaultDataSource.Factory(
                        context.applicationContext,
                        VantafynMediaCache.getHttpDataSourceFactory(),
                    ),
                    createExtractorsFactory(),
                ),
            )
            .setTrackSelector(createTrackSelector(context))
            .setLoadControl(videoLoadControl())

    @OptIn(UnstableApi::class)
    fun builder(context: Context, trackSelector: DefaultTrackSelector): ExoPlayer.Builder =
        ExoPlayer.Builder(context.applicationContext, renderersFactory(context))
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    DefaultDataSource.Factory(
                        context.applicationContext,
                        VantafynMediaCache.getHttpDataSourceFactory(),
                    ),
                    createExtractorsFactory(),
                ),
            )
            .setTrackSelector(trackSelector)
            .setLoadControl(videoLoadControl())
}

data class VantafynVideoPlayerInstance(
    val player: ExoPlayer,
    val assHandler: io.github.peerless2012.ass.media.AssHandler? = null,
) {
    fun release() {
        assHandler?.release()
        player.release()
    }
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
            summary = "Advanced ASS/SSA subtitle styling with hardware overlay rendering.",
            classNames = listOf(
                "io.github.peerless2012.ass.media.AssHandler",
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

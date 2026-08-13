package dev.vantafyn.core.cast

import com.google.android.gms.cast.MediaTrack
import dev.vantafyn.core.media.VantafynAudioTrack
import dev.vantafyn.core.media.VantafynSubtitleTrack

data class CastSubtitleTrack(
    val castTrackId: Long,
    val streamIndex: Int,
    val label: String,
    val language: String?,
    val codec: String?,
    val isExternal: Boolean,
    val isDefault: Boolean,
    val contentUrl: String,
    val contentType: String,
)

data class CastAudioTrack(
    val streamIndex: Int,
    val label: String,
    val language: String?,
    val codec: String?,
    val channels: Int?,
    val isDefault: Boolean,
)

data class CastTrackSupportResult(
    val subtitles: List<CastSubtitleTrack>,
    val audioTracks: List<CastAudioTrack>,
    val audioSwitchingSupported: Boolean = false,
) {
    val hasSupportedSubtitles: Boolean
        get() = subtitles.isNotEmpty()
}

object CastTrackMapper {
    fun map(
        subtitleTracks: List<VantafynSubtitleTrack>,
        audioTracks: List<VantafynAudioTrack>,
    ): CastTrackSupportResult =
        CastTrackSupportResult(
            subtitles = subtitleTracks
                .mapNotNull { it.toCastSubtitleTrack() }
                .distinctBy { it.castTrackId },
            audioTracks = audioTracks.map {
                CastAudioTrack(
                    streamIndex = it.index,
                    label = it.label,
                    language = it.language,
                    codec = it.codec,
                    channels = it.channels,
                    isDefault = it.isDefault,
                )
            },
            audioSwitchingSupported = false,
        )

    fun toMediaTrack(track: CastSubtitleTrack): MediaTrack =
        MediaTrack.Builder(track.castTrackId, MediaTrack.TYPE_TEXT)
            .setName(track.label)
            .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
            .setContentId(track.contentUrl)
            .setContentType(track.contentType)
            .apply { track.language?.takeIf { it.isNotBlank() }?.let(::setLanguage) }
            .build()

    private fun VantafynSubtitleTrack.toCastSubtitleTrack(): CastSubtitleTrack? {
        val url = deliveryUrl?.takeIf { CastUrlSecurity.isCastReachableServerAddress(it) } ?: return null
        val type = castSubtitleContentType(codec, url) ?: return null
        return CastSubtitleTrack(
            castTrackId = index.toLong().coerceAtLeast(1L),
            streamIndex = index,
            label = label.ifBlank { language?.uppercase() ?: "Subtitle $index" },
            language = language,
            codec = codec,
            isExternal = isExternal,
            isDefault = isDefault,
            contentUrl = url,
            contentType = type,
        )
    }

    private fun castSubtitleContentType(codec: String?, url: String): String? {
        val normalizedCodec = codec.orEmpty().lowercase()
        val path = url.substringBefore('?').lowercase()
        return when {
            normalizedCodec in setOf("webvtt", "vtt") || path.endsWith(".vtt") -> "text/vtt"
            normalizedCodec in setOf("ttml", "dfxp") ||
                path.endsWith(".ttml") ||
                path.endsWith(".dfxp") ||
                path.endsWith(".xml") -> "application/ttml+xml"
            normalizedCodec in setOf("cea608", "cea-608", "eia_608", "eia-608") -> "text/cea-608"
            else -> null
        }
    }
}

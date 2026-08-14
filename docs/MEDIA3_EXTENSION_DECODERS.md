# Media3 Extension Decoders

Vantafyn uses AndroidX Media3 ExoPlayer for local video playback, theme audio, and music service playback.

## Current Integration

- `core-media` exposes `VantafynExoPlayerFactory`.
- The factory enables Media3 extension renderers with `EXTENSION_RENDERER_MODE_ON`.
- Video playback, theme music, and the foreground music service now use this shared factory.
- Device/platform decoders remain first priority. Extension decoders are used as a fallback when Android cannot handle a format cleanly.

This keeps playback battery-friendly on normal streams while allowing bundled extension decoders to help with custom formats.

## Optional Extension Libraries

Media3 extension decoders such as FFmpeg and advanced subtitle renderers depend on native libraries being bundled with the app. Some Media3 extensions are not simple Maven dependencies and may need to be built from the AndroidX Media source tree or provided as prebuilt AARs.

Vantafyn now detects common extension libraries at runtime and reports their status in Playback Preferences:

- FFmpeg audio
- libass subtitles
- FLAC audio
- Opus audio
- AV1 video

If an extension is not bundled, Vantafyn still works normally with Android's platform decoders and Jellyfin transcoding/direct-play decisions.

## Subtitles

ASS/SSA external subtitle tracks are mapped to Media3's `TEXT_SSA` MIME type. Basic SSA/ASS parsing works through Media3 where supported. Full libass-quality styling requires a compatible libass extension to be bundled and detected at runtime.

## Server Compatibility

Most users should not need to change anything on their server. Jellyfin can still direct play supported formats and transcode unsupported formats. These client-side extension hooks improve direct playback only when:

- the server exposes a compatible direct stream, and
- the extension decoder is included in the app build, and
- the device can sustain the decode cost.

## Build Notes

No native extension AARs are currently committed into this repository. To add one later, include the AAR through Gradle and keep it in `core-media` so all playback surfaces share the same capability.

# Cast Track Support Audit

## Current Vantafyn Implementation

Cast sender support lives in `core-cast` and uses the Google Cast Default Media Receiver.

Before this pass, Vantafyn built Cast `MediaInfo` with stream URL, content type, artwork, metadata, duration, and Jellyfin item custom data, but did not attach receiver media tracks. The mobile player also had local Media3 audio/subtitle sheets only, so Cast playback had no real subtitle switching path.

This pass adds:

- `CastTrackMapper`
- `CastSubtitleTrack`
- `CastAudioTrack`
- `CastTrackSupportResult`
- Cast subtitle metadata on `RemoteQueueItem`
- Cast subtitle state on `RemotePlaybackState`
- `RemotePlaybackTarget.selectSubtitleTrack(trackId)`
- `RemoteMediaClient.setActiveMediaTracks(...)` subtitle switching
- a Cast-specific subtitle sheet in the fullscreen mobile Cast controller

## Default Media Receiver Findings

Google's Android sender media track documentation says `MediaTrack` can describe audio, video, or text, but the Styled Media Receiver and Default Media Receiver currently support only text tracks through this API. Audio and video track switching require a custom receiver.

Google's Android `RemoteMediaClient` API provides `setActiveMediaTracks(long[])`. Passing an empty array removes active tracks, which Vantafyn uses for Subtitle Off.

Google's Cast supported media documentation lists subtitle/closed-caption support for WebVTT, TTML, and CEA-608/708. It also states subtitle resources must support CORS. Vantafyn can verify URL shape/reachability class, but cannot fully prove receiver-side CORS from the phone before loading.

Sources:

- https://developers.google.com/cast/docs/android_sender/media_tracks
- https://developers.google.com/android/reference/com/google/android/gms/cast/framework/media/RemoteMediaClient
- https://developers.google.com/cast/docs/media

## Mapping Rules

Vantafyn maps Jellyfin subtitle tracks only when:

- Jellyfin provides a delivery URL.
- The delivery URL is `http` or `https` and not a phone-only address like `localhost`, `127.0.0.1`, or emulator loopback.
- The codec or URL extension maps to Cast-supported text: WebVTT, TTML/DFXP, or CEA-608.

Vantafyn does not expose unsupported subtitle formats as Cast options. Local ExoPlayer subtitle support remains broader and unchanged.

## Audio Limitation

Cast audio tracks are mapped internally for diagnostics/future custom receiver work, but `audioSwitchingSupported` is false for the Default Media Receiver. The mobile Cast UI does not expose an audio sheet while casting.

To support reliable Cast audio track switching later, Vantafyn should add a custom receiver or a proven Jellyfin transcode reload flow that preserves position and creates a new stream with the selected audio stream.

## Risks And Limitations

- Receiver-side CORS failures can still happen even when a URL is syntactically Cast-reachable.
- Embedded subtitle streams are not exposed unless Jellyfin provides a receiver-loadable text delivery URL.
- SRT/ASS are not exposed to Cast unless Jellyfin returns a WebVTT/TTML delivery URL.
- Audio switching while casting is intentionally hidden with the Default Media Receiver.

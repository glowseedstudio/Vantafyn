# Cast Playback Negotiation

Cast playback negotiation is owned by `core-jellyfin`.

## Repository API

`JellyfinPlaybackRepository` exposes two separate flows:

- `getPlaybackInfo(...)` for local Android playback.
- `getCastPlaybackInfo(...)` for Google Cast playback.

Both return `JellyfinPlaybackInfo`, but they use different Jellyfin device profiles.

## Google Cast Device Profile

`googleCastDeviceProfile()` is intentionally conservative:

- Direct video containers: `mp4`, `m4v`, `webm`
- Direct video codecs: `h264`, `vp8`, `vp9`
- Direct audio codecs: `aac`, `mp3`, `ac3`, `eac3`, `opus`, `vorbis`
- Transcode fallback: HLS MPEG-TS with `h264` video and `aac/ac3/mp3` audio
- Max streaming bitrate: `20 Mbps`
- Max audio channels: `6`

Local Android playback continues to use `androidMobileDeviceProfile()` and is not degraded by Cast constraints.

## Handoff Flow

When the user connects Cast while a movie or episode is open:

1. The mobile player captures the current ExoPlayer position.
2. The local player pauses.
3. The player asks `VantafynHomeViewModel.prepareCastPlayback(positionMs)`.
4. The ViewModel reports local playback stopped at the handoff position.
5. `core-jellyfin` requests Cast-specific playback info from Jellyfin.
6. The active `VantafynPlaybackItem` is replaced with a Cast-resolved source.
7. The player loads that source through `PlaybackOutputCoordinator.loadVideo(...)`.
8. The fullscreen player switches into Cast controller mode.

## Supported Content

Current safe mobile video Cast support:

- Movies
- Episodes

Not exposed yet:

- Live TV
- trailers/extras
- audio switching while casting with the Default Media Receiver

Cast subtitle switching is exposed only for receiver-supported text tracks with Cast-reachable Jellyfin delivery URLs. Unsupported subtitles stay hidden from the Cast subtitle sheet.

If Cast load fails, Vantafyn keeps local playback available and shows a concise Cast error.

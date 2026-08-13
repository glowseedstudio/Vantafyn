# Cast Implementation Audit

Vantafyn uses the Google Cast Default Media Receiver through `core-cast`. There is no custom receiver in this pass.

## Current Architecture

- `VantafynCastOptionsProvider` registers the Default Media Receiver application id.
- `GoogleCastRouteButton` hosts the Google Cast route button.
- `GoogleCastPlaybackTarget` owns Cast session callbacks, `RemoteMediaClient`, `MediaInfo` creation, queue load, play/pause, seek, and disconnect.
- `PlaybackOutputCoordinator` is the app-level output bridge for local output and Google Cast.
- Mobile video player observes `PlaybackOutputCoordinator.state` and switches to a Cast controller surface when the active item is loaded remotely.

## What Works

- Cast session discovery and connection are delegated to the official Google Cast framework.
- Music queue casting continues to use the existing queue path.
- Movies and episodes can now hand off to Cast from the mobile video player.
- Casted video uses Jellyfin Cast playback negotiation before loading the receiver.
- The local ExoPlayer is paused during handoff so local audio/video does not continue underneath Cast.
- The fullscreen player becomes a Vantafyn Cast controller with play/pause, seek, stop casting, and play-on-device actions.
- Remote state is synced from `RemoteMediaClient` and used for position/progress display.

## Default Media Receiver Behavior

The Default Media Receiver can load URL-based media with metadata and artwork. It does not give Vantafyn a custom protocol surface for rich Jellyfin-specific receiver commands. This pass keeps that receiver and uses conservative sender-side negotiation instead of adding a custom receiver.

## Incomplete Or Limited

- Cast subtitle/audio track switching is not exposed in UI yet because the Default Media Receiver path is not wired to reliable Jellyfin track switching. Local subtitle/audio selection remains unchanged.
- "Play on this device" resumes local playback from the remote position using the current player item; a deeper local re-resolve after Cast disconnect remains future hardening.
- Live TV Cast is not exposed from the video handoff path yet; it needs live-stream receiver validation on real devices.
- Artwork passed to Cast is limited by the current `VantafynPlaybackItem` fields. Detail/backdrop enrichment should be added later.

No URLs, tokens, auth headers, or signed stream strings are logged.

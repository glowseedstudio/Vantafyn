# Wholphin Playback Comparison

Scope: conceptual reference only. No Wholphin source was copied into Vantafyn.

## What Wholphin Appears To Do

- Requests Jellyfin playback info with `MediaInfoApi.getPostedPlaybackInfo(...)` and a client device profile.
- Uses `PlaybackInfoDto` with direct play/direct stream/transcoding flags and `autoOpenLiveStream = true`.
- Carries `mediaSourceId`, `playSessionId`, and `liveStreamId` through playback state.
- Reports playback with the full Jellyfin DTO APIs:
  - `reportPlaybackStart(PlaybackStartInfo)`
  - `reportPlaybackProgress(PlaybackProgressInfo)`
  - `reportPlaybackStopped(PlaybackStopInfo)`
- Has dedicated player state models and listener-style lifecycle reporting.
- Exposes debug-style playback state including play session and live stream identifiers.
- Treats audio/subtitle stream selection as playback state that can cause stream rebuilds.
- Uses a richer device profile and more detailed subtitle compatibility handling than Vantafyn currently does.

## What Vantafyn Currently Does

- Requests playback info in `core-jellyfin` through `MediaInfoApi.getPostedPlaybackInfo(...)`.
- Prefers Direct Play, then Direct Stream/HLS/transcode when Jellyfin exposes it.
- Uses `PlaybackStartInfo`, `PlaybackProgressInfo`, and `PlaybackStopInfo`.
- Carries `mediaSourceId`, `playSessionId`, `liveStreamId`, audio index, subtitle index, and Live TV state in `JellyfinPlaybackInfo`.
- Uses `autoOpenLiveStream = true` first for Live TV.
- Falls back to explicit `MediaInfoApi.openLiveStream(...)` for Live TV when playback-info auto-open fails.
- Calls `closeLiveStream(liveStreamId)` on stop/error/retry when Jellyfin supplies a live stream id.
- Keeps playback SDK work out of UI modules.
- Adds debug-safe Android logs under `VantafynPlayback`; tokens and full stream URLs are not logged.

## Differences That Matter

- Wholphin appears to have a more mature player state/debug layer and deeper subtitle handling.
- Vantafyn’s track switching currently rebuilds the Jellyfin playback stream at the current position rather than applying Media3 in-place track overrides.
- Vantafyn’s Live TV explicit fallback is intentionally narrow: it only runs after the current playback-info auto-open path fails.
- Vantafyn’s Android device profile is conservative and mobile-first.

## Improvements Applied

- Added explicit Live TV `openLiveStream(...)` fallback.
- Kept `closeLiveStream(...)` tied to active playback stop/error/retry paths.
- Preserved play/session/live stream ids through reporting.
- Added selected track state to the mobile player sheet.
- Fixed Subtitle Off so it requests no subtitle stream instead of falling back to Jellyfin default.

## Confirmation

No Wholphin source files were copied into Vantafyn.

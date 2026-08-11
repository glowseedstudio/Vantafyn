# Video Player Audit

## Current Architecture

- `core-jellyfin` prepares playback with `MediaInfoApi.getPostedPlaybackInfo(...)`, maps Jellyfin media sources, stream URLs, audio streams, subtitle streams, play session IDs, and live stream IDs into Vantafyn models.
- `feature-home` owns playback navigation and Jellyfin reporting. It starts playback, retries with transcode fallback, reports start/progress/stop, and maps playback info into `VantafynPlaybackItem`.
- `feature-player` owns the mobile fullscreen Media3 ExoPlayer surface.
- `core-media` owns shared playback models and Up Next models.

## Problems Found

- The mobile controls mixed default Material buttons, text buttons, and Vantafyn gradient buttons, which made the player look inconsistent and too colourful.
- The player created `MediaItem.fromUri(...)`, so external Jellyfin subtitles were never attached to Media3.
- The `deliveryUrl` supplied by Jellyfin subtitle metadata was dropped during mapping into `core-media`.
- Audio/subtitle selection restarted playback through the Jellyfin playback-info flow even when Media3 could switch the active track in-place.
- The bottom sheet used plain Material styling and dumped sparse track rows without premium hierarchy, badges, or clean empty states.
- Quality/source was only a retry/transcode fallback; there was no safe full quality selector to expose yet.
- Orientation changes could recreate or resize the player around a stale `startPositionMs`, making playback appear to jump back roughly to the previous periodic Jellyfin report.

## Repairs Made

- Rebuilt mobile controls as a dark cinematic overlay with calm glass icon buttons, white icons, a single restrained gradient play/pause control, fade in/out behavior, and a subtle buffering state.
- Added a premium More sheet with subtitles, audio, playback speed, retry, transcode fallback, watch-from-beginning, and stop actions.
- Preserved Jellyfin external subtitle delivery URLs into `VantafynSubtitleTrack`.
- Built `MediaItem` with Media3 `SubtitleConfiguration` entries for external subtitle URLs when Jellyfin exposes them.
- Added in-place Media3 audio/text track switching with `TrackSelectionOverride`; subtitle Off disables the text track type.
- Updated Vantafyn playback state on track changes so Jellyfin reporting uses the currently selected audio/subtitle stream indexes.
- Updated local playback position state before network progress reporting and let `MobileMainActivity` handle orientation/screen-size changes so rotation does not restart from stale state.
- Added real Media3 screen scaling choices: Fit, Fill, Zoom, Fixed Width, and Fixed Height.
- Kept solo Up Next behavior suppressed for Watch Party playback.

## Remaining Limitations

- Full bitrate/resolution quality selection is not exposed because the app does not yet maintain a complete selectable media-source ladder.
- Previous episode is not shown because the current shared playback model only carries the next candidate.
- Some subtitle formats still depend on Android/Media3 support or Jellyfin burn-in/transcoding behavior.
- Embedded track matching depends on Media3 exposing compatible track group metadata; external subtitles use the Jellyfin delivery URLs where available.

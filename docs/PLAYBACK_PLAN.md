# Playback Plan

Playback is intentionally not implemented yet. This plan prepares the next milestone.

## Jellyfin Endpoints

Use Jellyfin SDK MediaInfo/PlayState APIs:

- request playback info for an item through MediaInfo playback-info endpoints
- inspect returned `MediaSourceInfo` entries
- choose a direct-play compatible source first
- fall back to direct stream/transcode only when required
- report playback start/progress/stop through PlayState APIs

## Media Source Strategy

1. Fetch full media detail and playback info for the selected item.
2. Select a playable `MediaSourceInfo`.
3. Prefer direct play for mobile when container/codecs are supported by Media3.
4. If direct play is unsuitable, request Jellyfin stream URLs using the server-provided transcoding/direct-stream info.
5. Keep the selected media source and play session id in `core-media`.

## Media3 Integration

`core-media` should own:

- player/session models
- Media3 `MediaItem` construction
- headers/access token handling
- audio/subtitle track labels
- lifecycle-safe progress reporting

`feature-player` should own:

- mobile player UI
- loading/error overlays
- track picker UI
- skip/seek controls

## Audio And Subtitle Tracks

Use Jellyfin playback info media streams plus saved `UserConfiguration`:

- preferred audio language
- preferred subtitle language
- subtitle mode
- remembered selections where Jellyfin exposes them

Media3 track selection should be applied after preparing the source.

## Reporting

Use PlayState APIs:

- report start once playback starts
- report progress on an interval and major seek/pause events
- report stopped with final position

Do not log access tokens.

## TV Later

Mobile playback comes first. TV playback will need D-pad controls, focus-safe overlays, larger timeline controls, and refresh-rate/resolution handling later.


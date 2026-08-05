# Playback Implementation

## Implemented

- Mobile playback is wired from movie/episode detail Play/Resume into a fullscreen Media3 ExoPlayer screen.
- `core-jellyfin` owns Jellyfin playback API access through `JellyfinPlaybackRepository`.
- `feature-player` owns the Media3 player surface, custom dark controls, progress UI, error overlay, and basic track sheets.
- `feature-home` owns navigation and coordinates playback lifecycle reporting through the repository.
- TV playback is not implemented yet, but `app-tv` still builds.

## Jellyfin APIs Used

- `MediaInfoApi.getPostedPlaybackInfo(itemId, PlaybackInfoDto)` for playback info, media sources, play session id, stream metadata, and transcoding URLs.
- `PlayStateApi.onPlaybackStart(...)` for playback start reporting.
- `PlayStateApi.onPlaybackProgress(...)` for periodic progress, pause, and seek reporting.
- `PlayStateApi.onPlaybackStopped(...)` for exit/stop reporting.

## Direct Play Strategy

Vantafyn sends an Android mobile device profile with common ExoPlayer-compatible direct-play containers/codecs:

- Video containers: `mp4`, `m4v`, `mov`, `mkv`, `webm`
- Video codecs: `h264`, `hevc`, `vp8`, `vp9`, `av1`, `mpeg4`
- Audio codecs: `aac`, `mp3`, `ac3`, `eac3`, `opus`, `vorbis`, `flac`

When Jellyfin marks a media source as direct-play capable, Vantafyn builds a direct `/Videos/{itemId}/stream` URL and passes it to Media3.

## Transcoding Fallback

The playback profile allows HLS video transcoding to H.264/AAC. If Jellyfin returns a transcoding URL, Vantafyn stores it as a fallback. If ExoPlayer errors during direct playback, the app retries once with transcoding enabled.

## Resume and Reporting

Detail data now carries Jellyfin `playbackPositionTicks`. Resume uses those ticks when the item is partially watched and not already marked played.

Progress is reported every seven seconds during playback and also on pause/seek. Stop is reported when the player is closed. Jellyfin remains responsible for deciding watched thresholds.

## Tracks

Playback info exposes audio and subtitle streams as Vantafyn-owned models. The mobile player shows Audio and Subs sheets when tracks are available. Selecting a track re-requests playback info with the selected stream index.

Current limitation: Media3 in-stream track override is not yet implemented for already-opened direct streams. Track changes restart playback through the Jellyfin playback-info flow.

## Live TV

The same playback-info path is prepared for Live TV item ids, but Live TV-specific open/close live-stream lifecycle is not complete in this pass. If a Live TV item cannot play through regular playback info, it needs explicit `openLiveStream` / `closeLiveStream` handling next.

## Known Limitations

- TV playback UI remains TODO.
- External subtitle sidecar attachment handling is limited to URLs Jellyfin exposes in playback info.
- Subtitle formats that require burn-in depend on Jellyfin transcoding.
- Quality/source selection beyond direct vs transcode is not yet a full UI.
- Track changes restart playback rather than switching tracks in-place.

# Playback Implementation

## Implemented

- Mobile playback is wired from movie/episode detail Play/Resume into a fullscreen Media3 ExoPlayer screen.
- `core-jellyfin` owns Jellyfin playback API access through `JellyfinPlaybackRepository`.
- `feature-player` owns the Media3 player surface, premium dark controls, progress UI, error overlay, track sheets, playback speed sheet, screen fit/zoom controls, and safe source retry actions.
- `feature-home` owns navigation and coordinates playback lifecycle reporting through the repository.
- Mobile episode playback now supports a premium Up Next overlay with countdown autoplay.
- Mobile Watch Party can create a real Jellyfin SyncPlay group and start a matched item by setting the SyncPlay queue before entering the existing player route.
- Fixed-title Watch Party starts also send the selected item to the SyncPlay queue before entering the existing player route.
- Watch Party playback suppresses solo Up Next countdown/autoplay to avoid independent participant advancement.
- Watch Party invite receive is now app-wide on mobile while Vantafyn is open/connected. Accepting an invite stops music through the existing video policy before joining the SyncPlay lobby.
- Live TV channel/program taps now enter the same mobile player path.
- Live TV now has an explicit `openLiveStream(...)` fallback after playback-info auto-open failures.
- TV playback is not implemented yet, but `app-tv` still builds.

## Jellyfin APIs Used

- `MediaInfoApi.getPostedPlaybackInfo(itemId, PlaybackInfoDto)` for playback info, media sources, play session id, stream metadata, and transcoding URLs.
- `PlayStateApi.reportPlaybackStart(PlaybackStartInfo)` for playback start reporting, including position ticks.
- `PlayStateApi.reportPlaybackProgress(PlaybackProgressInfo)` for periodic progress, pause, and seek reporting.
- `PlayStateApi.reportPlaybackStopped(PlaybackStopInfo)` for exit/completion reporting.
- `UserLibraryApi.getItem(userId, itemId)` for current and candidate episode metadata.
- `TvShowsApi.getEpisodes(GetEpisodesRequest)` for same-series next episode lookup.
- `SyncPlayApi.syncPlayCreateGroup(...)`, `syncPlayGetGroups(...)`, `syncPlayJoinGroup(...)`, `syncPlayLeaveGroup(...)`, `syncPlayPause(...)`, `syncPlayUnpause(...)`, `syncPlaySeek(...)`, and `syncPlaySetNewQueue(...)` for Watch Party foundations.
- `SessionApi.getSessions(...)` and `sendMessageCommand(...)` for active-session Watch Party invite delivery.
- `MediaInfoApi.closeLiveStream(liveStreamId)` when Jellyfin marks the stream as live and returns a live stream id.
- `MediaInfoApi.openLiveStream(...)` as the explicit Live TV fallback when playback-info auto-open does not provide a playable stream.

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

Watch from beginning is explicit: detail actions call playback with `startPositionTicks = 0`, even if Jellyfin has saved resume progress. This does not erase the existing resume point by itself; Jellyfin is updated only through normal playback progress/stop reporting after playback starts.

Series playback chooses an episode target rather than trying to play the series container. The primary action uses the first unfinished loaded episode. The Watch from beginning action uses the first valid episode from the selected season when available.

Progress is reported every seven seconds during playback and also on pause/seek/background. Stop is reported when the player is closed or reaches completion. Jellyfin remains responsible for deciding watched thresholds.

Progress also updates Vantafyn's local `playbackItem`, active playback target, and playback-info start ticks before the network report is sent. That prevents orientation changes or Activity recreation from reopening the player at an older server-reported position.

`app-mobile` handles orientation/screen-size configuration changes on `MobileMainActivity`, so rotation resizes the active player surface instead of tearing down playback.

When playback closes, the detail item and home libraries are refreshed so Continue Watching/resume state can update from Jellyfin.

For Up Next, the next episode transition calls back into the same ViewModel playback startup path. The current episode is reported stopped with the current player position before playback info is requested for the next episode. The next episode then reports playback start from the normal `STATE_READY` callback. A local guard prevents countdown completion and player completion from starting the next item twice.

If next episode startup fails, the existing playback error overlay is used with retry/transcode options where available.

## Tracks

Playback info exposes audio and subtitle streams as Vantafyn-owned models. The mobile player shows Audio and Subtitle sheets when tracks are available.

External subtitle delivery URLs from Jellyfin are preserved into `VantafynSubtitleTrack` and attached to the Media3 `MediaItem` as subtitle configurations. Supported external formats are mapped to Media3 MIME types where possible.

Selecting audio/subtitles now applies an in-place Media3 track override with `TrackSelectionOverride`. Subtitle Off clears text overrides and disables text tracks. The ViewModel updates the selected Jellyfin stream indexes without restarting playback so playback reporting stays aligned with the current selection.

## Screen Fit

The mobile More sheet exposes real Media3 `PlayerView.resizeMode` options:

- Fit
- Fill
- Zoom
- Fixed Width
- Fixed Height

These options change how the active video surface is scaled. They do not request a new Jellyfin source and do not restart playback.

## Live TV

Live TV library, guide rows, home Live TV rows, and program taps now attempt playback using the channel id through `MediaInfoApi.getPostedPlaybackInfo(...)` with `autoOpenLiveStream = true`.

If Jellyfin returns a `liveStreamId`, Vantafyn marks the item as live, reports playback with the live stream id, disables seek semantics in reporting, and calls `closeLiveStream(liveStreamId)` on stop.

If playback-info auto-open fails for Live TV, Vantafyn calls `MediaInfoApi.openLiveStream(...)` with the Android mobile device profile and builds playback from the returned media source. If that source includes `liveStreamId`, stop/error/retry closes it.

Retries and track changes report stop on the previous active session before opening the new stream, which avoids duplicate Live TV sessions where Jellyfin provides closeable stream ids.

## Diagnostics

Debug-safe logs use the `VantafynPlayback` tag:

```bash
adb logcat -s VantafynPlayback
```

Logs include playback method, whether media/play/live stream ids are present, selected audio/subtitle indexes, and safe error class/message. Tokens and full signed URLs are not logged.

## Known Limitations

- TV playback UI remains TODO.
- Music playback is service-owned through Media3 and now exposes Android notification, lock-screen, and Android Auto browse/control paths. Video playback still uses the existing mobile fullscreen player path.
- TV Up Next UI remains TODO, though the shared model and Jellyfin lookup are reusable.
- Up Next lookup currently uses ordered same-series episodes instead of a dedicated server-side adjacent-episode endpoint.
- If autoplay is disabled, Vantafyn finishes normally instead of showing a non-countdown next episode prompt.
- External subtitle sidecar attachment handling is limited to URLs Jellyfin exposes in playback info.
- Subtitle formats that require burn-in depend on Jellyfin transcoding.
- Quality/source selection beyond direct vs transcode is not yet a full UI. The More sheet exposes retry/transcode only when a real fallback is available.
- Pre-playback media version selection is visible in Media Info but not yet selectable as a playback override.
- Explicit Live TV fallback is implemented, but Live TV behavior still depends on server tuner/provider support.
- Watch Party now subscribes to Jellyfin websocket/session events for connection, session presence, SyncPlay commands, and group updates. Per-member exact sync, ready, and buffering status are still not claimed unless Jellyfin exposes reliable state.

# Playback Test Matrix

Use this matrix before moving deeper into Android TV playback.

## Debug-Safe Logs

```bash
adb logcat -s VantafynPlayback
```

Logs include playback method, whether media/play/live stream IDs are present, selected audio/subtitle indexes, and safe error class/message. Logs do not include access tokens or full signed URLs.

## Basic Playback

- Movie starts from detail.
- Episode starts from detail.
- Resume starts near Jellyfin `playbackPositionTicks`.
- Watch from beginning starts at zero.
- Live TV starts through the existing channel playback path.
- Direct Play starts when Jellyfin allows it.
- Direct Play failure can retry with HLS/transcoding where Jellyfin provides a fallback.
- Playback completion reports stop and Jellyfin marks watched naturally.

## Mobile Controls

- Tap video toggles controls.
- Controls auto-hide while playing.
- Paused state keeps controls visible.
- Back icon reports stop/progress and returns to Vantafyn.
- Play/pause works.
- Back 10 seconds works.
- Forward 10 seconds works.
- Scrubber seeks.
- Buffering shows the Vantafyn loading state.
- Buttons use restrained white/glass styling with accent only for active/primary states.
- Rotating the phone keeps the current playback position instead of jumping back.

## Subtitles

- Subtitle button appears when Jellyfin exposes subtitle tracks.
- Subtitle sheet shows Off.
- Embedded subtitle tracks are listed with language/codec/default indicators where available.
- External subtitle tracks are attached through Media3 subtitle configurations when Jellyfin exposes a delivery URL.
- External subtitles without delivery URLs are shown as unavailable rather than as broken actions.
- Selecting a subtitle applies a Media3 text track override without restarting playback.
- Off disables text tracks.
- Current subtitle row is checked.

## Audio

- Single audio track hides the audio button.
- Multiple audio tracks appear in the audio sheet.
- Audio rows show language, codec, and channel information.
- Default audio is marked.
- Selecting an audio track applies a Media3 audio track override without restarting playback.
- Current audio row is checked.

## Source And Speed

- More sheet shows playback speed choices.
- Speed changes apply immediately for the active session.
- More sheet shows screen fit choices.
- Fit, Fill, Zoom, Fixed Width, and Fixed Height change Media3 surface scaling without restarting playback.
- Retry playback reloads through the existing ViewModel path.
- Try transcoding appears only when a fallback/source retry is available.
- Full bitrate/resolution selection is not exposed until Vantafyn has a real source ladder.

## Google Cast

- Cast button appears when a Cast device is available.
- Cast session connects and disconnects cleanly.
- Starting Cast from an open movie pauses local playback.
- Starting Cast from an open episode pauses local playback.
- Cast playback starts near the current local position.
- Chromecast receives movie/episode metadata rather than music metadata.
- Cast controller shows `Playing on <device>` when the receiver name is available.
- Cast play/pause controls the receiver.
- Cast scrubber seek controls the receiver.
- Cast subtitle button appears only when supported Cast subtitle tracks exist.
- Cast subtitle sheet shows Off and supported text tracks.
- Cast subtitle selection changes the receiver track through Cast, not local ExoPlayer.
- Cast audio sheet is hidden with the Default Media Receiver.
- Stop casting stops the receiver and returns to Vantafyn.
- Play on this device resumes local playback near the Cast position.
- Jellyfin dashboard/activity shows Cast playback after handoff.
- Cast progress updates Jellyfin from the remote position.
- Subtitle/audio options are not shown as Cast controls until actually supported.
- Local movie and episode playback still work after disconnecting Cast.

## Episodes And Watch Party

- Up Next appears for eligible episodes.
- Next episode control appears only when a next candidate exists.
- Final episodes do not show a broken next action.
- Watch Party playback suppresses solo Up Next/autoplay to avoid desync.

## Live TV

- Live TV library opens to channels.
- Guide/current program appears if Jellyfin exposes it.
- Channel tap starts playback.
- Program tap starts channel playback when a `channelId` is available.
- Playback-info `autoOpenLiveStream` path works where supported.
- Explicit `openLiveStream(...)` fallback works when auto-open playback info fails.
- Stop/back/completion/error closes `liveStreamId` when supplied.

## Regression

- Music playback still works.
- Music system notification remains service-owned.
- Ombi Requests still works.
- My List/Favorites still work.
- Home, Libraries, Details, Search, Live TV, and Admin still compile.
- `app-mobile` and `app-tv` build.

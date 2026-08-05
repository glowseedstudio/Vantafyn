# Playback Test Matrix

Use this matrix before moving to Android TV playback.

## Debug-Safe Logs

Filter:

```bash
adb logcat -s VantafynPlayback
```

Logs include playback method, whether media/play/live stream ids are present, stream type, selected audio/subtitle indexes, and safe error class/message. Logs do not include access tokens or full signed URLs.

## Movies

- Direct Play 1080p H.264/AAC starts.
- Direct Play 4K HEVC starts if the device supports it.
- Old/unsupported codec falls back to HLS/transcode.
- Resume from middle starts near Jellyfin `playbackPositionTicks`.
- Playback to completion reports stop and Jellyfin marks watched naturally.

## Episodes

- Episode Play starts playback.
- Episode Resume starts near saved position.
- Series primary action starts next unwatched/first available episode.
- Returning from player refreshes detail/home progress.

## Subtitles

- No subtitles: subtitle button is hidden.
- Internal subtitles appear in the subtitle sheet.
- External subtitles appear when Jellyfin exposes them in playback info.
- Subtitles Off sends subtitle stream index `-1`.
- Switching subtitles restarts playback at current position.

## Audio

- Single audio track: audio button is hidden.
- Multiple audio tracks appear in the audio sheet.
- Selected audio track is marked.
- Switching audio restarts playback at current position.

## Live TV

- Live TV library opens to channels.
- Guide/current program appears if Jellyfin exposes it.
- Channel tap starts playback.
- Program tap starts channel playback when a `channelId` is available.
- Playback-info `autoOpenLiveStream` path works where supported.
- Explicit `openLiveStream(...)` fallback works when auto-open playback info fails.
- Stop/back/completion/error closes `liveStreamId` when supplied.
- Retry after failure does not leave stale Live TV sessions.
- No-guide state shows channels without fake guide data.

## Network

- Local IP server URL playback works.
- Domain server URL playback works.
- Temporary network failure shows a playback error instead of crashing.

## Admin

- Active playback session appears as `Vantafyn Mobile` when Jellyfin exposes it.
- Stopped playback clears/updates the active session after Jellyfin receives stop.

## Build

```bash
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk ./gradlew :app-tv:assembleDebug :app-mobile:assembleDebug
```

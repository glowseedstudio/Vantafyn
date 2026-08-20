# Music Background Battery Audit

## Problem
Battery stats showed heavy drain during screen-off music playback:
- 6h46m playback used 3.1% battery
- CPU: 5h16m (77% of total time)
- Wi-Fi: 1,016,374 packets (~250 packets/second)
- Wake locks: 6h43m (matches playback time)

## Root Causes Found

### 1. Notification Updates via `startForeground()`
**File**: `VantafynMusicPlaybackService.kt`

Every state change (position update, play/pause) called `startForeground()` which is expensive — it triggers IPC to the system service, rebuilds the notification, and can cause unnecessary work.

**Fix**: After initial `startForeground()`, subsequent updates use `NotificationManager.notify()` directly.

### 2. Jellyfin Progress Reporting in Background
**File**: `MusicViewModel.kt`

Progress was reported every 10 seconds regardless of foreground/background state. In background, this creates unnecessary network traffic and CPU work.

**Fix**: 
- Foreground: 10s interval (unchanged)
- Background: 30s interval

### 3. Lyrics Prefetching While Backgrounded
**File**: `MusicViewModel.kt`

Lyrics were being prefetched even when the app was backgrounded, causing unnecessary Jellyfin API calls.

**Fix**: Added `AppForegroundStateRepository.isForeground` check to `shouldPrefetchLyrics()`.

## Changes Made

### VantafynMusicPlaybackService.kt
- Added `isForegroundService` flag to track initial foreground start
- Added `notificationManager` lazy property
- New `updateNotificationOnly()` method using `notify()` instead of `startForeground()`
- State collector now routes to `updateNotificationOnly()` after initial foreground
- Added debug lifecycle logs for service create/destroy, notification updates, artwork loading

### MusicPlaybackController.kt
- Added debug logs for ticker start/stop and `isPlaying` changes
- Ticker already had foreground/background awareness (1s vs 10s) — no changes needed

### MusicViewModel.kt
- Added `AppForegroundStateRepository` import
- `maybeReportTimedProgress()`: Uses 30s interval when backgrounded
- `reportProgress()`: Uses 30s interval when backgrounded
- `shouldPrefetchLyrics()`: Added foreground check
- Added `MusicBackgroundProgressReportIntervalMs = 30_000L` constant

## Debug Logs

All logs use `Log.d()` with tags:
- `MusicPlaybackService`: Service lifecycle, notification updates, artwork loading
- `MusicPlaybackController`: Ticker lifecycle, play/pause state changes

To view logs:
```bash
adb logcat -s MusicPlaybackService:D MusicPlaybackController:D
```

## Remaining Battery Consumers (Expected)

1. **ExoPlayer wake lock**: Held during playback for audio focus — necessary
2. **ExoPlayer audio decoding**: CPU work for MP3/AAC decoding — necessary
3. **Position ticker**: 10s background interval — minimal overhead
4. **Progress reporting**: 30s background interval — 3 HTTP POSTs/minute

## Testing

1. Play music for 30+ minutes with screen off
2. Check battery stats: `adb shell dumpsys batterystats`
3. Monitor logs: `adb logcat -s MusicPlaybackService:D MusicPlaybackController:D`
4. Verify notification still updates correctly
5. Verify lyrics don't load when screen is off

# Music Screen-Off Battery And Network Audit

## User-Reported Long Run

Samsung battery statistics from a screen-off music session:

- Background: 6h 46m
- Wake-ups: 0
- Wake locks: 6h 43m
- CPU: 5h 16m
- Wi-Fi: 1,016,374 packets
- Screen on: less than 1 minute

This is not a wake-up problem. The suspicious parts are the high reported CPU active time and the packet count during otherwise normal continuous audio playback.

## Device Snapshot Captured During This Pass

Connected phone:

- Serial: `R5GYB59M66D`
- Model: `SM_A266B`
- Package: `dev.vantafyn.mobile`
- Installed app: `versionCode=8`, `versionName=0.8.0`

The available ADB snapshot was not a clean 6-hour screen-off reproduction because the device had recent foreground UI activity and was charging. It is still useful for identifying active services and obvious hot loops.

Observed from `top -H -p <pid>`:

- No thread was actively hot in the instant sample.
- Threads present were expected for this app shape: `main`, `RenderThread`, `DefaultDispatch`, `OkHttp`, and ExoPlayer playback/background threads.

Observed from `dumpsys power`:

- No active Vantafyn-owned wake lock was visible in the snapshot.
- The music controller uses Media3/ExoPlayer network wake mode for reliable background streaming.
- The long wake-lock duration reported by Samsung is likely playback attribution during active streaming, not evidence of an extra manual Vantafyn wake lock.

Observed from recent `dumpsys batterystats dev.vantafyn.mobile`:

- Recent app Wi-Fi packets were visible, but the window included foreground use and was too short to compare with the 6h46m report.
- `OfflineUserDataSyncWorker` appeared once with a very short runtime and was not a major contributor in that sample.

## Root Causes Found

### 1. Widget Updates Were Driven By Playback Position Ticks

Files:

- `core-media/src/main/java/dev/vantafyn/core/media/VantafynMusicPlaybackService.kt`
- `app-mobile/src/main/java/dev/vantafyn/mobile/VantafynMusicWidget.kt`

The music service collected playback state and, while music was playing, persisted widget state and broadcast `ACTION_PLAYBACK_STATE_CHANGED` from position updates.

That broadcast wakes the widget receiver and can trigger a Glance widget update. The widget then loads artwork for the currently playing item. Before this pass, the artwork path could open the artwork URL directly during widget rendering.

This creates a plausible high-cost screen-off path:

`music position tick -> SharedPreferences write -> broadcast -> widget update -> artwork/network work`

That is exactly the kind of hidden background work that can inflate CPU active time and Wi-Fi packet counts without Android reporting wake-ups.

Fix:

- Widget state updates are now content-aware and throttled.
- Track/play-state/artwork/duration changes still update immediately.
- Foreground widget progress refresh is limited to 30 seconds.
- Background widget refresh is limited to 5 minutes.
- Background playback no longer forces progress-only widget refreshes.
- Widget preference writes now use async `apply()` instead of blocking `commit()`.
- Widget artwork is cached in app cache storage after the first load.

### 2. UI-Facing Playback State Was Updated While The App Was Backgrounded

File:

- `feature-music/src/main/java/dev/vantafyn/feature/music/MusicViewModel.kt`

`MusicViewModel` collected the service playback state for the lifetime of the ViewModel and copied every playback update into UI state. While playback itself must continue in the service, UI-only position changes should not continuously drive Compose-facing state when the app is backgrounded.

Fix:

- UI playback state is now published only when the app is foregrounded, the music screen is active, or popup lyrics are active.
- While backgrounded, the ViewModel still observes playback for Jellyfin reporting and lifecycle events, but it does not publish plain position ticks into UI state.
- Track, queue, play/pause, duration, and error changes can still update state so the UI reconnects cleanly when foregrounded.
- Entering the music screen immediately snapshots the current service state.

### 3. Jellyfin Music Progress Reporting Was Too Frequent For Screen-Off Playback

File:

- `feature-music/src/main/java/dev/vantafyn/feature/music/MusicViewModel.kt`

Progress reporting existed to keep Jellyfin state correct, but the background interval was still too eager for long screen-off music sessions.

Fix:

- Foreground music progress reporting remains 10 seconds.
- Background music progress reporting is now 60 seconds.
- Track start, stop, pause, seek, and transition events still report immediately.

This keeps Jellyfin reporting meaningful without turning screen-off audio into a constant API loop.

### 4. Diagnostic Long-Running Task Ticks Were Still Updating During Background Playback

File:

- `core-media/src/main/java/dev/vantafyn/core/media/MusicPlaybackController.kt`

The music ticker also updated `LongRunningTaskRegistry`. This is useful when foreground diagnostics are visible, but it does not need to tick every background playback position update.

Fix:

- Foreground diagnostic ticks remain responsive.
- Background diagnostic ticks are throttled to 60 seconds.
- The actual player position ticker remains service-owned and conservative.

## Network Traffic Sources To Separate In A Clean Run

Expected and required:

- Audio stream bytes from Jellyfin.
- Jellyfin playback start/stop/progress reports.
- Notification artwork fetches on track changes if artwork is not cached.

Should be rare or inactive during normal solo screen-off playback:

- Widget artwork/network refreshes.
- UI position-state publication.
- Lyrics prefetching.
- Watch Party / SyncPlay WebSocket work.
- Offline sync jobs.
- Retry or reconnect loops.

Current code guards:

- Lyrics prefetching requires foreground music UI.
- Watch Party realtime is not supposed to stay alive during normal solo background playback unless a party is active.
- Offline user-data sync is WorkManager driven and was short in the captured sample.

## Wake Lock Position

Do not remove the music wake mode just to improve a battery screen number.

Current music playback uses:

- `ExoPlayer.setWakeMode(C.WAKE_MODE_NETWORK)`
- Foreground media playback service.
- MediaSession/MediaLibraryService for notification, lock screen, media buttons and Android Auto.

That wake behavior is expected for reliable background network audio. The work reduced here is unnecessary CPU/network churn around the player, not the wake lock that keeps playback alive.

## Retest Plan

Use a clean controlled run after installing the fixed build:

```bash
adb -s R5GYB59M66D shell dumpsys batterystats --reset
```

Then:

1. Unplug the phone.
2. Start normal solo Jellyfin music playback.
3. Turn the screen off.
4. Let it run for at least 60-90 minutes; overnight is better.
5. Do not open Vantafyn during the test.
6. Capture:

```bash
adb -s R5GYB59M66D shell dumpsys batterystats dev.vantafyn.mobile > /tmp/vantafyn-music-after-batterystats.txt
adb -s R5GYB59M66D shell pidof dev.vantafyn.mobile
adb -s R5GYB59M66D shell top -H -b -n 1 -p <PID> > /tmp/vantafyn-music-after-threads.txt
adb -s R5GYB59M66D shell dumpsys power > /tmp/vantafyn-music-after-power.txt
```

Optional deeper run:

```bash
adb -s R5GYB59M66D shell perfetto -o /data/misc/perfetto-traces/vantafyn-music-screenoff.pftrace -t 10m sched freq idle am wm gfx view binder_driver hal network
adb -s R5GYB59M66D pull /data/misc/perfetto-traces/vantafyn-music-screenoff.pftrace /tmp/vantafyn-music-screenoff.pftrace
```

## Current Status

The code now removes the main unnecessary background work found in the audit:

- no per-position widget broadcasts while backgrounded
- no uncached widget artwork fetch path per widget render
- no UI-only playback position publication while backgrounded
- lower-frequency background Jellyfin progress reporting
- throttled background diagnostic ticks

The build passes. A clean long screen-off playback run is still required before claiming final before/after Samsung battery numbers.

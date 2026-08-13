# Battery / Lifecycle Audit

This pass audited long-running work that can affect idle battery use. No new product features were added.

## Long-running components

- `core-media` music playback service: owns the Media3 music player/session and should only run as a foreground media service while there is an active music session.
- `core-cast` Google Cast target: listens for Cast sessions while the mobile app is active and now only runs a position ticker when a Cast session has active media.
- `feature-player` mobile player: local video position and Jellyfin progress reporting loops are now lifecycle-gated to `STARTED`. Local video already pauses on app `ON_STOP`.
- `feature-home` Watch Party realtime: Jellyfin websocket is now demand-gated. It starts for the Watch Party screen, active Watch Party work, or active party sessions, not for every idle logged-in foreground session.
- `feature-home` admin dashboard refresh: still refreshes while the admin screen is visible, and is now tracked in diagnostics.
- `feature-requests` hero carousel: now lifecycle-gated to `STARTED`.
- Quick Connect polling: bounded to 60 attempts at 2 seconds and cancelled when leaving Quick Connect.

## Fixes made

- Added `LongRunningTaskRegistry` in `core-media` for admin diagnostics.
- Added `AppForegroundStateRepository` in `core-media` for visible foreground/background state.
- Reduced the Cast position ticker from an unconditional 1-second loop to an active-media-only 5-second loop.
- Fixed `GoogleCastPlaybackTarget.stop()` so the singleton can be restarted cleanly after the coroutine scope is cancelled.
- Stopped Cast ticker immediately on idle, disconnect, or target stop.
- Prevented music service from starting a blank foreground notification when Android Auto/system browsing touches the service without an active track.
- Tracked music service and music position ticker lifecycle.
- Stopped idle Watch Party websocket behavior after login/restore/foreground unless Watch Party is actually needed.
- Stopped Watch Party realtime when leaving the Watch Party screen and no active party remains.
- Gated Requests carousel and local video loops with lifecycle `repeatOnLifecycle`.
- Added Admin -> Battery / Lifecycle diagnostics panel showing active long-running work without tokens or server URLs.

## Remaining intentionally active work

- Music playback stays active in background/lockscreen while music is playing.
- Active Cast sessions keep Cast state updates while media is loaded.
- Active Watch Party sessions may keep realtime connected in background so party state is not silently lost.
- Admin dashboard refreshes while the admin screen is visible.

## No evidence found

- No `WorkManager`, jobscheduler, or alarm-based recurring work in Vantafyn source.
- No `GlobalScope` usage.
- No broad background polling loop outside visible UI, active media, Cast, or active Watch Party after this pass.

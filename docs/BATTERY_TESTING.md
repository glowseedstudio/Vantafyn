# Battery Testing

Use these commands after installing a debug build.

```bash
adb shell dumpsys batterystats --reset
adb shell dumpsys batterystats
adb shell dumpsys deviceidle
adb shell dumpsys activity services | grep -i vantafyn
adb shell dumpsys media_session
adb shell dumpsys jobscheduler | grep -i vantafyn
adb shell dumpsys alarm | grep -i vantafyn
```

## Idle test

1. Launch Vantafyn and log in.
2. Stay on Home without playing music, video, Cast, or Watch Party.
3. Open Admin -> Battery / Lifecycle and confirm no unexpected websocket, Cast reporter, playback reporter, or music service.
4. Background the app for several minutes.
5. Confirm no Vantafyn foreground service remains unless music is playing.

## Music test

1. Start music playback.
2. Confirm Android system controls appear.
3. Lock the phone and let the next song start.
4. Confirm only music service/ticker remains active.
5. Stop playback and confirm the service entry disappears.

## Cast test

1. Start Cast playback.
2. Confirm one Cast reporter appears in diagnostics.
3. Stop or disconnect Cast.
4. Confirm the Cast reporter disappears.

## Watch Party test

1. Open Watch Party and confirm realtime appears in diagnostics.
2. Leave Watch Party without an active party.
3. Confirm realtime stops.
4. Create/join an active party and confirm realtime remains only for the active party.

## Admin test

1. Open Admin and confirm `AdminScreen` refresh is visible in diagnostics.
2. Leave Admin and confirm the admin refresh entry disappears.

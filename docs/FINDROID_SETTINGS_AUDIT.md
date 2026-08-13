# Findroid Settings Audit

Findroid was inspected as a reference for practical Jellyfin app settings. No Findroid source was copied into Vantafyn and Findroid remains isolated under `_reference/Findroid`.

## Findroid Settings Areas Reviewed

- Account/server: active server, offline mode, login/session handling.
- Language: app language, preferred audio language, preferred subtitle language.
- Interface: theme, dynamic colors, home row visibility, extra media info.
- Player: preferred playback backend, platform captions, gestures, seek increments, chapter markers, media segment skip behavior, trickplay, picture-in-picture.
- Downloads/cache/network: mobile data behavior, roaming behavior, image cache, request/connect/socket timeouts.

## Vantafyn Coverage

- Account/server: covered through profile switching, add profile, Quick Connect, logout, auto-login with last profile, server recovery, and saved sessions.
- Language/playback: covered through Jellyfin user playback preferences for audio language, subtitle language, subtitle mode, default audio, and remembered track selections.
- Interface: covered through app background, home section customization, bottom navigation, and profile/admin/request access.
- Player: built-in Media3 video player is the primary player; Cast, audio/subtitle tracks, scaling, Up Next, passout protection, and watch progress are handled in-app.
- Music/system media: owned by the foreground music service and MediaSession.
- Permissions: notification permission is surfaced with a plain explanation for music controls.

## Added In This Pass

- Added a real Preferred Video Player setting under Playback Preferences.
- Default option: Vantafyn player, using the existing Media3 player and preserving Cast, track controls, Jellyfin progress reporting, and Up Next.
- Optional option: External app, which hands the resolved Jellyfin stream URL to Android through `ACTION_VIEW`.
- External player launch has a loading surface and a clean failure path if no suitable app is installed.
- The preference is persisted per Jellyfin profile with an app-level fallback.

## Deferred Intentionally

- MPV/libass backend: Findroid exposes an MPV backend, but Vantafyn does not currently ship MPV. Adding it would be a separate playback-engine milestone.
- Downloads/offline mode: not added until Vantafyn has a proper download queue, storage policy, and offline library model.
- Network timeout tuning: not exposed yet because Vantafyn does not have a user-facing networking settings repository.
- Gesture customization: Vantafyn has a custom Compose player; gesture tuning should be added only after the final gesture model is stable.
- Media segment auto-skip: requires server/plugin data and a separate UX pass.

# Music Background Playback Audit

## Current Architecture

- `core-media` owns `MusicPlaybackController`, the single app-wide ExoPlayer instance for music.
- `core-media` also declares `VantafynMusicPlaybackService`, a Media3 `MediaSessionService` with a foreground media playback notification.
- `feature-music` owns Jellyfin music browsing, lyrics, playback reporting, and user-facing controls.
- Music UI observes `MusicPlaybackController.state`; it does not create its own player.
- Logout, profile switch, and video playback explicitly stop music through `MusicPlaybackController.stop(...)`.

## Root Cause

The service and MediaSession existed, but the playback path was still fragile for locked-screen queue advancement:

- the selected track was the only item guaranteed to be prepared through Jellyfin playback info;
- later queue items relied on fallback URLs only;
- player error handling could leave ExoPlayer in a dead `ERROR`, `ENDED`, or `IDLE` state;
- tapping play after unlock did not always re-prepare the active queue item;
- notification metadata depended on the player recovering cleanly and transitioning to the next MediaItem.

No passwords, access tokens, auth headers, or full stream URLs are logged.

## Fix Approach

- Keep `MusicPlaybackController` as the authoritative player and queue owner.
- Keep `VantafynMusicPlaybackService` as the foreground MediaSession owner.
- Prepare queue entries through Jellyfin playback info before handing the playlist to Media3, falling back to Jellyfin universal audio URLs if playback info fails.
- Store prepared playback info by track id for Jellyfin reporting.
- Preserve queue state while backgrounded, locked, or foregrounded again.
- Recover play commands when the player is `ENDED` or `IDLE`.
- On player error, skip to the next queued item when possible instead of leaving a dead player.
- Update Media3 metadata per track using title, artist, album, and artwork URI.
- Use Media3 network wake mode so queued network streams can transition while the screen is off.

## Notification And Lock Screen

The Android system player and lock-screen controls are backed by the Media3 `MediaSessionService`.

Expected commands:

- play resumes or re-prepares the active item if the player was ended/idle;
- pause pauses the service-owned player;
- next/previous route to the active Media3 playlist;
- stop is handled by app cleanup paths and service shutdown.

Artwork and metadata come from each queued `MediaItem` and update on Media3 item transition.

## Remaining Notes

- If Android kills the process, full persistent queue restoration is still future work.
- Battery optimization guidance should be added later only if foreground MediaSession playback still gets killed by a specific OEM.

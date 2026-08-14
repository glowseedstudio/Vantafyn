# Offline Test Plan

## Automated

- Download identity equality across same item id on different servers. Implemented.
- Same server with two users cannot see each other's records.
- State transitions do not allow failed/cancelled records to appear as completed. Implemented.
- SQLite schema creates and upgrades without destructive migration.
- Pending user-data mutations are scoped by server and user.
- Offline schema v1 to v2 migration preserves records and adds remote artwork/profile-id columns.
- Offline schema v2 to v3 migration preserves records and adds sidecar manifest, lyrics, chapter, trickplay, and feature-flag columns.

## Manual Smoke Tests

- Queue movie, cancel, retry, remove. Implemented for direct-play movie items.
- Queue episode, cancel, retry, remove. Implemented for direct-play episode items.
- Queue selected TV season. Implemented by queuing each episode as a separate scoped download.
- Queue track from the music context menu.
- Queue album or playlist from its music detail header.
- Queue audiobook from its media detail More menu.
- Kill app during download and confirm record remains recoverable.
- Airplane mode, force-stop, launch app, browse downloaded media from the Downloads screen.
- Server unreachable on saved-profile restore with completed downloads opens the Downloads screen in offline mode.
- Missing local file shows unavailable/repair state rather than crashing.
- Poster artwork appears from app-private local cache for completed downloads.
- External subtitles resolve to local paths during offline playback when Jellyfin provides an external delivery URL.
- Downloaded music/audiobook lyrics are written to the offline sidecar bundle when Jellyfin exposes lyrics for that item.
- Downloaded video media segments are written to the offline sidecar bundle and passed to the existing Vantafyn player skip-segment logic.
- Chapter/trickplay sidecar paths remain empty until the active Jellyfin SDK repository surface exposes a stable download source; the app must not show fake offline chapter/trickplay support.
- Offline progress is persisted on player exit and scheduled for sync after reconnect.
- Remove account hides downloads from other users.
- Wi-Fi-only default persists and affects newly queued video, season, music track, album, playlist, and audiobook downloads.
- Delete-all removes downloaded files and records for the active profile only.

## Regression Areas

- Online home/search/library remain unchanged.
- Existing video player remains the playback owner.
- Existing music service remains the only music player/MediaSession, including local offline audio playback.
- Android Auto music is not regressed.
- Battery monitors show no continuous polling when no downloads are active.

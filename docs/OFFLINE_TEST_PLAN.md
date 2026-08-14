# Offline Test Plan

## Automated

- Download identity equality across same item id on different servers. Implemented.
- Same server with two users cannot see each other's records.
- State transitions do not allow failed/cancelled records to appear as completed. Implemented.
- SQLite schema creates and upgrades without destructive migration.
- Pending user-data mutations are scoped by server and user.
- Offline schema v1 to v2 migration preserves records and adds remote artwork/profile-id columns.

## Manual Smoke Tests

- Queue movie, cancel, retry, remove. Implemented for direct-play movie items.
- Queue episode, cancel, retry, remove. Implemented for direct-play episode items.
- Queue selected TV season. Implemented by queuing each episode as a separate scoped download.
- Queue track and album. Not implemented.
- Queue audiobook. Not implemented.
- Kill app during download and confirm record remains recoverable.
- Airplane mode, force-stop, launch app, browse downloaded media. Not complete; requires offline cold-start repository.
- Server unreachable on saved-profile restore with completed downloads opens the Downloads screen in offline mode.
- Missing local file shows unavailable/repair state rather than crashing.
- Poster artwork appears from app-private local cache for completed downloads.
- External subtitles resolve to local paths during offline playback. Not implemented.
- Offline progress is persisted on player exit and scheduled for sync after reconnect.
- Remove account hides downloads from other users.

## Regression Areas

- Online home/search/library remain unchanged.
- Existing video player remains the playback owner.
- Existing music service remains the only music player/MediaSession.
- Android Auto music is not regressed.
- Battery monitors show no continuous polling when no downloads are active.

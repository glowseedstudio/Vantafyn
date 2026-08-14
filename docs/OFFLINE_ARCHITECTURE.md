# Offline Architecture

Vantafyn treats online and offline media as different sources behind the same app, not as a second offline app.

## Current Implementation

Implemented in this pass:

1. Durable persistence/domain model in `core-downloads`.
2. WorkManager-backed background video download orchestration for direct-play movies, episodes, and selected TV seasons.
3. Atomic `.download` temp files promoted to final files only after transfer completion.
4. Mobile detail action to queue supported downloads.
5. Mobile Downloads screen with queued/downloading/completed/failed state, progress, play, cancel, retry, and remove actions.
6. Local video playback through the existing Vantafyn player using `file://` URIs.
7. Local resume-state persistence on offline player exit and a durable pending mutation record for later reconciliation.
8. Local poster/backdrop/logo asset caching for completed video downloads.
9. Offline recovery from a failed saved-session restore directly into the Downloads screen when completed downloads exist.
10. Constrained WorkManager reconciliation of pending offline playback state when the app has a restored online Jellyfin session.

Not yet complete:

1. Full offline browsing/search/library repository adapters.
2. Music album/track and audiobook downloads.
3. External subtitles, chapters, trickplay, and complete media-stream metadata download.
4. Conflict-aware reconciliation that compares Jellyfin timestamps before deciding whether local progress should overwrite server progress.
5. Download settings such as Wi-Fi-only defaults and delete-all storage management.

## Durable Identity

Every download belongs to:

- `serverId`
- `userId`
- `jellyfinItemId`
- `mediaSourceId`

This prevents cross-user and cross-server leakage even when Jellyfin item ids collide.

## Persistence

`core-downloads` owns:

- download records
- explicit state machine
- local media/artwork/subtitle paths
- persisted byte counts
- selected audio/subtitle metadata
- pending offline user-data mutations
- storage summaries

The first implementation uses Android SQLite directly with explicit schema versions. This avoids introducing Room/KSP into the project during the foundation pass while still giving real migrations and durable app-private persistence.

Implemented persistence pieces:

- `DownloadIdentity` with non-blank `serverId + userId + itemId + mediaSourceId` identity.
- `DownloadRecord` covering media metadata, local media/artwork/subtitle paths, progress, state, and local user-data.
- `PendingUserDataMutation` for offline playback progress/played-state reconciliation.
- `SqliteDownloadRepository` backed by app-private SQLite.
- `DownloadFileStore` for app-private file targets under `offline/{serverId}/{userId}` with `.download` temp files.
- `OfflineDownloadWorker` for foreground WorkManager transfers.
- `OfflineDownloadManager` for queue/cancel/retry/remove orchestration.
- `OfflineUserDataSyncWorker` and `OfflineSyncScheduler` for network-constrained pending playback-state reconciliation.
- Unit coverage for identity scoping and terminal state behaviour.

## Playback Integration

Offline playback must produce the same app-level playback models:

- video: `VantafynPlaybackItem`
- music/audio: `VantafynMusicTrack`

The player decides source by URI. Online items use Jellyfin URLs; downloaded items use app-private local file URIs. No second player or MediaSession is allowed.

## Background Work

The download engine persists intent first, then uses durable Android background work:

```text
queue request -> SQLite record -> worker/DownloadManager -> temp file -> final file -> COMPLETED
```

Foreground services should run only while actual long-running transfer work is active.

The current worker streams with `HttpURLConnection` inside foreground WorkManager instead of Android `DownloadManager`. This keeps authenticated Jellyfin URL resolution inside Vantafyn and avoids persisting access-token URLs in system download history.

## Offline Cold Start

Cold start in airplane mode should restore the selected saved profile locally, skip blocking live-auth requirements, mark server reachability as unavailable, and open the Downloads experience for that profile if normal online home data cannot load.

The current implementation hooks into the saved-profile restore failure path. If the server is unreachable or the network is unavailable and that profile has completed downloads, Vantafyn opens the app into Downloads with an offline profile scope. If no downloads exist, the normal premium connection recovery screen remains available.

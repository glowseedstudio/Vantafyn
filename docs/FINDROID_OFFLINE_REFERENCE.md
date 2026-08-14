# Findroid Offline Reference

Findroid was inspected from `_reference/Findroid`, updated to upstream `main` before this audit. It is GPL-licensed and was used only as an architectural and behavioural reference.

## Patterns Observed

- `DownloaderImpl` prepares downloads by resolving a media source, checking app-specific storage, estimating free space, creating `.download` destination files, and enqueueing the large payload through Android `DownloadManager`.
- It persists metadata before completion: item records, sources, user data, external media streams, segments, trickplay information, and image worker requests.
- `DownloadReceiver` listens for `DOWNLOAD_COMPLETE`, renames `.download` files to final paths, and deletes failed/inconsistent records.
- `ImagesDownloaderWorker` downloads primary/backdrop artwork into app-private files so offline screens can render without network.
- `SyncWorker` iterates saved servers/users and pushes locally changed user data back to Jellyfin.
- `ServerDatabase` is a Room database with explicit schema versions and migrations. Tables include server/user identity, movie/show/season/episode metadata, media sources, media streams, user data, trickplay, and segments.
- `JellyfinRepositoryOfflineImpl` implements the same repository contract as the online repository for key media operations. UI calls the repository contract rather than branching heavily on offline mode.
- Findroid scopes many records by server id and user data by user id. Vantafyn must go further and make every download record explicitly server/user scoped from the beginning.

## What Vantafyn Should Adopt

- Durable local metadata plus local files, not just downloaded video files.
- Temporary `.download` files and atomic finalisation before marking completed.
- A common online/offline repository concept.
- DownloadManager or WorkManager orchestration for long-running work, not Composable/ViewModel ownership.
- Separate image/subtitle/artwork persistence.
- Pending offline user-data sync.
- Conservative cleanup of shared parent metadata.

## What Vantafyn Should Improve

- Use an explicit `serverId + userId + itemId + mediaSourceId` download identity.
- Keep download state as a Vantafyn-owned state machine rather than exposing raw DownloadManager status in UI.
- Add music and audiobook concepts from the start.
- Avoid switching the whole app into "offline mode" based on a single preference; instead use server reachability plus an explicit Downloads destination.
- Keep Vantafyn's existing Media3 music service and video player as the only playback surfaces.

No Findroid source was copied into Vantafyn.

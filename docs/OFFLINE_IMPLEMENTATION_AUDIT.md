# Offline Implementation Audit

## Current Vantafyn Architecture

- Jellyfin API access lives in `core-jellyfin`, mostly through `JellyfinRepositoryProvider` and SDK-backed repository interfaces in `JellyfinModels.kt` / `JellyfinSdkRepositories.kt`.
- Active server identity is represented by `JellyfinServerConfig`. It has `serverId` from Jellyfin when available and a `localId` fallback of `serverId ?: url`.
- Active user identity is represented by `JellyfinUser.id`. A saved profile combines server URL and user id into `profileId`.
- Session persistence uses `SharedPreferencesJellyfinSessionStorage`. Access tokens are encrypted with Android Keystore AES/GCM. Passwords are not stored.
- There is no active Room database, WorkManager setup, or offline media database in Vantafyn today.
- App/user preferences are currently app-private `SharedPreferences` in feature view models and integration storage.
- Image loading uses Coil with remote Jellyfin image URLs. No offline image resolver exists yet.
- Movie and episode playback is prepared by `JellyfinPlaybackRepository.getPlaybackInfo(...)`, mapped to `VantafynPlaybackItem`, then played by `feature-player` using Media3 ExoPlayer.
- Music playback is owned by `MusicPlaybackController` in `core-media`; `VantafynMusicPlaybackService` exposes that single ExoPlayer through Media3 `MediaLibraryService` / MediaSession for notifications, lock screen, and Android Auto.
- Video playback is currently owned by `feature-player`'s mobile player Composable. It does not yet use a persistent video MediaSession service.
- Playback reporting is in `core-jellyfin` through start/progress/stop repository calls and in feature view models that call those repositories.
- Audio and subtitle track models live in `core-media` as `VantafynAudioTrack` and `VantafynSubtitleTrack`.
- External subtitle URLs are carried into `VantafynPlaybackItem` and attached to Media3 when online.
- Cast support lives in `core-cast` and maps Vantafyn track models to Google Cast tracks.
- Android Auto music browsing is implemented in `core-media/VantafynMusicMediaLibraryProvider`.
- There is no download-related production code, durable local media metadata, local artwork resolver, offline repository, or pending offline user-data mutation store.
- Requests/Ombi, admin, statistics, search, and home assume a reachable network-backed repository today, though failed server restore paths have UI fallbacks.

## High-Risk Integration Points

- Offline cold start cannot require live Jellyfin authentication, but must still scope visible downloads by saved profile/server/user.
- Local video playback should feed local URIs into the existing `VantafynPlaybackItem`, not create a second player.
- Local music playback must feed tracks into `MusicPlaybackController`, not create a second audio stack.
- Download records must be keyed by `serverId + userId + itemId + mediaSourceId`.
- External subtitles and artwork need local paths so offline screens do not repeatedly hit remote URLs.
- User progress made offline needs a durable pending mutation table and a deterministic conflict policy before sync is enabled.

## Phase 1 Files Expected To Change

- `settings.gradle.kts`: include a new shared downloads module.
- `gradle/libs.versions.toml`: add only dependencies required by the chosen implementation.
- `core-downloads/*`: new durable download domain/persistence foundation.
- `docs/OFFLINE_ARCHITECTURE.md`, `docs/OFFLINE_SYNC_POLICY.md`, `docs/OFFLINE_TEST_PLAN.md`, `docs/FINDROID_OFFLINE_REFERENCE.md`: required documentation.

Phase 1 intentionally established the durable model first, then the mobile implementation layered user-facing controls on top of it.

## Phase 1 Result

`core-downloads` now exists and builds independently. It contains the offline identity model, download state machine, app-private SQLite repository, migration handling, pending user-data mutation storage, local artwork/media file target helpers, foreground video download worker, and constrained user-data sync worker.

Mobile now exposes a Downloads screen, a detail-page download action for direct-play movies and episodes, local poster display for completed downloads, local playback through the existing video player, and an offline recovery path when a saved Jellyfin profile cannot reach its server but completed downloads exist. Player ownership and music MediaSession ownership remain unchanged.

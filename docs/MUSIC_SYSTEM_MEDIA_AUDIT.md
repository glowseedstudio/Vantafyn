# Music System Media Audit

## Vantafyn Current State

- `core-media/src/main/java/dev/vantafyn/core/media/MusicPlaybackController.kt` owns the single ExoPlayer instance for music.
- `feature-music` observes `MusicPlaybackController.state` and sends play, pause, seek, queue, shuffle, repeat, and favorite actions through that controller.
- `core-media/src/main/java/dev/vantafyn/core/media/VantafynMusicPlaybackService.kt` exposes the controller player through Media3.
- `app-mobile/src/main/AndroidManifest.xml` declares foreground service, media playback foreground service, notification, internet, network state, and wake-lock permissions.
- `PermissionRequestCoordinator` asks Android 13+ notification permission with user-facing music-controls copy.

## Missing Before This Pass

- The service was playback-session focused and did not expose a real Media3 music library for Android Auto/media-browser clients.
- Android Auto had no app descriptor metadata in the mobile app manifest.
- Browse clients could not ask Vantafyn for albums, artists, playlists, songs, search results, or the now-playing queue.
- System-originated media item selection could not hydrate the app queue from Jellyfin music data.

## Implemented In This Pass

- Converted `VantafynMusicPlaybackService` to `MediaLibraryService`.
- Added `VantafynMusicMediaLibraryProvider` as a Jellyfin-backed browser provider.
- Added Android Auto media metadata and `automotive_app_desc.xml`.
- Kept `MusicPlaybackController` as the only player/queue authority.
- Added `MusicPlaybackController.adoptSystemQueue(...)` so Android Auto/system browse playback adopts the same queue instead of creating a second player.

## Security Notes

- Media browser IDs are token-free stable Vantafyn IDs such as `vf-album:<id>` and `vf-track|<container>|<trackId>`.
- Stream URLs are only placed on playable `MediaItem` URIs for ExoPlayer.
- Passwords, access tokens, and full stream URLs are not logged.

## Remaining Test Points

- Confirm notification shade controls appear after starting music.
- Confirm lock-screen metadata and artwork update on track change.
- Confirm Android Auto discovers Vantafyn as an audio app.
- Confirm Android Auto browsing works after a saved Jellyfin profile exists.
- Confirm logged-out Android Auto browse shows the sign-in item instead of fake data.

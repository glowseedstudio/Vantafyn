# WispBench Android Media Audit

## Reference Location

`/home/glowseed/Documents/Project Folders/Backups/WispBench`

## Files Studied

- `lib/core/music/music_audio_handler.dart`
- `lib/core/music/music_browse_tree.dart`
- `lib/core/music/music_controller.dart`
- `android/app/src/main/AndroidManifest.xml`

## Working WispBench Pattern

WispBench used one background audio authority through `audio_service` and `just_audio`.

The useful architecture was:

- a single audio handler owned playback, queue, metadata, and state;
- UI controls talked to that handler instead of owning their own player;
- notification shade, lock screen, and Android Auto all read from the same queue and metadata;
- browse IDs were stable and did not contain secrets;
- browse roots exposed recent music, albums, artists, playlists, songs, queue, and search where supported;
- Android manifest declared the media browser service and automotive descriptor.

## Adapted For Vantafyn

- Vantafyn uses Media3 `MediaLibraryService` rather than Flutter `audio_service`.
- `MusicPlaybackController` remains the single ExoPlayer owner.
- `VantafynMusicPlaybackService` exposes the controller player as both a MediaSession and MediaLibrary session.
- `VantafynMusicMediaLibraryProvider` mirrors WispBench's browse-tree shape using real Jellyfin music repositories.
- Android Auto metadata was added to the mobile app manifest.

## Intentionally Excluded

- WispBench branding and app names.
- YearForge references.
- Flutter UI code.
- Old colors/theme.
- Visualizer experiments.
- Debug screens and unrelated app architecture.

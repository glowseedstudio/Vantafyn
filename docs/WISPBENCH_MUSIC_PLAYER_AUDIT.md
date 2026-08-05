# WispBench Music Player Audit

Source audited: `/home/glowseed/Documents/Project Folders/Backups/WispBench`

## Useful Source Areas

- `lib/core/music/music_models.dart`: track, playlist, queue/session, repeat/shuffle state models.
- `lib/core/music/music_controller.dart`: queue hydration, play/pause/next/previous, seek, shuffle/repeat, playlist edits, mini-player visibility.
- `lib/core/music/music_audio_handler.dart`: single audio player wrapped behind a controller, queue index sync, repeat/shuffle mapping, MediaSession/notification ideas.
- `lib/ui/music/music_mini_player_bar.dart`: mini-player interaction model.
- `lib/features/music/music_library_screen.dart`: music home, now playing, queue, playlist detail, add-to-playlist flows.
- `lib/ui/music/music_artwork.dart`: artwork fallback behavior.
- `lib/features/lyrics_studio/lyrics_studio_parser.dart`: LRC timestamp parsing/export behavior.
- `lib/features/lyrics_studio/lyrics_studio_models.dart`: synced/plain lyric models.

## Avoided

- YearForge and recap/statistics code.
- Visualizer code and microphone-reactive paths.
- Local file scanner/import paths, MediaStore permissions, sidecar write paths.
- WispBench app shell, routing, branding, cyber/neon theme components, debug/demo screens.
- Flutter notification/background service code for this milestone; it is not directly compatible with the Kotlin/Compose app.

## Dependencies Observed

WispBench used Flutter `just_audio`, `audio_service`, and `audio_session`. Vantafyn uses existing Android-native Media3 ExoPlayer instead.

## Adapted For Vantafyn

- Queue state, current-track state, repeat/shuffle, mini-player, fullscreen now-playing, playlist actions, and lyrics display were reimplemented in Kotlin/Compose.
- Music data is now Jellyfin-backed through `core-jellyfin`.
- Playback uses one shared Media3 music controller in `core-media`.
- Lyrics use Jellyfin `LyricsApi` first. The LRC parsing concept is kept for synced/plain display behavior, but old local sidecar lookup is removed.

## Kept Features

- Real music rows.
- Album artwork.
- Track/artist/album metadata.
- Queue playback.
- Next/previous, seek, pause/resume.
- Shuffle and repeat off/all/one.
- Mini-player and fullscreen Now Playing.
- Synced/plain lyrics presentation when Jellyfin exposes lyrics.
- Basic playlist creation and add-current-track support.

## Removed Features

- YearForge.
- Visualizer.
- Local import/scanner UI.
- Lyric editing/export.
- Social/share/export playlist features.
- WispBench branding and neon/cyber styling.

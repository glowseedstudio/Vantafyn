# Music Implementation

## Modules

- `core-jellyfin`: owns Jellyfin music models and `JellyfinMusicRepository`.
- `core-media`: owns the shared Media3 `MusicPlaybackController`.
- `feature-music`: owns mobile music UI, music view model, mini-player, now-playing, lyrics, queue, and playlist actions.
- `feature-home`: hosts the Music tab and coordinates app-level audio overlap rules.

## Jellyfin APIs Used

- `UserViewsApi.getUserViews(...)` to find music libraries.
- `ItemsApi.getItems(GetItemsRequest)` for songs, albums, artists, playlists, album tracks, artist albums, and search.
- `UniversalAudioApi.getUniversalAudioStreamUrl(...)` to build authenticated audio stream URLs.
- `MediaInfoApi.getPostedPlaybackInfo(...)` to prepare active music playback where Jellyfin returns playback metadata.
- `PlayStateApi.reportPlaybackStart(...)` for track starts.
- `PlayStateApi.reportPlaybackProgress(...)` for pause, resume, seek, and throttled progress.
- `PlayStateApi.reportPlaybackStopped(...)` for queue changes, skips, completion, logout/profile switch, and video handoff.
- `LyricsApi.getLyrics(itemId)` for server-provided synced/plain lyrics.
- `PlaylistsApi.createPlaylist(...)` for playlist creation.
- `PlaylistsApi.addItemToPlaylist(...)` for adding the current track.
- `PlaylistsApi.getPlaylistItems(...)` for playlist playback.
- `PlaylistsApi.removeItemFromPlaylist(...)` is wired in the repository, but the first mobile UI does not expose removal yet.

## Extracted From WispBench

The WispBench code was used as owned reference material for:

- queue shape and current-track derivation
- mini-player and fullscreen now-playing concepts
- repeat/shuffle behavior
- playlist action model
- synced/plain lyrics model and LRC parsing behavior

No unrelated WispBench modules were copied. The implementation is Kotlin/Compose/Media3 and uses Vantafyn styling.

## Removed From WispBench Scope

- YearForge/recaps
- visualizer and microphone-reactive paths
- local file scanning/importing
- sidecar lyric file writing
- old routing/app shell
- old branding and neon/cyber styling
- Flutter notification/background service

## Playback Behavior

Music uses a singleton `MusicPlaybackController` backed by Media3 ExoPlayer, so navigation inside the app does not create duplicate players. Music keeps playing while moving around the mobile app.

Starting video playback stops and clears music with a Jellyfin stop report when the music reporter is active. Switching profiles or logging out stops and clears the music queue. Detail-page theme music checks the shared music controller and does not play over active music.

The first implementation does not include a foreground notification/media service. Because of that, mobile music pauses when the app is backgrounded or the screen locks. Playback does not automatically resume on foreground; the user can resume from the mini-player or Now Playing.

## Jellyfin Play-State Reporting

`feature-music` prepares playback info for the selected track through `JellyfinPlaybackRepository.getPlaybackInfo(...)` before starting playback. When that succeeds, reports include the Jellyfin `playSessionId` and `mediaSourceId` returned by the server. If prepare fails, playback falls back to the universal audio stream URL and reports use item id plus position without a play session.

Reporting is event-driven from `MusicPlaybackController`:

- track start: `reportPlaybackStart`
- pause/resume: `reportPlaybackProgress`
- seek: `reportPlaybackProgress`
- timed progress: throttled to roughly every 10 seconds while playing
- next/previous/new queue: previous item `reportPlaybackStopped`, new item `reportPlaybackStart`
- stop/logout/profile switch/video handoff/completion/error: `reportPlaybackStopped`

Reporting failures are logged with debug-safe messages and do not stop playback.

## Lyrics

Vantafyn uses Jellyfin `LyricsApi.getLyrics(itemId)`. If Jellyfin returns timestamps, Now Playing highlights the active synced line. If Jellyfin returns plain lyrics only, Now Playing shows the plain text. If Jellyfin has no lyrics for the item, the UI shows `No lyrics available`.

Old WispBench local sidecar lookup is intentionally not used because Android cannot access arbitrary Jellyfin server filesystem paths.

Lyrics reset whenever the current track changes. Timing is driven from the Media3 controller position, so highlights follow seek and next/previous transitions. Malformed or unavailable Jellyfin lyrics are treated as missing lyrics and do not crash the UI.

## Mobile Music Navigation

The Music tab now includes lightweight mobile destinations for:

- album detail with track list and play action
- artist detail with artist albums
- playlist detail with loaded playlist items and play action
- all songs list

Playlist removal and queue reorder are not exposed yet because those controls need more UX work.

## Known Limitations

- TV music UI is not built yet.
- Foreground/background media notification service is not implemented in this milestone.
- Queue reorder and playlist remove are repository-ready but not exposed in the first UI.
- Jellyfin play-state reporting is wired for active mobile music sessions, but tracks reached by automatic queue transition may not have `playSessionId` until a richer queue pre-prepare path is added. They still report item id and position.
- Artist detail currently shows artist albums. Top tracks and richer artist metadata can be expanded later.
- Genres and instant mixes are not exposed in the first UI.

## Test Checklist

1. App builds.
2. TV app still builds.
3. Music tab appears.
4. Music libraries load from Jellyfin.
5. Albums load.
6. Artists load.
7. Tracks load.
8. Tapping track starts playback.
9. Next/previous works.
10. Seek works.
11. Pause/resume works.
12. Shuffle/repeat work.
13. Album artwork appears.
14. Mini-player appears while music is playing.
15. Full Now Playing screen opens.
16. Lyrics section opens.
17. Synced lyrics display/highlight if exposed by Jellyfin.
18. Plain lyrics display if exposed by Jellyfin.
19. No lyrics state is clean.
20. Playlists load if available.
21. Playlist creation works through `New Playlist`.
22. Add current track works for the first listed playlist.
23. Switching profile/logging out stops music cleanly.
24. Starting video pauses music cleanly.
25. Theme music does not overlap music playback.
26. No WispBench branding remains.
27. YearForge is not present.
28. Visualizer is not present.
29. Vantafyn styling is applied.
30. No unrelated WispBench code is copied.

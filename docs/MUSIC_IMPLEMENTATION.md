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
- full Now Playing hierarchy: art-led backdrop, large album art, icon transport controls, queue/actions, and dedicated lyrics mode

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

Mobile music now runs behind a Media3 `MediaSessionService` declared by `core-media`. The mobile manifest requests foreground media playback and notification permissions. The service uses the same singleton player, so background/lock-screen playback does not create a second player.

Android 13+ requires the user to grant `POST_NOTIFICATIONS` before Vantafyn can reliably show lock-screen and notification media controls. Vantafyn does not ask on cold launch. The first user-initiated music start/resume shows an in-app explainer before the Android permission dialog. If the user chooses `Not now` or denies the permission, music continues in-app, but lock-screen controls may not appear. The user can later manage this from Settings > Permissions.

When playback stops because of logout, profile switch, video playback, completion, or an explicit stop, `MusicPlaybackController` stops the Media3 service so the foreground notification is cleared.

## Notification Permission And Channel

Permission behavior:

- Android 13+: `POST_NOTIFICATIONS` is requested only after Vantafyn explains that it is used for music controls.
- Android 12 and below: no runtime notification permission is requested.
- Granted: music can continue in the background and the system media notification/lock-screen controls can appear.
- Denied or dismissed: playback can continue in the app, but notification and lock-screen controls may not appear.
- Permanently denied: Settings shows an action that opens Android notification settings.

The Media3 music service creates a notification channel:

- id: `vantafyn_music_playback`
- name: `Music playback`
- description: `Playback controls for music playing in Vantafyn`
- importance: low
- sound/vibration: disabled

Music `MediaItem` metadata includes title, artist, album, and artwork URI so the Media3 notification can show the current song and artwork when supported by the OS/provider.

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

Lyrics now open as a dedicated full-screen music surface from Now Playing. Synced lines are tappable for seeking, highlight the active line, and reset on track changes.

## Music Playlist Filtering

The Music tab only shows playlists classified as audio playlists. Because Jellyfin playlists do not always expose a reliable media type, `core-jellyfin` inspects the first page of each playlist. Playlists with audio items and no video items are included. Movie, episode, series, collection, mixed-video, empty, or unknown playlists are hidden from Music.

## Mobile Music Navigation

The Music tab now includes lightweight mobile destinations for:

- album detail with track list and play action
- artist detail with artist albums
- playlist detail with loaded playlist items and play action
- all songs list
- music-specific long-press actions for tracks: play, play next, add to queue, add to playlist, go to album placeholder, more info placeholder

Playlist removal and queue reorder are not exposed yet because those controls need more UX work.

## Known Limitations

- TV music UI is not built yet.
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
26. Android 13+ first music start shows the Vantafyn notification explainer before the OS dialog.
27. Denying notification permission does not crash playback.
28. Settings shows notification permission status and action.
29. Notification channel exists as `Music playback`.
30. No WispBench branding remains.
31. YearForge is not present.
32. Visualizer is not present.
33. Vantafyn styling is applied.
34. No unrelated WispBench code is copied.

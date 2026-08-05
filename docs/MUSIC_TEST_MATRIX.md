# Music Test Matrix

## Core Playback

1. Open the mobile Music tab.
2. Confirm songs, albums, artists, and playlists load from Jellyfin.
3. Tap a song and confirm playback starts.
4. Confirm mini-player appears.
5. Open Now Playing.
6. Pause and resume.
7. Seek forward and backward.
8. Use next and previous.
9. Toggle shuffle.
10. Cycle repeat off/all/one.

## Library Navigation

1. Open an album and confirm its track list loads.
2. Play an album from the album detail screen.
3. Open an artist and confirm albums load, or a clean empty state appears.
4. Open a playlist and confirm playlist items load.
5. Play a playlist from the playlist detail screen.
6. Open the all songs list from Music home.

## Playlists

1. Start a track.
2. Create a new playlist from Now Playing.
3. Confirm the playlist is refreshed in the Music tab.
4. Add the current track to the first available playlist.
5. Add a visible selected track to the first available playlist using the inline add control.
6. Confirm playlist API failures show a clean error and do not crash playback.

## Lyrics

1. Play a track with synced lyrics and confirm active-line highlighting follows playback.
2. Seek during synced lyrics and confirm the highlighted line updates.
3. Use next/previous and confirm lyrics reset for the new track.
4. Play a track with plain lyrics and confirm plain text displays.
5. Play a track with no lyrics and confirm the empty state says `No lyrics available`.
6. Confirm malformed or unavailable server lyrics are treated as missing.

## Jellyfin Reporting

1. Start a song and confirm the Jellyfin session shows Vantafyn Mobile as playing where supported.
2. Confirm start reports include item id and position.
3. Confirm prepared tracks include `playSessionId`/`mediaSourceId` when Jellyfin returns them.
4. Pause and resume, then confirm Jellyfin progress updates.
5. Seek and confirm Jellyfin progress updates.
6. Wait at least 10 seconds and confirm throttled progress reports continue.
7. Use next/previous and confirm the old item stops and the new item starts.
8. Let a queue finish and confirm a stop report is sent.

## Lifecycle

1. Start music, then start video playback. Music should stop and not overlap video.
2. Start music, then open a detail page with theme ambience. Theme ambience should not overlap music.
3. Start music, then switch profile. Music should stop and clear its queue.
4. Start music, then log out. Music should stop and clear its queue.
5. Start music, then background the app or lock the screen. Music should pause because no foreground notification service exists yet.
6. Return to the app and confirm playback can be resumed manually.

## Regression Checks

1. Confirm mobile video playback still works.
2. Confirm Live TV playback still works.
3. Confirm Android TV still builds.
4. Confirm no WispBench branding appears.
5. Confirm YearForge is absent.
6. Confirm visualizer features are absent.

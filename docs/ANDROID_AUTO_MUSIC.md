# Android Auto Music

Vantafyn exposes music to Android Auto through the Media3 `MediaLibraryService` in `core-media`.

## Browse Roots

- Recently added
- Albums
- Artists
- Playlists
- Songs
- Now playing queue

If no saved Jellyfin profile can be restored, Android Auto receives a single browsable sign-in item instructing the user to open Vantafyn on the phone.

## Data Source

`VantafynMusicMediaLibraryProvider` restores the active saved Jellyfin profile through `JellyfinRepositoryProvider`, then loads `musicRepository.getMusicHome(...)`.

Album, artist, playlist, song, queue, and search children are real Jellyfin data. No local fake music catalog is exposed.

## Playback

When Android Auto selects a playable item, `VantafynMusicPlaybackService` resolves the source container into a queue and passes it to `MusicPlaybackController.adoptSystemQueue(...)`.

The existing controller-owned ExoPlayer then handles playback, notification state, lock-screen state, queue advancement, shuffle, repeat, and UI updates.

## IDs And Secrets

Media IDs contain only Vantafyn route/container IDs and Jellyfin item UUIDs. They do not include server URLs, access tokens, or signed stream URLs.

Playable `MediaItem` URIs use Jellyfin stream URLs because ExoPlayer requires them.

## Manifest

`app-mobile` declares:

- `com.google.android.gms.car.application` metadata;
- `@xml/automotive_app_desc` with `<uses name="media" />`.

`core-media` declares `VantafynMusicPlaybackService` with Media3 library/session actions, legacy media browser action, and media playback foreground-service type.

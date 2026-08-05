# Vantafyn

Vantafyn is a premium Android TV and Android phone client for Jellyfin. The design direction is quiet, readable, and streaming-service polished: dark graphite surfaces, soft contrast, rounded media cards, clear TV focus states, and no cyberpunk or neon styling.

## Project Structure

- `app-tv`: Android TV / Google TV / Fire OS app shell.
- `app-mobile`: Android phone app shell.
- `core-jellyfin`: shared Jellyfin SDK integration boundary.
- `core-media`: shared Media3 ExoPlayer playback boundary.
- `core-ui`: shared Vantafyn design system.
- `feature-home`: splash, server address, login placeholder, and home placeholders.
- `feature-library`: placeholder library feature module.
- `feature-player`: placeholder player feature module.
- `docs`: architecture and research notes.
- `_reference`: research-only clones, including Wholphin.

## Setup

1. Open the repository in Android Studio with JDK 17 or newer.
2. Let Gradle sync the project.
3. Build the TV app with `./gradlew :app-tv:assembleDebug`.
4. Build the phone app with `./gradlew :app-mobile:assembleDebug`.

The skeleton currently includes real dependencies for the Jellyfin Kotlin SDK and AndroidX Media3 ExoPlayer, but authentication and playback are intentionally placeholders.

## Reference Policy

Wholphin is cloned under `_reference/Wholphin` only for research. It is not included in `settings.gradle.kts`, not imported as a Gradle module, and no Wholphin source files are copied into Vantafyn.

## Early Reference Notes

The local Wholphin reference clone describes a from-scratch Android TV Jellyfin client with a single-activity MVVM structure. Its developer guide lists Kotlin, Compose, Jellyfin Kotlin SDK, Navigation 3, Room, DataStore, Hilt, Media3/ExoPlayer, optional MPV/libmpv, Coil, and OkHttp.

Product ideas worth studying from Wholphin include customizable home rows, pinned collections/playlists/favorites/genres/studios, a navigation drawer, configurable library grids/lists, profile protection, subtitle workflows, ExoPlayer and MPV playback choices, Live TV/DVR, trickplay, refresh-rate/resolution switching, and TV-friendly seek controls.

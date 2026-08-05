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

The project includes real dependencies for the Jellyfin Kotlin SDK and AndroidX Media3 ExoPlayer. Authentication and library loading are implemented; playback is intentionally not implemented yet.

## Testing Jellyfin Login

1. Install either debug app on a device or emulator that can reach your Jellyfin server.
2. Enter a full Jellyfin server URL, for example `http://192.168.1.29:8096` or `https://example.com`.
3. Continue to test the server connection.
4. Enter a Jellyfin username and password.
5. After login, the app stores the server URL and access token locally and loads the authenticated user's libraries.
6. Relaunch the app to validate session restore. If the saved token is no longer accepted, Vantafyn returns to the login screen for the saved server.

Do not put credentials in source files, Gradle files, or logs.

## Reference Policy

Wholphin is cloned under `_reference/Wholphin` only for research. It is not included in `settings.gradle.kts`, not imported as a Gradle module, and no Wholphin source files are copied into Vantafyn.

## Early Reference Notes

The local Wholphin reference clone describes a from-scratch Android TV Jellyfin client with a single-activity MVVM structure. Its developer guide lists Kotlin, Compose, Jellyfin Kotlin SDK, Navigation 3, Room, DataStore, Hilt, Media3/ExoPlayer, optional MPV/libmpv, Coil, and OkHttp.

Product ideas worth studying from Wholphin include customizable home rows, pinned collections/playlists/favorites/genres/studios, a navigation drawer, configurable library grids/lists, profile protection, subtitle workflows, ExoPlayer and MPV playback choices, Live TV/DVR, trickplay, refresh-rate/resolution switching, and TV-friendly seek controls.

## Implementation Notes

- SDK entry point: `core-jellyfin` creates a Jellyfin SDK instance with Vantafyn client/device info.
- Server test: `systemApi.getPublicSystemInfo()`.
- Login: `userApi.authenticateUserByName(username, password)`.
- Session restore: `userApi.getCurrentUser()` with the saved access token.
- Libraries: `userViewsApi.getUserViews(userId)` mapped to Vantafyn `JellyfinLibrary` models.
- Session storage: `SharedPreferencesJellyfinSessionStorage` behind the `JellyfinSessionStorage` interface in `core-jellyfin`.

Next work: replace app-private token storage with encrypted storage, add richer library rows with item thumbnails, add image loading, add navigation to library detail screens, and only then start playback URL and Media3 integration.

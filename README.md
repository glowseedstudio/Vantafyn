# Vantafyn

Vantafyn is a premium Android TV and Android phone client for Jellyfin. The design direction is quiet, readable, and streaming-service polished: dark graphite surfaces, soft contrast, rounded media cards, clear TV focus states, and no cyberpunk or neon styling.

## Project Structure

- `app-tv`: Android TV / Google TV / Fire OS app shell.
- `app-mobile`: Android phone app shell.
- `core-jellyfin`: shared Jellyfin SDK integration boundary.
- `core-media`: shared Media3 ExoPlayer playback boundary.
- `core-ui`: shared Vantafyn design system.
- `feature-home`: splash, server address, profile picker, login, and home surfaces.
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
4. Confirm the discovered server name/version.
5. If the server exposes public users on the login screen, select a profile. Hidden users remain hidden by Jellyfin.
6. Enter the Jellyfin password for that user, or type a username manually through Add Profile when no public users are available.
7. After login, the app stores the server URL, user profile metadata, and access token locally, loads the authenticated user's libraries, and builds the mobile Home screen from real Jellyfin rows.
8. Relaunch the app to validate the returning flow. Vantafyn shows "Who's watching?" even when only one saved profile exists.
9. Select a profile to validate/restore the saved token. If the token is no longer accepted, Vantafyn returns to sign-in for that server/user.
10. From sign-in, use Quick Connect when your Jellyfin server has Quick Connect enabled. Vantafyn shows the code and polls until the server approves or the request expires.

Do not put credentials in source files, Gradle files, or logs.

## Installing Debug Builds

Build both apps:

```bash
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk ./gradlew :app-tv:assembleDebug :app-mobile:assembleDebug
```

Install the phone app:

```bash
adb install -r app-mobile/build/outputs/apk/debug/app-mobile-debug.apk
```

Install the TV app:

```bash
adb install -r app-tv/build/outputs/apk/debug/app-tv-debug.apk
```

Use `adb devices -l` first if you have multiple Android devices connected, then pass `-s <device-id>` to install to a specific phone or TV.

Local IP URLs such as `http://192.168.1.29:8096` only work when the device is on a network that can reach that address. Domain URLs such as `https://media.example.com` require valid DNS and HTTPS configuration from the device.

## Saved Profiles

Vantafyn stores saved profiles per app install. Each profile contains the server URL/name/version, Jellyfin user id/name/image tag, access token, and last-used timestamp. Passwords are never stored. The storage is currently app-private `SharedPreferences` behind the `JellyfinSessionStorage` abstraction so it can later move to encrypted storage or DataStore without changing UI code.

The profile picker uses public Jellyfin users when the connected server provides them, including primary user images when available. Saved profile removal is intentionally behind Manage mode and removes only the local saved session from this device.

## Reference Policy

Wholphin is cloned under `_reference/Wholphin` only for research. It is not included in `settings.gradle.kts`, not imported as a Gradle module, and no Wholphin source files are copied into Vantafyn.

## Early Reference Notes

The local Wholphin reference clone describes a from-scratch Android TV Jellyfin client with a single-activity MVVM structure. Its developer guide lists Kotlin, Compose, Jellyfin Kotlin SDK, Navigation 3, Room, DataStore, Hilt, Media3/ExoPlayer, optional MPV/libmpv, Coil, and OkHttp.

Product ideas worth studying from Wholphin include customizable home rows, pinned collections/playlists/favorites/genres/studios, a navigation drawer, configurable library grids/lists, profile protection, subtitle workflows, ExoPlayer and MPV playback choices, Live TV/DVR, trickplay, refresh-rate/resolution switching, and TV-friendly seek controls.

## Implementation Notes

- SDK entry point: `core-jellyfin` creates a Jellyfin SDK instance with Vantafyn client/device info.
- Server test: `systemApi.getPublicSystemInfo()`.
- Public login profiles: `userApi.getPublicUsers()`.
- Login: `userApi.authenticateUserByName(username, password)`.
- Quick Connect: `quickConnectApi.getQuickConnectEnabled()`, `quickConnectApi.initiateQuickConnect()`, `quickConnectApi.getQuickConnectState(secret)`, and `userApi.authenticateWithQuickConnect(secret)`.
- Session restore: `userApi.getCurrentUser()` with the saved access token.
- Libraries: `userViewsApi.getUserViews(userId)` mapped to Vantafyn `JellyfinLibrary` models.
- Mobile Home data: `itemsApi.getResumeItems()`, `tvShowsApi.getNextUp()`, and `userLibraryApi.getLatestMedia()` are mapped inside `SdkJellyfinHomeRepository` to hero items, Continue Watching/Next Up, Recently Added Movies, Recently Added TV, and My Media rows.
- Admin data: administrator accounts are detected through Jellyfin `UserPolicy.isAdministrator`. The mobile Admin tab uses only real Jellyfin SDK data from system info, sessions, users, and item-count queries. Watch-time totals and historical playback analytics are explicitly marked as requiring a future Vantafyn plugin or external reporting source; no fake statistics are shown.
- Session storage: `SharedPreferencesJellyfinSessionStorage` behind the `JellyfinSessionStorage` interface in `core-jellyfin`.
- Onboarding visuals: shared `VantafynOnboardingBackground` in `core-ui` renders the supplied nebula with dark and directional scrims for readable TV/phone setup screens.
- Mobile premium pass: `docs/MOBILE_PREMIUM_PASS.md` documents background selection, My List/favourites behavior, search grouping, Live TV guide limitations, admin data boundaries, and preference/editing limitations.
- Launch visuals: both app modules define dark adaptive icon backgrounds and Android 12+ splash attributes so Vantafyn does not launch on a white icon tile/background.
- Local server support: `JellyfinServerUrlNormalizer` accepts bare LAN IPs and `.local` names, defaults them to `http://...:8096`, and keeps public domains on HTTPS by default. Server testing tries sensible normalized candidates before failing.
- Android cleartext: both app manifests currently set `usesCleartextTraffic="true"` with a development network security config so local Jellyfin HTTP servers work on phone, TV, and Fire OS.
- Wholphin reference note: Wholphin also permits cleartext traffic and validates servers through Jellyfin public system info. Vantafyn uses its own normalizer and repository flow; no Wholphin source was copied.

Next work: replace app-private token storage with encrypted storage, add real library/detail navigation, finish search and profile settings actions, tune TV Home separately, and only then start playback URL and Media3 integration.

# Vantafyn Architecture

Vantafyn is a Kotlin, Jetpack Compose Android client for Jellyfin with TV and phone apps built on shared feature and core modules.

## Modules

- `app-tv`: Android TV, Google TV, and Fire OS entry point. This module owns TV manifests, launcher metadata, TV navigation shell, and TV-specific app wiring.
- `app-mobile`: Android phone entry point. This module owns phone manifests, launcher metadata, and phone-specific app wiring.
- `core-jellyfin`: Shared Jellyfin connection, authentication, API, session, storage abstraction, and library-view logic. It depends on the Jellyfin Kotlin SDK.
- `core-media`: Shared playback abstractions and Media3 integration. The first playback engine is AndroidX Media3 ExoPlayer.
- `core-ui`: Shared Vantafyn design system: colors, typography, spacing, card shapes, poster cards, focus treatment, and motion constants.
- `feature-home`: Shared onboarding and home surfaces. TV and phone compose their own layout around the same screen concepts.
- `feature-library`: Library browsing feature boundary. It will own media lists, filters, collection sections, and detail entry points.
- `feature-player`: Playback feature boundary. It will own player UI, transport controls, subtitle/audio selection, and session reporting.
- `docs`: Project documentation and research notes.
- `_reference`: Local research-only clones. GPL or third-party source here is not imported as a Vantafyn module and should not be copied into app source.

## Direction

The app modules should remain thin. Shared Jellyfin behavior belongs in `core-jellyfin`, shared playback behavior belongs in `core-media`, and reusable visual language belongs in `core-ui`. Feature modules should depend on core modules, while core modules should not depend on app or feature modules.

Playback is intentionally not implemented in this milestone. The next step is to add library detail screens and media item queries, then add playback URL selection before wiring Media3.

## Jellyfin Connection Flow

`core-jellyfin` owns the SDK and exposes plain Vantafyn models:

- `JellyfinAuthRepository`: server test, public login-user discovery, username/password login, saved-session restore, and logout.
- `JellyfinLibraryRepository`: authenticated library/view fetch.
- `JellyfinAdminRepository`: administrator-only read model for real server, session, user, library-count, server-tool, and statistics data.
- `JellyfinUserPreferencesRepository`: current-user playback settings, password changes, and current-user profile image changes.
- `JellyfinSessionStorage`: storage boundary for saved profiles, server metadata, user identity, and access tokens.
- `JellyfinResult`: success/failure wrapper so UI code does not catch SDK exceptions directly.

The Compose UI in `feature-home` uses `VantafynHomeViewModel`, which calls these repositories. App modules do not call the Jellyfin SDK directly.

SDK APIs currently used:

- `createJellyfin { ... }`
- `createApi(baseUrl, accessToken)`
- `systemApi.getPublicSystemInfo()`
- `userApi.getPublicUsers()`
- `userApi.authenticateUserByName(...)`
- `userApi.getCurrentUser()`
- `userViewsApi.getUserViews(userId)`
- `systemApi.getSystemInfo()`
- `sessionApi.getSessions()`
- `userApi.getUsers(...)`
- `imageApi.getUserImageUrl(...)`
- `imageApi.postUserImage(...)`
- `imageApi.deleteUserImage(...)`
- `itemsApi.getItems(GetItemsRequest)`

Session storage currently lives in app-private `SharedPreferences` via `SharedPreferencesJellyfinSessionStorage`. It supports multiple saved profiles and migrates the earlier single-session keys if present. The interface exists so encrypted storage or DataStore can replace it without changing UI or repository call sites. Passwords are not stored.

Admin surfaces must only display metrics that Jellyfin actually returns. Current admin counts come from real item-count queries and live session/user APIs. Historical watch-time analytics use the Jellyfin Playback Reporting plugin when its `user_usage_stats` endpoints are available; otherwise Vantafyn falls back to limited Jellyfin core data and clearly labels detailed statistics as unavailable.

Jellyfin user profile image editing is implemented in `core-jellyfin`; see `docs/JELLYFIN_PROFILE_IMAGES.md`. Mobile owns Android Photo Picker and local crop/resize UI, while the repository owns authenticated upload/delete calls and session/profile refresh. TV displays Jellyfin profile images but does not expose image picking yet.

The first-run and returning launch flow lives in `feature-home`:

- Welcome
- Connect Server
- Server Confirm
- Profile Picker for public server users when available
- Sign In
- Profile Picker
- Home

Returning launches show the profile picker even when only one saved profile exists. Selecting a saved profile validates the saved token before entering Home; expired profiles are sent back to Sign In for the saved server. Selecting a public server user pre-fills the username and then continues through password sign-in.

## Reference Boundary

Wholphin is cloned in `_reference/Wholphin` for research only. The Vantafyn Gradle settings do not include it, and `_reference/Wholphin/` is ignored by Git. Treat it as a product and integration reference, not as source material for direct reuse.

Early useful patterns to evaluate later:

- Single-activity app structure with stateful screen navigation.
- Shared Jellyfin SDK service layer for server/user/session operations.
- Local persistence split between app preferences, user/server records, and key-value settings.
- TV home customization around row ordering, image shape, pinned sections, and Continue Watching / Next Up behavior.
- Playback engine abstraction that can start with Media3 and leave room for broader compatibility later.

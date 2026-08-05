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

- `JellyfinAuthRepository`: server test, username/password login, saved-session restore, and logout.
- `JellyfinLibraryRepository`: authenticated library/view fetch.
- `JellyfinSessionStorage`: storage boundary for server URL, user identity, and access token.
- `JellyfinResult`: success/failure wrapper so UI code does not catch SDK exceptions directly.

The Compose UI in `feature-home` uses `VantafynHomeViewModel`, which calls these repositories. App modules do not call the Jellyfin SDK directly.

SDK APIs currently used:

- `createJellyfin { ... }`
- `createApi(baseUrl, accessToken)`
- `systemApi.getPublicSystemInfo()`
- `userApi.authenticateUserByName(...)`
- `userApi.getCurrentUser()`
- `userViewsApi.getUserViews(userId)`

Session storage currently lives in app-private `SharedPreferences` via `SharedPreferencesJellyfinSessionStorage`. The interface exists so encrypted storage can replace it without changing UI or repository call sites.

## Reference Boundary

Wholphin is cloned in `_reference/Wholphin` for research only. The Vantafyn Gradle settings do not include it, and `_reference/Wholphin/` is ignored by Git. Treat it as a product and integration reference, not as source material for direct reuse.

Early useful patterns to evaluate later:

- Single-activity app structure with stateful screen navigation.
- Shared Jellyfin SDK service layer for server/user/session operations.
- Local persistence split between app preferences, user/server records, and key-value settings.
- TV home customization around row ordering, image shape, pinned sections, and Continue Watching / Next Up behavior.
- Playback engine abstraction that can start with Media3 and leave room for broader compatibility later.

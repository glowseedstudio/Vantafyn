# Vantafyn Architecture

Vantafyn is a Kotlin, Jetpack Compose Android client for Jellyfin with Android phone and Android TV apps built on shared feature and core modules.

## Modules

- `app-mobile`: Android phone entry point. Owns phone manifests, launcher metadata, permissions, widget metadata, Cast metadata, automotive metadata and phone-specific app wiring.
- `app-tv`: Android TV, Google TV and Fire OS entry point. Owns TV manifests, launcher metadata, TV shell wiring and TV-specific app concerns.
- `core-jellyfin`: Shared Jellyfin SDK boundary for server setup, authentication, Quick Connect, session restore, media browsing, playback info, user data, admin data, playback reporting, media segments and user preferences.
- `core-media`: Shared Media3 playback foundation. Owns the music playback controller/service/session, Android Auto media library provider, Media3 player factory and long-running task tracking.
- `core-cast`: Google Cast sender and remote playback coordination for mobile video handoff.
- `core-downloads`: Durable downloads/offline domain, SQLite persistence, app-private files, WorkManager transfers, offline manifests and pending offline playback-state sync.
- `core-ombi`: Optional Ombi integration, setup/configuration, per-user auth, encrypted token/API-key storage, discovery, request state mapping and request submission.
- `core-integrations`: Shared integration storage/security helpers.
- `core-ui`: Shared Vantafyn design system: colours, typography, glass surfaces, cards, chips, skeleton loading, motion constants, onboarding backgrounds and reusable components.
- `feature-home`: Mobile onboarding, saved profiles, home, libraries, search, details, admin, settings, downloads entry points, watch-party surfaces and app navigation state. TV still uses this module for shared concepts while parity work continues.
- `feature-music`: Mobile music home, playlists, albums, lyrics, queues, mini-player, now playing and service-backed controls.
- `feature-player`: Mobile fullscreen video player, controls, track sheets, Cast controller, Up Next UI and skip-segment prompt UI.
- `feature-requests`: Mobile Ombi Requests setup, discovery, search, detail and request history UI.
- `feature-library`: Library feature boundary retained for ongoing modularisation.
- `companion-plugin`: Separate Vantafyn Companion Jellyfin plugin project.
- `docs`: Architecture, implementation notes, audits and test plans.
- `_reference`: Local research-only clones. GPL or third-party source here is not imported as a Vantafyn module and should not be copied into app source.

## Direction

The app modules should remain thin. Shared Jellyfin behaviour belongs in `core-jellyfin`, shared playback and system media behaviour belongs in `core-media`, downloads belong in `core-downloads`, Cast belongs in `core-cast`, optional integrations belong in their own core modules, and reusable visual language belongs in `core-ui`.

Mobile playback is implemented through Media3 and Jellyfin playback-info negotiation. TV playback is the largest parity gap: `app-tv` builds and keeps the TV module boundary in place, but its player/home experience still needs to be brought up to the mobile standard.

## Jellyfin Boundary

`core-jellyfin` owns the SDK and exposes plain Vantafyn models:

- `JellyfinAuthRepository`: server validation, local/remote endpoint fallback, public login-user discovery, username/password login, saved-session restore, saved-server updates and logout.
- `JellyfinQuickConnectRepository`: logged-out Quick Connect app login and authenticated device authorization.
- `JellyfinLibraryRepository`: authenticated library/view fetch.
- `JellyfinHomeRepository` and `JellyfinMediaRepository`: home rows, search, library content, details, people, episodes, favourites/My List and related media.
- `JellyfinPlaybackRepository`: playback-info negotiation, direct/transcode URL selection, stream metadata, playback reporting, Live TV open/close, Cast playback negotiation and Up Next lookup.
- `JellyfinMediaSegmentRepository`: Jellyfin Media Segments for skip intro/credits/recap/commercial/preview behaviour.
- `JellyfinAdminRepository`: administrator-only read/write model for real server, session, user, library-count, plugin, scheduled-task, server-tool and statistics data.
- `JellyfinUserPreferencesRepository`: current-user playback settings, password changes and current-user profile image changes.
- `JellyfinSessionStorage`: storage boundary for saved profiles, server metadata, user identity and access tokens.
- `JellyfinResult`: success/failure wrapper so UI code does not catch SDK exceptions directly.

Feature modules call repositories and ViewModels. App modules do not call the Jellyfin SDK directly.

SDK/API areas currently used include:

- `systemApi.getPublicSystemInfo()` and `systemApi.getSystemInfo()`
- `userApi.getPublicUsers()`, `authenticateUserByName(...)`, `authenticateWithQuickConnect(...)`, `getCurrentUser()`, user management and policy/configuration APIs
- `quickConnectApi.getQuickConnectEnabled()`, `initiateQuickConnect()`, `getQuickConnectState(...)`, and `authorizeQuickConnect(...)`
- `userViewsApi.getUserViews(...)`
- `itemsApi.getItems(...)`, resume/latest/favourite item queries and metadata fields
- `searchApi.getSearchHints(...)`
- `liveTvApi.getLiveTvChannels(...)` and recommended/on-now program queries
- `mediaInfoApi.getPostedPlaybackInfo(...)`, `openLiveStream(...)`, and `closeLiveStream(...)`
- `playStateApi.reportPlaybackStart(...)`, `reportPlaybackProgress(...)`, and `reportPlaybackStopped(...)`
- SyncPlay APIs for Watch Party foundations
- Media Segments API through the Jellyfin SDK where available
- image upload/delete APIs for Jellyfin user profile pictures
- plugin, scheduled-task and Playback Reporting plugin endpoints where available

## Session And Security

Session storage currently lives behind `JellyfinSessionStorage` and supports multiple saved profiles. Passwords are not stored.

Saved profiles can store local and remote URLs for the same Jellyfin server. Restore/connect prefers local, falls back to remote, and validates server identity where possible so credentials are not silently reused against an unrelated server.

Ombi secrets are handled separately by encrypted integration storage in `core-integrations`/`core-ombi`.

Logs must not include passwords, access tokens, API keys or full signed stream URLs.

## Playback Architecture

Video playback is mobile-first today:

- `feature-home` owns playback navigation and coordinates `VantafynHomeViewModel`.
- `core-jellyfin` resolves Jellyfin playback info and reporting data.
- `feature-player` renders the Media3 player surface and controls.
- `core-cast` owns Google Cast remote playback handoff/state.
- `core-downloads` can provide local file sources for offline video playback.

Music playback is service-owned:

- `MusicPlaybackController` owns the single music ExoPlayer.
- `VantafynMusicPlaybackService` exposes the Media3 `MediaLibraryService`.
- UI, notification shade, lock screen, Android Auto and widgets all observe/control the same playback authority.

No feature should create a second music playback stack.

## Downloads And Offline

`core-downloads` owns:

- download records and identity scoping by server/user/item/media source;
- WorkManager transfer orchestration;
- app-private local media/artwork/subtitle/manifest paths;
- progress/state persistence;
- offline playback metadata;
- pending user-data mutations for later Jellyfin sync.

Online screens do not silently switch into offline mode. Rich offline browsing is intentionally centred in Downloads.

## Admin And Statistics

Admin surfaces must display only data Jellyfin or installed plugins actually expose.

Current admin data includes:

- live sessions and progress;
- direct play/transcode labels and bitrate where exposed;
- server/user/library counts;
- plugin list;
- scheduled tasks and task progress;
- library scan state;
- Playback Reporting statistics when the plugin endpoints are available.

If a server/plugin does not expose a metric, the UI should show a premium unavailable/empty state rather than fake numbers.

## Reference Boundary

Wholphin, Findroid, Jellyfin Android TV, Finamp and similar projects are research references only unless explicitly stated otherwise. The Vantafyn Gradle settings do not include reference clones, and reference source must not be copied into Vantafyn without deliberate licence review.

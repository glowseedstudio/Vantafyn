# Mobile Home And Navigation

This document describes the current logged-in Android phone experience. It replaces the early skeleton notes from the first mobile milestone.

## Implemented Screens

- Home with real Jellyfin hero and row data.
- Libraries with real Jellyfin views and user-specific ordering.
- Library detail pages with paged Jellyfin item loading, filters, sorting and media cards.
- Search with debounced Jellyfin search, grouped filters and result navigation.
- My List backed by Jellyfin favourites/user-data APIs.
- Downloads with completed/offline media rails, local playback, removal actions, progress and recovery.
- Music with service-backed playback, playlists, lyrics, queues, downloads and mini-player surfaces.
- Requests with optional Ombi setup, discovery, search, details and request history.
- Admin, shown only for Jellyfin administrators, with real server users, active sessions, tools, plugins, tasks, media stats and playback-reporting statistics when available.
- Profile/Settings with profile image editing, saved profiles, Quick Connect device authorization, playback preferences, permissions, home-section customisation, downloads, app version/about and logout.
- Media detail pages for movies, series, episodes, collections, people and related content.
- Watch Party / Swipe to Match surfaces with SyncPlay foundations.

## Home Layout

The mobile Home screen is composed from `HomeSectionType` and `HomeSectionPreference` in `feature-home`. The default order is:

1. Media bar
2. My Media
3. Continue Watching / Up Next
4. Recently Added Movies
5. Recently Added TV
6. Live TV Channels, only when Jellyfin returns real channels/programs
7. Smart Rows
8. Other libraries

Preferences are stored in app-private `SharedPreferences` under `vantafyn_home_layout`, keyed by Jellyfin profile id. This keeps visibility, ordering, artwork type, shape, card size, and spacing local to the saved profile and survives app restarts.

The media bar uses `JellyfinHome.heroItems`, built from continue/resume items first, then latest movies, next-up episodes, and latest TV. This gives priority to relevant active viewing while still providing a featured item when there is no resume state.

The hero is a full-width cinematic banner, not a card. Mobile uses Android edge-to-edge so hero artwork can extend behind the status area while profile controls still respect safe insets. The hero prefers Backdrop artwork, overlays Logo artwork when Jellyfin exposes it, falls back to title text when needed, and fades into the app background instead of hard-stopping.

The Home top status-bar scrim is scroll-aware. It fades in only after the hero/header has passed behind the status bar so row titles do not clash with system icons.

## Navigation

The mobile shell uses a fixed-height glass bottom dock with equal-width icon slots. Normal users and administrators keep a stable dock count, with role-specific destinations where needed.

Android back navigation is app-owned through the mobile destination state. Nested screens return to their parent destination, Home is the final in-app root, and explicit Logout is the intentional path that clears an authenticated session.

Most major screens use the shared one-shot reveal animation. The animation is keyed by the opened destination/content identity so navigating between albums, playlists, libraries, details, settings pages and admin pages does not replay stale index state.

## Jellyfin SDK APIs

The mobile experience uses real Jellyfin SDK data through `core-jellyfin` repositories.

Key API areas include:

- Home rows: `itemsApi.getResumeItems()`, `tvShowsApi.getNextUp()`, `userLibraryApi.getLatestMedia()`.
- Libraries: `userViewsApi.getUserViews()`.
- Library detail: `itemsApi.getItems(GetItemsRequest)`.
- Media detail: `userLibraryApi.getItem()`, episode queries, people and related media.
- Search: `searchApi.getSearchHints(GetSearchHintsRequest)`.
- Favourites/My List: Jellyfin user-data favourite APIs.
- Live TV: `liveTvApi.getLiveTvChannels(...)`, `liveTvApi.getRecommendedPrograms(...)`, playback-info/open-stream APIs.
- Playback: `mediaInfoApi.getPostedPlaybackInfo(...)`, play-state start/progress/stop reporting, media stream metadata, Live TV open/close, Cast-specific playback negotiation and Media Segments.
- Quick Connect: app login with initiate/poll/authenticate, and authenticated device authorization with `authorizeQuickConnect(...)`.
- Admin: system info, sessions, users, item counts, plugins, scheduled tasks and Playback Reporting plugin endpoints where available.
- Profile images: Jellyfin image upload/delete APIs through the repository boundary.

## Playback Entry Points

Media cards and detail actions route into the existing mobile player where the item type is playable.

Implemented mobile playback includes:

- movies and episodes;
- Live TV channels/programs where Jellyfin provides a playable channel/source;
- direct play and transcoding fallback;
- subtitle and audio track switching;
- resume and watch-from-beginning;
- Up Next/autoplay;
- Jellyfin Media Segments prompt/auto-skip;
- Google Cast handoff;
- offline local video playback.

Music uses the separate service-owned music stack rather than the fullscreen video player.

## Downloads And Offline

Downloads are surfaced from Home/Profile navigation and backed by `core-downloads`.

The offline experience is intentionally centred in Downloads instead of silently replacing online Home/Library/Search with offline data. Completed downloads show local artwork/metadata where available, support local playback, and can be removed through Vantafyn-styled actions.

If a saved profile cannot restore because the server is unreachable and completed downloads exist for that profile, Vantafyn can recover into Downloads instead of trapping the user at a network error.

## Admin Boundaries

Admin surfaces must display real server data only.

- Active sessions come from Jellyfin sessions and update progress/bitrate where the server exposes it.
- Media stats come from Jellyfin item-count queries.
- Watch-time and historical statistics use the Playback Reporting plugin when its endpoints are available.
- Missing plugin/stat data is shown as unavailable, not fabricated.

## Known Limits

- Android TV parity is still in progress.
- Full source/quality ladder selection is not yet a complete user-facing UI.
- TV playback UI remains TODO.
- Offline browsing is richest inside Downloads; full offline adapters for the normal online tabs remain future polish.
- Some Jellyfin features depend on server plugins, server configuration, tuner/provider support, or Jellyfin exposing the needed API fields.

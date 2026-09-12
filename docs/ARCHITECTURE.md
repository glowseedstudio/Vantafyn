# Vantafyn Architecture

Vantafyn is a Kotlin, Jetpack Compose Android client for Jellyfin with Android phone, tablet, automotive, and Android TV applications built on modular feature and core components.

## Modules

- `app-mobile`: Android phone, tablet, and automotive entry point. Owns phone manifests, launcher metadata, permissions, widget metadata, Cast metadata, automotive metadata and mobile app wiring.
- `app-tv`: Android TV, Google TV, and Fire OS entry point. Owns TV manifests, Leanback launcher metadata, 10-foot D-pad navigation architecture, TV shell wiring, and TV-specific playback and settings.
- `core-jellyfin`: Shared Jellyfin SDK boundary for server setup, authentication, Quick Connect, session restore, media browsing (Movies, TV, Live TV, Audiobooks, eBooks), BoxSet collection linking, playback info, user data, admin telemetry, playback reporting, media segments, and user preferences.
- `core-media`: Shared Media3 playback foundation. Owns the music playback controller/service/session, Audiophile AutoEQ DSP engine (6,000+ headphone calibration profiles), real-time ReplayGain loudness normalization, Android Auto media library provider, Media3 player factory, and long-running playback task tracking.
- `core-cast`: Google Cast sender and remote playback coordination for video handoff to Chromecast, Google TV, and smart displays.
- `core-downloads`: Durable downloads and offline domain, SQLite persistence, app-private files, WorkManager transfers, offline manifests, and pending offline playback-state synchronization.
- `core-ombi`: Optional Ombi integration, setup/configuration, per-user authentication, encrypted token/API-key storage, discovery, request state mapping, and request submission.
- `core-integrations`: Shared integration storage and security helpers.
- `core-ui`: Shared Vantafyn design system: typography, obsidian living glass surfaces, cards, chips, skeleton loading, squiggly wave scrubber, motion tokens, and reusable components.
- `feature-home`: Mobile onboarding, saved profiles, home, libraries, search, details, admin telemetry, settings, live home customizer, downloads entry points, watch-party surfaces, and app navigation state.
- `feature-music`: Mobile music dashboard, quick picks, infinite station radio, playlists (with 4-quadrant dynamic collage), albums, synchronized lyrics, audio fidelity preferences, mini-player, full-screen now playing, and service-backed controls.
- `feature-player`: Fullscreen video player, gesture controls, track selection sheets, Cast controller, Up Next UI, and skip-segment prompt UI.
- `feature-requests`: Mobile Ombi Requests setup, discovery, search, detail, and request history UI.
- `feature-library`: Unified library browsing for Movies, TV, Live TV, Audiobooks, eBooks, and YouTube collections.
- `companion-plugin`: Separate Vantafyn Companion Jellyfin plugin project.
- `docs`: Architecture, design system documentation, implementation notes, audits, and test plans.
- `_reference`: Local research-only clones. Third-party source here is not imported as a Vantafyn module and is not compiled into the application.

## Direction

The app modules remain thin. Shared Jellyfin behaviour belongs in `core-jellyfin`, shared playback and system media behaviour belongs in `core-media`, downloads belong in `core-downloads`, Cast belongs in `core-cast`, optional integrations belong in their own core modules, and reusable visual language belongs in `core-ui`.

## Jellyfin 12 & Server Boundary

`core-jellyfin` owns the SDK and exposes clean Vantafyn domain models:

- `JellyfinAuthRepository`: Server validation, local/remote endpoint fallback, public login-user discovery, username/password login, saved-session restore, saved-server updates, and logout.
- `JellyfinQuickConnectRepository`: Logged-out Quick Connect app login and authenticated device authorization.
- `JellyfinLibraryRepository`: Authenticated library view fetch with first-class support for Movies, TV Shows, Music, Live TV, Audiobooks, and eBooks (Jellyfin 12).
- `JellyfinHomeRepository` and `JellyfinMediaRepository`: Home rows, search, library content, BoxSet collections ("Part of Collection"), details, people, episodes, favorites/My List (categorized by Songs, Movies, and Playlists), and related media.
- `JellyfinPlaybackRepository`: Playback-info negotiation, direct/transcode URL selection, stream metadata, playback reporting, Live TV open/close, Cast playback negotiation, and Up Next lookup.
- `JellyfinMediaSegmentRepository`: Jellyfin Media Segments for skip intro, credits, recap, commercial, and preview behavior.
- `JellyfinAdminRepository`: Administrator-only read/write model for real server status, active sessions (with live bitrate, Direct Play badges, and client IPs), user management, library counts, plugin list, scheduled tasks, and Playback Reporting statistics.
- `JellyfinUserPreferencesRepository`: Current-user playback settings, password changes, and current-user profile image changes (including Jellyfin 12 Base64 avatar management).
- `JellyfinSessionStorage`: Storage boundary for saved profiles, server metadata, user identity, and access tokens.
- `JellyfinResult`: Success/failure wrapper so UI code does not catch SDK exceptions directly.

SDK and API areas currently utilized:
- `systemApi.getPublicSystemInfo()` and `systemApi.getSystemInfo()`
- `userApi.getPublicUsers()`, `authenticateUserByName(...)`, `authenticateWithQuickConnect(...)`, `getCurrentUser()`, user management, and policy/configuration APIs
- `quickConnectApi.getQuickConnectEnabled()`, `initiateQuickConnect()`, `getQuickConnectState(...)`, and `authorizeQuickConnect(...)`
- `userViewsApi.getUserViews(...)`
- `itemsApi.getItems(...)`, resume/latest/favorite item queries, BoxSet collection items, book/audiobook queries, and metadata fields
- `searchApi.getSearchHints(...)`
- `liveTvApi.getLiveTvChannels(...)` and recommended/on-now program queries
- `mediaInfoApi.getPostedPlaybackInfo(...)`, `openLiveStream(...)`, and `closeLiveStream(...)`
- `playStateApi.reportPlaybackStart(...)`, `reportPlaybackProgress(...)`, and `reportPlaybackStopped(...)`
- SyncPlay APIs for Watch Party foundations
- Media Segments API for Intro and Credit skipping
- User profile picture and avatar upload/delete APIs
- Playback Reporting plugin and Achievement Badges plugin endpoints where installed

## Playback Architecture

### Video & Cinema
Video playback is built on AndroidX Media3 and ExoPlayer:
- `feature-home` and `feature-player` coordinate video playback navigation.
- Direct Play is prioritized with direct stream URLs for supported containers (`mp4`, `mkv`, `webm`, `mov`) and codecs (`h264`, `hevc`, `vp9`, `av1`).
- Fallback transcoding to H.264/AAC via HLS when direct playback is unsupported or encounters fatal playback exceptions.
- 4K HDR and Dolby Vision hardware acceleration with automatic aspect ratio fitting (Fit, Zoom, Stretch).
- Multi-track audio and subtitle selection with in-place track overrides.
- Media Segment detection for automatic or single-tap "Skip Intro" and "Skip Credits" prompts.
- Seamless "Up Next" countdown overlay with automated queue progression.
- Google Cast integration via `core-cast`, negotiating dedicated Cast device profiles and providing remote media controls.
- Android TV video playback is fully implemented in `app-tv`, delivering a 10-foot remote-controlled playback experience.

### Audiophile Music & DSP
Music playback is service-owned and centralized in `core-media`:
- `MusicPlaybackController` owns the single active ExoPlayer instance.
- `VantafynMusicPlaybackService` exposes the Media3 `MediaLibraryService`, ensuring that UI, background services, lock-screen controls, notification shades, Android Auto, and home screen widgets all control the same playback authority.
- **AutoEQ Integration**: Built-in access to over 6,000 headphone calibration profiles from Oratory1990, Crinacle, Rtings, and Innerfidelity. Compensation curves are applied directly to Android hardware audio sessions via a 10-band parametric equalizer.
- **ReplayGain Loudness Normalization**: Real-time track volume normalization with anti-clipping headroom calculation.
- **Audio Fidelity Policies**: Independent Wi-Fi and Cellular bitrate policies supporting Bit-perfect Original Master FLAC/ALAC, 320 kbps high-quality MP3/AAC, 192 kbps standard, and Data Saver modes.
- **Dynamic Wave Scrubber**: Fluid kinetics that reflect active playback and smoothly flatten to a linear progress bar when paused.
- **Synchronized Lyrics**: Real-time scrolling karaoke lyrics synced to playback timestamps.
- **Experience Mode**: Users can toggle between the Full Media suite and a dedicated Music-Only application mode with a specialized 4-tab audio dock.

## Downloads and Offline Storage

`core-downloads` provides a resilient offline media architecture:
- Download records are scoped by server, user, item, and media source.
- WorkManager orchestrates background transfers with network type constraints and automatic retry policies.
- Media, high-resolution artwork, and subtitle files are persisted in app-private storage.
- Progress, state persistence, and offline metadata allow full offline playback without server access.
- Pending playback state mutations (watched status, play counts, favorite toggles) are recorded offline and automatically reconciled upon reconnecting.

## Admin & Telemetry

The administrator suite provides real-time visibility into server activity:
- Live session tracking displaying active clients, Direct Play vs Transcode status, active video/audio codecs, transcode reasons, client IP addresses, and bandwidth consumption.
- Server health monitoring for Jellyfin 12 servers, including version, operating system, and architecture.
- One-tap library scans and scheduled task execution.
- User account creation, profile management, and access policy assignment.
- Analytics graphs powered by the Playback Reporting plugin when installed.

## Android TV Architecture

`app-tv` provides a dedicated 10-foot television interface:
- **D-Pad Focus Engine**: Custom `vantafynTvFocusable` modifier with smooth spring scaling (1.05x), focus glow, and high-contrast accessibility borders.
- **Expanding Sidebar**: Collapsed rail (72dp) that expands smoothly on focus (240dp) over the background.
- **Backdrop & Hero Scrims**: Cinematic hero banner with smooth bottom fade into the persistent graphite background layer (`VantafynTvBackground`).
- **Jellyfin 12 Collection Shelves**: "Part of Collection" shelves on TV detail pages for single-click franchise browsing.
- **10-Foot Onboarding & Setup**: Remote-friendly server discovery, Quick Connect code authorization, and profile switching.

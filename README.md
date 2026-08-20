# Vantafyn

Vantafyn is a premium Android phone and Android TV client for Jellyfin.

The project aims to make a private Jellyfin server feel like a polished streaming service: calm visual design, rich artwork, smooth motion, real server data, reliable playback, offline access, music, requests, admin tools, and TV-ready foundations.

For a public-facing project overview, roadmap, privacy stance, reference policy, and AI assistance disclosure, see [docs/GITHUB_PAGE.md](docs/GITHUB_PAGE.md).

## Current Status

Vantafyn is active pre-release software. The Android phone app is the current primary development target. The Android TV app builds and keeps the TV architecture in place, but TV feature parity is still ongoing.

Implemented areas include:

- Jellyfin server setup, username/password login, Quick Connect login, saved profile restore, and explicit logout.
- Separate Quick Connect flows for first-time app login and already-authenticated device authorization.
- Optional local and remote server URLs for one Jellyfin profile, with local-first fallback to remote.
- Real Jellyfin home rows, libraries, search, detail pages, people pages, episodes, favourites/My List, Live TV entry points, and profile switching.
- Mobile video playback through AndroidX Media3 ExoPlayer with resume, reporting, subtitles, audio track switching, screen fit modes, Up Next, skip segments, Live TV fallback, and Cast handoff.
- Music playback through a foreground Media3 service with notification shade controls, lock-screen controls, Android Auto browse/control support, queues, playlists, lyrics, downloads, and mini-player surfaces.
- Downloads and offline mode for video and audio content, including local playback, artwork metadata, recovery into Downloads when the server is unreachable, and pending playback-state sync.
- Ombi Requests integration with shared API-key and per-user Ombi account modes.
- Watch Party / SyncPlay foundations, app-wide invites, and Swipe to Match discovery.
- Mobile admin surfaces for users, active sessions, server tools, plugins, scheduled tasks, media stats, and Playback Reporting statistics where the server/plugin exposes them.
- A shared Vantafyn design system for glass surfaces, typography, motion, skeleton loading, cards, chips, modals, onboarding backgrounds, and navigation polish.

## Project Structure

- `app-mobile`: Android phone entry point, manifest, launcher metadata, permissions and mobile app wiring.
- `app-tv`: Android TV / Google TV / Fire OS entry point and TV app wiring.
- `core-jellyfin`: Jellyfin SDK integration, authentication, session restore/storage abstractions, repositories, playback info, admin data, user preferences and server APIs.
- `core-media`: Media3 playback foundations, music playback service/session, Android Auto media library integration, playback lifecycle helpers and Media3 extension support.
- `core-cast`: Google Cast sender integration for mobile video playback.
- `core-downloads`: durable downloads, offline records, WorkManager transfer orchestration, local files, offline manifests and pending sync.
- `core-ombi`: Ombi configuration, authentication, encrypted token storage, discovery, request and mapping logic.
- `core-integrations`: shared integration/security storage helpers.
- `core-ui`: shared Vantafyn design system and reusable premium components.
- `feature-home`: mobile onboarding, profile flows, home, libraries, details, admin, settings, watch party, downloads entry points and app navigation surfaces.
- `feature-music`: mobile music home, now playing, playlists, lyrics, queues and service-backed controls.
- `feature-player`: mobile Media3 video player UI and controls.
- `feature-requests`: mobile Ombi Requests setup, discovery, search, details and request history.
- `feature-library`: library feature boundary retained for ongoing modularisation.
- `companion-plugin`: separate Vantafyn Companion Jellyfin plugin project.
- `docs`: architecture, implementation notes, audits and test plans.
- `_reference`: research-only external projects. These are not Gradle modules.

## Build

Open the repository in Android Studio with JDK 17 or newer, then let Gradle sync.

Build both Android apps:

```bash
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk ./gradlew :app-mobile:assembleDebug :app-tv:assembleDebug
```

Install the phone app:

```bash
adb install -r app-mobile/build/outputs/apk/debug/app-mobile-debug.apk
```

Install the TV app:

```bash
adb install -r app-tv/build/outputs/apk/debug/app-tv-debug.apk
```

Use `adb devices -l` first if multiple Android devices are connected, then pass `-s <device-id>` to install to a specific device.

## Testing Jellyfin Login

1. Install the mobile or TV debug app on a device that can reach your Jellyfin server.
2. Enter a Jellyfin address. You can provide one address, or separate local/remote endpoints for the same server profile.
3. Continue to test the server connection and confirm the discovered server.
4. Sign in with username/password, or use Quick Connect if the server supports it.
5. After login, Vantafyn stores the authenticated session locally and loads the user's real Jellyfin data.
6. Relaunch the app to validate saved profile restore.
7. Use explicit Logout when you want to clear the authenticated session.

Local IP URLs such as `http://192.168.1.29:8096` only work when the device is on a network that can reach that address. Domain URLs such as `https://media.example.com` require valid DNS and HTTPS configuration from the device.

Do not put credentials in source files, Gradle files, documentation examples or logs.

## Playback

Mobile video playback is implemented in `feature-player` using Media3 ExoPlayer.

Current mobile playback support includes:

- movies, episodes and Live TV channel/program entry points;
- direct play and HLS transcoding fallback through Jellyfin playback info;
- Jellyfin start/progress/stop reporting;
- resume and watch-from-beginning flows;
- subtitle and audio track switching where Jellyfin/Media3 expose supported tracks;
- screen fit and zoom modes;
- Up Next and autoplay controls;
- Jellyfin Media Segments / skip intro-style prompt and auto-skip behaviour;
- Google Cast handoff through `core-cast`;
- screen-awake handling during video playback.

TV playback UI remains a parity target.

See [docs/PLAYBACK_IMPLEMENTATION.md](docs/PLAYBACK_IMPLEMENTATION.md) and [docs/PLAYBACK_TEST_MATRIX.md](docs/PLAYBACK_TEST_MATRIX.md).

## Music

Music playback is service-owned through `core-media` and controlled by `feature-music`.

The music stack uses one authoritative Media3 player/session so the app UI, mini-player, notification shade, lock screen, Android Auto and widgets all observe the same playback state.

See [docs/MUSIC_IMPLEMENTATION.md](docs/MUSIC_IMPLEMENTATION.md), [docs/MUSIC_SYSTEM_MEDIA_AUDIT.md](docs/MUSIC_SYSTEM_MEDIA_AUDIT.md), and [docs/ANDROID_AUTO_MUSIC.md](docs/ANDROID_AUTO_MUSIC.md).

## Downloads And Offline

Downloads live in `core-downloads` with WorkManager-backed transfer orchestration and app-private local storage.

Offline mode supports completed downloads, local playback, artwork/metadata caching, local search/filtering inside Downloads, and pending playback-state reconciliation when the server becomes reachable again.

See [docs/OFFLINE_ARCHITECTURE.md](docs/OFFLINE_ARCHITECTURE.md), [docs/OFFLINE_SYNC_POLICY.md](docs/OFFLINE_SYNC_POLICY.md), and [docs/OFFLINE_TEST_PLAN.md](docs/OFFLINE_TEST_PLAN.md).

## Requests And Integrations

Ombi support is optional and lives behind `core-ombi` and `feature-requests`.

Vantafyn supports shared API-key mode and per-user Ombi account linking. Secrets are stored through encrypted integration storage. Requests discovery, search, request details and personal request history are backed by verified Ombi endpoints where available.

See [docs/ombi-requests-implementation.md](docs/ombi-requests-implementation.md), [docs/OMBI_SETUP_FLOW_AUDIT.md](docs/OMBI_SETUP_FLOW_AUDIT.md), and [docs/OMBI_USER_AUTH.md](docs/OMBI_USER_AUTH.md).

The separate Companion Jellyfin plugin is in [companion-plugin](companion-plugin).

## Privacy And Security

Vantafyn is intended for private Jellyfin servers.

- No analytics, tracking or advertising SDKs should be included.
- Passwords are not stored.
- Jellyfin session data is contained behind `JellyfinSessionStorage`.
- Ombi API keys and per-user tokens use encrypted integration storage.
- Logs must not include passwords, tokens, full signed stream URLs or other secrets.

See [docs/PERMISSIONS.md](docs/PERMISSIONS.md) and [docs/INTEGRATIONS_ARCHITECTURE.md](docs/INTEGRATIONS_ARCHITECTURE.md).

## Reference Policy

Reference projects such as Wholphin, Findroid, Jellyfin Android TV and Finamp are used to study behaviour, architecture and UX patterns.

Reference code is not imported as a Gradle module. GPL source should not be copied into Vantafyn unless explicitly instructed and the license implications are accepted.

See [docs/RESEARCH_TARGETS.md](docs/RESEARCH_TARGETS.md).

## Important Docs

- [Architecture](docs/ARCHITECTURE.md)
- [Design System](docs/DESIGN_SYSTEM.md)
- [Mobile Home And Navigation](docs/MOBILE_HOME.md)
- [Playback Implementation](docs/PLAYBACK_IMPLEMENTATION.md)
- [Music Implementation](docs/MUSIC_IMPLEMENTATION.md)
- [Offline Architecture](docs/OFFLINE_ARCHITECTURE.md)
- [Ombi Requests Implementation](docs/ombi-requests-implementation.md)
- [Admin Features](docs/ADMIN_FEATURES.md)
- [GitHub Page Draft](docs/GITHUB_PAGE.md)

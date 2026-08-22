# Vantafyn

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![AndroidX Media3](https://img.shields.io/badge/Media3-ExoPlayer-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/guide/topics/media/media3)
[![Jellyfin](https://img.shields.io/badge/Jellyfin-Compatible-00A4DC.svg?logo=jellyfin&logoColor=white)](https://jellyfin.org/)

**Vantafyn** is a modern Android phone and Android TV client for [Jellyfin](https://jellyfin.org).

The project aims to make a private Jellyfin server feel like a polished streaming experience: calm visual design, rich artwork, smooth motion, real server data, reliable playback, offline access, foreground music playback, Ombi requests, admin tools, and TV-ready foundations.

---

## Key Features

- **Authentication & Profiles**: Jellyfin server setup, username/password login, Quick Connect authentication, multi-profile restore, and local/remote endpoint failover.
- **Home & Discovery**: Native Jellyfin home rows, library browsing, fast search, rich detail screens, people view, episodes, favorites/My List, and Live TV.
- **Video Playback**: Media3 ExoPlayer integration with resume, progress reporting, audio & subtitle track switching, screen zoom/fit modes, Up Next, skip intro/segments, and Google Cast handoff.
- **Music Player**: Foreground Media3 music service with notification shade and lock-screen controls, Android Auto support, queues, playlists, synchronized lyrics, and downloads.
- **Offline Mode & Downloads**: WorkManager-backed downloads, local playback, artwork metadata caching, and automatic playback-state reconciliation when returning online.
- **Ombi Requests**: Full Ombi integration with shared API-key or per-user accounts, search, issue reporting, and request history tracking.
- **Watch Party / SyncPlay**: Synchronized group playback foundations, app-wide invites, and Swipe to Match movie picker.
- **Admin Tools**: Session inspection, server dashboard, plugin status, scheduled tasks, and media statistics.
- **Design System**: Glass material styling, smooth Compose motion, skeleton loaders, and responsive layouts for phones and TVs.

---

## Project Structure

```
├── app-mobile/          # Android phone entry point, manifests, launcher & navigation wiring
├── app-tv/              # Android TV / Google TV / Fire OS entry point
├── core-cast/           # Google Cast sender integration
├── core-downloads/      # Offline storage, download transfers & sync reconciliation
├── core-integrations/   # Encrypted credentials & integration storage
├── core-jellyfin/       # Jellyfin SDK client, repositories, models & auth abstractions
├── core-media/          # Media3 playback foundations, music service & Android Auto
├── core-ombi/           # Ombi API client & request handling
├── core-ui/             # Vantafyn shared design system & Compose components
├── feature-home/        # Home, discovery, details, libraries, admin & settings
├── feature-music/       # Music hub, player sheet, lyrics & queue management
├── feature-player/      # Video player UI, gestures, subtitle/audio picker & PiP
├── feature-requests/    # Ombi Requests UI, discovery & search
├── companion-plugin/    # Companion Jellyfin plugin
└── docs/                # Architecture, design system & technical documentation
```

---

## Building and Running

### Prerequisites
- JDK 17 or newer
- Android SDK (API 34+)
- Android Studio Ladybug / Meerkat or command-line Gradle

### Build Debug APKs

Build both Phone and TV APKs:

```bash
./gradlew :app-mobile:assembleDebug :app-tv:assembleDebug
```

### Install to Connected Device

Phone:
```bash
adb install -r app-mobile/build/outputs/apk/debug/app-mobile-debug.apk
```

Android TV:
```bash
adb install -r app-tv/build/outputs/apk/debug/app-tv-debug.apk
```

---

## Documentation

- [Architecture Overview](docs/ARCHITECTURE.md)
- [Design System & Materials](docs/DESIGN_SYSTEM.md)
- [Video Playback Implementation](docs/PLAYBACK_IMPLEMENTATION.md)
- [Music Stack Implementation](docs/MUSIC_IMPLEMENTATION.md)
- [Offline & Downloads Architecture](docs/OFFLINE_ARCHITECTURE.md)
- [Ombi Requests Integration](docs/ombi-requests-implementation.md)
- [Admin Features](docs/ADMIN_FEATURES.md)
- [Permissions Guide](docs/PERMISSIONS.md)

---

## License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

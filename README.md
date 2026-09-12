<p align="center">
  <img src="assets/logo.png" width="140" alt="Vantafyn Logo" />
</p>

<h1 align="center">Vantafyn</h1>

<p align="center">
  <strong>The Next Evolution of <a href="https://jellyfin.org">Jellyfin</a> on Android.</strong><br>
  A fluid, cinema-grade client built with Jetpack Compose, AndroidX Media3, Audiophile AutoEQ DSP, gamified achievements, and native social messaging.
</p>

<p align="center">
  <a href="https://github.com/glowseedstudio/Vantafyn/releases/tag/v0.9.8"><img src="https://img.shields.io/badge/Release-v0.9.8-21D8FF.svg?style=flat-square" alt="Version 0.9.8" /></a>
  <a href="https://jellyfin.org"><img src="https://img.shields.io/badge/Jellyfin-v12_Ready-00A4DC.svg?style=flat-square&logo=jellyfin&logoColor=white" alt="Jellyfin v12 Ready" /></a>
  <a href="https://glowseedstudio.github.io/Vantafyn/"><img src="https://img.shields.io/badge/Website-Live_Showcase-E026FF.svg?style=flat-square" alt="Live Showcase" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?style=flat-square&logo=android&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://developer.android.com/guide/topics/media/media3"><img src="https://img.shields.io/badge/Media3-ExoPlayer-3DDC84.svg?style=flat-square&logo=android&logoColor=white" alt="Media3" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square" alt="License: GPL v3" /></a>
</p>

<p align="center">
  <a href="https://github.com/glowseedstudio/Vantafyn/releases/tag/v0.9.8"><strong>⬇️ Download Latest APK (v0.9.8)</strong></a> &nbsp;•&nbsp;
  <a href="https://glowseedstudio.github.io/Vantafyn/"><strong>🌐 Interactive Website & Gallery</strong></a> &nbsp;•&nbsp;
  <a href="#-quick-start"><strong>🚀 Quick Start</strong></a> &nbsp;•&nbsp;
  <a href="docs/ARCHITECTURE.md"><strong>📖 Architecture Docs</strong></a>
</p>

---

> [!TIP]
> **🚀 Jellyfin 12 Support is Here!**
> Vantafyn is fully optimized for **Jellyfin 12** and **10.10+**. Take advantage of Jellyfin 12's new BoxSet collection linking on Mobile and Android TV ("Part of Collection" shelves), updated native Base64 user avatar management, and high-density 3-column library browsing with 100 items per page!

---

## 🎬 Live Motion Preview

<p align="center">
  <a href="https://glowseedstudio.github.io/Vantafyn/">
    <img src="assets/preview.gif" width="100%" alt="Vantafyn Interface Preview" style="border-radius: 16px; box-shadow: 0 16px 40px rgba(0,0,0,0.5);" />
  </a>
  <br>
  <em>(Tap above to visit the <a href="https://glowseedstudio.github.io/Vantafyn/">Interactive Web Showcase</a> or watch the <a href="assets/vantafyn_demo.mp4">1080p full demo video</a>)</em>
</p>

---

## ✨ Why Vantafyn?

Vantafyn is crafted from the ground up to make your self-hosted Jellyfin server feel indistinguishable from a top-tier streaming service—pairing calm obsidian glassmorphism with high-performance audio/video engines.

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>🎬 Cinema Video Engine</h3>
      <ul>
        <li><strong>Jellyfin 12 Collection Linking</strong>: Dedicated "Part of Collection" shelf on Mobile and Android TV detail screens with 1-tap franchise browsing.</li>
        <li><strong>High-Performance Streaming</strong>: Built on AndroidX Media3 & ExoPlayer with automatic resume points.</li>
        <li><strong>4K HDR & Dolby Vision</strong>: Streamlined hardware-accelerated video pipelines.</li>
        <li><strong>Playback Enhancements</strong>: Multi-track audio/subtitle switching, zoom/stretch aspect ratios, Up Next countdowns, and Intro/Credit skipping.</li>
        <li><strong>Compact 3-Row Grid</strong>: High-density 3-column library browsing with 100 items/page.</li>
        <li><strong>Google Cast & PiP</strong>: Native Cast sender integration and Picture-in-Picture support.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🎵 Audiophile DSP & AutoEQ</h3>
      <ul>
        <li><strong>6,000+ Headphone Calibrations</strong>: Full integration of the <a href="https://github.com/jaakkopasanen/AutoEq">AutoEq</a> database (oratory1990, crinacle, Rtings) via hardware equalizer effects.</li>
        <li><strong>Animated Squiggly Wave</strong>: Dynamic sine wave scrubber that dances during playback and flattens when paused.</li>
        <li><strong>ReplayGain & Dynamic Radio</strong>: Automatic loudness leveling with anti-clipping pre-amp and infinite station radio.</li>
        <li><strong>Synced Lyrics & Android Auto</strong>: Live karaoke lyrics and touch-friendly in-car vehicle playback.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>🏆 Gamified Achievements & Social</h3>
      <ul>
        <li><strong>Milestone Badges & Rank Tiers</strong>: Progress from Bronze to Mythic with unlocked celebrations and rarity tiers.</li>
        <li><strong>Animated Chromatic Modals</strong>: Signature glowing gradient borders on unlock sheets and inspections.</li>
        <li><strong>1-to-1 Direct Messaging</strong>: Real-time chat with delivery receipts and battery-efficient lifecycle gating.</li>
        <li><strong>Media Cards & Reactions</strong>: Share movies directly in chat with "Watch Now" action buttons and touch-and-hold emoji reactions.</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>🚗 In-Car Dashboard & True Offline</h3>
      <ul>
        <li><strong>Widescreen Automotive Mode</strong>: Touch-optimized head-unit interface for cars and landscape docks.</li>
        <li><strong>Durable Background Downloads</strong>: WorkManager downloads with offline database and auto-reconciliation on reconnect.</li>
        <li><strong>Live Server Admin & Stats</strong>: Trigger library scans, monitor active sessions, and inspect viewing trends.</li>
        <li><strong>Dynamic Customization</strong>: Reorder home rows with live preview, customize card sizing, and toggle Nebula/Midnight themes.</li>
      </ul>
    </td>
  </tr>
</table>

---

## 📸 Interface Showcase

<p align="center">
  <a href="https://glowseedstudio.github.io/Vantafyn/">
    <img src="assets/screenshots/01_home.png" width="24%" alt="Home Screen" />
    <img src="assets/screenshots/05_details.png" width="24%" alt="Media Details" />
    <img src="assets/screenshots/10_now_playing.png" width="24%" alt="Now Playing Music" />
    <img src="assets/screenshots/12_achievements_hub.png" width="24%" alt="Achievements Hub" />
  </a>
  <br>
  <em>Explore all 18 full-resolution screens and interactive lightboxes on the <strong><a href="https://glowseedstudio.github.io/Vantafyn/#gallery">Interactive Web Gallery →</a></strong></em>
</p>

---

## 🔌 Recommended Server Plugins

While Vantafyn works seamlessly out-of-the-box with any standard Jellyfin server, installing these optional server plugins unlocks rich gamification, analytics, and automation:

| Plugin | What It Unlocks | Source |
| :--- | :--- | :---: |
| **Achievement Badges** | Milestone badges, rank tiers, server member friends list, and 1-to-1 direct messaging | [GitHub](https://github.com/ZL154/AchievementBadges_for_Jellyfin) |
| **Playback Reporting** | Server analytics, watch time breakdowns, and Most Watched media trends in Admin | [GitHub](https://github.com/jellyfin/jellyfin-plugin-playbackreporting) |
| **Intro Skipper** | Automatic audio fingerprint analysis to show seamless "Skip Intro" & "Skip Credits" buttons | [GitHub](https://github.com/Intro-Skipper/intro-skipper) |

---

## 🚀 Quick Start

### Direct Download
Production minified APKs are available on the **[Releases Page](https://github.com/glowseedstudio/Vantafyn/releases/tag/v0.9.8)**:
- **Phone / Tablet / Auto**: [`app-mobile-release.apk`](https://github.com/glowseedstudio/Vantafyn/releases/download/v0.9.8/app-mobile-release.apk) *(51 MB, R8-shrunk)*
- **Android TV**: [`app-tv-release.apk`](https://github.com/glowseedstudio/Vantafyn/releases/download/v0.9.8/app-tv-release.apk) *(15 MB)*

### Building from Source

```bash
# Clone the repository
git clone https://github.com/glowseedstudio/Vantafyn.git
cd Vantafyn

# Build Release or Debug APKs
./gradlew :app-mobile:assembleRelease :app-tv:assembleRelease

# Install directly to connected device
adb install -r app-mobile/build/outputs/apk/release/app-mobile-release.apk
```

---

## 📖 Architectural Documentation

Vantafyn features a clean, modular multi-module architecture. Detailed specifications and design documentation are available in the [`docs/`](docs/) directory:

- [Architecture Overview](docs/ARCHITECTURE.md) • [Design System & Materials](docs/DESIGN_SYSTEM.md)
- [Video Playback Pipeline](docs/PLAYBACK_IMPLEMENTATION.md) • [Music & Audio Stack](docs/MUSIC_IMPLEMENTATION.md)
- [Offline Downloads & Sync Engine](docs/OFFLINE_ARCHITECTURE.md) • [Server Admin Features](docs/ADMIN_FEATURES.md)

---

## 💖 Acknowledgements & Community

Vantafyn is built with love as a passion project for the Jellyfin community. Special thanks to:
- **[ZL154](https://github.com/ZL154)** — For the outstanding [Achievement Badges for Jellyfin](https://github.com/ZL154/AchievementBadges_for_Jellyfin) plugin that powers Vantafyn's gamification and social foundations.
- **[Jaakko Pasanen](https://github.com/jaakkopasanen)** — For the open-source [AutoEq](https://github.com/jaakkopasanen/AutoEq) project and dataset contributors (**oratory1990**, **crinacle**, **Rtings**, **Innerfidelity**).
- **[The Jellyfin Team](https://jellyfin.org)** — For building the premier free and open-source media system.
- **The AndroidX & Media3 Team** — For the rock-solid ExoPlayer foundations.

---

## ⚖️ License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
AutoEQ profiles and dataset integration are licensed under the [MIT License](https://github.com/jaakkopasanen/AutoEq/blob/master/LICENSE) courtesy of Jaakko Pasanen.

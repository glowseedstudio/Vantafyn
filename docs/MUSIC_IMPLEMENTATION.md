# Music Implementation

Vantafyn provides an audiophile-grade music streaming and offline playback engine engineered around AndroidX Media3 and custom DSP processing.

## 1. Single-Authority Playback Engine

Music playback is strictly centralized in `core-media`:
- `MusicPlaybackController` owns the single active `ExoPlayer` instance across the entire application.
- `VantafynMusicPlaybackService` implements Media3's `MediaLibraryService`.
- Background playback, lock screen controls, notification shade actions, Android Auto, Quick Settings tiles, and home screen widgets all observe and dispatch commands to this single service instance.
- Opening video playback cleanly suspends music playback.

## 2. Audiophile AutoEQ & DSP Engine

Vantafyn includes native hardware-level equalization backed by the open-source AutoEq calibration dataset:
- **6,000+ Headphone Profiles**: Direct access to compensation curves measured by Oratory1990, Crinacle, Rtings, and Innerfidelity.
- **Harman Target Equalization**: Automatically computes target gain offsets matching the neutral Harman target frequency response.
- **Hardware-Level 10-Band EQ**: Audio effects are attached directly to Android's hardware audio session (`android.media.audiofx.Equalizer`), avoiding software latency or transcoding degradation.
- **Real-Time ReplayGain Loudness Normalization**: Normalizes volume differences between tracks and albums using embedded ReplayGain tags with automatic pre-amp anti-clipping attenuation.

## 3. Streaming Quality & Network Policy

Users can configure independent audio quality policies for Wi-Fi and Cellular connections:
- **Lossless Original Master**: Direct-play bit-perfect FLAC or ALAC without server-side transcoding.
- **High Quality (320 kbps)**: High-bitrate MP3 or AAC.
- **Standard (192 kbps)**: Balanced quality and bandwidth.
- **Data Saver**: Low-bitrate streaming for constrained cellular networks.
- **Smart Network Switching**: Automatically transitions between profiles when switching between Wi-Fi and mobile networks.

## 4. UI & Interactive Playback Features

- **Dynamic Squiggly Wave Scrubber**: Features fluid kinetic wave animations during active playback that smoothly flatten to a calm linear bar when paused.
- **Synchronized Karaoke Lyrics**: High-precision scrolling lyrics synchronized to track millisecond timestamps.
- **4-Quadrant Playlist Collages**: Playlists automatically generate a dynamic 4-artwork cover grid composed of album art from tracks in the playlist.
- **Infinite Station Radio**: One-tap generation of an infinite radio queue based on the currently playing track, artist, or genre.
- **Quick Picks & On Repeat**: Server-backed listening history shelves for instant resumption of favorite tracks.
- **Dedicated Experience Mode**: Users can switch the entire application into a standalone "Music-Only" mode with a specialized 4-tab bottom navigation dock (Home, Explore, Library, Search).

## 5. Android Auto Integration

- `VantafynMusicMediaLibraryProvider` exposes Jellyfin browse trees to Android Auto.
- Drivers can browse Recently Added, Artists, Albums, Playlists, and custom Queues via automotive head-units.
- Selected items are adopted directly into `MusicPlaybackController` via `adoptSystemQueue(...)`.

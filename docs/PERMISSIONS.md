# Permissions

Vantafyn treats permissions as an integral part of the user experience. All runtime permissions must be explained clearly before the Android system prompt appears, must be strictly optional unless a feature cannot function without them, and must maintain a graceful denied state.

---

## Declared Permissions Overview

| Permission | Scope | Type | Required For |
| :--- | :--- | :--- | :--- |
| `android.permission.INTERNET` | All modules | Normal / Install-time | Jellyfin API, streaming media, artwork, WebSockets, Ombi, Achievement Badges |
| `android.permission.ACCESS_NETWORK_STATE` | All modules | Normal / Install-time | Connectivity monitoring, offline mode switching, network meter detection |
| `android.permission.POST_NOTIFICATIONS` | Mobile | Runtime (Android 13+ / API 33+) | Media playback controls, offline download progress, Watch Party / Social notifications |
| `android.permission.FOREGROUND_SERVICE` | Mobile | Normal / Install-time (Android 9+ / API 28+) | Background playback service and background data synchronization |
| `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Mobile | Normal / Install-time (Android 14+ / API 34+) | Media3 music/video foreground playback service |
| `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | Mobile | Normal / Install-time (Android 14+ / API 34+) | Offline media file downloads and media sync tasks |
| `android.permission.WAKE_LOCK` | Core Media | Normal / Install-time | Preventing CPU sleep during audio playback and background download transfers |

---

## 1. Notifications

- **Android Permission**: `android.permission.POST_NOTIFICATIONS`
- **Target Android Versions**: Runtime permission on Android 13+ (API 33+); automatically granted on Android 12 and below.
- **Optional**: Yes.
- **Why Vantafyn asks**:
  - To display Media3 notification playback controls (Play, Pause, Skip, Track Artwork, Progress Bar) on the lock screen and notification shade.
  - To show active background download progress when saving media for offline viewing.
  - To deliver incoming Watch Party session invites and Social chat notifications.
- **When Vantafyn asks**: When the user first starts or resumes media playback, starts an offline download, or manually toggles notifications in **Settings > Permissions**.
- **User-Facing Explanation**: *"Vantafyn uses notifications to keep media playing when your device is locked and to show playback and download progress."*
- **Privacy Guarantee**: Vantafyn only uses notifications for local device media and interactive session events. It never uses notifications for marketing, advertising, or telemetry.

### If Granted:
- Media playback continues smoothly in background with lock-screen and notification controls.
- Active downloads display real-time progress bars in the notification shade.

### If Denied or Dismissed:
- Media playback continues inside the app without lock-screen controls.
- Offline downloads complete silently in the background.
- Vantafyn does not re-prompt repeatedly after the user selects `Not now`.

---

## 2. Foreground Services & Background Execution

### Media Playback
- **Android Permissions**: `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- **Type**: Install-time permission (required by Android 14+ API 34+ for Media3 background audio services).
- **Service**: `VantafynMusicPlaybackService`
- **Notification Channel**:
  - Name: `Music playback`
  - Importance: `IMPORTANCE_LOW` (non-intrusive, silent)

### Data Synchronization & Offline Downloads
- **Android Permissions**: `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_DATA_SYNC`
- **Type**: Install-time permission (required by Android 14+ API 34+ for background file transfer services).
- **Service**: `VantafynDownloadService` / `OfflineSyncService`
- **Notification Channel**:
  - Name: `Media downloads`
  - Importance: `IMPORTANCE_LOW`

---

## 3. Network Access & Connectivity

- **Android Permissions**: `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`
- **Type**: Install-time permissions.
- **Why Vantafyn uses them**:
  - Communicating with authenticated Jellyfin servers over HTTPS / WebSockets.
  - Fetching library items, artist artwork, episode summaries, and subtitle tracks.
  - Dynamic bandwidth throttling based on cellular vs. Wi-Fi network state.
- **Offline Mode**: When network connectivity is lost, Vantafyn automatically adapts to offline storage without crashing or blocking the UI.

---

## 4. Power & CPU Wake Lock

- **Android Permission**: `android.permission.WAKE_LOCK`
- **Type**: Install-time permission.
- **Why Vantafyn uses it**:
  - Held strictly while active media playback or offline file transfer is in progress to prevent the OS CPU from sleeping during screen-off operation.
  - Automatically released when playback pauses or stops.

---

## 5. Hardware Features & Android TV Compatibility

Vantafyn declares all non-essential hardware features as `required="false"` to guarantee full compatibility across Android phones, tablets, foldables, and Android TV / Google TV devices:

- `android.software.leanback` (Required: `false`): Enables Android TV launcher integration while maintaining single-APK compatibility for mobile devices.
- `android.hardware.touchscreen` (Required: `false`): Ensures full D-Pad navigation support on TV boxes, remotes, and car head units.

---

## 6. Permissions Not Requested

Vantafyn is built with privacy-first principles:
- **No Location**: Does not ask for fine or coarse location.
- **No Camera / Microphone**: Does not request camera or audio recording permissions.
- **No Contacts / Phone State**: Does not access address books, device identifiers, or telephony state.
- **No External Storage**: Media is securely stored in app-specific scoped storage (`context.getExternalFilesDir()` / internal sandbox).

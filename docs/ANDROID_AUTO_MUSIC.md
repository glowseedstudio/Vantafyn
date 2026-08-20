# Android Auto Music Integration

## Overview
Vantafyn supports Android Auto via `MediaLibraryService` and a custom `VantafynMusicMediaLibraryProvider`.

## Architecture

### Service
**File**: `core-media/src/main/java/dev/vantafyn/core/media/VantafynMusicPlaybackService.kt`

Extends `MediaLibraryService` (not `MediaSessionService`) to support Android Auto browsing.

Key features:
- Notification with transport controls (previous/play-pause/next)
- Foreground service with `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`
- Media library browsing via `MediaLibrarySession.Callback`

### Media Library Provider
**File**: `core-media/src/main/java/dev/vantafyn/core/media/VantafynMusicMediaLibraryProvider.kt`

Provides browsable media tree for Android Auto:

```
Root
├── Recently Played (RECENT_ID)
├── Songs (SONGS_ID)
├── Queue (QUEUE_ID)
├── Albums (ALBUM_PREFIX)
│   └── {albumId}
├── Playlists (PLAYLIST_PREFIX)
│   └── {playlistId}
└── Search (SEARCH_PREFIX)
    └── {query}
```

### Playback Controller
**File**: `core-media/src/main/java/dev/vantafyn/core/media/MusicPlaybackController.kt`

Singleton controller managing ExoPlayer and playback state.

## Battery Considerations

### Wake Lock
- ExoPlayer uses `C.WAKE_MODE_NETWORK` to keep network alive for streaming
- Held for entire playback duration (expected behavior)
- Released when playback stops or service is destroyed

### Ticker
- Foreground: 1s updates for UI
- Background: 10s updates for notification/Android Auto
- Source: `AppForegroundStateRepository.isForeground`

### Notification Updates
- Initial: `startForeground()` (required by Android)
- Subsequent: `NotificationManager.notify()` (lightweight)
- Only updates on track change or play/pause state change

### Progress Reporting
- Foreground: 10s to Jellyfin server
- Background: 30s to Jellyfin server
- Ensures "Continue Watching" shows correct progress

## Android Auto Browsing

### Data Flow
1. Auto requests root → `onGetLibraryRoot()` returns root item
2. Auto requests children → `onGetChildren()` returns paged list
3. Auto plays item → `onSetMediaItems()` resolves queue and starts playback

### Queue Resolution
When Auto plays a track/album/playlist:
1. `resolveQueue()` fetches all tracks from Jellyfin
2. Creates `VantafynMusicTrack` list
3. `adoptSystemQueue()` updates ExoPlayer
4. Playback starts

### Search
1. Auto sends search query → `onSearch()`
2. `search()` fetches matching tracks from Jellyfin
3. `notifySearchResultChanged()` updates Auto UI
4. `onGetSearchResult()` returns paged results

## Debug Logs

```bash
# Service lifecycle
adb logcat -s MusicPlaybackService:D

# Playback controller
adb logcat -s MusicPlaybackController:D

# Media library provider
adb logcat -s MusicMediaLibraryProvider:D

# All music-related logs
adb logcat | grep -E "MusicPlayback|MusicMedia|MusicController"
```

## Testing

### Emulator
1. Start Android Auto emulator
2. Open Vantafyn in Auto
3. Browse Recently Played, Albums, Playlists
4. Play tracks, verify notification controls
5. Test search functionality

### Physical Device
1. Connect device via USB
2. Enable Android Auto developer mode
3. Launch Vantafyn in Auto
4. Verify browsing and playback
5. Test with screen off to verify background optimizations

# Battery Lifecycle Audit

## Overview
This document tracks all lifecycle-aware optimizations across the Vantafyn codebase to reduce battery drain when the app is backgrounded.

## Foreground State Detection

### AppForegroundStateRepository
**File**: `core-media/src/main/java/dev/vantafyn/core/media/AppForegroundStateRepository.kt`

Singleton that tracks app foreground state via `StateFlow<Boolean>`. Set by `HomeContentReveal` composable in `HomeScreens.kt`.

```kotlin
object AppForegroundStateRepository {
    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()
    fun setForeground(value: Boolean) { _isForeground.value = value }
}
```

## Lifecycle-Gated Animations

All animations are gated with `LocalLifecycleOwner` observer pattern and `AccessibilityManager.isTouchExplorationEnabled` for accessibility.

### 1. Background Drift Animation
**File**: `core-ui/src/main/java/dev/vantafyn/core/ui/VantafynComponents.kt`
- Component: `VantafynOnboardingBackground`
- Animation: `bgDrift` (25s translate cycle)
- Gate: `isResumed && !isTouchExplorationEnabled`

### 2. Search Sparkle
**File**: `feature-home/src/main/java/dev/vantafyn/feature/home/HomeScreens.kt`
- Component: `VantafynOnboardingBackground`
- Animation: `searchSparkle` (2s pulse)
- Gate: `isResumed`

### 3. What's New Dot
**File**: `feature-home/src/main/java/dev/vantafyn/feature/home/HomeScreens.kt`
- Component: `WhatsNewHeader`
- Animation: `whatsNewDot` (1.5s pulse)
- Gate: `isResumed`

### 4. Server Border Glow
**File**: `feature-home/src/main/java/dev/vantafyn/feature/home/HomeScreens.kt`
- Component: `ServerDiscoveryCard`
- Animation: `serverBorder` (1.2s glow)
- Gate: `isResumed`

### 5. Admin Task Progress
**File**: `feature-home/src/main/java/dev/vantafyn/feature/home/HomeScreens.kt`
- Component: `AdminTaskCard`
- Animation: `adminTaskProgress` (1.5s fade)
- Gate: `isResumed`

## Music Background Optimizations

### Position Ticker
**File**: `core-media/src/main/java/dev/vantafyn/core/media/MusicPlaybackController.kt`
- Foreground: 1s interval
- Background: 10s interval
- Source: `AppForegroundStateRepository.isForeground`

### Progress Reporting
**File**: `feature-music/src/main/java/dev/vantafyn/feature/music/MusicViewModel.kt`
- Foreground: 10s interval
- Background: 30s interval
- Source: `AppForegroundStateRepository.isForeground`

### Lyrics Prefetching
**File**: `feature-music/src/main/java/dev/vantafyn/feature/music/MusicViewModel.kt`
- Gated: `musicScreenActive && AppForegroundStateRepository.isForeground.value`
- Skips API calls when backgrounded

### Notification Updates
**File**: `core-media/src/main/java/dev/vantafyn/core/media/VantafynMusicPlaybackService.kt`
- Initial: `startForeground()` (required by Android)
- Subsequent: `NotificationManager.notify()` (lightweight)

## Monitoring

### Debug Logs
```bash
# Music service lifecycle
adb logcat -s MusicPlaybackService:D

# Music ticker and play state
adb logcat -s MusicPlaybackController:D

# All Vantafyn battery-related logs
adb logcat | grep -E "MusicPlayback|AppForeground|isResumed"
```

### Battery Stats
```bash
# Detailed battery usage
adb shell dumpsys batterystats

# App-specific battery usage
adb shell dumpsys batterystats --charged | grep -A 20 "vantafyn"
```

## Future Optimizations

1. **ExoPlayer wake mode**: Currently `WAKE_MODE_NETWORK` — consider `WAKE_MODE_NONE` when backgrounded if network not needed
2. **Audio offload**: Already enabled via `enableCompatibleAudioOffload()` — reduces CPU during playback
3. **Notification artwork**: Could skip loading large icon when backgrounded
4. **Palette extraction**: Not currently triggered in background, but should verify

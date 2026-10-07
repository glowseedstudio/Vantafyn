# Implementation Plan: Native NDS Emulation Core (melonDS via Libretro)

## Overview
This plan establishes a high-performance native C/C++ emulation pipeline for Nintendo DS inside Vantafyn, replacing the existing 32-bit WebAssembly/WebView runner (`EmulatorJS`). 

By adopting a native 64-bit (`arm64-v8a`) architecture:
1. **Memory Ceiling Resolved:** 512MB (4Gb) ROMs (*Pokemon Black/White 2*, etc.) run directly in 64-bit address space, eliminating WebAssembly memory crashes and renderer termination.
2. **True Native Performance:** Zero WebGL/JS bridge overhead, running with locked 60 FPS frame pacing and low-latency audio via `AudioTrack`.
3. **Foundation for Wireless Protocol:** Provides direct access to melonDS's internal network state and 802.11 DS wireless frames for future custom trading and battling protocols.

---

## Architecture Design

```
+-------------------------------------------------------------+
|                     Jetpack Compose UI                      |
|  - RetroTouchOverlay (D-Pad, ABXY, L/R, Touchscreen bottom) |
|  - GamePauseHud (Save/Load, Fast-Forward, Settings)         |
+-------------------------------------------------------------+
                              |
       Surface / Input State  |  Frame Rendering / Audio
                              v
+-------------------------------------------------------------+
|              Kotlin Native Emulator Manager                 |
|             (NativeLibretroSession / Bridge)                |
|  - SurfaceView / ANativeWindow lifecycle                    |
|  - AudioTrack streaming thread (lh_read_audio)              |
|  - State management (Pause, Resume, SRAM, SaveStates)       |
+-------------------------------------------------------------+
                              |  JNI
                              v
+-------------------------------------------------------------+
|             native_game_jni.c & libretro_host.c             |
|  (C11 Libretro Host based on Moonfin-Core implementation)   |
|  - Loads dynamic .so core via dlopen                        |
|  - Renders to ANativeWindow via OpenGL ES / direct blit     |
|  - Dual-screen compositing & touch coordinate translation   |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                 melonds_libretro_android.so                 |
|                 (Native ARM64 melonDS core)                 |
+-------------------------------------------------------------+
```

---

## Phased Implementation Roadmap

### Phase 1: Native Host & Build Integration
- **Module Structure:** Configure CMake in `:feature-player` (or create `:core-emulator`) referencing the portable C host in `_reference/Moonfin-Core/native/libretro_host`.
- **JNI Bridge:** Adapt `native_game_jni.c` to bind to Vantafyn's package namespace (`dev.vantafyn.feature.player.games.nativebridge`).
- **Core Provisioning:** Add logic in `GameStorageManager` to download or bundle the 64-bit `melonds_libretro_android.so` library into the app's internal native libraries directory.

### Phase 2: Kotlin Compose Rendering & Audio Engine
- **Composable Surface View:** Create `NativeGameSurfaceView` wrapped in Compose `AndroidView`, providing a `Surface` directly to native code via `ANativeWindow_fromSurface`.
- **Audio Pipeline:** Implement `NativeAudioPlayer` utilizing Android `AudioTrack` reading PCM samples from native ring buffer (`lh_read_audio`) with audio-sync pacing.
- **Save Management:** Connect native SRAM read/write and save states directly to Vantafyn's existing cloud and local storage backend.

### Phase 3: Touch Screen & Input Routing
- **Dual Screen Layout:** Configure melonDS options for vertical (stacked), horizontal (side-by-side), or single focused screen modes.
- **Touch Input Translation:** Map touch events on the lower screen partition directly to libretro pointer coordinates (`RETRO_DEVICE_POINTER` / `RETRO_DEVICE_LIGHTGUN`).
- **Controller & On-Screen Inputs:** Wire `RetroTouchOverlay` and physical gamepad inputs to the native input bitmask.

### Phase 4: Network Protocol Layer Preparation (Wireless Trading & Battling)
- **melonDS Wireless Hooks:** Tap into melonDS's local wireless packet pipeline.
- **Protocol Abstraction:** Design a transport abstraction (WebRTC DataChannel / WebSocket relay) to pass ad-hoc wireless packets between players for Pokemon trades and battles.

---

## Immediate Next Steps
1. Configure `externalNativeBuild` and CMake in `feature-player/build.gradle.kts`.
2. Port `libretro_host` and `native_game_jni.c` into `feature-player/src/main/cpp/`.
3. Build and verify compilation with NDK.

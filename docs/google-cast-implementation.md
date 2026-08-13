# Google Cast Implementation

Vantafyn is an Android-only application. This Cast integration uses Google's official Android Cast Application Framework and the Default Media Receiver. No iOS, CocoaPods, Swift, Objective-C, or cross-platform iOS setup is involved.

## Repository Findings

- App architecture: multi-module Kotlin Android project with Gradle Kotlin DSL.
- UI: Jetpack Compose for mobile and TV, with Android `View` interop where required for platform widgets.
- Jellyfin access: `core-jellyfin` owns session/auth/API logic. UI modules consume app repositories/view models rather than calling the Jellyfin SDK directly.
- Local music playback: `core-media` owns `MusicPlaybackController`, ExoPlayer-backed playback state, queue, position, shuffle, repeat, and existing Android media-session service integration.
- Video playback: `feature-player` currently owns the mobile video player directly with Media3 ExoPlayer.
- State management: Kotlin `StateFlow` and Compose state.
- Secure storage: existing Jellyfin/session storage lives in `core-jellyfin`.
- Build flavours: no Android product flavours currently exist in `app-mobile` or `app-tv`.

## Dependency

Added:

```toml
playServicesCast = "22.3.1"
mediarouter = "1.8.1"
play-services-cast-framework = { module = "com.google.android.gms:play-services-cast-framework", version.ref = "playServicesCast" }
androidx-mediarouter = { module = "androidx.mediarouter:mediarouter", version.ref = "mediarouter" }
```

`play-services-cast-framework` 22.3.1 is used for the Cast Application Framework. AndroidX MediaRouter 1.8.1 supplies the official `MediaRouteButton`.

## Module Structure

Added `core-cast` so Google Cast SDK usage stays behind a small app-owned abstraction.

Key classes:

- `RemotePlaybackTarget`: SDK-neutral remote playback contract.
- `GoogleCastPlaybackTarget`: Cast SDK implementation using `CastContext`, `SessionManager`, `SessionManagerListener`, `CastSession`, `RemoteMediaClient`, `MediaLoadRequestData`, and Cast queue APIs.
- `PlaybackOutputCoordinator`: coordinates local music playback and Google Cast so only one output owns playback.
- `GoogleCastRouteButton`: Compose wrapper around Google's official `MediaRouteButton`.
- `CastUrlSecurity`: validates Cast-reachable URLs and redacts token query parameters.
- `CastReceiverConfiguration`: future receiver configuration model. Default receiver is active now; custom receiver remains disabled.

Feature flags:

- `googleCastEnabled = true`
- `customCastReceiverEnabled = false`

Because the project has no Google-free or libre product flavour yet, there is no Cast-disabled build variant to wire. If one is added later, `core-cast` should be excluded from that variant and `googleCastEnabled` should default to false there.

## Manifest

`app-mobile` registers the Cast options provider:

```xml
<meta-data
    android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
    android:value="dev.vantafyn.core.cast.VantafynCastOptionsProvider" />
```

The provider uses:

```kotlin
CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
```

No custom receiver ID is copied or hardcoded.

## UI Placement

The official `MediaRouteButton` is shown in:

- Mobile full-screen video player controls.
- Music now-playing screen.
- Music mini-player.
- Home quick music player sheet.

It is not placed in the primary bottom navigation rail.

## Local To Cast Transfer

When a Cast session connects, `PlaybackOutputCoordinator` captures the active local music queue, current item, queue index, playback position, artwork, metadata, and stream URL from `MusicPlaybackController`.

It then queue-loads the receiver through `RemoteMediaClient`. Local music playback is stopped only after the Cast receiver accepts the load. If the Cast load fails, local playback is left running and the coordinator exposes a user-safe error.

The coordinator also handles the opposite order: if a user connects Cast first and starts music afterward, the active queue is transferred once playback begins.

## URL And Token Safety

The Cast receiver fetches media directly from Jellyfin, so the stream and artwork URLs must be absolute URLs reachable from the Cast receiver. `CastUrlSecurity` rejects loopback and phone-only addresses such as:

- `localhost`
- `127.0.0.1`
- `10.0.2.2`
- `.localhost`

`CastUrlSecurity.redact` redacts:

- `api_key`
- `token`
- `access_token`
- `X-Emby-Token`
- `X-MediaBrowser-Token`

Authenticated URLs are not logged, displayed, persisted, or written to docs/tests.

## Current Limitations

This pass implements the Cast sender abstraction, discovery/device chooser, connection state, queue load, playback controls, seek, previous/next, metadata, artwork, receiver volume/mute commands, session reconnection observation, and local-to-Cast transfer for current music playback.

Still needed for full production parity:

- Cast-specific Jellyfin playback-info negotiation and device profile selection.
- Preferred Cast server address setting and validation UI.
- Explicit Cast streaming quality setting and bitrate mapping.
- Cast-to-local transfer that resumes local playback from receiver position.
- Jellyfin playback-progress reporting while the Default Media Receiver owns playback.
- Cast notification/lock-screen integration beyond the Cast SDK's standard session controls.
- Fake-based coordinator tests for all state transitions.
- Instrumentation tests for route button visibility and connected state.
- Android Auto/Cast interaction testing.

These gaps are documented because implementing them honestly requires deeper Jellyfin playback-info integration rather than only loading the existing local stream URLs into the Default Media Receiver.

## Manual Test Checklist

Test with:

- Chromecast.
- Chromecast with Google TV.
- Google TV with built-in Cast.
- Nest or another audio-only Cast receiver.
- Direct-play MP3 and AAC.
- FLAC and a format that requires Jellyfin transcoding.
- Long audiobook.
- Seek, next, previous, repeat, and shuffle.
- App background and foreground.
- Phone lock and unlock.
- Receiver disconnect.
- Another sender taking control.
- Jellyfin behind a reverse proxy.
- Jellyfin hosted under a non-root base path.
- Expired or revoked Jellyfin session.

Expected first-pass behavior:

- Cast chooser opens from the official route button.
- Connecting a receiver transfers the current music queue after successful receiver load.
- Failed receiver load leaves local playback running.
- Metadata and artwork are sent to the receiver when the stream/artwork URLs are Cast-reachable.
- Loopback or emulator-only server URLs are rejected with a clean user-facing explanation.

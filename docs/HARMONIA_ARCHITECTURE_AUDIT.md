# Harmonia Architecture Audit

## Scope

This audit covers the first Harmonia implementation pass: data capture, statistics models, generation, persistence, and test/developer generation hooks. It intentionally does not add the large recap UI, redesign Music, or modify TV.

## Existing Music Architecture

- `feature-music/src/main/java/dev/vantafyn/feature/music/MusicViewModel.kt` owns the mobile Music UI state and delegates playback to the shared controller.
- `core-media/src/main/java/dev/vantafyn/core/media/MusicPlaybackController.kt` owns the single Media3/ExoPlayer music player and emits `VantafynMusicPlaybackEvent`.
- `core-media/src/main/java/dev/vantafyn/core/media/VantafynMusicPlaybackService.kt` owns foreground playback, notification, lock-screen, widget, and Android Auto integration.
- `core-media/src/main/java/dev/vantafyn/core/media/VantafynMusicMediaLibraryProvider.kt` exposes Jellyfin music to Android Auto and adopts system-selected queues into the same `MusicPlaybackController`.
- `core-jellyfin/src/main/java/dev/vantafyn/core/jellyfin/JellyfinSdkRepositories.kt` owns Jellyfin music, playback-info, and play-state API access.

## Existing Playback History

Vantafyn already reports music playback to Jellyfin via:

- `JellyfinPlaybackRepository.reportStarted`
- `JellyfinPlaybackRepository.reportProgress`
- `JellyfinPlaybackRepository.reportStopped`

These calls update Jellyfin server-side play state, but Vantafyn did not have a durable local music listening-history table suitable for deterministic Harmonia generation. Harmonia now listens to the existing `MusicPlaybackController.events` stream and records completed local listening records without creating another player or changing playback ownership.

## Existing Metadata

`JellyfinMusicTrack` already carried:

- track id
- title
- artist label
- album title
- album id
- duration
- artwork
- favorite state
- stream URL

The Jellyfin repository was already requesting `ItemFields.GENRES`, but the music model did not preserve genres. This pass adds `genres: List<String> = emptyList()` to `JellyfinMusicTrack` and `VantafynMusicTrack` so Harmonia can calculate genre statistics when Jellyfin provides real genre values.

## Existing Persistence

The project uses:

- SharedPreferences for sessions, settings, UI preferences, social local state, and widget state.
- A lightweight SQLiteOpenHelper implementation in `core-downloads` for downloads/offline mutation persistence.

Harmonia follows the existing lightweight SQLite pattern rather than adding Room or another persistence stack.

## New Harmonia Architecture

New package:

`feature-music/src/main/java/dev/vantafyn/feature/music/harmonia`

Files:

- `HarmoniaModels.kt`: period, recap, statistics, ranked items, aggregation row models, availability and confidence metadata.
- `HarmoniaPeriodCalculator.kt`: local-time calendar month/year period boundaries.
- `HarmoniaStatisticsCalculator.kt`: pure deterministic statistics aggregation.
- `HarmoniaGenerator.kt`: idempotent monthly/yearly recap generation.
- `SqliteHarmoniaStore.kt`: local playback-history and recap cache persistence.
- `HarmoniaPlaybackTracker.kt`: converts existing music playback events into durable listening records.

## Data Sources And Confidence

High-confidence local statistics:

- total listening time
- total tracks played
- unique tracks
- unique artists
- unique albums when album id/title exists
- average completed listening record
- longest listening day
- most active day
- most active hour
- listening streaks from local recorded days
- top artists
- top tracks
- top albums
- listening by month/day/hour
- day-of-week distribution
- daily heatmap
- monthly/yearly separation
- previous-month comparison when local previous-month data exists

Conditional statistics:

- top genres
- unique genres
- most dominant genre

These require real Jellyfin genre values on music items. Harmonia marks them unavailable when the recorded tracks do not contain genres.

Unavailable in this first pass:

- first-time artists
- first-time tracks

These need reliable local history before the recap period. Harmonia does not claim them until enough pre-period history exists.

## Period Detection

`HarmoniaPeriodCalculator` uses the user's local `ZoneId` by default:

- Monthly: current calendar month or previous completed calendar month.
- Yearly: current local calendar year.

This avoids UTC boundary errors around local listening days/months.

## Privacy

Every playback record is keyed by:

- `userId`
- `serverId`
- `profileId`

Generation queries require all three values, so recaps cannot mix users, Jellyfin servers, or Vantafyn profiles.

No Harmonia data is uploaded to external analytics services.

## Temporary Developer/Test Controls

Temporary generation hooks are public methods on:

`feature-music/src/main/java/dev/vantafyn/feature/music/MusicViewModel.kt`

- `generateTestMonthlyHarmonia(previousCompletedMonth: Boolean = true)`
- `generateTestYearlyHarmonia()`

They use the current user's real locally recorded listening data. They are not wired into production UI in this pass and can be removed cleanly once the final Harmonia UI/workflow exists.

State outputs:

- `MusicUiState.isHarmoniaGenerating`
- `MusicUiState.lastHarmoniaGenerationResult`
- `MusicUiState.harmoniaGenerationMessage`

## Tests

Added tests in:

`feature-music/src/test/java/dev/vantafyn/feature/music/harmonia/HarmoniaStatisticsCalculatorTest.kt`

Coverage includes:

- local timezone period calculation
- yearly period boundaries
- total listening time
- top artist
- top track
- top album
- top genre
- daily aggregation
- hourly aggregation
- heatmap aggregation
- monthly/yearly separation
- empty/insufficient data
- duplicate generation
- user isolation

## Known Limitations

- Harmonia starts collecting durable local history from this version forward; Jellyfin server historical listening data is not imported in this pass.
- Jellyfin play-state reporting is still the source for server playback state, but Harmonia uses local completed playback records for deterministic recap generation.
- Genre statistics depend on Jellyfin returning real genre values for audio items.
- Recap cache rows are persisted with summary metadata for this architecture pass; the final recap UI pass can expand serialization as its presentation contract firms up.


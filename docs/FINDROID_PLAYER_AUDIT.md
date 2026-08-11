# Findroid Player Audit

Findroid was cloned only as reference into `_reference/Findroid` and remains ignored by Git. No Findroid branding, package names, visual styling, or source files were imported into Vantafyn.

## Files Inspected

- `_reference/Findroid/player/local/src/main/java/dev/jdtech/jellyfin/player/local/presentation/PlayerViewModel.kt`
- `_reference/Findroid/app/phone/src/main/java/dev/jdtech/jellyfin/presentation/player/TrackSelectionDialogFragment.kt`
- `_reference/Findroid/player/local/src/main/java/dev/jdtech/jellyfin/player/local/domain/PlaylistManager.kt`
- `_reference/Findroid/player/local/src/main/java/dev/jdtech/jellyfin/player/local/domain/Extensions.kt`
- `_reference/Findroid/data/src/main/java/dev/jdtech/jellyfin/repository/JellyfinRepositoryImpl.kt`

## Relevant Concepts

- Findroid requests playback info with a Jellyfin device profile and subtitle profiles.
- External subtitle media streams are converted into Media3 subtitle configurations before playback starts.
- Audio/text selection is performed with Media3 track selection parameters and `TrackSelectionOverride`.
- Subtitle Off is implemented by clearing overrides for text and disabling the text track type.
- Playback speed is applied directly to the active player.
- Track labels are built from Media3 format label, language, and codec instead of showing raw stream dumps.

## Adapted In Vantafyn

- Vantafyn now attaches Jellyfin external subtitle URLs to the Media3 `MediaItem`.
- Vantafyn now switches audio/subtitle tracks in-place using Media3 track selection parameters.
- Vantafyn keeps its own Compose control surface and Vantafyn dark/glass design language.
- Vantafyn preserves Jellyfin stream indexes in app state for playback reporting.

## Not Copied

- Findroid Activities, fragments, XML layouts, themes, colours, strings, package names, MPV implementation, gesture helper, trickplay implementation, and unrelated app architecture were not copied.

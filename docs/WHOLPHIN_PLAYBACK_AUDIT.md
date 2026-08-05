# Wholphin Playback Audit

Scope: conceptual reference only. No Wholphin source was copied into Vantafyn, and Wholphin remains isolated under `_reference/Wholphin`.

## Findings

Wholphin separates playback into service/view-model/player concerns rather than placing Jellyfin SDK calls directly in UI. The relevant areas are playback view models, stream-choice services, player factories, playback lifecycle observers, device-profile utilities, and subtitle/audio track helpers.

Concepts identified:

- Playback info is requested through Jellyfin playback-info APIs using a client device profile.
- Media source choice is treated as a separate decision from player UI.
- Direct play is preferred when the server and selected media source allow it.
- Transcoding is available through Jellyfin-provided transcoding URLs when direct playback is not suitable.
- Audio and subtitle streams are modeled from Jellyfin media stream metadata and surfaced to the player layer.
- Playback start, progress, and stop are reported through Jellyfin play-state APIs.
- Progress reporting is lifecycle-aware and tied to active playback state.
- Subtitle handling is a real compatibility area; image/ASS/PGS style subtitles may require encode/burn-in or specialized parser support.
- Live TV needs extra care around live stream ids, opening/closing live streams, and server transcoding sessions.

## Vantafyn Takeaways

- Keep SDK/session/playback info logic in `core-jellyfin`.
- Keep Media3 player ownership in `feature-player`/`core-media`.
- Use Direct Play first, then Jellyfin transcoding fallback.
- Report playback lifecycle events through a repository boundary.
- Start with a conservative Android mobile device profile and document unsupported subtitle/live-TV cases.
- Do not expose passwords or tokens in logs.

## Confirmation

No Wholphin source files were copied into Vantafyn. This audit only informed architecture and compatibility concerns.

# Wholphin Detail Actions Audit

Wholphin was inspected only as conceptual reference. No Wholphin source files were copied into Vantafyn and Wholphin remains isolated under `_reference/Wholphin`.

## Observed Structure

Wholphin appears to separate detail experiences by media type:

- movie detail pages
- series detail pages
- episode detail pages
- music detail pages
- collection/library pages
- playback-specific view models and stream-choice helpers

The project also has dedicated services/data stores around favorite/watch state and playback history/language choices.

## Useful Concepts For Vantafyn

- Detail actions should be media-type aware, not one generic placeholder menu.
- Series pages need clear season/episode navigation rather than a single static episode row.
- Resume/start position should be explicit when entering playback.
- Favorite/watch state should be handled as real user data, not local-only UI state.
- Media stream details belong in a readable technical panel rather than a debug dump.

## Vantafyn Differences

- Vantafyn uses its own Compose mobile detail UI and Jellyfin Kotlin SDK repositories.
- Vantafyn maps My List to Jellyfin favorite state for now.
- Vantafyn does not expose destructive delete/remove actions.
- Vantafyn does not yet implement TV parity for these detail actions.
- Vantafyn currently keeps pre-playback track/source selection limited and leaves full switching to the player.

## Confirmation

This pass used Wholphin only to confirm high-level UX/API directions. No GPL source was copied or imported.

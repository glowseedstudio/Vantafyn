# Offline Sync Policy

This policy applies to locally stored user state created while the Jellyfin server is unavailable.

## Stored Mutations

Vantafyn stores pending mutations per:

- `serverId`
- `userId`
- `itemId`

Supported initial mutation fields:

- playback position ticks
- played state
- updated timestamp

Favorites can be added later only after conflict handling is explicit.

Current implementation status:

- Offline video playback writes local resume state and creates a pending mutation when leaving the player.
- `OfflineUserDataSyncWorker` reconciles pending mutations through the existing Jellyfin repositories after a saved profile is restored and network is available.
- Sync is scheduled after login/profile restore/Quick Connect, when the app foregrounds with an active session, and after a local offline playback mutation is written.
- The worker reports a stopped playback position and marks the item played when the local mutation explicitly says it was played.

## Conflict Policy

- Never blindly move Jellyfin progress backwards.
- If local state marks an item played, sync `played = true` unless the server has newer explicit user data showing an intentional reset.
- If local state is partial progress and server progress is greater, keep server progress.
- If local state is greater than server progress, sync local progress.
- If local state is near zero because playback was intentionally restarted, only sync the reset when Vantafyn can prove it was intentional.

Jellyfin timestamp availability still needs a deeper audit. The current worker syncs the last local stopped position and played flag because this mirrors how the existing player reports normal playback; it does not yet compare newer server-side user-data timestamps.

## Failure Handling

Failed sync attempts remain pending with a retry count and sanitized failure category. Tokens, URLs with credentials, and raw stack traces must not be logged.

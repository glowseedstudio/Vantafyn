# Watch Party Invite Transport Audit

## APIs Found

The installed Jellyfin Kotlin SDK exposes active session and command APIs:

- `SessionApi.getSessions(...)`
- `SessionApi.sendMessageCommand(sessionId, MessageCommand)`
- `SessionApi.play(...)`
- `SessionApi.sendPlaystateCommand(...)`
- `SessionApi.sendGeneralCommand(...)`

The SDK also exposes websocket infrastructure:

- `SocketApi.subscribeAll()`
- `subscribeGeneralCommands(...)`
- `subscribePlayStateCommands(...)`
- `subscribeSyncPlayCommands(...)`

## Current Vantafyn State

Vantafyn keeps a Jellyfin websocket listener alive while Watch Party messaging/sync needs it. The listener handles:

- SyncPlay group updates
- SyncPlay playback commands
- playstate commands
- active session updates
- general command messages used for Vantafyn Watch Party invites and admin display messages

## Chosen First Implementation

Phase 2 invites use Jellyfin active sessions:

1. Vantafyn loads currently active sessions from the active Jellyfin server.
2. The sender chooses one or more active recipients.
3. Vantafyn creates a real SyncPlay group if needed.
4. Vantafyn sends a `MessageCommand` to the selected active session ids.

The message includes a compact `VANTAFYN_WATCH_PARTY_INVITE` payload. Vantafyn parses this from incoming Jellyfin general command messages and shows the premium invite card when the target app session is active and connected.

## Limitations

- Invites are active-session only.
- Closed-app or background push delivery is not implemented.
- Push notifications require a future backend and FCM implementation.
- Jellyfin may expose only sessions visible to the current user/server policy.

The UI reflects this with copy: invites are delivered through Jellyfin active sessions and push requires a future backend.

## No Fake Push

No fake push notification path was added. No closed-app delivery is presented as working.

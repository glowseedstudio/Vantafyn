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

Vantafyn does not yet keep an app-wide Jellyfin websocket listener alive for logged-in sessions. Because of that, this pass does not claim reliable Vantafyn-to-Vantafyn invite receipt.

## Chosen First Implementation

Phase 2 invites use Jellyfin active sessions:

1. Vantafyn loads currently active sessions from the active Jellyfin server.
2. The sender chooses one or more active recipients.
3. Vantafyn creates a real SyncPlay group if needed.
4. Vantafyn sends a `MessageCommand` to the selected active session ids.

The message includes a compact `VANTAFYN_WATCH_PARTY_INVITE` payload that a future Vantafyn websocket listener can parse.

## Limitations

- Invites are in-app/active-session only.
- Closed-app or background push delivery is not implemented.
- Push notifications require a future backend and FCM implementation.
- Cross-device premium receive cards require the app-wide websocket listener phase.
- Jellyfin may expose only sessions visible to the current user/server policy.

The UI reflects this with copy: invites are delivered through Jellyfin active sessions and push requires a future backend.

## No Fake Push

No fake push notification path was added. No closed-app delivery is presented as working.

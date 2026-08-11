# Jellyfin Realtime Listener

Vantafyn uses the Jellyfin Kotlin SDK websocket attached to the authenticated `ApiClient`.

## Lifecycle

- Starts after successful login, profile restore, recovery restore, or Quick Connect authorization.
- Resumes on mobile foreground.
- Stops on background when there is no active Watch Party.
- Stops and clears pending invite state on logout, profile switch, or server switch.
- A single ViewModel-owned job prevents duplicate websocket collectors.
- Recoverable disconnects enter `Reconnecting` and retry with capped exponential backoff.

## Events

Handled websocket events:

- connection state
- active sessions
- SyncPlay group updates
- SyncPlay commands
- playstate commands
- general commands

Watch Party invites are parsed from Jellyfin general/session message payloads by `WatchPartyInviteEventMapper`.

## Security

The realtime path does not log passwords, tokens, auth headers, API keys, or full invite payloads. Invite payloads contain only non-secret IDs and display metadata required for in-app routing.

## Limitations

- This is not push notification delivery.
- The receiver must have Vantafyn open and connected.
- Sender-side delivery remains honest: Vantafyn can say sent through Jellyfin active sessions, but not delivered unless a future acknowledgement transport exists.
- Exact SyncPlay member readiness/sync labels are only shown when Jellyfin exposes usable state.

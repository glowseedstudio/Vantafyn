# SyncPlay Realtime State Audit

## Current Real Commands

Vantafyn already uses real Jellyfin SDK calls for Watch Party foundations:

- `SyncPlayApi.syncPlayCreateGroup(...)`
- `syncPlayGetGroups(...)`
- `syncPlayJoinGroup(...)`
- `syncPlayLeaveGroup(...)`
- `syncPlayPause(...)`
- `syncPlayUnpause(...)`
- `syncPlaySeek(...)`
- `syncPlaySetNewQueue(...)`
- `SessionApi.getSessions(...)`
- `SessionApi.sendMessageCommand(...)`

## Websocket APIs Found

The installed Jellyfin Kotlin SDK exposes websocket support from the authenticated `ApiClient`:

- `api.webSocket.state`
- `api.webSocket.subscribeAll()`
- `subscribeSyncPlayCommands(...)`
- `subscribePlayStateCommands(...)`
- `subscribeGeneralCommands(...)`

Message types confirmed in the SDK:

- `SessionsMessage`
- `SyncPlayCommandMessage`
- `SyncPlayGroupUpdateCommandMessage`
- `PlaystateMessage`
- `GeneralCommandMessage`

## Implemented In This Pass

`core-jellyfin` now has:

- `JellyfinRealtimeClient`
- `JellyfinWebSocketEvent`
- `SdkJellyfinRealtimeClient`
- event mapping for socket state, sessions, SyncPlay commands, SyncPlay group updates, playstate commands, and general commands
- SyncPlay group update metadata extraction for queued item, position, and playing state where Jellyfin supplies it

`feature-home` now:

- starts realtime collection when Watch Party UI/actions need it
- stops realtime collection when the party is left or the profile logs out
- stores realtime connection state
- stores active session/member state from real `SessionsMessage` data
- maps incoming SyncPlay/playstate websocket events into `VantafynSyncPlaybackCommand`
- starts the synced queue item through the existing playback target path when another group member changes the queue
- shows an honest Watch Party player pill
- shows Vantafyn lobby readiness separately from Jellyfin sync readiness

`feature-player` now:

- receives the current `VantafynSyncPlaybackCommand`
- applies pause, resume, seek, stop, and queue position updates to the existing Media3 player
- routes the same commands through the existing Cast coordinator when the item is cast
- publishes local pause, resume, and seek controls back through Jellyfin SyncPlay while a Watch Party is active
- ignores commands for unrelated item ids
- consumes each command key once to prevent duplicate application during recomposition

## Honest State Boundaries

Jellyfin websocket events confirm active session presence and SyncPlay command/group update activity. They do not, by themselves, prove every device is exactly synchronized at a shared clock position.

Because of that, Vantafyn does not show `Synced` in this pass. It shows:

- `Watch Party active`
- `Sync unknown`
- `Sync state unavailable`
- `Reconnecting`
- `Solo fallback`

## Ready State

Jellyfin SyncPlay does expose ready/buffer request commands in the SDK, but this pass does not yet have a confirmed member-ready event model suitable for reliable per-user UI.

Vantafyn therefore implements only local lobby readiness:

- it is labelled as Vantafyn lobby readiness
- it is not used to claim Jellyfin playback sync
- unknown member readiness remains unknown

## Buffering State

No reliable per-member buffering event is surfaced in the current implementation. The UI does not invent buffering state. Local player buffering remains handled by Media3.

## Up Next

Solo Up Next remains enabled.

During Watch Party playback, solo Up Next countdown/autoplay is suppressed to avoid participant desync. Group-aware Up Next remains future work and must be host-owned.

## Lifecycle

Realtime collection is tied to the ViewModel and active Watch Party usage:

- starts on Watch Party load/create/recipient discovery
- stops on leave party
- stops on logout/profile removal
- cancels in `onCleared`

No aggressive background polling was added.

## Remaining Work

- accept/decline feedback transport
- member ready/buffer reconciliation if Jellyfin emits reliable state
- host-owned group Up Next
- player member/status sheet
- TV UI reuse

## Validation

The Android app and TV app compile with:

`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk ./gradlew --console=plain :app-mobile:assembleDebug :app-tv:assembleDebug`

Multi-device runtime validation is still required on a real Jellyfin server to verify host/guest timing, mid-playback joins, reconnect behaviour, and next-episode group behaviour across separate devices.

# Vantafyn Watch Party / SyncPlay Architecture

## Scope

Watch Party is Vantafyn's mobile-first social playback system. It is backed by Jellyfin SyncPlay APIs where the Kotlin SDK exposes them. Vantafyn must not pretend playback is synchronized unless a real Jellyfin SyncPlay group command has been sent and the player is connected to that group state.

This pass implements the foundation and first usable mobile flow:

- shared Watch Party and SyncPlay models in `core-jellyfin`
- a `JellyfinWatchPartyRepository` boundary
- real Jellyfin SyncPlay create, join, leave, pause, resume, seek, and queue calls
- active-session invite recipient discovery and sender-side invite delivery through Jellyfin `MessageCommand`
- real Jellyfin candidate generation for the swipe matcher
- a premium mobile Watch Party screen reachable from Profile
- local first-pass matching state for the active profile

## Jellyfin SDK APIs Used

The installed Jellyfin Kotlin SDK exposes `SyncPlayApi` through `api.syncPlayApi`.

Current calls:

- `syncPlayCreateGroup(NewGroupRequestDto(groupName))`
- `syncPlayGetGroups()`
- `syncPlayJoinGroup(JoinGroupRequestDto(groupId))`
- `syncPlayLeaveGroup()`
- `syncPlayPause()`
- `syncPlayUnpause()`
- `syncPlaySeek(SeekRequestDto(positionTicks))`
- `syncPlaySetNewQueue(PlayRequestDto(...))`

Candidate discovery uses:

- `ItemsApi.getItems(GetItemsRequest(...))`
- `ItemsApi.getResumeItems(GetResumeItemsRequest(...))`

## Module Ownership

`core-jellyfin` owns:

- `WatchPartySession`
- `WatchPartyMember`
- `WatchPartyRules`
- `WatchPartyCandidate`
- `WatchPartyVote`
- `WatchPartyMatch`
- `SyncPlayCommand`
- `JellyfinWatchPartyRepository`
- SDK-backed SyncPlay and candidate API calls

`feature-home` owns:

- mobile navigation entry point
- Watch Party UI state coordination
- local swipe vote handling for this first pass
- starting matched playback through the existing playback route
- detail-page Watch Party mode picker
- mobile recipient picker and sender invite animation

`feature-player` remains the player surface. `feature-home` overlays the current mobile Watch Party pill while player-specific member sheets remain pending.

## Current Limitation

This is not yet full synchronized multi-device playback. Jellyfin group commands are real, and Vantafyn now subscribes to Jellyfin websocket/session events for connection, active sessions, SyncPlay commands, and group updates. The websocket events do not by themselves prove exact clock sync across all players, so Vantafyn still does not show `Synced`.

Until that is implemented:

- Vantafyn can create or leave a real SyncPlay group.
- Vantafyn can set the group queue when starting a matched item.
- The swipe matcher uses local vote state only.
- The UI does not claim remote participants are synchronized.
- Vantafyn lobby readiness is local readiness, not Jellyfin playback readiness.
- Invite sending is real for active Jellyfin sessions. Mobile now listens app-wide for Vantafyn invite messages while open/connected and shows a premium top-slide receiver card.
- Accept joins the real Jellyfin SyncPlay group and opens the Watch Party lobby. Decline dismisses locally. Sender acknowledgement remains delivery-unknown until Jellyfin exposes a reliable response transport or Vantafyn adds a backend/plugin.

## TV Reuse Plan

TV should reuse the `core-jellyfin` models and repository, but it needs its own D-pad-first layouts:

- landing/manage party screen
- focused rules selector
- poster-forward swipe/choose deck adapted for remote controls
- player group indicator
- host controls sheet

No TV Watch Party UI is implemented in this pass.

The realtime listener and invite mapper live outside TV-specific UI and can be reused by a future Android TV receiver card.

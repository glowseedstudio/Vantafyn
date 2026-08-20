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

## Current Playback Integration

Vantafyn now treats Jellyfin SyncPlay as the playback authority for Watch Party sessions.

- Creating, joining, leaving, queue start, pause, resume, and seek use native Jellyfin SyncPlay SDK calls.
- Incoming Jellyfin SyncPlay websocket events are mapped into a single `VantafynSyncPlaybackCommand`.
- `feature-home` forwards that command into the existing `feature-player` surface.
- `feature-player` applies the command to the existing Media3 player or active Cast route.
- Local player pause, resume, and seek actions publish native Jellyfin SyncPlay commands when a Watch Party is active.

This deliberately does not create another player, another MediaSession, another progress loop, or a Companion-plugin playback sync path.

The mobile app can now:

- create a real SyncPlay group
- invite active Jellyfin sessions through native Jellyfin display messages
- accept an invite and join the real SyncPlay group
- start a matched or fixed title by setting the SyncPlay queue
- apply incoming SyncPlay play queue, play, pause, and seek commands to the active player
- send local pause, resume, and seek controls back to the active SyncPlay group
- keep solo playback paths unchanged when no Watch Party is active

The UI still avoids overclaiming exact per-device clock accuracy. Jellyfin owns that group timing, while Vantafyn presents the active party state and applies Jellyfin's commands to its existing player.

## Companion Boundary

The Vantafyn Companion plugin contains a Watch Party orchestration foundation: party records, invitations, participants, snapshots, and a realtime transport abstraction.

The Android app does not currently depend on those plugin endpoints for playback sync. That is intentional:

- Jellyfin SyncPlay remains the playback sync source of truth.
- Companion should be used only for Vantafyn-specific orchestration such as richer invitations, metadata, user/device mapping, and future deep-link/push support.
- Servers without the Companion plugin can still use the current active-session invite and native SyncPlay flow.

## Remaining Work

The remaining Watch Party work is product/orchestration hardening rather than a second playback stack:

- persistent Companion-backed invitations and party metadata
- richer accept/decline feedback to the host
- host-owned group Up Next
- deeper reconnect/member-state presentation
- player member/status sheet
- TV UI reuse

## Compatibility Notes

- The swipe matcher still uses Vantafyn app state for choosing a title together.
- Vantafyn lobby readiness is separate from Jellyfin playback readiness.
- Invite delivery uses Jellyfin active sessions and works only for reachable sessions.
- Closed-app push delivery remains a future Companion/backend feature.

## TV Reuse Plan

TV should reuse the `core-jellyfin` models and repository, but it needs its own D-pad-first layouts:

- landing/manage party screen
- focused rules selector
- poster-forward swipe/choose deck adapted for remote controls
- player group indicator
- host controls sheet

No TV Watch Party UI is implemented in this pass.

The realtime listener and invite mapper live outside TV-specific UI and can be reused by a future Android TV receiver card.

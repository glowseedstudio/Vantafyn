# Swipe Match Picker

## Goal

The Watch Party picker helps people choose what to watch without turning the app into a settings screen. It uses real Jellyfin media candidates and a restrained Vantafyn treatment: dark glass, poster/backdrop art, simple metadata, and vivid action colour only when voting.

## Candidate Sources

Candidates are fetched through `JellyfinWatchPartyRepository.getCandidates(...)`.

Supported filters in this pass:

- movies only
- TV shows only
- movies and TV
- continue watching
- My List / favorites
- recently added
- genre list when provided by future UI
- unwatched only
- PG-and-under family-friendly mode
- runtime limits, applied locally after Jellyfin returns items

Every candidate includes the active Jellyfin server id, item id, title, overview, year, type, rating, genres, runtime, artwork, favorite state, and continue-watching state where Jellyfin provides it.

## Interaction

Mobile supports:

- drag right for Yes
- drag left for No
- button fallback for Yes and No
- subtle tilt during drag
- Yes/No treatment that appears only while dragging
- match result card once the rule engine accepts a candidate

The first pass supports `Everyone` and `Majority` rules. Invite receive and SyncPlay group join are now wired for active/open Vantafyn clients, but remote vote transport is still not claimed as fully synchronized. The current local deck treats the active profile as the voting member unless future realtime vote messages are added.

## Empty State

If the deck is empty, Vantafyn shows a premium empty state with a refresh action and copy that asks the user to widen filters.

## Next Work

- websocket-backed vote sync
- group member ready state
- join flow with visible available groups
- host + at least one and first mutual match rules
- details sheet from the candidate card
- TV remote-focused picker

## Relationship To Invites

Invites do not replace the swipe picker. A party can be created in `Swipe to match` mode from Profile or from a detail page by choosing `Let the group choose`. In that mode the current detail item is not locked as playback media; it is only context for the party. Receivers see a top-slide in-app card while Vantafyn is open and connected.

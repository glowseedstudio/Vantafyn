# Vantafyn Mobile Premium Pass

## Background Picker

The four supplied root images were imported into `core-ui` as shared `drawable-nodpi` resources:

- `vantafyn_background_1.png`
- `vantafyn_background_2.png`
- `vantafyn_background_3.png`
- `vantafyn_background_4.png`

`VantafynOnboardingBackground` now accepts a drawable resource and crossfades between backgrounds while preserving dark/readable scrims and gradients. The selected background is persisted per profile when signed in, with an app-wide fallback before profile selection. Custom selectable backgrounds use a lighter scrim than the default nebula so the new artwork remains visible.

## Bottom Navigation

Mobile navigation now exposes:

- Home
- Library
- Search
- My List
- Admin, only for Jellyfin administrators
- Settings

The current icon set is a small internal Compose line-icon set drawn with Canvas. No third-party icon pack is bundled in this pass, so there is no external icon license to track yet.

## My List

My List is backed by Jellyfin favourites. Vantafyn loads favourites after login/session restore and shows them in:

- the bottom navigation My List tab
- grouped My List sections by item type
- a home row when favourites are returned
- detail page copy using `Add to My List` / `In My List`

Favourite mutation is wired through Jellyfin SDK `UserLibraryApi`:

- add: `userLibraryApi.markFavoriteItem(userId, itemId)`
- remove: `userLibraryApi.unmarkFavoriteItem(userId, itemId)`
- refresh: `itemsApi.getItemUserData(userId, itemId)`

Vantafyn applies an optimistic detail-state update, rolls back on failure, and refreshes the My List data after successful writes.

## Search

Search still uses Jellyfin search hints through the current `JellyfinSearchRepository`. Results are grouped by returned item type and can be filtered with UI chips. Genre/year/library filters are intentionally not faked; they should be added when repository queries expose those parameters.

## Live TV Guide

Live TV now renders a richer program-guide section from real Jellyfin channel and program data:

- channel logo/name/number
- current program name when exposed
- upcoming/current program time text when exposed

No synthetic timeline data is generated. If Jellyfin returns no channel/program listings, Vantafyn shows a clean unavailable state.

## User Preferences

Playback preferences are wired through Jellyfin `UserConfiguration`:

- read: `userApi.getCurrentUser().configuration`
- save: `userApi.updateUserConfiguration(userId, updatedConfiguration)`

Editable fields currently exposed:

- preferred audio language
- preferred subtitle language
- subtitle playback mode
- play default audio track
- remember audio selections
- remember subtitle selections
- next episode autoplay

Current-user password changes use `userApi.updateUserPassword(userId, UpdateUserPassword(..., resetPassword = false))`.

Subtitle appearance and display language are not exposed on `UserConfiguration` in the installed Jellyfin Kotlin SDK 1.7.1 model used here, so Vantafyn does not show those controls.

## Admin

The admin screen remains real-data only. It now presents the existing server overview, sessions, users, and statistics in premium cards. Watch time, trends, and plugin-backed statistics are not shown unless a real Jellyfin/plugin endpoint is implemented later.

Admin user management uses real Jellyfin APIs:

- user detail: `userApi.getUserById(userId)`
- hide/unhide, enable/disable, admin toggle, library access-all toggle, per-library folder access: fetch current `UserPolicy`, change only intended fields, then `userApi.updateUserPolicy(userId, updatedPolicy)`
- admin password reset: `userApi.updateUserPassword(userId, UpdateUserPassword(..., resetPassword = true))`

Safety rule: Vantafyn blocks disabling the current admin profile or removing its own admin role. Library access uses `UserPolicy.enableAllFolders` and `enabledFolders`; unrelated policy fields are preserved.

Active sessions use real `sessionApi.getSessions()` fields. Item artwork/progress/codec details are only shown when the Jellyfin session payload exposes them; no values are invented.

## Detail Page

Detail pages keep full-width top artwork. Metadata chips wrap in compact rows instead of horizontal scrolling. My List and watched-state actions are wired:

- favourites: `userLibraryApi.markFavoriteItem` / `unmarkFavoriteItem`
- watched state: `playStateApi.markPlayedItem` / `markUnplayedItem`

Theme audio remains detail-page only and still does not implement video playback.

## Smart Rows

Smart rows are addable/removable from Home Sections for row types that are already backed by real Jellyfin `GetItems` queries:

- New in Crime
- New in Thrillers
- New in Comedy
- New in Action
- New in Horror
- New in Drama
- Highly Rated
- Family Friendly
- Unwatched Movies
- Unwatched TV
- Recently Released Movies
- Recently Released TV

These rows persist per profile. If the query returns no items, the row is not shown on Home. Collection-based and Because You Watched rows remain hidden until a concrete Jellyfin collection/recommendation query is implemented and verified.

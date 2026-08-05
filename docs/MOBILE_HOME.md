# Mobile Home Navigation

This pass makes the logged-in mobile experience navigable without adding playback.

## Implemented Screens

- Home with real Jellyfin hero/row data and tappable media/library cards.
- Libraries with real Jellyfin views.
- Library Detail with a first-page grid of real Jellyfin items.
- Search with debounced Jellyfin search hints.
- Favorites with real Jellyfin favorite items.
- Admin, shown only for Jellyfin administrators, with read-only real server/library/user/session data.
- Profile/Settings with profile, server, switch user, add profile, Quick Connect, and log out actions.
- Media Detail placeholder with real metadata/artwork and a non-playing Watch/Open action.
- Home Sections settings for per-profile section visibility, order, artwork type, card shape, card size, and spacing.

## Home Layout

The mobile Home screen is composed from `HomeSectionType` and `HomeSectionPreference` in `feature-home`. The default order is:

1. Media bar
2. My Media
3. Continue Watching / Up Next
4. Recently Added Movies
5. Recently Added TV
6. Live TV Channels, only when Jellyfin returns real channels/programs
7. Smart Rows
8. Other libraries

Preferences are stored in app-private `SharedPreferences` under `vantafyn_home_layout`, keyed by Jellyfin profile id. This keeps visibility, ordering, artwork type, shape, size, and spacing local to the saved profile and survives app restarts. The Profile screen links to Home Sections, where each row can be hidden/shown, moved up/down, tuned, or reset to defaults.

The media bar uses the `JellyfinHome.heroItems` list, which is built from continue/resume items first, then latest movies, next-up episodes, and latest TV. This gives priority to relevant active viewing while still providing a featured item when there is no resume state.

The hero is a full-width cinematic banner, not a card. Mobile uses Android edge-to-edge so the hero artwork can extend behind the status area while profile controls still respect safe insets. It uses Backdrop first, overlays Logo artwork when Jellyfin exposes it, falls back to a title when no logo exists, and uses scrims/gradients so compact metadata remains readable. The hero deliberately has no overview/body text and no Watch button; tapping the hero opens Media Detail.

Live TV is not shown as a fake standalone section. If the Jellyfin server exposes Live TV as a real user library/view, it appears in My Media with the rest of the user's libraries. A dedicated Live TV Channels row appears only when `LiveTvApi` returns real channels or recommended/on-now programs. Opening the Live TV library queries real channels and shows a simple Guide row from recommended/on-now programs when available. If no guide data is returned, Vantafyn shows "Guide data unavailable"; if no channels are returned, it shows "No Live TV channels found" instead of a blank screen.

Smart Rows are a foundation backed by real metadata queries. Current definitions include New in Crime, New in Thrillers, Highly Rated, Recently Released Movies, and Recently Released TV. Empty smart rows are hidden from Home; no smart-row results are fabricated.

## Navigation

The mobile shell uses a fixed-height glass bottom dock with equal-width icon slots so selected items do not expand and small phones do not clip the right edge. Normal users see Home, Libraries, Search, Favorites, and Profile. Administrators see Home, Libraries, Search, Admin, and Profile so the dock remains stable at five tabs.

The mobile Home header intentionally omits app name, server name, logged-in text, and search. The only Home header control is the compact profile avatar at top-right; Search remains available from the bottom navigation.

The bottom dock uses local vector-style glyphs drawn in Compose Canvas: home, library grid, search, favorite/admin shield, and profile. No external icon pack was added in this pass, keeping the build dependency-free while improving icon clarity.

## Image Strategy

Jellyfin image URL generation stays in `core-jellyfin` helpers. Hero items prefer Backdrop artwork with Logo overlays when Jellyfin exposes a Logo image. Poster rows use Primary images, wide/episode rows prefer Thumb or Backdrop, user/profile views use Primary user images, and library cards use available library artwork. Missing images fall back to Vantafyn-styled surfaces rather than empty black boxes.

Default row artwork rules are:

- Recently Added Movies: poster cards using Primary artwork.
- Recently Added TV: wide cards using Thumb/Backdrop.
- Continue Watching / Next Up: wide cards using Thumb/Backdrop with progress.
- My Media and More Libraries: wide library cards using library artwork.
- Live TV Channels: wide cards from channel/program images returned by Jellyfin.

## Jellyfin SDK APIs

- Home rows: `itemsApi.getResumeItems()`, `tvShowsApi.getNextUp()`, `userLibraryApi.getLatestMedia()`.
- Libraries: `userViewsApi.getUserViews()`.
- Library detail: `itemsApi.getItems(GetItemsRequest)`.
- Media detail: `userLibraryApi.getItem()`.
- Search: `searchApi.getSearchHints(GetSearchHintsRequest)`.
- Favorites: `itemsApi.getItems(GetItemsRequest(isFavorite = true))`.
- Live TV: `liveTvApi.getLiveTvChannels(GetLiveTvChannelsRequest(addCurrentProgram = true))` and `liveTvApi.getRecommendedPrograms(GetRecommendedProgramsRequest(isAiring = true))`.
- Smart Rows: `itemsApi.getItems(GetItemsRequest(... genres/minCommunityRating/sortBy ...))` with real Jellyfin metadata filters.
- Quick Connect: `quickConnectApi` plus `userApi.authenticateWithQuickConnect()`.
- Admin visibility: `userApi.getCurrentUser()` / authentication responses expose `UserPolicy.isAdministrator`; the mobile Admin tab is shown only when that flag is true.
- Admin overview: `systemApi.getSystemInfo()`, `sessionApi.getSessions()`, `userApi.getUsers(...)`, and `itemsApi.getItems(GetItemsRequest(... enableTotalRecordCount = true ...))`.
- Server found avatar: the card prefers a public administrator image only when Jellyfin exposes that policy data pre-login, then falls back to the saved/current matching user image. Jellyfin public system info does not expose a dedicated admin avatar.

## Intentional Placeholders

- Playback is not implemented. Watch/Open shows a "Playback coming next" dialog.
- The Home hero has no Watch button; tapping the artwork opens Media Detail.
- Downloads, audiobook playback, and plugin features are not implemented.
- Admin watch-time totals and historical playback analytics are not fabricated. Jellyfin core does not expose those totals through the current SDK flow, so the Admin screen marks them as requiring a future plugin or external reporting source.
- Search has no persisted recent searches yet.
- Library sorting/filter chips are visual placeholders for now.
- Smart row management currently exposes the foundation through Home Sections; richer add/remove/edit flows can build on the persisted models.
- Series seasons/episodes are noted in Media Detail but not expanded yet.

## Known Limits

- Library detail loads a reasonable first page rather than infinite paging.
- Favorites uses the broad Jellyfin item query with `isFavorite = true`; exact grouping by media type can be refined later.
- Mobile navigation is currently ViewModel state driven inside `feature-home`; a dedicated navigation graph can be introduced when feature modules split further.
- Home section reordering currently uses up/down controls instead of drag-and-drop to keep the first implementation simple and reliable.

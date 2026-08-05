# Detail Page Actions

## Current Mobile Detail Surface

Vantafyn mobile detail pages now support real Jellyfin-backed actions for video media where the current architecture can execute them.

Implemented:

- Play / Resume through `JellyfinPlaybackRepository.getPlaybackInfo(...)`.
- Watch from beginning with explicit `startPositionTicks = 0`.
- Add to My List / Remove from My List through Jellyfin favorite user-data APIs.
- Mark watched / Mark unwatched through Jellyfin play-state APIs.
- Series season selector through Jellyfin `TvShowsApi.getSeasons(...)` and `TvShowsApi.getEpisodes(...)`.
- Episode selection from the series detail page.
- Episode long press starts that episode from the beginning.
- Media Info sheet from Jellyfin item/media source/stream metadata.
- Admin-only filesystem path display in Media Info.

Deferred:

- Delete/remove media is intentionally not implemented.
- Refresh metadata and identify/edit metadata are not implemented yet. Admins see a disabled note only.
- Playlist actions for video are not exposed yet.
- Pre-playback audio/subtitle/version selection is limited to Media Info visibility for now. Runtime player track sheets remain the actual switching UI.
- Recursive series/season watched state is not implemented until the Jellyfin endpoint behavior is verified cleanly for the SDK.

## Actions By Item Type

### Movie

- Primary: Play or Resume.
- More: Play/Resume, Watch from beginning, Add/Remove My List, Mark watched/unwatched, Media Info.
- Watch from beginning starts the movie at `0` ticks and does not clear Jellyfin resume state until normal playback reporting updates it.

### Series

- Primary: plays the first unfinished loaded episode, falling back to the first loaded episode.
- Season selector: horizontal premium chips backed by `getSeasonEpisodes(...)`.
- More: Play next/first available episode, Watch from beginning, Add/Remove My List, Mark watched/unwatched for the series item where Jellyfin permits it, Media Info.
- Watch from beginning starts the first valid episode from the selected season, or the initially loaded episode list if no selected-season data is available.

### Episode

Vantafyn does not yet have a dedicated standalone episode detail screen. Episode rows on series details:

- Tap: play/resume episode.
- Long press: play episode from beginning.

### Music

Music detail/actions remain in `feature-music`. This pass did not change music playback actions.

## Permissions And Safety

Normal users see no destructive server actions. Admin-only filesystem paths are hidden unless `session.user.isAdministrator` is true.

If Jellyfin rejects a user-data write, Vantafyn reverts optimistic state where applicable and shows the returned user-safe error message.

## My List Mapping

Vantafyn currently maps `My List` to Jellyfin favorite user data:

- Add to My List = `markFavoriteItem(...)`
- Remove from My List = `unmarkFavoriteItem(...)`

The Favorites/My List screen is refreshed after writes.

## Watched State

Watched state uses:

- Mark watched = `PlayStateApi.markPlayedItem(...)`
- Mark unwatched = `PlayStateApi.markUnplayedItem(...)`

Home/library state is refreshed after context-menu writes. Detail state updates optimistically and reverts on failure.

## Media Info

The Media Info sheet shows available Jellyfin metadata such as:

- container
- media source/version name
- file size
- bitrate
- video/audio codecs
- resolution
- HDR/SDR range where exposed
- runtime
- IMDb/TMDb ids where exposed
- audio and subtitle stream labels

Filesystem paths are admin-only.

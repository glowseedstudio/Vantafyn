# Mobile Media Detail

The mobile Media Detail screen is a premium metadata-first page. It does not start video playback yet.

## Data Flow

- UI route: `MobileDestination.MediaDetail` in `feature-home`.
- ViewModel entry: `VantafynHomeViewModel.openMedia(itemId)`.
- Repository: `JellyfinMediaRepository.getMediaDetail(session, itemId)`.
- SDK boundary: all Jellyfin SDK calls stay in `core-jellyfin`.

## Jellyfin SDK APIs

- Main item metadata: `userLibraryApi.getItem(userId, itemId)`.
- Series seasons: `tvShowsApi.getSeasons(GetSeasonsRequest)`.
- Series episodes: `tvShowsApi.getEpisodes(GetEpisodesRequest)`.
- Related media: `libraryApi.getSimilarItems(GetSimilarItemsRequest)`.
- Theme songs: `libraryApi.getThemeSongs(GetThemeSongsRequest)`.
- Theme audio URL: `universalAudioApi.getUniversalAudioStreamUrl(...)`.

## Layout

- The mobile detail screen uses a full-width, top-bleed artwork hero rather than a framed card.
- The hero extends under the status area with strong top/bottom and side scrims.
- Back and secondary actions float over the artwork.
- The logo/title and metadata sit in the lower fade area and are followed by padded content sections.
- Content is split into simple `LazyColumn` items and horizontal rows to avoid nested unbounded scrolling crashes.

## Image Strategy

- Backdrop is used for the immersive hero background.
- Logo is preferred for title art.
- Primary image is used as poster fallback.
- Thumb/backdrop/primary are used for episode cards.
- Person primary image is used for cast/crew, with Vantafyn gradient initials as fallback.

## Theme Music

When enabled, detail pages request the first available Jellyfin theme song and play it through a small Media3 `ExoPlayer` instance scoped to the detail composable. The theme audio stream URL is generated with `universalAudioApi.getUniversalAudioStreamUrl(...)` and includes the saved access token as an `api_key` query parameter so ExoPlayer can fetch it outside the Jellyfin SDK client. It loops, requests Android audio focus through Media3 audio attributes, fades in on entry, and fades out when the user leaves the detail page or opens another item.

The setting lives in Profile/Settings as `Theme Music` and is persisted per saved profile in app-private preferences. Default is ON with `Soft` volume. Volume choices are `Soft`, `Medium`, `High`, and `Full`.

The theme player observes the app lifecycle: it fades down and pauses when the app stops, including backgrounding and lock-screen transitions, then resumes with a fade-in when the detail screen becomes active again.

## Placeholders

- Play/Resume/Open shows “Playback coming next”.
- Favorite, Watched, More, download/info actions show placeholders.
- External links are displayed, but browser opening is not wired yet.
- Episode cards currently show the first fetched season’s episodes.

## Crash Hardening

- Detail sections are only rendered when their lists are non-empty.
- Missing artwork, logos, people, related items, external links, and episodes fall back or disappear cleanly.
- Long title/metadata text uses max lines and ellipsis.
- The detail hero is not clipped into a rounded card, avoiding text/action clipping at the edges.

## Playback Milestone Prep

Future playback work needs media source selection, subtitle/audio track selection, playback session reporting, resume position, stream bitrate/profile handling, and a dedicated video player surface. This detail screen now exposes the metadata and action points that will feed that work.

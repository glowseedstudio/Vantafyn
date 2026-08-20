# Mobile Media Detail

The mobile Media Detail screen is a premium metadata and action surface for Jellyfin movies, series, episodes, collections and people.

## Data Flow

- UI route: `MobileDestination.MediaDetail` in `feature-home`.
- ViewModel entry: `VantafynHomeViewModel.openMedia(itemId)`.
- Repository: `JellyfinMediaRepository.getMediaDetail(session, itemId)`.
- Playback repository: `JellyfinPlaybackRepository` resolves playable sources and reporting models.
- SDK boundary: Jellyfin SDK calls stay in `core-jellyfin`.

## Jellyfin SDK APIs

- Main item metadata: `userLibraryApi.getItem(userId, itemId)`.
- Series seasons: `tvShowsApi.getSeasons(GetSeasonsRequest)`.
- Series episodes: `tvShowsApi.getEpisodes(GetEpisodesRequest)`.
- Related media: `libraryApi.getSimilarItems(GetSimilarItemsRequest)`.
- Theme songs: `libraryApi.getThemeSongs(GetThemeSongsRequest)`.
- Theme audio URL: `universalAudioApi.getUniversalAudioStreamUrl(...)`.
- Playback info: `mediaInfoApi.getPostedPlaybackInfo(...)`.
- Playback reporting: play-state start/progress/stop APIs.
- Favourites/My List: Jellyfin favourite user-data APIs.
- Watched state: Jellyfin played/unplayed APIs.
- Downloads: metadata and playback information used by `core-downloads`.
- Media Segments: Jellyfin Media Segments API where the server/plugin exposes segments.

## Layout

- The mobile detail screen uses a full-width, top-bleed artwork hero rather than a framed card.
- The hero extends under the status area with strong top/side scrims.
- The bottom of the hero fades transparently into the app background instead of ending with a hard black block.
- A scroll-aware top scrim appears once content moves behind the status bar.
- Back and secondary actions use flat white icons over the artwork.
- Logo/title and metadata sit in the lower hero area and flow into padded content sections.
- Content is split into `LazyColumn` items and horizontal rows to avoid nested unbounded scrolling crashes.

## Image Strategy

- Backdrop is used for the immersive hero background.
- Logo is preferred for title art.
- Primary image is used as poster fallback.
- Thumb/backdrop/primary are used for episode cards.
- Person primary image is used for cast/crew, with Vantafyn gradient initials as fallback.

## Implemented Actions

- Play / Resume through `JellyfinPlaybackRepository.getPlaybackInfo(...)`.
- Watch from beginning with explicit `startPositionTicks = 0`.
- Add to My List / Remove from My List through Jellyfin favourites.
- Mark watched / Mark unwatched through Jellyfin play-state APIs.
- Download where the item type/source is supported by `core-downloads`.
- Media Info from Jellyfin item/media source/stream metadata.
- Cast from the mobile player where a Cast session is available.
- Watch Party start/invite entry points where supported.

## Series And Episodes

Series detail pages load Jellyfin seasons and episodes.

- The primary action plays the first unfinished loaded episode, falling back to the first loaded episode.
- Season chips are backed by Jellyfin season/episode APIs.
- Episode rows can play/resume the selected episode.
- Episode long press starts from the beginning.

## Playback

Movie, episode and supported Live TV entry points route into the mobile Media3 player.

The player supports:

- direct play and HLS transcode fallback;
- resume position;
- Jellyfin playback reporting;
- subtitle/audio track switching;
- screen fit and zoom controls;
- Up Next/autoplay;
- Jellyfin Media Segments prompt/auto-skip;
- Google Cast handoff;
- offline local file playback.

See [PLAYBACK_IMPLEMENTATION.md](PLAYBACK_IMPLEMENTATION.md).

## Theme Music

When enabled, detail pages request the first available Jellyfin theme song and play it softly while the detail screen is active.

Theme music:

- uses Media3 through the shared player factory;
- fades in on entry and fades out on exit;
- pauses/fades when the app is backgrounded or locked;
- resumes cleanly when the detail screen becomes active again;
- respects the per-profile volume setting in Settings.

The setting lives in Profile/Settings as `Theme music on detail pages`. Volume choices are `Soft`, `Medium`, `High`, and `Full`.

## Deferred Or Limited

- Delete/remove media is intentionally not implemented.
- Refresh metadata and identify/edit metadata are not wired as admin write actions.
- Recursive series/season watched-state writes are deferred until the SDK/server endpoint behaviour is verified safely.
- Full pre-playback media version/source selection is visible in Media Info but not yet a complete picker.
- Some subtitle formats still depend on Android/Media3 support or Jellyfin transcoding.

## Crash Hardening

- Detail sections render only when their lists are non-empty.
- Missing artwork, logos, people, related items, external links and episodes fall back or disappear cleanly.
- Long title/metadata text uses bounded layout.
- The detail hero is not clipped into a rounded card, avoiding text/action clipping at the edges.

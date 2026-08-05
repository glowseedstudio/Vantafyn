# Wholphin Audit Before Playback

Wholphin was inspected only as a conceptual reference. No source code was copied into Vantafyn.

## Artwork

Wholphin consistently treats Jellyfin artwork as typed images: primary posters, backdrops, logos, thumbs, and person primary images. Vantafyn already follows the same model through `ImageType.PRIMARY`, `BACKDROP`, `LOGO`, and `THUMB`, including parent fallback for episodes/series where available.

Gap closed/confirmed: Vantafyn detail and card mapping already attempts primary/backdrop/logo/thumb and person imagery. Missing images are handled as empty artwork surfaces.

## Detail Pages

Wholphin separates detail metadata, row content, background/backdrop behavior, favourites, watched state, and theme music. Vantafyn now mirrors that separation at a smaller mobile-first scale:

- full-width artwork hero
- logo fallback to title
- related/people/episodes where Jellyfin returns them
- wrapped metadata chips
- user-data writes for My List and watched state

## Favourites/User Data

Wholphin uses Jellyfin user data/favourite mechanisms rather than local-only favourites. Vantafyn now writes favourites through `UserLibraryApi.markFavoriteItem` and `unmarkFavoriteItem`, refreshes user data with `ItemsApi.getItemUserData`, and refreshes My List rows after writes.

## Live TV

Wholphin uses `LiveTvApi.getLiveTvChannels` and guide/program APIs, plus user preferences for channel sorting. Vantafyn currently fetches channels and recommended/current programs and presents them as a channel/program guide list. Timeline grid and channel sorting preferences remain later work.

## Playback Preparation

Wholphin’s playback layer is built around Jellyfin playback info/media sources, stream choice, track selection, and playback lifecycle reporting. Vantafyn has not implemented playback yet. The next milestone should add a dedicated core playback-info repository before any player UI.

## User Settings

Wholphin uses Jellyfin `UserConfiguration` for playback preferences and app-local preferences for presentation features. Vantafyn now reads/writes the Jellyfin fields exposed by SDK 1.7.1: audio language, subtitle language, subtitle mode, default audio, remembered audio/subtitle selections, and next episode autoplay.

## Admin/User Management

Wholphin does not appear to be an admin-first client. Vantafyn implements admin user policy updates directly through Jellyfin `UserPolicy`, using fetch-modify-save and preserving unrelated fields.


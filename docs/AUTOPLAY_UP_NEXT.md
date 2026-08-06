# Autoplay Up Next

## Behavior

Vantafyn now supports a mobile-first Up Next overlay for TV episodes. When an episode is near the end, the player shows a cinematic glass panel with the next episode artwork, series name, episode label, title, countdown, Play Now, and Keep Watching.

The overlay appears only when:

- the current item is an episode
- playback is not Live TV
- playback is not music
- playback is not a movie
- Jellyfin returns a real playable next episode
- autoplay is enabled

Movies and Live TV finish normally and never autoplay unrelated content.

## Thresholds

Defaults live in `AutoplaySettings`:

- enabled: true
- countdown: 10 seconds
- passout protection: off by default
- passout protection limit: 180 minutes
- show before end: 45 seconds
- show after watched percent: 94%
- only episodes: true
- play next on completion: true

The overlay appears when remaining playback time is within 45 seconds or the item is at least 94% watched.

## Countdown

- Countdown starts when the overlay appears.
- Countdown pauses while playback is paused.
- Countdown resumes when playback resumes.
- Seeking back away from the end hides the overlay.
- Keep Watching cancels autoplay for the current episode.
- Back dismisses the overlay before exiting the player.
- Duplicate starts are guarded by local player state.

## Settings

Settings -> Playback includes:

- Autoplay next episode: backed by Jellyfin user playback preferences.
- Up Next countdown: local Vantafyn preference with 5, 10, 15, and 30 second options.
- Passout protection: local Vantafyn profile preference that prevents endless episode autoplay.
- Continue playing limit: local Vantafyn profile preference with whole-hour options from 1 hour to 5 hours.

If autoplay is disabled, Vantafyn does not show the countdown overlay in this pass.

When passout protection is enabled, manual playback starts a continuous watching window. Autoplayed episodes inherit that same window. Once the selected limit is reached, Vantafyn lets the current episode finish and reports playback normally, but it does not show Up Next or automatically start another episode.

## Next Episode Lookup

`core-jellyfin` resolves candidates through `JellyfinMediaRepository.getNextEpisode(...)`:

- load the current item from Jellyfin
- require `BaseItemKind.EPISODE`
- require `seriesId`
- query episodes for the series
- sort by season number and episode number
- choose the first accessible episode after the current one
- verify the candidate has play access and media sources

If any step fails or there is no next episode, no overlay is shown.

## TV Reuse

TV can reuse:

- `UpNextCandidate`
- `UpNextState`
- `AutoplaySettings`
- `JellyfinMediaRepository.getNextEpisode(...)`

TV still needs its own D-pad focused overlay UI and TV player integration.

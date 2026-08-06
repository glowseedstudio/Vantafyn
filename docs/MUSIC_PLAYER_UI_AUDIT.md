# Music Player UI Audit

## Current Issues

- The old Now Playing surface used a heavy dark artwork scrim and default-feeling glass icon buttons.
- The mini-player lacked previous controls and used static ellipsized text.
- The queue was plain text, with no artwork or current-track structure.
- The More action was not useful from the full player.
- Lyrics used the older muddy background treatment.

## Target Behavior

- Full-screen, YouTube Music-inspired Now Playing layout with Vantafyn dark cinematic styling.
- Album-reactive colour background with readable foreground contrast.
- Vivid Vantafyn gradient progress and central play/pause control.
- Flat secondary controls for shuffle, previous, next, repeat, queue, more, and close.
- Premium queue cards with artwork, title, artist, duration, and current-track highlight.
- Mini-player with artwork, marquee title/artist, previous, play/pause, next, and gradient progress.

## Components Changed

- `MusicMiniPlayer`
- `NowPlayingDialog`
- `LyricsScreen`
- `QueuePanel`
- current-track More sheet
- shared music UI helpers inside `MusicScreens.kt`
- music track favorite metadata plumbing

## Remaining Limitations

- The album-reactive background currently uses a deterministic palette derived from artwork/track metadata. True bitmap palette extraction can be added later with a dedicated image palette pipeline.
- Queue reordering is not implemented, so no drag handle is shown.
- Download and share actions are not shown because they are not implemented safely yet.

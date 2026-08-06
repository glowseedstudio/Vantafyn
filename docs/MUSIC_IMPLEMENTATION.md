# Music Implementation

Music playback remains owned by `MusicPlaybackController` and the foreground `VantafynMusicPlaybackService`.

The UI observes controller state through `MusicViewModel`; it does not create another player. This preserves:

- background playback;
- lock-screen playback;
- notification controls;
- queue advancement;
- video playback stopping music;
- logout/profile-switch cleanup.

Music UI additions in this pass:

- `VantafynMarqueeText` for long titles and artists;
- album-reactive background helper;
- gradient progress strip;
- flat icon controls;
- gradient primary play/pause;
- current-track More sheet;
- premium queue list.

Favorite/unfavorite uses the existing Jellyfin user-data favorite API through `JellyfinMediaRepository.setFavorite`.

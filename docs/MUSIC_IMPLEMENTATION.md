# Music Implementation

Music playback remains owned by `MusicPlaybackController` and the foreground `VantafynMusicPlaybackService`.

The UI observes controller state through `MusicViewModel`; it does not create another player. Android notification shade, lock screen, and Android Auto also talk to the same Media3 session/player.

This preserves:

- one ExoPlayer for music;
- background playback;
- lock-screen playback;
- notification controls;
- Android Auto browse/play controls;
- queue advancement;
- video playback stopping music;
- logout/profile-switch cleanup.

System integration additions:

- `VantafynMusicPlaybackService` is a Media3 `MediaLibraryService`;
- `VantafynMusicMediaLibraryProvider` restores the saved Jellyfin profile and exposes real music browse roots;
- Android Auto can browse recently added music, albums, artists, playlists, songs, search results, and the current queue;
- system-selected items are adopted into `MusicPlaybackController` through `adoptSystemQueue(...)`.

Music UI additions in this pass:

- `VantafynMarqueeText` for long titles and artists;
- album-reactive background helper;
- gradient progress strip;
- flat icon controls;
- gradient primary play/pause;
- current-track More sheet;
- premium queue list.

Favorite/unfavorite uses the existing Jellyfin user-data favorite API through `JellyfinMediaRepository.setFavorite`.

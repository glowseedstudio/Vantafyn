# Cast Subtitles And Audio

## Subtitles

When a movie or episode is handed to Google Cast, Vantafyn now maps supported Jellyfin subtitle tracks into Cast `MediaTrack` entries and attaches them to `MediaInfo`.

Supported Cast subtitle formats in this implementation:

- WebVTT: `text/vtt`
- TTML/DFXP: `application/ttml+xml`
- CEA-608 where identified by Jellyfin metadata

The fullscreen Cast controller shows a caption button only when the active Cast item has at least one supported Cast subtitle track. The sheet is Cast-specific and offers:

- Off
- subtitle name/language
- default badge when Jellyfin marks the track as default
- external badge when applicable
- current selected checkmark

Selecting a subtitle calls `RemoteMediaClient.setActiveMediaTracks(longArrayOf(trackId))`. Selecting Off calls the same API with an empty array. If the receiver rejects the change, Vantafyn shows: "Couldn't switch subtitles while casting."

## Audio

Audio track switching is not exposed while casting through the Default Media Receiver.

Google documents that Default and Styled Media Receivers only support text tracks through the sender media-track API. Audio and video track switching require a custom receiver. Vantafyn therefore keeps Cast audio controls hidden instead of shipping a no-op sheet.

Local playback audio switching still uses Media3 track selection and is unchanged.

## Defaults

If the local playback item has a selected subtitle stream and that stream maps to a supported Cast subtitle, Vantafyn asks Cast to activate it on load. Otherwise Vantafyn falls back to a Jellyfin default subtitle when one maps to a supported Cast subtitle. Off remains available after load.

## Future Work

Reliable Cast audio switching should be implemented only after either:

- a custom Vantafyn receiver exists; or
- a tested Jellyfin stream reload path can switch audio, preserve receiver position, and keep playback reporting consistent.

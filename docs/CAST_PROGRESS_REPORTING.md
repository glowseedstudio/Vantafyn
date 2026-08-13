# Cast Progress Reporting

Cast progress reporting uses the same Jellyfin playback reporting repository as local video playback.

## Reporting Ownership

- Local ExoPlayer reports through `VantafynHomeViewModel.reportPlaybackStarted`, `reportPlaybackProgress`, and `exitPlayback`.
- During Cast handoff, local reporting is stopped first.
- The active playback info is replaced with the Cast-negotiated `JellyfinPlaybackInfo`.
- The Cast controller reports progress using remote position and paused state from `RemoteMediaClient`.

## Cadence

The mobile Cast controller reports progress when the remote position has moved by about 10 seconds. This avoids tight polling and avoids reporting fake progress when no Cast position is available.

## Start/Stop

Local-to-Cast handoff:

1. Stop local Jellyfin reporting at the handoff position.
2. Resolve Cast playback info.
3. Load the Default Media Receiver.
4. Send playback start for the Cast-resolved playback session.
5. Send periodic Cast progress from remote position.

Stop casting from the Vantafyn controller:

1. Send a final paused progress update.
2. Stop the Cast session.
3. Return to the previous Vantafyn screen.

## Known Limitations

- External Cast session loss is observed through Cast state, but full automatic Cast-to-local re-resolution still needs hardening.
- Default Media Receiver does not provide enough Vantafyn-specific hooks for every subtitle/audio switching workflow. Unsupported controls are not shown as working Cast controls.
- Failed progress reports are swallowed by the repository result path and are not retried aggressively to avoid network spam.

# Wholphin Autoplay Audit

## Scope

Wholphin was inspected only as conceptual reference for end-of-episode and next-up playback behavior. No Wholphin source files, assets, layouts, or code were copied into Vantafyn.

## Observed Behavior

- Wholphin treats playback as a queue/playlist, where the current item can expose a next item.
- The playback state can hold a next-up item separately from the normal controls.
- Near the end of playback or around outro handling, Wholphin exposes next-up UI instead of waiting until after the file has ended.
- The next-up UI supports automatic continuation, manual play, and cancellation.
- Back handling can dismiss the next-up prompt before exiting playback.
- Starting the next item goes through the normal playback ViewModel flow, which keeps reporting and player lifecycle logic centralized.
- If there is no next item, the next-up state remains empty and playback finishes normally.

## Useful Concepts For Vantafyn

- Resolve the next episode before showing UI.
- Keep autoplay state separate from player controls.
- Let the user cancel autoplay for the current episode.
- Do not show next-up affordances for non-episode content.
- Start the next episode through the same playback startup path used by regular playback.
- Avoid duplicate next starts when completion and countdown finish close together.

## Vantafyn Plan

- Store shared `UpNextCandidate`, `UpNextState`, and `AutoplaySettings` in `core-media`.
- Keep Jellyfin candidate lookup inside `core-jellyfin`.
- Attach the resolved next episode candidate to `VantafynPlaybackItem`.
- Render mobile Up Next UI in `feature-player`.
- Reuse the same candidate/settings model for TV playback later.

## GPL Boundary

Wholphin is GPL-2.0. Vantafyn did not copy Wholphin code, source structure, UI files, or assets. This audit records behavioral ideas only.

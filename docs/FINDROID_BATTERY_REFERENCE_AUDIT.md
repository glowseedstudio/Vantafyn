# Findroid Battery Reference Audit

Reference inspected only from `_reference/Findroid`. No Findroid source was copied.

## Relevant patterns observed

- Findroid commonly collects Compose state with `collectAsStateWithLifecycle`.
- Player screens use lifecycle observers and `repeatOnLifecycle` for active screen work.
- Local playback activity/reporting loops are scoped to visible playback lifecycle rather than app-global work.
- Navigation helpers check lifecycle state before acting on routes.

## Lessons applied

- Vantafyn's Requests carousel and mobile video loops were moved to lifecycle-aware `STARTED` execution.
- Long-running playback/reporting should be owned by the active playback/session layer, not arbitrary UI composition.
- Idle home/profile/request screens should not own background network loops.

## Intentionally not copied

- Findroid playback implementation, UI, architecture, package structure, and source files were not copied into Vantafyn.

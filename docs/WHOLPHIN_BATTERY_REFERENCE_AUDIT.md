# Wholphin Battery Reference Audit

Reference inspected only from `_reference/Wholphin`. No Wholphin source was copied.

## Relevant patterns observed

- Wholphin uses lifecycle-aware entry points such as `LifecycleStartEffect` and `LifecycleResumeEffect` for screen refresh and playback surfaces.
- Websocket handling is centralized in `ServerEventListener`, with explicit subscribe and cancellation paths rather than anonymous app-wide loops.
- Music playback is owned by a service-level player/session concept instead of scattered Composable players.
- Playback reporting is handled through a dedicated reporting service and tied to playback ownership.

## Lessons applied

- Prefer explicit lifecycle start/stop over keeping realtime connections alive for convenience.
- Keep media ownership centralized.
- Use lifecycle boundaries for UI timers and refresh loops.
- Keep diagnostics/auditability around long-running tasks because battery regressions are usually caused by ownership drift.

## Intentionally not copied

- Wholphin UI, branding, colors, screens, player code, and GPL source were not copied into Vantafyn.
- Wholphin screensaver, update, voice, and unrelated service code were not imported.

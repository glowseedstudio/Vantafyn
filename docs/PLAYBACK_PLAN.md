# Playback Plan

This document is retained as a historical planning pointer.

The original pre-playback plan has been superseded. Mobile playback is now implemented through Media3, Jellyfin playback-info negotiation, playback reporting, track selection, Up Next, Media Segments, Live TV handling, Cast handoff, and offline local playback.

Use the current implementation documents instead:

- [PLAYBACK_IMPLEMENTATION.md](PLAYBACK_IMPLEMENTATION.md)
- [PLAYBACK_TEST_MATRIX.md](PLAYBACK_TEST_MATRIX.md)
- [VIDEO_PLAYER_AUDIT.md](VIDEO_PLAYER_AUDIT.md)
- [CAST_IMPLEMENTATION_AUDIT.md](CAST_IMPLEMENTATION_AUDIT.md)
- [CAST_PROGRESS_REPORTING.md](CAST_PROGRESS_REPORTING.md)
- [CAST_SUBTITLES_AUDIO.md](CAST_SUBTITLES_AUDIO.md)
- [OFFLINE_ARCHITECTURE.md](OFFLINE_ARCHITECTURE.md)

Remaining playback work is tracked as parity and hardening, not initial implementation:

- Android TV playback parity.
- A fuller source/quality/version picker.
- Continued Cast disconnect/reconnect hardening.
- Broader subtitle/container/device testing.

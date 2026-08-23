# Vantafyn Android Integration

At server login/restore, Vantafyn should call:

```http
GET /Vantafyn/Capabilities
```

If the endpoint returns 404, fails, or times out quickly, Companion is absent or unavailable. Vantafyn continues as a normal Jellyfin client.

If Companion is available, enable only the modules reported as ready:

- Settings sync
- Requests
- Watch Parties
- Personal playlists
- Ephemeral typing indicators

The Android app must reuse the existing authenticated Jellyfin session. Do not add a second Jellyfin auth stack.

Companion absence or partial configuration must never block playback, libraries, favourites, watched state, or other Jellyfin-owned features.

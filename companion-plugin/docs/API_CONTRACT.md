# Vantafyn Companion API Contract

All routes are under `/Vantafyn/`.

## Capabilities

```http
GET /Vantafyn/Capabilities
```

Authenticated Jellyfin user only. Returns effective feature capability for the current user.

## User Settings

```http
GET /Vantafyn/UserSettings
PUT /Vantafyn/UserSettings
DELETE /Vantafyn/UserSettings
```

Settings are scoped to the authenticated Jellyfin user. `PUT` uses optimistic concurrency by `revision`.

## Requests

```http
GET /Vantafyn/Requests/Capabilities
GET /Vantafyn/Requests/Search?query=...&type=movie|series
POST /Vantafyn/Requests/Movies
POST /Vantafyn/Requests/Series
```

Requests use server-side Ombi configuration. The Ombi API key is never returned to normal clients.

## Watch Parties

```http
POST /Vantafyn/WatchParties
GET /Vantafyn/WatchParties/{partyId}
POST /Vantafyn/WatchParties/{partyId}/Join
POST /Vantafyn/WatchParties/{partyId}/Leave
POST /Vantafyn/WatchParties/{partyId}/End
GET /Vantafyn/WatchParties/{partyId}/Snapshot
POST /Vantafyn/WatchParties/{partyId}/Commands
POST /Vantafyn/WatchParties/{partyId}/Invites
```

Host-authoritative in v0.1. Commands are sequenced by the server.

## Live Events

```http
GET /Vantafyn/Events
```

Authenticated SSE stream for connected clients.

## Personal Playlists

```http
GET /Vantafyn/PersonalPlaylists
POST /Vantafyn/PersonalPlaylists
POST /Vantafyn/PersonalPlaylists/{playlistId}/Items
```

Private per authenticated Jellyfin user. Stores Jellyfin item IDs only.

## Ephemeral Social & Typing Indicators

```http
POST /Vantafyn/Social/Typing
GET /Vantafyn/Social/Typing?conversationId=...&peerUserId=...
```

Transient, zero-disk-I/O in-memory state store with 3.5s TTL sliding expiration.
Allows Vantafyn clients to display live animated typing bubbles (`TypingBubbleAnimation`) safely without generating database records or server overhead.
Also broadcast via the authenticated `/Vantafyn/Events` SSE stream.


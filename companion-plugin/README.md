# Vantafyn Companion

Vantafyn Companion is an optional Jellyfin server plugin for Vantafyn-specific server persistence and live coordination.

It does not replace Jellyfin users, authentication, media libraries, playback, watched state, or favourites.

## Compatibility

- Jellyfin Server: 10.11.11 baseline
- Target framework: `net9.0`
- Plugin version: `0.1.0`
- API version: `1`

## Build

```bash
./scripts/build.sh
```

## Test

```bash
./scripts/test.sh
```

## Modules

- Capabilities: `GET /Vantafyn/Capabilities`
- User settings sync: `GET|PUT|DELETE /Vantafyn/UserSettings`
- Requests/Ombi: `GET /Vantafyn/Requests/Search`, `POST /Vantafyn/Requests/Movies`, `POST /Vantafyn/Requests/Series`
- Watch Parties: `POST /Vantafyn/WatchParties`, join/leave/end/snapshot/commands/invites
- Live events: `GET /Vantafyn/Events`
- Personal playlists: `GET|POST /Vantafyn/PersonalPlaylists`

All user endpoints require Jellyfin authentication. Admin configuration endpoints require Jellyfin elevation.

## Privacy

No Firebase, analytics, tracking, Vantafyn cloud relay, advertising SDKs, or phone-home services are included.

Ombi communication only happens when the server administrator configures Ombi in the Jellyfin dashboard.

## Install

See [docs/INSTALL.md](docs/INSTALL.md).

## Server Testing

See [docs/SERVER_TESTING.md](docs/SERVER_TESTING.md) for manual install, Plugin Manager/catalog install requirements, Companion configuration, and app test steps.

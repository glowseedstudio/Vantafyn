# Statistics Implementation

## Architecture

Statistics live in `core-jellyfin` and are exposed through `JellyfinAdminRepository.getOverview`.

`feature-home` renders the admin dashboard from plain Vantafyn models only. The UI does not call the Jellyfin SDK or Playback Reporting endpoints directly.

## Models

- `JellyfinStatisticsOverview`
- `JellyfinStatisticsCapability`
- `JellyfinUserWatchStats`
- `JellyfinMediaWatchStats`
- `JellyfinWatchTimeBucket`

These models are intentionally app-owned so the mobile and TV apps do not depend on plugin-specific JSON shapes.

## Data Fetching

Normal admin data still uses the Jellyfin Kotlin SDK:

- system info
- sessions
- users
- plugins
- scheduled tasks
- activity log
- devices
- logs
- item counts

Playback Reporting is called through a small raw HTTP reader in `SdkJellyfinAdminRepository` because the plugin endpoints are not part of the typed Jellyfin Kotlin SDK APIs. Requests use the active Jellyfin server URL and access token. Tokens are only sent as request headers and are not logged.

## Fallback Behavior

When Playback Reporting is not installed, disabled, unreachable, or empty, Vantafyn returns a `CoreActivityOnly` or `Unavailable` statistics model. The admin dashboard then shows real Jellyfin library counts and a short premium state explaining that detailed watch-time requires Playback Reporting.

## UI

The mobile admin dashboard renders statistics as:

- compact headline metrics
- watch-time trend chart
- top viewers leaderboard
- most-watched titles when provided
- recent activity by user

The screen avoids raw JSON, endpoint names, or developer text in the app UI.

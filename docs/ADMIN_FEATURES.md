# Admin Features

## Current Features

- Server overview with admin profile image, server name, version, and status.
- User management, including creating Jellyfin users through the Jellyfin API.
- User detail screens with premium profile-style controls.
- Active session cards with playback status, artwork, IP address, and direct play/transcoding state.
- Server tools for plugins, scheduled tasks, and library scans.
- Jellyfin statistics backed by the Playback Reporting plugin when available.

## Statistics

The statistics area is admin-only and uses real data only.

When Playback Reporting is available, Vantafyn shows watch time, play counts, top viewers, trends, and watched-title breakdowns. When it is not available, Vantafyn shows what Jellyfin core can retrieve and labels detailed historical analytics as requiring Playback Reporting.

## Security

Admin-only features require a Jellyfin administrator session. Vantafyn does not log passwords, access tokens, or server secrets.

# Admin Features

## Current Features

- Server overview with admin profile image, server name, Jellyfin 12 version, OS, architecture, and live operational status.
- Real-time active stream telemetry cards showing playback progress, artwork, client IP, device model, Direct Play vs Transcoding badges, active video/audio codecs, transcode reasons, and live bitrate bandwidth (e.g. 12 Mbps).
- User management, including creating, editing, and managing Jellyfin users through the Jellyfin API.
- User detail screens with profile-style controls and permission assignments.
- Server tools for installed plugins, scheduled tasks, and one-tap library scans.
- Jellyfin analytics graphs backed by the Playback Reporting plugin when available.

## Statistics

The statistics area is admin-only and uses real data only.

When Playback Reporting is available, Vantafyn shows watch time, play counts, top viewers, trends, and watched-title breakdowns. When it is not available, Vantafyn shows what Jellyfin core can retrieve and labels detailed historical analytics as requiring Playback Reporting.

## Security

Admin-only features require a Jellyfin administrator session. Vantafyn does not log passwords, access tokens, or server secrets.

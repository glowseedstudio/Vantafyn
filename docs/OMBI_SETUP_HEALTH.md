# Ombi Setup Health

Ombi setup health is admin-only.

## Admin Visible

The admin management surface may show:

- Ombi reachable state
- configured Ombi URL
- API key readiness and validity
- URL warnings for localhost, LAN-only addresses, public HTTP, and HTTPS recommendations
- detected search/request/listing capabilities
- moderation support when a verified endpoint exists
- shared vs per-user identity mode
- access mode
- linked users and local access requests

The current mobile admin screen includes a dedicated `Setup Health` view and a `Ready for Requests` checklist in Ombi management.

## Checks Implemented

- Ombi server reachability through the existing connection test.
- API key readiness without showing the key.
- URL safety hints for localhost, LAN-only addresses, and public HTTP.
- Request capabilities detected during setup test.
- Current shared/per-user identity mode.
- Local per-user mappings and access requests.
- Active Jellyfin availability index status.

## Not Verified By Ombi API Yet

Ombi-side Jellyfin/Plex/Emby media-server settings and Radarr/Sonarr settings are shown as `Not verified` because the confirmed Swagger endpoints used by Vantafyn do not expose a stable, safe setup-health endpoint for those internals.

Vantafyn does not fake those checks. It explains that Ombi may still request media, but availability labels may be incomplete until matching is configured and refreshed.

## Normal User Hidden

Normal users must not see:

- setup health
- API key state
- configured base URL
- request endpoint capability lists
- Swagger/audit details
- request-engine diagnostics
- reset/remove controls
- user mapping management

Normal users only see Requests browsing/search, request details, personal request history where supported, link Ombi account, request access, pending access, token-expired, or not-ready states.

## Current Limitations

Radarr/Sonarr health is not fully wired in the native Ombi setup health UI yet. It should remain an admin-only diagnostic until exact Ombi endpoints are confirmed and implemented.

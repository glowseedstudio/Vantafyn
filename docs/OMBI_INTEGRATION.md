# Ombi Integration

Ombi support is optional. Vantafyn does not host Ombi and does not require Ombi to browse or play Jellyfin media. Ombi must already be running on the user/admin server.

## Admin Setup

Jellyfin admin users configure Ombi inside Vantafyn:

`Profile / Settings -> Integrations & Requests -> Ombi`

The native setup wizard asks for:

- Ombi server URL
- Ombi API key
- connection test
- access mode: disabled, admins only, or all users
- final confirmation

Normal users cannot configure Ombi. If Ombi is not configured or is disabled, normal users do not see the Requests tab.

## Native API Endpoints

The current native provider uses Ombi API paths commonly exposed by Ombi v4:

- `GET /api/v1/Request/movie`
- `GET /api/v1/Request/tv`
- `GET /api/v1/Search/movie/{query}`
- `GET /api/v1/Search/tv/{query}`
- `POST /api/v1/Request/movie`
- `POST /api/v1/Request/tv`

Requests send the Ombi `ApiKey` header. When available, Vantafyn also sends an `ApiAlias` header using the current Jellyfin display name so Ombi can attribute requests if configured to support it.

## Account Model

Vantafyn supports two identity modes in its data model:

### Shared API Key Mode

- an admin configures one Ombi API key on the device
- search, request, and request-list calls use that configured Ombi identity
- Vantafyn may send the Jellyfin display name as `ApiAlias`, but Ombi remains authoritative for attribution

This is the default and currently supported request mode.

### Per-User Ombi Account Mode

Vantafyn can track which Jellyfin users need Ombi access and lets users request access from inside the app. Admins can mark local access tasks as account created, linked, or dismissed.

Per-user Ombi credential login/token-based requests are not enabled yet because a safe Ombi per-user token endpoint has not been confirmed. The app does not show a working password form until that support is verified.

## Capability Detection

The setup test detects:

- movie search
- TV search
- movie request capability, inferred when movie search is reachable
- TV request capability, inferred when TV search is reachable
- request listing
- admin moderation availability

Admin approve/deny is currently reported as unavailable because the endpoint has not been confirmed against the target Ombi Swagger/API. Vantafyn must not show broken approve/deny buttons.

## Permission Behavior

Vantafyn does not assume Jellyfin admin equals Ombi admin. Ombi API responses are treated as authoritative:

- `401` becomes unauthorized or bad API key.
- `403` becomes permission denied.
- `409` becomes already requested.
- `5xx` becomes server error.

Admin queue visibility currently uses the configured Ombi API key. Approval and deny actions are intentionally marked unsupported until the exact Ombi admin endpoints are verified against the target Ombi server Swagger.

## Current Limitations

- WebView fallback is documented but not implemented.
- Approval/deny actions are not wired yet because Ombi admin endpoints vary by version.
- The first UI pass is mobile-first.
- Search and request behavior depends on the configured Ombi API key permissions.
- Per-user Ombi login/token requests are modeled but not wired.
- Ombi access requests are local to this device/server setup until a Vantafyn sync/backend exists.

## Security

Vantafyn stores the Ombi API key through Android encrypted storage backed by the device keystore.

Vantafyn does not log Ombi API keys, passwords, tokens, authorization headers, or request URLs containing secrets.

HTTP is allowed for local home-server deployments. If a URL uses plain HTTP on a private address, Vantafyn shows a soft local-network warning. If a public URL uses plain HTTP, Vantafyn recommends HTTPS for remote access.

## WebView Fallback Plan

If native API coverage is insufficient later, an optional WebView fallback may be added with:

- clear labeling that the user is viewing Ombi web UI
- no injected credentials
- no token logging
- normal Android back handling
- clear SSL/error handling

Native API remains the preferred integration path.

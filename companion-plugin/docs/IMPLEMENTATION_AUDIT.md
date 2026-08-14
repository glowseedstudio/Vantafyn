# Vantafyn Companion Implementation Audit

## Target

- Jellyfin compatibility baseline: 10.11.11
- .NET target: `net9.0`
- Packages: `Jellyfin.Controller` 10.11.11 and `Jellyfin.Model` 10.11.11
- Plugin GUID: `fd7d0e8a-89a9-45a6-8f2b-1f4c5bb1c8cb`
- Plugin version: 0.1.0
- API version: 1

## Verified Platform Shape

The project follows the official Jellyfin plugin shape used by current template/plugins:

- `BasePlugin<TConfiguration>` for plugin lifecycle and configuration.
- `IHasWebPages` with an embedded dashboard configuration page.
- `IPluginServiceRegistrator` for dependency registration.
- ASP.NET `ControllerBase` controllers for plugin HTTP APIs.
- Jellyfin authorization context resolves the authenticated user. User-owned endpoints do not accept `UserId` query/body ownership.
- Admin routes use Jellyfin elevated authorization policy.
- Plugin data is derived from Jellyfin plugin data paths through the plugin instance, never hard-coded OS paths.

## Module Isolation

Each capability has a narrow service boundary:

- Core: diagnostics, paths, module state, atomic JSON helpers.
- UserSettings: file-backed per-user settings envelopes.
- Requests: typed Ombi adapter and narrow request endpoints.
- WatchParties: in-memory sessions, invites, commands, and realtime transport.
- PersonalPlaylists: private per-user playlist persistence foundation.

External integrations are not used during startup. Ombi is touched only on admin test connection or Requests API calls.

## Authentication And Authorization

- User APIs require Jellyfin authentication.
- Admin configuration and diagnostics require elevated Jellyfin authorization.
- Ownership is derived from the authenticated Jellyfin user.
- Client-supplied owner/user IDs are not trusted for user settings, watch party host, or personal playlist ownership.

## Persistence

v0.1 uses app-private plugin JSON files with:

- atomic temp-write/replace
- per-user locks
- bounded settings payload size
- JSON validation before write

The store interfaces allow later SQLite migration.

## Realtime

Vantafyn Companion uses its own authenticated SSE endpoint:

```http
GET /Vantafyn/Events
```

It does not abuse Jellyfin display/playback/session commands as a hidden protocol.

## Limitations

- This environment does not currently have `dotnet` installed, so build/test execution could not be run locally by Codex.
- Ombi endpoints are implemented against the common Ombi v1 API route shape and must be verified against the target server's live `/swagger` before production use.
- Watch Parties are intentionally ephemeral/in-memory in v0.1.
- Background push is explicitly unsupported; live connected-client events only.

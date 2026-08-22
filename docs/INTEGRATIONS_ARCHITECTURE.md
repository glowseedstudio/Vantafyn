# Integrations Architecture

Vantafyn integrations are optional modules that sit beside the Jellyfin client. Jellyfin remains the primary media/session system; integrations add capabilities such as requests, analytics, or external dashboards without becoming required app dependencies.

## Modules

- `core-integrations`: shared integration contracts, capability flags, request-provider models, and encrypted secret storage.
- `core-ombi`: Ombi configuration storage and native Ombi API implementation.
- `feature-requests`: native mobile Requests UI powered by the active request provider.

Future providers such as Jellyseerr, Overseerr, and Tautulli should implement the same `MediaRequestProvider` style contracts rather than being hard-coded into app screens.

## Capability Model

Integrations expose capabilities such as:

- `Requests`
- `RequestStatus`
- `RequestApproval`
- `RequestManagement`
- `UserRequests`
- `SearchExternalMedia`
- `AdminDashboard`

UI must check capabilities before showing request/search/admin integration features. If no provider is configured, the app must continue to work exactly like normal Jellyfin-only Vantafyn.

Capability discovery is provider-specific. For Ombi, Vantafyn stores the last detected capability set in non-secret configuration so the app can hide unavailable features such as moderation controls.

Access modes are also provider-specific configuration:

- disabled
- admins only
- all users

Normal users must not see integration setup controls. Normal users should only see request entry points when the provider is configured and enabled for all users. Admin-only provider state includes server URLs, API keys, setup health, capability detection, request-engine diagnostics, access mode selection, identity mode selection, user mappings, access-request management, and reset/remove controls.

Identity modes are separate from access modes. Ombi currently supports:

- shared API key mode, where requests use the configured admin/shared Ombi identity
- per-user account mode, where users with an admin-created Ombi profile sign in through Ombi's token endpoint and Vantafyn stores only the encrypted bearer token

Local access requests are intentionally modeled as sync-ready data but are not remote notifications. A later Vantafyn backend or server plugin can sync these tasks across devices.

Availability matching is owned by Jellyfin-facing core code. External request providers expose provider IDs; Vantafyn matches those IDs against the active Jellyfin availability index. Provider integrations must not claim an item is openable in Vantafyn unless that direct match exists.

## Security

Integration secrets are stored through `EncryptedIntegrationAuthStorage`, backed by Android Keystore AES-GCM encryption. Server URLs and enabled flags are stored separately as non-secret configuration. API keys and tokens must never be logged, placed in diagnostics, or shown in UI.

Route-level safety is enforced in feature ViewModels. Hiding a button is not enough: setup and write actions must also check the active Jellyfin admin flag before mutating integration configuration.

## Current Providers

- **Ombi**: Media request provider. Optional, admin-configured, with shared API key and per-user token authentication modes.
- **Achievement Badges & Native Social**: Server plugin integration for user achievements, rank progression, friends directory, friend requests, and real-time 1-to-1 messaging. Opt-in per user profile, with zero cross-profile state leakage and non-intrusive floating dock entry points.

Jellyfin profile picture editing is not an integration. It lives in `core-jellyfin` and uses Jellyfin user image APIs directly; see `docs/JELLYFIN_PROFILE_IMAGES.md`.

## Ombi Setup Ownership

Ombi setup is admin-owned. App UI calls feature view models; app screens do not call Ombi HTTP APIs directly.

`core-ombi` owns:

- encrypted admin API key storage
- encrypted per-user session token storage
- setup configuration
- request identity mode
- user lookup/matching
- access-request de-duplication

Normal-user routing is derived from setup state, request mode, local linked session, and access-request state.

## Native Social & 1-to-1 Messaging Architecture

Native Social connects to the Jellyfin Achievement Badges companion plugin:
- `core-jellyfin` provides `JellyfinSocialRepository` and associated data models (`JellyfinFriend`, `JellyfinFriendRequest`, `JellyfinSocialConversation`, `JellyfinSocialMessage`).
- `feature-home` manages `SocialScreen` (tabbed hub), `ChatScreen` (1-to-1 conversation), `FloatingSocialDock` (floating entry bubble with unread counter), `FloatingSocialPanel` (quick-access popup sheet), and `SocialIslandBanner` (in-app message toast).
- Polling lifecycle is safe and foreground-only: 30s ambient polling for unread messages when foregrounded, 4s active polling while chatting.
- Strict per-profile isolation via SharedPreferences key `social_enabled_$profileId` (defaults to `false`). Switching profiles completely resets social state and polling jobs.

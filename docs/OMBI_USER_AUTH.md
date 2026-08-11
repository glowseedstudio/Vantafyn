# Ombi User Auth

Per-user Ombi auth is optional and only used when an admin selects `Each user links Ombi account`.

## User Flow

1. Admin configures the Ombi URL and API key once.
2. Admin enables Requests for all users.
3. Admin selects per-user Ombi account mode.
4. A normal user opens Requests.
5. If no account mapping exists, the user can request access.
6. The admin creates or approves the Ombi account in Ombi, then marks the local Vantafyn access request as account created.
7. The user signs in with Ombi username/email and password.
8. Vantafyn calls `POST /api/v1/Token`, validates the returned token with `GET /api/v1/Identity`, stores only the bearer token, and clears the password field.

## User Fields

Normal users only provide:

- Ombi username or email
- Ombi password

Normal users never provide:

- Ombi server URL
- Ombi API key
- request engine settings
- Jellyfin matching or availability settings

## Session Restore

On return, Vantafyn validates the encrypted saved token with `GET /api/v1/Identity`. If validation fails with unauthorized, the user sees a sign-in-again state. If Requests are disabled or not configured, the user sees a user-facing unavailable state instead of setup controls.

Per-user request mode never silently falls back to the admin API key for normal-user requests. If the user token is missing or expired, the user must sign in again.

## Security

Passwords are never stored. Bearer tokens are stored through Android encrypted storage. Tokens, passwords, API keys, and auth headers must not be logged or shown in error UI.
## Link Ombi Routing

In per-user mode, Vantafyn checks whether the active Jellyfin profile likely has an Ombi account.

Matching order:

1. Exact Ombi username equals Jellyfin username.
2. Exact email match when Jellyfin email is available.
3. Case-insensitive username match.
4. Normalized display-name fallback as low confidence.

Jellyfin email is not currently available in `JellyfinUser`, so email matching is reserved for a later model expansion.

Lookup is not required for login. If Ombi user lookup is unavailable, the user can still manually sign in.

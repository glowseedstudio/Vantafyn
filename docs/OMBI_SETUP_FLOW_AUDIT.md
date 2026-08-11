# Ombi Setup Flow Audit

## Broken Behavior Found

- Admin setup used a URL/key/test/access sequence that made the shared API-key path feel like the only real mode.
- Per-user Ombi login existed in code, but was buried in the access/manage section instead of being a first-class setup choice.
- Normal users in per-user mode were routed by local mapping state only. If they already had an Ombi account but Vantafyn had not linked it yet, they could be shown Request Access before Link Ombi.
- Access requests were keyed mainly by Jellyfin user id. Old local data could accumulate repeated pending cards after repeated request taps or setup churn.
- Manage Ombi mixed normal status, capability checks, availability refresh, setup health, and destructive actions in one large text-heavy panel.

## Root Causes

- `OmbiIdentityMode.PerUserAccount` was available but not part of the main wizard decision.
- `RequestsUiState.canShowOmbiLogin` required local account-created/linked state, so no local mapping meant no login-first route.
- Access request storage did not have a stable logical key containing server, profile, and integration identity.
- Duplicate cleanup was not run before displaying admin access requests.

## Fixed Flow Design

- Admin setup is now:
  1. Connect Ombi
  2. Choose request mode
  3. User access
  4. Requests are ready
- Request modes are first-class:
  - Server request account
  - Per-user Ombi login
- Normal users never see Ombi URL, API key, setup health, endpoint/capability language, or admin setup actions.
- Per-user users see Link Ombi first unless they already have an active pending access request.
- If user lookup is available, Vantafyn pre-fills the likely Ombi username and says an account was found.
- If lookup is unavailable, users can still manually sign in.

## Data Model Changes

- Added `OmbiAccessRequestKey(serverAccountId, profileId, integrationId)`.
- Added server/integration identity fields to local `OmbiAccessRequest`.
- Added `OmbiUserProfile`, `OmbiUserMatch`, and `OmbiUserMatchState`.

## Migration And De-Dupe Plan

- Opening Requests/Manage Ombi runs access-request cleanup.
- Cleanup normalizes old requests with the current Ombi server identity.
- Requests are grouped by logical key.
- The best/latest request is kept with priority:
  `Linked > AccountCreated > Pending > Seen > Dismissed`.
- Removing Ombi setup clears local Ombi setup data, which archives/removes integration-local pending cards instead of carrying stale duplicates forward.

## Limitations

- Jellyfin email is not currently present in `JellyfinUser`, so matching uses username exact, case-insensitive username, then normalized display-name fallback.
- Ombi user lookup endpoint differs across Ombi versions. Vantafyn tries known safe admin endpoints and falls back to manual login if unavailable.
- Vantafyn does not auto-create Ombi users yet.

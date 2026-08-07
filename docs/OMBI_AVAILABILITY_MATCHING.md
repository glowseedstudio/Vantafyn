# Ombi Availability Matching

Vantafyn combines Ombi request status with direct Jellyfin provider-ID matching.

## Matching Source

The active Jellyfin session builds a lightweight availability index from Movies and Series using Jellyfin `ProviderIds`.

Indexed data:

- provider id key, such as TMDb, TVDb, IMDb
- Jellyfin item id
- item type
- title
- source server id/name
- movie count
- series count
- last refreshed time

No secrets are stored in the index.

## Match Rules

For movies, Vantafyn matches provider IDs in this order when present:

- TMDb / TheMovieDb
- IMDb

For TV series, Vantafyn matches:

- TVDb
- TMDb / TheMovieDb
- IMDb

Title/year fallback is intentionally not used for confident `Available` labels in this pass. If provider IDs are missing, Vantafyn leaves direct availability unknown rather than guessing.

## Status Combination

- If direct Jellyfin matching finds the item, show `Available` and route the action to the Jellyfin detail page.
- If Ombi says available but direct Jellyfin matching cannot verify it, show that it is available according to Ombi and keep a refresh/check path.
- If Ombi says requested, pending, approved, processing, declined, or failed, show that state and do not show duplicate request success.
- If neither Ombi nor Jellyfin confirms availability, show the request action when the account has permission.

## Cache Behavior

The Requests ViewModel builds the active-session availability index after Requests becomes usable. Admins can manually run `Refresh availability index` from Ombi management.

Multi-server availability is not implemented because the current mobile browsing flow operates on one active Jellyfin session. The index records the active source server so multi-server support can be extended later without changing the card contract.

## Current Limitations

- No title-only available match is used.
- No persisted cross-device availability index yet.
- No Ombi-side media-server health verification yet.
- Request cancellation remains hidden until endpoint behavior is confirmed.

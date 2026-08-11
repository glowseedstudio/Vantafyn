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

Matches are keyed by media type and provider ID. A TMDb value from a movie is not allowed to match a Jellyfin series, and a TMDb value from a series is not allowed to match a Jellyfin movie. The legacy untyped key is only accepted when the matched Jellyfin item type is still the requested type.

## Status Combination

- If direct Jellyfin matching finds the item, show `Available in Jellyfin` and route the action to the Jellyfin detail page.
- If Ombi says available but direct Jellyfin matching cannot verify it, show `Available according to Ombi`. Vantafyn must not show `Watch now` or open a Jellyfin item until the active Jellyfin provider-ID match is verified.
- If Ombi says requested, pending, approved, processing, declined, or failed, show that state and do not show duplicate request success.
- If neither Ombi nor Jellyfin confirms availability, show the request action when the account has permission.

Ombi request IDs must be meaningful. `requestId`, `mediaRequestId`, `requestGuid`, or nested `request.id` count as request evidence only when present and non-zero. Generic Ombi `id` fields from search/discovery responses are not treated as request records.

Ombi `createdDate` is not used as request evidence because discovery/detail models can expose dates for non-requested items.

`requestStatus` text is treated carefully: values containing `not requested` do not count as requested, and unknown non-empty values map to a neutral status instead of a confident badge.

## Cache Behavior

The Requests ViewModel builds the active-session availability index after Requests becomes usable. Admins can manually run `Refresh availability index` from Ombi management.

Multi-server availability is not implemented because the current mobile browsing flow operates on one active Jellyfin session. The index records the active source server so multi-server support can be extended later without changing the card contract.

## Current Limitations

- No title-only available match is used.
- No persisted cross-device availability index yet.
- No Ombi-side media-server health verification yet.
- Request cancellation remains hidden until endpoint behavior is confirmed.

## Debug Diagnostics

When Android logging enables `VantafynOmbiStatus` at debug level, `core-ombi` emits compact status-resolution diagnostics:

- source endpoint
- media type and title
- TMDb/TVDb/request IDs seen in the payload
- relevant Ombi flags such as requested, approved, denied, processing, available, fully/partly available, and requestStatus
- final Vantafyn request state

Secrets, API keys, bearer tokens, and full request URLs are not logged.

# Ombi Swagger Endpoint Audit

Audit date: 2026-08-07 Australia/Sydney

## Instance Inspected

- Requested local Swagger URL: `http://localhost:5000/swagger/v1/swagger.json`
- Local result from Codex environment: not reachable. `curl` reached the host environment but nothing was listening on `localhost:5000`.
- External app/API URL for Vantafyn runtime: `https://ombi.jelly-watch.org`
- External Swagger JSON used for this audit: `https://ombi.jelly-watch.org/swagger/v1/swagger.json`
- External Swagger result: HTTP 200, `application/json;charset=utf-8`, valid OpenAPI `3.0.1`
- Swagger title/version: `Ombi Api V1`, version `v1`
- Ombi runtime version from `GET /api/v1/Settings/about`: `4.60.15`
- Server details exposed by about endpoint: branch `Develop`, `.NET 8.0.29`, Nobara Linux 44, X64, SQLite databases.

Do not use `localhost` in the Android app. Android TV and phone clients must use the configured external base URL: `https://ombi.jelly-watch.org`.

## External URL Behavior

- `GET https://ombi.jelly-watch.org` returns HTTP 200 HTML for the Ombi web app.
- `GET https://ombi.jelly-watch.org/api` returns HTTP 200 HTML for the Ombi web app, not JSON API discovery.
- `GET https://ombi.jelly-watch.org/swagger/v1/swagger.json` returns HTTP 200 JSON.
- `GET https://ombi.jelly-watch.org/api/v1/Settings/about` returns HTTP 200 JSON.
- HTTPS is valid through Caddy. No unexpected redirect was observed for Swagger JSON or `/api/v1/Settings/about`.

## Auth Schemes

OpenAPI `components.securitySchemes` exposes only:

- `ApiKey`
- Type: `apiKey`
- Header: `ApiKey`
- Description from Swagger: `API Key provided by Ombi. Example: "ApiKey: {token}"`
- Global OpenAPI security: `ApiKey`

The Swagger document does not define a Bearer/JWT security scheme and does not explicitly mark `Authorization: Bearer ...` on operations. However, `/api/v1/Token` returns an `access_token`, so per-user auth should be tested as `Authorization: Bearer <access_token>` against real endpoints before implementation. Until verified, Bearer acceptance is documented as likely but not proven by Swagger.

No cookie/session auth scheme is declared in the OpenAPI document.

## Login Endpoint

### `POST /api/v1/Token`

Summary: `Gets the token.`

Request body schema: `Ombi.Models.UserAuthModel`

Fields:

- `username`: string, nullable
- `password`: string, nullable
- `rememberMe`: boolean
- `usePlexAdminAccount`: boolean
- `usePlexOAuth`: boolean
- `plexTvPin`: `OAuthPin`

Response 200 schema: `Ombi.Controllers.V1.Token`

Fields:

- `access_token`: string, nullable
- `expiration`: date-time

Error response:

- `401 Unauthorized`, `ProblemDetails`

Notes:

- This is the normal-user login/token endpoint found in Swagger.
- It can submit username/email-style identifier plus password via `username` and `password`.
- Token field is `access_token`.
- Swagger does not say this endpoint needs `ApiKey`; it is the login endpoint and should be treated as public.
- Do not log usernames, passwords, access tokens, or request bodies.

### `POST /api/v1/Token/refresh`

Summary: `Refreshes the token.`

Request body schema: `TokenRefresh`

Fields:

- `token`: string, nullable
- `userename`: string, nullable. This appears misspelled in the live Swagger schema.

Response:

- `401 Unauthorized` documented.

Notes:

- Swagger does not document a 200 response body here.
- Token refresh support should be tested before relying on it.

### Other token endpoints

- `POST /api/v1/Token/header_auth`: returns 200 or 401, no request body documented.
- `POST /api/v1/Token/requirePassword`: request body is `UserAuthModel`, response is boolean.
- `GET /api/v1/Token/{pinId}` and `POST /api/v1/Token/plextoken` exist for Plex auth flows.

## Current User / Token Validation

### `GET /api/v1/Identity`

Summary: `Gets the current logged in user.`

Response 200 schema: `Ombi.Core.Models.UI.UserViewModel`

Important fields:

- `id`
- `userName`
- `alias`
- `emailAddress`
- `claims`
- `lastLoggedIn`
- `language`
- `hasLoggedIn`
- `userType`
- request quotas and limits:
  - `movieRequestLimit`
  - `episodeRequestLimit`
  - `musicRequestLimit`
  - `movieRequestQuota`
  - `episodeRequestQuota`
  - `musicRequestQuota`

Recommendation:

- After `POST /api/v1/Token`, validate per-user token by calling `GET /api/v1/Identity`.
- Use returned `id`, `userName`, `alias`, `emailAddress`, and `claims` for local linked-account display.
- Because Swagger does not declare Bearer auth, test this with `Authorization: Bearer <access_token>`. If it returns 401, fallback is to validate by calling a search/request endpoint and handling 401.

Related endpoints:

- `GET /api/v1/Identity/User/{id}`: get user by id.
- `GET /api/v1/Identity/Users`: all users, likely admin.
- `GET /api/v1/Identity/claims`: all available claims.
- `PUT /api/v1/Identity/local`: local user updates.

## Search Endpoints

### Movie search

Preferred simple endpoint:

- `GET /api/v1/Search/movie/{searchTerm}`
- Summary: `Searches for a movie.`
- Path field: `searchTerm`
- Response: array of `Ombi.Core.Models.Search.SearchMovieViewModel`

Useful fields from `SearchMovieViewModel`:

- `title`, `originalTitle`, `overview`
- `posterPath`, `backdropPath`
- `releaseDate`, `digitalReleaseDate`
- `theMovieDbId`, `imdbId`, `theTvDbId`
- `requested`, `requestId`, `approved`, `denied`, `available`
- `has4KRequest`, `approved4K`, `available4K`, `denied4K`
- `quality`
- `plexUrl`, `embyUrl`, `jellyfinUrl`

Detail endpoint:

- `GET /api/v2/Search/movie/{movieDbId}`
- Response: `MovieFullInfoViewModel`

Browse endpoints verified from live Swagger:

- `GET /api/v2/Search/movie/popular/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/movie/nowplaying/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/movie/upcoming/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/movie/toprated/{currentPosition}/{amountToLoad}`

These return arrays of `SearchMovieViewModel`.

### TV search

Preferred simple endpoint:

- `GET /api/v1/Search/tv/{searchTerm}`
- Summary: `Searches for a Tv Show.`
- Path field: `searchTerm`
- Response: array of `Ombi.Core.Models.Search.SearchTvShowViewModel`

Useful fields from `SearchTvShowViewModel`:

- `title`, `overview`, `banner`, `posterPath`, `backdropPath`
- `seriesId`
- `status`, `firstAired`, `network`, `runtime`, `genre`
- `requestAll`, `firstSeason`, `latestSeason`
- `fullyAvailable`, `partlyAvailable`, `available`
- `requested`, `requestId`, `approved`, `denied`
- `seasonRequests`
- `theTvDbId`, `theMovieDbId`, `imdbId`

Detail endpoints:

- `GET /api/v1/Search/tv/info/{tvdbId}`
- `GET /api/v2/Search/tv/{tvdbId}`
- `GET /api/v2/Search/tv/moviedb/{moviedbid}`

Browse endpoints verified from live Swagger:

- `GET /api/v2/Search/tv/trending/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/tv/popular/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/tv/anticipated/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/tv/mostwatched/{currentPosition}/{amountToLoad}`

These return arrays of `SearchTvShowViewModel`.

### Multi-search

- `POST /api/v2/Search/multi/{searchTerm}`
- Summary: returns both TV and movies.
- Request body schema: `MultiSearchFilter`
- Fields: `movies`, `tvShows`, `music`, `people`
- Response: array of `MultiSearchResult`
- Fields: `id`, `mediaType`, `title`, `poster`, `overview`

Auth:

- Swagger globally declares `ApiKey`.
- Bearer token support must be runtime-tested. Use ApiKey for existing shared mode.

## Request Creation Endpoints

### Movie request

- `POST /api/v1/Request/movie`
- Summary: `Requests a movie.`
- Request body schema: `Ombi.Core.Models.Requests.MovieRequestViewModel`
- Response schema: `Ombi.Core.Engine.RequestEngineResult`

Request fields:

- `theMovieDbId`: integer
- `languageCode`: string, nullable
- `is4kRequest`: boolean
- `requestOnBehalf`: string, nullable
- `rootFolderOverride`: integer, nullable
- `qualityPathOverride`: integer, nullable

Response fields:

- `result`: boolean
- `message`: string, nullable
- `isError`: boolean, read-only
- `errorMessage`: string, nullable
- `errorCode`: enum
- `requestId`: integer

Known `errorCode` enum values:

- `AlreadyRequested`
- `NoPermissions`
- `NoPermissionsOnBehalf`
- `NoPermissionsRequestMovie`
- `MovieRequestQuotaExceeded`
- `RequestDoesNotExist`

### TV request

Preferred endpoint:

- `POST /api/v2/Requests/tv`
- Summary: `Requests a tv show/episode/season.`
- Request body schema: `Ombi.Core.Models.Requests.TvRequestViewModelV2`
- Response schema: `RequestEngineResult`

Request fields:

- `theMovieDbId`: integer
- `languageCode`: string, nullable
- `source`: `RequestSource`
- `requestAll`: boolean
- `latestSeason`: boolean
- `firstSeason`: boolean
- `languageProfile`: integer, nullable
- `seasons`: array of `SeasonsViewModel`, nullable
- `requestOnBehalf`: string, nullable
- `rootFolderOverride`: integer, nullable
- `qualityPathOverride`: integer, nullable

Season request shape:

- `seasonNumber`: integer
- `episodes`: array of `EpisodesViewModel`, nullable

Episode request shape:

- `episodeNumber`: integer

Deprecated older endpoint:

- `POST /api/v1/Request/tv`
- Request body schema: `TvRequestViewModel`
- Uses `tvDbId` instead of `theMovieDbId`
- Marked deprecated in Swagger.

TV request supports, based on schema:

- Whole series: `requestAll`
- Latest season: `latestSeason`
- First season: `firstSeason`
- Selected seasons: `seasons[].seasonNumber`
- Selected episodes: `seasons[].episodes[].episodeNumber`

Auth:

- Existing shared mode should continue with `ApiKey`.
- Per-user mode should use `Authorization: Bearer <access_token>` only after runtime validation confirms Ombi accepts it.

## Request History / Status Endpoints

### V1 lists

- `GET /api/v1/Request/movie`: all movie requests, response array of `MovieRequests`.
- `GET /api/v1/Request/tv`: TV requests, response array of `TvRequests`.
- `GET /api/v1/Request/tvlite`: TV requests without full season/episode graph.
- `GET /api/v1/Request/movie/{count}/{position}/{orderType}/{statusType}/{availabilityType}`: paged movie requests.
- `GET /api/v1/Request/tv/{count}/{position}/{orderType}/{statusFilterType}/{availabilityFilterType}`: paged TV requests.
- `GET /api/v1/Request/movie/info/{requestId}`: movie request info.
- `GET /api/v1/Request/tv/{requestId}`: full TV request object.
- `GET /api/v1/Request/userhasrequest?userId={id}`: boolean.

### V2 lists

- `GET /api/v2/Requests/movie/{count}/{position}/{sort}/{sortOrder}`
- `GET /api/v2/Requests/tv/{count}/{position}/{sort}/{sortOrder}`
- Both support optional query `requestedBy={userId}`.
- Status-specific endpoints exist for movie and TV:
  - `available`
  - `denied`
  - `pending`
  - `processing`
  - `unavailable`
- Movie-only typo endpoint also exists: `/api/v2/Requests/movie/availble/...`.
- `GET /api/v2/Requests/recentlyRequested` returns `RecentlyRequestedModel[]`.

Status fields found:

- Movie request entity: `approved`, `denied`, `deniedReason`, `requestedDate`, `available`, `markedAsAvailable`, `requestStatus`, `canApprove`, `requestedUserId`, `requestedByAlias`, `requestType`.
- TV child request entity: `approved`, `denied`, `deniedReason`, `requestedDate`, `available`, `markedAsAvailable`, `requestStatus`, `requestedUserId`, `requestedByAlias`, `canApprove`, `seasonRequests`.
- TV root entity: `childRequests`, `status`, `totalSeasons`, `qualityOverride`, `rootFolder`, `languageProfile`.
- Search result fields also expose `requested`, `approved`, `denied`, `available`, `fullyAvailable`, and `partlyAvailable`.

Visibility:

- Swagger does not state whether normal users see all requests or only their own.
- V2 supports `requestedBy={userId}` and should be preferred for per-user history.
- Ombi settings such as hiding requests from users may affect normal-user visibility. Treat server response as authoritative.

## Admin Endpoints Discovered, Not Implemented

These should remain hidden until permission testing and confirmation UX are added.

### Movie moderation

- `POST /api/v1/Request/movie/approve`
- `PUT /api/v1/Request/movie/deny`
- `POST /api/v1/Request/movie/available`
- `DELETE /api/v1/Request/movie/{requestId}`

Bodies:

- Approve/available use `MovieUpdateModel`.
- Deny uses `DenyMovieModel`.
- Delete uses path `requestId`.

### TV moderation

- `POST /api/v1/Request/tv/approve`
- `PUT /api/v1/Request/tv/deny`
- `POST /api/v1/Request/tv/available`
- `DELETE /api/v1/Request/tv/{requestId}`
- `POST /api/v1/Request/tv/quality/{requestId}/{qualityId}`
- `POST /api/v1/Request/tv/root/{requestId}/{rootFolderId}`
- `GET /api/v1/Request/tv/{requestId}/child`
- `GET /api/v1/Request/tv/child/{requestId}`

Bodies:

- Approve/available use `TvUpdateModel`.
- Deny uses `DenyTvModel`.

### User management

- `POST /api/v1/Identity`: creates user, body `UserViewModel`, response `IdentityResult`.
- `PUT /api/v1/Identity`: updates user, body `UserViewModel`, response `IdentityResult`.
- `GET /api/v1/Identity/Users`: all users.
- `GET /api/v1/Identity/User/{id}`: user by id.
- `GET /api/v1/Identity/claims`: available claims.

Risks:

- Vantafyn must not assume Jellyfin admin equals Ombi admin.
- Moderation, deletion, and user creation need explicit confirmation UI and Ombi permission testing.
- These actions should remain hidden in normal request UI for now.

## Implementation Recommendation

Keep current shared API-key mode:

- Admin configures external base URL, normally `https://ombi.jelly-watch.org`.
- Store the `ApiKey` securely.
- Search/request/list calls continue sending header `ApiKey: <redacted>`.

Implemented per-user mode flow:

1. User enters Ombi username/email and password.
2. `POST /api/v1/Token` with `UserAuthModel`.
3. Store `access_token` and `expiration` securely, not the password.
4. Validate with `GET /api/v1/Identity`.
5. Use returned `id` for V2 request history with `requestedBy={id}`.
6. Use Bearer token for search/request calls only after runtime testing confirms `Authorization: Bearer <access_token>` works on this Ombi instance.
7. If Bearer fails, keep per-user mode disabled and continue shared ApiKey mode.

Recommended endpoints for per-user feature:

- Login: `POST /api/v1/Token`
- Validate/current user: `GET /api/v1/Identity`
- Search movies: `GET /api/v1/Search/movie/{searchTerm}`
- Search TV: `GET /api/v1/Search/tv/{searchTerm}`
- Request movie: `POST /api/v1/Request/movie`
- Request TV: `POST /api/v2/Requests/tv`
- User movie history: `GET /api/v2/Requests/movie/{count}/{position}/{sort}/{sortOrder}?requestedBy={userId}`
- User TV history: `GET /api/v2/Requests/tv/{count}/{position}/{sort}/{sortOrder}?requestedBy={userId}`

Keep hidden for now:

- Approve/deny/available/delete request actions.
- Ombi user creation/update.
- Automatic Jellyfin-to-Ombi user provisioning.

## Risks / Unknowns

- Swagger declares only `ApiKey`; Bearer/JWT acceptance must be confirmed by real authenticated calls.
- `POST /api/v1/Token/refresh` schema has `userename`, likely typo from Ombi. Refresh behavior is not sufficiently documented.
- External `/api` returns HTML; apps must call concrete `/api/v1/...` or `/api/v2/...` endpoints.
- This Ombi instance is version `4.60.15` on branch `Develop`; endpoints may differ on stable Ombi versions.
- Some endpoints may return 200 with `RequestEngineResult.isError=true`; the app must inspect response body, not only HTTP status.
- Request visibility can be affected by Ombi server settings, including hiding requests from users.
- Vantafyn must never log ApiKey, passwords, Bearer tokens, or secret headers.
## Deferred Actions

Request cancellation and admin approve/deny remain hidden until endpoint behavior is confirmed and tested against the live target Ombi API. Vantafyn should show status only for requested/pending items instead of exposing broken controls.

Selected TV episode requests are also deferred. The implemented TV request UI uses confirmed v2 flags only: `requestAll`, `firstSeason`, and `latestSeason`.

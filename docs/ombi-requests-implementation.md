# Ombi Requests Implementation

## Repository Findings

- UI stack: Jetpack Compose with Material 3 and shared Vantafyn components in `core-ui`.
- Navigation: mobile navigation is enum/state driven inside `feature-home`; Requests is an existing bottom-rail destination rendered by `feature-requests`.
- Requests route: `RequestsScreen(session = state.session)` is hosted from `MobileDestination.Requests`.
- Ombi code: `core-ombi` owns Ombi configuration, auth/session storage, API calls, request mapping, and request provider behavior.
- Ombi configuration: base URL, access mode, identity mode, API alias, last test result, version, and capabilities are stored in `OmbiConfigStore`.
- Active Jellyfin identity: `JellyfinSession` contains server config, user, `profileId`, and internal access token.
- Jellyfin API: `core-jellyfin` wraps Jellyfin SDK repositories and exposes app-level domain models.
- Image loading: Coil 3 Compose is already used. Requests uses Coil through `AsyncImage`.
- Dependency injection: no Hilt/Koin setup is present; ViewModels instantiate repositories directly from `Application` context.
- State management: feature ViewModels use Kotlin `StateFlow`; Compose screens collect state from ViewModels.
- Local caching/database: no Room database is present. Current persistence uses SharedPreferences and encrypted secret storage.
- Secure credentials: `EncryptedIntegrationAuthStorage` uses Android Keystore AES-GCM. Ombi API keys and per-user tokens are stored there.
- Network logging/crash reporting: no central logging or crash SDK is configured. Code must not log credentials, auth headers, tokens, or passwords.
- Design system: `VantafynColors`, `VantafynGlassPanel`, `VantafynGlassCard`, `VantafynButton`, spacing, and background components live in `core-ui`.
- Existing detail patterns: mobile Jellyfin detail pages use edge-to-edge artwork, dark gradients, flat back buttons, premium chips, and action panels in `feature-home`.
- Jellyfin handoff: native media opening exists for Jellyfin item IDs, but provider-ID lookup from Ombi metadata to Jellyfin items is not wired yet.

## Verified Ombi API Basis

The current implementation is based on the audited Ombi Swagger captured in `docs/OMBI_SWAGGER_ENDPOINT_AUDIT.md`.

Verified runtime from the audit:

- Ombi version: `4.60.15`
- Swagger title: `Ombi Api V1`

Routes used by Vantafyn:

- `POST /api/v1/Token`
- `GET /api/v1/Identity`
- `GET /api/v1/Search/movie/{searchTerm}`
- `GET /api/v1/Search/tv/{searchTerm}`
- `POST /api/v1/Request/movie`
- `POST /api/v2/Requests/tv` with fallback to `POST /api/v1/Request/tv`
- `GET /api/v1/Request/movie`
- `GET /api/v1/Request/tv`
- `GET /api/v2/Requests/movie/{count}/{position}/{sort}/{sortOrder}?requestedBy={userId}`
- `GET /api/v2/Requests/tv/{count}/{position}/{sort}/{sortOrder}?requestedBy={userId}`
- `GET /api/v2/Requests/recentlyRequested`
- `GET /api/v2/Requests/movie/available/{count}/{position}/{sort}/{sortOrder}`
- `GET /api/v2/Requests/tv/available/{count}/{position}/{sort}/{sortOrder}`
- `GET /api/v2/Search/movie/popular/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/movie/nowplaying/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/movie/upcoming/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/movie/toprated/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/tv/trending/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/tv/popular/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/tv/anticipated/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/tv/mostwatched/{currentPosition}/{amountToLoad}`
- `GET /api/v2/Search/movie/{movieDbId}`
- `GET /api/v2/Search/tv/{tvdbId}`
- `GET /api/v2/Search/tv/moviedb/{moviedbid}`

Unsupported or unproven routes are kept behind graceful fallback. Vantafyn now uses Ombi's verified v2 search/browse endpoints for official movie and TV discovery rails.

## Architecture Added

`core-ombi` now exposes stable app-level models for the Requests UI:

- `RequestMediaType`
- `RequestState`
- `RequestMediaSummary`
- `OmbiDiscoverRailKind`
- `OmbiDiscoverRail`
- `OmbiUserCapabilities`

Compose does not consume raw Ombi JSON. `OmbiRepository` maps transport responses into Vantafyn domain models and centralizes request-state mapping.

## Authentication

Vantafyn supports:

- shared API-key mode for existing installations
- per-user Ombi account mode with bearer-token requests

Per-user linking flow:

1. Admin configures the Ombi server.
2. Admin marks a Jellyfin profile as having an Ombi account created.
3. The user signs in from Requests with Ombi username/password.
4. Vantafyn posts to `/api/v1/Token`.
5. Vantafyn validates the token with `/api/v1/Identity`.
6. Vantafyn stores only the bearer token in Android encrypted storage.
7. Search, request, and personal history calls use `Authorization: Bearer <token>`.

Passwords are not stored. Tokens are keyed by Jellyfin profile ID. The next hardening step is to include the Jellyfin server ID explicitly in the stored Ombi session key, although the current Jellyfin `profileId` already includes the server URL hash and user ID.

## Requests Home

The Requests destination now has a native discovery surface:

- premium hero area
- tabs for Discover, Search, and My Requests
- debounced native search
- official Ombi browse rows for popular/now-playing/upcoming/top-rated movies
- official Ombi browse rows for trending/popular/anticipated/most-watched series
- poster rails with stable keys
- independent discovery loading/error state
- request detail surface backed by Ombi movie/series detail endpoints
- request actions that disable duplicate taps

Only rails backed by verified Ombi endpoints are shown:

- Popular movies
- Now playing
- Upcoming movies
- Top-rated movies
- Trending series
- Popular series
- Anticipated series
- Most-watched series
- Recently requested
- Family queue, only for admins or users with inferred queue visibility
- Recently available
- My requests

Empty rails are hidden.

## Request State Mapping

`RequestState` is derived centrally from Ombi fields:

- `available`, `fullyAvailable`, `markedAsAvailable`
- `partlyAvailable`
- `requested`, `requestId`
- `approved`
- `processing`, `requestStatus`
- `denied`

Unknown server states are mapped to `Unknown` with calm user-facing copy.

## Current Limitations

- Native provider-ID handoff into Jellyfin is documented but not wired in this pass.
- Rich movie detail calls through `/api/v2/Search/movie/{movieDbId}` are mapped for title, artwork, overview, runtime, genres, rating, tagline, and status.
- Rich series detail calls through `/api/v2/Search/tv/{tvdbId}` and `/api/v2/Search/tv/moviedb/{moviedbid}` are mapped for title, artwork, overview, genres, network, status, and season/episode request state where Ombi returns it.
- Season/episode display is present, but selected-season or selected-episode request submission still needs confirmation UI and request-body construction.
- Request cancellation endpoints are documented in Swagger but not wired.
- No Room cache exists in the app yet; this pass keeps the existing persistence approach.
- No automated MockWebServer tests were added in this slice.

## Manual Test Focus

Use the Requests tab with:

- configured Ombi server URL at root path and behind a reverse-proxy base path
- shared API-key mode
- per-user account mode
- valid Ombi user
- invalid Ombi user/password
- expired or revoked token
- movie request allowed
- TV request allowed
- user with no Ombi mapping
- user with account-created mapping
- existing requested movie
- existing available movie
- partially available series
- Ombi unavailable
- Jellyfin user switch
- Jellyfin server switch

# My List / Favorites

Vantafyn's **My List** maps directly to Jellyfin `UserData.IsFavorite`.

## Source Of Truth

- Jellyfin user data is authoritative.
- Vantafyn does not maintain a separate local My List database.
- Favorites are fetched with the active Jellyfin session through `JellyfinFavoritesRepository`.
- Add/remove actions are written through `JellyfinMediaRepository.setFavorite(...)`.

## API Routing

Favorite writes must use only the active Jellyfin session:

- active Jellyfin server URL from `JellyfinSession.server.url`
- active Jellyfin access token from `JellyfinSession.accessToken`
- active Jellyfin user id from `JellyfinSession.user.id`
- Jellyfin SDK `userLibraryApi.markFavoriteItem(...)`
- Jellyfin SDK `userLibraryApi.unmarkFavoriteItem(...)`
- Jellyfin SDK `itemsApi.getItemUserData(...)` to verify the resulting state

My List/Favorites must never route through:

- Ombi
- integration providers
- server discovery
- connection-test clients
- request provider configuration

## UI Behavior

Favorite changes are optimistic:

1. The tapped item updates immediately.
2. Vantafyn calls the Jellyfin favorite API.
3. The repository verifies Jellyfin `UserData.IsFavorite`.
4. On success, the optimistic state remains and Favorites refreshes quietly.
5. On failure, the previous state is restored and a contextual error is shown.

Favorite failures do not use the generic server validation modal. The expected user-facing errors are:

- `Couldn't update My List. Check your server connection and try again.`
- `Couldn't reach Jellyfin. Check your server connection and try again.`
- `Session expired. Please sign in again.`

## Regression Checks

- Detail page heart adds and removes My List.
- Detail More menu adds and removes My List.
- Long-press context actions add and remove My List.
- Favorites screen remove action removes the item from the list.
- Reopening a detail page reflects Jellyfin `UserData.IsFavorite`.
- Relaunching the app preserves the Jellyfin favorite state.

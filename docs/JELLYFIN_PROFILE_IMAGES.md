# Jellyfin Profile Images

Vantafyn supports real Jellyfin user profile image editing on Android mobile. This is a Jellyfin feature, not an Ombi integration.

## Current Behavior

- The current user can change or remove their own profile picture from `Settings`.
- A Jellyfin administrator can change or remove another user's profile picture from `Admin > User Settings`.
- Android TV does not expose an image picker. TV continues to display profile images returned by Jellyfin.
- Profile picker, saved profiles, top-right home avatar, settings, and admin user views use the refreshed Jellyfin user image URL after upload.

## Jellyfin APIs

The implementation uses the Jellyfin Kotlin SDK in `core-jellyfin`:

- `imageApi.getUserImageUrl(userId)`
- `imageApi.postUserImage(userId, FileInfo(bytes, mimeType))`
- `imageApi.deleteUserImage(userId)`
- `userApi.getCurrentUser()`
- `userApi.getUserById(userId)`
- `userApi.getPublicUsers()`

Vantafyn app and feature UI do not call the Jellyfin SDK directly. UI calls `VantafynHomeViewModel`, which calls `JellyfinUserPreferencesRepository` for the current user and `JellyfinAdminRepository` for admin-targeted user edits.

## Permission Model

- Normal users only target their own Jellyfin user id.
- Admin user-image actions check `session.user.isAdministrator` before calling admin APIs.
- A Jellyfin `401` or `403` is shown as: "You don't have permission to change this profile picture."
- Unsupported or unavailable endpoints show a clean profile-image error without exposing endpoint details or SDK exceptions.

## Image Picker Flow

Mobile uses Android Photo Picker:

1. User taps avatar or `Change photo`.
2. Android opens an image-only picker without broad storage permission.
3. Vantafyn center-crops the selected image to square.
4. Vantafyn resizes it to 768 x 768.
5. Vantafyn compresses it as JPEG at quality 90.
6. User confirms the preview.
7. `core-jellyfin` uploads the binary image body to Jellyfin.

No raw selected image file is stored permanently.

## Cache Invalidation

Jellyfin returns `PrimaryImageTag` on user DTOs. Vantafyn appends that tag as a cache key to user image URLs. After upload or delete:

- current-user uploads refresh the session with `getCurrentUser()`;
- admin uploads refresh the edited user with `getUserById(userId)`;
- saved profiles are reloaded;
- public profile picker users are reloaded when possible;
- admin overview is refreshed for admins.

This avoids needing an app restart after changing the image.

## Limitations

- The first mobile pass uses a simple polished preview with center-crop, not a full pinch/drag crop editor.
- TV upload controls are intentionally hidden because Android TV does not have a reliable native photo picker flow here.
- If a Jellyfin server rejects user image writes, Vantafyn surfaces the failure but does not emulate or fake profile images locally.

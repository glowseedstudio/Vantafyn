# Home Interactions

## Long-Press Context Menus

Mobile media cards now support a Vantafyn-styled long-press context menu on home rows, library items, search results, and My List items.

Actions are intentionally non-destructive:

- View details
- Add to My List
- Remove from My List when the item came from My List
- Mark watched
- Mark unwatched
- Downloads placeholder

My List maps to Jellyfin per-user favorite state. The app does not delete server media and does not expose destructive admin actions.

If Jellyfin rejects a user-data write because of permissions, Vantafyn shows a clean message and leaves server media untouched.

## Live TV Guide

The Live TV Guide button no longer starts playback directly. It opens a safe guide summary with channel/program counts and a clean `Guide data unavailable` state when Jellyfin returns no guide data. Channels remain playable from guide rows when present.

## Hero Freshness

Home hero candidates are still built from real Jellyfin content: continue watching, recently added movies, next up, and recently added TV. The candidate order now shuffles on an hourly session seed so app launches do not always present the same ordered hero list unless the server has very little eligible media.

## Onboarding Transitions

Setup transitions use a slower foreground fade. The intent is to keep the nebula/background treatment calm and avoid abrupt panel swaps. A later polish pass can remove the remaining internal per-screen background wrappers if further flicker is observed on device.

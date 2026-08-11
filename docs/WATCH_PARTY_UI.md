# Watch Party UI

## Entry Point

Mobile Watch Party is currently available from Profile settings. It is intentionally not on the bottom rail, keeping the rail focused on daily navigation.

## Screen Structure

The mobile screen is a single scroll with separated premium panels:

- compact title/header
- room setup card with Jellyfin server context
- mode selector for `Watch this` or `Swipe to match`
- selected media card for fixed-title parties
- Watch Party settings
- recipient picker for active Jellyfin sessions
- create, refresh, and leave actions
- matching rules
- loading/error state
- swipe deck or match result
- sender-side invite animation
- app-wide receiver invite overlay

## Visual Language

The UI follows the existing Vantafyn mobile language:

- dark cinematic background
- graphite glass panels
- rounded poster-forward media card
- vivid blue/violet accent only for primary actions and positive match motion
- restrained rose treatment for No votes
- no default debug dialogs

## App-Wide Invite Overlay

The receiver invite card is hosted above the mobile app root so it can appear over Home, Library, Search, Requests, Details, Music, Settings/Profile, and safe player surfaces.

It is not a system notification and does not claim closed-app delivery.

## Player Integration

The mobile player now renders a small Watch Party pill when a party is active. It shows member count and an honest sync label such as `Watch Party active`, `Sync unknown`, or `Reconnecting`. It does not show `Synced` yet.

Verified per-member ready/buffer state remains pending until Jellyfin exposes reliable state for that use.

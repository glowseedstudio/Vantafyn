# Watch Party Invites

## Modes

Vantafyn supports two invite modes in the mobile Watch Party flow:

- `Watch this`: a fixed-title party started from a movie, episode, or selected media item.
- `Swipe to match`: a party where members join first and choose a title through the swipe matcher.

## Detail Page Entry

The detail page More sheet includes `Start Watch Party` for playable non-music, non-Live-TV media. The next sheet asks the user to choose:

- `Watch this title`
- `Let the group choose`

For series, Vantafyn does not start ambiguous whole-series playback directly. Episode selection still needs a dedicated episode picker before fixed-title series playback is considered complete.

## Recipient Picker

The Watch Party screen has an `Invites` panel:

- active Jellyfin sessions are shown as `Available now`
- rows show avatar, user/client/device where Jellyfin exposes them
- users can select recipients and send invites

If no sessions are exposed, Vantafyn shows a premium empty state instead of pretending delivery is available.

## Send Animation

After a successful send, a glass invite card appears and travels upward out of the panel. This is the sender-side half of the intended Vantafyn invite motion.

Sender copy is intentionally honest: invites are sent through Jellyfin active sessions and will appear while the recipient has Vantafyn open and connected.

## Receiver Card

Vantafyn now hosts a mobile app-wide invite overlay near the root composition. When the websocket receives a Vantafyn Watch Party invite message, the invite enters the central inbox and appears as a premium top-slide card.

Fixed-title cards show:

- host name/avatar treatment
- title/artwork when present
- media type/countdown
- Accept and Decline

Swipe-to-match cards show:

- host name/avatar treatment
- pick-together copy
- swipe/match explanation
- Accept and Decline

Duplicate invite IDs are de-duped. Multiple invites queue one at a time. Expired invites are removed and cannot be accepted.

## Delivery

Current delivery uses Jellyfin:

- `SessionApi.getSessions(...)`
- `SessionApi.sendMessageCommand(...)`

The payload is non-secret. It contains ids and display strings needed to reconstruct an invite:

- invite id
- party id
- mode
- optional media item id/title
- host display name
- created/expiry timestamps
- non-secret media artwork URL when available

Tokens and server secrets are not included.

## Current Limitations

- no closed-app push notification delivery
- sender accept/decline acknowledgement remains delivery-unknown unless Jellyfin forwards a supported response path
- ready-state synchronization is local/honest unless Jellyfin exposes reliable member state
- optional FCM/backend push for closed-app delivery

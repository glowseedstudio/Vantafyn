# Ombi User Access

Vantafyn does not require Ombi. When Ombi is enabled, Requests can work in two identity modes.

## Shared API Key Mode

This is the current default.

An admin configures Ombi once with a server URL and API key. Normal users can request movies and shows through that configured Ombi identity. This is simple and works well for home and family setups, but Ombi may show requests under the shared/API account rather than each separate user.

Normal users do not enter an Ombi URL or API key in this mode.

## Per-User Ombi Account Mode

This mode is for accountability. Each user links their own Ombi account after the admin has created or approved that Ombi profile.

Current implementation:

- users without a mapping see a premium access-needed state
- users can tap `Request access`
- Vantafyn stores a local access request for admins on this device/server setup
- admins can mark account created, mark linked, dismiss, or clear mappings
- users with an account-created mapping see an Ombi username/password sign-in form
- Vantafyn exchanges credentials through `POST /api/v1/Token`, validates with `GET /api/v1/Identity`, then stores only the returned token
- linked users can search Ombi, submit requests, and restore their session on the next app open

The user sign-in form only asks for Ombi username/email and password. It always uses the admin-configured Ombi URL behind the scenes.

Not implemented yet:

- automatic Ombi user creation
- token refresh through `/api/v1/Token/refresh`
- synced access requests across devices

These are intentionally not faked. Vantafyn requires the admin to create the Ombi account in Ombi, then Vantafyn links to it from the user's Requests tab.

## Local-Only Access Requests

Access requests include:

- Jellyfin user id
- Jellyfin display name
- server name
- timestamp
- suggested Ombi username
- status
- optional note

They are local to this Vantafyn installation for now. The UI says this honestly. A future Vantafyn backend or server plugin can sync these tasks.

## Security

The shared Ombi API key and per-user Ombi session tokens are stored with Android encrypted storage backed by the device keystore.

Vantafyn does not store Ombi passwords.

Vantafyn does not log Ombi API keys, passwords, tokens, auth headers, or secret URLs.

Normal users cannot navigate into Ombi setup or management screens. The UI hides those routes, and the Requests ViewModel rejects setup/write actions unless the current Jellyfin profile is an admin.

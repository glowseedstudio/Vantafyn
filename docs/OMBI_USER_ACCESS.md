# Ombi User Access

Vantafyn does not require Ombi. When Ombi is enabled, Requests can work in two identity modes.

## Shared API Key Mode

This is the current default.

An admin configures Ombi once with a server URL and API key. Normal users can request movies and shows through that configured Ombi identity. This is simple and works well for home and family setups, but Ombi may show requests under the shared/API account rather than each separate user.

## Per-User Ombi Account Mode

This mode is for accountability. Each user should eventually link their own Ombi account.

Current implementation:

- users without a mapping see `Requests need access`
- users can tap `Request access`
- Vantafyn stores a local access request for admins on this device/server setup
- admins can mark account created, mark linked, dismiss, or clear mappings

Not implemented yet:

- automatic Ombi user creation
- per-user Ombi password login
- per-user token requests
- synced access requests across devices

These are intentionally not faked. Vantafyn needs a confirmed safe Ombi user/token API before it stores user tokens or creates accounts.

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

The shared Ombi API key is stored with Android encrypted storage backed by the device keystore.

Vantafyn does not store Ombi passwords. Per-user tokens will only be stored if a safe token flow is implemented later, and they must use encrypted storage.

Vantafyn does not log Ombi API keys, passwords, tokens, auth headers, or secret URLs.

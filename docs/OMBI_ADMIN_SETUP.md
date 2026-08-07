# Ombi Admin Setup

Ombi is optional. Vantafyn works as a Jellyfin client without it.

## Enable Ombi

1. Sign into Vantafyn with a Jellyfin admin profile.
2. Open `Profile / Settings`.
3. Open `Integrations & Requests`.
4. Choose `Set up Ombi`.
5. Enter your Ombi URL.
6. Enter your Ombi API key.
7. Test the connection.
8. Choose who can use Requests.
9. Tap `Enable Requests`.

Only Jellyfin admins can open this setup flow. Normal users cannot see or edit Ombi server URLs, API keys, setup health, access modes, identity modes, capability checks, user mappings, or reset controls.

## Ombi URL

Use the address every Vantafyn device can reach. This admin-configured URL is reused for all normal users.

- Home-only use: a local address such as `http://192.168.1.29:5000` can work.
- Remote use: use your Ombi domain, such as `https://requests.example.com`.
- For public remote access, HTTPS is recommended.
- `localhost` only works on the device running Ombi. Phones, TVs, and Fire OS devices need a reachable IP address or domain.

Local addresses usually do not work when users are away from the home network.

## API Key

Find the API key in Ombi settings. The exact menu can vary by Ombi version.

Vantafyn stores the key securely on the device using Android encrypted storage. Vantafyn does not log Ombi credentials.

## Account Model

Shared API key mode remains the default.

The configured Ombi API key is the identity used for native Ombi requests. Vantafyn may send the Jellyfin display name as `ApiAlias`, but Ombi decides how that is attributed.

Per-user Ombi login is also supported. The admin still configures the Ombi URL and API key for setup/testing, then users with an account-created mapping can sign in with their own Ombi account from the Requests tab.

## Access Modes

- Disabled: Requests is hidden from normal users.
- Admins only: only admin users can use Requests.
- All users: normal users see the Requests tab and can search/request according to Ombi permissions.

## User Identity Mode

Shared request account is the default. Requests are made through the configured Ombi API key identity.

Each user links Ombi account lets users request Ombi access, then sign in once their Ombi account exists. Vantafyn stores only the returned Ombi session token, not the password.

## Access Requests

When per-user account mode is selected, unlinked normal users see `Requests need access`. They can request access once. The request appears in Vantafyn for admins on this device/server setup.

Admin actions are manual and non-destructive:

- mark account created
- mark linked
- dismiss
- clear local mapping

Vantafyn does not auto-create Ombi users.

After `mark account created`, the user sees an Ombi sign-in form. If no mapping exists for that Jellyfin profile, the Requests tab shows a contact-admin access message instead of a broken login form.

## Setup Health

Setup health is admin-only. It can show whether Ombi is reachable, whether the API key is valid, the configured URL, URL safety warnings, detected request capabilities, shared vs per-user mode, linked users, and local access requests.

Normal users never see setup health, API key state, endpoint details, Swagger/audit notes, or request-engine diagnostics.

Admins can also refresh the Vantafyn Jellyfin availability index from Ombi management. This matches Ombi discovery/search results to the active Jellyfin server by provider ID so available items can open directly in Vantafyn.

## Capabilities

Vantafyn detects and displays:

- movie search
- TV search
- movie request
- TV request
- request listing
- admin moderation

Approve/deny moderation is not wired yet. Vantafyn hides moderation buttons until the exact Ombi endpoint is confirmed against the target Ombi Swagger/API.

## Troubleshooting

- Bad URL: check the scheme, host, port, and reverse proxy path.
- Bad API key: generate or copy the key again from Ombi.
- Server unreachable: confirm Ombi is running and reachable from the device.
- CORS: not relevant for Vantafyn's native API calls.
- Local address away from home: use a remote URL or VPN.
- Permission denied: the configured Ombi API key does not have permission for that action.
- HTML response: the URL is likely a reverse proxy/web route instead of the Ombi API.

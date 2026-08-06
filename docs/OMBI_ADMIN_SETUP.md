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

## Ombi URL

Use the address your users can reach.

- Home-only use: a local address such as `http://192.168.1.29:5000` can work.
- Remote use: use your Ombi domain, such as `https://requests.example.com`.
- For public remote access, HTTPS is recommended.

Local addresses usually do not work when users are away from the home network.

## API Key

Find the API key in Ombi settings. The exact menu can vary by Ombi version.

Vantafyn stores the key securely on the device using Android encrypted storage. Vantafyn does not log Ombi credentials.

## Account Model

Current mode is shared API key mode.

The configured Ombi API key is the identity used for native Ombi requests. Vantafyn may send the Jellyfin display name as `ApiAlias`, but Ombi decides how that is attributed.

Per-user Ombi login is future work. It is not currently required for Vantafyn users to submit requests.

## Access Modes

- Disabled: Requests is hidden from normal users.
- Admins only: only admin users can use Requests.
- All users: normal users see the Requests tab and can search/request according to Ombi permissions.

## User Identity Mode

Shared request account is the default. Requests are made through the configured Ombi API key identity.

Each user links Ombi account is available as an access-tracking mode. Users can request Ombi access, and admins can manage those local tasks. Per-user credential linking is not enabled until a safe Ombi token/login endpoint is confirmed.

## Access Requests

When per-user account mode is selected, unlinked normal users see `Requests need access`. They can request access once. The request appears in Vantafyn for admins on this device/server setup.

Admin actions are manual and non-destructive:

- mark account created
- mark linked
- dismiss
- clear local mapping

Vantafyn does not auto-create Ombi users.

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

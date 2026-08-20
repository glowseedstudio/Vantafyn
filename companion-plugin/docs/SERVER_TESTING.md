# Vantafyn Companion Server Testing Guide

This guide is for installing and testing the optional Vantafyn Companion Jellyfin plugin on your own Jellyfin server.

Vantafyn works without this plugin. The plugin adds server-side Vantafyn features such as Companion capability detection, server-managed Ombi request routing, per-user Ombi login, Watch Party foundations, user settings sync, and personal playlist storage foundations.

## Current Status

- Plugin name: `Vantafyn Companion`
- Plugin version: `0.1.0`
- Plugin GUID: `fd7d0e8a-89a9-45a6-8f2b-1f4c5bb1c8cb`
- Jellyfin baseline used by the project: `10.11.11`
- Target framework: `.NET 9` / `net9.0`
- Package output folder: `companion-plugin/artifacts/Vantafyn.Plugin.Companion_0.1.0`

App integration is already present:

- The Android app checks `GET /Vantafyn/Capabilities`.
- If Companion is not installed, unavailable, or not configured, the app keeps using normal Jellyfin and local/native Ombi fallback behavior.
- If Companion reports Requests as ready, the Requests screen uses Companion endpoints.
- If Companion requires user login, each Vantafyn/Jellyfin profile signs into its own Ombi account through the existing premium user sign-in flow.
- The app does not expose the server Ombi API key to normal users.

## Local Validation

This Codex environment could inspect the source and Android wiring, but could not execute the plugin build or tests because `dotnet` is not installed here.

Run these on a machine with the .NET SDK installed:

```bash
cd /home/glowseed/Documents/coding\ projects/Vantafyn/companion-plugin
./scripts/test.sh
./scripts/build.sh
```

Expected build output:

```text
companion-plugin/artifacts/Vantafyn.Plugin.Companion_0.1.0/
```

That folder should contain:

- `Vantafyn.Plugin.Companion.dll`
- `Vantafyn.Plugin.Companion.deps.json`
- `Vantafyn.Plugin.Companion.xml`
- `meta.json`

## Manual Install For First Testing

Manual install is the fastest path for local testing.

1. Stop Jellyfin.
2. Find the Jellyfin plugin data folder.
   - Linux package installs commonly use `/var/lib/jellyfin/plugins`.
   - Docker installs use the plugins folder inside the mounted Jellyfin config/data volume.
   - Windows installs use Jellyfin's data directory for the service/user.
3. Create a plugin folder:

```bash
mkdir -p /path/to/jellyfin/plugins/Vantafyn.Plugin.Companion
```

4. Copy the built artifact contents into it:

```bash
cp -a companion-plugin/artifacts/Vantafyn.Plugin.Companion_0.1.0/. /path/to/jellyfin/plugins/Vantafyn.Plugin.Companion/
```

5. Start Jellyfin.
6. Open Jellyfin Dashboard -> Plugins.
7. Confirm `Vantafyn Companion` appears.
8. Open the Vantafyn Companion configuration page.

If you use Docker, copy into the mounted config volume rather than into a temporary container filesystem.

## Plugin Manager / Catalog Install

Jellyfin's Plugin Manager installs third-party plugins from a hosted plugin repository manifest. It cannot install directly from a local source folder.

To test through Plugin Manager:

1. Build the plugin.
2. Zip the artifact folder contents.
3. Host the zip somewhere Jellyfin can download over HTTP/HTTPS.
4. Create and host a Jellyfin plugin repository manifest JSON.
5. Add that manifest URL in Jellyfin Dashboard -> Plugins -> Repositories.
6. Install `Vantafyn Companion` from the catalog.
7. Restart Jellyfin when Jellyfin asks you to.

Example package command:

```bash
cd /home/glowseed/Documents/coding\ projects/Vantafyn/companion-plugin
./scripts/build.sh
cd artifacts/Vantafyn.Plugin.Companion_0.1.0
zip -r ../Vantafyn.Plugin.Companion_0.1.0.zip .
sha256sum ../Vantafyn.Plugin.Companion_0.1.0.zip
```

Use the resulting checksum in the hosted manifest. The current local `meta.json` intentionally has an empty `sourceUrl` and checksum, so it is useful for manual install but not enough for Plugin Manager catalog install by itself.

Example repository manifest shape:

```json
[
  {
    "category": "General",
    "guid": "fd7d0e8a-89a9-45a6-8f2b-1f4c5bb1c8cb",
    "name": "Vantafyn Companion",
    "description": "Optional server-side companion features for Vantafyn clients.",
    "overview": "Settings sync, Requests/Ombi, Watch Parties, live invitations, and private playlist foundations for Vantafyn.",
    "owner": "Glowseed Studio",
    "versions": [
      {
        "version": "0.1.0",
        "targetAbi": "10.11.11.0",
        "sourceUrl": "https://your-host.example.com/Vantafyn.Plugin.Companion_0.1.0.zip",
        "checksum": "sha256 checksum from sha256sum",
        "changelog": "Initial local development build."
      }
    ]
  }
]
```

## Configure Companion

In Jellyfin Dashboard -> Plugins -> Vantafyn Companion:

1. Keep `Profile & Settings Sync` enabled.
2. Keep `Requests / Ombi` enabled if you want Vantafyn Requests.
3. Enter the Ombi URL, for example:

```text
https://requests.example.com
```

or for local testing:

```text
http://192.168.1.19:5000
```

4. Enter an Ombi API key.
5. Choose whether each Vantafyn profile must sign into its own Ombi account.
   - Recommended for your setup: enabled, so requests are made under each user's Ombi account.
   - If disabled, Companion uses the configured Ombi API key identity for request calls.
6. Click `Save`.
7. Click `Test Connection`.

## Verify Plugin Endpoints

Sign into Jellyfin in a browser first, then open:

```text
https://your-jellyfin-server/Vantafyn/Capabilities
```

Expected shape:

```json
{
  "pluginVersion": "0.1.0",
  "apiVersion": 1,
  "requests": {
    "state": "ready",
    "provider": "ombi",
    "serverConfigured": true,
    "requiresUserLogin": true,
    "userLinked": false
  }
}
```

If the endpoint returns 404, Jellyfin has not loaded the plugin. Restart Jellyfin and check the plugin folder path.

If `requests.state` is `unconfigured`, save the Ombi URL/API key in the plugin config page.

If `requiresUserLogin` is true and `userLinked` is false, this is expected until that Jellyfin profile signs into Ombi from Vantafyn.

## Test From Vantafyn

1. Install/open the current Vantafyn mobile build.
2. Sign into the Jellyfin server that has Companion installed.
3. Open Requests.
4. If per-user Ombi login is enabled, sign into the user's Ombi account from the Requests screen.
5. Search for a movie.
6. Submit a request.
7. Confirm the request appears in Ombi under that user's account.
8. Repeat with a TV series.
9. Sign in as a different Jellyfin profile and repeat to confirm the Ombi identity is separate.

Expected behavior:

- Normal users do not see the Ombi server API key.
- Normal users can sign into their own Ombi account when Companion requires user login.
- If the plugin is disabled or missing, Vantafyn should fall back cleanly and not break Jellyfin browsing/playback.
- If Ombi is unreachable, Vantafyn should show a clean Requests error rather than crashing.

## Recommended Test Matrix

- Admin Jellyfin profile with Companion installed and Ombi configured.
- Normal Jellyfin profile with its own Ombi account.
- Normal Jellyfin profile without an Ombi account.
- Requests with `Require user login` enabled.
- Requests with `Require user login` disabled.
- Movie search and request.
- Series search and request.
- Ombi unavailable.
- Bad Ombi API key.
- Plugin disabled in Jellyfin dashboard.
- Plugin removed from Jellyfin.

## Known v0.1 Limits

- Plugin Manager install requires you to host a repository manifest and zip package.
- Watch Parties are in-memory for v0.1, so active rooms do not survive a Jellyfin server restart.
- Background push is not implemented; Companion uses authenticated live connected-client events only.
- Ombi request moderation is not exposed through Companion yet.
- The plugin stores Ombi user session tokens server-side for the authenticated Jellyfin user when per-user mode is enabled. Passwords are not stored.

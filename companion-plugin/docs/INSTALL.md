# Install Vantafyn Companion

## Option 1: Jellyfin Plugin Repository (Recommended)

To install directly within Jellyfin without manual file copying:

1. Open Jellyfin **Dashboard** ➔ **Plugins** ➔ **Repositories**.
2. Click **`+` (Add)**.
3. Set:
   - **Repository Name**: `Vantafyn Companion Repository`
   - **Repository URL**:
     ```text
     https://raw.githubusercontent.com/glowseedstudio/Vantafyn/main/companion-plugin/manifest.json
     ```
4. Click **Save**.
5. Switch to the **Catalog** tab, locate **Vantafyn Companion**, and click **Install**.
6. Restart Jellyfin.

---

## Option 2: Manual Install

For a complete server test checklist, see [SERVER_TESTING.md](SERVER_TESTING.md).

## Build

```bash
cd companion-plugin
./scripts/build.sh
```

The release output is written to:

```text
artifacts/Vantafyn.Plugin.Companion_0.1.0
```

## Locate Jellyfin Plugins Directory

The plugin directory depends on how Jellyfin is installed. Check Jellyfin Dashboard or server logs for the data path.

Common examples:

- Linux package installs often use `/var/lib/jellyfin/plugins`.
- Docker installs use the plugins directory inside the mounted Jellyfin config/data volume.
- Windows installs use the Jellyfin data directory under the service/user profile.

Do not assume these paths; verify your server's actual data directory.

## Manual Install

1. Stop Jellyfin.
2. Create a plugin directory, for example `Vantafyn.Plugin.Companion`.
3. Copy the contents of `artifacts/Vantafyn.Plugin.Companion_0.1.0` into that directory.
4. Start Jellyfin.
5. Open Dashboard -> Plugins and verify Vantafyn Companion appears.
6. Open the Vantafyn Companion configuration page.
7. Verify `/Vantafyn/Capabilities` while authenticated.

## Development Deploy

```bash
./scripts/deploy.sh /path/to/jellyfin/plugins/Vantafyn.Plugin.Companion
```

or:

```bash
JELLYFIN_PLUGIN_DIR=/path/to/jellyfin/plugins/Vantafyn.Plugin.Companion ./scripts/deploy.sh
```

Restart Jellyfin after deployment.

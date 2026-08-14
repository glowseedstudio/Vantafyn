#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLUGIN_DIR="${1:-${JELLYFIN_PLUGIN_DIR:-}}"

if [[ -z "$PLUGIN_DIR" ]]; then
  echo "Usage: scripts/deploy.sh /path/to/jellyfin/plugins/Vantafyn.Companion" >&2
  echo "Or set JELLYFIN_PLUGIN_DIR." >&2
  exit 1
fi

"$ROOT/scripts/build.sh"
mkdir -p "$PLUGIN_DIR"
cp -R "$ROOT/artifacts/Vantafyn.Plugin.Companion_0.1.0/"* "$PLUGIN_DIR/"
echo "Copied Vantafyn Companion to $PLUGIN_DIR. Restart Jellyfin to load it."

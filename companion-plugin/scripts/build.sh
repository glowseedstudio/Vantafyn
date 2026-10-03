#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.dotnet:$PATH"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="$ROOT/src/Vantafyn.Plugin.Companion/Vantafyn.Plugin.Companion.csproj"
ARTIFACTS="$ROOT/artifacts"
VERSION="${1:-0.2.15}"
REPO_URL="https://github.com/glowseedstudio/Vantafyn"
TAG="${2:-v0.9.49}"
CHANGELOG="Restored Puck as the Pokédex narration default, added three fixed voice choices, and added uncached admin voice previews."

rm -rf "$ARTIFACTS"
mkdir -p "$ARTIFACTS"

echo "=== Building Vantafyn Companion Plugin v${VERSION} ==="

# 1. Build net9.0 (Jellyfin 10.11 baseline)
OUT_NET9="$ARTIFACTS/net9.0"
mkdir -p "$OUT_NET9"
dotnet publish "$PROJECT" -c Release -f net9.0 -o "$OUT_NET9"
cat > "$OUT_NET9/meta.json" <<JSON
{
  "category": "General",
  "guid": "fd7d0e8a-89a9-45a6-8f2b-1f4c5bb1c8cb",
  "name": "Vantafyn Companion",
  "description": "Server-side companion features for Vantafyn: UnifiedPush notifications, retro games, settings sync, and requests.",
  "overview": "UnifiedPush notifications, retro games, Ombi requests, settings sync, and watch parties.",
  "owner": "Glowseed Studio",
  "versions": [
    {
      "version": "${VERSION}.0",
      "targetAbi": "10.11.0.0",
      "sourceUrl": "${REPO_URL}/releases/download/${TAG}/Vantafyn.Plugin.Companion_${VERSION}_net9.zip",
      "checksum": "",
      "changelog": "${CHANGELOG}"
    }
  ]
}
JSON

ZIP_NET9="$ARTIFACTS/Vantafyn.Plugin.Companion_${VERSION}_net9.zip"
(cd "$OUT_NET9" && zip -q -r "$ZIP_NET9" .)
MD5_NET9=$(md5sum "$ZIP_NET9" | awk '{print $1}')
echo "net9.0 package: $ZIP_NET9 (MD5: $MD5_NET9)"

# 2. Build net10.0 (Jellyfin 12 baseline)
OUT_NET10="$ARTIFACTS/net10.0"
mkdir -p "$OUT_NET10"
dotnet publish "$PROJECT" -c Release -f net10.0 -o "$OUT_NET10"
cat > "$OUT_NET10/meta.json" <<JSON
{
  "category": "General",
  "guid": "fd7d0e8a-89a9-45a6-8f2b-1f4c5bb1c8cb",
  "name": "Vantafyn Companion",
  "description": "Server-side companion features for Vantafyn: UnifiedPush notifications, retro games, settings sync, and requests.",
  "overview": "UnifiedPush notifications, retro games, Ombi requests, settings sync, and watch parties.",
  "owner": "Glowseed Studio",
  "versions": [
    {
      "version": "${VERSION}.1",
      "targetAbi": "12.0.0.0",
      "sourceUrl": "${REPO_URL}/releases/download/${TAG}/Vantafyn.Plugin.Companion_${VERSION}_net10.zip",
      "checksum": "",
      "changelog": "${CHANGELOG}"
    }
  ]
}
JSON

ZIP_NET10="$ARTIFACTS/Vantafyn.Plugin.Companion_${VERSION}_net10.zip"
(cd "$OUT_NET10" && zip -q -r "$ZIP_NET10" .)
MD5_NET10=$(md5sum "$ZIP_NET10" | awk '{print $1}')
echo "net10.0 package: $ZIP_NET10 (MD5: $MD5_NET10)"

# 3. Create default unversioned directory for manual local installation (deploy.sh)
LOCAL_INSTALL="$ARTIFACTS/Vantafyn.Plugin.Companion_${VERSION}"
mkdir -p "$LOCAL_INSTALL"
cp -a "$OUT_NET9/." "$LOCAL_INSTALL/"

# 4. Generate Jellyfin Plugin Repository manifest.json
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
cat > "$ROOT/manifest.json" <<JSON
[
  {
    "category": "General",
    "guid": "fd7d0e8a-89a9-45a6-8f2b-1f4c5bb1c8cb",
    "name": "Vantafyn Companion",
    "description": "Server-side companion features for Vantafyn: UnifiedPush notifications, retro games, settings sync, and requests.",
    "overview": "UnifiedPush notifications, retro games, Ombi requests, settings sync, and watch parties.",
    "owner": "Glowseed Studio",
    "versions": [
      {
        "version": "${VERSION}.0",
        "changelog": "${CHANGELOG}",
        "targetAbi": "10.11.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/${TAG}/Vantafyn.Plugin.Companion_${VERSION}_net9.zip",
        "checksum": "${MD5_NET9}",
        "timestamp": "${TIMESTAMP}"
      },
      {
        "version": "${VERSION}.1",
        "changelog": "${CHANGELOG}",
        "targetAbi": "12.0.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/${TAG}/Vantafyn.Plugin.Companion_${VERSION}_net10.zip",
        "checksum": "${MD5_NET10}",
        "timestamp": "${TIMESTAMP}"
      },
      {
        "version": "0.2.6.0",
        "changelog": "SRAM erased 0xFF bitfield guards, Pokédex milestone achievement revocation, duplicate-key crash prevention, and Pokédex entry modal visual overhaul.",
        "targetAbi": "10.11.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/v0.9.38/Vantafyn.Plugin.Companion_0.2.6_net9.zip",
        "checksum": "a60ec65cae824e367a02976614f578a5",
        "timestamp": "2026-10-02T02:06:49Z"
      },
      {
        "version": "0.2.6.1",
        "changelog": "SRAM erased 0xFF bitfield guards, Pokédex milestone achievement revocation, duplicate-key crash prevention, and Pokédex entry modal visual overhaul.",
        "targetAbi": "12.0.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/v0.9.38/Vantafyn.Plugin.Companion_0.2.6_net10.zip",
        "checksum": "76bfa2cf414215691c0df555749a5fe7",
        "timestamp": "2026-10-02T02:06:49Z"
      },
      {
        "version": "0.2.4.0",
        "changelog": "Custom Pokémon modal and vault background support (local server file path or URL), fallback default dark gradient, and complete unbundling of third-party assets.",
        "targetAbi": "10.11.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/v0.9.36/Vantafyn.Plugin.Companion_0.2.4_net9.zip",
        "checksum": "f433d7c5f77f8fd31ed3ede25bae563a",
        "timestamp": "2026-10-01T12:52:06Z"
      },
      {
        "version": "0.2.4.1",
        "changelog": "Custom Pokémon modal and vault background support (local server file path or URL), fallback default dark gradient, and complete unbundling of third-party assets.",
        "targetAbi": "12.0.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/v0.9.36/Vantafyn.Plugin.Companion_0.2.4_net10.zip",
        "checksum": "98cf34d3b20f530eaced80e0f9577dd0",
        "timestamp": "2026-10-01T12:52:06Z"
      },
      {
        "version": "0.2.2.0",
        "changelog": "Settings UI fix for all checkboxes, PKVault integration, 30-box storage, direct transfers, and save backups.",
        "targetAbi": "10.11.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/v0.9.34/Vantafyn.Plugin.Companion_0.2.2_net9.zip",
        "checksum": "228c55789d45d9e7f5f72e4da7c6fb44",
        "timestamp": "2026-10-01T08:57:35Z"
      },
      {
        "version": "0.2.2.1",
        "changelog": "Settings UI fix for all checkboxes, PKVault integration, 30-box storage, direct transfers, and save backups.",
        "targetAbi": "12.0.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/v0.9.34/Vantafyn.Plugin.Companion_0.2.2_net10.zip",
        "checksum": "15a5fa1d2a66a4a07f284c2a5d717775",
        "timestamp": "2026-10-01T08:57:35Z"
      },
      {
        "version": "0.1.4.0",
        "changelog": "Retro Gaming support: ROM library discovery, console system resolver, range-supported ROM streaming, and cloud save states.",
        "targetAbi": "10.11.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/v0.9.26/Vantafyn.Plugin.Companion_0.1.4_net9.zip",
        "checksum": "93d01e93dc2f91a21aa51844b7549dd5",
        "timestamp": "2026-09-28T08:39:13Z"
      },
      {
        "version": "0.1.4.1",
        "changelog": "Retro Gaming support: ROM library discovery, console system resolver, range-supported ROM streaming, and cloud save states.",
        "targetAbi": "12.0.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/v0.9.26/Vantafyn.Plugin.Companion_0.1.4_net10.zip",
        "checksum": "2cca480809f1a932e7994814aa465025",
        "timestamp": "2026-09-28T08:39:13Z"
      }
    ]
  }
]
JSON

# Also copy manifest into artifacts
cp "$ROOT/manifest.json" "$ARTIFACTS/manifest.json"

echo "=== Build Complete ==="
echo "Manifest: $ROOT/manifest.json"
echo "net9 ZIP: $ZIP_NET9 (MD5: $MD5_NET9)"
echo "net10 ZIP: $ZIP_NET10 (MD5: $MD5_NET10)"

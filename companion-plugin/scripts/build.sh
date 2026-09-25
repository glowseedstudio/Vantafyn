#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.dotnet:$PATH"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="$ROOT/src/Vantafyn.Plugin.Companion/Vantafyn.Plugin.Companion.csproj"
ARTIFACTS="$ROOT/artifacts"
VERSION="0.1.3"
REPO_URL="https://github.com/glowseedstudio/Vantafyn"
TAG="v0.9.22"

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
  "description": "Server-side companion features for Vantafyn: UnifiedPush notifications, settings sync, and requests.",
  "overview": "UnifiedPush notifications, Ombi requests, settings sync, and watch parties.",
  "owner": "Glowseed Studio",
  "versions": [
    {
      "version": "${VERSION}.0",
      "targetAbi": "10.11.0.0",
      "sourceUrl": "${REPO_URL}/releases/download/${TAG}/Vantafyn.Plugin.Companion_${VERSION}_net9.zip",
      "checksum": "",
      "changelog": "Native emby-checkbox alignment and styling fix, ApiClient fetch compatibility, unified settings and Ombi login toggles."
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
  "description": "Server-side companion features for Vantafyn: UnifiedPush notifications, settings sync, and requests.",
  "overview": "UnifiedPush notifications, Ombi requests, settings sync, and watch parties.",
  "owner": "Glowseed Studio",
  "versions": [
    {
      "version": "${VERSION}.1",
      "targetAbi": "12.0.0.0",
      "sourceUrl": "${REPO_URL}/releases/download/${TAG}/Vantafyn.Plugin.Companion_${VERSION}_net10.zip",
      "checksum": "",
      "changelog": "Native emby-checkbox alignment and styling fix, ApiClient fetch compatibility, unified settings and Ombi login toggles."
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
    "description": "Optional server-side companion features for Vantafyn clients: UnifiedPush notification routing, settings sync, watch party foundations, and Ombi requests.",
    "overview": "UnifiedPush notification support, Ombi request routing, settings sync, and watch party coordination.",
    "owner": "Glowseed Studio",
    "versions": [
      {
        "version": "${VERSION}.0",
        "changelog": "Native emby-checkbox alignment and styling fix, ApiClient fetch compatibility, unified settings and Ombi login toggles.",
        "targetAbi": "10.11.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/${TAG}/Vantafyn.Plugin.Companion_${VERSION}_net9.zip",
        "checksum": "${MD5_NET9}",
        "timestamp": "${TIMESTAMP}"
      },
      {
        "version": "${VERSION}.1",
        "changelog": "Native emby-checkbox alignment and styling fix, ApiClient fetch compatibility, unified settings and Ombi login toggles.",
        "targetAbi": "12.0.0.0",
        "sourceUrl": "${REPO_URL}/releases/download/${TAG}/Vantafyn.Plugin.Companion_${VERSION}_net10.zip",
        "checksum": "${MD5_NET10}",
        "timestamp": "${TIMESTAMP}"
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

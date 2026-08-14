#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="$ROOT/src/Vantafyn.Plugin.Companion/Vantafyn.Plugin.Companion.csproj"
OUT="$ROOT/artifacts/Vantafyn.Plugin.Companion_0.1.0"

dotnet publish "$PROJECT" -c Release -o "$OUT"
cat > "$OUT/meta.json" <<'JSON'
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
      "sourceUrl": "",
      "checksum": "",
      "changelog": "Initial local development build."
    }
  ]
}
JSON

echo "Built plugin artifact at $OUT"

#!/usr/bin/env bash
set -euo pipefail

URL="${1:-http://localhost:5000/swagger/v1/swagger.json}"
TMP_FILE="${TMPDIR:-/tmp}/ombi-swagger-audit.json"

echo "Fetching Ombi Swagger from: ${URL}"
curl -fsS "${URL}" -o "${TMP_FILE}"

echo
echo "OpenAPI info:"
jq -r '.info | "title=\(.title // "unknown") version=\(.version // "unknown")"' "${TMP_FILE}"

echo
echo "Auth schemes:"
jq '.components.securitySchemes // {}' "${TMP_FILE}"

echo
echo "Candidate paths:"
jq -r '.paths | keys[]' "${TMP_FILE}" |
  rg -i 'token|login|auth|identity|user|me|current|profile|search|request|movie|tv|approve|deny|available|settings' |
  sort

echo
echo "Token endpoint summary:"
jq '.paths["/api/v1/Token"] // "not found"' "${TMP_FILE}"

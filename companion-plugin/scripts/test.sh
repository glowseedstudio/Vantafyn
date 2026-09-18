#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.dotnet:$PATH"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
dotnet test "$ROOT/tests/Vantafyn.Plugin.Companion.Tests/Vantafyn.Plugin.Companion.Tests.csproj"

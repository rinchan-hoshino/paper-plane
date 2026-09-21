#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
exec bash "$root/1.21.1/gradlew" -p "$root/1.21.1" "$@"

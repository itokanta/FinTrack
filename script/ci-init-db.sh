#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

for f in sql/*.sql; do
  echo "apply ${f}"
  psql -v ON_ERROR_STOP=1 -f "${f}"
done
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "quality-gate: agent-aigc contract checks"
./scripts/check-contracts.sh

echo "quality-gate: backend build"
(
  cd backend
  mvn -q -DskipTests package
)

echo "quality-gate: frontend build"
(
  cd frontend
  pnpm build
)

echo "quality-gate: backend runtime probe"
./scripts/probe-backend-dev.sh

echo "quality-gate: ok"

#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${AIGC_ACCEPTANCE_PORT:-${1:-18188}}"
ATTEMPTS="${AIGC_ACCEPTANCE_ATTEMPTS:-90}"
TMP_ROOT="${TMPDIR:-/tmp}"
LOG_FILE="$TMP_ROOT/agent-aigc-v1-acceptance.$PORT.log"
PID_FILE="$TMP_ROOT/agent-aigc-v1-acceptance.$PORT.pid"
BASE_URL="http://127.0.0.1:$PORT"

cleanup() {
  if [[ -f "$PID_FILE" ]]; then
    local pid
    pid="$(cat "$PID_FILE")"
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" >/dev/null 2>&1 || true
    rm -f "$PID_FILE"
  fi
}

fail() {
  echo "v1-teaching-acceptance: $*" >&2
  if [[ -f "$LOG_FILE" ]]; then
    tail -n 180 "$LOG_FILE" >&2 || true
  fi
  exit 1
}

command -v curl >/dev/null 2>&1 || fail "curl is required"

cd "$ROOT"

echo "v1-teaching-acceptance: quality gate"
"$ROOT/scripts/quality-gate.sh"

cleanup
trap cleanup EXIT
rm -f "$LOG_FILE"

echo "v1-teaching-acceptance: start backend on $BASE_URL"
(
  cd "$ROOT/backend"
  SPRING_PROFILES_ACTIVE=dev SERVER_PORT="$PORT" mvn -q spring-boot:run >"$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
)

pid="$(cat "$PID_FILE")"
for _ in $(seq 1 "$ATTEMPTS"); do
  kill -0 "$pid" >/dev/null 2>&1 || fail "backend process exited before health check passed"

  if curl -fsS "$BASE_URL/api/test/health" >/dev/null 2>&1; then
    echo "v1-teaching-acceptance: AIGC smoke"
    "$ROOT/scripts/aigc-demo-smoke.sh" "$BASE_URL"
    echo "v1-teaching-acceptance: ok"
    echo "v1-teaching-acceptance: verified=scaffold-contracts,backend-build,frontend-build,runtime-openapi,aigc-smoke"
    exit 0
  fi

  sleep 1
done

fail "backend did not pass health check in ${ATTEMPTS}s"

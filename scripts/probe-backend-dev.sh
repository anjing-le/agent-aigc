#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${1:-18180}"
ATTEMPTS="${PROBE_ATTEMPTS:-90}"
TMP_ROOT="${TMPDIR:-/tmp}"
LOG_FILE="$TMP_ROOT/agent-aigc-backend-probe.$PORT.log"
PID_FILE="$TMP_ROOT/agent-aigc-backend-probe.$PORT.pid"
HEALTH_FILE="$TMP_ROOT/agent-aigc-backend-health.$PORT.json"
FEATURES_FILE="$TMP_ROOT/agent-aigc-backend-features.$PORT.json"
OPENAPI_FILE="$TMP_ROOT/agent-aigc-backend-openapi.$PORT.json"

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
  echo "probe-backend-dev: $*" >&2
  if [[ -f "$LOG_FILE" ]]; then
    tail -n 180 "$LOG_FILE" >&2 || true
  fi
  exit 1
}

command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v node >/dev/null 2>&1 || fail "node is required"

cleanup
trap cleanup EXIT

cd "$ROOT/backend"
rm -f "$LOG_FILE" "$HEALTH_FILE" "$FEATURES_FILE" "$OPENAPI_FILE"

(
  SPRING_PROFILES_ACTIVE=dev SERVER_PORT="$PORT" mvn -q spring-boot:run >"$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
)

pid="$(cat "$PID_FILE")"

for _ in $(seq 1 "$ATTEMPTS"); do
  kill -0 "$pid" >/dev/null 2>&1 || fail "backend process exited before health check passed"

  if curl -fsS "http://localhost:$PORT/api/test/health" >"$HEALTH_FILE" 2>/dev/null; then
    curl -fsS "http://localhost:$PORT/api/test/features" >"$FEATURES_FILE"
    curl -fsS "http://localhost:$PORT/v3/api-docs" >"$OPENAPI_FILE"
    node - "$HEALTH_FILE" "$FEATURES_FILE" "$OPENAPI_FILE" <<'NODE'
const fs = require('fs')

const health = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
const features = JSON.parse(fs.readFileSync(process.argv[3], 'utf8'))
const openapi = JSON.parse(fs.readFileSync(process.argv[4], 'utf8'))

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(health.code === '0', 'health response code must be 0')
assert(health.data?.status === 'UP', 'health status must be UP')
assert(Array.isArray(health.data?.activeProfiles), 'health activeProfiles must be an array')
assert(health.data.activeProfiles.includes('dev'), 'health activeProfiles must include dev')

assert(features.code === '0', 'features response code must be 0')
assert(features.data?.status === 'ready', 'features status must be ready')
assert(features.data?.summary?.byStatus?.degraded === 0, 'features degraded count must be 0')

const database = features.data.features.find((item) => item.name === 'Database')
assert(database?.version === 'H2 (MySQL mode)', 'dev database must be H2 in MySQL mode')

const redis = features.data.features.find((item) => item.name === 'Redis')
assert(redis?.status === 'disabled', 'dev Redis must be disabled')

assert(String(openapi.openapi || '').startsWith('3.'), 'OpenAPI version must be 3.x')
assert(openapi.info?.title === 'Anjing Agent AIGC API', 'OpenAPI title mismatch')
assert(openapi.paths?.['/api/auth/login']?.post, 'OpenAPI must include POST /api/auth/login')
assert(openapi.paths?.['/api/auth/me']?.get, 'OpenAPI must include GET /api/auth/me')
assert(openapi.paths?.['/api/test/health']?.get, 'OpenAPI must include GET /api/test/health')

const loginParameters = openapi.paths['/api/auth/login'].post.parameters || []
assert(loginParameters.some((item) => item.name === 'X-Request-Id' && item.in === 'header'), 'OpenAPI must include X-Request-Id header')
assert(loginParameters.some((item) => item.name === 'X-Time-Zone' && item.in === 'header'), 'OpenAPI must include X-Time-Zone header')

const schemas = openapi.components?.schemas || {}
assert(schemas.LoginRequest, 'OpenAPI must include LoginRequest schema')
assert(schemas.AuthTokenResponse, 'OpenAPI must include AuthTokenResponse schema')
assert(schemas.CurrentUserResponse, 'OpenAPI must include CurrentUserResponse schema')
NODE
    node "$ROOT/scripts/check-openapi-runtime-contract.js" "$OPENAPI_FILE"
    node "$ROOT/scripts/generate-openapi-frontend-types.js" "$OPENAPI_FILE" --check
    echo "probe-backend-dev: ok"
    echo "probe-backend-dev: health=$HEALTH_FILE"
    echo "probe-backend-dev: features=$FEATURES_FILE"
    echo "probe-backend-dev: openapi=$OPENAPI_FILE"
    exit 0
  fi

  sleep 1
done

fail "backend did not pass health check in ${ATTEMPTS}s"

#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${AIGC_BASE_URL:-${1:-http://127.0.0.1:10003}}"
BASE_URL="${BASE_URL%/}"
PROMPT="${AIGC_SMOKE_PROMPT:-blue futuristic course cover image for an AIGC workshop}"
CONTENT_TYPE="${AIGC_SMOKE_CONTENT_TYPE:-IMAGE}"
ATTEMPTS="${AIGC_SMOKE_ATTEMPTS:-60}"
SLEEP_SECONDS="${AIGC_SMOKE_SLEEP_SECONDS:-1}"
COLLECTION_SIZE="${AIGC_SMOKE_COLLECTION_SIZE:-3}"
USER_ID="${AIGC_SMOKE_USER_ID:-demo-smoke}"
USER_NAME="${AIGC_SMOKE_USER_NAME:-Demo Smoke}"
USER_ROLES="${AIGC_SMOKE_USER_ROLES:-R_ADMIN}"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/agent-aigc-demo-smoke.XXXXXX")"

cleanup() {
  rm -rf "$TMP_DIR"
}

fail() {
  echo "aigc-demo-smoke: $*" >&2
  exit 1
}

api() {
  local method="$1"
  local path="$2"
  curl -fsS -X "$method" "$BASE_URL$path" \
    -H "X-User-Id: $USER_ID" \
    -H "X-User-Name: $USER_NAME" \
    -H "X-User-Roles: $USER_ROLES"
}

api_body() {
  local method="$1"
  local path="$2"
  local body_file="$3"
  curl -fsS -X "$method" "$BASE_URL$path" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: $USER_ID" \
    -H "X-User-Name: $USER_NAME" \
    -H "X-User-Roles: $USER_ROLES" \
    --data-binary "@$body_file"
}

download() {
  local path="$1"
  local output_file="$2"
  curl -fsS "$BASE_URL$path" \
    -H "X-User-Id: $USER_ID" \
    -H "X-User-Name: $USER_NAME" \
    -H "X-User-Roles: $USER_ROLES" \
    -o "$output_file"
}

assert_api_success() {
  local file="$1"
  local label="$2"
  node - "$file" "$label" <<'NODE'
const fs = require('fs')

const file = process.argv[2]
const label = process.argv[3]
const json = JSON.parse(fs.readFileSync(file, 'utf8'))

if (json.code !== '0') {
  throw new Error(`${label} failed: code=${json.code}, message=${json.message || ''}`)
}
NODE
}

json_get() {
  local file="$1"
  local path="$2"
  node - "$file" "$path" <<'NODE'
const fs = require('fs')

const file = process.argv[2]
const path = process.argv[3]
const json = JSON.parse(fs.readFileSync(file, 'utf8'))
const value = path.split('.').reduce((current, key) => {
  if (current === null || current === undefined) {
    return undefined
  }
  return current[key]
}, json)

if (value === undefined || value === null || value === '') {
  process.exit(2)
}

if (typeof value === 'object') {
  process.stdout.write(JSON.stringify(value))
} else {
  process.stdout.write(String(value))
}
NODE
}

trap cleanup EXIT

command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v node >/dev/null 2>&1 || fail "node is required"

cd "$ROOT"

HEALTH_FILE="$TMP_DIR/health.json"
GENERATE_BODY_FILE="$TMP_DIR/generate-body.json"
GENERATE_FILE="$TMP_DIR/generate.json"
TASK_FILE="$TMP_DIR/task.json"
SAVE_BODY_FILE="$TMP_DIR/save-body.json"
SAVE_FILE="$TMP_DIR/save.json"
SHARE_FILE="$TMP_DIR/share.json"
REUSE_FILE="$TMP_DIR/reuse.json"
DOWNLOAD_FILE="$TMP_DIR/gallery-download.bin"
REPORT_FILE="$TMP_DIR/gallery-report.json"
PROVIDER_FILE="$TMP_DIR/provider-report.json"
COLLECTIONS_FILE="$TMP_DIR/gallery-collections.json"
TOPICS_FILE="$TMP_DIR/gallery-topics.json"

api GET "/api/test/health" > "$HEALTH_FILE"
assert_api_success "$HEALTH_FILE" "health"

node - "$PROMPT" "$CONTENT_TYPE" > "$GENERATE_BODY_FILE" <<'NODE'
const [prompt, contentType] = process.argv.slice(2)

process.stdout.write(JSON.stringify({
  prompt,
  contentTypeHint: contentType,
  generationParams: {
    aspectRatio: '16:9',
    quality: 'standard',
    style: 'teaching-demo'
  },
  referenceImages: [],
  referenceMaterialIds: []
}))
NODE

api_body POST "/api/aigc/generate" "$GENERATE_BODY_FILE" > "$GENERATE_FILE"
assert_api_success "$GENERATE_FILE" "generate"

task_id="$(json_get "$GENERATE_FILE" "data.taskId")"
[[ -n "$task_id" ]] || fail "generate response did not include taskId"

for _ in $(seq 1 "$ATTEMPTS"); do
  api GET "/api/aigc/task/$task_id" > "$TASK_FILE"
  assert_api_success "$TASK_FILE" "task status"

  task_status="$(json_get "$TASK_FILE" "data.status")"
  if [[ "$task_status" == "COMPLETED" ]]; then
    break
  fi

  if [[ "$task_status" == "FAILED" ]]; then
    error_message="$(json_get "$TASK_FILE" "data.errorMessage" || true)"
    fail "task failed: ${error_message:-unknown error}"
  fi

  sleep "$SLEEP_SECONDS"
done

task_status="$(json_get "$TASK_FILE" "data.status")"
[[ "$task_status" == "COMPLETED" ]] || fail "task did not complete after ${ATTEMPTS} attempts"

asset_id="$(json_get "$TASK_FILE" "data.result.assetId")"
[[ -n "$asset_id" ]] || fail "completed task did not include result.assetId"

node - "$asset_id" > "$SAVE_BODY_FILE" <<'NODE'
const assetId = process.argv[2]
process.stdout.write(JSON.stringify({ assetId }))
NODE

api_body POST "/api/aigc/gallery/save" "$SAVE_BODY_FILE" > "$SAVE_FILE"
assert_api_success "$SAVE_FILE" "gallery save"

api GET "/api/aigc/gallery/$asset_id/share" > "$SHARE_FILE"
assert_api_success "$SHARE_FILE" "gallery share"

api POST "/api/aigc/gallery/$asset_id/share/reuse" > "$REUSE_FILE"
assert_api_success "$REUSE_FILE" "gallery share reuse"

download "/api/aigc/gallery/$asset_id/download" "$DOWNLOAD_FILE"
[[ -s "$DOWNLOAD_FILE" ]] || fail "gallery download returned an empty file"

api GET "/api/aigc/gallery/reports/interactions?days=1" > "$REPORT_FILE"
assert_api_success "$REPORT_FILE" "gallery interaction report"

api GET "/api/aigc/models/provider-execution-report?days=7&contentType=$CONTENT_TYPE" > "$PROVIDER_FILE"
assert_api_success "$PROVIDER_FILE" "provider execution report"

api GET "/api/aigc/gallery/collections?size=$COLLECTION_SIZE" > "$COLLECTIONS_FILE"
assert_api_success "$COLLECTIONS_FILE" "gallery collections"

api GET "/api/aigc/gallery/topics?size=$COLLECTION_SIZE" > "$TOPICS_FILE"
assert_api_success "$TOPICS_FILE" "gallery topics"

node - "$TASK_FILE" "$REPORT_FILE" "$PROVIDER_FILE" "$COLLECTIONS_FILE" "$TOPICS_FILE" <<'NODE'
const fs = require('fs')

const taskResponse = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
const galleryResponse = JSON.parse(fs.readFileSync(process.argv[3], 'utf8'))
const providerResponse = JSON.parse(fs.readFileSync(process.argv[4], 'utf8'))
const collectionsResponse = JSON.parse(fs.readFileSync(process.argv[5], 'utf8'))
const topicsResponse = JSON.parse(fs.readFileSync(process.argv[6], 'utf8'))

function number(value) {
  const parsed = Number(value ?? 0)
  return Number.isFinite(parsed) ? parsed : 0
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

const task = taskResponse.data || {}
const result = task.result || {}
assert(task.status === 'COMPLETED', 'task status must be COMPLETED')
assert(result.success === true, 'task result.success must be true')
assert(result.assetId, 'task result.assetId is required')

const gallery = galleryResponse.data || {}
const funnel = gallery.shareFunnel || {}
const shareViewCount = number(gallery.shareViewCount ?? funnel.shareViewCount)
const downloadCount = number(gallery.downloadCount ?? funnel.downloadCount)
const promptReuseCount = number(gallery.promptReuseCount ?? funnel.promptReuseCount)

assert(shareViewCount >= 1, 'gallery report must include at least one share-view event')
assert(downloadCount >= 1, 'gallery report must include at least one public-download event')
assert(promptReuseCount >= 1, 'gallery report must include at least one prompt-reuse event')

const provider = providerResponse.data || {}
assert(number(provider.totalTasks) >= 1, 'provider report must include generated task')
assert(number(provider.completedTasks) >= 1, 'provider report must include completed task')
assert(Array.isArray(provider.providerMetrics) && provider.providerMetrics.length > 0, 'provider metrics are required')
assert(Array.isArray(provider.modelMetrics) && provider.modelMetrics.length > 0, 'model metrics are required')

const collectionsData = collectionsResponse.data || {}
const collections = collectionsData.collections || []
assert(Array.isArray(collections), 'gallery collections must be an array')
assert(collections.length >= 1, 'gallery collections must include at least one collection')
assert(number(collectionsData.collectionSize) >= 1, 'gallery collectionSize must be positive')

const createdAssetId = result.assetId
const collectionWithCreatedAsset = collections.find((collection) => {
  return Array.isArray(collection.assets)
    && collection.assets.some((asset) => asset.id === createdAssetId)
})
assert(collectionWithCreatedAsset, 'gallery collections must include the generated published asset')
assert(collectionWithCreatedAsset.coverAsset?.id, 'gallery collection coverAsset is required')
assert(number(collectionWithCreatedAsset.itemCount) >= 1, 'gallery collection itemCount must be positive')

const topicsData = topicsResponse.data || {}
const topics = topicsData.topics || []
assert(Array.isArray(topics), 'gallery topics must be an array')
assert(topics.length >= 1, 'gallery topics must include at least one topic')
assert(number(topicsData.topicSize) >= 1, 'gallery topicSize must be positive')

const topicWithCreatedAsset = topics.find((topic) => {
  return Array.isArray(topic.assets)
    && topic.assets.some((asset) => asset.id === createdAssetId)
})
assert(topicWithCreatedAsset, 'gallery topics must include the generated published asset')
assert(topicWithCreatedAsset.coverAsset?.id, 'gallery topic coverAsset is required')
assert(topicWithCreatedAsset.operationHint, 'gallery topic operationHint is required')

console.log('aigc-demo-smoke: ok')
console.log(`aigc-demo-smoke: taskId=${task.taskId}`)
console.log(`aigc-demo-smoke: assetId=${result.assetId}`)
console.log(`aigc-demo-smoke: gallery shareView=${shareViewCount} download=${downloadCount} promptReuse=${promptReuseCount}`)
console.log(`aigc-demo-smoke: provider totalTasks=${provider.totalTasks} completedTasks=${provider.completedTasks} successRate=${provider.successRate}`)
console.log(`aigc-demo-smoke: gallery collections=${collections.length} matched=${collectionWithCreatedAsset.id}`)
console.log(`aigc-demo-smoke: gallery topics=${topics.length} matched=${topicWithCreatedAsset.id}`)
NODE

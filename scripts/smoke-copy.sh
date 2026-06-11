#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SMOKE_NAME="${1:-anjing-copy-smoke}"
TMP_ROOT="${TMPDIR:-/tmp}"
WORK_DIR="$(mktemp -d "$TMP_ROOT/infra-dev-scaffolding-copy-smoke.XXXXXX")"
COPY_DIR="$WORK_DIR/$SMOKE_NAME"

fail() {
  echo "smoke-copy: $*" >&2
  exit 1
}

command -v rsync >/dev/null 2>&1 || fail "rsync is required"
command -v node >/dev/null 2>&1 || fail "node is required"
command -v rg >/dev/null 2>&1 || fail "rg is required"

case "$SMOKE_NAME" in
  *[!a-zA-Z0-9._-]* | "" )
    fail "project name must contain only letters, numbers, dot, underscore, or dash"
    ;;
esac

mkdir -p "$COPY_DIR"

rsync -a "$ROOT/" "$COPY_DIR/" \
  --exclude '.git/' \
  --exclude 'frontend/node_modules/' \
  --exclude 'frontend/dist/' \
  --exclude 'backend/target/' \
  --exclude 'backend/logs/'

cd "$COPY_DIR"

SMOKE_NAME="$SMOKE_NAME" node <<'NODE'
const fs = require('fs')
const path = require('path')

const projectName = process.env.SMOKE_NAME
const dbName = projectName.replace(/[^a-zA-Z0-9_]/g, '_')
const previousName = JSON.parse(fs.readFileSync('frontend/package.json', 'utf8')).name
const textExtensions = new Set([
  '.css',
  '.html',
  '.java',
  '.json',
  '.md',
  '.scss',
  '.sh',
  '.ts',
  '.vue',
  '.xml',
  '.yml',
  '.yaml'
])

function read(file) {
  return fs.readFileSync(file, 'utf8')
}

function write(file, content) {
  fs.writeFileSync(file, content)
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function replaceAll(content, from, to) {
  return content.replace(new RegExp(escapeRegExp(from), 'g'), to)
}

function walk(dir, callback) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (['.git', 'node_modules', 'dist', 'target', 'logs'].includes(entry.name)) continue
      walk(fullPath, callback)
      continue
    }

    callback(fullPath)
  }
}

walk('.', (file) => {
  if (!textExtensions.has(path.extname(file))) return
  const original = read(file)
  let next = original
  next = replaceAll(next, previousName, projectName)
  next = replaceAll(next, 'infra-dev-scaffolding', projectName)
  next = replaceAll(next, 'Infra Dev Scaffolding', projectName)
  next = replaceAll(next, 'infra_dev_scaffolding', dbName)
  if (next !== original) write(file, next)
})

const pkg = JSON.parse(read('frontend/package.json'))
pkg.name = projectName
write('frontend/package.json', `${JSON.stringify(pkg, null, 2)}\n`)

let pom = read('backend/pom.xml')
pom = pom.replace(/(<parent>[\s\S]*?<\/parent>)([\s\S]*)/, (_, parent, rest) => {
  rest = rest
    .replace(/<artifactId>[^<]+<\/artifactId>/, `<artifactId>${projectName}</artifactId>`)
    .replace(/<name>[^<]+<\/name>/, `<name>${projectName}</name>`)
    .replace(/<description>[^<]+<\/description>/, `<description>Anjing copy smoke project</description>`)
  return `${parent}${rest}`
})
write('backend/pom.xml', pom)

let app = read('backend/src/main/resources/application.yml')
app = app
  .replace(/^(\s{4}name:\s*)[^\s#]+/m, `$1${projectName}`)
  .replace(/localhost:3306\/[^?]+/g, `localhost:3306/${dbName}`)
write('backend/src/main/resources/application.yml', app)

let envExample = read('backend/.env.example')
envExample = envExample.replace(/localhost:3306\/[^?]+/g, `localhost:3306/${dbName}`)
write('backend/.env.example', envExample)

let logback = read('backend/src/main/resources/logback-spring.xml')
logback = logback
  .replace(/value="\/var\/log\/[^"]+"/g, `value="/var/log/${projectName}"`)
  .replace(/(<property name="APP_NAME" value=")[^"]+(")/g, `$1${projectName}$2`)
write('backend/src/main/resources/logback-spring.xml', logback)
NODE

./scripts/check-template.sh

if rg -n 'infra-dev-scaffolding|agent-dev-scaffolding|apifoxmock|6400575|6097373|Daymychen/art-design-pro' \
  README.md CONTRIBUTING.md project_document backend frontend \
  --glob '!frontend/node_modules/**' \
  --glob '!frontend/dist/**' \
  --glob '!backend/target/**'
then
  fail "copied project still contains stale identity or mock endpoint"
fi

if rg -n 'Agent Dev Scaffolding|Art Design Pro|UnoCSS|infra-dev-scaffolding|agent-dev-scaffolding|apifoxmock|6400575|6097373|Daymychen/art-design-pro' \
  frontend/.cursor backend/.cursor project_document/AI_ASSETS.md --hidden
then
  fail "copied project still contains stale Cursor identity or deprecated frontend stack"
fi

if rg -n '\[ModuleName\]Service|\[moduleName\]Api|src/api/models|request\.delete|request\.get/post/put/delete' frontend/.cursor/prompts frontend/.cursor/rules backend/.cursor/prompts backend/.cursor/rules --hidden
then
  fail "copied project still contains deprecated Cursor rule or prompt API contract"
fi

echo "smoke-copy: ok"
echo "smoke-copy: copied project at $COPY_DIR"

#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')

function fail(message) {
  console.error(`check-scaffold-governance: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function requireToken(relativeFile, token) {
  const source = read(relativeFile)
  if (!source.includes(token)) {
    fail(`${relativeFile} is missing token: ${token}`)
  }
}

function requireAbsent(relativeFile, pattern, description) {
  const source = read(relativeFile)
  if (pattern.test(source)) {
    fail(`${relativeFile} contains ${description}: ${pattern}`)
  }
}

function walk(dir, files = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === 'dist') continue
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(fullPath, files)
    } else if (/\.(ts|vue)$/.test(entry.name)) {
      files.push(fullPath)
    }
  }
  return files
}

for (const file of [
  'project_document/ci/quality-gate.yml',
  'project_document/NEW_MODULE_GUIDE.md',
  'project_document/SCAFFOLD_ADOPTION_PROMPT.md',
  'project_document/UI_DESIGN_GUIDE.md',
  'project_document/DEMO_EVIDENCE.md'
]) {
  read(file)
}

for (const token of [
  'actions/setup-java@v4',
  'java-version: \'17\'',
  'actions/setup-node@v4',
  'node-version: \'20.19.0\'',
  'pnpm/action-setup@v4',
  './scripts/quality-gate.sh'
]) {
  requireToken('project_document/ci/quality-gate.yml', token)
}

for (const token of [
  'project_document/PROJECT_CONSTRAINTS.md',
  'project_document/NEW_MODULE_GUIDE.md',
  'project_document/SCAFFOLD_ADOPTION_PROMPT.md',
  'project_document/UI_DESIGN_GUIDE.md',
  'project_document/DEMO_EVIDENCE.md',
  './scripts/quality-gate.sh'
]) {
  requireToken('README.md', token)
}

for (const token of [
  'project_document/PROJECT_CONSTRAINTS.md',
  'project_document/NEW_MODULE_GUIDE.md',
  'project_document/UI_DESIGN_GUIDE.md',
  'project_document/DEMO_EVIDENCE.md',
  './scripts/quality-gate.sh'
]) {
  requireToken('CONTRIBUTING.md', token)
}

for (const token of [
  'NEW_MODULE_GUIDE.md',
  'SCAFFOLD_ADOPTION_PROMPT.md',
  'UI_DESIGN_GUIDE.md',
  'DEMO_EVIDENCE.md',
  'ci/quality-gate.yml'
]) {
  requireToken('project_document/README.md', token)
}

for (const token of [
  'APIResponse<T>',
  'PageResult<T>',
  'openApiRequest(operationId)',
  '@ScaffoldSample',
  './scripts/quality-gate.sh'
]) {
  requireToken('project_document/NEW_MODULE_GUIDE.md', token)
}

for (const token of [
  'https://github.com/anjing-le/',
  'project_document/PROJECT_CONSTRAINTS.md',
  'project_document/NEW_MODULE_GUIDE.md',
  'project_document/UI_DESIGN_GUIDE.md',
  '先审计当前项目结构',
  '不要为了套模板而重写业务'
]) {
  requireToken('project_document/SCAFFOLD_ADOPTION_PROMPT.md', token)
}

for (const token of [
  'Less is more',
  '--default-glass-surface',
  '--default-glass-filter',
  '--default-border-dashed',
  'border-dashed',
  'hover'
]) {
  requireToken('project_document/UI_DESIGN_GUIDE.md', token)
}

for (const token of [
  'docs/evidence/YYYY-MM-DD/',
  'login-desktop.png',
  'backend-probe.txt',
  './scripts/probe-backend-dev.sh'
]) {
  requireToken('project_document/DEMO_EVIDENCE.md', token)
}

for (const token of [
  '--default-glass-surface',
  '--default-glass-strong-surface',
  '--default-glass-subtle-surface',
  '--default-glass-filter',
  '--default-border-dashed',
  '.glass-panel',
  '.glass-control',
  '.line-dashed'
]) {
  requireToken('frontend/src/assets/styles/core/tailwind.css', token)
}

for (const token of [
  'backdrop-filter: var(--default-glass-filter)',
  'border: 1px dashed var(--default-glass-border)',
  'background: var(--default-glass-surface)'
]) {
  requireToken('frontend/src/views/auth/login/style.css', token)
}

requireAbsent('frontend/src/api/paths.ts', /currentUser:\s*['"`]/, 'unused legacy currentUser path')

const allowedLegacyImportFiles = new Set([
  'frontend/src/api/paths.ts',
  'frontend/src/api/user.ts',
  'frontend/src/api/system-manage.ts'
])

for (const file of walk(path.join(root, 'frontend/src'))) {
  const relativeFile = path.relative(root, file)
  const source = fs.readFileSync(file, 'utf8')
  if (source.includes('ApiLegacyPaths') && !allowedLegacyImportFiles.has(relativeFile)) {
    fail(`ApiLegacyPaths must stay in compatibility API modules, found in ${relativeFile}`)
  }
}

console.log('check-scaffold-governance: ok')

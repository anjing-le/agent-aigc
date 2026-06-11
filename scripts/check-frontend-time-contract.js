#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')

const allowedFiles = new Set([
  'frontend/src/utils/time/index.ts',
  'frontend/src/utils/locale/index.ts'
])

const requiredTokens = [
  'formatDateTime',
  'formatDate',
  'formatTime',
  'formatDateKey',
  'formatFilenameTimestamp',
  'nowIsoString',
  'getClientTimeZone',
  'getClientLocale',
  'DEFAULT_TIME_ZONE'
]

const disallowedPatterns = [
  [/\btoLocale(DateString|TimeString)\s*\(/, 'direct locale date/time formatting'],
  [/\bnew Date\([^)]*\)\.toLocaleString\s*\(/, 'direct locale date/time formatting'],
  [/\bIntl\.DateTimeFormat\s*\(/, 'direct Intl.DateTimeFormat usage'],
  [/\bnavigator\.language\b/, 'direct navigator language usage'],
  [/\buseDateFormat\s*\(/, 'direct useDateFormat usage'],
  [/\b(dayjs|moment)\s*\(/, 'direct dayjs/moment formatting'],
  [/\bnew Date\(\)\.toISOString\s*\(/, 'direct current ISO timestamp formatting']
]

const searchRoots = [
  'frontend/src',
  'frontend/.cursor',
  'project_document',
  'README.md'
]

function fail(message) {
  console.error(`check-frontend-time-contract: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function walk(relativePath, visitor) {
  const fullPath = path.join(root, relativePath)
  if (!fs.existsSync(fullPath)) {
    return
  }

  const stat = fs.statSync(fullPath)
  if (stat.isFile()) {
    visitor(relativePath)
    return
  }

  for (const entry of fs.readdirSync(fullPath, { withFileTypes: true })) {
    if (entry.isDirectory() && ['node_modules', 'dist', 'target', 'logs'].includes(entry.name)) {
      continue
    }

    walk(path.join(relativePath, entry.name), visitor)
  }
}

const timeSource = read('frontend/src/utils/time/index.ts')
for (const token of requiredTokens) {
  if (!timeSource.includes(token)) {
    fail(`frontend/src/utils/time/index.ts is missing token: ${token}`)
  }
}

const checkedExtensions = new Set(['.ts', '.vue', '.md', '.mdc'])
for (const rootPath of searchRoots) {
  walk(rootPath, (relativeFile) => {
    if (!checkedExtensions.has(path.extname(relativeFile))) return
    if (allowedFiles.has(relativeFile)) return

    const source = read(relativeFile)
    for (const [pattern, description] of disallowedPatterns) {
      const match = source.match(pattern)
      if (match) {
        fail(`${relativeFile} contains ${description}: ${match[0]}`)
      }
    }
  })
}

console.log('check-frontend-time-contract: ok')

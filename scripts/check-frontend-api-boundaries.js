#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const apiPathsFile = 'frontend/src/api/paths.ts'
const boundaryFile = 'contracts/service-boundaries.json'

function fail(message) {
  console.error(`check-frontend-api-boundaries: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function readJson(relativeFile) {
  try {
    return JSON.parse(read(relativeFile))
  } catch (error) {
    fail(`invalid ${relativeFile}: ${error.message}`)
  }
}

function findBlock(source, marker) {
  const markerIndex = source.indexOf(marker)
  if (markerIndex < 0) {
    fail(`missing block marker: ${marker}`)
  }

  const start = source.indexOf('{', markerIndex)
  if (start < 0) {
    fail(`missing block opening brace: ${marker}`)
  }

  let depth = 0
  for (let index = start; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start + 1, index)
      }
    }
  }

  fail(`missing block closing brace: ${marker}`)
}

function buildFrontendRoutePaths(manifest) {
  const values = {}
  for (const boundary of manifest.boundaries || []) {
    if (!boundary.apiPathsKey) continue
    values[boundary.apiPathsKey] = values[boundary.apiPathsKey] || {}
    for (const route of boundary.routes || []) {
      if (route.frontendKey) {
        values[boundary.apiPathsKey][route.frontendKey] = route.path
      }
    }
  }
  return values
}

function extractTsModule(source, objectName, moduleName, routePaths) {
  const objectBlock = findBlock(source, `export const ${objectName}`)
  const moduleBlock = findBlock(objectBlock, `${moduleName}:`)
  const values = {}

  for (const match of moduleBlock.matchAll(/(\w+):\s*'([^']+)'/g)) {
    values[match[1]] = match[2]
  }

  for (const match of moduleBlock.matchAll(/(\w+):\s*SERVICE_BOUNDARY_ROUTE_PATHS\.(\w+)\.(\w+)/g)) {
    values[match[1]] = routePaths[match[2]]?.[match[3]]
  }

  for (const match of moduleBlock.matchAll(/(\w+):\s*\([^)]*\)\s*=>\s*`([^`]+)`/g)) {
    values[match[1]] = match[2].replace(/\$\{encodePathValue\((\w+)\)\}/g, '{$1}')
  }

  for (const match of moduleBlock.matchAll(/(\w+):\s*\([^)]*\)\s*=>\s*bindApiPathParams\(SERVICE_BOUNDARY_ROUTE_PATHS\.(\w+)\.(\w+),/g)) {
    values[match[1]] = routePaths[match[2]]?.[match[3]]
  }

  return values
}

const source = read(apiPathsFile)
const manifest = readJson(boundaryFile)
const routePaths = buildFrontendRoutePaths(manifest)

if (!source.includes('export const ApiLegacyPaths')) {
  fail('frontend/src/api/paths.ts must export ApiLegacyPaths for old template endpoints')
}

const declaredRoutes = new Map()
for (const boundary of manifest.boundaries || []) {
  if (!boundary.apiPathsKey) {
    continue
  }

  for (const route of boundary.routes || []) {
    if (route.frontendKey) {
      declaredRoutes.set(`${boundary.apiPathsKey}.${route.frontendKey}`, route.path)
    }
  }
}

if (!declaredRoutes.size) {
  fail('contracts/service-boundaries.json must define frontend route mappings')
}

const apiPathsBlock = findBlock(source, 'export const ApiPaths')
if (!apiPathsBlock.includes('SERVICE_BOUNDARY_ROUTE_PATHS')) {
  fail('ApiPaths must reference SERVICE_BOUNDARY_ROUTE_PATHS from frontend service-boundary contract')
}
if (/['"`]\/api\//.test(apiPathsBlock)) {
  fail('ApiPaths must use SERVICE_BOUNDARY_ROUTE_PATHS instead of direct /api/... literals')
}

for (const match of apiPathsBlock.matchAll(/([a-zA-Z]\w*):\s*{/g)) {
  const moduleName = match[1]
  const values = extractTsModule(source, 'ApiPaths', moduleName, routePaths)

  for (const [key, value] of Object.entries(values)) {
    const routeKey = `${moduleName}.${key}`
    const expected = declaredRoutes.get(routeKey)
    if (!expected) {
      fail(`ApiPaths.${routeKey} is not declared in contracts/service-boundaries.json`)
    }

    if (expected !== value) {
      fail(`ApiPaths.${routeKey} (${value}) does not match service-boundaries route ${expected}`)
    }
  }
}

for (const legacyToken of [
  'verify2FA',
  'sendOtp',
  'tenantMembers',
  'userInfo',
  'avatarUpload',
  'simpleMenus'
]) {
  if (apiPathsBlock.includes(legacyToken)) {
    fail(`ApiPaths must not contain legacy endpoint key: ${legacyToken}`)
  }
}

const legacyBlock = findBlock(source, 'export const ApiLegacyPaths')
for (const legacyPath of [
  '/auth/login/verify-2fa',
  '/auth/otp/send',
  '/auth/tenant/account/list',
  '/auth/user/info',
  '/api/v3/system/menus/simple'
]) {
  if (!legacyBlock.includes(legacyPath)) {
    fail(`ApiLegacyPaths is missing legacy path: ${legacyPath}`)
  }
}

console.log('check-frontend-api-boundaries: ok')

#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const boundaryPath = path.join(root, 'contracts/service-boundaries.json')
const platformPath = path.join(root, 'contracts/platform-contract.json')
const apiConstantsPath = path.join(root, 'backend/src/main/java/com/anjing/model/constants/ApiConstants.java')
const apiPathsPath = path.join(root, 'frontend/src/api/paths.ts')
const frontendPackagePath = path.join(root, 'frontend/package.json')

const requiredDocs = [
  'project_document/SERVICE_BOUNDARY_GUIDE.md',
  'project_document/API_PATH_GUIDE.md',
  'project_document/ARCHITECTURE_EVOLUTION.md',
  'project_document/STATUS.md'
]

function fail(message) {
  console.error(`check-service-boundaries: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function readJson(file, label) {
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${label}`)
  }

  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch (error) {
    fail(`invalid ${label}: ${error.message}`)
  }
}

function requireToken(relativeFile, token) {
  const source = read(relativeFile)
  if (!source.includes(token)) {
    fail(`${relativeFile} is missing token: ${token}`)
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

function evaluateJavaExpression(expression, constants) {
  return expression
    .split('+')
    .map((part) => part.trim())
    .map((part) => {
      const literalMatch = part.match(/^"([^"]*)"$/)
      if (literalMatch) {
        return literalMatch[1]
      }

      if (part === 'PlatformContractConstants.API_PREFIX') {
        return '/api'
      }

      if (Object.prototype.hasOwnProperty.call(constants, part)) {
        return constants[part]
      }

      if (/^\w+\.\w+$/.test(part)) {
        return `__QUALIFIED_REF__${part}`
      }

      fail(`cannot evaluate Java API path expression: ${expression}`)
    })
    .join('')
}

function extractJavaConstants(source, className) {
  const globalConstants = {}
  for (const match of source.matchAll(/public static final String\s+(\w+)\s*=\s*([^;]+);/g)) {
    if (match.index < source.indexOf('public static class')) {
      globalConstants[match[1]] = evaluateJavaExpression(match[2], globalConstants)
    }
  }

  const block = findBlock(source, `public static class ${className}`)
  const constants = { ...globalConstants }

  for (const match of block.matchAll(/public static final String\s+(\w+)\s*=\s*([^;]+);/g)) {
    constants[match[1]] = evaluateJavaExpression(match[2], constants)
  }

  return constants
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

function extractTsModule(source, moduleName, routePaths) {
  const block = findBlock(source, `${moduleName}:`)
  const values = {}

  for (const match of block.matchAll(/(\w+):\s*'([^']+)'/g)) {
    values[match[1]] = match[2]
  }

  for (const match of block.matchAll(/(\w+):\s*SERVICE_BOUNDARY_ROUTE_PATHS\.(\w+)\.(\w+)/g)) {
    values[match[1]] = routePaths[match[2]]?.[match[3]]
  }

  for (const match of block.matchAll(/(\w+):\s*\([^)]*\)\s*=>\s*`([^`]+)`/g)) {
    values[match[1]] = match[2].replace(/\$\{encodePathValue\((\w+)\)\}/g, '{$1}')
  }

  for (const match of block.matchAll(/(\w+):\s*\([^)]*\)\s*=>\s*bindApiPathParams\(SERVICE_BOUNDARY_ROUTE_PATHS\.(\w+)\.(\w+),/g)) {
    values[match[1]] = routePaths[match[2]]?.[match[3]]
  }

  return values
}

function ensureKebabId(id) {
  if (!/^[a-z][a-z0-9-]*$/.test(id)) {
    fail(`boundary id must be kebab-case: ${id}`)
  }
}

function ensureRoutePath(route, boundary) {
  if (!route.path.startsWith(boundary.basePath)) {
    fail(`${boundary.id}.${route.name} path ${route.path} must start with ${boundary.basePath}`)
  }

  if (!Array.isArray(route.methods) || route.methods.length === 0) {
    fail(`${boundary.id}.${route.name} must define HTTP methods`)
  }

  for (const method of route.methods) {
    if (!/^(GET|POST|PUT|PATCH|DELETE)$/.test(method)) {
      fail(`${boundary.id}.${route.name} has invalid method ${method}`)
    }
  }
}

const platform = readJson(platformPath, 'contracts/platform-contract.json')
const manifest = readJson(boundaryPath, 'contracts/service-boundaries.json')
const frontendPackage = readJson(frontendPackagePath, 'frontend/package.json')
const apiConstants = fs.readFileSync(apiConstantsPath, 'utf8')
const apiPaths = fs.readFileSync(apiPathsPath, 'utf8')
const frontendRoutePaths = buildFrontendRoutePaths(manifest)

if (manifest.schemaVersion !== 1) {
  fail('schemaVersion must be 1')
}

if (manifest.applicationId !== frontendPackage.name) {
  fail(`applicationId ${manifest.applicationId} must match frontend package name ${frontendPackage.name}`)
}

if (manifest.apiPrefix !== platform.apiPrefix) {
  fail(`apiPrefix ${manifest.apiPrefix} must match platform contract ${platform.apiPrefix}`)
}

const boundaries = manifest.boundaries || []
if (!boundaries.length) {
  fail('boundaries must not be empty')
}

const seenIds = new Set()
const seenBasePaths = new Set()
const seenRoutes = new Set()

for (const boundary of boundaries) {
  ensureKebabId(boundary.id)

  if (seenIds.has(boundary.id)) {
    fail(`duplicate boundary id: ${boundary.id}`)
  }
  seenIds.add(boundary.id)

  if (!boundary.basePath || !boundary.basePath.startsWith(manifest.apiPrefix + '/')) {
    fail(`${boundary.id} basePath must start with ${manifest.apiPrefix}/`)
  }

  if (seenBasePaths.has(boundary.basePath)) {
    fail(`duplicate boundary basePath: ${boundary.basePath}`)
  }
  seenBasePaths.add(boundary.basePath)

  if (!boundary.owner || !boundary.currentHost || !boundary.kind || !boundary.copyAction) {
    fail(`${boundary.id} must define owner, currentHost, kind, and copyAction`)
  }

  const constants = extractJavaConstants(apiConstants, boundary.apiConstantsClass)
  if (constants.BASE !== boundary.basePath) {
    fail(`ApiConstants.${boundary.apiConstantsClass}.BASE (${constants.BASE}) does not match ${boundary.id}.basePath (${boundary.basePath})`)
  }

  const frontend = boundary.apiPathsKey
    ? extractTsModule(apiPaths, boundary.apiPathsKey, frontendRoutePaths)
    : {}

  for (const route of boundary.routes || []) {
    ensureRoutePath(route, boundary)

    const routeKey = `${route.path}:${route.methods.slice().sort().join(',')}`
    if (seenRoutes.has(routeKey)) {
      fail(`duplicate route contract: ${routeKey}`)
    }
    seenRoutes.add(routeKey)

    if (!constants[route.backendConstant]) {
      fail(`${boundary.id}.${route.name} missing ApiConstants.${boundary.apiConstantsClass}.${route.backendConstant}`)
    }

    if (constants[route.backendConstant] !== route.path) {
      fail(`ApiConstants.${boundary.apiConstantsClass}.${route.backendConstant} (${constants[route.backendConstant]}) does not match ${route.path}`)
    }

    if (route.frontendKey) {
      if (!boundary.apiPathsKey) {
        fail(`${boundary.id}.${route.name} defines frontendKey without apiPathsKey`)
      }

      if (!frontend[route.frontendKey]) {
        fail(`${boundary.id}.${route.name} missing ApiPaths.${boundary.apiPathsKey}.${route.frontendKey}`)
      }

      if (frontend[route.frontendKey] !== route.path) {
        fail(`ApiPaths.${boundary.apiPathsKey}.${route.frontendKey} (${frontend[route.frontendKey]}) does not match ${route.path}`)
      }
    }
  }

  if (boundary.controller) {
    const controller = read(boundary.controller)
    if (!controller.includes(`@RequestMapping(ApiConstants.${boundary.apiConstantsClass}.BASE)`)) {
      fail(`${boundary.controller} must use ApiConstants.${boundary.apiConstantsClass}.BASE`)
    }

    if (boundary.openapi && !controller.includes('@Tag(')) {
      fail(`${boundary.controller} must define an OpenAPI @Tag`)
    }
  }
}

for (const requiredId of ['auth', 'test', 'common']) {
  if (!seenIds.has(requiredId)) {
    fail(`missing required boundary: ${requiredId}`)
  }
}

for (const doc of requiredDocs) {
  requireToken(doc, 'service-boundaries.json')
  requireToken(doc, 'check-service-boundaries.js')
}

console.log('check-service-boundaries: ok')

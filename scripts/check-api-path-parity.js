#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const apiConstantsPath = path.join(
  root,
  'backend/src/main/java/com/anjing/model/constants/ApiConstants.java'
)
const apiPathsPath = path.join(root, 'frontend/src/api/paths.ts')
const serviceBoundaryPath = path.join(root, 'contracts/service-boundaries.json')

function fail(message) {
  console.error(`check-api-path-parity: ${message}`)
  process.exit(1)
}

function read(file) {
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${path.relative(root, file)}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function readJson(file) {
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${path.relative(root, file)}`)
  }

  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch (error) {
    fail(`invalid ${path.relative(root, file)}: ${error.message}`)
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

const javaSource = read(apiConstantsPath)
const tsSource = read(apiPathsPath)
const serviceBoundaries = readJson(serviceBoundaryPath)
const frontendRoutePaths = buildFrontendRoutePaths(serviceBoundaries)

const routeMappings = []
const backendClasses = new Set()
const frontendModules = new Set()

for (const boundary of serviceBoundaries.boundaries || []) {
  if (!boundary.apiConstantsClass) {
    continue
  }

  backendClasses.add(boundary.apiConstantsClass)

  if (boundary.apiPathsKey) {
    frontendModules.add(boundary.apiPathsKey)
  }

  for (const route of boundary.routes || []) {
    if (route.backendConstant && route.frontendKey && boundary.apiPathsKey) {
      routeMappings.push([
        `${boundary.apiConstantsClass}.${route.backendConstant}`,
        `${boundary.apiPathsKey}.${route.frontendKey}`
      ])
    }
  }
}

if (!routeMappings.length) {
  fail('contracts/service-boundaries.json does not define any backend/frontend route mappings')
}

const backend = Object.fromEntries(
  [...backendClasses].map((className) => [className, extractJavaConstants(javaSource, className)])
)

const frontend = Object.fromEntries(
  [...frontendModules].map((moduleName) => [
    moduleName,
    extractTsModule(tsSource, moduleName, frontendRoutePaths)
  ])
)

function getBackendValue(key) {
  const [moduleName, constantName] = key.split('.')
  return backend[moduleName]?.[constantName]
}

function getFrontendValue(key) {
  const [moduleName, pathName] = key.split('.')
  return frontend[moduleName]?.[pathName]
}

for (const [backendKey, frontendKey] of routeMappings) {
  const backendValue = getBackendValue(backendKey)
  const frontendValue = getFrontendValue(frontendKey)

  if (!backendValue) {
    fail(`missing backend path ${backendKey}`)
  }

  if (!frontendValue) {
    fail(`missing frontend path ${frontendKey}`)
  }

  if (backendValue !== frontendValue) {
    fail(`${backendKey} (${backendValue}) does not match ${frontendKey} (${frontendValue})`)
  }
}

console.log('check-api-path-parity: ok')

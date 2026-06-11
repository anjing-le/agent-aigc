#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const openapiPath = process.argv[2]
const serviceBoundaryPath = path.join(root, 'contracts/service-boundaries.json')
const platformContractPath = path.join(root, 'contracts/platform-contract.json')

function fail(message) {
  console.error(`check-openapi-runtime-contract: ${message}`)
  process.exit(1)
}

function readJson(file, label) {
  if (!file || !fs.existsSync(file)) {
    fail(`missing required file: ${label}`)
  }

  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch (error) {
    fail(`invalid ${label}: ${error.message}`)
  }
}

function operationFor(openapi, routePath, method) {
  const pathItem = openapi.paths?.[routePath]
  if (!pathItem) {
    return null
  }
  return pathItem[method.toLowerCase()] || null
}

function operationParameters(operation) {
  return Array.isArray(operation?.parameters) ? operation.parameters : []
}

function ensureOperationHeaders(operation, routePath, method, headerNames) {
  const parameters = operationParameters(operation)
  for (const headerName of headerNames) {
    const hasHeader = parameters.some((parameter) => (
      parameter?.in === 'header' && parameter?.name === headerName
    ))
    if (!hasHeader) {
      fail(`${method} ${routePath} is missing OpenAPI header parameter ${headerName}`)
    }
  }
}

function expectedRuntimeRoutes(boundaries) {
  const routes = []
  for (const boundary of boundaries) {
    if (!boundary.openapi) continue
    for (const route of boundary.routes || []) {
      for (const method of route.methods || []) {
        routes.push({
          boundaryId: boundary.id,
          path: route.path,
          method
        })
      }
    }
  }
  return routes
}

function allowedOpenApiBasePaths(boundaries) {
  return boundaries
    .filter((boundary) => boundary.openapi)
    .map((boundary) => boundary.basePath)
}

function isAllowedOpenApiPath(routePath, basePaths) {
  return basePaths.some((basePath) => routePath === basePath || routePath.startsWith(`${basePath}/`))
}

if (!openapiPath) {
  fail('usage: node scripts/check-openapi-runtime-contract.js <openapi-json-file>')
}

const openapi = readJson(openapiPath, openapiPath)
const serviceBoundaries = readJson(serviceBoundaryPath, 'contracts/service-boundaries.json')
const platformContract = readJson(platformContractPath, 'contracts/platform-contract.json')

if (!String(openapi.openapi || '').startsWith('3.')) {
  fail('OpenAPI version must be 3.x')
}

if (!openapi.paths || typeof openapi.paths !== 'object') {
  fail('OpenAPI paths must be an object')
}

const boundaries = serviceBoundaries.boundaries || []
const apiPrefix = platformContract.apiPrefix
const headerNames = Object.values(platformContract.requestHeaders || {})

if (!apiPrefix) {
  fail('platform contract must define apiPrefix')
}

if (headerNames.length === 0) {
  fail('platform contract must define requestHeaders')
}

const routes = expectedRuntimeRoutes(boundaries)
if (routes.length === 0) {
  fail('service-boundaries.json must define openapi runtime routes')
}

for (const route of routes) {
  const operation = operationFor(openapi, route.path, route.method)
  if (!operation) {
    fail(`${route.boundaryId}.${route.method} ${route.path} is missing from OpenAPI paths`)
  }
  ensureOperationHeaders(operation, route.path, route.method, headerNames)
}

const allowedBasePaths = allowedOpenApiBasePaths(boundaries)
for (const routePath of Object.keys(openapi.paths)) {
  if (!routePath.startsWith(apiPrefix)) continue
  if (!isAllowedOpenApiPath(routePath, allowedBasePaths)) {
    fail(`OpenAPI path ${routePath} is not declared as an openapi service boundary`)
  }
}

console.log('check-openapi-runtime-contract: ok')

#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const boundaryPath = path.join(root, 'contracts/service-boundaries.json')
const outputPath = path.join(root, 'frontend/src/contracts/service-boundaries.ts')
const checkMode = process.argv.includes('--check')

function fail(message) {
  console.error(`generate-service-boundaries-frontend: ${message}`)
  process.exit(1)
}

function sortedObject(value) {
  return Object.fromEntries(Object.entries(value).sort(([left], [right]) => left.localeCompare(right)))
}

function generateContent(manifest) {
  const basePaths = {}
  const routePaths = {}

  for (const boundary of manifest.boundaries || []) {
    const key = boundary.apiPathsKey || boundary.id
    basePaths[key] = boundary.basePath

    if (boundary.apiPathsKey) {
      routePaths[boundary.apiPathsKey] = routePaths[boundary.apiPathsKey] || {}
      for (const route of boundary.routes || []) {
        if (route.frontendKey) {
          routePaths[boundary.apiPathsKey][route.frontendKey] = route.path
        }
      }
      routePaths[boundary.apiPathsKey] = sortedObject(routePaths[boundary.apiPathsKey])
    }
  }

  const serialized = JSON.stringify(manifest, null, 2)
  const serializedBasePaths = JSON.stringify(sortedObject(basePaths), null, 2)
  const serializedRoutePaths = JSON.stringify(sortedObject(routePaths), null, 2)

  return `/* eslint-disable */
// Generated from contracts/service-boundaries.json. Do not edit manually.
// Run: node scripts/generate-service-boundaries-frontend.js

export const SERVICE_BOUNDARY_CONTRACT = ${serialized} as const

export const APPLICATION_ID = SERVICE_BOUNDARY_CONTRACT.applicationId
export const SERVICE_BOUNDARY_BASE_PATHS = ${serializedBasePaths} as const
export const SERVICE_BOUNDARY_ROUTE_PATHS = ${serializedRoutePaths} as const

export type ServiceBoundaryContract = typeof SERVICE_BOUNDARY_CONTRACT
export type ServiceBoundaryId = ServiceBoundaryContract['boundaries'][number]['id']
export type ServiceBoundaryPathKey = keyof typeof SERVICE_BOUNDARY_BASE_PATHS
`
}

if (!fs.existsSync(boundaryPath)) {
  fail('missing contracts/service-boundaries.json')
}

let manifest
try {
  manifest = JSON.parse(fs.readFileSync(boundaryPath, 'utf8'))
} catch (error) {
  fail(`invalid contracts/service-boundaries.json: ${error.message}`)
}

const content = generateContent(manifest)

if (checkMode) {
  if (!fs.existsSync(outputPath)) {
    fail('missing frontend/src/contracts/service-boundaries.ts')
  }

  const current = fs.readFileSync(outputPath, 'utf8')
  if (current !== content) {
    fail('frontend/src/contracts/service-boundaries.ts is out of date')
  }

  console.log('generate-service-boundaries-frontend: ok')
  process.exit(0)
}

fs.mkdirSync(path.dirname(outputPath), { recursive: true })
fs.writeFileSync(outputPath, content)
console.log('generate-service-boundaries-frontend: wrote frontend/src/contracts/service-boundaries.ts')

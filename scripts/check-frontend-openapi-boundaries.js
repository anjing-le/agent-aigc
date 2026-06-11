#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const frontendSrc = path.join(root, 'frontend/src')

const allowedOpenApiImportPrefixes = [
  'frontend/src/contracts/openapi/',
  'frontend/src/api/openapiClient.ts',
  'frontend/src/api/model/'
]

function fail(message) {
  console.error(`check-frontend-openapi-boundaries: ${message}`)
  process.exit(1)
}

function walk(directory) {
  const entries = fs.readdirSync(directory, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    if (entry.name === 'node_modules' || entry.name === 'dist') continue
    const fullPath = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      files.push(...walk(fullPath))
    } else if (/\.(ts|vue)$/.test(entry.name)) {
      files.push(fullPath)
    }
  }
  return files
}

function relative(file) {
  return path.relative(root, file).replace(/\\/g, '/')
}

function isAllowedOpenApiConsumer(relativeFile) {
  return allowedOpenApiImportPrefixes.some((prefix) => relativeFile.startsWith(prefix))
}

for (const file of walk(frontendSrc)) {
  const relativeFile = relative(file)
  const source = fs.readFileSync(file, 'utf8')
  const importsOpenApiContract =
    source.includes("@/contracts/openapi/") ||
    source.includes('from "@/contracts/openapi/') ||
    source.includes("from '../contracts/openapi/") ||
    source.includes('from "../contracts/openapi/') ||
    source.includes("from './contracts/openapi/") ||
    source.includes('from "./contracts/openapi/')

  if (importsOpenApiContract && !isAllowedOpenApiConsumer(relativeFile)) {
    fail(`${relativeFile} imports generated OpenAPI contracts directly`)
  }
}

console.log('check-frontend-openapi-boundaries: ok')

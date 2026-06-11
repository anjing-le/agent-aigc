#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const boundaryPath = path.join(root, 'contracts/service-boundaries.json')
const outputPath = path.join(root, 'backend/src/main/java/com/anjing/model/constants/ServiceBoundaryConstants.java')
const checkMode = process.argv.includes('--check')

function fail(message) {
  console.error(`generate-service-boundaries-backend: ${message}`)
  process.exit(1)
}

function javaString(value) {
  return `"${String(value ?? '').replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`
}

function javaArray(values) {
  if (!values.length) return '{}'
  return `{ ${values.map(javaString).join(', ')} }`
}

function className(boundary) {
  return boundary.apiConstantsClass || boundary.id
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')
}

function generateBoundaryClass(boundary) {
  const routes = (boundary.routes || []).map((route) => route.name)
  return `    public static final class ${className(boundary)} {
        public static final String ID = ${javaString(boundary.id)};
        public static final String NAME = ${javaString(boundary.name)};
        public static final String KIND = ${javaString(boundary.kind)};
        public static final String OWNER = ${javaString(boundary.owner)};
        public static final String CURRENT_HOST = ${javaString(boundary.currentHost)};
        public static final String BASE_PATH = ${javaString(boundary.basePath)};
        public static final String API_CONSTANTS_CLASS = ${javaString(boundary.apiConstantsClass)};
        public static final String API_PATHS_KEY = ${javaString(boundary.apiPathsKey)};
        public static final boolean OPENAPI = ${Boolean(boundary.openapi)};
        public static final String COPY_ACTION = ${javaString(boundary.copyAction)};
        public static final String[] ROUTES = ${javaArray(routes)};

        private ${className(boundary)}() {
        }
    }`
}

function generateContent(manifest) {
  const boundaryIds = (manifest.boundaries || []).map((boundary) => boundary.id)
  const boundaryClasses = (manifest.boundaries || []).map(generateBoundaryClass).join('\n\n')

  return `package com.anjing.model.constants;

/**
 * Generated from contracts/service-boundaries.json. Do not edit manually.
 * Run: node scripts/generate-service-boundaries-backend.js
 */
public final class ServiceBoundaryConstants {

    public static final int SCHEMA_VERSION = ${manifest.schemaVersion};
    public static final String APPLICATION_ID = ${javaString(manifest.applicationId)};
    public static final String API_PREFIX = ${javaString(manifest.apiPrefix)};
    public static final String[] BOUNDARY_IDS = ${javaArray(boundaryIds)};

    private ServiceBoundaryConstants() {
    }

${boundaryClasses}
}
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
    fail('missing backend/src/main/java/com/anjing/model/constants/ServiceBoundaryConstants.java')
  }

  const current = fs.readFileSync(outputPath, 'utf8')
  if (current !== content) {
    fail('backend/src/main/java/com/anjing/model/constants/ServiceBoundaryConstants.java is out of date')
  }

  console.log('generate-service-boundaries-backend: ok')
  process.exit(0)
}

fs.mkdirSync(path.dirname(outputPath), { recursive: true })
fs.writeFileSync(outputPath, content)
console.log('generate-service-boundaries-backend: wrote backend/src/main/java/com/anjing/model/constants/ServiceBoundaryConstants.java')

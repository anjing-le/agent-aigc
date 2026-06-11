#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const apiConstantsPath = path.join(
  root,
  'backend/src/main/java/com/anjing/model/constants/ApiConstants.java'
)
const platformConstantsPath = path.join(
  root,
  'backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java'
)

function fail(message) {
  console.error(`check-api-constants: ${message}`)
  process.exit(1)
}

if (!fs.existsSync(apiConstantsPath)) {
  fail('missing backend/src/main/java/com/anjing/model/constants/ApiConstants.java')
}

if (!fs.existsSync(platformConstantsPath)) {
  fail('missing backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java')
}

const source = fs.readFileSync(apiConstantsPath, 'utf8')
const platformSource = fs.readFileSync(platformConstantsPath, 'utf8')
const lines = source.split(/\r?\n/)

const apiPrefixLine = lines.find((line) =>
  /public static final String\s+API_PREFIX\s*=\s*PlatformContractConstants\.API_PREFIX;/.test(line)
)

if (!apiPrefixLine) {
  fail('ApiConstants must define API_PREFIX from PlatformContractConstants.API_PREFIX')
}

if (!/public static final String\s+API_PREFIX\s*=\s*"\/api";/.test(platformSource)) {
  fail('PlatformContractConstants must define API_PREFIX = "/api"')
}

for (let index = 0; index < lines.length; index += 1) {
  const line = lines[index]
  if (line.includes('"/api') && !line.includes('API_PREFIX')) {
    fail(`line ${index + 1} hardcodes an API path; use API_PREFIX, BASE, relative path, and *_FULL`)
  }
}

for (const moduleName of ['Auth', 'Test', 'User', 'Admin', 'Common', 'Integration']) {
  const blockPattern = new RegExp(`public static class ${moduleName}[\\s\\S]*?private ${moduleName}\\(\\)`)
  const block = source.match(blockPattern)?.[0]
  if (!block) {
    fail(`missing ApiConstants.${moduleName} module block or private constructor`)
  }

  if (!block.includes('BASE = API_PREFIX +')) {
    fail(`ApiConstants.${moduleName} must define BASE using API_PREFIX`)
  }
}

console.log('check-api-constants: ok')

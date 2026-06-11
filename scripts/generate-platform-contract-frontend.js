#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const contractPath = path.join(root, 'contracts/platform-contract.json')
const outputPath = path.join(root, 'frontend/src/contracts/platform-contract.ts')
const checkMode = process.argv.includes('--check')

function fail(message) {
  console.error(`generate-platform-contract-frontend: ${message}`)
  process.exit(1)
}

function generateContent(contract) {
  const serialized = JSON.stringify(contract, null, 2)
  return `/* eslint-disable */\n// Generated from contracts/platform-contract.json. Do not edit manually.\n// Run: node scripts/generate-platform-contract-frontend.js\n\nexport const PLATFORM_CONTRACT = ${serialized} as const\n\nexport const API_SUCCESS_CODE = PLATFORM_CONTRACT.responseEnvelope.successCode\nexport const REQUEST_HEADERS = PLATFORM_CONTRACT.requestHeaders\nexport const FRONTEND_PROPAGATED_HEADER_KEYS = PLATFORM_CONTRACT.frontendPropagatedHeaders\nexport const BACKEND_PROPAGATED_HEADER_KEYS = PLATFORM_CONTRACT.backendPropagatedHeaders\nexport const DEFAULT_TIME_ZONE = PLATFORM_CONTRACT.time.defaultTimeZone\nexport const DEFAULT_LOCALE = PLATFORM_CONTRACT.locale.defaultLocale\nexport const SUPPORTED_LOCALES = PLATFORM_CONTRACT.locale.supportedLocales\n\nexport type PlatformContract = typeof PLATFORM_CONTRACT\nexport type PlatformRequestHeaderKey = keyof typeof REQUEST_HEADERS\nexport type PlatformFrontendPropagatedHeaderKey = (typeof FRONTEND_PROPAGATED_HEADER_KEYS)[number]\nexport type PlatformBackendPropagatedHeaderKey = (typeof BACKEND_PROPAGATED_HEADER_KEYS)[number]\nexport type PlatformSupportedLocale = (typeof SUPPORTED_LOCALES)[number]\n`
}

if (!fs.existsSync(contractPath)) {
  fail('missing contracts/platform-contract.json')
}

let contract
try {
  contract = JSON.parse(fs.readFileSync(contractPath, 'utf8'))
} catch (error) {
  fail(`invalid contracts/platform-contract.json: ${error.message}`)
}

const content = generateContent(contract)

if (checkMode) {
  if (!fs.existsSync(outputPath)) {
    fail('missing frontend/src/contracts/platform-contract.ts')
  }

  const current = fs.readFileSync(outputPath, 'utf8')
  if (current !== content) {
    fail('frontend/src/contracts/platform-contract.ts is out of date')
  }

  console.log('generate-platform-contract-frontend: ok')
  process.exit(0)
}

fs.mkdirSync(path.dirname(outputPath), { recursive: true })
fs.writeFileSync(outputPath, content)
console.log('generate-platform-contract-frontend: wrote frontend/src/contracts/platform-contract.ts')

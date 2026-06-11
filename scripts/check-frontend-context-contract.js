#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const contractPath = path.join(root, 'contracts/platform-contract.json')

const allowedFiles = new Set([
  'frontend/src/contracts/platform-contract.ts',
  'frontend/src/utils/http/context.ts',
  'frontend/src/utils/locale/index.ts'
])

const requiredContextTokens = [
  'export interface FrontendRequestContext',
  'export const createRequestId',
  'export const getOrCreateTraceId',
  'export const buildRequestContext',
  'export const buildRequestContextHeaders',
  'FRONTEND_PROPAGATED_HEADER_KEYS',
  "import { getLanguageTag } from '@/utils/locale'",
  'REQUEST_HEADERS[key]',
  'requestId: createRequestId()',
  'traceId: getOrCreateTraceId()',
  'timeZone: getClientTimeZone()',
  'acceptLanguage: getLanguageTag(language)',
  'SESSION_TRACE_ID_KEY'
]

const requiredErrorTokens = [
  'export interface HttpErrorContext',
  'export const readHttpHeader',
  'export const buildHttpErrorContext',
  'REQUEST_HEADERS.requestId',
  'REQUEST_HEADERS.traceId',
  'traceId?: string',
  'traceId: this.traceId'
]

const requiredHttpTokens = [
  'buildResponseErrorContext',
  'buildAxiosErrorContext',
  'buildHttpErrorContext',
  'handleUnauthorizedError(message, context)',
  'createHttpError(message || $t'
]

function fail(message) {
  console.error(`check-frontend-context-contract: ${message}`)
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
  if (!fs.existsSync(fullPath)) return

  const stat = fs.statSync(fullPath)
  if (stat.isFile()) {
    visitor(relativePath)
    return
  }

  for (const entry of fs.readdirSync(fullPath, { withFileTypes: true })) {
    if (entry.isDirectory() && ['node_modules', 'dist'].includes(entry.name)) {
      continue
    }

    walk(path.join(relativePath, entry.name), visitor)
  }
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

let contract
try {
  contract = JSON.parse(fs.readFileSync(contractPath, 'utf8'))
} catch (error) {
  fail(`invalid contracts/platform-contract.json: ${error.message}`)
}

const frontendHeaders = contract.frontendPropagatedHeaders || []
if (!frontendHeaders.length) {
  fail('contracts/platform-contract.json must define frontendPropagatedHeaders')
}

const requestHeaders = contract.requestHeaders || {}
for (const key of frontendHeaders) {
  if (!requestHeaders[key]) {
    fail(`frontendPropagatedHeaders contains unknown request header key: ${key}`)
  }
}

const contextSource = read('frontend/src/utils/http/context.ts')
for (const token of requiredContextTokens) {
  if (!contextSource.includes(token)) {
    fail(`frontend/src/utils/http/context.ts is missing token: ${token}`)
  }
}

const errorSource = read('frontend/src/utils/http/error.ts')
for (const token of requiredErrorTokens) {
  if (!errorSource.includes(token)) {
    fail(`frontend/src/utils/http/error.ts is missing token: ${token}`)
  }
}

const httpSource = read('frontend/src/utils/http/index.ts')
for (const token of requiredHttpTokens) {
  if (!httpSource.includes(token)) {
    fail(`frontend/src/utils/http/index.ts is missing token: ${token}`)
  }
}

const platformSource = read('frontend/src/contracts/platform-contract.ts')
if (!platformSource.includes('FRONTEND_PROPAGATED_HEADER_KEYS')) {
  fail('frontend/src/contracts/platform-contract.ts is missing FRONTEND_PROPAGATED_HEADER_KEYS')
}
for (const token of [
  'DEFAULT_LOCALE',
  'SUPPORTED_LOCALES',
  'PlatformSupportedLocale',
  'BACKEND_PROPAGATED_HEADER_KEYS',
  'PlatformBackendPropagatedHeaderKey'
]) {
  if (!platformSource.includes(token)) {
    fail(`frontend/src/contracts/platform-contract.ts is missing ${token}`)
  }
}

const localeSource = read('frontend/src/utils/locale/index.ts')
for (const token of [
  'DEFAULT_LOCALE',
  'SUPPORTED_LOCALES',
  'PlatformSupportedLocale',
  'matchSupportedLocale',
  'getClientLocale',
  'getLanguageTag'
]) {
  if (!localeSource.includes(token)) {
    fail(`frontend/src/utils/locale/index.ts is missing ${token}`)
  }
}

const checkedExtensions = new Set(['.ts', '.tsx', '.vue'])
const headerValues = Object.values(requestHeaders)

walk('frontend/src', (relativeFile) => {
  if (!checkedExtensions.has(path.extname(relativeFile))) return
  if (allowedFiles.has(relativeFile)) return

  const source = read(relativeFile)
  for (const header of headerValues) {
    const pattern = new RegExp(`['"\`]${escapeRegExp(header)}['"\`]`)
    if (pattern.test(source)) {
      fail(`${relativeFile} directly references platform request header: ${header}`)
    }
  }
})

console.log('check-frontend-context-contract: ok')

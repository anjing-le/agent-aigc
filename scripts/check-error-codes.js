#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const contractPath = path.join(root, 'contracts/platform-contract.json')
const errorCodeDir = path.join(root, 'backend/src/main/java/com/anjing/model/errorcode')
const remoteWrapperFile = 'backend/src/main/java/com/anjing/util/RemoteCallWrapper.java'

function fail(message) {
  console.error(`check-error-codes: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function parseRange(range) {
  if (/^\d+$/.test(range)) {
    const value = Number(range)
    return { start: value, end: value }
  }

  const match = range.match(/^(\d+)-(\d+)$/)
  if (!match) {
    fail(`invalid error code range in platform contract: ${range}`)
  }

  const start = Number(match[1])
  const end = Number(match[2])
  if (start > end) {
    fail(`invalid descending error code range in platform contract: ${range}`)
  }

  return { start, end }
}

function codeInRange(code, range) {
  const value = Number(code)
  return value >= range.start && value <= range.end
}

function enumNameFromFile(fileName) {
  return fileName.replace(/\.java$/, '')
}

if (!fs.existsSync(contractPath)) {
  fail('missing contracts/platform-contract.json')
}

if (!fs.existsSync(errorCodeDir)) {
  fail('missing backend errorcode package')
}

let contract
try {
  contract = JSON.parse(fs.readFileSync(contractPath, 'utf8'))
} catch (error) {
  fail(`invalid contracts/platform-contract.json: ${error.message}`)
}

const successCode = contract.responseEnvelope?.successCode
if (successCode !== '0') {
  fail('responseEnvelope.successCode must be "0"')
}

const errorRanges = (contract.errorCodeRanges || []).map((item) => ({
  label: item.range,
  ...parseRange(item.range)
}))

if (!errorRanges.length) {
  fail('platform contract must define errorCodeRanges')
}

const retryableRanges = (contract.retryableErrorCodeRanges || []).map((range) => ({
  label: range,
  ...parseRange(range)
}))

if (!retryableRanges.some((range) => range.label === '1800-1899')) {
  fail('platform contract must keep 1800-1899 as the default retryable remote range')
}

const javaFiles = fs.readdirSync(errorCodeDir)
  .filter((fileName) => fileName.endsWith('.java'))
  .sort()

const enumFiles = javaFiles.filter((fileName) => fileName !== 'ErrorCode.java')
if (!enumFiles.length) {
  fail('no XxxErrorCode enum files found')
}

const seenCodes = new Map()

for (const fileName of enumFiles) {
  const relativeFile = `backend/src/main/java/com/anjing/model/errorcode/${fileName}`
  const source = read(relativeFile)
  const expectedEnum = enumNameFromFile(fileName)

  const enumMatch = source.match(new RegExp(`public\\s+enum\\s+${expectedEnum}\\s+implements\\s+([^\\{]+)\\{`))
  if (!enumMatch || !/\bErrorCode\b/.test(enumMatch[1])) {
    fail(`${relativeFile} must declare "public enum ${expectedEnum} implements ErrorCode"`)
  }

  const entries = [...source.matchAll(/^\s*([A-Z][A-Z0-9_]*)\s*\(\s*"([^"]+)"\s*,/gm)]
  if (!entries.length) {
    fail(`${relativeFile} does not define any enum error codes`)
  }

  for (const entry of entries) {
    const [, name, code] = entry
    const location = `${expectedEnum}.${name}`

    if (code === successCode) {
      fail(`${location} uses success code "${successCode}", which is reserved for APIResponse`)
    }

    if (!/^\d{4}$/.test(code)) {
      fail(`${location} code must be a 4-digit string, got "${code}"`)
    }

    const matchedRange = errorRanges.find((range) => codeInRange(code, range))
    if (!matchedRange) {
      fail(`${location} code ${code} is outside contracts/platform-contract.json errorCodeRanges`)
    }

    const previous = seenCodes.get(code)
    if (previous) {
      fail(`duplicate error code ${code}: ${previous} and ${location}`)
    }
    seenCodes.set(code, location)

    if (expectedEnum === 'RemoteErrorCode') {
      const isRemoteRange = matchedRange.label === '1600-1899'
      if (!isRemoteRange) {
        fail(`${location} code ${code} must stay inside the remote range 1600-1899`)
      }
    }
  }
}

const remoteWrapper = read(remoteWrapperFile)
if (!remoteWrapper.includes('PlatformContractConstants.ErrorCodes.RETRYABLE_RANGES')) {
  fail(`${remoteWrapperFile} must use PlatformContractConstants.ErrorCodes.RETRYABLE_RANGES`)
}

const remoteRetryableCodes = [...seenCodes.entries()]
  .filter(([, location]) => location.startsWith('RemoteErrorCode.'))
  .filter(([code]) => retryableRanges.some((range) => codeInRange(code, range)))

if (!remoteRetryableCodes.length) {
  fail('RemoteErrorCode must define at least one retryable code in retryableErrorCodeRanges')
}

console.log('check-error-codes: ok')

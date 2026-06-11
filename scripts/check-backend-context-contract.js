#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const contractPath = path.join(root, 'contracts/platform-contract.json')

const files = {
  filter: 'backend/src/main/java/com/anjing/config/http/RequestContextFilter.java',
  context: 'backend/src/main/java/com/anjing/model/request/GlobalRequestContext.java',
  holder: 'backend/src/main/java/com/anjing/context/GlobalRequestContextHolder.java',
  aspect: 'backend/src/main/java/com/anjing/aspect/ControllerLogAspect.java',
  logback: 'backend/src/main/resources/logback-spring.xml',
  remoteWrapper: 'backend/src/main/java/com/anjing/util/RemoteCallWrapper.java',
  localeUtils: 'backend/src/main/java/com/anjing/util/LocaleUtils.java',
  timeZoneUtils: 'backend/src/main/java/com/anjing/util/TimeZoneUtils.java',
  apiResponse: 'backend/src/main/java/com/anjing/model/response/APIResponse.java',
  status: 'project_document/STATUS.md',
  roadmap: 'project_document/ROADMAP.md',
  architecture: 'project_document/ARCHITECTURE_EVOLUTION.md'
}

function fail(message) {
  console.error(`check-backend-context-contract: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function requireToken(relativeFile, token) {
  const source = read(relativeFile)
  if (!source.includes(token)) {
    fail(`${relativeFile} is missing token: ${token}`)
  }
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

const requestHeaders = contract.requestHeaders || {}
for (const headerName of Object.values(requestHeaders)) {
  requireToken('backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java', `"${headerName}"`)
}

for (const token of [
  'GlobalRequestContextHolder.set(context)',
  'response.setHeader(RequestHeaderConstants.REQUEST_ID',
  'response.setHeader(RequestHeaderConstants.TRACE_ID',
  'RequestHeaderConstants.ACCEPT_LANGUAGE',
  'LocaleUtils.normalizeAcceptLanguage',
  'RequestHeaderConstants.TIME_ZONE',
  'TimeZoneUtils.normalizeTimeZone',
  'MDC_REQUEST_ID = "requestId"',
  'MDC_TRACE_ID = "traceId"',
  'MDC_TENANT_ID = "tenantId"',
  'MDC_USER_ID = "userId"'
]) {
  requireToken(files.filter, token)
}

for (const token of [
  'private String requestId',
  'private String traceId',
  'private String tenantId',
  'private String userId',
  'private String callerId',
  'private String locale',
  'private String timeZone'
]) {
  requireToken(files.context, token)
}

for (const token of [
  'requestIdOrEmpty',
  'requestIdOrNull',
  'traceIdOrEmpty',
  'traceIdOrNull',
  'tenantIdOrEmpty',
  'userIdOrEmpty',
  'callerIdOrEmpty',
  'localeOrEmpty',
  'timeZoneOrEmpty'
]) {
  requireToken(files.holder, token)
}

for (const token of [
  'PlatformContractConstants.Locale.DEFAULT_LOCALE',
  'PlatformContractConstants.Locale.SUPPORTED_LOCALES',
  'normalizeAcceptLanguage',
  'Locale.lookup',
  'return DEFAULT_LOCALE'
]) {
  requireToken(files.localeUtils, token)
}

for (const token of [
  'PlatformContractConstants.Time.DEFAULT_TIME_ZONE',
  'defaultTimeZoneId',
  'normalizeTimeZone',
  'parseOrDefault',
  'return defaultTimeZoneId()'
]) {
  requireToken(files.timeZoneUtils, token)
}

for (const token of [
  'API_REQUEST_START',
  'API_REQUEST_END',
  'API_REQUEST_ERROR',
  'path={}',
  'durationMs={}',
  'errorCode={}',
  'resolveResultCode',
  'resolveExceptionCode',
  'APIResponse',
  'BizException',
  'SystemException'
]) {
  requireToken(files.aspect, token)
}

for (const token of [
  'rid=%X{requestId}',
  'tid=%X{traceId}',
  'tenant=%X{tenantId}',
  'user=%X{userId}'
]) {
  requireToken(files.logback, token)
}

for (const token of [
  'currentContextHeaders',
  'serviceCallHeaders',
  'PlatformContractConstants.BACKEND_PROPAGATED_HEADER_KEYS',
  'appendContextHeader',
  'ensureRequestTraceHeaders',
  'RequestHeaderConstants.REQUEST_ID',
  'RequestHeaderConstants.TRACE_ID',
  'RequestHeaderConstants.TENANT_ID',
  'RequestHeaderConstants.USER_ID',
  'RequestHeaderConstants.CALLER_ID',
  'RequestHeaderConstants.TIME_ZONE',
  'RequestHeaderConstants.ACCEPT_LANGUAGE'
]) {
  requireToken(files.remoteWrapper, token)
}

requireToken(files.apiResponse, 'GlobalRequestContextHolder.requestIdOrNull()')
requireToken(files.status, 'node scripts/check-backend-context-contract.js')
requireToken(files.architecture, 'scripts/check-backend-context-contract.js')
requireToken(files.roadmap, '日志统一输出 requestId、traceId、用户、租户、路径、耗时和错误码')

console.log('check-backend-context-contract: ok')

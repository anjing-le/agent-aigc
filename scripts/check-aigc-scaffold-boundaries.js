#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')

function fail(message) {
  console.error(`check-aigc-scaffold-boundaries: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function readJson(relativeFile) {
  try {
    return JSON.parse(read(relativeFile))
  } catch (error) {
    fail(`invalid ${relativeFile}: ${error.message}`)
  }
}

function requireToken(relativeFile, token) {
  const source = read(relativeFile)
  if (!source.includes(token)) {
    fail(`${relativeFile} is missing token: ${token}`)
  }
}

function requireAbsent(relativeFile, pattern, description) {
  const source = read(relativeFile)
  if (pattern.test(source)) {
    fail(`${relativeFile} contains ${description}: ${pattern}`)
  }
}

function walk(dir, matcher, files = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (['node_modules', 'dist', 'target'].includes(entry.name)) continue
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(fullPath, matcher, files)
    } else if (matcher(entry.name)) {
      files.push(fullPath)
    }
  }
  return files
}

function relative(file) {
  return path.relative(root, file)
}

function requireAbsentInFiles(relativeFiles, pattern, description) {
  for (const relativeFile of relativeFiles) {
    requireAbsent(relativeFile, pattern, description)
  }
}

const manifest = readJson('contracts/service-boundaries.json')
const aigcBoundary = (manifest.boundaries || []).find((boundary) => boundary.id === 'aigc')

if (!aigcBoundary) {
  fail('contracts/service-boundaries.json must declare the aigc boundary')
}

if (aigcBoundary.basePath !== '/api/aigc') {
  fail(`aigc basePath must be /api/aigc, got ${aigcBoundary.basePath}`)
}

if (aigcBoundary.apiConstantsClass !== 'Aigc' || aigcBoundary.apiPathsKey !== 'aigc') {
  fail('aigc boundary must map to ApiConstants.Aigc and ApiPaths.aigc')
}

for (const route of aigcBoundary.routes || []) {
  if (!route.backendConstant || !route.frontendKey || !route.path || !route.methods?.length) {
    fail(`aigc route ${route.name || '<unknown>'} must declare backendConstant, frontendKey, path and methods`)
  }
  if (!route.path.startsWith('/api/aigc')) {
    fail(`aigc route ${route.name} must stay under /api/aigc, got ${route.path}`)
  }
}

for (const token of [
  'public static class Aigc',
  'public static final String BASE = API_PREFIX + "/aigc"',
  'public static final String GENERATE_FULL = BASE + GENERATE',
  'public static final String ASSET_DETAIL_FULL = BASE + ASSET_DETAIL'
]) {
  requireToken('backend/src/main/java/com/anjing/model/constants/ApiConstants.java', token)
}

for (const token of [
  'AigcProviderRouteConfigRepository',
  'findByContentType',
  'JpaRepository'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/repository/AigcProviderRouteConfigRepository.java', token)
}

for (const token of [
  'AigcProviderCredentialConfigRepository',
  'findByProviderKey',
  'JpaRepository'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/repository/AigcProviderCredentialConfigRepository.java', token)
}

for (const token of [
  'getActiveProvider',
  'getConfiguredActiveProvider',
  'getRouteConfigSource',
  'saveActiveProvider'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcProviderRouteConfigService.java', token)
}

for (const token of [
  'getGoogleCredential',
  'isGoogleConfigured',
  'getGoogleCredentialSource',
  'saveGoogleCredential'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcProviderCredentialConfigService.java', token)
}

requireAbsentInFiles([
  'backend/src/main/java/com/anjing/aigc/model/dto/ModelInfo.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ModelListResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ProviderProbeResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ProviderRouteUpdateResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ProviderCredentialUpdateResponse.java',
  'frontend/src/api/model/aigcModel.ts',
  'frontend/src/views/aigc/models/index.vue'
], /\b(apiKey|accessKey|secretKey|accessKeySecret)\b/i, 'provider secret field exposed to frontend contracts')

requireAbsent(
  'backend/src/main/java/com/anjing/aigc/provider/ProviderRouter.java',
  /substring\(0,\s*Math\.min\(8,\s*key\.length\(\)\)|key\.substring\(key\.length\(\)\s*-\s*4\)/,
  'provider secret prefix/suffix logging'
)

for (const token of [
  'describeSecret',
  'configured(length=',
  '不输出任何密钥片段'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/provider/ProviderRouter.java', token)
}

for (const token of [
  'SENSITIVE_FIELD_NAMES',
  '"credential"',
  '"apiKey"',
  '"secretKey"',
  'redactJsonNode'
]) {
  requireToken('backend/src/main/java/com/anjing/aspect/ControllerLogAspect.java', token)
}

for (const relativeFile of [
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleImageProvider.java',
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleVideoProvider.java',
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleAudioProvider.java'
]) {
  requireToken(relativeFile, 'credentialConfigService.getGoogleCredential()')
  requireAbsent(relativeFile, /getProviders\(\)\.getGoogle\(\)\.getApiKey\(\)/, 'direct Google credential access')
}

for (const token of [
  'import com.anjing.model.constants.ApiConstants;',
  'import com.anjing.model.response.APIResponse;',
  'import com.anjing.model.response.PageResult;',
  '@RequestMapping(ApiConstants.Aigc.BASE)',
  '@Tag(name = "AIGC Creation"',
  'APIResponse<GenerateResponse>',
  'APIResponse<PageResult<GalleryDTO>>',
  'APIResponse<PageResult<AssetDTO>>',
  'APIResponse<PageResult<MaterialDTO>>'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/controller/AigcController.java', token)
}

requireAbsent(
  'backend/src/main/java/com/anjing/aigc/controller/AigcController.java',
  /@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\(\s*["'`]\/api/,
  'direct /api mapping instead of ApiConstants.Aigc'
)

for (const token of [
  'aigc: {',
  'generate: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.generate',
  'modelActiveProvider: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelActiveProvider',
  'modelProviderCredential: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderCredential',
  'assetDetail: (assetId: string | number)',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.assetDetail'
]) {
  requireToken('frontend/src/api/paths.ts', token)
}

for (const token of [
  "import { openApiRequest } from './openapiClient'",
  "openApiRequest('generate'",
  "openApiRequest('getTaskStatus'",
  "openApiRequest('getGalleryList'",
  "openApiRequest('getAssetList'",
  "openApiRequest('updateActiveProvider'",
  "openApiRequest('updateProviderCredential'",
  "openApiRequest('uploadMaterial'"
]) {
  requireToken('frontend/src/api/aigc.ts', token)
}

for (const token of [
  "import type {",
  'OpenApiOperationData',
  'OpenApiOperationQuery',
  'OpenApiOperationRequest',
  "import type * as Schemas from '@/contracts/openapi/schemas'",
  "OpenApiOperationRequest<'generate'>",
  "OpenApiOperationData<'getGalleryList'>",
  "OpenApiOperationData<'getAssetList'>",
  "OpenApiOperationData<'updateActiveProvider'>",
  "OpenApiOperationData<'updateProviderCredential'>"
]) {
  requireToken('frontend/src/api/model/aigcModel.ts', token)
}

requireAbsent('frontend/src/api/aigc.ts', /from ['"]@\/utils\/http|from ['"]\.\/paths|request\./, 'manual HTTP client or ApiPaths usage')
requireAbsent('frontend/src/api/model/aigcModel.ts', /interface\s+\w+\s*{/, 'handwritten interface blocks instead of OpenAPI-derived type aliases')

const aigcViewFiles = walk(path.join(root, 'frontend/src/views/aigc'), (name) => /\.(ts|vue)$/.test(name))
for (const file of aigcViewFiles) {
  const relativeFile = relative(file)
  const source = fs.readFileSync(file, 'utf8')
  if (/['"`]\/api\/aigc/.test(source)) {
    fail(`${relativeFile} must not hardcode /api/aigc paths`)
  }
  if (source.includes('openApiRequest(')) {
    fail(`${relativeFile} must call frontend/src/api/aigc.ts instead of openApiRequest directly`)
  }
  if (/from ['"]@\/utils\/http|from ['"]@\/api\/openapiClient|from ['"]@\/api\/paths/.test(source)) {
    fail(`${relativeFile} must keep HTTP/OpenAPI/path details inside API modules`)
  }
}

for (const token of [
  'com.anjing.aigc',
  'frontend/src/views/aigc',
  'ApiConstants.Aigc',
  'openApiRequest',
  './scripts/check-aigc-scaffold-boundaries.js'
]) {
  requireToken('project_document/SCAFFOLD_INHERITANCE_MAP.md', token)
}

console.log('check-aigc-scaffold-boundaries: ok')

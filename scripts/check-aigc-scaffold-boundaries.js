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
  'public static final String OWNERSHIP_BACKFILL_FULL = BASE + OWNERSHIP_BACKFILL',
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
  'AigcProviderParamConfigRepository',
  'findByContentTypeAndProviderKey',
  'JpaRepository'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/repository/AigcProviderParamConfigRepository.java', token)
}

for (const token of [
  'AigcProviderAuditLogRepository',
  'findByContentTypeAndAction',
  'JpaRepository'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/repository/AigcProviderAuditLogRepository.java', token)
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
  'getGoogleCredentialStorageMode',
  'AigcProviderCredentialCodec',
  'saveGoogleCredential'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcProviderCredentialConfigService.java', token)
}

for (const token of [
  'AES/GCM/NoPadding',
  'ENCRYPTED_PREFIX',
  'aigcProperties.getSecurity().getCredentialMasterKey()'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcProviderCredentialCodec.java', token)
}

requireAbsent(
  'backend/src/main/java/com/anjing/aigc/service/AigcProviderCredentialConfigService.java',
  /setCredentialValue\(normalizedCredential\)/,
  'provider credential stored without codec'
)

for (const token of [
  'getDefaultParams',
  'saveGoogleDefaultParams',
  'getParamConfigSource',
  'getGoogleImageAspectRatio',
  'getGoogleVideoDuration',
  'getGoogleAudioVoice'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcProviderParamConfigService.java', token)
}

for (const token of [
  'GlobalRequestContextHolder',
  'ACTION_ACTIVE_PROVIDER',
  'ACTION_CREDENTIAL',
  'ACTION_PARAMS',
  'ACTION_PERMISSION_DENIED',
  'ACTION_OWNERSHIP_BACKFILL',
  'recordPermissionDenied',
  'getAuditLogs'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcProviderAuditLogService.java', token)
}

for (const token of [
  'GlobalRequestContextHolder',
  'AuthErrorCode.PERMISSION_DENIED',
  'recordPermissionDenied',
  'getProviderManagement().getAdminRoles()',
  'assertCanManageAigc'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcProviderManagementPermissionService.java', token)
}

for (const token of [
  'AigcOwnershipBackfillService',
  'OwnershipBackfillRequest',
  'OwnershipBackfillResponse',
  'ACTION_OWNERSHIP_BACKFILL',
  'countMissingOwnership',
  'backfillMissingOwnership',
  'confirmBackfill',
  'currentOwnerId'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcOwnershipBackfillService.java', token)
}

for (const relativeFile of [
  'backend/src/main/java/com/anjing/aigc/repository/AigcAssetRepository.java',
  'backend/src/main/java/com/anjing/aigc/repository/AigcMaterialRepository.java',
  'backend/src/main/java/com/anjing/aigc/repository/AigcTaskRepository.java'
]) {
  requireToken(relativeFile, 'countMissingOwnership')
  requireToken(relativeFile, 'backfillMissingOwnership')
}

requireAbsentInFiles([
  'backend/src/main/java/com/anjing/aigc/model/dto/ModelInfo.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ModelListResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ProviderProbeResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ProviderRouteUpdateResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ProviderCredentialUpdateResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/request/ProviderSmokeTestRequest.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ProviderSmokeTestResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/StorageBackendStatusResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/StorageStatusResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/StorageAuditLogResponse.java',
  'backend/src/main/java/com/anjing/aigc/model/response/ProviderAuditLogResponse.java',
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
  'AigcProviderManagementPermissionService',
  'assertProviderManagementPermission',
  'ACTION_ACTIVE_PROVIDER',
  'ACTION_CREDENTIAL',
  'ACTION_PARAMS',
  'ACTION_SMOKE_TEST',
  'buildProviderChecks',
  'resolveCostProbe',
  'ProviderDiagnosticCheck',
  'smokeTestProvider',
  'confirmExternalCall',
  'createSmokeTestTask'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/impl/AigcServiceImpl.java', token)
}

for (const token of [
  'MODEL_PROVIDER_SMOKE_TEST',
  'MODEL_PROVIDER_SMOKE_TEST_FULL',
  'STORAGE_STATUS',
  'STORAGE_STATUS_FULL',
  'STORAGE_AUDITS',
  'STORAGE_AUDITS_FULL',
  'ASSET_DOWNLOAD',
  'ASSET_DOWNLOAD_FULL',
  'ASSET_PREVIEW',
  'ASSET_PREVIEW_FULL',
  'MATERIAL_DOWNLOAD',
  'MATERIAL_DOWNLOAD_FULL',
  'MATERIAL_PREVIEW',
  'MATERIAL_PREVIEW_FULL'
]) {
  requireToken('backend/src/main/java/com/anjing/model/constants/ApiConstants.java', token)
}

for (const token of [
  'ProviderSmokeTestRequest',
  'ProviderSmokeTestResponse',
  'StorageStatusResponse',
  'StorageAuditLogResponse',
  'AigcDownloadService',
  'AigcStorageAuditLogService',
  'AigcStorageService',
  'MODEL_PROVIDER_SMOKE_TEST',
  'STORAGE_STATUS',
  'STORAGE_AUDITS',
  'ASSET_DOWNLOAD',
  'ASSET_PREVIEW',
  'MATERIAL_DOWNLOAD',
  'MATERIAL_PREVIEW',
  'previewAsset',
  'previewMaterial',
  'ResponseEntity<Resource>'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/controller/AigcController.java', token)
}

for (const token of [
  'getStorageStatus',
  'saveBytes',
  'deleteByUrl',
  'resolveDownload',
  'buildLocalStatus',
  'buildOssStatus',
  'OssAigcStorageService',
  'AigcStorageAuditLogService',
  'resolveCleanupSupported',
  'recordSuccess',
  'recordFailure',
  'cleanupSupported',
  'cleanupAuditEnabled',
  'staticServingEnabled',
  'endpointConfigured',
  'bucketConfigured'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/storage/AigcStorageService.java', token)
}

for (const token of [
  'AigcDownloadService',
  'findVisibleByAssetId',
  'findVisibleByMaterialId',
  'storageService.resolveDownload',
  'ContentDisposition.attachment',
  'ContentDisposition.inline',
  'previewAsset',
  'previewMaterial'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcDownloadService.java', token)
}

for (const token of [
  'getResourceByUrl',
  'resolvePathByUrl',
  'startsWith(basePath)'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/storage/LocalAigcStorageService.java', token)
}

for (const token of [
  'staticServingEnabled'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/config/AigcProperties.java', token)
}

for (const token of [
  'static-serving-enabled',
  'AIGC_LOCAL_STATIC_SERVING_ENABLED'
]) {
  requireToken('backend/src/main/resources/application.yml', token)
}

for (const token of [
  'staticServingEnabled'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/model/response/StorageBackendStatusResponse.java', token)
  requireToken('frontend/src/views/aigc/assets/index.vue', token)
}

for (const token of [
  'S3Client',
  'PutObjectRequest',
  'DeleteObjectRequest',
  'GetObjectRequest',
  'S3Presigner',
  'RequestBody.fromBytes',
  'executeWithRetry',
  'getRetryCount',
  'getRetryIntervalMs',
  'getObjectKeyPrefix',
  'isPathStyleAccess',
  'isPublicRead',
  'buildAuthorizedDownloadUrl',
  'presignGetObjectUrl'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/storage/OssAigcStorageService.java', token)
}

for (const token of [
  'AigcStorageAuditLogRepository',
  'GlobalRequestContextHolder',
  'ACTION_UPLOAD',
  'ACTION_DELETE_URL',
  'isCleanupAuditEnabled',
  'ownershipService.currentOwnerId()',
  'ownershipService.currentTenantId()'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/storage/AigcStorageAuditLogService.java', token)
}

for (const token of [
  'AigcOwnershipService',
  'GlobalRequestContextHolder',
  'currentOwnerId',
  'currentTenantId',
  'applyOwnership',
  'canAccess'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcOwnershipService.java', token)
}

for (const token of [
  'ownerId',
  'tenantId'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/model/entity/AigcAsset.java', token)
  requireToken('backend/src/main/java/com/anjing/aigc/model/entity/AigcMaterial.java', token)
}

for (const token of [
  'findVisibleAssets',
  'findVisibleByAssetId'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/repository/AigcAssetRepository.java', token)
}

for (const token of [
  'findVisibleMaterials',
  'findVisibleByMaterialId',
  'findVisibleByMaterialIdIn'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/repository/AigcMaterialRepository.java', token)
}

for (const token of [
  'findVisibleByTaskId',
  'findVisibleByAssetId',
  'findVisibleByReferenceMaterialId'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/repository/AigcTaskRepository.java', token)
}

for (const token of [
  'AigcOwnershipService',
  'ownershipService.applyOwnership(material)',
  'findVisibleMaterials',
  'findVisibleByMaterialId'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcMaterialService.java', token)
}

for (const token of [
  'AigcOwnershipService',
  'ownershipService.applyOwnership(task)',
  'findVisibleTask',
  'findVisibleAsset',
  'findVisibleAssets',
  'findVisibleByMaterialIdIn',
  'findVisibleByReferenceMaterialId'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/impl/AigcServiceImpl.java', token)
}

for (const token of [
  'AigcStorageAuditLog',
  'aigc_storage_audit_log',
  'requestId',
  'traceId',
  'operatorId'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/model/entity/AigcStorageAuditLog.java', token)
}

for (const token of [
  'AigcStorageAuditLogRepository',
  'JpaRepository',
  'JpaSpecificationExecutor'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/repository/AigcStorageAuditLogRepository.java', token)
}

for (const token of [
  'software.amazon.awssdk',
  '<artifactId>s3</artifactId>',
  '<artifactId>commons-logging</artifactId>'
]) {
  requireToken('backend/pom.xml', token)
}

for (const token of [
  'AIGC_STORAGE_OSS_ENABLED',
  'AIGC_STORAGE_OSS_ENDPOINT',
  'AIGC_STORAGE_OSS_ACCESS_KEY_ID',
  'AIGC_STORAGE_OSS_ACCESS_KEY_SECRET',
  'AIGC_STORAGE_OSS_BUCKET',
  'AIGC_STORAGE_OSS_OBJECT_KEY_PREFIX',
  'AIGC_STORAGE_OSS_SIGNED_URL_ENABLED',
  'AIGC_STORAGE_OSS_RETRY_COUNT',
  'AIGC_STORAGE_CLEANUP_AUDIT_ENABLED'
]) {
  requireToken('backend/src/main/resources/application.yml', token)
}

for (const relativeFile of [
  'backend/src/main/java/com/anjing/aigc/service/AigcMaterialService.java',
  'backend/src/main/java/com/anjing/aigc/service/impl/AigcServiceImpl.java',
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleImageProvider.java',
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleVideoProvider.java',
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleAudioProvider.java'
]) {
  requireToken(relativeFile, 'AigcStorageService')
  requireAbsent(relativeFile, /LocalAigcStorageService/, 'direct local storage dependency outside storage adapter boundary')
}

for (const token of [
  'fetchSmokeTestProvider',
  'ProviderSmokeTestRequest',
  'ProviderSmokeTestResponse',
  'fetchGetStorageStatus',
  'StorageStatusResponse',
  'fetchGetStorageAuditLogs',
  'StorageAuditLogListResponse',
  'fetchBackfillOwnership',
  'OwnershipBackfillRequest',
  'OwnershipBackfillResponse'
]) {
  requireToken('frontend/src/api/aigc.ts', token)
}

for (const token of [
  'modelProviderSmokeTest',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderSmokeTest',
  'storageStatus',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.storageStatus',
  'storageAudits',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.storageAudits',
  'ownershipBackfill',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.ownershipBackfill',
  'assetDownload',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.assetDownload',
  'assetPreview',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.assetPreview',
  'materialDownload',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.materialDownload',
  'materialPreview',
  'SERVICE_BOUNDARY_ROUTE_PATHS.aigc.materialPreview'
]) {
  requireToken('frontend/src/api/paths.ts', token)
}

for (const token of [
  'ApiPaths.aigc.assetDownload',
  'ApiPaths.aigc.assetPreview',
  'ApiPaths.aigc.materialPreview',
  'resolveAigcAssetPreviewUrl',
  'resolveAigcMaterialPreviewUrl',
  'buildRequestContextHeaders',
  'REQUEST_HEADERS.userId',
  'fetch(resolveApiPath',
  'response.blob()'
]) {
  requireToken('frontend/src/utils/aigcAsset.ts', token)
}

for (const token of [
  'storageStatus',
  'storageAuditLogs',
  'formatStorageMode',
  'formatStorageAuditAction',
  'assetCleanupSupported',
  'fetchGetStorageStatus',
  'fetchGetStorageAuditLogs'
]) {
  requireToken('frontend/src/views/aigc/assets/index.vue', token)
}

for (const token of [
  'ProviderDiagnosticCheck',
  'costEstimateConfigured',
  'checks'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/model/dto/ModelInfo.java', token)
  requireToken('backend/src/main/java/com/anjing/aigc/model/response/ProviderProbeResponse.java', token)
  requireToken('frontend/src/views/aigc/models/index.vue', token)
}

for (const token of [
  'id',
  'label',
  'status',
  'message'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/model/response/ProviderDiagnosticCheck.java', token)
}

for (const token of [
  'AigcProviderCostEstimator',
  'STATUS_ESTIMATED',
  'STATUS_ESTIMATE_NOT_CONFIGURED',
  'getCost().getGoogle()'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcProviderCostEstimator.java', token)
}

for (const token of [
  'costEstimator.estimate(task)',
  'setEstimatedCostAmount',
  'setEstimatedCostCurrency',
  'setCostDescription'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/service/AigcTaskExecutor.java', token)
}

for (const token of [
  'estimatedCostAmount',
  'estimatedCostCurrency',
  'costUnit',
  'costDescription'
]) {
  requireToken('backend/src/main/java/com/anjing/aigc/model/response/ProviderExecutionSummary.java', token)
  requireToken('frontend/src/views/aigc/studio/components/GenerationPreview.vue', token)
  requireToken('frontend/src/views/aigc/assets/index.vue', token)
}

requireToken(
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleImageProvider.java',
  'paramConfigService.getGoogleImageAspectRatio()'
)
requireToken(
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleVideoProvider.java',
  'paramConfigService.getGoogleVideoDuration()'
)
requireToken(
  'backend/src/main/java/com/anjing/aigc/provider/google/GoogleAudioProvider.java',
  'paramConfigService.getGoogleAudioVoice()'
)

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
  'modelProviderParams: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderParams',
  'modelProviderAudits: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderAudits',
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
  "openApiRequest('updateProviderParams'",
  "openApiRequest('getProviderAuditLogs'",
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
  "OpenApiOperationData<'updateProviderCredential'>",
  "OpenApiOperationData<'updateProviderParams'>"
]) {
  requireToken('frontend/src/api/model/aigcModel.ts', token)
}

requireToken('frontend/src/views/aigc/models/index.vue', 'formatCredentialStorageMode')
for (const token of [
  'fetchBackfillOwnership',
  'ownershipBackfillResult',
  'handleOwnershipDryRun',
  'handleOwnershipApply',
  'ownership-backfill'
]) {
  requireToken('frontend/src/views/aigc/models/index.vue', token)
}

for (const token of [
  'REQUEST_HEADERS.userRoles',
  'REQUEST_HEADERS.userId',
  'REQUEST_HEADERS.userName',
  'applyUserContextHeaders'
]) {
  requireToken('frontend/src/utils/http/index.ts', token)
}

for (const token of [
  "OpenApiOperationQuery<'getProviderAuditLogs'>",
  "OpenApiOperationData<'getProviderAuditLogs'>"
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

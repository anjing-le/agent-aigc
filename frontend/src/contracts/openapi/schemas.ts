/* eslint-disable */
// Generated from OpenAPI JSON. Do not edit manually.
// Run: node scripts/generate-openapi-frontend-types.js <openapi-json-file>

export type JsonObject = Record<string, unknown>

export interface AgentAnalysis {
  analyzedIntent?: AnalyzedIntent
  audioParams?: AudioParams
  cleanPrompt?: string
  confidence?: number
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  imageParams?: ImageParams
  intent?: string
  optimizedPrompt?: string
  originalPrompt?: string
  selectedModel?: string
  videoParams?: VideoParams
}

export interface AnalyzedIntent {
  audioIntent?: boolean
  audioParams?: AudioParams
  cleanPrompt?: string
  confidence?: number
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  effectiveAudioParams?: AudioParams
  effectiveImageParams?: ImageParams
  effectiveVideoParams?: VideoParams
  enhancementSuggestions?: string[]
  hasReferenceImage?: boolean
  imageIntent?: boolean
  imageParams?: ImageParams
  intent?: string
  originalPrompt?: string
  videoIntent?: boolean
  videoParams?: VideoParams
}

export interface APIResponseAssetDetailResponse {
  code?: string
  data?: AssetDetailResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseAuthTokenResponse {
  code?: string
  data?: AuthTokenResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseCurrentUserResponse {
  code?: string
  data?: CurrentUserResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseGalleryAuthorProfileResponse {
  code?: string
  data?: GalleryAuthorProfileResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseGalleryDTO {
  code?: string
  data?: GalleryDTO
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseGalleryInteractionReportResponse {
  code?: string
  data?: GalleryInteractionReportResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseGalleryShareResponse {
  code?: string
  data?: GalleryShareResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseGenerateResponse {
  code?: string
  data?: GenerateResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseMapStringObject {
  code?: string
  data?: Record<string, unknown>
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseMaterialUploadResponse {
  code?: string
  data?: MaterialUploadResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseMiddlewareStatusReport {
  code?: string
  data?: MiddlewareStatusReport
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseModelListResponse {
  code?: string
  data?: ModelListResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseOwnershipBackfillResponse {
  code?: string
  data?: OwnershipBackfillResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponsePageResultAssetDTO {
  code?: string
  data?: PageResultAssetDTO
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponsePageResultGalleryAuditLogResponse {
  code?: string
  data?: PageResultGalleryAuditLogResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponsePageResultGalleryDTO {
  code?: string
  data?: PageResultGalleryDTO
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponsePageResultMaterialDTO {
  code?: string
  data?: PageResultMaterialDTO
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponsePageResultProviderAuditLogResponse {
  code?: string
  data?: PageResultProviderAuditLogResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponsePageResultStorageAuditLogResponse {
  code?: string
  data?: PageResultStorageAuditLogResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponsePageResultTaskStatusResponse {
  code?: string
  data?: PageResultTaskStatusResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseProviderCredentialUpdateResponse {
  code?: string
  data?: ProviderCredentialUpdateResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseProviderParamUpdateResponse {
  code?: string
  data?: ProviderParamUpdateResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseProviderProbeResponse {
  code?: string
  data?: ProviderProbeResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseProviderRouteUpdateResponse {
  code?: string
  data?: ProviderRouteUpdateResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseProviderSmokeTestResponse {
  code?: string
  data?: ProviderSmokeTestResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseStorageStatusResponse {
  code?: string
  data?: StorageStatusResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseString {
  code?: string
  data?: string
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseTaskStatusResponse {
  code?: string
  data?: TaskStatusResponse
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface APIResponseVoid {
  code?: string
  data?: unknown
  message?: string
  requestId?: string
  success?: boolean
  timestamp?: number
}

export interface AssetDetailResponse {
  asset?: AssetDTO
  task?: TaskStatusResponse
}

export interface AssetDTO {
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  createdAt?: string
  id?: string
  isPublished?: boolean
  model?: string
  prompt?: string
  thumbnailUrl?: string
  url?: string
}

export interface AudioParams {
  bpm?: number
  duration?: number
  mood?: string
  music?: boolean
  tts?: boolean
  type?: string
  voice?: string
}

/**
 * Authentication token payload
 */
export interface AuthTokenResponse {
  /**
   * Access token used in Authorization header
   */
  accessToken: string
  /**
   * Access token lifetime in seconds
   */
  expiresIn: number
  /**
   * Refresh token used to renew access token
   */
  refreshToken: string
  /**
   * Token type
   */
  tokenType: string
}

/**
 * Current authenticated user payload
 */
export interface CurrentUserResponse {
  /**
   * Avatar URL
   */
  avatar?: string
  /**
   * User creation time in ISO-8601 UTC format
   */
  createTime?: string
  /**
   * User email
   */
  email?: string
  /**
   * Display nickname
   */
  nickName?: string
  /**
   * Permission codes
   */
  permissions?: string[]
  /**
   * Role codes
   */
  roles: string[]
  /**
   * User id
   */
  userId: number
  /**
   * Login username
   */
  userName: string
}

export interface GalleryActionMetricResponse {
  action?: string
  successfulEvents?: number
  totalEvents?: number
}

export interface GalleryAssetMetricResponse {
  assetId?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  downloadCount?: number
  favoriteCount?: number
  likeCount?: number
  model?: string
  totalEvents?: number
}

export interface GalleryAuditLogResponse {
  action?: string
  assetId?: string
  callerId?: string
  clientIp?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  createdAt?: string
  id?: number
  message?: string
  model?: string
  operatorId?: string
  operatorName?: string
  promptSnapshot?: string
  requestId?: string
  success?: boolean
  tenantId?: string
  traceId?: string
}

export interface GalleryAuthorProfileResponse {
  assets?: PageResultGalleryDTO
  audioCount?: number
  authorId?: string
  authorName?: string
  dominantContentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  imageCount?: number
  publishedCount?: number
  topAssets?: GalleryDTO[]
  totalFavoriteCount?: number
  totalInteractionCount?: number
  totalLikeCount?: number
  videoCount?: number
}

export interface GalleryContentTypeMetricResponse {
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  successfulEvents?: number
  totalEvents?: number
}

export interface GalleryDailyMetricResponse {
  date?: string
  downloadCount?: number
  favoriteCount?: number
  likeCount?: number
  publishCount?: number
  successfulEvents?: number
  totalEvents?: number
}

export interface GalleryDTO {
  authorId?: string
  authorName?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  createdAt?: string
  favoriteCount?: number
  favoritedByCurrentUser?: boolean
  id?: string
  isPublished?: boolean
  likeCount?: number
  likedByCurrentUser?: boolean
  model?: string
  previewUrl?: string
  prompt?: string
  publicAccessMode?: string
  thumbnailUrl?: string
  url?: string
}

export interface GalleryInteractionReportResponse {
  actionMetrics?: GalleryActionMetricResponse[]
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  contentTypeMetrics?: GalleryContentTypeMetricResponse[]
  dailyMetrics?: GalleryDailyMetricResponse[]
  days?: number
  downloadCount?: number
  favoriteCount?: number
  generatedAt?: string
  likeCount?: number
  publishCount?: number
  startAt?: string
  successfulEvents?: number
  topAssets?: GalleryAssetMetricResponse[]
  totalEvents?: number
  unfavoriteCount?: number
  unlikeCount?: number
  unpublishCount?: number
}

export interface GalleryShareResponse {
  asset?: GalleryDTO
  downloadUrl?: string
  posterFooter?: string
  posterSubtitle?: string
  posterTitle?: string
  previewUrl?: string
  seoDescription?: string
  seoKeywords?: string
  seoTitle?: string
  sharePath?: string
}

export interface GenerateRequest {
  contentTypeHint?: string
  generationParams?: Record<string, unknown>
  prompt: string
  referenceImages?: string[]
  referenceMaterialIds?: string[]
}

export interface GenerateResponse {
  agentAnalysis?: AgentAnalysis
  estimatedTime?: number
  status?: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED"
  taskId?: string
}

export interface GenerationResult {
  assetId?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  errorCode?: string
  errorMessage?: string
  failed?: boolean
  metadata?: Record<string, unknown>
  model?: string
  processingTimeMs?: number
  prompt?: string
  success?: boolean
  taskId?: string
  thumbnailUrl?: string
  url?: string
}

export interface ImageParams {
  aspectRatio?: string
  imageSize?: string
  style?: string
  transparentBackground?: boolean
}

/**
 * Login request
 */
export interface LoginRequest {
  /**
   * Captcha code when enabled
   */
  captcha?: string
  /**
   * Password
   */
  password: string
  /**
   * Whether to keep the session longer
   */
  rememberMe?: boolean
  /**
   * Username or email
   */
  username: string
}

export interface MaterialDTO {
  contentType?: string
  createdAt?: string
  fileName?: string
  id?: string
  originalFileName?: string
  size?: number
  url?: string
}

export interface MaterialUploadResponse {
  contentType?: string
  createdAt?: string
  fileName?: string
  materialId?: string
  originalFileName?: string
  size?: number
  url?: string
}

export interface MiddlewareInfo {
  details?: string
  enabled?: boolean
  name?: string
  status?: "disabled" | "configured" | "ready" | "degraded"
  statusCode?: string
  statusDescription?: string
  version?: string
}

export interface MiddlewareStatusReport {
  features?: MiddlewareInfo[]
  status?: "disabled" | "configured" | "ready" | "degraded"
  statusCode?: string
  statusDescription?: string
  summary?: MiddlewareSummary
}

export interface MiddlewareSummary {
  byStatus?: Record<string, number>
  enabled?: number
  total?: number
}

export interface ModelInfo {
  active?: boolean
  activeProvider?: string
  available?: boolean
  checks?: ProviderDiagnosticCheck[]
  configuredModel?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  costEstimateConfigured?: boolean
  costStatus?: string
  credentialSource?: string
  credentialStorageMode?: string
  credentialUpdatedAt?: string
  defaultParams?: Record<string, unknown>
  description?: string
  icon?: string
  id?: string
  missingConfig?: string
  name?: string
  paramConfigSource?: string
  paramConfigUpdatedAt?: string
  provider?: string
  routeConfigSource?: string
  statusReason?: string
}

export interface ModelListResponse {
  audioModels?: ModelInfo[]
  imageModels?: ModelInfo[]
  videoModels?: ModelInfo[]
}

/**
 * 历史 AIGC 数据归属回填请求
 */
export interface OwnershipBackfillRequest {
  /**
   * 当 dryRun=false 时必须显式为 true，防止误写历史数据
   */
  confirmBackfill?: boolean
  /**
   * 是否仅预演；默认 true，不写入数据
   */
  dryRun?: boolean
}

/**
 * 历史 AIGC 数据归属回填结果
 */
export interface OwnershipBackfillResponse {
  /**
   * 待回填资产数量
   */
  assetCandidates?: number
  /**
   * 已回填资产数量
   */
  assetUpdated?: number
  /**
   * 检查时间
   */
  checkedAt?: string
  /**
   * 是否已显式确认写入
   */
  confirmed?: boolean
  /**
   * 是否仅预演
   */
  dryRun?: boolean
  /**
   * 待回填参考素材数量
   */
  materialCandidates?: number
  /**
   * 已回填参考素材数量
   */
  materialUpdated?: number
  /**
   * 执行提示
   */
  message?: string
  /**
   * 回填使用的用户 ID
   */
  ownerId?: string
  /**
   * 待回填任务数量
   */
  taskCandidates?: number
  /**
   * 已回填任务数量
   */
  taskUpdated?: number
  /**
   * 回填使用的租户 ID
   */
  tenantId?: string
}

export interface PageResultAssetDTO {
  current?: number
  records?: AssetDTO[]
  size?: number
  total?: number
}

export interface PageResultGalleryAuditLogResponse {
  current?: number
  records?: GalleryAuditLogResponse[]
  size?: number
  total?: number
}

export interface PageResultGalleryDTO {
  current?: number
  records?: GalleryDTO[]
  size?: number
  total?: number
}

export interface PageResultMaterialDTO {
  current?: number
  records?: MaterialDTO[]
  size?: number
  total?: number
}

export interface PageResultProviderAuditLogResponse {
  current?: number
  records?: ProviderAuditLogResponse[]
  size?: number
  total?: number
}

export interface PageResultStorageAuditLogResponse {
  current?: number
  records?: StorageAuditLogResponse[]
  size?: number
  total?: number
}

export interface PageResultTaskStatusResponse {
  current?: number
  records?: TaskStatusResponse[]
  size?: number
  total?: number
}

export interface ProviderAuditLogResponse {
  action?: string
  afterSummary?: Record<string, unknown>
  beforeSummary?: Record<string, unknown>
  callerId?: string
  clientIp?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  createdAt?: string
  id?: number
  operatorId?: string
  operatorName?: string
  providerKey?: string
  providerName?: string
  providerType?: string
  requestId?: string
  tenantId?: string
  traceId?: string
}

/**
 * Provider 凭证更新请求
 */
export interface ProviderCredentialUpdateRequest {
  /**
   * 内容类型
   */
  contentType: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  /**
   * Provider 凭证，只写入不回显
   */
  credential: string
  /**
   * Provider 类型或名称
   */
  provider: string
  /**
   * Provider 展示名称
   */
  providerName?: string
}

export interface ProviderCredentialUpdateResponse {
  available?: boolean
  configurationComplete?: boolean
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  credentialSource?: string
  credentialStorageMode?: string
  message?: string
  providerName?: string
  providerType?: string
  statusReason?: string
  updatedAt?: string
}

export interface ProviderDiagnosticCheck {
  id?: string
  label?: string
  message?: string
  status?: string
}

export interface ProviderExecutionSummary {
  costDescription?: string
  costStatus?: string
  costUnit?: string
  durationMs?: number
  estimatedCostAmount?: number
  estimatedCostCurrency?: string
  model?: string
  providerName?: string
  providerType?: string
}

/**
 * Provider 默认参数模板更新请求
 */
export interface ProviderParamUpdateRequest {
  /**
   * 内容类型
   */
  contentType: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  /**
   * 默认参数模板
   */
  defaultParams: Record<string, unknown>
  /**
   * Provider 类型或名称
   */
  provider: string
  /**
   * Provider 展示名称
   */
  providerName?: string
}

export interface ProviderParamUpdateResponse {
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  defaultParams?: Record<string, unknown>
  message?: string
  paramConfigSource?: string
  providerName?: string
  providerType?: string
  updatedAt?: string
}

/**
 * Provider 运行前探测请求
 */
export interface ProviderProbeRequest {
  /**
   * 内容类型
   */
  contentType: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  /**
   * Provider 类型或名称
   */
  provider: string
  /**
   * Provider 展示名称
   */
  providerName?: string
}

export interface ProviderProbeResponse {
  active?: boolean
  activeProvider?: string
  available?: boolean
  checkedAt?: string
  checks?: ProviderDiagnosticCheck[]
  configurationComplete?: boolean
  configuredModel?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  costEstimateConfigured?: boolean
  costStatus?: string
  credentialSource?: string
  credentialStorageMode?: string
  defaultParams?: Record<string, unknown>
  message?: string
  missingConfig?: string
  paramConfigSource?: string
  paramConfigUpdatedAt?: string
  providerName?: string
  providerType?: string
  registered?: boolean
  requestedProvider?: string
  routable?: boolean
  statusReason?: string
}

/**
 * Provider 运行时路由切换请求
 */
export interface ProviderRouteUpdateRequest {
  /**
   * 内容类型
   */
  contentType: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  /**
   * Provider 类型或名称
   */
  provider: string
  /**
   * Provider 展示名称
   */
  providerName?: string
}

export interface ProviderRouteUpdateResponse {
  activeProvider?: string
  available?: boolean
  configurationComplete?: boolean
  configuredModel?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  credentialSource?: string
  credentialStorageMode?: string
  defaultParams?: Record<string, unknown>
  message?: string
  missingConfig?: string
  paramConfigSource?: string
  paramConfigUpdatedAt?: string
  providerName?: string
  providerType?: string
  routable?: boolean
  routeConfigSource?: string
  statusReason?: string
  updatedAt?: string
}

/**
 * Provider 显式 smoke test 请求
 */
export interface ProviderSmokeTestRequest {
  /**
   * 确认会触发外部 Provider 调用；Google Provider 必须显式传 true
   */
  confirmExternalCall?: boolean
  /**
   * 内容类型；V1 仅支持 IMAGE
   */
  contentType: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  /**
   * 测试 Prompt；为空时使用最小验证 Prompt
   */
  prompt?: string
  /**
   * Provider 类型或名称
   */
  provider: string
  /**
   * Provider 展示名称
   */
  providerName?: string
}

export interface ProviderSmokeTestResponse {
  assetId?: string
  checkedAt?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  durationMs?: number
  errorCode?: string
  errorMessage?: string
  message?: string
  model?: string
  prompt?: string
  providerExecution?: ProviderExecutionSummary
  providerName?: string
  providerType?: string
  status?: string
  success?: boolean
  taskId?: string
  thumbnailUrl?: string
  url?: string
}

/**
 * Refresh token request
 */
export interface RefreshTokenRequest {
  /**
   * Refresh token returned by login
   */
  refreshToken: string
}

/**
 * 保存作品到灵感广场请求
 */
export interface SaveToGalleryRequest {
  /**
   * 资产 ID
   */
  assetId: string
}

export interface StorageAuditLogResponse {
  action?: string
  backend?: string
  callerId?: string
  clientIp?: string
  createdAt?: string
  directory?: string
  errorMessage?: string
  fileName?: string
  id?: number
  operatorId?: string
  operatorName?: string
  requestId?: string
  sizeBytes?: number
  success?: boolean
  tenantId?: string
  traceId?: string
  url?: string
}

export interface StorageBackendStatusResponse {
  available?: boolean
  backend?: string
  basePath?: string
  bucketConfigured?: boolean
  cdnConfigured?: boolean
  cleanupAuditEnabled?: boolean
  cleanupSupported?: boolean
  configured?: boolean
  enabled?: boolean
  endpointConfigured?: boolean
  message?: string
  objectKeyPrefix?: string
  pathStyleAccess?: boolean
  provider?: string
  publicRead?: boolean
  readable?: boolean
  retryCount?: number
  retryIntervalMs?: number
  signedUrlEnabled?: boolean
  signedUrlExpirationSeconds?: number
  staticServingEnabled?: boolean
  urlPrefix?: string
  writable?: boolean
}

export interface StorageStatusResponse {
  activeMode?: string
  assetCleanupSupported?: boolean
  checkedAt?: string
  local?: StorageBackendStatusResponse
  materialCleanupSupported?: boolean
  message?: string
  oss?: StorageBackendStatusResponse
}

export interface TaskStatusResponse {
  agentAnalysis?: AgentAnalysis
  createdAt?: string
  errorCode?: string
  errorMessage?: string
  progress?: number
  providerExecution?: ProviderExecutionSummary
  referenceMaterialIds?: string[]
  referenceMaterials?: MaterialDTO[]
  result?: GenerationResult
  status?: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED"
  taskId?: string
  updatedAt?: string
}

export interface VideoParams {
  aspectRatio?: string
  duration?: number
  modelSuffix?: string
  normalizedDuration?: number
  quality?: string
  resolution?: string
  withAudio?: boolean
}

export interface OpenApiSchemas {
  AgentAnalysis: AgentAnalysis
  AnalyzedIntent: AnalyzedIntent
  APIResponseAssetDetailResponse: APIResponseAssetDetailResponse
  APIResponseAuthTokenResponse: APIResponseAuthTokenResponse
  APIResponseCurrentUserResponse: APIResponseCurrentUserResponse
  APIResponseGalleryAuthorProfileResponse: APIResponseGalleryAuthorProfileResponse
  APIResponseGalleryDTO: APIResponseGalleryDTO
  APIResponseGalleryInteractionReportResponse: APIResponseGalleryInteractionReportResponse
  APIResponseGalleryShareResponse: APIResponseGalleryShareResponse
  APIResponseGenerateResponse: APIResponseGenerateResponse
  APIResponseMapStringObject: APIResponseMapStringObject
  APIResponseMaterialUploadResponse: APIResponseMaterialUploadResponse
  APIResponseMiddlewareStatusReport: APIResponseMiddlewareStatusReport
  APIResponseModelListResponse: APIResponseModelListResponse
  APIResponseOwnershipBackfillResponse: APIResponseOwnershipBackfillResponse
  APIResponsePageResultAssetDTO: APIResponsePageResultAssetDTO
  APIResponsePageResultGalleryAuditLogResponse: APIResponsePageResultGalleryAuditLogResponse
  APIResponsePageResultGalleryDTO: APIResponsePageResultGalleryDTO
  APIResponsePageResultMaterialDTO: APIResponsePageResultMaterialDTO
  APIResponsePageResultProviderAuditLogResponse: APIResponsePageResultProviderAuditLogResponse
  APIResponsePageResultStorageAuditLogResponse: APIResponsePageResultStorageAuditLogResponse
  APIResponsePageResultTaskStatusResponse: APIResponsePageResultTaskStatusResponse
  APIResponseProviderCredentialUpdateResponse: APIResponseProviderCredentialUpdateResponse
  APIResponseProviderParamUpdateResponse: APIResponseProviderParamUpdateResponse
  APIResponseProviderProbeResponse: APIResponseProviderProbeResponse
  APIResponseProviderRouteUpdateResponse: APIResponseProviderRouteUpdateResponse
  APIResponseProviderSmokeTestResponse: APIResponseProviderSmokeTestResponse
  APIResponseStorageStatusResponse: APIResponseStorageStatusResponse
  APIResponseString: APIResponseString
  APIResponseTaskStatusResponse: APIResponseTaskStatusResponse
  APIResponseVoid: APIResponseVoid
  AssetDetailResponse: AssetDetailResponse
  AssetDTO: AssetDTO
  AudioParams: AudioParams
  AuthTokenResponse: AuthTokenResponse
  CurrentUserResponse: CurrentUserResponse
  GalleryActionMetricResponse: GalleryActionMetricResponse
  GalleryAssetMetricResponse: GalleryAssetMetricResponse
  GalleryAuditLogResponse: GalleryAuditLogResponse
  GalleryAuthorProfileResponse: GalleryAuthorProfileResponse
  GalleryContentTypeMetricResponse: GalleryContentTypeMetricResponse
  GalleryDailyMetricResponse: GalleryDailyMetricResponse
  GalleryDTO: GalleryDTO
  GalleryInteractionReportResponse: GalleryInteractionReportResponse
  GalleryShareResponse: GalleryShareResponse
  GenerateRequest: GenerateRequest
  GenerateResponse: GenerateResponse
  GenerationResult: GenerationResult
  ImageParams: ImageParams
  LoginRequest: LoginRequest
  MaterialDTO: MaterialDTO
  MaterialUploadResponse: MaterialUploadResponse
  MiddlewareInfo: MiddlewareInfo
  MiddlewareStatusReport: MiddlewareStatusReport
  MiddlewareSummary: MiddlewareSummary
  ModelInfo: ModelInfo
  ModelListResponse: ModelListResponse
  OwnershipBackfillRequest: OwnershipBackfillRequest
  OwnershipBackfillResponse: OwnershipBackfillResponse
  PageResultAssetDTO: PageResultAssetDTO
  PageResultGalleryAuditLogResponse: PageResultGalleryAuditLogResponse
  PageResultGalleryDTO: PageResultGalleryDTO
  PageResultMaterialDTO: PageResultMaterialDTO
  PageResultProviderAuditLogResponse: PageResultProviderAuditLogResponse
  PageResultStorageAuditLogResponse: PageResultStorageAuditLogResponse
  PageResultTaskStatusResponse: PageResultTaskStatusResponse
  ProviderAuditLogResponse: ProviderAuditLogResponse
  ProviderCredentialUpdateRequest: ProviderCredentialUpdateRequest
  ProviderCredentialUpdateResponse: ProviderCredentialUpdateResponse
  ProviderDiagnosticCheck: ProviderDiagnosticCheck
  ProviderExecutionSummary: ProviderExecutionSummary
  ProviderParamUpdateRequest: ProviderParamUpdateRequest
  ProviderParamUpdateResponse: ProviderParamUpdateResponse
  ProviderProbeRequest: ProviderProbeRequest
  ProviderProbeResponse: ProviderProbeResponse
  ProviderRouteUpdateRequest: ProviderRouteUpdateRequest
  ProviderRouteUpdateResponse: ProviderRouteUpdateResponse
  ProviderSmokeTestRequest: ProviderSmokeTestRequest
  ProviderSmokeTestResponse: ProviderSmokeTestResponse
  RefreshTokenRequest: RefreshTokenRequest
  SaveToGalleryRequest: SaveToGalleryRequest
  StorageAuditLogResponse: StorageAuditLogResponse
  StorageBackendStatusResponse: StorageBackendStatusResponse
  StorageStatusResponse: StorageStatusResponse
  TaskStatusResponse: TaskStatusResponse
  VideoParams: VideoParams
}

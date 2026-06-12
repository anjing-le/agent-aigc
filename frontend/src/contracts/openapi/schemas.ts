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

export interface APIResponsePageResultAssetDTO {
  code?: string
  data?: PageResultAssetDTO
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

export interface GalleryDTO {
  authorName?: string
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  createdAt?: string
  id?: string
  isPublished?: boolean
  likeCount?: number
  model?: string
  prompt?: string
  thumbnailUrl?: string
  url?: string
}

export interface GenerateRequest {
  contentTypeHint?: string
  generationParams?: Record<string, unknown>
  prompt: string
  referenceImages?: string[]
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
  available?: boolean
  contentType?: "TEXT" | "IMAGE" | "VIDEO" | "AUDIO"
  description?: string
  icon?: string
  id?: string
  name?: string
  provider?: string
}

export interface ModelListResponse {
  audioModels?: ModelInfo[]
  imageModels?: ModelInfo[]
  videoModels?: ModelInfo[]
}

export interface PageResultAssetDTO {
  current?: number
  records?: AssetDTO[]
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

export interface TaskStatusResponse {
  createdAt?: string
  errorMessage?: string
  progress?: number
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
  APIResponseAuthTokenResponse: APIResponseAuthTokenResponse
  APIResponseCurrentUserResponse: APIResponseCurrentUserResponse
  APIResponseGenerateResponse: APIResponseGenerateResponse
  APIResponseMapStringObject: APIResponseMapStringObject
  APIResponseMaterialUploadResponse: APIResponseMaterialUploadResponse
  APIResponseMiddlewareStatusReport: APIResponseMiddlewareStatusReport
  APIResponseModelListResponse: APIResponseModelListResponse
  APIResponsePageResultAssetDTO: APIResponsePageResultAssetDTO
  APIResponsePageResultGalleryDTO: APIResponsePageResultGalleryDTO
  APIResponsePageResultMaterialDTO: APIResponsePageResultMaterialDTO
  APIResponseString: APIResponseString
  APIResponseTaskStatusResponse: APIResponseTaskStatusResponse
  APIResponseVoid: APIResponseVoid
  AssetDTO: AssetDTO
  AudioParams: AudioParams
  AuthTokenResponse: AuthTokenResponse
  CurrentUserResponse: CurrentUserResponse
  GalleryDTO: GalleryDTO
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
  PageResultAssetDTO: PageResultAssetDTO
  PageResultGalleryDTO: PageResultGalleryDTO
  PageResultMaterialDTO: PageResultMaterialDTO
  RefreshTokenRequest: RefreshTokenRequest
  SaveToGalleryRequest: SaveToGalleryRequest
  TaskStatusResponse: TaskStatusResponse
  VideoParams: VideoParams
}

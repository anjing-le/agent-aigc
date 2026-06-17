/* eslint-disable */
// Generated from OpenAPI JSON. Do not edit manually.
// Run: node scripts/generate-openapi-frontend-types.js <openapi-json-file>

import type * as Schemas from './schemas'

export type OpenApiHttpMethod = 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT'

export interface OpenApiOperationMeta {
  method: OpenApiHttpMethod
  path: string
  operationId: string
}

export const OPENAPI_OPERATIONS = {
  backfillOwnership: {
    method: "POST",
    path: "/api/aigc/ownership/backfill",
    operationId: "backfillOwnership"
  },
  createItem: {
    method: "POST",
    path: "/api/test/items",
    operationId: "createItem"
  },
  deleteAsset: {
    method: "DELETE",
    path: "/api/aigc/assets/{assetId}",
    operationId: "deleteAsset"
  },
  deleteItem: {
    method: "DELETE",
    path: "/api/test/items/{id}",
    operationId: "deleteItem"
  },
  deleteMaterial: {
    method: "DELETE",
    path: "/api/aigc/materials/{materialId}",
    operationId: "deleteMaterial"
  },
  downloadAsset: {
    method: "GET",
    path: "/api/aigc/assets/{assetId}/download",
    operationId: "downloadAsset"
  },
  downloadGalleryAsset: {
    method: "GET",
    path: "/api/aigc/gallery/{assetId}/download",
    operationId: "downloadGalleryAsset"
  },
  downloadMaterial: {
    method: "GET",
    path: "/api/aigc/materials/{materialId}/download",
    operationId: "downloadMaterial"
  },
  favoriteGalleryAsset: {
    method: "POST",
    path: "/api/aigc/gallery/{assetId}/favorite",
    operationId: "favoriteGalleryAsset"
  },
  features: {
    method: "GET",
    path: "/api/test/features",
    operationId: "features"
  },
  generate: {
    method: "POST",
    path: "/api/aigc/generate",
    operationId: "generate"
  },
  getAssetDetail: {
    method: "GET",
    path: "/api/aigc/assets/{assetId}",
    operationId: "getAssetDetail"
  },
  getAssetList: {
    method: "GET",
    path: "/api/aigc/assets",
    operationId: "getAssetList"
  },
  getCurrentUser: {
    method: "GET",
    path: "/api/auth/me",
    operationId: "getCurrentUser"
  },
  getFavoriteGalleryList: {
    method: "GET",
    path: "/api/aigc/gallery/favorites",
    operationId: "getFavoriteGalleryList"
  },
  getGalleryAuditLogs: {
    method: "GET",
    path: "/api/aigc/gallery/audits",
    operationId: "getGalleryAuditLogs"
  },
  getGalleryAuthorProfile: {
    method: "GET",
    path: "/api/aigc/gallery/authors/{authorId}",
    operationId: "getGalleryAuthorProfile"
  },
  getGalleryInteractionReport: {
    method: "GET",
    path: "/api/aigc/gallery/reports/interactions",
    operationId: "getGalleryInteractionReport"
  },
  getGalleryList: {
    method: "GET",
    path: "/api/aigc/gallery",
    operationId: "getGalleryList"
  },
  getGalleryRanking: {
    method: "GET",
    path: "/api/aigc/gallery/ranking",
    operationId: "getGalleryRanking"
  },
  getGalleryShare: {
    method: "GET",
    path: "/api/aigc/gallery/{assetId}/share",
    operationId: "getGalleryShare"
  },
  getItem: {
    method: "GET",
    path: "/api/test/items/{id}",
    operationId: "getItem"
  },
  getMaterialList: {
    method: "GET",
    path: "/api/aigc/materials",
    operationId: "getMaterialList"
  },
  getMaterialTasks: {
    method: "GET",
    path: "/api/aigc/materials/{materialId}/tasks",
    operationId: "getMaterialTasks"
  },
  getModels: {
    method: "GET",
    path: "/api/aigc/models",
    operationId: "getModels"
  },
  getProviderAuditLogs: {
    method: "GET",
    path: "/api/aigc/models/provider-audits",
    operationId: "getProviderAuditLogs"
  },
  getStorageAuditLogs: {
    method: "GET",
    path: "/api/aigc/storage/audits",
    operationId: "getStorageAuditLogs"
  },
  getStorageStatus: {
    method: "GET",
    path: "/api/aigc/storage/status",
    operationId: "getStorageStatus"
  },
  getTaskStatus: {
    method: "GET",
    path: "/api/aigc/task/{taskId}",
    operationId: "getTaskStatus"
  },
  health: {
    method: "GET",
    path: "/api/test/health",
    operationId: "health"
  },
  likeGalleryAsset: {
    method: "POST",
    path: "/api/aigc/gallery/{assetId}/like",
    operationId: "likeGalleryAsset"
  },
  listItems: {
    method: "GET",
    path: "/api/test/items",
    operationId: "listItems"
  },
  login: {
    method: "POST",
    path: "/api/auth/login",
    operationId: "login"
  },
  logout: {
    method: "POST",
    path: "/api/auth/logout",
    operationId: "logout"
  },
  ping: {
    method: "GET",
    path: "/api/test/ping",
    operationId: "ping"
  },
  previewAsset: {
    method: "GET",
    path: "/api/aigc/assets/{assetId}/preview",
    operationId: "previewAsset"
  },
  previewGalleryAsset: {
    method: "GET",
    path: "/api/aigc/gallery/{assetId}/preview",
    operationId: "previewGalleryAsset"
  },
  previewMaterial: {
    method: "GET",
    path: "/api/aigc/materials/{materialId}/preview",
    operationId: "previewMaterial"
  },
  probeModel: {
    method: "POST",
    path: "/api/aigc/models/probe",
    operationId: "probeModel"
  },
  recordGallerySharePromptReuse: {
    method: "POST",
    path: "/api/aigc/gallery/{assetId}/share/reuse",
    operationId: "recordGallerySharePromptReuse"
  },
  refreshToken: {
    method: "POST",
    path: "/api/auth/refresh",
    operationId: "refreshToken"
  },
  removeFromGallery: {
    method: "DELETE",
    path: "/api/aigc/gallery/{assetId}/publication",
    operationId: "removeFromGallery"
  },
  retryTask: {
    method: "POST",
    path: "/api/aigc/task/{taskId}/retry",
    operationId: "retryTask"
  },
  saveToGallery: {
    method: "POST",
    path: "/api/aigc/gallery/save",
    operationId: "saveToGallery"
  },
  smokeTestProvider: {
    method: "POST",
    path: "/api/aigc/models/provider-smoke-test",
    operationId: "smokeTestProvider"
  },
  testBizException: {
    method: "GET",
    path: "/api/test/exception/biz",
    operationId: "testBizException"
  },
  testSystemException: {
    method: "GET",
    path: "/api/test/exception/system",
    operationId: "testSystemException"
  },
  unfavoriteGalleryAsset: {
    method: "DELETE",
    path: "/api/aigc/gallery/{assetId}/favorite",
    operationId: "unfavoriteGalleryAsset"
  },
  unlikeGalleryAsset: {
    method: "DELETE",
    path: "/api/aigc/gallery/{assetId}/like",
    operationId: "unlikeGalleryAsset"
  },
  updateActiveProvider: {
    method: "POST",
    path: "/api/aigc/models/active-provider",
    operationId: "updateActiveProvider"
  },
  updateItem: {
    method: "PUT",
    path: "/api/test/items/{id}",
    operationId: "updateItem"
  },
  updateProviderCredential: {
    method: "POST",
    path: "/api/aigc/models/provider-credential",
    operationId: "updateProviderCredential"
  },
  updateProviderParams: {
    method: "POST",
    path: "/api/aigc/models/provider-params",
    operationId: "updateProviderParams"
  },
  uploadMaterial: {
    method: "POST",
    path: "/api/aigc/materials/upload",
    operationId: "uploadMaterial"
  },
} as const satisfies Record<string, OpenApiOperationMeta>

export type OpenApiOperationId = keyof typeof OPENAPI_OPERATIONS

export interface OpenApiOperationTypes {
  backfillOwnership: {
    pathParams: undefined
    query: undefined
    request: Schemas.OwnershipBackfillRequest
    response: Schemas.APIResponseOwnershipBackfillResponse
    data: NonNullable<Schemas.APIResponseOwnershipBackfillResponse['data']>
  }
  createItem: {
    pathParams: undefined
    query: undefined
    request: Record<string, unknown>
    response: Schemas.APIResponseMapStringObject
    data: NonNullable<Schemas.APIResponseMapStringObject['data']>
  }
  deleteAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  deleteItem: {
    pathParams: { id: number }
    query: undefined
    request: undefined
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  deleteMaterial: {
    pathParams: { materialId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  downloadAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: File
    data: unknown
  }
  downloadGalleryAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: File
    data: unknown
  }
  downloadMaterial: {
    pathParams: { materialId: string }
    query: undefined
    request: undefined
    response: File
    data: unknown
  }
  favoriteGalleryAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseGalleryDTO
    data: NonNullable<Schemas.APIResponseGalleryDTO['data']>
  }
  features: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseMiddlewareStatusReport
    data: NonNullable<Schemas.APIResponseMiddlewareStatusReport['data']>
  }
  generate: {
    pathParams: undefined
    query: undefined
    request: Schemas.GenerateRequest
    response: Schemas.APIResponseGenerateResponse
    data: NonNullable<Schemas.APIResponseGenerateResponse['data']>
  }
  getAssetDetail: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseAssetDetailResponse
    data: NonNullable<Schemas.APIResponseAssetDetailResponse['data']>
  }
  getAssetList: {
    pathParams: undefined
    query: { contentType?: string; current?: number; size?: number }
    request: undefined
    response: Schemas.APIResponsePageResultAssetDTO
    data: NonNullable<Schemas.APIResponsePageResultAssetDTO['data']>
  }
  getCurrentUser: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseCurrentUserResponse
    data: NonNullable<Schemas.APIResponseCurrentUserResponse['data']>
  }
  getFavoriteGalleryList: {
    pathParams: undefined
    query: { current?: number; size?: number }
    request: undefined
    response: Schemas.APIResponsePageResultGalleryDTO
    data: NonNullable<Schemas.APIResponsePageResultGalleryDTO['data']>
  }
  getGalleryAuditLogs: {
    pathParams: undefined
    query: { action?: string; assetId?: string; current?: number; size?: number; success?: boolean }
    request: undefined
    response: Schemas.APIResponsePageResultGalleryAuditLogResponse
    data: NonNullable<Schemas.APIResponsePageResultGalleryAuditLogResponse['data']>
  }
  getGalleryAuthorProfile: {
    pathParams: { authorId: string }
    query: { contentType?: string; current?: number; size?: number }
    request: undefined
    response: Schemas.APIResponseGalleryAuthorProfileResponse
    data: NonNullable<Schemas.APIResponseGalleryAuthorProfileResponse['data']>
  }
  getGalleryInteractionReport: {
    pathParams: undefined
    query: { contentType?: string; days?: number }
    request: undefined
    response: Schemas.APIResponseGalleryInteractionReportResponse
    data: NonNullable<Schemas.APIResponseGalleryInteractionReportResponse['data']>
  }
  getGalleryList: {
    pathParams: undefined
    query: { contentType?: string; current?: number; keyword?: string; model?: string; size?: number }
    request: undefined
    response: Schemas.APIResponsePageResultGalleryDTO
    data: NonNullable<Schemas.APIResponsePageResultGalleryDTO['data']>
  }
  getGalleryRanking: {
    pathParams: undefined
    query: { contentType?: string; current?: number; keyword?: string; model?: string; size?: number }
    request: undefined
    response: Schemas.APIResponsePageResultGalleryDTO
    data: NonNullable<Schemas.APIResponsePageResultGalleryDTO['data']>
  }
  getGalleryShare: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseGalleryShareResponse
    data: NonNullable<Schemas.APIResponseGalleryShareResponse['data']>
  }
  getItem: {
    pathParams: { id: number }
    query: undefined
    request: undefined
    response: Schemas.APIResponseMapStringObject
    data: NonNullable<Schemas.APIResponseMapStringObject['data']>
  }
  getMaterialList: {
    pathParams: undefined
    query: { contentType?: string; current?: number; size?: number }
    request: undefined
    response: Schemas.APIResponsePageResultMaterialDTO
    data: NonNullable<Schemas.APIResponsePageResultMaterialDTO['data']>
  }
  getMaterialTasks: {
    pathParams: { materialId: string }
    query: { current?: number; size?: number }
    request: undefined
    response: Schemas.APIResponsePageResultTaskStatusResponse
    data: NonNullable<Schemas.APIResponsePageResultTaskStatusResponse['data']>
  }
  getModels: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseModelListResponse
    data: NonNullable<Schemas.APIResponseModelListResponse['data']>
  }
  getProviderAuditLogs: {
    pathParams: undefined
    query: { action?: string; contentType?: string; current?: number; size?: number }
    request: undefined
    response: Schemas.APIResponsePageResultProviderAuditLogResponse
    data: NonNullable<Schemas.APIResponsePageResultProviderAuditLogResponse['data']>
  }
  getStorageAuditLogs: {
    pathParams: undefined
    query: { action?: string; backend?: string; current?: number; size?: number; success?: boolean }
    request: undefined
    response: Schemas.APIResponsePageResultStorageAuditLogResponse
    data: NonNullable<Schemas.APIResponsePageResultStorageAuditLogResponse['data']>
  }
  getStorageStatus: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseStorageStatusResponse
    data: NonNullable<Schemas.APIResponseStorageStatusResponse['data']>
  }
  getTaskStatus: {
    pathParams: { taskId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseTaskStatusResponse
    data: NonNullable<Schemas.APIResponseTaskStatusResponse['data']>
  }
  health: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseMapStringObject
    data: NonNullable<Schemas.APIResponseMapStringObject['data']>
  }
  likeGalleryAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseGalleryDTO
    data: NonNullable<Schemas.APIResponseGalleryDTO['data']>
  }
  listItems: {
    pathParams: undefined
    query: { keyword?: string }
    request: undefined
    response: Schemas.APIResponseMapStringObject
    data: NonNullable<Schemas.APIResponseMapStringObject['data']>
  }
  login: {
    pathParams: undefined
    query: undefined
    request: Schemas.LoginRequest
    response: Schemas.APIResponseAuthTokenResponse
    data: NonNullable<Schemas.APIResponseAuthTokenResponse['data']>
  }
  logout: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  ping: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseString
    data: NonNullable<Schemas.APIResponseString['data']>
  }
  previewAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: File
    data: unknown
  }
  previewGalleryAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: File
    data: unknown
  }
  previewMaterial: {
    pathParams: { materialId: string }
    query: undefined
    request: undefined
    response: File
    data: unknown
  }
  probeModel: {
    pathParams: undefined
    query: undefined
    request: Schemas.ProviderProbeRequest
    response: Schemas.APIResponseProviderProbeResponse
    data: NonNullable<Schemas.APIResponseProviderProbeResponse['data']>
  }
  recordGallerySharePromptReuse: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  refreshToken: {
    pathParams: undefined
    query: undefined
    request: Schemas.RefreshTokenRequest
    response: Schemas.APIResponseAuthTokenResponse
    data: NonNullable<Schemas.APIResponseAuthTokenResponse['data']>
  }
  removeFromGallery: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  retryTask: {
    pathParams: { taskId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseGenerateResponse
    data: NonNullable<Schemas.APIResponseGenerateResponse['data']>
  }
  saveToGallery: {
    pathParams: undefined
    query: undefined
    request: Schemas.SaveToGalleryRequest
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  smokeTestProvider: {
    pathParams: undefined
    query: undefined
    request: Schemas.ProviderSmokeTestRequest
    response: Schemas.APIResponseProviderSmokeTestResponse
    data: NonNullable<Schemas.APIResponseProviderSmokeTestResponse['data']>
  }
  testBizException: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  testSystemException: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
  }
  unfavoriteGalleryAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseGalleryDTO
    data: NonNullable<Schemas.APIResponseGalleryDTO['data']>
  }
  unlikeGalleryAsset: {
    pathParams: { assetId: string }
    query: undefined
    request: undefined
    response: Schemas.APIResponseGalleryDTO
    data: NonNullable<Schemas.APIResponseGalleryDTO['data']>
  }
  updateActiveProvider: {
    pathParams: undefined
    query: undefined
    request: Schemas.ProviderRouteUpdateRequest
    response: Schemas.APIResponseProviderRouteUpdateResponse
    data: NonNullable<Schemas.APIResponseProviderRouteUpdateResponse['data']>
  }
  updateItem: {
    pathParams: { id: number }
    query: undefined
    request: Record<string, unknown>
    response: Schemas.APIResponseMapStringObject
    data: NonNullable<Schemas.APIResponseMapStringObject['data']>
  }
  updateProviderCredential: {
    pathParams: undefined
    query: undefined
    request: Schemas.ProviderCredentialUpdateRequest
    response: Schemas.APIResponseProviderCredentialUpdateResponse
    data: NonNullable<Schemas.APIResponseProviderCredentialUpdateResponse['data']>
  }
  updateProviderParams: {
    pathParams: undefined
    query: undefined
    request: Schemas.ProviderParamUpdateRequest
    response: Schemas.APIResponseProviderParamUpdateResponse
    data: NonNullable<Schemas.APIResponseProviderParamUpdateResponse['data']>
  }
  uploadMaterial: {
    pathParams: undefined
    query: undefined
    request: { file: File }
    response: Schemas.APIResponseMaterialUploadResponse
    data: NonNullable<Schemas.APIResponseMaterialUploadResponse['data']>
  }
}

export type OpenApiOperationPathParams<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['pathParams']
export type OpenApiOperationQuery<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['query']
export type OpenApiOperationRequest<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['request']
export type OpenApiOperationResponse<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['response']
export type OpenApiOperationData<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['data']

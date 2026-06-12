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
  getGalleryList: {
    method: "GET",
    path: "/api/aigc/gallery",
    operationId: "getGalleryList"
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
  getModels: {
    method: "GET",
    path: "/api/aigc/models",
    operationId: "getModels"
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
  refreshToken: {
    method: "POST",
    path: "/api/auth/refresh",
    operationId: "refreshToken"
  },
  saveToGallery: {
    method: "POST",
    path: "/api/aigc/gallery/save",
    operationId: "saveToGallery"
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
  updateItem: {
    method: "PUT",
    path: "/api/test/items/{id}",
    operationId: "updateItem"
  },
  uploadMaterial: {
    method: "POST",
    path: "/api/aigc/materials/upload",
    operationId: "uploadMaterial"
  },
} as const satisfies Record<string, OpenApiOperationMeta>

export type OpenApiOperationId = keyof typeof OPENAPI_OPERATIONS

export interface OpenApiOperationTypes {
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
  getGalleryList: {
    pathParams: undefined
    query: { contentType?: string; current?: number; keyword?: string; model?: string; size?: number }
    request: undefined
    response: Schemas.APIResponsePageResultGalleryDTO
    data: NonNullable<Schemas.APIResponsePageResultGalleryDTO['data']>
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
  getModels: {
    pathParams: undefined
    query: undefined
    request: undefined
    response: Schemas.APIResponseModelListResponse
    data: NonNullable<Schemas.APIResponseModelListResponse['data']>
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
  refreshToken: {
    pathParams: undefined
    query: undefined
    request: Schemas.RefreshTokenRequest
    response: Schemas.APIResponseAuthTokenResponse
    data: NonNullable<Schemas.APIResponseAuthTokenResponse['data']>
  }
  saveToGallery: {
    pathParams: undefined
    query: undefined
    request: Schemas.SaveToGalleryRequest
    response: Schemas.APIResponseVoid
    data: NonNullable<Schemas.APIResponseVoid['data']>
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
  updateItem: {
    pathParams: { id: number }
    query: undefined
    request: Record<string, unknown>
    response: Schemas.APIResponseMapStringObject
    data: NonNullable<Schemas.APIResponseMapStringObject['data']>
  }
  uploadMaterial: {
    pathParams: undefined
    query: undefined
    request: { file: string }
    response: Schemas.APIResponseMaterialUploadResponse
    data: NonNullable<Schemas.APIResponseMaterialUploadResponse['data']>
  }
}

export type OpenApiOperationPathParams<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['pathParams']
export type OpenApiOperationQuery<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['query']
export type OpenApiOperationRequest<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['request']
export type OpenApiOperationResponse<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['response']
export type OpenApiOperationData<T extends OpenApiOperationId> = OpenApiOperationTypes[T]['data']

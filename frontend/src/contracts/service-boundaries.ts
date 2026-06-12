/* eslint-disable */
// Generated from contracts/service-boundaries.json. Do not edit manually.
// Run: node scripts/generate-service-boundaries-frontend.js

export const SERVICE_BOUNDARY_CONTRACT = {
  "schemaVersion": 1,
  "applicationId": "agent-aigc",
  "apiPrefix": "/api",
  "boundaries": [
    {
      "id": "auth",
      "name": "Authentication",
      "kind": "runtime",
      "owner": "infra-auth",
      "currentHost": "agent-aigc",
      "basePath": "/api/auth",
      "apiConstantsClass": "Auth",
      "apiPathsKey": "auth",
      "controller": "backend/src/main/java/com/anjing/controller/AuthController.java",
      "openapi": true,
      "copyAction": "replace with real auth center or database-backed auth",
      "routes": [
        {
          "name": "login",
          "backendConstant": "LOGIN_FULL",
          "frontendKey": "login",
          "path": "/api/auth/login",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "logout",
          "backendConstant": "LOGOUT_FULL",
          "frontendKey": "logout",
          "path": "/api/auth/logout",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "currentUser",
          "backendConstant": "ME_FULL",
          "frontendKey": "me",
          "path": "/api/auth/me",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "refreshToken",
          "backendConstant": "REFRESH_FULL",
          "frontendKey": "refresh",
          "path": "/api/auth/refresh",
          "methods": [
            "POST"
          ]
        }
      ]
    },
    {
      "id": "test",
      "name": "Scaffold Test",
      "kind": "sample",
      "owner": "agent-aigc",
      "currentHost": "agent-aigc",
      "basePath": "/api/test",
      "apiConstantsClass": "Test",
      "apiPathsKey": "test",
      "controller": "backend/src/main/java/com/anjing/controller/TestController.java",
      "openapi": true,
      "copyAction": "delete or replace after the copied project has its own health and sample strategy",
      "routes": [
        {
          "name": "health",
          "backendConstant": "HEALTH_FULL",
          "frontendKey": "health",
          "path": "/api/test/health",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "features",
          "backendConstant": "FEATURES_FULL",
          "frontendKey": "features",
          "path": "/api/test/features",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "ping",
          "backendConstant": "PING_FULL",
          "frontendKey": "ping",
          "path": "/api/test/ping",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "bizException",
          "backendConstant": "EXCEPTION_BIZ_FULL",
          "frontendKey": "bizException",
          "path": "/api/test/exception/biz",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "systemException",
          "backendConstant": "EXCEPTION_SYSTEM_FULL",
          "frontendKey": "systemException",
          "path": "/api/test/exception/system",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "items",
          "backendConstant": "ITEMS_FULL",
          "frontendKey": "items",
          "path": "/api/test/items",
          "methods": [
            "GET",
            "POST"
          ]
        },
        {
          "name": "itemDetail",
          "backendConstant": "ITEM_DETAIL_FULL",
          "frontendKey": "itemDetail",
          "path": "/api/test/items/{id}",
          "methods": [
            "GET",
            "PUT",
            "DELETE"
          ]
        }
      ]
    },
    {
      "id": "common",
      "name": "Common Platform",
      "kind": "reserved-runtime",
      "owner": "infra-common",
      "currentHost": "agent-aigc",
      "basePath": "/api/common",
      "apiConstantsClass": "Common",
      "apiPathsKey": "common",
      "openapi": false,
      "copyAction": "keep only endpoints implemented by the copied project",
      "routes": [
        {
          "name": "upload",
          "backendConstant": "UPLOAD_FILE_FULL",
          "frontendKey": "upload",
          "path": "/api/common/upload",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "uploadImage",
          "backendConstant": "UPLOAD_IMAGE_FULL",
          "frontendKey": "uploadImage",
          "path": "/api/common/upload/image",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "uploadWangEditor",
          "backendConstant": "UPLOAD_WANG_EDITOR_FULL",
          "frontendKey": "uploadWangEditor",
          "path": "/api/common/upload/wangeditor",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "download",
          "backendConstant": "DOWNLOAD_FILE_FULL",
          "frontendKey": "download",
          "path": "/api/common/download/{fileId}",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "deleteFile",
          "backendConstant": "DELETE_FILE_FULL",
          "frontendKey": "deleteFile",
          "path": "/api/common/files/{fileId}",
          "methods": [
            "DELETE"
          ]
        }
      ]
    },
    {
      "id": "aigc",
      "name": "AIGC Creation",
      "kind": "runtime",
      "owner": "agent-aigc",
      "currentHost": "agent-aigc",
      "basePath": "/api/aigc",
      "apiConstantsClass": "Aigc",
      "apiPathsKey": "aigc",
      "controller": "backend/src/main/java/com/anjing/aigc/controller/AigcController.java",
      "openapi": true,
      "copyAction": "core business boundary for multimodal generation, assets, gallery, and model metadata",
      "routes": [
        {
          "name": "generate",
          "backendConstant": "GENERATE_FULL",
          "frontendKey": "generate",
          "path": "/api/aigc/generate",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "taskStatus",
          "backendConstant": "TASK_STATUS_FULL",
          "frontendKey": "taskStatus",
          "path": "/api/aigc/task/{taskId}",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "taskRetry",
          "backendConstant": "TASK_RETRY_FULL",
          "frontendKey": "taskRetry",
          "path": "/api/aigc/task/{taskId}/retry",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "models",
          "backendConstant": "MODELS_FULL",
          "frontendKey": "models",
          "path": "/api/aigc/models",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "materials",
          "backendConstant": "MATERIALS_FULL",
          "frontendKey": "materials",
          "path": "/api/aigc/materials",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "materialDetail",
          "backendConstant": "MATERIAL_DETAIL_FULL",
          "frontendKey": "materialDetail",
          "path": "/api/aigc/materials/{materialId}",
          "methods": [
            "DELETE"
          ]
        },
        {
          "name": "materialTasks",
          "backendConstant": "MATERIAL_TASKS_FULL",
          "frontendKey": "materialTasks",
          "path": "/api/aigc/materials/{materialId}/tasks",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "materialUpload",
          "backendConstant": "MATERIAL_UPLOAD_FULL",
          "frontendKey": "materialUpload",
          "path": "/api/aigc/materials/upload",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "gallery",
          "backendConstant": "GALLERY_FULL",
          "frontendKey": "gallery",
          "path": "/api/aigc/gallery",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "gallerySave",
          "backendConstant": "GALLERY_SAVE_FULL",
          "frontendKey": "gallerySave",
          "path": "/api/aigc/gallery/save",
          "methods": [
            "POST"
          ]
        },
        {
          "name": "assets",
          "backendConstant": "ASSETS_FULL",
          "frontendKey": "assets",
          "path": "/api/aigc/assets",
          "methods": [
            "GET"
          ]
        },
        {
          "name": "assetDetail",
          "backendConstant": "ASSET_DETAIL_FULL",
          "frontendKey": "assetDetail",
          "path": "/api/aigc/assets/{assetId}",
          "methods": [
            "DELETE"
          ]
        }
      ]
    },
    {
      "id": "user",
      "name": "User Management",
      "kind": "reserved",
      "owner": "infra-auth",
      "currentHost": "future-service",
      "basePath": "/api/users",
      "apiConstantsClass": "User",
      "openapi": false,
      "copyAction": "use when the copied project implements real user management",
      "routes": []
    },
    {
      "id": "admin",
      "name": "Admin Operations",
      "kind": "reserved",
      "owner": "infra-admin",
      "currentHost": "future-service",
      "basePath": "/api/admin",
      "apiConstantsClass": "Admin",
      "openapi": false,
      "copyAction": "use for operations dashboards, logs, and platform admin APIs",
      "routes": []
    },
    {
      "id": "integration",
      "name": "External Integration",
      "kind": "reserved",
      "owner": "infra-integration",
      "currentHost": "future-service",
      "basePath": "/api/integration",
      "apiConstantsClass": "Integration",
      "openapi": false,
      "copyAction": "use for OSS, payment, notification, LLM provider, and other external adapters",
      "routes": []
    }
  ]
} as const

export const APPLICATION_ID = SERVICE_BOUNDARY_CONTRACT.applicationId
export const SERVICE_BOUNDARY_BASE_PATHS = {
  "admin": "/api/admin",
  "aigc": "/api/aigc",
  "auth": "/api/auth",
  "common": "/api/common",
  "integration": "/api/integration",
  "test": "/api/test",
  "user": "/api/users"
} as const
export const SERVICE_BOUNDARY_ROUTE_PATHS = {
  "aigc": {
    "assetDetail": "/api/aigc/assets/{assetId}",
    "assets": "/api/aigc/assets",
    "gallery": "/api/aigc/gallery",
    "gallerySave": "/api/aigc/gallery/save",
    "generate": "/api/aigc/generate",
    "materialDetail": "/api/aigc/materials/{materialId}",
    "materials": "/api/aigc/materials",
    "materialTasks": "/api/aigc/materials/{materialId}/tasks",
    "materialUpload": "/api/aigc/materials/upload",
    "models": "/api/aigc/models",
    "taskRetry": "/api/aigc/task/{taskId}/retry",
    "taskStatus": "/api/aigc/task/{taskId}"
  },
  "auth": {
    "login": "/api/auth/login",
    "logout": "/api/auth/logout",
    "me": "/api/auth/me",
    "refresh": "/api/auth/refresh"
  },
  "common": {
    "deleteFile": "/api/common/files/{fileId}",
    "download": "/api/common/download/{fileId}",
    "upload": "/api/common/upload",
    "uploadImage": "/api/common/upload/image",
    "uploadWangEditor": "/api/common/upload/wangeditor"
  },
  "test": {
    "bizException": "/api/test/exception/biz",
    "features": "/api/test/features",
    "health": "/api/test/health",
    "itemDetail": "/api/test/items/{id}",
    "items": "/api/test/items",
    "ping": "/api/test/ping",
    "systemException": "/api/test/exception/system"
  }
} as const

export type ServiceBoundaryContract = typeof SERVICE_BOUNDARY_CONTRACT
export type ServiceBoundaryId = ServiceBoundaryContract['boundaries'][number]['id']
export type ServiceBoundaryPathKey = keyof typeof SERVICE_BOUNDARY_BASE_PATHS

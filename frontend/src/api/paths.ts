/**
 * API path registry.
 *
 * Keep endpoint strings here so API modules and generated code reuse one contract.
 */

import { SERVICE_BOUNDARY_ROUTE_PATHS } from '@/contracts/service-boundaries'

const encodePathValue = (value: string | number): string => encodeURIComponent(String(value))

const bindApiPathParams = (apiPath: string, params: Record<string, string | number>): string => {
  return Object.entries(params).reduce((path, [name, value]) => {
    return path.replace(`{${name}}`, encodePathValue(value))
  }, apiPath)
}

const normalizeApiBase = (baseUrl: string | undefined): string => {
  const base = (baseUrl || '').trim()
  if (!base || base === '/') return ''
  return base.replace(/\/+$/, '')
}

export const resolveApiPath = (apiPath: string): string => {
  const path = apiPath.startsWith('/') ? apiPath : `/${apiPath}`
  const base = normalizeApiBase(import.meta.env.VITE_API_URL)
  return base ? `${base}${path}` : path
}

export const ApiPaths = {
  auth: {
    login: SERVICE_BOUNDARY_ROUTE_PATHS.auth.login,
    logout: SERVICE_BOUNDARY_ROUTE_PATHS.auth.logout,
    me: SERVICE_BOUNDARY_ROUTE_PATHS.auth.me,
    refresh: SERVICE_BOUNDARY_ROUTE_PATHS.auth.refresh
  },
  test: {
    health: SERVICE_BOUNDARY_ROUTE_PATHS.test.health,
    features: SERVICE_BOUNDARY_ROUTE_PATHS.test.features,
    ping: SERVICE_BOUNDARY_ROUTE_PATHS.test.ping,
    bizException: SERVICE_BOUNDARY_ROUTE_PATHS.test.bizException,
    systemException: SERVICE_BOUNDARY_ROUTE_PATHS.test.systemException,
    items: SERVICE_BOUNDARY_ROUTE_PATHS.test.items,
    itemDetail: (id: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.test.itemDetail, { id })
  },
  common: {
    upload: SERVICE_BOUNDARY_ROUTE_PATHS.common.upload,
    uploadImage: SERVICE_BOUNDARY_ROUTE_PATHS.common.uploadImage,
    uploadWangEditor: SERVICE_BOUNDARY_ROUTE_PATHS.common.uploadWangEditor,
    download: (fileId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.common.download, { fileId }),
    deleteFile: (fileId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.common.deleteFile, { fileId })
  },
  aigc: {
    generate: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.generate,
    taskStatus: (taskId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.taskStatus, { taskId }),
    taskRetry: (taskId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.taskRetry, { taskId }),
    models: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.models,
    modelProbe: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProbe,
    modelActiveProvider: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelActiveProvider,
    modelProviderCredential: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderCredential,
    modelProviderParams: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderParams,
    modelProviderSmokeTest: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderSmokeTest,
    modelProviderAudits: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderAudits,
    modelProviderExecutionReport: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.modelProviderExecutionReport,
    materials: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.materials,
    materialDetail: (materialId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.materialDetail, { materialId }),
    materialTasks: (materialId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.materialTasks, { materialId }),
    materialPreview: (materialId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.materialPreview, { materialId }),
    materialDownload: (materialId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.materialDownload, { materialId }),
    materialUpload: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.materialUpload,
    storageStatus: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.storageStatus,
    storageAudits: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.storageAudits,
    ownershipBackfill: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.ownershipBackfill,
    gallery: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.gallery,
    galleryRanking: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryRanking,
    galleryCollections: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryCollections,
    galleryTopics: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryTopics,
    galleryCreatorRanking: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryCreatorRanking,
    galleryCurationRules: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryCurationRules,
    galleryAudits: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryAudits,
    galleryInteractionReport: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryInteractionReport,
    galleryFavorites: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryFavorites,
    gallerySave: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.gallerySave,
    galleryPublication: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryPublication, { assetId }),
    galleryLike: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryLike, { assetId }),
    galleryFavorite: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryFavorite, { assetId }),
    galleryShare: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryShare, { assetId }),
    galleryShareReuse: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryShareReuse, { assetId }),
    galleryAuthorProfile: (authorId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryAuthorProfile, { authorId }),
    galleryAssetPreview: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryAssetPreview, { assetId }),
    galleryAssetDownload: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.galleryAssetDownload, { assetId }),
    assets: SERVICE_BOUNDARY_ROUTE_PATHS.aigc.assets,
    assetDetail: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.assetDetail, { assetId }),
    assetPreview: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.assetPreview, { assetId }),
    assetDownload: (assetId: string | number) =>
      bindApiPathParams(SERVICE_BOUNDARY_ROUTE_PATHS.aigc.assetDownload, { assetId })
  }
} as const

/**
 * Legacy template endpoints kept only for old mock/system pages.
 *
 * New runtime API code must use ApiPaths and the service-boundary manifest.
 */
export const ApiLegacyPaths = {
  auth: {
    verify: '/api/auth/verify',
    verify2FA: '/auth/login/verify-2fa',
    sendOtp: '/auth/otp/send',
    binding: '/auth/binding',
    bindStore: (storeNo: string) => `/auth/binding/${encodePathValue(storeNo)}`,
    tenantMembers: '/auth/tenant/account/list',
    userInfo: '/auth/user/info',
    register: '/auth/register',
    updatePassword: '/auth/user/password',
    userBasic: '/auth/user/basic',
    avatarUpload: '/auth/user/avatar'
  },
  system: {
    users: '/api/user/list',
    roles: '/api/role/list',
    simpleMenus: '/api/v3/system/menus/simple'
  }
} as const

export type ApiPaths = typeof ApiPaths
export type ApiLegacyPaths = typeof ApiLegacyPaths

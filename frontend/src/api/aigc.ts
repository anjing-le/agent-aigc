import { openApiRequest } from './openapiClient'
import type {
  GenerateRequest,
  GenerateResponse,
  TaskStatusResponse,
  GalleryItem,
  GalleryAuthorProfileResponse,
  GalleryListResponse,
  GalleryShareResponse,
  GalleryAuditLogListResponse,
  GalleryAuditLogSearchParams,
  AssetListResponse,
  GallerySearchParams,
  AssetSearchParams,
  AssetDetailResponse,
  MaterialListResponse,
  MaterialTaskListResponse,
  MaterialTaskSearchParams,
  MaterialSearchParams,
  ModelListResponse,
  ProviderProbeRequest,
  ProviderCredentialUpdateRequest,
  ProviderCredentialUpdateResponse,
  ProviderParamUpdateRequest,
  ProviderParamUpdateResponse,
  ProviderAuditLogListResponse,
  ProviderAuditLogSearchParams,
  ProviderRouteUpdateRequest,
  ProviderRouteUpdateResponse,
  ProviderSmokeTestRequest,
  ProviderSmokeTestResponse,
  StorageAuditLogListResponse,
  StorageAuditLogSearchParams,
  StorageStatusResponse,
  OwnershipBackfillRequest,
  OwnershipBackfillResponse,
  MaterialUploadResponse
} from './model/aigcModel'

/**
 * AIGC 生成接口 - 智能路由Agent入口
 *
 * 设计理念：用户只需描述需求，系统自动处理一切
 * - 用户提供：需求描述 + 可选素材
 * - Agent自动：意图识别 → 模型选择 → 提示词优化 → 生成内容
 *
 * @param data 生成请求参数（prompt + 可选referenceImages）
 * @returns 包含taskId和Agent分析结果的响应
 */
export function fetchGenerate(data: GenerateRequest) {
  return openApiRequest('generate', {
    body: data
  }) as Promise<GenerateResponse>
}

/**
 * 查询任务状态（异步轮询）
 * @param taskId 任务ID
 * @returns 任务状态（包含进度、Agent决策、生成结果）
 */
export function fetchGetTaskStatus(taskId: string) {
  return openApiRequest('getTaskStatus', {
    pathParams: { taskId }
  }) as Promise<TaskStatusResponse>
}

/**
 * 基于历史任务重新创建生成任务
 * @param taskId 原任务ID
 */
export function fetchRetryTask(taskId: string) {
  return openApiRequest('retryTask', {
    pathParams: { taskId }
  }) as Promise<GenerateResponse>
}

/**
 * 获取灵感广场作品列表
 * @param params 搜索参数
 */
export function fetchGetGalleryList(params: GallerySearchParams) {
  return openApiRequest('getGalleryList', {
    query: params
  }) as Promise<GalleryListResponse>
}

/**
 * 获取灵感广场发布和互动审计日志
 * @param params 查询参数
 */
export function fetchGetGalleryAuditLogs(params: GalleryAuditLogSearchParams) {
  return openApiRequest('getGalleryAuditLogs', {
    query: params
  }) as Promise<GalleryAuditLogListResponse>
}

/**
 * 获取当前用户/会话收藏的灵感广场作品
 * @param params 分页参数
 */
export function fetchGetFavoriteGalleryList(params: Pick<GallerySearchParams, 'current' | 'size'>) {
  return openApiRequest('getFavoriteGalleryList', {
    query: params
  }) as Promise<GalleryListResponse>
}

/**
 * 获取公开分享页作品详情
 * @param assetId 资产ID
 */
export function fetchGetGalleryShare(assetId: string) {
  return openApiRequest('getGalleryShare', {
    pathParams: { assetId }
  }) as Promise<GalleryShareResponse>
}

/**
 * 获取灵感广场公开作者主页
 * @param authorId 作者公开标识
 * @param params 分页和筛选参数
 */
export function fetchGetGalleryAuthorProfile(
  authorId: string,
  params: Pick<GallerySearchParams, 'current' | 'size' | 'contentType'>
) {
  return openApiRequest('getGalleryAuthorProfile', {
    pathParams: { authorId },
    query: params
  }) as Promise<GalleryAuthorProfileResponse>
}

/**
 * 获取我的资产列表
 * @param params 搜索参数
 */
export function fetchGetAssetList(params: AssetSearchParams) {
  return openApiRequest('getAssetList', {
    query: params
  }) as Promise<AssetListResponse>
}

/**
 * 获取资产详情和来源任务
 * @param assetId 资产ID
 */
export function fetchGetAssetDetail(assetId: string) {
  return openApiRequest('getAssetDetail', {
    pathParams: { assetId }
  }) as Promise<AssetDetailResponse>
}

/**
 * 获取参考素材列表
 * @param params 搜索参数
 */
export function fetchGetMaterialList(params: MaterialSearchParams) {
  return openApiRequest('getMaterialList', {
    query: params
  }) as Promise<MaterialListResponse>
}

// 注：模型选择由Agent自动处理，用户无需关心
// 模型列表由后端 ProviderRouter 派生，供创作页和管理后台展示可用能力。
export function fetchGetModelList() {
  return openApiRequest('getModels') as Promise<ModelListResponse>
}

export function fetchProbeProvider(data: ProviderProbeRequest) {
  return openApiRequest('probeModel', {
    body: data
  })
}

export function fetchUpdateActiveProvider(data: ProviderRouteUpdateRequest) {
  return openApiRequest('updateActiveProvider', {
    body: data
  }) as Promise<ProviderRouteUpdateResponse>
}

export function fetchUpdateProviderCredential(data: ProviderCredentialUpdateRequest) {
  return openApiRequest('updateProviderCredential', {
    body: data
  }) as Promise<ProviderCredentialUpdateResponse>
}

export function fetchUpdateProviderParams(data: ProviderParamUpdateRequest) {
  return openApiRequest('updateProviderParams', {
    body: data
  }) as Promise<ProviderParamUpdateResponse>
}

export function fetchGetProviderAuditLogs(params: ProviderAuditLogSearchParams) {
  return openApiRequest('getProviderAuditLogs', {
    query: params
  }) as Promise<ProviderAuditLogListResponse>
}

export function fetchSmokeTestProvider(data: ProviderSmokeTestRequest) {
  return openApiRequest('smokeTestProvider', {
    body: data
  }) as Promise<ProviderSmokeTestResponse>
}

export function fetchUploadMaterial(file: File) {
  return openApiRequest('uploadMaterial', {
    body: { file }
  }) as Promise<MaterialUploadResponse>
}

export function fetchGetStorageStatus() {
  return openApiRequest('getStorageStatus') as Promise<StorageStatusResponse>
}

export function fetchGetStorageAuditLogs(params: StorageAuditLogSearchParams) {
  return openApiRequest('getStorageAuditLogs', {
    query: params
  }) as Promise<StorageAuditLogListResponse>
}

export function fetchBackfillOwnership(data: OwnershipBackfillRequest) {
  return openApiRequest('backfillOwnership', {
    body: data
  }) as Promise<OwnershipBackfillResponse>
}

/**
 * 删除参考素材
 * @param materialId 素材ID
 */
export function fetchDeleteMaterial(materialId: string) {
  return openApiRequest('deleteMaterial', {
    pathParams: { materialId }
  }) as unknown as Promise<void>
}

/**
 * 按素材反查引用它的任务
 * @param materialId 素材ID
 * @param params 分页参数
 */
export function fetchGetMaterialTasks(
  materialId: string,
  params: Partial<MaterialTaskSearchParams> = {}
) {
  return openApiRequest('getMaterialTasks', {
    pathParams: { materialId },
    query: params
  }) as Promise<MaterialTaskListResponse>
}

/**
 * 保存作品到灵感广场
 * @param assetId 资产ID
 */
export function fetchSaveToGallery(assetId: string) {
  return openApiRequest('saveToGallery', {
    body: { assetId }
  }) as unknown as Promise<void>
}

/**
 * 从灵感广场撤回作品
 * @param assetId 资产ID
 */
export function fetchRemoveFromGallery(assetId: string) {
  return openApiRequest('removeFromGallery', {
    pathParams: { assetId }
  }) as unknown as Promise<void>
}

/**
 * 点赞灵感广场作品
 * @param assetId 资产ID
 */
export function fetchLikeGalleryAsset(assetId: string) {
  return openApiRequest('likeGalleryAsset', {
    pathParams: { assetId }
  }) as Promise<GalleryItem>
}

/**
 * 取消点赞灵感广场作品
 * @param assetId 资产ID
 */
export function fetchUnlikeGalleryAsset(assetId: string) {
  return openApiRequest('unlikeGalleryAsset', {
    pathParams: { assetId }
  }) as Promise<GalleryItem>
}

/**
 * 收藏灵感广场作品
 * @param assetId 资产ID
 */
export function fetchFavoriteGalleryAsset(assetId: string) {
  return openApiRequest('favoriteGalleryAsset', {
    pathParams: { assetId }
  }) as Promise<GalleryItem>
}

/**
 * 取消收藏灵感广场作品
 * @param assetId 资产ID
 */
export function fetchUnfavoriteGalleryAsset(assetId: string) {
  return openApiRequest('unfavoriteGalleryAsset', {
    pathParams: { assetId }
  }) as Promise<GalleryItem>
}

/**
 * 删除资产
 * @param assetId 资产ID
 */
export function fetchDeleteAsset(assetId: string) {
  return openApiRequest('deleteAsset', {
    pathParams: { assetId }
  }) as unknown as Promise<void>
}

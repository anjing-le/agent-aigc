import request from '@/utils/http'
import { ApiPaths } from './paths'
import type {
  GenerateRequest,
  GenerateResponse,
  TaskStatusResponse,
  GalleryListResponse,
  AssetListResponse,
  GallerySearchParams,
  AssetSearchParams,
  MaterialListResponse,
  MaterialTaskListResponse,
  MaterialTaskSearchParams,
  MaterialSearchParams,
  ModelListResponse,
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
  return request.post<GenerateResponse>({
    url: ApiPaths.aigc.generate,
    data
  })
}

/**
 * 查询任务状态（异步轮询）
 * @param taskId 任务ID
 * @returns 任务状态（包含进度、Agent决策、生成结果）
 */
export function fetchGetTaskStatus(taskId: string) {
  return request.get<TaskStatusResponse>({
    url: ApiPaths.aigc.taskStatus(taskId)
  })
}

/**
 * 获取灵感广场作品列表
 * @param params 搜索参数
 */
export function fetchGetGalleryList(params: GallerySearchParams) {
  return request.get<GalleryListResponse>({
    url: ApiPaths.aigc.gallery,
    params
  })
}

/**
 * 获取我的资产列表
 * @param params 搜索参数
 */
export function fetchGetAssetList(params: AssetSearchParams) {
  return request.get<AssetListResponse>({
    url: ApiPaths.aigc.assets,
    params
  })
}

/**
 * 获取参考素材列表
 * @param params 搜索参数
 */
export function fetchGetMaterialList(params: MaterialSearchParams) {
  return request.get<MaterialListResponse>({
    url: ApiPaths.aigc.materials,
    params
  })
}

// 注：模型选择由Agent自动处理，用户无需关心
// 模型列表由后端 ProviderRouter 派生，供创作页和管理后台展示可用能力。
export function fetchGetModelList() {
  return request.get<ModelListResponse>({
    url: ApiPaths.aigc.models
  })
}

export function fetchUploadMaterial(file: File) {
  const data = new FormData()
  data.append('file', file)
  return request.post<MaterialUploadResponse>({
    url: ApiPaths.aigc.materialUpload,
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 删除参考素材
 * @param materialId 素材ID
 */
export function fetchDeleteMaterial(materialId: string) {
  return request.del<void>({
    url: ApiPaths.aigc.materialDetail(materialId)
  })
}

/**
 * 按素材反查引用它的任务
 * @param materialId 素材ID
 * @param params 分页参数
 */
export function fetchGetMaterialTasks(materialId: string, params: Partial<MaterialTaskSearchParams> = {}) {
  return request.get<MaterialTaskListResponse>({
    url: ApiPaths.aigc.materialTasks(materialId),
    params
  })
}

/**
 * 保存作品到灵感广场
 * @param assetId 资产ID
 */
export function fetchSaveToGallery(assetId: string) {
  return request.post<void>({
    url: ApiPaths.aigc.gallerySave,
    data: { assetId }
  })
}

/**
 * 删除资产
 * @param assetId 资产ID
 */
export function fetchDeleteAsset(assetId: string) {
  return request.del<void>({
    url: ApiPaths.aigc.assetDetail(assetId)
  })
}

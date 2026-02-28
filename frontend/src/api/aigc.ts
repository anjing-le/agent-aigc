import request from '@/utils/http'
import type {
  GenerateRequest,
  GenerateResponse,
  TaskStatusResponse,
  GalleryListResponse,
  AssetListResponse,
  GallerySearchParams,
  AssetSearchParams
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
    url: '/api/aigc/generate',
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
    url: `/api/aigc/task/${taskId}`
  })
}

/**
 * 获取灵感广场作品列表
 * @param params 搜索参数
 */
export function fetchGetGalleryList(params: GallerySearchParams) {
  return request.get<GalleryListResponse>({
    url: '/api/aigc/gallery',
    params
  })
}

/**
 * 获取我的资产列表
 * @param params 搜索参数
 */
export function fetchGetAssetList(params: AssetSearchParams) {
  return request.get<AssetListResponse>({
    url: '/api/aigc/assets',
    params
  })
}

// 注：模型选择由Agent自动处理，用户无需关心
// fetchGetModelList 接口保留供管理后台使用

/**
 * 保存作品到灵感广场
 * @param assetId 资产ID
 */
export function fetchSaveToGallery(assetId: string) {
  return request.post<void>({
    url: '/api/aigc/gallery/save',
    data: { assetId }
  })
}

/**
 * 删除资产
 * @param assetId 资产ID
 */
export function fetchDeleteAsset(assetId: string) {
  return request.del<void>({
    url: `/api/aigc/assets/${assetId}`
  })
}


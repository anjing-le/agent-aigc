import type { PaginatedResponse, CommonSearchParams } from '@/types/common/response'

/** ==================== 生成相关类型 ==================== */

/** 内容类型 */
export type ContentType = 'image' | 'video' | 'audio'

/** 任务状态 */
export type TaskStatus = 'pending' | 'processing' | 'completed' | 'failed'

/**
 * 生成请求参数
 * 
 * 设计理念：用户只需描述需求，系统自动处理一切
 * - 不需要选择模型（Agent自动选择最优模型）
 * - 不需要调参（Agent自动优化参数）
 * - 不需要关心技术细节
 */
export interface GenerateRequest {
  /** 用户输入的需求描述（必填）*/
  prompt: string
  /** 参考素材URL列表（可选，支持图片/视频，用于图生图、图生视频等场景） */
  referenceImages?: string[]
}

/** 生成响应 */
export interface GenerateResponse {
  /** 任务ID */
  taskId: string
  /** 任务状态 */
  status: TaskStatus
  /** Agent分析结果 */
  agentAnalysis?: {
    /** 识别的意图 */
    intent: string
    /** 选择的内容类型 */
    contentType: ContentType
    /** 选择的模型 */
    selectedModel: string
    /** 优化后的提示词 */
    optimizedPrompt: string
  }
  /** 预估完成时间（秒） */
  estimatedTime?: number
}

/** Agent分析结果 */
export interface AgentAnalysis {
  /** 识别的意图 */
  intent: string
  /** 选择的内容类型 */
  contentType: ContentType
  /** 选择的模型 */
  selectedModel: string
  /** 优化后的提示词 */
  optimizedPrompt: string
}

/** 任务状态响应 */
export interface TaskStatusResponse {
  /** 任务ID */
  taskId: string
  /** 任务状态 */
  status: TaskStatus
  /** 进度百分比 0-100 */
  progress: number
  /** Agent分析结果 */
  agentAnalysis?: AgentAnalysis
  /** 生成结果（完成时返回） */
  result?: GenerationResult
  /** 错误信息（失败时返回） */
  errorMessage?: string
  /** 创建时间 */
  createdAt: string
  /** 更新时间 */
  updatedAt: string
}

/** 生成结果 */
export interface GenerationResult {
  /** 资产ID */
  assetId: string
  /** 内容类型 */
  contentType: ContentType
  /** 资源URL */
  url: string
  /** 缩略图URL */
  thumbnailUrl?: string
  /** 使用的提示词 */
  prompt: string
  /** 使用的模型 */
  model: string
  /** 元数据 */
  metadata?: Record<string, any>
}

/** ==================== 作品/资产相关类型 ==================== */

/** 作品/资产项 */
export interface AssetItem {
  /** 资产ID */
  id: string
  /** 内容类型 */
  contentType: ContentType
  /** 资源URL */
  url: string
  /** 缩略图URL */
  thumbnailUrl?: string
  /** 提示词 */
  prompt: string
  /** 使用的模型 */
  model: string
  /** 是否已发布到广场 */
  isPublished: boolean
  /** 创建时间 */
  createdAt: string
}

/** 灵感广场作品项（提示词工具库风格） */
export interface GalleryItem extends AssetItem {
  /** 标题 */
  title?: string
  /** 作者名称 */
  author?: string
  /** 分类标签 */
  category?: string
  /** 点赞数 */
  likeCount: number
}

/** 灵感广场搜索参数 */
export interface GallerySearchParams extends CommonSearchParams {
  /** 内容类型筛选 */
  contentType?: ContentType
  /** 分类筛选 */
  category?: string
  /** 关键词搜索 */
  keyword?: string
}

/** 资产搜索参数 */
export interface AssetSearchParams extends CommonSearchParams {
  /** 内容类型筛选 */
  contentType?: ContentType
}

/** 灵感广场列表响应 */
export type GalleryListResponse = PaginatedResponse<GalleryItem>

/** 资产列表响应 */
export type AssetListResponse = PaginatedResponse<AssetItem>

/** ==================== 模型相关类型 ==================== */

/** 模型信息 */
export interface ModelInfo {
  /** 模型ID */
  id: string
  /** 模型名称 */
  name: string
  /** 模型描述 */
  description: string
  /** 支持的内容类型 */
  contentType: ContentType
  /** 模型提供商 */
  provider: string
  /** 是否可用 */
  available: boolean
  /** 模型图标 */
  icon?: string
}

/** 模型列表响应 */
export interface ModelListResponse {
  /** 图片生成模型 */
  imageModels: ModelInfo[]
  /** 视频生成模型 */
  videoModels: ModelInfo[]
  /** 音频生成模型（预留） */
  audioModels: ModelInfo[]
}


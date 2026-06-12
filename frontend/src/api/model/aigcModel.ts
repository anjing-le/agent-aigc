import type { PaginatedResponse, CommonSearchParams } from '@/types/common/response'

/** ==================== 生成相关类型 ==================== */

/** 内容类型 */
export type ContentType = 'IMAGE' | 'VIDEO' | 'AUDIO'

/** 任务状态 */
export type TaskStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

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
  /** 内容类型提示；为空时由 Agent 自动识别 */
  contentTypeHint?: ContentType
  /** 生成参数；用于宽高比、尺寸、时长、音色等轻量配置 */
  generationParams?: Record<string, string | number | boolean>
  /** 参考素材URL列表（可选，支持图片/视频，用于图生图、图生视频等场景） */
  referenceImages?: string[]
  /** 参考素材ID列表（可选，用于任务历史追踪素材来源） */
  referenceMaterialIds?: string[]
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
    /** 原始提示词 */
    originalPrompt?: string
    /** 清洗后的提示词 */
    cleanPrompt?: string
    /** 优化后的提示词 */
    optimizedPrompt: string
    /** 完整意图分析 */
    analyzedIntent?: Record<string, any>
    /** 置信度 */
    confidence?: number
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
  /** 原始提示词 */
  originalPrompt?: string
  /** 清洗后的提示词 */
  cleanPrompt?: string
  /** 优化后的提示词 */
  optimizedPrompt: string
  /** 完整意图分析 */
  analyzedIntent?: Record<string, any>
  /** 置信度 */
  confidence?: number
}

/** Provider 调用观测摘要 */
export interface ProviderExecutionSummary {
  /** 实际调用的 Provider 名称 */
  providerName?: string
  /** Provider 类型 */
  providerType?: string
  /** 实际使用的模型 */
  model?: string
  /** 任务耗时（毫秒） */
  durationMs?: number
  /** 成本统计状态 */
  costStatus?: 'PENDING' | 'MOCK_FREE' | 'UNTRACKED' | string
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
  /** Provider 调用观测摘要 */
  providerExecution?: ProviderExecutionSummary
  /** 参考素材ID列表 */
  referenceMaterialIds?: string[]
  /** 参考素材详情 */
  referenceMaterials?: MaterialItem[]
  /** 生成结果（完成时返回） */
  result?: GenerationResult
  /** 错误信息（失败时返回） */
  errorMessage?: string
  /** 错误码（失败时返回） */
  errorCode?: string
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

/** 资产详情响应 */
export interface AssetDetailResponse {
  /** 资产信息 */
  asset: AssetItem
  /** 来源任务；老数据可能为空 */
  task?: TaskStatusResponse | null
}

/** 参考素材项 */
export interface MaterialItem {
  /** 素材ID */
  id: string
  /** 可访问 URL */
  url: string
  /** 保存后的文件名 */
  fileName: string
  /** 原始文件名 */
  originalFileName?: string
  /** MIME 类型 */
  contentType: string
  /** 文件大小 */
  size: number
  /** 创建时间 */
  createdAt: string
}

/** 素材搜索参数 */
export interface MaterialSearchParams extends CommonSearchParams {
  /** image 或 video */
  contentType?: 'image' | 'video'
}

/** 素材列表响应 */
export type MaterialListResponse = PaginatedResponse<MaterialItem>

/** 素材引用任务搜索参数 */
export type MaterialTaskSearchParams = CommonSearchParams

/** 素材引用任务列表响应 */
export type MaterialTaskListResponse = PaginatedResponse<TaskStatusResponse>

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

/** 素材上传响应 */
export interface MaterialUploadResponse {
  /** 素材ID */
  materialId?: string
  /** 可访问 URL */
  url: string
  /** 保存后的文件名 */
  fileName: string
  /** 原始文件名 */
  originalFileName?: string
  /** MIME 类型 */
  contentType: string
  /** 文件大小 */
  size: number
  /** 创建时间 */
  createdAt?: string
}

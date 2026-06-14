import type {
  OpenApiOperationData,
  OpenApiOperationQuery,
  OpenApiOperationRequest
} from '@/contracts/openapi/operations'
import type * as Schemas from '@/contracts/openapi/schemas'

type RequiredKeys<T, K extends keyof T> = Omit<T, K> & {
  [P in K]-?: NonNullable<T[P]>
}

type PageResult<TPage, TRecord> = Omit<TPage, 'records' | 'current' | 'size' | 'total'> & {
  records: TRecord[]
  current: number
  size: number
  total: number
}

type GenerationParams = Record<string, string | number | boolean>
type AigcContentType = NonNullable<Schemas.GenerationResult['contentType']>

/** ==================== 生成相关类型 ==================== */

/** 内容类型 */
export type ContentType = Extract<AigcContentType, 'IMAGE' | 'VIDEO' | 'AUDIO'>

/** 任务状态 */
export type TaskStatus = NonNullable<Schemas.TaskStatusResponse['status']>

/**
 * 生成请求参数
 *
 * 设计理念：用户只需描述需求，系统自动处理一切
 * - 不需要选择模型（Agent自动选择最优模型）
 * - 不需要调参（Agent自动优化参数）
 * - 不需要关心技术细节
 */
export type GenerateRequest = Omit<
  OpenApiOperationRequest<'generate'>,
  'contentTypeHint' | 'generationParams'
> & {
  /** 用户输入的需求描述（必填）*/
  prompt: string
  /** 内容类型提示；为空时由 Agent 自动识别 */
  contentTypeHint?: ContentType
  /** 生成参数；用于宽高比、尺寸、时长、音色等轻量配置 */
  generationParams?: GenerationParams
}

/** 生成响应 */
export type GenerateResponse = Omit<
  OpenApiOperationData<'generate'>,
  'taskId' | 'status' | 'agentAnalysis'
> & {
  /** 任务ID */
  taskId: string
  /** 任务状态 */
  status: TaskStatus
  /** Agent分析结果 */
  agentAnalysis?: AgentAnalysis
  /** 预估完成时间（秒） */
  estimatedTime?: number
}

/** Agent分析结果 */
export type AgentAnalysis = Omit<
  Schemas.AgentAnalysis,
  'analyzedIntent' | 'contentType' | 'intent' | 'optimizedPrompt' | 'selectedModel'
> & {
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
  analyzedIntent?: Schemas.AnalyzedIntent
}

/** Provider 调用观测摘要 */
export type ProviderExecutionSummary = Schemas.ProviderExecutionSummary

/** 任务状态响应 */
export type TaskStatusResponse = Omit<
  RequiredKeys<
    OpenApiOperationData<'getTaskStatus'>,
    'createdAt' | 'progress' | 'status' | 'taskId' | 'updatedAt'
  >,
  'agentAnalysis' | 'providerExecution' | 'referenceMaterials' | 'result'
> & {
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
export type GenerationResult = Omit<
  RequiredKeys<Schemas.GenerationResult, 'assetId' | 'contentType' | 'model' | 'prompt' | 'url'>,
  'contentType'
> & {
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
}

/** ==================== 作品/资产相关类型 ==================== */

/** 作品/资产项 */
export type AssetItem = Omit<
  RequiredKeys<
    Schemas.AssetDTO,
    'contentType' | 'createdAt' | 'id' | 'isPublished' | 'model' | 'prompt' | 'url'
  >,
  'contentType'
> & {
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
export type GalleryItem = Omit<
  RequiredKeys<
    Schemas.GalleryDTO,
    'id' | 'contentType' | 'createdAt' | 'isPublished' | 'likeCount' | 'model' | 'prompt' | 'url'
  >,
  'authorName' | 'contentType'
> & {
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
  /** 标题 */
  title?: string
  /** 作者名称 */
  author?: string
  /** 后端作者名称 */
  authorName?: string
  /** 分类标签 */
  category?: string
  /** 点赞数 */
  likeCount: number
}

/** 灵感广场搜索参数 */
export type GallerySearchParams = OpenApiOperationQuery<'getGalleryList'> & {
  /** 内容类型筛选 */
  contentType?: ContentType
  /** 分类筛选 */
  category?: string
  /** 关键词搜索 */
  keyword?: string
}

/** 资产搜索参数 */
export type AssetSearchParams = OpenApiOperationQuery<'getAssetList'> & {
  /** 内容类型筛选 */
  contentType?: ContentType
}

/** 灵感广场列表响应 */
export type GalleryListResponse = PageResult<OpenApiOperationData<'getGalleryList'>, GalleryItem>

/** 资产列表响应 */
export type AssetListResponse = PageResult<OpenApiOperationData<'getAssetList'>, AssetItem>

/** 资产详情响应 */
export type AssetDetailResponse = Omit<
  RequiredKeys<OpenApiOperationData<'getAssetDetail'>, 'asset'>,
  'asset' | 'task'
> & {
  /** 资产信息 */
  asset: AssetItem
  /** 来源任务；老数据可能为空 */
  task?: TaskStatusResponse | null
}

/** 参考素材项 */
export type MaterialItem = RequiredKeys<
  Schemas.MaterialDTO,
  'contentType' | 'createdAt' | 'fileName' | 'id' | 'size' | 'url'
>

/** 素材搜索参数 */
export type MaterialSearchParams = OpenApiOperationQuery<'getMaterialList'> & {
  /** image 或 video */
  contentType?: 'image' | 'video'
}

/** 素材列表响应 */
export type MaterialListResponse = PageResult<OpenApiOperationData<'getMaterialList'>, MaterialItem>

/** 素材引用任务搜索参数 */
export type MaterialTaskSearchParams = OpenApiOperationQuery<'getMaterialTasks'>

/** 素材引用任务列表响应 */
export type MaterialTaskListResponse = PageResult<
  OpenApiOperationData<'getMaterialTasks'>,
  TaskStatusResponse
>

/** ==================== 模型相关类型 ==================== */

/** 模型信息 */
export type ModelInfo = Omit<
  RequiredKeys<
    Schemas.ModelInfo,
    'available' | 'contentType' | 'description' | 'id' | 'name' | 'provider'
  >,
  'contentType' | 'defaultParams'
> & {
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
  /** 默认生成参数 */
  defaultParams?: GenerationParams
}

/** Provider 探测请求 */
export type ProviderProbeRequest = Omit<OpenApiOperationRequest<'probeModel'>, 'contentType'> & {
  /** 内容类型 */
  contentType: ContentType
}

/** Provider 探测响应 */
export type ProviderProbeResponse = OpenApiOperationData<'probeModel'>

/** Provider 路由切换请求 */
export type ProviderRouteUpdateRequest = Omit<
  OpenApiOperationRequest<'updateActiveProvider'>,
  'contentType'
> & {
  /** 内容类型 */
  contentType: ContentType
}

/** Provider 路由切换响应 */
export type ProviderRouteUpdateResponse = OpenApiOperationData<'updateActiveProvider'>

/** Provider 凭证更新请求 */
export type ProviderCredentialUpdateRequest = Omit<
  OpenApiOperationRequest<'updateProviderCredential'>,
  'contentType'
> & {
  /** 内容类型 */
  contentType: ContentType
}

/** Provider 凭证更新响应 */
export type ProviderCredentialUpdateResponse = OpenApiOperationData<'updateProviderCredential'>

/** 模型列表响应 */
export type ModelListResponse = Omit<
  OpenApiOperationData<'getModels'>,
  'audioModels' | 'imageModels' | 'videoModels'
> & {
  /** 图片生成模型 */
  imageModels: ModelInfo[]
  /** 视频生成模型 */
  videoModels: ModelInfo[]
  /** 音频生成模型（预留） */
  audioModels: ModelInfo[]
}

/** 素材上传响应 */
export type MaterialUploadResponse = RequiredKeys<
  Schemas.MaterialUploadResponse,
  'contentType' | 'fileName' | 'size' | 'url'
>

<!-- AIGC 创作工作台 - 主页面 -->
<!-- 设计理念：用户只需描述需求，系统自动处理一切 -->
<template>
  <div class="aigc-studio">
    <!-- 左侧：创作区域 -->
    <div class="aigc-studio__workspace">
      <!-- 中间：作品展示区域 -->
      <div class="aigc-studio__preview">
        <GenerationPreview
          :task="currentTask"
          :result="generationResult"
          :loading="generating"
          @regenerate="handleReusePrompt"
          @retry="handleRetryTask"
        />
      </div>

      <div class="aigc-studio__models">
        <div
          v-for="model in availableModelCards"
          :key="model.id"
          class="aigc-studio__model"
        >
          <span class="aigc-studio__model-type">{{ getContentTypeLabel(model.contentType) }}</span>
          <span class="aigc-studio__model-name">{{ model.name }}</span>
          <el-tag size="small" :type="model.available ? 'success' : 'info'" effect="plain">
            {{ model.available ? '可用' : '不可用' }}
          </el-tag>
        </div>
      </div>

      <!-- 底部：对话输入区域（极简设计，只需输入需求） -->
      <CreationInput
        v-model="userInput"
        v-model:files="uploadedFiles"
        v-model:content-type-hint="contentTypeHint"
        v-model:generation-params="generationParams"
        :loading="generating"
        @submit="handleCreate"
      />
    </div>

    <!-- 右侧：历史记录 -->
    <div class="aigc-studio__sidebar">
      <HistoryPanel
        :items="historyItems"
        :loading="historyLoading"
        @select="handleHistorySelect"
        @reuse="handleHistoryReuse"
        @delete="handleHistoryDelete"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import GenerationPreview from './components/GenerationPreview.vue'
import CreationInput from './components/CreationInput.vue'
import HistoryPanel from './components/HistoryPanel.vue'
import {
  fetchGenerate,
  fetchGetTaskStatus,
  fetchGetAssetList,
  fetchGetModelList,
  fetchUploadMaterial,
  fetchRetryTask
} from '@/api/aigc'
import type {
  GenerateRequest,
  TaskStatusResponse,
  AssetItem,
  ContentType,
  ModelInfo
} from '@/api/model/aigcModel'
import { nowIsoString } from '@/utils/time'

defineOptions({ name: 'AIGCStudio' })

// ==================== 状态管理 ====================
// 用户输入（文本 + 文件，仅此而已）
const userInput = ref('')
const uploadedFiles = ref<File[]>([])
const contentTypeHint = ref<ContentType | null>(null)
const generationParams = ref<Record<string, string | number | boolean>>({})

// 生成状态
const generating = ref(false)
const currentTask = ref<TaskStatusResponse | null>(null)
const generationResult = ref<AssetItem | null>(null)

// 历史记录
const historyItems = ref<AssetItem[]>([])
const historyLoading = ref(false)
const availableModels = ref<ModelInfo[]>([])

const availableModelCards = computed(() => availableModels.value.slice(0, 6))

// ==================== 方法 ====================

/** 加载历史记录 */
const loadHistory = async () => {
  try {
    historyLoading.value = true
    const response = await fetchGetAssetList({ current: 1, size: 20 })
    historyItems.value = response.records
  } catch (error) {
    console.error('加载历史记录失败:', error)
  } finally {
    historyLoading.value = false
  }
}

const loadModels = async () => {
  try {
    const response = await fetchGetModelList()
    availableModels.value = [
      ...response.imageModels,
      ...response.videoModels,
      ...response.audioModels
    ]
  } catch (error) {
    console.error('加载模型列表失败:', error)
  }
}

/**
 * 处理创作请求
 * 用户只需要：1. 输入需求描述 2. 可选上传参考素材
 * 系统自动：识别意图 → 选择模型 → 优化提示词 → 生成内容
 */
const handleCreate = async () => {
  if (!userInput.value.trim() && uploadedFiles.value.length === 0) {
    ElMessage.warning('请输入创作描述或上传参考素材')
    return
  }

  try {
    generating.value = true
    generationResult.value = null
    currentTask.value = null

    const uploadedMaterials = await uploadReferenceMaterials(uploadedFiles.value)

    // 构建请求 - 不指定模型，让Agent自动决策
    const request: GenerateRequest = {
      prompt: userInput.value.trim(),
      contentTypeHint: contentTypeHint.value || undefined,
      generationParams: normalizeGenerationParams(generationParams.value),
      referenceImages: uploadedMaterials.map(item => item.url),
      referenceMaterialIds: uploadedMaterials
        .map(item => item.materialId)
        .filter((id): id is string => Boolean(id))
    }

    const response = await fetchGenerate(request)

    // 初始化任务状态，显示Agent分析结果
    currentTask.value = {
      taskId: response.taskId,
      status: response.status,
      progress: 0,
      agentAnalysis: response.agentAnalysis,
      createdAt: nowIsoString(),
      updatedAt: nowIsoString()
    }

    // 开始轮询任务状态
    await pollTaskStatus(response.taskId)
  } catch (error) {
    console.error('创作请求失败:', error)
    ElMessage.error('创作请求失败，请重试')
  } finally {
    generating.value = false
  }
}

type UploadedMaterialRef = {
  url: string
  materialId?: string
}

/** 将文件上传为可访问 URL，并保留素材ID用于任务追踪 */
const uploadReferenceMaterials = async (files: File[]): Promise<UploadedMaterialRef[]> => {
  if (files.length === 0) return []

  const materials: UploadedMaterialRef[] = []
  for (const file of files) {
    const uploaded = await fetchUploadMaterial(file)
    materials.push({
      url: uploaded.url,
      materialId: uploaded.materialId
    })
  }
  return materials
}

/** 轮询任务状态 */
const pollTaskStatus = async (taskId: string) => {
  const maxAttempts = 180 // 最多轮询3分钟
  const interval = 1000

  for (let i = 0; i < maxAttempts; i++) {
    try {
      const status = await fetchGetTaskStatus(taskId)
      currentTask.value = {
        ...status,
        agentAnalysis: status.agentAnalysis || currentTask.value?.agentAnalysis
      }

      // 注意：后端返回的枚举是大写
      if (status.status === 'COMPLETED' && status.result) {
        generationResult.value = {
          id: status.result.assetId,
          contentType: status.result.contentType,
          url: status.result.url,
          thumbnailUrl: status.result.thumbnailUrl,
          prompt: status.result.prompt,
          model: status.result.model,
          isPublished: false,
          createdAt: status.createdAt
        }
        ElMessage.success('创作完成！')
        // 清空输入，准备下一次创作
        userInput.value = ''
        uploadedFiles.value = []
        loadHistory()
        return
      }

      if (status.status === 'FAILED') {
        ElMessage.error(status.errorMessage || '创作失败，请重试')
        return
      }

      await new Promise(resolve => setTimeout(resolve, interval))
    } catch (error) {
      console.error('查询任务状态失败:', error)
    }
  }

  ElMessage.error('创作超时，请重试')
}

const normalizeGenerationParams = (params: Record<string, string | number | boolean>) => {
  const normalized: Record<string, string | number | boolean> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      normalized[key] = value
    }
  })
  return Object.keys(normalized).length > 0 ? normalized : undefined
}

/** 处理历史记录选择 */
const handleHistorySelect = (item: AssetItem) => {
  generationResult.value = item
}

const handleReusePrompt = (prompt: string) => {
  userInput.value = prompt
  generationResult.value = null
}

const handleRetryTask = async (taskId: string) => {
  try {
    generating.value = true
    generationResult.value = null

    const response = await fetchRetryTask(taskId)
    currentTask.value = {
      taskId: response.taskId,
      status: response.status,
      progress: 0,
      agentAnalysis: response.agentAnalysis,
      createdAt: nowIsoString(),
      updatedAt: nowIsoString()
    }

    await pollTaskStatus(response.taskId)
  } catch (error) {
    console.error('重试任务失败:', error)
    ElMessage.error('重试任务失败，请稍后再试')
  } finally {
    generating.value = false
  }
}

const handleHistoryReuse = (item: AssetItem) => {
  userInput.value = item.prompt
  contentTypeHint.value = item.contentType
  generationParams.value = defaultParamsForType(item.contentType)
  generationResult.value = item
  ElMessage.success('已填入历史 Prompt')
}

const defaultParamsForType = (contentType: ContentType): Record<string, string | number | boolean> => {
  if (contentType === 'IMAGE') return { aspectRatio: '16:9', imageSize: '1K' }
  if (contentType === 'VIDEO') return { aspectRatio: '16:9', duration: 8, quality: 'standard' }
  if (contentType === 'AUDIO') return { audioType: 'tts', voice: 'Kore' }
  return {}
}

const getContentTypeLabel = (type: ContentType) => {
  const map: Record<ContentType, string> = {
    IMAGE: '图片',
    VIDEO: '视频',
    AUDIO: '音频'
  }
  return map[type]
}

/** 处理历史记录删除 */
const handleHistoryDelete = async (item: AssetItem) => {
  historyItems.value = historyItems.value.filter(h => h.id !== item.id)
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadHistory()
  loadModels()
})
</script>

<style lang="scss" scoped>
.aigc-studio {
  display: flex;
  height: calc(100vh - 120px);
  gap: 20px;
  padding: 20px;
  background: var(--el-bg-color-page);

  &__workspace {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
    min-width: 0;
  }

  &__preview {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--el-bg-color);
    border-radius: 12px;
    border: 1px solid var(--el-border-color-light);
    overflow: hidden;
  }

  &__models {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  &__model {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 32px;
    padding: 0 10px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;
    background: var(--el-bg-color);
    font-size: 12px;
    color: var(--el-text-color-regular);
  }

  &__model-type {
    color: var(--el-text-color-secondary);
  }

  &__model-name {
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__sidebar {
    width: 320px;
    flex-shrink: 0;
  }
}
</style>

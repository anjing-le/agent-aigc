<!-- AIGC 创作工作台 - 主页面 -->
<!-- 设计理念：用户只需描述需求，系统自动处理一切 -->
<template>
  <div class="aigc-studio">
    <!-- 左侧：创作区域 -->
    <div class="aigc-studio__workspace">
      <div class="aigc-studio__overview">
        <div class="aigc-studio__overview-main">
          <div class="aigc-studio__eyebrow">AIGC Studio</div>
          <h2 class="aigc-studio__title">智能创作工作台</h2>
          <p class="aigc-studio__subtitle">
            Agent 会自动完成意图识别、Prompt 优化、模型路由和生成结果归档
          </p>
        </div>

        <div class="aigc-studio__stats">
          <div v-for="item in studioStats" :key="item.label" class="aigc-studio__stat">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>

      <div class="aigc-studio__taskbar">
        <div class="aigc-studio__taskbar-left">
          <el-tag :type="currentTaskTagType" effect="plain">{{ currentTaskStatusText }}</el-tag>
          <span v-if="currentTask?.taskId" class="aigc-studio__task-id">
            {{ currentTask.taskId }}
          </span>
          <span v-else class="aigc-studio__task-id">等待新的创作任务</span>
        </div>
        <div class="aigc-studio__taskbar-right">
          <span>{{ currentModeText }}</span>
          <span>{{ uploadedFiles.length }} 个参考素材</span>
        </div>
      </div>

      <section class="aigc-studio__orchestration">
        <div class="aigc-studio__orchestration-header">
          <div>
            <h3>Agent 编排链路</h3>
            <p>{{ orchestrationSubtitle }}</p>
          </div>
          <el-tag :type="currentTaskTagType" effect="plain">{{ orchestrationStageText }}</el-tag>
        </div>

        <div class="aigc-studio__orchestration-steps">
          <article
            v-for="(item, index) in orchestrationSteps"
            :key="item.label"
            class="aigc-studio__orchestration-step"
            :class="`aigc-studio__orchestration-step--${item.state}`"
          >
            <span class="aigc-studio__orchestration-index">{{ index + 1 }}</span>
            <div class="aigc-studio__orchestration-copy">
              <strong>{{ item.label }}</strong>
              <span>{{ item.value }}</span>
            </div>
          </article>
        </div>
      </section>

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

      <div class="aigc-studio__models-panel">
        <div class="aigc-studio__models-header">
          <span>模型路由能力</span>
          <el-button type="primary" link size="small" @click="$router.push('/aigc/models')">
            配置
          </el-button>
        </div>
        <div class="aigc-studio__models">
          <div v-for="model in availableModelCards" :key="model.id" class="aigc-studio__model">
            <span class="aigc-studio__model-type">{{
              getContentTypeLabel(model.contentType)
            }}</span>
            <span class="aigc-studio__model-name">{{ model.name }}</span>
            <el-tag size="small" :type="model.available ? 'success' : 'info'" effect="plain">
              {{ model.available ? '可用' : '不可用' }}
            </el-tag>
          </div>
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

  const route = useRoute()
  const router = useRouter()

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
  const activePollId = ref(0)

  // 历史记录
  const historyItems = ref<AssetItem[]>([])
  const historyLoading = ref(false)
  const availableModels = ref<ModelInfo[]>([])

  const availableModelCards = computed(() => availableModels.value.slice(0, 6))

  type OrchestrationStepState = 'idle' | 'active' | 'done' | 'error'

  type OrchestrationStep = {
    label: string
    value: string
    state: OrchestrationStepState
  }

  const studioStats = computed(() => [
    {
      label: '创作记录',
      value: `${historyItems.value.length}`
    },
    {
      label: '可用模型',
      value: `${availableModels.value.filter((model) => model.available).length}`
    },
    {
      label: '能力类型',
      value: `${new Set(availableModels.value.map((model) => model.contentType)).size}`
    }
  ])

  const currentModeText = computed(() => {
    if (!contentTypeHint.value) return '自动路由'
    return `${getContentTypeLabel(contentTypeHint.value)}模式`
  })

  const currentTaskStatusText = computed(() => {
    if (generating.value) return '生成中'
    const status = currentTask.value?.status?.toUpperCase()
    const map: Record<string, string> = {
      PENDING: '排队中',
      PROCESSING: '生成中',
      COMPLETED: '已完成',
      FAILED: '失败'
    }
    return status ? map[status] || status : '待创建'
  })

  const currentTaskTagType = computed(() => {
    const status = currentTask.value?.status?.toUpperCase()
    if (generating.value || status === 'PROCESSING' || status === 'PENDING') return 'warning'
    if (status === 'COMPLETED') return 'success'
    if (status === 'FAILED') return 'danger'
    return 'info'
  })

  const orchestrationSubtitle = computed(() => {
    if (currentTask.value?.taskId) return `任务 ${currentTask.value.taskId}`
    if (userInput.value.trim()) return '基于当前输入预估执行链路'
    if (generationResult.value) return `最近作品 ${generationResult.value.id}`
    return '等待新的创作任务'
  })

  const orchestrationStageText = computed(() => {
    if (generationResult.value && currentTask.value?.status === 'COMPLETED') return '已归档'
    if (currentTask.value?.agentAnalysis) return 'Agent 已决策'
    if (userInput.value.trim() || uploadedFiles.value.length > 0) return '待提交'
    return '空闲'
  })

  const orchestrationSteps = computed<OrchestrationStep[]>(() => {
    const analysis = currentTask.value?.agentAnalysis
    const status = currentTask.value?.status?.toUpperCase()
    const hasDraft = Boolean(userInput.value.trim() || uploadedFiles.value.length > 0)
    const hasTask = Boolean(currentTask.value?.taskId)
    const failed = status === 'FAILED'
    const completed = status === 'COMPLETED' && Boolean(generationResult.value)

    return [
      {
        label: '需求输入',
        value: resolvePromptInputSummary(),
        state: hasDraft || hasTask || generationResult.value ? 'done' : 'idle'
      },
      {
        label: '意图识别',
        value: analysis?.intent || resolveContentTypeHintSummary(),
        state: analysis ? 'done' : hasDraft ? 'active' : 'idle'
      },
      {
        label: 'Prompt 优化',
        value: analysis?.optimizedPrompt
          ? truncateText(analysis.optimizedPrompt, 42)
          : analysis?.cleanPrompt
            ? truncateText(analysis.cleanPrompt, 42)
            : '等待 Agent 生成优化 Prompt',
        state: analysis?.optimizedPrompt ? 'done' : hasTask ? 'active' : 'idle'
      },
      {
        label: '模型路由',
        value: analysis?.selectedModel || generationResult.value?.model || '自动选择 Provider',
        state: analysis?.selectedModel || generationResult.value?.model ? 'done' : hasDraft ? 'active' : 'idle'
      },
      {
        label: '参数配置',
        value: resolveOrchestrationParamsSummary(),
        state: hasDraft || hasTask || generationResult.value ? 'done' : 'idle'
      },
      {
        label: '结果归档',
        value: failed ? formatTaskError(currentTask.value as TaskStatusResponse) : currentTaskStatusText.value,
        state: failed ? 'error' : completed ? 'done' : generating.value || hasTask ? 'active' : 'idle'
      }
    ]
  })

  // ==================== 方法 ====================

  const resolvePromptInputSummary = () => {
    const analysis = currentTask.value?.agentAnalysis
    const prompt = analysis?.originalPrompt || userInput.value.trim() || generationResult.value?.prompt
    if (prompt) return truncateText(prompt, 42)
    if (uploadedFiles.value.length > 0) return `${uploadedFiles.value.length} 个参考素材`
    return '等待输入'
  }

  const resolveContentTypeHintSummary = () => {
    if (contentTypeHint.value) return `${getContentTypeLabel(contentTypeHint.value)}模式`
    return '自动识别内容类型'
  }

  const resolveOrchestrationParamsSummary = () => {
    const analysis = currentTask.value?.agentAnalysis
    const analyzedParams = formatAnalyzedIntentParams(analysis)
    if (analyzedParams) return analyzedParams
    const requestParams = formatParamEntries(normalizeGenerationParams(generationParams.value) || {})
    return requestParams || '使用默认参数'
  }

  const formatAnalyzedIntentParams = (analysis?: TaskStatusResponse['agentAnalysis']) => {
    const intent = analysis?.analyzedIntent
    if (!analysis || !intent) return ''

    if (analysis.contentType === 'IMAGE' && intent.imageParams) {
      return formatParamEntries({
        比例: intent.imageParams.aspectRatio,
        尺寸: intent.imageParams.imageSize,
        风格: intent.imageParams.style
      })
    }
    if (analysis.contentType === 'VIDEO' && intent.videoParams) {
      return formatParamEntries({
        比例: intent.videoParams.aspectRatio,
        分辨率: intent.videoParams.resolution,
        时长: intent.videoParams.duration ? `${intent.videoParams.duration}s` : '',
        质量: intent.videoParams.quality
      })
    }
    if (analysis.contentType === 'AUDIO' && intent.audioParams) {
      return formatParamEntries({
        类型: intent.audioParams.type,
        音色: intent.audioParams.voice,
        情绪: intent.audioParams.mood
      })
    }
    return ''
  }

  const formatParamEntries = (params: Record<string, string | number | boolean | undefined>) =>
    Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== '' && value !== null)
      .map(([key, value]) => `${key}: ${value}`)
      .join(' · ')

  const truncateText = (value: string, maxLength = 42) => {
    const text = value.trim()
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
  }

  const applyRoutePrompt = () => {
    if (normalizeQueryValue(route.query.taskId)) return

    const prompt = normalizeQueryValue(route.query.prompt)
    if (!prompt || prompt === userInput.value) return

    userInput.value = prompt
    generationResult.value = null

    const contentType = normalizeContentType(normalizeQueryValue(route.query.contentType))
    if (contentType) {
      contentTypeHint.value = contentType
      generationParams.value = defaultParamsForType(contentType)
    }

    ElMessage.success('已填入复用 Prompt')
  }

  const normalizeQueryValue = (value: unknown) => {
    if (Array.isArray(value)) return value[0] || ''
    return typeof value === 'string' ? value : ''
  }

  const normalizeContentType = (value: string): ContentType | null => {
    const contentType = value.toUpperCase()
    if (contentType === 'IMAGE' || contentType === 'VIDEO' || contentType === 'AUDIO') {
      return contentType
    }
    return null
  }

  const syncTaskRoute = async (taskId: string) => {
    if (normalizeQueryValue(route.query.taskId) === taskId) return
    await router.replace({ query: { taskId } })
  }

  const clearTaskRoute = () => {
    if (!normalizeQueryValue(route.query.taskId)) return
    const query = { ...route.query }
    delete query.taskId
    router.replace({ query })
  }

  const cancelActivePolling = () => {
    activePollId.value += 1
  }

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
        referenceImages: uploadedMaterials.map((item) => item.url),
        referenceMaterialIds: uploadedMaterials
          .map((item) => item.materialId)
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
      await syncTaskRoute(response.taskId)

      // 开始轮询任务状态
      await pollTaskStatus(response.taskId, { clearInputOnComplete: true })
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

  type PollTaskOptions = {
    clearInputOnComplete?: boolean
    announceComplete?: boolean
    announceFailure?: boolean
  }

  const applyTaskStatus = (status: TaskStatusResponse) => {
    currentTask.value = {
      ...status,
      agentAnalysis: status.agentAnalysis || currentTask.value?.agentAnalysis
    }
  }

  const applyCompletedTaskResult = (status: TaskStatusResponse) => {
    if (!status.result) return false

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
    return true
  }

  const restoreTaskFromRoute = async () => {
    const taskId = normalizeQueryValue(route.query.taskId)
    if (!taskId) return
    if (currentTask.value?.taskId === taskId && generating.value) return

    try {
      generating.value = true
      generationResult.value = null
      const status = await fetchGetTaskStatus(taskId)
      applyTaskStatus(status)

      if (status.status === 'COMPLETED' && applyCompletedTaskResult(status)) {
        return
      }

      if (status.status === 'FAILED') {
        ElMessage.error(formatTaskError(status))
        return
      }

      await pollTaskStatus(taskId, { announceComplete: false, announceFailure: true })
    } catch (error) {
      console.error('恢复任务状态失败:', error)
      ElMessage.error('恢复任务状态失败，请刷新后重试')
    } finally {
      generating.value = false
    }
  }

  /** 轮询任务状态 */
  const pollTaskStatus = async (taskId: string, options: PollTaskOptions = {}) => {
    const maxAttempts = 180 // 最多轮询3分钟
    const interval = 1000
    const pollId = activePollId.value + 1
    activePollId.value = pollId
    const announceComplete = options.announceComplete !== false
    const announceFailure = options.announceFailure !== false

    for (let i = 0; i < maxAttempts; i++) {
      if (pollId !== activePollId.value) return

      try {
        const status = await fetchGetTaskStatus(taskId)
        applyTaskStatus(status)

        // 注意：后端返回的枚举是大写
        if (status.status === 'COMPLETED' && applyCompletedTaskResult(status)) {
          if (announceComplete) ElMessage.success('创作完成！')
          // 清空输入，准备下一次创作
          if (options.clearInputOnComplete) {
            userInput.value = ''
            uploadedFiles.value = []
          }
          loadHistory()
          return
        }

        if (status.status === 'FAILED') {
          if (announceFailure) ElMessage.error(formatTaskError(status))
          return
        }

        await new Promise((resolve) => setTimeout(resolve, interval))
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

  const formatTaskError = (status: TaskStatusResponse) => {
    const message = status.errorMessage || '创作失败，请重试'
    return status.errorCode ? `${message}（${status.errorCode}）` : message
  }

  /** 处理历史记录选择 */
  const handleHistorySelect = (item: AssetItem) => {
    cancelActivePolling()
    generating.value = false
    currentTask.value = null
    generationResult.value = item
    clearTaskRoute()
  }

  const handleReusePrompt = (prompt: string) => {
    cancelActivePolling()
    userInput.value = prompt
    currentTask.value = null
    generationResult.value = null
    clearTaskRoute()
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
      await syncTaskRoute(response.taskId)

      await pollTaskStatus(response.taskId)
    } catch (error) {
      console.error('重试任务失败:', error)
      ElMessage.error('重试任务失败，请稍后再试')
    } finally {
      generating.value = false
    }
  }

  const handleHistoryReuse = (item: AssetItem) => {
    cancelActivePolling()
    userInput.value = item.prompt
    contentTypeHint.value = item.contentType
    generationParams.value = defaultParamsForType(item.contentType)
    currentTask.value = null
    generationResult.value = item
    clearTaskRoute()
    ElMessage.success('已填入历史 Prompt')
  }

  const defaultParamsForType = (
    contentType: ContentType
  ): Record<string, string | number | boolean> => {
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
    historyItems.value = historyItems.value.filter((h) => h.id !== item.id)
  }

  // ==================== 生命周期 ====================
  onMounted(() => {
    if (normalizeQueryValue(route.query.taskId)) {
      restoreTaskFromRoute()
    } else {
      applyRoutePrompt()
    }
    loadHistory()
    loadModels()
  })

  watch(
    () => [route.query.prompt, route.query.contentType],
    () => {
      applyRoutePrompt()
    }
  )

  watch(
    () => route.query.taskId,
    () => {
      restoreTaskFromRoute()
    }
  )

  onBeforeUnmount(() => {
    cancelActivePolling()
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

    &__overview {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
      padding: 18px 20px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__overview-main {
      min-width: 0;
    }

    &__eyebrow {
      margin-bottom: 6px;
      font-size: 12px;
      font-weight: 600;
      color: var(--el-color-primary);
      text-transform: uppercase;
    }

    &__title {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    &__subtitle {
      margin: 8px 0 0;
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }

    &__stats {
      display: grid;
      grid-template-columns: repeat(3, minmax(88px, 1fr));
      gap: 10px;
      width: min(380px, 42%);
      flex-shrink: 0;
    }

    &__stat {
      display: flex;
      flex-direction: column;
      justify-content: center;
      min-height: 58px;
      padding: 10px 12px;
      background: var(--el-fill-color-light);
      border-radius: 8px;

      span {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }

      strong {
        margin-top: 6px;
        font-size: 20px;
        line-height: 1;
        color: var(--el-text-color-primary);
      }
    }

    &__taskbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      min-height: 40px;
      padding: 8px 12px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__taskbar-left,
    &__taskbar-right {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    &__taskbar-right {
      flex-shrink: 0;
    }

    &__task-id {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &__orchestration {
      padding: 12px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__orchestration-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 10px;

      h3 {
        margin: 0;
        font-size: 14px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      p {
        margin: 5px 0 0;
        font-size: 12px;
        color: var(--el-text-color-secondary);
        word-break: break-word;
      }
    }

    &__orchestration-steps {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 8px;
    }

    &__orchestration-step {
      display: flex;
      gap: 8px;
      min-width: 0;
      min-height: 68px;
      padding: 10px;
      background: var(--el-fill-color-lighter);
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;

      &--active {
        border-color: var(--el-color-warning-light-5);
        background: var(--el-color-warning-light-9);
      }

      &--done {
        border-color: var(--el-color-success-light-7);
        background: var(--el-color-success-light-9);
      }

      &--error {
        border-color: var(--el-color-danger-light-7);
        background: var(--el-color-danger-light-9);
      }
    }

    &__orchestration-index {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 22px;
      height: 22px;
      flex: 0 0 22px;
      border-radius: 50%;
      background: var(--el-bg-color);
      color: var(--el-text-color-secondary);
      font-size: 12px;
      font-weight: 600;
    }

    &__orchestration-copy {
      min-width: 0;

      strong,
      span {
        display: block;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      strong {
        margin-bottom: 5px;
        color: var(--el-text-color-primary);
        font-size: 12px;
        font-weight: 600;
        white-space: nowrap;
      }

      span {
        display: -webkit-box;
        color: var(--el-text-color-secondary);
        font-size: 12px;
        line-height: 1.4;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        word-break: break-word;
      }
    }

    &__preview {
      flex: 1 1 320px;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 320px;
      background: var(--el-bg-color);
      border-radius: 12px;
      border: 1px solid var(--el-border-color-light);
      overflow: hidden;
    }

    &__models-panel {
      padding: 12px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__models-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 10px;
      font-size: 13px;
      font-weight: 500;
      color: var(--el-text-color-primary);
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

  @media screen and (max-width: 1200px) {
    .aigc-studio {
      height: auto;
      min-height: calc(100vh - 120px);
      flex-direction: column;

      &__sidebar {
        width: 100%;
        min-height: 360px;
      }
    }
  }

  @media screen and (min-width: 1440px) {
    .aigc-studio {
      &__orchestration-steps {
        grid-template-columns: repeat(6, minmax(0, 1fr));
      }
    }
  }

  @media screen and (max-width: 768px) {
    .aigc-studio {
      padding: 12px;

      &__overview,
      &__taskbar,
      &__orchestration-header {
        align-items: stretch;
        flex-direction: column;
      }

      &__stats {
        grid-template-columns: repeat(3, minmax(0, 1fr));
        width: 100%;
      }

      &__orchestration-steps {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      &__taskbar-right {
        justify-content: space-between;
      }
    }
  }
</style>

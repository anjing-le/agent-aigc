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
        />
      </div>

      <!-- 底部：对话输入区域（极简设计，只需输入需求） -->
      <CreationInput
        v-model="userInput"
        v-model:files="uploadedFiles"
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
  fetchGetAssetList
} from '@/api/aigc'
import type {
  GenerateRequest,
  TaskStatusResponse,
  AssetItem
} from '@/api/model/aigcModel'

defineOptions({ name: 'AIGCStudio' })

// ==================== 状态管理 ====================
// 用户输入（文本 + 文件，仅此而已）
const userInput = ref('')
const uploadedFiles = ref<File[]>([])

// 生成状态
const generating = ref(false)
const currentTask = ref<TaskStatusResponse | null>(null)
const generationResult = ref<AssetItem | null>(null)

// 历史记录
const historyItems = ref<AssetItem[]>([])
const historyLoading = ref(false)

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

    // 构建请求 - 不指定模型，让Agent自动决策
    const request: GenerateRequest = {
      prompt: userInput.value.trim(),
      // 如果有上传文件，转换为base64或URL
      referenceImages: await convertFilesToUrls(uploadedFiles.value)
    }

    const response = await fetchGenerate(request)

    // 初始化任务状态，显示Agent分析结果
    currentTask.value = {
      taskId: response.taskId,
      status: response.status,
      progress: 0,
      agentAnalysis: response.agentAnalysis,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
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

/** 将文件转换为URL（后续可改为上传到OSS） */
const convertFilesToUrls = async (files: File[]): Promise<string[]> => {
  if (files.length === 0) return []

  // TODO: 实际应该上传到服务器，这里暂时转为base64
  const urls: string[] = []
  for (const file of files) {
    const base64 = await fileToBase64(file)
    urls.push(base64)
  }
  return urls
}

/** 文件转base64 */
const fileToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

/** 轮询任务状态 */
const pollTaskStatus = async (taskId: string) => {
  const maxAttempts = 180 // 最多轮询3分钟
  const interval = 1000

  for (let i = 0; i < maxAttempts; i++) {
    try {
      const status = await fetchGetTaskStatus(taskId)
      currentTask.value = status

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

/** 处理历史记录选择 */
const handleHistorySelect = (item: AssetItem) => {
  generationResult.value = item
}

/** 处理历史记录删除 */
const handleHistoryDelete = async (item: AssetItem) => {
  historyItems.value = historyItems.value.filter(h => h.id !== item.id)
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadHistory()
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

  &__sidebar {
    width: 320px;
    flex-shrink: 0;
  }
}
</style>


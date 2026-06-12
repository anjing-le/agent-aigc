<!-- 生成结果预览组件 -->
<template>
  <div class="generation-preview">
    <!-- 空状态 -->
    <div v-if="!loading && !result && !task" class="generation-preview__empty">
      <div class="generation-preview__empty-icon">
        <el-icon :size="80"><MagicStick /></el-icon>
      </div>
      <div class="generation-preview__empty-title">AI 创作工坊</div>
      <div class="generation-preview__empty-desc">
        描述你想要的内容，AI会自动理解并为你创作
      </div>
      <div class="generation-preview__empty-features">
        <div class="feature-item">
          <el-icon><Picture /></el-icon>
          <span>图片生成</span>
        </div>
        <div class="feature-item">
          <el-icon><VideoPlay /></el-icon>
          <span>视频创作</span>
        </div>
        <div class="feature-item">
          <el-icon><Edit /></el-icon>
          <span>智能优化</span>
        </div>
      </div>
    </div>

    <!-- 生成中状态 -->
    <div v-else-if="loading" class="generation-preview__loading">
      <div class="generation-preview__progress">
        <el-progress
          type="circle"
          :percentage="progressPercentage"
          :width="140"
          :stroke-width="10"
          :color="progressColor"
        >
          <template #default>
            <div class="generation-preview__progress-text">
              <span class="percentage">{{ progressPercentage }}%</span>
              <span class="status">{{ statusText }}</span>
            </div>
          </template>
        </el-progress>
      </div>

      <!-- Agent 分析结果展示 -->
      <div class="generation-preview__loading-info">
        <div class="generation-preview__loading-title">
          <el-icon class="is-loading"><Loading /></el-icon>
          AI 正在为你创作...
        </div>

        <!-- 智能决策展示（让用户了解AI在做什么） -->
        <div v-if="task?.agentAnalysis" class="generation-preview__agent-decision">
          <div class="decision-item">
            <span class="label">理解意图</span>
            <el-tag size="small" effect="plain">{{ task.agentAnalysis.intent }}</el-tag>
          </div>
          <div class="decision-item">
            <span class="label">选择模型</span>
            <el-tag size="small" type="success" effect="plain">{{ task.agentAnalysis.selectedModel }}</el-tag>
          </div>
          <div class="decision-item">
            <span class="label">输出类型</span>
            <el-tag size="small" type="warning" effect="plain">{{ getContentTypeLabel(task.agentAnalysis.contentType) }}</el-tag>
          </div>
          <div v-if="task.agentAnalysis.confidence" class="decision-item">
            <span class="label">置信度</span>
            <el-tag size="small" type="info" effect="plain">{{ formatConfidence(task.agentAnalysis.confidence) }}</el-tag>
          </div>
        </div>

        <div v-if="task?.agentAnalysis?.cleanPrompt" class="generation-preview__optimized-prompt">
          <div class="prompt-label">
            <el-icon><Edit /></el-icon>
            清洗后的提示词
          </div>
          <div class="prompt-content">{{ task.agentAnalysis.cleanPrompt }}</div>
        </div>

        <div v-if="agentParamsText" class="generation-preview__optimized-prompt">
          <div class="prompt-label">
            <el-icon><MagicStick /></el-icon>
            参数
          </div>
          <div class="prompt-content">{{ agentParamsText }}</div>
        </div>

        <div v-if="task?.agentAnalysis?.optimizedPrompt" class="generation-preview__optimized-prompt">
          <div class="prompt-label">
            <el-icon><MagicStick /></el-icon>
            优化后的提示词
          </div>
          <div class="prompt-content">{{ task.agentAnalysis.optimizedPrompt }}</div>
        </div>

        <div v-if="referenceMaterials.length > 0" class="generation-preview__references">
          <div class="generation-preview__references-title">参考素材</div>
          <div class="generation-preview__references-list">
            <button
              v-for="material in referenceMaterials"
              :key="material.id"
              class="generation-preview__reference"
              type="button"
              @click="openMaterial(material.url)"
            >
              <el-image
                v-if="isImageMaterial(material.contentType)"
                :src="material.url"
                fit="cover"
                class="generation-preview__reference-media"
              />
              <video
                v-else-if="isVideoMaterial(material.contentType)"
                :src="material.url"
                muted
                playsinline
                class="generation-preview__reference-media"
              />
              <div v-else class="generation-preview__reference-file">
                <el-icon><Headset /></el-icon>
              </div>
              <span>{{ material.originalFileName || material.fileName }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 失败状态 -->
    <div v-else-if="isFailed" class="generation-preview__failed">
      <div class="generation-preview__failed-icon">
        <el-icon :size="56"><WarningFilled /></el-icon>
      </div>
      <div class="generation-preview__failed-title">创作失败</div>
      <div class="generation-preview__failed-desc">
        {{ task?.errorMessage || '模型调用暂时不可用，请稍后重试' }}
      </div>
      <el-tag v-if="task?.errorCode" type="danger" effect="plain">
        {{ task.errorCode }}
      </el-tag>
      <div v-if="task?.taskId" class="generation-preview__failed-task">
        任务 {{ task.taskId }}
      </div>
      <div class="generation-preview__actions">
        <el-button type="primary" :icon="Refresh" @click="handleRetry">
          重试任务
        </el-button>
        <el-button
          v-if="task?.agentAnalysis?.optimizedPrompt"
          :icon="Edit"
          @click="handleRegenerateFromTask"
        >
          修改 Prompt
        </el-button>
      </div>
    </div>

    <!-- 结果展示 -->
    <div v-else-if="result" class="generation-preview__result">
      <!-- 图片结果 (后端返回大写 IMAGE) -->
      <template v-if="isImage">
        <el-image
          :src="result.url"
          :preview-src-list="[result.url]"
          fit="contain"
          class="generation-preview__image"
        >
          <template #placeholder>
            <div class="generation-preview__image-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
          </template>
        </el-image>
      </template>

      <!-- 视频结果 (后端返回大写 VIDEO) -->
      <template v-else-if="isVideo">
        <el-image
          v-if="isVisualPreview"
          :src="result.thumbnailUrl || result.url"
          fit="contain"
          class="generation-preview__image"
        />
        <video v-else :src="result.url" controls class="generation-preview__video" />
      </template>

      <!-- 音频结果 (后端返回大写 AUDIO) -->
      <template v-else-if="isAudio">
        <div class="generation-preview__audio">
          <el-image
            v-if="isVisualPreview"
            :src="result.thumbnailUrl || result.url"
            fit="contain"
            class="generation-preview__audio-cover"
          />
          <el-icon :size="48"><Headset /></el-icon>
          <audio v-if="!isVisualPreview" :src="result.url" controls class="generation-preview__audio-player" />
        </div>
      </template>

      <!-- 操作按钮 -->
      <div class="generation-preview__actions">
        <el-button :icon="Download" @click="handleDownload">下载</el-button>
        <el-button :icon="Refresh" @click="handleRegenerate">重新生成</el-button>
      </div>

      <div v-if="referenceMaterials.length > 0" class="generation-preview__references generation-preview__references--result">
        <div class="generation-preview__references-title">参考素材</div>
        <div class="generation-preview__references-list">
          <button
            v-for="material in referenceMaterials"
            :key="material.id"
            class="generation-preview__reference"
            type="button"
            @click="openMaterial(material.url)"
          >
            <el-image
              v-if="isImageMaterial(material.contentType)"
              :src="material.url"
              fit="cover"
              class="generation-preview__reference-media"
            />
            <video
              v-else-if="isVideoMaterial(material.contentType)"
              :src="material.url"
              muted
              playsinline
              class="generation-preview__reference-media"
            />
            <div v-else class="generation-preview__reference-file">
              <el-icon><Headset /></el-icon>
            </div>
            <span>{{ material.originalFileName || material.fileName }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  MagicStick,
  Loading,
  Download,
  Refresh,
  Headset,
  Picture,
  VideoPlay,
  Edit,
  WarningFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { TaskStatusResponse, AssetItem, ContentType } from '@/api/model/aigcModel'
import { downloadAigcAsset } from '@/utils/aigcAsset'

interface Props {
  task?: TaskStatusResponse | null
  result?: AssetItem | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  regenerate: [prompt: string]
  retry: [taskId: string]
}>()

// 进度百分比
const progressPercentage = computed(() => {
  return props.task?.progress || 0
})

// 进度条颜色
const progressColor = computed(() => {
  const progress = progressPercentage.value
  if (progress < 30) return '#409eff'
  if (progress < 70) return '#67c23a'
  return '#e6a23c'
})

// 状态文本 (后端返回大写枚举)
const statusText = computed(() => {
  const status = props.task?.status?.toUpperCase()
  const map: Record<string, string> = {
    PENDING: '排队中',
    PROCESSING: '生成中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status || 'PENDING'] || '准备中'
})

// 内容类型判断 (忽略大小写)
const isImage = computed(() => props.result?.contentType?.toUpperCase() === 'IMAGE')
const isVideo = computed(() => props.result?.contentType?.toUpperCase() === 'VIDEO')
const isAudio = computed(() => props.result?.contentType?.toUpperCase() === 'AUDIO')
const isVisualPreview = computed(() => props.result?.url?.startsWith('data:image/') || false)
const isFailed = computed(() => props.task?.status?.toUpperCase() === 'FAILED')
const referenceMaterials = computed(() => props.task?.referenceMaterials || [])

const agentParamsText = computed(() => {
  const analysis = props.task?.agentAnalysis
  const intent = analysis?.analyzedIntent
  if (!analysis || !intent) return ''

  if (analysis.contentType === 'IMAGE' && intent.imageParams) {
    return compactParams({
      比例: intent.imageParams.aspectRatio,
      尺寸: intent.imageParams.imageSize,
      风格: intent.imageParams.style
    })
  }
  if (analysis.contentType === 'VIDEO' && intent.videoParams) {
    return compactParams({
      比例: intent.videoParams.aspectRatio,
      分辨率: intent.videoParams.resolution,
      时长: intent.videoParams.duration ? `${intent.videoParams.duration}s` : '',
      质量: intent.videoParams.quality
    })
  }
  if (analysis.contentType === 'AUDIO' && intent.audioParams) {
    return compactParams({
      类型: intent.audioParams.type,
      音色: intent.audioParams.voice,
      情绪: intent.audioParams.mood
    })
  }
  return ''
})

const compactParams = (params: Record<string, string | number | undefined>) => {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${key}: ${value}`)
    .join(' · ')
}

/** 获取内容类型标签 */
const getContentTypeLabel = (type: ContentType) => {
  const typeUpper = type?.toUpperCase()
  const map: Record<string, string> = {
    IMAGE: '图片',
    VIDEO: '视频',
    AUDIO: '音频'
  }
  return map[typeUpper] || type
}

const formatConfidence = (confidence: number) => `${Math.round(confidence * 100)}%`

const isImageMaterial = (contentType: string) => contentType.startsWith('image/')
const isVideoMaterial = (contentType: string) => contentType.startsWith('video/')

const openMaterial = (url: string) => {
  window.open(url, '_blank', 'noopener,noreferrer')
}

/** 处理下载 */
const handleDownload = () => {
  if (!props.result) return
  downloadAigcAsset(props.result)
}

/** 处理重新生成 */
const handleRegenerate = () => {
  if (!props.result?.prompt) {
    ElMessage.info('请修改描述后重新提交')
    return
  }
  emit('regenerate', props.result.prompt)
}

const handleRetry = () => {
  if (!props.task?.taskId) return
  emit('retry', props.task.taskId)
}

const handleRegenerateFromTask = () => {
  const prompt = props.task?.agentAnalysis?.optimizedPrompt
  if (!prompt) return
  emit('regenerate', prompt)
}
</script>

<style lang="scss" scoped>
.generation-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;

  &__empty {
    text-align: center;

    &-icon {
      color: var(--el-color-primary-light-3);
      margin-bottom: 20px;
    }

    &-title {
      font-size: 24px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      margin-bottom: 12px;
    }

    &-desc {
      font-size: 15px;
      color: var(--el-text-color-secondary);
      margin-bottom: 32px;
    }

    &-features {
      display: flex;
      gap: 32px;
      justify-content: center;

      .feature-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
        color: var(--el-text-color-secondary);

        .el-icon {
          font-size: 28px;
          color: var(--el-color-primary-light-3);
        }

        span {
          font-size: 13px;
        }
      }
    }
  }

  &__loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 32px;
    max-width: 500px;

    &-info {
      text-align: center;
    }

    &-title {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 500;
      color: var(--el-text-color-primary);
      margin-bottom: 20px;
    }
  }

  &__agent-decision {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    justify-content: center;
    margin-bottom: 20px;

    .decision-item {
      display: flex;
      align-items: center;
      gap: 8px;

      .label {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }

  &__optimized-prompt {
    background: var(--el-fill-color-light);
    border-radius: 12px;
    padding: 16px;
    max-width: 100%;

    .prompt-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      margin-bottom: 8px;

      .el-icon {
        color: var(--el-color-primary);
      }
    }

    .prompt-content {
      font-size: 14px;
      color: var(--el-text-color-primary);
      line-height: 1.6;
    }
  }

  &__references {
    width: min(500px, 100%);
    margin: 16px auto 0;

    &--result {
      margin-top: 0;
    }

    &-title {
      margin-bottom: 10px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      text-align: left;
    }

    &-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
      gap: 10px;
    }
  }

  &__reference {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
    padding: 0;
    overflow: hidden;
    cursor: pointer;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;

    span {
      overflow: hidden;
      padding: 0 8px 8px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__reference-media {
    width: 100%;
    aspect-ratio: 4 / 3;
    object-fit: cover;
    background: var(--el-fill-color-light);
  }

  &__reference-file {
    display: grid;
    width: 100%;
    aspect-ratio: 4 / 3;
    color: var(--el-text-color-secondary);
    place-items: center;
    background: var(--el-fill-color-light);
  }

  &__progress-text {
    display: flex;
    flex-direction: column;
    align-items: center;

    .percentage {
      font-size: 28px;
      font-weight: 600;
      color: var(--el-color-primary);
    }

    .status {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      margin-top: 4px;
    }
  }

  &__result {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    max-width: 100%;
    max-height: 100%;
  }

  &__failed {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;
    width: min(520px, 100%);
    padding: 32px;
    text-align: center;
  }

  &__failed-icon {
    color: var(--el-color-danger);
  }

  &__failed-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__failed-desc {
    max-width: 420px;
    font-size: 14px;
    line-height: 1.6;
    color: var(--el-text-color-regular);
  }

  &__failed-task {
    max-width: 100%;
    overflow: hidden;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__image {
    max-width: 100%;
    max-height: calc(100% - 60px);
    border-radius: 8px;
    overflow: hidden;

    &-loading {
      width: 200px;
      height: 200px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--el-fill-color-light);
    }
  }

  &__video {
    max-width: 100%;
    max-height: calc(100% - 60px);
    border-radius: 8px;
  }

  &__audio {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    padding: 40px;
    background: var(--el-fill-color-light);
    border-radius: 12px;

    &-player {
      width: 300px;
    }

    &-cover {
      width: min(520px, 80vw);
      max-height: 300px;
      border-radius: 8px;
      overflow: hidden;
    }
  }

  &__actions {
    display: flex;
    gap: 12px;
  }
}
</style>

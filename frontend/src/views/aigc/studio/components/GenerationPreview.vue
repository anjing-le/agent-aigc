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
        </div>

        <!-- 优化后的提示词 -->
        <div v-if="task?.agentAnalysis?.optimizedPrompt" class="generation-preview__optimized-prompt">
          <div class="prompt-label">
            <el-icon><MagicStick /></el-icon>
            优化后的提示词
          </div>
          <div class="prompt-content">{{ task.agentAnalysis.optimizedPrompt }}</div>
        </div>
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
        <video
          :src="result.url"
          controls
          class="generation-preview__video"
        />
      </template>

      <!-- 音频结果 (后端返回大写 AUDIO) -->
      <template v-else-if="isAudio">
        <div class="generation-preview__audio">
          <el-icon :size="48"><Headset /></el-icon>
          <audio :src="result.url" controls class="generation-preview__audio-player" />
        </div>
      </template>

      <!-- 操作按钮 -->
      <div class="generation-preview__actions">
        <el-button :icon="Download" @click="handleDownload">下载</el-button>
        <el-button :icon="Refresh" @click="handleRegenerate">重新生成</el-button>
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
  Edit
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { TaskStatusResponse, AssetItem, ContentType } from '@/api/model/aigcModel'

interface Props {
  task?: TaskStatusResponse | null
  result?: AssetItem | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

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

/** 处理下载 */
const handleDownload = () => {
  if (!props.result?.url) return

  const link = document.createElement('a')
  link.href = props.result.url
  link.download = `aigc-${props.result.id}.${getFileExtension(props.result.contentType)}`
  link.click()
}

/** 获取文件扩展名 */
const getFileExtension = (contentType: string) => {
  const map: Record<string, string> = {
    IMAGE: 'png',
    VIDEO: 'mp4',
    AUDIO: 'mp3'
  }
  return map[contentType?.toUpperCase()] || 'file'
}

/** 处理重新生成 */
const handleRegenerate = () => {
  // 通过事件通知父组件重新生成
  ElMessage.info('请修改描述后重新提交')
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
  }

  &__actions {
    display: flex;
    gap: 12px;
  }
}
</style>


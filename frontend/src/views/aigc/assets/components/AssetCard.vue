<!-- 资产卡片组件 -->
<template>
  <div class="asset-card">
    <!-- 封面 (后端返回大写枚举 IMAGE/VIDEO/AUDIO) -->
    <div class="asset-card__cover" @click="$emit('preview')">
      <el-image
        v-if="isImage"
        :src="item.thumbnailUrl || item.url"
        fit="cover"
        lazy
      >
        <template #placeholder>
          <div class="asset-card__cover-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
        </template>
        <template #error>
          <div class="asset-card__cover-error">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
      </el-image>

      <div v-else class="asset-card__cover-icon">
        <el-icon v-if="isVideo" :size="48"><VideoPlay /></el-icon>
        <el-icon v-else :size="48"><Headset /></el-icon>
      </div>

      <!-- 类型标签 -->
      <el-tag
        :type="getContentTypeTag(item.contentType)"
        size="small"
        class="asset-card__type-tag"
      >
        {{ getContentTypeLabel(item.contentType) }}
      </el-tag>

    </div>

    <!-- 信息 -->
    <div class="asset-card__info">
      <div class="asset-card__prompt">{{ item.prompt }}</div>
      <div class="asset-card__meta">
        <span class="asset-card__time">{{ formatTime(item.createdAt) }}</span>
      </div>
    </div>

    <!-- 操作 -->
    <div class="asset-card__actions">
      <el-button size="small" :icon="View" @click="$emit('preview')">预览</el-button>
      <el-button
        size="small"
        :icon="Delete"
        type="danger"
        plain
        @click="$emit('delete')"
      >
        删除
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Loading,
  Picture,
  VideoPlay,
  Headset,
  View,
  Delete
} from '@element-plus/icons-vue'
import type { AssetItem, ContentType } from '@/api/model/aigcModel'

interface Props {
  item: AssetItem
}

interface Emits {
  preview: []
  delete: []
}

const props = defineProps<Props>()
defineEmits<Emits>()

// 内容类型判断 (忽略大小写)
const isImage = computed(() => props.item.contentType?.toUpperCase() === 'IMAGE')
const isVideo = computed(() => props.item.contentType?.toUpperCase() === 'VIDEO')

/** 获取内容类型标签 */
const getContentTypeTag = (type: ContentType) => {
  const typeUpper = type?.toUpperCase()
  const map: Record<string, '' | 'success' | 'warning' | 'info'> = {
    IMAGE: 'success',
    VIDEO: 'warning',
    AUDIO: 'info'
  }
  return map[typeUpper] || ''
}

/** 获取内容类型文本 */
const getContentTypeLabel = (type: ContentType) => {
  const typeUpper = type?.toUpperCase()
  const map: Record<string, string> = {
    IMAGE: '图片',
    VIDEO: '视频',
    AUDIO: '音频'
  }
  return map[typeUpper] || type
}

/** 格式化时间 */
const formatTime = (time: string) => {
  const date = new Date(time)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
}
</script>

<style lang="scss" scoped>
.asset-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  }

  &__cover {
    position: relative;
    aspect-ratio: 4 / 3;
    background: var(--el-fill-color-light);
    cursor: pointer;

    .el-image {
      width: 100%;
      height: 100%;
    }

    &-loading,
    &-error {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--el-text-color-placeholder);
    }

    &-icon {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, var(--el-color-primary-light-7), var(--el-color-primary-light-5));
      color: var(--el-color-primary);
    }
  }

  &__type-tag {
    position: absolute;
    top: 8px;
    left: 8px;
  }

  &__info {
    padding: 12px 16px;
  }

  &__prompt {
    font-size: 14px;
    color: var(--el-text-color-primary);
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    margin-bottom: 8px;
    min-height: 42px;
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__actions {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}
</style>


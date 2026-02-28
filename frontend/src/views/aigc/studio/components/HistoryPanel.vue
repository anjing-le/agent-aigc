<!-- 历史记录面板组件 -->
<template>
  <div class="history-panel">
    <div class="history-panel__header">
      <span class="history-panel__title">历史记录</span>
      <el-button
        v-if="items.length > 0"
        type="primary"
        link
        size="small"
        @click="$router.push('/aigc/assets')"
      >
        查看全部
      </el-button>
    </div>

    <div class="history-panel__content">
      <!-- 加载状态 -->
      <div v-if="loading" class="history-panel__loading">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- 空状态 -->
      <el-empty
        v-else-if="items.length === 0"
        description="暂无创作记录"
        :image-size="80"
      />

      <!-- 列表 -->
      <div v-else class="history-panel__list">
        <div
          v-for="item in items"
          :key="item.id"
          class="history-panel__item"
          @click="handleSelect(item)"
        >
          <!-- 缩略图 (后端返回大写枚举) -->
          <div class="history-panel__item-thumb">
            <el-image
              v-if="isImage(item.contentType)"
              :src="item.thumbnailUrl || item.url"
              fit="cover"
            >
              <template #error>
                <div class="history-panel__item-thumb-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else-if="isVideo(item.contentType)" class="history-panel__item-thumb-icon">
              <el-icon :size="24"><VideoPlay /></el-icon>
            </div>
            <div v-else class="history-panel__item-thumb-icon">
              <el-icon :size="24"><Headset /></el-icon>
            </div>
          </div>

          <!-- 信息 -->
          <div class="history-panel__item-info">
            <div class="history-panel__item-prompt">{{ item.prompt }}</div>
            <div class="history-panel__item-meta">
              <el-tag :type="getContentTypeTag(item.contentType)" size="small">
                {{ getContentTypeLabel(item.contentType) }}
              </el-tag>
              <span class="history-panel__item-time">{{ formatTime(item.createdAt) }}</span>
            </div>
          </div>

          <!-- 操作 -->
          <div class="history-panel__item-actions">
            <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, item)">
              <el-button :icon="MoreFilled" circle size="small" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="download">
                    <el-icon><Download /></el-icon>
                    下载
                  </el-dropdown-item>
                  <el-dropdown-item v-if="!item.isPublished" command="share">
                    <el-icon><Share /></el-icon>
                    分享
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Picture,
  VideoPlay,
  Headset,
  MoreFilled,
  Download,
  Share,
  Delete
} from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import type { AssetItem, ContentType } from '@/api/model/aigcModel'
import { fetchSaveToGallery, fetchDeleteAsset } from '@/api/aigc'

interface Props {
  items: AssetItem[]
  loading?: boolean
}

interface Emits {
  select: [item: AssetItem]
  delete: [item: AssetItem]
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

/** 判断内容类型 (忽略大小写) */
const isImage = (type: ContentType) => type?.toUpperCase() === 'IMAGE'
const isVideo = (type: ContentType) => type?.toUpperCase() === 'VIDEO'

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
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString()
}

/** 处理选择 */
const handleSelect = (item: AssetItem) => {
  emit('select', item)
}

/** 处理命令 */
const handleCommand = async (command: string, item: AssetItem) => {
  switch (command) {
    case 'download':
      handleDownload(item)
      break
    case 'share':
      await handleShare(item)
      break
    case 'delete':
      await handleDelete(item)
      break
  }
}

/** 处理下载 */
const handleDownload = (item: AssetItem) => {
  const link = document.createElement('a')
  link.href = item.url
  link.download = `aigc-${item.id}`
  link.click()
}

/** 处理分享 */
const handleShare = async (item: AssetItem) => {
  try {
    await fetchSaveToGallery(item.id)
    item.isPublished = true
    ElMessage.success('已分享到灵感广场')
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error('分享失败')
  }
}

/** 处理删除 */
const handleDelete = async (item: AssetItem) => {
  try {
    await ElMessageBox.confirm('确定要删除这个作品吗？', '提示', {
      type: 'warning'
    })

    await fetchDeleteAsset(item.id)
    emit('delete', item)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.history-panel {
  height: 100%;
  background: var(--el-bg-color);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  &__title {
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }

  &__content {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
  }

  &__loading {
    padding: 16px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__item {
    display: flex;
    gap: 12px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &-thumb {
      width: 60px;
      height: 60px;
      border-radius: 6px;
      overflow: hidden;
      flex-shrink: 0;

      .el-image {
        width: 100%;
        height: 100%;
      }

      &-icon {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--el-fill-color);
        color: var(--el-text-color-secondary);
      }

      &-error {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--el-fill-color);
        color: var(--el-text-color-placeholder);
      }
    }

    &-info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    &-prompt {
      font-size: 13px;
      color: var(--el-text-color-primary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-bottom: 6px;
    }

    &-meta {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    &-time {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    &-actions {
      display: flex;
      align-items: center;
    }
  }
}
</style>


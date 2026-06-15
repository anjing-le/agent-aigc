<!-- 提示词卡片组件 - Banana风格 -->
<template>
  <div class="prompt-card" @click="handleCardClick">
    <!-- 预览图 -->
    <div class="prompt-card__preview">
      <img v-if="shouldShowPreviewImage" :src="previewUrl" :alt="item.prompt" loading="lazy" />
      <div v-else class="prompt-card__placeholder">
        <el-icon :size="32"><Picture /></el-icon>
      </div>

      <!-- 内容类型标签 -->
      <div class="prompt-card__type-badge">
        {{ getTypeLabel(item.contentType) }}
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="prompt-card__content">
      <!-- 标题/简述 -->
      <h3 class="prompt-card__title">{{ item.title || truncatePrompt(item.prompt, 30) }}</h3>

      <!-- 提示词预览 -->
      <p class="prompt-card__prompt">{{ item.prompt }}</p>

      <!-- 底部信息 -->
      <div class="prompt-card__footer">
        <div class="prompt-card__author">
          <el-icon><User /></el-icon>
          <span>{{ item.author || '匿名用户' }}</span>
        </div>

        <div class="prompt-card__actions">
          <!-- 分类标签 -->
          <el-tag v-if="item.category" size="small" effect="plain" class="prompt-card__category">
            {{ getCategoryLabel(item.category) }}
          </el-tag>

          <el-button
            size="small"
            :icon="Star"
            :type="item.likedByCurrentUser ? 'warning' : undefined"
            plain
            class="prompt-card__like-btn"
            @click.stop="handleLike"
          >
            {{ item.likeCount || 0 }}
          </el-button>

          <el-button
            size="small"
            :icon="Collection"
            :type="item.favoritedByCurrentUser ? 'success' : undefined"
            plain
            class="prompt-card__favorite-btn"
            @click.stop="handleFavorite"
          >
            {{ item.favoriteCount || 0 }}
          </el-button>

          <el-tooltip content="下载作品" placement="top">
            <el-button
              size="small"
              :icon="Download"
              plain
              class="prompt-card__download-btn"
              @click.stop="handleDownload"
            />
          </el-tooltip>

          <el-tooltip content="复制公开链接" placement="top">
            <el-button
              size="small"
              :icon="Share"
              plain
              class="prompt-card__share-btn"
              @click.stop="handleShare"
            />
          </el-tooltip>

          <!-- 复制按钮 -->
          <el-button
            type="primary"
            size="small"
            :icon="DocumentCopy"
            class="prompt-card__copy-btn"
            @click.stop="handleCopy"
          >
            复制
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import {
    Picture,
    User,
    DocumentCopy,
    Star,
    Collection,
    Download,
    Share
  } from '@element-plus/icons-vue'
  import type { GalleryItem, ContentType } from '@/api/model/aigcModel'
  import { resolveAigcGalleryPreviewUrl } from '@/utils/aigcAsset'

  interface Props {
    item: GalleryItem
  }

  interface Emits {
    copy: [item: GalleryItem]
    use: [item: GalleryItem]
    like: [item: GalleryItem]
    favorite: [item: GalleryItem]
    download: [item: GalleryItem]
    share: [item: GalleryItem]
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()
  const previewUrl = computed(() => resolveAigcGalleryPreviewUrl(props.item))
  const shouldShowPreviewImage = computed(
    () => props.item.contentType === 'IMAGE' && Boolean(previewUrl.value)
  )

  /** 获取内容类型标签 */
  const getTypeLabel = (type: ContentType) => {
    const map: Record<ContentType, string> = {
      IMAGE: '图片',
      VIDEO: '视频',
      AUDIO: '音频'
    }
    return map[type] || type
  }

  /** 获取分类标签 - 直接返回分类名称 */
  const getCategoryLabel = (category: string) => {
    // Banana 数据的分类已经是中文，直接返回
    return category
  }

  /** 截断提示词 */
  const truncatePrompt = (prompt: string, maxLength: number) => {
    if (prompt.length <= maxLength) return prompt
    return prompt.slice(0, maxLength) + '...'
  }

  /** 点击卡片 */
  const handleCardClick = () => {
    emit('use', props.item)
  }

  /** 复制提示词 */
  const handleCopy = () => {
    emit('copy', props.item)
  }

  const handleLike = () => {
    emit('like', props.item)
  }

  const handleFavorite = () => {
    emit('favorite', props.item)
  }

  const handleDownload = () => {
    emit('download', props.item)
  }

  const handleShare = () => {
    emit('share', props.item)
  }
</script>

<style lang="scss" scoped>
  .prompt-card {
    overflow: hidden;
    cursor: pointer;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;
    transition:
      border-color 0.2s,
      box-shadow 0.2s;

    &:hover {
      border-color: var(--el-color-primary-light-5);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);

      .prompt-card__copy-btn {
        opacity: 1;
      }
    }

    &__preview {
      position: relative;
      width: 100%;
      height: 160px;
      overflow: hidden;
      background: var(--el-fill-color-light);

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.3s;
      }

      &:hover img {
        transform: scale(1.05);
      }
    }

    &__placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--el-text-color-placeholder);
    }

    &__type-badge {
      position: absolute;
      top: 12px;
      right: 12px;
      padding: 4px 12px;
      background: rgba(0, 0, 0, 0.6);
      color: #fff;
      font-size: 12px;
      border-radius: 6px;
    }

    &__content {
      padding: 16px;
    }

    &__title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 6px;
      color: var(--el-text-color-primary);
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    &__prompt {
      font-size: 13px;
      line-height: 1.5;
      margin-bottom: 12px;
      color: var(--el-text-color-regular);
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      min-height: 38px;
    }

    &__footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    &__author {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--el-text-color-secondary);

      .el-icon {
        font-size: 14px;
      }
    }

    &__actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    &__category {
      border-radius: 6px;
    }

    &__copy-btn {
      opacity: 0.8;
      transition: opacity 0.2s;
    }
  }
</style>

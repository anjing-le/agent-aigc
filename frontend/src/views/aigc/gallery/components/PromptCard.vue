<!-- 提示词卡片组件 - Banana风格 -->
<template>
  <div class="prompt-card" @click="handleCardClick">
    <!-- 预览图 -->
    <div class="prompt-card__preview">
      <img 
        v-if="item.thumbnailUrl" 
        :src="item.thumbnailUrl" 
        :alt="item.prompt"
        loading="lazy"
      />
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
          <el-tag 
            v-if="item.category" 
            size="small" 
            effect="plain"
            class="prompt-card__category"
          >
            {{ getCategoryLabel(item.category) }}
          </el-tag>
          
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
import { Picture, User, DocumentCopy } from '@element-plus/icons-vue'
import type { GalleryItem, ContentType } from '@/api/model/aigcModel'

interface Props {
  item: GalleryItem
}

interface Emits {
  copy: [item: GalleryItem]
  use: [item: GalleryItem]
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

/** 获取内容类型标签 */
const getTypeLabel = (type: ContentType) => {
  const map: Record<ContentType, string> = {
    image: '图片',
    video: '视频',
    audio: '音频'
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
</script>

<style lang="scss" scoped>
.prompt-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #eee;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);

    .prompt-card__copy-btn {
      opacity: 1;
    }
  }

  &__preview {
    position: relative;
    width: 100%;
    height: 160px;
    background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
    overflow: hidden;

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
    color: #ccc;
  }

  &__type-badge {
    position: absolute;
    top: 12px;
    right: 12px;
    padding: 4px 12px;
    background: rgba(0, 0, 0, 0.6);
    color: #fff;
    font-size: 12px;
    border-radius: 12px;
  }

  &__content {
    padding: 16px;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 6px;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__prompt {
    font-size: 13px;
    color: #666;
    line-height: 1.5;
    margin-bottom: 12px;
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
    color: #999;

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
    border-radius: 12px;
  }

  &__copy-btn {
    opacity: 0.8;
    border-radius: 16px;
    transition: opacity 0.2s;
  }
}
</style>


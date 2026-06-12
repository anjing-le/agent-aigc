<!-- 我的资产页面 -->
<template>
  <div class="aigc-assets">
    <!-- 筛选区 (后端使用大写枚举) -->
    <div class="aigc-assets__filter">
      <el-radio-group v-model="filterContentType" @change="handleFilter">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="IMAGE">图片</el-radio-button>
        <el-radio-button value="VIDEO">视频</el-radio-button>
        <el-radio-button value="AUDIO">音频</el-radio-button>
      </el-radio-group>

      <div class="aigc-assets__filter-right">
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 资产列表 -->
    <div v-loading="loading" class="aigc-assets__content">
      <el-empty v-if="!loading && assetList.length === 0" description="暂无作品">
        <el-button type="primary" @click="$router.push('/aigc/studio')">
          去创作
        </el-button>
      </el-empty>

      <div v-else class="aigc-assets__grid">
        <AssetCard
          v-for="item in assetList"
          :key="item.id"
          :item="item"
          @preview="handlePreview(item)"
          @download="handleDownload(item)"
          @publish="handlePublish(item)"
          @delete="handleDelete(item)"
        />
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="aigc-assets__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 36, 48]"
          layout="total, sizes, prev, pager, next"
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="作品详情"
      width="600px"
      destroy-on-close
    >
      <div v-loading="previewLoading" v-if="previewItem" class="asset-preview">
        <!-- 后端返回大写枚举 IMAGE/VIDEO/AUDIO -->
        <el-image
          v-if="previewItem.contentType?.toUpperCase() === 'IMAGE'"
          :src="previewItem.url"
          :preview-src-list="[previewItem.url]"
          fit="contain"
          class="asset-preview__image"
        />
        <video
          v-else-if="previewItem.contentType?.toUpperCase() === 'VIDEO'"
          :src="previewItem.url"
          controls
          class="asset-preview__video"
        />
        <audio
          v-else
          :src="previewItem.url"
          controls
          class="asset-preview__audio"
        />

        <div class="asset-preview__info">
          <div class="asset-preview__prompt">{{ previewItem.prompt }}</div>
          <div class="asset-preview__meta">
            <el-tag>{{ previewItem.model }}</el-tag>
            <span>{{ formatTime(previewItem.createdAt) }}</span>
          </div>
          <div class="asset-preview__actions">
            <el-button :icon="Download" @click="handleDownload(previewItem)">下载</el-button>
            <el-button
              v-if="!previewItem.isPublished"
              type="primary"
              :icon="Share"
              @click="handlePublish(previewItem)"
            >
              发布到广场
            </el-button>
          </div>
        </div>

        <div v-if="previewTask" class="asset-preview__task">
          <div class="asset-preview__section-title">来源任务</div>
          <div class="asset-preview__task-grid">
            <span>任务ID</span>
            <strong>{{ previewTask.taskId }}</strong>
            <span>状态</span>
            <strong>{{ getTaskStatusLabel(previewTask.status) }}</strong>
            <span>进度</span>
            <strong>{{ previewTask.progress }}%</strong>
          </div>
          <div v-if="previewTask.agentAnalysis" class="asset-preview__agent">
            <div class="asset-preview__section-title">Agent 决策</div>
            <div class="asset-preview__task-grid">
              <span>意图</span>
              <strong>{{ previewTask.agentAnalysis.intent }}</strong>
              <span>模型</span>
              <strong>{{ previewTask.agentAnalysis.selectedModel }}</strong>
              <span>置信度</span>
              <strong>{{ formatConfidence(previewTask.agentAnalysis.confidence) }}</strong>
            </div>
            <div v-if="previewTask.agentAnalysis.cleanPrompt" class="asset-preview__task-prompt">
              <span>清洗 Prompt</span>
              <p>{{ previewTask.agentAnalysis.cleanPrompt }}</p>
            </div>
            <div v-if="agentParamsText" class="asset-preview__task-prompt">
              <span>参数摘要</span>
              <p>{{ agentParamsText }}</p>
            </div>
            <div v-if="previewTask.agentAnalysis.optimizedPrompt" class="asset-preview__task-prompt">
              <span>优化 Prompt</span>
              <p>{{ previewTask.agentAnalysis.optimizedPrompt }}</p>
            </div>
          </div>
          <div v-else-if="previewTask.result?.prompt" class="asset-preview__task-prompt">
            <span>原始 Prompt</span>
            <p>{{ previewTask.result.prompt }}</p>
          </div>
          <div v-if="previewTask.referenceMaterials?.length" class="asset-preview__materials">
            <div class="asset-preview__section-title">参考素材</div>
            <div class="asset-preview__material-list">
              <button
                v-for="material in previewTask.referenceMaterials"
                :key="material.id"
                class="asset-preview__material"
                type="button"
                @click="openUrl(material.url)"
              >
                <el-image
                  v-if="isImageMaterial(material.contentType)"
                  :src="material.url"
                  fit="cover"
                />
                <video
                  v-else-if="isVideoMaterial(material.contentType)"
                  :src="material.url"
                  muted
                  playsinline
                />
                <span>{{ material.originalFileName || material.fileName }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Download, Refresh, Share } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AssetCard from './components/AssetCard.vue'
import { fetchGetAssetList, fetchGetAssetDetail, fetchDeleteAsset, fetchSaveToGallery } from '@/api/aigc'
import type { AssetItem, ContentType, TaskStatus, TaskStatusResponse } from '@/api/model/aigcModel'
import { formatDateTime } from '@/utils/time'
import { downloadAigcAsset } from '@/utils/aigcAsset'

defineOptions({ name: 'AIGCAssets' })

// ==================== 状态 ====================
const loading = ref(false)
const assetList = ref<AssetItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(24)
const filterContentType = ref<ContentType | ''>('')

// 预览
const previewVisible = ref(false)
const previewItem = ref<AssetItem | null>(null)
const previewTask = ref<TaskStatusResponse | null>(null)
const previewLoading = ref(false)

// ==================== 方法 ====================

/** 加载数据 */
const loadData = async () => {
  try {
    loading.value = true
    const response = await fetchGetAssetList({
      current: currentPage.value,
      size: pageSize.value,
      contentType: filterContentType.value || undefined
    })
    assetList.value = response.records
    total.value = response.total
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

/** 筛选 */
const handleFilter = () => {
  currentPage.value = 1
  loadData()
}

/** 分页变化 */
const handlePageChange = () => {
  loadData()
}

/** 预览 */
const handlePreview = async (item: AssetItem) => {
  previewItem.value = item
  previewTask.value = null
  previewVisible.value = true
  previewLoading.value = true
  try {
    const detail = await fetchGetAssetDetail(item.id)
    previewItem.value = detail.asset || item
    previewTask.value = detail.task || null
  } catch (error) {
    console.error('加载资产详情失败:', error)
    ElMessage.warning('资产详情加载失败，已展示基础信息')
  } finally {
    previewLoading.value = false
  }
}

/** 下载 */
const handleDownload = (item: AssetItem) => {
  downloadAigcAsset(item)
}

/** 发布到广场 */
const handlePublish = async (item: AssetItem) => {
  try {
    await fetchSaveToGallery(item.id)
    item.isPublished = true
    if (previewItem.value?.id === item.id) {
      previewItem.value.isPublished = true
    }
    ElMessage.success('已发布到灵感广场')
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败')
  }
}

/** 删除 */
const handleDelete = async (item: AssetItem) => {
  try {
    await ElMessageBox.confirm('确定要删除这个作品吗？', '提示', {
      type: 'warning'
    })

    await fetchDeleteAsset(item.id)
    assetList.value = assetList.value.filter(a => a.id !== item.id)
    total.value--
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

/** 格式化时间 */
const formatTime = (time: string) => formatDateTime(time)

const getTaskStatusLabel = (status: TaskStatus) => {
  const map: Record<TaskStatus, string> = {
    PENDING: '排队中',
    PROCESSING: '生成中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status
}

const isImageMaterial = (contentType: string) => contentType.startsWith('image/')
const isVideoMaterial = (contentType: string) => contentType.startsWith('video/')
const openUrl = (url: string) => window.open(url, '_blank', 'noopener,noreferrer')

const formatConfidence = (confidence?: number) => {
  if (confidence === undefined || confidence === null) return '-'
  return `${Math.round(confidence * 100)}%`
}

const agentParamsText = computed(() => {
  const analysis = previewTask.value?.agentAnalysis
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

// ==================== 生命周期 ====================
onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.aigc-assets {
  padding: 20px;
  background: var(--el-bg-color-page);
  min-height: calc(100vh - 120px);

  &__filter {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 16px;
    background: var(--el-bg-color);
    border-radius: 12px;
    border: 1px solid var(--el-border-color-light);
  }

  &__content {
    min-height: 400px;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: 24px;
    padding: 16px;
    background: var(--el-bg-color);
    border-radius: 12px;
  }
}

.asset-preview {
  &__image {
    width: 100%;
    max-height: 400px;
  }

  &__video {
    width: 100%;
  }

  &__audio {
    width: 100%;
    margin: 40px 0;
  }

  &__info {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid var(--el-border-color-light);
  }

  &__prompt {
    font-size: 14px;
    line-height: 1.6;
    color: var(--el-text-color-primary);
    margin-bottom: 12px;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__actions {
    display: flex;
    gap: 10px;
    margin-top: 16px;
  }

  &__task {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid var(--el-border-color-light);
  }

  &__section-title {
    margin-bottom: 12px;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__task-grid {
    display: grid;
    grid-template-columns: 80px 1fr;
    gap: 8px 12px;
    font-size: 13px;

    span {
      color: var(--el-text-color-secondary);
    }

    strong {
      min-width: 0;
      overflow: hidden;
      color: var(--el-text-color-primary);
      font-weight: 500;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__task-prompt {
    margin-top: 14px;

    span {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    p {
      margin-top: 6px;
      font-size: 13px;
      line-height: 1.6;
      color: var(--el-text-color-primary);
    }
  }

  &__materials {
    margin-top: 16px;
  }

  &__agent {
    margin-top: 16px;
  }

  &__material-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
    gap: 10px;
  }

  &__material {
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

    .el-image,
    video {
      width: 100%;
      aspect-ratio: 4 / 3;
      object-fit: cover;
      background: var(--el-fill-color-light);
    }

    span {
      overflow: hidden;
      padding: 0 8px 8px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>

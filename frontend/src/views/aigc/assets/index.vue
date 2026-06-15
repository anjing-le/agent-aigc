<!-- 我的资产页面 -->
<template>
  <div class="aigc-assets">
    <div class="aigc-assets__header">
      <div>
        <div class="aigc-assets__eyebrow">Asset Center</div>
        <h2 class="aigc-assets__title">我的资产</h2>
        <p class="aigc-assets__subtitle">统一管理生成作品、来源任务、发布状态和下载分发</p>
      </div>
      <div class="aigc-assets__header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
        <el-button type="primary" :icon="MagicStick" @click="$router.push('/aigc/studio')">
          去创作
        </el-button>
      </div>
    </div>

    <div class="aigc-assets__stats">
      <div v-for="item in assetStats" :key="item.label" class="aigc-assets__stat">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <div v-if="storageStatus" class="aigc-assets__storage">
      <div class="aigc-assets__storage-main">
        <span>存储</span>
        <strong>{{ formatStorageMode(storageStatus.activeMode) }}</strong>
        <el-tag size="small" :type="getStorageTagType(storageStatus)" effect="plain">
          {{ storageStatus.message }}
        </el-tag>
      </div>
      <div class="aigc-assets__storage-grid">
        <div>
          <span>本地目录</span>
          <strong>{{ storageStatus.local?.basePath || '-' }}</strong>
        </div>
        <div>
          <span>写入</span>
          <strong>{{ formatBooleanStatus(storageStatus.local?.writable) }}</strong>
        </div>
        <div>
          <span>清理</span>
          <strong>{{ formatBooleanStatus(storageStatus.assetCleanupSupported) }}</strong>
        </div>
        <div>
          <span>访问前缀</span>
          <strong>{{ storageStatus.local?.urlPrefix || '-' }}</strong>
        </div>
        <div>
          <span>OSS</span>
          <strong>{{ storageStatus.oss?.message || '-' }}</strong>
        </div>
      </div>
    </div>

    <section class="aigc-assets__audit">
      <div class="aigc-assets__audit-header">
        <div>
          <h3>最近存储审计</h3>
          <p>上传、删除和清理操作的请求上下文记录</p>
        </div>
        <el-tag size="small" effect="plain">{{ storageAuditLogs.length }} 条</el-tag>
      </div>
      <el-table
        v-loading="storageAuditLoading"
        :data="storageAuditLogs"
        size="small"
        empty-text="暂无存储审计"
      >
        <el-table-column label="动作" min-width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="getStorageAuditActionTag(row.action)" effect="plain">
              {{ formatStorageAuditAction(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="后端" min-width="80">
          <template #default="{ row }">{{ formatStorageMode(row.backend) }}</template>
        </el-table-column>
        <el-table-column label="对象" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ formatStorageAuditTarget(row) }}</template>
        </el-table-column>
        <el-table-column label="大小" min-width="88">
          <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
        </el-table-column>
        <el-table-column label="结果" min-width="84">
          <template #default="{ row }">
            <el-tag size="small" :type="row.success ? 'success' : 'danger'" effect="plain">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.operatorName || row.operatorId || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <!-- 筛选区 (后端使用大写枚举) -->
    <div class="aigc-assets__filter">
      <div class="aigc-assets__filter-left">
        <el-radio-group v-model="filterContentType" @change="handleFilter">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="IMAGE">图片</el-radio-button>
          <el-radio-button value="VIDEO">视频</el-radio-button>
          <el-radio-button value="AUDIO">音频</el-radio-button>
        </el-radio-group>

        <el-select v-model="publishStatus" placeholder="发布状态" class="aigc-assets__status">
          <el-option label="全部状态" value="ALL" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="未发布" value="UNPUBLISHED" />
        </el-select>

        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索 Prompt / 模型"
          class="aigc-assets__search"
        />
      </div>

      <div class="aigc-assets__filter-right">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="card" title="卡片视图">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
          <el-radio-button value="table" title="表格视图">
            <el-icon><List /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 资产列表 -->
    <div v-loading="loading" class="aigc-assets__content">
      <el-empty v-if="!loading && filteredAssetList.length === 0" description="暂无作品">
        <el-button type="primary" @click="$router.push('/aigc/studio')"> 去创作 </el-button>
      </el-empty>

      <div v-else-if="viewMode === 'card'" class="aigc-assets__grid">
        <AssetCard
          v-for="item in filteredAssetList"
          :key="item.id"
          :item="item"
          @preview="handlePreview(item)"
          @download="handleDownload(item)"
          @publish="handlePublish(item)"
          @delete="handleDelete(item)"
        />
      </div>

      <div v-else class="aigc-assets__table">
        <el-table :data="filteredAssetList" row-key="id">
          <el-table-column label="作品" min-width="280">
            <template #default="{ row }">
              <div class="aigc-assets__table-asset">
                <div class="aigc-assets__table-thumb">
                  <el-image
                    v-if="isImage(row) || hasVisualPreview(row)"
                    :src="row.thumbnailUrl || row.url"
                    fit="cover"
                  />
                  <el-icon v-else-if="row.contentType === 'VIDEO'"><VideoPlay /></el-icon>
                  <el-icon v-else><Headset /></el-icon>
                </div>
                <div class="aigc-assets__table-main">
                  <span>{{ row.prompt }}</span>
                  <small>{{ row.id }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="92">
            <template #default="{ row }">
              <el-tag :type="getContentTypeTag(row.contentType)" size="small" effect="plain">
                {{ getContentTypeLabel(row.contentType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="model" label="模型" min-width="150" show-overflow-tooltip />
          <el-table-column label="发布" width="92">
            <template #default="{ row }">
              <el-tag :type="row.isPublished ? 'success' : 'info'" size="small" effect="plain">
                {{ row.isPublished ? '已发布' : '未发布' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="View" @click="handlePreview(row)">预览</el-button>
              <el-button size="small" :icon="Download" @click="handleDownload(row)">下载</el-button>
              <el-button
                v-if="!row.isPublished"
                size="small"
                type="primary"
                plain
                :icon="Share"
                @click="handlePublish(row)"
              >
                发布
              </el-button>
              <el-button size="small" type="danger" plain :icon="Delete" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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
    <el-dialog v-model="previewVisible" title="作品详情" width="600px" destroy-on-close>
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
        <audio v-else :src="previewItem.url" controls class="asset-preview__audio" />

        <div class="asset-preview__info">
          <div class="asset-preview__prompt">{{ previewItem.prompt }}</div>
          <div class="asset-preview__meta">
            <el-tag>{{ previewItem.model }}</el-tag>
            <span>{{ formatTime(previewItem.createdAt) }}</span>
          </div>
          <div class="asset-preview__actions">
            <el-button :icon="Download" @click="handleDownload(previewItem)">下载</el-button>
            <el-button :icon="MagicStick" @click="handleReuse(previewItem)">复用 Prompt</el-button>
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
          <div v-if="providerExecutionItems.length" class="asset-preview__observe">
            <div
              v-for="item in providerExecutionItems"
              :key="item.label"
              class="asset-preview__observe-item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
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
            <div
              v-if="previewTask.agentAnalysis.optimizedPrompt"
              class="asset-preview__task-prompt"
            >
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
  import {
    Delete,
    Download,
    Grid,
    Headset,
    List,
    MagicStick,
    Refresh,
    Search,
    Share,
    VideoPlay,
    View
  } from '@element-plus/icons-vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import AssetCard from './components/AssetCard.vue'
  import {
    fetchGetAssetList,
    fetchGetAssetDetail,
    fetchDeleteAsset,
    fetchGetStorageAuditLogs,
    fetchGetStorageStatus,
    fetchSaveToGallery
  } from '@/api/aigc'
  import type {
    AssetItem,
    ContentType,
    ProviderExecutionSummary,
    StorageAuditLogItem,
    StorageStatusResponse,
    TaskStatus,
    TaskStatusResponse
  } from '@/api/model/aigcModel'
  import { formatDateTime } from '@/utils/time'
  import { downloadAigcAsset } from '@/utils/aigcAsset'

  defineOptions({ name: 'AIGCAssets' })

  const router = useRouter()

  // ==================== 状态 ====================
  const loading = ref(false)
  const assetList = ref<AssetItem[]>([])
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(24)
  const filterContentType = ref<ContentType | ''>('')
  const publishStatus = ref<'ALL' | 'PUBLISHED' | 'UNPUBLISHED'>('ALL')
  const keyword = ref('')
  const viewMode = ref<'card' | 'table'>('card')
  const storageStatus = ref<StorageStatusResponse | null>(null)
  const storageAuditLogs = ref<StorageAuditLogItem[]>([])
  const storageAuditLoading = ref(false)

  // 预览
  const previewVisible = ref(false)
  const previewItem = ref<AssetItem | null>(null)
  const previewTask = ref<TaskStatusResponse | null>(null)
  const previewLoading = ref(false)

  const filteredAssetList = computed(() => {
    const searchText = keyword.value.trim().toLowerCase()
    return assetList.value.filter((item) => {
      const matchesKeyword =
        !searchText ||
        item.prompt.toLowerCase().includes(searchText) ||
        item.model.toLowerCase().includes(searchText)
      const matchesPublishStatus =
        publishStatus.value === 'ALL' ||
        (publishStatus.value === 'PUBLISHED' && item.isPublished) ||
        (publishStatus.value === 'UNPUBLISHED' && !item.isPublished)
      return matchesKeyword && matchesPublishStatus
    })
  })

  const assetStats = computed(() => [
    {
      label: '当前页作品',
      value: `${assetList.value.length}`
    },
    {
      label: '已发布',
      value: `${assetList.value.filter((item) => item.isPublished).length}`
    },
    {
      label: '待发布',
      value: `${assetList.value.filter((item) => !item.isPublished).length}`
    },
    {
      label: '筛选结果',
      value: `${filteredAssetList.value.length}`
    }
  ])

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

  const loadStorageStatus = async () => {
    try {
      storageStatus.value = await fetchGetStorageStatus()
    } catch (error) {
      console.error('加载存储状态失败:', error)
    }
  }

  const loadStorageAuditLogs = async () => {
    try {
      storageAuditLoading.value = true
      const result = await fetchGetStorageAuditLogs({ current: 1, size: 8 })
      storageAuditLogs.value = result.records || []
    } catch (error) {
      console.error('加载存储审计失败:', error)
    } finally {
      storageAuditLoading.value = false
    }
  }

  const handleRefresh = () => {
    loadData()
    loadStorageStatus()
    loadStorageAuditLogs()
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

  const handleReuse = (item: AssetItem) => {
    router.push({
      path: '/aigc/studio',
      query: {
        prompt: item.prompt,
        contentType: item.contentType
      }
    })
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
      assetList.value = assetList.value.filter((a) => a.id !== item.id)
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

  const isImage = (item: AssetItem) => item.contentType?.toUpperCase() === 'IMAGE'
  const hasVisualPreview = (item: AssetItem) => {
    return item.url?.startsWith('data:image/') || item.thumbnailUrl?.startsWith('data:image/')
  }

  const getContentTypeTag = (type: ContentType) => {
    const typeUpper = type?.toUpperCase()
    const map: Record<string, 'success' | 'warning' | 'info'> = {
      IMAGE: 'success',
      VIDEO: 'warning',
      AUDIO: 'info'
    }
    return map[typeUpper]
  }

  const getContentTypeLabel = (type: ContentType) => {
    const typeUpper = type?.toUpperCase()
    const map: Record<string, string> = {
      IMAGE: '图片',
      VIDEO: '视频',
      AUDIO: '音频'
    }
    return map[typeUpper] || type
  }

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

  const providerExecutionItems = computed(() => {
    const execution = previewTask.value?.providerExecution
    if (!execution) return []

    return [
      { label: 'Provider', value: execution.providerName || execution.providerType },
      { label: '模型', value: execution.model },
      { label: '耗时', value: formatDuration(execution.durationMs) },
      { label: '成本', value: formatCost(execution) },
      { label: '成本说明', value: execution.costDescription }
    ].filter((item): item is { label: string; value: string } => Boolean(item.value))
  })

  const formatDuration = (durationMs?: number) => {
    if (durationMs === undefined || durationMs === null) return ''
    if (durationMs < 1000) return `${durationMs}ms`
    return `${(durationMs / 1000).toFixed(1)}s`
  }

  const formatCost = (execution: ProviderExecutionSummary) => {
    if (execution.estimatedCostAmount !== undefined && execution.estimatedCostAmount !== null) {
      const currency = execution.estimatedCostCurrency || 'USD'
      const unit = execution.costUnit ? `/${execution.costUnit}` : ''
      return `${currency} ${Number(execution.estimatedCostAmount).toFixed(6)}${unit}`
    }
    return formatCostStatus(execution.costStatus)
  }

  const formatCostStatus = (status?: string) => {
    const map: Record<string, string> = {
      PENDING: '统计中',
      MOCK_FREE: '模拟免费',
      ESTIMATED: '已估算',
      ESTIMATE_NOT_CONFIGURED: '待配置',
      UNTRACKED: '待接入'
    }
    return status ? map[status] || status : ''
  }

  const formatStorageMode = (mode?: string) => {
    const map: Record<string, string> = {
      LOCAL: '本地',
      OSS: 'OSS'
    }
    return mode ? map[mode] || mode : '-'
  }

  const formatBooleanStatus = (value?: boolean) => {
    if (value === undefined || value === null) return '-'
    return value ? '是' : '否'
  }

  const getStorageTagType = (status: StorageStatusResponse) => {
    if (status.local?.available) return 'success'
    if (status.oss?.enabled && !status.oss.configured) return 'warning'
    return 'danger'
  }

  const formatStorageAuditAction = (action?: string) => {
    if (action === 'upload') return '上传'
    if (action === 'delete-file') return '删文件'
    if (action === 'delete-url') return '删 URL'
    return action || '-'
  }

  const getStorageAuditActionTag = (action?: string) => {
    if (action === 'upload') return 'success'
    if (action === 'delete-file') return 'warning'
    if (action === 'delete-url') return 'warning'
    return 'info'
  }

  const formatStorageAuditTarget = (row: StorageAuditLogItem) => {
    if (row.directory || row.fileName) {
      return [row.directory, row.fileName].filter(Boolean).join('/')
    }
    return row.url || '-'
  }

  const formatBytes = (value?: number) => {
    if (value === undefined || value === null) return '-'
    if (value < 1024) return `${value} B`
    if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
    return `${(value / 1024 / 1024).toFixed(1)} MB`
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
    loadStorageStatus()
    loadStorageAuditLogs()
  })
</script>

<style lang="scss" scoped>
  .aigc-assets {
    padding: 20px;
    background: var(--el-bg-color-page);
    min-height: calc(100vh - 120px);

    &__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 16px;
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

    &__header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;
    }

    &__stats {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 16px;
    }

    &__stat {
      min-height: 72px;
      padding: 14px 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;

      span {
        display: block;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }

      strong {
        display: block;
        margin-top: 8px;
        font-size: 24px;
        line-height: 1;
        color: var(--el-text-color-primary);
      }
    }

    &__storage {
      display: grid;
      grid-template-columns: 220px minmax(0, 1fr);
      gap: 12px;
      margin-bottom: 16px;
      padding: 14px 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__storage-main {
      display: flex;
      min-width: 0;
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;

      span {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }

      strong {
        font-size: 20px;
        line-height: 1;
        color: var(--el-text-color-primary);
      }
    }

    &__storage-grid {
      display: grid;
      grid-template-columns: repeat(5, minmax(0, 1fr));
      gap: 10px;

      div {
        min-width: 0;
        padding: 8px 10px;
        border: 1px solid var(--el-border-color-lighter);
        border-radius: 6px;
        background: var(--el-fill-color-light);
      }

      span,
      strong {
        display: block;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      span {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }

      strong {
        margin-top: 4px;
        font-size: 13px;
        font-weight: 500;
        color: var(--el-text-color-primary);
      }
    }

    &__audit {
      margin-bottom: 16px;
      padding: 14px 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__audit-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 12px;

      h3 {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      p {
        margin: 4px 0 0;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__filter {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      margin-bottom: 20px;
      padding: 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__filter-left {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 0;
      flex-wrap: wrap;
    }

    &__filter-right {
      flex-shrink: 0;
    }

    &__status {
      width: 130px;
    }

    &__search {
      width: 260px;
    }

    &__content {
      min-height: 400px;
    }

    &__grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 20px;
    }

    &__table {
      overflow: hidden;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__table-asset {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 0;
    }

    &__table-thumb {
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      overflow: hidden;
      background: var(--el-fill-color-light);
      border-radius: 6px;
      color: var(--el-text-color-secondary);

      .el-image {
        width: 100%;
        height: 100%;
      }
    }

    &__table-main {
      min-width: 0;

      span,
      small {
        display: block;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      span {
        font-size: 13px;
        color: var(--el-text-color-primary);
      }

      small {
        margin-top: 4px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__pagination {
      display: flex;
      justify-content: center;
      margin-top: 24px;
      padding: 16px;
      background: var(--el-bg-color);
      border-radius: 8px;
    }
  }

  @media screen and (max-width: 1200px) {
    .aigc-assets {
      &__stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      &__storage {
        grid-template-columns: minmax(0, 1fr);
      }

      &__storage-grid {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }

      &__filter {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  }

  @media screen and (max-width: 768px) {
    .aigc-assets {
      padding: 12px;

      &__header {
        align-items: flex-start;
        flex-direction: column;
      }

      &__header-actions,
      &__search {
        width: 100%;
      }

      &__stats {
        grid-template-columns: 1fr;
      }

      &__storage-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
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

    &__observe {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 8px;
      margin-top: 14px;
    }

    &__observe-item {
      min-width: 0;
      padding: 8px;
      border: 1px solid var(--el-border-color-light);
      border-radius: 6px;
      background: var(--el-fill-color-lighter);

      span {
        display: block;
        margin-bottom: 4px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }

      strong {
        display: block;
        overflow: hidden;
        font-size: 13px;
        color: var(--el-text-color-primary);
        font-weight: 500;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
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

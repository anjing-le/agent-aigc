<template>
  <div class="gallery-report">
    <div class="gallery-report__header">
      <div>
        <div class="gallery-report__eyebrow">Gallery Report</div>
        <h2 class="gallery-report__title">互动报表</h2>
        <p class="gallery-report__subtitle">发布、点赞、收藏和公开下载的运营视角</p>
      </div>
      <div class="gallery-report__header-actions">
        <el-button :icon="DataAnalysis" @click="$router.push('/aigc/gallery')">灵感广场</el-button>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadReport">
          刷新
        </el-button>
      </div>
    </div>

    <div class="gallery-report__filters">
      <div class="gallery-report__filter-group">
        <span>内容</span>
        <el-radio-group v-model="activeContentType" @change="loadReport">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="IMAGE">图片</el-radio-button>
          <el-radio-button value="VIDEO">视频</el-radio-button>
          <el-radio-button value="AUDIO">音频</el-radio-button>
        </el-radio-group>
      </div>

      <div class="gallery-report__filter-group">
        <span>窗口</span>
        <el-select v-model="days" class="gallery-report__days" @change="loadReport">
          <el-option v-for="item in daysOptions" :key="item" :label="`${item} 天`" :value="item" />
        </el-select>
      </div>

      <el-tag v-if="report?.generatedAt" effect="plain">
        {{ formatTime(report.generatedAt) }}
      </el-tag>
    </div>

    <div v-loading="loading" class="gallery-report__body">
      <div class="gallery-report__stats">
        <div v-for="item in statItems" :key="item.label" class="gallery-report__stat">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>

      <div class="gallery-report__tables">
        <section class="gallery-report__panel">
          <div class="gallery-report__panel-header">
            <h3>动作分布</h3>
            <el-tag size="small" effect="plain">{{ actionMetrics.length }} 类</el-tag>
          </div>
          <el-table :data="actionMetrics" size="small" empty-text="暂无动作数据">
            <el-table-column label="动作" min-width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="getActionTag(row.action)" effect="plain">
                  {{ formatAction(row.action) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="总数" min-width="90" align="right">
              <template #default="{ row }">{{ formatNumber(row.totalEvents) }}</template>
            </el-table-column>
            <el-table-column label="成功" min-width="90" align="right">
              <template #default="{ row }">{{ formatNumber(row.successfulEvents) }}</template>
            </el-table-column>
            <el-table-column label="成功率" min-width="120">
              <template #default="{ row }">
                {{ formatRate(row.successfulEvents, row.totalEvents) }}
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="gallery-report__panel">
          <div class="gallery-report__panel-header">
            <h3>内容类型</h3>
            <el-tag size="small" effect="plain">{{ contentTypeMetrics.length }} 类</el-tag>
          </div>
          <el-table :data="contentTypeMetrics" size="small" empty-text="暂无内容类型数据">
            <el-table-column label="类型" min-width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="getContentTypeTag(row.contentType)" effect="plain">
                  {{ formatContentType(row.contentType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="总数" min-width="90" align="right">
              <template #default="{ row }">{{ formatNumber(row.totalEvents) }}</template>
            </el-table-column>
            <el-table-column label="成功" min-width="90" align="right">
              <template #default="{ row }">{{ formatNumber(row.successfulEvents) }}</template>
            </el-table-column>
            <el-table-column label="成功率" min-width="120">
              <template #default="{ row }">
                {{ formatRate(row.successfulEvents, row.totalEvents) }}
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>

      <section class="gallery-report__panel gallery-report__panel--wide">
        <div class="gallery-report__panel-header">
          <h3>高互动作品</h3>
          <el-tag size="small" effect="plain">{{ topAssets.length }} 个</el-tag>
        </div>
        <el-table :data="topAssets" size="small" empty-text="暂无作品互动数据">
          <el-table-column prop="assetId" label="资产" min-width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="getContentTypeTag(row.contentType)" effect="plain">
                {{ formatContentType(row.contentType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="model" label="模型" min-width="150" show-overflow-tooltip />
          <el-table-column label="互动" width="92" align="right">
            <template #default="{ row }">{{ formatNumber(row.totalEvents) }}</template>
          </el-table-column>
          <el-table-column label="点赞" width="92" align="right">
            <template #default="{ row }">{{ formatNumber(row.likeCount) }}</template>
          </el-table-column>
          <el-table-column label="收藏" width="92" align="right">
            <template #default="{ row }">{{ formatNumber(row.favoriteCount) }}</template>
          </el-table-column>
          <el-table-column label="下载" width="92" align="right">
            <template #default="{ row }">{{ formatNumber(row.downloadCount) }}</template>
          </el-table-column>
          <el-table-column label="打开" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                :icon="Link"
                :disabled="!row.assetId"
                @click="openSharePage(row.assetId)"
              >
                分享页
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { DataAnalysis, Link, Refresh } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'
  import { fetchGetGalleryInteractionReport } from '@/api/aigc'
  import type {
    ContentType,
    GalleryActionMetric,
    GalleryAssetMetric,
    GalleryContentTypeMetric,
    GalleryInteractionReportResponse
  } from '@/api/model/aigcModel'

  defineOptions({ name: 'AIGCGalleryReport' })

  const router = useRouter()
  const loading = ref(false)
  const days = ref(30)
  const daysOptions = [7, 30, 90, 180, 365]
  const activeContentType = ref<ContentType | ''>('')
  const report = ref<GalleryInteractionReportResponse | null>(null)

  const actionMetrics = computed<GalleryActionMetric[]>(() => report.value?.actionMetrics || [])
  const contentTypeMetrics = computed<GalleryContentTypeMetric[]>(
    () => report.value?.contentTypeMetrics || []
  )
  const topAssets = computed<GalleryAssetMetric[]>(() => report.value?.topAssets || [])

  const statItems = computed(() => [
    { label: '总事件', value: formatNumber(report.value?.totalEvents) },
    { label: '成功事件', value: formatNumber(report.value?.successfulEvents) },
    { label: '发布', value: formatNumber(report.value?.publishCount) },
    { label: '点赞', value: formatNumber(report.value?.likeCount) },
    { label: '收藏', value: formatNumber(report.value?.favoriteCount) },
    { label: '公开下载', value: formatNumber(report.value?.downloadCount) }
  ])

  const loadReport = async () => {
    try {
      loading.value = true
      report.value = await fetchGetGalleryInteractionReport({
        days: days.value,
        contentType: activeContentType.value || undefined
      })
    } catch (error) {
      console.error('加载广场互动报表失败:', error)
      ElMessage.error('报表加载失败')
    } finally {
      loading.value = false
    }
  }

  const formatNumber = (value?: number | null) => Number(value || 0).toLocaleString('zh-CN')

  const formatRate = (successful?: number | null, total?: number | null) => {
    const denominator = Number(total || 0)
    if (denominator <= 0) return '0%'
    return `${Math.round((Number(successful || 0) / denominator) * 100)}%`
  }

  const formatTime = (value?: string) => {
    if (!value) return '-'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return date.toLocaleString('zh-CN', { hour12: false })
  }

  const formatAction = (action?: string) => {
    const labels: Record<string, string> = {
      publish: '发布',
      unpublish: '撤回',
      like: '点赞',
      unlike: '取消点赞',
      favorite: '收藏',
      unfavorite: '取消收藏',
      'public-download': '公开下载'
    }
    return action ? labels[action] || action : '-'
  }

  const getActionTag = (action?: string) => {
    if (action === 'publish') return 'success'
    if (action === 'unpublish') return 'warning'
    if (action === 'like' || action === 'favorite') return 'primary'
    if (action === 'public-download') return 'info'
    return 'info'
  }

  const formatContentType = (contentType?: string) => {
    const labels: Record<string, string> = {
      IMAGE: '图片',
      VIDEO: '视频',
      AUDIO: '音频',
      TEXT: '文本'
    }
    return contentType ? labels[contentType] || contentType : '-'
  }

  const getContentTypeTag = (contentType?: string) => {
    if (contentType === 'IMAGE') return 'success'
    if (contentType === 'VIDEO') return 'warning'
    if (contentType === 'AUDIO') return 'info'
    return 'info'
  }

  const openSharePage = (assetId?: string) => {
    if (!assetId) return
    const route = router.resolve({ path: `/share/gallery/${encodeURIComponent(assetId)}` })
    window.open(`${window.location.origin}${window.location.pathname}${route.href}`, '_blank')
  }

  onMounted(() => {
    loadReport()
  })
</script>

<style lang="scss" scoped>
  .gallery-report {
    min-height: calc(100vh - 120px);
    padding: 20px;
    background: var(--el-bg-color-page);

    &__header {
      display: flex;
      align-items: flex-start;
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

    &__header-actions,
    &__filters,
    &__filter-group,
    &__panel-header {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    &__header-actions {
      flex-shrink: 0;
    }

    &__filters {
      justify-content: space-between;
      flex-wrap: wrap;
      margin-bottom: 16px;
      padding: 14px 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__filter-group {
      span {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__days {
      width: 112px;
    }

    &__body {
      min-height: 360px;
    }

    &__stats {
      display: grid;
      grid-template-columns: repeat(6, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 16px;
    }

    &__stat {
      min-height: 76px;
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
        margin-top: 9px;
        font-size: 24px;
        line-height: 1;
        color: var(--el-text-color-primary);
      }
    }

    &__tables {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 16px;
      margin-bottom: 16px;
    }

    &__panel {
      min-width: 0;
      padding: 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;

      &--wide {
        width: 100%;
      }
    }

    &__panel-header {
      justify-content: space-between;
      margin-bottom: 12px;

      h3 {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }
    }
  }

  @media screen and (max-width: 1280px) {
    .gallery-report {
      &__stats {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }

      &__tables {
        grid-template-columns: 1fr;
      }
    }
  }

  @media screen and (max-width: 768px) {
    .gallery-report {
      padding: 12px;

      &__header,
      &__filters {
        align-items: flex-start;
        flex-direction: column;
      }

      &__header-actions {
        width: 100%;
        flex-wrap: wrap;
      }

      &__stats {
        grid-template-columns: 1fr;
      }

      &__filter-group {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  }
</style>

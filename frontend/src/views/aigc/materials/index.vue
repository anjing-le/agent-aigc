<template>
  <div class="aigc-materials">
    <div class="aigc-materials__header">
      <div>
        <div class="aigc-materials__eyebrow">Material Library</div>
        <h2 class="aigc-materials__title">素材库</h2>
        <p class="aigc-materials__subtitle">管理参考图片和视频素材，追踪它们被哪些创作任务引用</p>
      </div>
      <div class="aigc-materials__header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="MagicStick" @click="$router.push('/aigc/studio')">
          去创作
        </el-button>
      </div>
    </div>

    <div class="aigc-materials__stats">
      <div v-for="item in materialStats" :key="item.label" class="aigc-materials__stat">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <div class="aigc-materials__filter">
      <div class="aigc-materials__filter-left">
        <el-radio-group v-model="filterContentType" @change="handleFilter">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="image">图片</el-radio-button>
          <el-radio-button value="video">视频</el-radio-button>
        </el-radio-group>

        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索文件名 / 类型"
          class="aigc-materials__search"
        />
      </div>

      <div class="aigc-materials__filter-right">
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

    <div v-loading="loading" class="aigc-materials__content">
      <el-empty v-if="!loading && filteredMaterialList.length === 0" description="暂无素材">
        <el-button type="primary" @click="$router.push('/aigc/studio')"> 去上传 </el-button>
      </el-empty>

      <div v-else-if="viewMode === 'card'" class="aigc-materials__grid">
        <article v-for="item in filteredMaterialList" :key="item.id" class="material-card">
          <button class="material-card__preview" type="button" @click="handlePreview(item)">
            <el-image
              v-if="isImage(item)"
              :src="resolveAigcMaterialPreviewUrl(item)"
              fit="cover"
              class="material-card__media"
            />
            <video
              v-else
              :src="resolveAigcMaterialPreviewUrl(item)"
              class="material-card__media"
              muted
              playsinline
            />
          </button>

          <div class="material-card__body">
            <div class="material-card__name" :title="item.originalFileName || item.fileName">
              {{ item.originalFileName || item.fileName }}
            </div>
            <div class="material-card__meta">
              <el-tag size="small">{{ item.contentType }}</el-tag>
              <span>{{ formatBytes(item.size) }}</span>
            </div>
            <div class="material-card__usage">
              <span>引用 {{ getTaskTotal(item.id) }} 次</span>
            </div>
            <div class="material-card__footer">
              <span>{{ formatTime(item.createdAt) }}</span>
              <div class="material-card__actions">
                <el-button
                  text
                  :icon="DocumentCopy"
                  @click="handleCopy(resolveAigcMaterialPreviewUrl(item))"
                />
                <el-button text type="danger" :icon="Delete" @click="handleDelete(item)" />
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="aigc-materials__table">
        <el-table :data="filteredMaterialList" row-key="id">
          <el-table-column label="素材" min-width="300">
            <template #default="{ row }">
              <div class="aigc-materials__table-material">
                <button
                  class="aigc-materials__table-thumb"
                  type="button"
                  @click="handlePreview(row)"
                >
                  <el-image
                    v-if="isImage(row)"
                    :src="resolveAigcMaterialPreviewUrl(row)"
                    fit="cover"
                  />
                  <video v-else :src="resolveAigcMaterialPreviewUrl(row)" muted playsinline />
                </button>
                <div class="aigc-materials__table-main">
                  <span>{{ row.originalFileName || row.fileName }}</span>
                  <small>{{ row.id }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag :type="getMaterialTagType(row)" size="small" effect="plain">
                {{ row.contentType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="110">
            <template #default="{ row }">
              {{ formatBytes(row.size) }}
            </template>
          </el-table-column>
          <el-table-column label="引用" width="90">
            <template #default="{ row }"> {{ getTaskTotal(row.id) }} 次 </template>
          </el-table-column>
          <el-table-column label="上传时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="View" @click="handlePreview(row)">预览</el-button>
              <el-button
                size="small"
                :icon="DocumentCopy"
                @click="handleCopy(resolveAigcMaterialPreviewUrl(row))"
              >
                复制
              </el-button>
              <el-button size="small" type="danger" plain :icon="Delete" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="total > 0" class="aigc-materials__pagination">
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

    <el-dialog v-model="previewVisible" title="素材预览" width="680px" destroy-on-close>
      <div v-if="previewItem" class="material-preview">
        <el-image
          v-if="isImage(previewItem)"
          :src="resolveAigcMaterialPreviewUrl(previewItem)"
          :preview-src-list="[resolveAigcMaterialPreviewUrl(previewItem)]"
          fit="contain"
          class="material-preview__image"
        />
        <video
          v-else
          :src="resolveAigcMaterialPreviewUrl(previewItem)"
          controls
          class="material-preview__video"
        />

        <div class="material-preview__info">
          <div>{{ previewItem.originalFileName || previewItem.fileName }}</div>
          <div class="material-preview__meta">
            <el-tag>{{ previewItem.contentType }}</el-tag>
            <span>{{ formatBytes(previewItem.size) }}</span>
            <span>{{ formatTime(previewItem.createdAt) }}</span>
          </div>
        </div>

        <div class="material-preview__tasks">
          <div class="material-preview__section-title">引用任务</div>
          <div v-loading="previewTasksLoading" class="material-preview__task-list">
            <el-empty
              v-if="!previewTasksLoading && previewTasks.length === 0"
              description="暂无引用任务"
              :image-size="80"
            />
            <div
              v-for="task in previewTasks"
              v-else
              :key="task.taskId"
              class="material-preview__task"
            >
              <div class="material-preview__task-main">
                <strong>{{
                  task.agentAnalysis?.intent || task.result?.prompt || task.taskId
                }}</strong>
                <span>{{ task.taskId }}</span>
              </div>
              <div class="material-preview__task-meta">
                <el-tag :type="getTaskStatusTag(task.status)" size="small" effect="plain">
                  {{ getTaskStatusLabel(task.status) }}
                </el-tag>
                <span>{{ task.progress }}%</span>
                <span>{{ formatTime(task.createdAt) }}</span>
              </div>
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
    DocumentCopy,
    Grid,
    List,
    MagicStick,
    Refresh,
    Search,
    View
  } from '@element-plus/icons-vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { fetchDeleteMaterial, fetchGetMaterialList, fetchGetMaterialTasks } from '@/api/aigc'
  import type {
    MaterialItem,
    MaterialSearchParams,
    TaskStatus,
    TaskStatusResponse
  } from '@/api/model/aigcModel'
  import { resolveAigcMaterialPreviewUrl } from '@/utils/aigcAsset'
  import { formatDateTime } from '@/utils/time'

  defineOptions({ name: 'AIGCMaterials' })

  const loading = ref(false)
  const materialList = ref<MaterialItem[]>([])
  const materialTaskTotals = ref<Record<string, number>>({})
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(24)
  const filterContentType = ref<MaterialSearchParams['contentType'] | ''>('')
  const keyword = ref('')
  const viewMode = ref<'card' | 'table'>('card')

  const previewVisible = ref(false)
  const previewItem = ref<MaterialItem | null>(null)
  const previewTasks = ref<TaskStatusResponse[]>([])
  const previewTasksLoading = ref(false)

  const filteredMaterialList = computed(() => {
    const searchText = keyword.value.trim().toLowerCase()
    if (!searchText) return materialList.value

    return materialList.value.filter((item) => {
      const fileName = `${item.originalFileName || ''} ${item.fileName || ''}`.toLowerCase()
      return fileName.includes(searchText) || item.contentType.toLowerCase().includes(searchText)
    })
  })

  const materialStats = computed(() => {
    const imageCount = materialList.value.filter((item) => isImage(item)).length
    const videoCount = materialList.value.filter((item) => !isImage(item)).length
    const usageCount = Object.values(materialTaskTotals.value).reduce(
      (sum, count) => sum + count,
      0
    )

    return [
      { label: '当前页素材', value: `${materialList.value.length}` },
      { label: '图片素材', value: `${imageCount}` },
      { label: '视频素材', value: `${videoCount}` },
      { label: '任务引用', value: `${usageCount}` }
    ]
  })

  const loadData = async () => {
    try {
      loading.value = true
      const response = await fetchGetMaterialList({
        current: currentPage.value,
        size: pageSize.value,
        contentType: filterContentType.value || undefined
      })
      materialList.value = response.records
      total.value = response.total
      loadTaskTotals(response.records)
    } catch (error) {
      console.error('加载素材失败:', error)
      ElMessage.error('加载素材失败')
    } finally {
      loading.value = false
    }
  }

  const loadTaskTotals = async (materials: MaterialItem[]) => {
    materialTaskTotals.value = {}
    await Promise.all(
      materials.map(async (material) => {
        try {
          const response = await fetchGetMaterialTasks(material.id, { current: 1, size: 1 })
          materialTaskTotals.value[material.id] = response.total
        } catch (error) {
          console.error('加载素材引用任务失败:', error)
          materialTaskTotals.value[material.id] = 0
        }
      })
    )
  }

  const handleFilter = () => {
    currentPage.value = 1
    loadData()
  }

  const handlePageChange = () => {
    loadData()
  }

  const handlePreview = async (item: MaterialItem) => {
    previewItem.value = item
    previewTasks.value = []
    previewVisible.value = true
    await loadPreviewTasks(item.id)
  }

  const loadPreviewTasks = async (materialId: string) => {
    try {
      previewTasksLoading.value = true
      const response = await fetchGetMaterialTasks(materialId, { current: 1, size: 5 })
      previewTasks.value = response.records
      materialTaskTotals.value[materialId] = response.total
    } catch (error) {
      console.error('加载素材引用任务失败:', error)
      previewTasks.value = []
    } finally {
      previewTasksLoading.value = false
    }
  }

  const handleCopy = async (url: string) => {
    await navigator.clipboard.writeText(url)
    ElMessage.success('素材地址已复制')
  }

  const handleDelete = async (item: MaterialItem) => {
    try {
      await ElMessageBox.confirm('确定要删除这个素材记录吗？', '提示', {
        type: 'warning'
      })
      await fetchDeleteMaterial(item.id)
      materialList.value = materialList.value.filter((material) => material.id !== item.id)
      total.value = Math.max(0, total.value - 1)
      ElMessage.success('删除成功')
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除素材失败:', error)
        ElMessage.error('删除素材失败')
      }
    }
  }

  const isImage = (item: MaterialItem) => item.contentType.startsWith('image/')
  const getMaterialTagType = (item: MaterialItem) => (isImage(item) ? 'success' : 'warning')

  const getTaskTotal = (materialId: string) => materialTaskTotals.value[materialId] || 0

  const getTaskStatusLabel = (status: TaskStatus) => {
    const map: Record<TaskStatus, string> = {
      PENDING: '排队中',
      PROCESSING: '生成中',
      COMPLETED: '已完成',
      FAILED: '失败'
    }
    return map[status] || status
  }

  const getTaskStatusTag = (status: TaskStatus) => {
    const map: Record<TaskStatus, 'info' | 'warning' | 'success' | 'danger'> = {
      PENDING: 'info',
      PROCESSING: 'warning',
      COMPLETED: 'success',
      FAILED: 'danger'
    }
    return map[status] || 'info'
  }

  const formatTime = (time: string) => formatDateTime(time)

  const formatBytes = (size: number) => {
    if (size < 1024) return `${size} B`
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
    return `${(size / 1024 / 1024).toFixed(1)} MB`
  }

  onMounted(() => {
    loadData()
  })
</script>

<style lang="scss" scoped>
  .aigc-materials {
    min-height: calc(100vh - 120px);
    padding: 20px;
    background: var(--el-bg-color-page);

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

    &__filter {
      display: flex;
      align-items: center;
      justify-content: space-between;
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

    &__search {
      width: 260px;
    }

    &__content {
      min-height: 400px;
    }

    &__grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;
    }

    &__table {
      overflow: hidden;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__table-material {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 0;
    }

    &__table-thumb {
      width: 56px;
      height: 42px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      padding: 0;
      overflow: hidden;
      cursor: pointer;
      background: var(--el-fill-color-light);
      border: 0;
      border-radius: 6px;

      .el-image,
      video {
        width: 100%;
        height: 100%;
        object-fit: cover;
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
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }
  }

  @media screen and (max-width: 1200px) {
    .aigc-materials {
      &__stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      &__filter {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  }

  @media screen and (max-width: 768px) {
    .aigc-materials {
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
    }
  }

  .material-card {
    overflow: hidden;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;

    &__preview {
      display: block;
      width: 100%;
      aspect-ratio: 4 / 3;
      padding: 0;
      overflow: hidden;
      cursor: pointer;
      background: var(--el-fill-color-light);
      border: 0;
    }

    &__media {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &__body {
      padding: 12px;
    }

    &__name {
      overflow: hidden;
      font-size: 14px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &__meta,
    &__usage,
    &__footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      margin-top: 10px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    &__actions {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .material-preview {
    &__image,
    &__video {
      width: 100%;
      max-height: 460px;
    }

    &__info {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid var(--el-border-color-light);
    }

    &__meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 10px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    &__tasks {
      margin-top: 18px;
      padding-top: 18px;
      border-top: 1px solid var(--el-border-color-light);
    }

    &__section-title {
      margin-bottom: 12px;
      font-size: 13px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    &__task-list {
      min-height: 80px;
    }

    &__task {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px solid var(--el-border-color-lighter);
    }

    &__task-main {
      min-width: 0;

      strong,
      span {
        display: block;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      strong {
        font-size: 13px;
        color: var(--el-text-color-primary);
      }

      span {
        margin-top: 4px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__task-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }
</style>

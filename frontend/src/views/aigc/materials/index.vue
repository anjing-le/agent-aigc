<template>
  <div class="aigc-materials">
    <div class="aigc-materials__filter">
      <el-radio-group v-model="filterContentType" @change="handleFilter">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="image">图片</el-radio-button>
        <el-radio-button value="video">视频</el-radio-button>
      </el-radio-group>

      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

    <div v-loading="loading" class="aigc-materials__content">
      <el-empty v-if="!loading && materialList.length === 0" description="暂无素材">
        <el-button type="primary" @click="$router.push('/aigc/studio')">
          去上传
        </el-button>
      </el-empty>

      <div v-else class="aigc-materials__grid">
        <article
          v-for="item in materialList"
          :key="item.id"
          class="material-card"
        >
          <button class="material-card__preview" type="button" @click="handlePreview(item)">
            <el-image
              v-if="isImage(item)"
              :src="item.url"
              fit="cover"
              class="material-card__media"
            />
            <video
              v-else
              :src="item.url"
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
                <el-button text :icon="DocumentCopy" @click="handleCopy(item.url)" />
                <el-button text type="danger" :icon="Delete" @click="handleDelete(item)" />
              </div>
            </div>
          </div>
        </article>
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

    <el-dialog
      v-model="previewVisible"
      title="素材预览"
      width="680px"
      destroy-on-close
    >
      <div v-if="previewItem" class="material-preview">
        <el-image
          v-if="isImage(previewItem)"
          :src="previewItem.url"
          :preview-src-list="[previewItem.url]"
          fit="contain"
          class="material-preview__image"
        />
        <video
          v-else
          :src="previewItem.url"
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
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete, DocumentCopy, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchDeleteMaterial, fetchGetMaterialList, fetchGetMaterialTasks } from '@/api/aigc'
import type { MaterialItem, MaterialSearchParams } from '@/api/model/aigcModel'
import { formatDateTime } from '@/utils/time'

defineOptions({ name: 'AIGCMaterials' })

const loading = ref(false)
const materialList = ref<MaterialItem[]>([])
const materialTaskTotals = ref<Record<string, number>>({})
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(24)
const filterContentType = ref<MaterialSearchParams['contentType'] | ''>('')

const previewVisible = ref(false)
const previewItem = ref<MaterialItem | null>(null)

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
  await Promise.all(materials.map(async material => {
    try {
      const response = await fetchGetMaterialTasks(material.id, { current: 1, size: 1 })
      materialTaskTotals.value[material.id] = response.total
    } catch (error) {
      console.error('加载素材引用任务失败:', error)
      materialTaskTotals.value[material.id] = 0
    }
  }))
}

const handleFilter = () => {
  currentPage.value = 1
  loadData()
}

const handlePageChange = () => {
  loadData()
}

const handlePreview = (item: MaterialItem) => {
  previewItem.value = item
  previewVisible.value = true
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
    materialList.value = materialList.value.filter(material => material.id !== item.id)
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

const getTaskTotal = (materialId: string) => materialTaskTotals.value[materialId] || 0

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

  &__filter {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
    padding: 16px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;
  }

  &__content {
    min-height: 400px;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
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
}
</style>

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
      <div v-if="previewItem" class="asset-preview">
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
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AssetCard from './components/AssetCard.vue'
import { fetchGetAssetList, fetchDeleteAsset } from '@/api/aigc'
import type { AssetItem, ContentType } from '@/api/model/aigcModel'

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
const handlePreview = (item: AssetItem) => {
  previewItem.value = item
  previewVisible.value = true
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
const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
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
}
</style>


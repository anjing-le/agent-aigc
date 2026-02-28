<!-- 灵感广场页面 - 提示词工具库风格 -->
<template>
  <div class="prompt-gallery">
    <!-- 顶部区域 -->
    <div class="prompt-gallery__header">
      <div class="prompt-gallery__title">
        <h1>🎨 提示词灵感库</h1>
        <p>发现优质提示词，一键复制使用</p>
      </div>
    </div>

    <!-- 标签筛选 -->
    <div class="prompt-gallery__tags">
      <el-tag
        v-for="tag in categoryTags"
        :key="tag.value"
        :type="activeTag === tag.value ? '' : 'info'"
        :effect="activeTag === tag.value ? 'dark' : 'plain'"
        class="prompt-gallery__tag"
        @click="handleTagClick(tag.value)"
      >
        {{ tag.label }}
      </el-tag>
    </div>

    <!-- 搜索区 -->
    <div class="prompt-gallery__search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索提示词..."
        :prefix-icon="Search"
        clearable
        size="large"
        class="prompt-gallery__search"
        @input="handleSearch"
      />
    </div>

    <!-- 提示词卡片列表 -->
    <div v-loading="loading" class="prompt-gallery__content">
      <el-empty v-if="!loading && galleryList.length === 0" description="暂无提示词" />

      <div v-else class="prompt-gallery__grid">
        <PromptCard
          v-for="item in galleryList"
          :key="item.id"
          :item="item"
          @copy="handleCopy(item)"
          @use="handleUse(item)"
        />
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="prompt-gallery__loadmore">
        <el-button :loading="loading" @click="loadMore">
          加载更多
        </el-button>
      </div>
    </div>

    <!-- 复制成功提示 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showCopyTip" class="copy-tip">
          ✅ 已复制到剪贴板
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { useDebounceFn, useClipboard } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import PromptCard from './components/PromptCard.vue'
import { fetchGetGalleryList } from '@/api/aigc'
import type { GalleryItem } from '@/api/model/aigcModel'

defineOptions({ name: 'AIGCGallery' })

const router = useRouter()

// ==================== 状态 ====================
const loading = ref(false)
const galleryList = ref<GalleryItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(24)

// 搜索筛选
const searchKeyword = ref('')
const activeTag = ref('all')

// 复制提示
const showCopyTip = ref(false)
const { copy } = useClipboard()

// 数据来源标识：'api' 表示后端接口，'static' 表示静态 JSON 后备
const dataSource = ref<'api' | 'static'>('api')

// 分类标签
const categoryTags = [
  { label: '全部', value: 'all' },
  { label: '有趣', value: '有趣' },
  { label: '工作', value: '工作' },
  { label: '学习', value: '学习' },
  { label: '生活', value: '生活' }
]

// 是否有更多数据
const hasMore = computed(() => galleryList.value.length < total.value)

// ==================== 方法 ====================

// 静态提示词数据缓存（后备方案）
const staticPrompts = ref<GalleryItem[]>([])

/** 加载数据 - 优先调用后端 API，无数据时使用静态后备 */
const loadData = async (append = false) => {
  try {
    loading.value = true

    if (dataSource.value === 'api') {
      await loadFromApi(append)
    } else {
      await loadFromStatic(append)
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

/** 从后端 API 加载数据 */
const loadFromApi = async (append = false) => {
  try {
    const res = await fetchGetGalleryList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      contentType: activeTag.value !== 'all' ? (activeTag.value as any) : undefined
    })

    const records = (res?.records || []).map((item: any) => ({
      ...item,
      likeCount: item.likeCount ?? 0
    }))

    // 后端无已发布作品时，自动切换到静态数据后备
    if (!append && records.length === 0 && currentPage.value === 1 && !searchKeyword.value) {
      console.info('后端暂无已发布作品，使用静态提示词数据')
      dataSource.value = 'static'
      await loadFromStatic(false)
      return
    }

    if (append) {
      galleryList.value = [...galleryList.value, ...records]
    } else {
      galleryList.value = records
    }
    total.value = res?.total ?? 0
  } catch (error) {
    console.warn('API 调用失败，使用静态数据后备:', error)
    dataSource.value = 'static'
    await loadFromStatic(false)
  }
}

/** 从静态 JSON 加载数据（后备方案） */
const loadFromStatic = async (append = false) => {
  await loadStaticPrompts()
  const filtered = filterStaticPrompts()

  if (append) {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    galleryList.value = [...galleryList.value, ...filtered.slice(start, end)]
  } else {
    galleryList.value = filtered.slice(0, pageSize.value)
  }
  total.value = filtered.length
}

/** 加载静态提示词数据 */
const loadStaticPrompts = async () => {
  if (staticPrompts.value.length > 0) return
  
  try {
    const response = await fetch('/data/prompts.json')
    const data = await response.json()
    staticPrompts.value = data.map((item: any) => ({
      ...item,
      contentType: item.contentType || 'image',
      likeCount: Math.floor(Math.random() * 100) + 10,
      url: item.thumbnailUrl || '',
      isPublished: true
    }))
  } catch (error) {
    console.error('加载静态数据失败:', error)
  }
}

/** 筛选静态数据 */
const filterStaticPrompts = () => {
  let result = [...staticPrompts.value]
  
  // 关键词筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(item => 
      item.prompt.toLowerCase().includes(keyword) ||
      (item.title?.toLowerCase().includes(keyword))
    )
  }
  
  // 分类筛选
  if (activeTag.value !== 'all') {
    result = result.filter(item => item.category === activeTag.value)
  }
  
  return result
}

/** 防抖搜索 */
const handleSearch = useDebounceFn(() => {
  currentPage.value = 1
  loadData()
}, 300)

/** 标签点击 */
const handleTagClick = (tag: string) => {
  activeTag.value = tag
  currentPage.value = 1
  // 切回 API 尝试（重新判断数据源）
  dataSource.value = 'api'
  loadData()
}

/** 加载更多 */
const loadMore = () => {
  currentPage.value++
  loadData(true)
}

/** 复制提示词 */
const handleCopy = async (item: GalleryItem) => {
  try {
    await copy(item.prompt)
    showCopyTip.value = true
    setTimeout(() => {
      showCopyTip.value = false
    }, 2000)
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

/** 使用提示词（跳转到创作工作台） */
const handleUse = (item: GalleryItem) => {
  router.push({
    path: '/aigc/studio',
    query: { prompt: item.prompt }
  })
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.prompt-gallery {
  min-height: calc(100vh - 120px);
  padding: 32px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);

  &__header {
    text-align: center;
    margin-bottom: 32px;
  }

  &__title {
    h1 {
      font-size: 32px;
      font-weight: 700;
      color: #1a1a1a;
      margin-bottom: 8px;
    }

    p {
      font-size: 16px;
      color: #666;
    }
  }

  &__tags {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 24px;
  }

  &__tag {
    cursor: pointer;
    padding: 8px 20px;
    font-size: 14px;
    border-radius: 20px;
    transition: all 0.2s;

    &:hover {
      transform: translateY(-2px);
    }
  }

  &__search-bar {
    display: flex;
    justify-content: center;
    margin-bottom: 32px;
  }

  &__search {
    width: 100%;
    max-width: 500px;

    :deep(.el-input__wrapper) {
      border-radius: 24px;
      padding: 4px 20px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }
  }

  &__content {
    max-width: 1400px;
    margin: 0 auto;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  &__loadmore {
    display: flex;
    justify-content: center;
    margin-top: 40px;
  }
}

// 复制成功提示
.copy-tip {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 8px;
  font-size: 14px;
  z-index: 9999;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(20px);
}
</style>


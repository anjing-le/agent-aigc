<!-- 灵感广场页面 - 提示词工具库风格 -->
<template>
  <div class="prompt-gallery">
    <div class="prompt-gallery__header">
      <div>
        <div class="prompt-gallery__eyebrow">Prompt Gallery</div>
        <h2 class="prompt-gallery__title">灵感广场</h2>
        <p class="prompt-gallery__subtitle">沉淀已发布作品和高质量 Prompt，一键复用到创作工作台</p>
      </div>
      <div class="prompt-gallery__header-actions">
        <el-tag :type="dataSource === 'api' ? 'success' : 'warning'" effect="plain">
          {{ dataSource === 'api' ? '后端作品' : '静态后备' }}
        </el-tag>
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
      </div>
    </div>

    <div class="prompt-gallery__stats">
      <div v-for="item in galleryStats" :key="item.label" class="prompt-gallery__stat">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <div class="prompt-gallery__filter">
      <div class="prompt-gallery__filter-left">
        <el-radio-group v-model="activeContentType" @change="handleFilterChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="IMAGE">图片</el-radio-button>
          <el-radio-button value="VIDEO">视频</el-radio-button>
          <el-radio-button value="AUDIO">音频</el-radio-button>
        </el-radio-group>

        <el-select
          v-model="activeTag"
          placeholder="分类"
          class="prompt-gallery__category"
          @change="handleFilterChange"
        >
          <el-option
            v-for="tag in categoryTags"
            :key="tag.value"
            :label="tag.label"
            :value="tag.value"
          />
        </el-select>

        <el-input
          v-model="searchKeyword"
          placeholder="搜索 Prompt / 标题"
          :prefix-icon="Search"
          clearable
          class="prompt-gallery__search"
          @input="handleSearch"
        />
      </div>

      <div class="prompt-gallery__filter-right">
        <el-button type="primary" :icon="MagicStick" @click="$router.push('/aigc/studio')">
          去创作
        </el-button>
      </div>
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
          @like="handleLike(item)"
          @favorite="handleFavorite(item)"
        />
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="prompt-gallery__loadmore">
        <el-button :loading="loading" @click="loadMore"> 加载更多 </el-button>
      </div>
    </div>

    <!-- 复制成功提示 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showCopyTip" class="copy-tip">已复制到剪贴板</div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
  import { MagicStick, Refresh, Search } from '@element-plus/icons-vue'
  import { useDebounceFn, useClipboard } from '@vueuse/core'
  import { ElMessage } from 'element-plus'
  import PromptCard from './components/PromptCard.vue'
  import {
    fetchFavoriteGalleryAsset,
    fetchGetGalleryList,
    fetchLikeGalleryAsset,
    fetchUnfavoriteGalleryAsset,
    fetchUnlikeGalleryAsset
  } from '@/api/aigc'
  import type { ContentType, GalleryItem } from '@/api/model/aigcModel'

  defineOptions({ name: 'AIGCGallery' })

  const router = useRouter()

  // ==================== 状态 ====================
  const loading = ref(false)
  const galleryList = ref<GalleryItem[]>([])
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(24)
  const likedAssetIds = ref<Set<string>>(new Set())
  const favoritedAssetIds = ref<Set<string>>(new Set())

  // 搜索筛选
  const searchKeyword = ref('')
  const activeTag = ref('all')
  const activeContentType = ref<ContentType | ''>('')

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

  const galleryStats = computed(() => [
    {
      label: '当前展示',
      value: `${galleryList.value.length}`
    },
    {
      label: '发布总数',
      value: `${total.value}`
    },
    {
      label: '图片 Prompt',
      value: `${galleryList.value.filter((item) => item.contentType === 'IMAGE').length}`
    },
    {
      label: '累计点赞',
      value: `${galleryList.value.reduce((sum, item) => sum + (item.likeCount || 0), 0)}`
    },
    {
      label: '累计收藏',
      value: `${galleryList.value.reduce((sum, item) => sum + (item.favoriteCount || 0), 0)}`
    }
  ])

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
        category: activeTag.value === 'all' ? undefined : activeTag.value,
        contentType: activeContentType.value || undefined
      })

      const records = (res?.records || []).map((item: any) => ({
        ...item,
        likeCount: item.likeCount ?? 0,
        likedByCurrentUser: likedAssetIds.value.has(item.id),
        favoriteCount: item.favoriteCount ?? 0,
        favoritedByCurrentUser: favoritedAssetIds.value.has(item.id)
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
        contentType: (item.contentType || 'IMAGE').toUpperCase(),
        likeCount: Math.floor(Math.random() * 100) + 10,
        likedByCurrentUser: likedAssetIds.value.has(item.id),
        favoriteCount: Math.floor(Math.random() * 20),
        favoritedByCurrentUser: favoritedAssetIds.value.has(item.id),
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
      result = result.filter(
        (item) =>
          item.prompt.toLowerCase().includes(keyword) || item.title?.toLowerCase().includes(keyword)
      )
    }

    // 分类筛选
    if (activeTag.value !== 'all') {
      result = result.filter((item) => item.category === activeTag.value)
    }

    if (activeContentType.value) {
      result = result.filter((item) => item.contentType === activeContentType.value)
    }

    return result
  }

  /** 防抖搜索 */
  const handleSearch = useDebounceFn(() => {
    currentPage.value = 1
    loadData()
  }, 300)

  const handleFilterChange = () => {
    currentPage.value = 1
    dataSource.value = 'api'
    loadData()
  }

  const handleRefresh = () => {
    currentPage.value = 1
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
    } catch {
      ElMessage.error('复制失败')
    }
  }

  const handleLike = async (item: GalleryItem) => {
    if (dataSource.value === 'static') {
      updateGalleryLikeState(item.id, {
        likeCount: Math.max(0, (item.likeCount || 0) + (item.likedByCurrentUser ? -1 : 1)),
        likedByCurrentUser: !item.likedByCurrentUser
      })
      return
    }

    try {
      const response = item.likedByCurrentUser
        ? await fetchUnlikeGalleryAsset(item.id)
        : await fetchLikeGalleryAsset(item.id)

      updateGalleryLikeState(item.id, {
        likeCount: response.likeCount ?? 0,
        likedByCurrentUser: !item.likedByCurrentUser
      })
    } catch (error) {
      console.error('更新点赞失败:', error)
      ElMessage.error('点赞失败，请稍后重试')
    }
  }

  const updateGalleryLikeState = (
    assetId: string,
    patch: Pick<GalleryItem, 'likeCount' | 'likedByCurrentUser'>
  ) => {
    galleryList.value = galleryList.value.map((item) =>
      item.id === assetId ? { ...item, ...patch } : item
    )
    if (patch.likedByCurrentUser) {
      likedAssetIds.value.add(assetId)
    } else {
      likedAssetIds.value.delete(assetId)
    }
    likedAssetIds.value = new Set(likedAssetIds.value)
  }

  const handleFavorite = async (item: GalleryItem) => {
    if (dataSource.value === 'static') {
      updateGalleryFavoriteState(item.id, {
        favoriteCount: Math.max(
          0,
          (item.favoriteCount || 0) + (item.favoritedByCurrentUser ? -1 : 1)
        ),
        favoritedByCurrentUser: !item.favoritedByCurrentUser
      })
      return
    }

    try {
      const response = item.favoritedByCurrentUser
        ? await fetchUnfavoriteGalleryAsset(item.id)
        : await fetchFavoriteGalleryAsset(item.id)

      updateGalleryFavoriteState(item.id, {
        favoriteCount: response.favoriteCount ?? 0,
        favoritedByCurrentUser: !item.favoritedByCurrentUser
      })
    } catch (error) {
      console.error('更新收藏失败:', error)
      ElMessage.error('收藏失败，请稍后重试')
    }
  }

  const updateGalleryFavoriteState = (
    assetId: string,
    patch: Pick<GalleryItem, 'favoriteCount' | 'favoritedByCurrentUser'>
  ) => {
    galleryList.value = galleryList.value.map((item) =>
      item.id === assetId ? { ...item, ...patch } : item
    )
    if (patch.favoritedByCurrentUser) {
      favoritedAssetIds.value.add(assetId)
    } else {
      favoritedAssetIds.value.delete(assetId)
    }
    favoritedAssetIds.value = new Set(favoritedAssetIds.value)
  }

  /** 使用提示词（跳转到创作工作台） */
  const handleUse = (item: GalleryItem) => {
    router.push({
      path: '/aigc/studio',
      query: {
        prompt: item.prompt,
        contentType: item.contentType
      }
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

    &__category {
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

    &__loadmore {
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
    .prompt-gallery {
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
    .prompt-gallery {
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

  // 复制成功提示
  .copy-tip {
    position: fixed;
    bottom: 40px;
    left: 50%;
    transform: translateX(-50%);
    padding: 12px 24px;
    background: var(--el-text-color-primary);
    color: var(--el-bg-color);
    border-radius: 8px;
    font-size: 14px;
    z-index: 9999;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }

  .fade-enter-active,
  .fade-leave-active {
    transition:
      opacity 0.3s,
      transform 0.3s;
  }

  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
    transform: translateX(-50%) translateY(20px);
  }
</style>

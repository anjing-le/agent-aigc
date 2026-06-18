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
        <el-button :icon="DataAnalysis" @click="$router.push('/aigc/gallery-report')">
          互动报表
        </el-button>
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
      </div>
    </div>

    <div class="prompt-gallery__stats">
      <div v-for="item in galleryStats" :key="item.label" class="prompt-gallery__stat">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <section
      v-if="galleryCollections.length"
      v-loading="collectionsLoading"
      class="prompt-gallery__collections"
    >
      <div class="prompt-gallery__collections-header">
        <div>
          <h3>精选合集</h3>
          <p>{{ collectionsDescription }}</p>
        </div>
        <el-tag effect="plain">{{ galleryCollections.length }} 个合集</el-tag>
      </div>

      <div class="prompt-gallery__collections-grid">
        <article
          v-for="collection in galleryCollections"
          :key="collection.id"
          class="prompt-gallery__collection"
        >
          <button
            type="button"
            class="prompt-gallery__collection-cover"
            @click="collection.coverAsset && openPublicGalleryPage(collection.coverAsset)"
          >
            <img
              v-if="collection.coverAsset"
              :src="resolveAigcGalleryPreviewUrl(collection.coverAsset)"
              alt=""
            />
            <span v-else>{{ formatContentType(collection.contentType) }}</span>
          </button>

          <div class="prompt-gallery__collection-main">
            <div class="prompt-gallery__collection-heading">
              <el-tag size="small" effect="plain">
                {{ formatCollectionStrategy(collection.strategy) }}
              </el-tag>
              <h3>{{ collection.title }}</h3>
            </div>
            <p>{{ collection.description }}</p>
            <div class="prompt-gallery__collection-meta">
              <span>{{ collection.itemCount || 0 }} 个作品</span>
              <span>{{ collection.heatScore || 0 }} 热度</span>
              <span>{{ collection.totalLikeCount || 0 }} 赞</span>
              <span>{{ collection.totalFavoriteCount || 0 }} 收藏</span>
            </div>
            <div class="prompt-gallery__collection-assets">
              <button
                v-for="asset in (collection.assets || []).slice(0, 3)"
                :key="asset.id"
                type="button"
                @click="handleUse(asset)"
              >
                {{ truncateText(asset.prompt, 28) }}
              </button>
            </div>
          </div>

          <div class="prompt-gallery__collection-actions">
            <el-button
              v-if="collection.coverAsset"
              size="small"
              :icon="MagicStick"
              @click="handleUse(collection.coverAsset)"
            >
              复用封面
            </el-button>
          </div>
        </article>
      </div>
    </section>

    <section v-if="galleryTopics.length" v-loading="topicsLoading" class="prompt-gallery__topics">
      <div class="prompt-gallery__topics-header">
        <div>
          <h3>人工运营专题</h3>
          <p>{{ topicsDescription }}</p>
        </div>
        <el-tag effect="plain">{{ galleryTopics.length }} 个专题</el-tag>
      </div>

      <div class="prompt-gallery__topics-grid">
        <article v-for="topic in galleryTopics" :key="topic.id" class="prompt-gallery__topic">
          <div class="prompt-gallery__topic-head">
            <div>
              <div class="prompt-gallery__topic-tags">
                <el-tag size="small" effect="plain">{{ topic.scenario || '运营专题' }}</el-tag>
                <el-tag size="small" type="success" effect="plain">
                  {{ formatContentType(topic.contentType) }}
                </el-tag>
              </div>
              <h3>{{ topic.title }}</h3>
            </div>
            <strong>{{ topic.heatScore || 0 }}</strong>
          </div>

          <p>{{ topic.description }}</p>
          <div class="prompt-gallery__topic-rule">{{ topic.curationRule }}</div>
          <div class="prompt-gallery__topic-hint">{{ topic.operationHint }}</div>

          <div class="prompt-gallery__topic-assets">
            <button
              v-for="asset in (topic.assets || []).slice(0, 3)"
              :key="asset.id"
              type="button"
              @click="handleUse(asset)"
            >
              {{ truncateText(asset.prompt, 34) }}
            </button>
          </div>

          <div class="prompt-gallery__topic-actions">
            <span>{{ topic.itemCount || 0 }} 个作品 · {{ topic.totalFavoriteCount || 0 }} 收藏</span>
            <div>
              <el-button
                v-if="topic.coverAsset"
                size="small"
                :icon="MagicStick"
                @click="handleUse(topic.coverAsset)"
              >
                复用主作品
              </el-button>
              <el-button
                v-if="topic.coverAsset"
                size="small"
                :icon="Link"
                @click="openPublicGalleryPage(topic.coverAsset)"
              >
                分享页
              </el-button>
            </div>
          </div>
        </article>
      </div>
    </section>

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
        <el-checkbox v-model="showOnlyFavorites" @change="handleFavoriteFilterChange">
          我的收藏
        </el-checkbox>
        <el-button type="primary" :icon="MagicStick" @click="$router.push('/aigc/studio')">
          去创作
        </el-button>
      </div>
    </div>

    <section v-if="rankedGalleryItems.length" v-loading="rankingLoading" class="prompt-gallery__ranking">
      <div class="prompt-gallery__ranking-header">
        <div>
          <h3>热门榜单</h3>
          <p>{{ rankingDescription }}</p>
        </div>
        <el-tag effect="plain">{{ rankingScopeLabel }}</el-tag>
      </div>

      <div class="prompt-gallery__ranking-list">
        <article
          v-for="entry in rankedGalleryItems"
          :key="entry.item.id"
          class="prompt-gallery__ranking-item"
        >
          <span class="prompt-gallery__ranking-index">#{{ entry.rank }}</span>
          <div class="prompt-gallery__ranking-copy">
            <strong>{{ entry.item.title || truncateText(entry.item.prompt, 42) }}</strong>
            <span>{{ truncateText(entry.item.prompt, 72) }}</span>
          </div>
          <div class="prompt-gallery__ranking-meta">
            <el-tag size="small" effect="plain">
              {{ formatContentType(entry.item.contentType) }}
            </el-tag>
            <span>{{ entry.score }} 热度</span>
            <span>{{ entry.item.likeCount || 0 }} 赞</span>
            <span>{{ entry.item.favoriteCount || 0 }} 收藏</span>
          </div>
          <div class="prompt-gallery__ranking-actions">
            <el-button size="small" :icon="MagicStick" @click="handleUse(entry.item)">
              复用
            </el-button>
            <el-button size="small" :icon="Link" @click="openPublicGalleryPage(entry.item)">
              分享页
            </el-button>
          </div>
        </article>
      </div>
    </section>

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
          @download="handleDownload(item)"
          @share="handleShare(item)"
        />
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="prompt-gallery__loadmore">
        <el-button :loading="loading" @click="loadMore"> 加载更多 </el-button>
      </div>
    </div>

    <section class="prompt-gallery__audit">
      <div class="prompt-gallery__audit-header">
        <div>
          <h3>最近广场审计</h3>
          <p>发布、撤回、点赞、收藏和公开下载的操作记录</p>
        </div>
        <div class="prompt-gallery__audit-actions">
          <el-tag effect="plain">{{ auditTotal }} 条</el-tag>
          <el-button :icon="Refresh" :loading="auditLoading" @click="loadGalleryAuditLogs">
            刷新
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="auditLoading"
        :data="galleryAuditLogs"
        size="small"
        class="prompt-gallery__audit-table"
        empty-text="暂无广场审计"
      >
        <el-table-column label="动作" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getGalleryAuditActionTag(row.action)" effect="plain">
              {{ formatGalleryAuditAction(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assetId" label="资产" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            {{ formatContentType(row.contentType) }}
          </template>
        </el-table-column>
        <el-table-column prop="model" label="模型" min-width="140" show-overflow-tooltip />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" effect="plain">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作者" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.operatorName || row.operatorId || row.clientIp || '匿名会话' }}
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">
            {{ formatAuditTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- 复制成功提示 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showCopyTip" class="copy-tip">已复制到剪贴板</div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
  import { DataAnalysis, Link, MagicStick, Refresh, Search } from '@element-plus/icons-vue'
  import { useDebounceFn, useClipboard } from '@vueuse/core'
  import { ElMessage } from 'element-plus'
  import PromptCard from './components/PromptCard.vue'
  import {
    fetchFavoriteGalleryAsset,
    fetchGetGalleryAuditLogs,
    fetchGetGalleryCollections,
    fetchGetFavoriteGalleryList,
    fetchGetGalleryList,
    fetchGetGalleryRanking,
    fetchGetGalleryTopics,
    fetchLikeGalleryAsset,
    fetchUnfavoriteGalleryAsset,
    fetchUnlikeGalleryAsset
  } from '@/api/aigc'
  import type {
    ContentType,
    GalleryAuditLogItem,
    GalleryCollection,
    GalleryItem,
    GalleryTopic
  } from '@/api/model/aigcModel'
  import {
    downloadAigcGalleryAsset,
    resolveAigcGalleryPreviewUrl
  } from '@/utils/aigcAsset'

  defineOptions({ name: 'AIGCGallery' })

  const router = useRouter()

  // ==================== 状态 ====================
  const loading = ref(false)
  const galleryList = ref<GalleryItem[]>([])
  const rankingList = ref<GalleryItem[]>([])
  const rankingLoading = ref(false)
  const rankingTotal = ref(0)
  const galleryCollections = ref<GalleryCollection[]>([])
  const collectionsLoading = ref(false)
  const galleryTopics = ref<GalleryTopic[]>([])
  const topicsLoading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(24)
  const likedAssetIds = ref<Set<string>>(new Set())
  const favoritedAssetIds = ref<Set<string>>(new Set())
  const galleryAuditLogs = ref<GalleryAuditLogItem[]>([])
  const auditLoading = ref(false)
  const auditTotal = ref(0)

  // 搜索筛选
  const searchKeyword = ref('')
  const activeTag = ref('all')
  const activeContentType = ref<ContentType | ''>('')
  const showOnlyFavorites = ref(false)

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

  const getRankingScore = (item: GalleryItem) =>
    Number(item.likeCount || 0) + Number(item.favoriteCount || 0) * 2

  const truncateText = (value?: string, maxLength = 48) => {
    const text = value?.trim() || '-'
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
  }

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

  const rankingSourceItems = computed(() =>
    rankingList.value.length > 0 ? rankingList.value : galleryList.value
  )

  const rankedGalleryItems = computed(() =>
    rankingSourceItems.value
      .map((item, index) => ({
        item,
        score: getRankingScore(item),
        originalIndex: index
      }))
      .sort((a, b) => b.score - a.score || a.originalIndex - b.originalIndex)
      .slice(0, 5)
      .map((entry, index) => ({
        ...entry,
        rank: index + 1
      }))
  )

  const rankingScopeLabel = computed(() =>
    rankingList.value.length > 0
      ? `全局 Top ${rankedGalleryItems.value.length}`
      : `当前筛选 Top ${rankedGalleryItems.value.length}`
  )

  const rankingDescription = computed(() =>
    rankingList.value.length > 0
      ? `后端按全部已发布作品计算热度，当前匹配 ${rankingTotal.value} 个候选作品`
      : '按当前筛选结果的点赞和收藏热度排序，方便快速复用高反馈 Prompt'
  )

  const collectionsDescription = computed(() =>
    activeContentType.value
      ? `${formatContentType(activeContentType.value)}作品的热门和最新聚合`
      : '按热度、发布时间和内容类型自动组织公开作品'
  )

  const topicsDescription = computed(() =>
    activeContentType.value
      ? `${formatContentType(activeContentType.value)}作品的运营专题建议`
      : '按运营场景组织可分享、可下载、可复用的公开作品'
  )

  const galleryAuditActionLabels: Record<string, string> = {
    publish: '发布',
    unpublish: '撤回',
    like: '点赞',
    unlike: '取消点赞',
    favorite: '收藏',
    unfavorite: '取消收藏',
    'public-download': '公开下载',
    'share-view': '分享访问',
    'prompt-reuse': 'Prompt 复用'
  }

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
      if (!append) {
        await loadRankingData()
        await loadCollectionsData()
        await loadTopicsData()
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
      const res = showOnlyFavorites.value
        ? await fetchGetFavoriteGalleryList({
            current: currentPage.value,
            size: pageSize.value
          })
        : await fetchGetGalleryList({
            current: currentPage.value,
            size: pageSize.value,
            keyword: searchKeyword.value || undefined,
            category: activeTag.value === 'all' ? undefined : activeTag.value,
            contentType: activeContentType.value || undefined
          })

      const records = (res?.records || []).map(normalizeGalleryItem)

      // 后端无已发布作品时，自动切换到静态数据后备
      if (
        !showOnlyFavorites.value &&
        !append &&
        records.length === 0 &&
        currentPage.value === 1 &&
        !searchKeyword.value
      ) {
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

  const loadRankingData = async () => {
    if (dataSource.value !== 'api' || showOnlyFavorites.value) {
      rankingList.value = []
      rankingTotal.value = 0
      return
    }

    try {
      rankingLoading.value = true
      const response = await fetchGetGalleryRanking({
        current: 1,
        size: 5,
        keyword: searchKeyword.value || undefined,
        contentType: activeContentType.value || undefined
      })
      rankingList.value = (response.records || []).map(normalizeGalleryItem)
      rankingTotal.value = response.total || 0
    } catch (error) {
      console.warn('加载广场热门榜单失败，使用当前列表回退:', error)
      rankingList.value = []
      rankingTotal.value = 0
    } finally {
      rankingLoading.value = false
    }
  }

  const loadCollectionsData = async () => {
    if (dataSource.value !== 'api' || showOnlyFavorites.value) {
      galleryCollections.value = []
      return
    }

    try {
      collectionsLoading.value = true
      const response = await fetchGetGalleryCollections({
        size: 4,
        keyword: searchKeyword.value || undefined,
        contentType: activeContentType.value || undefined
      })
      galleryCollections.value = (response.collections || []).map(normalizeGalleryCollection)
    } catch (error) {
      console.warn('加载灵感广场作品合集失败:', error)
      galleryCollections.value = []
    } finally {
      collectionsLoading.value = false
    }
  }

  const loadTopicsData = async () => {
    if (dataSource.value !== 'api' || showOnlyFavorites.value) {
      galleryTopics.value = []
      return
    }

    try {
      topicsLoading.value = true
      const response = await fetchGetGalleryTopics({
        size: 4,
        keyword: searchKeyword.value || undefined,
        contentType: activeContentType.value || undefined
      })
      galleryTopics.value = (response.topics || []).map(normalizeGalleryTopic)
    } catch (error) {
      console.warn('加载灵感广场运营专题失败:', error)
      galleryTopics.value = []
    } finally {
      topicsLoading.value = false
    }
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

  const normalizeGalleryItem = (item: GalleryItem): GalleryItem => ({
    ...item,
    likeCount: item.likeCount ?? 0,
    likedByCurrentUser: item.likedByCurrentUser ?? likedAssetIds.value.has(item.id),
    favoriteCount: item.favoriteCount ?? 0,
    favoritedByCurrentUser: item.favoritedByCurrentUser ?? favoritedAssetIds.value.has(item.id)
  })

  const normalizeGalleryCollection = (collection: GalleryCollection): GalleryCollection => {
    const assets = (collection.assets || []).map(normalizeGalleryItem)
    return {
      ...collection,
      assets,
      coverAsset: collection.coverAsset
        ? normalizeGalleryItem(collection.coverAsset)
        : assets[0]
    }
  }

  const normalizeGalleryTopic = (topic: GalleryTopic): GalleryTopic => {
    const assets = (topic.assets || []).map(normalizeGalleryItem)
    return {
      ...topic,
      assets,
      coverAsset: topic.coverAsset ? normalizeGalleryItem(topic.coverAsset) : assets[0]
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

  const handleFavoriteFilterChange = () => {
    currentPage.value = 1
    dataSource.value = 'api'
    loadData()
  }

  const handleRefresh = () => {
    currentPage.value = 1
    dataSource.value = 'api'
    loadData()
    loadGalleryAuditLogs()
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

  const handleDownload = async (item: GalleryItem) => {
    try {
      if (dataSource.value === 'static') {
        window.open(resolveAigcGalleryPreviewUrl(item), '_blank', 'noopener,noreferrer')
        return
      }
      await downloadAigcGalleryAsset(item)
      await loadGalleryAuditLogs()
    } catch (error) {
      console.error('下载广场作品失败:', error)
      ElMessage.error('下载失败，请稍后重试')
    }
  }

  const handleShare = async (item: GalleryItem) => {
    try {
      const url = resolvePublicGalleryUrl(item)
      await copy(url)
      showCopyTip.value = true
      setTimeout(() => {
        showCopyTip.value = false
      }, 2000)
      ElMessage.success('公开链接已复制')
    } catch (error) {
      console.error('复制公开链接失败:', error)
      ElMessage.error('复制失败')
    }
  }

  const resolvePublicGalleryUrl = (item: GalleryItem) => {
    if (dataSource.value !== 'static' && item.id) {
      return `${window.location.origin}/#/share/gallery/${encodeURIComponent(item.id)}`
    }
    const previewUrl = resolveAigcGalleryPreviewUrl(item)
    if (/^(https?:|data:|blob:)/i.test(previewUrl)) {
      return previewUrl
    }
    return `${window.location.origin}${previewUrl}`
  }

  const openPublicGalleryPage = (item: GalleryItem) => {
    window.open(resolvePublicGalleryUrl(item), '_blank', 'noopener,noreferrer')
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
      await loadGalleryAuditLogs()
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
    rankingList.value = rankingList.value.map((item) =>
      item.id === assetId ? { ...item, ...patch } : item
    )
    galleryCollections.value = patchGalleryCollections(assetId, patch)
    galleryTopics.value = patchGalleryTopics(assetId, patch)
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
      await loadGalleryAuditLogs()
      if (showOnlyFavorites.value) {
        await loadData()
      }
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
    rankingList.value = rankingList.value.map((item) =>
      item.id === assetId ? { ...item, ...patch } : item
    )
    galleryCollections.value = patchGalleryCollections(assetId, patch)
    galleryTopics.value = patchGalleryTopics(assetId, patch)
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

  const patchGalleryCollections = (assetId: string, patch: Partial<GalleryItem>) =>
    galleryCollections.value.map((collection) => ({
      ...collection,
      coverAsset:
        collection.coverAsset?.id === assetId
          ? { ...collection.coverAsset, ...patch }
          : collection.coverAsset,
      assets: (collection.assets || []).map((asset) =>
        asset.id === assetId ? { ...asset, ...patch } : asset
      )
    }))

  const patchGalleryTopics = (assetId: string, patch: Partial<GalleryItem>) =>
    galleryTopics.value.map((topic) => ({
      ...topic,
      coverAsset:
        topic.coverAsset?.id === assetId
          ? { ...topic.coverAsset, ...patch }
          : topic.coverAsset,
      assets: (topic.assets || []).map((asset) =>
        asset.id === assetId ? { ...asset, ...patch } : asset
      )
    }))

  const loadGalleryAuditLogs = async () => {
    try {
      auditLoading.value = true
      const response = await fetchGetGalleryAuditLogs({
        current: 1,
        size: 8,
        success: true
      })
      galleryAuditLogs.value = response.records || []
      auditTotal.value = response.total || 0
    } catch (error) {
      console.warn('加载广场审计失败:', error)
      galleryAuditLogs.value = []
      auditTotal.value = 0
    } finally {
      auditLoading.value = false
    }
  }

  const formatGalleryAuditAction = (action?: string) => {
    if (!action) return '-'
    return galleryAuditActionLabels[action] || action
  }

  const getGalleryAuditActionTag = (action?: string) => {
    if (action === 'publish') return 'success'
    if (action === 'unpublish') return 'warning'
    if (action === 'like' || action === 'favorite') return 'primary'
    if (action === 'public-download' || action === 'share-view' || action === 'prompt-reuse') {
      return 'info'
    }
    return 'info'
  }

  const formatContentType = (contentType?: string) => {
    const labels: Record<string, string> = {
      IMAGE: '图片',
      VIDEO: '视频',
      AUDIO: '音频'
    }
    return contentType ? labels[contentType] || contentType : '-'
  }

  const formatCollectionStrategy = (strategy?: string) => {
    const labels: Record<string, string> = {
      trending: '热门',
      latest: '最新',
      'content-type': '类型'
    }
    return strategy ? labels[strategy] || strategy : '合集'
  }

  const formatAuditTime = (value?: string) => {
    if (!value) return '-'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return date.toLocaleString('zh-CN', { hour12: false })
  }

  // ==================== 生命周期 ====================
  onMounted(() => {
    loadData()
    loadGalleryAuditLogs()
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

    &__collections {
      margin-bottom: 16px;
      padding: 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__collections-header {
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
        margin: 6px 0 0;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__collections-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }

    &__collection {
      display: grid;
      grid-template-columns: 112px minmax(0, 1fr) auto;
      gap: 12px;
      min-height: 132px;
      padding: 12px;
      background: var(--el-fill-color-lighter);
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
    }

    &__collection-cover {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 112px;
      height: 108px;
      padding: 0;
      overflow: hidden;
      cursor: pointer;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 6px;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      span {
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }
    }

    &__collection-main {
      min-width: 0;

      > p {
        display: -webkit-box;
        margin: 8px 0 0;
        overflow: hidden;
        color: var(--el-text-color-secondary);
        font-size: 12px;
        line-height: 1.5;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
      }
    }

    &__collection-heading {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;

      h3 {
        margin: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 15px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }
    }

    &__collection-meta {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 10px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    &__collection-assets {
      display: flex;
      gap: 8px;
      margin-top: 10px;
      overflow: hidden;

      button {
        max-width: 140px;
        height: 28px;
        padding: 0 10px;
        overflow: hidden;
        cursor: pointer;
        color: var(--el-text-color-regular);
        text-overflow: ellipsis;
        white-space: nowrap;
        background: var(--el-bg-color);
        border: 1px solid var(--el-border-color-light);
        border-radius: 6px;
      }
    }

    &__collection-actions {
      display: flex;
      align-items: flex-start;
      justify-content: flex-end;
      min-width: 96px;
    }

    &__topics {
      margin-bottom: 16px;
      padding: 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__topics-header {
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
        margin: 6px 0 0;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__topics-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }

    &__topic {
      min-width: 0;
      padding: 14px;
      background: var(--el-fill-color-lighter);
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;

      > p {
        display: -webkit-box;
        margin: 10px 0 0;
        overflow: hidden;
        color: var(--el-text-color-secondary);
        font-size: 12px;
        line-height: 1.5;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
      }
    }

    &__topic-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;

      h3 {
        margin: 8px 0 0;
        overflow: hidden;
        color: var(--el-text-color-primary);
        font-size: 15px;
        font-weight: 600;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      strong {
        flex-shrink: 0;
        font-size: 22px;
        line-height: 1;
        color: var(--el-color-primary);
      }
    }

    &__topic-tags {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

    &__topic-rule,
    &__topic-hint {
      margin-top: 10px;
      padding: 8px 10px;
      overflow: hidden;
      font-size: 12px;
      line-height: 1.5;
      text-overflow: ellipsis;
      white-space: nowrap;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 6px;
    }

    &__topic-rule {
      color: var(--el-text-color-secondary);
    }

    &__topic-hint {
      color: var(--el-text-color-regular);
    }

    &__topic-assets {
      display: flex;
      gap: 8px;
      margin-top: 10px;
      overflow: hidden;

      button {
        max-width: 168px;
        height: 28px;
        padding: 0 10px;
        overflow: hidden;
        cursor: pointer;
        color: var(--el-text-color-regular);
        text-overflow: ellipsis;
        white-space: nowrap;
        background: var(--el-bg-color);
        border: 1px solid var(--el-border-color-light);
        border-radius: 6px;
      }
    }

    &__topic-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-top: 12px;

      span {
        min-width: 0;
        overflow: hidden;
        color: var(--el-text-color-secondary);
        font-size: 12px;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      div {
        display: flex;
        gap: 8px;
        flex-shrink: 0;
        flex-wrap: wrap;
        justify-content: flex-end;
      }
    }

    &__ranking {
      margin-bottom: 16px;
      padding: 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__ranking-header {
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
        margin: 6px 0 0;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__ranking-list {
      display: grid;
      gap: 10px;
    }

    &__ranking-item {
      display: grid;
      grid-template-columns: 48px minmax(0, 1fr) minmax(220px, auto) auto;
      align-items: center;
      gap: 12px;
      min-height: 72px;
      padding: 12px;
      background: var(--el-fill-color-lighter);
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
    }

    &__ranking-index {
      font-size: 18px;
      font-weight: 700;
      line-height: 1;
      color: var(--el-color-primary);
    }

    &__ranking-copy {
      min-width: 0;

      strong,
      span {
        display: block;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      strong {
        font-size: 14px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      span {
        margin-top: 6px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__ranking-meta {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      min-width: 0;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    &__ranking-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      flex-wrap: wrap;
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
      display: flex;
      align-items: center;
      gap: 12px;
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

    &__audit {
      margin-top: 24px;
      padding: 16px;
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
    }

    &__audit-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 14px;

      h3 {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      p {
        margin: 6px 0 0;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__audit-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;
    }

    &__audit-table {
      width: 100%;
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

      &__collections-grid {
        grid-template-columns: 1fr;
      }

      &__topics-grid {
        grid-template-columns: 1fr;
      }

      &__ranking-item {
        grid-template-columns: 44px minmax(0, 1fr);
      }

      &__ranking-meta,
      &__ranking-actions {
        grid-column: 2;
      }

      &__ranking-actions {
        justify-content: flex-start;
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

      &__collections-header {
        flex-direction: column;
      }

      &__topics-header {
        flex-direction: column;
      }

      &__collection {
        grid-template-columns: 1fr;
      }

      &__collection-cover {
        width: 100%;
        height: auto;
        aspect-ratio: 16 / 9;
      }

      &__collection-assets {
        flex-wrap: wrap;

        button {
          max-width: 100%;
        }
      }

      &__collection-actions {
        justify-content: flex-start;
      }

      &__topic-assets {
        flex-wrap: wrap;

        button {
          max-width: 100%;
        }
      }

      &__topic-actions {
        align-items: flex-start;
        flex-direction: column;

        div {
          width: 100%;
          justify-content: flex-start;
        }
      }

      &__ranking-item {
        grid-template-columns: 1fr;
      }

      &__ranking-header {
        flex-direction: column;
      }

      &__ranking-meta,
      &__ranking-actions {
        grid-column: auto;
      }

      &__ranking-actions .el-button {
        flex: 1 1 110px;
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

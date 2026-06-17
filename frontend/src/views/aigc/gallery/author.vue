<template>
  <main class="gallery-author">
    <section class="gallery-author__hero">
      <div>
        <div class="gallery-author__eyebrow">Creator Gallery</div>
        <h1>{{ profile?.authorName || routeAuthorId }}</h1>
        <p>
          {{ profile?.publishedCount || 0 }} 个公开作品 · {{ dominantTypeLabel }}创作为主 ·
          {{ profile?.totalInteractionCount || 0 }} 次互动
        </p>
      </div>

      <div class="gallery-author__actions">
        <el-button :icon="Link" @click="handleCopyLink">复制主页</el-button>
        <el-button type="primary" :icon="MagicStick" @click="router.push('/aigc/studio')">
          去创作
        </el-button>
      </div>
    </section>

    <section class="gallery-author__stats">
      <div v-for="item in stats" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section v-if="topAssets.length > 0" class="gallery-author__spotlight">
      <div class="gallery-author__spotlight-title">
        <span>Top Works</span>
        <strong>高互动作品</strong>
      </div>

      <div class="gallery-author__top-list">
        <button
          v-for="(item, index) in topAssets"
          :key="item.id"
          type="button"
          class="gallery-author__top-item"
          @click="openShare(item)"
        >
          <span class="gallery-author__top-rank">#{{ index + 1 }}</span>
          <span class="gallery-author__top-copy">{{ truncate(item.prompt, 46) }}</span>
          <span class="gallery-author__top-score">
            {{ interactionScore(item) }} 互动
          </span>
        </button>
      </div>
    </section>

    <section class="gallery-author__toolbar">
      <el-radio-group v-model="activeContentType" @change="handleFilterChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="IMAGE">图片</el-radio-button>
        <el-radio-button value="VIDEO">视频</el-radio-button>
        <el-radio-button value="AUDIO">音频</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" :loading="loading" @click="() => loadProfile()">刷新</el-button>
    </section>

    <section v-loading="loading" class="gallery-author__content">
      <el-empty v-if="!loading && assets.length === 0" description="暂无公开作品" />

      <div v-else class="gallery-author__grid">
        <article v-for="item in assets" :key="item.id" class="gallery-author__item">
          <button type="button" class="gallery-author__preview" @click="openShare(item)">
            <img
              v-if="item.contentType === 'IMAGE'"
              :src="resolveAigcGalleryPreviewUrl(item)"
              :alt="item.prompt"
              loading="lazy"
            />
            <div v-else class="gallery-author__placeholder">
              <el-icon :size="30"><Picture /></el-icon>
              <span>{{ formatContentType(item.contentType) }}</span>
            </div>
          </button>

          <div class="gallery-author__item-body">
            <div class="gallery-author__item-meta">
              <el-tag size="small" effect="plain">{{ formatContentType(item.contentType) }}</el-tag>
              <span>{{ item.likeCount || 0 }} 赞</span>
              <span>{{ item.favoriteCount || 0 }} 收藏</span>
            </div>
            <h2>{{ truncate(item.prompt, 42) }}</h2>
            <p>{{ item.model }}</p>
            <div class="gallery-author__item-actions">
              <el-button text :icon="View" @click="openShare(item)">查看</el-button>
              <el-button text :icon="DocumentCopy" @click="copyPrompt(item)">复制 Prompt</el-button>
              <el-button text :icon="Download" @click="downloadItem(item)">下载</el-button>
            </div>
          </div>
        </article>
      </div>

      <div v-if="hasMore" class="gallery-author__loadmore">
        <el-button :loading="loading" @click="loadMore">加载更多</el-button>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
  import {
    DocumentCopy,
    Download,
    Link,
    MagicStick,
    Picture,
    Refresh,
    View
  } from '@element-plus/icons-vue'
  import { useClipboard } from '@vueuse/core'
  import { ElMessage } from 'element-plus'
  import { fetchGetGalleryAuthorProfile } from '@/api/aigc'
  import type {
    ContentType,
    GalleryAuthorProfileResponse,
    GalleryItem
  } from '@/api/model/aigcModel'
  import {
    downloadAigcGalleryAsset,
    resolveAigcGalleryPreviewUrl
  } from '@/utils/aigcAsset'

  defineOptions({ name: 'AIGCGalleryAuthor' })

  const route = useRoute()
  const router = useRouter()
  const { copy } = useClipboard()

  const profile = ref<GalleryAuthorProfileResponse | null>(null)
  const assets = ref<GalleryItem[]>([])
  const loading = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(12)
  const total = ref(0)
  const activeContentType = ref<ContentType | ''>('')
  const routeAuthorId = computed(() => String(route.params.authorId || 'anonymous'))
  const hasMore = computed(() => assets.value.length < total.value)
  const topAssets = computed(() => profile.value?.topAssets || [])
  const dominantTypeLabel = computed(() => {
    return profile.value?.dominantContentType
      ? formatContentType(profile.value.dominantContentType)
      : '多类型'
  })

  const stats = computed(() => [
    { label: '公开作品', value: profile.value?.publishedCount || 0 },
    { label: '总互动', value: profile.value?.totalInteractionCount || 0 },
    { label: '点赞', value: profile.value?.totalLikeCount || 0 },
    { label: '收藏', value: profile.value?.totalFavoriteCount || 0 },
    { label: '主类型', value: dominantTypeLabel.value },
    {
      label: '图 / 视 / 音',
      value: `${profile.value?.imageCount || 0} / ${profile.value?.videoCount || 0} / ${
        profile.value?.audioCount || 0
      }`
    }
  ])

  const loadProfile = async (append = false) => {
    loading.value = true
    try {
      const response = await fetchGetGalleryAuthorProfile(routeAuthorId.value, {
        current: currentPage.value,
        size: pageSize.value,
        contentType: activeContentType.value || undefined
      })
      profile.value = response
      const page = response.assets
      total.value = page?.total || 0
      assets.value = append ? [...assets.value, ...(page?.records || [])] : page?.records || []
    } catch (error) {
      console.error('加载公开作者主页失败:', error)
      ElMessage.error('作者主页加载失败')
    } finally {
      loading.value = false
    }
  }

  const handleFilterChange = () => {
    currentPage.value = 1
    assets.value = []
    loadProfile()
  }

  const loadMore = () => {
    if (loading.value || !hasMore.value) return
    currentPage.value += 1
    loadProfile(true)
  }

  const openShare = (item: GalleryItem) => {
    router.push(`/share/gallery/${encodeURIComponent(item.id)}`)
  }

  const handleCopyLink = async () => {
    await copy(window.location.href)
    ElMessage.success('主页链接已复制')
  }

  const copyPrompt = async (item: GalleryItem) => {
    await copy(item.prompt)
    ElMessage.success('Prompt 已复制')
  }

  const downloadItem = async (item: GalleryItem) => {
    try {
      await downloadAigcGalleryAsset(item)
    } catch (error) {
      console.error('下载公开作品失败:', error)
      ElMessage.error('下载失败，请稍后重试')
    }
  }

  const interactionScore = (item: GalleryItem) => {
    return (item.likeCount || 0) + (item.favoriteCount || 0)
  }

  const formatContentType = (value: ContentType) => {
    const labels: Record<ContentType, string> = {
      IMAGE: '图片',
      VIDEO: '视频',
      AUDIO: '音频'
    }
    return labels[value] || value
  }

  const truncate = (value: string, length: number) => {
    return value.length > length ? `${value.slice(0, length)}...` : value
  }

  watch(
    () => route.params.authorId,
    () => {
      currentPage.value = 1
      assets.value = []
      loadProfile()
    }
  )

  onMounted(() => loadProfile())
</script>

<style scoped lang="scss">
  .gallery-author {
    min-height: 100vh;
    padding: 34px 28px 48px;
    color: #172033;
    background: #f6f8fb;
  }

  .gallery-author__hero,
  .gallery-author__stats,
  .gallery-author__spotlight,
  .gallery-author__toolbar,
  .gallery-author__content {
    max-width: 1220px;
    margin: 0 auto;
  }

  .gallery-author__hero {
    display: flex;
    gap: 24px;
    align-items: flex-end;
    justify-content: space-between;

    h1 {
      margin: 8px 0;
      font-size: 40px;
      line-height: 1.15;
    }

    p {
      margin: 0;
      color: #60708a;
    }
  }

  .gallery-author__eyebrow {
    color: #2f6fed;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0;
    text-transform: uppercase;
  }

  .gallery-author__actions,
  .gallery-author__toolbar,
  .gallery-author__item-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
  }

  .gallery-author__stats {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
    margin-top: 24px;

    div {
      display: grid;
      gap: 6px;
      padding: 16px;
      background: #fff;
      border: 1px solid #dfe6f0;
      border-radius: 8px;
    }

    span {
      color: #6a7890;
      font-size: 13px;
    }

    strong {
      font-size: 24px;
    }
  }

  .gallery-author__spotlight {
    display: grid;
    grid-template-columns: 180px minmax(0, 1fr);
    gap: 18px;
    align-items: stretch;
    margin-top: 18px;
    padding: 18px;
    background: #fff;
    border: 1px solid #dfe6f0;
    border-radius: 8px;
  }

  .gallery-author__spotlight-title {
    display: grid;
    align-content: center;
    gap: 8px;

    span {
      color: #2f6fed;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0;
      text-transform: uppercase;
    }

    strong {
      font-size: 22px;
      line-height: 1.25;
    }
  }

  .gallery-author__top-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
    gap: 10px;
  }

  .gallery-author__top-item {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
    gap: 6px 10px;
    align-items: center;
    padding: 12px;
    text-align: left;
    cursor: pointer;
    background: #f7f9fd;
    border: 1px solid #dfe6f0;
    border-radius: 8px;
    transition:
      border-color 0.2s ease,
      transform 0.2s ease;

    &:hover {
      border-color: #2f6fed;
      transform: translateY(-1px);
    }
  }

  .gallery-author__top-rank {
    color: #2f6fed;
    font-size: 13px;
    font-weight: 800;
  }

  .gallery-author__top-copy {
    overflow: hidden;
    color: #172033;
    font-size: 13px;
    font-weight: 650;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .gallery-author__top-score {
    grid-column: 2;
    color: #6a7890;
    font-size: 12px;
  }

  .gallery-author__toolbar {
    justify-content: space-between;
    margin-top: 22px;
  }

  .gallery-author__content {
    min-height: 360px;
    margin-top: 18px;
  }

  .gallery-author__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
  }

  .gallery-author__item {
    overflow: hidden;
    background: #fff;
    border: 1px solid #dfe6f0;
    border-radius: 8px;
  }

  .gallery-author__preview {
    display: grid;
    place-items: center;
    width: 100%;
    aspect-ratio: 4 / 3;
    padding: 0;
    overflow: hidden;
    cursor: pointer;
    background: #101828;
    border: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .gallery-author__placeholder {
    display: grid;
    gap: 8px;
    justify-items: center;
    color: #d7deea;
  }

  .gallery-author__item-body {
    padding: 14px;

    h2 {
      min-height: 44px;
      margin: 12px 0 8px;
      font-size: 16px;
      line-height: 1.4;
    }

    p {
      margin: 0 0 10px;
      color: #60708a;
      font-size: 13px;
    }
  }

  .gallery-author__item-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    color: #6a7890;
    font-size: 12px;
  }

  .gallery-author__loadmore {
    display: flex;
    justify-content: center;
    margin-top: 22px;
  }

  @media (max-width: 760px) {
    .gallery-author {
      padding: 24px 16px 36px;
    }

    .gallery-author__hero {
      align-items: flex-start;
      flex-direction: column;

      h1 {
        font-size: 30px;
      }
    }

    .gallery-author__stats {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .gallery-author__spotlight {
      grid-template-columns: 1fr;
    }

    .gallery-author__toolbar {
      align-items: flex-start;
      flex-direction: column;
    }
  }
</style>

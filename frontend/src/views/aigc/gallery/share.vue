<template>
  <main class="gallery-share">
    <section class="gallery-share__hero">
      <div class="gallery-share__preview">
        <img
          v-if="asset?.contentType === 'IMAGE' && previewUrl"
          :src="previewUrl"
          :alt="asset.prompt"
        />
        <video
          v-else-if="asset?.contentType === 'VIDEO' && previewUrl"
          :src="previewUrl"
          controls
          playsinline
        />
        <audio v-else-if="asset?.contentType === 'AUDIO' && previewUrl" :src="previewUrl" controls />
        <div v-else class="gallery-share__empty">
          <el-icon :size="36"><Picture /></el-icon>
          <span>作品预览暂不可用</span>
        </div>
      </div>

      <div class="gallery-share__detail">
        <div class="gallery-share__topline">
          <el-tag effect="plain">{{ contentTypeLabel }}</el-tag>
          <span>{{ formatTime(asset?.createdAt) }}</span>
        </div>

        <h1>{{ title }}</h1>

        <div class="gallery-share__meta">
          <span>{{ asset?.model || '未知模型' }}</span>
          <button
            v-if="asset?.authorId"
            type="button"
            class="gallery-share__author-link"
            @click="openAuthorProfile"
          >
            {{ asset?.authorName || asset?.author || '匿名创作者' }}
          </button>
          <span v-else>{{ asset?.authorName || asset?.author || '匿名创作者' }}</span>
          <span>{{ asset?.likeCount || 0 }} 赞</span>
          <span>{{ asset?.favoriteCount || 0 }} 收藏</span>
        </div>

        <p class="gallery-share__prompt">{{ asset?.prompt || '这个作品暂时没有公开 Prompt。' }}</p>

        <div class="gallery-share__actions">
          <el-button type="primary" :icon="MagicStick" @click="handleReuse">复用 Prompt</el-button>
          <el-button :icon="Download" :disabled="!asset" @click="handleDownload">下载作品</el-button>
          <el-button :icon="Link" @click="handleCopyLink">复制链接</el-button>
          <el-button v-if="asset?.authorId" :icon="User" @click="openAuthorProfile">
            作者主页
          </el-button>
          <el-button text @click="router.push('/aigc/gallery')">打开广场</el-button>
        </div>
      </div>
    </section>

    <section class="gallery-share__body">
      <div class="gallery-share__prompt-panel">
        <div class="gallery-share__section-head">
          <h2>Prompt</h2>
          <el-button :icon="DocumentCopy" text @click="handleCopyPrompt">复制</el-button>
        </div>
        <p>{{ asset?.prompt || '-' }}</p>
      </div>

      <div class="gallery-share__info">
        <div>
          <span>访问策略</span>
          <strong>{{ asset?.publicAccessMode || 'published-preview' }}</strong>
        </div>
        <div>
          <span>资产 ID</span>
          <strong>{{ asset?.id || routeAssetId }}</strong>
        </div>
        <div>
          <span>模型</span>
          <strong>{{ asset?.model || '-' }}</strong>
        </div>
      </div>
    </section>

    <el-empty
      v-if="!loading && loadError"
      class="gallery-share__error"
      :description="loadError"
    >
      <el-button type="primary" @click="loadShare">重新加载</el-button>
    </el-empty>
  </main>
</template>

<script setup lang="ts">
  import { DocumentCopy, Download, Link, MagicStick, Picture, User } from '@element-plus/icons-vue'
  import { useClipboard } from '@vueuse/core'
  import { ElMessage } from 'element-plus'
  import { fetchGetGalleryShare } from '@/api/aigc'
  import type { GalleryItem, GalleryShareResponse } from '@/api/model/aigcModel'
  import {
    downloadAigcGalleryAsset,
    resolveAigcGalleryPreviewUrl
  } from '@/utils/aigcAsset'
  import { formatDateTime } from '@/utils/time'

  defineOptions({ name: 'AIGCGalleryShare' })

  const route = useRoute()
  const router = useRouter()
  const { copy } = useClipboard()

  const loading = ref(false)
  const loadError = ref('')
  const share = ref<GalleryShareResponse | null>(null)
  const asset = computed<GalleryItem | undefined>(() => share.value?.asset)
  const routeAssetId = computed(() => String(route.params.assetId || ''))
  const previewUrl = computed(() => {
    if (share.value?.previewUrl) return share.value.previewUrl
    return resolveAigcGalleryPreviewUrl(asset.value)
  })

  const title = computed(() => {
    const prompt = asset.value?.prompt?.trim()
    return prompt ? truncate(prompt, 34) : 'AIGC 公开作品'
  })

  const contentTypeLabel = computed(() => {
    const labels = {
      IMAGE: '图片',
      VIDEO: '视频',
      AUDIO: '音频'
    } as const
    return asset.value?.contentType ? labels[asset.value.contentType] : '作品'
  })

  const loadShare = async () => {
    if (!routeAssetId.value) return
    loading.value = true
    loadError.value = ''
    try {
      share.value = await fetchGetGalleryShare(routeAssetId.value)
    } catch (error) {
      console.error('加载公开分享作品失败:', error)
      loadError.value = '公开作品不存在或已撤回'
    } finally {
      loading.value = false
    }
  }

  const handleReuse = async () => {
    if (asset.value?.prompt) {
      await copy(asset.value.prompt)
    }
    router.push({
      path: '/aigc/studio',
      query: {
        prompt: asset.value?.prompt || undefined,
        contentType: asset.value?.contentType || undefined
      }
    })
  }

  const handleDownload = async () => {
    if (!asset.value) return
    try {
      await downloadAigcGalleryAsset(asset.value)
    } catch (error) {
      console.error('下载公开作品失败:', error)
      ElMessage.error('下载失败，请稍后重试')
    }
  }

  const handleCopyLink = async () => {
    await copyText(window.location.href, '链接已复制')
  }

  const handleCopyPrompt = async () => {
    if (!asset.value?.prompt) return
    await copyText(asset.value.prompt, 'Prompt 已复制')
  }

  const openAuthorProfile = () => {
    if (!asset.value?.authorId) return
    router.push(`/share/creators/${encodeURIComponent(asset.value.authorId)}`)
  }

  const copyText = async (text: string, successMessage: string) => {
    try {
      await copy(text)
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text)
      }
      ElMessage.success(successMessage)
    } catch (error) {
      console.error('复制文本失败:', error)
      ElMessage.error('复制失败')
    }
  }

  const formatTime = (value?: string) => {
    if (!value) return '刚刚发布'
    return formatDateTime(value, { second: undefined })
  }

  const truncate = (value: string, length: number) => {
    return value.length > length ? `${value.slice(0, length)}...` : value
  }

  onMounted(loadShare)
</script>

<style scoped lang="scss">
  .gallery-share {
    min-height: 100vh;
    background: #f6f8fb;
    color: #172033;
  }

  .gallery-share__hero {
    display: grid;
    grid-template-columns: minmax(0, 1.12fr) minmax(360px, 0.88fr);
    gap: 32px;
    max-width: 1280px;
    margin: 0 auto;
    padding: 40px 28px 24px;
  }

  .gallery-share__preview {
    display: grid;
    place-items: center;
    min-height: 520px;
    overflow: hidden;
    background: #101828;
    border: 1px solid #d9e1ec;
    border-radius: 8px;

    img,
    video {
      width: 100%;
      height: 100%;
      max-height: 72vh;
      object-fit: contain;
      background: #101828;
    }

    audio {
      width: min(520px, calc(100% - 48px));
    }
  }

  .gallery-share__empty {
    display: grid;
    gap: 12px;
    justify-items: center;
    color: #d7deea;
  }

  .gallery-share__detail {
    align-self: center;
    min-width: 0;
  }

  .gallery-share__topline,
  .gallery-share__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 14px;
    align-items: center;
    color: #5f6f89;
    font-size: 13px;
  }

  .gallery-share__author-link {
    padding: 0;
    color: #2f6fed;
    font: inherit;
    cursor: pointer;
    background: transparent;
    border: 0;
  }

  .gallery-share__detail h1 {
    margin: 18px 0 14px;
    font-size: 40px;
    line-height: 1.16;
    font-weight: 760;
  }

  .gallery-share__prompt {
    display: -webkit-box;
    margin: 24px 0;
    overflow: hidden;
    color: #35445e;
    font-size: 16px;
    line-height: 1.8;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 8;
  }

  .gallery-share__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  .gallery-share__body {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 320px;
    gap: 24px;
    max-width: 1280px;
    margin: 0 auto;
    padding: 0 28px 48px;
  }

  .gallery-share__prompt-panel,
  .gallery-share__info {
    background: #fff;
    border: 1px solid #dfe6f0;
    border-radius: 8px;
  }

  .gallery-share__prompt-panel {
    padding: 22px;

    p {
      margin: 0;
      color: #26354d;
      line-height: 1.9;
      white-space: pre-wrap;
      word-break: break-word;
    }
  }

  .gallery-share__section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;

    h2 {
      margin: 0;
      font-size: 18px;
    }
  }

  .gallery-share__info {
    display: grid;
    align-content: start;
    padding: 18px;

    div {
      display: grid;
      gap: 6px;
      padding: 14px 0;
      border-bottom: 1px solid #edf1f6;
    }

    div:last-child {
      border-bottom: 0;
    }

    span {
      color: #6a7890;
      font-size: 12px;
    }

    strong {
      min-width: 0;
      overflow-wrap: anywhere;
      color: #1d2a3e;
      font-size: 14px;
      font-weight: 650;
    }
  }

  .gallery-share__error {
    padding-bottom: 48px;
  }

  @media (max-width: 960px) {
    .gallery-share__hero,
    .gallery-share__body {
      grid-template-columns: 1fr;
      padding-right: 18px;
      padding-left: 18px;
    }

    .gallery-share__preview {
      min-height: 320px;
    }

    .gallery-share__detail h1 {
      font-size: 30px;
    }

    .gallery-share__body {
      padding-bottom: 32px;
    }
  }
</style>

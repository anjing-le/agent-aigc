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
          <el-button :icon="Picture" :disabled="!asset" @click="handleDownloadPoster">
            下载海报
          </el-button>
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

      <aside class="gallery-share__side">
        <div class="gallery-share__poster">
          <div class="gallery-share__poster-mark">agent-aigc</div>
          <div class="gallery-share__poster-media">
            <img
              v-if="asset?.contentType === 'IMAGE' && previewUrl"
              :src="previewUrl"
              :alt="posterTitle"
            />
            <div v-else>{{ contentTypeLabel }}</div>
          </div>
          <h2>{{ posterTitle }}</h2>
          <p>{{ posterSubtitle }}</p>
          <span>{{ posterFooter }}</span>
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
      </aside>
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
  import { fetchGetGalleryShare, fetchRecordGallerySharePromptReuse } from '@/api/aigc'
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
  const managedSeoElements = new Map<HTMLMetaElement | HTMLLinkElement, string | null>()
  const asset = computed<GalleryItem | undefined>(() => share.value?.asset)
  const routeAssetId = computed(() => String(route.params.assetId || ''))
  const previewUrl = computed(() => {
    if (share.value?.previewUrl) return share.value.previewUrl
    return resolveAigcGalleryPreviewUrl(asset.value)
  })

  const title = computed(() => {
    if (share.value?.posterTitle) return share.value.posterTitle
    const prompt = asset.value?.prompt?.trim()
    return prompt ? truncate(prompt, 34) : 'AIGC 公开作品'
  })

  const seoTitle = computed(() => share.value?.seoTitle || `${title.value} | agent-aigc`)
  const seoDescription = computed(() => {
    if (share.value?.seoDescription) return share.value.seoDescription
    return asset.value?.prompt ? truncate(asset.value.prompt, 96) : 'agent-aigc 公开分享作品'
  })
  const seoKeywords = computed(() => share.value?.seoKeywords || 'AIGC,Prompt,agent-aigc')
  const posterTitle = computed(() => share.value?.posterTitle || title.value)
  const posterSubtitle = computed(
    () =>
      share.value?.posterSubtitle ||
      `${contentTypeLabel.value} · ${asset.value?.authorName || asset.value?.author || '匿名创作者'} · ${
        asset.value?.model || '未知模型'
      }`
  )
  const posterFooter = computed(
    () => share.value?.posterFooter || `agent-aigc · /share/gallery/${routeAssetId.value}`
  )
  const shareUrl = computed(() => {
    if (typeof window === 'undefined') return share.value?.sharePath || ''
    const path = share.value?.sharePath || `/share/gallery/${routeAssetId.value}`
    const href = router.resolve({ path }).href
    return `${window.location.origin}${window.location.pathname}${href}`
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
      syncSeoMeta()
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
    if (asset.value?.id) {
      try {
        await fetchRecordGallerySharePromptReuse(asset.value.id)
      } catch (error) {
        console.error('记录 Prompt 复用失败:', error)
      }
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
    await copyText(shareUrl.value || window.location.href, '链接已复制')
  }

  const handleCopyPrompt = async () => {
    if (!asset.value?.prompt) return
    await copyText(asset.value.prompt, 'Prompt 已复制')
  }

  const openAuthorProfile = () => {
    if (!asset.value?.authorId) return
    router.push(`/share/creators/${encodeURIComponent(asset.value.authorId)}`)
  }

  const handleDownloadPoster = async () => {
    if (!asset.value) return
    try {
      const blob = await createPosterBlob()
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `aigc-gallery-${asset.value.id || routeAssetId.value}-poster.png`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(link.href)
    } catch (error) {
      console.error('生成分享海报失败:', error)
      ElMessage.error('海报生成失败')
    }
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

  const syncSeoMeta = () => {
    document.title = seoTitle.value
    upsertMeta('name', 'description', seoDescription.value)
    upsertMeta('name', 'keywords', seoKeywords.value)
    upsertMeta('property', 'og:title', seoTitle.value)
    upsertMeta('property', 'og:description', seoDescription.value)
    upsertMeta('property', 'og:type', 'article')
    upsertMeta('property', 'og:url', shareUrl.value || window.location.href)
    if (previewUrl.value) {
      upsertMeta('property', 'og:image', toAbsoluteUrl(previewUrl.value))
    }
    upsertCanonical(shareUrl.value || window.location.href)
  }

  const upsertMeta = (attribute: 'name' | 'property', key: string, content: string) => {
    let element = document.head.querySelector<HTMLMetaElement>(`meta[${attribute}="${key}"]`)
    const created = !element
    if (!element) {
      element = document.createElement('meta')
      element.setAttribute(attribute, key)
      element.dataset.aigcShareCreated = 'true'
      document.head.appendChild(element)
    }
    if (!managedSeoElements.has(element)) {
      managedSeoElements.set(element, created ? null : element.getAttribute('content'))
    }
    element.setAttribute('content', content)
  }

  const upsertCanonical = (href: string) => {
    let element = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]')
    const created = !element
    if (!element) {
      element = document.createElement('link')
      element.rel = 'canonical'
      element.dataset.aigcShareCreated = 'true'
      document.head.appendChild(element)
    }
    if (!managedSeoElements.has(element)) {
      managedSeoElements.set(element, created ? null : element.getAttribute('href'))
    }
    element.href = href
  }

  const restoreSeoMeta = () => {
    managedSeoElements.forEach((previousValue, element) => {
      if (element.dataset.aigcShareCreated === 'true') {
        element.remove()
      } else if (previousValue == null) {
        element.removeAttribute(element instanceof HTMLLinkElement ? 'href' : 'content')
      } else {
        element.setAttribute(element instanceof HTMLLinkElement ? 'href' : 'content', previousValue)
      }
    })
    managedSeoElements.clear()
  }

  const toAbsoluteUrl = (value: string) => {
    try {
      return new URL(value, window.location.origin).toString()
    } catch {
      return value
    }
  }

  const truncate = (value: string, length: number) => {
    return value.length > length ? `${value.slice(0, length)}...` : value
  }

  const createPosterBlob = async () => {
    const canvas = document.createElement('canvas')
    canvas.width = 1080
    canvas.height = 1440
    const context = canvas.getContext('2d')
    if (!context) throw new Error('Canvas context unavailable')

    context.fillStyle = '#101828'
    context.fillRect(0, 0, canvas.width, canvas.height)
    drawPosterBackground(context)

    if (asset.value?.contentType === 'IMAGE' && previewUrl.value) {
      try {
        const image = await loadImage(toAbsoluteUrl(previewUrl.value))
        drawContainedImage(context, image, 72, 96, 936, 620)
      } catch {
        drawPosterPlaceholder(context, 72, 96, 936, 620)
      }
    } else {
      drawPosterPlaceholder(context, 72, 96, 936, 620)
    }

    context.fillStyle = '#ffffff'
    context.font = '700 58px sans-serif'
    const titleBottom = drawWrappedText(context, posterTitle.value, 72, 820, 936, 72, 3)

    context.fillStyle = '#cbd5e1'
    context.font = '32px sans-serif'
    context.fillText(posterSubtitle.value, 72, titleBottom + 58)

    context.fillStyle = '#e2e8f0'
    context.font = '32px sans-serif'
    drawWrappedText(context, asset.value?.prompt || 'AIGC 公开作品', 72, titleBottom + 126, 936, 46, 4)

    context.fillStyle = '#94a3b8'
    context.font = '28px sans-serif'
    context.fillText(posterFooter.value, 72, 1322)
    context.fillText(shareUrl.value, 72, 1374)

    return new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(blob => (blob ? resolve(blob) : reject(new Error('Canvas export failed'))), 'image/png')
    })
  }

  const drawPosterBackground = (context: CanvasRenderingContext2D) => {
    const gradient = context.createLinearGradient(0, 0, 1080, 1440)
    gradient.addColorStop(0, '#172554')
    gradient.addColorStop(0.48, '#101828')
    gradient.addColorStop(1, '#0f172a')
    context.fillStyle = gradient
    context.fillRect(0, 0, 1080, 1440)
    context.fillStyle = 'rgba(255,255,255,0.08)'
    context.fillRect(72, 52, 232, 48)
    context.fillStyle = '#ffffff'
    context.font = '700 24px sans-serif'
    context.fillText('agent-aigc', 98, 84)
  }

  const drawPosterPlaceholder = (
    context: CanvasRenderingContext2D,
    x: number,
    y: number,
    width: number,
    height: number
  ) => {
    context.fillStyle = 'rgba(255,255,255,0.1)'
    context.fillRect(x, y, width, height)
    context.fillStyle = '#e2e8f0'
    context.font = '700 52px sans-serif'
    context.fillText(contentTypeLabel.value, x + 56, y + height / 2)
  }

  const drawContainedImage = (
    context: CanvasRenderingContext2D,
    image: HTMLImageElement,
    x: number,
    y: number,
    width: number,
    height: number
  ) => {
    context.fillStyle = 'rgba(255,255,255,0.1)'
    context.fillRect(x, y, width, height)
    const scale = Math.min(width / image.width, height / image.height)
    const drawWidth = image.width * scale
    const drawHeight = image.height * scale
    context.drawImage(image, x + (width - drawWidth) / 2, y + (height - drawHeight) / 2, drawWidth, drawHeight)
  }

  const drawWrappedText = (
    context: CanvasRenderingContext2D,
    text: string,
    x: number,
    y: number,
    maxWidth: number,
    lineHeight: number,
    maxLines: number
  ) => {
    let line = ''
    let lineCount = 0
    let currentY = y
    Array.from(text).forEach(char => {
      const testLine = line + char
      if (context.measureText(testLine).width > maxWidth && line) {
        context.fillText(line, x, currentY)
        line = char
        lineCount += 1
        currentY += lineHeight
      } else {
        line = testLine
      }
    })
    if (line && lineCount < maxLines) {
      context.fillText(line, x, currentY)
      currentY += lineHeight
    }
    return currentY
  }

  const loadImage = (src: string) =>
    new Promise<HTMLImageElement>((resolve, reject) => {
      const image = new Image()
      image.crossOrigin = 'anonymous'
      image.onload = () => resolve(image)
      image.onerror = reject
      image.src = src
    })

  onMounted(() => {
    loadShare()
  })
  onUnmounted(restoreSeoMeta)
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
  .gallery-share__poster,
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

  .gallery-share__side {
    display: grid;
    align-content: start;
    gap: 16px;
    min-width: 0;
  }

  .gallery-share__poster {
    min-width: 0;
    padding: 16px;
  }

  .gallery-share__poster-mark {
    width: fit-content;
    margin-bottom: 12px;
    padding: 5px 9px;
    color: #2f6fed;
    font-size: 12px;
    font-weight: 700;
    background: #eef4ff;
    border-radius: 6px;
  }

  .gallery-share__poster-media {
    display: grid;
    place-items: center;
    width: 100%;
    aspect-ratio: 4 / 3;
    overflow: hidden;
    color: #d7deea;
    font-size: 20px;
    font-weight: 700;
    background: #101828;
    border-radius: 6px;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .gallery-share__poster h2 {
    display: -webkit-box;
    margin: 16px 0 8px;
    overflow: hidden;
    color: #172033;
    font-size: 18px;
    line-height: 1.35;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
  }

  .gallery-share__poster p {
    margin: 0 0 12px;
    color: #53627a;
    font-size: 13px;
    line-height: 1.6;
  }

  .gallery-share__poster span {
    display: block;
    overflow-wrap: anywhere;
    color: #8190a6;
    font-size: 12px;
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

import type { AssetItem, ContentType, GalleryItem } from '@/api/model/aigcModel'
import { ApiPaths, resolveApiPath } from '@/api/paths'
import { useUserStore } from '@/store/modules/user'
import { buildRequestContextHeaders, REQUEST_HEADERS } from '@/utils/http/context'

type PreviewableMaterial = {
  id?: string
  url?: string
}

const EXTENSION_MAP: Record<ContentType, string> = {
  IMAGE: 'png',
  VIDEO: 'mp4',
  AUDIO: 'mp3'
}

export const getAigcAssetExtension = (contentType: ContentType) => {
  return EXTENSION_MAP[contentType?.toUpperCase() as ContentType] || 'file'
}

export const resolveAigcAssetPreviewUrl = (
  asset: Pick<AssetItem, 'id' | 'url' | 'thumbnailUrl'> | null | undefined
) => {
  if (!asset) return ''
  if (isInlinePreview(asset.thumbnailUrl)) return asset.thumbnailUrl || ''
  if (isInlinePreview(asset.url)) return asset.url || ''
  if (!asset.id) return asset.thumbnailUrl || asset.url || ''
  return resolveApiPath(ApiPaths.aigc.assetPreview(asset.id))
}

export const resolveAigcMaterialPreviewUrl = (material: PreviewableMaterial | null | undefined) => {
  if (!material) return ''
  if (isInlinePreview(material.url)) return material.url || ''
  if (!material.id) return material.url || ''
  return resolveApiPath(ApiPaths.aigc.materialPreview(material.id))
}

export const resolveAigcGalleryPreviewUrl = (
  item: Pick<GalleryItem, 'id' | 'url' | 'thumbnailUrl' | 'previewUrl'> | null | undefined
) => {
  if (!item) return ''
  const previewUrl = item.thumbnailUrl || item.previewUrl || item.url
  if (previewUrl) {
    return isAbsolutePreview(previewUrl) ? previewUrl : resolveApiPath(previewUrl)
  }
  return item.id ? resolveApiPath(ApiPaths.aigc.galleryAssetPreview(item.id)) : ''
}

export const resolveAigcGalleryDownloadUrl = (item: Pick<GalleryItem, 'id'> | null | undefined) => {
  return item?.id ? resolveApiPath(ApiPaths.aigc.galleryAssetDownload(item.id)) : ''
}

export const downloadAigcAsset = async (asset: Pick<AssetItem, 'id' | 'url' | 'contentType'>) => {
  if (!asset.id) return

  const response = await fetch(resolveApiPath(ApiPaths.aigc.assetDownload(asset.id)), {
    method: 'GET',
    headers: buildAigcDownloadHeaders(),
    credentials: import.meta.env.VITE_WITH_CREDENTIALS === 'true' ? 'include' : 'same-origin'
  })
  if (!response.ok) {
    throw new Error(`AIGC asset download failed: ${response.status}`)
  }
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  try {
    link.href = objectUrl
    link.download =
      resolveDownloadFileName(response.headers.get('content-disposition')) ||
      `aigc-${asset.id}.${getAigcAssetExtension(asset.contentType)}`
    link.rel = 'noopener noreferrer'
    document.body.appendChild(link)
    link.click()
  } finally {
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
  }
}

export const downloadAigcGalleryAsset = async (
  item: Pick<GalleryItem, 'id' | 'contentType'> | null | undefined
) => {
  if (!item?.id) return

  await downloadFromAigcUrl(
    resolveApiPath(ApiPaths.aigc.galleryAssetDownload(item.id)),
    `aigc-gallery-${item.id}.${getAigcAssetExtension(item.contentType)}`
  )
}

const downloadFromAigcUrl = async (url: string, fallbackFileName: string) => {
  const response = await fetch(url, {
    method: 'GET',
    headers: buildAigcDownloadHeaders(),
    credentials: import.meta.env.VITE_WITH_CREDENTIALS === 'true' ? 'include' : 'same-origin'
  })
  if (!response.ok) {
    throw new Error(`AIGC download failed: ${response.status}`)
  }
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  try {
    link.href = objectUrl
    link.download = resolveDownloadFileName(response.headers.get('content-disposition')) || fallbackFileName
    link.rel = 'noopener noreferrer'
    document.body.appendChild(link)
    link.click()
  } finally {
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
  }
}

const buildAigcDownloadHeaders = (): HeadersInit => {
  const userStore = useUserStore()
  const headers = buildRequestContextHeaders(userStore.language)
  if (userStore.accessToken) {
    headers.Authorization = userStore.accessToken
  }
  const userInfo = userStore.getUserInfo
  if (userInfo.userId) {
    headers[REQUEST_HEADERS.userId] = String(userInfo.userId)
  }
  if (userInfo.userName) {
    headers[REQUEST_HEADERS.userName] = userInfo.userName
  }
  if (userInfo.roles?.length) {
    headers[REQUEST_HEADERS.userRoles] = userInfo.roles.join(',')
  }
  return headers
}

const isInlinePreview = (url?: string) => Boolean(url?.startsWith('data:'))
const isAbsolutePreview = (url: string) => /^(https?:|data:|blob:)/i.test(url)

const resolveDownloadFileName = (contentDisposition: string | null) => {
  if (!contentDisposition) return null
  const encodedMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    return decodeURIComponent(encodedMatch[1])
  }
  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] || null
}

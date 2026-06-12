import type { AssetItem, ContentType } from '@/api/model/aigcModel'

const EXTENSION_MAP: Record<ContentType, string> = {
  IMAGE: 'png',
  VIDEO: 'mp4',
  AUDIO: 'mp3'
}

export const getAigcAssetExtension = (contentType: ContentType) => {
  return EXTENSION_MAP[contentType?.toUpperCase() as ContentType] || 'file'
}

export const downloadAigcAsset = (asset: Pick<AssetItem, 'id' | 'url' | 'contentType'>) => {
  if (!asset.url) return

  const link = document.createElement('a')
  link.href = asset.url
  link.download = `aigc-${asset.id}.${getAigcAssetExtension(asset.contentType)}`
  link.rel = 'noopener noreferrer'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

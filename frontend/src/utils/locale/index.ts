import {
  DEFAULT_LOCALE,
  SUPPORTED_LOCALES,
  type PlatformSupportedLocale
} from '@/contracts/platform-contract'
import { LanguageEnum } from '@/enums/appEnum'

export { DEFAULT_LOCALE, SUPPORTED_LOCALES }

const LANGUAGE_TAGS: Record<LanguageEnum, PlatformSupportedLocale> = {
  [LanguageEnum.ZH]: 'zh-CN',
  [LanguageEnum.EN]: 'en-US'
}

export const isSupportedLocale = (locale: string): locale is PlatformSupportedLocale =>
  (SUPPORTED_LOCALES as readonly string[]).includes(locale)

export const matchSupportedLocale = (locale?: string): PlatformSupportedLocale => {
  if (!locale) return DEFAULT_LOCALE
  if (isSupportedLocale(locale)) return locale

  const language = locale.split('-')[0]
  return (
    SUPPORTED_LOCALES.find((supported) => supported.split('-')[0] === language) || DEFAULT_LOCALE
  )
}

export const getClientLocale = (): PlatformSupportedLocale =>
  matchSupportedLocale(globalThis.navigator?.language)

export const getLanguageTag = (language?: LanguageEnum): PlatformSupportedLocale => {
  if (language && LANGUAGE_TAGS[language]) return LANGUAGE_TAGS[language]
  return getClientLocale()
}

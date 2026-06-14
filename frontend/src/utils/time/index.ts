import { DEFAULT_TIME_ZONE } from '@/contracts/platform-contract'
import { getClientLocale } from '@/utils/locale'

export type DateInput = Date | string | number | null | undefined

export interface FormatDateTimeOptions extends Intl.DateTimeFormatOptions {
  locale?: string
  timeZone?: string
}

export { DEFAULT_TIME_ZONE }

export const getClientTimeZone = (): string => {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || DEFAULT_TIME_ZONE
  } catch {
    return DEFAULT_TIME_ZONE
  }
}

export const toDate = (value: DateInput): Date | null => {
  if (value === null || value === undefined || value === '') return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

export const toIsoString = (value: DateInput): string | null => {
  const date = toDate(value)
  return date ? date.toISOString() : null
}

export const nowDate = (): Date => new Date()

export const nowIsoString = (): string => nowDate().toISOString()

const twoDigits = (value: number): string => String(value).padStart(2, '0')

const getDateParts = (
  value: DateInput,
  options: FormatDateTimeOptions = {}
): { year: string; month: string; day: string } | null => {
  const date = toDate(value)
  if (!date) return null

  const { locale = 'en-CA', timeZone = getClientTimeZone() } = options
  const parts = new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone
  }).formatToParts(date)

  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]))
  if (!values.year || !values.month || !values.day) return null

  return {
    year: values.year,
    month: twoDigits(Number(values.month)),
    day: twoDigits(Number(values.day))
  }
}

export const formatDateKey = (
  value: DateInput = nowDate(),
  options: FormatDateTimeOptions = {}
): string => {
  const parts = getDateParts(value, options)
  return parts ? `${parts.year}-${parts.month}-${parts.day}` : ''
}

export const formatFilenameTimestamp = (value: DateInput = nowDate()): string => {
  return (toIsoString(value) || nowIsoString()).replace(/[:.]/g, '-')
}

export const formatDateTime = (value: DateInput, options: FormatDateTimeOptions = {}): string => {
  const date = toDate(value)
  if (!date) return ''

  const { locale = getClientLocale(), timeZone = getClientTimeZone(), ...dateTimeOptions } = options

  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    timeZone,
    ...dateTimeOptions
  }).format(date)
}

export const formatDate = (value: DateInput, options: FormatDateTimeOptions = {}): string => {
  return formatDateTime(value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: undefined,
    minute: undefined,
    second: undefined,
    ...options
  })
}

export const formatTime = (value: DateInput, options: FormatDateTimeOptions = {}): string => {
  return formatDateTime(value, {
    year: undefined,
    month: undefined,
    day: undefined,
    hour: '2-digit',
    minute: '2-digit',
    second: undefined,
    ...options
  })
}

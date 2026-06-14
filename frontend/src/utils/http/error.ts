/**
 * HTTP 错误处理模块
 *
 * 提供统一的 HTTP 请求错误处理机制
 *
 * ## 主要功能
 *
 * - 自定义 HttpError 错误类，封装错误信息、状态码、时间戳等
 * - 错误拦截和转换，将 Axios 错误转换为标准的 HttpError
 * - 错误消息国际化处理，根据状态码返回对应的多语言错误提示
 * - 错误日志记录，便于问题追踪和调试
 * - 错误和成功消息的统一展示
 * - 类型守卫函数，用于判断错误类型
 *
 * ## 使用场景
 *
 * - HTTP 请求拦截器中统一处理错误
 * - 业务代码中捕获和处理特定错误
 * - 错误日志收集和上报
 *
 * @module utils/http/error
 * @author Art Design Pro Team
 */
import { AxiosError } from 'axios'
import { ApiStatus } from './status'
import { $t } from '@/locales'
import { extractResponseMessage } from './response'
import { nowIsoString } from '@/utils/time'
import { REQUEST_HEADERS } from '@/contracts/platform-contract'

// 错误响应接口
export interface ErrorResponse {
  /** 错误状态码 */
  code: number | string
  /** 错误消息（标准字段） */
  message?: string
  /** @deprecated 使用 message；仅用于旧 mock / 第三方接口过渡 */
  msg?: string
  /** 错误附加数据 */
  data?: unknown
  /** 请求 ID */
  requestId?: string
  /** Trace ID */
  traceId?: string
}

// 错误日志数据接口
export interface ErrorLogData {
  /** 错误状态码 */
  code: number
  /** 错误消息 */
  message: string
  /** 错误附加数据 */
  data?: unknown
  /** 错误发生时间戳 */
  timestamp: string
  /** 请求 URL */
  url?: string
  /** 请求方法 */
  method?: string
  /** 错误堆栈信息 */
  stack?: string
  /** 请求 ID */
  requestId?: string
  /** Trace ID */
  traceId?: string
}

// 自定义 HttpError 类
export class HttpError extends Error {
  public readonly code: number
  public readonly data?: unknown
  public readonly timestamp: string
  public readonly url?: string
  public readonly method?: string
  public readonly requestId?: string
  public readonly traceId?: string

  constructor(
    message: string,
    code: number,
    options?: {
      data?: unknown
      url?: string
      method?: string
      requestId?: string
      traceId?: string
    }
  ) {
    super(message)
    this.name = 'HttpError'
    this.code = code
    this.data = options?.data
    this.timestamp = nowIsoString()
    this.url = options?.url
    this.method = options?.method
    this.requestId = options?.requestId
    this.traceId = options?.traceId
  }

  public toLogData(): ErrorLogData {
    return {
      code: this.code,
      message: this.message,
      data: this.data,
      timestamp: this.timestamp,
      url: this.url,
      method: this.method,
      requestId: this.requestId,
      traceId: this.traceId,
      stack: this.stack
    }
  }
}

interface HeaderGetter {
  get(name: string): unknown
}

export interface HttpErrorContext {
  data?: unknown
  url?: string
  method?: string
  requestId?: string
  traceId?: string
}

const normalizeHeaderValue = (value: unknown): string | undefined => {
  if (Array.isArray(value)) return normalizeHeaderValue(value[0])
  if (typeof value !== 'string') return undefined

  const text = value.trim()
  return text || undefined
}

export const readHttpHeader = (headers: unknown, name: string): string | undefined => {
  if (!headers) return undefined

  const maybeGetter = (headers as Partial<HeaderGetter>).get
  if (typeof maybeGetter === 'function') {
    return (
      normalizeHeaderValue(maybeGetter.call(headers, name)) ||
      normalizeHeaderValue(maybeGetter.call(headers, name.toLowerCase()))
    )
  }

  const record = headers as Record<string, unknown>
  const directValue =
    normalizeHeaderValue(record[name]) || normalizeHeaderValue(record[name.toLowerCase()])
  if (directValue) return directValue

  const expectedName = name.toLowerCase()
  const actualKey = Object.keys(record).find((key) => key.toLowerCase() === expectedName)
  return actualKey ? normalizeHeaderValue(record[actualKey]) : undefined
}

export const buildHttpErrorContext = (options: {
  data?: ErrorResponse | null
  responseHeaders?: unknown
  requestHeaders?: unknown
  url?: string
  method?: string
}): HttpErrorContext => {
  return {
    data: options.data || undefined,
    url: options.url,
    method: options.method?.toUpperCase(),
    requestId:
      options.data?.requestId ||
      readHttpHeader(options.responseHeaders, REQUEST_HEADERS.requestId) ||
      readHttpHeader(options.requestHeaders, REQUEST_HEADERS.requestId),
    traceId:
      options.data?.traceId ||
      readHttpHeader(options.responseHeaders, REQUEST_HEADERS.traceId) ||
      readHttpHeader(options.requestHeaders, REQUEST_HEADERS.traceId)
  }
}

/**
 * 获取错误消息
 * @param status 错误状态码
 * @returns 错误消息
 */
const getErrorMessage = (status: number): string => {
  const errorMap: Record<number, string> = {
    [ApiStatus.unauthorized]: 'httpMsg.unauthorized',
    [ApiStatus.forbidden]: 'httpMsg.forbidden',
    [ApiStatus.notFound]: 'httpMsg.notFound',
    [ApiStatus.methodNotAllowed]: 'httpMsg.methodNotAllowed',
    [ApiStatus.requestTimeout]: 'httpMsg.requestTimeout',
    [ApiStatus.internalServerError]: 'httpMsg.internalServerError',
    [ApiStatus.badGateway]: 'httpMsg.badGateway',
    [ApiStatus.serviceUnavailable]: 'httpMsg.serviceUnavailable',
    [ApiStatus.gatewayTimeout]: 'httpMsg.gatewayTimeout'
  }

  return $t(errorMap[status] || 'httpMsg.internalServerError')
}

/**
 * 处理错误
 * @param error 错误对象
 * @returns 错误对象
 */
export function handleError(error: AxiosError<ErrorResponse>): never {
  const requestConfig = error.config

  // 处理取消的请求
  if (error.code === 'ERR_CANCELED') {
    console.warn('Request cancelled:', error.message)
    throw new HttpError(
      $t('httpMsg.requestCancelled'),
      ApiStatus.error,
      buildHttpErrorContext({
        requestHeaders: requestConfig?.headers,
        url: requestConfig?.url,
        method: requestConfig?.method
      })
    )
  }

  const statusCode = error.response?.status
  const errorMessage = extractResponseMessage(error.response?.data) || error.message
  const context = buildHttpErrorContext({
    data: error.response?.data,
    responseHeaders: error.response?.headers,
    requestHeaders: requestConfig?.headers,
    url: requestConfig?.url,
    method: requestConfig?.method
  })

  // 处理网络错误
  if (!error.response) {
    throw new HttpError($t('httpMsg.networkError'), ApiStatus.error, {
      ...context,
      data: undefined
    })
  }

  // 处理 HTTP 状态码错误
  const message = statusCode
    ? getErrorMessage(statusCode)
    : errorMessage || $t('httpMsg.requestFailed')
  throw new HttpError(message, statusCode || ApiStatus.error, {
    ...context,
    data: error.response.data
  })
}

/**
 * 显示错误消息
 * @param error 错误对象
 * @param showMessage 是否显示错误消息
 */
export function showError(error: HttpError, showMessage: boolean = true): void {
  if (showMessage) {
    ElMessage.error(error.message)
  }
  // 记录错误日志
  console.error('[HTTP Error]', error.toLogData())
}

/**
 * 显示成功消息
 * @param message 成功消息
 * @param showMessage 是否显示消息
 */
export function showSuccess(message: string, showMessage: boolean = true): void {
  if (showMessage) {
    ElMessage.success(message)
  }
}

/**
 * 判断是否为 HttpError 类型
 * @param error 错误对象
 * @returns 是否为 HttpError 类型
 */
export const isHttpError = (error: unknown): error is HttpError => {
  return error instanceof HttpError
}

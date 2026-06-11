import type { BaseResponse } from '@/types'
import { API_SUCCESS_CODE } from '@/contracts/platform-contract'
import { ApiStatus } from './status'

export { API_SUCCESS_CODE }

const UNAUTHORIZED_CODES = new Set(['401', '4000', '4002', '4003', '4004', '2106'])
const FORBIDDEN_CODES = new Set(['403', '4001', '2200'])

/** 判断响应码是否表示成功。数字 200 仅用于兼容旧 mock 或第三方响应。 */
export function isSuccessCode(code: BaseResponse['code']): boolean {
  return String(code) === API_SUCCESS_CODE || code === ApiStatus.success
}

/** 从标准响应中提取消息；msg 只作为旧接口过渡兼容。 */
export function extractResponseMessage(data?: Pick<BaseResponse, 'message' | 'msg'> | null) {
  return data?.message || data?.msg
}

/** 将后端业务错误码映射为前端可处理的 HTTP 风格错误码。 */
export function normalizeErrorCode(code: BaseResponse['code'] | undefined): number {
  if (code === undefined || code === null) return ApiStatus.error

  const textCode = String(code)
  if (UNAUTHORIZED_CODES.has(textCode)) return ApiStatus.unauthorized
  if (FORBIDDEN_CODES.has(textCode)) return ApiStatus.forbidden

  const numericCode = Number(code)
  return Number.isFinite(numericCode) ? numericCode : ApiStatus.error
}

export function isUnauthorizedCode(code: BaseResponse['code'] | undefined): boolean {
  return normalizeErrorCode(code) === ApiStatus.unauthorized
}

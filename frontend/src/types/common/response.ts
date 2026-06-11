/**
 * API 响应类型定义模块
 *
 * 提供统一的 API 响应结构类型定义
 *
 * ## 主要功能
 *
 * - 基础响应结构定义
 * - 泛型支持（适配不同数据类型）
 * - 统一的响应格式约束
 *
 * ## 使用场景
 *
 * - API 请求响应类型约束
 * - 接口数据类型定义
 * - 响应数据解析
 *
 * @module types/common/response
 * @author Art Design Pro Team
 */

/** 标准 API 响应结构：后端统一返回 code/message/data/timestamp/requestId */
export interface ApiResponseEnvelope<T = unknown> {
  /** 状态码，成功固定为字符串 "0" */
  code: string
  /** 消息 */
  message: string
  /** 数据 */
  data?: T | null
  /** 时间戳 */
  timestamp?: number
  /** 请求 ID，用于前后端日志关联 */
  requestId?: string
}

/** 基础 API 响应结构；保留 code: number 和 msg 仅用于旧 mock / 第三方接口过渡 */
export interface BaseResponse<T = unknown> extends Omit<ApiResponseEnvelope<T>, 'code' | 'message'> {
  /** 状态码（标准后端返回字符串 "0"） */
  code: number | string
  /** 消息（标准字段名） */
  message?: string
  /** @deprecated 使用 message；该字段仅用于旧 mock / 第三方接口过渡 */
  msg?: string
}

/** 旧接口层使用的响应命名，保留为 BaseResponse 的兼容别名 */
export type BaseResult<T = unknown> = BaseResponse<T>

/** 分页列表响应 */
export type PaginatedResponse<T = unknown> = Api.Common.PaginatedResponse<T>

/** 通用分页搜索参数 */
export type CommonSearchParams = Api.Common.CommonSearchParams

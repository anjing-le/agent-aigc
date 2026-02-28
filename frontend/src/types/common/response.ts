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

/** 基础 API 响应结构 */
export interface BaseResponse<T = unknown> {
  /** 状态码 (200 表示成功) */
  code: number
  /** 消息 */
  message: string
  /** 数据 */
  data: T
}

/** 通用搜索参数 */
export interface CommonSearchParams {
  /** 当前页码 */
  current?: number
  /** 每页大小 */
  size?: number
}

/** 分页响应结构 */
export interface PaginatedResponse<T = unknown> {
  /** 数据列表 */
  records: T[]
  /** 当前页码 */
  current: number
  /** 每页大小 */
  size: number
  /** 总数 */
  total: number
}

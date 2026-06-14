import request from '@/utils/http'
import {
  OPENAPI_OPERATIONS,
  type OpenApiHttpMethod,
  type OpenApiOperationData,
  type OpenApiOperationId,
  type OpenApiOperationPathParams,
  type OpenApiOperationQuery,
  type OpenApiOperationRequest
} from '@/contracts/openapi/operations'
import { resolveApiPath } from './paths'

export type OpenApiPathParamValue = string | number | boolean
export type OpenApiPathParams = Record<string, OpenApiPathParamValue>
export type OpenApiQueryParams = Record<string, unknown>

type OpenApiPathParamsOption<T extends OpenApiOperationId> =
  OpenApiOperationPathParams<T> extends undefined
    ? { pathParams?: never }
    : { pathParams: OpenApiOperationPathParams<T> }

type OpenApiQueryOption<T extends OpenApiOperationId> =
  OpenApiOperationQuery<T> extends undefined
    ? { query?: never }
    : { query?: OpenApiOperationQuery<T> }

type OpenApiBodyOption<T extends OpenApiOperationId> =
  OpenApiOperationRequest<T> extends undefined
    ? { body?: never }
    : { body: OpenApiOperationRequest<T> }

export type OpenApiRequestOptions<T extends OpenApiOperationId> = OpenApiPathParamsOption<T> &
  OpenApiQueryOption<T> &
  OpenApiBodyOption<T> & {
    showErrorMessage?: boolean
    showSuccessMessage?: boolean
  }

type OpenApiPathParamsArg<T extends OpenApiOperationId> =
  OpenApiOperationPathParams<T> extends undefined
    ? [pathParams?: undefined]
    : [pathParams: OpenApiOperationPathParams<T>]

type OpenApiOperationsWithOptionalOptions = {
  [K in OpenApiOperationId]: Record<string, never> extends OpenApiRequestOptions<K> ? K : never
}[OpenApiOperationId]

type OpenApiOperationsWithRequiredOptions = Exclude<
  OpenApiOperationId,
  OpenApiOperationsWithOptionalOptions
>

type OpenApiRuntimeOptions<T extends OpenApiOperationId> = {
  pathParams?: OpenApiPathParams
  query?: OpenApiQueryParams
  body?: OpenApiOperationRequest<T>
  showErrorMessage?: boolean
  showSuccessMessage?: boolean
}

interface OpenApiRuntimeRequestConfig {
  url: string
  method: OpenApiHttpMethod
  params?: OpenApiQueryParams
  data?: unknown
  showErrorMessage?: boolean
  showSuccessMessage?: boolean
}

const BODY_METHODS = new Set<OpenApiHttpMethod>(['POST', 'PUT', 'PATCH'])
const PATH_PARAM_PATTERN = /\{([^}]+)\}/g
const FILE_VALUE_TYPES = ['[object File]', '[object Blob]']

const isFileValue = (value: unknown): value is Blob => {
  return (
    typeof value === 'object' &&
    value !== null &&
    FILE_VALUE_TYPES.includes(Object.prototype.toString.call(value))
  )
}

const appendFormValue = (formData: FormData, key: string, value: unknown) => {
  if (value === undefined || value === null) return

  if (Array.isArray(value)) {
    value.forEach((item) => appendFormValue(formData, key, item))
    return
  }

  if (isFileValue(value)) {
    formData.append(key, value)
    return
  }

  if (typeof value === 'object') {
    formData.append(key, JSON.stringify(value))
    return
  }

  formData.append(key, String(value))
}

const toOpenApiRequestBody = (body: unknown) => {
  if (body instanceof FormData || isFileValue(body)) {
    return body
  }

  if (!body || typeof body !== 'object') {
    return body
  }

  const entries = Object.entries(body)
  if (
    !entries.some(
      ([, value]) => isFileValue(value) || (Array.isArray(value) && value.some(isFileValue))
    )
  ) {
    return body
  }

  const formData = new FormData()
  entries.forEach(([key, value]) => appendFormValue(formData, key, value))
  return formData
}

export const bindOpenApiPathParams = (apiPath: string, params: OpenApiPathParams = {}): string => {
  return apiPath.replace(PATH_PARAM_PATTERN, (token, name: string) => {
    const value = params[name]
    if (value === undefined) {
      throw new Error(`Missing OpenAPI path param: ${name}`)
    }
    return encodeURIComponent(String(value))
  })
}

export const openApiPath = <T extends OpenApiOperationId>(
  operationId: T,
  ...[pathParams]: OpenApiPathParamsArg<T>
): string => {
  return bindOpenApiPathParams(
    OPENAPI_OPERATIONS[operationId].path,
    pathParams as OpenApiPathParams | undefined
  )
}

export const resolveOpenApiPath = <T extends OpenApiOperationId>(
  operationId: T,
  ...[pathParams]: OpenApiPathParamsArg<T>
): string => {
  return resolveApiPath(
    bindOpenApiPathParams(
      OPENAPI_OPERATIONS[operationId].path,
      pathParams as OpenApiPathParams | undefined
    )
  )
}

export function openApiRequest<T extends OpenApiOperationsWithOptionalOptions>(
  operationId: T,
  options?: OpenApiRequestOptions<T>
): Promise<OpenApiOperationData<T>>

export function openApiRequest<T extends OpenApiOperationsWithRequiredOptions>(
  operationId: T,
  options: OpenApiRequestOptions<T>
): Promise<OpenApiOperationData<T>>

export function openApiRequest<T extends OpenApiOperationId>(
  operationId: T,
  options?: OpenApiRequestOptions<T>
): Promise<OpenApiOperationData<T>> {
  const requestOptions = (options || {}) as OpenApiRuntimeOptions<T>
  const operation = OPENAPI_OPERATIONS[operationId]
  const config: OpenApiRuntimeRequestConfig = {
    url: bindOpenApiPathParams(operation.path, requestOptions.pathParams),
    method: operation.method,
    showErrorMessage: requestOptions.showErrorMessage,
    showSuccessMessage: requestOptions.showSuccessMessage
  }

  if (requestOptions.query) {
    config.params = requestOptions.query
  }

  if (BODY_METHODS.has(operation.method) && requestOptions.body !== undefined) {
    config.data = toOpenApiRequestBody(requestOptions.body)
  }

  return request.request<OpenApiOperationData<T>>(config)
}
